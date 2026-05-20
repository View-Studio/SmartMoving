# SmartMoving 수영 (Swim/Dive) 애니메이션 작업 체크리스트

**작성일**: 2026-05-20
**근거**: [docs/research_swimming_animation.md](research_swimming_animation.md)
**작업 범위**: 애니메이션 (setAngles / ModelPart 회전 / setupTransforms / render). 자세 lean, 팔/다리 stroke/kick, head 회전, body sway, idle 진동.
**작업 범위 제외**: 기능 (state / motion / 박스 dim / 키 입력) — [[project_swimming_complete]] 완결, 절대 수정 X.

> **핵심 발견 (Phase 0 리서치 결과)**: 수영 애니메이션은 이미 1.7.10/1.12.2 1:1 매핑 완료 상태. 신규 구현 항목 없음. **인게임 시각 검증 + 미세 부호/축/pivot 보정** 위주.

---

## Phase 0 — 1:1 매핑 검증 ✅ (완료)

리서치 agent 가 1.7.10/1.12.2 SmartMovingModel.setRotationAngles + 우리 매핑 (`MixinPlayerEntityModelClient.sm_animateSwimming/sm_animateDiving` + `MixinPlayerEntityRenderer.sm_setupTransforms/sm_captureBodyYaw`) 1:1 교차 대조 완료.

식 / 매직넘버 / 회전 순서 / scale 식 모두 일치 — [research_swimming_animation.md §4 검증 표](research_swimming_animation.md) 참조.

---

## Phase 1 — 기본 자세 lean 시각 검증

### 1-1. isSwim 자세

- [ ] **1-1-1**: 수면 swim 진입 (offset 1.4~1.9) + 정지 → bipedOuter.X ≈ 78.75° (= Quarter - Sixteenth). 살짝 일어선 수평 자세.
- [ ] **1-1-2**: 수면 swim 이동 (W) → bipedOuter.X = 90° (= Quarter). 완전 수평 자세.
- [ ] **1-1-3**: 수면 swim 보행/달리기 (sprint) → standSneakFactor 변화 → 자세 부드러움 (fadeRotateAngleX 0.2 lerp).
- [ ] **1-1-4**: swim 진입 첫 frame visual jump 없음 (= prev 자세부터 0.2 lerp).

### 1-2. isDive 자세

- [ ] **1-2-1**: 잠수 (offset ≥ 1.9) + 입력 0 + jump/sneak 0 → isLevitating=true → bipedOuter.X = 78.75°.
- [ ] **1-2-2**: 잠수 + 마우스 위 보기 (pitch < 0) + W → bipedOuter.X = 90° - currentVerticalAngle → 머리부터 위.
- [ ] **1-2-3**: 잠수 + 마우스 아래 보기 (pitch > 0) + W → bipedOuter.X = 90° + |currentVerticalAngle| → 머리부터 아래.
- [ ] **1-2-4**: 잠수 + jump (= surface 점프 시도) → bipedOuter.X = 0° → 정자세 일시.
- [ ] **1-2-5**: jump → ground → 자세 자연 복귀.

### 1-3. 자세 transition

- [ ] **1-3-1**: standing → swim 진입 → 자세 부드러운 lean (fade 0.2).
- [ ] **1-3-2**: swim → dive 진입 → 자세 유지 (양쪽 SWIMMING POSE).
- [ ] **1-3-3**: dive → swim (surface 도달) → 자세 자연 변화.
- [ ] **1-3-4**: swim → standing (물 밖) → vanilla STANDING 자세 복귀 (cleanup 인프라).

---

## Phase 2 — 팔 stroke 시각 검증

### 2-1. isSwim 팔

- [ ] **2-1-1**: 이동 시 팔 X (pitch) = `(distance*0.5 % Whole - Half) * walkFactor` 톱니파 cycle — 자유형 영법.
- [ ] **2-1-2**: 좌/우 팔 phase 차이 `Half` (180°) → alternating stroke.
- [ ] **2-1-3**: 정지 시 팔 Z (roll) idle = `cos(totalTime*0.1F) * 0.8F` ±45° 진동 (= 펄럭임).
- [ ] **2-1-4**: 정지 시 팔 X idle = `Sixteenth * standSneakFactor` ±11.25° 정적 위치.
- [ ] **2-1-5**: 팔 yScale = `1F + Sixteenth*sneakFactor` (= 보행~달리기 호흡 ±15%).

### 2-2. isDive 팔

- [ ] **2-2-1**: 이동 시 팔 Z (roll) = `cos*0.52*2.5 + Quarter` 큰 진폭 (~75°) 평형 stroke.
- [ ] **2-2-2**: 정지 시 팔 Z = `(Quarter + Eighth) * standFactor` 112.5° 옆으로 벌림.
- [ ] **2-2-3**: 팔 yScale = `1F + Eighth*walkFactor` (= 이동 호흡 ±15%).

### 2-3. 1인칭 팔 (= ModelPart override leak 차단)

- [ ] **2-3-1**: swim 진행 중 1인칭 시점 전환 → 1인칭 손 정상 (= clearOverrideQuat 차단).
- [ ] **2-3-2**: swim 종료 후 1인칭 → 손 정상 (cleanup 인프라).

---

## Phase 3 — 다리 kick 시각 검증

### 3-1. isSwim 다리

- [ ] **3-1-1**: 이동 시 다리 X (pitch) = `cos(distance) * 0.523 * walkFactor` ±30° alternating kick (= 자유형 발차기).
- [ ] **3-1-2**: 좌/우 다리 phase 차이 `Half`.
- [ ] **3-1-3**: 정지 시 다리 Z idle = `cos(totalTime*0.1F) * 0.4F * (standFactor - sneakFactor)` ±23° 진동.
- [ ] **3-1-4**: sneak 시 다리 idle 부호 반전.
- [ ] **3-1-5**: 다리 yScale = `1F + Sixteenth*sneakFactor`.

### 3-2. isDive 다리

- [ ] **3-2-1**: 이동 시 다리 Z (roll) = `(cos+1) * 0.523 * walkFactor` 좌우 펼친 발차기 (= 평형 영법).
- [ ] **3-2-2**: 우 다리 Z = `(cos+1)*0.52*walkFactor + Sixteenth*standFactor` 항상 양수 (오른쪽 펼침).
- [ ] **3-2-3**: 좌 다리 Z = `(cos+Half-1)*0.52*walkFactor - Sixteenth*standFactor` 항상 음수 (왼쪽 펼침).
- [ ] **3-2-4**: 다리 yScale = `1F + Quarter*walkFactor` ±25% (= 이동 호흡, 팔보다 ×2 진폭).

---

## Phase 4 — head 회전 시각 검증

### 4-1. isSwim head

- [ ] **4-1-1**: 이동 시 head Y sway = `cos(distance/2 - Quarter) * walkFactor` 좌우 두리번.
- [ ] **4-1-2**: 정지 시 head X = `-Eighth * standSneakFactor` 위 보기 (= -45° 정도).
- [ ] **4-1-3**: head.pivotZ = -2F (= 앞으로 2px 이동, 자유형 영법 자세).

### 4-2. isDive head

- [ ] **4-2-1**: head.pitch = `-Eighth` 고정 (-22.5° 살짝 위).
- [ ] **4-2-2**: head.pivotZ = -2F.

### 4-3. head + bodyYaw

- [ ] **4-3-1**: sm_captureBodyYaw 가 horizontalAngle force → 머리+몸 정렬.
- [ ] **4-3-2**: head.yaw = 0 강제 (= netHeadYaw 무영향).

---

## Phase 5 — bodyYaw 추적 시각 검증

### 5-1. horizontalAngle 식

- [ ] **5-1-1**: 정지 (hDist < 0.015F) → currentCameraAngle (= 마우스 방향 추적).
- [ ] **5-1-2**: 이동 (hDist ≥ 0.015F) → currentHorizontalAngle (= 이동 방향 추적).
- [ ] **5-1-3**: sneak hold + 정지 → threshold 0.005 → 더 빠른 카메라 추적.
- [ ] **5-1-4**: sneak hold + 이동 → 0.005 threshold 적용.

### 5-2. bodyYaw force mechanism

- [ ] **5-2-1**: 정지 시 마우스 회전 → 몸 즉시 따라옴.
- [ ] **5-2-2**: 이동 시 몸이 이동 방향 향함 + 머리는 카메라 향함 (= head.yaw 자연).
- [ ] **5-2-3**: 잠수 → swim 전환 시 bodyYaw 연속 (jump 없음).

---

## Phase 6 — 호흡 scale 시각 검증

### 6-1. isSwim 호흡

- [ ] **6-1-1**: 정지~보행 사이 sneakFactor 변화 → 다리/팔 ±15% yScale 진동.
- [ ] **6-1-2**: 보행~달리기 사이 sneakFactor=0 → yScale 변화 없음.

### 6-2. isDive 호흡

- [ ] **6-2-1**: 이동 시 walkFactor=1 → 다리 ±25% / 팔 ±15% yScale.
- [ ] **6-2-2**: 정지 시 walkFactor=0 → yScale 변화 없음.

---

## Phase 7 — fade 보간 검증 (= 자세 전환 부드러움)

### 7-1. fadeRotateAngleX 효과

- [ ] **7-1-1**: 정지 ↔ 이동 자세 전환 시 bipedOuter.X 점진 변화 (= 즉시 jump X).
- [ ] **7-1-2**: swim ↔ dive 전환 시 자세 부드러움.
- [ ] **7-1-3**: 마우스 위/아래 빠른 회전 시 dive bipedOuter.X 점진 추적.

### 7-2. fade 구현 검증

- [ ] **7-2-1**: 우리 매핑이 matrices 직접 rotate — fade 별도 prev field 존재 여부.
- [ ] **7-2-2**: 자세 변화 frame 시각 검증 — visual jump 없음.
- [ ] **7-2-3**: 만약 visual jump 발생 → fade 식 누락 fix 후보.

---

## Phase 8 — 다른 영역과 transition

### 8-1. SM phase 간 전환

- [ ] **8-1-1**: standing → swim → standing 자연 전환.
- [ ] **8-1-2**: swim → 사다리 등반 (= isClimbing) 전환.
- [ ] **8-1-3**: swim → 비행 모드 토글 — 전환 자연.
- [ ] **8-1-4**: 비행 → 물 강제 진입 → swim 자세 자연.
- [ ] **8-1-5**: 얕은 물 swim → walk (= isSlow X) 전환 — 자세 복귀.
- [ ] **8-1-6**: 얕은 물 swim → crawl (= isSlow) 전환 — crawl 자세.

### 8-2. lava 진입 (= handleLava)

- [ ] **8-2-1**: lava 진입 시 자세 vanilla 위임 (= SM swim 자세 비활성).
- [ ] **8-2-2**: lava 안 자세 정상.

---

## Phase 9 — 멀티 동기화 검증 (REMOTE 시점)

### 9-1. 다른 player 수영 자세

- [ ] **9-1-1**: 다른 player swim 진입 시 자세 변경 자연.
- [ ] **9-1-2**: 다른 player swim 이동 시 자세 + 팔 stroke + 다리 kick 매끄러움.
- [ ] **9-1-3**: 다른 player dive 시 마우스 방향 추적 (= currentVerticalAngle 정확).

### 9-2. stats 동기화

- [ ] **9-2-1**: REMOTE side currentVerticalAngle / currentHorizontalAngle / currentSpeed / totalDistance 정확 수신.
- [ ] **9-2-2**: StatsPayload 또는 자체 atan2 식 정상.
- [ ] **9-2-3**: lerpPosAndRotation race 가능성 — REMOTE 자세 평탄화 잔존 여부.

---

## Phase 10 — 회귀 검증

### 10-1. 다른 완결 영역 회귀 차단

- [ ] **10-1-1**: 헤드점프 — single + multi 정상.
- [ ] **10-1-2**: 슬라이딩 — 정상.
- [ ] **10-1-3**: 엎드리기 (crawl) — 정상.
- [ ] **10-1-4**: 그랩 클라이밍 — 정상.
- [ ] **10-1-5**: 비행 / 낙하 / 각도점프 — 정상.

### 10-2. 1인칭 + 3인칭 검증

- [ ] **10-2-1**: 1인칭 시점 swim — 손/팔 정상.
- [ ] **10-2-2**: 3인칭 시점 swim — 모델 자세 정상.
- [ ] **10-2-3**: 시점 전환 자연.

### 10-3. orphan SWIMMING POSE 처리

- [ ] **10-3-1**: swim 종료 → 1 frame POSE=SWIMMING 잔존 시 SM smSmall 매치 X → orphan STANDING 강제 (메모리 [[feedback_orphan_swim_pose_lag]]).
- [ ] **10-3-2**: smSwim/smDive 모두 false + POSE=SWIMMING 잔존 시 정상 처리.

---

## Phase 11 — 완결 선언

- [ ] **11-1**: 사용자 인게임 통과 명시.
- [ ] **11-2**: 메모리 `project_swimming_animation_complete.md` 작성.
- [ ] **11-3**: MEMORY.md 인덱스 추가.
- [ ] **11-4**: 함부로 수정 금지 영역 추가.

---

## 작업 흐름 권장

1. **Phase 1** (자세 lean) — 가장 기본. 인게임 검증 시작.
2. **Phase 2-4** (팔/다리/head) — 자세 정착 후 detail 검증.
3. **Phase 5-6** (bodyYaw + 호흡) — 동적 식 검증.
4. **Phase 7** (fade) — 자세 전환 frame 시각.
5. **Phase 8-9** (transition + multi) — 통합 검증.
6. **Phase 10** (회귀) — 다른 영역 영향 없음 확인.
7. **Phase 11** (완결 선언).

각 Phase BUG 발견 시 [[feedback_fix_immediately]] 원칙 — 즉시 1:1 fix. 헤드점프 애니메이션 fix #79~#86 패턴 차용 (= 사용자 시각 보고 기반 frame 단위 부호/축/pivot 보정).

---

## 회귀 차단 의무

작업 진행 중 다음 영역 절대 건드리지 X (사용자 명시 완결):

- **[[project_swimming_complete]]** — 수영 기능 (state / motion / dim / 박스 / 키 입력).
- **[[project_headjump_all_complete]]** — 헤드점프 single + multi (#79~#100).
- **[[project_sliding_complete]]** / **[[project_sliding_animation_complete]]** — 슬라이딩.
- **[[project_crawl_complete]]** — 엎드리기.
- **[[project_grab_climbing_complete]]** / **[[project_vine_animation_complete]]** — 그랩/덩굴.
- **[[project_flying_complete]]** / **[[project_falling_complete]]** / **[[project_angle_jump_complete]]** — 비행/낙하/각도점프.

수영 애니메이션 작업이 위 영역과 교차 (가드 추가 / mixin 변경) 시 사용자 동의 + 회귀 검증 필수.

setAngles / setupTransforms 영역 한정 수정 — 다른 영역은 절대 건드리지 X.
