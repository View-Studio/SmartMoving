# Focus #2 — 스마트무빙 상태 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #2 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (재현 케이스 수집 필요) |
| 현재 단계 | 재현 케이스 수집 — §3 표 채우기 전까진 진입 금지 |
| 선행 의존 | 없음 |

---

## 2. 증상 — 원본 vs 현재

상태 플래그들(`isCrawling`/`isDipping`/`isSwimming_sm`/`isDiving`/`isClimbing`/
`isCrawlClimbing`/`isCeilingClimbing`/`isSliding`/`isHeadJumping`/`isFast`/`isSlow`/
`wasCrawling_st`/`contextContinueCrawl` 등)이 **잘못된 시점**에 true/false 로 됨.

해당 상태를 소비하는 다른 로직(애니메이션/전환 조건/키 커맨드)이 연쇄적으로
오동작. 따라서 이 포커스를 먼저 완료해야 #1/#3/#4 에서 원인-결과 혼동 없음.

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

플레이테스트로 아래 표를 채워야 진입 가능. 각 행은 1 상태 × 1 상황 → 1 불일치.

| # | 시나리오 (입력/환경) | 대상 상태 | 원본 기대 | 1.21.1 실제 | 비교 기준 |
|---|-------------------|----------|---------|-----------|----------|
| 1 | (예) 얕은 물 진입 + sneak | `isDipping` | true | false | 원본 `SmartMovingSelf.handleSwimming` 경계값 |
| 2 | (예) sneak + sprint + onGround | `isSlow` | ? | ? | `SmartMovingSelf.md` L2717 |
| 3 | | | | | |

**형식 안내**:
- "시나리오" 는 재현 가능한 키 입력 + 환경(물/공중/블록) 조합
- "원본 기대" 는 리서치 파일의 원본 코드 기반 (추측 금지, 근거 없으면 불명)
- "1.21.1 실제" 는 F3 디버그 / 로그 / 블록 반응 등으로 관찰
- "비교 기준" 은 원본 소스 라인 번호 / 리서치 파일 섹션

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | 없음 |
| 영향받는 후속 | #1 (애니메이션 입력 상태), #3 (전환 조건), #4 (키 커맨드 결과) |
| 영향 주는 완료 이식 | `SmartMovingClientState.tickEssential` 전체 / `SmartMovingSwimmer.updateSwimState` / 토글 블록 R-09 |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 상태 | 리서치 파일 | 줄 |
|------|------------|---|
| `isDipping` / `isSwimming_sm` / `isDiving` | `SmartMovingSelf.md` | L~1019 resetSwimming / L91-L100 필드 |
| `isCrawling` | `SmartMovingSelf.md` | L2419 wouldWantCrawl / L2474 `isCrawling = canCrawl && (wantCrawl \|\| mustCrawl)` |
| `isSlow` | `SmartMovingSelf.md` | L2711-2719 |
| `isFast` | `SmartMovingSelf.md` | 필드 선언 근처 + grab && sprint |
| `isHeadJumping` | `SmartMovingSelf.md` | L1607-1631 toSlidingOrCrawling / L2550 (헤드점프 진입) |
| `contextContinueCrawl` | `SmartMovingSelf.md` | L1388(=true) / L2411/L2416/L2447(=false) |
| `wasCrawling_st` / `wasSneaking` / `wasClimbCrawling` | R-09 블록 진입부 (저장 시점) | 1.21.1 tickEssential |

### 5.2. 확보된 원본 코드

- R-09 블록 전체는 `SmartMovingSelf.md` 에 덤프됨 (이전 세션)
- `handleSwimming` 의 offset 3분류 경계값 0.65/0.6/0.55/1.9 확보됨

### 5.3. 확보 필요 — 재현 케이스별 원본 해당 블록

재현 케이스 수집 후 각 케이스의 원본 계산 로직을 리서치 파일에서 확인.
부족하면 WebFetch:

```
대상 URL: SmartMovingSelf.java
추출 대상:
  1. <케이스 #N 에서 문제된 상태 필드> 가 갱신되는 모든 위치
  2. 주변 20줄 맥락 포함
```

---

## 6. 1:1 매핑 테이블

(재현 케이스 확보 후 구체 필드/메서드별로 작성)

| 원본 심볼 | 1.21.1 심볼 | 상태 |
|---|---|---|
| `isDipping` | `SmartMovingClientState.isDipping` | ✓ |
| `isSwimming` (원본) | `isSwimming_sm` (vanilla 충돌 회피 접미사) | ✓ |
| `isDiving` | `isDiving` | ✓ |
| `dippingDepth` | `dippingDepth` | ✓ |
| ... | ... | (재현 케이스별 확장) |

---

## 7. 구조적 차이 / 근사 이식 지점

- `getMaxPlayerSolidBetween` / `getMinPlayerLiquidBetween` 정밀 AABB → `canStandUp` /
  `hasLiquidCeiling` 근사 (이미 적용, 정밀도 손실 있음)
- 반-블록 단위 수직 탐색 → BlockState 단위
- vanilla `isSwimming()` 과 SM `isSwimming_sm` 병존 (접미사 회피) — 혼동 방지용

---

## 8. 현재 구현 스냅샷

재현 케이스 확보 후 문제된 상태별로 관련 코드 블록 임베드.

---

## 9. 예상 수정 diff

재현 케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집 (진입 전 단계)
- [ ] P-1. 플레이테스트로 §3 재현 케이스 표 최소 3행 채움
- [ ] P-2. 각 케이스의 원본 기대 값을 리서치 파일 라인 근거로 확인 (근거 없으면 WebFetch)

### A. 원인 분석 (케이스마다)
- [ ] A-N. (케이스 #N 별 — 원본 코드 vs 1.21.1 구현 side-by-side 비교)

### B. 수정 (원자 단위)
- [ ] B-N. (원인별 수정)

### C. 검증
- [ ] C-1. 빌드 성공
- [ ] C-2. 재현 케이스 전부 "예상 == 실제" 매칭
- [ ] C-3. 회귀 방지 감사
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → `#3` 갱신

---

## 11. 호출 타이밍 검증

(재현 케이스 확보 후 — 상태 갱신이 일어나는 훅 위치 원본과 대조)

---

## 12. 테스트 프로토콜

```
디버깅 방법:
  - F3 화면에 SM 상태 필드 표시 (신규 HUD 오버레이 필요할 수도)
  - 또는 임시 LOGGER.info 로 상태 값 매 틱 출력
  - 플레이테스트 중 특정 키 조합 수행 → 로그 비교
```

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표의 모든 케이스가 "예상 == 실제" 매칭
- [ ] 각 수정이 리서치 파일 원본 라인과 1:1 대응
- [ ] 신규 발견 등록
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| R-09 토글 블록 | `wasSneaking` / `wasCrawling_st` 저장 시점 변경하면 토글 블록 깨짐 | [ ] |
| `wouldWantSneak` / `wouldWantCrawl` | 참조 필드 의미 변경 시 연쇄 영향 | [ ] |
| `sendStatePacket` / `processStatePacket` | State 비트 매핑 변경하면 다른 플레이어 렌더 깨짐 | [ ] |
| `sm_isSneaking` override | isSlow → isSneaking 순환 주의 | [ ] |

---

## 15. 작업 기록

_(비어있음)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- 상태 디버그용 HUD 오버레이(F3 + Tab 같은) 추가는 별도 포커스 후보
