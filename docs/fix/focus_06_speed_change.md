# Focus #6 — increase/decrease 실제로 작동 안 함 (텍스트만 뜸)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #6 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (#5 완료 후 진입) |
| 현재 단계 | — |
| 선행 의존 | #5 (`enabled` / `toggler` 의미 확정) |

---

## 2. 증상 — 원본 vs 현재

### 원본 (1.7.10)

`speedIncrease` / `speedDecrease` 키 또는 `/smoving speed +N` 실행 시:
1. 채팅에 `"Smart Moving speed set to X% by user 'Y'"` 메시지
2. **실제 이동 속도가 X% 로 변함** — land/swim/climb 모든 이동 로직에 `getUserSpeedFactor()` 곱셈 적용

### 현재 1.21.1 구현

1. 채팅 메시지는 정상 출력
2. **이동 속도 변하지 않음** — `getUserSpeedFactor()` 가 실제 이동 처리에 **곱해지지 않음** (의심)

---

## 3. 재현 케이스 표

| # | 시나리오 | 원본 기대 | 현재 실제 | 검증 방법 |
|---|---------|---------|----------|----------|
| 1 | 싱글: speedIncrease × 5 후 달리기 | 5블록/초가 ≈8블록/초 | 변화 없음 | 5초 동안 이동한 거리 측정 |
| 2 | 싱글: speedDecrease × 5 후 달리기 | 5블록/초가 ≈2블록/초 | 변화 없음 | 동일 |
| 3 | 수영 중 speedIncrease × 3 | 수영 속도 증가 | 변화 없음 | 수중 10m 주파 시간 |
| 4 | 사다리 클라이밍 중 speedIncrease × 3 | 클라이밍 속도 증가 | 변화 없음 | 10블록 수직 등반 시간 |
| 5 | 멀티: 클라이언트가 키 누름 | 서버 개인값 저장 + 클라 즉시 반영 | 서버 저장만, 클라 미반영 | 서버 config 파일 `move.speed.users.exponents` 확인 |

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#5** — `enabled` / `toggler` 가 작동 게이트로 여전히 쓰이므로 먼저 확정 |
| 이 포커스가 영향 주는 대상 | 없음 (이동 속도 곱셈은 로컬 적용) |
| 영향받는 완료 이식 | `SmartMovingServer.processSpeedChangePacket` / `changeSingleSpeed` / `adminChangeSpeed` |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 파일 | 줄 | 내용 |
|------|---|------|
| `SmartMovingConfig.md` | L685-L691 | `getUserSpeedFactor()` 본체 `(1+factor)^exponent` |
| `SmartMovingConfig.md` | L670-L676 | `isUserSpeedEnabled()` / `isUserSpeedAlwaysDefault()` 게이트 |
| `SmartMovingSelf.md` | — | `landMotion` / `handleSwimming` / `handleClimbing` 에서 `getUserSpeedFactor()` 호출 위치 — **확보 필요** |
| `SmartMovingComm.md` | L59-L70 | 클라이언트 측 `processSpeedChangePacket` 수신 처리 |

### 5.2. 확보된 원본 Java 코드

**A. `SmartMovingConfig.getUserSpeedFactor()` 본체**
```java
public float getUserSpeedFactor() {
    if (isUserSpeedAlwaysDefault() || _speedUserExponent.value == 0)
        return 1F;
    return (float) Math.pow(1F + _speedUserFactor.value, _speedUserExponent.value);
}
public boolean isUserSpeedAlwaysDefault() {
    return !_speedUser.value || _speedUserFactor.value == 1F;
}
```

**B. 클라이언트 측 속도 변경 수신 (`SmartMovingComm.md` L59-L70)**
```java
public boolean processSpeedChangePacket(FMLProxyPacket packet, IEntityPlayerMP player, int difference, String username) {
    if (difference == 0)
        SmartMovingOptions.writeNoRightsToChangeSpeedMessageToChat(isConnectedToRemoteServer());
    else {
        Config.changeSpeed(difference);
        Options.writeServerSpeedMessageToChat(username, Config._globalConfig.value);
    }
    return true;
}
```

### 5.3. 확보 필요 (WebFetch 대상)

```
SmartMoving 1.7.10 원본에서 다음을 덤프:

대상 URL:
  - https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/moving/SmartMovingSelf.java

추출 대상:
  1. `getUserSpeedFactor()` 또는 `_speedUserFactor` 가 **호출/참조되는 모든 위치**
     (각 위치의 주변 5-10줄 포함, 어떤 속도 계수에 어떻게 곱해지는지)
  2. landMotion / handleSwimming / handleClimbing 내부에서 speedFactor 계산 블록
  3. `Config.changeSpeed(difference)` 가 실제로 영향을 주는 경로 —
     `_speedUserExponent` 가 갱신된 후 다음 틱에서 어떻게 movement 에 반영되는지

응답 형식: 원본 Java 코드 그대로, 해석 금지.
```

---

## 6. 1:1 매핑 테이블

| 원본 (1.7.10) | 1.21.1 포트 | 상태 |
|---|---|---|
| `Config.getUserSpeedFactor()` | `SmartMovingConfig.getUserSpeedFactor()` | ✓ 이식됨 |
| `Config.isUserSpeedEnabled()` | `cfg.speedUser && cfg.enabled` | ✓ 근사 |
| `Config.isUserSpeedAlwaysDefault()` | `!cfg.speedUser \|\| cfg.speedUserFactor==1F \|\| cfg.speedUserExponent==0` | ✓ 포함 (getUserSpeedFactor 내부) |
| `Config.changeSpeed(diff)` | `SmartMovingConfig.changeSpeed(diff)` | ✓ |
| `Config.changeSingleSpeed(player, diff)` | `SmartMovingConfig.changeSingleSpeed(username, diff)` | ✓ |
| `SmartMovingSelf.landMotion()` speedFactor 곱셈 | ?? | ❌ 확인 필요 |
| `SmartMovingSelf.handleSwimming()` speedFactor | `SmartMovingSwimmer.handleSwimming` 의 speedFactor | ❌ 확인 필요 |
| `SmartMovingSelf.handleClimbing()` speedFactor | `SmartMovingClimber.handleClimbing` | ❌ 확인 필요 |

---

## 7. 구조적 차이 / 근사 이식 지점

- Property 기반 `_speedUserExponent` → 단일 int 필드 (이미 단순화 완료)
- 개인 속도 맵 `_speedUsersExponents` → `Map<String,Integer> playerSpeedExponents` (이미 이식)
- 클라이언트가 `Config.changeSpeed()` 호출 시 `Config` 가 `SERVER_CONFIG` 인 상태에서
  다음 서버 재전송에 덮어씌워지는 시나리오 — 구조적 문제로 재검토 필요

---

## 8. 현재 구현 스냅샷

### 8.1. `SmartMovingClient.java` — SpeedChangePayload 수신

```java
ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
    (payload, context) -> {
        int difference = payload.difference();
        if (difference != 0) {
            client.execute(() -> {
                SmartMovingConfig.Config.changeSpeed(difference);   // ← INSTANCE or SERVER_CONFIG 둘 다 가능
                String percent = SmartMovingConfig.Config.getSpeedPercent();
                String msgKey = "100".equals(percent)
                    ? "smartmoving.message.speed.client.reset"
                    : "smartmoving.message.speed.client.change";
                if (client.player != null)
                    client.player.sendMessage(Text.translatable(msgKey, percent));
            });
        } else { /* no rights */ }
    });
```

### 8.2. `SmartMovingServer.processSpeedChangePacket`

```java
public static void processSpeedChangePacket(ServerPlayerEntity player, int difference) {
    if (!SmartMovingConfig.Config.speedUser) {
        ServerPlayNetworking.send(player, new SpeedChangePayload(0, null));
        return;
    }
    String username = player.getName().getString();
    SmartMovingConfig.INSTANCE.changeSingleSpeed(username, difference);
    SmartMovingConfig.save();
    ServerPlayNetworking.send(player, new SpeedChangePayload(difference, null));
}
```

### 8.3. `getUserSpeedFactor()` 호출처 검색 결과

```bash
grep -rn "getUserSpeedFactor" src/
# → ?? (A 단계에서 확인)
```

---

## 9. 예상 수정 diff

A 단계(호출처 감사) 결과 나온 후 확정. 예시 형태:

```diff
 // SmartMovingClimber.handleClimbing  (예시)
 double climbSpeed = CLIMB_SPEED_BASE * cfg.freeClimbingUpSpeedFactor;
+climbSpeed *= cfg.getUserSpeedFactor();   // 누락 — #6 복원

 // SmartMovingSwimmer.handleSwimming  (예시)
 float speedFactor = sm.isDiving ? cfg.diveSpeedFactor : cfg.swimSpeedFactor;
+speedFactor *= cfg.getUserSpeedFactor();  // 누락 — #6 복원
```

---

## 10. 원자 단위 작업 목록

### A. 호출처 감사
- [ ] A-1. Agent WebFetch — 원본 `SmartMovingSelf.java` 에서 `getUserSpeedFactor` / `_speedUserFactor` / `_speedUserExponent` 참조 위치 **전부** 덤프
- [ ] A-2. 1.21.1 `grep -rn "getUserSpeedFactor\|speedUserFactor\|speedUserExponent" src/` 실행 → 호출처 목록 작성
- [ ] A-3. 원본 호출처 vs 1.21.1 호출처 매핑 — 누락 위치 리스트업

### B. 호출처 이식 (A-3 누락 위치마다 원자 작업 추가)
- [ ] B-N. (A-3 결과에 따라 채움)

### C. 클라이언트/서버 동기화 경로 정합
- [ ] C-1. 서버 `processSpeedChangePacket` 이 `changeSingleSpeed` 로 서버측 개인값 갱신 후, 해당 플레이어에게 `broadcastConfig` 또는 **개인 Config 재전송** 호출 여부 확인
- [ ] C-2. 클라이언트 `SpeedChangePayload` 수신 시 `Config.changeSpeed()` 호출 대상(`INSTANCE` vs `SERVER_CONFIG`)이 원본 동작과 맞는지 재확인 + 수정

### D. 검증
- [ ] D-1. `./gradlew build` 성공
- [ ] D-2. 재현 케이스 T-1~T-5 수동 테스트 (이동 거리 측정)
- [ ] D-3. 회귀 방지 감사 (§14)
- [ ] D-4. `playtest_fixes.md` "현재 포커스" → `#2` 갱신

---

## 11. 호출 타이밍 검증

| 이벤트 | 원본 | 1.21.1 | 일치 |
|--------|------|--------|------|
| 키 누름 | speedIncreaseButton.StartPressed | `SmartMovingKeys.speedIncrease.wasPressed()` | ✓ |
| 서버로 요청 | 패킷 전송 | `SpeedChangePayload(1, null)` | ✓ |
| 서버 처리 | `SmartMovingServerComm.processSpeedChangePacket` → `options.changeSingleSpeed(player, diff)` | `SmartMovingServer.processSpeedChangePacket` → `INSTANCE.changeSingleSpeed(username, diff)` | ✓ |
| 서버 응답 | S2C 패킷 | `SpeedChangePayload(difference, null)` | ✓ |
| 클라이언트 반영 | `Config.changeSpeed(difference)` | `Config.changeSpeed(difference)` | ⚠ Config 대상 확인 필요 |
| 이동 시 곱셈 | 매 틱 landMotion 등에서 `getUserSpeedFactor()` | **?? 누락 의심** | ❌ A 단계 감사 |

---

## 12. 테스트 프로토콜

```
[T-1] 싱글 speedIncrease 측정
  1. 평지에서 player.getX() 기록
  2. 5초 전속력 달리기 (sprint hold)
  3. 최종 X 좌표 기록 → 거리 D0
  4. speedIncrease 키 × 5 (채팅: "150%")
  5. 5초 전속력 달리기
  6. 거리 D1 측정 → D1/D0 ≈ 1.5 여야 함

[T-2] Creative 비행 속도
  1. 기본 비행 속도 측정
  2. speedIncrease × 3
  3. 재측정 → 비행 속도 증가 확인

[T-3] 수영 속도 (3D)
  1. 수평 수영 10블록 시간 측정
  2. speedIncrease × 5
  3. 재측정

[T-4] 파일 지속성
  1. speedIncrease × 3
  2. 월드 나가기
  3. config/smart_moving_options.properties 에 `move.speed.user.exponent=3` 저장 확인
  4. 재진입 시 "130%" 메시지 + 속도 유지
```

---

## 13. 완료 전 검증 체크리스트

### 공통
- [ ] 리서치 근거: §5 WebFetch 대상 3건 완료
- [ ] side-by-side: 원본 호출처 vs 1.21.1 호출처 전부 일치
- [ ] 모든 분기: `isUserSpeedAlwaysDefault()` / `exponent==0` 조기 반환 포함
- [ ] 상수: `(1 + factor)^exponent` 공식 그대로
- [ ] 호출 타이밍: 각 이동 처리 블록의 speedFactor 계산 위치
- [ ] 근사 이식: 해당 없음 (수학 공식 완전 재현 가능)
- [ ] 신규 발견: §16
- [ ] 회귀 방지: §14
- [ ] 빌드: 성공

### #6 고유
- [ ] T-1~T-4 수동 테스트 전부 통과
- [ ] 이동 거리 측정값이 이론치(`1 + factor`)^exp 와 ±5% 이내
- [ ] 서버/클라이언트 Config 동기화 일관성 확인
- [ ] 개인 속도 지수가 파일 저장/로드에 살아남음

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `SmartMovingServer.adminChangeSpeed` | `INSTANCE.changeSpeed` + `save` + `logSpeedState` + `broadcastConfig` — 그대로 | [ ] 서버 관리자 커맨드 `/smoving speed +5` 정상 |
| `changeSingleSpeed` + `playerSpeedExponents` | 서버 맵 저장은 유지 | [ ] 재접속 시 개인값 복원 |
| vanilla 이동 처리 | `getUserSpeedFactor()` 곱셈이 추가되어도 factor=1F 기본값일 땐 동작 불변 | [ ] 기본 상태(exponent=0)에서 속도 변화 없음 |

---

## 15. 작업 기록

_(비어있음 — #5 완료 후 진입 시 기록)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- `isUserSpeedEnabled()` 가 완전 1:1 인지 — 원본 `enabled && _speedUser.value` vs 1.21.1 `cfg.speedUser && cfg.enabled` (순서만 다름, 의미 동등 추정)
- 속도 표시 UI(HUD 등)에 속도% 게이지 같은 원본 기능이 있는지 확인 — 없으면 별도 포커스
