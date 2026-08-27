# Changelog

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
