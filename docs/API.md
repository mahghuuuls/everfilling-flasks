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
- Values are bases. Player Flask modifiers and placed ingredients are applied by the core
  afterwards; do not include them yourself.
- `healPercentage` 0 makes a pure-hook Flask; drinking works at any health.
- `potency(stack, player)` (default 10) is the ingredient budget of your Flask's infusion
  grid; 0 means no ingredient fits.
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

## Registering an ingredient

Same idiom, one definition per item:

```java
FlaskApi.registerIngredient(MY_ITEM, new IngredientDefinition() {
    @Override public int potencyCost(ItemStack ingredient) { return 2; }
    @Override public void contribute(ItemStack ingredient, EntityPlayer player,
                                     FlaskBonuses bonuses) { bonuses.healing(0.25F); }
});
```

- The core provides the six-slot grid, cost accounting, the over-capacity unusable state,
  effective-value merging, and post-drink dispatch (`onDrinkCompleted(ingredient, flask,
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
  bottom-up while a charge refills (keep your liquid pixels in rows 3-7 of 9, like the
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

## Registration timing

Registrations are buffered: calls made before this mod's pre-initialization are applied when
it binds, so load order against this mod cannot matter. Registering later — your init or
even at runtime — also works; recognition is immediate.

## What is guaranteed, and what is not

Guaranteed: the two `api` packages' types and documented behavior; refusal-not-exception on
duplicate registration; isolation of your hooks, sources, ingredients, and HUD (your failure
costs only your feature, logged once); server authority (nothing an add-on registers can
move gameplay decisions to the client).

Not guaranteed: anything outside `api`/`api.client` (including NBT layouts and network
formats); call frequency of definition methods beyond "not every tick"; HUD layering; hook
ordering between multiple systems beyond "Flask hook before ingredient hooks".
