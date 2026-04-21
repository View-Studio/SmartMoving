# SmartCoreMethodVisitor.java (net.smart.core) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/core/SmartCoreMethodVisitor.java  
패키지: `net.smart.core`  
종류: `class`  
상속: `SmartCoreMethodVisitor extends MethodVisitor` (ObjectWeb ASM)

---

## 역할

`SmartCoreClassVisitor.visitMethod()`에서 반환하는 커스텀 `MethodVisitor`.  
vanilla 메서드의 **진입부**와 **각 return 명령어 직전**에 `SmartCoreEventHandler`의 static hook 메서드를 삽입한다.

삽입 패턴:
- **before hook**: 생성자에서 즉시 삽입 → 메서드 진입 시 실행
- **after hook**: return 계열 opcode 방문 시 삽입 → return 직전 실행

---

## 필드

```java
private final SmartCoreTransformation transformation;
```

---

## 생성자

```java
public SmartCoreMethodVisitor(MethodVisitor paramMethodVisitor, SmartCoreTransformation transformation)
{
    super(262144, paramMethodVisitor);

    this.transformation = transformation;

    invokeHook(transformation.beforeHookMethodName);
}
```

- `super(262144, paramMethodVisitor)`: ASM4, 기반 MethodVisitor로 위임
- `invokeHook(transformation.beforeHookMethodName)`: **생성자에서 즉시 호출** → 원본 메서드 명령어가 방문되기 전에 before hook 바이트코드를 삽입

ASM에서 MethodVisitor 생성자는 메서드 헤더 처리 직후 호출되므로, 이 시점에 `mv.visitVarInsn(...)` 등을 호출하면 메서드 첫 번째 명령어 앞에 삽입된다.

---

## 메서드 전체

### `visitInsn(int opcode)` — override

```java
@Override
public void visitInsn(int opcode)
{
    if(opcode == Opcodes.RETURN    ||
       opcode == Opcodes.IRETURN   ||
       opcode == Opcodes.LRETURN   ||
       opcode == Opcodes.FRETURN   ||
       opcode == Opcodes.DRETURN   ||
       opcode == Opcodes.ARETURN)
        invokeHook(transformation.afterHookMethodName);
    super.visitInsn(opcode);
}
```

- return 계열 opcode 6종 모두 감지
- 해당 opcode 방문 시: `invokeHook(afterHookMethodName)` → after hook 삽입 후 `super.visitInsn(opcode)` 로 원본 return 통과
- return 아닌 opcode: 그냥 `super.visitInsn(opcode)` 위임

**감지하는 return opcode:**

| opcode | 반환 타입 |
|--------|-----------|
| `RETURN` | void |
| `IRETURN` | int/boolean/byte/short/char |
| `LRETURN` | long |
| `FRETURN` | float |
| `DRETURN` | double |
| `ARETURN` | 객체/배열 참조 |

---

### `invokeHook(String hookName)` — private

```java
private void invokeHook(String hookName)
{
    if (hookName == null)
        return;

    mv.visitVarInsn(Opcodes.ALOAD, 0);
    int offset = 1;
    for(int i=0; i<transformation.parameterTypeNames.length; i++)
    {
        String typeName = transformation.parameterTypeNames[i];
        mv.visitVarInsn(LoadOpcode(typeName), offset);
        offset += LoadOffset(typeName);
    }
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, transformation.getHookClassDesc(), hookName, transformation.getHookMethodDesc(), false);
}
```

**삽입되는 바이트코드 순서:**

1. `ALOAD 0` — `this` (메서드가 속한 객체 인스턴스) 스택에 push
2. 각 파라미터를 로컬 변수 테이블에서 순서대로 load:
   - `offset` 시작값 = 1 (`this`가 slot 0이므로)
   - `mv.visitVarInsn(LoadOpcode(typeName), offset)` — 타입에 맞는 load opcode
   - `offset += LoadOffset(typeName)` — long/double은 2슬롯, 나머지는 1슬롯
3. `INVOKESTATIC hookClassDesc hookName hookMethodDesc` — static hook 호출

`hookName == null`이면 즉시 반환 (before-only 또는 after-only 변환 지원).

**예시 — `processPlayer(C03PacketPlayer packet)` 패치:**
```
ALOAD 0        // this (NetHandlerPlayServer)
ALOAD 1        // packet (C03PacketPlayer) — 객체이므로 ALOAD
INVOKESTATIC net/smart/core/SmartCoreEventHandler
              NetHandlerPlayServer_beforeProcessPlayer
              (Lnet/minecraft/network/NetHandlerPlayServer;Lnet/minecraft/network/play/client/C03PacketPlayer;)V
```

---

### `LoadOpcode(String typeName)` → int — private static

```java
private static int LoadOpcode(String typeName)
{
    if(typeName.equals(boolean.class.getName()) ||
       typeName.equals(byte.class.getName())    ||
       typeName.equals(short.class.getName())   ||
       typeName.equals(int.class.getName()))
        return Opcodes.ILOAD;
    if(typeName.equals(long.class.getName()))
        return Opcodes.LLOAD;
    if(typeName.equals(float.class.getName()))
        return Opcodes.FLOAD;
    if(typeName.equals(double.class.getName()))
        return Opcodes.DLOAD;
    return Opcodes.ALOAD;
}
```

**타입 → opcode 매핑:**

| 타입 | opcode |
|------|--------|
| `boolean`, `byte`, `short`, `int` | `ILOAD` |
| `long` | `LLOAD` |
| `float` | `FLOAD` |
| `double` | `DLOAD` |
| 그 외 (객체/배열) | `ALOAD` |

`typeName`은 `boolean.class.getName()` 등 Java primitive 이름 문자열 (`"boolean"`, `"byte"`, `"int"` 등).

---

### `LoadOffset(String typeName)` → int — private static

```java
private static int LoadOffset(String typeName)
{
    if(typeName.equals(long.class.getName()) || typeName.equals(double.class.getName()))
        return 2;
    return 1;
}
```

- `long`, `double`: 로컬 변수 테이블 2슬롯 차지 → offset +2
- 그 외: 1슬롯 → offset +1

---

## SmartCoreTransformation에서 파악된 구조 (이 파일에서 확인된 것만)

| 필드/메서드 | 타입 | 파악 경위 |
|-------------|------|-----------|
| `transformation.beforeHookMethodName` | `String` (nullable) | `invokeHook(transformation.beforeHookMethodName)` |
| `transformation.afterHookMethodName` | `String` (nullable) | `invokeHook(transformation.afterHookMethodName)` |
| `transformation.parameterTypeNames` | `String[]` | `for(int i=0; i<transformation.parameterTypeNames.length; i++)` |
| `transformation.getHookClassDesc()` | method → String | INVOKESTATIC 클래스 디스크립터 |
| `transformation.getHookMethodDesc()` | method → String | INVOKESTATIC 메서드 디스크립터 |
| `transformation.methodName` | `String` | SmartCoreClassVisitor에서 확인 |
| `transformation.getMethodDesc()` | method → String | SmartCoreClassVisitor에서 확인 |

---

## import

```java
import org.objectweb.asm.*;   // MethodVisitor, Opcodes
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `MethodVisitor` (ASM) | 상위 클래스 |
| `Opcodes` (ASM) | ALOAD, ILOAD, LLOAD, FLOAD, DLOAD, INVOKESTATIC, RETURN, IRETURN 등 |
| `SmartCoreTransformation` | 삽입할 hook 이름, 클래스/메서드 디스크립터, 파라미터 타입 정보 |
| `SmartCoreClassVisitor` | `visitMethod()`에서 이 클래스를 생성하여 반환 |
| `SmartCoreEventHandler` | 삽입된 INVOKESTATIC의 실제 대상 클래스 |

---

## 전체 변환 결과 (추상적 예시)

원본 `NetHandlerPlayServer.processPlayer(C03PacketPlayer packet)`:
```java
void processPlayer(C03PacketPlayer packet) {
    // ... original code ...
    return;  // RETURN
}
```

변환 후:
```java
void processPlayer(C03PacketPlayer packet) {
    SmartCoreEventHandler.NetHandlerPlayServer_beforeProcessPlayer(this, packet);  // 삽입
    // ... original code ...
    SmartCoreEventHandler.NetHandlerPlayServer_afterProcessPlayer(this, packet);   // 삽입
    return;
}
```

---

## 주요 관찰 사항

1. **생성자에서 before hook 삽입**: ASM MethodVisitor 생성자 호출 시점에 `mv.visitVarInsn()`을 사용하면 원본 명령어 순회 전에 바이트코드 삽입 효과. 별도의 `visitCode()` override 없이 처리.

2. **모든 return 계열 감지**: void/int/long/float/double/객체 반환 6종 모두 처리. 각 return 직전에 after hook을 삽입하므로 조기 return이 있어도 모두 캡처.

3. **null 허용**: `hookName == null`이면 삽입 생략 → before/after 중 한쪽만 있는 변환 지원.

4. **this 포함 ALOAD 0**: hook 메서드가 `SmartCoreEventHandler.Xxx(NetHandlerPlayServer netServerHandler, C03PacketPlayer packet)` 형태이므로, `this`(= netServerHandler)를 첫 인수로 전달.

5. **1.21.1 이식**: 이 클래스 전체 불필요. Mixin으로 대체:
   - before hook → `@Inject(at = @At("HEAD"))`
   - after hook → `@Inject(at = @At("RETURN"))`
   - ALOAD/파라미터 수동 로딩 → Mixin이 자동 처리
