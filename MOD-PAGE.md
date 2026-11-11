<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

Rechargeable healing flasks in the spirit of souls-like games. A flask lives in its own slot,
refills itself over time, and is drunk by holding a key (R by default). Drinking takes time,
slows movement, and a solid hit cancels it. A journal comes with it, opened from the flask
screen.

Like Black Myth: Wukong, flasks are swappable and their properties change with infusions.
Infusions are not consumed. They stay in the flask and keep working, making the drink faster,
the healing larger, the drink harder to interrupt, or leaving an effect behind afterwards.

Flasks are built around Everlasting Seeds. You start with a Humble Flask; it leads to the Sturdy
and then the Radiant, each crafted from the one below.

The seeds and this mod's infusions come from wild plants in the Overworld, one kind of plant for
each item, and the journal says where each grows. Hit a plant in bloom to get its seed or
infusion. The stem left behind blooms again after a while; breaking the stem removes the plant for
good. The Everlasting Sprout glows faintly, easiest to see at night. Plants only appear in land
generated after this mod's 1.4.0 update, so in an older world, explore new land.

This is a base rather than a big content mod. It ships a few flasks and a few infusions, and an
API for add-ons to bring the variety.

Flask recharge freezes while the Inhibited effect is active. That is in there because I play with
[Combat Inhibited](https://www.curseforge.com/minecraft/mc-mods/combat-inhibited) myself. What
the mod looks for is the effect rather than that particular mod, so anything providing it works.
Combat Inhibited is not required, and the recharge freeze is off by default.

---

## Add-ons

Mods that build on Everfilling Flasks:
- [Flasks Additions](https://www.curseforge.com/minecraft/mc-mods/flask-additions): many new
  flasks, each crafted from the rare items of other mods and each healing in its own way
- [Elusive Infusions](https://www.curseforge.com/minecraft/mc-mods/elusive-infusions): infusions
  crafted from Elusive Flora plants, with after-drink effects, flask modifiers with trade-offs,
  and stronger Concentrated versions

## Integrations

The six flask stats are player attributes. Pack makers can raise or lower them with the mods
below. This mod adds no such bonus of its own, and none of these mods is required.

The attributes:
- `everfillingflasks.healing`, starts at 1.0: how much a drink heals
- `everfillingflasks.drinkSpeed`, starts at 1.0: how fast a drink finishes
- `everfillingflasks.hitThreshold`, starts at 1.0: how much damage it takes to interrupt a drink
- `everfillingflasks.rechargeSpeed`, starts at 1.0: how fast charges refill
- `everfillingflasks.maxCharges`, starts at 0: extra charges, in whole charges
- `everfillingflasks.effectPower`, starts at 1.0: how strong a flask's secondary effect is (add-on
  flasks and infusions use it; the built-in flasks have none)

The five that start at 1.0 are multipliers: adding 0.2 is +20 percent, adding -0.5 is -50
percent. Max charges takes "add" only and rounds to a whole charge. Tooltips name them "Flask
Healing", "Flask Drink Speed", "Flask Damage Threshold", "Flask Recharge Speed", "Flask Max
Charges", and "Flask Effect Power". The
[pack-maker guide](https://github.com/mahghuuuls/everfilling-flasks/blob/main/docs/PACK-MAKERS.md)
has the full rules. The examples below were tested as written.

### Quality Tools

[Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools) (needs
[Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core)): a quality file in
`config/qualitytools/Quailities/` (the folder name is spelled that way).

```json
{
  "slot": "mainhand",
  "whitelist": [ { "item": "minecraft:carrot_on_a_stick" } ],
  "blacklist": [],
  "qualities": [
    {
      "name": "Flask Blessed",
      "color": "aqua",
      "weight": 1,
      "attributes": [
        { "name": "everfillingflasks.healing", "amount": 0.5, "operation": 1 },
        { "name": "everfillingflasks.maxCharges", "amount": 1, "operation": 0 }
      ]
    }
  ]
}
```

Held in the main hand, the item reads "+50% Flask Healing" and "+1 Flask Max Charges", and the
flask heals 50 percent more with one more charge.

### Setworks

[Setworks](https://www.curseforge.com/minecraft/mc-mods/setworks) (needs
[CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker)): a script in
`scripts/`.

```
#loader setworks
import mods.setworks.Setworks;

Setworks.armorSet("mypack:leather", "Leather Set",
    <minecraft:leather_helmet>, <minecraft:leather_chestplate>, <minecraft:leather_leggings>, <minecraft:leather_boots>);

Setworks.fullSetBonus("mypack:leather_flask", "mypack:leather", "Drinks twice as fast, +50% damage threshold")
    .attribute("everfillingflasks.drinkSpeed", 1.0, "add")
    .attribute("everfillingflasks.hitThreshold", 0.5, "add");
```

Wearing the full set, drinks take half as long and need 50 percent more damage to interrupt.

### Just Another Food System

[Just Another Food System](https://www.curseforge.com/minecraft/mc-mods/just-another-food-system)
uses food properties instead of attribute names. With it installed, this mod adds six:
`everfillingflasks:healing`, `everfillingflasks:drink_speed`, `everfillingflasks:hit_threshold`,
`everfillingflasks:recharge_speed`, and `everfillingflasks:effect_power` take `percent` (20 is
+20 percent); `everfillingflasks:max_charges` takes `flat` whole charges. Food entries go in the
list in `config/jafs/foods.json`.

```json
[
  {
    "id": "golden_carrot",
    "match": [ { "item": "minecraft:golden_carrot" } ],
    "fullDuration": 3600,
    "modifiers": [
      { "property": "everfillingflasks:healing", "operation": "percent", "amount": 20 },
      { "property": "everfillingflasks:recharge_speed", "operation": "percent", "amount": 100 },
      { "property": "everfillingflasks:max_charges", "operation": "flat", "amount": 1 }
    ]
  }
]
```

For an hour after eating (at the default timer speed), the flask heals 20 percent more, refills
twice as fast, and holds one more charge.

### Statusworks

[Statusworks](https://www.curseforge.com/minecraft/mc-mods/statusworks) (needs
[CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker)): a script in
`scripts/`. Pools and marks can use the attributes too; the
[pack-maker guide](https://github.com/mahghuuuls/everfilling-flasks/blob/main/docs/PACK-MAKERS.md#statusworks)
shows how.

```
import mods.statusworks.Statuses;
import mods.statusworks.Presentation;

// Flask Blessing: up to 3 stacks, 5 minutes.
// Each stack: +25% flask healing.
// While active: +50% drink speed, +50% hit threshold, double recharge speed,
// one more charge, +20% effect power.
Statuses.define("mypack:flask_blessing")
    .limit(3)
    .defaultDuration(6000)
    .contributeAttribute("everfillingflasks.healing", "add", "per_stack", 0.25)
    .contributeAttribute("everfillingflasks.drinkSpeed", "multiply_base", "fixed", 0.5)
    .contributeAttribute("everfillingflasks.hitThreshold", "add", "fixed", 0.5)
    .contributeAttribute("everfillingflasks.rechargeSpeed", "multiply_total", "fixed", 1.0)
    .contributeAttribute("everfillingflasks.maxCharges", "add", "fixed", 1.0)
    .contributeAttribute("everfillingflasks.effectPower", "add", "fixed", 0.2)
    .presentation(Presentation.create()
        .name("Flask Blessing")
        .description("Flasks heal more, drink faster, and hold one more charge.")
        .iconItem("minecraft:glass_bottle", 0))
    .register();
```

Give it with `/statusworks status apply @p mypack:flask_blessing 1`. With one stack, the flask
drinks in two thirds of the time, heals 25 percent more, needs 50 percent more damage to
interrupt, refills twice as fast, and holds one more charge. Each extra stack adds another 25
percent healing, up to 3 stacks (+75 percent). On a player with no other bonus, the three
operations give the same result on the five stats that start at 1.0; the guide's attribute rules
explain how they combine with other bonuses. On a mob the status still applies, without the flask
bonuses and without an error.

## For modpack creators

The config file provides options for:
- Changing each flask's properties: charges, healing, recharge time, drink time, damage
  threshold, and how much infusion potency it holds
- Changing each infusion's potency cost and its strength
- Removing any of the recipes
- Changing the starting flask, or granting none at all
- Each wild plant: on or off, its chance per chunk, the biome types or biome names it grows in
  (biomes of other mods too), and how long its stem takes to bloom again. With the Everlasting
  Sprout off and chest loot off, flasks cannot be crafted unless another source provides seeds
- Putting the seeds and infusions in dungeon, mineshaft, and village blacksmith chests as well
  (off by default; a config file from 1.3.0 or earlier keeps its own setting)
- Dropping flasks on death instead of keeping them
- Changing how much drinking slows movement, or removing the slowdown
- Switching on the Inhibited link
- Replacing or hiding any journal entry's text, per item, for when a pack moves something
  elsewhere
- A diagnostics log of every flask decision with its reason and every wild plant placed, for
  tracking down reports

Everything is in `config/everfillingflasks.cfg`; changes need a restart. The
[README](https://github.com/mahghuuuls/everfilling-flasks#configuration) lists the keys.

## For mod developers

Using the API, add-ons can:
- Register their own flasks, infusions and items, which appear in the journal automatically with
  no journal work on the add-on's side
- Give a flask its own numbers, including how many infusion slots it carries, and its own liquid
  colour, its own HUD icons, or a replacement for the flask HUD entirely
- Alter the properties of any flask a player drinks, for example a bauble that speeds up drinking
  or increases healing
- Run their own effect when a drink completes, and replace the burst and chime that follow it
- Declare a heal that pays out over time, push a flask's refill forward on their own events,
  read what a drink actually did, and scale a secondary effect through the effect power bonus
- Mark infusions that refuse to share a grid, read the drink outcome from an infusion, and get
  the infusion tooltip written for them in the same style as the built-in ones
- Change the flask stats with ordinary attribute modifiers, as the integrations above do

Wild plants and the chest-loot option cover only this mod's seed and infusions; an add-on supplies
its own items. Full reference: the
[add-on API guide](https://github.com/mahghuuuls/everfilling-flasks/blob/main/docs/API.md).

## Installation

Install on both client and server.

Requires:
[Inventory Button Bar](https://www.curseforge.com/minecraft/mc-mods/inventory-button-bar), which
adds the inventory button that opens the flask screen, and
[Patchouli ROFL Edition](https://www.curseforge.com/minecraft/mc-mods/patchouli-rofl-edition),
which draws the journal. If the flask button does not appear in a pack with many bar buttons, move or
resize the bar in Inventory Button Bar's config.

Source and add-on API: [GitHub](https://github.com/mahghuuuls/everfilling-flasks)
