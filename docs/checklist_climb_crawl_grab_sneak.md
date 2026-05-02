# 클라이밍/비행/엎드리기 3 기능 작업 체크리스트

리서치: `docs/research_climb_crawl_grab_sneak.md`

---

## 사용자 의도 (인게임 작동 목표)

1. **기능 1**: grab + 클라이밍 중 1 칸 갭 발견 + sneak → 그 1 칸 갭으로 클라이밍하면서 엎드리기.
2. **기능 2**: 비행 → 1 칸 공간에서 비행 풀림 → 바로 엎드리기.
3. **기능 3**: 엎드린 상태로 블록 앞에서 grab 누름 → 엎드린 채 클라이밍.

---

## 현재 매핑 cross-check 결과

| 항목 | 위치 | 상태 |
|------|------|------|
| 기능 1 메인 식 (`isClimbCrawling`) + 진입/해제 엣지 | `SmartMovingClientState.java:2028-2072` | ✅ 정확하게 이식됨 |
| 기능 1 좁은 갭 분기 (`handleClimbing` L996-1000) | `SmartMovingClimber.java:818-826` | ✅ 정확하게 이식됨 |
| 기능 2 호출 (`standupIfPossible` 호출) | `SmartMovingClientState.java:1631-1632` | ✅ 이식됨 |
| 기능 2 본체 (`standupIfPossible` 메서드) | `SmartMovingClientState.java:3000-3055` | ✅ 이식됨 |
| 기능 2 stale `restoreFromFlying` 클리어 fix | `SmartMovingClientState.java:1592, 1643` | ✅ 4-30 적용됨 |
| 기능 3 메인 식 (`isCrawlClimbing`) + canStandUp + 해제 엣지 | `SmartMovingClientState.java:1945-2010` | ✅ 정확하게 이식됨 |

→ **세 기능의 핵심 식과 진입/해제 엣지는 모두 이식 완료**.

---

## 차이/문제 식별

### 차이 2 — `wantClimbHolding` 의 `forwardPressed_holdGuard` 가드 (기능 1 차단)

**위치**: `SmartMovingClientState.java:1384-1391`

```java
boolean forwardPressed_holdGuard = player.input.movementForward > 0F;
wantClimbHolding = !forwardPressed_holdGuard && (...);
```

**원본**: 가드 없음 (`SmartMovingSelf.java:2721-2728`).

**부작용**:
- forward > 0 시 `wantClimbHolding = false` 강제
- → `canClimbCrawling = wantClimbHolding && wantClimbUp = false`
- → **`isClimbCrawling` 영구 비활성** = **기능 1 작동 안 함**

**가드 의도** (코드 주석 L1373-1383): 사다리 등반 + W + sneak 시 자세/박스 토글 + 찔끔찔끔 등반 BUG fix.

**상충**: 가드 의도 vs 사용자 의도 (좁은 갭 자동 진입) **충돌**.

### 차이 3 — handleClimbing matched skip (영향 없음)

**확인 결과**: `SmartMovingClimber.java:818-826` 의 좁은 갭 분기에서 `matched = true` 정상 set → `setOnlyShouldClimbSpeed` 호출 → `isClimbing = true` set. 좁은 갭 매칭 시 정상 작동. **차이 3 은 기능 1/3 에 영향 없음**.

---

## 작업 항목

### [ ] 1. 차이 2 가드 선별적 우회 (기능 1 활성화)

**전략 (옵션 B)**: 가드 자체는 유지하되, 좁은 갭이 발견된 경우 (`hasClimbCrawlGap || (hasClimbGap && isClimbHolding)` = `needClimbCrawling` 조건) 에 한해 가드를 우회.

**효과**:
- 좁은 갭 발견 시 → 가드 우회 → `wantClimbHolding=true` 가능 → `isClimbCrawling` 활성 (사용자 의도)
- 일반 사다리 등반 (위에 갭 없음) + W + sneak → 가드 유지 → 찔끔찔끔 등반 BUG 재발 방지

**코드** (`SmartMovingClientState.java:1384-1391` 수정):
```java
// needClimbCrawling 발생 시에는 forward 가드 우회 — 좁은 갭 자동 진입 (기능 1) 보장.
boolean needClimbCrawling17 = hasClimbCrawlGap || (hasClimbGap && isClimbHolding);
boolean forwardPressed_holdGuard = player.input.movementForward > 0F && !needClimbCrawling17;
wantClimbHolding = !forwardPressed_holdGuard && (...);
```

### [ ] 2. 기능 1 인게임 검증

**시나리오**: 사다리 / 덩굴 / 그랩 가능 표면 등반 중 + 위에 1 블록 좁은 갭 + W + sneak (또는 crawl toggle).

**기대 동작**:
- isClimbCrawling 활성 → 박스 0.8 (heightOffset = -1) → 좁은 갭으로 등반 진입.
- 갭 통과 후 mustCrawl/sneak/toggle 시 자동 엎드리기 전환.

**회귀 점검**:
- 일반 사다리 등반 (위 갭 없음) + W + sneak → 찔끔찔끔 등반 / 자세 토글 X (옵션 B 가드로 보호).
- 평지 + grab + sneak 엎드리기 → 정상 (기존 동작 유지).

### [ ] 3. 기능 2 인게임 검증 (재확인)

**시나리오**: 비행 (creative fly) → 1 블록 공간에서 비행 해제.

**기대 동작**:
- 비행 종료 엣지 → `restoreFromFlying = true` → `standupIfPossible` 호출.
- 1 블록 공간 → `groundClose=true`, `standUpPossible=false` → `toSlidingOrCrawling(gap)` 분기.
- grab 안 누르면 → `toCrawling()` → 엎드리기 진입.

**회귀 점검** (4-30 검증 통과 항목):
- 비행 → 비행 해제 → 평지 sneak+grab 엎드리기 → 정상 box 0.8.
- sneak 릴리즈 → 정상 해제.

### [ ] 4. 기능 3 인게임 검증

**시나리오**: 엎드린 상태 (`isCrawling=true`) + 사다리 / 덩굴 / 그랩 표면 정면 + grab + W + sneak (또는 crawl toggle).

**기대 동작**:
- `isCrawlClimbing` 5-AND 매치 → `isCrawling=false` (진입 엣지) → 박스 0.8 유지 → 등반 시작.
- 등반 중 위로 일어설 공간 발견 → canStandUp → `isCrawlClimbing=false` + `resetHeightOffset()` → 자동 standing 등반.
- 등반 종료 / W 해제 / sneak 해제 시 해제 엣지 3 분기 처리.

**회귀 점검**:
- 엎드린 채 사다리 정면 + W (sneak 없음) → 일반 standing 등반 (높이 복원).
- 엎드린 채 일반 블록 정면 + grab + W + sneak → 엎드린 채 등반.

---

## 작업 순서

1. 차이 2 가드 옵션 B 적용 → 컴파일 검증.
2. 사용자 인게임 검증 (3 기능 + 회귀) → 결과에 따라 추가 fix 또는 종결.
3. 종결 후 커밋.

---

## 검증 미통과 시 분기

- 기능 1 동작 안 함 → 가드 옵션 A (완전 제거) 시도 또는 추가 식 매핑 점검.
- 기능 2 재현 시 → restoreFromFlying 외 추가 stale 변수 점검.
- 기능 3 동작 안 함 → 5-AND 조건 중 어느 항이 false 인지 디버그 로그.

---

## 참고 — 기능 1 vs 기능 3 차이

| 항목 | 기능 1 (`isClimbCrawling`) | 기능 3 (`isCrawlClimbing`) |
|------|---------------------------|---------------------------|
| 시나리오 | 일어선 채 등반 → 좁은 갭 발견 → 좁은 자세로 갭 통과 | 엎드린 채 등반 표면 → 엎드린 자세로 등반 |
| 주 trigger | `hasClimbCrawlGap` (좁은 갭 발견) | `wasCrawling` (이전 틱 엎드리기) |
| forward 의존 | `wantClimbHolding && wantClimbUp` (= `forward>0` 필요) | 식에 `forward > 0F` 직접 명시 |
| 자동 해제 | climbIntoCount=0 + needClimbCrawling=false | canStandUp (위로 일어설 공간) |
| 동시 활성 | 가능 (각자 식 매치 시 둘 다 활성) | 가능 |
