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
| C-01 | 4-4 `processBlockCode` — 채팅 설정 파싱 구현 | A-09, A-10, B-19 | [ ] |
| C-02 | 6-3 EntityPose — 헤드점프 포즈 전략 확정 및 구현 | B-01, B-02, B-04 | [ ] |
| C-03 | 6-3 EntityPose — 슬라이딩 포즈 전략 확정 및 구현 | B-01, B-02, B-04 | [ ] |
| C-04 | 6-5 `recalculateDimensions` — 헤드점프 착지 포즈 복원 로직 | B-04, C-02 | [ ] |
| C-05 | 9-1 `isCrawling` DataTracker 동기화 | A-13, B-16 | [ ] |
| C-06 | 9-1 `isSliding` DataTracker 필요 여부 확인 및 구현 | A-14, B-16 | [ ] |
| C-07 | 12-3 bodyYaw `@ModifyArg` — forwardRotation 강제 | B-17, A-15 | [ ] |
| C-08 | 12-4 ModelRotationRenderer 대체 — 6-axis MatrixStack | A-15, B-08, B-09, B-18 | [ ] |
| C-09 | 12-5 중간 노드 부재 — isFlying/isHeadJumping body 기울기 | A-15, C-08 | [ ] |
| C-10 | 12-7 `smallOverGroundHeight` 실제 블록 탐색 계산 구현 | — | [ ] |
| C-11 | 13-1 설정 분리 — ServerConfig vs Options 전환 상태 관리 | A-04 | [ ] |
| C-12 | 13-2 설정 배포 프로토콜 전체 구현 | C-11 | [ ] |
| C-13 | `isRopeSliding` 애니메이션 구현 | A-16 | [ ] |
| C-14 | 서버 물리 재현 범위 결정 및 구현 | A-17 | [ ] |

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

**완료**: [ ]

---

### 청크 R-13: 교차 분석 — 중간 노드 대응 + 6-axis rotation 설계

**의존**: R-05, R-09 완료 후 진행

**확인 항목**: C-08, C-09

**분석 내용**:
- bipedOuter 계층의 각 노드를 1.21.1 ModelPart에 어떻게 대응시킬 것인지
- 6-axis rotation을 MatrixStack으로 구현하는 정확한 방법
- 각 SM 상태(climbing/swim/crawl)에서 비표준 회전 순서 적용 계획

**결과 기록 위치**: `docs/research/mapping/animation_system.md` (추가)

**완료**: [ ]

---

### 청크 R-14: 교차 분석 — Config 시스템 설계

**의존**: A-04 확인(R-01) 완료 후 진행

**확인 항목**: C-11, C-12

**분석 내용**:
- SmartMovingConfig / SmartMovingServerConfig / SmartMovingOptions 원본 전환 로직 전체
- 1.21.1에서 서버 설정 배포 프로토콜 구현 방법 (패킷 흐름)
- Config = ServerConfig / Config = Options 전환 시점 및 조건

**결과 기록 위치**: `docs/research/mapping/config_system.md` (신규)

**완료**: [ ]

---

### 청크 R-15: 교차 분석 — DataTracker 동기화 설계

**의존**: R-06, R-11 완료 후 진행

**확인 항목**: C-05, C-06

**분석 내용**:
- isCrawling/isSliding을 DataTracker로 동기화하는 설계
- 기존 State 패킷 릴레이 방식과 DataTracker 방식 중 선택 이유
- 타인 플레이어 렌더 시 포즈 동기화 전체 경로

**결과 기록 위치**: `docs/research/mapping/network_sync.md` (추가)

**완료**: [ ]

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

- 완료된 청크: R-01, R-02, R-03, R-04, R-05, R-06, R-07, R-08, R-09, R-10, R-11
- 다음 진행: **R-12** (EntityPose 전략 교차 분석)
