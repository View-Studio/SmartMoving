# 플레이어 상태 관리 리서치 (SmartMovingSelf)

> 원본 소스: `net.smart.moving.SmartMovingSelf` (103KB)
> 관련 파일: `SmartMovingBase.java`, `SmartMoving.java`, `Button.java`, `SmartMovingPlayerBase.java`

---

## 관련 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `SmartMovingSelf` | 핵심 상태 머신. 모든 이동 플래그 보유 + 매 틱 업데이트 |
| `SmartMovingPlayerBase` | PlayerAPI 훅. vanilla EntityPlayer 메서드 16개 오버라이드 |
| `SmartMovingBase` | 물리 유틸. 충돌, 클라이밍, 액체 감지 |
| `SmartMoving` | 상태 추적 + 파티클 |
| `Button` | 키 입력 래퍼 (Pressed 상태 관리) |

---

## 동작 원리 (전체 흐름)

```
매 틱 (updateEntityActionState 호출)
  ↓
1. 이전 상태 저장
  ↓
2. 키 입력 폴링 (Button.update)
  ↓
3. 비활성화 조건 체크 (탑승/수면/크리에이티브)
  ↓
4. 크롤 평가 → isCrawling
  ↓
5. 클라이밍 적합성 → wantClimbUp/Down
  ↓
6. 수직 이동 우선순위 결정
   isClimbing > isSwimming > isDiving > isStanding > isCrawling
  ↓
7. Exhaustion 제약 적용
  ↓
8. 점프 타입 감지 (차지/헤드/각도/벽)
  ↓
9. 수중 상태 평가
  ↓
10. 상태 확정
  ↓
11. 변경 있으면 64비트 패킷 인코딩 후 전송
```

---

## A. 전체 상태 플래그 목록

### Boolean — 이동 출력 상태 (렌더링/물리에 직접 영향)

| 플래그 | 설명 | 진입 조건 | 해제 조건 | 충돌 관계 |
|--------|------|-----------|-----------|-----------|
| `isCrawling` | 기어가기 | `canCrawl && (wantCrawl \|\| mustCrawl)` | 수영/클라이밍 진입, 천장 공간 확보 | isSwimming, isClimbing과 상호 배타 |
| `isClimbing` | 사다리/덩굴 클라이밍 | grabButton + 사다리/덩굴 인접 | grabButton 해제, 수중 진입, 탈진 | isDiving보다 낮은 우선순위 |
| `isCrawlClimbing` | 크롤 중 클라이밍 | `isCrawling && isClimbing` | 둘 중 하나 해제 | 복합 상태 |
| `isSwimming` | 수면 수영 | 수면 높이 = 목 이상 | 물 이탈, 다이빙 진입 | isDiving과 상호 배타 |
| `isDiving` | 잠수 | 수중 + moveForward > 0 | 수면 복귀, 탈진 | isSwimming과 상호 배타 |
| `isDipping` | 물 표면 (부분 잠수) | 수위 < 목 높이 | 완전 수중 or 이탈 | isSwimming의 전제 조건 |
| `isSliding` | 미끄러지기 (로프/얼음) | 슬라이딩 표면 + 점프 아님 | 점프 입력, 표면 이탈 | isCrawling과 히트박스 공유 |
| `isCeilingClimbing` | 천장 클라이밍 | grabButton + 천장 감지 | 버튼 해제, 장애물 | 중력 반전 |
| `isHeadJumping` | 헤드 점프 | grabButton + 스프린트 + 점프차지 | 공중에서 해제 | isSprintJump 대체 |
| `isWallJumping` | 벽 점프 | 벽 충돌 + 점프 입력 | 점프 완료 | 수평 충돌 각도 필요 |
| `isLevitating` | 공중부양 | 비행 능력 + !isFlying | 비행 활성화 | 크리에이티브 모드 |
| `isRopeSliding` | 로프 하강 | 덩굴 + 하강 + 손 미사용 | 상승 또는 손 사용 | isSliding과 연관 |
| `isAerodynamic` | 낙하 중 | 낙하 + 지면 미접촉 | 착지 or 수중 | 애니메이션 전용 |
| `isHandsVineClimbing` | 손 덩굴 클라이밍 | 덩굴 인접 + grabButton | 버튼 해제 | isFeetVineClimbing과 독립 |
| `isFeetVineClimbing` | 발 덩굴 클라이밍 | 발 위치에 덩굴 | 덩굴 이탈 | isHandsVineClimbing과 독립 |
| `isSprintJump` | 스프린트 점프 | 스프린트 중 점프 | 착지 | isHeadJumping보다 낮은 우선순위 |
| `isGroundSprinting` | 지면 스프린트 | 스프린트 + 지면 + !슬라이드/크롤 | 공중/슬라이드/크롤 | 점프 타입 결정에 사용 |
| `isFast` | 빠름 (속도 보너스) | 스프린트점프 or 빠른 스프린트 | 정상 속도 복귀 | isSlow와 상호 배타 |
| `isSlow` | 느림 (속도 감소) | 크롤링 or 크롤-클라이밍 | 정상 이동 | isFast와 상호 배타 |

### Boolean — 내부 추적 상태 (입력/의도)

| 플래그 | 설명 |
|--------|------|
| `wantClimbUp` | grabButton + moveForward > 0 → 올라가려는 의도 |
| `wantClimbDown` | wantClimb + moveForward ≤ 0 + !wantCrawl |
| `wantClimbCeiling` | grabButton + !wantCrawlNotClimb + !sneaking |
| `wantCrawlNotClimb` | grabButton + isCrawling + 수평 충돌 시 (기어서 지나가기) |
| `wouldIsSneaking` | sneakButton or grabButton(자유 클라이밍) or 크롤 엣지 |
| `isVineAnyClimbing` | `isVineOnlyClimbing \|\| isHandsVineClimbing \|\| isFeetVineClimbing` |
| `isClimbingStill` | 클라이밍 + 수직 입력 없음 |
| `isClimbHolding` | 클라이밍 + sneakButton (위치 고정) |
| `blockJumpTillButtonRelease` | 차지 취소 후 점프 버튼 해제까지 블록 |
| `wasOnGround` | 이전 틱 지면 접촉 여부 |
| `wasCrawling` | 이전 틱 크롤 상태 |

### Integer — 카운터/타입

| 필드 | 타입 | 용도 | 범위 |
|------|------|------|------|
| `angleJumpType` | int | 방향 점프 타입 (8방향) | 0-7 |
| `handsEdgeMeta` | int | 손 클라임 타입 (인코딩) | 0-3 |
| `feetEdgeMeta` | int | 발 클라임 타입 (인코딩) | 0-3 |
| `leftJumpCount` | int | 왼쪽 더블탭 감지 카운터 | -1~1 |
| `rightJumpCount` | int | 오른쪽 더블탭 감지 카운터 | -1~1 |
| `backJumpCount` | int | 뒤쪽 더블탭 감지 카운터 | -1~1 |
| `wallJumpCount` | int | 벽 점프 더블클릭 감지 | -1~1 |
| `collidedHorizontallyTickCount` | int | 연속 수평 충돌 틱 수 | 0~N |
| `updateCounter` | int | 다양한 타이밍 틱 카운터 | 0~N |

### Float — 물리 값

| 필드 | 용도 | 범위 |
|------|------|------|
| `exhaustion` | 피로도 (높을수록 행동 제한) | 0.0~MAX |
| `jumpCharge` | 차지 점프 충전량 | 0.0~MAX |
| `headJumpCharge` | 헤드 점프 충전량 | 0.0~MAX |
| `maxExhaustionForAction` | 행동 유지 피로도 임계값 | 0.0~MAX |
| `maxExhaustionToStartAction` | 행동 시작 피로도 임계값 | 0.0~MAX |
| `dippingDepth` | 수중 잠수 깊이 (0.0=수면, 1.0=완전잠수) | 0.0~1.0 |
| `horizontalCollisionAngle` | 벽 충돌 각도 (벽 점프에 사용) | 0.0~360.0 |
| `fadingPerspectiveFactor` | FOV 전환값 | -1~1 |

---

## B. 주요 상태 전환 상세

### 기어가기 (Crawling)

**진입:**
```java
boolean canCrawl =
    !isSwimming && !isDiving &&
    (!isDipping || (dippingDepth + heightOffset) < SwimCrawlWaterTopBorder) &&
    !isClimbing &&
    sp.fallDistance < Config._fallingDistanceMinimum.value;

// 천장이 낮아서 강제 크롤
double crawlStandUpCeiling = getMinPlayerSolidBetween(
    sp.boundingBox.maxY,
    sp.boundingBox.maxY + 1.1D, 0);
mustCrawl = (crawlStandUpCeiling - crawlStandUpBottom) < (sp.height - heightOffset);

isCrawling = canCrawl && (wantCrawl || mustCrawl);
```

**물리 변경:**
- 히트박스 높이: 1.8 → 0.6 블록
- 이동 속도: 일반 보행의 45%
- 눈 높이: 1.62 → 0.48 블록

---

### 클라이밍 (Climbing)

**진입:**
```java
boolean wouldWantClimb =
    (grabButton.Pressed ||
     (Config.isFreeClimbAutoLadderEnabled() && isFacedToLadder(isClimbCrawling)) ||
     (Config.isFreeClimbAutoVineEnabled() && isFacedToSolidVine)) &&
    (!isSliding || grabButton.Pressed && moveForward > 0F) &&
    !isHeadJumping && !wantCrawlNotClimb && !disabled;
```

**물리 변경:**
- 히트박스: 변경 없음 (0.6 x 1.8)
- 중력: 비활성화
- 이동: 수직 + 표면 방향만 허용
- 속도: 설정값에 따른 클라이밍 속도

---

### 수영 / 잠수 (Swimming / Diving)

**수영 진입:**
```java
if (playerSwimWaterBorder > SwimCrawlWaterTopBorder)
    isSwimming = swimming;
```

**잠수 진입:**
```java
boolean diving = Config.isDivingEnabled() &&
    (dippingDepth > DivingEntryWaterLevel) &&
    (moveForward > 0 || isVineAnyClimbing) &&
    (!isCrawling || couldStandUp);
isDiving = diving;
```

**물리 변경 (수영):**
- 속도: 보행의 ~60%
- 중력: 감소 (부력)
- Y 속도: 점프 시 상향 추력

**물리 변경 (잠수):**
- 속도: 수영의 ~50%
- 이동 방향: 3D (모든 축)
- 중력: 사실상 비활성화

---

### 벽 점프 (Wall Jumping)

**진입 + 방향 계산:**
```java
// 벽 점프 방향 = 벽 법선 방향 반사
float jumpAngle;
if (!wasCollidedHorizontally) {
    float movementAngle = getAngle(jumpMotionZ, -jumpMotionX);
    jumpAngle = horizontalCollisionAngle * 2 - movementAngle + 180F;
} else
    jumpAngle = horizontalCollisionAngle; // 벽에 수직으로 점프
```

---

### 헤드 점프 (Head Jump)

**진입 (충전 → 발동):**
```java
boolean isHeadJumpCharging = grabButton.Pressed &&
    (isGroundSprinting || isSprintJump || (isRunning() && sp.onGround)) &&
    !isCrawling;

if (isHeadJumpCharging && esp.movementInput.jump)
    headJumpCharge++;
else if (headJumpCharge > 0 && sp.onGround)
    tryJump(Config.HeadUp, null, null, null);
```

---

## C. updateEntityActionState() 실행 순서

```
1.  점프 상태 리셋 + exhaustion 임계값 저장
2.  키 입력 폴링 (8개 버튼: forward/left/right/back/jump/sprint/sneak/grab)
3.  비활성화 조건 체크 (탑승/수면/크리에이티브/UI열림)
4.  크롤 평가 → mustCrawl, isCrawling
5.  스니크/크롤 토글 처리
6.  클라이밍 적합성 → wantClimbUp, wantClimbDown
7.  천장 클라이밍 의도 → wantClimbCeiling
8.  수직 이동 우선순위 확정 (isClimbing > isSwimming > isDiving > isCrawling)
9.  Exhaustion 제약 적용 (임계값 초과 시 상태 진입 차단)
10. 스프린트 상태 (isGroundSprinting, isSprintJump, isFast)
11. 점프 타입 감지 (차지/헤드/각도/벽)
12. 수중 상태 평가 (isDipping, isSwimming, isDiving)
13. 클라이밍 상태 확정 (속도, 방향, exhaustion 적용)
14. 변경 감지 → addToSendQueue() (클라이언트 전용)
```

**충돌 해소 우선순위:**
- Flying > 모든 상태
- Climbing > Swimming > Crawling > Standing
- Exhaustion: 상태 진입 차단 (진행 중인 상태는 차단 안 함)
- 충돌(Collision): 즉시 상태 강제 해제

---

## D. 히트박스 (Bounding Box) 변경표

| 상태 | 너비 | 높이 | 눈 높이 | 비고 |
|------|------|------|---------|------|
| 기본 서있기 | 0.6 | 1.8 | 1.62 | 바닐라 동일 |
| 기어가기 | 0.6 | **0.6** | **0.48** | 렌더 오프셋 -1.0 |
| 수영/잠수 | 0.6 | 1.8 | 수면 레벨 | 히트박스 변경 없음 |
| 클라이밍 | 0.6 | 1.8 | 1.62 | 히트박스 변경 없음 |
| 슬라이딩 | 0.6 | **0.6** | **0.48** | 크롤과 동일 |
| 천장 클라이밍 | 0.6 | **0.6** | 천장 레벨 | 중력 반전 |

---

## E. 64비트 패킷 인코딩 (멀티플레이어 동기화)

28개 이상의 플래그를 64비트 long 하나에 압축하여 상태 변경 시에만 전송.

**비트 레이아웃:**
```
비트  0     : isSneaking
비트  1     : isRopeSliding
비트  2     : isWallJumping
비트  3     : isFast
비트  4     : isSlow
비트  5     : isClimbBackJumping
비트  6     : isClimbJumping
비트  7     : isHandsVineClimbing
비트  8     : isFeetVineClimbing
비트  9-11  : angleJumpType (3비트, 0-7 방향)
비트  12    : isSliding
비트  13    : isHeadJumping
비트  14    : isLevitating
비트  15    : isCeilingClimbing
비트  16    : doFlyingAnimation
비트  17    : doFallingAnimation
비트  18    : isSmall (height < 1.0)
비트  19    : isClimbing
비트  20    : isCrawling
비트  21    : isCrawlClimbing
비트  22    : isSwimming
비트  23    : isDipping
비트  24    : isDiving
비트  25    : isJumping
비트  26-29 : actualHandsClimbType (4비트)
비트  30-33 : actualFeetClimbType (4비트)
비트  34-63 : 예약 (미사용)
```

**인코딩 (addToSendQueue):**
```java
// 변경 있을 때만 전송 (delta compression)
if (state != prevPacketState) {
    SmartMovingPacketStream.sendState(comm, sp.getEntityId(), state);
    prevPacketState = state;
}
```

---

## F. SmartMovingPlayerBase 오버라이드 메서드 목록

| 메서드 | 방식 | 목적 |
|--------|------|------|
| `moveEntity(dx, dy, dz)` | before/after | 커스텀 충돌 응답, 계단 오르기 |
| `moveEntityWithHeading(strafe, forward)` | 완전 대체 | 커스텀 이동 벡터 계산 |
| `jump()` | 인터셉트 | 점프 의도 가로채기 (커스텀 점프 타입) |
| `onUpdate()` | before/after | 파티클, 애니메이션 업데이트 |
| `onLivingUpdate()` | before/after | Exhaustion 추적, 능력 체크 |
| `updateEntityActionState()` | 완전 대체 | 상태 플래그 매 틱 업데이트 |
| `isOnLadder()` | 완전 대체 | `isOnLadderOrVine()` 반환 |
| `isInsideOfMaterial(material)` | 완전 대체 | 정밀 액체 경계 감지 |
| `canTriggerWalking()` | 완전 대체 | 클라이밍/수영 중 걷기 소리 방지 |
| `isSneaking()` | 완전 대체 | 크롤/차지점프 시 스니크 강제 |
| `getBrightness(f)` | 완전 대체 | 잠수 중 밝기 조정 |
| `getFOVMultiplier()` | 완전 대체 | 스프린트/슬라이딩 시 FOV 변경 |
| `sleepInBedAt(x, y, z)` | before | 침대 진입 시 상태 초기화 |
| `setPositionAndRotation(...)` | before | 텔레포트 시 충돌 추적 초기화 |
| `writeEntityToNBT(tag)` | 완전 대체 | 비행 상태 NBT 저장 |
| `readEntityFromNBT(tag)` | hook | 비행 상태 NBT 복구 |

---

## 1.21.1 마이그레이션 포인트

| 원본 | Fabric 1.21.1 대응 |
|------|-------------------|
| `SmartMovingSelf` 클래스 | `SmartMovingState` 클래스 (별도 구현, `AttachmentType`으로 플레이어에 부착) |
| `PlayerAPI` 훅 | `@Mixin` on `ClientPlayerEntity` / `ServerPlayerEntity` |
| `updateEntityActionState()` 오버라이드 | `@Inject` in `LivingEntity.tickMovement()` or `PlayerEntity.tick()` |
| `moveEntity()` before/after | `@Inject` in `Entity.move()` |
| `jump()` 인터셉트 | `@Inject` in `PlayerEntity.jump()` |
| `isOnLadder()` 대체 | `@Inject` in `LivingEntity.isClimbing()` |
| `isSneaking()` 대체 | `@Inject` in `Entity.isInSneakingPose()` |
| 커스텀 패킷 (64비트) | `ServerPlayNetworking` + `ClientPlayNetworking` |
| 인스턴스 관리 (`SmartMovingFactory`) | `EntityAttachment` (Fabric AttachmentType API) |
| NBT 저장 (`writeEntityToNBT`) | `ServerPlayerEntity` Mixin + NBT 직렬화 |

---

## 미확인 / 추가 조사 필요

- [ ] `Factor()` 보간 유틸 구현 세부사항 — 애니메이션 부드러움에 핵심
- [ ] `angleJumpType` 8방향 점프 정확한 방향 매핑 (0-7 각각 어느 방향?)
- [ ] `handsEdgeMeta` / `feetEdgeMeta` 인코딩 의미 (0-3 각각?)
- [ ] 천장 클라이밍 중 중력 반전 구현 방식
- [ ] Fabric `AttachmentType` API로 `SmartMovingSelf` 대체 가능성 검증
- [ ] `wouldIsSneaking` → `isSneaking()` 변환이 렌더링에 미치는 정확한 영향
