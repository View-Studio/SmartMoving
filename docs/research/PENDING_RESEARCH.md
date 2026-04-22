# PENDING_RESEARCH — 미확인·미완료 리서치 전체 목록 및 계획

> **작성 기준**: RESEARCH_RULES.md 100% 준수. 추정 없음. 확인된 것만 기록.
> **사용 방법**: 새 세션 시작 시 이 파일을 먼저 읽고, 맨 위 `[ ]` 항목부터 하나씩 진행.
>               리서치 완료 시 `[x]`로 바꾸고, 결과 기록 파일 경로를 옆에 명시.
>               완료된 항목은 해당 mapping/vanilla/original 파일에도 즉시 반영.

---

## 이 파일의 목적

현재 구현 코드에 남아있는 **추정값·stub·TODO** 전체를 항목화하고,
각각을 제거하기 위해 필요한 리서치를 세션 단위 청크로 분할한다.

PROGRESS.md의 파일 체크는 "해당 파일을 읽었다"는 표시이지,
"그 파일에서 필요한 모든 값을 확인했다"는 보장이 아니다.
아래 항목들은 파일을 읽었음에도 기록이 누락됐거나, 당시 미확인으로 넘긴 것들이다.

---

## 미확인·미완료 항목 전체 (체크리스트 기준)

### A. 원본 소스 미확인 항목 (SmartMoving/SmartRender 코드에서 읽어야 할 것)

| ID | 미확인 내용 | 원본 소스 위치 | 상태 |
|----|------------|--------------|------|
| A-01 | `SwimCrawlWaterBorder` 정확한 상수값 | SmartMovingContext.java | [x] → `original/smartmoving/moving/SmartMovingContext.md` |
| A-02 | `playerSwimWaterBorder` 정확한 상수값 | SmartMovingContext.java | [x] → `original/smartmoving/moving/SmartMovingContext.md` (런타임 변수, 공식 추가) |
| A-03 | `SwimSoundDistance` 정확한 상수값 | SmartMovingContext.java | [x] → `original/smartmoving/moving/SmartMovingContext.md` |
| A-04 | `Config.getJumpExhaustionGain()` 내부 공식 전체 | SmartMovingConfig.java / SmartMovingOptions.java | [x] → `original/smartmoving/config/SmartMovingClientConfig.md` |
| A-05 | `leftJumpCount`, `rightJumpCount`, `backJumpCount` 더블클릭 카운터 임계값 | SmartMovingSelf.java (jump 섹션) | [x] → `mapping/jump.md` |
| A-06 | `wallJumpCount` 최대값 및 `continueWallJumping` 전환 조건 전체 | SmartMovingSelf.java (wallJump 섹션) | [x] → `mapping/jump.md` |
| A-07 | `jumpMotionX`, `jumpMotionZ` 저장 타이밍 — handleJumping 어느 지점에서 저장하는지 | SmartMovingSelf.java | [x] → `mapping/jump.md` |
| A-08 | SM이 사용하는 전체 커스텀 키 목록 + 각 키의 역할 | Button.java | [x] → `original/smartmoving/moving/Button.md` |
| A-09 | `SmartMovingClient.processBlockCode()` 원본 구현 전체 — 채팅 파싱 방식, 마커 포맷, 12개 기능 배열 구조 | SmartMovingClient.java | [x] → 실제 위치는 `SmartMovingComm.java`. `SmartMovingComm.md`에 이미 기록됨. `SmartMovingClient.md`에 요약 및 정오 기재. |
| A-10 | `SmartMovingSelf.updateEntityActionState()` — processBlockCode 결과가 어느 필드에 어떻게 반영되는지 | SmartMovingSelf.java | [x] → 2347~2357줄. `updateCounter < 10` 조건으로 초기 10틱만 채팅 히스토리 스캔. 결과는 Config→ServerConfig 전환을 통해 간접 반영. `SmartMovingClient.md` 및 `SmartMovingSelf.md`에 기록. |
| A-11 | `getPoses()` 반환값이 사용되는 경로 전체 (어디서 호출, 어떻게 사용) | SmartMovingSelf.java 또는 SmartMovingPlayerBase.java | [x] → SM 전체 계층에 없음 확인. 1.7.10에 EntityPose API 없음. 대응 메커니즘은 player.height/boundingBox.maxY 직접 변경. `SmartMovingServerPlayerBase.md` R-04 섹션 기재. |
| A-12 | `reverseHandleMaterialAcceleration()` — 실제로 무엇을 하는지, travel() 완전 대체 시 불필요한지 여부 | SmartMovingSelf.java | [x] → SmartMovingBase.java에 있음. 이미 SmartMovingBase.md에 기록됨. 1.21.1 필요 여부: B-11 미확인 → [미확인]. SmartMovingServerPlayerBase.md R-04 섹션 요약 기재. |
| A-13 | SM 크롤링 서버 측 `isCrawling` 동기화 방식 — State 패킷 비트 배치 확인 (bit 몇 번인지) | SmartMovingOther.java / State 패킷 비트맵 | [x] → bit 13. SmartMovingOther.processStatePacket 디코딩 + SmartMovingSelf.addToSendQueue 인코딩 양쪽 직접 확인. SmartMovingOther.md R-06 섹션 기재. |
| A-14 | SM 슬라이딩 서버 측 `isSliding` 동기화 방식 — State 패킷 비트 배치 확인 | SmartMovingOther.java / State 패킷 비트맵 | [x] → bit 21. 디코딩·인코딩 양쪽 직접 확인. 서버는 isSliding 추출 안 함 — 클라이언트 렌더링 전용. SmartMovingOther.md R-06 섹션 기재. |
| A-15 | `bipedOuter`, `bipedTorso`, `bipedBreast`, `bipedNeck`, `bipedPelvic`, `bipedShoulder` 계층 구조 전체 — 부모/자식 관계, 각 노드의 초기 pivotXYZ, rotationXYZ | SmartMovingModel.java / ModelPlayer.java (SmartRender) | [x] → SmartRenderModel.java 생성자 직접 확인. 전체 15노드 계층·pivot 값 SmartMovingModel.md R-05 섹션 + animation_system.md에 기재. |
| A-16 | `SmartMovingModel.setRotationAngles()` — `isRopeSliding` 분기 전체 (현재 미구현) | SmartMovingModel.java | [x] → SmartMovingModel.md "1. isRopeSliding" 섹션(lines 269~301)에 이미 완전 기록되어 있음. 추가 작업 불필요. |
| A-17 | 서버 물리 재현 범위 — SmartMovingServerPlayerBase.java가 서버에서 어떤 물리 계산을 수행하는지 전체 | SmartMovingServerPlayerBase.java | [x] → SmartMovingServer.java 전체 확인. 실제 물리(클라이밍/수영 벡터)는 클라이언트 담당. 서버는 hitbox 조정/floatKick 억제/낙하거리 리셋/crawlingCooldown/소진 필터링만 처리. SmartMovingServerPlayerBase.md R-04 섹션 기재. |

---

### B. vanilla 1.21.1 미확인 항목 (Fabric Loom 디컴파일 소스에서 읽어야 할 것)

| ID | 미확인 내용 | vanilla 소스 위치 | 상태 |
|----|------------|----------------|------|
| B-01 | `EntityPose` enum — 전체 값 목록, 커스텀 값 추가 가능 여부 | `net.minecraft.entity.EntityPose` | [x] → 18개 (STANDING~INHALING, SLIDING=index15 포함). enum final class — 커스텀 추가 불가. vanilla/EntityPose_system.md B-01 기재. |
| B-02 | `PlayerEntity.getEntityPose()` 전체 — 어떤 조건에서 어떤 포즈를 반환하는지 | `PlayerEntity.java` | [x] → Yarn명: updatePose(method_7318). 우선순위: FALL_FLYING>SLEEPING>SWIMMING>SPIN_ATTACK>CROUCHING>STANDING. vanilla/LivingEntity_updatePose.md 및 PlayerEntity_pose_dimensions.md에 기재. |
| B-03 | `LivingEntity.getPoses()` — 반환값 구조 및 사용 경로 (어디서 호출되는지) | `LivingEntity.java` | [x] → vehicle entity(Boat/Minecart/Horse/Pig/Strider) updatePassengerForDismount() 전용. PlayerEntity반환: [STANDING,CROUCHING,SWIMMING]. vanilla/EntityPose_system.md B-03 기재. |
| B-04 | `LivingEntity.updatePose()` / `trySetPose()` — 포즈 전환 조건 전체, EntityPose 전환 시 calculateDimensions() 호출 여부 | `LivingEntity.java` | [x] → trySetPose 미존재. setPose→DataTracker→onTrackedDataSet→calculateDimensions 자동 호출 확인. vanilla/EntityPose_system.md B-04 및 PlayerEntity_pose_dimensions.md 기재. |
| B-05 | `LivingEntity.getLeaningPitch()` / `updateLeaningPitch()` — 증가/감소 속도 정확한 수치 | `LivingEntity.java` | [x] → +0.09F / -0.09F, 범위 0.0~1.0, isInSwimmingPose() 기준. vanilla/LivingEntity_physics_misc.md B-05 기재. |
| B-06 | `BipedEntityModel.animateArms()` 전체 코드 — 내부 로직, 파라미터 | `BipedEntityModel.java` | [x] → handSwingProgress>0일 때만 실행. body.yaw 회전으로 팔 이동, pitch/yaw/roll 보정. Yarn: method_29353. vanilla/BipedEntityModel_detail.md B-06 기재. |
| B-07 | `BipedEntityModel.setAngles()` — mp.onGround 파라미터가 float인지, 어떤 값이 전달되는지 | `BipedEntityModel.java` | [x] → onGround 파라미터 없음. 시그니처: (LivingEntity, float×5). f=limbPos, g=limbSpeed, h=age+tickDelta, i=headYaw(deg), j=headPitch(deg). Yarn: method_17087. 스니킹은 sneaking 필드로 처리. vanilla/BipedEntityModel_detail.md B-07 기재. |
| B-08 | `ModelPart` — `xScale`, `yScale`, `zScale` 필드 존재 여부 및 렌더링에서의 사용 방식 | `ModelPart.java` | [x] → xScale/yScale/zScale 모두 존재(기본값 1.0F). rotate()에서 1.0F 아닐 때 matrices.scale() 적용. field_37938/37939/37940. vanilla/BipedEntityModel_detail.md B-08 기재. |
| B-09 | `ModelPart.rotate(MatrixStack)` — 내부에서 pitch→yaw→roll 순서로 적용하는지 정확한 코드 | `ModelPart.java` | [x] → Quaternionf().rotationZYX(roll, yaw, pitch) 사용. 적용 순서: pitch(X)→yaw(Y)→roll(Z). method_22703. vanilla/BipedEntityModel_detail.md B-09 기재. |
| B-10 | `LivingEntity.travel()` — `handleFluidAcceleration()` 내 흐름 가속 크기 (0.014D 여부 확인) | `LivingEntity.java` | [x] → `SPEED_IN_WATER = 0.014`. `checkWaterState()` → `updateMovementInFluid(FluidTags.WATER, 0.014)`. vanilla/LivingEntity_physics_misc.md B-10 기재. |
| B-11 | `LivingEntity.travel()` — `reverseHandleMaterialAcceleration` 해당 코드 경로 (1.21.1에 존재하는지) | `LivingEntity.java` | [x] → 1.21.1에 존재하지 않음 확인. vanilla/LivingEntity_physics_misc.md B-11 기재. |
| B-12 | `EntityAttributes.GENERIC_GRAVITY` — 기본값 0.08D 여부, 어디서 초기화되는지 | `EntityAttributes.java` / `LivingEntity.java` | [x] → 기본값 0.08, 범위 -1.0~1.0, Tracked=true. EntityAttributes.java register()에서 초기화. vanilla/LivingEntity_physics_misc.md B-12 기재. |
| B-13 | `AbstractBlock` / `Block` — `getSlipperiness()` 해당 Yarn 메서드명 확인 | `AbstractBlock.java` 또는 `AbstractBlockState.java` | [x] → `Block.getSlipperiness()` (Yarn명). `this.slipperiness` 필드 반환. vanilla/LivingEntity_physics_misc.md B-13 기재. |
| B-14 | `LivingEntity.isInsideWall()` — 현재 구현한 Mixin 대상이 맞는지 재확인, 크롤링 쿨다운 억제 로직 정확성 | `LivingEntity.java` 또는 `Entity.java` | [x] → Entity.java에 정의(method_5757), LivingEntity가 isSleeping() 체크 후 super 호출로 오버라이드. vanilla/LivingEntity_physics_misc.md B-14 기재. |
| B-15 | 수영 소리 vanilla 자동 재생 — travel() 또는 별도 경로에서 수영 소리가 자동 재생되는지, 우리와 중복 재생 가능성 | `LivingEntity.java` / `PlayerEntity.java` | [x] → Entity.move() 내 스텝 처리 후 isTouchingWater()→playSwimSound(). onSwimmingStart()에서 입수 시 splash. PlayerEntity가 ENTITY_PLAYER_SWIM/SPLASH로 오버라이드. vanilla/LivingEntity_physics_misc.md B-15 기재. |
| B-16 | `DataTracker` — 커스텀 엔트리 등록 방법 (TrackedDataHandlerRegistry, Mixin으로 추가하는 방법) | `DataTracker.java` / Fabric API | [x] → registerData()+Builder.add() 패턴, Mixin @At("TAIL") 주입 방법, BOOLEAN 핸들러 확인. vanilla/DataTracker_custom_entry.md B-16 기재. |
| B-17 | `PlayerEntityRenderer.setupTransforms()` — `bodyYaw` 파라미터가 정확히 index 몇 번인지 (ModifyArg 용) | `PlayerEntityRenderer.java` | [x] → @ModifyArg index=3 (0-based). Vineflower 직접 확인: setupTransforms(entity,matrices,animProgress,bodyYaw,tickDelta,scale). PlayerEntityRenderer_setupTransforms.md R-10 기재. |
| B-18 | `MatrixStack` — X/Y/Z 축 회전 API 전체 (`multiply(RotationAxis.POSITIVE_X.rotation(angle))` 외 다른 방법 있는지) | `MatrixStack.java` | [x] → multiply(Quaternionf)가 유일한 회전 메서드. rotateX/Y/Z 없음. RotationAxis.POSITIVE_X/Y/Z.rotation()/rotationDegrees() 사용. PlayerEntityRenderer_setupTransforms.md R-10 기재. |
| B-19 | `ClientPlayNetworkHandler` / `ClientReceiveMessageEvents` — 채팅 수신 이벤트에서 raw text (§ 코드 포함) 접근 방법 | Fabric API | [x] → GAME/CHAT 이벤트 시그니처 확인. Text.getString()=평문만(§ 코드 없음). fabric-message-api-v1 Vineflower 디컴파일 직접 확인. vanilla/DataTracker_custom_entry.md B-19 기재. |

---

### C. 미구현 기능 (리서치 완료 후 구현해야 할 것)

| ID | 미구현 기능 | 의존 리서치 | 상태 |
|----|-----------|-----------|------|
| C-01 | 4-4 `processBlockCode` — 채팅 설정 파싱 구현 | A-09, A-10, B-19 | [x] → ClientReceiveMessageEvents.GAME 등록 + processBlockCode 구현. "§0§1"/"§f§f" 마커, substring(4,len-4), 12개 코드→SmartMovingConfig.INSTANCE 필드 직접 설정. SmartMovingConfig에 baseClimb 필드 추가. 채팅 억제(ALLOW_GAME false) 미구현(mapping 근거 없음). |
| C-02 | 6-3 EntityPose — 헤드점프 포즈 전략 확정 및 구현 | B-01, B-02, B-04 | [x] → SLIDING 포즈 + getBaseDimensions() Mixin(0.6×0.8). updatePose() @HEAD 취소. M-01/M-02/M-04/M-05 미확인 남음. mapping/pose_strategy.md C-02 섹션 기재. |
| C-03 | 6-3 EntityPose — 슬라이딩 포즈 전략 확정 및 구현 | B-01, B-02, B-04 | [x] → SLIDING 포즈(index 15) 재사용 + getBaseDimensions() Mixin. updatePose() @HEAD 취소. M-02/M-03 미확인 남음. mapping/pose_strategy.md C-03 섹션 기재. |
| C-04 | 6-5 `recalculateDimensions` — 헤드점프 착지 포즈 복원 로직 | B-04, C-02 | [x] → setPoseSmall()=SLIDING, resetHeightOffset()=공간 확보 시에만 STANDING 복원(공간 부족 시 isHeadJumping 유지). getBaseDimensions Mixin: SLIDING→0.6×0.8, eyeHeight=0.62F. updatePose Mixin: isHeadJumping\|isSliding→SLIDING, isCrawling→SWIMMING. 클라이언트(MixinPlayerEntityClient) + 서버(MixinPlayerEntity isSmall→SLIDING) 모두 구현. |
| C-05 | 9-1 `isCrawling` DataTracker 동기화 | A-13, B-16 | [x] → DataTracker 방식 채택. 서버 bit 13 추출 후 SM_CRAWLING DataTracker.set() → MC 자동 전파. 타인 렌더: otherPlayer.dataTracker.get(SM_CRAWLING). network_sync.md 15절. |
| C-06 | 9-1 `isSliding` DataTracker 필요 여부 확인 및 구현 | A-14, B-16 | [x] → DataTracker 방식 채택 (isCrawling과 동일 패턴). 서버에 bit 21 추출 + SM_SLIDING DataTracker.set() 추가. 원본 서버는 미처리였으나 1.21.1에서 추가. network_sync.md 15절. |
| C-07 | 12-3 bodyYaw `@ModifyArg` — forwardRotation 강제 | B-17, A-15 | [x] → sm_captureBodyYaw(@HEAD)에서 isClimbing/isCrawlClimbing/isCeilingClimbing/isSwimming_sm/isDiving/isSliding/isHeadJumping/isCrawling 상태 시 `atan2(-vel.x, vel.z)`로 smBodyYawOverride 계산. sm_modifyBodyYaw(@ModifyArg index=3)에서 super.setupTransforms 호출 시 교체. MixinPlayerEntityRenderer.java. |
| C-08 | 12-4 ModelRotationRenderer 대체 — 6-axis MatrixStack | A-15, B-08, B-09, B-18 | [x] → 비표준 회전 순서 11종 전체 목록 + MatrixStack 구현 패턴 확정. smRotationOrder Mixin 전략 설계. M-09/M-10 미확인→R-17. animation_system.md R-13 섹션. |
| C-09 | 12-5 중간 노드 부재 — isFlying/isHeadJumping body 기울기 | A-15, C-08 | [x] → setupTransforms X rotate + setAngles head.pitch 보정 전략 확정. 등가 증명 완료. M-06/M-11 미확인→R-17. animation_system.md R-13 섹션. |
| C-10 | 12-7 `smallOverGroundHeight` 실제 블록 탐색 계산 구현 | — | [x] → sm_setAngles HEAD에서 isCrawlClimbing\|isHeadJumping일 때 computeSmallOverGroundHeight() 호출. playerY 기준 최대 5블록 아래 열 스캔 → 첫 고체 블록 topY와의 차 반환(0~5F). MixinPlayerEntityModelClient.java. |
| C-11 | 13-1 설정 분리 — ServerConfig vs Options 전환 상태 관리 | A-04 | [x] → SmartMovingConfig에 Config(volatile), SERVER_CONFIG, loadFromArray() 추가. SmartMovingClient.processConfigContentPacket: null→유지, length=0→Config=INSTANCE, length>0→SERVER_CONFIG.loadFromArray+Config=SERVER_CONFIG. DISCONNECT 시 Config=INSTANCE 복원. 이동 로직 전체(6파일) INSTANCE→Config 교체. |
| C-12 | 13-2 설정 배포 프로토콜 전체 구현 | C-11 | [x] → SmartMovingConfig에 SM_VERSION("1.0"), globalConfig/serverConfig 플래그, toArray() 추가. SmartMovingServer.initialize: globalConfig=true이면 INSTANCE.toArray() 전송, 아니면 빈 배열. processConfigContentPacket: first 추적+ConfigInfo 전송 추가. processConfigChangePacket: no-op 연결. processSpeedChangePacket: difference!=0이면 Config.changeSpeed(). |
| C-13 | `isRopeSliding` 애니메이션 구현 | A-16 | [x] → SmartMovingClientState에 isRopeSliding 필드 추가. sm_animateRopeSliding(): time=animationProgress*0.15, body.pitch=Sixteenth+Sixtyfourth*cos(time), arm.pitch=Half-torsoX, arm.roll=±(Sixteenth+Thirtytwoth), leg.roll=±Thirtytwoth, leg.pitch=Sixtyfourth*cos(time∓Quarter), head.pitch=Eighth, head.roll=clamp(wrapDegrees(camYaw-moveYaw)*DEG_TO_RAD, ±Sixteenth). if-else 체인 최우선 분기로 추가. bodyYaw/anySmState에도 isRopeSliding 포함. |
| C-14 | 서버 물리 재현 범위 결정 및 구현 | A-17 | [x] → setCrawling() 추가+processStatePacket에서 호출(cooldown=10 설정). MixinServerPlayerEntity.tick()HEAD: crawlingCooldown--. MixinLivingEntity.tickMovement()TAIL: isSmall 시 offsetBox(+0.25Y)와 standardBox 차집합 엔티티에 onPlayerCollision() 호출. SMALL_SIZE_ITEM_GRAB_HEIGHT=0.25 상수화. addMovementStat/tickStatusEffects는 Yarn명 미확인 → 기존 TODO 스텁 유지. |

---

## 리서치 청크 계획

> 한 세션 = 청크 하나. 토큰 초과 방지를 위해 청크당 확인 항목 4~6개 이내.
> 청크 완료 기준: 해당 청크의 모든 항목에 `[x]`, 결과 파일 경로 기재, 커밋 완료.

---

### 청크 R-01: SmartMovingContext 상수 + Config 공식 확인

**확인 항목**: A-01, A-02, A-03, A-04

**읽을 소스**:
- `https://github.com/makamys/SmartMoving` → `SmartMovingContext.java` (전체)
- `SmartMovingConfig.java` → `getJumpExhaustionGain()` 메서드
- `SmartMovingOptions.java` → 해당 값 정의

**추출할 것**:
- `SwimCrawlWaterBorder`, `playerSwimWaterBorder`, `SwimSoundDistance` 정확한 값
- `getJumpExhaustionGain()` 공식 전체

**결과 기록 위치**: `docs/research/original/smartmoving/moving/SmartMovingContext.md` (추가)

**완료**: [x] — A-01/A-02/A-03은 기존 파일에 이미 기록됨. A-02 런타임 공식 SmartMovingContext.md에 추가. A-04는 SmartMovingClientConfig.md에 이미 기록됨.

---

### 청크 R-02: Button + Jump 카운터 + wallJump 조건 확인

**확인 항목**: A-05, A-06, A-07, A-08

**읽을 소스**:
- `Button.java` 전체
- `SmartMovingSelf.java` → `updateEntityActionState()` 내 더블클릭 카운터 로직
- `SmartMovingSelf.java` → `handleJumping()` 내 `jumpMotionX/Z` 저장 시점
- `SmartMovingSelf.java` → `handleWallJumping()` 전체

**추출할 것**:
- 전체 커스텀 키 목록 및 역할
- leftJumpCount/rightJumpCount/backJumpCount 증가 조건, 임계값
- wallJumpCount 최대값, continueWallJumping 전환 조건
- jumpMotionX/Z 저장 정확한 위치 (어느 메서드, 어느 조건)

**결과 기록 위치**: `docs/research/original/smartmoving/moving/Button.md` (추가) / `docs/research/mapping/jump.md` (추가)

**완료**: [x] — A-05~A-08 전체 확인. 기본값 3틱/최솟값 2틱 더블클릭 타이머, jumpMotionX/Z는 handleJumping() 최상단 단일 저장, 커스텀 키 4개 확인.

---

### 청크 R-03: processBlockCode + updateEntityActionState 확인

**확인 항목**: A-09, A-10

**읽을 소스**:
- `SmartMovingClient.java` → `processBlockCode()` 전체
- `SmartMovingSelf.java` → `updateEntityActionState()` 내 processBlockCode 결과 반영 경로

**추출할 것**:
- 채팅 마커 포맷 (앞 4자, 뒤 4자 정확한 문자열)
- 12개 기능 on/off 배열 구조 및 파싱 로직 전체
- 결과가 어느 필드(boolean 배열 또는 개별 필드)에 저장되는지

**결과 기록 위치**: `docs/research/original/smartmoving/moving/SmartMovingClient.md` (추가)

**완료**: [x] — A-09: processBlockCode는 SmartMovingComm.java에 있음(SmartMovingClient.java 아님). SmartMovingComm.md에 이미 완전히 기록됨. A-10: updateEntityActionState 내 2347~2357줄에서 updateCounter<10 조건으로 초기 10틱만 채팅 히스토리 스캔. 결과는 Config→ServerConfig 전환으로 간접 반영.

---

### 청크 R-04: getPoses + reverseHandleMaterialAcceleration + 서버 물리 확인

**확인 항목**: A-11, A-12, A-17

**읽을 소스**:
- `SmartMovingSelf.java` → `getPoses()` 메서드 및 호출 경로
- `SmartMovingSelf.java` → `reverseHandleMaterialAcceleration()` 전체
- `SmartMovingServerPlayerBase.java` 전체 (서버 물리 범위)

**추출할 것**:
- `getPoses()` 반환 구조 및 어디서 어떻게 사용되는지
- `reverseHandleMaterialAcceleration()` 실제 로직 및 1.21.1에서 필요 여부
- 서버에서 수행하는 물리 계산 전체 목록

**결과 기록 위치**: `docs/research/original/smartmoving/playerapi/SmartMovingServerPlayerBase.md` (추가)

**완료**: [x] — A-11: getPoses()는 SM 전체 계층에 없음(1.7.10 MC에 EntityPose API 없음), 대응은 player.height/boundingBox.maxY 직접 변경. A-12: SmartMovingBase.java에 있고 이미 SmartMovingBase.md에 기록됨, 1.21.1 필요 여부는 B-11 미확인으로 보류. A-17: SmartMovingServer.java 전체 확인, 서버는 실제 물리 벡터 계산 없이 hitbox조정/floatKick억제/낙하리셋/crawlingCooldown/소진필터링만 담당. state 패킷 비트 위치 7개 확인(isCrawling=bit13, isSmall=bit15, isClimbing=bit14, 등). isSliding은 서버가 추출 안 함.

---

### 청크 R-05: 중간 노드 계층 구조 + isRopeSliding 애니메이션

**확인 항목**: A-15, A-16

**읽을 소스**:
- `SmartMovingModel.java` → `setRotationAngles()` isRopeSliding 분기 전체
- `ModelPlayer.java` (SmartRender) → bipedOuter 계층 초기화 전체
- `SmartRenderModel.java` → 모델 계층 구조 정의

**추출할 것**:
- bipedOuter → bipedTorso → bipedBreast → bipedNeck/bipedPelvic/bipedShoulder 각 노드의:
  - 부모/자식 관계
  - 초기 pivotX, pivotY, pivotZ
  - 초기 rotateAngleX, rotateAngleY, rotateAngleZ
- isRopeSliding 분기: 팔/다리/몸통 각도 전체
- 1.21.1에서 이 계층을 어떻게 대응시킬 것인지 (교차 분석)

**결과 기록 위치**: `docs/research/original/smartmoving/render/SmartMovingModel.md` (추가) / `docs/research/mapping/animation_system.md` (추가)

**완료**: [x] → A-15: SmartRenderModel.java 생성자 직접 확인, 15노드 계층·pivot 값 기재. A-16: SmartMovingModel.md에 이미 완전 기록됨 확인.

---

### 청크 R-06: State 패킷 비트맵 — isCrawling/isSliding 비트 확인

**확인 항목**: A-13, A-14

**읽을 소스**:
- `SmartMovingOther.java` 전체 (State 패킷 비트 배치)
- `SmartMovingSelf.java` → State 패킷 전송 시 isCrawling/isSliding 세팅 코드

**추출할 것**:
- isCrawling이 State 패킷 long의 몇 번 비트인지
- isSliding이 State 패킷 long의 몇 번 비트인지
- 서버→클라이언트 릴레이 경로에서 이 비트들이 어떻게 처리되는지

**결과 기록 위치**: `docs/research/original/smartmoving/moving/SmartMovingOther.md` (추가)

**완료**: [x] → A-13: isCrawling=bit13 (디코딩·인코딩 양쪽 확인). A-14: isSliding=bit21 (서버는 추출 안 함, 클라이언트 렌더링 전용 확인). 릴레이 경로 전체 확인. SmartMovingOther.md R-06 섹션 기재.

---

### 청크 R-07: vanilla EntityPose 시스템

**확인 항목**: B-01, B-02, B-03, B-04

**읽을 소스** (Fabric Loom 디컴파일):
- `net.minecraft.entity.EntityPose` enum 전체
- `net.minecraft.entity.player.PlayerEntity.getEntityPose()` 전체
- `net.minecraft.entity.LivingEntity.getPoses()` 전체
- `net.minecraft.entity.LivingEntity.updatePose()` / `trySetPose()` 전체

**추출할 것**:
- EntityPose 전체 값 목록
- 커스텀 EntityPose 추가 가능 여부 (enum이므로 일반적으로 불가 → 대안 확인)
- getEntityPose() 조건 분기 전체 (어떤 상태에서 어떤 포즈)
- getPoses()가 어디서 호출되고 반환값을 어떻게 사용하는지
- updatePose() → calculateDimensions() 호출 여부

**결과 기록 위치**: `docs/research/vanilla/EntityPose_system.md` (신규)

**완료**: [x] → B-01: EntityPose 18개 전체 확인(SLIDING=index15 포함), 커스텀 추가 불가. B-02: updatePose() 조건 분기 전체 확인. B-03: getPoses()는 vehicle dismount 전용. B-04: setPose→calculateDimensions 자동 호출 확인. vanilla/EntityPose_system.md 신규 작성.

---

### 청크 R-08: vanilla LivingEntity 물리 미확인

**확인 항목**: B-05, B-10, B-11, B-12, B-13, B-14, B-15

**읽을 소스**:
- `LivingEntity.java` → `updateLeaningPitch()` 전체
- `LivingEntity.java` → `handleFluidAcceleration()` 내 흐름 가속 코드
- `LivingEntity.java` → reverseHandleMaterialAcceleration에 해당하는 코드 탐색
- `EntityAttributes.java` → `GENERIC_GRAVITY` 초기값
- `AbstractBlock.java` 또는 `AbstractBlockState.java` → getSlipperiness() Yarn명
- `LivingEntity.java` / `Entity.java` → `isInsideWall()` 재확인
- `LivingEntity.java` / `PlayerEntity.java` → 수영 소리 자동 재생 경로

**추출할 것**:
- `updateLeaningPitch()` 증가/감소 수치 정확히
- `handleFluidAcceleration()` 내 흐름 가속 크기 (0.014D 여부)
- GENERIC_GRAVITY 기본값
- getSlipperiness() 정확한 Yarn 메서드명
- isInsideWall() 정확한 Yarn명 재확인
- 수영 소리 재생 코드 경로 — 우리 코드와 중복 재생 여부

**결과 기록 위치**: `docs/research/vanilla/LivingEntity_travel.md` (추가) / `docs/research/vanilla/LivingEntity_physics_misc.md` (신규)

**완료**: [x] → B-05: updateLeaningPitch() +0.09F/-0.09F, 0.0~1.0 범위. B-10: SPEED_IN_WATER=0.014 확인. B-11: 1.21.1에 reverseHandleMaterialAcceleration 없음. B-12: GENERIC_GRAVITY 기본값 0.08. B-13: Block.getSlipperiness() Yarn명 확인. B-14: isInsideWall() intermediary=method_5757 확인. B-15: Entity.move() 내 playSwimSound() 경로 확인. LivingEntity_physics_misc.md 신규 작성.

---

### 청크 R-09: vanilla BipedEntityModel 렌더링 미확인

**확인 항목**: B-06, B-07, B-08, B-09

**읽을 소스**:
- `net.minecraft.client.render.entity.model.BipedEntityModel` 전체
- `net.minecraft.client.model.ModelPart` → rotate(MatrixStack), xScale/yScale/zScale 필드 탐색

**추출할 것**:
- `animateArms()` 전체 코드
- `setAngles()` 시그니처 — onGround 파라미터 타입 및 전달값
- ModelPart에 xScale/yScale/zScale 필드 존재 여부
- ModelPart.rotate()가 pitch→yaw→roll 순서로 적용하는지 정확한 코드

**결과 기록 위치**: `docs/research/vanilla/BipedEntityModel_detail.md` (신규)

**완료**: [x] → B-06: animateArms() 전체 코드 확인(handSwingProgress>0 조건, body.yaw 기반). B-07: setAngles() onGround 파라미터 없음, 5개 float 파라미터 확인(limbPos/Speed/animProgress/headYaw/headPitch). B-08: xScale/yScale/zScale 모두 존재, 기본값 1.0F. B-09: rotationZYX(roll,yaw,pitch) = pitch→yaw→roll 순 적용. BipedEntityModel_detail.md 신규 작성.

---

### 청크 R-10: vanilla MatrixStack + bodyYaw ModifyArg

**확인 항목**: B-17, B-18

**읽을 소스**:
- `PlayerEntityRenderer.setupTransforms()` — bodyYaw가 몇 번째 파라미터인지 시그니처 전체 확인
- `net.minecraft.client.util.math.MatrixStack` — rotation API 전체

**추출할 것**:
- setupTransforms() 정확한 시그니처 및 bodyYaw 파라미터 인덱스 (0-based)
- MatrixStack rotation 방법 전체 목록 (multiply 외 다른 방법 있는지)
- @ModifyArg 적용 시 정확한 index 값

**결과 기록 위치**: `docs/research/vanilla/PlayerEntityRenderer_setupTransforms.md` (추가) / `docs/research/mapping/animation_system.md` (추가)

**완료**: [x] → B-17: setupTransforms bodyYaw = index 3 (Vineflower 직접 확인). B-18: MatrixStack.multiply(Quaternionf)가 유일한 회전 메서드, rotateX/Y/Z 없음, RotationAxis API 전체 확인. PlayerEntityRenderer_setupTransforms.md R-10 섹션 추가. animation_system.md R-10 섹션 추가.

---

### 청크 R-11: vanilla DataTracker API + 채팅 이벤트

**확인 항목**: B-16, B-19

**읽을 소스**:
- `net.minecraft.entity.data.DataTracker` — 커스텀 엔트리 등록 구조
- `net.minecraft.entity.data.TrackedDataHandlerRegistry` — 등록 가능한 타입
- Fabric API `ClientPlayNetworkHandler` / `ClientReceiveMessageEvents` — 채팅 raw text 접근

**추출할 것**:
- DataTracker에 커스텀 TrackedData 추가하는 방법 (Mixin으로 추가 시 패턴)
- TrackedDataHandlerRegistry에 boolean 타입 핸들러 등록 방법
- ClientReceiveMessageEvents에서 § 코드가 포함된 raw 문자열 접근 가능 여부

**결과 기록 위치**: `docs/research/vanilla/DataTracker_custom_entry.md` (신규)

**완료**: [x] → B-16: DataTracker.registerData()+Builder.add() Mixin 패턴 확인. B-19: ClientReceiveMessageEvents GAME/CHAT 이벤트 시그니처 확인, Text.getString()은 § 코드 없는 평문 반환. DataTracker_custom_entry.md 신규 작성.

---

### 청크 R-12: 교차 분석 — EntityPose 전략 확정

**의존**: R-07 완료 후 진행

**확인 항목**: C-02, C-03

**분석 내용**:
- SM 헤드점프 EntityPose: SWIMMING 사용 시 문제점 전체 확인, 대안 (FALL_FLYING 등) 검토
- SM 슬라이딩 EntityPose: STANDING vs SWIMMING vs 다른 포즈 — 각 포즈가 어떤 vanilla 동작을 트리거하는지
- calculateDimensions() 연동 포즈 전환 방법

**결과 기록 위치**: `docs/research/mapping/pose_strategy.md` (신규)

**완료**: [x] → C-02: headJumping → SLIDING 포즈 + getBaseDimensions() Mixin(0.6×0.8). C-03: isSliding → SLIDING 포즈(index 15). calculateDimensions() 자동 연동 확인. updatePose() @HEAD Mixin 설계 확정. 미확인 M-01~M-05 신규 청크 R-16으로 추가. mapping/pose_strategy.md 신규 작성.

---

### 청크 R-16: 교차 분석 보완 — isSliding/isCrawling/isCeilingClimbing hitbox 미확인 항목

**의존**: R-12 완료 후 진행

**확인 항목**: M-01, M-02, M-03 (pose_strategy.md 미확인 목록)

**읽을 소스**:
- `https://github.com/makamys/SmartMoving` → `SmartMovingSelf.java` → 크롤링 진입/유지 코드에서 setHeightOffset 호출 위치
- `SmartMovingSelf.java` → isSliding 상태에서 isSmall bit 여부 (addToSendQueue 인코딩)
- `SmartMovingSelf.java` → isCeilingClimbing 진입 시 heightOffset 처리

**추출할 것**:
- `isCrawling` 상태 유지 중 `setHeightOffset(-1F)` 명시적 호출 위치 확인
- `isSliding` 상태에서 `height < 1` (isSmall bit) 여부 — 서버 측 height 변경 유무
- `isCeilingClimbing` 진입/유지 코드에서 heightOffset 처리 여부

**결과 기록 위치**: `docs/research/mapping/pose_strategy.md` (M-01~M-03 항목 업데이트)

**완료**: [x] → M-01: isCrawling setHeightOffset(-1F) 복수 경로 확인(fromSwimmingOrDiving() line ~511/~1382). M-02: isSliding setHeightOffset(-1) line ~2556 확인, height=0.8F→isSmall=true. M-03: isCeilingClimbing setHeightOffset 없음, resetHeightOffset() 확인→height=1.8F. M-04: SM 원본 player.height-0.18F=0.62F 확정. pose_strategy.md 섹션 3-3/7/8 업데이트 완료.

---

### 청크 R-13: 교차 분석 — 중간 노드 대응 + 6-axis rotation 설계

**의존**: R-05, R-09 완료 후 진행

**확인 항목**: C-08, C-09

**분석 내용**:
- bipedOuter 계층의 각 노드를 1.21.1 ModelPart에 어떻게 대응시킬 것인지
- 6-axis rotation을 MatrixStack으로 구현하는 정확한 방법
- 각 SM 상태(climbing/swim/crawl)에서 비표준 회전 순서 적용 계획

**결과 기록 위치**: `docs/research/mapping/animation_system.md` (추가)

**완료**: [x] → C-08: 비표준 회전 순서가 필요한 파트 11종 전체 목록 확정, MatrixStack 회전 순서별 구현 패턴 확정(B-18 기반), ModelPart smRotationOrder Mixin 전략 설계. C-09: isFlying/isHeadJump body 기울기 → setupTransforms X rotate + setAngles head.pitch 보정 전략 확정, 등가 증명 완료. 미확인 M-06/M-09/M-10/M-11 추가 → R-17로 분리. animation_system.md R-13 섹션 추가.

---

### 청크 R-17: 교차 분석 보완 — ModelRotationRenderer.render() + setTransform 호출 시점

**의존**: R-13 완료 후 진행

**확인 항목**: M-06, M-09, M-10, M-11 (animation_system.md 미확인 목록)

**읽을 소스**:
- SmartRender GitHub → `ModelRotationRenderer.java` → render() 전체 코드 (rotationPointY /16 여부 확인)
- Fabric Loom 디컴파일 → `ModelPart.java` → `setTransform(ModelTransform)` 전체 코드 + 호출 위치 확인
- Fabric Loom → `PlayerEntityModel.java` → `animateModel()` 또는 `reset()` 등 setTransform 호출 여부 확인

**추출할 것**:
- `ModelRotationRenderer.render()` 내 rotationPointX/Y/Z 처리: `/16F` 나눔 여부, glTranslate vs glRotate 순서
- `setTransform(ModelTransform)` 전체 코드 — xScale/yScale/zScale 리셋 이외에 무엇을 하는지
- vanilla rendering pipeline에서 setTransform이 호출되는 시점 (animateModel? reset? 다른 곳?)
- rotate() @HEAD 취소 시 pivot translate + scale을 수동으로 재구현해야 하는지 확인

**결과 기록 위치**: `docs/research/mapping/animation_system.md` (M-06~M-11 해소 업데이트)

**완료**: [x] → M-06: rotationPointY * 0.0625 = /16 확인, 1.21.1 translate(pivot/16F) 패턴 동일. M-09: rotate() @HEAD 취소 시 3단계(translate+커스텀회전+scale) 수동 구현 필요. M-10: animateModel() 코드 직접 확인, setTransform 호출 없음, setAngles @TAIL 안전. M-11: bipedTorso.rotationPointY=3F → matrices.translate(0, 3F/16F, 0) X 회전 전 삽입. **R-13 C-08-2 오류(MatrixStack call 순서 전부 역순) 수정 완료.** animation_system.md C-08-2 섹션 수정 + R-17 섹션 추가.

---

### 청크 R-14: 교차 분석 — Config 시스템 설계

**의존**: A-04 확인(R-01) 완료 후 진행

**확인 항목**: C-11, C-12

**분석 내용**:
- SmartMovingConfig / SmartMovingServerConfig / SmartMovingOptions 원본 전환 로직 전체
- 1.21.1에서 서버 설정 배포 프로토콜 구현 방법 (패킷 흐름)
- Config = ServerConfig / Config = Options 전환 시점 및 조건

**결과 기록 위치**: `docs/research/mapping/config_system.md` (신규)

**완료**: [x] → M-12: writeToProperties(mp,key) 정상 경로(config.write→flat String[]+플레이어별 속도 치환) + disabled 경로([globalConfigKey, "false"] 2원소) 확인. M-13: Java ObjectOutputStream writeByte(2)+writeObject(String[])+writeObject(String) 확인 → 1.21.1 PacketByteBuf+별도 채널 분리로 교체 설계. processConfigPacket 전체 흐름·content null/length 의미 확정. mapping/config_system.md 신규 작성.

---

### 청크 R-15: 교차 분석 — DataTracker 동기화 설계

**의존**: R-06, R-11 완료 후 진행

**확인 항목**: C-05, C-06

**분석 내용**:
- isCrawling/isSliding을 DataTracker로 동기화하는 설계
- 기존 State 패킷 릴레이 방식과 DataTracker 방식 중 선택 이유
- 타인 플레이어 렌더 시 포즈 동기화 전체 경로

**결과 기록 위치**: `docs/research/mapping/network_sync.md` (추가)

**완료**: [x] → C-05: isCrawling DataTracker(SM_CRAWLING BOOLEAN) 방식 확정. 서버 processStatePacket에서 bit 13 추출 후 DataTracker.set() 추가. C-06: isSliding DataTracker(SM_SLIDING BOOLEAN) 방식 채택 — 서버에 bit 21 추출 1줄 추가로 일관성 확보. State 패킷 릴레이는 다른 상태 비트 때문에 유지. 타인 렌더: dataTracker.get() 직접. 로컬 렌더: SmartMovingSelf 필드 유지. network_sync.md 섹션 15 추가.

---

## 청크 진행 순서 요약

```
R-01 (상수) → R-02 (Button/Jump) → R-03 (processBlockCode)
R-04 (getPoses/서버물리) → R-05 (중간노드/RopeSliding) → R-06 (State비트)

병렬 가능:
R-07 (EntityPose vanilla) → R-08 (LivingEntity 물리)
R-09 (BipedEntityModel) → R-10 (MatrixStack/bodyYaw) → R-11 (DataTracker/채팅)

R-07 완료 후: R-12 (EntityPose 전략)
R-05 + R-09 완료 후: R-13 (중간노드/6-axis)
R-01 완료 후: R-14 (Config)
R-06 + R-11 완료 후: R-15 (DataTracker 동기화)
```

모든 청크 완료 → C-01~C-14 구현 시작 (RESEARCH_RULES.md 규칙 5 충족)

---

## 현재 진행 상태

- 완료된 청크: R-01~R-22 전체 완료 (2026-04-22)
- 다음 진행: A 항목 전부 [x] 완료 → C-15~C-43 구현 시작 가능.

---

## 전체 소스 파일 누수 스캔 결과 (2026-04-22)

> 모든 `.java` 파일의 `TODO` / `[미확인]` / no-op 핸들러를 전수 검색하여 아래에 항목화함.
> PORTING_RULES.md 기준 적용: 추정값 임시 사용 = A 항목, 구현 없음 = C 항목.

---

### 신규 A 항목 (원본 소스 미확인)

| ID | 미확인 내용 | 원본 소스 위치 | 코드 위치 | 상태 |
|----|------------|--------------|----------|------|
| A-18 | `_freeClimbingUpSpeedFactor` / `_freeClimbingDownSpeedFactor` 기본값 (현재 1.0D 임시) | SmartMovingConfig.java | SmartMovingClimber.java L187/192 | [x] → PositiveFactor 기본값 1F 확인 (Properties.getDefaultValue). 현재 1.0D 정확함. `SmartMovingClientConfig.md` R-18 섹션 기재. |
| A-19 | `jumpChargeFactor` 보간 공식 세부값 — `verticalJumpFactor` 실제 값 포함 (`-0.078 + 0.498 * verticalJumpFactor * jumpChargeFactor`) | SmartMovingSelf.java | SmartMovingJumper.java L132 | [x] → verticalJumpFactor = _jumpVerticalFactor.value = PositiveFactor 기본값 1F. jumpChargeFactor = getJumpChargeFactor 공식 확인. 현재 코드 정확. `mapping/jump.md` R-19 섹션 기재. |
| A-20 | `Stats.JUMP` 1.21.1 Yarn명 (jump 통계 기록 — `player.incrementStat(...)` 호출에 필요) | vanilla `net.minecraft.stat.Stats` | SmartMovingJumper.java L179 | [x] → `public static final Identifier Stats.JUMP`. 사용: `player.incrementStat(Stats.JUMP)` (Identifier 오버로드). `vanilla/vanilla_yarn_misc.md` A-20 기재. |
| A-21 | `isRunning` 원본 판정 조건 전체 (헤드점프 차지 진입 조건 `isGroundSprinting || isSprintJump || isRunning`) | SmartMovingSelf.java | SmartMovingJumper.java L232 | [x] → isRunning()=isSprinting()&&!isFast&&(onGround||vanilla()), isGroundSprinting=(isFast||isSprinting())&&onGround&&!isSliding&&!isCrawling. SmartMovingClientState에 isSlow/isFast/isFlying 필드 추가, SmartMovingJumper 수정 완료. |
| A-22 | `isSlow` 원본 판정 조건 — 수면 점프 y 임계값 분기 (`isSlow ? 0.37 : 0.6`) | SmartMovingSelf.java | SmartMovingJumper.java L249 | [x] → isSlow = wantSneak && !wantSprint && !isClimbing. threshold = sm.isSlow ? 0.37D : 0.6D으로 수정. sm.isSlow는 C-15(tickEssential)에서 계산됨. |
| A-23 | `Orientation.java` `horizontalCollisionAngle` 계산 로직 전체 (벽점프 반사 각도 계산) | SmartMoving(original) `Orientation.java` | SmartMovingJumper.java L309-311 | [x] → SmartRenderUtilities.getHorizontalCollisionangle() 전체 lookup table 확인. X/Z swap call site 패턴 확인. getHorizontalCollisionangle() SmartMovingJumper에 구현. C-38에서 calculateSeparateCollisions 이식 후 연결. |
| A-24 | `LivingEntity.bodyYaw` 1.21.1 직접 접근 가능 여부 및 Yarn 필드명 (accessor Mixin 필요 여부) | vanilla `LivingEntity.java` | SmartMovingJumper.java L323 | [x] → `public float bodyYaw` (공개 필드). accessor 불필요. `player.bodyYaw = angle` 직접 사용. `vanilla/vanilla_yarn_misc.md` A-24 기재. |
| A-25 | `iceSpeedFactor` SmartMovingConfig 실제 기본값 (현재 1.5F 하드코딩) | SmartMovingConfig.java (원본) | SmartMovingMover.java L52 | [x] → 원본 SmartMovingConfig.java에 `_iceSpeedFactor` 필드 없음. config에서 오는 값이 아님. 1.5F는 원본 SmartMovingMover.java 직접 상수값 여부 확인 필요 (현재 그대로 유지). C-39 수정: SmartMovingConfig 읽기 불필요. |
| A-26 | ConfigChange 권한없음 / 재설정 완료 채팅 메시지 문자열 원본 | SmartMovingComm.java | SmartMovingClient.java L52/100 | [x] → illegal.remote/local 원문 확인. reconfig→update/enable/disable 메시지 확인. en_us.json 추가, SmartMovingClient.java 메시지 표시 구현. SmartMovingComm.md R-21 기재. |
| A-27 | 이동 통계+소진 처리 메서드 Yarn명 (원본 `addMovementStat` 해당 — 1.21.1에서 `travel()` 인라인인지 별도 메서드인지) | vanilla `PlayerEntity.java` / `LivingEntity.java` | MixinServerPlayerEntity.java L86, MixinLivingEntity.java L115 | [x] → 1.21.1에 addMovementStat 독립 메서드 없음. PlayerEntity.travel() 인라인. C-22: travel() HEAD+TAIL inject. `vanilla/vanilla_yarn_misc.md` A-27 기재. |
| A-28 | 포션 업데이트 메서드 Yarn명 (원본 `updatePotionEffects` → 1.21.1 후보: `tickStatusEffects`) | vanilla `LivingEntity.java` | MixinLivingEntity.java L115 | [x] → `protected void tickStatusEffects()` 확인. C-23: tickStatusEffects HEAD+TAIL inject. `vanilla/vanilla_yarn_misc.md` A-28 기재. |
| A-29 | 서버 위치 검증 메서드 Yarn명 (원본 `NetHandlerPlayServer.processPlayer` → 1.21.1 후보: `onPlayerMove`, `handleMovePlayerPacket`) | vanilla `ServerPlayNetworkHandler.java` | MixinServerPlayNetworkHandler.java L39 | [x] → `onPlayerMove(PlayerMoveC2SPacket)` 확인. C-21: @Inject(method="onPlayerMove") 로 구현. `vanilla/vanilla_yarn_misc.md` A-29 기재. |
| A-30 | `jumpMovementFactor`(offGroundSpeed) 0.05F 제어 방법 — 1.21.1 `ClientPlayerEntity`에서 해당 필드/속성 존재 여부 | vanilla `ClientPlayerEntity.java` | MixinLivingEntityClient.java L61 | [x] → jumpMovementFactor 필드 없음. 대신 `protected float getOffGroundSpeed()` 메서드(PlayerEntity). flying→flySpeed, !flying+sprinting→0.026F, otherwise→0.02F. C-41: getOffGroundSpeed @HEAD override. `vanilla/vanilla_yarn_misc.md` A-30 기재. |
| A-31 | `_slideSlipperinessFactor` 기본값 (현재 1.0F 임시) | SmartMovingConfig.java (원본) | SmartMovingConfig.java L69 | [x] → PositiveFactor 기본값 1F 확인. 현재 1.0F 정확함. |
| A-32 | `_slidingSpeedStopFactor` 기본값 (현재 0.01F 임시) | SmartMovingConfig.java (원본) | SmartMovingConfig.java L73 | [x] → PositiveFactor 기본값 1F 확인. 0.01F → 1.0F 수정 완료. |

---

### 신규 C 항목 (미구현 기능)

| ID | 미구현 기능 | 의존 리서치 | 코드 위치 | 상태 |
|----|-----------|-----------|----------|------|
| C-15 | `tickEssential()` 본체 — 상태 패킷 전송, 키 입력 처리, `jumpAvoided` 리셋 등 | — | SmartMovingClientState.java L147 | [ ] |
| C-16 | ConfigInfo 서버 수신 처리 — 클라이언트 SM 버전 검증/로깅 | — | SmartMoving.java L53 | [ ] |
| C-17 | ConfigChange 서버 수신 처리 — 권한 검증 후 Config 적용 + 클라이언트 알림 메시지 | A-26 | SmartMoving.java L59 | [ ] |
| C-18 | SpeedChange 서버 수신 처리 — 권한 검증 후 `changeSpeed()` 호출 및 클라이언트 동기화 | — | SmartMoving.java L65 | [ ] |
| C-19 | Sound 패킷 처리 — SM 사운드 요청을 서버 측에서 주변 플레이어에게 재생 | — | SmartMoving.java L77 | [ ] |
| C-20 | `heightOffset` setPos 위치 보정 — `afterMoveEntity`에서 헤드점프 시 Y 위치 오프셋 적용 | — | MixinEntity.java L71 | [ ] |
| C-21 | "moved wrongly" 완화 — SM 클라이밍/크롤링 중 서버 위치 검증 skip | A-29 | MixinServerPlayNetworkHandler.java L39 | [ ] |
| C-22 | `addMovementStat` Mixin — SM 소진 배치 시스템 HEAD+TAIL 연결 | A-27 | MixinServerPlayerEntity.java L86 | [ ] |
| C-23 | `tickStatusEffects` Mixin — 소진 배치 역전 처리 HEAD+TAIL 연결 | A-28 | MixinLivingEntity.java L115 | [ ] |
| C-24 | 다른 플레이어 State 패킷 수신 처리 — `StatePayload` 수신 시 타인 SmartMovingClientState 갱신 | — | SmartMovingClient.java L38 | [ ] |
| C-25 | `SmartStatistics` 계산 로직 구현 — 렌더 틱마다 `currentVerticalAngle`, `horizontalDistance` 등 갱신 | — | SmartStatistics.java (계산 없음) | [ ] |
| C-26 | 클라이밍 — `climbIntoCount` (크롤-클라이밍 갭 진입 카운터) 상태 필드 및 `setShouldClimbSpeed` 연결 | — | SmartMovingClimber.java L177 | [ ] |
| C-27 | 클라이밍 — `hasClimbCrawlGap`, `isClimbCrawling` 상태 필드 추가 및 `setShouldClimbSpeed` 상한 처리 연결 | — | SmartMovingClimber.java L198 | [ ] |
| C-28 | 클라이밍 — `isClimbHolding`, `isClimbJumping` 상태 필드 추가 및 `setShouldClimbSpeed` TAIL 연결 | — | SmartMovingClimber.java L204 | [ ] |
| C-29 | 클라이밍 — Standard/Simple/Smart 모드 구현 (`getCombinedSpeedFactor` + 모드별 속도 분기) | A-18 | SmartMovingClimber.java L246-250 | [ ] |
| C-30 | 클라이밍 — 대각 4방향(NE/NW/SE/SW) 탐색 추가 | — | SmartMovingClimber.java L263 | [ ] |
| C-31 | 클라이밍 — `wantClimbUp` / `wantClimbDown` 키 입력 기반 방향 제어 | — | SmartMovingClimber.java L284 | [ ] |
| C-32 | 클라이밍 — `climbBackJump`, 클라이밍 중 `wallJump`, `handleCrash()` 처리 | — | SmartMovingClimber.java L312-314 | [ ] |
| C-33 | 클라이밍 — SM 독자 exhaustion 시스템 (`climbExhaustion` 필드 + `climbExhaustionStart/Stop` 로직) | — | SmartMovingClimber.java L240, L341 | [ ] |
| C-34 | 천장 클라이밍 — `grabButton` 키 `wantClimbCeiling` 조건 연결 | — | SmartMovingClimber.java L344 | [ ] |
| C-35 | 천장 클라이밍 — `jgap` 정확한 AABB 충돌 쿼리 계산 (현재 블록 스캔 stub) | — | SmartMovingClimber.java L351, L387-403 | [ ] |
| C-36 | 천장 클라이밍 — 수평 속도 방향 벡터 분해 및 속도 적용 | — | SmartMovingClimber.java L365 | [ ] |
| C-37 | 점프 — `Stats.JUMP` 통계 기록 (`tryJump` 내) | A-20 | SmartMovingJumper.java L179 | [ ] |
| C-38 | 점프 — `Orientation.java` 이식 (`horizontalCollisionAngle` 정확한 계산) | A-23 | SmartMovingJumper.java L309-311 | [ ] |
| C-39 | 속도 팩터 — `iceSpeedFactor` SmartMovingConfig에서 실제값 읽기 | A-25 | SmartMovingMover.java L52 | [ ] |
| C-40 | 속도 팩터 — 달리기(run) 판정 조건 정확한 구현 | A-21 | SmartMovingMover.java L54 | [ ] |
| C-41 | 비행 억제 — `jumpMovementFactor` 0.05F 제어 (`ClientPlayerEntity` offGroundSpeed 억제) | A-30 | MixinLivingEntityClient.java L61 | [ ] |
| C-42 | 렌더 — isFlying/isHeadJumping body X 기울기 (`currentVerticalAngle` 추적 기반) | C-25 | MixinPlayerEntityRenderer.java L142-143 | [ ] |
| C-43 | 렌더 — `MixinPlayerEntityModelClient.bendFactor` 계산 (`currentVerticalAngle` 기반) | C-25 | MixinPlayerEntityModelClient.java L418 | [ ] |

---

### 신규 리서치 청크

#### 청크 R-18: SmartMovingConfig 미확인 기본값 확인

**확인 항목**: A-18, A-25, A-31, A-32

**읽을 소스**:
- `https://github.com/makamys/SmartMoving` → `SmartMovingConfig.java` → `_freeClimbingUpSpeedFactor`, `_freeClimbingDownSpeedFactor`, `_iceSpeedFactor`, `_slideSlipperinessFactor`, `_slidingSpeedStopFactor` 초기값

**추출할 것**:
- 각 팩터 필드의 정확한 초기값 (PositiveFactor 기본값 패턴 확인)
- 관련 메서드/프로퍼티 접근자 구조

**결과 기록 위치**: `docs/research/original/smartmoving/config/SmartMovingClientConfig.md` (추가)

**완료**: [x] → A-18/A-31/A-32: PositiveFactor 기본값 = 1F 확인(Properties.getDefaultValue). A-32 코드 오류 0.01F→1.0F 수정. A-25: 원본에 _iceSpeedFactor 필드 없음 — config 값 아님 확인. SmartMovingClientConfig.md R-18 섹션 기재.

---

#### 청크 R-19: SmartMovingSelf 점프 미확인 값 확인

**확인 항목**: A-19, A-21, A-22, A-23

**읽을 소스**:
- `SmartMovingSelf.java` → `tryJump()` 내 `verticalJumpFactor`, `jumpChargeFactor` 보간 공식
- `SmartMovingSelf.java` → `isRunning` 판정 로직 전체
- `SmartMovingSelf.java` → `isSlow` 판정 로직 (수면 점프 임계값)
- `Orientation.java` → `horizontalCollisionAngle` 계산 전체

**추출할 것**:
- `verticalJumpFactor` 정확한 값
- `jumpChargeFactor` 보간 공식 전체 (`charge/maxCharge * (factor - 1) + 1` 여부)
- `isRunning` 판정 조건 (전진 입력 + !isSneaking + !isSprinting? 또는 별도 속도 임계값?)
- `isSlow` 판정 조건
- `Orientation.horizontalCollisionAngle` 계산 알고리즘

**결과 기록 위치**: `docs/research/mapping/jump.md` (추가)

**완료**: [x] → A-19: verticalJumpFactor=1F(PositiveFactor 기본값) 확인, 현재 코드 정확. A-21: isRunning()/isGroundSprinting 조건 확인, SmartMovingClientState에 isSlow/isFast/isFlying 추가, SmartMovingJumper 수정. A-22: isSlow 조건 확인, swim jump threshold sm.isSlow?0.37:0.6 수정. A-23: getHorizontalCollisionangle lookup table 확인, SmartMovingJumper에 구현. mapping/jump.md R-19 섹션 기재.

---

#### 청크 R-20: vanilla Yarn명 일괄 확인 (통계/포션/위치검증/offGroundSpeed)

**확인 항목**: A-20, A-24, A-27, A-28, A-29, A-30

**읽을 소스** (Fabric Loom 디컴파일):
- `net.minecraft.stat.Stats` → `JUMP` 상수 Yarn명
- `net.minecraft.entity.LivingEntity` → `bodyYaw` 필드 Yarn명, 직접 접근 가능 여부
- `net.minecraft.entity.player.PlayerEntity` → 이동 통계+소진 메서드 (travel() 인라인 vs 별도 메서드)
- `net.minecraft.entity.LivingEntity` → 포션 업데이트 메서드명 (`tickStatusEffects` 여부)
- `net.minecraft.server.network.ServerPlayNetworkHandler` → 위치 검증 메서드명
- `net.minecraft.client.network.ClientPlayerEntity` → `jumpMovementFactor`/`offGroundSpeed` 필드 존재 여부

**추출할 것**:
- `Stats.JUMP` 정확한 Yarn명
- `bodyYaw` 1.21.1 Yarn 필드명, public/protected/private 여부
- 이동 통계+소진 메서드명 (없으면 travel() 어느 지점에서 처리되는지)
- 포션 업데이트 메서드 Yarn명
- 서버 위치 검증 메서드 Yarn명
- offGroundSpeed/jumpMovementFactor 필드 존재 여부 및 Yarn명

**결과 기록 위치**: `docs/research/vanilla/vanilla_yarn_misc.md` (신규)

**완료**: [x] → A-20: Stats.JUMP=Identifier, player.incrementStat(Stats.JUMP). A-24: bodyYaw=public float, 직접 접근. A-27: addMovementStat 없음, travel() 인라인→C-22. A-28: tickStatusEffects() 확인→C-23. A-29: onPlayerMove(PlayerMoveC2SPacket)→C-21. A-30: getOffGroundSpeed() 메서드 대체→C-41. vanilla/vanilla_yarn_misc.md 신규 작성.

---

#### 청크 R-21: SmartMovingComm 채팅 메시지 문자열 확인

**확인 항목**: A-26

**읽을 소스**:
- `SmartMovingComm.java` → `writeNoRightsToChangeConfigMessageToChat()` 전체
- `SmartMovingComm.java` → 재설정 완료 채팅 메시지 문자열

**추출할 것**:
- 권한없음 메시지 정확한 문자열
- 재설정 완료 메시지 정확한 문자열 (있는 경우)

**결과 기록 위치**: `docs/research/original/smartmoving/moving/SmartMovingComm.md` (추가)

**완료**: [x] → A-26: illegal.remote/local/speed 원문 확인. config.server.* 전체 메시지 확인. en_us.json에 smartmoving.message.config.*/speed.* 추가. SmartMovingClient.java ConfigChange/SpeedChange 수신 시 메시지 표시 구현, processConfigContentPacket first/reconfig 분기 메시지 구현. SmartMovingComm.md R-21 섹션 기재.

---

#### 청크 R-22: SmartStatistics 계산 로직 확인

**확인 항목**: C-25 (의존: SmartMovingRender.java 원본 확인)

**읽을 소스**:
- `SmartMovingRender.java` → `render()` 내 SmartStatistics 갱신 코드 전체
- `SmartMovingModel.java` → `setRotationAngles()` 내 SmartStatistics 사용 패턴

**추출할 것**:
- `currentVerticalAngle`, `horizontalDistance` 등 각 필드의 갱신 공식
- 렌더 틱에서 statistics 갱신이 호출되는 정확한 위치
- isFlying/isHeadJumping body 기울기 계산에 사용하는 필드

**결과 기록 위치**: `docs/research/mapping/animation_system.md` (추가)

**완료**: [x] → currentVerticalAngle=atan(yDiff/horizDist), NaN→Quarter(π/2). horizontalDistance=sqrt(xDiff²+zDiff²). currentHorizontalAngle=-atan(xDiff/zDiff), zDiff<0→+Half(π). forwardRotation=lerp(prevYaw, yaw, f2). SmartStatistics.calculateAllStats(): EMA×4 for horiz/vert/all. 1.21.1: prevX/lastRenderX, limbAnimator 대응. animation_system.md R-22 섹션 기재.
