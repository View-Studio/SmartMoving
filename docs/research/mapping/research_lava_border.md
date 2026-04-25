# Research — Lava Liquid Border 전수 (포커스 #2.6)

> **목적**: 포커스 #2.6 (Lava Liquid Border) 의 1:1 이식 근거 자료. 원본 4 파일
> (`SmartMovingBase.java` 932줄 / `SmartMovingSelf.java` 3345줄 /
> `SmartMovingClientConfig.java` 595줄 / `SmartMovingConfig.java` 654줄, 총 5526줄)
> 을 처음부터 끝까지 전수 read 후 lava/liquid 관련 모든 라인 + 컨텍스트 추출.
> 해석/요약 금지 — 원본 Java 코드 그대로.
>
> **방법**: 4 Agent 병렬 위임 (포커스 #2.6 세션 1, 2026-04-25). grep 키워드:
> `lava|Lava|LAVA|liquid|Liquid|getLiquidBorder|getNormalWaterBorder|
> getFiniteLiquidWaterBorder|handleLava|handleLavaMovement|isInLava|
> isInLiquid|lavaLikeWater|isLavaLikeWaterEnabled|Material.lava|
> Material.water|material.isLiquid|Block.getBlockFromName|isLava`.

---

## 1. SmartMovingBase.java (932줄, 9 메서드)

### L123-L129 — `isLava(Block block)` 헬퍼
```java
@SuppressWarnings("static-method")
protected boolean isLava(Block block)
{
    if(block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return true;
    return block != null && block.getMaterial() == Material.lava;
}
```
**목적**: lava 블록 직접 체크. `flowing_lava` 포함. Material 기반 fallback.
**1.21.1 매핑**: `world.getFluidState(pos).isIn(FluidTags.LAVA)` (FluidState 기반).

---

### L131-L150 — `getLiquidBorder(int i, int j, int k)` ★ Phase A 핵심
```java
protected float getLiquidBorder(int i, int j, int k)
{
    float finiteLiquidBorder;
    Block block = sp.worldObj.getBlock(i, j, k);
    if(block == Block.getBlockFromName("water") || block == Block.getBlockFromName("flowing_water"))
        return getNormalWaterBorder(i, j, k);
    if(SmartMovingOptions.hasFiniteLiquid && (finiteLiquidBorder = getFiniteLiquidWaterBorder(i, j, k, block)) > 0)
        return finiteLiquidBorder;
    if(block == Block.getBlockFromName("lava") || block == Block.getBlockFromName("flowing_lava"))
        return Config._lavaLikeWater.value ? getNormalWaterBorder(i, j, k) : 0F;

    Material material = sp.worldObj.getBlock(i, j, k).getMaterial();
    if(material == null || material == Material.lava)
        return Config._lavaLikeWater.value ? 1F : 0F;
    if(material == Material.water)
        return getNormalWaterBorder(i, j, k);
    if(material.isLiquid())
        return 1F;

    return 0F;
}
```

**분기 트리 (절대 순서)**:
1. **L135-L136**: `water || flowing_water` → `getNormalWaterBorder(i, j, k)`
2. **L137-L138**: `hasFiniteLiquid && getFiniteLiquidWaterBorder() > 0` → `finiteLiquidBorder`
3. **L139-L140**: `lava || flowing_lava` → `_lavaLikeWater ? getNormalWaterBorder(i, j, k) : 0F`
4. **L142-L144**: `Material.lava || material == null` → `_lavaLikeWater ? 1F : 0F`
5. **L145-L146**: `Material.water` → `getNormalWaterBorder(i, j, k)`
6. **L147-L148**: `material.isLiquid()` → `1F` (modded liquid)
7. **L150**: 기본 `0F`

**상수**: `1F` (꽉찬 fluid) / `0F` (없음).

**1.21.1 매핑 (Phase A 이식 대상)**:
```java
// 분기 1: water → fluid.getHeight(world, pos)
// 분기 2: FiniteLiquid mod → §7 미이식 (mod 없음)
// 분기 3-4 통합: lava → cfg.isLavaLikeWaterEnabled() ? fluid.getHeight(world, pos) : 0F
// 분기 5: water Material → 분기 1 과 동일 → 통합
// 분기 6: 그 외 fluid → 1F
// 분기 7: 0F
```

---

### L152-L163 — `getNormalWaterBorder(int i, int j, int k)`
```java
protected float getNormalWaterBorder(int i, int j, int k)
{
    int blockMetaData = sp.worldObj.getBlockMetadata(i, j, k);
    if(blockMetaData >= 8)
        return 1F;
    if(blockMetaData == 0)
        if(sp.worldObj.isAirBlock(i, j + 1, k))
            return 0.8875F;
        else
            return 1F;
    return (8 - blockMetaData) / 8F;
}
```

**분기**:
- `metadata >= 8` → `1F` (가득)
- `metadata == 0 && 위 air` → `0.8875F` (가득 source 의 표면 보정 — height level 7/8 + small offset)
- `metadata == 0 && 위 not air` → `1F`
- 그 외 → `(8 - metadata) / 8F` (level 1~7 의 높이 0.875 ~ 0.125)

**상수**: `0.8875F` (특수 source 표면), `8F` 분모.

**1.21.1 매핑**: vanilla `FluidState.getHeight(world, pos)` 가 동일 시멘틱 (level + falling 보정).
`FlowableFluid.getHeight` 가 metadata 대신 `level` property 사용 — 결과는 동치.

---

### L165-L181 — `getFiniteLiquidWaterBorder(int i, int j, int k, Block block)`
```java
protected float getFiniteLiquidWaterBorder(int i, int j, int k, Block block)
{
    int type;
    if((type = Orientation.getFiniteLiquidWater(block)) > 0)
    {
        if(type == 2)
            return 1F;
        if(type == 1)
        {
            Block aboveBlock = sp.worldObj.getBlock(i, j + 1, k);
            if(Orientation.getFiniteLiquidWater(aboveBlock) > 0)
                return 1F;
            return (sp.worldObj.getBlockMetadata(i, j, k) + 1) / 16F;
        }
    }
    return 0F;
}
```

**1.21.1 매핑**: FiniteLiquid mod 미이식 → `§7 영구 근사`. 메서드 자체 이식 불필요.

---

### L411-L416 — `isInLiquid()`
```java
protected boolean isInLiquid()
{
    return
        getMaxPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.minY ||
        getMinPlayerLiquidBetween(sp.boundingBox.minY, sp.boundingBox.maxY) != sp.boundingBox.maxY;
}
```

**의미**: `getMax > minY` (최상위 fluid 가 minY 보다 위) **OR** `getMin < maxY` (최하위 fluid 가 maxY 보다 아래).
즉 플레이어 boundingBox 내 fluid 존재 시 true.

**1.21.1**: 이미 `ClientState.isInLiquid(player)` 로 이식됨 (focus_02_6 §0).
Phase A `getLiquidBorder` lava 반영 후 자동으로 lava 도 포함.

---

### L418-L432 — `getMaxPlayerLiquidBetween(double yMin, double yMax)`
```java
protected double getMaxPlayerLiquidBetween(double yMin, double yMax)
{
    int i = MathHelper.floor_double(sp.posX);
    int jMin = MathHelper.floor_double(yMin);
    int jMax = MathHelper.floor_double(yMax);
    int k = MathHelper.floor_double(sp.posZ);

    for(int j = jMax; j >= jMin; j--)
    {
        float swimWaterBorder = getLiquidBorder(i, j, k);
        if(swimWaterBorder > 0)
            return j + swimWaterBorder;
    }
    return yMin;
}
```

**의미**: yMax → yMin 위에서 아래로 스캔. 첫 fluid 만나면 `j + border` 반환.
**1.21.1**: 이미 이식됨. Phase A 후 lava 자동 반영.

---

### L434-L451 — `getMinPlayerLiquidBetween(double yMin, double yMax)`
```java
protected double getMinPlayerLiquidBetween(double yMin, double yMax)
{
    int i = MathHelper.floor_double(sp.posX);
    int jMin = MathHelper.floor_double(yMin);
    int jMax = MathHelper.floor_double(yMax);
    int k = MathHelper.floor_double(sp.posZ);

    for(int j = jMin; j <= jMax; j++)
    {
        float swimWaterBorder = getLiquidBorder(i, j, k);
        if(swimWaterBorder > 0)
            if(j > yMin)
                return j;
            else if(j + swimWaterBorder > yMin)
                return yMin;
    }
    return yMax;
}
```

**의미**: yMin → yMax 아래에서 위로 스캔. 첫 fluid 만나면 분기:
- `j > yMin` → `j` 반환
- `j == yMin && j+border > yMin` → `yMin` 반환

**1.21.1**: 이미 이식됨.

---

### L524-L543 — `isInsideOfMaterial(Material material)` (water 전용)
```java
public boolean isInsideOfMaterial(Material material)
{
    if(SmartMovingOptions.hasFiniteLiquid && material == Material.water)
    {
        double d = sp.posY + sp.getEyeHeight();
        int i = MathHelper.floor_double(sp.posX);
        int j = MathHelper.floor_float(MathHelper.floor_double(d));
        int k = MathHelper.floor_double(sp.posZ);
        Block l = sp.worldObj.getBlock(i, j, k);
        float border;
        if(l != null && (border = getFiniteLiquidWaterBorder(i, j, k, l)) > 0)
        {
            float f = (1 - border) - 0.1111111F;
            float f1 = (j + 1) - f;
            return d < f1;
        }
        return false;
    }
    return isp.localIsInsideOfMaterial(material);
}
```

**lava 무관**: `material == Material.water` 분기만. lava 호출 시 vanilla `localIsInsideOfMaterial` 위임.
**1.21.1**: FiniteLiquid mod 미이식 → 본 메서드 이식 불필요.

---

### L884-L932 — `reverseHandleMaterialAcceleration()` (water 전용)
```java
public void reverseHandleMaterialAcceleration()
{
    AxisAlignedBB axisalignedbb = sp.boundingBox.expand(0.0D, -0.40000000596046448D, 0.0D).contract(0.001D, 0.001D, 0.001D);
    Material material = Material.water;   // ★ water 고정
    Entity entity = sp;
    // ... bb 영역 내 water 블록 순회
    // ... block.velocityToAddToEntity 누적
    if(vec3d.lengthVector() > 0.0D)
    {
        vec3d = vec3d.normalize();
        double d = -0.014D; // instead +0.014D for reversal
        entity.motionX += vec3d.xCoord * d;
        entity.motionY += vec3d.yCoord * d;
        entity.motionZ += vec3d.zCoord * d;
    }
}
```

**lava 무관**: `Material material = Material.water` 고정. lava 가속 역방향 처리 안 함 (의도).
**1.21.1**: lava 미관련. 이식 불필요.

---

## 2. SmartMovingSelf.java (3345줄, 9 lava 위치)

### L132 — `isLiquidClimbing` 정의 (lava 무관 — 컨텍스트만)
```java
boolean isLiquidClimbing = Config.isFreeClimbingEnabled() && sp.fallDistance <= 3.0
                        && wantClimbUp && sp.isCollidedHorizontally && !isDiving;
```
**lava 조건 없음**. `handleLava` 호출 시 negate 인자로 사용.

---

### L133-L134 — `handleSwimming` / `handleLava` 호출 ★
```java
boolean handledSwimming = handleSwimming(moveForward, moveStrafing, speedFactor,
                                          wasSwimming, wasDiving, isLiquidClimbing,
                                          wasJumpingOutOfWater);
boolean handledLava = handleLava(moveForward, moveStrafing, handledSwimming, isLiquidClimbing);
```

**호출 순서**: `handleSwimming` → `handleLava` (handledSwimming 결과 전달).

---

### L135 — `handleAlternativeFlying(handledLava)` 호출
```java
boolean handledAlternativeFlying = handleAlternativeFlying(moveForward, moveStrafing,
                                                            speedFactor, handledSwimming,
                                                            handledLava);
```

---

### L136 — `handleLand(handledLava)` 호출
```java
handleLand(moveForward, moveStrafing, speedFactor, handledSwimming, handledLava,
           handledAlternativeFlying, wasShortInWater, wasClimbing, wasCeilingClimbing);
```

---

### L232 — `handleSwimming` 메서드 본체 lava 진입 조건
```java
boolean handleSwimming = !isFlying && !isLiquidClimbing
                      && (sp.isInWater()
                          || (wasSwimming && isInLiquid())
                          || (Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()));
```

**3-OR 조건**:
1. `sp.isInWater()` — vanilla 물
2. `wasSwimming && isInLiquid()` — 이전 틱 수영 + 현재 fluid 존재
3. `Config.isLavaLikeWaterEnabled() && sp.handleLavaMovement()` — lava 활성화 + lava 안

**1.21.1 이식 상태**: `SmartMovingSwimmer.updateSwimState` L98-L114 (B-7c, 세션 127) 이식됨.
**Phase B 검증 필요**: 정확히 같은 3-OR 조건인지.

---

### L578-L600 — `handleLava` 본체 ★★ Phase C 이식 대상
```java
private boolean handleLava(float moveForward, float moveStrafing,
                           boolean handledSwimming, boolean isLiquidClimbing)
{
    boolean handleLava = !isFlying && !handledSwimming && !isLiquidClimbing
                      && sp.handleLavaMovement();
    if(handleLava)
    {
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
        if(sp.isCollidedHorizontally
                && sp.isOffsetPositionInLiquid(sp.motionX,
                    ((sp.motionY + 0.60000002384185791D) - sp.posY) + d1, sp.motionZ))
        {
            sp.motionY = 0.30000001192092896D;
        }
    }
    return handleLava;
}
```

**진입 조건 (4-AND)**:
- `!isFlying` (SM 비행 X)
- `!handledSwimming` (수영 처리 X)
- `!isLiquidClimbing` (액체 클라이밍 X)
- `sp.handleLavaMovement()` (lava 안)

**처리 (진입 시)**:
1. `standupIfPossible()` (heightOffset >= 0 확인 후 -1F 인 경우 복원)
2. `resetClimbing()` (클라이밍 상태 리셋)
3. `resetSwimming()` (수영 상태 리셋)
4. `d1 = sp.posY` (충돌 체크용 prev posY 저장)
5. `sp.moveFlying(strafe, forward, 0.02F)` — fly speed 0.02F (vanilla `applyFluidMovingSpeed` 와 동치)
6. `sp.moveEntity(motionX, motionY, motionZ)` — 실 이동
7. damping 0.5: `motionX/Y/Z *= 0.5D`
8. 중력: `motionY -= 0.02D`
9. **lava 벽 점프**: `isCollidedHorizontally + isOffsetPositionInLiquid(...)` → `motionY = 0.30000001192092896D`

**상수 (정확한 D 보존 필수)**:
- `0.02F` (moveFlying speed)
- `0.5D` (damping)
- `0.02D` (중력)
- `0.60000002384185791D` (벽 점프 offset)
- `0.30000001192092896D` (벽 점프 motionY)

**vanilla 차이**:
- vanilla 1.21.1 `LivingEntity.travelInFluid` 도 lava 처리 (`applyFluidMovingSpeed` 0.02 + damping 0.5)
- 단 원본 `motionY -= 0.02D` (중력 감쇠) ↔ vanilla `motionY -= 0.05` 또는 fluid 별 처리
- **벽 점프** (`motionY = 0.3`) 는 vanilla 에 없음 — 원본 SM 자체 처리

**1.21.1 이식 위치 (Phase C-3)**:
- `SmartMovingSwimmer.handleLava(player, sm, movementInput)` 별도 메서드
- `MixinLivingEntityClient.sm_beforeTravel` 에서 호출 + `ci.cancel()` (vanilla travel 취소)

---

### L602-L604 — `handleAlternativeFlying` 시그니처 (handledLava 필터)
```java
private boolean handleAlternativeFlying(float moveForward, float moveStrafing,
                                        float speedFactor, boolean handledSwimming,
                                        boolean handledLava)
{
    boolean handleAlternativeFlying = !handledSwimming && !handledLava
                                   && sp.capabilities.isFlying && Config.isFlyingEnabled();
```

**필터**: `!handledLava` — lava 처리됨 → 대안 비행 불가.
**1.21.1 영향**: Phase C 이식 시 `handleAlternativeFlying` 호출처에서 `handledLava` 결과 반영.

---

### L633-L643 — `handleLand` 시그니처 (handledLava 필터)
```java
private void handleLand(float moveForward, float moveStrafing, float speedFactor,
                        boolean handledSwimming, boolean handledLava,
                        boolean handledAlternativeFlying, boolean wasShortInWater,
                        boolean wasClimbing, boolean wasCeilingClimbing)
{
    ...
    if(!handledSwimming && !handledLava && !handledAlternativeFlying)
    {
        resetSwimming();
        ...
```

**필터**: `!handledLava` — lava 처리됨 → 육지 처리 스킵.

---

### L1852 — `handleJumping` 점프 회피 lava 조건
```java
boolean jump = jumpAvoided && isp.getIsJumpingField()
            && !sp.isInWater() && !sp.handleLavaMovement();
```

**조건**: `!sp.handleLavaMovement()` → **lava 진입 시 점프 불가**.
**1.21.1 Phase D-1 감사 대상**.

---

### L2529-L2530 — `isHeadJumping` 해제 lava 조건
```java
wasHeadJumping = isHeadJumping;
isHeadJumping = isHeadJumping
              && !sp.onGround
              && !(isSwimming || isDiving)
              && !(isFlying || sp.capabilities.isFlying)
              && !(sp.handleWaterMovement() && sp.motionY < 0)
              && !sp.handleLavaMovement();
```

**조건**: `!sp.handleLavaMovement()` — **lava 진입 시 헤드점프 자동 해제**.
**1.21.1 Phase D-2 감사 대상**.

---

### L2643 — `isSprintJump` 해제 lava 조건
```java
if(sp.onGround || isFlying || sp.capabilities.isFlying || isSwimming || isDiving
   || sp.handleLavaMovement())
    isSprintJump = false;
```

**조건**: `sp.handleLavaMovement()` → **lava 진입 시 isSprintJump 해제**.
**1.21.1 Phase D-4 감사 대상**.

---

## 3. SmartMovingClientConfig.java (595줄, 1 메서드)

### L87-L90 — `isLavaLikeWaterEnabled()`
```java
public boolean isLavaLikeWaterEnabled()
{
    return _lavaLikeWater.value && enabled;
}
```

**의미**: `_lavaLikeWater` 활성 + SM 전체 활성 (`enabled`) 시 true.
**1.21.1**: 이미 이식됨 (`SmartMovingConfig.isLavaLikeWaterEnabled()` L673-L679, focus_02_6 §0).

---

## 4. SmartMovingConfig.java (654줄, 2 Property)

### L162-L163 — `_lavaLikeWater = Creative("move.lava.water")` ★
```java
public final Property<Boolean> _lavaLikeWater = Creative("move.lava.water")
    .comment("To switch on/off swimming and diving in lava")
    .book("Lava", "Below you find all lava movement options");
```

**팩토리 풀어쓰기**: `Modified("move.lava.water").defaults(Value(false).c(true))`.
- **default = false** (Survival/Adventure)
- **`.c(true)` = Creative override = true** (Creative 모드에서만 활성)
- Easy/Hard 난이도 추가 오버라이드 없음

**1.21.1**: 이미 이식됨 (`SmartMovingConfig.lavaLikeWater = false`, focus_02_6 §0).

---

### L164 — `_lavaSwimParticlePeriodFactor = 4F` ⚠️ **focus_02_6 누락 발견**
```java
public final Property<Float> _lavaSwimParticlePeriodFactor =
    PositiveFactor("move.lava.swim.particle.period.factor")
    .defaults(4F)
    .comment("Lava swim particle spawning period factor (>= 0)");
```

**의미**: lava 수영 시 파티클 spawn 주기 배수. default `4F`. 난이도 오버라이드 없음.

**1.21.1 이식 상태**: **미이식**. focus_02_6 §0 / §3 누락 — Phase A 와 함께 추가 권장.

**소비처**: SmartMovingBase / SmartMovingSelf 검색 결과 직접 사용처 0건. 단 SM 의 다른 파일 (예: SmartMovingRender / SmartMovingBaseAccessor) 에서 lava 파티클 생성 시 사용 가능. 1.21.1 이식 시 lava swim 파티클 동작 위해 필요.

---

## 5. 1.21.1 이식 매핑 종합

### 표면 매핑
| 원본 (1.7.10 Forge) | 1.21.1 Fabric |
|---|---|
| `sp.worldObj.getBlock(i, j, k)` | `world.getBlockState(pos).getBlock()` |
| `sp.worldObj.getBlockMetadata(i, j, k)` | `FluidState.getLevel()` (FlowableFluid) |
| `sp.worldObj.isAirBlock(i, j+1, k)` | `world.isAir(pos.up())` |
| `Block.getBlockFromName("water"/"flowing_water")` | `Blocks.WATER` (1.21 통합) |
| `Block.getBlockFromName("lava"/"flowing_lava")` | `Blocks.LAVA` |
| `block.getMaterial()` (Material.water/lava) | `world.getFluidState(pos).isIn(FluidTags.WATER/LAVA)` |
| `material.isLiquid()` | `!fluid.isEmpty()` |
| `sp.handleLavaMovement()` | `player.isInLava()` |
| `sp.handleWaterMovement()` | `player.isTouchingWater()` |
| `sp.isInWater()` | `player.isTouchingWater()` |
| `sp.moveFlying(strafe, forward, speed)` | `SmartMovingSwimmer.moveFlying(...)` (이미 이식) |
| `sp.moveEntity(motionX, motionY, motionZ)` | `player.move(MovementType.SELF, vec3d)` |
| `sp.isOffsetPositionInLiquid(dx, dy, dz)` | `player.wouldCollideAtOffset(offset)` 또는 AABB + fluid 체크 |
| `sp.boundingBox` | `player.getBoundingBox()` |
| `getNormalWaterBorder(i, j, k)` 결과 | `fluid.getHeight(world, pos)` (vanilla FlowableFluid 동치) |

### Phase A 이식 (getLiquidBorder lava + modded)
**원본**: SmartMovingBase L131-L150 (7 분기)
**1.21.1 이식 위치**: `SmartMovingClientState.getLiquidBorder` L2094

**이식 코드 (Phase A-1 + A-2 통합)**:
```java
public float getLiquidBorder(World world, BlockPos pos) {
    SmartMovingConfig cfg = SmartMovingConfig.Config;
    FluidState fluid = world.getFluidState(pos);

    // 분기 1 + 5 통합 (water + Material.water): fluid.getHeight
    if (fluid.isIn(FluidTags.WATER))
        return fluid.getHeight(world, pos);

    // 분기 2: FiniteLiquid mod — §7 영구 근사 (mod 미이식)

    // 분기 3 + 4 통합 (lava + Material.lava): lavaLikeWater 체크
    if (fluid.isIn(FluidTags.LAVA))
        return cfg.isLavaLikeWaterEnabled() ? fluid.getHeight(world, pos) : 0F;

    // 분기 6: modded liquid (material.isLiquid → 1F)
    if (!fluid.isEmpty())
        return 1F;

    // 분기 7: 0F
    return 0F;
}
```

### Phase C 이식 (handleLava SM 자체 처리, C-3 권고)
**원본**: SmartMovingSelf L578-L600
**1.21.1 이식 위치**: `SmartMovingSwimmer.handleLava` 신규 메서드 + `MixinLivingEntityClient.sm_beforeTravel` 에서 호출 + `ci.cancel()`

**이식 코드 골격**:
```java
public static boolean handleLava(ClientPlayerEntity player, SmartMovingClientState sm,
                                  float moveForward, float moveStrafing,
                                  boolean handledSwimming, boolean isLiquidClimbing) {
    boolean handleLava = !sm.isFlying && !handledSwimming && !isLiquidClimbing
                      && player.isInLava();
    if (handleLava) {
        sm.standupIfPossible();
        sm.resetClimbing();
        sm.resetSwimming();

        double d1 = player.getY();
        moveFlying(player, moveStrafing, moveForward, 0.02F);
        Vec3d vel = player.getVelocity();
        player.move(MovementType.SELF, vel);

        Vec3d v2 = player.getVelocity();
        double mx = v2.x * 0.5D;
        double my = v2.y * 0.5D - 0.02D;
        double mz = v2.z * 0.5D;

        // 벽 점프
        if (player.horizontalCollision
                && wouldCollideInLiquid(player, mx, ((my + 0.60000002384185791D) - player.getY()) + d1, mz)) {
            my = 0.30000001192092896D;
        }
        player.setVelocity(mx, my, mz);
    }
    return handleLava;
}
```

**상수 정확 보존**:
- `0.02F` / `0.02D` (moveFlying / 중력)
- `0.5D` (damping)
- `0.60000002384185791D` (벽 점프 offset)
- `0.30000001192092896D` (벽 점프 motionY)

### Phase D 이식 (lava 점프 조건 감사)
| 원본 라인 | 조건 | 1.21.1 grep 대상 |
|---|---|---|
| L1852 | `!sp.handleLavaMovement()` (jump 회피) | `SmartMovingJumper.handleJumping` 또는 `tryJump` 진입 조건 |
| L2530 | `!sp.handleLavaMovement()` (isHeadJumping 해제) | `SmartMovingClientState.tickEssential` isHeadJumping 갱신부 |
| L2643 | `sp.handleLavaMovement()` (isSprintJump 해제) | `SmartMovingClientState.tickEssential` isSprintJump 갱신부 |

---

## 6. focus_02_6 신규 발견 + 보강 항목

### 누락 항목 (focus_02_6 §0/§3 추가 필요)
1. **`_lavaSwimParticlePeriodFactor = 4F`** Config 필드 — Phase A 와 함께 신규 이식 (또는 §7 미이식 등록 — 파티클 시각 효과만 영향)
2. **`isLava(block)` 헬퍼** — 1.21.1 Phase D 점프 조건 검증 시 활용 가능
3. **`handleAlternativeFlying` / `handleLand` 의 `handledLava` 필터** — Phase C 이식 시 호출처에서 결과 반영 필요

### 보강 항목 (focus_02_6 §2 보강)
1. **`isInLiquid()` 정의 (Base L411-L416)**: `getMax > minY || getMin < maxY` — focus_02_6 의 isInLiquid 검증 시 정확한 공식 명시
2. **`getMax/MinPlayerLiquidBetween` 구현 (Base L418-L451)**: 스캔 방향 (Max: 위→아래, Min: 아래→위) 명시
3. **`getNormalWaterBorder` 구현 (Base L152-L163)**: metadata `>= 8 → 1F`, `==0+air → 0.8875F`, `==0+!air → 1F`, 그 외 `(8-meta)/8F` — vanilla FluidState 시멘틱 동치 근거 보강
4. **`reverseHandleMaterialAcceleration` water 전용** (Base L884-L932): lava 미관련 — 본 포커스 영향 0 명시

### Phase 추가 원자 후보
- **Phase A-5**: `_lavaSwimParticlePeriodFactor` 필드 신규 이식 + IO load/save (소형, A-1~A-2 와 함께 처리 가능)
- **Phase C-5**: `handleAlternativeFlying` / `handleLand` 의 handledLava 필터 1.21.1 영향 검증 (사실 1.21.1 에는 이런 메서드 없음 — vanilla 위임. 실제 영향 없음 가능. 검증 필요)
