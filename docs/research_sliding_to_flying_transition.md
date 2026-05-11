# 슬라이딩 → 비행 전환 BUG 리서치 (2026-05-12)

## 사용자 보고

- **엎드리기 (isCrawling) → 비행**: jump 키 더블 탭 시 *스무스* 전환.
- **슬라이딩 (isSliding) → 비행**: jump 키 더블 탭 시 *비행 진입 안 됨*. 사용자가 *sneak 키를 떼야* (= 슬라이딩 종료) 비행 진입 가능 + 애니메이션 뒤틀림.

## 핵심 가설 차이

| 항목 | 엎드리기 (isCrawling) | 슬라이딩 (isSliding) |
|------|-----------------------|----------------------|
| EntityPose (1.21.1) | **SWIMMING** | **SLIDING** |
| isSneaking() override 결과 | true (`!cfg.crawlOverEdge && isCrawling && !isClimbing`) | **true (fix #83 으로 `\|\| isSliding` 추가)** |
| clipAtLedge override | (자동 false — vanilla 식 = isSneaking 이지만 sneaking 검사 분기 무관) | **fix #84 — isSliding 시 false 강제** |
| sm_jumpingFilter_tickNewAi setJumping(false) | isCrawling 매치 → setJumping(false) | **isSliding 매치 → setJumping(false)** |
| 박스 크기 | 0.6 × 0.6 | 0.6 × 0.6 |
| isSwimming() flag (DataTracker bit 4) | false (vanilla updateSwimming 식: sprint + 잠수 시만 true) | false (동일) |

## vanilla 1.21.1 비행 toggle 메커니즘

`ClientPlayerEntity.tickMovement` 안 비행 toggle 코드 (디스어셈블 분석, 약 byte offset 677~813):

```java
boolean bl   = this.input.jumping;   // 메서드 시작 시점 jumping (= 이전 frame 결과)
boolean bl2  = this.input.sneaking;
boolean bl3  = this.isWalking();
PlayerAbilities playerAbilities = this.getAbilities();
// ... pose/sneak/swim 처리 ...
this.input.tick(...);  // input.jumping 갱신 (= 현재 frame)
// ...
boolean bl4 = false;
if (this.ticksToNextAutojump > 0) {
    this.ticksToNextAutojump--;
    bl4 = true;
    this.input.jumping = true;  // autojump 강제
}
// ...
if (playerAbilities.allowFlying) {
    if (interactionManager.isFlyingLocked()) {
        if (!playerAbilities.flying) { playerAbilities.flying = true; sendAbilitiesUpdate(); }
    } else if (!bl /* 이전 frame jumping=false */
            && this.input.jumping /* 현재 frame jumping=true → rising edge */
            && !bl4 /* autojump 아님 */) {
        if (this.abilityResyncCountdown == 0) {
            this.abilityResyncCountdown = 7;   // 첫 탭
        } else if (!this.isSwimming()) {       // 두 번째 탭 + !swim flag
            playerAbilities.flying = !playerAbilities.flying;  // toggle
            if (playerAbilities.flying && isOnGround()) this.jump();
            sendAbilitiesUpdate();
            this.abilityResyncCountdown = 0;
        }
    }
}
```

**toggle 가드 정리**:
1. `playerAbilities.allowFlying = true` (creative)
2. `bl = false` (이전 frame jumping = false)
3. `input.jumping = true` (현재 frame jumping = true) → **rising edge**
4. `bl4 = false` (autojump 진행 X)
5. `abilityResyncCountdown != 0` (= 7 카운트다운 진행 중) → 두 번째 탭 매치
6. `!isSwimming()` (DataTracker SWIMMING flag, POSE 무관)

**가드 어디에도 isSneaking / POSE / isCrawling / isSliding 검사 없음.**

## isSwimming() vs isInSwimmingPose() 명확화

| 메서드 | vanilla 1.21.1 식 |
|--------|--------------------|
| `Entity.isSwimming()` | `getFlag(4)` (= DataTracker SWIMMING flag) |
| `Entity.isInSwimmingPose()` | `getPose() == EntityPose.SWIMMING` |

비행 toggle 가드 = `isSwimming()` (DataTracker flag) → SM 슬라이딩 (POSE=SLIDING) 매치 X.

`Entity.updateSwimming()` 식:
```java
if (isSwimming()) setSwimming(isSprinting() && isTouchingWater() && !hasVehicle());
else setSwimming(isSprinting() && isSubmergedInWater() && !hasVehicle() && fluidState.isIn(WATER));
```

= **물 잠수 + sprint** 시만 SWIMMING flag true. **SM 슬라이딩 무관**.

## 가설 — 슬라이딩 시 비행 toggle 차단 원인 후보

vanilla 비행 toggle 가드는 isSliding/POSE 검사 X 인데도 슬라이딩 시 toggle 안 됨 = *우리 매핑 또는 SM 의 부수 동작* 이 input.jumping 의 rising edge 를 깨뜨림.

### 후보 A: SM 의 jump 키 wasPressed 소비
- `SmartMovingKeys.smartJump.wasPressed()` 또는 헤드점프 차징 키가 jump 키 입력 소비 → vanilla 코드 도달 시점에 input.jumping 이미 false 또는 KeyBinding 카운터 0.
- 검증: `SmartMovingKeys` 의 jump 키 관련 `wasPressed()` 호출 위치 확인.

### 후보 B: SM 의 input.jumping 직접 reset
- `MixinKeyboardInput` / `tickJumper` / 어떤 분기가 isSliding 시 `input.jumping = false` 직접 set → vanilla rising edge 매치 X.
- 검증: `grep "input.jumping = false"` + `input.jumping` 매핑 호출 전수.

### 후보 C: tickMovement 호출 순서 — vanilla 비행 toggle vs SM tickJumper
- 1.21.1 vanilla 비행 toggle 위치 = `ClientPlayerEntity.tickMovement` 의 첫 분기 (byte offset 약 700).
- SM tickJumper / 헤드점프 차징 시작 분기는 `LivingEntity.tickMovement` 보다 앞 또는 뒤?
- 후보: SM 이 비행 toggle *이전* 에 입력 가로채면 vanilla rising edge X.

### 후보 D: abilityResyncCountdown 카운트다운 0 도달 못 함
- 슬라이딩 매 frame SM 이 input.jumping=true 강제 → 매 frame bl=true (이전 frame) → rising edge 매치 X.
- 또는 매 frame countdown reset.

### 후보 E: 애니메이션 뒤틀림 — POSE=SLIDING 인 상태에서 isFlying=true 강제 시 transition
- 슬라이딩 phase 종료 sequence (= sneak 떼기 + POSE 변경 + 박스 dim 변경) 가 *비행 진입 시점*에 시작.
- POSE=SLIDING 상태에서 비행 진입 (= setupTransforms 의 isFlying 분기 매치) + 박스 처리가 동시 발생 → 시각 뒤틀림.

## 다음 진단 단계

1. **input.jumping 매 frame 추적 로그** — 슬라이딩 phase 동안 사용자 jump 키 누름 시:
   - tickMovement HEAD: `input.jumping` 값
   - tickMovement TAIL: `input.jumping` 값
   - abilityResyncCountdown 값
   - SM tickJumper 호출 전/후 input.jumping 값
2. **SmartMovingKeys.smartJump / 헤드점프 차징 wasPressed 호출 위치** — 슬라이딩 phase 동안 소비되는지 검증.
3. **`grep -n "input.jumping"` 우리 매핑 전수** — false set / true set / read 위치 모두 분류.
4. **사용자 정확한 키 시퀀스** — "jump 두 번 탭" 인지 "jump 한 번 탭" 인지 명확화. timing 도.
5. **애니메이션 뒤틀림 frame 영상** — POSE 전환 시점 / SM state 전환 시점 분리해서 어디서 뒤틀림.

## 관련 코드 위치

- vanilla 비행 toggle: `ClientPlayerEntity.tickMovement` byte offset 677-813 (디스어셈블 `/tmp/vanilla_check/cpe.txt`).
- SM POSE 매핑: `MixinPlayerEntityClient.sm_setPose` L270-290.
- SM jumping 차단: `MixinClientPlayerEntity.sm_jumpingFilter_tickNewAi` L131-158.
- SM isSneaking 강제: `MixinEntityClient.sm_isSneaking` L137-141 (fix #83).
- SM clipAtLedge override: `MixinPlayerEntityClient.sm_clipAtLedge_slidingException` (fix #84).
- 자체 슬라이딩 발사: `SmartMovingClientState` L2055~.
- 슬라이딩 종료 분기: `SmartMovingClientState` L2294~ (sneak 떼면 isSliding=false + toCrawling).
- isFlying 식: `SmartMovingClientState` L1795 `isFlyingEnabled() && abilities.flying && !isSwimming_sm && !isDiving`.

## 미해결 질문

- 슬라이딩 → 비행 전환 시 *기능적 차단* 메커니즘이 vanilla 가드 외에 어디서 발생하는지 (가설 A~D 중 어느 것).
- 애니메이션 뒤틀림 원인 (가설 E 또는 다른 setupTransforms 분기 우선순위).
- 엎드리기 (= POSE=SWIMMING) vs 슬라이딩 (= POSE=SLIDING) 차이가 vanilla 의 어느 메서드에서 분기되는지.
