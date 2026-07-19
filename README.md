# GTNH Calculator Utility

Runtime recipe exporter for **GT New Horizons** and the authoritative data source for the separate **GTNH Planner** web application.

## Project Status

GTNH Calculator Utility is under active development.

The exporter and schema-v2 JSON output are functional and are already consumed by the companion web UI. However, recipe-system coverage, automated contract testing, machine metadata, and release packaging are still being expanded.

The current version should be treated as a functional development build rather than a complete, stable release.

## Purpose

GTNH Calculator Utility is a Minecraft Forge mod for Minecraft 1.7.10 that runs inside a loaded GT New Horizons environment.

The mod reads recipe information directly from Minecraft and GregTech runtime registries, classifies the exported data, and writes it to a structured `recipes.json` file.

That export is consumed by a separate browser and production-planning application:

- [Exporter repository](https://github.com/kittyandy123/GTNH)
- [Planner UI repository](https://github.com/kittyandy123/GTNH-UI)

The Forge mod is the exporter layer. Recipe browsing, production calculations, graph interaction, saved plans, and other user-facing planner behavior belong in the web application.

## Architecture

The project is intentionally divided into two repositories.

### Forge exporter

This repository runs inside GTNH and is responsible for:

- Reading runtime recipe registries and GregTech recipe maps.
- Preserving exact recipe identities.
- Distinguishing consumed inputs and non-consumed tooling.
- Extracting programmed circuits and recipe metadata.
- Exporting item, fluid, duration, EU/t, and chance information.
- Detecting and skipping duplicate recipe identities.
- Recording diagnostics and extraction failures.
- Writing the resulting JSON document.

The exporter is the authoritative source for Minecraft and GTNH semantics that can be determined from the loaded game.

### Planner UI

The separate React and TypeScript application is responsible for:

- Loading and validating the exported recipe catalog.
- Searching and browsing recipes.
- Displaying exact recipes and grouped output views.
- Navigating between producers and consumers.
- Calculating production rates and machine requirements.
- Building production-line graphs and named planning workspaces.
- Persisting plans and comparing them against future base-state data.

The UI should not reproduce or guess recipe semantics that the exporter can determine more accurately.

### Data contract

`recipes.json` is the contract between the exporter and the planner UI.

The two repositories remain independent, but changes to the exported schema must be coordinated with the UI consumer.

## Current export coverage

The exporter currently includes:

- Vanilla furnace recipes.
- A curated set of GregTech recipe maps.

Current GregTech coverage:

- Mixer
- Centrifuge
- Electrolyzer
- Chemical Reactor
- Distillery
- Macerator
- Compressor
- Extractor
- Bender
- Wiremill
- Lathe
- Assembler
- Fluid Solidifier
- Forming Press
- Cutting Machine
- Laser Engraver
- Polarizer
- Extruder

This is not yet complete coverage of every GTNH recipe map, multiblock, passive resource system, magic system, or machine-specific mechanic.

Recipe maps are registered explicitly so that coverage can be expanded and validated deliberately instead of silently assuming that every discovered recipe is safe for planning.

## Current capabilities

The exporter currently supports:

- Schema-v2 JSON output.
- Vanilla furnace recipe extraction.
- GregTech recipe-map extraction.
- Item and fluid inputs and outputs.
- Consumed input classification.
- Non-consumed physical tooling in `tools[]`.
- Programmed circuit extraction through recipe metadata.
- Recipe duration in ticks and seconds.
- Recipe EU/t.
- Optional input and output chances.
- Machine ID, name, and category.
- GregTech recipe-map metadata.
- Hidden and fake-recipe metadata.
- Special-value and NBT-related metadata.
- Deterministic recipe identity generation.
- Duplicate recipe removal.
- Display-name cleanup and fallback reporting.
- Per-machine recipe counts.
- Recipe-export error diagnostics.
- Tool-extraction diagnostics.
- Pretty-printed JSON output for inspection and development.

## Requirements

### Runtime export

Generating an export requires:

- A GT New Horizons Minecraft 1.7.10 environment.
- The built GTNH Calculator Utility mod installed in that environment.
- A loaded client or server containing the runtime recipe registries to export.

### Development

Local development currently uses:

- JDK 25.
- The included Gradle wrapper.
- Git.

The repository uses Jabel for modern Java syntax while compiling to Java 8-compatible bytecode.

Jabel provides syntax support only. Source code must not rely on Java runtime APIs that are unavailable to the target environment.

## Building the project

The Gradle wrapper should be used instead of a separately installed Gradle distribution.

### Windows PowerShell

```powershell
.\gradlew.bat build
```

### Linux or macOS
```bash
./gradlew build
```

The `build` task assembles the project and runs its configured verification tasks.

## Development tasks

### Run a development client

Windows:
```powershell
.\gradlew.bat runClient
```

Linux or macOS:
```bash
./gradlew runClient
```

### Run a development server

Windows:
```powershell
.\gradlew.bat runServer
```

Linux or macOS:
```bash
./gradlew runServer
```

### Run tests

Windows:
```powershell
.\gradlew.bat test
```

Linux or macOS:
```bash
./gradlew test
```

### Check formatting

Windows:
```powershell
.\gradlew.bat spotlessCheck
```

Linux or macOS:
```bash
./gradlew spotlessCheck
```

### Apply formatting

Windows:
```powershell
.\gradlew.bat spotlessApply
```

Linux or macOS:
```bash
.\gradlew.bat spotlessApply
```

### Build-system troubleshooting

The GTNH build tooling provides a basic troubleshooting task:

Windows:
```powershell
.\gradlew.bat faq
```

Linux or macOS:
```bash
./gradlew faq
```

## Using the exporter

Install the built mod in a GTNH instance, launch the instance, and enter a world or server with the required recipe registries loaded.

Run:
```text
/gtnhcalc export
```

The command exports the current recipe catalog and prints:
- The total number of exported recipes.
- The output file path.
- The number of duplicate recipes skipped.
- Recipe counts by machine.

The command currently runs synchronously. A large export may briefly occupy the game thread while recipe extraction and JSON writing complete.

## Commands
| Command                | Purpose                                                                    |
|------------------------|----------------------------------------------------------------------------|
| `/gtnhcalc export`     | Exports the current recipe catalog to `recipes.json`.                      |
| `/gtnhcalc gt-summary` | Lists discovered GregTech recipe maps, truncated after 20 entries in chat. |
| `/gtnhcalc hello`      | Verifies that the mod command was registered successfully.                 |

Command aliases:
- `/gtnhcalculator`
- `/gtnhcu`

The command currently has no elevated permission requirement. Server operators should be aware that any command sender may invoke it in the present implementation.

## Export location

The exporter writes to:
```text
<Minecraft directory>/gtnh-calculator-utility/recipes.json
```

For a normal client installation, `<Minecraft directory>` is the root directory of the active Minecraft instance.

The UI repository currently consumes a copied or otherwise served version of this file.

## Export document

The top-level schema-v2 document contains:
```json
{
  "schemaVersion": 2,
  "pack": {},
  "export": {},
  "diagnostics": {},
  "recipes": []
}
```

### Top-level fields

| Field           | Purpose                                                                               |
|-----------------|---------------------------------------------------------------------------------------|
| `schemaVersion` | Identifies the structure and semantics of the export contract.                        |
| `pack`          | Identifies the modpack and Minecraft version represented by the export.               |
| `export`        | Records the exporter source, exporter version, and UTC export timestamp.              |
| `diagnostics`   | Records recipe totals, failures, fallbacks, duplicate counts, and tooling statistics. |
| `recipes`       | Contains the exported exact-recipe catalog.                                           |

## Recipe structure

Each exported recipe can contain:

| Field             | Meaning                                                   |
|-------------------|-----------------------------------------------------------|
| `id`              | Exporter-generated recipe identity.                       |
| `machine`         | Machine or recipe-source identity.                        |
| `durationTicks`   | Recipe duration in Minecraft ticks.                       |
| `durationSeconds` | Recipe duration in seconds.                               |
| `eut`             | Base recipe EU/t.                                         |
| `inputs`          | Consumed inputs for one recipe operation.                 |
| `tools`           | Required non-consumed tooling.                            |
| `outputs`         | Outputs produced by one recipe operation.                 |
| `metadata`        | Programmed circuits and other recipe-specific properties. |

## Stack semantics

Exported stacks include:

| Field         | Meaning                                                    |
|---------------|------------------------------------------------------------|
| `kind`        | Stack type, normally `item` or `fluid`.                    |
| `id`          | Registry or fluid identity.                                |
| `meta`        | Item metadata value; exported fluids use `0`.              |
| `displayName` | Human-readable label for display.                          |
| `amount`      | Quantity used or produced per recipe operation.            |
| `unit`        | Quantity unit, normally `items` or `L`.                    |
| `chance`      | Optional normalized probability for a probabilistic stack. |

Important identity rules:
- Item identity is based on `id` plus `meta`.
- Fluid identity is based on `id`.
- `displayName` is a presentation label and must not be used as a stable identity.
- `inputs[]` contains consumed resources.
- `tools[]` contains required resources that are not consumed per operation.
- `outputs[]` contains produced resources.
- `metadata.circuit` represents a programmed circuit selector, not a consumed input or physical tool.
- A missing `chance` field means that the stack is not represented as probabilistic in the export.

Planner calculations may derive per-second input and output rates from consumed stacks and recipe duration. Tooling must not be treated as per-second consumption.

## Diagnostics

The export includes diagnostics intended to expose regressions and incomplete extraction.

Current diagnostic categories include:

- Total exported recipes.
- Duplicate recipe identities skipped.
- Display-name fallback count and samples.
- Recipes skipped due to extraction errors.
- Recipe errors grouped by machine.
- Extracted non-consumed tool count.
- Tool counts grouped by machine.
- Zero-amount inputs moved to tooling.
- Zero-amount inputs remaining after classification.
- Tool amounts inferred by the exporter.
- Sample extracted tooling entries.
- Recipe counts grouped by machine.

Diagnostics should be inspected whenever exporter logic, dependencies, GTNH versions, or recipe-map coverage changes.

## Current limitations

The project currently has several deliberate limitations:

- GregTech recipe-map coverage is curated and incomplete.
- Not every GTNH mod or recipe system is exported.
- The schema does not yet include the exact GTNH pack version.
- Catalog fingerprints and identity-algorithm versions are not yet exported.
- Recipe IDs should not yet be treated as permanent foreign keys across arbitrary pack or exporter upgrades.
- A formal published JSON Schema and cross-repository contract test suite are not yet complete.
- Machine definitions are not yet exported separately.
- Voltage-tier compatibility is not yet modeled by the exporter.
- Overclocking and parallelization rules are not yet exported.
- Multiblock hatch, coil, tier, and structure constraints are not yet represented.
- Recipe-map support does not necessarily mean that a machine is fully modeled or safe for accurate production planning.
- The export is currently written as one pretty-printed JSON document and can become large.
- Live inventory, machine, power, and server telemetry are not part of the current exporter.
- The export command currently has no permission restriction.

The planner UI must represent unsupported or unknown machine behavior explicitly rather than silently assuming generic behavior.

## Development direction

Near-term exporter work is focused on:

1. Formalizing the versioned JSON contract.
2. Adding authoritative fixtures and automated exporter tests.
3. Adding cross-repository compatibility tests with the planner UI.
4. Exporting the GTNH pack version and catalog fingerprint.
5. Versioning the recipe identity algorithm.
6. Producing recipe-map coverage reports.
7. Expanding recipe-map support in validated batches.
8. Adding machine definitions and planning-safety metadata.
9. Improving chance-extraction diagnostics.
10. Separating extraction, semantic classification, identity, diagnostics, and file-writing responsibilities.

Longer-term data acquisition may expand from the current static export into an export bundle containing:

- Recipe data.
- Machine definitions.
- Multiblock definitions.
- Item, fluid, and machine assets.
- Tier and voltage metadata.
- Overclocking and parallelization capabilities.
- Equivalent ingredient information.
- Read-only base and inventory snapshots.
- Optional local or server-side snapshot integration.

Static exports will remain useful for reproducibility, offline planning, debugging, and sharing even as additional acquisition methods are introduced.

## Related project

The companion planner UI is maintained separately:
[GTNH-UI repository](https://github.com/kittyandy123/GTNH-UI).

The UI consumes `recipes.json` and is being developed as a planner-first GTNH engineering application rather than only a generic recipe calculator.

Its long-term scope includes:

- Recipe browsing and navigation.
- Named production-plan workspaces.
- Multi-node dependency graphs.
- Machine-count and throughput calculations.
- Byproduct routing.
- Base production and consumption records.
- Progression-aware machine modeling.
- Bottleneck and upgrade recommendations.
- Optional read-only game or server snapshots.

Those capabilities belong in the UI and planning domains rather than in this Forge exporter repository.

## License

This project is licensed under the MIT License.

See [LICENSE](LICENSE) for the full license text.
