# BaByDragons 0.67.A — Phase 1

**[Download BaByDragons 0.67.A.jar](https://github.com/ytwaffy-spec/WAFFYdragon/blob/main/downloads/BaByDragons%200.67.A.jar?raw=true)**

Requires **Paper 26.3**, **Java 25**, and **ModelEngine 4**. The owner supplied `ModelEngine-4.1.1.jar` as the initial target; startup logs the installed version and checks the public API signatures.

This is a **reconstructed development build**. The original uncommitted cloud workspace became unavailable. Source was reconstructed from the retained specification and implementation details, rebuilt, and tested again. It is not claimed to be byte-identical to the lost JAR.

## Install

1. Stop your Paper 26.3 server running Java 25.
2. Remove the older `BabyDragon-0.1.0.jar` prototype if installed; it owns the same command names.
3. Keep your licensed ModelEngine installation, purchased blueprints and already-working resource pack.
4. Download the JAR above and put it in `plugins/`.
5. Start the server; check the ModelEngine version and blueprint validation messages.
6. Run `/dragonadmin spawn` as an operator. Repeating this recovers the same dragon and assigns the executing player as owner.

No resource pack is rebuilt or distributed for 0.67.A. The older pack under downloads belongs only to the 0.1.0 prototype. No MythicMobs, MCPets or Oraxen dependency is used. No model scaling, ItemDisplay conversion, shoulder mode or riding.

**Live server/client acceptance has not been performed.** Compilation and logic tests cannot establish ModelEngine compatibility or visual behavior. See [TESTING.md](TESTING.md) before live deployment.

## Models and ModelEngine

| Role | Initial blueprint ID | Required animations |
| --- | --- | --- |
| Galaxy base | `cubee-galaxy_dragon` | idle, walk, pet, execute |
| Fire overlay | `cubee-fire_dragon` | execute |

The owner supplied these IDs from `plugins/ModelEngine/blueprints/Cubees/v20 Dragons/`. Purchased blueprints and ModelEngine binaries are absent in this workspace, so the IDs were **not locally detected**. Runtime registry validation checks both IDs and mapped animations. Missing Galaxy prevents spawning/movement; missing Fire prevents manual attacks. Errors identify the ID/clip. Correct the installation/config and use `/dragonadmin reload`.

The adapter directly invokes the documented public ModelEngine 4 API through cached reflective linkage; it validates signatures at startup. It uses ModelEngineAPI, ModeledEntity, ActiveModel, AnimationHandler and BaseEntityInteractEvent. There are no command bridges, NMS, fabricated API stubs or bundled proprietary dependencies. Signatures were reviewed against [the author's public ModelEngine 4 JavaDocs](https://github.com/Ticxo/Model-Engine-4.0-JavaDocs). Actual installed 4.1.1 binary compatibility still needs testing.

Galaxy attaches to an invisible, silent, invulnerable Vex with AI, natural expiration, collision and native attacks disabled. A TextDisplay carries the literal name. The installed model keeps its default size. Direct velocity flight can pass through obstacles; it is not pathfinding.

**Fire approach: temporary full-model overlay.** Galaxy remains attached and retains the main hitbox while a secondary Fire ActiveModel plays execute. Fire is removed/destroyed on completion or cancellation. The purchased bone hierarchy was unavailable; this does not hide Fire body bones or use a VFX-only derived blueprint. Expect both models to overlap during the short effect. Tune timings on the actual server.

## Commands and permissions

| Command | Behavior |
| --- | --- |
| `/dragon`, `/dragon gui` | Owner control GUI |
| `/dragon sit` | Save seat, stop following, idle |
| `/dragon follow` | Follow owner |
| `/dragon summon` | Move the existing dragon near owner and follow |
| `/dragon name <name> <color>` | Literal colored name |
| `/dragon info` | IDs, state, models, toggles and cooldowns |
| `/dragonadmin spawn` | Create/recover exactly one; executing player becomes owner |
| `/dragonadmin remove` | Remove dragon and saved record |
| `/dragonadmin info` | Debug details |
| `/dragonadmin sit\|follow\|summon` | Control existing dragon |
| `/dragonadmin animation <idle\|walk\|pet\|execute>` | Cosmetic test (mapped clip names) |
| `/dragonadmin autoattack <on\|off>` | Automatic toggle |
| `/dragonadmin manualattack <on\|off>` | Independent manual toggle |
| `/dragonadmin resetcooldowns` | Reset both timestamps |
| `/dragonadmin reload` | Validate config, reconnect visuals |

`babydragons.use`, `babydragons.name`, `babydragons.attack` default true but normal controls still require ownership. `babydragons.admin` defaults to OP. Tab completion is included. Admin commands intentionally bypass ownership restrictions.

Names accept 1–32 Unicode letters/numbers, spaces, apostrophes, dots, underscores or hyphens. Examples: `/dragon name Nova purple`, `/dragon name Little Nova #B026FF`, `/dragon name Nova aqua`. Named colors, purple/pink aliases and hex RGB work. Player names are literal Adventure Components, never MiniMessage input.

## GUI and effects

Owner empty-main-hand right-click opens the 27-slot control GUI. Sneak-right-click plays pet, hearts and a quiet sound instead. Off-hand/duplicate events are ignored; non-owners receive an ownership message.

| Zero-based slot | Control |
| --- | --- |
| 10 | Effects toggle |
| 11 | Rename command help |
| 12 / 14 | Sit / follow |
| 15 / 16 | Independent auto / manual toggles |
| 22 | 54-slot storage preview |
| 26 | Close |

Both menus use custom holders and cancel top/bottom clicks, shift transfers, number keys, double-click collection, drops, drags and inventory transfers. Buttons run the following tick after owner/menu/permission revalidation. Storage is a locked placeholder with Back/Close controls; no valuable items can be inserted.

Following with effects enabled grants Strength II, Haste II and Absorption. Default refresh is every 40 ticks, with 160-tick lifetime. Ownership checks amplifier, expiry and flags; other providers' effects are preserved, even weaker ones. External changes relinquish ownership. Cleanup removes matching owned effects only. Refresh preserves spent absorption hearts. After a process crash, untracked effects expire naturally.

## Automatic Galaxy defense

Uncancelled positive damage to the owner from a living entity or living projectile shooter triggers retaliation when enabled, ready and valid. Self and environmental damage are ignored. Approach attacker, face it, play Galaxy execute, apply 6 Bukkit damage by default, Levitation I for 5 seconds, and dragon-breath particles over roughly 5×5.

PvP settings, friendly-fire teams, damage-event cancellation and owner/dragon/tamed-pet exclusion apply. The 30-second cooldown starts only after accepted positive damage. Return to following or the exact saved seat.

## Manual Inferno Strike

1. Select **hotbar slot 9**.
2. Hold **nothing or a stick** in the main hand.
3. **Sneak**.
4. Aim at a **block within 64 blocks**.
5. **Right-click**.

No block means feedback only: no movement or cooldown. On a valid strike the dragon approaches above the block, overlays Fire execute, briefly shows a controlled End Crystal, removes it, plays pop particles/sound and applies custom damage. Default damage 8, radius 2.5. Valid targets are hostile Enemy mobs and eligible PvP players, excluding the owner, dragon, armor stands and tamed pets.

The crystal is invulnerable, controlled and nonpersistent. It is **removed, never detonated**. No real explosion or block-mutation API is called. Handlers also cancel tagged crystal damage, explosion and End-dimension block ignition.

Manual cooldown is independent (default 30 seconds); a completed visual strike consumes it even with no enemies present. Slot-9 action-bar feedback runs at most once per second. Both attack types share a lock through approach, animation and return. Logout/death/world change/removal/reload/disable/timeout/invalid targets clean up temporary state and visuals.

## Configuration

Configuration: `plugins/BaByDragons/config.yml`. Messages: `messages.yml`. Trusted admin MiniMessage supports titles, common messages, rename help and optional `gui.<effects|rename|sit|follow|auto|manual|storage|close>.name` overrides. Descriptive lore is currently in Java.

| Setting | Purpose |
| --- | --- |
| `models.galaxy.id`, `models.fire.id` | Distinct installed blueprint IDs |
| `animations.idle/follow/pet/auto-attack/fire-attack` | Clip mappings |
| `animations.pet-ticks` | Pet override duration |
| `animations.execute-windup-ticks` | Delay from execute to impact/crystal |
| `animations.execute-recovery-ticks` | Pause before return |
| `movement.follow-distance` | Stop 2.5–4 blocks from owner; default 3 |
| `movement.teleport-distance` | Emergency catch-up threshold; cross-world following also teleports |
| `movement.attack-stop-distance` | Auto approach stop distance |
| `movement.update-ticks` | Movement interval, 1–5 ticks |
| `movement.speed-per-tick/attack-speed-per-tick` | Velocity caps in blocks/tick |
| `movement.attack-timeout-seconds` | Maximum sequence duration |
| `movement.name-height` | Name offset; does not resize model |
| `effects.<strength|haste|absorption>.enabled/amplifier` | Effect availability and zero-based level |
| `effects.refresh-ticks/duration-ticks` | Refresh/lifetime; lifetime must exceed twice refresh |
| `auto-attack.enabled-by-default/manual-attack.enabled-by-default` | New dragon's initial toggles |
| `auto-attack.cooldown-seconds/damage` | Auto cooldown and damage |
| `auto-attack.levitation-seconds/levitation-amplifier/breath-radius` | Levitation and particle parameters |
| `manual-attack.target-range/cooldown-seconds/damage/radius` | Range (max 64), independent cooldown, damage sphere |
| `manual-attack.allowed-items` | AIR and/or STICK |
| `manual-attack.required-hotbar-slot` | Must remain 9 |
| `manual-attack.break-blocks` | Must remain false |
| `manual-attack.crystal-ticks` | Crystal display duration |
| `gui.title`, `storage.title` | Inventory titles |
| `storage.enabled` | Must remain false; preview is available |
| `persistence.save-interval-ticks` | Periodic save interval; default 200 |

Numeric settings are range-checked. Attack timings are server sequence durations, not measured purchased clip lengths.

## Persistence and restart

Atomic human-readable `plugins/BaByDragons/data.yml` stores dragon/owner/controller/name UUIDs, name/color, normal state, current location and saved seat, effects/auto/manual toggles, independent cooldown timestamps and base model ID. Commands, successful attacks and shutdown save immediately; periodic writes capture movement.

Recovery loads the saved chunk and finds PDC-tagged parts in loaded chunks, preferring saved IDs. It recreates missing parts, reconnects Galaxy and removes obvious plugin-tagged duplicates. Later chunk-load events clean stale parts; there are no per-tick world scans. One chunk ticket keeps the active dragon available. Malformed data is preserved and stops recovery. Missing saved worlds must be restored.

Attacks are transient; only the normal FOLLOWING/SITTING state is saved. A crash can lose up to the periodic interval of movement. Phase 1 supports one global dragon.

## Build

With Java 25 and access to Maven Central/Paper:

```sh
./gradlew clean build
```

Exact output: `build/libs/BaByDragons 0.67.A.jar`. Gradle 9.3.1 and its distribution checksum are pinned.

The cloud blocks Paper's Maven/server hosts. `bash tools/build-cloud.sh` compiles unmodified official Paper API source at `a4b87cf896e18419005ab172e8b1694ccc1abe7c` and official Mojang Brigadier at `9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98`. The ignored `.local/paper-api-source-build.jar` is compile/test input only. No API/server classes are bundled. This fallback is **not a runnable server**. Alternatively use `-PlocalPaperApi=/path/to/api.jar`.

Prepared cloud commands:

```sh
export JAVA_HOME=/workspace/toolchains/jdk-25.0.4.1+1
export GRADLE_USER_HOME=/workspace/gradle-cache
export JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
bash tools/build-cloud.sh
```

The proxy/CA option is cloud-specific; omit on ordinary machines.

## Acceptance and known limitations

Use a backed-up disposable server and follow [TESTING.md](TESTING.md). Test every admin command/animation, repeated spawn, ownership, GUI transfers, effect providers, auto/projectile/PvP protection, manual controls, crystal safety in the End, interruptions, exact seat return and restart identity. Use full stop/start, not Bukkit `/reload`.

Actual Paper/ModelEngine 4.1.1 startup, render appearance, flight physics, click routing, protection-plugin integration and live restart counts are unverified. Fire is a full temporary overlay. Direct flight can cross blocks. Name height and animation timing may need tuning. The JAR is published as a development build.

## Phase 2 / later TODO

Persistent storage; eggs/hatching; taming; loans/transfers; Enderman protection; multiple dragons; growth/hunger/breeding; shoulder/riding only if later requested; Bedrock renderer. None is implemented here.

## Asset policy

Only our source, tests, configuration/build files, Gradle wrapper and explicitly requested plugin download are published for 0.67.A. No purchased ModelEngine JAR, .bbmodel, textures, generated pack, vendor configuration or test-server files are uploaded. Earlier 0.1.0 prototype files and their attribution remain available as historical downloads.
