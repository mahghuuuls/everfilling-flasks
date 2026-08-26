# Changelog

## 1.0.0

First public release.

### Added

- A dedicated flask slot, opened from the Inventory Button Bar button, with the flask screen.
- Hold-to-drink healing: drinking takes time, slows walking, and is cancelled by releasing the
  key, opening a screen, or taking a solid hit (the hit threshold is configurable and can be
  raised by infusions and add-ons).
- Automatic recharge: flasks refill one charge at a time while equipped; the Inhibited mod's
  effect freezes recharge when that mod is installed.
- Three flasks: Humble (2 charges), Sturdy (3), Radiant (4), each crafted from the one below;
  every recipe has a config switch. All heal 33% of maximum health per drink by default.
- Equipping a flask empties it, so swapping flasks cannot refill charges mid-combat.
- The infusion grid: six slots on the flask itself with a potency budget of 10. Over-filling
  makes the flask unusable until pieces are removed.
- Four flask infusions found in dungeon, mineshaft, and village blacksmith chests: Sunpetal
  Leaf (+10% healing), Ironroot Sprig (+40% hit threshold), Quickmint Leaf (+20% drink speed),
  Second Wind Petal (regeneration after each completed drink). Chest loot can be turned off in
  the config.
- HUD charge icons that fill as the flask recharges, a centered cast bar with an interrupted
  flash, drinking animations in first and third person, and a completion burst with a chime.
- A starting flask granted once to each new player (configurable, can be disabled).
- A public add-on API: register flasks and infusions for your own items, contribute flask
  bonuses from your mod's conditions, replace the completion effects, recolor the HUD liquid,
  or replace the flask HUD entirely. See docs/API.md.
- An in-game journal, opened from a book button on the flask screen, with a Flasks section and
  an Infusions section. Every entry is built from the registration itself, so its numbers
  always match the config, its recipe comes from the recipe registry, and add-on content
  appears without the add-on shipping any journal data. Optional "Where to Find" lines can be
  replaced or hidden per item in the config. Drawn by Patchouli ROFL Edition.
- Configuration for every gameplay number, plus a diagnostics mode that logs one line per
  flask decision for pack authors and server owners.
