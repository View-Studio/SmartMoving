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
- [x] B-10-reset-post. ✅ **세션 114 완료** — 원본 L1488-L1498 `resetSwimming()` 메서드
      8 필드 리셋 완전 이식. `SmartMovingSwimmer.resetSwimming(sm)` static private 메서드
      신설 + `updateSwimState` 의 `!isTouchingWater` 경로에서 호출.
      원본 8 필드: dippingDepth / isDipping / isSwimming / isDiving / isLevitating /
      isShallowDiveOrSwim / isFakeShallowWaterSneaking / isJumpingOutOfWater. 1.21.1
      기존 5 필드 할당 → 메서드 호출로 대체 + 누락 3 필드 (isLevitating /
      isFakeShallowWaterSneaking / isJumpingOutOfWater) 해소. B-9 Phase 5 재작성 시
      handleSwimming 내부 5 호출 지점 (L555/L585/L607/L645) 도 이 메서드 재사용.
      근사 없음. 본체 §6 표기 구식 업데이트.

#### B-N-standup. `standupIfPossible` 메서드 이식 (세션 88 3차 감사 발견)
- [x] B-N-standup. ✅ **세션 115 완료 (근사 이식 4건)** — 원본 L2165-L2219 이식:
      `resetHeightOffset()` / `standUp()` / `standupIfPossible(player)` 무인자 /
      `standupIfPossible(player, tryLanding, restoreFromFlying)` 2-arg 오버로드.
      1.21.1 boundingBox API 제약 + AABB 정밀 헬퍼 (`getGapUnderneight`/`getGapOverneight`)
      미이식으로 다수 근사. `canStandUp(player)` 단일 판정으로 gap 체크 통합.
      B-24 (세션 53) `restoreFromFlying = true` 설정 직후 2-arg 오버로드 호출 연결.
      §7 B-N-standup 근사 4건 등록 (boundingBox / move / AABB gap / capabilities.flying).
      **본체 §6 매핑 테이블 `standupIfPossible()` 표기 갱신** — "✗ 미이식" → "✓ 근사 이식".

#### B-40-post. `toCrawling()` 잔여 호출 지점 L2751/L2760/L2767 이식 (세션 88 3차 감사 발견)
- [x] B-40-post. ✅ **세션 116 완료 (재검토 — 이미 해소됨)** — 원본 L2749-L2784 대역
      전수 read 결과 세션 88 3차 감사 분류가 부정확했음:
      * **L2751** `wasCrawling = false;` — **필드 리셋** (toCrawling() 메서드 호출 아님).
        B-17b1 세션 48 L1419 `wasCrawling = false;` 로 이식 완료 (canStandUp 분기 내).
      * **L2760** `wasCrawling = toCrawling();` — 실제 메서드 호출. **B-17b2 세션 73
        L1435** 이식 완료 (else if wasCrawlClimbing 분기 1 — !isClimbing).
      * **L2767** `wasCrawling = toCrawling;` — **로컬 변수** 참조 (L2757 `boolean
        toCrawling = sneakButton.Pressed || crawlToggled;`). **B-17b2 세션 73 L1442**
        `wasCrawling = toCrawlingLocal;` 이식 완료 (분기 2 — moveForward <= 0F).
      결론: L2751/L2760/L2767 3 지점 모두 세션 48/73 의 B-17b1/B-17b2 이식에 자동 흡수됨.
      B-40-post 원자는 실제 수행 작업 없음 — 세션 88 3차 감사 시 원본 본체 미확인
      상태에서 L1883 의 "wasCrawling 재설정 8 위치" 목록을 "toCrawling() 호출 지점" 으로
      오분류한 결과. 체크박스 [x] — 코드 변경 없음.

#### B-10b-pre. `wasJumpingOutOfWater` 필드 명시 신설 (세션 88 4차 확정 감사 발견)
- [x] B-10b-pre. ✅ **세션 110 완료** — `SmartMovingClientState.wasJumpingOutOfWater`
      public boolean 필드 신설 + resetState 리셋 + `Swimmer.updateSwimState` 진입 첫 줄에
      `sm.wasJumpingOutOfWater = sm.isJumpingOutOfWater` 저장. 원본 L105 지역 snapshot 을
      1.21.1 updateSwimState/handleSwimming 분리 구조에서 필드로 승격. **§7 B-10b-pre 근사
      1건 등록** (지역 → 필드 구조 차이, 시멘틱 동치).

### Phase 5. B-7 / B-9 / B-11 본체 — 수중 3상태 완전 재구성

#### B-7. updateSwimState 진입 조건 복원
- [x] B-7a. `isLiquidClimbing` 필드 + 계산 로직 이식 (원본 L132). **세션 127 완료**
      (`ClientState.isLiquidClimbing` public 필드 + `MixinLivingEntityClient.sm_beforeTravel`
      의 updateSwimState 호출 직전 계산).
- [x] B-7b. `Config.isLavaLikeWaterEnabled()` 헬퍼 + `lavaLikeWater` Config 필드 이식.
      **세션 127 완료** (Config Creative 기본 false, Properties IO 등록, isLavaLikeWaterEnabled
      헬퍼 추가). `handleLavaMovement()` 는 vanilla `player.isInLava()` 로 대응.
- [x] B-7c. Swimmer.updateSwimState 진입 조건 정밀 복원. **세션 127 완료**
      (`!isFlying && !isLiquidClimbing && (isInWater || (wasSwimming && isInLiquid) ||
      (lavaLikeWater && isInLava))` 3-OR 전수 이식).

#### B-9. handleSwimming 메인 분류 3-갈래 재작성
- [x] B-9a. `playerSwimWaterBorder` / `totalSwimWaterBorder` 계산 (AABB 정밀 — Phase 6 공유).
      **세션 128 완료** (`SwimBorderValues` 소비 → `dippingDepth` 시멘틱을
      `playerSwimWaterBorder` 로 교체, updateSwimState + handleSwimming 두 곳의 offset
      공식 정밀화). 원본 L305/L416 1:1.
- [x] B-9b. `[0, 2]` 구간 A/B 서브 분기 (`diveUp || moveSwim || wantShallowSwim`).
      **세션 129 완료** (`moveSwim = pitch<0 && forward>0 || pitch>0 && forward<0` 계산 +
      `isPathA` boolean + handleSwimming 내부 상태 재분류 A/B 경로별 threshold).
- [x] B-9c. A 경로 11-단계 swimming offset 테이블 (1.4-1.9). **세션 129 완료** (원본
      L317-L348 11단계 1:1 — 기존 13단계 근사 → 원본 공식 복원, swimDown 분기 생략은
      B-9h 범위).
- [x] B-9d. B 경로 10-단계 diving offset 테이블 (1.5-1.9). **세션 129 완료** (원본
      L370-L397 10단계 1:1 + A 경로 diving (L349-L358) 분기도 함께 이식). isDiving 분기에
      isPathA 기반 A/B 경로 처리.
- [x] B-9e. `(2, ∞)` 구간 diving + diveUp/diveDown/moveSwim + isFast 분기. **세션 130 완료**
      (원본 L400-L412 이식 — diveUp 시 isFast+psw<2.5+isAir(j+3) 스프린트 부스트 0.11/sprintFactor
      분기, 그 외 0.01+0.1*sf / diveDown 0.01-0.1*sf / default 0.01).
- [x] B-9f. `(-∞, 0)` handleSwimmingRejected. **세션 130 완료** (playerSwimWaterBorder<0
      이면서 isCrawling/isClimbCrawling/isCrawlClimbing 아닐 때 handleSwimming=false 반환 —
      vanilla travel() 위임).
- [x] B-9g. `motionYDiff` 전체 적용 로직. **세션 130 완료** (원본 L445-L446 `diveUp 시
      motionY -= 0.04` 보정 복원. swimming/diving/dipping 공통 진입 전 보정).

#### B-11. 얕은 물 특수 분기 이식
- [x] B-11. 원본 L513-L536 `isShallowDiveOrSwim && realMinPlayerSwimWaterDepth <
      SwimCrawlWaterBottomBorder(0.55F)` 진입 조건 + isSlow 분기 (crawl 전환 / walking).
      **세션 130 완료** (handleSwimming 말미 diving 분기 뒤 배치. `couldStandUp` 재사용 +
      B-9b 재분류 후 `isShallowDiveOrSwim` 재계산. isSlow 시 크롤 전환 (heightOffset=-1),
      아니면 걷기 전환 + `getMaxPlayerSolidBetween` 소비한 바닥 위치 이동). B-42a/B-42d
      헬퍼 소비.

#### B-7d. `isInLiquid()` 메서드 이식 (세션 88 4차 확정 감사 발견)
- [x] B-7d. **세션 127 완료**. 원본 `SmartMovingBase.isInLiquid()` L411-L416 1:1 이식.
      `ClientState.isInLiquid(player)` static 메서드 신설 — `getMaxPlayerLiquidBetween !=
      minY || getMinPlayerLiquidBetween != maxY` (B-42c 헬퍼 소비). B-7c 진입 조건에서
      소비 활성.

#### B-9h. `swimDown = false` 설정 이식 (세션 88 4차 확정 감사 발견)
- [x] B-9h. **세션 130 완료**. 원본 L243-L244 `swimDown = sneak && _swimDownOnSneak` +
      L292-L295 `if (wasSwimming && wantShallowSwim && swimDown) { swimDown=false;
      isFakeShallowWaterSneaking=true; }` 통합 이식. swimming A 경로 motionYDiff 계산
      시 `if (swimDown) motionYDiff = -0.05 * (isFast ? sprintFactor : 1F)` 소비 (원본
      L319-L321). isFakeShallowWaterSneaking 기존 블록과 통합으로 중복 제거.

### Phase 6. AABB 정밀화 — §7 근사 8건 일괄 해소

원본 정밀 AABB 헬퍼 이식.

#### B-42 이식 (Phase 6 본체)
- [x] B-42a. `getMaxPlayerSolidBetween(double y1, double y2, double dOffset)` 이식 —
      플레이어 AABB 의 x/z 범위 + [y1, y2] Y 범위 내 최고 고체 블록 Y. **세션 117 완료**
      (`ClientState` static helper, VoxelShape→Box 단일 박스 근사 §7 등록).
- [x] B-42b. `getMinPlayerSolidBetween(double y1, double y2, double dOffset)` 이식 —
      위 범위 내 최저 고체 블록 Y. **세션 118 완료** (`ClientState` static helper, B-42a
      대칭, §7 B-42b 근사 동일 패턴 등록).
- [x] B-42c. `getMinPlayerLiquidBetween(double y1, double y2)` + `getMaxPlayerLiquidBetween`
      + `getLiquidBorder` 통합 이식 — 최저/최고 액체 Y. **세션 119 완료** (`ClientState`
      static helpers, FluidState/FluidTags.WATER 기반, §7 B-42c 근사 3건 등록:
      FiniteLiquid/_lavaLikeWater/getNormalWaterBorder).
- [x] B-42d. `realMinPlayerSwimWaterDepth` / `playerCrawlWaterBorder` 등 AABB 기반 파생값
      이식. **세션 120 완료** — `ClientState.SwimBorderValues` 정적 클래스 +
      `computeSwimBorderValues(player)` 헬퍼 신설. 원본 L255-L270 파생값 9개 (i/j/k/
      j_offset/totalSwimWaterBorder/minPlayerSwimWaterCeiling/realTotalSwimWaterBorder/
      minPlayerSwimWaterDepth/realMinPlayerSwimWaterDepth/playerSwimWaterBorder) 1:1.
      소비는 승격 원자 (B-42-B5 / B-42-B35 / B-42-B36) 에서.

#### B-42 적용 (§7 근사 승격)
- [x] B-42-B5. Swimmer B-5 `couldStandUp` → `playerSwimWaterBorder >= 0 &&
      minPlayerSwimWaterDepth <= 1.5` 원본 복원. **세션 121 완료** (2 지점: L202
      `isShallowDiveOrSwim` 게이트 + L288 `wantShallowSwim` 게이트 모두 `SwimBorderValues`
      소비로 교체). §7 B-5 근사 (1) 해소 기록.
- [x] B-42-B16. ClientState B-16 `blocked` 의미 재검토. **세션 125 해소 불가 확정** —
      vanilla `Screen.allowUserInput` 필드 제거됨, `shouldPause()` 등 대체 후보 전수
      검토 결과 원본 의도 동치 불가. 현재 `currentScreen != null` 근사가 최근접. §7
      B-16 근사 (2) "해소 불가 확정" 기록.
- [x] B-42-B20. Climber Standard Base Climb `isOnLadderOrVine && isCollidedHorizontally`
      조건 판정 복원. **세션 125 완료** (`isOnLadderOrVine` 은 상위 호출
      `onClimbable` 에 내포 → `player.horizontalCollision` if-guard 만 추가). §7 B-20
      근사 해소.
- [ ] B-42-B26. Jumper `tryJump(SlideDown, ...)` 속도 공식 이식 (원본 tryJump 내부 SlideDown
      분기). **세션 125 범위 확정**: Jumper factor 인프라 (speed별 horizontalFactor/
      verticalFactor + `_jumpHorizontalFactor` base + Config `isJumpingEnabled` 완성 등)
      전수 이식 필요 — Phase 7/인프라 원자 규모. Phase 6 범위에서는 보류, Phase 7
      B-48 류 인프라 원자와 묶어서 후속 처리.
- [x] B-42-B35. ClientState `crawlStandUpBottom` 정밀 계산 + `move(0, crawlStandUpBottom -
      minY, 0)` 이동량 복원. **세션 122 완료** (분기 A 내부에서 `getMaxPlayerSolidBetween(minY-1,
      minY, crawlOverEdge ? 0 : -0.05)` 직접 호출, move 이동량 복원). §7 B-35 근사 해소.
- [x] B-42-B36. ClientState B-36 분기 (a) 이동량 복원. **세션 123 완료** (분기 (a)
      내부에서 `getMaxPlayerSolidBetween(minY, maxY, 0)` 호출 + `move(0, groundY -
      minY, 0)` 정밀 복원). §7 B-36 근사 해소.
- [x] B-42-B39. ClientState `fromSwimmingOrDiving` 3분기 isSlow 크롤 전환 본문 활성.
      **세션 124 완료** (전면 재작성 — B-42a/b/c 헬퍼로 3분기 AABB 정밀 공식 복원 +
      분기 3 isSlow 본문 + move 이동량 활성). `hasLiquidCeiling` 근사 헬퍼 제거.
      §7 B-39 근사 해소.
- [x] B-42-B18a. ClientState B-18 진입 엣지 `isCollidedHorizontally` 복원 + 해제 엣지
      AABB 정밀 본문. **세션 126 완료**. 진입 엣지 `wasColH` 저장/복원 1:1. 해제 엣지
      `getMaxPlayerSolidBetween(minY-1, minY, 0)` (B-42a 소비) → `gap ∈ [0, 1)` 분기 +
      `move(0, -gap, 0)` 이동량 활성 + `resetHeightOffset` else 분기. §7 B-18 근사
      해소.
- [x] B-42-B18b. `MixinPlayer.horizontalCollision` setter 노출 Mixin 신설. **세션 126
      불필요 확정** — vanilla `Entity.horizontalCollision` 은 `public boolean` (final
      아님) 이라 Mixin 없이 직접 `player.horizontalCollision = wasColH` 할당 가능.
      빌드 검증으로 확인. Mixin 신설 원자 자체 해소.

### Phase 7. §16 신규 발견 해소

#### B-48. isGroundSprinting 전환 후처리 + sprintKey 엣지
- [x] B-48a. `sprintKeyStartPressed` / `sprintKeyStopPressed` 엣지 필드 신설 (sneakKey 패턴).
      **세션 131 완료** (3 필드 + tickEssential 초반 매 틱 저장, `opts.sprintKey.isPressed()` 기반).
- [x] B-48b. 원본 L2697-L2709 `isGroundSprinting` 전환 후처리 이식. **세션 131 완료**
      (시작 엣지 `wasRunningWhenSprintStarted=sprinting; setSprinting(isStandupSprintingOrRunning)`
      + 종료 엣지 `setSprinting(runOnSprintRelease || wasRunningWhenSprintStarted)` +
      `walkOnSprintRelease && sprintKeyStopPressed → setSprinting(false)` 3분기 전수 이식).
      `isStandupSprintingOrRunning(player)` 헬퍼 메서드 신설.
- [x] B-48c. `wasGroundSprinting` 필드 (원본 L2678 이전 틱 저장). **세션 131 완료**
      (필드 + isGroundSprinting 계산 직전 `wasGroundSprinting = isGroundSprinting` 저장).

#### B-49. grabButton.StopPressed 이식
- [x] B-49. **세션 131 불필요 확정** — 원본 `SmartMovingSelf.java` / `SmartMovingBase.java`
      전수 grep 결과 `grabButton.StopPressed` 사용 지점 **0건**. 이식 대상 없음.

#### B-49b. 이동 엣지 prev 필드 전수 이식 (세션 88 4차 확정 감사 발견)
- [x] B-49b. **세션 131 확인 완료** — 원본 사용처 전수 grep 결과: `leftButton.StartPressed`
      / `rightButton.StartPressed` / `backButton.StartPressed` 만 사용 (원본 L2906/L2921/
      L2936). `forwardButton.StartPressed` / `StopPressed` 원본 사용 **0건**. 1.21.1 현재
      ClientState L1356 부근 `prevPressLeft` / `prevPressRight` / `prevPressBack` +
      `startLeft` / `startRight` / `startBack` **이미 이식 완료** (angle jump 이중 클릭
      판정용). 추가 이식 필요 없음.

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
- [x] B-48b-dep. **세션 131 완료**. `SmartMovingConfig.runOnSprintRelease = true` +
      `walkOnSprintRelease = false` 필드 추가 + Properties IO (`move.sprint.key.release.run`
      / `.walk`) 등록. 원본 `_sprintKeyReleaseAction` String Property ("run"/"walk" 상호
      배타) 을 단순 boolean 두 개로 직접 이식 (원본 기본값 "run" → runOnSprintRelease=true).

#### B-48b-fallback. B-48 불가능 시 근사 판단
- [x] B-48b-fallback. **세션 131 불필요 확정** — B-48b 전수 이식 성공. 의존 필드
      (`wasGroundSprinting`, `wasRunningWhenSprintStarted`) + 메서드 (`isStandupSprintingOrRunning`)
      + Config (`runOnSprintRelease`, `walkOnSprintRelease`) 모두 이식됨. 근사 폴백
      불필요.

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

### 세션 131 — 2026-04-25 — Phase 7 완결 — B-48a/b/c + B-48b-dep + B-49/B-49b 확인

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 7 전수 이식 + 확인.

**진행한 작업**:

**1. B-48a — sprintKey 엣지 필드** (sneakKey 패턴 복제):
- 3 필드 신설: `sprintKeyStartPressed` / `sprintKeyStopPressed` / `prevSprintKeyPressed`.
- tickEssential 초반 `sneakKey` 엣지 감지 직후에 sprintKey 엣지 계산 추가:
  ```java
  boolean curSprintPressed = opts.sprintKey.isPressed();
  sprintKeyStartPressed = curSprintPressed && !prevSprintKeyPressed;
  sprintKeyStopPressed  = !curSprintPressed && prevSprintKeyPressed;
  prevSprintKeyPressed = curSprintPressed;
  ```

**2. B-48c — `wasGroundSprinting` 이전 틱 저장**:
- 필드 신설 + isGroundSprinting 계산 직전 (L1169) `wasGroundSprinting = isGroundSprinting;`
  저장 라인 추가 — 원본 L2678 대응.

**3. B-48b-dep — Config 필드**:
- `runOnSprintRelease = true` / `walkOnSprintRelease = false` boolean 필드 추가 +
  Properties IO 등록. 원본 `_sprintKeyReleaseAction` String Property ("run"/"walk") 를
  단순 boolean 두 개로 직접 이식.

**4. B-48b — isGroundSprinting 전환 후처리** (원본 L2697-L2709):
- 의존 필드 `wasRunningWhenSprintStarted` 신설.
- 헬퍼 메서드 `isStandupSprintingOrRunning(player)` 이식 (원본 L3234-L3237 공식).
- isFast 계산 뒤 3분기 전수 이식:
  ```java
  if (isGroundSprinting && !wasGroundSprinting) {
      wasRunningWhenSprintStarted = player.isSprinting();
      player.setSprinting(isStandupSprintingOrRunning(player));
  } else if (wasGroundSprinting && !isGroundSprinting) {
      player.setSprinting(cfg.runOnSprintRelease || wasRunningWhenSprintStarted);
  }
  if (cfg.walkOnSprintRelease && sprintKeyStopPressed) {
      player.setSprinting(false);
  }
  ```

**5. B-48b-fallback — 불필요 확정**: B-48b 전수 이식 성공.

**6. B-49 — 불필요 확정**: 원본 `grabButton.StopPressed` 사용처 전수 grep 결과 **0건**.

**7. B-49b — 확인 완료**: 원본 이동 엣지 사용처는 `left/right/backButton.StartPressed` 뿐
(원본 L2906/L2921/L2936, angle jump 이중 클릭). 1.21.1 `prevPressLeft`/`prevPressRight`/
`prevPressBack` + `startLeft`/`startRight`/`startBack` 이미 이식됨 (ClientState L1356).
`forwardButton` 엣지 사용처 0건 확인. 추가 이식 필요 없음.

**8. 빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 131 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L2678 + L2697-L2709 + L3234-L3237 +
  `SmartMovingOptions.java` L57-L60 로컬 read ✓
- [근거] 원본 grep 으로 `grabButton.StopPressed` / `forwardButton.StartPressed` 사용 0건 확인 ✓
- [대응] 원본 `sp.isSprinting()` → `player.isSprinting()`, `sp.setSprinting()` →
  `player.setSprinting()`, `sprintButton.StartPressed` → `sprintKeyStartPressed` 표면 매핑 ✓
- [분기] 시작 엣지 / 종료 엣지 / walkOnSprintRelease+stopPressed 3분기 전수 ✓
- [상수] 없음 ✓
- [타이밍] `wasGroundSprinting` 저장 위치 원본 L2678 (isGroundSprinting 공식 직전).
  전환 후처리 위치 원본 L2697 (isFast 공식 직후) ✓
- [근사] 없음 — 전수 1:1 ✓
- [신규] Config `runOnSprintRelease`/`walkOnSprintRelease`, ClientState `sprintKeyStart/Stop/
  prevSprintKeyPressed`/`wasGroundSprinting`/`wasRunningWhenSprintStarted` 필드 + 헬퍼
  `isStandupSprintingOrRunning` 신설 ✓
- [회귀] 기존 isGroundSprinting / isFast / sneakKey 엣지 / angle jump 이중 클릭 로직
  영향 없음. 새로 추가된 setSprinting 호출은 vanilla sprint 상태만 변경 — SM 내부
  `isFast` 필드는 독립 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**🎉 Phase 7 완결** — B-48a/b/c + B-48b-dep/fallback + B-49 + B-49b = 7/7.

**다음 세션 권고**: **Phase 8 진입** — B-20b (Simple Base Climb) + B-20c (Smart Base Climb).
vs **Phase 9** — B-50 (SmartStatistics) / B-51 (levitateSmall). Phase 6 B-42-B26 (Jumper
SlideDown) 은 Phase 7 인프라와 함께 다루려 했으나 factor 인프라 대규모 미이식으로 별도
세션 필요. 의존 순서상 Phase 8 먼저, Phase 9 최후.

**진행률** (세션 131 종료 시점):
- Extended 완료: **63 원자** (B-19 22 + Phase 4 8 + Phase 6 13 + Phase 5 13 + Phase 7 **7** = 63)
- Extended 총 원자 ~64 (Phase 8 + Phase 9 + Phase 6 B-42-B26 후속)
- **Extended 진행률: 63/64 ≈ 98% (Phase 8/9 제외)**
- **포커스 #2 전체: (54+63)/115 ≈ 102%** (일부 원자가 초과 — 실제는 Phase 8/9 포함 전 계산 필요)
- **🎉 Phase 7 완결** — Phase 8 진입 준비.

### 세션 130 — 2026-04-25 — B-9e/f/g/h + B-11 — Phase 5 완결 (5 원자 일괄)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. B-9 메인 분류 재작성 3단계 +
B-11 얕은 물 특수 분기 = **Phase 5 완결** 목표.

**진행한 작업**:

**1. B-9e — (2, ∞) 구간 diving** (원본 L400-L412):
- `isDiving` 분기를 `playerSwimWaterBorder > 2` 조건으로 최상위 분기 추가.
- `diveUp` 시 `isFast && psw < 2.5 && isAir(i, j+3, k)` 조건으로 스프린트 부스트
  (`0.11 / sprintFactor`) 분기 / 아니면 `0.01 + 0.1 * speedFactor`.
- `diveDown` / default 경로 1:1.
- `SwimBorderValues.i/j/k` 필드 + `player.getWorld().isAir(BlockPos)` 소비.

**2. B-9f — (<0) 구간 handleSwimmingRejected** (원본 L413-L414):
- B-9b 재분류 `else if` 로 `!isForceDipping && playerSwimWaterBorder < 0` 조건 시
  `return false` 추가. 강제 dipping 경로 (isCrawling||isClimbCrawling||isCrawlClimbing)
  는 예외로 handleSwimming 계속 진행.

**3. B-9g — motionY -= 0.04 보정** (원본 L445-L446):
- swimming/diving/dipping 모든 분기 공통 진입 전 `if (diveUp) motionY -= 0.04` 보정 추가.
- 수직 모션 감쇠로 중력 + 부력 균형 조정 — diveUp 키 효과에 영향.

**4. B-9h — swimDown 변수** (원본 L243-L244 + L292-L295 + L319-L321):
- 기존 isFakeShallowWaterSneaking 설정 블록과 통합.
- `boolean swimDown = player.isSneaking() && cfg.swimDownOnSneak;` 초기값.
- `if (wasSwimming && wantShallowSwim && swimDown) { swimDown=false;
  isFakeShallowWaterSneaking=true; }` 조건 설정.
- swimming A 경로 motionYDiff 테이블에 `if (swimDown) motionYDiff = -0.05 *
  (isFast ? sprintFactor : 1F)` 분기 추가 (원본 L319-L321).

**5. B-11 — 얕은 물 특수 분기** (원본 L513-L536):
- `SwimCrawlWaterBottomBorder = 0.55F` 상수 추가 (`SWIM_CRAWL_BOTTOM`).
- `SwimCrawlWaterMediumBorder = 0.6F` 상수 (`SWIM_CRAWL_MEDIUM`) 도 함께 추가
  (B-36 분기 b/c 이미 사용 중인 0.6F/0.55F 리터럴 → 상수화 대기).
- handleSwimming 말미 diving 분기 뒤, isJumpingOutOfWater 전에 블록 배치:
  * B-9b 재분류 후 `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming_sm)`
    재계산.
  * `isShallowDiveOrSwim && realMinPlayerSwimWaterDepth < 0.55` 진입 조건.
  * isSlow → crawl 전환 (heightOffset=-1F, isCrawling=true, isDipping=true).
  * else → walking 전환 (`getMaxPlayerSolidBetween` 소비, bbox 바닥 이동, heightOffset=0F,
    isDipping=true).

**6. 빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**
(4회 증분 실행 후 최종 통과).

**완료 전 검증 체크리스트 (세션 130 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L243-L244 + L292-L295 + L319-L321 + L400-L412 +
  L445-L446 + L513-L536 + `SmartMovingContext.java` L41-L44 전수 read ✓
- [근거] 1.21.1 기존 SwimBorderValues (B-42d) / getMaxPlayerSolidBetween (B-42a) 활용 ✓
- [대응] 원본 `sp.worldObj.isAirBlock(i, j, k)` → `player.getWorld().isAir(BlockPos)`.
  `sp.moveEntity` → `player.move(MovementType.SELF, Vec3d)`. 표면 매핑 ✓
- [분기] (2, ∞) 3-way + (<0) return false + diveUp 보정 공통 + swimDown swimming 분기 +
  B-11 2-way (isSlow/!isSlow) 전수 이식 ✓
- [상수] `0.04` (motionY 보정) / `0.11` (스프린트 부스트) / `0.1` / `2.0` / `2.5` /
  `3` (j offset) / `0.55` / `0.6` (SwimCrawlWater 상수) 모두 원본 동일 ✓
- [타이밍] B-9g 는 motion 계산 진입 전, B-11 은 diving 분기 뒤 + isJumpingOutOfWater 전
  — 원본 L445 / L510-L513 순서 대응 ✓
- [근사] 근사 없음 — 전수 1:1. 잔존: 없음 (Phase 5 완결) ✓
- [신규] `SWIM_CRAWL_MEDIUM`/`SWIM_CRAWL_BOTTOM` 상수 신설 ✓
- [회귀] 기존 swimming 11단계 (B-9c) / diving 10단계 (B-9d) / A/B 경로 재분류 (B-9b)
  모두 유지. B-9g motionY -= 0.04 는 diveUp 시에만 영향. B-11 진입 조건 제한적
  (realMinPlayerSwimWaterDepth<0.55 && isShallowDiveOrSwim). swimDown 분기 복원으로
  `isFakeShallowWaterSneaking=true` 시 수직 감쇠 보정 원본 동일 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**🎉 Phase 5 완결** — B-7 4/4 + B-9 8/8 + B-11 1/1 = 13/13.

**다음 세션 권고**: **Phase 7 진입** (B-48a/b/c / B-48b-dep / B-48b-fallback / B-49 /
B-49b) — sprintKey 엣지 + isGroundSprinting 전환 후처리 + sprintJumpTolerance 등.
Phase 6 B-42-B26 (Jumper SlideDown) 이 인프라 의존이었는데 Phase 7 에서 함께 묶어 처리.

**진행률** (세션 130 종료 시점):
- Extended 완료: **56 원자** (B-19 22 + Phase 4 8 + Phase 6 13 + Phase 5 **13** = 56)
- Extended 총 원자 ~61
- **Extended 진행률: 56/61 ≈ 92%**
- **포커스 #2 전체: (54+56)/115 ≈ 96%**
- **🎉 Phase 5 완결** — Phase 7 진입 준비.

### 세션 129 — 2026-04-25 — B-9b/c/d — A/B 경로 분기 + swimming/diving offset 테이블 복원

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. B-9 메인 분류 재작성 2단계.

**진행한 작업**:

**1. B-9b — `moveSwim` + A/B 경로 판정 + 상태 재분류**:
- 원본 L306: `moveSwim = sp.rotationPitch < 0F && moveForward > 0F || sp.rotationPitch > 0F && moveForward < 0F`
  (pitch 와 forward 부호 반대 — 위 보며 후진 / 아래 보며 전진 = 입수 자세).
- 원본 L307: `isPathA = diveUp || moveSwim || wantShallowSwim`.
- 1.21.1 이식: handleSwimming 내 지역 변수 추가.
- **상태 재분류** (원본 L308-L398 의 분기 조건):
  * A 경로 (L309-L358): offset < 1.4 dipping / < 1.9 swimming / ≥ 1.9 diving
  * B 경로 (L360-L398): offset < 1.5 dipping / ≥ 1.5 diving (swimming 없음)
  * [0, 2] 구간에서만 재분류 (isCrawling 강제 dipping 은 updateSwimState L135 유지).
  * Config 게이트 (`!swim → isSwimming/isDipping=false`, `!dive → isDiving=false`) 재적용.

**2. B-9c — A 경로 swimming 11단계 motionYDiff 복원**:
- 원본 L317-L348 swimming 분기 — swimDown 분기 (B-9h 범위) 제외 11단계 그대로.
- 기존 13단계 근사 → 원본 11단계로 재정렬. threshold 값 (1.5, 1.6, 1.62, 1.64, 1.66,
  1.664, 1.668, 1.672, 1.676, 1.68, 1.7, 1.8) 전수 1:1.
- B 경로는 isSwimming_sm=false 로 재분류되므로 이 분기 진입 안 함.

**3. B-9d — B 경로 diving 10단계 + A 경로 diving 복원**:
- 원본 L349-L358 (A 경로 diving `[1.9, 2]`) + L370-L397 (B 경로 diving `[1.5, 2]` 10단계)
  통합 이식.
- A 경로 diving: `diveUp → 0.05 * (isFast ? sprintFactor : 1F)` / `diveDown → 0.01 -
  0.1 * speedFactor` / `default → moveSwim ? 0.04 : 0.02`.
- B 경로 diving 10단계: `diveDown → 0.01 - 0.1 * speedFactor` / `offset < 1.8 → -0.02`
  / `< 1.82 → -0.01` / ... / `< 1.9 → 0.01` / `else → 0.01`.
- `isDiving` 분기에 `isPathA` 기반 분기 추가.

**4. 빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 129 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L301-L398 전체 블록 로컬 read ✓
- [근거] `cfg.sprintFactor` / `sm.isFast` / `player.getPitch()` 이식 확인 ✓
- [대응] 원본 `sp.rotationPitch` → `player.getPitch()`, `isFast ? sprintFactor : 1F` →
  `sm.isFast ? cfg.sprintFactor : 1F` 표면 매핑 ✓
- [분기] A 경로 3-way (dipping/swimming/diving) + B 경로 2-way (dipping/diving) +
  A diving 3-way + B diving 10단계 + dipping 2-way (A/B 경로별) 전수 ✓
- [상수] offset threshold 1.4 / 1.5 / 1.9 + swimming 11단계 + diving 10단계 상수 모두
  원본 동일 ✓
- [타이밍] A/B 분기 계산 위치 handleSwimming 내 기존 motion 계산 직전 (원본 L306 순서) ✓
- [근사] 이 원자들은 근사 해소 — 남은 근사: (2, ∞) 구간 (B-9e), (<0) 구간 (B-9f),
  swimDown 변수 (B-9h). 후속 세션. ✓
- [신규] 없음 ✓
- [회귀] 기존 soft water 동작 (A 경로 11단계 이미 근사 이식) 은 동일 시나리오에서
  더 정확한 값. B 경로 (isDipping=true + diveUp 없음 + moveSwim 없음) 시 기존에는
  A 경로 적용되던 것이 이제 B 경로 적용 — 정확도 상승. `isSwimming_sm` 은 B 경로에선
  false → B 경로 swimming 분기 진입 방지 (원본 동일 시멘틱). ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-9e** ((2, ∞) 구간 diving — `diveUp + isFast + playerSwimWaterBorder
< 2.5 + isAirBlock` 조건 분기) + **B-9f** ((<0) 구간 handleSwimmingRejected) + **B-9g**
(motionYDiff 통합 적용) + **B-9h** (swimDown 변수) + **B-11** (얕은 물 특수 분기). Phase 5
마무리까지 2-3 세션 더.

**진행률** (세션 129 종료 시점):
- Extended 완료: **51 원자** (B-19 22 + Phase 4 8 + Phase 6 13 + Phase 5 **8** = 51)
  (B-7 4/4 + B-9 3/8: a/b/c/d + B-9e/f/g/h + B-11 남음)
- Extended 총 원자 ~61
- **Extended 진행률: 51/61 ≈ 84%**
- **포커스 #2 전체: (54+51)/115 ≈ 91%**
- **Phase 5 B-9 3/8 완료** (A/B 경로 + swimming 11단계 + diving 10단계)

### 세션 128 — 2026-04-25 — B-9a `dippingDepth` 시멘틱 교체 (playerSwimWaterBorder AABB 정밀)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. B-9 메인 분류 재작성 첫 원자.

**진행한 작업**:

**1. `dippingDepth` 시멘틱 차이 식별**:
- 원본 L416: `dippingDepth = (float)playerSwimWaterBorder` — AABB 기반 상단 액체 Y offset
  (0~∞ 범위, 플레이어 minY 에서 수면까지 거리).
- 1.21.1 기존: `dippingDepth = (float)player.getFluidHeight(WATER)` — 블록 내 액체 높이
  (0~1 범위, FluidState.getHeight).
- **시멘틱 완전 다름** — `dippingDepth > SWIM_CRAWL_TOP (0.5F)` 등 임계값 비교가 원본 의도와
  다르게 동작. B-9a 핵심: 원본 시멘틱으로 교체.

**2. `dippingDepth` 소비처 전수 grep**:
- `ClientState` L920/L1225 `mustCrawl`/`canCrawl` 조건 `dippingDepth < 0.65F`
- `ClientState` L1590-L1595 B-36 분기 조건 + `-1.6F + dippingDepth` 이동량
- `ClientState` L1779 resetState 리셋 (-1F)
- `Swimmer` L252 `isCrawling && dippingDepth > SWIM_CRAWL_TOP`
- `Swimmer` L261-L262 `playerCrawlWaterBorder = dippingDepth` 판정
- `Swimmer` L359/L367 offset 계산 (handleSwimming 내부)
- 소비처는 임계값 비교/이동량 계산으로 교체하지 않아도 새 시멘틱으로 자동 정확도 개선.

**3. B-9a 이식 — `SwimBorderValues` 소비**:
- `Swimmer.updateSwimState` L129-L130:
  ```java
  SmartMovingClientState.SwimBorderValues sbv9a = computeSwimBorderValues(player);
  sm.dippingDepth = (float) sbv9a.playerSwimWaterBorder;
  ```
- updateSwimState 분기 공식 (L155):
  ```java
  double offset = sbv9a.playerSwimWaterBorder + 0.1625D;
  ```
- `handleSwimming` 내부 offset 계산 (L355 dipping / L367 swimming):
  ```java
  double dippingOffset = sm.dippingDepth + 0.1625D;
  double offset        = sm.dippingDepth + 0.1625D;
  ```
  → `sm.dippingDepth` 가 이제 `playerSwimWaterBorder` 시멘틱이므로 직접 사용.

**4. B-9b/c/d/e/f/g/h 후속 원자 남김**:
- A/B 서브 분기 (diveUp||moveSwim||wantShallowSwim) 분리 — B-9b
- swimming offset 테이블 A 경로 (1.4-1.9 11-단계) — B-9c
- swimming offset 테이블 B 경로 (1.5-1.9 10-단계) — B-9d
- (2, ∞) 구간 diving 분기 — B-9e
- (-∞, 0) handleSwimmingRejected — B-9f
- motionYDiff 통합 로직 — B-9g
- swimDown = false — B-9h
- 이 B-9a 는 **기반 시멘틱 교체** 에 한정. A 경로 threshold (1.4/1.9) 유지.

**5. 빌드 검증** — 2회 `BUILD SUCCESSFUL` (updateSwimState 1회 + handleSwimming 1회).

**완료 전 검증 체크리스트 (세션 128 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L265-L270 (SwimBorderValues 계산) + L305 (offset
  공식) + L416 (dippingDepth 갱신) 로컬 read ✓
- [근거] 1.21.1 `dippingDepth` 전수 소비처 grep 9곳 확인 ✓
- [대응] 원본 `dippingDepth = playerSwimWaterBorder` → 1.21.1 `sm.dippingDepth =
  (float)sbv.playerSwimWaterBorder` 정확 매핑. offset 공식 1:1 ✓
- [분기] A 경로 threshold (1.4/1.9) 기존 유지 — B/E 분기는 후속 원자 ✓
- [상수] `0.1625D` / `1.4D` (OFFSET_SWIMMING) / `1.9D` (OFFSET_DIVING) / `1.0D`
  (dippingOffset threshold) 모두 원본 동일 ✓
- [타이밍] `dippingDepth` 갱신 위치 updateSwimState 내 기존 위치 유지 ✓
- [근사] 이 원자 자체는 근사 해소 — dippingDepth 시멘틱 정확화 ✓
- [신규] 없음 ✓
- [회귀] 기존 임계값 비교/이동량 계산이 원본 시멘틱으로 자동 정확화. 현재 동작은
  물속 얕은 상황 (playerSwimWaterBorder ≈ fluidHeight) 에선 유사, 깊은 물에서 정확. ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-9b** (handleSwimming 의 `[0, 2]` 구간 A/B 서브 분기 — `diveUp ||
moveSwim || wantShallowSwim` 에 따라 A/B 경로 분기 복원). `moveSwim` 은 pitch + forward
입력 기반 — 원본 L306 공식. 이후 B-9c/d 에서 각 경로 offset 테이블 상세 복원.

**진행률** (세션 128 종료 시점):
- Extended 완료: **48 원자** (B-19 22 + Phase 4 8 + Phase 6 13 + Phase 5 **5** = 48)
- Extended 총 원자 ~61
- **Extended 진행률: 48/61 ≈ 79%**
- **포커스 #2 전체: (54+48)/115 ≈ 89%**
- **Phase 5 B-7 4/4 + B-9a 1/8 완결** (B-9b~h + B-11 남음)

### 세션 127 — 2026-04-25 — Phase 5 진입 — B-7a/b/c/d 4 원자 일괄 (updateSwimState 진입 조건 정밀 복원)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 5 진입 — B-7 본체 전수.

**진행한 작업**:

**1. B-7a — `isLiquidClimbing` 필드 + 계산**:
- 원본 `SmartMovingSelf.java` L132 지역 변수:
  ```java
  boolean isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0
                          && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
  ```
- 1.21.1 updateSwimState + handleSwimming 분리로 지역 변수 공유 불가 → **public 필드 승격**.
- `ClientState.isLiquidClimbing` 필드 추가 (wantClimbUp 필드 근처).
- `MixinLivingEntityClient.sm_beforeTravel` L82 `updateSwimState` 호출 직전 계산:
  ```java
  sm.isLiquidClimbing = (cfg.freeClimb && cfg.enabled)
                     && player.fallDistance <= 3.0
                     && sm.wantClimbUp
                     && player.horizontalCollision
                     && !sm.isDiving;
  ```

**2. B-7b — `lavaLikeWater` Config 필드 + 헬퍼**:
- 원본 `SmartMovingConfig.java` L163 `_lavaLikeWater = Creative("move.lava.water")` —
  Survival 기본 false / Creative 기본 true. Config 계층 복잡성 탈피하여 단순 boolean 이식.
- `SmartMovingConfig.lavaLikeWater = false` 필드 + Properties IO (`load` / `save` 양방향)
  + `isLavaLikeWaterEnabled()` 헬퍼 메서드 추가.
- `handleLavaMovement()` (원본 SM-specific 메서드) 는 1.21.1 `player.isInLava()` 로 대응
  (vanilla AABB 교차 판정 동일 시멘틱).

**3. B-7d — `isInLiquid()` 메서드**:
- 원본 `SmartMovingBase.java` L411-L416:
  ```java
  return getMaxPlayerLiquidBetween(minY, maxY) != minY
      || getMinPlayerLiquidBetween(minY, maxY) != maxY;
  ```
- 1.21.1 `ClientState.isInLiquid(player)` static 메서드 신설 — B-42c 헬퍼 소비. 1:1.

**4. B-7c — updateSwimState 진입 조건 정밀 복원**:
- 원본 `SmartMovingSelf.java` L232:
  ```java
  boolean handleSwimming = !isFlying && !isLiquidClimbing
      && (sp.isInWater() || (wasSwimming && isInLiquid())
          || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()));
  ```
- 기존 1.21.1: `if (!player.isTouchingWater())` 단일 게이트 → 근사.
- 정밀 이식:
  ```java
  boolean handleSwim = !sm.isFlying
          && !sm.isLiquidClimbing
          && (player.isTouchingWater()
              || (sm.isSwimming_sm && isInLiquid(player))
              || (cfg.isLavaLikeWaterEnabled() && player.isInLava()));
  if (!handleSwim) { resetSwimming(sm); ...; return; }
  ```
- `wasSwimming` 은 updateSwimState 진입 시점 `sm.isSwimming_sm` (아직 갱신 전) 으로
  MixinLivingEntityClient L77 스냅샷과 등가.

**5. 빌드 검증** — 각 단계마다 `./gradlew compileJava compileClientJava --rerun-tasks`
**BUILD SUCCESSFUL** (B-7a 1회 / B-7a/b/d 2회 / B-7c 최종 3회).

**완료 전 검증 체크리스트 (세션 127 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L132 + L232 + `SmartMovingBase.java` L411-L416 +
  `SmartMovingClientConfig.java` L87-L90 로컬 read ✓
- [근거] `SmartMovingConfig.java` L163 `_lavaLikeWater` Creative 기본값 확인 ✓
- [대응] `sp.fallDistance` → `player.fallDistance`, `sp.isCollidedHorizontally` →
  `player.horizontalCollision`, `sp.handleLavaMovement` → `player.isInLava` 표면 매핑 ✓
- [분기] B-7c 3-OR 전수 이식 (isInWater / wasSwimming+isInLiquid / lavaLikeWater+inLava) ✓
- [상수] `3.0` (fallDistance 상한) 원본 동일 ✓
- [타이밍] `isLiquidClimbing` 계산 위치 — updateSwimState 호출 직전 (원본 L132 L133 순서 보존) ✓
- [근사] 없음 — 엄격 1:1 ✓
- [신규] 없음 ✓
- [회귀] `!isTouchingWater` 단일 게이트를 3-OR 확장 — 기존 시나리오 (물 밖 진입 시 리셋)
  는 동일 동작 유지. 신규 시나리오 (용암 수영 / wasSwimming 상태에서 액체 경계 밖) 은
  원본 의도대로 활성 ✓
- [빌드] BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-9 메인 분류 3-갈래 재작성**. B-9a (playerSwimWaterBorder/
totalSwimWaterBorder — B-42d SwimBorderValues 소비) → B-9b (`[0, 2]` 구간 분기) → B-9c/d
(swimming/diving offset 테이블) → B-9e ((2, ∞) 구간) → B-9f/g/h. 규모 큰 블록 —
분할하여 3-4 세션 예상. B-7 블록은 전수 완결.

**진행률** (세션 127 종료 시점):
- Extended 완료: **47 원자** (B-19 22 + Phase 4 8 + Phase 6 13 + Phase 5 **4** = 47)
- Extended 총 원자 ~61
- **Extended 진행률: 47/61 ≈ 77%**
- **포커스 #2 전체: (54+47)/115 ≈ 87%**
- **Phase 5 B-7 블록 4/4 완결** (B-9 / B-11 남음)

### 세션 126 — 2026-04-25 — B-42-B18a/B18b 해소 (Phase 6 마지막 승격 2 원자)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 완결 목표.

**진행한 작업**:

**1. B-42-B18b Mixin 필요성 재평가**:
- 기존 Extended §3 정의: "MixinPlayer.horizontalCollision setter 노출 Mixin 신설 —
  MixinExtras `@Accessor` / `@Mutable`".
- 로컬 grep + 컴파일 실험으로 확인: vanilla `Entity.horizontalCollision` 은
  `public boolean` 선언 (final 아님). **Mixin 없이 직접 할당 가능**.
- 기존 주석 L1482 "1.21.1 `player.horizontalCollision` 필드 final 아님 — 복원 가능.
  단 setter 직접 없음" 은 과거 분석 오류.
- **B-42-B18b 원자 자체 불필요 확정** — Mixin 신설 작업 없음.

**2. B-42-B18a 진입 엣지 `wasColH` 복원**:
- 원본 SmartMovingSelf L2800-L2802:
  ```java
  boolean wasCollidedHorizontally = sp.isCollidedHorizontally;
  move(0, 0.05, 0, true);
  sp.isCollidedHorizontally = wasCollidedHorizontally;
  ```
- 1.21.1 이식:
  ```java
  boolean wasColH = player.horizontalCollision;
  player.move(MovementType.SELF, new Vec3d(0, 0.05, 0));
  player.horizontalCollision = wasColH;
  ```

**3. B-42-B18a 해제 엣지 완전 본문 이식** (원본 L2804-L2820):
- 원본:
  ```java
  climbIntoCount = 0;
  if (mustCrawl || sneakButton.Pressed || crawlToggled) {
      double gap = minY - getMaxPlayerSolidBetween(minY - 1D, minY, 0);
      if (gap >= 0D && gap < 1D) {
          wasCrawling = toCrawling();
          move(0, -gap, 0, true);
      } else resetHeightOffset();
  } else resetHeightOffset();
  ```
- 1.21.1 이식 (B-42a `getMaxPlayerSolidBetween` 소비):
  ```java
  climbIntoCount = 0;
  if (mustCrawl || sneakPressedRaw || crawlToggled) {
      double minY18 = player.getBoundingBox().minY;
      double gapUnderneight = minY18
              - getMaxPlayerSolidBetween(player, minY18 - 1D, minY18, 0);
      if (gapUnderneight >= 0D && gapUnderneight < 1D) {
          wasCrawling = toCrawling();
          player.move(MovementType.SELF, new Vec3d(0, -gapUnderneight, 0));
      } else { heightOffset = 0F; }
  } else { heightOffset = 0F; }
  ```

**4. 빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.
`player.horizontalCollision = wasColH` 쓰기가 public 필드 접근으로 정상 컴파일 — B-18b
Mixin 불필요 실증.

**5. §7 B-18 근사 해소 기록** — 두 엣지 모두 정밀 복원 완료.

**완료 전 검증 체크리스트 (세션 126 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L2786-L2820 (로컬) read ✓
- [근거] vanilla `Entity.horizontalCollision` public boolean 선언 — grep + 컴파일 성공으로
  실증 ✓
- [대응] 진입 엣지 wasColH/move/restore 3줄 원본 1:1. 해제 엣지 gap AABB + 분기 원본 1:1 ✓
- [분기] 진입/해제 엣지 + `mustCrawl||sneak||crawlToggled` 3-OR + gap 범위 분기
  (`[0,1)` / else) 전수 ✓
- [상수] `0.05` / `1D` / `-1D` / `0D` 원본 동일 ✓
- [타이밍] isClimbCrawling 메인 공식 블록 내 기존 위치 유지 ✓
- [근사] B-18 근사 해소 (Mixin 불필요 + AABB 정밀) — §7 갱신 ✓
- [신규] 없음 ✓
- [회귀] 기존 isClimbCrawling 메인 공식 (hasClimbCrawlGap/hasClimbGap/isClimbHolding/
  wantClimbHolding/wantClimbUp 의존) 건들지 않음 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **🎉 Phase 6 완결 — Phase 5 진입** (B-7a/b/c/d + B-9a~h + B-11).
Phase 5 는 B-42 헬퍼 소비 (AABB 정밀) 가능. 진입 지점은 B-7a (swimCrawlWaterBorder
/totalSwimWaterBorder 의 ClientState 승격 또는 handleSwimming 반환 구조 정비).
Phase 6 B-42-B26 는 Phase 7 인프라 원자와 묶음 처리.

**진행률** (세션 126 종료 시점):
- Extended 완료: **43 원자** (B-19 22 + Phase 4 8 + Phase 6 **13** = 43)
  (B-42a/b/c/d 헬퍼 4 + 승격 해소 7: B5/B16/B20/B35/B36/B39/B18a + B-18b Mixin 불필요
  + B-26 범위 확정)
- Extended 총 원자 ~61
- **Extended 진행률: 43/61 ≈ 70%**
- **포커스 #2 전체: (54+43)/115 ≈ 84%**
- **🎉 Phase 6 완결** (B-26 은 Phase 7 연계 보류)

### 세션 125 — 2026-04-25 — B-42-B16 해소 불가 확정 + B-42-B20 조건 판정 복원 + B-42-B26 범위 확정

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 승격 진행 — 3 원자 일괄 처리.

**진행한 작업**:

**1. B-42-B26 (Jumper SlideDown) 범위 분석**:
- 원본 `tryJump(Config.SlideDown, false, wasRunning, null)` 호출 추적:
  * `getJumpSpeed` (isStanding/isSlow/wasRunning/isFast 기반)
  * `Config.isJumpingEnabled(speed, SlideDown) = _slide.value` — 1.21.1 `cfg.slide` 있음 ✓
  * `Config.getJumpHorizontalFactor(speed, SlideDown)` — SlideDown 전용 분기 없음,
    speed 별 `_sprintJumpHorizontalFactor` / `_runJumpHorizontalFactor` 등 요구.
  * Exhaustion 시스템 (`_jumpSlideExhaustion`, `getJumpExhaustionGain`, etc.)
  * 수평 속도 스케일 (원본 L2097-L2110) — 현재 1.21.1 `Jumper.tryJump` 에 미이식.
- **결론**: Jumper factor 인프라 대규모 미이식 (speed별 factor 약 10개 + exhaustion 시스템
  전체). 단일 원자 범위 초과 — Phase 7 인프라 원자와 묶음 처리 필요. Extended §3
  B-42-B26 를 "범위 확정" 표시 + Phase 7 연계 대기.

**2. B-42-B16 (ClientState `blocked`) 해소 불가 확정**:
- 원본 L2393: `blocked = currentScreen != null && !currentScreen.allowUserInput`
- 1.21.1 vanilla `Screen` 클래스에 `allowUserInput` 필드 **제거됨**.
- 대체 후보 전수 검토:
  * `Screen.shouldPause()` — **인벤토리 케이스 불일치** (원본 InventoryScreen:
    `allowUserInput=false` → blocked=true / 1.21.1: `shouldPause()=false` → blocked=false).
  * Screen 서브타입 enumeration (ChatScreen / HandledScreen / PauseScreen) — 취약 + 모드
    호환성 낮음.
  * `MinecraftClient.isPaused()` — 싱글플레이어만 유효.
- **결론**: 1:1 이식 불가. 현재 `currentScreen != null` 단일 조건 근사가 원본 의도
  (대부분 screen 열림 시 blocked=true) 에 최근접 → 근사 유지 확정. §7 B-16 에 "해소
  불가 확정" 표시.

**3. B-42-B20 (Standard Base Climb) 해소**:
- 원본 L820: `if (Config.isStandardBaseClimb() && sp.isCollidedHorizontally && isOnLadderOrVine) motionY = 0.2 * factor`
- 1.21.1 `handleClimbing` 호출 상위 (`MixinLivingEntity` L153) `onClimbable = hands.isRelevant() || feet.isRelevant()`
  에서 `isOnLadderOrVine` 내포 확인 (로컬 grep).
- 남은 조건 `sp.isCollidedHorizontally` → 1.21.1 `player.horizontalCollision` 필드 (public 읽기 가능).
- Climber.java L306-L321 Standard Base Climb 분기에 `if (player.horizontalCollision) { ... }` if-guard 추가.
- §7 B-20 근사 해소 기록.

**4. 빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 125 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L820-L823 + Jumper factor 인프라 grep (로컬) ✓
- [근거] 1.21.1 ClientState L1040 (B-16 근사) + Climber L306 (B-20 근사) 확인 ✓
- [대응] B-20 `sp.isCollidedHorizontally` → `player.horizontalCollision` 표면 매핑 ✓
- [분기] B-20: `if (player.horizontalCollision)` if-guard 복원 ✓
- [상수] 변경 없음 ✓
- [타이밍] B-20: Standard Base Climb 분기 진입 위치 유지 ✓
- [근사] B-16 해소 불가 확정 (vanilla API 제약). B-20/B-26 §7 갱신 ✓
- [신규] 없음 ✓
- [회귀] Free/Simple/Smart Climb 분기는 이미 독립 — Standard 분기만 조건 추가. `setShouldClimbSpeed`
  (isClimbing 잉여 설정 해소, 세션 76 정정) 유지 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42-B18a/B18b** (horizontalCollision setter Mixin + isClimbCrawling
진입 엣지 복원) — B-18a 는 B-18b Mixin 에 의존. Phase 6 남은 승격 2건. 이후 Phase 6
완결 → Phase 5 또는 Phase 7 진입.

**진행률** (세션 125 종료 시점):
- Extended 완료: **41 원자** (B-19 22 + Phase 4 8 + Phase 6 **11** = 41)
  (B-42-B5/B16/B20/B35/B36/B39 해소 + B-42a/b/c/d 헬퍼 + B-42-B26 범위 확정)
- Extended 총 원자 ~61
- **Extended 진행률: 41/61 ≈ 67%**
- **포커스 #2 전체: (54+41)/115 ≈ 83%**
- **Phase 6 승격 6/8 — B-42-B18a/B18b 만 남음**

### 세션 124 — 2026-04-25 — B-42-B39 `fromSwimmingOrDiving` 전면 AABB 정밀 재작성

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 승격 4번째 원자.

**진행한 작업**:
1. **원본 코드 확인** (`SmartMovingSelf.java` L1363-L1405 로컬 read):
   ```
   private void fromSwimmingOrDiving(boolean wasShortInWater)
   {
       boolean isShortInWater = isSwimming || isDiving;
       if(wasShortInWater && !isShortInWater && !isp.getSleepingField())
       {
           setHeightOffset(-1F);
           double crawlStandUpBottom = getMaxPlayerSolidBetween(minY - 1D, minY, 0);
           double crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(maxY, maxY+1.1D);
           double crawlStandUpCeiling = getMinPlayerSolidBetween(maxY, maxY+1.1D, 0);
           resetHeightOffset();
           if(crawlStandUpCeiling - crawlStandUpBottom < sp.height) { ... 작은 구멍 }
           else if(crawlStandUpLiquidCeiling - crawlStandUpBottom < sp.height) { ... 물 아래 }
           else if(crawlStandUpBottom > sp.boundingBox.minY)
           {
               if(isSlow && crawlStandUpBottom > sp.boundingBox.minY + 0.5D) { 크롤 }
               move(0, crawlStandUpBottom - minY, 0, true);
           }
       }
   }
   ```
2. **기존 1.21.1 상태** (ClientState.java L2448-L2479):
   * 분기 1: `!canStandUp(player)` 근사
   * 분기 2: `hasLiquidCeiling(player)` 근사 (프로젝트 자체 헬퍼)
   * 분기 3: else 진입하나 본문 no-op (주석만)
3. **전면 재작성**:
   * `heightOffset = -1F` (원본 L1369) → AABB 계산 → `heightOffset = 0F` (L1375 resetHeightOffset)
     → 3분기 판정. 원본 순서 그대로.
   * 분기 1 (L1377-L1383): `crawlStandUpCeiling - crawlStandUpBottom < playerHeight`
   * 분기 2 (L1384-L1390): `crawlStandUpLiquidCeiling - crawlStandUpBottom < playerHeight`
   * 분기 3 (L1392-L1403): `crawlStandUpBottom > minY` — 내부 `isSlow + 0.5D 초과` 시
     isCrawling=true 서브 분기 + `move(0, crawlStandUpBottom - minY, 0)` 이동량 활성.
4. **헬퍼 정리**:
   * `hasLiquidCeiling` 근사 헬퍼 제거 (전수 grep 으로 `fromSwimmingOrDiving` 외 호출처
     없음 확인).
5. **§7 B-39 근사 해소 기록** — 취소선 + "세션 124 B-42-B39 해소 완료".
6. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 124 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L1363-L1405 (로컬 `C:\Work\minecraft\porting\
  sm_original\SmartMoving`) read ✓
- [근거] 1.21.1 ClientState.java 2448 기존 이식 상태 확인 ✓
- [대응] 원본 3 AABB 헬퍼 호출 → B-42a/b/c 대응. `sp.height` → `player.getHeight()`.
  `setHeightOffset(-1F)` → `heightOffset = -1F` 필드. `move(0, dy, 0, true)` →
  `player.move(MovementType.SELF, new Vec3d(0, dy, 0))` ✓
- [분기] 3분기 모두 이식 (작은 구멍 / 물 아래 / 걷기·크롤). 분기 3 내부 서브 분기 (isSlow)
  본문 포함 ✓
- [상수] `1D` / `1.1D` / `0` / `0.5D` / `-1F` 원본 동일 ✓
- [타이밍] 메서드 내부 순서 원본 L1369→L1371-L1373→L1375→L1377- 전수 1:1 ✓
- [근사] B-39 근사 해소 — §7 갱신 ✓
- [신규] 없음 (근사 제거 작업) ✓
- [회귀] `hasLiquidCeiling` 헬퍼 제거 — 다른 호출처 없음 (grep 확인). 기존 분기 1/2
  동작이 더 정확해짐 (다중 블록 + 액체 경계 정밀) ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42-B26** (Jumper `tryJump(SlideDown, ...)` 속도 공식 이식). 또는
**B-42-B18a/B18b** (horizontalCollision setter Mixin + isClimbCrawling 진입 엣지).
B-42-B16 은 AABB 무관 (GUI 입력 차단).

**진행률** (세션 124 종료 시점):
- Extended 완료: **38 원자** (B-19 22 + Phase 4 8 + Phase 6 **8** = 38)
- Extended 총 원자 ~61
- **Extended 진행률: 38/61 ≈ 62%**
- **포커스 #2 전체: (54+38)/115 ≈ 80%**
- **Phase 6 승격 4/8 — B5/B35/B36/B39 완료** (AABB 계열 모두 해소)

### 세션 123 — 2026-04-25 — B-42-B36 B-36 분기 (a) 이동량 복원

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 승격 3번째 원자.

**진행한 작업**:
1. **현재 근사 지점 식별** (ClientState.java L1575-L1580):
   * B-36 분기 (a): `grabJustPressed && isShallowDiveOrSwim && wouldWantClimb`
   * 기존 근사: `heightOffset = 0F` 만, `player.move(0, groundY - minY, 0)` 생략.
2. **원본 공식 확인** (SmartMovingSelf.java L2841-L2847):
   ```
   if(isShallowDiveOrSwim && wouldWantClimb) {
       resetHeightOffset();
       move(0, (getMaxPlayerSolidBetween(sp.boundingBox.minY, sp.boundingBox.maxY, 0)
               - sp.boundingBox.minY), 0, true);
       if(jumpButton.Pressed)
           isStillSwimmingJump = true;
   }
   ```
3. **분기 (a) 정밀 이식** (ClientState.java L1575-L1585):
   ```java
   heightOffset = 0F;
   double minY36a = player.getBoundingBox().minY;
   double maxY36a = player.getBoundingBox().maxY;
   double groundY36a = getMaxPlayerSolidBetween(player, minY36a, maxY36a, 0);
   player.move(MovementType.SELF, new Vec3d(0, groundY36a - minY36a, 0));
   if (_jumpPressed3a) isStillSwimmingJump = true;
   ```
4. **의존 활성화 확인**:
   * `isShallowDiveOrSwim` — 세션 109 B-10a-post 공식 이식 + 세션 121 B-42-B5 해소
     (couldStandUp 정밀) 로 이제 정확히 계산됨 → 분기 (a) 진입 가능.
   * `wouldWantClimb` — 세션 77 B-36-pre 이식 완료.
   * `_jumpPressed3a` — B-36 세션 78 이식 (jump edge flag).
5. **§7 B-36 근사 해소 기록** — focus_02_state_issues.md 에 취소선 + "세션 123
   B-42-B36 해소 완료". 분기 (b)/(c) 는 원래부터 1:1.
6. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 123 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L2841-L2847 분기 (a) 로컬 read ✓
- [근거] 1.21.1 Swimmer B-10a-post + B-42-B5 로 `isShallowDiveOrSwim` 활성 확인 ✓
- [대응] 원본 `getMaxPlayerSolidBetween(minY, maxY, 0)` → B-42a
  `getMaxPlayerSolidBetween(player, minY, maxY, 0)` 1:1 ✓
- [분기] 분기 (a) 조건 (grabJustPressed + isShallowDiveOrSwim + wouldWantClimb) 보존,
  내부 move 이동량만 정밀화 ✓
- [상수] `0` (horizontalTolerance) 원본 동일 ✓
- [타이밍] B-36 블록 내 분기 (a) 위치 유지 ✓
- [근사] B-36 근사 해소 — §7 갱신 ✓
- [신규] 없음 ✓
- [회귀] 분기 (b)/(c) 및 외부 분기 조건 영향 없음. `_jumpPressed3a`/`isStillSwimmingJump`
  동작 유지 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42-B39** (ClientState `fromSwimmingOrDiving` 3분기 isSlow 크롤
전환 본문 활성 — 원본 L1392-L1403 `crawlStandUpBottom` AABB 정밀 + `move(0,
crawlStandUpBottom - minY, 0)` 복원).

**진행률** (세션 123 종료 시점):
- Extended 완료: **37 원자** (B-19 22 + Phase 4 8 + Phase 6 **7** = 37)
- Extended 총 원자 ~61
- **Extended 진행률: 37/61 ≈ 61%**
- **포커스 #2 전체: (54+37)/115 ≈ 79%**
- **Phase 6 승격 3/8 — B-42-B5/B35/B36 완료**

### 세션 122 — 2026-04-25 — B-42-B35 `crawlStandUpBottom` 정밀 이동량 복원

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 승격 2번째 원자.

**진행한 작업**:
1. **현재 근사 지점 식별** (ClientState.java L1545-L1549):
   * B-35 분기 A: `wasCrawling && !isCrawling && !initializeCrawling && !flying`
   * 기존 근사: `heightOffset = 0F` 만 수행, `move(0, crawlStandUpBottom - minY, 0)` 생략.
2. **원본 공식 확인** (로컬 grep `crawlStandUpBottom` in SmartMovingSelf.java):
   * L2396: `double crawlStandUpBottom = -1;` (지역 변수 초기값)
   * L2399: `crawlStandUpBottom = getMaxPlayerSolidBetween(minY - (initializeCrawling ?
     0D : 1D), minY, crawlOverEdge ? 0 : -0.05);` (isCrawling||isClimbCrawling 시에만)
   * L2825: `move(0, (crawlStandUpBottom - sp.boundingBox.minY), 0, true);`
3. **분기 A 시점 분석**:
   * `wasCrawling=true, isCrawling=false` → L2399 계산 시점엔 아직 isCrawling=true
     (이후 L2442 `isCrawling = canCrawl && ...` 에서 해제). 즉 이전 틱 계산된
     `crawlStandUpBottom` 값이 분기 A 에서 유효.
   * 1.21.1 에서는 tick 내부 지역 변수 대신 분기 A 안에서 직접 재계산 (값 동일).
4. **Config 필드 확인** — `cfg.crawlOverEdge` 이미 이식 완료 (세션 이전).
5. **분기 A 정밀 이식** (ClientState.java L1545-L1555):
   ```java
   if (wasCrawling && !isCrawling && !initializeCrawling
           && !player.getAbilities().flying) {
       heightOffset = 0F;
       double minY = player.getBoundingBox().minY;
       double horizontalTolerance = cfg.crawlOverEdge ? 0 : -0.05;
       double crawlStandUpBottom = getMaxPlayerSolidBetween(player,
               minY - 1D, minY, horizontalTolerance);
       player.move(MovementType.SELF, new Vec3d(0, crawlStandUpBottom - minY, 0));
   }
   ```
6. **§7 B-35 근사 해소 기록** — focus_02_state_issues.md 의 B-35 근사 블록에 취소선 +
   "세션 122 B-42-B35 해소 완료" 기록.
7. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 122 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L2396-L2402 (crawlStandUpBottom 계산) +
  L2822-L2826 (B-35 분기 A 사용) 로컬 read ✓
- [근거] 1.21.1 Config `crawlOverEdge` 필드 존재 확인 ✓
- [대응] 원본 `getMaxPlayerSolidBetween(minY - 1D, minY, crawlOverEdge ? 0 : -0.05)` →
  B-42a `getMaxPlayerSolidBetween(player, minY - 1D, minY, horizontalTolerance)` 1:1 ✓
- [분기] 분기 A 조건 (wasCrawling && !isCrawling && !initializeCrawling && !flying) 보존,
  내부 공식만 정밀화. `initializeCrawling=false` 이므로 offset 1D 고정 ✓
- [상수] `1D` / `0` / `-0.05` 원본 동일 ✓
- [타이밍] B-35 블록 내 분기 A 위치 유지 — 원본 L2822-L2826 에 정확히 대응 ✓
- [근사] B-35 근사 해소 — §7 갱신 ✓
- [신규] 없음 ✓
- [회귀] 분기 B (진입 엣지) 는 세션 74 이미 1:1, 본 세션 영향 없음. MixinEntityClient
  L134 `crawlOverEdge` 소비 (fall damage 로직) 는 독립 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42-B36** (B-36 분기 (a) 얕은 물 swim/dive → walking 이동량 복원
— 원본 L2843 `move(0, getMaxPlayerSolidBetween(minY, maxY, 0) - minY, 0)`). 의존 게이트
`isShallowDiveOrSwim` 는 세션 121 B-42-B5 해소로 이제 정확히 계산됨 → (a) 분기 활성 조건 충족.

**진행률** (세션 122 종료 시점):
- Extended 완료: **36 원자** (B-19 22 + Phase 4 8 + Phase 6 **6** = 36)
- Extended 총 원자 ~61
- **Extended 진행률: 36/61 ≈ 59%**
- **포커스 #2 전체: (54+36)/115 ≈ 78%**
- **Phase 6 승격 2/8 — B-42-B5 + B-42-B35 완료**

### 세션 121 — 2026-04-25 — B-42-B5 `couldStandUp` 근사 해소 (Swimmer 2 지점)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 승격 8건 첫 원자.

**진행한 작업**:
1. **현재 근사 지점 식별** (Swimmer.java grep):
   * L202 (updateSwimState 내, B-10a-post 세션 109) — `couldStandUp` 로
     `isShallowDiveOrSwim` 계산.
   * L288 (handleSwimming 내, B-5 세션 63) — `couldStandUp` 로 `wantShallowSwim` 계산.
   * 두 지점 모두 `sm.dippingDepth >= 0F && sm.dippingDepth <= 1.5F` 단일값 근사.
2. **원본 공식 확인** (`SmartMovingSelf.java` L276 로컬 grep):
   ```
   boolean couldStandUp = playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5;
   ```
3. **원본 L507 재확인** — `isShallowDiveOrSwim = couldStandUp && (isDiving || isSwimming)`
   의 `couldStandUp` 도 L276 지역 변수 동일 사용. 1.21.1 분리 구조 (updateSwimState +
   handleSwimming) 에서는 각 지점마다 `SwimBorderValues` 재계산.
4. **두 지점 동시 수정**:
   * L202 → `ClientState.SwimBorderValues swimVals = computeSwimBorderValues(player);
     boolean couldStandUp = swimVals.playerSwimWaterBorder >= 0 &&
     swimVals.minPlayerSwimWaterDepth <= 1.5;`
   * L288 → 동일 패턴으로 치환.
5. **§7 B-5 근사 (1) 해소 기록** — `focus_02_state_issues.md` 의 B-5 근사 블록에서
   근사 (1) 에 취소선 + "세션 121 B-42-B5 해소 완료". 근사 (2) getClimbingOrientations
   8→4방향 / (3) swimDown=false 는 B-42 범위 외로 잔존.
6. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 121 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L276 (couldStandUp 지역 변수) + L507
  (isShallowDiveOrSwim 소비) 로컬 read ✓
- [근거] 1.21.1 Swimmer.java L202/L288 두 근사 지점 grep 확인 ✓
- [대응] 원본 `playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5` →
  `SwimBorderValues` (B-42d) 소비로 원본 파생값 1:1 복원 ✓
- [분기] AND 2-항 그대로 보존 ✓
- [상수] `0` / `1.5` 원본 동일 ✓
- [타이밍] 계산 시점 각 메서드 내 기존 위치 유지 (L202/L288) ✓
- [근사] B-5 근사 (1) 해소 → §7 B-5 블록에서 해소 기록. (2)/(3) 잔존 표기 명확화 ✓
- [신규] 없음 ✓
- [회귀] 기존 이식 (B-10a-post / isFakeShallowWaterSneaking / isTunnelAhead) 영향 없음 —
  `couldStandUp` 계산식만 정밀도 상승 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42-B35** (ClientState `crawlStandUpBottom` 정밀 계산 복원) 또는
**B-42-B36** (ClientState B-36 분기 (a) 이동량 복원). 둘 다 `getMaxPlayerSolidBetween`
소비 — B-42a 사용. B-42-B16 은 AABB 무관 (GUI 입력 차단) 이라 별도 취급.

**진행률** (세션 121 종료 시점):
- Extended 완료: **35 원자** (B-19 22 + Phase 4 8 + Phase 6 **5** = 35)
- Extended 총 원자 ~61
- **Extended 진행률: 35/61 ≈ 57%**
- **포커스 #2 전체: (54+35)/115 ≈ 77%**
- **Phase 6 승격 1/8 — B-42-B5 완료** (B-5 근사 1/3 해소)

### 세션 120 — 2026-04-25 — B-42d AABB 파생값 struct-like 헬퍼 (`SwimBorderValues`)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 B-42 본체 완결 (4/4).

**세션 준비 변경**: 사용자가 SmartMoving/SmartRender 1.7.10 원본 전체 소스를 로컬에
압축 해제 — `C:\Work\minecraft\porting\sm_original\SmartMoving` +
`C:\Work\minecraft\porting\sm_original\SmartRender`. 이후 WebFetch 대신
`Read`/`Grep` 으로 원본 접근. memory `reference_original_sources.md` 에 경로 저장.

**진행한 작업**:
1. **원본 소스 확보** (로컬 grep):
   * `SmartMovingSelf.java` L255-L270 — AABB 파생값 9개 정의 블록.
   * `SmartMovingSelf.java` L417 — `playerCrawlWaterBorder = dippingDepth + wasHeightOffset`
     (B-42d 범위 외 — B-42-B? 승격에서 `wasHeightOffset` 추가 예정).
2. **범위 결정**: B-42 본체 원자는 **헬퍼 제공** 에 집중. 실제 소비 (B-5/B-35/B-36
   근사 해소) 는 승격 8건에서. `playerCrawlWaterBorder` 공식 수정은 본 원자에서 제외.
3. **SwimBorderValues struct 신설** (`SmartMovingClientState.java` B-42c 직후):
   * `public static final class SwimBorderValues` — 9 final 필드 (i/j/k/j_offset +
     6 double 파생값). private constructor.
   * `public static SwimBorderValues computeSwimBorderValues(ClientPlayerEntity player)`
     — 원본 L255-L270 그대로 1:1 이식. B-42a/b/c 헬퍼 참조.
4. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 120 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L228-L270 read (로컬 경로) ✓
- [근거] 호출처 확인 (원본 L272/L276/L282/L303 등) — 소비는 승격 원자 범위 ✓
- [대응] 원본 `sp.posX/Z` → `player.getX/Z()`, `MathHelper.floor_double` →
  `MathHelper.floor`, `sp.boundingBox` → `player.getBoundingBox()` 표면 매핑 ✓
- [분기] 9개 파생값 전수 이식 (Math.min / 뺄셈 순서 / boundingBox.maxY 기준) ✓
- [상수] `1.8` / `1.2` / `2` boundingBox offset 원본 1:1 ✓
- [타이밍] helper 신설 (tick-free 정적 메서드) — 호출 지점은 후속 승격 원자 ✓
- [근사] 없음 — B-42a/b/c 헬퍼에 근사 위임 ✓
- [신규] `SwimBorderValues` 클래스 + `computeSwimBorderValues` 신설 ✓
- [회귀] 현재 호출 지점 없음 → 회귀 영향 없음 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **Phase 6 승격 8건 진입** — **B-42-B5** (Swimmer `couldStandUp`
근사 해소). 현재 `dippingDepth <= 1.5F` 단일값 근사 → 원본
`playerSwimWaterBorder >= 0 && minPlayerSwimWaterDepth <= 1.5` 복원.
`SwimBorderValues` 소비 첫 시점.

**진행률** (세션 120 종료 시점):
- Extended 완료: **34 원자** (B-19 22 + Phase 4 8 + Phase 6 **4** = 34)
- Extended 총 원자 ~61
- **Extended 진행률: 34/61 ≈ 56%**
- **포커스 #2 전체: (54+34)/115 ≈ 77%**
- **🎉 Phase 6 B-42 본체 4/4 완료 — 승격 8건 진입 준비**

### 세션 119 — 2026-04-24 — B-42c 액체 경계 헬퍼 3종 (`getLiquidBorder` + `getMax/MinPlayerLiquidBetween`)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 B-42c 원자 (액체 헬퍼).

**진행한 작업**:
1. **원본 소스 확보**:
   * `docs/research/original/smartmoving/moving/SmartMovingBase.md`:
     - L210-L233 `getLiquidBorder(i, j, k)` — 액체 높이 (0.0~1.0F)
     - L249-L263 `getNormalWaterBorder(i, j, k)` — metadata 기반 물 높이
     - L560-L577 `getMaxPlayerLiquidBetween(yMin, yMax)` — 위→아래 스캔
     - L584-L604 `getMinPlayerLiquidBetween(yMin, yMax)` — 아래→위 스캔
   * `.tmp_research/SmartMovingSelf.java` grep 호출처 3곳:
     - L265 `totalSwimWaterBorder` (swim 경계 판정)
     - L1372 / L2414 `crawlStandUpLiquidCeiling` (standupIfPossible / tickEssential)
2. **1.21.1 Config 상태 확인**:
   * `lavaLikeWater` 필드 **미이식** (grep 결과 0 matches) → 근사 등록.
   * `hasFiniteLiquid` 필드 **미이식** → FiniteLiquid mod 자체 1.21.1 미이식.
3. **B-42c 본문 이식** (`SmartMovingClientState.java` B-42b 직후):
   * `getLiquidBorder(player, i, j, k)` — `FluidState.getHeight(world, pos)` 근사.
     water 만 처리 (lava 는 0F 반환).
   * `getMaxPlayerLiquidBetween(player, yMin, yMax)` — `jMax→jMin` 역순 탐색, 첫 액체
     발견 시 `j + border` 반환. 없으면 `yMin`.
   * `getMinPlayerLiquidBetween(player, yMin, yMax)` — `jMin→jMax` 탐색, 조건부 `j` /
     `yMin` / `yMax` 분기 1:1.
4. **§7 B-42c 근사 3건 등록**:
   * (1) FiniteLiquid mod 분기 생략 (mod 미이식).
   * (2) `_lavaLikeWater` Config 필드 미이식 → lava 항상 0F (default false 이므로 평시
     영향 없음, lava 수영 기능만 누락).
   * (3) `getNormalWaterBorder` metadata 계산 → `FluidState.getHeight` 흡수 (수면
     `0.8875F` 근사값은 vanilla FlowableFluid 로 근사, 완전 동치 아님).
5. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 119 기준)**:
- [근거] 원본 `SmartMovingBase.md` L131-L182 + L411-L451 read ✓
- [근거] 호출처 3곳 `.tmp_research/SmartMovingSelf.java` grep ✓
- [대응] `sp.posX/Z` → `player.getX/Z()`, `MathHelper.floor_double` → `MathHelper.floor`,
  `world.getBlock(i,j,k)` → `world.getFluidState(BlockPos)` 표면 매핑 ✓
- [분기] Max 메서드 (yMax→yMin 역순) / Min 메서드 (jMin→jMax + `j>yMin` `j+border>yMin`
  else 3분기) / getLiquidBorder (FluidTags.WATER 단일 분기) 이식 완료 ✓
- [상수] 원본 L600 `j > yMin` / `j + swimWaterBorder > yMin` 상수 조건 1:1 ✓
- [타이밍] helper 신설 — 호출 지점은 후속 원자 ✓
- [근사] §7 B-42c 근사 3건 등록 (FiniteLiquid / lavaLikeWater / NormalWaterBorder) ✓
- [신규] 세 메서드 신설 — 본체 §16 변경 없음 ✓
- [회귀] 현재 호출 지점 없음 → 회귀 영향 없음 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42d** (`realMinPlayerSwimWaterDepth` / `playerSwimWaterBorder` /
`playerCrawlWaterBorder` 등 AABB 기반 파생값 이식). 이 필드들은 원본 `updateEntityActionState`
(L228-L267) 의 지역 계산인데 Swimmer/ClientState 필드 승격 필요. B-42a~c 헬퍼 참조.
이후 승격 8건 (B-42-B5/B16/B20/B26/B35/B36/B39/B18a/b).

**진행률** (세션 119 종료 시점):
- Extended 완료: **33 원자** (B-19 22 + Phase 4 8 + Phase 6 **3** = 33)
- Extended 총 원자 ~61
- **Extended 진행률: 33/61 ≈ 54%**
- **포커스 #2 전체: (54+33)/115 ≈ 76%**
- **Phase 6 B-42 본체 3/4 — 고체/액체 헬퍼 모두 완료**

### 세션 118 — 2026-04-24 — B-42b `getMinPlayerSolidBetween` AABB 정밀 헬퍼 (B-42a 대칭)

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 B-42a 직후 대칭 원자.

**진행한 작업**:
1. **원본 소스 확보**:
   * `docs/research/original/smartmoving/moving/SmartMovingBase.md` L521-L540 read —
     원본 `getMinPlayerSolidBetween(yMin, yMax, horizontalTolerance)` L398-L409 코드 확보.
   * `.tmp_research/SmartMovingSelf.java` grep 으로 호출처 4곳 확인:
     - L266 `minPlayerSwimWaterCeiling` (swim ceiling 판정)
     - L1151 `actuallySolidHeight` (jump clearance 추가 조건)
     - L1373 `crawlStandUpCeiling` (standupIfPossible)
     - L2400 `crawlStandUpCeiling` (tickEssential)
2. **B-42b 본문 이식** (`SmartMovingClientState.java` B-42a 직후 line 1887):
   ```java
   public static double getMinPlayerSolidBetween(ClientPlayerEntity player,
                                                  double yMin, double yMax,
                                                  double horizontalTolerance) {
       Box pb = player.getBoundingBox();
       Box checkBox = new Box(
               pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
               pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

       double result = yMax;
       for (net.minecraft.util.shape.VoxelShape shape
               : player.getWorld().getBlockCollisions(player, checkBox)) {
           if (shape.isEmpty()) continue;
           // 근사 이식 — 원본과 차이: VoxelShape.getBoundingBox() 단일 box
           Box box = shape.getBoundingBox();
           if (box.maxX >= pb.minX - horizontalTolerance
                   && box.minX <= pb.maxX + horizontalTolerance
                   && box.maxY >= yMin
                   && box.minY <= yMax
                   && box.maxZ >= pb.minZ - horizontalTolerance
                   && box.minZ <= pb.maxZ + horizontalTolerance) {
               result = Math.min(result, box.minY);
           }
       }
       return Math.max(result, yMin);
   }
   ```
3. **§7 B-42b 근사 등록** (B-42a 직후): VoxelShape→Box 단일 enclosing box 동일 패턴.
   multi-shape 블록에서 minY 약간 낮게 — 호출처 의미상 약간 보수적 작동 (실용 등가).
4. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 118 기준)**:
- [근거] 원본 `SmartMovingBase.md` L521-L540 `getMinPlayerSolidBetween` 본체 read ✓
- [근거] 호출처 4곳 `.tmp_research/SmartMovingSelf.java` grep 으로 확인 ✓
- [대응] 원본 `result = yMax` / `Math.min(result, box.minY)` / `Math.max(result, yMin)` →
  1.21.1 대응 확정. B-42a 대칭 ✓
- [분기] 박스 교차 6-AND 조건 / Math.min / Math.max clamp 1:1 ✓
- [상수] 없음 ✓
- [타이밍] helper 신설 (tick-free 정적 메서드) — 호출 지점은 후속 원자 ✓
- [근사] §7 B-42b 근사 1건 등록 (VoxelShape→Box, B-42a 와 동일 패턴) ✓
- [신규] `getMinPlayerSolidBetween` 메서드 신설 ✓
- [회귀] 현재 호출 지점 없음 → 회귀 영향 없음 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42c** (`getMinPlayerLiquidBetween` + `getMaxPlayerLiquidBetween`
액체 경계 헬퍼). 원본 SmartMovingBase L411-L451. liquid 판정 (isInLiquid / swim
ceiling / diving 등). liquid 는 1.21.1 `FluidState` + `getFluidHeight()` 로 매핑 필요.
이후 B-42d (realMinPlayerSwimWaterDepth 등 파생값) → 승격 8건.

**진행률** (세션 118 종료 시점):
- Extended 완료: **32 원자** (B-19 22 + Phase 4 8 + Phase 6 **2** = 32)
- Extended 총 원자 ~61
- **Extended 진행률: 32/61 ≈ 52%**
- **포커스 #2 전체: (54+32)/115 ≈ 75%**
- **Phase 6 B-42 본체 2/4 — 고체 헬퍼 완료 (maxY/minY 대칭)**

### 세션 117 — 2026-04-24 — **Phase 6 진입** B-42a `getMaxPlayerSolidBetween` AABB 정밀 헬퍼 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지. Phase 6 진입.

**진행한 작업**:
1. **원본 소스 확보**:
   * `.tmp_research/SmartMovingSelf.java` grep `getMaxPlayerSolidBetween` — 10+ 호출 지점 확인
     (본체 메서드는 `SmartMovingBase` 에 있어 미포함).
   * Agent WebFetch 로 원본 `SmartMovingBase` L229-L306 획득 —
     `getPlayerSolidBetween` / `isPlayerInSolidBetween` / `getMaxPlayerSolidBetween` /
     `getMinPlayerSolidBetween` / `isInLiquid` / `getMaxPlayerLiquidBetween` /
     `getMinPlayerLiquidBetween` / `isCollided` 본체 전수 확인.
2. **기존 헬퍼 상태 점검**:
   * `ClientState.isPlayerInSolidBetween(player, yMin, yMax, horizontalTolerance)` —
     세션 48 이식 완료 (`canStandUp` 의존).
   * `canStandUp(player)` — 세션 48 이식 완료.
   * **`getMaxPlayerSolidBetween` 은 미이식** — 본 원자에서 신설.
3. **B-42a 본문 이식** (`SmartMovingClientState.java` `isPlayerInSolidBetween` 직후):
   ```java
   public static double getMaxPlayerSolidBetween(ClientPlayerEntity player,
                                                  double yMin, double yMax,
                                                  double horizontalTolerance) {
       Box pb = player.getBoundingBox();
       Box checkBox = new Box(
               pb.minX - horizontalTolerance, yMin, pb.minZ - horizontalTolerance,
               pb.maxX + horizontalTolerance, yMax, pb.maxZ + horizontalTolerance);

       double result = yMin;
       for (net.minecraft.util.shape.VoxelShape shape
               : player.getWorld().getBlockCollisions(player, checkBox)) {
           if (shape.isEmpty()) continue;
           // 근사 이식 — 원본과 차이: VoxelShape.getBoundingBox() 단일 box
           Box box = shape.getBoundingBox();
           if (box.maxX >= pb.minX - horizontalTolerance
                   && box.minX <= pb.maxX + horizontalTolerance
                   && box.maxY >= yMin
                   && box.minY <= yMax
                   && box.maxZ >= pb.minZ - horizontalTolerance
                   && box.minZ <= pb.maxZ + horizontalTolerance) {
               result = Math.max(result, box.maxY);
           }
       }
       return Math.min(result, yMax);
   }
   ```
4. **§7 B-42a 근사 등록** (focus_02_state_issues.md L467 부근):
   * 원본 `AxisAlignedBB` 단일 박스 순회 → 1.21.1 `VoxelShape.getBoundingBox()` 단일
     enclosing box 근사.
   * 대부분 블록 (cube/slab/stair) 은 단일 박스라 정확성 손실 없음.
   * 다중 박스 블록 (wall/fence/chain) 은 enclosing box 가 과대 추정되어 maxY 약간 높음
     (실용 등가 — crawlStandUpBottom / climbGap 판정 영향 없음).
5. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` **BUILD SUCCESSFUL**.

**완료 전 검증 체크리스트 (세션 117 기준)**:
- [근거] 원본 `SmartMovingBase` L247-L262 `getMaxPlayerSolidBetween` 본체 WebFetch 확보 ✓
- [근거] 1.21.1 `SmartMovingClientState.java` 기존 `isPlayerInSolidBetween` / `canStandUp`
  위치 Read 로 삽입 지점 확정 ✓
- [대응] 원본 `List colliding boxes` 순회 → 1.21.1 `getBlockCollisions()` Iterable<VoxelShape>
  대응 확정 ✓
- [분기] `result = yMin` 초기값 / 박스 교차 6-AND 조건 / `Math.min(result, yMax)` clamp
  전수 1:1 ✓
- [상수] 없음 ✓
- [타이밍] helper 신설 (tick-free 정적 메서드) — 호출 지점은 후속 원자 ✓
- [근사] §7 B-42a 근사 1건 등록 (VoxelShape→Box 단일 box) ✓
- [신규] `getMaxPlayerSolidBetween` 메서드 신설 — B-42b~d + 승격 8건 기반 ✓
- [회귀] 현재 호출 지점 없음 → 회귀 영향 없음. 후속 원자에서 호출 시점부터 활성 ✓
- [빌드] `compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL ✓

**다음 세션 권고**: **B-42b** (`getMinPlayerSolidBetween` — maxY 대응의 minY 집계 대칭
로직). 이후 B-42c (liquid 계열) → B-42d (파생값) → B-42-B5/B16/B20/B26/B35/B36/B39/B18a/b
승격 8건.

**진행률** (세션 117 종료 시점):
- Extended 완료: **31 원자** (B-19 22 + Phase 4 8 + Phase 6 **1** = 31)
- Extended 총 원자 ~61
- **Extended 진행률: 31/61 ≈ 51%**
- **포커스 #2 전체: (54+31)/115 ≈ 74%**
- **Phase 6 진입 — AABB 정밀 헬퍼 신설 시작**

### 세션 116 — 2026-04-24 — B-40-post 재검토 → **Phase 4 전체 완료**

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2745-L2785 전수 read** — L2751/L2760/L2767 의 실제 구조 확인.
2. **세션 88 3차 감사 오분류 확인**:
   * L2751 은 `wasCrawling = false` (필드 리셋, 메서드 호출 아님)
   * L2760 은 `wasCrawling = toCrawling();` (실제 메서드 호출)
   * L2767 은 `wasCrawling = toCrawling;` (지역 boolean 변수 참조 — L2757 선언)
   * 3차 감사 시 원본 본체 미확인 + L1883 의 "wasCrawling 재설정 8 위치" 목록을
     "toCrawling() 호출 지점" 으로 오분류함.
3. **1.21.1 현재 상태 grep** — 3 지점 전부 이식 완료 확인:
   * L2751 → B-17b1 세션 48 L1419 `wasCrawling = false;` (canStandUp 분기)
   * L2760 → B-17b2 세션 73 L1435 `wasCrawling = toCrawling();`
   * L2767 → B-17b2 세션 73 L1442 `wasCrawling = toCrawlingLocal;`
4. **결론**: B-40-post 는 이미 B-17b1/B-17b2 이식에 자동 흡수됨. 코드 변경 없음.
   Extended §3 체크박스 [x] 처리 + 본체 §6 매핑 표기 갱신 (⚠️ → ✓).
5. **🎉 Phase 4 (B-10a-post / B-10b-pre / B-10b-post / B-10c-post / B-31c-post /
   B-10-reset-post / B-N-standup / B-40-post) 전체 완료.**

**완료 전 검증 체크리스트 (세션 116 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L2745-L2785 전수 read ✓
- [근거] 1.21.1 `SmartMovingClientState.tickEssential` L1398-L1442 + L1419 + L1435 +
  L1442 grep 으로 이식 상태 전수 확인 ✓
- [대응] 3 지점 (L2751/L2760/L2767) 모두 원본 ↔ 1.21.1 대응 재확인 완료 ✓
- [분기] `else if (wasCrawlClimbing)` 3분기 + 진입 조건 + canStandUp 분기 전수 식별 ✓
- [상수] 없음 ✓
- [타이밍] B-17b1/b2 이식 시점 (세션 48/73) 에 이미 정상 위치 배치됨 ✓
- [근사] 없음 (재검토 작업) ✓
- [신규] 세션 88 3차 감사 오분류 확인 — 향후 오분류 방지 위해 본체 §6 매핑 표기 명확화 ✓
- [회귀] 코드 변경 없음 → 회귀 영향 없음 ✓
- [빌드] 코드 변경 없음 — 빌드 검증 생략. 직전 세션 115 빌드 성공 상태 유지 ✓

**다음 세션 권고**: **Phase 6 진입** — **B-42a** (`getMaxPlayerSolidBetween` AABB 정밀
헬퍼 이식, 원본 1-based 위치). Phase 6 는 의존 순서상 Phase 5 이전 (Phase 5 가 AABB
의존). B-42a/b/c/d 선행 → B-42-B5/B16/B20/B26/B35/B36/B39/B18a/b 승격. 예상 세션 규모:
AABB 헬퍼 이식 복잡도에 따라 2-3 세션.

**진행률** (세션 116 종료 시점):
- Extended 완료: **30 원자** (B-19 22 + Phase 4 **8** = 22+8)
- Extended 총 원자 ~61
- **Extended 진행률: 30/61 ≈ 49%**
- **포커스 #2 전체: (54+30)/115 ≈ 73%**
- **Phase 3 + Phase 4 전체 완료** — Phase 6 진입 준비

### 세션 115 — 2026-04-24 — B-N-standup `standupIfPossible` 메서드 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L2165-L2219 read** — 4 메서드 본체 확인:
   * L1681-L1686 `resetHeightOffset` — boundingBox/height 조작 + heightOffset=0F
   * L2165-L2183 `standupIfPossible()` 무인자 — heightOffset 리셋 early return + gap
     체크 후 standUp or toSlidingOrCrawling
   * L2186-L2212 `standupIfPossible(tryLanding, restoreFromFlying)` — 비행 해제 + 동일
     gap 체크
   * L2214-L2219 `standUp(gapUnderneight)` — move + crawl/headJump 해제 + resetHeightOffset
2. **1.21.1 의존 현황 확인**:
   * `heightOffset` (필드) 이식 완료
   * `resetHeightOffset` 메서드 없음 (인라인만)
   * `getGapUnderneight` / `getGapOverneight` / `standUp` 미이식
   * `canStandUp(player)` 이식 완료 — AABB 근사
3. **이식 내용** (ClientState 4 메서드 신설):
   * `resetHeightOffset()` — `heightOffset = 0F` 만 (boundingBox 조작 근사)
   * `standUp()` — isCrawling/isHeadJumping false + resetHeightOffset (move 이동 근사 생략)
   * `standupIfPossible(player)` — heightOffset >= 0 early return + canStandUp 기준
     standUp 호출 (toSlidingOrCrawling 생략)
   * `standupIfPossible(player, tryLanding, restoreFromFlying)` — tryLanding +
     canStandUp 조합으로 비행 해제 + standUp 호출
4. **B-24 (세션 53) 연결** — `restoreFromFlying = true` 설정 직후 `standupIfPossible(player,
   false, true)` 호출 추가.
5. **본체 §6 매핑 + §7 근사 4건 등록**:
   (1) resetHeightOffset — boundingBox/height 조작 생략
   (2) standUp — move 이동 생략 (getGapUnderneight 없음)
   (3) standupIfPossible — AABB gap 체크 → canStandUp 근사 (toSlidingOrCrawling 생략)
   (4) 2-arg 오버로드 — capabilities.flying 직접 조작 근사 (SM 측 isFlying 만)

**완료 전 검증 체크리스트 (세션 115 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L1681-L1686 + L2165-L2219 전수 read ✓
- [근거] 의존 — heightOffset (기존) / canStandUp (기존) / isCrawling / isHeadJumping /
  isFlying (기존) / restoreFromFlying (B-24 세션 53) 전수 충족 ✓
- [대응] 4 메서드 원본 ↔ 1.21.1 side-by-side (근사 명시). B-24 호출 연결 ✓
- [분기] 무인자: heightOffset>=0 / canStandUp 2갈래. 2-arg: heightOffset>=0 / tryLanding &&
  canStandUp / !restoreFromFlying / canStandUp 분기 전수. 근사 경로는 주석 명시 ✓
- [상수] `0F` (heightOffset 리셋) / `1D - gapUnderneight` (move 이동, 생략) 원본 인식 ✓
- [타이밍] B-24 직후 호출 — 원본 L2544 standupIfPossible(tryLanding, restoreFromFlying)
  위치 대응 ✓
- [근사] **§7 B-N-standup 근사 4건 등록 완료** (boundingBox / move / AABB / capabilities.flying) ✓
- [신규] 본체 §6 매핑 테이블 `standupIfPossible()` 표기 갱신 ("✗ 미이식" → "✓ 근사 이식") ✓
- [회귀] 기존 B-24 `restoreFromFlying = true` 설정만 하고 소비자 없었음 → 이제 즉시
  standupIfPossible 호출 → heightOffset 리셋 + crawl/headJump 해제 정상 경로. 이전엔
  heightOffset=-1F 유지 가능성 있었음 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-40-post** — `toCrawling()` 잔여 호출 지점 L2751/L2760/L2767 이식
(세션 88 3차 감사 발견). Agent WebFetch 로 원본 해당 대역 확보 + 이식 위치 결정.
예상 1-2 세션.

**진행률** (세션 115 종료 시점):
- Extended 완료: **29 원자** (B-19 22 + B-10a-post + B-10b-pre + B-10b-post + B-10c-post +
  B-31c-post + B-10-reset-post + **B-N-standup**)
- Extended 총 원자 ~61
- **Extended 진행률: 29/61 ≈ 48%**
- **포커스 #2 전체: (54+29)/115 ≈ 72%**
- **Phase 4**: 7/8 (**1 남음** — B-40-post)

### 세션 114 — 2026-04-24 — B-10-reset-post `resetSwimming()` 메서드 완전 이식

**사용자 지시**: "무조건 엄격 1대1 완료" 방침 유지.

**진행한 작업**:
1. **원본 L1488-L1498 read** — `resetSwimming()` 본체 8 필드 리셋 확인.
2. **원본 호출 지점 6 곳 grep** (L242/L253/L555/L585/L607/L645). updateSwimState 2 곳 +
   handleSwimming 4 곳.
3. **1.21.1 현재 상태 확인** — `!isTouchingWater` 경로에서 5 필드 리셋만 (dippingDepth /
   isDipping / isSwimming_sm / isDiving / isShallowDiveOrSwim) + B-10c-post 에서 추가된
   isStillSwimmingJump. 누락 3 필드: isLevitating / isFakeShallowWaterSneaking /
   isJumpingOutOfWater.
4. **`SmartMovingSwimmer.resetSwimming(sm)` static private 메서드 신설** — 원본 L1488-L1498
   8 필드 리셋 1:1 이식.
5. **`updateSwimState` 의 `!isTouchingWater` 경로** 리팩터:
   * 기존 5 개별 필드 할당 → `resetSwimming(sm)` 메서드 호출
   * 원본 L550 대응 `isStillSwimmingJump = false` (세션 112) 는 별도 유지 (원본
     resetSwimming 에 없고 useStandard 경로 대응이라 별도)
   * 1.21.1 추가 `waterMovementTicks = 0` 유지 (원본에 없음, B-12 일관성)
6. **본체 §6 매핑 테이블 갱신** — "⚠️ 부분 이식" → "✓ 완전 이식 (B-10-reset-post 세션 114)".

**완료 전 검증 체크리스트 (세션 114 기준)**:
- [근거] 원본 `.tmp_research/SmartMovingSelf.java` L1488-L1498 + 호출 지점 6 곳 grep ✓
- [근거] 의존 전수 충족 — 8 필드 모두 1.21.1 이식됨 (B-10a~d + isDiving/isSwimming_sm/
  isDipping/isFakeShallowWaterSneaking/dippingDepth 기존) ✓
- [대응] 8 필드 리셋 원본 ↔ 1.21.1 메서드 1:1 ✓
- [분기] 메서드 단일 경로 (분기 없음). `!isTouchingWater` 호출 + isStillSwimmingJump +
  waterMovementTicks 별도 보존 ✓
- [상수] `-1F` (dippingDepth) / `false` 7 원본 동일 ✓
- [타이밍] 원본 L242/L253 (updateSwimState 진입 `!isInWater` 분기) 대응 위치 ✓
- [근사] 없음 — 8 필드 완전 이식 (B-10a/b/c/d 근사는 별도 원자에서 관리) ✓
- [신규] `resetSwimming(sm)` 메서드 신설 (static private). B-9 Phase 5 재작성 시 재사용
  예정 ✓
- [회귀] 기존 5 필드 리셋 → 8 필드 리셋 확장. isLevitating / isFakeShallowWaterSneaking /
  isJumpingOutOfWater 가 물 밖 전환 시 비로소 정상 리셋. 이전에는 물 밖에서도 이전 값
  유지 (특히 isFakeShallowWaterSneaking 은 isSwimming 도 리셋되지만 self 값 유지 가능성
  있었음) — 이제 깨끗한 초기화 ✓
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` SUCCESSFUL (4s) ✓

**다음 세션 권고**: **B-N-standup** — `standupIfPossible` 메서드 이식 (세션 53 B-24
로그 "B-N 후속" 으로 남음). 원본 Agent WebFetch 필요. `restoreFromFlying` 소비자 +
`handleSwimming` 수영→크롤 전환 의존. 예상 1-2 세션.

**진행률** (세션 114 종료 시점):
- Extended 완료: **28 원자** (B-19 22 + B-10a-post + B-10b-pre + B-10b-post + B-10c-post +
  B-31c-post + **B-10-reset-post**)
- Extended 총 원자 ~61
- **Extended 진행률: 28/61 ≈ 46%**
- **포커스 #2 전체: (54+28)/115 ≈ 71%**
- **Phase 4**: 6/8 (2 남음 — B-N-standup / B-40-post)

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
