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

- [x] **B-19a0. `Orientation` 클래스 기본 구조 신설** ✅ **세션 90 완료**
      (원본 Orientation L36-L205 + L998-L1085)
      * 9 상수 (ZZ/PZ/NZ/ZP/ZN/PP/NN/PN/NP) + `_i`/`_k` + `_isDiagonal` + 각도 필드 3 +
        생성자 이식 완료
      * Meta 상수 3 (DefaultMeta/VineFrontMeta/VineSideMeta) + 내부 상수 top/middle/base/sub/
        subSub/NoGrab/HalfGrab/AroundGrab
      * `Orthogonals` HashSet + 정적 초기화
      * `setClimbingAngles` 3 오버로드 + `isWithinAngle` 2 오버로드 + `isRotationForClimbing`
      * `rotate(int angle)` 0/±45/±90/±135/±180 전수 이식
      * `getOrientation` / `getClimbingOrientations` / `addTo` (정적 HashSet 캐시 포함)
      * `getHorizontalBorderGap(double i, double k)` static-coord 버전 (인스턴스 `base_id`/
        `base_kd` 기반 오버로드는 B-19a1 이후)
      * **미포함 (B-19a1+ 이후)**: `isTunnelAhead` / `getKnownLadderOrientation` /
        `isFeetLadderSubstitute` / `isHandsLadderSubstitute` / `baseVineClimbing` /
        `seekClimbGap` / `handsClimbing` / `feetClimbing` / `isLadderSubstitute` 등
        SmartMovingContext 의존 메서드
      * `SmartMovingConfig` 에 `freeClimbingOrthogonalDirectionAngle=90F` /
        `freeClimbingDiagonalDirectionAngle=80F` 2 필드 + load/save 이식 (원본 L129-L130)
      * 빌드: `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (6s)

- [ ] **B-19a1. `SmartMovingContext` 이식 파트 1 — 수직 상태 헬퍼** (세션 91 재분해: 3 서브)
      세션 91 원본 확보 결과 헬퍼 전부 `Orientation.java` L2729-L2744 내부 static 필드 +
      L1148-L2625 메서드 대역에 있음. `SmartMovingContext.java` 자체는 상수/initialize 만.
      규모 크므로 서브 3개 분해:
      - [x] **B-19a1a** (세션 91 완료): 상태 필드 13 + 블록 식별 헬퍼 (`isLadder`/`isVine`/
            `isLadderOrVine`/`isTrapDoor`/`isClosedTrapDoor`/`isClimbable`) + Material 헬퍼
            2 (`isSolid`/`isFullEmpty`) + World 접근 3 (`getBlock`/`getBaseBlockId`/
            `getRemoteBlockId`) + 기본 3 ladder/vine 체크 (`isOnLadder`/`isOnVine`/
            `isOnLadderOrVine`). §7 근사 4-5 등록.
      - [x] **B-19a1b** (세션 92 완료): front/back/rope/trapdoor 인스턴스 헬퍼 — ladder
            orientation 역매핑 3 (`hasLadderOrientation`/`hasVineOrientation`/
            `getKnownLadderOrientation`) + front/back/behind 6 (`isOnLadderFront`/`Back` /
            `isOnVineFront`/`Back` / `isBehindLadder`/`Vine`) + rope 2 (전체 false 근사) +
            trap door 4 (`isOnOpenTrapDoor`/`isTrapDoorFront`/`getOpenTrapDoorOrientation`/
            `isRemoteSolid`). §7 B-19a1b 근사 2건 등록 (LadderKit/Carpenters 모드 + rope 3종).
      - [ ] **B-19a1c**: accessibility 판정 (세션 93 재분해: 4 서브)
            세션 93 원본 L2342-L2560 + 관련 헬퍼 L2020-L2340 read 결과 `isBaseAccessible`
            이 7 분기 + `isRemoteAccessible` 12+ 분기 + stair/slab/fence/wall/door 식별 헬퍼
            10+ 의존. 한 세션 불가 → 4 서브 분해:
            - [x] **B-19a1c1** (세션 93 완료): 기본 블록 식별 + stair/slab/fence/wall/door
                  헬퍼 (isStairCompact / isTopStairCompact / isStairCompactFront·Back
                  8분기 / isBottomStairCompactFront·NotBack / isTopStairCompactFront·Back /
                  isHalfBlock / isTopHalfBlock / isBottomHalfBlock / isFence / isFenceBase /
                  isWallBlock / isDoor / isDoorTop / isFenceGate / isOpenFenceGate /
                  isClosedFenceGate). §7 근사 4건 등록.
            - [x] **B-19a1c2** (세션 94 완료): `isEmpty` + `isBaseAccessible` 2 오버로드
                  (7 분기) + 좌표 기반 trapdoor 래퍼 3 + `isFullEmpty` 좌표 오버로드.
                  §7 근사 3건 등록 (RedPower / ASRope / Carpenters).
            - [ ] **B-19a1c3**: isRemoteAccessible 외 (세션 95 재분해: 3 서브).
                  세션 95 원본 L1741-L2000 + L2401-L2484 read 결과 `getWallFlag` 가
                  BlockPane/Fence/Wall/FenceGate 별 canConnect* 메서드 의존 — 1.21.1 에서는
                  BlockState property 기반으로 근사 이식. `headedToFrontWall` /
                  `headedToRemoteFlatWall` / `headedToWall` / `headedToBaseWall` /
                  `headedToBaseGrabWall` / `getAllWallsOnNoWall` / `isTopHalf` 등 wall-flag
                  인프라 의존. 3 서브 분해:
                  - [x] **B-19a1c3a** (세션 95 완료): `remoteLadderClimbing` +
                        `isAccessAccessible` + `isDoorFrontBlocked` — 단순 3개 (wall-flag
                        인프라 무관). 근사 없음 — vanilla door FACING/OPEN/HALF property 로
                        원본 metadata 8 case 전수 1:1 매핑.
                  - [x] **B-19a1c3b** (세션 96 완료): wall-flag 인프라 5 메서드 +
                        `toBlockDirection` 매핑 헬퍼 + `getConnectingFlag` / `getWallShapeFlag`
                        (BlockState property 조회). §7 근사 3건 등록 (Pane/Fence/Wall 동적
                        계산 → property 캐시 / BetterMisc reflection / Carpenters 생략).
                  - [x] **B-19a1c3c** (세션 97 완료): `headedToFrontWall` (4방향 wall flag
                        집계 + allOnNone 재해석) + `headedToRemoteFlatWall` (4방향 OR/!AND
                        패턴) + `isRemoteAccessible` 본체 (12+ 분기 OR 누적). §7 근사 2건
                        (RedPower / ASRope).
            - [x] **B-19a1c4** (세션 98 완료): `isFullAccessible` (grabRemote 2분기) +
                  `isFullExtentAccessible` + `isJustLowerHalfExtentAccessible` +
                  `isUpperHalfFrontEmpty` (7 분기) + `getWallBlockId` 보조. §7 근사 3건
                  (RedPower 2곳 + LadderKit). **B-19a1c 전체 완료**.

- [ ] **B-19a2. `SmartMovingContext` 이식 파트 2 — `isLadderSubstitute` 본체**
      (원본 L477-L606 + L608-L726 `hasHalfHold` + L728-L1000+ `hasBottomHold`)
      **세션 99 재분해 (8 서브)** — `hasHalfHold` 120줄 + `hasBottomHold` 300+줄 +
      `setHalfGrabType` 3 오버로드 + 보조 헬퍼 10+ 로 한 세션 불가. 서브 분해:
      - [x] **B-19a2a1** (세션 99 완료): wall 판정 보조 — `headedToFrontSideWall` +
            `headedToBaseWall` 3 오버로드 + `headedToBaseGrabWall` 2 오버로드. 근사 없음
            (B-19a1c3b `getWallFlag` 근사에만 의존).
      - [x] **B-19a2a2** (세션 100 완료): vine 보조 — `baseVineClimbing` 2 오버로드 +
            `remoteVineClimbing` 2 오버로드. 근사 없음 (hasVineOrientation + isVine +
            getHorizontalBorderGap 의존 전수 충족).
      - [x] **B-19a2a3** (세션 101 완료): half-solid 판정 — `isLowerHalfFrontFullEmpty` +
            `isUpperHalfFrontAnySolid` + `isUpperHalfFrontFullSolid`. §7 근사 2건 (RedPower /
            BetterThanWolves / ASRope / LadderKit 생략 + ASGrapplingHook / Carpenters 생략).
      - [x] **B-19a2a4** (세션 102 완료): 잔여 보조 10 메서드 — vanilla 2 (`getTriple` +
            `isHeadedToRope`) 1:1 + mod 8 (`isOnMiddleLadderFront` / `getCarpentersBlockData`
            / `isOnAnchorFront` / `isASGrapplingHookFront` / `getRopeId` / `getAnchorId` /
            `isASRope` / `isASGrapplingHook`) false/null 근사. §7 근사 3건 (Carpenters /
            BetterThanWolves / ASGrapplingHook-RopesPlus-ASRope). **B-19a2a 전체 완료**.
      - [x] **B-19a2b** (세션 103 완료): grab 상태 세팅 — `setHalfGrabType` 3 오버로드 +
            `setBottomGrabType` 3 오버로드 + `setGrabType` static + `initialize` +
            `initializeOffset` + `initializeLocal` (9 메서드). 근사 없음 — pure state
            setting + math. `base_jhd`/`local_halfOffset` 필드 2개 추가.
      - [x] **B-19a2c** (세션 104 완료): `hasHalfHold` 본체 (L608-L726). 13 vanilla
            분기 이식 + 3 mod 분기 근사 생략 + Config 헬퍼 `isFreeBaseClimb()` 신설 +
            `freeFenceClimbing` 필드 신설. §7 근사 3건.
      - [x] **B-19a2d** (세션 105 완료): `hasBottomHold` 본체 (L728-L935, 200+줄).
            vanilla 분기 16 이식 (ladder 4 + iron_bars / freeFenceClimbing 3 / belowWall 4
            서브 / 복합 중첩 6 AND / stair top / trap door / door frontBlocked / vine 4) +
            mod 3 카테고리 근사 생략. §7 근사 3건 (RedPower / BTW-RopesPlus / ASRope-ASGH).
      - [x] **B-19a2e** (세션 106 완료): `isLadderSubstitute` 본체 (L477-L606, 130줄) +
            Feet/Hands LadderSubstitute public 2 + 외부 API 래퍼 1 = 4 메서드.
            **gap 1-5 계산 + ClimbGap.canStand/mustCrawl 설정 핵심 — B-19 도미노 해소 실체**.
            §7 근사 1건 (ClimbGap.Meta 필드 생략 — BlockState 내재 표면 매핑).
            **B-19a2 (isLadderSubstitute 전체 8 서브) 완료**.

- [x] **B-19a3. `handsClimbing()` / `feetClimbing()` 판정 메서드 이식** ✅ **세션 107 완료**
      (원본 L329-L395 + L398-L475)
      * `handsClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, out_climbGap)` —
        `initializeOffset(3D, ...)` + 4 halfOffset (middle/base/sub/subSub) gap 판정.
        각 gap 결과로 HandsClimbing (NONE/UP/BOTTOM_HOLD/TOP_HOLD/SINK/FAST_UP) +
        ClimbGap 집계. `_climbGapTemp.skipGaps = isClimbCrawling || isCrawlClimbing`.
      * `feetClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, out_climbGap)` —
        `initializeOffset(0D, ...)` + 4 halfOffset (top/middle/base/sub) gap 판정.
        최종 `isCrawlClimbing || isCrawling` → BASE_WITH_HANDS 강제 승격.
      * 의존: B-19a2e isLadderSubstitute, B-19a2b initializeOffset, B-19a0 상수
        (top/middle/base/sub/subSub + NoGrab/HalfGrab/AroundGrab) 전수 충족.
      * 필드 추가 3: `_handClimbingHoldGap` (static final float) + `_climbGapTemp` /
        `_climbGapOuterTemp` (static final ClimbGap 인스턴스).
      * `HandsClimbing.max` / `FeetClimbing.max` 는 1.21.1 에 `ClimbGap[]` 배열 래핑 시그니처로
        이미 이식됨 — 호출부에서 `ClimbGap[] outArr = { out_climbGap }` 배열 래핑 후 전달.
      * 근사 없음 — HandsClimbing/FeetClimbing enum + ordinal 순서 비교 1:1.

- [x] **B-19a4. `Orientation.seekClimbGap` 메서드 이식 + Climber.handleClimbing 연결**
      ✅ **세션 108 완료 — B-19a / B-19b / B-19c / B-19d 전체 해소**
      (원본 L207-L224 + Self.java L937-L961)
      * `seekClimbGap(...)` 메서드 이식 완료 — isRotationForClimbing 게이트 + initialize +
        handsClimbing/feetClimbing 호출 + max 누적
      * 1.21.1 `SmartMovingClimber.handleClimbing` 내부에 seekClimbGap 8방향 호출 +
        ClientState 필드 5 대입 블록 추가 (기존 `getOnLadderOrVine` 탐색 결과와 병렬).
        기존 속도 결정 로직 보존 + ClientState 필드만 신규 대입.
      * 원본 L918-L920 jh 조정 (isClimbCrawling/isCrawlClimbing/isSmallClimbing → jh -= 2)
      * 원본 L945-L947: `sm.isNeighborClimbing` / `hasNeighborClimbGap` /
        `hasNeighborClimbCrawlGap` 대입 → **B-19c / B-19d (부분) 해소**
      * 원본 L960-L961: `sm.hasClimbGap` / `hasClimbCrawlGap` 대입 → **B-19b 해소**
      * 근사 없음.
      * 의존: B-19a0 / B-19a1 / B-19a2 / B-19a3 전수 충족.

**예상 세션 수 (B-19a 전체)**: 5-8 세션. Orientation.java 단독 2860줄이라 B-19a1/a2 가 가장
큼. `SmartMovingContext` 원본 확보 필수.

**방침 대안 (사용자 승인 시)**: 엄격 완료 방침 유지 vs 근사 이식 채택. 근사 시 B-19a 전체를
"`getOnLadderOrVine` 에 ClimbGap.canStand/mustCrawl 을 수직 블록 공간 체크로 근사 계산"
단일 원자로 축소 가능 (§7 B-19 근사 등록). **현재는 엄격 완료 방침 유지로 서브 5개 구조 선택**.

#### B-19b. `hasClimbGap` / `hasClimbCrawlGap` 계산 이식
- [x] B-19b. ✅ **세션 108 완료 (B-19a4 에서 해소)** — 원본 Free Climb 분기 내부 `hasClimbGap`
      + `hasClimbCrawlGap` 갱신 로직.
      4방향 (또는 8방향) ClimbGap 결과 순회하여 OR 집계.
      `isClimbHolding` / B-18 `isClimbCrawling` 메인 공식의 `needClimbCrawling = hasClimbCrawlGap
      || (hasClimbGap && isClimbHolding)` 에 필수.

#### B-19c. `isNeighborClimbing` 계산 이식
- [x] B-19c. ✅ **세션 108 완료 (B-19a4 에서 해소)** — 원본 Free Climb 내부 인접 블록 등반
      가능 판정.
      B-17 `isCrawlClimbing = (wasCrawling || isCrawlClimbing) && isClimbing &&
      isNeighborClimbing && (sneakPressed || crawlToggled) && moveForward > 0F` 에 필수.

#### B-19d. 대각 4방향 확장 (`hasNeighborClimbGap` / `hasNeighborClimbCrawlGap`)
- [x] B-19d. ✅ **세션 108 완료 (B-19a4 에서 해소)** — isSmall 아닐 때 PP/NP/NN/PN 대각
      4방향 추가 탐색.
      `hasNeighborClimbGap` / `hasNeighborClimbCrawlGap` 갱신.
      필드 이식은 B-15c 세션 40 완료 — 갱신 로직만 추가.

### Phase 4. B-10 + B-31c 공식 완성 — isShallowDiveOrSwim / isJumpingOutOfWater / isStillSwimmingJump / initializeCrawling

의존 필드는 B-10a/b/c / B-31c 세션 38 에서 이식됨. 공식 갱신만 남음.

#### B-10a-post. `isShallowDiveOrSwim` 공식 이식
- [x] B-10a-post. ✅ **세션 109 완료** — 원본 L507 `isShallowDiveOrSwim = couldStandUp &&
      (isDiving || isSwimming);` 이식. Swimmer.updateSwimState 에 3 지점 처리 추가:
      * !isTouchingWater return 앞: `isShallowDiveOrSwim = false` (원본 L548)
      * 3-OR 강제 isDipping 경로: false 리셋
      * 말미 (waterMovementTicks 갱신 직후): 공식 `couldStandUp && (isDiving ||
        isSwimming_sm)` 대입 (couldStandUp = B-5 세션 63 근사 `dippingDepth>=0F &&
        dippingDepth<=1.5F`)
      근사 없음 (couldStandUp 자체는 기존 근사). B-36 분기 (a) 얕은 물 swim/dive →
      walking 전환 활성화 + B-11 Phase 5 (얕은 물 특수 분기) 진입 게이트 활성화.

#### B-10b-post. `wantJumpOutOfWater` + `isJumpingOutOfWater` 공식 이식
- [x] B-10b-post. ✅ **세션 111 완료** — 원본 L486-L487 공식 이식.
      Swimmer.updateSwimState `if (isSwimming_sm || isDiving)` 분기 내 ticks 증분 직후에 배치:
      * `wantJumpOutOfWater = (movementForward != 0 || movementSideways != 0) &&
        player.horizontalCollision && diveUp16 && !sm.isSlow` (지역 변수)
      * `sm.isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 ||
        player.isOnGround() || sm.wasJumpingOutOfWater)` 필드 대입
      근사 없음. 의존 전수 충족 (B-10b-pre wasJumpingOutOfWater + B-10d diveUp16 +
      B-12 ticks + vanilla horizontalCollision/isOnGround).

#### B-10c-post. `isStillSwimmingJump` false 리셋
- [x] B-10c-post. ✅ **세션 112 완료** — 원본 L550 `useStandard` 경로에서
      `isStillSwimmingJump = false` 리셋 이식. 1.21.1 `Swimmer.updateSwimState` 의
      `!isTouchingWater` return 경로에 한 줄 추가 (물 밖 전환 시 수영 점프 hold 상태
      해제). true 설정은 B-36 분기 (a) (원본 L2845, ClientState 세션 78 이식 완료) 에서만.
      resetState 리셋 (L1735) 은 세션 38 이식 완료. 근사 없음.

#### B-31c-post. `initializeCrawling` 공식 이식
- [x] B-31c-post. ✅ **세션 113 완료** — 원본 L2343-L2356 `initializeCrawling` true 설정
      블록 이식. 원본은 tickEssential 내부 **지역 변수** `boolean initializeCrawling = false`
      매 틱 선언 + 조건부 true 설정. 1.21.1 은 필드 승격 → 매 틱 `this.initializeCrawling =
      false` 리셋 + 동일 조건에서 `true` 설정:
      ```
      if (!initialized && !(remote && multiPlayerInitialized != 0) && !hasVehicle()) {
          if (!canStandUp(player)) {   // 근사 — 원본 getMaxPlayerSolidBetween 정밀 AABB
              initializeCrawling = true;
              toCrawling();
          }
          initialized = true;
      }
      if (multiPlayerInitialized > 0) multiPlayerInitialized--;
      ```
      **신설**: `SmartMovingClientState.initialized` public boolean 필드 + resetState 리셋.
      **§7 B-31c-post 근사 1건**: AABB → canStandUp (B-42 Phase 6 완료 시 정밀 복원).
      배치 위치: tickEssential pre-compute 블록 내 mustCrawl 계산 **직전** — 원본 L2343
      (mustCrawl L2395 이전) 순서 보존.
      소비자 활성화: B-35 분기 B `initializeCrawling → toCrawling() 추가 호출` +
      분기 A `!initializeCrawling` 억제 조건 정상 동작.

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
- [x] B-10b-pre. ✅ **세션 110 완료** — `SmartMovingClientState.wasJumpingOutOfWater`
      public boolean 필드 신설 + resetState 리셋 + `Swimmer.updateSwimState` 진입 첫 줄에
      `sm.wasJumpingOutOfWater = sm.isJumpingOutOfWater` 저장. 원본 L105 지역 snapshot 을
      1.21.1 updateSwimState/handleSwimming 분리 구조에서 필드로 승격. **§7 B-10b-pre 근사
      1건 등록** (지역 → 필드 구조 차이, 시멘틱 동치).

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

### 세션 113 — 2026-04-24 — B-31c-post `initializeCrawling` true 설정 경로 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2343-L2356 grep + read** — initializeCrawling 지역 변수 선언 + 조건부 true
   설정 + multiPlayerInitialized 감소 블록 확인. 소비 지점 L2399 / L2822 / L2827 / L2831 /
   L2834 (B-35/B-36 범위).
2. **1.21.1 초기화 필드 확인**:
   * `multiPlayerInitialized` (L669) 이식 완료 / 감소 로직 미이식
   * `initialized` 필드 **없음** → 신설
   * `canStandUp(player)` (L1760) 이식 완료 (AABB 근사)
   * `toCrawling()` (B-40 세션 44) 이식 완료
3. **이식 내용**:
   * `SmartMovingClientState.initialized` public boolean 필드 신설 (multiPlayerInitialized
     직후) + resetState false 리셋 추가
   * tickEssential pre-compute 블록 내 `grabHeld0` 선언 직후 + mustCrawl 계산 **직전**에
     initializeCrawling 설정 블록 이식:
     - `this.initializeCrawling = false` 매 틱 리셋 (원본 L2343 지역 초기값)
     - `if (!initialized && !(isClient && multiPlayerInitialized != 0) && !hasVehicle())` 가드
     - `if (!canStandUp(player))` **§7 근사** → initializeCrawling=true + toCrawling()
     - `initialized = true`
     - `multiPlayerInitialized--` (원본 L2355-L2356)
4. **본체 §7 B-31c-post 근사 1건 등록** (AABB 정밀 → canStandUp).

**완료 전 검증 체크리스트 (세션 113 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L2343-L2356 전수 read ✓
- [근거] 의존 전수 충족 — multiPlayerInitialized (기존) + canStandUp (기존) + toCrawling
  (B-40) + B-35 소비자 지점 이식 완료 (세션 74) ✓
- [대응] 원본 L2343-L2356 ↔ 1.21.1 side-by-side. `boolean initializeCrawling = false` 지역
  선언 → `this.initializeCrawling = false` 필드 리셋. 조건 체인 (initialized + !remote &&
  !multiPlayerInitialized + !isRiding) + 내부 canStandUp 분기 + initialized=true +
  multiPlayerInitialized 감소 전수 보존 ✓
- [분기] 외부 if (3-AND) + 내부 if (canStandUp 근사) + 후속 multiPlayerInitialized > 0
  감소 전수 ✓
- [상수] 없음 ✓
- [타이밍] 원본 L2343 은 speedChange 패킷 처리 (L2337-L2340) 직후, mustCrawl 계산
  (L2395) 직전. 1.21.1 tickEssential pre-compute 블록의 mustCrawl (L859) 직전에 배치 —
  원본 순서 보존 ✓
- [근사] **§7 B-31c-post 근사 1건 등록** (AABB → canStandUp). 주석 명시 ✓
- [신규] `SmartMovingClientState.initialized` 필드 + resetState 리셋 신설 ✓
- [회귀] 기존 initializeCrawling 항상 false → 이제 초기 틱 canStandUp false 시 true.
  B-35 분기 B 의 `toCrawling()` 추가 호출 경로 활성화. multiPlayerInitialized 감소 로직이
  비로소 이식되어 서버 동기화 직후 pushOutOfBlocks 억제 카운터 감소 정상 동작 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-10-reset-post** — `resetSwimming()` 메서드 완전 이식 (세션 88 3차
감사 발견). 원본 `resetSwimming()` 전체 리셋 필드 목록 Agent WebFetch 로 확보 필요.
예상 1 세션.

**진행률** (세션 113 종료 시점):
- Extended 완료: **27 원자** (B-19 완결 22 + B-10a-post + B-10b-pre + B-10b-post +
  B-10c-post + **B-31c-post**)
- Extended 총 원자 ~61
- **Extended 진행률: 27/61 ≈ 44%**
- **포커스 #2 전체: (54+27)/115 ≈ 70%**
- **Phase 4**: 5/8 (3 남음 — B-10-reset-post / B-N-standup / B-40-post)

### 세션 112 — 2026-04-24 — B-10c-post `isStillSwimmingJump` 리셋 (원본 L550)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L550 + L2359 + L2845 grep** — isStillSwimmingJump 갱신 3 지점 확인:
   * L550: useStandard 경로 false 리셋 (이식 대상)
   * L2359: resetState false — 1.21.1 L1735 이식 완료 (세션 38)
   * L2845: B-36 분기 (a) true 설정 — 1.21.1 ClientState L1535 이식 완료 (세션 78)
2. **Swimmer.updateSwimState `!isTouchingWater` return 경로에 `isStillSwimmingJump = false`
   추가** (원본 L550 대응 — 물 밖 전환 시 수영 점프 hold 해제).
3. **3-OR 강제 isDipping 경로에는 추가 안 함** — 원본 L544-L550 useStandard 경로는 완전
   리셋 (dipping=false 포함). 1.21.1 3-OR 강제 isDipping 경로는 dipping=true 유지 —
   원본 L544-L550 대응 아님. 안전상 리셋 안 함 (원본 동작 유지).

**완료 전 검증 체크리스트 (세션 112 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L550 + L2359 + L2845 grep ✓
- [근거] 1.21.1 resetState L1735 + B-36 분기 (a) L1535 이식 확인 ✓
- [대응] 원본 L550 ↔ 1.21.1 updateSwimState `!isTouchingWater` 경로 한 줄 추가.
  완전 리셋 경로 동치 ✓
- [분기] 원본 else (useStandard) 분기 vs 1.21.1 `!isTouchingWater` return — 둘 다 swim/
  dive/dipping/isShallowDiveOrSwim/isStillSwimmingJump 전부 false 설정 경로로 대응 ✓
- [상수] 없음 ✓
- [타이밍] `isShallowDiveOrSwim = false` (B-10a-post) 직후 배치 — 원본 L548-L550 순서 보존 ✓
- [근사] 없음 ✓
- [신규] 없음 ✓
- [회귀] 기존 isStillSwimmingJump 는 true 설정 (B-36) 후 리셋 경로 미이식으로 계속
  true 유지 가능성이 있었음. 이제 물 밖 전환 시 정상 리셋. B-9 재작성 시 useStandard
  경로에서도 리셋 추가 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-31c-post** — `initializeCrawling` true 설정 경로 이식 (원본 공식
미확인 — Agent WebFetch 로 `initializeCrawling = true` 설정 지점 전수 확인 필요). 예상 1-2
세션.

**진행률** (세션 112 종료 시점):
- Extended 완료: **26 원자** (B-19 22 + B-10a-post + B-10b-pre + B-10b-post + **B-10c-post**)
- Extended 총 원자 ~61
- **Extended 진행률: 26/61 ≈ 43%**
- **포커스 #2 전체: (54+26)/115 ≈ 70%**
- **Phase 4**: 4/8 (4 남음 — B-31c-post / B-10-reset-post / B-N-standup / B-40-post)

### 세션 111 — 2026-04-24 — B-10b-post `wantJumpOutOfWater` + `isJumpingOutOfWater` 공식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L486-L487 재확인** (수면 탈출 점프 공식):
   * `wantJumpOutOfWater = (moveForward != 0 || moveStrafing != 0) && sp.isCollidedHorizontally
     && diveUp && !isSlow` (지역 변수)
   * `isJumpingOutOfWater = wantJumpOutOfWater && (waterMovementTicks > 10 || sp.onGround ||
     wasJumpingOutOfWater)` 필드 대입
2. **Swimmer.updateSwimState 에 이식** — `if (isSwimming_sm || isDiving)` 분기 내 ticks
   증분 **직후** 에 배치 (원본 L481-L487 순서 보존):
   * `wantJumpOutOfWater` 지역 boolean
   * `sm.isJumpingOutOfWater` 필드 할당
3. **1.21.1 매핑**:
   * `sp.moveForward != 0` / `moveStrafing != 0` → `player.input.movementForward != 0F` /
     `player.input.movementSideways != 0F` (float 비교)
   * `sp.isCollidedHorizontally` → `player.horizontalCollision`
   * `diveUp` → `diveUp16` (B-10d 세션 71 의 지역 변수)
   * `sp.onGround` → `player.isOnGround()`
   * `wasJumpingOutOfWater` → `sm.wasJumpingOutOfWater` (B-10b-pre 세션 110 필드)
   * `isSlow` → `sm.isSlow`
   * `waterMovementTicks` → `sm.waterMovementTicks`

**완료 전 검증 체크리스트 (세션 111 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L486-L487 read ✓
- [근거] 의존 전수 충족 — B-10b 필드 (세션 38) + B-10b-pre wasJumpingOutOfWater (세션
  110) + B-10d diveUp16 (세션 71) + B-12 waterMovementTicks (세션 64) + B-2 isSlow
  (세션 43) + vanilla input/horizontalCollision/isOnGround ✓
- [대응] 원본 2 라인 ↔ 1.21.1 2 라인 side-by-side. `wantJumpOutOfWater` 지역 (원본 L486)
  + `isJumpingOutOfWater` 필드 대입 (원본 L487) 순서 + AND/OR 구조 원본 1:1 ✓
- [분기] 2 AND 체인 (wantJumpOutOfWater 4-AND + isJumpingOutOfWater 2-AND) + 내부 OR
  (waterMovementTicks > 10 || onGround || wasJumpingOutOfWater) 전수 ✓
- [상수] `10` (waterMovementTicks threshold) / `0F` (float 비교) / `!isSlow` 원본 동일 ✓
- [타이밍] B-12 ticks 증분 직후 (원본 L482 → L486 순서) 배치. swim/dive 분기 내에서만
  실행 (원본과 동치) ✓
- [근사] 없음 — 공식 1:1 ✓
- [신규] 없음 ✓
- [회귀] 기존 isJumpingOutOfWater 항상 false → 이제 실제 공식 기반. 소비처 (원본 L500
  `motionY = 0.30000001192092896D` 설정) 는 B-9 재작성 범위. 현재는 필드만 정확히 세팅 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-10c-post** — 원본 L550 `isStillSwimmingJump = false` 리셋 이식
(useStandard 경로 — 1.21.1 에서는 Swimmer.updateSwimState 의 물 밖 / 크롤 강제 경로에서).
작은 작업. 예상 1 세션.

**진행률** (세션 111 종료 시점):
- Extended 완료: **25 원자** (B-19 완결 22 + B-10a-post + B-10b-pre + **B-10b-post**)
- Extended 총 원자 ~61
- **Extended 진행률: 25/61 ≈ 41%**
- **포커스 #2 전체: (54+25)/115 ≈ 69%**
- **Phase 4**: 3/8 (5 남음 — B-10c-post / B-31c-post / B-10-reset-post / B-N-standup /
  B-40-post)

### 세션 110 — 2026-04-24 — B-10b-pre `wasJumpingOutOfWater` 필드 승격

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L105 + L229 + L487 grep** — 원본 `wasJumpingOutOfWater` 는 `updateEntityActionState`
   내부 **지역 변수 snapshot** (L105). handleSwimming 에 파라미터로 전달 → L487 공식에서
   hysteresis 제공.
2. **1.21.1 클래스 분리 구조 대응** — Swimmer.updateSwimState 와 handleSwimming 이 2개의
   정적 메서드로 분리되어 지역 변수 공유 불가 → **필드로 승격** (§7 근사 등록).
3. **3 지점 수정**:
   * `SmartMovingClientState.isJumpingOutOfWater` 필드 (L433) 직후에 `wasJumpingOutOfWater`
     public boolean 필드 신설 + JavaDoc (원본 L105 + 근사 사유 명시).
   * `resetState` (L1720 근처) 에 `wasJumpingOutOfWater = false` 리셋 추가.
   * `Swimmer.updateSwimState` 진입 **첫 줄** (isTouchingWater 체크 전) 에
     `sm.wasJumpingOutOfWater = sm.isJumpingOutOfWater` 저장 (원본 L105 대응 위치).
4. **본체 §7 B-10b-pre 근사 1건 등록** — 지역 snapshot → 필드 승격 (클래스 분리 대응).

**완료 전 검증 체크리스트 (세션 110 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L105 + L487 + L229 grep ✓
- [근거] 1.21.1 `isJumpingOutOfWater` 필드 (B-10b 세션 38) 이식 완료 ✓
- [대응] 원본 지역 변수 snapshot ↔ 1.21.1 필드 저장 동치 (시멘틱 보존) ✓
- [분기] 없음 (단순 필드 + 대입) ✓
- [상수] 없음 ✓
- [타이밍] Swimmer.updateSwimState 진입 첫 줄 저장 — 원본 L105 가 handleSwimming 호출
  직전에 있으므로 1.21.1 updateSwimState 가 handleSwimming 직전 호출되는 구조에서 동치
  시점 ✓
- [근사] **§7 B-10b-pre 근사 1건 등록** (지역 → 필드 승격, 구조 차이) ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 변경 없음 (신규 필드). B-10b-post 공식 이식 시 참조 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-10b-post** — 원본 L486-L487 `wantJumpOutOfWater` + `isJumpingOutOfWater`
공식 이식. 의존: `wasJumpingOutOfWater` (B-10b-pre 완료) + `isJumpingOutOfWater` 필드
(B-10b 세션 38) + `waterMovementTicks` (B-12 세션 64). 예상 1 세션.

**진행률** (세션 110 종료 시점):
- Extended 완료: **24 원자** (B-19 완결 22 + B-10a-post + **B-10b-pre**)
- Extended 총 원자 ~61
- **Extended 진행률: 24/61 ≈ 39%**
- **포커스 #2 전체: (54+24)/115 ≈ 68%**
- **Phase 4**: 2/8 (나머지 6 — B-10b-post / B-10c-post / B-31c-post / B-10-reset-post /
  B-N-standup / B-40-post)

### 세션 109 — 2026-04-24 — B-10a-post `isShallowDiveOrSwim` 공식 이식 (Phase 4 시작)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**Phase 3 (B-19) 완료 → Phase 4 진입.**

**진행한 작업**:
1. **원본 L500-L551 read** — `isShallowDiveOrSwim` 공식 위치 + useStandard 외 else 분기 확인.
   * 원본 L507: `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming);`
   * 원본 L548: else 분기 `isShallowDiveOrSwim = false;`
2. **1.21.1 Swimmer.updateSwimState 3 지점 처리 추가**:
   * L65-L71 `!isTouchingWater` return 앞: `isShallowDiveOrSwim = false` (원본 L548 대응)
   * L80-L88 3-OR 강제 isDipping 경로: `isShallowDiveOrSwim = false` (swim/dive false
     자동 반영 + 명시성)
   * L130-L135 `waterMovementTicks` 갱신 **직후**: `couldStandUp = dippingDepth >= 0F &&
     dippingDepth <= 1.5F` 지역 계산 + `sm.isShallowDiveOrSwim = couldStandUp &&
     (isDiving || isSwimming_sm)` 공식 대입
3. **`couldStandUp` 은 B-5 (세션 63) 기존 근사** — `dippingDepth` 단일값 기반. 원본 AABB
   `minPlayerSwimWaterDepth` 정밀 스캔 미이식 (§7 B-5 근사와 연동). B-10a-post 자체는
   근사 없이 공식 1:1 이식.

**완료 전 검증 체크리스트 (세션 109 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L500-L551 read ✓
- [근거] 의존 `isShallowDiveOrSwim` 필드 이식 완료 (B-10a 세션 38) + `couldStandUp`
  근사 이식 완료 (B-5 세션 63) ✓
- [대응] 3 지점 이식 원본 ↔ 1.21.1 side-by-side. 공식 `couldStandUp && (isDiving ||
  isSwimming)` 원본 1:1 ✓
- [분기] !isTouchingWater (원본 L548) / 3-OR 강제 isDipping (원본 L301 근방 + L507 swim/
  dive false 자동 반영) / 정상 경로 공식 대입 (원본 L507) 전수 ✓
- [상수] `1.5F` (couldStandUp threshold) 원본 동일 ✓
- [타이밍] updateSwimState 말미 (ticks 갱신 직후) — 원본 L504-L508 (isDiving/isLevitating/
  isSwimming 대입 직후 L507) 순서 보존. B-10d (세션 71) isLevitating 대입 후속으로 배치 ✓
- [근사] 없음 — 공식 1:1. `couldStandUp` 지역 변수는 B-5 근사 동일 패턴 재사용 ✓
- [신규] 없음 ✓
- [회귀] B-36 분기 (a) 얕은 물 swim/dive → walking 전환 이전 항상 false → 이제 실제
  couldStandUp 기반. B-11 Phase 5 진입 게이트 활성화 준비. 다른 호출처 없어 영향 최소 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-10b-pre** — `wasJumpingOutOfWater` 필드 명시 분리 (Phase 1 필드
선언 연장선). 의존 없음. 예상 1 세션 작은 작업.

**진행률** (세션 109 종료 시점):
- Extended 완료: **23 원자** (B-19 완결 22 + **B-10a-post**)
- Extended 총 원자 ~61
- **Extended 진행률: 23/61 ≈ 38%**
- **포커스 #2 전체: (54+23)/115 ≈ 67%**
- **Phase 4 진입** (B-10a-post 완료 / B-10b-pre / B-10b-post / B-10c-post / B-31c-post /
  B-10-reset-post / B-N-standup / B-40-post 7 남음)

### 세션 108 — 2026-04-24 — 🎉 B-19a4 seekClimbGap + Climber 연결 — **B-19 전체 도미노 해소 완결**

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**🎯 마일스톤 세션** — Phase 3 B-19 (a0~a4, b, c, d) 전체 완료. B-17 `hasClimbCrawlGap` /
B-18 `hasClimbGap` / B-16c `wouldWantClimb` 4-OR 공식 **활성화 개시**.

**진행한 작업**:
1. **원본 L207-L224 `seekClimbGap` 전수 read + 원본 L848-L854 좌표 추출 규칙 확인**:
   * `id = sp.posX` (double) / `jd = sp.boundingBox.minY` (double) / `kd = sp.posZ` (double)
   * `i = floor(id)` / `j = floor(jd)` / `k = floor(kd)` (int)
   * `jh = jd * 2D + 1` (원본 L926)
2. **`Orientation.seekClimbGap(...)` 메서드 이식** (원본 L207-L224):
   * `isRotationForClimbing(rotation)` 게이트
   * `initialize(world, i, id, jhd, k, kd)` 상태 필드 설정
   * `handsClimbing(...)` + `feetClimbing(...)` 호출 결과를 `max()` 로 inout 누적
   * `ClimbGap[]` 배열 래핑 (1.21.1 max 시그니처 대응)
3. **`SmartMovingClimber.handleClimbing` 에 seekClimbGap 블록 삽입**:
   * 기존 `getOnLadderOrVine` 4/대각 탐색 결과 `handsClimbing`/`feetClimbing` 변수 보존 +
     속도 결정 로직 보존 (변경 없음)
   * L386 `if (!handsClimbing.isRelevant() && !feetClimbing.isRelevant()) return;` **앞**
     에 새 블록 추가:
     - 좌표 id/jd/kd 추출 (player.getX()/getBoundingBox().minY/getZ())
     - rotation 정규화 (player.getYaw() % 360F + 음수 보정)
     - isSmallClimbing = isCrawling || isSliding
     - jh = jd * 2D + 1, 조건 시 jh -= 2D (원본 L918-L920 의 jd += -1D 상응)
     - inoutH/inoutF (NONE 초기화) + outHandsGap/outFeetGap (new ClimbGap)
     - **4 orthogonal PZ/NZ/ZP/ZN seekClimbGap 호출**
     - ClientState 필드 3 대입: `isNeighborClimbing` / `hasNeighborClimbGap` /
       `hasNeighborClimbCrawlGap` (원본 L945-L947) → **B-19c / B-19d (부분) 해소**
     - isSmallClimbing 아니면 **대각 4 PP/NP/NN/PN seekClimbGap 호출** (원본 L951-L954)
     - 최종 ClientState 필드 2 대입: `hasClimbGap` / `hasClimbCrawlGap` (원본 L960-L961) →
       **B-19b 해소**
4. **Climber import 추가**: `choco.ratel.smartmoving.climbing.Orientation`.
5. **Extended §3 체크박스 4 완료** — B-19a4 + B-19b + B-19c + B-19d (a4 가 세 개 동시 해소).

**완료 전 검증 체크리스트 (세션 108 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L207-L224 + `.tmp_research/SmartMovingSelf.java`
  L848-L854 + L918-L961 전수 read ✓
- [근거] 의존 전수 충족 — B-19a0 (Orientation 상수 PZ/NZ/ZP/ZN/PP/NN/PN/NP) + B-19a2b
  (initialize) + B-19a3 (handsClimbing/feetClimbing) + ClientState 필드 5 (isNeighborClimbing /
  hasNeighborClimbGap / hasNeighborClimbCrawlGap / hasClimbGap / hasClimbCrawlGap — 전부
  B-15 세션 40 이식 + 기존 L272 hasClimbCrawlGap) ✓
- [대응] seekClimbGap 원본 1:1 + Climber 연결 블록 8 방향 호출 + 필드 5 대입 원본 1:1 ✓
- [분기] seekClimbGap: isRotationForClimbing 게이트 1갈래. Climber 블록: 4 orthogonal
  (isSmallClimbing 무관) + 4 대각 (isSmallClimbing 제외) + 필드 대입 5. 전수 식별 ✓
- [상수] `2D` (jh 계산) / `360F` (rotation) / `0F` (음수 보정) 원본 동일 ✓
- [타이밍] 원본 L926 `jh = jd * 2D + 1` 계산 → L932-L935 reset → L937-L940 4방향 →
  L945-L947 필드 3 대입 → L949-L955 대각 4방향 (isSmallClimbing 가드) → L960-L961 필드 2
  대입 순서 보존. `if (!handsClimbing.isRelevant() && !feetClimbing.isRelevant()) return;`
  **앞** 에 배치하여 매 틱 필드 대입 보장 (resetClimbing 에 4 필드 빠져 있음, 원본 동일
  동작) ✓
- [근사] 없음 ✓
- [신규] Orientation import Climber 에 추가. 기존 탐색 (getOnLadderOrVine) 은 보존 +
  seekClimbGap 블록 병렬 추가. Climber.resetClimbing 에 4 필드 (hasClimbGap 등) 추가 안 함
  (원본과 동일) — 원본이 resetClimbing 에 이 필드들을 포함하지 않음 ✓
- [회귀] 기존 속도 결정 로직 변경 없음 (getOnLadderOrVine 결과 변수 handsClimbing/
  feetClimbing 그대로 사용). ClientState 필드 5 대입만 신규 추가. B-17/B-18/B-16c 공식
  평가 결과가 변화 — **이전: 항상 false / 이제: 실제 값 기반 동작**. 원본 의도 복원.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**🎉 B-19 전체 도미노 해소 완결**:
- B-19a (a0/a1a/a1b/a1c1-c4/a2a1-a4/a2b/a2c/a2d/a2e/a3/a4) 18 서브 완료
- B-19b (hasClimbGap/hasClimbCrawlGap) 해소
- B-19c (isNeighborClimbing) 해소
- B-19d (hasNeighborClimbGap/hasNeighborClimbCrawlGap) 해소

**B-17 / B-18 / B-16c 공식 활성화** — 이전엔 isNeighborClimbing / hasClimbGap /
hasClimbCrawlGap 필드가 항상 false 라 공식 평가 항상 false 였음. 이제 seekClimbGap 기반
실제 값 저장 → 자동 등반 + 크롤 등반 + 얇은 갭 등반 경로 활성화.

**다음 세션 권고**: **B-10a-post** (Phase 4 시작) — `isShallowDiveOrSwim` 공식 이식 (원본
L507). B-19 도미노 해소 완료로 Phase 3 완전 종결 → Phase 4 진입. 의존: B-10a 필드 (세션
38 이식 완료). 예상 1 세션.

**진행률** (세션 108 종료 시점):
- Extended 완료: **22 원자** (B-19a0 / a1a / a1b / a1c1-c4 / a2a1-a4 / a2b-a2e / a3 /
  **a4** + **b** + **c** + **d**)
- Extended 총 원자 ~61
- **Extended 진행률: 22/61 ≈ 36%**
- **포커스 #2 전체: (54+22)/115 ≈ 66%**
- **Phase 3 (B-19) 전체 완료** — Phase 4 진입 가능

### 세션 107 — 2026-04-24 — B-19a3 handsClimbing() + feetClimbing() 판정 — **B-19a3 완료**

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L329-L395 + L398-L475 전수 read** — 이식 범위 재확인 (세션 89 부분 read 보완).
2. **필드 3 추가** (Orientation 클래스):
   * `_handClimbingHoldGap` static final float — Config 기반 threshold
     `Math.min(0.25F, 0.06F * Math.max(upSpeedFactor, downSpeedFactor))`.
     Config 기본값 (1.0F/1.0F) 에서 = 0.06F.
   * `_climbGapTemp` / `_climbGapOuterTemp` static final ClimbGap 인스턴스 2개 —
     `handsClimbing`/`feetClimbing`/`seekClimbGap` (B-19a4) 에서 재사용.
3. **`handsClimbing()` 이식** (원본 L329-L395):
   * `initializeOffset(3D, ...)` (B-19a2b) 호출
   * `HandsClimbing result = NONE`
   * 4 halfOffset 별 `isLadderSubstitute` (B-19a2e) 호출:
     - middle: gap > 0 → jh_offset 비교 → UP 또는 NONE
     - base: gap > 0 → jh_offset 비교 → BOTTOM_HOLD 또는 UP
     - sub (skipGaps 설정 후): gap > 0 && !(isCrawling && gap > 1) → 4 갈래
       (FAST_UP (2가지) / grabType 분기 (UP/TOP_HOLD/TOP_HOLD/SINK))
     - subSub: gap > 0 && !isCrawling → 3 갈래 (TOP_HOLD/FAST_UP/SINK)
   * 각 max 호출 시 `ClimbGap[] outArr = { out_climbGap }` 배열 래핑 (1.21.1 max
     시그니처가 `ClimbGap[]` 로 이식됨)
4. **`feetClimbing()` 이식** (원본 L398-L475):
   * `initializeOffset(0D, ...)` + FeetClimbing result = NONE
   * 4 halfOffset (top/middle/base/sub) 별 gap 판정:
     - top: gap > 0 → NONE (placeholder — ClimbGap 만 업데이트)
     - middle (skipGaps 후): gap > 0 && !isCrawling → 4 갈래
       (FAST_UP / BASE_WITH_HANDS / SLOW_UP_WITH_HOLD_WITHOUT_HANDS / TOP_WITH_HANDS)
     - base: gap > 0 → 5 갈래 (gap > 3 FAST_UP / gap > 2 SLOW_UP 2종 / base 2 갈래
       BASE_WITH_HANDS/BASE_HOLD)
     - sub: gap > 0 → NONE
   * 최종 `isCrawlClimbing || isCrawling` → BASE_WITH_HANDS 강제 승격
5. **enum 매핑**:
   * 원본 `HandsClimbing.None/Up/FastUp/TopHold/BottomHold/Sink` → 1.21.1
     `NONE/UP/FAST_UP/TOP_HOLD/BOTTOM_HOLD/SINK`
   * 원본 `FeetClimbing.None/BaseHold/BaseWithHands/TopWithHands/
     SlowUpWithHoldWithoutHands/SlowUpWithSinkWithoutHands/FastUp` → 1.21.1 동일 upper case
6. `_climbGapTemp.SkipGaps` → `_climbGapTemp.skipGaps` (1.21.1 ClimbGap 필드 camelCase).

**완료 전 검증 체크리스트 (세션 107 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L329-L475 전수 read ✓
- [근거] 의존 전수 충족 — B-19a2e isLadderSubstitute + B-19a2b initializeOffset +
  B-19a0 상수 (top/middle/base/sub/subSub/NoGrab/HalfGrab/AroundGrab) +
  `HandsClimbing.max` / `FeetClimbing.max` (기존 이식) ✓
- [대응] 2 메서드 원본 ↔ 1.21.1 side-by-side. `handsClimbing` 4 halfOffset x 4/5 갈래
  내부 분기 + `feetClimbing` 동일 + 최종 crawl 승격 전수 ✓
- [분기] `handsClimbing` middle/base 각 2갈래 + sub 5갈래 + subSub 3갈래 + grabType 분기
  전수. `feetClimbing` top 1갈래 + middle 4갈래 + base 5갈래 + sub 1갈래 + 최종 승격.
  모든 `if-else` 중첩 depth 3+ 보존 ✓
- [상수] `3D` (handsClimbing offset) / `0D` (feetClimbing offset) / `1D - _handClimbingHoldGap`
  threshold / gap 임계값 (> 0 / > 1 / > 2 / > 3) 원본 동일 ✓
- [타이밍] out_climbGap.reset() → _climbGapTemp.reset() → initializeOffset → halfOffset
  별 isLadderSubstitute 호출 순서 원본 1:1. `_climbGapTemp.skipGaps` 설정 시점 (sub 직전,
  middle 직전) 원본 동일 ✓
- [근사] 없음 — HandsClimbing/FeetClimbing enum + ClimbGap[] 배열 래핑은 1.21.1 표면 매핑 ✓
- [신규] `_handClimbingHoldGap` / `_climbGapTemp` / `_climbGapOuterTemp` 필드 3개 신설
  (원본 L2722-L2727 대응) ✓
- [회귀] 기존 코드 미사용. B-19a4 `seekClimbGap` 이 handsClimbing/feetClimbing 호출 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-19a4 (마지막)** — `seekClimbGap` 메서드 이식 (원본 L207-L224) +
`Climber.handleClimbing` 연결 (원본 L937-L961 `sm.isNeighborClimbing` /
`sm.hasNeighborClimbGap` / `sm.hasNeighborClimbCrawlGap` / `sm.hasClimbGap` /
`sm.hasClimbCrawlGap` 대입 이식). **B-19 도미노 해소의 소비자 연결 마지막 단계**. 의존
전수 충족. 예상 1 세션.

**진행률** (세션 107 종료 시점):
- Extended 완료: **18 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / a2a4 / a2b / a2c / a2d / a2e / **a3**)
- Extended 총 원자 ~61
- **Extended 진행률: 18/61 ≈ 30%**
- **포커스 #2 전체: (54+18)/115 ≈ 63%**
- **B-19a 마일스톤**: a0/a1/a2/a3 완료 — **a4 (Climber 연결) 1개만 남음**.

### 세션 106 — 2026-04-24 — B-19a2e `isLadderSubstitute` 본체 — **B-19a2 전체 완료**

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L477-L606 전수 read** — isLadderSubstitute 본체 130줄. L299-L327 (Feet/Hands
   public + 외부 API 래퍼) 도 포함.
2. **4 메서드 이식**:
   * `isFeetLadderSubstitute(w, bi, j, bk)` public (원본 L299-L306): middle/base 2 gap OR
   * `isHandsLadderSubstitute(w, bi, j, bk)` public (원본 L308-L316): middle/base/sub 3 gap OR
   * `isLadderSubstitute(worldObj, i, j, k, halfOffset)` 외부 API 래퍼 (원본 L318-L327):
     static 상태 필드 설정 후 내부 본체 호출
   * `isLadderSubstitute(localOffset, out_climbGap)` **본체 130줄** (원본 L477-L606):
     - `initializeLocal(localOffset)` (B-19a2b 이식) 호출
     - `local_half == 1` (upper): hasHalfHold() 성공 시
       - !grabRemote (base grab): overLadder/overAccessible/overFullAccessible 조합 →
         gap 1/5/crawl?3:5
       - grabRemote (remote grab): isBaseAccessible(0) + isUpperHalfFrontEmpty +
         isFullAccessible(1) + isFullExtentAccessible(2)/isJustLowerHalfExtentAccessible(2)
         → gap 1/3/4/5
     - `local_half == 0` (lower): hasBottomHold() 성공 시
       - !grabRemote: 동일 패턴 → gap 1/2/4/crawl?2:4
       - grabRemote: isFullAccessible(0) + isFullExtentAccessible(1) → gap 0/1/2/4
     - 최종 out_climbGap 설정 (gap > 0):
       - `state = grabBlock`
       - `canStand = gap > 3`
       - `mustCrawl = gap > 1 && gap < 4`
       - `direction = this.toBlockDirection()`
3. **1.21.1 매핑**:
   * `out_climbGap.Block = grabBlock` → `out_climbGap.state = grabBlock` (BlockState)
   * `out_climbGap.Meta = grabMeta` **생략** (§7 근사) — 1.21.1 ClimbGap 에 meta 필드 없음
     (BlockState 내재 표면 매핑)
   * `out_climbGap.Direction = this` → `out_climbGap.direction = toBlockDirection()`
     (B-19a1c3b 헬퍼)
4. **원본 버그 1:1 유지**: L548-L550 `overOverLadder` 에서 `isOnWallRope(0)` (0 이어야
   1) 반복. 원본 타입 오류로 보이나 1:1 이식 방침으로 그대로 유지 (코드 주석 명시).
5. **§7 B-19a2e 근사 1건 등록** (`ClimbGap.Meta` 생략).

**완료 전 검증 체크리스트 (세션 106 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L299-L327 + L477-L606 전수 read ✓
- [근거] 의존 메서드 전수 충족 — B-19a1a (상태 필드) + B-19a1a/b/c1-c4 (15+ 헬퍼) +
  B-19a2a1/a2/a3/a4 (보조) + B-19a2b (setHalfGrabType/setBottomGrabType/initializeLocal/
  initializeOffset/initialize) + B-19a2c (hasHalfHold) + B-19a2d (hasBottomHold) ✓
- [대응] 4 메서드 원본 ↔ 1.21.1 side-by-side. 본체 130줄 원본 구조 완전 보존 (if-else 중첩
  depth 5+) ✓
- [분기] upper half (local_half==1): hasHalfHold 실패/성공 x !grabRemote/grabRemote x
  overLadder/overAccessible 등 조합 전수 (gap 0/1/3/5 분기).
  lower half (local_half==0): hasBottomHold 실패/성공 x !grabRemote/grabRemote x 동일
  (gap 0/1/2/4 분기). if-else depth 5+ 전수 보존 ✓
- [상수] gap 값 1-5 + crawl 분기 (crawl?3:5 / crawl?2:4) + `gap > 3` / `gap > 1 && gap < 4`
  threshold 원본 동일 ✓
- [타이밍] `initializeLocal` → gap 계산 → ClimbGap 설정 순서 원본 1:1 ✓
- [근사] **§7 B-19a2e 근사 1건 등록 완료** (ClimbGap.Meta 생략) ✓
- [신규] 원본 L548-L550 `isOnWallRope(0)` 반복 (1 대신) — 원본 버그 가능성, 1:1 유지 주석 명시 ✓
- [회귀] 기존 코드 미사용. B-19a3 (handsClimbing/feetClimbing) 이 isLadderSubstitute 를
  각 halfOffset (top/middle/base/sub/subSub) 별 호출 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**🎉 B-19a2 (isLadderSubstitute 전체 8 서브 — a2a1/a2a2/a2a3/a2a4/a2b/a2c/a2d/a2e) 완료**
— **B-19 도미노 해소의 실체 완결**. `ClimbGap.canStand`/`mustCrawl` 설정 로직이 비로소 실제
값 저장 → B-17 `hasClimbCrawlGap` / B-18 `hasClimbGap` 공식이 실제 값 기반 평가 준비 완료
(B-19a4 Climber 연결 후 활성).

**다음 세션 권고**: **B-19a3** — `handsClimbing()` / `feetClimbing()` 판정 메서드 이식 (원본
L329-L475, 150줄). `isLadderSubstitute` (B-19a2e) 를 각 halfOffset (top/middle/base/sub/
subSub) 별 호출하여 HandsClimbing/FeetClimbing enum 결과 + ClimbGap 결과 집계. 1 세션
예상.

**진행률** (세션 106 종료 시점):
- Extended 완료: **17 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / a2a4 / a2b / a2c / a2d / **a2e**)
- Extended 총 원자 ~61
- **Extended 진행률: 17/61 ≈ 28%**
- **포커스 #2 전체: (54+17)/115 ≈ 62%**
- **B-19a 마일스톤**: a0/a1/a2 완료 (17+ 서브). a3/a4 남음 → **B-19a 완료 임박**.

### 세션 105 — 2026-04-24 — B-19a2d `hasBottomHold` 본체 (원본 L728-L935)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L728-L935 read** — 하부 grab 판정 200+줄. 복합 중첩 분기 다수.
2. **`hasBottomHold` 본체 이식** (vanilla 16 분기 + mod 3 근사):
   * (1) FreeBaseClimb 4 ladder (base-1 / base 0 / remote-1 / remote 0 AroundGrab)
   * (2) [§7 근사] BTW/RopesPlus rope+anchor 분기 생략
   * (3) [§7 근사] RedPower wire 4 서브 생략
   * (4) isEmpty(base-1) + remoteBelow iron_bars + frontWall → HalfGrab
   * (5) freeFenceClimbing 3 서브 (fence remoteBelow / cobblestone remoteBelow / remote)
   * (6) belowWallId 존재 시 4 서브 (양옆 empty + iron_bars/middleLadder/headedToBaseGrabWall /
     freeFenceClimbing fence)
   * (7) 복합 6-AND 중첩 (remoteLowerHalfEmpty + isBaseAccessible(-1, true, false) +
     isUpperHalfFrontAnySolid + !isBottomHalfBlock + !(stair bottomFront) +
     (!isDoor || isDoorTop) + (!isDoor(base) || !isDoorFrontBlocked) +
     (freeFenceClimbing || !isFence))
   * (8) stair top remote + !topBack + upperHalfFrontFullSolid → BottomGrab
   * (9) baseBelow open trap door → BottomGrab
   * (10) baseBelow door top + frontBlocked + isBaseAccessible(0) → BottomGrab
   * (11) [§7 근사] ASGrapplingHook/RopesPlus 4 서브 분기 생략
   * (12)-(15) FreeBaseClimb 4 vine (base-1 / base 0 / remote-1 / remote 0 HalfGrab)
   * (16) default NoGrab
3. **핵심 매핑** (B-19a2c 동일):
   * `Blocks.IRON_BARS` / `Blocks.COBBLESTONE_WALL` / `Blocks.VINE.getDefaultState()`
   * 모든 Block 파라미터 → BlockState (B-19a1a grabBlock 필드 타입 일관)
   * `Config._freeFenceClimbing.value` → `cfg.freeFenceClimbing` (B-19a2c 이식)
4. **본체 §7 B-19a2d 근사 3건 등록**.

**완료 전 검증 체크리스트 (세션 105 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L728-L935 전수 read ✓
- [근거] 의존 메서드 (세션 90-104 이식 전수) 충족 — isEmpty/isBaseAccessible/isFence/
  isStairCompact/isTrapDoor/isClosedTrapDoor/isDoor/isDoorTop/isDoorFrontBlocked/
  isBottomHalfBlock/isTopStairCompact/isTopStairCompactBack/isBottomStairCompactFront/
  isLowerHalfFrontFullEmpty/isUpperHalfFrontAnySolid/isUpperHalfFrontFullSolid/isWallBlock/
  headedToFrontWall/headedToRemoteFlatWall/headedToFrontSideWall/headedToBaseWall/
  headedToBaseGrabWall/isOnMiddleLadderFront/isOnLadder/isOnLadderFront/remoteLadderClimbing/
  baseVineClimbing/remoteVineClimbing/setHalfGrabType/setBottomGrabType ✓
- [대응] 16 vanilla 분기 + 3 mod 근사 + default 원본 ↔ 1.21.1 side-by-side ✓
- [분기] 복합 6-AND 중첩 (원본 L853-L865) 전수 식별 — `remoteLowerHalfEmpty` +
  `isBaseAccessible(-1, true, false)` + `isUpperHalfFrontAnySolid` + `!isBottomHalfBlock` +
  `!(stair + bottomFront)` + `(!isDoor || isDoorTop)` + `(!isDoor(base) ||
  !isDoorFrontBlocked)` + `(freeFenceClimbing || !isFence)`. 원본 if 체인 중첩 7 단계
  전수 보존 ✓
- [상수] `NoGrab` / `HalfGrab` / `AroundGrab` / `DefaultMeta` 원본 동일. `Blocks.IRON_BARS` /
  `Blocks.COBBLESTONE_WALL` / `Blocks.VINE` vanilla 매핑 ✓
- [타이밍] hasBottomHold 는 isLadderSubstitute subSub 분기에서 1회 호출 (B-19a2e 에서 연결) ✓
- [근사] **§7 B-19a2d 근사 3건 등록 완료**. 각 생략 지점 "근사 이식 — 원본과 차이: X"
  주석 ✓
- [신규] 없음 (B-19a2c 에서 모든 Config/블록 매핑 신설 완료) ✓
- [회귀] 기존 코드 미사용. B-19a2e (isLadderSubstitute) 가 hasBottomHold 소비 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (9s) ✓

**다음 세션 권고**: **B-19a2e** — `isLadderSubstitute` 본체 (원본 L477-L606, 130줄).
**B-19 도미노 해소의 실체** — gap 1-5 스케일 계산 + `ClimbGap.canStand=gap>3` /
`mustCrawl=gap>1 && gap<4` 설정 핵심. `hasHalfHold` / `hasBottomHold` (B-19a2c/d) 의 결과를
소비. 의존 전수 충족 (B-19a1a~c + B-19a2a~d). 예상 1 세션 (큰 세션).

**진행률** (세션 105 종료 시점):
- Extended 완료: **16 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / a2a4 / a2b / a2c / **a2d**)
- Extended 총 원자 ~61
- **Extended 진행률: 16/61 ≈ 26%**
- **포커스 #2 전체: (54+16)/115 ≈ 61%**

### 세션 104 — 2026-04-24 — B-19a2c `hasHalfHold` 본체 (원본 L608-L726)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 `hasHalfHold` 본체 재확인** (세션 99 read 완료) + Config 의존 WebFetch 확인.
2. **Config 신설**:
   * `freeFenceClimbing = false` 필드 + load/save (원본 L250 Unmodified 기본값)
   * `isFreeBaseClimb()` 헬퍼 — `freeClimb` 단순 반환 근사 (원본 `_baseClimb.is("free").and(_freeClimb)`
     Property). 1.21.1 boolean 4 필드 이식 구조에서 동치.
3. **`hasHalfHold` 본체 이식** (120줄 → 1.21.1 약 90줄, mod 분기 생략):
   * (1)-(2) FreeBaseClimb ladder 자동 grab (base/remote) — AroundGrab
   * (3) [§7 근사] BetterThanWolves/RopesPlus rope+anchor 분기 생략
   * (4) isEmpty + remote iron_bars + frontWall → HalfGrab (remote)
   * (5) wallId iron_bars + baseWall(0) → HalfGrab (base)
   * (6) wallId + isOnMiddleLadderFront → AroundGrab (remote)
   * (7) `freeFenceClimbing` 블록 6 서브 분기 (fence remote/remoteBelow + wallId/belowWallId +
     cobblestone_wall remote/remoteBelow)
   * (8) bottom half block OR (stair bottomNotBack AND !(baseBelow stair bottomFront))
   * (9) remote trap door closed
   * (10) base trap door open
   * (11) [§7 근사] ASGrapplingHook/RopesPlus isASRope + isASGrapplingHookFront 생략
   * (12)-(13) FreeBaseClimb baseVineClimbing(0) / remoteVineClimbing(0) > DefaultMeta →
     Blocks.VINE.getDefaultState() + meta
   * (14) default NoGrab
4. **핵심 매핑**:
   * `Block.getBlockFromName("iron_bars")` → `Blocks.IRON_BARS`
   * `Block.getBlockFromName("cobblestone_wall")` → `Blocks.COBBLESTONE_WALL`
   * `Block.getBlockFromName("vine")` → `Blocks.VINE.getDefaultState()`
   * `Config.isFreeBaseClimb()` / `Config._freeFenceClimbing.value` → 1.21.1 헬퍼/필드
   * 모든 Block 파라미터 → BlockState (B-19a1a grabBlock 필드 타입 일관)
5. **본체 §7 B-19a2c 근사 3건 등록** (config 헬퍼 + mod 2 카테고리).

**완료 전 검증 체크리스트 (세션 104 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L608-L726 전수 read (세션 99) +
  WebFetch Config 기본값 확인 ✓
- [근거] 의존 메서드 전수 충족 — B-19a1a/b/c1/c2/c3a/c3b/c3c/c4/a1/a2/a3/a4/b 이식 ✓
- [대응] 14 분기 원본 ↔ 1.21.1 side-by-side. 13 vanilla 이식 + 3 mod 근사 생략 +
  Config 헬퍼/필드 신설. ✓
- [분기] 원본 14 분기 전수 식별 (vanilla 11 + mod 3 + default). `freeFenceClimbing` 내부
  6 서브 분기 전수. bottom-half/stair 복합 조건 + trap door open/closed 분리 전수 ✓
- [상수] `Blocks.IRON_BARS` / `Blocks.COBBLESTONE_WALL` / `Blocks.VINE` vanilla 상수 +
  `NoGrab=0` / `HalfGrab=1` / `AroundGrab=2` / `DefaultMeta=-1` 원본 동일 ✓
- [타이밍] hasHalfHold 는 isLadderSubstitute middle 분기에서 1회 호출. 단독 메서드 — 호출
  타이밍은 B-19a2e 에서 처리 ✓
- [근사] **§7 B-19a2c 근사 3건 등록 완료**:
  (1) isFreeBaseClimb() = freeClimb 근사
  (2) BetterThanWolves/RopesPlus rope+anchor 분기 생략
  (3) ASGrapplingHook/RopesPlus isASRope+isASGrapplingHookFront 분기 생략
  각 근사 지점 "근사 이식 — 원본과 차이: X" 주석 ✓
- [신규] Config `freeFenceClimbing` 필드 + `isFreeBaseClimb()` 헬퍼 신설 (B-19a2c
  범위 내). 본체 §6 Config 매핑 테이블 갱신은 후속 B-19a2d/e 완료 시 일괄 처리 ✓
- [회귀] 기존 코드 미사용. B-19a2e (isLadderSubstitute) 가 hasHalfHold 소비 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a2d** — `hasBottomHold` 본체 (원본 L728-L1000+, 300+줄). 유사
패턴 but 규모 더 큼 (하부 grab 판정 + RedPower wire 분기 + trap door/door + vine 3 서브 등).
mod 분기 근사 생략 (RedPower / BetterThanWolves / RopesPlus / ASRope / Carpenters).
의존 전수 충족. 예상 1 세션 (큰 세션).

**진행률** (세션 104 종료 시점):
- Extended 완료: **15 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / a2a4 / a2b / **a2c**)
- Extended 총 원자 ~61
- **Extended 진행률: 15/61 ≈ 25%**
- **포커스 #2 전체: (54+15)/115 ≈ 60%**

### 세션 103 — 2026-04-24 — B-19a2b grab 상태 세팅 + initialize 헬퍼 9 메서드

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L937-L995 (set*GrabType 6 + setGrabType static) + L2686-L2720 (initialize
   3) read** 완료.
2. **필드 2 추가**:
   * `base_jhd` (double) — `initialize` 에서 설정 / `initializeOffset` 이 소비
   * `local_halfOffset` (int) — `initializeLocal` 내부 중간값
3. **9 메서드 이식** (근사 없음):
   * `initialize(w, i, id, jhd, k, kd)` (원본 L2686-L2699) — world / base_i / base_id /
     base_jhd / base_k / base_kd 설정 + remote_i/remote_k 계산 (`base_i + _i`, `base_k + _k`)
   * `initializeOffset(offset_halfs, isClimbCrawling, isCrawlClimbing, isCrawling)` static
     (원본 L2701-L2713) — crawl flag OR + offset_jhd 계산 + `MathHelper.floor` + jh_offset /
     all_j / all_offset 분해. `MathHelper.floor_double` → `MathHelper.floor` 표면 매핑.
   * `initializeLocal(localOffset)` static (원본 L2715-L2720) — local_halfOffset/local_half/
     local_offset 계산
   * `setGrabType(type, block, remote, hasGrab, metaClimb)` static (원본 L987-L995) — 최종
     상태 필드 (grabRemote/grabType/grabBlock/grabMeta) 할당 + hasGrab 반환
   * `setHalfGrabType` 3 오버로드 (원본 L937-L960):
     - 2-arg 래퍼 (remote=true)
     - 3-arg 래퍼 (metaClimb=-1)
     - 4-arg 본체 — diagonal 진입 시 CCW/CW 2방향 `isUpperHalfFrontEmpty` AND 체크
   * `setBottomGrabType` 3 오버로드 (원본 L962-L985):
     - 동일 패턴, diagonal 체크에 `isLowerHalfFrontFullEmpty` 사용 (upper → lower 차이)
4. **BlockState 타입 일관성** — 원본 `Block` 파라미터 → `BlockState` 표면 매핑 (B-19a1a
   grabBlock 필드 타입과 일치).

**완료 전 검증 체크리스트 (세션 103 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L937-L995 + L2686-L2720 전수 read ✓
- [근거] B-19a1c4 isUpperHalfFrontEmpty + B-19a2a3 isLowerHalfFrontFullEmpty + B-19a1a
  상태 필드 (world/grabBlock/grabType/grabRemote/grabMeta) + B-19a0 rotate 전수 충족 ✓
- [대응] 9 메서드 원본 ↔ 1.21.1 side-by-side. set*GrabType 3+3 오버로드 + setGrabType
  static + initialize 3 ✓
- [분기] `setHalfGrabType` 본체 2갈래 (hasGrab + remote + _isDiagonal 시 엣지 체크 / 외
  직접 setGrabType). `setBottomGrabType` 동일 패턴. `initializeOffset` crawl 3-OR +
  offset 분해. 전수 식별 ✓
- [상수] `NoGrab=0` 원본 동일 / offset_halfs 파라미터로 `0D`/`3D` 전달 (B-19a3
  handsClimbing=3D / feetClimbing=0D) ✓
- [타이밍] `initialize` → `initializeOffset` → `initializeLocal` → `isLadderSubstitute`
  순서 확정 (B-19a2e/a3/a4 에서 소비). 본 세션은 선언만 ✓
- [근사] 근사 없음 ✓
- [신규] `base_jhd`/`local_halfOffset` 필드 2개 — 원본 static 필드 (L2730+). B-19a2a/b
  범위에 맞게 신설 ✓
- [회귀] 기존 코드 미사용. B-19a2c/d (hasHalfHold/hasBottomHold) 가 set*GrabType 호출 +
  B-19a3 (handsClimbing/feetClimbing) 가 initializeOffset 호출 + B-19a2e
  (isLadderSubstitute) 가 initializeLocal 호출 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a2c** — `hasHalfHold` 본체 (원본 L608-L726). vanilla 분기
(isOnLadder/isOnLadderFront/iron_bars/wall/fence/cobblestone/slab/stair/trapDoor/door/
baseVineClimbing/remoteVineClimbing) 이식 + mod 분기 (BetterThanWolves/RopesPlus/ASRope
/Carpenters) 근사 생략. 의존 모두 이식 완료 (B-19a2a1-a4 + B-19a2b). 예상 1 세션.

**진행률** (세션 103 종료 시점):
- Extended 완료: **14 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / a2a4 / **a2b**)
- Extended 총 원자 ~61
- **Extended 진행률: 14/61 ≈ 23%**
- **포커스 #2 전체: (54+14)/115 ≈ 59%**

### 세션 102 — 2026-04-24 — B-19a2a4 잔여 보조 10 메서드 — **B-19a2a 전체 완료**

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L1241-L1272 + L1386-L1440 + L1487-L1511 + L2011-L2024 read** — 10 메서드 확인.
2. **10 메서드 이식** — vanilla 2 (1:1) + mod 8 (근사):
   * **vanilla 1:1**:
     - `getTriple(primary, secondary)` static (원본 L2011-L2024) — 소수부 중심 오프셋
       비교로 -1/0/1 반환. Pure math.
     - `isHeadedToRope()` (원본 L1386-L1412) — `getTriple` 9 분기 (3x3) 로 Orientation
       매칭. rope mod 판정용이지만 logic 자체는 pure.
   * **mod 근사 8개**:
     - `getCarpentersBlockData` static (Carpenters) → -1
     - `isOnMiddleLadderFront(j_offset)` (Carpenters 간접) → false
     - `isOnAnchorFront(j_offset)` (BetterThanWolves) → false
     - `getAnchorId(j_offset)` static (BetterThanWolves) → null
     - `isASGrapplingHookFront(state)` (ASGrapplingHook) → false
     - `getRopeId(j_offset)` static (BetterThanWolves/RopesPlus) → null
     - `isASRope(state)` static (ASRope) → false
     - `isASGrapplingHook(state)` static (ASGrapplingHook) → false
3. **본체 §7 B-19a2a4 근사 3건 등록**:
   (1) Carpenters (isOnMiddleLadderFront + getCarpentersBlockData)
   (2) BetterThanWolves (isOnAnchorFront + getAnchorId)
   (3) ASGrapplingHook/RopesPlus/ASRope (isASGrapplingHookFront + getRopeId + isASRope +
       isASGrapplingHook)

**완료 전 검증 체크리스트 (세션 102 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1241-L1272 + L1386-L1440 + L1487-L1511
  + L2011-L2024 전수 read ✓
- [근거] Carpenters/BetterThanWolves/ASGrapplingHook/RopesPlus/ASRope mod 1.21.1 미이식
  확인 (B-19a1b `isRope`/`isOnWallRope` false 근사 + §7 기존 등록과 일관) ✓
- [대응] 10 메서드 원본 ↔ 1.21.1 side-by-side. `getTriple` 5 분기 / `isHeadedToRope`
  3x3=9 분기 vanilla 1:1 + 8 mod 근사 false/null 단일 반환 ✓
- [분기] `getTriple` (|primary|*2 < |secondary| / primary > 0 / primary < 0 / 0) 4갈래 +
  `isHeadedToRope` (iTriple > 0 / < 0 / = 0) x (kTriple > 0 / < 0 / = 0) = 9갈래 전수
  이식. mod 근사 8개는 본래 다중 분기지만 전체 false/null 반환으로 효과 동일 ✓
- [상수] `getTriple` 내부 `0.5` 중심 오프셋 / `Math.abs(primary)*2` 비교 원본 동일 ✓
- [타이밍] 순수 math/logic — 호출 타이밍 무관 ✓
- [근사] **§7 B-19a2a4 근사 3건 등록 완료** (mod 3 카테고리 묶음) ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용. B-19a2c/d (hasHalfHold/hasBottomHold) 에서 이 헬퍼들을
  전수 소비. mod 근사 false/null 반환이 `hasHalfHold`/`hasBottomHold` 의 mod 분기를
  자연스럽게 건너뜀 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**B-19a2a (보조 헬퍼 전체 4 서브) 완료** — a2a1/a2a2/a2a3/a2a4. `hasHalfHold`/`hasBottomHold`
이식 (B-19a2c/d) 을 위한 선행 인프라 완성.

**다음 세션 권고**: **B-19a2b** — grab 상태 세팅. `setHalfGrabType` 3 오버로드 +
`setBottomGrabType` 3 오버로드 + `initializeLocal` + `initializeOffset` + `initialize(world,
i, id, jhd, k, kd)`. 원본 L937-L996 + 별도 위치. `hasHalfHold` 본체가 이 setter 를 직접
소비. 예상 1 세션.

**진행률** (세션 102 종료 시점):
- Extended 완료: **13 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / a2a3 / **a2a4**)
- Extended 총 원자 ~61
- **Extended 진행률: 13/61 ≈ 21%**
- **포커스 #2 전체: (54+13)/115 ≈ 58%**

### 세션 101 — 2026-04-24 — B-19a2a3 half-solid 판정 (isLowerHalfFrontFullEmpty + isUpperHalfFrontAnySolid + isUpperHalfFrontFullSolid)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2065-L2153 read** — 3 메서드 + 의존 확인.
2. **3 메서드 이식**:
   * `isLowerHalfFrontFullEmpty(i, j_offset, k)` (원본 L2065-L2115):
     6 vanilla 분기 OR + 4 mod 근사 생략. isFullEmpty / stair top front / slab TOP /
     wall 관통 / door 열림 (vanilla) + RedPower / BetterThanWolves / ASRope / LadderKit
     (근사 생략).
   * `isUpperHalfFrontAnySolid(i, j_offset, k)` (원본 L2117-L2125):
     `isUpperHalfFrontFullSolid` 기본 + wall 블록 + !headedToFrontWall 시 solid=false 감쇠.
   * `isUpperHalfFrontFullSolid(i, j_offset, k)` static (원본 L2127-L2153):
     `isSolid` 기본 + 5 vanilla 제외 (sign/wall_sign/pressurePlate/trapDoor/openFenceGate)
     + 2 mod 근사 (ASGrapplingHook / Carpenters).
3. **의존 전수 충족**:
   * B-19a1a: isFullEmpty (좌표 오버로드) / isSolid / getBlock / world
   * B-19a1b: isTrapDoor
   * B-19a1c1: isStairCompact / isTopStairCompactFront / isTopHalfBlock /
     isWallBlock / isDoor / isOpenFenceGate / AbstractSignBlock / WallSignBlock /
     PressurePlateBlock
   * B-19a1c3a: isDoorFrontBlocked
   * B-19a1c3c: headedToFrontWall
   * B-19a0: rotate
4. **본체 §7 B-19a2a3 근사 2건 등록**.

**완료 전 검증 체크리스트 (세션 101 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L2065-L2153 전수 read ✓
- [근거] 의존 메서드 (세션 90-100 이식 전수) 충족 ✓
- [대응] 3 메서드 원본 ↔ 1.21.1 side-by-side. `isLowerHalfFrontFullEmpty` 6 vanilla 분기 +
  4 mod 근사 / `isUpperHalfFrontAnySolid` 2 갈래 / `isUpperHalfFrontFullSolid` 5 vanilla
  제외 + 2 mod 근사 ✓
- [분기] `isLowerHalfFrontFullEmpty` 9 분기 (vanilla 6 + mod 3 + LadderKit 복귀 1)
  전수 식별. `isUpperHalfFrontFullSolid` 7 분기 (base + 5 vanilla 제외 + 2 mod) ✓
- [상수] 없음 (class/property 기반 식별) ✓
- [타이밍] 순수 BlockState 조회 — 호출 타이밍 무관 ✓
- [근사] **§7 B-19a2a3 근사 2건 등록 완료**. 각 생략 지점에 "근사 이식 — 원본과 차이: X"
  주석 ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용. B-19a2c/d (hasHalfHold/hasBottomHold) 가 이 헬퍼를 소비 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a2a4** — 잔여 보조 (`isOnMiddleLadderFront` + `isHeadedToRope`
mod 근사 false + `getTriple` 등). 원본 L1386+ 와 추가 위치 grep 필요. `isOnMiddleLadderFront`
위치도 확인 요.

**진행률** (세션 101 종료 시점):
- Extended 완료: **12 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / a2a1 / a2a2 / **a2a3**)
- Extended 총 원자 ~61 (세션 99 재분해 후)
- **Extended 진행률: 12/61 ≈ 20%**
- **포커스 #2 전체: (54+12)/115 ≈ 57%**

### 세션 100 — 2026-04-24 — B-19a2a2 vine 보조 (baseVineClimbing + remoteVineClimbing 2 오버로드씩)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 `.tmp_research/Orientation.java.md` L1087-L1146 재확인** (세션 92 read 완료).
2. **4 메서드 이식** (근사 없음):
   * `baseVineClimbing(int j_offset)` (원본 L1087-L1103) — int 반환 (DefaultMeta=-1 /
     VineFrontMeta=0 / VineSideMeta=1). `isOnVine` 선행 체크 + `isOnVineFront` 우선 +
     4 orthogonal vine 측면 체크.
   * `baseVineClimbing(int j_offset, Orientation orientation)` (원본 L1105-L1113) —
     자기 자신 제외 + `orientation.rotate(180).hasVineOrientation(world, base_i, local_offset
     + j_offset, base_k)` + `orientation.getHorizontalBorderGap() >= 0.65`.
   * `remoteVineClimbing(int j_offset)` (원본 L1120-L1132) — `isBehindVine && isOnVineBack`
     우선 → VineFrontMeta, 4 orthogonal 측면 체크 → VineSideMeta.
   * `remoteVineClimbing(int j_offset, Orientation orientation)` (원본 L1134-L1146) —
     `(base_i - orientation._i, j_offset, base_k - orientation._k)` 위치 vine + 방향 +
     경계거리 조건.
3. **의존 전수 충족**:
   * `isOnVine` / `isVine` / `getBlock` (B-19a1a)
   * `isOnVineFront` / `isBehindVine` / `isOnVineBack` / `hasVineOrientation` (B-19a1b)
   * `getHorizontalBorderGap()` 인스턴스 오버로드 (B-19a1a)
   * `rotate` (B-19a0)

**완료 전 검증 체크리스트 (세션 100 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1087-L1146 전수 read ✓
- [근거] 의존 메서드 (isOnVine/isVine/getBlock/isOnVineFront/isBehindVine/isOnVineBack/
  hasVineOrientation/getHorizontalBorderGap/rotate) 전수 이식 확인 ✓
- [대응] 4 메서드 원본 ↔ 1.21.1 side-by-side. `baseVineClimbing` front/side 우선순위 +
  4 orthogonal 재귀. `remoteVineClimbing` 동일 패턴. ✓
- [분기] `baseVineClimbing(j_offset)` 4 분기 (vine 없음 / front / 4 orthogonal side / 외) +
  `baseVineClimbing(j_offset, orientation)` 2 갈래 (this → false / 실제 조건) +
  `remoteVineClimbing(j_offset)` 동일 + `remoteVineClimbing(j_offset, orientation)` 동일 ✓
- [상수] `DefaultMeta=-1` / `VineFrontMeta=0` / `VineSideMeta=1` (B-19a0 이식) +
  `0.65F` / `0.65` 경계거리 임계값 ✓
- [타이밍] 순수 메서드 호출 — 호출 타이밍 무관 ✓
- [근사] 근사 없음 ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용. B-19a2c/d (hasHalfHold/hasBottomHold) + B-19a2e (isLadderSubstitute)
  가 이 헬퍼를 소비 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a2a3** — half-solid 판정. `isLowerHalfFrontFullEmpty` (원본 L2065-L2112,
RedPower + BetterThanWolves 근사 포함) + `isUpperHalfFrontAnySolid` + `isUpperHalfFrontFullSolid`
(원본 위치 확인 필요). 예상 1 세션.

### 세션 99 — 2026-04-24 — B-19a2a1 wall 판정 보조 (headedToFrontSideWall + headedToBaseWall 3 + headedToBaseGrabWall 2)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L608-L1000+ 범위 재확인** — `hasHalfHold` 120줄 + `hasBottomHold` 300+줄 +
   `setHalfGrabType` 3 오버로드 + 보조 헬퍼 10+ (isOnMiddleLadderFront / headedToBaseWall /
   headedToBaseGrabWall / headedToFrontSideWall / isLowerHalfFrontFullEmpty /
   isUpperHalfFrontAnySolid / isUpperHalfFrontFullSolid / baseVineClimbing /
   remoteVineClimbing / isHeadedToRope) 의존 확인.
2. **Extended §3 B-19a2 서브 8개 재분해** — a2a1/a2a2/a2a3/a2a4/a2b/a2c/a2d/a2e. 세션
   98 예상 "2-3 세션" 은 과소평가 — 실제 4-6 세션 + mod 근사 처리 필요.
3. **B-19a2a1 이식** — wall 판정 보조 5 메서드:
   * `headedToFrontSideWall(i, j_offset, k, state)` (원본 L1759-L1798) — 4방향 wall flag
     + allOnNone 재해석 + base_id/base_kd topHalf 2x2 조합별 4 갈래 4방향 headedToWall OR
   * `headedToBaseWall(j_offset, state)` (원본 L1809-L1839) — 4 diagonal 2x2 조합 → 다음
     오버로드 호출
   * `headedToBaseWall(diagonal, left, right, leftFront, rightFrontOpposite, rightFront,
     leftFrontOpposite, co, leaf)` (원본 L1841-L1855) — 3 갈래 (diagonal / left / right)
   * `headedToBaseWall(front, sideOpposite, side, frontOpposite, coreOnly)` static (원본
     L1857-L1863) — 5 boolean 조합 OR
   * `headedToBaseGrabWall(j_offset, state)` (원본 L1865-L1915) — base + above (j_offset+1)
     위치 flag 수집 + above 블록 상태별 4갈래 분기 + base_id/base_kd topHalf 2x2 조합으로
     static 헬퍼 호출
   * `headedToBaseGrabWall(i, k, front, side, frontOpposite, sideOpposite, aboveFront,
     aboveSide, aboveFrontOpposite, aboveSideOpposite)` static (원본 L1917-L1938) — 6-갈래
     OR 조합
4. **근사 없음** — B-19a1c3b `getWallFlag` 의 BlockState property 근사에만 간접 의존.

**완료 전 검증 체크리스트 (세션 99 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1759-L1798 + L1809-L1938 전수 read ✓
- [근거] B-19a1c3b wall-flag 인프라 + B-19a1a isFullEmpty + B-19a1c1 isWallBlock + B-19a1c3b
  isTopHalf 전수 의존 충족 ✓
- [대응] 5 메서드 원본 ↔ 1.21.1 side-by-side. `headedToFrontSideWall` 2x2 = 4갈래 /
  `headedToBaseWall` instance 2x2 = 4갈래 → 다음 오버로드 / `headedToBaseWall` 3갈래
  오버로드 → static 5-flag OR / `headedToBaseGrabWall` above 3갈래 + 2x2 = 4갈래 /
  static 6-OR 전수 ✓
- [분기] `headedToFrontSideWall` iTop/kTop 2x2 (4 갈래) + 각 4 방향 OR /
  `headedToBaseWall` iTop/kTop 2x2 + diagonal / left / right 3갈래 + static 5-flag OR /
  `headedToBaseGrabWall` above 블록 상태 3갈래 (fullEmpty / wallBlock / other) + iTop/kTop
  2x2 + static 6-flag OR. 전수 식별 ✓
- [상수] 없음 (flag 조합 + `this._i`/`this._k` 부호 비교) ✓
- [타이밍] 순수 flag 계산 — 호출 타이밍 무관 ✓
- [근사] 근사 없음. 간접적으로 B-19a1c3b `getWallFlag` 의 BlockState property 근사에 의존 ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용. B-19a2c/d (hasHalfHold/hasBottomHold) 가 이 헬퍼를 소비 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-19a2a2** — vine 보조 (baseVineClimbing 2 오버로드 + remoteVineClimbing
2 오버로드). 원본 L1087-L1145. 비교적 단순 — 1 세션에 완료 예상. `hasVineOrientation`
(B-19a1b) + `isVine` (B-19a1a) 의존 충족.

**진행률** (세션 99 종료 시점):
- Extended 완료: **10 원자** (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c /
  a1c4 / **a2a1**)
- Extended 총 약 61 원자 (세션 99 B-19a2 서브 8개 재분해로 +7)
- **Extended 진행률: 10/61 ≈ 16%**
- **포커스 #2 전체: (54+10)/(54+61) = 64/115 ≈ 56%**

### 세션 98 — 2026-04-24 — B-19a1c4 accessibility 최종 (isFullAccessible 외) — B-19a1c 전체 완료

**사용자 지시**: "무조건 엄격 1대1 완료" + "진행률 퍼센트도 적어줘".

**진행한 작업**:
1. **원본 L2486-L2535 + L2325-L2331 + L2543-L2586 read** — B-19a1c 최종 서브 범위 확정.
2. **5 메서드 이식**:
   * `getWallBlockId(i, j_offset, k)` (원본 L2325-L2331) — 보조 헬퍼, wall 블록 반환 또는
     null.
   * `isFullAccessible(j_offset, grabRemote)` (원본 L2528-L2535) — grabRemote 2분기 (base +
     remote + access 3-AND / base 단독 isEmpty).
   * `isFullExtentAccessible(j_offset, grabRemote)` (원본 L2486-L2512) — RedPower 생략 →
     단순 `isFullAccessible` 전달.
   * `isJustLowerHalfExtentAccessible(j_offset)` (원본 L2514-L2526) — top half block OR
     top stair compact front. 근사 없음.
   * `isUpperHalfFrontEmpty(i, j_offset, k)` (원본 L2543-L2586) — 7 분기 OR:
     isFullEmpty / bottom half block / bottom stair front / trap door / wall 패턴 관통.
     RedPower + LadderKit 근사 생략.
3. **본체 §7 B-19a1c4 근사 3건 등록**:
   (1) `isFullExtentAccessible` RedPower 분기 생략
   (2) `isUpperHalfFrontEmpty` RedPower 분기 생략
   (3) `isUpperHalfFrontEmpty` LadderKit 분기 생략

**완료 전 검증 체크리스트 (세션 98 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L2325-L2331 + L2486-L2535 + L2543-L2586
  전수 read ✓
- [근거] B-19a1a~c3c 모든 의존 메서드 (isBaseAccessible / isRemoteAccessible /
  isAccessAccessible / isEmpty / isFullEmpty / isTopHalfBlock / isBottomHalfBlock /
  isStairCompact / isTopStairCompactFront / isBottomStairCompactFront / isTrapDoor /
  isWallBlock / headedToFrontWall) 전수 충족 ✓
- [대응] 5 메서드 원본 ↔ 1.21.1 side-by-side. `isFullAccessible` grabRemote 2분기 + 3-AND
  누적 / `isUpperHalfFrontEmpty` 7 분기 OR 누적 (vanilla 4 이식 + mod 2 근사 + wall 패턴
  관통 1 이식) ✓
- [분기] `isFullAccessible` grabRemote 2갈래 / `isJustLowerHalfExtentAccessible` 2-OR /
  `isUpperHalfFrontEmpty` 7-OR (그 중 2 mod 근사) / `isFullExtentAccessible` RedPower
  근사 단순화. `getWallBlockId` null 반환 삼항 ✓
- [상수] 없음 ✓
- [타이밍] 순수 BlockState 조회 — 호출 타이밍 무관 ✓
- [근사] **§7 B-19a1c4 근사 3건 등록 완료** ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용. B-19a1c 전체 완료 — B-19a2 `isLadderSubstitute` 가 이 헬퍼
  들을 소비할 예정 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**B-19a1c 전체 완료 (c1/c2/c3a/c3b/c3c/c4 = 6 서브)** — accessibility 판정 헬퍼 전수 이식.

**다음 세션 권고**: **B-19a2** — `isLadderSubstitute` 본체 (원본 L477-L606) + `hasHalfHold`
(L608-L648) + `hasBottomHold`. gap 1-5 계산 + `ClimbGap.canStand/mustCrawl/Block/Direction`
설정 핵심. B-19a1c 의 accessibility 헬퍼 7+ 를 직접 사용. 규모 매우 큼 — 2-3 세션 분할
예상. Agent WebFetch 로 원본 `hasHalfHold` / `hasBottomHold` / `setHalfGrabType` 등 추가
확보 필요.

**진행률** (세션 98 종료 시점):
- 본체 포커스 #2: B Phase 1/2 54 원자 + C-1/C-2/C-3 = **완료**
- Extended 총 원자 약 54 (세션 89/91/93/95 재분해 후)
- Extended 완료: 9 원자 (B-19a0 / a1a / a1b / a1c1 / a1c2 / a1c3a / a1c3b / a1c3c / a1c4)
- **Extended 진행률: 9/54 ≈ 17%**
- **포커스 #2 전체 (본체 + Extended): (54+9) / (54+54) = 63/108 ≈ 58%**
- Phase 3 B-19a1 (accessibility 관련): a1a/a1b/a1c 완료 → **B-19a1 완료 임박** (a2/a3/a4 남음)

### 세션 97 — 2026-04-24 — B-19a1c3c `headedToFrontWall` + `headedToRemoteFlatWall` + `isRemoteAccessible`

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L1741-L1757 + L1941-L1951 + L2401-L2475 재확인** — 세션 94-95 에서 이미 대부분
   read 완료. B-19a1c3 서브 마지막 (c3c).
2. **3 메서드 이식**:
   * `headedToFrontWall(i, j_offset, k, state)` (원본 L1741-L1757):
     - 4방향 wall flag (ZN/ZP/NZ/PZ) 수집 → `getWallFlag` (B-19a1c3b)
     - `allOnNone` (pane) + 전부 false → 4방향 true 승격 (고립 pane)
     - 4방향 headedToWall (NZ/PZ/ZN/ZP 대응 반대쪽 wall flag) OR
   * `headedToRemoteFlatWall(state, j_offset)` (원본 L1941-L1951):
     - `!this && rotate(90) && !rotate(180) && rotate(-90)` 4방향 AND
     - 평평한 벽 (flat wall) 패턴 — 진행 방향 연결 없고 옆 방향 연결
   * `isRemoteAccessible(j_offset)` (원본 L2401-L2475):
     - 12+ 분기 OR 누적 — isEmpty / trap door (front 체크) / door (frontBlocked) /
       remoteLadderClimbing / 닫힌 remote trap door / remote wall block / remote 아래
       fence (cobblestone_wall 예외 + headedToRemoteFlatWall) / remote door
     - 2 mod 분기 근사 생략 (RedPower + ASRope)
3. **핵심 매핑**:
   * 원본 `isTrapDoor(id) && !isTrapDoorFront(getBlockMetadata(...))` → 1.21.1 `isTrapDoor(baseState)
     && !isTrapDoorFront(baseState)` (BlockState 파라미터 직접 전달)
   * 원본 `isClosedTrapDoor(getRemoteBlockMetadata(j_offset))` → 1.21.1 `isClosedTrapDoor(remote_i,
     j_offset, remote_k)` (B-19a1c2 좌표 래퍼)
   * 원본 `Block.getBlockFromName("cobblestone_wall")` → 1.21.1 `Blocks.COBBLESTONE_WALL`
4. **본체 §7 B-19a1c3c 근사 2건 등록**:
   (1) RedPower 분기 생략 (원본 L2404-L2422)
   (2) ASRope 분기 생략 (원본 L2467-L2471)

**완료 전 검증 체크리스트 (세션 97 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1741-L1757 + L1941-L1951 + L2401-L2475
  전수 read ✓
- [근거] B-19a1c3b wall-flag 인프라 + B-19a1c3a remoteLadderClimbing/isDoorFrontBlocked +
  B-19a1b isTrapDoorFront + B-19a1c1 isWallBlock/isDoor/isFence + B-19a1c2 isEmpty/좌표
  trapdoor 래퍼 + B-19a1a getBaseBlockId/getRemoteBlockId 전수 충족 ✓
- [대응] 3 메서드 원본 ↔ 1.21.1 side-by-side. `headedToFrontWall` 4방향 flag + allOnNone
  재해석 + 4방향 headedToWall OR. `isRemoteAccessible` 12+ 분기 중 vanilla 10 이식 +
  mod 2 근사 ✓
- [분기] `headedToFrontWall` 3갈래 (flag 수집 / allOnNone + 전부 false 승격 / 4방향 OR).
  `headedToRemoteFlatWall` 4-AND. `isRemoteAccessible` 메인 분기 ~12 개 전수 식별
  (accessible=isEmpty / RedPower 생략 / accessible 상태 역체크 3 / closed trap door /
  !accessible 상태 wall/fence/door/ASRope) ✓
- [상수] `COBBLESTONE_WALL` 예외 확인. `_blockCarpentersLadder` 없음 ✓
- [타이밍] 순수 BlockState 조회 — 호출 타이밍 무관 ✓
- [근사] **§7 B-19a1c3c 근사 2건 등록 완료**. RedPower + ASRope 각 분기 위치에 "근사 이식
  — 원본과 차이: X" 주석 ✓
- [신규] 없음. `isTrapDoorFront` 시그니처가 BlockState 파라미터 (B-19a1b) 이므로 원본
  metadata 전달이 BlockState 전달로 자연 교체 ✓
- [회귀] 기존 코드 미사용 → 영향 없음. 의존 메서드 전수 충족. `Blocks.COBBLESTONE_WALL`
  참조로 Blocks import 추가 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-19a1c4** — accessibility 판정 나머지 4 메서드:
`isFullAccessible` (L2528-L2535, grabRemote 분기) + `isFullExtentAccessible` (L2486-L2512,
RedPower 포함) + `isJustLowerHalfExtentAccessible` (L2514-L2526) + `isUpperHalfFrontEmpty`
(L2543-L2580+). 의존 모두 충족 — c4 완료 시 B-19a1c (accessibility 전체) 완료. 예상 1 세션.

### 세션 96 — 2026-04-24 — B-19a1c3b wall-flag 인프라 (getWallFlag 외)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L1727-L1738 + L1801-L1807 + L1953-L2009 read** — wall-flag 인프라 5 메서드 확인.
2. **1.21.1 클래스 구조 파악**:
   * `ConnectingBlock` (super) — NORTH/SOUTH/EAST/WEST BooleanProperty 선언. `PaneBlock` /
     `FenceBlock` 공통 부모.
   * `WallBlock` — NORTH_SHAPE/SOUTH_SHAPE/EAST_SHAPE/WEST_SHAPE EnumProperty<WallShape>.
     `!= WallShape.NONE` 이면 연결.
   * `FenceGateBlock.FACING` Direction + `OPEN` Boolean.
3. **5 메서드 + 3 보조 이식**:
   * `toBlockDirection()` (Orientation → Direction 매핑 헬퍼): NZ/PZ/ZN/ZP → WEST/EAST/NORTH/SOUTH
   * `isFenceGateFront(state)` (원본 L1727-L1738): metadata % 4 → FenceGateBlock.FACING
     매핑. NZ/PZ = EW 축 gate (SOUTH/NORTH facing) / ZP/ZN = NS 축 gate (WEST/EAST facing).
     근사 없음 — 1:1.
   * `headedToWall(base, result)` (원본 L1801-L1807): `this == base || base.rotate(±45)`.
     근사 없음.
   * `getAllWallsOnNoWall(state)` (원본 L2001-L2004): `instanceof PaneBlock`. 근사 없음.
   * `isTopHalf(d)` (원본 L2006-L2009): pure math. 근사 없음.
   * `getWallFlag(direction, i, j_offset, k, state)` (원본 L1953-L1999): 6 분기 —
     PaneBlock → getConnectingFlag / FenceBlock → getConnectingFlag / WallBlock →
     getWallShapeFlag / FenceGateBlock → isClosedFenceGate && isFenceGateFront / Carpenters
     생략 / default false. **§7 근사 3건 등록**.
   * `getConnectingFlag(state, direction)` / `getWallShapeFlag(state, direction)` — property
     조회 헬퍼 2개 (private static).
4. **본체 §7 B-19a1c3b 근사 3건 등록**:
   (1) Pane/Fence/Wall 동적 canConnect* 계산 → BlockState property 캐시 조회
   (2) BetterMisc reflection 분기 생략
   (3) Carpenters 분기 생략

**완료 전 검증 체크리스트 (세션 96 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1727-L1738 + L1801-L1807 + L1953-L2009
  전수 read ✓
- [근거] 1.21.1 `ConnectingBlock.NORTH/SOUTH/EAST/WEST` + `WallBlock.*_SHAPE` +
  `WallShape.NONE` 확인 ✓
- [대응] 5 메서드 + 3 보조 원본 ↔ 1.21.1 side-by-side. fence gate direction % 4 매핑과
  FACING Direction 4 case 전수 ✓
- [분기] `getWallFlag` 6 분기 (Pane/Fence/Wall/FenceGate/Carpenters/default). `isFenceGateFront`
  4 갈래 (NZ·PZ / ZP·ZN / 나머지 false) + diagonal false. `headedToWall` 3 갈래 ✓
- [상수] `WallShape.NONE` 비교 기준 원본 `canConnect*` 반환 true 와 등가 (1.21.1 wall 은
  "연결 없음" = NONE, 그 외 모두 연결) ✓
- [타이밍] 순수 BlockState 조회 — 호출 타이밍 무관 ✓
- [근사] **§7 B-19a1c3b 근사 3건 등록 완료**. getWallFlag 내 각 분기에 "근사 이식 — 원본과
  차이: X" 주석 ✓
- [신규] `toBlockDirection` / `getConnectingFlag` / `getWallShapeFlag` 보조 메서드 3개 —
  원본엔 없으나 1.21.1 property 조회 구조 편의. Extended §3 의 원자 정의 변경 없음 ✓
- [회귀] 기존 코드 미사용 → 영향 없음. B-19a1a/b/c1/c2/c3a 의존 충족 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a1c3c** — `headedToFrontWall` (L1741-L1757) +
`headedToRemoteFlatWall` (L1941-L1951) + `isRemoteAccessible` 본체 (L2401-L2475, 12+ 분기).
의존 모두 충족 (wall-flag 인프라 세션 96 + trap door/ladder/wallBlock/door/isFence 전부
이식). 예상 1-2 세션 — `isRemoteAccessible` 분기량이 많음.

### 세션 95 — 2026-04-24 — B-19a1c3a `remoteLadderClimbing` + `isAccessAccessible` + `isDoorFrontBlocked`

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L1115-L1118 + L1741-L2000 + L2301-L2323 + L2401-L2484 read** — `isRemoteAccessible`
   이 `getWallFlag` 를 BlockPane/Fence/Wall/FenceGate 별 `canConnect*` 메서드 호출로 쓰고,
   그 결과를 `headedToFrontWall` / `headedToRemoteFlatWall` 등 9+ 헬퍼에서 소비하는 구조
   확인. 한 세션 불가 → B-19a1c3 를 3 서브 (c3a/c3b/c3c) 로 재분해.
2. **Extended §3 B-19a1c3 서브 3개 재분해**.
3. **B-19a1c3a 이식** — wall-flag 인프라 무관한 3 메서드:
   * `remoteLadderClimbing(j_offset)` (원본 L1115-L1118) — 한 줄: `isBehindLadder &&
     isOnLadderBack`. B-19a1b 의존 충족.
   * `isAccessAccessible(j_offset)` (원본 L2477-L2484) — diagonal 전용 2 방향 isEmpty 체크.
   * `isDoorFrontBlocked(i, j_offset, k)` (원본 L2300-L2323) — 8 metadata case 전수 이식.
     upper half 재귀 + 4 쌍 (open/closed + facing 4방향) → `this._k < 0` / `_i > 0` /
     `_k > 0` / `_i < 0` 판정.
4. **vanilla 1.7.10 door metadata → 1.21.1 매핑**:
   * metadata 0-3 = lower + closed + facing EAST/SOUTH/WEST/NORTH
   * metadata 4-7 = lower + open + facing EAST/SOUTH/WEST/NORTH
   * metadata 8 = upper half
   * 1.21.1: `DoorBlock.FACING` Direction + `DoorBlock.OPEN` Boolean + `DoorBlock.HALF`
     DoubleBlockHalf 조합으로 8 case 전수 식별.

**완료 전 검증 체크리스트 (세션 95 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1115-L1118 + L2300-L2323 + L2477-L2484
  전수 read ✓
- [근거] vanilla 1.7.10 door metadata 비트 → 1.21.1 DoorBlock property 매핑 검증 (facing +
  open + half) ✓
- [대응] 3 메서드 원본 ↔ 1.21.1 side-by-side. `isDoorFrontBlocked` 8 case × 2 (open+closed)
  쌍 = 4 if 분기 전수 ✓
- [분기] `isDoorFrontBlocked` 8 metadata case 모두 식별: case 8 (upper recursion) +
  case 0/1/2/3/4/5/6/7 → 4 pairs (EAST open/SOUTH closed · SOUTH open/WEST closed ·
  WEST open/NORTH closed · NORTH open/EAST closed). `isAccessAccessible` 2 갈래 (!_isDiagonal
  / 2개 isEmpty AND). ✓
- [상수] 없음 (Direction enum + DoubleBlockHalf enum) ✓
- [타이밍] 호출 타이밍 무관 (순수 BlockState 조회) ✓
- [근사] **근사 없음** — vanilla door FACING/OPEN/HALF property 로 원본 metadata 를 1:1
  식별 가능. mod-specific 분기 없음 ✓
- [신규] 없음 ✓
- [회귀] 기존 코드 미사용 → 영향 없음. B-19a1a/b/c1/c2 의존 모두 충족 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a1c3b** — wall-flag 인프라. `getWallFlag(direction, i, j_offset,
k, state)` + `getAllWallsOnNoWall` + `headedToWall` + `isFenceGateFront` + `isTopHalf`.
1.21.1 에서는 BlockPane/Fence/Wall 의 NORTH/SOUTH/EAST/WEST BooleanProperty 로 원본
`canPaneConnectToBlock`/`canConnectFenceTo`/`canConnectWallTo` 근사 이식 (§7 추가 근사 1-2
등록 예상). 예상 1 세션.

### 세션 94 — 2026-04-24 — B-19a1c2 `isEmpty` + `isBaseAccessible` + trapdoor 좌표 래퍼

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2224-L2238 + L2342-L2399 + L2537-L2541 read** — 이전 세션들에서 거의 확인 완료.
   `isFullEmpty(Block)` 단일 파라미터를 1.21.1 에서 어떻게 호출할지 결정.
2. **이식 세트** (8 메서드):
   * `isTrapDoor(i, j_offset, k)` / `isClosedTrapDoor(i, j_offset, k)` /
     `isOpenTrapDoor(i, j_offset, k)` — 좌표 기반 trapdoor 래퍼 3 (원본 L2224-L2238)
   * `isFullEmpty(i, j_offset, k)` — 좌표 기반 오버로드 (B-19a1a `isFullEmpty(BlockState,
     World, BlockPos)` 3-arg 래퍼). 호출부 간소화 + static world 활용.
   * `isEmpty(i, j_offset, k)` (원본 L2537-L2541) — `isFullEmpty && !isFence(j_offset-1)`
   * `isBaseAccessible(j_offset)` 1-arg 래퍼 (원본 L2342-L2345)
   * `isBaseAccessible(j_offset, bottom, full)` 3-arg 본체 (원본 L2347-L2399) — 7 분기
     OR 누적
3. **vanilla 4 분기 1:1 이식**:
   * (1) `isEmpty(base_i, j_offset, base_k)`
   * (3) `isFullEmpty(baseBlock, world, pos)` — 블록 자체 비어있음
   * (4) `isOpenTrapDoor(base_i, j_offset, base_k)`
   * (5) bottom && `isClosedTrapDoor(base_i, j_offset, base_k)`
   * (6) !full && `isWallBlock(baseBlock)` — B-19a1c1 이식
   * (8) `isDoor(baseBlock)` — B-19a1c1 이식
4. **§7 근사 3건 등록** (본체 §7 추가):
   (1) RedPower wire 분기 (원본 L2352-L2369) 전체 생략
   (2) ASRope 분기 (원본 L2385-L2389) 생략 — B-19a1b `isRope`/`isOnWallRope` false 와 연동
   (3) Carpenters `_blockCarpentersLadder` 분기 (원본 L2394-L2396) 생략

**완료 전 검증 체크리스트 (세션 94 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L2224-L2238 + L2342-L2399 + L2537-L2541
  전수 read (세션 93 에서 대부분 read 완료) ✓
- [근거] 1.21.1 B-19a1a `isFullEmpty(BlockState, World, BlockPos)` / B-19a1b
  `isTrapDoor`/`isClosedTrapDoor` / B-19a1c1 `isFence`/`isWallBlock`/`isDoor` 의존
  전수 충족 ✓
- [대응] 8 메서드 원본 ↔ 1.21.1 side-by-side. 7 분기 OR 누적 순서 보존 (vanilla 분기 1/3/4/5/6/8
  + 근사 생략 2/7/9) ✓
- [분기] `isBaseAccessible` 7 분기 전수 식별. vanilla 분기 6개 이식 + mod 분기 3개 근사 생략
  주석 명시 ✓
- [상수] 없음 (조건 조합만) ✓
- [타이밍] `isEmpty` / `isBaseAccessible` 는 순수 조회 함수 — 호출 타이밍 무관. base_i/
  base_k/local_offset 는 `initialize()` 에서 설정되나 현재 `initialize` 자체는 B-19a2/a4
  에서 이식 예정 ✓
- [근사] **§7 B-19a1c2 근사 3건 등록 완료**. RedPower/ASRope/Carpenters 각 생략 위치에
  "근사 이식 — 원본과 차이: X" 주석 ✓
- [신규] 없음. `isFullEmpty(int, int, int)` 좌표 오버로드는 B-19a1a 3-arg 대응 편의 —
  원본 semantics 보존 ✓
- [회귀] 기존 Climber/ClientState 는 새 헬퍼 미사용 → 영향 없음. B-19a1c1 헬퍼와 연동 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a1c3** — `isRemoteAccessible` (12+ 분기, 원본 L2401-L2475) +
`isAccessAccessible` (diagonal 용, L2477-L2484) + `isDoorFrontBlocked` (L2301-L2323) +
`headedToFrontWall` (L1741+) + `headedToRemoteFlatWall` (L1941+) + `remoteLadderClimbing`
(L1115+). 복잡한 wall/door/ladder 조합 판정 — 예상 1-2 세션.

### 세션 93 — 2026-04-24 — B-19a1c1 블록 식별 + stair/slab/fence/wall/door 헬퍼

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2342-L2560 + 관련 헬퍼 L2020-L2340 read** — `isBaseAccessible` 7 분기 +
   `isRemoteAccessible` 12+ 분기 + stair/slab/fence/wall/door 식별 헬퍼 10+ 의존 확인.
   한 세션 불가 → B-19a1c 4 서브 재분해.
2. **Extended §3 B-19a1c 서브 4개 재분해** — c1/c2/c3/c4.
3. **B-19a1c1 이식** — 블록 식별 + stair/slab/fence/wall/door 판정 헬퍼 18개:
   * Stair 헬퍼 6 (원본 L1544-L1615): `isStairCompact` (class 기반) +
     `isTopStairCompact` (HALF) + `isStairCompactFront` (8 분기) +
     `isStairCompactBack` (8 분기) + `isTopStairCompactFront` + `isTopStairCompactBack` +
     `isBottomStairCompactFront` + `isBottomStairCompactNotBack`
   * Slab 헬퍼 3 (원본 L2026-L2056): `isHalfBlock` (SlabBlock 제외 DOUBLE) +
     `isTopHalfBlock` (SlabType.TOP) + `isBottomHalfBlock` (SlabType.BOTTOM + BedBlock 예외)
   * Fence 헬퍼 5 (원본 L2177-L2222): `isFenceBase` (FenceBlock + WallBlock) +
     `isFenceGate` + `isClosedFenceGate` + `isOpenFenceGate` + `isFence` (좌표 래퍼 포함)
   * Wall 헬퍼 1 (원본 L2333-L2340): `isWallBlock` (PaneBlock + FenceBase)
   * Door 헬퍼 2 (원본 L2289-L2298): `isDoor` (DoorBlock) + `isDoorTop` (HALF==UPPER)
4. **1.21.1 매핑 핵심** — vanilla 1.7.10 metadata → 1.21.1 BlockState property:
   * stair `metadata & 3`: 0=EAST/1=WEST/2=SOUTH/3=NORTH ↔ `StairsBlock.FACING`
   * stair `metadata & 4`: ↔ `StairsBlock.HALF == BlockHalf.TOP`
   * slab `metadata & 8`: ↔ `SlabBlock.TYPE == SlabType.TOP/BOTTOM`
   * door `metadata == 8`: ↔ `DoorBlock.HALF == DoubleBlockHalf.UPPER`
   * fenceGate `metadata & 4 == 0`: ↔ `!FenceGateBlock.OPEN`
5. **Import 추가 8건**: `DoorBlock` / `FenceBlock` / `FenceGateBlock` / `PaneBlock` /
   `SlabBlock` / `StairsBlock` / `WallBlock` + enum 3 (`BlockHalf` / `DoubleBlockHalf` /
   `SlabType`).
6. **본체 §7 B-19a1c1 근사 4건 등록**:
   (1) `isStairCompact` — `_knownCompactStairBlocks` (mod stair 리스트) 생략
   (2) `isHalfBlock` — `_knownHalfBlocks` (mod slab) 생략
   (3) `isBottomHalfBlock` — BetterThanWolves anchor 블록 예외 생략
   (4) `isWallBlock` — `_knownThinWallBlocks` + Carpenters `_blockCarpentersLadder` 생략

**완료 전 검증 체크리스트 (세션 93 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1544-L1615 / L2026-L2063 / L2177-L2222
  / L2289-L2340 대역 전수 read ✓
- [근거] vanilla 1.7.10 metadata 비트 → 1.21.1 BlockState property 매핑 각 블록 타입별
  1:1 검증 (stair facing/half / slab type / door half / fenceGate open) ✓
- [대응] 18 메서드 원본 ↔ 1.21.1 side-by-side. stair 8 분기 (orthogonal 4 + diagonal 4 OR) /
  slab 3 / fence 5 / wall 1 / door 2 전수 ✓
- [분기] `isStairCompactFront` 8 분기 + `isStairCompactBack` 8 분기 + `isBottomHalfBlock`
  BedBlock 예외 + `isWallBlock` PaneBlock OR FenceBase 전수 ✓
- [상수] 없음 (class/property 기반 식별) — 매핑 상수는 vanilla Direction/BlockHalf/
  DoubleBlockHalf/SlabType enum ✓
- [타이밍] 헬퍼는 호출 시점 무관 (pure block state inspection). `initialize` 후속 호출에서
  사용 ✓
- [근사] **§7 B-19a1c1 근사 4건 등록 완료**. 각 함수 JavaDoc 에 "근사 이식 — 원본과 차이"
  주석 ✓
- [신규] 없음. `BedBlock` 은 `net.minecraft.block.BedBlock` fully qualified 참조 (import 불필요
  — 1회 사용) ✓
- [회귀] 기존 Climber/ClientState 는 새 헬퍼 미사용 → compileClientJava 무영향. B-19a1a/b
  상태 필드/World 접근 헬퍼 활용 — getBlock 경유 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a1c2** — `isEmpty` + `isBaseAccessible` 2 오버로드 이식. 원본
L2342-L2399 (B-19a1c1 헬퍼 18개 대부분 직접 사용) + `isEmpty` L2537-L2541 +
`isOpenTrapDoor(i, j_offset, k)` / `isClosedTrapDoor(i, j_offset, k)` / `isTrapDoor(i, j_offset, k)`
좌표 래퍼 3. 추가 §7 근사: RedPower wire + ASRope + Carpenters 분기 생략 예상. 1 세션.

### 세션 92 — 2026-04-24 — B-19a1b `Orientation` front/back/rope/trapdoor 헬퍼 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지 (세션 90 프롬프트 재사용).

**진행한 작업**:
1. **원본 확보 추가 read** — `.tmp_research/Orientation.java.md` L1160-L1180 + L1217-L1238
   + L1274-L1309 + L1311-L1359 + L1361-L1384 + L1443-L1485 + L1513-L1541 대역.
2. **핵심 인사이트** — ladder/vine/trapdoor metadata 원본 매핑을 1.21.1 Direction property
   로 1:1 변환:
   * ladder `metadata & 0x7`: 5→NZ / 4→PZ / 2→ZP / 3→ZN = LadderBlock.FACING
     EAST/WEST/NORTH/SOUTH ↔ Orientation 반대측 접근 방향
   * vine metadata 비트: 0x1→ZP / 0x2→NZ / 0x4→ZN / 0x8→PZ = VineBlock.SOUTH/WEST/NORTH/EAST
   * trapdoor `metadata & 3`: 0→ZP / 1→ZN / 2→PZ / 3→NZ = TrapdoorBlock.FACING
     SOUTH/NORTH/EAST/WEST
3. **Direction import 추가**.
4. **14 메서드 이식** (원본 1:1, 근사 표기):
   * `hasLadderOrientation(i, j_offset, k)` (instance) — LadderBlock.FACING 4 분기
   * `hasVineOrientation(world, i, j, k)` (instance, world 파라미터) — VineBlock 비트 4 분기
   * `getKnownLadderOrientation(world, i, j, k)` (static) — Direction → Orientation 역매핑
   * `isOnLadderFront` / `isOnLadderBack` / `isOnVineFront` / `isOnVineBack` (instance) —
     base/remote + rotate(180) 조합
   * `isBehindLadder` / `isBehindVine` (static) — remote 위치 ladder/vine 체크
   * `isRope(j_offset)` / `isOnWallRope(j_offset)` — **전체 false 근사** (§7 B-19a1b-approx-2)
   * `isOnOpenTrapDoor(j_offset)` — base trap door + !closed
   * `isTrapDoorFront(state)` (instance) — 8 Orientation × Direction 매핑 (orthogonal 4 +
     diagonal 4 조합 OR)
   * `getOpenTrapDoorOrientation(world, i, j, k)` (static) — Direction → Orientation 역매핑
   * `isRemoteSolid(world, i, j, k)` — `(i+_i, j, k+_k)` 위치 solid 체크
5. **본체 §7 B-19a1b 근사 2건 등록**:
   (1) LadderKit/Carpenters 모드 분기 생략 (`_ladderKitLadderTypes`/`carpentersBlockData`)
   (2) isRope/isOnWallRope — BetterThanWolves/RopesPlus/ASRope 모드 블록 전체 false

**완료 전 검증 체크리스트 (세션 92 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L1160-L1541 대역 전수 read ✓
- [근거] vanilla 1.7.10 ladder/vine/trapdoor metadata 비트 매핑 → 1.21.1 Direction
  property 매핑 각 상수 별 1:1 검증 (EAST.getOpposite()=WEST=NZ 등) ✓
- [대응] 14 메서드 원본 ↔ 1.21.1 side-by-side. orthogonal 4 + diagonal 4 trap door
  front 매핑 + orthogonal 4 ladder/vine 매핑 전수 ✓
- [분기] `hasLadderOrientation` 4 분기 (NZ/PZ/ZP/ZN) + `hasVineOrientation` 4 분기 +
  `isTrapDoorFront` 8 분기 (4 orthogonal + 4 diagonal) + `getKnownLadderOrientation` /
  `getOpenTrapDoorOrientation` Direction switch 4 + `isBehindLadder` 3갈래 원본 전수 ✓
- [상수] LadderBlock.FACING / VineBlock.NORTH/SOUTH/EAST/WEST / TrapdoorBlock.FACING/OPEN
  property 타입 정합 (BooleanProperty / EnumProperty<Direction>) ✓
- [타이밍] 메서드 호출 시점은 `initialize(world, ...)` 의 상태 필드 설정 이후 — B-19a2/a4
  에서 정확한 호출 순서 구성 예정 ✓
- [근사] **§7 B-19a1b 근사 2건 등록 완료**:
  (1) LadderKit/Carpenters 분기 생략 — 주석 명시
  (2) Rope 3종 (fcRopeBlock/blockRopeCentral/blockRope) 전체 false — 주석 명시
- [신규] 없음. `isTrapDoorFront` 가 원본은 int metadata, 1.21.1 은 BlockState 파라미터로
  표면 매핑 (메타 추출 단계 제거) — 시멘틱 동일 ✓
- [회귀] 기존 `SmartMovingClimber.handleClimbing` 은 새 헬퍼 미사용 → compileClientJava
  무영향. B-19a1a 상태 필드/헬퍼와 연동 — `getBaseBlockId`/`getBlock` 활용 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (5s) ✓

**다음 세션 권고**: **B-19a1c** — accessibility 판정. `isBaseAccessible` 2 오버로드
(L2342/L2347) + `isFullAccessible` (L2528) + `isFullExtentAccessible` (L2486) +
`isJustLowerHalfExtentAccessible` (L2514) + `isUpperHalfFrontEmpty` (L2543). 원본 L2342-L2560
대역 전수 read 필요. 복잡한 블록 접근성 판정 — 예상 규모: 1-2 세션.

### 세션 91 — 2026-04-24 — B-19a1a `Orientation` 상태 필드 + core 헬퍼 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지 (세션 90 재사용 프롬프트).

**진행한 작업**:
1. **원본 확보 추가** — Agent WebFetch 로 `net.smart.moving.SmartMovingContext.java` 확인:
   상수/initialize 만 있고 수직 상태 헬퍼 없음. 실제 의존 헬퍼 전부 **`Orientation.java`
   내부** (L1148-L2625) 확인. L2729-L2744 에 static 상태 필드 13개 선언.
2. **Extended §3 B-19a1 서브 3개 분해** — 원본 범위가 너무 커서 한 세션 불가:
   * B-19a1a (이 세션): 상태 필드 + 블록 식별 헬퍼 + Material 2 + World 접근 3 + 기본
     ladder/vine 체크 3
   * B-19a1b: front/back/rope/trapdoor 인스턴스 헬퍼
   * B-19a1c: accessibility 판정 (`isBaseAccessible` 외)
3. **`Orientation.java` 상태 필드 13 추가** (원본 L2729-L2744 1:1):
   * `world` (World) / `all_j`/`all_offset` (int) / `base_i`/`base_k`/`remote_i`/`remote_k`
     (int) / `base_id`/`base_kd` (double) / `crawl` (boolean) / `local_half`/`local_offset`
     (int) / `grabRemote` (boolean) / `grabType` (int) / `grabBlock` (BlockState) / `grabMeta`
     (int) / `jh_offset` (double, B-19a2/a3 초기화)
4. **블록 식별 헬퍼 6 이식** (원본 L1188-L1214 + L2241):
   * `isLadder(state)` → `instanceof LadderBlock`
   * `isVine(state)` → `instanceof VineBlock`
   * `isLadderOrVine(state)` → OR (LadderKit 모드 체크 근사 생략)
   * `isTrapDoor(state)` → `instanceof TrapdoorBlock`
   * `isClosedTrapDoor(state)` → `isTrapDoor && !state.get(TrapdoorBlock.OPEN)`
   * `isClimbable(world, i, j, k)` → `BlockTags.CLIMBABLE` 태그 근사
5. **Material 헬퍼 2 이식** (원본 L2155-L2175, L2616-L2619):
   * `isSolid(state, world, pos)` — Material API 제거 → `state.isSolidBlock` 근사
   * `isFullEmpty(state, world, pos)` — `AbstractSignBlock`/`WallSignBlock`/
     `PressurePlateBlock` 예외 처리. ASGrapplingHook/RopesPlus 모드 체크 생략 (근사).
6. **World 접근 3 이식** (원본 L2621-L2639):
   * `getBlock(i, j_offset, k)` / `getBaseBlockId(j_offset)` / `getRemoteBlockId(j_offset)`
7. **기본 ladder/vine 체크 3 이식** (원본 L1148-L1186):
   * `isOnLadder(j_offset)` — ladder 또는 isClimbable 태그
   * `isOnVine(j_offset)` — vine 전용
   * `isOnLadderOrVine(j_offset)` — base 블록 OR `grabBlock` vine 체크
8. **`getHorizontalBorderGap()` 인스턴스 오버로드 추가** (원본 L231-L234) — `base_id`/
   `base_kd` 필드 이식으로 호출 가능해짐.
9. **본체 §7 에 B-19a1a 근사 4건 등록** — LadderKit / Forge ladder hook / Material API /
   mod 호환 블록. 각 근사 사유 + 1.21.1 대체 수단 명시.

**완료 전 검증 체크리스트 (세션 91 기준)**:
- [근거] 원본 `Orientation.java` L1148-L2744 대역 전수 read (`.tmp_research/Orientation.java.md`) ✓
- [근거] `SmartMovingContext.java` WebFetch → 상태 필드는 `Orientation` 내부 확정 ✓
- [대응] 13 상태 필드 + 6 블록 식별 + 2 Material + 3 World + 3 기본 체크 + 1 HorizontalBorderGap
  오버로드 원본 1:1 (근사 4건 제외) ✓
- [분기] `isOnLadder` 3갈래 (ladder / vine false / climbable) + `isFullEmpty` 4갈래 (null /
  solid / sign-pressure / 모드 생략) + `isLadderOrVine` OR + `isClosedTrapDoor` AND 전부
  원본 보존 ✓
- [상수] `DefaultMeta=-1` / `VineFrontMeta=0` / `VineSideMeta=1` / `NoGrab/HalfGrab/AroundGrab`
  / top/middle/base/sub/subSub 원본 값 유지 (세션 90 이식) ✓
- [타이밍] `initialize(world, i, id, jhd, k, kd)` 가 상태 필드 설정 후 헬퍼 호출 패턴
  원본 유지. `initialize` 자체는 B-19a2/a4 이식 시 추가 ✓
- [근사] **§7 B-19a1a 근사 4건 등록 완료**:
  (1) `isLadderOrVine` LadderKit 제외
  (2) `isClimbable` Forge hook → BlockTags.CLIMBABLE
  (3) `isSolid` Material → `isSolidBlock`
  (4) `isFullEmpty` mod 호환 체크 생략
  각 함수 JavaDoc 에 "근사 이식 — 원본과 차이: X" 주석 ✓
- [신규] `getHorizontalBorderGap()` 인스턴스 오버로드 (세션 90 JavaDoc 에 "B-19a1 이후
  추가" 로 예고, 본 세션에서 base_id/base_kd 필드 이식하며 자연 추가) ✓
- [회귀] 기존 `SmartMovingClimber.handleClimbing` 은 새 Orientation 필드/헬퍼 미사용 →
  compileClientJava 무영향. Config 변경 없음 (세션 90 이미 추가). 다른 이식 영향 없음 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (6s) ✓

**다음 세션 권고**: **B-19a1b** — front/back/rope/trapdoor 인스턴스 헬퍼 이식:
`isOnLadderFront` / `isOnLadderBack` / `isOnVineFront` / `isOnVineBack` (원본 L1217-L1238)
+ `hasLadderOrientation` / `hasVineOrientation` (원본 L1311+) + `isRope` / `isOnWallRope` /
`isOnOpenTrapDoor` / `isTrapDoorFront`. rope/wallRope 은 SmartMoving 모드 블록 →
**전체 false 근사** (§7 등록). 예상 규모: 1 세션.

### 세션 90 — 2026-04-24 — B-19a0 `Orientation` 클래스 기본 구조 신설

**사용자 지시**: "무조건 엄격 1대1 완료" — 세션 89 에서 제시한 근사 이식 대안 각하,
서브 5개 완전 이식 방침 확정.

**진행한 작업**:
1. **Config 2 필드 이식** (원본 SmartMovingConfig L129-L130):
   * `freeClimbingOrthogonalDirectionAngle = 90F` (Positive, 기본 90F)
   * `freeClimbingDiagonalDirectionAngle = 80F` (Positive, 기본 80F)
   * load (`loadProperties`) + save (`saveProperties`) 전수 이식.
   * WebFetch 로 원본 기본값 2건 직접 확인 완료.
2. **`choco.ratel.smartmoving.climbing.Orientation` 클래스 신설** — 원본 L36-L205 +
   L998-L1085 (각도 판정 인프라) 대역 1:1 이식:
   * 9 방향 상수 + Meta 3 + 내부 상수 8 + `Orthogonals` HashSet
   * 생성자 + `setClimbingAngles` 3 오버로드 (i/k 조합 → 방향각 스위치 + halfAreaAngle 계산
     + min/max 정규화)
   * `isWithinAngle` 2 오버로드 (인스턴스 + static) + `isRotationForClimbing`
   * `rotate(int angle)` 8갈래 switch (0/±45/±90/±135/±180)
   * `getOrientation(player, tolerance, orthogonals, diagonals)` — 플레이어 회전각 기반
     방향 선택. 1.21.1 `player.getYaw()` 표면 매핑.
   * `getClimbingOrientations(player, orth, diag)` — 정적 HashSet 캐시 반환 + `addTo`
     보조 (원본 L262-L297 1:1)
   * `getHorizontalBorderGap(i, k)` — 4 orthogonal 방향별 블록 경계 거리 계산 (static-coord
     버전만 이식; `base_id`/`base_kd` 기반 인스턴스 오버로드는 B-19a1 이후)
3. **원본 근거 주석 전수** — 각 메서드/필드마다 원본 L번호 (예 L39-L49 / L81-L88 /
   L998-L1037 / L1064-L1085) 매핑.
4. **`extends SmartMovingContext` 미적용** (B-19a1 에서 추가 예정) — B-19a0 은 순수
   기하/각도 판정만 포함. JavaDoc 에 명시.

**완료 전 검증 체크리스트 (세션 90 기준)**:
- [근거] 원본 `.tmp_research/Orientation.java.md` L36-L205 + L998-L1085 직접 read ✓
- [근거] 원본 `SmartMovingConfig.java` L129-L130 WebFetch 로 기본값 (90F / 80F) 확인 ✓
- [대응] 9 상수 + 5 내부 상수 + `_i`/`_k`/`_isDiagonal` + 각도 3필드 + 생성자 원본 1:1 ✓
- [대응] `setClimbingAngles` i/k 스위치 9갈래 + halfAreaAngle 계산 + min/max 정규화 원본 1:1 ✓
- [대응] `rotate` 8갈래 + 4 직접 매핑 + 4 재귀 원본 1:1 ✓
- [대응] `getOrientation` / `getClimbingOrientations` / `addTo` / `getHorizontalBorderGap`
  원본 1:1 ✓
- [분기] 모든 switch/if 분기 (setClimbingAngles 3x3 / rotate 8 / getOrientation 2x2 +
  8 방향) 전수 이식 ✓
- [상수] `90F`/`80F`/`135F/90F/45F/180F/0F/360F/225F/270F/315F` 등 각도 상수 원본 동일 ✓
- [타이밍] `Orientation` 은 정적 상수 초기화 (JVM 로딩 시 1회). 런타임 호출 타이밍 없음 ✓
- [근사] 없음 — 1:1 엄격 이식. `EntityPlayer` → `PlayerEntity` / `rotationYaw` →
  `getYaw()` / `Config._xxx.value` → `SmartMovingConfig.Config.xxx` 는 1.21.1 Fabric 표면 매핑 ✓
- [신규] B-19a0 에 `getHorizontalBorderGap` 인스턴스 오버로드 미포함 — B-19a1
  SmartMovingContext 이식 시 추가 명시. JavaDoc 에 기록 ✓
- [회귀] compileJava + compileClientJava 모두 BUILD SUCCESSFUL (6s) ✓
  * 기존 `Orientation` 없는 상태 → 신설이므로 기존 호출처 없음
  * Config 2 필드 추가는 기존 호출처 없음 (B-19a1~a4 에서 소비)
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (6s) ✓

**다음 세션 권고**: **B-19a1** — `SmartMovingContext` 수직 상태 헬퍼 이식. 원본
`SmartMovingContext.java` Agent WebFetch 확보 필요. 의존 헬퍼 10+ 이식 (`isOnLadderOrVine`/
`isOnOpenTrapDoor`/`isRope`/`isOnWallRope`/`isBaseAccessible`/`isFullAccessible`/
`isFullExtentAccessible`/`isJustLowerHalfExtentAccessible`/`isFullEmpty`/`isSolid`).
규모 큼 — 세션 2-3 회 분할 예상. B-19a2 `isLadderSubstitute` 본체가 이 모든 헬퍼 의존.

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
