# Everfilling Flasks

A Minecraft 1.12.2 Forge mod: a rechargeable healing flask in its own equipment slot, drunk by
holding a key, in the spirit of souls-like games. Flasks refill on their own over time, drinking
can be interrupted by hits, and flasks can be infused with ingredients found in treasure chests.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- Add-on API for other mods: [docs/API.md](docs/API.md)
- License: [MIT](LICENSE) — see also [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

## Identity and installation

- Mod ID: `everfillingflasks`
- Required on both client and server.
- Dependencies (minimums, no upper bounds): Forge 14.23.5.2847+,
  [Inventory Button Bar](https://www.curseforge.com/minecraft/mc-mods/inventory-button-bar) 1.0.0+.
  Built and tested against those exact versions.

## Configuration

One file, `config/everfillingflasks.cfg`, read at game start:

- `general`: `startingFlask`, `keepFlaskOnDeath`, `drinkSlowdown`, `ingredientLoot`,
  `diagnostics` (one log line per flask decision, for pack authors)
- `flasks.<tier>`: `maxCharges`, `healPercentage`, `rechargeTicks`, `drinkTicks`,
  `hitThreshold`, `potency` per built-in flask
- `ingredients.<name>`: `cost` and `strength` per built-in ingredient
- `recipes.<tier>`: one switch per built-in flask recipe

## Scope notes for pack authors

- The server decides everything: drink start, interrupts, completion, healing, charges,
  recharge, and ingredient effects. Clients only display.
- Equipping a flask empties it. Charges are only earned while it sits in the slot, so swapping
  flasks mid-combat cannot refill anything.
- Ingredients come from dungeon, mineshaft, and village blacksmith chests
  (`general.ingredientLoot` turns that off). They are not craftable.
- Add-ons can register their own flasks, ingredients, modifier bonuses, and HUD through
  `docs/API.md`; the public surface is the `api` packages only.
