# shift+grab 엎드리기 진입 BUG 작업 체크리스트

리서치: `docs/research_shift_grab_crawl_bug.md`
종결: 2026-04-30

---

## 진단 흐름 요약

1. **1차 추측 (research_shift_grab_crawl_bug.md)**: `wantFreeClimb` 가드 / `(isCrawling && isClimbing)` mixin 분기 / `playerHeight` mixin 영향. → **틀림** (인게임 검증 결과 부작용 발생).
2. **1차 fix (Fix A/B) 롤백** + 디버그 로그 추가 (사용자 요청).
3. **로그 1차 분석**: 박스 `bbY=[y, y+0.6]` (height 0.6) 잔존 발견. mustCrawl=true 오발동 → sneak 릴리즈 후 isCrawling 잔존.
4. **로그 2차 분석 (사용자 추측: 비행 → 엎드리기 시퀀스)**: `setBoundingBox` / `calculateDimensions` stack trace 추적 mixin 추가.
5. **결정적 단서 — stack trace**:
   ```
   [setBox] oldH=0.800 newH=0.600
   stack: ... ← Entity.calculateDimensions ← SmartMovingClientState.standupIfPossible:3137
            ← SmartMovingClientState.tickEssential:1645
   ```
6. **Root cause 확정**: `restoreFromFlying = true` stale 잔존 → 매 틱 standupIfPossible 호출 → 끝의 `player.calculateDimensions()` 가 sm.isCrawling 갱신 (L1697) **전** 시점 호출 → mixin 가드 매치 못 함 → vanilla SWIMMING POSE dim (0.6×0.6) 적용 → box height 0.6 → mustCrawl=true 오발동.
7. **Fix 적용 + 사용자 인게임 검증 통과**.

---

## 적용된 Fix

### [x] `SmartMovingClientState.java:1645` — restoreFromFlying stale 클리어

**위치**: tickEssential 안 standupIfPossible 호출 직후.

**변경 전**:
```java
if (restoreFromFlying || tryLanding) {
    standupIfPossible(player, tryLanding, restoreFromFlying);
}
```

**변경 후**:
```java
if (restoreFromFlying || tryLanding) {
    standupIfPossible(player, tryLanding, restoreFromFlying);
    // 🔴 (사용자 보고 — 비행 → 엎드리기 박스 0.6 BUG / 2026-04-30):
    //   restoreFromFlying = true 는 비행/헤드점프 종료 엣지 (L1573/L1577/L1804)
    //   에서 set 되는데 어디서도 클리어 안 됨 → stale 잔존 → 매 틱 standupIfPossible
    //   호출 → 끝부분 player.calculateDimensions() (L3137) 가 sm.isCrawling 갱신
    //   (L1697) 전 시점에 호출되어 mixin 가드 매치 못 함 → vanilla SWIMMING POSE
    //   dim (0.6×0.6) 적용 → box height 0.6 → mustCrawl=true 오발동 → sneak 릴리즈
    //   해도 isCrawling 유지.
    //   해결: 호출 후 즉시 클리어 → 종료 엣지 1회만 유지.
    //   원본 1.7.10 은 setHeightOffset 가 box 직접 조작 → calculateDimensions 호출
    //   자체 없음. 1.21.1 EntityPose 호환 위해 L3137 추가했고 stale 결합이 BUG 발현.
    this.restoreFromFlying = false;
}
```

---

## 사용자 보고 3 증상 ↔ 원인 매핑

| 증상 | 원인 |
|------|------|
| 정상 엎드리기보다 콜리전 더 줄어듦 | box height 0.6 (vanilla SWIMMING dim) 적용 — 정상은 0.8 |
| 속도 거의 안 움직임 | `mustCrawl=true` 오발동 + crawl factor 0.15 + 작은 박스 collision 응답 차이 사용자 인지 |
| shift 릴리즈해도 해제 안 됨 | `mustCrawl=true` 잔존 → `isCrawling = canCrawl && (wantCrawl=false || mustCrawl=true) = TRUE` 유지 |

3 증상 모두 **하나의 root cause** 에서 파생.

---

## Trigger 조건

- 비행 진입 → 비행 해제 (vanilla creative fly off, 또는 SM auto-landing).
- 비행 해제 엣지에서 `(!isFlying && wasFlying) || (!isLevitating && wasLevitating)` 매치 → `restoreFromFlying = true` set (`SmartMovingClientState.java:L1573/L1577`).
- 그 후 어떤 시점이든 sneak+grab 으로 SM crawl 진입 시 BUG 발현.
- 또는 헤드점프 종료 엣지 (`L1804`) 에서도 동일하게 set → 동일 trigger.

---

## 회귀 점검 (사용자 인게임 검증 통과)

- [x] 비행 → 비행 해제 → 평지 sneak+grab 엎드리기 → 정상 box 0.8.
- [x] sneak 릴리즈 → 정상 해제.
- [x] 비행 안 한 케이스 엎드리기 → 정상 (변경 영향 없음).

---

## 의도된 변경 외 영향 검토

- `restoreFromFlying = false` 클리어 위치: standupIfPossible 호출 직후. 호출 안의 분기 (resetHeightOffset / standUp / toSlidingOrCrawling) 는 모두 호출 안에서 처리 완료. 이후 reset 영향 없음.
- 비행 종료 엣지 → 한 번만 standupIfPossible 호출 → `false` 클리어. 다음 틱부터 `restoreFromFlying=false` → L1644 분기 미진입 → calculateDimensions 매 틱 재호출 차단.
- 원본 1.7.10 SmartMovingSelf 도 standupIfPossible 후 명시적 클리어는 없으나, **원본은 `calculateDimensions` 호출 자체가 없음** (setHeightOffset/resetHeightOffset 가 boundingBox.minY/height 직접 조작) → stale 잔존 영향 0. 1.21.1 EntityPose 호환 위해 추가한 `L3137 player.calculateDimensions()` 가 stale 과 결합되어 BUG. 1.21.1 매핑 특유의 결함이라 보수적 fix 정당.

---

## 디버그 로그 (제거 완료)

추적 과정에서 5 개 파일에 추가됐던 `[SM-CRAWL-BUG][...]` 로그 모두 제거:
- `SmartMovingClientState.java`: dumpCrawlBugState 헬퍼 + tickEssential 끝 trigger dump + mustCrawl-calc dump.
- `MixinLivingEntityClient.java`: travel-pre / travel-post-climb dump.
- `SmartMovingClimber.java`: climb-search / climb-match dump.
- `MixinPlayerEntityClient.java`: getDim 분기별 dump + updatePose-crawl dump.
- `MixinEntityClient.java`: setBox / calcDim 추적 mixin (stack trace 포함).
