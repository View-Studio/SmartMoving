# SmartCoreInfo.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreInfo.java  
패키지: `net.smart.core`  
종류: `class`  
상속 없음

---

## 전체 소스

```java
package net.smart.core;

public class SmartCoreInfo
{
    public static final String ModName    = "Smart Core";
    public static final String ModVersion = SmartCorePlugin.Version;
}
```

---

## 역할

SmartCore 모드의 식별 상수를 한 곳에 모아 두는 상수 클래스.  
`SmartCoreContainer.createMetadata()`에서 `meta.name`, `meta.version`으로 사용된다.

---

## 필드

```java
public static final String ModName    = "Smart Core";
public static final String ModVersion = SmartCorePlugin.Version;
```

| 필드 | 값 | 비고 |
|------|----|------|
| `ModName` | `"Smart Core"` | 하드코딩 |
| `ModVersion` | `SmartCorePlugin.Version` | SmartCorePlugin 클래스의 static 필드에서 가져옴 |

---

## SmartCorePlugin에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드 | 파악 경위 |
|------|-----------|
| `SmartCorePlugin.Version` | `SmartCorePlugin.Version` 참조 |

실제 값은 `SmartCorePlugin.java` 리서치에서 확인 필요.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartCorePlugin` | `Version` 필드 참조 |
| `SmartCoreContainer` | `ModName`, `ModVersion` 소비 |

---

## 주요 관찰 사항

1. **SmartRenderInfo와 동일한 패턴**: `SmartRenderInfo`도 `ModName`, `ModId`, `ModVersion` 세 상수를 동일하게 정의. SmartCore는 `ModId` 없이 `ModName`/`ModVersion` 두 개만.

2. **`ModVersion`이 `SmartCorePlugin.Version`을 참조**: 버전 문자열이 플러그인 클래스에 정의됨. FML 코어모드 플러그인 클래스(`IFMLLoadingPlugin` 구현체)에서 버전을 관리하는 패턴.

3. **1.21.1 이식**: `fabric.mod.json`에 `id`, `name`, `version` 선언으로 대체. 이 클래스 불필요.
