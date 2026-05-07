# 헤드점프 (Head Jump) 기능 — 작업 체크리스트

> 본 체크리스트는 `docs/research_headjump.md` 의 §A~§L (기능 영역) 을 차근차근
> **처음부터** 1:1 정합 감사하면서 진행한다. 애니메이션 영역(§M)은 별도 사이클.
>
> 진행 원칙:
> - 각 단계는 **(a) 원본 코드 다시 읽기 → (b) 1.21.1 현재 코드 정독 → (c) 1:1 정합 감사 → (d) 발견 즉시 수정 → (e) 회귀 검증**.
> - 감사 중 다른 영역 누락/오역 발견 시 **기록 후 즉시 1:1 수정** (메모리 `feedback_fix_immediately` 원칙).
> - 추측 분기 1차 실패 시 **즉시 디버그 로그** 추가 (메모리 `feedback_debug_log_first`).
> - server/self/remote 3측 분리 분석 의무 (메모리 `feedback_network_explain_three_sides`).
> - self → server/remote 매핑 시 self 코드 그대로 1:1 복제 (메모리 `feedback_self_to_server_remote_one_to_one`).
> - 인게임 테스트는 통합 단계로 분리 (메모리 `feedback_integration_test`).
>
> 진행 표기: `[ ]` 미시작 / `[~]` 진행중 / `[x]` 완료 / `[!]` 차단 / `[-]` 무관·불필요 결정

---

## Phase 0. 사전 준비

- [x] 0-1. 현재 브랜치 / git status 확인 (잔존 변경 충돌 검사)
  - 브랜치: `master`. 최근 커밋: BUG-7 정착 (9493b21).
  - 미커밋: `.claude/settings.local.json` (M) + `.tmp_research/` (이전 작업) + `docs/checklist_bug11_*` + `docs/research_bug11_*` + `docs/sync_task/` + 본 작업 docs.
  - **헤드점프 작업과 충돌 없음** (모두 다른 영역).
- [x] 0-2. 원본 소스 경로 재확인
  - `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\` 정상. `SmartMovingSelf.java`/`SmartMoving.java`/`SmartMovingOther.java`/`config/` 등 모두 존재.
- [x] 0-3. 1.21.1 측 헤드점프 관련 파일 인덱스 (research §1.21.1 표) 확인
  - `SmartMovingClientState.java` / `SmartMovingJumper.java` / `SmartMovingClimber.java` / `SmartMovingMover.java` / `SmartMovingHud.java` / `SmartMovingClient.java` / `SmartMovingConfig.java` / `SmartMovingState.java` / `SmartMovingServer.java`
  - Mixin: `MixinPlayerEntity.java` / `MixinClientPlayerEntity.java` / `MixinPlayerEntityClient.java` / `MixinLivingEntityClient.java` / `MixinEntityClient.java` / `MixinPlayerEntityModelClient.java` / `MixinPlayerEntityRenderer.java` (※ 마지막 2개는 애니메이션 영역)
  - `MixinCapeFeatureRenderer.java` (※ 망토, 애니메이션)
- [x] 0-4. `docs/log_temp.txt` 보존 확인
  - 5608 라인 보존됨 (`feedback_log_temp_keep` 준수, 절대 삭제 금지)
- [x] 0-5. 본 체크리스트 + research 문서 작업 화면에 띄움

---

## Phase A. 상태 / 필드 1:1 정합 (research §A) — ✅ 완료

### A-1. `isHeadJumping` 필드 ✅
- [x] 원본 `SmartMoving.java` L47 ↔ 1.21.1 `SmartMovingClientState.java` L55 — 정합
- [x] 아키텍처 차이 확인: 원본 abstract base (Self+Other 공통) → 1.21.1 ClientState/Server/State 3분리. `feedback_remote_motion_correction` 패턴과 일치하므로 정상.

### A-2. `headJumpCharge` 필드 ✅
- [x] 원본 `SmartMovingSelf.java` L1452 ↔ 1.21.1 `SmartMovingClientState.java` L46 — 정합 (float / public)
- [x] self only — remote 비공유, broadcast 는 isHeadJumping bit 만 (원본 동일 의도)

### A-3. `wasHeadJumping` 필드 ✅
- [x] 원본 `SmartMovingSelf.java` L3075 ↔ 1.21.1 `SmartMovingClientState.java` L63 — 정합
- [x] 1.21.1 측 주석에 원본 L2524 매핑 의도 명시됨

### A-4. `SlideToHeadJumpingFallDistance` 상수 ✅ (수정 완료)
- [x] 원본 `SmartMovingContext.java` L51 `public static final float SlideToHeadJumpingFallDistance = 0.05F;`
- [x] 사용자 결정: **상수로 분리** → `SmartMovingClientState.java` L114-L119 `SLIDE_TO_HEADJUMPING_FALL_DISTANCE = 0.05F` 추가 + L2049 사용처 상수 참조로 변경

### A-5. 인터페이스 / Getter ✅
- [x] 원본 `ISmartMovingSelf.getHeadJumpCharge()` / `ISmartMovingClient.getMaximumHeadJumpCharge()` — 1.21.1 측 둘 다 미구현
- [x] 결정: **불필요** — 1.21.1 단일 모듈이라 외부 API 노출 무관. 직접 필드 접근 (HUD 등) 패턴 유지.

### A-6. 발견된 오역/누락 ✅
- A-1~A-3 / A-5: 없음
- A-4: 인라인 → 상수 분리 (수정 완료)

---

## Phase B. 입력 / 차지 로직 1:1 정합 (research §B, 원본 L1880-L1899) — ✅ 완료

### B-1. `Config.isHeadJumpingEnabled()` 가드 ✅
- [x] 원본 L1881 `_headJump.value && enabled` AND 식
- [x] 1.21.1: 함수 시작 `if (!cfg.enabled) return;` (L360) + 헤드점프 블록 `if (cfg.headJump)` (L450) — 분리되어 있으나 의미 동등 (BUG-14 세션 36 패턴)

### B-2. `isHeadJumpCharging` 결정식 (★ 핵심) ✅ — 🔴 BUG fix
- [x] **BUG 발견**: `SmartMovingJumper.java` L450-L453 에서 `isGroundSprinting` / `isRunning` 로컬 변수 잘못 재정의
  - 원본 `isGroundSprinting` 필드 (`canHorizontallySprint && (onGround||isLevitating) && !swimming && !diving && !climbing`) 누락 — 잘못된 식 (`isStandupSprintingOrRunning` 식) 사용
  - 원본 `isRunning()` 메서드의 `vanilla()` (= `!Config.enabled || _vanillaStyle.value`) 대신 `sm.isFlying` 잘못 사용
- [x] **fix**: 로컬 변수 삭제 + `sm.isGroundSprinting` 필드 + `sm.isRunning(player)` 메서드 직접 사용 (1.21.1 측 L205/L3270 매핑 활용)
- [x] `!sm.isSliding` 가드는 B-Slide-HeadJump-fix (의도적 원본 위반) 로 유지

### B-3. 차징 중 `headJumpCharge++` ✅ — 🔧 수정
- [x] 원본 L1886 `headJumpCharge++` (clamp 없음, getHeadJumpFactor 안에서만 clamp)
- [x] **fix**: `Math.min(... + 1F, ...)` → `sm.headJumpCharge++` 로 1:1 정정

### B-4. release edge → tryJump(HeadUp) + charge=0 ✅
- [x] 원본 L1887-L1892 ↔ 1.21.1 L463-L468 정합

### B-5. 차징 외 + 차지 잔존 → blockJumpTillButtonRelease ✅
- [x] 원본 L1893-L1898 ↔ 1.21.1 L469-L473 정합

### B-6. 일반 점프 가드 `!isHeadJumpCharging` ✅
- [x] 원본 L1915 5-AND ↔ 1.21.1 L498-L499 정합

### B-7. movementInput.jump vs raw key ✅ — 🔧 수정
- [x] 원본 L1885 `esp.movementInput.jump` (vanilla movementInput, 비행/sleeping 가공 후 값)
- [x] **fix**: `mc.options.jumpKey.isPressed()` (raw KeyBinding) → `player.input.jumping` 변경
- [x] `feedback_movementInput_vs_isSneaking` 패턴 적용
- [x] L587 (다른 함수) raw key 사용은 Phase G 이후 검토 (본 Phase 범위 외)

### B-8. 발견된 오역/누락 ✅
- B-2: 로컬 변수 잘못 재정의 → sm 필드/메서드 직접 사용으로 1:1 정정 (수정 완료)
- B-3: charge clamp 차이 → `++` 로 1:1 정정 (수정 완료)
- B-7: raw KeyBinding → `player.input.jumping` 정정 (수정 완료)

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL

---

## Phase C. 발사 로직 (`tryJump`) 1:1 정합 (research §C, 원본 L1999-L2136) — ✅ 완료

### C-1. 함수 시그니처 ✅
- [x] 원본 인스턴스 메서드 ↔ 1.21.1 정적 메서드 (player + sm 인자 추가) — 정합

### C-2. type 상수 정의 (15종) ✅
- [x] `SmartMovingConfig.java` L40-L54 — Up=0, ChargeUp=1, Angle=2, HeadUp=3, ..., WallHeadSlide=14 모두 일치

### C-3. `noVertical` 분기 ✅
- [x] 원본 L2002-L2006 ↔ 1.21.1 L162-L170 정합 (WallUpSlide/WallHeadSlide → WallUp/WallHead + noVertical=true)

### C-4. `up` / `head` boolean ✅
- [x] up: 12 type, head: 4 type — 모두 정합

### C-5. exhaustion 분기 ✅
- [x] **SKIP 유지** (easy 모드 = exhaustion 없음, vanilla hunger 위임)

### C-6. potion + 3 factor ✅
- [x] vanilla `JUMP_BOOST` 인스턴스 + horizontalJumpFactor/verticalJumpFactor/jumpChargeFactor 정합

### C-7. `!up` 분기 sqrt 변환 ✅
- [x] head 미진입. 1.21.1 `Math.sqrt` ↔ 원본 `MathHelper.sqrt_float` 정합

### C-8. 초기 motion 식 ✅
- [x] `-0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor` 정합

### C-9. maxHorizontalMotion 산출 ✅
- [x] `horizontalCollision` 매핑 정합 (원본 `isCollidedHorizontally`)

### C-10. vanilla Up 분기 if/else 구조 ✅
- [x] F-1 세션 21 fix 후 정합 (원본 L2047-L2111 if/else 구조 복원)

### C-11. ★ head 각도 재계산 (헤드점프 핵심) ✅
- [x] 원본 L2065-L2079 ↔ 1.21.1 L252-L263 정합 (atan/sqrt/sin/cos + maxH 비율 조정)

### C-12. angle != null 분기 ✅
- [x] `RadiantToAngle = 57.295776F` = 원본 `360F/Whole(2π)` ≈ 57.2957795 정합
- [x] `reset = (WallUp || WallHead)` + `getJumpMoving` 3분기 정합

### C-13. horizontalMotion > 0 스케일 ✅
- [x] sign 처리 + maxH clamp 정합

### C-14. up && !noVertical → motionY + Stats + isSprintJump ✅
- [x] motionY/JUMP stat/isSprintJump 정합

### C-15. exhaustion 누적 ✅
- [x] **SKIP 유지** (C-5 와 동일)

### C-16. ★ head 후처리 + 박스 dim ✅
- [x] 원본 `setHeightOffset(-1)` (height=0.8F) ↔ 1.21.1 `setPoseSmall(SLIDING) + heightOffset=-1F`
- [x] **박스 정합 확인**: SM Mixin (`MixinPlayerEntityClient.java` L140/L146) 가 SLIDING dim 을 `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F)` 로 강제 — **0.6 × 0.8** 매핑 (vanilla 0.6F 가 아님). 슬라이딩/비행/헤드점프 모두 동일 박스. ✅
- [x] 의심영역 #2/#9 (박스 차이) **무효** — 원본과 1:1 정합

### C-17. `sp.isAirBorne / isJumping / onLivingJump` ✅
- [x] `setVelocity` + `isJumping=true`. `isAirBorne` / `onLivingJump` 는 vanilla 자동 처리 (검증 추후)

### C-18. D-18 후처리 ✅ — 🔧 수정 (1:1 정합)
- [x] **fix**: tryJump 끝의 D-18 4줄 (`jumpCharge=0 / headJumpCharge=0 / blockJumpTillButtonRelease=true / jumpPending=false`) 제거
- [x] 호출 측 분리 처리 (원본대로):
  - 차지/헤드 release 분기에서 charge=0 set (이미 호출 측에서 처리 중)
  - `blockJumpTillButtonRelease=true` 는 차징 외 + 잔존 분기에서만 set (이미 호출 측에서 처리 중)
  - `jumpPending=false` 는 handleJumping 시작 (L367) 에서 매 tick reset
- [x] 영향: wall/climb back/angle/slide/water/CreativeFlying 점프 후 `blockJumpTillButtonRelease` 미설정 → 점프 키 hold 중 다음 점프 가능 (원본 동작)

### C-19. 발견된 오역/누락 즉시 수정 ✅
- C-18: tryJump 내부 D-18 4줄 제거로 1:1 정합 (수정 완료)
- 그 외: 모두 정합

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL

---

## Phase D. Config 1:1 정합 (research §D) — ✅ 완료

### D-1. 키 / 기본값 정합 표 ✅
원본 키 → 1.21.1 필드 / 기본값 모두 일치:
- [x] `_headJump` (true) → `headJump = true` (L700)
- [x] `_headJumpControlFactor` (0.2F) → `headJumpControlFactor = 0.2F` (L701)
- [x] `_headJumpChargeMaximum` (10F) → `headJumpChargeMaximum = 10F` (L702)
- [x] `_headFallDamageStartDistance` (values(2F,1F,3F)) → `headFallDamageStartDistance = 2F` (L1202)
- [x] `_headFallDamageFactor` (defaults(2F)) → `headFallDamageFactor = 2F` (L1209)
- [x] `_climbBackHeadJump` (true) → `climbBackHeadJump = true` (L743)
- [x] `_climbBackHeadJumpVerticalFactor` (0.2F) → `climbBackHeadJumpVerticalFactor = 0.2F` (L746)
- [x] `_climbBackHeadJumpHorizontalFactor` (0.3F) → `climbBackHeadJumpHorizontalFactor = 0.3F` (L749)
- [x] `_climbBackHeadJumpHandsOnlyVerticalFactor` (0.8F) → `climbBackHeadJumpHandsOnlyVerticalFactor = 0.8F` (L752)
- [x] `_climbBackHeadJumpHandsOnlyHorizontalFactor` (DecreasingFactor 기본 1F) → `climbBackHeadJumpHandsOnlyHorizontalFactor = 1F` (L755)
- [x] `_wallHeadJump` (true) → `wallHeadJump = true` (L671)
- [x] `_wallHeadJumpVerticalFactor` (0.3F) → `wallHeadJumpVerticalFactor = 0.3F` (L683)
- [x] `_wallHeadJumpHorizontalFactor` (0.15F) → `wallHeadJumpHorizontalFactor = 0.15F` (L687)
- [x] `_wallHeadJumpFallMaximumDistance` (3F) → `wallHeadJumpFallMaximumDistance = 3F` (L675)
- [-] `_climbJumpBackHeadExhaustion` (true) — SKIP
- [-] `_climbJumpBackHeadExhaustionGainFactor` (20F) — SKIP
- [-] `_climbJumpBackHeadExhaustionStopFactor` (80F) — SKIP
- [-] `_wallHeadJumpExhaustion` (true) — SKIP
- [-] `_wallHeadJumpExhaustionGainFactor` (20F) — SKIP
- [-] `_wallHeadJumpExhaustionStopFactor` (80F) — SKIP

### D-2. type 상수 ✅
- [x] HeadUp=3, ClimbBackHead=9, ClimbBackHeadHandsOnly=10, WallHead=12, WallHeadSlide=14 (Phase C-2 와 중복 검증 완료)

### D-3. `getHeadJumpFactor` 식 ★ ✅
- [x] 1.21.1 L235-L241 (원본 1:1):
  ```java
  if (!enabled || !this.headJump) return 1F;
  headJumpCharge = Math.min(headJumpCharge, headJumpChargeMaximum);
  return (headJumpCharge - 1) / (headJumpChargeMaximum - 1);
  ```
- [x] 경계값: charge=1 → 0 / charge=10 → 1 / charge=11 → clamp 후 1 / charge=0 → -1/9 (음수, 호출 측 가드 의존)

### D-4. `getJumpVerticalFactor` 헤드 분기 (누적) ★ ✅
- [x] 1.21.1 L272-L320 4개 누적 패턴 모두 정확 일치:
  - `ClimbBackHead || ClimbBackHeadHandsOnly` → `× climbBackHeadJumpVerticalFactor` (L291-L292)
  - `ClimbBackHeadHandsOnly` → `× climbBackHeadJumpHandsOnlyVerticalFactor` 가산 (L293-L294)
  - `WallUp || WallHead` → `× wallUpJumpVerticalFactor` (L296-L297) ★ 둘 다
  - `WallHead` → `× wallHeadJumpVerticalFactor` 가산 (L298-L299) ★ WallHead 만 추가

### D-5. `getJumpHorizontalFactor` 헤드 분기 (★ vertical 과 다른 점) ✅
- [x] 1.21.1 L362-L407 정합:
  - `ClimbBackHead || ClimbBackHeadHandsOnly` → `× climbBackHeadJumpHorizontalFactor` (L376-L377)
  - `ClimbBackHeadHandsOnly` → `× climbBackHeadJumpHandsOnlyHorizontalFactor` 가산 (L378-L379)
  - `WallUp` → `× wallUpJumpHorizontalFactor` (L381-L382) ★ WallUp 단독
  - `WallHead` → `× wallHeadJumpHorizontalFactor` (L383-L384) ★ WallHead 단독, wallUp 비누적

### D-6. `isJumpingEnabled` head 분기 ✅
- [x] 1.21.1 L119-L151 정합:
  - `ClimbBackHead || ClimbBackHeadHandsOnly → climbBackHeadJump` (L131-L132)
  - `WallHead → wallHeadJump` (L136-L137)

### D-7. exhaustion 3함수 head 분기 ✅
- [x] **결정 완료**: SKIP (easy 모드). 1.21.1 plain pass-through

### D-8. `headFallDamage*` 사용처 ✅
- [x] 1.21.1 `SmartMovingClientState.java` L2041: `handleCrash(player, cfg0.headFallDamageStartDistance, cfg0.headFallDamageFactor)` 정합 (Phase E-5 에서 본격 검증)

### D-9. 직렬화 (config 파일 read/write) ✅
- [x] read (`SmartMovingConfig.java` L1647-L1680) — 헤드 관련 14개 키 모두 정합:
  - L1647: `move.jump.wall.head` → `wallHeadJump`
  - L1649: `move.jump.wall.head.fall.maximum.distance` → `wallHeadJumpFallMaximumDistance`
  - L1652: `move.jump.wall.head.vertical.factor` → `wallHeadJumpVerticalFactor`
  - L1654: `move.jump.wall.head.horizontal.factor` → `wallHeadJumpHorizontalFactor`
  - L1659-L1663: head charge 5개 키
  - L1676-L1680: climb back head 5개 키
- [x] write (L1815-L1848) — 동일 14개 키 양방향 정합

### D-10. 발견된 오역/누락 ✅
- 모든 헤드점프 관련 키 / 함수 / 직렬화 정확히 1:1 정합. **수정 사항 없음.**

---

## Phase E. 매 틱 상태 재평가 1:1 정합 (research §E) — ✅ 완료

### E-1. setIsJumping (vanilla 점프 invoke 차단) ✅ — 🔴 BUG-3 fix
- [x] **BUG 발견**: `MixinClientPlayerEntity.java` L122-L125 단순화 매핑
  - 원본: `(headJumpEnabled && grab && sprint)` 복합 조건 → 1.21.1: `isHeadJumping` 단일 (이미 발사된 상태만)
  - 원본: `(jumpChargeEnabled && wouldIsSneaking && onGround && isStanding)` → 1.21.1: `jumpCharge > 0`
- [x] **fix**: 5-AND 식 부정 1:1 복원 — `(cfg.headJump && grab && sprint) || (cfg.jumpCharge && wouldIsSneaking && onGround && isStanding)` 복합 조건 + 기존 4개 단일 조건 모두 OR

### E-2. wantClimb 가드 `!isHeadJumping` ✅
- [x] 1.21.1 `SmartMovingClientState.java` L1589 정합

### E-3. ★ isHeadJumping 매 틱 5-AND 재평가 ✅
- [x] 1.21.1 L2030-L2036 — 5조건 모두 원본 1:1 정합:
  - `!onGround / !(swim||dive) / !(flying||abilities.flying) / !(isTouchingWater && vy<0) / !isInLava`

### E-4. !isHeadJumping → isAerodynamic = false ✅
- [x] L2039 정합

### E-5. 해제 엣지 handleCrash + restoreFromFlying ✅ — 🔴 BUG-1/BUG-2 fix
- [x] L2046-L2050 호출 흐름 정합 (B-24 + B-N-standup)
- [x] **handleCrash 본체 BUG fix** (ClientState.java L3626-L3635 + Climber.java L1080-L1089 두 복사본):
  - **BUG-1 fix**: 함수 끝 `player.fallDistance = 0F;` 추가 (원본 L2242 누락 fix)
  - **BUG-2 fix**: `>` → `>=` 정정 (원본 L2237 경계값)
  - **부수**: `Stats.FALL_ONE_CM` 갱신 추가 (원본 L2234-L2235 distanceFallenStat)
  - **수정**: damage = `(float) Math.ceil((fallDistance - startDistance) * factor)` (원본 정수 데미지 1:1)
  - step sound 강제 (`distanceClimbedModified`) — 별도 시스템 deferred

### E-6. tryLanding / restoreFromFlying 호출 ✅
- [x] B-N-standup 매핑 (L2050 `standupIfPossible(player, false, true)` 직접 호출)
- [x] 본 헤드점프 영역에서 정합. `tryLanding` 별도 분기 (비행 자동 착지) 는 비행 사이클에서 검증

### E-7. ★ SlideToHeadJumping 전환 ✅
- [x] L2055-L2059 정합 (`SLIDE_TO_HEADJUMPING_FALL_DISTANCE` 상수 + isAerodynamic=true)

### E-8. SlideDown 진입 (`isHeadJumping=false` set 포함) ✅
- [x] L2003-L2020 6-AND 모든 가드 + isHeadJumping=false / isAerodynamic=false set 정합
- [x] sneak rising-edge (`sneakKeyStartPressed`) 정합

### E-9. wouldWantSneak 가드 `!isHeadJumping` ✅
- [x] L1503 정합

### E-10. canWallJumping 가드 `!isHeadJumping` ✅
- [x] `SmartMovingJumper.java` L564 정합

### E-11. 호출 순서 ✅
- [x] 원본: 5-AND → isAerodynamic → handleCrash → standupIfPossible → SlideToHeadJumping → SlideDown 진입
- [x] 1.21.1: SlideDown 진입(L2003) → 5-AND(L2030) → isAerodynamic(L2039) → handleCrash+standup(L2046) → SlideToHeadJumping(L2055)
- [x] **차이점**: SlideDown 진입 위치 (1.21.1 가 1번째). SlideDown 가드 식이 isHeadJumping 무관 → **동작 영향 없음**

### E-12. 발견된 오역/누락 ✅
- BUG-1 (handleCrash fallDistance reset 누락) → 두 복사본 fix
- BUG-2 (handleCrash `>` → `>=`) → 두 복사본 fix
- BUG-3 (setIsJumping 가드 단순화) → 5-AND 식 1:1 복원
- 부수: Stats.FALL_ONE_CM 추가 / damage 정수 캐스팅 fix

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL (incrementStat → increaseStat API 차이 정정)

---

## Phase F. headJumpControlFactor 1:1 정합 (research §F) — ✅ 완료

### F-1. 적용 위치 ✅ — 🔧 수정
- [x] **Mover.handleLand (L230-L235)**: 원본 L702-L706 1:1 정합 (cfg.enabled 가드 명시 추가)
- [x] **climbMotion (L326-L332)**: 원본에 없는 추가 매핑 → **제거**
  - 원본은 등반 분기 (isClimbing && ...) 에 jumpControlFactor / headJumpControlFactor 미적용
  - `wouldWantClimb` 식의 `!isHeadJumping` 가드로 헤드점프 중 등반 진입 차단 → dead code
  - 1:1 정합 + dead code 제거 (주석으로 의도 명시)

### F-2. 적용 식 ✅
- [x] Mover.handleLand: `if (sm.isHeadJumping) speedFactor *= cfg.headJumpControlFactor;` + `else if (cfg.enabled && !onGround && !flying && !sm.isFlying) speedFactor *= cfg.jumpControlFactor;`

### F-3. 적용 가드 (isSliding) ✅
- [x] `MixinLivingEntityClient.java` L143 `if (SmartMovingSlider.handleSliding(player, sm)) { ... }` 가 isSliding 시 sm_travel cancel
- [x] handleLand / climbMotion 분기 도달 안 함 → 원본 `else if (!isSliding)` 진입 가드와 동치

### F-4. 발견된 오역/누락 ✅ — 🔧 수정 완료
- F-1 (a): Mover.handleLand cfg.enabled 가드 누락 (cosmetic) → 명시 추가
- F-1 (b): climbMotion 의 헤드/jump control factor 분기 (dead code, 원본 위반) → 제거

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL

---

## Phase G. WallHeadJump 1:1 정합 (research §G, 원본 `handleWallJumping()` L1946-L1996) — ✅ 완료

### G-1. wantWallJumping / NaN 가드 ✅
- [x] 1.21.1 L617 `if (!sm.wantWallJumping) return;` 정합
- [x] NaN 가드는 `calculateSeparateCollisionAngle` 의 fallback 처리로 동등

### G-2. grab → WallHead/Slide 분기 ✅
- [x] 원본 L1952-L1957 정합 (`wallHeadJumpFallMaximumDistance` 3F 가드)

### G-3. !grab → WallUp/Slide 분기 ✅
- [x] 원본 L1958-L1963 정합

### G-4. jumpAngle 산출 ✅ — 🔧 차이 1 수정
- [x] 원본 L1965-L1975 (jumpMotion 기반 movementAngle)
- [x] **차이 1 fix**: `vel` 기반 `Math.atan2(-vel.x, vel.z)` → `sm.jumpMotionX/Z` 기반 `Math.atan2(-sm.jumpMotionX, sm.jumpMotionZ)` 로 정정 (점프 시점 motion 보존)
- [x] NaN 가드 추가 (1:1)

### G-5. orthogonalTolerance 정렬 ✅ — 🔴 BUG-1 fix
- [x] **BUG-1 fix**: `while>360` + `orthogonalTolerance` 적용을 if/else 분기 외부로 이동 (원본 L1977-L1988 1:1)
- [x] 영향: 이전부터 벽에 닿아있던 경우 (Slide 타입) 90° 정렬 미적용 BUG → 정합

### G-6. tryJump 호출 후처리 ✅ — 🔧 차이 2/3 수정
- [x] **차이 2 fix**: `if (tryJump(...)) { 후처리 }` 구조 1:1 복원 (원본 L1990)
- [x] **차이 3 fix**: `isWallJumping = true` 이중 set 제거 → `if` 분기 안에서만 1회 set
- [x] 후처리 순서: `continueWallJumping = !isHeadJumping` → `horizontalCollision=false` → `setYaw(jumpAngle)` → `bodyYaw=jumpAngle` → `isWallJumping=true` → `fallDistance=0F`

### G-7. onStartWallJump 부수효과 ✅
- [x] inline 매핑 (`isWallJumping=true` + `fallDistance=0F`) — `if (tryJump)` 분기 안 (원본 1:1 위치)
- [x] **prev rotateAngleY 누락은 보류** (애니메이션 영역 — Phase H ClimbBack 회전 보류와 동일 패턴, 사용자 원본 확인 대기)

### G-8. 발견된 오역/누락 ✅
- BUG-1 (orthogonalTolerance Slide 분기 미적용) → 분기 외부로 이동
- 차이 1 (movementAngle vel → jumpMotion) → 정정
- 차이 2 (tryJump 결과 무시) → if 분기 복원
- 차이 3 (isWallJumping 이중 set) → 1회 set
- **차이 4 (bodyYaw set) 보류**: 회전 동기화 의도로 유지 (사용자 결정 대기)
- **차이 5 (prev rotateAngleY) 보류**: 애니메이션 사이클로 deferred

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL

---

## Phase H. ClimbBackHeadJump 1:1 정합 (research §H, 원본 L1055-L1077) — ✅ 완료

### H-1. 점프 키 rising-edge 가드 ✅ — 🔴 BUG-4 fix
- [x] **BUG-4 fix**: `sm.jumpPending` (sm_jump 인터셉트 매 점프 시도 플래그) → `sm.jumpKeyStartPressed` (rising-edge) 정정
- [x] `feedback_keybinding_rising_edge` 패턴 적용

### H-2. handsOnly 결정 ✅ — 🔴 BUG-2 fix
- [x] **BUG-2 fix**: 변수 자체 누락 → `boolean handsOnly = sm.actualFeetClimbType != FeetClimbing.NONE.ordinal();` 추가
- [x] **★ 변수명 함정 보존**: handsOnly==true → 일반 type, handsOnly==false → *HandsOnly type (원본 L1064-L1066 1:1)

### H-3. ★ type 4종 결정 (3중 ternary) ✅ — 🔴 BUG-1 fix
- [x] **BUG-1 fix**: 기존 `useHead` 단일 boolean → 4종 type 결정 매핑 정정
- [x] `cfg.climbJumpBackHead ? grabPressed : !grabPressed` (원본 Options._climbJumpBackHeadOnGrab)
- [x] type: `useHead ? (handsOnly ? CLIMB_BACK_HEAD : CLIMB_BACK_HEAD_HANDS_ONLY) : (handsOnly ? CLIMB_BACK : CLIMB_BACK_HANDS_ONLY)`

### H-4. jumpAngle = rotationYaw + 180F ✅
- [x] `player.getYaw() + 180F` 정합

### H-5. tryJump 후처리 ✅ — 🔴 BUG-3 fix
- [x] **BUG-3 fix**: `tryJump` 미호출 + inline motion 식 → `if (tryJump(...)) { 후처리 }` 구조 1:1 복원
- [x] tryJump 가 D-15 head 분기 시 isHeadJumping/setPoseSmall/heightOffset 처리 + D-11 angle 분기 시 motion 계산 (factor 적용)
- [x] 후처리 순서: `continueWallJumping = !isHeadJumping` → `isClimbing = false` → `setYaw(jumpAngle)` → `bodyYaw=jumpAngle` → `isClimbBackJumping=true`
- [x] inline 동일 식 (vanilla 0.42 jump + 0.3 push) 제거 — type 별 factor 정상 적용

### H-6. onStartClimbBackJump prev rotateAngleY ✅ (사용자 보류 결정 적용)
- [x] **보류 유지**: `prev rotateAngleY += isHeadJumping ? Half : Quarter` (180°/90°) — 애니메이션 사이클 / 사용자 원본 확인 대기
- [x] `isClimbBackJumping = true` set 은 본 사이클에서 매핑 (기능 영역)

### H-7. 발견된 오역/누락 ✅
- BUG-1 (type 4종 미구분) → 3중 ternary 복원
- BUG-2 (handsOnly 변수 누락) → 추가
- BUG-3 (tryJump 미호출, inline motion) → tryJump 호출로 정정 (factor 정상 적용)
- BUG-4 (jumpPending vs jumpKeyStartPressed) → rising-edge 정정
- prev rotateAngleY 보류 (애니메이션)

### 빌드 검증
- [x] `./gradlew compileJava compileClientJava` BUILD SUCCESSFUL

---

## Phase I. standUp / toSlidingOrCrawling 1:1 정합 (research §I) — ✅ 완료 (수정 없음)

### I-1. standUp ✅
- [x] `SmartMovingClientState.standUp` L3402-L3406 — 원본 L2214-L2220 1:1 정합
- [x] `setPosition(y + (1-gap)) + isCrawling=false + isHeadJumping=false + resetHeightOffset()` 흐름

### I-2. ★ toSlidingOrCrawling ✅
- [x] `SmartMovingClientState.toSlidingOrCrawling` L3419-L3437 — 원본 L2222-L2230 1:1 정합
- [x] `move(-gap)` + `if (slide && (grab || wasHeadJumping)) → isCrawling=false; isSliding=true; else → wasCrawling = toCrawling();`
- [x] `isCrawling=false` 추가는 사용자 보고 fix (90° 꺾임 방지, 2026-05-04)

### I-3. resetState ✅
- [x] `SmartMovingClientState.java` L2766 `isHeadJumping = false;` (원본 L2294 1:1)

### I-4. 발견된 오역/누락 ✅
- standUp / toSlidingOrCrawling / resetState 모두 1:1 정합. **수정 사항 없음**.

### 별도 발견 (Phase 범위 외 — 의심 영역으로 추적)
- `SmartMovingJumper.resetHeightOffset` (L99-L138) 이 원본 `resetHeightOffset` (L1681-L1686 단순 reset) 와 다름 — 슬라이딩/크롤링 분기 포함 (mini standupIfPossible)
- 호출 위치: `MixinLivingEntityClient.java` L150 (`if (isOnGround && isHeadJumping)`) — 별도 헤드점프 착지 흐름
- Phase E-5 의 B-N-standup `standupIfPossible(player, false, true)` 호출과 잠재적 중복 처리 위험
- **본 Phase 범위 외** — 헤드점프 착지 시나리오 전체 영향이라 별도 검증 사이클 권장

---

## Phase J. 네트워크 동기화 1:1 정합 (research §J) — ✅ 완료 (수정 없음)

### J-1. self → server pack ✅
- [x] `SmartMovingClientState.java` L3910 `s.isHeadJumping = isHeadJumping;`
- [x] `SmartMovingState.encode` L84 `if (s.isHeadJumping) bits |= 1L << 20;`
- [x] 원본 SmartMovingSelf L3146 (shift 패턴) 과 의미 동등 (1.21.1 명시 bit 위치)
- [x] bit 20 — 다른 bit (isLevitating bit 19, isSliding bit 21) 와 충돌 없음

### J-2. server broadcast → remote unpack ✅
- [x] 1.21.1 remote: `SmartMovingClientState.java` L1124 `isHeadJumping = ((bits >> 20) & 1) != 0;`
- [x] 1.21.1 server: `SmartMovingServer.java` L168 (서버도 자체 unpack — POSE 결정용)
- [x] `SmartMovingState.decode` L116 동일 식

### J-3. 3측 분리 검증 ✅
- [x] **self** (`SmartMovingClientState` pack L3910 + unpack L1124): 매 틱 set/해제 + state pack
- [x] **server** (`SmartMovingServer` L97 필드 + L168 unpack + broadcast): state 보존 + 4분기 POSE 결정 + broadcast
- [x] **remote** (`SmartMovingClientState` L1124 unpack): broadcast 받아 시각/박스 적용

### J-4. SLIDING POSE 자동 dim 적용 ✅
- [x] 클라 (`MixinPlayerEntityClient` L131 + L140/L146): `isHeadJumping || isSliding → SLIDING POSE` + dim 0.6×0.8
- [x] 서버 (`MixinPlayerEntity` L131-L133 + L72-L78): 동일 4분기 + smSmall (isHeadJumping 포함) → dim 0.6×0.8
- [x] 클라/서버 대칭 — `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F)` 동일 강제 (Phase C-16 검증과 일관)

### J-5. server reconcile ✅
- [x] 클라 박스 0.6 × 0.8 ↔ 서버 박스 0.6 × 0.8 → 동일 → **server position correction 발생 X**
- [x] 메모리 `feedback_server_reconcile_box_sync` 패턴 정합

### J-6. 발견된 오역/누락 ✅
- 모든 네트워크 동기화 1:1 정합. **수정 사항 없음.**

---

## Phase K. 채팅 §9 / HUD (research §K, §L) — ✅ 완료 (수정 없음)

### K-1. 채팅 §9 ✅
- [x] 원본 `SmartMovingComm.java` L161 ↔ 1.21.1 `SmartMovingClient.java` L272 — 의미 동등 (§9 수신 시 `cfg.headJump = false`)

### K-2. HUD 차지 게이지 ✅
- [x] 원본 `SmartMovingRender.java` L222-L223 ↔ 1.21.1 `SmartMovingHud.java` L51-L56 — 1:1 정합 (`headJumpChargeMaximum` clamp + `Math.min` 패턴)

### K-3. 발견된 오역/누락 ✅
**없음 — 수정 사항 없음**

---

## Phase L. 통합 검증 — ✅ 빌드 완료, 인게임 테스트 사용자 진행

### L-1. 빌드 / 컴파일 ✅
- [x] `./gradlew build` BUILD SUCCESSFUL (compileJava + compileClientJava + sourcesJar + remapJar + assemble)
- [x] 경고: Gradle deprecation 만 (코드 무관)

### L-2. 디버그 로그 시스템 ✅
- [x] 기존 `SmartMovingClientState.smDebugDumpState` (L3309-L3344) 가 헤드점프 관련 모든 필드 포함:
  - `isHeadJumping`, `heightOffset`, `pose`, `velocity`, `fallDistance`, `onGround`, `grab`, `isFast`, `isJumping`, `isSliding`, `mustCrawl`, dim 등
- [x] 추가 dump 불필요 — 인게임 테스트 시 사용자 보고 BUG 발생 시 `feedback_debug_log_first` 패턴으로 추가 로그 즉시 작성

### L-3. 단위 시나리오 표 (인게임 수동 테스트 — 통합 단계로 deferred 가능)
| # | 시나리오 | 기대 동작 | 결과 |
|---|----------|-----------|------|
| L-3-1 | 평지 sprint + grab + jump tap | 헤드점프 발사, 박스 1m, sliding pose | [ ] |
| L-3-2 | 평지 sprint + grab + jump 길게 누름 | 차지 누적 (HUD 표시), 떼는 순간 발사 | [ ] |
| L-3-3 | 평지 sprint + grab + jump → 착지 후 grab 유지 | 착지 후 자동 슬라이딩 진입 | [ ] |
| L-3-4 | 평지 sprint + grab + jump → 착지 후 grab 뗌 | 착지 후 직립 (slide 미진입) | [ ] |
| L-3-5 | 평지 sprint + grab + jump → 큰 낙하 | 헤드 낙하 데미지 | [ ] |
| L-3-6 | 헤드점프 중 물 진입 | isHeadJumping 해제 (motion.y<0 체크) | [ ] |
| L-3-7 | 헤드점프 중 라바 진입 | isHeadJumping 해제 | [ ] |
| L-3-8 | 헤드점프 중 비행 toggle | isHeadJumping 해제 | [ ] |
| L-3-9 | 슬라이딩 중 0.05F 초과 낙하 | 자동 헤드점프 + isAerodynamic | [ ] |
| L-3-10 | 슬라이딩 중 sneak 떼기 | 슬라이딩 종료 + 크롤 진입 (E-8 분기) | [ ] |
| L-3-11 | 등반 중 grab 떼고 jump (또는 grab 누른 채 jump — 옵션 의존) | ClimbBackHead 발사 (180° 회전) | [ ] |
| L-3-12 | 벽 충돌 중 grab + jump | WallHead 발사 (벽 반사 각도) | [ ] |
| L-3-13 | 벽 충돌 + fall>3F | WallHead 발사 차단 | [ ] |
| L-3-14 | 헤드점프 중 sneak 시도 | sneak 미적용 | [ ] |
| L-3-15 | 헤드점프 중 wall jump 시도 | wall jump 차단 | [ ] |
| L-3-16 | 헤드점프 중 climb 시도 | climb 차단 | [ ] |
| L-3-17 | crawling 중 grab + jump | 헤드점프 미발사 (!isCrawling 가드) | [ ] |
| L-3-18 | 멀티 — 다른 클라이언트가 헤드점프 | remote 박스 SLIDING dim 적용 | [ ] |
| L-3-19 | 채팅 §9 메시지 수신 | cfg.headJump = false → 헤드점프 비활성화 | [ ] |
| L-3-20 | HUD 차지 게이지 | sprint+grab+jump 누름 시 점진 증가 (10틱 max) | [ ] |

### L-4. 회귀 검증 — 다른 완결 시스템 영향 확인
- [ ] 비행 / 낙하 / Angle Jump / 그랩 클라이밍 / 늘어진 덩굴 / ICC / restoreFromFlying / 사다리·덩굴 ICC EXIT / grab sneak hold / crawl 머리 회전 / crawl 게임 재진입 / crawl 클라이밍 / crawl 시스템 / 슬라이딩 시스템
- [ ] 인게임 단위 시나리오 1개씩 빠르게 회귀 확인 (해당 영역 README 참조)

### L-5. 멀티 회귀 (BUG-7 ICC velocity clamp 영향 검증)
- [ ] 멀티에서 헤드점프 self / remote 동기화
- [ ] BUG-7 fix 패턴과의 상호작용 검증

---

## Phase M. 마무리

### M-1. 메모리 업데이트
- [ ] 헤드점프 시스템 완결 시 `project_headjump_complete.md` 작성
- [ ] 발견된 새 패턴 / 함정 → `feedback_*.md` 추가
- [ ] `MEMORY.md` 인덱스 갱신

### M-2. 다음 사이클 준비
- [ ] 헤드점프 애니메이션 (research §M) 사이클 시작 — 별도 research/checklist
- [ ] `MEMORY.md` `project_next_step` 갱신 (다음 작업 = 헤드점프 애니메이션)

### M-3. Git 커밋
- [ ] 본 세션 변경 분 커밋 (CLAUDE.md 컨벤션 따름)
- [ ] 메시지 예: `fix(headjump): 헤드점프 기능 1:1 매핑 정합 — Phase X-Y 영역`

---

# § 진행 상태 / 메모

## 결정 완료 항목
- [x] (Phase C-5 / C-15 / D-1 exhaustion 키 6개 / D-7) 헤드점프 exhaustion **SKIP 유지** (easy 모드 1.21.1 vanilla hunger 위임). 관련 체크 항목은 `[-]` 무관 처리.

## 결정 보류 항목 (사용자 원본 확인 대기)
- [ ] (Phase H-6) `onStartClimbBackJump` prev rotateAngleY 가산 (`isHeadJumping ? Half : Quarter`) — 본 사이클 vs 애니메이션 사이클. 사용자가 원본 확인 후 결정 알림 예정.

## 누적 발견 사항 (감사 중 채움)
- (없음)

## 의심 영역 (research 섹션 §1.21.1 — 점검/누락 후보 10개)
1. SlideToHeadJumpingFallDistance 인라인 vs 상수
2. setPoseSmall 박스 0.6F vs 원본 0.8F
3. continueWallJumping set 순서 race
4. handleCrash + restoreFromFlying + standupIfPossible 호출 순서
5. WallHead vertical 누적 vs horizontal 단독 (D-4/D-5)
6. 차징 조건 isRunning() 매핑 정확성
7. setIsJumping 가드 식 정확성
8. onStartClimbBackJump prev rotateAngleY 보정
9. setPoseSmall 박스 차이 영향 (콜리전/카메라/grip)
10. 채팅 §9 서버 packet 차이
