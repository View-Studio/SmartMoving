# Name.java (net.smart.utilities) 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/utilities/Name.java  
패키지: `net.smart.utilities`  
종류: `class`  
상속 없음  
라이선스 헤더: "Smart Render and Smart Moving" — **두 모드가 공유하는 파일**

---

## 전체 소스

```java
package net.smart.utilities;

public class Name
{
    public final String obfuscated;
    public final String forgefuscated;
    public final String deobfuscated;

    public Name(String name)
    {
        this(name, null);
    }

    public Name(String deobfuscatedName, String obfuscatedName)
    {
        this(deobfuscatedName, null, obfuscatedName);
    }

    public Name(String deobfuscatedName, String forgefuscatedName, String obfuscatedName)
    {
        deobfuscated = deobfuscatedName;
        forgefuscated = forgefuscatedName;
        obfuscated = obfuscatedName;
    }
}
```

---

## 역할

`Reflect` 유틸리티가 reflection으로 클래스·메서드·필드를 조회할 때 사용하는 이름 컨테이너.  
1.7.10 Minecraft는 실행 환경에 따라 세 가지 이름 체계가 공존한다:
- **deobfuscated**: 개발 환경(MCP)의 사람이 읽을 수 있는 이름
- **forgefuscated**: Forge의 SRG(Searge) 이름 (예: `field_78812_q`, `func_xxxxx_x`)
- **obfuscated**: 프로덕션 런타임의 난독화 이름 (예: `t`, `d`)

`Reflect` 클래스가 실행 환경을 감지하여 세 이름 중 맞는 것을 선택하는 것으로 보임 — [Reflect.java 리서치에서 확인].

---

## 필드

```java
public final String obfuscated;      // 프로덕션 난독화 이름 (예: "t", "d", "u")
public final String forgefuscated;   // Forge SRG 이름 (예: "field_78812_q", "func_78788_d")
public final String deobfuscated;    // MCP 개발 환경 이름 (예: "compiled", "register")
```

모두 `public final` — 생성 후 불변, 외부에서 직접 읽기.

---

## 생성자 (3개)

### 1-arg: `Name(String name)`

```java
public Name(String name)
{
    this(name, null);
}
```

→ `this(name, null)` → `Name(deobfuscatedName=name, obfuscatedName=null)` → 3-arg 호출  
결과: `deobfuscated = name`, `forgefuscated = null`, `obfuscated = null`

**사용 예** (SmartRenderMod):
```java
new Name("net.smart.render.playerapi.SmartRender")   // 클래스명 — 난독화 없음
new Name("register")                                  // 메서드명 — 난독화 없음
```

---

### 2-arg: `Name(String deobfuscatedName, String obfuscatedName)`

```java
public Name(String deobfuscatedName, String obfuscatedName)
{
    this(deobfuscatedName, null, obfuscatedName);
}
```

→ 3-arg 호출  
결과: `deobfuscated = deobfuscatedName`, `forgefuscated = null`, `obfuscated = obfuscatedName`

---

### 3-arg: `Name(String deobfuscatedName, String forgefuscatedName, String obfuscatedName)`

```java
public Name(String deobfuscatedName, String forgefuscatedName, String obfuscatedName)
{
    deobfuscated = deobfuscatedName;
    forgefuscated = forgefuscatedName;
    obfuscated = obfuscatedName;
}
```

**사용 예** (SmartRenderInstall):
```java
new Name("compiled",          "field_78812_q", "t")   // ModelRenderer.compiled 필드
new Name("compileDisplayList","func_78788_d",  "d")   // ModelRenderer.compileDisplayList 메서드
new Name("displayList",       "field_78811_r", "u")   // ModelRenderer.displayList 필드
```

---

## 생성자 위임 체인

```
Name(name)
  └─ Name(name, null)                           [deobfuscated=name, obfuscatedName=null]
       └─ Name(name, null, null)                [deobfuscated=name, forgefuscated=null, obfuscated=null]

Name(deobfuscatedName, obfuscatedName)
  └─ Name(deobfuscatedName, null, obfuscatedName)
```

---

## 실제 사용 패턴 요약 (다른 리서치 파일에서 확인된 것)

| 생성자 | 용도 |
|--------|------|
| `new Name("클래스전체경로")` | reflection 클래스 로드. 클래스명은 환경 무관하게 동일 |
| `new Name("메서드명")` | reflection 메서드 조회. SRG/obf 이름 없이 deobf만 |
| `new Name(deobf, srg, obf)` | vanilla 필드/메서드 접근. 세 환경 이름 모두 필요 |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `Reflect` (net.smart.utilities) | 이 `Name` 인스턴스를 인수로 받아 reflection 수행 |

---

## 주요 관찰 사항

1. **두 모드 공유**: 라이선스 헤더에 "Smart Render and Smart Moving"이 명시되어 있음. SmartMoving의 `net.smart.utilities.Name`도 동일 파일. PROGRESS.md에서 SmartMoving 쪽 `Name.java`는 별도 항목으로 나열되어 있으나 내용이 동일할 것으로 보임 — [SmartMoving 리서치에서 확인 필요].

2. **`forgefuscated` = SRG 이름**: 1.7.10에서 Forge는 FML 환경에서 SRG 이름(Searge names)을 사용. 개발 시 MCP 이름과 프로덕션 obf 이름 사이의 중간 레이어.

3. **1.21.1 이식**: 1.21.1 Fabric에서는 Yarn 매핑 사용. obfuscated/forgefuscated 구분이 없음 (Fabric은 런타임에 intermediary 이름 사용). `Name` 클래스 자체가 불필요하거나, deobfuscated(Yarn 이름)만 보관하는 단순 래퍼로 축소 가능.
