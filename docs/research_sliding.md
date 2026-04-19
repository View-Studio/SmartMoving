# 슬라이딩 (Sliding) 기능 리서치

> 원본 소스: `SmartMovingSelf.java`, `SmartMovingBase.java`

---

## 관련 클래스/메서드

| 위치 | 메서드 | 역할 |
|------|--------|------|
| `SmartMovingSelf` | `updateEntityActionState()` | 슬라이딩 상태 결정 |
| `SmartMovingSelf` | `moveEntityWithHeading()` | 슬라이드 방향 조정 적용 |
| `SmartMovingSelf` | `setHeightOffset(-1F)` | 히트박스 축소 |
| `SmartMovingBase` | 마찰 공식 | 블록 미끄러움 기반 감속 계산 |

---

## 동작 원리

```
지면 스프린트 중 스니크 버튼 눌림 (StartPressed)
  + grabButton.Pressed
  + !isCrawling && !isDipping
  ↓
isSliding = true
  ↓
setHeightOffset(-1F) → 히트박스 축소
  ↓
매 틱: 마찰 공식으로 수평 속도 감쇠
      strafing 입력으로 진행 방향 미세 조정
  ↓
속도 < _slidingSpeedStopFactor × 0.01 또는 스니크 해제
  ↓
isSliding = false, resetHeightOffset()
```

---

## A. 진입 조건

```java
boolean canStartSlide =
    Config.isSlidingEnabled() &&
    grabButton.Pressed &&
    (isGroundSprinting || (wasRunning && !isRunning && sp.onGround)) &&
    !isCrawling &&
    sneakButton.StartPressed &&  // 스니크 버튼 누른 순간 (1프레임)
    !isDipping;

if (canStartSlide)
    isSliding = true;
```

**핵심:** `sneakButton.StartPressed` — 누르는 순간(1프레임)에만 진입 판정.

---

## B. 해제 조건

```java
if (isSliding && (
    !sneakButton.Pressed ||
    horizontalSpeedSquare < Config._slidingSpeedStopFactor.value * 0.01
)) {
    isSliding = false;
}
```

| 해제 조건 | 설명 |
|----------|------|
| `sneakButton.Pressed = false` | 스니크 버튼 떼면 즉시 해제 |
| 속도 < 임계값 | `_slidingSpeedStopFactor × 0.01` 미만 시 해제 |

---

## C. 물리 변경

### 히트박스
```java
// 슬라이딩 진입 시 크롤과 동일한 히트박스 축소
setHeightOffset(-1F);
move(0, -1D, 0, true); // 1블록 아래로 이동 (웅크림 포지션)
```

| 항목 | 기본 | 슬라이딩 중 |
|------|------|-----------|
| 히트박스 높이 | 1.8 | **0.8** |
| 눈 높이 | 1.62 | **~0.48** |

### 마찰 공식 (속도 감쇠)
```java
float slipperiness = block.slipperiness; // 블록별 미끄러움 (일반=0.6, 얼음=0.98 등)

float horizontalDamping = 1F / (
    ((1F / slipperiness) - 1F) / 25F * Config._slideSlipperinessFactor.value + 1F
) * 0.98F;
// 최종 감쇠: 0.98F 고정 배수
```

**블록별 slipperiness 예시:**

| 블록 | slipperiness | 슬라이딩 효과 |
|------|-------------|-------------|
| 일반 블록 | 0.6 | 빠른 감속 |
| 얼음 (ice) | 0.98 | 거의 감속 없음 |
| 포장된 얼음 | 0.989 | 최고 속도 유지 |

### 방향 조정 (Steering)
```java
if (moveStrafing != 0 && Config._slideControlDegrees.value > 0) {
    double angle = -Math.atan(sp.motionX / sp.motionZ);
    if (sp.motionZ < 0) angle += Math.PI;
    // 현재 이동 방향 각도에서 strafing 방향으로 회전
    angle -= Config._slideControlDegrees.value / RadiantToAngle * Math.signum(moveStrafing);
    double hMotion = Math.sqrt(sp.motionX² + sp.motionZ²);
    sp.motionX = hMotion * -Math.sin(angle);
    sp.motionZ = hMotion *  Math.cos(angle);
}
// 속도 크기는 유지, 방향만 변경
```

**기본값:** `_slideControlDegrees = 1F` (매 틱 1도씩 방향 전환 가능)

---

## D. 로프 슬라이딩 (isRopeSliding) vs 지면 슬라이딩

| 항목 | 지면 슬라이딩 | 로프 슬라이딩 |
|------|------------|------------|
| 트리거 | 스프린트 + 스니크 | 덩굴에서 손 미사용 하강 |
| 히트박스 | -1F 축소 | 변경 없음 |
| 물리 | 블록 마찰 적용 | 덩굴 마찰 적용 |
| 방향 조정 | 가능 | 불가 (수직 하강) |
| 애니메이션 | 슬라이딩 포즈 | 로프 슬라이딩 포즈 |
| 패킷 비트 | 비트 21 | 비트 32 |

---

## E. 설정 플래그

| 설정 | 역할 |
|------|------|
| `Config.isSlidingEnabled()` | 슬라이딩 마스터 토글 |
| `Config._slidingSpeedStopFactor.value` | 슬라이딩 자동 해제 속도 임계값 |
| `Config._slideSlipperinessFactor.value` | 블록 미끄러움 가중치 조정 |
| `Config._slideControlDegrees.value` | 매 틱 최대 방향 전환 각도 |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `block.slipperiness` | `BlockState.getBlock().getSlipperiness()` (Fabric) |
| `setHeightOffset(-1F)` | `Entity.getDimensions()` Mixin → `EntityDimensions` |
| `move(0, -1D, 0, true)` | `Entity.move(MovementType, Vec3d)` |
| 방향 각도 계산 (`atan`) | 그대로 포팅 (순수 Java 수학) |
| `_slideControlDegrees` | 커스텀 설정 (Cloth Config 또는 직접 구현) |

---

## 미확인 / 추가 조사 필요

- [ ] 1.21.1 `BlockState.getSlipperiness()` 가 정확히 어느 클래스에 있는지 확인
- [ ] 슬라이딩 진입 시 `move(0, -1D, ...)` 가 실제로 필요한지, 아니면 히트박스 변경만으로 충분한지
- [ ] 슬라이딩 중 점프 시 어떤 점프 타입이 발동되는지 (`isSliding && jump`)
- [ ] 얼음 위 슬라이딩 + 경사면 조합 시 속도 누적 방지 로직 있는지
