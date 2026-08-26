# Everfilling Flasks

A Minecraft 1.12.2 Forge mod: a rechargeable healing flask in its own equipment slot, drunk by
holding a key, in the spirit of souls-like games. Flasks refill on their own over time, drinking
can be interrupted by hits, and flasks can be infused with infusions found in treasure chests.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- Add-on API for other mods: [docs/API.md](docs/API.md)
- License: [MIT](LICENSE) — see also [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

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

- `general`: `startingFlask`, `keepFlaskOnDeath`, `drinkSlowdown`, `infusionLoot`,
  `diagnostics` (one log line per flask decision, for pack authors)
- `flasks.<tier>`: `maxCharges`, `healPercentage`, `rechargeTicks`, `drinkTicks`,
  `hitThreshold`, `potency` per built-in flask
- `infusions.<name>`: `cost` and `strength` per built-in infusion
- `recipes.<tier>`: one switch per built-in flask recipe
- `journal.hintOverrides`: replace or hide a journal "Where to Find" line per registry name,
  as `everfillingflasks:sunpetal_leaf=your text` or `everfillingflasks:sunpetal_leaf=` to hide

## Scope notes for pack authors

- The server decides everything: drink start, interrupts, completion, healing, charges,
  recharge, and infusion effects. Clients only display.
- Equipping a flask empties it. Charges are only earned while it sits in the slot, so swapping
  flasks mid-combat cannot refill anything.
- Infusions come from dungeon, mineshaft, and village blacksmith chests
  (`general.infusionLoot` turns that off). They are not craftable.
- The journal builds itself from the registries on the client, so its numbers follow the config
  and its recipes follow the recipe registry. Nothing in it is duplicated data to maintain.
- Add-ons can register their own flasks, infusions, modifier bonuses, and HUD through
  `docs/API.md`; the public surface is the `api` packages only.
