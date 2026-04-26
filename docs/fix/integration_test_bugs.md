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

**영향 포커스**:
- **#2 / #2.5** (Jumper Factor 인프라 / Creative flying speed 처리) — 이동 자체 차단
- **#1** (sm_animateFlying 자세) — 애니메이션 차이

**의심 지점**:
1. **`flyingCreative` 분기** (`MixinPlayerEntityModelClient.java` L129) — sm_animateFlying 호출 조건. 진입 시 다른 분기 처리가 이동을 차단할 가능성.
2. **B-7 입력값 교체** (세션 32) — flying L576-L578 = `sm.stats.totalDistance/currentSpeed` 사용. SmartStatistics.calculate 가 비행 중 갱신되지 않으면 distance=0/speed=0 → 애니메이션 고정. **확인 필요**: `MixinEntityClient.java` L80 `sm.stats.calculate(...)` 호출이 비행 중에도 실행되는지.
3. **이동 자체 차단** — Mixin 이 player.move() 또는 ClientPlayerEntity tick 에서 비행 중 이동 처리 차단? `MixinClientPlayerEntity` / `SmartMovingClientState` tick 진입 로직 검토.
4. **B-15 isLevitating 우선 처리** (세션 29) — flying 분기 진입 전에 isLevitating 분기로 빠지는 가능성. flying 시 isLevitating false 보장?

**리서치 필요 파일**:
- 원본: `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` — flying 진입/유지/종료 로직 (`startFlying`/`updateFlying`/`stopFlying` 또는 등가)
- 원본: `SmartMovingModel.java` L474-L520 (isFlying 분기 본체)
- 1.21.1: `MixinClientPlayerEntity.java` (tick 진입) + `SmartMovingClientState.java` (isFlying 갱신)
- 1.21.1: `MixinEntityClient.java` L80 (stats.calculate 호출 조건)

**우선순위**: 🔴 매우 높음 (이동 자체 차단 — 게임플레이 결정적)

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

**리서치 필요 파일**:
- 1.21.1: `MixinPlayerEntityModelClient.java` L83-L96 (anySmState 계산 + reset 인프라)
- 1.21.1: `SmartMovingClientState.java` L912-L916 (resetState 호출 조건)
- 1.21.1: `SmartMovingConfig.java` (Config.enabled 토글 처리)
- 원본: `SmartMovingSelf.java` — `isActive()` 와 SM disabled 시 reset 로직

**우선순위**: 🔴 매우 높음 (비활성화 자체가 작동 안 함 — 토글 무용지물)

---

## 진행 권장 순서

| 순서 | 버그 | 우선순위 | 의존 |
|------|------|---------|------|
| 1 | BUG-7 SM disabled reset 실패 | 🔴 매우 높음 | 없음 (단순 가드) |
| 2 | BUG-6 I/O 키 비활성화 | 🟢 낮음 (요청 처리) | 없음 (단순) |
| 3 | BUG-1 비행 고정 | 🔴 매우 높음 | #2/#2.5 |
| 4 | BUG-3 crawl 진동 | 🔴 매우 높음 | #2.7 (POSE/BBox 동기화) |
| 5 | BUG-5 swim 진동 | 🔴 매우 높음 | #2.6 (lava liquid border) |
| 6 | BUG-2 climbing 작동 안 함 | 🔴 매우 높음 | #2 (climbing 진입) |
| 7 | BUG-4 다이빙 망가짐 | 🟠 높음 | #4 (키 조합) + #2 (dive 진입) |

---

## 발견 시점 메타

- **세션**: 35 (2026-04-26)
- **발견 단계**: 포커스 #1 Phase R + Phase B + 회귀 감사 통과 후 인게임 통합테스트
- **사용자 보고 형식**: 자유 서술 + 증상 직접 경험 (시각 + 게임플레이)
- **AI 단독 진단 한계**: 인게임 재현 영상/로그 없음 → 본 docs 의 "의심 지점" + "리서치 필요 파일" 은 코드 패턴 기반 추정. 실제 디버깅 시 console log + breakpoint 활용 필요.

---

## 후속 작업 큐

- [ ] BUG-7 sm_setAngles flyingCreative 가드 — `Config.enabled` 추가
- [x] BUG-6 I/O 키 비활성화 — 세션 35 완료 (`SmartMovingClientState.java` L894-L910)
- [ ] BUG-1 비행 고정 — #2 / #2.5 stats.calculate 호출 검증 + isFlying 갱신 위치 분석
- [ ] BUG-3 crawl 진동 — #2.7 POSE/BBox 동기화 검증 + isCrawling 종료 조건
- [ ] BUG-5 swim 진동 — #2.6 lava liquid border + 수면 경계 판정 정확성
- [ ] BUG-2 climbing 작동 안 함 — handsClimbType/feetClimbType 갱신 + grab 키 처리
- [ ] BUG-4 다이빙 망가짐 — #4 키 조합 + isDiving 종료 조건
