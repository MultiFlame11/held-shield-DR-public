# Held Shield DR

Passive damage reduction when holding a shield, no active block needed.

Normally if you're just carrying a shield without raising it you take full damage. This changes that. Having a shield in either hand cuts incoming damage by a set percentage. How much is up to you in the config.

---

## What it does

- Reduces damage when a shield is held in main hand or offhand
- No active blocking needed, purely passive
- Players only by default, but can be extended to mobs
- Per-entity and per-item DR overrides if you want finer control
- Optional durability drain so the shield pays a cost for what it absorbs
- Shield items show their DR value in the tooltip

---

## Config

Forge/NeoForge: `heldshielddr-common.toml` in your config folder
Fabric: `heldshielddr.json` in your config folder

### General

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Turns the whole mod off if false. |
| `damageReductionPercent` | `33.0` | How much damage is reduced for players and whitelisted entities. 33 = one third less. |
| `applyToPlayers` | `true` | Whether players get the DR at all. |
| `playersUseItemOverrides` | `true` | If the held shield has a per-item override, players use it instead of the normal percent. |
| `showShieldTooltips` | `true` | Adds a line to shield tooltips showing the DR value. Requires client install to show. |
| `showItemOverrideText` | `false` | Adds "(Item Override)" to the tooltip when a per-item value is being used. |

### Entities

Off by default. Opt in if you want mobs to benefit too.

| Option | Default | Description |
|---|---|---|
| `applyToAllEntities` | `false` | Gives passive shield DR to any living entity holding a shield. |
| `allEntitiesDamageReductionPercent` | `33.0` | DR percent for entities covered by the above. Separate from the player value. |
| `entitiesUseItemOverrides` | `false` | Whether non-player entities can use per-item DR overrides. |
| `entityWhitelist` | *(example entries)* | Specific entity IDs that use the shared `damageReductionPercent`. Useful if you want a couple specific mobs covered without enabling it for everything. |
| `entityDamageReductionOverrides` | *(example entries)* | Per-entity DR values. These take priority over everything else for that entity. |

### Per-item overrides

| Option | Default | Description |
|---|---|---|
| `itemDamageReductionOverrides` | *(empty)* | Custom DR for specific shield items. Format: `item_id;percent` |

Example:
```
itemDamageReductionOverrides = ["basemetals:diamond_shield;90.0"]
```

### Durability drain

The shield loses durability based on how much damage it absorbed. Off by default.

| Option | Default | Description |
|---|---|---|
| `shieldDurabilityDrainEnabled` | `false` | Enables durability drain on passive blocks. |
| `shieldDurabilityDrainMultiplier` | `1.0` | Durability lost per point of damage prevented. 1.0 matches damage absorbed, 0.5 is half, 2.0 is double. |
| `shieldDurabilityDrainCap` | `0.0` | Max durability drained per hit. 0 means no cap. |

---

## Priority order

For non-player entities, DR is picked in this order:

1. Per-entity override - wins, ignores everything else
2. Entity whitelist - uses `damageReductionPercent`
3. All-entities fallback - uses `allEntitiesDamageReductionPercent`
4. Not covered - no DR applied

Per-item overrides layer on top: if an entity is covered by whitelist or all-entities and the held item has an override, it keeps whichever value is higher. Entities with a per-entity override always use that value and ignore item overrides.

---

## Versions

| Version | Loader | Jar |
|---|---|---|
| 1.12.2 | Forge | `held-shield-dr-1.1.1+1.12.2-forge.jar` |
| 1.16.5 | Forge | `held-shield-dr-1.1.1+1.16.5-forge.jar` |
| 1.16.5 | Fabric | `held-shield-dr-1.1.1+1.16.5-fabric.jar` |
| 1.18.2 | Forge | `held-shield-dr-1.1.1+1.18.2-forge.jar` |
| 1.18.2 | Fabric | `held-shield-dr-1.1.1+1.18.2-fabric.jar` |
| 1.20.1 | Forge | `held-shield-dr-1.1.1+1.20.1-forge.jar` |
| 1.20.1 | Fabric | `held-shield-dr-1.1.1+1.20.1-fabric.jar` |
| 1.20.4 | NeoForge | `held-shield-dr-1.1.1+1.20.4-neoforge.jar` |
| 1.20.4 | Fabric | `held-shield-dr-1.1.1+1.20.4-fabric.jar` |
| 1.21.1 | Forge | `held-shield-dr-1.1.1+1.21.1-forge.jar` |
| 1.21.1 | NeoForge | `held-shield-dr-1.1.1+1.21.1-neoforge.jar` |
| 1.21.1 | Fabric | `held-shield-dr-1.1.1+1.21.1-fabric.jar` |

---

## Notes

- Server-side only for the DR to work. Tooltips need client install.
- The default config has `touhou_little_maid` example entries in the whitelist and overrides. Clear them out if you're not using that mod, they don't break anything but they're just sitting there. Note: the entity ID changed between mod versions. Older releases use `touhou_little_maid:entity.passive.maid`, newer ones just use `touhou_little_maid:maid`. Check yours with F3 or a mod like Just Enough Items if you're not sure which one to put in.
- Durability drain defaults to off because it's a pretty different playstyle to have your shield slowly wearing down just from holding it. Enable it if that's what you want.
