# 체크리스트 — `currentHorizontalSpeedFlattened` 원본 1:1 FIR 매핑

> 기반: `docs/research_flattened_fir_mapping.md`. 옵션 B (단순 ring buffer).

---

## Phase A — `SmartStatistics.java` 변경

### A-1. history field 추가
- [ ] `HISTORY_SIZE = 10` 상수.
- [ ] `currentHorizontalSpeedHistory[10]` (legYaw 값).
- [ ] `prevCurrentHorizontalSpeedHistory[10]` (prevLegYaw 값).
- [ ] `historyReady[10]` (boolean).
- [ ] `historyIndex = -1` (init).

### A-2. `calculate()` 안 history 저장
- [ ] EMA 갱신 후 (= `currentHorizontalSpeed` 최신값 set 후) history 저장.
- [ ] `historyIndex++` (mod 10).
- [ ] `prevCurrentHorizontalSpeedHistory[i] = prevCurrentHorizontalSpeed`.
- [ ] `currentHorizontalSpeedHistory[i] = currentHorizontalSpeed`.
- [ ] `historyReady[i] = true`.

### A-3. `getCurrentHorizontalSpeedFlattened(pt)` FIR 매핑
- [ ] history 가 비었으면 (`historyIndex < 0`) 0 반환.
- [ ] 최근 N (= 10) data 의 `min(1.0, prevLegYaw + (legYaw-prev)*pt)` 평균.
- [ ] count > 0 시 sum/count 반환.

### A-4. 기존 EMA(0.5) 식 제거
- [ ] `currentHorizontalSpeedFlattened = ... * 0.5 + ...` 식 제거.
- [ ] `currentHorizontalSpeedFlattened` field 제거 또는 사용 안 함.
- [ ] `prevCurrentHorizontalSpeedFlattened` 동일.
- [ ] reset() 에서도 정리.

### A-5. 컴파일 확인

---

## Phase B — 인게임 검증

### B-1. 슬라이딩 (사용자 보고 시나리오)
- [ ] **진입 transient**: 진자 amplitude 점진 도달.
- [ ] **steady state**: 진자 amplitude 일정 (= 원본과 동일).
- [ ] **감속 phase**: 진자 amplitude linear decay (vs 우리 EMA exponential).

### B-2. 회귀 검증 (다른 분기)
- [ ] **비행** (sm_animateFlying): 날개짓 amplitude transient 변화 확인.
- [ ] **낙하** (sm_animateFalling): 팔/다리 amplitude transient.
- [ ] **엎드리기** (sm_animateCrawling): 영향 미비 예상.
- [ ] **수영/잠수**: walkFactor 사용 여부 확인.

---

## Phase C — 완료 처리

### C-1. 검증 통과 시
- [ ] 메모리 등록 (선택).
- [ ] 커밋.

### C-2. 검증 실패 시
- [ ] 롤백 — `git restore src/main/java/choco/ratel/smartmoving/stat/SmartStatistics.java`.

---

## 작업 순서

```
A (state 변경) → B (검증) → C (완료/롤백)
```
