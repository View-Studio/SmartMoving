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
| `tryJump` / isHeadJumping lava 연동 | SmartMovingSelf L1852 / L2530 | ❌ 확인 필요 |

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

### 2.3 원본 tryJump / isHeadJumping lava 연동
- L1852: `boolean jump = jumpAvoided && isp.getIsJumpingField()
           && !sp.isInWater() && !sp.handleLavaMovement();`
- L2530: `isHeadJumping = isHeadJumping && ...
           && !sp.handleLavaMovement();`
- L2643: `if (sp.onGround || isFlying || capabilities.isFlying
           || isSwimming || isDiving || sp.handleLavaMovement())`

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

**A-1. lava 분기** (원본 L139-L140, L143-L144)
- [ ] A-1. `ClientState.getLiquidBorder` 에 FluidTags.LAVA 분기 추가:
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

**A-2. modded liquid 분기** (원본 L147-L148)
- [ ] A-2. `ClientState.getLiquidBorder` 에 `!fluid.isEmpty() && !water && !lava` 시 `1F` 반환
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

**A-3. 주석 갱신**
- [ ] A-3. `getLiquidBorder` Javadoc §7 근사 B-42c 기록 갱신 (lava + modded 해소).

**A-4. §7 근사 기록 갱신**
- [ ] A-4. `focus_02_state_issues.md` §7 B-42c (2) 해소 완료 기록. FiniteLiquid mod 분기만
  잔존 (mod 미이식 — 해소 불가).

---

### Phase B. `isInLiquid` + `getMax/MinPlayerLiquidBetween` 검증

**B-1. `isInLiquid` 자동 반영 확인**
- [ ] B-1. Phase A 완료 후 `isInLiquid(player)` 는 자동으로 lava + modded liquid 포함. 코드
  수정 불필요. **검증만** — `getLiquidBorder` lava 반환 시 `getMaxPlayerLiquidBetween !=
  minY` 만족 → `isInLiquid = true`.

**B-2. `getMax/MinPlayerLiquidBetween` 자동 반영 확인**
- [ ] B-2. 내부적으로 `getLiquidBorder` 호출. Phase A 완료 후 lava 반영 자동. **검증만**.

**B-3. 소비처 전수 감사**
- [ ] B-3. `isInLiquid` / `getMaxPlayerLiquidBetween` / `getMinPlayerLiquidBetween` 모든
  소비처 검토 — lava 가 water 와 동일 처리되는지:
  - `Swimmer.updateSwimState` L98-L114 (B-7c)
  - `ClientState.fromSwimmingOrDiving` (B-42-B39) — crawlStandUpLiquidCeiling
  - `ClientState.SwimBorderValues` computeSwimBorderValues — totalSwimWaterBorder
  - `handleSwimming` motion 계산 (이미 lavaLikeWater 활성 시 진입)

---

### Phase C. `handleLava` SM 자체 처리 이식 결정

**C-1. 이식 필요성 판단**
- [ ] C-1. 원본 `handleLava` L578-L600 은 `lavaLikeWater=false` 시 lava 에서 SM 자체 이동
  처리 (moveFlying + damping 0.5 + motionY -= 0.02 + 벽 점프 특수). 1.21.1 은 vanilla
  `LivingEntity.travel()` 이 lava 수영을 처리하므로 **이식 불필요 확인** 또는 **vanilla 와
  차이 확인**.
  - vanilla 1.21.1 lava 이동: `LivingEntity.travelInFluid` → `applyFluidMovingSpeed` + damping
    `0.5`. **원본과 동일 damping (0.5)** — 기본 lava 이동 동치.
  - 차이: 원본 `motionY -= 0.02D` (중력 감쇠) vs vanilla `0.05` (중력) — 미세 차이.
  - 차이: 원본 L594-L597 `isCollidedHorizontally + isOffsetPositionInLiquid → motionY=0.3`
    (lava 벽 점프) — vanilla `LivingEntity.swimUpward` 유사 처리 있음.

**C-2. 결정: 미이식 유지 + 근사 기록**
- [ ] C-2. `handleLava` SM 자체 이식은 **불필요** 결정 (vanilla 동치). §7 근사 기록 (B-42c
  관련 잔존):
  - lava 기본 이동 vanilla 위임 — 시멘틱 동치
  - 미세 상수 차이 (motionY -= 0.02D vs vanilla 0.05 중력): 체감 영향 낮음

**C-3. 대안: 이식 진행 (엄격 1:1 원칙)**
- [ ] C-3. 사용자 지시가 "엄격 1:1" 이므로 C-2 대신 `handleLava` 도 이식 선택 가능:
  - `SmartMovingSwimmer.handleLava(player, sm, movementInput)` 별도 메서드
  - `MixinLivingEntityClient.sm_beforeTravel` 에서 `handleSwimming` 반환 false + lava
    + !isLiquidClimbing + !isFlying 시 호출
  - 내부: standupIfPossible + resetClimbing + resetSwimming + moveFlying + damping 0.5 +
    motionY -= 0.02 + 벽 점프 motionY=0.3
  - `ci.cancel()` 로 vanilla travel() 취소

**C-4. 선택 권고**
- [ ] C-4. **C-3 (엄격 이식) 권고** — Extended #2 방침 따름. 단 플레이테스트에서 체감 차이
  없으면 C-2 (근사 유지) 로 후퇴 가능.

---

### Phase D. lava 연관 상태/점프 경로 감사

**D-1. `tryJump(SLIDE_DOWN/Up)` lava 조건**
- [ ] D-1. 원본 L1852 `jump = jumpAvoided && isJumping && !isInWater() && !isInLava()` —
  1.21.1 에서 점프 차단 조건이 lava 포함인지 grep 으로 확인. 미포함 시 추가.

**D-2. `isHeadJumping` 해제 lava 조건**
- [ ] D-2. 원본 L2525-L2530 `isHeadJumping = isHeadJumping && ... && !isInLava()` — 1.21.1
  이식 상태 grep. 미포함 시 `|| player.isInLava()` 해제 조건 추가.

**D-3. `isGroundSprinting` 해제 lava 조건**
- [ ] D-3. 원본 L2679 `isGroundSprinting = ... && !isSwimming && !isDiving && !isClimbing`
  — lava 조건 없음. 1.21.1 동일 확인.

**D-4. `jumpAvoided` lava 조건** (원본 L2643)
- [ ] D-4. 원본 `jumpAvoided = onGround || isFlying || capabilities.isFlying || isSwimming
  || isDiving || isInLava()` — 1.21.1 grep 하여 `isInLava()` 포함 여부 확인.

---

### Phase E. 빌드 + 플레이테스트

**E-1. 빌드 검증**
- [ ] E-1. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL.

**E-2. 회귀 감사**
- [ ] E-2. 포커스 #2 Extended 기존 이식 영향 확인:
  - `cfg.lavaLikeWater = false` (기본값) 시 기존 water-only 동작 유지
  - `cfg.lavaLikeWater = true` 시 lava swim/dive 신규 활성
  - Creative 모드에서 `lavaLikeWater = true` 기본값 (원본 Creative) — Config 별도 처리

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

**전체 규모**: Phase A 4 + Phase B 3 + Phase C 1 (C-3 선택 시 본문 이식 소규모) + Phase D 4 +
Phase E 3 = **약 15 원자 / 예상 2-3 세션**.

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

---

## 7. 근사 이식 지점 (이 포커스)

**§7 이 파일**: Phase 진행 중 불가피한 근사 이식 지점. 현재 시작 시점: 0건.

잔존 근사 후보:
- FiniteLiquid mod 분기 (원본 L137-L138) — mod 1.21.1 미이식. 해소 불가. **유지**.

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

### 원본 소스 경로 (로컬)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingBase.java` (L131-L151 getLiquidBorder)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java` (L578-L600 handleLava, L132-L134 호출 컨텍스트)
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\config\SmartMovingClientConfig.java` (L87-L90 isLavaLikeWaterEnabled)

### 1.21.1 이식 대상
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` (L2094 getLiquidBorder)
- `src/client/java/choco/ratel/smartmoving/client/SmartMovingSwimmer.java` (lava 이동 처리 추가 시)
- `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` (handleLava 호출 추가 시)
- `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` (lavaLikeWater 이미 이식)

### 연관 포커스
- **#2 Extended** (완결 — 세션 134): B-42c / B-42d / B-7c 이식 완료. 이 포커스의 의존 선행.
- **#2.5** Jumper Factor: 독립. 동시 진행 가능.
