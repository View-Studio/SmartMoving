# 리서치 진행 현황

새 세션 시작 시 이 파일을 먼저 읽고, 체크 안 된 항목 중 맨 위부터 하나씩 진행한다.
하나 완료할 때마다 체크하고 커밋한다. 한 세션에 하나만 해도 된다.

아래 목록은 읽어야 할 **전체 파일 목록**이다. 예외 없다.

---

## 작업 순서 원칙

1. 한 세션에 **파일 하나**만 한다.
2. 그 파일을 **첫 줄부터 마지막 줄까지 모든 코드** 읽고 문서화한다. 절반만 하고 넘어가지 않는다.
3. 완료 즉시 커밋 + 이 파일 체크 업데이트 + 커밋. 커밋이 곧 진행 저장이다.
4. 파일이 매우 크면 (SmartMovingSelf.java 103KB 등) 시스템 단위로 쪼개도 된다.
   단, 쪼갤 경우 어디까지 했는지 이 파일에 명시한다. 쪼갠다고 해서 일부만 읽는 게 아니다. 전부 읽는다.

---

## SmartRender 원본 리서치 (31개 파일)

저장 위치: `docs/research/original/smartrender/`
소스: https://github.com/makamys/SmartRender

### net.smart.render
- [x] `SmartRenderModel.java`
- [x] `SmartRenderRender.java`
- [x] `ModelRotationRenderer.java`
- [x] `ModelPlayer.java`
- [x] `RenderPlayer.java`
- [x] `IModelPlayer.java`
- [x] `IRenderPlayer.java`
- [x] `SmartRenderContext.java`
- [x] `SmartRenderInfo.java`
- [x] `SmartRenderInstall.java`
- [x] `SmartRenderMod.java`
- [x] `SmartRenderUtilities.java`
- [x] `RendererData.java`
- [x] `ModelSpecialRenderer.java`
- [x] `ModelCapeRenderer.java`
- [x] `ModelEarsRenderer.java`

### net.smart.render.playerapi
- [x] `SmartRender.java`
- [x] `SmartRenderModelPlayerBase.java`
- [x] `SmartRenderRenderPlayerBase.java`

### net.smart.render.statistics
- [x] `IEntityPlayerSP.java`
- [x] `SmartStatistics.java`
- [x] `SmartStatisticsContext.java`
- [x] `SmartStatisticsData.java`
- [x] `SmartStatisticsDatas.java`
- [x] `SmartStatisticsFactory.java`
- [x] `SmartStatisticsOther.java`

### net.smart.render.statistics.playerapi
- [x] `SmartStatistics.java`
- [x] `SmartStatisticsFactory.java`
- [x] `SmartStatisticsPlayerBase.java`

### net.smart.utilities
- [x] `Name.java`
- [x] `Reflect.java`

---

## SmartMoving 원본 리서치 (72개 파일)

저장 위치: `docs/research/original/smartmoving/`
소스: https://github.com/makamys/SmartMoving

### net.smart.core
- [x] `SmartCoreClassVisitor.java`
- [x] `SmartCoreContainer.java`
- [x] `SmartCoreEventHandler.java`
- [x] `SmartCoreInfo.java`
- [x] `SmartCoreMethodVisitor.java`
- [x] `SmartCorePlugin.java`
- [x] `SmartCoreTransformation.java`
- [x] `SmartCoreTransformer.java`

### net.smart.moving (핵심)
- [x] `SmartMovingSelf.java` ← 103KB, 최우선, 크면 시스템별로 쪼갤 것
- [x] `SmartMovingBase.java`
- [x] `SmartMoving.java`
- [x] `SmartMovingClient.java`
- [x] `SmartMovingServer.java`
- [x] `SmartMovingServerComm.java`
- [x] `SmartMovingComm.java`
- [x] `SmartMovingContext.java`
- [x] `SmartMovingCoreEventHandler.java`
- [x] `SmartMovingFactory.java`
- [x] `SmartMovingInfo.java`
- [x] `SmartMovingInstall.java`
- [x] `SmartMovingMod.java`
- [x] `SmartMovingOther.java`
- [x] `SmartMovingPacketStream.java`
- [x] `Button.java`
- [x] `ClimbGap.java`
- [x] `Compat.java`
- [x] `FeetClimbing.java`
- [x] `HandsClimbing.java`
- [x] `IEntityPlayerMP.java`
- [x] `IEntityPlayerSP.java`
- [x] `ILocalUserNameProvider.java`
- [x] `IPacketReceiver.java`
- [x] `IPacketSender.java`
- [x] `ISmartMovingClient.java`
- [x] `ISmartMovingSelf.java`
- [x] `LocalUserNameProvider.java`
- [x] `Orientation.java`

### net.smart.moving.config
- [x] `SmartMovingClientConfig.java`
- [x] `SmartMovingConfig.java`
- [x] `SmartMovingOptions.java`
- [x] `SmartMovingProperties.java`
- [x] `SmartMovingServerConfig.java`
- [x] `SmartMovingServerOptions.java`

### net.smart.moving.playerapi
- [x] `SmartMoving.java`
- [x] `SmartMovingFactory.java`
- [x] `SmartMovingPlayerBase.java`
- [x] `SmartMovingSelf.java`
- [x] `SmartMovingServerPlayerBase.java`

### net.smart.moving.render
- [x] `SmartMovingModel.java`
- [x] `SmartMovingRender.java`
- [x] `SmartRenderContext.java`
- [x] `IModelPlayer.java`
- [x] `IRenderPlayer.java`
- [x] `ModelPlayer.java`
- [ ] `RenderPlayer.java`

### net.smart.moving.render.playerapi
- [ ] `SmartMoving.java`
- [ ] `SmartMovingModelPlayerBase.java`
- [ ] `SmartMovingRenderPlayerBase.java`

### net.smart.moving.test
- [ ] `SmartMovingTestCommand.java`
- [ ] `SmartMovingTestMod.java`

### net.smart.properties
- [ ] `Properties.java`
- [ ] `Property.java`
- [ ] `Value.java`

### net.smart.utilities
- [ ] `Name.java`
- [ ] `Reflect.java`

---

## vanilla 1.21.1 리서치

저장 위치: `docs/research/vanilla/`
소스: `~/.gradle/caches/fabric-loom/1.21.1/minecraft-common.jar` (디컴파일)
Yarn 매핑 확인: `~/.gradle/caches/fabric-loom/1.21.1/net.fabricmc.yarn.*/mappings.jar` → `mappings/mappings.tiny`

- [ ] `LivingEntity.travel()`
- [ ] `LivingEntity.tick()` / `tickMovement()`
- [ ] `LivingEntity.updatePose()` → `trySetPose()`
- [ ] `LivingEntity.isInSwimmingPose()` — 호출되는 모든 곳 추적
- [ ] `LivingEntity.jump()`
- [ ] `PlayerEntity.getEntityPose()` 해당 Yarn 메서드
- [ ] `PlayerEntityRenderer.setupTransforms()`
- [ ] `PlayerEntityRenderer.getPositionOffset()`
- [ ] `PlayerEntityModel.setAngles()`
- [ ] `LivingEntityRenderer.render()`
- [ ] `Entity.calculateDimensions()`
- [ ] 서버 위치/속도 보정 코드 경로 (`ServerPlayNetworkHandler` 등)

---

## 교차 분석

저장 위치: `docs/research/mapping/`
각 시스템의 원본 + vanilla 리서치가 **둘 다 완료된 후**에만 진행.

- [ ] 애니메이션 시스템
- [ ] 수영/잠수
- [ ] 크롤링/슬라이딩
- [ ] 클라이밍/천장클라이밍
- [ ] 점프 전체
- [ ] 이동 속도/물리
- [ ] 네트워크/서버 동기화
