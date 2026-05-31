# SmartMoving

> [English](README.md) | [한국어](README.ko.md)

A Minecraft mod that enriches character movement.

Fabric 1.21.1 port of the classic [SmartMoving](https://www.curseforge.com/minecraft/mc-mods/smart-moving) (1.7.10 Forge).

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2%2B-DBB69B?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square)
![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)

## Features

- **Climbing** — Block / ladder / vine / fence / iron bar self-climbing + ceiling climbing
- **Crawling** — 1-block-tall space entry, head rotation, crawl-climbing, slide ↔ crawl transitions
- **Sliding** — Sneak-launched sliding, post-flight landing slide, transition camera stabilization
- **Head Jump** — Jump + sneak launched jump, fox movement
- **Swimming** — Swim / dive states + left-click swing animation
- **Wall Jump** — Mid-air wall jump with motion order / rotation correction
- **Vine Climbing** — Weeping / twisting vines ladder animation
- **Multiplayer Sync** — Full self ↔ server ↔ remote state synchronization

## Requirements

- Minecraft `1.21.1`
- [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.2+`
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java `21`

## Installation

1. Install Fabric Loader
2. Add Fabric API to the `mods/` folder
3. Add the SmartMoving jar to the `mods/` folder
4. Launch Minecraft

For multiplayer, both server and client must have the mod installed.

## Build

```bash
git clone https://github.com/View-Studio/SmartMoving.git
cd SmartMoving
./gradlew build
```

The built jar will be located in `build/libs/`.

```bash
./gradlew runClient   # Run client
./gradlew runServer   # Run server
```

## Credits

| Version | Loader | Maintainer |
|---------|--------|------------|
| 1.21.1  | Fabric | [View-Studio](https://github.com/View-Studio) |
| 1.12.2  | Forge  | [doch2](https://github.com/doch2/SmartMovingReboot) |
| 1.7.10  | Forge  | [Divisor](https://www.curseforge.com/minecraft/mc-mods/smart-moving) *(original)* |

## Support ☕

If SmartMoving has been useful, a small tip helps fuel the next project.

| Platform | Audience  | Link |
|----------|-----------|------|
| Ko-fi    | Worldwide | [ko-fi.com/viewstudio](https://ko-fi.com/viewstudio) |

## Bug Reports

Please report bugs on [GitHub Issues](https://github.com/View-Studio/SmartMoving/issues). Including reproduction steps and logs helps with faster resolution.

## License

[GPL-3.0-or-later](LICENSE) — inherits the original SmartMoving license.
