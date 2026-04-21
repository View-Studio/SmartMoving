# SmartMoving 1.21.1 Fabric 포팅 구현 체크리스트

> **규칙**
> - 각 항목에는 Mixin 대상 클래스·메서드·at 포인트, @Unique 필드, 인터페이스, 패킷 채널, 서버/클라이언트 구분을 명시
> - ★★★★ 이상 항목: `[위험]` 태그 부착
> - 미확인 항목: `[미확인 — 추가 리서치 필요]` 태그
> - 체크 완료 시 `- [x]` 처리

---

## 1. 준비 — 데이터 타입 변환 & 공통 유틸

### 1-1. Java enum 변환

- [x] `HandsClimbing` Typesafe Enum → Java enum
  - 값: `None, Sink, TopHold, BottomHold, Up, FastUp`
  - 필수 메서드: `max()`, `IsRelevant()`, `IsUp()`, `ToUp()`, `ToDown()`
- [x] `FeetClimbing` Typesafe Enum → Java enum
  - 값: `None, BaseHold, BaseWithHands, TopWithHands, SlowUpWithHoldWithoutHands, SlowUpWithSinkWithoutHands, FastUp`
- [x] `ClimbGap` 타입 변환
  - 원본: Block+Meta int 쌍 → 대상: `BlockState` 단일 객체 (null=미설정)

### 1-2. SmartStatistics 이동 데이터 구현

- [x] `totalVerticalDistance`, `currentVerticalSpeed` 등 SmartStatistics 이동 데이터 직접 구현
  - 원본은 SmartRender의 SmartStatistics에 의존 → 1.21.1에서 직접 구현 필요
  - animation_system.md 기반으로 전체 필드 목록 확인 후 구현 완료

### 1-3. moveFlying 비표준 공식 구현

- [x] `moveFlying(speed, strafe, upward, forward)` 독립 메서드 구현
  - 공식: `sqrt(sqrt(x²+z²) + y²)` — 표준 3D 유클리드 거리와 **다름**
  - 원본 코드: `MathHelper.sqrt_float(MathHelper.sqrt_float(x*x + z*z) + y*y)`
  - vanilla `applyMovementInput()` 사용 금지 (공식 불일치)
  - 수영/비행/잠수에서 공통 사용

### 1-4. Factor() 유틸 메서드

- [x] `Factor(x, x0, x1)` 선형 보간 유틸 구현
  - 공식: `(x - x0) / (x1 - x0)` — 0~1 클램프
  - 애니메이션 시스템 전반에서 사용

### 1-5. Button / 키입력 에지 감지

- [x] SM 전용 키바인딩 Fabric `KeyBinding` 등록
  - Grab 키 (`grabButton`): 헤드점프 차지, 클라이밍 제어
  - `WasPressed` (rising edge) / `Pressed` (홀드 상태) 직접 구현
  - [미확인 — SM이 사용하는 전체 커스텀 키 목록 Button.md 재확인 필요]

### 1-6. supportsCeilingClimbing 재설계

- [x] Block ID/Meta → BlockState + 설정 파싱 전면 재설계
  - `"tile.fenceIron"` → `Blocks.IRON_BARS` (`minecraft:iron_bars`)
  - `"tile.trapdoor"` → `TrapdoorBlock` (모든 목재/철 트랩도어, `OPEN=false` 조건)
  - 메타 정수 → `BlockState` 프로퍼티 (방향, 열림 여부 등)
  - 구현: `CeilingClimbBlocks.supports(BlockState)` (Phase 7)
  - 설정 파일 파싱 로직 재구현: TODO Phase 12

### 1-7. getOnLadderOrVine 로직

- [x] `LadderBlock.FACING` Direction 접근: `Direction.getOpposite()`
- [x] Vine 블록 방향 프로퍼티: `NORTH`, `SOUTH`, `EAST`, `WEST` boolean 접근
  - 구현: `SmartMovingClimber.getOnLadderOrVine()` (Phase 7)

---

## 2. 네트워크 레이어 — 패킷 직렬화 & 채널 등록

### 2-1. 채널 등록 순서 [서버 + 클라이언트]

- [x] **서버 채널 수신 핸들러 등록** (ModInitializer 또는 ServerLifecycleEvents.SERVER_STARTING)
  ```
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "state"), handler)
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "config_info"), handler)
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "config_change"), handler)
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "speed_change"), handler)
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "hunger_change"), handler)
  ServerPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "sound"), handler)
  ```
  - 총 6개 (Config Content는 서버→클라이언트 방향이므로 서버 수신 불필요)

- [x] **클라이언트 채널 수신 핸들러 등록** (ClientModInitializer)
  ```
  ClientPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "config_content"), handler)
  ClientPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "config_change"), handler)
  ClientPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "speed_change"), handler)
  ClientPlayNetworking.registerGlobalReceiver(
      new Identifier("smartmoving", "state"), handler)  // 다른 플레이어 상태 수신
  ```

- [x] **LocalUserNameProvider 주입 시점**: `ServerLifecycleEvents.SERVER_STARTING`에서 설정
  - `MinecraftClient.getInstance().getSession().getUsername()` 으로 대체

### 2-2. 패킷 직렬화 — ObjectOutputStream → PacketByteBuf [클라이언트 + 서버]

- [x] State 패킷 (ID=0): `int(entityId) + long(state)` → `buf.writeInt() + buf.writeLong()`
- [x] ConfigInfo 패킷 (ID=1): `Object(String)` → `buf.writeString(str, 32767)`
- [x] ConfigContent 패킷 (ID=2): `Object(String[]) + Object(String)` → `buf.writeByte(len) + loop buf.writeString() + buf.writeString()`
- [x] ConfigChange 패킷 (ID=3): 페이로드 없음 (1바이트 ID만)
- [x] SpeedChange 패킷 (ID=4): `int + Object(String)` → `buf.writeInt() + buf.writeString()`
- [x] HungerChange 패킷 (ID=5): `float` → `buf.writeFloat()`
- [x] Sound 패킷 (ID=6): `Object(String) + float + float` → `buf.writeString() + buf.writeFloat() + buf.writeFloat()`
  - ⚠️ 원본 IPacketReceiver 파라미터명은 `distance`이나 실제는 `volume` 직렬화됨 — 포팅 시 `volume`으로 통일

### 2-3. State 패킷 비트 레이아웃 [클라이언트→서버]

- [x] 34비트 long 비트맵 구현
  - bit 0: `isClimbing`
  - bit 1: `isHandsVineClimbing`
  - bit 2: `isFeetVineClimbing`
  - bit 3: `isClimbJumping`
  - bit 4: `isClimbBackJumping`
  - bit 5: `isWallJumping`
  - bit 6: `isCeilingClimbing`
  - bits 7-10: `actualHandsClimbType` (4비트, HandsClimbing enum ordinal)
  - bits 11-14: `actualFeetClimbType` (4비트, FeetClimbing enum ordinal)
  - bit 12: `isCrawlClimbing` ← network_sync 분석 기준 (bits 11-14와 겹침 — [미확인 — 정확한 비트 배치 SmartMovingOther.md 재확인 필요])
  - bit 13: `isCrawling`
  - bit 14: `isClimbing` (bit 0과 중복? — [미확인])
  - bit 15: `isSmall`
  - bit 18: `isCeilingClimbing` (bit 6과 중복? — [미확인])
  - bit 22: `isSliding`
  - bit 30: `isCrawling` (bit 13과 중복? — [미확인])
  - bit 31: `isWallJumping`
  - bit 33: `isSneakButtonPressed`
  - 점프 관련 추가 비트 (jump.md 기준):
    - `isFast/isSprintJump`, `isClimbBackJumping`, `isClimbJumping` (1비트씩)
    - `angleJumpType` (3비트, 0~7)
    - `isHeadJumping`, `doFlyingAnimation()`, `doFallingAnimation()`, `jumping` (1비트씩)

### 2-4. 서버 수신 후 릴레이 [서버]

- [x] `SmartMovingServer.processStatePacket()` 에서 State 패킷을 추적 플레이어에게 릴레이
  - `PlayerLookup.tracking(serverPlayer)` 반환 컬렉션 순회
  - 각 플레이어에게 `ServerPlayNetworking.send(player, packet)` 전송
  - [미확인 — `PlayerLookup.tracking()` 정확한 API 서명 확인 필요]

---

## 3. 서버 측 SM 로직 — SmartMovingServer

### 3-1. SmartMovingServer @Unique 필드 [서버]

다음 필드를 `ServerPlayerEntity`에 주입하거나 별도 컴포넌트 클래스로 관리:

- [x] `resetFallDistance: boolean` — afterOnUpdate에서 낙하 거리 리셋 여부
- [x] `resetTicksForFloatKick: boolean` — afterOnUpdate에서 floatKick 틱 리셋 여부
- [x] `initialized: boolean` — initialize() 완료 여부
- [x] `withinOnLivingUpdate: boolean` — onLivingUpdate 진행 중 여부
- [x] `crawlingCooldown: int` — 크롤링 종료 후 쿨다운 (10틱)
- [x] `isCrawling: boolean` — 서버 측 크롤링 상태
- [x] `isSmall: boolean` — 서버 측 작은 크기 상태
- [x] `hunger: float` — 클라이언트 소진값 (-1=억제 해제, 0=소진 없음)
- [x] `disableAddExhaustionDepth: int` — 소진 억제 중첩 깊이
- [x] `disableAddExhaustion: boolean` — 소진 추가 비활성화 여부
- [x] `isSneakButtonPressed: boolean` — 클라이언트 sneaking 버튼 상태
- [x] `forceIsSneaking: Boolean` — isSneaking() 강제 오버라이드 (null=비강제)
  - Map<UUID, SmartMovingServer> 방식으로 구현 (@Unique 필드 주입 대신)

### 3-2. [위험] floatingTicks 리셋 Mixin [서버] ★★★★☆

- [x] **Mixin 대상**: `ServerPlayNetworkHandler`
- [x] **@Shadow**: `floatingTicks` (Yarn: `field_14138`, 타입 `int`) — @Accessor 대신 @Shadow 직접 접근
- [x] **리셋 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing` (벽점프 제외)
- [x] **리셋 타이밍**: `ServerPlayNetworkHandler.tick()` HEAD Inject
- [x] **임계값 확인**: 80틱 초과 시 kick (vanilla 1.21.1 확인 완료)

### 3-3. [위험] 소진 인터셉트 구조 [서버] ★★★★☆

- [x] **Mixin 대상**: `ServerPlayerEntity` → `addExhaustion(float)`
  - **at**: `@At("HEAD")`, `cancellable = true`
  - `disableAddExhaustion == true` 시 `ci.cancel()` 호출
- [x] **Mixin 대상**: `ServerPlayerEntity.addMovementStat(double, double, double)` (또는 `LivingEntity`)
  - stub 작성 완료 (MixinServerPlayerEntity.java 주석)
  - [미확인 — 1.21.1 Yarn 메서드명 확인 필요, TODO Phase 13]
- [x] **의도적 역전 처리**: `beforeUpdatePotionEffects()` → `afterAddMovingHungerBatch()`, `afterUpdatePotionEffects()` → `beforeAddMovingHungerBatch()`
  - stub 작성 완료 (MixinLivingEntity.java 주석)
  - [미확인 — 1.21.1 포션 업데이트 메서드명 Yarn 확인 필요, TODO Phase 13]

### 3-4. isSneaking() 오버라이드 [서버]

- [x] **Mixin 대상**: `ServerPlayerEntity` → `isSneaking()`
  - **at**: `@At("HEAD")`, `cancellable = true`
  - `forceIsSneaking != null` 시 `cir.setReturnValue(forceIsSneaking)` 호출

### 3-5. isEntityInsideOpaqueBlock() 크롤링 쿨다운 억제 [서버]

- [x] **Mixin 대상**: `ServerPlayerEntity` → `isInsideWall()` (Yarn 1.21.1 확인 완료)
  - **at**: `@At("HEAD")`, `cancellable = true`
  - `crawlingCooldown > 0` 시 `cir.setReturnValue(false)` 호출

### 3-6. setSmall() 크기 변경 [서버]

- [x] `isSmall == true`: `EntityDimensions.changing(0.6F, 0.8F)` 반환
  - `MixinPlayerEntity.sm_getBaseDimensions_server()` 에 isSmall+STANDING 분기 추가
  - `SmartMovingServer.setSmall()` 헬퍼 추가 — `calculateDimensions()` 호출로 갱신+동기화
  - 구현: Phase 11

### 3-7. SmartMovingServer.initialize() — 접속 시 설정 전송 [서버]

- [x] `ServerPlayConnectionEvents.JOIN` 등록 (stub — 설정 전송 로직은 Phase 7에서 구현)
- [x] `ServerPlayConnectionEvents.DISCONNECT` 등록 — `SmartMovingServer.remove(player)` 호출
- [x] 플레이어 접속 시 설정 내용 전송 (Phase 7에서 구현)
  - `SmartMovingServer.initialize(player, server)` → `ConfigContentPayload(new String[0], null)` 전송
  - TODO Phase 12: SmartMovingConfig 기반 실제 설정 배열 전송

### 3-8. processConfigChangePacket 권한 확인 [서버]

- [x] `localUserName == username` 비교: 원본 `==` → `equals()`로 교체 완료 (SmartMovingServer.hasPermission())

### 3-9. resetFallDistance() [서버]

- [x] `player.fallDistance = 0`
- [x] `player.setVelocity(vel.x, 0.08, vel.z)` (원본: `motionY = 0.08` — 중력 상쇄)
- [x] 조건: `isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping`

---

## 4. 클라이언트 측 SM 로직 — SmartMovingClient

### 4-1. SmartMovingClient @Unique 필드 [클라이언트]

- [x] `jumpPending: boolean` — 다음 처리 틱에 점프 실행
- [x] `jumpAvoided: boolean` — vanilla jump() 회피 여부
- [x] `jumpCharge: float` — 차지 점프 누적 (0.0~Config.MaxJumpCharge)
- [x] `headJumpCharge: float` — 헤드점프 차지 누적
- [x] `blockJumpTillButtonRelease: boolean` — 버튼 릴리즈까지 점프 차단
- [x] `isSprintJump: boolean` — 스프린트 점프 상태
- [x] `isHeadJumping: boolean` — 헤드점프 상태
- [x] `isWallJumping: boolean` — 벽점프 상태
- [x] `angleJumpType: int` — 방향 점프 타입 (0~7)
- [x] `continueWallJumping: boolean` — 벽점프 연속 여부
- [x] `heightOffset: float` — 히트박스 오프셋 (헤드점프 시 -1F)
- [x] `isCrawling: boolean` — 크롤링 상태
- [x] `isSliding: boolean` — 슬라이딩 상태
- [x] 기타 이동 상태 플래그 (`isClimbing`, `isCrawlClimbing`, `isCeilingClimbing`, `isSmall`)
  - Map<UUID, SmartMovingClientState> 방식으로 구현 (SmartMovingClientState.java)

### 4-2. tickEssential() 무조건 호출 [클라이언트]

- [x] **Mixin 대상**: `ClientPlayerEntity.tickMovement()`
  - **at**: `@At("HEAD")`
  - SM isActive 여부 무관하게 `sm.tickEssential()` 항상 호출
  - `tickEssential()` 내부는 TODO Phase 5 (상태 패킷 전송, 키 입력 처리)

### 4-3. isConnectedToRemoteServer() [클라이언트]

- [x] `MinecraftClient.getInstance().getServer() == null` 기준으로 구현
  - `getServer()`는 IntegratedServer 반환 — 싱글/LAN: non-null, 원격: null
  - 원본 3중 조건 → 1.21.1 단일 조건으로 단순화 (network_sync.md 6절 확인)
  - `SmartMovingClientState.isConnectedToRemoteServer()` 정적 메서드로 구현

### 4-4. processBlockCode — 채팅 코드 설정 [클라이언트]

- [ ] `ClientReceiveMessageEvents.CHAT` 또는 `GAME` 이벤트 훅
  - 채팅 텍스트 `"§0§1...§f§f"` 앞 4자·뒤 4자 마커 감지
  - 12개 기능 on/off 파싱
  - [미확인 — 1.21.1 Text 시스템에서 `§` 코드 접근 방법 확인 필요]
  - stub 메서드만 작성 (SmartMovingClient.processBlockCode), 실제 구현은 Phase 7/13

---

## 5. 이동/물리 Mixin

### 5-1. [위험] travel() Mixin — 이동 진입점 [클라이언트 + 서버] ★★★★☆

- [x] **Mixin 대상**: `LivingEntity.travel(Vec3d movementInput)` (method 확인 필요)
  - **at**: `@At("HEAD")` — SM 상태 시 vanilla 수영/용암 분기 skip을 위해
  - SM 상태 활성 시 `ci.cancel()` 후 SM 자체 파이프라인 실행:
    ```
    handleJumping → handleSwimming → handleLava →
    handleAlternativeFlying → handleLand → handleWallJumping →
    addMovementStat → handleExhaustion
    ```
  - **주의**: `@Overwrite` 사용 금지, 상태별 별도 Mixin 또는 cancellable `@Inject` 권장
  - 서버 파이프라인: `MixinLivingEntity.sm_travel()` — TODO Phase 8 완전 구현
  - 클라이언트 클라이밍: `MixinLivingEntityClient.sm_travel_client()` — 클라이밍 표면 감지 시 cancel + SM 물리 적용 (Phase 7 구현 완료)

### 5-2. [위험] jump() 인터셉트 [클라이언트] ★★★★☆

- [x] **Mixin 대상**: `LivingEntity.jump()` (method_6043) 또는 `PlayerEntity.jump()`
  - **at**: `@At("HEAD")`, `cancellable = true`
  - SM 활성 시 `ci.cancel()`
  - `jumpAvoided = true; jumpPending = true` 세팅
  - **차단 부작용**:
    - `addExhaustion()` 차단됨 → SM 자체 소진 시스템으로 대체
    - `Stats.JUMP` 통계 차단됨 → SM에서 별도 `player.increaseStat(Stats.JUMP, 1)` 호출
    - 스프린트 점프 `+0.2F` 차단됨 → SM `tryJump()` 내에서 자체 계산
  - 구현: `MixinLivingEntityClient.sm_jump()` — Phase 10 tryJump() 구현 전까지 점프 비활성화

### 5-3. [위험] jumpingCooldown 우회 [클라이언트] ★★★★☆

- [x] vanilla `PlayerEntity`에서 `jumpingCooldown` 10틱 쿨다운이 SM 다중 점프 차단 가능
  - `jump()` 인터셉트로 vanilla 쿨다운 발동 자체를 차단
  - SM이 자체 `blockJumpTillButtonRelease` 플래그로 대체 관리
  - 5-2 구현(jump() 취소)으로 자동 달성 — 별도 Mixin 불필요

### 5-4. jumping 필드 제어 [클라이언트]

- [x] **Mixin 대상**: `LivingEntity.tickMovement()` HEAD, @Shadow jumping 사용
  - 억제 조건: `isCrawling || isSliding || isHeadJumping || jumpCharge>0 || blockJumpTillButtonRelease`
  - 구현: `MixinLivingEntityClient.sm_jumpingFilter()` (Phase 10)

### 5-5. [위험] isClimbing() 오버라이드 [클라이언트 + 서버] ★★★★☆

- [x] **Mixin 대상**: `LivingEntity.isClimbing()` (또는 `PlayerEntity`)
  - **at**: `@At("HEAD")`, `cancellable = true`
  - SM 커스텀 클라이밍 활성 시 `ci.cancel()` + `false` 반환
  - **목적**: `applyClimbingSpeed()` x/z ±0.15F 클램프 간섭 방지
  - **목적**: `applyMovementInput()` y=0.2 강제 간섭 방지
  - 서버: `MixinLivingEntity.sm_isClimbing_server()` — SmartMovingServer 상태 기반
  - 클라이언트: `MixinLivingEntityClient.sm_isClimbing_client()` — SmartMovingClientState 상태 기반

### 5-6. applyClimbingSpeed() 간섭 우회 [클라이언트]

- [x] **Mixin 대상**: `LivingEntity.applyClimbingSpeed(Vec3d)` (Yarn: applyClimbingSpeed, intermediary: method_18801 확인 완료)
  - **at**: `@At("HEAD")`, `cancellable = true`
  - SM 커스텀 클라이밍 상태 시 velocity 그대로 반환
  - 구현: `MixinLivingEntityClient.sm_applyClimbingSpeed()` — 5-5(isClimbing=false)의 이중 방어

### 5-7. velocityDirty 세팅 [클라이언트]

- [x] SM이 직접 Y 속도를 변경할 때마다 `player.velocityDirty = true` 수동 세팅
  - 서버 속도 동기화를 위해 필수
  - 구현: `MixinEntity.velocityDirty` @Shadow 필드 노출
  - 참고: Entity.setVelocity()는 내부에서 velocityDirty=true 자동 설정. setVelocity() 사용 시 수동 설정 불필요.

### 5-8. beforeMoveEntity / afterMoveEntity 처리 [클라이언트]

- [x] **Mixin 대상**: `Entity.move()` 전후
  - **at HEAD**: `ySize`(계단 오르기 높이) 대응 → `STEP_HEIGHT` 속성을 0으로 설정 (스니킹 등 조건 시)
  - **at TAIL**: `heightOffset > 0` 시 `player.setPos()` 로 위치 수동 조정
  - **at TAIL**: 클라이밍 이동 거리 누적 (피로도 계산용: 지상 1.2 / 공중 0.9)
  - **at TAIL**: 수영 소리 누적 (`SwimSoundDistance > 1.0D` 시 재생)
  - 구현: `MixinEntity.sm_beforeMove()` + `sm_afterMove()` (Phase 7 서버 측 구현 완료)
    - beforeMove: ServerPlayerEntity 크롤링/천장 클라이밍 시 STEP_HEIGHT=0 억제
    - afterMove: STEP_HEIGHT 복원 + 클라이밍 이동 거리 누적 (SmartMovingServer.distanceClimbedModified)
    - 클라이언트 측 STEP_HEIGHT 억제: 구현 완료 — `MixinEntityClient` (Phase 8)

### 5-9. 벽점프 — calculateSeparateCollisions() [클라이언트]

- [x] 반사 각도 공식 + 90° 반올림 + horizontalCollision=false + fallDistance=0 구현 완료
  - 구현: `SmartMovingJumper.handleWallJumping()` (Phase 10)
  - [미확인 — horizontalCollisionAngle 정확한 계산 (Orientation 이식) 미완료]

### 5-10. 속도 컷오프 0.003 대응 [클라이언트]

- [x] SM 클라이밍 최소 속도 `ClimbDownMotion=0.01D` > 0.003 → 직접 피해 없음 (확인 완료)
- [ ] `setOnlyShouldClimbSpeed()` 결과가 0.003 이하로 떨어지는 극단 케이스 테스트 필요 (통합 테스트 시 확인)

### 5-11. reverseHandleMaterialAcceleration() [클라이언트]

- [ ] 물 흐름 상쇄 로직 재구현
  - `FluidState.getVelocity(BlockView, BlockPos)` 로 흐름 벡터 취득
  - 역방향 0.014D 크기 적용: `addVelocity(-flowVec.x * 0.014, -flowVec.y * 0.014, -flowVec.z * 0.014)`
  - [미확인 — 1.21.1에서 흐름 가속 크기 0.014D 동일 여부 확인 필요]
  - 구현 위치: travel() SM 파이프라인 내부 — 별도 Mixin 불필요, Phase 7/8 구현

---

## 6. 포즈/치수 시스템 Mixin

### 6-1. [위험] getBaseDimensions() Mixin [클라이언트 + 서버] ★★★★☆

- [x] **Mixin 대상**: `PlayerEntity.getBaseDimensions(EntityPose)` (method_55694)
  - **at**: `@At("HEAD")`, `cancellable = true`
  - 서버: `MixinPlayerEntity.sm_getBaseDimensions_server()` — `isCrawling && SWIMMING` → `0.6W × 1.0H, eyeHeight 0.4F`
  - 클라이언트: `MixinPlayerEntityClient.sm_getBaseDimensions_client()` — 동일 로직
  - `LivingEntity.getDimensions(EntityPose)` 는 **final** → `getBaseDimensions()` 우회가 유일한 진입점
  - `MixinPlayerEntity` → `smartmoving.mixins.json`, `MixinPlayerEntityClient` → `smartmoving.client.mixins.json` 등록 완료
  - SM 헤드점프 상태: 헤드점프용 `EntityDimensions` 반환 — TODO Phase 10

### 6-2. [위험] isInSwimmingPose() 오버라이드 [클라이언트 + 서버] ★★★★☆

- [x] **Mixin 대상**: `LivingEntity.isInSwimmingPose()` (method_20232)
  - **at**: `@At("HEAD")`, `cancellable = true`
  - 서버: `MixinLivingEntity.sm_isInSwimmingPose_server()` — `isCrawling || isCrawlClimbing` → `false`
  - 클라이언트: `MixinLivingEntityClient.sm_isInSwimmingPose_client()` — 동일 로직
  - **목적**: `setupTransforms()` Branch 2 (몸 90° 눕힘 + `translate(0, -1, 0.3)`) 자동 발동 방지
  - **목적**: `updateLeaningPitch()` 자동 1.0 상승 차단 → Phase 12 setAngles Mixin에서 직접 제어
  - ⚠️ SWIMMING 포즈를 크롤링에 쓰되 `isInSwimmingPose()` 만 false 반환하는 방식 확정

### 6-3. EntityPose 전략 결정 [클라이언트 + 서버]

- [x] SM 크롤링: `EntityPose.SWIMMING` 사용 + `isInSwimmingPose()` 억제 — 확정
- [ ] SM 헤드점프: `EntityPose.SWIMMING` 또는 SM 전용 포즈 (SM 전용 enum 등록 방법 확인 필요)
  - [미확인 — 1.21.1에서 커스텀 EntityPose 추가 방법 확인 필요]
- [ ] SM 슬라이딩: 적절한 포즈 결정 필요 — [미확인 — 추가 리서치 필요]

### 6-4. updateLeaningPitch 간섭 대응 [클라이언트]

- [ ] SWIMMING 포즈 사용 시 `leaningPitch += 0.09F/틱` 자동 증가 (11틱에 1.0 도달)
  - 6-2에서 `isInSwimmingPose()=false` → `updateLeaningPitch()` 감소 경로 진입 → leaningPitch 0으로 수렴
  - SM 수영/잠수에서: leaningPitch 자동 회전이 SM 자체 각도와 충돌 → **억제 필요**
  - 억제 방법: `model.leaningPitch = 0` 강제 세팅 (Phase 12 setAngles Mixin에서)

### 6-5. recalculateDimensions 없음 처리 [클라이언트]

- [ ] `PlayerEntity`에서 `recalculateDimensions()`가 **자동 호출되지 않음**
  - 헤드점프 착지 시 포즈 전환 + 공간 체크를 SM이 직접 수행 — Phase 10에서 구현
  - 1블록 공간에서 스탠딩으로 전환 시도 시: 위 블록 블록 체크 → 불가능하면 SWIMMING 포즈 유지

---

## 7. 클라이밍 시스템 구현

> **참고**: 1-6(supportsCeilingClimbing 재설계)과 1-7(getOnLadderOrVine 로직)은 이 단계에서 구현한다.

### 7-1. 클라이밍 속도 상수 정의

- [x] `FastUpMotion = 0.2D`
- [x] `CatchCrawlGapMotion = 0.17D`
- [x] `MediumUpMotion = 0.14D`
- [x] `SlowUpMotion = 0.1D`
- [x] `HoldMotion = 0.08D`
- [x] `SinkDownMotion = 0.05D`
- [x] `ClimbDownMotion = 0.01D`
  - 구현: `SmartMovingClimber` 상수 (Phase 7)

### 7-2. handleClimbing() Free 모드 [클라이언트]

- [x] 4방향 탐색 로직 구현 (대각 4방향은 TODO)
- [x] exhaustion 체크 (TODO stub — SM exhaustion 미구현)
- [x] 속도 보간 (`setOnlyShouldClimbSpeed()`)
  - `relevant = value < 0 || value > motionY` 조건 구현
  - 상방: `motionY = (value - HoldMotion) × upFactor × combinedFactor + HoldMotion`
  - 하방: `motionY = HoldMotion - (HoldMotion - value) × downFactor × combinedFactor`
  - upFactor/downFactor: 1.0D 임시 (미확인 — TODO Phase 12)
  - 구현: `SmartMovingClimber.handleClimbing()` (Phase 7)

### 7-3. handleCeilingClimbing() 속도 [클라이언트]

- [x] `jgap > 1.2` → 수평 속도 `0.12`
- [x] `jgap > 1.115` → 수평 속도 `0.08`
- [x] else → 수평 속도 `0.04`
- [x] `fallDistance = 0` 강제 초기화
  - jgap 계산: 발 Y부터 위 블록까지 AABB 충돌 쿼리 구현 완료 (Phase 9)
  - 구현: `SmartMovingClimber.handleCeilingClimbing()` (Phase 7) + `computeJgap()` (Phase 9)

### 7-4. [위험] floatingTicks 리셋 연동 [서버] ★★★★☆

- [x] 3-2 항목 (MixinServerPlayNetworkHandler floatingTicks 리셋)에서 이미 구현 완료
  - 조건: `resetTicksForFloatKick == true` (isClimbing || isCrawlClimbing || isCeilingClimbing)
  - 벽점프 제외 — 벽점프는 순간적이므로 kick 위험 없음

---

## 8. 수영/잠수 시스템 구현

### 8-1. 수영 상태 3분류 [클라이언트]

- [x] `getFluidHeight(FluidTags.WATER) + 0.1625D` = offset 기반 직접 구현
  - `isDipping`: offset < 1.4
  - `isSwimming_sm`: 1.4 ≤ offset < 1.9
  - `isDiving`: offset ≥ 1.9
  - 구현: `SmartMovingSwimmer.updateSwimState()` (Phase 8)
  - `waterMovementTicks`: 물속 틱마다 ++, 물 밖이면 0 리셋

### 8-2. handleSwimming() — travel() Mixin 내부 [클라이언트]

- [x] SM 수영 상태 시 vanilla `travel()` 수영 분기 skip (`ci.cancel()` 적용)
- [x] 감쇠 재현 (swim_dive.md 기준):
  - dipping: x/z=0.80D, y=0.83D
  - swimming: 0.85D
  - diving: 0.83D
  - [미확인 — speed_physics.md와 값 불일치. swim_dive.md 기준 선택]
- [x] 물 탈출 점프 ([8-5] 통합): `motionY = 0.30000001192092896D`
- [x] 잠수 상승 (`diveUp`): `jumping` 필드 기반 (MixinLivingEntityClient @Shadow 경유)
- [x] 수영 속도: `SmartMovingSwimmer.moveFlying()` — SM 비표준 sqrt(sqrt) 공식 유지
- [x] 구현: `SmartMovingSwimmer.handleSwimming()` (Phase 8)

### 8-3. SWIMMING 포즈 leaningPitch 충돌 대응 [클라이언트]

- [ ] SM 수영 상태에서 `model.leaningPitch = 0` 강제 (setAngles Mixin에서)
  - leaningPitch 억제로 vanilla -90° 자동 회전 차단
  - SM setAngles에서 자체 45° 기울기 애니메이션 구현
  - → **Phase 12 (렌더/애니메이션) 로 이관**

### 8-4. heightOffset(-1F) SWIMMING 포즈 이중 적용 방지 [클라이언트]

- [ ] SWIMMING 포즈 히트박스 0.6H + SM heightOffset(-1F) 이중 적용 방지 설계
  - `getPositionOffset()`에서 SWIMMING 포즈 오프셋 vs SM 헤드점프 오프셋 누적 계산
  - → **Phase 12 (렌더/히트박스) 로 이관**

### 8-5. isJumpingOutOfWater [클라이언트]

- [x] `player.horizontalCollision` + `waterMovementTicks > 10` + `diveUp` → `motionY = 0.30000001192092896D`
  - 구현: `SmartMovingSwimmer.handleSwimming()` 내부 (Phase 8)

### 8-6. 수영 소리 [클라이언트]

- [x] `distanceSwom` 누적 → `SwimSoundDistance(1/0.7F)` 초과 시 `ENTITY_PLAYER_SWIM` 재생
  - 구현: `MixinEntityClient.sm_afterMove_client()` (Phase 8)

---

## 9. 크롤링/슬라이딩 시스템 구현

### 9-1. DataTracker 동기화 [클라이언트 + 서버]

- [x] `isSliding` 서버→클라이언트 동기화: State 패킷 bit 22로 전달, `processStatePacket()`에서 파싱 완료 (Phase 9)
- [ ] `isCrawling` DataTracker 엔트리 등록 (또는 커스텀 패킷 동기화) — TODO Phase 13
- [ ] `isSliding` DataTracker 엔트리 (별도 DataTracker 등록 필요 여부 확인)

### 9-2. [위험] SWIMMING 포즈 + isInSwimmingPose() 억제 [클라이언트] ★★★★☆

→ 6-2 항목 참조. 크롤링 시 반드시 `isInSwimmingPose() = false` 보장.

### 9-3. LimbAnimator 억제 [클라이언트]

- [x] **Mixin 대상**: `LivingEntity.updateLimbs(Z)V` HEAD, `cancellable = true`
  - `isCrawling || isSliding` 시 `ci.cancel()` — 보행 LimbAnimator 갱신 전체 억제
  - 구현: `MixinLivingEntityClient.sm_updateLimbs_client()` (Phase 9)

### 9-4. 슬라이딩 감쇠 공식 [클라이언트]

- [x] `1 / ((1/slip - 1) / 25 * slideSlipperinessFactor + 1) * 0.98F`
  - `slip` = `below.getBlock().getSlipperiness()` (1.21.1 public 확인 완료)
  - 구현: `SmartMovingSlider.handleSliding()` (Phase 9)

### 9-5. 슬라이딩 파티클 [클라이언트]

- [x] `BlockStateParticleEffect(ParticleTypes.BLOCK, blockState)` 생성
- [x] `world.getBlockState(player.getBlockPos().down())` 으로 아래 블록 상태 취득
- [x] `world.addParticle()` 으로 파티클 추가 (motionX/Z = dir × -4D, motionY = 1.5D)
  - 구현: `SmartMovingSlider.spawnSlidingParticle()` (Phase 9)

### 9-6. getPositionOffset() 크롤링 오프셋 [클라이언트]

- [x] **Mixin 대상**: `PlayerEntityRenderer.getPositionOffset()` HEAD, `cancellable = true`
  - 크롤링 시 `Vec3d(0, -entity.getScale() × 0.125D, 0)` 반환 (SWIMMING 포즈 오프셋 대체)
  - 구현: `MixinPlayerEntityRenderer.sm_getPositionOffset()` (Phase 9)

---

## 10. 점프 시스템 구현

### 10-1. handleJumping() — 점프 판정 진입점 [클라이언트]

- [x] `sm_travel_client()` HEAD(수영 체크 전)에서 매 틱 호출
  - 차지 점프: Sneak 홀드 → `jumpCharge` 누적 → 릴리즈 시 `tryJump(CHARGE_UP, ...)`
  - 헤드점프 차지: Grab 홀드 → `headJumpCharge` 누적 → 릴리즈 시 `tryJump(HEAD_UP, ...)`
  - 수면 점프: `isDipping && jumpPending && frac > 0.6` → `tryJump(UP, ...)` [미확인 — isSlow 생략]
  - 일반 점프: `jumpPending && !isClimbing && !isCrawlClimbing` → `tryJump(UP, ...)`
  - 방향 점프: `angleJumpType = ((360 - movementAngle) / 45) % 8`
  - 구현: `SmartMovingJumper.handleJumping()` (Phase 10)

### 10-2. tryJump() — 실제 속도 계산 [클라이언트]

- [x] 일반 Up 점프 수직 속도: `0.41999998688697815D + potionJump * 0.1F`
  - `potionJump` = `JUMP_BOOST` 포션 `getAmplifier()` (없으면 0)
- [x] 기타 타입: `-0.078 + 0.498 * 1.0D * jumpChargeFactor` [미확인 — jumpChargeFactor 보간 세부값]
- [x] 스프린트 점프: `motionX -= sin(yaw)*0.2F; motionZ += cos(yaw)*0.2F`
- [x] 헤드점프 각도 재계산: `normalAngle = atan(vMotion/hSpeed); newAngle = headJumpControlFactor * normalAngle`
- [x] 속도 적용: `player.setVelocity()` (내부에서 velocityDirty 자동 세팅)
- [x] `isHeadJumping → setPoseSmall()` (SWIMMING 포즈 전환 + calculateDimensions)
  - 통계 기록: [미확인 — Stats.JUMP Yarn명 TODO]
  - 구현: `SmartMovingJumper.tryJump()` (Phase 10)

### 10-3. getJumpMoving() — 각도 점프 수평 속도 [클라이언트]

- [x] 원본 공식 그대로 이식:
  - `reset=false`: `actual + move * horizontal`
  - `reset=true, 반대방향`: `move * horizontalJumpFactor`
  - `reset=true, 같은방향`: `max(|actual|, |move| * horizontal) * signum(move)`
  - 구현: `SmartMovingJumper.getJumpMoving()` (Phase 10)

### 10-4. [위험] heightOffset — 헤드점프 히트박스 [클라이언트] ★★★★☆

- [x] `setPoseSmall()`: SWIMMING 포즈 전환 + `calculateDimensions()`
- [x] `getBaseDimensions()` Mixin: `isHeadJumping && SWIMMING` → `EntityDimensions.changing(0.6F, 1.5F).withEyeHeight(0.4F)` [미확인 — 정확한 치수]
- [x] `resetHeightOffset()` (착지 시): 위 블록 공간 체크 → STANDING 포즈 복원. 공간 부족 시 SWIMMING 유지
  - 구현: `SmartMovingJumper.setPoseSmall/resetHeightOffset()`, `MixinPlayerEntityClient` 수정 (Phase 10)

### 10-5. handleWallJumping() [클라이언트]

- [x] `player.horizontalCollision` 감지
- [x] 반사 각도 공식: `reflectedAngle = hCollAngle*2 - moveAngle + 180`; `jumpAngle = round(reflectedAngle/90)*90`
- [x] `player.setYaw(jumpAngle)` + `player.bodyYaw = jumpAngle`
- [x] `player.horizontalCollision = false` + `player.fallDistance = 0`
  - [미확인 — horizontalCollisionAngle: Orientation.java 이식 필요. 현재 이동 방향 반대 임시값 사용]
  - 구현: `SmartMovingJumper.handleWallJumping()` (Phase 10)

### 10-6. HUD — 차지 바 [클라이언트]

- [x] `HudRenderCallback.EVENT.register()` 등록
- [x] `jumpCharge / cfg.jumpChargeMaximum` 비율 → 초록 바 (0xFF00FF00), 너비 100px 기준
- [x] `headJumpCharge / cfg.headJumpChargeMaximum` 비율 → 파랑 바 (0xFF0000FF)
  - 구현: `SmartMovingHud.java` (Phase 10)

---

## 11. 속도/물리 시스템

### 11-1. 속도 팩터 4단계 구조 [클라이언트]

- [x] `getConfigSpeedFactor()`: `cfg.speedFactor × cfg.getUserSpeedFactor()`
- [x] `getPotionSpeedFactor()`: `GENERIC_MOVEMENT_SPEED × 10F / (isSprinting ? 1.3F : 1F)`
  - iceSpeedFactor: [미확인 — SmartMovingConfig 미구현, TODO Phase 13]
- [x] `getNonSlowInputSpeedFactor()`: slip>0.6 → 1.5F(임시) / isSprinting → sprintFactor / 기타 → 1.0F
- [x] `getSlowInputSpeedFactor()`: isUsingItem×0.2 / isCrawling×crawlFactor / isSneaking×sneakFactor / isCeilingClimbing×ceilingClimbingSpeedFactor
  - 구현: `SmartMovingMover.java` (Phase 11)

### 11-2. 중력 상수 확인

- [x] `GENERIC_GRAVITY` 속성으로 교체 완료
  - SmartMovingSlider.java: `0.08D` → `player.getAttributeValue(EntityAttributes.GENERIC_GRAVITY)`
  - SmartMovingServer.applyFallDistanceReset(): `0.08` → `player.getAttributeValue(EntityAttributes.GENERIC_GRAVITY)`
  - SmartMovingClimber.HOLD_MOTION = 0.08D는 클라이밍 속도 상수이므로 교체 제외

### 11-3. [위험] 서버 검증 통과 ★★★★★

- [x] **floating kick 방지**: 3-2 항목 (MixinServerPlayNetworkHandler floatingTicks 리셋) 구현 완료
- [x] **moved too quickly 방지**: SM 클라이밍 최대 속도 0.2D → distanceSq=0.04 < 임계값 100 — 안전 확인
- [x] **moved wrongly 방지**: stub 작성 완료 (MixinServerPlayNetworkHandler.java 주석)
  - [미확인 — onPlayerMove Yarn명 확인 + 구현 TODO Phase 13]

### 11-4. 비행 억제 [클라이언트]

- [x] `isFlying && !cfg.fly` 시 `motionY *= 0.5999755859375D`
  - jumpMovementFactor 제어: [미확인 — TODO Phase 12]
  - 구현: `MixinLivingEntityClient.sm_travel_client()` HEAD (Phase 11)

### 11-5. STEP_HEIGHT 속성 제어 [클라이언트]

- [x] `ySize = 0F` (1.7.10) → `GENERIC_STEP_HEIGHT = 0` 클라이언트 측
  - 조건: `isCrawling || isCrawlClimbing || isCeilingClimbing`
  - 구현: `MixinEntityClient.sm_beforeMove_client()` / `sm_afterMove_client()` (Phase 8에서 이미 완료)

---

## 12. 애니메이션/렌더링 Mixin

### 12-1. [위험] setAngles() Mixin — 11가지 이동 상태 [클라이언트] ★★★★☆

- [ ] **Mixin 대상**: `PlayerEntityModel.setAngles()` (method_17087)
  - **at**: `@At("TAIL")` — vanilla 공식 완료 후 SM이 덮어쓰기
  - SM 상태 활성 시 `model.leaningPitch = 0` 강제 (수영 팔 Step 13 차단)
  - **11가지 상태별 파트 각도 적용**:
    - `isRopeSliding`: [미확인 — 원본 애니메이션 로직 확인 필요]
    - `isClimbing` / `isCrawlClimbing`: [미확인 — 원본 animation_system.md 재확인 필요]
    - `isClimbJumping`: rightArm.pitch=Half+Sixteenth, leftArm.pitch=Half+Sixteenth, rightArm.roll=-Thirtytwoth, leftArm.roll=Thirtytwoth
    - `isCeilingClimbing`: [미확인]
    - `isSwimming`: SM 45° 기울기 자체 구현
    - `isDiving`: [미확인]
    - `isCrawling`: [미확인]
    - `isSliding`: [미확인]
    - `isFlying`: body(=outer).pitch=(Quarter-verticalAngle)*walkFactor, body.yaw=horizontalAngle, head.pitch=-body.pitch/2
    - `isHeadJumping`: body.pitch=Quarter-currentVerticalAngle, body.yaw=currentHorizontalAngle, head.pitch=-body.pitch/2; 팔/다리 Z 각도 + smallOverGroundHeight 클램프
    - `isFalling`: arm/leg cos 진동 애니메이션 (fallDistance 기반)
  - **상수 대응**: `Quarter=π/2`, `Half=π`, `Eighth=π/4`, `Sixteenth=π/8`, `Thirtytwoth=π/16`, `Sixtyfourth=π/32`
  - **sneaking 간섭 처리**: `model.sneaking = entity.isInSneakingPose()` 자동 세팅 → SM CROUCHING 미사용 시에도 발동 → 필요 시 Mixin으로 억제

### 12-2. animateAngleJumping() [클라이언트]

- [ ] `setAngles()` Mixin 내부, `isAngleJumping() = angleJumpType > 1 && angleJumpType < 7` 시 실행
  - `angle = angleJumpType * Eighth`
  - `backness = 1F - |angle - Half| / Quarter`
  - `leftness = -min(angle - Half, 0F) / Quarter`
  - `rightness = max(angle - Half, 0F) / Quarter`
  - body.yaw 재설정 + 다리/팔 방향 각도 조정

### 12-3. setupTransforms() Mixin — bodyYaw 제어 [클라이언트]

- [ ] **Mixin 대상**: `PlayerEntityRenderer.setupTransforms()`
  - **@ModifyArg(index=3)**: bodyYaw 파라미터를 SM forwardRotation으로 교체
  - 적용 상태: `isHeadJumping || isFlying || isSwimming || ...`

### 12-4. ModelRotationRenderer 대체 — MatrixStack [클라이언트]

- [ ] 6가지 회전 순서(XYZ/XZY/YXZ/YZX/ZXY/ZYX) MatrixStack 수동 조작 구현
  - vanilla ModelPart는 pitch(X)→yaw(Y)→roll(Z) 고정 → 순서 변경 불가 → MatrixStack 필수
- [ ] `scaleY` 런타임 적용: 파트 렌더 전후 `matrixStack.scale(1, scaleY, 1)`
- [ ] `offsetXYZ` 적용: `matrixStack.translate(offsetX, offsetY, offsetZ)`
- [ ] `fade` 보간: 부드러운 전환을 위한 자체 보간 로직
- [ ] `ignoreBase` / `ignoreSuperRotation` 플래그 처리

### 12-5. 중간 노드 부재 대응 [클라이언트]

- [ ] 원본 `bipedOuter → bipedTorso → bipedBreast → bipedNeck/Pelvic/Shoulder` 계층 없음
  - 1.21.1 모델 파트: `head, body, rightArm, leftArm, rightLeg, leftLeg` (BipedEntityModel)
  - SM "outer" 개념 → `body` 또는 root 파트로 대응
  - 중간 노드 기반 계층 변환을 개별 파트 직접 조작으로 재구현

### 12-6. getPositionOffset() Mixin — heightOffset [클라이언트]

- [ ] **Mixin 대상**: `PlayerEntityRenderer.getPositionOffset()` (method_23206)
  - **at**: `@At("TAIL")` 또는 `@ModifyReturnValue`
  - SM 헤드점프 상태 시 Y 오프셋 추가
  - ⚠️ SWIMMING 포즈 사용 시 `setupTransforms()` Branch 2에서 `translate(0, -1, 0.3)` 추가 적용됨 → 두 오프셋 누적 계산 필요

### 12-7. smallOverGroundHeight 계산 [클라이언트]

- [ ] `isCrawlClimbing || isHeadJumping` 시: `world.getBlockState(BlockPos)` 으로 위 5블록 범위 탐색
  - 블록까지의 거리 반환
  - 헤드점프 팔 Z 각도 클램프에 사용: `armFactorZ = min(armFactorZ, smallOverGroundHeight / 5F)`

---

## 13. 설정 시스템

### 13-1. SmartMovingConfig → 1.21.1 설정 이식

- [x] Config 파일 로드/저장 로직 재구현 (`Properties` 기반) — Phase 1 완료 (java.util.Properties 단순화)
- [ ] 서버 설정(`ServerConfig`) vs 클라이언트 설정(`Options`) 분리 유지
- [ ] `Config = ServerConfig` / `Config = Options` 전환 상태 관리

### 13-2. 설정 배포 프로토콜

- [ ] 전체 흐름 구현:
  ```
  플레이어 접속 → SmartMovingServer.initialize() → sendConfigContent()
  → 클라이언트 processConfigPacket() → ServerConfig.loadFromProperties()
  → Config = ServerConfig → sendConfigInfo()
  ```
- [ ] 재설정 분기: `first = (Config != ServerConfig)` 상태 추적

---

## 미확인 항목 목록

> 구현 전 반드시 추가 리서치 필요

- [ ] `animateArms` 내부 구현 세부 — animation_system.md에 [미확인]으로 기재
- [ ] `mp.onGround float` 동등값 — animation_system.md에 [미확인]으로 기재
- [ ] 수영 소리 vanilla 자동 재생 여부 — swim_dive.md에 [미확인]으로 기재
- [ ] `ModelPart xScale/yScale/zScale` 존재 여부 — swim_dive.md에 [미확인]으로 기재
- [ ] `reverseHandleMaterialAcceleration` 1.21.1 travel() 완전 대체 시 불필요 여부
- [ ] `getLeaningPitch()` 증가/감소 속도 (수치 미확인)
- [x] State 패킷 34비트 전체 비트 배치 — SmartMovingOther.md로 확인 완료 (bits 0-33)
- [ ] SM 전용 커스텀 EntityPose 등록 가능 여부
- [ ] `isEntityInsideOpaqueBlock()` 해당 1.21.1 Yarn 메서드명
- [ ] `GENERIC_GRAVITY` 속성 기본값 0.08D 여부
- [ ] 슬립 접근 1.21.1 Yarn 메서드 (`Block.getSlipperiness()` 또는 `AbstractBlock.Settings`)
- [ ] `jumpMotionX/Z` 저장 타이밍 (handleJumping 시작 시점 추정)
- [ ] `Config.getJumpExhaustionGain()` 내부 공식 세부값
- [ ] `leftJumpCount`, `rightJumpCount`, `backJumpCount` 더블클릭 카운터 임계값
- [ ] `wallJumpCount` 최대값 및 `continueWallJumping` 전환 조건
- [ ] `getPoses()` 반환값 사용 경로
- [ ] 1.21.1에서 흐름 가속 크기 0.014D 동일 여부
- [ ] `SmartMovingContext`의 `SwimCrawlWaterBorder`, `playerSwimWaterBorder`, `SwimSoundDistance` 정확한 상수값
- [ ] SM이 사용하는 전체 커스텀 키 목록 (Button.md)
- [ ] 서버 물리 재현 범위 설계 (moved wrongly 방지용)

---

## 전체 구현 순서 요약 + 이유

> 의존 관계를 최우선으로 순서를 결정. 의존 대상이 없으면 테스트가 불가능하므로 하위 레이어부터 구현.

### Phase 1 — 기반 (의존 없음)

1. **데이터 타입 변환**: `HandsClimbing`, `FeetClimbing` enum, `ClimbGap` BlockState 변환
   - 이유: 다른 모든 시스템이 이 타입에 의존
2. **공통 유틸**: `moveFlying()`, `Factor()`, `getJumpMoving()` — 순수 수학, 독립적
3. **키바인딩 등록**: Fabric `KeyBinding` 등록 — 입력 감지의 기반
4. **설정 시스템**: Config 파일 로드/저장 — 모든 속도/기능 활성화 판단 기반

### Phase 2 — 네트워크 레이어

5. **패킷 직렬화 레이어**: `PacketByteBuf` 기반 7종 패킷 포맷 재구현
   - 이유: 서버/클라이언트 통신 없이는 동기화 불가
6. **채널 등록**: 서버 6개 + 클라이언트 4개 핸들러 등록
7. **SmartMovingServer 이식**: @Unique 필드, initialize(), processStatePacket() 핵심 로직
   - 이유: 서버 측 SM 로직의 중심, 이후 모든 서버 Mixin이 이 클래스를 참조

### Phase 3 — 서버 안전 (kick 방지)

8. **[위험] floatingTicks Mixin**: `ServerPlayNetworkHandler.floatingTicks` @Accessor + 0 리셋
   - 이유: 클라이밍 구현 시작 즉시 kick 발생 — 첫 번째로 막아야 함
9. **resetFallDistance()**: 클라이밍/벽점프 중 낙하 거리 리셋
10. **소진 인터셉트 Mixin**: addExhaustion + addMovementStat + updatePotionEffects 훅
    - 이유: 서버 물리 루프에서 소진 계산이 매 틱 발동, 조기 구현 필요

### Phase 4 — 이동 물리 (메인 파이프라인)

11. **[위험] isClimbing() 오버라이드 Mixin**: vanilla 간섭 차단
    - 이유: travel() 구현 전에 반드시 간섭 차단이 먼저여야 함
12. **[위험] travel() Mixin**: SM 이동 파이프라인 진입점 구현
    - 이유: 모든 이동 상태(수영, 클라이밍, 점프 등)의 처리 순서가 여기서 결정됨
13. **[위험] jump() 인터셉트 Mixin**: vanilla jump 차단 + jumpPending 플래그
14. **handleClimbing() 시스템**: Free/Simple/Ceiling 클라이밍 속도 적용
15. **handleSwimming() 시스템**: 3상태 수영, moveFlying 공식 적용
16. **handleJumping() + tryJump()**: 점프 판정 + 속도 계산

### Phase 5 — 포즈/치수 시스템

17. **[위험] getBaseDimensions() Mixin**: SM 크롤링/헤드점프 EntityDimensions
    - 이유: 포즈 전환이 이동 물리에서 발생하므로 Phase 4 이후 구현
18. **[위험] isInSwimmingPose() 오버라이드**: 크롤링 중 false 반환
19. **EntityPose 전략 최종 결정**: 크롤링/헤드점프 포즈 매핑
20. **STEP_HEIGHT 속성 제어**: ySize 대체 구현

### Phase 6 — 렌더링/애니메이션

21. **[위험] setAngles() Mixin**: 11가지 상태별 파트 각도, leaningPitch 강제 0
    - 이유: 포즈 시스템이 확정된 후 렌더링 구현
22. **ModelRotationRenderer 대체**: MatrixStack 6축 회전 순서 구현
23. **setupTransforms() Mixin**: bodyYaw 제어
24. **getPositionOffset() Mixin**: heightOffset 렌더 적용
25. **HUD 차지 바**: HudRenderCallback 등록

### Phase 7 — 서버 릴레이 + 설정 배포

26. **sendPacketToTrackedPlayers 릴레이**: PlayerLookup.tracking() 기반 구현
27. **설정 배포 프로토콜**: initialize() → sendConfigContent() → processConfigPacket() 흐름
28. **processBlockCode**: 채팅 이벤트 훅

### Phase 8 — 통합 테스트

29. **floating kick 통합 테스트**: 클라이밍 80틱 이상 지속 → kick 없어야 함
30. **moved wrongly 통합 테스트**: 클라이밍/점프 이동 서버-클라이언트 위치 일치 확인
31. **SWIMMING 포즈 충돌 테스트**: 크롤링/잠수/헤드점프 렌더 상호 간섭 없음 확인
32. **leaningPitch 전환 속도 테스트**: 크롤링 진입/이탈 시 애니메이션 자연스러운지 확인

---

## 미완료 항목 모음

> 구현 중단된 항목들. 이유와 필요한 리서치 방향을 기록해둠.
> 재개 시: 리서치 파일 작성 → 체크리스트 항목 복귀 → 구현 순서로 진행.

---

### [Phase 3] 3-3-B: `addMovementStat` HEAD+TAIL Mixin

**내용**
`PlayerEntity.addMovementStat(dx, dy, dz)` 앞뒤에 `beforeAddMovingHungerBatch()` / `afterAddMovingHungerBatch()`를 Inject해서, SM이 직접 계산한 허기값을 적용하고 vanilla 소진은 차단하는 구조.
- HEAD: `disableAddExhaustion = true` 설정
- TAIL: `sm.hunger` 값으로 직접 addExhaustion 호출 후 `disableAddExhaustion = false` 해제

**미완료 이유**
1.21.1 Yarn 매핑에서 이 메서드의 정확한 이름을 확인하지 못했다. 원본(1.7.10 MCP)의 `addMovementStat`이 Yarn에서 같은 이름인지, 아니면 `increaseStat` / `addTravelExhaustion` 등 다른 이름으로 매핑됐는지 불확실하다. 잘못된 메서드명으로 Mixin을 걸면 런타임 `Invalid mixin target` 크래시가 발생한다.

**필요한 리서치**
- 1.21.1 Yarn 소스에서 `PlayerEntity` 내 이동 거리 통계 및 소진을 처리하는 메서드명 확인
- 참고: Yarn GitHub `net/minecraft/entity/player/PlayerEntity.java` 또는 Fabric loom decompile 결과

---

### [Phase 3] 3-3-C: `updatePotionEffects` HEAD+TAIL 역전 처리

**내용**
원본 SM의 의도적 역전 구조: `LivingEntity.updatePotionEffects()` (또는 동등 메서드)의
- HEAD: `afterAddMovingHungerBatch()` 호출 (`disableAddExhaustion = false`)
- TAIL: `beforeAddMovingHungerBatch()` 호출 (`disableAddExhaustion = true`)

포션 업데이트 사이클 타이밍과 허기 배치 처리를 맞추기 위한 구조로, 3-3-B(`addMovementStat` Mixin)와 쌍으로 동작한다.

**미완료 이유**
두 가지.
1. 3-3-B(`addMovementStat`)가 선행 구현되어야 이 항목이 의미 있다.
2. 1.21.1 Yarn에서 `LivingEntity`의 포션 업데이트 메서드명이 `tickStatusEffects`인지 `updatePotionEffects`인지 확인되지 않았다.

**필요한 리서치**
- 1.21.1 Yarn 소스에서 `LivingEntity` 내 포션/상태효과 틱 처리 메서드명 확인
- 참고: Yarn GitHub `net/minecraft/entity/LivingEntity.java` 또는 loom decompile

---

### [Phase 3] 3-6: `setSmall()` — 서버 플레이어 히트박스 동적 변경

**내용**
크롤링(`isSmall == true`) 시 서버 플레이어 히트박스를 높이 0.8F로 축소, 해제 시 1.8F로 복원.
원본은 `player.setHeight(float)` 한 줄이었으나 1.21.1에서 `EntityDimensions` 시스템으로 교체됐다.

**미완료 이유**
1.21.1에서 런타임 히트박스 변경 방법이 복잡하다. 단순 setter가 없고 아래 구조가 필요하다:
1. `EntityDimensions.fixed(width, height)` 생성
2. Mixin으로 `getBaseDimensions(EntityPose)` 오버라이드 — `isSmall` 상태일 때 축소 치수 반환
3. `player.calculateDimensions()` 호출 — 서버에서 히트박스 갱신 + 클라이언트에 동기화

이를 잘못 구현하면 서버-클라이언트 히트박스 desync가 발생한다. Phase 5(포즈/치수 시스템)에서 `getBaseDimensions` Mixin을 이미 다룰 예정이므로, 그 시점에 함께 구현하는 것이 안전하다.

**필요한 리서치**
- Phase 5 `getBaseDimensions()` / `calculateDimensions()` Mixin 리서치와 통합해서 진행
- 참고: `docs/research_포즈치수.md` (Phase 5 리서치 파일 작성 시 함께 다룰 것)
