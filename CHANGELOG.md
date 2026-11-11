# Changelog

## 1.4.0

### Added

- Five wild plants grow in new Overworld chunks, at most one of each kind per chunk, each with a
  3 percent chance per chunk (Second Wind 1 percent):
  - Sunpetal in plains and savannas, on grass.
  - Ironroot in mountains and hills, on grass or stone.
  - Quickmint in forests, jungles, and swamps, on grass or dirt.
  - Second Wind in forests and plains, and with biome mods also magical and lush lands, on grass.
  - Everlasting Sprout anywhere in the Overworld, on grass or dirt.

  They also grow under trees and in tall grass. Hit one in bloom to get its infusion, or an
  Everlasting Seed from the Everlasting Sprout. A stem stays behind and blooms again after 90
  minutes of play for Sunpetal, Ironroot, and Quickmint, 180 minutes for Second Wind and the
  Everlasting Sprout. Breaking the stem removes the plant for good. The Everlasting Sprout glows
  faintly and sparkles now and then.
- Config: `general.plantRegrowMinutes` (180), and per plant under `plants.sunpetal`,
  `plants.ironroot`, `plants.quickmint`, `plants.second_wind`, and
  `plants.everlasting_sprout`: `enabled`, `chancePercent`, `biomeTypes` (Forge biome dictionary
  types, `*` for every biome), `biomeNames` (biome registry names, including biomes of other
  mods), and `regrowFactor`.
- The journal says where each herb and the seed grow wild, and follows the config.
- With `general.diagnostics` on, the log gets one line per plant placed, with its position and
  biome.

### Changed

- `general.infusionLoot` now defaults to false: Everlasting Seeds and infusions no longer turn up
  in dungeon, mineshaft, and village blacksmith chests. Set it to true to bring them back.
- Existing worlds and packs: land generated before this version gets no plants, so explore new
  land to find them. A config file that already holds `general.infusionLoot` keeps its value, so
  chest loot stays as it was there until you change it.

## 1.3.0

### Added

- The six flask stats are player attributes: `everfillingflasks.healing`,
  `everfillingflasks.drinkSpeed`, `everfillingflasks.hitThreshold`,
  `everfillingflasks.rechargeSpeed`, `everfillingflasks.maxCharges`, and
  `everfillingflasks.effectPower`. Mods that change player
  attributes by name, such as [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools)
  and [Setworks](https://www.curseforge.com/minecraft/mc-mods/setworks), can now change a player's
  flasks. Item tooltips name them "Flask Healing" and so on.
- With [Just Another Food System](https://www.curseforge.com/minecraft/mc-mods/just-another-food-system)
  1.0.0 or later installed, foods can change the flask stats through six food properties. Not
  required.
- `docs/PACK-MAKERS.md`: the attribute names and one working example for each of those mods.

### Changed

- The flask's state is computed on the tick after a login, a respawn, or a dimension change, so a
  bonus to max charges that Just Another Food System or Setworks puts back in those events is
  counted and does not cost a full flask a charge.

## 1.2.1

### Fixed

- The "Conflicts with ..." message in the flask screen was cut off for long infusion names. It
  now appears as a tooltip at the cursor, wrapped, so the full name shows.

## 1.2.0

Infusion API additions, all backward compatible: an add-on built against 1.1.0 runs unchanged.

### Added

- Infusion conflicts. An infusion definition may declare which pieces it refuses to share a
  grid with. Either side's answer is enough. A conflicting piece cannot be placed by clicking,
  swapping, or shift-clicking, and the flask screen names the placed piece it conflicts with.
- The drink outcome for infusion hooks. The post-drink hook has a four-argument form that
  receives the same outcome the flask's own hook received, effect power included. The older
  three-argument form keeps working.
- The core writes the tooltip of every registered infusion: a header, the potency cost, then
  the effect sentence or one signed line per bonus, such as "+20% healing" and "-15% drink
  speed". An add-on's own tooltip lines come after those; an add-on that wrote its own cost or
  effect lines should drop them, or they show twice.

### Changed

- A grid that holds a conflicting pair anyway, from an old save or a command, is inert like an
  over-capacity grid: no piece contributes, no hook runs, and the flask cannot drink until a
  piece is removed. The screen and the flask tooltip say so.
- The flask screen shows its warnings on one line under the title instead of beside it.

## 1.1.0

Add-on API additions, all backward compatible: an add-on built against 1.0.0 runs unchanged.

### Added

- Healing over time, owned by the core. A flask definition may declare a second heal spread
  over a number of ticks. The core pays it out on the server, reports it on the tooltip and in
  diagnostics, and applies the same healing bonuses as the instant part. A new drink replaces a
  running payout; death stops it; a relog drops it.
- A drink outcome for the completion hook: health before, heal requested, heal actually
  applied, over-time heal scheduled, charges left, and effect power. The old two-argument hook
  keeps working.
- `FlaskApi.advanceRecharge(player, ticks)`: an add-on can push the equipped flask's refill
  forward, completing charges with carry-over. Works while the Inhibited pause is active.
- Effect power, a sixth bonus channel the core carries to the flask for its secondary effect.

### Changed

- The three built-in flasks show plain white names. Sturdy and Radiant no longer carry the
  yellow and aqua rarity colours, so they read like add-on flasks in a list.
- The Inhibited link (`general.inhibitedIntegration`) is off by default. The feature is unchanged;
  a pack that wants recharge to freeze under the Inhibited effect turns it on. A config file
  written by 1.0.0 keeps the value it has.

## 1.0.0

First public release.

### Added

- A dedicated flask slot, opened from the Inventory Button Bar button, with the flask screen.
- Hold-to-drink healing: drinking takes time, slows walking, and is cancelled by releasing the
  key, opening a screen, or taking a solid hit (the damage threshold is configurable and can be
  raised by infusions and add-ons).
- Automatic recharge: flasks refill one charge at a time while equipped; the Inhibited mod's
  effect freezes recharge when that mod is installed.
- Three flasks: Humble (2 charges), Sturdy (3), Radiant (4), each crafted from the one below;
  every recipe has a config switch. All heal 33% of maximum health per drink by default.
- Equipping a flask empties it, so swapping flasks cannot refill charges mid-combat.
- Everlasting Seeds, found in dungeon chests, abandoned mineshafts, and village blacksmiths.
  They are why a flask refills itself, and every flask is built around one. Chest loot can be
  turned off in the config, which removes the seeds and the infusions together.
- The infusion grid: five slots on the flask with a potency budget of 10, so a full grid of the
  cheapest infusions spends it exactly. Over-filling makes the flask unusable until pieces are
  removed. A flask states its own slot count, from one to twelve, so an add-on can trade slots
  against budget.
- Four infusions from the same chests: Sunpetal Leaf (+8% healing), Ironroot Sprig (+32% damage
  threshold), Quickmint Leaf (+16% drink speed), and Second Wind Petal (regeneration after each
  completed drink).
- HUD charge icons that fill as the flask recharges, a centered cast bar with an interrupted
  flash, drinking animations in first and third person, and a completion burst with a chime.
- A starting flask granted once to each new player (configurable, can be disabled).
- A public add-on API: register flasks and infusions for your own items, decide how many
  infusion slots your flask carries, give any item a journal page, contribute flask bonuses from
  your mod's conditions, replace the completion effects, recolour the HUD liquid, or replace the
  flask HUD entirely. See docs/API.md.
- An in-game journal, opened from a book button on the flask screen, with Flasks, Infusions and
  Items sections. Every entry is built from the registration itself: the item, whatever is worth
  saying about it, and its recipe read from the recipe registry. Add-on content appears without
  the add-on shipping any journal data, and pack authors can replace or hide an entry's text per
  item in the config. Drawn by Patchouli ROFL Edition.
- Configuration for every gameplay number, plus a diagnostics mode that logs one line per
  flask decision for pack authors and server owners.
