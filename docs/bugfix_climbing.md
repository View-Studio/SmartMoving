# 클라이밍 (Grab Key) 버그 픽스 트래킹

> 시작: 2026-04-27
> 사용자 보고: "그랩 키 클라이밍이 거의 동작 안 하는 수준".
> 작업 방식: **위에서부터 하나씩**. 한 번에 하지 말고, 각 항목별 원본 라인 단위 리서치 + 1:1 비교 + 매핑.

---

## 사용자 보고 버그 목록

### 🔴 BUG-1. 일반 블록 grab 클라이밍 — 뚝뚝 끊김
- 증상: 일반 벽 grab + W 시 모델 애니메이션 + 실제 클라이밍 동작 둘 다 끊김.
- 우선순위: 1 (가장 흔한 케이스).
- 상태: **미해결**.

### 🔴 BUG-2. 울타리 (Fence) — 클라이밍 아예 안 됨
- 증상: fence 옆 grab + W 발동 X.
- 우선순위: 2.
- 상태: **미해결**.

### 🔴 BUG-3. 철 창살 (Iron Bars) — grab 시 그 자리 고정, 릴리즈 시 풀림
- 증상: iron bars 옆 grab → 정지 (등반 X). grab 떼면 클라이밍 풀림 (낙하).
- 우선순위: 3.
- 상태: **미해결**.

### 🔴 BUG-4. 사다리/덩굴 — 애니메이션 안 나옴
- 증상: 사다리/덩굴은 자동 등반 동작 OK 추정. 그러나 클라이밍 모델 애니메이션 (팔/다리 잡는 자세 등) 안 나옴.
- 추가: 1.21.1 의 늘어진 덩굴 (cave vines) / 휘어진 덩굴 (twisting/weeping vines) 도 기본 덩굴과 같이 취급.
- 우선순위: 4.
- 상태: **미해결**.

---

## 작업 진행 방식

각 BUG 마다:
1. 원본 SmartMoving 1.7.10 코드 라인 단위 read (관련 모든 파일).
2. 우리 1.21.1 매핑과 1:1 비교.
3. 차이/누락 식별.
4. 수정 + 빌드 + 인게임 검증.

각 BUG 해결 시 이 파일에 진행 상황 기록.

---

## BUG-1: 일반 블록 grab 클라이밍 끊김

### 1-1. 리서치 대상 파일

**원본 (1.7.10)**:
- [ ] `SmartMovingSelf.java` 의 `onLivingUpdate` (전체 흐름) — handleClimbing 호출 시점.
- [ ] `SmartMovingSelf.java:814-1110` `handleClimbing` (이미 read).
- [ ] `SmartMovingSelf.java:1500-1551` `setShouldClimbSpeed/setOnlyShouldClimbSpeed` (이미 read).
- [ ] `SmartMovingSelf.java:2420-2495` 입력 처리 + wantClimbUp/Down (Agent 보고 대비 정밀 read).
- [ ] `SmartMovingSelf.java:resetClimbing` — 매 tick 어떤 필드 리셋되는지.
- [ ] `Orientation.java:300-423` `seekClimbGap` + `isLadderSubstitute` 본문 read.

**우리 (1.21.1)**:
- [ ] `SmartMovingClimber.java:handleClimbing` — 8방향 결과 누적 변경 후 정밀 read.
- [ ] `MixinLivingEntityClient.java:travel` — cancel 가드 변경 후 정밀 read.
- [ ] `SmartMovingClientState.java:tickEssential` — 매 tick 호출 흐름 + reset 시점.
- [ ] `MixinClientPlayerEntity.java:tickMovement` — tickEssential 호출 위치.

### 1-2. 가설 (해결 전)

- **가설 A**: handleClimbing 매 tick 호출 + isClimbing on/off 진동 → 끊김.
- **가설 B**: ci.cancel() 와 vanilla travel 의 비일관성 → server reject → ricochet.
- **가설 C**: `setOnlyShouldClimbSpeed` 의 `relevant` 가드 (`value < 0 || value > motionY`) 가 일부 frame 에서 실패 → motionY 미변경.
- **가설 D**: 8방향 seekClimbGap 결과가 일관되지 않음 → handsClimbing on/off → setShouldClimbSpeed 호출 안 함 진동.

### 1-3. 진행 로그

#### 2026-04-27 — 1차 수정

**발견 1**: `SmartMovingClimber.handleClimbing` 의 8방향 `seekClimbGap` 결과가 main `handsClimbing/feetClimbing` 변수에 누적 안 됨 — 원본은 main 과 inout 이 동일 시작 후 결과 다시 가져옴 (`SmartMovingSelf.java:937-958`). 우리는 inout 만 NONE 시작 → 일반 벽 인식 X → setShouldClimbSpeed 결정 시 None.
- **수정**: inout 시작값을 main 으로 + 4방향/8방향 후 main 갱신.

**발견 2**: `MixinLivingEntityClient.travel` 가 클라이밍 미발동 시도 `ci.cancel()` → vanilla travel 무력화 → player 정지.
- **수정**: `if (!sm.isClimbing && !sm.isCeilingClimbing) return;` 추가.

**발견 3** (★ 끊김 핵심): `setOnlyShouldClimbSpeed` 의 `isClimbing=true` 호출 시점이 원본과 1:1 안 됨.
- 원본 L1515: 진입 시 무조건 `isClimbing=true`.
- 우리 매핑: `relevant` 시만 `isClimbing=true`.
- 결과: motionY 가 이미 충분히 크면 `relevant=false` → `isClimbing` 안 set → travel inject 가 vanilla 진행 → 댐핑 안 됨 → 떨어짐 → 다음 tick 다시 발동 → **on/off 진동 = 끊김**.
- **수정**: `setOnlyShouldClimbSpeed` 진입 시 무조건 `isClimbing=true` set. `setShouldClimbSpeed` 도 setOnly 호출로 단순화. `isClimbingStill` 도 원본 1:1 추가 (HoldMotion 시).

**상태**: 빌드 성공. 인게임 검증 대기.

#### 2026-04-27 — 2차 수정 (1차 검증 후 "여전히 그 자리 고정")

**진짜 원인 발견**: 원본 `SmartMovingSelf.handleLand` (L655-659):
```java
move(motionX, motionY, motionZ, ...);    // 1. vanilla move() 호출로 위치 갱신
handleClimbing(...);                     // 2. motionY 새로 set (다음 tick 용)
setLandMotions(horizontalDamping);       // 3. motionY -= 0.08, *= 0.98 (gravity)
```

원본은 매 tick **명시적 `move()` 호출** + `setLandMotions` (gravity 적용).

우리 매핑은 travel HEAD inject + `ci.cancel()` → vanilla travel 본문 (안에 `player.move()` 있음) 미실행 → setVelocity 만으로 위치 갱신 X → player 가 그 자리 고정.

**수정** (`MixinLivingEntityClient.java:213-244`):
1. handleClimbing 후 `setLandMotions` 등가:
   - `motionY = (vel.y - 0.08D) * 0.98D` (gravity + drag).
   - `motionX/Z *= 0.91F` (air damping).
2. vertical clamp `-0.15` (원본 Self L780).
3. **`player.move(MovementType.SELF, player.getVelocity())` 명시적 호출** (원본 L655 1:1).
4. `ci.cancel()`.

**상태**: 빌드 성공. 인게임 재검증 대기.

#### 2026-04-28 — 4차 수정 (`_handClimbingHoldGap` static final 버그)

**사용자 보고**: "grab 만 누르고 W 안 누름 → 그 자리 정지 안 됨" (원본은 BOTTOM_HOLD → HoldMotion → 정지).

**원인**: `Orientation.java:480` 의 `_handClimbingHoldGap` 가 `static final` →
클래스 로드 시 한 번만 평가. Fabric 환경 클래스 로드 시점에 `SmartMovingConfig.Config` 미초기화 → freeClimbingUpSpeedFactor=0F → **_handClimbingHoldGap=0**.

→ `jh_offset < 0` 항상 false → `BottomHold` 분기 절대 발동 안 함 → 항상 `Up` →
wantClimbDown 분기 → `SinkDownMotion` → 천천히 하강.

**수정**: `static final` → 메서드 `_handClimbingHoldGap()` (매번 평가). Config 초기화 후 정상 0.06 반환. 호출처 6곳 변경.

**상태**: 빌드 성공. 인게임 재검증 대기.

#### 2026-04-27 — 3차 수정 (2칸 벽 + 속도 디테일)

**사용자 보고 후속**: 부드러운 등반 OK. 단:
1. 2칸 벽 점프 없이 안 올라감 (원본은 자동 등반).
2. 속도 디테일 원본과 다름.

**원인 분석** (원본 `SmartMovingSelf.java:979-1027` `wantClimbUp` 분기 정밀 비교):

원본 분기 6 단계 + `handsClimbing.toUp()` 호출:
1. `handsClimbing = handsClimbing.toUp()` (BottomHold→Up 전환).
2. L991 `feetClimbing.FastUp + onGround/bed 예외` → FastUpMotion.
3. L996 `(hasClimbGap||hasClimbCrawlGap) && handsClimbing.FastUp && (feet None or BaseWithHands)` → SlowUp/FastUp **★ 2칸 벽 자동 등반 분기**.
4. L1001 `feet+hands 둘 다 relevant + 3 예외 조합` → MediumUpMotion.
5. L1006 `handsClimbing.isUp()` → SlowUpMotion.
6. L1011 `TopHold || BaseHold || ...` → HoldMotion.
7. L1022 `Sink || ...` → SinkDownMotion.

우리 매핑은 5 단순 분기 (FAST/MEDIUM/SLOW/SLOW/HOLD) — 2칸 벽 분기 (★) + AllLimbed + Sink 누락. **`handsClimbing.toUp()` 호출 자체 누락**.

원본 L1521-1524 factor:
- `factor = getCombinedSpeedFactor()`.
- `if (isFast) factor *= sprintFactor`.

우리 매핑은 `combinedFactor` 만 사용, sprint factor 누락.

**수정** (`SmartMovingClimber.java:617-715`):
1. `handsClimbing = handsClimbing.toUp()` 추가.
2. `wantClimbUp` 7 분기 원본 1:1 매핑 (★ 2칸 벽 분기 포함).
3. `wantClimbDown` 도 원본 L1028-1053 1:1 매핑.
4. `if (sm.isFast) freeFactor *= cfg.sprintFactor` 추가.

**상태**: 빌드 성공. 인게임 재검증 대기.

---

## BUG-2 ~ BUG-4: (BUG-1 해결 후 진행)
