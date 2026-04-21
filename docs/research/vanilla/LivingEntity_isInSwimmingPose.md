# LivingEntity.isInSwimmingPose() 호출 전체 추적

소스: IntelliJ decompile 캐시 `AppData/Local/Temp/net/minecraft/entity/`  
Yarn 매핑: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/mappings.tiny`

---

## 정의 체인

### Entity.isInSwimmingPose() — 베이스 정의

- Yarn: `isInSwimmingPose` (intermediary: `method_20232`, obf: `ce`, desc: `()Z`)
- 클래스: `net.minecraft.entity.Entity`
- 소스 위치: `Entity.java:3179`

```java
public boolean isInSwimmingPose() {
    return this.isInPose(EntityPose.SWIMMING);
}
```

- `isInPose` Yarn: `method_41328` (obf: `c`)

```java
// Entity.java:576
public boolean isInPose(EntityPose pose) {
    return this.getPose() == pose;
}
```

- `getPose()` → `DataTracker.get(POSE)` (TrackedData, 네트워크 동기화됨)

---

### LivingEntity.isInSwimmingPose() — 오버라이드

- Yarn: 동일 (`method_20232`)
- 클래스: `net.minecraft.entity.LivingEntity`
- 소스 위치: `LivingEntity.java:3325`

```java
@Override
public boolean isInSwimmingPose() {
    return super.isInSwimmingPose() || !this.isFallFlying() && this.isInPose(EntityPose.FALL_FLYING);
}
```

**반환 true 조건** (양쪽 중 하나):
1. `pose == SWIMMING` (Entity 베이스 조건)
2. `!isFallFlying() && pose == FALL_FLYING`

조건 2의 의미: 포즈는 FALL_FLYING이지만 실제로 fall-flying이 아닌 상태. 즉 **크롤링 중인 플레이어**가 해당된다 (updatePose에서 공간 부족 시 FALL_FLYING 대신 SWIMMING이 아닌 — 실제로는 SWIMMING 포즈가 강제됨, 상세는 updatePose 문서 참고).

---

### Entity.isCrawling() — isInSwimmingPose 사용

- Yarn: `isCrawling` (intermediary: `method_20448`, obf: `cf`, desc: `()Z`)
- 소스 위치: `Entity.java:3192`
- Javadoc: "An entity is crawling if it is in swimming pose, but is not touching water."

```java
public boolean isCrawling() {
    return this.isInSwimmingPose() && !this.isTouchingWater();
}
```

- `isTouchingWater` Yarn: `method_5799` (obf: `bf`)
- 오버라이드 없음 — Entity 클래스에만 정의
- 현재 decompiled source(Java)에서 `isCrawling()`을 직접 호출하는 곳은 발견되지 않음

---

## 호출 위치 전체

### 호출 위치 1: LivingEntity.updateLeaningPitch()

- Yarn: `updateLeaningPitch` (intermediary: `method_6072`, obf: `N`, desc: `()V`)
- 소스 위치: `LivingEntity.java:3155`
- 실행 위치: **클라이언트 + 서버** (tickMovement에서 호출, isLogicalSideForUpdatingMovement 제어)
- 호출 시점: `tickMovement()` 내부 (LivingEntity_tick_tickMovement.md 문서 참고)

```java
private void updateLeaningPitch() {
    this.lastLeaningPitch = this.leaningPitch;
    if (this.isInSwimmingPose()) {
        this.leaningPitch = Math.min(1.0F, this.leaningPitch + 0.09F);
    } else {
        this.leaningPitch = Math.max(0.0F, this.leaningPitch - 0.09F);
    }
}
```

**관련 필드 Yarn 이름:**
| 필드 | obf | intermediary | Yarn |
|------|-----|-------------|------|
| `leaningPitch` | `cn` | `field_6243` | `leaningPitch` |
| `lastLeaningPitch` | `co` | `field_6264` | `lastLeaningPitch` |

**동작:**
- `isInSwimmingPose()` = true → `leaningPitch` +0.09F/틱, max 1.0F
- `isInSwimmingPose()` = false → `leaningPitch` -0.09F/틱, min 0.0F
- 0→1 전환: 약 12틱 (0.09 × 12 ≈ 1.08)
- 1→0 전환: 약 12틱

`getLeaningPitch(float tickDelta)` — Yarn: `method_6024` (obf: `a`):
```java
public float getLeaningPitch(float tickDelta) {
    return MathHelper.lerp(tickDelta, this.lastLeaningPitch, this.leaningPitch);
}
```

`leaningPitch`는 렌더러에서 수영/크롤링 자세 기울임의 lerp 계수로 사용된다.

---

### 호출 위치 2: PlayerEntity.getLeashPos(float delta)

- Yarn: `getLeashPos` (intermediary: `method_30951`, obf: `s`, desc: `(F)Lexc;`)
- 소스 위치: `PlayerEntity.java:2119`
- 실행 위치: **클라이언트 (렌더 관련)**

```java
@Override
public Vec3d getLeashPos(float delta) {
    double d = 0.22 * (this.getMainArm() == Arm.RIGHT ? -1.0 : 1.0);
    float f = MathHelper.lerp(delta * 0.5F, this.getPitch(), this.prevPitch) * (float)(Math.PI / 180.0);
    float g = MathHelper.lerp(delta, this.prevBodyYaw, this.bodyYaw) * (float)(Math.PI / 180.0);
    if (this.isFallFlying() || this.isUsingRiptide()) {
        // ... elytra/riptide 케이스
        return this.getLerpedPos(delta).add(new Vec3d(d, -0.11, 0.85).rotateZ(-k).rotateX(-f).rotateY(-g));
    } else if (this.isInSwimmingPose()) {
        return this.getLerpedPos(delta).add(new Vec3d(d, 0.2, -0.15).rotateX(-f).rotateY(-g));
    } else {
        double l = this.getBoundingBox().getLengthY() - 1.0;
        double e = this.isInSneakingPose() ? -0.2 : 0.07;
        return this.getLerpedPos(delta).add(new Vec3d(d, l, e).rotateY(-g));
    }
}
```

**`isInSwimmingPose()` = true 시 반환값:**
- 기준점: `getLerpedPos(delta)` (틱 보간 위치)
- 오프셋: `Vec3d(d, 0.2, -0.15)` — 수직 +0.2, 앞쪽 -0.15 (로컬 공간)
- 역할: 수영/크롤링 시 리쉬(목줄) 연결 위치 오프셋, 시각적 효과만

---

### 호출 위치 3: PlayerEntityRenderer.setupTransforms (바이트코드)

- Yarn: `setupTransforms` (intermediary: `method_4212`, obf: `a`)
- 클래스: `net.minecraft.client.render.entity.PlayerEntityRenderer`
- 소스: Java 파일 없음, `.class` 파일만 존재 → `javap -c`로 분석
- 실행 위치: **클라이언트 전용**
- 시그니처: `setupTransforms(AbstractClientPlayerEntity player, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float headYaw)`

**재구성한 로직:**

```
float leaningPitch = player.getLeaningPitch(tickDelta)
float pitch = player.getPitch(tickDelta)

if (player.isFallFlying()):
    // 1. Elytra 분기
    super.setupTransforms(...)
    float f9 = getFallFlyingTicks() + tickDelta
    float f10 = clamp(f9 * f9 / 100.0, 0, 1)
    if (!isUsingRiptide()):
        matrices.multiply(POSITIVE_X.rotationDegrees(f10 * (-90.0f - pitch)))
    // 속도 방향 Y축 회전 계산 ...
    matrices.multiply(POSITIVE_Y.rotation(signum(cross) * acos(dot)))

else if (leaningPitch > 0):
    // 2. 수영/크롤링 분기  ← isInSwimmingPose()는 여기서 호출됨
    super.setupTransforms(...)
    float targetAngle = isTouchingWater() ? (-90.0f - pitch) : -90.0f
    float f10 = lerp(leaningPitch, 0, targetAngle)   // lerp(t, start, end)
    matrices.multiply(POSITIVE_X.rotationDegrees(f10))
    if (player.isInSwimmingPose()):
        matrices.translate(0, -1.0f, 0.3f)           // ← 핵심 오프셋

else:
    // 3. 일반 서기/웅크리기 분기
    super.setupTransforms(...)
```

**`isInSwimmingPose()` = true 시 적용 변환:**
- `matrices.translate(0, -1.0f, 0.3f)` (로컬 공간 기준)
- 렌더링 위치를 아래로 1블록, 앞쪽으로 0.3블록 이동
- **leaningPitch > 0인 경우에만 이 분기에 진입** — leaningPitch = 0이면 이 translate는 절대 실행되지 않음

**수치 요약:**
| 수치 | 의미 |
|------|------|
| `leaningPitch > 0` | 수영/크롤링 분기 진입 조건 |
| `-90.0f - pitch` | 수중 수평 기울임 목표각 |
| `-90.0f` | 비수중(크롤링) 수평 기울임 목표각 |
| `translate(0, -1.0, 0.3)` | isInSwimmingPose 시 추가 위치 오프셋 |

---

## 전체 흐름 요약

```
tickMovement()
  └─ updateLeaningPitch()
       ├─ isInSwimmingPose() = true  → leaningPitch +0.09/틱 → max 1.0
       └─ isInSwimmingPose() = false → leaningPitch -0.09/틱 → min 0.0

렌더 (클라이언트):
  setupTransforms()
    ├─ leaningPitch > 0 분기:
    │    X축 회전(lerp 0→-90°)
    │    isInSwimmingPose() → translate(0,-1,0.3)
    ├─ isFallFlying 분기: 별도 처리
    └─ else: 일반 처리

getLeashPos()
  └─ isInSwimmingPose() → Vec3d(d, 0.2, -0.15) 오프셋
```

---

## SM 1.21.1 포팅 충돌 분석

### 충돌 1: leaningPitch 자동 제어

SM 크롤링은 SWIMMING 포즈(또는 FALL_FLYING 포즈)를 강제한다.  
→ `isInSwimmingPose()` = true → `updateLeaningPitch()`가 `leaningPitch` 를 1.0으로 올린다.  
→ 렌더러 `setupTransforms()`에서 X축 -90° 회전 + translate(0,-1,0.3)이 자동 적용된다.

**vanilla 1.21.1의 크롤링 비주얼은 이 leaningPitch 메커니즘으로 완전히 구현되어 있다.**  
SM 원본은 자체 렌더 코드로 회전을 직접 조작했지만, 1.21.1에서는 leaningPitch가 이를 대신한다.  
→ SM 렌더 코드가 추가 회전을 적용하면 **이중 회전 충돌** 발생.

### 충돌 2: translate(0,-1,0.3) 오프셋

`isInSwimmingPose()` = true이고 `leaningPitch > 0`이면 항상 적용된다.  
SM 크롤링 시 이 오프셋을 원하는지 여부를 결정해야 한다.

### 충돌 3: isCrawling()과 SM 크롤링의 관계

`isCrawling()` = `isInSwimmingPose() && !isTouchingWater()`  
SM이 SWIMMING 포즈로 크롤링을 표현하면, 물 밖에서는 `isCrawling()` = true가 된다.  
vanilla에서 isCrawling()이 어디서 더 사용되는지는 현재 decompiled Java source 범위 내에서는 확인되지 않음.  
→ [미확인 — binary .class 파일들(PlayerEntityModel 등)에서의 isCrawling() 사용처 미확인]

---

## Yarn 이름 확인 목록

| Yarn 이름 | intermediary | obf | desc | 클래스 |
|-----------|-------------|-----|------|--------|
| `isInSwimmingPose` | `method_20232` | `ce` | `()Z` | Entity (LivingEntity override) |
| `isInPose` | `method_41328` | `c` | `(Lbua;)Z` | Entity |
| `isCrawling` | `method_20448` | `cf` | `()Z` | Entity |
| `isTouchingWater` | `method_5799` | `bf` | `()Z` | Entity |
| `updateLeaningPitch` | `method_6072` | `N` | `()V` | LivingEntity |
| `getLeaningPitch` | `method_6024` | `a` | `(F)F` | LivingEntity |
| `leaningPitch` (필드) | `field_6243` | `cn` | `F` | LivingEntity |
| `lastLeaningPitch` (필드) | `field_6264` | `co` | `F` | LivingEntity |
| `getLeashPos` | `method_30951` | `s` | `(F)Lexc;` | PlayerEntity |
| `setupTransforms` | `method_4212` | `a` | `(Lgdy;Lfbi;FFFF)V` | PlayerEntityRenderer |
