# 인게임 통합테스트 버그 — 세션 35 (2026-04-26)

> **컨텍스트**: 포커스 #1 (애니메이션) Phase R + Phase B + 회귀 감사 통과 후 사용자
> 인게임 통합테스트 결과. 발견된 버그 7건. 포커스 #1 단독 해결 불가 — 다른 포커스
> (#2/#2.5/#2.6/#2.7) 의 입력 상태/동기화 정확성에도 의존. 각 버그별 영향 포커스 +
> 의심 지점 + 리서치 필요 파일 정리.

---

## 진단 원칙

사용자 보고 ("애니메이션이 잘못 나옴" / "기능 자체가 안 됨" / "원상태로 복귀 안 됨") 를
다음 4 분류로 식별:

1. **상태 진입 실패** (#2 / #2.5 / #2.6 / #2.7) — SM 분기 자체 활성화 실패
2. **상태 유지 실패** (#2 / #2.7 BBox/POSE) — 진입 후 매 틱 false → true 진동
3. **상태 종료 실패** (#2 / #2.7) — 종료 조건 미충족 → 잔여 상태로 다음 동작 영향
4. **애니메이션 정합 (#1)** — 위 3 중 어느 하나 정상 작동 후, sm_setAngles 결과 시각 차이

→ #1 은 마지막 단계. 입력 상태가 정확해야 의미 있음. 본 버그 대부분 = 상태 진입/유지/
종료 단계 의심.

---

## BUG-1. 비행 시 몸 고정 + 움직일 수 없음 + 애니메이션 고정/원본 차이

**증상**:
- SM enabled 상태에서 Creative 비행 진입 즉시 몸 고정 (이동 불가)
- 비행 애니메이션 고정 (원본 1.7.10 비행 자세와 다름)

**연관 버그**: **BUG-8 (SM 비행 움직임 시스템 미작동) — 동일 원인 가능성 매우 높음**. SmartMovingFlyer.handleFlying() 가 가로챘는데 vanilla travel() 와 충돌하면 이동 차단 + 애니메이션 0 입력.

**영향 포커스**:
- **#2 / #2.5** (Jumper Factor 인프라 / Creative flying speed 처리) — 이동 자체 차단
- **#1** (sm_animateFlying 자세) — 애니메이션 차이

**의심 지점**:
1. **`flyingCreative` 분기** (`MixinPlayerEntityModelClient.java` L129) — sm_animateFlying 호출 조건. 진입 시 다른 분기 처리가 이동을 차단할 가능성.
2. **B-7 입력값 교체** (세션 32) — flying L576-L578 = `sm.stats.totalDistance/currentSpeed` 사용. SmartStatistics.calculate 가 비행 중 갱신되지 않으면 distance=0/speed=0 → 애니메이션 고정. **확인 필요**: `MixinEntityClient.java` L80 `sm.stats.calculate(...)` 호출이 비행 중에도 실행되는지.
3. **이동 자체 차단** — Mixin 이 player.move() 또는 ClientPlayerEntity tick 에서 비행 중 이동 처리 차단? `MixinClientPlayerEntity` / `SmartMovingClientState` tick 진입 로직 검토.
4. **B-15 isLevitating 우선 처리** (세션 29) — flying 분기 진입 전에 isLevitating 분기로 빠지는 가능성. flying 시 isLevitating false 보장?
5. **🔥 BUG-8 SmartMovingFlyer 충돌** — `SmartMovingFlyer.handleFlying()` (이미 부분 이식 / `MixinLivingEntityClient.java` L149 호출) 가 `ci.cancel()` 후 SM 비행 물리 처리 시도. 이 경로가 vanilla travel() 와 충돌 또는 sm.isFlying 갱신 부정확 시 이동 0 결과.

**리서치 필요 파일**:
- 원본: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` — flying 진입/유지/종료 로직 (`startFlying`/`updateFlying`/`stopFlying` 또는 등가)
- 원본: `SmartMovingModel.java` L474-L520 (isFlying 분기 본체)
- 1.21.1: `MixinClientPlayerEntity.java` (tick 진입) + `SmartMovingClientState.java` (isFlying 갱신)
- 1.21.1: `MixinEntityClient.java` L80 (stats.calculate 호출 조건)
- 1.21.1: `SmartMovingFlyer.java` (이미 부분 이식 / handleFlying + moveFlying)
- 1.21.1: `MixinLivingEntityClient.java` L149 (handleFlying 호출 조건)

**우선순위**: 🔴 매우 높음 (이동 자체 차단 — 게임플레이 결정적). **BUG-8 과 함께 진단 권장**.

**해결 (세션 37 AI 완결 / 인게임 검증 대기)**:

원인 진단 = 의심 지점 5 (BUG-8 SmartMovingFlyer 충돌) 가 핵심. 원본 SmartMovingSelf L624 `sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ)` 매핑 누락:

- `sm_travel_client` 가 handleFlying true 시 `ci.cancel()` → vanilla travel() 본체 작동 안 함 → vanilla `move()` 호출 안 됨
- `SmartMovingFlyer.handleFlying` 가 motion 계산 + `setVelocity` 만 하고 **실제 `player.move()` 호출 누락** → motion 적용 안 됨 = **비행 시 몸 고정 (BUG-1) + SM 비행 시스템 미작동 (BUG-8)**

수정 (`SmartMovingFlyer.java`):
- import `net.minecraft.entity.MovementType` 추가
- `moveFlying` 호출 직후 `player.move(MovementType.SELF, motion)` 추가
- 0.91F 감쇠는 move() 후 적용 (원본 L626-L628 동일 순서)

회귀 안전성: SM disabled 시 sm_travel_client return (BUG-11 가드) 으로 handleFlying 도달 안 함 → 영향 없음. SM enabled 시 vanilla move() 처리 (collision + 위치 갱신) 정상 → 비행 motion 적용.

빌드 검증 — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

**세션 37 AI 완결 / 인게임 검증 대기**.

---

## BUG-2. 그랩 클라이밍 + 사다리 등반 + 머리 위 레더 잡기 — 기능 + 애니메이션 모두 안 됨

**증상**:
- 그랩(Ctrl) + 블록 클라이밍 = 작동 안 함
- 사다리 등반 (레더류 블록 위로 이동) = 작동 안 함
- 머리 위 레더류 잡고 이동 (UpGrab 자세) = 작동 안 함
- 모든 기능 자체 작동 안 함 + 애니메이션도 안 나옴

**영향 포커스**:
- **#2** (스마트무빙 상태 이상) — climbing 상태 진입 자체 실패
- **#2.5** (Jumper Factor 인프라) — climbing/grab 처리
- **#3** (상태 전환 조건) — climbing 진입 조건
- **#4** (키 커맨드 조합) — Ctrl 키 처리
- **#1** (sm_animateClimbing 자세 — 부수적, 위 진입 정상 후 의미 있음)

**의심 지점**:
1. **grab 키 (LCONTROL/LeftControl)** — `SmartMovingKeys.grab` 등록은 정상 (`SmartMovingKeys.java` L21/L27-L32). 키 입력 처리가 안 되는지 또는 grab 상태가 SmartMovingClientState 에 반영 안 되는지.
2. **`isClimbing`/`isCrawlClimbing` 갱신** — `SmartMovingClientState.isClimbing` 이 매 틱 false 유지 → sm_setAngles 의 climbing 분기 진입 안 됨. 갱신 위치 = MixinClientPlayerEntity 또는 SmartMovingSelf 등가 tick.
3. **사다리 블록 감지** — vanilla 의 ladder 충돌 감지 (`Entity.isClimbing()`) vs SM 의 ladder 감지 분리. SM 이 자체 감지 로직 가지면 미이식 가능성.
4. **handsClimbType / feetClimbType ordinal** — `SmartMovingClientState.actualHandsClimbType` 갱신 안 되면 sm_animateClimbing 분기에서 모두 NoGrab 처리.

**리서치 필요 파일**:
- 원본: `SmartMovingSelf.java` — `updateClimbing` / `isClimbing` / `handsClimbType` 갱신 로직 전체
- 원본: `SmartMoving/.../moving/Orientation.java` (클라이밍 방향·gap 계산)
- 원본: `HandsClimbing.java` / `FeetClimbing.java` / `ClimbGap.java` (enum + 결정 로직)
- 1.21.1: `SmartMovingClientState.java` — `isClimbing` / `actualHandsClimbType` / `isHandsVineClimbing` 등 필드 갱신 위치 grep
- 1.21.1: `MixinClientPlayerEntity.java` 또는 별도 tick Mixin — climbing 갱신 호출
- 1.21.1: vanilla `Entity.isClimbing()` (ladder/vine 자동 감지) 와의 통합 여부

**우선순위**: 🔴 매우 높음 (SM 핵심 기능 — climbing 전체 작동 안 함)

---

## BUG-3. 엎드리기 (crawl) — 진입 안 됨 + 몸 위아래 진동 + 애니메이션 망가짐 + 원상태 복귀 안 됨

**증상**:
- 엎드리기 키 (또는 트리거) → crawl 자세로 안 변함
- 대신 몸이 위아래로 계속 진동 (false → true → false 토글 의심)
- 애니메이션 망가짐 (sm_animateCrawling 의 결과가 시각적으로 깨짐)
- 종료 후 원상태 (standing) 복귀 실패

**영향 포커스**:
- **#2 / #2.5** (Jumper Factor — crawl 진입 조건)
- **#2.7** (BBox/POSE/EyeHeight 서버 sync) — crawl pose 동기화. POSE 미동기화 시 vanilla 가 다른 pose 로 강제 → 진동.
- **#3** (전환 조건) — crawl 진입 / 종료 조건 정확성
- **#1** (sm_animateCrawling — 입력 상태 정확 후 의미)
- **#4** (키 커맨드 — crawl 트리거 키 조합)

**의심 지점**:
1. **POSE 동기화 (#2.7)** — crawl 진입 시 vanilla EntityPose 가 SWIMMING 또는 CROUCHING 으로 강제. POSE 패킷 동기화 불완전 시 매 틱 vanilla 가 STANDING 으로 reset → SM 이 다시 CRAWL 시도 = **위아래 진동의 원인 1**.
2. **BBox/EyeHeight 동기화** — crawl 시 BBox 가 1×0.6×1 (낮음). vanilla 가 매 틱 STANDING BBox 로 reset → SM 진입 시도 → 진동 = **원인 2**.
3. **isCrawling 종료 조건** — SmartMovingClientState.isCrawling 이 한 번 true 후 false 로 돌아오는 조건이 명확하지 않으면 잔여 상태 유지 → 원상태 복귀 실패.
4. **B-12 (세션 26) head.pivotZ + body.pivotY** — 본 보강은 isCrawling 진입 후만 적용. 진입 자체 실패 시 영향 없음.
5. **sm_getPositionOffset crawl Y -scale*0.125** (이미 이식) — 진입 후 적용. 진동 원인 아님.

**리서치 필요 파일**:
- 원본: `SmartMovingSelf.java` — `updateCrawling` / `isCrawling` / `mustCrawl` 갱신
- 원본: `SmartMoving/.../moving/SmartMovingBase.java` — BBox/AABB 헬퍼 (`getMaxPlayerSolidBetween` 등)
- 1.21.1: `focus_02_7_bbox_server_sync.md` 의 POSE/BBox 동기화 상태 (Phase H 22/23 = 96%)
- 1.21.1: `MixinClientPlayerEntity.java` (POSE 진입 분기) + `MixinEntity.java`/`MixinPlayerEntityClient.java` (BBox)
- 1.21.1: vanilla `LivingEntity.updatePose()` — vanilla 의 POSE 자동 결정 로직 vs SM 분리 처리
- 1.21.1: `SmartMovingClientState.java` — `isCrawling` 갱신 위치

**우선순위**: 🔴 매우 높음 (진동 = 시각적 + 게임플레이 둘 다 영향)

---

## BUG-4. 다이빙 (앞+그랩+스프린트+점프) — 애니메이션 망가짐 + 원상태 복귀 안 됨

**증상**:
- 다이빙 트리거 (W + Ctrl + Sprint + Space) → 애니메이션 망가짐 (시각적 깨짐)
- 종료 후 원상태 복귀 실패
- 기능적 이동은 추후 테스트 필요 (본 보고 시점에서는 이동 자체는 가능 여부 불명)

**영향 포커스**:
- **#4** (키 커맨드 조합 — 4 키 조합 트리거)
- **#2** (isDiving 상태 진입/유지/종료)
- **#1** (sm_animateDiving 자세 + B-6 입력값 교체)
- **#2.7** (POSE — diving = SWIMMING pose 또는 별도)

**의심 지점**:
1. **4 키 조합 트리거** (#4) — `focus_04_key_combos.md` 진행 중 (8/10 = 80%). 다이빙 트리거 8 채팅 옵션 결정 대기 항목 중 하나일 가능성.
2. **isDiving 종료 조건** — BUG-3 의 crawl 과 유사 패턴. 한 번 true 후 false 안 됨 → 잔여 상태.
3. **B-6 (세션 32) input 교체** — diving L440-L442 = `sm.stats.totalDistance/currentSpeed`. 다이빙 중 stats.calculate 가 비정상이면 애니메이션 깨짐.
4. **sm_setupTransforms isDiving 분기** (`MixinPlayerEntityRenderer.java` L225-L238) — tiltAngle 계산 (Levitate / Jump / 일반 3 분기). 분기 결정 부정확 시 자세 망가짐.
5. **B-13 같은 패턴** — body.offsetY 부재로 vanilla 단일 모델 보정 한계 (다이빙은 #16-19 미해당이지만 유사 케이스 가능).

**리서치 필요 파일**:
- 원본: `SmartMovingSelf.java` — `isDive` / `mustDive` / `startDive` 갱신
- 원본: `SmartMovingModel.java` L363-L390 (isDive 분기 본체)
- 1.21.1: `focus_04_key_combos.md` 의 다이빙 트리거 키 조합 처리
- 1.21.1: `MixinPlayerEntityRenderer.java` L225-L238 (sm_setupTransforms isDiving)
- 1.21.1: `SmartMovingClientState.java` — `isDiving` 종료 조건

**우선순위**: 🟠 높음 (애니메이션 + 종료 실패)

---

## BUG-5. 수영 — 애니메이션 전혀 안 됨 + 수면 위아래 진동 + 뚝뚝뚝 움직임

**증상**:
- 수면에서 수영 시 sm_animateSwimming 결과 시각적으로 안 나옴 (또는 깨짐)
- 수면에서 몸이 계속 위아래 진동 (Y 좌표 토글 의심)
- 뚝뚝뚝 움직임 (lerp/보간 부재 의심)

**영향 포커스**:
- **#2.6** (Lava Liquid Border) — 액체 경계 처리. 수면 = 물 표면 경계. 경계 판정 부정확 시 isSwimming false→true 진동.
- **#2.7** (BBox/POSE/EyeHeight) — SWIMMING POSE 동기화
- **#1** (sm_animateSwimming + B-10 head 자세 + B-11 body.yaw)
- **#2** (isSwimming_sm 상태 진입/유지)

**의심 지점**:
1. **수면 경계 판정** (#2.6) — `focus_02_6_lava_liquid_border.md` 진행 중 (90%, E-2/E-3 진행). 액체 경계 판정 = 물 표면 ±1 픽셀 진동 시 isSwimming 토글. **위아래 진동의 강력한 원인**.
2. **SWIMMING POSE 동기화** (#2.7) — vanilla 가 isSwimming → POSE.SWIMMING 자동 변환. SM 의 isSwimming_sm 과 vanilla isSwimming 의 충돌. 매 틱 vanilla 가 reset → 진동.
3. **swim entity tick 진입** — SmartStatistics.calculate 가 수영 중 매 틱 호출되지 않거나 부정확한 좌표 사용 → 뚝뚝뚝 움직임 (보간 부재).
4. **B-10/B-11 (세션 26-27) 자세** — 진입 정상 후만 의미. 진입 자체 토글이면 본 보강 효과 없음.
5. **leaningPitch 강제 0** (`MixinPlayerEntityModelClient` L88) — SM enabled 시 leaningPitch=0 강제. vanilla 의 SWIMMING POSE → leaningPitch>0 자동 변환과 충돌.

**리서치 필요 파일**:
- 원본: `SmartMovingSelf.java` — `updateSwimming` / `isSwimming` / `swimBorderUp` (수면 경계 판정)
- 원본: `SmartMoving/.../moving/SmartMovingBase.java` — `getLiquidBorder` (액체 경계 판정 본체)
- 원본: `SmartMovingModel.java` L317-L362 (isSwim 분기 본체)
- 1.21.1: `focus_02_6_lava_liquid_border.md` 전체 (Phase A/B/C/D 19/21 = 90%)
- 1.21.1: `SmartMovingClientState.java` — `isInLiquid` / `computeSwimBorderValues` / `isSwimming_sm` 갱신
- 1.21.1: `MixinClientPlayerEntity.java` 또는 등가 — 매 틱 액체 판정 + isSwimming_sm 진입
- 1.21.1: vanilla `LivingEntity.isInSwimmingPose()` / `LivingEntity.updatePose()` — vanilla SWIMMING POSE 자동 처리

**우선순위**: 🔴 매우 높음 (수영 = SM 핵심 기능 / 진동 = 시각 + 게임플레이)

---

## BUG-6. I/O 키 (속도 인크리즈/디크리즈) — 임시 비활성화 요청

**증상**: 사용자 명시 — "잠깐 비활성화 해놔야될 듯. 나중에 다시 활성화 가능하니 위치 기록".

**영향 포커스**: **#6** (increase/decrease — ✅ 완료된 작업)

**비활성화 작업** (세션 35):
- **위치**: `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` L894-L910
- **변경**: `userSpeedEnabled` 변수 강제 false 설정 (DEFER 마커 + 원래 조건 주석 보존)
- **재활성화 방법**: `userSpeedEnabled = false;` 라인 제거 + 원래 조건 주석 복원

**리서치 필요**: 없음 (재활성화는 단순)

**우선순위**: 🟢 낮음 (사용자 임시 비활성화 요청 / 나중에 복원)

---

## BUG-8. SM enabled 비행 — 비행 움직임 시스템 자체 미작동 (원본과 다름)

**증상** (사용자 보고):
- SM enabled 시 비행은 원본 (1.7.10) 에서 vanilla 비행과 다른 자체 시스템 사용
- 1.21.1 = 그 시스템이 "번역 안 되어 있는 것 같음" — 실제 비행 동작이 vanilla 와 동일 (= disabled 와 같음)
- 즉 SM enabled vs disabled 비행 차이가 보이지 않음

**현재 이식 상태 분석**:
- ✅ `SmartMovingFlyer.java` (130 라인) — `handleAlternativeFlying()` 본체 + `moveFlying()` 정규화 공식 모두 1:1 이식 완료
- ✅ `MixinLivingEntityClient.java` L149 — `handleFlying` 호출 분기 존재
- ❌ **호출되지 않거나 / 호출되어도 효과 없거나 / vanilla 처리에 덮여 씀** — 원인 진단 필요

**SM 비행 시스템의 원본 특징** (SmartMovingFlyer 코드에서 확인):
1. **sneak/jump 수직 보정**: `motionY ± 0.15D` + `moveUpward ± 0.98F` (강한 수직 입력)
2. **moveFlying 정규화**: `sqrt(sqrt(x²+z²) + y²)` 비표준 정규화 (수평+수직 균형)
3. **HorizontalAirDamping**: `0.91F` 매 틱 감쇠 (vanilla 와 다른 계수)
4. **flyControlVertical**: pitch 기반 수직 보정 (treeDimensional=true 시) — 마우스 위/아래 보면 자동 상승/하강
5. **flyingSpeedFactor 배수**: `Config._flyingSpeedFactor.value` (vanilla 0.05F 기본 외 추가 배수)
6. **flyWhileOnGround**: 지면 충돌 시 자동 비행 종료 (옵션)

**영향 포커스**:
- **#2 / #2.5** (Jumper Factor — 비행 진입/유지) — 진입 정확성
- **BUG-1 동일 원인 가능성** — 비행 처리 가로챘는데 결과 0 → 이동 차단 = BUG-1 의 "몸 고정"
- **#1** (sm_animateFlying — 가시 효과) — B-7 입력값 (sm.stats.totalDistance/currentSpeed) 비행 중 갱신되어야 정상 애니메이션

**의심 지점**:
1. **`MixinLivingEntityClient.java` L149 호출 조건** — `handleFlying` 가 어떤 메서드 어떤 위치에서 inject 되는지. travel() / tick() / move() 중 잘못된 위치 inject 시 작동 안 함.
2. **`sm.isFlying` 갱신 위치** — `SmartMovingClientState.isFlying` 이 매 틱 어디서 갱신되는지. 갱신 안 되면 `handleFlying` 가 즉시 return false.
3. **`cfg.fly` 옵션 기본값** — `SmartMovingConfig.fly` 가 false 면 진입 안 됨. 기본값 + 사용자 설정 검증.
4. **vanilla `LivingEntity.travel()` 와의 충돌** — vanilla 의 비행 처리 (`Abilities.flying` 시 별도 분기) 가 sm_handleFlying 호출 후/전에 다시 덮어 씀? → 효과 0.
5. **CallbackInfo cancel 부재** — handleFlying 가 true 반환해도 호출 측에서 ci.cancel() 안 하면 vanilla 처리 계속. MixinLivingEntityClient L149 본문 확인 필요.
6. **Mover.getCombinedSpeedFactor 의존** — `flyingSpeedFactor` 계산이 `combinedFactor` 에 의존 (B-6 세션 26). combinedFactor 가 비행 시 0 또는 부정확하면 flyingSpeed=0 → 이동 0.

**리서치 필요 파일**:

원본 (라인별 read 필수):
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`:
  - L80 `if(sp.capabilities.isFlying && !Config.isFlyingEnabled())` — 비행 가드
  - **L602-L631 `handleAlternativeFlying`** — SM 비행 본체
  - L633-L663 `handleLand` — 비행 외 처리 (handleAlternativeFlying false 일 때)
  - L1803-L1831 비행 진입/종료 처리 (flyWhileOnGround / wasCapabilitiesIsFlying)
  - L2198-L2199 비행 종료 처리
  - L2320 `boolean isLevitating = sp.capabilities.isFlying && !isFlying;` — Levitate 분기
  - L2404 `if(esp.capabilities.isFlying && (Config.isFlyingEnabled() || Config.isLevitateSmallEnabled()))` — 비행+Levitate 통합 처리
- `SmartMoving/.../moving/SmartMovingBase.java`:
  - `moveFlying()` 본체 (L56-L93 — SmartMovingFlyer.moveFlying 의 원본)
  - `HorizontalAirDamping` 상수 정의
- `SmartMoving/.../moving/config/SmartMovingOptions.java`:
  - L68-L71 `_flyCloseToGround` / `_flyWhileOnGround` / `_flyControlVertical` — 비행 옵션
- `SmartMoving/.../moving/config/SmartMovingConfig.java`:
  - `_flyingSpeedFactor` / `_runFactorLevitate` / `_sprintFactorLevitate` 등 비행 속도 배수

1.21.1 (현재 이식 상태):
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingFlyer.java` (130 라인 — handleFlying + moveFlying 1:1 이식 완료)
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` L149 (호출 조건 — read 필수)
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — isFlying 필드 + 갱신 위치 grep
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingMover.java` — getCombinedSpeedFactor 헬퍼
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` — fly / flyingSpeedFactor / flyControlVertical 등 옵션
- `MixinClientPlayerEntity.java` — 비행 진입/종료 처리 (capabilities.flying ↔ sm.isFlying 동기화)

**해결 접근 (예상)**:
1. **MixinLivingEntityClient L149 read** — handleFlying 가 어느 메서드 어디서 inject 되는지 + ci.cancel 호출 여부 확인.
2. **sm.isFlying 갱신 grep** — 매 틱 어디서 true/false 토글되는지 확인 + capabilities.isFlying 과 동기화 검증.
3. **cfg.fly 기본값 검증** — SmartMovingConfig.fly 가 기본 true 인지 + 사용자가 disabled 한 적 있는지.
4. **handleFlying 효과 디버깅** — log 추가 (handleFlying 호출 시 / vel 변경 전후) 또는 breakpoint 로 실제 호출 + 효과 확인.

**우선순위**: 🔴 매우 높음 (BUG-1 의 동일 원인 가능성 매우 높음 / 게임플레이 결정적)

---

## BUG-17. 🔴 sm_jump cfg.enabled 가드 부재 — 점프 자체 차단 (점프 안 됨 직접 원인 / 세션 36 3차 감사)

**증상** (사용자 보고): BUG-14 (handleJumping 가드) 추가 후에도 SM disabled 시 점프 작동 안 함.

**원인 (3차 감사)**: `MixinLivingEntityClient.java` L255-L262 `sm_jump` 가 vanilla `LivingEntity.jump()` 메서드 자체를 `@Inject(HEAD, cancellable=true)` + `ci.cancel()` 으로 **완전 차단** + jumpAvoided/jumpPending 세팅. **cfg.enabled 가드 부재** → SM disabled 시에도 vanilla 점프 차단 → 점프 자체 작동 안 함.

**해결 (세션 36 AI 완결)**: sm_jump 진입 직후 `if (!SmartMovingConfig.Config.enabled) return;` 추가. SM disabled 시 ci.cancel 안 됨 → vanilla 점프 정상.

**우선순위**: 🔴 매우 높음 (점프 안 됨 직접 원인 — BUG-14 보다 더 깊은 원인).

---

## BUG-18. sm_jumpingFilter cfg.enabled 가드 (보완 / 세션 36 3차 감사)

**위치**: `MixinLivingEntityClient.java` L232 `sm_jumpingFilter` (tickMovement HEAD).

**원인**: sm.* 조건 시 `this.jumping = false` 강제. sm.* 자체가 disabled 시 false 라 영향 거의 없으나 jumpCharge/blockJumpTillButtonRelease 잔존 가능성 차단 + 명시성.

**해결 (세션 36 AI 완결)**: 진입 직후 `if (!SmartMovingConfig.Config.enabled) return;` 추가.

**우선순위**: 🟠 높음 (보완 / BUG-19 resetState 보강과 함께).

---

## BUG-19. resetState 점프 잔존 필드 reset (안전망 / 세션 36 3차 감사)

**위치**: `SmartMovingClientState.java` resetState() L1947 부근.

**원인**: jumpAvoided / jumpPending / blockJumpTillButtonRelease / jumpCharge 가 resetState 에서 미reset → BUG-17/18 가드 추가 후에도 disabled 진입 시 잔존 가능 (단 sm_jump/sm_jumpingFilter 자체 작동 안 함 → 영향 적음).

**해결 (세션 36 AI 완결)**: resetState 에 4 필드 reset 추가 (안전망):
```java
jumpAvoided                  = false;
jumpPending                  = false;
blockJumpTillButtonRelease   = false;
jumpCharge                   = 0F;
```

**우선순위**: 🟠 높음 (안전망 / 잔존 차단).

---

## BUG-13. SM disabled 시 sm_animateFalling 잔존 (낙하 모션 / 세션 36 2차 감사)

**증상** (사용자 보고): SM disabled 인데도 공중 낙하 시 sm_animateFalling 모션 작동.

**위치**: `MixinPlayerEntityModelClient.java` L143-L152 — sm_setAngles 의 else (isFalling) 분기.

**원인**: else 분기의 `if (isFalling)` 조건이 vanilla 만 검사 (`!player.isOnGround() && fallDistance > 1.5f`) → SM disabled 시에도 sm_animateFalling 호출. 11 sm.* 분기는 sm.* 자체가 disabled 시 false 라 안전했지만 isFalling 은 sm.* 무관.

**해결 (세션 36 AI 완결)**: BUG-13/16 통합 해결 — sm_setAngles 진입점에 reset 인프라 + cloak.pitch 처리 후 `if (!cfgEnabled) return;` 추가. 모든 SM 분기 + isFalling + isAngleJumping 한 번에 skip.

**우선순위**: 🔴 매우 높음 (사용자 보고 직접 영향).

---

## BUG-14. SmartMovingJumper.handleJumping cfg.enabled 가드 부재 (점프 안 됨 / 세션 36 2차 감사)

**증상** (사용자 보고): SM disabled 시 점프 자체 안 됨.

**위치**: `SmartMovingJumper.java` L354 `handleJumping(player, sm)` 진입.

**원인**: cfg.enabled 가드 부재 → SM disabled 시에도 jumpPending 클리어 / jumpAvoided 세팅 / vanilla jump 가로챔 등 모든 처리 작동. vanilla jump() 차단 + SM 점프도 disabled (jumpAvoided 만 세팅) → 점프 자체 작동 안 함.

**해결 (세션 36 AI 완결)**: handleJumping 진입 직후 `if (!cfg.enabled) return;` 추가. SM disabled 시 vanilla 점프 그대로 정상 작동.

**호출처 검증**: `MixinLivingEntityClient.sm_travel_client` L80 (BUG-11 가드 후 도달 안 함이지만, 다른 호출처 가능성 + handleJumping 자체 가드가 안전).

**우선순위**: 🔴 매우 높음 (사용자 보고 직접 영향).

---

## BUG-15. SmartMovingClientState.tickEssential disabled→enabled 전환 잔존 (엣지 / 세션 36 2차 감사)

**위치**: `SmartMovingClientState.java` L920-L921.

**상태**: 검증 결과 — disabled 진입 시 resetState() 호출 정상 ✅. enabled 전환 후 매 틱 sm.* 재계산 → 잔존 상태 있어도 다음 프레임 정상.

**해결**: 추가 수정 불필요. sm_setAngles 의 cfgEnabled return 가드 (BUG-13/16) 로 SM 분기 자체 작동 안 함. 안전.

**우선순위**: 🟢 낮음 (엣지 / 실제 영향 없음).

---

## BUG-16. sm_setAngles 11 분기 + isFalling + isAngleJumping cfgEnabled 통합 가드 (세션 36 2차 감사 / BUG-13 통합)

**증상**: 사용자 보고 — SM disabled 시 SM 기능들 다 꺼져야 하는데 안 그렇다.

**원인**: sm_setAngles 의 if-else 체인 본체에 cfgEnabled 가드 부재. 각 sm.* 분기는 sm.* 자체가 disabled 시 false 라 안전했지만 isFalling (else 분기) + isAngleJumping (별도 if) 은 sm.* 무관 또는 SmartMovingJumper 잔존 상태 기반 → 작동 가능.

**해결 (세션 36 AI 완결)**: 진입점에 reset 인프라 + cloak.pitch 처리 후 `if (!cfgEnabled) return;` 추가:
- reset 인프라 (`if (anySmState || !cfgEnabled)` 4 필드 = 0) → SM 잔존 정리
- cloak.pitch (`cfgEnabled ? SIXTYFOURTH : 0f`) → SM 잔존 정리
- 그 후 `if (!cfgEnabled) return;` → smallOverGroundHeight + 11 분기 + isFalling + isAngleJumping 한 번에 skip

cloak.pitch 처리 위치도 진입점 위로 이동 (기존 메서드 끝에서 → return 가드 위로).

**우선순위**: 🔴 매우 높음 (BUG-13 통합 해결).

---

## BUG-9. SM disabled 시 multiPlayerInitialized 갱신 잔존 (BUG-7 확장 / 세션 36 전수 감사)

**증상**: SM disabled 상태에서 서버→클라이언트 위치 동기화 시 (`onPlayerPositionLook`) `sm.multiPlayerInitialized = 5` 강제 세팅 → 다음 5 틱 동안 sm_travel_client 의 pushOutOfBlocks 억제가 작동 (간접 영향).

**위치**: `MixinClientPlayNetworkHandler.java` L28-34 `sm_beforePlayerPositionLook`.

**해결 (세션 36 AI 완결)**: 진입 시 `if (!SmartMovingConfig.Config.enabled) return;` 추가. SmartMovingConfig import 추가.

**우선순위**: 🔴 매우 높음 (BUG-7 확장 패턴).

---

## BUG-10. SM disabled 시 forceIsSneaking 강제 잔존 (BUG-7 확장 / 세션 36 전수 감사)

**증상**: SM disabled 상태에서도 블록 상호작용 시 `sm.forceIsSneaking = player.isSneaking()` 강제 세팅 → `sm_isSneaking()` override 가 vanilla 결과 그대로 반환되도록 영향. 외부 모드/플러그인 호환성 영향 가능.

**위치**: `MixinClientPlayerInteractionManager.java` L30-42 `sm_beforeInteractBlock` + `sm_afterInteractBlock`.

**해결 (세션 36 AI 완결)**: HEAD/RETURN 양쪽에 `if (!SmartMovingConfig.Config.enabled) return;` 추가 (4 라인). SmartMovingConfig import 추가.

**우선순위**: 🔴 매우 높음 (BUG-7 확장 패턴).

---

## BUG-11. SM disabled 시 sm_travel_client 의 vanilla flying 억제 작동 (BUG-7 확장 / 세션 36 전수 감사)

**증상** (가장 심각): SM disabled 상태에서 Creative 비행 시 `sm_travel_client` L67 `if (player.getAbilities().flying && !cfg.fly)` 가 작동 → `motionY *= 0.5999...` (50% 감쇠) → vanilla creative 비행 약화. SM disabled 사용자가 비행 약화 경험.

**위치**: `MixinLivingEntityClient.java` L53-L207 `sm_travel_client` 전체 메서드. L67 vanilla flying 억제 + handleSwimming/handleLava/handleSliding/handleFlying/climbing 모든 분기.

**해결 (세션 36 AI 완결)**: L62 `if (player.isSpectator() || player.isFallFlying()) return;` 다음에 `if (!cfg.enabled) return;` 추가. SM disabled 시 travel 전체 작동 안 함 → vanilla 정상.

**회귀 안전성**: SM enabled 시 기존 동작 그대로. SM disabled 시 vanilla travel 정상 작동 + sm.* 필드는 resetState 로 false 유지 → 다른 SM 분기 영향 없음.

**우선순위**: 🔴 매우 높음 (BUG-7 패턴 + 게임플레이 즉시 영향 — Creative 비행 약화).

---

## BUG-12. SM disabled 시 sm_captureBodyYaw + sm_setupTransforms 진입점 가드 부재 (BUG-7 확장 / 세션 36 전수 감사)

**증상**: SM disabled 상태에서도 `sm_captureBodyYaw` 와 `sm_setupTransforms` 가 진입 → 내부 sm.* 필드 false 이므로 실제 변환은 적용 안 되지만 무용 계산 + 명시성 부재. 추가로 `smOuterTiltX = 0f;` reset 만 작동 (matrices 변환은 모든 분기 false 로 skip).

**위치**:
- `MixinPlayerEntityRenderer.java` L87 `sm_captureBodyYaw` (HEAD)
- `MixinPlayerEntityRenderer.java` L207 `sm_setupTransforms` (TAIL)

**해결 (세션 36 AI 완결)**:
- `sm_captureBodyYaw`: `localPlayer` 캐스팅 직후 `if (!SmartMovingConfig.Config.enabled) return;` 추가. smBodyYawActive 는 진입 시 이미 false 로 reset (L92).
- `sm_setupTransforms`: `sm.smOuterTiltX = 0f;` reset 후 `if (!SmartMovingConfig.Config.enabled) return;` 추가. 매 호출마다 0 reset 으로 cape 클램프 fallback 보장.

**우선순위**: 🟠 높음 (간접 가드로 실제 동작 영향 낮지만 명시성 + 성능).

---

## BUG-7. SM disabled 시 — 애니메이션 초기화 안 됨 + 비행 시 SM 비행 애니메이션

**증상**:
- SM 비활성화 (Config.enabled = false) 후에도 sm_setAngles 의 SM 분기 결과 잔존
- 특히 비행 시 SM 비행 애니메이션 (sm_animateFlying) 그대로 나옴 = 비활성화 무시

**영향 포커스**:
- **#5** (옵션토글 2상태 — ✅ 완료된 작업이지만 반환 인터페이스 미확인)
- **#1** (sm_setAngles 의 disabled 시 reset 처리)

**의심 지점**:
1. **`SmartMovingConfig.Config.enabled` 의 즉시 반영** — 토글 후 다음 프레임 sm_setAngles 가 새 값 읽는지. 캐시 또는 지연 가능성.
2. **`resetState()` 호출** — `SmartMovingClientState.java` L915-L916 `if (!Config.enabled || isSpectator || isFallFlying) resetState();`. resetState 가 모든 SM 상태 (isFlying / isClimbing / 등) 를 false 로 reset 하지만, **모델의 ModelPart 상태 (head.pivotY / body.pivotZ / 등) 는 reset 안 함**. → vanilla setAngles 가 매 프레임 reset 하는 필드 (sneak 분기) 외는 SM 변경값 잔존.
3. **sm_setAngles HEAD reset 인프라 (B-9/B-11/B-13 / 4 필드)** — `if (anySmState)` 안에서만 적용. SM disabled → anySmState false → reset 안 됨 → 이전 SM 상태의 head.pivotZ / body.pivotZ / body.yaw / head.roll 잔존.
4. **flyingCreative 분기** — `MixinPlayerEntityModelClient.java` L86 `boolean flyingCreative = player.getAbilities().flying;`. `anySmState = ... || flyingCreative;` 으로 비행 시 anySmState true → SM 비행 애니메이션 적용. **SM disabled 일 때도 flyingCreative 만으로 진입** = **버그 원인**.

**해결 방안 (BUG-7 의 핵심)**:
- `flyingCreative` 분기를 `SmartMovingConfig.Config.enabled` 가드로 감싸야 함.
- 또는 `anySmState` 계산을 `Config.enabled && (...)` 형태로 변경.
- 또는 sm_setAngles 의 `if (anySmState)` 를 `if (Config.enabled && anySmState)` 로 변경.

**해결 (세션 36 AI 완결)**:
원인 진단 = 의심 지점 4 (flyingCreative Config.enabled 가드 부재) 가 핵심. 추가로 cloak.pitch SIXTYFOURTH (B-16) 도 가드 부재 발견.

`MixinPlayerEntityModelClient.java` 3 가드 추가:
1. **L84 `flyingCreative` 가드**:
   ```java
   boolean cfgEnabled = SmartMovingConfig.Config.enabled;
   boolean flyingCreative = cfgEnabled && player.getAbilities().flying;   // BUG-7
   ```
2. **L88-L100 reset 인프라 분기 확장**: `if (anySmState || !cfgEnabled)` — SM disabled 진입 시 잔존 4 필드 (head.pivotZ + body.pivotZ + body.yaw + head.roll) 정리.
3. **L150 cloak.pitch 가드**: `cfgEnabled ? SIXTYFOURTH : 0f` — disabled 시 0 reset (vanilla 미reset 필드).

`leaningPitch` 강제 0 분기는 `if (anySmState)` 그대로 유지 (SM enabled 일 때만 vanilla setupTransforms Branch 2 차단 — disabled 시 vanilla leaningPitch 동작 보존).

**회귀 안전성**:
- SM enabled + 비행 → cfgEnabled=true → flyingCreative=true → SM 비행 처리 (기존 동작 보존)
- SM enabled + 비행 외 → cfgEnabled=true / flyingCreative=false → sm.* 분기 처리 (기존 동작 보존)
- SM disabled + 비행 → cfgEnabled=false → flyingCreative=false → vanilla 비행 처리 + reset 인프라 작동 → SM 잔존 정리 + cloak.pitch=0 reset (BUG-7 해결)
- SM disabled + 비행 외 → cfgEnabled=false → 모든 sm.* false (resetState 강제) + flyingCreative=false → reset 인프라 작동 → vanilla 정상

빌드 검증 — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

**세션 36 AI 완결 / 인게임 검증 대기**.

**리서치 필요 파일**:
- 1.21.1: `MixinPlayerEntityModelClient.java` L83-L96 (anySmState 계산 + reset 인프라)
- 1.21.1: `SmartMovingClientState.java` L912-L916 (resetState 호출 조건)
- 1.21.1: `SmartMovingConfig.java` (Config.enabled 토글 처리)
- 원본: `SmartMovingSelf.java` — `isActive()` 와 SM disabled 시 reset 로직

**우선순위**: 🔴 매우 높음 (비활성화 자체가 작동 안 함 — 토글 무용지물)

---

## 진행 권장 순서

| 순서 | 버그 | 우선순위 | 의존 | 비고 |
|------|------|---------|------|------|
| 1 | ✅ BUG-7+9~24 SM disabled 가드 전수 보강 (17 BUG) | 🔴 매우 높음 | 없음 | **세션 36 인게임 검증 완료** — sm_setAngles + sm_jump + handleJumping + 17 inject 가드 + resetState 4 필드 reset + sm_setAngles 통합 가드 (cloak.pitch 위로 이동 + return 가드) |
| 2 | BUG-6 I/O 키 비활성화 | ✅ 완료 | 없음 (단순) | 세션 35 완료 |
| 3 | **BUG-1 + BUG-8** 비행 고정 + 비행 시스템 미작동 | 🔴 매우 높음 | #2/#2.5 / SmartMovingFlyer | **함께 진단** — 동일 원인 가능성 매우 높음 |
| 4 | BUG-3 crawl 진동 | 🔴 매우 높음 | #2.7 (POSE/BBox 동기화) | |
| 5 | BUG-5 swim 진동 | 🔴 매우 높음 | #2.6 (lava liquid border) | |
| 6 | BUG-2 climbing 작동 안 함 | 🔴 매우 높음 | #2 (climbing 진입) | |
| 7 | BUG-4 다이빙 망가짐 | 🟠 높음 | #4 (키 조합) + #2 (dive 진입) | |

---

## 발견 시점 메타

- **세션**: 35 (2026-04-26)
- **발견 단계**: 포커스 #1 Phase R + Phase B + 회귀 감사 통과 후 인게임 통합테스트
- **사용자 보고 형식**: 자유 서술 + 증상 직접 경험 (시각 + 게임플레이)
- **AI 단독 진단 한계**: 인게임 재현 영상/로그 없음 → 본 docs 의 "의심 지점" + "리서치 필요 파일" 은 코드 패턴 기반 추정. 실제 디버깅 시 console log + breakpoint 활용 필요.

---

## 후속 작업 큐

- [✅ 인게임 검증 완료 (세션 36)] BUG-7 sm_setAngles flyingCreative 가드 + cloak.pitch 가드 + reset 인프라 분기 확장 — 세션 36. 3 가드 추가로 SM disabled 시 잔존 정리.
- [✅ 인게임 검증 완료 (세션 36)] **BUG-9** MixinClientPlayNetworkHandler.sm_beforePlayerPositionLook — multiPlayerInitialized 갱신 가드 (세션 36)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-10** MixinClientPlayerInteractionManager.sm_beforeInteractBlock + sm_afterInteractBlock — forceIsSneaking 강제 가드 (세션 36)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-11** MixinLivingEntityClient.sm_travel_client — vanilla flying 억제 + 모든 SM 분기 진입 가드 (세션 36 / Creative 비행 약화 해결)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-12** MixinPlayerEntityRenderer.sm_captureBodyYaw + sm_setupTransforms — 진입점 가드 (세션 36)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-13** sm_animateFalling 잔존 (낙하 모션) — sm_setAngles 진입점 cfgEnabled return 가드로 통합 해결 (세션 36 2차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-14** SmartMovingJumper.handleJumping cfg.enabled 가드 — 점프 작동 안 함 해결 (세션 36 2차 감사)
- [✅ 검증 완료 — 추가 수정 불필요] **BUG-15** SmartMovingClientState.tickEssential disabled→enabled 전환 — resetState 호출 정상 ✅ (세션 36 2차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-16** sm_setAngles 11 분기 + isFalling + isAngleJumping 통합 가드 — 진입점 cfgEnabled return + cloak.pitch 처리 위로 이동 (세션 36 2차 감사 / BUG-13 통합)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-17 (점프 안 됨 직접 원인)** sm_jump cfg.enabled 가드 — vanilla jump() ci.cancel 차단을 SM disabled 시 skip → vanilla 점프 정상 (세션 36 3차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-18** sm_jumpingFilter cfg.enabled 가드 — this.jumping=false 강제 가드 보완 (세션 36 3차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-19** resetState 점프 잔존 4 필드 reset 추가 — jumpAvoided/jumpPending/blockJumpTillButtonRelease/jumpCharge 안전망 (세션 36 3차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-20** sm_getBaseDimensions_client cfg.enabled 가드 — 비행 진입 첫 프레임 0.6×0.8 bbox 강제 차단 (세션 36 4차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-21** sm_getOffGroundSpeed cfg.enabled 가드 — cfg.fly=false 사용자 SM disabled 시 비행 속도 0.05F 강제 차단 (세션 36 4차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-22** sm_updatePose_client cfg.enabled 가드 — 비행 진입 첫 프레임 SLIDING POSE 강제 차단 (세션 36 4차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-23** sm_isClimbing_client + sm_applyClimbingSpeed + sm_updateLimbs_client + sm_isInSwimmingPose_client cfg.enabled 가드 — sm.* 잔존 timing 차단 (4 inject) (세션 36 4차 감사)
- [✅ 인게임 검증 완료 (세션 36)] **BUG-24** sm_beforeMove_client + sm_afterMove_client cfg.enabled 가드 — sm.heightOffset 잔존으로 인한 player.setPos 영향 차단 (비행 진입 뚜둑 핵심 원인 가능) (세션 36 4차 감사)
- [✅ AI 완결 / 인게임 검증 대기] **BUG-1+8** SmartMovingFlyer.handleFlying — player.move() 호출 누락 — 원본 L624 `sp.moveEntity` 매핑 부재 — SM 비행 motion 적용 안 됨 = 몸 고정 (세션 37)
- [✅ AI 완결 / 인게임 검증 대기] **BUG-25** 비행 속도 원본보다 느림 — Flying Phase F-6 (세션 40): SmartMovingFlyer 에 isFast 시 sprint 배수 곱셈 추가 (sprintFactor=1.5F 또는 sprintFactorLevitate). 원본 getNonSlowInputSpeedFactor (L197-L227) 누락 보강.
- [✅ AI 완결 / 인게임 검증 대기] **BUG-26** 비행 시 땅에 닿아도 착지 안 됨 — Flying Phase F-7 (세션 43-44): 세션 43 default 값 정정 (true→false) 후에도 사용자 환경 disk 의 이전 값 우선 → 자동 마이그레이션 추가 (세션 44). load() 후 marker `move.config.migration.flying` 미존재 시 강제 false reset + save → 다음 로드부터는 새 default 적용. 사용자 의도적 true 설정 시 마이그레이션 후 다시 수동 변경 가능.
- [⏳ BUG-25 후 재평가] **BUG-27** 위아래 보면서 전진 속도 너무 느림 — F-2 결과: moveFlying 정확 1:1 → BUG-25 의 부분 증상 가능성.
- [✅ AI 완결 / 인게임 검증 대기] **BUG-28** 처음 비행 시 하늘 끝까지 날아감 — Flying Phase F-7 (세션 44 진짜 원인): 세션 42/43 motionY 관련 fix들 (sm_jump 가드, vanilla boost cancel, jumping=raw input) 모두 유지 — 1:1 보강. **그러나 진짜 직접 원인은 heightOffset 매핑 오류**: 원본 SmartMovingSelf.setHeightOffset (L1694-L1704) = `boundingBox.minY -= heightOffset + height += heightOffset` (박스 크기만 변경, player 위치 불변). 1.21.1 매핑 (MixinEntityClient.afterMoveEntity L99-L101) = `player.setPos(y - heightOffset)` (**player 위치 변경**) + 매 틱 호출 = **매 틱 +1 누적 → 매 틱 +1 블록 = 20 블록/초 무한 상승**. 비행 진입 시 heightOffset=-1F set 되어 매 틱 누적. 정정: SmartMovingClientState L1312-L1323 의 isFlying/isLevitating 진입 엣지 setHeightOffset(-1F) **제거** (1.21.1 vanilla 자동 hitbox 처리 → SM 보정 불필요). 누적 0 → 무한 상승 해소.
- [✅ AI 완결 / 인게임 검증 대기] **BUG-29** 비행 진입 뚝 끊김 + 몸 중심점 발끝 느낌 — Flying Phase F-7 (세션 44): BUG-28 과 **동일 원인** = heightOffset=-1F 매 틱 player.setPos(y+1) 누적. 사용자 보고 "몸 중심점이 발끝으로 바뀐 느낌" = 매 틱 모델이 1블록 위로 (= 발끝이 원래 몸 중심 위치) 시각화. 정정: 비행/Levitate 진입 엣지 setHeightOffset(-1F) 제거 (BUG-28 동일 fix).
- [✅ AI 완결 / 인게임 검증 대기] **BUG-30** 비행 가만히 있을 때 팔 회전 축 다름 — Flying Phase F-7 (세션 42): setAnglesXZY/YXZ/ZXY 3 헬퍼 모두 quaternion 곱 순서 reversal 오류 발견. 원본 ModelRotationRenderer.rotate XZY (L148/L151/L157) GL call 순서 = Y, Z, X → vertex 적용 순서 X, Z, Y → JOML qY * qZ * qX (right-most 부터 적용). 이전 매핑 = qX * qZ * qY (vertex 적용 Y, Z, X — 원본 반대). 큰 roll (π/2 비행 자세) 시 가시화 = 사용자 보고 "팔 방향과 같은 축으로 스크류 회전" 직접 원인. setAnglesYXZ/ZXY 도 동일 패턴 오류 정정 (isSwim head, isSlide body, animateAngleJumping 다리 영향).
- [✅ 인게임 검증 완료 (세션 41)] **BUG-31** 몸 기울기 방향 다름 (몸 앞쪽이 하늘) — Flying Phase F-6 (세션 40): X 회전 부호 반전 (`-theta`). 사용자 확인 "기우는 방향 자체는 고쳐짐" ✅.
- [⏳ BUG-30 후 재평가] **BUG-32** 머리 이상하게 고정 — F-3 결과: head.pitch = -theta/2 매핑 정확 → BUG-30 setAngles* 정정으로 함께 해소 가능 (사용처 isSwim head 등). 인게임 재확인 대기.
- [⏳ BUG-25 후 재평가] **BUG-33** 비행 모든 애니메이션 부드럽지 않음 (프레임 끊김) — F-3 결과: sm.stats.calculate 정상 호출 → 다른 원인 (BUG-25 sprint 미적용 영향 가능).
- [⏳ BUG-25/31 후 재평가] **BUG-34** 비행 디테일 (각도/움직임/속도/스무스함) 다름 — F-3 결과: sm_animateFlying 정확 1:1 → BUG-25/31 누적 효과.
- [✅ AI 완결 / 인게임 검증 대기] **잉여 (BUG-25 부수)** sm_flyWhileOnGround `!cfg.flyCloseToGround` 가드 삭제 — F-6: 원본 L1827 에 없는 잉여 가드. flyCloseToGround=true 시 자동 비행 복원 차단 영향.
- [✅ AI 완결 / 인게임 검증 대기] **누락 (BUG-25 부수)** 비행 중 fallDistance reset 추가 — F-6: 원본 L1803-L1804 매핑. MixinLivingEntityClient.sm_jumpingFilter 에 `if (player.getAbilities().flying) player.fallDistance = 0F;` 추가.
- [x] BUG-6 I/O 키 비활성화 — 세션 35 완료 (`SmartMovingClientState.java` L894-L910)
- [ ] **BUG-1 + BUG-8** 비행 고정 + SM 비행 시스템 미작동 — **함께 진단** (동일 원인 가능):
    - SmartMovingFlyer.handleFlying 호출 조건 + ci.cancel 검증 (`MixinLivingEntityClient.java` L149)
    - sm.isFlying 갱신 위치 grep (`SmartMovingClientState.java`)
    - SmartMovingConfig.fly 기본값 검증
    - vanilla travel() 와의 inject 순서 검증
    - getCombinedSpeedFactor 비행 시 결과 검증 (Mover.java)
    - SmartMovingSelf.java L602-L631 + L1803-L1831 + L2198-L2199 + L2320 + L2404 라인별 read
- [ ] BUG-3 crawl 진동 — #2.7 POSE/BBox 동기화 검증 + isCrawling 종료 조건
- [ ] BUG-5 swim 진동 — #2.6 lava liquid border + 수면 경계 판정 정확성
- [ ] BUG-2 climbing 작동 안 함 — handsClimbType/feetClimbType 갱신 + grab 키 처리
- [ ] BUG-4 다이빙 망가짐 — #4 키 조합 + isDiving 종료 조건
