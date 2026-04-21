# ModelCapeRenderer.java 리서치

소스: https://github.com/makamys/SmartRender/blob/master/src/main/java/net/smart/render/ModelCapeRenderer.java  
패키지: `net.smart.render`  
상속: `ModelCapeRenderer extends ModelSpecialRenderer`

---

## 역할

망토(cloak/cape)의 물리 시뮬레이션과 변환을 담당하는 렌더러.  
플레이어의 이동 속도·방향·회전 및 걷기 애니메이션을 기반으로 망토 각도를 매 프레임 계산한다.  
`ModelSpecialRenderer`를 상속하므로 기본적으로 `ignoreRender = true`, `beforeRender()`/`afterRender()`로 렌더 구간을 제어한다.

- **실행 위치**: 클라이언트 전용

---

## 필드

```java
private final ModelRotationRenderer outer;   // bipedOuter 레퍼런스 (몸통 X 회전값 참조용)
private EntityPlayer entityplayer;           // 현재 렌더 중인 플레이어
private float setFactor;                     // renderPartialTicks 보간 계수
```

---

## 생성자

```java
public ModelCapeRenderer(ModelBase modelBase, int i, int j,
                          ModelRotationRenderer baseRenderer,
                          ModelRotationRenderer outerRenderer)
{
    super(modelBase, i, j, baseRenderer);
    outer = outerRenderer;
}
```

SmartRenderModel에서의 호출:
```java
bipedCloak = new ModelCapeRenderer(mp, 0, 0, bipedBreast, bipedOuter);
```
- 부모(base): `bipedBreast` — 변환 체인의 부모
- outer: `bipedOuter` — 몸통 전체 회전값 참조용 (망토 각도 상한 계산에 사용)

---

## 메서드 전체

### `beforeRender(EntityPlayer entityplayer, float factor)`

```java
public void beforeRender(EntityPlayer entityplayer, float factor)
{
    this.entityplayer = entityplayer;
    this.setFactor = factor;
    super.beforeRender(true);   // doPopPush = true, ignoreRender = false
}
```

- `doPopPush = true` — `doRender()` 직전에 `glPopMatrix + glPushMatrix` 실행
- `setFactor` = renderPartialTicks (보간 계수)
- `SmartRenderRender.renderSpecials()`에서 `modelBipedMain.bipedCloak.beforeRender(entityplayer, f)`로 호출

---

### `preTransform(float factor, boolean push)` — override

`super.preTransform(factor, push)` 호출 후 망토 물리 계산 + GL 회전 적용.

```java
@Override
public void preTransform(float factor, boolean push)
{
    super.preTransform(factor, push);

    // 1. 망토 위치와 플레이어 위치의 차이 계산 (SRG 이름 필드)
    double d = (entityplayer.field_71091_bM + (entityplayer.field_71094_bP - entityplayer.field_71091_bM) * setFactor)
             - (entityplayer.prevPosX      + (entityplayer.posX            - entityplayer.prevPosX)       * setFactor);
    // d = 보간된 망토X - 보간된 플레이어X

    double d1 = (entityplayer.field_71096_bN + (entityplayer.field_71095_bQ - entityplayer.field_71096_bN) * setFactor)
              - (entityplayer.prevPosY        + (entityplayer.posY           - entityplayer.prevPosY)        * setFactor);
    // d1 = 보간된 망토Y - 보간된 플레이어Y

    double d2 = (entityplayer.field_71097_bO + (entityplayer.field_71085_bR - entityplayer.field_71097_bO) * setFactor)
              - (entityplayer.prevPosZ        + (entityplayer.posZ           - entityplayer.prevPosZ)        * setFactor);
    // d2 = 보간된 망토Z - 보간된 플레이어Z

    // 2. 플레이어 회전 방향 벡터
    float f1 = entityplayer.prevRenderYawOffset
             + (entityplayer.renderYawOffset - entityplayer.prevRenderYawOffset) * setFactor;
    // f1 = 보간된 renderYawOffset (degree 단위)

    double d3 = MathHelper.sin((f1 * 3.141593F) / 180F);   // sin(f1 라디안)
    double d4 = -MathHelper.cos((f1 * 3.141593F) / 180F);  // -cos(f1 라디안)

    // 3. Y 오프셋 (망토 상하 처짐)
    float f2 = (float)d1 * 10F;
    if(f2 < -6F) f2 = -6F;   // 클램프 하한
    if(f2 > 32F) f2 = 32F;   // 클램프 상한

    // 4. 플레이어 진행 방향·측면 성분 분리
    float f3 = (float)(d * d3 + d2 * d4) * 100F;   // 진행 방향 성분 (뒤로 당겨지는 힘)
    float f4 = (float)(d * d4 - d2 * d3) * 100F;   // 측면 성분 (좌우 흔들림)
    if(f3 < 0.0F) f3 = 0.0F;                        // 앞으로 나가는 경우는 0으로 클램프

    // 5. 걷기 애니메이션에 따른 f2 보정
    float f5 = entityplayer.prevCameraYaw
             + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * setFactor;
    f2 += MathHelper.sin(
              (entityplayer.prevDistanceWalkedModified
             + (entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified) * setFactor)
            * 6F
          ) * 32F * f5;

    // 6. 최종 각도 계산
    float localAngle    = 6F + f3 / 2.0F + f2;
    float localAngleMax = Math.max(70.523F - outer.rotateAngleX * RadiantToAngle, 6F);
    float realLocalAngle = Math.min(localAngle, localAngleMax);

    // 7. GL 회전 적용
    GL11.glRotatef(realLocalAngle, 1.0F, 0.0F, 0.0F);   // X축: 망토 펼침 각도
    GL11.glRotatef(f4 / 2.0F, 0.0F, 0.0F, 1.0F);         // Z축: 좌우 흔들림
    GL11.glRotatef(-f4 / 2.0F, 0.0F, 1.0F, 0.0F);        // Y축: 흔들림 보정
    GL11.glRotatef(180F, 0.0F, 1.0F, 0.0F);              // Y축 180도: 망토가 뒤를 향하도록
}
```

---

## SRG 이름 필드 → 의미 정리

| SRG 이름 | 의미 |
|----------|------|
| `field_71091_bM` | 망토의 이전 프레임 X 위치 (prevCloakX) |
| `field_71094_bP` | 망토의 현재 X 위치 (cloakX) |
| `field_71096_bN` | 망토의 이전 프레임 Y 위치 (prevCloakY) |
| `field_71095_bQ` | 망토의 현재 Y 위치 (cloakY) |
| `field_71097_bO` | 망토의 이전 프레임 Z 위치 (prevCloakZ) |
| `field_71085_bR` | 망토의 현재 Z 위치 (cloakZ) |

1.21.1 Yarn 매핑 대응 이름 — [미확인, vanilla 리서치에서 확인 필요]

---

## 수치 상수 전체

| 수치 | 사용 위치 | 의미 |
|------|-----------|------|
| `3.141593F` | f1 라디안 변환 | π 하드코딩 리터럴 (Math.PI 아님) |
| `-6F` / `32F` | f2 클램프 | Y 오프셋 범위 |
| `10F` | d1 → f2 변환 | Y 차이 증폭 계수 |
| `100F` | d→f3, d→f4 변환 | 위치 차이 증폭 계수 |
| `6F` | f3·f4 없을 때 기본 localAngle | 망토 최소 펼침 각도 |
| `70.523F` | localAngleMax 기준 | 몸통 X 회전에 따른 망토 각도 상한 기준값 |
| `6F` | localAngleMax 최솟값 | `Math.max(70.523F - outer.rotateAngleX * RadiantToAngle, 6F)` |
| `6F` | distanceWalkedModified 곱셈 | 걷기 사이클 주파수 |
| `32F` | 걷기 애니 진폭 | f2 보정 최대값 |
| `180F` | 마지막 GL Y 회전 | 망토 방향 반전 |

---

### `canBeRandomBoxSource()` — override

```java
@Override
public boolean canBeRandomBoxSource()
{
    return false;
}
```

- 망토는 파티클 등 랜덤 박스 선택 대상에서 제외

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `ModelSpecialRenderer` | 상위 클래스 |
| `ModelRotationRenderer outer` | bipedOuter.rotateAngleX 참조 (localAngleMax 계산) |
| `EntityPlayer` (vanilla) | 망토 위치 필드(SRG), renderYawOffset, cameraYaw, distanceWalkedModified 등 |
| `MathHelper` (vanilla) | sin, cos |
| `GL11` (LWJGL) | glRotatef |
| `RadiantToAngle` (SmartRenderUtilities 상속) | 57.296 — 라디안→각도 |

---

## 주요 관찰 사항

1. **beforeRender에서 `doPopPush = true`**: 망토 렌더 직전에 `glPopMatrix + glPushMatrix`로 GL 스택 리셋. 이는 vanilla `preRenderCallback`이 이미 특정 변환을 쌓아 놓은 상태에서 망토 고유의 변환을 독립적으로 적용하기 위함.

2. **`outer.rotateAngleX` 사용**: `bipedOuter`(몸통 전체)의 X 회전에 따라 망토가 최대로 펼쳐질 수 있는 각도를 제한. 몸통이 앞으로 기울수록 망토 상한각이 줄어듦 (`70.523F - outer.rotateAngleX * 57.296`).

3. **SRG 이름 필드 6개**: 1.7.10 obfuscation 이름 그대로 사용. 1.21.1에서는 Yarn 매핑 이름으로 교체 필요 — [미확인]

4. **`3.141593F` 리터럴**: `Math.PI`를 사용하지 않고 하드코딩. 오차는 약 `7.3E-8`로 실용적으로 무시 가능.

5. **1.21.1 이식 포인트**:
   - GL11 4개 glRotatef → MatrixStack rotate 호출로 교체
   - SRG 이름 6개 → 1.21.1 Yarn 이름으로 교체
   - `doPopPush` 패턴(`glPop + glPush`) → MatrixStack의 pop/push로 교체
   - `MathHelper.sin/cos` → 1.21.1의 동등 메서드로 교체
