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
| F-1 | 원본 SmartMovingSelf 비행 처리 (handleAlternativeFlying + handleLand 비행 분기 + standupIfPossible + wasFlying 엣지 + flyWhileOnGround) | 🔄 진행 — 청크 1 (handleAlternativeFlying) ✅ + 청크 2 (진입/종료 standupIfPossible/tryLanding/flyWhileOnGround/fallDistance) ✅ + getNonSlowInputSpeedFactor 정의 ✅ / 청크 3 (보조) ⏳ | 38 진행 중 |
| F-2 | 원본 SmartMovingBase.moveFlying L56-L93 + HorizontalAirDamping | ⏳ 대기 | 38 예정 |
| F-3 | 원본 SmartMovingModel isFlying 분기 L474-L520 | ⏳ 대기 | 38 예정 |
| F-4 | 1.21.1 SmartMovingFlyer + sm_animateFlying + sm_setupTransforms + sm.isFlying 갱신 + standupIfPossible 모두 read + 매핑 | ⏳ 대기 | 39 예정 |
| F-5 | 차이점 표 + 사용자 보고 10건 → BUG 매핑 | ⏳ 대기 | 39 예정 |
| F-6 | 일괄 정정 (분할 가능) | ⏳ 대기 | 39+ 예정 |
| F-7 | 빌드 + 사용자 인게임 검증 인계 | ⏳ 대기 | 40 예정 |

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
