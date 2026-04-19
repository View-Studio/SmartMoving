# 기어가기 (Crawling) 기능 리서치

> 원본 소스: `SmartMovingSelf.java` (주요 로직), `SmartMovingBase.java` (충돌 유틸)

---

## 관련 클래스/메서드

| 위치 | 메서드 | 역할 |
|------|--------|------|
| `SmartMovingSelf` | `updateEntityActionState()` | 크롤 상태 결정 |
| `SmartMovingSelf` | `toCrawling()` | 크롤 상태 진입 |
| `SmartMovingSelf` | `standupIfPossible()` | 크롤 → 기립 전환 |
| `SmartMovingSelf` | `setHeightOffset(float)` | 히트박스/높이 변경 |
| `SmartMovingBase` | `getMaxPlayerSolidBetween()` | 아래 솔리드 경계 탐지 |
| `SmartMovingBase` | `getMinPlayerSolidBetween()` | 위 솔리드 경계 탐지 |

---

## 동작 원리 (흐름)

```
매 틱
  ↓
1. isCrawling || isClimbCrawling이면 천장 체크 (mustCrawl)
   getMaxPlayerSolidBetween() → 바닥 위치
   getMinPlayerSolidBetween() → 천장 위치
   천장-바닥 < 플레이어 키 → mustCrawl = true
  ↓
2. 입력 평가 (wantCrawl)
   스니크 버튼 OR (자유클라이밍 비활성 + 그랩 버튼)
  ↓
3. canCrawl 검증
   수영/잠수/클라이밍 아님 + 낙하거리 < 최소값
  ↓
4. isCrawling = canCrawl && (wantCrawl || mustCrawl)
  ↓
5. 크롤 중이면 setHeightOffset(-1F) → 히트박스 축소
  ↓
6. standupIfPossible() — 매 틱 기립 가능 여부 체크
```

---

## A. 진입 조건 (완전한 boolean 로직)

```java
// --- 천장 강제 크롤 감지 ---
double crawlStandUpBottom = -1;
if (isCrawling || isClimbCrawling) {
    crawlStandUpBottom = getMaxPlayerSolidBetween(
        sp.boundingBox.minY - (initializeCrawling ? 0D : 1D),
        sp.boundingBox.minY,
        Config._crawlOverEdge.value ? 0 : -0.05
    );
    double crawlStandUpCeiling = getMinPlayerSolidBetween(
        sp.boundingBox.maxY,
        sp.boundingBox.maxY + 1.1D,
        0
    );
    // 공간 부족 → 강제 크롤
    mustCrawl = (crawlStandUpCeiling - crawlStandUpBottom) < (sp.height - heightOffset);
}

// --- 플레이어 입력 기반 크롤 의도 ---
boolean inputContinueCrawl = Options.isCrawlToggleEnabled()
    ? crawlToggled
    : sneakButton.Pressed || (!Config.isFreeClimbingEnabled() && grabButton.Pressed);

boolean wouldWantCrawl =
    !esp.capabilities.isFlying &&
    (
        (isCrawling && (inputContinueCrawl || contextContinueCrawl)) ||
        (grabButton.StartPressed && (sneakToggled || sneakButton.Pressed) && sp.onGround)
    );

boolean wantCrawl = Config.isCrawlingEnabled() && wouldWantCrawl;

// --- 크롤 가능 조건 ---
boolean canCrawl =
    !isSwimming &&
    !isDiving &&
    (!isDipping || (dippingDepth + heightOffset) < SwimCrawlWaterTopBorder) &&
    !isClimbing &&
    sp.fallDistance < Config._fallingDistanceMinimum.value;

// --- 최종 결정 ---
wasCrawling = isCrawling;
isCrawling = canCrawl && (wantCrawl || mustCrawl);
```

---

## B. 물리 변경

### 히트박스
```java
// 크롤 진입 시
setHeightOffset(-1F);
// 결과: height 1.8 → 0.8, boundingBox.minY += 1.0

// 크롤 해제 시
resetHeightOffset();
// 결과: height 0.8 → 1.8, boundingBox.minY -= 1.0
```

| 항목 | 기본 | 크롤 중 |
|------|------|---------|
| 히트박스 높이 | 1.8 | **0.8** |
| 눈 높이 | 1.62 | **~0.6** |
| boundingBox.minY | 바닥 | 바닥 + 1.0 |

### 이동 속도
```java
if (isCrawling || (isCrawlClimbing && !isClimbCrawling))
    speedFactor *= Config._crawlFactor.value;
// _crawlFactor 기본값: ~0.3~0.5 (보행의 30~50%)
```

---

## C. 해제 조건

1. 스니크 버튼 해제 (토글 모드 아닐 때)
2. 토글 모드: `willStopCrawl` 시 `crawlToggled = false`
3. 수영/잠수/클라이밍 상태 진입
4. `standupIfPossible()` 성공

---

## D. 기립 전환 (standupIfPossible)

```java
private void standupIfPossible() {
    if (heightOffset >= 0) return; // 크롤 중 아니면 스킵

    double gapUnder = getGapUnderneight(); // 발 아래 공간
    boolean groundClose = gapUnder < 1D;

    if (!groundClose) {
        resetHeightOffset(); // 공중: 즉시 기립
    } else {
        double gapOver = getGapOverneight(); // 머리 위 공간
        boolean standUpPossible = (gapUnder + gapOver) >= 1D; // 합 1블록 이상

        if (standUpPossible)
            standUp(gapUnder);
        else
            toSlidingOrCrawling(gapUnder); // 공간 부족 → 슬라이딩 또는 유지
    }
}

private void standUp(double gapUnder) {
    move(0, 1D - gapUnder, 0, true); // 플레이어를 위로 밀어올림
    isCrawling = false;
    isHeadJumping = false;
    resetHeightOffset();
}
```

**핵심:** 기립에 필요한 공간 = `gapUnder + gapOver >= 1.0`. 즉 머리 위에 최소 1블록 공간 필요.

---

## E. 크롤-클라이밍 연계 (wantCrawlNotClimb)

```java
wantCrawlNotClimb =
    (wantCrawlNotClimb || (grabButton.StartPressed && !wasCrawling)) &&
    grabButton.Pressed &&
    esp.movementInput.moveForward > 0F &&
    isCrawling &&
    sp.isCollidedHorizontally;
// 크롤 중 + 그랩 + 전진 + 벽 충돌 → 크롤-클라이밍으로 전환
```

---

## F. toCrawling() 진입 메서드

```java
private boolean toCrawling() {
    isCrawling = true;
    if (Options.isCrawlToggleEnabled())
        crawlToggled = true;
    ignoreNextStopSneakButtonPressed = true;
    // 첫 스니크 해제 무시 (실수 방지)
    return true;
}
```

---

## G. 설정 플래그

| 설정 | 역할 | 기본값 |
|------|------|--------|
| `Config.isCrawlingEnabled()` | 크롤 기능 마스터 토글 | true |
| `Config._crawlFactor.value` | 크롤 이동 속도 배수 | ~0.35 |
| `Config._crawlOverEdge.value` | 절벽 끝에서 크롤 허용 | true |
| `Config._fallingDistanceMinimum.value` | 크롤 진입 허용 최대 낙하거리 | ~3.0 |
| `Options.isCrawlToggleEnabled()` | 크롤 토글 모드 | false |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `sp.boundingBox.minY -= heightOffset` | `@Mixin(Entity)` `getDimensions()` → `EntityDimensions.of(w, h)` |
| `sp.height += heightOffset` | `LivingEntity.getActiveEyeHeight()` Mixin |
| `getMaxPlayerSolidBetween()` | `World.getBlockCollisions(entity, box)` |
| `getMinPlayerSolidBetween()` | `World.getBlockCollisions(entity, box)` |
| `speedFactor *= _crawlFactor` | `@Mixin(LivingEntity.travel())` 속도 배수 주입 |
| `standupIfPossible()` | `@Inject(LivingEntity.tickMovement())` 매 틱 호출 |

---

## 미확인 / 추가 조사 필요

- [ ] `EntityDimensions`가 1.21.1에서 동적으로 변경 가능한지 (캐싱 여부)
- [ ] `LivingEntity.getDimensions(EntityPose pose)` — pose 별로 다른 크기 지원하는지
- [ ] `getMaxPlayerSolidBetween` 대체 구현 — `World.getBlockCollisions` 정확한 사용법
- [ ] 크롤 중 눈 높이(`getEyeHeight`) Mixin 위치 확인
- [ ] 크롤 상태에서 카메라 보간이 자연스럽게 이루어지는지 테스트 필요
