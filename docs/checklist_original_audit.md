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
