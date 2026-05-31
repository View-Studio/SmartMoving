# SmartMoving (Fabric 1.21.1)

원본 [SmartMoving (1.7.10 Forge)](https://www.curseforge.com/minecraft/mc-mods/smart-moving) 의 1:1 매핑 기반 1.21.1 Fabric 포팅. 기어오르기 / 엎드리기 / 슬라이딩 / 헤드점프 / 수영 등 캐릭터 무빙을 풍부하게 만드는 마인크래프트 모드입니다.

## 주요 기능

- **기어오르기 (Climbing)** — 블록 / 사다리 / 덩굴 / 울타리 / 철창 자체등반 + 천장 등반
- **엎드리기 (Crawling)** — 1칸 공간 진입, 머리 회전, 크롤 클라이밍, 슬라이드 ↔ 크롤 전환
- **슬라이딩 (Sliding)** — Sneak 자체 슬라이딩, 비행 착지 슬라이딩, 전환 카메라 안정화
- **헤드점프 (Head Jump)** — 점프 + Sneak 발사형 점프, 여우무빙 (= 자체 슬라이딩 + 헤드점프 연결)
- **수영 (Swimming)** — Swim / Dive 상태 + 좌클릭 swing 애니메이션
- **벽 점프 (Wall Jump)** — 공중 벽 점프 + 식 순서 / 회전 보정
- **늘어진/휘어진 덩굴 등반** — Weeping/Twisting vines 사다리 애니메이션
- **싱글 + 멀티 동기화** — 모든 상태 self ↔ server ↔ remote 동기화 (1.21.1 lerpPosAndRotation 평탄화 우회 포함)

## 환경

| 항목 | 버전 |
|------|------|
| Minecraft | 1.21.1 |
| Mod Loader | Fabric Loader 0.19.2 이상 |
| 필수 의존 | Fabric API |
| Java | 21 |

## 설치

1. [Fabric Loader](https://fabricmc.net/use/installer/) 설치
2. [Fabric API](https://modrinth.com/mod/fabric-api) 를 `mods/` 폴더에 추가
3. 본 모드의 빌드된 jar 를 `mods/` 폴더에 추가
4. 마인크래프트 실행

## 빌드

```bash
git clone https://github.com/View-Studio/SmartMoving.git
cd SmartMoving
./gradlew build
```

빌드된 jar 는 `build/libs/` 에 생성됩니다.

## 개발자 모드 실행

```bash
./gradlew runClient    # 클라이언트
./gradlew runServer    # 서버
```

## 호환성

- **싱글플레이 / 멀티플레이** 모두 지원
- 서버 측 모드 설치 필요 (멀티플레이 시 호스트와 클라이언트 양쪽 모두 설치)
- 다른 모드와의 호환성: 미검증. Mixin 충돌 가능성 있음 — issue 로 보고해주세요

## 작업 흐름

신규 기능 / 복잡 작업 시 본 저장소는 다음 흐름을 따릅니다:

1. **리서치** — 원본 1.7.10 소스 및 vanilla 1.21.1 분석
2. **리서치 문서** — `docs/research_<주제>.md`
3. **체크리스트** — `docs/checklist_<주제>.md`
4. **구현** — 1:1 매핑 우선, 차이 발생 시 vanilla disassembly 검증

자세한 규칙은 [CLAUDE.md](./CLAUDE.md) 참조.

## 라이선스

본 저장소는 원본 SmartMoving (by Divisor) 의 1.7.10 Forge 버전을 1.21.1 Fabric 으로 포팅한 것입니다. 원본 모드의 라이선스를 따릅니다.

## 개발자

- **Rudals** ([@Rudals](https://github.com/Rudals)) — 1.21.1 Fabric 포팅
- 이메일: 2153rud@gmail.com
- 저장소: [View-Studio/SmartMoving](https://github.com/View-Studio/SmartMoving)

## 원본 크레딧

- **원작자**: [Divisor](https://github.com/Divisor) (SmartMoving 1.7.10 Forge)
- **참고**: [SmartRender](https://www.curseforge.com/minecraft/mc-mods/smart-render) (애니메이션 시스템)

## 이슈 / 버그 보고

[GitHub Issues](https://github.com/View-Studio/SmartMoving/issues) 에 보고해주세요. 재현 가능한 스텝 + 로그 첨부 시 빠른 처리 가능합니다.
