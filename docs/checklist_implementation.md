# SmartMoving 1.21.1 구현 체크리스트

> 리서치 기반: `docs/research_*.md` 전체 참고
> 규칙: 각 항목 완료 후 `[x]` 체크. 미확인 사항은 해당 리서치 파일 업데이트.

---

## Phase 1 — 기반 시스템

### 1-1. 프로젝트 구조 세팅
- [x] `src/main/java/choco/ratel/smartmoving/` 하위 패키지 구조 생성
  - [x] `state/` — 상태 관리 클래스
  - [x] `input/` — 입력 처리 클래스
  - [x] `mixin/` — Mixin 클래스 (client/server 분리)
  - [ ] `physics/` — 이동/충돌 물리 유틸 (Phase 2에서 생성)
  - [ ] `render/` — 렌더링/애니메이션 (Phase 3에서 생성)
  - [ ] `network/` — 패킷 시스템 (Phase 4에서 생성)
  - [ ] `config/` — 설정 시스템 (Phase 5에서 생성)
  - [ ] `util/` — 공용 유틸 (Phase 2에서 생성)
- [x] `smartmoving.mixins.json` 에 서버 Mixin 목록 구성
- [x] `smartmoving.client.mixins.json` 에 클라이언트 Mixin 목록 구성
- [x] `fabric.mod.json` entrypoints 확인 (main, client)
- [x] `Smartmoving.java` (서버 초기화), `SmartmovingClient.java` (클라이언트 초기화) 뼈대 작성

---

### 1-2. 플레이어 상태 시스템
> 참고: `research_player_state.md` — 섹션 A, C

- [x] `SmartMovingState` 클래스 작성 (원본 `SmartMovingSelf` 필드 포팅)

  **Boolean 이동 출력 상태 (19개)**
  - [x] `isCrawling`, `wasCrawling`
  - [x] `isClimbing`, `wasClimbing`
  - [x] `isCrawlClimbing`, `isClimbCrawling`
  - [x] `isSwimming`, `isDiving`, `isDipping`
  - [x] `isSliding`, `isRopeSliding`
  - [x] `isCeilingClimbing`
  - [x] `isHeadJumping`, `isSprintJump`, `isWallJumping`
  - [x] `isHandsVineClimbing`, `isFeetVineClimbing`
  - [x] `isLevitating`, `isAerodynamic`
  - [x] `isFast`, `isSlow`
  - [x] `isGroundSprinting`

  **Boolean 내부 의도 상태 (10개)**
  - [x] `wantClimbUp`, `wantClimbDown`, `wantClimbCeiling`
  - [x] `wantCrawlNotClimb`, `wouldIsSneaking`
  - [x] `isClimbingStill`, `isClimbHolding`
  - [x] `crawlToggled`, `sneakToggled`
  - [x] `blockJumpTillButtonRelease`

  **Integer 카운터/타입 (8개)**
  - [x] `angleJumpType` (0-7)
  - [x] `handsEdgeMeta`, `feetEdgeMeta` (0-3)
  - [x] `leftJumpCount`, `rightJumpCount`, `backJumpCount`, `wallJumpCount` (-1~N)
  - [x] `collidedHorizontallyTickCount`, `updateCounter`

  **Float 물리 값 (9개)**
  - [x] `exhaustion`, `maxExhaustionForAction`, `maxExhaustionToStartAction`
  - [x] `jumpCharge`, `headJumpCharge`
  - [x] `dippingDepth`
  - [x] `horizontalCollisionAngle`
  - [x] `fadingPerspectiveFactor`
  - [x] `heightOffset`

  **이전 프레임 추적**
  - [x] `prevMotionX`, `prevMotionY`, `prevMotionZ`
  - [x] `wasOnGround`

- [x] `SmartMovingState` — `reset()` 메서드 (상태 전체 초기화)
- [x] `SmartMovingState` — `tick()` 메서드 뼈대 (updateEntityActionState 진입점)
- [ ] `SmartMovingState` — `tickPre()` / `tickPost()` 구분 (Phase 2에서 필요 시 추가)

- [x] Fabric `AttachmentType` 등록
  - [x] `Smartmoving.java` 에서 `AttachmentType<SmartMovingState>` 등록
  - [x] 기본값 팩토리: `() -> new SmartMovingState()`
  - [x] `player.getAttachedOrCreate(SmartMovingAttachments.STATE)` 사용

- [x] `PlayerEntity` Mixin — `tick()` `@Inject` → `state.tick()` 호출 (클라이언트 로컬 플레이어만)
- [ ] `PlayerEntity` Mixin — 탑승/수면/크리에이티브 시 상태 비활성화 체크 (Phase 2에서 구현)
- [ ] 상태 우선순위 충돌 해소 로직 구현 (Phase 2에서 구현)
  - [ ] `Flying > Climbing > Swimming > Crawling > Standing` 순서
  - [ ] Exhaustion 초과 시 상태 진입 차단 (해제는 하지 않음)

---

### 1-3. 입력 시스템
> 참고: `research_input.md` — 섹션 A, B, C, D, E

- [x] `SmartMovingButton` 클래스 작성 (원본 `Button` 포팅)
  - [x] `boolean pressed` — 현재 프레임 상태
  - [x] `boolean wasPressed` — 이전 프레임 상태
  - [x] `boolean startPressed` — false→true 전환 (1프레임)
  - [x] `boolean stopPressed` — true→false 전환 (1프레임)
  - [x] `update(boolean pressed)` 메서드
  - [ ] `update(KeyBinding binding)` 오버로드 (InputHandler에서 직접 처리)
  - [ ] `inGameHasFocus` 체크 (Phase 2에서 필요 시 추가)
  - [ ] UI 열려있을 때 `allowUserInput` 체크 (Phase 2에서 필요 시 추가)

- [x] KeyBinding 4개 등록 (Fabric `KeyBindingHelper`)
  - [x] `keyBindGrab` — 기본값: Left Ctrl (GLFW_KEY_LEFT_CONTROL)
  - [x] `keyBindConfigToggle` — 기본값: F9
  - [x] `keyBindSpeedIncrease` — 기본값: O
  - [x] `keyBindSpeedDecrease` — 기본값: I
  - [x] 카테고리: `"key.categories.smartmoving"` (번역 키 추가)

- [x] `SmartMovingState` 에 Button 8개 필드 추가
  - [x] `forwardButton`, `backButton`, `leftButton`, `rightButton`
  - [x] `jumpButton`, `sprintButton`, `sneakButton`, `grabButton`

- [x] `ClientTickEvents.START_CLIENT_TICK` 에서 매 틱 폴링 (InputHandler)
  - [x] `forwardButton.update(client.options.forwardKey.isPressed())`
  - [x] `backButton.update(client.options.backKey.isPressed())`
  - [x] `leftButton.update(client.options.leftKey.isPressed())`
  - [x] `rightButton.update(client.options.rightKey.isPressed())`
  - [x] `jumpButton.update(client.options.jumpKey.isPressed())`
  - [x] `sprintButton.update(client.options.sprintKey.isPressed())`
  - [x] `sneakButton.update(client.options.sneakKey.isPressed())`
  - [x] `grabButton.update(SmartMovingKeys.GRAB.isPressed())`

- [ ] 크롤 토글 로직 구현 (Phase 2-1에서 구현)
  - [ ] `willStartCrawl` 감지 → `crawlToggled = true`
  - [ ] `ignoreNextStopSneakButtonPressed` 플래그 처리
  - [ ] `willStopCrawl` 감지 → `crawlToggled = false`
  - [ ] `isCrawlToggleEnabled()` 설정 체크

- [ ] 스니크 토글 로직 구현 (Phase 2-1에서 구현)
  - [ ] `sneakToggled` 플래그 관리

---

### 1-4. 히트박스 시스템
> 참고: `research_movement.md` — 섹션 C, `research_player_state.md` — 섹션 D

- [x] `EntityDimensions` 동적 변경 Mixin 구현
  - [x] `@Mixin(LivingEntity.class)` — `getBaseDimensions(EntityPose pose)` `@Inject`
  - [x] 상태 기반 분기: `isCrawling || isSliding || isCeilingClimbing → height 0.8F`
  - [x] 기본 상태: `height 1.8F` (기존 vanilla 동작 유지)

- [x] 눈 높이 보정
  - [x] `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.48F)` — 1.21.1 방식으로 통합 처리
  - [x] 크롤/슬라이딩 시: `eyeHeight = 0.48F` (원본: 0.8 × 0.6)

- [ ] 렌더 위치 오프셋 보정 (멀티플레이어 높이 보정) (Phase 3에서 구현)
  - [ ] `heightOffset` 값을 렌더 Y 좌표에 반영
  - [ ] `@Mixin(PlayerEntityRenderer.class)` — `render()` 에서 Y 보정 적용

- [ ] 히트박스 변경이 서버에도 전파되는지 확인 (`ServerPlayerEntity`) (Phase 4에서 검증)
  - [ ] 서버 측 `getDimensions()` Mixin 동일하게 적용

---

## Phase 2 — 이동 물리

### 2-1. 기어가기 (Crawling)
> 참고: `research_crawling.md` 전체, `research_movement.md` — 섹션 C

- [x] **천장 체크 (`mustCrawl`) 구현**
  - [x] `BlockUtil.mustCrawl()` — 1.8H 박스 충돌 체크로 구현
  - [ ] 정밀 솔리드 경계 탐색 (`getMinBlockSolidAbove` 등) — Phase 2 후속 개선 예정

- [x] **크롤 진입 조건 구현** (research_crawling.md 섹션 A)
  - [x] `canCrawl`: `!flying && !swimming && !diving && !climbing && fallDist < MAX`
  - [x] `wantCrawl`: 홀드 모드 구현 (토글은 Phase 5)
  - [x] `isCrawling = canCrawl && (wantCrawl || mustCrawl)` 최종 결정

- [ ] **`toCrawling()` 토글 진입 메서드** (Phase 5 설정에서 구현)

- [x] **`standupIfPossible()` 기본 구현** (mustCrawl=false 시 isCrawling=false)
  - [ ] `move()` 기반 정밀 스탠드업 애니메이션 (Phase 3에서 보완)

- [x] **크롤 속도 적용**
  - [x] `LivingEntity.travel()` `@ModifyVariable` — CRAWL_SPEED_FACTOR=0.35 적용

- [x] **크롤-클라이밍 연계 (`wantCrawlNotClimb`)** 플래그 설정

- [x] **`isSlow` 플래그 설정** (크롤링 중 true)

- [x] **매 틱 갱신** — `CrawlingHandler.update()` in `SmartMovingState.tick()`

---

### 2-2. 클라이밍 (Free Climbing)
> 참고: `research_climbing.md` — 섹션 A~E, `research_movement.md` — 섹션 D

- [ ] **Orientation 8방향 seekClimbGap 시스템** (Phase 2 후속 개선 예정 — 현재 horizontalCollision 사용)

- [ ] **`ClimbGap` 데이터 클래스** (Phase 4 네트워킹 시 필요)

- [x] **`FeetClimbing` enum 구현** — None~FastUp 7값 구현

- [x] **`HandsClimbing` enum 구현** — None~FastUp 6값 구현

- [x] **클라이밍 진입 조건**
  - [x] `grabButton.Pressed` + `horizontalCollision || climbable block`
  - [x] `fallDistance < MAX`
  - [x] `!isHeadJumping && !wantCrawlNotClimb`
  - [ ] exhaustion 게이트 (Phase 5)

- [x] **클라이밍 수직 속도 적용** — travel() before move() 주입
  - [x] `wantClimbUp` → `FAST_UP_MOTION = 0.20D`
  - [x] `wantClimbDown` → `SINK_DOWN_MOTION = -0.05D`
  - [x] 기본 → `0.0D` (호버)
  - [x] `fallDistance = 0F`

- [x] **`LivingEntity.isClimbing()` Mixin** — `isClimbing || isCeilingClimbing` 시 true

- [x] **덩굴 분리 추적** — `isHandsVineClimbing`, `isFeetVineClimbing`, `isRopeSliding`

- [ ] **탈진 게이트** (Phase 5)
- [ ] **걷기 소리 방지** (Phase 5)

---

### 2-3. 천장 클라이밍 (Ceiling Climbing)
> 참고: `research_climbing.md` — 섹션 F, `research_movement.md` — 섹션 D

- [x] **`supportsCeilingClimbing()` 구현**
  - [x] 플레이어 머리 위 블록 확인 (`headPos = blockPos.up()`, `aboveHead = blockPos.up().up()`)
  - [x] 기본 지원 블록: `BlockTags.CLIMBABLE` (사다리, 덩굴 포함) — Phase 5에서 커스텀 태그로 확장

- [x] **천장 감지 로직**
  - [x] `wantClimbCeiling = grabButton.pressed && !wantCrawlNotClimb && !sneakButton.pressed`
  - [x] `topBlock = supportsCeilingClimbing(pos.up())` 체크
  - [x] `bottomBlock = supportsCeilingClimbing(pos.up(2))` 체크
  - [x] `jgap = 1 - jd + (bottomSupport ? 1 : 0)` 계산
  - [x] 조건: `jgap < 1.9 && solidHeight >= playerY + 0.5`

- [x] **천장 클라이밍 속도 적용**
  - [x] 갭 크기별 motionY 결정
    - `jgap > 1.2` → `motionY = 0.12D`
    - `jgap > 1.115` → `motionY = 0.08D`
    - 그 이하 → `motionY = 0.04D`
  - [x] `fallDistance = 0F` (낙하 대미지 방지)
  - [x] 수평 이동: `CEILING_SPEED_FACTOR = 0.4F` — `@ModifyVariable`로 적용

- [ ] **히트박스 변경**
  - [ ] 천장 클라이밍 진입 시 `heightOffset = -1F` 적용 (Phase 3 렌더링 시 보완)
  - [ ] AABB 상단이 천장 블록에 닿도록 Y 오프셋 보정

- [ ] **탈진 게이트** (Phase 5)

- [x] **해제 조건**
  - [x] `grabButton.pressed = false`
  - [x] 천장 블록 없음 (`topSupport && bottomSupport` 모두 false)
  - [x] 장애물 (`solidHeight < playerY + 0.5`)
  - [ ] exhaustion 초과 (Phase 5)

---

### 2-4. 슬라이딩 (Sliding)
> 참고: `research_sliding.md` 전체

- [x] **슬라이딩 진입 조건**
  - [x] `grabButton.Pressed && isGroundSprinting && sneakButton.startPressed`
  - [x] `!isCrawling && !isDipping`

- [x] **마찰 공식 구현** — `getSlipperiness()` 기반 `horizontalDamping` 계산

- [x] **방향 조정 (Steering)** — strafing 입력으로 속도 벡터 방향 회전

- [x] **히트박스 변경** — `isSmall()` 에 `isSliding` 포함 (0.6×0.8)

- [x] **해제 조건** — `!sneakButton.pressed || hSpeedSq < threshold`

- [x] **로프 슬라이딩** 기본 플래그 설정

---

### 2-5. 수영 / 잠수 강화 (Swimming / Diving)
> 참고: `research_swimming.md` 전체

- [x] **수위 감지 구현** — `FluidState.getHeight()` 기반 최대 수위 탐색 (SwimmingHandler)

- [x] **3가지 수중 상태 전환**
  - [x] `playerSwimOffset = waterBorder - playerY - 0.1625`
  - [x] `< 1.4` → isDipping, `1.4~1.9` → isSwimming, `≥1.9` → isDiving

- [x] **상태별 감쇠 적용** — `travel()` before move() 주입
  - [x] isDipping: X/Z×0.80, Y×0.83 + 탈출 점프
  - [x] isSwimming: 전체×0.85 + 부력 그라디언트
  - [x] isDiving: 전체×0.83 + 3D 방향

- [x] **부력 그라디언트** — 7단계 offset 기반 motionY 조정값 구현

- [x] **잠수 3D 방향 이동** — pitch 기반 수직 속도 계산

- [x] **물 탈출 점프** — `WATER_EXIT_MOTION_Y = 0.30000001192092896D`

- [ ] **`reverseHandleMaterialAcceleration()`** — 물 흐름 저항 역적용 (Phase 5)
- [ ] **소리 재생** (Phase 5-3)

---

### 2-6. 점프 시스템 (Jump System)
> 참고: `research_jumping.md` 전체, `research_movement.md` — 섹션 F

- [x] **`LivingEntity.jump()` Mixin 인터셉트** — PlayerEntityJumpMixin (클라이언트)

- [x] **`tryJump(type, ..., angle)` 구현** (JumpHandler)
  - [x] 바닐라 Up: `motionY = 0.41999998688697815D` (포션 적용)
  - [x] 기본 공식: `-0.078 + 0.498 × vertFactor × chargeFactor`
  - [x] 각도 기반 수평 벡터 (ANGLE 타입)
  - [ ] exhaustion 체크/소모 (Phase 5)
  - [ ] 수평 속도 상한 (Phase 5)

- [x] **점프 물약 연동** — `StatusEffects.JUMP_BOOST` 확인 + `(amp+1)×0.2F`

- [x] **차지 점프 (ChargeUp)** — 매 틱 jumpCharge++, 해제 시 tryJump(CHARGE_UP)
  - [x] `getJumpChargeFactor()`: `1F + (charge/20F) × 0.3F`

- [x] **헤드 점프 (HeadUp)** — grabButton + sprinting + jump 충전, totalMotion 보존 각도 변환

- [x] **8방향 각도 점프 (Angle)** — 더블탭 감지 (leftJumpCount / rightJumpCount / backJumpCount)

- [ ] **벽 점프 (Wall Jump)** — horizontalCollisionAngle 추적 필요 (Phase 4에서 구현)

- [ ] **탈진 게이트** (Phase 5)

---

## Phase 3 — 애니메이션

### 3-1. 애니메이션 인프라
> 참고: `research_animation.md` — 섹션 B, C, D, E

- [x] **`AnimationUtil` 유틸 클래스**
  - [x] `factor(x, x0, x1)` 선형 보간 구현 (research_animation.md 섹션 B)
    - `x0 > x1` (내림): `clamp((x0-x)/(x0-x1), 0, 1)`
    - `x0 <= x1` (오름): `clamp((x-x0)/(x1-x0), 0, 1)`
  - [x] 각도 상수 정의
    - `WHOLE = 2π`, `HALF = π`, `QUARTER = π/2`
    - `EIGHTH = π/4`, `SIXTEENTH = π/8`
    - `THIRTYTWOTH = π/16`, `SIXTYFOURTH = π/32`

- [x] **`PlayerEntityModel` Mixin 기반 구조**
  - [x] `@Mixin(PlayerEntityModel.class)`
  - [x] `@Inject(method = "setAngles(LivingEntity;FFFFF)V", at = @At("TAIL"))` — LivingEntity 브릿지 타겟팅
  - [x] 상태 플래그 조회: `player.getAttached(STATE)`
  - [x] 상태 없으면 vanilla 애니메이션 그대로

- [ ] **커스텀 ModelPart 확장** (research_animation.md 섹션 D) — Phase 3 후속
  - [ ] `bipedOuter` — 루트 본 (전체 Y 회전 + 페이드)
  - [ ] `bipedTorso` — 상체
  - [ ] `bipedBreast` — 가슴 레이어
  - [ ] `bipedRightShoulder`, `bipedLeftShoulder` — 어깨 (팔과 독립)
  - [ ] `bipedPelvic` — 골반 (몸통/다리 분리)
  - [ ] `PlayerEntityModel`에 필드 주입 방법 결정 (Mixin accessor or duck interface)

- [ ] **회전 순서 시스템** (Phase 3 후속)
  - [ ] `YZX`, `ZYX`, `XZY`, `YXZ` 적용 방법 구현
  - [ ] `MatrixStack` 수동 회전 순서 적용

- [ ] **스케일링 시스템** (Phase 3 후속)
  - [ ] `Scale (0)`: `MatrixStack.scale()` 직접 적용
  - [ ] `setArmScales(rightScale, leftScale)` 구현
  - [ ] `setLegScales(rightScale, leftScale)` 구현

- [ ] **3개 모델 동기화 구조** (Phase 3 후속)
  - [ ] `PlayerEntityRenderer` Mixin 에서 상태 동기화

---

### 3-2. 기어가기 애니메이션
> 참고: `research_animation.md` — 섹션 D-7

- [x] `isCrawling` 분기 구현 (PlayerEntityModelMixin.applyCrawlingAngles)
  - [x] `walkFactor = factor(speed, 0F, 0.12951545F)`
  - [x] `standFactor = factor(speed, 0.12951545F, 0F)`
  - [x] 몸통: `pitch = QUARTER - THIRTYTWOTH`, `pivotY = 3F`
  - [x] 팔: `pitch = HALF + EIGHTH`, `yaw = ±QUARTER`
  - [x] 팔 Z진동: `cos(dist + HALF) × SIXTYFOURTH × walkFactor ± THIRTYTWOTH`
  - [x] 다리 X: `cos(dist ± QUARTER) × SIXTYFOURTH × walkFactor ± THIRTYTWOTH`
  - [x] 다리 Z: `(cos(dist - QUARTER) ± 1F) × 0.25F × walkFactor ± THIRTYTWOTH`
  - [ ] 팔/다리 스케일 적용 (Phase 3 후속 — 커스텀 ModelPart 필요)

---

### 3-3. 클라이밍 애니메이션
> 참고: `research_animation.md` — 섹션 D-2

- [x] `isClimbing` 기본 분기 구현 (PlayerEntityModelMixin.applyClimbingAngles)
  - [x] 팔 진동: `cos(dist + HALF) × 0.52F × walkFactor - QUARTER` (좌우 위상 반전)
  - [x] 다리 진동: 팔과 위상 반전 적용
  - [ ] 손 클라이밍 타입별 파라미터 분기 (MiddleGrab/UpGrab/NoGrab) — Phase 3 후속
  - [ ] 덩굴 클라이밍 스케일 보정 — Phase 3 후속 (커스텀 ModelPart 필요)

- [ ] `isCrawlClimbing` 특수 케이스 — Phase 3 후속
- [ ] `isClimbJumping` 정적 포즈 — Phase 3 후속

---

### 3-4. 천장 클라이밍 애니메이션
> 참고: `research_animation.md` — 섹션 D-4

- [x] `isCeilingClimbing` 분기 구현 (PlayerEntityModelMixin.applyCeilingClimbingAngles)
  - [x] `walkFactor = factor(speed, 0F, 0.12951545F)`, `standFactor = ...`
  - [x] 팔 X: `(cos(dist) × 0.52F + HALF) × walkFactor + HALF × standFactor`
  - [x] 팔 X 우: `(cos(dist + HALF) × 0.52F - HALF) × walkFactor - HALF × standFactor`
  - [x] 다리 X 좌: `-cos(dist) × 0.12F × walkFactor`
  - [x] 다리 X 우: `-cos(dist + HALF) × 0.32F × walkFactor`
  - [x] Y 회전 진동: `body.yaw = cos(dist) × 0.44F × walkFactor`

---

### 3-5. 슬라이딩 애니메이션
> 참고: `research_animation.md` — 섹션 D-8

- [x] `isSliding` 분기 구현 (PlayerEntityModelMixin.applySlidingAngles)
  - [x] 몸통: `pitch = QUARTER`, `pivotY = 5F`
  - [x] 팔: `pitch = cos(dist+QUARTER) × SIXTYFOURTH × walkFactor + HALF - SIXTYFOURTH`
  - [x] 팔 Y: `±QUARTER` 외전
  - [x] 다리: `roll = ±THIRTYTWOTH`

---

### 3-6. 수영 / 잠수 애니메이션
> 참고: `research_animation.md` — 섹션 D-5, D-6

- [x] `isSwimming` 기본 분기 구현 (PlayerEntityModelMixin.applySwimmingAngles)
  - [x] `walkFactor = factor(speed, 0.15679921F, 0.52264464F)`, `standFactor = ...`
  - [x] 몸통: `pitch = -QUARTER × walkFactor`
  - [x] 팔 X: cos 진동 기반
  - [x] 다리 X: `cos(dist ± HALF) × 0.3F × walkFactor`
  - [ ] 팔 Z 진동 (totalTime 기반) — Phase 3 후속
  - [ ] 다리/팔 스케일 시간 기반 진동 — Phase 3 후속

- [x] `isDiving` 기본 분기 구현 (PlayerEntityModelMixin.applyDivingAngles)
  - [x] 몸통: `pitch = -HALF` (수직 잠수)
  - [x] 팔: `pitch = HALF` (앞으로 완전히 뻗음)
  - [x] 다리: cos 스트로크 동작
  - [ ] 현재 수직 각도 기반 동적 몸통 각도 — Phase 3 후속

---

### 3-7. 점프 애니메이션
> 참고: `research_animation.md` — 섹션 D-10, D-11

- [x] `isHeadJumping` 분기 (PlayerEntityModelMixin.applyHeadJumpingAngles)
  - [x] 몸통: `pitch = vAngle`, 머리: `-vAngle × 0.5F`
  - [x] `bendFactor = min(factor(vAngle, QUARTER, 0), factor(vAngle, -QUARTER, 0))`
  - [x] 팔 pitch: `HALF - SIXTEENTH + factor(vAngle, QUARTER, -QUARTER) × EIGHTH`
  - [ ] 천장 높이 기반 팔 제한 — Phase 3 후속

- [ ] `isAerodynamic` (낙하) 분기 — Phase 3 후속
- [ ] `isWallJumping` 전환 애니메이션 — Phase 4 (WallJump 구현 후)

---

### 3-8. 렌더 파이프라인 훅
> 참고: `research_animation.md` — 섹션 F

- [x] **`PlayerEntityRenderer` Mixin** (PlayerEntityRendererMixin)
  - [x] `getPositionOffset()` RETURN 주입 — `heightOffset` Y 보정 적용

- [ ] **몸통 회전 고정** (`rotatePlayer()` 대응) — Phase 3 후속
  - [ ] 클라이밍/비행/수영/슬라이딩 시 `renderYawOffset = forwardRotation`

- [ ] **`isSneaking()` 오버라이드** — Phase 3 후속
  - [ ] 크롤 중 or 차지점프 중 → `isSneaking() = true`

- [ ] **원격 플레이어 높이 오프셋** — Phase 4 (네트워킹 이후)

---

## Phase 4 — 네트워킹

### 4-1. 패킷 시스템
> 참고: `research_networking.md` — 섹션 A, B

- [x] **커스텀 페이로드 등록** (Fabric 1.21.1 `PayloadTypeRegistry`)
  - [x] `SmartMovingStatePayload` — 64비트 상태 전송 (`playC2S` + `playS2C`)
  - [ ] `SmartMovingConfigPayload` — 서버 설정 배포 (Phase 5)

- [x] **`SmartMovingStatePayload` 구현**
  - [x] `CustomPayload.Id<SmartMovingStatePayload>` 등록
  - [x] `PacketCodec.tuple(INTEGER + VAR_LONG)` — entityId + state
  - [x] `PacketCodec` 등록

- [x] **64비트 인코딩 구현** (`StateEncoder.encode()`)
  - [x] 비트 0-3: `feetClimbingType`, 비트 4-7: `handsClimbingType`
  - [x] 비트 8-33: boolean 플래그 순서대로 (research_networking.md 섹션 B)
  - [x] `prevPacketState` 비교 → 변경 시에만 전송 (delta compression)

- [x] **64비트 디코딩 구현** (`StateEncoder.decode()`)
  - [x] 동일 비트 순서로 우시프트 추출
  - [ ] `isClimbBackJumping` / `isWallJumping` 전환 콜백 (Phase 4 후속)

- [x] **클라이언트 → 서버 전송**
  - [x] `ClientPlayNetworking.send()` — 상태 변경 시 (`SmartMovingClientNetworking`)
  - [x] `ClientTickEvents.END_CLIENT_TICK` 에서 변경 감지

- [x] **서버 수신 및 relay** (`SmartMovingServerNetworking`)
  - [x] `ServerPlayNetworking.registerGlobalReceiver()`
  - [x] 서버 측 상태 갱신 (`StateEncoder.decode`)
  - [x] 낙하거리 리셋: `isClimbing || isCeilingClimbing || isWallJumping`
  - [x] `PlayerLookup.tracking(sender)` 로 주변 플레이어에게 relay
  - [ ] 히트박스 동기화 서버 측 Mixin (Phase 4-3에서 구현)

---

### 4-2. 원격 플레이어 관리
> 참고: `research_networking.md` — 섹션 C

- [x] **`RemotePlayerManager` 클래스** (원본 `SmartMovingOther` 대응)
  - [x] `applyRemoteState(player, encodedState)` — StateEncoder.decode() 래퍼
  - [x] `lastUpdateTick` Map으로 접속 해제 감지 지원
  - [x] `onPlayerLeave(uuid)` — 정리 메서드

- [x] **원격 플레이어 인스턴스 관리**
  - [x] AttachmentType으로 상태 직접 관리 (별도 Map 불필요)
  - [x] `ClientPlayConnectionEvents.DISCONNECT` — prevPacketState 초기화

- [x] **원격 플레이어 애니메이션**
  - [x] `PlayerEntityModelMixin` — 원격 플레이어도 동일 Mixin 적용됨 (AttachmentType 공유)
  - [x] `PlayerEntityRendererMixin.getPositionOffset()` — 원격 플레이어 Y 오프셋도 적용됨

---

### 4-3. 서버 설정 배포
> 참고: `research_networking.md` — 섹션 D, E

- [ ] **접속 시 설정 전송**
  - [ ] `ServerPlayConnectionEvents.JOIN` — 서버 설정 전송
  - [ ] 설정 키-값 배열 직렬화

- [ ] **클라이언트 설정 수신**
  - [ ] `ClientPlayNetworking.registerGlobalReceiver()`
  - [ ] 수신된 설정으로 `ServerConfig` 갱신
  - [ ] 활성 설정 `Config = ServerConfig` 교체

---

## Phase 5 — HUD, 설정, 마무리

### 5-1. HUD 렌더링
> 참고: `research_animation.md` — 섹션 G

- [ ] **아이콘 텍스처 제작** — Phase 5 후속 (현재 DrawContext fill로 대체)

- [x] **Exhaustion 바 구현** (SmartMovingHud)
  - [x] `HudRenderCallback.EVENT` 사용
  - [x] 위치: 화면 우측 하단
  - [x] `maxExhaustionForAction` 기준 DrawContext.fill() 렌더
  - [ ] 수중 Y 오프셋, 하트 아이콘 — Phase 5 후속

- [x] **점프 차지 바 구현** (SmartMovingHud)
  - [x] 위치: 화면 좌측 하단
  - [x] `jumpCharge` (하늘색), `headJumpCharge` (분홍색) 바 분리 표시
  - [x] `DrawContext.fill()` + `RenderTickCounter` API 사용

---

### 5-2. 설정 시스템
> 참고: `research_networking.md` — 섹션 E

- [x] **기능 ON/OFF 설정 정의** (`SmartMovingConfig`)
  - [x] `crawlingEnabled`, `climbingEnabled`, `ceilingClimbingEnabled`
  - [x] `slidingEnabled`, `swimmingEnabled`, `divingEnabled`
  - [x] `jumpChargeEnabled`, `headJumpEnabled`
  - [x] `angleJumpSideEnabled`, `angleJumpBackEnabled`
  - [x] `wallJumpEnabled`

- [x] **물리 값 설정 정의** (`SmartMovingConfig`)
  - [x] `crawlFactor` (기본 0.35F)
  - [x] `swimSpeedFactor`, `diveSpeedFactor`
  - [x] `jumpChargeMaximum` (20F), `jumpChargeFactor` (1.3F)
  - [x] `headJumpChargeMaximum` (10F)
  - [x] `angleJumpDoubleClickTicks` (3)
  - [x] `slideControlDegrees`, `slideSlipperinessFactor`
  - [x] `fallingDistanceMinimum`, `freeClimbFallMaximumDistance`

- [x] **탈진 관련 설정** (`SmartMovingConfig`)
  - [x] `climbExhaustionEnabled`, `climbExhaustionStart/Stop`
  - [x] `climbUpExhaustionGain`, `climbDownExhaustionGain`
  - [x] `ceilingClimbExhaustionEnabled`, `ceilingClimb...`

- [x] **설정 파일 저장/로드** (`ConfigManager`)
  - [x] Gson 기반 JSON 파일 `config/smartmoving.json`
  - [ ] Cloth Config API GUI — Phase 5 후속 (현재 JSON 직접 편집)

---

### 5-3. 소리 (Sound)

- [x] 클라이밍 소리 등록 및 재생 (SmartMovingSounds.CLIMB, 14틱 주기)
- [x] 천장 클라이밍 소리 등록 및 재생 (SmartMovingSounds.CEILING_CLIMB)
- [x] 슬라이딩 소리 등록 및 재생 (SmartMovingSounds.SLIDE)
- [x] `sounds.json` 정의 (ladder.step, player.swim 바닐라 이벤트 참조)
- [ ] 물 입수/탈출 소리 — Phase 5 후속

---

---

## Phase 6 — 버그 수정 및 누락 기능 구현
> 참고: `docs/research_comparison.md` 전체

---

### 6-1. 클라이밍 홀드 추락 버그 수정 (B-2)
> `LivingEntityTravelMixin.java`

- [x] `wantClimbUp / wantClimbDown` 모두 false일 때 `motionY = 0.0D` → `HOLD_MOTION = 0.08D`로 수정
  - `ClimbingHandler`에 `HOLD_MOTION = 0.08D` 상수 추가 (이미 주석에 있음, 실제 적용 안 됨)
  - TravelMixin isClimbing 분기에서 else 케이스에 `ClimbingHandler.HOLD_MOTION` 사용

---

### 6-2. heightOffset 미설정 수정 (B-1)
> `CrawlingHandler.java`, `SlidingHandler.java`, `CeilingClimbingHandler.java`, `SmartMovingState.java`

- [x] `SmartMovingState.tick()` 마지막에 `heightOffset = isSmall() ? -0.5F : 0F` 한 줄 처리
- [x] `PlayerEntityRendererMixin`에서 heightOffset 적용 확인

---

### 6-3. 각도 점프 더블탭 감지 연결 (B-3, M-3)
> `JumpHandler.java`, `SmartMovingState.java`

- [x] `JumpHandler.update()`에 더블탭 감지 로직 추가
  - `state.leftButton.startPressed` → `leftJumpCount` 처리
    - `leftJumpCount > 0` → 발동 (`leftJumpCount = -1`)
    - `leftJumpCount == 0` → 카운터 시작 (`leftJumpCount = angleJumpDoubleClickTicks`)
  - `state.rightButton.startPressed` → `rightJumpCount` 동일 처리
  - `state.backButton.startPressed` → `backJumpCount` 동일 처리
- [ ] 매 틱 카운터 감소 처리
  - `if (leftJumpCount > 0) leftJumpCount--` 등
- [ ] `leftJumpCount < 0` 시 실제 각도 점프 트리거
  - `leftJumpCount = 0`으로 리셋 후 `tryAngleJump(LEFT)` 호출
  - 마찬가지로 `rightJumpCount`, `backJumpCount`
- [ ] `angleJumpSideEnabled`, `angleJumpBackEnabled` 설정 체크
- [ ] 점프 중이거나 지상이 아닐 때 카운터 리셋

---

### 6-4. 각도 점프 물리 구현 (M-3 계속)
> `JumpHandler.java`

- [x] `tryAngleJump(direction)` 메서드 구현
  - **LEFT**: `angle = (player.getYaw() + 270) % 360`
  - **RIGHT**: `angle = (player.getYaw() + 90) % 360`
  - **BACK**: `angle = (player.getYaw() + 180) % 360`
- [ ] 각도 → 수평 벡터 변환
  - `angleRad = Math.toRadians(angle)`
  - `motionX = -Math.sin(angleRad) × horizontalFactor`
  - `motionZ = Math.cos(angleRad) × horizontalFactor`
- [ ] 수직 속도: 기본 Up 점프와 동일 (`vertFactor = 1.0F`)
- [ ] `state.angleJumpType` 설정 (1=left, 2=right, 3=back 등 — StateEncoder 기준)
- [ ] `state.isHeadJumping = false`, `blockJumpTillButtonRelease = true` 설정

---

### 6-5. 헤드점프 수평→수직 속도 재분배 (B-4)
> `JumpHandler.java`

- [x] `tryJump(HEAD_UP)` 내부에 속도 재분배 로직 수정 (`normalAngle + headFactor * (π/2 - normalAngle)`)
  - `hMag = sqrt(player.getVelocity().x² + player.getVelocity().z²)`
  - `totalMotion = sqrt(verticalMotion² + hMag²)`
  - `if (hMag > 1e-6)`:
    - `normalAngle = atan2(verticalMotion, hMag)`
    - `headFactor` 계산 (headJumpCharge 기반, 충전 많을수록 더 수직)
    - `newAngle = normalAngle + headFactor × (QUARTER - normalAngle)`
    - `newVertical = totalMotion × sin(newAngle)`
    - `newHorizontal = totalMotion × cos(newAngle)`
    - `ratio = newHorizontal / hMag`
    - `motionX × ratio`, `motionZ × ratio`로 수평 속도 조정

---

### 6-6. 벽 점프 구현 (M-1)
> `JumpHandler.java`, `SmartMovingState.java`

- [x] `tryWallJump()` 메서드 구현
  - 조건: `!isOnGround && horizontalCollision && grabButton.pressed && jumpButton.startPressed`
  - 수직 속도: 기본 점프와 동일
  - 수평 속도: 현재 수평 속도 반대 방향으로 × 0.3
  - `state.isWallJumping = true`
  - `state.fallDistance = 0F`
- [x] `wallJumpEnabled` 설정 체크

---

### 6-7. 클라이밍 점프 구현 (M-2)
> `JumpHandler.java`, `ClimbingHandler.java`

- [x] 클라이밍 중 점프 트리거 감지
  - `isClimbing && jumpButton.startPressed && !wantClimbUp`
- [x] 클라이밍 점프 구현
  - `isClimbing = false` (클라이밍 해제)
  - `motionY = VANILLA_JUMP_Y × jumpPotionFactor`
  - `state.fallDistance = 0F`
- [x] 클라이밍 뒤로 점프
  - 조건: `isClimbing && backButton.pressed && jumpButton.startPressed`
  - 뒤 방향(yaw+180) 수평 속도 × 0.4 추가

---

### 6-8. 크롤 토글 모드 구현 (M-5)
> `CrawlingHandler.java`

- [x] `crawlToggled` 플래그 처리 추가
  - `grabButton.startPressed && sneakButton.pressed && isOnGround()` → `crawlToggled = !crawlToggled`
  - `wantCrawl = wantCrawl || crawlToggled`
- [x] 토글 해제 조건
  - 점프하면 `crawlToggled = false`

---

### 6-9. 탈진 시스템 활성화 (M-4)
> `ClimbingHandler.java`, `CeilingClimbingHandler.java`, `SmartMovingState.java`

- [x] `ClimbingHandler.update()`에 탈진 증가 로직 추가
  - `isClimbing && wantClimbUp` → `exhaustion += climbUpExhaustionGain`
  - `isClimbing && wantClimbDown` → `exhaustion += climbDownExhaustionGain`
  - 지상이면 → `exhaustion = max(0, exhaustion - 0.002)`
- [x] `CeilingClimbingHandler.update()`에 탈진 증가 로직 추가
  - `isCeilingClimbing` → `exhaustion += ceilingClimbExhaustionGain`
- [x] `maxExhaustionForAction`, `maxExhaustionToStartAction` 갱신
- [x] 탈진 초과 시 클라이밍 강제 해제

---

## Phase 7 — 마무리 검증

- [ ] **기능별 단독 테스트**
  - [ ] 기어가기 — 1블록 공간 이동, mustCrawl 강제 발동, 토글 모드
  - [ ] 클라이밍 — 벽/사다리/덩굴, 홀드, 위/아래, 클라이밍 점프
  - [ ] 천장 클라이밍 — 지지 블록 위에서 활성화, heightOffset 보정
  - [ ] 슬라이딩 — 얼음 위 진입, 방향 조정
  - [ ] 수영/잠수 — 3가지 상태 전환, 3D 이동
  - [ ] 점프 강화 — 차지/헤드(재분배)/각도(더블탭)/벽 점프 각각 검증
  - [ ] 탈진 — 클라이밍 중 탈진 증가, 한계 도달 시 차단

- [ ] **멀티플레이어 테스트**
  - [ ] 원격 플레이어 애니메이션 동기화
  - [ ] 히트박스 동기화 (서버 측 크롤 히트박스)
  - [ ] 접속/해제 시 인스턴스 정리 확인

- [ ] **엣지 케이스 테스트**
  - [ ] 크롤 중 물 진입
  - [ ] 클라이밍 중 탈진 초과
  - [ ] 슬라이딩 중 장애물 충돌
  - [ ] 헤드점프 중 천장 충돌
  - [ ] 벽 점프 연속 발동

- [ ] **성능 테스트**
  - [ ] 매 틱 상태 계산 오버헤드 확인
  - [ ] 패킷 전송 빈도 (delta compression 효과)
  - [ ] 원격 플레이어 인스턴스 메모리 누수 없음 확인

- [ ] `CLAUDE.md` 업데이트 (구현 완료 내용 반영)
- [ ] 최종 빌드 성공 확인 (`./gradlew build`)
