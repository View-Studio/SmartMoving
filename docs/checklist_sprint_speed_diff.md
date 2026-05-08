# 신속+Sprint 속도 차이 작업 체크리스트

리서치: `docs/research_sprint_speed_diff.md`

## 완료 (2026-05-09)

### 식 1:1 매핑 검증
- [x] vanilla 1.21.1 LivingEntity.travel disasm — `applyMovementInput` + `setVelocity(× fxx)` 식 분석.
- [x] vanilla 1.21.1 EntityAttributeInstance.computeValue disasm — `ADD_MULTIPLIED_TOTAL = e *= (1+amount)` 누적 곱 (= 1.7.10 MULTIPLY_TOTAL 동등).
- [x] vanilla 1.21.1 SPRINTING_SPEED_BOOST disasm — `value=0.30000001192092896, ADD_MULTIPLIED_TOTAL`.
- [x] vanilla 1.21.1 StatusEffects SPEED/SLOWNESS amount disasm — 0.2/-0.15.
- [x] vanilla 1.21.1 movementInputToVelocity disasm — `d > 1 ? normalize : input` × speed.
- [x] vanilla 1.7.10 jar (= obfuscated) 직접 disasm:
  - [x] sv.class (= EntityLivingBase) — bk()=false 기본, bl()=`bk()?bp:0.1F`.
  - [x] yz.class (= EntityPlayer) — bl() override = `(float) attribute.getValue()`.
  - [x] sa.class (= Entity) — a(FFF)V (moveFlying) = vanilla 식 동등.
  - [x] aji.class (= Block) — slipperiness default 0.6F.
- [x] 식 결과 1.7.10 = 1.21.1 = 0.208 (sprint+LV3 attribute) 검증.

### 우리 매핑 식 적용
- [x] fix #44 — `getPotionSpeedFactor` vanilla attribute 우회 + 1.7.10 식 직접 계산. (이전 세션 적용)
- [x] fix #45 — `Mover.handleLand` air rawSpeed = 0.02F hardcoded.
- [x] fix #46 — `Mover.handleLand` air `else if (isAerodynamic) horizontalDamping = 0.999F`.
- [x] fix #47 — `tickEssential` 5-AND 직전 `if (isHeadJumping&&isSliding) wasHeadJumping=false` 강제.
- [x] fix #48 — `SS-SlideStop` 종료 분기 case B fox 시 `isAerodynamic=true` 강제.
- [x] fix #49 — `Mover.applyLandMoveFlying` vanilla 1.7.10 EntityLivingBase.moveFlying 단순 ADD 식 1:1 (sqrt(sqrt) → 단순 sqrt).

### 검증 dump 추가
- [x] `[SPRINT-SPEED-DBG]` (Mover.java) — ground sprint+SPEED 매 10 tick 출력.
  - 측정값 검증: rawSpeed=0.1, speedFactor=2.4, moveFlySpd=0.24, hDamping=0.546, getMS=0.208, potionFactor=1.6, terminal=0.283.
  - 모두 식 결과와 일치.

### 사용자 보고 효과 확인
- [x] fox movement 일반: 거의 1:1 (사용자 "fix #47/#48 후 OK").
- [x] fox movement 점프강화: 거의 1:1.
- [x] fox movement 키 떼면 끊김 fix (fix #48).
- [ ] 신속+sprint 속도: **여전히 1.617x 느림** (= 미해결).

## 미해결 (다음 세션 우선)

### 사용자 1.21.1 환경 거리 D 정확값 측정
- [ ] 사용자에게 9.64초 측정 시 *시작 X/Z ↔ 종료 X/Z 차이* 정확 D 알려달라 요청 (한 번만).
- [ ] D 결과 분기:
  - [ ] **D ≈ 54 블록** (= dump 0.283 × 9.64 × 20) → 가설 A 폐기. dump 정확 = 식 결과. 다음 단계: 1.7.10 환경 추가 보너스 진단 (가설 C).
  - [ ] **D < 54 블록 (예: 30-40)** → 가설 B. dump 후 추가 감속 위치 발견 위해 `tickMovement` TAIL inject dump 추가 + 사용자 측정 비교.

### 가설별 진단 단계

#### 가설 B 진행 (D < 54 시)
- [ ] `MixinClientPlayerEntity.tickMovement` TAIL inject 에 `[FINAL-MOTION-DBG]` 추가.
- [ ] 사용자 측정 → tickMovement TAIL motion vs Mover.handleLand 끝 dump 비교.
- [ ] 차이 발견 시 → vanilla LivingEntity.tickMovement 의 *우리 매핑 cancel 후 처리* 또는 *우리 mod 다른 mixin* 발견.

#### 가설 C 진행 (D ≈ 54 시)
- [ ] 1.7.10 환경 측정 mod 추가 — SmartMoving source `SmartMovingSelf.java:719` 직후 println:
  ```java
  if (sp.isSprinting() && sp.getActivePotionEffect(Potion.moveSpeed) != null) {
      System.out.println("[ORIG-DBG] motionX=" + sp.motionX + " motionZ=" + sp.motionZ
          + " mag=" + Math.sqrt(sp.motionX*sp.motionX + sp.motionZ*sp.motionZ)
          + " AIMoveSpeed=" + sp.getAIMoveSpeed()
          + " jumpMovementFactor=" + sp.jumpMovementFactor);
  }
  ```
- [ ] 사용자 build 환경 가능 여부 확인.
- [ ] 1.7.10 측정값 = 0.458 m/tick 확정 시 — 우리 매핑 어딘가 누락 (= 식 1:1 검증과 모순. 추가 검증 필요).

## 메모리 갱신 (이미 적용)

- `feedback_air_sprint_rawspeed.md` — fix #45 식.
- `feedback_aerodynamic_damping.md` — fix #46 식.
- `feedback_fox_caseB_wasHeadJumping.md` — fix #47 식.
- `feedback_moveFlying_landBranch_overload.md` — fix #49 식.
