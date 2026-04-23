# Focus #5 — 옵션토글 4단계 순환 (disabled / easy / medium / hard)

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스 #5.
> 이 문서 + playtest_fixes.md 2개만 열고 작업한다.

---

## 진행 상황

**상태**: 🟡 진행 중

**현재 단계**: ⏳ 원본 근거 수집 — Agent WebFetch 대기

---

## 증상

원본(1.7.10)은 `configToggle` 키바인딩 또는 `/smoving config toggle` 커맨드로
**disabled → easy → medium → hard → disabled** 4상태 순환. 현재 1.21.1 구현은
**on/off 2상태** 토글만 동작.

## 이전 판단 오류 (내 오판 기록)

이전 세션에서 "단일 config key 시스템(1.21.1 '_default' key 단순 on/off 토글)이
원본 `toggler==0 ↔ -1` 순환과 기능 동등"이라고 판단했지만 **틀렸음**.

원본은 `configKeys = {"e", "m", "h"}` 길이 3 배열 + `toggler` 순환으로 4상태를
구현. `toggler 0 → 1 → 2 → -1 → 0` 순환:
- `toggler = -1`: disabled
- `toggler = 0`: easy (key="e")
- `toggler = 1`: medium (key="m")
- `toggler = 2`: hard (key="h")

---

## 원본 근거

### 이미 확보된 정보 (리서치 파일)

**SmartMovingConfig.md L610-L620**:
```java
_survivalConfigKeys       = Strings(...).singular().defaults({"e", "m", "h"});
_survivalDefaultConfigKey = String(...).singular().defaults("m");
_creativeConfigKeys       = Strings(...).singular().defaults({"c"});
_creativeDefaultConfigKey = String(...).singular().defaults("c");
_adventureConfigKeys      = Strings(...).singular().defaults({"e", "m", "h"});
_adventureDefaultConfigKey = String(...).singular().defaults("m");
_configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"));
```

**SmartMovingProperties.md L325-L332 `toggle()`**:
```java
public void toggle() {
    int length = keys == null ? 0 : keys.length;
    toggler++;
    if (toggler == length) toggler = -1;
    update();
}
```

**SmartMovingProperties.md L345-L355 `setKeys(String[] keys)`**:
```java
public void setKeys(String[] keys) {
    if (keys == null || keys.length == 0)
        keys = _defaultKeys;
    this.keys = keys;
    toggler = 0;
    update();
}
```

### 추가 확보 필요 (Agent WebFetch 대상)

| 항목 | 필요한 이유 |
|------|------------|
| `SmartMovingProperties.setCurrentKey(String)` 본체 | toggler 를 key 이름으로 설정하는 방법 |
| `SmartMovingProperties.getCurrentKey()` 본체 | toggler 값으로 key 이름 반환 |
| `SmartMovingProperties.getNextKey(String)` 본체 | 다음 key 로 순환하는 정확한 로직 |
| `SmartMovingProperties.hasKey(String)` 본체 | key 유효성 검증 |
| `SmartMovingProperties.update()` 본체 | toggler 변경 후 enabled 재계산 방식 |
| `SmartMovingOptions.toggle()` override 본체 | Options 측의 toggle 확장 동작 (저장/로그 등) |
| `SmartMovingOptions.initializeForGameIfNeccessary()` | 게임타입별 keys 설정 진입점 |
| `SmartMovingConfig.setKeys` / `setCurrentKey` 호출 맥락 | 생성자 흐름에서 언제 호출되는지 |

---

## 원자 단위 작업 목록

### A. 원본 소스 확보 (Agent WebFetch)
- [ ] `SmartMovingProperties.java` 에서 `setKeys` / `setCurrentKey` / `getCurrentKey` / `getNextKey` / `hasKey` / `update` / `toggle` / `toggler` 사용처 전체 덤프
- [ ] `SmartMovingOptions.java` 의 `toggle()` override + `initializeForGameIfNeccessary()` 전체 덤프
- [ ] 덤프 결과를 리서치 파일(`SmartMovingProperties.md` / `SmartMovingOptions.md` / `SmartMovingConfig.md`)에 보완 반영

### B. SmartMovingConfig 필드/메서드 이식
- [ ] `SmartMovingConfig` 에 필드 추가:
  - `public String[] configKeys = {"e", "m", "h"};` (기본 Survival)
  - `public int toggler = ?;` (원본 기본값 확인 필요 — `_survivalDefaultConfigKey="m"` → toggler=1?)
  - `public String currentConfigKey = "m";`
  - `public Map<String,String> configKeyName = {"":"", "e":"Easy", "m":"Medium", "h":"Hard"};`
- [ ] readFrom/writeTo 확장 (config 파일 저장)
- [ ] `setKeys(String[] keys)` 메서드 — 원본 L345 1:1
- [ ] `setCurrentKey(String key)` 메서드
- [ ] `getCurrentKey()` 메서드 — toggler==-1 시 null 반환
- [ ] `getNextKey(String key)` 메서드
- [ ] `hasKey(String key)` 메서드

### C. SmartMovingConfig.toggle() 재구현
- [ ] 현재 `enabled = !enabled; save();` 를 원본 L325-L332 toggle() 로 교체:
  ```java
  int length = configKeys == null ? 0 : configKeys.length;
  toggler++;
  if (toggler == length) toggler = -1;
  // update(): enabled = (toggler != -1); currentConfigKey = (toggler>=0 ? configKeys[toggler] : null)
  save();
  ```
- [ ] `update()` 헬퍼 메서드 (toggler → enabled/currentConfigKey 재계산)

### D. 로그/UI 반영
- [ ] `SmartMovingServer.logConfigState()` 의 `currentKey != null` 경로 활성화 (원본 L352-L366):
  ```
  currentKey==null  → "uses default server configuration"
  configName==""    → "uses server configuration with key \"m\""
  configName="Easy" → "uses server configuration \"Easy\""
  ```
- [ ] `logConfigState(cfg, username, reconfig=true)` 호출 시 위 분기 적용 확인

### E. 클라이언트 반영
- [ ] `SmartMovingClientState.tickEssential()` 의 `configToggle.wasPressed()` 처리 경로 확인 — 싱글플레이 시 `INSTANCE.toggle()` 호출 (이미 존재), 새 toggler 순환 로직 자동 적용
- [ ] 채팅 피드백 메시지 4상태 반영:
  - 원본: enabled=false → "disabled"
  - enabled=true && currentKey==null → "default configuration"
  - enabled=true && configKeyName!="" → configKeyName (Easy/Medium/Hard)
  - 현재 번역 키: `smartmoving.message.config.client.enabled` / `.disabled` 2개만 → 3개 추가 필요

### F. 서버측 관리자 경로
- [ ] `SmartMovingServer.adminToggleConfig(player)` 는 `INSTANCE.toggle()` 호출 중 → 자동으로 새 순환 로직 적용 (코드 변경 불필요)
- [ ] `broadcastConfig(server)` 호출로 모든 접속자에게 새 상태 전파 (이미 존재)

### G. 검증 / 컴파일
- [ ] `./gradlew build` 성공
- [ ] 완료 전 검증 체크리스트 통과 (아래)
- [ ] 체크리스트(`checklist_original_audit.md`) 신규 발견 표에 처리 완료 기록
- [ ] playtest_fixes.md 의 "현재 포커스" 를 #6 으로 갱신

---

## 완료 전 검증 체크리스트 (이 포커스 기준)

- [ ] `SmartMovingProperties.toggle()` L325-L332 원본과 `SmartMovingConfig.toggle()` 1:1 대응
- [ ] `setKeys` 호출 시 toggler=0 초기화 (원본 L353)
- [ ] `toggler == length` 조건이 `==` 인지 `>=` 인지 원본 그대로 (`==`)
- [ ] `update()` 에서 enabled / currentConfigKey 재계산이 원본과 일치
- [ ] `logConfigState` 의 4가지 출력 경로 전부 동작 (disabled / default / with key "m" / "Medium")
- [ ] 클라이언트 채팅 피드백이 4상태를 구분하여 표시
- [ ] 컴파일 성공
- [ ] 구현 중 발견한 신규 누락은 아래 "신규 발견" 섹션에 기록

---

## 작업 기록 (시간순 로그)

### 세션 1 — 2026-04-23

**진행한 작업**:
- 진입점 문서(`playtest_fixes.md`) 생성
- 이 포커스 파일(`focus_05_config_toggle.md`) 생성

**다음 작업**: A 섹션 — Agent WebFetch 로 원본 `SmartMovingProperties` / `SmartMovingOptions` 코드 확보

---

## 신규 발견 (구현 중 발견한 누락/오역)

_(비어있음 — 작업 진행하며 기록)_

---

## 잔여 / 후속

_(비어있음 — 작업 진행하며 기록)_
