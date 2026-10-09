# BaByDragons 1.67.A Verification

Local Java 25 `clean build`: **passed, 44 tests, zero failures/errors**.
JAR metadata and absence of purchased assets/bundled server APIs/tests were checked.
SHA-256 is recorded beside the packaged JAR in `downloads/`.

## Automated Coverage

Run `./gradlew clean build` with Java 25. Test reports are in
`build/reports/tests/test/` and `build/test-results/test/`.

- Movement step bounds, acceleration/deceleration, vertical progress, arrival,
  shortest-arc rotation and owner-facing without resting teleports.
- Galaxy target selection with one through six targets, four visual moments,
  lost/dead/protected targets, exactly one execute start, playback completion gate,
  delayed cooldown, survivor-only Levitation applied once, and cancellation cleanup.
- Fire entity-lock range, grace expiry/reacquisition, one lock sound per change,
  owned glow cleanup and preservation of foreign replacement glow.
- Slot-9 stick/sneak/right-click input; no AIR; no Fire cooldown despite legacy timestamps.
- Fire impact delay, 2-block box extents, player/mob damage parameters,
  temporary invulnerable nonpersistent crystal and cleanup.
- Independent assist/defense combinations and dragon-damage recursion guard.
- Immunity message, combat cancellation, negative cleanse, positive preservation and end.
- Return watchdog, owner world change and cancellation releasing the shared lock.
- Config limits, timeline validation and migration; schema-1 toggle migration to
  independent schema-2 fields; identity/seat/name/cooldown persistence and corrupt-file preservation.
- Existing naming, effect lease, shared attack gate and input regression tests.

Controller tests use Mockito against the actual Paper API. Registry doubles exist
only under `src/test`; neither they nor Paper/ModelEngine binaries are bundled.
These tests do not constitute a live Paper or ModelEngine test.

The adapter was checked against the author's [ModelEngine animation property API](https://ticxo.github.io/Model-Engine-4.0-JavaDocs/com/ticxo/modelengine/api/animation/property/IAnimationProperty.html)
and [blueprint animation API](https://ticxo.github.io/Model-Engine-4.0-JavaDocs/com/ticxo/modelengine/api/animation/BlueprintAnimation.html).

## Required Live Acceptance

No live Paper + ModelEngine + Minecraft client is available in this workspace.
Verify on a backed-up server with ModelEngine 4.1.1 and your licensed models:

1. Confirm blueprint IDs, default model scale, textures, name and recovery after restart.
2. Walk, sprint, turn and pause; confirm smooth travel, owner-facing and 3-block spacing.
3. Sit, attack and return; verify grounded static pose and exact saved seat/yaw.
4. Use `/dragonadmin animationstatus` during execute; verify the full clip renders once.
   Tune the four Galaxy timing points and Fire impact delay against the actual clips.
5. Test one, two, three, four and more targets; kill, teleport or protect a target
   midway. Expect all four visual moments and no lingering immunity or busy state.
6. Check armor/resistance, protection-plugin cancellations, friendly-fire teams,
   PvP disabled, pets and spectators. Check Levitation only on surviving hit entities.
7. Acquire Fire locks near 100 blocks, behind walls and at range boundaries; slip
   aim briefly, switch slots/items, disable Fire, logout and reload. Check glow cleanup.
8. Confirm stick + slot 9 + sneak + right-click, Fire overlay at 0.75 speed,
   moving-target accuracy, crystal timing, particles and configurable sounds.
9. Test crystal damage, explosions and ignition in the End; terrain must remain intact.
10. Interrupt approach, execute and return with logout/death/world change/removal/reload.
    Confirm safe recovery, no residual Fire model/crystal/glow, and immunity cleared.
11. Exercise all GUI buttons, item-transfer protections, effects, naming, pet and storage preview.

Rendering smoothness, installed API compatibility, clip timing and protection-plugin
interactions require this acceptance. Do not use Bukkit `/reload`; fully restart
for JAR installation and use the plugin's own config reload for settings.
