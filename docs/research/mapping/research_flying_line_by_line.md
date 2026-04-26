# 비행 원본 ↔ 1.21.1 라인별 매핑 (Flying Phase)

> **목적**: SmartMoving 비행 처리 (Creative + flyingEnabled) 의 정밀 1:1 라인별 매핑.
> 사용자 보고 10건 (BUG-1+8 + BUG-25~34) 의 직접 원인 확정 + 일괄 정정 위한 진단 자료.

> **작업 prompt**: [`docs/fix/flying_phase_prompt.md`](../../fix/flying_phase_prompt.md)
> **BUG 등재**: [`docs/fix/integration_test_bugs.md`](../../fix/integration_test_bugs.md)

> **사용자 지시 (2026-04-26 세션 37)**: 비행 처리 정밀 1:1 매핑 — 추측 기반 수정 회피, 라인별 비교로 모든 차이점 식별.

> **분류 5종**: [정합] / [오역] / [누락] / [잉여] / [N/A]

---

## 진행 상태

| F-단계 | 대상 | 상태 | 세션 |
|---|---|---|---|
| F-1 | 원본 SmartMovingSelf 비행 처리 (전체 청크 1+2+3) | ✅ 완료 (세션 38-39) | 38-39 |
| F-2 | 원본 SmartMovingBase.moveFlying L56-L93 + 1.21.1 비교 | ✅ 완료 — 정확 1:1 매핑 (차이 없음) | 39 |
| F-3 | 원본 SmartMovingModel isFlying 분기 L474-L520 + 1.21.1 매핑 | ✅ 완료 — BUG-31 직접 원인 확정 | 39 |
| F-4 | 1.21.1 모두 read (F-1~F-3 매핑 표에 통합 완료) | ✅ 완료 | 39 |
| F-5 | 차이점 표 + 사용자 보고 10건 → BUG 매핑 | ✅ 완료 — F-5 통합 표 작성 | 39 |
| F-6 | 일괄 정정 (분할 가능) | ✅ 4 정정 완료 (BUG-31/25/잉여/누락) — 빌드 통과 / BUG-29 별도 / BUG-26 사용자 안내 | 40 |
| F-7 | 빌드 + 사용자 인게임 검증 인계 | ⏳ 대기 — 사용자 인게임 재현 후 잔존 BUG (BUG-27/28/29/30/32/33/34) 평가 | 40+ |

---

## 사용자 보고 10건 (세션 35 + 37)

| BUG | 증상 (사용자 보고) | 우선순위 | F-N 매핑 (예상) |
|-----|-------|---|---|
| 1+8 ✅ | 비행 시 몸 고정 / SM 비행 시스템 미작동 | 🔴 | F-4 (player.move 호출 누락 / 세션 37 해결) |
| 25 | 비행 속도 원본보다 느림 | 🔴 | F-1/F-4 (speedFactor 계산) |
| 26 | 땅에 닿아도 착지 안 됨 | 🔴 | F-1 (standupIfPossible / tryLanding) |
| 27 | 위아래 보면서 전진 속도 너무 느림 | 🟠 | F-2 (moveFlying 정규화) |
| 28 | 처음 비행 시 하늘 끝까지 날아감 | 🔴 | F-1 (jump 분기 / 0.91F 감쇠) |
| 29 | 비행 진입 뚝 끊김 | 🟠 | F-1 (heightOffset / wasFlying 엣지) |
| 30 | 비행 가만히 있을 때 팔 회전 축 다름 | 🟠 | F-3 (isFlying 팔 setAnglesXZY) |
| 31 | 몸 기울기 방향 다름 (몸 앞쪽이 하늘) | 🔴 | F-3 (sm_setupTransforms theta 부호) |
| 32 | 머리 이상하게 고정 | 🟠 | F-3 (head.pitch ANIM-01) |
| 33 | 비행 애니메이션 프레임 끊김 | 🟠 | F-4 (stats.calculate timing) |
| 34 | 비행 디테일 (각도/움직임/속도/스무스함) 다름 | 🟢 | F-3/F-4 (전반 1:1) |

---

## F-1: 원본 SmartMovingSelf 비행 처리

### F-1 청크 1 — `handleAlternativeFlying` L602-L631 (세션 38)

vs 1.21.1 `SmartMovingFlyer.handleFlying` L28-L77 (BUG-1+8 세션 37 수정 후 기준):

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L602 | `private boolean handleAlternativeFlying(float moveForward, float moveStrafing, float speedFactor, boolean handledSwimming, boolean handledLava)` | `public static boolean handleFlying(ClientPlayerEntity player, SmartMovingClientState sm, Vec3d movementInput, boolean jumping)` (L28-L29) | [정합] | 시그니처 차이 — handledSwimming/Lava 는 sm_travel_client 호출 흐름 (Swimmer→Lava→Sliding→handleFlying 순서) 으로 대체. moveForward/Strafing 은 movementInput.z/x 에서 추출. speedFactor 는 본체에서 직접 계산. |
| L604 | `boolean handleAlternativeFlying = !handledSwimming && !handledLava && sp.capabilities.isFlying && Config.isFlyingEnabled();` | L31 `if (!sm.isFlying \|\| !cfg.fly) return false;` | [정합] | sm.isFlying 갱신 (SmartMovingClientState L1298) = `cfg.isFlyingEnabled() && capabilities.flying && !isSwimming_sm && !isDiving` 가 원본 가드 모두 내포. cfg.fly 는 sm.isFlying 안에 cfg.isFlyingEnabled 통해 이미 포함 — 단순 가드로 명시. |
| L605 | `if(handleAlternativeFlying)` | (가드 통과 후 본체 진입) | [정합] | 가드 후 분기 |
| L607 | `resetSwimming();` | `MixinLivingEntityClient.sm_travel_client` Swimmer 분기 (L101+) 처리 흐름 — handleFlying 진입 시점 = 이미 swim 처리 완료 | [N/A] | 호출 위치 분리 |
| L608 | `resetClimbing();` | `MixinLivingEntityClient.sm_travel_client` L147-L153 (handleFlying 호출 직전) — `sm.isClimbing/isCrawlClimbing/isCeilingClimbing/isClimbJumping/isClimbHolding/isClimbCrawling/isClimbBackJumping = false` | [정합] | 호출 위치 동등 |
| L610 | `float moveUpward = 0F;` | L33 `float moveUpward = 0F;` | [정합] | 동일 |
| L611 | `if(esp.movementInput.sneak)` | L37 `if (player.isSneaking())` | **[오역 후보]** | ⚠️ **원본 = `movementInput.sneak` (키 입력 raw)** vs **1.21.1 = `player.isSneaking()` (vanilla pose 결과)**. SM enabled 시 forceIsSneaking 가로챔 등 영향 가능 → 키 입력과 pose 결과 불일치 케이스. **추가 검증 필요** (BUG-29/30/32 잠재 영향). |
| L613 | `sp.motionY += 0.14999999999999999D;` | L38 `player.setVelocity(vel.x, vel.y + 0.14999999999999999D, vel.z);` | [정합] | sneak 시 motionY 증가 (양수 = 하강 의도, vanilla 좌표계) |
| L614 | `moveUpward -= 0.98F;` | L40 `moveUpward -= 0.98F;` | [정합] | 동일 |
| L615 | `}` | L41 | [정합] | |
| L616 | `if(esp.movementInput.jump)` | L43 `if (jumping)` | [정합 / 의심] | `jumping` = `LivingEntity.jumping` shadow field. vanilla 가 `movementInput.jump` 키 입력으로 매 틱 갱신 → 동등. 단 BUG-18 (sm_jumpingFilter cfg.enabled 가드) 영향 검토 — SM enabled 시 `this.jumping = false` 강제 가능 (sm.isCrawling 등 조건). **비행 중 jumping 필드 정확성 검증 필요**. |
| L618 | `sp.motionY -= 0.14999999999999999D;` | L44 `player.setVelocity(vel.x, vel.y - 0.14999999999999999D, vel.z);` | [정합] | jump 시 motionY 감소 (음수 = 상승 의도) |
| L619 | `moveUpward += 0.98F;` | L45 동일 | [정합] | |
| L620 | `}` | L46 | [정합] | |
| L622 | `moveFlying(moveUpward, moveStrafing, moveForward, speedFactor * 0.05F * Config._flyingSpeedFactor.value, Options._flyControlVertical.value);` | L57-L61: `combinedFactor = SmartMovingMover.getCombinedSpeedFactor(player, cfg); flyingSpeed = combinedFactor * 0.05F * cfg.flyingSpeedFactor; moveFlying(player, moveUpward, moveStrafe, moveForward, flyingSpeed, cfg.flyControlVertical);` | **🔴 [오역]** | **🔴 BUG-25 직접 원인 확정**: 원본 `speedFactor` (SmartMovingSelf L119) = `getConfigSpeedFactor * getPotionSpeedFactor * getNonSlowInputSpeedFactor(moveForward, moveStrafing)`. **1.21.1 `combinedFactor` = `getConfigSpeedFactor * getPotionSpeedFactor` 만 — getNonSlowInputSpeedFactor 누락**. SmartMovingFlyer L52-L57 주석에서 "근사" 인정. |
| L624 | `sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);` | L69-L70: `Vec3d motion = player.getVelocity(); player.move(MovementType.SELF, motion);` | [정합] | **BUG-1+8 (세션 37) 해결됨** |
| L626 | `sp.motionX *= HorizontalAirDamping;` | L73-L74: `velAfterMove = player.getVelocity(); player.setVelocity(velAfterMove.x * 0.91F, velAfterMove.y * 0.91F, velAfterMove.z * 0.91F);` | [정합] | move() 후 감쇠 (원본 L626 도 moveEntity 후 적용 동일 순서). HorizontalAirDamping = 0.91F 하드코딩. |
| L627 | `sp.motionY *= HorizontalAirDamping;` | L74 (위와 같은 호출) | [정합] | |
| L628 | `sp.motionZ *= HorizontalAirDamping;` | L74 | [정합] | |
| L629 | `}` | L75 | [정합] | |
| L630 | `return handleAlternativeFlying;` | L76 `return true;` | [정합] | true 반환 (가드 통과 시) |
| L631 | `}` | L77 | [정합] | |

**청크 1 통계 (30 라인)**: 정합 23 / 오역 1 (L622 NonSlowInput) / 오역 후보 2 (L611 sneak / L616 jumping) / 누락 0 / 잉여 0 / N/A 4 (L607 resetSwimming + L612/L617/L621/L629 닫는괄호 등 구조)

**핵심 발견 (F-1 청크 1)**:
1. 🔴 **[오역] L622 — getNonSlowInputSpeedFactor 누락 = BUG-25 직접 원인 확정**
   - 원본 `speedFactor = getConfigSpeedFactor * getPotionSpeedFactor * getNonSlowInputSpeedFactor`
   - 1.21.1 `combinedFactor = getConfigSpeedFactor * getPotionSpeedFactor` (NonSlowInput 누락)
   - 단 사용자 보고 "느림" 과 NonSlowInput "옆/뒤 0.6배 감속" 은 부분적 모순 — getNonSlowInputSpeedFactor 의 정확한 정의 (앞 입력 시 1.0, 옆/뒤 0.6 외 다른 케이스?) 추가 확인 필요. **F-1 청크 추가 — getNonSlowInputSpeedFactor 정의 read 필요**.
2. ⚠️ **[오역 후보] L611 sneak 검사** — `movementInput.sneak` (키 raw) vs `isSneaking()` (pose 결과). SM enabled 시 forceIsSneaking 가로챔 영향 가능. 정확한 매핑 = vanilla `Input.sneaking` 또는 `MinecraftClient.options.sneakKey.isPressed()` 검토 필요.
3. ⚠️ **[오역 후보] L616 jumping 필드** — sm_jumpingFilter (BUG-18 가드) 영향 검토. 비행 중 `this.jumping` 강제 false 가능성.

**F-1 청크 1 발견 — F-5 BUG 매핑 후보**:
- BUG-25 (속도 느림) → L622 getNonSlowInputSpeedFactor 누락 [원인 확정]
- BUG-29/30/32 (진입 끊김 / 팔축 / 머리) → L611 sneak 검사 차이 잠재 영향
- BUG-28 (하늘 끝까지) → L616 jumping 필드 검토 + L613/L618 motionY 부호 검증 (sneak +0.15 / jump -0.15 — 원본 동일)

**다음 청크**: F-1 청크 2 (비행 진입/종료 처리 — L1803-L1831 + L2186-L2212 + L2542+ + getNonSlowInputSpeedFactor 정의).

### F-1 보조 — `getNonSlowInputSpeedFactor` 정의 (L197-L227, BUG-25 정확한 원인 확정)

원본 `getNonSlowInputSpeedFactor(moveForward, moveStrafing)`:

```java
float speedFactor = 1f;
if (isFast)
    speedFactor *= (!isLevitating() ? Config._sprintFactor.value : Config._sprintFactorLevitate.value);
if (isClimbing)
    if (moveStrafing != 0F || moveForward != 0F)
        speedFactor *= Config._freeClimbingHorizontalSpeedFactor.value;
    else if (wantClimbDown && ...)
        // 클라이밍 특수 처리 (비행 무관)
return speedFactor;
```

**비행 (cfg.fly enabled, isLevitating false) 시 핵심**:
- `isFast` = true (sprint 키 hold + 비행 가능 + 다른 조건) → `_sprintFactor.value` 곱셈 (기본 1.3F = 30% 가속)
- `isFast` = false → 1.0F (변화 없음)
- `isClimbing` = false (handleAlternativeFlying 진입 시 resetClimbing 호출됨) → 영향 없음

**🔴 BUG-25 정확한 원인 (Agent 가설 정정)**:
- Agent 가설 = "NonSlowInput 의 옆/뒤 0.6배 감속 누락" → ❌ **실제로는 NonSlow 가 옆/뒤 감속을 하지 않음**
- 정확한 원인 = **isFast 시 sprint 배수 (`_sprintFactor` = 1.3F) 누락** → SM 비행 sprint 시 30% 느림 (사용자 보고 일치)

**해결 방향** (F-6 정정):
- `SmartMovingFlyer.handleFlying` 또는 `SmartMovingMover` 에 NonSlowInput 헬퍼 추가
- `if (sm.isFast) flyingSpeed *= (sm.isLevitating ? cfg.sprintFactorLevitate : cfg.sprintFactor)`
- 비행 sprint (Ctrl 또는 sprint 키 hold) 시 1.3F 가속 적용

**정합 / 잉여**:
- isFast / isLevitating / isClimbing 필드 모두 1.21.1 SmartMovingClientState 에 존재 ✓
- cfg.sprintFactor / cfg.sprintFactorLevitate 모두 1.21.1 SmartMovingConfig 에 존재 ✓ (cfg.sprintFactorLevitate = 1.5F 확인됨)
- → F-6 정정 = 단순 sprint 배수 곱셈 추가만 필요. 의존 없음.

### F-1 청크 2 — 비행 진입/종료 처리 (세션 38)

#### 2-1. `standupIfPossible` L2186-L2212 vs 1.21.1 SmartMovingClientState L2575-L2605

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L2186 | `private void standupIfPossible(boolean tryLanding, boolean restoreFromFlying)` | L2575 `public void standupIfPossible(ClientPlayerEntity player, boolean tryLanding, boolean restoreFromFlying)` | [정합] | 시그니처 (player 추가) |
| L2188-L2189 | `if (heightOffset >= 0) return;` | L2576 `if (this.heightOffset >= 0) return;` | [정합] | |
| L2191 | `gapUnderneight = getGapUnderneight()` | L2578 동일 | [정합] | |
| L2192 | `groundClose = gapUnderneight < 1D` | L2579 동일 | [정합] | |
| L2193 | `gapOverneight = groundClose ? getGapOverneight() : -1D` | L2580 동일 | [정합] | |
| L2194 | `standUpPossible = gapUnderneight + gapOverneight >= 1D` | L2581 동일 | [정합] | |
| L2196-L2201 | `if (tryLanding && groundClose && standUpPossible) { isFlying=false; sp.capabilities.isFlying=false; restoreFromFlying=true; }` | L2583-L2591 `isFlying=false; player.getAbilities().flying=false; networkHandler.sendPacket(UpdatePlayerAbilitiesC2SPacket); restoreFromFlying=true;` | [정합] | 서버 sync 패킷 추가 (1.21.1 필수) |
| L2203-L2204 | `if (!restoreFromFlying) return;` | L2593 동일 | [정합] | |
| L2206 | `if (!groundClose && !sneakButton.Pressed) resetHeightOffset();` | L2595-L2599 `sneakPressed = player.isSneaking(); ... resetHeightOffset();` | [오역 후보] | ⚠️ sneakButton.Pressed (키 raw) vs isSneaking() (pose). L611 동일 패턴 |
| L2208 | `else if (standUpPossible && !(sneakButton.Pressed && grabButton.Pressed)) standUp(...);` | L2600-L2601 동일 | [정합] | grabPressed = SmartMovingKeys.grab.isPressed() ✓ |
| L2210-L2211 | `else toSlidingOrCrawling(...);` | L2602-L2604 동일 | [정합] | |

**청크 2-1 결과**: 매우 정확한 1:1 매핑. sneakButton.Pressed → isSneaking() 차이는 잠재 BUG (BUG-26 자체가 아닌 다른 BUG 영향).

#### 2-2. tryLanding 계산 L2542 vs 1.21.1 SmartMovingClientState L1338-L1344

| 원본 L | 원본 코드 | 1.21.1 매핑 | 분류 |
|---|---|---|---|
| L2542 | `tryLanding = isFlying && !Options._flyCloseToGround.value && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D` | L1338-L1341 동일 (`!cfg0.flyCloseToGround`) | [정합] |
| L2543-L2544 | `if (restoreFromFlying \|\| tryLanding) standupIfPossible(...)` | L1342-L1344 동일 | [정합] |

**🟢 BUG-26 결론 — 원본과 정확한 1:1 매핑 (의도된 동작)**:
- 원본 (1.7.10): `_flyCloseToGround` 기본값 = true → tryLanding 조건 false → 자동 착지 안 함
- 원본 SmartMovingOptions L68 주석: "To switch on/off flying close to the ground" = true 면 지면 가까이 비행 가능 (의도)
- 1.21.1: `cfg.flyCloseToGround = true` 기본값 (SmartMovingConfig L801) — 정확 1:1
- 사용자 보고 "땅에 닿아도 착지 안 됨" = **원본 의도된 동작** (점프 두 번으로 비행 종료가 정상 방법)

**해결 (F-6)**:
- 옵션 1: 사용자에게 안내 — config 에서 `flyCloseToGround = false` 설정 시 자동 착지 가능
- 옵션 2: 1:1 번역 위반하지 않는 한 기본값 변경 안 함

#### 2-3. flyWhileOnGround L1820-L1834 vs 1.21.1 MixinClientPlayerEntity.sm_flyWhileOnGround L43-L56

| 원본 L | 원본 코드 | 1.21.1 매핑 | 분류 |
|---|---|---|---|
| L1820-L1822 | `beforeOnLivingUpdate { wasCapabilitiesIsFlying = sp.capabilities.isFlying; }` | (별도 위치 — SmartMovingClientState L1302 `wasCapabilitiesIsFlying = player.getAbilities().flying;` tickEssential 안) | [정합] | 위치 분리 |
| L1825 | `afterOnLivingUpdate()` | sm_flyWhileOnGround (`@Inject(method=tickMovement, at=TAIL, order=900)`) | [정합] | TAIL inject |
| L1827 | `if (_flyWhileOnGround.value && !(sneakButton.Pressed && grabButton.Pressed) && wasCapabilitiesIsFlying && !sp.capabilities.isFlying && sp.onGround)` | L48-L52 `if (!cfg.enabled \|\| !cfg.flyCloseToGround \|\| !cfg.flyWhileOnGround) return; if (sneak+grab) return; if (sm.wasCapabilitiesIsFlying && !flying && isOnGround)` | [정합 / 의심] | ⚠️ **`!cfg.flyCloseToGround` 추가 가드** — 원본 L1827 에는 `_flyCloseToGround` 가드 없음! 1.21.1 = flyCloseToGround=true 시 sm_flyWhileOnGround skip → 자동 비행 복원 안 함. **잠재 BUG**. |
| L1829-L1830 | `sp.cameraYaw = 0; sp.prevCameraYaw = 0;` | (1.21.1 cameraYaw 필드 부재) | [N/A] | 1.21.1 vanilla 미존재 |
| L1831 | `sp.capabilities.isFlying = true;` | L53 `player.getAbilities().flying = true;` | [정합] | |
| L1832 | `sendQueue.addToSendQueue(C13PacketPlayerAbilities)` | L54 `player.sendAbilitiesUpdate();` | [정합] | 서버 sync |

**🔴 추가 발견 — sm_flyWhileOnGround 의 `!cfg.flyCloseToGround` 가드 잉여**:
- 원본 L1827 = `_flyWhileOnGround.value` 만 가드 (flyCloseToGround 무관)
- 1.21.1 L48 = `!cfg.flyCloseToGround` 추가 가드 → flyCloseToGround=true 시 자동 비행 복원 안 함
- → **flyCloseToGround=true 기본값 시 vanilla 가 자동으로 flying false 해도 SM 이 복원 안 함 → 사용자가 자동 착지 못 함 (위 BUG-26 진단 보강)**

**[잉여] 가드 제거 필요** — `!cfg.flyCloseToGround` 가드 삭제 (또는 의도적 추가라면 주석 명시 필요).

#### 2-4. fallDistance 비행 중 0 reset L1803-L1804

| 원본 L | 원본 코드 | 1.21.1 매핑 | 분류 |
|---|---|---|---|
| L1803-L1804 | `if (sp.capabilities.isFlying) sp.fallDistance = 0F;` | (1.21.1 grep 결과 = SmartMovingClimber L317 만, 비행 중 reset 누락) | **[누락]** | 🔴 비행 중 fallDistance reset 누락 → 비행 종료 직후 착지 시 누적 fallDistance 로 낙하 데미지 가능 |

**[누락] 잠재 BUG** — 비행 중 fallDistance reset 매핑 누락. 단 사용자 보고 (BUG-25~34) 와 직접 연관 없음 (게임플레이 영향만).

**청크 2 통계 (47 라인 / 4 분기)**: 정합 23 / 오역 후보 1 (sneak 검사) / 잉여 1 (sm_flyWhileOnGround flyCloseToGround 가드) / 누락 1 (fallDistance reset) / N/A 1

**핵심 발견 (F-1 청크 2)**:
1. 🟢 **BUG-26 = 원본 1:1 매핑 (의도된 동작)** — flyCloseToGround=true 기본값 시 자동 착지 안 함이 원본 동작
2. 🔴 **sm_flyWhileOnGround `!cfg.flyCloseToGround` 가드 잉여** — 원본에 없는 추가 가드. 자동 비행 복원 차단. **F-6 정정 = 가드 삭제**
3. 🔴 **비행 중 fallDistance reset 누락** — 원본 L1803-L1804 미매핑. F-6 정정 = MixinLivingEntityClient 또는 적절한 위치에 추가
4. ⚠️ **sneakButton.Pressed → isSneaking() 차이** — L611 / L2206 동일 패턴 잠재 영향

**다음 청크**: F-1 청크 3 (보조 — L80 + L633-L640 + L2320 + L2404 + L2509+) 또는 F-2 (moveFlying) / F-3 (isFlying 분기).

### F-1 청크 3 — 비행 보조 분기 (세션 39)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 |
|---|---|---|---|
| L80-L84 | `if (sp.capabilities.isFlying && !Config.isFlyingEnabled()) jumpMovementFactor = 0.05F;` (vanilla 비행 + SM disabled 시 공중 속도 억제) | `MixinPlayerEntityClient.sm_getOffGroundSpeed` L84-L90 — `if (player.getAbilities().flying && !cfg.fly) cir.setReturnValue(0.05F);` (BUG-21 가드 추가됨) | [정합] |
| L633-L640 | `handleLand`: `if (movementInput.jump && isSprintingEnabled && sprintButton.Pressed && capabilities.isFlying) motionY += sprintFactorLevitate * sprintFactorLevitateVertical;` (vanilla 비행 + sprint+jump 수직 가속) | `MixinLivingEntityClient` L172-L178 — 정확 1:1 매핑 | [정합] |
| L2320 | `boolean isLevitating = sp.capabilities.isFlying && !isFlying;` (vanilla flying + SM isFlying false = Levitate effect) | `SmartMovingClientState.isLevitating` 갱신 위치 (별도 함수) | [정합 — 추가 검증 권장] |
| L2404-L2405 | `if (esp.capabilities.isFlying && (isFlyingEnabled \|\| isLevitateSmallEnabled)) mustCrawl = false;` | `SmartMovingClientState.tickEssential` L984-L987 — 정확 1:1 매핑 | [정합] |
| L2507-L2514 | `restoreFromFlying = false; wasFlying = isFlying; isFlying = isFlyingEnabled && capabilities.isFlying && !isSwimming && !isDiving; if (isFlying && !wasFlying) setHeightOffset(-1); else if (!isFlying && wasFlying) restoreFromFlying = true;` | `SmartMovingClientState` L1297-L1300 + L1310+ 매핑 | [정합 / **잠재 BUG-29 원인**] | ⚠️ **L2511-L2512: isFlying && !wasFlying → setHeightOffset(-1)** — 비행 진입 첫 프레임 heightOffset = -1 적용 → sm_afterMove_client 의 player.setPos(y - heightOffset) = +1 보정 = **Y 1블록 점프 가능 = BUG-29 (뚝 끊김)**. 1.21.1 매핑 검증 필요. |

**청크 3 통계 (5 분기)**: 정합 5 / 잠재 BUG 1 (L2511 setHeightOffset 진입 끊김)

## F-2: 원본 SmartMovingBase.moveFlying L56-L93 (세션 39 — 정밀 1:1 매핑 결과)

| 원본 L | 원본 코드 | 1.21.1 매핑 (SmartMovingFlyer L79-L128) | 분류 |
|---|---|---|---|
| L56-L93 (전체) | yaw 기반 수평 + pitch 기반 수직 + 비표준 정규화 `sqrt(sqrt(x²+z²) + y²)` + speedFactor / total | 정확 1:1 매핑 | **[정합 — 차이 없음]** |

**🟢 F-2 결론**: SmartMovingFlyer.moveFlying = 원본 1:1 정확. 비표준 정규화 + sin/cos / signum / pitch 보정 모두 동일.

→ **BUG-27 (위아래 보면서 전진 속도 너무 느림) = moveFlying 자체 정확. BUG-25 (sprint 누락) 의 부분 증상 가능성** (sprint 미적용으로 위아래 비행 시 더 두드러짐).

## F-3: 원본 SmartMovingModel isFlying 분기 L474-L520 + 1.21.1 sm_animateFlying / sm_setupTransforms 매핑 (세션 39)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L477 | `distance = totalDistance * 0.08F` | `MixinPlayerEntityModelClient.sm_animateFlying` L578 `distance = sm.stats.totalDistance * 0.08f` (B-7 세션 32) | [정합] | |
| L478 | `walkFactor = Factor(currentSpeed, 0F, 1)` | L579 `walkFactor = smFactor(sm.stats.currentSpeed, 0f, 1f)` | [정합] | |
| L479 | `standFactor = Factor(currentSpeed, 1F, 0F)` | L580 동일 | [정합] | |
| L480 | `time = totalTime * 0.15F` | L585+ `cos(totalTime * 0.15f)` 직접 사용 | [정합] | time 변수 안 쓰고 inline |
| L481 | `verticalAngle = isJump ? Math.abs(currentVerticalAngle) : currentVerticalAngle` | (1.21.1 sm_setupTransforms 직접 currentVerticalAngle 사용) | **[누락]** | 🔴 **isJump 분기 누락** — sm.isJumping 시 abs() 적용 안 함 |
| L482 | `horizontalAngle = horizontalDistance < 0.05F ? currentCameraAngle : currentHorizontalAngle` | `MixinPlayerEntityRenderer.sm_captureBodyYaw` L148-L154 isFlying 분기 — 정확 1:1 매핑 (threshold 0.05F) | [정합] | |
| L484-L485 | `bipedOuter.fadeRotateAngleX = true; bipedOuter.rotateAngleX = (Quarter - verticalAngle) * walkFactor;` | `MixinPlayerEntityRenderer.sm_setupTransforms` L254-L258 `theta = ((float) Math.PI / 2f - sm.stats.currentVerticalAngle) * walkFactor; matrices.multiply(POSITIVE_X.rotation(theta));` | **🔴 [오역 — BUG-31 직접 원인]** | ⚠️ **vanilla setupTransforms 가 먼저 POSITIVE_Y.rotation(180 - bodyYaw) 적용 = 캐릭터 Y 180° 뒤집음 → SM 의 추가 POSITIVE_X 회전이 좌표계 뒤집힌 상태에서 적용 → 회전 방향 반대 = 사용자 보고 "몸 앞쪽이 하늘"** |
| L486 | `bipedOuter.rotateAngleY = horizontalAngle` | `sm_captureBodyYaw` (위 L482 매핑 동일) | [정합] | |
| L488 | `bipedHead.rotateAngleX = -bipedOuter.rotateAngleX / 2F` | `sm_animateFlying` 끝부분 ANIM-01 (head.pitch 처리) | [정합 / **검증 필요 — BUG-32**] | head.pitch = -theta/2 매핑 정확 단 vanilla 좌표계 차이로 효과 다를 수 있음 |
| L490-L491 | `bipedRightArm.rotationOrder = XZY; bipedLeftArm.rotationOrder = XZY;` | `sm_animateFlying` `setAnglesXZY(rightArm, ...)` (B-2 세션 2 헬퍼) | [정합] | |
| L493-L494 | `arm.rotateAngleY = (cos(time) * Sixteenth) * standFactor` (정지 시 미세 흔들림) | `sm_animateFlying` L585-L586 동일 | [정합] | |
| L496-L497 | `arm.rotateAngleZ = (cos(distance + offset) * Sixtyfourth + (Half - Sixteenth)) * walkFactor + Quarter * standFactor` | `sm_animateFlying` L587-L590 동일 | [정합] | |
| L499-L500 | `leg.rotateAngleX = cos(distance) * Sixtyfourth * walkFactor + cos(time + offset) * Sixtyfourth * standFactor` | `sm_animateFlying` L595-L598 동일 | [정합] | |
| L502-L503 | `leg.rotateAngleZ = ±Sixtyfourth` | `sm_animateFlying` L599-L600 동일 | [정합] | |

**F-3 통계 (16 분기)**: 정합 13 / 오역 1 (BUG-31) / 누락 1 (isJump 분기)

**🔴 핵심 발견 (F-3)**:

1. **BUG-31 직접 원인 확정** — vanilla setupTransforms 의 `POSITIVE_Y.rotation(180 - bodyYaw)` 좌표계 뒤집힘 + SM 의 추가 POSITIVE_X 회전 = 방향 반대.
   - 해결 (F-6): `matrices.multiply(POSITIVE_X.rotation(-theta))` 부호 반전 (또는 회전 적용 위치 변경)
2. **BUG-32 검증 필요** — head.pitch = -theta/2 매핑 정확하지만 좌표계 차이로 시각 효과 다를 수 있음. BUG-31 정정 후 재검토.
3. **L481 isJump 분기 누락** — sm.isJumping 시 `verticalAngle = Math.abs(currentVerticalAngle)` 적용 안 함. 점프 중 비행 (이중 입력) 시 영향.

## F-5 통합 — 사용자 보고 10건 → BUG 매핑 (세션 39 정밀 매핑 결과)

| BUG | 사용자 보고 | 진단 결과 (F-1~F-3) | F-6 정정 방향 |
|-----|---|---|---|
| **BUG-25** | 비행 속도 느림 | `getNonSlowInputSpeedFactor` 누락 = sprint 시 1.3F 가속 부재 (L622) | SmartMovingFlyer 또는 Mover 에 isFast 시 sprint 배수 곱셈 추가 |
| **BUG-26** | 땅에 닿아도 착지 안 됨 | 원본 1:1 매핑 (flyCloseToGround=true 기본값 = 의도된 동작) | 사용자 안내 (config 변경) |
| **BUG-27** | 위아래 전진 너무 느림 | F-2 moveFlying 정확 1:1 → BUG-25 의 부분 증상 가능성 | BUG-25 해결 후 재평가 |
| **BUG-28** | 처음 비행 시 하늘 끝까지 | jump 키 hold 시 motionY 누적 + 0.91F 감쇠 만 — 누적 속도 매우 큼 가능. **추가 검증 필요** | jump 키 한 번만 가속 (Phase A 같은) 옵션 검토 |
| **BUG-29** | 비행 진입 뚝 끊김 | L2511 setHeightOffset(-1) 첫 프레임 적용 + sm_afterMove_client 의 player.setPos(+1 보정) = Y 점프 | heightOffset 적용 timing 또는 1 프레임 지연 |
| **BUG-30** | 팔 회전 축 다름 | sm_animateFlying setAnglesXZY 정확 1:1 → vanilla Y 180° 좌표계 영향 가능 (BUG-31 동일) | BUG-31 정정 후 재평가 |
| **BUG-31** | 몸 기울기 방향 다름 | **🔴 vanilla setupTransforms POSITIVE_Y 180° 뒤집힘 + SM POSITIVE_X 누적 = 방향 반대 (직접 원인 확정)** | sm_setupTransforms 의 X 회전 부호 반전 (또는 적용 위치 변경) |
| **BUG-32** | 머리 고정 다름 | head.pitch = -theta/2 매핑 정확 → BUG-31 동일 좌표계 영향 | BUG-31 정정 후 재평가 |
| **BUG-33** | 애니메이션 프레임 끊김 | sm.stats.calculate 호출 정상 (sm_afterMove_client TAIL inject) → 다른 원인 | BUG-25 해결 후 재평가 |
| **BUG-34** | 디테일 다름 | sm_animateFlying 정확 1:1 → BUG-25/31 의 누적 효과 | BUG-25/31 해결 후 재평가 |

## F-6 일괄 정정 권장 순서

1. **🔴 BUG-31** (몸 기울기 방향) — sm_setupTransforms isFlying 분기 X 회전 부호 반전 (또는 적용 위치 변경). 모든 비행 자세에 영향. 다른 분기 (Swim/Dive/Slide/HeadJump) 도 동일 패턴 가능 → 각 분기 검토.
2. **🔴 BUG-25** (속도 느림) — SmartMovingFlyer 또는 Mover 에 sprint 배수 곱셈 추가 (`if (isFast) speedFactor *= cfg.sprintFactor`).
3. **🔴 잉여** — sm_flyWhileOnGround 의 `!cfg.flyCloseToGround` 가드 삭제.
4. **🔴 누락** — 비행 중 `fallDistance = 0` reset 추가 (MixinLivingEntityClient 적절 위치).
5. **⚠️ BUG-29** (진입 끊김) — heightOffset 적용 timing 검토 (1 프레임 지연 또는 다른 메커니즘).
6. **🟡 BUG-26** (착지) — 사용자 안내.
7. **🟢 잔존 (BUG-27/28/30/32/33/34)** — 위 1-5 정정 후 사용자 인게임 재평가.

## F-6 정정 결과 (세션 40-41)

### ✅ BUG-31 (몸 기울기) — 사용자 인게임 검증 완료 (세션 41)
- 정정: sm_setupTransforms isFlying 분기 `POSITIVE_X.rotation(theta)` → `POSITIVE_X.rotation(-theta)`
- 사용자 보고: "기우는 방향 자체는 고쳐짐" ✅
- 다른 자세 분기 (Swim/Dive/Slide/HeadJump) 부호 검증: **사용자 인게임 검증 결과 받기 전 deferred** — Swim/Dive/Slide/HeadJump 사용자 보고는 별도 BUG-3/4/5 (진동/뚝뚝뚝/망가짐) 이므로 자세 자체 X 회전 부호 영향 검증 불가능 (자세가 보이지 않을 만큼 깨진 상태). BUG-3/4/5 진단 후 결정.

### ✅ BUG-25 (속도 느림) / 잉여 / 누락 — 정정 완료 / 인게임 검증 대기 (세션 40)

### ⏳ BUG-29 (진입 끊김) — 추가 분석 결과 / 정정 deferred (세션 41)

**추가 분석**:
- 원본 SmartMovingSelf afterMoveEntity L1608-L1609: `if (heightOffset != 0F) sp.posY = sp.posY + heightOffset;` — heightOffset(-1) 시 posY -1 (매 프레임)
- 원본 setHeightOffset (L1694-L1704): boundingBox.minY -= heightOffset + height += heightOffset 만 (posY 변경 없음)
- 1.21.1 sm_afterMove_client L99-L101: `player.setPos(... y - heightOffset ...)` — heightOffset(-1) 시 y +1 (반대 방향)
- 1.21.1 매핑자 주석: "원본: setPosition(x, y - heightOffset, z) / heightOffset = -1F 시: y - (-1F) = y + 1F → 플레이어를 1블록 위로 보정" → 의도된 동작 명시

**진단 결과**:
- 부호 자체는 의도된 (vanilla 1.21.1 처리 차이로 +1 보정 필요)
- 첫 프레임 적용 = 시각적 한 프레임 +1 점프 = "뚝 끊김"
- 단순 부호 정정 X — 정정 = heightOffset 적용 timing 변경 (1 프레임 지연 / 보간) 또는 다른 메커니즘 필요

**deferred 사유**:
- BUG-31 정정으로 자세 정상화 → 사용자 체감 시각적 영향 줄어들 가능성
- 정정 = 큰 작업 (timing 변경 = 다른 SM 분기 영향)
- BUG-25/잉여/누락 + 잔존 BUG (BUG-27/28/30/32/33/34) 인게임 검증 결과 받은 후 우선순위 재평가 권장

### 🟡 BUG-26 (착지) — 사용자 안내
- config 파일에서 `flyCloseToGround = false` 설정 시 자동 착지 가능
- 원본 1:1 (의도된 동작)

### 사용자 인게임 검증 결과 (세션 41)

**확인된 정정 효과**:
- ✅ **BUG-31** (몸 기울기 방향) — "기우는 방향 자체는 고쳐짐"
- 🟡 **BUG-25** (속도) — "비행 속도 자체는 원본보다 좀더 빠른데" — 일부 개선 (sprint 효과 확인) 단 위/아래 비행은 잔존 (BUG-27 분리)

**잔존 BUG 사용자 보고**:

#### BUG-27 (위아래 W키 이동 너무 느림) — 잔존 + 신규 정밀 정보
- 사용자: "비행 속도 자체는 좀더 빠른데, 위아래 보고 앞으로 가기 키로 움직이면 엄청엄청 느리게 해당 방향으로 움직임. 즉 y축 보고 w키로 이동하는게 원본이랑 많이 다름"
- 분석:
  - moveFlying 정확 1:1 (F-2 결과)
  - divingHorizontalFactor = cos(pitch_rad), divingVerticalFactor = -sin(pitch_rad) * signum(moveForward)
  - pitch=45° + W키 = 수평 0.707, 수직 0.707 → 비표준 정규화 영향으로 motion 작음
  - 원본도 동일 처리 — **차이 = vanilla 1.21.1 의 다른 처리?** (예: vanilla flying speed 처리)
- 추가 진단 필요

#### BUG-28 (처음 비행 시 하늘 끝까지) — 잔존 / 정정 시도 X
- 사용자: "처음 점프두번 눌러서 비행진입하면 그냥 아무거도 조작 안해도 하늘끝까지 올라가는거 안고쳐짐"
- 분석:
  - SmartMovingFlyer motion 흐름: setVelocity(y - 0.15) + moveFlying(motionY += 0.98 * 0.05) + 0.91F 감쇠
  - 평형 motion = -1.02 (1.02 m/s 상승) → 무한 상승 X 이론상
  - 원본 1.7.10 = 같은 처리 → 동일 결과 예상
  - 사용자 보고 "처음" 한정 → 첫 비행만 무한 상승, 두 번째는 정상?
- 가설:
  - sm_jump (vanilla jump cancel + jumpAvoided=true) → handleJumping 이 jump 분기 진입 → tryJump 추가 motion
  - 비행 진입 시 jumpAvoided 잔존 가능성
- 추가 정보 필요 (사용자 = 두 번째 비행 확인 / 키 release 영향 / 게임 재접속 동작)

#### BUG-29 (진입 시점 아래로 내려감) — 사용자 명시 추가 정보
- 사용자: "비행 진입시 시점이 아래로 좀 내려가는 느낌인데 이 부분 원본확인해야됨"
- 분석:
  - 이전 분석 (sm_afterMove_client setPos +1) = 시점 위로 점프 가설
  - 사용자 = "아래로 내려감" = 반대 방향
  - 가능: heightOffset = -1F + sm_afterMove_client setPos(y +1) → 발 +1 (시점 +1) → eyeHeight 차이 (-0.18) 적용 후 시점 변화
  - 또는 다른 처리 영향 (vanilla EyeHeight 계산)
- 추가 진단 필요 — 원본 1.7.10 의 시점 처리 확인

#### BUG-30 (팔 회전 축) / BUG-32 (머리 고정) — 잔존
- 사용자: "비행시 가만히 있을 때 팔회전 축이 여전히 원본과 다름. 머리도 여전히 이상함"
- 분석:
  - sm_animateFlying setAnglesXZY 헬퍼 정확 1:1 (F-3 결과)
  - head.pitch = -theta/2 (ANIM-01) 정확 1:1
  - BUG-31 정정 (좌표계) 후에도 잔존 → setAnglesXZY 헬퍼 재검증 또는 다른 차이
- 추가 진단 필요

#### BUG-33/34 (부드러움 / 디테일) — 잔존 + 정밀 정보
- 사용자: "원본이랑 엄청 퀄리티가 달라. 각 동작 자체도 스무스해야되는데, 각 동작에서 다른 동작으로 넘어갈 때 모든 동작이 이어지는 느낌으로 스무스 해야됨"
- 분석:
  - sm.stats.calculate 정상 호출 (sm_afterMove_client TAIL — player.move 후)
  - sm_animateFlying 입력값 정확 1:1
  - 차이 = vanilla limbAnimator 의 보간 처리 차이?
  - 또는 vanilla 의 다른 보간 메커니즘 (예: leaningPitch, headPitch lerp)
- 추가 진단 필요 — 보간 처리 정밀 비교

#### BUG-26 (땅 닿으면 비행 해제) — 사용자 요청 반영 정정 (세션 41)
- 사용자: "비행하다가 '땅에 닿으면' 비행이 해제되야되는데 아직도 안됨"
- 정정: SmartMovingClientState.tryLanding 의 `!cfg.flyCloseToGround` 가드 삭제 (1:1 번역 위반 — 사용자 의도 우선)
- 조건 잔존: 정지 (수평 속도 < 0.003) + motionY > -0.03 → 정지 시 자동 착지
- 사용자 의도가 더 적극적 (지면 도달 즉시 착지) 면 추가 조건 변경 필요

### 다음 작업 우선순위 (사용자 보고 기반)

1. 🔴 **BUG-28** (하늘 끝까지) — 비행 사용 불가. jumping/jumpAvoided 흐름 정밀 진단.
2. 🔴 **BUG-26** ✅ 정정 완료 (세션 41) — 인게임 검증 대기.
3. 🟠 **BUG-30/32** (팔축/머리) — sm_animateFlying setAnglesXZY 재검증.
4. 🟠 **BUG-27** (위아래 W키) — moveFlying 정밀 비교 + vanilla 차이 확인.
5. 🟠 **BUG-29** (시점 아래로) — heightOffset + EyeHeight 정밀 진단.
6. 🟢 **BUG-33/34** (부드러움) — 보간 처리 정밀 비교 (BUG-25/27/30 후 재평가).

---

## F-2: 원본 SmartMovingBase.moveFlying + HorizontalAirDamping (대기 — 세션 38 예정)

대상 라인:
- L56-L93: `moveFlying(moveUpward, moveStrafing, moveForward, speedFactor, treeDimensional)` 본체
- HorizontalAirDamping 상수 정의

→ 다음 세션 작업.

---

## F-3: 원본 SmartMovingModel isFlying 분기 L474-L520 (대기 — 세션 38 예정)

대상 라인:
- L474-L520: isFlying 분기 (sm_animateFlying 본체 매핑)
- L477-L479: distance/walkFactor/standFactor 입력값
- L480: time = totalTime * 0.15F
- L481-L482: verticalAngle/horizontalAngle 계산
- L484-L520: 팔/다리/몸/머리 자세 계산

→ 다음 세션 작업.

---

## F-4: 1.21.1 비행 처리 모두 read + 매핑 (대기 — 세션 39 예정)

대상 파일:
- `SmartMovingFlyer.java` (130 라인) — 비행 motion 처리 + moveFlying 정규화
- `SmartMovingMover.java` — getCombinedSpeedFactor / getConfigSpeedFactor / getPotionSpeedFactor
- `SmartMovingClientState.java` — sm.isFlying 갱신 (L1298) / standupIfPossible (L2575+) / tryLanding (L1338)
- `MixinLivingEntityClient.java` — handleFlying 호출 (L158) + sprint 점프 (L172)
- `MixinClientPlayerEntity.java` — sm_flyWhileOnGround (L43)
- `MixinPlayerEntityModelClient.java` — sm_animateFlying (L575+)
- `MixinPlayerEntityRenderer.java` — sm_setupTransforms isFlying 분기

→ 다음 세션 작업 (F-1~F-3 완료 후).

---

## F-5: 차이점 표 + 사용자 보고 10건 → BUG 매핑 (대기 — 세션 39 예정)

F-1~F-4 완료 후 모든 [오역]/[누락]/[잉여] 종합 + 사용자 보고 10건과 매핑.

---

## F-6/F-7 (대기)

F-5 완료 후 일괄 정정 + 빌드 + 인게임 검증.

---

## 진행 메타

- **세션 37 (2026-04-26)**: 본 산출물 + flying_phase_prompt 초기 구조 작성. F-1~F-7 모두 ⏳ 대기.
- **세션 38+**: F-1~F-3 (원본 read + 매핑) 진입.
- **세션 39**: F-4~F-5 (1.21.1 매핑 + BUG 매핑).
- **세션 39+**: F-6 (정정).
- **세션 40**: F-7 (인게임 검증 인계).
