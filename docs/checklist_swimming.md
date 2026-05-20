# SmartMoving 수영 (Swim/Dive) 기능 작업 체크리스트

**상태**: ✅ **완결 (2026-05-20)** — 사용자 명시 선언. 함부로 수정 금지.
**적용 fix**: #101~#109 (총 9 fix).
**완결 메모리**: [[project_swimming_complete]]
**작성일**: 2026-05-20
**근거**: [docs/research_swimming.md](research_swimming.md)
**작업 범위**: 기능 (state / motion / transition / 박스 dim / 키 입력 / 멀티 동기화). 애니메이션 제외.

> **원칙**: 헤드점프 시리즈 같은 단위 fix 적용. self 측 동작 절대 망가뜨리지 X. 메모리 패턴 ([[feedback_server_side_state_push_mirror]], [[feedback_remote_setpos_lerp_cancel]], [[feedback_self_to_server_remote_one_to_one]] 등) 활용.

---

## Phase 0 — 사전 검증 (구조 매핑 확정)

### 0-1. 우리 매핑 vs 원본 1:1 대조

- [ ] **0-1-1**: `SmartMovingSwimmer.updateSwimState` ↔ `SmartMovingSelf.handleSwimming L229-302 (offset 계산 + 트리거 + 분류)`. 누락 식 점검.
- [ ] **0-1-2**: `SmartMovingSwimmer.handleSwimming` 의 motionYDiff 21 step 테이블 ↔ 원본 `L309-437` 11+10 step 식. 각 step 부호/계수 검증.
- [ ] **0-1-3**: damping 4 분기 (swim/dive/dip/standard) ↔ 원본 `L448-471`. 매직넘버 0.85/0.83/0.80/0.9 일치 확인.
- [ ] **0-1-4**: `levitating = diving && !diveUp && !diveDown && 입력 0` 식 위치 확인 — 우리 측 `Swimmer L188` 부근.
- [ ] **0-1-5**: `waterMovementTicks++` / `=0` 갱신 위치 ↔ 원본 `L481-484`.
- [ ] **0-1-6**: `isJumpingOutOfWater` 식 ↔ 원본 `L486-487`. 가드 4 항 (수평입력 + 벽충돌 + diveUp + !isSlow) + 추가 OR 3 항 (waterTicks>10 || onGround || wasJumpingOutOfWater).
- [ ] **0-1-7**: motion 분기 5 가지 ↔ 원본 `L489-502`. diving + (diveUp||diveDown||levitating) / diving + 일반 / swimming + swimDown / isJumpingOutOfWater / 기본.
- [ ] **0-1-8**: `setHeightOffset(-1F)` 호출 위치 ↔ 원본 `L510-511`. (isDiving || isSwimming) 가드.
- [ ] **0-1-9**: 얕은 물 transition 2 분기 (crawl / walk) ↔ 원본 `L513-535`. isSlow 분기 + y-snap moveEntity.
- [ ] **0-1-10**: `useStandard` 분기 (Config 비활성 fallback) ↔ 원본 `L553-572`.
- [ ] **0-1-11**: `handleLava` 매핑 ↔ 원본 `L578-600`.
- [ ] **0-1-12**: `resetSwimming` 8 필드 ↔ 원본 `L1488-1498`. 호출 위치 6 곳 모두 매핑 확인.
- [ ] **0-1-13**: `fallDistance = 0F` 매 tick reset ↔ 원본 `L603` (handleSwimming 반환 후).

### 0-2. 우리 매핑 자체 일관성 검증

- [ ] **0-2-1**: `SmartMovingSwimmer` 사용 함수 visibility (public/private) 적절성 검토.
- [ ] **0-2-2**: `SmartMovingClientState` 의 swim 관련 가드 위치 30+ 곳 누락 / 부적합 그룹화 확인.
- [ ] **0-2-3**: `sm_updatePose_client` 안 POSE 결정 식에 swim/dive 분기 포함 확인 (POSE=SWIMMING 매핑).
- [ ] **0-2-4**: `sm_getBaseDimensions` 안 dim 분기 — swim/dive 시 eye/height 값 확인 (heightOffset=-1F 효과 = mixin offset 활성).
- [ ] **0-2-5**: `MixinEntity.sm_offsetBoundingBoxForFlying` 가드 — swim/dive POSE 시 적용 분기 확인.

---

## Phase 1 — Server 측 패리티 보강

### 1-1. SmartMovingState packet bit 검증

- [ ] **1-1-1**: server `SmartMovingState.java` bit 10 (isDipping) 디코딩 식 추가 확인. 누락 시 추가.
- [ ] **1-1-2**: server `isDipping` 필드 선언 확인. 누락 시 추가.
- [ ] **1-1-3**: server `isLevitating` 비트 (19?) 디코딩 검증.

### 1-2. server side calculateDimensions 호출

- [ ] **1-2-1**: `SmartMovingServer.processStatePacket` 끝에 `player.calculateDimensions()` 호출 위치 확인. swim/dive bit 갱신 시 dim 영향 정확 갱신.
- [ ] **1-2-2**: [[feedback_processStatePacket_calculateDimensions]] 원칙 — 새 SM phase 가 dim 영향 시 즉시 갱신 의무.

### 1-3. server reconcile 가드 (sm_suppressPositionCheck)

- [ ] **1-3-1**: `sm_suppressPositionCheck` 식 안 swim/dive 가드 포함 확인. self side entity.y push (setHeightOffset=-1F) 시 server reconcile 차단 필요.
- [ ] **1-3-2**: 누락 시 [[feedback_sm_suppressPositionCheck_phase_guard]] 패턴 적용 (isSwimming || isDiving 추가).

---

## Phase 2 — 박스 dim / mixin offset 영향 검증

### 2-1. POSE 매핑

- [ ] **2-1-1**: swim 진입 시 POSE 결정 확인 (= SWIMMING? SLIDING?). vanilla 1.21.1 POSE=SWIMMING 이 표준.
- [ ] **2-1-2**: dive 진입 시 POSE 결정 확인.
- [ ] **2-1-3**: SmartMovingClient packet lambda 의 targetPose 결정 식 (L121-155) 안 swim/dive 분기 확인. 현재 `isSwimming_sm || isDiving → SWIMMING` 매핑 검증.

### 2-2. mixin offset 가드 영향

- [ ] **2-2-1**: swim/dive 시 mixin offset 활성 여부 확인. dim eye 와 POSE 둘 다 가드 통과 분기 검증.
- [ ] **2-2-2**: swim → dive 전환 시 mixin offset 일관성 (POSE 유지 / 변화).
- [ ] **2-2-3**: dive → swim 전환 시 동일.
- [ ] **2-2-4**: swim → standing (물 나옴) 시 mixin offset 차단 → bb drop 발생 여부 확인. self side push 식 있는가.

### 2-3. orphan POSE 차단

- [ ] **2-3-1**: SmartMovingClient packet lambda L131-154 의 orphan POSE 차단 분기 — swim/dive 종료 시 POSE=SWIMMING 잔존 차단 동작 확인.
- [ ] **2-3-2**: [[feedback_orphan_swim_pose_lag]] 패턴 점검.

---

## Phase 3 — 멀티 동기화 보강 (필요 시)

### 3-1. waterMovementTicks 동기화 (선택)

- [ ] **3-1-1**: self side `waterMovementTicks` 가 jump-out-of-water 판정 source — remote 측 다른 player 의 surface 점프 시각에 영향 있는가 평가.
- [ ] **3-1-2**: 영향 있으면 [[feedback_tickessential_self_only]] 패턴으로 SmartMovingState packet bit / StatsPayload 동기화 추가.

### 3-2. isJumpingOutOfWater 동기화 (선택)

- [ ] **3-2-1**: self side 1 tick set → motionY=0.3 push 후 즉시 false 가능. server bit 추가 vs derive 검토.
- [ ] **3-2-2**: 멀티 surface 점프 시각 BUG 발생 시 stat packet 동기화.

### 3-3. dippingDepth 동기화 (다음 단계 — 애니메이션 영향)

- [ ] **3-3-1**: 본 작업 범위 외 (애니메이션 영향). 표시만 — 다음 단계에서 처리.

---

## Phase 4 — 멀티 시나리오 시각 BUG fix

> 메모리 패턴 적용: [[feedback_server_side_state_push_mirror]], [[feedback_remote_setpos_lerp_cancel]], [[feedback_remote_phase_fix_ground_basis]], [[feedback_self_to_server_remote_one_to_one]].

### 4-1. standing → swim 진입 (BUG 후보)

- [ ] **4-1-1**: self side: `isSwimming_sm=true` set + `setHeightOffset(-1F)` 매핑 위치 확인. entity.y push 발동 여부.
- [ ] **4-1-2**: server side mirror push 필요 여부 평가.
- [ ] **4-1-3**: remote side packet lambda `!wasSwim && isSwim` 분기 (SmartMovingClient.java) 존재 여부 확인. 없으면 추가 검토 (1m up/down 시각 BUG 차단).
- [ ] **4-1-4**: 멀티 인게임 검증 — 다른 player 가 standing → swim 진입 시 박스/모델 위치 자연.

### 4-2. swim → standing (물 나옴) — BUG 후보

- [ ] **4-2-1**: self side: `wasSwim && !swim` → `resetSwimming` + `resetHeightOffset` 매핑 위치. entity.y 변화 / 박스 drop 검증.
- [ ] **4-2-2**: remote 측 1m down jump BUG 가능성 ([[feedback_slide_exit_willdrop_pose_kept]] 패턴 가능).
- [ ] **4-2-3**: 멀티 인게임 검증.

### 4-3. swim ↔ dive 전환

- [ ] **4-3-1**: POSE 변화 X (둘 다 SWIMMING) → mixin offset 일관 → 박스 변화 X 예상.
- [ ] **4-3-2**: motion damping 변화 (0.85 ↔ 0.83) — 시각 영향 미미 예상.
- [ ] **4-3-3**: 멀티 인게임 — surface ↔ underwater 전환 시 다른 player 박스/모델 자연.

### 4-4. shallow swim/dive → crawl 전환

- [ ] **4-4-1**: self side `L513-524` 분기 (isSlow) → `setHeightOffset(-1F) + isCrawling=true + isDipping=true`. 박스 dim 변화 확인.
- [ ] **4-4-2**: POSE 변화 (SWIMMING → SWIMMING 유지? crawl POSE 별도?) 확인.
- [ ] **4-4-3**: remote 측 packet lambda 의 isCrawling 분기 매치 확인.
- [ ] **4-4-4**: 멀티 인게임 — 얕은 물 + sneak 시 다른 player 자연 crawl 전환.

### 4-5. shallow swim/dive → walk 전환

- [ ] **4-5-1**: self side `L525-535` 분기 (!isSlow) → `resetHeightOffset` + `moveEntity(0, ground - bb.minY, 0)` y-snap.
- [ ] **4-5-2**: moveEntity 가 entity.y 변경 시 server broadcast lag 가능 → remote 측 잠긴 시각 가능.
- [ ] **4-5-3**: 멀티 인게임 — 얕은 물 + W 시 다른 player 자연 walk 전환.

### 4-6. jump-out-of-water (수면 점프)

- [ ] **4-6-1**: self side `motionY = 0.3` 1 tick 식 ([[feedback_self_to_server_remote_one_to_one]] = self 1:1).
- [ ] **4-6-2**: motionY 1 tick burst → server broadcast → remote vanilla lerp 추격 — 시각 자연 예상.
- [ ] **4-6-3**: 멀티 인게임 — 다른 player surface 점프 시 자연.

### 4-7. lava 진입 / 탈출

- [ ] **4-7-1**: handleLava 진입 시 resetSwimming + damping 0.5 + motionY -= 0.02 + 점프 식.
- [ ] **4-7-2**: 멀티 인게임 — 다른 player lava 진입 자연.

### 4-8. swim ↔ 다른 SM phase

- [ ] **4-8-1**: swim → climbing (water 안 사다리) — handleSwimming 진입 가드 `!isLiquidClimbing` 검증.
- [ ] **4-8-2**: swim → flying — handleSwimming 가드 `!isFlying`.
- [ ] **4-8-3**: swim → grab climbing — 가드 확인.
- [ ] **4-8-4**: 비행 → water (강제 swim 진입) — 자연 전환.

### 4-9. 1 칸 두께 천장 / 동굴 등 edge case

- [ ] **4-9-1**: `realTotalSwimWaterBorder = min(totalSwimWaterBorder, minPlayerSwimWaterCeiling)` 의 천장 가드 검증. 수중 동굴 시 정확 분류.
- [ ] **4-9-2**: 멀티 인게임 검증.

---

## Phase 5 — 인게임 멀티 통합 검증

> 모든 시나리오 self / remote 양측 자연 동작 확인.

- [ ] **5-1**: 단순 swim — 표면 수영 자연.
- [ ] **5-2**: 단순 dive — 잠수 자연.
- [ ] **5-3**: swim ↔ dive 자유 전환.
- [ ] **5-4**: 얕은 물 swim → walk / crawl 전환.
- [ ] **5-5**: 깊은 물 dive → surface jump.
- [ ] **5-6**: 물 → 사다리 등반.
- [ ] **5-7**: 비행 → 물 강제 진입.
- [ ] **5-8**: 폭포 / 흐르는 물 안 수영.
- [ ] **5-9**: lava 진입 / 탈출.
- [ ] **5-10**: weeping vines / kelp 안 수영.
- [ ] **5-11**: 회귀 검증 — 헤드점프 / 슬라이딩 / 크롤 / 그랩 / 비행 모두 정상.

---

## Phase 6 — 완결 선언

- [ ] **6-1**: 사용자 인게임 통과 명시 확인.
- [ ] **6-2**: 메모리 `project_swimming_complete.md` 작성.
- [ ] **6-3**: MEMORY.md 인덱스 추가.
- [ ] **6-4**: 다음 작업 영역 = **수영 애니메이션** (setAngles / ModelPart 회전 / setupTransforms).

---

## 작업 순서 권장

1. **Phase 0** (검증 only) — 코드 grep / 라인 인용 정합성 점검. 수정 X.
2. **Phase 1-3** (서버/패킷 보강) — fix 단위 commit. 인게임 검증 + 회귀 안전성.
3. **Phase 4** (멀티 시나리오 BUG fix) — 헤드점프 시리즈 패턴 1:1 적용. fix 단위 commit.
4. **Phase 5** (통합 검증) — 사용자 실측 통과.
5. **Phase 6** (완결 메모리).

---

## 회귀 차단 의무

작업 진행 중 다음 영역 절대 건드리지 X (사용자 명시 완결):
- [[project_headjump_all_complete]] — 헤드점프 single + multi (fix #79~#100).
- [[project_sliding_complete]] / [[project_sliding_animation_complete]] — 슬라이딩.
- [[project_crawl_complete]] — 엎드리기.
- [[project_grab_climbing_complete]] — 그랩 클라이밍.
- [[project_vine_animation_complete]] — 덩굴 애니메이션.
- [[project_flying_complete]] / [[project_falling_complete]] / [[project_angle_jump_complete]] — 비행/낙하/각도점프.

수영 작업이 위 영역과 교차 (가드 추가 / mixin 변경) 시 사용자 동의 + 회귀 검증 필수.
