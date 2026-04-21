# Name.java (net.smart.utilities) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/utilities/Name.java  
패키지: `net.smart.utilities`  
종류: `class`  
전체 라인: 42줄  
비고: 헤더 주석에 "Smart Render and Smart Moving" — 두 모드 공유 파일

---

## 역할

메서드/클래스 이름을 세 가지 형태(deobfuscated / forgefuscated / obfuscated)로 묶어 보관하는 단순 데이터 객체. `Reflect.java`에서 리플렉션 대상 메서드/필드를 지정할 때 사용하는 것으로 보인다.

---

## 필드

```java
public final String obfuscated;
public final String forgefuscated;
public final String deobfuscated;
```

세 필드 모두 `public final`. 생성 후 변경 불가.

| 필드 | 의미 |
|------|------|
| `deobfuscated` | 평문(개발 환경) 이름 |
| `forgefuscated` | Forge SRG 이름 (Forge가 부여하는 중간 이름) |
| `obfuscated` | 난독화 이름 (배포 환경) |

---

## 생성자 3개

### `Name(String name)`

```java
public Name(String name) {
    this(name, null);
}
```

`deobfuscatedName = name`, `obfuscatedName = null`로 2인자 생성자 위임.

### `Name(String deobfuscatedName, String obfuscatedName)`

```java
public Name(String deobfuscatedName, String obfuscatedName) {
    this(deobfuscatedName, null, obfuscatedName);
}
```

`forgefuscatedName = null`로 3인자 생성자 위임.

### `Name(String deobfuscatedName, String forgefuscatedName, String obfuscatedName)`

```java
public Name(String deobfuscatedName, String forgefuscatedName, String obfuscatedName) {
    deobfuscated = deobfuscatedName;
    forgefuscated = forgefuscatedName;
    obfuscated = obfuscatedName;
}
```

세 필드 직접 설정.

---

## 의존 관계

없음 (import 없음, 다른 클래스 의존 없음).

---

## 주요 관찰 사항

1. **세 이름 체계**: 1.7.10 Forge 환경에서 리플렉션으로 vanilla 메서드에 접근할 때 개발/SRG/난독화 이름을 모두 제공해야 했음. `Name`은 이 세 가지를 하나의 객체로 묶는 역할.

2. **forgefuscated**: Forge MCP SRG 이름 (`func_XXXXX_a` 형식). 개발 환경에서 SRG 이름을 직접 쓰는 경우는 드물어서 대부분 `null`로 생성됨(1인자/2인자 생성자 경로).

3. **1.21.1 이식 관련**:
   - 1.21.1 Fabric + Yarn 매핑 환경에서는 단일 이름(Yarn 이름)만 필요.
   - `obfuscated` / `forgefuscated` 필드 불필요.
   - Mixin accessor나 Fabric의 `ReflectionHelper` 대체 수단 사용 시 이 클래스 전체가 불필요할 수 있음.
