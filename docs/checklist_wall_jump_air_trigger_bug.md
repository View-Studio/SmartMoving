# 체크리스트 — Space 연타 뱅글뱅글+상승+탈출불가 BUG fix

**리서치 참조**: `docs/research_wall_jump_air_trigger_bug.md`

**root cause 2건**:
- **Cause 1**: `SmartMovingJumper.calculateSeparateCollisionAngle` NaN fallback 이 원본 NaN 가드 무력화 → 공중 wall jump 발동.
- **Cause 2**: `SmartMovingJumper.updateWallJumpState` 식 순서 역전 → jumpKey release 시 wantWallJumping 자가유지 cycle.

**사용자 verbatim 매핑**:
- "벽이 없어도 평지에서도 발동됨" → Cause 1.
- "jump키 떼면 풀리는데 우리거는 탈출이 안됨" → Cause 2.

---

## Phase 1 — 사용자 시나리오 검증 (스킵)

사용자 답변 (2026-05-27): "그것도 너가 찾아봐". 원본 식 분석으로 모드 결정 완료:
- Survival 또는 Creative+cfg.fly=false → isFlying=false → 발동 가능.
- Creative+cfg.fly=true → isFlying=true → 발동 X.

= 사용자 verbatim "서바이벌이나 비행이 disable 된 상태" 정확 매치.

## Phase 2 — Fix-A 적용 (NaN 가드 복원, Cause 1)

### Step 2-1. `calculateSeparateCollisionAngle` NaN fallback 제거

- [ ] `SmartMovingJumper.java` L717-L727 정정:
  - 시그니처에서 `movementAngle` 인자 제거.
  - 반환식: `return getHorizontalCollisionangle(posZ, negZ, posX, negX);` (NaN 그대로).
- [ ] 호출 측 L651-L653 fallback 계산 코드 제거.

### Step 2-2. `handleWallJumping` NaN 가드 추가

- [ ] L644 `if (!sm.wantWallJumping) return;` 다음에 즉시:
  ```java
  float horizontalCollisionAngle = calculateSeparateCollisionAngle(player);
  if (Float.isNaN(horizontalCollisionAngle)) return;
  ```
- [ ] 기존 L653 의 horizontalCollisionAngle 계산 코드 제거.

## Phase 3 — Fix-C 적용 (식 순서 정정, Cause 2)

### Step 3-1. `updateWallJumpState` 본문 재배치

- [ ] `SmartMovingJumper.java` L588-L626 정정. 원본 L2863-L2896 순서 1:1 복원:
  ```java
  public static void updateWallJumpState(ClientPlayerEntity player, SmartMovingClientState sm) {
      SmartMovingConfig cfg = SmartMovingConfig.Config;

      // 1. (원본 L2865-L2866) continueWallJumping false 전환 — 먼저
      boolean jumpPressed = MinecraftClient.getInstance().options.jumpKey.isPressed();
      if (sm.continueWallJumping && (player.isOnGround() || sm.isClimbing || !jumpPressed)) {
          sm.continueWallJumping = false;
      }

      // 2. (원본 L2868) canWallJumping
      boolean isWallJumpEnabled = cfg.wallUpJump || cfg.wallHeadJump;
      boolean canWallJumping = isWallJumpEnabled && !sm.isHeadJumping && !player.isOnGround()
              && !sm.isClimbing && !sm.isSwimming_sm && !sm.isDiving
              && !sm.isLevitating && !sm.isFlying;

      // 3. (원본 L2869-L2892) double click 처리
      if (cfg.wallJumpDoubleClick) {
          if (canWallJumping) {
              if (sm.jumpKeyStartPressed) {
                  if (sm.wallJumpCount == 0) {
                      sm.wallJumpCount = (int) Math.ceil(cfg.wallJumpDoubleClickTicks);
                  } else {
                      sm.triggerWallJumping = true;
                      sm.wallJumpCount = 0;
                  }
              } else if (sm.wallJumpCount > 0) {
                  sm.wallJumpCount--;
              }
          } else {
              sm.wallJumpCount = 0;
          }
      } else {
          sm.triggerWallJumping = sm.jumpKeyStartPressed;
      }

      // 4. (원본 L2894-L2896) wantWallJumping — 마지막
      sm.wantWallJumping = canWallJumping &&
              (sm.triggerWallJumping || sm.continueWallJumping ||
               (sm.wantWallJumping && jumpPressed && !player.horizontalCollision));
  }
  ```

## Phase 4 — 빌드 + 컴파일 확인

- [ ] `gradlew compileJava` (또는 적절 task) 통과.
- [ ] IDE compile 에러 없음.

## Phase 5 — 인게임 검증

- [ ] **Cause 1 차단 확인**: Survival 평지 → space 연타 → 회전 + 상승 안 함. 정상 점프만 작동.
- [ ] **Cause 2 차단 확인**: 벽 옆 공중 → space 더블클릭 → wall jump 정상 발동 → jumpKey release → 1~2 tick 안 탈출 (= wall jump 풀림 + 자유낙하).
- [ ] **회귀 확인 — wall jump 정상 동작**:
  - 벽 옆 공중 + space 더블클릭 → wall jump 정상 발동 (= 벽 반사 방향).
  - grab + space 더블클릭 → wall head jump 정상 발동.
  - wallJumpDoubleClick=false 설정 → 단일 space → wall jump 발동 (벽 있을 때).
- [ ] **회귀 확인 — 다른 점프 시스템**:
  - 일반 jumpKey 점프 정상.
  - 차지 점프 (sneak hold + space) 정상.
  - 헤드 점프 (grab + sprint + space) 정상.
  - 더블클릭 방향 점프 (a/s/d 두번 탭) 정상.
  - grab climbing jump 정상.
- [ ] log_temp.txt 추적 (= 필요 시 debug dump 추가).

## Phase 6 — fix 후 정리

- [ ] 디버그 dump 제거 (Phase 2/3 진행 중 추가했을 시).
- [ ] 메모리 업데이트:
  - 신규 메모리 파일 (정착 시):
    - `feedback_wall_jump_nan_guard_pattern.md` — NaN 가드 패턴.
    - `feedback_wall_jump_state_order_pattern.md` — continueWallJumping/wantWallJumping 식 순서 의무.
    - `project_wall_jump_air_trigger_fix_complete.md` — fix 완결.
  - `MEMORY.md` 에 새 entry 추가.
- [ ] 사용자 명시 완결 선언 받은 후 git commit:
  - 커밋 메시지: `fix(wall-jump): 공중 발동 + 탈출 불가 BUG fix — NaN 가드 복원 + 식 순서 정정`

## Phase 7 — Fix-B (deferred)

- [ ] 원본 `calculateSeparateCollisions(d, d1, d2)` 이동 시도 collision 검사 매핑. Fix-A+C 만으로 BUG 차단 시 보류. 의미적 정확도 향상 외 BUG 영향 X.

---

## 차단/보류 조건

- **Phase 5 회귀** (= wall jump 정상 발동 X) → fix 식 정밀 검증. NaN 가드 위치 또는 collision 검사 정밀도 (= 0.001 offset 너무 작음?) 별도 분석.
- **Phase 5 추가 BUG 발견** → research 재검토.

## 같은 시도 반복 금지

- `calculateSeparateCollisionAngle` 의 fallback 식 부활. 원본 의도는 NaN = wall jump skip.
- `handleWallJumping` 안에서 NaN 가드 없이 jumpAngle 계산 시도.
- `updateWallJumpState` 식 순서 (wantWallJumping 계산 → continueWallJumping false set) 유지. 원본은 반대.
