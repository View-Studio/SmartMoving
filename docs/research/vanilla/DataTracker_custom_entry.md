# DataTracker 커스텀 TrackedData / ClientReceiveMessageEvents — R-11 리서치

소스: Fabric Loom 디컴파일 (Vineflower 1.11.1)  
Yarn: `net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2`  
확인 파일:
- `common-unpicked.jar` → `DataTracker.class`, `DataTracker$Builder.class`, `TrackedDataHandlerRegistry.class`
- `fabric-api-0.116.11+1.21.1.jar` → `fabric-message-api-v1-0.116.11.jar` → `ClientReceiveMessageEvents.class`

---

## B-16 — DataTracker 커스텀 TrackedData 추가 패턴 (확인됨)

### 1. ID 등록: `DataTracker.registerData()`

```java
// DataTracker.java
public static <T> TrackedData<T> registerData(
    Class<? extends DataTracked> entityClass,
    TrackedDataHandler<T> dataHandler
) {
    int i = CLASS_TO_LAST_ID.put(entityClass);
    if (i > 254) {
        throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
    }
    return dataHandler.create(i);
}
```

- `CLASS_TO_LAST_ID`: 클래스별 자동 증가 카운터
- 최대 ID: **254** (255 이상이면 IllegalArgumentException)
- **반드시 클래스 static 초기화 시점에 호출** (클래스 로드 시 1회)

### 2. 기본값 등록: `DataTracker.Builder`

```java
// DataTracker$Builder.java
public class DataTracker$Builder {
    public DataTracker$Builder(DataTracked entity) {
        this.entries = new Entry[DataTracker.CLASS_TO_LAST_ID.getNext(entity.getClass())];
    }

    public <T> DataTracker$Builder add(TrackedData<T> data, T value) {
        int i = data.id();
        // i > entries.length → IllegalArgumentException
        // entries[i] != null → IllegalArgumentException (중복)
        // TrackedDataHandlerRegistry.getId(data.dataType()) < 0 → IllegalArgumentException (미등록 핸들러)
        this.entries[data.id()] = new Entry(data, value);
        return this;
    }

    public DataTracker build() {
        // entries 중 null 있으면 IllegalStateException
        return new DataTracker(this.entity, this.entries);
    }
}
```

- `Builder.add()` 조건:
  - `data.id()` 가 entries 크기 이내여야 함
  - 중복 ID 불허
  - **`TrackedDataHandlerRegistry`에 등록된 핸들러만 허용**

### 3. 기본 핸들러: `TrackedDataHandlerRegistry`

```java
// TrackedDataHandlerRegistry.java (static 등록 순서)
// BYTE(0), INTEGER(1), LONG(2), FLOAT(3), STRING(4), TEXT_COMPONENT(5),
// OPTIONAL_TEXT_COMPONENT(6), ITEM_STACK(7), BOOLEAN(8), ROTATION(9), ...
// ENTITY_POSE(20), ...
public static final TrackedDataHandler<Boolean> BOOLEAN = TrackedDataHandler.create(PacketCodecs.BOOL);
```

SM에서 필요한 핸들러:
| 핸들러 | 용도 |
|--------|------|
| `BOOLEAN` | 클라이밍/크롤링 등 상태 플래그 |
| `BYTE` | 여러 상태를 비트마스크로 묶을 때 |
| `FLOAT` | 수치 상태 값 |

### 4. LivingEntity.initDataTracker() 패턴 (확인됨)

```java
// LivingEntity.java
protected void initDataTracker(Builder builder) {
    builder.add(LIVING_FLAGS, (byte)0);
    builder.add(POTION_SWIRLS, List.of());
    builder.add(POTION_SWIRLS_AMBIENT, false);
    builder.add(STUCK_ARROW_COUNT, 0);
    builder.add(STINGER_COUNT, 0);
    builder.add(HEALTH, 1.0F);
    builder.add(SLEEPING_POSITION, Optional.empty());
}
```

- `Entity.initDataTracker(Builder)` : **abstract** (`Entity.java:354`)
- 엔티티 생성자에서 `this.initDataTracker(builder); this.dataTracker = builder.build();` 호출

### 5. SM Mixin 패턴

```java
// SM 커스텀 TrackedData 추가 (Mixin)
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    
    // static 필드 — 클래스 로드 시 ID 자동 할당
    private static final TrackedData<Boolean> CLIMBING = DataTracker.registerData(
        PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> CRAWLING = DataTracker.registerData(
        PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void smInitDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(CLIMBING, false);
        builder.add(CRAWLING, false);
    }
}
```

- `@At("TAIL")`: 부모 클래스의 `builder.add()` 완료 후 추가 → 순서 보장
- `registerData` 중복 호출 금지: static 필드로 1회만 실행
- Builder 크기는 `CLASS_TO_LAST_ID.getNext(entity.getClass())` — Mixin이 registerData를 먼저 호출하면 자동으로 크기 확장됨

### 6. 주의 사항

- `registerData()` 는 **반드시 Mixin이 적용된 클래스 이름으로** 호출해야 함 (debug 로그에서 검사)
- Mixin target이 `PlayerEntity.class`인 경우 `registerData(PlayerEntity.class, ...)` — 별도 인터페이스 클래스 사용 금지
- `set(data, value)` / `get(data)` : 클라이언트/서버 모두 dataTracker에서 직접 접근
  ```java
  this.dataTracker.set(CLIMBING, true);
  boolean climbing = this.dataTracker.get(CLIMBING);
  ```

---

## B-19 — ClientReceiveMessageEvents (확인됨)

소스: `fabric-api-0.116.11+1.21.1.jar` → `fabric-message-api-v1-0.116.11.jar` → Vineflower 디컴파일

### 이벤트 목록

```java
// ClientReceiveMessageEvents.java (net.fabricmc.fabric.api.client.message.v1)
@Environment(EnvType.CLIENT)
public final class ClientReceiveMessageEvents {
    
    // 게임/시스템 메시지 허용 여부 결정 (false 반환 시 표시 억제)
    public static final Event<AllowGame> ALLOW_GAME;
    
    // 게임/시스템 메시지 내용 수정
    public static final Event<ModifyGame> MODIFY_GAME;
    
    // 게임/시스템 메시지 수신 (알림용, 취소 불가)
    public static final Event<Game> GAME;
    
    // 채팅 메시지 허용 여부 결정
    public static final Event<AllowChat> ALLOW_CHAT;
    
    // 채팅 메시지 수신 (알림용, 취소 불가)
    public static final Event<Chat> CHAT;
    
    // 취소된 메시지 알림
    public static final Event<GameCanceled> GAME_CANCELED;
    public static final Event<ChatCanceled> CHAT_CANCELED;
}
```

### 핵심 인터페이스 시그니처

```java
// 게임/시스템 메시지 수신
@FunctionalInterface
public interface Game {
    void onReceiveGameMessage(Text message, boolean overlay);
    // overlay: true면 핫바 위 액션바(action bar)에 표시
}

// 채팅 메시지 수신
@FunctionalInterface
public interface Chat {
    void onReceiveChatMessage(
        Text message,
        @Nullable SignedMessage signedMessage,   // 서명된 메시지 (null 가능)
        @Nullable GameProfile sender,            // 발신자 프로필 (null 가능)
        MessageType.Parameters params,           // 메시지 타입 파라미터
        Instant receptionTimestamp               // 수신 시각
    );
}

// 게임 메시지 수정
@FunctionalInterface
public interface ModifyGame {
    Text modifyReceivedGameMessage(Text message, boolean overlay);
}
```

### § 코드 접근 가능 여부 (확인됨)

```java
// Text.java
default String getString() {
    return super.getString();   // StringVisitable 기본 구현: 포맷 코드 없이 평문만 반환
}
default String asTruncatedString(int length) {
    // 평문 길이 제한 버전
}
```

**결론: `Text.getString()`은 § 포맷 코드 없이 평문(plain text)만 반환한다.**

1.21.1에서 서버→클라이언트 메시지는 JSON Text 구조로 전송되며, § 코드는 `Style` 객체(color, bold 등)로 파싱됨. 원본 § 문자열은 Text 객체에 보존되지 않음.

단, 서버가 LiteralText로 `"§aHello"` 그대로 전송한 경우 `getString()`이 `"§aHello"` 반환 가능. 이는 서버 구현에 따라 다름 — 보장되지 않음.

### SM에서의 사용 패턴

```java
// 서버 명령/상태 메시지 인터셉트 예시
ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
    String text = message.getString();
    if (text.contains("/sml")) {
        // SM 관련 메시지 처리
    }
});
```

SM 원본 1.7.10의 채팅 커맨드 인터셉트는 1.21.1에서:
- 클라이언트→서버 명령: **Fabric Commands API** (`CommandRegistrationCallback`) 사용 권장
- 서버→클라이언트 알림: `ClientReceiveMessageEvents.GAME` + `message.getString()` 으로 평문 파싱

---

## 전체 요약

| 항목 | 확인 결과 |
|------|----------|
| `registerData()` 위치 | `DataTracker` static 메서드 |
| Builder 생성 | `Entity` 생성자 → `initDataTracker(Builder)` 호출 → `builder.build()` |
| Mixin 진입점 | `@Inject(method = "initDataTracker", at = @At("TAIL"))` |
| BOOLEAN 핸들러 | `TrackedDataHandlerRegistry.BOOLEAN` |
| 최대 TrackedData ID | 254 (초과 시 IllegalArgumentException) |
| GAME 이벤트 파라미터 | `(Text message, boolean overlay)` |
| CHAT 이벤트 파라미터 | `(Text, SignedMessage, GameProfile, MessageType.Parameters, Instant)` |
| § 코드 raw 접근 | **불가** — `getString()`은 평문만 반환 |
