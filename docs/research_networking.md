# 네트워킹 / 멀티플레이어 동기화 리서치

> 원본 소스: `SmartMovingComm.java`, `SmartMovingOther.java`, `SmartMovingPacketStream.java`, `SmartMovingFactory.java`, `SmartMovingServer.java`

---

## 관련 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `SmartMovingComm` | 패킷 수신/처리 중앙 허브 |
| `SmartMovingOther` | 원격 플레이어 상태 저장 + 디코딩 |
| `SmartMovingPacketStream` | 패킷 직렬화/역직렬화 |
| `SmartMovingFactory` | 플레이어 인스턴스 생명주기 관리 |
| `SmartMovingServer` | 서버 측 유효성 검증 + 배포 |

---

## 동작 원리

```
[로컬 플레이어] 상태 변경 감지 (addToSendQueue)
  ↓
64비트 long으로 압축 → 변경 시에만 전송
  ↓
[서버] processStatePacket()
  - 최소한의 검증 (isSmall, isClimbing 등)
  - 트래킹 중인 모든 플레이어에게 relay
  ↓
[다른 클라이언트] processStatePacket()
  - SmartMovingFactory에서 원격 플레이어 인스턴스 조회/생성
  - SmartMovingOther.processStatePacket() 비트 디코딩
  - 애니메이션 플래그 갱신
```

---

## A. 패킷 타입 및 구조

| ID | 타입 | 방향 | 용도 | 빈도 |
|----|------|------|------|------|
| 0 | State | Client→Server→All | 이동 상태 동기화 | 매 틱 (변경 시) |
| 1 | ConfigInfo | 양방향 | 설정 버전 정보 | 접속/변경 시 |
| 2 | ConfigContent | Server→Client | 전체 설정 데이터 | 접속/변경 시 |
| 3 | ConfigChange | Server→Client | 설정 변경 거부 알림 | 권한 없을 때 |
| 4 | SpeedChange | 양방향 | 속도 조정 | 속도 변경 시 |
| 5 | HungerChange | - | 미구현 | - |
| 6 | Sound | - | 미구현 | - |

### 패킷 바이트 구조 (ObjectOutputStream 기반)

```
STATE 패킷 (ID=0):
  [byte  ] 패킷 ID (0)
  [int   ] entityId
  [long  ] 64비트 압축 상태

CONFIG_CONTENT 패킷 (ID=2):
  [byte  ] 패킷 ID (2)
  [String[]] 설정 키-값 쌍 배열
  [String] 변경한 플레이어 username

SPEED_CHANGE 패킷 (ID=4):
  [byte  ] 패킷 ID (4)
  [int   ] 속도 차이값
  [String] username
```

---

## B. 64비트 상태 패킷 비트 레이아웃 (확정판)

`SmartMovingOther.processStatePacket(long state)` 디코딩 순서:

```
비트 0-3   : actualFeetClimbType  (4비트)
비트 4-7   : actualHandsClimbType (4비트)
비트 8     : isJumping
비트 9     : isDiving
비트 10    : isDipping
비트 11    : isSwimming
비트 12    : isCrawlClimbing
비트 13    : isCrawling
비트 14    : isClimbing
비트 15    : isSmall (height < 1.0)  ← 서버도 이 비트 사용
비트 16    : doFallingAnimation
비트 17    : doFlyingAnimation
비트 18    : isCeilingClimbing
비트 19    : isLevitating
비트 20    : isHeadJumping
비트 21    : isSliding
비트 22-24 : angleJumpType (3비트, 0-7)
비트 25    : isFeetVineClimbing
비트 26    : isHandsVineClimbing
비트 27    : isClimbJumping
비트 28    : isClimbBackJumping  ← 전환 시 onStartClimbBackJump() 콜백
비트 29    : isSlow
비트 30    : isFast
비트 31    : isWallJumping       ← 전환 시 onStartWallJump() 콜백
비트 32+   : isRopeSliding
비트 33    : isSneakButtonPressed (서버 전용 체크)
```

**디코딩 코드:**
```java
actualFeetClimbType  = (int)(state & 15); state >>>= 4;
actualHandsClimbType = (int)(state & 15); state >>>= 4;
isJumping      = (state & 1) != 0; state >>>= 1;
isDiving       = (state & 1) != 0; state >>>= 1;
// ...
// 전환 감지 (콜백 트리거)
boolean wasClimbBackJumping = isClimbBackJumping;
isClimbBackJumping = (state & 1) != 0; state >>>= 1;
if (!wasClimbBackJumping && isClimbBackJumping)
    onStartClimbBackJump();

boolean wasWallJumping = isWallJumping;
isWallJumping = (state & 1) != 0; state >>>= 1;
if (!wasWallJumping && isWallJumping)
    onStartWallJump(null);
```

**최적화 포인트:** 이전 상태(prevPacketState)와 동일하면 전송 안 함 (delta compression).

---

## C. SmartMovingFactory 인스턴스 생명주기

### 싱글톤 구조
```java
// 전역 단 1개 인스턴스만 존재
private static SmartMovingFactory factory;
```

### 로컬 플레이어
- 팩토리가 직접 관리하지 않음
- `IEntityPlayerSP.getMoving()` — 플레이어 엔티티 자신이 인스턴스 보유

### 원격 플레이어 (Hashtable 관리)
```java
Hashtable<Integer, SmartMovingOther> otherSmartMovings;

// 조회 (없으면 즉시 생성)
SmartMovingOther moving = otherSmartMovings.get(entityId);
if (moving == null)
    moving = new SmartMovingOther(entity);
    otherSmartMovings.put(entityId, moving);
```

### 메모리 누수 방지 (매 틱 정리)
```java
void doHandleMultiPlayerTick(Minecraft minecraft) {
    // 1. 현재 월드의 모든 플레이어를 순회 → foundAlive = true
    for (Entity player : minecraft.theWorld.playerEntities) {
        SmartMovingOther moving = doGetOtherSmartMoving(player);
        moving.foundAlive = true;
    }

    // 2. foundAlive가 false인 인스턴스 제거 (접속 끊김)
    Iterator<Integer> it = otherSmartMovings.keySet().iterator();
    while (it.hasNext()) {
        SmartMovingOther moving = otherSmartMovings.get(it.next());
        if (moving.foundAlive)
            moving.foundAlive = false;  // 다음 틱을 위해 초기화
        else
            it.remove();               // 접속 끊긴 플레이어 정리
    }
}
```

---

## D. 서버 사이드 로직

### 최소 검증 후 relay
```java
public void processStatePacket(FMLProxyPacket packet, long state) {
    // 히트박스 동기화
    boolean isSmall = ((state >>> 15) & 1) != 0;
    mp.setHeight(isSmall ? 0.8F : 1.8F);

    // 낙하 거리 리셋 (클라이밍 중 낙하 대미지 방지)
    resetFallDistance = isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping;

    // 트래킹 중인 모든 플레이어에게 relay
    mp.sendPacketToTrackedPlayers(packet);
}
```

서버는 실질적인 안티치트 없이 거의 그대로 relay. 히트박스 높이와 낙하 거리만 처리.

### 접속 시 설정 배포
```java
public SmartMovingServer(IEntityPlayerMP mp, boolean onTheFly) {
    if (Options._globalConfig.value)
        sendConfigContent(mp, optionsHandler.writeToProperties(), null);
    else if (Options._serverConfig.value)
        sendConfigContent(mp, optionsHandler.writeToProperties(mp, false), null);
}
```

---

## E. 설정 동기화 (서버 권한)

### 서버 권한 설정 목록
- `_globalConfig` — 전역 설정 모드 여부
- `_baseClimb`, `_freeClimb`, `_ceilingClimbing` — 클라이밍 기능
- `_swim`, `_dive`, `_crawl`, `_slide`, `_fly` — 이동 기능
- `_jumpCharge`, `_headJump`, `_angleJumpSide`, `_angleJumpBack` — 점프 기능
- `_usersWithChangeConfigRights[]` — 설정 변경 권한 유저 목록
- `_usersWithChangeSpeedRights[]` — 속도 변경 권한 유저 목록

### 설정 수신 시 클라이언트 동작
```java
// 1. ServerConfig에 수신된 설정 로드
ServerConfig.loadFromProperties(content, blockCode);
// 2. 활성 설정을 ServerConfig로 교체 (로컬 설정 무효화)
Config = ServerConfig;
```

서버 설정이 수신되면 클라이언트 로컬 설정은 완전히 무시.

---

## 1.21.1 마이그레이션 포인트

| 원본 (Forge 1.7.10) | Fabric 1.21.1 대응 |
|---------------------|-------------------|
| FML 패킷 채널 | `PayloadTypeRegistry` + `CustomPayload` (Fabric) |
| `ObjectOutputStream` 직렬화 | `PacketByteBuf` (RegistryByteBuf) |
| `mp.sendPacketToTrackedPlayers()` | `PlayerLookup.tracking()` (Fabric) |
| `mp.getAllPlayers()` | `server.getPlayerManager().getPlayerList()` |
| `Hashtable<Integer, SmartMovingOther>` | `Map<UUID, SmartMovingOther>` (EntityId 대신 UUID 사용) |
| `mp.setHeight(float)` | `@Mixin(ServerPlayerEntity)` → `getDimensions()` |
| FML 이벤트 기반 수신 | `ServerPlayNetworking.registerGlobalReceiver()` |
| 매 틱 정리 | `ServerEntityEvents.ENTITY_UNLOAD` 또는 `ServerPlayConnectionEvents.DISCONNECT` |

---

## 미확인 / 추가 조사 필요

- [ ] Fabric `PayloadTypeRegistry`로 커스텀 페이로드 등록하는 정확한 방법 (1.21.1 API)
- [ ] `PlayerLookup.tracking(entity)` — 특정 엔티티를 트래킹하는 플레이어 목록 반환 가능한지
- [ ] UUID vs EntityId — Fabric에서는 EntityId가 재시작 시 변경 → UUID로 교체 필요
- [ ] 서버에서 플레이어 히트박스 강제 변경 방법 (1.21.1 `ServerPlayerEntity`)
- [ ] 64비트 압축 유지 vs `PacketByteBuf`의 `writeBoolean` 개별 전송 중 어느 게 더 효율적인지
