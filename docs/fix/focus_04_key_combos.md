# Focus #4 — 상태 전환 키 커맨드 조합 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #4 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (재현 케이스 수집 / #2 #3 선행) |
| 현재 단계 | 재현 케이스 수집 |
| 선행 의존 | #2, #3 |

---

## 2. 증상 — 원본 vs 현재

특정 키 조합(예: `sneak(hold) + grab + jump`)이 원본과 다른 동작을 발동시키거나,
아예 발동 안 함. 차지 점프, 헤드 점프, 벽 점프, 더블클릭 방향 점프, 크롤링 진입
등이 특정 키 타이밍에서 원본과 어긋남.

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

| # | 입력 조합 (키 이벤트 시퀀스) | 원본 기대 동작 | 1.21.1 실제 | 원본 라인 근거 |
|---|--------------------------|-------------|-----------|-------------|
| 1 | sneak hold → jump hold (0.5초) → sneak 릴리즈 → jump 릴리즈 | 차지 점프 발동 (jumpCharge 소모) | (확인 필요) | 원본 handleJumping 차지점프 분기 |
| 2 | sprint + grab hold → jump hold → 릴리즈 | 헤드 점프 차지 + 발동 | (확인 필요) | 원본 headJump 분기 |
| 3 | 벽 밀면서 jump 빠른 더블탭 | 벽 점프 발동 (wallJumpCount 타이머) | (확인 필요) | §벽 점프 L2863-2897 |
| 4 | A 빠른 더블탭 (onGround) | 왼쪽 방향 점프 발동 | (확인 필요) | IMPL-03 leftJumpCount |
| 5 | sneak + grab + spacebar (동시) | (확인 필요) | (확인 필요) | — |
| 6 | | | | |

**입력 시퀀스 표기**: "hold" = 꾹 누름 유지, "릴리즈" = 놓음, "→" = 다음 이벤트,
"(시간)" = 대략 지속 시간.

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2** (입력이 상태 변경 후 키 처리), **#3** (전환 조건이 키에 의존) |
| 영향받는 후속 | 없음 |
| 영향 주는 완료 이식 | `SmartMovingJumper.handleJumping` / `updateWallJumpState` / IMPL-03 더블클릭 / R-09 토글 |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 키 커맨드 | 리서치 파일 | 섹션 |
|----------|------------|------|
| 차지 점프 | `SmartMovingSelf.md` | L1400-L1420 handleJumping |
| 헤드 점프 차지 | `SmartMovingSelf.md` | 관련 분기 + `Config._headJump` |
| 벽 점프 더블클릭 | `SmartMovingSelf.md` L2863-2897 + `mapping/jump.md` L623-668 | 완전 덤프됨 |
| 방향 점프 더블클릭 | `SmartMovingSelf.md` L1904 | IMPL-03 |
| 크롤링 토글 진입 | `SmartMovingSelf.md` R-09 (L2966-L3045) | 완전 덤프됨 |
| 스니크 토글 | 동일 | 완전 덤프됨 |

### 5.2. 확보 필요

재현 케이스에서 원본 기대가 불명확한 행은 WebFetch:

```
대상 URL: SmartMovingSelf.java
추출 대상:
  1. <케이스 #N 대상 동작> 이 발동되는 조건 블록 전체
  2. 해당 동작에서 사용되는 모든 키 버튼(jump/sneak/grab/sprint) StartPressed/
     StopPressed/Pressed 참조
  3. 같은 프레임 내 동시 입력 처리 우선순위
```

---

## 6. 1:1 매핑 테이블

| 원본 | 1.21.1 | 상태 |
|---|---|---|
| `jumpButton.StartPressed` | `jumpKeyStartPressed` (엣지) | ✓ |
| `jumpButton.StopPressed` | `jumpKeyStopPressed` | ✓ |
| `jumpButton.Pressed` | `MinecraftClient.getInstance().options.jumpKey.isPressed()` | ✓ |
| `sneakButton.StartPressed/StopPressed/Pressed` | `sneakKeyStartPressed` / `sneakKeyStopPressed` / `sneakKey.isPressed()` | ✓ |
| `grabButton.*` | `SmartMovingKeys.grab.wasPressed()` / `.isPressed()` | ⚠️ wasPressed 는 LWJGL Button 의 Start/Stop 구분과 다름 — 검증 필요 |
| `sprintButton.*` | vanilla `player.isSprinting()` | ⚠️ sprint 키 엣지 감지 미구현 가능성 |
| `jumpPending` (vanilla 가로채기) | `SmartMovingClientState.jumpPending` | ⚠️ `jumpKeyStartPressed` 와 의미 충돌 가능 |

---

## 7. 구조적 차이 / 근사 이식 지점

- LWJGL Button.update() 시스템 → Fabric `KeyBinding.wasPressed()` + 수동 엣지 감지
- `jumpPending` (vanilla jump() 가로채기) + `jumpKeyStartPressed` (엣지) 이중 시스템 공존 —
  원본은 단일 Button 객체로 처리, 1.21.1 은 "vanilla jump 발동 여부" vs "단순 키 엣지" 둘 다 필요
- 같은 프레임 내 multiple 키 이벤트 순서 보장은 tickEssential 순서에 의존

---

## 8. 현재 구현 스냅샷

재현 케이스 확보 후 해당 처리 블록 임베드.

---

## 9. 예상 수정 diff

케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집
- [ ] P-1. §3 표 채우기 (최소 3행)
- [ ] P-2. 각 케이스의 원본 조건식 확인 (리서치 파일 + 필요 시 WebFetch)

### A. 원인 분석
- [ ] A-1. grab 키 Start/Stop 엣지 감지 구현 점검 — 현재 `SmartMovingKeys.grab.wasPressed()` 만 있음
- [ ] A-2. sprint 키 엣지 감지 구현 점검 — 미구현일 가능성
- [ ] A-3. `jumpPending` vs `jumpKeyStartPressed` 이중 시스템 충돌 점검

### B. 수정
- [ ] B-N. (케이스별)

### C. 검증
- [ ] C-1. 빌드
- [ ] C-2. 재현 케이스 전부 매칭
- [ ] C-3. 회귀 방지
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → `#1` 갱신

---

## 11. 호출 타이밍 검증

| 원본 이벤트 | 1.21.1 생성 위치 | 소비 위치 |
|-----------|---------------|----------|
| Button.update() 매 틱 | tickEssential 시작부 (현재 jump/sneak 만) | handleJumping / updateWallJumpState / R-09 |
| jumpButton.StartPressed 프레임 내 여러 번 참조 | `jumpKeyStartPressed` 필드 유지 (매 틱 초기화 X) | 동일 프레임 내 여러 핸들러 접근 가능 |

**의심**: grab/sprint 엣지 감지가 빠졌다면 이 표에 추가 필요.

---

## 12. 테스트 프로토콜

```
[T-N] 케이스별 키 시퀀스 테스트
  - 로그에 jumpKeyStart/Stop/sneakKeyStart/Stop/grabStart/Stop 출력
  - 키 입력 → 예상 이벤트 발화 → 처리된 동작 확인
  - 프레임 타이밍 확인 (키 입력 → 1틱 후 또는 2틱 후 처리)
```

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표 전부 매칭
- [ ] 각 키 이벤트 엣지 감지가 원본 Button.StartPressed/StopPressed 와 논리적으로 등가
- [ ] 같은 프레임 내 다중 이벤트 우선순위 원본과 일치
- [ ] `jumpPending` / `jumpKeyStartPressed` 이중 시스템 충돌 해결
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| R-09 토글 블록 | sneakKeyStartPressed/StopPressed 동작 변경 시 토글 깨짐 | [ ] |
| `wantWallJumping` 시스템 | jumpKeyStartPressed 의미 변경 시 벽점프 깨짐 | [ ] |
| 더블클릭 방향 점프 (IMPL-03) | prevPressLeft/Right/Back 엣지 감지 | [ ] |
| 차지 점프 (handleJumping) | sneak hold → 릴리즈 시 tryJump | [ ] |

---

## 15. 작업 기록

_(비어있음)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- grab/sprint 키 엣지 감지 보완이 범위 초과 시 별도 포커스로 분리
- 프레임 내 다중 이벤트 순서 보장 — 테스트 프로토콜 강화
