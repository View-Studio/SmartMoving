# SmartMoving 1:1 포팅 완결 가이드

> **목적**: 이 문서만 따르면 1:1 포팅이 완수된다.  
> 원자 단위 감사(2026-04-22)에서 확인한 **모든 미구현·버그·누락**을 목록화하고,  
> 동일한 실수가 재발하지 않도록 **검증 프로토콜**과 **금지 패턴**을 명시한다.

---

## PART 1 — 남은 미구현·버그 완결 목록

> 각 항목: 원본 명세 → 현재 코드 상태 → 정확한 수정 코드.  
> 완료 즉시 `[ ]` → `[x]` 로 체크한다.

---

### R-09. 천장 클라이밍 수직 velocity 미설정 🔴

**파일**: `src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java`

**원본** (`SmartMovingSelf.md` handleCeilingClimbing 1112-1174줄):
```java
// jgap 기준 motionY 설정 (원본)
if(jgap > 1.2)       sp.motionY = 0.12;
else if(jgap > 1.115) sp.motionY = 0.08;
else                   sp.motionY = 0.04;
sp.fallDistance = 0.0F;
// 이후 수평 이동 처리 (입력이 있을 때만)
if(distSq > 0.0001F) { sp.motionX = ...; sp.motionZ = ...; }
```

원본은 jgap 기준으로 `motionY`를 항상 양수로 설정하여 천장에 부착된다.  
**입력 없어도 motionY는 반드시 설정된다.**

**현재 코드** (L536-549, 버그):
```java
// 현재: horizontalSpeed 계산 후 수평 입력이 있을 때만 setVelocity 호출
// motionY는 vel.y(이전 틱 값) 그대로 유지 → 천장에서 서서히 낙하
if (distSq > 0.0001F) {
    ...
    player.setVelocity(motionX, vel.y, motionZ);  // ← vel.y가 문제
}
// 입력 없으면 setVelocity 자체를 호출하지 않음 → motionY 방치
```

`sm_travel_client`에서 `ci.cancel()`로 vanilla travel이 취소되므로 vanilla 중력(−0.08/틱)은 안 붙지만,  
이전 틱에서 남은 음수 y속도가 유지되고 `max(vel.y, -0.15D)` 클램프만 적용되어 최대 −0.15D/틱 낙하.

**수정**:
```java
// horizontalSpeed 계산 블록 직후, fallDistance 리셋 전에 삽입
// (입력 유무와 무관하게 항상 motionY를 설정한다)
Vec3d velBefore = player.getVelocity();

float forward = player.input.movementForward;
float strafe  = player.input.movementSideways;
float distSq  = forward * forward + strafe * strafe;
if (distSq > 0.0001F) {
    float dist   = (float) Math.sqrt(distSq);
    float ratio  = (float) (horizontalSpeed / Math.max(dist, 1F));
    strafe  *= ratio;
    forward *= ratio;
    double yawRad = Math.toRadians(player.getYaw());
    double cos = Math.cos(yawRad);
    double sin = Math.sin(yawRad);
    double motionX = strafe * cos - forward * sin;
    double motionZ = forward * cos + strafe * sin;
    player.setVelocity(motionX, horizontalSpeed, motionZ);  // ← horizontalSpeed가 motionY도 겸함
} else {
    // 입력 없을 때도 motionY는 반드시 설정
    player.setVelocity(velBefore.x, horizontalSpeed, velBefore.z);
}
```

**검증**: 천장에 grab 후 이동 입력 없이 3초 이상 유지 → 낙하하면 실패, 부착 유지면 성공.

---

### R-10. 클라이밍 애니메이션 손 타입 분기 미구현 🟠

**파일**: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`  
**관련**: `src/client/java/choco/ratel/smartmoving/climbing/HandsClimbing.java`

**원본** (`SmartMovingModel.md` 2번 분기 handsClimbType 스위치):

| handsClimbType | DistanceUpFactor | DistanceUpOffset |
|---|---|---|
| MIDDLE_GRAB (=2) | 2F | `-Quarter` (−π/2) |
| UP_GRAB (=1) | 2F | `-2.5F` |
| default (NO_GRAB=0, 그 외) | 0F | `-0.5F` |

또한 `isHandsVineClimbing`이면 팔 Y 각도를 `× 1.6662배 ± Eighth` 보정.

**현재 코드** (L195-203, 단일 포즈만 구현):
```java
// 현재: MiddleGrab(-Quarter) 고정. UpGrab(-2.5F) / NoGrab(-0.5F) 분기 없음
float rPitch = MathHelper.cos(limbSwing * 0.6662f + HALF) * verticalSpeed * 2f - QUARTER;
```

**수정** — `sm_animateClimbing`의 팔 계산 부분을 아래로 교체:
```java
// actualHandsClimbType → HandsClimbing.NO_GRAB/UP_GRAB/MIDDLE_GRAB 상수와 대조
// MixinLivingEntityClient L102: sm.actualHandsClimbType = hands[0].ordinal()
// HandsClimbing.java: ordinal NONE=0, SINK=1, TOP_HOLD=2, BOTTOM_HOLD=3, UP=4, FAST_UP=5
// 원본 handsClimbType 매핑:
//   UP/FAST_UP → UP_GRAB(1)
//   TOP_HOLD/BOTTOM_HOLD → MIDDLE_GRAB(2)
//   NONE/SINK → NO_GRAB(0)
int handsOrd = sm.actualHandsClimbType;
float handsDistUp, handsOffset;
if (handsOrd == HandsClimbing.MIDDLE_GRAB) {         // MIDDLE_GRAB(2) = ordinal TOP_HOLD or BOTTOM_HOLD
    handsDistUp = 2f;
    handsOffset = -QUARTER;
} else if (handsOrd == HandsClimbing.UP_GRAB) {      // UP_GRAB(1) = ordinal UP or FAST_UP
    handsDistUp = 2f;
    handsOffset = -2.5f;
} else {                                              // NO_GRAB(0) = NONE/SINK
    handsDistUp = 0f;
    handsOffset = -0.5f;
}
float rPitch = MathHelper.cos(limbSwing * 0.6662f + HALF) * verticalSpeed * handsDistUp + handsOffset;
float lPitch = MathHelper.cos(limbSwing * 0.6662f)        * verticalSpeed * handsDistUp + handsOffset;
float rYaw   = MathHelper.cos(limbSwing * 0.6662f + QUARTER) * horizontalSpeed;
float lYaw   = MathHelper.cos(limbSwing * 0.6662f)            * horizontalSpeed;
setAnglesYZX(rightArm, rPitch, rYaw, 0f);
setAnglesYZX(leftArm,  lPitch, lYaw, 0f);

// isHandsVineClimbing 추가 보정 (원본 SmartMovingModel.md L346-352)
if (sm.isHandsVineClimbing) {
    // YZX 적용 후 yaw만 추가 보정 (피치는 건드리지 않음)
    rightArm.yaw = rightArm.yaw * (1f + 0.6662f) - EIGHTH;
    leftArm.yaw  = leftArm.yaw  * (1f + 0.6662f) + EIGHTH;
}
```

> **주의**: `actualHandsClimbType`은 현재 `hands[0].ordinal()`(NONE=0…FAST_UP=5)로 저장된다.  
> `HandsClimbing.NO_GRAB=0, UP_GRAB=1, MIDDLE_GRAB=2` 상수와 ordinal이 다르므로,  
> 구현 전 `MixinLivingEntityClient.java:102`에서 ordinal → 애니메이션 타입으로 변환 로직을 먼저 추가하거나,  
> 아래처럼 ordinal 범위로 직접 판단한다:
> ```java
> // ordinal 기반 직접 매핑 (변환 로직 없이)
> // UP(4)/FAST_UP(5) → UP_GRAB  
> // TOP_HOLD(2)/BOTTOM_HOLD(3) → MIDDLE_GRAB
> // NONE(0)/SINK(1) → NO_GRAB
> int h = sm.actualHandsClimbType;
> boolean isUpGrab     = (h == 4 || h == 5);  // UP, FAST_UP
> boolean isMiddleGrab = (h == 2 || h == 3);  // TOP_HOLD, BOTTOM_HOLD
> ```

**검증**: 사다리 클라이밍 vs 넝쿨 클라이밍 시 팔 각도가 다른지 확인.

---

### R-10b. 클라이밍 애니메이션 발 타입 분기 + isFeetVineClimbing 분기 미구현 🟠

**파일**: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`

**원본** (`SmartMovingModel.md` 발 타입 테이블):

| feetClimbType | DistanceUpFactor | DistanceUpOffset | DistanceSideFactor |
|---|---|---|---|
| UpGrab(UP/FAST_UP ordinal) | `0.3F/verticalSpeed` | `-0.3F` | `0.5F` |
| default | `0F` | `0.0F` | `0.0F` |

**isFeetVineClimbing 대체 공식** (원본 L366-376):
```java
float total = (cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth;
rightLeg.rotateAngleX = -total;
leftLeg.rotateAngleX  = -total;

float difference = Math.max(0, cos(totalDistance - Quarter)) * Sixtyfourth;
leftLeg.rotateAngleZ  += -difference;
rightLeg.rotateAngleZ += difference;
```

**현재 코드** (L203-211): 발 각도 항상 0. 발 타입 분기 없음.

**수정** — 발 각도 계산을 아래로 교체:
```java
if (sm.isFeetVineClimbing) {
    // 넝쿨 발 클라이밍 전용 공식 (totalDistance ≈ limbSwing 사용)
    float total = (MathHelper.cos(limbSwing + HALF) + 1f) * THIRTYTWOTH + SIXTEENTH;
    rightLeg.pitch = -total;
    leftLeg.pitch  = -total;
    float diff = Math.max(0f, MathHelper.cos(limbSwing - QUARTER)) * SIXTYFOURTH;
    rightLeg.roll =  diff;
    leftLeg.roll  = -diff;
} else {
    int fOrd = sm.actualFeetClimbType;
    boolean feetUpGrab = (fOrd == 4 || fOrd == 5); // UP, FAST_UP
    if (feetUpGrab && verticalSpeed > 0f) {
        float feetDistUp = 0.3f / verticalSpeed;
        rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f)        * feetDistUp * verticalSpeed - 0.3f;
        leftLeg.pitch  = MathHelper.cos(limbSwing * 0.6662f + HALF) * feetDistUp * verticalSpeed - 0.3f;
        rightLeg.roll  = -(MathHelper.cos(limbSwing * 0.6662f) - 1f) * horizontalSpeed * 0.5f;
        leftLeg.roll   = -(MathHelper.cos(limbSwing * 0.6662f + QUARTER) + 1f) * horizontalSpeed * 0.5f;
    } else {
        rightLeg.pitch = 0f;
        leftLeg.pitch  = 0f;
        rightLeg.roll  = 0f;
        leftLeg.roll   = 0f;
    }
    rightLeg.yaw = 0f;
    leftLeg.yaw  = 0f;
}
```

---

### R-10c. isCrawlClimbing — legAngleZ(roll) 누락 🟡

**파일**: `MixinPlayerEntityModelClient.java:213-232`

**원본** (`SmartMovingModel.md` isCrawlClimb 보정 L380-413):
```java
if(height < bodyLength)           legAngleZ = Thirtytwoth;
else if(height < bodyLength + legLength)  legAngleZ = Thirtytwoth * (legAngleX / 1.537F);
else                                       legAngleZ = 0F;
bipedRightLeg.rotateAngleZ =  legAngleZ;
bipedLeftLeg.rotateAngleZ  = -legAngleZ;
```

**현재 코드** (L222-229): `legAngleZ` 계산 없음. `rightLeg.roll`/`leftLeg.roll` 미설정.

**수정** — isCrawlClimbing 블록에 legAngleZ 추가:
```java
if (sm.isCrawlClimbing) {
    float height = sm.smallOverGroundHeight + 0.25f;
    float bodyLength = 0.7f, legLength = 0.55f;
    float bodyAngleX, legAngleX, legAngleZ;
    if (height < bodyLength) {
        bodyAngleX = Math.max(0f, (float) Math.acos(height / bodyLength));
        legAngleX  = QUARTER - bodyAngleX;
        legAngleZ  = THIRTYTWOTH;
    } else if (height < bodyLength + legLength) {
        bodyAngleX = 0f;
        legAngleX  = Math.max(0f, (float) Math.acos((height - bodyLength) / legLength));
        legAngleZ  = THIRTYTWOTH * (legAngleX / 1.537f);
    } else {
        bodyAngleX = 0f;
        legAngleX  = 0f;
        legAngleZ  = 0f;
    }
    body.pitch     =  bodyAngleX;
    head.pitch     = -bodyAngleX;
    rightLeg.pitch =  legAngleX;
    leftLeg.pitch  =  legAngleX;
    rightLeg.roll  =  legAngleZ;   // ← 추가
    leftLeg.roll   = -legAngleZ;   // ← 추가
}
```

---

### R-11. isFeetVineClimbing / isHandsVineClimbing 항상 false 🟠

**파일**: `src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java`

**현재**: `getOnLadderOrVine()`에서 VineBlock 탐지는 하지만, 탐지 결과가 vine인지 ladder인지를  
`sm.isFeetVineClimbing` / `sm.isHandsVineClimbing`에 저장하지 않는다.  
`SmartMovingClientState.java:302-303` reset()에서 false로 초기화되고 그 이후 true 설정 없음.

**수정** — `getOnLadderOrVine()` 시그니처 확장 또는 호출 측에서 설정:

옵션 A: 출력 파라미터 추가 (권장)
```java
// SmartMovingClimber.getOnLadderOrVine() 시그니처에 추가
public static void getOnLadderOrVine(
        ClientPlayerEntity player, World world,
        boolean isSmall, boolean faceOnly,
        HandsClimbing[] out_hands, FeetClimbing[] out_feet,
        ClimbGap[] out_handsGap, ClimbGap[] out_feetGap,
        boolean[] out_handsVine,   // 추가
        boolean[] out_feetVine) {  // 추가
    ...
    // 넝쿨 판정 블록 안에서
    if (hasVineOnFace && ...) {
        if (isHandsLevel) {
            out_hands[0] = out_hands[0].max(HandsClimbing.UP, out_handsGap, gap);
            out_handsVine[0] = true;   // 추가
        } else {
            out_feet[0] = out_feet[0].max(...);
            out_feetVine[0] = true;    // 추가
        }
    }
}
```

호출 측 (`MixinLivingEntityClient.java:98-103`):
```java
boolean[] handsVine = {false};
boolean[] feetVine  = {false};
SmartMovingClimber.getOnLadderOrVine(player, world, isSmall, false,
        hands, feet, handsGap, feetGap, handsVine, feetVine);
sm.actualHandsClimbType = hands[0].ordinal();
sm.actualFeetClimbType  = feet[0].ordinal();
sm.isHandsVineClimbing  = handsVine[0];   // 추가
sm.isFeetVineClimbing   = feetVine[0];    // 추가
```

**검증**: 넝쿨 위에서 클라이밍 중 `sm.isFeetVineClimbing == true` 확인.

---

### R-12. distanceClimbedModified 서버 누적 없음 🟡

**파일**: `src/main/java/choco/ratel/smartmoving/server/SmartMovingServer.java:68`

**현재**: 필드 선언만 있고 서버에서 클라이밍 거리를 누적하는 코드 없음.

**원본** (`SmartMovingBase.md`): 클라이밍 이동 거리를 서버에서 추적하여 서버 측 피로도 계산에 사용.

**영향**: 낮음 — 클라이언트 `sm.exhaustion` 시스템이 이미 동작 중. 순수 서버 기반 피로도 경로는 선택적.

**수정** (낮은 우선순위):
- `MixinServerPlayNetworkHandler.java`에서 State 패킷 수신 시,  
  isClimbing/isCrawlClimbing/isCeilingClimbing 활성 플레이어의 이동 거리를  
  `SmartMovingServer.distanceClimbedModified`에 누적.

---

### R-13. sm_animateClimbing Javadoc stale 마커 🟢

**파일**: `MixinPlayerEntityModelClient.java:187`

현재:
```java
* Phase 13: handsClimbType/feetClimbType 세분화, 속도 기반 verticalSpeed 추적.
```

R-10 완료 후 교체:
```java
* handsClimbType/feetClimbType ordinal로 손/발 포즈 분기 (R-10/R-10b).
```

---

## PART 2 — 번역 품질 보장 규칙 (절대 금지 패턴)

> 이 규칙을 어긴 코드는 **미구현**으로 간주한다.

---

### 규칙 T-01. "필드 선언 = 구현 완료"가 아니다

**금지**:
```java
public boolean isFeetVineClimbing;   // ← 선언만 있으면 미구현
```

**요구**: 필드를 선언하면 **반드시** 아래 세 가지를 모두 확인한다:
1. 이 필드를 `true`/non-zero로 설정하는 코드가 존재하는가?
2. 설정 코드가 올바른 시점(올바른 Mixin/메서드)에서 호출되는가?
3. 이 필드를 **읽어서 사용하는** 코드가 존재하는가?

**검증 명령**:
```bash
grep -rn "fieldName\s*=" src/  # true/non-zero 설정 확인
grep -rn "fieldName"    src/  # 소비 확인
```

---

### 규칙 T-02. "encode/decode 구현 = 값 전달 완료"가 아니다

패킷에 비트를 encode하고 decode하는 코드가 있어도,  
**전송 측에서 그 비트를 올바르게 채우지 않으면** 상대방은 항상 0을 받는다.

**체크리스트**:
```
[ ] 값을 계산하여 필드에 저장하는 코드가 있는가?
[ ] encode() 시 그 필드가 비트에 반영되는가?
[ ] decode() 후 그 값이 애니메이션/물리에서 실제로 사용되는가?
```

**사례**: `isFeetVineClimbing` — decode 완벽, encode 완벽, 하지만 전송 전 필드가 항상 false.

---

### 규칙 T-03. "Javadoc에 원본 설명 = 코드 구현"이 아니다

```java
/**
 * 원본: SmartMovingSelf.handleCeilingClimbing() — motionY = HOLD_MOTION
 */
public static void handleCeilingClimbing(...) {
    // ← 원본 설명은 있지만 motionY 설정 코드가 없으면 미구현
}
```

**요구**: Javadoc의 "원본:" 설명이 있는 모든 메서드는  
실제 코드가 그 동작을 수행하는지 **코드 레벨에서** 직접 검증한다.

---

### 규칙 T-04. 원본에서 "항상" 실행되는 코드는 현재도 "항상" 실행되어야 한다

원본:
```java
// 입력 유무와 무관하게 항상 실행
sp.motionY = value;
if(distSq > 0.0001F) { sp.motionX = ...; sp.motionZ = ...; }
```

현재 (잘못된 포팅):
```java
if (distSq > 0.0001F) {
    player.setVelocity(motionX, value, motionZ);  // ← 조건부 실행 → 입력 없으면 미실행
}
```

**요구**: 원본 코드의 실행 조건을 보존한다. 조건 밖에 있던 코드는 조건 밖에 있어야 한다.

---

### 규칙 T-05. 단일 구현만으로 원본의 다중 분기를 대체하지 않는다

원본에 N가지 경우(switch/if-else chain)가 있으면,  
현재 구현도 N가지 경우를 모두 처리해야 한다.  
"기본값 하나로 대부분 커버됨"은 1:1 번역 기준에서 미구현이다.

**사례**: handsClimbType 3가지 분기 → 현재 1가지만 구현.

---

### 규칙 T-06. 메서드 존재 ≠ 메서드 호출됨

서버/클라이언트 어느 위치에서 호출되는지 grep으로 반드시 확인한다.

```bash
grep -rn "methodName(" src/  # 호출 위치 확인
```

---

### 규칙 T-07. 원본 조건식을 정확히 이식한다

원본: `isFeetVineClimbing ? 공식A : 공식B`  
현재: 공식B만 구현 → 넝쿨 클라이밍 시 잘못된 포즈

조건식 이식 체크리스트:
- `&&` / `||` 연산자 우선순위 보존
- null/0 분기 처리 여부
- else 분기 누락 여부

---

## PART 3 — 새 작업 시작 전 의무 체크리스트

> 모든 새 구현/수정 시 아래 절차를 따른다.

```
[ ] 1. research 파일에서 원본 코드를 먼저 읽는다.
[ ] 2. 원본 메서드의 실행 경로(어디서 호출되는가)를 확인한다.
[ ] 3. 원본 코드의 모든 분기(if-else, switch)를 목록화한다.
[ ] 4. 구현 후: 각 분기마다 대응하는 현재 코드 라인을 명시한다.
[ ] 5. 구현 후: T-01~T-07 규칙을 위반하는 부분이 없는지 재검사한다.
[ ] 6. 구현 후: 이 문서의 해당 항목을 [x]로 체크한다.
```

---

## PART 4 — 완료 검증 명령

> 모든 항목이 [x]가 되면 아래 명령으로 최종 검증한다.

```bash
# 1. 남은 TODO/미구현 마커 확인 (이 파일 자체는 제외)
grep -rn "TODO\|미구현\|stub\|Phase [0-9]" src/ --include="*.java" \
  | grep -v "//.*원본\|PENDING\|distanceClimbed"

# 2. isFeetVineClimbing / isHandsVineClimbing true 설정 확인
grep -rn "isFeetVineClimbing\s*=\s*true\|isHandsVineClimbing\s*=\s*true" src/

# 3. 천장 클라이밍 velocity 수정 확인
grep -n "setVelocity" src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java

# 4. 클라이밍 애니메이션 분기 확인
grep -n "actualHandsClimbType\|actualFeetClimbType\|handsOrd\|feetOrd" \
  src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java

# 5. 컴파일 에러 없음 확인
./gradlew compileJava compileClientJava
```

---

## PART 5 — 구현 순서 권장

```
우선순위 순:

R-09  (🔴 1줄 버그, 즉시 수정)
  └── R-11  (🟠 vine 플래그 설정, getOnLadderOrVine 시그니처 변경)
        └── R-10  (🟠 손 타입 분기, R-11의 isHandsVineClimbing 필요)
              └── R-10b  (🟠 발 타입 분기 + isFeetVineClimbing 분기, R-11 필요)
                    └── R-10c  (🟡 crawlClimbing legAngleZ, 독립적)
                          └── R-12  (🟡 서버 distanceClimbedModified, 선택적)
                                └── R-13  (🟢 Javadoc 정리)
```

---

*마지막 감사: 2026-04-22 원자 단위 전수 검사*  
*다음 감사 트리거: R-09~R-13 완료 후*
