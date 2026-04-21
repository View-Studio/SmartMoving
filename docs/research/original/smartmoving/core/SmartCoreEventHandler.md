# SmartCoreEventHandler.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreEventHandler.java  
패키지: `net.smart.core`  
종류: `class`  
상속 없음

---

## 역할

ASM 바이트코드 주입으로 vanilla 메서드에 삽입된 후크(hook) 진입점과, 그 후크를 구독하는 핸들러 등록소를 제공한다.

**구조:**
- `static Set<SmartCoreEventHandler> handlers` — 등록된 핸들러 집합
- **static 디스패치 메서드**: ASM이 vanilla 바이트코드에 주입하는 호출 대상. vanilla 메서드 파라미터 전체를 받아 핸들러 인스턴스 메서드로 분배
- **instance 메서드**: 서브클래스가 override할 후크 포인트. 기본 구현은 빈 메서드

- **실행 위치**: 서버(NetHandlerPlayServer, ItemInWorldManager) + 클라이언트(PlayerControllerMP) 혼재

---

## static 핸들러 등록소

```java
private final static Set<SmartCoreEventHandler> handlers = new HashSet<SmartCoreEventHandler>();

public static void Add(SmartCoreEventHandler handler)    { handlers.add(handler); }
public static void Remove(SmartCoreEventHandler handler) { handlers.remove(handler); }
```

- `HashSet` — 중복 등록 방지
- 서브클래스 인스턴스를 `Add()`로 등록하면 모든 static 디스패치 메서드에서 해당 인스턴스 메서드를 호출

---

## static 디스패치 메서드 (ASM 주입 대상)

모두 `for(SmartCoreEventHandler eventHandler : handlers) eventHandler.xxx(...)` 패턴.  
`@SuppressWarnings("unused")` — ASM 주입으로만 호출되므로 IDE에서 미사용으로 표시됨.

### `NetHandlerPlayServer_beforeProcessPlayer` / `_afterProcessPlayer`

```java
public static void NetHandlerPlayServer_beforeProcessPlayer(
    NetHandlerPlayServer netServerHandler, C03PacketPlayer packetPlayer)

public static void NetHandlerPlayServer_afterProcessPlayer(
    NetHandlerPlayServer netServerHandler, C03PacketPlayer packetPlayer)
```

- 주입 위치: `NetHandlerPlayServer.processPlayer(C03PacketPlayer)` 앞/뒤
- `C03PacketPlayer`: 플레이어 위치/방향 패킷 (서버 수신)
- 핸들러 인스턴스에 전달하는 파라미터: `netServerHandler` 만 (`packetPlayer` 버림)

---

### `NetHandlerPlayServer_beforeProcessPlayerBlockPlacement` / `_afterProcessPlayerBlockPlacement`

```java
public static void NetHandlerPlayServer_beforeProcessPlayerBlockPlacement(
    NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)

public static void NetHandlerPlayServer_afterProcessPlayerBlockPlacement(
    NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place)
```

- 주입 위치: `NetHandlerPlayServer.processPlayerBlockPlacement(C08PacketPlayerBlockPlacement)` 앞/뒤
- `C08PacketPlayerBlockPlacement`: 블록 배치 패킷 (서버 수신)
- **이 두 메서드만 `@SuppressWarnings("unused")` 없음** — 원본의 어노테이션 누락 또는 의도적
- 핸들러 인스턴스에 전달: `netServerHandler`, `packet15place` 둘 다 전달

---

### `PlayerControllerMP_beforeOnPlayerRightClick` / `_afterOnPlayerRightClick`

```java
public static void PlayerControllerMP_beforeOnPlayerRightClick(
    PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP,
    World world, ItemStack itemStack,
    int integer1, int integer2, int integer3, int integer4, Vec3 vec3)

public static void PlayerControllerMP_afterOnPlayerRightClick(
    PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP,
    World world, ItemStack itemStack,
    int integer1, int integer2, int integer3, int integer4, Vec3 vec3)
```

- 주입 위치: `PlayerControllerMP.onPlayerRightClick(...)` 앞/뒤 (클라이언트)
- 핸들러 인스턴스에 전달: `playerControllerMP`, `entityPlayerSP` 만 (world/itemStack/좌표/Vec3 버림)

---

### `ItemInWorldManager_beforeActivateBlockOrUseItem` / `_afterActivateBlockOrUseItem`

```java
public static void ItemInWorldManager_beforeActivateBlockOrUseItem(
    ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer,
    World world, ItemStack itemStack,
    int integer1, int integer2, int integer3, int integer4,
    float float1, float float2, float float3)

public static void ItemInWorldManager_afterActivateBlockOrUseItem(
    ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer,
    World world, ItemStack itemStack,
    int integer1, int integer2, int integer3, int integer4,
    float float1, float float2, float float3)
```

- 주입 위치: `ItemInWorldManager.activateBlockOrUseItem(...)` 앞/뒤 (서버)
- 핸들러 인스턴스에 전달: `itemInWorldManager`, `entityPlayer` 만 (world/itemStack/좌표/float 버림)

---

## instance 메서드 (후크 포인트) — 전부 빈 구현

```java
@SuppressWarnings("unused")
public void beforeProcessPlayer(NetHandlerPlayServer netServerHandler) {}

@SuppressWarnings("unused")
public void afterProcessPlayer(NetHandlerPlayServer netServerHandler) {}

@SuppressWarnings("unused")
public void beforeProcessPlayerBlockPlacement(
    NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place) {}

@SuppressWarnings("unused")
public void afterProcessPlayerBlockPlacement(
    NetHandlerPlayServer netServerHandler, C08PacketPlayerBlockPlacement packet15place) {}

@SuppressWarnings("unused")
public void beforeOnPlayerRightClick(
    PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP) {}

@SuppressWarnings("unused")
public void afterOnPlayerRightClick(
    PlayerControllerMP playerControllerMP, EntityPlayer entityPlayerSP) {}

@SuppressWarnings("unused")
public void beforeActivateBlockOrUseItem(
    ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer) {}

@SuppressWarnings("unused")
public void afterActivateBlockOrUseItem(
    ItemInWorldManager itemInWorldManager, EntityPlayer entityPlayer) {}
```

서브클래스에서 필요한 메서드만 override.

---

## 주입 대상 vanilla 메서드 요약

| static 디스패치 메서드 | vanilla 주입 대상 | 실행 위치 |
|----------------------|-------------------|-----------|
| `NetHandlerPlayServer_before/afterProcessPlayer` | `NetHandlerPlayServer.processPlayer(C03PacketPlayer)` | 서버 |
| `NetHandlerPlayServer_before/afterProcessPlayerBlockPlacement` | `NetHandlerPlayServer.processPlayerBlockPlacement(C08PacketPlayerBlockPlacement)` | 서버 |
| `PlayerControllerMP_before/afterOnPlayerRightClick` | `PlayerControllerMP.onPlayerRightClick(...)` | 클라이언트 |
| `ItemInWorldManager_before/afterActivateBlockOrUseItem` | `ItemInWorldManager.activateBlockOrUseItem(...)` | 서버 |

---

## 파라미터 전달 축소 규칙

static 디스패치는 vanilla 메서드 전체 파라미터를 받지만, instance 메서드에는 핵심 객체만 전달:

| 주입 대상 | static 파라미터 | instance 파라미터 |
|-----------|----------------|-----------------|
| processPlayer | `(NetHandlerPlayServer, C03PacketPlayer)` | `(NetHandlerPlayServer)` |
| processPlayerBlockPlacement | `(NetHandlerPlayServer, C08Packet...)` | `(NetHandlerPlayServer, C08Packet...)` — 전부 전달 |
| onPlayerRightClick | `(PlayerControllerMP, EntityPlayer, World, ItemStack, int×4, Vec3)` | `(PlayerControllerMP, EntityPlayer)` |
| activateBlockOrUseItem | `(ItemInWorldManager, EntityPlayer, World, ItemStack, int×4, float×3)` | `(ItemInWorldManager, EntityPlayer)` |

`processPlayerBlockPlacement`만 패킷 객체까지 instance 메서드에 전달.

---

## import

```java
import java.util.*;
import net.minecraft.client.multiplayer.*;  // PlayerControllerMP
import net.minecraft.entity.player.*;       // EntityPlayer
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;             // NetHandlerPlayServer
import net.minecraft.server.management.*;  // ItemInWorldManager
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.network.play.client.*; // C03PacketPlayer, C08PacketPlayerBlockPlacement
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartCoreMethodVisitor` / `SmartCoreTransformation` | ASM 주입으로 static 메서드를 vanilla 바이트코드에 삽입 |
| `SmartMovingCoreEventHandler` (추정) | 이 클래스를 상속하여 `Add()`로 등록 — [SmartMovingCoreEventHandler 리서치에서 확인 필요] |

---

## 주요 관찰 사항

1. **ASM 주입 ↔ 이벤트버스 대체 패턴**: FML 이벤트버스 대신 ASM으로 vanilla 메서드에 직접 static 메서드 호출을 삽입. `SmartCoreClassVisitor`/`SmartCoreMethodVisitor`가 바이트코드에 `SmartCoreEventHandler.Xxx_before/after(...)` 호출을 삽입.

2. **`processPlayerBlockPlacement`의 `@SuppressWarnings` 누락**: 다른 static 디스패치 메서드와 달리 이 두 메서드에만 어노테이션이 없음. 원본 코드의 일관성 오류로 보임 — 동작에는 영향 없음.

3. **파라미터 버림 설계**: 좌표, 월드, 아이템스택 등 세부 파라미터는 핸들러에 전달하지 않음. `SmartMoving`이 필요로 하는 것은 플레이어/서버핸들러 객체 자체이므로 나머지는 불필요.

4. **1.21.1 이식**: ASM 주입 전체를 Mixin `@Inject`로 교체. `before` 후크 → `@Inject(at = @At("HEAD"))`, `after` 후크 → `@Inject(at = @At("RETURN"))`. `handlers` Set + `Add/Remove` 패턴 → Mixin 자체에 구현하거나 Fabric 이벤트로 대체.
