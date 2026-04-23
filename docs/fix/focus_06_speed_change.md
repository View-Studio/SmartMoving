# Focus #6 — increase/decrease 실제로 작동 안 함 (텍스트만 뜸)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #6 일 때 진입.
> 이 문서 + playtest_fixes.md 2개만 열고 작업한다.

---

## 진행 상황

**상태**: ⚪ 대기 (#5 완료 후 진입)

**현재 단계**: —

---

## 증상

`speedIncrease` / `speedDecrease` 키바인딩을 눌러도, 또는 `/smoving speed +N` 커맨드를
실행해도 **채팅 메시지만** "Smart Moving speed set to X%" 출력. **실제 이동 속도는
변하지 않음**.

---

## 의심 지점

### (a) 클라이언트 응답 처리 경로 (SmartMovingClient.java L79-L104)
- 서버가 `SpeedChangePayload(difference, null)` 반환
- 클라이언트가 `SmartMovingConfig.Config.changeSpeed(difference)` 호출
- 문제: `Config` 가 `SERVER_CONFIG` 인 경우, 다음 서버 재전송에서 덮어써짐 → 실효 없음

### (b) 서버 측 처리 (SmartMovingServer.processSpeedChangePacket)
- 현재: `INSTANCE.changeSingleSpeed(username, difference)` 로 서버측 `playerSpeedExponents` 만 저장
- 서버가 플레이어에게 **새 Config 재전송을 안 함** → 클라이언트 config 에 개인값 반영 안 됨

### (c) `getUserSpeedFactor()` 적용 경로 — **주요 의심**
- `SmartMovingConfig.getUserSpeedFactor()` = `(1 + speedUserFactor)^speedUserExponent`
- **이 factor 가 실제 이동 속도에 곱해지는 위치가 없음** 가능성
- 원본 `SmartMovingSelf.landMotion` / `handleSwimming` / `handleClimbing` 등에서 이 factor 가 어떻게 사용되는지 감사 필요

---

## 원본 근거 (수집 필요)

### 확보 필요 (Agent WebFetch 또는 리서치 파일 감사)

| 항목 | 목적 |
|------|------|
| `SmartMovingSelf.landMotion` 본체 | `getUserSpeedFactor()` 호출 위치 + 이동 속도에 곱해지는 방식 |
| `SmartMovingSelf.handleSwimming` 속도 계수 | swim/dive 에서 userSpeedFactor 적용 위치 |
| `SmartMovingSelf.handleClimbing` | 클라이밍 속도에 factor 적용 여부 |
| `SmartMovingComm.processSpeedChangePacket` 클라이언트 측 (SmartMovingComm.md L59-L70) | 원본 수신 처리 — `Config.changeSpeed(diff)` 의 대상 |
| `SmartMovingConfig.isUserSpeedEnabled()` | factor 활성 게이트 |

---

## 원자 단위 작업 목록

### A. 원본 근거 수집
- [ ] `SmartMovingSelf.landMotion` / `handleSwimming` / 관련 속도 계수 계산 원본 덤프
- [ ] 리서치 파일 보완 (필요 시)

### B. `getUserSpeedFactor()` 적용 경로 감사
- [ ] 1.21.1 이동 처리 코드(`SmartMovingClimber` / `SmartMovingSwimmer` / vanilla travel 경로)에서 `cfg.getUserSpeedFactor()` 호출 여부 검색
- [ ] 누락된 위치 전부 식별 → 각각 원자 작업으로 분해
- [ ] 각 위치에 `* cfg.getUserSpeedFactor()` 적용 (단, `isUserSpeedEnabled()` 게이트 고려)

### C. 클라이언트/서버 동기화 경로 정합
- [ ] 서버 `processSpeedChangePacket` 응답 후 클라이언트 `Config` 가 실제 반영되는 흐름 재검토
- [ ] `changeSingleSpeed` 로 서버 저장된 개인값이 클라이언트에 전달되는 경로 확인 (broadcastConfig 호출 필요?)
- [ ] 필요 시 `processSpeedChangePacket` 에서 `broadcastConfig(server)` 또는 해당 플레이어에게만 재전송

### D. 검증 / 컴파일
- [ ] `./gradlew build` 성공
- [ ] 인게임 테스트: speedIncrease 키 → 실제 이동 속도 변화 확인
- [ ] playtest_fixes.md 의 "현재 포커스" 를 #2 로 갱신

---

## 완료 전 검증 체크리스트

- [ ] `getUserSpeedFactor()` 가 원본에서 호출되는 모든 위치에 1.21.1 구현도 호출하고 있다
- [ ] `isUserSpeedEnabled()` 게이트가 원본과 동일한 조건
- [ ] 클라이언트가 서버로부터 받은 개인 속도 지수가 실제 `Config` 에 반영됨
- [ ] 컴파일 성공
- [ ] 인게임 이동 속도 체감 확인

---

## 작업 기록

_(비어있음 — #5 완료 후 진입 시 기록)_

---

## 신규 발견

_(비어있음)_

---

## 잔여 / 후속

_(비어있음)_
