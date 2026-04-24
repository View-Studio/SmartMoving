# Focus #2 — 스마트무빙 상태 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #2 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟠 **Extended 진행 중** (세션 88 — 엄격 완료 결정 / Phase 3~8 범위 복원) |
| 현재 단계 | A/B Phase 1/2 핵심 54 원자 + C-1/C-2/C-3 완료. **세션 88 사용자 지시로 엄격 완료 방침 확정** — 세션 85 의 "별도 포커스 분리" 계획 취소. Phase 3~8 (B-19 도미노 해소 / B-10 공식 완성 / B-7/B-9/B-11 / AABB 정밀화 / B-48/B-49 / Simple·Smart Base Climb) 모두 포커스 #2 범위로 복원. **후속 작업은 [`focus_02_extended.md`](focus_02_extended.md) 로 분리**. C-4/C-5 는 Phase 3~8 완료 후. |
| 선행 의존 | 없음 (#5/#6 완료) |

---

## 2. 증상 — 원본 vs 현재

상태 플래그들(`isCrawling`/`isDipping`/`isSwimming_sm`/`isDiving`/`isClimbing`/
`isCrawlClimbing`/`isCeilingClimbing`/`isSliding`/`isHeadJumping`/`isFast`/`isSlow`/
`wasCrawling_st`/`contextContinueCrawl` 등)이 **잘못된 시점**에 true/false 로 됨.

해당 상태를 소비하는 다른 로직(애니메이션/전환 조건/키 커맨드)이 연쇄적으로
오동작. 따라서 이 포커스를 먼저 완료해야 #1/#3/#4 에서 원인-결과 혼동 없음.

---

## 3. 재현 케이스 표 (보조 참고용 — 세션 29 제약 완화)

**방식 전환** (세션 29): "진입 전 필수" 제약 완화. 포커스 #5/#6 처럼 **원본 grep + 1:1
코드 감사** 기반 진행. 재현 케이스 있으면 A-N 우선순위 판정용 보조, 없어도 감사 진행 가능.

각 행은 1 상태 × 1 상황 → 1 불일치.

| # | 시나리오 (입력/환경) | 대상 상태 | 원본 기대 | 1.21.1 실제 | 비교 기준 |
|---|-------------------|----------|---------|-----------|----------|
| 1 | (예) 얕은 물 진입 + sneak | `isDipping` | true | false | 원본 `SmartMovingSelf.handleSwimming` 경계값 |
| 2 | (예) sneak + sprint + onGround | `isSlow` | ? | ? | `SmartMovingSelf.md` L2717 |
| 3 | | | | | |

**형식 안내**:
- "시나리오" 는 재현 가능한 키 입력 + 환경(물/공중/블록) 조합
- "원본 기대" 는 리서치 파일의 원본 코드 기반 (추측 금지, 근거 없으면 불명)
- "1.21.1 실제" 는 F3 디버그 / 로그 / 블록 반응 등으로 관찰
- "비교 기준" 은 원본 소스 라인 번호 / 리서치 파일 섹션

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | 없음 |
| 영향받는 후속 | #1 (애니메이션 입력 상태), #3 (전환 조건), #4 (키 커맨드 결과) |
| 영향 주는 완료 이식 | `SmartMovingClientState.tickEssential` 전체 / `SmartMovingSwimmer.updateSwimState` / 토글 블록 R-09 |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 상태 | 리서치 파일 | 줄 |
|------|------------|---|
| `isDipping` / `isSwimming_sm` / `isDiving` | `SmartMovingSelf.md` | L~1019 resetSwimming / L91-L100 필드 |
| `isCrawling` | `SmartMovingSelf.md` | L2419 wouldWantCrawl / L2474 `isCrawling = canCrawl && (wantCrawl \|\| mustCrawl)` |
| `isSlow` | `SmartMovingSelf.md` | L2711-2719 |
| `isFast` | `SmartMovingSelf.md` | 필드 선언 근처 + grab && sprint |
| `isHeadJumping` | `SmartMovingSelf.md` | L1607-1631 toSlidingOrCrawling / L2550 (헤드점프 진입) |
| `contextContinueCrawl` | `SmartMovingSelf.md` | L1388(=true) / L2411/L2416/L2447(=false) |
| `wasCrawling_st` / `wasSneaking` / `wasClimbCrawling` | R-09 블록 진입부 (저장 시점) | 1.21.1 tickEssential |

### 5.2. 확보된 원본 코드

- R-09 블록 전체는 `SmartMovingSelf.md` 에 덤프됨 (이전 세션)
- `handleSwimming` 의 offset 3분류 경계값 0.65/0.6/0.55/1.9 확보됨
- **R-15** (세션 36 — A-6 완료): 이력 3개 `wasCrawling_st`/`wasSneaking`/`wasClimbCrawling`
  전수 감사 — `SmartMovingSelf.md` R-15 섹션 참조. 범위: R-15.1 필드 선언 (이식 완료 +
  미이식 3건 확인) / R-15.2 저장 위치 맵 / R-15.3 저장 시점 동치성 분석 / R-15.4 R-09
  블록 종료부 누락 2건 / R-15.5 R-09 블록 세부 조건 전수 대조 / R-15.6 **불일치 4건** /
  R-15.7 B-43~B-44 원자 예비안. **A-6 는 대부분 기존 원자(B-4/B-22b/B-10d/B-2/B-18/B-33)
  에 흡수 — 신규 원자 B-43 (R-09 종료부) + B-44a~c (저장 시점 조정) 만 추가**.

- **R-14** (세션 35 — A-5 완료): `isCrawling`/`contextContinueCrawl` 전수 감사 —
  `SmartMovingSelf.md` R-14 섹션 참조. 범위: R-14.1 필드 선언 (3건 미이식) /
  R-14.2 갱신 위치 맵 (원본 19곳 + contextContinueCrawl 4곳) / R-14.3 tickEssential
  크롤 판정 블록 (mustCrawl + inputContinueCrawl + contextContinueCrawl 해제 +
  wouldWantCrawl + canCrawl 5-AND + 메인 공식) / R-14.4 1.21.1 IMPL-01 구조 차이 /
  R-14.5 toCrawling() 함수 / R-14.6 handleClimbing wall 오르기 / R-14.7 landMotionPost
  3분기 / R-14.8 grab.StartPressed 수영/크롤 3분기 / R-14.9 wasCrawling↔isCrawling
  전환 후처리 / R-14.10 **불일치 15건** / R-14.11 B-31~B-42 원자 예비안.

- **R-13** (세션 34 — A-4 완료): `isHeadJumping`/`isSliding` 전환 쌍 전수 감사 —
  `SmartMovingSelf.md` R-13 섹션 참조. 범위: R-13.1 필드 선언 (4건 미이식) /
  R-13.2 갱신 위치 맵 / R-13.3 isHeadJumping 매 틱 재평가 5-AND 공식 /
  R-13.4 isSliding 직접 진입 6-AND 조건 + 부수 동작 / R-13.5 fallDistance 분기 /
  R-13.6 toSlidingOrCrawling 조건 / R-13.7 isAerodynamic 이식 완료 확인 /
  R-13.8 미이식 필드 4건 / R-13.9 **불일치 13건** / R-13.10 B-22~B-30 원자 예비안.

- **R-12** (세션 33 — A-3 완료): `isClimbing`/`isCeilingClimbing`/`isCrawlClimbing`/
  `isClimbCrawling` 등반 4상태 전수 감사 — `SmartMovingSelf.md` R-12 섹션 참조. 범위:
  R-12.1 필드 선언 (9건 미이식 포함) / R-12.2 갱신 위치 맵 (원본 vs 1.21.1) /
  R-12.3 handleClimbing 구조 / R-12.4 handleCeilingClimbing 구조 /
  R-12.5 isCrawlClimbing 5-AND 공식 + 전환 블록 / R-12.6 isClimbCrawling 공식 +
  climbIntoCount 카운터 / R-12.7 isClimbHolding/wantClimbHolding 3-OR / R-12.8 의존 필드 /
  R-12.9 1.21.1 side-by-side / R-12.10 **불일치 14건** / R-12.11 B-14~B-21 원자 예비안.

- **R-11** (세션 32 — A-2 완료): `isSwimming_sm`/`isDiving`/`isDipping` 수중 3상태 전수
  감사 — `SmartMovingSelf.md` R-11 섹션 참조. 범위: R-11.1 필드 선언 / R-11.2 진입 조건 /
  R-11.3 사전 준비 (AABB + couldStandUp + wantShallowSwim) / R-11.4 크롤 3-OR 강제 isDipping /
  R-11.5 메인 분류 3-갈래 + A/B 서브 + 11-단계 offset 테이블 / R-11.6 크롤↔수영 전환 /
  R-11.7 Config 게이트 / R-11.8 수중 3상태 갱신 + isShallowDiveOrSwim / R-11.9 얕은 물
  특수 분기 / R-11.10 useStandard 경로 / R-11.11 리셋 위치 / R-11.12 side-by-side +
  **불일치 11건** (B-6~B-13 원자 예비안).

- **R-10** (세션 31 — B-0 완료): `isFast`/`isSlow`/`wouldIsSneaking` 의존 체인 완전 덤프
  — `SmartMovingSelf.md` L2508 이후 R-10 섹션 참조. 범위:
  - R-10.1 필드 선언 (L1413-L1448)
  - R-10.2 `disabled` (L2373-L2375)
  - R-10.3 `sneakContinueInput` / `wouldWantSneak` / `wantSneak` (L2576-L2590)
  - R-10.4 `moveButtonPressed` / `moveForwardButtonPressed` (L2592-L2593)
  - R-10.5 `wantSprint` 6조건 (L2595-L2615)
  - R-10.6 `isSprintJump` / `exhaustionAllowsSprinting` / `preferSprint` (L2633-L2657)
  - R-10.7 `isClimbSprintSpeed` + `can*` 4 + 6 Sprint 변종 (L2659-L2684)
  - R-10.8 `standing` + `isFast` + isGroundSprinting 전환 후처리 (L2686-L2709)
  - R-10.9 `wouldIsSneaking` + `isSlow` (L2711-L2719)
  - R-10.10 `isFast`/`isSlow` 전수 사용처 (grep)
  - R-10.11 1.21.1 이식 매핑 예비안 (Config 헬퍼 / KeyBinding / 순환 의존 주의)

### 5.3. 확보 필요 — 재현 케이스별 원본 해당 블록

재현 케이스 수집 후 각 케이스의 원본 계산 로직을 리서치 파일에서 확인.
부족하면 WebFetch:

```
대상 URL: SmartMovingSelf.java
추출 대상:
  1. <케이스 #N 에서 문제된 상태 필드> 가 갱신되는 모든 위치
  2. 주변 20줄 맥락 포함
```

---

## 6. 1:1 매핑 테이블 (A-7 세션 37 통합 완성)

R-10 ~ R-15 리서치 섹션의 전체 매핑을 이 §6 에 통합. B 단계 진행 시 이 표를 기준으로
이식. **범례**: ✓ 이식됨 / ✗ 미이식 / ⚠️ 부분/오역 / 🔄 근사

### 6.1 13 핵심 상태 필드

| 원본 심볼 (`SmartMovingSelf.java`) | 1.21.1 심볼 (`SmartMovingClientState`) | 상태 | 수정 원자 |
|---|---|---|---|
| `isCrawling` | `isCrawling` (L138?) | ⚠️ 메인 공식 구조 차이 | B-33 |
| `isDipping` | `isDipping` (L??) | ⚠️ 메인 분류 공식 완전 대체 | B-9 |
| `isSwimming` | `isSwimming_sm` (vanilla 충돌 회피 접미사) | ⚠️ 메인 분류 공식 / 얕은 물 분기 | B-9/B-11 |
| `isDiving` | `isDiving` | ⚠️ 메인 분류 공식 | B-9 |
| `isClimbing` | `isClimbing` | ✓ 진입/해제 이식 완료 | — (B-14 resetClimbing) |
| `isCeilingClimbing` | `isCeilingClimbing` | ⚠️ 진입 이식, 해제 엣지 누락 | B-14/B-21/B-38 |
| `isCrawlClimbing` | `isCrawlClimbing` (L135) | ✗ 갱신 로직 완전 미이식 | **B-17** |
| `isClimbCrawling` | `isClimbCrawling` (L150) | ✗ 갱신 로직 완전 미이식 | **B-18** |
| `isSliding` | `isSliding` | ⚠️ 직접 진입 6-AND 간소화 + 부수 동작 누락 | B-25/B-26/B-27/B-28 |
| `isHeadJumping` | `isHeadJumping` (L50) | ⚠️ 매 틱 재평가 5-AND 미이식 | **B-23/B-24** |
| `isFast` | `isFast` | ✗ 공식 완전 오역 (grab+isSprinting 단순) | **B-1f** |
| `isSlow` | `isSlow` | ⚠️ Config.isSneakingEnabled 가드 누락 + 중복 | **B-2** |
| `contextContinueCrawl` | `contextContinueCrawl` | ✓ 4곳 모두 이식 | — |

### 6.2 이력/이전 틱 스냅샷 필드

| 원본 | 1.21.1 | 상태 | 수정 원자 |
|---|---|---|---|
| `wasSneaking` (지역) | `wasSneaking` (L241) | ✓ 이식, 저장 시점 조정 가능 | B-44a |
| `wasClimbCrawling` (지역) | `wasClimbCrawling` (L247) | ✓ 이식, 저장 시점 조정 가능 | B-44c |
| `wasCrawling` (필드 L3074, 다용도) | `wasCrawling` (개명 완료 세션 41) | ✓ 필드 개명 완료, 나머지 갱신 위치는 B-33 이후 | B-44b |
| `wasHeadJumping` | — | ✗ 미이식 | **B-22a** |
| `wasRunning` | — | ✗ 미이식 | **B-22b** |
| `wasLevitating` | — | ✗ 미이식 (isLevitating 의존) | B-10d + B-43 |
| `wasGroundSprinting` | — | ✗ 미이식 (isGroundSprinting 의존) | B-1d |
| `wasCollidedHorizontally` | `wasCollidedHorizontally` | ✓ 이식 확인 | — |
| `wasCapabilitiesIsFlying` | `wasCapabilitiesIsFlying` (H-4) | ✓ 이식 확인 | — |

### 6.3 의존 필드 — A-1 isFast/isSlow 체인 (B-0/R-10)

| 원본 | 1.21.1 | 상태 | 수정 원자 |
|---|---|---|---|
| `isGroundSprinting` (L1439 public) | — (Jumper L350 로컬) | ✗ ClientState 필드 미이식 | B-1d |
| `isClimbSprinting` (지역) | — | ✗ 미이식 | B-1d |
| `isSwimSprinting` (지역) | — | ✗ 미이식 | B-1d |
| `isDiveSprinting` (지역) | — | ✗ 미이식 | B-1d |
| `isCeilingSprinting` (지역) | — | ✗ 미이식 | B-1d |
| `isFlyingSprinting` (지역) | — | ✗ 미이식 | B-1d |
| `canHorizontallySprint` (지역) | — | ✗ 미이식 | B-1c |
| `canAllSprint` (지역) | — | ✗ 미이식 | B-1c |
| `canAnySprint` (지역) | — | ✗ 미이식 | B-1c |
| `canVerticallySprint` (지역) | — | ✗ 미이식 | B-1c |
| `isClimbSprintSpeed` (지역) | — | ✗ 미이식 (SmartStatisticsFactory 의존) | B-1c |
| `standing` (지역) | — (로컬 변수) | ✗ 미이식 | B-1e |
| `disabled` (지역 L2375) | — | ✗ 미이식 | B-3a |
| `wantSprint` (L1415 public) | — (주석만 L106) | ✗ 미이식 | **B-3a** |
| `wantSneak` (지역 L2588) | — | ✗ 미이식 (Config.isSneakingEnabled 포함) | B-2 |
| `wouldWantSneak` (지역) | `wouldWantSneak` (L640 지역) | ✓ 이식 | — |
| `wouldIsSneaking` (L1420 public) | `wouldIsSneaking` (필드) | ⚠️ 공식 간소 (wantSprint → isSprinting) | **B-3b** |
| `moveForwardButtonPressed` (지역) | — | ✗ 미이식 | B-3a |
| `moveButtonPressed` (지역) | — | ✗ 미이식 | B-3a |
| `preferSprint` (지역) | — | ✗ 미이식 | B-1c |
| `exhaustionAllowsSprinting` (지역) | — | ✗ 미이식 | B-1c 포함 |
| `isSprintJump` | `isSprintJump` (확인 필요) | ⚠️ 순환 의존 주의 | B-1f 설계 시 |
| `collidedHorizontallyTickCount` | — | ✗ 미이식 (can* 의존) | B-1c 서브 |
| `SmartStatisticsFactory.getTickDistance()` | — | ✗ 미이식 (SmartRender 측) | B-1c 서브 또는 별도 포커스 |

### 6.4 의존 필드 — A-2 수중 3상태 체인 (R-11)

| 원본 | 1.21.1 | 상태 | 수정 원자 |
|---|---|---|---|
| `isShallowDiveOrSwim` (public) | — | ✗ 미이식 | **B-10a** |
| `isJumpingOutOfWater` (public) | — | ✗ 미이식 | **B-10b** |
| `isStillSwimmingJump` (public) | — | ✗ 미이식 | **B-10c** |
| `isLevitating` (public) | — | ✗ 미이식 | **B-10d** |
| `isLiquidClimbing` (로컬 파라미터) | — | ✗ 미이식 | B-7 서브 |
| `isInLiquid()` (메서드) | — | ✗ 미이식 | B-7 서브 |
| `waterMovementTicks` | `waterMovementTicks` | ⚠️ 증분 조건 차이 (dipping 시 리셋 누락) | **B-12** |
| `isFakeShallowWaterSneaking` | `isFakeShallowWaterSneaking` (L215) | ✓ 이식 | — |
| `couldStandUp` (지역) | 1.21.1 Swimmer L163 근사 | 🔄 AABB 근사 (§7 기록) | B-11 (근사 주석 정돈) |
| `wantShallowSwim` (지역) | Swimmer L164 | ✓ 이식 | — |
| `playerSwimWaterBorder` (지역) | `dippingDepth` 로 근사 | 🔄 AABB 근사 | B-9 서브 |

### 6.5 의존 필드 — A-3 등반 체인 (R-12)

| 원본 | 1.21.1 | 상태 | 수정 원자 |
|---|---|---|---|
| `isNeighborClimbing` (L1426 public) | — | ✗ 미이식 | **B-15a** |
| `hasClimbGap` (L1427 public) | — | ✗ 미이식 | **B-15b** |
| `hasClimbCrawlGap` (L1428 public) | `hasClimbCrawlGap` (L153) | ⚠️ 필드만 이식, 갱신 로직 미이식 | B-19 |
| `hasNeighborClimbGap` (L1429) | — | ✗ 미이식 | B-15c |
| `hasNeighborClimbCrawlGap` (L1430) | — | ✗ 미이식 | B-15c |
| `isVineOnlyClimbing` | — | ✗ 미이식 | B-15d |
| `isVineAnyClimbing` | — | ✗ 미이식 | B-15d |
| `isClimbingStill` | — | ✗ 미이식 | B-15e |
| `isClimbHolding` | `isClimbHolding` (L147) | ⚠️ 필드만, 공식 미이식 | **B-16** |
| `wantClimbHolding` (지역) | — | ✗ 미이식 | B-16 |
| `climbIntoCount` | `climbIntoCount` (L156) | ⚠️ 필드만, 카운터 로직 미이식 | B-18 |
| `needClimbCrawling` (지역) | — | ✗ 미이식 | B-18 |
| `canClimbCrawling` (지역) | — | ✗ 미이식 | B-18 |
| `handsEdgeBlock` / `feetEdgeBlock` / edgeMeta | — | ✗ 미이식 | B-15f |
| `actualHandsClimbType` / `actualFeetClimbType` | `actualHandsClimbType` / `actualFeetClimbType` | ⚠️ 필드만, 갱신 미이식 | B-19 |
| `isClimbBackJumping` | `isClimbBackJumping` | ✓ 이식 | — |
| `isWallJumping` | `isWallJumping` | ✓ 이식 | — |
| `isClimbJumping` | `isClimbJumping` | ✓ 이식 | — |
| `isFeetVineClimbing` / `isHandsVineClimbing` | 필드 | ✓ 이식 | — |

### 6.6 의존 필드 — A-4/A-5 체인 (R-13/R-14)

| 원본 | 1.21.1 | 상태 | 수정 원자 |
|---|---|---|---|
| `isAerodynamic` | `isAerodynamic` | ✓ 이식 완료 (R-05/포커스#6 B-5) | — |
| `isStanding` (public L1419) | — | ✗ 미이식 | B-22d/B-30 |
| `isRunning` (**메서드 override L3239-L3242**, 필드 아님) | `ClientState.isRunning(player)` 메서드 (✅ 세션 42) | ✓ 이식 완료 (메서드만) | — |
| `wantCrawl` | `wantCrawl` | ✓ 이식 | — |
| `mustCrawl` | `mustCrawl` | 🔄 AABB 근사 (canStandUp) | B-42 (별도 포커스) |
| `inputContinueCrawl` | 동일 | ✓ 이식 | — |
| `wouldWantCrawl` | 동일 | ✓ 이식 | — |
| `wouldWantClimb` (지역) | — | ✗ 미이식 (B-36 에 필요) | B-36 서브 |
| `wantCrawlNotClimb` | — | ✗ 미이식 | B-31b/B-41 |
| `initializeCrawling` | — | ✗ 미이식 | B-31c |
| `crawlStandUpBottom` (지역) | — | ✗ 미이식 | B-35 포함 |
| `crawlStandUpCeiling` (지역) | — | 🔄 canStandUp 근사 | B-42 |
| `sneakContinueInput` | 동일 | ✓ 이식 | — |
| `isSmall` | `isSmall` (R-04 이식) | ✓ 이식 | — |

### 6.7 Config / Options 필드 매핑

| 원본 | 1.21.1 `SmartMovingConfig` | 상태 |
|---|---|---|
| `Config.enabled` | `cfg.enabled` | ✓ |
| `Config._sneak.value` | `cfg.sneak` | ✓ |
| `Config._crawl.value` | `cfg.crawl` | ✓ |
| `Config._swim.value` | `cfg.swim` | ✓ |
| `Config._dive.value` | `cfg.dive` | ✓ |
| `Config._slide.value` | `cfg.slide` | ✓ |
| `Config._fly.value` | `cfg.fly` | ✓ |
| `Options._sneakToggle.value` | `cfg.sneakToggle` | ✓ |
| `Options._crawlToggle.value` | `cfg.crawlToggle` | ✓ |
| `Config._diveDownOnSneak.value` | `cfg.diveDownOnSneak` | ✓ |
| `Config._swimDownOnSneak.value` | `cfg.swimDownOnSneak` | ✓ |
| `Config._sprintFactor.value` | `cfg.sprintFactor` (확인 필요) | ✓? |
| `Config._diveSpeedFactor.value` | `cfg.diveSpeedFactor` | ✓ |
| `Config._swimSpeedFactor.value` | `cfg.swimSpeedFactor` | ✓ |
| `Config._fallingDistanceMinimum.value` | `cfg.fallingDistanceMinimum` (확인) | ✓? |
| `Config._crawlOverEdge.value` | `cfg.crawlOverEdge` (확인) | ✓? |
| `Config._freeClimbingUpSpeedFactor.value` | `cfg.freeClimbingUpSpeedFactor` (확인) | ✓? |
| `Config._freeClimbingDownSpeedFactor.value` | `cfg.freeClimbingDownSpeedFactor` (확인) | ✓? |
| **`Config._sprintEnableStanding.value`** | — | ✗ **미이식 (B-1a)** |
| `Config._sprintExhaustionStop.value` | — | ✗ 미이식 (B-1c 의존) |
| `Config._sprintExhaustionStart.value` | — | ✗ 미이식 |
| `Config._sprintDuringItemUsage.value` | — | ✗ 미이식 (B-1c 의존) |
| `Options._runOnSprintRelease.value` | — | ✗ 미이식 (B-23 후처리) |
| `Options._walkOnSprintRelease.value` | — | ✗ 미이식 |
| `Config._headFallDamageStartDistance.value` | `cfg.headFallDamageStartDistance` (확인) | ✓? |
| `Config._headFallDamageFactor.value` | `cfg.headFallDamageFactor` (확인) | ✓? |
| `Config._slidingSpeedStopFactor.value` | `cfg.slidingSpeedStopFactor` (확인) | ✓? |
| `Options._flyCloseToGround.value` | — | ✗ 미이식 (B-23 후처리) |
| `Options._diveControlVertical.value` | — | ✗ 미이식 |
| `Config.isCrawlingEnabled()` | `cfg.crawl && cfg.enabled` (inline) | ✓ |
| `Config.isSneakingEnabled()` = `_sneak.value \|\| !enabled` (**OR**) | `cfg.isSneakingEnabled()` (✅ 세션 39) | ✓ 이식 완료 |
| `Config.isSprintingEnabled()` = `_sprint.value && enabled` | `cfg.isSprintingEnabled()` (✅ 세션 39) | ✓ 이식 완료 |
| `Config.isSwimmingEnabled()` = `_swim.value && enabled` | `cfg.isSwimmingEnabled()` (✅ 세션 39) | ✓ 이식 완료 |
| `Config.isDivingEnabled()` = `_dive.value && enabled` | `cfg.isDivingEnabled()` (✅ 세션 39) | ✓ 이식 완료 |
| `Options.isSneakToggleEnabled()` = `_sneakToggle && enabled` | `cfg.isSneakToggleEnabled()` (✅ 세션 45) | ✓ 이식 완료 |
| `Options.isCrawlToggleEnabled()` = `_crawlToggle && enabled` | `cfg.isCrawlToggleEnabled()` (✅ 세션 45) | ✓ 이식 완료 |
| `Config.isLavaLikeWaterEnabled()` | — | ✗ 미이식 (B-7) |
| `Config.isFreeClimbingEnabled()` | `cfg.freeClimb` (확인) | ✓? |
| `Config.isSmartBaseClimb()` / `isSimpleBaseClimb()` / `isStandardBaseClimb()` | — | ✗ 미이식 (B-20) |
| `Config.isSlidingEnabled()` | `cfg.slide && cfg.enabled` | ✓ |
| `Config.isFlyingEnabled()` | `cfg.fly && cfg.enabled` | ✓ |
| `Config.isLevitateSmallEnabled()` | — | ✗ 미이식 |

### 6.8 Button ↔ KeyBinding 매핑 (B-1b — 세션 65 검증)

세션 65 B-1b: 실제 구현 grep 으로 전수 검증 후 상태 갱신.

| 원본 `Button` | 1.21.1 대응 | 상태 |
|---|---|---|
| `grabButton.Pressed` | `SmartMovingKeys.grab.isPressed()` | ✓ |
| `grabButton.StartPressed` | `SmartMovingKeys.grab.wasPressed()` (vanilla KeyBinding 카운터 API) | ⚠️ **다중 호출 주의** — §16 세션 65 기록 (호출 시 카운터 1 감소, 같은 틱 2회째부터 false) |
| `grabButton.StopPressed` | — | ✗ 엣지 검출 미이식 (원본에서 사용 시 별도 필드 필요) |
| `sneakButton.Pressed` | `MinecraftClient.options.sneakKey.isPressed()` 또는 `player.isSneaking()` | ✓ |
| `sneakButton.StartPressed` | `sneakKeyStartPressed` 필드 (L449, prev vs cur 비교) | ✓ |
| `sneakButton.StopPressed` | `sneakKeyStopPressed` 필드 (L451) | ✓ |
| `sprintButton.Pressed` | `MinecraftClient.options.sprintKey.isPressed()` (L872 실제 사용) | ✓ |
| `sprintButton.StartPressed` / `StopPressed` | — | ✗ 엣지 미이식 (원본 사용 지점 확인 필요 — B-48 isGroundSprinting 전환 후처리 범위 가능) |
| `jumpButton.Pressed` | `MinecraftClient.options.jumpKey.isPressed()` | ✓ |
| `jumpButton.StartPressed` | `jumpKeyStartPressed` 필드 (L96, prev vs cur 비교) | ✓ |
| `jumpButton.StopPressed` | `jumpKeyStopPressed` 필드 (L456) | ✓ |
| `moveForwardButton.Pressed` | `player.input.movementForward > 0F` | ✓ |
| `moveBackwardButton.Pressed` | `player.input.movementForward < 0F` | ✓ |
| `moveLeftButton` / `moveRightButton` | `player.input.movementSideways` | ✓ |
| `moveForward/Backward/Left/Right.StartPressed/StopPressed` | vanilla input 은 엣지 비제공 | ⚠️ 원본 이동 엣지 사용 시 prev 필드 비교 필요 (현재 `prevPressRight/Back` 일부 이식 확인) |

**정합성 등급**:
- ✓: 원본 1:1 이식 확정 (구현 grep 로 검증)
- ⚠️: 이식됐으나 원본 시멘틱과 차이 (엣지 감지 정확성 등)
- ✗: 미이식 — 필요 시 별도 원자 신설

### 6.9 메서드/클래스 매핑

| 원본 | 1.21.1 대응 | 상태 |
|---|---|---|
| `getMaxPlayerSolidBetween(y1, y2, d)` | `canStandUp(player)` / AABB helpers | 🔄 근사 (§7) |
| `getMinPlayerSolidBetween(y1, y2, d)` | 동일 근사 | 🔄 |
| `getMaxPlayerLiquidBetween(y1, y2)` | `player.getFluidHeight(WATER)` | 🔄 근사 |
| `getMinPlayerLiquidBetween(y1, y2)` | 동일 근사 | 🔄 |
| `isPlayerInSolidBetween(y1, y2)` | BlockState 스캔 근사 | 🔄 |
| `Orientation.isClimbable(world, i, j, k)` | 1.21.1 ladder/vine BlockState 체크 | ⚠️ 확인 필요 |
| `Orientation.getClimbingOrientations(sp, hasFeet, hasHands)` | 4방향 루프 (대각 누락) | ⚠️ 부분 이식 |
| `Orientation.isTunnelAhead(world, i, j, k)` | Swimmer `isTunnelAhead` 메서드 | ⚠️ 확인 |
| `SmartStatisticsFactory.getInstance(sp).getTickDistance()` | — | ✗ SmartRender 측 미이식 |
| `sp.handleWaterMovement()` | `player.isTouchingWater()` | ✓ 표면 매핑 |
| `sp.handleLavaMovement()` | `player.isInLava()` | ✓ |
| `sp.isCollidedHorizontally` | `player.horizontalCollision` | ✓ |
| `sp.isCollidedVertically` | `player.verticalCollision` | ✓ |
| `sp.isBurning()` | `player.isOnFire()` | ✓ |
| `sp.isUsingItem()` | `player.isUsingItem()` | ✓ |
| `sp.capabilities.isFlying` | `player.getAbilities().flying` | ✓ |
| `sp.setSprinting(bool)` | `player.setSprinting(bool)` | ✓ |
| `toCrawling()` 함수 | — (inline 만) | ✓ 헬퍼 이식 (B-40 세션 44) / ⚠️ 잔여 호출 지점 L2751/L2760/L2767 미흡수 (Extended B-40-post) |
| `resetClimbing()` 메서드 | `Climber.resetClimbing()` | ✓ 이식 완료 (B-14 세션 57) |
| `resetSwimming()` 메서드 | Swimmer `updateSwimState` 물 밖 분기 (부분) | ⚠️ 부분 이식 (Extended B-10-reset-post) |
| `standupIfPossible()` 메서드 | — | ✗ 미이식 (Extended B-N-standup — restoreFromFlying 소비자) |
| `resetState()` | `ClientState.resetState()` | ✓ |
| `handleClimbing()` | `Climber.handleClimbing()` | ⚠️ Free 만 이식, Standard/Simple 미이식 (B-20) |
| `handleCeilingClimbing()` | `Climber.handleCeilingClimbing()` | ✓ 주요 경로 이식 |
| `handleSwimming()` | `Swimmer.handleSwimming()` | ⚠️ 메인 분류 공식 간소 (B-9) |
| `landMotionPost / fromSwimmingOrDiving` | `ClientState.fromSwimmingOrDiving` | ⚠️ 3분기 중 2 이식 (B-39) |
| `tryJump()` | `Jumper.tryJump()` | ✓ 이식 |
| `Config.SlideDown` | — | ✗ 상수 미이식 (B-26) |

### 6.10 이식 규모 총괄

- **✓ 완전 이식**: 13 핵심 상태 필드 중 4개 (isCrawling/contextContinueCrawl 부분 제외) +
  이력 필드 3개 + Config 주요 필드 대부분 + Button/KeyBinding 매핑 대부분
- **⚠️ 부분/오역**: 13 핵심 필드 중 7개 공식에 문제 + 의존 필드 다수
- **✗ 완전 미이식**: 13 핵심 필드 중 2개 (isCrawlClimbing/isClimbCrawling 갱신 로직) +
  의존 필드 30+ + Config 헬퍼 메서드 5개 + 메서드 2개 (resetClimbing/toCrawling) +
  Standard/Simple Base Climb 옵션

**최종 B-N 원자 누적**: 약 50개 (서브원자 포함 70+). **미이식 필드 30+**.
B 단계 Phase 1 (필드 선언 일괄) 부터 실행 권고.

---

## 7. 구조적 차이 / 근사 이식 지점

- `getMaxPlayerSolidBetween` / `getMinPlayerLiquidBetween` 정밀 AABB → `canStandUp` /
  `hasLiquidCeiling` 근사 (이미 적용, 정밀도 손실 있음)
- 반-블록 단위 수직 탐색 → BlockState 단위
- vanilla `isSwimming()` 과 SM `isSwimming_sm` 병존 (접미사 회피) — 혼동 방지용
- **B-5 근사** (세션 63): `SmartMovingSwimmer.handleSwimming` 내
  `isFakeShallowWaterSneaking` 설정 경로 원본 L226-L246 대비 3건 근사:
  (1) `couldStandUp` 수심 측정 — 원본 `minPlayerSwimWaterDepth <= 1.5` (AABB 내 최소 수심
  정밀 스캔) → 1.21.1 `dippingDepth <= 1.5F` (player.getFluidHeight 단일 값)
  (2) `getClimbingOrientations` — 원본 대각 포함 8방향 → 1.21.1 `Direction.Type.HORIZONTAL`
  4방향만
  (3) `swimDown=false` (원본 L244) 미이식 — 1.21.1 swim 수직 속도 로직이 swimDown 비의존이라
  동작상 차이 없음 (B-9 메인 분류 재작성 시 재검토)
- **B-16 근사** (세션 68, 세션 69 갱신): `SmartMovingClientState.tickEssential` 내
  wantClimbHolding/isClimbHolding 3-OR 갱신 공식 (원본 L2721-L2732) 구조 1:1 이식.
  ~~(1) `wantClimb` 2-OR 근사~~ → **세션 69 B-16a/b/c 로 해소 완료** (4-OR 완전 복원).
  (2) `blocked` = `currentScreen != null && !currentScreen.allowUserInput` (원본 L2393).
  1.21.1 `allowUserInput` 필드 제거됨 → `currentScreen != null` 단일 조건 근사. 모든 열린
  screen 을 입력 차단으로 간주 (게임 메뉴 열어도 매달림 유지 동작) — 원본 의도와 근접.
- **B-35 근사** (세션 74): `ClientState.tickEssential` B-17 블록 뒤 + R-09 블록 앞에
  wasCrawling↔isCrawling 전환 후처리 (원본 L2822-L2836) 근사 이식. 분기 A 의
  `crawlStandUpBottom` 정밀 AABB (`getMaxPlayerSolidBetween(minY-1, minY, ...)`) 미이식 →
  `crawlStandUpBottom - minY ≈ 0` 근사 (발 아래 고체 바로 붙어있음 가정). `move(0, dy, 0)`
  이동량 생략 → `heightOffset = 0F` 리셋만. 공중에서 크롤 해제 시 정확도 낮음 (드물긴 함).
  분기 B (진입 엣지) 는 `heightOffset=-1F + move(0,-1D,0)` 1:1 이식.
- **B-26 근사** (세션 75): `ClientState.tickEssential` B-25 IMPL-02 슬라이딩 직접 진입
  블록에 원본 L2555-L2557 부수 동작 이식. `heightOffset = -1F` + `player.move(SELF, new Vec3d(0,
  -1D, 0))` 1:1 이식. **`tryJump(Config.SlideDown, false, wasRunning, null)` 호출 생략** —
  `Jumper.SLIDE_DOWN` 상수 + 전용 속도 공식 (원본 tryJump 내부 SlideDown 분기) 미이식.
  `isFromRunning` 파라미터 영향 생략. 효과: 슬라이딩 진입 시 SlideDown 전용 하강 점프
  추진 모션 누락 — 주로 시각/이펙트 영향, 핵심 상태 플래그에는 영향 없음.
- **B-20 근사** (세션 76): `SmartMovingClimber.handleClimbing` Standard Base Climb 분기
  (원본 L820-L823) 의 `isOnLadderOrVine && isCollidedHorizontally` 조건 판정 생략 — Standard
  분기 진입 자체를 ladder/vine 접촉으로 간주. 실제로는 vanilla ladder 물리가 진입을 조건부로
  처리하므로 실용 등가. `setOnlyShouldClimbSpeed` → `setShouldClimbSpeed` 교체로 isClimbing
  잉여 설정 해소 (포커스 #2 상태 플래그 정확성 개선). Simple/Smart Base Climb (L825-L894)
  전체 미이식은 별도 포커스 후보.
- **B-36 근사** (세션 78): `ClientState.tickEssential` B-35 뒤 + R-09 앞에 grab.StartPressed
  수영/크롤 3분기 (원본 L2839-L2862) 이식. 분기 (a) 의 `getMaxPlayerSolidBetween(minY, maxY, 0)
  - minY` 이동량 — AABB 정밀 스캔 미이식 → `0` 근사 (발 아래 고체 가정) 로 `player.move`
  호출 생략, `heightOffset = 0F` 리셋만 수행. 분기 (b)/(c) 는 1:1. B-10a isShallowDiveOrSwim
  공식 미이식으로 분기 (a) 자체도 항상 false — 근사 영향 제한.
- **B-39 근사** (세션 80): `ClientState.fromSwimmingOrDiving` 에 원본 L1392-L1403 3분기
  구조 복원. `else` 분기 진입은 되나 본문은 no-op (주석만). `crawlStandUpBottom` AABB
  정밀 스캔 미이식 → `≈ minY` 근사 → `minY + 0.5D` 초과 조건 항상 false → isSlow 크롤
  전환 미발동. `move(0, crawlStandUpBottom - minY, 0)` 이동량도 0 근사 생략. 현재 동작
  유지 + 구조 복원으로 focus_14 (AABB 정밀) 완료 시 자동 활성 경로 확보.
- **B-18 근사** (세션 81): `ClientState.tickEssential` B-17 뒤 + B-35 앞에 isClimbCrawling
  공식 이식. 진입 엣지의 `isCollidedHorizontally` 복원 생략 — 1.21.1 `player.horizontalCollision`
  필드 setter 미제공 (mixin 필요). 해제 엣지 본문 (mustCrawl/sneak 상황별 crawl 전환 +
  resetHeightOffset) 은 리서치 요약만 → `climbIntoCount = 0` 리셋만 이식, 나머지는 TODO
  주석 + 서브 원자 B-18b 로 분해 대기 (Agent WebFetch 필요).
- **B-19a2a3 근사 2건** (세션 101): `Orientation` half-solid 판정 (`isLowerHalfFrontFullEmpty`,
  `isUpperHalfFrontAnySolid`, `isUpperHalfFrontFullSolid`) 이식 시 mod 호환 분기 생략:
  (1) `isLowerHalfFrontFullEmpty` — RedPower wire + BetterThanWolves anchor + ASRope +
  LadderKit 4 mod 분기 생략. vanilla ladder 는 `isFullEmpty` 에서 non-empty 처리로 LadderKit
  분기 없어도 기본 동작 보존.
  (2) `isUpperHalfFrontFullSolid` — ASGrapplingHook + Carpenters `_blockCarpentersLadder`
  분기 생략. vanilla 제외 처리 (AbstractSignBlock / WallSignBlock / PressurePlateBlock /
  TrapdoorBlock / FenceGateBlock.OPEN) 는 1:1 이식.
- **B-19a1c4 근사 3건** (세션 98): `Orientation` accessibility 최종 서브 (`isFullAccessible`,
  `isFullExtentAccessible`, `isJustLowerHalfExtentAccessible`, `isUpperHalfFrontEmpty`,
  `getWallBlockId`) 이식 시 mod 호환 분기 생략:
  (1) `isFullExtentAccessible` RedPower wire 분기 (원본 L2490-L2510) 전체 생략 — RedPower
  mod 1.21.1 미이식. `isFullAccessible` 그대로 반환 (추가 AND 체크 없음).
  (2) `isUpperHalfFrontEmpty` RedPower 분기 (원본 L2559-L2567) 생략.
  (3) `isUpperHalfFrontEmpty` LadderKit 분기 (원본 L2581-L2583) 생략 — `isBlockIdOfType(block,
  _ladderKitLadderTypes)` + `rotate(180).hasLadderOrientation` 이 true 면 empty=false 복귀.
  LadderKit mod 1.21.1 미이식 → vanilla ladder 는 `isLadderOrVine` 에 포함되어 `isFullEmpty`
  에서 이미 non-empty 처리됨 (기본 동작 보존).
  `isFullAccessible` / `isJustLowerHalfExtentAccessible` 은 근사 없이 1:1. **B-19a1c
  (accessibility 전체) 완료**.
- **B-19a1c3c 근사 2건** (세션 97): `Orientation.isRemoteAccessible` 이식 시 mod 호환 분기
  생략:
  (1) RedPower wire 분기 (원본 L2404-L2422) 전체 생략 — `isRedPowerWire` / `getRpCoverSides`
  / `isRedPowerWireAnyFront` / `isRedPowerWireAnyBack` 체인. RedPower mod 1.21.1 미이식.
  B-19a1c2 와 같은 패턴.
  (2) ASRope 분기 (원본 L2467-L2471) 생략 — `isASRope` + `rotate(180).isASGrapplingHookFront`
  체크. ASRope/ASGrapplingHook mod 1.21.1 미이식. B-19a1b `isRope`/`isOnWallRope` false
  근사와 연동. `headedToFrontWall` / `headedToRemoteFlatWall` 은 근사 없이 1:1 이식 —
  `getWallFlag` 의 BlockState property 근사 (B-19a1c3b 연동) 에만 영향.
- **B-19a1c3b 근사 1건 + mod 분기 생략 2건** (세션 96): `Orientation.getWallFlag` 이식 시
  1.21.1 BlockState property 기반 근사 + mod 호환 분기 생략:
  (1) **Pane/Fence/Wall 연결 판정** — 원본 `BlockPane.canPaneConnectToBlock(neighbor)` /
  `BlockFence.canConnectFenceTo(world, x, y, z)` / `BlockWall.canConnectWallTo(world, x, y, z)`
  동적 호출 → 1.21.1 `ConnectingBlock.NORTH/SOUTH/EAST/WEST` BooleanProperty +
  `WallBlock.NORTH_SHAPE` 등 `EnumProperty<WallShape>` (`!= NONE` 체크) 조회. BlockState 가
  이미 연결 결과를 property 로 저장 — 대부분 동치. neighbor 변경 직후 같은 tick 안에서는
  약간의 캐시 지연 가능성.
  (2) **BetterMisc reflection 분기 생략** — 원본 `_canConnectFenceTo` reflection 호출 (mod
  fence 지원). BetterMisc mod 1.21.1 미이식 → false.
  (3) **Carpenters 분기 생략** — 원본 `getCarpentersBlockData(i, j_offset, k)` switch 분기.
  Carpenters mod 1.21.1 미이식 → false.
  각 분기는 `getWallFlag` 내 주석 명시. `isFenceGateFront` 는 근사 없이 1:1 이식
  (FenceGateBlock.FACING Direction 매핑).
- **B-19a1c2 근사 3건** (세션 94): `Orientation.isBaseAccessible` 이식 시 mod 호환 분기
  생략:
  (1) RedPower wire 분기 (원본 L2352-L2369) — `isRedPowerWire` / `getRpCoverSides` /
  `isRedPowerWireBottom` / `isRedPowerWireTop` 체인 전체 생략. RedPower mod 1.21.1 미이식.
  (2) ASRope 분기 (원본 L2385-L2389) — `isASRope` + `hasASGrapplingHook`/`hasRopesPlus`
  모드 체크 생략. B-19a1b 의 `isRope`/`isOnWallRope` false 근사와 연동.
  (3) Carpenters 분기 (원본 L2394-L2396) — `_blockCarpentersLadder` + `isExternalBlockType`
  체크 생략. Carpenters mod 1.21.1 미이식.
  각 근사는 해당 `if` 블록 위치에 "근사 이식 — 원본과 차이: X" 주석. vanilla 기반
  접근성 판정 (isEmpty + isFullEmpty + trapdoor + wallBlock + door) 은 모두 1:1.
- **B-19a1c1 근사 4건** (세션 93): `Orientation` 기본 블록 식별 + stair/slab/fence/wall/door
  헬퍼 이식 시 mod 호환성 관련 원본 분기 생략:
  (1) `isStairCompact` — 원본 `_knownCompactStairBlocks` (mod 추가 stair 리스트) 체크 생략.
  vanilla `StairsBlock` 만 감지.
  (2) `isHalfBlock` — 원본 `_knownHalfBlocks` (mod slab 리스트) 체크 생략. vanilla
  `SlabBlock` 만. `!isOpaqueCube()` 는 `SlabType != DOUBLE` 로 1:1 매핑.
  (3) `isBottomHalfBlock` — BetterThanWolves anchor 블록 (metadata==1 예외) 생략. vanilla
  `BedBlock` 예외는 1:1 이식.
  (4) `isWallBlock` — `_knownThinWallBlocks` (mod pane) + Carpenters `_blockCarpentersLadder`
  생략. vanilla `PaneBlock` + `FenceBlock` + `WallBlock` + closed `FenceGateBlock` 만.
  각 근사는 해당 함수 JavaDoc 에 "근사 이식 — 원본과 차이: X" 주석. 1.21.1 vanilla 블록
  식별은 모두 정확 — mod 추가 블록이 있는 환경에서만 영향.
- **B-19a1b 근사 2건** (세션 92): `Orientation` front/back/rope/trapdoor 헬퍼 이식 시
  모드 호환성 관련 원본 분기 생략:
  (1) `hasLadderOrientation` / `getKnownLadderOrientation` — 원본 `_ladderKitLadderTypes`
  (LadderKit 모드) + `carpentersBlockData` (Carpenters 모드) 분기 생략. vanilla ladder
  `LadderBlock.FACING` Direction 직접 비교만 이식. metadata & 0x7 매핑 (5→NZ/4→PZ/2→ZP/3→ZN)
  은 Direction.EAST/WEST/NORTH/SOUTH 와 1:1 대응 — vanilla 측 로직 손실 없음.
  (2) `isRope(j_offset)` / `isOnWallRope(j_offset)` — BetterThanWolves `fcRopeBlock` /
  RopesPlus `blockRopeCentral` / ASRope `blockRope` 모드 블록 체크. 해당 모드 1.21.1 에
  미이식 → **전체 false 반환**. `isLadderSubstitute` (B-19a2 이식 예정) 에서 rope 경로가
  모두 사라져 gap 판정 결과 영향 있음. vanilla ladder/vine 기반 기능은 정상 동작.
- **B-19a1a 근사 4건** (세션 91): `choco.ratel.smartmoving.climbing.Orientation` 에 기본
  블록 식별 + Material 헬퍼 이식 시 1.21.1 API 제약으로 불가피한 근사 등록:
  (1) `isLadderOrVine(state)` — 원본 `isBlockIdOfType(block, _ladderKitLadderTypes)` 추가
  체크 생략. LadderKit 모드 1.21.1 에 미이식 → 순수 vanilla ladder/vine 만 감지.
  (2) `isClimbable(world, i, j, k)` — 원본 Forge `Block.isLadder(world, x, y, z, player)`
  훅 → 1.21.1 `BlockTags.CLIMBABLE` 태그 기반. vanilla 기본 태그에 ladder/vine 포함 +
  모드가 추가한 climbable 블록 감지 — 원본 의도와 거의 등가.
  (3) `isSolid(state, world, pos)` — 원본 `material.isSolid() && material.blocksMovement()`
  → 1.21.1 `state.isSolidBlock(world, pos)`. Material API 1.19+ 에서 완전 제거 →
  isSolidBlock 이 light propagation + collision 통합 기준으로 AND 조건에 근접.
  (4) `isFullEmpty(state, world, pos)` — 원본 `hasASGrapplingHook`/`hasRopesPlus` 모드
  호환 체크 생략 (해당 모드 1.21.1 에 없음). vanilla sign/wall_sign/pressure_plate 예외
  처리는 `AbstractSignBlock`/`WallSignBlock`/`PressurePlateBlock` 로 1:1 이식.
  B-19a1b/c 진행 시 이 근사들이 연쇄 영향 → B-19a4 `seekClimbGap` 결과 정확도에 반영.
- **B-1c 근사** (세션 51, 세션 88 등록): `SmartMovingClientState.tickEssential` B-1c3
  `isClimbSprintSpeed` 판정 — 원본은 `SmartStatisticsFactory.getInstance(sp).getTickDistance()`
  (SmartRender 측 tick 이동 거리 통계) 기반. 1.21.1 SmartStatisticsFactory 전체 미이식 →
  `true` 근사 (모든 등반 속도 허용). 주석으로 명시됨 (ClientState 내부). **→ Extended Phase 9
  (SmartStatistics 이식) 에서 해소 대기**. B-1d/B-1f 의 `isClimbSprinting` 의존 필드 계산에
  영향 — 등반 스프린트가 사실상 항상 가능한 상태 (엄격 이식은 통계 수집 인프라 필요).

---

## 8. 현재 구현 스냅샷

재현 케이스 확보 후 문제된 상태별로 관련 코드 블록 임베드.

---

## 9. 예상 수정 diff

재현 케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

> **방식 전환 (세션 29)**: 재현 케이스 "진입 전 필수" 제약 완화. 포커스 #5/#6 처럼
> **원본 grep + 1:1 코드 감사** 기반으로 진행. 재현 케이스는 있으면 우선순위 판정용 보조,
> 없어도 감사 진행 가능. 원본 `SmartMovingSelf.java` 에서 각 상태 필드가 갱신되는 전수
> 위치를 Agent WebFetch 로 덤프 → 1.21.1 `grep` 으로 갱신 위치 전수 확인 → 불일치 매핑.

### A. 상태 필드별 원본 감사 (Agent WebFetch + grep 기반)

#### A-0 감사 계획 (세션 29)

**13개 상태 필드 의존 그래프**:

```
레벨 0 (vanilla 입력):
  onGround / sprint attribute / sneak key / jump key
레벨 1 (SM 직접 파생):
  isSlow    ← sneakKey 엣지 + 토글(R-09) + wouldWantSneak
               (flying/sliding/headJumping/dive/swim/crawl 상호 의존)
  isFast    ← grab.isPressed() && sprint
레벨 2 (수중/등반/전환 1차 상태):
  isSwimming_sm / isDiving / isDipping   ← Swimmer.updateSwimState 의 offset 3갈래
  isClimbing / isCeilingClimbing          ← Climber feet/hands 판정
  isHeadJumping                            ← grab + sprint + jump 조합 (Jumper)
  isSliding                                ← sneak + sprint + onGround (Slider)
레벨 3 (복합/이력 파생):
  isCrawling              ← wantCrawl + mustCrawl + canCrawl + inputContinueCrawl + contextContinueCrawl
  isCrawlClimbing         ← isCrawling + Climber 관련
  isClimbCrawling         ← (파생 조합)
  contextContinueCrawl    ← isCrawling 토글 + fromSwimmingOrDiving
  wasCrawling_st / wasSneaking / wasClimbCrawling ← 이전 틱 스냅샷
```

**감사 그룹 분해** (우선순위 + 공통성 기준):

- [x] A-0. 감사 계획 수립 (이 블록)
- [x] A-1. **`isSlow` / `isFast`** 원본 덤프 완료 — 원본 `SmartMovingSelf.java` 전수 감사
      (Agent WebFetch). **불일치 3건 확정** (§16 세션 29 기록):
      (1) **`isFast` 완전 오역**: 원본 L2688-L2695 6갈래 OR (`isGroundSprinting \|\|
          isClimbSprinting \|\| isSwimSprinting \|\| isDiveSprinting \|\| isCeilingSprinting \|\|
          isFlyingSprinting \|\| isClimbSprinting(중복)`) vs 1.21.1 L653 `grab.isPressed()
          && player.isSprinting()` 단순.
      (2) **`isSlow` 부분 오역**: 원본 L2718 `isSlow = wantSneak && wouldIsSneaking` where
          `wantSneak = Config.isSneakingEnabled() && wouldWantSneak` vs 1.21.1 L651
          `isSlow = sneakContinueInput && wouldIsSneaking` — `Config.isSneakingEnabled()`
          체크 누락, `sneakContinueInput` 중복 사용.
      (3) **`wouldIsSneaking` 부분 오역**: 원본 L2712 `wouldWantSneak && !wantSprint &&
          !isClimbing` vs 1.21.1 L650 `wouldWantSneak && !player.isSprinting() && !isClimbing`
          — `wantSprint` (SM 복합) 을 vanilla `isSprinting()` 로 대체. `wantSprint` 은 6조건
          OR (sprintButton + 수영/비행/등반/크롤 컨텍스트 + disabled 체크) 필수.
- [x] A-2. ✅ **세션 32 완료** — 수중 3상태 `isSwimming_sm`/`isDiving`/`isDipping` 전수
      감사. `SmartMovingSelf.md` R-11 섹션 (L2508 이후 R-10 뒤) 덤프 완료. **불일치 11건
      확정** (§16 세션 32): 진입 조건 간소화 / Config 게이트 누락 / isClimbCrawling 조건
      누락 / 메인 분류 3-갈래 완전 대체 / isShallowDiveOrSwim·isJumpingOutOfWater·
      isStillSwimmingJump·isLevitating 필드 미이식 / 얕은 물 특수 분기 미이식 /
      waterMovementTicks 증분 조건 차이 / 크롤↔수영 전환 isSliding 누락. B-6~B-13 원자 추가.
- [x] A-3. ✅ **세션 33 완료** — 등반 4상태 `isClimbing`/`isCeilingClimbing`/
      `isCrawlClimbing`/`isClimbCrawling` 전수 감사. `SmartMovingSelf.md` R-12 섹션 덤프
      완료. **불일치 14건 확정** (§16 세션 33): resetClimbing() 메서드 미이식 /
      isCrawlClimbing+isClimbCrawling 갱신 로직 완전 미이식 / isClimbHolding+wantClimbHolding
      계산 미이식 / 의존 필드 9건 미이식 (isNeighborClimbing / hasClimbGap /
      isVineOnlyClimbing / isVineAnyClimbing / isClimbingStill / hasNeighborClimbGap /
      hasNeighborClimbCrawlGap / handsEdgeBlock / feetEdgeBlock) / Standard·Simple Base
      Climb 미이식 / isCeilingClimbing 해제 엣지 미이식. B-14~B-21 원자 추가.
- [x] A-4. ✅ **세션 34 완료** — 전환 쌍 `isHeadJumping`/`isSliding` 전수 감사.
      `SmartMovingSelf.md` R-13 섹션 덤프 완료. **불일치 13건 확정** (§16 세션 34):
      isHeadJumping 매 틱 재평가 5-AND 공식 미이식 / 해제 엣지 후처리 (handleCrash +
      restoreFromFlying) 미이식 / 직접 진입 6-AND 조건 간소화 / 부수 동작 3건
      (setHeightOffset + move + tryJump SlideDown) 미이식 / fallDistance>fallingDistance
      분기 미이식 / handleClimbing 진입 isSliding=false 미이식 / toSlidingOrCrawling
      조건 완전 대체 / 미이식 필드 4건 (wasHeadJumping / wasRunning / isRunning /
      isStanding). **isAerodynamic 은 R-05/포커스#6 B-5 이식 완료 확인**. B-22~B-30
      원자 추가.
- [x] A-5. ✅ **세션 35 완료** — `isCrawling`+`contextContinueCrawl` 전수 감사.
      `SmartMovingSelf.md` R-14 섹션 덤프 완료. **불일치 15건 확정** (§16 세션 35):
      메인 공식 구조 차이 (매 틱 vs 이원화) / canCrawl 9-AND 근사 (원본 5-AND) /
      wasCrawling 필드 누락 / capabilities.flying 해제 점프 미이식 / 전환 후처리
      heightOffset+move 미이식 / grab.StartPressed 수영/크롤 3분기 미이식 / handleClimbing
      wall 오르기 진입 미이식 / handleCeilingClimbing 해제 미이식 / landMotionPost 3분기 중
      1개 누락 / toCrawling() 헬퍼 부분 이식 / wantCrawlNotClimb 미이식 / initializeCrawling
      미이식 / mustCrawl AABB 근사. B-31~B-42 원자 추가.
- [x] A-6. ✅ **세션 36 완료** — 이력 3개 `wasCrawling_st`/`wasSneaking`/`wasClimbCrawling`
      전수 감사. **3개 필드 자체는 이식 완료** (ClientState L241/L244/L247 필드 + L560-L562
      저장 + L905-L907 리셋). R-09 블록 전체 세부 조건 1:1 대조 완료 — 거의 1:1 이식됨.
      **불일치 4건 확정** (§16 세션 36):
      (1) R-09 블록 종료부 `wasRunning=isRunning` / `wasLevitating=isLevitating` 저장 누락
          (A-4 B-22b / A-2 B-10d 의존)
      (2) 3개 이력 필드 저장 시점이 공식 직전 아닌 tickEssential 초반 일괄 저장 (결과적
          동치이나 B-2/B-18/B-33 수정 시 공식 직전으로 이동 필요)
      (3) R-09 L788 간소 매핑 — 기존 B-4 범위
      B-43 (R-09 종료부) + B-44a~c (저장 시점 정밀 조정) 원자 추가. A-6 대부분 기존 B-N
      에 흡수.
- [x] A-7. ✅ **세션 37 완료** — 13 상태 필드 + 의존 필드 + Config + Button +
      메서드/클래스 매핑 테이블 §6 통합 완성. §6 에 10 서브섹션 (6.1 13 핵심 상태 /
      6.2 이력 / 6.3 A-1 체인 / 6.4 A-2 체인 / 6.5 A-3 체인 / 6.6 A-4/A-5 체인 /
      6.7 Config / 6.8 Button↔KeyBinding / 6.9 메서드 / 6.10 이식 규모 총괄). **A 단계
      전체 완료** — B 단계 Phase 1 진입 대기.

**각 A-N 그룹은 Agent WebFetch 로 원본 갱신 위치 전수 덤프 + 1.21.1 grep + 매핑**.
불일치 발견 시 해당 그룹에서 B-N 원자 작업 추가하여 수정. 포커스 #6 A→B-N 확장 패턴과 동일.

**세션 규모 예상**: 각 A-N 그룹이 Agent 1회 + grep 여러 번 + 매핑 — 적정 세션당 1-2 그룹.
전체 A 단계 완료까지 3-4 세션. B 단계 (수정) 는 매핑 결과에 따라 변동.

### B. 각 필드별 불일치 수정 (A-N 매핑 결과 기반)

> **원칙 (세션 30 재확인)**: 근사/간소/대체 매핑 금지. 원본 코드 100% 이식. 의존 필드도
> 원본 선언·계산을 그대로. 1.21.1 컨벤션(`MinecraftClient` / `KeyBinding` 등) 으로의
> 표면 매핑만 허용, 로직 구조는 원본 1:1. 컨텍스트 압박 시 각 원자를 더 쪼개도 됨.

#### B-0. 원본 리서치 보강 (B-1/B-3 선행 필수)
- [x] B-0. ✅ **세션 31 완료** — `.tmp_research/SmartMovingSelf.java` (원본) 에서 전수 덤프 →
      `SmartMovingSelf.md` R-10 섹션 (L2508+) 임베드. 포함 블록:
      - R-10.1 필드 선언 / R-10.2 `disabled` / R-10.3 wouldWantSneak+wantSneak /
        R-10.4 move*Pressed / R-10.5 wantSprint / R-10.6 isSprintJump+preferSprint /
        R-10.7 isClimbSprintSpeed+can*+6 Sprint 변종 / R-10.8 standing+isFast+전환 후처리 /
        R-10.9 wouldIsSneaking+isSlow / R-10.10 전수 사용처 / R-10.11 이식 매핑 예비안.
      §5.2 에도 인용 링크 + 범위 목록 추가.

#### B-1. `isFast` 공식 6갈래 OR 이식 (원자 6개 분해)
- [x] B-1a. ✅ **세션 40 완료** — `Config._sprintEnableStanding` → `SmartMovingConfig.sprintEnableStanding = false` (원본 `SmartMovingConfig.java` L313 `Unmodified("move.sprint.enable.ground")`).
- [x] B-1b. ✅ **세션 65 완료** — §6.8 Button ↔ KeyBinding 매핑 테이블 전수 검증 +
      상태 갱신. 기존 테이블 (세션 37 A-7 작성) 의 `⚠️`/`✗` 항목을 실제 구현 grep 으로
      검증하여 정합성 등급 재확정:
      (1) `jumpButton.StartPressed` ✗ → ✓ (L96 `jumpKeyStartPressed` 필드 존재 확인)
      (2) `sprintButton.Pressed` ⚠️ → ✓ (L872 `sprintKey.isPressed()` 실제 사용 확인)
      (3) `grabButton.StartPressed` `wasPressed()` 매핑의 **다중 호출 시 카운터 감소** 시멘틱
          차이 발견 → §16 세션 65 기록 + 신규 원자 B-46 후보.
      (4) `sprintButton.StartPressed/StopPressed`, `grabButton.StopPressed` 미이식 기록 (B-48
          범위 또는 별도 원자).
      정합성 등급 표기 (✓/⚠️/✗) 범례 추가. 원본 이동 엣지 (`move*Button.StartPressed` 등)
      는 vanilla input 엣지 비제공 — prev 필드 비교 필요.
- [x] B-1c. ✅ **세션 50-51 완료** — Config 7필드/3헬퍼 + ClientState 필드 2 +
      공식 이식. 세션 50: B-1c1 (Config 피로/스프린트). 세션 51: B-1c2/3
      (collidedHorizontallyTickCount 필드+갱신, preferSprint, isClimbSprintSpeed 근사 `true`,
      can* 4). SmartStatisticsFactory.getTickDistance() 1.21.1 미이식 → `true` 근사 (주석 명시).
- [x] B-1d. ✅ **세션 51 완료** — 6 Sprint 변종 이식. `isGroundSprinting` public 필드 +
      5 지역 변수 (`isSwim/Dive/Ceiling/Flying/ClimbSprinting`). 원본 L2678-L2684 1:1.
      isLevitating 필드 참조는 B-10d 후 자동 활성.
- [x] B-1e. ✅ **세션 51 완료** — `_standing17 = onGround && !isSliding && !isCrawling` 지역
      변수 이식 (원본 L2686).
- [x] B-1f. ✅ **세션 51 완료** — `isFast` 공식 **6갈래 OR 완전 복원** (원본 L2688-L2695).
      `isClimbSprinting` 중복 (L2695) 1:1 보존. 기존 `grab && isSprinting()` 제거.
      **A-1 불일치 #1 해소**.

#### B-2. `isSlow` 공식 정정 (단일 원자)
- [x] B-2. ✅ **세션 43 완료** — `SmartMovingClientState.tickEssential` 정정:
      - `wantSneak = cfg0.isSneakingEnabled() && wouldWantSneak` 신설 (원본 L2588-L2590)
      - `isSlow = wantSneak && wouldIsSneaking` (원본 L2718, 기존 sneakContinueInput 중복 제거)
      - 주석 원본 라인 L2576-L2590/L2711-L2719 1:1 매핑 기록
      - ※ wouldIsSneaking 는 여전히 `!player.isSprinting()` — B-3b 에서 `!wantSprint` 로 정정 예정

#### B-3. `wantSprint` 신설 + `wouldIsSneaking` 정정 (원자 2개 분해)
- [x] B-3a. ✅ **세션 49 완료** — `wantSprint` public 필드 신설 + 6조건 OR 공식 이식
      (원본 L2595-L2615). 지역 변수 `disabled` (원본 L2375) 는 `!cfg.enabled ||
      hasVehicle() || isSleeping() || getSleepTimer()>0` (startSleeping 근사).
      sprint/jump 키는 `MinecraftClient.options.sprintKey/jumpKey` 표면 매핑, move 입력은
      `player.input.movementForward/Sideways`. 6-AND 공식:
      `isSprintingEnabled && !isSliding && sprintPressed && (지면전진 || 등반 || 수영+입력
      || 잠수+입력+점프 || 비행+입력+점프/스니크) && !disabled`.
- [x] B-3b. ✅ **세션 49 완료** — `wouldIsSneaking = wouldWantSneak && !wantSprint &&
      !isClimbing` (원본 L2712). 기존 `!player.isSprinting()` (vanilla 단순) → `!wantSprint`
      (SM 복합) 으로 정정. A-1 불일치 3번 해소.

#### B-4. R-09 토글 블록 `wantSneak_/wantSprint_` 간소 매핑 제거 (§16 세션 30 신규)
- [x] B-4. ✅ **세션 52 완료** — R-09 블록 L1170-L1173 지역 변수 `wantSneak_/wantSprint_`
      제거, B-2 `wantSneak` / B-3a `wantSprint` 필드 사용 (원본 L2990 1:1 복원).
      "간소 매핑" 주석 제거. wantSneak 는 else 블록 직계 지역 변수 (L835) 로 R-09
      동일 스코프 내 접근 가능.

#### B-5. `SmartMovingSwimmer.java` L159 "1.21.1 간소화" 원본 대조 (§16 세션 30 신규)
- [x] B-5. ✅ **세션 63 완료** — 원본 L226-L246 `isFakeShallowWaterSneaking=true` 경로 대조 결과:
      **근사 이식 3건 확정** (1:1 아님, 정정 원자도 별도 추가 않음 — B-9 재작성 범위에 흡수):
      (1) `couldStandUp` 수심 측정 — 원본 AABB 정밀 `minPlayerSwimWaterDepth` → 1.21.1
          `dippingDepth` (fluidHeight 단일 값)
      (2) `getClimbingOrientations` — 원본 대각 8방향 → 1.21.1 Horizontal 4방향
      (3) `swimDown=false` (원본 L244) 미이식 — 1.21.1 swim 수직 로직이 swimDown 비의존
      Swimmer 주석을 "1.21.1 간소화" → "근사 이식 — 원본과 차이 3건" 으로 정리.
      §7 근사 이식 지점에 B-5 근사 3건 등록. B-9 메인 분류 재작성 시 재평가.

#### B-6. Swimmer `updateSwimState` L78 — `isClimbCrawling` 누락 조건 추가 (A-2 발견)
- [x] B-6. ✅ **세션 60 완료** — `SmartMovingSwimmer.updateSwimState` L78 조건
      `sm.isCrawling || sm.isCrawlClimbing` (2-OR) 에 `sm.isClimbCrawling` 추가해
      원본 L301 3-OR `isCrawling || isClimbCrawling || isCrawlClimbing` 1:1 복원.
      isClimbCrawling 공식 이식은 B-18 범위라 현재 항상 false 유지이나 조건은 1:1 정렬.
      주석에 원본 라인 + B-18 의존 명시.

#### B-7. Swimmer `updateSwimState` 진입 조건 복원 (A-2 발견)
- [~] B-7. **→ Extended Phase 5 (B-7a/b/c) 로 이전** (세션 88). 원본 L232 진입 조건
      복원. 세부 분해: B-7a (isLiquidClimbing) / B-7b (lavaLikeWater 헬퍼) / B-7c (진입 조건
      정밀 이식). 상세 [`focus_02_extended.md#phase-5`](focus_02_extended.md) 참조.

#### B-8. `Config.isSwimmingEnabled() / isDivingEnabled()` 게이트 추가 (A-2 발견)
- [x] B-8. ✅ **세션 67 완료** — 원본 L436-L441 Config 게이트 이식.
      `SmartMovingSwimmer.updateSwimState` 말미 (offset 기반 3상태 결정 직후) 에 게이트
      블록 추가:
      * `!cfg.isSwimmingEnabled() → isSwimming_sm = false; isDipping = false`
      * `!cfg.isDivingEnabled()   → isDiving = false`
      Config 비활성화 시 **상태 플래그 자체를 false 로 정화** → 소비처 (#1/#3/#4) 잘못된
      true 참조 방지. 기존 handleSwimming `return false` 는 경로만 차단하고 플래그 정화
      안 했음 (오역). `useStandard` 재판정은 B-9 메인 분류 재작성 범위 — 여기선 플래그
      정화만. 헬퍼 이식은 세션 39 B-2/B-3a/B-8 에서 완료.

#### B-9. 메인 분류 공식 재작성 (A-2 발견 — 가장 큰 수정)
- [~] B-9. **→ Extended Phase 5 (B-9a~g) 로 이전** (세션 88). 원본 L303-L414 3-갈래 메인
      분류 이식. 7 서브 원자 분해 — AABB 정밀 (Phase 6) 의존 다수. 상세
      [`focus_02_extended.md#phase-5`](focus_02_extended.md) 참조.

#### B-10. 미이식 필드 4건 ClientState 이식 + 갱신 로직 (A-2 발견)
- [~] B-10a. **필드만 이식 완료 (세션 38) / 공식 → Extended Phase 4 B-10a-post** — `isShallowDiveOrSwim` 필드 ClientState 추가.
      공식 갱신 (`couldStandUp && (isDiving || isSwimming_sm)` 원본 L507) 은 Extended 이전.
- [~] B-10b. **필드만 이식 완료 (세션 38) / 공식 → Extended Phase 4 B-10b-post** — `isJumpingOutOfWater` 필드 추가. 조건 이식
      (wantJumpOutOfWater + waterMovementTicks>10 원본 L486-L487) 은 B-12 수정 시.
- [~] B-10c. **필드만 이식 완료 (세션 38) / 공식 → Extended Phase 4 B-10c-post** — `isStillSwimmingJump` 필드 추가. false 리셋
      (useStandard 경로 원본 L550) 은 B-9 수정 시.
- [x] B-10d. ✅ **세션 71 완료** — 원본 L474 + L505 공식 이식. Swimmer.updateSwimState Config
      게이트 뒤에 배치:
      `diveUp = player.input.jumping; diveDown = player.isSneaking() && cfg.diveDownOnSneak;`
      `isLevitating = isDiving && !diveUp && !diveDown && movementSideways==0F && movementForward==0F;`
      원본 `diveUp = isp.getIsJumpingField()` (L468) / `diveDown = sneak && Config._diveDownOnSneak`
      (L469) 지역 변수 이식. ClientState `sendStatePacket` L1775 `isLevitating = false` 강제
      리셋 제거 — updateSwimState 갱신값 유지. 필드 선언 주석도 "수중 정적 자세" 의미 명시.
      B-43 세션 56 `wasLevitating = isLevitating` 저장이 이제 실제 값 참조.

#### B-11. 얕은 물 특수 분기 이식 (A-2 발견)
- [~] B-11. **→ Extended Phase 5 로 이전** (세션 88). 원본 L513-L536 얕은 물 특수 분기 이식.
      AABB 정밀 (Phase 6) 의존. 상세 [`focus_02_extended.md#phase-5`](focus_02_extended.md)
      참조.

#### B-12. `waterMovementTicks` 증분 조건 정정 (A-2 발견)
- [x] B-12. ✅ **세션 64 완료** — 원본 L481-L484 1:1 정정. `SmartMovingSwimmer.updateSwimState`
      2곳 수정:
      (1) L80-L86 크롤→isDipping 강제 분기: `sm.waterMovementTicks++` → `sm.waterMovementTicks = 0`
          (dipping 강제 경로는 swimming/diving 아님)
      (2) L88-L99 수심 분기 말미: 무조건 `sm.waterMovementTicks++` →
          `if (sm.isSwimming_sm || sm.isDiving) ticks++; else ticks = 0;`
      B-10b `isJumpingOutOfWater` 공식 (원본 L486-L487 `wantJumpOutOfWater +
      ticks>10 || onGround`) 이식은 별도 원자 유지 — B-12 는 ticks 자체 정정만.

#### B-13. 크롤↔수영 전환 조건 `isSliding` 추가 (A-2 발견)
- [x] B-13. ✅ **세션 61 완료** — `SmartMovingSwimmer.handleSwimming` L126 조건
      `wasCrawling && sm.dippingDepth >= 0F` → `(wasCrawling || sm.isSliding) &&
      sm.dippingDepth >= 0F` 로 확장. 원본 L2434 (= R-06 L418) `(isCrawling || isSliding) &&
      playerCrawlWaterBorder < SwimCrawlWaterMaxBorder` 1:1 복원. 원본 isCrawling 은
      L2415 분기 뒤라 1.21.1 진입 시 스냅샷 `wasCrawling` 과 의미적 등가. isSliding 은
      L121 에서 수정 안 되므로 원본과 동일 시점. L121 `isCrawling && dippingDepth >
      SWIM_CRAWL_TOP` 조건은 원본 L2415 와 일치 (isSliding 불필요) — 수정 없음.

#### B-14. `resetClimbing()` 메서드 신설 + handleClimbing 진입 시 호출 (A-3 발견)
- [x] B-14. ✅ **세션 57 완료** — 원본 L1474-L1486 `resetClimbing()` 이식.
      **ClientState**: `resetClimbing()` public 메서드 신설 — 10 필드 리셋
      (`isClimbing` / `isHandsVineClimbing` / `isFeetVineClimbing` / `isVineOnlyClimbing` /
      `isVineAnyClimbing` / `isClimbingStill` / `isNeighborClimbing` /
      `actualHandsClimbType = HandsClimbing.NO_GRAB` / `actualFeetClimbType = FeetClimbing.NO_STEP` /
      `isCeilingClimbing`). **Climber.handleClimbing**: L238 진입부
      (SmartMovingConfig cfg = ... 직후, exhaustion 체크 앞) 에 `sm.resetClimbing()` 호출 추가.
      원본 L816 첫 문장 위치 1:1. `HandsClimbing`/`FeetClimbing` import 2건 추가.
      **B-21 (isCeilingClimbing 해제 엣지) 자동 해소.** B-28 은 원본 L985 조건부
      (wantClimbUp + handsClimbing.IsRelevant) 라 별도 원자 유지 (B-19 의존).

#### B-15. 미이식 등반 필드 9건 ClientState 이식 (A-3 발견)
- [x] B-15a. ✅ **세션 40 완료** — `isNeighborClimbing` 필드 추가 + resetState 리셋
- [x] B-15b. ✅ **세션 40 완료** — `hasClimbGap` 필드 추가 + resetState 리셋 — B-18 의존 해소
- [x] B-15c. ✅ **세션 40 완료** — `hasNeighborClimbGap` / `hasNeighborClimbCrawlGap` 필드 추가 + resetState 리셋
- [x] B-15d. ✅ **세션 40 완료** — `isVineOnlyClimbing` / `isVineAnyClimbing` 필드 추가 + resetState 리셋
- [x] B-15e. ✅ **세션 40 완료** — `isClimbingStill` 필드 추가 + resetState 리셋
- [x] B-15f. ✅ **세션 40 완료** — `handsEdgeBlock` / `feetEdgeBlock` BlockState 필드 추가 (원본 Block+meta → 1.21.1 BlockState 흡수 — vanilla API 표면 매핑) + resetState null 리셋. 갱신 로직은 B-19 범위.

#### B-16. `wantClimbHolding` / `isClimbHolding` 갱신 블록 이식 (A-3 발견)
- [x] B-16. ✅ **세션 68 완료 (근사 이식)** — 원본 L2721-L2732 3-OR 공식 **구조 1:1 이식**.
      tickEssential isSlow 공식 직후 (원본 L2718 → L2721 순서) 에 블록 추가:
      `wantClimbHolding = (isClimbHolding && sneak) || (isClimbing && blocked) ||
      (wantClimb && !swim && !dive && !crawl && (sneak || crawlToggled));
       isClimbHolding = wantClimbHolding && isClimbing;`
      **의존 필드 2건 근사** (§7 B-16 근사 등록):
      (a) `wantClimb` — 원본 `Config.isFreeClimbingEnabled() && wouldWantClimb`. 4-OR 중
          `isFacedToLadder`/`isFacedToSolidVine` + `freeClimbAutoLadder/Vine` Config 미이식
          → 2-OR 근사 (`grab || isClimbHolding+sneak`).
      (b) `blocked` — 원본 `currentScreen!=null && !currentScreen.allowUserInput`. 1.21.1
          `allowUserInput` 제거됨 → `currentScreen != null` 단일 조건 근사.
      Agent WebFetch (세션 68) 로 `wantClimb`/`blocked`/`wouldWantClimb` 원본 정의 확보.
      **B-16 완전 이식은 서브 원자 B-16a/b/c 로 분해** (아래).

#### B-16a. `isFacedToLadder` / `isFacedToSolidVine` 헬퍼 이식 (B-16 근사 해소 서브)
- [x] B-16a. ✅ **세션 69 완료** — 원본 SmartMovingBase L184-L192 이식. SmartMovingClimber
      에 public static 메서드 2개 추가. 원본 `getOnLadder(1, true, isSmall) > 0` /
      `getOnVine(1, true, isSmall) > 0` 는 `getOnLadderOrVine` 의 onlyLadder/onlyVine 필터
      파라미터를 받으나 1.21.1 `getOnLadderOrVine` 에는 이 필터 없음. 근사: 기존
      `out_handsVine[0]`/`out_feetVine[0]` 플래그로 vine 여부 구분 — `relevant && !vine` 은
      ladder, `relevant && vine` 은 solidVine. 1.21.1 `getOnLadderOrVine` 내부 L143-L144 에
      이미 "solid 뒤" 체크 있어 solidVine 조건 내장됨.

#### B-16b. `Config.freeClimbAutoLadder` / `freeClimbAutoVine` Config 필드 이식 (B-16 근사 해소 서브)
- [x] B-16b. ✅ **세션 69 완료** — Agent WebFetch 로 원본 기본값 `true` 확인
      (Properties.java L171 `Unmodified → return true`). SmartMovingConfig 에 2개 필드 추가:
      `freeClimbAutoLadder = true` / `freeClimbAutoVine = true`. 헬퍼 메서드 2개 추가:
      `isFreeClimbAutoLadderEnabled()` / `isFreeClimbAutoVineEnabled()` — 원본
      SmartMovingClientConfig L57-L65 `_value && enabled` AND 패턴.

#### B-16c. `wouldWantClimb` 4-OR 완전 이식 (B-16 근사 해소 서브)
- [x] B-16c. ✅ **세션 69 완료** — ClientState B-16 블록의 `wouldWantClimb16` 2-OR → 4-OR
      확장. 원본 L2467-L2477 1:1:
      `(grab || isClimbHolding+sneak ||
        (Config.isFreeClimbAutoLadderEnabled() && isFacedToLadder(isClimbCrawling)) ||
        (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine(isClimbCrawling)))
       && (!isSliding || grab+forward) && !isHeadJumping && !wantCrawlNotClimb && !disabled`.
      B-16 근사 2건 중 (a) `wantClimb` 근사 **해소 완료** — 원본 4-OR 구조 완전 복원.
      (b) `blocked` 근사 (`allowUserInput` 제거) 만 §7 에 잔존. `isClimbCrawling` 현재 미이식
      (B-18 대기) 이라 `false` 전달 — B-18 이식 후 자동 활성.

#### B-17. `isCrawlClimbing` 메인 공식 + 전환 블록 이식 (A-3 발견)
- [x] B-17a. ✅ **세션 47 완료** — `isCrawlClimbing` 메인 5-AND 공식 이식 (원본 L2737).
- [x] B-17b1. ✅ **세션 48 완료** — 원본 L2738-L2754 canStandUp 분기 이식:
      - `_wasCrawlClimbing17` 지역 변수 저장 (공식 직전, 원본 L2736)
      - `isPlayerInSolidBetween(player, y1, y2)` 정밀 헬퍼 신설 (AABB 직접 스캔 — 근사 X)
      - `canStandUp` 판정 시 → wasCrawlClimbing=false, isCrawlClimbing=false,
        `!isClimbCrawling → heightOffset = 0F`
      - `!wasCrawlClimbing → wasCrawling=false, isCrawling=false`
- [x] B-17b2. ✅ **세션 73 완료** — 원본 L2755-L2783 `else if (wasCrawlClimbing)` 복합 전환
      3분기 이식. Agent WebFetch 로 원본 본문 확보 후 ClientState B-17b1 블록 직후에 추가:
      * **분기 1** (`!isClimbing`): `wasCrawling = toCrawling();` 메서드 호출 + 바닥 스냅
        `move(0, -minY + Math.floor(minY), 0)`.
      * **분기 2** (`moveForward <= 0F`): 로컬 `toCrawling` 반영 `wasCrawling=isCrawling=toCrawling`
        + `wantClimbUp=wantClimbDown=false` + `!toCrawling → heightOffset=0F` + 조건부 높이 복귀
        `move(0, -minY+floor(minY)+(toCrawling ? 0F : 1F), 0)`.
      * **분기 3** (`!toCrawling`): `heightOffset=0F` + 위쪽 스냅 `move(0, ceil(minY)-minY, 0)`.
      `player.move(MovementType.SELF, Vec3d)` 로 이동 + collision + boundingBox 갱신. import
      `MovementType`/`Vec3d` 2건 추가. toCrawling() 메서드 (B-40 세션 44) / wantClimbUp/Down
      필드 (B-17b2-pre 세션 72) 의존 모두 충족.
- [x] **B-17b2-pre** ✅ **세션 72 완료** — B-17b2 선행 의존 필드 3개 승격. ClientState:
      `wantClimb` / `wantClimbUp` / `wantClimbDown` public 필드 신설.
      tickEssential B-16 블록 직후에 원본 L2491-L2500 `wantClimbUp/wantClimbDown` 계산 이식.
      B-16c 블록 `wantClimb16` 지역 → `this.wantClimb` 필드 승격. resetState 리셋 3건 추가.
      의존 전수 이식 확인: isVineAnyClimbing (B-15d) / isFacedToSolidVine (B-16a) /
      나머지 vanilla. 연산자 우선순위 (&& > ||) 주의 — 원본 `(wantClimb && forward>0) ||
      (vineClimb + jump + crawl/slide 충돌 조건)` 1:1.

#### B-18. `isClimbCrawling` 메인 공식 + climbIntoCount 카운터 이식 (A-3 발견)
- [x] **B-18-pre** ✅ **세션 81 완료** — wantClimbHolding public 필드 승격. B-16 블록 지역
      `boolean wantClimbHolding` → `this.wantClimbHolding` 필드 할당. resetState 리셋 추가.
- [x] B-18. ✅ **세션 81 완료 (부분, 해제 엣지 본문 제외)** — 원본 L2786-L2820 이식:
      ClientState tickEssential B-17 블록 뒤 + B-35 앞에 배치.
      * `needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding)` 1:1 ✓
      * `canClimbCrawling = wantClimbHolding && wantClimbUp` 1:1 ✓
      * climbIntoCount 카운터: `>1 감소` / `==0 && needClimbCrawling 없음` → `6` 재장전 ✓
      * `isClimbCrawling = canClimbCrawling && ((needClimbCrawling && count==0) || count>1)` ✓
      * 진입 엣지 (원본 L2812-L2817): `heightOffset=-1F + move(0, 0.05, 0)` ✓.
        `isCollidedHorizontally` 복원은 §7 B-18 근사 등록 (1.21.1 player.horizontalCollision
        setter 없음 → mixin 필요 — 별도).
      * 해제 엣지 (원본 L2819-L2820): `climbIntoCount = 0` 만 이식. 나머지 본문 (mustCrawl/
        sneak 상황별 crawl 전환 + resetHeightOffset) 은 리서치 요약만 — Agent WebFetch 후
        별도 서브 원자 **B-18b** (TODO 주석). 의존 전수 충족: hasClimbCrawlGap 필드 (기존) /
        hasClimbGap (B-15b) / isClimbHolding (B-16) / wantClimbHolding (B-18-pre) /
        wantClimbUp (B-17b2-pre) / climbIntoCount 필드.
- [x] **B-18b** ✅ **세션 83 완료 (근사 이식)** — 세션 81 B-18 해제 엣지의 TODO 본문 완전
      이식. Agent WebFetch 로 원본 L2804-L2820 확보:
      ```java
      climbIntoCount = 0;
      if (mustCrawl || sneakButton.Pressed || crawlToggled) {
          double gap = minY - getMaxPlayerSolidBetween(minY-1, minY, 0);
          if (gap >= 0 && gap < 1) { wasCrawling = toCrawling(); move(0, -gap, 0); }
          else resetHeightOffset();
      } else resetHeightOffset();
      ```
      ClientState 이식: `sneakPressedRaw || mustCrawl || crawlToggled` 시 `wasCrawling =
      toCrawling()` (§7 B-18 근사 — `gap ≈ 0` 가정으로 항상 `[0, 1)` 범위 + move 생략).
      그 외 `heightOffset = 0F`. 스코프: B-18 본체가 pre-compute 블록 내 배치 → `mustCrawl/
      sneakPressedRaw/crawlToggled` 모두 접근 가능 (빌드 성공 확인).

#### B-19. `hasClimbCrawlGap` / `hasClimbGap` / `isNeighborClimbing` 갱신 로직 이식 (A-3 발견)
- [~] B-19. **→ Extended Phase 3 (B-19a/b/c/d) 로 이전** (세션 88). 원본 handleClimbing
      Free Climbing 분기 (L896-L1108) 내부 Orientation 판정 → hasClimbCrawlGap /
      hasClimbGap / isNeighborClimbing 계산. **포커스 #2 최우선 도미노 해소** — 세션
      47/48/68/69/72/73/77/81/83 이식한 B-17/B-18/B-16c 공식이 모두 이 원자에 의존.
      상세 [`focus_02_extended.md#phase-3`](focus_02_extended.md) 참조.

#### B-20. Standard / Simple Base Climb 이식 (A-3 발견)
- [x] B-20. ✅ **세션 76 완료 (Standard 만) / Simple·Smart → Extended Phase 8 (B-20b/c)** —
      원본 L820-L823 Standard Base Climb
      `isClimbing 설정 안 함` 원칙 복원. SmartMovingClimber.handleClimbing L305-L313 Standard
      분기에서 `setOnlyShouldClimbSpeed` (L273 `isClimbing=true` 내장) → `setShouldClimbSpeed`
      (isClimbing 안 건드림) 로 교체. 속도 보정 (motionY = 0.2 * combinedFactor) 유지.
      §7 B-20 근사 등록: `isOnLadderOrVine && isCollidedHorizontally` 조건 판정 생략 —
      Standard 분기 진입을 ladder 접촉으로 간주 (vanilla ladder 물리 조건부 처리로 실용 등가).
      **Simple/Smart Base Climb 분기 (L825-L894) 는 미이식 유지** — 별도 포커스 후보 (옵션
      모드 전체 이식 규모 큼). Standard 모드의 `isClimbing` 오염만 해소.

#### B-21. `isCeilingClimbing` 해제 엣지 이식 (A-3 발견)
- [x] B-21. ✅ **세션 57 완료 (B-14 로 자동 해소)** — 원본 L1485 resetClimbing 이 매 틱
      `isCeilingClimbing = false` 리셋. B-14 의 resetClimbing() 이식으로 자동 해소.

#### B-22. 미이식 필드 4건 이식 (A-4 발견)
- [x] B-22a. ✅ **세션 38 완료** — `wasHeadJumping` 필드 추가 + resetState 리셋.
- [x] B-22b. ✅ **세션 38 완료** — `wasRunning` 필드 추가 + resetState 리셋.
- [x] B-22c. ✅ **세션 42 완료** — 원본은 필드 아닌 **메서드만** 존재 (L3239-L3242).
      수정 내용:
      (1) `vanilla()` private 헬퍼 이식 (원본 L3327-L3330 `!Config.enabled ||
          Config._vanillaStyle.value` → `!cfg.enabled || cfg.vanillaStyle`)
      (2) `isRunning(ClientPlayerEntity player)` public 메서드 이식
          (원본 override `isSprinting() && !isFast && (onGround || vanilla())` 완전 일치)
      (3) handleExhaustion L1163 로컬 변수 `isSprinting && !isFast && onGround` → 메서드
          호출 `isRunning(player)` 로 교체. 기존 부정확 주석 ("vanilla() 는 false") 제거.
      **필드 승격 불필요** 판정 — 원본이 메서드만 사용하므로 1:1 원칙상 메서드 이식.
- [x] B-22d. ✅ **세션 38 완료** — `isStanding` 필드 추가 + resetState 리셋. 갱신 공식
      L2734 은 B-30 범위.

#### B-23. `isHeadJumping` 매 틱 재평가 5-AND 공식 이식 (A-4 발견)
- [x] B-23. ✅ **세션 46 완료** — 원본 L2524-L2533 tickEssential 재평가 블록 이식:
      `wasHeadJumping = isHeadJumping;`
      `isHeadJumping = isHeadJumping && !isOnGround() && !(isSwimming_sm||isDiving) &&
      !(isFlying||capabilities.flying) && !(isTouchingWater && motionY<0) && !isInLava;`
      `if (!isHeadJumping) isAerodynamic = false;` (재평가 뒤 위치로 이동 — 원본 L2533 대응).
      SlideToHeadJumping 전환 앞에 배치 (원본 L2524 → L2546 순서 복원).
      B-24 (해제 엣지 후처리) 자리는 주석으로 확보.

#### B-24. 해제 엣지 후처리 이식 (A-4 발견)
- [x] B-24. ✅ **세션 53 완료** — 원본 L2535-L2540 이식:
      - Config 2필드 신설: `headFallDamageStartDistance=2F` / `headFallDamageFactor=2F`
        (원본 L395-L396)
      - ClientState `restoreFromFlying` boolean 필드 + resetState 리셋
      - ClientState `handleCrash(player, startDistance, factor)` public static 메서드
        (원본 L2232-L2243 — Climber 버전 동치)
      - B-23 자리에 `wasHeadJumping && !isHeadJumping && onGround → handleCrash +
        restoreFromFlying=true` 로직 추가
      - standupIfPossible 은 미이식 (별도 B-N) — restoreFromFlying 필드 설정은 선행 가능.

#### B-25. `isSliding` 직접 진입 6-AND 조건 복원 (A-4 발견)
- [x] B-25. ✅ **세션 55 완료** — IMPL-02 L1013-L1022 조건 원본 L2553 으로 정정:
      `cfg.slide && cfg.enabled && SmartMovingKeys.grab.isPressed() &&
      (isGroundSprinting || (wasRunning && !isRunning(player) && onGround)) &&
      !isCrawling && sneakKeyStartPressed && !isDipping`.
      필드 세팅: `isSliding=true; isHeadJumping=false; isAerodynamic=false` (원본 L2558-L2560).
      ※ wasRunning 저장 (B-43) 미이식 → wasRunning 분기 비활성. isGroundSprinting 분기만 활성.

#### B-26. 직접 진입 부수 동작 이식 (A-4 발견)
- [x] B-26. ✅ **세션 75 완료 (근사 이식)** — 원본 L2555-L2560 이식. ClientState B-25
      IMPL-02 블록 내 isSliding 직접 진입 성공 분기에 부수 동작 3건 추가:
      * `heightOffset = -1F;` (원본 L2555) 1:1
      * `player.move(MovementType.SELF, new Vec3d(0, -1D, 0));` (원본 L2556) 1:1
      * `tryJump(Config.SlideDown, false, wasRunning, null)` (원본 L2557) **생략** —
        §7 B-26 근사 등록. Jumper.SLIDE_DOWN 상수 + 전용 속도 공식 미이식. 효과: SlideDown
        전용 하강 점프 추진 모션 누락 (주로 이펙트).
      * `isSliding=true` / `isHeadJumping=false` / `isAerodynamic=false` — B-25 세션 55 이미 이식됨.

#### B-27. `fallDistance > _fallingDistanceMinimum` 분기 이식 (A-4 발견)
- [x] B-27. ✅ **세션 54 완료** — 원본 L2569-L2574 이식. SlideToHeadJumping 뒤에 배치:
      `if (isSliding && fallDistance > cfg.fallingDistanceMinimum) {
         isSliding=false; wasCrawling=true; isCrawling=false; }`.
      `fallingDistanceMinimum=3F` (세션 44 이식됨) 사용.

#### B-28. handleClimbing 진입 시 `isSliding=false` 이식 (A-4 발견)
- [x] B-28. ✅ **세션 63 완료 (B-37 로 자동 해소)** — A-4 B-28 과 A-5 B-37 은 동일 원본
      L985-L986 (wantClimbUp 분기 내 `isSliding && handsClimbing.IsRelevant()` →
      `isSliding=false; isCrawling=true`) 을 가리킴. 세션 62 B-37 이식으로 자동 해소.
      §10 규칙 "근사 이식이면 '근사 이식 — 원본과 차이: X' 주석" 해당 없음 — 1:1 이식.

#### B-29. `toSlidingOrCrawling` 조건 정정 (A-4 발견)
- [x] B-29. ✅ **세션 54 완료** — `SmartMovingJumper.resetHeightOffset` L103 조건 원본 L2226
      1:1 복원:
      `cfg.slide && cfg.enabled && (SmartMovingKeys.grab.isPressed() || sm.wasHeadJumping)`.
      기존 `(player.isSprinting() || sm.isFast) && cfg.slide` 간소/대체 매핑 제거.
      `wasHeadJumping` 은 B-22a 이식 + B-23 에서 tickEssential 저장 완료.

#### B-30. `isStanding` 갱신 공식 이식 (A-4 발견)
- [x] B-30. ✅ **세션 43 완료** — tickEssential 에 원본 L2734 공식 추가:
      `isStanding = horizontalSpeedSquare < 0.0005` (horizontalSpeedSquare = motionX² + motionZ²).
      R-09 블록 진입 직전 (isSmall 뒤) 에 추가. 블록 스코프로 변수명 충돌 방지.

#### B-31. 미이식 필드 3건 이식 (A-5 발견)
- [x] B-31a. ✅ **세션 41 완료** — 기존 `wasCrawling_st` (R-09 전용) 을 `wasCrawling` 으로
      개명하여 원본 L3074 public 필드와 이름 일치시킴. 5곳 참조 일괄 갱신 (L383/L700/
      L910/L959/L1045). 개명 후 동일 필드가 다용도로 사용됨 — 현재 이식된 용도는
      (1) tickEssential 이전 틱 저장 + (2) R-09 willStartCrawl 판정 2건. 나머지 (3)(4)(5)
      갱신 위치는 B-33/B-34/B-35/B-36/B-41 수정 시 추가 등록.
- [x] B-31b. ✅ **B-41 세션 70 에서 공식 이식 완료** — `wantCrawlNotClimb` 필드 (세션 38)
      + 갱신 블록 (B-41 세션 70 `wantCrawlNotClimb = (이전값 || (grabJustPressed &&
      !wasCrawling)) && grab.isPressed() && forward>0 && isCrawling && horizontalCollision;`)
      이식 완료. 상태 `[~]` → `[x]` 로 갱신 (세션 88 문서 정리).
- [~] B-31c. **필드만 이식 완료 (세션 38) / 공식 → Extended Phase 4 B-31c-post** —
      `initializeCrawling` 필드 추가. 공식 이식 (true 설정 경로) 은 미이식 — 현재 항상 false.
      Extended Phase 4 B-31c-post 에서 이식 예정 (Agent WebFetch 로 원본 true 설정 지점
      확인 필요).

#### B-32. `canCrawl` 공식 원본 5-AND 복원 (A-5 발견 — 1:1 원칙 위배)
- [x] B-32. ✅ **세션 44 완료** — ClientState IMPL-01 `canCrawl` 공식 원본 L2434-L2439 로
      정정. 잉여 5조건 (`!isCrawlClimbing && !isCeilingClimbing && !isSliding &&
      !isHeadJumping && !isFlying`) 제거 + `player.fallDistance < cfg.fallingDistanceMinimum`
      추가. 전제 작업: `SmartMovingConfig.fallingDistanceMinimum = 3F` 필드 추가 (원본 L348).

#### B-33. 메인 공식 재작성 — 매 틱 재계산 구조 (A-5 발견)
- [x] B-33. ✅ **세션 84 완료** — IMPL-01 이원화 구조 → 원본 매 틱 공식 1:1 전환.
      ClientState tickEssential IMPL-01 블록 (L1127-L1163) 전체 제거 후 원본 L2441-L2442
      3줄로 교체:
      ```java
      if (cfg.crawl) {
          boolean canCrawl = !isSwimming_sm && !isDiving
                  && (!isDipping || dippingDepth < 0.65F)
                  && !isClimbing
                  && player.fallDistance < cfg.fallingDistanceMinimum;
          wasCrawling = isCrawling;                              // 원본 L2441
          isCrawling = canCrawl && (wantCrawl || mustCrawl);     // 원본 L2442
      }
      ```
      기존 IMPL-01 의 "grab 재 누름 해제" 분기 제거 — 원본은 R-09 블록의 `willStopCrawl
      → crawlToggled=false` 자동 처리 + wantCrawl 의 `inputContinueCrawl` 체크로 해제.
      crawlToggled 설정은 toCrawling() (B-40/B-36/B-18b/B-35 등) 에서 독립.
      의존 전수 충족: B-31a wasCrawling (세션 41) / B-32 canCrawl (세션 44) / wantCrawl 필드
      L892 계산 / mustCrawl 필드 L848 계산.

#### B-34. capabilities.flying 해제 점프 이식 (A-5 발견)
- [x] B-34. ✅ **세션 59 완료** — 원본 L2449-L2450 이식. ClientState tickEssential
      IMPL-01 블록 (L988-L1021) 종료 직후, B-25 IMPL-02 앞에 배치:
      `if (wasCrawling && !isCrawling && player.getAbilities().flying) {
         SmartMovingJumper.tryJump(player, this, SmartMovingJumper.UP, 0F); }`.
      원본 `tryJump(Config.Up, null, null, null)` (4-param, angle==null) → 1.21.1
      `tryJump(player, sm, UP, 0F)` (2-param 축소, charge=0 → vanilla Up 경로) 표면 매핑.
      wasCrawling 은 L761 tickEssential 초반 일괄 저장된 이전 틱 값. isCrawling 은
      IMPL-01 종료 시점 최종값. B-31a wasCrawling 필드 이식 (세션 41) 완료 상태.

#### B-35. wasCrawling↔isCrawling 전환 후처리 이식 (A-5 발견)
- [x] B-35. ✅ **세션 74 완료 (근사 이식)** — 원본 L2822-L2836 이식. ClientState tickEssential
      B-17 블록 뒤 + R-09 블록 앞에 배치:
      * 분기 A (`wasCrawling && !isCrawling && !initializeCrawling && !flying`):
        `heightOffset = 0F` 리셋. `move(0, crawlStandUpBottom - minY, 0)` 이동량은 근사로 생략
        (§7 B-35 근사 등록).
      * 분기 B (`(isCrawling && !wasCrawling) || initializeCrawling`):
        `heightOffset = -1F` + `player.move(SELF, new Vec3d(0, -1D, 0))` + `if (initializeCrawling)
        toCrawling();` 1:1 이식.
      근사 사유: `crawlStandUpBottom` = `getMaxPlayerSolidBetween(minY-1, minY, ...)` 정밀
      AABB 스캔 미이식. 일반적으로 `crawlStandUpBottom ≈ minY` (발 아래 고체) 이므로 이동량 ≈ 0.
      의존 전수 충족: B-31a wasCrawling / B-31c initializeCrawling 필드 / B-40 toCrawling() /
      MovementType/Vec3d import (세션 73).

#### B-36. grab.StartPressed 수영/크롤 3분기 이식 (A-5 발견)
- [x] B-36. ✅ **세션 78 완료 (근사 이식)** — Agent WebFetch 로 원본 L2839-L2862 + 상수 4개
      확보 (MediumBorder=0.6F / BottomBorder=0.55F). ClientState tickEssential B-35 분기 B
      직후 + R-09 블록 앞에 원본 중괄호 없는 체이닝 구조를 중괄호로 명시화하여 이식:
      (a) `isShallowDiveOrSwim && wouldWantClimb`: `heightOffset=0F` + `_jumpPressed3a →
          isStillSwimmingJump=true`. `move(0, getMaxPlayerSolidBetween(...)-minY, 0)` 생략
          (§7 B-36 근사 — AABB 정밀 스캔 미이식).
      (b) `isDipping && wouldWantCrawl && depth>=0.55F && depth>=0.6F`: `heightOffset=-1F`
          + `player.move(0, -1.6F+dippingDepth, 0)` + `isCrawling=false` 1:1.
      (c) `isDipping && wouldWantCrawl && depth>=0.55F && depth<0.6F`: `heightOffset=-1F`
          + `player.move(0, -1D, 0)` + `wasCrawling=toCrawling()` 1:1.
      의존 전수 충족: B-36-pre wouldWantClimb/wouldWantCrawl (세션 77), B-10c
      isStillSwimmingJump 필드, B-40 toCrawling(), B-46 grabJustPressed. B-10a
      isShallowDiveOrSwim 공식 미이식 → (a) 분기 항상 비활성 (필드 false).
- [x] **B-36-pre** ✅ **세션 77 완료** — B-36 본 원자 선행 의존 필드 2개 승격:
      `wouldWantClimb` / `wouldWantCrawl` ClientState public boolean 신설. B-16c 블록 지역
      `wouldWantClimb16` → `this.wouldWantClimb` 필드 할당. pre-compute 블록 지역
      `wouldWantCrawl_` → `this.wouldWantCrawl` 필드 할당 (`_` 별칭 유지). resetState 리셋
      2건 추가. 원본 L2838-L2861 본문은 리서치에 없음 — B-36 본체 이식 시 Agent WebFetch 필요.

#### B-37. handleClimbing wall 오르기 crawl 진입 이식 (A-5 발견)
- [x] B-37. ✅ **세션 62 완료** — 원본 L985-L986 이식. `SmartMovingClimber.handleClimbing`
      L385 `if (wantClimbUp)` 블록 내부 최상단 (속도 분기 앞) 에 추가:
      `if (sm.isSliding && handsClimbing.isRelevant()) { sm.isSliding=false;
      sm.isCrawling=true; }`. 슬라이딩 상태에서 grab+전진 입력하면 크롤로 전환하여
      벽 오르기 시작 경로 복원. 원본 L987 `handsClimbing.ToUp()` 은 별도 이슈 (B-37
      범위 외).

#### B-38. handleCeilingClimbing 진입 시 isCrawling=false 이식 (A-5 발견)
- [x] B-38. ✅ **세션 58 완료** — 원본 L1170 이식. `SmartMovingClimber.handleCeilingClimbing`
      성공 분기 종료부 (L577 `sm.isCeilingClimbing = true` 직후, 분기 닫는 `}` 직전) 에
      `sm.isCrawling = false;` 추가. 원본 L1162 isCeilingClimbing=true 와 L1170
      isCrawling=false 가 동일한 성공 분기 내부 (속도/fallDistance 세팅 뒤) 에 위치하는
      원본 순서 그대로 복원.

#### B-39. landMotionPost 3분기 (isSlow + 0.5D) 이식 (A-5 발견)
- [x] B-39. ✅ **세션 80 완료 (근사 이식, 구조 복원)** — ClientState.fromSwimmingOrDiving
      에 원본 L1392-L1403 3분기 구조 복원. `else` 분기 추가 (`canStandUp && !hasLiquidCeiling`
      뒤). 본문은 no-op 주석만 — `crawlStandUpBottom` AABB 스캔 미이식 → `minY + 0.5D`
      조건 항상 false → isSlow 크롤 전환 + 이동량 모두 생략. §7 B-39 근사 등록. 완전 이식은
      focus_14 AABB 정밀 완료 시 자동 활성 경로 확보 — 주석으로 명시.

#### B-40. `toCrawling()` 헬퍼 메서드 신설 + 호출 지점 정리 (A-5 발견)
- [x] B-40. ✅ **세션 44 완료** — 원본 L3047-L3054 `toCrawling()` 메서드 이식
      (`SmartMovingClientState` 에 public 메서드 추가). 공식:
      `isCrawling=true; if (cfg.crawlToggle && cfg.enabled) crawlToggled=true;
      ignoreNextStopSneakButtonPressed=true; return true;`
      기존 IMPL-01 inline 3줄 → `toCrawling()` 호출로 교체. **cfg.enabled 가드 포함 —
      기존 inline 은 누락했었음 (§16 세션 44 기록)**.
      호출 지점 다른 5곳 (L2566/L2760/L2767/L2812/L2835/L2860) 은 B-35/B-36 이식 시 추가.

#### B-41. `wantCrawlNotClimb` 갱신 블록 이식 (A-5 발견)
- [x] B-41. ✅ **세션 70 완료** — 원본 L2451-L2463 이식. ClientState tickEssential IMPL-01
      + B-34 뒤 + B-25 IMPL-02 앞에 배치:
      `wantCrawlNotClimb = (wantCrawlNotClimb || (grabJustPressed && !wasCrawling))
                        && grab.isPressed() && moveForward > 0F
                        && isCrawling && horizontalCollision;`
      의미: 크롤 중 전진+grab+수평 충돌 시 "등반 아닌 크롤 선호" 플래그. B-16c 이식된
      `wouldWantClimb` 의 `!wantCrawlNotClimb` 억제 조건 활성화. 의존: B-31b 필드 이식
      (세션 38) / B-31a wasCrawling (세션 41) / B-46 grabJustPressed (세션 66) 모두 완료.

#### B-42. `mustCrawl` AABB 정밀 개선 (A-5 발견 — 근사 이식 기록)
- [~] B-42. **~~세션 79 별도 포커스 분리 완료~~ (세션 88 취소) → Extended Phase 6 (B-42a~d
      + B-42-B5/B16/B20/B26/B35/B36/B39/B18) 로 이전**. AABB 정밀 근사 개선 + §7 근사 8건
      일괄 승격. 세션 88 엄격 완료 결정으로 포커스 #2 범위 복원. 상세
      [`focus_02_extended.md#phase-6`](focus_02_extended.md) 참조.

#### B-43. R-09 블록 종료부 저장 2건 이식 (A-6 발견)
- [x] B-43. ✅ **세션 56 완료** — 원본 L3043-L3044 이식. ClientState R-09 블록 종료부
      (L1261 sneakKeyStopPressed 처리 뒤, 블록 `}` 직전) 에 2줄 추가:
      `wasRunning = isRunning(player);`
      `wasLevitating = isLevitating;`
      **부산물** — `wasLevitating` public 필드 신설 (wasRunning 옆) + resetState 리셋 추가.
      B-10d 미해소로 `isLevitating` 은 여전히 기본값 false 유지이나 저장 라인 1:1 이식만 먼저
      수행. B-25 의 `wasRunning && !isRunning && onGround` isSliding 직접 진입 분기 정상
      활성. L1022 기존 "미이식 → 항상 false" 주석을 "B-43 이식 완료" 로 갱신.

#### B-44. 이력 3개 저장 시점 정밀 조정 (A-6 발견)
- [x] B-44a. ✅ **세션 43 완료** — `wasSneaking = isSlow` 저장을 tickEssential 초반
      (L709 일괄 저장) → isSlow 공식 직전 (원본 L2716 대응) 으로 이동. B-2 수정 시 함께.
- [x] B-44b. ✅ **세션 84 완료 (B-33 동시)** — `wasCrawling = isCrawling` 저장을 tickEssential
      초반 일괄 저장 L829 에서 B-33 isCrawling 매 틱 공식 직전 (원본 L2441 대응) 으로 이동.
      B-33 과 불가분 묶음 처리 (세션 81 B-18 + B-18-pre 패턴). 초반 저장 주석을 "이동 완료"
      로 갱신 — tickEssential 초반 일괄 저장 블록 전부 제거됨 (wasSneaking: B-44a 세션 43,
      wasClimbCrawling: B-44c 세션 82, wasCrawling: B-44b 세션 84).
- [x] B-44c. ✅ **세션 82 완료** — `wasClimbCrawling = isClimbCrawling` 저장을 tickEssential
      초반 일괄 저장 L830 에서 B-18 isClimbCrawling 공식 직전 (원본 L2786 대응) 으로 이동.
      B-18 본체 지역 변수 `boolean wasClimbCrawling = isClimbCrawling` → `this.wasClimbCrawling`
      public 필드 저장으로 전환. 동일 값이므로 B-18 공식 결과 영향 없음. 초반 저장 주석도
      갱신 (B-44c 이동 완료 기록). 원본 구조 1:1 정렬 완료.

#### B-45. `isSneakToggleEnabled()` / `isCrawlToggleEnabled()` Config 헬퍼 신설 + 호출 정리 (세션 44 발견)
- [x] B-45a. ✅ **세션 45 완료** — `SmartMovingConfig.isSneakToggleEnabled()` /
      `isCrawlToggleEnabled()` 헬퍼 2개 추가 (원본 SmartMovingOptions L449-L468 AND 패턴).
- [x] B-45b. ✅ **세션 45 완료** — ClientState 6곳 + Jumper 1곳 전수 헬퍼 치환:
      (1) ClientState L738 inputContinueCrawl / (2) L788 sneakContinueInput /
      (3-4) L943-L944 R-09 블록 / (5) L959 wantSneak_ / (6) toCrawling() 내부 L1173 /
      (7) Jumper L110 → sm.toCrawling() 호출로 승격 (ignoreNextStopSneakButtonPressed
      자동 포함). cfg.enabled 누락 3곳 해소 (L788/L959/Jumper L112).

#### B-46. `grab.wasPressed()` 다중 호출 해소 (§16 세션 65 발견)
- [x] B-46. ✅ **세션 66 완료** — vanilla `KeyBinding.wasPressed()` 의 카운터 소비성
      (같은 틱 2회째부터 false) 으로 인해 tickEssential 내 2곳 호출이 순서 의존적 오동작
      가능. **해소**:
      (1) ClientState `grabJustPressed` public boolean 필드 신설 (jumpKeyStopPressed 뒤).
      (2) tickEssential 초반 sneakKey 엣지 계산 직후 `grabJustPressed =
          SmartMovingKeys.grab.wasPressed();` 1회 저장.
      (3) pre-compute 블록 L786 `boolean grabJustPressed0 = SmartMovingKeys.grab.wasPressed();`
          제거 + L828 참조를 `grabJustPressed` 필드로 변경.
      (4) IMPL-01 L989 `boolean grabJustPressed = SmartMovingKeys.grab.wasPressed();` 지역 변수
          제거 — L1013 참조는 public 필드로 자연 resolve.
      (5) resetState 리셋 추가.
      원본 `grabButton.StartPressed` 시멘틱 (틱 내 불변 불리언) 정렬.

#### B-N. A-7 이후 추가 발견에 따라 동적 추가

---

### ⭐ Extended 이전 원자 안내 (세션 88)

다음 원자들은 `[~]` 상태로 **[`focus_02_extended.md`](focus_02_extended.md) 에서 계속 진행**:

| 원자 | Extended Phase | 내용 요약 |
|------|---------------|-----------|
| B-7 | Phase 5 (B-7a/b/c) | updateSwimState 진입 조건 + 용암 등반 |
| B-9 | Phase 5 (B-9a~g) | 메인 분류 3-갈래 재작성 + offset 테이블 |
| B-10a | Phase 4 (B-10a-post) | isShallowDiveOrSwim 공식 |
| B-10b | Phase 4 (B-10b-post) | wantJumpOutOfWater / isJumpingOutOfWater 공식 |
| B-10c | Phase 4 (B-10c-post) | isStillSwimmingJump false 리셋 |
| B-11 | Phase 5 | 얕은 물 특수 분기 |
| B-19 | Phase 3 (B-19a/b/c/d) | Orientation/ClimbGap/isNeighborClimbing 갱신 (**최우선**) |
| B-20 (Simple/Smart) | Phase 8 (B-20b/c) | Simple/Smart Base Climb 완전 이식 |
| B-31c | Phase 4 (B-31c-post) | initializeCrawling true 설정 공식 |
| B-42 | Phase 6 (B-42a~d + 승격 8건) | AABB 정밀화 + §7 근사 일괄 해소 |
| §16 세션 65 (2) | Phase 7 (B-48a/b/c) | sprint 엣지 + isGroundSprinting 전환 후처리 |
| §16 세션 65 (3) | Phase 7 (B-49) | grabKeyStopPressed |
| B-1c SmartStatistics 근사 (§7 추가) | Phase 9 (B-50) | SmartStatisticsFactory 이식 |
| Options `_runOnSprintRelease/_walkOnSprintRelease` | Phase 9 (B-48b-dep) | Options 필드 + B-48b 의존 |

모든 미완료 항목은 Extended 에서 관리 — 본체는 완료된 기록 보존 목적.

### C. 검증
- [x] C-1. ✅ **세션 85 완료** — `./gradlew clean build` 성공 (10 actionable tasks, 9s).
      compileJava + compileClientJava + remapJar + assemble + build 전 단계 통과. 경고는
      Gradle 10 호환성 관련 deprecation 뿐 (기능 영향 없음).
- [x] C-2. ✅ **세션 86 완료** — §14 회귀 방지 감사 전수 통과. 4개 체크포인트 (R-09 토글
      블록 / wouldWantSneak·wouldWantCrawl / sendStatePacket·processStatePacket /
      sm_isSneaking override) + 공통 회귀 감사 체크리스트 (B/C/D) 모두 ✓. 상세 감사 결과는
      §14 표 갱신 기록. 결론: 상태 소비처 (#1/#3/#4) 에 영향 주는 회귀 없음.
- [x] C-3. ✅ **세션 87 완료** — checklist_original_audit.md L1100 에 포커스 #2 감사 행
      추가 (기존 #5/#6 동일 형식). 54 원자 완료 요약 + B Phase 1/2 핵심 이식 + 4 원자 별도
      포커스 분리 (B-7/B-9+B-11/B-19/B-42 → focus_14/15/16/17) + §7 근사 7건 등록 + C-1/C-2
      통과 전체 기록.
- [ ] C-4. 사용자 인게임 재검증 (§3 재현 케이스 실제 채워지면 매칭 확인)
      — **세션 88: Extended §6 로 이전 (Phase 3~8 완료 후 수행)**
- [ ] C-5. `playtest_fixes.md` "현재 포커스" → `#3` 갱신
      — **세션 88: Extended §6 로 이전 (Phase 3~8 완료 후 수행)**

---

## 11. 호출 타이밍 검증

(재현 케이스 확보 후 — 상태 갱신이 일어나는 훅 위치 원본과 대조)

---

## 12. 테스트 프로토콜

```
디버깅 방법:
  - F3 화면에 SM 상태 필드 표시 (신규 HUD 오버레이 필요할 수도)
  - 또는 임시 LOGGER.info 로 상태 값 매 틱 출력
  - 플레이테스트 중 특정 키 조합 수행 → 로그 비교
```

---

## 13. 완료 전 검증 체크리스트

> **주의** (세션 88): 아래 체크리스트는 포커스 #2 **최종 완료** 기준.
> Phase 3~8 Extended 작업 전체 완료 + Extended §6 최종 C 단계 (C-4/C-5) 통과 후 해소.
> 현재 본체 B Phase 1/2 + C-1/C-2/C-3 까지만 완료 — Extended 진행 후 재평가.

- [ ] §3 표의 모든 케이스가 "예상 == 실제" 매칭
- [ ] 각 수정이 리서치 파일 원본 라인과 1:1 대응
- [ ] 신규 발견 등록
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

**세션 86 (2026-04-24) 감사 결과** — C-2 체크포인트 전수 통과.

| 기존 이식 | 영향 포인트 | 확인 | 감사 결과 (세션 86) |
|----------|-----------|------|---------------------|
| R-09 토글 블록 | `wasSneaking` / `wasCrawling_st` 저장 시점 변경하면 토글 블록 깨짐 | [x] | `wasSneaking` (B-44a 세션 43 공식 직전 이동) / `wasCrawling` (B-31a 세션 41 개명 + B-44b 세션 84 공식 직전 이동) 완료. R-09 블록이 매 틱 isCrawling 값 참조하므로 세션 84 B-33 IMPL-01 제거 후에도 `willStopCrawl → crawlToggled=false` 자동 해제 경로 일관성 유지 ✓ |
| `wouldWantSneak` / `wouldWantCrawl` | 참조 필드 의미 변경 시 연쇄 영향 | [x] | wouldWantCrawl 필드 승격 (B-36-pre 세션 77, 지역 별칭 유지). wouldIsSneaking B-3b 세션 49 `!wantSprint` 정정. wantCrawl = crawlingEnabled && wouldWantCrawl 공식 그대로 → B-33 매 틱 공식에 의미 유지 ✓ |
| `sendStatePacket` / `processStatePacket` | State 비트 매핑 변경하면 다른 플레이어 렌더 깨짐 | [x] | 비트 매핑 변경 없음 (SmartMovingState 필드/비트 위치 유지). 세션 71 isLevitating 강제 false 제거는 계산 갱신만 — 네트워크 전송 형식 불변 ✓ |
| `sm_isSneaking` override | isSlow → isSneaking 순환 주의 | [x] | pre-compute 블록에서 `sneakPressedRaw` 사용 (L784) 로 순환 방지 이미 적용. 세션 84 B-33 IMPL-01 제거로 `player.isSneaking()` 호출 소멸 → 순환 위험 오히려 감소 ✓ |

**추가 감사** (공통 회귀 감사 체크리스트 §playtest_fixes §B/C/D):
- [B] B-N 원자별 필드/메서드 시그니처 변경 — 호출처 전수 확인 완료 (Config 헬퍼/KeyBinding 매핑).
  B-16a/b/c (세션 69) 신규 헬퍼 추가 외 기존 시그니처 보존.
- [C] 공통 플래그 (`enabled` / `isCrawling` / `isSlow` / `isFast` / `toggler` / `sneakToggled`
  등) 의미 변경 확인 — checklist_original_audit.md 해당 완료 항목 (포커스 #5/#6) 재검토 불필요
  (B Phase 1/2 수정이 상태 플래그 계산 규칙만 수정, 의미 자체는 원본 1:1 유지).
- [D] 네트워크 페이로드 (ConfigContentPayload / StatePayload / SpeedChangePayload) 변경
  없음 — 세션 29-85 작업 전체에서 네트워크 프로토콜 미변경.

**결론**: 회귀 방지 감사 전수 통과. C-2 체크박스 [x].

---

## 15. 작업 기록

### 세션 29 — 2026-04-24 — 방식 전환 + A-0 감사 계획

**배경**: 사용자 지적 "원본 코드와 비교해서 1:1 번역 되어있는지 원자 단위까지 검수하면
고쳐지는 거 아니냐" — 세션 29 시작 시점 프롬프트의 "재현 케이스 진입 전 필수" 제약에
대한 의문 제기. 포커스 #5/#6 는 실제로 grep + 1:1 비교 기반으로 진행했음을 확인 →
포커스 #2 에도 동일 패턴 적용으로 방식 전환.

**진행한 작업**:
- §3 "진입 전 필수" 제약 완화 — "보조 참고용" 으로 전환. 재현 케이스 있으면 A-N 우선순위
  판정용, 없어도 감사 진행 가능.
- §10 재정의:
  - P 단계 (재현 케이스) 제거 → A 단계 (grep 기반 감사) + B 단계 (매핑 결과 기반 수정)
    + C 단계 (검증) 로 단순화. 포커스 #6 A-1/A-2/A-3/B-N 패턴 그대로 적용.
  - A-0 감사 계획 수립 — 13필드 의존 그래프 + 그룹 분해 A-1~A-7.
  - B-N 은 A-7 매핑 결과에 따라 동적 추가 (포커스 #6 선례).
- §1 상태 "대기" → "진행 중 (A-0 감사 계획 수립)".

**A-0 감사 그룹 결정** (우선순위 근거):
- A-1 `isSlow`/`isFast` — 레벨 1, 광범위 사용, R-09 토글 연관
- A-2 수중 3상태 `isSwimming_sm`/`isDiving`/`isDipping` — Swimmer 집중
- A-3 등반 계열 4상태 `isClimbing`/`isCeilingClimbing`/`isCrawlClimbing`/`isClimbCrawling`
- A-4 전환 쌍 `isHeadJumping`/`isSliding`
- A-5 가장 복잡 `isCrawling`+`contextContinueCrawl`
- A-6 이력 스냅샷 3개
- A-7 전체 1.21.1 grep + 매핑 테이블

**완료 전 검증 체크리스트 (A-0 기준)**:
- [근거] `grep "public boolean is*"` 로 ClientState 13필드 전부 선언 확인 ✓
- [대응] 포커스 #6 A-1/A-2/A-3 패턴 재사용 — 방식 일관성 ✓
- [분기] 13필드 의존 그래프 레벨 0-3 분류 완료 ✓
- [상수/타이밍/근사] 해당 없음 (계획 단계)
- [신규] A-N 그룹 6개 + A-7 매핑 정의 ✓
- [회귀] 문서만 — 코드 영향 없음
- [빌드] 해당 없음

**다음 작업**: A-1 — `isSlow`/`isFast` Agent WebFetch. 원본 `SmartMovingSelf.java` 에서
두 필드가 갱신되는 모든 위치 덤프 → 1.21.1 `tickEssential` 갱신 위치와 side-by-side 비교.
R-09 토글 블록 + wouldWantSneak/wouldIsSneaking 공식 정합성 확인.

### 세션 29 (계속) — 2026-04-24 — A-1 (isSlow/isFast) 완료

**진행한 작업**:
- Agent WebFetch 로 원본 `SmartMovingSelf.java` 전수 감사 (`isSlow`/`isFast` 필드 선언 +
  할당 위치 + 의존 공식 + 버튼 필드 + R-09 토글 블록).
- **원본 할당 위치 정확히 3곳**:
  - Self L2275-L2276 `resetState()` 리셋
  - Self L2688-L2695 `isFast` 메인 공식 (6갈래 OR)
  - Self L2716-L2719 `isSlow` 메인 공식 (wantSneak && wouldIsSneaking)
- 1.21.1 `SmartMovingClientState` L637/L651/L653 비교 → **3건 불일치 확정** (§16 세션 29 상세):
  1. isFast 완전 오역 (원본 6갈래 OR vs 1.21.1 grab+isSprinting)
  2. isSlow 중복 + 가드 누락 (sneakingEnabled 체크 빠짐)
  3. wouldIsSneaking 의 wantSprint 이 vanilla isSprinting() 으로 대체
- 이전 세션 추정 정정:
  - 기존 주석 "isFast = grabButton.Pressed && isSprinting()" → **틀림**
  - 기존 주석 "wantSneak = sneakContinueInput 로 매핑" → **틀림** (wantSneak 은 별도)
- §10 B-1/B-2/B-3 신규 원자 작업 추가 — A-2 진행 전 선행 수정 가능 (추천) 또는 A 전체 완료
  후 일괄 수정.

**완료 전 검증 체크리스트 (A-1 기준)**:
- [근거] 원본 3곳 할당 위치 전수 덤프 ✓
- [근거] 의존 공식 (wantSneak / wouldWantSneak / wouldIsSneaking / wantSprint) 전부 확인 ✓
- [대응] 원본 3곳 ↔ 1.21.1 3곳 side-by-side — 불일치 3건 ✓
- [분기] 원본 6갈래 OR / 가드 조건 / 조건 순서 전부 식별 ✓
- [상수] 해당 없음 (공식만)
- [타이밍] isFast 는 isGroundSprinting/isClimbSprinting 등 계산 직후, isSlow 는 wasSneaking
  저장 직후 — 타이밍 원본과 다름 없음 확인
- [근사] 해당 없음 (엄밀 1:1 위반)
- [신규] B-1/B-2/B-3 원자 작업 추가 ✓
- [회귀] 코드 변경 없음 (감사)
- [빌드] 해당 없음

**다음 작업 선택지**:
- **옵션 A**: B-1/B-2/B-3 선행 수정 후 A-2 진행 — A-1 발견 즉시 수정. 불일치 쌓이기 전에
  고치는 원칙 (세션 29 내 규모 큼).
- **옵션 B**: A-2~A-6 먼저 진행 후 B-N 일괄 — 매핑 전체 완성 후 일관성 있게 수정. 세션
  여러 회에 걸쳐 감사 먼저.
- 기본 추천: **옵션 A** — A-1 불일치가 크고 의존 필드 많아 B-1 부터 단계적 수정 필요.
  B-1 의존 필드 (canHorizontallySprint 등) 가 미이식이면 규모 더 커짐 — 먼저 확인.

### 세션 30 — 2026-04-24 — 1:1 원칙 재확인 + §10 B 원자 분해

**배경**: 사용자 지시 — "이제부터는 진짜로 무조건 1:1 번역이 되어야 된다. 컨텍스트
터질 것 같으면 작업을 쪼개서 파일 체크리스트를 수정하면 되잖아. 상태와 관련된 모든
코드와 원본 리서치를 그대로 1:1 번역." → 세션 29 말미 내가 제안한 "B-2 경량 먼저"
하이브리드 절충안 철회. 근사·간소·대체 매핑 전부 금지 재확인.

**진행한 작업**:
- B-1/B-3 의존 필드 1.21.1 grep 전수 확인:
  * `isGroundSprinting` — Jumper 로컬 변수 1곳만 (ClientState 필드 아님)
  * `isClimbSprinting` / `isSwimSprinting` / `isDiveSprinting` / `isCeilingSprinting` /
    `isFlyingSprinting` — 전부 미이식
  * `canHorizontallySprint` / `canAllSprint` / `canAnySprint` / `isClimbSprintSpeed` —
    전부 미이식
  * `Config._sprintEnableStanding` — 1.21.1 Config 미이식
  * `wantSprint` — 주석에만 존재 (계산 블록 없음)
  * `standing` — 미이식
- 리서치 파일 상태 확인:
  * `docs/research/original/smartmoving/moving/SmartMovingSelf.md` 에 `wantSprint` /
    `isGroundSprinting` / `sprintButton` / `_sprintEnableStanding` 관련 블록 일부 존재
  * `can*` 4 판정은 리서치에도 미수록 → B-0 원자로 Agent WebFetch 덤프 선행
- "간소 매핑" 주석 2건 발견 (§16 세션 30):
  * `SmartMovingClientState.java` L788 — R-09 토글 블록 `wantSneak_/wantSprint_`
  * `SmartMovingSwimmer.java` L159 — 수영 상태 "1.21.1 간소화"
  → §10 B-4 / B-5 원자 추가
- §10 B 단계 재구성:
  * B-0 신설 — 원본 리서치 보강 (Agent WebFetch)
  * B-1 → B-1a~f 6원자 분해 (Config 필드 → Button 매핑 → can* → 6 Sprint 변종 →
    standing → isFast)
  * B-2 단일 유지 (isSlow — Config.isSneakingEnabled 헬퍼 신설 포함)
  * B-3 → B-3a~b 2원자 (wantSprint 신설 → wouldIsSneaking 정정)
  * B-4 R-09 간소 매핑 제거
  * B-5 Swimmer 간소화 원본 대조
  * B-N 동적 추가 (A-2~A-6 발견분)
- §1 진행 상황 + §16 신규 발견 갱신.

**완료 전 검증 체크리스트 (세션 30 기준 — 문서 재구성)**:
- [근거] 1.21.1 grep 으로 의존 필드 미이식 전수 확인 ✓
- [근거] 리서치 파일 기존 수록 범위 grep 확인 ✓
- [대응] 원본 관련 코드 전부 1:1 이식 원칙 명시 (§10 B 도입부) ✓
- [분기] B-1 6원자 / B-3 2원자로 분해 — 각 원자 의존 필드 명시 ✓
- [상수] `_sprintEnableStanding` = `"move.sprint.enable.ground"` 기본값 원본 참조 명시 ✓
- [타이밍] 계산 순서 (Sprint 변종 → standing → isFast / wantSprint → wouldIsSneaking →
  isSlow) 원자 순서에 반영 ✓
- [근사] 근사 매핑 금지 명시, B-4/B-5 로 기존 간소 매핑 제거 원자 추가 ✓
- [신규] §16 "간소 매핑" 2건 등록 ✓
- [회귀] 코드 변경 없음 (문서만)
- [빌드] 해당 없음

**다음 작업 선택지 (세션 30 기준)**:
- **권고 순서**: B-0 (리서치 보강) → A-2~A-6 병행 감사 → A-7 매핑 → B-1a~f → B-2 →
  B-3a~b → B-4 → B-5 → C. A-2~A-6 에서 수중/등반 필드의 sprint 의존 교차 발견 가능.
- 또는 B-0 → B-1a~f → B-2 → B-3a~b 먼저 완결 후 A-2~A-6 감사. 사용자 판단.

### 세션 31 — 2026-04-24 — 경로 A 확정 + B-0 (리서치 보강) 완료

**배경**: 사용자 판단 "1:1 번역 관점에서 어느 쪽이 나은가?" → 경로 A 확정
(B-0 → A-2~A-6 → A-7 → B-N → C). 완전 추출 원칙 + 미확인 차단 원칙 근거.

**진행한 작업**:
- `.tmp_research/SmartMovingSelf.java` (원본 전체 3345줄) 활용 — Agent WebFetch 대신
  로컬 파일 직접 읽기로 B-0 전수 덤프 수행
- `SmartMovingSelf.md` L2508 이후 **R-10 섹션 신설** — isFast/isSlow/wouldIsSneaking
  의존 체인 완전 덤프:
  * R-10.1 필드 선언 (L1413-L1448) — wantSprint/wouldIsSneaking/isGroundSprinting public
  * R-10.2 disabled 지역변수 (L2373-L2375)
  * R-10.3 sneakContinueInput / wouldWantSneak / wantSneak (L2576-L2590)
  * R-10.4 moveButtonPressed / moveForwardButtonPressed (L2592-L2593)
  * R-10.5 wantSprint 6조건 OR (L2595-L2615)
  * R-10.6 isSprintJump / exhaustionAllowsSprinting / preferSprint (L2633-L2657)
    — 순환 의존 발견: isFast 이전 틱 값 참조 (L2633/L2640)
  * R-10.7 isClimbSprintSpeed + can* 4 + 6 Sprint 변종 (L2659-L2684)
    — 의존 발견: SmartStatisticsFactory.getTickDistance (SmartRender 측, 1.21.1 이식
    확인 필요), collidedHorizontallyTickCount (미이식 가능성)
  * R-10.8 standing + isFast + isGroundSprinting 전환 후처리 (L2686-L2709)
    — vanilla setSprinting 호출 포함 (운전자 엣지 처리)
  * R-10.9 wouldIsSneaking + isSlow (L2711-L2719) — wasSneaking 지역변수 R-09 연결
  * R-10.10 isFast/isSlow 전수 사용처 (grep 25+ 위치) — A-7 매핑 기초 자료
  * R-10.11 1.21.1 이식 매핑 예비안 — Config 헬퍼 / KeyBinding / 순환 의존 / 미이식
    필드 신설 결정 지점
- focus_02 §5.2 에 R-10 인용 링크 + 범위 목록 추가
- 오타 정정 1건 (`다이비` → `다이빙`, SmartMovingSelf.md L2505 — 기존 문서 흔적)

**신규 발견 (B-N 후보 원자 추가 대상)**:
- **순환 의존**: `isSprintJump` L2633/L2640 이 이전 틱 `isFast` 참조 — B-1f 수정 시
  `wasFast` 저장 또는 계산 순서 재설계 필요
- **미이식 필드 2건 후보**:
  * `SmartStatisticsFactory.getTickDistance()` — 등반 sprint 속도 게이트 — 1.21.1 이식
    여부 확인 후 미이식이면 `true` 근사 또는 신설 원자
  * `collidedHorizontallyTickCount` — can* 판정 — 미이식이면 신설 원자 (B-1c 범위)
- **isGroundSprinting 전환 후처리** (L2697-L2709): vanilla `setSprinting()` 호출 +
  `Options._runOnSprintRelease` / `_walkOnSprintRelease` / `wasRunningWhenSprintStarted`
  필드 — 1.21.1 이식 여부 확인 필요. 미이식이면 B-N 추가 원자.

**완료 전 검증 체크리스트 (B-0 기준)**:
- [근거] 원본 SmartMovingSelf.java 전체 3345줄 로컬 확보 ✓
- [근거] grep 으로 isFast/isSlow/wouldIsSneaking + 의존 필드 전수 위치 확인 ✓
- [대응] 원본 L2373-L2719 범위 전체 덤프 (필드 선언 + 계산 블록 + 사용처) ✓
- [분기] 6 Sprint 변종 각 공식 / wantSprint 5 OR 게이트 / wouldWantSneak 7 && 조건
  전부 보존 ✓
- [상수] `_sprintEnableStanding` / `_diveDownOnSneak` / `_swimDownOnSneak` /
  `_sprintFactor` / `_freeClimbingUpSpeedFactor` / `_freeClimbingDownSpeedFactor` /
  `_sprintExhaustionStart` / `_sprintExhaustionStop` / `_sprintDuringItemUsage` 전부
  R-10.11 매핑 테이블 기록 ✓
- [타이밍] 계산 순서 (disabled → wouldWantSneak → wantSprint → Sprint 변종 → isFast
  → wouldIsSneaking → isSlow) 명시 + 순환 의존 isSprintJump 주의사항 기록 ✓
- [근사] 없음 (원본 덤프 단계)
- [신규] §16 에 순환 의존 + 미이식 2건 + setSprinting 후처리 발견 등록 (아래)
- [회귀] 코드 변경 없음 (리서치 문서만)
- [빌드] 해당 없음

**다음 작업**: A-2 — 수중 3상태 (`isSwimming_sm` / `isDiving` / `isDipping`) 원본
전수 감사. `.tmp_research/SmartMovingSelf.java` `handleSwimming` (L229-L576) +
필드 선언 + 사용처 덤프 → `SmartMovingSelf.md` R-11 섹션 또는 기존 handleSwimming
섹션 확장 → 1.21.1 `SmartMovingSwimmer.updateSwimState` grep + 매핑. B-5 (Swimmer
간소화 원본 대조) 함께 수행.

### 세션 32 — 2026-04-24 — A-2 (수중 3상태) 완료

**진행한 작업**:
- `.tmp_research/SmartMovingSelf.java` 원본 `handleSwimming` (L229-L576) + 리셋 위치
  (L1377-L1403 landMotionPost / L1488-L1498 resetSwimming / L2290-L2297 resetState) +
  `SmartMoving.java` 부모 필드 선언 (L38-L41) 전수 감사
- `SmartMovingSelf.md` **R-11 섹션 신설** (L2508 이후 R-10 뒤) — 12 서브섹션:
  * R-11.1 필드 선언 (이식 4건 + 미이식 5건 확인)
  * R-11.2 진입 조건 (3-OR 내부 + 2-AND 바깥)
  * R-11.3 사전 준비 (AABB 측정 + couldStandUp + wantShallowSwim +
    isFakeShallowWaterSneaking 설정 블록)
  * R-11.4 크롤/ClimbCrawl/CrawlClimb 강제 isDipping (3-OR)
  * R-11.5 메인 분류 3-갈래 + A/B 서브 분기 + 11-단계 offset 테이블
  * R-11.6 크롤↔수영 전환 (R-06 구간)
  * R-11.7 Config 게이트 + useStandard 재판정
  * R-11.8 수중 3상태 갱신 (L504-L511)
  * R-11.9 얕은 물 특수 분기 (isSlow 조합 crawl/walking 전환)
  * R-11.10 useStandard=true 경로 리셋
  * R-11.11 리셋 위치 (resetSwimming/resetState/landMotionPost)
  * R-11.12 1.21.1 side-by-side + 불일치 11건 + B-6~B-13 이식 우선순위
- 1.21.1 `SmartMovingSwimmer.updateSwimState` (L64-L91) + `ClientState` grep 결과:
  * 이식 필드: `isDipping` / `isSwimming_sm` / `isDiving` / `dippingDepth` /
    `waterMovementTicks` / `isFakeShallowWaterSneaking` / `isClimbCrawling` /
    `isCrawlClimbing`
  * **미이식 필드**: `isShallowDiveOrSwim` / `isJumpingOutOfWater` / `isStillSwimmingJump` /
    `isLevitating` / `isLiquidClimbing`
- **불일치 11건 확정** (§16 세션 32 기록) — 가장 심각: 메인 분류 공식 완전 대체
  (원본 3-갈래 + A/B 서브 + 11-단계 offset 테이블 vs 1.21.1 단순 offset 3분류)
- §10 B-6 ~ B-13 원자 신설 (동적 추가):
  * B-6 isClimbCrawling 누락 조건 / B-7 진입 조건 복원 / B-8 Config 게이트 /
  * B-9 메인 분류 재작성 (7 서브원자 권장) / B-10 미이식 필드 4건 /
  * B-11 얕은 물 특수 분기 / B-12 waterMovementTicks 증분 / B-13 isSliding 조건
- B-5 (Swimmer 간소화) 는 A-2 감사 결과에 흡수 — B-9 (메인 분류 재작성) 에서 함께 처리
- §1 진행 상황 + §5.2 R-11 인용 갱신

**완료 전 검증 체크리스트 (A-2 기준)**:
- [근거] 원본 `handleSwimming` L229-L576 + 리셋 3곳 + 부모 필드 선언 전수 확보 ✓
- [근거] 1.21.1 `SmartMovingSwimmer` L64-L91 + ClientState grep 확인 ✓
- [대응] 원본 갱신 위치 11곳 ↔ 1.21.1 updateSwimState side-by-side 완료 ✓
- [분기] 3-갈래 (`[0,2]`/`(2,∞)`/`(-∞,0)`) + A/B 서브 (`diveUp||moveSwim||wantShallowSwim`) +
  11-단계 offset 테이블 + 얕은 물 특수 2분기 전부 식별 ✓
- [상수] `SwimCrawlWaterTopBorder=0.65` / `SwimCrawlWaterMaxBorder=1.0` /
  `SwimCrawlWaterBottomBorder` / 1.9 다이빙 경계 / 1.4 수영 경계 / 0.1625 offset / 2.0
  border / 1.5 couldStandUp depth / 0.5 crawl 전환 offset 기록 ✓
- [타이밍] 갱신 순서 (resetSwimming → 분류 지역변수 → Config 게이트 → 필드 갱신
  → 얕은 물 특수 분기 → setHeightOffset) 기록 ✓
- [근사] SM 정밀 AABB → `getFluidHeight(WATER)` 근사 (§7 이미 기록)
- [신규] §10 B-6~B-13 원자 8개 추가 ✓
- [회귀] 코드 변경 없음 (리서치/문서만)
- [빌드] 해당 없음

**다음 작업**: A-3 — 등반 계열 4상태 (`isClimbing` / `isCeilingClimbing` /
`isCrawlClimbing` / `isClimbCrawling`) 전수 감사. `.tmp_research/SmartMovingSelf.java`
`handleClimbing` (L814-L1110) + `handleCeilingClimbing` (L1112-L1174) + 상태 전환
(L2736-L2820) 덤프 → 1.21.1 `SmartMovingClimber` grep + 매핑.

### 세션 33 — 2026-04-24 — A-3 (등반 4상태) 완료

**진행한 작업**:
- `.tmp_research/SmartMovingSelf.java` 원본 `handleClimbing` (L814-L1110) +
  `handleCeilingClimbing` (L1112-L1174) + 상태 전환 블록 (L2721-L2820) +
  `resetClimbing()` (L1474-L1486) + `setOnlyShouldClimbSpeed()` (L1515) +
  `resetState()` (L2278-L2287) 전수 감사
- `SmartMoving.java` 부모 필드 선언 (L30-L45) 추가 확인
- `SmartMovingSelf.md` **R-12 섹션 신설** — 11 서브섹션:
  * R-12.1 필드 선언 (9건 미이식 확인)
  * R-12.2 갱신 위치 맵 (원본 vs 1.21.1 side-by-side)
  * R-12.3 handleClimbing 구조 요약 (Standard/Simple/Smart/Free 4분기)
  * R-12.4 handleCeilingClimbing 구조 요약
  * R-12.5 isCrawlClimbing 5-AND 공식 + canStandUp 전환 블록
  * R-12.6 isClimbCrawling 공식 + climbIntoCount 카운터 (6→1 재장전 로직)
  * R-12.7 isClimbHolding/wantClimbHolding 3-OR 계산
  * R-12.8 의존 필드 계산 (isNeighborClimbing / hasClimbGap 등)
  * R-12.9 1.21.1 SmartMovingClimber side-by-side (L221/L479/L571/MixinL117)
  * R-12.10 **불일치 14건 표** (모두 [누락] 분류 — 거대 미이식 구간)
  * R-12.11 B-14~B-21 원자 예비안 (서브원자 분해 포함)
- 1.21.1 grep 결과:
  * 이식된 갱신: `isClimbing=true` L221 (setOnlyShouldClimbSpeed) + `isClimbing=false`
    L479 (handleClimbBackJump) + `isCeilingClimbing=true` L571 (handleCeilingClimbing) +
    resetState false 4건
  * **미이식 필드 9건**: `isNeighborClimbing` / `hasClimbGap` / `hasNeighborClimbGap` /
    `hasNeighborClimbCrawlGap` / `isVineOnlyClimbing` / `isVineAnyClimbing` /
    `isClimbingStill` / `handsEdgeBlock` / `feetEdgeBlock`
  * **갱신 로직 완전 미이식**: `isCrawlClimbing` / `isClimbCrawling` / `isClimbHolding` /
    `wantClimbHolding` — 필드는 있으나 갱신 계산 블록 전부 없음
  * **resetClimbing() 메서드 미이식** — Mixin L117 주석만 있고 실제 호출 없음
- §10 B-14~B-21 원자 신설 (B-15/B-19 는 서브원자 분해 포함):
  * B-14 resetClimbing 신설 / B-15a~f 미이식 필드 이식 / B-16 isClimbHolding 계산 /
    B-17 isCrawlClimbing 공식 / B-18 isClimbCrawling 공식+카운터 / B-19 의존 필드 갱신 /
    B-20 Standard/Simple Base Climb / B-21 isCeilingClimbing 해제 엣지

**완료 전 검증 체크리스트 (A-3 기준)**:
- [근거] 원본 handleClimbing + handleCeilingClimbing + 상태 전환 + resetClimbing +
  setOnlyShouldClimbSpeed + resetState 전수 확보 ✓
- [근거] 1.21.1 Climber + ClientState + MixinLivingEntityClient grep 확인 ✓
- [대응] 원본 갱신 위치 ↔ 1.21.1 side-by-side 매핑 완료 ✓
- [분기] 5-AND (isCrawlClimbing) + 카운터 로직 (climbIntoCount) + 3-OR (wantClimbHolding)
  + 진입/해제 엣지 블록 전부 식별 ✓
- [상수] `FAST_UP_MOTION` / `SLOW_UP_MOTION` / `MEDIUM_UP_MOTION` / `HOLD_MOTION` /
  `SINK_DOWN_MOTION` / `CLIMB_DOWN_MOTION` / `CLIMB_PULL_MOTION` / `CATCH_CRAWL_GAP_MOTION`
  / `climbIntoCount` 재장전값 6 기록 ✓
- [타이밍] 갱신 순서 (resetClimbing → Standard/Simple 분기 → Free 내부 판정 →
  setOnlyShouldClimbSpeed → isClimbHolding → isCrawlClimbing → isClimbCrawling) 기록 ✓
- [근사] isPlayerInSolidBetween 는 1.21.1 에 direct 대응 없음 → B-17 에서 근사 필요 기록
- [신규] §10 B-14~B-21 원자 8개 + 서브원자 (B-15a~f, B-19a~) 추가 ✓
- [회귀] 코드 변경 없음 (리서치/문서만)
- [빌드] 해당 없음

**다음 작업**: A-4 — 전환 쌍 `isHeadJumping` / `isSliding` 전수 감사.
`.tmp_research/SmartMovingSelf.java` L1607 toSlidingOrCrawling + L2524-L2560
isAerodynamic + L2546-L2550 SlideToHeadJumping 전환 덤프 → 1.21.1 ClientState grep 매핑.

### 세션 34 — 2026-04-24 — A-4 (전환 쌍 isHeadJumping/isSliding) 완료

**진행한 작업**:
- 원본 갱신 위치 전수 grep + 블록 확보:
  * isHeadJumping 6곳: L2128/L2218/L2294/L2524-L2530/L2549/L2559
  * isSliding 7곳: L985/L2227/L2296/L2548/L2558/L2565/L2571
  * isAerodynamic 4곳: L2533/L2550/L2560/resetState
  * 관련 메서드: toSlidingOrCrawling L2222-L2230 / 직접 진입 블록 L2553-L2561 /
    매 틱 재평가 L2524-L2540 / fallDistance 분기 L2569-L2574
- 1.21.1 grep 결과:
  * 이식된 갱신: Jumper L98/L104/L217 / Climber L472 / ClientState L702/L710/L711/L712/
    L715/L716 (직접 진입 + SlideToHeadJumping + isAerodynamic) / Slider L48/L76 /
    resetState L896/L912/L932
  * isAerodynamic 전체 4곳 이식 완료 (R-05/포커스#6 B-5 결과)
  * **미이식 필드 4건**: `wasHeadJumping` / `wasRunning` / `isRunning`(필드) / `isStanding`
  * **미이식 블록 6건**: isHeadJumping 매 틱 재평가 5-AND / 해제 엣지 후처리 /
    직접 진입 부수 동작 3건 / fallDistance 분기 / handleClimbing 진입 isSliding=false /
    toSlidingOrCrawling 조건 정정
  * **상수 확인**: `SlideToHeadJumpingFallDistance = 0.05F` 일치 ✓
- `SmartMovingSelf.md` **R-13 섹션 신설** (10 서브섹션):
  * R-13.1 필드 선언 / R-13.2 갱신 위치 맵 / R-13.3 매 틱 재평가 / R-13.4 직접 진입
    6-AND / R-13.5 fallDistance 분기 / R-13.6 toSlidingOrCrawling / R-13.7 isAerodynamic
    이식 완료 / R-13.8 미이식 필드 / R-13.9 **불일치 13건** / R-13.10 B-22~B-30 예비안
- §10 B-22 ~ B-30 9 원자 신설 (B-22a~d 서브 4 포함):
  * B-22 미이식 필드 4건 / B-23 매 틱 재평가 / B-24 해제 엣지 / B-25 직접 진입 조건 /
  * B-26 부수 동작 / B-27 fallDistance 분기 / B-28 handleClimbing 해제 / B-29 toSlidingOrCrawling / B-30 isStanding 공식
- §1 진행 상황 갱신, §5.2 R-13 인용 추가

**완료 전 검증 체크리스트 (A-4 기준)**:
- [근거] 원본 isHeadJumping/isSliding/isAerodynamic 갱신 위치 17곳 전수 확보 ✓
- [근거] 매 틱 재평가 + 직접 진입 + fallDistance + toSlidingOrCrawling 블록 전수 읽기 ✓
- [대응] 원본 17곳 ↔ 1.21.1 대응 위치 side-by-side 완료 ✓
- [분기] 매 틱 재평가 5-AND / 직접 진입 6-AND / 해제 엣지 후처리 / fallDistance 분기 /
  toSlidingOrCrawling 2갈래 전부 식별 ✓
- [상수] `SlideToHeadJumpingFallDistance = 0.05F` / `_fallingDistanceMinimum` /
  `_headFallDamageStartDistance` / `_headFallDamageFactor` / `_slidingSpeedStopFactor`
  기록 ✓
- [타이밍] 갱신 순서 (wasHeadJumping 저장 → isHeadJumping 재평가 → isAerodynamic 리셋 →
  해제 엣지 handleCrash → 직접 진입 6-AND → SlideToHeadJumping → 수평속도 판정 →
  fallDistance 판정) 기록 ✓
- [근사] isAerodynamic 은 R-05 이미 1:1 이식됨 확인 ✓
- [신규] §10 B-22~B-30 원자 9개 추가 ✓
- [회귀] 코드 변경 없음 (리서치/문서만)
- [빌드] 해당 없음

**다음 작업**: A-5 — `isCrawling` + `contextContinueCrawl` 전수 감사 (가장 복잡,
종합 의존). 원본 `handleSwimming` 크롤 분기 + tickEssential 크롤 판정 +
updateEntityActionState L2347-L3044 전체 크롤 관련 + R-09 블록 (L2966-L3045).

### 세션 35 — 2026-04-24 — A-5 (isCrawling + contextContinueCrawl) 완료

**진행한 작업**:
- 원본 `.tmp_research/SmartMovingSelf.java` 갱신 위치 전수 grep:
  * isCrawling 19곳 / contextContinueCrawl 4곳 / wantCrawl/mustCrawl/canCrawl 계산 블록
  * tickEssential L2395-L2450 (mustCrawl + inputContinueCrawl + contextContinueCrawl 해제
    + wouldWantCrawl + wantCrawl + canCrawl 5-AND + 메인 공식)
  * toCrawling() 함수 L3047-L3054
  * 전환 후처리 L2822-L2836 + grab.StartPressed 3분기 L2838-L2861
  * wall 오르기 L985-L986 / handleCeilingClimbing L1170 / landMotionPost L1377-L1403
- 1.21.1 ClientState IMPL-01 블록 (L661-L693) + pre-compute (L564-L622) +
  fromSwimmingOrDiving (L1056-L1070) side-by-side
- `SmartMovingSelf.md` **R-14 섹션 신설** (11 서브섹션):
  * R-14.1 필드 선언 (3건 미이식) / R-14.2 갱신 위치 맵 (원본 19곳 + contextContinueCrawl 4곳) /
  * R-14.3 tickEssential 크롤 판정 블록 (메인 공식 포함) / R-14.4 1.21.1 IMPL-01 구조 차이 /
  * R-14.5 toCrawling() 함수 / R-14.6 wall 오르기 / R-14.7 landMotionPost 3분기 /
  * R-14.8 grab.StartPressed 수영/크롤 3분기 / R-14.9 wasCrawling↔isCrawling 전환 후처리 /
  * R-14.10 **불일치 15건** / R-14.11 B-31~B-42 원자 예비안
- **핵심 발견**:
  * 메인 공식 **구조 차이** — 원본 매 틱 재계산 vs 1.21.1 IMPL-01 진입/해제 이원화 (중대)
  * **canCrawl 9-AND 잉여** — 원본 5-AND (1:1 원칙 위배, 근사 주석 달려있음)
  * **wasCrawling 필드 누락** — wasCrawling_st (R-09 전용) 와 혼동 방지 필요
  * `grab.StartPressed` 수영/크롤 3분기 전체 미이식 (A-2 isShallowDiveOrSwim 의존)
  * 전환 후처리 (heightOffset + move) + `initializeCrawling` 필드 미이식
  * wall 오르기 `isSliding=false + isCrawling=true` 쌍 전환 미이식
  * landMotionPost 3분기 중 isSlow+0.5D 분기 1개 누락
- §10 B-31 ~ B-42 12 원자 신설 (B-31 서브 3개 포함 최대 14):
  * B-31 미이식 필드 3건 / B-32 canCrawl 5-AND 복원 / B-33 메인 공식 재작성
  * B-34 capabilities.flying 해제 점프 / B-35 전환 후처리 / B-36 grab 3분기
  * B-37 wall 오르기 / B-38 ceiling 진입 / B-39 landMotionPost 3분기
  * B-40 toCrawling 헬퍼 / B-41 wantCrawlNotClimb / B-42 mustCrawl AABB 정밀
- §1 진행 상황 갱신, §5.2 R-14 인용 추가

**완료 전 검증 체크리스트 (A-5 기준)**:
- [근거] 원본 isCrawling 19곳 + contextContinueCrawl 4곳 + 의존 계산 블록 전수 확보 ✓
- [근거] tickEssential 크롤 판정 + 전환 후처리 + grab 3분기 + wall/ceiling/landMotionPost
  전수 확인 ✓
- [대응] 원본 ↔ 1.21.1 (ClientState IMPL-01 + pre-compute + fromSwimmingOrDiving)
  side-by-side 완료 ✓
- [분기] mustCrawl AABB / 5-AND canCrawl / 매 틱 공식 / 전환 후처리 2방향 / grab
  3분기 / handleClimbing 분기 / landMotionPost 3분기 전부 식별 ✓
- [상수] `SwimCrawlWaterTopBorder` / `SwimCrawlWaterBottomBorder` /
  `SwimCrawlWaterMediumBorder` / `_fallingDistanceMinimum` / `_crawlOverEdge` /
  `crawlStandUpCeiling 1.1D 오프셋` / `-0.05 ground 제한` 기록 ✓
- [타이밍] 갱신 순서 (mustCrawl → inputContinueCrawl → contextContinueCrawl 해제 →
  wouldWantCrawl → wantCrawl → canCrawl → wasCrawling 저장 → isCrawling 메인 공식 →
  contextContinueCrawl !isCrawling 해제 → capabilities.flying 해제 점프 → 전환 후처리 →
  grab 3분기) 기록 ✓
- [근사] mustCrawl AABB 는 §7 에 기록된 근사. B-42 에서 개선 시도 (별도 포커스 후보)
- [신규] §10 B-31~B-42 원자 12개 추가 ✓
- [회귀] 코드 변경 없음 (리서치/문서만)
- [빌드] 해당 없음

**다음 작업**: A-6 — 이력 3개 `wasCrawling_st` / `wasSneaking` / `wasClimbCrawling`
전수 감사. R-09 토글 블록 + 이전 틱 스냅샷 저장 위치 확인.

### 세션 36 — 2026-04-24 — A-6 (이력 3개) 완료 — **A 단계 감사 완료**

**진행한 작업**:
- 원본 `.tmp_research/SmartMovingSelf.java` 에서 wasCrawling / wasSneaking /
  wasClimbCrawling + wasRunning / wasLevitating / wasHeadJumping / wasGroundSprinting
  저장 위치 전수 grep
- R-09 블록 전체 (원본 L2968-L3045) 와 1.21.1 ClientState L769-L840 전수 1:1 대조
- `SmartMovingSelf.md` **R-15 섹션 신설** (7 서브섹션):
  * R-15.1 필드 선언 (이식 완료 3건 + 미이식 3건 확인)
  * R-15.2 저장 위치 맵 (원본 7곳 + 1.21.1 3곳)
  * R-15.3 저장 시점 동치성 분석 (결과적 동치 증명)
  * R-15.4 R-09 블록 종료부 누락 (wasRunning/wasLevitating)
  * R-15.5 R-09 블록 세부 조건 18행 대조표
  * R-15.6 **불일치 4건**
  * R-15.7 B-43~B-44 원자 예비안 (기존 원자 흡수)
- **핵심 발견**:
  * 3개 이력 필드 (wasSneaking/wasCrawling_st/wasClimbCrawling) 자체는 **이식 완료**
  * R-09 블록 거의 1:1 이식 (세션 28 작업) — 남은 불일치는 L788 간소 매핑 (B-4 범위)
    + L3043-L3044 종료부 저장 누락
  * 저장 시점은 결과적 동치지만 B-2/B-18/B-33 수정 시 원본처럼 공식 직전으로 이동 권장
  * A-6 에서 **새로 발견한 독립 원자는 B-43 (종료부 저장) 1건**. 나머지 B-44 는 기존
    B-2/B-18/B-33 수정에 편승하는 보조 작업
- §10 B-43 + B-44a~c 4 원자 신설 (대부분 기존 B-N 의존)
- §1 상태 갱신 — **A 단계 감사 완료**, 다음 A-7 매핑 테이블 통합

**완료 전 검증 체크리스트 (A-6 기준)**:
- [근거] 원본 이력 필드 7개 저장 위치 전수 grep ✓
- [근거] R-09 블록 원본 L2968-L3045 vs 1.21.1 L769-L840 전체 18행 대조 ✓
- [대응] 3개 이력 필드 이식 완료 확인 + 미이식 4건 확인 ✓
- [분기] willStopCrawl / willStopSneak / willStartSneak / willStartCrawl / sneakToggled /
  crawlToggled 갱신 분기 전수 식별 ✓
- [상수] isSneakToggleEnabled / isCrawlToggleEnabled 표면 매핑 확인 ✓
- [타이밍] 저장 시점 (원본 공식 직전 vs 1.21.1 초반 일괄) 동치성 증명 + B-N 수정 시
  공식 직전 이동 권고 기록 ✓
- [근사] 없음 (이식된 3개 필드는 근사 없음)
- [신규] B-43 (R-09 종료부) + B-44a~c (저장 시점) 4 원자 추가 ✓
- [회귀] 코드 변경 없음 (리서치/문서만)
- [빌드] 해당 없음

**A 단계 감사 완료 총괄**:
- 세션 31-36 (6 세션) 동안 A-0 ~ A-6 + B-0 완료
- R-10 ~ R-15 리서치 섹션 6개 추가 (의존 체인 완전 덤프)
- **불일치 누적 70건**:
  * A-1 isSlow/isFast: 3건 (원자 B-1a~f/B-2/B-3a~b/B-4/B-5)
  * A-2 수중 3상태: 11건 (B-6~B-13)
  * A-3 등반 4상태: 14건 (B-14~B-21)
  * A-4 전환 쌍: 13건 (B-22~B-30)
  * A-5 isCrawling: 15건 (B-31~B-42)
  * A-6 이력 3개: 4건 (B-43~B-44)
  * 세션 30 간소 매핑: 2건 (B-4/B-5)
  * 세션 31 B-0 후속: 3건 (순환 의존/미이식 2필드/전환 후처리)
- **B 단계 원자 누적**: 약 50개 (서브원자 포함 약 70+)
- **미이식 필드 총합**: isGroundSprinting 관련 4 can* + 6 Sprint 변종 + standing +
  disabled + wantSprint + _sprintEnableStanding (A-1/B-0) / isShallowDiveOrSwim +
  isJumpingOutOfWater + isStillSwimmingJump + isLevitating + isLiquidClimbing (A-2) /
  isNeighborClimbing + hasClimbGap + hasNeighborClimbGap + hasNeighborClimbCrawlGap +
  isVineOnlyClimbing + isVineAnyClimbing + isClimbingStill + handsEdgeBlock +
  feetEdgeBlock (A-3) / wasHeadJumping + wasRunning + isRunning(필드) + isStanding +
  wasLevitating (A-4/A-6) / wasCrawling(tickEssential전용) + wantCrawlNotClimb +
  initializeCrawling (A-5)
  = **30개+ 미이식 필드**

**다음 작업**: A-7 — 13 상태 필드 1.21.1 매핑 테이블 통합. §6 갱신 — 원본 심볼 ↔
1.21.1 심볼 전체 정리 + B-N 원자 우선순위 최종 확정.

### 세션 37 — 2026-04-24 — A-7 (매핑 테이블 통합) 완료 — **A 단계 100% 완료**

**진행한 작업**:
- §6 1:1 매핑 테이블을 기초 4행 → **10 서브섹션 전체 통합 완성**으로 교체:
  * §6.1 13 핵심 상태 필드 (4 ✓ / 7 ⚠️ / 2 ✗)
  * §6.2 이력/이전 틱 스냅샷 필드 (wasSneaking/wasCrawling_st/wasClimbCrawling +
    미이식 4건)
  * §6.3 A-1 isFast/isSlow 체인 (B-0 덤프 내용) — 24 의존 필드
  * §6.4 A-2 수중 3상태 체인 — 12 의존 필드
  * §6.5 A-3 등반 체인 — 19 의존 필드
  * §6.6 A-4/A-5 체인 — 15 의존 필드
  * §6.7 Config/Options 필드 매핑 — 40+ 항목
  * §6.8 Button ↔ KeyBinding 매핑 — 15 항목 (B-1b 참조)
  * §6.9 메서드/클래스 매핑 — 25+ 항목 (AABB / Orientation / Config 헬퍼 / handle* 등)
  * §6.10 이식 규모 총괄
- R-10 ~ R-15 리서치에서 수집한 모든 매핑 정보를 §6 에 통합 — B 단계에서 단일
  참조점으로 사용 가능
- §10 A-7 [x] / §1 "A 단계 100% 완료, B 단계 Phase 1 진입 대기"

**완료 전 검증 체크리스트 (A-7 기준)**:
- [근거] R-10~R-15 전수 참조 ✓
- [대응] 13 상태 + 의존 필드 + Config + Button + 메서드 전수 매핑 ✓
- [분기] 이식 상태 3분류 (✓/⚠️/✗) + 근사 🔄 표기 ✓
- [상수] Config 상수 / Button 상수 전체 포함 ✓
- [신규] 없음 (통합 작업)
- [회귀] 문서 변경만
- [빌드] 해당 없음

**A 단계 전체 감사 완료 (세션 31-37, 7 세션)**:
- R-10 ~ R-15 리서치 섹션 6개 추가
- §6 통합 매핑 테이블 10 서브섹션 완성
- **불일치 누적 70건** (A-1:3 / A-2:11 / A-3:14 / A-4:13 / A-5:15 / A-6:4 / 세션 30 간소:2 / 세션 31 B-0 후속:3 / 세션 32-36 추가:5)
- **B 단계 원자 누적 약 50개** (서브원자 포함 70+)
- **미이식 필드 30+**
- §10 Phase 1~5 실행 순서 확정 (세션 36 기록)

**다음 세션 — B 단계 진입**: Phase 1 (필드 선언 일괄). 권고 시작 원자:
- **B-22a** `wasHeadJumping` (가장 단순 — boolean 필드 1개)
- 또는 **B-2 Config.isSneakingEnabled() 헬퍼** (기존 cfg.sneak && cfg.enabled inline 헬퍼化)
- 또는 **B-1a Config._sprintEnableStanding** (Config 필드 신설)

한 세션에 Phase 1 의 2~3 원자 묶어서 진행 가능 — 의존 없는 독립 필드들 (B-22a~d /
B-10a~d / B-15a~f 등) 은 병렬 가능.

### 세션 38 — 2026-04-24 — B Phase 1 일부 — 독립 필드 8건 이식

**진행한 작업**:
- `SmartMovingClientState.java` 에 미이식 필드 8개 일괄 추가 + resetState 리셋 8개:
  * L54-L59  `wasHeadJumping` (B-22a) — isHeadJumping 매 틱 재평가 이전 틱 저장
  * L118-L126 `isStanding` (B-22d) — horizontalSpeedSquare < 0.0005 결과
  * L128-L133 `wasRunning` (B-22b) — R-09 종료부 저장, sliding 직접 진입 조건
  * L236-L253 `isShallowDiveOrSwim` (B-10a) — couldStandUp && (isDiving || isSwimming)
  * L255-L262 `isJumpingOutOfWater` (B-10b) — 수면 탈출 점프 진행 조건
  * L264-L270 `isStillSwimmingJump` (B-10c) — 수영 점프 hold 상태
  * L288-L295 `wantCrawlNotClimb` (B-31b) — wouldWantClimb 조건에 사용
  * L297-L304 `initializeCrawling` (B-31c) — 크롤 초기화 플래그
- `isLevitating` 은 **L179 에 이미 존재** 확인 → B-10d 는 갱신 로직만 (B-9 범위로 이전)
- resetState (L896-L935) 에 8개 false 리셋 추가
- 각 필드에 원본 위치 + 의존 원자 + 용도 상세 주석
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 38 기준)**:
- [근거] §6 매핑 테이블 (A-7 세션 37) + R-10~R-15 참조 ✓
- [근거] 원본 SmartMovingSelf 필드 선언 위치 전수 grep ✓
- [대응] 8 필드 원본 선언 ↔ 1.21.1 이식 위치 1:1 주석 ✓
- [분기] 각 필드 사용처 (tickEssential / R-09 / isSliding 직접 진입 / handleSwimming /
  landMotionPost) 주석 기록 ✓
- [상수] 기본값 false — 원본 public 필드 선언과 동일 ✓
- [타이밍] 필드 선언만 — 갱신 공식은 후속 B-N 에서 ✓
- [근사] 없음 (순수 필드 선언)
- [신규] B-10d (isLevitating) 는 필드 기존 존재 확인 → 상태 [~] 로 부분 완료 처리
- [회귀] compileJava 성공 — 기존 코드 영향 없음 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**다음 작업 — Phase 1 잔여**:
- **B-22c** `isRunning` 필드 승격 (로컬 → 필드) + `isRunning()` override (handleExhaustion
  수정 포함 — 별도 세션)
- **B-31a** `wasCrawling` 필드 추가 (wasCrawling_st 와 용도 구분 검토)
- **B-1a** `Config._sprintEnableStanding` (`SmartMovingConfig` 수정)
- **B-2** 헬퍼 `Config.isSneakingEnabled()` 메서드 신설
- **B-3a** 헬퍼 `Config.isSprintingEnabled()` 메서드 신설
- **B-8** 헬퍼 `Config.isSwimmingEnabled() / isDivingEnabled()` 메서드 신설
- **B-15a~f** 등반 9 필드 (isNeighborClimbing / hasClimbGap / hasNeighborClimbGap /
  hasNeighborClimbCrawlGap / isVineOnlyClimbing / isVineAnyClimbing / isClimbingStill /
  handsEdgeBlock / feetEdgeBlock)

### 세션 39 — 2026-04-24 — B Phase 1 Config 헬퍼 4종 이식 (B-2/B-3a/B-8)

**진행한 작업**:
- 원본 `SmartMovingClientConfig.java` 헬퍼 메서드 공식 확인 (리서치 L69-L100):
  * `isSneakingEnabled() = _sneak.value || !enabled` — **OR 패턴** (vanilla 스닉 허용)
  * `isSprintingEnabled() = _sprint.value && enabled` — AND 패턴
  * `isSwimmingEnabled() = _swim.value && enabled` — AND 패턴
  * `isDivingEnabled() = _dive.value && enabled` — AND 패턴
- `SmartMovingConfig.java` L456-L502 에 4개 헬퍼 메서드 추가 (원본 L69/L87/L88/L95 대응):
  * `isSneakingEnabled()` → `sneak || !enabled` ← **OR 패턴**
  * `isSprintingEnabled()` → `sprint && enabled`
  * `isSwimmingEnabled()` → `swim && enabled`
  * `isDivingEnabled()` → `dive && enabled`
- 각 메서드에 원본 파일/라인 + `enabled` 패턴 주석 (리서치 L428 인용) + 사용처 + 의존
  B-N 원자 상세 주석
- **🚨 신규 발견 (§16 세션 39)**: 기존 §6.7 매핑 테이블 및 §10 B-2 설명에서
  `isSneakingEnabled` 를 `cfg.sneak && cfg.enabled` (AND 패턴) 으로 잘못 매핑했던
  오류 발견 및 수정. 원본은 OR 패턴 — "SM 비활성 시 vanilla 스닉 허용" 용도.
  매핑 테이블 §6.7 정정 완료.
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 39 기준)**:
- [근거] 원본 `SmartMovingClientConfig.java` L69/L87/L88/L95 리서치 확보 ✓
- [근거] 리서치 L428 `enabled` 패턴 주석 (|| !enabled vs && enabled) 참조 ✓
- [대응] 4 헬퍼 메서드 원본 공식 1:1 ✓
- [분기] OR/AND 패턴 차이 구분 (isSneaking 유일 OR) ✓
- [상수] 없음 (메서드 본문만)
- [타이밍] 헬퍼 메서드 — 호출 시점 영향 없음 ✓
- [근사] 없음 (1:1)
- [신규] 기존 매핑 오류 1건 발견 + §6.7 정정 ✓
- [회귀] compileJava 성공 — 기존 호출처 없음 (신규 메서드) ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**B Phase 1 진행 상황**:
- ✅ 독립 필드 8건 (B-22a/b/d + B-10a/b/c + B-31b/c) — 세션 38
- ✅ Config 헬퍼 4종 (B-2 + B-3a 일부 + B-8) — 세션 39
- ⏳ B-1a Config 필드 (`_sprintEnableStanding`)
- ⏳ B-15a~f 등반 9 필드
- ⏳ B-22c `isRunning` 필드 승격 (handleExhaustion 수정 포함)
- ⏳ B-31a `wasCrawling` 필드

**다음 작업 권고**:
- **B-1a** `Config._sprintEnableStanding` (`SmartMovingConfig.java` 추가) — 단순
- **B-15a~f** 등반 9 필드 (ClientState 추가 + resetState 리셋) — 세션 38 패턴 재사용

### 세션 40 — 2026-04-24 — B Phase 1 Config 필드 B-1a + 등반 9 필드 B-15a~f

**진행한 작업**:
- `SmartMovingConfig.java` 에 `sprintEnableStanding = false` 필드 추가
  (원본 L313 `Unmodified("move.sprint.enable.ground")`) — 주석에 원본 라인 + 기본값
  + 사용처 (isFast 공식 L2689) + 의존 B-1a 기록
- `SmartMovingClientState.java` 에 등반 9 필드 추가 (원본 L1421-L1446 매핑):
  * `isVineOnlyClimbing` (L1421) / `isVineAnyClimbing` (L1422) (B-15d)
  * `isClimbingStill` (L1424) (B-15e)
  * `isNeighborClimbing` (L1426) — **isCrawlClimbing 공식 필수 (B-17)** (B-15a)
  * `hasClimbGap` (L1427) — **isClimbCrawling 공식 필수 (B-18)** (B-15b)
  * `hasNeighborClimbGap` (L1429) / `hasNeighborClimbCrawlGap` (L1430) (B-15c)
  * `handsEdgeBlock` / `feetEdgeBlock` (L1443-L1446) — **BlockState** 타입 (B-15f)
- **Block+meta → BlockState 흡수**: 원본 `Block handsEdgeBlock; int handsEdgeMeta`
  2필드 → 1.21.1 `BlockState handsEdgeBlock` 1필드. vanilla API 표면 매핑 (metadata
  정보 BlockState 가 내포) — 1:1 원칙 하 표면 매핑 허용 범위.
- resetState 에 9 필드 리셋 추가 (boolean false / BlockState null)
- 각 필드에 원본 라인 + 사용처 + 의존 B-N 원자 상세 주석
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 40 기준)**:
- [근거] 원본 SmartMovingSelf L1421-L1446 필드 선언 직접 read ✓
- [근거] `SmartMovingConfig.md` L313 원본 `_sprintEnableStanding` 확보 ✓
- [대응] 원본 9 필드 + Config 1 필드 1:1 이식 (Block+meta → BlockState 표면 매핑) ✓
- [분기] 필드 사용처 (isCrawlClimbing 공식 / isClimbCrawling 공식 / 애니메이션 파라미터)
  주석 기록 ✓
- [상수] `sprintEnableStanding = false` 원본 Unmodified 기본값 동일 ✓
- [타이밍] 필드 선언만 — 갱신 공식은 후속 B-N 에서 ✓
- [근사] Block+meta → BlockState 는 vanilla API 표면 매핑 (근사 아님) ✓
- [신규] 없음 (계획된 원자 수행)
- [회귀] compileJava 성공 — 기존 코드 영향 없음 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**B Phase 1 진행 상황 (세션 40 기준)**:
- ✅ 독립 필드 8건 — 세션 38
- ✅ Config 헬퍼 4종 — 세션 39
- ✅ **Config 필드 1건 + 등반 9 필드 (B-1a + B-15a~f) — 세션 40**
- ⏳ B-22c `isRunning` 필드 승격 (handleExhaustion 수정 포함 — 별도 세션)
- ⏳ B-31a `wasCrawling` 필드 (wasCrawling_st 와 구분 — 용도 정리 검토)

**다음 작업 권고**: B-22c 또는 B-31a — 둘 다 기존 로컬 변수/필드와 용도 중첩이 있어
신중한 접근 필요. B-31a 는 wasCrawling_st 를 공용으로 쓸지 별도 필드 신설할지 판단
필요. B-22c 는 handleExhaustion 의 로컬 `isRunning` (L995) 을 필드로 승격하고 기존
호출 정합성 검증 필요.

### 세션 41 — 2026-04-24 — B Phase 1 B-31a (wasCrawling 개명)

**진행한 작업**:
- 결정: `wasCrawling_st` → `wasCrawling` **개명** 으로 원본 이름과 일치 (옵션 B).
  별도 필드 신설 (옵션 A) 대신 — 원본이 단일 필드로 다용도 사용하므로 1:1 에 부합.
- `SmartMovingClientState.java` 5곳 일괄 개명 (`Edit replace_all` 사용):
  * L383 필드 선언 → 원본 L3074 `public boolean wasCrawling` 매핑 주석 확장
  * L700 저장: `wasCrawling_st = isCrawling` → `wasCrawling = isCrawling`
  * L910 주석
  * L959 R-09 조건: `isCrawling && !wasCrawling`
  * L1045 resetState 리셋
- 필드 주석에 원본 5가지 사용 용도 상세 기록:
  * (1) tickEssential 저장 ✓ 이식
  * (2) R-09 willStartCrawl 판정 ✓ 이식
  * (3) L2449 capabilities.flying 해제 점프 — B-34
  * (4) L2457 wantCrawlNotClimb 계산 — B-41
  * (5) L2566/L2572/L2751/L2760/L2767/L2812/L2835/L2860 여러 전환 블록 재설정 —
    B-33/B-35/B-36 수정 시 등록
- Swimmer L116 로컬 변수 `wasCrawling` 과 이름 충돌 확인 — **별도 스코프라 무관** (Swimmer
  메서드 내 지역 변수 vs ClientState 필드)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 41 기준)**:
- [근거] 원본 SmartMovingSelf L3074 `public boolean wasCrawling` + 사용처 grep ✓
- [근거] 기존 `wasCrawling_st` 5곳 참조 전수 확인 + 원본 대응 위치 매핑 ✓
- [대응] 개명 후 원본 필드 이름과 1:1 일치 ✓
- [분기] 저장 (L700) + R-09 판정 (L959) + resetState (L1045) 3곳 일관 갱신 ✓
- [상수] 없음 (이름만 변경)
- [타이밍] 개명은 의미 변경 없음 — 타이밍 동치 ✓
- [근사] 없음 (순수 이름 변경)
- [신규] 없음 (계획된 작업)
- [회귀] compileJava 성공 — Swimmer 로컬 변수와 스코프 충돌 없음 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**B Phase 1 진행 상황 (세션 41 기준)**:
- ✅ 독립 필드 8건 — 세션 38
- ✅ Config 헬퍼 4종 — 세션 39
- ✅ Config 필드 1건 + 등반 9 필드 — 세션 40
- ✅ **wasCrawling 개명 (B-31a) — 세션 41**
- ⏳ **B-22c `isRunning` 필드 승격** (handleExhaustion 수정 포함)

**다음 작업**: B-22c (Phase 1 마지막 원자). 원본 `isRunning` 은 **메서드 override**
(L3241 `public boolean isRunning() { return isSprinting() && !isFast && (onGround || vanilla()); }`)
+ **필드** 조합. 1.21.1 에서는 필드 신설 + 메서드 추가로 처리. 기존 handleExhaustion
L995 로컬 변수 제거 + 필드 참조로 변경. tickEssential 에서 필드 갱신 위치 결정 필요.

### 세션 42 — 2026-04-24 — B Phase 1 마지막 B-22c — **Phase 1 100% 완료**

**진행한 작업**:
- 원본 `SmartMovingSelf.java` `isRunning()` 재확인 (L3239-L3242) — **필드가 아니라
  메서드 override** 였음. 기존 B-22c 계획 "필드 승격" 은 잘못된 전제. 수정:
  * 원본 `public boolean isRunning() { return sp.isSprinting() && !isFast && (sp.onGround || vanilla()); }`
  * 파생 메서드 — 필드 저장 X. 매 호출마다 재계산
  * 사용처: L3043 `wasRunning = isRunning` (메서드 결과를 필드에 저장) + L1202/L1278
    Config.getFactor 파라미터
- 원본 `vanilla()` 헬퍼 (L3327-L3330) 이식: `return !Config.enabled || Config._vanillaStyle.value`
- `SmartMovingConfig.vanillaStyle` 필드 L51 에 이미 이식됨 — 그대로 사용
- `SmartMovingClientState.java` 수정:
  * L1112-L1131 (canStandUp 뒤) 에 **vanilla() 헬퍼 + isRunning(ClientPlayerEntity) 메서드
    2개 추가**. 각 원본 위치 (L3327-L3330 / L3239-L3242) 주석 + 사용처 기록
  * handleExhaustion L1163 기존 로컬 변수 `isSprinting() && !isFast && onGround` →
    `isRunning(player)` 메서드 호출로 교체
  * 기존 **부정확한 주석 제거** (원본 `vanilla() = !Config.enabled || Config._vanillaStyle.value`
    이므로 `cfg.vanillaStyle=true` + `cfg.enabled=true` 경로에서도 vanilla() 가 true.
    기존 "handleExhaustion 은 cfg.enabled 때만 호출되므로 vanilla() 는 false" 주석은 부정확)
- `./gradlew compileJava --rerun-tasks` 성공 (로컬 변수 `boolean isRunning = isRunning(player)`
  는 RHS 먼저 평가되므로 Java 해석 정상)

**완료 전 검증 체크리스트 (세션 42 기준)**:
- [근거] 원본 `isRunning()` / `vanilla()` 메서드 정의 직접 read ✓
- [근거] `vanillaStyle` Config 이식 확인 (L51) ✓
- [대응] 원본 2 메서드 1:1 이식 (`cfg.enabled` 가드 + `cfg.vanillaStyle` 조합) ✓
- [분기] `isSprinting() && !isFast && (onGround || vanilla())` 3-AND 공식 보존 ✓
- [상수] 없음 (메서드 본문)
- [타이밍] 메서드는 매 호출 재계산 — 원본 타이밍과 동일 ✓
- [근사] 없음 (1:1)
- [신규] 기존 부정확 주석 정정 ✓
- [회귀] compileJava 성공 — handleExhaustion 동작 정상 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

---

## 🎉 B Phase 1 (필드 선언 일괄) 100% 완료

**누적 성과 (세션 38-42, 5 세션)**:
- **ClientState 필드 추가**: 17 boolean + 2 BlockState = **19 필드**
  * 이력 스냅샷 (3): wasHeadJumping / wasRunning / (wasCrawling 개명)
  * 상태 (4): isStanding
  * 수중 (4): isShallowDiveOrSwim / isJumpingOutOfWater / isStillSwimmingJump (+ isLevitating 이미 있음)
  * 크롤 (2): wantCrawlNotClimb / initializeCrawling
  * 등반 (9): isVineOnlyClimbing / isVineAnyClimbing / isClimbingStill /
    isNeighborClimbing / hasClimbGap / hasNeighborClimbGap /
    hasNeighborClimbCrawlGap / handsEdgeBlock / feetEdgeBlock
- **Config 추가**:
  * 필드 1: sprintEnableStanding
  * 헬퍼 메서드 4: isSneakingEnabled / isSprintingEnabled / isSwimmingEnabled / isDivingEnabled
- **ClientState 메서드 추가 2**: vanilla() / isRunning(player)
- **개명**: wasCrawling_st → wasCrawling (원본과 일치)

**B Phase 2 (공식 이식) 진입 준비 완료**.

**Phase 2 권고 우선순위** (세션 36 §15 기록):
- B-1c (can* 4 판정) / B-1d (6 Sprint 변종) / B-1e (standing) / B-1f (isFast 6-OR)
- B-3a (wantSprint 공식) / B-3b (wouldIsSneaking 정정)
- B-16 (isClimbHolding/wantClimbHolding 3-OR)
- B-17 (isCrawlClimbing 공식) / B-18 (isClimbCrawling + climbIntoCount)
- B-23 (isHeadJumping 매 틱 재평가)
- B-30 (isStanding 공식)
- B-32 (canCrawl 5-AND 복원)
- B-33 (isCrawling 메인 공식 재작성)
- B-40 (toCrawling 헬퍼)
- B-2 (isSlow 정정) — 간단한 2줄 수정

**다음 권고 시작**: **B-2 (isSlow 정정)** — 가장 단순한 2줄 교체로 첫 Phase 2 원자로 적합.
또는 **B-30 (isStanding 공식 이식)** — isStanding 필드 이미 이식됨 + 공식 1줄.

### 세션 43 — 2026-04-24 — B Phase 2 착수: B-2 + B-44a + B-30

**진행한 작업**:
- **B-2 `isSlow` 정정** (`SmartMovingClientState.tickEssential`):
  * `wantSneak = cfg0.isSneakingEnabled() && wouldWantSneak` 신설 (원본 L2588-L2590)
  * `isSlow = wantSneak && wouldIsSneaking` (원본 L2718) — 기존 `sneakContinueInput`
    중복 곱 제거
  * 주석 원본 L2576-L2590/L2711-L2719 1:1 매핑 기록
- **B-44a wasSneaking 저장 시점 이동**:
  * 기존 L709 tickEssential 초반 일괄 저장에서 **isSlow 공식 직전** (원본 L2716) 으로
    이동. wasCrawling / wasClimbCrawling 는 아직 기존 위치 유지 (B-33/B-18 수정 시 함께)
- **B-30 `isStanding` 공식 이식**:
  * R-09 블록 진입 직전 (isSmall 뒤) 에 원본 L2734 공식 추가:
    `isStanding = (motionX² + motionZ²) < 0.0005`
  * 블록 스코프로 감싸 지역 변수 이름 충돌 방지 (`_motionX`/`_motionZ`/`_horizontalSpeedSquare`)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 43 기준)**:
- [근거] 원본 L2588-L2590 / L2716 / L2718 / L2734 직접 read ✓
- [근거] `isSneakingEnabled()` 헬퍼 (B-2 선행) 이식 완료 확인 ✓
- [대응] B-2 원본 공식 1:1 (wantSneak = isSneakingEnabled && wouldWantSneak /
  isSlow = wantSneak && wouldIsSneaking) ✓
- [분기] sneakContinueInput 중복 제거 + wantSneak 신설 + 저장 시점 이동 3건 일관 ✓
- [상수] 0.0005 isStanding 임계값 원본 유지 ✓
- [타이밍] B-44a 저장 시점이 공식 직전으로 이동 — 결과는 동일 (L709 위치에서도 이전 틱
  값 유지되지만, 원본 구조와 엄격 일치 확보) ✓
- [근사] 없음 (원본 완전 일치)
- [신규] 없음
- [회귀] compileJava 성공 — wouldIsSneaking 은 여전히 `!player.isSprinting()` (B-3b 에서 정정)
  이라 B-2 만으로 완전 해결 아님. 점진 개선 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황**:
- ✅ B-2 (isSlow 공식 정정)
- ✅ B-44a (wasSneaking 저장 시점)
- ✅ B-30 (isStanding 공식)
- ⏳ B-1c ~ B-1f (can* / 6 Sprint 변종 / standing / isFast)
- ⏳ B-3a ~ B-3b (wantSprint / wouldIsSneaking 정정)
- ⏳ B-16 (isClimbHolding + wantClimbHolding)
- ⏳ B-17 / B-18 (isCrawlClimbing / isClimbCrawling 공식)
- ⏳ B-23 (isHeadJumping 매 틱 재평가)
- ⏳ B-32 / B-33 / B-40 (canCrawl / isCrawling 공식 / toCrawling 헬퍼)
- 등 40+ 원자

**다음 작업 권고**: **B-3b (wouldIsSneaking 정정)** — B-3a (wantSprint) 선행이 완벽하진
않지만 `!player.isSprinting()` → `!wantSprint` 로 바꾸려면 wantSprint 필드가 먼저 있어야
함. 또는 **B-40 (toCrawling 헬퍼)** — 단독 가능 (기존 inline 3줄을 헬퍼로 추출).
또는 **B-32 (canCrawl 5-AND 복원)** — 단순 조건 축소 (9-AND → 5-AND).

### 세션 44 — 2026-04-24 — B Phase 2 계속: B-32 + B-40 + Config 필드 1

**진행한 작업**:
- 원본 `Options.isCrawlToggleEnabled()` / `isSneakToggleEnabled()` 공식 확인
  (`SmartMovingOptions.md` L449-L468) — 둘 다 **AND 패턴** (`_toggle.value && enabled`)
- 원본 `Config._fallingDistanceMinimum` 기본값 3F 확인 (`SmartMovingConfig.md` L348)
- `SmartMovingConfig` 에 `fallingDistanceMinimum = 3F` 필드 추가 — B-32 전제
- **B-32 canCrawl 5-AND 복원** (`ClientState.tickEssential` IMPL-01):
  * 기존 9-AND 잉여 5조건 (`!isCrawlClimbing && !isCeilingClimbing && !isSliding &&
    !isHeadJumping && !isFlying`) 제거 — 원본에 없는 잉여 조건 (1:1 원칙)
  * `player.fallDistance < cfg.fallingDistanceMinimum` 추가 (원본 L2439)
  * 결과: 원본 5-AND 공식 그대로 — `!swim && !dive && (!dipping || border) && !climbing
    && fallDistance < minimum`
  * 기존 "1.21.1 근사" 주석 제거
- **B-40 toCrawling() 헬퍼 신설**:
  * `ClientState` 에 public `toCrawling()` 메서드 추가 (원본 L3047-L3054)
  * 공식: `isCrawling = true; if (cfg.crawlToggle && cfg.enabled) crawlToggled = true;
    ignoreNextStopSneakButtonPressed = true; return true;`
  * IMPL-01 진입 시 inline 3줄 → `toCrawling()` 호출 교체
  * **cfg.enabled 가드 포함** — 기존 inline 에서 누락된 부분 (원본 `Options.isCrawlToggleEnabled()
    = _crawlToggle.value && enabled` AND 패턴 준수)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 44 기준)**:
- [근거] 원본 L2434-L2439 canCrawl + L3047-L3054 toCrawling + L348 _fallingDistanceMinimum
  직접 read ✓
- [근거] 원본 `Options.isCrawlToggleEnabled()` 공식 (AND 패턴) 확보 ✓
- [대응] B-32: 9-AND → 5-AND 원본 1:1 ✓
- [대응] B-40: toCrawling 본체 + 호출 전환 + cfg.enabled 가드 추가 ✓
- [분기] canCrawl 5-AND (`!swim`/`!dive`/`!dipping||border`/`!climbing`/`fallDistance`) ✓
- [상수] `0.65F` SwimCrawlWaterTopBorder / `3F` fallingDistanceMinimum 원본 값 ✓
- [타이밍] canCrawl 는 여전히 !isCrawling 진입 분기에서만 평가 — 메인 공식 구조는 B-33 에서 처리 ✓
- [근사] 기존 "1.21.1 근사" 주석 제거 — 실제 근사 아닌 잉여였음 ✓
- [신규] §16 신규 발견: `isCrawlToggleEnabled()` 호출 지점 중 inline `cfg.crawlToggle` 만
  있고 `cfg.enabled` 체크 누락한 곳이 여럿 (B-40 에서 일부 해소, 나머지는 별도 원자 필요)
- [회귀] compileJava 성공 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 44 기준)**:
- ✅ B-2 / B-44a / B-30 — 세션 43
- ✅ B-32 / B-40 / Config.fallingDistanceMinimum — 세션 44
- ⏳ B-1c~f / B-3a~b / B-16 / B-17 / B-18 / B-23 / B-33 등 ~35+ 원자

**다음 작업 권고**:
- **B-1a 후속: Config.isRunningEnabled() / isJumpingEnabled() / isFlyingEnabled() /
  isCrawlingEnabled() / isClimbExhaustionEnabled 등 Config 헬퍼** — 원본 공식 맞춰 일괄 이식.
  1:1 원칙 상 각 헬퍼별 OR/AND 패턴 확인 필수.
- **B-3b (wouldIsSneaking 정정)** — `!player.isSprinting()` → `!wantSprint`. 다만
  `wantSprint` 필드 + 계산 블록 (B-3a) 선행 필요.
- **B-17 / B-18 (isCrawlClimbing / isClimbCrawling 공식 이식)** — B-15 의존 필드 완성됨 →
  규모 큰 공식 이식 가능.

### 세션 45 — 2026-04-24 — B Phase 2 계속: B-45 (Config 토글 헬퍼 2종 + 호출 정리)

**진행한 작업**:
- `SmartMovingConfig` 에 `isSneakToggleEnabled()` / `isCrawlToggleEnabled()` 2 헬퍼 추가
  (원본 `SmartMovingOptions.md` L449-L468 — AND 패턴: `_toggle && enabled`)
- 원본 그대로 주석에 파일/라인 + 사용처 + B-N 의존 기록
- **ClientState 6곳 + Jumper 1곳 전수 헬퍼 치환**:
  * ClientState L738 `isCrawlToggleEnabled0 = ...` → `cfg0.isCrawlToggleEnabled()` 직접 사용
  * ClientState L788 `sneakContinueInput` 조건 — **cfg.enabled 가드 추가** (기존 누락)
  * ClientState L943-L944 R-09 블록 isSneak/CrawlToggleEnabled — 헬퍼로 통일
  * ClientState L959 `wantSneak_` (R-09 블록 간소 매핑) — **cfg.enabled 가드 추가**
  * ClientState L1173 toCrawling() 내부 — 헬퍼 치환
  * Jumper L112 `sm.isCrawling=true + cfg.crawlToggle → crawlToggled=true` → **`sm.toCrawling()`
    승격** — 원본 toSlidingOrCrawling else 분기 `wasCrawling=toCrawling()` 와 일치,
    cfg.enabled 가드 + ignoreNextStopSneakButtonPressed 자동 포함
- §6.7 Config 매핑 테이블에 헬퍼 2종 추가
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 45 기준)**:
- [근거] 원본 `SmartMovingOptions.md` L449-L468 공식 확인 ✓
- [대응] 2 헬퍼 원본 AND 패턴 1:1 ✓
- [분기] 7곳 호출 전수 헬퍼 치환 — cfg.enabled 누락 3건 해소 ✓
- [상수] 없음 (메서드)
- [타이밍] 헬퍼 치환은 의미 변경 — cfg.enabled=false 시 이전과 동작 다름 (버그 수정) ✓
- [근사] 없음
- [신규] Jumper L110 inline → toCrawling() 호출 승격 — ignoreNextStopSneakButtonPressed
  자동 포함. 이 플래그 사용 여부 실제 동작에서 확인 필요 (별도 테스트 시)
- [회귀] compileJava 성공 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 45 기준)**:
- ✅ B-2 / B-44a / B-30 — 세션 43
- ✅ B-32 / B-40 / Config.fallingDistanceMinimum — 세션 44
- ✅ B-45 (Config 2 헬퍼 + 7곳 호출 정리) — 세션 45
- ⏳ 잔여 ~33 원자 (B-1c~f / B-3a~b / B-16 / B-17 / B-18 / B-23 등)

**다음 작업 권고**:
- **B-17 (isCrawlClimbing 공식 이식)** — B-15a `isNeighborClimbing` 이식 완료 → 규모 중간.
  원본 L2737 5-AND 공식 + L2737-L2754 canStandUp/wasCrawlClimbing 전환 블록 이식.
- 또는 **B-18 (isClimbCrawling 공식 + climbIntoCount 카운터)** — B-15b `hasClimbGap` +
  B-16 `isClimbHolding` 의존. B-16 선행 필요.
- 또는 **B-23 (isHeadJumping 매 틱 재평가)** — B-22a `wasHeadJumping` 이식 완료 → 가능.
  원본 L2524-L2530 5-AND 해제 공식.

### 세션 46 — 2026-04-24 — B Phase 2 B-23 (isHeadJumping 매 틱 재평가)

**진행한 작업**:
- `SmartMovingClientState.tickEssential` 에 B-23 재평가 블록 추가:
  * `wasHeadJumping = isHeadJumping;` — 이전 틱 저장 (원본 L2524)
  * 5-AND 해제 공식: `isHeadJumping && !onGround && !(swim_sm||dive) && !(flying||
    capabilities.flying) && !(isTouchingWater && velocity.y<0) && !isInLava` (원본 L2525-L2530)
  * `if (!isHeadJumping) isAerodynamic = false;` (원본 L2533) — 재평가 뒤 위치로 이동
- **순서 정리**: 원본 L2524→L2533→L2546 순서에 맞게 SlideToHeadJumping 전환 앞에 배치.
  기존 `if (!isHeadJumping) isAerodynamic = false` 는 SlideToHeadJumping 뒤 위치였음 —
  재평가 뒤로 이동하여 원본과 일치.
- **B-24 자리 확보**: 재평가 블록과 SlideToHeadJumping 사이에 주석으로 B-24 (handleCrash +
  restoreFromFlying 해제 엣지 후처리) 위치 명시 — 미이식 원자.
- 1.21.1 매핑 확인:
  * `sp.onGround` → `player.isOnGround()`
  * `isSwimming` → `isSwimming_sm` (1.21.1 접미사)
  * `sp.capabilities.isFlying` → `player.getAbilities().flying`
  * `sp.handleWaterMovement()` → `player.isTouchingWater()`
  * `sp.motionY` → `player.getVelocity().y`
  * `sp.handleLavaMovement()` → `player.isInLava()`
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 46 기준)**:
- [근거] 원본 SmartMovingSelf L2524-L2530 5-AND 공식 + L2533 isAerodynamic 직접 read ✓
- [근거] R-13 세션 34 불일치 1건 확정 — `자동 해제 경로 없음` 해결 ✓
- [대응] 원본 5-AND 1:1 이식 (순서 + 조건 + vanilla API 표면 매핑) ✓
- [분기] 5-AND 각 조건 (onGround/swim||dive/flying||capabilities/water+motionY<0/lava)
  전부 식별 + 주석 ✓
- [상수] 없음 (조건만)
- [타이밍] SlideToHeadJumping 전환 앞 위치 — 원본 L2524→L2546 순서 복원 ✓
- [근사] 없음 (1:1)
- [신규] B-24 자리 주석 확보 — 다음 원자에서 추가 예정
- [회귀] compileJava 성공 — 기존 SlideToHeadJumping 동작 보존 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 46 기준)**:
- ✅ B-2 / B-44a / B-30 — 세션 43
- ✅ B-32 / B-40 — 세션 44
- ✅ B-45 — 세션 45
- ✅ **B-23** — 세션 46
- ⏳ 잔여 ~32 원자

**다음 작업 권고**:
- **B-24 (해제 엣지 후처리)** — B-23 뒤 자리에 `wasHeadJumping && !isHeadJumping && onGround →
  handleCrash + restoreFromFlying` 이식. 다만 `_headFallDamageStartDistance/Factor` Config
  필드 이식 여부 + handleCrash 메서드 위치 확인 필요.
- **B-17 (isCrawlClimbing 공식)** — B-15a `isNeighborClimbing` 이식됨, 의존 해소. 규모 중간.
- **B-16 (isClimbHolding / wantClimbHolding)** — 후속 B-18 의 선행 조건.

### 세션 47 — 2026-04-24 — B Phase 2 B-17a (isCrawlClimbing 메인 공식)

**진행한 작업**:
- 의존 필드 확인: `isNeighborClimbing` / `wasCrawling` / `isClimbing` / `crawlToggled` 전부
  이식됨. `sneakPressed` / `moveForward` 는 MinecraftClient / player.input 에서 직접 참조.
- B-16 시도 무산: `blocked` (UI 열림 판정) / `wantClimb` (Climber 로컬 변수) 둘 다 미이식.
  B-46 (blocked 이식) + B-47 (wantClimb 필드 승격) 선행 원자 신설 필요 — 별도 세션.
- B-24 시도 무산: `_headFallDamageStartDistance/Factor` Config 미이식 +
  `handleCrash` Climber private + `restoreFromFlying` 필드 미이식 — 세션 여러 개 필요.
- **B-17a 채택**: isCrawlClimbing 메인 5-AND 공식 이식 (원본 L2737):
  ```java
  isCrawlClimbing = (wasCrawling || isCrawlClimbing)
                 && isClimbing
                 && isNeighborClimbing
                 && (sneakPressed || crawlToggled)
                 && moveForward > 0F;
  ```
  B-30 뒤, R-09 블록 앞에 배치 — 원본 순서 (wantClimbHolding → isStanding → isCrawlClimbing
  → isClimbCrawling) 준수.
- **no-op 상태 인식**: `isNeighborClimbing` 갱신 로직 (B-19) 미이식 — 값 항상 false →
  isCrawlClimbing 공식 결과도 항상 false. B-19 완료 후 자동 활성화.
- B-17b 전환 블록 (원본 L2737-L2754 canStandUp + wasCrawlClimbing 전환) 은 다음 원자로
  분리 (isPlayerInSolidBetween 근사 필요).
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 47 기준)**:
- [근거] 원본 SmartMovingSelf L2737 5-AND 공식 직접 read (R-12.5) ✓
- [근거] 의존 필드 전수 이식 확인 grep ✓
- [대응] 원본 5-AND 1:1 이식 ✓
- [분기] 5조건 (wasCrawling||isCrawlClimbing / isClimbing / isNeighborClimbing /
  sneakPressed||crawlToggled / moveForward>0) 전부 ✓
- [상수] 없음 (공식만)
- [타이밍] B-30 뒤 / R-09 앞 위치 — 원본 L2734 isStanding → L2737 isCrawlClimbing 순서 ✓
- [근사] 없음 — `sneakKey.isPressed()` 는 vanilla 표면 매핑 (원본 sneakButton.Pressed)
- [신규] no-op 상태 + B-19 의존 관계 기록 ✓
- [회귀] compileJava 성공 — isNeighborClimbing=false 때문에 기존 동작과 동일 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 47 기준)**:
- ✅ B-2 / B-44a / B-30 — 세션 43
- ✅ B-32 / B-40 — 세션 44
- ✅ B-45 — 세션 45
- ✅ B-23 — 세션 46
- ✅ **B-17a** — 세션 47
- ⏳ 잔여 ~31 원자 (B-17b / B-18 / B-24 / B-16 / B-3a~b / B-1c~f / B-33 등)

**다음 작업 권고**:
- **B-46 + B-47 (blocked + wantClimb 이식)** — B-16 선행. 단순 2건.
- **B-17b (canStandUp 전환 블록)** — B-17a 후속 — isPlayerInSolidBetween 근사 + move API.
- **B-3b (wouldIsSneaking `!wantSprint` 정정)** — wantSprint 필드 없이는 위배. B-3a 선행 필요.

### 세션 48 — 2026-04-24 — B Phase 2 B-17b1 (canStandUp 분기)

**진행한 작업**:
- 원본 `wantClimb` 은 지역 변수 (L2479 `isFreeClimbingEnabled() && wouldWantClimb`),
  필드 아님 확인. B-47 "wantClimb 필드 승격" 불필요 — 원본 구조 유지.
- `wouldWantClimb` (L2467-L2477) 공식이 `grabPressed || isClimbHolding+sneakPressed ||
  auto-ladder/vine` 등 복잡 의존 → B-16 완전 이식은 의존 체인 큼, 분리.
- 이번 세션 **B-17b1 (canStandUp 분기)** 진행:
  * `isPlayerInSolidBetween(player, y1, y2)` **정밀 헬퍼 신설** (canStandUp 근처) —
    vanilla `World.isSpaceEmpty(entity, box)` 로 원본 AABB 스캔 동치 (근사 X)
  * B-17a 블록 확장: `_wasCrawlClimbing17` 지역 변수 저장 (원본 L2736 공식 직전) +
    canStandUp 분기 (원본 L2738-L2754) 이식
  * canStandUp 성립 시 `isCrawlClimbing=false` + `!isClimbCrawling → heightOffset=0F`
  * 조건부 `wasCrawling=false, isCrawling=false` (원본 L2749-L2753)
- B-17b2 (전환 3분기 `else if(wasCrawlClimbing)`) 는 `wantClimbUp/wantClimbDown` 필드
  + `move()` API 의존 → 별도 원자
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 48 기준)**:
- [근거] 원본 L2738-L2754 canStandUp 분기 직접 read (R-12.5) ✓
- [근거] `isPlayerInSolidBetween` 의미 확인 (AABB 내 solid 블록 존재) ✓
- [대응] 정밀 헬퍼 원본 1:1 (AABB 구성 + 공간 검사) ✓
- [분기] canStandUp 성립/미성립 + isClimbCrawling 여부 + !wasCrawlClimbing 4-way 전수 ✓
- [상수] `0.95D` (isClimbCrawling) / `1D` (else) 크롤 오프셋 원본 동일 ✓
- [타이밍] 지역 저장 → 메인 공식 → canStandUp 분기 순서 원본 동일 ✓
- [근사] **정밀 구현** (vanilla World.isSpaceEmpty 사용) — 근사 아님 ✓
- [신규] 없음
- [회귀] compileJava 성공 — isCrawlClimbing no-op 상태 유지 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 48 기준)**:
- ✅ B-2 / B-44a / B-30 — 세션 43
- ✅ B-32 / B-40 — 세션 44
- ✅ B-45 — 세션 45
- ✅ B-23 — 세션 46
- ✅ B-17a — 세션 47
- ✅ **B-17b1** — 세션 48
- ⏳ 잔여 ~30 원자

**다음 작업 권고**:
- **B-17b2** — `else if(wasCrawlClimbing)` 전환 3분기. `wantClimbUp/wantClimbDown` 필드
  승격 선행. `move()` 1.21.1 API 매핑. 중간 규모.
- **B-3a (wantSprint 6조건 OR)** — 독립적. 의존 필드 대부분 이식됨. 공식 이식.
- **B-18** — `isClimbCrawling` 공식 + climbIntoCount 카운터. B-16 선행 필요.

### 세션 49 — 2026-04-24 — B Phase 2 B-3a + B-3b (wantSprint + wouldIsSneaking 정정)

**진행한 작업**:
- `SmartMovingClientState` 에 `wantSprint` public 필드 추가 (원본 L1415 — public 필드).
  resetState 에 리셋 추가.
- 원본 L2595-L2615 `wantSprint` 6조건 OR 공식 이식 — isSlow 공식 블록 내 wantSneak 계산
  직후 배치:
  * 지역 변수 `disabled` (원본 L2375) 계산:
    `!cfg.enabled || hasVehicle() || isSleeping() || getSleepTimer()>0` (startSleeping 근사)
  * 지역 변수 `sprintPressed/jumpPressed/moveForwardPressed/movePressed` 계산
    (vanilla key/input 표면 매핑)
  * 6-AND 공식: `isSprintingEnabled && !isSliding && sprintPressed && (지면전진 || 등반 ||
    수영+입력+sneakDown || 잠수+입력+jump/sneakDown || 비행+입력+jump/sneak) && !disabled`
- **B-3b**: `wouldIsSneaking` 공식 정정 — 기존 `!player.isSprinting()` (vanilla 단순)
  → `!wantSprint` (SM 복합). **A-1 불일치 #3 해소** — wantSprint 의 6조건 컨텍스트
  (수영/잠수/비행 sneakDown 허용) 이 반영됨.
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 49 기준)**:
- [근거] 원본 L2595-L2615 wantSprint + L2712 wouldIsSneaking 직접 read (R-10.5/R-10.9) ✓
- [근거] L2373-L2375 disabled + L2592-L2593 move*Pressed 지역변수 공식 확인 ✓
- [대응] 원본 6-AND + 5-OR 서브 + 4-AND (수영/잠수/비행) 전부 1:1 ✓
- [분기] 6-AND 각 항 (enabled/sliding/sprint/상황별 OR/disabled) + 5갈래 OR (지면/등반/
  수영/잠수/비행) + 각 컨텍스트 AND 조건 전수 ✓
- [상수] 없음 (조건만)
- [타이밍] wantSneak 계산 뒤 wantSprint 계산 → wouldIsSneaking → isSlow 순서 원본 일치 ✓
- [근사] `startSleeping` 1.21.1 대응 없음 — `getSleepTimer() > 0` 으로 근사 확장
  (주석 명시). 원본 의도 (수면 시작 중 SM 비활성) 반영
- [신규] 없음 (A-1 불일치 해소)
- [회귀] compileJava 성공. wouldIsSneaking 이전 틱 `!isSprinting()` 의 근사 제거됨 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**A-1 불일치 3건 해소 현황**:
- ✅ #1 isFast 6갈래 OR (미해소 — B-1f 에서 이식 예정)
- ✅ #2 isSlow Config.isSneakingEnabled() 가드 (B-2 완료)
- ✅ **#3 wouldIsSneaking !wantSprint 정정** (B-3b 완료 세션 49)

**Phase 2 진행 상황 (세션 49 기준)**:
- ✅ 세션 43: B-2 / B-44a / B-30
- ✅ 세션 44: B-32 / B-40
- ✅ 세션 45: B-45
- ✅ 세션 46: B-23
- ✅ 세션 47: B-17a
- ✅ 세션 48: B-17b1
- ✅ 세션 49: **B-3a + B-3b**
- ⏳ 잔여 ~28 원자 (B-17b2 / B-18 / B-24 / B-16 / B-1c~f / B-33 등)

**다음 작업 권고**:
- **B-1f (isFast 6갈래 OR 공식 이식)** — A-1 불일치 #1 해소 대상. 의존 필드
  `isGroundSprinting / isClimbSprinting / isSwimSprinting / isDiveSprinting /
  isCeilingSprinting / isFlyingSprinting` + `standing` 미이식 → B-1d/B-1c/B-1e 선행.
  규모 큼 (여러 세션 분할).
- **B-17b2** — `else if(wasCrawlClimbing)` 전환 3분기. wantClimbUp/Down 필드 + move() API.
- **B-18** — `isClimbCrawling` 공식 + climbIntoCount. B-16 선행.

### 세션 50 — 2026-04-24 — B Phase 2 B-1c1 (Config 피로/스프린트 필드 7 + 헬퍼 3)

**진행한 작업**:
- `SmartMovingConfig.java` 에 B-1 체인 선행 필드/헬퍼 일괄 추가:
  * `runExhaustionStart = 75F` (원본 L298)
  * `runExhaustionStop = 100F` (원본 L299, up 100F)
  * `sprintExhaustionStart = 50F` (원본 L314)
  * `sprintExhaustionStop = 100F` (원본 L315, up 100F)
  * `sprintDuringItemUsage = false` (원본 L589 Modified)
  * `runExhaustion = false` (원본 SmartMovingClientConfig L90)
  * `sprintExhaustion = false` (원본 L94)
  * 헬퍼 3: `isRunExhaustionEnabled()` / `isClimbExhaustionEnabled()` /
    `isSprintExhaustionEnabled()` (전부 AND 패턴, 원본 SmartMovingClientConfig L92-L96)
- 각 필드/헬퍼에 원본 라인 + 사용처 + B-N 의존 주석
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 50 기준)**:
- [근거] 원본 SmartMovingConfig L298/L299/L314/L315/L589 + SmartMovingClientConfig
  L90/L92/L93/L94/L96 직접 확인 ✓
- [대응] 원본 필드 1:1 이식 + 헬퍼 AND 패턴 1:1 ✓
- [분기] Modified (Boolean) vs Positive/Float 기본값 구분 — Modified 기본 false ✓
- [상수] 75F / 100F / 50F / 100F 원본 기본값 동일 ✓
- [타이밍] Config 필드만 — 타이밍 해당 없음 ✓
- [근사] 없음 (순수 필드/헬퍼)
- [신규] 없음
- [회귀] compileJava 성공 — 기존 호출처 없음 (신규 필드) ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**Phase 2 진행 상황 (세션 50 기준)**:
- ✅ B-2 / B-44a / B-30 / B-32 / B-40 / B-45 / B-23 / B-17a / B-17b1 / B-3a / B-3b — 세션 43-49
- ✅ **B-1c1** (Config 선행 7필드+3헬퍼) — 세션 50
- ⏳ B-1c2 (ClientState `collidedHorizontallyTickCount` + 갱신)
- ⏳ B-1c3 (`preferSprint` + can* 4 + `isClimbSprintSpeed` 공식 이식)
- ⏳ B-1d (6 Sprint 변종)
- ⏳ B-1e (`standing` 지역 계산)
- ⏳ B-1f (`isFast` 6갈래 OR — A-1 불일치 #1 최종 해소)
- 기타 ~22 원자

**다음 작업 권고**:
- **B-1c2 + B-1c3** 묶음 — ClientState `collidedHorizontallyTickCount` 필드 +
  `tickEssential` 내 갱신 (`horizontalCollision ? ++count : 0`) + `preferSprint` +
  can* 4 + `isClimbSprintSpeed` 공식 이식. SmartStatisticsFactory 근사 `true` 필요.
- 그 뒤 **B-1d + B-1e + B-1f** 일괄 (isFast 완성 — A-1 #1 해소).

### 세션 51 — 2026-04-24 — B Phase 2 **B-1c2~B-1f 일괄 이식 — isFast 체인 완성**

**진행한 작업**:
- `SmartMovingClientState` 필드 2개 추가:
  * `isGroundSprinting` public (원본 L1439) + resetState 리셋
  * `collidedHorizontallyTickCount` int (원본 L1432 근처 추정) + resetState 리셋
- tickEssential 에 **collidedHorizontallyTickCount 매 틱 갱신**:
  `horizontalCollision ? ++count : 0` (B-1c2)
- 기존 L885 `isFast = grab && isSprinting()` 한 줄을 **원본 L2617-L2695 블록 축소판**으로
  완전 교체 (B-1c3 + B-1d + B-1e + B-1f):
  * isSprintJump 매 틱 갱신 (원본 L2633-L2634 true + L2643-L2644 false)
  * exhaustionAllowsSprinting (원본 L2636-L2641) — Config 헬퍼 isSprintExhaustionEnabled 사용
  * preferSprint (원본 L2646-L2657) — maxExhaustion 조정 축소 (handleExhaustion 미참조)
  * isClimbSprintSpeed (원본 L2659-L2671) — SmartStatisticsFactory 미이식 → `true` 근사
  * can* 4 판정 (원본 L2673-L2676) — `isBurning`/`isUsingItem`/`sprintDuringItemUsage`/
    `verticalCollision`/`collidedHorizontallyTickCount` 사용
  * 6 Sprint 변종 (원본 L2678-L2684) — isGroundSprinting 필드 + 5 지역 변수.
    isLevitating 필드 참조 — B-10d 후 자동 활성
  * standing 지역 (원본 L2686) — `onGround && !isSliding && !isCrawling`
  * **isFast 6갈래 OR** (원본 L2688-L2695) — isClimbSprinting 중복 1:1 보존
- **A-1 불일치 #1 해소**: 기존 `grab && isSprinting()` 간소 매핑 → 원본 6갈래 OR 완전 복원.
  수영/잠수/비행/등반/천장 컨텍스트 전부 sprint 변종 감지.
- isGroundSprinting 전환 후처리 (원본 L2697-L2709) — 별도 B-N 원자 분리 (setSprinting
  호출 + wasRunningWhenSprintStarted / Options._runOnSprintRelease 등 미이식 의존)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 51 기준)**:
- [근거] 원본 L2617-L2695 전수 read (R-10.6~R-10.8) ✓
- [근거] B-1c1 Config 헬퍼/필드 이식 확인 ✓
- [대응] 6갈래 OR 공식 원본 1:1 (isClimbSprinting 중복 포함) ✓
- [분기] isSprintJump 2분기 / exhaustionAllows* / preferSprint / isClimbSprintSpeed /
  can* 4 / 6 Sprint 변종 / standing / isFast 6갈래 전수 ✓
- [상수] `sprintExhaustionStop=100F` / `sprintExhaustionStart=50F` / `sprintEnableStanding=false`
  / `collidedHorizontallyTickCount<3` 임계값 원본 동일 ✓
- [타이밍] isSprintJump 이전 틱 `isFast` 참조 — 해당 블록 이전에 isFast 이전 틱 값이
  유지되는 구조 (현재 틱 isFast 는 블록 말미에 갱신) ✓
- [근사] `isClimbSprintSpeed=true` 근사 (SmartStatisticsFactory 미이식), `maxExhaustion*`
  조정 축소 (handleExhaustion 축소판 — 필드 미참조). 각 주석 명시 ✓
- [신규] isGroundSprinting 전환 후처리 미이식 → 별도 B-N 원자 (B-48 후보) ✓
- [회귀] compileJava 성공 — 기존 `grab && isSprinting()` 호출 대체 완료 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**🎉 A-1 불일치 3건 전부 해소**:
- ✅ #1 **isFast 6갈래 OR** (B-1f 세션 51) — 간소 매핑 → 원본 1:1 완전 복원
- ✅ #2 isSlow Config.isSneakingEnabled() (B-2 세션 43)
- ✅ #3 wouldIsSneaking `!wantSprint` (B-3b 세션 49)

**Phase 2 진행 상황 (세션 51 기준)**:
- ✅ 세션 43-49: B-2 / B-44a / B-30 / B-32 / B-40 / B-45 / B-23 / B-17a/b1 / B-3a/b (11 원자)
- ✅ 세션 50: B-1c1 (Config 7필드+3헬퍼)
- ✅ 세션 51: **B-1c2/B-1c3/B-1d/B-1e/B-1f** (isFast 체인 완성, A-1 #1 해소)
- ⏳ 잔여 ~23 원자 (B-17b2 / B-18 / B-24 / B-16 / B-33 / B-48 isGroundSprinting 전환 등)

**다음 작업 권고**:
- **B-48** (isGroundSprinting 전환 후처리) — `wasRunningWhenSprintStarted` + `Options._run/walkOnSprintRelease`
  필드 + `isStandupSprintingOrRunning()` 메서드 이식. 중간 규모.
- **B-18** (isClimbCrawling + climbIntoCount 카운터) — B-16 (isClimbHolding) 선행 필요.
- **B-17b2** (isCrawlClimbing 전환 3분기) — wantClimbUp/Down 필드 승격 + move() API.
- **B-24** (isHeadJumping 해제 엣지 handleCrash) — Config 필드 2개 신설 + handleCrash 공유.

### 세션 52 — 2026-04-24 — B Phase 2 B-4 (R-09 간소 매핑 제거)

**진행한 작업**:
- R-09 블록 L1168-L1173 간소 매핑 주석 + 지역 변수 `wantSneak_` / `wantSprint_` 제거
- L1179 조건을 원본 L2990 1:1 으로 복원:
  `wantSneak && wantSprint && sneakKeyStartPressed && sneakToggled`
  * `wantSneak` — L835 else 블록 직계 지역 변수 (B-2 세션 43, `cfg0.isSneakingEnabled() &&
    wouldWantSneak`)
  * `wantSprint` — public 필드 (B-3a 세션 49, 원본 L2595-L2615 6조건 OR)
  * 둘 다 R-09 블록과 동일 스코프 (else 블록 내) 접근 가능
- 기존 "간소 매핑" 주석 제거 — 1:1 복원 완료 명시
- §16 세션 30 신규 발견 **#1 해소** (R-09 L788 간소 매핑)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 52 기준)**:
- [근거] 원본 L2990 `wantSneak && wantSprint && sneakButton.StartPressed && sneakToggled`
  직접 read (R-15.5 표 8번째 항목) ✓
- [근거] B-2 wantSneak / B-3a wantSprint 이식 완료 확인 ✓
- [대응] 원본 L2990 1:1 (지역 변수 + public 필드 각각 원본 구조 반영) ✓
- [분기] sneakStartPressed + sneakToggled 엣지 조건 그대로 ✓
- [상수] 없음
- [타이밍] R-09 블록 내 wantSneak 스코프 검증 완료 ✓
- [근사] 제거됨 — 간소 매핑 → 1:1 원본 복원 ✓
- [신규] 없음
- [회귀] compileJava 성공 — 기존 `sneakToggled + sneakStartPressed` 조건 동치성 확인 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**§16 세션 30 신규 발견 해소 현황**:
- ✅ **#1 R-09 L788 간소 매핑 주석** (B-4 세션 52)
- ⏳ #2 Swimmer L159 "1.21.1 간소화" (B-5 — B-9 메인 분류 재작성 시 해소 예정)

**Phase 2 진행 상황 (세션 52 기준)**:
- ✅ 세션 43-51: 17 원자 (B-2/B-44a/B-30/B-32/B-40/B-45/B-23/B-17a/b1/B-3a/b/B-1c~f)
- ✅ 세션 52: **B-4** (R-09 간소 매핑 제거 — §16 #1 해소)
- ⏳ 잔여 ~22 원자

**다음 작업 권고**:
- **B-48** (isGroundSprinting 전환 후처리) — 원본 L2697-L2709. 중간 규모 + Options 필드
  선행. `wasRunningWhenSprintStarted` 필드 + `Options._runOnSprintRelease` /
  `_walkOnSprintRelease` + `isStandupSprintingOrRunning()` 메서드.
- **B-24** (isHeadJumping 해제 엣지 handleCrash) — Config 2 필드
  (`headFallDamageStartDistance=2F` / `Factor=2F`) + `restoreFromFlying` 필드 +
  `handleCrash` 공유 가시성 변경 + B-23 자리에 로직 추가.
- **B-18** (isClimbCrawling + climbIntoCount) — B-16 선행. 규모 큼.

### 세션 53 — 2026-04-24 — B Phase 2 B-24 (isHeadJumping 해제 엣지 handleCrash)

**진행한 작업**:
- `SmartMovingConfig` Config 2 필드 추가 (원본 L395-L396):
  * `headFallDamageStartDistance = 2F` (Positive defaults values 2F/1F/3F — 기본 2F)
  * `headFallDamageFactor = 2F` (IncreasingFactor defaults 2F)
- `SmartMovingClientState` 에 B-24 필드/메서드 추가:
  * `restoreFromFlying` boolean 필드 + resetState 리셋 (원본 L1461 대응 — 정확
    선언 위치는 리서치 미확인이나 SmartMovingSelf 필드로 존재)
  * `handleCrash(player, startDistance, factor)` public static 메서드
    (원본 L2232-L2243 — Climber private 버전 동치, 복사 이식)
- B-23 재평가 블록 뒤 자리에 해제 엣지 로직 이식 (원본 L2535-L2540):
  `if (wasHeadJumping && !isHeadJumping && player.isOnGround()) {
       handleCrash(player, cfg0.headFallDamageStartDistance, cfg0.headFallDamageFactor);
       restoreFromFlying = true;
   }`
- `restoreFromFlying = true` 설정은 standupIfPossible 트리거 — 1.21.1 미이식이나
  필드 값 설정 자체는 정확 동작. B-N 후속 standupIfPossible 이식 시 자동 연결.
- **R-13 세션 34 A-4 불일치 #2 해소** (handleCrash + restoreFromFlying 미이식).
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 53 기준)**:
- [근거] 원본 L395-L396 Config 필드 + L2232-L2243 handleCrash + L2535-L2540 해제 엣지
  전수 read (R-13.3) ✓
- [근거] Climber L489 handleCrash 이식본 확인 — ClientState 복사 동치 ✓
- [대응] Config 2필드 기본값 2F/2F 원본 동일 ✓
- [대응] handleCrash 공식 원본 1:1 (`fallDistance > startDistance` → 데미지) ✓
- [대응] 해제 엣지 조건 `wasHeadJumping && !isHeadJumping && onGround` 원본 그대로 ✓
- [분기] 3-AND 조건 + handleCrash 내부 fallDistance 판정 ✓
- [상수] 2F 원본 Positive defaults ✓
- [타이밍] B-23 재평가 뒤 isAerodynamic 리셋 뒤 위치 — 원본 L2533→L2535 순서 복원 ✓
- [근사] 없음 — `restoreFromFlying` 필드 설정은 standupIfPossible 미이식이나 값 설정
  자체는 정확 ✓
- [신규] 없음
- [회귀] compileJava 성공 — 기존 Climber handleCrash 호출 영향 없음 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**R-13 A-4 불일치 현황**:
- ✅ #1 매 틱 재평가 5-AND (B-23 세션 46)
- ✅ #2 해제 엣지 후처리 handleCrash+restoreFromFlying (B-24 세션 53)
- ⏳ #3~#13 (직접 진입 조건/부수 동작/fallDistance/isSliding 등)

**Phase 2 진행 상황 (세션 53 기준)**:
- ✅ 세션 43-52: 18 원자
- ✅ 세션 53: **B-24**
- ⏳ 잔여 ~21 원자

**다음 작업 권고**:
- **B-48** (isGroundSprinting 전환 후처리) — Options 필드 이식 필요. 중간 규모.
- **B-18** (isClimbCrawling + climbIntoCount 카운터) — B-16 선행.
- **B-29** (toSlidingOrCrawling 조건 정정) — `grabPressed || wasHeadJumping` 로 정정.
  wasHeadJumping 이식됨 → 가능.
- **B-27** (isSliding fallDistance 분기 이식) — 단일 공식. 가능.

### 세션 54 — 2026-04-24 — B Phase 2 B-29 + B-27

**진행한 작업**:
- **B-29 `toSlidingOrCrawling` 조건 정정** (`SmartMovingJumper.resetHeightOffset` L103):
  * 기존: `(player.isSprinting() || sm.isFast) && cfg.slide`
  * 정정: `cfg.slide && cfg.enabled && (SmartMovingKeys.grab.isPressed() || sm.wasHeadJumping)`
  * 원본 L2226 `Config.isSlidingEnabled() && (grabButton.Pressed || wasHeadJumping)` 1:1 복원
  * `wasHeadJumping` B-22a 이식 + B-23 에서 tickEssential 저장 완료 → 의존 해소
  * 의미 차이 상당 — 원본은 "잡기 or 헤드점프 종료", 기존은 "스프린트 or SM Fast"
- **B-27 isSliding fallDistance 분기 이식** (tickEssential SlideToHeadJumping 뒤):
  * 원본 L2569-L2574:
    `if (isSliding && fallDistance > cfg.fallingDistanceMinimum) {
        isSliding=false; wasCrawling=true; isCrawling=false; }`
  * `fallingDistanceMinimum=3F` (B-32 세션 44 이식됨) 사용
  * SlideToHeadJumping 0.05F 임계와 대조: 큰 낙하 → 크롤 전환 준비 (반면 0.05F 는
    살짝 낙하 → 헤드점프 전환)
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 54 기준)**:
- [근거] 원본 L2226 toSlidingOrCrawling 조건 직접 read (R-13.6) ✓
- [근거] 원본 L2569-L2574 fallDistance 분기 직접 read (R-13.5) ✓
- [대응] B-29 원본 1:1 (AND + OR 조건) ✓
- [대응] B-27 원본 1:1 (3필드 동시 변경) ✓
- [분기] SlideToHeadJumping (0.05F) vs B-27 fallDistance (3F) 2단계 임계값 구분 ✓
- [상수] `fallingDistanceMinimum=3F` 원본 일치 ✓
- [타이밍] Jumper 타이밍 확인 — tickEssential 이후 travel() 시점, wasHeadJumping 이번
  틱 재평가 직전 값 (원본 동일) ✓
- [근사] 없음 ✓
- [신규] 없음
- [회귀] compileJava 성공 — 기존 Jumper 분기 로직 의미 완전히 다른 거라 동작 변경 예상 ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**R-13 A-4 불일치 현황**:
- ✅ #1 매 틱 재평가 5-AND (B-23 세션 46)
- ✅ #2 해제 엣지 후처리 (B-24 세션 53)
- ✅ #6 fallDistance > fallingDistanceMinimum 분기 (B-27 세션 54)
- ✅ #8 toSlidingOrCrawling 조건 완전 대체 (B-29 세션 54)
- ⏳ #3 직접 진입 6-AND 조건 / #4 부수 동작 3건 / #5 isHeadJumping=false / #7 handleClimbing 해제 / #9-#13 미이식 필드

**Phase 2 진행 상황 (세션 54 기준)**:
- ✅ 21 원자 완료
- ⏳ 잔여 ~19 원자

**다음 작업 권고**:
- **B-28** (handleClimbing 진입 시 isSliding=false) — R-13 #7 / R-14 #7. Climber 에 추가
  or B-14 resetClimbing 에 포함. 단순.
- **B-25/B-26** (직접 진입 6-AND + 부수 동작) — 의존 필드 다수 필요 (wasRunning+isRunning+
  isGroundSprinting+sneakStartPressed — wasRunning/isRunning 이식 완료, isGroundSprinting
  이식 완료 → 가능). 규모 중간.
- **B-48** (isGroundSprinting 전환 후처리) — Options 필드 선행.

### 세션 55 — 2026-04-24 — B Phase 2 B-25 (isSliding 직접 진입 6-AND)

**진행한 작업**:
- IMPL-02 L1013-L1022 조건 원본 L2553-L2561 로 완전 정정:
  * 기존: `isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping` (간소)
  * 정정: `cfg.slide && cfg.enabled && grabPressed && (isGroundSprinting ||
    (wasRunning && !isRunning && onGround)) && !isCrawling && sneakKeyStartPressed &&
    !isDipping`
  * 필드 세팅 3건 추가 (원본 L2558-L2560): `isSliding=true; isHeadJumping=false;
    isAerodynamic=false`
- wasRunning 저장 (B-43 R-09 종료부) 미이식 → wasRunning 분기 항상 false.
  isGroundSprinting 분기만 활성. B-43 이식 시 완전 복원.
- B-28 검토: 원본 L985 은 wantClimbUp+handsClimbing.IsRelevant 조건부 — 단순 handleClimbing
  진입 해제가 아님. B-19 (Free climb 분기) 의존. 보류.
- B-26 검토: `Config.SlideDown=4` 상수 미이식 + Jumper.tryJump 시그니처 확장 필요.
  규모 큼 — 별도 세션.
- `./gradlew compileJava --rerun-tasks` 성공

**완료 전 검증 체크리스트 (세션 55)**:
- [근거] 원본 L2553-L2561 직접 read (R-13.4) + 의존 필드 전수 확인 ✓
- [대응] 6-AND + 필드 세팅 3건 원본 1:1 ✓
- [분기] 6-AND 각 항 + 2갈래 OR + 필드 세팅 3건 ✓
- [상수] 없음
- [타이밍] wasRunning 저장 미이식 주석 명시 ✓
- [근사] 기존 간소 매핑 완전 제거 ✓
- [신규] 없음
- [회귀] compileJava 성공 — IMPL-02 진입 조건 의미 변경 (기존보다 엄격) ✓
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**R-13 A-4 불일치 현황**:
- ✅ #1/#2/#6/#8 (이전 세션들)
- ✅ **#3 직접 진입 6-AND** + **#5 isHeadJumping=false** (B-25 세션 55)
- ⏳ #4 부수 동작 3건 (B-26) / #7 handleClimbing 해제 (B-28) / #9~#13 미이식

**Phase 2 진행 상황**: 22 원자 완료 / 잔여 ~18

**다음 작업 권고**:
- **B-43** (R-09 종료부 `wasRunning=isRunning` + `wasLevitating=isLevitating` 저장) —
  단순. B-25 의 wasRunning 분기 활성화 효과.
- **B-26** (부수 동작) — Config.SlideDown 상수 + Jumper.tryJump 확장. 규모 중간.
- **B-48** (isGroundSprinting 전환 후처리) — Options 필드 선행.

### 세션 56 — 2026-04-24 — B Phase 2 B-43 (R-09 종료부 저장 2건)

**진행한 작업**:
- ClientState `wasLevitating` public boolean 필드 신설 (wasRunning 바로 뒤 L149 근처).
  주석에 원본 L3044 + 사용처 (isGroundSprinting 전환 후처리 L2699) 표기. isLevitating 공식
  B-10d 미이식이라 결과적 false 유지지만 저장 라인 1:1 이식이 목표.
- R-09 블록 종료부 (L1261 sneakKeyStopPressed 처리 뒤, 블록 `}` 직전) 에 저장 2줄 추가:
  * `wasRunning    = isRunning(player);`
  * `wasLevitating = isLevitating;`
  원본 L3043-L3044 1:1 대응. `isRunning(player)` 는 세션 42 B-22c 로 이식된 메서드.
- L1022 기존 "wasRunning 미이식 → 항상 false" 주석을 "B-43 (세션 56) 이식 완료" 로 갱신.
  B-25 isSliding 직접 진입 6-AND 의 두 번째 OR 분기 (`wasRunning && !isRunning(player)
  && onGround`) 가 이제 정상 활성.
- resetState 에 `wasLevitating = false` 추가 (wasRunning=false 바로 뒤).
- `./gradlew compileJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 56)**:
- [근거] 원본 L3043-L3044 직접 read (SmartMovingSelf.md L2030-L2031 + R-15.4) ✓
- [근거] R-15.4 R-09 블록 종료부 누락 2건 확정 자료 재확인 ✓
- [대응] 저장 2줄 + 필드 선언 + resetState 리셋 원본 1:1 ✓
- [분기] 없음 (단순 저장)
- [상수] 없음
- [타이밍] R-09 블록 내부 마지막 statement — 원본 `sneakButton.StopPressed` 처리 뒤 저장
  순서와 동일 ✓
- [근사] 없음 — isLevitating 갱신 공식이 B-10d/B-9 로 지연되어 현재 false 지만 저장 자체는
  1:1. 공식 이식 시 자동 활성.
- [신규] 없음
- [회귀] compileJava 성공 — B-25 의 wasRunning 분기가 이제 실제 값 기반으로 평가됨
  (기존엔 항상 false). 즉 슬라이딩 직접 진입 조건이 더 너그러워질 수 있음 — 원본 의도.
- [빌드] ./gradlew compileJava --rerun-tasks ✓

**A-6 R-15.4 불일치 현황**:
- ✅ **#1 wasRunning 저장** + **#2 wasLevitating 저장** (B-43 세션 56)
- 저장 시점 이동 (B-44a~c) 은 각 공식 이식 원자와 함께 조정 예정

**Phase 2 진행 상황**: 23 원자 완료 / 잔여 ~17

**다음 작업 권고**:
- **B-26** (부수 동작) — Config.SlideDown 상수 + Jumper.tryJump 확장. 규모 중간.
- **B-14** (resetClimbing 신설) — 등반 필드 리셋 헬퍼. B-21/B-28 해소.
- **B-16** (isClimbHolding/wantClimbHolding 3-OR 공식) — B-18 선행 블록.
- **B-10d** (isLevitating 공식) — 간단한 4조건 AND — wasLevitating 완전 활성화.

### 세션 57 — 2026-04-24 — B Phase 2 B-14 + B-21 (resetClimbing 이식)

**진행한 작업**:
- ClientState `resetClimbing()` public 메서드 신설 (toCrawling 바로 뒤 배치). 원본
  SmartMovingSelf L1474-L1486 `private void resetClimbing()` 1:1 이식 — 10 필드 리셋:
  * `isClimbing = false; isHandsVineClimbing = false; isFeetVineClimbing = false;`
  * `isVineOnlyClimbing = false; isVineAnyClimbing = false; isClimbingStill = false;`
  * `isNeighborClimbing = false; isCeilingClimbing = false;`
  * `actualHandsClimbType = HandsClimbing.NO_GRAB;` (int 0)
  * `actualFeetClimbType  = FeetClimbing.NO_STEP;`  (int 0)
- SmartMovingClimber.handleClimbing L238 진입부 (`SmartMovingConfig cfg = ...` 직후,
  exhaustion 체크 앞) 에 `sm.resetClimbing();` 호출 추가. 원본 L816 첫 문장 위치 1:1.
  Standard/Simple/Smart/Free Base Climb 공통 리셋.
- ClientState import 에 `choco.ratel.smartmoving.climbing.HandsClimbing` + `FeetClimbing` 2건
  추가 (NO_GRAB/NO_STEP 상수 참조 위해).
- **B-21 자동 해소**: 원본 L1485 `isCeilingClimbing = false` (resetClimbing 내부) 가
  resetClimbing() 이식으로 자동 수행됨. 별도 해제 엣지 불필요.
- **B-28 은 별도 유지**: 원본 L985 `isSliding = false` 는 `wantClimbUp && handsClimbing
  .IsRelevant()` 조건부 (resetClimbing 내부 아님). B-19 Free Climb 분기 의존 — 별도 원자.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공 (1회 import 누락 수정 후).

**완료 전 검증 체크리스트 (세션 57)**:
- [근거] 원본 L1474-L1486 resetClimbing() 원문 확보 (research/.../SmartMovingSelf.md L993-L1008) ✓
- [근거] R-12.3 handleClimbing 구조 + R-12.10 #1/#13 resetClimbing 미이식 확정 ✓
- [대응] 10 필드 리셋 원본 1:1 — 필드명/값/순서 모두 동일 ✓
- [분기] 없음 (단순 대입 10건)
- [상수] `HandsClimbing.NO_GRAB = 0` / `FeetClimbing.NO_STEP = 0` 상수 B-15f 이전 이식 확인 ✓
- [타이밍] Climber.handleClimbing 진입 직후 호출 — 원본 L816 위치 1:1 ✓
- [근사] 없음 — 1:1 이식. 원본 enum `HandsClimbing.NoGrab` → 1.21.1 int `NO_GRAB` 표면 매핑만.
- [신규] 없음
- [회귀] compileJava + compileClientJava 둘 다 ✓. 등반 10 필드가 매 틱 무조건 리셋 →
  B-15 세션 40 에서 이식한 필드들이 이전 틱 쓰레기 값 남기던 문제 해소.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing() 매 틱 호출 (B-14 세션 57)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 자동 해소 세션 57)
- ⏳ #2~#12/#14 (B-16/B-17b2/B-18/B-19/B-20/B-28 등)

**Phase 2 진행 상황**: 24 원자 완료 (B-14 + B-21) / 잔여 ~16

**다음 작업 권고**:
- **B-16** (wantClimbHolding/isClimbHolding 3-OR 공식) — 원본 L2721-L2732. B-18 선행.
- **B-26** (isSliding 직접 진입 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처.
- **B-38** (handleCeilingClimbing 진입 isCrawling=false) — 한 줄 추가 단순.
- **B-34** (capabilities.flying 해제 점프) — tryJump 단순 호출.

### 세션 58 — 2026-04-24 — B Phase 2 B-38 (handleCeilingClimbing 진입 isCrawling=false)

**진행한 작업**:
- `SmartMovingClimber.handleCeilingClimbing` 성공 분기 종료부에 `sm.isCrawling = false;` 한 줄
  추가 (L577 `sm.isCeilingClimbing = true;` 직후, 분기 `}` 직전).
- 원본 L1170 대응. 원본 L1162 `isCeilingClimbing=true` 와 L1170 `isCrawling=false` 가 동일한
  성공 분기 내부에 배치된 구조 1:1. 1.21.1 에서는 속도 세팅(L571)/fallDistance(L575) 후
  isCeilingClimbing=true(L577) + isCrawling=false 순서.
- 주석에 원본 라인 번호 + 위치 맥락 명시.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 58)**:
- [근거] 원본 L1170 위치 (handleCeilingClimbing 진입 성공) 확정 자료 R-14.x +
  R-14.10 #8 불일치 §16 세션 35 ✓
- [근거] 원본 L1112-L1174 handleCeilingClimbing 구조 (R-12.4) 재확인 ✓
- [대응] 한 줄 대입 원본 1:1 — 필드/값 동일 ✓
- [분기] 없음 (성공 분기 종료부 단일 대입)
- [상수] 없음
- [타이밍] isCeilingClimbing=true 직후 — 원본 L1162/L1170 동일 분기 내부 배치 순서 일치 ✓
- [근사] 없음
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 천장 클라이밍 진입 시 크롤 자동 해제 →
  R-14.10 #8 해소. 다른 경로 (resetState 등) 의 isCrawling 갱신과 충돌 없음 (성공 분기
  내부 한정).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #8 L1170 handleCeilingClimbing 진입 isCrawling=false (B-38 세션 58)
- ⏳ #1~#7/#9~#15 (B-16/B-17b2/B-18/B-33/B-34/B-35/B-36/B-37/B-39/B-41 등)

**Phase 2 진행 상황**: 25 원자 완료 / 잔여 ~15

**다음 작업 권고**:
- **B-34** (capabilities.flying 해제 점프 tryJump) — 원본 L2449-L2450. 단순. B-31a wasCrawling 선행 완료.
- **B-37** (handleClimbing wall 오르기 crawl 진입) — 원본 L985-L986. B-19 Free climb 분기 내부 — 위치 탐색 필요.
- **B-16** (wantClimbHolding/isClimbHolding 3-OR) — 원본 L2721-L2732. B-18 선행용 중요 블록.
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.

### 세션 59 — 2026-04-24 — B Phase 2 B-34 (capabilities.flying 해제 점프)

**진행한 작업**:
- ClientState tickEssential IMPL-01 블록 (L988-L1021) 종료 직후 + B-25 IMPL-02 앞에 원본
  L2449-L2450 이식. 원본: `if (wasCrawling && !isCrawling && esp.capabilities.isFlying)
  tryJump(Config.Up, null, null, null);`
- 1.21.1 코드:
  ```java
  if (wasCrawling && !isCrawling && player.getAbilities().flying) {
      SmartMovingJumper.tryJump(player, this, SmartMovingJumper.UP, 0F);
  }
  ```
- 표면 매핑: `esp.capabilities.isFlying` → `player.getAbilities().flying` /
  `tryJump(Config.Up, null, null, null)` → `tryJump(player, sm, UP, 0F)` (4-param 축소).
  원본 `angle==null` 은 1.21.1 `charge=0F` 와 동일 의미 (vanilla Up 경로).
- 주석으로 원본 라인 + 배치 이유 (wasCrawling L761 이전 틱 저장 + isCrawling IMPL-01 최종값) 명시.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 59)**:
- [근거] 원본 L2449-L2450 확보 (research/.../SmartMovingSelf.md L4026-L4028 R-14.3) ✓
- [근거] R-14.10 #4 불일치 `capabilities.flying 해제 점프` 확정 자료 §16 세션 35 ✓
- [대응] if 조건 3-AND + tryJump 호출 원본 1:1 ✓
- [분기] 없음 (단일 if)
- [상수] `SmartMovingJumper.UP = 0` 이식 완료 확인 ✓
- [타이밍] IMPL-01 종료 직후 — isCrawling 최종값 확정 시점. wasCrawling 은 L761 tickEssential
  초반 저장된 이전 틱 값. 원본 L2441 직후 위치와 의미적 동치 ✓
- [근사] `tryJump` 4-param→2-param 축소는 B-1 세션 25 이식 결정. `angle==null ↔ charge=0F`
  vanilla Up 경로 등가 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 비행 모드에서 크롤 해제 시 tryJump 발동 경로
  복원. 다른 호출 지점과 간섭 없음.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #4 L2449-L2450 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #8 L1170 handleCeilingClimbing 진입 isCrawling=false (B-38 세션 58)
- ⏳ #1~#3/#5~#7/#9~#15

**Phase 2 진행 상황**: 26 원자 완료 / 잔여 ~14

**다음 작업 권고**:
- **B-16** (wantClimbHolding/isClimbHolding 3-OR) — 원본 L2721-L2732. B-18 선행용 중요 블록.
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장.
- **B-37** (handleClimbing wall 오르기 crawl 진입) — 원본 L985-L986. Free climb 분기 내부.
- **B-39** (landMotionPost 3분기 isSlow > minY+0.5D) — 원본 L1392-L1403. ClientState 내부.

### 세션 60 — 2026-04-24 — B Phase 2 B-6 (Swimmer updateSwimState 3-OR 복원)

**진행한 작업**:
- `SmartMovingSwimmer.updateSwimState` L78 조건을 원본 L301 1:1 로 복원:
  * 기존: `if (sm.isCrawling || sm.isCrawlClimbing)` (2-OR, isClimbCrawling 누락)
  * 정정: `if (sm.isCrawling || sm.isClimbCrawling || sm.isCrawlClimbing)` (3-OR)
- 주석에 원본 L301 + L308-L309 handleSwimming 맥락 + B-18 의존 (isClimbCrawling 공식
  미이식 현재 항상 false) 명시.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.
- **다른 후보 평가 기록**: B-39 는 부모 분기 (3번째 else if `crawlStandUpBottom > minY`)
  조차 미이식 + AABB 정밀 계산 필요 → 규모 중간-대. B-16 은 wantClimb/blocked 필드
  미이식 → 의존 분해 필요. 단순 1-줄 원자 B-6 선택.

**완료 전 검증 체크리스트 (세션 60)**:
- [근거] 원본 L301/L308-L309 3-OR 강제 isDipping 공식 (R-11.4 + R-11.12) 확보 ✓
- [근거] R-11.12 불일치 #3 "isClimbCrawling 조건 누락" 확정 §16 세션 32 ✓
- [대응] OR 3항 원본 1:1 — 필드명/순서/논리 연산자 동일 ✓
- [분기] 없음 (OR 조건만 추가)
- [상수] 없음
- [타이밍] updateSwimState 진입 직후 분기 — 원본 L301 위치 일치 ✓
- [근사] 없음 — 1:1 정렬. isClimbCrawling 공식 미이식은 B-18 범위 (별도 이슈)
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. isClimbCrawling 이 현재 항상 false 라
  동작상 변화 없음 (B-18 이식 후 활성). 조건 구조만 1:1 정렬.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-11 A-2 불일치 현황**:
- ✅ #3 isClimbCrawling 조건 누락 (B-6 세션 60)
- ⏳ #1/#2/#4~#11 (B-7/B-8/B-9/B-11/B-12/B-13 등 — 대부분 B-9 메인 분류 재작성 범위)

**Phase 2 진행 상황**: 27 원자 완료 / 잔여 ~13

**다음 작업 권고**:
- **B-13** (crawl↔swim 전환 isSliding 추가) — `SmartMovingSwimmer.handleSwimming` L119/L124
  조건에 `(isCrawling || isSliding)` 반영. 단순 1줄.
- **B-37** (handleClimbing wall 오르기 crawl 진입) — 원본 L985-L986. B-19 Free climb 분기
  내부 — wantClimbUp/handsClimbing.IsRelevant 의존 위치 탐색 필요.
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.

### 세션 61 — 2026-04-24 — B Phase 2 B-13 (crawl↔swim 전환 isSliding 추가)

**진행한 작업**:
- `SmartMovingSwimmer.handleSwimming` L126 조건 확장. 원본 L2434 (= R-06 L418)
  `(isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder` 1:1 복원:
  * 기존: `if (wasCrawling && sm.dippingDepth >= 0F)`
  * 정정: `if ((wasCrawling || sm.isSliding) && sm.dippingDepth >= 0F)`
- 주석에 원본 라인 + 등가성 근거 (`wasCrawling` = L121 이전 스냅샷 / isSliding L121 에서
  수정 안 됨) 명시.
- L121 `isCrawling && dippingDepth > SWIM_CRAWL_TOP` 조건은 원본 L2415 와 일치 (isSliding
  불필요) — §10 B-13 의 "L119/L124" 범위 중 L119 는 수정 대상 아님 확정.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 61)**:
- [근거] 원본 L2434 (R-06 L418) `(isCrawling || isSliding) && playerCrawlWaterBorder <
  SwimCrawlWaterMaxBorder` (research/.../SmartMovingSelf.md L2434 + L3066) 확보 ✓
- [근거] R-11.6 크롤↔수영 전환 + R-11.12 불일치 #11 확정 자료 §16 세션 32 ✓
- [대응] OR 2항 원본 1:1 — 필드명/논리 연산자/순서 동일 ✓
- [분기] 없음 (if 조건 확장)
- [상수] 없음
- [타이밍] `wasCrawling` 캡처 시점 L118 (진입 직후) = 원본 L2415 이전 시점. isSliding 은
  L121 에서 변경 안 되므로 원본 L2434 시점과 일치 ✓
- [근사] 1.21.1 `playerCrawlWaterBorder = dippingDepth` + `SwimCrawlWaterMaxBorder` → `SWIM_CRAWL_MAX`
  상수 매핑은 R-11.12 기존 이식 (근사 아님). isSliding 추가는 1:1.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. isSliding 시 얕은 물 반발 경로 활성화 —
  원본 의도 복원. B-25 (isSliding 직접 진입 6-AND) 이식 완료 후라 실제 작동 가능.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-11 A-2 불일치 현황**:
- ✅ #3 isClimbCrawling 조건 누락 (B-6 세션 60)
- ✅ #11 crawl↔swim 전환 isSliding 누락 (B-13 세션 61)
- ⏳ #1/#2/#4~#10 (B-7/B-8/B-9/B-11/B-12 등 — 대부분 B-9 메인 분류 재작성 범위)

**Phase 2 진행 상황**: 28 원자 완료 / 잔여 ~12

**다음 작업 권고**:
- **B-37** (handleClimbing wall 오르기 crawl 진입) — 원본 L985-L986. Free climb 분기 내부
  `wantClimbUp + handsClimbing.IsRelevant` 조건. Climber L374-L375 지역 변수 존재 —
  해당 분기 위치 탐색 후 단순 2줄 추가 가능.
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존 — 규모 중.

### 세션 62 — 2026-04-24 — B Phase 2 B-37 (handleClimbing wall 오르기 crawl 진입)

**진행한 작업**:
- `SmartMovingClimber.handleClimbing` L385 `if (wantClimbUp)` 블록 내부 최상단 (속도
  분기 L386 앞) 에 원본 L985-L986 이식:
  ```java
  if (sm.isSliding && handsClimbing.isRelevant()) {
      sm.isSliding  = false;
      sm.isCrawling = true;
  }
  ```
- 원본 위치: handleClimbing Free Climbing 분기 내 `if (wantClimbUp)` 첫 줄. 1.21.1 대응
  위치는 이미 `wantClimbUp` 분기가 이식된 Climber L385 — 내부 블록 진입 직후 배치.
- 슬라이딩 + grab + 전진 입력 조합 → 크롤로 전환하여 벽 오르기 시작하는 경로 복원.
- 원본 L987 `handsClimbing = handsClimbing.ToUp()` 은 B-37 범위 외 (별도 이슈).
- B-19 Free Climb 분기 내부 이슈는 Orientation/ClimbGap 계산 범위 — wantClimbUp 블록
  자체는 이식되어 있어 B-37 진행 가능.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 62)**:
- [근거] 원본 L985-L986 원문 확보 (research/.../SmartMovingSelf.md L4088-L4099 R-14.6) ✓
- [근거] R-14.10 #7 L986 wall 오르기 crawl 진입 [누락] 확정 §16 세션 35 ✓
- [대응] if 조건 + 2줄 대입 원본 1:1 ✓
- [분기] 없음 (단일 if)
- [상수] 없음
- [타이밍] `wantClimbUp` 블록 진입 첫 줄 — 원본 L985 위치 1:1 ✓
- [근사] 없음. `handsClimbing.IsRelevant()` → `handsClimbing.isRelevant()` 표면 매핑만
  (Java 네이밍 컨벤션).
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 슬라이딩 중 등반 시작 경로 복원 — 다른
  isSliding/isCrawling 갱신과 충돌 없음 (wantClimbUp + handsClimbing.isRelevant 게이트).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #4 L2449-L2450 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #7 L986 wall 오르기 crawl 진입 (B-37 세션 62)
- ✅ #8 L1170 handleCeilingClimbing 진입 isCrawling=false (B-38 세션 58)
- ⏳ #1~#3/#5/#6/#9~#15

**Phase 2 진행 상황**: 29 원자 완료 / 잔여 ~11

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존 — 규모 중.
- **B-31b/c** 공식 이식 (wantCrawlNotClimb / initializeCrawling) — B-41/B-35 범위 분배.

### 세션 63 — 2026-04-24 — B Phase 2 B-5 + B-28 (근사 이식 정리 + 자동 해소)

**진행한 작업**:
- **B-5 대조 완료**: `SmartMovingSwimmer.handleSwimming` L159 "1.21.1 간소화" 지점 원본
  SmartMovingSelf L226-L246 `isFakeShallowWaterSneaking=true` 설정 경로와 side-by-side 대조.
  **근사 3건 확정** — 정정 원자 추가 않고 §7 근사 이식 지점에 등록 (B-9 재작성 시 재평가):
  * (1) `couldStandUp` 수심 측정: 원본 AABB 정밀 `minPlayerSwimWaterDepth` → 1.21.1
    `player.getFluidHeight(WATER)` 단일 값.
  * (2) `getClimbingOrientations`: 원본 대각 포함 8방향 → 1.21.1 `Direction.Type.HORIZONTAL`
    4방향만.
  * (3) `swimDown=false` (원본 L244) 미이식 — 1.21.1 swim 수직 속도 로직이 swimDown 비의존.
  Swimmer 주석을 "1.21.1 간소화" → "근사 이식 — 원본과 차이 3건" 으로 정리 (L154-L173).
  §7 구조적 차이/근사 이식 지점에 B-5 근사 등록.
- **B-28 자동 해소**: A-4 R-13.9 #7 (B-28) 과 A-5 R-14.10 #7 (B-37) 은 동일 원본 L985-L986
  을 가리킴. 세션 62 B-37 이식 (`wantClimbUp` 블록 내 `isSliding && handsClimbing.isRelevant()`
  → `isSliding=false; isCrawling=true`) 으로 자동 해소. 별도 수정 없이 체크박스만 정리.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공 (주석만 변경).

**완료 전 검증 체크리스트 (세션 63)**:
- [근거] B-5: 원본 L226-L246 research/.../SmartMovingSelf.md 확인 + 1.21.1 Swimmer L150-L191
  코드 직접 read ✓
- [근거] B-28: R-13.9 #7 (§16 세션 34) + R-14.10 #7 (§16 세션 35) 동일 원본 라인 확인 ✓
- [대응] B-5: 근사 3건 문서화 완료 / B-28: B-37 세션 62 코드 재확인 ✓
- [분기] 없음 (문서/주석 중심)
- [상수] 없음
- [타이밍] 없음 (구조 변경 없음)
- [근사] **B-5 자체가 근사 이식 등록 작업** — §7 에 원본 차이 3건 명시 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 주석 변경만이라 동작 변화 없음.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**A-4 R-13.9 / A-5 R-14.10 불일치 현황 갱신**:
- ✅ R-13.9 #7 L985 handleClimbing isSliding=false (B-28 / B-37 세션 62 = 자동 해소)
- ✅ R-14.10 #7 L986 wall 오르기 crawl 진입 (B-37 세션 62)
- B-5 는 불일치 카운트 대상 아님 — 기존 근사 이식의 문서화 원자

**Phase 2 진행 상황**: 31 원자 완료 (B-5 + B-28 추가) / 잔여 ~9

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-12** (waterMovementTicks 증분 조건 정정) — 원본 L481-L484. 단순 조건 변경 가능성.

### 세션 64 — 2026-04-24 — B Phase 2 B-12 (waterMovementTicks 증분 조건 정정)

**진행한 작업**:
- `SmartMovingSwimmer.updateSwimState` 의 `waterMovementTicks` 갱신 2곳 원본 L481-L484
  1:1 정정. 원본: `if(swimming || diving) ticks++; else ticks = 0;`.
  * L80-L86 크롤/등반 크롤 → isDipping 강제 분기: `ticks++` → `ticks = 0` (dipping 강제
    경로는 swimming/diving 아님).
  * L88-L99 수심 분기 말미: 무조건 `ticks++` → `if (sm.isSwimming_sm || sm.isDiving)
    ticks++; else ticks = 0;`.
- 주석에 원본 라인 + 오역 원인 + B-10b isJumpingOutOfWater 공식 이식 대기 표기.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.
- **B-10b 범위 분리**: isJumpingOutOfWater 공식 (원본 L486-L487 `wantJumpOutOfWater +
  ticks>10 || onGround || wasJumpingOutOfWater`) 이식은 별도 원자로 유지 — B-12 는
  ticks 갱신 정확성만.

**완료 전 검증 체크리스트 (세션 64)**:
- [근거] 원본 L481-L484 research/.../SmartMovingSelf.md L3106-L3110 확보 ✓
- [근거] R-11.12 불일치 #9 `waterMovementTicks++ dipping 포함 증분 오역` 확정
  (§16 세션 32) ✓
- [대응] 2곳 모두 원본 `if(swim||dive) ticks++; else ticks=0` 1:1 ✓
- [분기] else 브랜치 (ticks=0) 양 위치 모두 명시 ✓
- [상수] 없음
- [타이밍] updateSwimState 내 dipping/swimming/diving 확정 직후 — 원본 L481 순서 동일 ✓
- [근사] 없음 — 1:1 이식
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. ticks 가 dipping 시 리셋되면서 wantJumpOutOfWater
  경로 (ticks>10 조건) 가 dipping 체류 중 발동되지 않음 — 원본 의도 복원.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-11 A-2 불일치 현황**:
- ✅ #3 isClimbCrawling 조건 누락 (B-6 세션 60)
- ✅ #9 waterMovementTicks dipping 포함 증분 오역 (B-12 세션 64)
- ✅ #11 crawl↔swim 전환 isSliding 누락 (B-13 세션 61)
- ⏳ #1/#2/#4~#8/#10 (대부분 B-9 메인 분류 재작성 범위)

**Phase 2 진행 상황**: 32 원자 완료 / 잔여 ~8

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-31b/c** 공식 이식 — B-41/B-35 범위 분배.

### 세션 65 — 2026-04-24 — B Phase 2 B-1b (Button ↔ KeyBinding 매핑 테이블 검증)

**진행한 작업**:
- §6.8 Button ↔ KeyBinding 매핑 테이블 전수 검증 (세션 37 A-7 에서 기본 테이블 작성됐으나
  `⚠️`/`✗` 항목 상태 미확정 — 실제 구현 grep 으로 정합성 마감).
- 수정 사항:
  * `jumpButton.StartPressed` ✗ → ✓ : `jumpKeyStartPressed` 필드 L96 존재 확인.
  * `sprintButton.Pressed` ⚠️ → ✓ : `sprintKey.isPressed()` L872 실제 사용 확인.
  * `grabButton.StartPressed` `wasPressed()` 매핑의 **시멘틱 차이** 명시 — vanilla API 는
    카운터 감소성 → 같은 틱 2회째부터 false. 다중 호출 주의 표기.
  * 원본 이동 엣지 관련 행 추가 — vanilla input 엣지 비제공 ⚠️.
  * 정합성 등급 범례 명시 (✓/⚠️/✗).
- §16 세션 65 신규 발견 기록:
  * (1) `grab.wasPressed()` 다중 호출 시 오동작 가능성 — 분류 [오역], 신규 원자 B-46 후보.
  * (2) `sprintButton.StartPressed/StopPressed` 미이식 — B-48 isGroundSprinting 전환 후처리
    범위에서 평가.
  * (3) `grabButton.StopPressed` 미이식 — 원본 사용 지점 확인 후 원자 신설.
- 코드 수정 없음 — 문서만 갱신.

**완료 전 검증 체크리스트 (세션 65)**:
- [근거] SmartMovingKeys.java 전체 read + SmartMovingClientState grep (엣지 필드 전수) ✓
- [근거] §6.8 세션 37 기본 테이블 재확인 + 실제 구현 grep ✓
- [대응] 각 Button 매핑 1.21.1 실제 이식 상태와 일치 ✓
- [분기] 없음 (문서 작업)
- [상수] 없음
- [타이밍] 없음
- [근사] `wasPressed()` 카운터 감소성 명시 (원본 StartPressed 와 시멘틱 차이) ✓
- [신규] 3건 §16 세션 65 등록 ✓
- [회귀] 없음 (문서만)
- [빌드] 코드 변경 없음

**Phase 2 진행 상황**: 33 원자 완료 / 잔여 ~7

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-46** (grab.wasPressed 다중 호출 해소) — §16 세션 65 발견 — 신규 원자. 1회 캐시 필드 추가.

### 세션 66 — 2026-04-24 — B Phase 2 B-46 (grab.wasPressed 다중 호출 해소)

**진행한 작업**:
- §16 세션 65 에서 발견된 `KeyBinding.wasPressed()` 카운터 소비성 버그 해소.
- ClientState `grabJustPressed` public boolean 필드 신설 (L463 jumpKeyStopPressed 뒤).
  주석에 vanilla API 시멘틱 + B-46 해소 근거 명시.
- tickEssential sneakKey 엣지 계산 직후 (L703) `grabJustPressed = SmartMovingKeys.grab
  .wasPressed();` 1회 저장. 이 시점 이후 모든 소비 지점은 필드 참조.
- pre-compute 블록 L786 지역 변수 `grabJustPressed0` 제거 + L828 참조를 `grabJustPressed`
  로 변경.
- IMPL-01 블록 L989 지역 변수 `grabJustPressed` 제거 — L1013 참조는 public 필드로 자연
  resolve (Java 접근 규칙).
- resetState 에 `grabJustPressed = false` 리셋 추가.
- §10 에 B-46 원자 신설 + 체크박스 완료.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 66)**:
- [근거] §16 세션 65 B-1b 발견 — `grab.wasPressed()` 다중 호출 오동작 확정 ✓
- [근거] vanilla `KeyBinding.wasPressed()` 구현 (timesPressed 카운터 1 소비 후 true) 확인 ✓
- [대응] 원본 `grabButton.StartPressed` 시멘틱 (틱 내 불변 불리언) 과 등가 ✓
- [분기] 없음 (필드 저장 + 참조 치환)
- [상수] 없음
- [타이밍] sneakKey 엣지 계산 직후 (L703) — 모든 소비 지점 (L828 pre-compute / L1013
  IMPL-01) 보다 앞서 저장 ✓
- [근사] 없음 — 시멘틱 정렬. sneakKey 스타일 (prev vs cur) 대신 wasPressed() 1회 호출
  유지한 이유: 원본 Button.update() 는 press 이벤트 기반, vanilla wasPressed() 도 이벤트
  카운터 기반이라 시멘틱 유사. prev vs cur 는 tick 경계만 보므로 짧은 click 놓칠 수 있음.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 다중 호출 제거로 pre-compute (wouldWantCrawl
  grab 진입 분기) 와 IMPL-01 (crawlToggled grab 해제 분기) 둘 다 동일 틱 값 참조 —
  원본 동작 복원.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**§16 세션 65 발견 현황**:
- ✅ (1) grab.wasPressed 다중 호출 오동작 (B-46 세션 66)
- ⏳ (2) sprintButton.StartPressed/StopPressed 미이식 — B-48 범위
- ⏳ (3) grabButton.StopPressed 미이식 — 원본 사용 지점 확인 후 신설

**Phase 2 진행 상황**: 34 원자 완료 / 잔여 ~6

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-48** 신설 후보 (isGroundSprinting 전환 후처리 + sprintKey 엣지) — 규모 중.

### 세션 67 — 2026-04-24 — B Phase 2 B-8 (Config 게이트 updateSwimState 적용)

**진행한 작업**:
- `SmartMovingSwimmer.updateSwimState` 말미 (offset 기반 3상태 결정 직후, waterMovementTicks
  증분 앞) 에 원본 L436-L441 Config 게이트 이식:
  ```java
  SmartMovingConfig cfg = SmartMovingConfig.Config;
  if (!cfg.isSwimmingEnabled()) { sm.isSwimming_sm = false; sm.isDipping = false; }
  if (!cfg.isDivingEnabled())   sm.isDiving = false;
  ```
- 원본 시멘틱: `swimming = !useStandard && swimming && Config.isSwimmingEnabled();
  diving = ... && Config.isDivingEnabled(); dipping = ... && Config.isSwimmingEnabled();`.
  Config 비활성화 시 해당 상태 플래그 false 정화.
- `useStandard` 재판정 (vanilla swim 경로로 위임) 은 B-9 메인 분류 재작성 범위로 분리 —
  B-8 은 **상태 플래그 정화만** 수행.
- 주석에 원본 라인 + 기존 handleSwimming return false 는 경로 차단만이고 플래그 정화 안
  됨 (오역) 표기.
- 헬퍼 `cfg.isSwimmingEnabled()` / `cfg.isDivingEnabled()` 는 세션 39 B-2/B-3a/B-8 에서
  이미 이식됨.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 67)**:
- [근거] 원본 L436-L441 research/.../SmartMovingSelf.md L3089-L3098 (R-11.7) 확보 ✓
- [근거] R-11.12 불일치 #2 `Config 게이트 누락` 확정 §16 세션 32 ✓
- [대응] `isSwimming_sm`/`isDipping` 2건 + `isDiving` 1건 게이트 원본 1:1 ✓
- [분기] Swimmer 게이트 / Diving 게이트 양쪽 명시 ✓
- [상수] 없음
- [타이밍] offset 분류 직후 — 원본 L436 위치 (분류 뒤 재게이트) 동일 ✓
- [근사] useStandard 재판정 생략 — B-9 범위. 현재 handleSwimming return false 로 vanilla
  경로 위임은 여전히 동작. 플래그 정화는 이번 세션에 확정.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. swim/dive Config 비활성화 시 상태 플래그가
  false 로 정화 → 애니메이션/전환/키 커맨드 (포커스 #1/#3/#4) 잘못된 true 참조 방지.
  기존 handleSwimming 경로 차단과 함께 안전 네트.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-11 A-2 불일치 현황**:
- ✅ #2 Config 게이트 누락 (B-8 세션 67)
- ✅ #3 isClimbCrawling 조건 누락 (B-6 세션 60)
- ✅ #9 waterMovementTicks dipping 증분 오역 (B-12 세션 64)
- ✅ #11 crawl↔swim 전환 isSliding 누락 (B-13 세션 61)
- ⏳ #1/#4~#8/#10 (대부분 B-9 메인 분류 재작성 범위)

**Phase 2 진행 상황**: 35 원자 완료 / 잔여 ~5

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-16** (wantClimbHolding 3-OR) — wantClimb/blocked 필드 의존 — 규모 중-대.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-20** (Standard/Simple Base Climb) — Config 분기 미이식 — 규모 중.

### 세션 68 — 2026-04-24 — B Phase 2 B-16 (wantClimbHolding 3-OR 근사 이식)

**진행한 작업**:
- Agent WebFetch 로 원본 `wantClimb` (L2479) / `blocked` (L2393) / `wouldWantClimb` (L2467)
  정의 확보. 핵심 발견:
  * `wantClimb` = `Config.isFreeClimbingEnabled() && wouldWantClimb` (단순 grab 아님)
  * `wouldWantClimb` = 4-OR (grab / isClimbHolding+sneak / autoLadder / autoVine) +
    4-AND 억제 (isSliding 제외 / !isHeadJumping / !wantCrawlNotClimb / !disabled)
  * `blocked` = `currentScreen != null && !currentScreen.allowUserInput` — GUI 입력 차단
    상태 (수평 충돌 아님). 1.21.1 `allowUserInput` 필드 제거됨.
- 의존 필드 `isFacedToLadder` / `isFacedToSolidVine` / `freeClimbAutoLadder/Vine` Config
  필드 모두 1.21.1 미이식 확인 (grep 결과 0건).
- **근사 이식 전략** (세션 63 B-5 패턴): 핵심 공식 `wantClimbHolding` 3-OR + `isClimbHolding`
  갱신 **구조만 1:1**, 의존 `wantClimb`/`blocked` 는 근사 + §7 B-16 근사 지점 등록.
- ClientState tickEssential isSlow 공식 (L918) 직후에 B-16 블록 이식:
  ```java
  {
      MinecraftClient mc16 = MinecraftClient.getInstance();
      boolean blocked = mc16.currentScreen != null;
      boolean wouldWantClimb16 =
              (grabPressed0 || (isClimbHolding && sneakPressedRaw))
              && (!isSliding || (grabPressed0 && player.input.movementForward > 0F))
              && !isHeadJumping && !wantCrawlNotClimb && !_disabled3a;
      boolean wantClimb16 = cfg0.freeClimb && cfg0.enabled && wouldWantClimb16;

      boolean wantClimbHolding =
              (isClimbHolding && sneakPressedRaw)
              || (isClimbing && blocked)
              || (wantClimb16 && !isSwimming_sm && !isDiving && !isCrawling
                      && (sneakPressedRaw || crawlToggled));
      isClimbHolding = wantClimbHolding && isClimbing;
  }
  ```
- §10 B-16 체크 + **서브 원자 B-16a/b/c 추가** (완전 이식 경로):
  * B-16a: `isFacedToLadder/isFacedToSolidVine` 헬퍼 Climber 이식
  * B-16b: `Config.freeClimbAutoLadder/freeClimbAutoVine` 필드 이식
  * B-16c: `wouldWantClimb` 2-OR → 4-OR 확장
- §7 B-16 근사 2건 등록 (wantClimb 2-OR / blocked allowUserInput 제거).
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 68)**:
- [근거] Agent WebFetch 로 원본 L2393/L2467-L2477/L2479-L2481/L2721-L2732 확보 ✓
- [근거] R-12.7 `isClimbHolding` (갱신 로직 없음 — 필드는 있으나 항상 false) 불일치
  §16 세션 33 #6/#7 재확인 ✓
- [대응] 3-OR + `isClimbHolding = wantClimbHolding && isClimbing` 공식 구조 1:1 ✓
- [분기] 3-OR 각 항 + `wouldWantClimb` 2-OR + 4-AND 억제 명시 ✓
- [상수] 없음
- [타이밍] isSlow 공식 직후 (원본 L2718 → L2721) 순서 복원 ✓
- [근사] **2건 근사 주석 + §7 등록** (wantClimb 2-OR / blocked 단순화) ✓
- [신규] B-16a/b/c 서브 원자 §10 추가 ✓
- [회귀] compileJava + compileClientJava 모두 ✓. `isClimbHolding` 이 이전 항상 false 에서
  실제 값 갱신으로 전환 — 소비처 (`isCrawlClimbing` 공식 `wasCrawling||isCrawlClimbing` 등)
  가 실제 값 참조. B-16c 완료 시 4-OR 확장으로 자동 등반 경로 활성.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing() 매 틱 호출 (B-14 세션 57)
- ✅ #6/#7 isClimbHolding/wantClimbHolding 갱신 (B-16 세션 68, 근사)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 자동 해소 세션 57)
- ⏳ #2~#5/#8~#12/#14 (B-17b2/B-18/B-19/B-20 등)

**Phase 2 진행 상황**: 36 원자 완료 (B-16 근사) + B-16a/b/c 서브 추가 → 잔여 ~7

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-20** (Standard/Simple Base Climb) — Config 분기 미이식 — 규모 중.
- **B-16a/b/c** (B-16 근사 해소 서브) — 자동 등반 경로 완전 이식 — 규모 중.

### 세션 69 — 2026-04-24 — B Phase 2 B-16a + B-16b + B-16c (자동 등반 완전 이식)

**진행한 작업**:
- **B-16b (Config 필드)**: Agent WebFetch 로 원본 Properties.java L171 `Unmodified → return true`
  기본값 확인. SmartMovingConfig 에 2개 필드 추가:
  * `freeClimbAutoLadder = true` (원본 `_freeClimbingAutoLaddder` Unmodified)
  * `freeClimbAutoVine   = true` (원본 `_freeClimbingAutoVine` Unmodified)
  헬퍼 2개 추가 (원본 SmartMovingClientConfig L57-L65 `_value && enabled` AND 패턴):
  * `isFreeClimbAutoLadderEnabled()` / `isFreeClimbAutoVineEnabled()`
- **B-16a (Climber 헬퍼)**: Agent 로 확보한 원본 SmartMovingBase L184-L192 이식.
  SmartMovingClimber 에 public static 2개 추가:
  * `isFacedToLadder(player, isSmall)` — 기존 `getOnLadderOrVine` 재사용 +
    `relevant && !vine` 조건 (ladder 만 필터 근사).
  * `isFacedToSolidVine(player, isSmall)` — `relevant && vine` 조건. 1.21.1
    `getOnLadderOrVine` L143-L144 에서 이미 "solid 뒤" 체크 내장되어 solidVine 조건 충족.
- **B-16c (4-OR 확장)**: ClientState B-16 블록 `wouldWantClimb16` 2-OR → 4-OR:
  ```java
  (grabPressed0
   || (isClimbHolding && sneakPressedRaw)
   || (cfg0.isFreeClimbAutoLadderEnabled()
           && SmartMovingClimber.isFacedToLadder(player, isClimbCrawling))
   || (cfg0.isFreeClimbAutoVineEnabled()
           && SmartMovingClimber.isFacedToSolidVine(player, isClimbCrawling)))
  ```
  원본 L2467-L2477 1:1 복원.
- **§7 근사 갱신**: B-16 (a) `wantClimb` 근사 **해소 완료** 기록. (b) `blocked` 근사만 잔존.
- **§10 B-16a/b/c 체크박스 완료**.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.
- **참고**: `isClimbCrawling` 현재 미이식 (B-18 대기, 필드는 이식됨 항상 false) → 헬퍼 호출
  시 `false` 전달 → B-18 이식 후 자동 활성.

**완료 전 검증 체크리스트 (세션 69)**:
- [근거] Agent WebFetch 로 SmartMovingBase L184-L192 (헬퍼) + SmartMovingConfig L119-L120
  (Config 필드) + SmartMovingClientConfig L57-L65 (헬퍼) + Properties.java L171 (기본값) 확보 ✓
- [근거] R-12.7 `isClimbHolding` 관련 #6/#7 불일치 + B-16 근사 2건 중 (a) 해소 계획 ✓
- [대응] 3개 서브 원자 원본 1:1 — 기본값/헬퍼 AND 패턴/4-OR 구조 동일 ✓
- [분기] 4-OR 각 항 명시 + 억제 조건 그대로 ✓
- [상수] `freeClimbAutoLadder/Vine = true` (원본 Unmodified 기본값) ✓
- [타이밍] 없음 (헬퍼 추가 + Config 필드 추가 + OR 확장)
- [근사] ladder/vine 필터는 `vine[0]` 플래그 기반 근사 (1.21.1 getOnLadderOrVine 에 필터
  파라미터 없음) — `handsVine=true` 판정은 L143-L144 solid 체크 내장이라 solidVine 조건
  충족. §7 에 근사 방식 명시 불필요 (원본 시멘틱과 동치).
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 자동 사다리/덩굴 진입 경로 복원 —
  사다리/덩굴을 바라보면 grab 없이도 자동 매달림. 게임플레이 체감 크게 개선.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing() 매 틱 호출 (B-14 세션 57)
- ✅ #6/#7 isClimbHolding/wantClimbHolding 갱신 (B-16 세션 68 근사 → B-16a/b/c 세션 69 완전)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 자동 해소 세션 57)
- ⏳ #2~#5/#8~#12/#14 (B-17b2/B-18/B-19/B-20 등)

**Phase 2 진행 상황**: 39 원자 완료 (B-16a + B-16b + B-16c 3개 추가) / 잔여 ~4

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-20** (Standard/Simple Base Climb) — Config 분기 미이식 — 규모 중.
- **B-18** (isClimbCrawling 메인 공식 + 카운터) — 현재 isClimbCrawling 항상 false. B-16c 효과 증폭.

### 세션 70 — 2026-04-24 — B Phase 2 B-41 (wantCrawlNotClimb 갱신 공식)

**진행한 작업**:
- ClientState tickEssential IMPL-01 종료 + B-34 (flying 점프) 뒤, B-25 IMPL-02 앞에
  원본 L2451-L2463 이식. 단일 4-AND 복합 표현식:
  ```java
  wantCrawlNotClimb =
          (wantCrawlNotClimb || (grabJustPressed && !wasCrawling))
          && SmartMovingKeys.grab.isPressed()
          && player.input.movementForward > 0F
          && isCrawling
          && player.horizontalCollision;
  ```
- 의존 전수 이식 확인:
  * `wantCrawlNotClimb` 이전 값 — B-31b 세션 38 필드 이식 ✓
  * `grabJustPressed` — B-46 세션 66 필드 이식 ✓
  * `wasCrawling` — B-31a 세션 41 필드 이식 ✓
  * `grab.isPressed()` / `movementForward` / `isCrawling` / `horizontalCollision` — vanilla API ✓
- 주석에 원본 라인 + 의미 (크롤 중 등반 억제 플래그) + B-16c 의 `!wantCrawlNotClimb`
  억제 조건 연동 명시.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 70)**:
- [근거] 원본 L2451-L2463 Agent WebFetch (세션 68) 로 확보 ✓
- [근거] R-14.10 #12 `wantCrawlNotClimb 필드+갱신 [누락]` §16 세션 35 ✓
- [대응] 4-AND + OR-조합 조건 원본 1:1 ✓
- [분기] OR 2-gate (이전값 또는 grabStart + !wasCrawling) + 4-AND 명시 ✓
- [상수] 없음
- [타이밍] IMPL-01 종료 + B-34 뒤 — 원본 L2441 isCrawling 공식 이후 L2451 위치와 의미
  등가 (isCrawling 최종값 확정 시점). `wasCrawling` 은 L761 이전 틱 저장 — 원본 L2441
  `wasCrawling=isCrawling` 전 시점과 동일 값. 1.21.1 IMPL-01 진입/해제 이원화 구조
  하에서 결과적 동치 ✓
- [근사] 없음 — 1:1 이식
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. wantCrawlNotClimb 가 이제 실제 조건 기반
  갱신 — B-16c `wouldWantClimb` 억제 조건 활성화. 크롤 중 전진+grab+수평 충돌 상황에서
  등반 자동 진입 차단 (원본 의도).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #4 L2449-L2450 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #7 L986 wall 오르기 crawl 진입 (B-37 세션 62)
- ✅ #8 L1170 handleCeilingClimbing 진입 isCrawling=false (B-38 세션 58)
- ✅ #12 wantCrawlNotClimb 필드+갱신 (B-31b 세션 38 + B-41 세션 70)
- ⏳ #1~#3/#5/#6/#9~#11/#13~#15

**Phase 2 진행 상황**: 40 원자 완료 / 잔여 ~3

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 의존.
- **B-20** (Standard/Simple Base Climb) — Config 분기 미이식 — 규모 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존 (hasClimbCrawlGap/climbIntoCount).

### 세션 71 — 2026-04-24 — B Phase 2 B-10d (isLevitating 공식 이식)

**진행한 작업**:
- `SmartMovingSwimmer.updateSwimState` Config 게이트 (L103-L110) 뒤, ticks 증분 앞에
  원본 L474 + L505 이식:
  ```java
  boolean diveUp16   = player.input.jumping;
  boolean diveDown16 = player.isSneaking() && cfg.diveDownOnSneak;
  sm.isLevitating = sm.isDiving
          && !diveUp16 && !diveDown16
          && player.input.movementSideways == 0F
          && player.input.movementForward == 0F;
  ```
  원본 `diveUp = isp.getIsJumpingField()` (L468) / `diveDown = sneak && Config._diveDownOnSneak`
  (L469) 지역 변수 + `levitating = diving && !diveUp && !diveDown && moveStrafe==0 && moveForward==0`
  (L474) + `isLevitating = levitating` (L505) 1:1.
- **ClientState `sendStatePacket` L1775 강제 `isLevitating = false` 제거** — 원본 주석
  "로프 미구현" 이유로 덮어쓰던 것이 updateSwimState 갱신값을 무효화. 원본 L505 의 주요
  용도는 "수중 정적 자세" 이므로 계산값 유지. 주석 갱신.
- 필드 선언 L323 주석 갱신 — "로프 등 부양" → "원본 L505 수중 정적 자세" 의미 명시.
- **B-43 세션 56 연결**: R-09 블록 종료부 `wasLevitating = isLevitating` 저장이 이제 실제
  isLevitating 값 참조 (이전엔 항상 false 였음).
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 71)**:
- [근거] 원본 L468-L469/L474/L505 research/.../SmartMovingSelf.md L3106-L3119 (R-11.8) +
  L468-L469 (R-11) 확보 ✓
- [근거] R-11.12 불일치 #11 `isLevitating 필드 미이식` 확정 §16 세션 32 ✓
- [대응] diveUp/diveDown 지역 변수 + levitating 5-AND 공식 원본 1:1 ✓
- [분기] 5-AND 전체 항 명시 (isDiving / !diveUp / !diveDown / sideways=0 / forward=0) ✓
- [상수] 없음 (float 0F 비교)
- [타이밍] updateSwimState Config 게이트 뒤 (isDiving 확정 후) — 원본 L505 위치와
  의미적 등가 (isDiving 확정 직후) ✓
- [근사] 없음 — 1:1 이식. `isp.getIsJumpingField()` → `player.input.jumping` 표면 매핑만.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. sendStatePacket 강제 false 제거로
  isLevitating 네트워크 전송값이 실제 수중 정적 자세 반영. 애니메이션 (isDive Quarter-Sixteenth
  수직각) 에서 정상 참조. B-43 wasLevitating 저장이 실제 값 참조 (B-43 세션 56 효과 증폭).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-11 A-2 불일치 현황**:
- ✅ #2 Config 게이트 누락 (B-8 세션 67)
- ✅ #3 isClimbCrawling 조건 누락 (B-6 세션 60)
- ✅ #9 waterMovementTicks dipping 증분 오역 (B-12 세션 64)
- ✅ #11 isLevitating 필드+공식 이식 (B-10d 세션 71, 필드는 L179 기존)
- ✅ #12 crawl↔swim 전환 isSliding (B-13 세션 61) — 기존 R-11 번호 재확인
- ⏳ #1/#4~#8/#10 (대부분 B-9 메인 분류 재작성 범위)

**Phase 2 진행 상황**: 41 원자 완료 / 잔여 ~2

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 규모 중간.
- **B-17b2** (else if(wasCrawlClimbing) 복합 전환 3분기) — wantClimbUp/Down 필드 승격 선행.
- **B-20** (Standard/Simple Base Climb) — Climber 구조 변경 — 규모 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.

### 세션 72 — 2026-04-24 — B Phase 2 B-17b2-pre (wantClimb/Up/Down 필드 승격)

**진행한 작업**:
- §1 진행 상황 재정비 — 세션 71 "잔여 ~2" 는 오집계. §10 `- [ ] B-` grep 결과 잔여 15개
  (B-7/B-9/B-11/B-17b2/B-18/B-19/B-20/B-26/B-33/B-35/B-36/B-39/B-42/B-44b/B-44c). 대부분
  중간-대형 규모 의존 엮임. 후속 원자는 서브 분해 + 근사 이식 전략 필요.
- **B-17b2 선행 서브 원자 "B-17b2-pre"** 신설 진행:
  * ClientState 필드 3개 승격: `wantClimb` / `wantClimbUp` / `wantClimbDown` public boolean.
    주석에 원본 L2479/L2491-L2500 공식 + 연산자 우선순위 주의 명시.
  * B-16 블록의 지역 `wantClimb16` → `this.wantClimb` 필드 할당으로 전환.
  * tickEssential B-16 블록 직후에 `wantClimbUp/wantClimbDown` 계산 블록 추가. 원본
    L2491-L2500 1:1:
    ```java
    wantClimbUp =
            (wantClimb && forward17 > 0F)
            || ((isVineAnyClimbing && jumpPressed17
                    && !(sneakPressedRaw && isFacedVine17))
                && (!isCrawling || hCollision17)
                && (!isSliding  || hCollision17));
    wantClimbDown = wantClimb && forward17 <= 0F && !wantCrawl;
    ```
    연산자 우선순위 (&& > ||) 적용 — 전진 등반 OR (덩굴+점프 + 크롤/슬라이딩 수평충돌 조건).
  * resetState 리셋 3건 추가.
- Climber 지역 `wantClimbUp`/`wantClimbDown` (L374-L375) 는 그대로 유지 — 추후 B-17b2 본체
  이식 시 Climber 지역 변수를 필드 참조로 전환할 계획 (B-17b2 의 `else if (wasCrawlClimbing)`
  블록이 ClientState 에 있어야 하므로 필드 기반 통일).
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 72)**:
- [근거] 원본 L2479/L2491-L2500 Agent WebFetch (세션 68) 로 확보 ✓
- [근거] R-12.5/R-12.6 `wantClimb`/`wantClimbUp/Down` 필드 승격 필요성 확정 ✓
- [대응] 3개 필드 + 승격 코드 + 계산 블록 원본 1:1 ✓
- [분기] wantClimbUp 의 OR-연결 2분기 + 각 AND 조건 명시 + wantClimbDown 3-AND ✓
- [상수] 없음 (float 0F 비교)
- [타이밍] B-16 블록 직후 — 원본 L2479 wantClimb 계산 뒤 L2491 wantClimbUp 순서 복원 ✓
- [근사] 없음 — 1:1 이식. `jumpButton.Pressed` → `_jumpPressed3a` / `sneakButton.Pressed` →
  `sneakPressedRaw` / `isp.isCollidedHorizontally` → `player.horizontalCollision` 표면 매핑만.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. wantClimbUp/Down 이 이전 항상 false →
  실제 조건 기반 갱신. 기존 Climber L374-L375 지역 변수는 영향 없음 (독립 계산).
  B-17b2 본체에서 이 필드 활용 가능.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**Phase 2 진행 상황**: 42 원자 완료 (B-17b2-pre 서브 추가) / 잔여 15 원자

**다음 작업 권고**:
- **B-17b2** 본체 — else if(wasCrawlClimbing) 3분기 이식. ClientState 에 배치. wantClimbUp/
  Down 필드 준비 완료. move() API / crawlStandUpBottom 근사 필요.
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 중간.
- **B-20** (Standard/Simple Base Climb) — Climber 구조 변경. 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존 (hasClimbCrawlGap/climbIntoCount).

### 세션 73 — 2026-04-24 — B Phase 2 B-17b2 (else if wasCrawlClimbing 3분기)

**진행한 작업**:
- Agent WebFetch 로 원본 L2736-L2784 본문 확보 (기존 리서치는 요약만). 핵심 식별:
  * 분기 1: `!isClimbing` → `toCrawling()` 메서드 호출 + 바닥 스냅
  * 분기 2: `moveForward <= 0F` → 로컬 toCrawling 반영 + wantClimb* 리셋 + 조건부 높이 복귀
  * 분기 3: `!toCrawling` → heightOffset 리셋 + 위쪽 스냅
- ClientState B-17b1 (L1332 `}`) 직후 `else if (_wasCrawlClimbing17)` 블록 이식.
  `_wasCrawlClimbing17` 은 L1307 공식 직전 저장된 이전 틱 값.
- `player.move(MovementType.SELF, Vec3d)` 로 이동 — collision + boundingBox 갱신 자동 처리.
  `MovementType`/`Vec3d` import 2건 추가.
- `resetHeightOffset()` → `heightOffset = 0F` 필드 대입으로 근사 (B-17b1 동일 패턴).
- `toCrawling()` 메서드 (B-40 세션 44) / wantClimbUp/Down 필드 (B-17b2-pre 세션 72) 의존
  모두 충족.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 73)**:
- [근거] Agent WebFetch 로 원본 L2736-L2784 원문 확보 (Self.java) ✓
- [근거] R-12.10 불일치 #3 `isCrawlClimbing 메인 공식 + 전환 3분기 미이식` 확정 ✓
- [대응] 3분기 조건 + 본문 (toCrawling/move/heightOffset 리셋) 원본 1:1 ✓
- [분기] 3분기 각각의 순서 (`!isClimbing` → `moveForward<=0` → `!toCrawling`) 명시 ✓
- [상수] `toCrawlingLocal ? 0F : 1F` / `-minY + Math.floor(minY)` / `Math.ceil(minY) - minY` 원본 동일 ✓
- [타이밍] B-17b1 canStandUp 분기 바로 뒤 `else if` — 원본 순서 복원 ✓
- [근사] `resetHeightOffset()` → `heightOffset = 0F` 근사 (B-17b1 동일 패턴). 원본 메서드는
  bounding box 조작까지 포함하나 1.21.1 은 필드 대입 + `player.move` 로 분리 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. isCrawlClimbing 이 B-18 미이식으로 여전히
  항상 false — else if (_wasCrawlClimbing17) 분기도 발동 가능성 낮음. B-18 이식 후 활성화.
  현재 구조 기반 시 분기 동작 예상 정상.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing() (B-14 세션 57)
- ✅ #3 isCrawlClimbing 메인 공식 (B-17a 세션 47) + canStandUp 분기 (B-17b1 세션 48)
      + else if 3분기 (B-17b2 세션 73)
- ✅ #6/#7 isClimbHolding/wantClimbHolding (B-16 세션 68/69)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 자동 해소 세션 57)
- ⏳ #2/#4/#5/#8/#9/#10/#11/#12/#14 (B-18/B-19/B-20 등)

**Phase 2 진행 상황**: 43 원자 완료 / 잔여 14 원자

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 중간.
- **B-20** (Standard/Simple Base Climb) — Climber 구조 변경. 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존 (hasClimbCrawlGap/climbIntoCount).
- **B-35** (wasCrawling↔isCrawling 전환 후처리) — crawlStandUpBottom 근사 필요.

### 세션 74 — 2026-04-24 — B Phase 2 B-35 (전환 후처리 근사 이식)

**진행한 작업**:
- ClientState tickEssential B-17 블록 (L1370 `}`) 뒤 + R-09 블록 앞에 원본 L2822-L2836
  wasCrawling↔isCrawling 전환 후처리 2분기 이식:
  ```java
  // 분기 A: wasCrawling && !isCrawling && !initializeCrawling && !flying
  if (wasCrawling && !isCrawling && !initializeCrawling
          && !player.getAbilities().flying) {
      heightOffset = 0F;  // resetHeightOffset 근사
      // move(0, crawlStandUpBottom - minY, 0) 이동량 생략 (근사)
  }
  // 분기 B: (isCrawling && !wasCrawling) || initializeCrawling
  if ((isCrawling && !wasCrawling) || initializeCrawling) {
      heightOffset = -1F;
      player.move(MovementType.SELF, new Vec3d(0, -1D, 0));
      if (initializeCrawling) toCrawling();
  }
  ```
- §7 **B-35 근사 등록**: `crawlStandUpBottom = getMaxPlayerSolidBetween(minY-1, minY, ...)`
  정밀 AABB 스캔 미이식 → `crawlStandUpBottom ≈ minY` 근사로 이동량 0. 공중 크롤 해제
  정확도 낮으나 드문 상황.
- 의존 전수 충족: B-31a wasCrawling / B-31c initializeCrawling 필드 / B-40 toCrawling() /
  세션 73 MovementType/Vec3d import 재사용.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 74)**:
- [근거] 원본 L2822-L2836 research/.../SmartMovingSelf.md + R-14.9 확보 ✓
- [근거] R-14.10 #10 `landMotionPost` / #11 `crawl 전환 후처리` 불일치 §16 세션 35 ✓
- [대응] 2분기 조건 + 본문 원본 1:1 (이동량 근사 제외) ✓
- [분기] 분기 A 4-AND + 분기 B 2-OR 명시 ✓
- [상수] `-1D` / `-1F` / `0F` 원본 동일 ✓
- [타이밍] B-17 뒤 + R-09 앞 — 원본 L2822 (L2737 isCrawlClimbing 뒤) 순서 복원 ✓
- [근사] `crawlStandUpBottom - minY ≈ 0` 명시 + §7 B-35 근사 등록 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. wasCrawling→!isCrawling 전환 시
  heightOffset 리셋 복원 (B-32 세션 44 canCrawl 이식으로 실제 전환 발생 가능). 진입 시
  heightOffset=-1 + 1블록 하강 경로 복원. toCrawling() 호출은 initializeCrawling 필드가
  B-31c 공식 미이식으로 항상 false 라 발동 안 함 (B-35 자체 기능엔 영향 없음).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #4 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #7 wall 오르기 crawl 진입 (B-37 세션 62)
- ✅ #8 handleCeilingClimbing isCrawling=false (B-38 세션 58)
- ✅ #10/#11 전환 후처리 분기 (B-35 세션 74, 근사)
- ✅ #12 wantCrawlNotClimb (B-41 세션 70)
- ⏳ #1~#3/#5/#6/#9/#13~#15

**Phase 2 진행 상황**: 44 원자 완료 / 잔여 13 원자

**다음 작업 권고**:
- **B-26** (isSliding 부수 동작) — Config.SlideDown + Jumper.tryJump 시그니처 확장. 중간.
- **B-20** (Standard/Simple Base Climb) — Climber 구조 변경. 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.
- **B-36** (grab.StartPressed 3분기) — wouldWantClimb 필드 승격 필요.

### 세션 75 — 2026-04-24 — B Phase 2 B-26 (isSliding 부수 동작 근사 이식)

**진행한 작업**:
- ClientState tickEssential B-25 IMPL-02 직접 진입 블록 성공 분기에 원본 L2555-L2557
  부수 동작 3건 추가 (기존 필드 세팅 3건은 B-25 세션 55 이식):
  ```java
  heightOffset = -1F;                                     // 원본 L2555
  player.move(MovementType.SELF, new Vec3d(0, -1D, 0));   // 원본 L2556
  // tryJump(Config.SlideDown, false, wasRunning, null) 생략 (§7 B-26 근사)
  isSliding = true;                                        // 원본 L2558 (B-25)
  isHeadJumping = false;                                   // 원본 L2559 (B-25)
  isAerodynamic = false;                                   // 원본 L2560 (B-25)
  ```
- §7 **B-26 근사 등록**: tryJump(Config.SlideDown) 호출 생략 — Jumper.SLIDE_DOWN 상수 +
  SlideDown 전용 속도 공식 (원본 tryJump 내부 분기) 미이식. isFromRunning 파라미터 영향
  생략. gameplay 영향: SlideDown 전용 하강 점프 추진 모션 누락 — 주로 시각/이펙트.
- `MovementType`/`Vec3d` import 는 세션 73 B-17b2 에서 이미 추가됨.
- 주석 정리: B-25 블록 헤더에서 "B-26 별도 원자" 표기 제거, 세션 75 완료 사실 반영.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 75)**:
- [근거] 원본 L2555-L2560 research/.../SmartMovingSelf.md L3728-L3742 (R-13.4) 확보 ✓
- [근거] R-13.9 #4 L2555-L2557 직접 진입 부수 동작 [누락] 확정 §16 세션 34 ✓
- [대응] heightOffset + move 원본 1:1, tryJump 근사 명시 ✓
- [분기] 없음 (순차 실행)
- [상수] `-1F`/`-1D` 원본 동일 ✓
- [타이밍] B-25 진입 6-AND 조건 성공 시 — 원본 L2553 조건 뒤 L2555 순서 복원 ✓
- [근사] **tryJump(SlideDown) 생략 + §7 B-26 등록 + 주석 명시** ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 슬라이딩 진입 시 heightOffset=-1 + 1블록
  하강 경로 복원. SlideDown 추진 누락으로 슬라이딩 수평 속도가 기존보다 작을 수 있음 —
  §7 등록으로 향후 B-26b 서브 원자 검토 가능.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-13 A-4 불일치 현황**:
- ✅ #3 직접 진입 6-AND 조건 (B-25 세션 55)
- ✅ #4 직접 진입 부수 동작 (B-26 세션 75, 근사)
- ✅ #5 isHeadJumping=false (B-25 세션 55)
- ⏳ #1/#2/#6/#7~#13 (이전 완료 항목 중 수치 확인 필요)

**Phase 2 진행 상황**: 45 원자 완료 / 잔여 12 원자

**다음 작업 권고**:
- **B-20** (Standard/Simple Base Climb) — Climber 구조 변경. 중.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.
- **B-36** (grab.StartPressed 3분기) — wouldWantClimb 필드 승격 필요.
- **B-39** (landMotionPost 3분기) — crawlStandUpBottom 근사 필요.

### 세션 76 — 2026-04-24 — B Phase 2 B-20 (Standard Base Climb isClimbing 오염 해소)

**진행한 작업**:
- `SmartMovingClimber.handleClimbing` L305-L313 Standard 분기에서 `setOnlyShouldClimbSpeed`
  → `setShouldClimbSpeed` 한 단어 교체.
  * `setOnlyShouldClimbSpeed` (L273): `if (relevant) sm.isClimbing = true;` 내장 — Standard
    모드에서도 isClimbing 잉여 설정 → **오역** (원본은 Standard 에서 vanilla ladder 물리 위임이라
    isClimbing 설정 안 함).
  * `setShouldClimbSpeed`: motionY 보정만 수행, isClimbing 안 건드림.
- 속도 보정 (motionY = 0.2 × combinedFactor) 유지 — 세션 25 B-1 User 배율 주입 효과 보존.
- §7 **B-20 근사 등록**: 원본 `isOnLadderOrVine && isCollidedHorizontally` 조건 판정 생략 —
  Standard 분기 진입 자체를 ladder/vine 접촉으로 간주. vanilla ladder 물리가 진입을 조건부로
  처리하므로 실용 등가.
- **Simple/Smart Base Climb 분기 (L825-L894) 는 미이식 유지** — 별도 포커스 후보 (옵션
  모드 전체 이식 규모 큼). B-20 본 원자는 "Standard 모드 isClimbing 오염 해소" 에 한정.
- 주석 업데이트: 원본 L820-L823 인용 + 근사 명시 + isClimbing 오염 해소 근거 상세 기록.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 76)**:
- [근거] 원본 L820-L823 Standard Base Climb + SmartMovingSelf.md R-12.3 확보 ✓
- [근거] setOnlyShouldClimbSpeed 내부 `isClimbing=true` 설정 L273 재확인 ✓
- [대응] 한 단어 교체로 원본 의도 (Standard 에서 isClimbing 설정 안 함) 복원 ✓
- [분기] 없음 (단일 호출)
- [상수] `FAST_UP_MOTION = 0.2D` (원본 L712 `0.2` 일치) 유지 ✓
- [타이밍] 기존 위치 그대로 (exhaustion 체크 뒤 Standard 분기 진입부) ✓
- [근사] `isOnLadderOrVine && isCollidedHorizontally` 조건 판정 생략 — §7 B-20 근사 등록 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. Standard 모드에서 `isClimbing=true` 잉여
  설정 제거 → 포커스 #2 상태 플래그 정확성 개선. 속도 보정 유지로 세션 25 B-1 효과 보존.
  소비처 (isCrawlClimbing 공식 등) 에서 Standard 모드 접촉 시 등반 상태 오분류 해소.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing() (B-14 세션 57)
- ✅ #3 isCrawlClimbing 메인 공식 + 전환 (B-17a/b1/b2 세션 47/48/73)
- ✅ #6/#7 isClimbHolding/wantClimbHolding (B-16 세션 68/69)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 자동 해소 세션 57)
- ✅ Standard Base Climb isClimbing 오염 해소 (B-20 세션 76, 부분)
- ⏳ #2/#4/#5/#8~#12/#14 (B-18/B-19 등)

**Phase 2 진행 상황**: 46 원자 완료 / 잔여 11 원자

**다음 작업 권고**:
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존 (hasClimbCrawlGap/climbIntoCount).
- **B-36** (grab.StartPressed 3분기) — wouldWantClimb 필드 승격 필요.
- **B-39** (landMotionPost 3분기 isSlow) — crawlStandUpBottom 근사 필요.
- **B-44b** (wasCrawling 저장 시점 이동) — B-33 함께 조정 예정이나 단독 문서 정리 가능.

### 세션 77 — 2026-04-24 — B Phase 2 B-36-pre (wouldWantClimb/Crawl 필드 승격)

**진행한 작업**:
- B-36 본 원자 (grab.StartPressed 3분기 이식) 선행 서브 원자. 의존 필드 2개 승격:
  * `wouldWantClimb` public boolean 신설 — 원본 L2467-L2477 4-OR + 억제 조건. B-16c 세션 69
    에서 지역 `wouldWantClimb16` 로 이미 이식 → 이번에 필드 승격 + `this.wouldWantClimb`
    할당으로 전환.
  * `wouldWantCrawl` public boolean 신설 — 원본 L2419-L2430 4-OR. pre-compute 블록 L855-L861
    지역 `wouldWantCrawl_` → `this.wouldWantCrawl` 필드 할당. `_` 별칭 하위 호환 유지.
- resetState 리셋 2건 추가.
- 주석에 원본 라인 + B-36 본체 이식 대기 명시.
- 잔여 원자 카운트 증가 없음 (B-36 본체는 여전히 미완료). 필드 승격만으로 47 원자.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.
- **B-36 본체 대기**: 원본 L2838-L2861 본문이 리서치에 없어 Agent WebFetch 필요. 3분기
  조건 (a/b/c) + SwimCrawlWater 경계 상수 (BottomBorder/MediumBorder) 추가 확보 후 다음
  세션에서 진행.

**완료 전 검증 체크리스트 (세션 77)**:
- [근거] 원본 L2467-L2477 (wouldWantClimb 4-OR) Agent 세션 68 에서 확보 ✓
- [근거] 원본 L2419-L2430 (wouldWantCrawl 4-OR) 리서치 L1793-L1802 확인 ✓
- [근거] R-14.3 tickEssential 크롤 판정 블록 + R-12.7 wouldWantClimb 호출 맥락 확정 ✓
- [대응] 기존 지역 변수 계산식 그대로 유지, 필드 할당만 추가 ✓
- [분기] 없음 (승격 작업)
- [상수] 없음
- [타이밍] 기존 계산 위치 유지 (pre-compute 블록 L855 + B-16c 블록 L964) ✓
- [근사] 없음 — 필드 승격 작업
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 기존 지역 변수 참조점 (wantClimb 계산
  L942 의 `wouldWantClimb16`) 을 필드 참조로 전환 — 의미 동일. 다른 호출처 없어 영향 최소.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**Phase 2 진행 상황**: 47 원자 완료 (B-36-pre 서브 추가) / 잔여 11 원자

**다음 작업 권고**:
- **B-36 본체** — Agent WebFetch 로 L2838-L2861 + SwimCrawlWater 상수 확보 후 3분기 이식.
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.
- **B-39** (landMotionPost 3분기 isSlow) — crawlStandUpBottom 근사 필요.
- **B-42** — "별도 포커스 후보" 공식 분리 문서 작업.

### 세션 78 — 2026-04-24 — B Phase 2 B-36 본체 (grab.StartPressed 3분기)

**진행한 작업**:
- Agent WebFetch 로 원본 L2835-L2875 본문 + SmartMovingContext L35-L52 상수 4개 확보:
  * 원본 L2839-L2862 구조 = `if / else if { if / else }` 중괄호 없는 체이닝.
  * `SwimCrawlWaterMediumBorder = 0.6F` / `SwimCrawlWaterBottomBorder = 0.55F` 값 확정.
- ClientState tickEssential B-35 분기 B 직후 (L1421) + R-09 블록 앞에 3분기 이식 (중괄호
  명시화):
  ```java
  if (grabJustPressed) {
      if (isShallowDiveOrSwim && wouldWantClimb) {
          // (a) 얕은 물 swim/dive → walking
          heightOffset = 0F;
          if (_jumpPressed3a) isStillSwimmingJump = true;
      } else if (isDipping && wouldWantCrawl && dippingDepth >= 0.55F) {
          if (dippingDepth >= 0.6F) {
              // (b) dipping → swim/dive
              heightOffset = -1F;
              player.move(SELF, new Vec3d(0, -1.6F + dippingDepth, 0));
              isCrawling = false;
          } else {
              // (c) dipping → 얕은 물 crawl
              heightOffset = -1F;
              player.move(SELF, new Vec3d(0, -1D, 0));
              wasCrawling = toCrawling();
          }
      }
  }
  ```
- §7 **B-36 근사 등록**: 분기 (a) `getMaxPlayerSolidBetween(minY, maxY, 0) - minY` 이동량
  — AABB 정밀 스캔 미이식 → `0` 근사, player.move 생략. B-10a isShallowDiveOrSwim 공식
  미이식으로 분기 (a) 자체 항상 비활성 (필드 false).
- 의존 전수 충족: B-36-pre (wouldWantClimb/wouldWantCrawl 세션 77) / B-10c isStillSwimmingJump
  필드 / B-40 toCrawling() / B-46 grabJustPressed.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 78)**:
- [근거] Agent WebFetch 로 원본 L2835-L2875 본문 확보 ✓
- [근거] SmartMovingContext.java L35-L52 상수 4개 값 확인 ✓
- [근거] R-14.10 #11 L2838-L2861 grab.StartPressed 3분기 [누락] §16 세션 35 ✓
- [대응] 3분기 중첩 구조 중괄호 명시화 + 조건/본문 원본 1:1 (분기 (a) 이동량 제외) ✓
- [분기] (a) / (b) dipping+MediumBorder 이상 / (c) dipping+MediumBorder 미만 3갈래 ✓
- [상수] `0.55F` / `0.6F` / `-1.6F` / `-1D` 원본 동일 ✓
- [타이밍] B-35 뒤 + R-09 앞 — 원본 L2839 가 L2822 (B-35) 뒤 L2864 (R-09 전) 와 동일 순서 ✓
- [근사] 분기 (a) 이동량 근사 + §7 B-36 등록 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. grab 엣지 입력 시 물속 상태 전환 경로 복원.
  (a) 분기는 B-10a isShallowDiveOrSwim 공식 이식 후 자동 활성 (현재 항상 false). (b)/(c)
  분기는 dipping + wouldWantCrawl + depth 조건 만족 시 즉시 발동 가능.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #4 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #7 wall 오르기 crawl 진입 (B-37 세션 62)
- ✅ #8 handleCeilingClimbing isCrawling=false (B-38 세션 58)
- ✅ #10/#11 전환 후처리 (B-35 세션 74) + grab.StartPressed 3분기 (B-36 세션 78)
- ✅ #12 wantCrawlNotClimb (B-41 세션 70)
- ⏳ #1~#3/#5/#6/#9/#13~#15

**Phase 2 진행 상황**: 48 원자 완료 / 잔여 10 원자

**다음 작업 권고**:
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.
- **B-39** (landMotionPost 3분기 isSlow) — crawlStandUpBottom 근사 필요.
- **B-42** — "별도 포커스 후보" 공식 분리 문서 작업.
- **B-44b** (wasCrawling 저장 시점 이동) — B-33 함께 조정 예정이나 단독 문서 정리 가능.

### 세션 79 — 2026-04-24 — B Phase 2 B-42 (별도 포커스 분리)

**진행한 작업**:
- B-42 AABB 정밀 근사 개선을 포커스 #2 범위 외로 결정 — `focus_14_aabb_precision.md` 로
  분리 (파일 실제 생성은 후속 작업 시점, 현재는 참조만 등록).
- focus_02 §17 잔여/후속 섹션에 분리 근거 + §7 근사 지점 전수 참조 등록 (B-5/B-16 blocked/
  B-20/B-26/B-35/B-36 등 — 모두 AABB 정밀 스캔 미이식에서 파생).
- focus_02 §10 B-42 체크박스 [x] 해소. "현재 근사 유지" 결정으로 추가 코드 변경 없음.
- playtest_fixes.md 에 포커스 #2 진행 중 메모 추가 (focus_11/12/13 분리 패턴 동일).
- Simple/Smart Base Climb (B-20 세션 76 의 Standard 모드 외 부분) 도 후속 포커스 후보로
  §17 에 명시.
- 빌드 검증 불필요 (코드 변경 없음).

**완료 전 검증 체크리스트 (세션 79)**:
- [근거] §7 에 B-5/B-16/B-20/B-26/B-35/B-36 근사 지점 전수 등록 확인 ✓
- [근거] focus_11/12/13 후속 포커스 분리 패턴 (playtest_fixes.md 참조) 재사용 ✓
- [대응] §10 체크박스 해소 + §17 분리 근거 기록 + playtest_fixes.md 참조 추가 ✓
- [분기] 없음 (문서만)
- [상수] 없음
- [타이밍] 없음 (코드 변경 없음)
- [근사] **B-42 자체가 "현재 근사 유지" 결정 원자** — §7 전수 등록으로 추적성 확보 ✓
- [신규] 없음 (기존 근사 지점들을 한곳에 정리)
- [회귀] 없음 (문서만)
- [빌드] 코드 변경 없음

**Phase 2 진행 상황**: 49 원자 완료 / 잔여 9 원자

**다음 작업 권고**:
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존 (hasClimbCrawlGap/climbIntoCount).
- **B-39** (landMotionPost 3분기 isSlow) — crawlStandUpBottom 근사로 실효성 낮음.
- **B-44b** (wasCrawling 저장 시점 이동) — B-33 동시 조정 필요.
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing/isInLiquid/isLavaLikeWaterEnabled 의존.

### 세션 80 — 2026-04-24 — B Phase 2 B-39 (landMotionPost 3분기 구조 복원 근사)

**진행한 작업**:
- `ClientState.fromSwimmingOrDiving` 에 원본 L1392-L1403 3분기 구조 복원.
  기존 `canStandUp=false` (1) + `hasLiquidCeiling=true` (2) 분기 뒤에 `else` 분기 신설.
- 본문은 no-op 주석만 — `crawlStandUpBottom` AABB 정밀 스캔 미이식이므로 원본 조건
  `isSlow && crawlStandUpBottom > minY + 0.5D` 를 근사로 `false` 처리. `move(0,
  crawlStandUpBottom - minY, 0)` 이동량도 `0` 근사 생략.
- 현재 동작 유지 + 구조 복원으로 focus_14 AABB 정밀 완료 시 자동 활성 경로 확보. 주석에
  원본 원문 + 근사 사유 + focus_14 완료 시 활성 지시 명시.
- §7 B-39 근사 등록. §10 체크박스 해소. §15 세션 80 로그.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공 (주석만 추가).

**완료 전 검증 체크리스트 (세션 80)**:
- [근거] 원본 L1392-L1403 research/.../SmartMovingSelf.md L3211-L3229 (R-11.11) 확보 ✓
- [근거] R-14.10 #9 `landMotionPost 3분기 중 1개 누락` §16 세션 35 확정 ✓
- [대응] else 분기 추가 (원본 3분기 구조 복원) — 본문은 주석만 (근사) ✓
- [분기] else 진입 자체는 가능 (canStandUp 성공 + !hasLiquidCeiling 시) ✓
- [상수] 없음 (0.5D 는 주석에만 명시)
- [타이밍] 기존 fromSwimmingOrDiving 본체 블록 마지막 else — 원본 L1392 else if 순서 동일 ✓
- [근사] **본문 전체 근사 (crawlStandUpBottom AABB 미이식)** + §7 B-39 등록 ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. else 분기 본문이 no-op 이므로 기존 동작
  완전 유지. 구조 복원만으로 focus_14 완료 시 자동 활성화 경로 확보.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**Phase 2 진행 상황**: 50 원자 완료 / 잔여 8 원자

**다음 작업 권고**:
- **B-18** (isClimbCrawling 공식 + 카운터) — 대규모 의존.
- **B-44b** (wasCrawling 저장 시점 이동) — B-33 동시 조정 필요 (단독 무의미).
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing 등 의존 확인 필요.
- **B-11** (얕은 물 특수 분기) — B-9 메인 분류 재작성 범위.

### 세션 81 — 2026-04-24 — B Phase 2 B-18-pre + B-18 (isClimbCrawling 공식 + 카운터)

**진행한 작업**:
- **B-18-pre**: ClientState `wantClimbHolding` public 필드 신설 (isClimbHolding 근처).
  B-16 블록 지역 `boolean wantClimbHolding` → `this.wantClimbHolding` 필드 할당 전환.
  resetState 리셋 추가.
- **B-18 본체**: ClientState tickEssential B-17 블록 뒤 + B-35 앞 (원본 L2786 순서) 에
  원본 L2786-L2820 본체 이식:
  ```java
  boolean wasClimbCrawling = isClimbCrawling;
  boolean needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding);
  boolean canClimbCrawling = wantClimbHolding && wantClimbUp;
  if (climbIntoCount > 1) climbIntoCount--;
  else if (isClimbCrawling && !needClimbCrawling && count==0) climbIntoCount = 6;
  isClimbCrawling = canClimbCrawling && ((needClimbCrawling && count==0) || count>1);
  if (isClimbCrawling && !wasClimbCrawling) {
      heightOffset = -1F;
      player.move(SELF, new Vec3d(0, 0.05, 0));
  } else if (!isClimbCrawling && wasClimbCrawling) {
      climbIntoCount = 0;
      // TODO (B-18b): mustCrawl/sneak 상황별 crawl 전환 + resetHeightOffset
  }
  ```
- §7 **B-18 근사 등록**: 진입 엣지 `isCollidedHorizontally` 복원 생략 (1.21.1
  player.horizontalCollision setter 없음 → mixin 필요, 별도). 해제 엣지 본문은 리서치
  요약만 → `climbIntoCount = 0` 만 이식, 나머지는 TODO + 서브 원자 B-18b (Agent WebFetch
  필요).
- 의존 전수 충족: hasClimbCrawlGap (기존 L272) / hasClimbGap (B-15b 세션 40) / isClimbHolding
  (B-16 세션 68) / wantClimbHolding (B-18-pre 세션 81) / wantClimbUp (B-17b2-pre 세션 72) /
  climbIntoCount (기존 L275).
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 81)**:
- [근거] 원본 L2786-L2820 research/.../SmartMovingSelf.md L3497-L3524 (R-12.6) 확보 ✓
- [근거] R-12.10 #2 `isClimbCrawling 메인 공식 [누락]` §16 세션 33 ✓
- [대응] 본문 + 카운터 + 진입 엣지 원본 1:1 (isCollidedHorizontally 복원 근사 + 해제 엣지
  부분만) ✓
- [분기] 카운터 3-분기 + isClimbCrawling 메인 + 진입/해제 엣지 2갈래 ✓
- [상수] `6` 카운터 재장전 / `0.05D` 이동 / `-1F` 높이 원본 동일 ✓
- [타이밍] B-17 뒤 + B-35 앞 — 원본 L2786 순서 복원 ✓
- [근사] §7 B-18 근사 등록 (isCollidedHorizontally 복원 + 해제 엣지 본문) ✓
- [신규] B-18b 서브 원자 필요 (해제 엣지 Agent WebFetch) §10 내 TODO 기록 ✓
- [회귀] compileJava + compileClientJava 모두 ✓. isClimbCrawling 이 이전 항상 false →
  `wantClimbHolding && wantClimbUp && (needClimbCrawling || count>1)` 조건 기반 갱신 활성.
  B-16c (wantClimb 4-OR) + B-17b2-pre (wantClimbUp) + B-18-pre (wantClimbHolding) 연쇄로
  실제 동작 가능.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #1 resetClimbing (B-14 세션 57)
- ✅ #2 isClimbCrawling 메인 공식 (B-18 세션 81, 부분)
- ✅ #3 isCrawlClimbing 메인 공식 + 전환 (B-17a/b1/b2)
- ✅ #6/#7 isClimbHolding/wantClimbHolding (B-16 세션 68/69 + B-18-pre 세션 81)
- ✅ #13 isCeilingClimbing 해제 엣지 (B-21 세션 57)
- ⏳ #4/#5/#8~#12/#14 (B-19 Orientation 판정 등)

**Phase 2 진행 상황**: 51 원자 완료 (B-18-pre + B-18 부분) / 잔여 7 원자

**다음 작업 권고**:
- **B-18b** (해제 엣지 본문) — Agent WebFetch 로 L2819-L2820 상세 확보 후 이식.
- **B-44c** (wasClimbCrawling 저장 시점 조정) — B-18 완료로 단독 실행 가능 검토.
- **B-44b** (wasCrawling 저장 시점) — B-33 동시 조정 필요.
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing 등 의존 확인 필요.

### 세션 82 — 2026-04-24 — B Phase 2 B-44c (wasClimbCrawling 저장 시점 이동)

**진행한 작업**:
- `wasClimbCrawling = isClimbCrawling` 저장 위치 재정비:
  * 기존: tickEssential 초반 L830 에 일괄 저장 (B-44 세션 36 A-6 에서 "결과적 동치" 로
    유지되던 위치)
  * 정정: B-18 공식 직전 (원본 L2786 `wasClimbCrawling = isClimbCrawling` 위치 복원)
- B-18 본체 지역 변수 `boolean wasClimbCrawling = isClimbCrawling` → `this.wasClimbCrawling`
  public 필드 저장으로 전환 — 필드와 지역 변수 동일 값 (스코프 충돌 없음).
- L826-L830 주석 갱신: B-44c 이동 완료 + B-44b (wasCrawling) 는 B-33 동시 조정 대기 명시.
- 동일 값이므로 B-18 공식 결과 영향 없음 — 구조 1:1 정렬만 수행.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 82)**:
- [근거] 원본 L2786 `wasClimbCrawling = isClimbCrawling` 공식 직전 저장 위치 확정 ✓
- [근거] R-15.6 불일치 #2 `저장 시점 정밀 조정` 확정 §16 세션 36 ✓
- [대응] 저장 위치 이동 + 지역 변수 → 필드 통합. 원본 1:1 정렬 ✓
- [분기] 없음 (단순 이동)
- [상수] 없음
- [타이밍] B-18 본체 진입 직후 — 원본 L2786 순서 1:1 ✓
- [근사] 없음 — 결과적 동치 (같은 값 저장).
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. 지역 변수와 필드 동일 값이므로 B-18 공식
  결과 불변. 초반 저장 제거 → `willStartCrawl` 판정 (R-09 블록 L1504 `isClimbCrawling &&
  !wasClimbCrawling`) 도 동일 값 참조 (tickEssential 내 저장 시점이 R-09 앞이므로 순서 유지).
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**A-6 R-15.6 불일치 현황**:
- ✅ #1 R-09 블록 종료부 저장 (B-43 세션 56)
- ✅ #2 저장 시점 정밀 조정:
  * wasSneaking → isSlow 공식 직전 (B-44a 세션 43)
  * wasClimbCrawling → isClimbCrawling 공식 직전 (B-44c 세션 82)
  * wasCrawling → isCrawling 공식 직전 (B-44b, B-33 동시 조정 대기)

**Phase 2 진행 상황**: 52 원자 완료 / 잔여 6 원자

**다음 작업 권고**:
- **B-18b** (해제 엣지 본문) — Agent WebFetch 로 L2819-L2820 상세 확보 후 이식.
- **B-44b** (wasCrawling 저장 시점) — B-33 동시 조정 필요.
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing 등 의존 확인 필요.
- **B-11** (얕은 물 특수 분기) — B-9 메인 분류 재작성 범위.

### 세션 83 — 2026-04-24 — B Phase 2 B-18b (isClimbCrawling 해제 엣지 본문)

**진행한 작업**:
- Agent WebFetch 로 원본 L2804-L2820 해제 엣지 본문 확보. 완전 구조:
  ```java
  else if (!isClimbCrawling && wasClimbCrawling) {
      climbIntoCount = 0;
      if (mustCrawl || sneakButton.Pressed || crawlToggled) {
          double gap = minY - getMaxPlayerSolidBetween(minY - 1D, minY, 0);
          if (gap >= 0D && gap < 1D) {
              wasCrawling = toCrawling();
              move(0, -gap, 0, true);
          } else {
              resetHeightOffset();
          }
      } else {
          resetHeightOffset();
      }
  }
  ```
- ClientState B-18 해제 엣지의 기존 TODO (세션 81) → 완전 본문 이식:
  ```java
  climbIntoCount = 0;
  if (mustCrawl || sneakPressedRaw || crawlToggled) {
      // gap ≈ 0 근사 → 항상 [0, 1) 범위 → 크롤 전환 + move no-op
      wasCrawling = toCrawling();
  } else {
      heightOffset = 0F;  // resetHeightOffset 근사
  }
  ```
- §7 **B-18 근사 확장**: `getMaxPlayerSolidBetween` 정밀 AABB 미이식 → `gap ≈ 0` 근사 →
  `gap >= 0 && gap < 1` 항상 true → mustCrawl/sneak/crawlToggled 조건 만족 시 크롤 전환
  으로 진입. `move(0, -gap, 0)` 이동량도 0 근사 (no-op). heightOffset 유지.
- 스코프 확인: B-18 본체가 pre-compute 블록 내 배치 → `mustCrawl`/`sneakPressedRaw`/
  `crawlToggled` 모두 접근 가능. 빌드 성공으로 확인.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 83)**:
- [근거] Agent WebFetch 로 원본 L2804-L2820 완전 본문 확보 ✓
- [근거] R-12.6 해제 엣지 전환 분기 [누락] §16 세션 33 ✓
- [대응] climbIntoCount=0 + 조건부 toCrawling/resetHeightOffset 원본 1:1 (gap 이동량 근사) ✓
- [분기] 3갈래 (mustCrawl/sneak/crawlToggled 조건 → gap [0,1) / 외 → resetHeightOffset) ✓
- [상수] `0D` / `1D` 원본 동일. gap 근사 0 으로 조건 [0, 1) 자동 만족.
- [타이밍] B-18 해제 엣지 내부 (climbIntoCount=0 직후) — 원본 순서 1:1 ✓
- [근사] §7 B-18 근사 확장 (gap 정밀 측정 + move 이동량 생략) ✓
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. isClimbCrawling 해제 시 mustCrawl/sneak/
  crawlToggled 만족 → 크롤 전환 경로 복원. 원본 설계 의도 (크롤 상태 유지 vs 정상 기립)
  분기 선택 활성.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-12 A-3 불일치 현황**:
- ✅ #2 isClimbCrawling 메인 공식 + 해제 엣지 (B-18 + B-18b 세션 81/83)
- 나머지 기존 갱신과 동일

**Phase 2 진행 상황**: 53 원자 완료 (B-18b 서브 추가) / 잔여 6 원자

**다음 작업 권고**:
- **B-44b** (wasCrawling 저장 시점) — B-33 동시 조정 필요 (단독 무의미).
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing 등 의존 확인 필요.
- **B-11** (얕은 물 특수 분기) — B-9 메인 분류 재작성 범위.
- **B-19** (hasClimbGap 갱신) — 대규모 Orientation 판정 필요.

### 세션 84 — 2026-04-24 — B Phase 2 B-33 + B-44b (isCrawling 매 틱 공식 + wasCrawling 이동)

**진행한 작업** (불가분 묶음 원자):
- **B-33**: IMPL-01 이원화 구조 (L1127-L1163) 제거 + 원본 L2441-L2442 매 틱 공식 이식:
  ```java
  if (cfg.crawl) {
      boolean canCrawl = !isSwimming_sm && !isDiving
              && (!isDipping || dippingDepth < 0.65F)
              && !isClimbing
              && player.fallDistance < cfg.fallingDistanceMinimum;
      wasCrawling = isCrawling;                              // 원본 L2441
      isCrawling = canCrawl && (wantCrawl || mustCrawl);     // 원본 L2442
  }
  ```
- **B-44b**: tickEssential 초반 L829 `wasCrawling = isCrawling` 일괄 저장 제거, B-33 매 틱
  공식 직전으로 이동. 초반 저장 블록 3건 (wasSneaking/wasClimbCrawling/wasCrawling) 모두
  이동 완료 → 일괄 저장 블록 전부 제거됨.
- **IMPL-01 유지/해제 분기** 완전 제거 — 기존 `if (!isCrawling) ... toCrawling()` + `else
  if (mustCrawl/crawlToggled/isSneaking)` 대신 원본 단일 매 틱 공식으로 해제 자동 처리:
  * `wantCrawl || mustCrawl` false → `isCrawling = false` 자동.
  * R-09 블록 `willStopCrawl = !isCrawling && !isCrawlClimbing && !isClimbCrawling` →
    `crawlToggled = false` 자동.
  * grab 재누름 → `inputContinueCrawl` false (crawlToggle 모드에서 crawlToggled=false 참조) →
    wantCrawl false → isCrawling=false → 다음 틱 crawlToggled=false.
- 주석에 원본 라인 + IMPL-01 제거 근거 + 의존 체인 상세 기록.
- `./gradlew compileJava compileClientJava --rerun-tasks` 성공.

**완료 전 검증 체크리스트 (세션 84)**:
- [근거] 원본 L2441-L2447 매 틱 공식 research/.../SmartMovingSelf.md L4018-L4028 (R-14.3) ✓
- [근거] R-14.10 #1/#2 `isCrawling 이원화 구조 [오역]` + R-15.6 #2 `wasCrawling 저장 시점` ✓
- [대응] 매 틱 공식 3줄 + wasCrawling 이동 원본 1:1 ✓
- [분기] canCrawl 5-AND + 메인 공식 OR 2-OR + contextContinueCrawl 해제 (기존) 일관 ✓
- [상수] `0.65F` (SwimCrawlWaterTopBorder) 원본 유지 ✓
- [타이밍] `wasCrawling = isCrawling` 공식 직전 — 원본 L2441 위치 1:1 ✓
- [근사] 없음 — 1:1 이식. `canCrawl` 은 B-32 세션 44 에서 1:1 복원됨.
- [신규] 없음
- [회귀] compileJava + compileClientJava 모두 ✓. **주요 회귀 가능성**:
  * IMPL-01 `grab 재누름 해제` 분기 제거 — R-09 블록 자동 해제 경로 의존.
  * 매 틱 공식이 wantCrawl/mustCrawl 모두 false 시 즉시 isCrawling=false — 기존 이원화
    구조가 1틱 지연 해제였다면 차이 발생 가능.
  * 인게임 검증 필요 — 사용자 G-2 수동 테스트 범위.
- [빌드] ./gradlew compileJava compileClientJava --rerun-tasks ✓

**R-14 A-5 불일치 현황**:
- ✅ #1/#2 isCrawling 이원화 → 매 틱 공식 (B-33 세션 84)
- ✅ #4 capabilities.flying 해제 점프 (B-34 세션 59)
- ✅ #7 wall 오르기 crawl 진입 (B-37 세션 62)
- ✅ #8 handleCeilingClimbing isCrawling=false (B-38 세션 58)
- ✅ #9 landMotionPost 3분기 구조 (B-39 세션 80, 근사)
- ✅ #10/#11 전환 후처리 + grab.StartPressed 3분기 (B-35/B-36)
- ✅ #12 wantCrawlNotClimb (B-41 세션 70)
- ⏳ #3/#5/#6/#13~#15

**R-15.6 불일치 현황 (저장 시점 정밀)**:
- ✅ wasSneaking (B-44a 세션 43)
- ✅ wasClimbCrawling (B-44c 세션 82)
- ✅ wasCrawling (B-44b 세션 84)

**Phase 2 진행 상황**: 54 원자 완료 (B-33 + B-44b 묶음) / 잔여 4 원자

**다음 작업 권고**:
- **B-7** (updateSwimState 진입 조건) — isLiquidClimbing/isInLiquid/isLavaLikeWaterEnabled
  의존 — 대규모.
- **B-9** (메인 분류 재작성) — 대형.
- **B-11** (얕은 물 특수 분기) — B-9 범위.
- **B-19** (hasClimbGap 갱신) — Orientation 판정 대형.

**남은 4 원자 모두 대규모 의존** — 추가 Agent WebFetch + 서브 분해 필요. 포커스 #2 의
**"상태 플래그 값 자체의 정확성"** 목적은 B-33 매 틱 공식 완료로 핵심 해소. 잔여 원자는
소비처 (애니메이션/전환/키) 에 영향 작거나 별도 포커스 후보.

### 세션 85 — 2026-04-24 — C 단계 진입 (C-1 clean build + 잔여 4 원자 별도 포커스 분리)

**진행한 작업**:
- **C-1 완료**: `./gradlew clean build` 성공.
  * 10 actionable tasks 전부 통과 (compileJava / compileClientJava / remapJar / assemble /
    build). 경고는 Gradle 10 deprecation 만 (기능 영향 없음). BUILD SUCCESSFUL 9s.
- **잔여 4 원자 별도 포커스 분리 결정** (§17 기록):
  * **B-7 → `focus_15_liquid_climbing.md`** — isLiquidClimbing / isInLiquid /
    isLavaLikeWaterEnabled / handleLavaMovement 모두 미이식. Free climbing liquid
    (용암 등반) 전체 이식 필요. 포커스 #2 범위 밖.
  * **B-9 + B-11 → `focus_16_swim_classification.md`** — handleSwimming 대규모 재구성
    (L303-L414 offset 테이블 11단계+10단계 + motionYDiff). AABB 정밀 근사 의존 →
    focus_14 와도 연계.
  * **B-19 → `focus_17_climb_orientation.md`** — Free Climb Orientation 4/8방향 판정
    대규모 이식. focus_14 AABB 와도 연계 가능.
- **근거**: 세션 84 B-33 매 틱 공식 완료로 "13 상태 플래그 값 자체의 정확성" (포커스 #2
  핵심 목적) 달성. 잔여 4 원자는 **주요 소비처 #1/#3/#4 에 직접적 영향 작음** —
  별도 포커스로 분리하여 후속 처리.
- **§10 C-1 체크박스 [x] 해소**. §1 진행 상황 "C 단계 진입" 으로 갱신.

**완료 전 검증 체크리스트 (세션 85)**:
- [근거] `./gradlew clean build` 성공 — 10 tasks 전체 통과 ✓
- [근거] 세션 84 B-33 완료로 핵심 목적 달성 + 세션 29-84 작업 누적 ✓
- [대응] clean build 성공이 C-1 기준 충족 ✓
- [분기] 없음 (명령 실행)
- [상수] 없음
- [타이밍] 모든 B Phase 2 핵심 완료 후 C 진입 — 원자 순서 정합 ✓
- [근사] 잔여 4 원자는 별도 포커스 분리로 처리 — §7 근사 지점 유지 (focus_14/15/16/17
  에서 점진 해소 예정)
- [신규] B-7 별도 포커스 (focus_15), B-9/B-11 (focus_16), B-19 (focus_17) 3개 신규 포커스
  후보 §17 기록 ✓
- [회귀] clean build 전체 통과 — 이전 세션 누적 수정이 상호 충돌 없음. 세션 84 B-33 매 틱
  공식 전환 후에도 전체 빌드 안정.
- [빌드] ./gradlew clean build ✓ (10 tasks, 9s)

**Phase 진행 상황**: B Phase 2 54 원자 완료 / 잔여 4 원자 (별도 포커스 분리) / C 단계 C-1
완료 / C-2~C-5 대기.

**다음 작업 권고**:
- **C-2 §14 회귀 방지 감사** — 상태 소비처 (애니메이션/전환/키 커맨드) 영향 감사.
- **C-3 checklist_original_audit.md 기록** — 포커스 #2 결과 요약.
- **C-5 playtest_fixes.md "현재 포커스" → #3** 전환 (C-4 사용자 몫 제외).
- **focus_15/16/17 신규 파일 생성** — 세션 85 분리 결정의 후속 실행 (post #3 진입 후 또는
  focus_14 와 묶어 일괄).

### 세션 86 — 2026-04-24 — C-2 회귀 방지 감사

**진행한 작업**:
- §14 회귀 방지 감사 체크리스트 4개 체크포인트 전수 감사:
  1. **R-09 토글 블록**: `wasSneaking` (B-44a 세션 43) / `wasCrawling` (B-31a 세션 41 개명 +
     B-44b 세션 84 공식 직전 이동) 완료. 매 틱 isCrawling 참조로 세션 84 B-33 IMPL-01 제거
     후에도 `willStopCrawl → crawlToggled=false` 자동 해제 경로 일관성 유지 ✓
  2. **wouldWantSneak / wouldWantCrawl**: wouldWantCrawl 필드 승격 (B-36-pre 세션 77).
     wouldIsSneaking B-3b 세션 49 `!wantSprint` 정정. wantCrawl 의미 유지 ✓
  3. **sendStatePacket / processStatePacket**: SmartMovingState 비트 매핑 변경 없음. 세션 71
     isLevitating 제거는 계산만 — 네트워크 전송 형식 불변 ✓
  4. **sm_isSneaking override**: pre-compute `sneakPressedRaw` 사용으로 순환 방지 이미 적용.
     세션 84 B-33 IMPL-01 제거로 `player.isSneaking()` 호출 소멸 → 순환 위험 감소 ✓
- 공통 회귀 감사 체크리스트 (B/C/D) 추가 감사:
  * [B] 시그니처 변경 — B-16a/b/c 신규 헬퍼 추가 외 보존
  * [C] 공통 플래그 의미 변경 없음 (#5/#6 완료 항목 재검토 불필요)
  * [D] 네트워크 페이로드 변경 없음
- §14 감사 결과 표 갱신 + §10 C-2 체크박스 [x] 해소 + §1 진행 상황 갱신.
- 코드 변경 없음 — 감사 기록만.

**완료 전 검증 체크리스트 (세션 86)**:
- [근거] §14 체크리스트 4개 체크포인트 + 공통 회귀 감사 B/C/D 항목 전수 확인 ✓
- [근거] 세션 29-85 작업 누적 재검토 (B-44a/B-31a/B-44b/B-36-pre/B-3b/B-33 등) ✓
- [대응] 감사 결과 §14 표 세션 86 칼럼 기록 ✓
- [분기] 없음 (감사 작업)
- [상수] 없음
- [타이밍] C-1 완료 후 C-2 — §10 C 단계 순서 정합 ✓
- [근사] 없음 — 감사 작업
- [신규] 없음
- [회귀] 감사 자체 — 세션 29-85 누적 수정이 소비처 #1/#3/#4 에 영향 주는 회귀 없음 확정 ✓
- [빌드] C-1 세션 85 clean build 성공 이미 확인

**Phase 진행 상황**: B Phase 2 54 + 별도 포커스 분리 4 / C 단계 2/5 완료 (C-1/C-2).

**다음 작업 권고**:
- **C-3 checklist_original_audit.md 기록** — 포커스 #2 결과 요약 (파일 존재 여부 확인 후).
- **C-5 playtest_fixes.md "현재 포커스" → #3** 전환.
- **C-4** 는 사용자 인게임 몫 (G-2 테스트).

### 세션 87 — 2026-04-24 — C-3 checklist_original_audit.md 기록

**진행한 작업**:
- `docs/checklist_original_audit.md` L1100 에 포커스 #2 감사 결과 행 추가 (기존 #5/#6 동일
  형식, 파이프 표 1행).
- 기록 범위 요약:
  * A 단계 A-0~A-7 전수 감사 (불일치 70건 확정)
  * B Phase 1 필드 일괄 이식 (20+ 필드)
  * B Phase 2 54 원자 완료 (세션 29-85)
    - 핵심: B-33+B-44b isCrawling 매 틱 공식 / B-16 자동 등반 4-OR / B-17/B-18 isCrawlClimbing·
      isClimbCrawling / B-1c~f+B-3a/b isFast/wantSprint / B-43+B-44a/b/c 저장 시점 정밀 조정 /
      B-46 grab.wasPressed 해소 등
  * 잔여 4 원자 별도 포커스 분리 (B-7/B-9+B-11/B-19 → focus_15/16/17, B-42 → focus_14)
  * §7 근사 지점 7건 등록 (focus_14 AABB 정밀 개선으로 일괄 해소 경로)
  * C-1/C-2 clean build + 회귀 방지 감사 통과
- 결론 명시: "포커스 #2 '13 상태 플래그 값 자체의 정확성' 핵심 목적 달성 / BUILD SUCCESSFUL".
- 코드 변경 없음 — 문서 기록 원자.

**완료 전 검증 체크리스트 (세션 87)**:
- [근거] 기존 #5 (L1096-L1098) / #6 (L1099) 형식 참고 — 동일 구조 및 상세도 유지 ✓
- [근거] 세션 29-86 작업 누적 모두 반영 — 원자 번호 + 근거 + 세션 번호 명시 ✓
- [대응] B Phase 2 54 원자 + 별도 포커스 분리 + C-1/C-2 전수 기록 ✓
- [분기] 없음 (문서 작업)
- [상수] 없음
- [타이밍] C-3 는 C 단계 3번째 원자 — 순서 정합 ✓
- [근사] 본문에 §7 근사 7건 등록 사실 명시 ✓
- [신규] 없음
- [회귀] 없음 (기록 작업)
- [빌드] 코드 변경 없음

**Phase 진행 상황**: C 단계 3/5 완료 (C-1/C-2/C-3). 잔여 C-4 인게임 (사용자 몫) / C-5 포커스
전환.

**다음 작업 권고**:
- **C-5 playtest_fixes.md "현재 포커스" → #3 전환** — 포커스 #2 공식 완료.
- **C-4** 는 사용자 인게임 테스트 결과 대기 (세션 86 회귀 감사 통과 + clean build 성공 기반).
- focus_14/15/16/17 신규 파일 생성은 포커스 #3 진입 후 필요 시 진행.

### 세션 88 — 2026-04-24 — 엄격 완료 결정 + Extended 파일 분리

**사용자 지시**: "무조건적인 엄격 완료야. 포커스 파일이 길어질거 같으면 포커스2익스텐디드
파일을 만들어도됨."

**방침 전환 근거**:
- 세션 85 "별도 포커스 분리" (B-7/B-9/B-11/B-19 + §7 AABB 근사 8건 + B-10 부분 이식 등) 는
  사실상 범위 축소였음.
- **B-19 미이식의 도미노 효과** 확인: `isNeighborClimbing` / `hasClimbGap` /
  `hasClimbCrawlGap` 이 항상 false → 세션 47/48/68/69/72/73/77/81/83 에서 이식한
  B-17a/b1/b2 (isCrawlClimbing) + B-18/B-18-pre/B-18b (isClimbCrawling) + B-16c 자동 등반
  4-OR 공식이 **모두 항상 false** 유지 → 포커스 #2 "13 상태 플래그 값 자체의 정확성"
  핵심 목적 사실상 미달성.
- B-10a 공식 미이식 → `isShallowDiveOrSwim` 항상 false → B-36 분기 (a) 무력화.

**결정**:
1. 세션 85 분리 계획 **전체 취소** (focus_14/15/16/17 신규 파일 생성 취소).
2. Phase 3~8 (B-19 도미노 해소 / B-10 공식 완성 / B-7/B-9/B-11 / AABB 정밀화 / B-48/B-49 /
   Simple·Smart Base Climb) 전부 포커스 #2 범위로 복원.
3. 본체 focus_02_state_issues.md 이미 4832 줄로 과대 → **`focus_02_extended.md` 신규 생성**
   으로 경량화. 본체는 Phase 3~8 참조 링크만, Extended 가 상세 관리.
4. C-4/C-5 는 Phase 3~8 완료 후로 연기.

**진행한 작업** (메타 결정 + 문서 구조 재편):
- `focus_02_extended.md` 신규 생성 (목적 / 본체 관계 / Phase 3~8 원자 약 30개 목록 / 의존
  순서 / 작업 기록 / 최종 C 단계).
- 본체 §1 상태 변경 — "Extended 진행 중 / Phase 3~8 범위 복원".
- 본체 §17 에 세션 85 분리 계획 취소 + Extended 참조 기록. focus_14/15/16/17 분리 명시
  취소.
- `playtest_fixes.md` 의 focus_14 분리 메모 업데이트 (Extended 흡수).
- 코드 변경 없음 — 메타 결정 세션.

**완료 전 검증 체크리스트 (세션 88)**:
- [근거] 사용자 지시 명확 ("무조건적인 엄격 완료") ✓
- [근거] B-19 도미노 효과 분석 — B-17/B-18 공식이 isNeighborClimbing=false 로 무력화되는
  구조 확인 ✓
- [대응] 본체 경량화 + Extended 분리 (4832 → 분할 관리) ✓
- [분기] 없음 (메타 결정)
- [상수] 없음
- [타이밍] C-3 완료 후, C-4/C-5 연기 결정 ✓
- [근사] 없음 — 엄격 완료 방침으로 §7 근사 8건 모두 Phase 6 에서 해소 예정
- [신규] Phase 3~8 원자 약 30개 §Extended §3 등록. focus_14/15/16/17 별도 파일 생성 계획
  전체 취소 ✓
- [회귀] 없음 (코드 변경 없음)
- [빌드] 코드 변경 없음

**Phase 진행 상황 갱신**:
- B Phase 1/2 핵심 54 원자 완료 (포커스 #2 본체)
- B Phase 3~8 신규 범위 진입 (Extended)
- C 단계 3/5 완료 (C-1/C-2/C-3) / C-4/C-5 연기

**다음 세션**: Phase 3 B-19a 시작 — Agent WebFetch 로 원본 SmartMovingSelf.java L906-L1020
Orientation 판정 본문 확보 후 Climber 에 이식.

---

## 16. 신규 발견

### 세션 65 B-1b — `grab.wasPressed()` 다중 호출 + sprint 엣지 미이식 발견

**B-1b §6.8 검증 중 발견**:

1. **`grab.wasPressed()` 다중 호출 시 두 번째부터 false**
   - vanilla `KeyBinding.wasPressed()` 는 "press 이벤트 카운터에서 1 꺼냄" 시멘틱
   - 현재 ClientState 에서 같은 틱에 여러 곳 호출: L772 `grabJustPressed0`, L989
     `grabJustPressed` (IMPL-01). 지역 변수로 1회 캐시되나 사용 범위 제한.
   - 원본 `grabButton.StartPressed` 는 틱 내 불변 불리언 — 1.21.1 `wasPressed()` 는 소비성.
   - 분류: [오역] — 원본 시멘틱과 차이. 수정 방안: tickEssential 초반 1회만 호출 →
     `grabJustPressed` 필드에 저장 → 나머지 지점은 필드 참조. 또는 prev vs cur 비교로
     엣지 필드 추가 (sneakKey 방식). **신규 원자 B-46** 후보.

2. **`sprintButton.StartPressed` / `StopPressed` 미이식**
   - vanilla `sprintKey` 는 `isPressed()` 만 사용 중 (L872). 엣지 검출 없음.
   - 원본 사용 지점은 isGroundSprinting 전환 후처리 (L2697-L2709 / B-48 범위) 에서
     확인 필요. 필요 시 `sprintKeyStartPressed` / `sprintKeyStopPressed` 필드 신설.
   - 분류: [누락] — B-48 수행 시 함께 평가.
   - **→ Extended Phase 7 B-48a/b/c 로 이전** (세션 88).

3. **`grab.StopPressed` 미이식**
   - 원본에서 `grabButton.StopPressed` 사용 여부 전수 확인 필요. 사용 지점 있으면
     별도 엣지 필드 (`grabKeyStopPressed`) 신설.
   - 분류: [누락] — B-N 신설 대기.
   - **→ Extended Phase 7 B-49 로 이전** (세션 88).

**우선순위**: (1) 이 가장 영향 큼 — 현재 다중 호출 구조로 IMPL-01 grab 엣지 판정 오동작
가능. B-18/B-33 등 핵심 공식 재작성 시 반드시 해소 필요. **(1) 은 B-46 세션 66 해소**.

### 세션 29 A-1 — `isSlow`/`isFast` 3건 불일치 확정

**Agent WebFetch 로 원본 `SmartMovingSelf.java` 전수 감사 결과**:

1. **`isFast` 공식 완전 오역**
   - 원본 (Self L2688-L2695): 6갈래 OR 합성 — `isGroundSprinting || isClimbSprinting ||
     isSwimSprinting || isDiveSprinting || isCeilingSprinting || isFlyingSprinting ||
     isClimbSprinting(중복)`
   - 의존: `canHorizontallySprint` / `canAllSprint` / `canAnySprint` / `isClimbSprintSpeed` /
     `standing = onGround && !isSliding && !isCrawling` / `Config._sprintEnableStanding`
   - 1.21.1 (ClientState L653): `grab.isPressed() && player.isSprinting()` 단순 이식 — 원본과
     완전 다름. `grab` 은 원본의 `grabButton` 이나 원본 isFast 계산에 쓰이지 않음.

2. **`isSlow` 공식 부분 오역**
   - 원본 (Self L2718): `isSlow = wantSneak && wouldIsSneaking`
     where `wantSneak = Config.isSneakingEnabled() && wouldWantSneak` (L2588-L2590)
   - 1.21.1 (ClientState L651): `isSlow = sneakContinueInput && wouldIsSneaking`
     — `sneakContinueInput` 은 `wouldWantSneak` 계산에 이미 포함되므로 **중복 곱**.
     + `Config.isSneakingEnabled()` 체크 **누락**. 결과: `cfg.sneak` / `cfg.enabled` 가 false
     라도 isSlow 가 true 될 수 있는 버그.

3. **`wouldIsSneaking` 공식 부분 오역**
   - 원본 (Self L2712): `wouldIsSneaking = wouldWantSneak && !wantSprint && !isClimbing`
   - 1.21.1 (ClientState L650): `wouldIsSneaking = wouldWantSneak && !player.isSprinting() &&
     !isClimbing` — `wantSprint` 을 vanilla `isSprinting()` 로 대체.
   - 원본 `wantSprint` (Self L2595-L2615): `Config.isSprintingEnabled() && !isSliding &&
     sprintButton.Pressed && (moveForwardButtonPressed || isClimbing || 수영/잠수/비행
     컨텍스트별 세부 조건) && !disabled` — 6조건 복합.
   - vanilla `isSprinting()` 은 단순히 스프린트 중 attribute 상태 — SM 컨텍스트 조건 전부 누락.

**영향**:
- `isFast` 오역은 특히 심각 — 원본에서 비행/수영/잠수/등반 중에도 Fast 가 될 수 있는데
  (컨텍스트별 조건), 1.21.1 에선 **지면 스프린트 + grab 키** 만 인식.
- `isSlow` 중복 + 가드 누락: `cfg.sneak=false` 설정 시에도 sneak 동작 가능 (원본과 불일치).
- `wouldIsSneaking` 의 `!wantSprint` 를 `!isSprinting()` 으로 대체: 수영 중 sneakDown 활성
  시점에 미세 차이 가능.

**수정 범위**: §10 B-1 (isFast) / B-2 (isSlow) / B-3 (wouldIsSneaking + wantSprint) 3건.
각 B-N 은 의존 필드 신설 포함이라 적당한 규모.

### 세션 30 — "간소 매핑" 주석 2건 발견 (B-4 / B-5)

1:1 번역 원칙 재확인 중 1.21.1 코드 grep 으로 발견:

1. **`SmartMovingClientState.java` L788** — R-09 토글 블록
   - 주석: `// 원본 L2986: wantSneak/wantSprint 참조. 간소 매핑: wantSneak=sneakContinueInput,
     wantSprint=isSprinting.`
   - 로컬 변수 `wantSneak_` (L789-L791) + `wantSprint_` (L792) 가 원본 `wantSneak` (Config
     게이트 포함 6조건) / `wantSprint` (Config 게이트 포함 6조건 OR) 과 다른 간소 매핑.
   - 수정: B-4 — B-2/B-3 완료 후 확정된 필드로 교체.

2. **`SmartMovingSwimmer.java` L159** — 수영 상태 판정 관련
   - 주석: `// 1.21.1 간소화:` — 어떤 부분이 간소화되었는지 원본 대조 필요.
   - 수정: B-5 — A-2 (수중 3상태) 감사 시 함께 대조, 간소화 부분 원자 분해.

**원칙 (세션 30)**: 1:1 번역 절대 원칙 — 근사·간소·대체 매핑 전부 금지. 의존 필드 규모
크면 체크리스트 쪼개서 여러 세션 분산 (§10 B-1a~f 선례). 컨텍스트 압박 ≠ 축약 허용 근거.

### 세션 31 B-0 — 원본 의존 체인 전수 덤프 중 발견 3건

R-10 섹션 작성 중 1.21.1 이식 시 주의·추가 원자 필요 지점:

1. **순환 의존: `isSprintJump` ↔ `isFast`** (원본 L2633/L2640)
   - `isSprintJump = true` 조건이 `isFast` 이전 틱 값 참조
   - 1.21.1 `tickEssential` 은 매 틱 `isFast` 를 덮어쓰므로 순환 끊기 위해:
     * `wasFast` 필드 신설 + beforeOnLivingUpdate 에서 저장, 또는
     * `isSprintJump` 계산을 `isFast` 확정 이전으로 이동 (단 L2633-L2640 은 실제
       `isFast` 계산 L2688 이전에 위치 — 원본도 이전 틱 값 참조로 구조 일관성)
   - 수정: B-1f (isFast 공식 교체) 원자 설계 시 계산 순서 주의 — 이전 틱 저장 패턴 사용.

2. **미이식 필드 2건 후보** — B-1c 원자 범위에서 판단:
   - `SmartStatisticsFactory.getInstance(sp).getTickDistance()` — SmartRender 측 통계.
     등반 sprint 속도 게이트 (`isClimbSprintSpeed` L2670). 1.21.1 이식 여부 grep 확인 →
     미이식 시 옵션:
     (a) 1.21.1 전용 틱 거리 통계 별도 이식 (원자 신설)
     (b) `true` 근사 → 등반 sprint 항상 가능 (원본 게이트 의미 약화, 1:1 위배)
     → **원칙상 (a)** 하지만 포커스 #2 범위 판단 필요. 포커스 #12/#13 으로 분리 후보.
   - `collidedHorizontallyTickCount` — 수평 충돌 연속 틱 카운터 (`canHorizontallySprint`
     L2675). 1.21.1 이식 여부 grep 확인 → 미이식 시 `ClientState` 또는 Jumper 측에 신설.

3. **isGroundSprinting 전환 후처리** (원본 L2697-L2709):
   - `wasGroundSprinting = isGroundSprinting` 이전 틱 저장 → Sprint 시작/종료 엣지에서
     vanilla `setSprinting()` 호출
   - 의존: `Options._runOnSprintRelease` / `_walkOnSprintRelease` /
     `wasRunningWhenSprintStarted` / `isStandupSprintingOrRunning()` 메서드
   - 1.21.1 이식 여부 grep 확인 필요. 미이식 시 B-N 추가 원자 (Options 필드 + 메서드
     + 후처리 블록 이식).

**수정 범위**: §10 B-1 서브원자 설계 시 위 3건 반영. 특히 B-1c (can* 4 판정) 에서
`collidedHorizontallyTickCount` / `SmartStatisticsFactory` 이식 여부 grep 전용 하위
원자 추가. 필요 시 별도 포커스로 분리.

### 세션 32 A-2 — 수중 3상태 불일치 11건 확정

R-11 섹션 (SmartMovingSelf.md L2508 이후) 에 원본 전수 덤프 + 1.21.1 side-by-side.

1. **진입 조건 간소화** (원본 L232 → 1.21.1 Swimmer L65)
   - 원본: `!isFlying && !isLiquidClimbing && (sp.isInWater() || (wasSwimming &&
     isInLiquid()) || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()))`
   - 1.21.1: `player.isTouchingWater()` 만
   - 영향: 비행/물등반/라바수영/연속성 전혀 반영 안 됨 → 물 진입 순간 즉시 수중 상태.
   - 분류: [오역] / 수정 B-7.

2. **Config.isSwimmingEnabled()/isDivingEnabled() 게이트 누락** (원본 L239/L438-L440)
   - 원본: `Config.isSwimmingEnabled()` / `Config.isDivingEnabled()` 둘 다 false 면
     useStandard 로 분기 + 분류 후 Config 재게이트 적용
   - 1.21.1: updateSwimState 에 Config 게이트 없음 (handleSwimming L145-L146 에서 진입
     막지만 isSwimming_sm/isDiving 값은 이미 true 설정된 후)
   - 분류: [누락] / 수정 B-8.

3. **isClimbCrawling 누락** (원본 L301 → 1.21.1 Swimmer L78)
   - 원본 3-OR: `isCrawling || isClimbCrawling || isCrawlClimbing` → isDipping=true
   - 1.21.1 2-OR: `sm.isCrawling || sm.isCrawlClimbing` (isClimbCrawling 누락)
   - ※ 주석 (Swimmer L77) 은 3-OR 명시했지만 코드는 2-OR
   - 분류: [누락] / 수정 B-6.

4. **메인 분류 공식 완전 대체** (원본 L303-L414 → 1.21.1 Swimmer L86-L89)
   - 원본: 3-갈래 (`[0,2]` / `(2,∞)` / `(-∞,0)`) + A/B 서브 (`diveUp||moveSwim||
     wantShallowSwim`) + 11-단계 swimming offset 테이블 (1.4~1.9) + 10-단계 diving
     offset 테이블 + motionYDiff 전체 적용
   - 1.21.1: 단순 offset 3분류 (`<1.4 / [1.4,1.9) / >=1.9`) — diveUp/moveSwim 분기
     전무, playerSwimWaterBorder>2 처리 흡수됨
   - 분류: [오역] / 수정 B-9 (가장 큼, 서브원자 7개 분해 권장).

5. **isShallowDiveOrSwim 필드 미이식** (원본 L507)
   - 원본: `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming)` — handleSwimming
     메인 분류 이후 갱신
   - 1.21.1: ClientState 필드 없음 (grep 0건)
   - 영향: 얕은 물 특수 분기 L513-L536 판정 불가능
   - 분류: [누락] / 수정 B-10a.

6. **isJumpingOutOfWater 필드 미이식** (원본 L487)
   - 원본: `isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 ||
     sp.onGround || wasJumpingOutOfWater)` — 수면 탈출 점프 진행 조건
   - 1.21.1: ClientState 필드 없음
   - 영향: 수면에서 점프로 탈출하는 특수 물리 (L500 `motionY = 0.30000001192092896D`) 없음
   - 분류: [누락] / 수정 B-10b.

7. **isStillSwimmingJump 필드 미이식** (원본 L550)
   - 원본: useStandard 경로에서 false 리셋
   - 1.21.1: ClientState 필드 없음
   - 분류: [누락] / 수정 B-10c.

8. **isLevitating 필드 미이식** (원본 L505)
   - 원본: `isLevitating = levitating` where `levitating = diving && !diveUp && !diveDown
     && moveStrafing==0F && moveForward==0F` (L474) — diving 중 정지 모드
   - 1.21.1: ClientState 필드 없음
   - 영향: diving 중 정지 상태 렌더/물리 차이
   - 분류: [누락] / 수정 B-10d.

9. **얕은 물 특수 분기 미이식** (원본 L513-L536)
   - 원본: `isShallowDiveOrSwim && realMinPlayerSwimWaterDepth < SwimCrawlWaterBottomBorder`
     진입 조건 + isSlow 분기 (swimming/diving → crawling / walking)
   - 1.21.1: 미이식 (B-10a 먼저 필요)
   - 분류: [누락] / 수정 B-11.

10. **waterMovementTicks 증분 조건 차이** (원본 L481-L484 vs 1.21.1 L82/L90)
    - 원본: `if(swimming || diving) waterMovementTicks++; else waterMovementTicks=0;`
      — dipping 에서는 0 리셋
    - 1.21.1: `sm.waterMovementTicks++` (L82/L90) — dipping 에서도 증분
    - 영향: isJumpingOutOfWater 의 `waterMovementTicks > 10` 판정 부정확
    - 분류: [오역] / 수정 B-12.

11. **크롤↔수영 전환 isSliding 조건 누락** (원본 L418 vs 1.21.1 handleSwimming L119/L124)
    - 원본: `(isCrawling || isSliding) && playerCrawlWaterBorder < SwimCrawlWaterMaxBorder`
    - 1.21.1: `sm.isCrawling && sm.dippingDepth > SWIM_CRAWL_TOP` (L119) — isSliding 누락
    - 영향: 슬라이딩 중 물 진입 시 전환 동작 누락
    - 분류: [누락] / 수정 B-13.

**수정 범위**: §10 B-6~B-13 (8 원자 + B-9 서브원자 7개 권장 = 최대 14 원자). B-9 가 가장
큼 — 메인 분류 공식 재작성 시 세션 분산 필수.

**우선순위 권고**:
- 1순위: B-6 (isClimbCrawling 한 조건 추가) — 1줄 수정, 즉시 가능
- 2순위: B-10a~d (필드 이식) — 이후 B-11/B-12 전제
- 3순위: B-12 (waterMovementTicks 증분) — B-10b 완료 후
- 4순위: B-9 (메인 분류 재작성) — 가장 큰 작업, 여러 세션 분할
- 5순위: B-7/B-8/B-11/B-13 — 순차 진행

### 세션 44 B-40 — isCrawlToggleEnabled() 누락 (다중 호출 지점)

**발견**: B-40 toCrawling 이식 중 원본 `Options.isCrawlToggleEnabled() = _crawlToggle
&& enabled` (AND 패턴) 재확인. 1.21.1 의 다른 호출 지점들이 `cfg.enabled` 체크 누락
상태:

1. `ClientState.java` L784-L786 sneakContinueInput 계산:
   ```java
   boolean sneakContinueInput = cfg0.sneakToggle  // cfg.enabled 체크 없음
       ? (sneakToggled || sneakKeyStartPressed)
       : sneakPressedRaw;
   ```
   원본 공식: `Options.isSneakToggleEnabled()` = `sneakToggle && enabled`.

2. `ClientState.java` L931-L932 R-09 블록 진입:
   ```java
   boolean isSneakToggleEnabled = cfg.sneakToggle && cfg.enabled;  // 이건 맞음 ✓
   boolean isCrawlToggleEnabled = cfg.crawlToggle && cfg.enabled;  // 이것도 맞음 ✓
   ```

3. `ClientState.java` L789-L790 (R-09 블록 내 `wantSneak_`):
   ```java
   boolean wantSneak_ = cfg.sneakToggle  // cfg.enabled 체크 없음 (B-4 범위)
   ```

**권고 원자 신설 (B-45)**:
- ClientState L784 sneakContinueInput 및 기타 모든 `cfg.sneakToggle ? ... : ...` 또는
  `cfg.crawlToggle ? ... : ...` 위치를 `isSneakToggleEnabled()` / `isCrawlToggleEnabled()`
  헬퍼로 감싸기. Config 에 2 헬퍼 신설.

**교훈**: A-7 매핑 테이블 §6.7 Config 섹션 재검토 필요 — `isSneakToggleEnabled` /
`isCrawlToggleEnabled` 도 (공식 확정 AND 패턴) 매핑 기록 추가.

---

### 세션 39 B-2/B-3a/B-8 — Config.isSneakingEnabled 매핑 오류 정정

**발견**: 세션 31 B-0 R-10.11 매핑 예비안 + A-1 §16 기록 + §10 B-2 설명에서
`Config.isSneakingEnabled()` 를 `cfg.sneak && cfg.enabled` (AND 패턴) 으로 잘못
매핑했음. 원본 `SmartMovingClientConfig.java` L69 확인 결과:

```java
public boolean isSneakingEnabled()   { return _sneak.value || !enabled; }  // OR!
```

**원본 주석 (리서치 L428)**: "SmartMoving 전체 비활성화 시 vanilla 동작 허용하는 메서드는
`|| !enabled`, SmartMoving 전용 기능은 `&& enabled`. `isSneakingEnabled`,
`isStandardBaseClimb`, `isRunningEnabled`, `isHungerGainEnabled`가 전자."

**영향**:
- `wantSneak = isSneakingEnabled() && wouldWantSneak` (원본 L2588-L2590)
- AND 매핑 시 `cfg.enabled=false` 에서 wantSneak 항상 false → vanilla 스닉도 막힘 (버그)
- OR 매핑 시 `cfg.enabled=false` 에서도 `_sneak.value=true` 면 wantSneak 정상 → vanilla 스닉 허용

**수정**:
- §6.7 매핑 테이블에 원본 공식 명시 + AND/OR 구분
- §10 B-2 설명에 OR 패턴 강조
- `SmartMovingConfig.isSneakingEnabled()` 구현: `sneak || !enabled` (세션 39)

**교훈**: 다른 헬퍼들(`isRunningEnabled`/`isStandardBaseClimb`/`isHungerGainEnabled`) 도
OR 패턴 가능성. B-N 진행 시 각 헬퍼 이식 전 원본 `SmartMovingClientConfig` 재확인 필수.

---

### 세션 33 A-3 — 등반 4상태 불일치 14건 확정 (모두 [누락])

R-12 섹션 (SmartMovingSelf.md L3000+) 전수 덤프 기반. 1.21.1 Climber 는 **`isClimbing`
진입/해제 + `isCeilingClimbing` 진입만 이식**, 나머지 대부분 미이식.

1. **`resetClimbing()` 메서드 자체 미이식** (원본 L1474-L1486)
   - 원본: handleClimbing 진입 시 매 틱 호출 — 10 필드 (isClimbing /
     isHandsVineClimbing / isFeetVineClimbing / isVineOnlyClimbing / isVineAnyClimbing /
     isClimbingStill / isNeighborClimbing / actualHandsClimbType / actualFeetClimbType /
     isCeilingClimbing) 리셋
   - 1.21.1: Mixin L117 주석만 있고 실제 호출 없음. handleClimbing 진입 시 이전 틱 상태
     잔존 위험
   - 분류: [누락] / 수정 B-14.

2. **`isCrawlClimbing` 메인 공식 완전 미이식** (원본 L2737)
   - 5-AND: `(wasCrawling || isCrawlClimbing) && isClimbing && isNeighborClimbing &&
     (sneakPressed || crawlToggled) && moveForward > 0F`
   - 1.21.1: 갱신 로직 없음 → 값 항상 false (packet 수신 외)
   - 분류: [누락] / 수정 B-17.

3. **`isCrawlClimbing` 전환 블록 미이식** (원본 L2737-L2754)
   - canStandUp 판정 + wasCrawlClimbing 전환 + wasCrawling/isCrawling 전환
   - 1.21.1: 없음
   - 분류: [누락] / 수정 B-17.

4. **`isClimbCrawling` 메인 공식 완전 미이식** (원본 L2795)
   - `canClimbCrawling && ((needClimbCrawling && count==0) || count>1)`
   - 1.21.1: 갱신 로직 없음
   - 분류: [누락] / 수정 B-18.

5. **`climbIntoCount` 카운터 로직 미이식** (원본 L2786-L2820)
   - 6→5→...→1→0 감소 + 재장전 규칙
   - 1.21.1: 필드는 있으나 카운터 갱신 로직 없음 (resetState 0 만)
   - 분류: [누락] / 수정 B-18.

6. **`isClimbHolding` 공식 미이식** (원본 L2730)
   - `isClimbHolding = wantClimbHolding && isClimbing`
   - 1.21.1: 필드는 있으나 항상 false
   - 분류: [누락] / 수정 B-16.

7. **`wantClimbHolding` 3-OR 계산 미이식** (원본 L2721-L2728)
   - `(isClimbHolding && sneakPressed) || (isClimbing && blocked) || (wantClimb && !수영 && !다이빙 && !크롤 && 스니크/크롤토글)`
   - 1.21.1: 없음
   - 분류: [누락] / 수정 B-16.

8. **`isNeighborClimbing` 필드 미이식** (원본 L1426)
   - handleClimbing Free 분기 내부 Orientation 판정으로 갱신
   - 1.21.1: 필드 없음 → isCrawlClimbing 공식 평가 불가능
   - 분류: [누락] / 수정 B-15a + B-19.

9. **`hasClimbGap` 필드 미이식** (원본 L1427)
   - needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding) 에 필수
   - 1.21.1: 필드 없음
   - 분류: [누락] / 수정 B-15b + B-19.

10. **`hasClimbCrawlGap` 갱신 로직 미이식**
    - 1.21.1 ClientState 필드 선언은 있으나 갱신 로직 (handleClimbing 내부) 없음
    - 분류: [누락] / 수정 B-19.

11. **`isVineOnlyClimbing` / `isVineAnyClimbing` / `isClimbingStill` 필드 미이식**
    - 애니메이션/상태 표시용. 1.21.1 필드 자체 없음
    - 분류: [누락] / 수정 B-15d + B-15e.

12. **`handsEdgeBlock` / `feetEdgeBlock` / edgeMeta 필드 미이식** (원본 L1443-L1446)
    - 등반 애니메이션 파라미터. 1.21.1 필드 없음
    - 분류: [누락] / 수정 B-15f.

13. **`isCeilingClimbing` 해제 엣지 미이식** (원본 L1485)
    - 원본 resetClimbing() 에서 매 틱 false. 1.21.1 resetState 만 있음
    - 분류: [누락] / 수정 B-14 완료 시 자동 해결 (B-21).

14. **Standard / Simple Base Climb 분기 완전 미이식** (원본 L820-L844)
    - `Config.isStandardBaseClimb()` / `Config.isSimpleBaseClimb()` 옵션 분기. motionY
      0.2 / FastUpMotion / SlowUpMotion 직접 설정 (isClimbing 은 설정 안 함)
    - 1.21.1 Climber 는 Free 만 이식. Standard/Simple 선택 시 SM 물리 동작 안 함
    - 분류: [누락] / 수정 B-20.

**수정 범위**: §10 B-14~B-21 (8 원자 + B-15a~f 6 서브 + B-19 서브 2+ = 최대 18 원자).
B-15 (필드 이식) 과 B-19 (갱신 로직) 가 전제 의존. 순서 권고:

**우선순위 권고 (A-3)**:
- 1순위: **B-14** (resetClimbing 신설) — 모든 등반 상태 리셋 매 틱 보장. 의존 없음.
- 2순위: **B-15a~f** (미이식 필드 9건 이식) — 선언만 추가, 갱신은 B-19
- 3순위: **B-16** (isClimbHolding 계산) — wantClimb / blocked 필드 확인 필요
- 4순위: **B-19** (의존 필드 갱신) — handleClimbing Free 분기 내부 Orientation 판정.
  규모 큼, 서브원자 분해 필수
- 5순위: **B-17** (isCrawlClimbing 공식) — B-15a `isNeighborClimbing` 선행
- 6순위: **B-18** (isClimbCrawling 공식 + 카운터) — B-15b / B-16 선행
- 7순위: **B-20** (Standard/Simple Base Climb) — 독립 가능
- 8순위: **B-21** (isCeilingClimbing 해제) — B-14 완료 시 자동

### 세션 34 A-4 — 전환 쌍 isHeadJumping/isSliding 불일치 13건 확정

R-13 섹션 전수 덤프 기반. isAerodynamic 은 이미 R-05/포커스#6 B-5 에서 이식 완료 확인
— 이번 포커스 수정 대상 아님.

1. **isHeadJumping 매 틱 재평가 5-AND 공식 미이식** (원본 L2524-L2530)
   - 조건: `!onGround && !(swimming||diving) && !(flying||capabilities.flying) &&
     !(waterMovement && motionY<0) && !lavaMovement`
   - 1.21.1 tickEssential 에 재평가 블록 없음. 한 번 true 가 된 isHeadJumping 은
     SlideToHeadJumping 전환 경로 외에는 자동 해제되지 않음.
   - 분류: [누락] / 수정 B-23.

2. **해제 엣지 후처리 미이식** (원본 L2535-L2540)
   - `wasHeadJumping && !isHeadJumping && onGround` 시 `handleCrash(_headFallDamageStart,
     _headFallDamageFactor)` + `restoreFromFlying = true`
   - 낙하 데미지 + standupIfPossible 트리거 누락
   - 분류: [누락] / 수정 B-24.

3. **isSliding 직접 진입 6-AND 조건 간소화** (원본 L2553 vs 1.21.1 L699)
   - 원본: `SlidingEnabled && grabPressed && (isGroundSprinting || (wasRunning && !isRunning
     && onGround)) && !isCrawling && sneakStartPressed && !isDipping`
   - 1.21.1: `isSneaking && isSprinting && onGround && !isClimbing && !isHeadJumping`
   - 차이: grab 조건 누락 / isGroundSprinting → isSprinting 간소 / wasRunning 엣지 누락 /
     sneak 엣지(StartPressed) → 상태(isSneaking) / !isDipping 누락
   - 분류: [오역] / 수정 B-25.

4. **직접 진입 부수 동작 3건 미이식** (원본 L2555-L2557)
   - `setHeightOffset(-1)` — pose 낮춤
   - `move(0, -1D, 0, true)` — 1 블록 하강
   - `tryJump(Config.SlideDown, false, wasRunning, null)` — SlideDown 점프
   - 1.21.1 L702 는 `isSliding = true; isAerodynamic = false` 만
   - 분류: [누락] / 수정 B-26.

5. **직접 진입 블록 isHeadJumping=false 누락** (원본 L2559)
   - 1.21.1 L702-L704 에 없음
   - 분류: [누락] / 수정 B-26.

6. **fallDistance > _fallingDistanceMinimum 분기 미이식** (원본 L2569-L2574)
   - `isSliding = false; wasCrawling = true; isCrawling = false;`
   - 분류: [누락] / 수정 B-27.

7. **handleClimbing 진입 시 isSliding=false 미이식** (원본 L985)
   - 1.21.1 Climber 에 isSliding 해제 위치 없음
   - 분류: [누락] / 수정 B-28 (또는 B-14 resetClimbing 에 포함).

8. **toSlidingOrCrawling 조건 완전 대체** (원본 L2226 vs 1.21.1 Jumper L103)
   - 원본: `SlidingEnabled && (grabPressed || wasHeadJumping)`
   - 1.21.1: `(isSprinting || isFast) && cfg.slide`
   - **의미 자체가 다름** — 원본 "잡기/이전헤드점프" vs 1.21.1 "스프린트/Fast"
   - 분류: [오역] / 수정 B-29.

9. **`wasHeadJumping` 필드 미이식**
   - 원본 L2524 매 틱 저장. 해제 엣지 후처리 + toSlidingOrCrawling 조건 사용
   - 분류: [누락] / 수정 B-22a.

10. **`wasRunning` 필드 미이식**
    - 원본 L2553 직접 진입 조건 + L2557 tryJump 파라미터 사용
    - 1.21.1 엔 필드 없음 (로컬만)
    - 분류: [누락] / 수정 B-22b.

11. **`isRunning` 필드 미이식**
    - 원본 L3241 override 메서드 `isSprinting() && !isFast && (onGround || vanilla())`
    - 1.21.1 엔 handleExhaustion 로컬 변수만 (L995)
    - 분류: [누락] / 수정 B-22c.

12. **`isStanding` 필드 미이식**
    - 원본 L1419 필드 + L2734 갱신 공식 (`horizontalSpeedSquare < 0.0005`)
    - 1.21.1 엔 필드 없음 (ClientState grep 0건 — 실제 용도는 getJumpSpeed +
      handleExhaustion Config.getFactor)
    - 분류: [누락] / 수정 B-22d + B-30.

13. **standUp() 내 isHeadJumping=false 경로 일치 검증 필요** (원본 L2218 ↔ Jumper L98)
    - 1.21.1 Jumper L98 에 있으나 호출 경로가 원본 standUp 과 동일한지 검증 필요
    - 분류: [부분] / 수정 검증 필요.

**수정 범위**: §10 B-22~B-30 (9 원자 + B-22a~d 4 서브 = 최대 13 원자). B-22 (필드 이식)
가 전제 — B-23 / B-24 / B-25 / B-29 / B-30 이 의존.

**우선순위 권고 (A-4)**:
- 1순위: **B-22a~d** (미이식 필드 4건 이식) — 선언만, 나머지 B-N 이 전부 의존
- 2순위: **B-30** (isStanding 공식) — 기존 호출처가 로컬 변수로 임시 계산 중
- 3순위: **B-23** (매 틱 재평가 공식) — B-22a 선행. 가장 구조적 불일치
- 4순위: **B-24** (해제 엣지 후처리) — B-23 완료 후
- 5순위: **B-29** (toSlidingOrCrawling 조건 정정) — B-22a 선행
- 6순위: **B-25+B-26** (직접 진입 조건 + 부수 동작) — 묶음 (B-22b+c+B-1d 선행)
- 7순위: **B-27** (fallDistance 분기)
- 8순위: **B-28** (handleClimbing 해제) — B-14 와 겹치면 합병

### 세션 35 A-5 — isCrawling + contextContinueCrawl 불일치 15건 확정

R-14 섹션 전수 덤프. isCrawling 은 가장 복잡한 상태 — 원본 19곳 갱신 중 1.21.1 미이식
다수. contextContinueCrawl 4곳은 거의 이식됨.

1. **메인 공식 구조 차이** (원본 L2442 vs 1.21.1 IMPL-01 L661-L693)
   - 원본: 매 틱 `wasCrawling = isCrawling; isCrawling = canCrawl && (wantCrawl || mustCrawl)`
   - 1.21.1: 진입 분기(!isCrawling → canCrawl 평가) + 해제 분기(isCrawling → mustCrawl/
     crawlToggled/isSneaking 판정) 이원화
   - 영향: 원본의 자연 해제 (canCrawl false 시 isCrawling=false) 가 1.21.1 에서 누락.
     수영/등반 진입해도 isCrawling 유지 가능.
   - 분류: [오역] / 수정 B-33.

2. **canCrawl 9-AND 잉여 (원본 5-AND)** (원본 L2434-L2439 vs 1.21.1 L669-L672)
   - 원본 5-AND: `!swim && !dive && (!dipping || shallow) && !climbing && fallDistance<min`
   - 1.21.1 9-AND: 추가 `!crawlClimbing && !ceilingClimbing && !sliding && !headJumping && !flying`
   - 주석 "1.21.1 근사" 로 의도적 추가 (세션 30 원칙 위배 — 1:1 복원 필요)
   - 분류: [잉여] / 수정 B-32.

3. **`wasCrawling` 필드 누락** (원본 L2441 tickEssential 저장)
   - 1.21.1 에 `wasCrawling_st` (R-09 전용) 만 있고 tickEssential 저장 전용 wasCrawling
     없음. 혼동 방지 위해 별도 필드 필요
   - 분류: [누락] / 수정 B-31a.

4. **capabilities.flying 해제 점프 미이식** (원본 L2449-L2450)
   - `wasCrawling && !isCrawling && capabilities.flying → tryJump(Config.Up, null,null,null)`
   - 비행 중 크롤 해제 시 자동 점프 누락
   - 분류: [누락] / 수정 B-34.

5. **wasCrawling↔isCrawling 전환 후처리 미이식** (원본 L2822-L2836)
   - 해제: `resetHeightOffset + move(0, crawlStandUpBottom - minY, 0)`
   - 진입: `setHeightOffset(-1F) + move(0, -1D, 0)` (+ initializeCrawling 분기)
   - 크롤 진입/해제 시 1 블록 수직 조정 전무 → 플레이어 위치 이상
   - 분류: [누락] / 수정 B-35.

6. **grab.StartPressed 수영/크롤 3분기 미이식** (원본 L2838-L2861)
   - (a) shallow dive/swim + wouldWantClimb → walking 전환
   - (b) dipping + wouldWantCrawl + depth>=Medium → 수영/다이빙
   - (c) dipping + wouldWantCrawl + depth<Medium → 얕은 물 크롤
   - 의존: A-2 isShallowDiveOrSwim 미이식 (B-10a) → 연쇄 영향
   - 분류: [누락] / 수정 B-36.

7. **handleClimbing wall 오르기 crawl 진입 미이식** (원본 L985-L986)
   - `isSliding && handsClimbing.IsRelevant() → isSliding=false; isCrawling=true`
   - Climber 내 wall 오르기 시 slide→crawl 쌍 전환 없음
   - 분류: [누락] / 수정 B-37.

8. **handleCeilingClimbing 진입 시 isCrawling=false 미이식** (원본 L1170)
   - 천장 등반 진입 시 crawl 자동 해제 없음 — 상태 충돌 가능
   - 분류: [누락] / 수정 B-38.

9. **landMotionPost 3분기 중 isSlow+0.5D 누락** (원본 L1398)
   - 1.21.1 fromSwimmingOrDiving 에 1/2분기만 이식 (L1063/L1068), 3분기 (`isSlow &&
     crawlStandUpBottom > minY + 0.5D`) 누락
   - 분류: [누락] / 수정 B-39.

10. **`toCrawling()` 함수 부분 이식** (원본 L3047-L3054)
    - 1.21.1 IMPL-01 내부 3줄 inline 만 — 다른 호출 지점 (5/6 미이식) 에서 미사용
    - 분류: [부분] / 수정 B-40 (헬퍼 신설 + 호출 정합성).

11. **`wantCrawlNotClimb` 필드 + 갱신 미이식** (원본 L2452-L2461)
    - grab.StartPressed + !wasCrawling 4-AND 갱신
    - 사용처: climbing 관련 — A-3 B-19 의존 가능성
    - 분류: [누락] / 수정 B-31b + B-41.

12. **`initializeCrawling` 필드 + 블록 미이식** (원본 L2399/L2822/L2831/L2834)
    - mustCrawl 계산 + 전환 후처리에 사용 — 초기 틱 진입 시 boundingBox 처리 차이
    - 분류: [누락] / 수정 B-31c.

13. **`mustCrawl` AABB 근사** (원본 L2396-L2401 vs 1.21.1 L578)
    - 원본: getMaxPlayerSolidBetween / getMinPlayerSolidBetween 정밀 AABB
    - 1.21.1: `canStandUp(player)` 메서드 근사 (§7 기록)
    - 분류: [근사] / 수정 B-42 (별도 포커스 후보).

14. **`crawlStandUpBottom` 변수 미이식**
    - 원본 L2396 지역변수 (B-35 전환 후처리에 사용)
    - 분류: [누락] / B-35 내 포함.

15. **토글 모드 분기 시점 차이** — 원본은 `inputContinueCrawl` 공식 내 `grabButton.Pressed`
    가 `!Config.isFreeClimbingEnabled()` 와 조합 (L2407). 1.21.1 L586-L594 는
    crawlToggle 분기만 있고 grabButton.Pressed 조건 위치가 조금 다를 수 있음. 검증 필요.
    - 분류: [검증 필요] / B-N 추가 시.

**수정 범위**: §10 B-31~B-42 (12 원자 + B-31a~c 3 서브 = 최대 14).
**우선순위 권고 (A-5)**:
- 1순위: **B-31a~c** (미이식 필드 3건) — 다른 B-N 의존
- 2순위: **B-32** (canCrawl 5-AND 복원) — 1:1 원칙 즉시 수정 대상
- 3순위: **B-33** (메인 공식 재작성) — 가장 구조적 변경
- 4순위: **B-40** (toCrawling 헬퍼) — 중복 제거
- 5순위: **B-34+B-35** (capabilities 점프 + 전환 후처리)
- 6순위: **B-37+B-38** (Climber 수정 2건)
- 7순위: **B-39** (landMotionPost 3분기)
- 8순위: **B-41** (wantCrawlNotClimb) + **B-36** (grab 3분기) — 의존 필드 선행 필요
- 9순위: **B-42** (mustCrawl AABB — 별도 포커스 분리 권장)

### 세션 36 A-6 — 이력 3개 불일치 4건 확정

R-15 섹션 전수 덤프 기반. 3개 이력 필드 **이식 완료** 확인. 남은 불일치는 기존 B-N
원자에 대부분 흡수.

1. **R-09 블록 종료부 저장 2건 누락** (원본 L3043-L3044)
   - `wasRunning = isRunning` / `wasLevitating = isLevitating` 저장 없음
   - 의존 필드 자체 미이식 (A-4 B-22b wasRunning+isRunning / A-2 B-10d isLevitating)
   - 분류: [누락] / 수정 B-43 (의존 원자 선행).

2. **이력 필드 저장 시점 차이** (원본 공식 직전 vs 1.21.1 tickEssential 초반 일괄)
   - 원본: wasSneaking L2716 (isSlow 공식 L2717 직전), wasClimbCrawling L2786,
     wasCrawling L2441
   - 1.21.1: L560-L562 일괄 저장
   - 결과적 동치 (저장 ~ 공식 갱신 사이에 필드 변경 없음) 이나 1:1 구조 엄격 적용 시 이동
   - 분류: [구조 차이] / 수정 B-44a~c (B-2/B-18/B-33 수정 시 함께 조정).

3. **R-09 L788 wantSneak_/wantSprint_ 간소 매핑** — 기존 B-4 범위 (§16 세션 30 발견).

4. **미이식 필드 확장 인식** — A-6 감사 중 다른 `was*` 필드 미이식 재확인:
   - `wasHeadJumping` (A-4 B-22a)
   - `wasGroundSprinting` (A-1/B-1d 관련 — 원본 L2678)
   - `wasRunning` (A-4 B-22b)
   - `wasLevitating` (A-2 B-10d 파생)
   - `wasCrawling` tickEssential 전용 (A-5 B-31a)
   - 분류: 기존 B-N 범위.

**수정 범위**: B-43 + B-44a~c (4 신규) + 기존 B-4 / B-2 / B-18 / B-33 수정 시 저장
시점 이동 포함.

**우선순위 권고 (A-6)**:
- 1순위: **기존 B-N 선행 완료** — B-22b (wasRunning+isRunning) / B-10d (isLevitating) /
  B-2 (isSlow) / B-18 (isClimbCrawling) / B-33 (isCrawling 메인 공식) / B-4 (R-09 간소
  매핑 제거) 이 완료되어야 A-6 후속 원자 의미 생김
- 2순위: **B-43** (R-09 종료부 저장) — B-22b + B-10d 완료 후
- 3순위: **B-44a~c** (저장 시점 이동) — 각 기존 원자 수정 시 함께

### A 단계 완료 총괄 — B 단계 통합 우선순위 최종안

A 단계 6 세션 감사로 불일치 **70건** 확정 → B 단계 원자 약 **50개** (서브원자 포함 70+).

**B 단계 의존 그래프 기반 실행 순서** (권고):

```
Phase 1 (필드 선언 일괄):
  B-1a (Config._sprintEnableStanding)
  B-1b (Button ↔ KeyBinding 매핑 테이블)
  B-2 헬퍼 (Config.isSneakingEnabled)
  B-3a 헬퍼 (Config.isSprintingEnabled)
  B-8 헬퍼 (Config.isSwimmingEnabled/isDivingEnabled)
  B-10a~d (isShallowDiveOrSwim/isJumpingOutOfWater/isStillSwimmingJump/isLevitating)
  B-15a~f (isNeighborClimbing/hasClimbGap/.../edgeBlock)
  B-22a~d (wasHeadJumping/wasRunning/isRunning/isStanding)
  B-31a~c (wasCrawling/wantCrawlNotClimb/initializeCrawling)

Phase 2 (공식 이식 — 의존 필드 완성 후):
  B-1c (can* 4 판정)
  B-1d (6 Sprint 변종)
  B-1e (standing)
  B-1f (isFast 6-OR)
  B-3a (wantSprint 6조건 OR)
  B-3b (wouldIsSneaking 정정)
  B-16 (isClimbHolding/wantClimbHolding)
  B-17 (isCrawlClimbing 공식)
  B-18 (isClimbCrawling 공식 + 카운터)
  B-23 (isHeadJumping 매 틱 재평가)
  B-30 (isStanding 공식)
  B-32 (canCrawl 5-AND 복원)
  B-33 (isCrawling 메인 공식)
  B-40 (toCrawling 헬퍼)

Phase 3 (보조 블록):
  B-2 (isSlow 정정 — B-44a 포함)
  B-6 (Swimmer isClimbCrawling 조건)
  B-7 (updateSwimState 진입 조건)
  B-8 (Config 게이트)
  B-9 (메인 분류 3-갈래)
  B-11 (얕은 물 특수 분기)
  B-12 (waterMovementTicks 증분)
  B-13 (crawl↔swim 전환 isSliding)
  B-14 (resetClimbing 신설)
  B-19 (hasClimb* 갱신 로직)
  B-20 (Standard/Simple Base Climb)
  B-21 (isCeilingClimbing 해제)
  B-24 (해제 엣지 후처리)
  B-25 (isSliding 직접 진입 조건)
  B-26 (직접 진입 부수 동작)
  B-27 (fallDistance 분기)
  B-28 (handleClimbing isSliding 해제)
  B-29 (toSlidingOrCrawling 조건)
  B-34 (capabilities.flying 해제 점프)
  B-35 (wasCrawling↔isCrawling 전환 후처리)
  B-36 (grab.StartPressed 3분기)
  B-37 (wall 오르기 진입)
  B-38 (handleCeilingClimbing 해제)
  B-39 (landMotionPost 3분기)
  B-41 (wantCrawlNotClimb 갱신)
  B-43 (R-09 종료부 저장)
  B-44a~c (저장 시점 조정)

Phase 4 (정리):
  B-4 (R-09 간소 매핑 제거)
  B-5 (Swimmer 간소화 — B-9 에 흡수됨)
  B-42 (mustCrawl AABB — 별도 포커스 후보)

Phase 5 (C 단계):
  C-1 ./gradlew clean build
  C-2 §14 회귀 방지 감사
  C-3 checklist_original_audit.md 기록
  C-4 사용자 인게임 재검증
  C-5 playtest_fixes.md → #3
```

---

## 17. 잔여 / 후속

- 상태 디버그용 HUD 오버레이(F3 + Tab 같은) 추가는 별도 포커스 후보
- **~~남은 4 원자 별도 포커스 분리~~ (세션 85 결정) — 세션 88 엄격 완료 결정으로 취소**.
  * 사용자 지적: B-19 미이식 도미노 효과로 B-17/B-18 공식 항상 false 유지 → 포커스 #2
    "상태 플래그 정확성" 목적 사실상 미달성.
  * Phase 3~8 전 범위를 포커스 #2 내에서 엄격 완료 진행.
  * **후속 작업 전체는 [`focus_02_extended.md`](focus_02_extended.md) 에서 관리.**
  * 기존 분리 계획 (focus_14/15/16/17) 모두 취소 — Extended 가 대체.
- **~~B-42 → `focus_14_aabb_precision.md` 분리~~ (세션 79) — 세션 88 취소**: Extended Phase 6
  (AABB 정밀화) 으로 흡수. 1.21.1 `canStandUp(player)` /
  `isPlayerInSolidBetween` / `crawlStandUpBottom` 근사가 여러 원자에서 사용 중 (B-17b1 /
  B-35 / B-36 / B-39 등). 원본 `getMaxPlayerSolidBetween / getMinPlayerSolidBetween /
  getMinPlayerLiquidBetween` 정밀 AABB 스캔으로 일괄 개선할 수 있으나, 1.21.1 AABB API
  제약으로 설계 재검토 필요 — 포커스 #2 범위 외 작업. §7 에 현재 근사 지점 전수 등록 (B-5/
  B-16/B-20/B-26/B-35/B-36 등). 후속 포커스에서 AABB 정밀도 개선 시 이들 근사가 일괄
  해소될 수 있음.
- Simple/Smart Base Climb (원본 L825-L894) 전체 미이식 — B-20 세션 76 에서 Standard 만
  일부 해소. 옵션 모드 전체 이식은 Climber 구조 변경 큼 — 후속 포커스 후보.
