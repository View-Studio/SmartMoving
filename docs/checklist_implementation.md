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

- [ ] **`supportsCeilingClimbing()` 구현**
  - [ ] 플레이어 머리 위 블록 확인
  - [ ] 설정에 등록된 블록(`TagKey<Block>` 또는 딕셔너리) 매칭
  - [ ] 기본 지원 블록: 사다리, 덩굴, 천장 클라이밍용 태그 블록

- [ ] **천장 감지 로직**
  - [ ] `wantClimbCeiling = grabButton.Pressed && !wantCrawlNotClimb && !sneaking`
  - [ ] `topBlock = supportsCeilingClimbing(pos.up())` 체크
  - [ ] `bottomBlock = supportsCeilingClimbing(pos.up(2))` 체크
  - [ ] `jgap = distance to ceiling block` 계산
  - [ ] 조건: `jgap < 1.9 && !obstacle`

- [ ] **천장 클라이밍 속도 적용**
  - [ ] 갭 크기별 motionY 결정
    - `jgap > 1.2` → `motionY = 0.12D`
    - `jgap > 1.115` → `motionY = 0.08D`
    - 그 이하 → `motionY = 0.04D`
  - [ ] `fallDistance = 0F` (낙하 대미지 방지)
  - [ ] 매 틱 motionY 양수 유지 (중력 상쇄)
  - [ ] 수평 이동: `speedFactor *= ceilingClimbingSpeedFactor`

- [ ] **히트박스 변경**
  - [ ] 천장 클라이밍 진입 시 `heightOffset = -1F` 적용
  - [ ] AABB 상단이 천장 블록에 닿도록 Y 오프셋 보정

- [ ] **탈진 게이트**
  - [ ] 이동 중에만 exhaustion 소모 (정지 시 없음)

- [ ] **해제 조건**
  - [ ] `grabButton.Pressed = false`
  - [ ] 천장 블록 없음
  - [ ] 장애물 (`solidHeight >= pos.y + 0.5`)
  - [ ] exhaustion 초과

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

- [ ] **`AnimationUtil` 유틸 클래스**
  - [ ] `factor(x, x0, x1)` 선형 보간 구현 (research_animation.md 섹션 B)
    - `x0 > x1` (내림): `clamp((x0-x)/(x0-x1), 0, 1)`
    - `x0 <= x1` (오름): `clamp((x-x0)/(x1-x0), 0, 1)`
  - [ ] 각도 상수 정의
    - `WHOLE = 2π`, `HALF = π`, `QUARTER = π/2`
    - `EIGHTH = π/4`, `SIXTEENTH = π/8`
    - `THIRTYTWOTH = π/16`, `SIXTYFOURTH = π/32`

- [ ] **`PlayerEntityModel` Mixin 기반 구조**
  - [ ] `@Mixin(PlayerEntityModel.class)`
  - [ ] `@Inject(method = "setAngles", at = @At("TAIL"))`
  - [ ] 상태 플래그 조회: `player.getAttached(STATE)`
  - [ ] 상태 없으면 vanilla 애니메이션 그대로

- [ ] **커스텀 ModelPart 확장** (research_animation.md 섹션 D)
  - [ ] `bipedOuter` — 루트 본 (전체 Y 회전 + 페이드)
  - [ ] `bipedTorso` — 상체
  - [ ] `bipedBreast` — 가슴 레이어
  - [ ] `bipedRightShoulder`, `bipedLeftShoulder` — 어깨 (팔과 독립)
  - [ ] `bipedPelvic` — 골반 (몸통/다리 분리)
  - [ ] `PlayerEntityModel`에 필드 주입 방법 결정 (Mixin accessor or duck interface)

- [ ] **회전 순서 시스템**
  - [ ] `YZX`, `ZYX`, `XZY`, `YXZ` 적용 방법 구현
  - [ ] `MatrixStack` 수동 회전 순서 적용

- [ ] **스케일링 시스템** (research_animation.md 섹션 E)
  - [ ] `Scale (0)`: `MatrixStack.scale()` 직접 적용
  - [ ] `NoScaleStart (1)`: 상단 오프셋 조정
  - [ ] `NoScaleEnd (2)`: 하단 오프셋 조정
  - [ ] `setArmScales(rightScale, leftScale)` 구현
  - [ ] `setLegScales(rightScale, leftScale)` 구현

- [ ] **3개 모델 동기화 구조**
  - [ ] 메인 모델 (전체 스케일)
  - [ ] 흉갑 갑옷 모델 (`NoScaleStart` 팔, `NoScaleEnd` 다리)
  - [ ] 표준 갑옷 모델 (`NoScaleStart` 팔, `Scale` 다리)
  - [ ] `PlayerEntityRenderer` Mixin 에서 상태 동기화

---

### 3-2. 기어가기 애니메이션
> 참고: `research_animation.md` — 섹션 D-7

- [ ] `isCrawling` 분기 구현
  - [ ] `distance = totalHorizontalDistance × 1.3F`
  - [ ] `walkFactor = factor(speed, 0F, 0.12951545F)`
  - [ ] `standFactor = factor(speed, 0.12951545F, 0F)`
  - [ ] 몸통: `rotateAngleX = QUARTER - THIRTYTWOTH`, `rotationPointY = 3F`
  - [ ] 팔: `rotateAngleX = HALF + EIGHTH`, `rotateAngleY = ±QUARTER`
  - [ ] 팔 Z진동: `cos(distance + HALF) × SIXTYFOURTH × walkFactor ± THIRTYTWOTH`
  - [ ] 다리 X: `cos(distance ± QUARTER) × SIXTYFOURTH + THIRTYTWOTH` × factors
  - [ ] 다리 Z: `(cos(distance - QUARTER) ± 1F) × 0.25F × walkFactor ± THIRTYTWOTH`
  - [ ] 팔/다리 스케일 적용

---

### 3-3. 클라이밍 애니메이션
> 참고: `research_animation.md` — 섹션 D-2

- [ ] `isClimbing` 분기 구현
  - [ ] 손 클라이밍 타입별 파라미터 분기 (MiddleGrab/UpGrab/NoGrab)
  - [ ] 팔 진동: `cos(vertDist × freq + HALF) × vSpeed × distFactor + offset`
  - [ ] 팔 Y: `cos(horizDist × freqSide + QUARTER) × hSpeed × sideFactor + sideOffset`
  - [ ] 다리 진동: 손 공식과 위상차(HALF) 적용
  - [ ] 덩굴 클라이밍: `setArmScales(|cos(armAngleX)|, ...)`, `setLegScales(...)`

- [ ] `isCrawlClimbing` 특수 케이스
  - [ ] `height = smallOverGroundHeight + 0.25F`
  - [ ] `bodyAngleX = acos(height / 0.7F)` (역삼각함수 기하학 계산)
  - [ ] `legAngleX = QUARTER - bodyAngleX`
  - [ ] 몸통/어깨/머리/다리 각도 적용

- [ ] `isClimbJumping` 정적 포즈
  - [ ] 팔: `rotateAngleX = HALF + SIXTEENTH`, Z: `±THIRTYTWOTH`

---

### 3-4. 천장 클라이밍 애니메이션
> 참고: `research_animation.md` — 섹션 D-4

- [ ] `isCeilingClimbing` 분기 구현
  - [ ] `walkFactor = factor(speed, 0F, 0.12951545F)`
  - [ ] `standFactor = factor(speed, 0.12951545F, 0F)`
  - [ ] 팔 X: `(cos(dist) × 0.52F + HALF) × walkFactor + HALF × standFactor`
  - [ ] 팔 X 우: `(cos(dist + HALF) × 0.52F - HALF) × walkFactor - HALF × standFactor`
  - [ ] 다리 X 좌: `-cos(dist) × 0.12F × walkFactor`
  - [ ] 다리 X 우: `-cos(dist + HALF) × 0.32F × walkFactor`
  - [ ] Y 회전 진동: `rotateY = cos(dist) × 0.44F × walkFactor`

---

### 3-5. 슬라이딩 애니메이션
> 참고: `research_animation.md` — 섹션 D-8

- [ ] `isSliding` 분기 구현
  - [ ] 몸통 외전: `rotateAngleX = QUARTER`, `rotationPointY = 5F`
  - [ ] 바디 Y오프셋: `offsetY = -0.4F`, `rotationPointY = +6.5F`
  - [ ] 팔: `rotateAngleX = cos(dist+QUARTER) × SIXTYFOURTH × walkFactor + HALF - SIXTYFOURTH`
  - [ ] 팔 Y: `±QUARTER` 외전
  - [ ] 다리: `rotateAngleZ = ±THIRTYTWOTH`

---

### 3-6. 수영 / 잠수 애니메이션
> 참고: `research_animation.md` — 섹션 D-5, D-6

- [ ] `isSwimming` 분기 구현
  - [ ] 속도 구간 3개 (정지/느린수영/빠른수영) Factor 계산
    - `walkFactor = factor(speed, 0.15679921F, 0.52264464F)`
    - `sneakFactor = min(factor(speed, 0, 0.15679921F), factor(speed, 0.52264464F, 0.15679921F))`
    - `standFactor = factor(speed, 0.15679921F, 0F)`
  - [ ] 몸통: `rotateAngleX = QUARTER - SIXTEENTH × standSneakFactor`
  - [ ] 머리: 회전 순서 YXZ, `rotateAngleY = cos(dist/2 - QUARTER) × walkFactor`
  - [ ] 팔 Z: `±(QUARTER + EIGHTH) ± cos(totalTime × 0.1F) × standSneakFactor × 0.8F`
  - [ ] 팔 X: `(dist × 0.5F) % WHOLE - HALF` 거리 기반
  - [ ] 다리 X: `cos(dist + HALF) × 0.52264464F × walkFactor`
  - [ ] 다리/팔 스케일: 시간 기반 진동

- [ ] `isDiving` 분기 구현
  - [ ] 몸통: `rotateAngleX = QUARTER - currentVerticalAngle`
  - [ ] 다리 Z: `(cos(dist) + 1F) × 0.52264464F × walkFactor + SIXTEENTH × standFactor`
  - [ ] 팔 Z: 진폭 2.5배 (`× 0.52264464F × 2.5F`)

---

### 3-7. 점프 애니메이션
> 참고: `research_animation.md` — 섹션 D-10, D-11

- [ ] `isHeadJumping` 분기
  - [ ] 몸통: `rotateAngleX = QUARTER - currentVerticalAngle`
  - [ ] 머리: `-rotateAngleX / 2F`
  - [ ] `bendFactor = min(factor(vAngle, QUARTER, 0), factor(vAngle, -QUARTER, 0))`
  - [ ] 팔 Z: `HALF - SIXTEENTH + factor(vAngle, QUARTER, -QUARTER) × EIGHTH`
  - [ ] 천장 높이로 팔 Z 제한 (`smallOverGroundHeight / 5F`)

- [ ] `isAerodynamic` (낙하) 분기
  - [ ] 팔 Z: `cos(dist) × EIGHTH ± QUARTER`
  - [ ] 팔 Y: `cos(dist + QUARTER) × EIGHTH`
  - [ ] 다리 X: `cos(dist + HALF + QUARTER) × SIXTEENTH + THIRTYTWOTH`

- [ ] `isWallJumping` 전환 애니메이션 처리

---

### 3-8. 렌더 파이프라인 훅
> 참고: `research_animation.md` — 섹션 F

- [ ] **`PlayerEntityRenderer` Mixin**
  - [ ] `render()` `@Inject` — 상태 플래그 모델에 동기화
  - [ ] 3개 모델 변형 동기화 루프

- [ ] **높이 오프셋 보정** (`renderPlayerAt()` 대응)
  - [ ] 크롤링/슬라이딩 플레이어 Y 오프셋 보정
  - [ ] 원격 플레이어도 동일하게 적용 (멀티플레이어)

- [ ] **몸통 회전 고정** (`rotatePlayer()` 대응)
  - [ ] 클라이밍/비행/수영/슬라이딩 시 `renderYawOffset = forwardRotation`

- [ ] **`isSneaking()` 오버라이드**
  - [ ] 크롤 중 or 차지점프 중 → `isSneaking() = true` (스니크 포즈 애니메이션)

---

## Phase 4 — 네트워킹

### 4-1. 패킷 시스템
> 참고: `research_networking.md` — 섹션 A, B

- [ ] **커스텀 페이로드 등록** (Fabric 1.21.1 `PayloadTypeRegistry`)
  - [ ] `SmartMovingStatePayload` — 64비트 상태 전송
  - [ ] `SmartMovingConfigPayload` — 서버 설정 배포

- [ ] **`SmartMovingStatePayload` 구현**
  - [ ] `Identifier id = Identifier.of("smartmoving", "state")`
  - [ ] `PacketByteBuf` — `entityId (int)` + `state (long)`
  - [ ] `PacketCodec` 등록

- [ ] **64비트 인코딩 구현** (`addToSendQueue` 포팅)
  - [ ] research_networking.md 섹션 B 비트 레이아웃 그대로 구현
  - [ ] 비트 0-3: `actualFeetClimbType`
  - [ ] 비트 4-7: `actualHandsClimbType`
  - [ ] 비트 8-33: 나머지 boolean 플래그 순서대로
  - [ ] `prevPacketState` 비교 → 변경 시에만 전송 (delta compression)

- [ ] **64비트 디코딩 구현** (`processStatePacket` 포팅)
  - [ ] 동일 비트 순서로 역방향 우시프트 (`>>>`) 추출
  - [ ] `isClimbBackJumping` 전환 감지 → 콜백 트리거
  - [ ] `isWallJumping` 전환 감지 → 콜백 트리거

- [ ] **클라이언트 → 서버 전송**
  - [ ] `ClientPlayNetworking.send()` — 상태 변경 시
  - [ ] `ClientTickEvents` 에서 변경 감지

- [ ] **서버 수신 및 relay**
  - [ ] `ServerPlayNetworking.registerGlobalReceiver()`
  - [ ] 히트박스 동기화: `isSmall` 비트 → `ServerPlayerEntity` 높이 변경
  - [ ] 낙하거리 리셋: `isClimbing || isCeilingClimbing || isWallJumping`
  - [ ] `PlayerLookup.tracking(entity)` 로 주변 플레이어에게 relay

---

### 4-2. 원격 플레이어 관리
> 참고: `research_networking.md` — 섹션 C

- [ ] **`SmartMovingRemoteState` 클래스** (원본 `SmartMovingOther` 대응)
  - [ ] 원격 플레이어 상태 필드 (애니메이션용)
  - [ ] `processStatePacket(long state)` — 디코딩 + 플래그 적용
  - [ ] `foundAlive` 플래그 (메모리 누수 방지용)

- [ ] **원격 플레이어 인스턴스 관리**
  - [ ] `Map<UUID, SmartMovingRemoteState>` (EntityId 대신 UUID)
  - [ ] 패킷 수신 시 조회/생성 (on-demand)
  - [ ] `ServerPlayConnectionEvents.DISCONNECT` — 접속 끊김 시 정리
  - [ ] 매 틱 `foundAlive` 체크 → false 인스턴스 제거

- [ ] **원격 플레이어 애니메이션**
  - [ ] `PlayerEntityRenderer` Mixin — 원격 플레이어도 상태 기반 애니메이션 적용
  - [ ] `renderPlayerAt()` 높이 오프셋 원격 플레이어에도 적용

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

- [ ] **아이콘 텍스처 제작**
  - [ ] `assets/smartmoving/textures/gui/icons.png` — 9×9 픽셀 아이콘 그리드
  - [ ] 빈/반/꽉 하트 아이콘 (exhaustion 표시용)
  - [ ] 차지/반차지 아이콘 (점프차지 표시용)

- [ ] **Exhaustion 바 구현**
  - [ ] `HudRenderCallback` 또는 `@Mixin(InGameHud)` 사용
  - [ ] 위치: 화면 우측 하단 (방어구 바 아래)
  - [ ] 수중 시 10px 위로 이동
  - [ ] `maxExhaustionForAction` 기준 하트 개수 계산 + 렌더

- [ ] **점프 차지 바 구현**
  - [ ] 위치: 화면 좌측 하단 (방어구 바 아래)
  - [ ] `jumpCharge`, `headJumpCharge` 중 큰 값 표시
  - [ ] `DrawContext` API 사용 (1.21.1)

---

### 5-2. 설정 시스템
> 참고: `research_networking.md` — 섹션 E

- [ ] **기능 ON/OFF 설정 정의**
  - [ ] `crawlingEnabled`, `climbingEnabled`, `ceilingClimbingEnabled`
  - [ ] `slidingEnabled`, `swimmingEnabled`, `divingEnabled`
  - [ ] `jumpChargeEnabled`, `headJumpEnabled`
  - [ ] `angleJumpSideEnabled`, `angleJumpBackEnabled`
  - [ ] `wallJumpEnabled`

- [ ] **물리 값 설정 정의**
  - [ ] `crawlFactor` (기본 0.35)
  - [ ] `swimSpeedFactor`, `diveSpeedFactor`
  - [ ] `jumpChargeMaximum` (20), `jumpChargeFactor` (1.3)
  - [ ] `headJumpChargeMaximum` (10)
  - [ ] `angleJumpDoubleClickTicks` (3)
  - [ ] `slideControlDegrees` (1.0), `slideSlipperinessFactor`
  - [ ] `fallingDistanceMinimum`
  - [ ] `freeClimbFallMaximumDistance`

- [ ] **탈진 관련 설정**
  - [ ] `climbExhaustionEnabled`
  - [ ] `climbExhaustionStart`, `climbExhaustionStop`
  - [ ] `climbUpExhaustionGain`, `climbDownExhaustionGain`
  - [ ] `ceilingClimbExhaustionEnabled`
  - [ ] `ceilingClimbExhaustionStart`, `ceilingClimbExhaustionStop`, `ceilingClimbExhaustionGain`

- [ ] **Cloth Config API 연동** (또는 자체 구현)
  - [ ] 설정 GUI 화면 등록
  - [ ] 설정 파일 저장/로드 (`config/smartmoving.json`)

---

### 5-3. 소리 (Sound)

- [ ] 클라이밍 소리 등록 및 재생
- [ ] 슬라이딩 소리 등록 및 재생
- [ ] 물 입수/탈출 소리 (`random.splash`)
- [ ] `sounds.json` 정의

---

### 5-4. 마무리 검증

- [ ] **기능별 단독 테스트**
  - [ ] 기어가기 — 1블록 공간 이동, mustCrawl 강제 발동
  - [ ] 클라이밍 — 8방향 표면 탐지, 덩굴/사다리 구분
  - [ ] 천장 클라이밍 — 지지 블록 위에서 활성화
  - [ ] 슬라이딩 — 얼음 위 진입, 방향 조정
  - [ ] 수영/잠수 — 3가지 상태 전환, 3D 이동
  - [ ] 점프 강화 — 차지/헤드/각도/벽 점프 각각 검증

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
