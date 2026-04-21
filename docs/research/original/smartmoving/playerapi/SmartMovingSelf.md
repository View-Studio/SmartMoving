# SmartMovingSelf.java (net.smart.moving.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/playerapi/SmartMovingSelf.java  
패키지: `net.smart.moving.playerapi`  
종류: `class`  
상속: `net.smart.moving.SmartMovingSelf` (extends)  
실행 위치: 클라이언트

주의: 같은 이름의 `net.smart.moving.SmartMovingSelf`(103KB)와 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.playerapi;

import java.lang.reflect.*;

import api.player.client.*;

import net.minecraft.client.entity.*;
import net.minecraft.entity.player.*;

import net.smart.moving.config.*;
import net.smart.utilities.*;

public class SmartMovingSelf extends net.smart.moving.SmartMovingSelf
{
    public SmartMovingSelf(EntityPlayer sp, SmartMovingPlayerBase playerBase)
    {
        super(sp, playerBase);
    }

    @Override
    public boolean doFlyingAnimation()
    {
        return SmartMovingOptions.hasSinglePlayerCommands && isSPCFlying(esp) || super.doFlyingAnimation();
    }

    public static boolean isSPCFlying(EntityPlayerSP entityPlayer)
    {
        if(!SmartMovingOptions.hasSinglePlayerCommands)
            return false;

        ClientPlayerBase spcPlayerBase = ((IClientPlayerAPI)entityPlayer).getClientPlayerBase(SmartMoving.SPC_ID);
        if(spcPlayerBase == null)
            return false;

        if(playerHelperField == null)
            playerHelperField = Reflect.GetField(spcPlayerBase.getClass(), new Name("ph"), false);
        if(playerHelperField == null)
            return false;

        Object playerHelper = Reflect.GetField(playerHelperField, spcPlayerBase);
        if(playerHelper == null)
            return false;

        if(flyingField == null)
            flyingField = Reflect.GetField(playerHelper.getClass(), new Name("flying"), false);
        if(flyingField == null)
            return false;

        Object isFlying = Reflect.GetField(flyingField, playerHelper);
        if(isFlying == null)
            return false;

        return isFlying instanceof Boolean && (Boolean)isFlying;
    }

    private static Field playerHelperField = null;
    private static Field flyingField = null;
}
```

---

## 역할

`net.smart.moving.SmartMovingSelf`의 PlayerAPI 클라이언트 확장. Single Player Commands(SPC) 모드와의 호환을 위해 `doFlyingAnimation()`을 오버라이드한다. SPC 모드가 관리하는 비행 상태를 리플렉션으로 읽어 SmartMoving 비행 애니메이션 조건에 추가한다.

`SmartMovingPlayerBase` 생성자에서 `new SmartMovingSelf(player, this)`로 생성됨.

---

## import

```java
import java.lang.reflect.*;          // Field
import api.player.client.*;          // ClientPlayerBase, IClientPlayerAPI
import net.minecraft.client.entity.*; // EntityPlayerSP
import net.minecraft.entity.player.*; // EntityPlayer
import net.smart.moving.config.*;    // SmartMovingOptions
import net.smart.utilities.*;        // Reflect, Name
```

---

## 정적 필드

```java
private static Field playerHelperField = null;
private static Field flyingField = null;
```

리플렉션으로 얻은 Field 객체를 캐싱. 처음 조회 후 재사용 — 매 호출마다 리플렉션 탐색 비용 방지.

---

## 생성자

```java
public SmartMovingSelf(EntityPlayer sp, SmartMovingPlayerBase playerBase)
{
    super(sp, playerBase);
}
```

`net.smart.moving.SmartMovingSelf(EntityPlayer, IEntityPlayerSP)` 생성자에 위임. `SmartMovingPlayerBase`는 `IEntityPlayerSP`를 구현하므로 두 번째 인자로 전달 가능.

---

## 메서드

### `doFlyingAnimation()` (override)

```java
@Override
public boolean doFlyingAnimation()
{
    return SmartMovingOptions.hasSinglePlayerCommands && isSPCFlying(esp) || super.doFlyingAnimation();
}
```

**조건**: `(SPC 있음 && SPC 비행 중)` OR `부모의 doFlyingAnimation()`

- `SmartMovingOptions.hasSinglePlayerCommands`: SPC 모드 존재 여부 (`initialize()`에서 설정된 static boolean)
- `isSPCFlying(esp)`: SPC PlayerBase의 `ph.flying` 필드 리플렉션으로 읽기
- `esp`: 부모 `net.smart.moving.SmartMovingSelf`의 `EntityPlayerSP` 필드

SPC가 없거나 SPC 비행 중이 아니면 `super.doFlyingAnimation()`으로 폴백.

연산자 우선순위: `&&`가 `||`보다 높으므로 `(A && B) || C` 로 평가.

---

### `isSPCFlying(EntityPlayerSP entityPlayer)` (static)

```java
public static boolean isSPCFlying(EntityPlayerSP entityPlayer)
{
    if(!SmartMovingOptions.hasSinglePlayerCommands)
        return false;

    ClientPlayerBase spcPlayerBase =
        ((IClientPlayerAPI)entityPlayer).getClientPlayerBase(SmartMoving.SPC_ID);
    if(spcPlayerBase == null)
        return false;

    if(playerHelperField == null)
        playerHelperField = Reflect.GetField(spcPlayerBase.getClass(), new Name("ph"), false);
    if(playerHelperField == null)
        return false;

    Object playerHelper = Reflect.GetField(playerHelperField, spcPlayerBase);
    if(playerHelper == null)
        return false;

    if(flyingField == null)
        flyingField = Reflect.GetField(playerHelper.getClass(), new Name("flying"), false);
    if(flyingField == null)
        return false;

    Object isFlying = Reflect.GetField(flyingField, playerHelper);
    if(isFlying == null)
        return false;

    return isFlying instanceof Boolean && (Boolean)isFlying;
}
```

**SPC 비행 상태 접근 경로**:

```
EntityPlayerSP (IClientPlayerAPI로 캐스팅)
  └─ getClientPlayerBase("Single Player Commands")  → ClientPlayerBase (SPC PlayerBase)
       └─ "ph" 필드 (리플렉션)                      → Object (SPC PlayerHelper 인스턴스)
            └─ "flying" 필드 (리플렉션)             → Object (Boolean)
                 └─ (Boolean) cast                  → true/false
```

**조회 단계별 null 방어**:

| 단계 | 실패 조건 | 반환 |
|------|-----------|------|
| 1 | `hasSinglePlayerCommands == false` | `false` |
| 2 | SPC PlayerBase 미등록 | `false` |
| 3 | `"ph"` 필드 없음 | `false` |
| 4 | `ph` 필드 값 null | `false` |
| 5 | `"flying"` 필드 없음 | `false` |
| 6 | `flying` 필드 값 null | `false` |
| 7 | `flying` 값이 Boolean 아님 | `false` |

**`Reflect.GetField(class, name, false)` 세 번째 인자 `false`**: 예외 발생 없이 실패 시 null 반환. (net.smart.utilities.Reflect 리서치에서 확인됨)

**`new Name("ph")` / `new Name("flying")`**: SmartMoving의 `Name` 유틸리티로 필드 이름을 감싸 난독화 처리된 이름도 처리 가능하게 함. (net.smart.utilities.Name 리서치에서 확인됨)

**`SmartMoving.SPC_ID`**: `net.smart.moving.playerapi.SmartMoving.SPC_ID = "Single Player Commands"` — SPC PlayerBase 등록 키.

**캐싱 패턴**: `playerHelperField == null`이면 탐색, null이 아니면 캐시된 Field 재사용. SPC PlayerBase 클래스와 PlayerHelper 클래스가 런타임에 변경되지 않으므로 안전.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.moving.SmartMovingSelf` | 상속 — `esp`(EntityPlayerSP) 필드, `doFlyingAnimation()` 기본 구현 |
| `SmartMovingOptions.hasSinglePlayerCommands` | SPC 존재 여부 static 플래그 |
| `SmartMoving.SPC_ID` | `"Single Player Commands"` — SPC PlayerBase 조회 키 |
| `IClientPlayerAPI` | `EntityPlayerSP`에서 PlayerBase 조회 |
| `ClientPlayerBase` | SPC PlayerBase 타입 |
| `Reflect.GetField(Class, Name, boolean)` | 리플렉션 필드 탐색 |
| `Reflect.GetField(Field, Object)` | 리플렉션 필드 값 읽기 |
| `Name` | 필드 이름 래퍼 |

---

## 주요 관찰 사항

1. **최소 오버라이드**: 부모 `net.smart.moving.SmartMovingSelf` 대비 추가된 것은 `doFlyingAnimation()` override 하나뿐. SPC 호환 로직만 추가.

2. **리플렉션 2단계 접근**: SPC PlayerBase → `ph`(PlayerHelper) → `flying`. SPC 내부 구조가 외부에 노출되지 않으므로 리플렉션 불가피. `false` 옵션으로 필드 없을 때 예외 없이 graceful fallback.

3. **`esp` 필드**: 부모 `net.smart.moving.SmartMovingSelf`에 선언된 `EntityPlayerSP esp` 필드. `doFlyingAnimation()`에서 직접 참조.

4. **`static` isSPCFlying**: 인스턴스 없이 호출 가능 — 다른 클래스에서도 SPC 비행 상태를 조회할 때 재사용 가능한 구조.

5. **1.21.1 이식 관련**:
   - SPC 모드는 1.7.10 전용 → 1.21.1에서 `hasSinglePlayerCommands`는 항상 false이므로 이 파일 전체가 사실상 무효화
   - `doFlyingAnimation()` override는 `super.doFlyingAnimation()`만 호출하도록 단순화 가능
   - PlayerAPI(`IClientPlayerAPI`, `ClientPlayerBase`) → Mixin으로 대체
   - `isSPCFlying()` 메서드 전체 제거 또는 빈 stub으로 대체
