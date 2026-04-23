# Focus #3 — 상태 전환 조건 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #3 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (재현 케이스 수집 필요 / #2 선행) |
| 현재 단계 | 재현 케이스 수집 |
| 선행 의존 | #2 (상태값이 정확해야 전환 조건 검증 의미 있음) |

---

## 2. 증상 — 원본 vs 현재

특정 **조합** 에서 상태가 원본과 다른 **타이밍**에 전환됨. 혹은 전환 자체가 일어나지
않음. 플래그는 맞는데 전환 경로가 어긋나는 경우.

전형적인 의심:
- 크롤링 ↔ 수영 ↔ 잠수 전환
- 슬라이딩 진입 조건
- 클라이밍 → 크롤링 ↔ 점프
- 벽 점프 더블클릭 타이머 리셋

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

| # | 시나리오 | 대상 전환 | 원본 기대 트리거 | 1.21.1 실제 | 원본 라인 근거 |
|---|---------|---------|---------------|-----------|-------------|
| 1 | (예) 전속력 + sneak + onGround | idle → isSliding=true | `sprint && sneak && onGround && !climbing && !headJump` | ? | `SmartMovingSelf.md` IMPL-02 대응 |
| 2 | (예) 물에서 벽으로 부딪힘 | isSwimming → isJumpingOutOfWater | `wantJumpOutOfWater && waterMovementTicks>10` | ? | `SmartMovingSwimmer.handleSwimming` §8-5 |
| 3 | | | | | |

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2** (상태값 정확성) |
| 영향받는 후속 | #4 (키 커맨드 결과), #1 (애니메이션 전환 순간) |
| 영향 주는 완료 이식 | IMPL-01 크롤링 전환 / IMPL-02 슬라이딩 / `updateWallJumpState` / `handleClimbing` |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 전환 영역 | 리서치 파일 | 섹션 |
|----------|------------|------|
| 크롤링 진입/유지/해제 | `SmartMovingSelf.md` | L2419-L2432 wouldWantCrawl / L2474 isCrawling = canCrawl && (wantCrawl \|\| mustCrawl) |
| 슬라이딩 진입 | `SmartMovingSelf.md` | IMPL-02 대응, slidingSpeedStopFactor 참조 |
| 벽 점프 트리거 | `SmartMovingSelf.md` + `mapping/jump.md` | L2863-2897 canWallJumping / wantWallJumping |
| 수영→크롤링 전환 | `SmartMovingSelf.md` | handleSwimming 내 standupIfPossible |
| 클라이밍 진입 | `SmartMovingSelf.md` | wouldWantClimb L1814-1822 |

### 5.2. 확보 필요

재현 케이스별 원본 전환 블록을 리서치 파일에서 확인. 부족하면 WebFetch:

```
대상 URL: SmartMovingSelf.java
추출 대상:
  1. <케이스 #N 대상 전환> 이 일어나는 조건식 전체
  2. 해당 전환이 호출되는 메서드 + 주변 30줄
  3. 전환 전후 다른 상태 플래그의 변경 흐름
```

---

## 6. 1:1 매핑 테이블

(재현 케이스 확보 후 구체 조건별로 작성)

---

## 7. 구조적 차이 / 근사 이식 지점

- `player.isSneaking()` ↔ `sm_isSneaking` override 순환 (sm_isSneaking 이 isSlow 참조)
  → 일부 조건에서 raw sneakKey 사용으로 회피 (이미 R-09 에서 적용)
- Button.StartPressed / StopPressed ↔ 엣지 감지 (`jumpKeyStartPressed` 등) — 이미 이식
- IMPL-01 블록 위치가 원본 `updateEntityActionState` 흐름 순서와 완전 일치하는가 점검

---

## 8. 현재 구현 스냅샷

재현 케이스 확보 후 해당 전환 블록 임베드.

---

## 9. 예상 수정 diff

케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집
- [ ] P-1. §3 표 최소 3행 채움
- [ ] P-2. 원본 기대 조건을 리서치 파일 라인 번호로 확인

### A. 원인 분석
- [ ] A-N. (케이스별 원본 조건식 vs 1.21.1 구현 대조)

### B. 수정
- [ ] B-N. (원인별)

### C. 검증
- [ ] C-1. 빌드
- [ ] C-2. 재현 케이스 전부 매칭
- [ ] C-3. 회귀 방지
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → `#4` 갱신

---

## 11. 호출 타이밍 검증

원본 `updateEntityActionState` → handleJumping → handleSwimming → handleLava →
handleAlternativeFlying → handleLand → handleWallJumping 순서.
1.21.1 `sm_travel_client` 순서와 원본 순서 **실제 일치** 여부 재검토.

---

## 12. 테스트 프로토콜

(재현 케이스별 구체 테스트 수순)

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표 전부 매칭
- [ ] 각 전환 조건 원본 라인과 1:1 대응
- [ ] 훅 호출 순서가 원본과 일치
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `wantCrawl`/`mustCrawl` pre-compute | isSlow 이전 계산 순서 보장 | [ ] |
| R-09 토글 블록 | 토글 블록이 전환 플래그 소비 | [ ] |
| `fromSwimmingOrDiving` | 수영↔크롤 전환 연쇄 | [ ] |
| `wantWallJumping` 자기참조 식 | 이전 틱 값 사용 | [ ] |

---

## 15. 작업 기록

_(비어있음)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- sm_travel_client 내 훅 호출 순서가 원본 updateEntityActionState 와 완전 일치하는지
  감사 — 별도 포커스 후보
