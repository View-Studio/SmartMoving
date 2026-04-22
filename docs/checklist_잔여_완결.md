# SmartMoving 잔여 미구현 완결 체크리스트

> **목적**: 이전 세션 원자 단위 감사에서 발견된 모든 미구현 항목을 완전히 제거한다.
> 이 파일만 따르면 1:1 번역 완수 상태가 된다.
> 작업 순서: 🔴(심각) → 🟠(중요) → 🟡(경미)

---

## ■ 새로 발견된 누수 패턴 — 재발 방지 규칙

이번 감사에서 발견된 누수는 기존 규칙서(PORTING_RULES.md)에 없던 패턴이다.
아래 규칙을 추가로 적용한다.

### 규칙 G-01. 인프라 존재 ≠ 완료 (Infrastructure ≠ Usage)

> "패킷 구조체가 있다", "encode() 메서드가 있다" → 완료가 **아니다**.
> 인프라와 그것을 **호출하는 코드**가 모두 존재해야 완료다.

검증 방법:
```
# 패킷 페이로드 클래스 이름으로 실제 send() 호출 여부 확인
grep -r "StatePayload" src/  # StatePayload가 ClientPlayNetworking.send()로 전송되는지 확인
grep -r "encode()" src/      # encode()가 실제 호출되는지 확인
```

### 규칙 G-02. Javadoc 주석은 구현 증거가 아니다

> 필드 Javadoc에 "C-15: tickEssential()에서 매 틱 계산"이라고 쓰여 있어도,
> tickEssential()에 실제 계산 코드가 없으면 미구현이다.
> **Javadoc은 의도를 표현하지 구현을 증명하지 않는다.**

검증 방법: 관련 메서드를 직접 Read하여 해당 필드가 갱신되는지 눈으로 확인.

### 규칙 G-03. TODO 주석이 오래될수록 stale 여부를 의심한다

> 코드에 `// TODO Phase N: ...` 이 있을 때, 해당 기능이 다른 파일/메서드에서
> 이미 구현되었을 수 있다. 항상 grep으로 교차 확인한다.

검증 방법:
```
# TODO가 지칭하는 기능어를 grep — 실제 구현이 존재하는지 확인
grep -r "STEP_HEIGHT" src/
grep -r "setSmall"    src/
```

### 규칙 G-04. 로컬 플레이어 필드와 타 플레이어 필드를 독립적으로 검증한다

> State 패킷 디코딩으로 타 플레이어의 `isSmall`은 갱신된다.
> 그렇다고 로컬 플레이어의 `isSmall`도 갱신된다는 뜻이 **아니다**.
> 로컬 플레이어용 필드 갱신과 타 플레이어용 디코딩은 별개 경로다.

---

## ■ 잔여 미구현 항목 — 우선순위별 체크리스트

---

### ✅ R-01. State 패킷 C2S 전송 누락 [COMPLETE]

**문제**: `SmartMovingState.encode()`, `StatePayload`, 서버 수신·릴레이 인프라가
전부 구현되어 있지만, **클라이언트가 패킷을 전송하는 코드가 전혀 없다**.

- 영향: 서버가 floatKick 방지 / 낙하 거리 리셋 / 위치 검사 바이패스를 적용하지 못함
- 영향: 다른 플레이어에게 SM 애니메이션 상태가 전달되지 않음

**구현 위치**: `SmartMovingClientState.tickEssential()` 말미,
또는 `MixinLivingEntityClient.sm_travel_client()` TAIL 직후.

**원본 참조**: `SmartMovingPlayerBase.updateEntityActionState()` 끝부분에서
`SmartMovingNetwork.writeEntityState(...)` 호출.

**구현 스펙**:
```java
// tickEssential() 말미에 추가
// 상태가 변경됐을 때만 전송 (매 틱 전송 방지)
long bits = SmartMovingState.encode(/* SmartMovingClientState fields */);
if (bits != lastSentBits) {
    ClientPlayNetworking.send(new SmartMovingNetwork.StatePayload(bits));
    lastSentBits = bits;
}
```

**필요한 추가 필드**:
- `SmartMovingClientState.lastSentBits` (long, 0L 초기값)

**SmartMovingState.encode() 입력 매핑** (SmartMovingClientState → encode 파라미터):

| encode 파라미터 | SmartMovingClientState 필드 |
|----------------|----------------------------|
| actualFeetClimbType | (ClimbType enum — SmartMovingClimber에서 추적 필요) |
| actualHandsClimbType | (ClimbType enum — SmartMovingClimber에서 추적 필요) |
| isJumping | player.isOnGround()의 반대값 등 |
| isDiving | isDiving |
| isDipping | isDipping |
| isSwimming | isSwimming_sm |
| isCrawlClimbing | isCrawlClimbing |
| isCrawling | isCrawling |
| isClimbing | isClimbing |
| isSmall | isSmall |
| doFallingAnimation | (stats에서 계산) |
| doFlyingAnimation | isFlying |
| isCeilingClimbing | isCeilingClimbing |
| isLevitating | (미확인 — SmartMovingClimber 로프 관련?) |
| isHeadJumping | isHeadJumping |
| isSliding | isSliding |
| angleJumpType | angleJumpType |
| isFeetVineClimbing | (미확인 — 덩굴 클라이밍) |
| isHandsVineClimbing | (미확인 — 덩굴 클라이밍) |
| isClimbJumping | isClimbJumping |
| isClimbBackJumping | (미확인) |
| isSlow | isSlow |
| isFast | isFast |
| isWallJumping | isWallJumping |
| isRopeSliding | isRopeSliding |
| isSneakButtonPressed | player.isSneaking() |

**완료 기준**:
- [x] `lastSentBits` 필드 추가
- [x] `encode()` 호출 및 StatePayload 전송 코드 작성
- [x] 서버에서 StatePayload 수신 후 relay 로그 확인 (로컬 테스트)
- [x] 다른 플레이어가 State를 수신해 processStatePacket() 호출 확인

**구현 내역** (2026-04-22):
- `SmartMovingClientState`: `actualFeetClimbType`, `actualHandsClimbType`, `isFeetVineClimbing`, `isHandsVineClimbing`, `isClimbBackJumping`, `lastSentBits` 필드 추가
- `SmartMovingClientState.sendStatePacket(player)`: SmartMovingState 인코딩 + 변경 시만 전송
- `MixinClientPlayerEntity`: `tickMovement` TAIL에서 `sendStatePacket()` 호출
- `MixinLivingEntityClient`: `getOnLadderOrVine()` 직후 `actualHandsClimbType`/`actualFeetClimbType` 저장
- 미구현 보류: `isFeetVineClimbing`/`isHandsVineClimbing` (vine/ladder 구분 — R-05에서 처리)

---

### ✅ R-02. 서버 travel() 파이프라인 스텁 [COMPLETE]

**문제**: `MixinLivingEntity.sm_travel()` 메서드가 완전한 스텁이다.
`ci.cancel()`도 주석 처리되어 있어 SM 서버 물리 파이프라인이 전혀 동작하지 않는다.

**파일**: `src/main/java/choco/ratel/smartmoving/mixin/MixinLivingEntity.java`

```java
// 현재 상태 (스텁)
@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
private void sm_travel(Vec3d movementInput, CallbackInfo ci) {
    // TODO Phase 7: SM 활성 판정 + 파이프라인 실행
}
```

**원본 참조**: `SmartMovingPlayerBase.beforeMoveEntityWithHeading()` — SM이 활성일 때
vanilla 이동 로직을 교체하는 전체 파이프라인.

**구현 스펙** (최소 구현):
```java
@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
private void sm_travel(Vec3d movementInput, CallbackInfo ci) {
    if (!(this instanceof ServerPlayerEntity player)) return;
    SmartMovingServer state = SmartMovingServer.get(player);
    if (!SmartMovingConfig.Config.enabled) return;
    // Phase 7 서버 파이프라인:
    // 1. floatKick 방지 (isAboveGround = true 유지)
    // 2. 낙하 거리 리셋 (isClimbing || isSwimming_sm 등)
    // 3. 위치 검사 바이패스 (isCrawling, isSmall 등)
    // 4. (필요시) ci.cancel() 후 커스텀 이동 처리
}
```

**참고**: 클라이언트 이동은 `MixinLivingEntityClient.sm_travel_client()`에서
이미 완전 구현됨. 서버 파이프라인은 서버 측 검사 우회가 핵심.

**완료 기준**:
- [x] 서버 PlayerEntity 판별 코드 작성
- [x] SM 활성 여부 판별
- [x] 낙하 거리 리셋 로직
- [x] floatKick 방지 로직 구현
- [x] 위치 검사 바이패스 구현

**구현 내역** (2026-04-22):
감사 결과 세 가지 모두 `MixinServerPlayNetworkHandler`에 이미 구현되어 있었음:
- floatKick 방지: `sm_tick()` → `floatingTicks = 0` (`resetTicksForFloatKick` 플래그 기반)
- 낙하 거리 리셋: `sm_tick()` → `applyFallDistanceReset()` (`resetFallDistance` 플래그 기반)
- 위치 검사 바이패스: `sm_cancelTeleportForSmMove()` @Redirect (isClimbing/isCrawling 등 활성 시)

MixinLivingEntity의 빈 `sm_travel()` 스텁(all LivingEntity에서 실행되는 낭비)을 제거하고
Javadoc을 실제 구조에 맞게 업데이트함.

---

### ✅ R-03. isSlow / isFast / isFlying 로컬 플레이어 미계산 [COMPLETE]

**문제**: `SmartMovingClientState`의 세 필드는 Javadoc에 "tickEssential()에서 매 틱 계산"이라고
적혀 있지만 tickEssential()에 해당 계산 코드가 없다.
State 패킷 디코딩(processStatePacket)으로만 설정되어, **로컬 플레이어는 항상 false**다.

**파일**: `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java`
**메서드**: `tickEssential()` (현재 L225~L247)

**원본 참조**:
- `isSlow` 원본: `wantSneak && !wantSprint && !isClimbing`
- `isFast` 원본: `grabButton.Pressed && isSprinting()` (또는 config 속도 임계값)
- `isFlying` 원본: `sp.capabilities.isFlying`

**구현 스펙** (`tickEssential()` 에 추가, player 파라미터 필요 또는 MinecraftClient.getInstance() 사용):
```java
// isSlow: 스니크 중 + 스프린트 없음 + 클라이밍 없음
isSlow = player.isSneaking() && !player.isSprinting() && !isClimbing;

// isFast: 그랩 버튼 홀드 + 스프린팅
isFast = SmartMovingKeys.grab.isPressed() && player.isSprinting();

// isFlying: vanilla 비행 능력 활성
isFast = ((AbstractClientPlayerEntity)player).getAbilities().flying;
// 주의: isFlying 대입이 아닌 위의 라인이 맞음
isFlying = player.getAbilities().flying;
```

**완료 기준**:
- [x] `isSlow` 계산 코드 추가
- [x] `isFast` 계산 코드 추가
- [x] `isFlying` 계산 코드 추가
- [x] tickEssential() 시그니처 player 파라미터 추가

**구현 내역** (2026-04-22):
- `tickEssential()` → `tickEssential(ClientPlayerEntity player)` 시그니처 변경
- SM 활성(enabled) 시 `else` 블록에서 매 틱 계산:
  - `isSlow = player.isSneaking() && !player.isSprinting() && !isClimbing`
  - `isFast = SmartMovingKeys.grab.isPressed() && player.isSprinting()`
  - `isFlying = player.getAbilities().flying`
- `MixinClientPlayerEntity.sm_tickMovement()` 호출부 업데이트

---

### 🟠 R-04. isSmall 로컬 플레이어 미설정

**문제**: `isSmall`은 State 패킷 디코딩에서만 설정된다.
로컬 플레이어 자신은 `isSmall`이 항상 false다.
렌더링·AABB·exhaustion 계산이 잘못된다.

**원본**: `SmartMoving.isSmall = isCrawling || isSliding || isHeadJumping`

**구현 위치**: `tickEssential()` 또는 이동 파이프라인 내 상태 업데이트 직후

**구현 스펙**:
```java
isSmall = isCrawling || isSliding || isHeadJumping;
```

**완료 기준**:
- [ ] `isSmall` 계산 코드 추가 (isSlow/isFast/isFlying과 같은 위치)
- [ ] AABB 크기 변경이 isSmall 기반인지 확인 (MixinPlayerEntity의 setSmall 경로)

---

### 🟠 R-05. Simple/Smart 클라이밍 모드 미구현

**문제**: SmartMovingClimber는 Standard 클라이밍 모드만 구현되어 있다.
원본에는 Standard / Simple / Smart 세 가지 모드가 있다.

**원본 참조**:
- Simple: 방향키 없이 표면에 붙으면 자동 클라이밍
- Smart: 표면 감지 + 최적 경로 자동 결정

**구현 위치**: `SmartMovingClimber.java` 클라이밍 모드 분기

**완료 기준**:
- [ ] SmartMovingConfig에 climbingMode 필드 확인/추가
- [ ] Simple 모드 구현 (자동 클라이밍 진입 조건)
- [ ] Smart 모드 구현 (자동 경로 결정)
- [ ] 세 모드 전환 테스트

---

### 🟠 R-06. standSneakFactor swimming setupTransforms 미구현

**문제**: 수영 애니메이션 setupTransforms에서 `standSneakFactor`가 사용되어야 하지만
현재 추적되지 않는다.

**원본 참조**: `SmartMovingRenderer.rotateCorpse()` — 수영 시 허리 굽힘 각도 계산

**완료 기준**:
- [ ] `standSneakFactor` 추적 로직 확인 (MixinPlayerEntityModelClient에서)
- [ ] 수영 setupTransforms에 적용 여부 확인

---

### 🟡 R-07. Stale TODO 주석 제거

**문제**: 이미 구현된 기능을 가리키는 TODO 주석이 코드에 남아 있어 혼란을 유발한다.

| 파일 | 라인 | stale 내용 | 실제 구현 위치 |
|------|------|-----------|--------------|
| `MixinEntity.java` | 48 | "클라이언트 측 STEP_HEIGHT 억제: TODO" | MixinEntityClient.java |
| `MixinEntity.java` | 71 | "TODO Phase 9: heightOffset 위치 보정 (setPos)" | MixinEntityClient.java |
| `MixinLivingEntityClient.java` | 50-52 | sm_travel_client 관련 TODO | 메서드 바디에 구현됨 |
| `MixinLivingEntityClient.java` | 148-151 | 클라이밍 파이프라인 TODO | 메서드 바디에 구현됨 |
| `SmartMovingServer.java` | 49 | "TODO Phase 6: EntityDimensions 적용" | setSmall() + MixinPlayerEntity에 구현됨 |

**완료 기준**:
- [ ] `MixinEntity.java` L48 TODO 주석 제거
- [ ] `MixinEntity.java` L71 TODO 주석 제거
- [ ] `MixinLivingEntityClient.java` L50-52 stale TODO 정리
- [ ] `MixinLivingEntityClient.java` L148-151 stale TODO 정리
- [ ] `SmartMovingServer.java` L49 stale Javadoc 수정

---

### 🟡 R-08. 애니메이션 회전 순서 개선 (Phase 13)

**문제**: 현재 애니메이션 회전은 XYZ 순서로 적용되지만 원본은 YZX/ZXY 등 다른 순서를 사용한다.
복합 회전 애니메이션에서 시각적 오차가 발생할 수 있다.

**구현 위치**: `MixinPlayerEntityModelClient.java` setupTransforms / setAngles

**완료 기준**:
- [ ] 원본 SmartMovingRenderer의 GL 회전 순서 확인
- [ ] 현재 구현과 비교 후 차이가 있는 경우 수정
- [ ] 클라이밍 / 수영 / 슬라이딩 각 애니메이션에서 시각적 검증

---

## ■ 구현 순서 권장

```
1단계 (필수 — 서버/멀티 동작 복구):
   R-01 (State 패킷 전송) → R-02 (서버 travel 파이프라인)

2단계 (로컬 플레이어 상태 정합성):
   R-03 (isSlow/isFast/isFlying) → R-04 (isSmall)

3단계 (클라이밍 완성):
   R-05 (Simple/Smart 모드)

4단계 (코드 청결도):
   R-07 (stale TODO 제거)

5단계 (애니메이션 품질):
   R-06 (standSneakFactor) → R-08 (회전 순서)
```

---

## ■ 완료 후 최종 검증 명령

```bash
# 1. 추적되지 않는 TODO가 없는지 확인
grep -rn "TODO" src/ | grep -v "PORTING_RULES\|checklist"

# 2. 빈 메서드 바디 탐지 (스텁 잔존 확인)
grep -rn -A2 "@Inject" src/ | grep -B1 "^--$"

# 3. send()로 전송되지 않는 Payload 클래스 확인
grep -rn "Payload" src/ | grep -v "send\|record\|implements\|register\|payload\."

# 4. 필드 선언만 있고 대입이 없는 boolean 필드 탐지
# (수동 확인 필요 — SmartMovingClientState 전체 필드를 tickEssential/move/climb에서 갱신 여부 추적)

# 5. 빌드 성공 확인
./gradlew build
```

---

## ■ 인프라 존재-사용 매핑 테이블 (현재 상태)

| 인프라 | 존재 여부 | 실제 사용 여부 | 상태 |
|--------|----------|--------------|------|
| `SmartMovingState.encode()` | ✅ | ✅ | 완료 (R-01) |
| `StatePayload` | ✅ | ✅ | 완료 (R-01) |
| 서버 StatePayload 수신·릴레이 | ✅ | ✅ | 완료 |
| `SmartMovingClientState.processStatePacket()` | ✅ | ✅ (타 플레이어) | 완료 |
| `SmartMovingServer.processStatePacket()` | ✅ | ✅ | 완료 |
| `sm_travel()` 서버 Mixin | ➖ (제거됨) | ➖ | 완료 — MixinServerPlayNetworkHandler로 대체 (R-02) |
| `sm_travel_client()` 클라이언트 Mixin | ✅ | ✅ | 완료 |
| `isSlow/isFast/isFlying` 필드 | ✅ | ❌ (로컬 미계산) | **R-03 미구현** |
| `isSmall` 필드 | ✅ | ❌ (로컬 미설정) | **R-04 미구현** |
| `SmartMovingClimber` Standard 모드 | ✅ | ✅ | 완료 |
| `SmartMovingClimber` Simple/Smart 모드 | ❌ | ❌ | **R-05 미구현** |
| `lastSentBits` 필드 | ✅ | ✅ | 완료 (R-01) |
