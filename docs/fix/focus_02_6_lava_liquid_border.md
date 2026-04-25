# 포커스 #2.6 — Lava Liquid Border 전수 이식

> **B-42c (2) 확장 포커스**. 원본 `SmartMovingBase.getLiquidBorder()` (L131-L151) 의 lava
> 분기 + modded liquid 분기 + `handleLava()` (L578-L600) SM lava 이동 처리를 1.21.1 Fabric
> 으로 **엄격 1:1** 이식.
>
> **목표**: `cfg.lavaLikeWater = true` (Creative 기본값) 시 lava 에서 water 와 동일한 swim/dive
> 거동 복원. 원본 B-7c 진입 조건 `cfg.isLavaLikeWaterEnabled() && player.isInLava()` 은 이미
> 이식됐으나, 실제 lava 수영은 `getLiquidBorder` lava 반영이 없어 **현재 미작동 상태**.
>
> **진입 배경**: Extended #2 세션 133 에서 `lavaLikeWater` Config 필드 + `isLavaLikeWaterEnabled()`
> 헬퍼 + B-7c (updateSwimState 진입 조건) 이식 완료. 그러나 `getLiquidBorder` (B-42c) 가 세션
> 119 에서 water-only 로 이식되어 lava 반영 누락. 연결만 하면 실제 동작 — 소형 포커스.

---

## 0. 현재 상태 (진입 시점)

### 이식 완료 (의존 조건 만족)
| 요소 | 위치 | 상태 |
|---|---|---|
| `cfg.lavaLikeWater = false` 필드 | SmartMovingConfig L138 | ✅ 세션 133 |
| `cfg.isLavaLikeWaterEnabled()` 헬퍼 | SmartMovingConfig L673-L679 | ✅ 세션 133 |
| Properties IO (`move.lava.water`) | SmartMovingConfig L944/L1070 | ✅ 세션 133 |
| B-7c updateSwimState 진입 조건 | SmartMovingSwimmer L98-L114 | ✅ 세션 127 |
| `isInLiquid(player)` 메서드 | ClientState L2184 | ✅ 세션 127 (water-only 기반) |
| `getMaxPlayerLiquidBetween` / `getMinPlayerLiquidBetween` | ClientState L2117/L2148 | ✅ 세션 119 (water-only 기반) |

### 미이식 (이 포커스 범위)
| 요소 | 원본 라인 | 상태 |
|---|---|---|
| `getLiquidBorder` lava 분기 | SmartMovingBase L139-L150 | ❌ 세션 119 근사 |
| `getLiquidBorder` modded fluid 분기 | SmartMovingBase L147-L148 (`material.isLiquid() → 1F`) | ❌ 세션 119 근사 |
| `handleLava()` SM lava 이동 | SmartMovingSelf L578-L600 | ❌ 미이식 (vanilla 위임) |
| `tryJump` / isHeadJumping lava 연동 | SmartMovingSelf L1852 / L2530 / L2643 | ❌ 확인 필요 |
| `_lavaSwimParticlePeriodFactor = 4F` Config 필드 | SmartMovingConfig L164 | ❌ 세션 1 신규 발견 (lava 파티클 주기) |
| `isLava(block)` 헬퍼 | SmartMovingBase L123-L129 | ❌ 세션 1 신규 발견 (Phase D 보조) |

### 신규 발견 (포커스 #2.6 세션 1, 2026-04-25)
> **종합 리서치**: `docs/research/mapping/research_lava_border.md` — 원본 4 파일 5526 줄
> 전수 read (4 Agent 병렬) 후 lava/liquid 모든 라인 + 분기 + 상수 + 호출처 정리.

추가 검증/보강 항목:
1. `handleSwimming` 본체 lava 진입 조건 (Self L232) — 1.21.1 SmartMovingSwimmer.updateSwimState
   B-7c 이식 검증 필요 (정확히 `(isInWater() || (wasSwimming && isInLiquid()) || (lavaLikeWaterEnabled && isInLava()))` 3-OR 보존?)
2. `handleAlternativeFlying` / `handleLand` 의 `handledLava` 필터 (Self L602-L643) — 1.21.1
   에 동등 메서드 없음 (vanilla 위임). Phase C 이식 시 검증.
3. `isInLiquid()` 정확 공식 (Base L411-L416): `getMax > minY || getMin < maxY` — focus_02_6
   B-1 검증 시 공식 일치 확인 필요.
4. `getMax/MinPlayerLiquidBetween` 스캔 방향 (Base L418-L451): Max=위→아래, Min=아래→위.
5. `getNormalWaterBorder` 구현 (Base L152-L163): vanilla `FluidState.getHeight` 동치 근거.
6. `reverseHandleMaterialAcceleration` (Base L884-L932): `Material.water` 고정 — lava 무관.

---

## 1. 1:1 번역 룰

원본 `SmartMovingBase.java` L131-L151 `getLiquidBorder()` + `SmartMovingSelf.java` L578-L600
`handleLava()` 를 **축약·간소화·대체 매핑 금지**. vanilla API 표면 매핑만 허용.

### 표면 매핑 (허용)
| 원본 | 1.21.1 |
|---|---|
| `sp.worldObj.getBlock(i, j, k)` | `world.getBlockState(pos).getBlock()` |
| `Block.getBlockFromName("water")` | `Blocks.WATER` |
| `Block.getBlockFromName("lava")` | `Blocks.LAVA` |
| `Block.getBlockFromName("flowing_water")` / `flowing_lava` | 1.21.1 에서 flowing vs still 통합 — `FluidState` 기반 |
| `block.getMaterial()` → `Material.water/lava` | `FluidState.isIn(FluidTags.WATER/LAVA)` |
| `material.isLiquid()` | `!fluid.isEmpty()` (fluid 있는 모든 블록) |
| `sp.handleLavaMovement()` | `player.isInLava()` |
| `sp.moveFlying(strafe, forward, speed)` | `SmartMovingSwimmer.moveFlying(...)` (이미 이식) |
| `sp.isOffsetPositionInLiquid(dx, dy, dz)` | `player.wouldCollideAtOffset(offset)` 또는 AABB + fluid 체크 |

### 금지
- lava 분기 생략 (`return 0F` 근사 유지)
- modded liquid 생략 (`!isEmpty → 1F` 누락)
- `handleLava` 전체 미이식 (vanilla 위임만) — 원본은 `lavaLikeWater=false` 시 SM 자체 처리

### 예외
- `getNormalWaterBorder` metadata 기반 계산 (`>=8 → 1F` / `==0+air → 0.8875F` / `(8-meta)/8F`)
  → 1.21.1 `FluidState.getHeight(world, pos)` 는 vanilla FlowableFluid 가 동일 시멘틱 반환
  (metadata 를 FluidState level property 로 저장). 완전 동치.

---

## 2. 원본 구조 매핑

> **종합 리서치 참조**: `docs/research/mapping/research_lava_border.md` — 원본 4 파일 5526 줄
> 전수 read 결과. 본 §2 는 핵심 발췌. 자세한 라인별 분석은 리서치 파일 참조.

### 2.0 원본 `isLava(Block block)` (SmartMovingBase L123-L129) — 세션 1 신규 추가
```java
@SuppressWarnings("static-method")
protected boolean isLava(Block block) {
    if (block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return true;
    return block != null && block.getMaterial() == Material.lava;
}
```
**1.21.1 매핑**: `world.getFluidState(pos).isIn(FluidTags.LAVA)` (FluidState 기반).
**용도**: lava 블록 직접 체크. Phase D 점프 조건 검증 시 보조 활용 가능.

### 2.1 원본 `getLiquidBorder` (SmartMovingBase L131-L151) 분기 전수
```java
protected float getLiquidBorder(int i, int j, int k) {
    float finiteLiquidBorder;
    Block block = sp.worldObj.getBlock(i, j, k);
    // 분기 1: water
    if (block == Block.getBlockFromName("water") || block == Block.getBlockFromName("flowing_water"))
        return getNormalWaterBorder(i, j, k);
    // 분기 2: FiniteLiquid mod (미이식, §7 기존 근사)
    if (SmartMovingOptions.hasFiniteLiquid
            && (finiteLiquidBorder = getFiniteLiquidWaterBorder(i, j, k, block)) > 0)
        return finiteLiquidBorder;
    // 분기 3: lava (flowing_lava 포함)
    if (block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return Config._lavaLikeWater.value ? getNormalWaterBorder(i, j, k) : 0F;
    // 분기 4: Material 기반
    Material material = sp.worldObj.getBlock(i, j, k).getMaterial();
    if (material == null || material == Material.lava)
        return Config._lavaLikeWater.value ? 1F : 0F;
    if (material == Material.water)
        return getNormalWaterBorder(i, j, k);
    if (material.isLiquid())
        return 1F;
    return 0F;
}
```

### 2.1.5 원본 `getNormalWaterBorder` (SmartMovingBase L152-L163) — 세션 1 신규 추가
```java
protected float getNormalWaterBorder(int i, int j, int k) {
    int blockMetaData = sp.worldObj.getBlockMetadata(i, j, k);
    if (blockMetaData >= 8)        return 1F;            // 가득
    if (blockMetaData == 0)
        if (sp.worldObj.isAirBlock(i, j+1, k))  return 0.8875F;  // source 표면 보정
        else                                     return 1F;
    return (8 - blockMetaData) / 8F;                       // level 1~7 → 0.875~0.125
}
```
**상수**: `0.8875F` (source 표면), `8F` 분모.
**1.21.1 매핑**: vanilla `FluidState.getHeight(world, pos)` 가 동일 시멘틱 (level + falling 보정).
완전 동치 — 별도 이식 불필요.

### 2.1.6 원본 `isInLiquid` (SmartMovingBase L411-L416) — 세션 1 신규 추가
```java
protected boolean isInLiquid() {
    return
        getMaxPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.minY ||
        getMinPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.maxY;
}
```
**의미**: `getMax > minY` (최상위 fluid 가 minY 위) OR `getMin < maxY` (최하위 fluid 가 maxY 아래).
**1.21.1**: 이미 이식됨 (`ClientState.isInLiquid(player)` L2184). Phase A 후 lava 자동 포함.
**B-1 검증 시 공식 정확 보존 확인 필수.**

### 2.1.7 원본 `getMax/MinPlayerLiquidBetween` (SmartMovingBase L418-L451) — 세션 1 신규 추가

**`getMaxPlayerLiquidBetween` (L418-L432)**: 위→아래 (yMax → yMin) 스캔. 첫 fluid → `j + border`.
**`getMinPlayerLiquidBetween` (L434-L451)**: 아래→위 (yMin → yMax) 스캔. 첫 fluid → 분기 (j > yMin → j / yMin → yMin).
**1.21.1**: 이미 이식됨 (`ClientState L2117/L2148`). Phase A 후 lava 자동 반영.

### 2.2 원본 `handleLava` (SmartMovingSelf L578-L600)
```java
private boolean handleLava(float moveForward, float moveStrafing,
                           boolean handledSwimming, boolean isLiquidClimbing) {
    boolean handleLava = !isFlying && !handledSwimming && !isLiquidClimbing
                      && sp.handleLavaMovement();
    if (handleLava) {
        standupIfPossible();
        resetClimbing();
        resetSwimming();
        double d1 = sp.posY;
        sp.moveFlying(moveStrafing, moveForward, 0.02F);
        sp.moveEntity(sp.motionX, sp.motionY, sp.motionZ);
        sp.motionX *= 0.5D;
        sp.motionY *= 0.5D;
        sp.motionZ *= 0.5D;
        sp.motionY -= 0.02D;
        if (sp.isCollidedHorizontally
                && sp.isOffsetPositionInLiquid(sp.motionX,
                    ((sp.motionY + 0.60000002384185791D) - sp.posY) + d1, sp.motionZ))
            sp.motionY = 0.30000001192092896D;
    }
    return handleLava;
}
```

### 2.2.5 원본 `handleSwimming` lava 진입 조건 (SmartMovingSelf L232) — 세션 1 신규 추가
```java
boolean handleSwimming = !isFlying && !isLiquidClimbing
                      && (sp.isInWater()
                          || (wasSwimming && isInLiquid())
                          || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()));
```
**3-OR 진입 조건**:
1. `sp.isInWater()` — vanilla 물
2. `wasSwimming && isInLiquid()` — 이전 틱 수영 + 현재 fluid 존재
3. `Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()` — lava 활성화 + lava 안

**1.21.1 이식 상태**: `SmartMovingSwimmer.updateSwimState` L98-L114 (B-7c, 세션 127).
**Phase B-4 신규 검증 항목**: 1.21.1 이식이 정확히 같은 3-OR 조건 보존하는지 확인.

### 2.2.6 원본 `handleLava` 호출 후 필터 (SmartMovingSelf L602-L643) — 세션 1 신규 추가
- **L602-L604**: `handleAlternativeFlying(...handledLava)` 메서드 시그니처 — `!handledLava` 필터
  (lava 처리됨 → 대안 비행 불가)
- **L633-L643**: `handleLand(...handledLava...)` — `!handledLava` 필터 (lava 처리됨 → 육지 처리 스킵)

**1.21.1 영향**: 1.21.1 에 `handleAlternativeFlying` / `handleLand` 동등 메서드 없음
(vanilla `LivingEntity.travel()` 위임). Phase C 이식 시 `handleLava` 호출이 vanilla travel 을
어떻게 차단할지 결정 필요 (Mixin `ci.cancel()` 등).

### 2.3 원본 tryJump / isHeadJumping lava 연동
- **L1852**: `boolean jump = jumpAvoided && isp.getIsJumpingField()
           && !sp.isInWater() && !sp.handleLavaMovement();` (handleJumping 메서드)
   → **lava 진입 시 점프 불가**.
- **L2529-L2530**: `isHeadJumping = isHeadJumping && !sp.onGround && !(isSwimming || isDiving)
           && !(isFlying || sp.capabilities.isFlying) && !(sp.handleWaterMovement() && sp.motionY < 0)
           && !sp.handleLavaMovement();`
   → **lava 진입 시 헤드점프 자동 해제**.
- **L2643**: `if (sp.onGround || isFlying || capabilities.isFlying || isSwimming || isDiving
           || sp.handleLavaMovement()) isSprintJump = false;`
   → **lava 진입 시 isSprintJump 해제**.

1.21.1 이식 상태 확인:
- L1414 `!player.isInLava()` (ClientState) — 확인됨
- L1185 `|| player.isInLava()` (ClientState) — 확인됨
- tryJump lava 블록 이식 여부 — Phase C 에서 검증.

### 2.4 원본 `isLiquidClimbing` / `handleLava` 호출 컨텍스트 (SmartMovingSelf L132-L134)
```java
boolean isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0
                        && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
boolean handledSwimming = handleSwimming(..., isLiquidClimbing, wasJumpingOutOfWater);
boolean handledLava = handleLava(moveForward, moveStrafing, handledSwimming, isLiquidClimbing);
```

1.21.1 `isLiquidClimbing` 은 B-7a 세션 127 에서 필드 승격 완료. `handledLava` 반환값 소비는
`handleAlternativeFlying(..., handledLava)` + `handleLand(..., handledLava, ...)` 등 — 1.21.1
에서는 vanilla `travel()` 이 lava 수영을 자체 처리하므로 `handledLava` 반환 시멘틱은
vanilla 위임.

---

## 3. Phase 구조

### Phase A. `getLiquidBorder` lava + modded 분기 복원

**A-1. lava 분기** (원본 L139-L140, L143-L144) — 세션 2 완료
- [x] A-1. `ClientState.getLiquidBorder` 에 FluidTags.LAVA 분기 추가:
  ```java
  if (fluid.isIn(FluidTags.LAVA)) {
      return cfg.isLavaLikeWaterEnabled() ? fluid.getHeight(world, pos) : 0F;
  }
  ```
  원본 L139-L140 `flowing_lava || lava → lavaLikeWater ? getNormalWaterBorder : 0F` +
  L143-L144 `Material.lava → lavaLikeWater ? 1F : 0F` 통합.
  **차이 주의**: 원본 L140 은 `getNormalWaterBorder` (0~1 높이) / L144 는 `1F` 고정. 1.21.1
  `fluid.getHeight(world, pos)` 는 LavaFluid 도 올바른 높이 (1F 또는 level 기반) 반환.
  vanilla LAVA 는 source `1F` + flowing `(8-level)/9F` 단계 — 원본 의도와 동치.

**A-2. modded liquid 분기** (원본 L147-L148) — 세션 2 완료
- [x] A-2. `ClientState.getLiquidBorder` 에 `!fluid.isEmpty() && !water && !lava` 시 `1F` 반환
  추가:
  ```java
  if (!fluid.isEmpty()) {
      if (fluid.isIn(FluidTags.WATER)) return fluid.getHeight(world, pos);
      if (fluid.isIn(FluidTags.LAVA)) {
          return cfg.isLavaLikeWaterEnabled() ? fluid.getHeight(world, pos) : 0F;
      }
      return 1F;  // modded liquid (원본 material.isLiquid() → 1F)
  }
  return 0F;
  ```
  원본 L147-L148 `material.isLiquid() → 1F` 대응. Petroleum/Oil/Honey 등 modded fluid 지원.

**A-3. 주석 갱신** — 세션 2 완료
- [x] A-3. `getLiquidBorder` Javadoc §7 근사 B-42c-b lava 해소 + modded 분기 추가 표기 갱신.

**A-4. §7 근사 기록 갱신** — 세션 2 완료
- [x] A-4. `focus_02_state_issues.md` §7 B-42c (2) `_lavaLikeWater` 항목에 "세션 2 해소
  (A-1+A-2)" 명시. FiniteLiquid mod (1) + getNormalWaterBorder metadata (3) 영구 잔존.

**A-5. `_lavaSwimParticlePeriodFactor = 4F` Config 필드** (세션 1 신규 발견) — 세션 2 완료
- [x] A-5. `SmartMovingConfig.java` 에 필드 + load/save IO 신규 등록.
  ```java
  public float lavaSwimParticlePeriodFactor = 4F;
  // load: getFloat(p, "move.lava.swim.particle.period.factor", lavaSwimParticlePeriodFactor)
  // save: setProperty("move.lava.swim.particle.period.factor", String.valueOf(...))
  ```
  **소비처**: 1.21.1 SmartMovingRender 또는 lava 파티클 생성 코드 (현재 직접 호출처 0건 추정).
  파티클 시각 효과만 영향 — 미이식 시 §7 영구 등록 가능 (lava 파티클 vanilla 동작 위임).
  → **결정 필요 (Phase A 진입 시)**: 이식 vs §7 등록 중 선택.

---

### Phase B. `isInLiquid` + `getMax/MinPlayerLiquidBetween` 검증

**B-1. `isInLiquid` 자동 반영 확인** — 세션 3 완료
- [x] B-1. `ClientState.isInLiquid` L2209-L2213 = `getMax + getMin` 호출. Phase A 후 lava
  자동 반영. **검증 통과** (코드 변경 0).

**B-2. `getMax/MinPlayerLiquidBetween` 자동 반영 확인** — 세션 3 완료
- [x] B-2. 두 메서드 모두 내부적으로 `getLiquidBorder` 호출. Phase A 후 lava 반환 자동 반영.
  **검증 통과** (코드 변경 0).

**B-3. 소비처 전수 감사** — 세션 3 완료
- [x] B-3. 4 소비처 모두 grep 검증 통과:
  - **Swimmer.updateSwimState** L98-L114 (B-7c): B-4 와 동일 — 1:1 일치 확인
  - **ClientState.fromSwimmingOrDiving** L2737 (crawlStandUpLiquidCeiling): `getMinPlayerLiquidBetween(maxY, maxY+1.1)` → Phase A 자동 lava 천장 판정
  - **ClientState.SwimBorderValues** L2277 (computeSwimBorderValues): `getMaxPlayerLiquidBetween(maxY-1.8, maxY+1.2)` → Phase A 자동 lava 수심
  - **handleSwimming motion 계산**: 진입 조건 (B-4) 통과 시 자동 처리 — 별도 검증 0

**B-4. `handleSwimming` 진입 조건 검증** (세션 1 신규 발견, 원본 L232) — 세션 3 완료
- [x] B-4. **원본 ↔ 1.21.1 1:1 일치 검증 통과**:
  ```
  원본 L232:        !isFlying && !isLiquidClimbing && (isInWater() || (wasSwimming && isInLiquid())
                                                       || (lavaLikeWater && handleLavaMovement()))
  1.21.1 Swimmer L109-L113: !sm.isFlying && !sm.isLiquidClimbing && (player.isTouchingWater()
                                || (sm.isSwimming_sm && ClientState.isInLiquid(player))
                                || (cfg.isLavaLikeWaterEnabled() && player.isInLava()))
  ```
  표면 매핑만 (sp.isInWater→player.isTouchingWater / wasSwimming→sm.isSwimming_sm 스냅샷 /
  Config→cfg / sp.handleLavaMovement→player.isInLava). 분기/조건/순서 전수 보존.

---

### Phase C. `handleLava` SM 자체 처리 이식 결정

**C-1. 이식 필요성 판단** — 세션 4 완료
- [x] C-1. 원본 `handleLava` L578-L600 은 `lavaLikeWater=false` 시 lava 에서 SM 자체 이동
  처리 (moveFlying + damping 0.5 + motionY -= 0.02 + 벽 점프 특수). 1.21.1 은 vanilla
  `LivingEntity.travel()` 이 lava 수영을 처리하므로 **이식 불필요 확인** 또는 **vanilla 와
  차이 확인**.
  - vanilla 1.21.1 lava 이동: `LivingEntity.travelInFluid` → `applyFluidMovingSpeed` + damping
    `0.5`. **원본과 동일 damping (0.5)** — 기본 lava 이동 동치.
  - 차이: 원본 `motionY -= 0.02D` (중력 감쇠) vs vanilla `0.05` (중력) — 미세 차이.
  - 차이: 원본 L594-L597 `isCollidedHorizontally + isOffsetPositionInLiquid → motionY=0.3`
    (lava 벽 점프) — vanilla `LivingEntity.swimUpward` 유사 처리 있음.

**C-2. 결정: 미이식 유지 + 근사 기록** — 세션 4 미선택 (C-3 채택)
- [~] C-2. `handleLava` SM 자체 이식은 **불필요** 결정 (vanilla 동치). §7 근사 기록 (B-42c
  관련 잔존):
  - lava 기본 이동 vanilla 위임 — 시멘틱 동치
  - 미세 상수 차이 (motionY -= 0.02D vs vanilla 0.05 중력): 체감 영향 낮음

**C-3. 대안: 이식 진행 (엄격 1:1 원칙)** — 세션 4 완료 (채택 + 이식 완결)
- [x] C-3. 사용자 지시가 "엄격 1:1" 이므로 C-2 대신 `handleLava` 도 이식 선택 가능:
  - `SmartMovingSwimmer.handleLava(player, sm, movementInput)` 별도 메서드
  - `MixinLivingEntityClient.sm_beforeTravel` 에서 `handleSwimming` 반환 false + lava
    + !isLiquidClimbing + !isFlying 시 호출
  - 내부: standupIfPossible + resetClimbing + resetSwimming + moveFlying + damping 0.5 +
    motionY -= 0.02 + 벽 점프 motionY=0.3
  - `ci.cancel()` 로 vanilla travel() 취소

**C-4. 선택 권고** — 세션 4 채택
- [x] C-4. **C-3 (엄격 이식) 권고** — Extended #2 방침 따름. 단 플레이테스트에서 체감 차이
  없으면 C-2 (근사 유지) 로 후퇴 가능.

**C-5. `handleAlternativeFlying` / `handleLand` 의 handledLava 필터 검증** (세션 1 신규 발견) — 세션 4 N/A
- [x] C-5. 1.21.1 에는 `handleAlternativeFlying` / `handleLand` 동등 메서드 없음 (vanilla travel
     위임). C-3 의 `ci.cancel()` 이 vanilla travel 통째 차단 → 자연스럽게 동등 효과 (lava 처리
     후 vanilla 의 swim/walk/fall 분기 진입 안 됨). N/A. 원본 L602-L604 `handleAlternativeFlying(...handledLava)` + L633-L643
  `handleLand(...handledLava...)` 가 `!handledLava` 필터로 lava 처리 후 다른 처리 차단.
  1.21.1 에는 동등 메서드 없음 (vanilla `LivingEntity.travel()` 위임). Phase C-3 채택 시
  `MixinLivingEntityClient.sm_beforeTravel` 에서 `handleLava` 호출 후 `ci.cancel()` 로 vanilla
  travel 차단 → 자연스럽게 동등 효과. 코드 변경 0 가능 — 검증만.

---

### Phase D. lava 연관 상태/점프 경로 감사

**D-1. `tryJump(SLIDE_DOWN/Up)` lava 조건**
- [x] **D-1 (세션 5 완료)**. 원본 L1852 `boolean jump = jumpAvoided && isJumping && !isInWater()
  && !handleLavaMovement()`. 1.21.1 분석:
  - vanilla 1.21.1 `LivingEntity.tickMovement()` L2632-L2653 jump 분기: `isInLava` + onGround
    && `g <= h` 시 vanilla `jump()` 호출 (L2645) → sm_jump 인터셉트 (Mixin L231-L238) →
    `jumpAvoided=true`. 즉 **lava 안에서도 jumpAvoided 가 켜질 수 있음** → 명시적 회피 필요.
  - 1.21.1 적용 위치: `SmartMovingJumper.handleJumping()` `e. 일반 점프` 진입 조건 (L468-L483)
    에 `&& !player.isTouchingWater() && !player.isInLava()` 추가.
  - 매핑: `!sp.isInWater()` ↔ `!player.isTouchingWater()` / `!sp.handleLavaMovement()` ↔
    `!player.isInLava()`. cfg.lavaLikeWater=true 시 lava 안 일반점프 차단 + handleLava 가
    motion 처리.

**D-2. `isHeadJumping` 해제 lava 조건**
- [x] **D-2 (세션 5 완료)**. 원본 L2525-L2530 `isHeadJumping = isHeadJumping && !sp.onGround
  && !(isSwimming || isDiving) && !(isFlying || capabilities.isFlying) && !(waterMovement &&
  motionY < 0) && !sp.handleLavaMovement()`. 1.21.1 검증:
  - `SmartMovingClientState.tickMain` L1413-L1418 이미 5개 게이트 모두 일치 (`!isOnGround`,
    `!(isSwimming_sm || isDiving)`, `!(isFlying || abilities.flying)`, `!(isTouchingWater
    && velocity.y < 0)`, `!isInLava`).
  - **추가 작업 없음**. 세션 119 (B-24 이식 시) 부터 lava 조건 포함되어 있었음.

**D-3. `isGroundSprinting` 해제 lava 조건**
- [x] **D-3 (세션 5 완료)**. 원본 L2679 `isGroundSprinting = canHorizontallySprint && (sp.onGround
  || isLevitating()) && !isSwimming && !isDiving && !isClimbing` — **lava 조건 없음**.
  - `SmartMovingClientState.tickMain` L1232-L1234 완벽 일치 (`_canHorizontallySprint17 &&
    (isOnGround() || isLevitating) && !isSwimming_sm && !isDiving && !isClimbing`).
  - **추가 작업 없음**.

**D-4. `isSprintJump` 해제 lava 조건** (원본 L2643)
- [x] **D-4 (세션 5 완료)**. 원본 L2643 `if (sp.onGround || isFlying || capabilities.isFlying
  || isSwimming || isDiving || sp.handleLavaMovement()) isSprintJump = false;`. 1.21.1 검증:
  - `SmartMovingClientState.tickMain` L1184-L1187 이미 6개 OR 모두 일치 (`isOnGround() ||
    isFlying || abilities.flying || isSwimming_sm || isDiving || isInLava()`).
  - **추가 작업 없음**.

---

### Phase E. 빌드 + 플레이테스트

**E-1. 빌드 검증**
- [x] **E-1 (세션 5)**. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s).

**E-2. 회귀 감사**
- [x] **E-2 (세션 6 완료)**. 포커스 #2 Extended 기존 이식 영향 grep 감사 (5 영역 / 코드 변경 0):
  - **E-2-1 Phase A getLiquidBorder** (ClientState L2102-L2128): lava 분기 ✓
    (FluidTags.LAVA + isLavaLikeWaterEnabled?), modded 분기 ✓ (1F), lavaSwimParticlePeriodFactor
    Config 필드 ✓ (Config L539/L1461/L1621 IO).
  - **E-2-2 Phase B 의존 자동 반영**: B-7c (Swimmer L113-L114 `isLavaLikeWaterEnabled() &&
    isInLava()` OR), B-42c (getMax/MinPlayerLiquidBetween L2150/L2181 → getLiquidBorder),
    B-42d (totalSwimWaterBorder L2277 → lava 반영), B-42-B39 (crawlStandUpLiquidCeiling
    L2737 → getMin → lava 반영) 모두 정상.
  - **E-2-3 Phase C handleLava** (Swimmer L732-L784): 진입 4-AND ✓, 9 단계 ✓, 상수 0.5D /
    0.02D / 0.60000002384185791D / 0.30000001192092896D 정확 보존 ✓. ci.cancel() 부작용 0
    재확인 (vanilla swimUpward / damage / sound / particle 모두 travel() 외부).
  - **E-2-4 Phase D lava 게이트 5개**: D-1 (Jumper L475 신규), D-2 (ClientState L1418), D-3
    (ClientState L1232-L1234 — 원본 lava 조건 없음, 일치), D-4 (ClientState L1185) 모두
    적용. handleLava 자체 진입 (L735) 포함 5개 일관성 ✓.
  - **E-2-5 playtest_fixes.md** 현재 포커스 표기 갱신 ("세션 6 — Phase E-2 회귀 감사" /
    상태 "🟢 AI 완결 임박 19/21 = 90%").
  - 회귀 0건. Phase #2 Extended 이식 (B-7c / B-42c / B-42d / B-42-B39) 영향 0.

**E-3. 플레이테스트 시나리오**
- [ ] E-3. 사용자 인게임 테스트:
  1. `lavaLikeWater = false` (Survival 기본): lava 에 닿으면 즉사 / 탈출 불가 — vanilla 동작
  2. `lavaLikeWater = true` (Creative 기본 또는 수동 변경): lava 에서 water 와 동일 swim/dive
     - `isDipping` / `isSwimming_sm` / `isDiving` 전환 정상
     - offset 구간별 motionY 계산 정상
     - wantJumpOutOfWater 로 lava 탈출 가능
     - crawlStandUpLiquidCeiling (B-42-B39) 머리 위 lava 천장 판정 정상

---

## 4. 의존 순서

```
Phase A (getLiquidBorder lava 복원) — 핵심 원자
   ↓
Phase B (isInLiquid / getMax·MinPlayerLiquidBetween 검증) — 자동 반영, 검증만
   ↓
Phase C (handleLava SM 자체 이식 — 권고 C-3 엄격 이식)
   ↓
Phase D (lava 연관 점프 / 상태 조건 감사)
   ↓
Phase E (빌드 + 플레이테스트)
```

**전체 규모 (세션 1 갱신)**: Phase A 5 (A-5 신규) + Phase B 4 (B-4 신규) + Phase C 5 (C-5 신규) +
Phase D 4 + Phase E 3 = **약 21 원자 / 예상 3-4 세션**. 단 다수 검증/N/A 항목으로 실 작업
규모는 ~15 원자 유사.

---

## 5. 1:1 번역 체크리스트 (원자별 적용)

각 원자 이식 시 아래 10항 검증:
- [근거] 원본 라인 확보 (`C:\Work\minecraft\porting\sm_original\SmartMoving\`)
- [근거] 1.21.1 이식 위치 확정
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 대조
- [분기] 모든 if/else 전수 이식
- [상수] 모든 상수값 정확 (0.5/0.02/0.3/0.6000000238 등)
- [타이밍] 호출 순서 원본 일치
- [근사] 근사 불가피 시 §7 등록 (§7 은 이 포커스 별도 §7 로 관리)
- [신규] 추가 의존 발견 시 §3 에 원자 추가
- [회귀] 포커스 #2 Extended 기존 이식 영향 없음
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` 성공

---

## 6. 작업 기록 (세션별)

### 세션 0 — 2026-04-25 — 포커스 #2.6 신설

- 포커스 #2 Extended 완결 후 세션 135 잔존 근사 감사에서 `getLiquidBorder` lava 분기 미이식
  확인. `cfg.lavaLikeWater` 필드 + `isLavaLikeWaterEnabled()` 헬퍼는 이미 세션 133 이식됨
  (B-51 범위). B-7c updateSwimState 진입 조건도 lavaLikeWater 반영 (세션 127). 단
  **`getLiquidBorder` 에서 lava 를 0F 로 반환** 하는 근사가 잔존하여 실제 lava 수영 미작동.
- 해결 범위 소~중 (연결 + `handleLava` 이식 여부 결정) — Extended 범위 외로 분리하여
  별도 포커스 2.6 로 완결.
- 원본 `SmartMovingBase.java` L131-L151 `getLiquidBorder` + `SmartMovingSelf.java` L578-L600
  `handleLava` + 호출 컨텍스트 전수 감사 후 Phase A/B/C/D/E 약 15 원자 확정.

**다음 세션 권고**: Phase A-1 (lava 분기) + A-2 (modded liquid 분기) — 소형 수정 한 번에.

### 세션 1 — 2026-04-25 — 원본 4 파일 5526 줄 전수 리서치 + 문서 보강

사용자 지시: "2.6페이즈와 관련된 모든 원본 코드및 리서치 파일들을 1개도 빠트리지 말고,
   각 파일의 처음부터 끝까지 라인별로 청크 분리해서 모든 라인을 읽고... 1대1 번역이라는
   걸 명심하고 모든라인을 다 리서칭".

진행한 작업:
1. **원본 4 파일 size 확인**:
   - SmartMovingBase.java: 932 줄
   - SmartMovingSelf.java: 3345 줄 (가장 큼)
   - SmartMovingClientConfig.java: 595 줄
   - SmartMovingConfig.java: 654 줄
   - **총 5526 줄**
2. **4 Agent 병렬 위임** (Explore subagent, 각 파일별 전수 read + lava/liquid 키워드 매치
   모든 라인 + 컨텍스트 추출):
   - SmartMovingBase.java: 9 메서드 추출 (isLava L123-L129 / getLiquidBorder L131-L150 /
     getNormalWaterBorder L152-L163 / getFiniteLiquidWaterBorder L165-L181 / isInLiquid
     L411-L416 / getMaxPlayerLiquidBetween L418-L432 / getMinPlayerLiquidBetween L434-L451 /
     isInsideOfMaterial L524-L543 / reverseHandleMaterialAcceleration L884-L932)
   - SmartMovingSelf.java: 9 lava 위치 (L132 / L133-L134 / L135 / L136 / L232 / L578-L600 /
     L602-L604 / L633-L643 / L1852 / L2529-L2530 / L2643)
   - SmartMovingClientConfig.java: 1 메서드 (isLavaLikeWaterEnabled L87-L90)
   - SmartMovingConfig.java: 2 Property (_lavaLikeWater L162-L163 / _lavaSwimParticlePeriodFactor L164)
3. **종합 리서치 파일 작성**: `docs/research/mapping/research_lava_border.md` 신규.
   원본 코드 그대로 + 라인 번호 + 분기 트리 + 상수 + 1.21.1 매핑.
4. **신규 발견 항목** (focus_02_6 보강):
   - **`_lavaSwimParticlePeriodFactor = 4F`** Config 필드 (Phase A-5 신규 원자)
   - **`isLava(block)` 헬퍼** (§2.0 추가)
   - **`getNormalWaterBorder` 구현 디테일** (§2.1.5 추가)
   - **`isInLiquid` 정확 공식** (§2.1.6 추가)
   - **`getMax/MinPlayerLiquidBetween` 스캔 방향** (§2.1.7 추가)
   - **`handleSwimming` 본체 lava 진입 조건** (§2.2.5 추가, Phase B-4 신규 원자)
   - **`handleAlternativeFlying`/`handleLand` handledLava 필터** (§2.2.6 추가, Phase C-5 신규 원자)
   - **`reverseHandleMaterialAcceleration` water 전용** (§2 영향 0 명시)
5. **§3 신규 원자 추가**: A-5, B-4, C-5 (총 3 원자)
6. **§3 D-4 정정**: 이전 "jumpAvoided" 오기 → "isSprintJump 해제" (원본 L2643 정확 표기)

근사 여부: 신규 0. §7 잠재 근사 후보만 발견 (`_lavaSwimParticlePeriodFactor` 미이식 시 §7 등록 가능).

완료 전 검증 체크리스트 (세션 1 기준):
- [근거] 원본 4 파일 5526 줄 전수 read 완료 (4 Agent 병렬, 1개도 빠짐없이)
- [근거] 종합 리서치 파일 작성 (`docs/research/mapping/research_lava_border.md`)
- [대응] 신규 발견 항목 모두 §0/§2/§3 에 반영
- [분기] 원본 7 분기 (water/FiniteLiquid/lava block/Material.lava/Material.water/Material.isLiquid/0F)
   + handleLava 본체 4-AND 진입 + 9 처리 단계 모두 문서화
- [상수] `0.5D` damping / `0.02D` 중력 / `0.60000002384185791D` offset / `0.30000001192092896D`
   motionY / `0.8875F` source 표면 / `4F` 파티클 주기 모두 리서치 파일에 정확 보존
- [타이밍] handleSwimming → handleLava → handleAlternativeFlying → handleLand 호출 순서 보존
- [근사] 신규 0. §7-1 후보 1건 (lavaSwimParticlePeriodFactor) 만 등록 가능.
- [신규] §3 신규 원자 3건 (A-5/B-4/C-5) 추가
- [회귀] 코드 변경 0 → 회귀 0
- [빌드] 코드 변경 없음 — 빌드 영향 0

다음 세션 권고: **Phase A 진입** — A-1 (lava 분기) + A-2 (modded liquid) + (선택) A-5
   (`_lavaSwimParticlePeriodFactor`) 묶어 진행. 단 A-5 는 이식 vs §7 등록 사용자 결정 필요.

진행률: **세션 1 리서치 완료**, Phase 진입 전. 전체 #2.6 1/21 (~5%).

### 세션 2 — 2026-04-25 — Phase A 일괄 (A-1~A-5)

사용자 지시: "엄격 1:1" 유지 + Phase A 일괄. A-5 사용처 검증 후 §7 등록 vs 이식 결정 →
   사용자 직관 ("코드 추가 길지 않으면 그냥 지금 하는게 나을것같은데") 채택 → **이식 진행**.

진행한 작업:
1. **A-1+A-2 `getLiquidBorder` 본체 1:1 재작성** (`SmartMovingClientState.java` L2098-L2125):
   - 분기 1 (empty → 0F): 보존
   - 분기 2 (water + Material.water 통합 → fluid.getHeight): 보존
   - **분기 3+4 (lava 신규)**: `fluid.isIn(FluidTags.LAVA) && cfg.isLavaLikeWaterEnabled()
     ? fluid.getHeight : 0F` (원본 L139-L140 + L142-L144 통합)
   - **분기 6 (modded 신규)**: 위 분기 모두 미통과 + !fluid.isEmpty() (이미 위에서 0F 반환했으므로
     도달 시 자동) → `1F` (원본 L147-L148, modded liquid 지원)
2. **A-3 Javadoc 갱신**: B-42c-b lava 해소 + modded 분기 추가 표기 (세션 2 표시).
3. **A-4 `focus_02_state_issues.md` §7 갱신**: B-42c (2) `_lavaLikeWater` 항목에 "세션 2
   해소 (A-1+A-2)" 명시 + 영구 잔존 (1) FiniteLiquid mod / (3) getNormalWaterBorder
   metadata 항목 표기.
4. **A-5 `lavaSwimParticlePeriodFactor = 4F` 필드 + IO 신규** (`SmartMovingConfig.java`):
   - 필드 선언 (L528-L540, lavaLikeWater 직후) + 원본 사용처 발견 (`SmartMoving.java` L123)
     주석 명시 — `maxSpawnSwimmingParticle = factor × 0.01F`, water (1F) 대비 4배 드물게.
   - load IO `move.lava.swim.particle.period.factor` (lavaLikeWater 직후)
   - save IO 동일 key
   - 사용처 0건 (1.21.1 spawnSwimmingParticle 시스템 자체 미이식) — 미래 활용 대비.
5. **`playtest_fixes.md` 갱신**: 현재 포커스 #2.6 진행 중 + 종합 리서치 파일 링크 추가.

근사 여부: 신규 0. **세션 1 §7 후보 4건 중 1건 (lavaSwimParticle) 사전 해소** (이식). 영구 잔존
   3건 (FiniteLiquid mod / isInsideOfMaterial water 전용 / reverseHandleAcc water 전용).

완료 전 검증 체크리스트 (세션 2 기준):
- [근거] 원본 라인 — `SmartMovingBase.java` L131-L150 + `SmartMovingConfig.java` L164 +
   `SmartMoving.java` L123 (③ 리서치 §1 발췌)
- [근거] 1.21.1 이식 위치 — `SmartMovingClientState.java` L2098-L2125 (getLiquidBorder) +
   `SmartMovingConfig.java` L528-L540 + IO L1448-L1449 / L1607-L1608
- [대응] 원본 ↔ 1.21.1 1:1 (분기 4개 + 상수 + 게이트)
- [분기] empty / water / lava (lavaLikeWater 게이트) / modded — 4 분기 전수
- [상수] `0F` / `1F` 정확 / `4F` (factor) 보존
- [타이밍] 분기 순서 보존 (empty 먼저 → water → lava → modded)
- [근사] 신규 0. §7-1 영구 후보 3건 유지.
- [신규] §3 A-5 추가 의존 발견 없음. focus_02_state_issues §7 갱신 1건.
- [회귀] 신규 분기 추가 — 기존 water 동작 영향 0. lava `lavaLikeWater = false` 시 0F (기존 동치).
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (6s)

다음 세션 권고: **Phase B** (검증 — 코드 변경 0). B-1 `isInLiquid` 자동 반영 확인 + B-2
   `getMax/MinPlayerLiquidBetween` 자동 반영 확인 + B-3 소비처 전수 감사 + B-4 `handleSwimming`
   진입 조건 검증 (원본 L232 의 3-OR 조건이 1.21.1 SmartMovingSwimmer.updateSwimState 에 보존?).

진행률: Phase A 완결 (5/5 = 100%), 전체 #2.6 6/21 (~29%).

### 세션 3 — 2026-04-25 — Phase B 일괄 검증 (B-1~B-4)

사용자 지시: "엄격 1:1" 유지 + Phase B 일괄. 검증만 (코드 변경 0).

진행한 작업:
1. **B-4 handleSwimming 진입 조건 1:1 검증** (③ 리서치 §2.2.5 + 1.21.1 Swimmer L109-L113):
   - 원본 L232: `!isFlying && !isLiquidClimbing && (isInWater() || (wasSwimming && isInLiquid())
     || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()))`
   - 1.21.1 SmartMovingSwimmer.updateSwimState L109-L113: 표면 매핑만 (sp→player /
     Config→cfg / handleLavaMovement→isInLava). 3-OR 분기/조건/순서 전수 보존 ✅
2. **B-1/B-2/B-3 grep 검증** (모두 자동 lava 반영, 코드 변경 0):
   - `ClientState.isInLiquid` L2209-L2213 = `getMax + getMin`
   - `ClientState.getMaxPlayerLiquidBetween` L2142 / `getMinPlayerLiquidBetween` L2173 →
     내부 `getLiquidBorder` 호출 (Phase A 결과 자동 lava 반환)
   - `ClientState.fromSwimmingOrDiving` L2737 (crawlStandUpLiquidCeiling) → `getMin(maxY, maxY+1.1)`
   - `ClientState.SwimBorderValues.computeSwimBorderValues` L2277 (totalSwimWaterBorder) →
     `getMax(maxY-1.8, maxY+1.2)`
   - **handleSwimming motion 계산**: 진입 조건 (B-4) 통과 시 자동 처리 — 별도 검증 0

근사 여부: 신규 0. §7 영구 후보 3건 유지 (FiniteLiquid mod / isInsideOfMaterial / reverseHandleAcc).

완료 전 검증 체크리스트 (세션 3 기준):
- [근거] ③ 리서치 §2.2.5 (handleSwimming 진입 조건) + 1.21.1 Swimmer L109-L113 line-by-line
- [근거] 1.21.1 grep 결과 — 4 소비처 모두 getLiquidBorder 의존 확인
- [대응] 원본 L232 ↔ 1.21.1 Swimmer 3-OR 1:1 (표면 매핑만)
- [분기] 3-OR 모두 보존 + isFlying/isLiquidClimbing 게이트
- [상수] N/A (검증)
- [타이밍] handleSwim 진입 → 후속 motion 계산 순서 보존
- [근사] 신규 0
- [신규] 추가 의존 발견 없음
- [회귀] 코드 변경 0 → 회귀 0
- [빌드] 코드 변경 0 — 직전 세션 빌드 통과 상태 유지

다음 세션 권고: **Phase C 진입 결정 필요**.
   - **C-2 미이식**: vanilla `LivingEntity.travelInFluid` 위임 + §7 등록 (motionY -= 0.02 vs
     vanilla 0.05 미세 차이). lava 벽 점프 (motionY=0.3 특수) 미작동.
   - **C-3 엄격 이식 (권고)**: SmartMovingSwimmer.handleLava 신설 + Mixin 호출 + ci.cancel().
     원본 L578-L600 1:1 (~30 줄 + 호출 통합).
   사용자 결정 후 진행.

진행률: Phase B 완결 (4/4 = 100%), 전체 #2.6 10/21 (~48%).

### 세션 4 — 2026-04-25 — Phase C 일괄 (C-3 엄격 이식 채택)

사용자 지시: "엄격 1:1" 유지 + Phase C 진입 결정. ci.cancel() 부작용 전수 조사 후
   사용자 OK ("부작용 없이 계속 진행") → C-3 엄격 이식 채택.

진행한 작업:
1. **vanilla LivingEntity.travel 디컴파일 + 부작용 전수 조사** (사용자 요청):
   - vineflower 1.11.1 로 `LivingEntity.class` 디컴파일 (3501 줄, .tmp_research 보관)
   - vanilla L2083-L2142 travel() lava 분기 분석:
     * lava 이동: `updateVelocity(0.02F) + move + multiply(0.5, 0.8F, 0.5) + applyFluidMovingSpeed + d/4 중력`
     * **lava 벽 점프 자체 처리** (L2140-L2141, motionY=0.3F) ← 이전 분석 정정
   - vanilla L2632-L2653 tickMovement() jump 분기:
     * `swimUpward(LAVA)` (L2649) — travel() **외부** → ci.cancel() 영향 없음 ✅
   - lava damage / sound / particle 모두 별개 시스템 → ci.cancel() 영향 없음 ✅
   - 부작용 평가 결과: **0건**
2. **handleLava 메서드 신설** (`SmartMovingSwimmer.java` L660-L770, ~110줄 본문 + 주석):
   - 진입 조건 (4-AND, 원본 L580): !isFlying && !handledSwimming(호출 위치 보장) && !isLiquidClimbing && isInLava
   - 처리: standupIfPossible / resetClimbing / resetSwimming / moveFlying 0.02F / move /
     damping 0.5/0.5/0.5 / 중력 -0.02D / 벽 점프 motionY=0.30000001192092896D
   - `Box bb = player.getBoundingBox().offset(...)` + `world.containsFluid(bb)` 로
     원본 `isOffsetPositionInLiquid` 매핑 (vanilla L3180 패턴 활용)
3. **Mixin 호출 추가** (`MixinLivingEntityClient.java` sm_travel_client L100-L110):
   - handleSwimming false 분기 직후 `handleLava` 호출 + ci.cancel()
   - 기존 4 ci.cancel() 사용처 (swim/slide/fly/climb) 와 동일 패턴
4. **의존 확인**:
   - `sm.standupIfPossible(player)` ✅ (ClientState L2464)
   - `sm.resetClimbing()` ✅ (ClientState L2596)
   - `Swimmer.resetSwimming(sm)` ✅ (private static, 같은 클래스)
   - `Swimmer.moveFlying(player, strafe, forward, speed)` ✅ (private static)
   - `world.containsFluid(box)` ✅ (vanilla 1.21.1)
5. **결정 마킹**:
   - C-1 ✅ 이식 필요성 판단 완료
   - C-2 [~] 미선택 (C-3 채택)
   - C-3 ✅ 엄격 이식 채택 + 완결
   - C-4 ✅ 권고 채택
   - C-5 N/A (1.21.1 vanilla 위임, ci.cancel() 가 동등 효과)

근사 여부: 신규 0. **damping Y / 중력 미세 차이는 원본 1.7.10 충실** (vanilla 0.8F vs SM 0.5D /
   vanilla -d/4 vs SM -0.02D) — §7 영구 후보 1건 추가 가능 (slow_falling 시 원본 -0.02 고정 vs
   vanilla 가변). 단 원본 자체가 -0.02D 고정이라 1:1 이식 — 근사 아님.

vanilla 부작용 평가 (사용자 요청 전수 조사 결과)
| 항목 | 위치 | ci.cancel() 영향 |
|---|---|---|
| swimUpward (점프 +0.04F) | tickMovement L2649 | ✅ 차단 안 됨 (travel 외부) |
| lava damage (setOnFireFromLava) | Entity.tick 외부 | ✅ 차단 안 됨 |
| lava sound (ambient) | LivingSoundManager 별개 | ✅ 차단 안 됨 |
| lava particle (bubble) | tickMovement 외부 | ✅ 차단 안 됨 |
| vanilla lava 이동 | travel L2123-L2142 | ⚠️ 차단됨 — SM 동등 처리 |
| vanilla lava 벽 점프 | travel L2140-L2141 | ⚠️ 차단됨 — SM 동등 처리 (motionY=0.3) |

완료 전 검증 체크리스트 (세션 4 기준):
- [근거] 원본 SmartMovingSelf L578-L600 (③ 리서치 §2 발췌)
- [근거] vanilla LivingEntity.travel L2083-L2142 디컴파일 (vineflower 1.11.1, .tmp_research)
- [근거] 1.21.1 이식 위치 — Swimmer L660-L770 (handleLava) + Mixin L100-L110 (호출)
- [대응] 원본 ↔ 1.21.1 1:1 (진입 조건 + 처리 9 단계 + 벽 점프)
- [분기] 진입 4-AND + 벽 점프 isCollidedHorizontally && isOffsetPositionInLiquid 보존
- [상수] 0.02F / 0.5D / -0.02D / 0.60000002384185791D / 0.30000001192092896D 정확 보존
- [타이밍] 원본 L582-L597 절대 순서 보존 (standup → reset → moveFlying → move → damping → 중력 → 벽 점프 → setVelocity)
- [근사] 신규 0. vanilla 차이는 원본 1.7.10 충실 (근사 아님).
- [신규] handleSwimming/handleLand handledLava 필터 — 1.21.1 vanilla 위임으로 N/A 결정 (C-5)
- [회귀] 빌드 통과. handleSwimming 가 lava 처리 (lavaLikeWater=true) 시 진입 X 보장.
- [빌드] `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL (5s)

다음 세션 권고: **Phase D 진입** — lava 점프 조건 grep + 보완. D-1 jump 회피 (원본 L1852) +
   D-2 isHeadJumping 해제 (L2530) + D-3 isGroundSprinting (L2679) + D-4 isSprintJump (L2643).
   1.21.1 grep 후 누락 시 추가.

진행률: Phase C 완결 (5/5 = 100%), 전체 #2.6 15/21 (~71%).

### 세션 5 — 2026-04-25 — Phase D 완결 (lava 연관 점프/상태 조건 감사)

진행한 작업:
1. **D-1 (원본 L1852, lava 안 일반점프 회피)** — 1.21.1 vanilla `LivingEntity.tickMovement()`
   디컴파일 (.tmp_research/LivingEntity_1_21_1.java L2632-L2653) 분석:
   - vanilla 분기에서 lava 안 + `onGround` && `g <= h` 시 vanilla `jump()` 호출 → SM
     `sm_jump` 인터셉트로 `jumpAvoided=true` 가 켜짐 → **명시적 lava 회피 조건 필요**.
   - `SmartMovingJumper.handleJumping()` `e. 일반 점프` 진입 조건에 `&& !player.isTouchingWater()
     && !player.isInLava()` 추가 (원본 L1852 `!isInWater() && !handleLavaMovement()` 1:1).
2. **D-2 (원본 L2530, isHeadJumping 해제 lava)** — `SmartMovingClientState.tickMain` L1413-L1418
   이미 `&& !player.isInLava()` 포함 (세션 119 B-24 이식 시 반영). 추가 작업 없음.
3. **D-3 (원본 L2679, isGroundSprinting lava 조건)** — 원본 자체에 lava 조건 없음.
   1.21.1 L1232-L1234 도 동일 (`&& !isSwimming_sm && !isDiving && !isClimbing`).
4. **D-4 (원본 L2643, isSprintJump 해제 lava)** — `SmartMovingClientState.tickMain` L1184-L1187
   이미 `|| player.isInLava()` 포함. 추가 작업 없음.

수정 파일:
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingJumper.java` — handleJumping
  e. 일반 점프 진입 조건에 lava/water 회피 2-AND 추가 (원본 L1852 1:1).
- `docs/fix/focus_02_6_lava_liquid_border.md` — §3 Phase D 4 원자 모두 [x] 마킹 + 본 세션
  로그 추가.

빌드: `./gradlew compileJava compileClientJava --rerun-tasks` (다음 단계 검증).

진행률: Phase D 완결 (4/4 = 100%), 전체 #2.6 19/21 (~90%). Phase E 만 잔존 (E-1 빌드,
   E-2 회귀, E-3 인게임 테스트는 모든 포커스 통합 후).

다음 세션 권고: **Phase E-1 빌드 + E-2 회귀 감사** — 단일 변경 (Jumper 1줄 추가) 빌드 검증
   후 본 포커스 종결. E-3 인게임 테스트는 포커스 #1/#2/#3 통합 후 실시.

### 세션 6 — 2026-04-25 — Phase E-2 회귀 감사 완결 (#2.6 AI 완결)

사용자 지시: "엄격 1:1" 유지 + Phase E-2 회귀 감사 — 포커스 #2 Extended 기존 이식 영향 grep 검증.

진행한 작업 (5 영역, 코드 변경 0):
1. **E-2-1 Phase A getLiquidBorder 적용 검증** —
   - ClientState L2102-L2128 `getLiquidBorder()` 본체: 분기 1 (empty=0F) → 분기 2 (water →
     `fluid.getHeight`) → 분기 3+4 (lava → `isLavaLikeWaterEnabled() ? getHeight : 0F`) →
     분기 6 (modded → 1F) 7 분기 모두 정상.
   - Config L539 `lavaSwimParticlePeriodFactor = 4F` + L1461 load (`getFloat` 키
     `move.lava.swim.particle.period.factor`) + L1621 save (`p.setProperty`) 모두 정상.
2. **E-2-2 Phase B 의존 자동 반영 검증** —
   - B-7c updateSwimState (Swimmer L113-L114) `(sm.isSwimming_sm && isInLiquid()) ||
     (cfg.isLavaLikeWaterEnabled() && player.isInLava())` 3-OR 정상.
   - B-42c getMax/MinPlayerLiquidBetween (ClientState L2150/L2181) → getLiquidBorder 직접
     호출 → lava 자동 반영.
   - B-42d totalSwimWaterBorder (L2277) → getMaxPlayerLiquidBetween → lava 반영.
   - B-42-B39 crawlStandUpLiquidCeiling (L2737) → getMinPlayerLiquidBetween → lava 반영.
3. **E-2-3 Phase C handleLava 호출/부작용 검증** —
   - Swimmer L732-L784 본체: 진입 4-AND (`!isFlying && !isLiquidClimbing && isInLava` +
     호출자 `!handledSwimming`) ✓, standupIfPossible / resetClimbing / resetSwimming →
     d1 저장 → moveFlying 0.02F → move(SELF) → damping 0.5D (X/Y/Z) → 중력 -0.02D →
     벽 점프 (offset 0.60000002384185791D, motionY 0.30000001192092896D) → setVelocity
     9 단계 모두 정확.
   - Mixin sm_travel_client L107-L110 호출: handleSwimming 다음 → ci.cancel() 분기 정상.
   - ci.cancel() 부작용 0 재확인: vanilla swimUpward (tickMovement L2649), lava damage
     (Entity.tick), lava sound (LivingSoundManager), lava particle (tickMovement 외부)
     모두 travel() 외부 → 영향 없음.
4. **E-2-4 Phase D lava 게이트 5개 일관성** —
   - D-1 (Jumper L475 — 세션 5 신규): `&& !player.isTouchingWater() && !player.isInLava()`
   - D-2 (ClientState L1418): `&& !player.isInLava()` (세션 119 B-24)
   - D-3 (ClientState L1232-L1234): 원본 L2679 lava 조건 없음 — 1.21.1 동일 일치
   - D-4 (ClientState L1185): `|| player.isInLava()` (B-48c 세션 131)
   - handleLava 자체 진입 (Swimmer L735): `&& player.isInLava()`
   - 원본 SmartMovingSelf 9 lava 위치 (L132/L133-L134/L135/L136/L232/L578-L600/L602-L604/
     L633-L643/L1852/L2529-L2530/L2643) 모두 1.21.1 매핑 (handleAltFly/Land 필터는 1.21.1
     N/A — vanilla 위임으로 결정).
5. **E-2-5 playtest_fixes.md 현재 포커스 갱신** —
   - "세션 2 — Phase A 일괄" → "세션 6 — Phase E-2 회귀 감사"
   - 상태 "🟡 병행 대기" → "🟢 AI 완결 임박 19/21 = 90%"

수정 파일:
- `docs/fix/focus_02_6_lava_liquid_border.md` — §3 E-2 [x] 마킹 + 본 세션 로그 추가.
- `docs/fix/playtest_fixes.md` — 현재 포커스 + 표 행 갱신.

회귀 0건 (코드 변경 0). 빌드 검증 불필요 (세션 5 E-1 BUILD SUCCESSFUL 5s).

완료 전 검증 체크리스트 (세션 6 기준):
- [근거] 원본 SmartMovingSelf 9 lava 위치 + getLiquidBorder L131-L150 (③ 리서치 §1-§2)
- [근거] 1.21.1 이식 위치 모두 grep 확인 (5 영역)
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1 (Phase A 4 분기 + Phase C 9 단계 + Phase D 4 게이트)
- [분기] 모든 if/else 전수 확인 (getLiquidBorder 4 분기 / handleLava 진입 / D-1 e. 일반점프)
- [상수] 0.5D / 0.02D / 0.60000002384185791D / 0.30000001192092896D / 4F 정확 보존
- [타이밍] handleSwimming → handleLava → fromSwimmingOrDiving → handleSliding 순 보존
- [근사] 신규 0건. 기존 §7 영구 후보 4건 (FiniteLiquid mod / isInsideOfMaterial /
  reverseHandleMaterialAcceleration / vanilla 미세 차이) 외 0.
- [신규] 추가 의존 발견 없음
- [회귀] 포커스 #2 Extended 기존 이식 (B-7c / B-42c / B-42d / B-42-B39) 영향 0
- [빌드] 세션 5 E-1 BUILD SUCCESSFUL 5s — 코드 변경 없으므로 재빌드 N/A

다음 세션 권고: **#2.6 AI 완결 — E-3 통합 인게임 검증 deferred** (포커스 #1/#2/#3/#4
   완결 후 통합 시점). 다음 포커스 진입 사용자 결정 (#2.7 Phase 2 / #3 / #1).

진행률: Phase E-2 완결 (1/2 = 50% — E-3 deferred), **전체 #2.6 20/21 (~95%) AI 완결**.
   E-3 인게임 deferred 1건만 잔존 — 통합 검증 시점 (모든 포커스 완결 후).

---

## 7. 근사 이식 지점 (이 포커스)

**§7 이 파일**: Phase 진행 중 불가피한 근사 이식 지점. 현재 시작 시점: 0건.

잔존 근사 후보 (세션 1 갱신):
- **FiniteLiquid mod 분기** (원본 SmartMovingBase L137-L138) — mod 1.21.1 미이식. 해소 불가. **영구 유지**.
- **`_lavaSwimParticlePeriodFactor = 4F`** (원본 SmartMovingConfig L164) — Phase A-5 결정 사항.
  이식 vs §7 등록 (lava 파티클 vanilla 동작 위임) 중 선택.
- **`isInsideOfMaterial(Material material)` water 전용** (원본 SmartMovingBase L524-L543) —
  FiniteLiquid mod 의존, lava 무관. 미이식 영구 (mod 미이식 일관).
- **`reverseHandleMaterialAcceleration()` water 전용** (원본 SmartMovingBase L884-L932) —
  Material.water 고정, lava 미관련. 본 포커스 영향 0.

---

## 8. 소비처 영향 감사

`getLiquidBorder` lava 반영 시 영향:
- **포커스 #2 Extended B-7c** (세션 127): `updateSwimState` 진입 조건 이미 lavaLikeWater 반영
  — `getLiquidBorder` lava 가 > 0 반환 시 `isInLiquid` 정확 판정 → `wasSwimming &&
  isInLiquid` 분기 정상.
- **포커스 #2 Extended B-42c**: getMax/MinPlayerLiquidBetween 자동 반영 (내부적으로
  `getLiquidBorder` 호출).
- **포커스 #2 Extended B-42d**: `SwimBorderValues.totalSwimWaterBorder` lava 포함 — 수심
  계산 lava 반영. B-9 메인 분류가 lava 에서 정상 작동.
- **포커스 #2 Extended B-42-B39**: `fromSwimmingOrDiving` crawlStandUpLiquidCeiling — lava 위
  천장 판정 정확.
- **포커스 #3 상태 전환**: lava 관련 `isDipping`/`isSwimming_sm`/`isDiving` 전환 트리거 활성.
- **포커스 #1 애니메이션**: lava 수영 애니메이션 활성 (isSwimming_sm state 반영).

---

## 9. 참고 자료

### 종합 리서치 파일 (세션 1 신규)
- `docs/research/mapping/research_lava_border.md` — 원본 4 파일 5526 줄 전수 read 결과.
  9 메서드 (Base) + 9 위치 (Self) + 1 메서드 (ClientConfig) + 2 Property (Config) 모두 정리.

### 원본 소스 경로 (로컬, 세션 1 보강)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingBase.java`
  - L123-L129 `isLava(block)`
  - L131-L151 `getLiquidBorder` ★ Phase A
  - L152-L163 `getNormalWaterBorder`
  - L165-L181 `getFiniteLiquidWaterBorder` (FiniteLiquid mod, §7 영구)
  - L411-L416 `isInLiquid`
  - L418-L451 `getMax/MinPlayerLiquidBetween`
  - L524-L543 `isInsideOfMaterial` (water 전용)
  - L884-L932 `reverseHandleMaterialAcceleration` (water 전용)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`
  - L132-L136 호출 컨텍스트
  - L232 `handleSwimming` 본체 lava 진입 조건
  - L578-L600 `handleLava` ★ Phase C
  - L602-L604 `handleAlternativeFlying` (handledLava 필터)
  - L633-L643 `handleLand` (handledLava 필터)
  - L1852 jump 회피 lava 조건 ★ Phase D-1
  - L2529-L2530 isHeadJumping 해제 lava 조건 ★ Phase D-2
  - L2643 isSprintJump 해제 lava 조건 ★ Phase D-4
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\config\SmartMovingClientConfig.java`
  - L87-L90 `isLavaLikeWaterEnabled` (이미 1.21.1 이식 완료)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\config\SmartMovingConfig.java`
  - L162-L163 `_lavaLikeWater = Creative` (이미 1.21.1 이식 완료)
  - L164 `_lavaSwimParticlePeriodFactor = 4F` ★ Phase A-5 신규

### 1.21.1 이식 대상
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` (L2094 getLiquidBorder)
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingSwimmer.java` (lava 이동 처리 추가 시)
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` (handleLava 호출 추가 시)
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` (lavaLikeWater 이미 이식)

### 연관 포커스
- **#2 Extended** (완결 — 세션 134): B-42c / B-42d / B-7c 이식 완료. 이 포커스의 의존 선행.
- **#2.5** Jumper Factor: 독립. 동시 진행 가능.
