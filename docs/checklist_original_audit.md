# 원본 코드 전수 감사 체크리스트

> **목적**: original/ 리서치 파일을 처음부터 끝까지 읽고, 대응 구현부와 1:1 대조하여
> 오역·누락·버그를 발견 즉시 수정한다.
>
> **이 파일의 우선 규칙**: PORTING_RULES.md > 이 파일 > 개인 판단
>
> **전수 검증**: `find docs/research/original -name "*.md" | wc -l` = 96개
> REVIEW 53개 + SKIP 43개 = 96개 (전체 커버 확인됨)

---

## 감사 규칙 (PORTING_RULES.md 기반)

```
규칙 A. 전체 읽기 원칙
   리서치 파일은 처음부터 끝까지 전부 읽는다.
   대응 구현 파일도 처음부터 끝까지 전부 읽는다.
   중간에 건너뛰는 섹션이 있으면 그 섹션은 감사하지 않은 것이다.

규칙 B. 원본 근거 원칙 (PORTING_RULES §4 원칙 4)
   수정은 반드시 리서치 파일의 원본 코드 근거에 기반한다.
   "아마 이럴 것이다"로 수정하지 않는다.
   리서치 파일에 없는 동작은 수정 근거가 없다 → 리서치 파일을 먼저 보완한다.

규칙 C. 즉시 수정 원칙 (PORTING_RULES §5-2)
   불일치를 발견한 순간 그 자리에서 수정한다.
   "나중에 모아서" 수정하지 않는다.
   수정 후 컴파일 확인 (PORTING_RULES §6-1).

규칙 D. 불일치 3분류
   [오역]   구현이 원본과 다른 값/조건/로직을 사용한다
   [누락]   원본의 분기·상수·조건이 구현에 존재하지 않는다
   [잉여]   원본에 없는 로직이 구현에 추가되어 있다

규칙 E. 파일 완료 조건 (PORTING_RULES §6-2 적용)
   □ 리서치 파일의 모든 메서드/분기/상수를 구현과 대조했다
   □ 발견된 불일치를 전부 수정했다
   □ 수정 후 컴파일이 성공한다
   □ 새로 발견한 미구현 항목이 있으면 이 파일 하단 [신규 발견] 섹션에 기록했다

규칙 F. 세션 단위
   한 세션에 1~2개 파일. 컨텍스트 창 압박 방지.
   세션 끝에 반드시 커밋 (CLAUDE.md 규칙).

규칙 G. SKIP 파일 정의
   Fabric에서 대응 구현 자체가 없거나 순수 인프라인 파일.
   SKIP 파일도 이 목록에 명시적으로 기재한다 (누락 방지).
```

---

## REVIEW 목록 (53개) — 감사 필수

> 체크 형식: `[ ]` 감사 전 / `[x]` 감사 완료
> 경로 기준: `docs/research/original/smartmoving/` 또는 `docs/research/original/smartrender/`

---

### ★ 1순위 — 핵심 물리/이동 (6개)

| 상태 | 리서치 파일 (smartmoving/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [x] | `moving/SmartMovingContext.md` | `SmartMovingContext.java` |
| [ ] | `moving/SmartMovingBase.md` | `SmartMovingClimber.java`, `SmartMovingSwimmer.java`, `SmartMovingJumper.java`, `SmartMovingSlider.java`, `SmartMovingFlyer.java`, `MixinLivingEntityClient.java` |
| [ ] | `moving/SmartMovingSelf.md` | `SmartMovingClient.java`, `SmartMovingClientState.java`, `MixinLivingEntityClient.java` |
| [ ] | `playerapi/SmartMovingPlayerBase.md` | 위 구현 파일 전체 (PlayerAPI 훅 → Mixin 대응) |
| [ ] | `playerapi/SmartMovingSelf.md` | `SmartMovingClient.java`, `MixinLivingEntityClient.java` (moving/SmartMovingSelf.md와 별도 파일) |
| [ ] | `playerapi/SmartMovingServerPlayerBase.md` | `MixinLivingEntity.java`(서버측), `MixinPlayerEntity.java` |

---

### ★ 2순위 — 렌더/애니메이션 (20개)

#### smartmoving/render/ (9개)

| 상태 | 리서치 파일 (smartmoving/render/) | 대응 구현 파일 |
|------|----------------------------------|--------------|
| [ ] | `SmartMovingModel.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `SmartMovingRender.md` | `MixinPlayerEntityRendererClient.java` (또는 관련 렌더 Mixin) |
| [ ] | `ModelPlayer.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `RenderPlayer.md` | 렌더 관련 Mixin 전체 |
| [ ] | `SmartRenderContext.md` | `SmartMovingContext.java` 또는 별도 컨텍스트 클래스 |
| [ ] | `IModelPlayer.md` | 인터페이스 대응 확인 (구현 클래스 grep) |
| [ ] | `IRenderPlayer.md` | 인터페이스 대응 확인 |
| [ ] | `playerapi/SmartMovingModelPlayerBase.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `playerapi/SmartMovingRenderPlayerBase.md` | 렌더 관련 Mixin 전체 |

#### smartrender/ 렌더 핵심 (11개)

| 상태 | 리서치 파일 (smartrender/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [ ] | `SmartRenderModel.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `SmartRenderRender.md` | 렌더 Mixin |
| [ ] | `SmartRenderContext.md` | `SmartMovingContext.java` 또는 별도 |
| [ ] | `SmartRenderUtilities.md` | `SmartMovingJumper.java`(getHorizontalCollisionangle 등) |
| [ ] | `RendererData.md` | `SmartMovingClientState.java`(stats 필드) |
| [ ] | `ModelPlayer.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `RenderPlayer.md` | 렌더 관련 Mixin |
| [ ] | `IModelPlayer.md` | 인터페이스 대응 확인 |
| [ ] | `IRenderPlayer.md` | 인터페이스 대응 확인 |
| [ ] | `playerapi/SmartRenderModelPlayerBase.md` | `MixinPlayerEntityModelClient.java` |
| [ ] | `playerapi/SmartRenderRenderPlayerBase.md` | 렌더 Mixin |

---

### ★ 3순위 — SmartRender 통계 (7개)

| 상태 | 리서치 파일 (smartrender/statistics/) | 대응 구현 파일 |
|------|--------------------------------------|--------------|
| [ ] | `SmartStatistics.md` | `SmartMovingStats.java` 또는 `SmartMovingClientState.stats` |
| [ ] | `SmartStatisticsData.md` | 통계 필드 대응 |
| [ ] | `SmartStatisticsDatas.md` | 통계 필드 대응 |
| [ ] | `SmartStatisticsContext.md` | 통계 상수 |
| [ ] | `IEntityPlayerSP.md` | 통계 갱신 훅 대응 확인 |
| [ ] | `playerapi/SmartStatisticsPlayerBase.md` | 통계 갱신 훅 |
| [ ] | `../SmartRenderInfo.md` | 렌더 정보 대응 (smartrender/SmartRenderInfo.md) |

---

### ★ 3순위 — 특수 렌더 (4개)

| 상태 | 리서치 파일 (smartrender/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [ ] | `ModelCapeRenderer.md` | 망토 렌더 Mixin (있는 경우) |
| [ ] | `ModelEarsRenderer.md` | 귀 렌더 Mixin (있는 경우) |
| [ ] | `ModelRotationRenderer.md` | 회전 렌더 Mixin |
| [ ] | `ModelSpecialRenderer.md` | 특수 렌더 Mixin |

---

### ★ 3순위 — 지원 클래스 (11개)

| 상태 | 리서치 파일 (smartmoving/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [ ] | `moving/ClimbGap.md` | `climbing/ClimbGap.java` |
| [ ] | `moving/FeetClimbing.md` | `climbing/FeetClimbing.java` |
| [ ] | `moving/HandsClimbing.md` | `climbing/HandsClimbing.java` |
| [ ] | `moving/Button.md` | `SmartMovingKeys.java`, 키 처리 코드 |
| [ ] | `moving/Orientation.md` | 사용처 grep으로 확인 |
| [ ] | `moving/Compat.md` | Compat 관련 구현 (있는 경우) |
| [ ] | `moving/ISmartMovingClient.md` | 인터페이스 대응 확인 |
| [ ] | `moving/ISmartMovingSelf.md` | 인터페이스 대응 확인 |
| [ ] | `moving/SmartMovingOther.md` | 기타 유틸 대응 확인 |
| [ ] | `moving/SmartMovingClient.md` | `SmartMovingClient.java` |
| [ ] | `moving/SmartMovingServer.md` | 서버 측 Mixin |

---

### ★ 4순위 — 설정 (5개)

| 상태 | 리서치 파일 (smartmoving/config/) | 대응 구현 파일 |
|------|----------------------------------|--------------|
| [ ] | `SmartMovingConfig.md` | `SmartMovingConfig.java` |
| [ ] | `SmartMovingOptions.md` | `SmartMovingConfig.java` (Options → Config 통합) |
| [ ] | `SmartMovingClientConfig.md` | `SmartMovingConfig.java` |
| [ ] | `SmartMovingServerConfig.md` | 서버 설정 대응 (있는 경우) |
| [ ] | `SmartMovingServerOptions.md` | 서버 설정 대응 |

---

## SKIP 목록 (43개) — Fabric 대응 없는 인프라

> 이 파일들은 Forge/PlayerAPI 전용 인프라이거나 순수 유틸이라 대응 구현이 없다.
> 목록에 명시되어 있으므로 "빠진 것"이 아님.

### smartmoving/config/ (1개)
```
SmartMovingProperties.md  — config 속성 시스템 인프라 (Properties/Property/Value 레이어). Fabric에 불필요.
```

### smartmoving/core/ (8개)
```
SmartCorePlugin.md          — Forge ASM 플러그인 진입점. Fabric에 대응 없음.
SmartCoreTransformer.md     — 바이트코드 변환기. Mixin으로 완전 대체.
SmartCoreClassVisitor.md    — ASM ClassVisitor. 대응 없음.
SmartCoreMethodVisitor.md   — ASM MethodVisitor. 대응 없음.
SmartCoreTransformation.md  — 변환 정의. 대응 없음.
SmartCoreContainer.md       — 컨테이너. 대응 없음.
SmartCoreEventHandler.md    — Forge 이벤트 핸들러. FabricLoader로 대체.
SmartCoreInfo.md            — 모드 메타정보. fabric.mod.json으로 대체.
```

### smartmoving/moving/ (14개)
```
IEntityPlayerMP.md        — Forge 서버 플레이어 인터페이스. 불필요.
IEntityPlayerSP.md        — Forge 클라이언트 플레이어 인터페이스. 불필요.
ILocalUserNameProvider.md — 유저명 유틸 인터페이스. 불필요.
IPacketReceiver.md        — 패킷 인터페이스. 불필요.
IPacketSender.md          — 패킷 인터페이스. 불필요.
LocalUserNameProvider.md  — 유저명 유틸. 불필요.
SmartMoving.md            — Forge 모드 진입점 클래스. FabricMod로 대체됨.
SmartMovingComm.md        — 통신 레이어 (Forge 패킷). Fabric 네트워킹으로 대체.
SmartMovingCoreEventHandler.md — Forge 코어 이벤트. 대응 없음.
SmartMovingFactory.md     — PlayerAPI 팩토리 패턴. 불필요.
SmartMovingInfo.md        — 모드 정보. 불필요.
SmartMovingInstall.md     — 설치 유틸. Fabric에 불필요.
SmartMovingMod.md         — Forge ModContainer. FabricMod로 대체됨.
SmartMovingPacketStream.md — Forge 패킷 스트림. Fabric 네트워킹으로 대체.
SmartMovingServerComm.md  — 서버 통신. 대응 없음.
```

### smartmoving/playerapi/ (2개)
```
SmartMoving.md         — PlayerAPI 등록 진입점. Mixin으로 대체.
SmartMovingFactory.md  — PlayerAPI 팩토리. 불필요.
```

### smartmoving/properties/ (3개)
```
Properties.md  — 설정 속성 시스템 인프라. 불필요.
Property.md    — 설정 속성 클래스. 불필요.
Value.md       — 속성값 래퍼. 불필요.
```

### smartmoving/render/playerapi/ (1개)
```
SmartMoving.md  — 렌더 PlayerAPI 등록 진입점. Mixin으로 대체.
```

### smartmoving/test/ (2개)
```
SmartMovingTestCommand.md  — 테스트 커맨드. 불필요.
SmartMovingTestMod.md      — 테스트 모드. 불필요.
```

### smartmoving/utilities/ (2개)
```
Name.md     — 리플렉션 유틸. 불필요.
Reflect.md  — 리플렉션 유틸. 불필요.
```

### smartrender/ (2개)
```
SmartRenderInstall.md  — 설치 유틸. 불필요.
SmartRenderMod.md      — Forge 모드 진입점. FabricMod로 대체됨.
```

### smartrender/playerapi/ (1개)
```
SmartRender.md  — PlayerAPI 등록. Mixin으로 대체.
```

### smartrender/statistics/ (4개)
```
SmartStatisticsFactory.md          — 팩토리 패턴. 불필요.
SmartStatisticsOther.md            — 유틸. 게임플레이 로직 없음.
playerapi/SmartStatistics.md       — PlayerAPI 등록. 불필요.
playerapi/SmartStatisticsFactory.md — PlayerAPI 팩토리. 불필요.
```

### smartrender/utilities/ (2개)
```
Name.md     — 리플렉션 유틸. 불필요.
Reflect.md  — 리플렉션 유틸. 불필요.
```

---

## 감사 진행 기록

> 각 파일 감사 완료 시 아래 형식으로 기록한다.

```
### [완료일] 파일명

발견한 불일치:
- [오역/누락/잉여] 섹션명: 내용 → 수정 내용

불일치 없음:
- 해당 없음 (또는 확인된 내용 메모)

신규 발견 미구현:
- 없음 (또는 발견 내용)
```

---

### [2026-04-22] moving/SmartMovingContext.md

대응 구현: SmartMovingClimber.java (상수), SmartMovingSwimmer.java, SmartMovingFlyer.java, MixinLivingEntityClient.java, MixinEntityClient.java

발견한 불일치:
- [누락] 상수 4종 미구현: `SwimCrawlWaterTopBorder`(0.65F), `SwimCrawlWaterMediumBorder`(0.6F), `SwimCrawlWaterBottomBorder`(0.55F), `SwimCrawlWaterMaxBorder`(1F) — 크롤링 중 수중 진입 시 swim 전환 로직 자체가 없음
- [누락] `HorizontalGroundDamping`(0.546F) — 지면 수평 감쇠 상수 미사용
- [누락] `HorizontalAirodynamicDamping`(0.999F) — 공기역학 감쇠 상수 미사용
- [누락] `SlideToHeadJumpingFallDistance`(0.05F) — 슬라이딩 중 낙하 시 헤드점프 전환 조건 로직 없음

오역 없음: 구현된 상수(클라이밍 속도 8종, HorizontalAirDamping, SwimSoundDistance)는 전부 정확한 값 사용

신규 발견 미구현:
- 위 누락 상수 4종의 로직은 SmartMovingSelf.handleSwimming() / handleSliding() 에 속함 → SmartMovingSelf.md 감사 시 함께 처리

---

## 신규 발견 항목 (감사 중 발견한 미구현)

> 감사 중 발견한 항목을 즉시 여기에 기록한다.

| 발견일 | 소스 파일 | 설명 | 처리 여부 |
|--------|----------|------|----------|
| 2026-04-22 | `SmartMovingContext.md` | `SwimCrawlWaterTopBorder`(0.65F) 등 4개 수경계 상수 — 크롤→수영 전환 로직 자체가 미구현. SmartMovingSelf 감사 시 처리 | 미처리 (SmartMovingSelf 감사 예정) |
| 2026-04-22 | `SmartMovingContext.md` | `HorizontalGroundDamping`(0.546F) — 지면 수평 감쇠 미구현. 사용처 없음 | 미처리 (SmartMovingSelf 감사 예정) |
| 2026-04-22 | `SmartMovingContext.md` | `HorizontalAirodynamicDamping`(0.999F) — 공기역학적 감쇠 미구현 | 미처리 (SmartMovingSelf 감사 예정) |
| 2026-04-22 | `SmartMovingContext.md` | `SlideToHeadJumpingFallDistance`(0.05F) — 슬라이드→헤드점프 전환 로직이 SmartMovingSlider에 없음 | 미처리 (SmartMovingSelf 감사 예정) |
