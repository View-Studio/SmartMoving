# SmartRenderInfo.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderInfo.java  
패키지: `net.smart.render`  
종류: 일반 클래스 (상속 없음)

---

## 역할

SmartRender 모드의 식별 정보(modid, name, version)를 상수로 제공하는 클래스.  
`SmartRenderMod.class`에 붙은 FML `@Mod` 어노테이션을 런타임에 읽어서 상수를 초기화한다.  
메서드 없음. 생성자 없음(기본 생성자만). 순수 상수 홀더.

- **실행 위치**: 클라이언트/서버 공통 (상수 초기화 시점에 실행)

---

## 필드 전체

```java
private static final Mod Mod = SmartRenderMod.class.getAnnotation(Mod.class);

public static final String ModId      = Mod.modid();
public static final String ModName    = Mod.name();
public static final String ModVersion = Mod.version();
```

- `SmartRenderMod.class.getAnnotation(Mod.class)` — `SmartRenderMod`에 붙은 `@Mod` 어노테이션 객체를 reflection으로 획득
- `ModId`, `ModName`, `ModVersion`의 실제 값은 `SmartRenderMod`의 `@Mod` 어노테이션에 정의됨 — [미확인, SmartRenderMod 리서치 후 확인]

---

## import

```java
import cpw.mods.fml.common.*;   // Mod 어노테이션 타입
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderMod` | `@Mod` 어노테이션 소스 클래스 |
| `cpw.mods.fml.common.Mod` | FML 어노테이션 타입 |

---

## 주요 관찰 사항

1. **1.21.1 이식 포인트**: FML `@Mod`는 1.21.1 Fabric에 없음. Fabric에서는 `fabric.mod.json`이 mod 메타데이터를 담고, `FabricLoader.getInstance().getModContainer(modid)`로 접근. 이 상수 클래스 자체는 단순히 문자열만 제공하므로 하드코딩 또는 `FabricLoader` 방식으로 교체 가능.
