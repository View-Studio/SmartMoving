# SmartMovingFactory.java (net.smart.moving.playerapi) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/playerapi/SmartMovingFactory.java  
패키지: `net.smart.moving.playerapi`  
종류: `class`  
상속: `net.smart.moving.SmartMovingFactory` (extends)

주의: 같은 이름의 `net.smart.moving.SmartMovingFactory`와 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.playerapi;

import net.minecraft.entity.player.*;

import net.smart.moving.*;

public class SmartMovingFactory extends net.smart.moving.SmartMovingFactory
{
    public static void initialize()
    {
        if(!isInitialized())
            new SmartMovingFactory();
    }

    @Override
    protected net.smart.moving.SmartMoving doGetInstance(EntityPlayer entityPlayer)
    {
        net.smart.moving.SmartMoving moving = super.doGetInstance(entityPlayer);
        if(moving != null)
            return moving;

        IEntityPlayerSP playerBase = SmartMoving.getPlayerBase(entityPlayer);
        if(playerBase != null)
            return playerBase.getMoving();

        return null;
    }
}
```

---

## 역할

`net.smart.moving.SmartMovingFactory`를 확장하여 PlayerAPI 기반 클라이언트 플레이어(`EntityPlayerSP`)에서도 `SmartMoving` 인스턴스를 조회할 수 있게 한다. 클라이언트 측에서 초기화하는 Factory 구현체.

---

## import

```java
import net.minecraft.entity.player.*;  // EntityPlayer
import net.smart.moving.*;             // IEntityPlayerSP, net.smart.moving.SmartMoving (부모)
```

`SmartMoving` — 같은 패키지의 `net.smart.moving.playerapi.SmartMoving`을 비정규 이름으로 참조. `net.smart.moving.SmartMoving`은 정규화된 이름(`net.smart.moving.SmartMoving`)으로 구분.

---

## 메서드

### `initialize()`

```java
public static void initialize()
{
    if(!isInitialized())
        new SmartMovingFactory();
}
```

- `isInitialized()`: 부모 `net.smart.moving.SmartMovingFactory`에서 제공 — `instance != null` 여부
- `new SmartMovingFactory()`: 부모 생성자에서 `instance = this`로 등록 (SmartMovingFactory 리서치에서 확인됨)

이미 초기화되었으면(instance != null) 아무 일도 하지 않는다. 클라이언트 측 모드 초기화(`SmartMovingInstall` 또는 `SmartMovingMod`)에서 호출.

---

### `doGetInstance(EntityPlayer entityPlayer)` (override)

```java
@Override
protected net.smart.moving.SmartMoving doGetInstance(EntityPlayer entityPlayer)
{
    net.smart.moving.SmartMoving moving = super.doGetInstance(entityPlayer);
    if(moving != null)
        return moving;

    IEntityPlayerSP playerBase = SmartMoving.getPlayerBase(entityPlayer);
    if(playerBase != null)
        return playerBase.getMoving();

    return null;
}
```

**조회 순서**:

1. `super.doGetInstance(entityPlayer)` — 부모(`net.smart.moving.SmartMovingFactory`)의 기본 구현 먼저 시도  
   → `EntityPlayerMP` 기반 서버 플레이어에서 `IEntityPlayerMP.getMoving()` 조회 (SmartMovingFactory 리서치에서 확인됨)

2. `moving != null`이면 즉시 반환

3. `moving == null`이면 → `SmartMoving.getPlayerBase(entityPlayer)` 호출  
   → `entityPlayer instanceof EntityPlayerSP`이면 `SmartMovingPlayerBase.getPlayerBase(EntityPlayerSP)` → `IEntityPlayerSP` 반환  
   → 아니면 null

4. `playerBase != null`이면 → `playerBase.getMoving()` → `net.smart.moving.SmartMoving` 반환

5. 모두 실패하면 `null` 반환

**확장 의도**: 부모 Factory는 `EntityPlayerMP`(서버 플레이어) 기반 조회를 처리. 이 서브클래스가 `EntityPlayerSP`(클라이언트 로컬 플레이어) 기반 조회를 추가로 담당.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.moving.SmartMovingFactory` | 상속 — `isInitialized()`, `doGetInstance()` 기본 구현, `instance` 등록 |
| `net.smart.moving.playerapi.SmartMoving` | `getPlayerBase(EntityPlayer)` → `IEntityPlayerSP` 조회 |
| `IEntityPlayerSP` | `getMoving()` 호출 경로 |
| `net.smart.moving.SmartMoving` | 반환 타입 |
| `EntityPlayer` | 파라미터 타입 |

---

## 주요 관찰 사항

1. **두 단계 조회 패턴**: `super → PlayerBase` 순서. 부모가 서버 플레이어(MP), 이 클래스가 클라이언트 플레이어(SP)를 담당하도록 역할 분담.

2. **이름 충돌 회피**: `net.smart.moving.SmartMoving`과 `net.smart.moving.playerapi.SmartMoving`이 동명. 반환 타입은 정규화 이름(`net.smart.moving.SmartMoving`)으로, 메서드 호출은 비정규 이름(`SmartMoving.getPlayerBase`)으로 구분.

3. **`initialize()` 멱등성**: `isInitialized()` 검사로 중복 초기화 방지. 여러 곳에서 호출되어도 안전.

4. **1.21.1 이식 관련**:
   - PlayerAPI + Factory 패턴 전체가 Fabric Mixin으로 대체됨
   - `doGetInstance()` 로직 — Mixin 환경에서는 플레이어 엔티티에 직접 SmartMoving 인스턴스를 첨부(attach)하는 방식으로 대체 (`@Unique` 필드 또는 별도 Map)
   - `initialize()` 호출 지점 → `ClientModInitializer.onInitializeClient()`
