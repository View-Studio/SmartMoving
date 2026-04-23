# Focus #2 — 스마트무빙 상태 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #2 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 진행 중 (세션 31 — B-0 완료) |
| 현재 단계 | A-0/A-1/B-0 완료 / ⏳ **A-2 (수중 3상태 isSwimming_sm/isDiving/isDipping)** |
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

## 6. 1:1 매핑 테이블

(재현 케이스 확보 후 구체 필드/메서드별로 작성)

| 원본 심볼 | 1.21.1 심볼 | 상태 |
|---|---|---|
| `isDipping` | `SmartMovingClientState.isDipping` | ✓ |
| `isSwimming` (원본) | `isSwimming_sm` (vanilla 충돌 회피 접미사) | ✓ |
| `isDiving` | `isDiving` | ✓ |
| `dippingDepth` | `dippingDepth` | ✓ |
| ... | ... | (재현 케이스별 확장) |

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
- [ ] A-2. **`isSwimming_sm` / `isDiving` / `isDipping`** 원본 덤프 (수중 3상태)
- [ ] A-3. **`isClimbing` / `isCeilingClimbing` / `isCrawlClimbing` / `isClimbCrawling`** 원본 덤프 (등반 계열 4상태)
- [ ] A-4. **`isHeadJumping` / `isSliding`** 원본 덤프 (전환 쌍)
- [ ] A-5. **`isCrawling` + `contextContinueCrawl`** 원본 덤프 (가장 복잡, 종합 의존)
- [ ] A-6. **`wasCrawling_st` / `wasSneaking` / `wasClimbCrawling`** 원본 스냅샷 위치 확인
- [ ] A-7. 각 A-1~A-6 그룹별 1.21.1 grep + side-by-side 매핑 테이블 작성

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

#### B-N. A-2~A-6 추가 발견에 따라 동적 추가

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

---

## 17. 잔여 / 후속

- 상태 디버그용 HUD 오버레이(F3 + Tab 같은) 추가는 별도 포커스 후보
