# 종합 리서치 — BBox / POSE / EyeHeight 서버 동기화 (#2.7)

> **포커스 #2.7 1차 근거 문서**. 4 Agent 병렬 read 결과 (2026-04-25, 세션 2).
> 원본 13 파일 5091 줄 + vanilla 1.21.1 11 리서치 + 1.21.1 이식 5 파일 모두 라인별 전수
> read. **모든 라인-by-라인 작업의 1차 근거**.

| 영역 | 파일 | 줄 수 | Agent |
|---|---|---|---|
| heightOffset 시스템 (원본) | SmartMovingSelf.java | 3345 | A |
| 서버 sync 인프라 (원본) | Server / ServerComm / PacketStream / Comm / Client / IEntityPlayerMP / ISmartMovingClient | 1000 | B |
| playerapi 베이스 (원본) | PlayerBase / ServerPlayerBase / playerapi/Self / IEntityPlayerSP / ISmartMovingSelf | 746 | C |
| 1.21.1 vanilla + 매핑 + 이식 | vanilla 11 리서치 + Mixin·State·Network 5 코드 | (참고) | D |
| **합계 (원본만)** | **13 파일** | **5091 줄** | — |

---

## §1. SmartMovingSelf heightOffset 시스템

### 1.1 본체 (L1681-L1722)

**`resetHeightOffset()` — L1681-L1686**
```java
private void resetHeightOffset()
{
    sp.boundingBox.minY += heightOffset;      // L1683: bbox 복원
    sp.height -= heightOffset;                 // L1684: height 복원
    heightOffset = 0F;                         // L1685
}
```

**`resetInternalHeightOffset()` — L1688-L1692** ★ **신규 발견 (수면 전용)**
```java
private void resetInternalHeightOffset()
{
    sp.height -= heightOffset;                 // L1690: height 만 변경 (bbox 유지)
    heightOffset = 0F;                         // L1691
}
```
- 호출처: **L1359** (`landMotionPost` — `heightOffset != 0 && isSleeping` 시 수면 진입).
- 의미: bbox 그대로 두고 height 만 초기화. 수면 시 bbox 재계산 차단 목적.

**`setHeightOffset(float offset)` — L1694-L1704**
```java
private void setHeightOffset(float offset)
{
    resetHeightOffset();                       // L1696: 누적 방지
    if (offset == 0F) return;                  // L1697-L1698
    heightOffset = offset;                     // L1700
    sp.boundingBox.minY -= heightOffset;       // L1702: bbox minY 낮춤
    sp.height += heightOffset;                 // L1703: height 증가
}
```

**`getBrightness(f)` — L1708-L1714 (렌더 보정)**
```java
public float getBrightness(float f)
{
    sp.posY -= heightOffset;                   // L1710: posY 임시 상향 (-)
    float result = isp.localGetBrightness(f);
    sp.posY += heightOffset;                   // L1712: posY 복원 (+)
    return result;
}
```
- `getBrightnessForRender(f)` (L1716-L1722) 동일 패턴.
- 의미: heightOffset 음수 (-1F) 시 posY 가 1 만큼 낮으므로, 라이팅 계산은 머리 위치 기준 → 임시 상향.

**`afterMoveEntity` 의 매 프레임 posY 보정 — L1608-L1609** ★ **신규 발견**
```java
public void afterMoveEntity(double d, double d1, double d2)
{
    // ... (L1605-L1607 잡다)
    if (heightOffset != 0F)
        sp.posY = sp.posY + heightOffset;      // L1609: 매 프레임 posY 보정
```
- 의미: moveEntity 후 posY 를 heightOffset 만큼 자동 보정. 카메라/렌더 위치 보장.
- 1.21.1 매핑: vanilla `setPos()` 후 별도 처리 없으므로, **EntityDimensions 시스템이 자동 처리**
  (POSE 변경 → calculateDimensions → setBoundingBox + standingEyeHeight). 명시 매핑 불필요.

### 1.2 setHeightOffset(-1F) 호출처 14건 (정확 확정)

| # | 라인 | 진입 SM 상태 | 호출 메서드 | 컨텍스트 |
|---|---|---|---|---|
| 1 | L511 | isDiving / isSwimming | handleSwimming | 다이빙·수영 진입 |
| 2 | L518 | isCrawling (얕은 물) | handleSwimming | 얕은 물 크롤로 전환 |
| 3 | L1369 | (transient) | fromSwimmingOrDiving | 물 밖 진출 시 임시 적용 |
| 4 | L1382 | isCrawling | fromSwimmingOrDiving | 작은 공간 크롤 진입 |
| 5 | L1390 | isCrawling | fromSwimmingOrDiving | 수면 아래 크롤 진입 |
| 6 | L1400 | isCrawling | fromSwimmingOrDiving | 스니크 크롤 진입 |
| 7 | L2129 | isHeadJumping | tryJump (head=true) | 헤드 점프 진입 |
| 8 | L2512 | isFlying | updateEntityActionState | SM 비행 진입 엣지 |
| 9 | L2519 | isLevitating | updateEntityActionState | 부양 포션 엣지 |
| 10 | L2555 | isSliding | updateEntityActionState | 슬라이드 진입 |
| 11 | L2798 | isClimbCrawling | updateEntityActionState | 등반 크롤 진입 |
| 12 | L2829 | isCrawling | updateEntityActionState | 일반 크롤 진입 |
| 13 | L2851 | isDiving (얕은 물) | updateEntityActionState | 얕은 물 다이빙 |
| 14 | L2858 | isCrawling (얕은 물) | updateEntityActionState | 얕은 물 크롤 재진입 |

### 1.3 setHeightOffset(wasHeightOffset) 호출처 (이전 offset 복원, -1F 아님)

| 라인 | 컨텍스트 |
|---|---|
| L422 | 얕은 물 크롤 유지 — 이전 offset 그대로 복원 |
| L558 | 표준 수영 모드 + isCrawling — 이전 offset 보존 |

### 1.4 resetHeightOffset() 호출처 14건 ★ 추가 발견

| # | 라인 | 메서드 | 진입 트리거 |
|---|---|---|---|
| 1 | L249 | handleSwimming | useStandard && !isCrawling — 표준 수영 모드 진입 |
| 2 | L528 | handleSwimming | 얕은 물 → 일반 보행 |
| 3 | L1171 | handleCeilingClimbing | 천장 등반 + 크롤 충돌 |
| 4 | L1375 | fromSwimmingOrDiving | 물 밖 충돌 검사 후 |
| 5 | L2173 | standupIfPossible | 벽에서 내려올 때 |
| 6 | L2207 | standupIfPossible | 비행 상태 복원 |
| 7 | L2219 | standUp | isCrawling=false 전환 |
| 8 | L2273 | resetState | SM 비활성화 |
| 9 | L2746 | updateEntityActionState | isCrawlClimbing && canStandUp |
| 10 | L2774 | updateEntityActionState | wasCrawlClimbing && !toCrawling |
| 11 | L2780 | updateEntityActionState | isClimbCrawling && !toCrawling |
| 12 | L2816 | updateEntityActionState | mustCrawl == false 전환 |
| 13 | L2819 | updateEntityActionState | 크롤 동작 후 정리 |
| 14 | L2824 | updateEntityActionState | wasCrawling && !isCrawling — 일어남 |
| 15 | L2842 | updateEntityActionState | isShallowDiveOrSwim → 등반 전환 |

### 1.5 sp.height / sp.boundingBox 직접 참조 (heightOffset 외)

- **L3110** `boolean isSmall = sp.height < 1` — 크롤·다이빙 여부 판정 헬퍼.
- **L2401** `mustCrawl = crawlStandUpCeiling - crawlStandUpBottom < sp.height - heightOffset` — heightOffset 음수 시 더 큰 공간 요구.
- **sp.boundingBox.minY 읽기 전용** L256/L263/L680/L723/L799/L800/L828/L849/L1371/L1392/L1395/L1402/L2346/L2399/L2401/L2740/L2762/L2775/L2781/L2809/L2825 — 수위 / 충돌 / 크롤 판정용. 수정 없음.
- **`sp.setSize(...)` vanilla 호출 0건** — SmartMoving 부모 클래스가 처리.

### 1.6 heightOffset 필드 선언

원본 `SmartMoving.java` 부모 클래스: `protected float heightOffset = 0F;` (추정).

---

## §2. 서버 sync 인프라 (원본)

### 2.1 SmartMovingPacketStream.java (214줄)

**상태 패킷 구조**: **64-bit `long state` 단일 필드**.

**송신 (sendState)** L93-L109:
```java
objectOutput.writeByte(SmartMovingInfo.StatePacketId);   // L99
objectOutput.writeInt(entityId);                          // L100 — 추적 대상 ID
objectOutput.writeLong(state);                            // L101 — 64 bit state
```

**수신 (receivePacket)** L39-L91:
- L45 `byte packetId = objectInput.readByte();`
- L48-L51: StatePacketId 케이스 → entityId + state(long) 디코딩.

**Bit 사용 (★ 정확 확정)** — 64 bit 중 7 bit 만 활성:

| Bit | 필드 | 자료형 |
|---|---|---|
| 12 | `isCrawlClimbing` | bool |
| 13 | `isCrawling` | bool |
| 14 | `isClimbing` | bool |
| 15 | **`isSmall`** ★ | bool |
| 18 | `isCeilingClimbing` | bool |
| 31 | `isWallJumping` | bool |
| 33 | `isSneakButtonPressed` | bool |

**★ 핵심 발견**: 원본 PacketStream 에 **`isHeadJumping` / `isSliding` / `isSwimming` / `isDiving` / `isDipping` / `isFlying` / `isLevitating` 비트 없음**.

→ 원본 서버는 **`isSmall` (bit 15) 단일 필드** 로 작은 플레이어 통합 sync. 모든 0.8H 상태가 클라에서 isSmall=true 로 묶여 단일 비트 송신.

### 2.2 SmartMovingServer.java (354줄)

**서버 측 상태 필드 11종**:

| 라인 | 필드 | 자료형 | 용도 |
|---|---|---|---|
| 35 | `resetFallDistance` | bool | 낙상거리 초기화 플래그 |
| 36 | `resetTicksForFloatKick` | bool | 플로트 킥 틱 초기화 |
| 37 | `initialized` | bool | 패킷 스트림 초기화 |
| 38 | `withinOnLivingUpdate` | bool | onLivingUpdate 진행 중 |
| 40 | `crawlingInitialized` | bool | 크롤링 초기화 |
| 41 | `crawlingCooldown` | int | 크롤링 종료 후 쿨다운 (10 tick) |
| 42 | `isCrawling` | bool | 크롤링 활성 |
| 43 | **`isSmall`** ★ | bool | 작은 크기 (0.8H 통합) |
| 44 | `hunger` | float | 허기 |
| 48 | `isSneakButtonPressed` | bool | 스니크 버튼 상태 |
| 49 | `forceIsSneaking` | Boolean? | 스니크 오버라이드 |

**핵심 메서드**:
- **`processStatePacket(packet, state)` L69-L91**: 비트 디코딩 → setCrawling / setSmall / 직접 필드 (isClimbing, isCrawlClimbing, isCeilingClimbing, isWallJumping, isSneakButtonPressed) 저장. **L90**: `mp.sendPacketToTrackedPlayers(packet)` — 다른 플레이어 브로드캐스트.
- **`setCrawling(crawling)` L225-L230**: 상태 변경 + 쿨다운 10.
- **`setSmall(isSmall)` L232-L236**: ★ `mp.setHeight(isSmall ? 0.8F : 1.8F)` — 서버 측 height 직접 sync.
- **`afterOnLivingUpdate` L265-L296**: isSmall 시 아이템 수집 영역 확장 (`mp.expandBox(1, offset, 1)` L276).

### 2.3 SmartMovingServerComm.java (74줄)

`IPacketReceiver` 구현, 핸들러 7종:

| 메서드 | 라인 | 위임 |
|---|---|---|
| `processStatePacket` | 27-31 | `player.getMoving().processStatePacket(...)` |
| `processConfigInfoPacket` | 34-38 | 설정 정보 |
| `processConfigContentPacket` | 41-44 | 설정 내용 (클라 거부) |
| `processConfigChangePacket` | 47-51 | 설정 변경 요청 |
| `processSpeedChangePacket` | 54-58 | 이동 속도 변경 |
| `processHungerChangePacket` | 61-65 | 허기 sync |
| `processSoundPacket` | 68-72 | 음향 효과 |

### 2.4 SmartMovingComm.java (173줄)

클라이언트 측 패킷 처리:
- **L32-L42 `processStatePacket`**: `Minecraft.getMinecraft().theWorld.getEntityByID(entityId)` → `SmartMovingFactory.getOtherSmartMoving(EntityOtherPlayerMP)` → `SmartMovingOther.processStatePacket(state)` (다른 플레이어 상태 동기화).
- **L141-L144 `sendPacket(byte[])`**: `Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload(SmartMovingPacketStream.Id, data))`.

### 2.5 SmartMovingClient.java / SmartMovingPacketStream / 인터페이스

- **SmartMovingClient (80줄)**: ISmartMovingClient 구현 — 허기 / 점프 차징 / UI 드로잉. **패킷 전송 코드 없음** (Comm 담당).
- **IEntityPlayerMP.java (69줄, 21 메서드)** — 핵심: `setHeight` (L37) / `getHeight` (L41) / `getMinY` (L39) / `setMaxY` (L43) / `getBox` (L53) / `expandBox` (L55) / `sendPacketToTrackedPlayers` (L29).
- **ISmartMovingClient.java (36줄, 8 메서드)** — 허기 / 점프 차징 / UI 만.

### 2.6 송수신 흐름 종합

```
클라이언트 매 tick (SmartMovingSelf 상태 갱신 시)
   ↓
SmartMovingPacketStream.sendState(comm, entityId, state)
   ↓ writeByte(StatePacketId) + writeInt(entityId) + writeLong(state)
   ↓
Forge FMLProxyPacket → 서버
   ↓
SmartMovingPacketStream.receivePacket → SmartMovingServerComm.processStatePacket
   ↓
SmartMovingServer.processStatePacket(state)
   ↓ 비트 12,13,14,15,18,31,33 디코딩 → 필드 저장
   ↓ setSmall(isSmall) → mp.setHeight(0.8F or 1.8F)
   ↓
mp.sendPacketToTrackedPlayers(packet)  // L90 — 다른 클라들에 브로드캐스트
   ↓
다른 클라이언트 SmartMovingComm.processStatePacket → SmartMovingOther.processStatePacket(state)
   ↓ 동일 비트 디코딩 → SmartMovingOther 필드 갱신 (애니메이션·렌더용)
```

---

## §3. playerapi 베이스 (원본)

### 3.1 SmartMovingPlayerBase.java (313줄, 클라이언트)

**역할**: 클라이언트 PlayerAPI override. 43 메서드 (정적 포함).

**핵심 발견**:
- **bbox/height 직접 수정 0건** — PlayerBase 는 bbox 변경 안 함.
- **`isActive()` 분기 패턴** — 대부분 메서드가 `if (!moving.isActive()) return super.X()` 후 SM 위임.
- **`updateEntityActionState()` L203-L212** ★ 이중 패턴:
  ```java
  moving.tickEssential();              // ★ 항상 실행 (isActive 무관)
  if (!moving.isActive()) {
      localUpdateEntityActionState();  // vanilla
      return;
  }
  moving.updateEntityActionState(false);
  ```
- **`isOnLadder()` L190-L194**: `moving.isOnLadderOrVine()` (vine 포함, 메서드 이름 치환).
- **`localGetFOVMultiplier()` L284-L287**: `playerAPI.localGetFOVMultiplier()` — super 아닌 PlayerAPI 위임.
- **`writeEntityToNBT()` L252-L255**: isActive 체크 **없음** — 데이터 유실 방지.

### 3.2 SmartMovingServerPlayerBase.java (266줄, 서버)

**역할**: 서버 PlayerAPI override. 35 메서드.

**핵심 — bbox/height 직접 조작은 서버 PlayerBase 만 수행**:

| 메서드 | 라인 | 본체 |
|---|---|---|
| `getHeight()` | 52-55 | `return player.height;` |
| `setHeight(float)` | 214-217 | `player.height = height;` |
| `getMinY()` | 58-61 | `return player.boundingBox.minY;` |
| `setMaxY(double)` | 64-67 | `player.boundingBox.maxY = maxY;` |
| `getBox()` | 112-115 | `return player.boundingBox;` |
| **`getEyeHeight()`** ★★★ | **142-145** | **`return player.height - 0.18F;`** |

**`getEyeHeight()` 공식 ★ 확정**: `player.height - 0.18F` (고정 오프셋, 모든 포즈 공통).
- isSmall (height=0.8F): eyeHeight = **0.62F**
- 일반 (height=1.8F): eyeHeight = 1.62F

**hook 역전 패턴 L190-L199**:
```java
public void beforeUpdatePotionEffects() { moving.afterAddMovingHungerBatch(); }   // before → after
public void afterUpdatePotionEffects()  { moving.beforeAddMovingHungerBatch(); }  // after → before
```
- 의도적 역전 — 포션 업데이트가 허기 처리를 포함하므로 SM 허기 배치와 타이밍 맞춤.

**기타**:
- `sendPacket(byte[])` L220-L223 — FMLProxyPacket → playerNetServerHandler 직접 송신.
- `resetTicksForFloatKick()` L239-L242 — Reflection 으로 NetHandler 의 ticksForFloatKick 0 리셋 (클라이밍·비행 시 kick 방지).
- `getAllPlayers()` L257-L264 — 서버 전체 플레이어 → IEntityPlayerMP 배열.

### 3.3 클라/서버 비대칭 ★ 핵심 발견

| 메서드 | PlayerBase (클) | ServerPlayerBase (서) |
|---|---|---|
| `getHeight` / `setHeight` | ✗ | ✓ |
| `getMinY` / `setMaxY` | ✗ | ✓ |
| `getBox` | ✗ | ✓ |
| **`getEyeHeight()`** | **✗ (vanilla 통과)** | **✓ (`height - 0.18F`)** |
| `updateEntityActionState` / `moveEntityWithHeading` | ✓ | ✗ |
| `getBrightness` / `getFOVMultiplier` | ✓ | ✗ |
| `isOnLadder` (`isOnLadderOrVine`) | ✓ | ✗ |
| `writeEntityToNBT` (NBT) | ✓ | ✗ |
| `jump` | ✓ | ✗ |

→ **원본은 클라/서버 비대칭**. 클라 PlayerBase 는 bbox/height/eyeHeight override **안 함** — vanilla 가 SmartMovingSelf 의 sp.height 직접 변경분으로 자동 반영. 서버 PlayerBase 만 명시 override (서버에는 SmartMovingSelf 가 없으므로).

→ **1.21.1 매핑 시점**: client/server 모두 Mixin 으로 EntityDimensions/POSE 통일 처리 → **원본보다 대칭화** 필요. Phase 1 (세션 137) 클라 Mixin + Phase 2 (#2.7) 서버 Mixin 동등화가 정확.

### 3.4 playerapi/SmartMovingSelf.java (74줄)

`doFlyingAnimation()` SPC 호환 + `isSPCFlying()` 정적 (Reflection 7단계). **1.21.1 N/A** — SPC 모드 미존재 → 데드 코드.

### 3.5 IEntityPlayerSP.java (61줄, 18 메서드) / ISmartMovingSelf.java (32줄, 6 메서드)

- **IEntityPlayerSP**: 클라이언트 플레이어 접근 — 필드 getter/setter 9 + local vanilla 호출 9. **bbox/height 메서드 없음** (서버만).
- **ISmartMovingSelf**: 점프 차징 / 피로 6 메서드 — bbox 무관.

---

## §4. 1.21.1 vanilla 시스템 (매핑 근거)

### 4.1 EntityDimensions

**팩토리**:
- `EntityDimensions.changing(width, height)` — scale 변환 가능
- `EntityDimensions.fixed(w, h)` — scale 고정
- `.withEyeHeight(float)` — 눈높이 명시
- `.scaled(float)` — scale 배수

**`PlayerEntity.POSE_DIMENSIONS` 맵 (전수)**:

| EntityPose | width | height | eyeHeight | 타입 |
|---|---|---|---|---|
| STANDING | 0.6F | 1.8F | 1.62F | changing |
| CROUCHING | 0.6F | 1.5F | 1.27F | changing |
| SWIMMING | 0.6F | 0.6F | 0.4F | changing |
| FALL_FLYING | 0.6F | 0.6F | 0.4F | changing |
| SPIN_ATTACK | 0.6F | 0.6F | 0.4F | changing |
| SLEEPING | 0.2F | 0.2F | 0.2F | fixed |
| DYING | 0.2F | 0.2F | 1.62F | fixed |

**미등록 POSE** (getOrDefault → STANDING 폴백):
- `SLIDING` (index 15) ★
- LONG_JUMPING / CROAKING / 기타 11종 (mob 전용)

### 4.2 EntityPose enum 18종

`STANDING(0)` / `FALL_FLYING(1)` / `SLEEPING(2)` / `SWIMMING(3)` / `SPIN_ATTACK(4)` / `CROUCHING(5)` / `LONG_JUMPING(6)` / `DYING(7)` / `CROAKING(8)` / `USING_TONGUE(9)` / `SITTING(10)` / `ROARING(11)` / `SNIFFING(12)` / `EMERGING(13)` / `DIGGING(14)` / **`SLIDING(15)`** / `SHOOTING(16)` / `INHALING(17)`.

### 4.3 호출 체인 (POSE 변경 → dimensions 즉시 반영)

```
PlayerEntity.tick() (line 315)
  → super.tick()
  → updatePose()  ← Mixin @HEAD cancellable=true 진입점
       → setPose(EntityPose) → DataTracker.set(POSE, ...)
            → onTrackedDataSet(POSE)
                 → Entity.calculateDimensions()
                      1) newDims = LivingEntity.getDimensions(pose)
                            = pose==SLEEPING ? SLEEPING_DIMS : getBaseDimensions(pose).scaled(getScale())
                              ↑ Mixin @HEAD cancellable=true 진입점 (getBaseDimensions)
                      2) this.dimensions = newDims
                      3) this.standingEyeHeight = newDims.eyeHeight()  ← 카메라 즉시 반영
                      4) refreshPosition() → setBoundingBox()         ← 히트박스 즉시 반영
```

### 4.4 updatePose() vanilla 분기 (LivingEntity)

```
canChangeIntoPose(SWIMMING) == false → 전체 스킵 (현재 POSE 유지)

canChangeIntoPose(SWIMMING) == true:
  선호 POSE:
    isFallFlying()              → FALL_FLYING
    isSleeping()                → SLEEPING
    isSwimming()                → SWIMMING
    isUsingRiptide()            → SPIN_ATTACK
    isSneaking() && !flying     → CROUCHING
    else                        → STANDING

  실제 POSE:
    isSpectator || hasVehicle || canChangeIntoPose(선호) → 선호
    canChangeIntoPose(CROUCHING)                          → CROUCHING
    else                                                  → SWIMMING (강제 크롤링)
```

### 4.5 getEyeHeight 우선순위

1. **EntityDimensions.eyeHeight** (`POSE_DIMENSIONS.withEyeHeight(f)`) — **최우선**.
2. `getDefaultEyeHeight()` — POSE_DIMENSIONS 에 eyeHeight 없을 때 폴백.

calculateDimensions() 가 `standingEyeHeight = newDims.eyeHeight()` 즉시 반영. 카메라/getEyeY/getBrightness 등 모두 자동 반영.

### 4.6 PlayerEntityRenderer 렌더 보정

**setupTransforms 분기 3종**:
- **isFallFlying**: 엘리트라 회전 (super + 추가).
- **leaningPitch > 0**: 수영/크롤 (vanilla SWIMMING 포즈 감지) — `targetAngle = isTouchingWater ? -90-pitch : -90` / `lerp(leaningPitch, 0, target)` X축 회전 + `isInSwimmingPose() → translate(0, -1, 0.3)`.
- **leaningPitch == 0**: 일반.

**getPositionOffset (PlayerEntityRenderer)** — 크롤 시 Y -0.125 × scale 오프셋 (타인 플레이어 렌더링).

→ **SM 영향**: leaningPitch 가 SWIMMING 포즈 자동 진입으로 증가 → SLIDING 포즈 사용 시 vanilla 자동 leaningPitch 차단됨.

---

## §5. 1.21.1 현재 이식 상태 (Phase 1 + Phase 2 진입 전)

### 5.1 MixinPlayerEntityClient (Phase 1 — 세션 137 완료)

**`sm_getBaseDimensions_client` L55-L75** — `@At("HEAD") cancellable=true`:
```
smSmall = isCrawling || isClimbCrawling || isHeadJumping || isSliding
       || isSwimming_sm || isDiving || isFlying || isLevitating
→ smSmall ? cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F))

pose == SLIDING → cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F))

이외 → vanilla 통과
```

**`sm_updatePose_client` L109-L135** — `@At("HEAD") cancellable=true`:
```
isCrawling || isClimbCrawling      → setPose(SWIMMING),  ci.cancel()
isHeadJumping || isSliding         → setPose(SLIDING),   ci.cancel()
isSwimming_sm || isDiving          → setPose(SWIMMING),  ci.cancel()
isFlying || isLevitating           → setPose(SLIDING),   ci.cancel()  ★ SLIDING (수영 애니 회피)
이외 (isDipping 포함)              → vanilla 통과
```

**`sm_getOffGroundSpeed_client` L84-L90** — A-30 (세션 137).

### 5.2 MixinPlayerEntity (Phase 2 진입 전 — 미완)

**현 상태 (구 로직)**:
- **`sm_getBaseDimensions_server` L35-L46**: 분기:
  - `pose == SLIDING` → `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F)` ✓
  - `pose == SWIMMING && isCrawling` → `EntityDimensions.changing(0.6F, 1.0F).withEyeHeight(0.4F)` ⚠️ **height 1.0F 가 원본 0.8F 와 불일치** ★
  - 이외 → vanilla 통과

- **`sm_updatePose_server` L56-L68**: 분기:
  - `isCrawling` → `setPose(SWIMMING)`, cancel
  - `isSmall` → `setPose(SLIDING)`, cancel
  - 이외 → vanilla
  - **누락**: client 측 8 SM 상태 전수 매핑 안 됨. isHeadJumping/isSliding/isSwimming/isDiving/isFlying/isLevitating/isClimbCrawling 어떻게 처리하나? — 서버는 isHeadJumping 비트 수신 안 함 (★ 원본 PacketStream 분석과 정합).

- **`sm_addExhaustion` L77-L84**: SM 자체 소진 관리 — 현 포커스 무관.

### 5.3 SmartMovingServer.java (1.21.1, Phase 2 진입 전)

**현 필드**: `isCrawling` / `isSmall` / 기타 (네트워크 미배선).
**`processStatePacket(player, bits)` L112-L127**:
```java
isClimbing        = ((bits >> 14) & 1) != 0;
isCrawlClimbing   = ((bits >> 12) & 1) != 0;
isCeilingClimbing = ((bits >> 18) & 1) != 0;
isWallJumping     = ((bits >> 31) & 1) != 0;
setCrawling(((bits >> 13) & 1) != 0);
boolean newSmall = ((bits >> 15) & 1) != 0;
if (newSmall != isSmall) setSmall(player, newSmall);
isSneakButtonPressed = ((bits >> 33) & 1) != 0;
```

**★ 정확 — 원본 PacketStream 와 동일 7 비트만 디코딩**.

### 5.4 SmartMovingState.java (네트워크 인코딩, 34 비트)

| Bit | 필드 | 자료형 |
|---|---|---|
| 0-3 | actualFeetClimbType | int (4 bit) |
| 4-7 | actualHandsClimbType | int (4 bit) |
| 8 | isJumping | bool |
| 9 | isDiving | bool |
| 10 | isDipping | bool |
| 11 | isSwimming | bool |
| 12 | isCrawlClimbing | bool |
| 13 | isCrawling | bool |
| 14 | isClimbing | bool |
| 15 | **isSmall** ★ | bool |
| 16 | doFallingAnimation | bool |
| 17 | doFlyingAnimation | bool |
| 18 | isCeilingClimbing | bool |
| 19 | isLevitating | bool |
| 20 | isHeadJumping | bool |
| 21 | isSliding | bool |
| 22-24 | angleJumpType | int (3 bit) |
| 25 | isFeetVineClimbing | bool |
| 26 | isHandsVineClimbing | bool |
| 27 | isClimbJumping | bool |
| 28 | isClimbBackJumping | bool |
| 29 | isSlow | bool |
| 30 | isFast | bool |
| 31 | isWallJumping | bool |
| 32 | isRopeSliding | bool |
| 33 | isSneakButtonPressed | bool |

→ 1.21.1 SmartMovingState **이미 34 비트 사용** (원본 7 비트 보다 확장). Phase B "누락 필드 추가" 는 **이미 완료** — Phase B 는 의미 없음.

### 5.5 SmartMovingNetwork.java (Payload 정의)

7종 Payload (`StatePayload` / `ConfigInfo` / `ConfigContent` / `ConfigChange` / `SpeedChange` / `HungerChange` / `Sound`) 모두 정의 + register 완료.

**미배선** ★:
- `ServerPlayNetworking.registerReceiver(StatePayload.ID, ...)` — 서버 수신 핸들러.
- `ClientPlayNetworking.registerReceiver(StatePayload.ID, ...)` — 클라 수신 (다른 플레이어 상태).

---

## §6. 매핑 정합 표 + 누락 영역 종합

### 6.1 setHeightOffset(-1F) 호출처 14건 → 1.21.1 매핑

| # | 원본 라인 | SM 상태 | 1.21.1 POSE | 1.21.1 Dimensions | 정합 |
|---|---|---|---|---|---|
| 1 | L511 | isDiving / isSwimming | SWIMMING (vanilla 0.6×0.6) | changing(0.6, 0.8).withEyeHeight(0.62) | ⚠️ vanilla SWIMMING 0.6H 와 0.2 차이 — Mixin 보정 필요 |
| 2 | L518 | isCrawling | SWIMMING → SLIDING (Phase 1) | 동일 | ✓ |
| 3-6 | L1369-L1400 | isCrawling | SWIMMING → SLIDING | 동일 | ✓ |
| 7 | L2129 | isHeadJumping | SLIDING | 동일 | ✓ |
| 8 | L2512 | isFlying | SLIDING | 동일 | ✓ |
| 9 | L2519 | isLevitating | SLIDING | 동일 | ✓ |
| 10 | L2555 | isSliding | SLIDING | 동일 | ✓ |
| 11 | L2798 | isClimbCrawling | SWIMMING → SLIDING | 동일 | ✓ |
| 12 | L2829 | isCrawling | SWIMMING → SLIDING | 동일 | ✓ |
| 13 | L2851 | isDiving (얕은 물) | SWIMMING → SLIDING | 동일 | ✓ |
| 14 | L2858 | isCrawling (얕은 물) | SWIMMING → SLIDING | 동일 | ✓ |

→ Phase 1 client 측 모두 SLIDING 으로 통일 (8 SM 상태). vanilla SWIMMING 의 leaningPitch 자동 발동 회피.

### 6.2 ★ 핵심 의문 1 — Server MixinPlayerEntity height 1.0F 의 출처

`MixinPlayerEntity.sm_getBaseDimensions_server` L43-L44 의 `pose == SWIMMING && isCrawling → height 1.0F` 가 **원본 0.8F 와 불일치**.

원본 분석:
- `setHeightOffset(-1F)` → `sp.height += (-1F)` → vanilla 1.8F → SM 0.8F.
- 원본은 모든 small 상태 0.8F 통일.

→ **1.0F 는 1:1 번역 위반**. Phase 2 에서 0.8F 로 정정 필수.

### 6.3 ★ 핵심 의문 2 — focus_02_7 §3 Phase A "8 필드 추가" 의 1:1 번역 정합성

원본 PacketStream 은 `isSmall` 단일 비트로 통합 sync. 클라가 `isCrawling/isHeadJumping/isSliding/isSwimming/isDiving/isClimbCrawling/isFlying/isLevitating` 을 평가하여 OR 결과를 `isSmall` 로 송신. 서버는 `isSmall` 로 단순화.

**현 1.21.1 SmartMovingState 는 이미 21 bit 분리 송신** (isSmall + 8 개별 + 기타) — **원본보다 풍부**. 1:1 번역 관점에서:
- **방안 A (1:1 엄격)**: 서버가 `isSmall` 단일 비트만 디코딩, dimensions 결정에 isSmall 만 사용. 1.21.1 상태 비트는 다른 클라 애니메이션용으로만 활용.
- **방안 B (확장 활용)**: 서버가 21 bit 모두 디코딩 + dimensions 정밀 결정. 클라/서버 완벽 대칭.

★ 결정 권고: **방안 A** — 원본 1:1 충실. dimensions 은 `isSmall` 단일로 결정 + Phase 1 client 의 8 SM 상태 OR 결과를 `isSmall` 비트로 송신. 8 개별 비트는 SmartMovingOther (다른 플레이어 애니) 에서만 소비.

→ focus_02_7 §3 Phase A "isClimbCrawling / isHeadJumping / isSliding / isSwimming / isDiving / isDipping / isFlying / isLevitating 서버 필드 추가" 는 **방안 A 채택 시 불필요**. Phase A 대폭 축소 가능.

### 6.4 ★ 핵심 의문 3 — `getEyeHeight` 클라/서버 비대칭

원본:
- 클라 `SmartMovingPlayerBase` 에 `getEyeHeight` override **없음** — vanilla `sp.height - 0.18F` (vanilla EntityPlayer.getEyeHeight 가 height 기반).
- 서버 `SmartMovingServerPlayerBase` 만 명시 override `player.height - 0.18F`.

1.21.1:
- `EntityDimensions.withEyeHeight(0.62F)` 가 클라/서버 양쪽 dimensions 시스템에 자동 반영.
- 별도 `getEyeHeight` Mixin 불필요 — Phase 2 추가 작업 0.

### 6.5 누락 영역 (현재 1.21.1 미구현)

| 항목 | 위치 | 영향 |
|---|---|---|
| `ServerPlayNetworking.registerReceiver(StatePayload)` | SmartMoving.onInitialize | 서버 패킷 수신 X — 핵심 |
| `ClientPlayNetworking.registerReceiver(StatePayload)` | SmartMoving.onInitialize | 다른 플레이어 상태 수신 X |
| 클라 → 서버 매 tick 송신 | SmartMovingClientState.tick* | 송신 코드 X |
| 서버 → 다른 클라 브로드캐스트 | SmartMovingServer.processStatePacket | sendToOtherPlayers X |
| MixinPlayerEntity.sm_getBaseDimensions_server height 0.8F 정정 | L43-L44 | **1:1 번역 위반** |
| MixinPlayerEntity.sm_updatePose_server isSmall 단일 분기 | L56-L68 | 정합 — 단 Phase 1 의 8 SM OR 결과를 isSmall 송신해야 |
| client 측 isSmall 비트 인코딩 (8 SM OR) | SmartMovingState.encode | 정합 — isCrawling 외 OR 누락 가능, 검증 필요 |
| `afterMoveEntity` posY 보정 | (미구현) | 1.21.1 EntityDimensions 자동 처리로 N/A |
| `getBrightness` 보정 | (미구현) | 1.21.1 standingEyeHeight 자동 반영으로 N/A |
| PlayerEntityRenderer.setupTransforms | (미구현) | SLIDING 포즈 사용으로 leaningPitch 회피 — 추가 검증 필요 |
| PlayerEntityRenderer.getPositionOffset | (미구현) | 타인 플레이어 Y -0.125 — 우선순위 낮음 |

### 6.6 SLIDING 포즈 활용 정당성

vanilla 1.21.1 의 `EntityPose.SLIDING (15)` 이 `POSE_DIMENSIONS` 에 미등록 → STANDING 폴백 (0.6×1.8). SM 이 `getBaseDimensions(SLIDING)` Mixin override 로 0.6×0.8 + 0.62F 주입 가능 → vanilla 충돌 0. **재활용 정당** ✓.

vanilla SWIMMING 사용 시:
- leaningPitch 자동 증가 → setupTransforms 의 X축 회전 적용.
- isInSwimmingPose() = true → translate(0, -1, 0.3) 자동 적용.
- SM 크롤/슬라이드 등에서 부적합 → SLIDING 우선.

---

## §7. 보강 권고 (포커스 #2.7)

1. **§3 Phase A 대폭 축소** — 8 필드 추가 → `isSmall` 단일 충분 (원본 1:1).
2. **§3 Phase B 삭제** — SmartMovingState 이미 21 bit 분리 인코딩 완료.
3. **§3 Phase C 정정** — server `getBaseDimensions` height **1.0F → 0.8F**.
4. **§3 Phase D 단순화** — server `updatePose` 분기를 `isSmall` 단일로 (원본 일치).
5. **§3 Phase 신규 추가** — **Phase G (네트워크 핸들러 배선)** — 가장 핵심 누락:
   - G-1: SmartMoving.onInitialize 에 서버 수신 등록
   - G-2: 클라 → 서버 매 tick 송신
   - G-3: 서버 → 다른 클라 브로드캐스트
   - G-4: 클라 다른 플레이어 수신 → SmartMovingOther
6. **§3 Phase 신규 추가** — **Phase H (isSmall OR 인코딩 검증)** — 클라가 `isCrawling || isClimbCrawling || isHeadJumping || isSliding || isSwimming_sm || isDiving || isFlying || isLevitating` 의 OR 결과를 `isSmall` 비트로 송신하는지 SmartMovingState.encode 에서 grep.
7. **§9 신규 발견 추가** — `resetInternalHeightOffset` (수면 전용, 1.21.1 N/A) + `afterMoveEntity` posY 보정 (1.21.1 자동 처리).
8. **§7 근사 등록 후보**: `getBrightness` 렌더 보정 (1.21.1 standingEyeHeight 자동 → 영구 동치).

---

## §8. 1:1 번역 결론

원본 1.7.10 의 BBox/POSE/EyeHeight 시스템은 **클라/서버 비대칭** 으로 구성됨:
- 클라: `sp.height` 직접 변경 (heightOffset 시스템) → vanilla 자동 반영.
- 서버: `mp.setHeight` 명시 호출 (PacketStream isSmall 비트 → setSmall → setHeight).

1.21.1 매핑은 **클라/서버 대칭화** (Mixin EntityDimensions/POSE 양쪽 동일):
- Phase 1 (세션 137): 클라 Mixin 완료.
- Phase 2 (#2.7): 서버 Mixin 완성 + **네트워크 핸들러 배선** 이 핵심 잔존.

기존 focus_02_7 의 "8 필드 + 21 bit 확장" 계획은 **원본보다 과도한 확장**. 1:1 번역 = `isSmall` 단일 비트 충분. 이를 받아들이면 #2.7 작업 규모 ~10 원자로 축소 가능.
