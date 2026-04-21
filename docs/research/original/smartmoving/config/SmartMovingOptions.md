# SmartMovingOptions.java (net.smart.moving.config) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/config/SmartMovingOptions.java  
패키지: `net.smart.moving.config`  
종류: `class`  
상속: `SmartMovingClientConfig` (extends)

상속 계층: `SmartMovingOptions → SmartMovingClientConfig → SmartMovingConfig → SmartMovingProperties`

---

## 전체 소스

```java
package net.smart.moving.config;

import java.io.*;
import java.lang.reflect.*;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.resources.*;
import net.minecraft.client.settings.*;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings.*;

import net.smart.moving.*;
import net.smart.properties.*;
import net.smart.utilities.*;

public class SmartMovingOptions extends SmartMovingClientConfig
{
    public final Property<Boolean> _localUserHasChangeConfigRight = Unmodified("move.global.config.right.local.user")
        .comment("Whether the current local user has the right to change the global configuration in-game ...").section();
    public final Property<Boolean> _localUserHasChangeSpeedRight = Unmodified("move.global.speed.right.local.user")
        .comment("Whether the current local user has the right to change the global speed in-game ...");

    public final Property<Float> _perspectiveFadeFactor = PositiveFactor("move.perspective.fade.factor").values(0.5F, 0.1F, 1F)
        .comment("Fading speed factor between the different perspectives (>= 0.1, <= 1, set to '1' to switch off)").book("Viewpoint perspective", ...);
    public final Property<Float> _perspectiveSpeedFactor = Float("move.perspective.speed.factor").defaults(1F)
        .comment("Movement speed-based perspective factor (set to '0' to switch off)");
    public final Property<Float> _perspectiveSpeedFactorMax = PositiveFactor("move.perspective.speed.factor.max").defaults(0F)
        .comment("Maximum movement speed-based FOV change. [1-x, 1+x]. (>= 0, set to '0' to uncap)");
    public final Property<Float> _perspectiveRunFactor = Float("move.perspective.run.factor")
        .key("move.run.perspective.factor", _pre_sm_2_1).defaults(1F)
        .comment("Standard sprinting perspective (set to '0' to switch off)");
    public final Property<Float> _perspectiveSprintFactor = Float("move.perspective.sprint.factor")
        .key("move.sprint.perspective.factor", _pre_sm_2_1).defaults(1.5F)
        .comment("Smart on ground sprinting perspective (set to '0' to switch off)");

    public final Property<Float> _angleJumpDoubleClickTicks = Positive("move.jump.angle.double.click.ticks")
        .singular().up(3F, 2F).comment("The maximum number of ticks between two clicks to trigger a side or back jump (>= 2)")
        .book("User interface", ...);

    public final Property<Boolean> _wallJumpDoubleClick = Unmodified("move.jump.wall.double.click").singular()
        .comment("Whether wall jumping should be triggered by single or double clicking ...").section();
    public final Property<Float> _wallJumpDoubleClickTicks = Positive("move.jump.wall.double.click.ticks")
        .singular().up(3F, 2F).comment("The maximum number of ticks between two clicks to trigger a wall jump (>= 2, ...)");

    public final Property<Boolean> _climbJumpBackHeadOnGrab = Unmodified("move.jump.climb.back.head.on.grab").singular()
        .comment("Whether pressing or not pressing the grab button while climb jumping back results in a head jump").section();

    public final Property<String> _sprintKeyReleaseAction = String().defaults("run").key("move.sprint.key.release.action")
        .comment("Determines what should happen when the sprint key is released (possible values are \"run\" and \"walk\")").section();

    public final Property<Boolean> _runOnSprintRelease  = _sprintKeyReleaseAction.is("run").and(_run);
    public final Property<Boolean> _walkOnSprintRelease = _sprintKeyReleaseAction.is("walk").andNot(_runOnSprintRelease);

    public final Property<Boolean> _displayExhaustionBar = Unmodified("move.gui.exhaustion.bar").singular()
        .comment("Whether to display the exhaustion bar in the game overlay").section();
    public final Property<Boolean> _displayJumpChargeBar = Unmodified("move.gui.jump.charge.bar").singular()
        .comment("Whether to display the jump charge bar in the game overlay");

    public final Property<Boolean> _sneakToggle = Modified("move.sneak.toggle").comment("To switch on/off sneak toggling").section();
    public final Property<Boolean> _crawlToggle = Modified("move.crawl.toggle").comment("To switch on/off crawl toggling");

    public final Property<Boolean> _flyCloseToGround  = Modified("move.fly.ground.close").comment("To switch on/off flying close to the ground").section();
    public final Property<Boolean> _flyWhileOnGround  = Modified("move.fly.ground.collide").depends(_flyCloseToGround)
        .comment("To switch on/off flying while colliding with the ground (Relevant only if \"move.fly.ground.close\" is true)");

    public final Property<Boolean> _flyControlVertical  = Unmodified("move.fly.control.vertical")
        .comment("Whether flying control also depends on where the player looks vertically.").section();
    public final Property<Boolean> _diveControlVertical = Unmodified("move.dive.control.vertical")
        .comment("Whether diving control also depends on where the player looks vertically.");

    private final Property<Integer> _old_toggleKeyCode = Integer("move.toggle.key", _pre_sm_1_7).singular().defaults(67);
    private final Property<String> _defaultConfigToggleKeyName = String("move.config.toggle.default.key.name")
        .key("move.toggle.key.name", _pre_sm_3_2).singular().defaults("F9")
        .source(_old_toggleKeyCode.toKeyName(), _pre_sm_1_7).singular()
        .comment("Key name to toggle Smart Moving features in-game (default: \"F9\")").section();
    private final Property<String> _defaultGrabKeyName = String("move.grab.default.key.name").singular().defaults("LCONTROL")
        .singular().comment("Default key name to \"grab\" (default: \"LCONTROL\")");
    private final Property<String> _speedIncreaseKeyName = String("move.speed.increase.default.key.name")
        .key("move.speed.increase.key.name", _pre_sm_3_2).singular().defaults("O").singular()
        .comment("Key name to increase the moving speed ingame (default: \"O\")");
    private final Property<String> _speedDecreaseKeyName = String("move.speed.decrease.default.key.name")
        .key("move.speed.decrease.key.name", _pre_sm_3_2).singular().defaults("I").singular()
        .comment("Key name to decrease the moving speed ingame (default: \"I\")");

    public final Property<Integer> _defaultConfigToggleKeyCode = _defaultConfigToggleKeyName.toKeyCode(67);
    public final Property<Integer> _defaultGrabKeyCode         = _defaultGrabKeyName.toKeyCode(29);
    public final Property<Integer> _defaultSpeedIncreaseKeyCode = _speedIncreaseKeyName.toKeyCode(24);
    public final Property<Integer> _defaultSpeedDecreaseKeyCode = _speedDecreaseKeyName.toKeyCode(23);

    public final Property<Boolean> _configChat       = Unmodified("move.config.chat").singular().comment(...).book("Message Management", ...);
    public final Property<Boolean> _configChatInit   = Unmodified("move.config.chat.init").depends(_configChat).singular().comment(...);
    public final Property<Boolean> _configChatInitHelp = Unmodified("move.config.chat.init.help").depends(_configChatInit).singular().comment(...);
    public final Property<Boolean> _configChatServer = Unmodified("move.config.chat.server").depends(_configChat).singular().comment(...);

    public final Property<Boolean> _speedChat       = Unmodified("move.speed.chat").singular().comment(...).section();
    public final Property<Boolean> _speedChatInit   = Unmodified("move.speed.chat.init").depends(_speedChat).singular().comment(...);
    public final Property<Boolean> _speedChatInitHelp = Unmodified("move.speed.chat.init.help").depends(_speedChatInit).singular().comment(...);
    public final Property<Boolean> _speedChatServer = Unmodified("move.config.chat.server").depends(_speedChat).singular().comment(...);

    public KeyBinding keyBindGrab;
    public KeyBinding keyBindConfigToggle;
    public KeyBinding keyBindSpeedIncrease;
    public KeyBinding keyBindSpeedDecrease;

    public static final File optionsPath = net.minecraft.client.Minecraft.getMinecraft().mcDataDir;

    // ... (생성자, 메서드 아래 섹션 참고)

    public static boolean hasRedPowerWire           = false;
    public static boolean hasBuildCraftTransportation = false;
    public static boolean hasFiniteLiquid           = false;
    public static boolean hasBetterThanWolves       = false;
    public static boolean hasSinglePlayerCommands   = false;
    public static boolean hasRopesPlus              = false;
    public static boolean hasASGrapplingHook        = false;
    public static boolean hasBetterMisc             = false;

    public int gameType;

    private static Field _currentGameType = Reflect.GetField(PlayerControllerMP.class, SmartMovingInstall.PlayerControllerMP_currentGameType);
}
```

---

## 역할

`SmartMovingClientConfig`를 상속하는 최상위 클라이언트 설정 클래스. 클라이언트에서만 필요한 추가 설정(시점 원근, UI, 키 바인딩, 채팅 메시지)과 게임 런타임 상태(`gameType`, 모드 호환 플래그)를 관리한다.

`SmartMovingContext.Options`가 이 클래스의 인스턴스를 참조한다.

---

## import

```java
import java.io.*;
import java.lang.reflect.*;
import net.minecraft.client.*;                   // Minecraft, KeyBinding
import net.minecraft.client.gui.*;              // GuiNewChat
import net.minecraft.client.multiplayer.*;      // PlayerControllerMP
import net.minecraft.client.resources.*;        // I18n
import net.minecraft.client.settings.*;         // (KeyBinding 등)
import net.minecraft.util.*;                    // ChatComponentText
import net.minecraft.world.WorldSettings.*;     // GameType (inner class)
import net.smart.moving.*;                      // SmartMovingContext, SmartMovingInfo, SmartMovingInstall
import net.smart.properties.*;
import net.smart.utilities.*;                   // Reflect
```

---

## Property 필드

### 1. 로컬 사용자 권한

```java
public final Property<Boolean> _localUserHasChangeConfigRight =
    Unmodified("move.global.config.right.local.user").section();
public final Property<Boolean> _localUserHasChangeSpeedRight =
    Unmodified("move.global.speed.right.local.user");
```

`LocalUserNameProvider.getLocalConfigUserName()` / `getLocalSpeedUserName()`에서 `.value`로 직접 읽음.

---

### 2. Viewpoint Perspective (시점 원근)

```java
public final Property<Float> _perspectiveFadeFactor     = PositiveFactor("move.perspective.fade.factor").values(0.5F, 0.1F, 1F);
// default 0.5F, min 0.1F, max 1F — 1이면 원근 전환 즉시 (fading 없음)

public final Property<Float> _perspectiveSpeedFactor    = Float("move.perspective.speed.factor").defaults(1F);
// 이동 속도 기반 원근 배율, 0이면 비활성

public final Property<Float> _perspectiveSpeedFactorMax = PositiveFactor("move.perspective.speed.factor.max").defaults(0F);
// FOV 변화 최대치 [1-x, 1+x], 0이면 무제한

public final Property<Float> _perspectiveRunFactor      = Float("move.perspective.run.factor")
    .key("move.run.perspective.factor", _pre_sm_2_1).defaults(1F);
// 바닐라 달리기(run) 원근, 0이면 비활성

public final Property<Float> _perspectiveSprintFactor   = Float("move.perspective.sprint.factor")
    .key("move.sprint.perspective.factor", _pre_sm_2_1).defaults(1.5F);
// 스마트 질주(sprint) 원근
```

`Float("...")` 팩토리 — `PositiveFactor`와 달리 음수 허용.

---

### 3. User Interface (UI 설정)

#### 각도 점프 더블클릭

```java
public final Property<Float> _angleJumpDoubleClickTicks =
    Positive("move.jump.angle.double.click.ticks").singular().up(3F, 2F);
// default 3F, min 2. Math.ceil()로 정수화하여 사용
```

#### 벽 점프 더블클릭

```java
public final Property<Boolean> _wallJumpDoubleClick =
    Unmodified("move.jump.wall.double.click").singular();
// true: 더블클릭 후 홀드로 벽 점프 / false: 싱글클릭+홀드

public final Property<Float> _wallJumpDoubleClickTicks =
    Positive("move.jump.wall.double.click.ticks").singular().up(3F, 2F);
// default 3F, min 2. depends 없음 — 항상 파일에서 읽힘
```

#### 클라이밍 백헤드 점프

```java
public final Property<Boolean> _climbJumpBackHeadOnGrab =
    Unmodified("move.jump.climb.back.head.on.grab").singular();
// true: grab 누를 때 헤드 점프 / false: grab 안 누를 때 헤드 점프
```

#### 스프린트 키 해제 동작

```java
public final Property<String>  _sprintKeyReleaseAction = String().defaults("run").key("move.sprint.key.release.action");
// "run" 또는 "walk"

public final Property<Boolean> _runOnSprintRelease  = _sprintKeyReleaseAction.is("run").and(_run);
// "run" 설정 AND _run 활성화 시 true
public final Property<Boolean> _walkOnSprintRelease = _sprintKeyReleaseAction.is("walk").andNot(_runOnSprintRelease);
// "walk" 설정 AND _runOnSprintRelease가 false일 때 true
```

#### HUD 표시

```java
public final Property<Boolean> _displayExhaustionBar = Unmodified("move.gui.exhaustion.bar").singular();
public final Property<Boolean> _displayJumpChargeBar = Unmodified("move.gui.jump.charge.bar").singular();
```

#### 토글 동작

```java
public final Property<Boolean> _sneakToggle = Modified("move.sneak.toggle");
public final Property<Boolean> _crawlToggle = Modified("move.crawl.toggle");
```

#### 비행 지면 근접

```java
public final Property<Boolean> _flyCloseToGround = Modified("move.fly.ground.close");
public final Property<Boolean> _flyWhileOnGround  = Modified("move.fly.ground.collide").depends(_flyCloseToGround);
// _flyCloseToGround가 false이면 _flyWhileOnGround도 비활성
```

#### 수직 제어

```java
public final Property<Boolean> _flyControlVertical  = Unmodified("move.fly.control.vertical");
public final Property<Boolean> _diveControlVertical = Unmodified("move.dive.control.vertical");
```

---

### 4. 키 바인딩

#### 키 이름 (설정 파일 저장용, private)

```java
private final Property<Integer> _old_toggleKeyCode =
    Integer("move.toggle.key", _pre_sm_1_7).singular().defaults(67);
// v1.7 이전 키코드 정수 저장 방식 (67 = F9)

private final Property<String> _defaultConfigToggleKeyName =
    String("move.config.toggle.default.key.name")
    .key("move.toggle.key.name", _pre_sm_3_2)           // v3.2 이전: 다른 키 이름
    .singular().defaults("F9")
    .source(_old_toggleKeyCode.toKeyName(), _pre_sm_1_7); // v1.7 이전: 정수→키 이름 변환
// _old_toggleKeyCode.toKeyName(): LWJGL 키코드 67 → "F9" 변환

private final Property<String> _defaultGrabKeyName =
    String("move.grab.default.key.name").singular().defaults("LCONTROL");

private final Property<String> _speedIncreaseKeyName =
    String("move.speed.increase.default.key.name")
    .key("move.speed.increase.key.name", _pre_sm_3_2)
    .singular().defaults("O");

private final Property<String> _speedDecreaseKeyName =
    String("move.speed.decrease.default.key.name")
    .key("move.speed.decrease.key.name", _pre_sm_3_2)
    .singular().defaults("I");
```

#### 키코드 (런타임 사용, public)

```java
public final Property<Integer> _defaultConfigToggleKeyCode = _defaultConfigToggleKeyName.toKeyCode(67);
// 키 이름 "F9" → LWJGL 키코드 67. 변환 실패 시 폴백 67

public final Property<Integer> _defaultGrabKeyCode          = _defaultGrabKeyName.toKeyCode(29);
// "LCONTROL" → 29

public final Property<Integer> _defaultSpeedIncreaseKeyCode = _speedIncreaseKeyName.toKeyCode(24);
// "O" → 24

public final Property<Integer> _defaultSpeedDecreaseKeyCode = _speedDecreaseKeyName.toKeyCode(23);
// "I" → 23
```

**LWJGL 키코드 대응표**:

| 키 이름 | 키코드 |
|---------|--------|
| F9 | 67 |
| LCONTROL | 29 |
| O | 24 |
| I | 23 |

`.toKeyCode(fallback)` — 키 이름을 LWJGL 키코드 int로 변환. 변환 실패 시 fallback 사용.

---

### 5. Message Management (채팅 메시지 설정)

```java
public final Property<Boolean> _configChat        = Unmodified("move.config.chat").singular();
// config 관련 채팅 메시지 on/off

public final Property<Boolean> _configChatInit    = Unmodified("move.config.chat.init").depends(_configChat).singular();
// 게임 시작 시 초기 config 상태 메시지

public final Property<Boolean> _configChatInitHelp = Unmodified("move.config.chat.init.help").depends(_configChatInit).singular();
// 초기 도움말 메시지 (개선된 keybinding GUI가 없을 때)

public final Property<Boolean> _configChatServer  = Unmodified("move.config.chat.server").depends(_configChat).singular();
// 멀티플레이어 진입 시 서버 config 메시지

public final Property<Boolean> _speedChat         = Unmodified("move.speed.chat").singular();
public final Property<Boolean> _speedChatInit     = Unmodified("move.speed.chat.init").depends(_speedChat).singular();
public final Property<Boolean> _speedChatInitHelp = Unmodified("move.speed.chat.init.help").depends(_speedChatInit).singular();
public final Property<Boolean> _speedChatServer   = Unmodified("move.config.chat.server").depends(_speedChat).singular();
// 주목: _speedChatServer의 키가 "move.config.chat.server"로 _configChatServer와 동일한 키를 공유
```

`_speedChatServer.key = "move.config.chat.server"` — `_configChatServer`와 같은 키. 동일 설정 값을 두 곳에서 읽는 구조.

---

## non-Property 필드

### KeyBinding 인스턴스

```java
public KeyBinding keyBindGrab;
public KeyBinding keyBindConfigToggle;
public KeyBinding keyBindSpeedIncrease;
public KeyBinding keyBindSpeedDecrease;
```

생성자에서 초기화됨. `SmartMovingMod.init()`에서 Forge 키 바인딩 등록에 사용.

### optionsPath

```java
public static final File optionsPath = net.minecraft.client.Minecraft.getMinecraft().mcDataDir;
```

Minecraft 데이터 디렉터리 (`.minecraft/`). static final이지만 `getMinecraft().mcDataDir` 값 — 클래스 로딩 시 결정.

### 모드 호환 플래그

```java
public static boolean hasRedPowerWire           = false;
public static boolean hasBuildCraftTransportation = false;
public static boolean hasFiniteLiquid           = false;
public static boolean hasBetterThanWolves       = false;
public static boolean hasSinglePlayerCommands   = false;
public static boolean hasRopesPlus              = false;
public static boolean hasASGrapplingHook        = false;
public static boolean hasBetterMisc             = false;
```

`initialize()` 메서드로 설정. `Compat.java`의 static 플래그(`isStarMinerPresent` 등)와는 별개 — 이쪽은 `SmartMovingInstall`이 전달하는 모드 존재 여부.

### gameType

```java
public int gameType;
```

현재 게임 타입 ID. `Unknown(-1)`, `Survival(0)`, `Creative(1)`, `Adventure(2)` (SmartMovingConfig에서 상수 상속).

### _currentGameType (리플렉션 필드)

```java
private static Field _currentGameType =
    Reflect.GetField(PlayerControllerMP.class, SmartMovingInstall.PlayerControllerMP_currentGameType);
```

`PlayerControllerMP`의 `currentGameType` 필드에 대한 리플렉션 Field 객체. `initializeForGameIfNeccessary()`에서 현재 게임 타입을 읽는 데 사용.

---

## 생성자

```java
public SmartMovingOptions()
{
    loadFromOptionsFile(optionsPath);
    saveToOptionsFile(optionsPath);

    keyBindGrab           = new KeyBinding("key.climb",        _defaultGrabKeyCode.value,          "key.categories.gameplay");
    keyBindConfigToggle   = new KeyBinding("key.config.toggle", _defaultConfigToggleKeyCode.value,  "key.categories.smartmoving");
    keyBindSpeedIncrease  = new KeyBinding("key.speed.increase", _defaultSpeedIncreaseKeyCode.value, "key.categories.smartmoving");
    keyBindSpeedDecrease  = new KeyBinding("key.speed.decrease", _defaultSpeedDecreaseKeyCode.value, "key.categories.smartmoving");
}
```

순서:
1. `loadFromOptionsFile(optionsPath)` — 설정 파일 읽기
2. `saveToOptionsFile(optionsPath)` — 즉시 저장 (버전 마이그레이션 결과 반영)
3. KeyBinding 4개 생성

`keyBindGrab` 카테고리: `"key.categories.gameplay"` (일반 게임플레이)  
나머지 3개 카테고리: `"key.categories.smartmoving"` (SmartMoving 전용)

---

## 메서드

### `isSneakToggleEnabled()`

```java
public boolean isSneakToggleEnabled()
{
    return _sneakToggle.value && enabled;
}
```

`enabled` — `SmartMovingContext.enabled`. SM이 비활성이면 false.

---

### `isCrawlToggleEnabled()`

```java
public boolean isCrawlToggleEnabled()
{
    return _crawlToggle.value && enabled;
}
```

---

### `angleJumpDoubleClickTicks()`

```java
public int angleJumpDoubleClickTicks()
{
    return (int)Math.ceil(_angleJumpDoubleClickTicks.value);
}
```

float 설정값을 올림하여 int 반환. 기본값 3F → 3.

---

### `wallJumpDoubleClickTicks()`

```java
public int wallJumpDoubleClickTicks()
{
    return (int)Math.ceil(_wallJumpDoubleClickTicks.value);
}
```

기본값 3F → 3.

---

### `toggle()` (override)

```java
@Override
public void toggle()
{
    super.toggle();   // SmartMovingClientConfig.toggle() → SmartMovingProperties.toggle()

    if(_configChat.value)
        writeClientConfigMessageToChat(false);

    Property<String> defaultKey = null;
    switch(gameType)
    {
        default:
        case Survival:
            defaultKey = _survivalDefaultConfigKey;
            break;
        case Creative:
            defaultKey = _creativeDefaultConfigKey;
            break;
        case Adventure:
            defaultKey = _adventureDefaultConfigKey;
            break;
    }

    if(defaultKey != null)
    {
        String currentKey = getCurrentKey();
        defaultKey.setValue(currentKey);
        saveToOptionsFile(optionsPath);
    }
}
```

순서:
1. `super.toggle()` — config key 순환 (SmartMovingProperties 구현)
2. 채팅 메시지 (설정 활성화 시)
3. 현재 게임 타입의 default key를 현재 key로 업데이트
4. 파일 저장

`gameType`이 `Unknown(-1)`이면 `default` 케이스 → `_survivalDefaultConfigKey` 사용.

---

### `changeSpeed(int difference)` (override)

```java
@Override
public void changeSpeed(int difference)
{
    super.changeSpeed(difference);        // _speedUserExponent.value += difference
    writeClientSpeedMessageToChat(false);
    saveToOptionsFile(optionsPath);
}
```

속도 변경 후 채팅 메시지 출력 + 파일 저장.

---

### `writeClientConfigMessageToChat(boolean everyone)`

```java
private void writeClientConfigMessageToChat(boolean everyone)
{
    String prefix = getClientEveryonePrefix("move.config.chat.client", everyone);

    if(SmartMovingContext.Config.enabled)
    {
        String name = SmartMovingContext.Config._configKeyName.value;
        if(name.isEmpty())
            name = null;

        boolean unnamed = name == null;
        if(unnamed)
            name = getCurrentKey();

        if(name == SmartMovingProperties.Enabled || (unnamed && getKeyCount() == 1))
            writeToChat(prefix + "enabled", SmartMovingInfo.ConfigChatId);
        else
            writeToChat(prefix + (unnamed ? "unnamed" : "named"), SmartMovingInfo.ConfigChatId, new Object[] { name });
    }
    else
        writeToChat(prefix + "disabled", SmartMovingInfo.ConfigChatId);
}
```

**분기 로직**:
- SM 비활성: `prefix + "disabled"`
- SM 활성 + 이름 있음: `prefix + "named"` (이름 파라미터 포함)
- SM 활성 + 이름 없음 + (key==Enabled OR key 하나뿐): `prefix + "enabled"`
- SM 활성 + 이름 없음 + 그 외: `prefix + "unnamed"` (key 이름 파라미터 포함)

`SmartMovingProperties.Enabled` — "enabled" 문자열 상수 (SmartMovingProperties에서 확인 필요).

---

### `writeClientSpeedMessageToChat(boolean everyone)`

```java
public void writeClientSpeedMessageToChat(boolean everyone)
{
    if(!_speedChat.value)
        return;

    Object percent = SmartMovingContext.Config.getSpeedPercent();
    String prefix = getClientEveryonePrefix("move.speed.chat.client", everyone);
    String key = prefix + (percent.equals(SmartMovingConfig.defaultSpeedPercent) ? "reset" : "change");
    writeToChat(key, SmartMovingInfo.SpeedChatId, percent);
}
```

`SmartMovingConfig.defaultSpeedPercent = "100"` — 속도가 100%면 "reset" 키 사용, 아니면 "change" 키 + 퍼센트 값.

---

### `getClientEveryonePrefix(String base, boolean everyone)` (private static)

```java
private static String getClientEveryonePrefix(String base, boolean everyone)
{
    String result = base + ".";
    if(everyone)
        result += "everyone.";
    return result;
}
```

`everyone=false` → `"move.config.chat.client."`  
`everyone=true` → `"move.config.chat.client.everyone."`

---

### `writeServerConfigMessageToChat()`

```java
public void writeServerConfigMessageToChat()
{
    if(!_configChatServer.value)
        return;

    if(SmartMovingContext.Config.enabled)
    {
        String configName = SmartMovingContext.Config._configKeyName.value;
        if(configName != null && !configName.isEmpty())
            writeToChat("move.config.chat.server.global.named", SmartMovingInfo.DefaultChatId, configName);
        else
            writeToChat("move.config.chat.server.global.unnamed", SmartMovingInfo.DefaultChatId);
    }
    else
        writeToChat("move.config.chat.server.disable", SmartMovingInfo.DefaultChatId);
}
```

서버 전역 config 적용 시 채팅 메시지.

---

### `writeServerReconfigMessageToChat(boolean wasEnabled, String username, boolean everyone)`

```java
public void writeServerReconfigMessageToChat(boolean wasEnabled, String username, boolean everyone)
{
    if(Minecraft.getMinecraft().thePlayer.getGameProfile().getName().equals(username))
        writeClientConfigMessageToChat(everyone);   // 변경자가 자신이면 클라이언트 메시지
    else if(_configChatServer.value)
    {
        if(SmartMovingContext.Config.enabled)
        {
            String configname = SmartMovingContext.Config._configKeyName.value;
            boolean hasConfigName = configname != null && !configname.isEmpty();

            if(wasEnabled)
                if(hasConfigName)
                    if(username != null)
                        writeToChat("move.config.chat.server.update.named.user",   ..., configname, username);
                    else
                        writeToChat("move.config.chat.server.update.named",        ..., configname);
                else  // !hasConfigName
                    if(username != null)
                        writeToChat("move.config.chat.server.update.unnamed.user", ..., username);
                    else
                        writeToChat("move.config.chat.server.update.unnamed",      ...);
            else  // !wasEnabled (이전에 비활성이었음 → 새로 활성화됨)
                if(hasConfigName)
                    if(username != null)
                        writeToChat("move.config.chat.server.update.named.user",   ..., configname, username);
                    else
                        writeToChat("move.config.chat.server.update.named",        ..., configname);
                else
                    if(username != null)
                        writeToChat("move.config.chat.server.enable.user",         ..., username);
                    else
                        writeToChat("move.config.chat.server.enable",              ...);
        }
        else if(wasEnabled)   // 현재 비활성, 이전에 활성이었음 → 비활성화됨
            if(username != null)
                writeToChat("move.config.chat.server.disable.user", ..., username);
            else
                writeToChat("move.config.chat.server.disable",      ...);
    }
}
```

**분기 경우의 수 (8가지)**:

| 현재 enabled | wasEnabled | hasConfigName | username | 채팅 키 |
|---|---|---|---|---|
| true | true | true | 있음 | `.server.update.named.user` |
| true | true | true | null | `.server.update.named` |
| true | true | false | 있음 | `.server.update.unnamed.user` |
| true | true | false | null | `.server.update.unnamed` |
| true | false | true | 있음 | `.server.update.named.user` |
| true | false | true | null | `.server.update.named` |
| true | false | false | 있음 | `.server.enable.user` |
| true | false | false | null | `.server.enable` |
| false | true | - | 있음 | `.server.disable.user` |
| false | true | - | null | `.server.disable` |
| false | false | - | - | (아무것도 안 함) |

---

### `writeServerDeconfigMessageToChat()`

```java
public void writeServerDeconfigMessageToChat()
{
    if(_configChatServer.value)
        writeToChat("move.config.chat.server.local", SmartMovingInfo.DefaultChatId);
}
```

서버 강제 설정 해제 시 메시지.

---

### `writeServerSpeedMessageToChat(String username, boolean everyone)`

```java
public void writeServerSpeedMessageToChat(String username, boolean everyone)
{
    if(Minecraft.getMinecraft().thePlayer.getGameProfile().getName().equals(username))
        writeClientSpeedMessageToChat(everyone);   // 변경자가 자신이면 클라이언트 메시지
    else if(_speedChatServer.value)
    {
        Object percent = SmartMovingContext.Config.getSpeedPercent();
        String prefix = "move.speed.chat.server.";
        if(percent.equals(SmartMovingConfig.defaultSpeedPercent))
            writeToChat(prefix + "reset",  SmartMovingInfo.SpeedChatId, username);
        else
            writeToChat(prefix + "change", SmartMovingInfo.SpeedChatId, percent, username);
    }
}
```

---

### `writeNoRightsToChangeConfigMessageToChat(boolean isRemote)` (static)

```java
public static void writeNoRightsToChangeConfigMessageToChat(boolean isRemote)
{
    writeToChat("move.config.chat.server.illegal." + (isRemote ? "remote" : "local"), SmartMovingInfo.ConfigChatId);
}
```

---

### `writeNoRightsToChangeSpeedMessageToChat(boolean isRemote)` (static)

```java
public static void writeNoRightsToChangeSpeedMessageToChat(boolean isRemote)
{
    writeToChat("move.speed.chat.server.illegal." + (isRemote ? "remote" : "local"), SmartMovingInfo.SpeedChatId);
}
```

---

### `writeToChat(String key, int id, Object... parameters)` (private static)

```java
private static void writeToChat(String key, int id, Object... parameters)
{
    String message = parameters == null || parameters.length == 0
        ? I18n.format(key)
        : I18n.format(key, parameters);

    GuiNewChat guiChat = Minecraft.getMinecraft().ingameGUI.getChatGUI();

    // bugfix: also delete multi-lined chat messages
    if(id != 0)
        for(int i=0; i<5; i++)
            guiChat.deleteChatLine(id);

    guiChat.printChatMessageWithOptionalDeletion(new ChatComponentText(message), id);
}
```

- `I18n.format(key)` — 현지화 문자열 포맷
- `id != 0`이면 같은 id의 이전 메시지 최대 5줄 삭제 (멀티라인 채팅 메시지 중복 방지 버그 수정)
- `SmartMovingInfo.DefaultChatId` / `ConfigChatId` / `SpeedChatId` — 각 용도별 채팅 ID

---

### `initialize(...)` (static)

```java
public static void initialize(boolean redPowerWiring, boolean buildCraftTransportation, boolean finiteLiquid,
    boolean betterThanWolves, boolean singlePlayerCommands, boolean ropesPlus,
    boolean aSGrapplingHook, boolean betterMisc)
{
    hasRedPowerWire             = redPowerWiring;
    hasBuildCraftTransportation = buildCraftTransportation;
    hasFiniteLiquid             = finiteLiquid;
    hasBetterThanWolves         = betterThanWolves;
    hasSinglePlayerCommands     = singlePlayerCommands;
    hasRopesPlus                = ropesPlus;
    hasASGrapplingHook          = aSGrapplingHook;
    hasBetterMisc               = betterMisc;
}
```

`SmartMovingInstall.init()` 등에서 호출하여 8개 모드 존재 여부 설정.

---

### `resetForNewGame()`

```java
public void resetForNewGame()
{
    gameType = -1;   // Unknown
}
```

새 게임 시작 시 호출. `gameType`을 -1로 리셋하여 `initializeForGameIfNeccessary()`가 다음 호출에서 재초기화하도록 유도.

---

### `initializeForGameIfNeccessary()`

```java
public void initializeForGameIfNeccessary()
{
    PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
    if(controller == null)
        return;

    int currentGameType = ((GameType)Reflect.GetField(_currentGameType, controller)).getID();
    if(currentGameType == gameType)
        return;   // 게임 타입 변경 없으면 즉시 반환

    gameType = currentGameType;

    String[] keys = null;
    String defaultKey = null;

    switch(gameType)
    {
        case Survival:
            keys       = _survivalConfigKeys.value;
            defaultKey = _survivalDefaultConfigKey.value;
            break;
        case Creative:
            keys       = _creativeConfigKeys.value;
            defaultKey = _creativeDefaultConfigKey.value;
            break;
        case Adventure:
            keys       = _adventureConfigKeys.value;
            defaultKey = _adventureDefaultConfigKey.value;
            break;
        default:
            defaultKey = "";
    }

    setKeys(keys);
    if(!defaultKey.isEmpty())
        setCurrentKey(defaultKey);

    if(_configChatInit.value)
        writeClientConfigMessageToChat(false);

    if(isUserSpeedEnabled() && _speedChatInit.value)
    {
        Object speedPercent = getSpeedPercent();
        if(!speedPercent.equals(defaultSpeedPercent))
            writeToChat("move.speed.chat.client.init", SmartMovingInfo.DefaultChatId, speedPercent);
    }
}
```

**실행 흐름**:
1. `controller == null` → 반환 (아직 게임 진입 안 됨)
2. 리플렉션으로 `PlayerControllerMP.currentGameType` 필드 읽기 → `GameType.getID()`
3. `gameType` 변경 없으면 반환
4. 게임 타입별 config key 목록과 기본 key 설정
5. `setKeys(keys)` — 유효한 config key 목록 설정
6. `setCurrentKey(defaultKey)` — 현재 key 설정 (비어있지 않으면)
7. config 초기화 채팅 메시지 (설정에 따라)
8. 속도 초기화 채팅 메시지 (속도가 기본값과 다를 때만)

**gameType=Unknown(-1)** → `default` 케이스 → `defaultKey = ""` → `setCurrentKey` 호출 안 함.

리플렉션 경로: `Reflect.GetField(_currentGameType, controller)` → `(GameType)` 캐스팅 → `.getID()`.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingClientConfig` | 상속 |
| `SmartMovingContext.Config` | 전역 config 싱글톤 접근 |
| `SmartMovingContext.enabled` | SM 활성화 여부 |
| `SmartMovingInfo.ConfigChatId` / `SpeedChatId` / `DefaultChatId` | 채팅 메시지 ID |
| `SmartMovingInstall.PlayerControllerMP_currentGameType` | 리플렉션 필드 이름 |
| `SmartMovingProperties.Enabled` | "enabled" 상수 |
| `SmartMovingConfig.defaultSpeedPercent` | "100" 상수 |
| `Minecraft.getMinecraft()` | 싱글톤 접근 |
| `PlayerControllerMP` | 게임 타입 읽기 |
| `I18n.format()` | 현지화 |
| `GuiNewChat` | 채팅 출력 |
| `Reflect.GetField()` | 리플렉션 |

---

## 주요 관찰 사항

1. **최상위 클라이언트 설정 클래스**: `SmartMovingContext.Options`가 이 클래스의 인스턴스. 클라이언트 전용 — `Minecraft.getMinecraft()` 직접 접근 다수.

2. **`_speedChatServer` 키 중복**: `"move.config.chat.server"` 키를 `_configChatServer`와 공유. 동일 설정 항목을 config 채팅과 speed 채팅 모두에 적용하는 의도.

3. **`writeToChat` 버그픽스**: 멀티라인 채팅 메시지 삭제를 위해 `deleteChatLine(id)`를 5회 반복. 소스 주석 `"// bugfix: also delete multi-lined chat messages"`.

4. **게임 타입 감지**: `PlayerControllerMP.currentGameType`을 리플렉션으로 읽음. Forge API가 아닌 내부 필드 직접 접근 — `SmartMovingInstall`에 필드 이름 문자열 정의.

5. **`initializeForGameIfNeccessary()` 폴링 방식**: 매 틱(또는 그보다 자주) 호출되어 게임 타입 변경을 감지하는 구조. `resetForNewGame()`으로 -1을 설정하면 다음 호출에서 반드시 재초기화.

6. **`toggle()` 저장**: super.toggle() 후 currentKey를 defaultKey로 저장. 다음 게임 시작 시 해당 key로 시작하도록 영속화.

7. **`_old_toggleKeyCode.toKeyName()`**: int 키코드 → 키 이름 문자열 변환. v1.7 이전 설정 파일의 정수 키코드를 현재 문자열 형식으로 마이그레이션.

8. **1.21.1 이식 관련**:
   - `KeyBinding` → Fabric `KeyBinding` (net.fabricmc.fabric.api.client.keybinding.v1 또는 `net.minecraft.client.option.KeyBinding`)
   - `I18n.format()` → `net.minecraft.client.resource.language.I18n.translate()`
   - `GuiNewChat` → 1.21.1에 해당 API 없음. 채팅 메시지는 `MinecraftClient.getInstance().inGameHud.getChatHud()` 경로로 대체 필요
   - `PlayerControllerMP.currentGameType` 리플렉션 → `ClientPlayerInteractionManager.currentGameMode` (Fabric API에서 직접 접근 가능)
   - `Minecraft.getMinecraft()` → `MinecraftClient.getInstance()`
   - `optionsPath = mcDataDir` → `MinecraftClient.getInstance().runDirectory`
   - `ChatComponentText` → `Text.literal()`
