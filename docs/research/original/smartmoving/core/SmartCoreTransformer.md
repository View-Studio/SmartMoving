# SmartCoreTransformer.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreTransformer.java  
패키지: `net.smart.core`  
종류: `class`  
구현: `SmartCoreTransformer implements IClassTransformer`

---

## 전체 소스

```java
package net.smart.core;

import java.util.*;
import net.minecraft.launchwrapper.*;

public class SmartCoreTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        List<SmartCoreTransformation> list = null;
        for(SmartCoreTransformation transformation : _transformations)
            if(transformedName.equals(transformation.className))
                (list == null ? (list = new ArrayList<SmartCoreTransformation>()) : list).add(transformation);

        if(list != null)
            return SmartCoreClassVisitor.transform(bytes, list);
        return bytes;
    }

    private static final SmartCoreTransformation[] _transformations = new SmartCoreTransformation[]
    {
        new SmartCoreTransformation
        (
            "net.minecraft.network.NetHandlerPlayServer",
            Name("processPlayer", "a"),
            new String[]
            {
                Name("net.minecraft.network.play.client.C03PacketPlayer", "jd")
            },
            null,
            "net.smart.core.SmartCoreEventHandler",
            "NetHandlerPlayServer_beforeProcessPlayer",
            "NetHandlerPlayServer_afterProcessPlayer"
        ),
        new SmartCoreTransformation
        (
            "net.minecraft.network.NetHandlerPlayServer",
            Name("processPlayerBlockPlacement", "a"),
            new String[]
            {
                Name("net.minecraft.network.play.client.C08PacketPlayerBlockPlacement", "jo")
            },
            null,
            "net.smart.core.SmartCoreEventHandler",
            "NetHandlerPlayServer_beforeProcessPlayerBlockPlacement",
            "NetHandlerPlayServer_afterProcessPlayerBlockPlacement"
        ),
        new SmartCoreTransformation
        (
            Name("net.minecraft.client.multiplayer.PlayerControllerMP", "bje"),
            Name("onPlayerRightClick", "a"),
            new String[]
            {
                Name("net.minecraft.entity.player.EntityPlayer", "yz"),
                Name("net.minecraft.world.World", "ahb"),
                Name("net.minecraft.item.ItemStack", "add"),
                "int",
                "int",
                "int",
                "int",
                Name("net.minecraft.util.Vec3", "azw")
            },
            "boolean",
            "net.smart.core.SmartCoreEventHandler",
            "PlayerControllerMP_beforeOnPlayerRightClick",
            "PlayerControllerMP_afterOnPlayerRightClick"
        ),
        new SmartCoreTransformation
        (
            Name("net.minecraft.server.management.ItemInWorldManager", "qx"),
            Name("activateBlockOrUseItem", "a"),
            new String[]
            {
                Name("net.minecraft.entity.player.EntityPlayer", "yz"),
                Name("net.minecraft.world.World", "ahb"),
                Name("net.minecraft.item.ItemStack", "add"),
                "int",
                "int",
                "int",
                "int",
                "float",
                "float",
                "float"
            },
            "boolean",
            "net.smart.core.SmartCoreEventHandler",
            "ItemInWorldManager_beforeActivateBlockOrUseItem",
            "ItemInWorldManager_afterActivateBlockOrUseItem"
        )
    };

    private static String Name(String deobfuscated, String obfuscated)
    {
        return SmartCorePlugin.isObfuscated ? obfuscated : deobfuscated;
    }
}
```

---

## 역할

FML이 클래스를 로드할 때마다 `transform()`을 호출하는 **ASM 변환기 진입점**.  
`_transformations` 배열에 정의된 4개 변환 명세를 보유하고, 로드 중인 클래스명이 변환 대상이면 `SmartCoreClassVisitor.transform()`에 위임한다.

`SmartCorePlugin.getASMTransformerClass()`가 이 클래스를 FML에 등록.

---

## `transform(String name, String transformedName, byte[] bytes)` — override

```java
@Override
public byte[] transform(String name, String transformedName, byte[] bytes)
{
    List<SmartCoreTransformation> list = null;
    for(SmartCoreTransformation transformation : _transformations)
        if(transformedName.equals(transformation.className))
            (list == null ? (list = new ArrayList<SmartCoreTransformation>()) : list).add(transformation);

    if(list != null)
        return SmartCoreClassVisitor.transform(bytes, list);
    return bytes;
}
```

- `name`: obfuscated 클래스명 (런타임 로더가 전달)
- `transformedName`: deobfuscated/forgefuscated 클래스명 (FML이 변환 후 전달)
- `bytes`: 원본 클래스 바이트코드

**매칭 로직:**
- `_transformations` 배열 전체 순회
- `transformedName.equals(transformation.className)`: 로드 중인 클래스가 변환 대상인지 확인
- 매칭된 변환이 하나라도 있으면 `list`를 lazy 초기화(`list == null ? new ArrayList<>() : list`) 후 추가
- `list != null` → `SmartCoreClassVisitor.transform(bytes, list)` 반환
- 매칭 없음 → `bytes` 그대로 반환 (변환 없음)

**하나의 클래스에 여러 변환이 가능**: `NetHandlerPlayServer`에 2개 변환이 등록되어 있으므로, 두 번째 변환도 같은 `list`에 쌓여 한 번에 처리됨.

---

## `Name(String deobfuscated, String obfuscated)` — private static

```java
private static String Name(String deobfuscated, String obfuscated)
{
    return SmartCorePlugin.isObfuscated ? obfuscated : deobfuscated;
}
```

- `SmartCorePlugin.isObfuscated == true` (프로덕션): obfuscated 이름 반환
- `SmartCorePlugin.isObfuscated == false` (개발환경): deobfuscated(MCP) 이름 반환
- `SmartCorePlugin.injectData()`에서 `isObfuscated`가 설정된 후 `_transformations` static 배열이 초기화되므로 정상 동작

---

## `_transformations` 배열 — 4개 변환 명세

모두 `private static final SmartCoreTransformation[]`.

### 변환 1: `NetHandlerPlayServer.processPlayer`

| 항목 | deobfuscated | obfuscated |
|------|-------------|-----------|
| 클래스명 | `net.minecraft.network.NetHandlerPlayServer` | (없음 — 하드코딩) |
| 메서드명 | `processPlayer` | `a` |
| 파라미터[0] | `net.minecraft.network.play.client.C03PacketPlayer` | `jd` |

```java
new SmartCoreTransformation(
    "net.minecraft.network.NetHandlerPlayServer",            // className — 하드코딩 (Name() 미사용)
    Name("processPlayer", "a"),                              // methodName
    new String[] {
        Name("net.minecraft.network.play.client.C03PacketPlayer", "jd")  // parameterTypeNames[0]
    },
    null,                                                    // returnType → "V" (void)
    "net.smart.core.SmartCoreEventHandler",                  // hookClassName
    "NetHandlerPlayServer_beforeProcessPlayer",               // beforeHookMethodName
    "NetHandlerPlayServer_afterProcessPlayer"                 // afterHookMethodName
)
```

**주목:** `className`은 `Name()` 없이 `"net.minecraft.network.NetHandlerPlayServer"` 하드코딩.  
`transformedName`과 비교에 사용되며, FML이 `transformedName`을 deobfuscated로 제공하므로 항상 deobfuscated로 고정.

---

### 변환 2: `NetHandlerPlayServer.processPlayerBlockPlacement`

| 항목 | deobfuscated | obfuscated |
|------|-------------|-----------|
| 클래스명 | `net.minecraft.network.NetHandlerPlayServer` | (하드코딩) |
| 메서드명 | `processPlayerBlockPlacement` | `a` |
| 파라미터[0] | `net.minecraft.network.play.client.C08PacketPlayerBlockPlacement` | `jo` |

```java
new SmartCoreTransformation(
    "net.minecraft.network.NetHandlerPlayServer",
    Name("processPlayerBlockPlacement", "a"),
    new String[] {
        Name("net.minecraft.network.play.client.C08PacketPlayerBlockPlacement", "jo")
    },
    null,
    "net.smart.core.SmartCoreEventHandler",
    "NetHandlerPlayServer_beforeProcessPlayerBlockPlacement",
    "NetHandlerPlayServer_afterProcessPlayerBlockPlacement"
)
```

**주목:** `processPlayer`와 `processPlayerBlockPlacement` 둘 다 obfuscated 메서드명이 `"a"`. 파라미터 타입 디스크립터로 구별.

---

### 변환 3: `PlayerControllerMP.onPlayerRightClick`

| 항목 | deobfuscated | obfuscated |
|------|-------------|-----------|
| 클래스명 | `net.minecraft.client.multiplayer.PlayerControllerMP` | `bje` |
| 메서드명 | `onPlayerRightClick` | `a` |
| 파라미터 (8개) | 아래 표 참조 | 아래 표 참조 |
| 반환 타입 | `boolean` | — |

**파라미터 8개:**

| 인덱스 | deobfuscated | obfuscated |
|--------|-------------|-----------|
| 0 | `net.minecraft.entity.player.EntityPlayer` | `yz` |
| 1 | `net.minecraft.world.World` | `ahb` |
| 2 | `net.minecraft.item.ItemStack` | `add` |
| 3 | `int` | — (primitive, Name() 미사용) |
| 4 | `int` | — |
| 5 | `int` | — |
| 6 | `int` | — |
| 7 | `net.minecraft.util.Vec3` | `azw` |

```java
new SmartCoreTransformation(
    Name("net.minecraft.client.multiplayer.PlayerControllerMP", "bje"),
    Name("onPlayerRightClick", "a"),
    new String[] {
        Name("net.minecraft.entity.player.EntityPlayer", "yz"),
        Name("net.minecraft.world.World", "ahb"),
        Name("net.minecraft.item.ItemStack", "add"),
        "int",
        "int",
        "int",
        "int",
        Name("net.minecraft.util.Vec3", "azw")
    },
    "boolean",
    "net.smart.core.SmartCoreEventHandler",
    "PlayerControllerMP_beforeOnPlayerRightClick",
    "PlayerControllerMP_afterOnPlayerRightClick"
)
```

**주목:** 클라이언트 전용 클래스(`PlayerControllerMP`)이므로 `className`도 `Name()`으로 obf/deobf 선택.  
`returnType = "boolean"` — 이 변환에서만 non-void.

---

### 변환 4: `ItemInWorldManager.activateBlockOrUseItem`

| 항목 | deobfuscated | obfuscated |
|------|-------------|-----------|
| 클래스명 | `net.minecraft.server.management.ItemInWorldManager` | `qx` |
| 메서드명 | `activateBlockOrUseItem` | `a` |
| 파라미터 (10개) | 아래 표 참조 | — |
| 반환 타입 | `boolean` | — |

**파라미터 10개:**

| 인덱스 | deobfuscated | obfuscated |
|--------|-------------|-----------|
| 0 | `net.minecraft.entity.player.EntityPlayer` | `yz` |
| 1 | `net.minecraft.world.World` | `ahb` |
| 2 | `net.minecraft.item.ItemStack` | `add` |
| 3 | `int` | — |
| 4 | `int` | — |
| 5 | `int` | — |
| 6 | `int` | — |
| 7 | `float` | — |
| 8 | `float` | — |
| 9 | `float` | — |

```java
new SmartCoreTransformation(
    Name("net.minecraft.server.management.ItemInWorldManager", "qx"),
    Name("activateBlockOrUseItem", "a"),
    new String[] {
        Name("net.minecraft.entity.player.EntityPlayer", "yz"),
        Name("net.minecraft.world.World", "ahb"),
        Name("net.minecraft.item.ItemStack", "add"),
        "int",
        "int",
        "int",
        "int",
        "float",
        "float",
        "float"
    },
    "boolean",
    "net.smart.core.SmartCoreEventHandler",
    "ItemInWorldManager_beforeActivateBlockOrUseItem",
    "ItemInWorldManager_afterActivateBlockOrUseItem"
)
```

---

## 4개 변환 명세 요약표

| # | 대상 클래스 | 메서드 (deobf/obf) | 반환 | 실행 위치 |
|---|------------|-------------------|------|-----------|
| 1 | `NetHandlerPlayServer` | `processPlayer` / `a` | void | 서버 |
| 2 | `NetHandlerPlayServer` | `processPlayerBlockPlacement` / `a` | void | 서버 |
| 3 | `PlayerControllerMP` (obf: `bje`) | `onPlayerRightClick` / `a` | boolean | 클라이언트 |
| 4 | `ItemInWorldManager` (obf: `qx`) | `activateBlockOrUseItem` / `a` | boolean | 서버 |

---

## import

```java
import java.util.*;                         // List, ArrayList
import net.minecraft.launchwrapper.*;       // IClassTransformer
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IClassTransformer` (launchwrapper) | 구현 인터페이스. FML이 `transform()` 호출 |
| `SmartCorePlugin.isObfuscated` | `Name()` 헬퍼에서 obf/deobf 이름 선택 |
| `SmartCoreTransformation` | 변환 명세 객체 — 배열에 4개 보유 |
| `SmartCoreClassVisitor.transform()` | 실제 바이트코드 변환 위임 |
| `SmartCoreEventHandler` | 문자열로 참조. hook 메서드 실제 구현체 |

---

## className이 Name()을 쓰는 경우와 안 쓰는 경우

`transform()`에서 비교하는 `transformedName`은 FML이 deobfuscated 이름으로 제공한다.  
따라서 `className`은 항상 deobfuscated로 고정해도 되는데, 3번/4번 변환에서는 `Name(deobf, obf)`를 사용한다.

- 변환 1·2 (`NetHandlerPlayServer`): `className` 하드코딩 deobf, 메서드/파라미터는 `Name()` 사용
- 변환 3·4 (`PlayerControllerMP`, `ItemInWorldManager`): `className`도 `Name()` 사용

FML이 `transformedName`을 항상 deobfuscated로 제공한다면, 변환 3·4에서 `className`에 `Name()`을 사용해도 개발환경에서는 deobf 이름을 선택하므로 결과가 같다. 일관성 없는 스타일로 보임 — 동작에는 영향 없음.

---

## 주요 관찰 사항

1. **`transformedName` vs `name`**: `transform()`이 `name`은 무시하고 `transformedName`만 비교. FML이 SRG 이름으로 제공하는 `transformedName` 기준으로 매칭.

2. **lazy list 초기화**: `(list == null ? (list = new ArrayList<>()) : list).add(transformation)` — 매칭이 없으면 `List` 객체를 만들지 않음.

3. **obf 메서드명 충돌**: 변환 1·2·3·4 모두 obfuscated 메서드명이 `"a"`. 실제 구별은 `getMethodDesc()`에서 파라미터 디스크립터로 이루어짐 (`SmartCoreClassVisitor.visitMethod()` 단계).

4. **`NetHandlerPlayServer`**: `className`이 `Name()` 없이 하드코딩 — 변환 3·4와 일관성 없음. 그러나 FML `transformedName`이 항상 deobf이므로 기능상 문제 없음.

5. **1.21.1 이식**: 이 클래스 전체 불필요. Mixin이 대체:
   - `NetHandlerPlayServer` → `ServerPlayNetworkHandler` 등 1.21.1 대응 클래스에 `@Mixin` 적용
   - `processPlayer` / `processPlayerBlockPlacement` / `onPlayerRightClick` / `activateBlockOrUseItem` 각각 `@Inject` 대상 메서드 확인 필요
   - obf/deobf 이름 선택 로직 → Mixin이 자동 처리 (Fabric은 항상 intermediary/yarn 이름 사용)
