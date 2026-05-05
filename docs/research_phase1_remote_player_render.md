# 리서치 — Phase 1: 다른 player render 시 SM 자세 적용

## 작업 범위

다른 player (= remote) 의 SM state 가 client side 에 sync 됐지만 **render 시 mixin 가드 (`instanceof ClientPlayerEntity`) 에서 skip 되어 SM 자세 미적용**. 다른 player 도 SM 자세 적용되도록 가드 변경 + 관련 인프라 정비.

## 핵심 BUG (dump 로 확정)

`MixinPlayerEntityModelClient.sm_setAnglesHead` L116 / `sm_setAngles` L167 / `MixinPlayerEntityRenderer.sm_setupTransforms` L466 / `sm_captureBodyYaw` L149:
```java
if (!(entity instanceof ClientPlayerEntity player)) return;
```
- local player (= 자기 자신) 만 처리.
- 다른 player (= `AbstractClientPlayerEntity`) 는 **return** → SM 자세 미적용.

## 가드 분류 (= mixin 별 적용 대상)

### Group A — 자세/매트릭스 (★ Phase 1 변경 대상 ★)
Render path. 다른 player 도 SM 자세 적용 필요.

| Mixin | 라인 | 역할 |
|-------|------|------|
| MixinPlayerEntityModelClient | L116 (sm_setAnglesHead) | 비행 sneaking 임시 변경, override quat clear |
| MixinPlayerEntityModelClient | L167 (sm_setAngles) | 분기별 sm_animateXxx (자세 매핑) |
| MixinPlayerEntityRenderer | L466 (sm_setupTransforms) | body tilt R_x 매트릭스 |
| MixinPlayerEntityRenderer | L149 (sm_captureBodyYaw) | body yaw force/fade |
| MixinPlayerEntityRenderer | sm_modifyBodyYaw (ModifyArg) | body yaw 인자 변경 |
| MixinPlayerEntityRenderer | sm_modifyNetHeadYaw (ModifyArg) | head yaw 인자 변경 |
| MixinPlayerEntityRenderer | sm_getPositionOffset | 모델 위치 보정 |

### Group B — Local 전용 (변경 X)
Physics/input/state-update path. 자기 entity 의 mixin 이라 ClientPlayerEntity 가 정확.

- `MixinAbstractClientPlayerEntityClient` — sneak/abilities/input 처리.
- `MixinEntityClient` — move/physics/box.
- `MixinLivingEntityClient` — physics/pose/swim.
- `MixinPlayerEntityClient` — pose/dim/jump.
- L466 의 자기 자신 `localPlayer` 처리는 일부 분기.

### Group C — 일부 다른 player 처리 (검토)
- `MixinCapeFeatureRenderer` L71 — cape 클램프 (현재 local 만, 다른 player 도 가능?).
- `MixinLivingEntityRenderer` L38 — sneakNameTag (= 다른 player 만).

## 핵심 발견 — 일부 변수 static (multiplayer BUG)

```java
public static float smCachedAnimationProgress;   // 매 frame entity 별 갱신 → 마지막 entity 값 잔존
public static boolean smBodyYawActive_publicShared;
public static float smCachedBodyYawNaturalDeg;
public static float smCachedBodyYawLaggedDeg;
public static float smStandardBodyYawPrev;
public static float smHeadPitchFaded;
public static float smSlidingTiltXFaded;
... (fade prev 다수)
```

★ `smCachedXxx` 는 매 setupTransforms 호출 시 **마지막 player 의 값** 으로 덮어씀 → 다음 player render 시 잘못된 값 사용 ★.

★ fade prev 도 static — 두 player 가 동시 SM state 변경 시 fade lerp 가 player 간 mixed ★.

**Instance field (= player 별 정상)**:
- `SmartMovingClientState.smOuterTiltX` (instance).
- `SmartMovingClientState.smOuterTiltX_prev` (instance).
- `SmartMovingClientState.smOuterExtraYaw_prev` (instance).
- `SmartMovingClientState.smOuterFade_prevTime` (instance).

## Local vs Remote — 처리 차이

| 항목 | Local (ClientPlayerEntity) | Remote (AbstractClientPlayerEntity) |
|------|---------------------------|-------------------------------------|
| SM state source | client tick 매 tick 계산 (`tickEssential`) | server-relay sync (`processStatePacket`) |
| input | KeyBinding | server packet 의 키 정보 (= 일부 미동기화) |
| physics | SmartMovingClient handle | server 측 처리 |
| 자세 매핑 | sm_animateXxx | **동일 sm_animateXxx 적용 가능** (state 같음) |
| body tilt R_x | sm_setupTransforms | **동일** |
| body yaw | sm_modifyBodyYaw + smBodyYawActive | **동일** (state 따라) |

★ 자세/매트릭스/yaw force 모두 SmartMovingClientState 의 SM state 값에 의존 — local/remote 처리 동일 ★.

## 메모리 검토

- `feedback_remote_isclient_pattern.md` — `worldObj.isRemote = multi player` 매핑. 1.21.1 server 측 `MinecraftClient.getServer()==null` 사용.
- `MixinAbstractClientPlayerEntityClient` 존재 — 다른 player (= remote) 처리 mixin 이미 있음. 그러나 `ClientPlayerEntity` 가드라 local 만.
- 메모리 `project_sliding_animation_complete.md` 등 모든 자세 완결 메모리 → 자기 자신만 검증된 상태.

## Fix 설계

### 옵션 A — 가드 변경 (단순)
- `instanceof ClientPlayerEntity` → `instanceof AbstractClientPlayerEntity`.
- 모든 mixin 의 entity 변수 type 변경.
- input/physics 사용하는 코드는 ClientPlayerEntity cast 추가.

### 옵션 B — 두 가드 분기
- ClientPlayerEntity 분기 — 기존 path.
- AbstractClientPlayerEntity 분기 (= ClientPlayerEntity 외) — render 만 적용 (input/physics 제외).

### 권장 = 옵션 A + cast
- 자세/매트릭스 path 는 entity type 무관 처리.
- input/physics 사용 코드 는 ClientPlayerEntity 명시 cast (`if (entity instanceof ClientPlayerEntity local) { ... }` 분기).

## 위험 / 회귀 가능성

1. **input 사용 코드** — `player.input.sneaking` 등 ClientPlayerEntity 만 가능. 다른 player 도 처리 시 input 미접근 → NullPointerException 위험. cast 분기 필요.
2. **smCachedXxx static 변수** — 두 player render 시 mixed 값 → fade lerp 잘못. instance 화 또는 player 별 분기 필요.
3. **smBodyYawOverride force** — 다른 player 도 force 시 vanilla bodyYaw 무시. 다른 player 의 server-side SM state 따라 force.
4. **MixinModelPart override quat** — 슬라이딩 등 override 적용 ModelPart 가 모든 player 공유 (= ModelPart 자체 mixin). 한 player 의 override quat 가 다른 player render 에 잔존? 확인 필요.

## 작업 단계 (Phase 1)

### Phase 1-A — 자세 매핑 mixin 가드 변경 (= Group A)
1. **MixinPlayerEntityModelClient.sm_setAnglesHead** — entity type AbstractClientPlayerEntity 처리.
2. **MixinPlayerEntityModelClient.sm_setAngles** — 동일.
3. **MixinPlayerEntityRenderer.sm_setupTransforms** — 동일.
4. **MixinPlayerEntityRenderer.sm_captureBodyYaw** — 동일.
5. **MixinPlayerEntityRenderer.sm_modifyBodyYaw / sm_modifyNetHeadYaw** — local/remote 둘 다 처리 (state 따라 force).
6. **MixinPlayerEntityRenderer.sm_getPositionOffset** — 동일.

### Phase 1-B — input 사용 코드 분기
- `player.input.sneaking` 등 ClientPlayerEntity 만 가능 코드:
  - 다른 player 처리 시: server-relay sync 의 SM state (sneaking 비트) 또는 entity.isSneaking() 사용.
  - 또는 cast 분기 (`if (entity instanceof ClientPlayerEntity local) input 사용`).

### Phase 1-C — static 변수 player 별 분리
- `smCachedAnimationProgress`, `smCachedBodyYawNaturalDeg`, `smCachedBodyYawLaggedDeg` 등.
- 옵션 1: instance field 로 이동 (player 별 SmartMovingClientState).
- 옵션 2: render 시 player 별 lookup map.
- 우선순위: 자세 path 만 변경 → static 변수 필요 시점에 변경.

### Phase 1-D — MixinModelPart override quat 검증
- 다른 player render 시 override quat 적용 path 확인.
- player 별 override 분리 필요 시 수정.

### Phase 1-E — 인게임 검증
- Client1 의 SM 자세 (비행/슬라이딩/엎드리기 등) 가 Client2 측에서 보임.
- 두 player 동시 SM state 변경.

### Phase 1-F — 회귀 검증
- Local player 자세 정상.
- input/physics 정상.
- 다른 분기 (cape, name tag 등) 영향 X.

## 인게임 검증 시나리오

- (a) **rudals 비행** → Tester2 측에서 rudals 비행 자세 (슈퍼맨) 보임.
- (b) **rudals 슬라이딩** → Tester2 측에서 슬라이딩 자세 보임.
- (c) **rudals 엎드리기** → Tester2 측에서 엎드리기 자세 보임.
- (d) **두 player 동시 SM** → 각자 자기/다른 player 자세 정확.
- (e) **회귀**: 자기 자신 자세 그대로.

## 다음 단계 미뤄진 작업 (Phase 2 등)

- **Phase 2 — late join sync** — 새 player 접속 시 기존 player SM state broadcast.
- **Phase 3 — fade variable instance 화** — multiplayer fade lerp 정확.
- **Phase 4 — input source 결정** — server-relay 의 sneak/grab 비트 활용.
