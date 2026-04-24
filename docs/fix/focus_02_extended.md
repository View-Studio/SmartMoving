# Focus #2 Extended — 엄격 완료 (Phase 3~8)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스 #2 Extended 일 때 진입.
> 본체: [`focus_02_state_issues.md`](focus_02_state_issues.md) — A/B Phase 1/2 핵심 +
> C-1/C-2/C-3 완료 기록.

---

## 1. 목적

**엄격 완료 결정** (사용자 지시, 세션 88 2026-04-24):

세션 85 에서 잔여 4 원자 + §7 근사 + 부분 이식 서브를 "별도 포커스 분리" 로 결정했으나,
실제로는 의존 필드 미이식으로 **이식된 공식이 실질적으로 무력화**되는 도미노 효과 발견:

- **B-19 미이식 → `isNeighborClimbing` / `hasClimbGap` / `hasClimbCrawlGap` 항상 false**
  → 세션 47/48/68/69/72/73/77/81/83 에서 이식한 B-17a/b1/b2 (isCrawlClimbing) +
  B-18/B-18-pre/B-18b (isClimbCrawling) + B-16c (wouldWantClimb 4-OR) 공식이 **모두 false**
  유지 → 포커스 #2 "13 상태 플래그 값 자체의 정확성" 목적 미달성
- **B-10a 공식 미이식 → `isShallowDiveOrSwim` 항상 false**
  → B-36 분기 (a) (얕은 물 swim/dive → walking) 항상 비활성
- **B-9 미이식 → swim 경계값 offset 11+10 단계 테이블 전부 근사**
  → 수영/다이빙 수심 경계 정확도 낮음
- **§7 AABB 근사 8건** → B-5/B-16/B-20/B-26/B-35/B-36/B-39/B-18 모두 근사 유지

**결론**: 분리 포커스는 사실상 범위 축소. 사용자 엄격 완료 방침으로 **Phase 3~8 범위
복원** — 원본 1:1 완전 이식 추구.

---

## 2. 본체 focus_02 와 관계

| 측면 | focus_02 본체 | focus_02_extended (이 문서) |
|------|--------------|----------------------------|
| A 단계 감사 | §5/§10 A-0~A-7 완료 | 동일 근거 참조 |
| B Phase 1 필드 일괄 | 완료 기록 | 동일 |
| B Phase 2 공식 54 원자 | §10 체크박스 완료 | 동일 |
| Phase 3~8 신규 범위 | §10 에 참조 링크만 | **본 문서 §3 에 전체** |
| 작업 기록 (세션 88+) | 세션 88 메타 결정만 | §5 세션 88+ 상세 |
| C 단계 | C-1/C-2/C-3 완료 / C-4/C-5 는 Phase 3~8 완료 후 | 본 문서 §6 최종 C-4/C-5 |
| §7 근사 | 기존 8건 기록 유지 | Phase 6 완료 시 해소 기록 |
| §16 신규 발견 | 기존 유지 | Phase 7 해소 기록 |

**문서 경량화**: focus_02 본체는 4832 줄로 이미 크므로 Phase 3~8 전용 기록을 Extended 로
분리. 참조 중복 최소화.

---

## 3. Phase 3~8 원자 목록

### Phase 3. B-19 + 의존 도미노 해소 (최우선)

`isCrawlClimbing` / `isClimbCrawling` / wouldWantClimb 자동 등반 활성화의 핵심.
원본 SmartMovingSelf.java `handleClimbing` Free Climbing 분기 (L896-L1108) 내부
Orientation 판정 + ClimbGap 계산.

#### B-19a. Climber Orientation 4방향 판정 로직 이식 (서브 원자 5개로 재분해 — 세션 89)

**세션 89 재평가 결과** (Agent WebFetch 로 원본 `Orientation.java` 2860줄 확보 후):
- 원본 `seekClimbGap` (L207-L224) 은 `handsClimbing()`/`feetClimbing()` 호출만 — 실체는
  **`isLadderSubstitute()` (L477-L606) 내부의 gap 정밀 계산 + `ClimbGap.CanStand/MustCrawl`
  설정 (L601-L602)** 에 있음.
- `isLadderSubstitute` 는 `hasHalfHold` / `hasBottomHold` / `isBaseAccessible` /
  `isFullAccessible` / `isFullExtentAccessible` / `isJustLowerHalfExtentAccessible` /
  `isOnLadderOrVine` / `isOnOpenTrapDoor` / `isRope` / `isOnWallRope` 등 10+ 의존 헬퍼를
  씀. 대부분 `SmartMovingContext` 에 있음.
- 1.21.1 `ClimbGap.canStand`/`mustCrawl` 은 **설정 로직 자체 부재** — `reset()` 에서 false
  고 그대로. 따라서 Climber 4방향 탐색 결과가 ClientState 에 반영되더라도 canStand=
  mustCrawl=false 라서 효과 없음.
- 결론: 단일 원자로 한 세션 불가. 아래 5개 서브로 분해.

**원본 근거**: `.tmp_research/Orientation.java.md` (WebFetch 저장, 2860줄).

- [ ] **B-19a0. `Orientation` 클래스 기본 구조 신설** (원본 Orientation L36-L205)
      * 9 상수 (ZZ/PZ/NZ/ZP/ZN/PP/NN/PN/NP) + `_i`/`_k` 필드 + 생성자
      * `isWithinAngle` / `isRotationForClimbing` / `getKnownLadderOrientation` / `addTo`
      * `getHorizontalBorderGap` / `isTunnelAhead` / `getClimbingOrientations` (정적 헬퍼)
      * 1.21.1 `choco.ratel.smartmoving.climbing.Orientation` 패키지에 신설.

- [ ] **B-19a1. `SmartMovingContext` 이식 파트 1 — 수직 상태 헬퍼**
      (원본 SmartMovingContext / Orientation 의 `isOnLadderOrVine` / `isOnOpenTrapDoor` /
      `isRope` / `isOnWallRope` / `isBaseAccessible` / `isFullAccessible` /
      `isFullExtentAccessible` / `isJustLowerHalfExtentAccessible` / `isFullEmpty` /
      `isSolid`)
      * `SmartMovingContext` 원본 Agent WebFetch 필요.
      * 1.21.1 BlockState 매핑 (ladder/vine/trapdoor/rope 식별 — rope 은 SmartMoving 모드
        블록, 1.21.1 vanilla 외부).

- [ ] **B-19a2. `SmartMovingContext` 이식 파트 2 — `isLadderSubstitute` 본체**
      (원본 L477-L606 + L608-L648 `hasHalfHold` + `hasBottomHold`)
      * `halfOffset` (middle/base/sub/subSub/top) enum 이식
      * gap 계산 1-5 스케일 + `ClimbGap.CanStand/MustCrawl/Block/Meta/Direction` 설정 로직
      * 의존: B-19a1 헬퍼 전수 이식.

- [ ] **B-19a3. `handsClimbing()` / `feetClimbing()` 판정 메서드 이식**
      (원본 L329-L475)
      * `handsClimbing`: 4개 gap (middle/base/sub/subSub) 판정 → HandsClimbing 결과 + gap
        threshold 별 분기 (FastUp/Up/TopHold/BottomHold/Sink)
      * `feetClimbing`: 4개 gap (top/middle/base/sub) + isCrawlClimbing/isClimbCrawling
        조건부 분기
      * `HandsClimbing.max` / `FeetClimbing.max` 메서드 확인 (1.21.1 에 이미 있는지 grep)
      * 의존: B-19a2 `isLadderSubstitute`.

- [ ] **B-19a4. `Orientation.seekClimbGap` 메서드 이식 + Climber.handleClimbing 연결**
      (원본 L207-L224 + Self.java L937-L961)
      * `seekClimbGap(rotation, world, i, id, jhd, k, kd, isClimbCrawling, isCrawlClimbing,
        isCrawling, inout_handsClimbing, inout_feetClimbing, out_handsClimbGap,
        out_feetClimbGap)` 메서드 이식
      * 1.21.1 `SmartMovingClimber.handleClimbing` 4방향 탐색을 `getOnLadderOrVine` →
        `Orientation.PZ/NZ/ZP/ZN.seekClimbGap` 4회 호출로 교체 (원본 L937-L940)
      * 원본 L945-L947 `sm.isNeighborClimbing`/`hasNeighborClimbGap`/`hasNeighborClimbCrawlGap`
        대입 이식 (→ B-19c / B-19d 의 일부 해소)
      * 대각 탐색도 `Orientation.PP/NP/NN/PN.seekClimbGap` 로 교체 (원본 L951-L954)
      * 원본 L960-L961 `sm.hasClimbGap`/`hasClimbCrawlGap` 대입 이식 (→ B-19b 해소)
      * 의존: B-19a0 / B-19a1 / B-19a2 / B-19a3.

**예상 세션 수 (B-19a 전체)**: 5-8 세션. Orientation.java 단독 2860줄이라 B-19a1/a2 가 가장
큼. `SmartMovingContext` 원본 확보 필수.

**방침 대안 (사용자 승인 시)**: 엄격 완료 방침 유지 vs 근사 이식 채택. 근사 시 B-19a 전체를
"`getOnLadderOrVine` 에 ClimbGap.canStand/mustCrawl 을 수직 블록 공간 체크로 근사 계산"
단일 원자로 축소 가능 (§7 B-19 근사 등록). **현재는 엄격 완료 방침 유지로 서브 5개 구조 선택**.

#### B-19b. `hasClimbGap` / `hasClimbCrawlGap` 계산 이식
- [ ] B-19b. 원본 Free Climb 분기 내부 `hasClimbGap` + `hasClimbCrawlGap` 갱신 로직.
      4방향 (또는 8방향) ClimbGap 결과 순회하여 OR 집계.
      `isClimbHolding` / B-18 `isClimbCrawling` 메인 공식의 `needClimbCrawling = hasClimbCrawlGap
      || (hasClimbGap && isClimbHolding)` 에 필수.

#### B-19c. `isNeighborClimbing` 계산 이식
- [ ] B-19c. 원본 Free Climb 내부 인접 블록 등반 가능 판정.
      B-17 `isCrawlClimbing = (wasCrawling || isCrawlClimbing) && isClimbing &&
      isNeighborClimbing && (sneakPressed || crawlToggled) && moveForward > 0F` 에 필수.

#### B-19d. 대각 4방향 확장 (`hasNeighborClimbGap` / `hasNeighborClimbCrawlGap`)
- [ ] B-19d. isSmall 아닐 때 PP/NP/NN/PN 대각 4방향 추가 탐색.
      `hasNeighborClimbGap` / `hasNeighborClimbCrawlGap` 갱신.
      필드 이식은 B-15c 세션 40 완료 — 갱신 로직만 추가.

### Phase 4. B-10 + B-31c 공식 완성 — isShallowDiveOrSwim / isJumpingOutOfWater / isStillSwimmingJump / initializeCrawling

의존 필드는 B-10a/b/c / B-31c 세션 38 에서 이식됨. 공식 갱신만 남음.

#### B-10a-post. `isShallowDiveOrSwim` 공식 이식
- [ ] B-10a-post. 원본 L507 `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming)`.
      `couldStandUp` 은 Swimmer 지역 변수 (L169 이식 완료).
      Swimmer.updateSwimState 말미 또는 handleSwimming 시작부에서 갱신.
      이식 완료 시 B-36 분기 (a) 얕은 물 swim/dive → walking 전환 활성화.

#### B-10b-post. `wantJumpOutOfWater` + `isJumpingOutOfWater` 공식 이식
- [ ] B-10b-post. 원본 L486-L488:
      `wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0) &&
                            isCollidedHorizontally && diveUp && !isSlow;`
      `isJumpingOutOfWater = wantJumpOutOfWater &&
                             (waterMovementTicks > 10 || onGround || wasJumpingOutOfWater);`
      Swimmer.updateSwimState 에서 계산 (B-12 ticks 정정 이후 위치).
      의존: `wasJumpingOutOfWater` 이전 틱 저장 필요 (신규 필드?).

#### B-10c-post. `isStillSwimmingJump` false 리셋
- [ ] B-10c-post. 원본 L550 `useStandard` 경로에서 `isStillSwimmingJump = false` 리셋.
      현재 B-36 분기 (a) 에서 true 설정만 있고 리셋 경로 없음.
      B-9 메인 분류 재작성 시 자연스럽게 포함되나 Phase 4 에서 별도 이식 가능.

#### B-31c-post. `initializeCrawling` 공식 이식
- [ ] B-31c-post. `initializeCrawling` 필드는 B-31c 세션 38 에서 이식됨 — true 설정 로직은
      미이식 (현재 항상 false). 원본 사용 지점:
      (1) B-35 분기 B 의 `(isCrawling && !wasCrawling) || initializeCrawling` 에서 **소비**.
      (2) B-35 분기 B 본문 `if (initializeCrawling) toCrawling();` 에서 **소비**.
      (3) B-35 분기 A 의 `!initializeCrawling` 에서 **소비** (억제 조건).
      Agent WebFetch 로 원본 `initializeCrawling = true` 설정 지점 확인 필요
      (예상 위치: grab 엣지 + 특정 조건 — 원본 `updateEntityActionState` 내 어딘가).
      현재 true 설정 경로 없어 B-35 분기 B 의 "initializeCrawling → toCrawling() 추가 호출"
      경로가 완전히 비활성. 공식 이식 후 Crawl 초기화 경로 활성화.

#### B-10-reset-post. `resetSwimming()` 메서드 완전 이식 (세션 88 3차 감사 발견)
- [ ] B-10-reset-post. 본체 §6 L366 `resetSwimming()` **부분 이식** 표기 해소.
      원본 `resetSwimming()` (SmartMovingSelf.java L1488-L1498 추정) 전체 리셋 필드 목록
      확보 필요 (Agent WebFetch). 현재 Swimmer.updateSwimState 물 밖 분기에서 리셋되는
      필드는 5개 수준 — 원본은 8개 이상 가능성. 추가 리셋 대상 예상:
      `isShallowDiveOrSwim` (B-10a), `isFakeShallowWaterSneaking`, `isJumpingOutOfWater`
      (B-10b), `isLevitating` (B-10d), `waterMovementTicks` 초기화 등. 의존: B-10a/b/c/d
      필드 이식 완료 (Phase 1). 완전 이식 시 물 밖 전환 엣지에서 모든 수중 관련 상태
      정리가 원본과 1:1.

#### B-N-standup. `standupIfPossible` 메서드 이식 (세션 88 3차 감사 발견)
- [ ] B-N-standup. 원본 `standupIfPossible()` (SmartMovingSelf.java — 정확 위치 Agent
      WebFetch 필요) 메서드 이식. 소비 지점:
      (1) `restoreFromFlying = true` 이후 (B-24 세션 53 이식됨) — 비행 해제 시 일어설 수 있으면
          자동 standup 트리거.
      (2) `handleSwimming` 내 수영→크롤링 전환 (focus_03 §5.1 참조).
      현재 `restoreFromFlying` 필드 값 설정만 정확 동작 — standupIfPossible 미이식으로
      실제 "일어서기 시도" 로직 비활성. 세션 53 B-24 완료 전 후속 원자로 명시.
      이식 대상: boolean 반환 메서드 + boundingBox 확장 가능 판정 + pose 변경.
      예상 의존: AABB 헬퍼 (Phase 6 B-42a/b 이후 정밀 가능 — 전에는 `canStandUp(player)` 근사).
      완료 시 비행 해제/수영→크롤 전환 엣지 경로 1:1 복원.

#### B-40-post. `toCrawling()` 잔여 호출 지점 L2751/L2760/L2767 이식 (세션 88 3차 감사 발견)
- [ ] B-40-post. 본체 세션 41 L1883 기록된 `wasCrawling` 재설정 8 위치 중 B-27/B-35/B-36
      에서 각각 L2566 / L2572 / L2812 / L2835 / L2860 을 흡수했으나 **L2751 / L2760 /
      L2767 은 미해소**. Agent WebFetch 로 원본 해당 대역 확보 후 전환 블록 특정 +
      이식 위치 결정. 예상 영역: 수영/다이빙 → 육상 전환 또는 그 반대 경로 내부 crawl
      재설정 구간. 의존: Phase 5 B-7/B-9/B-11 swim 재구성 완료 후 자연 흡수 가능 —
      선 Phase 5 후 남은 부분만 별도 원자화.

#### B-10b-pre. `wasJumpingOutOfWater` 필드 명시 신설 (세션 88 4차 확정 감사 발견)
- [ ] B-10b-pre. B-10b-post "의존: wasJumpingOutOfWater 이전 틱 저장 필요" 를 별도 원자로
      분리. `SmartMovingClientState` public boolean 필드 신설 + resetState 리셋 + R-09
      종료부 저장 (`wasJumpingOutOfWater = isJumpingOutOfWater`). B-10b-post 진행 전
      선행 완료 필요. Phase 1 필드 선언 규칙 연장선.

### Phase 5. B-7 / B-9 / B-11 본체 — 수중 3상태 완전 재구성

#### B-7. updateSwimState 진입 조건 복원
- [ ] B-7a. `isLiquidClimbing` 필드 + 계산 로직 이식 (원본 L280 Free climbing liquid 판정).
- [ ] B-7b. `Config.isLavaLikeWaterEnabled()` + `handleLavaMovement()` 헬퍼 이식.
- [ ] B-7c. Swimmer.updateSwimState 진입 조건 정밀 복원:
      `!isFlying && !isLiquidClimbing && (isInWater || (wasSwimming && isInLiquid) ||
      (lavaLikeWater && handleLavaMovement()))`

#### B-9. handleSwimming 메인 분류 3-갈래 재작성
- [ ] B-9a. `playerSwimWaterBorder` / `totalSwimWaterBorder` 계산 (AABB 정밀 — Phase 6 공유).
- [ ] B-9b. `[0, 2]` 구간 A/B 서브 분기 (`diveUp || moveSwim || wantShallowSwim`).
- [ ] B-9c. A 경로 11-단계 swimming offset 테이블 (1.4-1.9).
- [ ] B-9d. B 경로 10-단계 diving offset 테이블 (1.5-1.9).
- [ ] B-9e. `(2, ∞)` 구간 diving + diveUp/diveDown/moveSwim + isFast 분기.
- [ ] B-9f. `(-∞, 0)` handleSwimmingRejected.
- [ ] B-9g. `motionYDiff` 전체 적용 로직.

#### B-11. 얕은 물 특수 분기 이식
- [ ] B-11. 원본 L513-L536 `isShallowDiveOrSwim && realMinPlayerSwimWaterDepth <
      SwimCrawlWaterBottomBorder(0.55F)` 진입 조건 + isSlow 분기 (crawl 전환 / walking).
      B-9 완료 + Phase 6 AABB 의존.

#### B-7d. `isInLiquid()` 메서드 이식 (세션 88 4차 확정 감사 발견)
- [ ] B-7d. 본체 §6.2 L219 "isInLiquid() 미이식 (B-7 서브)" 명시 분리. 원본
      `SmartMovingSelf.isInLiquid()` 메서드 이식 — 물 + 용암 통합 판정. B-7c 본문 내
      `(wasSwimming && isInLiquid)` 조건 활성화용. Agent WebFetch 로 원본 본문 확보 필요.
      예상 구조: `isInWater() || (Config.isLavaLikeWaterEnabled() && isInLava())`.

#### B-9h. `swimDown = false` 설정 이식 (세션 88 4차 확정 감사 발견)
- [ ] B-9h. 본체 §7 B-5 근사 (3) "swimDown=false (원본 L244) 미이식 — B-9 메인 분류 재작성
      시 재검토" 를 Phase 5 명시 원자로 승격. 원본 L244 `swimDown` 지역 변수 초기값 false
      설정 후 특정 조건 (isSlow + diveDown 등) 에서 갱신되는 경로 이식. `isFakeShallowWaterSneaking`
      경로와 연계 가능. B-9 재작성 본문 중 어느 위치에 들어가는지는 Agent WebFetch 로 원본
      확보 후 결정.

### Phase 6. AABB 정밀화 — §7 근사 8건 일괄 해소

원본 정밀 AABB 헬퍼 이식.

#### B-42 이식 (Phase 6 본체)
- [ ] B-42a. `getMaxPlayerSolidBetween(double y1, double y2, double dOffset)` 이식 —
      플레이어 AABB 의 x/z 범위 + [y1, y2] Y 범위 내 최고 고체 블록 Y.
- [ ] B-42b. `getMinPlayerSolidBetween(double y1, double y2, double dOffset)` 이식 —
      위 범위 내 최저 고체 블록 Y.
- [ ] B-42c. `getMinPlayerLiquidBetween(double y1, double y2)` 이식 — 최저 액체 Y.
- [ ] B-42d. `realMinPlayerSwimWaterDepth` / `playerCrawlWaterBorder` 등 AABB 기반 파생값
      이식.

#### B-42 적용 (§7 근사 승격)
- [ ] B-42-B5. Swimmer B-5 `couldStandUp` → `playerSwimWaterBorder >= 0 &&
      minPlayerSwimWaterDepth <= 1.5` 원본 복원.
- [ ] B-42-B16. ClientState B-16 `blocked` 의미 재검토 (GUI 입력 차단) — AABB 무관하나 §7
      등록됨. `allowUserInput` 대체 로직 검토.
- [ ] B-42-B20. Climber Standard Base Climb `isOnLadderOrVine && isCollidedHorizontally`
      조건 판정 복원.
- [ ] B-42-B26. Jumper `tryJump(SlideDown, ...)` 속도 공식 이식 (원본 tryJump 내부 SlideDown
      분기).
- [ ] B-42-B35. ClientState `crawlStandUpBottom` 정밀 계산 + `move(0, crawlStandUpBottom -
      minY, 0)` 이동량 복원.
- [ ] B-42-B36. ClientState B-36 분기 (a) 이동량 복원.
- [ ] B-42-B39. ClientState `fromSwimmingOrDiving` 3분기 isSlow 크롤 전환 본문 활성.
- [ ] B-42-B18a. ClientState B-18 진입 엣지 `isCollidedHorizontally` 복원 — AABB/판정
      본문 (Mixin 결과 소비). B-42-B18b 완료 후 활성.
- [ ] B-42-B18b. `MixinPlayer.horizontalCollision` setter 노출 Mixin 신설 (세션 88 4차
      확정 감사 발견 — 기존 B-42-B18 에 "Mixin 필요" 만 명시되고 Mixin 원자 자체 미분리).
      1.21.1 `player.horizontalCollision` 은 public 필드이나 Entity 소스 인젉션 위치 확인
      후 MixinExtras `@Accessor` / `@Mutable` 로 setter 노출. B-42-B18a 본문 활성화 전제.

### Phase 7. §16 신규 발견 해소

#### B-48. isGroundSprinting 전환 후처리 + sprintKey 엣지
- [ ] B-48a. `sprintKeyStartPressed` / `sprintKeyStopPressed` 엣지 필드 신설 (sneakKey 패턴).
- [ ] B-48b. 원본 L2697-L2709 `isGroundSprinting` 전환 후처리 이식 —
      `wasRunningWhenSprintStarted` / `Options._runOnSprintRelease` 의존 확인 후 이식 또는
      근사.
- [ ] B-48c. `wasGroundSprinting` 필드 (원본 L2678 이전 틱 저장) + R-09 종료부 저장 추가.

#### B-49. grabButton.StopPressed 이식
- [ ] B-49. 원본 `grabButton.StopPressed` 사용 지점 전수 grep 후 필요 시 `grabKeyStopPressed`
      필드 이식 (prev vs cur 비교 방식).

#### B-49b. 이동 엣지 prev 필드 전수 이식 (세션 88 4차 확정 감사 발견)
- [ ] B-49b. 본체 §6.8 L336 "vanilla input 엣지 비제공 — 현재 prevPressRight/Back 일부
      이식 확인" 항목 Extended 원자 승격. 원본 `Options.moveForward/Backward/Left/Right`
      의 `StartPressed` / `StopPressed` 전수 이식. `prevPressForward` / `prevPressBack` /
      `prevPressLeft` / `prevPressRight` 필드 신설 (기존 일부 중복 확인) + 매 틱 저장 +
      엣지 판정 헬퍼. 사용 지점: handleClimbing 방향 전환 / 수영 방향 입력 / 벽점프 등.
      grep 사용 지점 전수 확인 후 각 호출처 엣지 판정으로 치환.

### Phase 8. Simple / Smart Base Climb 전체 이식

#### B-20b. Simple Base Climb (원본 L825-L843)
- [ ] B-20b. `cfg.simpleClimb` 분기 이식 — feet/hands 조합별 motionY 4갈래
      (feet+hands: FastUpMotion / feet only: FastUpMotion / hands only: SlowUpMotion /
      둘 다 없음: 0.0D) + combinedFactor 곱.

#### B-20c. Smart Base Climb (원본 L856-L894)
- [ ] B-20c. `cfg.smartClimb` 분기 이식 — handsSubstitute/feetSubstitute 판정 (PZ/NZ/ZP/ZN
      방향 isHandsLadderSubstitute/isFeetLadderSubstitute) + 조합별 motionY.

### Phase 9. SmartStatistics + Options 후속 엣지 케이스

**세션 88 추가** — 세션 85 분리 취소 + 전수 감사에서 발견된 잔여 엣지.

#### B-50. SmartStatisticsFactory 이식 (B-1c 근사 해소)
- [ ] B-50. 원본 `SmartStatisticsFactory.getInstance(sp).getTickDistance()` 이식 — SmartRender
      측 플레이어 tick 이동 거리 통계 시스템. B-1c3 세션 51 에서 `isClimbSprintSpeed = true`
      근사 이식한 부분 해소용. 규모 대 (SmartRender 별도 인프라 전체 이식 필요).
      엄격 이식이면: 플레이어별 tick 이동량 수집 + 통계 캐시 + getter 제공.
      간소 근사면: 단순 `getVelocity().horizontalLength()` 로 tick 거리 대체 가능.
      B-1c 의 `isClimbSprintSpeed` 는 "등반 중 충분한 속도 유지" 판정이라 근사로도 실용.
      상세 이식 전에 Agent WebFetch 로 원본 SmartStatisticsFactory 코드 + 사용 지점 전수
      확보 필요.

#### B-48b-dep. `Options._runOnSprintRelease` / `_walkOnSprintRelease` 필드 이식
- [ ] B-48b-dep. 1.21.1 SmartMovingConfig 미이식 — grep 확인 (세션 88). Phase 7 B-48b
      (isGroundSprinting 전환 후처리) 의존 필드. 이식 시 `Options` 위치 (SmartMovingConfig
      또는 별도 Options 클래스) + 기본값 (Modified 계열) 확인.

#### B-48b-fallback. B-48 불가능 시 근사 판단
- [ ] B-48b-fallback. 원본 L2697-L2709 `isGroundSprinting` 전환 후처리 본문 확보 후 의존
      필드 `wasRunningWhenSprintStarted` / `isStandupSprintingOrRunning()` 등 이식 난이도
      평가. 대규모 이식 불가 시 §7 근사 등록 (원본 의도: sprint 해제 후 walk/run 전환 시
      관성 유지).

#### B-51. `Config.isLevitateSmallEnabled()` + `isSmall` 게이트 이식 (세션 88 4차 확정 감사 발견)
- [ ] B-51. 본체 §6.7 L314 "Config.isLevitateSmallEnabled() ✗ 미이식" 항목 Extended 원자
      승격. `Config.isLevitateSmallEnabled()` 헬퍼 + 관련 `isSmall` 게이트 전수 이식 —
      원본 `isSmall` 판정 (작은 플레이어 모드) 과 `levitateSmall` 옵션 조합이 부양
      (levitate) / 자동 점프 / 중력 적용에 영향. 의존: Config 필드 grep 확인 → 부재 시
      SmartMovingConfig 에 필드 + 헬퍼 동시 신설. Agent WebFetch 로 원본 사용 지점 전수
      확인 권고.

---

## 4. 의존 순서 + 실행 권고

```
Phase 3 (B-19 도미노 해소)       ← 최우선 (의존 다수 활성화)
    ↓
Phase 4 (B-10 + B-31c 공식 완성) ← B-36 분기 (a) 활성화
    ↓
Phase 6 (AABB 정밀화)            ← Phase 5/7/8 이전 또는 병행
    ↓
Phase 5 (B-7/B-9/B-11)           ← Phase 6 AABB 필요
    ↓
Phase 7 (B-48/B-49)              ← 독립 가능
    ↓
Phase 8 (Simple/Smart)           ← 독립 가능
    ↓
Phase 9 (SmartStatistics + 엣지) ← 최후 (인프라 규모 평가 필요)
```

**추정 원자 수**: 약 47 신규 원자 + 일부 재이식 (Phase 9 신설 +3, 3차 감사 +3
→ B-10-reset-post / B-N-standup / B-40-post, 4차 확정 감사 +7 → B-7d / B-9h / B-10b-pre
/ B-42-B18a / B-42-B18b / B-49b / B-51, **세션 89 B-19a 재분해 +4 → B-19a0/a1/a2/a3/a4
(단일 원자 → 5 서브)**).
**추정 세션 수**: 37-55 세션 (B-19a 서브 1a1/1a2 가 가장 큼 — `SmartMovingContext` 전체
헬퍼 이식 필요).
**Agent WebFetch 필요**: 대부분 원자. **`SmartMovingContext.java` / `HandsClimbing.java` /
`FeetClimbing.java` / Properties 원본 추가 확보** 필요 (B-19a1/a2/a3 의존).

---

## 5. 작업 기록

### 세션 89 — 2026-04-24 — B-19a 원본 확보 + 서브 원자 5개 재분해

**사용자 지시**: "이 규칙 지키면서 작업 이어서 진행해줘" (세션 88 프롬프트 재사용).

**목표**: Phase 3 B-19a 시작 (엄격 완료 방침, 최우선 도미노 해소).

**진행한 작업**:
1. **원본 확보** — Agent WebFetch 로 `net/smart/moving/Orientation.java` 전체 2860줄 확보.
   `.tmp_research/Orientation.java.md` 저장.
2. **1.21.1 현재 상태 grep**:
   - `SmartMovingClimber.handleClimbing` — 4방향 + 대각 탐색 자체는 이식됨
     (`getOnLadderOrVine` + 수동 대각 루프) 이나 결과를 `sm.isNeighborClimbing` /
     `sm.hasClimbGap` / `sm.hasClimbCrawlGap` / `sm.hasNeighborClimbGap` /
     `sm.hasNeighborClimbCrawlGap` 에 대입하는 코드 **완전 부재**.
   - `ClimbGap.canStand` / `mustCrawl` — public 필드로 있으나 설정하는 로직 **완전 부재**
     (`reset()` 에서 false 되고 그대로). 원본은 `isLadderSubstitute` L597-L604 에서
     `CanStand = gap > 3; MustCrawl = gap > 1 && gap < 4` 로 설정.
3. **범위 재평가**:
   - 원본 `seekClimbGap` (L207-L224) 은 `handsClimbing`/`feetClimbing` 호출 래퍼 — 실체
     로직은 `isLadderSubstitute` (L477-L606) 의 gap 1-5 스케일 판정 + `hasHalfHold`
     (L608+) / `hasBottomHold` 분기.
   - `isLadderSubstitute` 의 의존: `isOnLadderOrVine` / `isOnOpenTrapDoor` / `isRope` /
     `isOnWallRope` / `isBaseAccessible` / `isFullAccessible` / `isFullExtentAccessible` /
     `isJustLowerHalfExtentAccessible` 등 10+ 헬퍼. 대부분 `SmartMovingContext` 에 있음.
   - **결론**: B-19a 단일 원자로 한 세션 불가. 최소 5개 서브로 분해 필요.
4. **Extended §3 B-19a 재구조화** — 5개 서브 원자로 분해:
   * B-19a0: `Orientation` 클래스 기본 구조 신설 (9 상수 + 각도 판정 헬퍼)
   * B-19a1: `SmartMovingContext` 헬퍼 파트 1 (수직 상태 판정 10+ 헬퍼)
   * B-19a2: `isLadderSubstitute` 본체 + `hasHalfHold`/`hasBottomHold` (gap 계산 + CanStand/
     MustCrawl 설정의 핵심)
   * B-19a3: `handsClimbing()` / `feetClimbing()` 판정 메서드 이식
   * B-19a4: `seekClimbGap` 메서드 이식 + Climber.handleClimbing 연결 (원본 L937-L961)
5. **§3 B-19a 설명 교체 완료** — 서브 원자별 범위 / 의존 / 원본 근거 라인 명시.

**코드 변경 없음** — 메타 결정 + 리서치 확보 + 원자 재분해 세션. 빌드 검증 불필요.

**완료 전 검증 체크리스트 (세션 89 기준)**:
- [근거] 원본 `Orientation.java` 2860줄 전체 WebFetch ✓
- [근거] 1.21.1 `SmartMovingClimber.handleClimbing` 전수 read + ClimbGap 구조 확인 ✓
- [근거] `isNeighborClimbing`/`hasClimbGap`/`hasClimbCrawlGap`/`hasNeighborClimbGap`/
  `hasNeighborClimbCrawlGap` ClientState 필드는 이미 이식 (B-15 세션 40) 확인 ✓
- [대응] 원본 L906-L1020 범위 ↔ 1.21.1 현재 구조 side-by-side 완료 ✓
- [분기] seekClimbGap 내부 handsClimbing/feetClimbing 4 gap (middle/base/sub/subSub) +
  4 gap (top/middle/base/sub) + isLadderSubstitute gap 1-5 스케일 전체 식별 ✓
- [상수] `CanStand = gap > 3 / MustCrawl = gap > 1 && gap < 4` (원본 L601-L602) /
  `_handClimbingHoldGap` / `DefaultMeta` 확인 ✓
- [타이밍] 원본 tickEssential 호출 → handleClimbing 내부 4방향 탐색 → 대각 → 필드 대입
  순서 원본 L937-L961 이식 계획에 반영 ✓
- [근사] 없음 (서브 원자별 엄격 1:1 이식 방침 유지) — 필요 시 B-19a4 에서 ladder/vine
  이외 매핑 (rope/wallRope 등 SmartMoving 모드 고유 블록) 근사 등록 가능성 §7 에 사전 명시
- [신규] Orientation 클래스 + SmartMovingContext 헬퍼 이식이 Extended §3 에 포함 안 되어
  있었음 — B-19a 서브로 정식 등록 ✓
- [회귀] 코드 변경 없음 — 회귀 영향 없음 ✓
- [빌드] 코드 변경 없음 — 빌드 검증 불필요 ✓

**다음 세션 권고**: **B-19a0 (Orientation 클래스 기본 구조 신설)** — 의존 없는 독립 원자.
`choco.ratel.smartmoving.climbing.Orientation` 신설. 9 상수 + `_i`/`_k` + 각도 판정
메서드 이식. Orientation.java.md L36-L205 근거.

### 세션 88 — 2026-04-24 — 엄격 완료 결정 + Extended 파일 분리 + 전수 감사 2회

**사용자 지시**:
> 무조건적인 엄격 완료야. 포커스 파일이 길어질거 같으면 포커스2익스텐디드 파일을 만들어도됨.
> 포커스2 파일에서 미결되고 완료되지 않은 것들은 다 익스텐디드 파일로 옮겨왔는지 꼼꼼히
> 확인해 (2회)

**결정**:
1. 세션 85 "별도 포커스 분리" 결정 취소 — Phase 3~8 모두 포커스 #2 범위로 복원.
2. focus_02_state_issues.md 이미 4832 줄 → Extended 분리로 경량화.
3. Phase 3~8 원자 약 30개 §3 등록 + 의존 순서 §4 기록.
4. focus_14/15/16/17 별도 포커스 분리 계획 **취소** — 본 Extended 가 대체.
5. C-4/C-5 는 Phase 3~8 완료 후로 연기.

**전수 감사 1차 (누락 2건 발견)**:
- **B-31c `initializeCrawling` 공식** — Extended Phase 4 미등록 → 추가.
- **B-31b 체크박스 구식** — B-41 세션 70 완료 상태 반영.

**전수 감사 2차 (누락 3건 추가 발견)**:
- **B-1c `isClimbSprintSpeed = true` 근사** — §7 미등록 → 본체 §7 에 추가.
- **SmartStatisticsFactory 이식 필요** — Extended 어느 Phase 에도 없음 → Phase 9 신설.
- **Options `_runOnSprintRelease` / `_walkOnSprintRelease`** — grep 확인 1.21.1 미이식 →
  Phase 9 B-48b-dep 로 추가.
- Phase 9 "SmartStatistics + 후속 엣지 케이스" 신설 (B-50 / B-48b-dep / B-48b-fallback).

**전수 감사 5차 (처음부터 끝까지 청크 순차 읽기 — 신규 누락 0건 확인)**:
- 사용자 지시: 본체 파일을 0번째 라인부터 토큰 한도까지 청크로 읽으며 라인 번호를 기록,
  다음 세션은 기록된 라인부터 계속. 매 청크마다 Extended 교차 확인.
- 실행: `.tmp_research/focus_02_audit_progress.md` 에 진행 기록. 본체 4917줄을 7 청크 (L1-500
  / L501-1000 / L1001-1500 / L1501-2100 / L2101-2700 / L2701-3400 / L3401-4100 / L4101-4917)
  로 분할하여 각 청크 Read 후 "미이식/⚠️/✗/[누락]/[근사]" 표현 전수 추출 + Extended 원자
  대조.
- **결과: 신규 누락 0건 확정**. 청크 1 에서 9건 의심 후보 올렸으나 청크 2 로 넘어가며 전부
  해소됨:
  * B-12 waterMovementTicks → 세션 64 완료 (본체 §6.4 표기 구식)
  * B-10d isLevitating → 세션 71 완료 (본체 §6.4 표기 구식)
  * B-15d/e/f (isVine*/isClimbingStill/edgeBlock) → 필드 세션 40 완료, 갱신 로직은
    Phase 3 B-19a~d 흡수
  * getMaxPlayerLiquidBetween → Phase 6 B-42 AABB 묶음 범위
  * isPlayerInSolidBetween → B-17b1 세션 48 정밀 이식 완료
  * Orientation.isClimbable/isTunnelAhead → Phase 3 B-19a 범위
- 5차 감사는 4차 Agent 교차 대조 결과의 정확성 확인 역할. Extended 총 원자 **43개** 유지.

**전수 감사 4차 (Agent 기반 기계적 교차 대조 — 확정 누락 6건 추가 발견)**:
- 사용자 지적: 세 번 감사로도 누락이 계속 나옴 → 감사 방법 근본 재설계.
- 방법: general-purpose 에이전트에 (a) 본체 파일 전수 read (b) Extended §3 원자 전수
  read (c) 교차 대조로 "extended 에 대응 원자 없는 항목" 확정 리스트 요구.
- 확정 누락 6건 (전부 Extended 에 추가):
  1. **B-7d** — `isInLiquid()` 메서드 이식 (Phase 5 신설).
  2. **B-9h** — `swimDown = false` 설정 이식 (Phase 5 신설) — §7 B-5 근사 (3) 승격.
  3. **B-10b-pre** — `wasJumpingOutOfWater` 필드 명시 분리 (Phase 4 신설).
  4. **B-42-B18a / B-42-B18b** — 기존 B-42-B18 을 AABB 본문 + Mixin setter 두 서브로 분리.
  5. **B-49b** — 이동 엣지 prev 필드 전수 이식 (Phase 7 신설) — §6.8 L336 승격.
  6. **B-51** — `Config.isLevitateSmallEnabled()` + isSmall 게이트 이식 (Phase 9 신설)
     — §6.7 L314 승격.
- 구식 표기 (Extended 대상 아님 — 본체 정리만 필요):
  * §6.9 `resetClimbing()` "✗ 미이식 (B-14)" — 실제 B-14 세션 57 완료.
  * §6.7 Config 필드 미이식 표기 (`_sprintFactor` / `_sprintExhaustion*` / `_sprintDuringItemUsage`
    / `_flyCloseToGround` / `_diveControlVertical`) — 전부 이미 이식 완료.

**전수 감사 3차 (처음부터 끝까지 순차 읽기 — 누락 3건 추가 발견)**:
- **`standupIfPossible` 메서드 전체 미이식** — 세션 53 B-24 로그 L2493-L2494
  "B-N 후속 standupIfPossible 이식 시 자동 연결" 이후 어떤 Phase 에도 등록 안 됨.
  `restoreFromFlying` 소비자 + `handleSwimming` 수영→크롤 전환 의존 (focus_03 §5.1).
  → Phase 4 **B-N-standup** 신설.
- **`resetSwimming()` 부분 이식** — 본체 §6 L366 "⚠️ 부분 이식" 만 표기. 완전 이식 원자
  어느 Phase 에도 없음.
  → Phase 4 **B-10-reset-post** 신설.
- **`toCrawling()` 잔여 호출 지점 L2751 / L2760 / L2767** — 세션 41 L1883 기록 8 위치
  중 B-27/B-35/B-36 에서 5 위치만 흡수. 나머지 3 위치 미해소. B-40 세션 44 로그 L1045
  는 "B-35/B-36 이식 시 추가" 로 위임했으나 실제로 안 됨.
  → Phase 4 **B-40-post** 신설.
- **확인 완료 (잔여 안전)**: `collidedHorizontallyTickCount` (B-1c2 세션 51 이식), B-27
  (세션 54 L2569-L2574 이식), B-33 (세션 84 L2441-L2442 이식), B-35 (세션 74 L2812/L2835),
  B-36 (세션 78 L2860) 는 모두 완료 상태 재검.

**참고 사항 (세션 88 확인)**:
- `_sprintFactor` / `_sprintExhaustionStart/Stop` / `_sprintDuringItemUsage` /
  `_diveControlVertical` / `_flyCloseToGround` 등 Config 필드는 **이미 이식 완료** — 본체 §6
  매핑 표의 "미이식" 표기가 구식. Extended 진행 중 표기 갱신 가능 (사소).
- 본체 §6 "⚠️ 확인 필요" 3건도 현 시점에서 확인 완료 가능 (Extended 진행 중).

**진행한 작업**:
- `focus_02_extended.md` 신규 생성 (이 파일).
- `focus_02_state_issues.md` §1 상태 변경 (Extended 진행 중) + §17 Extended 참조 + §7
  B-1c 근사 추가 등록 + §10 Extended 이전 명시 전수 + §10 B-N 섹션에 ⭐ Extended 이전
  안내 표 추가.
- `focus_02_extended.md` Phase 3~9 원자 약 33개 등록 (Phase 9 신설 반영).
- `playtest_fixes.md` 의 focus_14 분리 메모 업데이트 (Extended 흡수).
- 코드 변경 없음 — 메타 결정 + 문서 구조 재편 세션.

**다음 세션**: Phase 3 B-19a 시작 — Agent WebFetch 로 원본 L906-L1020 Orientation 판정
본문 확보 후 Climber 에 이식.

---

## 6. 최종 C 단계 (Phase 3~8 완료 후)

- [ ] C-4. 사용자 인게임 재검증 — Phase 3~8 완료 후 전체 동작 확인.
- [ ] C-5. `playtest_fixes.md` "현재 포커스" → `#3` 전환.

---

## 7. 기록 가이드

- 각 원자 완료 시 본 문서 §3 체크박스 [x] + §5 세션 로그 추가
- §16 신규 발견은 본체 focus_02 §16 에 계속 기록 (단일 소스)
- §7 근사 해소 시 본체 §7 에 해소 기록 (Phase 6 완료 시 대부분)
- 빌드 검증 + 커밋 규칙은 기존 동일
