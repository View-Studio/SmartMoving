# 수영 / 잠수 강화 (Swimming / Diving) 기능 리서치

> 원본 소스: `SmartMovingSelf.java`, `SmartMovingBase.java`

---

## 관련 클래스/메서드

| 위치 | 메서드 | 역할 |
|------|--------|------|
| `SmartMovingSelf` | `handleSwimming()` | 수영 상태 결정 |
| `SmartMovingSelf` | `moveEntityWithHeading()` | 수중 3D 이동 처리 |
| `SmartMovingBase` | `getLiquidBorder()` | 수면 높이 계산 |
| `SmartMovingBase` | `getMaxPlayerLiquidBetween()` | 플레이어 범위 내 최대 수위 |
| `SmartMovingBase` | `reverseHandleMaterialAcceleration()` | 물 흐름 저항 역적용 |

---

## 동작 원리

```
매 틱 수위 감지
  getLiquidBorder() → 각 블록 수위
  getMaxPlayerLiquidBetween() → 플레이어 범위 최대 수위
  playerSwimWaterBorder = 최대수위 - 플레이어Y - 보정값
  ↓
offset 범위로 상태 결정
  < 1.4  → isDipping
  < 1.9  → isSwimming
  ≥ 1.9  → isDiving
  ↓
상태별 감쇠 + 부력 적용
  ↓
잠수: 피치 각도 → 3D 방향 벡터
```

---

## A. 수위 감지

### getLiquidBorder() — 블록별 수위 계산
```java
// 흐르는 물 (metadata >= 8): 1.0F
// 고인 물 (metadata 0): 0.875F
// 일반적: (8 - meta) / 8F
// 비액체: 0F
float border = (8 - blockMetaData) / 8F;
```

| 메타데이터 | 수위 |
|----------|------|
| 0 (꽉 찬 블록) | 0.875F |
| 1 | 0.750F |
| 4 | 0.500F |
| 7 | 0.125F |
| 8+ (흐름) | 1.000F |

### 플레이어 수위 오프셋 계산
```java
double totalSwimWaterBorder = getMaxPlayerLiquidBetween(
    sp.boundingBox.maxY - 1.8,
    sp.boundingBox.maxY + 1.2
);
double playerSwimWaterBorder = totalSwimWaterBorder - j - j_offset;
// j_offset = 0.1625D (보정값)
```

---

## B. 3가지 수중 상태와 정확한 임계값

| offset 범위 | 상태 | 설명 |
|------------|------|------|
| 0 ~ 1.4 | `isDipping` | 부분 잠수 (목 이하) |
| 1.4 ~ 1.9 | `isSwimming` | 수면 수영 |
| ≥ 1.9 | `isDiving` | 완전 잠수 |

### 감쇠 계수

| 상태 | X/Z 감쇠 | Y 감쇠 |
|------|---------|--------|
| `isDipping` | 0.80D | 0.83D |
| `isSwimming` | 0.85D | 0.85D |
| `isDiving` | 0.83D | 0.83D |

---

## C. 부력 그라디언트 (Swimming 상태)

수면 높이에 따른 세밀한 Y 속도 조정:

```
offset 1.500: motionY -= 0.000625D
offset 1.600: motionY -= 0.00125D
offset 1.620: motionY -= 0.0025D
offset 1.640: motionY -= 0.005D
offset 1.660: motionY -= 0.01D
offset 1.668: motionY -= 0.015D
offset 1.672: motionY -= 0.02D
```
→ 수면에 가까울수록 부력 증가. 자연스러운 수면 유지 효과.

---

## D. 잠수 3D 방향 (Pitch 기반)

```java
float rotation = treeDimensional ? sp.rotationPitch / RadiantToAngle : 0;
float divingHorizontalFactor = MathHelper.cos(rotation);  // 수평 속도 배수
float divingVerticalFactor   = -MathHelper.sin(rotation) * Math.signum(moveForward);
// 전진 방향으로 피치 → Y 속도 결정
// 위를 보면서 전진 → 상승
// 아래를 보면서 전진 → 하강
```

**결과:** 시선 방향으로 3D 자유 이동. `moveForward` 부호로 전진/후진 방향 반전.

---

## E. 상태 전환 로직

```
물 진입
  ↓
isDipping (offset < 1.4)
  - Y 속도 약한 하방 당김 (-0.02D)
  - 수평 감쇠 0.80D
  ↓ (offset >= 1.4)
isSwimming
  - 부력 그라디언트 적용
  - 수평/수직 감쇠 0.85D
  - speedFactor *= _swimSpeedFactor
  ↓ (offset >= 1.9)
isDiving
  - 3D 피치 기반 이동
  - 수평/수직 감쇠 0.83D
  - speedFactor *= _diveSpeedFactor
```

---

## F. 물 탈출 점프

```java
// isDipping 상태에서 위로 이동 시
if (isDipping && movingUp) {
    sp.motionY = 0.30000001192092896D; // 고정 탈출 속도
    // 소리: "random.splash", vol=0.05F, pitch=1.0F ± 0.4F (랜덤)
}
```

---

## G. reverseHandleMaterialAcceleration()

물 흐름의 가속 효과를 역으로 적용 (저항으로 전환):
```java
// 바닐라: motionX += vec.x * 0.014D  (흐름 방향으로 가속)
// SmartMoving: motionX += vec.x * (-0.014D)  (흐름 반대로 저항)
```

---

## H. 설정 플래그

| 설정 | 역할 |
|------|------|
| `Config.isSwimmingEnabled()` | 수영 강화 마스터 토글 |
| `Config.isDivingEnabled()` | 잠수 기능 마스터 토글 |
| `Config._swimSpeedFactor.value` | 수영 속도 배수 |
| `Config._diveSpeedFactor.value` | 잠수 속도 배수 |
| `Config._baseExhautionLossFactor.value` | 수중 탈진 소모 기본값 |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `getLiquidBorder()` | `FluidState.getHeight(WorldView, BlockPos)` |
| `getMaxPlayerLiquidBetween()` | `Entity.getFluidHeight(FluidTags.WATER)` |
| `motionX *= 0.85D` | `@Inject(LivingEntity.travel())` 수중 처리 블록 |
| `sp.rotationPitch / RadiantToAngle` | `entity.getPitch() × (π/180)` |
| `reverseHandleMaterialAcceleration()` | `@Redirect` 또는 `@ModifyVariable` in `travel()` |
| 3D 잠수 방향 | `Vec3d`로 직접 속도 설정 |

---

## 미확인 / 추가 조사 필요

- [ ] 1.21.1 `FluidState.getHeight()` 가 SmartMoving의 `getLiquidBorder()` 결과와 동일한지
- [ ] `Entity.getFluidHeight()` 로 수중 깊이 오프셋 직접 계산 가능한지
- [ ] 바닐라 1.21.1 수영과 SmartMoving 수영의 충돌 — `isSwimming()` 플래그 오버라이드 방법
- [ ] 잠수 중 산소(Oxygen/Air) 바 연동 방식 확인
- [ ] `reverseHandleMaterialAcceleration`을 `@Redirect`로 구현 시 다른 모드와 호환성
