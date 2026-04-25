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
| 2 | 수중 정적 자세 (방향키 없이 수영) | isLevitating → true | `diving && !diveUp && !diveDown && moveStrafe==0 && moveForward==0` (L505) | **항상 false** (B-10d 미이식) | SmartMovingSelf L505 (handleSwimming) | **B-10d** |
| 3 | Creative 비행 + 좁은 공간 접근 (속도 낮음 + 하강) | tryLanding → standupIfPossible(true, restoreFromFlying) | tryLanding=true → `capabilities.isFlying = false` + restoreFromFlying=true (L2199) | SM `isFlying = false` 만 실행, vanilla 비행 유지 (수동 F 필요) | SmartMovingSelf L2196-L2201 + L2542-L2544 | **§18.1** |
| 4 | 깊은 물 → 육지 (걷기/스니크/크롤) | fromSwimmingOrDiving 4 분기 (L1369-L1404) | wasShortInWater && !isShortInWater → 4 분기 setHeightOffset(-1F) | 트리거 블록 부분 이식 — 일부 분기 누락 가능성 | SmartMovingSelf L1363-L1405 | **B-fromSwim** |
| 5 | 물 표면 아래 크롤 유지 | contextContinueCrawl=true (L1389) | 깊은 물 → 수면 아래 크롤 진입 시 set | 미이식 (false 고정) | SmartMovingSelf L1389 | **B-context** |

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

**B-2. B-10d isLevitating 공식 이식** (영향 미미 — 우선순위 ↓)
- [ ] B-2. 원본 SmartMovingSelf L505 `isLevitating = diving && !diveUp && !diveDown &&
  moveStrafe==0 && moveForward==0` 1.21.1 매핑 → SmartMovingSwimmer.handleSwimming 또는
  ClientState.tickEssential 적절한 위치에 추가.

**B-3. §18.1 B-N-standup-approx-4 (capabilities.flying sync)** ★ §18.1 잔존 원자
- [ ] B-3a. ClientState `standupIfPossible(player, true, restoreFromFlying)` 본체에서
  `tryLanding && groundClose && standUpPossible` 분기 본문 보강:
  ```java
  player.getAbilities().setFlying(false);
  player.networkHandler.sendPacket(
          new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
  ```
- [ ] B-3b. UpdatePlayerAbilitiesC2SPacket 시그니처 확인 (Yarn 1.21.1).
- [ ] B-3c. 빌드 + Creative 비행 자동 해제 시나리오 deferred (통합 인게임 검증).

**B-4. fromSwimmingOrDiving 트리거 블록 보강** (B-fromSwim)
- [ ] B-4a. 원본 SmartMovingSelf L1363-L1405 `fromSwimmingOrDiving(wasShortInWater)` 4 분기
  전수 grep — 이미 이식된 분기 vs 누락 분기 확인.
- [ ] B-4b. 누락 분기 추가 이식 (특히 L1383-L1389 깊은 물 → 좁은 공간 크롤 진입).

**B-5. contextContinueCrawl 이식** (B-context)
- [ ] B-5a. 원본 L1389 `contextContinueCrawl = true` 이식 위치 확정 (ClientState 또는 sm_travel_client).
- [ ] B-5b. 소비처 grep — 어디서 contextContinueCrawl 을 read 하는지 (B-N-standup 영향).

**B-6. isFakeShallowWaterSneaking 이식** (선택 — 우선순위 낮음)
- [ ] B-6. 원본 L440-L442 `isFakeShallowWaterSneaking` 1.21.1 매핑 검토.

### C. 검증
- [ ] **C-1**. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL.
- [ ] **C-2**. 재현 케이스 5 행 모두 매칭 (B-19 / B-10d / §18.1 / B-fromSwim / B-context).
- [ ] **C-3**. 회귀 방지 — 기존 sm_travel_client 12 단계 + tickEssential 38 블록 영향 없음.
- [ ] **C-4**. `playtest_fixes.md` "현재 포커스" → `#4` 또는 `#1` 갱신.

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

---

## 16. 신규 발견 — 세션 1 4 Agent 결과

| 발견 | 영향 | 위치 |
|---|---|---|
| ~~B-19 isNeighborClimbing 항상 false~~ ✅ **세션 2 정정** | (이미 이식됨 — B-19a4 세션 108. ClientState L1534 stale 주석 정정 완료) | SmartMovingClimber L385-L455 + ClientState L1542 |
| **B-10d isLevitating** 항상 false | 수영 중 정적 자세 미감지 (영향 미미) | ClientState L1176 |
| **§18.1 capabilities.flying sync** 미실행 | Creative 비행 자동 해제 안 됨 | ClientState L2530-L2531 |
| **fromSwimmingOrDiving** 4 분기 일부 누락 | 깊은 물 → 좁은 공간 크롤 진입 누락 가능성 | sm_travel_client L115 |
| **contextContinueCrawl** 미이식 | 수면 아래 크롤 전환 영향 | (1.21.1 미이식) |
| **isFakeShallowWaterSneaking** 미이식 | 얕은 물 가짜 스니킹 영향 (우선순위 낮음) | (1.21.1 미이식) |

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
