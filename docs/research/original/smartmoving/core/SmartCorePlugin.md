# SmartCorePlugin.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCorePlugin.java  
패키지: `net.smart.core`  
종류: `class`  
구현: `SmartCorePlugin implements IFMLLoadingPlugin`  
어노테이션: `@IFMLLoadingPlugin.MCVersion("1.7.10")`

---

## 전체 소스

```java
package net.smart.core;

import java.util.*;
import cpw.mods.fml.relauncher.*;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class SmartCorePlugin implements IFMLLoadingPlugin
{
    public static String Version = "1.0.3";

    public static boolean isObfuscated;

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] { "net.smart.core.SmartCoreTransformer" };
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

    @Override
    public String getModContainerClass()
    {
        return "net.smart.core.SmartCoreContainer";
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        isObfuscated = (Boolean)data.get("runtimeDeobfuscationEnabled");
    }
}
```

---

## 역할

FML(Forge Mod Loader) 코어모드 플러그인 진입점.  
FML이 이 클래스를 발견하고 `IFMLLoadingPlugin` 인터페이스를 통해 ASM 변환기·모드 컨테이너를 등록한다.

- **실행 위치**: FML 부트스트랩 단계 (게임 진입 전, JVM 클래스 로딩 초기)

---

## 필드

```java
public static String Version = "1.0.3";    // SmartCore 버전. SmartCoreInfo.ModVersion에서 참조
public static boolean isObfuscated;        // 프로덕션(true) vs 개발 환경(false). injectData()에서 설정
```

**`isObfuscated`**: `SmartCoreTransformer`에서 `Name`의 obfuscated/deobfuscated 중 어느 이름을 우선할지 결정하는 데 사용될 것으로 보임 — [SmartCoreTransformer 리서치에서 확인 필요].

---

## 메서드 전체

### `getASMTransformerClass()` → String[]

```java
@Override
public String[] getASMTransformerClass()
{
    return new String[] { "net.smart.core.SmartCoreTransformer" };
}
```

- FML에 `SmartCoreTransformer`를 클래스 변환기로 등록
- FML이 클래스 로딩 시 이 변환기를 거치도록 설정

---

### `getAccessTransformerClass()` → String

```java
@Override
public String getAccessTransformerClass()
{
    return null;
}
```

- 접근 변환기 없음

---

### `getModContainerClass()` → String

```java
@Override
public String getModContainerClass()
{
    return "net.smart.core.SmartCoreContainer";
}
```

- `SmartCoreContainer`를 FML 모드 컨테이너로 등록 → FML 모드 목록에 "Smart Core" 표시

---

### `getSetupClass()` → String

```java
@Override
public String getSetupClass()
{
    return null;
}
```

- 별도 설정 클래스 없음

---

### `injectData(Map<String, Object> data)` — void

```java
@Override
public void injectData(Map<String, Object> data)
{
    isObfuscated = (Boolean)data.get("runtimeDeobfuscationEnabled");
}
```

- FML이 호출하며 환경 정보를 `Map`으로 전달
- `"runtimeDeobfuscationEnabled"`: `true` = 프로덕션(난독화 런타임), `false` = 개발 환경(MCP 이름 사용)
- `(Boolean)` 캐스트 — null이면 NullPointerException 발생 가능 (FML이 항상 이 키를 제공한다는 전제)
- `isObfuscated` static 필드에 저장

---

## FML 코어모드 등록 흐름

```
META-INF/fml_coremods.properties (또는 유사 메커니즘)
  └─ SmartCorePlugin 클래스 참조
       └─ FML: IFMLLoadingPlugin 인터페이스 호출
            ├─ getASMTransformerClass() → SmartCoreTransformer 등록
            ├─ getModContainerClass()   → SmartCoreContainer 등록
            └─ injectData()            → isObfuscated 설정
```

---

## 다른 파일과의 연결 확인

| 이 파일의 값 | 참조 위치 | 용도 |
|-------------|-----------|------|
| `Version = "1.0.3"` | `SmartCoreInfo.ModVersion` | FML 모드 목록 버전 표시 |
| `isObfuscated` | `SmartCoreTransformer` (추정) | Name 해석 시 obf/deobf 선택 |
| `"net.smart.core.SmartCoreTransformer"` | FML 부트스트랩 | 클래스 변환기 등록 |
| `"net.smart.core.SmartCoreContainer"` | FML 부트스트랩 | 모드 컨테이너 등록 |

---

## import

```java
import java.util.*;                  // Map
import cpw.mods.fml.relauncher.*;   // IFMLLoadingPlugin
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IFMLLoadingPlugin` (FML) | 구현 인터페이스 |
| `SmartCoreTransformer` | 문자열로만 참조. FML이 리플렉션으로 로드 |
| `SmartCoreContainer` | 문자열로만 참조. FML이 리플렉션으로 로드 |
| `SmartCoreInfo.ModVersion` | `Version` 필드 소비 |

---

## 주요 관찰 사항

1. **`@IFMLLoadingPlugin.MCVersion("1.7.10")`**: 이 코어모드가 MC 1.7.10 전용임을 FML에 선언.

2. **`Version = "1.0.3"`**: `final`이 아닌 `static String` — 변경 가능하지만 실제 변경 코드는 없음.

3. **`isObfuscated`**: `Reflect`/`SmartCoreTransformer`에서 이 플래그로 obfuscated vs deobfuscated 이름 선택을 조정할 것으로 보임 — [SmartCoreTransformer 리서치에서 확인 필요]. `SmartCoreMethodVisitor`의 `LoadOpcode`는 이미 ASM 바이트코드를 직접 다루므로 이 플래그와 무관.

4. **1.21.1 이식**: `IFMLLoadingPlugin` 전체가 Fabric 아키텍처에서 불필요. `fabric.mod.json` + `FabricLoader` 초기화로 대체. ASM 변환은 Mixin으로 대체.
