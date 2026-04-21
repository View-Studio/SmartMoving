# IRenderPlayer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/IRenderPlayer.java  
패키지: `net.smart.render`  
종류: `interface`

---

## 역할

`SmartRenderRender`가 실제 렌더러 구현체(`RenderPlayer` 또는 PlayerAPI `SmartRenderRenderPlayerBase`)를 호출할 때 사용하는 인터페이스.  
`SmartRenderRender.irp` 필드의 타입이 이 인터페이스이므로, SmartRenderRender는 구현체가 PlayerAPI 경로인지 비-PlayerAPI 경로인지 구분하지 않고 동일하게 호출한다.  
`IModelPlayer`가 모델 측 추상화라면, `IRenderPlayer`는 렌더러 측 추상화다.

- **실행 위치**: 클라이언트 전용

---

## 구현체

| 구현체 | 경로 |
|--------|------|
| `RenderPlayer` | `net.smart.render.RenderPlayer` — PlayerAPI 없는 경로 |
| `SmartRenderRenderPlayerBase` | `net.smart.render.playerapi.SmartRenderRenderPlayerBase` — PlayerAPI 경로 |

---

## 메서드 전체 (전부 추상, 11개)

### 모델 생성 / 초기화

```java
IModelPlayer createModel(ModelBiped existing, float f);
```
- SmartRenderRender 생성자에서 메인(f=0.0F), 흉갑(f=1.0F), 갑옷(f=0.5F) 세 번 호출
- `existing`: 기존 ModelBiped (RenderPlayer 구현에서는 사용하지 않음)
- 반환: 새로 생성한 `IModelPlayer` 인스턴스

```java
void initialize(ModelBiped modelBipedMain, ModelBiped modelArmorChestplate,
                ModelBiped modelArmor, float shadowSize);
```
- SmartRenderRender 생성자에서 `createModel()` 세 번 호출 후 마지막에 한 번 호출
- 렌더러의 `mainModel`, `modelBipedMain/Armor/Chestplate`, `shadowSize` 필드를 SmartRender 모델로 교체

---

### vanilla super 호출용 (4개)

```java
void superRenderPlayer(AbstractClientPlayer entityplayer,
                        double d, double d1, double d2, float f, float renderPartialTicks);
```
- SmartRenderRender.renderPlayer()에서 `CurrentMainModel` 설정 후 호출
- 구현체: `super.doRender()` (vanilla RenderPlayer.doRender)

```java
void superDrawFirstPersonHand(EntityPlayer entityPlayer);
```
- SmartRenderRender.drawFirstPersonHand()에서 호출
- 구현체: `super.renderFirstPersonArm()` (vanilla)

```java
void superRotatePlayer(AbstractClientPlayer entityplayer,
                        float totalTime, float actualRotation, float f2);
```
- SmartRenderRender.rotatePlayer()에서 actualRotation=0으로 조작 후 호출
- 구현체: `super.rotateCorpse()` (vanilla)

```java
void superRenderSpecials(AbstractClientPlayer entityplayer, float f);
```
- SmartRenderRender.renderSpecials()에서 귀/망토 before 훅 후 호출
- 구현체: `super.preRenderCallback()` (vanilla)

---

### getter (6개)

```java
RenderManager getRenderManager();
```
- 구현체: vanilla `renderManager` 필드 반환

```java
ModelBiped getModelBipedMain();
ModelBiped getModelArmorChestplate();
ModelBiped getModelArmor();
```
- SmartRenderRender 생성자에서 `createModel()` 호출 전에 기존 모델을 읽어올 때 사용
- 구현체: 각 모델 필드 반환 (ModelBiped 타입으로 캐스트)

```java
IModelPlayer[] getRenderModels();
```
- SmartRenderRender.renderPlayer()와 rotatePlayer()에서 모든 레이어에 데이터 세팅 시 사용
- 구현체: `[getRenderModelBipedMain(), getRenderModelArmorChestplate(), getRenderModelArmor()]` lazy init 배열

---

## IModelPlayer와의 대응 비교

| IModelPlayer (모델 측) | IRenderPlayer (렌더러 측) |
|------------------------|--------------------------|
| `getRenderModel()` | `getRenderModels()` |
| `initialize(biped* 9개)` | `initialize(ModelBiped 3개 + shadowSize)` |
| `superRender()` | `superRenderPlayer()` |
| `superSetRotationAngles()` | `superRotatePlayer()` |
| `superRenderCloak()` | `superRenderSpecials()` |
| 파트 getter 16개 | 모델 getter 3개 |
| `animate*` 11개 | (없음 — 렌더러는 animate 직접 호출 안 함) |
| — | `createModel()` |
| — | `superDrawFirstPersonHand()` |
| — | `getRenderManager()` |

---

## 호출 흐름 요약

```
SmartRenderRender 생성자
  ├─ irp.createModel(irp.getModelBipedMain(), 0.0F)  → modelBipedMain 생성
  ├─ irp.createModel(irp.getModelArmorChestplate(), 1.0F)
  ├─ irp.createModel(irp.getModelArmor(), 0.5F)
  └─ irp.initialize(main.mp, chestplate.mp, armor.mp, 0.5F)

SmartRenderRender.renderPlayer()
  ├─ irp.getRenderModels()  → 모든 레이어에 데이터 세팅
  └─ irp.superRenderPlayer(...)

SmartRenderRender.drawFirstPersonHand()
  └─ irp.superDrawFirstPersonHand(...)

SmartRenderRender.rotatePlayer()
  └─ irp.superRotatePlayer(..., actualRotation=0, ...)

SmartRenderRender.renderSpecials()
  └─ irp.superRenderSpecials(...)
```

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `IModelPlayer` | createModel 반환 타입, getRenderModels 배열 원소 타입 |
| `ModelBiped` (vanilla) | initialize/getModel* 파라미터·반환 타입 |
| `AbstractClientPlayer` (vanilla) | superRenderPlayer, superRotatePlayer, superRenderSpecials 파라미터 |
| `EntityPlayer` (vanilla) | superDrawFirstPersonHand 파라미터 |
| `RenderManager` (vanilla) | getRenderManager 반환 타입 |
