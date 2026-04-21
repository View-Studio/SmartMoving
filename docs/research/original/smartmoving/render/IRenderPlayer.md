# IRenderPlayer.java (net.smart.moving.render) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/render/IRenderPlayer.java  
패키지: `net.smart.moving.render`  
종류: `interface`  
실행 위치: 클라이언트 (렌더링)

주의: 같은 이름의 `net.smart.render.IRenderPlayer`와 다른 파일.

---

## 전체 소스

```java
package net.smart.moving.render;

import net.minecraft.client.entity.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.*;

public interface IRenderPlayer
{
    void superRenderRenderPlayer(AbstractClientPlayer entityplayer, double d, double d1, double d2, float f, float renderPartialTicks);

    void superRenderRotatePlayer(AbstractClientPlayer entityplayer, float totalTime, float actualRotation, float f2);

    void superRenderRenderPlayerAt(AbstractClientPlayer entityplayer, double d, double d1, double d2);

    void superRenderRenderName(EntityLivingBase par1EntityPlayer, double par2, double par4, double par6);

    RenderManager getRenderManager();

    IModelPlayer getPlayerModelBipedMain();

    IModelPlayer getPlayerModelArmorChestplate();

    IModelPlayer getPlayerModelArmor();

    IModelPlayer[] getPlayerModels();
}
```

---

## 역할

`SmartMovingRender`가 vanilla 렌더 메서드(super 계열)와 렌더러 내부 상태에 접근할 때 사용하는 인터페이스. `SmartMovingRenderPlayerBase`가 구현체이며, `SmartMovingRender.irp` 필드의 타입이다.

---

## import

```java
import net.minecraft.client.entity.*;          // AbstractClientPlayer
import net.minecraft.client.renderer.entity.*; // RenderManager
import net.minecraft.entity.*;                 // EntityLivingBase
```

---

## 메서드 목록

### super 렌더 메서드

| 메서드 | 파라미터 | 역할 |
|--------|----------|------|
| `superRenderRenderPlayer` | `AbstractClientPlayer, double d, double d1, double d2, float f, float renderPartialTicks` | vanilla 플레이어 렌더 |
| `superRenderRotatePlayer` | `AbstractClientPlayer, float totalTime, float actualRotation, float f2` | vanilla 플레이어 회전 |
| `superRenderRenderPlayerAt` | `AbstractClientPlayer, double d, double d1, double d2` | vanilla 플레이어 위치 기준 렌더 |
| `superRenderRenderName` | `EntityLivingBase par1EntityPlayer, double par2, double par4, double par6` | vanilla 이름 태그 렌더 |

### 상태 접근 메서드

| 메서드 | 반환 타입 | 역할 |
|--------|-----------|------|
| `getRenderManager()` | `RenderManager` | 렌더 매니저 접근 (livingPlayer 등) |
| `getPlayerModelBipedMain()` | `IModelPlayer` | 주 바디 모델 |
| `getPlayerModelArmorChestplate()` | `IModelPlayer` | 흉갑 갑옷 레이어 모델 |
| `getPlayerModelArmor()` | `IModelPlayer` | 나머지 갑옷 레이어 모델 |
| `getPlayerModels()` | `IModelPlayer[]` | 모든 모델 레이어 배열 |

---

## 파라미터 주의사항

`superRenderRenderName`의 파라미터 이름이 `par1EntityPlayer`임에도 타입은 `EntityLivingBase`. 플레이어뿐 아니라 `EntityLivingBase` 일반을 받는다.

---

## 사용 관계

```
SmartMovingRenderPlayerBase  ─implements─▶  IRenderPlayer
                                                  │
SmartMovingRender.irp  ────────────────────────▶ │
```

`SmartMovingRender`에서 사용되는 호출 지점:

| `SmartMovingRender` 메서드 | 사용하는 IRenderPlayer 메서드 |
|---------------------------|-------------------------------|
| 생성자 | `getPlayerModelBipedMain()`, `getPlayerModelArmorChestplate()`, `getPlayerModelArmor()` |
| `renderPlayer()` | `getPlayerModels()`, `superRenderRenderPlayer()` |
| `rotatePlayer()` | `superRenderRotatePlayer()` |
| `renderPlayerAt()` | `superRenderRenderPlayerAt()` |
| `renderName()` | `getRenderManager()`, `superRenderRenderName()` |

---

## SmartRender의 동명 인터페이스와 비교

| 항목 | `net.smart.render.IRenderPlayer` | `net.smart.moving.render.IRenderPlayer` (이 파일) |
|------|----------------------------------|--------------------------------------------------|
| 패키지 | net.smart.render | net.smart.moving.render |
| `getPlayerModel*()` 반환 타입 | `net.smart.render.IModelPlayer` | `net.smart.moving.render.IModelPlayer` |
| 구현체 | `SmartRenderRenderPlayerBase` | `SmartMovingRenderPlayerBase` |

---

## 주요 관찰 사항

1. **`SmartMovingRender.irp` 타입**: 이 인터페이스. SM 렌더의 모든 vanilla 접근 경로가 이 인터페이스를 통함.

2. **모델 레이어 분리**: `getPlayerModelBipedMain` / `getPlayerModelArmorChestplate` / `getPlayerModelArmor` 3개 별도 getter + `getPlayerModels()` 전체 배열. 생성자에서 개별 getter로 스케일 타입 설정, 렌더 시 배열로 일괄 플래그 반영.

3. **1.21.1 이식 관련**:
   - PlayerAPI(RenderPlayerBase) → Mixin으로 대체
   - `superRender*()` → `@Invoker` 또는 CallbackInfo로 vanilla 원본 접근
   - `AbstractClientPlayer` → `AbstractClientPlayerEntity`
   - `RenderManager` → `EntityRenderDispatcher`
   - `EntityLivingBase` → `LivingEntity`
   - `getPlayerModels()` → Fabric Armor Layer 시스템과 연동 필요
