# Everfilling Flasks

A Minecraft 1.12.2 Forge mod: a rechargeable healing flask in its own equipment slot, drunk by
holding a key, in the spirit of souls-like games. Flasks refill on their own over time, drinking
can be interrupted by hits, and flasks can be infused with infusions picked from wild plants.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- Add-on API for other mods: [docs/API.md](docs/API.md)
- Flask stats in qualities, set bonuses, food bonuses, and statuses: [docs/PACK-MAKERS.md](docs/PACK-MAKERS.md)
- License: [MIT](LICENSE) , see also [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

## Identity and installation

- Mod ID: `everfillingflasks`
- Required on both client and server.
- Dependencies: Forge 14.23.5.2847+ and
  [Inventory Button Bar](https://www.curseforge.com/minecraft/mc-mods/inventory-button-bar) 1.0.0+
  (minimums, no upper bounds), plus
  [Patchouli ROFL Edition](https://www.curseforge.com/minecraft/mc-mods/patchouli-rofl-edition),
  which draws the in-game journal. Patchouli carries no version bound, because that fork's
  version string does not order reliably as a Maven range; it is built and tested against
  `Patchouli-1.0-28.jar` (file 6178311). The other two are tested at their stated minimums.

## Configuration

One file, `config/everfillingflasks.cfg`, read at game start:

- `general`: `startingFlask`, `keepFlaskOnDeath`, `drinkSlowdown`, `infusionLoot` (chest loot,
  off by default since 1.4.0), `plantRegrowMinutes`, `diagnostics` (one log line per flask
  decision and per wild plant placed, for pack authors)
- `flasks.<tier>`: `maxCharges`, `healPercentage`, `rechargeTicks`, `drinkTicks`,
  `hitThreshold`, `potency` per built-in flask
- `infusions.<name>`: `cost` and `strength` per built-in infusion
- `recipes.<tier>`: one switch per built-in flask recipe
- `plants.<plant>` (`sunpetal`, `ironroot`, `quickmint`, `second_wind`, `everlasting_sprout`):
  `enabled`, `chancePercent`, `biomeTypes` (Forge biome dictionary types, `*` for every biome),
  `biomeNames` (biome registry names, also of other mods), `regrowFactor`
- `journal.textOverrides`: replace or hide the text on a journal entry per registry name, as
  `everfillingflasks:sunpetal_leaf=your text` or `everfillingflasks:sunpetal_leaf=` to hide

## Scope notes for pack authors

- The server decides everything: drink start, interrupts, completion, healing, charges,
  recharge, and infusion effects. Clients only display.
- Equipping a flask empties it. Charges are only earned while it sits in the slot, so swapping
  flasks mid-combat cannot refill anything.
- Everlasting Seeds and infusions come from wild plants in new Overworld chunks, one plant kind
  for each item. Hitting a plant in bloom gives the item and leaves a stem that blooms again;
  breaking the stem removes the plant for good. Land generated before 1.4.0 gets no plants.
  Neither item is craftable; the seed is what a flask is built around. `general.infusionLoot`
  puts them back into dungeon, mineshaft, and village blacksmith chests; it is off by default
  since 1.4.0, and a config file that already holds the key keeps its value.
- The journal builds itself from the registries on the client, so its numbers follow the config
  and its recipes follow the recipe registry. Nothing in it is duplicated data to maintain.
- Add-ons can register their own flasks, infusions, modifier bonuses, and HUD through
  `docs/API.md`; the public surface is the `api` packages only.
- Since 1.3.0 the six flask stats are player attributes (`everfillingflasks.healing` and the
  rest), so mods that change attributes by name can change flasks; with
  [Just Another Food System](https://www.curseforge.com/minecraft/mc-mods/just-another-food-system)
  installed, foods can too, and
  [Statusworks](https://www.curseforge.com/minecraft/mc-mods/statusworks) statuses can name them
  in a script. See `docs/PACK-MAKERS.md`.
