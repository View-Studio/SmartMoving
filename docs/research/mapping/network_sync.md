# 교차 분석 — 네트워크/서버 동기화

## 분석 대상 파일

### 원본 (SmartMoving 1.7.10)
- `SmartMovingInfo.java` — 패킷 ID 상수, 채널 ID 소스
- `SmartMovingPacketStream.java` — 직렬화/역직렬화 + 디스패치 레이어
- `IPacketReceiver.java` — 수신 인터페이스 (7개 메서드)
- `IPacketSender.java` — 송신 인터페이스 (1개 메서드)
- `SmartMovingComm.java` — 클라이언트 측 수신 디스패처
- `SmartMovingServerComm.java` — 서버 측 수신 디스패처
- `SmartMovingServer.java` — 서버 측 플레이어별 처리 객체
- `SmartMovingPlayerBase.java` — 클라이언트 PlayerAPI hook
- `SmartMovingServerPlayerBase.java` — 서버 PlayerAPI hook + IEntityPlayerMP 구현
- `IEntityPlayerMP.java` — 서버 플레이어 인터페이스
- `ILocalUserNameProvider.java` — 로컬 유저명 제공 인터페이스
- `SmartMovingInstall.java` — 리플렉션 대상 필드명 상수
- `SmartMovingMod.java` — 모드 진입점 (채널 등록, 패킷 이벤트 핸들러)

### vanilla 1.21.1
- `ServerPlayNetworkHandler.onPlayerMove()` — 서버 이동 검증 (이전 세션 완독)

---

## 1. 네트워크 채널 등록

### 원본 동작 (SmartMovingMod.init())

```java
NetworkRegistry.INSTANCE.newEventDrivenChannel(SmartMovingPacketStream.Id).register(this);
```

- `SmartMovingPacketStream.Id`: `SmartMovingInfo.ModComId`를 최대 15자로 자른 문자열
- `SmartMovingInfo.ModComId` 공식: `ModName.replace(" ", "") + " " + ModComVersion`
  - 예: `"Smart Moving"` → `"SmartMoving"` + `" "` + `"2.3.1"` = `"SmartMoving 2.3.1"` → 15자 제한으로 `"SmartMoving 2.3."` (15자)
- `register(this)`: `SmartMovingMod` 인스턴스를 채널 이벤트 리스너로 등록
- 채널 수신 이벤트: `@SubscribeEvent onPacketData(ServerCustomPacketEvent)` / `onPacketData(ClientCustomPacketEvent)`

### 1.21.1 대응
- Fabric: `ServerPlayNetworking.registerGlobalReceiver(Identifier, ServerPlayNetworking.PlayChannelHandler)`
- 채널 식별: `Identifier` (namespace + path 형식) — 문자열 길이 제한 없음
- 클라이언트 수신: `ClientPlayNetworking.registerGlobalReceiver(Identifier, ClientPlayNetworking.PlayChannelHandler)`

### 동작 차이
- FML: 단일 채널 ID(String, 15자 제한)로 모든 패킷 타입을 다중화
- Fabric: `Identifier` 사용, 15자 제한 없음
- FML: `newEventDrivenChannel().register(this)` 방식 → Fabric: 람다/핸들러 직접 등록 방식

### 포팅 주의사항
- `SmartMovingPacketStream.Id` 15자 제한 로직: Fabric에서는 불필요 — `Identifier("smartmoving", "packets")` 형태로 대체
- 클라이언트/서버 수신 핸들러를 각각 별도 등록 필요
- 싱글 인스턴스 리스너(`SmartMovingMod`) → Fabric은 핸들러별 등록 구조로 분리

---

## 2. 패킷 ID 체계 (SmartMovingInfo)

### 원본 상수

| 상수명 | 값 | 용도 | 방향 |
|--------|----|------|------|
| `StatePacketId` | `0` (byte) | SM 이동 상태 전송 | 클라이언트→서버 |
| `ConfigInfoPacketId` | `1` (byte) | 클라이언트 설정 버전 통지 | 클라이언트→서버 |
| `ConfigContentPacketId` | `2` (byte) | 서버 설정 내용 배포 | 서버→클라이언트 |
| `ConfigChangePacketId` | `3` (byte) | 설정 변경 요청/거부 알림 | 양방향 |
| `SpeedChangePacketId` | `4` (byte) | 속도 변경 요청/응답 | 양방향 |
| `HungerChangePacketId` | `5` (byte) | 소진(exhaustion) 값 전송 | 클라이언트→서버 |
| `SoundPacketId` | `6` (byte) | 사운드 재생 요청 | 클라이언트→서버 |

### 1.21.1 대응
- Fabric: 패킷 타입마다 별도 `Identifier` 채널을 사용하거나, 단일 채널에 `PacketByteBuf.writeByte(packetId)` 로 내부 구분
- 별도 `Identifier`방식: `new Identifier("smartmoving", "state")`, `new Identifier("smartmoving", "config_content")` 등

### 동작 차이
- FML: 단일 채널 내 1바이트 packetId로 7종 다중화
- Fabric: 권장 방식은 패킷 타입별 채널 분리 (각 `Identifier`당 하나의 핸들러)
- 내부 packetId 바이트를 유지하면 단일 채널로 동일 구조 이식 가능하나 비Fabric 관용적 방식

### 포팅 주의사항
- 패킷 타입별 채널 분리 vs 단일 채널 내부 다중화 방식 결정 필요
- 단일 채널 유지 시 `PacketByteBuf.readByte()` + switch 구조는 그대로 이식 가능

---

## 3. 직렬화/역직렬화 (SmartMovingPacketStream)

### 원본 동작

**직렬화**: Java `ObjectOutputStream` + `ByteArrayOutputStream`
```
writeByte(packetId) [공통]
+ 패킷 타입별 추가 데이터 (writeInt/writeLong/writeFloat/writeObject)
```

**역직렬화**: Java `ObjectInputStream` + `ByteArrayInputStream`
```
readByte() → packetId → switch → readXxx() → comm.processXxx()
```

**패킷 포맷 (packetId 이후 페이로드)**:

| packetId | 페이로드 |
|----------|---------|
| 0 (State) | `int(entityId)` + `long(state)` — 총 12바이트 |
| 1 (ConfigInfo) | `Object(String info)` — Java 직렬화 오버헤드 포함 |
| 2 (ConfigContent) | `Object(String[] content)` + `Object(String username)` |
| 3 (ConfigChange) | (없음) — 1바이트만 |
| 4 (SpeedChange) | `int(difference)` + `Object(String username)` |
| 5 (HungerChange) | `float(hunger)` |
| 6 (Sound) | `Object(String soundId)` + `float(volume)` + `float(pitch)` |

**에러 처리**: `HashSet<StackTraceElement> errors` — 동일 위치 에러 첫 발생 시 전체 스택 출력, 이후 한 줄만 출력

**Java 직렬화 오버헤드**: `ObjectOutputStream`은 스트림 시작 시 4바이트 매직 + 버전 헤더를 씀

### 1.21.1 대응
- `FMLProxyPacket.payload().array()` → Fabric `PacketByteBuf` (Netty `ByteBuf` 기반)
- `ObjectOutputStream/InputStream` → `PacketByteBuf.writeXxx/readXxx()` 직접 메서드

### 동작 차이
- `ObjectOutputStream`은 Java 직렬화 프로토콜 헤더 오버헤드 포함 — `PacketByteBuf`는 없음
- `writeObject(String)` → `PacketByteBuf.writeString(str, maxLength)` 또는 `writeEncodedString(str)` 로 교체
- `writeObject(String[])` → 배열 길이 + 각 문자열 순차 기록으로 직접 구현
- `ObjectInputStream.readObject()` → 역직렬화 시 클래스 로딩이 발생하므로 보안/성능 위험

### 포팅 주의사항
- `ObjectOutputStream` 대신 `PacketByteBuf` 직접 사용 권장 (보안, 성능, Fabric 관용적)
- `String[]` 직렬화: `writeByte(length)` + 루프 `writeString()` 로 직접 구현
- `writeObject(String)` → `writeString(str, 32767)` (MC 표준 최대 길이)
- `errors HashSet` 에러 중복 방지 패턴: 그대로 유지 가능

---

## 4. IPacketReceiver / IPacketSender

### 원본 동작

**IPacketReceiver**: 7개 메서드, 모두 `boolean` 반환
- `processStatePacket(packet, player, int entityId, long state)`
- `processConfigInfoPacket(packet, player, String info)`
- `processConfigContentPacket(packet, player, String[] content, String username)`
- `processConfigChangePacket(packet, player)`
- `processSpeedChangePacket(packet, player, int difference, String username)`
- `processHungerChangePacket(packet, player, float hunger)`
- `processSoundPacket(packet, player, String soundId, float distance, float pitch)`
  - ⚠️ 파라미터 이름 `distance` 이나 PacketStream에서 `volume`으로 직렬화 — 이름 불일치, 동작 영향 없음

**IPacketSender**: 1개 메서드
- `sendPacket(byte[] data)` — 직렬화 완료된 바이트 배열 전송

**반환값(boolean)**: `SmartMovingPacketStream.receivePacket()`에서 반환값을 사용하지 않음 — 의미 불명확

### 1.21.1 대응
- `IPacketReceiver` → Fabric: `ServerPlayNetworking.PlayChannelHandler` (서버) / `ClientPlayNetworking.PlayChannelHandler` (클라이언트)
- `IPacketSender` → Fabric: `ServerPlayNetworkHandler.sendPacket()` / `ClientPlayNetworkHandler.sendPacket()`
- `FMLProxyPacket` 파라미터: Fabric에서는 제거 (이미 역직렬화된 데이터만 처리)

### 동작 차이
- `boolean` 반환값: Fabric 핸들러는 `void` 반환 — 반환값 제거 가능
- `player` 파라미터(`IEntityPlayerMP`): 서버 수신 시 Fabric은 `ServerPlayerEntity`로 교체

### 포팅 주의사항
- `IPacketReceiver`/`IPacketSender` 인터페이스는 그대로 유지하되, `FMLProxyPacket` 파라미터를 제거하거나 `PacketByteBuf`로 교체
- `IEntityPlayerMP` → Fabric 서버 플레이어 인터페이스로 교체

---

## 5. SmartMovingServerComm — 서버 수신 디스패처

### 원본 동작

**패턴**: 순수 디스패처. 로직 없이 `player.getMoving().processXxx()`로 전부 위임

```
processStatePacket → player.getMoving().processStatePacket(packet, state)
processConfigInfoPacket → player.getMoving().processConfigPacket(info)
processConfigContentPacket → return false  ← 서버는 클라이언트 설정 내용 무시
processConfigChangePacket → player.getMoving().processConfigChangePacket(localUserName)
processSpeedChangePacket → player.getMoving().processSpeedChangePacket(difference, localUserName)
processHungerChangePacket → player.getMoving().processHungerChangePacket(hunger)
processSoundPacket → player.getMoving().processSoundPacket(soundId, volume, pitch)
```

**`localUserNameProvider`**: `SmartMovingServerComm.localUserNameProvider` (static, 초기값 `null`)
- `processConfigChangePacket`: `localUserNameProvider != null ? localUserNameProvider.getLocalConfigUserName() : null`
- `processSpeedChangePacket`의 `username` 파라미터: 패킷에 포함된 이름을 **사용하지 않음** — 항상 `localUserNameProvider`에서 가져옴
- `processStatePacket`의 `entityId` 파라미터: **사용하지 않음** — `player` 객체로 이미 대상 특정

### 1.21.1 대응
- `ServerPlayNetworking.PlayChannelHandler` 람다 또는 별도 클래스로 구현
- `player.getMoving()` 패턴 유지 — Fabric 서버 플레이어에서 `SmartMovingServer` 인스턴스 접근

### 동작 차이
- FML: 이벤트 기반 단일 채널 핸들러 → 내부 switch
- Fabric: 채널별 핸들러 직접 등록

### 포팅 주의사항
- `processConfigContentPacket → return false` 유지 필요 — 서버는 클라이언트 설정 내용을 절대 처리하지 않음
- `localUserNameProvider` 주입 시점: Fabric에서는 `ServerLifecycleEvents.SERVER_STARTING` 등 초기화 이벤트에서 설정

---

## 6. SmartMovingComm — 클라이언트 수신 디스패처

### 원본 동작

**클라이언트가 서버로부터 받는 패킷 처리**:

```
processStatePacket:
  → world.getEntityByID(entityId) → SmartMovingFactory.getOtherSmartMoving(entity) → moving.processStatePacket(state)
  → 대상: 다른 플레이어(EntityOtherPlayerMP), 로컬 플레이어 아님

processConfigInfoPacket → return false  ← 클라이언트는 ConfigInfo 수신 안 함

processConfigContentPacket → SmartMovingComm.processConfigPacket(content, username, false)

processConfigChangePacket → Options.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer())

processSpeedChangePacket:
  → difference == 0: writeNoRightsToChangeSpeedMessageToChat()
  → difference != 0: Config.changeSpeed(difference)

processHungerChangePacket → return false

processSoundPacket → return false
```

**sendPacket(byte[] data)**:
```java
Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload(SmartMovingPacketStream.Id, data));
```
클라이언트→서버 방향 패킷 전송.

**isConnectedToRemoteServer()**:
```java
MinecraftServer.getServer() == null
  || Minecraft.getMinecraft().getIntegratedServer() == null
  || !Minecraft.getMinecraft().getIntegratedServer().isSinglePlayer()
```
3개 조건 중 하나라도 true → 원격 서버

**processConfigPacket(content, username, blockCode)** — 서버 설정 수신 핵심 로직:

| `content` 값 | 동작 |
|---|---|
| `[globalConfig키, "true"/"false"]` 2원소 배열 | `isGloballyConfigured` 추출 후 `content = null` 처리 |
| `null` (1차 변환 후) | `ServerConfig.load(false)` + `setCurrentKey(null)` |
| `length != 0` | `ServerConfig.loadFromProperties(content, blockCode)` |
| `length == 0` (빈 배열) | `Config = Options` (서버 설정 해제) + return |

**first/재설정 분기**:
- `first = (Config != ServerConfig)` — 최초 서버 설정 수신 여부
- `first == true`: `Config = ServerConfig` (이후 모든 설정 조회는 ServerConfig 기준), `writeServerConfigMessageToChat()`, `(!blockCode) → sendConfigInfo()`
- `first == false` (재설정): 채팅 메시지만 출력, `Config = ServerConfig`는 이미 설정됨

**processBlockCode(String text)** — 채팅 텍스트 기반 설정:
- 형식: `"§0§1...§f§f"` (앞 4자, 뒤 4자가 마커)
- 12개 기능 on/off를 채팅 텍스트 코드(`§0`~`§b`)로 제어
- `_baseClimb` = `"standard"`, 나머지 = `"false"` (비활성화)

### 1.21.1 대응
- `C17PacketCustomPayload` → Fabric `ClientPlayNetworking.send(CustomPayload)`
- `EntityOtherPlayerMP` → `OtherClientPlayerEntity`
- `Minecraft.getMinecraft().theWorld.getEntityByID()` → `MinecraftClient.getInstance().world.getEntityById()`
- `isConnectedToRemoteServer()`: `MinecraftClient.getInstance().isConnectedToRealms()` 또는 `getServer() == null`

### 동작 차이
- `EntityOtherPlayerMP` → 1.21.1 `OtherClientPlayerEntity`로 타입 교체
- `Config`, `ServerConfig` 정적 필드 참조: 1.21.1 설정 시스템과 통합 필요

### 포팅 주의사항
- `processBlockCode()` 메커니즘: 채팅 이벤트 훅으로 구현해야 함 — Fabric `ClientReceiveMessageEvents.CHAT` 또는 `GAME`
- 채팅 텍스트 형식 (`§0§1...§f§f`): 1.21.1 Text 시스템과 호환 여부 확인 필요
- `processConfigPacket(content, null, true)` 재귀 방지(`blockCode=true`): 1.21.1 이식 시에도 동일 패턴 유지 필요

---

## 7. SmartMovingServer — 서버 플레이어별 처리

### 원본 인스턴스 필드

| 필드 | 타입 | 초기값 | 용도 |
|------|------|--------|------|
| `mp` | `IEntityPlayerMP` | 생성자 | 플레이어 인터페이스 |
| `resetFallDistance` | boolean | false | afterOnUpdate에서 낙하 거리 리셋 여부 |
| `resetTicksForFloatKick` | boolean | false | afterOnUpdate에서 floatKick 틱 리셋 여부 |
| `initialized` | boolean | false | initialize() 완료 여부 |
| `withinOnLivingUpdate` | boolean | false | onLivingUpdate 진행 중 여부 |
| `crawlingInitialized` | boolean | (기본) | 크롤링 AABB 초기화 여부 |
| `crawlingCooldown` | int | 0 | 크롤링 종료 후 쿨다운 틱 |
| `isCrawling` | boolean | false | 크롤링 중 |
| `isSmall` | boolean | false | 작은 크기 상태 |
| `hunger` | float | 0 | 클라이언트 소진값 |
| `disableAddExhaustionDepth` | int | 0 | 소진 억제 중첩 깊이 |
| `disableAddExhaustion` | boolean | false | 소진 추가 비활성화 여부 |
| `isSneakButtonPressed` | boolean | false | 클라이언트 sneaking 버튼 상태 |
| `forceIsSneaking` | Boolean | null | isSneaking() 강제 오버라이드 |

### initialize(boolean alwaysSendMessage) — 플레이어 접속 시 설정 전송

| 조건 | 전송 내용 |
|------|---------|
| `Options._globalConfig.value == true` | 전체 설정 (`writeToProperties()`) |
| `Options._serverConfig.value == true` | 개별 설정 (`writeToProperties(mp, false)`) |
| `alwaysSendMessage == true` | `enabled ? new String[0] : null` (빈=활성, null=비활성) |

### processStatePacket(FMLProxyPacket, long state) — 핵심 메서드

**비트 추출**:

| 비트 위치 | `state >>> n & 1` | 추출 상태 |
|-----------|-------------------|---------|
| 12 | `isCrawlClimbing` | 크롤-클라이밍 여부 |
| 13 | `isCrawling` | 크롤링 여부 |
| 14 | `isClimbing` | 클라이밍 여부 |
| 15 | `isSmall` | 작은 크기 여부 |
| 18 | `isCeilingClimbing` | 천장 클라이밍 여부 |
| 31 | `isWallJumping` | 벽 점프 여부 |
| 33 | `isSneakButtonPressed` | 스니크 버튼 여부 |

**비트 위치 33**: Java `long`은 64비트이므로 유효. `state >>> 33`은 정상 동작.

**resetFallDistance 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing || isWallJumping`
**resetTicksForFloatKick 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing` (벽점프 제외)

**마지막**: `mp.sendPacketToTrackedPlayers(packet)` — 원본 패킷을 추적 중인 다른 플레이어에게 릴레이

### processConfigChangePacket(String localUserName) — 권한 확인

```
!Options._globalConfig.value → toggleSingleConfig() (개인)
localUserName == username (참조 동등성!) → toggleConfig() (전체)
username이 _usersWithChangeConfigRights에 있음 → toggleConfig() (전체)
그 외 → sendConfigChange(mp) (거부 응답)
```

⚠️ **`localUserName == username`은 `==` 참조 비교** — `equals()` 아님. 문자열 인터닝 의존.

### processHungerChangePacket(float hunger)

`this.hunger = hunger` 저장. 이후 `addMovementStat()`에서 소진 대체.

**hunger 특수값**: `hunger == -1` → 소진 억제 비활성 (`beforeAddMovingHungerBatch()`에서 체크)
**hunger == 0** → `addMovementStat()`에서 `localAddExhaustion(hunger)` 호출 안 함

### processSoundPacket(soundId, volume, pitch)

`mp.localPlaySound(soundId, volume, pitch)` → `player.playSound()` 직접 호출

### afterOnUpdate() — 매 틱 후처리

```
resetFallDistance == true → mp.resetFallDistance()
  [mp.resetFallDistance() 내부: fallDistance = 0; motionY = 0.08]
resetTicksForFloatKick == true → mp.resetTicksForFloatKick()
  [mp.resetTicksForFloatKick() 내부: Reflect.SetField(NetHandlerPlayServer, "floatingTickCount"/"field_147365_f"/"f", 0)]
```

`resetFallDistance`/`resetTicksForFloatKick`은 직전 상태 패킷에서 설정됨.

### 소진 인터셉트 구조

```
addMovementStat(x, y, z):
  beforeAddMovingHungerBatch()    ← disableAddExhaustion = true (hunger != -1 시)
  mp.localAddMovementStat(x,y,z) ← 내부에서 addExhaustion() 호출됨
    addExhaustion(val):           ← disableAddExhaustion == true → 무시됨
  hunger != 0 && !withinOnLivingUpdate → mp.localAddExhaustion(hunger)  ← SM 소진값 적용
  afterAddMovingHungerBatch()     ← disableAddExhaustion = false

beforeUpdatePotionEffects() hook → moving.afterAddMovingHungerBatch()
afterUpdatePotionEffects() hook  → moving.beforeAddMovingHungerBatch()
[의도적 역전: 포션 업데이트 사이클과 허기 배치 처리 타이밍 동기화]
```

**목적**: vanilla의 이동 소진 계산을 억제하고 클라이언트 SM 소진값으로 대체.

### setSmall(boolean isSmall) — 크기 변경

```
isSmall == true  → mp.setHeight(0.8F)
isSmall == false → mp.setHeight(1.8F)
```

### isSneaking() — 스니킹 오버라이드

```
forceIsSneaking != null → 강제 반환
그 외 → mp.localIsSneaking()

beforeActivateBlockOrUseItem() → forceIsSneaking = isSneakButtonPressed
afterActivateBlockOrUseItem()  → forceIsSneaking = null
```

**목적**: 크롤링/스몰 상태에서 블록 상호작용 시 스니킹 판정을 클라이언트 버튼 상태와 동기화.

### isEntityInsideOpaqueBlock() — 크롤링 쿨다운 기간 억제

```
crawlingCooldown > 0 → return false  (10틱간 억제)
그 외 → mp.localIsEntityInsideOpaqueBlock()
```

크롤링 종료 직후(`setCrawling(false)`) → `crawlingCooldown = 10` 설정.

### 1.21.1 대응

| 원본 | 1.21.1 Fabric |
|------|---------------|
| `FMLLog.warning/info()` | `LOGGER.warn/info()` |
| `IEntityPlayerMP mp` | Fabric `ServerPlayerEntity` wrapping |
| `mp.resetTicksForFloatKick()` | `ServerPlayNetworkHandler.floatingTicks` Mixin 접근 |
| `mp.getAllPlayers()` | `server.getPlayerManager().getPlayerList()` |
| `optionsHandler.writeToProperties()` | SM 설정 직렬화 → `PacketByteBuf` 기반 재구현 |

---

## 8. SmartMovingServerPlayerBase — 서버 PlayerAPI hook

### 원본 동작

**생성자**: `moving = new SmartMovingServer(this, false)` — `onTheFly=false` (지연 초기화)

**패킷 전송 (`sendPacket(byte[] data)`)**:
```java
player.playerNetServerHandler.sendPacket(
    new FMLProxyPacket(Unpooled.wrappedBuffer(data), SmartMovingPacketStream.Id));
```
- `Unpooled.wrappedBuffer(data)`: 복사 없이 byte[] 래핑 (Netty)
- `FMLProxyPacket(ByteBuf, channelId)` → `NetServerHandler.sendPacket()`

**패킷 릴레이 (`sendPacketToTrackedPlayers(FMLProxyPacket packet)`)**:
```java
player.mcServer.worldServerForDimension(player.dimension)
    .getEntityTracker().func_151247_a(player, packet);
```
- `func_151247_a`: 난독화된 EntityTracker 메서드 — 해당 플레이어를 추적 중인 모든 클라이언트에 패킷 전송
- `player.dimension`: 현재 차원 ID

**`resetFallDistance()`**:
```java
player.fallDistance = 0;
player.motionY = 0.08;  // 하강 속도 상쇄 (gravity = 0.08)
```

**`resetTicksForFloatKick()`**:
```java
Reflect.SetField(
    net.minecraft.network.NetHandlerPlayServer.class,
    player.playerNetServerHandler,
    SmartMovingInstall.NetServerHandler_ticksForFloatKick,  // "floatingTickCount"/"field_147365_f"/"f"
    0);
```
- 리플렉션으로 `NetHandlerPlayServer.floatingTickCount` 필드를 0으로 리셋
- 클라이밍/크롤-클라이밍/천장클라이밍 중 floating kick 방지

**`getAllPlayers()`**:
```java
List<?> playerEntityList = player.mcServer.getConfigurationManager().playerEntityList;
// 각 EntityPlayerMP → IServerPlayerAPI → getServerPlayerBase(ModName) → IEntityPlayerMP
```

**hook 메서드 (isActive 체크 없음)**:
- `beforeOnUpdate()` → `moving.beforeOnUpdate()` (crawlingCooldown--)
- `afterOnUpdate()` → `moving.afterOnUpdate()` (fallDistance/floatKick 리셋)
- `beforeOnLivingUpdate()` → `moving.beforeOnLivingUpdate()` (withinOnLivingUpdate=true)
- `afterOnLivingUpdate()` → `moving.afterOnLivingUpdate()` (isSmall 아이템 획득 확장)
- `beforeUpdatePotionEffects()` → `moving.afterAddMovingHungerBatch()` (의도적 역전)
- `afterUpdatePotionEffects()` → `moving.beforeAddMovingHungerBatch()` (의도적 역전)
- `isEntityInsideOpaqueBlock()` → `moving.isEntityInsideOpaqueBlock()` (크롤링 쿨다운)
- `addExhaustion(exhaustion)` → `moving.addExhaustion(exhaustion)` (소진 인터셉트)
- `addMovementStat(x,y,z)` → `moving.addMovementStat(x,y,z)`
- `isSneaking()` → `moving.isSneaking()` (스니킹 오버라이드)

**`SmartMovingServerPlayerBase` 특이점**: 클라이언트의 `SmartMovingPlayerBase`는 모든 hook에 `moving.isActive()` 체크가 있으나, 서버 측은 isActive 체크 없이 **무조건** 위임.

### 1.21.1 대응
- `ServerPlayerBase` / `ServerPlayerAPI` → Fabric Mixin
- `player.playerNetServerHandler` → `serverPlayer.networkHandler` (`ServerPlayNetworkHandler`)
- `FMLProxyPacket` + `Unpooled` → Fabric `CustomPayload` + `PacketByteBuf`
- `func_151247_a` → 1.21.1 `PlayerLookup.tracking(entity)` 또는 `ServerWorld.getChunkManager().sendToOtherNearbyPlayers()`
- `NetHandlerPlayServer.floatingTickCount` → Yarn: `ServerPlayNetworkHandler.floatingTicks` (`field_14138`)
- `player.mcServer.getConfigurationManager().playerEntityList` → `server.getPlayerManager().getPlayerList()`

### 동작 차이
- `player.fallDistance = 0; player.motionY = 0.08`: 1.21.1에서 `motionY` → `player.setVelocity(vel.x, 0.08, vel.z)` 또는 `Velocity.y = 0.08`
- `func_151247_a` 릴레이: 1.21.1에서 Fabric `PlayerLookup.tracking(serverPlayer)` 반환 컬렉션 이용

### 포팅 주의사항
- **`resetTicksForFloatKick()` 핵심**: `ServerPlayNetworkHandler.floatingTicks` (Yarn: `field_14138`)를 0으로 리셋해야 함. Mixin `@Accessor` 또는 `@Shadow`로 접근 필요
- **`sendPacketToTrackedPlayers`**: 1.21.1 EntityTracker API 변경으로 재구현 필요 — `PlayerLookup.tracking(ServerWorld, entity.getBlockPos())` 사용 검토
- **isActive 체크 없음**: 서버 훅은 SM 비활성 시에도 실행 → Mixin에서 SM 활성 여부 체크 로직 추가 필요 여부 결정

---

## 9. SmartMovingPlayerBase — 클라이언트 PlayerAPI hook

### 원본 주요 hook (네트워크 관련)

**`updateEntityActionState()`** — 상태 패킷 전송 진입점:
```
moving.tickEssential()  ← isActive() 무관하게 항상 호출
isActive() → moving.updateEntityActionState(false)
!isActive() → localUpdateEntityActionState()
```

**`writeEntityToNBT(NBTTagCompound)`** — isActive 무관하게 항상:
```java
moving.writeEntityToNBT(nBTTagCompound);  // 항상 실행 (세이브 데이터)
```

**`moveEntityWithHeading(float f, float f1)`** — 이동 + 패킷 전송:
```
isActive() → moving.moveEntityWithHeading(f, f1)
!isActive() → super.moveEntityWithHeading(f, f1)
```

**네트워크 관련 hook 목록**:
- `isOnLadder()` → `moving.isOnLadderOrVine()` (vine 포함 확장 판정)
- `isSneaking()` → `moving.isSneaking()`

### 1.21.1 대응
- `ClientPlayerBase` → Fabric Mixin
- `@Inject(at = @At("HEAD"))` = before hook
- `@Inject(at = @At("TAIL"))` = after hook
- `@Invoker` / `@Accessor` = local 메서드
- `moving` 필드 → `@Unique` 필드 + 인터페이스 injection
- `tickEssential()` 무조건 호출: Mixin HEAD에서 isActive 무관하게 실행

### 포팅 주의사항
- `tickEssential()` + `isActive()` 분기 패턴: Mixin에서 `@Inject` HEAD에 tickEssential 호출, 이후 분기 처리
- `updateEntityActionState()` → 1.21.1 `PlayerEntity.tickMovement()` 또는 `ClientPlayerEntity.sendMovementPackets()` Mixin

---

## 10. 상태 패킷 전송 타이밍 (클라이언트→서버)

### 원본 동작

상태 패킷 전송은 `SmartMovingSelf`(또는 연결된 처리)에서 이동 상태가 변경될 때 호출:
```java
SmartMovingPacketStream.sendState(SmartMovingComm.instance, sp.getEntityId(), state);
```

`state` = 33비트 long (비트 위치는 SmartMovingOther 리서치에서 확인됨):
- bit 12: isCrawlClimbing
- bit 13: isCrawling
- bit 14: isClimbing
- bit 15: isSmall
- bit 18: isCeilingClimbing
- bit 31: isWallJumping
- bit 33: isSneakButtonPressed
- (기타 이동 상태 비트들 — SmartMovingOther 리서치 참조)

### 서버 수신 후 릴레이

```java
// SmartMovingServer.processStatePacket()
mp.sendPacketToTrackedPlayers(packet);  // 원본 패킷 그대로 다른 플레이어에게 릴레이
```

릴레이된 패킷은 다른 클라이언트의 `onPacketData(ClientCustomPacketEvent)` → `SmartMovingComm.processStatePacket()` → `SmartMovingOther.processStatePacket(state)` 로 처리.

### 1.21.1 대응
- 클라이언트 전송: `ClientPlayNetworking.send(statePacket)`
- 서버 수신 후 릴레이: `PlayerLookup.tracking(serverPlayer)` + `ServerPlayNetworking.send(player, packet)`

### 포팅 주의사항
- **릴레이 구조 재구현 필수**: 서버가 단순히 원본 패킷을 다른 플레이어에게 그대로 전달하는 방식
- Fabric에서는 `PacketByteBuf`를 재생성하거나 래핑해서 릴레이
- 추적 중인 플레이어 집합: `PlayerLookup.tracking(serverWorld, serverPlayer.getBlockPos())` 또는 `PlayerLookup.tracking(serverPlayer)` — API 확인 필요

---

## 11. floating kick 방지 — resetTicksForFloatKick()

### 원본 동작

**서버 측 kick 메커니즘 (vanilla 1.7.10)**:
- `NetHandlerPlayServer.floatingTickCount` (Yarn: `field_147365_f`, obf: `f`)
- 플레이어가 공중에 불법으로 유지되면 틱마다 증가
- 일정 값 초과 시 kick

**SM의 우회**:
```java
// SmartMovingServer.processStatePacket()에서
resetTicksForFloatKick = isClimbing || isCrawlClimbing || isCeilingClimbing;

// SmartMovingServer.afterOnUpdate()에서
if(resetTicksForFloatKick)
    mp.resetTicksForFloatKick();

// SmartMovingServerPlayerBase.resetTicksForFloatKick()
Reflect.SetField(NetHandlerPlayServer.class, player.playerNetServerHandler,
    SmartMovingInstall.NetServerHandler_ticksForFloatKick, 0);
// 필드명: "floatingTickCount" / "field_147365_f" / "f"
```

### 1.21.1 vanilla 대응 (ServerPlayNetworkHandler_onPlayerMove 리서치에서 확인)

**Yarn 매핑**:
- 필드: `ServerPlayNetworkHandler.floatingTicks` = Yarn `field_14138`
- 타입: `int`

**tick() 내 처리**:
```
floating == true → floatingTicks++
  floatingTicks > 80 → kick("multiplayer.disconnect.flying")
floating == false → floatingTicks = 0
```

**floating 판정 조건** (onPlayerMove에서 설정):
```
floating = true if:
  moveY stored (dy) < -0.03125 (하강 중)
  AND !groundCollision (move() 후 isOnGround가 false)
  AND !SPECTATOR
  AND !server.isFlightEnabled()
  AND !player.getAbilities().allowFlying
  AND !hasStatusEffect(LEVITATION)
  AND !isFallFlying
  AND !isUsingRiptide
  AND isEntityOnAir(player)
```

**SM 클라이밍이 kick을 받는 이유**: 클라이밍 중 공중에 유지 → `floating = true` → `floatingTicks > 80` → kick

### 포팅 주의사항
- `ServerPlayNetworkHandler.floatingTicks` 필드를 0으로 리셋하는 Mixin `@Accessor` 또는 `@Shadow`+`@Mutable` 필요
- **리셋 조건**: `isClimbing || isCrawlClimbing || isCeilingClimbing` — 벽점프는 제외
- 1.21.1 `floatingTicks` 임계값: **80틱** (1.7.10과 동일한지 확인 필요 — vanilla 리서치에서 확인됨)
- `floating` 판정 자체를 조작하는 방법도 검토 가능 (SM 클라이밍 중 `allowFlying` 강제 true 등) — 단, 부작용 있음

---

## 12. 서버 이동 검증과 SM (ServerPlayNetworkHandler 관련성)

### vanilla 검증 요약 (이전 세션 완독 내용)

| 검증 | 임계값 | SM 영향 |
|------|--------|---------|
| NaN/Infinity | 항상 | 해당 없음 |
| 대기 텔레포트 | `requestedTeleportPos != null` | SM rubber-band 후 위치 확정 전까지 이동 패킷 무시 |
| 탑승 중 | `hasVehicle()` | SM 이동 억제 |
| 슬리핑 중 이동 | `distanceSq > 1` | 해당 없음 |
| 속도 초과 | `distanceSq - velocityLenSq > 100 * packetsSinceLastTick` | 고속 클라이밍 시 rubber-band 위험 |
| "moved wrongly" | postDistanceSq > 0.0625 | 서버-클라이언트 물리 불일치 시 경고 |
| floating | floatingTicks > 80 | SM 클라이밍 중 kick → resetTicksForFloatKick 필수 |
| 충돌 통과 | isPlayerNotCollidingWithBlocks 검사 | SM 위치 이동 시 블록 충돌 없어야 |

### "moved too quickly" 상세
```
threshold = isFallFlying ? 300f : 100f
if (distanceSq - velocityLenSq > threshold * packetsSinceLastTick AND !isHost())
  → rubber-band
```
- SM 클라이밍 최대 속도 `FastUpMotion = 0.2D` → 1틱 이동 거리 최대 0.2 → `distanceSq = 0.04` — 임계값 100 대비 안전
- 단, 벽점프 또는 여러 패킷 누적 시 초과 가능성 주의

### 포팅 주의사항
- **서버 측 SM 상태 인식**: 서버가 클라이밍 상태를 알아야 floating 판정 우회 가능
- SM 상태 패킷을 서버가 수신해야 `resetTicksForFloatKick` 타이밍 결정 가능
- **rubber-band 방지**: SM 이동이 "moved wrongly" 조건을 트리거하면 서버가 위치를 되돌림 → SM 클라이언트 이동이 서버 물리와 동일하게 계산되어야 함 (서버 측 동일 물리 재현 필요)

---

## 13. 설정 배포 프로토콜 전체 흐름

```
[클라이언트 접속]
서버 SmartMovingServer.initialize()
  → sendConfigContent(mp, content, null)
    → 클라이언트 processConfigContentPacket()
      → processConfigPacket(content, null, false)
        → ServerConfig.loadFromProperties()
        → Config = ServerConfig
        → sendConfigInfo(SmartMovingComm.instance, SmartMovingConfig._sm_current)
          → 서버 processConfigInfoPacket()
            → player.getMoving().processConfigPacket(info)  [버전 호환성 로그]

[클라이언트 설정 변경 요청]
클라이언트 sendConfigChange(SmartMovingComm.instance)
  → 서버 processConfigChangePacket()
    → 권한 있음: toggleConfig() → 모든 플레이어에게 sendConfigContent()
    → 권한 없음: sendConfigChange(mp) → 클라이언트 processConfigChangePacket()
      → writeNoRightsToChangeConfigMessageToChat()

[서버 설정 해제]
서버 sendConfigContent(mp, new String[0], null)
  → 클라이언트 processConfigPacket([], null, false)
    → Config = Options  [서버 설정 해제, 클라이언트 기본으로 복귀]
```

---

## 14. LocalUserNameProvider — 로컬 유저명 제공

### 원본 동작

```java
// SmartMovingMod.init()에서
SmartMovingServerComm.localUserNameProvider = new LocalUserNameProvider();
```

`ILocalUserNameProvider` 구현체:
- `getLocalConfigUserName()`: config 변경 패킷에 사용할 로컬 유저명
- `getLocalSpeedUserName()`: speed 변경 패킷에 사용할 로컬 유저명

`SmartMovingServerComm`에서 `processConfigChangePacket`/`processSpeedChangePacket` 수신 시 `localUserNameProvider.getLocalConfigUserName()/getLocalSpeedUserName()` 호출.

### 1.21.1 대응
- `LocalUserNameProvider` 구현체: `MinecraftClient.getInstance().getSession().getUsername()` 등으로 교체
- 두 메서드가 같은 값을 반환할 가능성 높음 (인터페이스 분리는 설계적 여지)

---

## 전체 포팅 난이도 요약

| 항목 | 난이도 | 이유 |
|------|--------|------|
| 네트워크 채널 등록 | ★★☆☆☆ | FML → Fabric API 교체, 구조는 유사 |
| 패킷 직렬화 (ObjectOutputStream → PacketByteBuf) | ★★★☆☆ | String[] 직접 직렬화 구현, 포맷 재정의 |
| SmartMovingServerComm (순수 디스패처) | ★☆☆☆☆ | 로직 없음, API 교체만 필요 |
| SmartMovingComm (클라이언트 수신 + processConfigPacket) | ★★★☆☆ | 설정 상태 관리 복잡, processBlockCode 채팅 훅 |
| SmartMovingServer (소진 인터셉트) | ★★★★☆ | addExhaustion/addMovementStat 훅 구조 복잡, depth 카운터 관리 |
| resetTicksForFloatKick | ★★★★☆ | floatingTicks Mixin accessor 필수, 타이밍 정확해야 함 |
| sendPacketToTrackedPlayers (릴레이) | ★★★☆☆ | 1.21.1 PlayerLookup API 재조사 필요 |
| 상태 패킷 long 33비트 구조 | ★★☆☆☆ | 비트 연산 그대로 이식 가능 |
| PlayerAPI → Mixin 변환 (hook 구조) | ★★★★★ | before/after/local/override hook 전체를 Mixin으로 재구현 |
| 설정 배포 프로토콜 | ★★★☆☆ | 상태 전환 로직 복잡 (first/재설정 분기, Config = ServerConfig 전환) |
| processBlockCode (채팅 코드 설정) | ★★☆☆☆ | Fabric 채팅 이벤트 훅으로 대체 가능 |
| 소진 before/afterUpdatePotionEffects 역전 | ★★★☆☆ | 의도적 역전 로직, 1.21.1 포션 업데이트 타이밍 재확인 필요 |

### 포팅 최우선 과제

1. **PlayerAPI → Mixin 전환**: 모든 hook 메서드(`beforeXxx`, `afterXxx`, `localXxx`, override)를 Mixin으로 재구현. `SmartMovingPlayerBase`와 `SmartMovingServerPlayerBase`의 메서드 전체 목록이 Mixin 작성 체크리스트.

2. **resetTicksForFloatKick**: `ServerPlayNetworkHandler.floatingTicks` (`field_14138`)를 Mixin accessor로 접근해 0으로 리셋. 미구현 시 클라이밍 중 80틱 후 kick.

3. **소진 인터셉트**: `addExhaustion()` + `addMovementStat()` + `updatePotionEffects()` 전후 hook 구조를 Mixin으로 재현. `depth` 카운터와 `withinOnLivingUpdate` 플래그 모두 정확히 유지 필요.

4. **서버 상태 패킷 릴레이**: 클라이언트→서버 상태 패킷을 서버가 추적 플레이어에게 릴레이. Fabric `PlayerLookup.tracking()` API 활용.

5. **설정 배포**: `SmartMovingServer.initialize()` → `sendConfigContent()` → 클라이언트 `processConfigPacket()` 흐름 재현. `Config = ServerConfig` 전환 상태 관리 유지.

### 권장 포팅 순서

1. 패킷 직렬화 레이어 재구현 (`PacketByteBuf` 기반)
2. 채널 등록 + 서버/클라이언트 수신 핸들러 등록
3. `SmartMovingServer` 클래스 이식 (소진 인터셉트 포함)
4. `resetTicksForFloatKick` Mixin 구현
5. `sendPacketToTrackedPlayers` 릴레이 구현
6. 클라이언트 수신 처리 (`SmartMovingComm`, `processConfigPacket`)
7. 설정 배포 프로토콜 통합 테스트

---

## 15. DataTracker 동기화 설계 — R-15 (C-05, C-06)

### 15-1. 배경 — 원본 SM 상태 동기화 구조

원본 SM의 타인 플레이어 상태 동기화는 State 패킷 릴레이 방식:

```
로컬 클라이언트: SmartMovingSelf → State 패킷(33비트 long) C→S
서버: SmartMovingServer.processStatePacket()
  → 일부 비트 추출(isCrawling bit13, isSmall bit15 등) → 서버 처리
  → mp.sendPacketToTrackedPlayers(packet) → 원본 패킷 그대로 릴레이
다른 클라이언트: SmartMovingOther.processStatePacket(state) → 모든 상태 필드 수동 설정
```

1.21.1에서 `SmartMovingOther` 패턴을 그대로 이식하거나, 일부 상태를 DataTracker로 대체하는 두 방식이 가능하다. R-15는 `isCrawling`과 `isSliding`에 대해 DataTracker 방식 적합성을 분석한다.

---

### 15-2. C-05 — isCrawling DataTracker 동기화 (확인됨)

#### 원본 동기화 경로 (코드 근거)

```java
// 1) 클라이언트 인코딩 (SmartMovingSelf.addToSendQueue)
state <<= 1; state |= isCrawling ? 1 : 0;   // → bit 13

// 2) 서버 추출 (SmartMovingServer.processStatePacket)
boolean isCrawling = (state >>> 13) & 1) != 0;
setCrawling(isCrawling);                      // hitbox 갱신: crawlingCooldown=10 or 0

// 3) 서버 릴레이
mp.sendPacketToTrackedPlayers(packet);

// 4) 다른 클라이언트 (SmartMovingOther.processStatePacket — bit 13)
isCrawling = (state & 1) != 0;               // bit 13 추출 후 직접 필드 설정
```

#### DataTracker 방식 설계

**서버가 State 패킷에서 bit 13을 추출하여 DataTracker에 설정**:

```java
// DataTracker TrackedData 등록 (Mixin static 초기화)
@Mixin(PlayerEntity.class)
public abstract class PlayerEntitySmMixin {
    private static final TrackedData<Boolean> SM_CRAWLING =
        DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void smInitDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(SM_CRAWLING, false);
    }
}
```

```java
// 서버측 State 패킷 처리 (SmartMovingServer.processStatePacket 대응)
boolean isCrawling = ((state >>> 13) & 1) != 0;
serverPlayer.getDataTracker().set(SM_CRAWLING, isCrawling);
// → MC가 자동으로 이 플레이어를 추적 중인 모든 클라이언트에 DataTracker 패킷 전송
```

```java
// 다른 클라이언트 렌더링 (SmartMovingOther 대응 코드)
boolean isCrawling = otherPlayer.getDataTracker().get(SM_CRAWLING);
// → SmartMovingOther.processStatePacket의 isCrawling 필드 수동 설정 대체
```

#### 충돌 분석

| 충돌 대상 | 충돌 여부 | 근거 |
|-----------|-----------|------|
| `PlayerEntity.initDataTracker()` | **없음** | `@TAIL` 주입 — vanilla `builder.add()` 완료 후 추가. B-16 확인 |
| DataTracker ID 충돌 | **없음** | `registerData()`가 `CLASS_TO_LAST_ID` 카운터로 자동 증가. Mixin 호출 순서로 중복 없음. B-16 확인 |
| `sendPacketToTrackedPlayers` 릴레이 | **대체 가능** | DataTracker 자동 전파로 isCrawling에 대한 수동 릴레이 불필요 |

#### State 패킷 릴레이와의 관계

- State 패킷 자체는 여전히 필요 (isClimbing, isSliding 등 나머지 30+ 비트 상태 때문)
- `sendPacketToTrackedPlayers` 릴레이도 여전히 필요 (다른 상태 동기화를 위해)
- DataTracker 방식은 `isCrawling`에 대해 "추가 동기화 경로"를 제공하는 것 — 릴레이 방식과 병렬로 동작하거나 isCrawling을 State 패킷에서 제거하고 DataTracker로만 전달 가능

**결론**: isCrawling DataTracker 동기화 **적합**. 서버가 이미 bit 13을 처리하므로 DataTracker.set() 1줄 추가로 자동 전파 가능. 타인 플레이어 렌더 시 `SmartMovingOther` 맵 조회 대신 `otherPlayer.dataTracker.get(SM_CRAWLING)` 직접 접근.

---

### 15-3. C-06 — isSliding DataTracker 필요 여부 (확인됨)

#### 원본 동기화 경로 (코드 근거)

```java
// 1) 클라이언트 인코딩 (SmartMovingSelf.addToSendQueue)
state <<= 1; state |= isSliding ? 1 : 0;    // → bit 21

// 2) 서버 (SmartMovingServer.processStatePacket)
// isSliding 미추출 — 서버는 isSliding 처리 없음 (A-14 확인)

// 3) 서버 릴레이
mp.sendPacketToTrackedPlayers(packet);       // 원본 패킷 그대로 릴레이

// 4) 다른 클라이언트 (SmartMovingOther.processStatePacket — bit 21)
isSliding = (state & 1) != 0;               // bit 21 추출 후 직접 필드 설정
```

`isSliding`은 서버 물리 처리 없음 (A-14: "서버는 isSliding을 processStatePacket에서 추출하지 않음 — 서버 물리에 불필요"). 렌더링 전용.

#### DataTracker 방식 적용 시 필요 변경

서버가 `isSliding`을 DataTracker에 설정하려면:
1. 서버의 `processStatePacket`에서 bit 21 추출 추가 (현재 없음)
2. `player.getDataTracker().set(SM_SLIDING, isSliding)` 설정 추가

```java
// 서버 processStatePacket에 추가 필요 (신규)
boolean isSliding = ((state >>> 21) & 1) != 0;
serverPlayer.getDataTracker().set(SM_SLIDING, isSliding);
```

**DataTracker 방식의 장점**:
- `SmartMovingOther` 패턴 없이 `otherPlayer.dataTracker.get(SM_SLIDING)` 직접 접근
- isCrawling과 동일한 패턴 — 일관성

**DataTracker 방식의 단점**:
- 서버에 bit 21 추출 코드 추가 필요 (서버 물리에는 불필요한 처리)
- State 패킷 릴레이로 이미 동작하는 것을 변경

#### 결론: isSliding DataTracker 방식 채택

- State 패킷 릴레이와 DataTracker를 병행 사용하는 것은 복잡도 증가
- isCrawling과 동일 패턴을 적용하면 `SmartMovingOther` 맵 의존 제거 가능 (일관성)
- 서버 추가 코드는 bit 21 추출 1줄 + DataTracker.set() 1줄로 최소
- **결정**: isSliding도 DataTracker 방식 채택. 서버가 bit 21 추출 후 `SM_SLIDING` DataTracker 설정

---

### 15-4. 두 상태 DataTracker 종합 설계

#### TrackedData 등록

```java
@Mixin(PlayerEntity.class)
public abstract class PlayerEntitySmStateMixin {

    // 서버가 설정 → MC 자동 전파 → 다른 클라이언트 렌더링에 사용
    static final TrackedData<Boolean> SM_CRAWLING =
        DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    static final TrackedData<Boolean> SM_SLIDING =
        DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void smInitDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(SM_CRAWLING, false);
        builder.add(SM_SLIDING, false);
    }
}
```

- `registerData()`: static 필드 → 클래스 로드 시 1회 실행. `PlayerEntity.class` 지정 필수 (B-16: "Mixin target이 PlayerEntity.class인 경우 registerData(PlayerEntity.class, ...)")
- `initDataTracker @TAIL`: vanilla Builder.add() 완료 후 추가 → ID 순서 충돌 없음

#### 서버 State 패킷 처리 (SmartMovingServer 대응)

```java
// 서버 수신 processStatePacket 내부
boolean isCrawling = ((state >>> 13) & 1) != 0;
boolean isSliding  = ((state >>> 21) & 1) != 0;

// 기존 서버 처리 (setCrawling, setSmall 등) 그대로 유지
setCrawling(isCrawling);

// DataTracker 설정 — MC가 자동으로 추적 중인 모든 클라이언트에 전파
serverPlayer.getDataTracker().set(SM_CRAWLING, isCrawling);
serverPlayer.getDataTracker().set(SM_SLIDING, isSliding);
```

#### 다른 클라이언트 렌더링 접근

```java
// SmartMovingOther 대응 렌더 코드에서 (타인 플레이어 렌더링)
// 기존: SmartMovingFactory 맵에서 SmartMovingOther 조회 후 isCrawling/isSliding 접근
// 변경: DataTracker 직접 접근

boolean otherCrawling = otherPlayer.getDataTracker().get(PlayerEntitySmStateMixin.SM_CRAWLING);
boolean otherSliding  = otherPlayer.getDataTracker().get(PlayerEntitySmStateMixin.SM_SLIDING);
```

MC의 DataTracker 동기화가 자동으로 처리되므로 `sendPacketToTrackedPlayers`의 isCrawling/isSliding 부분은 DataTracker가 대체. 단, 나머지 상태 비트(isClimbing, isSwimming 등)를 위한 State 패킷 릴레이는 여전히 필요.

#### 로컬 플레이어 자신의 상태 설정

DataTracker는 서버 권한 — 클라이언트가 `localPlayer.dataTracker.set(SM_CRAWLING, true)` 직접 호출 시 로컬에만 반영, 서버로 전송되지 않음. 따라서:

- **로컬 플레이어의 isCrawling**: `SmartMovingSelf`의 `isCrawling` 필드를 그대로 사용 (로컬 물리/렌더용)
- **DataTracker의 SM_CRAWLING**: 서버가 설정 → 다른 클라이언트 렌더링용

즉, 로컬 플레이어는 `SmartMovingSelf.isCrawling`으로 렌더링하고, 타인 플레이어는 `otherPlayer.dataTracker.get(SM_CRAWLING)`으로 렌더링.

---

### 15-5. State 패킷 릴레이와 DataTracker 병행 방식 충돌

State 패킷 릴레이 (`sendPacketToTrackedPlayers`)와 DataTracker 전파가 동시에 동작하면 다른 클라이언트에서:
1. DataTracker 패킷 수신 → `SM_CRAWLING = true`
2. State 패킷 수신 → `SmartMovingOther.processStatePacket(state)` → `isCrawling = true` (수동 설정)

두 경로가 병행되면 isCrawling/isSliding이 두 번 설정될 수 있음. 값이 동일하므로 최종 결과는 동일하지만, **SmartMovingOther가 DataTracker 값을 사용하도록 변경하면** 수동 설정 코드 제거 가능.

**권장 방식**: State 패킷 릴레이 유지 (다른 상태 비트 때문에 필수), `SmartMovingOther.processStatePacket()`에서 isCrawling/isSliding 설정 라인만 제거하고 DataTracker를 신뢰.

또는: `SmartMovingOther.processStatePacket()`에서 DataTracker 값으로 덮어쓰기 허용 (무해, 동일 값).

---

### 15-6. 미확인 항목

| ID | 내용 | 이유 |
|----|------|------|
| M-14 | `DataTracker.get()` 접근 시 `static` TrackedData 참조 — 다른 클래스의 Mixin static 필드를 렌더 코드에서 접근하는 패턴 | Mixin static 필드의 접근성 제한(private/package) 확인 필요. `accessor` 인터페이스 또는 package-private 으로 공개 필요 여부 미확인. |

M-14는 구현 시점에서 접근자 설계로 해소 가능 (Mixin accessor 인터페이스 또는 별도 유틸 클래스에 TrackedData 보관). 청크 추가 불필요.

---

### C-05, C-06 완료 요약

| 항목 | 결정 | 근거 |
|------|------|------|
| isCrawling 동기화 방식 | DataTracker (`SM_CRAWLING` BOOLEAN) | 서버가 이미 bit 13 처리 → DataTracker.set() 추가로 자동 전파 |
| isSliding 동기화 방식 | DataTracker (`SM_SLIDING` BOOLEAN) | 일관성 + 서버 bit 21 추출 최소 변경으로 전환 가능 |
| SmartMovingOther 수동 설정 | 렌더 코드를 DataTracker로 전환하면 제거 가능 | State 패킷 릴레이 자체는 유지 (다른 상태 때문) |
| registerData 대상 클래스 | `PlayerEntity.class` | B-16: Mixin target과 동일 클래스 지정 필수 |
| initDataTracker 진입점 | `@Inject @TAIL` | B-16: 부모 Builder.add() 완료 후 추가 → 순서 보장 |
| 로컬 플레이어 렌더 | `SmartMovingSelf.isCrawling/isSliding` | DataTracker는 서버 권한 — 클라이언트 자체 set 불가 |
| 타인 플레이어 렌더 | `otherPlayer.dataTracker.get(SM_CRAWLING/SLIDING)` | DataTracker 자동 동기화 활용 |
