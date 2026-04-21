# RendererData.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/RendererData.java  
패키지: `net.smart.render`  
종류: 일반 클래스 (상속 없음, 인터페이스 없음)

---

## 역할

`ModelRotationRenderer`의 fade(보간) 시스템에서 이전 프레임 상태를 저장하는 순수 데이터 컨테이너.  
메서드 없음. 생성자 없음(기본 생성자만). import 없음. public 필드 10개만 존재.

---

## 필드 전체

```java
public float offsetX;
public float offsetY;
public float offsetZ;
public float rotateAngleX;
public float rotateAngleY;
public float rotateAngleZ;
public float rotationPointX;
public float rotationPointY;
public float rotationPointZ;
public float totalTime = Float.MIN_VALUE;
```

- 처음 9개 float 필드: 기본값 `0.0F` (Java float 기본값)
- `totalTime`: 기본값 `Float.MIN_VALUE` (= `1.4E-45`, Java에서 양수 float 최솟값)

---

## `totalTime = Float.MIN_VALUE`의 의미

`ModelRotationRenderer.fadeIntermediate()`의 보간 발동 조건:
```java
if(previous != null && totalTime - previous.totalTime <= 2F)
```

`previous.totalTime`이 `Float.MIN_VALUE`이면 `currentTotalTime - Float.MIN_VALUE`가 사실상 `currentTotalTime`과 같아 매우 크다 → 조건 `<= 2F` 불충족 → **첫 프레임에는 보간 스킵**, 현재 값 그대로 사용.  
즉 생성 직후 첫 렌더에서 이전 데이터가 없을 때 안전하게 보간을 건너뛰는 초기화 패턴.

---

## 사용 위치

| 사용처 | 용도 |
|--------|------|
| `ModelRotationRenderer.previous` | 각 파트의 이전 프레임 상태 저장 (fade 보간용). `bipedOuter`에만 실제로 연결됨 |
| `ModelRotationRenderer.fadeStore(float)` | 현재 `offsetX/Y/Z`, `rotateAngle*`, `rotationPoint*`, `totalTime`을 previous에 저장 |
| `ModelRotationRenderer.fadeIntermediate(float)` | previous 값과 현재 값을 `* 0.2F * timeDelta`로 보간 |
| `SmartRenderRender.previousRendererData` | `Map<EntityPlayer, RendererData>` — 플레이어별 bipedOuter 회전 이력 저장 |
| `SmartRenderModel.prevOuterRenderData` | 렌더 프레임 간 bipedOuter 상태 전달용 레퍼런스 |

---

## 저장·복원 흐름

```
[매 렌더 프레임]
SmartRenderRender.renderPlayer()
  └─ modelPlayer.prevOuterRenderData = SmartRenderRender.getPreviousRendererData(entityplayer)
       ↑ Map<EntityPlayer, RendererData>에서 가져옴 (없으면 new RendererData())

SmartRenderModel.setRotationAngles()
  ├─ bipedOuter.previous = prevOuterRenderData   ← 이전 프레임 데이터 연결
  ├─ (애니메이션 로직으로 bipedOuter.rotateAngle* 등 설정)
  ├─ bipedOuter.fadeIntermediate(totalTime)       ← 이전 값과 현재 값 보간
  └─ bipedOuter.fadeStore(totalTime)              ← 현재 값을 previous(= prevOuterRenderData)에 저장
```

→ 다음 프레임에 Map에서 같은 RendererData 인스턴스를 꺼내면 이전 프레임의 저장값이 그대로 있음.

---

## 주요 관찰 사항

1. **bipedOuter 전용 실질 사용**: `ModelRotationRenderer.previous`는 모든 파트에 존재하지만, `SmartRenderModel`에서 `previous`를 실제로 연결하는 것은 `bipedOuter`뿐. 다른 파트는 `reset()`에서 `previous = null`로 초기화되고 재연결 없음.

2. **플레이어별 지속 보관**: `SmartRenderRender.previousRendererData`(Map)가 RendererData를 플레이어 인스턴스별로 보관하여 프레임 간 상태 지속성을 제공.

3. **1.21.1 이식**: 이 클래스 자체는 순수 Java 데이터 객체이므로 그대로 유지 가능. fade 시스템을 유지한다면 이 클래스를 그대로 포팅.
