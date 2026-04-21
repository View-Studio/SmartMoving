# SmartMovingTestCommand.java (net.smart.moving.test) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/test/SmartMovingTestCommand.java  
패키지: `net.smart.moving.test`  
종류: `class`  
상속: `net.minecraft.command.CommandBase` (extends)  
실행 위치: 클라이언트 또는 서버 (명령어)

---

## 전체 소스

```java
package net.smart.moving.test;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

public class SmartMovingTestCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "smartmovingtest";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if(args.length == 1) {
            if(args[0].equals("run")) {
                sender.addChatMessage(new ChatComponentText(
                    ">> Starting test. Keep the game focused, and don't touch anything! I hope you're on a superflat world with a spawn-proof block."));
                SmartMovingTestMod.instance.startTest();
                return;
            }
        }
        throw new WrongUsageException(getCommandName() + " run");
    }
}
```

---

## 역할

SM 자동 테스트를 시작하는 인게임 명령어 `/smartmovingtest run` 구현체. 명령어 처리 시 `SmartMovingTestMod.instance.startTest()`를 호출한다.

---

## import

```java
import java.util.List;                          // 미사용 (import만 있음)
import net.minecraft.command.CommandBase;        // 상속
import net.minecraft.command.ICommandSender;    // 명령어 전송자
import net.minecraft.command.WrongUsageException; // 잘못된 사용 예외
import net.minecraft.util.ChatComponentText;    // 채팅 메시지
```

`java.util.List`는 import되어 있으나 코드 내에서 사용되지 않는다.

---

## 메서드

### `getCommandName()`

```java
@Override
public String getCommandName() {
    return "smartmovingtest";
}
```

명령어 이름: `/smartmovingtest`

### `getCommandUsage(ICommandSender sender)`

```java
@Override
public String getCommandUsage(ICommandSender sender) {
    return "";
}
```

사용법 문자열: 빈 문자열.

### `getRequiredPermissionLevel()`

```java
public int getRequiredPermissionLevel()
{
    return 0;
}
```

권한 레벨 `0` — 모든 플레이어 사용 가능(`@Override` 없음, `CommandBase`의 기본값 재정의).

### `processCommand(ICommandSender sender, String[] args)`

```java
@Override
public void processCommand(ICommandSender sender, String[] args) {
    if(args.length == 1) {
        if(args[0].equals("run")) {
            sender.addChatMessage(new ChatComponentText(
                ">> Starting test. Keep the game focused, and don't touch anything! I hope you're on a superflat world with a spawn-proof block."));
            SmartMovingTestMod.instance.startTest();
            return;
        }
    }
    throw new WrongUsageException(getCommandName() + " run");
}
```

**유효 입력**: `args.length == 1 && args[0].equals("run")`

**실행 순서**:
1. 채팅에 경고 메시지 출력
2. `SmartMovingTestMod.instance.startTest()` 호출
3. 즉시 return

**잘못된 입력**: `WrongUsageException("smartmovingtest run")` throw

채팅 메시지 전문:
```
>> Starting test. Keep the game focused, and don't touch anything! I hope you're on a superflat world with a spawn-proof block.
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `CommandBase` | 상속 |
| `ICommandSender` | 명령어 전송자 |
| `WrongUsageException` | 잘못된 사용 시 예외 |
| `ChatComponentText` | 채팅 메시지 생성 |
| `SmartMovingTestMod.instance` | 테스트 시작 진입점 |

---

## 주요 관찰 사항

1. **단일 서브커맨드**: `run` 하나만 지원. 다른 인자는 모두 `WrongUsageException`.

2. **`java.util.List` 미사용 import**: 코드에서 사용되지 않는 import. 자동완성 흔적으로 보임.

3. **`@Override` 없는 `getRequiredPermissionLevel()`**: `CommandBase`의 메서드를 재정의하지만 `@Override` 어노테이션 없음.

4. **1.21.1 이식 관련**:
   - `CommandBase` → Fabric의 `LiteralArgumentBuilder` / Brigadier 명령어 시스템
   - `ICommandSender.addChatMessage()` → `CommandContext`의 `sendFeedback()` 또는 유사 API
   - `ChatComponentText` → `Text.literal()`
   - `WrongUsageException` → Brigadier에서는 명령어 등록 시 구조로 처리
