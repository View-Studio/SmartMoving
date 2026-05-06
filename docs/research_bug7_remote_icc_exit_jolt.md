# BUG-7 remote ICC EXIT — Y motion 덜컹 잔존 (2026-05-06)

## 1. 개요 + 현재 상태

**작업 주제**: remote (= 다른 플레이어) 가 ICC (isClimbCrawling) EXIT 시 Y 위치/render 동기화 BUG.

**핵심 메커니즘**:
- ICC dim 매핑 = SmartMoving 의 self side 가 사다리/그랩 climbing 시 entity.y 를 -1m down 으로 유지 (= dim 0.5m + offset).
- ICC EXIT 시 self.y 가 +1m up + gravity → server broadcast.
- **remote 측에서 svY 가 ICC 시점 (= -1m down) → +1m up 까지 lerp** = 1~3 tick 동안 +1m up 매끄러운 lerp 진행.
- **그러나 packet 처리 lambda 안에서 SM relay packet 으로 ICC=false 변경 즉시 dim 갱신 → bb 가 갑자기 +1m 위로 변경. lerp 진행 중 entity.y 와 dim 위치 mismatch → 사용자 시각 "1m down jump"**.

**현재 fix 버전**: **v25.7.10** (2026-05-06).

**현재 잔존 BUG**: 사용자 보고 "아까보단 괜찮은데 약간 덜컹하는 느낌이 있음" (2026-05-06 11:42 검증).

## 2. 사용자 보고 history

| 시점 | 보고 |
|------|------|
| 세션 35 BUG-7 | 1m down jump (= 박스/모델 1칸 아래 보임 → 위로 jump). |
| v25.3 정착 | "그랩류 완벽" + "ladder 약간 뒤로 끌림". |
| v25.6.x 진행 | bobbing iteration. |
| v25.7.x 진행 | "이제 자야되서 작업파일 만들어줘" (현재). |
| v25.7.6 후 | 사용자: "ladder 위아래 진동 큼". |
| v25.7.7 후 | dip 차단. 그러나 "+0.84m 1 tick lerp" 잔존. |
| v25.7.8 (롤백) | 그랩에 ladder fix 적용 → 사용자 거부 "당연히 다르게 해야지". |
| v25.7.9 후 | 변수 분리. |
| v25.7.10 후 | "약간 덜컹" 잔존 (= 현재). |

## 3. Fix 진화 timeline

### v25.3 (= BUG-7 초기 fix, 정착 = 2026-05-06 메모리)

**logic**:
- packet 처리 lambda 안 enter-edge 검출 (= ICC=true → ICC=false).
- `setPos(remote.x, remote.y + 1.0, remote.z)` 즉시 적용.
- `setBodyTrackingIncrements(0)` (= vanilla lerpPos 차단).

**의도**: ICC=false 와 동시에 entity.y +1m up → bb 일관성 유지.

**부작용 (= v25.5 발견)**: 사다리에서 +0.10m up bobbing.

---

### v25.5 (= ladder offset 0.84 도입)

**logic**:
- 사다리류 (= `remote.isClimbing()=true`) 시 offset = 0.84.
- 그랩류 시 offset = 1.0.

**의도**: server-side 가 setPos +1m + gravity (= -0.16m) 후 broadcast → svY = self.y + 0.84. **0.84 정확 일치**.

**부작용 (= v25.6 발견)**: server 가 2 단계 broadcast (1차 = setPos +1m only, 2차 = +1m + gravity 후) → 1차 svY=76.190 와 ourSetY=76.176+0.84=77.016 이 0.826m gap. 그러나 매 tick lerp 진행 → bobbing.

---

### v25.6 watchfix (= server-side 2 단계 broadcast 흡수)

**logic**:
- packet handler: `target.smIccExitWatchTicks = 10`. ourSetY 저장.
- MixinPlayerEntityClient.java `sm_handleRemoteFlyingExitYSync`: 매 tick svY 검사.
  - svY != ourSetY 시 svY=ourSetY 강제 reset.

**의도**: vanilla lerp 가 svY 따라 ourSetY 향해 안 가게 차단. svY=ourSetY → lerp dy=0 → Y 안정.

**부작용**: bti=0 강제 시 X/Z lerp 도 차단 (v25.6.3 발견).

---

### v25.6.3 ~ v25.6.6 watchfix 세부 조정

- v25.6.3: bti=0 제거 → svY reset 만.
- v25.6.4: 1차 broadcast svY 저장 + 2차 broadcast 검사.
- v25.6.5: ladder + 그랩류 모두 watchfix 적용.
- **v25.6.6 (정착)**:
  - svY > ourSetY (= 1차) → svY=ourSetY reset.
  - svY < ourSetY (= 2차, 진짜 풀림) → setPos(svY) + setBti(0) + return.

**효과**: bobbing 차단. 사용자 "그랩류 완벽" 보고. 그러나 ladder "뒤로 끌림" 잔존.

---

### v25.7.x — bodyYaw / X-Z 부호 반전 시도 (모두 롤백)

- v25.7 (옵션 1): bodyYaw force flip → 목 꺾임 BUG. 롤백.
- v25.7.5: X/Z 부호 반전. 롤백.

**교훈**: self/server 정보 일치. server 가 "안 되는" 게 아니라 사용자 시각 인지 차이 + lag 누적.

---

### v25.7.6 — ladder 분기 setBti(0) skip

**logic**:
```java
if (!isLadderType) {
    acc.sm_setBodyTrackingIncrements(0);
}
```

**의도**: ladder 의 "뒤로 끌림" = packet handler bti=0 → 다음 broadcast 미도달 동안 X/Z lerp 차단 → 3 tick 정지 → 누적 lag → 큰 catch-up motion. **ladder 만 setBti(0) skip → vanilla setBti(3) 그대로 → X/Z lerp 정상**.

**효과**: ladder 뒤로 끌림 차단.

---

### v25.7.7 — ladder 분기 watchfix threshold 0.5m

**logic**:
```java
double thresh = sm.smIccExitIsLadder ? 0.5 : 0.0;
if (svY < sm.smIccExitOurSetY - thresh) {
    setPos(svY); setBti(0); return;
}
```

**의도**: ladder offset 0.84 와 self side gravity 결과의 미세 0.016m gap 이 watchfix 의 svY < ourSetY 분기 발동 → -0.016m dip → 사용자 "위아래 진동" 인지. **threshold 0.5m → 미세 gap 흡수, 진짜 풀림 (= -1m down) 만 setPos**.

**효과**: ladder dip 완전 차단. 그러나 사용자 보고 "ladder 덜컹" 잔존.

---

### v25.7.8 (롤백) — 그랩에도 같은 logic 통합 시도

**사용자 거부**: "당연히 다르게 해야지. 서로 영향 주면 안 되지". → v25.7.7 복원.

---

### v25.7.9 — 그랩에 ladder logic 적용 + **변수 분리**

**사용자 명시**: "그랩 다시 수정해봐 ladder 랑 같은 로직으로, 근데 둘이 당연히 변수는 따로 써야된다".

**logic**:
- SmartMovingClient.java 상수:
  ```java
  ICC_EXIT_LADDER_Y_OFFSET = 0.84;
  ICC_EXIT_REMOTE_Y_OFFSET = 1.0;   // 그랩용
  ICC_EXIT_LADDER_Y_THRESH = 0.5;
  ICC_EXIT_GRAB_Y_THRESH = 0.5;
  ```
- packet handler:
  ```java
  target.smIccExitYThresh = isLadderType ? LADDER_Y_THRESH : GRAB_Y_THRESH;
  ```
- watchfix:
  ```java
  if (svY < sm.smIccExitOurSetY - sm.smIccExitYThresh) { ... }
  ```
- ladder + 그랩 모두 setBti(0) skip.

**효과**: ladder/그랩 별도 변수, 미세 조정 자유. 사용자 보고 "ladder 덜컹" 잔존.

---

### v25.7.10 (현재) — render lerp prevY/lastRenderY 동기화

**root 분석**:

dump (v25.7.9 시점, 첫 ladder EXIT) 의 frame 단위 추적:
| frame | render Y | 변화 |
|-------|----------|------|
| L1372 t=29 pt=0.06 | 76.156 | 매끄러움 |
| L1373 t=29 pt=0.24 | 76.158 | +0.002 |
| L1374 BEFORE | (packet 도달) | |
| L1375 setPos +0.84 | y=77.007 | |
| L1377 t=29 pt=0.42 | **76.513** | ★ **+0.355m jump** |
| L1378 t=29 pt=0.58 | 76.650 | +0.137 |
| L1379 t=29 pt=0.76 | 76.803 | +0.153 |
| L1380 t=29 pt=0.94 | 76.956 | +0.153 |
| L1381 [B-TICK] t=29 | 77.007 | tick 종료 |

★ **L1373 → L1377 = 1 frame 안 +0.355m up render jump**.

**원인**: vanilla render lerp = `lerp(prevY, y, pt)`. packet lambda 가 entity.y +0.84m setPos 만 호출. **prevY 는 ICC 마지막 frame 의 76.156 그대로**. 다음 frame 의 lerp = `lerp(76.156, 77.007, 0.42)` = 76.513. 이게 frame 단위 jump.

**fix logic**:
```java
remote.setPosition(remote.getX(), newY, remote.getZ());
remote.lastRenderY = newY;  // ← 추가
remote.prevY = newY;        // ← 추가
acc.sm_setServerY(newY);
```

**효과 (= v25.7.10 dump 검증, L2665~L2700)**:
| frame | render Y | y | lrY | prevY |
|-------|----------|---|-----|-------|
| L2664 t=29 pt=0.28 (직전) | 76.150 | 76.161 | 76.146 | 76.146 |
| L2665 BEFORE | y=76.161 | | | |
| L2666 setPos +0.84 | y=77.001 | | | |
| **L2668 t=29 pt=0.46 (직후)** | **77.001** | 77.001 | **77.001** | **77.001** |
| L2669~ stable | 77.001 | 77.001 | 77.001 | 77.001 |

★ **render = lerp(77.001, 77.001, *) = 77.001** (= dy=0, 1 frame instant teleport + 즉시 안정).

**4 EXIT 모두 동일 패턴 검증**: 1차 ladder, 2차 그랩, 3차 ladder, 4차 그랩 모두 동기화 작동.

**잔존 사용자 인지 BUG**: "약간 덜컹".

## 4. 잔존 "약간 덜컹" 분석

### 가능 root 1 — 1 frame instant teleport 자체

**현재 동작**: packet 직전 frame 의 render → 직후 frame 의 render 가 +0.84/+1.0m 즉시 변화.
- 1차 ladder: 76.150 → 77.001 (= +0.851m, 1 frame).
- 2차 그랩: 78.146 → 79.154 (= +1.008m, 1 frame).
- 3차 ladder: 76.144 → 77.004 (= +0.860m, 1 frame).
- 4차 그랩: ~78 → ~79 (= +1.000m, 1 frame).

**시각**: "위로 instant 점프 후 안정". **사용자 인지 "덜컹"**.

**대안 fix 옵션**:
- A. **lastRender + prev 동기화 안 함** → v25.7.9 의 1 frame +0.355m jump + 4 frame lerp. 사용자 "덜컹 진행" 인지 (= 이전 동작).
- B. **현재 (v25.7.10)** → 1 frame +0.851m teleport. 사용자 "instant 점프" 인지.
- C. **부분 동기화** = lastRenderY/prevY 를 newY 가 아닌 중간값 (예: y - 0.4m) 로 set. lerp 가 partial 진행.
- D. **offset 줄이기** = 0.84 → 0.4. 절대값 작음. bb 잘못 위치.
- E. **ICC dim 자체 수정** = ICC 동안 bb 가 +1m up 위치. self side refactor.

### 가능 root 2 — ICC 중에 X/Z 의 lag 효과

ICC 동안 self.x/z 매 tick +0.x m motion. **remote 측 entity.x/z 가 lerp lag** (= server svZ 와 0.x m 차이).

EXIT 시점:
- entity.x/z = lerp 진행 위치.
- 우리 setPos 가 x/z 안 건드림.
- 그러나 packet 처리 후 vanilla lerpPos 가 svX/svZ 따라 매끄럽게 진행 → catch-up.

**현재 dump (v25.7.10) 의 X/Z**:
- 매 frame +0.012m south 매끄러운 lerp.
- 사용자 시각 = "정면 방향 자연 motion".

**그러나** ICC EXIT 시점에 entity.x/z 가 lerp lag (예: 0.1m south 차이) → catch-up 으로 빠르게 진행. **이게 Y teleport 와 결합 = "덜컹"**.

**fix 옵션**:
- F. **lastRenderX/Z 도 동기화** (= 실제 svX/svZ 까지). 그러나 svX/svZ 가 ICC 시점이라 너무 north → entity.x/z 가 svX/svZ 향해 lerp 가 backward. **위험**.
- G. **packet handler 의 setPos 시 lerpPos 도 같이 svX/svZ 까지** (= entity.x/z 즉시 svX/svZ 반영). 사용자 "뒤로 jump" 인지. **위험**.

### 가능 root 3 — bodyYaw motion 끝 인지

dump (v25.7.10 1차 ladder):
- L2665 BEFORE bodyYaw=-1.29.
- L2668 AFTER bodyYaw=-1.48 (= packet 후 첫 partialTicks).
- L2669 -1.42, L2670 -1.37, L2671 -1.31.
- L2672 [B-TICK] t=29 bodyYaw=-1.29.
- L2674~L2679 t=28: -1.26 → -1.06.

★ **bodyYaw 변화 매 frame -0.04~0.06°. 매끄러움**.

**그러나 ICC 마지막 (L2660 [B-TICK] t=30) bodyYaw=-1.64** → EXIT 후 -1.29 (= +0.35°).

**작은 변화. 인지 미미**.

### 가능 root 4 — pose 갱신 timing

dump (v25.7.10 1차 ladder):
- L2664 t=29 pt=0.28 pose=STANDING bb=[Z-300.600..-300.000] (= 1m 깊이).
- L2668 t=29 pt=0.46 pose=STANDING bb=[Z-300.600..-300.000] (= 같음).
- L2674 t=28 pt=0.12 pose=STANDING bb=[Z-300.532..-299.932] (= 약간 north shift, X/Z lerp).
- L2676 t=28 pt=0.46 icc=false crawl=false climb=false pose=STANDING bb=[Z-300.532..-299.932].
- L2682 t=27 pt=0.14 pose=STANDING (= 변화 없음? 또는 SWIMMING).
- L2684 t=27 pt=0.48 pose=SWIMMING bb=[Z-300.448..-299.848] (= pose 변화).

**pose 변화**: STANDING → SWIMMING (= crawling). t=27 partialTicks 0.48 시점.

**bb 변화**:
- ICC 동안: bb=[Z-300.667..-300.067] (= 1m 깊이).
- EXIT 직후: bb=[Z-300.600..-300.000] (= 같은 1m 깊이).
- t=28 동안: bb=[Z-300.532..-299.932] (= X/Z lerp 따라 north shift).
- t=27 SWIMMING: bb=[Z-300.448..-299.848] (= 더 north + height SWIMMING dim 변경 가능).

**SWIMMING dim** (vanilla):
- height = 0.6m.
- vs STANDING height = 1.8m.

★ **t=27 의 pose=SWIMMING → bb height 변화**. **이게 "덜컹" 의 가능 root**:
- packet 도달 시 ICC=false + crawl=true 변경.
- 그러나 vanilla pose 가 SWIMMING 으로 즉시 안 변함 (= t=27 도달 까지 lag).
- t=27 pose=SWIMMING 시 bb height 가 1.8 → 0.6m 변화.
- **render Y 와 bb 위치 mismatch 가능**.

**fix 옵션**:
- H. **packet handler 시 pose 강제 SWIMMING** (= ClientPlayerEntity.setPose). 그러나 vanilla 가 다음 tick 매 tick pose 갱신 → 영향 미미.
- I. **packet handler 시 calculateDimensions 호출** (= 이미 있음, L100). 효과 검증 필요.

### 종합 — "약간 덜컹" 의 가장 가능 root

**1순위**: 1 frame +0.85/+1.0m instant teleport 자체 (= visual 변화 큼).

**2순위**: pose=STANDING → SWIMMING 의 bb height 변화 timing (= 1~2 tick lag).

**3순위**: X/Z lerp lag catch-up 과 Y teleport 결합.

## 5. 다음 fix 옵션 (우선순위)

### 옵션 A — 부분 동기화 (= teleport 크기 줄이기)

```java
double partialOffset = offset * 0.5;  // 0.42 또는 0.5 비율.
remote.lastRenderY = newY - partialOffset;
remote.prevY = newY - partialOffset;
```

**effect**: render 가 partial frame jump (= +0.4~0.5m) + 다음 partialTicks lerp 가 +0.4~0.5m 추가. **2-step 분할**. 둘 다 작은 jump.

**risk**: lerp 진행 시 entity.y (= newY) 와 prevY (= newY - 0.4) 사이 lerp → 매 partialTicks +0.4m * pt 진행. **여전히 frame jump 가능**.

### 옵션 B — pose 강제 변경 + dim 즉시 반영

```java
// packet handler 안:
remote.setPose(net.minecraft.entity.EntityPose.SWIMMING);  // 또는 STANDING
remote.calculateDimensions();
```

**effect**: bb height 즉시 변경. 다음 frame 부터 일관.

**risk**: vanilla 가 다음 tick pose 갱신 시 다시 변경 가능. 충돌 가능.

### 옵션 C — ICC dim 자체 수정 (= 큰 refactor)

self side 의 ICC dim 매핑 변경: ICC 동안 entity.y -1m down 안 함 + bb 0.5m 사용. EXIT 시 entity.y 변화 없음 (= +1m up 매핑 안 함).

**effect**: remote 측 svY 매끄러움. setPos 안 함.

**risk**: 큰 변경. self side 의 인게임 동작 영향.

### 옵션 D — 사용자 수용

"약간 덜컹" 수용. 다른 시나리오 진행.

### 추천 순서

1. **B (pose 강제) + I (calculateDimensions)** 검증 → bb timing fix.
2. **A (부분 동기화)** 시도 → teleport 작게.
3. **C (refactor)** 마지막 수단.

## 6. 코드 위치 + 변수 정리

### 상수 (SmartMovingClient.java)

```java
public static final double ICC_EXIT_REMOTE_Y_OFFSET = 1.0;   // 그랩
public static final double ICC_EXIT_LADDER_Y_OFFSET = 0.84;
public static final double ICC_EXIT_LADDER_Y_THRESH = 0.5;
public static final double ICC_EXIT_GRAB_Y_THRESH = 0.5;
```

### packet handler (SmartMovingClient.java L102~L162)

```java
if (wasIcc && !target.isClimbCrawling
        && entity instanceof AbstractClientPlayerEntity remote
        && !(entity instanceof ClientPlayerEntity)) {
    MixinLivingEntityAccessor acc = (MixinLivingEntityAccessor) remote;
    boolean isLadderType = remote.isClimbing();
    double offset = isLadderType ? ICC_EXIT_LADDER_Y_OFFSET : ICC_EXIT_REMOTE_Y_OFFSET;
    double newY = remote.getY() + offset;
    remote.setPosition(remote.getX(), newY, remote.getZ());
    remote.lastRenderY = newY;  // v25.7.10
    remote.prevY = newY;         // v25.7.10
    acc.sm_setServerY(newY);
    target.smIccExitWatchTicks = 10;
    target.smIccExitOurSetY = newY;
    target.smIccExitIsLadder = isLadderType;
    target.smIccExitYThresh = isLadderType ? ICC_EXIT_LADDER_Y_THRESH : ICC_EXIT_GRAB_Y_THRESH;
    // 그랩만 setBti(0) (= 단방향 lerp 차단). ladder 는 X/Z lerp 자유.
    // 잠깐 v25.7.10 코드 확인 필요 (= 분기 vs 통합).
}
```

★ **확인 필요**: v25.7.10 시점에 setBti(0) 분기 유지인지 통합인지. 아래 watchfix 와 일관 검증 필요.

### watchfix (MixinPlayerEntityClient.java L402~L420)

```java
if (sm.smIccExitWatchTicks > 0) {
    sm.smIccExitWatchTicks--;
    MixinLivingEntityAccessor acc = (MixinLivingEntityAccessor) remote;
    double svY = acc.sm_getServerY();
    if (svY != sm.smIccExitOurSetY) {
        if (svY < sm.smIccExitOurSetY - sm.smIccExitYThresh) {
            // 진짜 풀림 → setPos + setBti(0) + return.
            remote.setPosition(remote.getX(), svY, remote.getZ());
            acc.sm_setBodyTrackingIncrements(0);
            sm.smIccExitWatchTicks = 0;
            return;
        }
        // 미세 gap → svY=ourSetY reset.
        acc.sm_setServerY(sm.smIccExitOurSetY);
    }
}
```

### state field (SmartMovingClientState.java)

```java
public int smIccExitWatchTicks = 0;
public double smIccExitLastSvY = 0.0;
public int smIccExitBroadcastCount = 0;
public double smIccExitOurSetY = 0.0;
public double smIccExitFirstBroadcastSvY = 0.0;
public boolean smIccExitIsLadder = false;
public double smIccExitYThresh = 0.0;
```

## 7. 메모리 + 관련 패턴

- `project_bug7_remote_icc_exit_complete.md`: 정착 (= v25.3, 사실상 outdated).
- `feedback_packet_lambda_immediate_fix.md`: SM packet lambda 안 즉시 setPos 패턴.
- `feedback_remote_motion_correction.md`: AbstractClientPlayerEntity 미러 패턴.
- `feedback_server_reconcile_box_sync.md`: server reconcile 박스 동기화.
- `feedback_smSmall_dim_omission.md`: smSmall dim 분기 SM state 누락.
- `project_crawl_release_camera_jump_fix.md`: lastRenderY 동기화 패턴 (= 같은 fix 메커니즘).
- `feedback_dynamic_pose_bb_check.md`: vanilla 1.21.1 동적 pose bb 검사.
- `feedback_orphan_swim_pose_lag.md`: pose 갱신 lag 패턴.

## 8. 다음 세션 시작 시 검증 사항

### 코드 상태 확인

```
git diff src/client/java/choco/ratel/smartmoving/client/SmartMovingClient.java
git diff src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityClient.java
git diff src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java
```

### dump 파일

`docs/log_temp.txt` (= 사용자 인게임 로그 영구 파일, .gitignore 등록).
- 최신 = v25.7.10 결과 (= 4 EXIT 시점).
- L2665~ 1차 ladder.
- L3294~ 2차 그랩.
- L3670~ 3차 ladder.
- L4002~ 4차 그랩.

### 인게임 검증 시나리오

1. multi 환경 + remote rudals.
2. ladder/그랩 climbing → ICC EXIT.
3. self 측에서 remote 시각 (= 3인칭 또는 다른 player 시점) "덜컹" 인지 검증.
4. 옵션 B 적용 후 비교.

### 우선 적용 옵션

**옵션 B (= pose 강제 + calculateDimensions)** 부터:

```java
// packet handler 안 (setPos 후):
if (target.isCrawling) {
    remote.setPose(net.minecraft.entity.EntityPose.SWIMMING);
}
remote.calculateDimensions();  // 이미 L100 에 있음 (= processStatePacket 후).
```

**기대 효과**: bb height 즉시 0.6m 적용 → render Y 와 bb 일관.

## 9. 핵심 dump 위치 요약 (v25.7.10 검증 dump)

| EXIT | BEFORE | AFTER | 직후 frame |
|------|--------|-------|-----------|
| 1차 ladder (11:41:57) | L2665 y=76.161 | L2667 y=77.001 | L2668 render=77.001 lrY=77.001 prevY=77.001 |
| 2차 그랩 (11:42:02) | L3294 y=78.154 | L3296 y=79.154 | L3297 render=79.154 lrY=79.154 prevY=79.154 |
| 3차 ladder (11:42:11) | L3670 y=76.164 | L3672 y=77.004 | L3673 render=77.004 lrY=77.004 prevY=77.004 |
| 4차 그랩 (11:42:15) | L4002 y=78.154 | L4004 y=79.154 | (TBD frame 단위 검증) |

★ **모두 1 frame instant teleport 작동 확인**.

## 10. fix 시점별 git status snapshot

```
M src/client/java/choco/ratel/smartmoving/client/SmartMovingClient.java
M src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java
M src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityClient.java
M src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityAccessor.java  (sm_setServerX/Z 제거)
?? src/client/java/choco/ratel/smartmoving/mixin/client/MixinEntityClient.java  (DBG-7 dump)
?? src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityRenderer.java  (DBG-7 dump)
?? src/main/java/choco/ratel/smartmoving/mixin/MixinPlayerEntity.java  (DBG-7 dump)
?? src/main/java/choco/ratel/smartmoving/mixin/server/MixinServerPlayNetworkHandler.java  (DBG-7 dump)
?? src/main/java/choco/ratel/smartmoving/server/SmartMovingServer.java  (이미 commit, dirty 검사)
```

★ **DBG-7 dump 코드는 root cause 분석 후 제거 가능** (= [TEMP DBG-7] 주석으로 마킹).

## 11. 사용자 명시 결정 history (참고)

| 발언 | 의미 |
|------|------|
| "그랩류 완벽" | v25.6.6 그랩 동작 보존 의도. |
| "ladder 약간 뒤로 끌림" | ladder 만 추가 fix 필요. |
| "당연히 다르게 해야지" | ladder/그랩 분기 유지 의도. |
| "그랩 다시 수정해봐 ladder 랑 같은 로직으로, 변수는 따로" | logic 통일 + 변수 분리. |
| "둘다 해봐" (= lastRenderY 동기화) | ladder + 그랩 모두 적용. |
| "약간 덜컹" | v25.7.10 잔존 BUG. |

## 12. 기술 참조

### vanilla render lerp 식

`EntityRenderDispatcher.render` 안:
```java
double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
```

`Entity.tick` 마지막에 `lastRenderX/Y/Z = getX/Y/Z` (= 매 tick 의 시작값 저장).

`prevX/Y/Z` 도 같은 시점 저장 (= 다른 lerp 용도).

### vanilla LivingEntity.tickMovement 의 lerpPos

```java
if (this.bodyTrackingIncrements > 0) {
    double dx = (this.serverX - this.getX()) / this.bodyTrackingIncrements;
    double dy = (this.serverY - this.getY()) / this.bodyTrackingIncrements;
    double dz = (this.serverZ - this.getZ()) / this.bodyTrackingIncrements;
    this.setPosition(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
    this.bodyTrackingIncrements--;
}
```

### EntityPositionS2CPacket → setBti

vanilla packet handler 가 `lerpPosAndRotation(3, x, y, z, yaw, pitch)` 호출 → `serverX/Y/Z` set + `bodyTrackingIncrements = 3`.

### partialTicks (= tickDelta)

minecraft tick = 50ms (20 TPS). frame = 16ms (= 60 fps). partialTicks = 0~1 사이 (= tick 진행 비율).

매 tick 의 [B-TICK] dump = entity.x/y/z snapshot. partialTicks 진행 중 [B-RENDER] dump = render 결과.

## 13. 종료 요약

**현재 (v25.7.10)**:
- ladder/그랩 변수 분리 (= offset, threshold).
- watchfix threshold 적용 (= 미세 svY gap 흡수, 진짜 풀림만 setPos).
- setBti(0) skip (= ladder 만 또는 통합 — 코드 확인 필요).
- lastRenderY/prevY 동기화 (= 1 frame instant teleport).

**잔존**:
- 사용자 보고 "약간 덜컹".
- root: 1 frame +0.85/+1.0m instant teleport 자체 (1순위) 또는 pose timing (2순위) 또는 X/Z lag catch-up (3순위).

**다음 우선**:
- 옵션 B (= pose 강제 + calculateDimensions) 검증.
- 또는 옵션 A (= 부분 동기화) 시도.
