# SmartRenderContext.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/SmartRenderContext.java  
패키지: `net.smart.render`  
종류: `abstract class`  
상속: `SmartRenderContext extends SmartRenderUtilities`

---

## 역할

렌더러 등록(FML `RenderingRegistry`)을 담당하는 추상 클래스.  
`SmartRenderUtilities`를 상속하여 상수들을 하위 클래스에 전달하는 상속 계층의 중간 노드 역할도 한다.  
자체 필드나 인스턴스 메서드는 없고, static 메서드 2개만 존재한다.

---

## 상속 계층에서의 위치

```
SmartRenderUtilities          ← 상수 정의 (RadiantToAngle, Whole, Half, Quarter 등)
└── SmartRenderContext         ← 렌더러 등록 메서드
    ├── SmartRenderModel       ← 모델 클래스
    └── SmartRenderRender      ← 렌더러 클래스

ModelRotationRenderer          ← SmartRenderUtilities 상수를 직접 참조
```

이 클래스를 상속하는 SmartRenderModel, SmartRenderRender는 `SmartRenderUtilities`의 모든 상수를 `protected static final`로 사용할 수 있다. 상수의 실제 값은 `SmartRenderUtilities` 리서치 후 확인 필요 — [미확인]

---

## 필드

없음.

---

## 메서드 전체

### `registerRenderers()` — static

```java
public static void registerRenderers()
{
    registerRenderers(net.smart.render.RenderPlayer.class);
}
```

- PlayerAPI 없는 경로의 `RenderPlayer` 클래스를 인자로 `registerRenderers(Class<?>)` 호출
- SmartRenderMod 또는 SmartRenderInstall에서 호출됨 (호출 측은 해당 파일 리서치 후 확인)

---

### `registerRenderers(Class<?> type)` — static

```java
public static void registerRenderers(Class<?> type)
{
    Render render;
    try
    {
        render = (Render)type.newInstance();   // reflection으로 인스턴스 생성
    }
    catch (Exception e)
    {
        return;   // 예외 발생 시 조용히 리턴 (로그 없음)
    }

    RenderingRegistry.registerEntityRenderingHandler(EntityPlayerSP.class, render);
    RenderingRegistry.registerEntityRenderingHandler(EntityOtherPlayerMP.class, render);
    render.setRenderManager(RenderManager.instance);
}
```

- FML `RenderingRegistry`(cpw.mods.fml.client.registry)를 사용 — Forge 1.7.10 렌더러 등록 방식
- `EntityPlayerSP`(로컬 플레이어)와 `EntityOtherPlayerMP`(원격 플레이어) 두 엔티티 타입에 동일한 렌더러 등록
- `render.setRenderManager(RenderManager.instance)` — vanilla RenderManager 싱글턴 연결
- `type.newInstance()` 실패 시 예외를 삼키고 return — 렌더러 등록 실패해도 게임 크래시 없음

---

## import 목록

```java
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.entity.*;          // EntityPlayerSP, EntityOtherPlayerMP
import net.minecraft.client.renderer.entity.*; // Render, RenderManager
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartRenderUtilities` | 상위 클래스 — 상수 제공 |
| `RenderingRegistry` (FML) | 엔티티 렌더러 등록 |
| `EntityPlayerSP` (vanilla) | 로컬 플레이어 엔티티 타입 |
| `EntityOtherPlayerMP` (vanilla) | 원격 플레이어 엔티티 타입 |
| `Render` (vanilla) | 렌더러 베이스 타입 |
| `RenderManager` (vanilla) | 렌더 매니저 싱글턴 |
| `net.smart.render.RenderPlayer` | `registerRenderers()` 기본 경로 구현체 |

---

## 주요 관찰 사항

1. **상수는 이 클래스에 없음**: 이전 분석에서 상수(RadiantToAngle 등)가 SmartRenderContext에 있다고 추정했으나, 실제로는 `SmartRenderUtilities`에 있다. SmartRenderContext는 단순히 상속 계층을 통해 전달만 한다.

2. **PlayerAPI 경로 분기**: `registerRenderers(Class<?> type)`이 Class를 파라미터로 받는 이유는, PlayerAPI 경로에서 `SmartRenderRenderPlayerBase`를 등록하는 경우에도 같은 메서드를 재사용하기 위함으로 보임 — [미확인, SmartRenderInstall 리서치 후 확인 필요]

3. **1.21.1 이식 포인트**: FML `RenderingRegistry`는 1.21.1 Fabric에 없음. Fabric에서는 `EntityRendererRegistry.register(EntityType, EntityRendererFactory)`를 사용. `EntityPlayerSP`, `EntityOtherPlayerMP`에 해당하는 1.21.1 타입은 vanilla 리서치 후 확인 필요.
