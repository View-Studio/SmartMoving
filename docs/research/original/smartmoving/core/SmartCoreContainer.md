# SmartCoreContainer.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreContainer.java  
패키지: `net.smart.core`  
종류: `class`  
상속: `SmartCoreContainer extends DummyModContainer` (FML)

---

## 전체 소스

```java
package net.smart.core;

import java.util.*;
import com.google.common.eventbus.*;
import cpw.mods.fml.common.*;

public class SmartCoreContainer extends DummyModContainer
{
    public SmartCoreContainer()
    {
        super(createMetadata());
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        return true;
    }

    private static ModMetadata createMetadata()
    {
        ModMetadata meta = new ModMetadata();

        meta.modId = "SmartCore";
        meta.name = SmartCoreInfo.ModName;
        meta.version = SmartCoreInfo.ModVersion;
        meta.description = "Adds some core hooks required by Smart Moving";
        meta.url = "http://www.minecraftforum.net/topic/738498-";
        meta.authorList = Arrays.asList(new String[] { "Divisor" });

        return meta;
    }
}
```

---

## 역할

FML(Forge Mod Loader)에 "SmartCore"를 별도 모드 항목으로 등록하는 경량 컨테이너.  
`@Mod` 어노테이션 없이 코어모드(coremod, FML 플러그인)가 FML 모드 목록에 나타나게 하는 용도.  
실제 모드 라이프사이클 이벤트는 처리하지 않음 (`registerBus()`가 버스에 등록 안 함).

- **실행 위치**: FML 초기화 단계 (게임 진입 전)

---

## 메서드 전체

### 생성자

```java
public SmartCoreContainer()
{
    super(createMetadata());
}
```

- `createMetadata()`로 생성한 `ModMetadata`를 `DummyModContainer` 생성자에 전달

---

### `registerBus(EventBus bus, LoadController controller)` → boolean — override

```java
@Override
public boolean registerBus(EventBus bus, LoadController controller)
{
    return true;
}
```

- `DummyModContainer`/`ModContainer` 인터페이스 구현 필수 메서드
- `bus`에 this를 등록하지 않고 `true`만 반환 → FML 라이프사이클 이벤트(`@Subscribe`) 수신 없음
- `true` 반환은 "등록 성공"을 의미 (FML 기대값)

---

### `createMetadata()` → ModMetadata — private static

```java
private static ModMetadata createMetadata()
{
    ModMetadata meta = new ModMetadata();

    meta.modId = "SmartCore";
    meta.name = SmartCoreInfo.ModName;
    meta.version = SmartCoreInfo.ModVersion;
    meta.description = "Adds some core hooks required by Smart Moving";
    meta.url = "http://www.minecraftforum.net/topic/738498-";
    meta.authorList = Arrays.asList(new String[] { "Divisor" });

    return meta;
}
```

**ModMetadata 필드값:**

| 필드 | 값 |
|------|-----|
| `modId` | `"SmartCore"` (하드코딩) |
| `name` | `SmartCoreInfo.ModName` |
| `version` | `SmartCoreInfo.ModVersion` |
| `description` | `"Adds some core hooks required by Smart Moving"` (하드코딩) |
| `url` | `"http://www.minecraftforum.net/topic/738498-"` (하드코딩) |
| `authorList` | `["Divisor"]` (하드코딩, 단일 원소 List) |

---

## SmartCoreInfo에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드 | 타입 | 파악 경위 |
|------|------|-----------|
| `SmartCoreInfo.ModName` | String (추정) | `meta.name = SmartCoreInfo.ModName` |
| `SmartCoreInfo.ModVersion` | String (추정) | `meta.version = SmartCoreInfo.ModVersion` |

— [SmartCoreInfo 리서치에서 실제 값 확인 필요]

---

## import

```java
import java.util.*;                     // Arrays, List
import com.google.common.eventbus.*;    // EventBus (Guava)
import cpw.mods.fml.common.*;           // DummyModContainer, ModMetadata, LoadController
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `DummyModContainer` (FML) | 상위 클래스. 경량 ModContainer 구현 |
| `ModMetadata` (FML) | 모드 메타데이터 보관 |
| `EventBus` (Guava) | `registerBus()` 파라미터 타입 |
| `LoadController` (FML) | `registerBus()` 파라미터 타입 |
| `SmartCoreInfo` | `ModName`, `ModVersion` |

---

## 주요 관찰 사항

1. **`DummyModContainer` 용도**: FML의 정식 `@Mod` 모드가 아닌 코어모드가 FML 모드 목록에 표시되게 하는 표준 패턴. `SmartCorePlugin`(FML 코어모드 플러그인)에서 이 컨테이너를 반환하여 FML에 등록하는 방식으로 보임 — [SmartCorePlugin 리서치에서 확인 필요].

2. **`registerBus()` 빈 구현**: FML 라이프사이클 이벤트(`FMLPreInitializationEvent`, `FMLInitializationEvent` 등) 수신 없음. SmartCore는 FML 이벤트 대신 코어모드 변환 파이프라인(`SmartCoreTransformer`)으로 동작.

3. **저자 "Divisor"**: SmartMoving 원작자. SmartRender의 동일 저자.

4. **1.21.1 이식**: Fabric에는 `DummyModContainer`/`ModContainer` 개념이 없음. `fabric.mod.json`으로 모드 메타데이터를 선언. 이 클래스 전체 불필요.
