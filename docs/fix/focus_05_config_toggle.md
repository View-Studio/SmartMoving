# Focus #5 — 옵션토글 4단계 순환 (disabled / easy / medium / hard)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스 #5.
> 이 문서 + playtest_fixes.md 2개만 열고 작업한다.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | 🟡 진행 중 |
| 현재 단계 | ✅ A 완료 (리서치 파일만으로 충분) / ⏳ B-1 대기 |
| 이전 판단 오류 | ⚠️ 기록됨 — 2-"이전 판단 오류" 참조 |
| 컴파일 상태 | (수정 전) 정상 |

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
- [ ] B-2. `toggler` 필드 (int, default **-2** — 원본 L30 초기값 그대로, `load()` 후 0, `setCurrentKey(default)` 후 특정 인덱스)
- [ ] B-3. `configKeyName` 필드 (Map, default 4개 매핑)
- [ ] B-4. `readFrom`/`writeTo` 에 `move.config.key.current` / `move.config.keys` 추가 (원본 저장 포맷 확인 필요 — A-1/A-2 결과 기반)

### C. `SmartMovingConfig` 메서드 이식
- [ ] C-1. `toggle()` 재구현 — 원본 L325-L332 1:1
- [ ] C-2. `setKeys(String[])` 이식 — 원본 L345-L355 1:1
- [ ] C-3. `updateToggler()` 헬퍼 — `enabled = (toggler != -1)`
- [ ] C-4. `getCurrentKey()` — toggler == -1 시 null
- [ ] C-5. `getNextKey(String)` — A-1 확보 후 1:1 이식
- [ ] C-6. `hasKey(String)` — configKeys 검색

### D. 서버 로그 4상태 확장
- [ ] D-1. `SmartMovingServer.logConfigState` 의 `currentKey==null` / `configName==""` / `configName!=""` 3갈래 분기 복원

### E. 클라이언트 채팅 피드백 4상태
- [ ] E-1. `SmartMovingClient.tickEssential` configToggle 블록의 msgKey 분기 4상태 확장
- [ ] E-2. `en_us.json` 에 `smartmoving.message.config.client.default` / `.named` 추가
- [ ] E-3. 기존 `.enabled` / `.disabled` 번역 키는 유지 (호환)

### F. 게임타입별 configKeys 적용
- [ ] F-1. `SmartMovingServer.initialize` 에서 `player.interactionManager.getGameMode()` 로 gameType 판정
- [ ] F-2. gameType → setKeys 호출 (survival/adventure → "e,m,h", creative → "c")
- [ ] F-3. gameType 변경 시 setKeys 재호출 여부 확인 — gamemode 변경 이벤트 훅 필요할 수도 (신규 발견 대상)

### G. 검증
- [ ] G-1. `./gradlew build` 성공
- [ ] G-2. 재현 케이스 표 모든 행 검증 (수동 테스트)
- [ ] G-3. 회귀 방지 감사 (섹션 14)
- [ ] G-4. `checklist_original_audit.md` 신규 발견 표에 "4상태 토글 복원" 기록
- [ ] G-5. `playtest_fixes.md` 의 "현재 포커스" 를 `#6` 으로 갱신

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
| `SmartMovingServer.adminToggleConfig` | `INSTANCE.toggle()` 호출 — 새 순환 자동 반영 | [ ] 커맨드 `/smoving config toggle` 4상태 정상 |
| `SmartMovingServer.broadcastConfig` | 모든 접속자에게 재전송 — toggler 값이 `INSTANCE.toArray()` 에 포함되는지 | [ ] readFrom/writeTo 에 toggler 저장 확인 |
| `cfg.enabled` 참조 위치 모두 | 의미 유지 (enabled = toggler != -1) | [ ] `grep -rn "cfg.enabled\|Config.enabled" src/` 결과 전부 의미 맞음 |
| `isSneakToggleEnabled` / `isCrawlToggleEnabled` | `cfg.sneakToggle && cfg.enabled` — 원본과 동일 | [ ] 변경 없음 확인 |
| `SmartMovingClient.processConfigContentPacket` | 서버가 보낸 toggler 값으로 SERVER_CONFIG 갱신 | [ ] 파일 포맷에 key/toggler 포함 |

### 공통 grep 스캔

- [ ] `grep -rn "// TODO\|// \[미확인\]" src/main/java/choco/ratel/smartmoving/config/ src/main/java/choco/ratel/smartmoving/server/` — 추가 TODO 없음
- [ ] `grep -rn "cfg.toggle\|INSTANCE.toggle" src/` — 새 toggle() 의미로 쓰이는지 확인

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

---

## 16. 신규 발견 (구현 중 발견한 누락/오역)

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
- **gamemode 변경 이벤트 훅** (survival↔creative 전환 시 setKeys 재호출): Fabric 이벤트 확인 필요.
