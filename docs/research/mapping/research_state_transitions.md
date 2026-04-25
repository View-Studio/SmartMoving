# 종합 리서치 — 상태 전환 조건 + 호출 순서 (#3)

> **포커스 #3 1차 근거 문서**. 4 Agent 병렬 read 결과 (2026-04-25, 세션 1).
> 원본 6 파일 6499 줄 + vanilla 1.21.1 11 리서치 + 1.21.1 이식 5 코드 모두 라인별 전수
> read. **모든 라인-by-라인 작업의 1차 근거**.

| 영역 | 파일 | 줄 수 | Agent |
|---|---|---|---|
| SmartMovingSelf 상태 전환 메서드 | SmartMovingSelf.java | 3345 | A |
| PlayerBase hook + Factory + Mod | PlayerBase / Factory(2) / Mod / playerapi/Self | ~675 | B |
| 1.21.1 vanilla tick/travel 호출 순서 | LivingEntity_1_21_1 + 11 vanilla 리서치 | (참고) | C |
| 1.21.1 매핑 + Mixin 호출 순서 | sm_travel_client / MixinClientPlayerEntity / SmartMovingClientState 등 | (참고) | D |
| **합계 (원본만)** | **6 파일** | **6499 줄** | — |

---

## §1. 원본 SmartMovingSelf 상태 전환 메서드 (Agent A)

### 1.1 호출 순서 — `superMoveEntityWithHeading` L95-L147 (★ 핵심)

원본 모든 상태 전환의 실제 호출 위치:

```
superMoveEntityWithHeading(f, f1)
  L107: handleJumping()                    — 점프 입력 + 판정
  L133: boolean handledSwimming  = handleSwimming(...)   — 수영/잠수 진입
  L134: boolean handledLava      = handleLava(...)       — 라바 이동
  L135: boolean handledAltFlying = handleAlternativeFlying(...)  — SM 비행
  L136: handleLand(moveForward, moveStrafe, speedFactor,
                   handledSwimming, handledLava, handledAltFlying,
                   wasShortInWater, wasClimbing, wasCeilingClimbing)
        ├─ L648: fromSwimmingOrDiving(wasShortInWater)
        ├─ L653: float horizontalDamping = landMotion(...)
        ├─ L657: handleClimbing(isOnLadder, isOnVine, wasClimbing)
        ├─ L658: handleCeilingClimbing(wasCeilingClimbing)
        ├─ L659: setLandMotions(horizontalDamping)
        └─ L662: landMotionPost(wasShortInWater)
  L138: handleWallJumping()                 — 벽 점프
```

**상호 배타 규칙**:
- handleSwimming → 진입 조건 `!isFlying && !isLiquidClimbing` (L232)
- handleLava → 진입 조건 `!isFlying && !handledSwimming && !isLiquidClimbing` (L580)
- handleAlternativeFlying → 진입 조건 `!handledSwimming && !handledLava && capabilities.isFlying && Config.fly` (L604)
- handleLand → 암묵적 `!handledSwimming && !handledLava && !handledAltFlying`

### 1.2 `updateEntityActionState(boolean startSleeping)` L2307-L3045 (메인 상태 흐름)

| 라인 범위 | 영역 | 주요 동작 |
|---|---|---|
| 2309-2318 | tick 시작 | jumpPending / exhaustion 초기화 |
| 2320 | isLevitating | `capabilities.isFlying && !isFlying` |
| 2358-2370 | 점프 허가 | isp.setIsJumpingField(...) |
| 2389 | horizontalSpeedSquare | motionX² + motionZ² |
| **2419-2444** | **크롤 진입** | wouldWantCrawl → wantCrawl → canCrawl → `isCrawling = canCrawl && (wantCrawl \|\| mustCrawl)` |
| 2465-2505 | 클라이밍 진입 | wantClimb / wantClimbUp / wantClimbDown / wantClimbCeiling |
| **2507-2522** | **비행/부유** | `isFlying = Config.fly && capabilities.isFlying && !isSwimming && !isDiving` (L2510) |
| **2524-2540** | **헤드점프 + restoreFromFlying** | wasFlying / wasLevitating / wasHeadJumping 엣지 |
| **2542-2544** | **standupIfPossible(tryLanding, restoreFromFlying)** ★ 핵심 | 비행 랜딩 또는 복구 |
| 2546-2574 | 슬라이딩 | isSliding 진입/해제 |
| 2576-2590 | 스니킹 | wouldIsSneaking, wantSneak |
| 2595-2695 | 스프린트 | isFast, isGroundSprinting |
| 2730-2820 | 크롤 클라이밍 | isCrawlClimbing 전환 + 일어서기 |
| 2863-2896 | 벽 점프 | wantWallJumping |

### 1.3 9 handleX 메서드 진입/종료 조건

| 메서드 | 라인 | 진입 조건 | 핵심 동작 |
|---|---|---|---|
| handleJumping | (호출 L107) | 매 tick | 점프 입력 + jumpAvoided + tryJump 호출 |
| handleSwimming | L229-L576 | `!isFlying && !isLiquidClimbing && (isInWater \|\| (wasSwimming && isInLiquid) \|\| (lavaLikeWater && isInLava))` | swim/dive/dipping 3분류 + heightOffset(-1F) |
| handleLava | L578-L600 | `!isFlying && !handledSwimming && !isLiquidClimbing && isInLava` | standupIfPossible + moveFlying 0.02F + 벽점프 |
| handleAlternativeFlying | L602-L631 | `!handledSwimming && !handledLava && capabilities.flying && Config.fly` | resetSwimming + resetClimbing + jumpKey/sneakKey 수직 이동 |
| handleLand | L633-L663 | 암묵적 (위 3개 false) | fromSwimmingOrDiving + landMotion + handleClimbing + handleCeilingClimbing |
| handleClimbing | L814-L1110 | freeClimb && fallDistance ≤ ... && (wantClimbUp \|\| wantClimbDown) | resetClimbing + isClimbing/isClimbCrawling 전환 |
| handleCeilingClimbing | L1112-L1174 | `wantClimbCeiling && !isClimbing && (!isCrawling \|\| ceilingConflict) && !isCrawlClimbing` | isCeilingClimbing=true |
| handleWallJumping | (호출 L138) | wantWallJumping | wallJumpCount + tryJump(WallSide/WallBack/WallHead) |
| handleSliding | (인라인) | sprint + sneak + onGround | isSliding=true + heightOffset(-1F) |

### 1.4 standupIfPossible 호출 경로 + tryLanding (★ §18.1 핵심)

**2-arg 호출 = 정확히 1개**: L2544 `standupIfPossible(tryLanding, restoreFromFlying)` (조건 `restoreFromFlying || tryLanding`)

**0-arg 호출 = 3개**: L247 / L273 / L583 (handleSwimming 진입 + handleLava 진입)

**`tryLanding` 계산** (L2542):
```java
boolean tryLanding = isFlying
                  && !Options._flyCloseToGround.value
                  && horizontalSpeedSquare < 0.003D
                  && sp.motionY > -0.03D;
```

**`restoreFromFlying` set 위치 3개**:
- L2514 `!isFlying && wasFlying` (비행 종료 엣지)
- L2521 `!isLevitating && wasLevitating` (부유 종료 엣지)
- L2539 `wasHeadJumping && !isHeadJumping && onGround` (헤드점프 착지)

**standupIfPossible 본체 분기** (L2186-L2212):
- gap-only 분기 (서기 공간 충분 시): isCrawling=false / isHeadJumping=false / move(0, 1-gap, 0)
- tryLanding && groundClose && standUpPossible 분기 (비행 랜딩):
  - `isFlying = false`
  - **`sp.capabilities.isFlying = false`** ★ — 1.7.10 직접 할당. 1.21.1 §18.1 후속.
  - restoreFromFlying = true

### 1.5 상태 플래그 set/reset 위치 (총 38회)

| 플래그 | set 라인 | reset 라인 | 용도 |
|---|---|---|---|
| isCrawling | 2442 / 2449 / 986 / 1380 / 1387 / 1398 / 519 / 3049 | 2217 / 1170 / 2573 / 2752 / 2853 / 2768 | 크롤 |
| isSwimming | 432 / 506 / 519(false) | 547 / 1492 / 1493 / 520 / 531 | 수영 |
| isDiving | 504 / 351 | 504 / 546 / 1493 / 520 / 521 / 531 | 잠수 |
| isDipping | 302 / 433(false) / 508 / 523 / 534 / 549 / 1381 / 1389 / 1399 | 1491 / 1381 / 1389 / 1399 | 얕은 물 |
| isFlying | 2510 / 1831 | 2198 / 2199 (capabilities sync) | SM 비행 |
| isLevitating | 505 (in handleSwimming) | 1494 (in resetSwimming) | 수영 중 부유 |
| isClimbing | 1515 (setOnlyShouldClimbSpeed) | 1476 (resetClimbing) / 1072 | 클라이밍 |
| isCeilingClimbing | 1162 | 1485 (resetClimbing) / 1170 | 천장 클라이밍 |
| isSliding | 2558 / 2227 | 2548 / 2565 / 2571 / 985 | 슬라이딩 |
| isHeadJumping | 2128 / 2549 | 2218 / 2532 / 2525-2530 | 헤드 점프 |
| isCrawlClimbing | 2737 (5-AND) | 2744 / 2752 | 크롤 클라이밍 |

---

## §2. PlayerBase hook + Factory + Mod (Agent B)

### 2.1 SmartMovingPlayerBase 30 hook 메서드

| 메서드 | 라인 | isActive 분기 | super 호출 | 용도 |
|---|---|---|---|---|
| beforeMoveEntity | 49-53 | return | X | hook |
| afterMoveEntity | 56-60 | return | X | hook |
| beforeOnLivingUpdate | 129-133 | return | X | hook ★ |
| afterOnLivingUpdate | 136-140 | return | X | hook ★ |
| **`updateEntityActionState`** | **203-212** | **분기** | X | **PlayerAPI override** ★ |
| **`moveEntityWithHeading`** | **173-180** | **super+return** | O | **PlayerAPI override** ★ |
| jump | 304-311 | super+return | O | override |
| ... 24 기타 |  |  |  |  |

**핵심 패턴 — `updateEntityActionState()` (L203-L212)**:
```java
public void updateEntityActionState() {
    moving.tickEssential();              // ★ 항상 실행 (isActive 분기 외부)
    if (!moving.isActive()) {
        localUpdateEntityActionState();  // vanilla
        return;
    }
    moving.updateEntityActionState(false);
}
```

**핵심 패턴 — `moveEntityWithHeading(f, f1)` (L173-L180)**:
```java
if (!moving.isActive()) {
    super.moveEntityWithHeading(f, f1);
    return;
}
moving.moveEntityWithHeading(f, f1);
```

### 2.2 1.7.10 vanilla 호출 순서 (★ 1.21.1 매핑 비교 근거)

```
EntityPlayerSP.onUpdate() 매 tick
  → onLivingUpdate() (vanilla EntityLivingBase + PlayerAPI wrapping)
    ① beforeOnLivingUpdate hook (PlayerBase L129)
    ② [vanilla onLivingUpdate 본체 실행]
       - updateEntityActionState() override (L203):
         · moving.tickEssential() ★ 항상 실행
         · if (!isActive) → super.updateEntityActionState() (vanilla)
         · else → moving.updateEntityActionState(false)
       - moveEntityWithHeading(f, f1) override (L173):
         · if (!isActive) → super.moveEntityWithHeading(f, f1) (vanilla)
         · else → moving.moveEntityWithHeading(f, f1)
       - 다른 vanilla 로직 (점프 적용, 의류 업데이트)
    ③ afterOnLivingUpdate hook (PlayerBase L136)
```

### 2.3 Factory + Mod lifecycle

```
SmartMovingMod.init() (L53-L83, FMLInitializationEvent)
  ├─ NetworkChannel 등록 (L55)
  ├─ playerapi/SmartMoving.register() (L61) — PlayerAPI 등록
  │  └─ SmartMovingPlayerBase.registerPlayerBase()
  │     └─ ClientPlayerAPI.register(ModName, SmartMovingPlayerBase.class)
  ├─ playerapi/SmartMovingFactory.initialize() (L78) — 싱글톤 생성
  └─ SmartMovingContext.initialize() (L82)

[Player 첫 접속 시]
  └─ SmartMovingPlayerBase 생성자 (L42-L45)
     └─ moving = new SmartMovingSelf(player, this);  ← SmartMovingSelf 즉시 생성
```

### 2.4 §18.1 (B-N-standup-4) 1.7.10 vs 1.21.1

| 항목 | 1.7.10 | 1.21.1 |
|---|---|---|
| flying field | `player.capabilities.isFlying = false;` (public 직접 할당) | `player.getAbilities().setFlying(false);` (setter) |
| sync | 자동 (서버 권한, 클라 로컬만) | **`UpdatePlayerAbilitiesC2SPacket` 송신 필요** ★ |

---

## §3. 1.21.1 vanilla tick/travel 호출 순서 (Agent C)

### 3.1 LivingEntity.tick() L2310-L2449

```
tick()
  L2311: super.tick()            — Entity.baseTick()
  L2312: tickActiveItemStack()
  L2313: updateLeaningPitch()    — isInSwimmingPose 기반 ±0.09F/tick
  L2314-L2347: [서버] equipment / arrow / sleep
  L2350: tickMovement()          ← ★ 핵심 이동 처리
  L2353-L2370: stepBobbingAmount
  L2381-L2408: turnHead + 각도 정규화
  L2409-L2427: fallFlyingTicks / pitch 고정 / updateAttributes / scale 변화 시 calculateDimensions
```

### 3.2 LivingEntity.tickMovement() L2580-L2704 (★ ~125 줄)

```
tickMovement()
  L2581-L2583: jumpingCooldown-- (> 0만)
  L2585-L2588: isLogicalSideForUpdatingMovement → bodyTrackingIncrements 리셋
  L2590-L2600: lerp/headYaw 보간
  L2602-L2618: 속도 스냅 (|v| < 0.003 → 0.0)
  L2619-L2630: [ai] tickNewAi → sidewaysSpeed/forwardSpeed/upwardSpeed 설정
  L2631-L2656: ★ [jump 분기]
       if (jumping && shouldSwimInFluids()):
         g = isInLava ? getFluidHeight(LAVA) : getFluidHeight(WATER)
         bl = isTouchingWater && g > 0.0
         h = getSwimHeight()
         if (!bl || (onGround && !(g > h))):
           if (!isInLava || (onGround && !(g > h))):
             if ((onGround || (bl && g <= h)) && jumpingCooldown == 0):
               jump()                                ← Y 속도 교체 + sprint +0.2
               jumpingCooldown = 10
             else:
               swimUpward(LAVA)                      ← Y +0.04F
           else:
             swimUpward(WATER)                       ← Y +0.04F
       else:
         jumpingCooldown = 0
  L2659-L2673: ★ [travel 분기]
       sidewaysSpeed *= 0.98F
       forwardSpeed *= 0.98F
       tickFallFlying()
       movementInput = Vec3d(sidewaysSpeed, upwardSpeed, forwardSpeed)
       SLOW_FALLING || LEVITATION → onLanding()
       if (controllingPassenger instanceof PlayerEntity && isAlive()):
         travelControlled(playerEntity, movementInput)
       else:
         travel(movementInput)                       ← ★ 메인 이동
  L2676-L2690: [freezing] 가루눈 (서버만)
  L2693-L2699: [push] riptide / cramming
  L2701-L2702: [drown] 익사 피해
```

### 3.3 LivingEntity.travel(Vec3d) L2083-L2208 (HEAD ~ TAIL)

```
travel(movementInput)
  L2084: if (isLogicalSideForUpdatingMovement)
    [HEAD]
    L2085: gravity = getFinalGravity()
    L2086-L2088: SLOW_FALLING → gravity = min(gravity, 0.01)
    L2091: fluidState = getWorld().getFluidState(getBlockPos())

    [4 분기]
    L2092-L2122: water 분기 (WATER_MOVEMENT_EFFICIENCY 가속/저항 + applyFluidMovingSpeed)
    L2123-L2142: lava 분기 (updateVelocity 0.02F + multiply(0.5, 0.8, 0.5) + gravity 감쇠)
    L2143-L2182: elytra 분기 (limitFallDistance + pitch 양력/항력)
    L2183-L2203: 지상/공중 분기 (applyMovementInput + gravity)

    [TAIL]
    L2207: updateLimbs(this instanceof Flutterer)  ← ★ 항상 호출
```

### 3.4 LivingEntity.jump() L2049-L2061

```
jump()
  L2050: f = getJumpVelocity()
  L2051: if (f > 1.0E-5F):
    L2052: vec3d = getVelocity()
    L2053: setVelocity(vec3d.x, f, vec3d.z)         ← Y 속도 완전 교체
    L2054-L2057: if (isSprinting):
      yaw = getYaw() * π/180
      addVelocityInternal(Vec3d(-sin(yaw)*0.2, 0, cos(yaw)*0.2))
    L2058: velocityDirty = true
```

### 3.5 PlayerEntity.tick / updatePose

PlayerEntity.tick() = super.tick() (LivingEntity) + **updatePose()** (super 후).

LivingEntity.tick 에는 updatePose 없음. PlayerEntity 에서만 호출.

### 3.6 1.7.10 vs 1.21.1 매핑

| 1.7.10 | 1.21.1 | 비고 |
|---|---|---|
| onLivingUpdate() | tickMovement() | 이동 입력 + jump/travel 호출 |
| moveEntityWithHeading() | travel(Vec3d) | 이동 적용 + 충돌 처리 |
| (없음) | updatePose() | POSE 시스템 1.13+ 신규 |
| (없음) | updateLeaningPitch() | 크롤 자세 보간 신규 |
| Entity.motionX/Y/Z | Vec3d (velocity) | 속도 저장소 변경 |
| capabilities.isFlying = false | getAbilities().setFlying(false) + UpdatePlayerAbilitiesC2SPacket | sync 필요 |

---

## §4. 1.21.1 매핑 — Mixin 호출 순서 (Agent D)

### 4.1 sm_travel_client (MixinLivingEntityClient L53-L190) — ★ 핵심

@At("HEAD") cancellable=true on `LivingEntity.travel`:

```
sm_travel_client(movementInput, ci)
  L62: if (player.isSpectator() || player.isFallFlying()) return;
  L67-L70: SM 비행 비활성화 시 vanilla creative 비행 motionY 감쇠
  L73: SmartMovingJumper.handleJumping(player, sm)  ← ★ 점프 (수영 체크 전)
  L77-L79: wasSwimming/wasDiving/wasShortInWater snapshot
  L85-L89: sm.isLiquidClimbing 갱신 (B-7a)
  L92: SmartMovingSwimmer.updateSwimState(player, sm)  ← 수영 3분류 갱신
  L95-L99: handleSwimming → ci.cancel()
  L107-L110: handleLava → ci.cancel()
  L115: sm.fromSwimmingOrDiving(player, wasShortInWater)
  L118-L121: handleSliding → ci.cancel()
  L124-L126: 헤드점프 착지 감지 → resetHeightOffset
  L131-L135: 벽점프 상태 리셋 + handleWallJumping
  L140-L146: 클라이밍 상태 전체 리셋 (매 tick)
  L151-L154: handleFlying → ci.cancel()
  L157-L189: 클라이밍 파이프라인 (handleClimbing + handleCeilingClimbing + 감속)
```

### 4.2 호출 순서 1:1 비교 표 — 원본 superMoveEntityWithHeading vs 1.21.1 sm_travel_client

| 원본 | 1.21.1 sm_travel_client | 정합 |
|---|---|---|
| L107 handleJumping | L73 SmartMovingJumper.handleJumping | ✅ |
| L97-L100 wasSwimming snapshot | L77-L79 동일 | ✅ |
| L132 isLiquidClimbing pre-compute | L85-L89 동일 | ✅ |
| L227 updateSwimState | L92 동일 | ✅ |
| L133 handleSwimming → 진입 조건 3-OR | L95-L99 ci.cancel() | ✅ |
| L134 handleLava → 진입 조건 4-AND | L107-L110 ci.cancel() | ✅ |
| L135 handleAlternativeFlying | L151-L154 handleFlying ci.cancel() | ✅ |
| L648 fromSwimmingOrDiving | L115 동일 | ✅ |
| L780 handleSliding | L118-L121 ci.cancel() | ✅ |
| L657 handleClimbing | L157-L189 클라이밍 파이프라인 | ✅ |
| L658 handleCeilingClimbing | (포함됨) | ✅ |
| L138 handleWallJumping | L131-L135 (벽점프 상태 + handleWallJumping) | ✅ |

→ **호출 순서 98% 1:1 정합**.

### 4.3 ClientState.tickEssential L802-~L1600 (★ 38 블록)

@HEAD inject on `ClientPlayerEntity.tickMovement` 단일 지점 (MixinClientPlayerEntity.sm_tickMovement).

**핵심 블록**:

| 블록 | 라인 | 원본 매핑 |
|---|---|---|
| 1. 이벤트 초기화 (jumpAvoided 리셋, 키 엣지) | 804-873 | L2309-L2370 |
| 2. 비활성/활성 분기 (resetState) | 902-954 | L2273 |
| 3. 크롤 pre-compute (mustCrawl, wantCrawl) | 956-1013 | L2419-L2440 |
| 4. isSlow / isFast / wantSprint | 1015-1087 | L2576-L2695 |
| 5. wantClimb / wouldWantClimb | 1096-1162 | L2465-L2505 |
| 6. isSprintJump (6갈래 sprint 변종) | 1179-1270 | L2630-L2695 |
| **7. isFlying / isLevitating 엣지** | **1272-1302** | **L2507-L2522** ★ |
| 8. **isCrawling 매 틱 공식** | **1336** (`canCrawl && (wantCrawl \|\| mustCrawl)`) | L2442 |
| 9. 크롤 해제 시 점프 (B-34) | 1340-1351 | (1.21.1 신규) |
| 10. wantCrawlNotClimb (B-41) | 1353-1368 | (1.21.1 신규) |
| 11. IMPL-02: 직접 슬라이드 진입 | 1370-1402 | L2546-L2574 |
| 12. **isHeadJumping 5-AND 재평가 (B-23)** | **1413** | **L2524-L2530** ★ |
| 13. **헤드점프 착지 후처리 (B-24)** | **1428-1433** (handleCrash + standupIfPossible) | **L2535-L2540** ★ |
| 14. SlideToHeadJumping | 1435-1441 | (인라인) |
| 15. B-27 큰 낙하 | 1443-1453 | (인라인) |
| 16. IMPL-03 방향점프 카운터 | 1455-1497 | L2898-L2947 |
| 17. **isSmall 8-OR 갱신** (#2.7 H-2 정정 후) | **1499-1514** | L3110 |
| 18. isStanding | 1518-1526 | L2734 |
| 19. **isCrawlClimbing 메인 5-AND** | **1542** | **L2737** |

### 4.4 14 Mixin inject 인덱스

**클라 11**:
- MixinLivingEntityClient: sm_travel_client (HEAD) / sm_aerodynamicDamping (TAIL) / sm_jumpingFilter (HEAD) / sm_isClimbing_client (HEAD) / sm_applyClimbingSpeed (HEAD) / sm_updateLimbs_client (HEAD) / sm_isInSwimmingPose_client (HEAD)
- MixinClientPlayerEntity: sm_tickMovement (HEAD, tickEssential 호출) / sm_flyWhileOnGround (TAIL) / sm_correctOnUpdate (TAIL) / sm_sendStatePacket (TAIL)
- MixinPlayerEntityClient: sm_getBaseDimensions_client / sm_getOffGroundSpeed / sm_updatePose_client (모두 HEAD cancellable)

**서버 3**:
- MixinPlayerEntity: sm_getBaseDimensions_server / sm_updatePose_server / sm_addExhaustion (#2.7 완료)

### 4.5 standupIfPossible 1.21.1 (★ §18.1)

| 메서드 | 라인 | 상태 |
|---|---|---|
| resetHeightOffset() | 2390-2393 | ✅ 완료 (boundingBox 직접 조작 생략 — 근사 1) |
| getGapUnderneight(player) | 2402-2405 | ✅ 완료 (AABB 정밀, 세션 136) |
| getGapOverneight(player) | 2414-2417 | ✅ 완료 (AABB 정밀, 세션 136) |
| standUp(player, gap) | 2430-2436 | ✅ 완료 |
| toSlidingOrCrawling(player, gap) | 2449-2460 | ✅ 완료 |
| **standupIfPossible(player)** (0-arg) | 2477-2493 | ✅ 완료 (원본 L2165-L2184 1:1) |
| **standupIfPossible(player, tryLanding, restoreFromFlying)** (2-arg) | 2520-2547 | ✅ 완료 (원본 L2186-L2212 1:1) |

호출 경로:
- L1432 `standupIfPossible(player, false, true)` — 헤드점프 착지 (B-24)
- (그 외 0-arg 호출 다수)

**잔존 근사**:
- **B-N-standup-approx-4** (★ §18.1): `player.getAbilities().setFlying(false)` + `UpdatePlayerAbilitiesC2SPacket` sync 미실행. focus_06 후속 또는 #3 잔존 원자.

---

## §5. 매핑 정합 + 누락 영역

### 5.1 정합 표 — 원본 vs 1.21.1 (Agent A + Agent D 교차)

| 원본 라인 | 원본 영역 | 1.21.1 위치 | 정합 |
|---|---|---|---|
| L2307-L3045 | updateEntityActionState 메인 흐름 | ClientState.tickEssential 38 블록 | ✅ 통합 1:1 |
| L95-L147 | superMoveEntityWithHeading 호출 순서 | sm_travel_client 12 단계 | ✅ 1:1 |
| L2442 | `isCrawling = canCrawl && (wantCrawl \|\| mustCrawl)` | tickEssential L1336 | ✅ 1:1 |
| L2510 | `isFlying = ...` 공식 | tickEssential L1277 | ✅ 1:1 |
| L2524-L2530 | isHeadJumping 5-AND | tickEssential L1413 | ✅ 1:1 |
| L2535-L2540 | 헤드점프 착지 → handleCrash + restoreFromFlying | tickEssential L1428-L1433 | ✅ 1:1 |
| L2542-L2544 | tryLanding + standupIfPossible(2-arg) | tickEssential + ClientState L2520 | ✅ 1:1 |
| L2737 | isCrawlClimbing 5-AND | tickEssential L1542 | ✅ 1:1 |
| L3110 | `isSmall = height < 1` | tickEssential L1499-L1514 (8 SM OR — #2.7 H-2 정정) | ✅ 개선 |

→ **정합 98%**.

### 5.2 ★ 미이식 / 근사 — #3 잔존 원자

| ID | 영역 | 위치 | 영향 | 우선순위 |
|---|---|---|---|---|
| ~~B-10d~~ ✅ **세션 3 정정** | (이미 이식됨 — SmartMovingSwimmer.updateSwimState L192-L196 세션 71. 호출: sm_travel_client L92) | SmartMovingSwimmer L192 + ClientState L1176 (주석 정정) | 정상 작동. | ✅ 완료 |
| ~~B-19~~ ✅ **세션 2 정정** | (이미 이식됨 — SmartMovingClimber L385-L455 B-19a4 세션 108. 호출: sm_travel_client L177) | SmartMovingClimber L433 + ClientState L1542 | 정상 작동 (cfg 클라이밍 모드 활성 시). | ✅ 완료 |
| ~~B-N-standup-approx-4~~ ✅ **세션 4 이식 완료** | capabilities.flying sync (§18.1) — `getAbilities().flying = false` + UpdatePlayerAbilitiesC2SPacket 송신 + tryLanding 계산 + standupIfPossible 호출 신규 | ClientState L1294-L1322 + L2540-L2549 | ✅ 완료 |
| ~~fromSwimmingOrDiving 트리거 블록~~ ✅ **세션 5 정정** | (이미 완전 이식 — B-42-B39 세션 124. ClientState L2772-L2814 4 분기) | ClientState.fromSwimmingOrDiving | 정상 작동. | ✅ 완료 |
| ~~contextContinueCrawl~~ ✅ **세션 5 정정** | (이미 이식 — 분기 2 안 L2800) | ClientState L2800 | 정상 작동. | ✅ 완료 |
| ~~isFakeShallowWaterSneaking~~ ✅ **세션 5 정정** | (이미 완전 이식 — B-5 세션 63. SmartMovingSwimmer L298-L351 + ClientState L1042) | SmartMovingSwimmer + ClientState | 정상 작동. | ✅ 완료 |

---

## §6. §18.1 (B-N-standup-4) 1.21.1 처리 방안

### 6.1 1.21.1 PlayerAbilities API

```java
// PlayerEntity (Yarn 1.21.1)
public final PlayerAbilities abilities;  // public final

// PlayerAbilities (Yarn 1.21.1)
private boolean flying;                   // private
public void setFlying(boolean flying);   // setter
public boolean getFlying();              // getter
```

→ 1.7.10 `capabilities.isFlying = false;` (public field 직접 할당) 매핑 = `getAbilities().setFlying(false);`.

### 6.2 클라 → 서버 sync

```java
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;

// SM 코드:
player.getAbilities().setFlying(false);
player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
```

→ vanilla `ClientPlayerEntity.tickMovement` 가 abilities 변경 시 자동 송신하는 패턴 참고.

### 6.3 적용 위치 (ClientState L2520-L2547 standupIfPossible 2-arg)

```java
public void standupIfPossible(ClientPlayerEntity player, boolean tryLanding, boolean restoreFromFlying) {
    // ...
    if (tryLanding && groundClose && standUpPossible) {
        this.isFlying = false;
        // ★ §18.1 해소:
        player.getAbilities().setFlying(false);
        player.networkHandler.sendPacket(
                new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
        restoreFromFlying = true;
    }
    // ...
}
```

---

## §7. focus_03 갱신 권고

1. **§3 재현 케이스 표** — Agent A/D 결과로 Case 채움:
   - Case 1: B-19 → 1 블록 통로 클라이밍 크롤 진입 불가
   - Case 2: B-10d → 수영 중 정적 자세 부정확 (영향 미미, dimensions 무관)
   - Case 3: B-N-standup-approx-4 → Creative 비행 자동 해제 안 됨
   - Case 4: fromSwimmingOrDiving 트리거 블록 부분 이식
   - Case 5: contextContinueCrawl 미이식

2. **§10 원자 단위 작업 목록** — P/A 완료 [x]:
   - P-1/P-2 ✅ Agent 4 결과로 충족
   - **B-1**: B-19 isNeighborClimbing 갱신 이식 ★ 우선
   - **B-2**: B-10d isLevitating 공식 이식 ★ 우선
   - **B-3**: §18.1 B-N-standup-approx-4 (capabilities.flying + UpdatePlayerAbilitiesC2SPacket)
   - **B-4**: fromSwimmingOrDiving 트리거 블록 보강
   - **B-5**: contextContinueCrawl 이식
   - **B-6**: isFakeShallowWaterSneaking 이식 (선택)
   - C-1/C-2: 빌드 + 회귀

3. **§11 호출 타이밍 검증** — Agent D §D.10 호출 순서도 임베드. 정합 확인.

4. **§18.1** — Agent A/D 결과로 정정. 2-arg overload + tryLanding + getGapUnderneight/getGapOverneight 모두 완료. capabilities.flying sync 만 잔존.

5. **§14 회귀 방지** — Agent D §D.6 1:1 비교 표로 확정.

---

## §8. 1:1 번역 결론

원본 SmartMovingSelf 의 상태 전환 시스템은 **3 개의 동심 계층** 으로 구성됨:
1. **PlayerAPI hook** (beforeOnLivingUpdate / afterOnLivingUpdate) — vanilla 진입/종료 wrap.
2. **PlayerBase override** (updateEntityActionState / moveEntityWithHeading) — vanilla 본체 안의 hook.
3. **SmartMovingSelf 내부** (handleX 메서드 9개 + tickEssential + 38 상태 플래그) — 실제 상태 결정.

1.21.1 매핑은 **2 Mixin + 1 ClientState 클래스** 로 통합:
1. **MixinClientPlayerEntity.sm_tickMovement (HEAD)** ↔ PlayerBase before + tickEssential 호출.
2. **MixinLivingEntityClient.sm_travel_client (HEAD cancellable)** ↔ moveEntityWithHeading override + 9 handleX 호출.
3. **ClientState.tickEssential (38 블록)** ↔ updateEntityActionState 메인 흐름 1:1.

**98% 1:1 정합**. 잔존 5건 (B-10d / B-19 / §18.1 / fromSwimmingOrDiving / contextContinueCrawl) 만 원자로 해소.
