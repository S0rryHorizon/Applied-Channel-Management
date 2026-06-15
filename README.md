# Applied Channel Management

**应用频道管理** is an Applied Energistics 2 addon for Minecraft 1.21.1 and NeoForge.

It treats every geometrically exposed face of a valid ME Controller multiblock as one shared 32-channel source. Wired devices are assigned first; wireless distributor branches then receive the remaining capacity by descending priority and stable distributor UUID.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.169
- Applied Energistics 2 19.2.17 exactly
- Java 21

The AE2 dependency is intentionally locked because the mod applies a narrow Mixin to AE2 channel pathing.

## Devices

### ME Channel Hub

- One active hub per controller grid.
- Does not consume a channel; drains 32 AE/t by default.
- Stores a globally unique, case-insensitive name, owner UUID, and whitelist.
- Shows total capacity, wired use, wireless use, and remaining capacity.

### ME Channel Distributor

- Binds to an authorized hub by name.
- Exposes at most 32 channels total across all connected faces.
- Drains 16 AE/t in the same dimension or 64 AE/t across dimensions.
- Never chunk-loads either endpoint and disconnects when an endpoint unloads or loses power.
- Rejects remote grids containing a controller, another hub, or a previously claimed distributor subnet.

Right-click either device to open its configuration screen. Hub whitelist entries accept online player names or UUIDs separated by commas.

Both devices appear in the Applied Energistics 2 creative tab. Their standard shaped recipes are discovered automatically by JEI; no separate JEI plugin is required.

## Channel Model

```text
pool capacity = exposed controller faces * 32
available wireless capacity = pool capacity - wired channels - wireless channels
```

Only faces touching another controller are internal. Covers, opaque blocks, and missing cable connections do not reduce the pool. AE2's local 8-channel and dense 32-channel cable bottlenecks remain unchanged.

The Mixin defers ACM virtual controller roots until AE2 finishes native wired allocation, then processes them by priority. It also enforces the global pool and the 32-channel per-distributor ceiling.

## Development

```bash
./gradlew build
./gradlew runData
./gradlew runGameTestServer
./gradlew runClient
```

The development environment uses the Gradle wrapper and downloads all dependencies automatically. The built jar is written to `build/libs/`.

## Configuration

The generated server config can override:

- Hub AE/t usage
- Same-dimension distributor AE/t usage
- Cross-dimension distributor AE/t usage
- Maximum hub name length
- Cross-dimension linking

## License

MIT
