# 교차 분석: 크롤링 / 슬라이딩

원본: SmartMoving 1.7.10 (Forge, PlayerAPI)
대상: 1.21.1 Fabric (Yarn 매핑)
분석 기준 파일: SmartMovingSelf.md, SmartMoving.md, SmartMovingBase.md, SmartMovingContext.md, SmartMovingConfig.md, SmartMovingModel.md, SmartMovingRender.md, LivingEntity_updatePose.md, PlayerEntityModel_setAngles.md, PlayerEntityRenderer_setupTransforms.md, PlayerEntityRenderer_getPositionOffset.md, LivingEntityRenderer_render.md

---

## 상태 필드

### `SmartMoving.isCrawling` / `SmartMoving.isSliding`

- 원본 동작: `public boolean isCrawling; public boolean isSliding;` — SmartMoving 클래스에 선언된 플랫 상태 필드. SmartMovingSelf에서 직접 읽고 씀.
- 1.21.1 대응: 없음 — 직접 구현 필요. PlayerEntity Mixin에 추가하거나 별도 AttachmentType/CompoundTag 등으로 관리.
- 동작 차이: 1.21.1 vanilla에는 isCrawling/isSliding 개념 없음. EntityPose에 SWIMMING이 있으나 크롤링과 수영을 구분하지 않음.
- 포팅 주의사항: 필드가 서버/클라이언트 양쪽에서 사용되므로 동기화 필수. 네트워크 패킷 또는 DataTracker로 관리해야 함.

---

## 크롤링 진입 조건

### `mustCrawl` 판정

- 원본 동작: `crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset` — 위/아래 고체 블록 경계 사이 공간이 플레이어 키(heightOffset 적용 후)보다 좁으면 mustCrawl=true. 크롤링 중 일어설 수 없는 공간이면 강제 크롤 유지.
- 1.21.1 대응: `PlayerEntity.updatePose()` 내부 `canChangeIntoPose(STANDING)` 체크. `world.isSpaceEmpty(this, getDimensions(STANDING).getBoxAt(pos).contract(1E-7))` 로 공간 확인.
- 동작 차이: vanilla는 EntityPose enum 기반 공간 체크, SM은 직접 minY/maxY 고체 경계를 계산(`getMinPlayerSolidBetween`, `getMaxPlayerSolidBetween`). SM이 더 정밀하게 경계를 계산하며 클라이밍 상태 등 복합 조건도 반영.
- 포팅 주의사항: vanilla의 `canChangeIntoPose` 로직이 mustCrawl 역할을 어느 정도 대체하지만, SM 특유의 복합 상태(isCrawlClimbing 등)에서는 별도 조건 체크 필요.

### `wantCrawl` / `canCrawl` 조건 체인

- 원본 동작: `wantCrawl` = 스니크 키 입력 + 현재 서 있음. `canCrawl` = Config._crawl.value && !isFlying && !isSwimming && !isDiving && ... (여러 비양립 상태 제외). `mustCrawl` OR (`wantCrawl` AND `canCrawl`) → 크롤링 진입.
- 1.21.1 대응: 없음 — 직접 구현 필요. Mixin으로 PlayerEntity.tickMovement() 내에 조건 삽입.
- 동작 차이: vanilla에 wantCrawl/canCrawl 체인 없음. SWIMMING 포즈 진입은 updatePose()가 자동 처리하므로 SM 논리와 타이밍이 다를 수 있음.
- 포팅 주의사항: canCrawl 조건에서 비양립 상태(수영, 다이빙, 클라이밍, 플라이 등)를 모두 명시적으로 열거해야 함.

### `toCrawling()` 메서드 (SmartMovingSelf lines 3047-3054)

- 원본 동작: `isCrawling = true; crawlToggled = true; ignoreNextStopSneakButtonPressed = true;` — 크롤링 전환 시 토글 플래그와 스니크 버튼 무시 플래그를 함께 세팅.
- 1.21.1 대응: 없음 — 직접 구현 필요.
- 동작 차이: vanilla 스니크는 토글 방식이 아닌 키 홀드 방식. SM은 스니크 키로 크롤링을 토글하기 위해 `ignoreNextStopSneakButtonPressed`를 별도로 관리.
- 포팅 주의사항: 1.21.1 스니크 키 이벤트 처리 방식 확인 필요. KeyBinding.wasPressed() vs isPressed() 차이에 따라 구현 방식 달라짐.

### `standupIfPossible()` (SmartMovingSelf lines 2165-2243)

- 원본 동작: 크롤링 중 일어설 공간이 있으면 isCrawling=false로 전환. 공간 없으면 mustCrawl 유지.
- 1.21.1 대응: `canChangeIntoPose(STANDING)` 체크 패턴으로 대체 가능.
- 동작 차이: SM은 직접 bounding box 조작으로 공간 확인, vanilla는 EntityPose 기반 getDimensions().getBoxAt() 사용.
- 포팅 주의사항: SWIMMING 포즈로 크롤링을 구현할 경우, 일어서기 시도 시 CROUCHING을 거쳐 STANDING으로 가는 vanilla updatePose() 흐름과 충돌 가능. Mixin 개입 필요.

### `toSlidingOrCrawling()` (SmartMovingSelf lines 1607-1631)

- 원본 동작: 헤드점프 착지 후 속도/공간 조건에 따라 슬라이딩 또는 크롤링으로 전환. `isHeadJumping && onGround && (sprintKey || runKey)` → isSliding=true, 그 외 → isCrawling=true.
- 1.21.1 대응: 없음 — 직접 구현 필요.
- 동작 차이: 헤드점프 자체가 SM 독자 기능이므로 전환 로직 전체 직접 구현.
- 포팅 주의사항: 착지 시점 감지(handleLand/onLanding)와 연동 필요.

---

## Hitbox 조정

### `setHeightOffset(-1F)` + bounding box 조작

- 원본 동작: `heightOffset = -1F` 설정 후 `boundingBox.minY += 1F` (아래쪽 1블록 축소), `height += 1F` (내부 키 보정). 크롤링/수영/다이빙/헤드점프에서 동일 패턴 사용. afterMoveEntity()에서 posY 보정: `posY += heightOffset` (즉 posY -= 1F 효과 없음, 반대로 minY 기준).
- 1.21.1 대응: `EntityPose.SWIMMING` → dimensions = 0.6W × 0.6H, eyeHeight = 0.4F. `Entity.calculateDimensions()` → `setBoundingBox()`로 hitbox 자동 갱신.
- 동작 차이:
  - SM: boundingBox.minY를 직접 +1F 조작 → 플레이어 발 위치 유지하며 위쪽을 줄임 (키 1→0).
  - vanilla SWIMMING: 중심 기준으로 0.6×0.6 hitbox. 높이 1블록짜리 공간이 있어야 크롤링 가능한 SM과 다르게, vanilla는 0.6H로 더 낮은 공간 통과 가능.
  - SM heightOffset 패턴은 크롤링 hitbox = 약 1블록 높이, vanilla SWIMMING = 0.6블록 높이.
- 포팅 주의사항: **이중 적용 불가**. SWIMMING 포즈 사용 시 SM의 boundingBox 직접 조작은 제거해야 함. 공간 통과 가능 높이가 SM(1블록)과 vanilla(0.6블록)로 다르므로 맵 호환성 문제 발생 가능. SM 행동을 원할 경우 커스텀 EntityDimensions 주입 필요.

---

## 포즈 / vanilla 크롤링 포즈

### `EntityPose.SWIMMING` — 크롤링 포즈로 사용

- 원본 동작: SM 원본에 EntityPose 개념 없음. isCrawling 불리언으로 모든 분기 처리.
- 1.21.1 대응: `EntityPose.SWIMMING` — 공간 부족 시 `PlayerEntity.updatePose()`가 자동으로 CROUCHING 시도 → 실패 → SWIMMING 강제 적용. 1블록 높이 공간에서 vanilla 크롤링이 이미 SWIMMING 포즈로 구현됨.
- 동작 차이:
  - vanilla updatePose()는 스니크 키 없이도 천장이 낮으면 자동 SWIMMING 진입.
  - SM은 스니크 키 입력(wantCrawl)이나 mustCrawl 조건 명시적으로 필요.
  - vanilla SWIMMING은 수영과 크롤링을 구분하지 않음 → leaningPitch가 두 경우 모두 증가.
- 포팅 주의사항: vanilla가 자동 SWIMMING 진입하는 타이밍과 SM isCrawling 플래그 세팅 타이밍이 맞지 않을 수 있음. SM 크롤링 상태를 vanilla SWIMMING 포즈와 강결합하면 수영과의 충돌 불가피. 별도 구분 메커니즘(DataTracker 등) 필요.

### `isInSwimmingPose()` 연쇄 효과

- 원본 동작: SM에 isInSwimmingPose() 없음. 렌더는 isCrawling 플래그 직접 참조.
- 1.21.1 대응: `LivingEntity.isInSwimmingPose()` — SWIMMING 포즈 또는 FALL_FLYING 포즈이면 true. true이면 → `getLeaningPitch()` 증가 → `animateModel()` leaningPitch 세팅 → `setupTransforms()` Branch 2 진입 → X축 -90° 회전 + translate(0,-1,0.3).
- 동작 차이: SM 크롤링 렌더는 X축 회전 없이 bipedTorso.rotateAngleX = Quarter-Thirtytwoth 등 개별 파트 회전. vanilla SWIMMING 포즈를 사용하면 setupTransforms에서 전체 모델이 X축으로 눕혀짐.
- 포팅 주의사항: **크롤링에 SWIMMING 포즈를 사용하면 setupTransforms Branch 2가 자동 활성화**되어 SM 크롤링 렌더와 완전히 다른 결과 발생. Mixin으로 `isInSwimmingPose()` 결과를 크롤링 시 false로 오버라이드하거나, setupTransforms를 직접 오버라이드해야 함.

---

## 속도 제어

### `getConfigSpeedFactor()` → `_crawlFactor` 적용

- 원본 동작: `isCrawling → speedFactor *= Config._crawlFactor.value` (기본 0.15F). getSlowInputSpeedFactor()와 곱해서 최종 이동 속도 결정. `beforeMoveEntity()`에서 적용.
- 1.21.1 대응: 없음 — 직접 구현 필요. `LivingEntity.travel()` Mixin에서 이동 벡터 스케일 조정.
- 동작 차이: SM은 PlayerAPI의 beforeMoveEntity() 훅으로 이동 전 속도 조작. 1.21.1은 travel() 내부에서 `movementSpeed` 또는 이동 입력 벡터를 조작해야 함.
- 포팅 주의사항: travel() 내부 구조가 1.21.1에서 변경됨. movementSpeed 필드 또는 getMovementSpeed() 오버라이드로 접근 필요. 0.15F 배율은 매우 느리므로 기아 게임플레이에도 영향.

### `distanceWalkedModified` 억제 (beforeMoveEntity/afterMoveEntity)

- 원본 동작: `isSliding || isCrawling` 시 beforeMoveEntity()에서 `distanceWalkedModified` 현재값 저장 후, move() 이후 afterMoveEntity()에서 저장값 복원 → limbSwing 애니메이션 갱신 방지.
- 1.21.1 대응: `LivingEntity.limbAnimator` (LimbAnimator 클래스). `limbAnimator.updateLimbs(speed)` 가 tickMovement()에서 호출됨. `limbAnimator.getPos()` / `getSpeed()` 가 렌더에서 limbSwing/limbSwingAmount로 사용됨.
- 동작 차이: 1.21.1에서는 distanceWalkedModified 필드가 없고 LimbAnimator 객체로 관리. 억제하려면 limbAnimator.updateLimbs 호출을 막거나, LimbAnimator 내부 pos/speed를 직접 조작해야 함.
- 포팅 주의사항: LimbAnimator의 내부 필드가 접근 가능한지(Mixin accessor 필요 여부) 확인 필요. SM 크롤링 애니메이션은 자체 limbSwing 계산을 사용하므로 vanilla limbAnimator와 분리 관리해야 함.

---

## 크롤링 중 스니킹 동작

### `isSneaking()` 오버라이드

- 원본 동작: `!Config._crawlOverEdge.value && isCrawling && !isClimbing → return true` — 크롤 중 스니킹으로 간주하여 엣지에서 떨어지지 않음.
- 1.21.1 대응: `PlayerEntity.isSneaking()` 오버라이드 또는 Mixin. vanilla 스니킹은 `getPose() == CROUCHING`에 기반.
- 동작 차이: SM은 isCrawling 플래그로 isSneaking 결과를 강제. vanilla는 포즈 기반이므로 SWIMMING 포즈로 크롤링 구현 시 isSneaking()=false가 됨 → 엣지 보호 없음.
- 포팅 주의사항: 엣지 보호 원하면 `LivingEntity.travel()`의 엣지 스텝 로직과 함께 Mixin 필요. Config._crawlOverEdge 해당 옵션도 구현.

### `getPositionOffset()` — 크롤링 Y 오프셋

- 원본 동작: SM 렌더에서 타인 플레이어 크롤 시 `d1 += 0.125D` (non-local player, isSneaking && isCrawl 조건).
- 1.21.1 대응: `PlayerEntityRenderer.getPositionOffset()` — CROUCHING 포즈 시 `Vec3d(0, -scale*0.125, 0)`. SWIMMING 포즈 시 Vec3d.ZERO.
- 동작 차이: SM은 렌더러에서 직접 Y 보정. vanilla는 getPositionOffset()으로 처리. SWIMMING 포즈로 크롤링 구현 시 vanilla는 Y 오프셋 0 반환 → SM의 0.125 보정 재현 불가.
- 포팅 주의사항: `PlayerEntityRenderer.getPositionOffset()` Mixin으로 isCrawling 시 Vec3d(0, -0.125*scale, 0) 반환 추가 필요.

### `model.sneaking` 자동 적용 충돌

- 원본 동작: SM에서 sneaking 모델 플래그는 SmartMovingRender가 직접 제어.
- 1.21.1 대응: `LivingEntityRenderer.setModelPose()` → `model.sneaking = entity.isInSneakingPose()` (= `getPose() == CROUCHING`). sneaking=true 시 BipedEntityModel.setAngles()에서 body.pitch=0.5F, 모든 파트 pivotY +3.2~5.2F.
- 동작 차이: CROUCHING 포즈 사용 시 sneaking=true가 자동으로 세팅되어 pivotY 전체 이동 발생. SWIMMING 포즈 사용 시 sneaking=false → pivotY 이동 없음. SM 크롤링 모델은 sneaking pivotY 적용 없이 자체 rotationPoint 사용.
- 포팅 주의사항: CROUCHING 포즈 기반 크롤링 구현 불가 (sneaking pivotY가 SM 크롤 애니메이션 좌표계를 망가뜨림). SWIMMING 포즈 + isInSwimmingPose() 억제 + 자체 Y오프셋이 가장 안전한 경로.

---

## 크롤링 + 클라이밍 복합 상태

### `isCrawlClimbing` / `isClimbCrawling`

- 원본 동작: `isCrawlClimbing = isClimbing && isCrawling` (벽을 기어오르며 크롤). `isClimbCrawling` = 별도 상태 (천장 크롤 진입 중). SmartMovingRender에서 `isCrawlClimb = moving.isCrawlClimbing || (moving.isClimbing && moving.isCrawling)` 로 렌더 분기.
- 1.21.1 대응: 없음 — 직접 구현 필요. 클라이밍 분석 완료 후 연동.
- 동작 차이: vanilla에 복합 상태 없음.
- 포팅 주의사항: 클라이밍 교차 분석 완료 후 이 상태의 우선순위와 렌더 분기를 함께 결정해야 함.

### `SmartMovingBase.getOnLadderOrVine()` — isSmall 보정

- 원본 동작: `isSmall = isCrawling || isSwimming || isDiving || ...` 조건에서 `isSmall=true`이면 `minj--` (래더/바인 탐색 시작 Y를 한 칸 아래로). 크롤 상태에서 래더 감지 범위 확장.
- 1.21.1 대응: `LivingEntity.isClimbing()` / `onLadder()` 관련 메서드 Mixin으로 수정 필요.
- 동작 차이: vanilla 래더 감지는 플레이어 bounding box 기준 고정 범위. 크롤 상태의 낮아진 hitbox를 자동 반영하지 않음.
- 포팅 주의사항: 크롤링 hitbox(SWIMMING)에서 래더 감지가 올바르게 동작하는지 확인 필요. SWIMMING hitbox 0.6H에서 발 위치 기준 래더 감지 범위 계산 달라질 수 있음.

---

## 크롤링 렌더 (SmartMovingModel)

### `isCrawl` 애니메이션 블록 (SmartMovingModel.md lines 572-629)

- 원본 동작:
  - `distance = totalHorizontalDistance * 1.3F`
  - `walkFactor = Factor(currentHorizontalSpeedFlattened, 0F, 0.12951545F)` (파라미터 오버라이드 전 원본 limbSwing 기반 값)
  - `standFactor = Factor(currentHorizontalSpeedFlattened, 0.12951545F, 0F)` (정지 시 1, 이동 시 0)
  - bipedTorso.rotationOrder = YZX, rotateAngleX = Quarter - Thirtytwoth, rotationPointY = 3F
  - bipedRightArm.rotateAngleX = Half + Eighth, rotateAngleY = -Quarter
  - bipedLeftArm.rotateAngleX = Half + Eighth, rotateAngleY = Quarter
  - 다리: cos 기반 보행 애니메이션, bipedRightLeg/bipedLeftLeg Z 개방 패턴
- 1.21.1 대응: 없음 — `PlayerEntityModel.setAngles()` Mixin으로 구현 필요.
- 동작 차이: vanilla BipedEntityModel.setAngles()는 이 로직 없음. 전체 크롤 애니메이션을 Mixin에서 직접 작성해야 함.
- 포팅 주의사항:
  - `currentHorizontalSpeedFlattened`에 해당하는 값: 1.21.1에서 `limbAnimator.getSpeed()` 또는 속도 벡터 수평 성분 직접 계산.
  - `totalHorizontalDistance`에 해당하는 값: `limbAnimator.getPos()`.
  - Quarter/Half/Eighth/Thirtytwoth 상수: SM 내부 상수 (π/4, π/2, π/8, π/32 등). 포팅 시 수치 확인 필요.
  - bipedTorso = 1.21.1 body 파트에 해당.

---

## 크롤링 기타

### `correctOnUpdate()` — renderYawOffset 보정

- 원본 동작: `isSmall = isSwimming || isDiving || isDipping || isCrawling` 조건. `0.02 < deltaYawOffset < 0.05`이고 isSmall이면 renderYawOffset += deltaYaw * 보정계수.
- 1.21.1 대응: `PlayerEntityRenderer` 또는 `LivingEntityRenderer`에서 bodyYaw 보정 로직 필요.
- 동작 차이: 1.21.1 렌더러 구조에서 bodyYaw 보정이 어떻게 이루어지는지 별도 확인 필요.
- 포팅 주의사항: 크롤링 중 회전 시 방향 보정이 없으면 모델이 순간적으로 틀어질 수 있음. 미확인 — 실제 테스트 필요.

### `crawlingThroughWeb` (SmartMovingSelf.handleLand lines 633-663)

- 원본 동작: `crawlingThroughWeb = (isCrawling || isCrawlClimbing) && isInWeb` — 착지 시 거미줄 내 크롤 여부 기록.
- 1.21.1 대응: `LivingEntity.inCobweb` (Yarn: `inCobweb` boolean 필드). 별도 crawlingThroughWeb 플래그는 없음.
- 동작 차이: vanilla는 거미줄 내 이동 감속을 자동 처리. SM이 별도 관리하는 이유는 착지 판정과 거미줄 인터랙션을 결합하기 위함.
- 포팅 주의사항: `inCobweb` 필드와 연동 방식 확인 필요. SM 특수 착지 로직이 거미줄 상태를 필요로 하는지 클라이밍 분석 이후 재검토.

### `_crawlNameTag.value=true` 처리

- 원본 동작: SmartMovingRender에서 `temporaryIsSneaking = true` — 이름표 숨김.
- 1.21.1 대응: `EntityRenderer.hasLabel()` 오버라이드 또는 isSneaking() 오버라이드로 이름표 숨김 제어.
- 동작 차이: 1.21.1에서 이름표 표시 조건이 다를 수 있음 — 미확인.
- 포팅 주의사항: Config 옵션으로 구현 여부 선택 가능하므로 낮은 우선순위.

### `_crawlOverEdge` 옵션

- 원본 동작: `!Config._crawlOverEdge.value && isCrawling && !isClimbing → isSneaking()=true` — 비활성화 시 크롤 중 엣지 보호(스니킹 처리).
- 1.21.1 대응: `LivingEntity.travel()`에서 스니킹 엣지 보호 로직. PlayerEntity.isSneaking() 반환값에 의존.
- 동작 차이: isSneaking() 오버라이드로 구현 가능하나 SWIMMING 포즈와 충돌 가능.
- 포팅 주의사항: travel() 내 엣지 보호 코드 위치 확인 후 Mixin 삽입점 결정 필요.

---

## 슬라이딩

### `SmartMoving.isSliding` + `spawnSlindingParticle` (오타: Slinding)

- 원본 동작: `public boolean isSliding; private float spawnSlindingParticle;` — isSliding 상태와 파티클 타이머 필드. 필드명 오타(Slinding)는 원본 그대로.
- 1.21.1 대응: 없음 — 직접 구현 필요.
- 동작 차이: vanilla에 슬라이딩 상태 없음.
- 포팅 주의사항: 파티클 타이머는 서버 틱에서 관리해야 하는지 클라이언트에서만 처리 가능한지 확인 필요.

### 슬라이딩 진입 조건: `toSlidingOrCrawling()`

- 원본 동작: 헤드점프(isHeadJumping) 착지 후 `sprintKey || runKey` 입력 시 isSliding=true. 그 외 → isCrawling=true. `SlideToHeadJumpingFallDistance = 0.05F` (SmartMovingContext 상수) — 헤드점프 착지 판정 최소 낙하 거리.
- 1.21.1 대응: 없음 — 직접 구현 필요. handleLand/onLanding Mixin 필요.
- 동작 차이: 헤드점프 자체가 SM 독자 기능. 헤드점프 교차 분석 완료 후 연동.
- 포팅 주의사항: 1.21.1 onLanding 콜백 시점과 키 입력 감지 타이밍이 맞아야 함.

### 슬라이딩 수평 감쇠 공식

- 원본 동작 (landMotion() isSliding 분기):
  ```
  damping = 1F / (((1F / slipperiness) - 1F) / 25F * _slideSlipperinessFactor + 1F) * 0.98F
  ```
  - slipperiness: 현재 블록 미끄러움 계수. 얼음(ice)=0.98, 일반=0.6.
  - `_slideSlipperinessFactor`: Config.PositiveFactor (기본값 미확인).
  - 0.98F: 공기 저항 유사 계수.
  - 수평 이동 벡터에 damping 곱해서 감속.
- 1.21.1 대응: `LivingEntity.travel()` 내 `getVelocityMultiplier()` / `Block.getSlipperiness()`. 없음 — 직접 구현 필요.
- 동작 차이: vanilla 슬라이딩 감속 없음. `Block.getSlipperiness()` (Yarn: `slipperiness` 필드, Ice=0.98F, PackedIce=0.98F, 기본=0.6F)로 접근 가능하지만 SM 공식 직접 구현 필요.
- 포팅 주의사항: 1.21.1에서 `BlockState.getBlock().getSlipperiness()` 접근 방법 확인 필요. `Entity.getVelocityMultiplier()` vs Block slipperiness 값 혼동 주의.

### 슬라이딩 방향 제어

- 원본 동작: `isSliding → isSneaking()=false (엣지 보호 없음), renderYawOffset = forwardRotation (SmartMovingRender.rotatePlayer())`. `_slideControlDegrees`: PositiveFactor 기본 1F — 방향 전환 민감도.
- 1.21.1 대응: 없음 — 직접 구현 필요. 렌더 Mixin 필요.
- 동작 차이: vanilla에 슬라이딩 방향 제어 없음. bodyYaw 강제 고정은 1.21.1 렌더러에서 별도 처리 필요.
- 포팅 주의사항: 렌더에서 bodyYaw를 forwardRotation으로 고정하는 로직, 1.21.1 PlayerEntityRenderer Mixin으로 구현.

### 슬라이딩 종료 조건

- 원본 동작: `_slidingSpeedStopFactor` 이하로 속도 감소 시 슬라이딩 종료. 또는 점프 입력(`_jumpSlideExhaustion` 관련 exhaustion 체크).
- 1.21.1 대응: 없음 — 직접 구현 필요. travel() 또는 tickMovement() Mixin.
- 동작 차이: vanilla에 isSliding 종료 조건 없음.
- 포팅 주의사항: velocity 벡터의 수평 크기 계산으로 종료 조건 구현. exhaustion 시스템은 1.21.1 `PlayerEntity.addExhaustion()` 사용 가능.

### 슬라이딩 점프/Exhaustion

- 원본 동작: 슬라이딩 중 점프 시 `_jumpSlideExhaustion` 소모. `_jumpSlideExhaustionGainFactor` 기본 10F (소모 배율), `_jumpSlideExhaustionStopFactor` 기본 90F (이 수준 이상이면 슬라이딩 점프 불가).
- 1.21.1 대응: `PlayerEntity.addExhaustion(float)` (Yarn: `addExhaustion`). `HungerManager.exhaustion` 필드.
- 동작 차이: 1.21.1 exhaustion 시스템은 동일 개념으로 존재. 수치 매핑만 필요.
- 포팅 주의사항: SM의 exhaustion 단위와 1.21.1 exhaustion 단위 비교 필요. 1.21.1은 exhaustion > 4.0이면 saturation/food 감소.

### 슬라이딩 파티클 (SmartMoving.spawnParticles)

- 원본 동작: `isSliding` 시 `spawnSlindingParticle` 타이머 증가. `> Config._slideParticlePeriodFactor.value * 0.1F` (기본 0.5F * 0.1F = 0.05F) 초과 시 파티클 생성 + 타이머 리셋. 파티클: `"blockcrack_<blockId>_<meta>"` 타입, motionX/Z = 이동 방향 * -4D, motionY = 1.5D.
- 1.21.1 대응: `World.addParticle(ParticleEffect, ...)` / `BlockStateParticleEffect(ParticleTypes.BLOCK, blockState)`. 블록 ID/메타 시스템 → BlockState 시스템으로 변경.
- 동작 차이: 1.7.10 blockcrack 파티클 = 1.21.1 `ParticleTypes.BLOCK` + BlockState. 파티클 속도 파라미터 동일 개념으로 존재.
- 포팅 주의사항: 발 위치 블록의 BlockState 얻기: `world.getBlockState(BlockPos.ofFloored(pos).down())`. meta 시스템 없음 — BlockState 직접 전달.

---

## 슬라이딩 렌더 (SmartMovingModel)

### `isSlide` 애니메이션 블록 (SmartMovingModel.md lines 632-671)

- 원본 동작:
  - `distance = totalHorizontalDistance * 0.7F`
  - `walkFactor = Factor(currentHorizontalSpeed, 0F, 1F) * 0.8F`
  - bipedOuter.fadeRotateAngleY = false
  - bipedOuter.rotateAngleY = currentHorizontalAngle (이동 방향 각도)
  - bipedOuter.rotationPointY = 5F
  - bipedOuter.rotateAngleX = Quarter (90° 앞으로 기울기 → 눕는 효과)
  - bipedBody.rotationOrder = YXZ
  - bipedBody.offsetY = -0.4F
  - bipedBody.rotationPointY = +6.5F
  - bipedHead.rotateAngleX = -Eighth - Sixteenth
  - bipedRightLeg/bipedLeftLeg: rotateAngleZ = Thirtytwoth (고정 벌림)
  - bipedRightArm.rotateAngleY = -Quarter, bipedLeftArm.rotateAngleY = Quarter
- 1.21.1 대응: 없음 — `PlayerEntityModel.setAngles()` Mixin으로 구현 필요.
- 동작 차이: SM의 bipedOuter (outer layer 파트)는 1.21.1 PlayerEntityModel의 `jacket`, `leftSleeve`, `rightSleeve`, `leftPants`, `rightPants`, `hat` 파트에 해당. 직접 매핑 필요.
- 포팅 주의사항:
  - `bipedOuter.rotateAngleX = Quarter` (π/4 = 45°? 또는 π/2 = 90°?) — Quarter 상수 정확한 값 확인 필요.
  - bipedOuter가 단일 파트가 아닌 여러 파트의 집합이므로 각각에 적용해야 함.
  - `currentHorizontalAngle`에 해당하는 값: 이동 방향 계산 (yaw 기반 또는 velocity.horizontalAngle).

### `SmartMovingRender.rotatePlayer()` — 슬라이딩 방향 고정

- 원본 동작: `isSliding → renderYawOffset = forwardRotation` — 플레이어 렌더 yaw를 이동 방향으로 강제 고정.
- 1.21.1 대응: `PlayerEntityRenderer` 또는 `LivingEntityRenderer`에서 bodyYaw 처리. `entity.bodyYaw` 필드.
- 동작 차이: 1.21.1에서 bodyYaw는 entity 필드로 접근 가능. 렌더러가 bodyYaw를 행렬 변환에 적용.
- 포팅 주의사항: bodyYaw를 렌더 중 임시 변경하는 방식 또는 renderer Mixin에서 회전 행렬 직접 조작 필요.

---

## 네트워크 패킷 (비트 인코딩)

### `addToSendQueue()` — isCrawling / isSliding 비트

- 원본 동작: 33비트 패킷에서 bit 30 = isCrawling, bit 22 = isSliding. `SmartMovingPacketStream`으로 직렬화.
- 1.21.1 대응: 없음 — 직접 구현 필요. Fabric 네트워킹 API (`ServerPlayNetworking`, `ClientPlayNetworking`), `PacketByteBuf`.
- 동작 차이: 1.21.1은 Fabric 네트워킹 API 사용. 33비트 int 패킷 → 커스텀 패킷 클래스(record + PacketCodec) 권장.
- 포팅 주의사항: 네트워크/서버 동기화 교차 분석에서 전체 패킷 구조 재설계. isCrawling/isSliding 비트 위치는 새 패킷 구조에서 재정의.

---

## 설정 항목 (SmartMovingConfig)

### 크롤링 설정

| 원본 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `_crawl` | boolean | 미확인 | 크롤링 기능 활성화 |
| `_crawlFactor` | DecreasingFactor | 0.15F | 크롤링 이동 속도 배율 |
| `_crawlNameTag` | Modified | — | 크롤 중 이름표 숨김 |
| `_crawlOverEdge` | Unmodified | — | 크롤 중 엣지 보호 비활성화 |
| `_crawlingExhaustionLossFactor` | — | 미확인 | 크롤 중 exhaustion 회복 배율 |
| `_crawlingHungerGainFactor` | — | 미확인 | 크롤 중 hunger 소모 배율 |

### 슬라이딩 설정

| 원본 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `_slide` | boolean | 미확인 | 슬라이딩 기능 활성화 |
| `_slideControlDegrees` | PositiveFactor | 1F | 슬라이딩 방향 전환 민감도 |
| `_slideSlipperinessFactor` | PositiveFactor | 미확인 | 슬라이딩 감쇠 계수 |
| `_slidingSpeedStopFactor` | PositiveFactor | 미확인 | 슬라이딩 종료 최소 속도 |
| `_slideParticlePeriodFactor` | PositiveFactor | 0.5F | 슬라이딩 파티클 생성 주기 |
| `_jumpSlideExhaustion` | — | 미확인 | 슬라이딩 점프 exhaustion 소모 기본값 |
| `_jumpSlideExhaustionGainFactor` | PositiveFactor | 10F | exhaustion 소모 배율 |
| `_jumpSlideExhaustionStopFactor` | PositiveFactor | 90F | 슬라이딩 점프 불가 exhaustion 임계값 |

- 1.21.1 대응: Fabric Config API (FabricLoader + 별도 설정 라이브러리) 또는 직접 JSON 설정 파일 구현.
- 포팅 주의사항: DecreasingFactor/PositiveFactor/Modified/Unmodified 타입은 SM 독자 타입이므로 1.21.1에서 재구현 필요.

---

## 전체 포팅 난이도 요약

### 크롤링

| 항목 | 난이도 | 비고 |
|---|---|---|
| hitbox (heightOffset → SWIMMING 포즈) | 높음 | SWIMMING 포즈의 부작용(setupTransforms Branch 2, leaningPitch) 차단 Mixin 다수 필요 |
| 크롤링 진입/종료 조건 | 중간 | wantCrawl/mustCrawl 체인 직접 구현, vanilla updatePose()와 충돌 관리 |
| 속도 배율 (0.15F) | 낮음 | travel() Mixin으로 처리 |
| 엣지 보호 (isSneaking override) | 낮음 | Mixin 1개 |
| limbSwing 억제 | 중간 | LimbAnimator 내부 접근 필요 (accessor Mixin) |
| 렌더 애니메이션 | 높음 | setAngles() Mixin에서 전체 크롤 포즈 재구현, sneaking 자동 적용 차단 |
| 타인 플레이어 Y 보정 | 낮음 | getPositionOffset() Mixin 1개 |
| 크롤+클라이밍 복합 | 높음 | 클라이밍 분석 완료 후 연동 |

### 슬라이딩

| 항목 | 난이도 | 비고 |
|---|---|---|
| 슬라이딩 진입 (헤드점프 착지) | 높음 | 헤드점프 구현에 종속 |
| 수평 감쇠 공식 | 중간 | Block.slipperiness 접근 후 공식 이식 |
| 방향 고정 (bodyYaw) | 중간 | 렌더 Mixin |
| 파티클 재구현 | 낮음 | ParticleTypes.BLOCK + BlockState |
| 렌더 애니메이션 (눕기) | 높음 | bipedOuter 파트 매핑 + Quarter 상수 값 확인 |
| 종료 조건 / exhaustion | 낮음 | addExhaustion() 사용 |

### 공통

| 항목 | 난이도 | 비고 |
|---|---|---|
| 상태 필드 동기화 | 중간 | DataTracker 또는 커스텀 패킷 |
| Config 시스템 | 낮음 | Fabric Config 라이브러리 선택 후 이식 |

**총평**: 크롤링은 vanilla SWIMMING 포즈의 부작용을 차단하는 Mixin이 다수 필요하여 복잡도가 높다. 슬라이딩은 헤드점프에 종속되어 있고 렌더 파트 매핑이 복잡하다. 두 기능 모두 단독 구현보다 다른 시스템(클라이밍, 헤드점프, 네트워크)과 연동 이후 최종 완성 가능하다.
