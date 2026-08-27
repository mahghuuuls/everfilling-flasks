# Everfilling Flasks — add-on API

Everything an add-on may touch lives in two packages:

- `com.mahghuuuls.everfillingflasks.api` — safe on both sides.
- `com.mahghuuuls.everfillingflasks.api.client` — client only. Never load these classes in
  code a dedicated server can reach; call them from your client proxy.

Everything else, including `api.internal`, is internal and may change between versions
without notice.

Add a dependency on `everfillingflasks` in your `@Mod` line if you require it, or use
`after:everfillingflasks` for a soft dependency.

## Registering your own Flask

Register your item — any `Item`, no inheritance, no casts — with a `FlaskDefinition`:

```java
FlaskApi.registerFlask(MY_ITEM, new FlaskDefinition() {
    @Override public int maxCharges(ItemStack stack, EntityPlayer player) { return 3; }
    @Override public float healPercentage(ItemStack stack, EntityPlayer player) { return 0.25F; }
    @Override public int rechargeTicks(ItemStack stack, EntityPlayer player) { return 1200; }
    @Override public int drinkTicks(ItemStack stack, EntityPlayer player) { return 30; }
    @Override public float hitThreshold(ItemStack stack, EntityPlayer player) { return 1.0F; }
});
```

- Every method receives the exact stack and player, so values may depend on NBT or the
  player. Keep them cheap and side-effect free: they are consulted whenever effective values
  are computed (never every tick).
- Values are bases. Player Flask modifiers and placed infusions are applied by the core
  afterwards; do not include them yourself.
- `healPercentage` 0 makes a pure-hook Flask; drinking works at any health.
- `potency(stack, player)` (default 10) is the infusion budget of your Flask's infusion
  grid; 0 means no infusion fits.
- `infusionSlots(stack)` (default 5) is how many slots that grid has, from 1 to 12. The stack
  alone decides, with no player, because a Flask's stored infusions are read where there is no
  player to ask. A value outside the range is clamped and your definition is named once in the
  log. Answer from the stack and nothing else: each side works the count out for itself, so a
  count that depends on world state or a server-only setting draws a grid on one side the other
  refuses. Never read the stack's own infusions from it — that read asks this method back.
- Slots and potency are independent, and that is the point: a Flask with ten slots and a budget
  of six plays differently from one with three slots and a budget of twelve.
- First registration per item wins. A duplicate is refused with a log line, never an
  exception.

### Completion behavior

- `onDrinkCompleted(stack, player)` runs on the logical server after the charge is spent and
  the healing applied. A thrown exception is caught and logged once per definition class; it
  cannot corrupt Flask or player state. Mutating the Flask's charges from the hook is
  unsupported: the completed drink is already committed, and anything a hook writes is
  treated like any other external write at the next state sync, not as part of the drink.
- `completionEffect` / `completionSound` (default `true`) keep or replace the core's
  completion burst and chime. Return `false` to disable one half, or play your own first and
  then return `false`. A throw is logged once and the default plays.

## Registering an infusion

Same idiom, one definition per item:

```java
FlaskApi.registerInfusion(MY_ITEM, new InfusionDefinition() {
    @Override public int potencyCost(ItemStack infusion) { return 2; }
    @Override public void contribute(ItemStack infusion, EntityPlayer player,
                                     FlaskBonuses bonuses) { bonuses.healing(0.25F); }
});
```

- The core provides the grid, cost accounting, the over-capacity unusable state,
  effective-value merging, and post-drink dispatch (`onDrinkCompleted(infusion, flask,
  player)`, once per placed piece). You describe only cost and effect.
- While the summed costs exceed the Flask's potency, the grid is inert: drinking refuses and
  no piece contributes.
- Contributions use the same `FlaskBonuses` accumulator as player modifiers: percentages of
  one kind, from every source, add together before multiplying the base.

### HUD appearance

Three more default methods let your Flask restyle the default HUD's charge icons without
replacing the whole HUD:

- `hudLiquidColor(stack, player)`: the liquid's 0xRRGGBB color; -1 keeps the built-in red.
- `hudGlassTexture(stack, player)`: your own glass icon (drawn at nine by nine points);
  null keeps the built-in.
- `hudLiquidTexture(stack, player)`: your own liquid layer, drawn over the glass and cropped
  bottom-up while a charge refills (keep your liquid pixels in rows 1-7 of 9, like the
  built-in mask, because the crop maps progress onto those rows). It is tinted by
  `hudLiquidColor`; with a custom layer,
  -1 means untinted. Null keeps the built-in white mask.

These are read on the client with the synced stack, at most once per frame. A throw is
logged once and the defaults draw from then on.

## Player Flask modifiers

```java
FlaskApi.registerModifierSource((player, bonuses) -> {
    if (/* your condition, e.g. your bauble is worn */) {
        bonuses.healing(0.5F);      // +50 percent healing
        bonuses.drinkSpeed(1.0F);   // drinks twice as fast
        bonuses.hitResistance(1.0F); // doubled hit threshold
        bonuses.rechargeSpeed(1.0F); // recharges twice as fast
        bonuses.maxCharges(1);      // one extra charge slot
    }
});
```

Sources are consulted when effective values are computed: at drink start, on charge changes,
on state sync, and about once per second otherwise. A source that throws stays registered,
is logged once, and only its own contribution is lost.

## Reading state

`FlaskApi.snapshot(player)` returns a read-only `FlaskSnapshot`: the equipped Flask (a
copy), charges, effective maximum, recharge progress and duration, the Inhibited pause flag,
drink progress, and the effective hit threshold.

- On the logical server: authoritative, for any player.
- On the client: the synced mirror, for the local player only; anyone else answers empty.
  On this path `drinkTicks()` is meaningful only while `drinking()` is true; the mirror
  carries a placeholder when idle.
- Queries (`isFlask`, `definition`, `snapshot`) answer safely before this mod initializes
  (no Flasks, empty snapshot); they never throw for timing.

## Replacing the HUD

Client only:

```java
FlaskHudApi.setRenderer((state, resolution, partialTicks) -> {
    // draw your HUD from the snapshot
});
```

- One slot, no layering: registering suppresses the default HUD entirely, including its cast
  bar and charge icons. The last registration wins with a log line; `null` restores the
  default.
- Your renderer runs once per frame during the game overlay with the local player's
  snapshot. If it throws, it is disabled for the rest of the session and the default stays
  suppressed — a broken HUD fails visibly rather than half-drawing.
- Known gap in 1.0: the snapshot does not carry the drink outcome, so a replacement cannot
  reproduce the default cast bar's "interrupted" flash — it can tell a drink stopped, not
  why. If you need this, ask; it is a candidate for a later snapshot field.

## The journal

The journal has three sections: Flasks, Infusions, and Items. Every Flask and every infusion you
register appears in the first two on its own. You
write no journal file, declare no category, supply no icon, and provide no ordering. Your entry
is built from the definition you already wrote: its name, its item icon, the values your
definition returns, and its crafting recipe if the recipe registry has one for that item.
Entries are listed alphabetically by the name the player sees. Nothing on a page names the mod
that added it, deliberately: the journal is written in the game's voice, not the modpack's.

### An item that is neither

The journal has a third section, Items, for anything that belongs to this ecosystem without
being a flask or an infusion — a vessel part, a brewing tool, whatever your add-on invents:

```java
FlaskApi.registerJournalItem(MY_ITEM, "mymod.journal.teainfuser");
```

Pass `null` for the text and the page is just the item. Presentation only: nothing about the
item changes, and an item that also behaves as an infusion registers that separately and appears
in both places. The core registers the Everlasting Seed here, which is what gives it a page.


One optional method enriches an entry. It exists on `FlaskDefinition` and on
`InfusionDefinition`, returns a language key, and returns `null` by default:

```java
@Override
public String journalText(ItemStack stack) {
    return "mymod.journal.emberflask.text";
}
```

Write whatever a player should be told beyond the item itself. Where the thing is normally found
is the usual answer, but it is not required to be — the entry is perfectly valid as the item
alone. Keep it separate in your head from the recipe: a recipe says how something can be made,
which is a different question and one the journal already answers on its own.

A pack author can replace your text, or hide it, per registry name in `journal.textOverrides` in
this mod's config, because a pack often changes where content comes from. Write the truth for
your own mod and let them correct it.

It is called while the journal is built, on the client, and a throw costs your entry alone.
Escape a literal percent sign in that translation as `%%`, because the line goes through
Minecraft's own formatter.

An infusion whose effect cannot be read off `FlaskBonuses` — one that acts after a drink, or
through a system this mod knows nothing about — can put it into words for its own tooltip:

```java
@Override
public ITextComponent effectDescription(ItemStack infusion) {
    return new TextComponentTranslation("mymod.emberdust.effect", secondsOfFire);
}
```

Return a translation component, not finished text. The built-in infusions use this for their
own tooltips, so one sentence serves wherever the effect is shown.

One contract note: building the journal also calls your definition's ordinary value methods
(`maxCharges`, `healPercentage`, `rechargeTicks`, `drinkTicks`, `hitThreshold`, `potency`,
`potencyCost`, `contribute`) **on the client**, with a bare stack of your item and a viewer that
may be null during startup. Gameplay still calls them only on the server. Answer for a plain
stack and do not require a server to be present, or your entry will be the only casualty.

The values shown come from the client's own configuration file. A dedicated server that changes
its configuration without shipping the same file to its players will have a journal that reads
the player's file rather than the server's. The HUD and the flask screen are unaffected: those
show the server's own numbers.

The journal is drawn by Patchouli ROFL Edition, which is a required dependency of this mod. You
do not depend on Patchouli yourself, and you never call it.

## Registration timing

Registrations are buffered: calls made before this mod's pre-initialization are applied when
it binds, so load order against this mod cannot matter. Registering later — your init or
even at runtime — also works; recognition is immediate.

## What is guaranteed, and what is not

Guaranteed: the two `api` packages' types and documented behavior; refusal-not-exception on
duplicate registration; isolation of your hooks, sources, infusions, and HUD (your failure
costs only your feature, logged once); server authority (nothing an add-on registers can
move gameplay decisions to the client).

Not guaranteed: anything outside `api`/`api.client` (including NBT layouts and network
formats); call frequency of definition methods beyond "not every tick"; HUD layering; hook
ordering between multiple systems beyond "Flask hook before infusion hooks".
