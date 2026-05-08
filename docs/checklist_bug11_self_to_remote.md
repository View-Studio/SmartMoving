# BUG-11 self → remote 1:1 복제 체크리스트

## Phase 1 — 헬퍼 시그니처 확장 (remote 호환) ✅
- [x] `SmartMovingClientState.getGapUnderneight` 시그니처 `ClientPlayerEntity` → `AbstractClientPlayerEntity` 로 확장.
- [x] `SmartMovingClientState.getGapOverneight` 시그니처 확장.
- [x] `SmartMovingClientState.getMinPlayerSolidBetween` 시그니처 확장.
- [x] `SmartMovingClimber.getMinPlayerSolidBetween` 시그니처 확장.
- [x] self 측 호출처 자동 upcast 호환 (= `ClientPlayerEntity` 는 `AbstractClientPlayerEntity` subclass).

## Phase 2 — remote-side fix (= BUG-7 v25.3 패턴) ✅
- [x] `MixinPlayerEntityClient.sm_handleRemoteFlyingExitYSync` 제거.
- [x] `SmartMovingClientState.smPrevWasFlyingForLerpFix` / `smFlyingExitYSyncTicks` field 제거.
- [x] `SmartMovingClient.registerClientReceivers` 의 StatePayload lambda 안:
  - [x] `wasFlying` / `wasLevitating` enter-edge 검출.
  - [x] self 가드 (= `AbstractClientPlayerEntity && !ClientPlayerEntity`).
  - [x] `getGapUnderneight` / `getGapOverneight` 측정.
  - [x] `groundClose` / `standUpPossible` 가드 (self 1:1).
  - [x] 분기 매핑:
    - [x] **toSlidingOrCrawling 분기** (`isCrawling || isSliding`): `move(0, -gap, 0)` + `setPos(y+1)` + `lastRenderY/prevY +=1` + lerp cancel.
    - [x] **standUp 분기** (`standUpPossible`): `setPos(y + (1 - gap))` + lerp cancel.
    - [x] **!groundClose && !standUpPossible 분기**: 좌표 미변경 (자유 낙하).
  - [x] 안전망 (self L3602-L3607) 솔리드 push up.

## Phase 3 — lerp cancel ✅
- [x] `MixinLivingEntityAccessor.sm_setServerY(newY)` 호출.
- [x] `MixinLivingEntityAccessor.sm_setBodyTrackingIncrements(0)` 호출.

## Phase 4 — 빌드 + 검증 ✅
- [x] `gradlew compileJava` — 통과.
- [x] `gradlew compileClientJava` — 통과.

## Phase 5 — 인게임 테스트 (사용자 수행)
회귀 위험 영역:
- [ ] **비행 종료 + 지면 가까이** (= `groundClose && standUpPossible`): self 와 동일 정확히 위치. 묻힘/점프 없음.
- [ ] **비행 종료 + 1m+ 공중** (= `!groundClose`): 좌표 변경 없음 → 자연 낙하.
- [ ] **비행 종료 + 천장 막힘** (= 1칸 공간): toSlidingOrCrawling 분기 → 엎드리기 자세.
- [ ] **Levitate 종료**: 비행 종료와 동일 동작.
- [ ] **self 측 비행 종료**: 변경 없음 (자기 측은 자체 standupIfPossible).
- [ ] **BUG-7 ICC ENTER/EXIT**: 회귀 없음 (= 같은 packet handler 안 fix).
- [ ] **사용자 평가**: "살짝 플리킹" 잔존 vs 없음.

## 작업 외 (= 본 작업 범위 아님)
- server-side fix (= bit 17 디코딩 + ServerPlayNetworking lambda 안 setPos 보정). remote-side fix 결과 평가 후 추가 결정.
