# 체크리스트 — Phase 1: 다른 player render 시 SM 자세 적용

> 기반: `docs/research_phase1_remote_player_render.md`. 옵션 A + cast.
> Phase 1 = 자세 매핑 mixin 가드 변경 (= 다른 player render 시 SM 자세 표시).

---

## Phase 1-A — 자세 매핑 mixin 가드 변경

### A-1. `MixinPlayerEntityModelClient.sm_setAnglesHead` (L116)
- [ ] 가드 `instanceof ClientPlayerEntity` → `instanceof AbstractClientPlayerEntity`.
- [ ] entity 변수 type AbstractClientPlayerEntity 로 변경.
- [ ] **input 접근 코드** 검사:
  - `player.input.sneaking` (= weeping vines 처리). 다른 player 는 `player.isSneaking()` 사용 (= server-sync flag).
  - 또는 cast 분기 (`if (entity instanceof ClientPlayerEntity local) ...`).
- [ ] override quat clear (rightArm/leftArm) — 다른 player ModelPart 도 동일 적용 가능 검증.

### A-2. `MixinPlayerEntityModelClient.sm_setAngles` (L167)
- [ ] 가드 변경.
- [ ] `firstPersonArmRender` 가드 — local 만. 다른 player 는 무관.
- [ ] sneaking 원복 — 다른 player 도 동일 처리.
- [ ] `flyingCreative` (`player.getAbilities().flying`) — 다른 player 는 abilities 미접근. server-relay state 의 isFlying 사용.
- [ ] `isFallingForReset` — `player.fallDistance` (= 다른 player 도 entity.fallDistance 접근 가능).
- [ ] `weeping/twisting vines` 검사 — 모든 player 동일.
- [ ] sm_animateXxx 호출 — 동일 (state 사용).

### A-3. `MixinPlayerEntityRenderer.sm_setupTransforms` (L466)
- [ ] 가드 `(player instanceof ClientPlayerEntity localPlayer)` → AbstractClientPlayerEntity.
- [ ] 모든 분기 SmartMovingClientState.get(player.getUuid()) 사용 (= 다른 player 의 SM state).
- [ ] `localPlayer` 변수명 → 더 일반적 이름 (예: `clientPlayer`).
- [ ] body tilt R_x 분기 모두 검사.

### A-4. `MixinPlayerEntityRenderer.sm_captureBodyYaw` (L149)
- [ ] 가드 변경.
- [ ] `localPlayer.setBodyYaw(...)`, `localPlayer.prevBodyYaw = ...` — 다른 player 도 동일 처리 (entity field).

### A-5. `MixinPlayerEntityRenderer.sm_modifyBodyYaw` ModifyArg
- [ ] 위치: ModifyArg method body. 매 setupTransforms 호출 시 인자 변경.
- [ ] entity 정보가 method body 에 어떻게 접근? — `sm_currentRenderPlayer` 같은 캡처 변수 검토.
- [ ] 자기 vs 다른 player 분기 처리.

### A-6. `MixinPlayerEntityRenderer.sm_modifyNetHeadYaw` ModifyArg
- [ ] 동일 패턴.

### A-7. `MixinPlayerEntityRenderer.sm_getPositionOffset`
- [ ] 가드 변경.
- [ ] 모든 분기 entity SM state 따라 처리.

### A-8. 컴파일 + 기본 검증
- [ ] `gradlew compileClientJava` 통과.
- [ ] Local player 자세 정상 (회귀 없음 확인 1차).

---

## Phase 1-B — input 사용 코드 분기

### B-1. `player.input.sneaking/jumping/grab` 등 input 사용 코드
- [ ] sm_setAnglesHead 의 `weepingTwistingVines` 분기 — `player.isSneaking()` 사용 가능 검증.
- [ ] 다른 mixin 도 검색.
- [ ] 다른 player 처리 시 input 미접근 → server-relay state (= isSneaking flag) 또는 entity.isSneaking() 사용.

### B-2. `player.getAbilities().flying`
- [ ] 다른 player abilities 접근 가능 — 같은 client 라 datatracker 통해 sync 됨.
- [ ] 또는 SmartMovingClientState 의 isFlying 사용 (= server-relay).

---

## Phase 1-C — static 변수 multiplayer 영향 검토 (deferred)

> Phase 1-A 후 시각 검증 + 추후 fade lerp 정확도 검증 시 진행.

- [ ] `smCachedAnimationProgress` — 매 setupTransforms 시 마지막 entity 값 잔존.
- [ ] `smCachedBodyYawNaturalDeg/LaggedDeg` — 동일.
- [ ] fade prev field — 두 player 동시 SM 시 mixed.
- [ ] 우선순위: Phase 1-A 검증 후 fade 정확도 문제 발생 시 진행.

---

## Phase 1-D — MixinModelPart override quat 검증 (deferred)

> Phase 1-A 후 슬라이딩 자세 시각 검증.

- [ ] 다른 player 슬라이딩 시 override quat 적용 검증.
- [ ] ModelPart 자체 mixin 이라 모든 player 공유 — 한 player 의 quat 가 다른 player 에 잔존?
- [ ] sm_setAnglesHead 의 override clear 가 다른 player 도 처리하는지.

---

## Phase 1-E — 인게임 검증

### E-1. 핵심 시나리오
- [ ] **rudals 비행** → Tester2 측에서 rudals 비행 자세 (슈퍼맨) 보임.
- [ ] **rudals 슬라이딩** (grab+sneak hold + 착지) → Tester2 측에서 슬라이딩 자세.
- [ ] **rudals 엎드리기** (sneak hold + 1칸 천장) → Tester2 측에서 엎드리기 자세.
- [ ] **rudals 헤드점프** → Tester2 측에서 헤드점프 자세.

### E-2. 양방향 검증
- [ ] **Tester2 비행** → rudals 측에서 Tester2 비행 자세 보임.

### E-3. 회귀 검증
- [ ] **Local player 자세** — Client1 자기 자신 자세 정상.
- [ ] **input 처리** — sneak/grab/jump 정상 작동.
- [ ] **physics** — move/box/collision 정상.

---

## Phase 1-F — 완료 처리

### F-1. 디버그 로그 정리
- [ ] SYNC-DBG 임시 로그 제거.

### F-2. 메모리 등록
- [ ] `feedback_remote_player_render_pattern.md` 또는 비슷.

### F-3. 커밋

---

## 작업 순서 요약

```
A (자세 mixin 가드 변경)
  → A-8 (1차 컴파일/회귀 검증)
  → B (input 분기)
  → E (인게임 검증)
  → C/D (deferred — 검증 후 필요 시)
  → F (완료)
```

---

## 위험 모니터링

- **input NullPointerException** — 다른 player 의 input 접근 시 NPE. cast 분기 필수.
- **smCached static 변수** — multiplayer 환경에서 mixed → fade jitter. Phase 1-C 진행 시 fix.
- **abilities.flying** — 다른 player abilities 는 datatracker sync 되지만 Mojang 처리 차이 가능. SmartMovingClientState 의 isFlying 사용이 안전.
- **render 순서** — render 시 매 entity 별 호출. 한 entity 처리 끝까지 완료 후 다음 entity.
