# Button.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/Button.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: `SmartMovingContext`

---

## 전체 소스

```java
package net.smart.moving;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.*;

public class Button extends SmartMovingContext
{
	public boolean Pressed;
	public boolean WasPressed;

	public boolean StartPressed;
	public boolean StopPressed;

	public void update(KeyBinding binding)
	{
		update(Minecraft.getMinecraft().inGameHasFocus && isKeyDown(binding));
	}

	public void update(int keyCode)
	{
		update(Minecraft.getMinecraft().inGameHasFocus && isKeyDown(keyCode));
	}

	public void update(boolean pressed)
	{
		WasPressed = Pressed;
		Pressed = pressed;

		StartPressed = !WasPressed && Pressed;
		StopPressed = WasPressed && !Pressed;
	}

	private static boolean isKeyDown(KeyBinding keyBinding)
	{
		return isKeyDown(keyBinding, keyBinding.isPressed());
	}

	private static boolean isKeyDown(KeyBinding keyBinding, boolean wasDown)
	{
		GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
		if(currentScreen == null || currentScreen.allowUserInput)
			return isKeyDown(keyBinding.getKeyCode());
		return wasDown;
	}

	private static boolean isKeyDown(int keyCode)
	{
		if(keyCode >= 0)
			return Keyboard.isKeyDown(keyCode);
		return Mouse.isButtonDown(keyCode + 100);
	}
}
```

---

## 역할

SmartMoving에서 사용하는 **키/마우스 버튼 상태 래퍼**. 매 틱 `update()`를 호출해서 현재 프레임의 눌림 상태와 에지(시작/종료) 이벤트를 계산한다.

---

## import

```java
import org.lwjgl.input.Keyboard;   // LWJGL 키보드
import org.lwjgl.input.Mouse;      // LWJGL 마우스
import net.minecraft.client.*;      // Minecraft
import net.minecraft.client.gui.*;  // GuiScreen
import net.minecraft.client.settings.*;  // KeyBinding
```

---

## 필드

```java
public boolean Pressed;      // 현재 틱 눌려 있는가
public boolean WasPressed;   // 직전 틱 눌려 있었는가

public boolean StartPressed; // 이번 틱에 새로 눌림 (false→true 에지)
public boolean StopPressed;  // 이번 틱에 새로 떼어짐 (true→false 에지)
```

4개 모두 `public`. 외부에서 직접 읽음.

---

## 메서드

### `update(KeyBinding binding)`

```java
public void update(KeyBinding binding)
{
    update(Minecraft.getMinecraft().inGameHasFocus && isKeyDown(binding));
}
```

`Minecraft.inGameHasFocus`가 `false`이면 항상 `false` → 게임 화면에 포커스가 없을 때 키 무시.  
`isKeyDown(binding)` → 아래 private 오버로드로 위임.

---

### `update(int keyCode)`

```java
public void update(int keyCode)
{
    update(Minecraft.getMinecraft().inGameHasFocus && isKeyDown(keyCode));
}
```

keyCode 직접 지정 버전. 역시 `inGameHasFocus` 조건.

---

### `update(boolean pressed)`

```java
public void update(boolean pressed)
{
    WasPressed = Pressed;
    Pressed = pressed;

    StartPressed = !WasPressed && Pressed;
    StopPressed = WasPressed && !Pressed;
}
```

핵심 로직. 이전 상태를 `WasPressed`에 저장하고 새 상태로 `Pressed` 갱신.  
- `StartPressed = !WasPressed && Pressed` → 이전에 안 눌렸고 지금 눌린 경우 (rising edge)  
- `StopPressed = WasPressed && !Pressed` → 이전에 눌렸고 지금 안 눌린 경우 (falling edge)

---

### `isKeyDown(KeyBinding keyBinding)` (private static)

```java
private static boolean isKeyDown(KeyBinding keyBinding)
{
    return isKeyDown(keyBinding, keyBinding.isPressed());
}
```

`keyBinding.isPressed()`를 `wasDown` 기본값으로 넘김.

---

### `isKeyDown(KeyBinding keyBinding, boolean wasDown)` (private static)

```java
private static boolean isKeyDown(KeyBinding keyBinding, boolean wasDown)
{
    GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
    if(currentScreen == null || currentScreen.allowUserInput)
        return isKeyDown(keyBinding.getKeyCode());
    return wasDown;
}
```

`currentScreen == null` → 인게임 HUD (GUI 없음) → 키 코드로 직접 확인.  
`currentScreen.allowUserInput == true` → 인게임 입력을 허용하는 GUI (채팅창 등 일부) → 키 코드로 직접 확인.  
그 외(일반 GUI 열려 있음) → `wasDown`(이전 상태) 그대로 반환 → GUI 열린 동안 상태 유지, 변화 없음.

---

### `isKeyDown(int keyCode)` (private static)

```java
private static boolean isKeyDown(int keyCode)
{
    if(keyCode >= 0)
        return Keyboard.isKeyDown(keyCode);
    return Mouse.isButtonDown(keyCode + 100);
}
```

keyCode 분기:
- `keyCode >= 0` → LWJGL 키보드 (`Keyboard.isKeyDown`)
- `keyCode < 0` → LWJGL 마우스 (`Mouse.isButtonDown(keyCode + 100)`)

Minecraft 1.7.10의 마우스 버튼 keyCode 인코딩: 마우스 버튼 인덱스를 `-100 - buttonIndex`로 인코딩.  
예: 마우스 버튼 0(좌클릭) → keyCode=-100 → `Mouse.isButtonDown(0)`.

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `SmartMovingContext` | 상속 (Context의 싱글톤 등 상속, 실제로는 필드/메서드 직접 사용 없음) |
| `org.lwjgl.input.Keyboard` | 키보드 눌림 상태 |
| `org.lwjgl.input.Mouse` | 마우스 버튼 눌림 상태 |
| `net.minecraft.client.Minecraft` | `inGameHasFocus`, `currentScreen` |
| `net.minecraft.client.gui.GuiScreen` | `allowUserInput` |
| `net.minecraft.client.settings.KeyBinding` | `isPressed()`, `getKeyCode()` |

---

## 주요 관찰 사항

1. **에지 감지 패턴**: `StartPressed`/`StopPressed`로 rising/falling edge를 매 틱 계산. SmartMoving에서 특정 동작(점프 시작, 클라이밍 시작 등)을 "버튼을 새로 눌렀을 때" 트리거하는 데 사용.

2. **`SmartMovingContext` 상속**: `Button`은 컨텍스트 필드/메서드를 직접 사용하지 않지만, SmartMoving 모든 클래스 계층의 일관성을 위해 상속. 실질적으로는 독립 유틸리티 클래스.

3. **GUI 열림 처리**: `currentScreen != null && !allowUserInput`이면 현재 눌림 상태 변화 없이 이전 상태(`wasDown`) 유지. GUI가 열린 동안 키 입력이 상태를 변경하지 않도록 보호.

4. **마우스 버튼 keyCode 인코딩**: Minecraft 1.7.10 `KeyBinding`에서 마우스 버튼은 `-100 - index`로 인코딩. `Button.isKeyDown(int)`에서 `+100`을 더해서 LWJGL `Mouse.isButtonDown(index)`로 변환.

5. **1.21.1 이식**:
   - LWJGL 3로 변경: `org.lwjgl.glfw.GLFW.glfwGetKey()`, `glfwGetMouseButton()` 또는 Fabric `InputUtil`/`KeyBinding` API 사용
   - `Minecraft.getMinecraft()` → `MinecraftClient.getInstance()`
   - `currentScreen.allowUserInput` → 1.21.1 Screen에서 해당 필드 없음, 다른 방식으로 확인 필요
   - `KeyBinding.getKeyCode()` → `KeyBinding.boundKey` (InputUtil.Key 타입)
   - `inGameHasFocus` → `MinecraftClient.getInstance().mouse.isCursorLocked()`
