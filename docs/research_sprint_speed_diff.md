# 신속(SPEED potion) + Sprint 속도 차이 미해결 사이클

작성일: 2026-05-09 (세션 종료 시점)

## 사용자 보고

원본 1.7.10 SmartMoving vs 우리 1.21.1 SmartMoving — Easy 모드 + Creative + 신속 LV3 + sprint+W 단순 측정:

- 1.7.10: **5.96초**
- 1.21.1: **9.64초**
- 비율: **1.617x** (= 우리가 더 느림)

사용자 검증 사항 (다시 묻지 말 것):
- 둘 다 **Easy 모드** (= sprint exhaustion 자동 해제 무관).
- 둘 다 **Creative**.
- 둘 다 신속 **LV3**.
- 단순 **sprint+W** (= jump/grab/sneak 없음, fox movement 아님).
- `_speedUserExponent = 0` (= "Speed: 100%" 화면 확인) 통제.
- 모든 변수 통제하여 측정.

## 우리 환경 dump 측정 결과 (`log_temp.txt` `[SPRINT-SPEED-DBG]`)

```
speedAmp=2 (= LV3) isFast=true moveFwd=0.9800 moveStr=0.0000
rawSpeed=0.100000 speedFactor=2.4000 moveFlySpd=0.240000
hDamping=0.5460 preH≈postH=0.28286
getMS=0.20800 potionFactor=1.6000
```

→ **terminal motion = 0.283 m/tick** = 식 결과 정확:
```
ADD = moveFlySpd × 0.98 = 0.235.
terminal = ADD × damping/(1-damping) = 0.235 × 0.546/0.454 = 0.283.
```

## vanilla 1.7.10 vs 1.21.1 disasm 검증 결과

vanilla 1.7.10 jar 직접 disasm (`/c/Users/user/AppData/Roaming/.minecraft/versions/1.7.10/1.7.10.jar`):

| 메서드 / 식 | vanilla 1.7.10 (직접 disasm) | vanilla 1.21.1 (직접 disasm) | 동등 |
|------|------|------|------|
| `EntityLivingBase.moveFlying(FFF)V` (sa.a) | `f4 = strafe²+forward²; if(<1) return; f4 = sqrt; if(<1) f4=1; speed/=f4; ... motion += rotated` | `Entity.movementInputToVelocity` (Vec3d, F, F)Vec3d → 동일 식 결과 | ✓ |
| `EntityLivingBase.bk()` | `return false;` (기본) | (1.21.1 동등) | ✓ |
| `EntityLivingBase.bl()` (getAIMoveSpeed) | `bk() ? bp : 0.1F` | (1.21.1 `getMovementSpeed = movementSpeed field`) | ✓ |
| `EntityPlayer.bl()` override | `(float) getAttribute(movementSpeed).getValue()` | 동일 식 | ✓ |
| `EntityAttributeInstance.computeValue` | MULTIPLY_TOTAL `e *= (1+amount)` 누적 곱 | ADD_MULTIPLIED_TOTAL `e *= (1+amount)` 누적 곱 | ✓ (동일 식, 이름만 다름) |
| `SPRINTING_SPEED_BOOST.value` | 0.30000001192092896 | 0.30000001192092896 | ✓ |
| SPEED potion modifier value | 0.20000000298023224 | 0.20000000298023224 | ✓ |
| movementSpeed base | 0.10000000149011612 | 0.10000000149011612 | ✓ |
| Block.slipperiness ground default | 0.6F | 0.6F | ✓ |

→ **vanilla motion 식 100% 동일**. dump `getMS=0.208` (= `0.1 × 1.3 × 1.6` sprint+LV3) 양 버전 동등.

## 우리 매핑 식 1:1 검증

원본 SmartMovingSelf (1.7.10) vs 우리 매핑:
| 위치 | 원본 식 | 우리 매핑 식 | 결과 |
|------|---------|--------------|------|
| `getConfigSpeedFactor` | `speedFactor × userSpeedFactor` | `cfg.speedFactor × isCreative ? userSpeedFactor : 1F` | 1.0 |
| `getPotionSpeedFactor` (fix #44) | `getAIMoveSpeed × 10 / sprintMul` = `0.208×10/1.3` | `base × sprintMul × speedMul × slowMul × 10 / sprintMul` = `0.1×1.3×1.6×1×10/1.3` | 1.6 |
| `getNonSlowInputSpeedFactor` | `if (isFast) × sprintFactor (1.5)` | 동일 | 1.5 |
| `getSlowInputSpeedFactor` | `1F` (no sneak/crawl/item) | 동일 | 1.0 |
| `landMotion rawSpeed` (fix #45) | `onGround ? 0.1×f3 : jumpMovementFactor/(sprint?1.3:1)` | `onGround ? 0.1×f3 : 0.02F` (1.7.10 결과식 동등) | 0.1 |
| `f3` 식 | `0.16277/(slip×0.91)³` = 1.000 | 동일 | 1.000 |
| `applyLandMoveFlying` (fix #49) | vanilla EntityLivingBase.moveFlying 단순 ADD | 동일 | ADD = 0.2352 |
| `setLandMotions` | `motionY -= 0.08; *=0.98; motion *= damping (0.546)` | 동일 | terminal × 0.546 |

→ **모든 식 1:1**. 결과 motion = **0.283 m/tick** (= dump 검증 일치).

## 차이 1.617x 의 원인 — 미해결

dump 측정 0.283 = 식 결과. vanilla 1.7.10 식 동등 → 식 결과 0.283 동등이어야. 그러나 사용자 측정 1.7.10 = 0.458 m/tick (= 1.617x).

코드 식 1:1 + dump 정확 인 상황에서 1.617x 차이의 가능 원인 (= 다음 세션 진단):

### 가설 A — 사용자 측정 거리 D 가 양 환경 다름

사용자가 *동일 거리 D 측정* 가정했지만 실제로 *다른 거리*:
- 1.7.10 측정: D₁ 블록 / 5.96초 = m/s.
- 1.21.1 측정: D₂ 블록 / 9.64초 = m/s.
- 만약 D₁ = D₂ → 비율 1.617 = 속도 비율.
- 만약 D₁ ≠ D₂ → 비율 다른 의미.

**검증 — 다음 세션 우선**: 사용자 1.21.1 환경 9.64초 측정 시 *시작 X/Z ↔ 종료 X/Z 차이* 정확 D 알려달라 요청.

식 결과 = `0.283 × 9.64 × 20 = 54.6 블록`.

- 만약 D₂ ≈ 54 → dump 정확 = 식 결과. 1.7.10 환경 추가 보너스 진단 필요.
- 만약 D₂ < 54 → 우리 매핑이 dump 외 위치에서 추가 감속. 추가 dump 추가 후 위치 발견.

### 가설 B — dump 외 위치 추가 motion 감속

dump 위치 = `Mover.handleLand` 끝 `setVelocity(newX, newY, newZ)` 직후. 그 *후*:
1. `MixinLivingEntityClient.sm_travel_client` `ci.cancel()` + `return`.
2. vanilla `LivingEntity.tickMovement` travel 호출 후 처리 (freezing/riptide/cramming) — 평지 sprint 무관.
3. vanilla `ClientPlayerEntity.tickMovement` super.tickMovement() 후 sprint 자동 해제 검사 — 영향 없음.

→ dump 후 추가 motion 변경 *논리적으로 없음*. 그러나 *실측이 dump 와 다르면* 위 가설 A.

**검증 방법**: tickMovement TAIL inject 추가 dump → travel 후 final motion 측정. dump 직접 비교.

### 가설 C — vanilla 1.7.10 추가 식 (우리가 못 찾은)

vanilla 1.7.10 jar disasm 까지 검증 → 식 동등. 추가 식 발견 X.

가능성: Forge 1.7.10 patches 또는 PlayerAPI 의 추가 motion hook. 검증 안 됨.

**검증 방법**: 사용자 1.7.10 환경에 SmartMoving source println 추가 후 build → motion 직접 측정 → 우리 dump 와 비교.

## 다음 세션 우선 진행

1. 사용자에게 *1.21.1 환경 9.64초 측정 거리 D 정확값* 요청 (= 한 번만).
2. D 결과에 따라:
   - D ≈ 54 → 가설 A 폐기, dump 정확. 1.7.10 환경 추가 보너스 진단 (가설 C 시도).
   - D < 54 → 가설 B. tickMovement TAIL dump 추가 + 사용자 측정 비교 → motion 추가 감속 위치 발견.

## 측정 환경 dump 위치 (Mover.java L296-336)

```java
boolean _sprintSpeedDbg = player.isOnGround() && player.isSprinting()
        && player.getStatusEffect(StatusEffects.SPEED) != null
        && !sm.isHeadJumping && !sm.isSliding && !sm.isCrawling;
Vec3d _preSprintVel = _sprintSpeedDbg ? player.getVelocity() : null;
float _preSprintSpeedFactor = _sprintSpeedDbg ? speedFactor : 0F;
float _preSprintRawSpeed = _sprintSpeedDbg ? rawSpeed : 0F;
applyLandMoveFlying(...);
...
setLandMotions + setVelocity;
if (_sprintSpeedDbg && tick % 10 == 0) {
    println("[SPRINT-SPEED-DBG] tick=... speedAmp=... isFast=... moveFwd=... moveStr=... "
        + "rawSpeed=... speedFactor=... moveFlySpd=... hDamping=... "
        + "preH=... postH=... ADD=(...,...) getMS=... potionFactor=...");
}
```

## 적용된 fix 목록 (이번 세션)

| Fix | 위치 | 상태 |
|-----|------|------|
| #44 | `Mover.getPotionSpeedFactor` (이전 세션) | 적용 완료 (식 검증 = 1.7.10 동등) |
| #45 | `Mover.handleLand` air 분기 rawSpeed = 0.02F | 적용 완료 |
| #46 | `Mover.handleLand` air 분기 isAerodynamic 0.999F | 적용 완료 |
| #47 | `tickEssential` 5-AND 직전 `if (isHeadJumping&&isSliding) wasHeadJumping=false` | 적용 완료 |
| #48 | `SS-SlideStop` 종료 분기 `isAerodynamic=true` 강제 | 적용 완료 |
| #49 | `Mover.applyLandMoveFlying` vanilla 단순 ADD 식 | 적용 완료 |
| dump | `[SPRINT-SPEED-DBG]` ground sprint+SPEED 매 10 tick | 적용 완료 |

fix #44-#49 효과:
- fox movement (일반/점프강화) 1:1 정착 (사용자 명시: "원본과 거의 동일").
- 신속+sprint 속도 1.617x 차이 — **미해결** (다음 세션).
