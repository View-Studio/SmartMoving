# Grab 키 클라이밍 체크리스트

> 리서치: `docs/research/research_grab_climbing.md`
> 핵심: `MixinLivingEntityClient.travel` 의 `onClimbable` 가드에 `sm.wantClimb` 추가.

---

## 0. 사전 확인

- [x] 원본 `SmartMovingSelf.handleClimbing` (L814-946) read.
- [x] 원본 `SmartMovingSelf` grab 입력 처리 (L2387, L2467-2493) read.
- [x] 원본 `SmartMovingSelf.setOnlyShouldClimbSpeed` (L1500-1551) read.
- [x] 우리 `SmartMovingClimber.handleClimbing` (L289-651) read — Free Climb 본문 이식 확인.
- [x] 우리 `MixinLivingEntityClient.travel` (L180-213) read — break point 확인.

## 1. 사전 검증

- [x] **1-1**. `SmartMovingClientState.tickEssential` 에서 `wantClimb` 필드 갱신 — L1262 `wantClimb = cfg0.freeClimb && cfg0.enabled && wouldWantClimb` 확인.
- [x] **1-2**. `SmartMovingConfig.freeClimb` 기본값 = `true` 확인 (L464).
- [x] **1-3**. `SmartMovingClientState.java:247` `public boolean wantClimb` 정의 확인.

## 2. 수정 — `MixinLivingEntityClient.travel`

- [x] **2-1**. `MixinLivingEntityClient.java:198-211` 의 `onClimbable` 가드 수정 완료 — `wantFreeClimb = sm.wantClimb` 추가 게이트.
  ```java
  // 변경 전
  boolean onClimbable = hands[0].isRelevant() || feet[0].isRelevant();
  if (!onClimbable && !sm.isCeilingClimbing) return;
  if (onClimbable) SmartMovingClimber.handleClimbing(player, sm);
  ```
  →
  ```java
  // 변경 후
  boolean onClimbable = hands[0].isRelevant() || feet[0].isRelevant();
  // Free Climb (일반 벽 grab) 는 onClimbable 무관 — handleClimbing 안에서 seekClimbGap 8방향 검사.
  // 원본 SmartMovingSelf L657 은 handleClimbing 무조건 호출. 우리는 wantClimb 시 호출.
  boolean wantFreeClimb = sm.wantClimb;  // tickEssential 에서 freeClimb && grab 검증된 값
  if (!onClimbable && !sm.isCeilingClimbing && !wantFreeClimb) return;
  if (onClimbable || wantFreeClimb) SmartMovingClimber.handleClimbing(player, sm);
  ```

## 3. 빌드 검증

- [x] **3-1**. `./gradlew compileClientJava` 통과.

## 4. 코드 리뷰

- [ ] **4-1**. `sm.wantClimb` 가 실제로 `tickEssential` 에서 매 tick 갱신되는 것 확인.
- [ ] **4-2**. `handleClimbing` 안의 Free Climb 분기 (L587-) 가 `wantClimb`/`wantClimbUp`/`wantClimbDown` 으로 자체 가드 → 진입은 OK 가 됐어도 8방향 `seekClimbGap` 후 무발동 시 빠르게 return.
- [ ] **4-3**. Standard/Simple/Smart Base Climb 분기 (L306-585) 는 `horizontalCollision` 검사 그대로 → Free Climb 외 케이스 무영향.
- [ ] **4-4**. `isCeilingClimbing` 분기 변경 없음.

## 5. 천장 클라이밍 블록 검사 누락 (★ 추가 발견)

- [x] **5-1**. 원본 `SmartMovingSelf.handleCeilingClimbing` L1139-1145 의 `supportsCeilingClimbing` 검사 read.
- [x] **5-2**. 우리 `CeilingClimbBlocks.supports(BlockState)` 헬퍼 이미 이식 확인 (climbing/CeilingClimbBlocks.java).
- [x] **5-3**. `SmartMovingClimber.handleCeilingClimbing` 에 `CeilingClimbBlocks.supports(topState/bottomState)` 호출 추가.
- [x] **5-4**. 빌드 통과.

## 6. 인게임 테스트 (deferred)

- [ ] **5-1**. 일반 벽 앞에서 grab (LEFT_CTRL) + W → 위로 등반.
- [ ] **5-2**. grab + S → 아래로 등반.
- [ ] **5-3**. grab 떼면 정지/낙하.
- [ ] **5-4**. 사다리에서 기존 동작 유지 (자동 등반).
- [ ] **5-5**. 덩굴 (벽 옆) 에서 자동 등반 유지.
- [ ] **5-6**. 천장 클라이밍 (CeilingClimb) 동작 유지.
