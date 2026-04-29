# 엎드리기 (Crawl) 기능 1:1 매핑 리서치

작업: 원본 SmartMoving 1.7.10 의 엎드리기 (crawl) 기능 모든 코드를 1.21.1 우리 측과 라인별 1:1 대조. 사용자 명령: "단 한줄 코드 한개 누락 없이 차이 식별 + 수정".

**범위**: 엎드리기 **기능만**. 애니메이션 별도 task.

---

## 1. 핵심 변수 / 메서드 매핑 (Agent 분석 + 직접 grep 통합)

### 1-1. 필드 (✅ 모두 이식 완료)

| 변수 | 원본 위치 | 1.21.1 위치 | 상태 |
|------|----------|------------|------|
| `isCrawling` | SmartMoving.java:48 | SmartMovingClientState.java:211 | ✅ |
| `wasCrawling` | SmartMovingSelf.java:3074 | SmartMovingClientState.java:538 | ✅ |
| `mustCrawl` | tickEssential 지역 변수 (L2395) | SmartMovingClientState.java:493 (필드 승격) | ✅ |
| `wantCrawl` | tickEssential 지역 변수 (L2430) | SmartMovingClientState.java:487 (필드 승격) | ✅ |
| `wouldWantCrawl` | 지역 변수 (L2419) | SmartMovingClientState.java:263 (필드 승격) | ✅ |
| `wantCrawlNotClimb` | SmartMovingSelf.java:1416 | SmartMovingClientState.java:502 | ✅ |
| `crawlToggled` | SmartMovingSelf.java:3100 | SmartMovingClientState.java:425 | ✅ |
| `ignoreNextStopSneakButtonPressed` | SmartMovingSelf.java:3078 | SmartMovingClientState.java:427 | ✅ |
| `contextContinueCrawl` | SmartMovingSelf.java:3077 | SmartMovingClientState.java:522 | ✅ |
| `initializeCrawling` | 지역 변수 (L2395) | SmartMovingClientState.java:510 (필드 승격) | ✅ |
| `heightOffset` | SmartMoving.java:57 | SmartMovingClientState.java:107 | ✅ |
| `dippingDepth` | SmartMovingSelf.java:1432 | SmartMovingClientState.java:115 | ✅ |

### 1-2. 메서드 (모두 이식 완료)

| 메서드 | 원본 위치 | 1.21.1 위치 | 상태 |
|--------|----------|------------|------|
| `toCrawling()` | SmartMovingSelf.java L3047-L3054 | SmartMovingClientState.java L3012-L3019 | ✅ |
| `setHeightOffset(-1F)` | SmartMovingSelf.java L1694-L1704 | `heightOffset = -1F` 필드 set 으로 대체 + `player.calculateDimensions()` | ⚠️ 패턴 변경 |
| `resetHeightOffset()` | SmartMovingSelf.java L1681-L1686 | `heightOffset = 0F` 필드 set 으로 대체 | ⚠️ 패턴 변경 |
| 크롤 진입 엣지 | L2827-L2836 (`setHeightOffset(-1F)` + `move(0,-1D,0)`) | SmartMovingClientState.java L2022-L2026 | ✅ |
| 크롤 종료 엣지 | L2822-L2826 (`resetHeightOffset()` + `move(0, crawlStandUpBottom - minY, 0)`) | SmartMovingClientState.java L2009-L2019 | ✅ |

---

## 2. 진정한 1:1 차이 항목 (직접 검증 후 확정)

### 2-1. ❌ **canCrawl 의 `dippingDepth + heightOffset` 누락** [HIGH]

**원본 SmartMovingSelf.java L2437**:
```java
boolean canCrawl =
    !isSwimming &&
    !isDiving &&
    (!isDipping || (dippingDepth + heightOffset) < SwimCrawlWaterTopBorder) &&
    !isClimbing &&
    sp.fallDistance < Config._fallingDistanceMinimum.value;
```

**1.21.1 SmartMovingClientState.java L1630-L1634**:
```java
boolean canCrawl = !isSwimming_sm
        && !isDiving
        && (!isDipping || dippingDepth < 0.65F)   // ← heightOffset 누락
        && !isClimbing
        && player.fallDistance < cfg.fallingDistanceMinimum;
```

**차이**: `dippingDepth + heightOffset` → `dippingDepth` (heightOffset 누락).

**의미**:
- 원본: 크롤 중 (heightOffset=-1) 시 실제 유효 물 깊이 -1 보정. 즉 dippingDepth 가 0.65 + 1 = 1.65 이상이어야 canCrawl=false.
- 1.21.1: 단순 dippingDepth 검사. heightOffset 영향 없음 → 깊이 0.65 만 넘으면 canCrawl=false.

**시각/기능 영향**: 크롤 중 물 위로 올라올 때 canCrawl 판정 부정확. 크롤 종료 timing 차이.

**권장 수정**:
```java
&& (!isDipping || (dippingDepth + heightOffset) < 0.65F)
```

---

### 2-2. ⚠️ **mustCrawl 식의 canCrawl 일부 조건 합침** [MID]

**원본 SmartMovingSelf.java L2395-L2402**:
```java
boolean mustCrawl = false;
if(isCrawling || isClimbCrawling) {
    crawlStandUpBottom = getMaxPlayerSolidBetween(...);
    double crawlStandUpCeiling = getMinPlayerSolidBetween(...);
    mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset;
}
```
→ mustCrawl = **천장 부족 단순 검사**.

**1.21.1 SmartMovingClientState.java L1147-L1156**:
```java
if (isCrawling || isClimbCrawling) {
    mustCrawl = !canStandUp(player)
            && !isSwimming_sm && !isDiving
            && (!isDipping || dippingDepth < 0.65F);
    if (player.getAbilities().flying
            && (cfg0.isFlyingEnabled() || cfg0.isLevitateSmallEnabled())) {
        mustCrawl = false;
    }
} else {
    mustCrawl = false;
}
```

**차이**:
1. `canStandUp(player)` 근사 (원본 정확 식: `crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset`).
2. mustCrawl 식에 canCrawl 의 일부 조건 합침 (`!isSwimming_sm && !isDiving && (!isDipping || ...)`).

**의미**:
- 원본 isCrawling 결과: `canCrawl && (wantCrawl || mustCrawl)`. 원본 mustCrawl=true 시도 canCrawl=false 면 isCrawling=false.
- 1.21.1: mustCrawl 안에 이미 일부 조건. canCrawl 도 같은 조건 또 검사 → AND 중복. **결과는 동등** (수영/잠수/dipping 시 둘 다 false).

**결과 동등이라 영향 없음**, 그러나 1:1 명시성 떨어짐. 특히 mustCrawl 식의 `(!isDipping || dippingDepth < 0.65F)` 도 heightOffset 누락 동일.

**권장 수정**:
- mustCrawl 식을 원본 단순 매핑으로 정정 (천장 부족 만):
  ```java
  if (isCrawling || isClimbCrawling) {
      mustCrawl = !canStandUp(player);
      if (player.getAbilities().flying
              && (cfg0.isFlyingEnabled() || cfg0.isLevitateSmallEnabled())) {
          mustCrawl = false;
      }
  } else {
      mustCrawl = false;
  }
  ```
- canCrawl 의 `(!isDipping || dippingDepth < 0.65F)` 만 정정 (heightOffset 추가).

---

### 2-3. ⚠️ **contextContinueCrawl 해제 — 액체 천장 검사 단순화** [LOW]

**원본 SmartMovingSelf.java L2412-L2417**:
```java
if (contextContinueCrawl) {
    if (inputContinueCrawl || sp.isInWater() || mustCrawl)
        contextContinueCrawl = false;
    else if (isCrawling) {
        double crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(sp.boundingBox.maxY, sp.boundingBox.maxY + 1.1D);
        if (crawlStandUpLiquidCeiling - crawlStandUpBottom >= sp.height + 1F)
            contextContinueCrawl = false;
    }
}
```

**1.21.1 SmartMovingClientState.java L1173-L1180**:
```java
if (contextContinueCrawl) {
    if (inputContinueCrawl || player.isTouchingWater() || mustCrawl) {
        contextContinueCrawl = false;
    } else if (isCrawling && !isDipping) {
        // 원본: 포복 위 천장까지 액체 여유가 충분하면 해제. 1.21.1: 물 밖이면 해제 근사.
        contextContinueCrawl = false;
    }
}
```

**차이**: 원본은 액체 천장 정확 거리 검사 (`getMinPlayerLiquidBetween` + 비교). 1.21.1 은 `!isDipping` 단순 검사 (근사).

**의미**: 물 밖으로 나오는 timing 약간 다름 (원본은 천장까지 충분 거리, 1.21.1 은 dipping 해제 즉시).

**권장 수정**: `getMinPlayerLiquidBetween` 헬퍼 이식 + 정확 식 적용. 다만 1.21.1 vanilla 액체 BlockState 검사 매핑 필요 (시간 소요). 후순위.

---

### 2-4. ⚠️ **mustCrawl 의 canStandUp(player) 근사** [MID]

**원본 식**: `mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset`
- 정확 식: 천장 위 1.1 거리 + 바닥 아래 (initializeCrawling ? 0 : 1) 거리 사이의 최소/최대 solid → 차이 비교.

**1.21.1**: `!canStandUp(player)` (헬퍼 함수 호출).

**검증 필요**: `canStandUp(player)` 본체 정확 식 확인. 원본 ceiling/bottom 계산과 동등한가?

**참고**: B-31c-post 해소 (세션 134) 주석에 "원본 L2346 `getMaxPlayerSolidBetween(minY, maxY, 0) > minY` 정밀 복원" 명시. canStandUp 이 정확 식 매핑일 가능성. 코드 검증 필요.

---

### 2-5. ✅ **sneak 가드 / 키 매핑** (확인 완료)

**원본**: `sneakButton.Pressed`, `sneakToggled` (custom Button 클래스).
**1.21.1**: `SmartMovingKeys.sneak.isPressed()`, `sneakToggled` (Fabric KeyBinding).

**상태**: 1:1 등가 매핑.

---

### 2-6. ✅ **wantCrawlNotClimb 계산** (이미 1:1)

**원본 L2452-L2461**:
```java
wantCrawlNotClimb =
    (wantCrawlNotClimb || (grabButton.StartPressed && !wasCrawling))
    && grabButton.Pressed && esp.movementInput.moveForward > 0F
    && isCrawling && sp.isCollidedHorizontally;
```

**1.21.1 L1662-L1668**: 1:1 정확.

---

### 2-7. ⚠️ **`SwimCrawlWaterTopBorder` 상수 직접 hardcode**

**원본**: `SwimCrawlWaterTopBorder` (= 0.65F).
**1.21.1**: `0.65f` 직접.

**결과 동일**, 명시성 위해 상수 추가 가능. 후순위.

---

### 2-8. ✅ **wouldWantCrawl 의 `facedClimbable_` 가드 추가** (의도된 변경)

**원본**: 가드 없음.
**1.21.1 L1200-L1201**: `&& !facedClimbable_` (사다리/덩굴 정면 + grab+sneak 시 등반 우선).

**의미**: 사용자 명시 추가. 의도된 차이. **유지**.

---

### 2-9. ⚠️ **`heightOffset` 의 setHeightOffset/resetHeightOffset 패턴 차이**

**원본 (L1694-L1704)**: `setHeightOffset(-1F)` = `boundingBox.minY -= -1F (= +1F)` + `height += -1F` (히트박스 축소).

**1.21.1**: `heightOffset = -1F` 필드 set + `player.calculateDimensions()` 호출 → vanilla CROUCHING/SWIMMING 자세 dimensions 자동 적용.

**의미**: vanilla EntityPose 시스템 의존. 결과는 동등 (player height 0.8F, eyeHeight 자동 조정) 추정. 다만 매핑 위치 차이.

**검증 필요**: 매 frame heightOffset=-1F 잔존 시 vanilla dimensions 갱신 정확 timing. 메모리 `feedback_heightoffset_pattern.md` 참조 (BUG-28/29 패턴).

---

## 3. 사용자 명시 우선순위 (정확성 최우선)

| 우선 | 항목 | 영향 | 매핑 정정 가능성 |
|------|------|------|-----------------|
| **HIGH** | (2-1) canCrawl `dippingDepth + heightOffset` 누락 | 크롤 중 물 위 timing 차이 | 한 줄 수정 |
| **MID** | (2-2) mustCrawl 식 단순화 + heightOffset 누락 | 결과 동등이지만 1:1 명시성 | 식 정정 |
| **MID** | (2-4) canStandUp(player) 근사 — 원본 정확 식 검증 | 천장 fit 판정 정확도 | 본체 검증 후 결정 |
| **LOW** | (2-3) contextContinueCrawl 액체 천장 검사 | 물 밖 나오는 timing 미세 | 헬퍼 이식 (시간 소요) |
| **LOW** | (2-7) `SwimCrawlWaterTopBorder` 상수 | 명시성 | 상수 도입 |
| **검증** | (2-9) heightOffset 패턴 — vanilla CROUCHING 자동 dimensions | timing 검증 | 인게임 검증 |
| **유지** | (2-8) facedClimbable_ 가드 | 사용자 의도 추가 | 유지 |

---

## 4. 검증 필요 (인게임 + 코드)

1. **`canStandUp(player)` 본체** — 원본 `getMaxPlayerSolidBetween + getMinPlayerSolidBetween` 와 동등한지.
2. **`heightOffset = -1F` + `calculateDimensions()` 매 tick 호출 timing** — `vanilla` POSE 자동 적용 검증.
3. **모든 fix 후 회귀 점검** — 그랩 클라이밍 / 비행 / 낙하 등 다른 시스템 영향 없는지.
