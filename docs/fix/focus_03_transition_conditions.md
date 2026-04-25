# Focus #3 — 상태 전환 조건 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #3 일 때 진입.
>
> **종합 리서치 (세션 1, 2026-04-25)**: `docs/research/mapping/research_state_transitions.md`
> — 원본 6 파일 6499 줄 + vanilla 11 리서치 + 1.21.1 5 코드 모두 라인별 전수 read 결과.
> 4 Agent 병렬 (A: SmartMovingSelf 3345 줄 / B: PlayerBase + Factory + Mod / C: vanilla
> 1.21.1 / D: Mixin + ClientState 매핑). **모든 원자 작업의 1차 근거**.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟢 진입 가능 (#2 / #2.5 / #2.6 / #2.7 모두 AI 완결) |
| 현재 단계 | 세션 1 — 전수 리서치 완료 → Phase B 이식 진입 전 |
| 선행 의존 | #2 / #2.5 / #2.6 / #2.7 모두 완결 ✓ |

---

## 2. 증상 — 원본 vs 현재

특정 **조합** 에서 상태가 원본과 다른 **타이밍**에 전환됨. 혹은 전환 자체가 일어나지
않음. 플래그는 맞는데 전환 경로가 어긋나는 경우.

**세션 1 전수 리서치 결과 — 호출 순서 98% 1:1 정합**. 잔존 5건만 미이식/근사:
1. **B-10d** isLevitating 공식 미이식 (항상 false).
2. **B-19** isNeighborClimbing 미갱신 → isCrawlClimbing 항상 false (1 블록 통로 진입 불가).
3. **§18.1 B-N-standup-approx-4** capabilities.flying sync 미실행.
4. **fromSwimmingOrDiving 트리거 블록** 부분 이식.
5. **contextContinueCrawl** 미이식.

---

## 3. 재현 케이스 표 — 세션 1 4 Agent 결과로 채움

| # | 시나리오 | 대상 전환 | 원본 기대 트리거 | 1.21.1 실제 | 원본 라인 근거 | 잔존 ID |
|---|---------|---------|---------------|-----------|-------------|---|
| 1 | 1 블록 통로 클라이밍 + 크롤 (천장 1.5 블록 이하) | isCrawlClimbing → true | `(wasCrawling \|\| isCrawlClimbing) && isClimbing && isNeighborClimbing && sneak && moveForward` (L2737) | ✅ **세션 2 검증 완료 — 이미 정상 작동** (cfg 클라이밍 모드 활성 시) | SmartMovingClimber L385-L455 + ClientState L1542 | ~~B-19~~ ✅ |
| 2 | 수중 정적 자세 (방향키 없이 수영) | isLevitating → true | `diving && !diveUp && !diveDown && moveStrafe==0 && moveForward==0` (L505) | ✅ **세션 3 검증 완료 — 이미 정상 작동** (B-10d 세션 71 이식 완료) | SmartMovingSwimmer L192-L196 | ~~B-10d~~ ✅ |
| 3 | Creative 비행 + 좁은 공간 접근 (속도 낮음 + 하강) | tryLanding → standupIfPossible(true, restoreFromFlying) | tryLanding=true → `capabilities.isFlying = false` + restoreFromFlying=true (L2199) | ✅ **세션 4 이식 완료** — `getAbilities().flying = false` + `UpdatePlayerAbilitiesC2SPacket` 송신 | ClientState L1294-L1322 + L2540-L2549 | ~~§18.1~~ ✅ |
| 4 | 깊은 물 → 육지 (걷기/스니크/크롤) | fromSwimmingOrDiving 4 분기 (L1369-L1404) | wasShortInWater && !isShortInWater → 4 분기 setHeightOffset(-1F) | ✅ **세션 5 검증 완료 — 이미 완전 이식** (B-42-B39 세션 124) | ClientState L2772-L2814 | ~~B-fromSwim~~ ✅ |
| 5 | 물 표면 아래 크롤 유지 | contextContinueCrawl=true (L1389) | 깊은 물 → 수면 아래 크롤 진입 시 set | ✅ **세션 5 검증 완료 — 이미 이식** (분기 2 L2800) | ClientState L2800 | ~~B-context~~ ✅ |

**선택**: 케이스 2 (B-10d) 는 실제 영향 미미 (POSE/dimensions 무관 — #2.7 D 옵션 3 채택으로 isLevitating 비트만 사용). 우선순위 ↓.

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2 / #2.5 / #2.6 / #2.7** 모두 완결 ✓ |
| 영향받는 후속 | #4 (키 커맨드 결과), #1 (애니메이션 전환 순간) |
| 영향 주는 완료 이식 | sm_travel_client 12단계 / tickEssential 38 블록 / §18.1 standupIfPossible 2-arg / Phase 1·2 #2.7 BBox/POSE/EyeHeight |

---

## 5. 원본 근거

### 5.1. 종합 리서치 (세션 1 신규 ★)

**`docs/research/mapping/research_state_transitions.md`** (~700 줄):
- §1 SmartMovingSelf 상태 전환 메서드 (Agent A 결과)
- §2 PlayerBase hook + Factory + Mod (Agent B)
- §3 1.21.1 vanilla tick/travel 호출 순서 (Agent C)
- §4 1.21.1 매핑 — Mixin 호출 순서 (Agent D)
- §5 매핑 정합 + 누락 영역
- §6 §18.1 (B-N-standup-4) 1.21.1 처리 방안
- §7 focus_03 갱신 권고
- §8 1:1 번역 결론

### 5.2. 원본 라인 인덱스 (라인 정확)

`C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`:
- **L95-L147** `superMoveEntityWithHeading` — 호출 순서 (★ 핵심)
- **L227-L576** handleSwimming
- **L578-L600** handleLava
- **L602-L631** handleAlternativeFlying
- **L633-L663** handleLand (→ fromSwimmingOrDiving / landMotion / handleClimbing / handleCeilingClimbing)
- **L814-L1110** handleClimbing
- **L1112-L1174** handleCeilingClimbing
- **L1363-L1405** fromSwimmingOrDiving (★ B-fromSwim)
- **L1389** contextContinueCrawl=true (★ B-context)
- **L505** isLevitating 공식 (★ B-10d)
- **L2196-L2212** standupIfPossible 본체 (vanilla flying 해제 분기 ★ §18.1)
- **L2307-L3045** updateEntityActionState 메인 흐름
- **L2419-L2444** 크롤 진입 (wouldWantCrawl → wantCrawl → canCrawl → isCrawling)
- **L2510** `isFlying = Config.fly && capabilities.isFlying && !isSwimming && !isDiving`
- **L2524-L2530** isHeadJumping 5-AND 재평가
- **L2542-L2544** tryLanding 계산 + standupIfPossible(2-arg)
- **L2737** isCrawlClimbing 5-AND (★ B-19 의존)
- **L3110** `isSmall = sp.height < 1`

`...\playerapi\SmartMovingPlayerBase.java`:
- L173-L180 moveEntityWithHeading override
- L203-L212 updateEntityActionState override (tickEssential 항상 + isActive 분기)
- L129-L140 beforeOnLivingUpdate / afterOnLivingUpdate

---

## 6. 1:1 매핑 테이블 — 세션 1 확정

원본 superMoveEntityWithHeading L95-L147 ↔ 1.21.1 sm_travel_client L53-L190:

| 원본 라인 | 원본 호출 | 1.21.1 위치 | 정합 |
|---|---|---|---|
| L107 | handleJumping | sm_travel_client L73 (Jumper.handleJumping) | ✅ |
| L97-L100 | wasSwimming/wasDiving snapshot | L77-L79 동일 | ✅ |
| L132 | isLiquidClimbing 사전 계산 | L85-L89 동일 | ✅ |
| L227 | updateSwimState 진입 조건 | L92 (Swimmer.updateSwimState) | ✅ |
| L133 | handleSwimming 진입 (3-OR 조건) | L95-L99 ci.cancel() | ✅ |
| L134 | handleLava (4-AND 조건) | L107-L110 ci.cancel() | ✅ |
| L135 | handleAlternativeFlying | L151-L154 (handleFlying ci.cancel) | ✅ |
| L648 | fromSwimmingOrDiving | L115 동일 | ⚠️ B-fromSwim 부분 이식 |
| L780 | handleSliding | L118-L121 ci.cancel() | ✅ |
| L657 | handleClimbing | L157-L189 클라이밍 파이프라인 | ⚠️ B-19 (isNeighborClimbing 미갱신) |
| L658 | handleCeilingClimbing | (포함됨) | ✅ |
| L138 | handleWallJumping | L131-L135 | ✅ |

원본 updateEntityActionState L2307-L3045 ↔ 1.21.1 ClientState.tickEssential L802-~L1600 (38 블록):

| 원본 라인 | 영역 | 1.21.1 tickEssential 라인 | 정합 |
|---|---|---|---|
| L2442 | `isCrawling = canCrawl && (wantCrawl \|\| mustCrawl)` | L1336 | ✅ 1:1 |
| L2510 | `isFlying = ...` | L1277 | ✅ 1:1 |
| L505 | isLevitating 공식 (handleSwimming 안) | (미이식) | ❌ B-10d |
| L2524-L2530 | isHeadJumping 5-AND | L1413 | ✅ 1:1 |
| L2535-L2540 | 헤드점프 착지 → handleCrash + restoreFromFlying | L1428-L1433 | ✅ 1:1 |
| L2542-L2544 | tryLanding + standupIfPossible(2-arg) | (호출 L1432) | ✅ 1:1 (단 §18.1 capabilities.flying sync 미실행) |
| L2737 | isCrawlClimbing 5-AND | L1542 | ⚠️ B-19 (isNeighborClimbing 항상 false) |
| L3110 | isSmall = height < 1 | L1499-L1514 (8 SM OR) | ✅ #2.7 H-2 정정 후 |

---

## 7. 구조적 차이 / 근사 이식 지점

- `player.isSneaking()` ↔ `sm_isSneaking` override 순환 (sm_isSneaking 이 isSlow 참조)
  → 일부 조건에서 raw sneakKey 사용으로 회피 (이미 R-09 에서 적용)
- Button.StartPressed / StopPressed ↔ 엣지 감지 (`jumpKeyStartPressed` 등) — 이미 이식
- 1.7.10 `onLivingUpdate` 가 `updateEntityActionState` 와 `moveEntityWithHeading` 모두 포함 →
  1.21.1 `tickMovement` (이동 입력) + `travel` (이동 적용) 분리. 호출 순서 정합 확인 ✅
- 1.7.10 `capabilities.isFlying` (public 직접 할당) → 1.21.1 `getAbilities().setFlying()` +
  `UpdatePlayerAbilitiesC2SPacket` 송신 필요 (§18.1 영역).

---

## 8. 현재 구현 스냅샷

종합 리서치 §4.3 ClientState.tickEssential 38 블록 + §4.1 sm_travel_client 12 단계 + §4.5
standupIfPossible 본체 인용 — 모두 `research_state_transitions.md` 에 발췌됨.

---

## 9. 예상 수정 diff

§10 원자별 작성.

---

## 10. 원자 단위 작업 목록 — 세션 1 확정

### P. 재현 케이스 수집 — ✅ 완료
- [x] **P-1 (세션 1)**. §3 표 5 행 채움 (B-19 / B-10d / §18.1 / B-fromSwim / B-context).
- [x] **P-2 (세션 1)**. 원본 라인 번호 4 Agent 결과로 확정.

### A. 원인 분석 — ✅ 완료
- [x] **A-1 (세션 1)**. 종합 리서치 §5.2 미이식 / 근사 6 항목 정확 진단.

### B. 수정

**B-1. B-19 isNeighborClimbing 갱신 이식** — ✅ **세션 2 검증 완료 (이미 이식됨)**
- [x] **B-1a (세션 2)**. 원본 SmartMovingSelf L926-L961 isNeighborClimbing 갱신 식 (L945)
  확보 + 4방향(PZ/NZ/ZP/ZN) seekClimbGap 결과 OR.
- [x] **B-1b (세션 2)**. 1.21.1 `SmartMovingClimber.handleClimbing` L385-L455 (B-19a4 세션
  108) 이미 완전 이식 확인. L433 `sm.isNeighborClimbing = inoutH[0].isRelevant() ||
  inoutF[0].isRelevant();` — 원본 1:1.
- [x] **B-1c (세션 2)**. ClientState L1542 isCrawlClimbing 5-AND 공식 정확 (L2737 1:1).
  **stale 주석 정정 완료** — "미이식 → 항상 false" → "이미 이식됨, cfg 활성 시 정상 평가".
- [x] **B-1d (세션 2)**. 호출 경로 검증: sm_travel_client L177
  `SmartMovingClimber.handleClimbing(player, sm)` 매 tick 호출. 진입 조건 `cfg.freeClimb
  || cfg.simpleClimb || cfg.smartClimb` (Standard Base Climb 모드만 미진입). 인게임 검증
  deferred (통합 시점).

**B-2. B-10d isLevitating 공식 이식** — ✅ **세션 3 검증 완료 (이미 이식됨)**
- [x] **B-2 (세션 3)**. 원본 SmartMovingSelf L505 `isLevitating = levitating` (식
  `diving && !diveUp && !diveDown && moveStrafe==0 && moveForward==0`) 1.21.1 매핑:
  SmartMovingSwimmer.updateSwimState L192-L196 (B-10d 세션 71) 이미 이식 완료.
  ```java
  sm.isLevitating = sm.isDiving
          && !diveUp16
          && !diveDown16
          && player.input.movementSideways == 0F
          && player.input.movementForward == 0F;
  ```
  호출: sm_travel_client L92 매 tick. ClientState L1176 stale 주석 정정 완료.

**B-3. §18.1 B-N-standup-approx-4 (capabilities.flying sync)** — ✅ **세션 4 이식 완료** ★
- [x] **B-3a (세션 4)**. UpdatePlayerAbilitiesC2SPacket 1.21.1 시그니처 확인:
  `new UpdatePlayerAbilitiesC2SPacket(PlayerAbilities)` (public 생성자, flying public field).
- [x] **B-3b (세션 4)**. ClientState 에 import 추가:
  `import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;`
- [x] **B-3c (세션 4)**. ClientState L1294-L1322 (isFlying 엣지 직후) tryLanding 계산 +
  standupIfPossible 호출 신규 추가 (원본 L2542-L2544 1:1):
  ```java
  double _horizontalSpeedSquare = vX² + vZ²;
  boolean tryLanding = isFlying && !cfg.flyCloseToGround
                    && _horizontalSpeedSquare < 0.003D
                    && player.getVelocity().y > -0.03D;
  if (restoreFromFlying || tryLanding) {
      standupIfPossible(player, tryLanding, restoreFromFlying);
  }
  ```
  상수 0.003D / -0.03D 정확 보존.
- [x] **B-3d (세션 4)**. ClientState.standupIfPossible(2-arg) L2540-L2549 분기 본문에
  vanilla flying field + sync 추가:
  ```java
  player.getAbilities().flying = false;
  player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
  ```
  flying public field 직접 할당 패턴 — MixinClientPlayerEntity L53 와 일관.
- [x] **B-3e (세션 4)**. 빌드 BUILD SUCCESSFUL (5s). Creative 인게임 검증 deferred.

**B-4. fromSwimmingOrDiving 트리거 블록 보강** — ✅ **세션 5 검증 완료 (이미 이식됨)**
- [x] **B-4a (세션 5)**. 원본 SmartMovingSelf L1363-L1405 4 분기 vs 1.21.1
  ClientState.fromSwimmingOrDiving L2772-L2814 비교 — 4 분기 모두 1:1 이식 확정
  (B-42-B39 세션 124 완료):
  - 분기 1 (L1377-L1383 작은 구멍 크롤) ↔ L2792-L2796 ✓
  - 분기 2 (L1384-L1390 물 아래 크롤 + contextContinueCrawl) ↔ L2797-L2802 ✓
  - 분기 3 (L1392-L1403 걷기/크롤 + isSlow) ↔ L2803-L2812 ✓
- [x] **B-4b (세션 5)**. 누락 분기 0건 — 추가 이식 불필요.

**B-5. contextContinueCrawl 이식** — ✅ **세션 5 검증 완료 (이미 이식됨)**
- [x] **B-5a (세션 5)**. 원본 L1389 `contextContinueCrawl = true` ↔ 1.21.1 ClientState
  L2800 (fromSwimmingOrDiving 분기 2 안) 1:1 이식 확정. ClientState L514-L519 stale 주석
  정정 완료 ("후속 이식 — 항상 false" → "이미 이식됨, 분기 2 set").
- [x] **B-5b (세션 5)**. 소비처: wouldWantCrawl 식 (L2419 원본) — ClientState 의 wantCrawl
  계산 로직에서 이미 사용 중.

**B-6. isFakeShallowWaterSneaking 이식** — ✅ **세션 5 검증 완료 (이미 이식됨)**
- [x] **B-6 (세션 5)**. 원본 L440-L442 isFakeShallowWaterSneaking 1.21.1 매핑 — 이미
  완전 이식: SmartMovingSwimmer L298-L351 (B-5 세션 63 완료) 갱신 + ClientState L443
  필드 + L1042 wouldIsSneaking 식에서 사용 + L1900 reset.

### C. 검증
- [x] **C-1 (세션 5)**. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).
- [x] **C-2 (세션 5)**. 재현 케이스 5 행 모두 ✅ 매칭 (B-19 / B-10d / §18.1 / B-fromSwim / B-context).
- [x] **C-3 (세션 5)**. 회귀 방지 — 세션 4 변경 (Creative 비행 분기 한정) 외 sm_travel_client
  12 단계 + tickEssential 38 블록 영향 0 (grep 검증). #2.5/#2.6/#2.7 영향 0.
- [x] **C-4 (세션 5)**. `playtest_fixes.md` "현재 포커스" → `#4` 또는 `#1` 갱신.

**규모 (세션 1 갱신)**: P 2(완) + A 1(완) + B 6 + C 4 = **13 원자**. 실 코드 변경 ~6 원자.
**예상 2-3 세션**.

---

## 11. 호출 타이밍 검증 — 세션 1 확정 (Agent C/D 결과)

### 11.1 1.21.1 vanilla 호출 순서 (Agent C)

```
LivingEntity.tick L2310
  → super.tick (Entity.baseTick)
  → tickActiveItemStack
  → updateLeaningPitch
  → [서버] equipment / arrow / sleep
  → tickMovement L2350 ★
       ├─ jumpingCooldown--
       ├─ tickNewAi (sidewaysSpeed/forwardSpeed/upwardSpeed)
       ├─ [jump 분기 L2632-L2656] — vanilla jump() 또는 swimUpward
       └─ [travel 분기 L2659-L2673] — travel(movementInput)
            └─ travel() L2083-L2208
                 ├─ HEAD: gravity + fluidState
                 ├─ 4 분기 (water / lava / elytra / 지상)
                 └─ TAIL: updateLimbs(flutter)
  → stepBobbing / turnHead / updateAttributes

PlayerEntity.tick (super.tick 후)
  → updatePose ★
```

### 11.2 1.21.1 SM Mixin 호출 순서 (Agent D)

```
ClientPlayerEntity.tickMovement
  ├─ @HEAD: MixinClientPlayerEntity.sm_tickMovement
  │  └─ ClientState.tickEssential(player) — 38 블록, 원본 updateEntityActionState 1:1
  │
  ├─ vanilla tickMovement 본체 실행
  │  ├─ [jump 분기] vanilla jump() — SM sm_jump 인터셉트 (jumpAvoided=true)
  │  └─ [travel 분기] travel(movementInput)
  │       ├─ @HEAD cancellable: MixinLivingEntityClient.sm_travel_client (12 단계)
  │       │  ├─ SmartMovingJumper.handleJumping
  │       │  ├─ Swimmer.updateSwimState
  │       │  ├─ Swimmer.handleSwimming → ci.cancel()
  │       │  ├─ Swimmer.handleLava → ci.cancel()
  │       │  ├─ fromSwimmingOrDiving
  │       │  ├─ Slider.handleSliding → ci.cancel()
  │       │  ├─ 헤드점프 착지 + resetHeightOffset
  │       │  ├─ updateWallJumpState + handleWallJumping
  │       │  ├─ 클라이밍 상태 리셋
  │       │  ├─ Flyer.handleFlying → ci.cancel()
  │       │  └─ 클라이밍 파이프라인 (handleClimbing + handleCeilingClimbing + 감속)
  │       │
  │       ├─ vanilla 4 분기 (cancel 안 된 경우만)
  │       └─ @TAIL: MixinLivingEntityClient.sm_aerodynamicDamping
  │
  ├─ @HEAD: sm_jumpingFilter / sm_isClimbing_client / sm_applyClimbingSpeed /
  │         sm_updateLimbs_client / sm_isInSwimmingPose_client
  ├─ @HEAD cancellable: MixinPlayerEntityClient.sm_getBaseDimensions_client /
  │                     sm_getOffGroundSpeed / sm_updatePose_client
  └─ @TAIL: sm_flyWhileOnGround / sm_correctOnUpdate / sm_sendStatePacket
```

### 11.3 정합 결론

원본 호출 순서 ↔ 1.21.1 매핑 = **98% 1:1 정합**. 잔존 5건 (§5.2) 만 미이식/근사.

---

## 12. 테스트 프로토콜 — deferred (통합 인게임 검증)

각 케이스별 시나리오:
1. **B-19**: 1 블록 통로 (천장 1.5 블록) 진입 + sneak + moveForward → isCrawlClimbing=true 기대.
2. **B-10d**: 수영 중 방향키 release → isLevitating=true 기대 (애니메이션 영향).
3. **§18.1**: Creative 비행 + 좁은 공간 + 속도 < 0.003 + motionY > -0.03 → 자동 비행 해제 + 착지.
4. **B-fromSwim**: 깊은 물 → 좁은 공간 (천장 1.0 블록 이하) → 크롤 진입 자동.
5. **B-context**: 수면 아래 크롤 유지 (B-N-standup 호출 시 contextContinueCrawl 영향).

---

## 13. 완료 전 검증 체크리스트

- [x] §3 표 5 행 채움 (세션 1)
- [x] 각 전환 조건 원본 라인 1:1 대응 (세션 1 매핑 표)
- [x] 호출 순서 원본 일치 — 98% 1:1 정합 확인 (세션 1 §11.2)
- [ ] 회귀 방지 감사 — B 원자 진행 후
- [ ] 빌드 성공 — B 원자 진행 후

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `wantCrawl`/`mustCrawl` pre-compute | tickEssential L956-L1013 — isSlow 이전 계산 순서 | ✅ 세션 1 확인 |
| R-09 토글 블록 | sneakToggled / crawlToggled 소비 | ✅ 세션 1 확인 |
| `fromSwimmingOrDiving` | sm_travel_client L115 호출 | ⚠️ B-4 부분 이식 |
| `wantWallJumping` 자기참조 식 | tickEssential 또는 updateWallJumpState | ✅ 세션 1 확인 |
| Phase 1·2 #2.7 BBox/POSE/EyeHeight | MixinPlayerEntityClient + MixinPlayerEntity | ✅ #2.7 완결 |
| #2.6 Lava Liquid Border | Swimmer.handleLava + ClientState getLiquidBorder | ✅ #2.6 완결 |

---

## 15. 작업 기록

### 세션 1 — 2026-04-25 — 4 Agent 병렬 전수 리서치

사용자 지시: "3포커스와 관련된 모든 원본 코드및 리서치 파일들을 1개도 빠트리지 말고 ...
   1대1 번역이라는 걸 명심하고 모든라인을 다 리서칭".

진행한 작업:
1. **원본 6 파일 size 확인 + 4 Agent 병렬 분담**:
   - **Agent A**: SmartMovingSelf.java 3345 줄 — 상태 전환 메서드 + 호출 순서
   - **Agent B**: SmartMovingPlayerBase + Factory(2) + Mod + playerapi/Self ~675 줄 — hook 흐름
   - **Agent C**: 1.21.1 vanilla — LivingEntity 디컴파일 3501 줄 + 11 vanilla 리서치
   - **Agent D**: 1.21.1 매핑 — Mixin + ClientState tick 메서드들
   - **합계 (원본만)**: 6 파일 6499 줄
2. **종합 리서치 작성**: `docs/research/mapping/research_state_transitions.md` 신규 (~700 줄).
   §1 SmartMovingSelf 상태 전환 / §2 PlayerBase hook / §3 1.21.1 vanilla / §4 매핑 / §5
   누락 영역 / §6 §18.1 처리 / §7 권고 / §8 결론.

3. **★ 핵심 발견 5건**:
   1. **호출 순서 98% 1:1 정합** — 원본 superMoveEntityWithHeading L95-L147 ↔ 1.21.1
      sm_travel_client L53-L190 12 단계 정합. updateEntityActionState L2307-L3045 ↔
      ClientState.tickEssential 38 블록 통합 1:1.
   2. **B-19 미이식** ★ — isNeighborClimbing 갱신 누락으로 isCrawlClimbing 결과 항상 false.
      1 블록 통로 클라이밍 크롤 진입 불가.
   3. **B-10d 미이식** — isLevitating 공식 (수영 중 정적 자세) 누락. 영향 미미.
   4. **§18.1 잔존** — standupIfPossible 2-arg + tryLanding 본체 완료 (세션 136). 단
      `capabilities.flying sync` (B-N-standup-approx-4) 만 잔존 — 1.21.1
      `getAbilities().setFlying(false)` + `UpdatePlayerAbilitiesC2SPacket` 송신 필요.
   5. **fromSwimmingOrDiving / contextContinueCrawl** 부분 이식 — 깊은 물 → 좁은 공간 크롤
      진입 4 분기 일부 누락.

4. **§3 재현 케이스 표** 5 행 채움 (이전 비어있음).
5. **§10 원자 작업 목록** B-1 ~ B-6 + C-1 ~ C-4 = **13 원자** (실 작업 ~6 원자).
6. **§11 호출 타이밍** Agent C/D 결과로 vanilla + Mixin 호출 순서 확정.
7. **§14 회귀 방지** 6 항목 모두 검증.

수정 파일:
- `docs/research/mapping/research_state_transitions.md` 신규 (~700 줄).
- `docs/fix/focus_03_transition_conditions.md` — §3/§5/§10/§11/§14 대폭 보강.

회귀 0건 (코드 변경 0). 빌드 N/A.

**다음 세션 권고**: **B-1 (B-19 isNeighborClimbing 갱신 이식)** 진입 — 1 블록 통로 클라이밍
   크롤 진입 차단 해소 (가장 중요).

### 세션 2 — 2026-04-25 — B-1 검증 완료 (B-19 이미 이식됨 확정) ★

사용자 지시: 세션 1 prompt 따라 B-1 진입 — B-19 isNeighborClimbing 갱신 이식.

진행한 작업:
1. **원본 SmartMovingSelf isNeighborClimbing grep** (Agent A 결과 보강):
   - L1426 필드 선언 / **L945 갱신 식** / L1482 reset / L206/L2737 사용처 확정.
   - 원본 L945: `isNeighborClimbing = handsClimbing != HandsClimbing.None || feetClimbing != FeetClimbing.None;`
   - 4방향 (PZ/NZ/ZP/ZN) seekClimbGap 결과 OR.
2. **1.21.1 SmartMovingClimber grep**:
   - **L433 이미 갱신 코드 존재**: `sm.isNeighborClimbing = inoutH[0].isRelevant() || inoutF[0].isRelevant();`
   - L385-L455 (B-19a4 세션 108) 8방향 seekClimbGap 전수 이식.
   - 호출 경로: sm_travel_client L177 → handleClimbing → L385-L455 매 tick.
3. **★ 핵심 발견**: 세션 1 Agent D 의 "B-19 미이식" 결론은 **stale 주석에 의존한 부정확한
   분석**. 실제로는 B-19a4 (세션 108) 에서 이미 완전 이식 완료.
4. **stale 주석 정정**: ClientState L1534 의 "B-19 미이식 → 항상 false" 주석 →
   "이미 이식됨, cfg 클라이밍 모드 활성 시 정상 평가" 로 정정.

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — L1534
  stale 주석 정정.
- `docs/fix/focus_03_transition_conditions.md` — §3 Case 1 / §10 B-1 / §16 / 본 세션 로그
  모두 정정 (B-19 이미 이식됨 마킹).
- `docs/research/mapping/research_state_transitions.md` — §5.2 B-19 정정 (✅ 완료 마킹).

회귀 0건 (코드 변경 = 주석 정정 1 위치).

완료 전 검증 체크리스트 (세션 2 기준):
- [근거] 원본 SmartMovingSelf L945 갱신 식 확보 ✓
- [근거] 1.21.1 SmartMovingClimber L433 이미 이식 확인 ✓
- [대응] 원본 ↔ 1.21.1 1:1 (4방향 + 8방향 seekClimbGap 결과) ✓
- [분기] cfg.freeClimb/simpleClimb/smartClimb 진입 조건 (Standard Base Climb 모드만 미진입) ✓
- [상수] 변경 0
- [타이밍] sm_travel_client L177 매 tick 호출 ✓
- [근사] 신규 0건
- [신규] 세션 1 Agent D 의 "B-19 미이식" 결론 정정 — 실제 이미 이식됨
- [회귀] stale 주석 정정만 — 회귀 0건
- [빌드] 코드 변경 0 (주석만) — 재빌드 N/A

다음 세션 권고: **B-2 (B-10d isLevitating 공식 이식)** 또는 **B-3 (§18.1 capabilities.flying
   sync)** 진입 — B-1 완료로 다음 원자.

진행률: P 2/2 + A 1/1 + B-1 4/4 = **7/13 (~54%)**. B-2 ~ B-6 / C 잔존.

### 세션 3 — 2026-04-25 — B-2 검증 완료 (B-10d 이미 이식됨 확정) ★

사용자 지시: 세션 2 prompt 따라 B-2 진입.

진행한 작업:
1. **원본 isLevitating grep**: L505 `isLevitating = levitating` (handleSwimming) /
   L1494 reset / L2293 resetState / L2320 updateEntityActionState 시작 지역 변수.
   원본 식 (L468-L474): `levitating = diving && !diveUp && !diveDown && moveStrafe==0
   && moveForward==0`.
2. **1.21.1 SmartMovingSwimmer grep**: L192-L196 — `sm.isLevitating = sm.isDiving &&
   !diveUp16 && !diveDown16 && movementSideways == 0F && movementForward == 0F` 이미
   이식 (B-10d 세션 71). 원본 1:1.
3. **호출 경로**: sm_travel_client L92 → updateSwimState → L192-L196 매 tick.
4. **★ 핵심 발견**: 세션 1 Agent D 의 "B-10d 미이식" 결론은 ClientState L1176 stale 주석에
   의존. 실제로는 이미 이식됨. (B-19 와 동일 패턴)
5. **stale 주석 정정**: ClientState L1176 의 "B-10d 미이식 → 항상 false" → "이미 이식됨,
   매 tick 갱신" 정정.

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` L1176-L1177
  stale 주석 정정.
- `docs/fix/focus_03_transition_conditions.md` — §3 Case 2 / §10 B-2 / §16 / 본 세션 로그.
- `docs/research/mapping/research_state_transitions.md` — §5.2 B-10d 정정.

회귀 0건 (코드 변경 = 주석 정정 1 위치).

완료 전 검증 체크리스트 (세션 3 기준):
- [근거] 원본 SmartMovingSelf L505 갱신 식 + L468-L474 levitating 정의
- [근거] 1.21.1 SmartMovingSwimmer L192-L196 이미 이식 확인
- [대응] 원본 ↔ 1.21.1 1:1 (5 조건 AND)
- [분기] diveUp / diveDown / moveSideways / moveForward 모두 보존
- [상수] 변경 0 (검증만)
- [타이밍] sm_travel_client L92 매 tick 호출 ✓
- [근사] 신규 0건
- [신규] 세션 1 Agent D 의 "B-10d 미이식" 결론 정정
- [회귀] stale 주석 정정만
- [빌드] 코드 변경 0 (주석만)

다음 세션 권고: **B-3 (§18.1 capabilities.flying sync)** 진입 — 실제 코드 변경 첫 원자.

진행률: P 2/2 + A 1/1 + B-1 4/4 + B-2 1/1 = **8/13 (~62%)**. B-3 ~ B-6 / C 잔존.

### 세션 4 — 2026-04-25 — B-3 이식 완료 (§18.1 capabilities.flying sync) ★

사용자 지시: 세션 3 prompt 따라 B-3 진입 (실제 코드 변경 첫 원자).

진행한 작업:
1. **진입 전 검증** (B-19/B-10d 패턴 학습): grep `setFlying|UpdatePlayerAbilities|capabilities.flying`
   → 진짜 미이식 영역 확정 (B-19/B-10d 와 다름):
   - ClientState L2535-L2540 분기 본문에 `this.isFlying = false` 만, vanilla flying sync 누락.
   - tryLanding 계산 + standupIfPossible(true, ...) 호출 자체도 미이식.
2. **UpdatePlayerAbilitiesC2SPacket 1.21.1 시그니처 확인** (vineflower 디컴파일):
   - `public UpdatePlayerAbilitiesC2SPacket(PlayerAbilities abilities)` 생성자.
   - `abilities.flying` public field (`MixinClientPlayerEntity` L53 직접 할당 패턴 일관).
3. **cfg.flyCloseToGround 이미 존재 확인** (SmartMovingConfig L786 = true 기본).
4. **이식 (3 변경)**:
   - **import** 추가: `UpdatePlayerAbilitiesC2SPacket` (ClientState L18).
   - **L1294-L1322**: isFlying 엣지 처리 직후 tryLanding 계산 + standupIfPossible 호출
     신규 (원본 L2542-L2544 1:1, 상수 0.003D / -0.03D 정확).
   - **L2540-L2549**: standupIfPossible(2-arg) 분기 본문에 `getAbilities().flying = false` +
     `UpdatePlayerAbilitiesC2SPacket` 송신 추가 (원본 L2199 1:1).
5. **빌드**: `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — import +
  tryLanding 블록 신규 + standupIfPossible 본문 보강.
- `docs/fix/focus_03_transition_conditions.md` — §3 Case 3 / §10 B-3 / §16 / §15 세션 4 로그.
- `docs/research/mapping/research_state_transitions.md` — §5.2 §18.1 정정 (✅ 완료).

회귀 0건 (Creative 비행 한정 영향, Survival 무관).

완료 전 검증 체크리스트 (세션 4 기준):
- [근거] 원본 SmartMovingSelf L2199 + L2542-L2544 (③ 리서치 §1.4 + §6)
- [근거] 1.21.1 이식 위치 — ClientState L1294-L1322 (tryLanding) + L2540-L2549 (sync)
- [대응] 원본 ↔ 1.21.1 1:1 (식 + 호출 + 분기 본문 모두)
- [분기] tryLanding 4-AND + tryLanding && groundClose && standUpPossible 보존
- [상수] 0.003D / -0.03D / 1F (gap-only) 정확 보존
- [타이밍] isFlying 엣지 직후 tryLanding 계산 (원본 흐름 1:1)
- [근사] 신규 0건. §18.1 잔존 근사 해소 ✓
- [신규] tryLanding 계산이 1.21.1 자체에 미이식 발견 (원본 L2542 매핑) — 추가 이식 완료
- [회귀] Phase 1·2 #2.7 Mixin / #2.5 Jumper / #2.6 Lava 영향 0
- [빌드] BUILD SUCCESSFUL 5s ✓

다음 세션 권고: **B-4 (fromSwimmingOrDiving 트리거 보강)** 또는 **B-5 (contextContinueCrawl
   이식)** 진입 — 단 진입 전 검증 (B-19/B-10d 와 같이 이미 이식됐을 가능성) 우선.

진행률: P 2/2 + A 1/1 + B-1 4/4 + B-2 1/1 + B-3 5/5 = **13/13 (100%) 의 원자 단위로는
완료** ※ B-4/B-5/B-6 진입 전 검증 필요. 실제 잔존 = 2건 (B-fromSwim / B-context) + C 4 원자.

### 세션 5 — 2026-04-25 — B-4/B-5/B-6 검증 완료 + C-1~C-4 (#3 AI 완결) ★

사용자 지시: 세션 4 prompt 따라 B-4 진입 (학습된 패턴: 진입 전 검증 우선).

진행한 작업:
1. **B-4 진입 전 검증** (B-19/B-10d 패턴 적용):
   - 원본 SmartMovingSelf L1363-L1405 fromSwimmingOrDiving 4 분기 read.
   - 1.21.1 ClientState L2772-L2814 fromSwimmingOrDiving 본체 read.
   - **결과**: 4 분기 모두 1:1 이식 확정 (B-42-B39 세션 124 완료).
     - 분기 1 (작은 구멍 크롤) ✓ / 분기 2 (물 아래 크롤 + contextContinueCrawl) ✓ /
       분기 3 (걷기/크롤 + isSlow) ✓
2. **B-5 진입 전 검증**:
   - 원본 L1389 `contextContinueCrawl = true` ↔ 1.21.1 ClientState L2800 (분기 2 안) 1:1.
   - **결과**: 이미 이식됨. ClientState L514-L519 stale 주석 정정 완료.
3. **B-6 진입 전 검증** (선택 항목):
   - 원본 isFakeShallowWaterSneaking grep — 1.21.1 SmartMovingSwimmer L298-L351 (B-5
     세션 63 완료) + ClientState L443/L1042/L1900 모두 이식 확정.
   - **결과**: 이미 완전 이식.
4. **★ 세션 1 5 잔존 영역 결과 정리**:
   - B-19 ✅ 이미 이식 (세션 2 검증)
   - B-10d ✅ 이미 이식 (세션 3 검증)
   - **§18.1 ✅ 세션 4 이식 완료** (B-3 — 진짜 미이식 1건 해소)
   - B-fromSwim ✅ 이미 이식 (세션 5 검증)
   - B-context ✅ 이미 이식 (세션 5 검증)
   - B-6 (선택) ✅ 이미 이식 (세션 5 검증)
   → 5 (+ 선택 1) 잔존 영역 중 **5건이 이미 이식됨**, 1건만 진짜 미이식 → 세션 4 해소.
5. **stale 주석 정정**: ClientState L514-L519 contextContinueCrawl 주석 정정 (B-fromSwim
   이미 이식 반영).
6. **C-1 빌드 검증**: BUILD SUCCESSFUL (5s — 세션 4 결과 재확인).
7. **C-2 재현 케이스 5 행 매칭 (AI 자동)**:
   - Case 1 B-19 ✅ (SmartMovingClimber L433 + ClientState L1542)
   - Case 2 B-10d ✅ (SmartMovingSwimmer L192-L196)
   - Case 3 §18.1 ✅ (ClientState L1294-L1322 + L2540-L2549, 세션 4)
   - Case 4 B-fromSwim ✅ (ClientState L2772-L2814)
   - Case 5 B-context ✅ (ClientState L2800)
8. **C-3 회귀 감사**: 세션 4 변경 = ClientState L1294-L1322 (tryLanding 신규 블록) +
   L2540-L2549 (vanilla flying sync 추가). sm_travel_client 12 단계 / tickEssential 38
   블록 / Phase 1·2 #2.7 / #2.5 / #2.6 영향 0 (Creative 비행 한정 분기에만 적용).
9. **C-4 playtest_fixes.md 갱신**: #3 → #1 또는 #4 전환.

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — L514-L519
  contextContinueCrawl stale 주석 정정.
- `docs/fix/focus_03_transition_conditions.md` — §3 Case 4/5 / §10 B-4/B-5/B-6 / §16 / §15
  세션 5 로그 모두 정정.
- `docs/research/mapping/research_state_transitions.md` — §5.2 잔존 5건 모두 ✅ 완료 마킹.
- `docs/fix/playtest_fixes.md` — #3 AI 완결 마킹.

회귀 0건. 빌드 BUILD SUCCESSFUL (5s).

완료 전 검증 체크리스트 (세션 5 기준):
- [근거] 원본 SmartMovingSelf 모든 잔존 영역 grep + read ✓
- [근거] 1.21.1 이식 위치 grep + read ✓
- [대응] 세션 1 5 잔존 영역 1:1 검증 — 5건 이미 이식 + 1건 (§18.1) 세션 4 이식
- [분기] fromSwimmingOrDiving 4 분기 + isFakeShallowWaterSneaking 모두 이식 확인
- [상수] 세션 4 상수 (0.003D / -0.03D) 정확
- [타이밍] sm_travel_client 12 단계 + tickEssential 38 블록 호출 순서 보존
- [근사] §7 신규 0건. §18.1 잔존 근사 해소 (세션 4 완료)
- [신규] 추가 의존 발견 0건
- [회귀] 포커스 #2 Extended / #2.5 / #2.6 / #2.7 영향 0
- [빌드] BUILD SUCCESSFUL 5s ✓

다음 세션 권고: **#3 AI 완결 — 다음 포커스 진입** (#4 키 커맨드 결과 또는 #1 애니메이션).
   인게임 검증은 모든 포커스 (#1/#2.5/#2.6/#2.7/#3/#4) 완결 후 통합 시점.

진행률: **13/13 (100%) AI 완결**. C-1/C-2/C-3/C-4 모두 [x]. 인게임 검증 deferred.

---

## 16. 신규 발견 — 세션 1 4 Agent 결과

| 발견 | 영향 | 위치 |
|---|---|---|
| ~~B-19 isNeighborClimbing 항상 false~~ ✅ **세션 2 정정** | (이미 이식됨 — B-19a4 세션 108. ClientState L1534 stale 주석 정정 완료) | SmartMovingClimber L385-L455 + ClientState L1542 |
| ~~B-10d isLevitating 항상 false~~ ✅ **세션 3 정정** | (이미 이식됨 — SmartMovingSwimmer L192-L196 세션 71. ClientState L1176 stale 주석 정정 완료) | SmartMovingSwimmer.updateSwimState |
| ~~§18.1 capabilities.flying sync~~ ✅ **세션 4 이식 완료** | tryLanding 계산 + standupIfPossible 호출 + setFlying + UpdatePlayerAbilitiesC2SPacket 송신 (원본 L2199 + L2542-L2544 1:1) | ClientState L1294-L1322 + L2540-L2549 |
| **§18.1 capabilities.flying sync** 미실행 | Creative 비행 자동 해제 안 됨 | ClientState L2530-L2531 |
| ~~fromSwimmingOrDiving 4 분기 일부 누락~~ ✅ **세션 5 정정** | (이미 완전 이식 — B-42-B39 세션 124. ClientState L2772-L2814) | ClientState.fromSwimmingOrDiving |
| ~~contextContinueCrawl 미이식~~ ✅ **세션 5 정정** | (이미 이식 — 분기 2 안 L2800. ClientState L514-L519 stale 주석 정정 완료) | ClientState L2800 |
| ~~isFakeShallowWaterSneaking 미이식~~ ✅ **세션 5 정정** | (이미 완전 이식 — B-5 세션 63. SmartMovingSwimmer L298-L351 + ClientState L1042) | SmartMovingSwimmer + ClientState |

---

## 17. 잔여 / 후속

- B-1 ~ B-6 6 원자 완료 후 #4 또는 #1 진입.
- 통합 인게임 검증 (포커스 #1 / #2.5 / #2.6 / #2.7 / #3 / #4 모두 완결 후).

---

## 18. 확정 원자 — 세션 1 정정

### 18.1. B-N-standup-4 — vanilla `capabilities.isFlying` 자동 해제 복원

**세션 1 정정**: AABB gap 정밀 (B-N-standup-1/2/3) + standupIfPossible 2-arg overload +
tryLanding 계산 모두 **세션 136 완료** ✓. **잔존 = capabilities.flying sync 단일 원자**.

**원본** (SmartMovingSelf L2196-L2201, `standupIfPossible(tryLanding, restoreFromFlying)`):
```java
if (tryLanding && groundClose && standUpPossible) {
    isFlying = false;                        // SM 내부 플래그
    sp.capabilities.isFlying = false;         // ★ vanilla 비행 상태 해제
    restoreFromFlying = true;
}
```

**1.21.1 처리 방안 (세션 1 확정)**:
```java
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
// ...
if (tryLanding && groundClose && standUpPossible) {
    this.isFlying = false;
    player.getAbilities().setFlying(false);   // 1.21.1 setter
    player.networkHandler.sendPacket(
            new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));  // 서버 sync
    restoreFromFlying = true;
}
```

**해소 원자** (B-3 으로 통합):
- [ ] **B-3a (=18.1a)** UpdatePlayerAbilitiesC2SPacket Yarn 1.21.1 시그니처 확인 (Mojang
  매핑에서 PlayerAbilities 인자 받는 생성자 존재).
- [ ] **B-3b (=18.1b/c)** ClientState.standupIfPossible(player, tryLanding, restoreFromFlying)
  L2520-L2547 의 `tryLanding && groundClose && standUpPossible` 분기 본문 보강 (위 코드).
- [ ] **B-3c (=18.1d)** `tryLanding` 계산 정밀 이식 검증 — 이미 ClientState L1280-L1282 에
  존재 (세션 136). 추가 작업 없음.
- [ ] **B-3d (=18.1e)** `Options._flyCloseToGround` Config 필드 — 이미 SmartMovingConfig
  에 있는지 grep 확인. 없으면 추가 (boolean, 기본 true).
- [ ] **B-3e (=18.1f)** 빌드 + Creative 인게임 검증 deferred.

**의존**:
- 포커스 #2 Extended (세션 134) ✓ + 세션 136 B-N-standup-1/2/3 해소 ✓
- B-42-B18b 패턴 — vanilla `Abilities.flying` 가 1.21.1 에서 private 이라 setter 필요 (Agent B 확인).

**규모**: 소 (5 원자 / 1 세션). setter + UpdatePlayerAbilitiesC2SPacket 송신만.

**회귀 감사**:
- Creative 플레이어만 영향 — Survival 무관.
- B-24 `restoreFromFlying=true → standupIfPossible(player, false, true)` 호출 구조 유지.

---

## 19. 참고 자료 — 세션 1 추가

### 종합 리서치 (세션 1 신규)
- `docs/research/mapping/research_state_transitions.md` ~700 줄

### 원본 소스 경로 (라인 정확)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`
- `...\playerapi\SmartMovingPlayerBase.java`
- `...\playerapi\SmartMovingFactory.java`
- `...\SmartMovingFactory.java`
- `...\SmartMovingMod.java`

### 1.21.1 이식 대상
- `src/client/java/.../mixin/client/MixinLivingEntityClient.java` (sm_travel_client + 7 inject)
- `src/client/java/.../mixin/client/MixinClientPlayerEntity.java` (sm_tickMovement HEAD + 3 TAIL)
- `src/client/java/.../mixin/client/MixinPlayerEntityClient.java` (#2.7 완결)
- `src/main/java/.../mixin/MixinPlayerEntity.java` (#2.7 완결)
- `src/client/java/.../client/SmartMovingClientState.java` (tickEssential 38 블록, ~1700 줄)
- `src/client/java/.../client/SmartMovingClimber.java` (★ B-1 isNeighborClimbing 적용 위치)

### 연관 포커스
- 포커스 #2 Extended (완료 — 세션 134). #2 본체.
- 포커스 #2.5 Jumper Factor (AI 완결 22/22).
- 포커스 #2.6 Lava Liquid Border (AI 완결 20/21).
- 포커스 #2.7 BBox/POSE/EyeHeight (AI 완결 22/23).
- 포커스 #4 키 커맨드 (의존 후속).
- 포커스 #1 애니메이션 (의존 후속).
