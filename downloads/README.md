# BaByDragons Downloads

## Current: BaByDragons 0.67.A

**[Download BaByDragons 0.67.A.jar](https://github.com/ytwaffy-spec/WAFFYdragon/blob/main/downloads/BaByDragons%200.67.A.jar?raw=true)**

Paper 26.3, Java 25, ModelEngine 4 required. Use your existing working ModelEngine
resource pack and installed `cubee-galaxy_dragon` / `cubee-fire_dragon` blueprints.
Remove the old prototype JAR before installing this one, then run `/dragonadmin spawn`.

This reconstructed development build passes 19 automated logic tests. Live
Paper/ModelEngine/client behavior is not tested. See the [installation guide](../README.md)
and [test report](../TESTING.md). No purchased assets are included.

## Historical: BabyDragon 0.1.0 prototype

Paper 26.3 / Java 25 proof of concept. Both files are required to see the custom dragon:

- [Plugin JAR](https://github.com/ytwaffy-spec/WAFFYdragon/raw/refs/heads/main/downloads/BabyDragon-0.1.0.jar)
- [Resource-pack ZIP](https://github.com/ytwaffy-spec/WAFFYdragon/raw/refs/heads/main/downloads/BabyDragon-Resources.zip)

Put the JAR in the server's `plugins` folder and restart. Load the ZIP in a vanilla
Minecraft 26.3 client, or host it as a server resource pack. As an operator, run
`/dragonadmin testmodel`, then `/dragonadmin follow`.

The resource pack includes the actual supplied Baby Dragon model converted into
a static Java item model. Follow, sit, shoulder, naming, interaction, removal,
and persistence code are included. There are no articulated animation clips.

The Java 25 build, nine unit tests, conversion checks, and packaged-asset checks
passed. Live Paper server/client behavior has not been tested in this environment.

Resource-pack SHA-1: `c91254723fb2d682d4d34b344d38779e7b5894f2`.

Model: Baby Dragon by hussle101, CC BY 4.0. See [CREDITS.md](CREDITS.md) for the
original source and modifications. Attribution is also included inside the ZIP.
