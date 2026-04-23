# Focus #2 — 스마트무빙 상태 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #2 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 진행 중 (세션 38 — B Phase 1 일부) |
| 현재 단계 | A 완료 + B-22a/b/d + B-10a/b/c + B-31b/c (필드 선언 8건) 완료 / ⏳ **Phase 1 잔여 (B-22c / B-31a / B-1a / B-2 헬퍼 / B-3a 헬퍼 / B-8 헬퍼 / B-15a~f 등반 9건)** |
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
| `wasCrawling` (필드 L3074, 다용도) | `wasCrawling_st` (L244, R-09 전용) | ⚠️ 부분 이식 (다용도 필드 별도 신설 필요) | B-31a + B-44b |
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
| `isRunning` (필드 + override L3241) | — (로컬만 L995) | ✗ 필드 미이식 | **B-22c** |
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
| `Config.isSneakingEnabled()` | — | ✗ 헬퍼 신설 (B-2) |
| `Config.isSprintingEnabled()` | — | ✗ 헬퍼 신설 (B-3a) |
| `Config.isSwimmingEnabled()` | — | ✗ 헬퍼 신설 (B-8) |
| `Config.isDivingEnabled()` | — | ✗ 헬퍼 신설 (B-8) |
| `Config.isLavaLikeWaterEnabled()` | — | ✗ 미이식 (B-7) |
| `Config.isFreeClimbingEnabled()` | `cfg.freeClimb` (확인) | ✓? |
| `Config.isSmartBaseClimb()` / `isSimpleBaseClimb()` / `isStandardBaseClimb()` | — | ✗ 미이식 (B-20) |
| `Config.isSlidingEnabled()` | `cfg.slide && cfg.enabled` | ✓ |
| `Config.isFlyingEnabled()` | `cfg.fly && cfg.enabled` | ✓ |
| `Config.isLevitateSmallEnabled()` | — | ✗ 미이식 |

### 6.8 Button ↔ KeyBinding 매핑 (B-1b)

| 원본 `Button` | 1.21.1 대응 | 상태 |
|---|---|---|
| `grabButton.Pressed` | `SmartMovingKeys.grab.isPressed()` | ✓ |
| `grabButton.StartPressed` | `SmartMovingKeys.grab.wasPressed()` (엣지 근사) | ⚠️ 엣지 검출 정합성 확인 |
| `grabButton.StopPressed` | — | ✗ 엣지 검출 미이식 |
| `sneakButton.Pressed` | `MinecraftClient.options.sneakKey.isPressed()` 또는 `player.isSneaking()` | ✓ |
| `sneakButton.StartPressed` | `sneakKeyStartPressed` 필드 (이전 틱 비교) | ✓ |
| `sneakButton.StopPressed` | `sneakKeyStopPressed` 필드 | ✓ |
| `sprintButton.Pressed` | `MinecraftClient.options.sprintKey.isPressed()` | ⚠️ 확인 필요 |
| `sprintButton.StopPressed` | — | ✗ 엣지 미이식 |
| `jumpButton.Pressed` | `MinecraftClient.options.jumpKey.isPressed()` | ✓ |
| `jumpButton.StartPressed` | — | ✗ 엣지 검출 미이식 |
| `jumpButton.StopPressed` | `jumpKeyStopPressed` 필드 | ✓ |
| `moveForwardButton` | `player.input.movementForward > 0F` | ✓ |
| `moveBackwardButton` | `< 0F` | ✓ |
| `moveLeftButton` / `moveRightButton` | `player.input.movementSideways` | ✓ |

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
| `toCrawling()` 함수 | — (inline 만) | ✗ 헬퍼 미이식 (B-40) |
| `resetClimbing()` 메서드 | — (Mixin 주석만) | ✗ 미이식 (B-14) |
| `resetSwimming()` 메서드 | Swimmer `updateSwimState` 물 밖 분기 (부분) | ⚠️ 부분 이식 |
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
- [ ] B-1a. `Config._sprintEnableStanding` 1.21.1 `SmartMovingConfig` 이식 확인
      (원본 `SmartMovingConfig.java` L313 `Unmodified("move.sprint.enable.ground")`).
      필드 grep → 미이식 시 신설 + `SmartMovingProperties` 확장.
- [ ] B-1b. Button 클래스 ↔ 1.21.1 KeyBinding 매핑 테이블 §6 에 작성:
      `sprintButton` / `jumpButton` / `grabButton` / `sneakButton` /
      `moveForwardButton` / `moveBackwardButton` / `moveLeftButton` / `moveRightButton` —
      각 `.Pressed` / `.StartPressed` / `.StopPressed` 의 1.21.1 대응 (`isPressed()` /
      `wasPressed()` / `SmartMovingKeys.*` 엣지 검출 필드). 기존 구현 grep 으로 확인.
- [ ] B-1c. `canHorizontallySprint` / `canAllSprint` / `canAnySprint` / `isClimbSprintSpeed`
      4 판정 ClientState 이식 — 원본 선언 + 계산 블록 1:1.
- [ ] B-1d. 6 Sprint 변종 (`isGroundSprinting` / `isClimbSprinting` / `isSwimSprinting` /
      `isDiveSprinting` / `isCeilingSprinting` / `isFlyingSprinting`) ClientState 이식
      — 원본 선언 + 계산 블록 1:1.
- [ ] B-1e. `standing = onGround && !isSliding && !isCrawling` 로컬 변수 이식
      (`tickEssential` isFast 계산 직전).
- [ ] B-1f. `SmartMovingClientState.tickEssential` L653 `isFast` 공식 6갈래 OR 로 교체
      (원본 `SmartMovingSelf` L2688-L2695, `isClimbSprinting` 중복 1:1 보존).

#### B-2. `isSlow` 공식 정정 (단일 원자)
- [ ] B-2. `SmartMovingClientState.tickEssential` L650-L651 정정 (원본 L2588-L2590 / L2718):
      - `wantSneak = Config.isSneakingEnabled() && wouldWantSneak` 신설
      - `Config.isSneakingEnabled()` 1.21.1 매핑은 `cfg.sneak && cfg.enabled` 로 원자 B-2
        범위 내 헬퍼 메서드 신설 (Config 에 `boolean isSneakingEnabled()`)
      - `isSlow = wantSneak && wouldIsSneaking`
      - 기존 `sneakContinueInput` 중복 곱 제거
      - ※ `wouldIsSneaking` 은 B-3b 에서 처리 — B-2 는 `isSlow` 우변만 교체

#### B-3. `wantSprint` 신설 + `wouldIsSneaking` 정정 (원자 2개 분해)
- [ ] B-3a. `wantSprint` 필드 + 6조건 OR 계산 블록 이식 (원본 `SmartMovingSelf` L2595-L2615).
      의존 필드 전수 확인 + 미이식 시 신설:
      - `Config.isSprintingEnabled()` 1.21.1 매핑 (`cfg.sprint && cfg.enabled`) 헬퍼 메서드
      - `sprintButton.Pressed` (B-1b 매핑 결과 사용)
      - `moveForwardButtonPressed` / `moveButtonPressed` / `jumpButton` (B-1b 매핑)
      - `disabled` (ClientState 또는 Config 필드 존재 여부 확인)
      - `isFlying` / `isSliding` / `isClimbing` / `isSwimming_sm` / `isDiving`
        (ClientState 기존 필드)
- [ ] B-3b. `SmartMovingClientState.tickEssential` L650 `wouldIsSneaking` 정정:
      `wouldWantSneak && !wantSprint && !isClimbing` (원본 L2712). vanilla `isSprinting()`
      단순 대체 제거.

#### B-4. R-09 토글 블록 `wantSneak_/wantSprint_` 간소 매핑 제거 (§16 세션 30 신규)
- [ ] B-4. `SmartMovingClientState.tickEssential` L788-L792 "간소 매핑" 주석 + 로컬 변수
      `wantSneak_` / `wantSprint_` 를 B-2/B-3 에서 확정된 필드(`wantSneak` / `wantSprint`)로
      교체. 원본 L2986 참조. 간소 매핑 주석 제거.

#### B-5. `SmartMovingSwimmer.java` L159 "1.21.1 간소화" 원본 대조 (§16 세션 30 신규)
- [ ] B-5. `SmartMovingSwimmer.java` L159 간소화 지점 원본 `handleSwimming` 과 side-by-side
      대조 → 불일치면 정정 원자 추가 (B-5a~). 1:1 이면 주석만 제거.

#### B-6. Swimmer `updateSwimState` L78 — `isClimbCrawling` 누락 조건 추가 (A-2 발견)
- [ ] B-6. 원본 L301 3-OR (`isCrawling || isClimbCrawling || isCrawlClimbing`) 에 맞게
      Swimmer L78 조건에 `sm.isClimbCrawling` 추가.

#### B-7. Swimmer `updateSwimState` 진입 조건 복원 (A-2 발견)
- [ ] B-7. 원본 L232 진입 조건 (`!isFlying && !isLiquidClimbing && (isInWater || (wasSwimming
      && isInLiquid) || (Config.isLavaLikeWaterEnabled() && handleLavaMovement()))`) 복원.
      의존 확인: `isLiquidClimbing` / `isInLiquid()` / `Config.isLavaLikeWaterEnabled()` —
      미이식 시 각각 신설 원자 분해 (B-7a/b/c).

#### B-8. `Config.isSwimmingEnabled() / isDivingEnabled()` 게이트 추가 (A-2 발견)
- [ ] B-8. Config 에 `isSwimmingEnabled()` / `isDivingEnabled()` 헬퍼 메서드 신설
      (`cfg.swim && cfg.enabled` / `cfg.dive && cfg.enabled` — B-2/B-3a 와 동일 패턴) +
      `updateSwimState` 에서 게이트 적용 (원본 L239/L438-L440 대응).

#### B-9. 메인 분류 공식 재작성 (A-2 발견 — 가장 큰 수정)
- [ ] B-9. 원본 L303-L414 3-갈래 메인 분류 이식:
      - (a) playerSwimWaterBorder/totalSwimWaterBorder 계산 (AABB 근사 또는 getFluidHeight 활용)
      - (b) `[0, 2]` 구간 A/B 서브 분기 (`diveUp || moveSwim || wantShallowSwim`)
      - (c) A 경로 11-단계 swimming offset 테이블 (1.4~1.9)
      - (d) B 경로 10-단계 diving offset 테이블 (1.5~1.9)
      - (e) `(2, ∞)` 구간 항상 diving + diveUp/diveDown/moveSwim + isFast 분기
      - (f) `(-∞, 0)` handleSwimmingRejected
      - (g) motionYDiff 전체 적용 로직
      - ※ 규모 매우 큼 — 세션 여러 회 분할 권장 (B-9a~g 서브원자 신설 가능)

#### B-10. 미이식 필드 4건 ClientState 이식 + 갱신 로직 (A-2 발견)
- [~] B-10a. **필드만 이식 완료 (세션 38)** — `isShallowDiveOrSwim` 필드 ClientState 추가.
      공식 갱신 (`couldStandUp && (isDiving || isSwimming_sm)` 원본 L507) 은 B-9 수정 시 이식.
- [~] B-10b. **필드만 이식 완료 (세션 38)** — `isJumpingOutOfWater` 필드 추가. 조건 이식
      (wantJumpOutOfWater + waterMovementTicks>10 원본 L486-L487) 은 B-12 수정 시.
- [~] B-10c. **필드만 이식 완료 (세션 38)** — `isStillSwimmingJump` 필드 추가. false 리셋
      (useStandard 경로 원본 L550) 은 B-9 수정 시.
- [ ] B-10d. `isLevitating` **필드는 이미 L179 에 존재** — 공식 이식 `diving && !diveUp &&
      !diveDown && moveStrafe==0 && moveForward==0` 원본 L474/L505 만 남음. B-9 범위로 이전.

#### B-11. 얕은 물 특수 분기 이식 (A-2 발견)
- [ ] B-11. 원본 L513-L536 이식 — `isShallowDiveOrSwim && realMinPlayerSwimWaterDepth <
      SwimCrawlWaterBottomBorder` 진입 조건 + isSlow 분기 (crawl 전환 / walking 전환).
      AABB 근사 판정 필요 (`realMinPlayerSwimWaterDepth` 대응).

#### B-12. `waterMovementTicks` 증분 조건 정정 (A-2 발견)
- [ ] B-12. 원본 L481-L484 — `swimming || diving` 만 증분, else (dipping 포함) 0 리셋.
      1.21.1 `updateSwimState` L82/L90 은 dipping 포함 증분 → 원본대로 정정. B-10b
      `isJumpingOutOfWater` 의존.

#### B-13. 크롤↔수영 전환 조건 `isSliding` 추가 (A-2 발견)
- [ ] B-13. `SmartMovingSwimmer.handleSwimming` L119/L124 SwimCrawlWater 전환 조건에
      원본 L418 `(isCrawling || isSliding)` 반영. 현재 `wasCrawling` 만 체크.

#### B-14. `resetClimbing()` 메서드 신설 + handleClimbing 진입 시 호출 (A-3 발견)
- [ ] B-14. 원본 L1474-L1486 `resetClimbing()` 이식 — `isClimbing` / `isHandsVineClimbing` /
      `isFeetVineClimbing` / `isVineOnlyClimbing` / `isVineAnyClimbing` / `isClimbingStill` /
      `isNeighborClimbing` / `actualHandsClimbType` / `actualFeetClimbType` /
      `isCeilingClimbing` 10 필드 리셋. `SmartMovingClimber.handleClimbing` 진입 시 호출
      (원본 L816). 이식된 필드만 먼저 리셋, 미이식 필드는 B-15 이후.

#### B-15. 미이식 등반 필드 9건 ClientState 이식 (A-3 발견)
- [ ] B-15a. `isNeighborClimbing` 필드 추가 (원본 L1426)
- [ ] B-15b. `hasClimbGap` 필드 추가 (원본 L1427) — B-18 선행 의존
- [ ] B-15c. `hasNeighborClimbGap` / `hasNeighborClimbCrawlGap` 필드 (원본 L1429-L1430)
- [ ] B-15d. `isVineOnlyClimbing` / `isVineAnyClimbing` 필드 (원본 L1421-L1422)
- [ ] B-15e. `isClimbingStill` 필드 (원본 L1424)
- [ ] B-15f. `handsEdgeBlock` / `feetEdgeBlock` / edgeMeta 필드 (원본 L1443-L1446) — 애니메이션 참조

#### B-16. `wantClimbHolding` / `isClimbHolding` 갱신 블록 이식 (A-3 발견)
- [ ] B-16. 원본 L2721-L2732 3-OR 공식 이식:
      `wantClimbHolding = (isClimbHolding && sneakPressed) || (isClimbing && blocked) ||
      (wantClimb && !isSwimming && !isDiving && !isCrawling && (sneakPressed || crawlToggled))`
      → `isClimbHolding = wantClimbHolding && isClimbing`.
      의존: `wantClimb` / `blocked` 필드 확인.

#### B-17. `isCrawlClimbing` 메인 공식 + 전환 블록 이식 (A-3 발견)
- [ ] B-17. 원본 L2736-L2754 이식:
      `isCrawlClimbing = (wasCrawling || isCrawlClimbing) && isClimbing && isNeighborClimbing
      && (sneakPressed || crawlToggled) && moveForward > 0F`
      + canStandUp 분기 (isPlayerInSolidBetween 근사 필요) + wasCrawlClimbing 전환 분기.
      의존: B-15a `isNeighborClimbing` 선행 필수.

#### B-18. `isClimbCrawling` 메인 공식 + climbIntoCount 카운터 이식 (A-3 발견)
- [ ] B-18. 원본 L2786-L2820 이식:
      - `needClimbCrawling = hasClimbCrawlGap || (hasClimbGap && isClimbHolding)` (B-15b/B-16 선행)
      - `canClimbCrawling = wantClimbHolding && wantClimbUp`
      - climbIntoCount 카운터: `>1`감소 / `isClimbCrawling&&!needClimbCrawling&&count==0`→`6` 재장전
      - `isClimbCrawling = canClimbCrawling && ((needClimbCrawling && count==0) || count>1)`
      - 진입 엣지 (setHeightOffset(-1) + move(0,0.05,0))
      - 해제 엣지 (mustCrawl/sneak 상황별 crawl 전환 + resetHeightOffset)

#### B-19. `hasClimbCrawlGap` / `hasClimbGap` / `isNeighborClimbing` 갱신 로직 이식 (A-3 발견)
- [ ] B-19. 원본 handleClimbing Free Climbing 분기 (L896-L1108) 내부 Orientation 판정
      → hasClimbCrawlGap / hasClimbGap / isNeighborClimbing 계산. 규모 큼 — 서브원자
      분해 (B-19a: Orientation 4방향 판정 / B-19b: ClimbGap out 파라미터 / ...).

#### B-20. Standard / Simple Base Climb 이식 (A-3 발견)
- [ ] B-20. 원본 L820-L844 이식 — Config.isStandardBaseClimb / isSimpleBaseClimb 분기.
      motionY 직접 설정 (FastUpMotion / SlowUpMotion 상수 이식). isClimbing 설정 안 함
      (vanilla ladder 물리 위임). 1.21.1 은 Free 만 이식되어 있어 옵션 분기 전체 미이식.

#### B-21. `isCeilingClimbing` 해제 엣지 이식 (A-3 발견)
- [ ] B-21. 원본 L1485 resetClimbing 에서 false 설정. B-14 완료 후 자동 해결.

#### B-22. 미이식 필드 4건 이식 (A-4 발견)
- [x] B-22a. ✅ **세션 38 완료** — `wasHeadJumping` 필드 추가 + resetState 리셋.
- [x] B-22b. ✅ **세션 38 완료** — `wasRunning` 필드 추가 + resetState 리셋.
- [ ] B-22c. `isRunning` 필드 추가 (현재 로컬 변수 → 필드 승격) + `isRunning()` override
      이식 (원본 L3241 `isSprinting() && !isFast && (onGround || vanilla())`) — 별도
      세션 (로컬→필드 승격은 handleExhaustion 수정 포함).
- [x] B-22d. ✅ **세션 38 완료** — `isStanding` 필드 추가 + resetState 리셋. 갱신 공식
      L2734 은 B-30 범위.

#### B-23. `isHeadJumping` 매 틱 재평가 5-AND 공식 이식 (A-4 발견)
- [ ] B-23. 원본 L2524-L2530 tickEssential 에 이식:
      `wasHeadJumping = isHeadJumping;`
      `isHeadJumping = isHeadJumping && !onGround && !(swim||dive) && !(flying||capabilities.flying) && !(waterMovement && motionY<0) && !lavaMovement`
      + `if (!isHeadJumping) isAerodynamic = false;` (기존 이식됨, 재평가 위치 확인)
      의존: B-22a `wasHeadJumping` 선행.

#### B-24. 해제 엣지 후처리 이식 (A-4 발견)
- [ ] B-24. 원본 L2535-L2540 이식 — `wasHeadJumping && !isHeadJumping && onGround` 시:
      - `handleCrash(_headFallDamageStartDistance, _headFallDamageFactor)` 호출
      - `restoreFromFlying = true` 설정 → standupIfPossible 트리거
      의존: B-22a + B-23 선행.

#### B-25. `isSliding` 직접 진입 6-AND 조건 복원 (A-4 발견)
- [ ] B-25. ClientState L699 `wantSlide` 조건 원본 L2553 으로 정정:
      `Config.isSlidingEnabled() && grabPressed && (isGroundSprinting || (wasRunning && !isRunning && onGround)) && !isCrawling && sneakStartPressed && !isDipping`
      의존: B-1d `isGroundSprinting` + B-22b `wasRunning` + B-22c `isRunning` 선행.
      `sneakStartPressed` 엣지 검출 — 기존 `sneakKeyStartPressed` 활용.

#### B-26. 직접 진입 부수 동작 이식 (A-4 발견)
- [ ] B-26. 원본 L2555-L2560 부수 동작 이식:
      - `heightOffset = -1F` 설정
      - `move(0, -1D, 0)` — 1 블록 하강 (Box collision 체크 포함)
      - `tryJump(Config.SlideDown, false, wasRunning, null)` 호출
      - `isSliding = true; isHeadJumping = false; isAerodynamic = false`
      의존: B-25 선행. `Config.SlideDown` 상수 1.21.1 이식 여부 확인 필요.

#### B-27. `fallDistance > _fallingDistanceMinimum` 분기 이식 (A-4 발견)
- [ ] B-27. 원본 L2569-L2574 이식:
      `if (isSliding && fallDistance > cfg.fallingDistanceMinimum) {
          isSliding = false; wasCrawling = true; isCrawling = false; }`
      `_fallingDistanceMinimum` Config 필드 1.21.1 이식 확인.

#### B-28. handleClimbing 진입 시 `isSliding=false` 이식 (A-4 발견)
- [ ] B-28. 원본 L985 대응 — 클라이밍 진입 시 슬라이딩 해제. B-14 resetClimbing 에
      `isSliding=false` 포함하거나 별도 원자로 SmartMovingClimber.handleClimbing 진입부 추가.

#### B-29. `toSlidingOrCrawling` 조건 정정 (A-4 발견)
- [ ] B-29. Jumper L103 조건 원본 L2226 으로 정정:
      `Config.isSlidingEnabled() && (grabPressed || wasHeadJumping)` → isSliding = true.
      의존: B-22a `wasHeadJumping` 선행.

#### B-30. `isStanding` 갱신 공식 이식 (A-4 발견)
- [ ] B-30. tickEssential 에 원본 L2734 공식 추가:
      `isStanding = horizontalSpeedSquare < 0.0005` (horizontalSpeedSquare = motionX² + motionZ²).
      의존: B-22d `isStanding` 필드 선행.

#### B-31. 미이식 필드 3건 이식 (A-5 발견)
- [ ] B-31a. `wasCrawling` 필드 ClientState 추가 (wasCrawling_st 와 구분 — tickEssential
      이전 틱 저장 전용, L2441 대응). **B-33 메인 공식 재작성 시 wasCrawling_st 와 용도
      통합 검토**.
- [~] B-31b. **필드만 이식 완료 (세션 38)** — `wantCrawlNotClimb` 필드 추가. L2452-L2461
      갱신 블록은 B-41 범위.
- [~] B-31c. **필드만 이식 완료 (세션 38)** — `initializeCrawling` 필드 추가. 관련 로직은
      B-35 전환 후처리 범위.

#### B-32. `canCrawl` 공식 원본 5-AND 복원 (A-5 발견 — 1:1 원칙 위배)
- [ ] B-32. ClientState L669-L672 조건 원본 L2434-L2439 로 정정:
      `!isSwimming_sm && !isDiving && (!isDipping || (dippingDepth + heightOffset) <
      SwimCrawlWaterTopBorder) && !isClimbing && fallDistance < _fallingDistanceMinimum`
      — 9-AND 잉여 조건 (isCrawlClimbing / isCeilingClimbing / isSliding / isHeadJumping /
      isFlying) 제거.

#### B-33. 메인 공식 재작성 — 매 틱 재계산 구조 (A-5 발견)
- [ ] B-33. IMPL-01 (L661-L693) 진입/해제 이원화 → 원본 매 틱 공식으로 전환:
      `wasCrawling = isCrawling;`
      `isCrawling = canCrawl && (wantCrawl || mustCrawl);`
      `if (!isCrawling) contextContinueCrawl = false;`
      (L2446-L2447 은 이미 L622 에 이식됨)
      IMPL-01 의 진입 엣지 처리 (crawlToggled 설정 + ignoreNextStopSneakButtonPressed) 는
      B-40 toCrawling() 헬퍼로 이전.
      의존: B-31a `wasCrawling` / B-32 canCrawl 정정 선행.

#### B-34. capabilities.flying 해제 점프 이식 (A-5 발견)
- [ ] B-34. 원본 L2449-L2450 이식 — `wasCrawling && !isCrawling && capabilities.flying
      → tryJump(Config.Up, null, null, null)`. 의존: B-31a 선행.

#### B-35. wasCrawling↔isCrawling 전환 후처리 이식 (A-5 발견)
- [ ] B-35. 원본 L2822-L2836 이식 — 두 방향 전환 시:
      - `wasCrawling && !isCrawling && !initializeCrawling && !flying` →
        resetHeightOffset + `move(0, crawlStandUpBottom - minY, 0)`
      - `(isCrawling && !wasCrawling) || initializeCrawling` →
        `setHeightOffset(-1F)` + `move(0, -1D, 0)` + (initializeCrawling 이면 toCrawling)
      의존: B-31a/B-31c + B-40 toCrawling 헬퍼 선행.

#### B-36. grab.StartPressed 수영/크롤 3분기 이식 (A-5 발견)
- [ ] B-36. 원본 L2838-L2861 이식 — grab 엣지 3분기:
      (a) isShallowDiveOrSwim + wouldWantClimb → walking 전환
      (b) isDipping + wouldWantCrawl + depth>=BottomBorder + depth>=MediumBorder → 수영/다이빙 전환
      (c) isDipping + wouldWantCrawl + depth>=BottomBorder + depth<MediumBorder → 얕은 물 크롤
      의존: B-10a `isShallowDiveOrSwim` / B-10c `isStillSwimmingJump` + `wouldWantClimb` 선행.

#### B-37. handleClimbing wall 오르기 crawl 진입 이식 (A-5 발견)
- [ ] B-37. 원본 L985-L986 이식 — Climber.handleClimbing wantClimbUp + handsClimbing
      IsRelevant 분기에 `isSliding=false; isCrawling=true` 추가.

#### B-38. handleCeilingClimbing 진입 시 isCrawling=false 이식 (A-5 발견)
- [ ] B-38. 원본 L1170 대응 — Climber.handleCeilingClimbing L571 isCeilingClimbing=true
      이후 `sm.isCrawling = false` 추가.

#### B-39. landMotionPost 3분기 (isSlow + 0.5D) 이식 (A-5 발견)
- [ ] B-39. 원본 L1392-L1403 3분기 (`crawlStandUpBottom > minY`) 중 **isSlow && > minY+0.5D
      → crawling** 서브 분기 ClientState fromSwimmingOrDiving 에 추가 (현재 2분기만 이식).

#### B-40. `toCrawling()` 헬퍼 메서드 신설 + 호출 지점 정리 (A-5 발견)
- [ ] B-40. 원본 L3047-L3054 함수 이식 — `boolean toCrawling()` (ClientState 메서드):
      `{ isCrawling = true; if (cfg.crawlToggle) crawlToggled = true;
         ignoreNextStopSneakButtonPressed = true; return true; }`
      기존 inline 3줄 (IMPL-01 L674-L677) + B-35/B-36 에서 사용.

#### B-41. `wantCrawlNotClimb` 갱신 블록 이식 (A-5 발견)
- [ ] B-41. 원본 L2452-L2461 이식 — grab.StartPressed + !wasCrawling 등 4-AND 조건.
      의존: B-31b 필드 선행.

#### B-42. `mustCrawl` AABB 정밀 개선 (A-5 발견 — 근사 이식 기록)
- [ ] B-42. 1.21.1 `canStandUp(player)` 메서드 근사 → 원본 `getMaxPlayerSolidBetween /
      getMinPlayerSolidBetween` 정밀 AABB 근사. 1.21.1 AABB API 제약으로 §7 근사 유지
      가능 — 별도 포커스 후보 (focus_??? 분리).

#### B-43. R-09 블록 종료부 저장 2건 이식 (A-6 발견)
- [ ] B-43. 원본 L3043-L3044 이식 — `ClientState.tickEssential` L839 이후 R-09 블록
      종료부에 추가:
      `wasRunning = isRunning;`
      `wasLevitating = isLevitating;`
      의존: A-4 B-22b (wasRunning+isRunning) / A-2 B-10d (isLevitating+wasLevitating) 선행.

#### B-44. 이력 3개 저장 시점 정밀 조정 (A-6 발견)
- [ ] B-44a. `wasSneaking = isSlow` 저장 L560 → isSlow 공식 직전 (원본 L2716 대응).
      B-2 (isSlow 정정) 수정 시 함께 조정.
- [ ] B-44b. `wasCrawling_st = isCrawling` 저장 L561 → isCrawling 공식 직전 (원본 L2441
      대응). B-33 (A-5 메인 공식 재작성) 수정 시 함께 조정.
- [ ] B-44c. `wasClimbCrawling = isClimbCrawling` 저장 L562 → isClimbCrawling 공식 직전
      (원본 L2786 대응). B-18 (A-3 isClimbCrawling 이식) 수정 시 함께 조정.

#### B-N. A-7 이후 추가 발견에 따라 동적 추가

### C. 검증
- [ ] C-1. `./gradlew clean build` 성공
- [ ] C-2. §14 회귀 방지 감사 (상태 소비처 — 애니메이션/전환/키 커맨드 영향 확인)
- [ ] C-3. checklist_original_audit.md 에 포커스 #2 결과 기록
- [ ] C-4. 사용자 인게임 재검증 (§3 재현 케이스 실제 채워지면 매칭 확인)
- [ ] C-5. `playtest_fixes.md` "현재 포커스" → `#3` 갱신

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

- [ ] §3 표의 모든 케이스가 "예상 == 실제" 매칭
- [ ] 각 수정이 리서치 파일 원본 라인과 1:1 대응
- [ ] 신규 발견 등록
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| R-09 토글 블록 | `wasSneaking` / `wasCrawling_st` 저장 시점 변경하면 토글 블록 깨짐 | [ ] |
| `wouldWantSneak` / `wouldWantCrawl` | 참조 필드 의미 변경 시 연쇄 영향 | [ ] |
| `sendStatePacket` / `processStatePacket` | State 비트 매핑 변경하면 다른 플레이어 렌더 깨짐 | [ ] |
| `sm_isSneaking` override | isSlow → isSneaking 순환 주의 | [ ] |

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

---

## 16. 신규 발견

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
