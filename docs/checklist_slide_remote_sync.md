# 슬라이딩 remote 동기화 fix 체크리스트

## 1. 사전 점검

- [x] `research_slide_remote_sync.md` 작성 완료.
- [x] self 측 진입 (fix #60) / 종료 (fix #62 v2 + #70) 코드 위치 확인.
- [x] remote 측 packet handler (`SmartMovingClient.registerClientReceivers`) 현재 매핑 상태 확인 — isSliding 진입/종료 누락 확정.
- [x] server 측 (`SmartMovingServer.processStatePacket`) calc dim 호출 확인 (fix #63 정착).
- [x] BUG-7 ICC EXIT 대칭 패턴 검증 — v25.3 packet lambda 즉시 setPos.

## 2. fix 구현 (`SmartMovingClient.java`)

### 2-1. 신규 변수 추가
- [ ] `boolean wasSliding = target.isSliding;` (wasIcc 직후).
- [ ] `double slideBoxMinYBefore = 0;`
- [ ] `wasSliding && entity instanceof AbstractClientPlayerEntity rs` 가드 안 `slideBoxMinYBefore = rs.getBoundingBox().minY;` (calc dim *전*).

### 2-2. isSliding 진입 분기 (= ICC EXIT 패턴 부호 반전)
- [ ] 위치: 기존 ICC EXIT 분기 직후.
- [ ] 조건: `!wasSliding && target.isSliding && entity instanceof AbstractClientPlayerEntity && !(entity instanceof ClientPlayerEntity)`.
- [ ] 처리:
  - [ ] `newY = remoteSlideEnter.getY() - 1.0;`
  - [ ] `setPosition(x, newY, z);`
  - [ ] `lastRenderY = newY;`
  - [ ] `prevY = newY;`

### 2-3. isSliding 종료 분기 (= ICC EXIT 패턴 + predictive drop 검사)
- [ ] 위치: 진입 분기 직후.
- [ ] 조건: `wasSliding && !target.isSliding && entity instanceof AbstractClientPlayerEntity && !(entity instanceof ClientPlayerEntity)`.
- [ ] 처리:
  - [ ] `newY = remoteSlideExit.getY();`
  - [ ] `willDrop = (slideBoxMinYBefore - newY) > 0.5;`
  - [ ] `if (willDrop)`:
    - [ ] `newY += 1.0;`
    - [ ] `setPosition(x, newY, z);`
    - [ ] `lastRenderY = newY;`
    - [ ] `prevY = newY;`

### 2-4. 주석
- [ ] BUG-7 v25.3 패턴 참조.
- [ ] self side fix #60 / #62 v2 / #70 1:1 복제 명시.
- [ ] self (ClientPlayerEntity) 가드 이유 명시.

## 3. 컴파일 검증

- [ ] `./gradlew compileJava` 실행 → syntax/타입 오류 없음 확인.

## 4. 사용자 인게임 검증 (golden + 회귀)

### Golden path
- [ ] remote (= 다른 player) 가 슬라이딩 시작 시 — 공중 1칸 위 시작 BUG 사라짐.
- [ ] remote 슬라이딩 → 엎드리기 종료 시 — 땅속 1m 들어감 BUG 사라짐.

### 회귀 검증
- [ ] **self 슬라이딩**: 자체 1인칭 시 변동 없음 (= 정착된 self fix 영향 X).
- [ ] **remote ICC ENTER/EXIT**: 기존 매핑 (= BUG-7 fix v25.5) 정상.
- [ ] **remote 비행 → 슬라이딩 종료 (fix #87 시나리오)**: 비행 진입 시 isSliding=false 즉시 set → calc dim 차단 → bb 정상 → willDrop=F → push 안 함.
- [ ] **remote 헤드점프 / 자체 슬라이딩 발사 (여우무빙)**: 진입 시 같은 tick 자동 cycle → wasSliding state 변화 시점 정확 동기화.
- [ ] **remote 일반 슬라이딩 짧게 종료 (sneak 떼기)**: 정상 엎드리기.
- [ ] **remote 슬라이딩 짧게 (속도 < 임계) 자동 종료**: 정상 엎드리기.

## 5. 사후 처리

- [x] 메모리 기록: `project_slide_remote_sync_complete.md` (= 완결 메모리).
- [x] `MEMORY.md` index 갱신.
- [ ] git commit (CLAUDE.md 컨벤션).

## 6. 추가 fix (정착)

### 6-1. lerp cancel (fix B)
- [x] vanilla `OtherClientPlayerEntity.lerpPosAndRotation` 이 srvY 로 entity.y 끌고 가는 BUG 발견.
- [x] 진입/종료 setPos 후 `acc.sm_setServerY(newY)` + `sm_setBodyTrackingIncrements(0)` 추가.
- [x] 메모리: `feedback_remote_setpos_lerp_cancel.md`.

### 6-2. sm_handleRemoteFlyingExitYSync 가드 (fix C)
- [x] stack trace dump 으로 root cause 확정: 슬라이딩 진입과 비행 종료 push 충돌.
- [x] 가드에 `!sm.isSliding` 추가.
- [x] 메모리: `feedback_flying_exit_sliding_guard.md`.

## 7. 잔존 BUG

- BUG D: 비행→슬라이딩 transition 시각 점프 (= deferred). `project_bug_d_flying_to_slide_visual_jump_deferred.md`.
