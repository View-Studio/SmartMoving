# SmartMovingInstall.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/SmartMovingInstall.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import net.smart.utilities.*;

public class SmartMovingInstall
{
	public final static Name RopesPlusCore = new Name("atomicstryker.ropesplus.common.RopesPlusCore");
	public final static Name ModBlockFence = new Name("net.minecraft.src.modBlockFence", "modBlockFence");
	public final static Name MacroModCore = new Name("net.eq2online.macros.core.MacroModCore");
	public final static Name BlockSturdyLadder = new Name("mods.chupmacabre.ladderKit.sturdyLadders.BlockSturdyLadder");
	public final static Name BlockRopeLadder = new Name("mods.chupmacabre.ladderKit.ropeLadders.BlockRopeLadder");

	public final static Name RopesPlusClient = new Name("atomicstryker.ropesplus.client.RopesPlusClient");
	public final static Name RopesPlusClient_onZipLine = new Name("onZipLine");

	public final static Name CarpentersBlockLadder = new Name("com.carpentersblocks.block.BlockCarpentersLadder");
	public final static Name CarpentersTEBaseBlock = new Name("com.carpentersblocks.tileentity.TEBase");
	public final static Name CarpentersTEBaseBlock_getData = new Name("getData");

	public final static Name NetServerHandler_ticksForFloatKick = new Name("floatingTickCount", "field_147365_f", "f");
	public final static Name GuiNewChat_chatMessageList = new Name("chatLines", "field_146252_h", "h");
	public final static Name PlayerControllerMP_currentGameType = new Name("currentGameType", "field_78779_k", "k");
	public final static Name ModifiableAttributeInstance_attributeValue = new Name("cachedValue", "field_111139_h", "h");
}
```

---

## 역할

SmartMoving이 **리플렉션으로 접근하는 외부 클래스/메서드/필드 이름**을 한 곳에 모은 상수 클래스.

두 종류의 이름을 보관:
1. **호환 모드 클래스/메서드 이름**: RopesPlus, Carpenter's Blocks, LadderKit, Macro Mod 등 서드파티 모드 클래스
2. **vanilla/MC 내부 필드 이름**: 난독화된 이름이 있는 MC 내부 필드 (deobf/MCP Searge/obf 3가지 형태)

메서드 없음. 생성자 없음. 모두 `public static final Name`.

---

## import

```java
import net.smart.utilities.*;  // Name
```

---

## 호환 모드 클래스 이름

### 1-arg `Name(String)` — deobf 이름만 존재하는 클래스

```java
public final static Name RopesPlusCore = new Name("atomicstryker.ropesplus.common.RopesPlusCore");
public final static Name MacroModCore = new Name("net.eq2online.macros.core.MacroModCore");
public final static Name BlockSturdyLadder = new Name("mods.chupmacabre.ladderKit.sturdyLadders.BlockSturdyLadder");
public final static Name BlockRopeLadder = new Name("mods.chupmacabre.ladderKit.ropeLadders.BlockRopeLadder");
public final static Name RopesPlusClient = new Name("atomicstryker.ropesplus.client.RopesPlusClient");
public final static Name CarpentersBlockLadder = new Name("com.carpentersblocks.block.BlockCarpentersLadder");
public final static Name CarpentersTEBaseBlock = new Name("com.carpentersblocks.tileentity.TEBase");
```

| 상수 | 클래스 FQN | 모드 |
|------|-----------|------|
| `RopesPlusCore` | `atomicstryker.ropesplus.common.RopesPlusCore` | RopesPlus (AtomicStryker) |
| `MacroModCore` | `net.eq2online.macros.core.MacroModCore` | Macro/Keybind Mod |
| `BlockSturdyLadder` | `mods.chupmacabre.ladderKit.sturdyLadders.BlockSturdyLadder` | LadderKit |
| `BlockRopeLadder` | `mods.chupmacabre.ladderKit.ropeLadders.BlockRopeLadder` | LadderKit |
| `RopesPlusClient` | `atomicstryker.ropesplus.client.RopesPlusClient` | RopesPlus |
| `CarpentersBlockLadder` | `com.carpentersblocks.block.BlockCarpentersLadder` | Carpenter's Blocks |
| `CarpentersTEBaseBlock` | `com.carpentersblocks.tileentity.TEBase` | Carpenter's Blocks |

### 2-arg `Name(String deobf, String obf)` — obf 이름이 있는 클래스

```java
public final static Name ModBlockFence = new Name("net.minecraft.src.modBlockFence", "modBlockFence");
```

| 상수 | deobf | obf | 비고 |
|------|-------|-----|------|
| `ModBlockFence` | `net.minecraft.src.modBlockFence` | `modBlockFence` | modloader 계열 울타리 블록 |

---

## 호환 모드 메서드 이름

```java
public final static Name RopesPlusClient_onZipLine = new Name("onZipLine");
public final static Name CarpentersTEBaseBlock_getData = new Name("getData");
```

| 상수 | 메서드 이름 | 소속 클래스 |
|------|------------|------------|
| `RopesPlusClient_onZipLine` | `"onZipLine"` | `RopesPlusClient` |
| `CarpentersTEBaseBlock_getData` | `"getData"` | `CarpentersTEBaseBlock` (TEBase) |

1-arg `Name` — deobf 이름만. 서드파티 모드 메서드는 난독화 없음.

---

## vanilla/MC 내부 필드 이름

3-arg `Name(String, String, String)` — readable / MCP Searge / obf 3가지 형태:

```java
public final static Name NetServerHandler_ticksForFloatKick = new Name("floatingTickCount", "field_147365_f", "f");
public final static Name GuiNewChat_chatMessageList = new Name("chatLines", "field_146252_h", "h");
public final static Name PlayerControllerMP_currentGameType = new Name("currentGameType", "field_78779_k", "k");
public final static Name ModifiableAttributeInstance_attributeValue = new Name("cachedValue", "field_111139_h", "h");
```

| 상수 | readable 이름 | MCP Searge 이름 | obf 이름 | 소속 클래스 |
|------|--------------|----------------|---------|------------|
| `NetServerHandler_ticksForFloatKick` | `"floatingTickCount"` | `"field_147365_f"` | `"f"` | `NetHandlerPlayServer` |
| `GuiNewChat_chatMessageList` | `"chatLines"` | `"field_146252_h"` | `"h"` | `GuiNewChat` |
| `PlayerControllerMP_currentGameType` | `"currentGameType"` | `"field_78779_k"` | `"k"` | `PlayerControllerMP` |
| `ModifiableAttributeInstance_attributeValue` | `"cachedValue"` | `"field_111139_h"` | `"h"` | `ModifiableAttributeInstance` |

**각 필드의 의미:**

| 상수 | 의미 |
|------|------|
| `NetServerHandler_ticksForFloatKick` | 서버가 플레이어를 kick하기 전까지 카운트하는 부유 틱 — `SmartMovingServer.resetTicksForFloatKick()`이 이것을 리셋 |
| `GuiNewChat_chatMessageList` | 채팅창의 메시지 목록 — `SmartMovingOptions`에서 채팅 메시지 ID로 교체 시 사용 |
| `PlayerControllerMP_currentGameType` | 클라이언트 게임 타입 — Creative/Survival 판별 등에 사용 |
| `ModifiableAttributeInstance_attributeValue` | 속성 인스턴스의 캐시된 값 — 이동 속도 등 속성 조작 시 사용 |

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.smart.utilities.Name` | 이름 래퍼 클래스 (deobf/obf 선택) |

이 파일은 `Name` 상수를 보관하기만 하고, 실제 리플렉션 접근은 `SmartMovingOther`, `Compat` 등 다른 클래스에서 이 상수를 참조해서 수행함.

---

## 주요 관찰 사항

1. **`Name` 3-arg 생성자**: SmartRender의 `Name(deobf, obf)` 2-arg와 달리, 이 파일에서 vanilla 내부 필드는 `Name(readable, srgName, obf)` 3-arg 형태 사용. `net.smart.utilities.Name`(SmartMoving 버전)이 3-arg 생성자를 추가로 가짐 — SmartMoving `Name.java` 리서치 시 확인 필요.

2. **호환 모드 목록**: 이 파일에서 확인된 SmartMoving이 호환성을 지원하는 서드파티 모드:
   - **RopesPlus** (AtomicStryker): 코어 + 클라이언트, `onZipLine` 메서드
   - **LadderKit** (chupmacabre): SturdyLadder, RopeLadder 두 블록
   - **Macro Mod** (eq2online): 코어 클래스
   - **Carpenter's Blocks**: 사다리 블록 + TEBase(`getData`)

3. **`NetServerHandler_ticksForFloatKick`**: SmartMovingServer의 `resetTicksForFloatKick()` 동작과 연결 — 클라이밍 중 서버가 부유 판정으로 kick하지 않도록 이 필드를 리셋.

4. **`ModBlockFence` obf이름**: `"modBlockFence"` — modloader 시대 패치 클래스 (1.7.10에서도 modloader 호환성 유지).

5. **1.21.1 이식**: 이 파일 전체가 1.7.10 전용 클래스/필드 참조. 1.21.1에서는 대부분 해당 모드가 없거나 완전히 다른 API를 가짐. vanilla 필드 4개는 Yarn 매핑 기준으로 재확인 후 Mixin accessor로 접근하는 방식으로 교체 필요.
