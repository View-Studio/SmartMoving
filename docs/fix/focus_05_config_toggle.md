# Focus #5 — 옵션토글 4단계 순환 (disabled / easy / medium / hard)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스 #5.
> 이 문서 + playtest_fixes.md 2개만 열고 작업한다.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 진행 중 |
| 현재 단계 | ✅ A~F + G-1/G-3/G-4 완료 / ⏳ G-2 사용자 수동 검증 대기 / G-5 대기 |
| 잔여 섹션 | G-2(수동 테스트, **사용자 몫**) + G-5(포커스 전환, G-2 후) |
| 이전 판단 오류 | ⚠️ 기록됨 — 2-"이전 판단 오류" 참조 / checklist_original_audit.md L1096 에도 명시 정정 |
| 컴파일 상태 | ✅ 빌드 성공 (G-1 확인) / 회귀 감사 통과 (G-3) |

---

## 2. 증상 — 원본 vs 현재

### 원본 (1.7.10)

`configToggle` 키바인딩 또는 `/smoving config toggle` 커맨드 → **4상태 순환**:
```
disabled → easy(e) → medium(m) → hard(h) → disabled → ...
```

### 현재 1.21.1 구현

```java
// SmartMovingConfig.toggle()
public void toggle() {
    enabled = !enabled;   // ← 2상태(on/off)만
    save();
}
```

### 이전 판단 오류 (내 오판 기록)

이전 세션에서 **"단일 config key 시스템(1.21.1 'default' key 단순 on/off 토글)이
원본 `toggler==0 ↔ -1` 순환과 기능 동등"** 이라고 판단했지만 **틀렸음**.

원본은 `configKeys = {"e", "m", "h"}` 길이 3 배열 + `toggler` 순환으로 4상태를
구현하며, 단일 key(`{"c"}` creative) 때만 on/off 등가. 게임타입이 survival 또는
adventure(기본)이면 4상태.

---

## 3. 재현 케이스 표

| # | 시나리오 | 원본 기대 | 현재 실제 | 원본 라인 근거 |
|---|---------|---------|----------|--------------|
| 1 | Survival 월드 진입 + configToggle 키 누름 × 1회 | `disabled → easy` 메시지 | `enabled=true` 토글 (on/off) | `SmartMovingProperties.md` L325-L332 |
| 2 | easy 상태에서 configToggle 키 × 1회 | `easy → medium` | `enabled=false` | 동일 |
| 3 | medium 상태에서 configToggle 키 × 1회 | `medium → hard` | `enabled=true` | 동일 |
| 4 | hard 상태에서 configToggle 키 × 1회 | `hard → disabled` | `enabled=false` | 동일 |
| 5 | Creative 월드 + configToggle 키 | `disabled ↔ creative(c)` 2상태 | 동일 (우연히 맞음) | `_creativeConfigKeys={"c"}` 길이 1 |

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | 없음 |
| 이 포커스가 영향 주는 대상 | #6 (speed change — `enabled` 체크 경로 공유) |
| 영향받는 완료 이식 | `SmartMovingServer.adminToggleConfig` (이미 `INSTANCE.toggle()` 호출 중 — 자동 반영) |
| 영향받는 번역 키 | `smartmoving.message.config.client.enabled` / `.disabled` → 4상태 분화 필요 |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 파일 | 줄 | 내용 |
|------|---|------|
| `docs/research/original/smartmoving/config/SmartMovingConfig.md` | L610-L620 | `_survivalConfigKeys` / `_creativeConfigKeys` / `_adventureConfigKeys` / `_configKeyName` 선언 |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | L325-L332 | `toggle()` 본체 |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | L345-L355 | `setKeys()` 본체 |
| `docs/research/original/smartmoving/config/SmartMovingOptions.md` | — | `toggle()` override / `initializeForGameIfNeccessary()` — **미확보** |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | — | `setCurrentKey` / `getCurrentKey` / `getNextKey` / `hasKey` / `update` — **부분 확보** |

### 5.2. 이미 확보된 원본 Java 코드

**A. SmartMovingConfig.java L610-L620 — 필드 선언**
```java
public final Property<String[]> _survivalConfigKeys
    = Strings(...).singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _survivalDefaultConfigKey
    = String(...).singular().defaults("m");

public final Property<String[]> _creativeConfigKeys
    = Strings(...).singular().defaults(new String[]{"c"}).defaults(new String[0], _pre_sm_2_3);
public final Property<String>   _creativeDefaultConfigKey
    = String(...).singular().defaults("c").defaults("", _pre_sm_2_3);

public final Property<String[]> _adventureConfigKeys
    = Strings(...).singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _adventureDefaultConfigKey
    = String(...).singular().defaults("m");

public final Property<String> _configKeyName
    = String(...).defaults(Value((String)null).e("Easy").m("Medium").h("Hard"));
```

**B. SmartMovingProperties.java L325-L332 — toggle()**
```java
public void toggle() {
    int length = keys == null ? 0 : keys.length;
    toggler++;
    if (toggler == length)
        toggler = -1;
    update();
}
```

**C. SmartMovingProperties.java L345-L355 — setKeys()**
```java
public void setKeys(String[] keys) {
    if (keys == null || keys.length == 0)
        keys = _defaultKeys;   // 단순 on/off 모드 (length 1)
    this.keys = keys;
    toggler = 0;               // 항상 첫 key 로 초기화
    update();
}
```

**D. 순환 패턴**
- `keys.length == 3` (survival/adventure, "e"/"m"/"h"): `0 → 1 → 2 → -1 → 0 → ...`
- `keys.length == 1` (creative, "c"): `0 → -1 → 0 → ...` (on/off)
- `keys == _defaultKeys` (게임타입 모름/override): 동일

### 5.3. 확보 필요 (WebFetch 대상)

A 섹션 원자 작업으로 처리. 아래는 Agent 에 넘길 프롬프트:

```
SmartMoving 1.7.10 원본에서 다음 정보를 덤프. 원본 Java 코드 그대로.

대상 URL:
  - https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/properties/Property.java
  - https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/moving/config/SmartMovingOptions.java

추출 대상:
  1. Property.setCurrentKey(String) / getCurrentKey() / getNextKey(String) / hasKey(String) / update() 본체
  2. SmartMovingOptions.toggle() override 본체
  3. SmartMovingOptions.initializeForGameIfNeccessary() 본체
  4. _defaultKeys 값 (상수 확인)

응답 형식:
  - `// --- <파일명> L<start>-L<end> ---` 표기
  - 원본 그대로 (해석 금지)
```

---

## 6. 1:1 매핑 테이블

| 원본 (1.7.10) | 1.21.1 포트 | 비고 |
|---|---|---|
| `Property<String[]> _survivalConfigKeys` | `String[] SmartMovingConfig.configKeys` | 필드로 단순화 — 게임타입별 분기는 서버 컨텍스트에서 선택 |
| `Property<String>   _survivalDefaultConfigKey` | `String SmartMovingConfig.defaultConfigKey` | 기본 key 이름 |
| `Property<String>   _configKeyName` | `Map<String,String> SmartMovingConfig.configKeyName` | 각 key 의 표시 이름 |
| `int toggler` (Property 내부) | `int SmartMovingConfig.toggler` | 순환 카운터 |
| `Property.keys` | `SmartMovingConfig.configKeys` | 동일 |
| `Property.toggle()` | `SmartMovingConfig.toggle()` | L325-L332 1:1 |
| `Property.setKeys(String[])` | `SmartMovingConfig.setKeys(String[])` | L345-L355 1:1 |
| `Property.update()` | `SmartMovingConfig.updateToggler()` | toggler → enabled/currentKey 파생 |
| `Options.isSneakToggleEnabled()` | `cfg.sneakToggle && cfg.enabled` | 이미 존재 (변경 없음) |
| `Options.initializeForGameIfNeccessary()` | `SmartMovingServer.initialize` 에서 gameType 기반 setKeys | gameType 판정은 `player.interactionManager.getGameMode()` |

---

## 7. 구조적 차이 / 근사 이식 지점

### 7.1. 불가능/의미 없는 부분

- **key 별 값 차별화** (`_swim.e=true / _swim.m=true / _swim.h=false` 같은 **값 변동**):
  1.21.1 단일 `java.util.Properties` 구조 유지. `toggler` 는 "이름표 라벨" 수준으로만
  동작. 실제 `_swim` 필드는 여전히 단일 boolean.
- **Property 시스템 전체 재구현**: 본 포커스 범위 초과. 이식하지 않음.

### 7.2. 근사 이식

- `_configKeyName` 의 Property 기반 key-scoped defaults (`Value(null).e("Easy").m("Medium").h("Hard")`)
  → 1.21.1 `Map<String,String> configKeyName = Map.of("e","Easy","m","Medium","h","Hard","c","Creative")`
- `initializeForGameIfNeccessary(gameType)` 의 Property 기반 switch → 단순 if/else

### 7.3. 완전 재현 지점

- `toggle()` 순환 로직 (toggler++/length/-1 wrap) — **완전 재현 가능**
- 4상태 출력(disabled/easy/medium/hard) — **완전 재현 가능**

---

## 8. 현재 구현 스냅샷 (수정 전)

### 8.1. `SmartMovingConfig.java` — 관련 필드/메서드

```java
// 필드 (요약)
public boolean enabled = true;
// (configKeys/toggler/currentConfigKey/configKeyName 없음)

// 메서드
public void toggle() {
    enabled = !enabled;
    save();
}

// readFrom / writeTo 에 `move.enabled` 만 있음
```

### 8.2. `SmartMovingServer.java` — logConfigState

```java
static void logConfigState(SmartMovingConfig config, String username, boolean reconfig) {
    String message = "Smart Moving ";
    if (config.globalConfig) {
        if (!reconfig) LOGGER.info("{}overrides client configurations", message);
        String postfix = getPostfix(username);
        if (config.enabled) {
            // 현재: currentKey==null 경로만 이식
            LOGGER.info("{}{}default server configuration{}",
                    message, reconfig ? "changed to " : "uses ", postfix);
        } else {
            LOGGER.info("{}disabled{}", message, postfix);
        }
    } else {
        LOGGER.info("{}allows client configurations", message);
    }
}
```

### 8.3. `SmartMovingClient.java` — 채팅 피드백 (configToggle 키)

```java
if (SmartMovingKeys.configToggle.wasPressed()) {
    if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
        SmartMovingConfig.INSTANCE.toggle();
        String msgKey = SmartMovingConfig.INSTANCE.enabled
            ? "smartmoving.message.config.client.enabled"
            : "smartmoving.message.config.client.disabled";
        if (player != null) player.sendMessage(Text.translatable(msgKey));
    } else {
        ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
    }
}
```

---

## 9. 예상 수정 diff

### 9.1. SmartMovingConfig — 필드 추가 + toggle 재구현

```diff
 public boolean enabled = true;
+/** 원본 Property.keys — 현재 활성 config key 배열. gameType 에 따라 setKeys 로 갱신. */
+public String[] configKeys = {"e", "m", "h"};       // survival 기본
+/** 원본 Property.toggler — -1(disabled) / 0..length-1(각 key) */
+public int toggler = 1;                              // survival default "m" 위치
+/** 원본 _configKeyName. key 별 표시 이름 (Easy/Medium/Hard/Creative) */
+public java.util.Map<String,String> configKeyName = java.util.Map.of(
+    "e", "Easy", "m", "Medium", "h", "Hard", "c", "Creative"
+);

 public void toggle() {
-    enabled = !enabled;
-    save();
+    // 원본 SmartMovingProperties.toggle() L325-L332 1:1
+    int length = configKeys == null ? 0 : configKeys.length;
+    toggler++;
+    if (toggler == length) toggler = -1;
+    updateToggler();
+    save();
 }

+/** 원본 SmartMovingProperties.setKeys(String[]) L345-L355 1:1. */
+public void setKeys(String[] keys) {
+    if (keys == null || keys.length == 0) keys = new String[]{""};  // _defaultKeys 대응
+    this.configKeys = keys;
+    this.toggler = 0;
+    updateToggler();
+}

+/** toggler → enabled / currentKey 파생 재계산. 원본 update() 대응. */
+private void updateToggler() {
+    enabled = (toggler != -1);
+}

+/** 원본 Property.getCurrentKey() — toggler==-1 → null, 그 외 configKeys[toggler]. */
+public String getCurrentKey() {
+    if (toggler < 0 || configKeys == null || toggler >= configKeys.length) return null;
+    return configKeys[toggler];
+}
```

### 9.2. SmartMovingServer.logConfigState — 키 이름 경로 활성화

```diff
 if (config.enabled) {
-    LOGGER.info("{}{}default server configuration{}",
-            message, reconfig ? "changed to " : "uses ", postfix);
+    String currentKey = config.getCurrentKey();
+    String action = reconfig ? "changed to " : "uses ";
+    if (currentKey == null) {
+        LOGGER.info("{}{}default server configuration{}", message, action, postfix);
+    } else {
+        String keyName = config.configKeyName.getOrDefault(currentKey, "");
+        if (keyName.isEmpty()) {
+            LOGGER.info("{}{}server configuration with key \"{}\"{}",
+                    message, action, currentKey, postfix);
+        } else {
+            LOGGER.info("{}{}server configuration \"{}\"{}",
+                    message, action, keyName, postfix);
+        }
+    }
 } else {
     LOGGER.info("{}disabled{}", message, postfix);
 }
```

### 9.3. 클라이언트 채팅 피드백 — 4상태 메시지

```diff
-String msgKey = SmartMovingConfig.INSTANCE.enabled
-    ? "smartmoving.message.config.client.enabled"
-    : "smartmoving.message.config.client.disabled";
-if (player != null) player.sendMessage(Text.translatable(msgKey));
+SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
+Text msg;
+if (!cfg.enabled) {
+    msg = Text.translatable("smartmoving.message.config.client.disabled");
+} else {
+    String currentKey = cfg.getCurrentKey();
+    if (currentKey == null) {
+        msg = Text.translatable("smartmoving.message.config.client.default");
+    } else {
+        String keyName = cfg.configKeyName.getOrDefault(currentKey, currentKey);
+        msg = Text.translatable("smartmoving.message.config.client.named", keyName);
+    }
+}
+if (player != null) player.sendMessage(msg);
```

`en_us.json` 추가 키:
- `smartmoving.message.config.client.default` = "Smart Moving enabled (default)"
- `smartmoving.message.config.client.named` = "Smart Moving enabled: %s"

---

## 10. 원자 단위 작업 목록

### A. 원본 소스 확보 (WebFetch)
- [x] A-1. ~~Agent WebFetch — `Property.java`~~ → **리서치 파일 `SmartMovingProperties.md` L15-L174 에 전체 소스 이미 완전 임베드 확인**. `setCurrentKey`/`getCurrentKey`/`getNextKey`/`hasKey`/`update`/`toggle`/`setKeys` 본체, `_defaultKeys`/`keys`/`toggler`/`enabled` 필드 전부 존재. WebFetch 불필요.
- [x] A-2. ~~Agent WebFetch — `SmartMovingOptions.java`~~ → **리서치 파일 `SmartMovingOptions.md` L500-L531(toggle override), L840-L889(initializeForGameIfNeccessary) 에 완전 임베드 확인**. `SmartMovingConfig.md` L630-L633 에 `Survival=0`/`Creative=1`/`Adventure=2`/`Unknown=-1` 상수도 확보됨. WebFetch 불필요.
- [x] A-3. 리서치 파일 이미 완전 — 보완 불필요.

### B. `SmartMovingConfig` 필드 추가
- [x] B-1. `configKeys` 필드 (String[]) — **원본 1:1 수정**: 초기값을 survival 기본이 아닌
      원본 `_defaultKeys = {null}` 그대로 (`DEFAULT_KEYS` 상수). 보조 상수 `CONFIG_KEY_ENABLED`
      = "enabled" / `CONFIG_KEY_DISABLED` = "disabled" 도 함께 추가 (원본 L26-L27). 게임타입별
      `{"e","m","h"}` / `{"c"}` 는 F 섹션의 `initializeForGameIfNeccessary()` 에서 setKeys 로 설정.
- [x] B-2. `toggler` 필드 (int, default **-2** — 원본 L30 초기값 그대로). 상태표(−2 센티넬 / −1 disabled / 0..length−1 enabled) + 파생 규칙(`enabled = toggler != -1`) javadoc 기록.
- [x] B-3. `configKeyName` 필드 (Map 근사, default 3개 매핑 e/m/h). Creative "c" 는 의도적 부재 —
      원본 `_configKeyName` 가 Creative key 에 defaults 없어 빈 문자열 반환하는 동작과 등가.
- [x] B-4. `readFrom`/`writeTo` 에 **6개 저장 포맷** 추가 — **§10 재정의**: 원본 `move.config.key.current`/`move.config.keys` 가 아니라, 원본 L456-L463 대로 게임타입별 6개:
      `move.config.survival.keys` + `.keys.default`, `move.config.creative.keys` + `.keys.default`,
      `move.config.adventure.keys` + `.keys.default`. CSV 직렬화(원본 comment `"entries seperated by ','"`).
      6개 필드 신규 추가(`survivalConfigKeys`/`survivalDefaultConfigKey` 등) + `getCsvArray`/`csvJoin` 헬퍼.
      현재 활성 key 상태는 별도 필드로 저장하지 않고 **해당 gameType 의 `defaultConfigKey` 값을
      `toggle()` 이 갱신**하는 원본 방식 유지 (C/D 섹션에서 구현).

### C. `SmartMovingConfig` 메서드 이식
- [x] C-1. `toggle()` 재구현 — 원본 `SmartMovingProperties.toggle()` (리서치 L325-L332 =
      원본 Java L81-L88) 1:1 이식. `enabled = !enabled; save();` → `toggler++/length wrap
      /updateToggler()/save()`. save() 호출은 원본 Properties.toggle() 에는 없지만 원본
      SmartMovingOptions.toggle() override (L500-L531) 에서 saveToOptionsFile 호출 — 1.21.1
      통합 계층에서 여기 유지. SmartMovingOptions.toggle() 의 추가 동작(_configChat 메시지,
      gameType 별 defaultConfigKey 갱신) 은 별도 원자 작업으로 분리 예정 (C-7 후보).
- [x] C-2. `setKeys(String[])` 이식 — 원본 `SmartMovingProperties.setKeys()` L90-L97 1:1
      (리서치 L345-L355). null/empty → DEFAULT_KEYS, toggler=0, updateToggler() 호출.
      save() 는 원본에도 없어 호출 안 함. 호출처는 F 섹션 `initializeForGameIfNeccessary` 예정.
- [x] C-3. `updateToggler()` 헬퍼 — `enabled = (toggler != -1)` (원본 `update()` L163-L172
      의 Property 루프는 1.21.1 Property 부재로 N/A, enabled 파생만 이식). **의존 순서상
      C-1 보다 먼저 진행** — C-1 `toggle()` 이 `updateToggler()` 호출 예정.
- [x] C-4. `getCurrentKey()` — **§10 정정**: 원래 "toggler == -1 시 null" 은 오기. 원본
      `SmartMovingProperties.getCurrentKey()` (L138-L143) 는 toggler==-1 시 `Disabled`
      (= `"disabled"`) 반환, null 은 `keys[toggler]==null` (DEFAULT_KEYS 케이스, 단순 on/off
      enabled) 인 경우에만. 원본 1:1 로 이식: `toggler==-1 → CONFIG_KEY_DISABLED`,
      `configKeys[toggler]` 반환. toggler==-2 센티넬 방어 코드 포함 (F 섹션 이식 전 과도기용).
- [x] C-5. `getNextKey(String)` — 원본 L106-L118 1:1 + 의존 `getKey(int)` L99-L104 함께 이식.
      두 메서드는 연계 동작(getNextKey 가 getKey(0) 호출)이므로 단일 원자 단위로 묶음.
      `key==null \|\| "disabled"` → `getKey(0)` / keys 매칭 다음 인덱스 / 마지막·미매칭 → "disabled".
- [x] C-6. `hasKey(String)` — 원본 L145-L156 1:1. `Enabled`/`Disabled` 특수 분기 +
      배열 내 null 포함 검색. 호출처는 `setCurrentKey(String)` 이식 시 유효성 체크로 연결 예정.
- [x] C-7. `setCurrentKey(String)` — 원본 L120-L136 1:1 이식. §16 의 "C-7 후보" 를 정식 승격.
      F-1/F-2 `initializeForGameIfNeccessary` 포팅의 선행 의존 (원본 L892 에서 호출).
      3갈래: (1) `key==null || "disabled"` → `toggler=-1`, (2) `keys=={null} && key=="enabled"`
      → `toggler=0`, (3) 배열 탐색 + 미매칭 시 `toggler=-1`. 끝에 `updateToggler()` 호출.

### D. 서버 로그 4상태 확장
- [x] D-1. `SmartMovingServer.logConfigState` 의 `currentKey==null` / `configName==""` /
      `configName!=""` 3갈래 분기 복원. 원본 SmartMovingServerOptions.md L80-L113 1:1.
      `config._configKeyName.value` 는 1.21.1 근사로 `configKeyName.getOrDefault(currentKey, "")`.

### E. 클라이언트 채팅 피드백 4상태
- [x] E-1/E-2/E-3. **§10 재정의 (원본 4갈래 반영)**: 원본 `writeClientConfigMessageToChat()`
      (SmartMovingOptions.md L560-L584) 은 `disabled`/`enabled`/`named`/`unnamed` **4갈래**
      (§9.3 예상 diff 의 3갈래 `default/named` 보다 정확). 4갈래 단일 원자로 통합 처리:
      - E-1: `SmartMovingClientState.configToggle` 블록 4갈래 분기 이식 (`unnamed && keyCount==1`,
        `CONFIG_KEY_ENABLED.equals(name)` 조건 포함).
      - E-2: `en_us.json` 에 `.named` / `.unnamed` 추가. **기존 `.enabled`/`.disabled` 텍스트도**
        원본(`"Smart Moving enabled"`, `"Smart Moving disabled"`) 으로 정정 (발견 즉시 수정 원칙).
      - E-3: 기존 키는 유지 (그대로). 텍스트만 정정됨.
      - 번역 키 원본 텍스트: Agent WebFetch 로 `en_US.lang` 확보 → `SmartMovingOptions.md` 에 추가.

### F. 게임타입별 configKeys 적용

**§10 F 재정의 (원본 구조 반영)**: 원본 `initializeForGameIfNeccessary()` 는 **클라이언트 측**
메서드 (`Minecraft.getMinecraft().playerController` 기반). 1.21.1 대응은
`MinecraftClient.getInstance().interactionManager.getCurrentGameMode()`. 매 tick 호출 + gameType
캐시 비교로 변경 감지. 기존 "SmartMovingServer.initialize" 명시는 오지정 — 재정의.

- [x] F-1/F-2. `initializeForGameIfNeccessary(int)` 이식 — 원본 L854-L903 1:1 (클라이언트 측).
      gameType 캐시 필드 + switch (Survival/Creative/Adventure/default) + setKeys(keys) +
      setCurrentKey(defaultKey). gameType 판정은 호출자에서 주입 (controller null 체크 포함).
      `resetForNewGame()` 도 함께 이식 (원본 L844-L848). 상수 `GAME_TYPE_UNKNOWN/SURVIVAL/
      CREATIVE/ADVENTURE` 정의 (원본 SmartMovingConfig L630-L633).
      채팅 초기화 (_configChatInit / _speedChatInit 분기) 는 범위 초과 — §16 에 기록.
- [x] F-3. 호출 타이밍 — `SmartMovingClientState.tickEssential` 시작부에 삽입. 원본
      `SmartMovingContext.interceptTick() L264` 와 동일 위치. `Config == INSTANCE`
      조건으로 서버 설정 덮어쓰기 방지. `client.interactionManager.getCurrentGameMode().getId()`
      로 gameType 주입. tick 폴링으로 gameType 변경 감지 — 별도 이벤트 훅 불필요.

### G. 검증
- [x] G-1. `./gradlew build` 성공 — F 섹션 완료 시점 + G 세션 재실행 모두 통과
- [ ] G-2. 재현 케이스 표 모든 행 검증 (수동 테스트) — **사용자 확인 대기** (인게임 실행 필요)
- [x] G-3. 회귀 방지 감사 (§14) — TODO/미확인 0건, `INSTANCE.toggle()` 2호출처 의미 적합,
      `cfg.enabled` 31참조 의미 일치. **구조적 누락** 2건 (toggler 서버→클라 동기화 / SERVER_CONFIG
      에서 getCurrentKey 항상 -2 경로) §16/§17 기록 — 회귀는 아니지만 후속 필요.
- [x] G-4. `checklist_original_audit.md` 신규 발견 표에 "4상태 토글 복원" 기록 — 포커스 #5
      전체 요약 (B/C/D/E/F 섹션 작업 내용) + 잔여(§17 후속) 기록. 이전 L1092 의 "단일 key=
      on/off 등가" 오판 명시적 정정 포함.
- [ ] G-5. `playtest_fixes.md` 의 "현재 포커스" 를 `#6` 으로 갱신 — G-2 완료 후

---

## 11. 호출 타이밍 검증

| 호출자 | 원본 타이밍 | 1.21.1 타이밍 | 일치 |
|--------|------------|--------------|------|
| `Property.toggle()` | `SmartMovingOptions.toggle()` 에서 super 호출 (키바인딩 / 커맨드 수신 시) | `SmartMovingConfig.toggle()` 직접 호출 (동일 트리거) | ✓ |
| `Property.setKeys()` | `initializeForGameIfNeccessary(gameType)` 에서 게임타입 판정 후 | `SmartMovingServer.initialize()` 에서 gameType 기반 (F 섹션) | ✓ 예정 |
| `toggler++` | 키 입력마다 1회 | 동일 | ✓ |
| `save()` | `SmartMovingOptions.toggle()` 끝에서 `saveToOptionsFile` | `SmartMovingConfig.save()` | ✓ |

---

## 12. 테스트 프로토콜 (인게임 검증)

```
[T-1] Survival 월드 단일 플레이
  1. 월드 진입 — 채팅: "Smart Moving enabled: Medium" (toggler=1, key="m")
  2. configToggle 키 × 1 → "Smart Moving enabled: Hard" (toggler=2)
  3. configToggle 키 × 1 → "Smart Moving disabled" (toggler=-1)
  4. configToggle 키 × 1 → "Smart Moving enabled: Easy" (toggler=0)
  5. configToggle 키 × 1 → "Smart Moving enabled: Medium"

[T-2] Creative 월드
  1. /gamemode creative — setKeys({"c"}) 재호출 확인
  2. 진입 시 "Smart Moving enabled: Creative" (toggler=0)
  3. configToggle × 1 → "Smart Moving disabled" (toggler=-1)
  4. configToggle × 1 → "Smart Moving enabled: Creative"

[T-3] 멀티플레이 (globalConfig=true)
  1. 관리자 /smoving config toggle × 1 → 모든 플레이어 채팅에 "Smart Moving
     changed to server configuration \"Medium\"" 같은 메시지
  2. 서버 콘솔 로그: "Smart Moving changed to server configuration \"Medium\"
     by user 'admin'"
  3. broadcastConfig 로 모든 접속자 Config 재전송

[T-4] 파일 지속성
  1. 월드 나가기 → config/smart_moving_options.properties 확인
  2. toggler / configKeys 값이 저장됨
  3. 재진입 시 같은 toggler 로 복원
```

---

## 13. 완료 전 검증 체크리스트 (포커스 #5 고유 + 공통)

### 공통 (playtest_fixes.md §공통 완료 전 검증)
- [ ] 리서치 근거: 원본 L325-L332 / L345-L355 확보 + 보완 완료
- [ ] side-by-side 1:1: 원본 `toggle()` 과 구현 `toggle()` 줄 단위 대응
- [ ] 모든 분기: `toggler == length` → `-1` 분기 구현됨
- [ ] 상수: configKeys 기본값 `{"e","m","h"}` / Creative `{"c"}` 양쪽 모두
- [ ] 호출 타이밍: setKeys 는 게임타입 판정 직후 (§11 참조)
- [ ] 근사 이식 주석: 해당 없음 (완전 재현 가능 영역)
- [ ] 신규 발견 등록: §16 에 기록
- [ ] 회귀 방지: §14 통과
- [ ] 빌드: `./gradlew build` 성공

### #5 고유
- [ ] 4상태 전이 테이블 T-1 전부 매칭
- [ ] Creative(단일 key) T-2 매칭
- [ ] 서버 로그 3-갈래 출력(disabled / default / named) 전부 동작
- [ ] 클라이언트 채팅 피드백 4가지 메시지 올바름
- [ ] 파일 저장/로드 시 toggler 지속
- [ ] `enabled` 플래그가 여전히 공통 활성 게이트로 작동 (다른 이식이 쓰는 `cfg.enabled` 의미 유지)

---

## 14. 회귀 방지 감사

### 이 수정이 영향을 줄 수 있는 기존 이식

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `SmartMovingServer.adminToggleConfig` | `INSTANCE.toggle()` 호출 — 새 순환 자동 반영 | [x] 코드 상 `INSTANCE.toggle()` 호출 유지 + 4상태 순환 로직 적용됨 (수동 인게임 검증은 G-2) |
| `SmartMovingServer.broadcastConfig` | 모든 접속자에게 재전송 — toggler 값이 `INSTANCE.toArray()` 에 포함되는지 | [△] toggler 자체는 전송 안 됨 — 게임타입별 keys 6개(survivalConfigKeys 등)만 전송. **서버→클라 toggler 동기화 구조적 누락** → §16 에 기록, §17 후속. 실제 Config 값 전송은 정상 (포커스 범위 외 영향). |
| `cfg.enabled` 참조 위치 모두 | 의미 유지 (enabled = toggler != -1) | [x] `grep` 결과 9개 파일 31개 참조 — 모두 "활성화 상태" read-only 용도, 의미 일치 |
| `isSneakToggleEnabled` / `isCrawlToggleEnabled` | `cfg.sneakToggle && cfg.enabled` — 원본과 동일 | [x] 이 포커스에서 변경한 파일에 없음 — 불변 |
| `SmartMovingClient.processConfigContentPacket` | 서버가 보낸 toggler 값으로 SERVER_CONFIG 갱신 | [△] 서버는 toggler 미전송 → 클라 SERVER_CONFIG.toggler 는 초기값 -2. SERVER_CONFIG 경로에서는 `tickEssential` 의 initializeForGameIfNeccessary 도 `Config==INSTANCE` 가드에 의해 호출 안 됨 → SERVER_CONFIG 에서 getCurrentKey 는 항상 "disabled 방어 코드" 경로 (-2 → null). 구조적 한계, §17 후속. |

### 공통 grep 스캔

- [x] `grep "// TODO\|// \[미확인\]" src/main/java/choco/ratel/smartmoving/config/ src/main/java/choco/ratel/smartmoving/server/` — **0건**, 통과
- [x] `grep "INSTANCE\.toggle\|cfg\.toggle\|Config\.toggle"` — 2개 호출처 (`adminToggleConfig`, `configToggle` 키) 모두 4상태 순환 의미로 적합

---

## 15. 작업 기록 (세션별)

### 세션 1 — 2026-04-23

**진행한 작업**:
- 진입점 문서 (`playtest_fixes.md`) 생성 + 17-섹션 공통 구조 명세
- 이 포커스 파일 (`focus_05_config_toggle.md`) 17-섹션 구조로 재작성

**다음 작업**: A-1 / A-2 — Agent WebFetch 로 원본 `Property.java` / `SmartMovingOptions.java` 코드 확보

### 세션 2 — 2026-04-23

**진행한 작업**:
- Agent WebFetch 시도 → Agent 가 `net.smart.properties.Property` 를 봤으나 실제 대상은
  `net.smart.moving.config.SmartMovingProperties` (별개 클래스). 혼동 확인.
- 리서치 파일 재검증:
  - `SmartMovingProperties.md` L15-L174 전체 소스 임베드 확인 — `toggle`/`setKeys`/
    `getCurrentKey`/`getNextKey`/`setCurrentKey`/`hasKey`/`update`/`_defaultKeys`/`toggler`/`keys` 모두 존재
  - `SmartMovingOptions.md` L500-L531 `toggle()` override 확인
  - `SmartMovingOptions.md` L840-L889 `initializeForGameIfNeccessary()` 확인
  - `SmartMovingConfig.md` L630-L633 gameType 상수 `Survival=0` / `Creative=1` / `Adventure=2` / `Unknown=-1` 확인
- 결론: A-1 / A-2 / A-3 추가 WebFetch 불필요. 리서치 파일만으로 B 단계 진입 가능.
- A-1 / A-2 / A-3 [x] 처리.

**완료 전 검증 체크리스트 (A 섹션 기준)**:
- [근거] 원본 코드 리서치 파일 존재 확인 ✓
- [근거] 출처 URL 리서치 파일 상단 명시 ✓
- 나머지 항목은 "리서치 확인" 작업이라 해당 없음

**다음 작업**: B-1 — `SmartMovingConfig` 에 `configKeys` / `toggler` / `currentConfigKey` /
`configKeyName` 필드 추가.

### 세션 3 — 2026-04-23 — B-1

**진행한 작업**:
- B-1: `SmartMovingConfig` 에 원본 `SmartMovingProperties` L26-L31 대응 필드/상수 추가.
  - `CONFIG_KEY_ENABLED = "enabled"` (public static final String)
  - `CONFIG_KEY_DISABLED = "disabled"` (public static final String)
  - `DEFAULT_KEYS = new String[] { null }` (private static final)
  - `public String[] configKeys = DEFAULT_KEYS` — 원본 `keys = _defaultKeys` 1:1
- **§9 예상 diff 원본 1:1 방향으로 정정**: 초기값을 `{"e","m","h"}` 가 아닌 `DEFAULT_KEYS`
  ({null}) 로. 게임타입별 배열 설정은 F 섹션 (`initializeForGameIfNeccessary`) 이식 시
  `setKeys()` 호출로 교체되는 원본 흐름 재현.

**완료 전 검증 체크리스트 (B-1 기준)**:
- [근거] `SmartMovingProperties.md` L15-L174 재확인, 특히 L26-L31 임베드 소스 확인 ✓
- [대응] 원본 4심볼(Enabled/Disabled/_defaultKeys/keys) ↔ 구현 4심볼 1:1 매핑 ✓
- [상수] 원본 상수값 그대로 ("enabled" / "disabled" / `new String[1]`) ✓
- [분기] 해당 없음 (필드 선언만)
- [타이밍] 해당 없음
- [근사] 해당 없음 (완전 재현)
- [신규] 없음
- [회귀] 기존 `enabled = true` 초기값 유지 (아직 `update()` 파생으로 변경 안 함 — C 섹션 예정)
- [빌드] `./gradlew build` ✓

**다음 작업**: B-2 — `toggler` 필드 (int, default -2 원본 그대로).

### 세션 3 — 2026-04-23 — B-2

**진행한 작업**:
- B-2: `SmartMovingConfig.toggler` 필드 추가. 원본 L30 `private int toggler = -2` 1:1.
  접근 제어는 public (다른 파일에서 읽기 필요 — `SmartMovingServer.logConfigState` 등).
  javadoc 에 상태표(−2/−1/0..length−1) + 파생 규칙(`enabled = toggler != -1`) 기록.

**완료 전 검증 체크리스트 (B-2 기준)**:
- [근거] `SmartMovingProperties.md` L30 임베드 소스 확인 ✓
- [대응] 원본 `toggler = -2` ↔ 구현 `toggler = -2` 1:1 ✓
- [상수] 초기값 -2 그대로 ✓
- [분기/타이밍/근사] 해당 없음
- [신규] 없음
- [회귀] 해당 없음 (아직 어디서도 참조 안 함 — C 섹션 `update()` 에서 처음 사용 예정)
- [빌드] `./gradlew build` ✓

**다음 작업**: B-3 — `configKeyName` 필드 (Map, default 4개 매핑). 원본
`_configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"))`
대응 근사 (1.21.1 단순 Map).

### 세션 3 — 2026-04-23 — B-3

**진행한 작업**:
- B-3: `SmartMovingConfig.configKeyName` Map 필드 추가.
  - 원본 `_configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"))` 근사.
  - Property key-scoped defaults → `Map<String,String>` 으로 단순화.
  - 기본 매핑: `{"e":"Easy", "m":"Medium", "h":"Hard"}` — 3개만 (원본도 3개).
  - Creative "c" 는 원본 `_configKeyName` 에 defaults 없음 → `logConfigState` 에서 `"with
    key X"` 분기로 빠지는 동작과 등가 (`getOrDefault(key, "")` 로 구현).
  - 문서 §9 예상 diff 의 4번째 매핑("c":"Creative") 은 **잉여**였으므로 제외.

**완료 전 검증 체크리스트 (B-3 기준)**:
- [근거] `SmartMovingConfig.md` L620 원본 코드 재확인 ✓
- [대응] 원본 `_configKeyName` key-scoped defaults ↔ 구현 `Map.of(...)` 3개 키 근사 ✓
- [상수] 3개 키/값 원본 그대로 ✓
- [분기/타이밍] 해당 없음
- [근사] Property 시스템 단순화 — javadoc 에 "1.21.1 근사" 명시 ✓
- [신규] 없음
- [회귀] 해당 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: B-4 — `readFrom`/`writeTo` 에 `toggler` / `configKeys` 저장 포맷 추가. 원본
`SmartMovingConfig.md` L456-L463 의 `move.config.survival.keys` / `move.config.survival.keys.default`
식 키 이름을 1.21.1 에 어떻게 매핑할지 결정 필요. (원본은 `Property<String[]>` / `Property<String>`
로 분리 저장, 1.21.1 은 단일 `configKeys` 필드 하나만 있으므로 저장 포맷 단순화.)

### 세션 4 — 2026-04-23 — B-4

**진행한 작업**:
- **§10 B-4 재정의**: 원래 문구 "`move.config.key.current` / `move.config.keys`" 는
  부정확. 원본은 **게임타입별 6개 필드** 로 저장 (SmartMovingConfig L456-L463). 재정의 후 이식.
- 리서치 파일 보완: `SmartMovingConfig.md` L608-L620 의 `Strings(...)` 괄호 안 키 이름
  명시로 교체 (`move.config.survival.keys` 등 6종). Agent 세션 2 결과 반영.
- SmartMovingConfig 에 필드 6개 신규 추가:
    `survivalConfigKeys`       = `{"e","m","h"}`
    `survivalDefaultConfigKey` = `"m"`
    `creativeConfigKeys`       = `{"c"}`
    `creativeDefaultConfigKey` = `"c"`
    `adventureConfigKeys`      = `{"e","m","h"}`
    `adventureDefaultConfigKey`= `"m"`
  원본 defaults 와 1:1.
- readFrom/writeTo 에 6개 저장 포맷 추가. String[] 은 CSV (원본 comment `"entries seperated by ','"`
  기반). 유틸 헬퍼 `getCsvArray(p, key, def)` / `csvJoin(arr)` 신규.
- 현재 활성 key 는 별도 필드로 저장 안 함 — 원본처럼 `toggle()` 이 gameType 의 `defaultConfigKey`
  값을 갱신하는 방식 유지 (C/D 섹션에서 구현).

**완료 전 검증 체크리스트 (B-4 기준)**:
- [근거] `SmartMovingConfig.md` L456-L463 (Agent 세션 2 확보 + 리서치 파일 보완) ✓
- [대응] 원본 6 필드 ↔ 구현 6 필드 + 6 키 매칭 ✓
- [상수] 기본값 `{"e","m","h"}` / `"m"` / `{"c"}` / `"c"` 모두 원본 그대로 ✓
- [분기] 해당 없음
- [타이밍] readFrom/writeTo 호출 시점 동일 (기존 enabled 옆에 추가)
- [근사] CSV 포맷은 원본 Strings Property 와 동등 (comment 명시)
- [신규] 없음
- [회귀] 기존 readFrom/writeTo 다른 필드 처리 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: C-1 — `toggle()` 재구현. 현재 `enabled = !enabled; save();` 를 원본
`SmartMovingProperties.toggle()` L81-L88 (`toggler++; if (toggler == length) toggler = -1; update();`)
으로 교체.

### 세션 5 — 2026-04-23 — C-3 (순서 조정)

**진행한 작업**:
- **순서 조정**: §10 문서상 순서는 C-1 먼저지만, 의존 관계상 `toggle()` 이 `updateToggler()`
  호출에 의존 → C-3 을 먼저 진행. (이전 세션 종료 메시지에 예고됨)
- C-3: `SmartMovingConfig.updateToggler()` private 메서드 추가.
  - 원본 `SmartMovingProperties.update()` L163-L172 1:1 근사:
      ```
      protected void update() {
          List<Property<?>> properties = getProperties();
          Iterator<Property<?>> it = properties.iterator();
          String currentKey = getCurrentKey();
          while (it.hasNext()) it.next().update(currentKey);  // ← Property 부재로 N/A
          enabled = toggler != -1;
      }
      ```
  - 1.21.1 Property 계층 부재 → Property 루프 생략, `enabled = toggler != -1;` 만 이식.
  - 호출처: toggle / setKeys / setCurrentKey / load 등 toggler 변경 직후 (다음 작업들에서 삽입).
  - 현재는 아직 호출 없음 → IDE `unused method` 경고 가능. C-1/C-2/C-4 이식 시 호출 연결.

**완료 전 검증 체크리스트 (C-3 기준)**:
- [근거] `SmartMovingProperties.md` L163-L172 원본 코드 임베드 확인 ✓
- [대응] 원본 `update()` 의 ②(enabled 파생) ↔ 구현 `updateToggler()` 1:1. ①(Property 루프)는
  구조적 N/A — javadoc 에 명시 ✓
- [분기] 해당 없음 (단순 할당)
- [상수] 해당 없음
- [타이밍] 호출처(toggle/setKeys/setCurrentKey/load) 는 후속 작업에서 연결
- [근사] "1.21.1: Property 계층 부재 → ① 는 N/A" 주석 달림 ✓
- [신규] 없음
- [회귀] 없음 (아직 호출 없음)
- [빌드] `./gradlew build` ✓

**다음 작업**: C-1 — `toggle()` 재구현 (toggler++ 순환 + updateToggler() 호출 + save()).

### 세션 5 (계속) — 2026-04-23 — C-1

**진행한 작업**:
- C-1: `SmartMovingConfig.toggle()` 재구현. 원본 `SmartMovingProperties.toggle()` L81-L88 1:1.
  - 기존 `enabled = !enabled; save();` 를 4상태 순환 로직으로 교체:
      ```java
      int length = configKeys == null ? 0 : configKeys.length;
      toggler++;
      if (toggler == length) toggler = -1;
      updateToggler();
      save();
      ```
  - save() 호출은 원본 Properties.toggle() 에는 없으나 SmartMovingOptions.toggle() override
    (L500-L531) 의 saveToOptionsFile 에 해당. 1.21.1 통합 계층이라 여기서 호출 유지.
  - **Options.toggle() 의 추가 동작(_configChat 메시지 + gameType 별 defaultKey 갱신)은
    별도 원자 작업** — §10 C-7 신규 등록 필요 (신규 발견 기록).

**완료 전 검증 체크리스트 (C-1 기준)**:
- [근거] `SmartMovingProperties.md` L81-L88 임베드 소스 확인 ✓
- [근거] `SmartMovingOptions.md` L500-L531 override 확인 (추가 동작은 별도) ✓
- [대응] 원본 4줄 ↔ 구현 4줄 1:1 ✓
- [분기] `if (toggler == length)` 조건 그대로, `==` 유지 ✓
- [상수] 해당 없음
- [타이밍] C-3 `updateToggler()` 호출 연결 ✓
- [근사] save() 위치만 원본과 다름 — javadoc 명시 ✓
- [신규] Options.toggle() 추가 동작(채팅 + defaultKey 갱신) → §16 기록 + §10 C-7 후속 원자 작업 예정
- [회귀] 기존 `enabled = !enabled` 의미가 완전히 달라짐 → §14 회귀 감사 대상
- [빌드] `./gradlew build` ✓

**⚠ 중간 상태 주의**: C-1 완료 시점에서 `toggler = -2` 초기값 → 첫 토글 시 `-2+1=-1 → disabled`.
원본도 load() 에서 toggler=0 으로 설정 후 initializeForGameIfNeccessary 에서 setCurrentKey
로 default key 인덱스(survival 은 1)로 이동. F 섹션 완료 전까지 이 초기화 흐름 부재 →
중간 상태에서는 토글이 예상대로 작동 안 함. F 완료 시점에 정상화.

**다음 작업**: C-2 — `setKeys(String[])` 이식 (원본 L90-L97).

### 세션 5 (계속) — 2026-04-23 — C-2

**진행한 작업**:
- C-2: `SmartMovingConfig.setKeys(String[])` 신규 메서드 추가.
  - 원본 `SmartMovingProperties.setKeys()` L90-L97 1:1:
      ```java
      public void setKeys(String[] keys) {
          if (keys == null || keys.length == 0) keys = DEFAULT_KEYS;
          this.configKeys = keys;
          this.toggler = 0;
          updateToggler();
      }
      ```
  - null/empty 방어 → `DEFAULT_KEYS` ({null}) 로 fallback (단순 on/off 모드).
  - `toggler = 0` 재설정 (원본 L95 `toggler = 0; update();`).
  - save() 호출은 원본에도 없음 — 생략.
  - 호출처는 F 섹션 `initializeForGameIfNeccessary(gameType)` 이식 시
    gameType 별 keys 배열 전달로 연결 예정.

**완료 전 검증 체크리스트 (C-2 기준)**:
- [근거] `SmartMovingProperties.md` L90-L97 원본 임베드 확인 ✓
- [대응] 원본 4줄 ↔ 구현 4줄 1:1 ✓
- [분기] `keys == null || keys.length == 0` 조건 그대로 ✓
- [상수] `DEFAULT_KEYS` (= 원본 `_defaultKeys`) 사용 ✓
- [타이밍] 호출처 F 섹션에서 연결 예정
- [근사] 해당 없음
- [신규] 없음
- [회귀] 신규 메서드 추가만이라 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-4 — `getCurrentKey()` (toggler == -1 시 null, 그 외 `configKeys[toggler]`).

### 세션 6 — 2026-04-23 — C-4

**진행한 작업**:
- C-4: `SmartMovingConfig.getCurrentKey()` 신규 메서드 추가.
- **§10 문구 정정**: 원래 "toggler == -1 시 null" 은 오기. 원본 1:1:
    ```java
    public String getCurrentKey() {
        if (toggler == -1) return CONFIG_KEY_DISABLED;  // "disabled" (null 아님)
        if (toggler < 0 || configKeys == null || toggler >= configKeys.length) return null;
        return configKeys[toggler];
    }
    ```
- 반환값 의미 (javadoc 명시):
    - `toggler == -1` → `"disabled"` (비활성 상태)
    - `toggler >= 0 && keys[toggler] == null` → null (DEFAULT_KEYS {null}, 단순 on/off enabled)
    - `toggler >= 0 && keys[toggler] != null` → "e"/"m"/"h"/"c" 등 key 이름
- toggler == -2 (센티넬, F 이식 전 과도기) 방어 코드 추가 — 원본은 load() 후만 호출 가정이라
  미정의. 1.21.1 과도기에는 null (단순 on/off) 반환.
- 기존 호출자 없음 → 회귀 영향 없음 (D 섹션 서버 로그 4상태 분화에서 첫 소비).

**완료 전 검증 체크리스트 (C-4 기준)**:
- [근거] `SmartMovingProperties.md` L138-L143 원본 임베드 확인 ✓
- [대응] 원본 3줄 ↔ 구현 3줄(+방어 1줄) 1:1 ✓
- [분기] toggler == -1 분기 그대로 + 센티넬/범위 방어 ✓
- [상수] `CONFIG_KEY_DISABLED = "disabled"` (= 원본 `Disabled`) 사용 ✓
- [타이밍] 호출처는 D 섹션 logConfigState, E 섹션 채팅 피드백에서 연결
- [근사] toggler==-2 방어 코드는 근사 (원본 미정의 케이스) — javadoc 명시 ✓
- [신규] 없음
- [회귀] 신규 메서드 추가만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-5 — `getNextKey(String key)` (원본 L106-L118).

### 세션 6 (계속) — 2026-04-23 — C-5 (+ getKey)

**진행한 작업**:
- C-5: `getNextKey(String)` + 의존 `getKey(int)` 함께 이식 (원본 L106-L118 + L99-L104).
  두 메서드는 연계 동작(getNextKey → getKey(0) 호출) — 단일 원자 단위로 묶음.
  `getKey(int)` 는 §10 에 별도 체크박스 없지만 C-5 내 포함.
- 원본 1:1:
    ```java
    public String getKey(int index) {
        if (configKeys[index] == null) return CONFIG_KEY_ENABLED;
        return configKeys[index];
    }
    public String getNextKey(String key) {
        if (key == null || key.equals(CONFIG_KEY_DISABLED))
            return getKey(0);
        int index;
        for (index = 0; index < configKeys.length; index++)
            if (key.equals(configKeys[index])) break;
        index++;
        if (index < configKeys.length) return configKeys[index];
        return CONFIG_KEY_DISABLED;
    }
    ```
- 동작 예시 javadoc 에 기록 (키 배열별 전이표).

**완료 전 검증 체크리스트 (C-5 기준)**:
- [근거] `SmartMovingProperties.md` L99-L118 원본 임베드 확인 ✓
- [대응] 원본 getKey 3줄 + getNextKey 13줄 ↔ 구현 동일 수준 1:1 ✓
- [분기] null/disabled 조기 반환, for 매칭, 마지막 분기 전부 재현 ✓
- [상수] `CONFIG_KEY_ENABLED` / `CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처 없음 (C-7 setCurrentKey 또는 toggle 의 후속 이식에서 참조 가능)
- [근사] 해당 없음
- [신규] 없음
- [회귀] 신규 메서드만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-6 — `hasKey(String)` (원본 L145-L156).

### 세션 7 — 2026-04-23 — C-6

**진행한 작업**:
- C-6: `SmartMovingConfig.hasKey(String)` 신규 메서드 추가.
- 원본 `SmartMovingProperties.hasKey()` L145-L156 1:1:
    ```java
    public boolean hasKey(String key) {
        if (CONFIG_KEY_ENABLED.equals(key))
            return configKeys[0] == null;
        if (CONFIG_KEY_DISABLED.equals(key))
            return true;
        for (int i = 0; i < configKeys.length; i++)
            if (key == null && configKeys[i] == null
                    || key != null && key.equals(configKeys[i]))
                return true;
        return false;
    }
    ```
- 분기 3갈래:
    - `"enabled"` → `configKeys[0] == null` (DEFAULT_KEYS 단순 on/off 모드 판정)
    - `"disabled"` → 항상 true
    - 그 외 → 배열 탐색 (null 포함 동치 비교)
- 호출처 없음 (C-7 예정 `setCurrentKey(String)` 에서 유효성 체크로 참조). 신규 메서드만 — 회귀 영향 없음.

**완료 전 검증 체크리스트 (C-6 기준)**:
- [근거] `SmartMovingProperties.md` L145-L156 원본 임베드 확인 ✓
- [대응] 원본 11줄 ↔ 구현 11줄 1:1 (중괄호/들여쓰기 포함 동등) ✓
- [분기] 세 분기(Enabled/Disabled/배열 탐색) 그대로 ✓
- [상수] `CONFIG_KEY_ENABLED`/`CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처 없음 — C-7(setCurrentKey) 이식 시 연결
- [근사] 해당 없음 (완전 재현)
- [신규] 없음
- [회귀] 신규 메서드만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-7 (신규 등록 필요) — `setCurrentKey(String)` 이식 + Options.toggle()
override 추가 동작(_configChat 채팅 + gameType 별 defaultKey 갱신).
또는 D 섹션 진입(`SmartMovingServer.logConfigState` 4상태 분기 복원) 중 선택.

### 세션 8 — 2026-04-23 — D-1

**진행한 작업**:
- D-1: `SmartMovingServer.logConfigState` 의 enabled=true 경로 3갈래 분기 복원.
- 원본 근거: `SmartMovingServerOptions.md` L80-L113 1:1 (리서치 파일 grep 확인 후 해당 섹션 재독).
- 3갈래:
    ```java
    String currentKey = config.getCurrentKey();
    String action = reconfig ? "changed to " : "uses ";
    if (currentKey == null) {
        LOGGER.info("{}{}default server configuration{}", message, action, postfix);
    } else {
        String configName = config.configKeyName.getOrDefault(currentKey, "");
        if (configName.isEmpty()) {
            LOGGER.info("{}{}server configuration with key \"{}\"{}", message, action, currentKey, postfix);
        } else {
            LOGGER.info("{}{}server configuration \"{}\"{}", message, action, configName, postfix);
        }
    }
    ```
- `config._configKeyName.value` 원본 의미 = Property key-scoped defaults 기반 현재 key의 표시 이름.
  1.21.1 근사: C-4 (`getCurrentKey()`) + B-3 (`configKeyName` Map) 조합으로 `getOrDefault(currentKey, "")`.
- Creative "c" → 매핑 부재 → "" → `"with key \"c\""` 분기 (원본 `_configKeyName` 가 "c" 에 defaults 없음과 등가).
- 기존 주석의 "1.21.1 범위 제한" / "키 이름 경로는 추후 추가" 문구 제거 — 본 원자 작업으로 해소.

**완료 전 검증 체크리스트 (D-1 기준)**:
- [근거] `SmartMovingServerOptions.md` L80-L113 리서치 파일 확인 ✓
- [대응] 원본 3갈래 분기 + 출력 포맷 8패턴 (§379-L388 표) ↔ 구현 3갈래 1:1 ✓
- [분기] `currentKey==null` / `configName.isEmpty()` / else 모두 복원 ✓
- [상수] 문자열 리터럴 "default server configuration" / "server configuration with key" / "server configuration" 원본 그대로 ✓
- [타이밍] 기존 호출처(initialize/adminToggleConfig) 변경 없음 — 출력 내용만 확장
- [근사] `_configKeyName.value` Property key-scoped → Map.getOrDefault 근사, javadoc 명시 ✓
- [신규] 없음
- [회귀] globalConfig=false 경로 / enabled=false 경로 / reconfig=false 의 "overrides" 출력 모두 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: E-1 — `SmartMovingClient.tickEssential` configToggle 블록의 msgKey 분기 4상태
확장 + E-2 번역 키 추가 + E-3 기존 키 유지.

### 세션 9 — 2026-04-23 — E-1/E-2/E-3 (4갈래 통합)

**진행한 작업**:
- **§10 E 섹션 재정의**: 원본 `writeClientConfigMessageToChat()` 재독 후 §9.3 예상 diff 가
  3갈래 (`disabled`/`default`/`named`) 였으나 실제 원본은 **4갈래** (`disabled`/`enabled`/
  `named`/`unnamed`). 원본 1:1 우선 원칙에 따라 §10 E 재정의, E-1/E-2/E-3 묶음 처리.
- **Agent WebFetch**: 원본 `en_US.lang` 에서 4개 키 값 확보:
    ```
    move.config.chat.client.enabled=Smart Moving enabled
    move.config.chat.client.disabled=Smart Moving disabled
    move.config.chat.client.named=Smart Moving set to '%s'
    move.config.chat.client.unnamed=Smart Moving set to key '%s'
    ```
  리서치 파일 `SmartMovingOptions.md` §writeClientConfigMessageToChat 에 "번역 키 원문"
  하위 섹션으로 보완.
- **기존 `en_us.json` 텍스트 오역 정정** (발견 즉시 수정):
    - `.enabled`: "Smart Moving is now enabled." → "Smart Moving enabled"
    - `.disabled`: "Smart Moving is now disabled." → "Smart Moving disabled"
- **신규 키 추가**:
    - `.named` = "Smart Moving set to '%s'"
    - `.unnamed` = "Smart Moving set to key '%s'"
- **`SmartMovingClientState.tickEssential` configToggle 블록 4갈래 이식**:
    ```java
    if (!cfg.enabled) → "disabled"
    else:
      name = configKeyName.getOrDefault(currentKey, "");
      if (name.isEmpty()) name = null;
      unnamed = name == null;
      if (unnamed) name = currentKey;
      if (CONFIG_KEY_ENABLED.equals(name) || (unnamed && keyCount == 1)) → "enabled"
      else if (unnamed) → "unnamed" + name
      else → "named" + name
    ```
  원본 L567-L583 분기 13줄 1:1.

**완료 전 검증 체크리스트 (E-1+E-2+E-3 기준)**:
- [근거] `SmartMovingOptions.md` L560-L584 원본 분기 확인 ✓
- [근거] `en_US.lang` 4키 원문 WebFetch 확보 + 리서치 파일 보완 ✓
- [대응] 원본 4갈래 ↔ 구현 4갈래 1:1 ✓
- [분기] `name.isEmpty() → null` / `unnamed = name==null` / `unnamed && keyCount==1` /
  `"enabled".equals(name)` / 일반 `named` / `unnamed` 기본 경로 모두 재현 ✓
- [상수] 4개 번역 키 텍스트 원본 그대로 `.lang` 확보 후 적용 ✓
- [타이밍] `configToggle.wasPressed()` 직후 `toggle()` → 메시지 (원본 `super.toggle()` 후
  `writeClientConfigMessageToChat` 순서와 등가)
- [근사] `_configKeyName.value` Property key-scoped → Map.getOrDefault (B-3 근사 활용) ✓
- [신규] 없음 (§9.3 예상 diff 오기 정정은 §10 재정의 로 반영)
- [회귀] 기존 `.enabled`/`.disabled` 키 유지 — 텍스트만 원본 일치로 정정. 다른 참조처 없음.
- [빌드] `./gradlew build` ✓

**다음 작업**: F-1 — `SmartMovingServer.initialize` 에서 gameType 기반 setKeys 호출 흐름 이식.
`player.interactionManager.getGameMode()` 로 gameType 판정. `initializeForGameIfNeccessary` 포팅.

### 세션 10 — 2026-04-23 — C-7 (신규 승격)

**진행한 작업**:
- §16 에 "C-7 후보" 로 기록되어 있던 `setCurrentKey(String)` 를 §10 정식 체크박스로 승격.
  F-1/F-2 `initializeForGameIfNeccessary` 포팅의 선행 의존 (원본 L892:
  `if (!defaultKey.isEmpty()) setCurrentKey(defaultKey);`).
- C-7: `SmartMovingConfig.setCurrentKey(String)` 신규 메서드 추가. 원본 L120-L136 1:1:
    ```java
    public void setCurrentKey(String key) {
        if (key == null || key.equals(CONFIG_KEY_DISABLED))
            toggler = -1;
        else if (configKeys.length == 1 && configKeys[0] == null
                && key.equals(CONFIG_KEY_ENABLED))
            toggler = 0;
        else {
            for (toggler = 0; toggler < configKeys.length; toggler++)
                if (key.equals(configKeys[toggler]))
                    break;
            if (toggler == configKeys.length)
                toggler = -1;
        }
        updateToggler();
    }
    ```
- 3갈래 분기:
    - (1) `key == null || "disabled"` → `toggler = -1` (비활성)
    - (2) `configKeys == {null} (DEFAULT_KEYS) && key == "enabled"` → `toggler = 0` (단순 on/off enabled)
    - (3) 배열 탐색 (for 매칭 없으면 length 도달 → `toggler = -1`)
- 끝에 `updateToggler()` 호출 (원본 `update()` 의 ② enabled 파생 재계산).
- 호출처 없음 (F 섹션에서 첫 소비).

**완료 전 검증 체크리스트 (C-7 기준)**:
- [근거] `SmartMovingProperties.md` L120-L136 원본 임베드 확인 ✓
- [대응] 원본 17줄 ↔ 구현 15줄(if/else 블록 구조 포함) 1:1 ✓
- [분기] 3갈래 + 배열 탐색 for 루프 + 미매칭 `toggler=length → -1` 모두 재현 ✓
- [상수] `CONFIG_KEY_ENABLED`/`CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처는 F 섹션에서 연결
- [근사] 해당 없음 (완전 재현 영역)
- [신규] 없음
- [회귀] 신규 메서드 추가만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: F-1/F-2 묶음 (gameType 판정 + setKeys/setCurrentKey 호출). 원본
`initializeForGameIfNeccessary()` 는 클라이언트 측 로직 (`Minecraft.getMinecraft().playerController`).
1.21.1 대응: `MinecraftClient.getInstance().interactionManager.getCurrentGameMode()`.
§10 F 재정의 필요 (현재 "SmartMovingServer.initialize" 로 명시되어 있으나 원본은 클라이언트 측).

### 세션 11 — 2026-04-23 — F-1/F-2/F-3 (gameType 초기화)

**진행한 작업**:
- **§10 F 재정의**: 원본 `initializeForGameIfNeccessary()` 는 클라이언트 측
  (`Minecraft.getMinecraft().playerController`). 기존 "SmartMovingServer.initialize" 오지정
  — 클라이언트 tick 폴링으로 재정의.
- **상수 추가** (`SmartMovingConfig`, 원본 SmartMovingConfig L630-L633):
    - `GAME_TYPE_UNKNOWN   = -1`
    - `GAME_TYPE_SURVIVAL  = 0`
    - `GAME_TYPE_CREATIVE  = 1`
    - `GAME_TYPE_ADVENTURE = 2`
- **필드 추가**: `private int gameType = GAME_TYPE_UNKNOWN` (원본 캐시 필드).
- **F-1/F-2 묶음**: `SmartMovingConfig.initializeForGameIfNeccessary(int)` 이식. 원본 L854-L903 1:1:
    ```java
    public void initializeForGameIfNeccessary(int currentGameType) {
        if (currentGameType == gameType) return;
        gameType = currentGameType;
        String[] keys = null;
        String defaultKey = null;
        switch (gameType) {
            case GAME_TYPE_SURVIVAL:  keys = survivalConfigKeys;  defaultKey = survivalDefaultConfigKey;  break;
            case GAME_TYPE_CREATIVE:  keys = creativeConfigKeys;  defaultKey = creativeDefaultConfigKey;  break;
            case GAME_TYPE_ADVENTURE: keys = adventureConfigKeys; defaultKey = adventureDefaultConfigKey; break;
            default:                  defaultKey = "";
        }
        setKeys(keys);
        if (!defaultKey.isEmpty()) setCurrentKey(defaultKey);
    }
    ```
  - controller null 체크는 호출자로 위임 (원본 L859-L861).
  - 리플렉션 → GameMode.getId() 주입 방식.
  - `_configChatInit` / `_speedChatInit` 채팅 분기는 범위 초과 (§16 기록).
- **resetForNewGame()** 도 함께 이식 (원본 L844-L848):
    ```java
    public void resetForNewGame() { gameType = GAME_TYPE_UNKNOWN; }
    ```
- **F-3 호출 타이밍**: `SmartMovingClientState.tickEssential` 시작부에 삽입:
    ```java
    MinecraftClient client = MinecraftClient.getInstance();
    if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE
            && client.interactionManager != null) {
        SmartMovingConfig.INSTANCE.initializeForGameIfNeccessary(
                client.interactionManager.getCurrentGameMode().getId());
    }
    ```
  - 원본 `SmartMovingContext.interceptTick()` L264 와 동일 위치 (tick 폴링).
  - `Config == INSTANCE` 조건으로 서버 설정 덮어쓰기 방지 (원본은 Options 객체 전용이라
    서버 설정과 분리됨).
- **빌드 문제 해결**: `MinecraftClient mc` 가 이미 내부 블록에 선언되어 있어 쉐도잉 충돌.
  새 선언 이름 `client` 로 변경하여 회피.

**완료 전 검증 체크리스트 (F-1/F-2/F-3 기준)**:
- [근거] `SmartMovingOptions.md` L844-L903 (resetForNewGame + initializeForGameIfNeccessary) 확인 ✓
- [근거] `SmartMovingContext.md` L264 호출 위치 확인 ✓
- [대응] 원본 switch 4분기 ↔ 구현 switch 4분기(상수 치환) 1:1 ✓
- [분기] 캐시 비교 조기 반환 / switch / `!defaultKey.isEmpty()` 가드 / `setKeys` / `setCurrentKey` ✓
- [상수] Survival/Creative/Adventure 정수값 원본과 일치 (GameMode.getId() 와도 일치) ✓
- [타이밍] 원본 `interceptTick` → 1.21.1 `tickEssential` 시작부 (동등) ✓
- [근사] 리플렉션 `currentGameType` → GameMode.getId() 주입. controller null → 호출자.
  채팅 초기화 블록 범위 초과. javadoc 에 "1.21.1 차이" 명시 ✓
- [신규] 채팅 초기화 블록 미이식 — §16 에 추가 기록
- [회귀] 기존 `tickEssential` 로직 영향 없음 — 시작부에 추가만. `MinecraftClient mc` 쉐도잉
  충돌은 새 변수명 `client` 로 회피. 다른 호출처 없음.
- [빌드] `./gradlew build` ✓

**다음 작업**: G 섹션 — 회귀 방지 감사 + checklist_original_audit.md 신규 발견 기록 +
playtest_fixes.md 포커스 #6 전환.

### 세션 12 — 2026-04-23 — G-1 / G-3

**진행한 작업**:
- **G-1** `./gradlew build` 재실행 → 성공. F 섹션 완료 이후 변경 없음 확인.
- **G-3** §14 회귀 방지 감사 수행:
    - `grep "// TODO\|// \[미확인\]" src/main/java/choco/ratel/smartmoving/config/
       src/main/java/choco/ratel/smartmoving/server/` → **0건** (통과).
    - `grep "INSTANCE\.toggle"` → 2곳 (`SmartMovingServer.adminToggleConfig:244`,
      `SmartMovingClientState:492`). 양쪽 모두 4상태 순환 의미로 정상 작동.
    - `grep "\.enabled"` → 12개 파일 31개 참조. `SmartMovingConfig.java` 의 4개 중 1개는
      선언·파생 계산(L225/L252/L614/L621), 1개는 `readFrom` L413. 나머지는 모두 read-only
      소비. 의미 일치.
    - `readFrom` L413 `enabled = getBool(p, "move.enabled", ...)`: 파일 저장된 값과 tick 내
      `initializeForGameIfNeccessary` 파생값 충돌 여부 검토 → 첫 tick 에서 gameType 판정 시
      `setKeys → toggler=0 → enabled=true` 로 항상 덮어써짐. readFrom 은 무해(무의미). 정리
      필요하지만 포커스 #5 직접 영향 없음.
- §14 의 5개 기존 이식 영향 표 체크:
    - adminToggleConfig ✓, cfg.enabled 의미 ✓, isSneakToggleEnabled 불변 ✓
    - broadcastConfig △: `toggler` 자체 전송 안 됨. 게임타입별 keys 6개로 대체 전송.
      실제 Config 값 전파는 정상, key 라벨 표시만 서버→클라 단절. §17 후속.
    - processConfigContentPacket △: 위와 동일 구조. SERVER_CONFIG 경로에서 gameType
      초기화 경로 없어 `toggler=-2` 유지 → getCurrentKey 방어 경로(null). §17 후속.
- **§16 세션 12 추가**: toggler 영속화/동기화 부재 2건 기록.
- **§17 추가**: 후속 포커스 후보 `focus_08_toggle_persistence.md` 로 영속화 + 동기화
  묶음 제안. Options.toggle override 추가 동작(세션 5)과 함께 해결.

**완료 전 검증 체크리스트 (G-1+G-3 기준)**:
- [근거] §14 체크리스트 항목별 grep/검토 수행 ✓
- [대응] 원본 1:1 대응은 C/D/E/F 에서 이미 완료 — G 는 검증 성격
- [분기] 회귀 발생한 분기 없음 ✓
- [상수] 원본 상수 변경 없음 ✓
- [타이밍] 기존 호출 타이밍 불변 ✓
- [근사] 신규 근사 없음 (§16 에 구조적 누락만 기록)
- [신규] toggler 영속화/동기화 2건 §16 기록 + §17 후속 추가 ✓
- [회귀] 확인 결과: 0건 (구조적 누락은 기존 회귀 아닌 포커스 #5 범위 외 잔여)
- [빌드] `./gradlew build` ✓

**다음 작업**: G-4 — `checklist_original_audit.md` 신규 발견 표에 "4상태 토글 복원" 기록.
G-2 는 사용자 수동 검증 대기. G-5 는 G-2 통과 후.

### 세션 12 (계속) — 2026-04-23 — G-4

**진행한 작업**:
- `docs/checklist_original_audit.md` 신규 발견 표 끝에 포커스 #5 전체 요약 1행 추가.
- 구조: 발견일 / 소스 3파일 / [오역/누락] 태그 + 이전 L1092 오판 명시적 정정 / 처리 내역
  (B-1~4 / C-1~7 / D-1 / E-1~3 / F-1~3 각 섹션 작업 내용 요약) / 잔여 §17 후속 포인터.
- 신규 발견은 "이전 감사에서 `단일 key=on/off 등가` 오판" 을 명시적으로 기록 — 재발 방지용.

**완료 전 검증 체크리스트 (G-4 기준)**:
- [근거] focus_05 §10 B~F 모든 [x] 작업 내용 요약 확인 ✓
- [대응] 해당 없음 (문서 기록 작업)
- [분기] 해당 없음
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] 기록 자체가 기존 신규 발견 정리
- [회귀] 없음 (문서만)
- [빌드] 코드 변경 없음

**다음 작업**: G-2 (사용자 수동 테스트 대기). 통과 확인 후 G-5 (포커스 #6 전환).

---

## 16. 신규 발견 (구현 중 발견한 누락/오역)

### 세션 12 (2026-04-23) — G-3 회귀 감사 중 발견

- **`toggler` 서버→클라 동기화 누락**: `SmartMovingConfig.toArray()/loadFromArray()` 가
  `toggler` 값을 네트워크에 포함하지 않음. 서버 관리자가 `/smoving config toggle` 로 상태를
  바꿔도 클라이언트의 `SERVER_CONFIG.toggler` 는 초기 -2 로 유지. 실제 이동 수치 설정은
  `survival/creative/adventure ConfigKeys` 6개로 전송되어 문제 없지만, "현재 어떤 key
  (Easy/Medium/Hard) 가 선택됐는지" 라벨 표시는 서버→클라 전파 안 됨. 구조적 한계 —
  `move.config.toggler` 같은 키를 `writeTo/readFrom` 에 추가하거나, key 전용 serialize
  필드 필요. 포커스 #5 범위 초과 — §17 후속.

- **`SERVER_CONFIG` 경로에서 gameType 초기화 부재**: `tickEssential` 의
  `initializeForGameIfNeccessary` 호출은 `Config == INSTANCE` 가드로 로컬 설정에서만 작동.
  서버 수신 설정(`SERVER_CONFIG`) 은 gameType 초기화 경로 없음 → `toggler=-2` 유지 →
  `getCurrentKey()` 는 방어 코드로 null 반환. `enabled = (toggler != -1)` 이므로 `-2` 는
  `enabled=true` 로 간주 (-1 이 아니므로). logConfigState 는 "default server configuration"
  출력 (null currentKey 경로). 기능 상 동작하지만 key 라벨 표시 안 됨. 포커스 #5 범위 초과
  — §17 후속.

### 세션 11 (2026-04-23) — F-1/F-2/F-3 중 발견

- **`_configChatInit` / `_speedChatInit` 채팅 초기화 블록 미이식**: 원본
  `initializeForGameIfNeccessary()` L893-L902 에서 gameType 전환 직후:
  (1) `_configChatInit.value` 면 `writeClientConfigMessageToChat(false)` 호출
  (2) `isUserSpeedEnabled() && _speedChatInit.value && speedPercent != defaultSpeedPercent`
      이면 `"move.speed.chat.client.init"` 메시지 출력
  → **현재 이식 범위 밖**. F 섹션은 gameType 기반 keys/defaultKey 적용만 포함. 이 두 분기는
  별도 원자 작업 (속성 `_configChatInit`/`_speedChatInit` 도 미이식) 으로 분리 권장.
  포커스 #5 범위 초과 — 후속 포커스 또는 별도 원자 작업.

### 세션 5 (2026-04-23) — C-1 중 발견

- **Options.toggle() override 추가 동작 미이식** (§10 C-7 후속 원자 작업 신규 등록 필요):
  원본 SmartMovingOptions.toggle() (L500-L531) 은 super.toggle() 호출 후:
  (1) `_configChat.value` 면 `writeClientConfigMessageToChat(false)` 호출 — 채팅 피드백
  (2) gameType switch 로 해당 게임타입의 `_*DefaultConfigKey.setValue(getCurrentKey())` — 현재
      key 를 gameType 별 defaultKey 에 저장 (다음 게임 시작 시 복원용)
  (3) `saveToOptionsFile(optionsPath)` — 파일 저장 (1.21.1 save() 에 해당)
  (1)(2)(3) 중 (3) 은 C-1 에서 이미 처리. (1) 은 E 섹션의 4상태 채팅 피드백과 결합 예정.
  (2) 는 F 섹션 gameType 판정 완료 후 구현. **§10 에 C-7 추가** 또는 F-4 로 편입 고려.

- **`SmartMovingConfig.load()` 에서 toggler 초기화 누락**: 원본 `SmartMovingProperties.load()`
  (L35-L55) 끝에서 `toggler = 0; update();` 호출. 1.21.1 `SmartMovingConfig.load()` 는 file
  읽고 `INSTANCE.readFrom(props)` 만 호출 — `toggler = 0` 설정 없음. 이 때문에 F 섹션 완료 전
  토글 동작이 비정상. F 이식 시 `load()` 에 `toggler = 0; updateToggler();` 추가 필요.

### 세션 2 (2026-04-23) — 리서치/Agent 혼동 주의

- **`net.smart.properties.Property` ≠ `net.smart.moving.config.SmartMovingProperties`**:
  서로 다른 클래스. Agent WebFetch 시 경로 혼동 가능성 있음. SmartMovingProperties
  는 SmartMoving 전용 추상 클래스로 `toggle`/`setKeys`/`toggler`/`keys` 를 정의.
  `net.smart.properties.Property` 는 단일 Property 래퍼로 `update(key)`/`getCurrentKey()`
  (version source 기반) 등 전혀 다른 API.
- **교훈**: WebFetch 프롬프트 작성 시 패키지 전체 경로 명시 필수. 리서치 파일의
  "소스" URL 이 이미 있으므로 리서치 파일 먼저 확인하면 Agent 호출 불필요한 경우가 많음.
- **영향**: 추후 포커스에서도 리서치 파일 우선 확인 원칙 재강조.

---

## 17. 잔여 / 후속

### 이 포커스 범위 초과 — 새 포커스 후보

- **key 별 값 차별화** (`_swim.e=true / _swim.h=false`): Property 시스템 전체 이식 필요 →
  별도 포커스 후보이지만 1.21.1 구조상 N/A 권장.
- **`_survivalDefaultConfigUserKeys` 플레이어별 config key 맵**: `writeToProperties(player, toggle)`
  ↔ `getPlayerConfigurationKey` — 이 포커스 완료 후 `focus_07_player_config_key.md` 로 분리 가능.
- **gamemode 변경 이벤트 훅** (survival↔creative 전환 시 setKeys 재호출): tick 폴링으로
  충분 (F-3 에서 해결) — 이벤트 훅 불필요.
- **`toggler` / 현재 key 영속화 + 서버→클라 동기화** (세션 12 발견): 두 가지 묶음.
  (1) 파일 저장 시 현재 key 를 gameType 별 defaultConfigKey 에 기록 (원본 Options.toggle
      override 의 L500-L531 동작) — 다음 로드 시 `setCurrentKey(defaultConfigKey)` 로 복원.
  (2) `toArray()` 에 현재 key 또는 toggler 포함 → 서버→클라 라벨 동기화.
  Options.toggle 추가 동작 (§16 세션 5) 과 묶어서 `focus_08_toggle_persistence.md` 로 분리 가능.
- **`_configChatInit` / `_speedChatInit` 채팅 초기화** (세션 11 발견): `initializeForGameIfNeccessary`
  끝의 채팅 초기화 블록. 현재 미이식. 별도 속성 `_configChatInit` 등도 신설 필요.
