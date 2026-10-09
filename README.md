# BaByDragons 1.67.A - Final Phase 1 Patch

Plugin artifact: [BaByDragons 1.67.A.jar](downloads/BaByDragons%201.67.A.jar).

Requires Paper 26.3, Java 25 and ModelEngine 4.1.1. This patches the existing
Phase 1 plugin. Galaxy remains the permanent model; Fire is a temporary overlay.
Purchased models, textures, resource packs and ModelEngine binaries are not included.
No model scaling is applied. No MythicMobs, MCPets or Oraxen is required.

## Installation

1. Stop the server and back up `plugins/BaByDragons/`.
2. Replace all older BaByDragons plugin JARs with `BaByDragons 1.67.A.jar`.
3. Keep your existing licensed ModelEngine assets and working resource pack.
4. Fully start the server. Check blueprint validation and animation API linkage.
5. Use `/dragonadmin info` to confirm version, state, toggles and model playback.

Existing data migrates to schema 2: the old `autoAttackEnabled` value becomes both
`assistAttackEnabled` and `defenseAttackEnabled`. Identity, name, seat, effects and
manual toggle are preserved. The old manual cooldown is ignored. Missing config
fields are populated and the original config is backed up as
`config.before-1.67.A.yml`. Legacy configs move to a one-tick update interval.
Do not downgrade the migrated data to an older JAR without restoring your backup.

## Movement and Sitting

Controlled position steps use smooth acceleration, deceleration, shortest-arc yaw
and a small follow deadband. The dragon faces the owner even when resting within
follow range. Default follow distance is 3 blocks, follow speed 0.35 blocks/tick,
attack speed 0.65, rotation factor 0.20 and emergency catch-up distance 30 blocks.
Direct flight is not obstacle pathfinding and can cross blocks.

Sit grounds the controller and saves its exact position. An empty
`animations.sitting` stops Galaxy animations for a static pose. Attack return
travels back to the owner or saved seat; denied/stalled movement has a two-second
watchdog and a bounded emergency recovery path.

## Galaxy Combat

- **Combat Assist:** a successful owner hit starts a sequence.
- **Guardian Defense:** a successful incoming combat hit starts the same sequence.
- Both switches are independent and share one five-second Galaxy cooldown.
- One execute animation contains four visual strike moments, default ticks 8/16/24/32.
- Nearby targets within 2.5 blocks are selected with unused targets preferred.
- Target loss causes replacement or a visual strike at the last valid position.
- Damage per strike defaults to 4 raw health points for players and 20 for mobs.
- Successful-hit survivors receive Levitation once, after the full execute finishes.
- Cooldown begins after return completes. Another sequence cannot overlap.

Configured strike times must fit the installed execute clip at its configured
speed. The plugin reads the blueprint duration, forces one-shot playback and
checks animation completion before normal movement resumes. Invalid timing produces
an explicit error instead of shortening the animation. Target loss does not cancel
execute; owner logout/death/world change, disable and explicit cancellation do.

Dragon Immunity protects the owner from entity/projectile combat damage during
Galaxy approach, execute and return. It cleanses only configured negative effects
at the beginning and is cleared on all completion/cancellation paths. Environmental
damage is unaffected. Messages use the dragon's name.

## Inferno Strike

Hold a **stick in hotbar slot 9**, aim at a living entity within **100 blocks**, then
**sneak and right-click**. Air and block-only targeting no longer fire. Lock requires
line of sight; a 700 ms aim grace period stabilizes selection. Changing to a new
target plays a private lock sound. Glow is short-lived; foreign glow is preserved
when observable through potion-effect events. The outline is visible to other clients.

Fire has **no cooldown**, but must wait until the active attack and return finish.
The dragon travels above the locked entity, plays Fire execute at 0.75 speed, and
tracks the target until impact. Default impact delay is 20 ticks. A cosmetic beam,
temporary invulnerable crystal, particles and configurable sounds accompany impact.
The crystal is removed by the plugin, never detonated. Damage uses a 4 x 4 x 4 box:
at most 14 raw player damage and default 20 mob damage.

All damage uses Bukkit events, armor and protection handling. Owner, dragon parts,
armor stands, tamed pets, spectators, creative players and friendly-fire-protected
teammates are excluded. PvP must be enabled. Galaxy strikes clear vanilla hurt
invulnerability for each strike so eight-tick moments reach the damage event pipeline;
rejected damage restores the previous hurt timer. No health-setting or explosion API
is used. External protection plugins can cancel damage normally.

## Controls

The existing GUI, effects, rename, pet interaction, name display, storage preview,
persistence and restart recovery remain. GUI combat statuses refresh once per second.

| Command | Purpose |
| --- | --- |
| `/dragon` | Owner GUI |
| `/dragon sit`, `/dragon follow`, `/dragon summon` | Movement controls |
| `/dragon name <name> <color>` | Name and color |
| `/dragonadmin spawn`, `/dragonadmin remove` | Manage the single dragon |
| `/dragonadmin info`, `/dragonadmin movementdebug` | Movement, attack and lock diagnostics |
| `/dragonadmin animationstatus` | Model attachment, handler and clip playback |
| `/dragonadmin animation <idle|walk|pet|execute>` | Direct animation diagnostic |
| `/dragonadmin galaxytest <entity UUID or player name>` | Galaxy test using normal combat rules |
| `/dragonadmin firetest <entity UUID or player name>` | Fire test using normal combat rules |
| `/dragonadmin assist <on|off>`, `/dragonadmin defense <on|off>` | Independent Galaxy switches |
| `/dragonadmin manualattack <on|off>` | Inferno toggle |
| `/dragonadmin autoattack <on|off>` | Legacy alias changing both Galaxy switches |
| `/dragonadmin resetcooldowns`, `/dragonadmin reload` | Reset or validate/reload settings |

Diagnostic attack commands also accept the current Fire lock when no entity is
specified. They deal real game damage and respect toggles, permissions and cooldowns.
Owner permissions remain `babydragons.use`, `babydragons.name`, `babydragons.attack`;
`babydragons.admin` defaults to operators. Storage remains a locked preview.

## Build and Acceptance

With Java 25: `./gradlew clean build`. Output is `build/libs/BaByDragons 1.67.A.jar`.
GitHub Actions uploads artifact `BaByDragons-1.67.A` after a successful main-branch build.
The configured Paper API is resolved from Paper's Maven repository; optional local
API input remains available with `-PlocalPaperApi=/path/to/api.jar`.

See [TESTING.md](TESTING.md) for automated coverage and the live acceptance checklist.
Automated API doubles do not validate ModelEngine rendering or a real Minecraft client.
No Phase B features are implemented.
