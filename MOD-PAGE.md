<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

Rechargeable healing flasks in the spirit of souls-like games. A flask lives in its own slot,
refills itself over time, and is drunk by holding a key (R by default). Drinking takes time,
slows movement, and a solid hit cancels it. A journal comes with it, opened from the flask
screen.

Like Black Myth: Wukong, flasks are swappable and their properties change with infusions.
Infusions are not consumed. They stay in the flask and keep working, making the drink faster,
the healing larger, the drink harder to interrupt, or leaving an effect behind afterwards.

Flasks are built around Everlasting Seeds, found in dungeon chests, abandoned mineshafts, and
village blacksmiths. The infusions come from the same chests. The Humble Flask leads to the
Sturdy and then the Radiant, each crafted from the one below.

This is a base rather than a big content mod. It ships a few flasks and a few infusions, and an
API for add-ons to bring the variety.

Flask recharge freezes while the Inhibited effect is active. That is in there because I play with
[Combat Inhibited](https://www.curseforge.com/minecraft/mc-mods/combat-inhibited) myself. What
the mod looks for is the effect rather than that particular mod, so anything providing it works.
It is not required, and the link can be switched off in the config.

---

## For modpack creators

The config file provides options for:
- Changing each flask's properties: charges, healing, recharge time, drink time, damage
  threshold, and how much infusion potency it holds
- Changing each infusion's potency cost and its strength
- Removing any of the recipes
- Changing the starting flask, or granting none at all
- Taking the seeds and infusions out of chest loot, which leaves flasks uncraftable unless
  another source provides them
- Dropping flasks on death instead of keeping them
- Changing how much drinking slows movement, or removing the slowdown
- Switching off the Inhibited link
- Replacing or hiding any journal entry's text, per item, for when a pack moves something
  elsewhere
- A diagnostics log that records every flask decision and its reason, for tracking down reports

## For mod developers

Using the API, add-ons can:
- Register their own flasks, infusions and items, which appear in the journal automatically with
  no journal work on the add-on's side
- Give a flask its own numbers, including how many infusion slots it carries, and its own liquid
  colour, its own HUD icons, or a replacement for the flask HUD entirely
- Alter the properties of any flask a player drinks, for example a bauble that speeds up drinking
  or increases healing
- Run their own effect when a drink completes, and replace the burst and chime that follow it

## Installation

Install on both client and server.

Requires:
[Inventory Button Bar](https://www.curseforge.com/minecraft/mc-mods/inventory-button-bar), which
adds the inventory button that opens the flask screen, and
[Patchouli ROFL Edition](https://www.curseforge.com/minecraft/mc-mods/patchouli-rofl-edition),
which draws the journal.

Source and add-on API: [GitHub](https://github.com/mahghuuuls/everfilling-flasks)
