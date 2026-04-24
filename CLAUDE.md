# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Setup

The NVRAM maps from Tomlogic are included as a Git submodule under `maps/`. Clone with:

```bash
git clone --recurse-submodules https://github.com/syd711/java-pinmame-nvmaps.git
# To update later:
git pull --recurse-submodules
```

## Commands

```bash
mvn clean install                             # Build
mvn test                                      # Run all tests
mvn -Dtest=NVRamMapParserTest test            # Run a single test class
```

During `mvn` build, the `download-maven-plugin` fetches `roms.json` from the Superhac release (v1.0.3) into `resources/superhac/` at the `generate-resources` phase. This only downloads if the file does not already exist.

## Architecture

This library parses pinball machine NVRAM (`.nv`) binary files to extract high scores, audits, adjustments, and DIP switch settings. It supports three independent parsing backends behind a common `NVRamParser` interface (`net.nvrams.mapping`):

| Backend | Package | Data Source |
|---|---|---|
| **MapParser** | `net.nvrams.mapping.map` | JSON maps from [tomlogic/pinmame-nvram-maps](https://github.com/tomlogic/pinmame-nvram-maps) (the `maps/` submodule) |
| **SuperhacParser** | `net.nvrams.mapping.superhac` | `resources/superhac/roms.json` downloaded at build time |
| **PinemhiParser** | `net.nvrams.mapping.pinemhi` | pinemhi emulator text output, parsed by adapters |

### Core data flow

1. Caller provides a ROM name (e.g. `"afm_113b"`) and a `.nv` file path.
2. The parser locates the ROM's mapping definition (JSON or roms.json).
3. The `.nv` binary is loaded into a `SparseMemory` object, which handles endianness and byte-range reads.
4. Score/audit/adjustment definitions are walked; `ByteDecoders`, `EntryDecoders`, and `ScoreDecoders` translate raw bytes into typed values.
5. Results are returned as `List<NVRamScore>` (each entry has initials, score, label, position).

### Key classes

- `NVRamMap` — top-level model for a ROM's mapping (high scores, mode champions, adjustments, audits, checksums, DIP switches)
- `SparseMemory` — wraps the raw `.nv` byte array; knows the platform's endianness and provides typed reads
- `NVRamPlatform` — CPU/hardware platform record (endianness, address space)
- `NVRamScore` — a single decoded score entry
- `NvRamScoreDecoders` — registry that dispatches to the right decoder strategy for a given score type

### PinemhiParser adapters

Pinemhi outputs plain text; parsing is done by adapter classes under `net.nvrams.mapping.pinemhi.adapters`. Each adapter handles a distinct output format. `RawScoreParser` is the entry point for converting that text into `NVRamScore` objects.

### GUI and CLI tools

- **JavaFX app** (`net.nvrams.mapping.extracter`) — `App.java` launches `MainView.fxml`; `VpxService` manages VPX file I/O; `MainController` wires the UI to the parsers.
- **CLI tools** (`net.nvrams.mapping.tools`) — `NVRamToolDump`, `NVRamToolHexDump`, `NVRamExtractTool`, `NVRamParserCompareTool` for headless inspection and parser comparison.

### Decoder package

`net.nvrams.mapping.decoder` contains the low-level decoding utilities and NVRam tool generators. Decoders are stateless and registered by type string, so adding support for a new encoding means adding a decoder implementation and registering it.
