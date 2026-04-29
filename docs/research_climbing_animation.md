# 그랩 클라이밍 애니메이션 1:1 매핑 리서치

작업: 원본 SmartMoving 1.7.10 의 그랩(Grab) 클라이밍 애니메이션을 1.21.1 Fabric 포팅과 라인별 1:1 으로 정합시킨다. 우선순위 = **기본 블록 그랩 클라이밍** (사다리/넝쿨이 아닌 일반 블록을 잡고 오르내리는 케이스).

---

## 1. 원본 데이터 도메인 (HandsClimbing / FeetClimbing)

원본 `HandsClimbing.java` 와 `FeetClimbing.java` 는 **두 도메인** 을 공유한다.

### 1-1. 인스턴스 도메인 (`_value`)
실시간 클라이밍 상태 추적용. `HandsClimbing` 인스턴스를 변수에 담아서 비교/max 연산.

| 인스턴스 | `_value` |
|----------|----------|
| None       | -3 |
| Sink       | -2 |
| TopHold    | -1 |
| BottomHold |  0 |
| Up         |  1 |
| FastUp     |  2 |

| 인스턴스 | `_value` |
|----------|----------|
| None                          | -3 |
| BaseHold                      | -2 |
| BaseWithHands                 | -1 |
| TopWithHands                  |  0 |
| SlowUpWithHoldWithoutHands    |  1 |
| SlowUpWithSinkWithoutHands    |  2 |
| FastUp                        |  3 |

### 1-2. 애니메이션 분기 도메인 (`int handsClimbType`)
**원본 `SmartMovingSelf.actualHandsClimbType` 필드는 `int` 타입이고 다음 3 값만 저장한다**:

```java
public class HandsClimbing {
    public static final int NoGrab     = 0;
    public static final int UpGrab     = 1;
    public static final int MiddleGrab = 2;
}
public class FeetClimbing {
    public static final int NoStep   = 0;
    public static final int DownStep = 1;
}
```

`SmartMovingSelf.setShouldClimbSpeed()` (L1500-L1510) 가 set 하는 유일한 진입점:

```java
private void setShouldClimbSpeed(double value) {
    setShouldClimbSpeed(value, HandsClimbing.UpGrab, FeetClimbing.DownStep);  // 기본
}
private void setShouldClimbSpeed(double value, int handsClimbType, int feetClimbType) {
    setOnlyShouldClimbSpeed(value);
    actualHandsClimbType = handsClimbType;   // 0/1/2 중 하나
    actualFeetClimbType = feetClimbType;     // 0/1 중 하나
}
```

호출 패턴 (`SmartMovingSelf.java`):
- L994: `setShouldClimbSpeed(FastUpMotion, NoGrab, DownStep)`
- L999: `setShouldClimbSpeed(..., MiddleGrab, DownStep)`
- L1004: `setShouldClimbSpeed(MediumUpMotion, MiddleGrab/UpGrab, DownStep)` (조건부)
- L1041: `setShouldClimbSpeed(ClimbDownMotion, NoGrab, DownStep)`
- L1052: `setShouldClimbSpeed(SinkDownMotion, MiddleGrab/UpGrab, NoStep)` (조건부)
- 기타 인자 1개 호출 → `(UpGrab, DownStep)` 기본.

**핵심**: 인스턴스 도메인 (Up/FastUp/Sink/...) 과 애니메이션 도메인 (NoGrab/UpGrab/MiddleGrab) 은 **별도 채널**. 인스턴스가 BottomHold 인 시점에서도 setShouldClimbSpeed 호출 시 매핑되는 handsClimbType 는 호출 컨텍스트마다 다름.

---

## 2. 1.21.1 우리 측 매핑

### 2-1. 우리 enum (6-way)
```java
public enum HandsClimbing {
    NONE,        // ordinal 0
    SINK,        // 1
    TOP_HOLD,    // 2
    BOTTOM_HOLD, // 3
    UP,          // 4
    FAST_UP      // 5
}
public enum FeetClimbing {
    NONE,                                    // 0
    BASE_HOLD,                               // 1
    BASE_WITH_HANDS,                         // 2
    TOP_WITH_HANDS,                          // 3
    SLOW_UP_WITH_HOLD_WITHOUT_HANDS,         // 4
    SLOW_UP_WITH_SINK_WITHOUT_HANDS,         // 5
    FAST_UP                                  // 6
}
```

`SmartMovingClientState.actualHandsClimbType / actualFeetClimbType` 에는 위 enum 의 **ordinal** 을 저장한다 (`MixinLivingEntityClient.java:212-213`).

### 2-2. enum→3-way 변환 (애니메이션 입구)
`MixinPlayerEntityModelClient.sm_animateClimbing` (L351-L364):

```java
int h = sm.actualHandsClimbType;
if (sm.isHandsVineClimbing && h >= 2 && h < 4) h = 4;  // MiddleGrab → UpGrab
if (h >= 4) {         // UP/FAST_UP → UpGrab
    handsDistUp = 2f;  handsOffset = -2.5f;
} else if (h >= 2) {  // TOP_HOLD/BOTTOM_HOLD → MiddleGrab
    handsDistUp = 2f;  handsOffset = -QUARTER;
} else {              // NONE/SINK → NoGrab
    handsDistUp = 0f;  handsOffset = -0.5f;
}
```

**우리 측은 인스턴스 도메인에서 직접 분기**. 원본 setShouldClimbSpeed 채널을 우회하므로 의미 매핑이 정확한지 별도 검증 필요 (이미 동작 확인됨, 함부로 변경 금지).

---

## 3. 원본 본체 라인별 (SmartMovingModel L127-L287)

### 3-1. 진입 + root 회전 + 머리 (L127-L132)
```java
else if(isClimb || isCrawlClimb) {
    bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle;  // L129 — root yaw
    bipedHead.rotateAngleY = 0.0F;                               // L131
    bipedHead.rotateAngleX = viewVerticalAngelOffset / RadiantToAngle;  // L132
```

### 3-2. 다리 회전 순서 (L134-L135)
```java
    bipedLeftLeg.rotationOrder  = ModelRotationRenderer.YZX;
    bipedRightLeg.rotationOrder = ModelRotationRenderer.YZX;
```

### 3-3. handsClimbType 보정 + 속도 clamp (L140-L145)
```java
    int handsClimbType = this.handsClimbType;
    if(isHandsVineClimbing && handsClimbType == HandsClimbing.MiddleGrab)
        handsClimbType = HandsClimbing.UpGrab;
    float verticalSpeed   = Math.min(0.5f, currentVerticalSpeed);
    float horizontalSpeed = Math.min(0.5f, currentHorizontalSpeed);
```

### 3-4. handsClimbType 3-way 파라미터 (L147-L176)
모든 분기에서 `handsFrequence{Up,Side}Factor = 0.6662F`, `handsDistanceSideFactor = 1.0F`, `handsDistanceSideOffset = 0.0F` 로 동일.
분기되는 값은 `handsDistanceUpFactor / handsDistanceUpOffset`:
- MiddleGrab: 2F / -π/2
- UpGrab: 2F / -2.5F
- NoGrab (default): 0F / -0.5F

### 3-5. feetClimbType 2-way 파라미터 (L178-L198)
- UpGrab (case `HandsClimbing.UpGrab` = 1): `feetDistanceUpFactor=0.3F/verticalSpeed`, `feetDistanceUpOffset=-0.3F`, `feetDistanceSideFactor=0.5F`, `feetDistanceSideOffset=0.0F`
- default (NoStep 등): 모두 0.

⚠️ 주의: 원본 코드는 `feetClimbType` 분기에 `case HandsClimbing.UpGrab` 을 사용한다. `HandsClimbing.UpGrab=1` 과 `FeetClimbing.DownStep=1` 의 정수값이 같아서 우연 일치. 분기 의미는 "발이 위로 등반 = DownStep (=1)" 이다.

### 3-6. 팔 X/Y (L200-L204)
```java
bipedRightArm.rotateAngleX = cos(totalVerticalDistance * 0.6662 + Half) * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
bipedLeftArm.rotateAngleX  = cos(totalVerticalDistance * 0.6662)         * verticalSpeed * handsDistanceUpFactor + handsDistanceUpOffset;
bipedRightArm.rotateAngleY = cos(totalHorizontalDistance * 0.6662 + Quarter) * horizontalSpeed * 1.0F + 0.0F;  // sideFactor=1, sideOffset=0
bipedLeftArm.rotateAngleY  = cos(totalHorizontalDistance * 0.6662)            * horizontalSpeed * 1.0F + 0.0F;
```

### 3-7. isHandsVineClimbing 추가 (L206-L215)
```java
if(isHandsVineClimbing) {
    bipedLeftArm.rotateAngleY  *= 1F + handsFrequenceSideFactor;  // ×1.6662
    bipedRightArm.rotateAngleY *= 1F + handsFrequenceSideFactor;
    bipedLeftArm.rotateAngleY  += Eighth;   // +π/4
    bipedRightArm.rotateAngleY -= Eighth;   // -π/4
    setArmScales(abs(cos(rArm.X)), abs(cos(lArm.X)));
}
```

### 3-8. 발 X 일반 경로 (L217-L221) — `if(!isFeetVineClimbing)` 가드
```java
if(!isFeetVineClimbing) {
    bipedRightLeg.rotateAngleX = cos(totalVerticalDistance * feetFrequenceUpFactor)        * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
    bipedLeftLeg.rotateAngleX  = cos(totalVerticalDistance * feetFrequenceUpFactor + Half) * feetDistanceUpFactor * verticalSpeed + feetDistanceUpOffset;
}
```
UpGrab 분기 시 `feetDistanceUpFactor*verticalSpeed = (0.3/v)*v = 0.3` → `cos*0.3 - 0.3` 으로 단순화.

### 3-9. 발 Z 일반 경로 (L223-L224) — **무조건** 적용
```java
bipedRightLeg.rotateAngleZ = -(cos(totalHorizontalDistance * feetFrequenceSideFactor) - 1.0F)         * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
bipedLeftLeg.rotateAngleZ  = -(cos(totalHorizontalDistance * feetFrequenceSideFactor + Quarter) + 1.0F) * horizontalSpeed * feetDistanceSideFactor + feetDistanceSideOffset;
```

### 3-10. isFeetVineClimbing 추가 (L226-L237)
```java
if(isFeetVineClimbing) {
    float total = (cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth;
    bipedRightLeg.rotateAngleX = -total;
    bipedLeftLeg.rotateAngleX  = -total;
    float difference = max(0, cos(totalDistance - Quarter)) * Sixtyfourth;
    bipedLeftLeg.rotateAngleZ  += -difference;
    bipedRightLeg.rotateAngleZ +=  difference;
    setLegScales(abs(cos(rLeg.X)), abs(cos(lLeg.X)));
}
```

### 3-11. isCrawlClimb 추가 (L239-L277)
```java
if(isCrawlClimb) {
    float height = smallOverGroundHeight + 0.25F;
    float bodyLength = 0.7F, legLength = 0.55F;
    // 3 분기로 bodyAngleX, legAngleX, legAngleZ 결정
    bipedTorso.rotateAngleX           =  bodyAngleX;
    bipedRightShoulder.rotateAngleX   = -bodyAngleX;   // ⚠️
    bipedLeftShoulder.rotateAngleX    = -bodyAngleX;   // ⚠️
    bipedHead.rotateAngleX            = -bodyAngleX;
    bipedRightLeg.rotateAngleX        =  legAngleX;
    bipedLeftLeg.rotateAngleX         =  legAngleX;
    bipedRightLeg.rotateAngleZ        =  legAngleZ;
    bipedLeftLeg.rotateAngleZ         = -legAngleZ;
}
```
⚠️ `bipedRightShoulder/bipedLeftShoulder` 는 SmartRender 가 추가한 노드. 1.21.1 PlayerEntityModel 에는 없음.

### 3-12. NoGrab + non-NoStep 추가 (L279-L286) — `if(isCrawlClimb)` **밖**
```java
if(handsClimbType == HandsClimbing.NoGrab && feetClimbType != FeetClimbing.NoStep) {
    bipedTorso.rotateAngleX = 0.5F;
    bipedHead.rotateAngleX -= 0.5F;
    bipedPelvic.rotateAngleX -= 0.5F;
    bipedTorso.rotationPointZ = -6.0F;
}
```
**핵심**: 이 블록은 `if(isCrawlClimb)` 블록 종료 후 (L277 의 `}` 다음) 별도로 실행. 즉 **모든 클라이밍 (CrawlClimb 여부 무관) 에서** NoGrab + non-NoStep 시 적용.

---

## 4. 1.21.1 우리 측 라인별 대조 결과

`MixinPlayerEntityModelClient.sm_animateClimbing` (L337-L475) 와 원본 본체 비교.

### 4-1. ✅ 일치 항목
| 원본 라인 | 우리 라인 | 내용 |
|-----------|-----------|------|
| L131-L132 | L346-L347 | head Y/X |
| L141-L142 | L353 | vine + MiddleGrab → UpGrab |
| L144-L145 | L342-L343 | speed clamp (단 horizontalSpeed = limbSwingAmount) |
| L147-L176 | L355-L364 | handsClimbType 3-way (sideFactor=1 / sideOffset=0 단순화) |
| L178-L198 | L391-L408 | feetClimbType 2-way |
| L200-L204 | L367-L370 | 팔 X/Y |
| L206-L215 | L374-L381 | isHandsVineClimbing 보정 |
| L217-L221 | L394-L404 | 발 X 일반 경로 (verticalSpeed=0 가드 추가) |
| L223-L224 | L408-L410 | 발 Z 일반 경로 |
| L226-L237 | L416-L429 | isFeetVineClimbing |

### 4-2. ❌ 차이/누락 항목

**(D-1) L129 `bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle` 누락**
- bipedOuter 는 SR root. 1.21.1 직접 대응 노드 없음.
- 이동 방향 ↔ 몸통 yaw 정합. 우리 `sm_setupTransforms` ModifyArg 의 `bodyYaw force` 가 대체 가능성 — 별도 검증 필요.
- 영향: 측면 이동 시 몸/팔 yaw 가 카메라/이동 방향과 다를 가능성.

**(D-2) L134-L135 다리 `rotationOrder = YZX` 누락**
- 우리는 다리에 직접 `pitch / roll / yaw` set. ModelPart 기본 ZYX.
- 다리 yaw = 0 (L431-L432) 이라 X/Z 회전 순서 차이는 결과 동일.
- 영향: 미미. 명시 매핑은 안전 위해 헬퍼 적용 가능 (선택).

**(D-3) L267-L268 `bipedRightShoulder/LeftShoulder.rotateAngleX = -bodyAngleX` (CrawlClimb 시) 누락**
- 1.21.1 shoulder 노드 부재. 의도 = arm 이 body 와 같이 기울어지는 보정.
- 1.21.1 매핑: arm.pitch 에 `-bodyAngleX` 누적 (`+=`).
- 영향: CrawlClimb (낮은 천장 클라이밍) 시에만. **사용자 요청 우선순위 = 기본 블록 그랩 클라이밍이므로 후순위.**

**(D-4) L279-L286 `NoGrab + non-NoStep` 보정 위치 오류**
- 원본: `if(isCrawlClimb)` 블록 **밖**. 모든 클라이밍에 적용.
- 우리: L466-L473 = `if(isCrawlClimb)` 블록 **안**. CrawlClimb 만 적용.
- 영향: **일반 그랩 클라이밍에서 NoGrab+non-NoStep 시 몸 기울기/머리/팔/다리 -0.5 보정 미적용.**
- **사용자 요청 = "기본 블록 그랩 클라이밍 1:1" 의 핵심 누락.**

### 4-3. ⚠️ 검증 필요 항목

**(V-1) horizontalSpeed = limbSwingAmount vs 원본 currentHorizontalSpeed**
- 원본 `currentHorizontalSpeed` 는 SmartStatistics 로 EMA 누적된 수평 속도.
- 우리는 vanilla `limbSwingAmount` (애니메이션 매개변수) 사용.
- 둘이 등가일 가능성 (SmartStatistics.calculate 가 vanilla limbAnimator 와 같은 식이라는 코멘트). 그러나 `verticalSpeed` 는 `sm.stats.currentVerticalSpeed` 로 정정한 전례 있음 → `horizontalSpeed` 도 `sm.stats.currentHorizontalSpeed` 로 정정해야 일관.

**(V-2) ordinal 매핑 (TOP_HOLD/BOTTOM_HOLD → MiddleGrab, SINK → NoGrab)**
- 원본 setShouldClimbSpeed 호출처가 인스턴스 ↔ 애니메이션 도메인 변환을 매번 컨텍스트 별로 처리.
- 우리는 인스턴스 ordinal 직접 매핑 → 의미 차이 가능성.
- 동작 확인된 매핑이므로 함부로 변경 금지. 인게임 시각 차이 발견 시에만 검토.

---

## 5. 우선순위 (사용자 요청 = 기본 블록 그랩 클라이밍)

기본 블록 그랩 = `isHandsVineClimbing=false` + `handsClimbType ∈ {UpGrab, MiddleGrab, NoGrab}` + 일반 클라이밍 (`!isCrawlClimb`).

이 시나리오에서 가장 영향 큰 항목:

| 항목 | 영향 | 우선순위 |
|------|------|----------|
| (D-4) NoGrab+non-NoStep 위치 오류 | 직접 영향 | **HIGH** |
| (D-1) bipedOuter.rotateAngleY | 측면 이동 yaw | MID |
| (V-1) horizontalSpeed 출처 | 좌우 진동 진폭 정합성 | MID |
| (D-2) 다리 rotationOrder | 미미 (yaw=0) | LOW |
| (D-3) shoulder 누락 | CrawlClimb 만 | 후순위 |
| (V-2) ordinal 매핑 | 동작 확인됨 | 변경 X |
