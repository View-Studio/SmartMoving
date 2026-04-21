# SmartMoving.java (net.smart.moving.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/playerapi/SmartMoving.java  
패키지: `net.smart.moving.playerapi`  
종류: `abstract class`  
상속: `Object` (직접)

주의: 같은 이름의 `net.smart.moving.SmartMoving`과 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.playerapi;

import net.minecraft.client.entity.*;
import net.minecraft.entity.player.*;
import net.smart.moving.*;

public abstract class SmartMoving
{
    public static final String SPC_ID = "Single Player Commands";

    public static void register()
    {
        SmartMovingPlayerBase.registerPlayerBase();
        SmartMovingServerPlayerBase.registerPlayerBase();
    }

    public static IEntityPlayerSP getPlayerBase(EntityPlayer entityPlayer)
    {
        if(entityPlayer instanceof EntityPlayerSP)
            return SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayer);
        return null;
    }

    public static IEntityPlayerMP getServerPlayerBase(EntityPlayer entityPlayer)
    {
        if(entityPlayer instanceof EntityPlayerMP)
            return SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
        return null;
    }
}
```

---

## 역할

PlayerAPI 등록과 플레이어 베이스 인스턴스 조회를 위한 정적 진입점 클래스. `abstract`로 선언되어 인스턴스화 불가. 모든 멤버가 `static`.

---

## import

```java
import net.minecraft.client.entity.*;  // EntityPlayerSP
import net.minecraft.entity.player.*;  // EntityPlayer, EntityPlayerMP
import net.smart.moving.*;             // IEntityPlayerSP, IEntityPlayerMP
```

---

## 상수

```java
public static final String SPC_ID = "Single Player Commands";
```

Single Player Commands 모드 식별자. `SmartMovingOptions.hasSinglePlayerCommands`와 연관되나, 이 파일 내에서는 선언만 있고 사용되지 않음 — 다른 클래스에서 이 상수를 참조할 것.

---

## 메서드

### `register()`

```java
public static void register()
{
    SmartMovingPlayerBase.registerPlayerBase();
    SmartMovingServerPlayerBase.registerPlayerBase();
}
```

클라이언트(`SmartMovingPlayerBase`)와 서버(`SmartMovingServerPlayerBase`) 양쪽 PlayerBase를 PlayerAPI에 등록. `SmartMovingInstall` 또는 `SmartMovingMod.init()` 등에서 모드 초기화 시 호출될 것 (SmartMovingInstall 리서치에서 확인됨).

---

### `getPlayerBase(EntityPlayer entityPlayer)`

```java
public static IEntityPlayerSP getPlayerBase(EntityPlayer entityPlayer)
{
    if(entityPlayer instanceof EntityPlayerSP)
        return SmartMovingPlayerBase.getPlayerBase((EntityPlayerSP)entityPlayer);
    return null;
}
```

- `entityPlayer`가 `EntityPlayerSP`(클라이언트 로컬 플레이어)이면 → `SmartMovingPlayerBase.getPlayerBase(EntityPlayerSP)`로 위임 → `IEntityPlayerSP` 반환
- 그 외(`EntityPlayerMP` 등)이면 → `null` 반환

`EntityPlayerSP`로 캐스팅 후 전달 — `EntityPlayer` 타입으로 입력받지만 SP에 한해서만 처리.

---

### `getServerPlayerBase(EntityPlayer entityPlayer)`

```java
public static IEntityPlayerMP getServerPlayerBase(EntityPlayer entityPlayer)
{
    if(entityPlayer instanceof EntityPlayerMP)
        return SmartMovingServerPlayerBase.getPlayerBase(entityPlayer);
    return null;
}
```

- `entityPlayer`가 `EntityPlayerMP`(서버 측 플레이어)이면 → `SmartMovingServerPlayerBase.getPlayerBase(EntityPlayer)`로 위임 → `IEntityPlayerMP` 반환
- 그 외이면 → `null` 반환

`getPlayerBase`와 달리 캐스팅 없이 `EntityPlayer` 그대로 전달. `SmartMovingServerPlayerBase.getPlayerBase(EntityPlayer)` 시그니처가 `EntityPlayer` 파라미터를 받음.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingPlayerBase` | 클라이언트 PlayerBase 등록 및 조회 |
| `SmartMovingServerPlayerBase` | 서버 PlayerBase 등록 및 조회 |
| `IEntityPlayerSP` | 클라이언트 플레이어 인터페이스 반환 타입 |
| `IEntityPlayerMP` | 서버 플레이어 인터페이스 반환 타입 |
| `EntityPlayer` | 공통 플레이어 슈퍼타입 (입력) |
| `EntityPlayerSP` | 클라이언트 로컬 플레이어 타입 판별 |
| `EntityPlayerMP` | 서버 플레이어 타입 판별 |

---

## 주요 관찰 사항

1. **`abstract` + 정적 멤버만**: 추상 메서드 없음. `abstract` 선언은 순수하게 인스턴스화 방지 목적.

2. **`getPlayerBase` vs `getServerPlayerBase` 파라미터 타입 차이**: 둘 다 `EntityPlayer`를 받지만 `getPlayerBase`는 내부에서 `(EntityPlayerSP)` 캐스팅하는 반면, `getServerPlayerBase`는 캐스팅 없이 그대로 전달. `SmartMovingServerPlayerBase.getPlayerBase(EntityPlayer)` 시그니처가 `EntityPlayer`를 받도록 설계됨.

3. **`SPC_ID` 미사용**: 이 파일에서 선언되지만 사용 안 됨 — 외부 참조용 상수 공개 지점.

4. **1.21.1 이식 관련**:
   - PlayerAPI 자체가 1.7.10 Forge 전용 — Fabric 1.21.1에 존재하지 않음
   - `EntityPlayerSP` / `EntityPlayerMP` → Fabric의 `ClientPlayerEntity` / `ServerPlayerEntity`
   - `registerPlayerBase()` 메커니즘 → Fabric Mixin으로 대체
   - `getPlayerBase()` / `getServerPlayerBase()` 패턴 → Mixin accessor 또는 별도 컴패니언 오브젝트로 대체
