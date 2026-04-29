# 엎드리기 (Crawl) 기능 1:1 fix 체크리스트

리서치: `docs/research_crawl.md`
대상: `SmartMovingClientState.java`

작업 순서: HIGH → MID → LOW. 각 항목 단위로 사용자 인게임 검증 후 다음.

---

## Phase 1 — HIGH 우선순위

### [x] (2-1) `canCrawl` 의 `dippingDepth + heightOffset` 누락 fix

**위치**: `SmartMovingClientState.java:1632`

**현재**:
```java
&& (!isDipping || dippingDepth < 0.65F)
```

**수정**:
```java
&& (!isDipping || (dippingDepth + heightOffset) < 0.65F)
```

**원본 SmartMovingSelf.java L2437 1:1 매핑**.

**시각/기능 영향**: 크롤 중 (heightOffset=-1) 시 실제 유효 물 깊이 -1 보정 → 깊이 1.65 미만이어야 canCrawl=false (이전: 0.65 미만). 즉 크롤 중 더 깊은 물에서도 크롤 유지 가능.

**인게임 검증**: 얕은 물 (dippingDepth=0.3~1.5) 에서 크롤 진입 → 물 깊이 변화 시 canCrawl 변경 timing.

---

## Phase 2 — MID 우선순위

### [x] (2-2) `mustCrawl` 식의 일관성 정리 — 선택 1 (한 줄)

**위치**: `SmartMovingClientState.java:1147-L1156`

**현재**:
```java
if (isCrawling || isClimbCrawling) {
    mustCrawl = !canStandUp(player)
            && !isSwimming_sm && !isDiving
            && (!isDipping || dippingDepth < 0.65F);   // ← heightOffset 누락
    if (player.getAbilities().flying
            && (cfg0.isFlyingEnabled() || cfg0.isLevitateSmallEnabled())) {
        mustCrawl = false;
    }
} else {
    mustCrawl = false;
}
```

**선택 1 (안전 — 한 줄만)**: `dippingDepth + heightOffset` 추가 (canCrawl 와 일관성).
```java
&& (!isDipping || (dippingDepth + heightOffset) < 0.65F);
```

**선택 2 (정확 1:1)**: 원본 mustCrawl 단순 식 (천장 부족 만):
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
원본은 `!isSwimming_sm && !isDiving && (!isDipping...)` 조건이 mustCrawl 안에 없음. canCrawl 별도 검사로 처리.

**결과**: 두 선택 모두 isCrawling 결과 동등 (canCrawl && (wantCrawl || mustCrawl) AND 검사 시 중복 OK). **선택 1 권장** (변경 최소).

**인게임 검증**: 크롤 중 천장 부족 + 물에서 mustCrawl 발동 timing.

---

### [ ] (2-4) `canStandUp` 본체 검증 (선택사항)

**위치**: `SmartMovingClientState.java:2282-2287`

**현재**:
```java
private static boolean canStandUp(ClientPlayerEntity player) {
    Box standBox = player.getDimensions(EntityPose.STANDING)
                         .getBoxAt(player.getPos())
                         .contract(1.0E-7);
    return player.getWorld().isSpaceEmpty(player, standBox);
}
```

**원본 식**:
- `crawlStandUpBottom = getMaxPlayerSolidBetween(minY - (initializeCrawling ? 0 : 1), minY, crawlOverEdge ? 0 : -0.05)`
- `crawlStandUpCeiling = getMinPlayerSolidBetween(maxY, maxY + 1.1, 0)`
- `mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset`

**근사 차이**:
- 원본: 발 아래 1 블록 + 머리 위 1.1 블록 범위 (총 1.9 + 0.8 = 1.9 거리, heightOffset 보정 시 더 큼).
- 1.21.1: STANDING box (1.8 height) 빈 공간 검사.
- 차이 ~0.1 블록. `crawlOverEdge` 옵션 무시 (원본 horizontalTolerance).

**권장**: 정확 1:1 매핑 위해 원본 식 직접 이식 가능. 다만 다른 곳 (B-17b isCrawlClimbing 해제 등) 에서도 canStandUp 사용 → 변경 시 회귀 위험. **후순위**.

---

## Phase 3 — LOW 우선순위 (선택사항)

### [ ] (2-3) `contextContinueCrawl` 액체 천장 정확 식 이식

**위치**: `SmartMovingClientState.java:1176-1179`

**현재**:
```java
} else if (isCrawling && !isDipping) {
    contextContinueCrawl = false;
}
```

**원본 L2412-L2417**:
```java
else if (isCrawling) {
    double crawlStandUpLiquidCeiling = getMinPlayerLiquidBetween(boundingBox.maxY, boundingBox.maxY + 1.1D);
    if (crawlStandUpLiquidCeiling - crawlStandUpBottom >= sp.height + 1F)
        contextContinueCrawl = false;
}
```

**수정 시 필요**: `getMinPlayerLiquidBetween` 헬퍼 이식 (1.21.1 vanilla 액체 BlockState 검사 매핑). 시간 소요. **후순위**.

---

### [x] (2-7) `SwimCrawlWaterTopBorder/Medium/Bottom` 상수 도입 (4곳 hardcode 치환)

**위치**: 0.65F hardcode 사용처 모든 곳.

**원본**: `SmartMovingContext.SwimCrawlWaterTopBorder = 0.65F`.

**수정**: 1.21.1 `SmartMovingContext` 또는 적절한 위치에 상수 정의 + hardcode 치환.

**영향**: 명시성만 (결과 동일).

---

## 검증 안 하는 항목 (1:1 유지)

- ✅ 모든 필드 (isCrawling/wasCrawling/mustCrawl/wantCrawl/etc.) — 1:1 이식 완료.
- ✅ 모든 메서드 (toCrawling 등) — 1:1 이식 완료.
- ✅ B-35 영역 (크롤 진입/종료 엣지) — 원본 L2822-L2836 정밀 이식 (Agent 추측 잘못, 직접 검증 결과 정확 매핑).
- ✅ B-36 영역 (grab.StartPressed 3분기) — 원본 L2839-L2862 1:1.
- ✅ R-09 토글 블록 (willStartCrawl/willStopCrawl) — 원본 L2979-L2984 1:1.
- ✅ wantCrawlNotClimb 계산 — 원본 L2452-L2461 1:1.
- ✅ wasCrawling 저장 timing — 원본 L2441 (isCrawling 공식 직전) 1:1.
- ✅ B-34 (wasCrawling && !isCrawling && flying → tryJump) — 원본 L2449 1:1.

## 의도된 차이 (유지)

- ✅ wouldWantCrawl 의 `!facedClimbable_` 가드 — 사용자 명시 추가 (사다리/덩굴 정면 + grab+sneak 시 등반 우선).
- ✅ heightOffset 패턴 (boundingBox 직접 조작 → vanilla CROUCHING 자세) — 1.21.1 EntityPose 시스템 활용.

---

## 작업 흐름

1. Phase 1 (2-1) 만 먼저 한 줄 수정 → 빌드 → 사용자 인게임 검증.
2. 사용자 OK → Phase 2 (2-2 선택 1) 진행.
3. Phase 3 / 2-4 는 별도 요청 시.
4. 모든 작업 완료 후 회귀 점검 + 커밋.
