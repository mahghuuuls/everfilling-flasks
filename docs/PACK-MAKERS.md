# Everfilling Flasks for pack makers

Since 1.3.0 the six flask stats are player attributes, so mods that add attribute modifiers by
attribute name can change a player's flasks. The three below were tested: item qualities, set
bonuses, and food bonuses. Everfilling Flasks adds no quality, set, or food of its own; this page
shows how to make yours.

## The six attributes

| Attribute | Starts at | What it changes |
| --- | --- | --- |
| `everfillingflasks.healing` | 1.0 | How much a drink heals |
| `everfillingflasks.drinkSpeed` | 1.0 | How fast a drink finishes |
| `everfillingflasks.hitThreshold` | 1.0 | How much damage it takes to interrupt a drink |
| `everfillingflasks.rechargeSpeed` | 1.0 | How fast charges refill |
| `everfillingflasks.maxCharges` | 0 | Extra charges, in whole charges |
| `everfillingflasks.effectPower` | 1.0 | How strong a flask's secondary effect is (add-on flasks and infusions use it; the built-in flasks have none) |

- The five stats that start at 1.0 are multipliers: 1.2 is +20 percent, 0.5 is -50 percent. They
  never go below 0.
- On those five, every vanilla operation means what it says on a player with no other modifier:
  "add 0.2", "multiply base by 0.2", and "multiply total by 0.2" are all +20 percent.
- Prefer "add" (operation 0). Several "add" modifiers simply sum: two +20 percent bonuses are
  +40 percent. Two "multiply total" bonuses of 0.2 compound to +44 percent, the way vanilla
  combines them.
- Max charges starts at 0 and takes "add" only: multiplying a base of 0 gives nothing, even
  though Quality Tools still shows such a bonus as a percent. It rounds to the nearest whole
  charge: 0.6 is one charge, 0.4 is none. A flask's maximum never drops below one charge.
- These bonuses add to infusions and to add-on bonuses: a +20 percent healing attribute and a
  +8 percent healing infusion heal 28 percent more.
- A change takes effect within about a second. A drink already under way keeps the values it
  started with. When a bonus to max charges ends, the flask's maximum drops back and a charge
  above the new maximum is lost, as with any other source.
- The names show in item tooltips as "Flask Healing", "Flask Drink Speed", "Flask Damage
  Threshold", "Flask Recharge Speed", "Flask Max Charges", and "Flask Effect Power".

## Quality Tools

[Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools) (it needs
[Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core)) reads quality files from
`config/qualitytools/Quailities/` (the folder name is spelled that way). A quality names the
attribute and an operation. Operations 1 and 2 show as a percent in the tooltip; operation 0
shows the raw amount. By default Quality Tools puts qualities only on items that do not stack
(its `allow stackable items` option changes that), and an item keeps the quality it first got unless it is reforged.
Quality Tools puts its modifiers back on its own every few ticks, so right after a respawn a
quality's bonus to max charges can be missing for a moment, and a full flask can lose that
charge.

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

Held in the main hand, this item reads "+50% Flask Healing" and "+1 Flask Max Charges", and the
flask heals 50 percent more with one more charge.

## Setworks

[Setworks](https://www.curseforge.com/minecraft/mc-mods/setworks) takes the attribute name as
written in a set bonus's `.attribute(...)`, with `add`, `multiply_base`, or `multiply_total`.
Scripts go in `scripts/` and need [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker)
4.1.20 or later. A bonus without a description shows the attribute's id in its generated tooltip
line; give it a description, as below.

```zenscript
#loader setworks
import mods.setworks.Setworks;

Setworks.armorSet("mypack:leather", "Leather Set",
    <minecraft:leather_helmet>, <minecraft:leather_chestplate>, <minecraft:leather_leggings>, <minecraft:leather_boots>);

Setworks.fullSetBonus("mypack:leather_flask", "mypack:leather", "Drinks twice as fast, +50% damage threshold")
    .attribute("everfillingflasks.drinkSpeed", 1.0, "add")
    .attribute("everfillingflasks.hitThreshold", 0.5, "add");
```

Wearing the full set, drinks take half as long and need 50 percent more damage to interrupt.

## Just Another Food System

[Just Another Food System](https://www.curseforge.com/minecraft/mc-mods/just-another-food-system)
1.0.0 or later accepts only the properties registered with it, so Everfilling Flasks registers
six food properties when it is installed:

| Property | Operation | Range |
| --- | --- | --- |
| `everfillingflasks:healing` | `percent` | -100 to 1000 |
| `everfillingflasks:drink_speed` | `percent` | -100 to 1000 |
| `everfillingflasks:hit_threshold` | `percent` | -100 to 1000 |
| `everfillingflasks:recharge_speed` | `percent` | -100 to 1000 |
| `everfillingflasks:max_charges` | `flat` | -10 to 10 |
| `everfillingflasks:effect_power` | `percent` | -100 to 1000 |

Percent amounts are whole percent: 20 is +20 percent. Max charges are whole charges; a fraction
rounds to the nearest one. Several active foods add up. The bonus ends when the food runs out
and comes back after a relog while the food is active. Death clears foods unless JAFS's
`keepFoodsOnDeath` is on; with it on, the bonus comes back after the respawn.

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

For an hour after eating (at JAFS's default timer speed), the flask heals 20 percent more,
refills twice as fast, and holds one more charge.

## Other mods

Another mod that adds attribute modifiers to players by attribute name can use these six
names the same way; only the three above were tested. Add-on authors can also reach the same stats through the code API
([API.md](API.md)).
