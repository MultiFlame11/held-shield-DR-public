# Held Shield DR

Passive damage reduction for anyone holding a shield — no active block required.

If you're carrying a shield but haven't raised it, you still take full damage. This mod changes that. As long as a shield-like item is in your main hand or offhand, you take a configurable percentage less damage. It won't make you invincible. It just makes holding a shield feel like it actually matters.

---

## What it does

- Reduces incoming damage when a shield-like item is held (main hand or offhand)
- Works without actively blocking — purely passive
- Applies to players by default, optionally to non-player entities too
- Configurable reduction percentage, per-entity overrides, and per-item overrides
- Optional durability drain: the shield loses durability based on how much damage it absorbed
- Tooltips on shield items show their DR value so players know what they're holding

---

## Config

All config options live in `heldshielddr-common.toml` (Forge/NeoForge) or `heldshielddr.json` (Fabric) in your config folder.

### General

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch. Set to `false` to disable everything. |
| `damageReductionPercent` | `33.0` | Percentage of damage reduced for players and whitelisted entities. `33` = one third less damage. |
| `applyToPlayers` | `true` | Whether players benefit from passive shield DR. |
| `playersUseItemOverrides` | `true` | Whether players use per-item DR values when holding a matching shield. |
| `showShieldTooltips` | `true` | Shows a tooltip line on shield items describing their DR value. |
| `showItemOverrideText` | `false` | Appends `(Item Override)` to tooltips when a shield uses a per-item override. |

### Entities

By default only players are affected. These options let you extend it to mobs.

| Option | Default | Description |
|---|---|---|
| `applyToAllEntities` | `false` | Extends passive shield DR to all living entities holding a shield, not just players. |
| `allEntitiesDamageReductionPercent` | `33.0` | DR percent used for entities covered by `applyToAllEntities`. Separate from the player value so you can tune them independently. |
| `entitiesUseItemOverrides` | `false` | Whether eligible non-player entities can use per-item DR overrides. |
| `entityWhitelist` | *(see below)* | List of specific entity IDs that use the shared `damageReductionPercent`. Useful when `applyToAllEntities` is off but you still want a few specific mobs covered. |
| `entityDamageReductionOverrides` | *(see below)* | Per-entity custom DR values. Takes priority over everything else for that entity. |

### Per-item overrides

| Option | Default | Description |
|---|---|---|
| `itemDamageReductionOverrides` | *(empty)* | Custom DR values for specific shield items. Format: `item_id;percent`. |

Example:
```toml
itemDamageReductionOverrides = ["basemetals:diamond_shield;90.0"]
```

### Durability drain

When enabled, the held shield loses durability proportional to how much damage it absorbed. Shields earn their keep.

| Option | Default | Description |
|---|---|---|
| `shieldDurabilityDrainEnabled` | `false` | Whether blocking passively drains shield durability. |
| `shieldDurabilityDrainMultiplier` | `1.0` | Durability drained per point of damage prevented. `1.0` = drain matches damage absorbed. `0.5` = half that. `2.0` = double. |
| `shieldDurabilityDrainCap` | `0.0` | Maximum durability drained per hit. `0` = no cap. |

---

## Entity whitelist and overrides

The `entityWhitelist` and `entityDamageReductionOverrides` lists use entity IDs in `modid:entity_id` format.

**Whitelist** — uses the shared `damageReductionPercent`:
```toml
entityWhitelist = ["touhou_little_maid:entity.passive.maid"]
```

**Per-entity override** — uses its own custom value, ignores everything else:
```toml
entityDamageReductionOverrides = ["touhou_little_maid:entity.passive.maid;65.0"]
```

An entity in `entityDamageReductionOverrides` does not also need to be in `entityWhitelist`.

---

## Priority order

When figuring out how much DR an entity gets, the mod checks in this order:

1. Per-entity override (`entityDamageReductionOverrides`) — wins outright
2. Entity whitelist (`entityWhitelist`) — uses `damageReductionPercent`
3. All-entities fallback (`applyToAllEntities`) — uses `allEntitiesDamageReductionPercent`
4. Nothing — entity is not affected

Per-item overrides sit on top of this: if the held item has an override and item overrides are enabled, the higher of the two values wins (for whitelist/all-entities entities). Entities with a per-entity override always use that value and ignore item overrides entirely.

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

- Server-side only. No client installation needed (tooltips require client install to show).
- The default config includes `touhou_little_maid` entries as examples. Clear them out if you don't use that mod — they don't cause errors, they just sit there being a little embarrassing.
- Durability drain is off by default. It's opt-in because making your shield slowly wear down from just holding it is a meaningful change and not everyone wants that.
