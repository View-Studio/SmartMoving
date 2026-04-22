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
| [x] | `moving/SmartMovingBase.md` | `SmartMovingClimber.java`, `SmartMovingSwimmer.java`, `SmartMovingJumper.java`, `SmartMovingSlider.java`, `SmartMovingFlyer.java`, `MixinLivingEntityClient.java` |
| [x] | `moving/SmartMovingSelf.md` | `SmartMovingClient.java`, `SmartMovingClientState.java`, `MixinLivingEntityClient.java` |
| [x] | `playerapi/SmartMovingPlayerBase.md` | 위 구현 파일 전체 (PlayerAPI 훅 → Mixin 대응) |
| [x] | `playerapi/SmartMovingSelf.md` | `SmartMovingClient.java`, `MixinLivingEntityClient.java` (moving/SmartMovingSelf.md와 별도 파일) |
| [x] | `playerapi/SmartMovingServerPlayerBase.md` | `MixinLivingEntity.java`(서버측), `MixinPlayerEntity.java` |

---

### ★ 2순위 — 렌더/애니메이션 (20개)

#### smartmoving/render/ (9개)

| 상태 | 리서치 파일 (smartmoving/render/) | 대응 구현 파일 |
|------|----------------------------------|--------------|
| [x] | `SmartMovingModel.md` | `MixinPlayerEntityModelClient.java` |
| [x] | `SmartMovingRender.md` | `MixinPlayerEntityRenderer.java`, `SmartMovingHud.java` |
| [x] | `ModelPlayer.md` | `MixinPlayerEntityModelClient.java` |
| [x] | `RenderPlayer.md` | `MixinPlayerEntityRenderer.java`, `MixinPlayerEntityModelClient.java` |
| [x] | `SmartRenderContext.md` | N/A — 다층 갑옷 모델 레이어 스케일 상수 (1.21.1 해당 없음) |
| [x] | `IModelPlayer.md` | N/A — PlayerAPI super*() 위임 인터페이스. TAIL inject로 대체됨 |
| [x] | `IRenderPlayer.md` | N/A — PlayerAPI superRender*() 위임 인터페이스. Inject 패턴으로 대체됨 |
| [x] | `playerapi/SmartMovingModelPlayerBase.md` | N/A — PlayerAPI ModelBase + IModelPlayer 구현. TAIL inject로 대체됨 |
| [x] | `playerapi/SmartMovingRenderPlayerBase.md` | N/A — PlayerAPI RenderBase + IRenderPlayer 구현. Inject 패턴으로 대체됨 |

#### smartrender/ 렌더 핵심 (11개)

| 상태 | 리서치 파일 (smartrender/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [x] | `SmartRenderModel.md` | N/A — SR 전용 모델 계층 + vanilla 애니메이션 래퍼. TAIL inject로 대체됨 |
| [x] | `SmartRenderRender.md` | `SmartStatistics.java`, `MixinEntityClient.java`, 렌더 Mixin |
| [x] | `SmartRenderContext.md` | N/A — FML RenderingRegistry 렌더러 등록. Mixin inject로 대체됨 |
| [x] | `SmartRenderUtilities.md` | `SmartMovingJumper.java`(getHorizontalCollisionangle 등) |
| [x] | `RendererData.md` | N/A — bipedOuter fade/보간 시스템 전용 데이터 컨테이너. bipedOuter 없으므로 N/A |
| [x] | `ModelPlayer.md` | N/A — SmartRenderModel 위임 래퍼 + PlayerAPI 없는 경로 중개 클래스. SmartRenderModel N/A |
| [x] | `RenderPlayer.md` | N/A — SmartRenderRender 위임 래퍼 + PlayerAPI 없는 경로 중개 클래스. SmartRenderRender/3-layer model N/A |
| [x] | `IModelPlayer.md` | N/A — SmartRenderModel←→구현체 브릿지 인터페이스. SmartRenderModel N/A이므로 N/A |
| [x] | `IRenderPlayer.md` | N/A — SmartRenderRender←→구현체 브릿지 인터페이스. SmartRenderRender N/A이므로 N/A |
| [x] | `playerapi/SmartRenderModelPlayerBase.md` | N/A — PlayerAPI ModelPlayerBase + IModelPlayer 어댑터. PlayerAPI/SmartRenderModel 둘 다 N/A |
| [x] | `playerapi/SmartRenderRenderPlayerBase.md` | N/A — PlayerAPI RenderPlayerBase + IRenderPlayer 어댑터. PlayerAPI/SmartRenderRender 둘 다 N/A |

---

### ★ 3순위 — SmartRender 통계 (7개)

| 상태 | 리서치 파일 (smartrender/statistics/) | 대응 구현 파일 |
|------|--------------------------------------|--------------|
| [x] | `SmartStatistics.md` | `SmartMovingStats.java` 또는 `SmartMovingClientState.stats` |
| [x] | `SmartStatisticsData.md` | 통계 필드 대응 |
| [x] | `SmartStatisticsDatas.md` | 통계 필드 대응 |
| [x] | `SmartStatisticsContext.md` | 통계 상수 |
| [x] | `IEntityPlayerSP.md` | 통계 갱신 훅 대응 확인 |
| [x] | `playerapi/SmartStatisticsPlayerBase.md` | 통계 갱신 훅 |
| [x] | `../SmartRenderInfo.md` | 렌더 정보 대응 (smartrender/SmartRenderInfo.md) |

---

### ★ 3순위 — 특수 렌더 (4개)

| 상태 | 리서치 파일 (smartrender/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [x] | `ModelCapeRenderer.md` | 망토 렌더 Mixin (있는 경우) |
| [x] | `ModelEarsRenderer.md` | 귀 렌더 Mixin (있는 경우) |
| [x] | `ModelRotationRenderer.md` | 회전 렌더 Mixin |
| [x] | `ModelSpecialRenderer.md` | 특수 렌더 Mixin |

---

### ★ 3순위 — 지원 클래스 (11개)

| 상태 | 리서치 파일 (smartmoving/) | 대응 구현 파일 |
|------|--------------------------|--------------|
| [x] | `moving/ClimbGap.md` | `climbing/ClimbGap.java` |
| [x] | `moving/FeetClimbing.md` | `climbing/FeetClimbing.java` |
| [x] | `moving/HandsClimbing.md` | `climbing/HandsClimbing.java` |
| [x] | `moving/Button.md` | `SmartMovingKeys.java`, 키 처리 코드 |
| [x] | `moving/Orientation.md` | `SmartMovingClimber.java` (getOnLadderOrVine, handleClimbing) |
| [x] | `moving/Compat.md` | `SmartMovingClientState.tickEssential()`, `MixinLivingEntityClient.sm_travel_client` |
| [x] | `moving/ISmartMovingClient.md` | N/A — 외부 모드 플러그인 API. 1.21.1 해당 모드 없음 |
| [x] | `moving/ISmartMovingSelf.md` | N/A — 외부 모드 실시간 상태 API. SmartMovingClientState 필드 직접 접근으로 대체 |
| [x] | `moving/SmartMovingOther.md` | `SmartMovingClientState.processStatePacket()` |
| [x] | `moving/SmartMovingClient.md` | `SmartMovingClient.java` |
| [x] | `moving/SmartMovingServer.md` | 서버 측 Mixin |

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

### [2026-04-22] moving/SmartMovingBase.md

대응 구현: SmartMovingClimber.java, SmartMovingSwimmer.java, SmartMovingFlyer.java, SmartMovingMover.java, MixinLivingEntityClient.java

발견한 불일치:
- [오역] `SmartMovingSwimmer.moveFlying()` 방향벡터 공식 오류 → 즉시 수정
  - 버그: `dx = forward*cos - strafe*sin`, `dz = forward*sin + strafe*cos`
  - 원본: `dx = strafe*cos - forward*sin`, `dz = forward*cos + strafe*sin`
  - 수정: SmartMovingSwimmer.java L187-188 수정 완료
- [누락] `reverseHandleMaterialAcceleration()` 미구현 — 수영 중 물 흐름 +0.014D 역상쇄(-0.014D) 없음. SmartMovingSelf 감사 시 처리
- [누락] `correctOnUpdate(isSmall, reverseMaterialAcceleration)` 미구현 — 느린 이동(0.02<f<0.05) 시 renderYawOffset 보정 없음. SmartMovingSelf 감사 시 처리

오역 없음:
- `SmartMovingFlyer.moveFlying()` 5-arg 공식: `sqrt(sqrt(x²+z²) + y²)` 원본과 일치 ✓
- `getOnLadderOrVine()` 탐색 범위 (minY~py+1, isSmall→py-1 확장): 원본과 일치 ✓
- `handleCeilingClimbing()` jgap 기반 속도(0.04/0.08/0.12): 원본과 일치 ✓

신규 발견 미구현:
- `reverseHandleMaterialAcceleration`, `correctOnUpdate` → SmartMovingSelf.md 감사 시 처리

---

### [2026-04-22] moving/SmartMovingSelf.md

대응 구현: SmartMovingSwimmer.java, SmartMovingJumper.java, SmartMovingClimber.java, SmartMovingClient.java

발견한 불일치:
- [오역] `handleSwimming` swimming motionYDiff: 5단계 → 13단계 원본 테이블로 수정 (SmartMovingSwimmer.java L119-130)
  - 원본: 1.62~1.7D 구간에 7개 세부 단계(0.005→0.000625→0→0.000625→...)
  - 버그: `< 1.7 → 0D`, `< 1.8 → 0.01D` (너무 단순)
- [오역] `handleSwimming` dipping motionYDiff 조건 누락 (SmartMovingSwimmer.java L112-113)
  - 원본: offset < 1.0 → -0.02D, else → -0.01D
  - 버그: 항상 -0.02D 사용 (전형적 dipping 구간 1.0~1.4에서 부정확)
- [오역] `handleSwimming` diving diveDown 공식 부호 오류 (SmartMovingSwimmer.java L142)
  - 원본: `0.01 - 0.1 * speedFactor` (speedFactor=1 → -0.09D)
  - 버그: `-(0.01 + 0.1 * speedFactor)` (speedFactor=1 → -0.11D)

누락 기록:
- `handleWallJumping` fallDistance 체크 미구현 (wallUpJump/wallHeadJumpFallMaximumDistance)
- `handleWallJumping` wasCollidedHorizontally → WallUpSlide/WallHeadSlide(noVertical) 미구현
- `afterOnUpdate → correctOnUpdate` 호출 미구현 (renderYawOffset 보정 + reverseHandleMaterialAcceleration)

잉여 없음 (구현 전반적으로 원본 로직 준수)

신규 발견 미구현:
- 위 누락 항목들 → 별도 이슈로 관리

---

### [2026-04-22] playerapi/SmartMovingPlayerBase.md

대응 구현: MixinLivingEntityClient.java, MixinLivingEntity.java, MixinEntity.java, MixinEntityClient.java, MixinPlayerEntity.java, SmartMovingClient.java

발견한 불일치:
- 오역 없음: 구현된 훅들(beforeMoveEntity/afterMoveEntity → STEP_HEIGHT억제/복원, beforeOnUpdate/afterOnUpdate → tickEssential, jump → sm_jump, moveEntityWithHeading → sm_travel_client, isOnLadder → sm_isClimbing, updateEntityActionState → sm_jumpingFilter, isSneaking(서버) → MixinLivingEntity)은 원본과 정확히 대응

누락 기록 (6종):
- [누락] `canTriggerWalking` override 미구현 — 크롤링/클라이밍 중 발소리·발자국 억제. 원본: isCrawling || isClimbing → false 반환
- [누락] `isInsideOfMaterial` override 미구현 — isSwimming_sm/isDiving/isDipping 중 물속 판정 오버라이드. 원본: offset 기반 직접 판정
- [누락] `beforeSetPositionAndRotation` 미구현 — multiPlayerInitialized 카운터(0→5) 세팅. 멀티플레이어 초기화 완료 전 상태 패킷 무시에 사용
- [누락] `beforeOnLivingUpdate/afterOnLivingUpdate` 미구현 — flyWhileOnGround 처리(착지했지만 isFlying 유지 조건)
- [누락] 클라이언트 측 `isSneaking` override 미구현 — isSmall/isCrawling 중 crawlOverEdge 보호용 sneak 강제
- [누락] `getFOVMultiplier` override 미구현 — SM 비행/클라이밍 속도에 따른 FOV 배율 조정

신규 발견 미구현:
- 위 누락 6종 → 신규 발견 항목 테이블에 추가

---

### [2026-04-22] playerapi/SmartMovingSelf.md

대응 구현: SmartMovingClient.java (해당 없음)

불일치 없음:
- SPC(Single Player Commands) 1.7.10 전용 모드 호환용 `doFlyingAnimation()` override만 포함
- SPC는 1.21.1에 존재하지 않는 구 모드 → 파일 전체 N/A, 1:1 포팅 대상 아님

신규 발견 미구현: 없음

---

### [2026-04-23] moving/SmartMovingServer.md

대응 구현: SmartMovingServer.java, MixinServerPlayerEntity.java, MixinServerPlayNetworkHandler.java, MixinLivingEntity.java, MixinPlayerEntity.java, MixinEntity.java, SmartMoving.java

발견한 불일치:
- [잉여] `SmartMovingServer.processStatePacket()` — `isSliding = ((bits >> 22) & 1) != 0;` 잘못 추가됨
  - 원본 서버는 isSliding 미추출. bit 22는 angleJumpType LSB (클라이언트 전용)
  - 수정: 해당 라인 + `public boolean isSliding;` 필드 제거
- [오역] `SmartMovingServer.beforeAddMovingHungerBatch()` — hunger 조건 누락
  - 원본: `if(hunger != -1) disableAddExhaustion = true;` → vanilla 소진 허용(hunger=-1) vs SM override(hunger≥0) 구분
  - 구현: 조건 없이 항상 `disableAddExhaustion = true;` → hunger=-1(미수신)일 때도 차단
  - 수정: `if (hunger >= 0F) disableAddExhaustion = true;`

메서드 매핑 검증 (전체):
- `processStatePacket(bits 12,13,14,15,18,31,33)` → SmartMovingServer.processStatePacket ✓ (isSliding 잉여 제거 후)
- `setCrawling(boolean)` → SmartMovingServer.setCrawling (cooldown=10) ✓
- `setSmall(boolean)` → SmartMovingServer.setSmall (calculateDimensions 경유) ✓
- `initialize(player)` → SmartMovingServer.initialize (ConfigContent 패킷 전송) ✓
- `processConfigInfoPacket` → SmartMovingServer.processConfigInfoPacket ✓
- `processConfigChangePacket` → SmartMovingServer.processConfigChangePacket (ConfigChange S2C 반환) ✓
- `processSpeedChangePacket` → SmartMovingServer.processSpeedChangePacket (speedUser 검증) ✓
- `hasPermission` → SmartMovingServer.hasPermission (equals 교체) ✓
- `beforeAddMovingHungerBatch/afterAddMovingHungerBatch` → SmartMovingServer 메서드 ✓ (hunger 조건 수정 후)
- `applyFallDistanceReset` → SmartMovingServer.applyFallDistanceReset ✓
- State 릴레이 `mp.sendPacketToTrackedPlayers` → SmartMoving.java PlayerLookup.tracking 루프 ✓
- `beforeActivateBlockOrUseItem/afterActivateBlockOrUseItem` → forceIsSneaking 블록 상호작용 훅 미구현

신규 발견 미구현:
- [누락] `beforeActivateBlockOrUseItem` / `afterActivateBlockOrUseItem` — 블록 상호작용 시 `forceIsSneaking` 설정/해제 훅
  - 원본: 블록 활성화 전 `forceIsSneaking=true`, 후 `forceIsSneaking=null` 설정하여 MixinEntity.sm_isSneaking이 강제 반환
  - `forceIsSneaking` 필드는 선언·읽기 코드 존재, 쓰는 훅(activateBlock 전후)이 없음
  - 영향: 크롤링 중 블록 상호작용 시 isSneaking()이 올바르게 오버라이드되지 않을 수 있음

---

### [2026-04-23] playerapi/SmartMovingServerPlayerBase.md

대응 구현: MixinEntity.java, MixinLivingEntity.java, MixinPlayerEntity.java, MixinServerPlayerEntity.java, MixinServerPlayNetworkHandler.java, SmartMovingServer.java

발견한 불일치:
- 오역 없음: 전체 훅 매핑이 1.21.1 메커니즘으로 올바르게 대응됨

훅 매핑 검증 (전체):
- `beforeOnUpdate(crawlingCooldown--)` → MixinServerPlayerEntity.sm_beforeTick ✓
- `afterOnUpdate(resetFallDistance/FloatKick)` → MixinServerPlayNetworkHandler.sm_tick ✓
- `beforeOnLivingUpdate()` → SmartMovingServer에 해당 메서드 없음 (N/A, A-17에서도 내용 불명)
- `afterOnLivingUpdate(isSmall grab)` → MixinLivingEntity.sm_afterTickMovement ✓
- `isSneaking()` → MixinEntity.sm_isSneaking ✓
- `isEntityInsideOpaqueBlock()` → MixinLivingEntity.sm_isInsideWall (crawlingCooldown) ✓
- `addExhaustion()` → MixinPlayerEntity.sm_addExhaustion ✓
- `addMovementStat()` → MixinServerPlayerEntity.sm_beforeTravel/sm_afterTravel ✓ (1.21.1 travel 인라인 처리)
- `beforeUpdatePotionEffects/afterUpdatePotionEffects` → MixinLivingEntity.sm_beforeTickStatusEffects/sm_afterTickStatusEffects ✓ (역전 유지)
- `resetFallDistance()` → SmartMovingServer.applyFallDistanceReset (motionY=GENERIC_GRAVITY=0.08 의도적 대체) ✓
- `resetTicksForFloatKick()` → MixinServerPlayNetworkHandler floatingTicks=0 ✓
- `afterSetPosition/beforeIsPlayerSleeping` → 1.21.1 포즈 기반 AABB으로 대체 (별도 Mixin 불필요) ✓

신규 발견 미구현: 없음 (rigorously checked)

---

### [2026-04-23] render/SmartMovingModel.md

대응 구현: MixinPlayerEntityModelClient.java

발견한 불일치:
- [오역] vine MiddleGrab → UpGrab 전환 미구현 (SmartMovingModel L317-319)
  - 원본: `if(isHandsVineClimbing && handsClimbType == HandsClimbing.MiddleGrab) handsClimbType = HandsClimbing.UpGrab;`
  - 버그: vine 클라이밍 중 MiddleGrab(h=2,3)이어도 UpGrab 전환 없이 offset=-Quarter 사용
  - 수정: sm_animateClimbing 진입 시 `if (sm.isHandsVineClimbing && h >= 2 && h < 4) h = 4;` 추가
- [누락] isCrawlClimbing NoGrab + non-NoStep 보정 (SmartMovingModel L415-421)
  - 원본: `handsClimbType==NoGrab && feetClimbType!=NoStep` 시 torso.X=0.5F, head.X-=0.5F
  - 버그: 해당 조건 분기 자체 없음
  - 수정: isCrawlClimbing 블록 끝에 `if (h < 2 && feetOrd > 0) { body.pitch=0.5f; head.pitch-=0.5f; }` 추가
  - 1.21.1 제한: bipedPelvic.X-=0.5F, bipedTorso.rotationPointZ=-6.0F 대응 노드 없음 → 생략

불일치 없음 (주요 애니메이션 로직):
- 11가지 상태 if-else 체인 우선순위: 원본과 완전 일치 ✓
- 각도 상수 (Half/Quarter/…): 원본과 일치 ✓
- isClimbJump 팔 각도: 원본과 일치 ✓
- isCeilingClimbing 속도 임계값(0.12951545F): 원본과 일치 ✓
- isSwim 속도 구간(0.15679921F/0.52264464F): 원본과 일치 ✓
- isDive/isCrawl/isSlide/isFlying/isHeadJumping/isFalling 각도: 원본과 일치 ✓
- Factor()/Between()/Normalize() → smFactor()/clamp()/wrapDegrees(): 정확히 대응 ✓
- animateAngleJumping: 원본과 일치 ✓

신규 발견 미구현:
- isSwim: isGenericSneaking threshold (0.005 vs 0.015) 미적용 — 시각적 영향 미미
- isDive: isLevitate/isJump 상태 미추적 — SM 비행+점프 중 수직각 abs() 미적용 (희귀 케이스)
- isFeetVineClimbing + UpGrab 동시: vine "+=" vs 구현 "=" 차이 — 극히 희귀한 상태 조합

---

### [2026-04-23] render/SmartMovingRender.md

대응 구현: MixinPlayerEntityRenderer.java (rotatePlayer→sm_captureBodyYaw/sm_modifyBodyYaw/sm_setupTransforms, renderPlayerAt→sm_getPositionOffset), SmartMovingHud.java (renderGuiIngame)

발견한 불일치:
- [누락] sm_captureBodyYaw(rotatePlayer 대응) smActive 조건에서 `isFlying`, `isAngleJumping()` 누락
  - 원본 rotatePlayer() 조건: isFlying, isAngleJumping() 포함
  - 버그: 비행/각도점프 중 bodyYaw가 forwardRotation으로 강제되지 않음
  - 수정: smActive에 `|| sm.isFlying || sm.isAngleJumping()` 추가

잉여 기록 (기능 저하 없음, 원본 근거 없음):
- [잉여] sm_captureBodyYaw에 `sm.isCrawling` — 원본 rotatePlayer()에 isCrawling 단독 없음
- [잉여] sm_captureBodyYaw에 `sm.isRopeSliding` — 원본에 isSliding만 있고 isRopeSliding은 별도 상태

불일치 없음 (주요 렌더 로직):
- rotatePlayer() → sm_captureBodyYaw + sm_modifyBodyYaw: bodyYaw 강제 패턴 일치 ✓
- renderPlayerAt() → sm_getPositionOffset(): heightOffset Y 보정 일치 ✓
- sm_setupTransforms() X 기울기: isSwimming/isDiving/isSliding/isFlying/isHeadJumping 각도 원본 일치 ✓
- HUD Y 위치: height-49, 물/갑옷 조건 -10 일치 ✓
- HUD X 위치: jumpCharge=(width/2-91)+i*8, exhaustion=(width/2+90)-(i+1)*8 일치 ✓
- 점프 차지 바 max/ceil 공식 원본 일치 ✓

신규 발견 미구현:
- renderName() 미구현 — 타인 플레이어 이름 태그 높이 보정(heightOffset-1→d1-=0.2) + 크롤/스니킹 임시isSneaking 변경. 타인 플레이어 SM 상태 동기화 전체가 미구현인 상태에서 단독 구현 불가.
- HUD exhaustion bar minFitnessForAction/ToStartAction 아이콘 구분 미구현 — SmartMovingClientState에 maxExhaustionForAction/ToStartAction 필드 없어 원본의 5종 아이콘 구분 불가. 단순화된 2종 아이콘(full/half)으로 대체됨.

---

### [2026-04-23] render/ModelPlayer.md

대응 구현: MixinPlayerEntityModelClient.java

불일치 없음:
- PlayerAPI 없는 경로의 중개 클래스. 1.21.1에서 BipedEntityModel Mixin으로 완전 대체.
- animate*() → SmartMovingModel.animate*() 위임: setAngles() TAIL inject로 대응 ✓
- isStandard=true(vanilla 위임): inject에서 해당 SM 상태 없으면 아무것도 안 함 → vanilla 결과 유지 ✓
- isStandard=false(SM 커스텀): inject에서 직접 각도 설정 ✓
- superAnimate*() 경로: 불필요 (TAIL inject로 대체됨) ✓
- getMovingModel(): 불필요 (Mixin이 직접 SmartMovingClientState 접근) ✓
- factor 파라미터(모델 스케일): 1.21.1 setAngles() 파라미터에 없음 → N/A

신규 발견 미구현: 없음

---

### [2026-04-23] render/SmartRenderContext.md

대응 구현: N/A

불일치 없음:
- `SmartRenderContext` (net.smart.moving.render): `Scale=0`, `NoScaleStart=1`, `NoScaleEnd=2` 상수 3개만 정의
- 이 상수들은 1.7.10에서 다층 갑옷 모델 레이어(modelBipedMain/modelArmorChestplate/modelArmor)별 팔·다리 스케일 타입을 구분하는 데 사용됨
- 1.21.1에서는 단일 PlayerEntityModel만 존재하므로 다층 스케일 타입 구분 자체가 불필요
- `setArmScales()`/`setLegScales()` 메서드도 구현되지 않음 (SmartMovingModel.md 감사 시 이미 N/A 확인)
- 각도 상수(Half/Quarter/…)는 `net.smart.render.SmartRenderContext` (SmartRender 패키지)의 것이며, `MixinPlayerEntityModelClient.java` 47~55행에 HALF/QUARTER/EIGHTH/… 로 이미 구현됨
- 결론: Scale/NoScaleStart/NoScaleEnd → N/A (구조적 차이)

신규 발견 미구현: 없음

---

### [2026-04-23] render/playerapi/SmartMovingModelPlayerBase.md + render/playerapi/SmartMovingRenderPlayerBase.md

대응 구현: N/A

불일치 없음:
- `SmartMovingModelPlayerBase`: PlayerAPI `ModelPlayerBase` + `IModelPlayer` 구현체
  - `dynamicOverride*()` → SM animate 위임: 1.21.1 TAIL inject에서 SM이 직접 애니메이션 처리 ✓
  - `superAnimate*()` → `super.dynamic()` PlayerAPI 체인: TAIL inject에서 vanilla가 먼저 실행되므로 불필요 ✓
  - `@Deprecated` getter 16개 (SmartRenderModel 노드): SmartRenderModel 자체가 N/A → 이 getter도 N/A ✓
  - 지연 초기화 필드 `model`: Mixin에서는 SmartMovingClientState 직접 접근으로 대체 ✓
- `SmartMovingRenderPlayerBase`: PlayerAPI `RenderPlayerBase` + `IRenderPlayer` 구현체
  - `renderPlayer()` → `getRenderModel().renderPlayer()`: sm_captureBodyYaw/sm_setupTransforms으로 대체 ✓
  - `rotatePlayer()` → `getRenderModel().rotatePlayer()`: sm_modifyBodyYaw/sm_setupTransforms으로 대체 ✓
  - `renderPlayerSleep()` → `getRenderModel().renderPlayerAt()`: sm_getPositionOffset()으로 대체 ✓
  - `passSpecialRender()` → `getRenderModel().renderName()`: sm_renderLabel()으로 대체 ✓
  - `getPlayerModels()` / `getPlayerModel*()`: 단일 BipedEntityModel Mixin으로 대체, 다층 모델 없음 ✓
  - `isRenderedWithBodyTopAlwaysInAccelerateDirection()`: 외부 PlayerBase용, 1.21.1에 호출처 없음 ✓
- 결론: 두 파일 모두 N/A (PlayerAPI → Mixin 전환에 따른 구조적 소멸)

신규 발견 미구현: 없음

---

### [2026-04-23] render/IModelPlayer.md + render/IRenderPlayer.md

대응 구현: N/A

불일치 없음:
- 두 인터페이스 모두 1.7.10 PlayerAPI 기반 아키텍처의 브릿지 인터페이스
- `IModelPlayer`: `SmartMovingModel.imp` 타입 — isStandard 시 vanilla `super*()` 호출 위임
  - 1.21.1 대응: TAIL inject에서 vanilla setAngles()가 먼저 실행된 후 SM이 덮어씀 → 인터페이스 불필요
- `IRenderPlayer`: `SmartMovingRender.irp` 타입 — superRender*() / getPlayerModels*() 위임
  - 1.21.1 대응: inject 패턴으로 vanilla 메서드 직접 intercept → 인터페이스 불필요
  - 다층 모델 접근(getPlayerModelBipedMain/ArmorChestplate/Armor) → 1.21.1 단일 모델로 N/A
- 결론: 두 파일 모두 N/A (PlayerAPI → Mixin 전환에 따른 구조적 소멸)

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/SmartRenderRender.md

대응 구현: SmartStatistics.java (stats 계산), MixinEntityClient.java (calculate 호출), MixinPlayerEntityRenderer.java (bodyYaw override)

발견한 불일치:
- [오역] `currentVerticalAngle` 가드/공식 오류 (SmartStatistics.java)
  - 원본: `atan(yDiff/horizontalDistance)`, horizontalDistance==0 → NaN → `Quarter(π/2)`
  - 버그: `(distance > 1e-4) ? atan2(diffY, h) : 0f` — horizontalDistance=0, diffY<0(하강)이면 `-π/2` 반환
  - 영향: 크리에이티브 비행 중 순수 수직 하강 시 몸통 기울기 θ=π 발생 (180° 잘못 기울어짐)
  - 수정: `(horizontalDistance > 1e-4) ? atan2(diffY, h) : (float)(Math.PI/2f)` — 원본 Quarter 일치

불일치 없음 (나머지):
- `renderPlayer()` stats 전달 → `sm.stats.calculate()` via MixinEntityClient.move TAIL ✓
- `horizontalDistance`/`verticalDistance`/`distance` 공식: 원본과 일치 ✓
- `currentHorizontalAngle` 공식: atan2(x,z) vs 원본 -atan(x/z)+(z<0?π/2:0). 공식 다르지만 **animation에서 미사용** → N/A
- `currentCameraAngle`: 미계산 (항상 0), **animation에서 미사용** → N/A
- `rotatePlayer()` → `sm_captureBodyYaw`: SM 상태에서만 bodyYaw override (원본은 항상). bipedOuter 계층 없으므로 정상적 차이 ✓
- `drawFirstPersonHand()` / `renderSpecials()` / `before/afterHandleRotationFloat()` → N/A (1.21.1 별도 처리)
- `getPreviousRendererData()` (fade 시스템) → N/A (bipedOuter 없음)

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/SmartRenderModel.md

대응 구현: N/A

불일치 없음:
- SmartRenderModel은 두 역할을 담당: (1) SR 전용 계층 모델 구조, (2) vanilla 애니메이션 래퍼
- **SR 전용 모델 계층** (bipedOuter/Torso/Breast/Neck/Shoulder/Pelvic 등): 1.21.1 단일 BipedEntityModel로 이 구조 자체가 없음 → N/A
- **vanilla 애니메이션 메서드** (animateHeadRotation/ArmSwinging/Riding/ItemHolding/Working/Sneaking/Arms/BowAiming): TAIL inject 전에 vanilla BipedEntityModel.setAngles()가 먼저 실행하여 처리 → N/A
- **render()의 ignoreRender/renderIgnoreBase 패턴**: 1.21.1 단일 렌더 패스 → N/A
- **setRotationAngles()의 firstPerson/isInventory 조기 리턴**: 1.21.1 별도 처리 경로 → N/A
- **SM 특화 애니메이션 부분(isClimbing/isCrawling/…)**: SmartMovingModel.md 감사 시 이미 완전 검증됨
- DEG_TO_RAD = 1/RadiantToAngle 변환 상수: SmartMovingModel.md 감사 시 확인됨

신규 발견 미구현: 없음

---

### [2026-04-23] render/RenderPlayer.md

대응 구현: MixinPlayerEntityRenderer.java, MixinPlayerEntityModelClient.java

불일치 없음:
- PlayerAPI 없는 경로의 렌더 중개 클래스. 1.21.1에서 PlayerEntityRenderer Mixin으로 완전 대체.
- doRender → renderPlayer(): setAngles() TAIL inject (SmartMovingClientState 직접 읽음) ✓
- rotateCorpse → rotatePlayer(): setupTransforms() inject ✓
- renderLivingAt → renderPlayerAt(): getPositionOffset() inject ✓
- passSpecialRender → renderName(): renderLabelIfPresent() inject + MixinLivingEntityRenderer ✓
- createModel(): 불필요 (Mixin 직접 접근) ✓
- getPlayerModels() / allIModelPlayers: 불필요 ✓

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/SmartRenderUtilities.md

대응 구현: SmartMovingJumper.java (getHorizontalCollisionangle), MixinPlayerEntityModelClient.java (각도 상수), SmartMovingConfig.java (wallUpJumpOrthogonalTolerance)

발견한 불일치:
- [누락] `wallUpJumpOrthogonalTolerance` 미구현 — 원본: tolerance!=0 && `abs(aligned)<tolerance`일 때만 90° 스냅. 버그: 항상 90° 스냅(`Math.round(jumpAngle/90F)*90F`)
  - 원본 (`SmartMovingConfig.md` L436): `_wallUpJumpOrthogonalTolerance.defaults(5F)`
  - 수정: `SmartMovingConfig.java` — `wallUpJumpOrthogonalTolerance=5F` 필드+readFrom+writeTo 추가
  - 수정: `SmartMovingJumper.java` L448 — tolerance 체크 후 스냅 조건 추가

불일치 없음:
- 각도 상수 (Half/Quarter/Eighth/Sixteenth/Thirtytwoth/Sixtyfourth): MixinPlayerEntityModelClient.java에 올바르게 구현됨 ✓
- `getHorizontalCollisionangle()` 진리표(4방향 충돌 조합 → 벽 법선 각도): SmartMovingJumper.java L494-514과 완전 일치 ✓
- `getAngle(x,y)` → `atan2(-vel.x, vel.z)`: 수학적으로 동등, zero-velocity 체크도 포함 ✓

신규 발견 미구현: wallUpJumpOrthogonalTolerance (위 처리 완료)

---

### [2026-04-23] smartrender/RendererData.md

대응 구현: N/A

불일치 없음:
- `RendererData`는 `ModelRotationRenderer`의 fade/보간 시스템에서 `bipedOuter`의 이전 프레임 상태를 저장하는 순수 데이터 컨테이너
- 필드 10개: offsetX/Y/Z, rotateAngleX/Y/Z, rotationPointX/Y/Z, totalTime=Float.MIN_VALUE
- `totalTime=Float.MIN_VALUE` 초기화: 첫 프레임에 보간 스킵하는 안전 패턴
- `bipedOuter`가 1.21.1에 없으므로 fade 시스템 자체가 불필요 → 전체 N/A
- `SmartRenderRender.previousRendererData`(Map) / `SmartRenderModel.prevOuterRenderData` 모두 N/A
- 1.21.1에서 body X 기울기는 `sm_setupTransforms()` MatrixStack transform으로 직접 처리 (fade 없음)

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/ModelPlayer.md

대응 구현: N/A

불일치 없음:
- `ModelPlayer extends ModelBiped implements IModelPlayer` — PlayerAPI 없는 경로의 모델 중개 클래스
- 모든 render/setRotationAngles/animate* 메서드가 `SmartRenderModel`에 위임 → SmartRenderModel 자체가 N/A이므로 전체 N/A
- `initialize()`: vanilla biped* 필드를 SmartRender `ModelRotationRenderer` 파트로 교체 → ModelRotationRenderer N/A이므로 N/A
- getter 16개(getOuter/getTorso/getBreast/getNeck/getPelvic 등): SmartRender 전용 노드들 → 1.21.1 단일 PlayerEntityModel에 해당 노드 없음 → N/A
- `animateArmSwinging`/`animateSneaking` 등 11개 animate* 위임: vanilla TAIL inject에서 setAngles()로 처리 → N/A
- 결론: SmartRenderModel 의존 + bipedOuter 계열 → 전체 구조적 N/A

신규 발견 미구현: 없음

---

### [2026-04-23] moving/ISmartMovingClient.md

대응 구현: N/A

불일치 없음:
- `ISmartMovingClient`는 외부 1.7.10 모드가 SmartMoving 내부 데이터에 접근하는 플러그인 API 인터페이스
- `SmartMovingContext.Client` static 필드 타입 → 1.21.1에서 SmartMovingClientState per-player로 교체, Client singleton 없음
- 소진 API (`setMaximumExhaustionValue/getMaximumExhaustionValue/removeMaximumExhaustionValue`): 외부 모드 key-value 등록 시스템 → 1.21.1 해당 모드 없음 → N/A
- `getMaximumExhaustion()`: 원본 `Math.max(Config.getMaxExhaustion(), 외부등록값)` → 1.21.1 `cfg.climbExhaustionStop` 직접 사용 (외부 등록값 없으므로 동등) ✓
- `getMaximumUpJumpCharge/getMaximumHeadJumpCharge`: 점프 충전 최대값 query API → 외부 모드용 → N/A
- `setNativeUserInterfaceDrawing/getNativeUserInterfaceDrawing`: SM HUD 제어 → SmartMovingHud.register() 고정 → N/A
- 결론: 전체 N/A (외부 API 인터페이스, 1.21.1에서 해당 API를 사용하는 모드 없음)

신규 발견 미구현: 없음

---

### [2026-04-23] moving/Compat.md

대응 구현: `SmartMovingClientState.tickEssential()`, `MixinLivingEntityClient.sm_travel_client`

발견한 불일치:
- [누락] `isSpectator` 체크 누락 (tickEssential + sm_travel_client)
  - 원본: `isActive = !Compat.isBlockedByIncompatibility(sp)` → `isSpectator(sp)` = EtFuturum 스펙테이터 gameType ID 3
  - 1.21.1: vanilla 스펙테이터 → `player.isSpectator()` 사용
  - 버그: tickEssential에서 스펙테이터 시 resetState() 미호출, sm_travel_client에서 스펙테이터 flying 감쇠 잘못 적용
  - 수정: tickEssential `!cfg.enabled` 조건에 `|| player.isSpectator()` 추가, sm_travel_client 상단에 early return 추가
- [누락] `isElytraFlying` 체크 누락 (tickEssential + sm_travel_client)
  - 원본: `isElytraFlying(sp)` = EtFuturum Requiem `IElytraPlayer.etfu$isElytraFlying()`
  - 1.21.1: vanilla 엘리트라 → `player.isFallFlying()` 사용
  - 버그: 엘리트라 비행 중 resetState() 미호출, SM 이동 처리 개입 가능
  - 수정: 동일하게 `|| player.isFallFlying()` 추가

N/A (구조적 변환):
- `init()`, `isStarMinerGravitized()`, `isOnCuchazShip()`, 내부 클래스 3개: StarMiner/Ships/EtFuturum 모드 → 1.21.1 미존재, 전체 N/A
- `classExists()` 이중 검사: EtFuturum 버전별 클래스 존재 여부 확인 → N/A
- Compat 클래스 자체 없음 (기능 인라인으로 대체됨)

컴파일: BUILD SUCCESSFUL ✓

신규 발견 미구현: 없음

---

### [2026-04-23] moving/Button.md

대응 구현: `SmartMovingKeys.java`, 키 처리 코드 전체

불일치 없음:
- 원본 `Button`: LWJGL2 `Keyboard.isKeyDown(keyCode)` + `inGameHasFocus` + `allowUserInput` + StartPressed/StopPressed 에지 감지
- 1.21.1 대응: Fabric `KeyBinding.isPressed()` / `KeyBinding.wasPressed()` — 에지 감지(`wasPressed()`), 지속 상태(`isPressed()`)로 완전 대체
- grab/configToggle/speedIncrease/speedDecrease 4개 KeyBinding 등록 (`SmartMovingKeys.java`) ✓
- StopPressed 패턴: `SmartMovingJumper.java` L317-319에서 `!isPressed()` 조건으로 직접 처리됨 ✓
- LWJGL2 keyCode → GLFW 키코드 변환 (LCONTROL→GLFW_KEY_LEFT_CONTROL, F9→GLFW_KEY_F9 등) ✓
- `inGameHasFocus` / `allowUserInput` 조건: 1.21.1 Fabric에서 KeyBinding.isPressed()가 내부적으로 처리 → N/A ✓
- 결론: N/A (Button 래퍼 클래스 → Fabric KeyBinding API 직접 사용으로 구조적 대체)

신규 발견 미구현: 없음

---

### [2026-04-23] moving/Orientation.md

대응 구현: `SmartMovingClimber.java` (getOnLadderOrVine, handleClimbing 내 대각 탐색)

발견한 불일치:
- [오역] `handleClimbing()` 대각 탐색 vine 방향 판정 완전 반전 (L304-307)
  - 원본 근거: `Orientation.hasVineOrientation()` — NZ(서쪽 접근) → West face, PZ(동쪽 접근) → East face, ZP(남쪽 접근) → South face, ZN(북쪽 접근) → North face
  - 버그: d[0]>0(East 성분) → vine.WEST, d[0]<0(West 성분) → vine.EAST, d[1]>0(South 성분) → vine.NORTH, d[1]<0(North 성분) → vine.SOUTH (4방향과 반전)
  - 수정: vine.EAST/WEST/SOUTH/NORTH로 올바르게 교체 (4방향 탐색 L133-139과 일치하게)

불일치 없음:
- `getOnLadderOrVine()` 4방향 vine 판정 (L133-139): dir=NORTH→vine.NORTH, dir=SOUTH→vine.SOUTH, dir=EAST→vine.EAST, dir=WEST→vine.WEST ✓
- 사다리 방향 판정: `ladderFacing.getOpposite() == dir` ✓ (원본: 탐색 방향에서 붙은 사다리만)
- vine 뒤 solid 블록 체크: `bx+dir.getOffsetX()` = vine block 너머 블록 (vine face 뒤) ✓
- Orientation의 나머지 기능 (반-블록 단위 수직 위치, 펜스/트랩도어/계단/문, 호환 모드 블록)은 1.21.1에서 N/A:
  - 1.7.10 블록 메타데이터 → BlockState property로 완전 교체
  - BetterThanWolves/RopesPlus/ASGrapplingHook/LadderKit/Carpenter's Blocks 등 → 1.21.1 미존재
  - 반-블록 단위 정밀 수직 탐색 → Y 블록 단위 정수 탐색으로 단순화
  - Orientation 9방향 상수 → Direction enum (NORTH/SOUTH/EAST/WEST) 대체

컴파일: BUILD SUCCESSFUL ✓

신규 발견 미구현: 없음 (vine 방향 오역 수정 완료)

---

### [2026-04-23] moving/FeetClimbing.md + moving/HandsClimbing.md

대응 구현: `climbing/FeetClimbing.java`, `climbing/HandsClimbing.java`

발견한 불일치:
- [오역] `FeetClimbing.isUp()` — 원본: `this == SlowUpWithHoldWithoutHands || this == SlowUpWithSinkWithoutHands || this == FastUp` (ordinal≥4)
  - 버그: `this.ordinal() > BASE_HOLD.ordinal()` (ordinal>1 → BASE_WITH_HANDS/TOP_WITH_HANDS도 true)
  - 영향: 현재 feetClimbing은 NONE/SLOW_UP_WITH_HOLD_WITHOUT_HANDS만 사용되므로 기능 영향 없으나 논리 오류
  - 수정: `this == SLOW_UP_WITH_HOLD_WITHOUT_HANDS || this == SLOW_UP_WITH_SINK_WITHOUT_HANDS || this == FAST_UP`
- [누락] `FeetClimbing.max()` SkipGaps 조건 — 원본: `!SkipGaps` 조건으로 canStand/mustCrawl 합산 제어. 버그: 항상 OR 합산
  - 수정: `if (!otherGap.skipGaps)` 조건 블록 추가, state/direction만 교체 (canStand/mustCrawl/skipGaps 복사 안 함)
- [누락] `HandsClimbing.max()` SkipGaps 조건 — FeetClimbing과 동일한 패턴
  - 수정: 동일하게 SkipGaps 조건 추가

불일치 없음:
- `HandsClimbing.isUp()`: `ordinal() > BOTTOM_HOLD.ordinal()` → UP/FAST_UP만 true. 원본 `_value > 0` 과 동등 ✓
- `HandsClimbing.toUp()`/`toDown()`: BottomHold→Up, TopHold→Sink 전환 ✓
- `FeetClimbing.isRelevant()`, `isIndependentlyRelevant()` ✓
- int 상수: DownStep/NoStep → DOWN_STEP/NO_STEP, MiddleGrab/UpGrab/NoGrab → MIDDLE_GRAB/UP_GRAB/NO_GRAB ✓

컴파일: BUILD SUCCESSFUL ✓

신규 발견 미구현: 없음

---

### [2026-04-23] moving/ClimbGap.md

대응 구현: `climbing/ClimbGap.java`

불일치 없음:
- `Block Block` + `int Meta = -1` → `BlockState state` (null = 미설정) ✓ 1.21.1 BlockState 대응
- `boolean CanStand` → `canStand`, `boolean MustCrawl` → `mustCrawl`, `boolean SkipGaps` → `skipGaps` ✓
- `Orientation Direction` → `Direction direction` (타입 차이: 9방향→6방향 enum). direction 필드는 현재 설정만 되고 읽히지 않음(미사용). Orientation.md 감사 시 재검토.
- 생성자 `ClimbGap() { reset(); }` 없음 → Java 기본값으로 동등 ✓
- `copyFrom(ClimbGap other)` 추가: [잉여] 원본 없음. FeetClimbing/HandsClimbing.max()에서 실제 사용됨 → 수정 불필요

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/ 특수 렌더 4개 (ModelRotationRenderer.md + ModelSpecialRenderer.md + ModelCapeRenderer.md + ModelEarsRenderer.md)

대응 구현: N/A

불일치 없음 (4개 파일 일괄):
- **`ModelRotationRenderer`**: vanilla `ModelRenderer` 상속. GL11 displayList 기반 렌더, 6종 회전 순서(XYZ/XZY/YXZ/YZX/ZXY/ZYX), fade 보간(RendererData), `bipedOuter` 등 SmartRender 노드 전체의 기반 타입. 1.21.1은 MatrixStack 기반, displayList 없음, `SmartRenderModel` 노드(bipedOuter/Torso/Breast/Pelvic 등) 자체가 없음 → 전체 N/A
- **`ModelSpecialRenderer`**: `ModelRotationRenderer` 상속. `ignoreRender=true` 기본 잠금 + `beforeRender()/afterRender()` 토글 + `doPopPush` glPop/glPush 패턴. `ModelRotationRenderer` N/A이므로 N/A
- **`ModelCapeRenderer`**: `ModelSpecialRenderer` 상속. 망토 물리 시뮬레이션 — renderYawOffset 보간, 망토 위치 SRG 필드 6개, `outer.rotateAngleX`(bipedOuter) 참조, GL11 glRotatef 4회. `bipedOuter`/`bipedBreast` 노드 N/A. 1.21.1은 vanilla `CapeFeatureRenderer`가 자체 망토 물리 처리 → N/A
- **`ModelEarsRenderer`**: `ModelSpecialRenderer` 상속. 귀 렌더 — `_i` 카운터로 좌우 교대(±0.375F X오프셋), -0.375F Y이동, 1.333F 확대, GL11 glTranslatef/glScalef. `bipedHead`(as `ModelRotationRenderer`) N/A. `SmartRenderRender.renderSpecials()` 호출 경로 전체 N/A → N/A

신규 발견 미구현: 없음

---

### [2026-04-23] smartrender/statistics/ (7개 파일)

대응 구현: SmartStatistics.java, MixinEntityClient.java (calculate 호출), SmartMovingClientState.stats

발견한 불일치:
- [오역] `SmartStatistics.java` `currentSpeed/currentHorizontalSpeed/currentVerticalSpeed` EMA 스케일 오류
  - 원본 `SmartStatisticsData.calcualte()`: `distance *= 4F; legYaw += (distance - legYaw) * 0.4F`
  - 버그: `currentSpeed = (float) distance` (원시 거리값, 4배/EMA 없음)
  - 영향: `sm_setupTransforms/sm_animateFlying` 에서 `walkFactor = min(1, currentSpeed)` → 비행 몸통 기울기 4배 약화
  - 수정: `currentSpeed += ((float) distance * 4f - currentSpeed) * 0.4f` 형태로 3종 EMA 적용 (SmartStatistics.java 완료)

불일치 없음 (나머지):
- `SmartStatisticsData.md`: 링 버퍼 슬롯(horizontal/vertical/all) → 1.21.1 SmartStatistics 단일 객체로 통합 ✓
- `SmartStatisticsDatas.md`: 링 버퍼 컨테이너(10슬롯) + renderPartialTicks 보간 → 1.21.1 tick-based 단일 계산 ✓
- `SmartStatisticsContext.md`: `calculateHorizontalStats` flag + `onTickInGame()` → N/A (vanilla limbSwing* 덮어쓰기 불필요) ✓
- `IEntityPlayerSP.md`: `getStatistics()` 인터페이스 → SmartMovingClientState.stats 필드 직접 접근으로 대체 ✓
- `playerapi/SmartStatisticsPlayerBase.md`: `afterMoveEntityWithHeading` → MixinEntityClient.move TAIL inject ✓, `afterUpdateRidden` → N/A (ticksRiding 미구현이지만 해당 필드 1.21.1 미사용)
- `SmartRenderInfo.md`: FML @Mod 상수 홀더 → fabric.mod.json 대체 → N/A ✓

신규 발견 미구현:
- 없음 (SmartStatistics EMA 오역만 발견, 수정 완료)

---

### [2026-04-23] smartrender/RenderPlayer.md + IModelPlayer.md + IRenderPlayer.md + playerapi/SmartRenderModelPlayerBase.md + playerapi/SmartRenderRenderPlayerBase.md

대응 구현: N/A

불일치 없음 (5개 파일 일괄):
- `smartrender/RenderPlayer.md`: `RenderPlayer extends vanilla RenderPlayer implements IRenderPlayer`. doRender/rotateCorpse/preRenderCallback/handleRotationFloat/renderFirstPersonArm 전부 SmartRenderRender에 위임. SmartRenderRender + 3-layer model(main/chestplate/armor) 전체 N/A → 파일 N/A
- `smartrender/IModelPlayer.md`: SmartRenderModel↔구현체(ModelPlayer/SmartRenderModelPlayerBase) 브릿지 인터페이스. super* 3개 + getter 16개(getOuter/getTorso 등 SR 전용 노드) + animate* 11개. SmartRenderModel N/A → 인터페이스 전체 N/A
- `smartrender/IRenderPlayer.md`: SmartRenderRender↔구현체(RenderPlayer/SmartRenderRenderPlayerBase) 브릿지 인터페이스. createModel/initialize + super* 4개 + getter 6개(3-layer model 포함). SmartRenderRender + 3-layer model N/A → 인터페이스 전체 N/A
- `playerapi/SmartRenderModelPlayerBase.md`: `ModelPlayerBase + IModelPlayer` 어댑터. PlayerAPI dynamic dispatch 패턴(animate* → modelPlayerAPI.dynamic() → dynamicVirtual*). SmartRenderModel lazy init + initialize() biped* 교체. PlayerAPI/SmartRenderModel 둘 다 N/A
- `playerapi/SmartRenderRenderPlayerBase.md`: `RenderPlayerBase + IRenderPlayer` 어댑터. SmartRenderRender lazy init + createModel(기존 ModelBiped를 PlayerAPI 타입으로 캐스트). shadowSize=0.5F 하드코딩. getRenderModels() 참조 동등성 캐시. PlayerAPI/SmartRenderRender 둘 다 N/A
- 공통 근거: PlayerAPI(1.7.10 전용) + SmartRender 모델 계층(bipedOuter/ModelRotationRenderer/3-layer) → 1.21.1 Mixin inject 패턴으로 완전 대체. 구조적 소멸.

신규 발견 미구현: 없음

---

## 신규 발견 항목 (감사 중 발견한 미구현)

> 감사 중 발견한 항목을 즉시 여기에 기록한다.

| 발견일 | 소스 파일 | 설명 | 처리 여부 |
|--------|----------|------|----------|
| 2026-04-22 | `SmartMovingContext.md` | `SwimCrawlWaterTopBorder`(0.65F) 등 4개 수경계 상수 — 크롤→수영 전환 로직 자체가 미구현. | **처리 완료** — SmartMovingSelf.md R-06 보완(원본 handleSwimming 크롤링 분기 전체 수록); SmartMovingClientState: dippingDepth 필드 추가+리셋, mustCrawl에 canCrawl 게이트 적용; SmartMovingSwimmer: updateSwimState() 크롤링 isDipping 강제+dippingDepth 설정, handleSwimming() SwimCrawlWater 전환 로직 추가 (standupIfPossible→얕은물 계속크롤/깊은물 수영전환) |
| 2026-04-22 | `SmartMovingContext.md` | `HorizontalGroundDamping`(0.546F) — 지면 수평 감쇠. | **N/A** — 0.6(블록 마찰) × 0.91(HorizontalAirDamping) = 0.546F, vanilla 1.21.1이 동일 값 적용 |
| 2026-04-22 | `SmartMovingContext.md` | `HorizontalAirodynamicDamping`(0.999F) — 공기역학적 감쇠. | **처리 완료** — SmartMovingSelf.md R-05 보완; State: isAerodynamic 필드 추가; MixinLivingEntityClient.sm_aerodynamicDamping (travel TAIL, factor=0.999F/0.91F) |
| 2026-04-22 | `SmartMovingContext.md` | `SlideToHeadJumpingFallDistance`(0.05F) — 슬라이드→헤드점프 전환. | **처리 완료** — SmartMovingSelf.md R-05 보완; SmartMovingClientState.tickEssential(): fallDistance>0.05F 시 isSliding→isHeadJumping+isAerodynamic=true 전환 |
| 2026-04-22 | `SmartMovingBase.md` | `reverseHandleMaterialAcceleration()` — 수영 중 물 흐름 가속 역상쇄(-0.014D). | **N/A** — SM이 travel() cancel로 vanilla 수중 물리 자체를 차단, 역상쇄 불필요 |
| 2026-04-22 | `SmartMovingBase.md` | `correctOnUpdate(isSmall, reverseMaterialAcceleration)` — 느린 이동 시 renderYawOffset 보정. | **처리 완료** — MixinClientPlayerEntity.sm_correctOnUpdate (tickMovement TAIL). reverseMatAccel은 N/A |
| 2026-04-22 | `SmartMovingSelf.md` | swimming motionYDiff 5단계→13단계 수정, dipping offset 조건 수정, diving diveDown 부호 수정. SmartMovingSwimmer.java 완료 | 처리 완료 |
| 2026-04-22 | `SmartMovingSelf.md` | `handleWallJumping` fallDistance 체크 누락 — `cfg.wallUpJumpFallMaximumDistance / wallHeadJumpFallMaximumDistance` | **처리 완료** — Config: 2개 필드 추가(readFrom/writeTo 포함), SmartMovingJumper.handleWallJumping() fallDistance 조건 추가 |
| 2026-04-22 | `SmartMovingSelf.md` | `handleWallJumping` `wasCollidedHorizontally` → WallUpSlide/WallHeadSlide(noVertical) 미구현 | **처리 완료** — State: wasCollidedHorizontally 필드+캡처+리셋 추가; Jumper: WALL_UP_SLIDE/WALL_HEAD_SLIDE 상수, handleWallJumping 타입/각도 분기, tryJump noVertical 처리 |
| 2026-04-22 | `SmartMovingSelf.md` | `afterOnUpdate → correctOnUpdate` 호출: isSwimming/diving/dipping/crawling 시 renderYawOffset 보정 | **처리 완료** — (correctOnUpdate 항목과 동일, 위 참조) |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `canTriggerWalking` override 미구현 — `moving.canTriggerWalking()` 실제 구현이 SmartMovingBase.md/SmartMovingSelf.md에 없음 | **처리 완료** — SmartMovingSelf.md R-05 보완(행 1469-1471); MixinPlayerEntityClient.sm_canTriggerWalking (PlayerEntity.canTriggerWalking HEAD, isClimbing||isDiving → false) |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `isInsideOfMaterial` override — FiniteLiquid 모드 전용 코드, 1.21.1 N/A. localIsInsideOfMaterial(=vanilla)과 동일 동작이므로 구현 불필요 | N/A |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `beforeSetPositionAndRotation` 미구현 — `initialized=false; multiPlayerInitialized=5` 세팅. | **처리 완료** — MixinClientPlayNetworkHandler.sm_beforePlayerPositionLook (onPlayerPositionLook HEAD inject) |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `pushOutOfBlocks` multiPlayerInitialized 체크 미구현 | **처리 완료** — MixinPlayerEntityClient.sm_pushOutOfBlocks |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `beforeOnLivingUpdate/afterOnLivingUpdate` 미구현 — flyWhileOnGround 처리. | **처리 완료** — Config: flyCloseToGround/flyWhileOnGround 추가, State: wasCapabilitiesIsFlying 추가, MixinClientPlayerEntity.sm_flyWhileOnGround (TAIL inject) |
| 2026-04-22 | `SmartMovingPlayerBase.md` | 클라이언트 `isSneaking` override 미구현 | **처리 완료** — Config: crawlOverEdge 추가, State: wouldIsSneaking/forceIsSneaking 추가, MixinLivingEntityClient.sm_isSneaking |
| 2026-04-22 | `SmartMovingPlayerBase.md` | `getFOVMultiplier` override 미구현 | **처리 완료** — Config: perspectiveFadeFactor 등 5개 추가, State: fadingPerspectiveFactor EMA 계산 추가, MixinClientPlayerEntity.sm_getFovMultiplier (AbstractClientPlayerEntity.getFovMultiplier HEAD inject) |
| 2026-04-23 | `SmartMovingRender.md` | `renderName()` 미구현 — 타인 플레이어 이름 태그 Y 보정(heightOffset=-1→-0.2F) + 크롤/스니킹 이름 표시 제어. | **처리 완료** — Config: sneakNameTag/crawlNameTag 추가; MixinPlayerEntityRenderer.sm_renderLabel (크롤숨김+heightOffset보정+스니킹보정); MixinLivingEntityRenderer.sm_isSneakyForLabel (sneakNameTag=true → isSneaky()=false → 64 거리기준) |
| 2026-04-23 | `SmartMovingRender.md` | HUD exhaustion bar `minFitnessForAction`/`minFitnessToStartAction` 기반 5종 아이콘. | **N/A** — setMaxExhaustionForAction()은 외부 모드 연동용 ISmartMovingSelf API. SmartMoving 내부에서 호출 없음 → minFitnessForAction 항상 0 → 단순 2종 아이콘 구현이 올바름. |
| 2026-04-23 | `render/SmartRenderContext.md` | 없음 — Scale/NoScaleStart/NoScaleEnd 상수 3개, 다층 갑옷 모델 레이어 전용 → 구조적 N/A | N/A |
| 2026-04-23 | `render/IModelPlayer.md` | 없음 — PlayerAPI super*() 위임 인터페이스 전체 N/A | N/A |
| 2026-04-23 | `render/IRenderPlayer.md` | 없음 — PlayerAPI superRender*() 위임 인터페이스 전체 N/A | N/A |
| 2026-04-23 | `render/playerapi/SmartMovingModelPlayerBase.md` | 없음 — dynamicOverride*/superAnimate* 전부 TAIL inject 대체, @Deprecated getter 16개 N/A | N/A |
| 2026-04-23 | `render/playerapi/SmartMovingRenderPlayerBase.md` | 없음 — renderPlayer/rotatePlayer/renderPlayerAt/passSpecialRender 전부 Inject 대체, getPlayerModels N/A | N/A |
| 2026-04-23 | `smartrender/SmartRenderModel.md` | 없음 — SR 전용 모델 계층(bipedOuter/Torso/Shoulder/Pelvic) + vanilla 애니메이션 래퍼 전체 N/A | N/A |
| 2026-04-23 | `smartrender/SmartRenderRender.md` | `currentVerticalAngle` 가드 오류 — `distance>1e-4` & atan2→순수 하강 시 -π/2. 원본: h==0 → Quarter(π/2). 비행 중 수직 하강 몸통 기울기 180° 버그 | **처리 완료** — SmartStatistics.java: `(horizontalDistance > 1e-4) ? atan2(y,h) : π/2` |
| 2026-04-23 | `smartrender/SmartRenderUtilities.md` | `wallUpJumpOrthogonalTolerance` 미구현 — 항상 90° 스냅. 원본: tolerance!=0 && abs(aligned)<5° 일 때만 스냅. | **처리 완료** — SmartMovingConfig.java: 필드+readFrom+writeTo 추가(default=5F); SmartMovingJumper.java: tolerance 체크 후 조건부 스냅 |
| 2026-04-23 | `smartrender/RendererData.md` | 없음 — bipedOuter fade 시스템 전용 데이터 컨테이너. bipedOuter N/A → 전체 N/A | N/A |
| 2026-04-23 | `smartrender/ModelPlayer.md` | 없음 — SmartRenderModel 위임 래퍼. SmartRenderModel/ModelRotationRenderer/bipedOuter 계열 전체 N/A | N/A |
| 2026-04-23 | `smartrender/RenderPlayer.md` | 없음 — SmartRenderRender 위임 래퍼 + 3-layer model(main/chestplate/armor). SmartRenderRender/3-layer 전체 N/A | N/A |
| 2026-04-23 | `smartrender/IModelPlayer.md` | 없음 — SmartRenderModel↔구현체 브릿지 인터페이스. SmartRenderModel/ModelRotationRenderer N/A → 전체 N/A | N/A |
| 2026-04-23 | `smartrender/IRenderPlayer.md` | 없음 — SmartRenderRender↔구현체 브릿지 인터페이스. SmartRenderRender/3-layer model N/A → 전체 N/A | N/A |
| 2026-04-23 | `smartrender/playerapi/SmartRenderModelPlayerBase.md` | 없음 — PlayerAPI ModelPlayerBase + IModelPlayer 어댑터. PlayerAPI/SmartRenderModel 둘 다 N/A | N/A |
| 2026-04-23 | `smartrender/playerapi/SmartRenderRenderPlayerBase.md` | 없음 — PlayerAPI RenderPlayerBase + IRenderPlayer 어댑터. PlayerAPI/SmartRenderRender 둘 다 N/A | N/A |
| 2026-04-23 | `smartrender/statistics/SmartStatistics.md` | `currentSpeed/currentHorizontalSpeed/currentVerticalSpeed` 스케일 오류 — 원본: `EMA(dist*4, 0.4)`, 버그: 원시 거리값 그대로 사용. 비행 walkFactor 4배 약화. | **처리 완료** — SmartStatistics.java: EMA with *4 계수 적용 (`dist*4f - speed) * 0.4f`) |
| 2026-04-23 | `smartrender/statistics/SmartStatisticsData.md` | 없음 — 링 버퍼 내 개별 슬롯(horizontal/vertical/all). 1.21.1에서 SmartStatistics 단일 객체로 통합. 링 버퍼/보간 없음. | N/A |
| 2026-04-23 | `smartrender/statistics/SmartStatisticsDatas.md` | 없음 — 링 버퍼 컨테이너(10슬롯). renderPartialTicks 보간 패턴. 1.21.1에서 tick-based 단일 계산으로 대체. | N/A |
| 2026-04-23 | `smartrender/statistics/SmartStatisticsContext.md` | 없음 — `calculateHorizontalStats` static flag + `onTickInGame()` Forge 이벤트. vanilla limbSwing* 덮어쓰기 패턴 불필요. | N/A |
| 2026-04-23 | `smartrender/statistics/IEntityPlayerSP.md` | 없음 — `getStatistics()` 단일 메서드 인터페이스. SmartMovingClientState.stats 필드로 대체. | N/A |
| 2026-04-23 | `smartrender/statistics/playerapi/SmartStatisticsPlayerBase.md` | 없음 — PlayerAPI ClientPlayerBase + IEntityPlayerSP 구현체. `afterMoveEntityWithHeading` → Mixin TAIL inject로 대체. | N/A |
| 2026-04-23 | `smartrender/SmartRenderInfo.md` | 없음 — FML @Mod 상수 홀더(ModId/ModName/ModVersion). fabric.mod.json으로 완전 대체. | N/A |
| 2026-04-23 | `smartrender/ModelRotationRenderer.md` | 없음 — SmartRender GL11 모델 파트 시스템 전체(6회전순서/fade보간/displayList/reflection). 1.21.1은 MatrixStack 기반, displayList 없음, SmartRenderModel 노드(bipedOuter 등) 없음. | N/A |
| 2026-04-23 | `smartrender/ModelSpecialRenderer.md` | 없음 — ignoreRender 토글 + doPopPush GL스택 리셋 패턴. ModelRotationRenderer 의존 → N/A. | N/A |
| 2026-04-23 | `smartrender/ModelCapeRenderer.md` | 없음 — 망토 물리 시뮬레이션. bipedOuter(outer.rotateAngleX 참조)/bipedBreast 노드(N/A) + GL11 의존. 1.21.1 vanilla는 CapeFeatureRenderer에서 자체 망토 물리 처리. | N/A |
| 2026-04-23 | `smartrender/ModelEarsRenderer.md` | 없음 — 귀 렌더. bipedHead as ModelRotationRenderer(N/A) + GL11 의존. SmartRenderRender.renderSpecials() 호출 경로 전체 N/A. | N/A |
| 2026-04-23 | `moving/ClimbGap.md` | 없음 — 데이터 컨테이너 6필드. Block+Meta→BlockState, Orientation→Direction 대응. canStand/mustCrawl/skipGaps ✓. copyFrom()은 잉여이나 FeetClimbing/HandsClimbing에서 실제 사용됨. | N/A |
| 2026-04-23 | `moving/FeetClimbing.md` | `isUp()` 오역 — 원본: `SlowUpWithHoldWithoutHands\|\|SlowUpWithSinkWithoutHands\|\|FastUp` (ordinal≥4), 버그: `ordinal > BASE_HOLD` (ordinal≥2). `max()` SkipGaps 조건 누락. | **처리 완료** — FeetClimbing.java: isUp() 3개 enum 상수 비교로 수정; max() SkipGaps 조건 복원 |
| 2026-04-23 | `moving/HandsClimbing.md` | `isUp()` ✓ (원본 `_value > BottomHold._value` = ordinal>3 대응). `max()` SkipGaps 조건 누락. | **처리 완료** — HandsClimbing.java: max() SkipGaps 조건 복원 |
| 2026-04-23 | `moving/Orientation.md` | `handleClimbing()` 대각 탐색 vine 방향 완전 반전 — d[0]>0(East) → vine.WEST, d[1]>0(South) → vine.NORTH. 원본 `hasVineOrientation()`: 탐색방향==vine face 방향. | **처리 완료** — SmartMovingClimber.java L304-307: vine.EAST/WEST/SOUTH/NORTH로 올바르게 수정 |
| 2026-04-23 | `moving/Compat.md` | `isSpectator()` + `isFallFlying()` 체크 누락 — 원본: `Compat.isBlockedByIncompatibility()` (isSpectator=EtFuturum gameType3, isElytraFlying=IElytraPlayer). 1.21.1 vanilla: `isSpectator()`/`isFallFlying()` 미구현. | **처리 완료** — tickEssential resetState 조건 확장, sm_travel_client 상단 early return 추가 |
| 2026-04-23 | `moving/ISmartMovingSelf.md` | 없음 — 외부 모드 실시간 상태 API (getExhaustion/getUpJumpCharge/addExhaustion 등). 1.21.1에서 해당 외부 모드 없음. SmartMovingClientState 필드(sm.exhaustion/sm.jumpCharge/sm.headJumpCharge)로 직접 접근 대체. | N/A |
| 2026-04-23 | `moving/SmartMovingClient.md` | 블록코드 채팅 메시지 억제 누락 — 원본: processBlockCode 반환 true → chatMessageList.remove(i--) (채팅창에서 제거). 1.21.1: GAME 이벤트로 처리만 하고 §-코드 메시지가 채팅에 노출됨. | **처리 완료** — ALLOW_GAME 이벤트에서 마커 감지 후 false 반환으로 채팅 억제 |
| 2026-04-23 | `moving/SmartMovingOther.md` | `processStatePacket()` 렌더링 필드 4개 누락 — actualFeetClimbType(bits 0-3), actualHandsClimbType(bits 4-7), isFeetVineClimbing(bit 25), isHandsVineClimbing(bit 26). 모두 MixinPlayerEntityModelClient.sm_animateClimbing에서 사용됨. | **처리 완료** — processStatePacket()에 4개 추출 추가, 비트 순서를 원본 역직렬화 순서(bit 0부터)에 맞춰 정렬. isClimbBackJumping(bit 28)도 추가(field 존재, onStartClimbBackJump 미이식). 컴파일: BUILD SUCCESSFUL ✓ |
| 2026-04-23 | `moving/SmartMovingClient.md` | 블록코드 채팅 메시지 억제 누락 — 원본(A-10): processBlockCode 반환 true → chatMessageList.remove(i--). 1.21.1: GAME 이벤트로 처리만 하고 채팅에서 제거하지 않음 → §0§1...§f§f 메시지 노출. | **처리 완료** — GAME→ALLOW_GAME 이벤트 전환, 마커 검사 후 false 반환으로 채팅 억제. processBlockCode 내부 중복 마커 검사 제거. BUILD SUCCESSFUL ✓
| 2026-04-23 | `moving/SmartMovingServer.md` | [잉여] processStatePacket 내 `isSliding = ((bits >> 22) & 1) != 0` — 원본 서버 미추출, bit 22는 angleJumpType LSB(클라이언트 전용). 필드+라인 제거. | **처리 완료** — SmartMovingServer.java: isSliding 필드 + 비트 추출 라인 제거. BUILD SUCCESSFUL ✓
| 2026-04-23 | `moving/SmartMovingServer.md` | [오역] beforeAddMovingHungerBatch() hunger 조건 누락 — 원본: `if(hunger != -1) disableAddExhaustion = true;` 구현: 조건 없이 항상 차단. hunger=-1(미수신) 시 vanilla 소진이 차단되는 버그. | **처리 완료** — `if (hunger >= 0F) disableAddExhaustion = true;` 조건 추가. BUILD SUCCESSFUL ✓
| 2026-04-23 | `moving/SmartMovingServer.md` | [누락] beforeActivateBlockOrUseItem / afterActivateBlockOrUseItem — 블록 상호작용 시 forceIsSneaking 설정/해제 훅. forceIsSneaking 필드는 선언·읽기 코드 존재하나 쓰는 훅 없음. 크롤링 중 블록 상호작용 시 isSneaking() 오버라이드 불작동. | 미처리 (이슈 등록 필요)
