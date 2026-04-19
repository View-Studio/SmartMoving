# 클라이밍 / 그랩 / 천장 클라이밍 기능 리서치

> 원본 소스: `SmartMovingSelf.java`, `SmartMovingBase.java`, `Orientation.java`, `FeetClimbing.java`, `HandsClimbing.java`, `ClimbGap.java`

---

## 관련 클래스/메서드

| 위치 | 메서드 | 역할 |
|------|--------|------|
| `SmartMovingSelf` | `handleClimbing()` | 클라이밍 상태 결정 + 속도 설정 |
| `SmartMovingSelf` | `handleCeilingClimbing()` | 천장 클라이밍 처리 |
| `SmartMovingBase` | `getOnLadderOrVine()` | 사다리/덩굴 감지 |
| `SmartMovingBase` | `supportsCeilingClimbing()` | 천장 지지 블록 확인 |
| `Orientation` | `seekClimbGap()` | 8방향 클라이밍 표면 탐색 |
| `FeetClimbing` | (enum) | 발 클라이밍 7가지 상태 |
| `HandsClimbing` | (enum) | 손 클라이밍 8가지 상태 |
| `ClimbGap` | (data) | 탐지된 클라이밍 갭 정보 |

---

## A. 자유 클라이밍 (Free Climbing) 탐지

### 핵심 원리
바닐라 마인크래프트는 사다리 블록 위에 있어야만 클라이밍. SmartMoving은 **8방향으로 클라이밍 가능한 표면/갭을 탐지**하여 어떤 표면이든 올라갈 수 있게 함.

### 탐지 흐름
```java
// 8방향으로 ClimbGap 탐색
Orientation.PZ.seekClimbGap(rotation, world, i, id, jh, k, kd,
    isClimbCrawling, isCrawlClimbing, isSmallClimbing,
    inout_handsClimbing, inout_feetClimbing,
    out_handsClimbGap, out_feetClimbGap);
Orientation.NZ.seekClimbGap(...); // 뒤
Orientation.ZP.seekClimbGap(...); // 오른쪽
Orientation.ZN.seekClimbGap(...); // 왼쪽
// 크롤 아닐 때 대각선 4방향 추가
Orientation.PP.seekClimbGap(...);
Orientation.PN.seekClimbGap(...);
Orientation.NP.seekClimbGap(...);
Orientation.NN.seekClimbGap(...);
```

### ClimbGap 데이터 구조
```
Block     : 클라이밍 대상 블록
Meta      : 블록 메타데이터 (-1 = 미설정)
CanStand  : 해당 위치에서 기립 가능 여부
MustCrawl : 해당 위치에서 크롤 필요 여부
Direction : Orientation 방향
SkipGaps  : 갭 회피 플래그
```

---

## B. 진입 조건

```java
boolean exhaustionAllowsClimbing =
    !Config.isClimbExhaustionEnabled() ||
    (
        exhaustion <= Config._climbExhaustionStop.value &&
        (wasClimbing || exhaustion <= Config._climbExhaustionStart.value)
    );

boolean preferClimb = false;
if (wantClimbUp || wantClimbDown) {
    if (Config.isClimbExhaustionEnabled()) {
        maxExhaustionForAction  = min(maxExhaustionForAction,  Config._climbExhaustionStop.value);
        maxExhaustionToStartAction = min(maxExhaustionToStartAction, Config._climbExhaustionStart.value);
    }
    if (exhaustionAllowsClimbing)
        preferClimb = true;
}

// 클라이밍 최종 진입
boolean wantClimbUp = Config.isFreeClimbingEnabled() && ...grabButton.Pressed && !isHeadJumping && !wantCrawlNotClimb...;
isClimbing = preferClimb && (hasClimbGap || hasNeighborClimbGap || ...);
```

**진입 필수 조건 요약:**
1. `Config.isFreeClimbingEnabled()` = true
2. `fallDistance <= Config._freeClimbFallMaximumDistance.value`
3. `grabButton.Pressed` (또는 자동 사다리/덩굴 설정)
4. `!isHeadJumping && !wantCrawlNotClimb`
5. `exhaustionAllowsClimbing`
6. `seekClimbGap()`으로 탐지된 표면 존재

---

## C. 손 vs 발 클라이밍 상태 시스템

### FeetClimbing 상태
| 상수 | 값 | 동작 |
|------|-----|------|
| `None` | -3 | 클라이밍 없음 |
| `BaseHold` | -2 | 기본 접촉, 정지 |
| `BaseWithHands` | -1 | 손 보조로 유지 |
| `TopWithHands` | 0 | 상체 개입 |
| `SlowUpWithHoldWithoutHands` | 1 | 느린 상승 + 그립 유지 |
| `SlowUpWithSinkWithoutHands` | 2 | 느린 상승 + 하강 혼합 |
| `FastUp` | 3 | 빠른 상승 |

### HandsClimbing 상태
| 상수 | 값 | 동작 |
|------|-----|------|
| `None` | -3 | 없음 |
| `Sink` | -2 | 미끄러져 내려감 |
| `TopHold` | -1 | 상단 그립 유지 |
| `BottomHold` | 0 | 하단 그립 유지 |
| `Up` | 1 | 상승 |
| `FastUp` | 2 | 빠른 상승 |

### 속도 결정 로직
```java
if (feetClimbing == FeetClimbing.FastUp) {
    setShouldClimbSpeed(FastUpMotion,   HandsClimbing.NoGrab,      FeetClimbing.DownStep);
} else if (hasClimbGap && handsClimbing == HandsClimbing.FastUp) {
    setShouldClimbSpeed(FastUpMotion,   HandsClimbing.MiddleGrab,  FeetClimbing.DownStep);
} else if (feetClimbing == FeetClimbing.SlowUpWithHoldWithoutHands) {
    setShouldClimbSpeed(SlowUpMotion,   HandsClimbing.TopHold,     FeetClimbing.BaseHold);
} else if (feetClimbing == FeetClimbing.BaseWithHands) {
    setShouldClimbSpeed(HoldMotion,     HandsClimbing.BottomHold,  FeetClimbing.BaseWithHands);
} else {
    setShouldClimbSpeed(SinkDownMotion, HandsClimbing.Sink,        FeetClimbing.None);
}
```

---

## D. 덩굴(Vine) vs 사다리(Ladder) 차이

```java
// 덩굴 여부 별도 추적
isHandsVineClimbing = isClimbing && handsEdgeBlock == Blocks.vine;
isFeetVineClimbing  = isClimbing && feetEdgeBlock  == Blocks.vine;
isVineAnyClimbing   = isHandsVineClimbing || isFeetVineClimbing;
isVineOnlyClimbing  = isVineAnyClimbing && !(non-vine blocks exist);
```

**덩굴 클라이밍 특징:**
- 애니메이션에서 스케일링 적용 (`setArmScales`, `setLegScales`)
- `isRopeSliding` — 손 미사용 시 덩굴에서 자연 하강
- 네트워크 패킷에 별도 비트로 동기화 (비트 25, 26)

---

## E. 탈진(Exhaustion) 게이트

```java
// 클라이밍 중 탈진 소모
if (isClimbing && !isStill && Config.isClimbExhaustionEnabled()) {
    float gain = Config._baseExhaustionGainFactor.value;
    if (isVerticalStill)
        gain *= Config._climbStrafeExhaustionGain.value;
    else if (wantClimbUp)
        gain *= Config._climbUpExhaustionGain.value;
    else if (wantClimbDown)
        gain *= Config._climbDownExhaustionGain.value;
    additionalExhaustion += gain;
}
```

**특징:**
- 정지(hold) 시 탈진 소모 없음
- 상승/하강/옆이동마다 다른 소모율
- 탈진 초과 시 클라이밍 진입 차단 (진행 중인 클라이밍은 즉시 해제 아님)

---

## F. 천장 클라이밍 (Grab / isCeilingClimbing)

### 지지 블록 확인
```java
protected Block supportsCeilingClimbing(int i, int j, int k) {
    Block block = world.getBlock(i, j, k);
    // Config에 등록된 블록/메타데이터 딕셔너리로 확인
    Dictionary<Object, Set<Integer>> config = Config._ceilingClimbConfigurationObject.value;
    Set<Integer> metas = config.get(block);
    if (metas == null) metas = config.get(block.getUnlocalizedName());
    // 메타 일치하면 반환, 없으면 null
}
```

### 활성화 시퀀스
```java
Block topBlock    = supportsCeilingClimbing(i, j,     k); // 플레이어 머리 위
Block bottomBlock = supportsCeilingClimbing(i, j + 1, k); // 그 위 한 칸

if (topBlock != null || bottomBlock != null) {
    double jgap = 1D - jd + j;
    if (bottomBlock != null) jgap++;

    double solidHeight = getMinPlayerSolidBetween(jd, jd + 0.6, 0.2);

    if (jgap < 1.9 && solidHeight < jd + 0.5) {
        // 갭 크기에 따라 상승 속도 결정
        if      (jgap > 1.2)   sp.motionY = 0.12; // 멀리 있음 → 빠르게
        else if (jgap > 1.115) sp.motionY = 0.08;
        else                   sp.motionY = 0.04; // 거의 닿음 → 천천히
        sp.fallDistance = 0F;
        isCeilingClimbing = true;
    }
}
```

**중요:** 중력 반전이 아님. `motionY`를 양수로 설정하여 위로 밀어올림. 렌더링에서만 뒤집힌 것처럼 보임.

### 천장에서의 이동
```java
if (isCeilingClimbing)
    speedFactor *= Config._ceilingClimbingSpeedFactor.value;
// 이동: 수평 방향은 일반 지면 이동과 동일하게 처리
// 수직: motionY를 지속적으로 양수로 유지 (gravity 상쇄)
```

### 해제 조건
1. `grabButton.Pressed` = false
2. 지지 블록 없음 (`topBlock == null && bottomBlock == null`)
3. 탈진 초과 (`exhaustion > _ceilingClimbExhaustionStop`)
4. 장애물 (`actuallySolidHeight >= jd + 0.5`)

---

## G. 설정 플래그

### 클라이밍
| 설정 | 역할 |
|------|------|
| `Config.isFreeClimbingEnabled()` | 자유 클라이밍 마스터 토글 |
| `Config._freeClimbFallMaximumDistance.value` | 클라이밍 진입 허용 최대 낙하거리 |
| `Config.isClimbExhaustionEnabled()` | 탈진 시스템 활성화 |
| `Config._climbExhaustionStart.value` | 클라이밍 시작 탈진 임계값 |
| `Config._climbExhaustionStop.value` | 클라이밍 중단 탈진 임계값 |
| `Config._climbUpExhaustionGain.value` | 상승 시 탈진 소모율 |
| `Config._climbDownExhaustionGain.value` | 하강 시 탈진 소모율 |
| `Config.isFreeClimbAutoLadderEnabled()` | 사다리 자동 클라이밍 |
| `Config.isFreeClimbAutoVineEnabled()` | 덩굴 자동 클라이밍 |

### 천장 클라이밍
| 설정 | 역할 |
|------|------|
| `Config._ceilingClimbConfigurationObject.value` | 지지 블록 딕셔너리 |
| `Config._ceilingClimbingSpeedFactor.value` | 이동 속도 배수 |
| `Config.isCeilingClimbExhaustionEnabled()` | 탈진 시스템 활성화 |
| `Config._ceilingClimbExhaustionStart.value` | 시작 탈진 임계값 |
| `Config._ceilingClimbExhaustionStop.value` | 중단 탈진 임계값 |
| `Config._ceilingClimbExhaustionGain.value` | 탈진 소모율 |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `grabButton.Pressed` | 커스텀 KeyBinding + `Button` 래퍼 |
| `Orientation.seekClimbGap()` | 직접 포팅 (순수 Java 로직, API 의존 없음) |
| `world.getBlock(i, j, k)` | `World.getBlockState(pos).getBlock()` |
| `isOnLadderOrVine()` | `@Mixin(LivingEntity.isClimbing())` |
| `sp.motionY = FastUpMotion` | `entity.setVelocity(vel.x, FastUpMotion, vel.z)` |
| `FeetClimbing` / `HandsClimbing` enum | 그대로 포팅 (Java enum) |
| `AttachmentType`으로 클라이밍 상태 저장 | `AttachmentType<ClimbingState>` (Fabric 1.21+) |
| 천장 블록 설정 | `TagKey<Block>` 또는 JSON 설정 |

---

## 미확인 / 추가 조사 필요

- [ ] `Orientation` 클래스의 `seekClimbGap()` 정확한 구현 — 어떻게 갭을 찾는지
- [ ] 1.21.1에서 `LivingEntity.isClimbing()` Mixin 시 기존 사다리 클라이밍 유지 방법
- [ ] `AttachmentType`으로 `FeetClimbing`, `HandsClimbing` 상태를 플레이어에 붙이는 방법
- [ ] 천장 클라이밍 지지 블록을 `TagKey<Block>`으로 관리하면 데이터팩 호환 가능한지
- [ ] `entity.setVelocity()` 호출 위치 — `travel()` Mixin vs 별도 tick 이벤트
- [ ] 자유 클라이밍 탐지 반경이 1.21.1 블록 경계 계산과 정확히 일치하는지
