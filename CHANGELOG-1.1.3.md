# Held Shield DR 1.1.3

Big config and parity pass across every shipped version.

## Added

- 1.21.1 NeoForge build
- `passiveBlockChance` for chance-based passive shield DR
- `damageReductionMode` with `fixed` and `random_range`
- `damageReductionMinPercent` and `damageReductionMaxPercent` for range tuning
- optional proc sound config with sound id, volume, pitch, and cooldown

## Changed

- all 12 builds now share the same shield DR feature set and config surface
- default behavior stays the same unless you turn on the new knobs
- publish setup was cleaned up so builds no longer depend on local Java paths

## Build coverage

All 12 builds touched, all 12 ship:

1.12.2 Forge, 1.16.5 Forge, 1.16.5 Fabric, 1.18.2 Forge, 1.18.2 Fabric, 1.20.1 Forge, 1.20.1 Fabric, 1.20.4 NeoForge, 1.20.4 Fabric, 1.21.1 Forge, 1.21.1 NeoForge, 1.21.1 Fabric.

## Upgrading

Drop in the new jar for your loader.

Existing configs still load fine. The new options will show up with their defaults when the config updates.

## Issues

If something breaks, open an issue with your version, loader, and config. Makes debugging way faster.
