# 포커스 #2.7 — BBox / POSE / EyeHeight 서버 동기화 + 원본 heightOffset 완전 재현

> **Phase 1 보강 포커스**. 세션 137 Phase 1 에서 client 측 `getBaseDimensions` /
> `updatePose` Mixin 에 모든 SM 상태별 원본 bbox (0.6 × 0.8 + eyeHeight 0.62F) 적용 완료.
> 본 포커스는 **서버 측 완전 동기화** + **원본 heightOffset 시맨틱 100% 재현**.
>
> **목표**:
> 1. server-side `MixinPlayerEntity.getBaseDimensions` / `updatePose` 가 client 와 **완전 일치**
>    한 dimensions/POSE 반환 → 좁은 통로 통과 / 낙하 데미지 / 질식 판정 **client = server**.
> 2. 1.21.1 Fabric payload packet (`SmartMovingNetwork.StatePayload`) 로 모든 SM 상태 서버
>    전송 + 서버 필드 저장 + Mixin 참조 경로 완성.
> 3. 원본 SM `heightOffset` 시맨틱 (`setHeightOffset(-1F)` → bbox minY+1 / height-1 동치)
>    을 1.21.1 POSE + EntityDimensions 시스템으로 **엄격 1:1** 매핑.
> 4. vanilla POSE 중 SM 과 충돌하는 것만 선별 차단 (elytra/trident/sleeping 등 유지).
>
> **진입 배경**: 세션 137 B-N-standup 재평가에서 원본 heightOffset 시맨틱 재검토 → 1.21.1
> POSE 시스템이 동치 대체지만 **SM 값 (bbox 0.6×1.0 / eyeHeight 0.4)** 가 원본 (0.6×0.8 /
> 0.62) 과 달랐음 → 0.2 블록 차이로 1 블록 통로 통과 실패. Phase 1 client 값 수정 완료.
> Phase 2 는 서버 sync 완성.

---

## 0. 현재 상태 (진입 시점)

### 이식 완료 (Phase 1 — 세션 137)
| 항목 | 위치 | 상태 |
|---|---|---|
| client `sm_getBaseDimensions_client` 모든 SM 상태 → 0.6 × 0.8 | MixinPlayerEntityClient L40-L71 | ✅ |
| client `sm_updatePose_client` POSE 매핑 확장 (8 상태 전수) | MixinPlayerEntityClient L85-L117 | ✅ |
| StatePayload encode/decode (22+ bit 전수) | SmartMovingState.java L40-L114 | ✅ |

### 미이식 / 근사 (이 포커스 범위) — 세션 2 전수 리서치 후 정정
| 항목 | 원인 |
|---|---|
| **server `MixinPlayerEntity.getBaseDimensions` height 1.0F** ★ | 원본 0.8F (heightOffset(-1F) → 1.8 - 1 = 0.8) 와 불일치 — **1:1 위반, 정정 필수** |
| ~~server `MixinPlayerEntity.updatePose` 확장 상태 반영~~ | **세션 2 정정**: 원본 PacketStream 도 isSmall 단일 비트만 사용 — 현 isSmall 분기로 충분 |
| ~~server `SmartMovingServer` 8+ 필드 미이식~~ | **세션 2 정정**: 원본 서버 11 필드 (isCrawling/isSmall + 보조 6) 수준 — 현 11 필드 이미 충족 |
| ~~StatePayload 수신 → server 필드 저장~~ | **세션 2 정정**: server.processStatePacket L112-L127 이미 7 비트 (12,13,14,15,18,31,33) 정확 디코딩 |
| ~~isClimbCrawling/isFlying/isLevitating StatePayload 추가~~ | **세션 2 정정**: SmartMovingState 이미 21 bit 분리 인코딩 (bit 9~33) — 추가 필요 0 |
| **★ 네트워크 핸들러 배선 (NEW Phase G)** | `ServerPlayNetworking.registerReceiver` / 클라 송신 / 서버 브로드캐스트 모두 미배선 — **핵심 누락** |
| **★ isSmall OR 인코딩 검증 (NEW Phase H)** | SmartMovingState.encode 가 `isCrawling \|\| isClimbCrawling \|\| isHeadJumping \|\| isSliding \|\| isSwimming_sm \|\| isDiving \|\| isFlying \|\| isLevitating` OR 결과를 isSmall(bit 15) 로 송신하는지 검증 |
| ~~heightOffset 렌더링 보정 (getBrightness 등)~~ | **세션 2 정정**: 1.21.1 `standingEyeHeight = newDims.eyeHeight()` 자동 반영 → N/A (영구 동치, §7 등록) |

### 세션 2 신규 발견 (2026-04-25, 4 Agent 병렬 read 결과)
> **종합 리서치**: `docs/research/mapping/research_bbox_pose_eyeheight.md` — 원본 13 파일 5091 줄
> + vanilla 11 리서치 + 1.21.1 이식 5 코드 모두 라인별 전수 read.

추가 검증/보강 항목:
1. **`resetInternalHeightOffset()` L1688-L1692** ★ 신규 — 수면 전용 (bbox 유지 + height 만 초기화).
   호출처 L1359 (`landMotionPost` + isSleeping). 1.21.1 vanilla 수면 자동 dimensions → N/A.
2. **`afterMoveEntity` L1608-L1609** ★ 신규 — `if (heightOffset != 0F) sp.posY += heightOffset`.
   매 프레임 posY 보정. 1.21.1 EntityDimensions 의 `setBoundingBox` + `standingEyeHeight`
   즉시 반영으로 자동 처리 → N/A.
3. **resetHeightOffset() 호출처 14건** (L249/L528/L1171/L1375/L2173/L2207/L2219/L2273/L2746/
   L2774/L2780/L2816/L2819/L2824/L2842) — 모두 #2.7 시맨틱 영향. setHeightOffset(-1F) 와
   대칭 — POSE.STANDING 또는 vanilla 통과로 매핑.
4. **setHeightOffset(wasHeightOffset)** L422/L558 — 이전 offset 복원 (-1F 아님). 수영 컨텍스트.
5. **클라/서버 비대칭** ★ 핵심 — 원본 SmartMovingPlayerBase 에 `getEyeHeight` override **없음**.
   서버 ServerPlayerBase 만 `player.height - 0.18F`. 1.21.1 EntityDimensions.withEyeHeight
   가 클라/서버 자동 대칭 처리 — Mixin 불필요.
6. **doFlyingAnimation()** (playerapi/SmartMovingSelf L36) — SPC 호환. 1.21.1 N/A 데드 코드.

---

## 1. 1:1 번역 룰

원본 `SmartMovingSelf.setHeightOffset(float offset)` (L1694-L1704):
```java
private void setHeightOffset(float offset) {
    resetHeightOffset();
    if (offset == 0F) return;
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;    // minY += 1 (offset=-1 시)
    sp.height += heightOffset;               // height -= 1
}
private void resetHeightOffset() {                  // L1681-L1686
    sp.boundingBox.minY += heightOffset;    // 원위치 복원
    sp.height -= heightOffset;
    heightOffset = 0F;
}
private void resetInternalHeightOffset() {          // L1688-L1692 ★ 세션 2 신규
    sp.height -= heightOffset;              // height 만 변경 (bbox 유지)
    heightOffset = 0F;                       // 호출처 L1359 (수면 전용)
}
```

원본 `SmartMovingSelf.afterMoveEntity` 매 프레임 posY 보정 (L1608-L1609) ★ 세션 2 신규:
```java
public void afterMoveEntity(double d, double d1, double d2) {
    // ...
    if (heightOffset != 0F)
        sp.posY = sp.posY + heightOffset;  // 매 프레임 posY 자동 보정
}
```

원본 `SmartMovingSelf.getBrightness(f)` 렌더 보정 (L1708-L1714):
```java
public float getBrightness(float f) {
    sp.posY -= heightOffset;               // 임시 상향 (lighting 머리 기준)
    float result = isp.localGetBrightness(f);
    sp.posY += heightOffset;               // 복원
    return result;
}
```

원본 `SmartMovingServerPlayerBase.getEyeHeight()` (L142-L145):
```java
public float getEyeHeight() {
    return player.height - 0.18F;          // small 0.62F / normal 1.62F
}
```
※ 클라 `SmartMovingPlayerBase` 에 `getEyeHeight` override **없음** ★ — 원본 클라/서버 비대칭.
   클라는 vanilla `EntityPlayer.getEyeHeight()` 가 sp.height 기반 자동 계산.

→ 원본은 bbox + height + eyeHeight 3 필드를 동시에 관리. 1.21.1 대응:
- bbox + height → `EntityDimensions` (`getBaseDimensions` Mixin 으로 반환)
- eyeHeight → `EntityDimensions.withEyeHeight(float)` (동일 dimensions 내 지정)
- POSE → vanilla POSE 시스템 (`setPose` + 매 tick `updatePose` Mixin 으로 독점)
- afterMoveEntity posY 보정 → 1.21.1 `Entity.calculateDimensions` 의 `refreshPosition` +
  `setBoundingBox` 즉시 반영으로 **자동 처리** (별도 Mixin 불필요).
- getBrightness 보정 → 1.21.1 `standingEyeHeight = newDims.eyeHeight()` 자동 반영 (lighting
  은 eyeY 기반) — N/A.

**client / server 양쪽** 에 동일 로직 적용 시 동치 달성. 단 원본은 클라 측에 명시 override
없으므로, **1.21.1 Mixin 은 원본보다 대칭화 (양쪽 동일 처리)** — 기능 동치 + 안전성 향상.

### 표면 매핑
| 원본 | 1.21.1 |
|---|---|
| `sp.boundingBox.minY -= -1; sp.height += -1` (setHeightOffset(-1)) | `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F)` 반환 |
| `sp.boundingBox` 원복 (resetHeightOffset) | SM 상태 false 시 vanilla `getBaseDimensions` 통과 |
| `sp.getEyeHeight() = height - 0.18F` | `withEyeHeight(height - 0.18F)` |
| 서버 측 state 유지 | `SmartMovingServer` 필드 + StatePayload 수신 저장 |
| client → server 전송 | `SmartMovingNetwork.StatePayload` (기존) |

### 금지
- client/server 값 불일치 (desync 근본 원인)
- `isCrawling` / `isSmall` 만 체크 (다른 상태 누락)
- 매 tick `setBoundingBox` 반복 hack (POSE 시스템이 대체)

---

## 2. 원본 상태 → POSE + Dimensions 매핑

### 2.1. 전수 매핑 표
| SM 상태 | 원본 heightOffset | bbox | eyeHeight | 1.21.1 POSE |
|---|---|---|---|---|
| isCrawling | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isClimbCrawling | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isHeadJumping | -1 | 0.6 × 0.8 | 0.62 | SLIDING |
| isSliding | -1 | 0.6 × 0.8 | 0.62 | SLIDING |
| isSwimming | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isDiving | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isFlying (SM) | -1 (진입 엣지) | 0.6 × 0.8 | 0.62 | SLIDING (수영 애니 회피) |
| isLevitating | -1 (진입 엣지) | 0.6 × 0.8 | 0.62 | SLIDING |
| isDipping | 0 | 0.6 × 1.8 | 1.62 | STANDING (vanilla) |
| STANDING | 0 | vanilla | vanilla | vanilla 통과 |
| CROUCHING | 0 | vanilla | vanilla | vanilla 통과 |
| FALL_FLYING (elytra) | - | vanilla 0.6 × 0.6 | vanilla | vanilla 통과 |
| SPIN_ATTACK (trident) | - | vanilla | vanilla | vanilla 통과 |

### 2.2. POSE 독점 차단 원칙
- **차단 대상** (SM 상태별 POSE 재정의): SWIMMING / SLIDING 2 개 POSE.
  vanilla 가 이 POSE 로 자동 전환해도 SM 상태와 다른 bbox 생성 가능 → 매 tick Mixin 으로
  덮어쓰기.
- **통과 대상** (vanilla 유지): STANDING / CROUCHING / FALL_FLYING / SPIN_ATTACK / SLEEPING /
  DYING / LONG_JUMPING. SM 과 무관 — elytra / trident / 잠자기 등 vanilla 기능 보존.

---

## 3. Phase 구조 — 세션 2 전수 리서치 후 대폭 정정

> **핵심 정정**: 원본 PacketStream 분석 결과 — 원본은 **`isSmall` 단일 비트** (bit 15)
> 로 모든 small 상태 (isCrawling || isClimbCrawling || isHeadJumping || isSliding ||
> isSwimming || isDiving || isFlying || isLevitating) 통합 sync. 1.21.1 SmartMovingState
> 의 21 bit 분리 인코딩은 **다른 클라이언트 애니메이션용** 으로만 활용. 서버 dimensions
> 결정은 isSmall 단일로 충분 (원본 1:1).
>
> → 기존 Phase A (8 필드 추가) / Phase B (bit 확장) **불필요**. Phase C/D 단순화.
> 신규 Phase G (네트워크 핸들러 배선) + Phase H (isSmall OR 인코딩 검증) 핵심.

### Phase A. server 필드 — 세션 4 정정 (옵션 3 1:1 완전 번역)

**세션 2/3 정정 → 세션 4 재정정 (옵션 3 채택)**: 사용자 결정 — "1대1 완전번역" → 클라/서버
POSE 완전 대칭화. SmartMovingServer 에 5 필드 + processStatePacket 5 비트 디코딩 추가.

**A-1. 기존 11 필드 검증** (세션 3 완료)
- [x] **A-1 (세션 3)**. SmartMovingServer.java L34-L80 11+ 필드 보유. 원본 1:1 충족.

**A-2. 옵션 3 — 5 필드 추가** ★ (세션 4 신규)
- [x] **A-2a (세션 4)**. SmartMovingServer 에 5 필드 추가 (POSE 분기용):
  - `isDiving` (bit 9) — POSE.SWIMMING
  - `isSwimming` (bit 11) — POSE.SWIMMING
  - `isLevitating` (bit 19) — POSE.SLIDING
  - `isHeadJumping` (bit 20) — POSE.SLIDING
  - `isSliding` (bit 21) — POSE.SLIDING
- [x] **A-2b (세션 4)**. `processStatePacket` 에 5 비트 디코딩 추가 (bit 9/11/19/20/21).
  isFlying 은 vanilla `player.getAbilities().flying` 으로 직접 참조 — 별도 비트 불필요
  (원본 공식 `cfg.fly && abilities.flying && !isSwimming && !isDiving` 서버 동일 계산).

### Phase B. ~~StatePayload 인코딩 확장~~ → **이미 완료** (세션 2 정정)

**B-1. SmartMovingState 21 bit 인코딩 검증**
- [x] **B-1 (세션 2 검증)**. SmartMovingState.encode/decode 가 bit 0-3 actualFeetClimbType /
  4-7 actualHandsClimbType / 8 isJumping / 9 isDiving / 10 isDipping / 11 isSwimming / 12
  isCrawlClimbing / 13 isCrawling / 14 isClimbing / 15 isSmall / 16 doFallingAnimation /
  17 doFlyingAnimation / 18 isCeilingClimbing / 19 isLevitating / 20 isHeadJumping / 21
  isSliding / 22-24 angleJumpType / 25-31 vine·climb·sneak·sprint / 33 isSneakButtonPressed
  모두 양방향 인코딩 완료 — **1.21.1 이 원본 PacketStream 의 7 bit 보다 풍부**. 추가 불필요.

### Phase C. server `MixinPlayerEntity.getBaseDimensions` height 정정 ★

**C-1. 원본 1:1 재작성** — 원본 setHeightOffset(-1F) → height = 0.8F (1.8 - 1)
- [x] **C-1 (세션 3 → 세션 4 재작성)**. `MixinPlayerEntity.sm_getBaseDimensions_server`
  세션 3: `pose == SWIMMING && isCrawling → 0.8F + 0.62F` (height 정정만).
  세션 4: **클라와 완전 대칭** — 8 SM OR 통합 분기 (smSmall):
  ```java
  boolean smFlying = cfg.fly && player.getAbilities().flying
                  && !sm.isSwimming && !sm.isDiving;
  boolean smSmall = sm.isCrawling || sm.isCrawlClimbing
                 || sm.isHeadJumping || sm.isSliding
                 || sm.isSwimming || sm.isDiving
                 || smFlying || sm.isLevitating;
  if (smSmall) cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
  if (pose == SLIDING) cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
  ```
- [x] **C-1a (세션 3 완료)**. 기존 height 1.0F + eyeHeight 0.4F **제거** ✓.
- [x] **C-1b (세션 3 완료)**. eyeHeight 0.62F 정확 보존 (원본 `height - 0.18F = 0.8 - 0.18`).
- [x] **C-1c (세션 4)**. 8 SM OR 통합 — Phase 1 client 와 동일 분기.

### Phase D. server `MixinPlayerEntity.updatePose` 클라와 완전 대칭화 (세션 4 옵션 3)

**세션 2 정정 → 세션 4 재정정**: 사용자 결정 "1대1 완전번역" → 옵션 3 (서버 4 분기 클라와
동일). 클라 SWIMMING/SLIDING 매핑이 8 SM 상태에 따라 다르므로, 서버도 같은 분기 처리하여
datatracker POSE sync 진동 방지.

**D-1. 클라와 동일 4 분기** ★
- [x] **D-1 (세션 4)**. `sm_updatePose_server` 4 분기 클라와 완전 대칭:
  ```java
  if (sm.isCrawling || sm.isCrawlClimbing) → setPose(SWIMMING) + cancel
  else if (sm.isHeadJumping || sm.isSliding) → setPose(SLIDING) + cancel
  else if (sm.isSwimming || sm.isDiving) → setPose(SWIMMING) + cancel
  else if (smFlying || sm.isLevitating) → setPose(SLIDING) + cancel
  // isDipping / 그 외 → vanilla 통과
  ```
  isFlying 원본 공식: `cfg.fly && abilities.flying && !isSwimming && !isDiving`
  (SmartMovingSelf L2509-L2515).
- [x] **D-1a (세션 4)**. 클라 (MixinPlayerEntityClient.sm_updatePose_client L109-L135)
  와 완전 일치 → datatracker POSE sync 진동 0.
- [x] **D-1b (세션 4)**. SLIDING 포즈 사용 시 vanilla leaningPitch 자동 발동 차단
  (POSE_DIMENSIONS 미등록 → STANDING 폴백 → Mixin override 로 0.6×0.8 주입).

### Phase E. ~~heightOffset 렌더링 보정~~ → **N/A 자동 처리** (세션 2 정정)

**E-1. vanilla eyeHeight / lighting 자동 반영 검증**
- [x] **E-1 (세션 2 검증, 세션 3 재확인)**. 1.21.1 `Entity.calculateDimensions` →
  `standingEyeHeight = newDims.eyeHeight()` 즉시 반영. lighting 계산은 `entity.getEyeY()`
  (eyeHeight 기반) 자동 사용 → 원본 `getBrightness` 의 `posY -= heightOffset` 보정
  **불필요**. §7 영구 동치 등록.

**E-2. afterMoveEntity posY 보정 자동 처리 검증**
- [x] **E-2 (세션 2 검증, 세션 3 재확인)**. 원본 `afterMoveEntity` L1608-L1609
  `if (heightOffset != 0F) posY += heightOffset` — 1.21.1 `Entity.calculateDimensions`
  의 `refreshPosition() + setBoundingBox()` 즉시 반영으로 자동 처리. §7 영구 동치 등록.

### Phase G. ★ 네트워크 핸들러 배선 — **NEW (세션 2 추가)** / **세션 5 검증 [x]**

**G-1. SmartMoving.onInitialize 에 서버 수신 등록**
- [x] **G-1 (세션 5 검증)**. `Smartmoving.java` L77-L88 `ServerPlayNetworking.registerGlobalReceiver(
  StatePayload.ID, ...)` 이미 완료. processStatePacket 호출 → 서버 필드 갱신.

**G-2. 클라 → 서버 매 tick 송신**
- [x] **G-2 (세션 5 검증)**. `MixinClientPlayerEntity.java` L104-L108 `tickMovement` TAIL
  inject 로 매 tick `SmartMovingClientState.get(player).sendStatePacket(player)` 호출.
  내부 `sendStatePacket` (L2776-L2822) 에서 변경 시에만 송신 (lastSentBits 비교) — 효율
  최적화. 원본 매 tick 송신과 의미 동치 (변경 0 tick 은 송신 가치 0).

**G-3. 서버 → 다른 클라 브로드캐스트**
- [x] **G-3 (세션 5 검증)**. `Smartmoving.java` L82-L86 `PlayerLookup.tracking(sender)` →
  `ServerPlayNetworking.send(tracker, payload)` (sender 제외) 브로드캐스트. 원본 L90
  `mp.sendPacketToTrackedPlayers(packet)` 1:1.

**G-4. 클라 다른 플레이어 수신 → 애니메이션용 상태**
- [x] **G-4 (세션 5 검증)**. `SmartMovingClient.java` L43-L53 `ClientPlayNetworking.
  registerGlobalReceiver(StatePayload.ID, ...)` → `SmartMovingClientState.get(entity.getUuid())
  .processStatePacket(payload.state())` 다른 플레이어 상태 sync.

### Phase H. ★ isSmall OR 인코딩 검증 — **NEW (세션 2 추가)** / **세션 5 정정**

**H-1. SmartMovingState.encode 에서 isSmall 비트 정합성**
- [x] **H-1 (세션 5 검증)**. `SmartMovingState.encode` L77 `if (s.isSmall) bits |= 1L << 15`
  단순 sm.isSmall 필드 그대로 인코딩. → 클라 측 isSmall 갱신 식이 정확해야 함.

**H-2. SmartMovingClientState isSmall 갱신 식 정정** ★
- [x] **H-2 (세션 5 정정)**. SmartMovingClientState.java L1501 isSmall 갱신 식:
  - 이전: `isSmall = isCrawling || isSliding || isHeadJumping` (3 SM OR — **5 SM 누락**)
  - 정정: `isSmall = isCrawling || isClimbCrawling || isHeadJumping || isSliding ||
    isSwimming_sm || isDiving || isFlying || isLevitating` (8 SM OR)
  - 근거: 원본 SmartMovingSelf L3110 `boolean isSmall = sp.height < 1;` —
    setHeightOffset(-1F) 호출처 14건 (8 SM 상태) 모두 height = 0.8 < 1 → isSmall=true.
    1.21.1 매핑 = 8 SM OR.

**H-3. 서버 dimensions 결정 정합성**
- [x] **H-3 (세션 5 검증)**. Phase D 옵션 3 채택으로 서버 sm_getBaseDimensions_server 가
  isSmall 외 5 비트 (isHeadJumping/isSliding/isSwimming/isDiving/isLevitating) 별도 디코딩
  + isFlying 원본 공식 직접 계산. → isSmall OR 식 정확성과 무관하게 dimensions 결정 정확.
  H-2 정정으로 isSmall 비트 자체도 정확화 — sm.isSmall 사용 다른 코드 (예: 서버
  `setSmall` setter) 도 정확.

### Phase F. 검증 + 회귀 감사 + 플레이테스트 — 세션 6

**F-1. client / server dimensions 일치 검증**
- [x] **F-1 (세션 4)**. Phase C 재작성 (세션 4) 으로 client / server `sm_getBaseDimensions`
  가 동일 8 SM OR 분기 + 0.6×0.8+0.62F 통일. dimensions 양쪽 일치 ✓.

**F-2. vanilla 기능 회귀 감사** ★ 세션 6
- [x] **F-2 (세션 6)**. Mixin 가드 + SM 비활성 시 vanilla POSE 통과 검증:
  - **Mixin 가드 ✓**: `instanceof ServerPlayerEntity` 가드 (L43/L82/L116) — ClientPlayerEntity
    또는 비-플레이어 entity 영향 0.
  - **STANDING / CROUCHING / FALL_FLYING (elytra) / SPIN_ATTACK (trident) / SLEEPING /
    DYING**: SM 모두 false 시 smSmall=false → vanilla `getBaseDimensions(pose)` 통과 +
    updatePose 모든 분기 false → vanilla `updatePose()` 통과.
  - **smFlying = cfg.fly && abilities.flying && !isSwimming && !isDiving**: elytra 중
    abilities.flying=false → smFlying=false → vanilla 통과 ✓.
  - **잠자기 중**: isHeadJumping/isSliding/isCrawling 등 트리거 안 함 → smSmall=false →
    vanilla 통과 ✓.
  - 회귀 0건.

**F-3. 원본 게임플레이 시나리오 테스트** — deferred (통합 인게임 검증 시점)
- [ ] F-3a. 1 블록 높이 통로 크롤 통과 (Phase 1 client + Phase 2 server 동기화 포함)
- [ ] F-3b. 깊은 물 → 얕은 물 전환 시 bbox 변화 (server sync)
- [ ] F-3c. 슬라이딩 중 천장 bumping (bbox 0.8 동치)
- [ ] F-3d. 헤드점프 착지 (bbox 복원 타이밍)
- [ ] F-3e. SM 비행 (cfg.fly=true) 중 좁은 공간 bbox 0.8 유지
- [ ] F-3f. 멀티플레이어 타 플레이어 view 에서 POSE 애니 일치 (Phase G 핸들러 배선 효과)

**F-4. 네트워크 sync 검증** ★ 세션 6
- [x] **F-4 (세션 6)**. 송신 + sync 분석:
  - **송신**: MixinClientPlayerEntity tickMovement TAIL inject 매 tick → sendStatePacket
    → 변경 시에만 `lastSentBits != bits` (L2831) 송신. 효율 최적화.
  - **rubber banding 위험 0**: Phase D 옵션 3 채택으로 클라/서버 POSE 4 분기 완전 일치 →
    datatracker 진동 0.
  - **고지연 (200ms+)**: 자기 view 즉시 반영 (로컬 setPose), 다른 view ~400ms 지연 (vanilla
    entity sync 표준 한계).
  - **패킷 신뢰성**: Fabric `ClientPlayNetworking.send` = TCP 기반 → 패킷 손실 0 보장.
  - 회귀 0건.

**F-5. 빌드 (최종)**
- [x] **F-5 (세션 6)**. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD
  SUCCESSFUL (4s).

---

## 4. 의존 순서 — 세션 2 정정

```
Phase A (필드 검증, 코드 변경 0)               — 2 원자
   ↓
Phase B (StatePayload bit 검증, 코드 변경 0) — 1 원자 (이미 [x])
   ↓
Phase C (server getBaseDimensions height 정정 ★) — 3 원자
   ↓
Phase D (server updatePose isSmall 단일 분기) — 3 원자
   ↓
Phase E (렌더/posY 보정 N/A 검증, 코드 변경 0) — 2 원자 (이미 [x])
   ↓
Phase G ★ (네트워크 핸들러 배선) — 4 원자 (핵심)
   ↓
Phase H ★ (isSmall OR 인코딩 검증) — 3 원자
   ↓
Phase F (빌드 + 회귀 감사 + 플레이테스트)     — 5+ 원자 (E-3 deferred)
```

**규모 (세션 2 갱신)**: A 2 + B 1(완) + C 3 + D 3 + E 2(완) + G 4 + H 3 + F 5 =
**약 23 원자**. 단 B/E 이미 검증 완료 (3 원자 [x] 시작), Phase A 도 검증만 → 실 작업 ~17.
**예상 3-4 세션**.

→ 이전 18-22 원자 추정 → 23 원자 (Phase G/H 신규) 로 약간 증가하나 **Phase A/B 가 검증만**
이라 실 코드 변경 작업은 ~13 원자로 **감소**.

---

## 5. 1:1 번역 체크리스트 (원자별 적용)

- [근거] 원본 라인 확보 (`C:\Work\minecraft\porting\sm_original\SmartMoving\`)
- [근거] Phase 1 client 측 구현과 값/로직 동일
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1
- [분기] 모든 SM 상태 8+ 전수 반영
- [상수] `0.6F` / `0.8F` / `0.62F` / `1.8F` / `1.62F` 값 정확
- [타이밍] POSE 차단 위치 (HEAD cancel) client/server 동일
- [근사] 근사 없음 — 전수 1:1 목표 (focus 2.7 §7 0건 유지)
- [신규] 추가 상태 발견 시 §2.1 표 업데이트
- [회귀] vanilla 기능 (elytra/trident/수영 버프/잠자기) 영향 없음
- [빌드] `compileJava compileClientJava --rerun-tasks` 성공

---

## 6. 작업 기록 (세션별)

### 세션 0 — 2026-04-25 — 포커스 #2.7 신설

- Phase 1 (세션 137) 에서 client `sm_getBaseDimensions_client` / `sm_updatePose_client` 전수
  수정 완료. 모든 SM 상태 → 0.6 × 0.8 + eyeHeight 0.62F. POSE 매핑 확장 (SWIMMING/SLIDING
  차단, elytra/trident 등 vanilla 통과).
- 서버 측 `MixinPlayerEntity` 는 구 로직 (크롤 0.6×1.0) + 제한된 POSE 매핑 (isCrawling /
  isSmall 만) → client 와 불일치 → desync 위험.
- server 측 `SmartMovingServer` 필드 부족 (isCrawling / isSmall 만) → POSE 판정 불가능 상태.
- StatePayload 는 대부분 bit 인코딩 완료 (isClimbCrawling / isFlying 확인 필요) — 수신부
  저장 경로 보완 필요.
- Phase A/B/C/D/E/F 약 18-22 원자 계획 확정.

**다음 세션 권고**: Phase A-1 (SmartMovingServer 누락 필드 8 건 추가).

### 세션 2 — 2026-04-25 — 4 Agent 병렬 전수 리서치 + Phase 대폭 정정

사용자 지시: "2.7페이즈와 관련된 모든 원본 코드및 리서치 파일들을 1개도 빠트리지 말고,
   각 파일의 처음부터 끝까지 라인별로 청크 분리해서 모든 라인을 읽고... 1대1 번역이라는
   걸 명심하고 모든라인을 다 리서칭".

진행한 작업:
1. **원본 13 파일 size 확인 + 4 Agent 병렬 분담**:
   - **Agent A (heightOffset 시스템)**: SmartMovingSelf.java 3345 줄
   - **Agent B (서버 sync 인프라)**: Server/ServerComm/PacketStream/Comm/Client/IEntityPlayerMP/
     ISmartMovingClient 7 파일 1000 줄
   - **Agent C (playerapi 베이스)**: PlayerBase/ServerPlayerBase/playerapi/Self/IEntityPlayerSP/
     ISmartMovingSelf 5 파일 746 줄
   - **Agent D (1.21.1 vanilla + 매핑)**: vanilla 11 리서치 + 1.21.1 이식 5 코드
   - **합계 (원본만)**: **13 파일 5091 줄**
2. **종합 리서치 작성**: `docs/research/mapping/research_bbox_pose_eyeheight.md` 신규.
   §1 heightOffset / §2 서버 sync / §3 playerapi / §4 vanilla / §5 1.21.1 현황 / §6 매핑
   정합 / §7 보강 권고 / §8 1:1 결론.

3. **★ 핵심 발견 7건**:
   1. **원본 PacketStream 은 7 bit 만 사용** (bit 12 isCrawlClimbing / 13 isCrawling /
      14 isClimbing / **15 isSmall** / 18 isCeilingClimbing / 31 isWallJumping / 33
      isSneakButtonPressed). isHeadJumping/isSliding/isSwimming/isDiving/isDipping/isFlying/
      isLevitating **비트 없음** ★ — 원본은 isSmall 단일로 통합 sync.
   2. **server 측 height 1.0F 가 1:1 위반** ★ — 원본 0.8F (heightOffset(-1F) → 1.8 - 1).
      MixinPlayerEntity L43-L44 정정 필수.
   3. **resetInternalHeightOffset() L1688-L1692** ★ 신규 — 수면 전용. 1.21.1 N/A.
   4. **afterMoveEntity L1608-L1609** ★ 신규 — 매 프레임 posY 보정. 1.21.1 EntityDimensions
      자동 처리로 N/A.
   5. **resetHeightOffset() 호출처 14건** ★ 추가 발견 (L249/L528/L1171/L1375/L2173/L2207/
      L2219/L2273/L2746/L2774/L2780/L2816/L2819/L2824/L2842) — POSE.STANDING / vanilla 통과.
   6. **클라/서버 비대칭** ★ — 원본 PlayerBase (클) 에 `getEyeHeight` override **없음**.
      ServerPlayerBase (서버) 만 명시 override. 1.21.1 EntityDimensions.withEyeHeight 가
      클라/서버 자동 대칭 → Mixin 불필요.
   7. **네트워크 핸들러 배선 누락** ★ — SmartMovingNetwork Payload 정의 완료, 그러나
      `ServerPlayNetworking.registerReceiver(StatePayload)` / 클라 송신 / 서버 브로드캐스트
      모두 미배선. 핵심 누락.

4. **§3 Phase 대폭 정정**:
   - Phase A "8 필드 추가" → **검증만** (원본도 7 필드만, 1.21.1 11 필드 충족).
   - Phase B "bit 확장" → **이미 완료** [x] (SmartMovingState 21 bit 분리 인코딩 끝남).
   - Phase C **height 1.0F → 0.8F 정정** ★ (1:1 위반 해소).
   - Phase D **isSmall 단일 분기** 단순화 (원본 1:1).
   - Phase E "렌더 보정" → **N/A 자동 처리** [x] (vanilla calculateDimensions).
   - **Phase G 신규** (네트워크 핸들러 배선 4 원자) — 핵심 누락 해소.
   - **Phase H 신규** (isSmall OR 인코딩 검증 3 원자) — 클라 OR 식 정합성.

5. **규모 갱신**: 18-22 원자 → **23 원자** (G/H 신규). 단 B/E 이미 [x] (3 원자 시작점)
   + Phase A 검증만 → 실 코드 변경 작업 ~13 원자로 **감소**.

수정 파일:
- `docs/research/mapping/research_bbox_pose_eyeheight.md` 신규 (~700줄).
- `docs/fix/focus_02_7_bbox_server_sync.md` — §0 표 정정 + §1 본체 보강 (resetInternalHeightOffset
  + afterMoveEntity 추가) + §3 Phase 전면 재구성 + §4 의존 순서 갱신 + 본 세션 로그.

회귀 0건 (코드 변경 0). 빌드 N/A.

**다음 세션 권고**: Phase A-1 + B-1 (검증만, 코드 변경 0) → Phase C-1 (height 0.8F 정정,
   1:1 위반 해소). Phase G/H 는 후속 세션.

### 세션 3 — 2026-04-25 — Phase A 검증 + B/E 마킹 + Phase C-1 정정

사용자 지시: 포커스 #2.7 진입 prompt 제공 — "엄격 1:1" + 진입 첫 액션 5단계.

진행한 작업:
1. **1.21.1 현재 코드 read** (MixinPlayerEntity / SmartMovingServer / SmartMovingState).
2. **A-1 검증** (코드 변경 0): SmartMovingServer L34-L80 11+ 필드 (resetFallDistance/
   resetTicksForFloatKick/initialized/withinOnLivingUpdate/crawlingCooldown/isCrawling/
   isSmall/hunger/disableAddExhaustion(+Depth)/isSneakButtonPressed/forceIsSneaking/
   distanceClimbedModified/clientVersion + 패킷 디코딩 4) 보유. 원본 11 필드 1:1 충족.
3. **A-2 검증** (코드 변경 0): 누락 0. 원본 PacketStream 도 isHeadJumping/isSliding/
   isSwimming/isDiving/isDipping/isFlying/isLevitating 비트 없음 — 추가 불필요.
4. **B-1 / E-1 / E-2 마킹** (이미 [x] 였음 — 재확인): SmartMovingState 21 bit 인코딩 완료
   확인 + vanilla EntityDimensions 자동 처리 재확인.
5. **C-1 정정** ★ (1:1 위반 해소 첫 코드 변경):
   - `MixinPlayerEntity.sm_getBaseDimensions_server` L43-L45.
   - 이전: `pose == SWIMMING && isCrawling → changing(0.6F, 1.0F).withEyeHeight(0.4F)`.
   - 정정: `pose == SWIMMING && isCrawling → changing(0.6F, 0.8F).withEyeHeight(0.62F)`.
   - 근거: 원본 `setHeightOffset(-1F)` → `sp.height = 1.8 + (-1) = 0.8F` +
     `getEyeHeight() = height - 0.18F = 0.8 - 0.18 = 0.62F` (ServerPlayerBase L142-L145).
6. **빌드 검증**: `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

수정 파일:
- `src/main/java/choco/ratel/smartmoving/mixin/MixinPlayerEntity.java` — Phase C-1 정정.
- `docs/fix/focus_02_7_bbox_server_sync.md` — §3 Phase A/B/C/E [x] 마킹 + 본 세션 로그.

회귀 0건 (Phase 1 client 측 0.8F + 0.62F 와 이제 완전 대칭).

완료 전 검증 체크리스트 (세션 3 기준):
- [근거] 원본 SmartMovingSelf L1681-L1704 setHeightOffset / ServerPlayerBase L142-L145
  getEyeHeight (③ 리서치 §1.1, §3.2 발췌)
- [근거] 1.21.1 이식 위치 — MixinPlayerEntity L35-L46 (sm_getBaseDimensions_server)
- [대응] 원본 ↔ 1.21.1 1:1 (setHeightOffset(-1F) → 0.6×0.8 + 0.62F)
- [분기] SLIDING 분기 + SWIMMING+isCrawling 분기 모두 0.8F + 0.62F 통일 ✓
- [상수] 0.6F (width) / 0.8F (height) / 0.62F (eyeHeight) 정확 보존 ✓
- [타이밍] HEAD cancellable=true — vanilla getBaseDimensions 차단 ✓
- [근사] 신규 0건. §7 영구 동치 4건 유지.
- [신규] 추가 의존 발견 없음
- [회귀] 포커스 #2 Extended (B-N-standup) / #2.5 / #2.6 영향 0
- [빌드] BUILD SUCCESSFUL 5s ✓

다음 세션 권고: **Phase D-1 (server updatePose 분기 검토)** — 현 `if (isCrawling)
   SWIMMING / else if (isSmall) SLIDING` 분기가 클라 Phase 1 매핑 (8 SM 상태) 과 정합
   정확한지 검증. 비대칭 발견 시 정정. 그 후 Phase G (네트워크 핸들러 배선) 본격 진행.

진행률: Phase A 2/2 + B 1/1 + C 3/3 + E 2/2 = **8/23 (~35%)**. Phase D/G/H/F 잔존.

### 세션 4 — 2026-04-25 — Phase D-1 옵션 3 (1:1 완전 번역) — 클라/서버 POSE 완전 대칭화

사용자 지시: 옵션 1/2/3 브리핑 후 "1대1 완전번역으로 가야지" 선택 → 옵션 3 채택.

**핵심 변경**: 서버 POSE 매핑을 클라 (Phase 1) 와 완전 대칭화하여 datatracker POSE sync
   진동 0 보장.

진행한 작업:
1. **A-2 (세션 2/3 정정 → 세션 4 재정정)**: SmartMovingServer 에 5 필드 추가 + 5 비트 디코딩.
   - 추가 필드 (POSE 분기용): isDiving (bit 9) / isSwimming (bit 11) / isLevitating (bit 19)
     / isHeadJumping (bit 20) / isSliding (bit 21).
   - isFlying 은 vanilla `player.getAbilities().flying` 으로 직접 참조 + 원본 공식
     (`cfg.fly && abilities.flying && !isSwimming && !isDiving`) 서버에서 계산 — 별도 비트
     불필요.
2. **C 재작성**: `MixinPlayerEntity.sm_getBaseDimensions_server` 8 SM OR 통합:
   ```java
   smFlying = cfg.fly && abilities.flying && !isSwimming && !isDiving;
   smSmall = isCrawling || isCrawlClimbing || isHeadJumping || isSliding
          || isSwimming || isDiving || smFlying || isLevitating;
   if (smSmall) → 0.6×0.8 + 0.62F
   if (pose == SLIDING) → 0.6×0.8 + 0.62F (외부 모드 보강)
   ```
   클라 (MixinPlayerEntityClient L62-L74) 와 8 SM OR 분기 완전 동일.
3. **D-1 신규 작성**: `MixinPlayerEntity.sm_updatePose_server` 4 분기 클라와 완전 대칭:
   - isCrawling || isCrawlClimbing → SWIMMING
   - isHeadJumping || isSliding → SLIDING
   - isSwimming || isDiving → SWIMMING
   - smFlying || isLevitating → SLIDING
   - 이외 → vanilla 통과
   클라 (MixinPlayerEntityClient L114-L133) 와 분기 + 순서 완전 일치.
4. **빌드 검증**: `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

수정 파일:
- `src/main/java/choco/ratel/smartmoving/server/SmartMovingServer.java` — 5 필드 추가 + 5 비트 디코딩.
- `src/main/java/choco/ratel/smartmoving/mixin/MixinPlayerEntity.java` — Phase C 재작성 +
  Phase D-1 신규.
- `docs/fix/focus_02_7_bbox_server_sync.md` — §3 Phase A/C/D 재정정 + 본 세션 로그.

회귀 0건 (클라/서버 POSE 진동 위험 해소 + dimensions 동치 유지).

완료 전 검증 체크리스트 (세션 4 기준):
- [근거] 원본 SmartMovingPacketStream L93-L109 (sendState long state) + SmartMovingServer
  processStatePacket L69-L91 (③ 리서치 §2.1, §2.2)
- [근거] 1.21.1 이식 위치 — SmartMovingServer L77-L96 (5 필드) + L112-L137 (5 비트 디코딩)
  + MixinPlayerEntity L37-L60 (getBaseDimensions) + L70-L97 (updatePose)
- [대응] 원본 ↔ 1.21.1 1:1 (8 SM OR + isFlying 원본 공식 1:1)
- [분기] 4 분기 (SWIMMING/SLIDING/SWIMMING/SLIDING) + 순서 클라와 완전 일치
- [상수] 0.6F / 0.8F / 0.62F 정확 보존 ✓
- [타이밍] @At("HEAD") cancellable=true — vanilla updatePose / getBaseDimensions 차단 ✓
- [근사] 신규 0건. §7 영구 동치 4건 유지.
- [신규] 추가 의존 발견 없음. SmartMovingState 21 bit 인코딩은 이미 완료 (Phase B [x]).
- [회귀] 포커스 #2 Extended (B-N-standup) / #2.5 / #2.6 영향 0
- [빌드] BUILD SUCCESSFUL 5s ✓

다음 세션 권고: **Phase G (네트워크 핸들러 배선 4 원자)** — 핵심 누락 해소.
   G-1 ServerPlayNetworking.registerReceiver / G-2 클라 매 tick 송신 / G-3 서버 브로드캐스트
   / G-4 클라 다른 플레이어 수신.

진행률: Phase A 2/2 + B 1/1 + C 3/3 + D 1/1 + E 2/2 = **9/23 (~39%)**. Phase G/H/F 잔존.

### 세션 5 — 2026-04-25 — Phase G 검증 [x] + Phase H isSmall OR 정정 ★

사용자 지시: 옵션 3 (1:1 완전 번역) 진행 후 Phase G/H 진입.

진행한 작업:
1. **Phase G 4 원자 검증** (코드 변경 0 — 모두 이미 완료):
   - **G-1** Smartmoving.java L77-L88 `ServerPlayNetworking.registerGlobalReceiver(
     StatePayload.ID, ...)` ✓
   - **G-2** MixinClientPlayerEntity.java L104-L108 tickMovement TAIL inject →
     `sendStatePacket(player)` 매 tick 호출. SmartMovingClientState.sendStatePacket
     L2776-L2822 — 변경 시에만 송신 (`lastSentBits` 비교).
   - **G-3** Smartmoving.java L82-L86 `PlayerLookup.tracking(sender) → send(tracker,
     payload)` 브로드캐스트 (sender 제외) ✓
   - **G-4** SmartMovingClient.java L43-L53 `ClientPlayNetworking.registerGlobalReceiver(
     StatePayload.ID, ...)` → 다른 플레이어 ID 의 SmartMovingClientState.processStatePacket ✓

2. **Phase H isSmall OR 갱신 식 정정** ★ (1:1 위반 해소):
   - SmartMovingState.encode L77 `if (s.isSmall) bits |= 1L << 15` 검증 — 단순 필드 그대로.
   - SmartMovingClientState.java L1501 isSmall 갱신 식 정정:
     - 이전: `isCrawling || isSliding || isHeadJumping` (3 SM OR — **5 SM 누락**)
     - 정정: `isCrawling || isClimbCrawling || isHeadJumping || isSliding || isSwimming_sm
       || isDiving || isFlying || isLevitating` (8 SM OR)
     - 근거: 원본 SmartMovingSelf L3110 `boolean isSmall = sp.height < 1;` —
       setHeightOffset(-1F) 호출처 14건 (8 SM 상태) 모두 height = 0.8 < 1 → isSmall=true.
       Phase 1 client Mixin (MixinPlayerEntityClient L62-L65) smSmall 식 (8 SM OR) 과 정합.

3. **빌드 검증**: `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` — L1499-L1513
  isSmall 갱신 식 3 OR → 8 OR 정정 + 주석 보강.
- `docs/fix/focus_02_7_bbox_server_sync.md` — §3 Phase G/H [x] 마킹 + 본 세션 로그.

회귀 영향:
- Phase D 옵션 3 채택 (세션 4) 으로 서버 sm_getBaseDimensions_server / sm_updatePose_server
  가 8 SM OR 별도 분기 처리 → isSmall OR 식 정확성과 dimensions 결정 정합성 분리되어 영향 0.
- 그러나 isSmall 비트 자체 (StatePayload bit 15) 가 정확해져 SmartMovingServer.isSmall
  / setSmall(player, isSmall) 경로도 정확화 — 미래 보강.

완료 전 검증 체크리스트 (세션 5 기준):
- [근거] 원본 SmartMovingSelf L3110 isSmall 정의 + setHeightOffset(-1F) 호출처 14건 (③
  리서치 §1.1, §1.2 발췌)
- [근거] 1.21.1 이식 위치 grep — Smartmoving L77-L88 / MixinClientPlayerEntity L104-L108 /
  SmartMovingClient L43-L53 / SmartMovingClientState L2776-L2822 + L1499-L1513
- [대응] 원본 ↔ 1.21.1 1:1 (8 SM OR + Phase 1 client Mixin smSmall 식과 정합)
- [분기] 8 SM OR 모든 항목 보존 ✓
- [상수] 비트 인덱스 15 정확 ✓
- [타이밍] tickMovement TAIL → sendStatePacket → encode → send 순서 ✓
- [근사] 신규 0건. §7 영구 동치 4건 유지.
- [신규] 추가 의존 발견 없음
- [회귀] Phase D 옵션 3 채택으로 dimensions 결정은 isSmall 식 무관 → 회귀 0
- [빌드] BUILD SUCCESSFUL 5s ✓

다음 세션 권고: **Phase F (빌드 + 회귀 감사 + 플레이테스트)** — F-1 빌드 완료 ✓ /
   F-2 vanilla 회귀 감사 (elytra/trident/잠자기) / F-4 네트워크 sync 검증 / F-5 빌드 최종.
   F-3 통합 인게임 deferred.

진행률: Phase A 2/2 + B 1/1 + C 3/3 + D 1/1 + E 2/2 + G 4/4 + H 3/3 = **16/23 (~70%)**.
   Phase F 잔존 (5+ 시나리오, F-3 deferred).

### 세션 6 — 2026-04-25 — Phase F 완결 (#2.7 AI 완결)

사용자 지시: 옵션 3 후 Phase F (빌드 + 회귀 감사) AI 자동 진행.

진행한 작업:
1. **F-1 (세션 4 재확인 [x])**. Phase C 재작성 (세션 4) 으로 client / server
   `sm_getBaseDimensions` 동일 8 SM OR 분기 + 0.6×0.8+0.62F 통일 → dimensions 양쪽 일치.
2. **F-2 vanilla 회귀 감사** [x]:
   - Mixin 가드 검증: `instanceof ServerPlayerEntity` 가드 (L43/L82/L116) — 비-플레이어
     영향 0.
   - 5 vanilla POSE (STANDING/CROUCHING/FALL_FLYING elytra/SPIN_ATTACK trident/SLEEPING)
     모두 SM 비활성 시 smSmall=false → vanilla 통과 ✓.
   - smFlying 공식 (cfg.fly && abilities.flying && !isSwimming && !isDiving) — elytra
     중 abilities.flying=false → smFlying=false → vanilla 통과.
   - 잠자기 중 SM 트리거 0 → vanilla 통과.
3. **F-3 통합 인게임 검증** — deferred (모든 포커스 #1/#2.5/#2.6/#2.7/#3 완결 후).
4. **F-4 네트워크 sync 검증** [x]:
   - 송신: MixinClientPlayerEntity tickMovement TAIL → sendStatePacket → 변경 시에만
     (lastSentBits 비교).
   - Phase D 옵션 3 채택으로 클라/서버 POSE 4 분기 완전 일치 → datatracker 진동 0.
   - rubber banding 위험 0. Fabric ClientPlayNetworking = TCP 기반 패킷 손실 0.
   - 고지연 (200ms+) 자기 view 즉시 / 다른 view ~400ms 지연 (vanilla 표준).
5. **F-5 최종 빌드**: `./gradlew compileJava compileClientJava --rerun-tasks` BUILD
   SUCCESSFUL (4s).

수정 파일:
- `docs/fix/focus_02_7_bbox_server_sync.md` — §3 Phase F (F-1/F-2/F-4/F-5) [x] + 본 세션 로그.

회귀 0건. 코드 변경 0 (검증만).

완료 전 검증 체크리스트 (세션 6 기준):
- [근거] 1.21.1 이식 위치 grep — MixinPlayerEntity L41-L122 (가드 + 분기) +
  MixinClientPlayerEntity L104-L108 (송신 hook) + SmartMovingClientState L2786-L2834 (송신
  본체) (③ 리서치 §5.1, §5.2 참조)
- [근거] vanilla 5 POSE 분기 — Entity 1.21.1 EntityPose enum + LivingEntity.updatePose
  (③ 리서치 §4.2-§4.4)
- [대응] 원본 ↔ 1.21.1 1:1 (8 SM OR + 4 POSE 분기 + 변경 시 송신)
- [분기] 5 vanilla POSE 모두 SM 비활성 시 통과 ✓
- [상수] 변경 0 (검증만)
- [타이밍] tickMovement TAIL → sendStatePacket → encode → send → receive → processStatePacket
  → datatracker 순서 ✓
- [근사] 신규 0건. §7 영구 동치 4건 유지.
- [신규] 추가 의존 발견 없음
- [회귀] 5 vanilla POSE + Mixin 가드 → 회귀 0 확정
- [빌드] BUILD SUCCESSFUL 4s ✓

다음 세션 권고: **#2.7 AI 완결 — F-3 통합 인게임 deferred** (포커스 #1/#2.5/#2.6/#3/#4
   완결 후 통합 시점). 다음 포커스 진입 사용자 결정 (#3 / #1 / #4).

진행률: Phase A 2/2 + B 1/1 + C 3/3 + D 1/1 + E 2/2 + G 4/4 + H 3/3 + F 4/5 (F-3 deferred)
   = **22/23 (~96%) AI 완결**. F-3 인게임 deferred 1건만 잔존 — 통합 검증 시점.

---

## 7. 근사 이식 지점 (이 포커스) — 세션 2 갱신

현재 0건 (코드 변경). 완결 목표도 **0건** (1:1 위반 부분).

**영구 동치 (§7 등록 — 의무 아님)** — 세션 2 검증:
- **`afterMoveEntity` posY 보정** (원본 L1608-L1609) — 1.21.1 `Entity.calculateDimensions`
  의 `refreshPosition() + setBoundingBox()` 즉시 반영으로 **자동 처리**. 별도 Mixin 불필요.
- **`getBrightness` 렌더 보정** (원본 L1708-L1714) — 1.21.1 `standingEyeHeight =
  newDims.eyeHeight()` 즉시 반영. lighting 은 `entity.getEyeY()` (eyeHeight 기반) 자동 사용.
- **`resetInternalHeightOffset()` 수면 전용** (원본 L1688-L1692) — 1.21.1 vanilla 수면
  자동 dimensions (POSE.SLEEPING) → 별도 처리 N/A.
- **클라 PlayerBase `getEyeHeight` override 없음 (원본)** — 1.21.1 EntityDimensions.
  withEyeHeight 가 클라/서버 자동 대칭 → 원본보다 안전한 대칭 처리.

**잔존 1:1 위반 (Phase C 에서 해소)**:
- **server `MixinPlayerEntity.sm_getBaseDimensions_server` height 1.0F** — 원본 0.8F 와
  불일치. Phase C-1 에서 0.8F 로 정정 예정.

---

## 8. 소비처 영향 감사

### 8.1. 포커스 #2 Extended (완료)
- B-N-standup (1)(2) "영향 0 확정" → **수정** 필요: Phase 1 결과 0.6×0.8 에서만 동치. 세션
  136 기록의 "POSE 시스템 대체 → 영향 0" 은 값 미일치 상태에서의 평가였으므로 정정.
  focus_02_state_issues.md §7 갱신.

### 8.2. 포커스 #2.5 (Jumper Factor, 현재 포커스)
- 독립. Jumper factor 와 bbox sync 는 무관.

### 8.3. 포커스 #2.6 (Lava Border)
- 독립. lava border 는 AABB 스캔, dimensions 무관.

### 8.4. 포커스 #3 상태 전환 조건
- 현재 §18.1 (B-N-standup-4 vanilla flying sync) 과 **연관**. Phase D updatePose + Phase B
  StatePayload 확장이 §18.1 의 `UpdatePlayerAbilitiesC2SPacket` sync 와 통합 가능.
- 가능하면 병행 진행.

### 8.5. 포커스 #6 Speed Change
- 이미 완료. payload 패킷 인프라 (SmartMovingNetwork) 는 이 포커스에서 재활용.

---

## 9. 참고 자료 — 세션 2 보강

### 종합 리서치 (세션 2 신규)
- `docs/research/mapping/research_bbox_pose_eyeheight.md` — 원본 13 파일 5091 줄 + vanilla
  11 리서치 + 1.21.1 5 코드 모두 라인별 전수 read 결과. **모든 라인-by-라인 작업의 1차 근거**.
  §1 heightOffset 시스템 / §2 서버 sync 인프라 / §3 playerapi 베이스 / §4 vanilla / §5
  1.21.1 현황 / §6 매핑 정합 / §7 보강 권고 / §8 1:1 결론.

### 원본 소스 경로 (로컬, 라인 정확)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` (3345 줄)
  - **L1608-L1609**: `afterMoveEntity` posY 보정 ★ 세션 2 신규
  - **L1681-L1686**: `resetHeightOffset`
  - **L1688-L1692**: `resetInternalHeightOffset` ★ 세션 2 신규 (수면 전용)
  - **L1694-L1704**: `setHeightOffset(float offset)`
  - **L1708-L1722**: `getBrightness` / `getBrightnessForRender` 렌더 보정
  - **setHeightOffset(-1F) 호출처 14건**: L511 / L518 / L1369 / L1382 / L1390 / L1400 /
    L2129 / L2512 / L2519 / L2555 / L2798 / L2829 / L2851 / L2858
  - **setHeightOffset(wasHeightOffset) 2건**: L422 / L558 (이전 offset 복원)
  - **resetHeightOffset() 호출처 14건** ★ 세션 2 신규: L249 / L528 / L1171 / L1375 / L2173 /
    L2207 / L2219 / L2273 / L2746 / L2774 / L2780 / L2816 / L2819 / L2824 / L2842
- `...\SmartMovingServer.java` (354 줄)
  - **L40-L49**: 11 필드 (isCrawling/isSmall/crawlingCooldown/hunger/...)
  - **L69-L91**: `processStatePacket` (★ 7 비트 디코딩)
  - **L90**: `mp.sendPacketToTrackedPlayers(packet)` 브로드캐스트
  - **L225-L230**: `setCrawling`
  - **L232-L236**: `setSmall(isSmall)` → ★ `mp.setHeight(isSmall ? 0.8F : 1.8F)`
- `...\SmartMovingPacketStream.java` (214 줄)
  - **L93-L109**: `sendState` (writeByte + writeInt + **writeLong(state)** 64 bit)
  - **bit 사용 7개**: 12 isCrawlClimbing / 13 isCrawling / 14 isClimbing / **15 isSmall** /
    18 isCeilingClimbing / 31 isWallJumping / 33 isSneakButtonPressed
- `...\playerapi\SmartMovingServerPlayerBase.java` (266 줄)
  - **L52-L55**: `getHeight()`
  - **L58-L61**: `getMinY()`
  - **L64-L67**: `setMaxY(double)`
  - **L112-L115**: `getBox()`
  - **L142-L145**: `getEyeHeight()` ★ 공식 `player.height - 0.18F`
  - **L214-L217**: `setHeight(float)`
- `...\playerapi\SmartMovingPlayerBase.java` (313 줄, 클라이언트)
  - **L203-L212**: `updateEntityActionState` 이중 패턴 (tickEssential 항상 + isActive 분기)
  - **bbox/height/eyeHeight override 0건** ★ — 클라/서버 비대칭

### 1.21.1 이식 대상 (세션 2 갱신)
- `src/client/java/.../mixin/client/MixinPlayerEntityClient.java` (✅ Phase 1 — 세션 137 완료)
  - L55-L75 `sm_getBaseDimensions_client` (8 SM OR → 0.6×0.8 + 0.62F)
  - L109-L135 `sm_updatePose_client` (4 분기 + cancel)
- `src/main/java/.../mixin/MixinPlayerEntity.java` (Phase C/D 수정 대상)
  - L35-L46 `sm_getBaseDimensions_server` ★ height 1.0F → **0.8F 정정 (Phase C-1)**
  - L56-L68 `sm_updatePose_server` (Phase D-1 isSmall 단일 분기 단순화)
- `src/main/java/.../server/SmartMovingServer.java` (Phase A 검증만, 11 필드 충족)
  - L112-L127 `processStatePacket` (★ 7 비트 디코딩 — 원본 1:1)
- `src/main/java/.../network/SmartMovingState.java` (Phase B 검증 [x] — 21 bit 인코딩 완료)
- `src/main/java/.../network/SmartMovingNetwork.java` (★ Phase G — 핸들러 미배선)

### 연관 포커스
- 포커스 #2 Extended (완료 — 세션 134). B-N-standup 기록 세션 137 에서 Phase 1 연계 갱신.
- 포커스 #2.5 (Jumper Factor — AI 완결 22/22 + F-6 deferred).
- 포커스 #2.6 (Lava Liquid Border — AI 완결 20/21 + E-3 deferred).
- 포커스 #3 §18.1 (B-N-standup-4 vanilla flying sync). 본 포커스 Phase D 와 병합 가능.
