# ServerPlayNetworkHandler.onPlayerMove() — vanilla 1.21.1 리서치

## 소스
- 클래스: `net/minecraft/server/network/ServerPlayNetworkHandler`
- Yarn 이름: `ServerPlayNetworkHandler`
- 바이트코드 추출: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-48f5f74c97/…/minecraft-common-48f5f74c97-1.21.1-…jar`
- 분석 명령: `javap -c net/minecraft/server/network/ServerPlayNetworkHandler.class`

---

## Yarn 이름 목록 (mappings.tiny 직접 확인)

### 메서드
| 디스크립터 | obf | Yarn 이름 |
|---|---|---|
| `(Lahg;)V` | `a` | `onPlayerMove` → `method_12063` |
| `(DDDFF)V` | `a` | `requestTeleport(DDDFF)` → `method_14363` |
| `(DDDFFLjava/util/Set;)V` | `a` | `requestTeleport(DDDFFSet)` → `method_14360` |
| `(DDDFF)Z` | `b` | `isMovementInvalid` → `method_14371` |
| `()V` | `l` | `syncWithPlayerPosition` → `method_14372` |
| `()Z` | `p` | `handlePendingTeleport` → `method_60947` |
| `()Z` | `h` | `isHost` → `method_52402` |

### 필드 (ServerPlayNetworkHandler)
| 타입 | obf | Yarn 이름 |
|---|---|---|
| `Vec3d` | `F` | `requestedTeleportPos` → `field_14119` |
| `int` | `G` | `requestedTeleportId` → `field_14123` |
| `int` | `H` | `prevTeleportCheckTicks` → `field_14139` |
| `boolean` | `I` | `floating` → `field_14131` |
| `int` | `J` | `floatingTicks` → `field_14138` |
| `int` | `M` | `movePacketsCount` → `field_14117` |
| `double` | `s` | `lastTickX` → `field_14130` |
| `double` | `t` | `lastTickY` → `field_14146` |
| `double` | `u` | `lastTickZ` → `field_14128` |
| `double` | `v` | `updatedX` → `field_14145` |
| `double` | `w` | `updatedY` → `field_14126` |
| `double` | `x` | `updatedZ` → `field_14144` |

---

## onPlayerMove() 전체 흐름

패킷 타입: `PlayerMoveC2SPacket` (Yarn: `ahg`)

### 1. 초기 검증 (early return)

```
isMovementInvalid(packetX, packetY, packetZ, packetYaw, packetPitch)
  → NaN 또는 Infinity 포함 시 → disconnect("multiplayer.disconnect.invalid_player_movement") + return

notInAnyWorld == true → return

ticks == 0 → syncWithPlayerPosition()

handlePendingTeleport() == true → return   ← 대기 텔레포트 있으면 패킷 무시
```

### 2. 패킷 좌표 정규화

```java
double packetX = clampHorizontal(packet.getX(serverX));
double packetY = clampVertical(packet.getY(serverY));
double packetZ = clampHorizontal(packet.getZ(serverZ));
float packetYaw  = MathHelper.wrapDegrees(packet.getYaw(serverYaw));
float packetPitch = MathHelper.wrapDegrees(packet.getPitch(serverPitch));
```

- `clampHorizontal`: 절대값이 너무 크면 서버 현재 값으로 클램프
- `clampVertical`: 마찬가지

### 3. 탑승 중 처리

```
hasVehicle() == true:
  → updatePositionAndAngles(serverX, serverY, serverZ, packetYaw, packetPitch)
  → chunkManager.updatePosition(player)
  → return
```
탑승 중에는 이동 검증 없이 위치만 유지하고 각도는 클라이언트 값 반영.

### 4. 슬리핑 중 처리

```
double dx = packetX - lastTickX
double dy = packetY - lastTickY
double dz = packetZ - lastTickZ
double distanceSq = dx*dx + dy*dy + dz*dz
double velocityLenSq = player.getVelocity().lengthSquared()

isSleeping() AND distanceSq > 1.0:
  → requestTeleport(serverX, serverY, serverZ, yaw, pitch)  ← rubber-band
  → return
```

### 5. 속도 초과 검증 ("moved too quickly")

```
isFallFlying = player.isFallFlying()
shouldTick = world.getTickManager().shouldTick()

if shouldTick:
  movePacketsCount++
  packetsSinceLastTick = movePacketsCount - lastTickMovePacketsCount

  if packetsSinceLastTick > 5:
    LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)")
    packetsSinceLastTick = 1

  if !isInTeleportationState():
    if !(DISABLE_ELYTRA_MOVEMENT_CHECK gamerule AND isFallFlying):
      threshold = isFallFlying ? 300f : 100f
      if (distanceSq - velocityLenSq) > threshold * packetsSinceLastTick AND !isHost():
        LOGGER.warn("{} moved too quickly! {},{},{}")
        → requestTeleport(serverX, serverY, serverZ, yaw, pitch)  ← rubber-band
        → return
```

### 6. 실제 이동 처리 (메인 경로)

**서버 bounding box 저장**: `Box oldBBox = player.getBoundingBox()`

**실제 이동 벡터 계산** (updatedX/Y/Z 기준):
```java
double moveX = packetX - updatedX
double moveY = packetY - updatedY
double moveZ = packetZ - updatedZ
boolean jumping = moveY > 0
```

**점프 처리**:
```
isOnGround() AND packet.isOnGround() == false:
  → (점프 조건 성립 — jump() 호출 안 함)
isOnGround() AND !packet.isOnGround() AND jumping:
  → player.jump()
```
※ 정확히는: `isOnGround() AND !packet.isOnGround()` 이면 jump 호출 (line 658-684)

**물리 적용**:
```java
player.move(MovementType.PLAYER, Vec3d(moveX, moveY, moveZ))
```

**move 후 델타 재계산**:
```java
double postMoveX = serverX_after_move - packetX
double postMoveY = serverY_after_move - packetY  // dy: [-0.5, 0.5] 범위 밖이면 0으로 clamp
double postMoveZ = serverZ_after_move - packetZ
double postDistanceSq = postMoveX² + postMoveY² + postMoveZ²
```

**"moved wrongly" 판정**:
```
!isInTeleportationState()
AND postDistanceSq > 0.0625
AND !isSleeping()
AND !isCreative()
AND !SPECTATOR:
  → wronglyMoved = true
  → LOGGER.warn("{} moved wrongly!")
```

**충돌 검증 및 rubber-band**:
```
!noClip AND !isSleeping():
  if wronglyMoved:
    world.isSpaceEmpty(player, oldBBox) == false:
      → requestTeleport(oldX, oldY, oldZ, yaw, pitch)
      → player.handleFall(deltaX, deltaY, deltaZ, packet.isOnGround())
      → return
  else:
    !isPlayerNotCollidingWithBlocks(worldView, oldBBox, packetX, packetY, packetZ):
      → requestTeleport(oldX, oldY, oldZ, yaw, pitch)
      → return
```

**정상 케이스 — 서버 위치 패킷 위치로 업데이트**:
```java
player.updatePositionAndAngles(packetX, packetY, packetZ, packetYaw, packetPitch)
```

**floating 판정** (공중에 비합법적으로 떠있음):
```
floating = false if 다음 중 하나라도 해당:
  - moveY stored (dy) >= -0.03125  ← 하강 중 아님
  - groundCollision (move() 결과 isOnGround)
  - SPECTATOR gamemode
  - server.isFlightEnabled()
  - player.getAbilities().allowFlying
  - player.hasStatusEffect(LEVITATION)
  - isFallFlying
  - isUsingRiptide
  - !isEntityOnAir(player)
floating = true 나머지 경우
```

**청크 매니저 업데이트**:
```java
chunkManager.updatePosition(player)
```

**낙하 및 onGround 처리**:
```java
Vec3d delta = Vec3d(serverX - packetX, serverY - packetY, serverZ - packetZ)
player.setOnGround(packet.isOnGround(), delta)
player.handleFall(deltaX, deltaY, deltaZ, packet.isOnGround())
player.setOnGround(delta)   // 두 번 호출 (overload 다름)
```

**착지 콜백**:
```
jumping == true → player.onLanding()
```

**이동 통계**:
```java
player.increaseTravelMotionStats(deltaX, deltaY, deltaZ)
```

**updatedX/Y/Z 업데이트**:
```java
updatedX = player.getX()
updatedY = player.getY()
updatedZ = player.getZ()
```

---

## requestTeleport(DDDFF) 흐름

```java
// 1. 상대/절대 플래그 적용 (Set<PositionFlag> 버전)
double baseX = flags.contains(X) ? player.getX() : 0
double baseY = flags.contains(Y) ? player.getY() : 0
double baseZ = flags.contains(Z) ? player.getZ() : 0
float baseYaw   = flags.contains(Y_ROT) ? player.getYaw() : 0
float basePitch = flags.contains(X_ROT) ? player.getPitch() : 0

// 2. 상태 저장
requestedTeleportPos = Vec3d(x, y, z)
requestedTeleportId = (requestedTeleportId + 1) % MAX_INT
prevTeleportCheckTicks = ticks

// 3. 서버 위치 즉시 업데이트
player.updatePositionAndAngles(x, y, z, yaw, pitch)

// 4. 클라이언트에 패킷 전송
player.networkHandler.sendPacket(
  new PlayerPositionLookS2CPacket(
    x - baseX, y - baseY, z - baseZ,
    yaw - baseYaw, pitch - basePitch,
    flags, requestedTeleportId
  )
)
```

---

## onTeleportConfirm() 흐름

```
teleportId == requestedTeleportId:
  requestedTeleportPos == null → disconnect("invalid_player_movement")
  else:
    player.updatePositionAndAngles(requestedTeleportPos.x/y/z, yaw, pitch)
    updatedX/Y/Z = requestedTeleportPos.x/y/z
    player.onTeleportationDone() (if isInTeleportationState)
    requestedTeleportPos = null
teleportId != requestedTeleportId: 무시
```

---

## 핵심 설계 원칙 정리

| 검증 | 조건 | 결과 |
|---|---|---|
| NaN/Infinity | 항상 | disconnect |
| 대기 텔레포트 | `requestedTeleportPos != null` | 패킷 무시 |
| 탑승 중 | `hasVehicle()` | 위치 동결, 각도만 반영 |
| 슬리핑 중 이동 | `distanceSq > 1` | rubber-band |
| 속도 초과 | `distanceSq - velocityLenSq > threshold * count` | rubber-band |
| 충돌 통과 | "moved wrongly" + 블록 공간 체크 | rubber-band |
| 정상 | 위 조건 모두 해당 없음 | `updatePositionAndAngles` |

**rubber-band 메커니즘**: 서버가 `requestTeleport(서버 위치)`로 클라이언트를 서버 위치로 되돌림. 클라이언트는 `TeleportConfirmC2SPacket`으로 응답해야 위치가 확정됨. 응답 전까지 `handlePendingTeleport()` = true → 이후 이동 패킷 전부 무시.

**SmartMoving 관련성**: 커스텀 이동 (기어오르기, 슬라이딩 등)은 모두 이 검증을 통과해야 함. 특히:
- `moved too quickly` 체크: threshold=100 (isFallFlying=false 기준). 한 틱에 너무 빠르면 rubber-band
- `moved wrongly` 체크: 물리 적용 후 서버-클라이언트 위치 차이 > 0.0625이면 경고
- `isPlayerNotCollidingWithBlocks`: 클라이언트 위치가 서버 월드에서 블록과 충돌 없어야 함
- floating 판정: 공중에서 비합법적으로 유지되면 `floating=true` → tick()에서 kick

---

## 관련 tick() 처리 (floatingTicks)

`tick()` 메서드에서 (line 62):
```
handlePendingTeleport() == true → return (텔레포트 대기 중 tick 스킵)
floating == true → floatingTicks++
  floatingTicks > 80 → kick("multiplayer.disconnect.flying")
floating == false → floatingTicks = 0
lastTickMovePacketsCount = movePacketsCount
lastTickX/Y/Z = player.getX/Y/Z
```
