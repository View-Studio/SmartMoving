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

#### B-19a. Climber Orientation 4방향 판정 로직 이식
- [ ] B-19a. 원본 L906-L1020 대역 4방향 (PZ/NZ/ZP/ZN) Orientation 기반 블록 탐색 로직 이식.
      Agent WebFetch 로 원본 본문 확보 필요.
      `Orientation` 타입 이식 (ClimbGap 에 이미 존재 확인 필요).
      주변 블록 등반 가능 여부 → `inout_handsClimbing` / `inout_feetClimbing` / `ClimbGap`
      결과 집계. 현재 `getOnLadderOrVine` 가 일부 기능 커버하나 Free Climb 의 전체 로직
      부재.

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
- [ ] B-42-B18. ClientState B-18 진입 엣지 `isCollidedHorizontally` 복원 (Mixin 필요).

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

**추정 원자 수**: 약 33 신규 원자 + 일부 재이식 (Phase 9 신설로 3개 추가).
**추정 세션 수**: 25-40 세션.
**Agent WebFetch 필요**: 대부분 원자 (원본 본문 리서치 미확보 대역 많음).

---

## 5. 작업 기록

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
