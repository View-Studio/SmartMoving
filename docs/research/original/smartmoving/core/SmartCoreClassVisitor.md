# SmartCoreClassVisitor.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreClassVisitor.java  
패키지: `net.smart.core`  
종류: `final class`  
상속: `SmartCoreClassVisitor extends ClassVisitor` (ObjectWeb ASM)

---

## 역할

ASM `ClassVisitor`를 사용하여 바이트코드를 변환(transform)하는 클래스.  
`List<SmartCoreTransformation>`을 받아 각 transformation에 지정된 메서드를 방문할 때 `SmartCoreMethodVisitor`로 교체하여 바이트코드 수정을 수행한다.

FML(Forge Mod Loader)의 클래스 변환 파이프라인에서 `SmartCoreTransformer` → `SmartCoreClassVisitor` → `SmartCoreMethodVisitor` 순으로 호출된다.

- **실행 위치**: 클래스 로딩 시 (변환 단계, 게임 진입 전)

---

## 전체 소스

```java
package net.smart.core;

import java.io.*;
import java.util.*;
import org.objectweb.asm.*;

public final class SmartCoreClassVisitor extends ClassVisitor
{
    public static byte[] transform(byte[] bytes, List<SmartCoreTransformation> transformations)
    {
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ClassReader cr = new ClassReader(in);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            SmartCoreClassVisitor p = new SmartCoreClassVisitor(cw, transformations);

            cr.accept(p, 0);

            byte[] result = cw.toByteArray();
            in.close();
            return result;
        }
        catch(IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
    }

    private final List<SmartCoreTransformation> transformations;

    public SmartCoreClassVisitor(ClassVisitor classVisitor, List<SmartCoreTransformation> transformations)
    {
        super(262144, classVisitor);
        this.transformations = transformations;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
        String signature, String[] exceptions)
    {
        MethodVisitor base = super.visitMethod(access, name, desc, signature, exceptions);

        for(SmartCoreTransformation transformation : transformations)
            if(name.equals(transformation.methodName) && desc.equals(transformation.getMethodDesc()))
                return new SmartCoreMethodVisitor(base, transformation);

        return base;
    }
}
```

---

## 필드

```java
private final List<SmartCoreTransformation> transformations;
```

---

## 메서드 전체

### `transform(byte[] bytes, List<SmartCoreTransformation> transformations)` → byte[] — static

```java
public static byte[] transform(byte[] bytes, List<SmartCoreTransformation> transformations)
{
    try
    {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        SmartCoreClassVisitor p = new SmartCoreClassVisitor(cw, transformations);

        cr.accept(p, 0);

        byte[] result = cw.toByteArray();
        in.close();
        return result;
    }
    catch(IOException ioe)
    {
        throw new RuntimeException(ioe);
    }
}
```

**처리 단계:**
1. `ByteArrayInputStream(bytes)` — 원본 바이트코드를 스트림으로
2. `ClassReader(in)` — ASM ClassReader 생성
3. `ClassWriter(ClassWriter.COMPUTE_MAXS)` — 출력용 ClassWriter. `COMPUTE_MAXS` 플래그: max stack/locals 자동 계산 (프레임은 자동 계산 안 함)
4. `new SmartCoreClassVisitor(cw, transformations)` — this 클래스 인스턴스 생성
5. `cr.accept(p, 0)` — ClassReader가 `p`(SmartCoreClassVisitor)를 방문자로 클래스 전체 순회. 플래그 `0` = 특별한 옵션 없음
6. `cw.toByteArray()` — 변환된 바이트코드 추출
7. `in.close()`
8. IOException → RuntimeException 변환

**호출 위치**: `SmartCoreTransformer.transform()` 또는 유사한 FML 변환 진입점 — [SmartCoreTransformer 리서치에서 확인 필요]

---

### 생성자: `SmartCoreClassVisitor(ClassVisitor classVisitor, List<SmartCoreTransformation> transformations)`

```java
public SmartCoreClassVisitor(ClassVisitor classVisitor, List<SmartCoreTransformation> transformations)
{
    super(262144, classVisitor);
    this.transformations = transformations;
}
```

- `super(262144, classVisitor)`:
  - `262144` = `0x40000` = `Opcodes.ASM4` — ASM API 버전 4
  - `classVisitor` = ClassWriter — 변환 결과를 이 visitor에 위임(delegate)
- `transformations`: 적용할 변환 목록 저장

---

### `visitMethod(int access, String name, String desc, String signature, String[] exceptions)` → MethodVisitor — override

```java
@Override
public MethodVisitor visitMethod(int access, String name, String desc,
    String signature, String[] exceptions)
{
    MethodVisitor base = super.visitMethod(access, name, desc, signature, exceptions);

    for(SmartCoreTransformation transformation : transformations)
        if(name.equals(transformation.methodName) && desc.equals(transformation.getMethodDesc()))
            return new SmartCoreMethodVisitor(base, transformation);

    return base;
}
```

**동작:**
1. `super.visitMethod(...)` — ClassWriter에 메서드 위임 → 기본 `MethodVisitor` 획득
2. `transformations` 순회:
   - 조건: `name.equals(transformation.methodName)` AND `desc.equals(transformation.getMethodDesc())`
   - 매칭 시: `new SmartCoreMethodVisitor(base, transformation)` 반환 — 첫 번째 매칭만 적용 후 `return`
3. 매칭 없으면: `base` 그대로 반환 (변환 없음)

**매칭 조건:**
- `transformation.methodName` — 메서드 이름 (예: `"render"`, `"setRotationAngles"`)
- `transformation.getMethodDesc()` — JVM 메서드 디스크립터 (예: `"(Lnet/minecraft/entity/Entity;FFFFFF)V"`)
- 이름과 디스크립터 **둘 다** 일치해야 함

**단일 매칭**: 하나의 메서드에 여러 transformation이 있어도 리스트의 첫 번째만 적용됨.

---

## SmartCoreTransformation에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드/메서드 | 타입 | 파악 경위 |
|-------------|------|-----------|
| `.methodName` | `String` | `name.equals(transformation.methodName)` |
| `.getMethodDesc()` | method → String | `desc.equals(transformation.getMethodDesc())` |

---

## import

```java
import java.io.*;           // ByteArrayInputStream, IOException
import java.util.*;         // List
import org.objectweb.asm.*; // ClassVisitor, ClassReader, ClassWriter, MethodVisitor
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ClassVisitor` (ASM) | 상위 클래스 |
| `ClassReader` (ASM) | 원본 바이트코드 파싱 |
| `ClassWriter` (ASM) | 변환된 바이트코드 생성. `COMPUTE_MAXS` 사용 |
| `MethodVisitor` (ASM) | `visitMethod()` 반환 타입 |
| `SmartCoreTransformation` | 변환 대상 메서드 정보 보관 |
| `SmartCoreMethodVisitor` | 실제 바이트코드 수정 수행 |

---

## 주요 관찰 사항

1. **ASM4 (`262144 = 0x40000`)**: `Opcodes.ASM4` 상수를 직접 리터럴로 사용. ASM 라이브러리의 API 버전 4.

2. **`ClassWriter.COMPUTE_MAXS`**: max stack/locals만 자동 계산. 스택 프레임(Java 6+ 검증용)은 자동 계산하지 않음. 1.7.10 Minecraft가 Java 6 바이트코드이므로 COMPUTE_FRAMES 불필요.

3. **`cr.accept(p, 0)` 플래그 `0`**: `ClassReader.SKIP_DEBUG`, `ClassReader.SKIP_FRAMES`, `ClassReader.EXPAND_FRAMES` 중 어느 것도 사용 안 함. 전체 클래스 정보 그대로 읽음.

4. **단일 변환 적용**: `visitMethod()`에서 첫 번째 매칭 transformation만 적용. 동일 메서드에 여러 transformation을 체이닝하는 구조가 아님.

5. **1.21.1 이식**: Fabric은 ASM 기반 변환 대신 Mixin을 권장. 이 클래스 전체가 Mixin `@Inject`/`@Redirect`/`@ModifyVariable` 등으로 대체됨. `SmartCoreTransformer`/`SmartCoreClassVisitor`/`SmartCoreMethodVisitor` 삼총사는 1.21.1에서 불필요.
