# SmartMovingTestMod.java (net.smart.moving.test) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/test/SmartMovingTestMod.java  
패키지: `net.smart.moving.test`  
종류: `class` (FML Mod)  
어노테이션: `@Mod(modid = "SmartMovingTestMod", version = "0.0")`  
실행 위치: 클라이언트

---

## 전체 소스

소스는 GitHub 링크 참조.

---

## 역할

SmartMoving의 이동 동작을 자동으로 수행하고 매 틱의 위치 변화(delta)를 파일에 기록하는 자동화 테스트 모드. `/smartmovingtest run` 명령어로 시작하며, 사전 정의된 키 입력 시퀀스를 자동으로 실행하고 결과를 `smart-moving-test-output.txt`에 저장한다.

---

## import

```java
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
```

---

## 필드

```java
public static final Logger LOGGER = LogManager.getLogger("smartmovingtest");

private static final int TEST_TICKS_PER_SECOND =
    Integer.parseInt(System.getProperty("smartMovingTest.ticksPerSecond", "20"));

private double lastX;
private double lastY;
private double lastZ;
private TestEvent lastEventRun;

private boolean running;
private int time;

public static SmartMovingTestMod instance;
private static Minecraft mc;

FileWriter out;
```

**`TEST_TICKS_PER_SECOND`**: JVM 시스템 프로퍼티 `smartMovingTest.ticksPerSecond`로 설정, 기본값 `20`. 각 이벤트 duration(초) × 이 값 = 틱 수.

**`out`**: `package-private` FileWriter. 출력 파일 핸들.

---

## 내부 클래스

### `TestEvent` (private static)

```java
private static class TestEvent {
    String name;
    int duration;
    Runnable[] actions;

    private TestEvent(String name, int duration, Runnable...actions) {
        this.name = name;
        this.duration = duration;
        this.actions = actions;
    }
}
```

- `name`: 이벤트 식별 문자열 (로그/파일 출력에 사용)
- `duration`: 이 이벤트가 지속되는 **초** 단위 시간 (틱 수 = `duration * TEST_TICKS_PER_SECOND`)
- `actions`: 이벤트 시작 시 실행할 Runnable 배열

---

### `SetKeyState` (private static, implements Runnable)

```java
private static class SetKeyState implements Runnable {
    private KeyBinding keyBind;
    private boolean state;

    public SetKeyState(Function<GameSettings, KeyBinding> keyFunction, boolean state) {
        keyBind = keyFunction.apply(Minecraft.getMinecraft().gameSettings);
        this.state = state;
    }

    @Override
    public void run() {
        // Not using KeyBinding.setKeyBindState because it doesn't work with conflicting keybinds
        ReflectionHelper.setPrivateValue(KeyBinding.class, keyBind, state, "pressed", "field_74513_e");
    }
}
```

**생성 시점**: `testEvents` 리스트 초기화 시 (static 필드). `Minecraft.getMinecraft().gameSettings`에서 키 바인딩 취득.

**`run()` 구현**: `KeyBinding.setKeyBindState()` 대신 리플렉션으로 `pressed` 필드 직접 설정.  
이유 (주석): `setKeyBindState`는 키 충돌이 있을 때 동작하지 않음.  
필드 이름 목록: `"pressed"`, `"field_74513_e"` (난독화 이름 대비 두 가지 제공).

---

## `testEvents` 정적 리스트

```java
private static final List<TestEvent> testEvents = Arrays.asList(...)
```

총 25개 이벤트. 실행 순서:

| # | 이벤트명 | duration(초) | 액션 |
|---|---------|------------|------|
| 1 | WAIT_BEFORE_WALK | 3 | `/effect @p clear` |
| 2 | WALK_PRE | 4 | forward=true |
| 3 | WALK_JUMP | 8 | jump=true |
| 4 | WALK_END | 4 | jump=false |
| 5 | SPRINT_PREPARE | 2 | sprint=true |
| 6 | SPRINT_PRE | 4 | sprint=false |
| 7 | SPRINT_JUMP | 8 | jump=true |
| 8 | SPRINT_END | 4 | jump=false |
| 9 | WAIT_BEFORE_SWIFT | 3 | forward=false |
| 10 | SWIFT_PREPARE | 2 | `/effect @p 1 600 0` (Speed I) |
| 11 | SWIFT_WALK_PRE | 4 | forward=true |
| 12 | SWIFT_WALK_JUMP | 8 | jump=true |
| 13 | SWIFT_WALK_END | 4 | jump=false |
| 14 | SWIFT_SPRINT_PREPARE | 2 | sprint=true |
| 15 | SWIFT_SPRINT_PRE | 4 | sprint=false |
| 16 | SWIFT_SPRINT_JUMP | 8 | jump=true |
| 17 | SWIFT_SPRINT_END | 4 | jump=false |
| 18 | WAIT_BEFORE_HIJUMP | 3 | forward=false |
| 19 | HIJUMP_PREPARE_1 | 1 | `/effect @p clear` |
| 20 | HIJUMP_PREPARE_2 | 1 | `/effect @p 8 600 0` (Jump Boost I) |
| 21 | HIJUMP_WALK_PRE | 4 | forward=true |
| 22 | HIJUMP_JUMP | 8 | jump=true |
| 23 | HIJUMP_WALK_END | 4 | jump=false |
| 24 | WAIT_BEFORE_FINISH | 3 | forward=false |
| 25 | FINISH | 1 | `/effect @p clear` |

**총 시간**: 3+4+8+4+2+4+8+4+3+2+4+8+4+2+4+8+4+3+1+1+4+8+4+3+1 = **97초**

**테스트 시나리오 구성**:
- 일반 걷기 점프 (WALK)
- 스프린트 점프 (SPRINT)
- Speed I 효과 + 걷기/스프린트 점프 (SWIFT)
- Jump Boost I 효과 + 걷기 점프 (HIJUMP)

**키 액션 상세**:
- `gs -> gs.keyBindForward`: 전진 키
- `gs -> gs.keyBindJump`: 점프 키
- `gs -> gs.keyBindSprint`: 스프린트 키

---

## FML 이벤트 핸들러

### `preinit(FMLPreInitializationEvent)`

```java
@EventHandler
public void preinit(FMLPreInitializationEvent event) {
    instance = this;
    mc = Minecraft.getMinecraft();
    FMLCommonHandler.instance().bus().register(this);
}
```

`instance` 설정, `mc` 설정, FML 이벤트 버스에 this 등록 (onTick 수신용).

### `init(FMLInitializationEvent)`

```java
@EventHandler
public void init(FMLInitializationEvent event) {
    ClientCommandHandler.instance.registerCommand(new SmartMovingTestCommand());
}
```

`/smartmovingtest` 명령어 등록.

---

## `onTick(PlayerTickEvent)` (@SubscribeEvent)

```java
@SubscribeEvent
public void onTick(PlayerTickEvent event) {
    if(event.side == Side.CLIENT && event.phase == Phase.START && running) {
```

**실행 조건**: 클라이언트 사이드 + Phase.START + `running == true`

### P키 중단

```java
if(Keyboard.isKeyDown(Keyboard.KEY_P)) {
    System.out.println("P key pressed, aborting test!");
    running = false;
}
```

### 이벤트 디스패치

```java
int timeCounter = 0;
TestEvent eventRun = null;
for(TestEvent e : testEvents) {
    if(time == timeCounter) {
        System.out.println("Running event " + e.name + " for " + e.duration + " seconds.");
        eventRun = e;
        for(Runnable a : e.actions) {
            a.run();
        }
        break;
    }
    timeCounter += e.duration * TEST_TICKS_PER_SECOND;
}
if(time > timeCounter) {
    System.out.println("All test events executed!");
    mc.getSoundHandler().playSound(
        PositionedSoundRecord.func_147674_a(new ResourceLocation("random.levelup"), 1.0F));
    running = false;
}
```

- `timeCounter`는 각 이벤트 시작 틱을 누적
- `time == timeCounter`이면 해당 이벤트의 `actions` 전부 실행
- 모든 이벤트 통과 후(`time > timeCounter`): `random.levelup` 사운드 재생, `running = false`

### 위치 델타 기록

```java
PlayerTickEvent pte = (PlayerTickEvent)event;
double x = pte.player.posX;
double y = pte.player.posY;
double z = pte.player.posZ;

String line = String.format(Locale.ROOT, "%12.4f%12.4f%12.4f", x-lastX, y-lastY, z-lastZ);
ArrayList<String> comments = new ArrayList<>();
if(time == 0) {
    if(TEST_TICKS_PER_SECOND != 20) {
        comments.add("note: ticks per second: " + TEST_TICKS_PER_SECOND);
    }
}
if(lastEventRun != null) {
    comments.add("start " + lastEventRun.name);
}
if(!comments.isEmpty()) {
    line += " # " + String.join("; ", comments);
}
line += "\n";

if(time != 0) {
    try {
        out.write(line);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

**출력 포맷**: `%12.4f%12.4f%12.4f` — 각 열 12자 너비, 소수 4자리. `Locale.ROOT`로 로케일 무관 소수점.  
**예**: `      0.0000      0.0000      0.0000`

**주석(#) 추가 조건**:
- `time == 0` && `TEST_TICKS_PER_SECOND != 20`: `"note: ticks per second: N"`
- `lastEventRun != null`: `"start <이벤트명>"` (이 틱에 새 이벤트가 시작된 경우)

**`time == 0` 틱**: 파일에 쓰지 않음 (`if(time != 0)`). 초기 위치 기준점 설정용.

### 틱 카운터 진행 및 상태 갱신

```java
time++;

lastX = x;
lastY = y;
lastZ = z;
lastEventRun = eventRun;

if(!running) {
    try {
        out.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

`time++` 후 `lastX/Y/Z` 갱신, `lastEventRun = eventRun` (이번 틱 실행된 이벤트 기록).  
`running`이 false가 된 틱에 즉시 `out.close()`.

---

## `startTest()`

```java
public void startTest() {
    System.out.println("Tests will take " + testEvents.stream().mapToInt(e -> e.duration).sum() + " seconds in total.");

    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    player.rotationPitch = player.rotationYaw = 0;
    time = 0;
    try {
        out = new FileWriter("smart-moving-test-output.txt");
    } catch (IOException e) {
        e.printStackTrace();
    }
    running = true;
}
```

**총 시간 출력**: `testEvents.stream().mapToInt(e -> e.duration).sum()` = 97초 (고정).

**플레이어 회전 초기화**: `rotationPitch = rotationYaw = 0` — 정면 수평 시점으로 고정.

**출력 파일**: `"smart-moving-test-output.txt"` — 게임 실행 디렉토리 기준 상대 경로.

**`running = true`**: onTick 루프 시작.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingTestCommand` | init에서 명령어 등록 |
| `ReflectionHelper` | SetKeyState — `KeyBinding.pressed` 필드 직접 설정 |
| `FMLCommonHandler.instance().bus()` | onTick 이벤트 수신 등록 |
| `ClientCommandHandler` | 명령어 등록 |
| `Keyboard` | P키 중단 감지 |
| `PositionedSoundRecord.func_147674_a` | 완료 사운드 재생 (난독화 메서드명) |

---

## 주요 관찰 사항

1. **`time == 0` 데이터 스킵**: 첫 틱은 `lastX/Y/Z`가 0으로 초기화된 상태이므로 의미 없는 델타가 기록될 수 있어 파일 출력 제외.

2. **`SetKeyState` 리플렉션 이유**: `KeyBinding.setKeyBindState()`는 충돌하는 키 바인딩이 있을 때 동작하지 않는 버그가 있어, 직접 `pressed` 필드를 설정. 두 이름(`"pressed"`, `"field_74513_e"`) 제공으로 난독화 여부 무관하게 동작.

3. **`lastEventRun` 기록 방식**: 이벤트 실행된 틱에서 `eventRun`을 `lastEventRun`에 저장. 다음 틱에 `"start <이벤트명>"` 주석이 출력됨 → 이벤트 시작 직후 첫 델타 행에 주석 부착.

4. **출력 파일 구조**:  
   ```
         0.0000      0.0000      0.0000 # start WALK_PRE
         0.2000      0.0000      0.0000
   ...
   ```
   각 행이 한 틱의 위치 델타(dX, dY, dZ).

5. **`func_147674_a`**: 난독화된 `PositionedSoundRecord` 정적 메서드명. 1.21.1에서는 `SoundInstance` API로 완전 교체.

6. **1.21.1 이식 관련**:
   - FML → Fabric API (`@Mod` → `ModInitializer`, FML 이벤트 → Fabric 이벤트)
   - `PlayerTickEvent` → Fabric `ClientTickEvents.END_PLAYER_TICK`
   - `Keyboard.isKeyDown` → `InputUtil.isKeyPressed`
   - `ReflectionHelper.setPrivateValue` → 리플렉션 직접 사용 또는 Mixin accessor
   - `ClientCommandHandler` → Fabric `ClientCommandRegistrationCallback`
   - `CommandBase` → Brigadier (`LiteralArgumentBuilder`)
   - `PositionedSoundRecord.func_147674_a` → `PositionedSoundInstance.master()`
   - `ChatComponentText` → `Text.literal()`
