# BaByDragons 0.67.A verification

This report applies to the reconstructed build, not the lost earlier cloud artifact.

## Executed

- Java 25 compilation with Gradle 9.3.1 against unmodified official Paper 26.3 API source.
- **19 JUnit tests**, covering name/color validation and injection rejection; shared attack locking; independent toggles/cooldowns and exact expiry; failed/cancelled attacks; stale callbacks; YAML round trips for every persisted field; corrupt data preservation; invalid seat/cooldown/toggle/coordinate rejection; deletion; effect-ownership matching; manual input matrix; valid packaged settings and unsafe configuration rejection.
- ModelEngine public API signatures reviewed against the author's published ModelEngine 4 JavaDocs.
- Source inspection of GUI cancellation, crystal ignition/explosion guards, ownership, lifecycle cleanup and PDC recovery.
- JAR filename, required dependency metadata, entrypoint, Java 25 class version and absence of bundled APIs/purchased assets checked.
- Published JAR verified against a fresh Git checkout by SHA-256.

JUnit reports: `build/reports/tests/test/index.html`, `build/test-results/test/`. Source inspection is not runtime validation.

## Not executed

No real Paper/ModelEngine server or Minecraft client ran. Paper's download hosts are blocked in this environment, and the proprietary ModelEngine JAR/blueprints are absent. No claim of live compatibility, working visuals or exception-free server startup is made.

## Pending live acceptance

1. **Dependencies:** Without ModelEngine, Paper refuses the required dependency. With the installed engine, inspect version/API linkage and both IDs/animations. Wrong IDs log clear errors; correction and plugin reload recover.
2. **Identity:** Spawn twice, retaining one Vex/name/Galaxy model. Verify owner UUID and normal/non-owner/admin permission checks.
3. **Clicks:** Main-hand empty right-click opens control; sneak-right-click pets without GUI/attack; off-hand does not double-trigger.
4. **Inventory safety:** In both menus test top/bottom shift transfer, number keys, off-hand swap, collect, drag, drop, creative clone and rapid clicks. No GUI items can leave and no items can be stored.
5. **Names:** Named/hex/alias colors, Unicode and spaces render. Invalid/overlong/injected names or colors leave existing data intact.
6. **Movement:** Walk/sprint/turn/stop, sit, follow, summon, long-distance and cross-world following. Check default-size rendering, name offset and exact seat/yaw retention.
7. **Effects:** Strength II/Haste II/Absorption while following/enabled; sit/off/logout cleanup. Test beacon, stronger/same-level longer potion and other plugins. Spent absorption does not refill on refresh.
8. **Auto triggers:** Player, living mob and projectile shooter trigger; environment/self/cancelled/zero damage does not. Toggle independently from manual.
9. **Auto strike:** Approach, face, Galaxy execute, accepted damage 6 before reductions, 5-second Levitation I, breath radius 2.5. PvP/protection cancellation is respected. Only successful damage starts 30 seconds.
10. **Manual controls:** Wrong slot/item, nonowner, no permission/sneak, left/off-hand click or disabled toggle do nothing. Ninth slot + sneak + empty/stick + right-click hits blocks up to 64. Sky produces feedback only.
11. **Manual strike:** Approach, secondary Fire execute overlay, short invulnerable crystal, removal/pop, radius-2.5 custom damage 8 to eligible enemies. Galaxy remains the base. Empty-area visual strike consumes only manual cooldown.
12. **Terrain:** Strike near fragile blocks and in the End. No blocks change, ignite or explode. External damage/explosions cannot detonate tagged crystals.
13. **Lock/interruption:** Simultaneous attack attempts serialize. Return to following or exact saved seat. Interrupt every phase with logout/death/world change/removal/reload/disable/timeout/invalid target; no Fire model/crystal/lock remains.
14. **Restart:** Record IDs/name/color/state/seat/toggles/cooldowns. Restart in each state and during attacks. One canonical dragon returns; transient attacks do not resume. Missing parts rebuild; stale duplicates in later-loaded chunks are removed.
15. **Faults:** On a backup, corrupt YAML/schema/UUID/state/seat/toggle/timestamp and remove saved world. Recovery must stop without replacing invalid data. Restore originals.
16. **Admin/config:** Test all commands and mapped clips/tab completion. Remove/restart/reload old chunks: no old dragon returns. Invalid config reports cleanly. Monitor console for runtime errors throughout.

Use this plugin's config reload or a full server restart, not Bukkit `/reload`.
