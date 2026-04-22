# vanilla Yarn명 기타 확인 (1.21.1)

소스: Fabric Loom 디컴파일 / `javap` 분석  
작성: 2026-04-22 (R-20)

---

## A-20: `Stats.JUMP` Yarn명

```java
// net/minecraft/stat/Stats.java
public static final Identifier JUMP;
```

**사용 패턴:**
```java
player.incrementStat(Stats.JUMP);
// incrementStat(Identifier) 오버로드 존재 → 직접 사용 가능
```

원본 1.7.10: `sp.addStat(StatList.jumpStat, 1)`  
1.21.1: `player.incrementStat(Stats.JUMP)`

---

## A-24: `bodyYaw` 1.21.1 Yarn 필드명

```java
// net/minecraft/entity/LivingEntity.java
public float bodyYaw;       // 공개 필드 — 직접 접근 가능
public float prevBodyYaw;   // 이전 틱 값
```

`setBodyYaw(float)` / `getBodyYaw()` 메서드도 존재.  
직접 `player.bodyYaw = angle;` 사용 가능 (accessor Mixin 불필요).

---

## A-27: `addMovementStat` → 1.21.1

1.21.1 `PlayerEntity` / `LivingEntity`에 `addMovementStat` 독립 메서드 없음.  
이동 통계 + 소진 처리는 `PlayerEntity.travel()` 내부에 인라인.

**1.21.1 대응 전략:**
- C-22: `@Inject(method="travel", at=@At("HEAD"))` → `beforeAddMovingHungerBatch()`
- C-22: `@Inject(method="travel", at=@At("TAIL"))` → SM hunger 적용 + `afterAddMovingHungerBatch()`

---

## A-28: 포션 업데이트 메서드 Yarn명

```java
// net/minecraft/entity/LivingEntity.java
protected void tickStatusEffects();
```

**1.21.1 대응 전략:**
- C-23: `@Inject(method="tickStatusEffects", at=@At("HEAD"))` → `afterAddMovingHungerBatch()`
- C-23: `@Inject(method="tickStatusEffects", at=@At("TAIL"))` → `beforeAddMovingHungerBatch()`

---

## A-29: 서버 위치 검증 메서드 Yarn명

```java
// net/minecraft/server/network/ServerPlayNetworkHandler.java
public void onPlayerMove(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket);
```

원본 1.7.10: `NetHandlerPlayServer.processPlayer()`  
1.21.1: `ServerPlayNetworkHandler.onPlayerMove(PlayerMoveC2SPacket)`

**C-21 구현:**
```java
@Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
```
→ SM 클라이밍/크롤링 상태 시 positionSqDist 체크 skip으로 rubber-band 방지.

---

## A-30: `jumpMovementFactor`/`offGroundSpeed` 1.21.1 구현

1.21.1에서 `jumpMovementFactor` 필드 없음.  
대신 `LivingEntity`/`PlayerEntity`에 `getOffGroundSpeed()` 메서드로 추상화:

```java
// PlayerEntity.getOffGroundSpeed()
protected float getOffGroundSpeed() {
    if (abilities.flying && !hasVehicle()) {
        return isSprinting() ? abilities.getFlySpeed() * 2 : abilities.getFlySpeed();
    }
    return isSprinting() ? 0.026F : 0.02F;
}
```

원본 1.7.10: `jumpMovementFactor = 0.05F` (SM이 flight 억제 시 설정)  
1.21.1 대응:

**C-41 구현:**
```java
@Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
private void sm_getOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
    // SM fly 비활성화 시 공중 이동 속도를 0.05F로 억제
    // 원본: jumpMovementFactor = 0.05F
}
```
