<div align="center">

# SmartMoving

**Fabric 1.21.1 port of the classic [SmartMoving](https://www.curseforge.com/minecraft/mc-mods/smart-moving) mod**

마인크래프트 캐릭터 무빙을 풍부하게 — 기어오르기, 엎드리기, 슬라이딩, 헤드점프, 수영, 벽 점프

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Loader_0.19.2%2B-DBB69B?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-All--Rights--Reserved-lightgrey?style=flat-square)](#-라이선스)

</div>

---

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 🧗 Climbing
블록 · 사다리 · 덩굴 · 울타리 · 철창 자체등반 + 천장 등반

### 🏃 Crawling
1칸 공간 진입 · 머리 회전 · 크롤 클라이밍 · 슬라이드 ↔ 크롤 전환

### 🛹 Sliding
Sneak 자체 슬라이딩 · 비행 착지 슬라이딩 · 전환 카메라 안정화

### 🦊 Head Jump
점프 + Sneak 발사형 점프 · 여우무빙 (= 자체 슬라이딩 + 헤드점프 연결)

</td>
<td width="50%" valign="top">

### 🌊 Swimming
Swim / Dive 상태 + 좌클릭 swing 애니메이션

### 🦘 Wall Jump
공중 벽 점프 + 식 순서 / 회전 보정

### 🌿 Vine Climbing
Weeping · Twisting vines 사다리 애니메이션

### 🔄 Multiplayer Sync
모든 상태 self ↔ server ↔ remote 동기화 (lerpPosAndRotation 평탄화 우회 포함)

</td>
</tr>
</table>

---

## ⚙️ Requirements

| | |
|---|---|
| **Minecraft** | `1.21.1` |
| **Mod Loader** | [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.2` 이상 |
| **Dependency** | [Fabric API](https://modrinth.com/mod/fabric-api) |
| **Java** | `21` |

---

## 📦 Installation

```
1. Fabric Loader 설치
2. Fabric API 를 mods/ 폴더에 추가
3. SmartMoving jar 를 mods/ 폴더에 추가
4. 마인크래프트 실행
```

> 💡 멀티플레이는 **서버 / 클라이언트 양쪽** 모두 설치 필요.

---

## 🛠️ Build

```bash
git clone https://github.com/View-Studio/SmartMoving.git
cd SmartMoving
./gradlew build
```

빌드 결과물 → `build/libs/`

### Development

```bash
./gradlew runClient    # 클라이언트 실행
./gradlew runServer    # 서버 실행
```

---

## 👥 Credits

<table>
<tr>
<td valign="top">

**Developer**
[View-Studio](https://github.com/View-Studio)
*1.21.1 Fabric Port*

</td>
<td valign="top">

**Original Author**
[Divisor](https://www.curseforge.com/minecraft/mc-mods/smart-moving)
*1.7.10 Forge*

</td>
<td valign="top">

**1.12.2 Port**
[doch2](https://github.com/doch2/SmartMovingReboot)
*Smart Moving Reboot*

</td>
</tr>
</table>

---

## 🐛 Bug Reports

[**GitHub Issues**](https://github.com/View-Studio/SmartMoving/issues) 에 보고해주세요.

재현 스텝 + 로그 첨부 시 빠른 처리가 가능합니다.

---

## 📜 라이선스

원본 SmartMoving 의 라이선스를 따릅니다.

<div align="center">

---

Made with ❤️ by [View-Studio](https://github.com/View-Studio)

</div>
