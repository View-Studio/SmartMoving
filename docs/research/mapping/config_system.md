# Config 시스템 + 서버 설정 배포 프로토콜 교차 분석 — R-14

소스: 원본 SmartMoving 1.7.10 Forge 소스 (docs/research/original/smartmoving/config/ + moving/)  
대상: 1.21.1 Fabric 구현 설계  
RESEARCH_RULES.md: 추정 없음. 확인된 것만 기록.

---

## 1. 원본 Config 계층 구조 (1.7.10)

```
SmartMovingProperties           (net.smart.properties — Property<T> 컨테이너, 설정 파일 R/W)
  └─ SmartMovingConfig          (모든 설정 Property 필드 + 버전 관리 + key/toggle 시스템)
       ├─ SmartMovingClientConfig   (이동 로직 API: isXxxEnabled(), getJumpExhaustionGain(), getFactor())
       │    ├─ SmartMovingOptions   (클라이언트 최상위: 키 바인딩, UI, 파일 저장 경로, toggle() 채팅 출력)
       │    └─ SmartMovingServerConfig   (서버 수신 어댑터: flat String[] → Properties 파싱)
       └─ SmartMovingServerOptions  (서버 관리자: 플레이어별 key/속도, toggle/changeSpeed, writeToProperties() 직렬화)
```

### 싱글톤 전역 변수 (SmartMovingContext)

```java
public static final SmartMovingOptions Options = new SmartMovingOptions();
public static final SmartMovingServerConfig ServerConfig = new SmartMovingServerConfig();
public static SmartMovingClientConfig Config = Options;  // non-final — 전환됨
```

- 초기값: `Config = Options` (클라이언트 자체 설정 사용)
- 서버 설정 첫 수신: `Config = ServerConfig` (서버 설정 우선)
- 서버 설정 해제: `Config = Options` (클라이언트 자체 설정 복원)

---

## 2. 서버→클라이언트 설정 배포 흐름 (확인됨)

### 2-1. 서버 측 전송 (SmartMovingServer.initialize)

```java
// 플레이어 접속 시 호출
if(Options._globalConfig.value)
    // 전역 설정: 모든 플레이어에게 동일한 설정 전송
    SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(), null);
else if(Options._serverConfig.value)
    // 개인 설정: 플레이어별 설정 전송 (toggle=false → 현재 key 유지)
    SmartMovingPacketStream.sendConfigContent(mp, optionsHandler.writeToProperties(mp, false), null);
else if(alwaysSendMessage)
    // SM 비활성 알림: enabled → new String[0], !enabled → null
    SmartMovingPacketStream.sendConfigContent(mp, Options.enabled ? new String[0] : null, null);
```

### 2-2. writeToProperties 직렬화 (M-12 확인됨)

`SmartMovingServerOptions.writeToProperties(IEntityPlayerMP mp, String key)`:

**disabled 단축 경로**:
```java
if(key == null ? !config.enabled : key == SmartMovingProperties.Disabled)
    return new String[] { config._globalConfig.getCurrentKey(), config._globalConfig.getValueString() };
```
- `key == null && !config.enabled`: 전역 비활성 → `[globalConfigKey, "false"]` 2원소 반환
- `key == SmartMovingProperties.Disabled` (`"disabled"` 상수, == 비교): 해당 key 비활성 → 동일 2원소 반환

**정상 경로**:
```java
Properties properties = new Properties();
config.write(properties, key);          // key에 해당하는 설정을 Properties 맵에 기록

String[] result = new String[properties.size() * 2];
// flat 배열 [key1, val1, key2, val2, ...]
int i = 0;
while(keys.hasNext()) {
    Map.Entry<Object, Object> entry = keys.next();
    String propertyKey = result[i++] = entry.getKey().toString();
    // 플레이어별 속도 치환: 전역 speedUserExponent 대신 개인 속도 삽입
    if(mp != null && propertyKey.equals(speedUserExponentKey)) {
        Integer userExponent = config._speedUsersExponents.value.get(mp.getUsername());
        if(userExponent != null)
            entry.setValue(config._speedUserExponent.getValueString(userExponent));
    }
    result[i++] = entry.getValue().toString();
}
return result;  // [k1, v1, k2, v2, ...]
```

반환 형식: `String[]` flat 배열 `[key1, val1, key2, val2, ...]`  
이 형식이 `SmartMovingServerConfig.loadFromProperties(String[], boolean)` 파싱 입력과 1:1 대응.

### 2-3. 패킷 직렬화 (M-13 확인됨)

`SmartMovingPacketStream.sendConfigContent(IPacketSender comm, String[] content, String username)`:

```
writeByte(2) + writeObject(String[] content) + writeObject(String username)
```

전체 패킷 포맷 (Java ObjectOutputStream 기반):

| packetId | 상수 | 페이로드 |
|----------|------|---------|
| 0 | StatePacketId | `int(entityId)` + `long(state)` |
| 1 | ConfigInfoPacketId | `Object(String info)` |
| 2 | ConfigContentPacketId | `Object(String[] content)` + `Object(String username)` |
| 3 | ConfigChangePacketId | (없음) |
| 4 | SpeedChangePacketId | `int(difference)` + `Object(String username)` |
| 5 | HungerChangePacketId | `float(hunger)` |
| 6 | SoundPacketId | `Object(String soundId)` + `float(volume)` + `float(pitch)` |

- 모든 패킷: 첫 1바이트 = packetId
- `writeObject` = Java 직렬화 (ObjectOutputStream) — String, String[] 모두 Serializable
- 채널 ID: `SmartMovingInfo.ModComId` 최대 15자 자름 (`Id = modComId.substring(0, 15)`)

### 2-4. 클라이언트 수신 처리 (SmartMovingComm.processConfigPacket)

```java
processConfigPacket(String[] content, String username, boolean blockCode):
  // 1) content == [globalConfigKey, "true"/"false"] → isGloballyConfigured 추출, content = null
  // 2) first = (Config != ServerConfig)   // 첫 수신 여부
  // 3-a) content != null && length != 0   → ServerConfig.loadFromProperties(content, blockCode)
  // 3-b) content != null && length == 0   → Config = Options (서버 설정 해제), return
  // 3-c) content == null                  → ServerConfig.load(false) (top → 일반 덮어씌움)
  // 4) ServerConfig._globalConfig.value = isGloballyConfigured
  // 5-a) first=false → writeServerReconfigMessageToChat()
  // 5-b) first=true  → Config = ServerConfig, writeServerConfigMessageToChat()
  //      !blockCode  → SmartMovingPacketStream.sendConfigInfo(instance, _sm_current)
```

핵심 전환:
- `content.length == 0` (new String[0]): `Config = Options` — 서버가 SM을 클라이언트에 위임
- 첫 수신 (`first=true`): `Config = ServerConfig` — 서버 설정을 클라이언트에 강제

### 2-5. 블록 코드 파싱 (SmartMovingComm.processBlockCode)

```java
if(!text.startsWith("§0§1") || !text.endsWith("§f§f")) return false;
String codes = text.substring(4, text.length() - 4);
// §0→_baseClimb("standard"), §1→_freeClimb("false"), ..., §b→_angleJumpBack("false")
```

채팅 메시지로 SM 기능 12개를 on/off하는 서버 제어 메커니즘.  
1.21.1: `ClientReceiveMessageEvents.GAME` 이벤트 + `message.getString()` 평문 파싱 (B-19 확인).

---

## 3. 1.21.1 Fabric 대응 설계

### 3-1. Config 클래스 계층 단순화

1.7.10 계층 구조는 `net.smart.properties.Properties` 자체 시스템에 강하게 의존.  
1.21.1에서는 이 시스템을 그대로 포팅하지 않고, 핵심 필드와 API만 유지하며 단순화한다.

**유지할 것**:
- `SmartMovingConfig` → 설정 필드 POJO (boolean/float/int 필드)
- `SmartMovingClientConfig` → `isXxxEnabled()`, `getFactor()`, `getJumpExhaustionGain()` 등 API 메서드
- `SmartMovingOptions` → 클라이언트 파일 저장/로드, 키 바인딩
- `SmartMovingServerConfig` → 서버 수신 설정 파싱 어댑터
- `SmartMovingContext.Config` → non-final static, 전환 패턴 그대로 유지

**제거할 것**:
- `net.smart.properties.Properties` / `Property<T>` — vanilla `Properties` 또는 단순 Map으로 대체
- 버전 관리 시스템 (v0.1~v3.2 누적 배열) — 1.21.1 신규 시작이므로 불필요
- FML 로그 (`FMLLog`) → SLF4J (`LogManager.getLogger()`)

### 3-2. SmartMovingServerOptions — Fabric 대응

| 1.7.10 | 1.21.1 대응 |
|--------|------------|
| `IEntityPlayerMP` | `ServerPlayerEntity` |
| `FMLLog.info()` | `LOGGER.info()` (SLF4J) |
| `config._speedUsersExponents.value.get(mp.getUsername())` | `Map<UUID, Integer>` (username 대신 UUID) |
| `config._userConfigKeys.value.get(mp.getUsername())` | `Map<UUID, String>` |
| `synchronized` 메서드 | 동일하게 유지 (서버 멀티스레드) |
| `config.write(properties, key)` | 1.21.1 자체 직렬화 메서드로 구현 |
| `gameType` 판별 | `player.interactionManager.getGameMode()` |

### 3-3. 패킷 시스템 — Fabric Network API 대응

**채널 등록**:
```java
// 채널 ID (SmartMovingPacketStream.Id 대응)
public static final Identifier CONFIG_CONTENT_CHANNEL = new Identifier("smartmoving", "config_content");
public static final Identifier STATE_CHANNEL = new Identifier("smartmoving", "state");
public static final Identifier CONFIG_INFO_CHANNEL = new Identifier("smartmoving", "config_info");
public static final Identifier CONFIG_CHANGE_CHANNEL = new Identifier("smartmoving", "config_change");
public static final Identifier SPEED_CHANGE_CHANNEL = new Identifier("smartmoving", "speed_change");
public static final Identifier HUNGER_CHANGE_CHANNEL = new Identifier("smartmoving", "hunger_change");
public static final Identifier SOUND_CHANNEL = new Identifier("smartmoving", "sound");
```

**Java ObjectOutputStream → PacketByteBuf 직접 인코딩** (Java 직렬화 보안/성능 문제 제거):

| 1.7.10 | 1.21.1 PacketByteBuf 대응 |
|--------|--------------------------|
| `writeByte(packetId)` | 채널 ID로 packetId 구분 → 불필요 |
| `writeInt(entityId)` | `buf.writeInt(entityId)` |
| `writeLong(state)` | `buf.writeLong(state)` |
| `writeObject(String s)` | `buf.writeString(s)` |
| `writeObject(String[] arr)` | `buf.writeVarInt(arr.length)` + 각 `buf.writeString(s)` |
| `writeFloat(f)` | `buf.writeFloat(f)` |

**서버→클라이언트 전송**:
```java
// 서버
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(content != null ? content.length : -1);
if(content != null)
    for(String s : content) buf.writeString(s);
buf.writeString(username != null ? username : "");
ServerPlayNetworking.send(player, CONFIG_CONTENT_CHANNEL, buf);
```

**클라이언트 수신 등록**:
```java
// 클라이언트 초기화 시
ClientPlayNetworking.registerGlobalReceiver(CONFIG_CONTENT_CHANNEL, (client, handler, buf, responseSender) -> {
    int len = buf.readVarInt();
    String[] content = len >= 0 ? new String[len] : null;
    if(content != null) for(int i = 0; i < len; i++) content[i] = buf.readString();
    String username = buf.readString();
    client.execute(() -> SmartMovingComm.processConfigContentPacket(content, username));
});
```

**클라이언트→서버 전송** (ConfigInfo 전송: 버전 정보):
```java
PacketByteBuf buf = PacketByteBufs.create();
buf.writeString(info);
ClientPlayNetworking.send(CONFIG_INFO_CHANNEL, buf);
```

### 3-4. content null/length 의미 정리

| content 값 | 의미 | 처리 |
|------------|------|------|
| `null` | SM 완전 비활성 | `Config = Options` 유지, 알림 채팅 없음 |
| `new String[0]` (length=0) | SM 활성 but 클라이언트에 위임 | `Config = Options` (복원) |
| `[k1,v1,...]` (length > 0) | 서버 설정 전송 | `ServerConfig.loadFromProperties()` → `Config = ServerConfig` |
| `[globalConfigKey, "true"/"false"]` (length=2, 특수) | 전역 플래그만 | `isGloballyConfigured` 추출, `ServerConfig.load(false)` |

PacketByteBuf 직렬화 시: `null` → length=-1, `new String[0]` → length=0으로 구분.

### 3-5. 블록 코드 파싱 (1.21.1)

```java
// SmartmovingClient.onInitializeClient() 또는 ClientReceiveMessageEvents 등록
ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
    if(overlay) return;  // 액션바는 스킵
    String text = message.getString();
    SmartMovingComm.processBlockCode(text);
});
```

- `message.getString()` = § 코드 없는 평문 (B-19 확인)
- 원본 마커: `§0§1` (시작) + `§f§f` (끝) — `getString()` 결과에 § 코드 포함 여부 불확실
- **주의**: 서버가 LiteralText로 `"§0§1..."` 전송 시 `getString()`이 그대로 반환할 수 있음 (서버 구현 의존)
- 대안: 서버가 별도 커스텀 채널로 블록 코드 전송 → `ClientPlayNetworking`으로 수신 (더 안전)

---

## 4. M-12, M-13 해소 정리

### M-12 확인됨 (writeToProperties 구체 구현)

`writeToProperties(mp, key)` 정상 경로:
1. `config.write(properties, key)` — key에 해당하는 모든 설정 값을 Properties 맵에 기록
2. `String[] result = new String[properties.size() * 2]` — flat 배열 할당
3. 이터레이터 순회: `result[i++] = key`, `result[i++] = value`
4. 플레이어별 속도 치환: `_speedUsersExponents.value.get(mp.getUsername())` → null 아니면 덮어씌움
5. 반환: `[k1, v1, k2, v2, ...]` flat String 배열

disabled 경로: `[globalConfigKey, globalConfigValueString]` 2원소 배열 반환.

### M-13 확인됨 (패킷 직렬화 형식)

원본 직렬화: Java `ObjectOutputStream` — `writeByte(id)` + `writeObject(String[])` + `writeObject(String)`  
1.21.1 대응: Fabric `PacketByteBuf` — 각 패킷을 별도 채널로 분리, `writeVarInt/writeString/writeLong/writeFloat`으로 직접 인코딩.

---

## 5. 구현 시 주의 사항

1. **`Config = ServerConfig` 전환 동기화**: 1.21.1에서도 `SmartMovingContext.Config`를 `volatile` 또는 `AtomicReference`로 선언하여 클라이언트 렌더/틱 스레드 간 가시성 보장.

2. **플레이어 식별자**: 원본은 `getUsername()` (String). 1.21.1에서는 `player.getUuid()` (UUID) 사용. Map 키를 UUID로 변경하면 이름 변경에도 안전.

3. **`_globalConfig` / `_serverConfig` 플래그**: 원본에서 서버 설정 적용 여부를 제어하는 핵심 플래그. 1.21.1에서도 두 플래그를 유지하고 `initialize()` 분기 로직을 그대로 이식.

4. **Java 직렬화 제거**: `ObjectOutputStream`/`ObjectInputStream`은 보안 취약점(역직렬화 공격), 성능 오버헤드(스트림 헤더), 버전 호환성 문제가 있음. `PacketByteBuf` 직접 인코딩으로 교체.

5. **`config.write(properties, key)`**: 원본 `net.smart.properties.Properties` 시스템 의존. 1.21.1에서는 설정 필드들을 `Map<String, String>`에 직접 쓰는 메서드로 대체 구현.

6. **블록 코드 대안**: § 코드가 `Text.getString()`에 보존되지 않을 경우, 서버가 커스텀 채널(`smartmoving:block_code`)로 직접 전송하는 방식으로 전환. 원본 채팅 인터셉트 방식은 호환성 보장 안 됨.
