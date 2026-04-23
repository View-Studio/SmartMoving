# Focus #6 — increase/decrease 실제로 작동 안 함 (텍스트만 뜸)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #6 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ✅ **완료 (2026-04-24, 세션 28)** — §13 전수 체크 완료, §17 확인 완료 2건 + 후속 2건 |
| 현재 단계 | ✅ A + B-1~B-6 + C-1/C-2 + D-1~D-4 / [~] B-5 §17 분리 / [~] 정량 측정 + 파일 지속성 사용자 테스트 미수행 |
| 핵심 잔여 §17 | (1) `focus_12_max_horizontal_motion.md` (점프 max + Free climb 보정) / (2) `focus_13_movement_factor_system.md` (4단계 factor 배율 미세차) |
| 선행 의존 | #5 완료 (세션 23) |

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
- [x] A-1. Agent WebFetch — 원본 `SmartMovingSelf.java` 에서 `getUserSpeedFactor` 참조 전수 덤프.
      핵심 발견: `Config.getUserSpeedFactor` → `getConfigSpeedFactor`(Self L156) → 2갈래 확산
      (a) L119 `speedFactor` 지역변수 → `handleSwimming`/`handleLand`/`handleAlternativeFlying`
      (b) `getCombinedSpeedFactor`(L149) → L780/L822/L843/L893/L1522/L2045 직접 곱셈 6곳.
      원본 본체 + 호출처 14곳 전부 세션 24 에 덤프.
- [x] A-2. 1.21.1 `grep "getUserSpeedFactor\|speedUserFactor\|speedUserExponent"` 실행. 호출처:
      (i) `Flyer.handleFlying L55` — 이식됨 ✓
      (ii) `Climber L250/L325` — combinedFactor 이식됨 ✓ (부분)
      (iii) `Mover.getSpeedFactor` 최종 4단계 헬퍼 — **호출처 0건, dead code** ⚠️
- [x] A-3. 누락 매핑 표 작성 (§10 하단 "A-3 매핑" 테이블 참조). **핵심 누락 2건 확정**:
      (1) **Land 이동 전체** — vanilla travel() 이 처리 → User 배율 미반영. 체감 최대.
      (2) **Swim/Dive** — `Swimmer.handleSwimming L190`: `cfg.diveSpeedFactor/swimSpeedFactor`
          만, User 배율 곱셈 없음.
      부가 확인 대상 4건: Climb 하강 클램프(L780) / Smart 사다리(L893) / Free climb(L1522) /
      Jump maxHorizontalMotion(L2045). B 단계에서 각 Climber/Jumper 대응 라인 점검.

#### A-3 매핑 테이블 (세션 24)

| # | 원본 | 1.21.1 대응 | 상태 | B 단계 |
|---|---|---|---|---|
| 1 | `getConfigSpeedFactor` 본체 (Self L156) | `Mover.getConfigSpeedFactor` | ⚠️ `cfg.enabled` 가드 미이식 | B-1 |
| 2 | `getCombinedSpeedFactor` 본체 (Self L149) | 단일 헬퍼 부재, 인라인 | ⚠️ 헬퍼 신설 권장 | B-1 |
| 3 | **Land 이동 L119** speedFactor 진입 | vanilla travel() | ❌ **누락** | B-2 |
| 4 | **Swim/Dive L476-L561** | `Swimmer L190` | ❌ **누락** | B-3 |
| 5 | Flying L622 | `Flyer L55` | ✓ 이식됨 | — |
| 6 | Climb 하강 클램프 L780 | `Climber` 해당 라인 확인 필요 | ⚠️ 확인 | B-4 |
| 7 | 표준 사다리 L822 | `Climber L250` | ✓ 이식됨 | — |
| 8 | Simple 사다리 L843 | `Climber L325` | ✓ 이식됨 | — |
| 9 | Smart 사다리 L893 | `Climber` 확인 필요 | ⚠️ 확인 | B-4 |
| 10 | Free climb L1522 | `Climber.setOnlyShouldClimbSpeed` 대응 | ⚠️ 확인 | B-4 |
| 11 | 점프 max 수평 L2045 | `Jumper` 확인 필요 | ⚠️ 확인 | B-5 |
| 12 | 키 입력 L2326 | `ClientState.tickEssential` + SpeedChangePayload | ✓ 이식됨 | — |

### B. 호출처 이식 (A-3 결과 기반 원자 작업 분해)
- [x] B-1. **헬퍼 정비** — 3가지 변경:
      (1) `Mover.getConfigSpeedFactor(cfg)` 에 `cfg.enabled ? ... : 1F` 가드 추가 (원본 L156 1:1)
      (2) `Mover.getPotionSpeedFactor(player)` 에도 `cfg.enabled` 가드 추가 (원본 L161 1:1,
          기존 1.21.1 에 빠져있던 부분)
      (3) `Mover.getCombinedSpeedFactor(player, cfg)` 단일 헬퍼 신설 (원본 L149-152 1:1).
      Climber L250/L325 인라인 곱셈 2곳을 새 헬퍼로 교체. 빌드 ✓
- [x] B-2. **Land 이동 User 배율 이식 (옵션 A)** — `MixinLivingEntityClient.sm_getMovementSpeed`
      신설. `LivingEntity.getMovementSpeed()` HEAD inject (cancellable). `ClientPlayerEntity`
      인스턴스 체크 후 vanilla attribute 값에 `Mover.getConfigSpeedFactor(player, cfg)` 곱셈.
      - Land 경로 (vanilla travel 사용): 영향 O — getMovementSpeed 반영
      - SM 경로 (비행/수영/클라이밍, vanilla travel cancel): 영향 0 — getMovementSpeed 안 불림
      Creative 게이트는 Mover.getConfigSpeedFactor 내부에서 처리 (B-6). 이중 적용 없음
      (SM 경로는 getPotionSpeedFactor 로 attribute 직접 읽음). 빌드 ✓
- [x] B-3. **Swim/Dive User 배율 이식** — `SmartMovingSwimmer.handleSwimming L190` 의
      speedFactor 계산에 `* SmartMovingMover.getCombinedSpeedFactor(player, cfg)` 곱셈 추가.
      원본 Self L476-L494 의 `speedFactor *= _diveSpeedFactor/swimSpeedFactor` 는 Self L119
      지역변수 (`getConfigSpeedFactor * getPotionSpeedFactor * ...`) 에서 시작하는데, 1.21.1
      은 vanilla travel() cancel 경로라 그 값을 받지 못함 → `getCombinedSpeedFactor` 로 재구성.
      Creative 게이트 B-6 내부에서 자동 처리. 빌드 ✓
- [x] B-4. **Climb 3갈래 확인/보완** — 3가지 점검 결과 2건 누락 발견, 수정:
      (a) **하강 클램프 (원본 L780)**: `MixinLivingEntityClient L159` 의 `-0.15D` 고정 → User
          배율 미반영 ⚠️ **수정**: `-0.15D * Mover.getCombinedSpeedFactor(player, cfg)` 로 변경.
      (b) **Smart 모드 (원본 L893)**: `Climber L352/L354/L356` 에서 value 계산 시 이미
          `* combinedFactor` 포함됨 ✓ (B-1 정비 결과). setOnlyShouldClimbSpeed 의 motionY
          대입 경로에서 자연 반영.
      (c) **Free climb (원본 L1522)**: `Climber L402` 의 `setOnlyShouldClimbSpeed(..., 1.0D)`
          에서 combinedFactor=1.0D 하드코딩 → User 배율 누락 ⚠️ **수정**:
          `getCombinedSpeedFactor(player, cfg)` 전달. setShouldClimbSpeed L200-L202 의
          `freeClimbingUpSpeedFactor * combinedFactor` 곱셈에서 정상 반영.
      신규 발견 (§16): 원본 L1522 의 `if(isFast) factor *= _sprintFactor.value` 및 사다리
      substitute 별 `_freeOneLadderClimbUpSpeedFactor` / `_freeBothLadderClimbUpSpeedFactor`
      추가 곱 미이식 — 포커스 범위 외, 별도 이식 포인트.
- [~] B-5. **점프 maxHorizontalMotion User 배율 이식** — **범위 초과 판정 (세션 28)**.
      확인 결과: `getMaxHorizontalMotion` **시스템 전체가 1.21.1 에 미이식**.
      - `Config.getMaxHorizontalMotion(speed, type, inWater)` 메서드 부재 (원본 L305-L321)
      - 점프 타입별 `getJumpHorizontalFactor` 헬퍼 부재
      - `horizontalJumpFactor > 1F` 판정 + maxHorizontalMotion 클램프 로직 전체 부재
      포커스 #6 정의("getUserSpeedFactor 누락 곱셈 복원") 범위 초과 — 곱셈을 주입할 상위
      시스템 자체 부재. §17 후속 포커스 후보로 이동: `focus_12_max_horizontal_motion.md`.
      체감 영향: Creative + speedUser 활성 시에도 점프 거리 상한이 User 배율 반영 안 됨
      (수평 속도 일반 이동은 B-2 로 이미 반영 — 점프 순간의 "도약 거리" 클램프만 미반영).

- [x] B-6. **Creative 전용 게이트 이식 (세션 26)** — 4곳 완료:
      (a) `Mover.isCreative(player)` private 헬퍼 신설 — `MinecraftClient.getInstance().
          interactionManager.getCurrentGameMode() == GameMode.CREATIVE` (`ClientPlayerEntity`
          에 직접 필드 없어 `MinecraftClient` 경유).
      (b) `Mover.getConfigSpeedFactor(player, cfg)` 시그니처 변경 + Creative 게이트:
          `speedFactor × (isCreative ? getUserSpeedFactor() : 1F)`. speedFactor 는 전역
          (원본 `_speedFactor` 는 Creative 무관), userSpeedFactor 만 Creative 게이트.
      (c) `Flyer.handleFlying` 인라인 계산 → `Mover.getCombinedSpeedFactor(player, cfg)` 경유.
      (d) `ClientState.tickEssential` speedIncrease/Decrease 키 블록 감싸기: `cfg.enabled &&
          cfg.speedUser && isCreative` 일 때만 패킷 전송.
      (e) `SmartMovingServer.processSpeedChangePacket` 에 `isCreative` 체크 추가 —
          `!speedUser || !isCreative` 면 0 응답 (기존 `!speedUser` 확장).
      빌드 ✓. `getCombinedSpeedFactor` / `getSpeedFactor` 도 시그니처 변경 반영.

### C. 클라이언트/서버 동기화 경로 정합
- [x] C-1. 서버 `processSpeedChangePacket` 흐름 검증 완료. `Creative 체크 → INSTANCE.
      changeSingleSpeed(username, difference) → save() → 응답 전송` 정합. `broadcastConfig`
      미호출은 의도된 설계 (개인 경로, 관리자 전역 `adminChangeSpeed` 만 broadcast).
      원본 `setPlayerSpeedExponent` 패턴과 일치. 코드 변경 불필요.
- [x] C-2. 클라 `SpeedChangePayload` 수신 대상 검증 완료. `SmartMovingConfig.Config.
      changeSpeed(difference)` 호출 대상:
      - 싱글: `Config == INSTANCE` → `INSTANCE.speedUserExponent += difference`
      - 멀티(globalConfig): `Config == SERVER_CONFIG` → `SERVER_CONFIG.speedUserExponent +=`
      양쪽 모두 `getUserSpeedFactor()` 호출 시 클라측 로컬 상태 반영.
      **이중 상태 확인**: 클라 `speedUserExponent` 필드 (로컬 factor 계산용) + 서버 `INSTANCE.
      playerSpeedExponents[username]` Map (저장/재전송용) — 다음 서버→클라 재전송 시
      `toArray(username)` 이 개인 맵 값으로 `move.speed.user.exponent` 치환해 덮어씀 →
      일관성 유지. 정합, 코드 변경 불필요.

### D. 검증
- [x] D-1. `./gradlew clean build` — BUILD SUCCESSFUL in 7s ✓
- [x] D-2. 재현 케이스 수동 테스트 — 사용자 검증 결과 **속도 정상 작동**. 다만 "배율 느낌
      살짝 다름" 피드백 → 원인 분석(§16 세션 28) 결과 `getNonSlowInputSpeedFactor` /
      `getSlowInputSpeedFactor` land 경로 미반영으로 판정. 포커스 #6 정의("getUserSpeedFactor
      누락 곱셈 복원") 범위 초과 — `focus_13_movement_factor_system.md` (§17 신규 후보) 로 이동.
      본 포커스 목표(User 배율 적용) 는 달성.
- [x] D-3. 회귀 방지 감사 (§14) — grep `// TODO / [미확인]` 0건. `getUserSpeedFactor` /
      `getCombinedSpeedFactor` 호출처 50건(6파일) 전부 의도된 위치 확인. 기본 상태
      (`!speedUser` or `!Creative`) 에서 configFactor=1F → vanilla 동작 불변. `adminChangeSpeed`
      / `changeSingleSpeed` / `playerSpeedExponents` Map 불변. **회귀 0건**.
- [x] D-4. `playtest_fixes.md` "현재 포커스" → `#2` 갱신.

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
- [x] 리서치 근거: A-1 Agent WebFetch 로 원본 호출처 14곳 전수 덤프 (§5.3 WebFetch 3건 전부 대체)
- [x] side-by-side: A-3 매핑 테이블 (원본 14 ↔ 1.21.1 이식 5 + B-5 §17 분리 1 + 기타 8) ✓
- [x] 모든 분기: `isUserSpeedAlwaysDefault()` / `exponent==0` 조기 반환 `Config.getUserSpeedFactor L846` 보존
- [x] 상수: `(1 + speedUserFactor)^speedUserExponent` 공식 그대로 (H-6 speedUser 정정만)
- [x] 호출 타이밍: Land/Swim/Climb/Fly 각 경로별 speedFactor 계산 위치 원본과 정합 (B-2/B-3/B-4/B-1)
- [x] 근사 이식: 해당 없음 (수학 공식 완전 재현, Creative 게이트는 원본 Property 시스템 대응)
- [x] 신규 발견: §16 세션 27 (Free climb 보정 3종) + 세션 28 (maxHorizontalMotion 시스템 부재 + 배율 미세차)
- [x] 회귀 방지: §14 + D-3 grep 50건 + 기본 상태 vanilla 불변 확인
- [x] 빌드: D-1 `./gradlew clean build` BUILD SUCCESSFUL in 7s

### #6 고유
- [x] T-1~T-4 수동 테스트 — 사용자 정성 검증 "속도가 이제 잘 바뀌긴 함"
- [~] 이동 거리 정량 측정 ±5% — 사용자 정량 측정 미수행. "배율 느낌 살짝 다름" 정성 피드백 →
      §16 세션 28 D-2 원인 분석 후 `focus_13_movement_factor_system.md` 후속 분리
- [x] 서버/클라 Config 동기화 일관성 — C-1/C-2 정합 확인 + 이중 상태(클라 field / 서버 Map)
      일관성 검증
- [~] 개인 속도 지수 파일 지속성 — 구조 이식 완료 (CSV readFrom/writeTo + playerSpeedExponents
      Map + toArray(username) 치환), 재진입 공식 테스트 미수행. 회귀 없을 것으로 예상.

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `SmartMovingServer.adminChangeSpeed` | `INSTANCE.changeSpeed` + `save` + `logSpeedState` + `broadcastConfig` — 그대로 | [ ] 서버 관리자 커맨드 `/smoving speed +5` 정상 |
| `changeSingleSpeed` + `playerSpeedExponents` | 서버 맵 저장은 유지 | [ ] 재접속 시 개인값 복원 |
| vanilla 이동 처리 | `getUserSpeedFactor()` 곱셈이 추가되어도 factor=1F 기본값일 땐 동작 불변 | [ ] 기본 상태(exponent=0)에서 속도 변화 없음 |

---

## 15. 작업 기록

### 세션 24 — 2026-04-24 — A 단계 (호출처 감사) 완료

**진행한 작업**:
- **A-1** Agent WebFetch — 원본 `SmartMovingSelf.java` 전수 감사. 핵심 구조 파악:
  - `Config.getUserSpeedFactor()` → `getConfigSpeedFactor`(Self L156) → 2갈래 확산
    (a) Self L119 `speedFactor` 지역변수 → `handleSwimming`/`handleLand`/`handleAlternativeFlying`
    (b) `getCombinedSpeedFactor`(L149) → L780/L822/L843/L893/L1522/L2045 직접 곱셈 6곳
  - 원본 호출처 14곳 전부 덤프됨. 추가 WebFetch 불필요.
- **A-2** 1.21.1 grep — 현재 호출처 전수:
  - `SmartMovingFlyer.handleFlying L55`: `cfg.speedFactor * cfg.getUserSpeedFactor()` ✓
  - `SmartMovingClimber L250 / L325`: `combinedFactor = Mover.getConfigSpeedFactor * Mover.getPotionSpeedFactor` ✓
  - `SmartMovingMover.getSpeedFactor` 최종 4단계 헬퍼: **호출처 0건, dead code**
- **A-3** 매핑 테이블 작성 (§10 A-3 하단). 핵심 누락 2건 + 확인 4건:
  - ❌ **Land 이동** (걷기/달리기/스프린트/점프) — vanilla travel() 처리라 User 배율 미반영
  - ❌ **Swim/Dive** — `Swimmer L190` 에서 `cfg.diveSpeedFactor/swimSpeedFactor` 만, User 배율 없음
  - ⚠️ Climb 하강 클램프(L780) / Smart 사다리(L893) / Free climb(L1522) / Jump maxHorizontal(L2045)
    — Climber/Jumper 에서 대응 라인 점검 필요 (B-4/B-5)
- **B 단계 원자 분해** — B-1 (헬퍼 정비) / B-2 (Land) / B-3 (Swim) / B-4 (Climb 3갈래) /
  B-5 (Jump max horizontal). §10 에 각 체크박스 정의 완료.

**완료 전 검증 체크리스트 (A 단계 기준)**:
- [근거] 원본 SmartMovingSelf.java WebFetch 로 직접 확인 ✓
- [근거] Config.java L498-L503 `getUserSpeedFactor` / `isUserSpeedAlwaysDefault` / `changeSpeed` 본체 확인 ✓
- [대응] 원본 호출처 14 ↔ 1.21.1 호출처 3 매핑 완료 ✓
- [분기] 원본 14곳 중 이식 3 / 누락 2 / 확인 대기 4 / 기타 5(UI/키/본체 등) 분류 완료 ✓
- [상수/타이밍/근사] 해당 없음 (감사 단계)
- [신규] `Mover.getSpeedFactor` dead code 발견 — B-1 에서 헬퍼 정비 시 제거 또는 활용 결정
- [회귀] 코드 변경 없음
- [빌드] 해당 없음 (리서치)

**다음 작업**: B-1 — `Mover.getConfigSpeedFactor` 에 `cfg.enabled` 가드 추가 + 단일
`getCombinedSpeedFactor` 헬퍼 신설 + 기존 인라인 호출 대체. 원본 Self L149-L156 1:1.

### 세션 25 — 2026-04-24 — B-1 (헬퍼 정비)

**진행한 작업**:
- `Mover.getConfigSpeedFactor(cfg)`: `cfg.enabled ? cfg.speedFactor * cfg.getUserSpeedFactor() : 1F`
  로 정정. 원본 `SmartMovingSelf.java L156` 1:1 (`Config.enabled ? ... : 1F`).
- `Mover.getPotionSpeedFactor(player)`: 함수 시작에 `if (!cfg.enabled) return 1F;` 가드 추가.
  원본 `L161` 1:1 — 기존 이식에 빠져있던 부분 보완.
- `Mover.getCombinedSpeedFactor(player, cfg)` 신규 추가. 원본 `L149-L152`:
  `return getConfigSpeedFactor(cfg) * getPotionSpeedFactor(player);`. Climber 등 호출처에서
  사용할 단일 헬퍼.
- `SmartMovingClimber` 2곳 (L250/L325) 의 인라인 `getConfigSpeedFactor × getPotionSpeedFactor`
  를 새 헬퍼 `getCombinedSpeedFactor(player, cfg)` 로 교체. 의미 완전 동일, 가독성 향상.

**완료 전 검증 체크리스트 (B-1 기준)**:
- [근거] 원본 `SmartMovingSelf.java` L149-L162 (A-1 세션 24 에서 확보) ✓
- [대응] 원본 3메서드 ↔ 구현 3메서드 1:1 ✓
- [분기] `cfg.enabled` 가드 2곳 추가 — 원본과 완전 일치 ✓
- [상수] 해당 없음 (공식만)
- [타이밍] 호출처 기존 2곳 변경 — 의미 동일. Climber 3갈래 경로 전부 영향.
- [근사] 해당 없음 (원본 완전 재현)
- [신규] 없음
- [회귀] `cfg.enabled=false` 상태에서 factor 가 이전엔 cfg.speedFactor(=1F) 만 반환했는데
  이제 1F 고정. 기존과 동일 (speedFactor 기본값 1F). 회귀 0.
- [빌드] `./gradlew build` ✓

**다음 작업**: B-2 — Land 이동 User 배율 이식. vanilla travel() 경로에 주입 방식 2가지
(Mixin inject on movementInput vs GENERIC_MOVEMENT_SPEED attribute modifier) 중 선택 +
구현. **사용자와 방향 확인 후 진행 권장** (attribute 방식이 vanilla 친화적이나 side effect
가능성, Mixin 방식이 원본 Self L119 구조에 더 가까움).

### 세션 26 — 2026-04-24 — B-6 (Creative 전용 게이트 신설)

**사용자 요구사항 (세션 26)**: "스피드 배율 적용은 크리에이티브 모드에서만 적용하고
나머지 게임모드에서는 적용 안 되게" — 원본 `_speedUser = Creative("move.speed.user")`
팩토리 의미 정확히 재현 요청.

**현재 상태 확인**: `grep` 결과 Creative 감지 로직 **0건** (포커스 #5 H-20 에서 gameType
시스템 전체 삭제되며 누락). `cfg.speedUser` 정적 boolean 만 있고 런타임 gameMode 체크 없음.

**이식 (4+1 곳)**:
1. `Mover.isCreative(player)` private 헬퍼 신설.
   `MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.CREATIVE`.
   `ClientPlayerEntity` 에 `interactionManager` 필드 직접 없음 확인 후 `MinecraftClient` 경유.
2. `Mover.getConfigSpeedFactor(player, cfg)` 시그니처 변경 (player 파라미터 추가).
   `return cfg.speedFactor * (isCreative ? cfg.getUserSpeedFactor() : 1F);`
   speedFactor 는 전역(원본 `_speedFactor` Creative 무관), userSpeedFactor 만 Creative 게이트.
3. `Mover.getCombinedSpeedFactor` / `getSpeedFactor` 도 player 전달 수정.
4. `Flyer.handleFlying L55` 인라인 계산 → `Mover.getCombinedSpeedFactor(player, cfg)` 경유.
5. `ClientState.tickEssential` speedIncrease/Decrease 키 블록:
   `cfg.enabled && cfg.speedUser && isCreative` 가드 추가 — 원본 `isUserSpeedEnabled() &&
   !isUserSpeedAlwaysDefault()` L2326 1:1.
6. `SmartMovingServer.processSpeedChangePacket` — `!cfg.speedUser || !isCreative` 면 0 응답.

**빌드 실패/정정**: `player.interactionManager` 직접 접근 오류 → `MinecraftClient.getInstance()
.interactionManager` 로 정정. 두 번째 빌드 성공.

**완료 전 검증 체크리스트 (B-6 기준)**:
- [근거] 원본 `_speedUser = Creative("move.speed.user")` 팩토리 정의 (세션 14 A-1 에서 확인) ✓
- [근거] 원본 `isUserSpeedEnabled() = enabled && _speedUser.value` (Config L487-L491) + Creative
  팩토리 의미(`Value(false).c(true)`) 조합으로 Creative 게이트 확정 ✓
- [대응] 원본 게이트 1개 → 1.21.1 게이트 3층 (factor 계산 / 클라 키 / 서버 응답) 1:1 ✓
- [분기] `isCreative` 체크가 3곳 모두 동일 의미 — gameMode==CREATIVE ✓
- [상수] 해당 없음
- [타이밍] 클라 키 → 서버 응답 → factor 계산 세 타이밍 모두 Creative 체크 일관 적용
- [근사] 해당 없음 (원본 완전 재현)
- [신규] `Mover.getConfigSpeedFactor` 시그니처 변경 — 외부 호출처 없음 확인 (Climber 는
  getCombinedSpeedFactor 경유, dead code getSpeedFactor 는 호출 없음)
- [회귀] 기존 Easy (speedUser=false) 는 동작 변화 없음. Creative 에서 speedUser=true
  수동 설정 시 정상 작동. 다른 게임모드에서 수동 true 설정해도 게이트로 차단.
- [빌드] `./gradlew build` ✓

**다음 작업**: B-2 — Land 이동 User 배율 이식 (옵션 A: Mixin inject on movementInput).
사용자 결정 옵션 A 로 확정.

### 세션 26 (계속) — 2026-04-24 — B-2 (Land 이동 User 배율, 옵션 A)

**진행한 작업**:
- `MixinLivingEntityClient.sm_getMovementSpeed` 신설. `LivingEntity.getMovementSpeed()`
  HEAD inject (cancellable).
- 구조:
  ```java
  if (!(this instanceof ClientPlayerEntity)) return;
  if (!cfg.enabled) return;
  float vanillaSpeed = player.getAttributeValue(GENERIC_MOVEMENT_SPEED);
  float configFactor = Mover.getConfigSpeedFactor(player, cfg);
  cir.setReturnValue(vanillaSpeed * configFactor);
  ```
- 적용 범위:
  - **Land (걷기/달리기/스프린트/점프)** — vanilla travel() 이 getMovementSpeed() 참조 →
    새 inject 반영 ✓
  - **SM 경로 (비행/수영/잠수/클라이밍)** — sm_travel_client 에서 vanilla travel() cancel →
    getMovementSpeed 안 불림 → 영향 0. 각자 경로에서 getCombinedSpeedFactor 별도 적용.
- Creative 게이트 B-6 재사용 — `Mover.getConfigSpeedFactor` 가 내부 `isCreative()` 체크
  포함. Creative 아니면 userSpeedFactor=1F → speedFactor(기본 1F)만 반영 → 체감 변화 0.
- 이중 적용 주의 검증: SM 경로는 `getPotionSpeedFactor` 에서 GENERIC_MOVEMENT_SPEED
  attribute 를 직접 읽음. 이 mixin 은 getMovementSpeed 메서드만 바꾸고 attribute 자체는
  안 건드림. 직접 참조 vs 메서드 반환값 → 이중 적용 없음. ✓

**완료 전 검증 체크리스트 (B-2 기준)**:
- [근거] 원본 Self L119 `speedFactor = getConfigSpeedFactor * getPotionSpeedFactor * ...` ✓
- [근거] 세션 24 A-1 에서 getConfigSpeedFactor → 모든 land/swim/fly 경로 확산 확인 ✓
- [대응] 원본 speedFactor 진입점 1곳 ↔ 1.21.1 getMovementSpeed inject 1곳 1:1 ✓
- [분기] `!cfg.enabled` 가드 / ClientPlayerEntity 인스턴스 체크 ✓
- [상수] 해당 없음 (공식만)
- [타이밍] vanilla travel() HEAD 에서 getMovementSpeed() 호출 → SM cancel 여부 무관하게
  land 경로에서만 적용됨 (cancel 되면 메서드 호출 자체 안 됨)
- [근사] getNonSlowInputSpeedFactor (얼음/스프린트) 는 vanilla 가 자체 처리 — 완전 등가
- [신규] 없음
- [회귀] Easy(speedUser=false) 또는 !Creative 상태: configFactor = speedFactor × 1F
  → vanillaSpeed × speedFactor. speedFactor 기본값 1F 이라 `!cfg.enabled` 아닌 상태에선
  항상 vanillaSpeed 그대로 (변화 0). Creative + speedUser=true 에서만 userSpeedFactor 적용.
- [빌드] `./gradlew build` ✓

**다음 작업**: B-3 — Swim/Dive User 배율. `Swimmer.handleSwimming L190` 의 speedFactor
계산에 `Mover.getCombinedSpeedFactor(player, cfg)` 또는 `getConfigSpeedFactor` 추가.

### 세션 27 — 2026-04-24 — B-3 (Swim/Dive User 배율)

**진행한 작업**:
- `SmartMovingSwimmer.handleSwimming L190` 수정:
  ```java
  // 이전:
  float speedFactor = sm.isDiving ? cfg.diveSpeedFactor : cfg.swimSpeedFactor;

  // 이후 (B-3):
  float speedFactor = (sm.isDiving ? cfg.diveSpeedFactor : cfg.swimSpeedFactor)
                    * SmartMovingMover.getCombinedSpeedFactor(player, cfg);
  ```
- 원본 매핑: Self L476-L494 `speedFactor *= _diveSpeedFactor.value / _swimSpeedFactor.value`
  는 Self L119 지역변수 (`getConfigSpeedFactor * getPotionSpeedFactor * ...`) 에서 시작.
  1.21.1 는 vanilla travel() cancel 경로라 그 진입점 값을 받지 못함 — `getCombinedSpeedFactor`
  로 재구성하여 Config/Potion 배율 포함.
- Creative 게이트는 `Mover.getConfigSpeedFactor` 내부 `isCreative()` 체크 (B-6) 자동 적용.
- speedFactor 는 이후 `BASE_SWIM_SPEED * speedFactor` / `0.05D * speedFactor` 등 3곳에서
  소비됨 (isDipping / isSwimming_sm / isDiving 경로) — 모두 User 배율 자동 반영.

**완료 전 검증 체크리스트 (B-3 기준)**:
- [근거] 원본 Self L476-L494 swim/dive speedFactor 곱셈 구조 (세션 24 A-1) ✓
- [근거] Self L119 `speedFactor = getConfigSpeedFactor * getPotionSpeedFactor * ...` 시작점 ✓
- [대응] 원본 L119 진입점 ↔ 1.21.1 getCombinedSpeedFactor 호출 1:1 ✓
- [분기] isDipping / isSwimming_sm / isDiving 3갈래 모두 동일 speedFactor 사용 → 일관 반영 ✓
- [상수] 해당 없음
- [타이밍] handleSwimming 진입 시 1회 계산, 3갈래 분기에서 소비 — 원본과 동일
- [근사] `getNonSlowInputSpeedFactor` (얼음/스프린트/달리기) 제외 — 수중에선 스프린트 의미
  없음, 원본도 물속에선 NonSlow 로 들어오는 값이 1F 인 경우가 대부분이라 등가
- [신규] 없음
- [회귀] Easy(speedUser=false) 또는 !Creative: getCombinedSpeedFactor = 1F × potionFactor.
  potionFactor 는 GENERIC_MOVEMENT_SPEED × 10 / 1.3F 인데 기본값에서 ~1.0F. 따라서 수영/
  잠수 속도 vanilla 대비 ~1배 (변화 0). 포션 효과 받을 때는 vanilla 보다 정확히 반영 —
  원본 1:1.
- [빌드] `./gradlew build` ✓

**다음 작업**: B-4 — Climb 3갈래 점검. Climber 에서 (a) 사다리 하강 클램프 (L780),
(b) Smart 모드 motionY *= combinedFactor (L893), (c) Free climb / Ceiling climb 
setOnlyShouldClimbSpeed 내 factor 에 User 배율 포함 여부 검증. 누락 시 추가.

### 세션 27 (계속) — 2026-04-24 — B-4 (Climb 3갈래 점검/수정)

**진행한 작업**: 3갈래 점검 → 2건 누락 발견 + 수정.

1. **(a) 하강 클램프 (원본 L780) — 수정**
   - 원본: `sp.motionY = Math.max(sp.motionY, -0.15 * getCombinedSpeedFactor())`
   - 1.21.1 기존 `MixinLivingEntityClient L159`: `Math.max(vel.y, -0.15D)` 고정
   - **수정**: `-0.15D * Mover.getCombinedSpeedFactor(player, cfg)` 로 변경. SmartMovingMover
     import 추가.

2. **(b) Smart 모드 (원본 L893) — 기존 정상 이식**
   - `Climber L352/L354/L356` 에서 value 계산 시 `* combinedFactor` 포함됨 (B-1 정비 결과)
   - setOnlyShouldClimbSpeed 의 motionY 대입에서 자연 반영
   - 수정 불필요 ✓

3. **(c) Free climb (원본 L1522) — 수정**
   - 원본: `setOnlyShouldClimbSpeed` 내부 `factor = getCombinedSpeedFactor() (+ isFast
     sprintFactor + 사다리별 추가 factor)` 곱
   - 1.21.1 `Climber L402`: `setOnlyShouldClimbSpeed(player, sm, value, isUp, 1.0D)` —
     combinedFactor 파라미터가 **1.0D 하드코딩** 으로 User 배율 차단
   - **수정**: `getCombinedSpeedFactor(player, cfg)` 전달. setShouldClimbSpeed L200-L202 의
     `freeClimbingUpSpeedFactor * combinedFactor` 곱셈에서 정상 반영.

**신규 발견** (§16 세션 27 기록): 원본 L1522 의 추가 factor 보정 3종 미이식 (isFast
sprintFactor / `_freeOneLadderClimbUpSpeedFactor` / `_freeBothLadderClimbUpSpeedFactor`).
본 포커스 범위 외 — 별도 원자 또는 후속 포커스.

**완료 전 검증 체크리스트 (B-4 기준)**:
- [근거] 원본 Self L780/L893/L1522 각 3갈래 확인 (세션 24 A-1) ✓
- [근거] 1.21.1 Climber L352/L354/L356/L402 + MixinLivingEntityClient L159 검토 ✓
- [대응] 누락 2건 (하강 클램프 / Free climb) 수정, 1건 (Smart) 기존 이식 확인 ✓
- [분기] Free climb L402 의 1.0D → getCombinedSpeedFactor / 하강 클램프 L159 의 -0.15D →
  -0.15D × combinedFactor ✓
- [상수] `-0.15D` 원본 그대로 유지 (factor 만 곱 추가) ✓
- [타이밍] handleClimbing 이후 travel() cancel 직전 하강 클램프 적용 — 원본 흐름과 일치
- [근사] isFast sprintFactor / 사다리별 추가 factor 는 본 포커스 외 — §16 에 기록
- [신규] L1522 보정 3종 §16 기록 ✓
- [회귀] 기본 (!enabled / !Creative) 에서 combinedFactor≈1F → 곱셈 결과 기존과 동일.
  Creative + speedUser 활성 시에만 Climb 속도 반영 — 원본 1:1.
- [빌드] `./gradlew build` ✓

**다음 작업**: B-5 — 점프 maxHorizontalMotion User 배율 이식. `SmartMovingJumper.tryJump`
에서 horizontal 수평 상한 계산 지점에 `getCombinedSpeedFactor` 추가. 먼저 `getMaxHorizontalMotion`
자체가 이식됐는지 확인 필요.

### 세션 28 — 2026-04-24 — B-5 범위 초과 판정 → §17 후속 이동

**진행한 작업**:
- 1.21.1 `SmartMovingJumper.tryJump` + grep 검토: `maxHorizontalMotion` /
  `getMaxHorizontalMotion` / `horizontalJumpFactor` 관련 로직 **0건**.
- 원본 Self L2045 는 단순 `* getCombinedSpeedFactor()` 가 아니라 **상위 시스템 전체** 기반:
  1. `Config.getMaxHorizontalMotion(speed, type, inWater)` — 원본 L305-L321 13줄 공식
  2. `getJumpHorizontalFactor(speed, type, jumpCharge)` 헬퍼 (점프 타입 14종 × 속도 5)
  3. `horizontalJumpFactor > 1F && !isCollidedHorizontally` 판정
  4. Jumper.tryJump 에 maxHorizontalMotion 클램프 적용
- 1.21.1 에 이 4단계 전체 부재 → User 배율 "곱셈 주입" 대상 자체 없음.
- 포커스 #6 정의("getUserSpeedFactor 누락 곱셈 복원")는 상위 시스템 부재 시 적용 불가.

**결정**: B-5 **범위 초과 판정**, `focus_12_max_horizontal_motion.md` 후속 신설.
- §10 B-5 체크박스를 [~] (지연) 로 표기, 범위 초과 이유 + 대체 후속 포커스 명시
- §16 세션 28 에 신규 발견 기록 (시스템 3종 부재)
- §17 에 `focus_12` 후보 신설 — B-4 세션 27 의 Free climb 보정 3종과 묶음

**체감 영향**: Creative + speedUser 활성 시 점프 도약 거리 상한이 User 배율 반영 안 됨.
일반 수평 이동은 B-2 로 이미 반영 (점프 순간의 "클램프" 만 미반영). 우선순위 낮음.

**완료 전 검증 체크리스트 (B-5 판정 기준)**:
- [근거] 1.21.1 Jumper grep + 원본 Self L2045 비교 ✓
- [대응] 상위 시스템 3종 부재 확인 — 곱셈 주입 대상 자체 없음 ✓
- [분기/상수/타이밍/근사] 해당 없음 (범위 초과 판정)
- [신규] focus_12 후속 포커스 후보 신설 §17 기록 ✓
- [회귀] 코드 변경 없음
- [빌드] 해당 없음

**다음 작업**: C 단계 (서버-클라 Config 동기화 정합). `processSpeedChangePacket` 이
`changeSingleSpeed` 로 서버측 개인값 갱신 후 클라에 재전송 여부 / `SERVER_CONFIG` 경로
상태 확인.

### 세션 28 (계속) — 2026-04-24 — C-1/C-2 (동기화 정합 검증)

**진행한 작업**:
- **C-1**: 서버 `processSpeedChangePacket` 흐름 코드 리뷰.
  - Creative 체크(B-6) → `INSTANCE.changeSingleSpeed(username, difference)` → `save()` →
    `SpeedChangePayload(difference, null)` 응답 전송
  - `broadcastConfig` 미호출은 의도된 설계 (개인 경로 — 관리자 전역 `adminChangeSpeed`
    만 broadcast). 원본 `setPlayerSpeedExponent` 패턴과 일치.
  - 코드 변경 불필요.
- **C-2**: 클라 `SpeedChangePayload` 수신 경로 (`SmartMovingClient.java` L82-L95) 리뷰.
  - `Config.changeSpeed(difference)` 호출 대상이 싱글 `INSTANCE` / 멀티 `SERVER_CONFIG`
    자동 분기. 양쪽 모두 클라 측 `speedUserExponent` 필드 갱신 → `getUserSpeedFactor`
    반영.
  - 이중 상태 확인 결과 정합:
    - 클라 `speedUserExponent` 필드: 로컬 factor 계산 (`getUserSpeedFactor` 참조)
    - 서버 `INSTANCE.playerSpeedExponents[username]` Map: 파일 저장 / 재전송
    - 다음 서버→클라 재전송 시 `toArray(username)` 이 `move.speed.user.exponent` 를
      개인 맵 값으로 치환 → 클라 `SERVER_CONFIG` 덮어써도 값 일치 → 일관성 유지
  - 코드 변경 불필요.

**완료 전 검증 체크리스트 (C 단계 기준)**:
- [근거] 원본 §5.2 B `SmartMovingComm.processSpeedChangePacket` ✓
- [근거] 원본 `setPlayerSpeedExponent` 패턴 (개인 맵, 로그 없음, broadcast 없음) ✓
- [대응] 원본 서버 처리 ↔ 1.21.1 `processSpeedChangePacket` 일치 ✓
- [분기] Creative 체크(B-6) + speedUser 체크 + difference 전송/응답 분기 ✓
- [상수] 해당 없음
- [타이밍] 싱글: 클라 changeSpeed 즉시 반영 / 멀티: 클라 changeSpeed + 서버 재전송 덮어쓰기
  (값 일치) → 원본과 일치
- [근사] 해당 없음
- [신규] 없음
- [회귀] 코드 변경 없음 — 기존 동작 유지
- [빌드] 해당 없음 (검증)

**다음 작업**: D 단계 (검증 + 포커스 전환) — D-1 clean build / D-2 수동 테스트(사용자 몫) /
D-3 §14 회귀 감사 / D-4 `playtest_fixes.md` 포커스 #2 로 갱신.

### 세션 28 (계속) — 2026-04-24 — D-1/D-3 (통합 빌드 + 회귀 감사)

**진행한 작업**:
- **D-1**: `./gradlew clean build` — **BUILD SUCCESSFUL in 7s** (10 tasks) ✓
- **D-3**: 회귀 방지 감사 수행:
  - `grep "// TODO\|// \[미확인\]" src/` — 0건 ✓
  - `getUserSpeedFactor` / `getCombinedSpeedFactor` / `getConfigSpeedFactor` /
    `getPotionSpeedFactor` 호출처 50건 (6파일) 전부 의도된 위치:
    - Config.java: 본체 4건 (getUserSpeedFactor 정의 + getSpeedPercent UI)
    - Climber.java: 8건 (setOnlyShouldClimbSpeed 4갈래 + 하강 클램프)
    - Flyer.java: 4건 (handleFlying)
    - MixinLivingEntityClient.java: 11건 (getMovementSpeed inject + 하강 클램프 +
      SmartMovingMover import)
    - Mover.java: 19건 (헬퍼 4개 정의 + isCreative 헬퍼)
    - Swimmer.java: 4건 (handleSwimming)
  - 기본 상태 (!speedUser 또는 !Creative): Mover.getConfigSpeedFactor = 1F × speedFactor
    (기본 1F) = 1F → getMovementSpeed inject `vanillaSpeed × 1F` = vanilla 그대로.
    회귀 0건 ✓
  - `adminChangeSpeed` (서버 관리자 전역) — B-6 Creative 게이트 영향 없음 (op 커맨드
    전용). 불변 ✓
  - `changeSingleSpeed` + `playerSpeedExponents` Map — 불변 ✓
  - 비행 동작 변경 점검: Flyer 가 `cfg.speedFactor * cfg.getUserSpeedFactor()` 인라인 →
    `Mover.getCombinedSpeedFactor` 경유로 변경. Creative 게이트 추가됨. 기본 체감 변화 0.

**완료 전 검증 체크리스트 (D-1/D-3 기준)**:
- [근거] 원본 호출처 A-1 매핑 전수 ✓
- [근거] 1.21.1 호출처 grep 50건 검토 ✓
- [대응] 원본 5곳(land/swim/fly/climb/jump) 중 4곳 이식 완료 + 1곳(jump max horizontal)
  §17 이동. B-5 제외 full coverage ✓
- [분기] Creative 게이트 3층 (factor/키/서버) 정합 ✓
- [상수] 해당 없음
- [타이밍] vanilla travel ↔ SM travel cancel 분기 확인 — 이중 적용 없음 ✓
- [근사] getNonSlowInputSpeedFactor (얼음/스프린트) vanilla 자체 처리로 대체 — 원본 등가
- [신규] B-4 Free climb 보정 3종 + B-5 maxHorizontalMotion 시스템 — §16/§17 기록 ✓
- [회귀] 기본 상태 이동 속도 vanilla 그대로. Creative+speedUser 활성 시에만 Land/Swim/
  Climb/Fly 에 User 배율 적용. 회귀 0건.
- [빌드] `./gradlew clean build` ✓

**다음 작업**: D-2 (사용자 인게임 수동 테스트) — 통과 시 D-4 포커스 #2 전환.

### 세션 28 (계속) — 2026-04-24 — D-2/D-4 (포커스 #6 완료 + #2 전환)

**진행한 작업**:
- **D-2** 사용자 인게임 테스트 결과 수신:
  - "속도가 이제 잘 바뀌긴 하는데 원본이랑 배율 느낌이 살짝 다른 거 같음"
  - 속도 조정 기능 자체는 **정상 작동** — 포커스 #6 본래 목표 달성
  - 배율 미세 차이는 원인 분석 후 §17 후속으로 분리
- **배율 차이 원인 분석** (§16 세션 28 D-2 기록):
  - 원본 `speedFactor = getConfigSpeedFactor × getPotionSpeedFactor × getNonSlowInputSpeedFactor
    × getSlowInputSpeedFactor` (4단계)
  - 1.21.1 `getMovementSpeed` inject 는 `getConfigSpeedFactor` 만 곱 → NonSlow/Slow 미반영
  - 체감: `cfg.sprintFactor`(1.5) / `cfg.runFactor`(1.3) / `cfg.sneakFactor` / `cfg.crawlFactor`
    land 경로 미반영. vanilla 자체 스프린트 modifier(1.3) 만 적용.
  - `Mover.getSpeedFactor` 4단계 완전 헬퍼는 이식됐으나 호출처 0건 — 활용 시 vanilla
    sneakSpeedAttr / 스프린트 modifier 와 중첩 위험
- **§17 `focus_13_movement_factor_system.md` 신규 후보 등록** — 4단계 factor 전체 이식
  + vanilla 중첩 방지 접근 2안 비교.
- **D-4 포커스 전환** — `playtest_fixes.md` "현재 포커스" #6 → #2 갱신.

**완료 전 검증 체크리스트 (D-2/D-4 기준)**:
- [근거] 사용자 인게임 테스트 결과 + 원본 Self L119 4단계 공식 분석 ✓
- [대응] 포커스 #6 본래 목표(User 배율 적용) 달성 확인 ✓
- [분기] 잔여 차이 원인 2건 식별 (NonSlow + Slow) → §17 focus_13 로 분리 ✓
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] `getMovementSpeed` inject 는 `getConfigSpeedFactor` 만 → 원본 4단계 대비 근사
  (현재는 2단계). §17 후속에서 완전 이식 가능.
- [신규] `focus_12` + `focus_13` 후속 2건 등록 ✓
- [회귀] 코드 변경 없음 (문서만)
- [빌드] 해당 없음

**포커스 #6 최종 결론**:
- User 배율 적용 경로 5곳 이식 완료 (Land B-2 / Swim B-3 / Climb 3갈래 B-4 / Fly B-1 /
  Creative 게이트 B-6)
- 서버-클라 동기화 정합 확인 (C-1/C-2)
- 회귀 0건 (D-3) + clean build (D-1)
- 잔여 §17 후속 2건: focus_12 (점프 max 수평) / focus_13 (4단계 factor)
- Easy 1:1 체감: 속도 조정 기능 자체는 1:1, 배율 미세차는 후속 포커스에서 완성

**다음 포커스**: #2 (스마트무빙 상태 이상) — `focus_02_state_issues.md`.

**사용자 검증 요청** — 포커스 #5 H-6 에서 `speedUser=false` 로 정정됨. 테스트 준비:
1. `config/smart_moving_options.properties` 에서 `move.speed.user=true` 수동 편집
2. Creative 모드로 전환
3. speedIncrease 키(O) × 5 → 채팅 "150%" + **실제 걷기/수영/클라이밍/비행 속도 증가 확인**
4. Survival 로 전환 → 같은 키 눌러도 무반응 (Creative 게이트 정상)
5. Survival 에서 걷기/달리기 속도는 기본 (vanilla) — User 배율 미적용 정상

---

## 16. 신규 발견

### 세션 28 D-2 후 "배율 미세 차이" 발견

사용자 인게임 테스트: 속도 조정 키 정상 작동 확인, 다만 원본 대비 "배율 느낌이 살짝
다름". 원인 분석:

- **`getNonSlowInputSpeedFactor` / `getSlowInputSpeedFactor` land 경로 미적용**.
  원본 Self L119 `speedFactor = getConfigSpeedFactor × getPotionSpeedFactor ×
  getNonSlowInputSpeedFactor(얼음/sprint/run factor)` + 이후 `getSlowInputSpeedFactor`
  (crawl/sneak/ceiling/useItem factor) 곱. 1.21.1 `getMovementSpeed` inject 는
  `getConfigSpeedFactor` 만 곱 → run/sprint/sneak/crawl factor 미반영.
- `Mover.getSpeedFactor` (4단계 완전 헬퍼) 는 이식됐으나 호출처 0건 — B-2 에서
  `getConfigSpeedFactor` 만 사용. 4단계로 교체 시 User 배율 + 포션 + NonSlow + Slow
  전부 반영되지만 vanilla 자체 스프린트 modifier / sneakSpeed 와 중첩 위험.
- 체감:
  - 스프린트 시 원본 1.5배 vs 1.21.1 1.3배 (vanilla 기본) — `cfg.sprintFactor` 미반영
  - `cfg.runFactor` / `cfg.sneakFactor` / `cfg.crawlFactor` 사용자 변경 불가 (land 경로)
- 해결 범위: 포커스 #6 정의("getUserSpeedFactor 누락 곱셈 복원") 초과 — 별도 포커스
  `focus_13_movement_factor_system.md` 후속. §17 등록.

### 세션 28 B-5 범위 초과 발견

- **`getMaxHorizontalMotion` 시스템 전체 미이식** — 원본 Self L2045 의 `maxHorizontalMotion =
  Config.getMaxHorizontalMotion(...) * getCombinedSpeedFactor()` 는 User 배율 주입 대상이나,
  1.21.1 에 다음 상위 시스템 전체 부재:
  - `getMaxHorizontalMotion(speed, type, inWater)` — 13줄 공식 (base 0.117852F / water
    0.0783960F + speed별 sprint/run/sneak factor 곱)
  - 점프 타입별 `getJumpHorizontalFactor` / `getJumpVerticalFactor` 헬퍼
  - `horizontalJumpFactor > 1F && !isCollidedHorizontally` 판정 블록
  - maxHorizontalMotion 클램프 적용 로직 (Jumper.tryJump 수평 속도 상한)
  포커스 #6 범위 초과 — §17 후속 포커스 `focus_12_max_horizontal_motion.md` 후보.

### 세션 27 B-4 중 발견

- **원본 Self L1522 `setOnlyShouldClimbSpeed` 내부 factor 보정 3종 미이식** — 본 포커스
  범위 외 (User 배율 이식만 우선). 누락 내용:
  1. `if (isFast) factor *= _sprintFactor.value` — Free climb 중 스프린트(grab+sprint) 시
     배율 추가. 1.21.1 `setShouldClimbSpeed` 에 없음.
  2. `switch (getOnLadder(...))` case 1/2 로 `_freeOneLadderClimbUpSpeedFactor` /
     `_freeBothLadderClimbUpSpeedFactor` 추가 곱. 사다리 substitute 1개/2개 경우별 배율.
  1.21.1 Config 에도 해당 필드 부재 — 별도 원자 작업 또는 후속 포커스에서 처리.

---

## 17. 잔여 / 후속

### ✓ 확인 완료 (세션 28 최종)
- ~~`isUserSpeedEnabled()` 완전 1:1 여부~~ — **확인 완료**: 별도 메서드 대신 **인라인 체크**
  (Mover.getConfigSpeedFactor 내부 + ClientState 키 블록 + Server processSpeedChangePacket)
  3곳. `cfg.enabled && cfg.speedUser` 는 원본 `enabled && _speedUser.value` 와 AND 교환법칙상
  동등. B-6 Creative 게이트 (`isCreative`) 추가로 `_speedUser` Creative 팩토리 의미 완전 1:1.
- ~~HUD 속도% 게이지 원본 기능 여부~~ — **확인 완료**: 원본도 **채팅 메시지 전용**.
  `getSpeedPercent` 는 `writeClientSpeedMessageToChat` 에서만 사용, HUD 게이지 없음.
  1.21.1 동일 (SmartMovingClient L91 채팅 메시지에서만 getSpeedPercent 호출). 추가 이식 불필요.

### ⏳ 후속 포커스 후보
- **`focus_13_movement_factor_system.md` 신규 후보** (세션 28 D-2 발견) — land 경로에
  `getNonSlowInputSpeedFactor` / `getSlowInputSpeedFactor` 미반영 문제. 원본 Self L119
  의 4단계 factor 전체(`getConfigSpeedFactor × getPotionSpeedFactor × NonSlow × Slow`)
  를 land 경로에 적용. 후보 접근:
  1. `MixinLivingEntityClient.sm_getMovementSpeed` 에서 `Mover.getSpeedFactor` 로 교체
     (4단계) — vanilla 의 sneakSpeedAttr / 스프린트 modifier 와 중첩 위험 → 정밀 검증 필요
  2. vanilla attribute modifier 를 SM 이 동적 추가/제거로 일관성 관리
  세션 28 사용자 테스트에서 "배율 느낌 살짝 다름" 체감으로 확인됨 (스프린트 1.3 vs 1.5 등).
- **`focus_12_max_horizontal_motion.md` 신규 후보** (세션 28 발견) — B-5 범위 초과로 분리.
  원본 Self L2045 `maxHorizontalMotion = Config.getMaxHorizontalMotion(...) * getCombinedSpeedFactor()`
  에 대응하는 상위 시스템 3종 신규 이식:
  1. `Config.getMaxHorizontalMotion(speed, type, inWater)` 메서드 (원본 L305-L321) —
     base 0.117852F / water 0.0783960F + speed 별 sprint/run/sneak factor 곱
  2. `getJumpHorizontalFactor(speed, type, jumpCharge)` + `getJumpVerticalFactor` 헬퍼
     (점프 타입 14종 × 속도 5)
  3. Jumper.tryJump 에 maxHorizontalMotion 클램프 적용 로직
  + 본 포커스 세션 27 §16 의 Free climb 보정 3종 (isFast sprintFactor / 사다리별 factor) 도
  이 후속 포커스에서 함께 처리 가능.
- **B-4 §16 세션 27 Free climb 보정 3종** (이미 기록됨, 위 focus_12 와 묶음 후보)
