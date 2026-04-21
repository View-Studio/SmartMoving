# ClimbGap.java (net.smart.moving) 리서치

소스: https://github.com/makamys/SmartMoving/blob/master/src/main/java/net/smart/moving/ClimbGap.java  
패키지: `net.smart.moving`  
종류: `class`  
상속: 없음 (`Object` 직접 상속)

---

## 전체 소스

```java
package net.smart.moving;

import net.minecraft.block.*;

public class ClimbGap
{
	public Block Block;
	public int Meta;
	public boolean CanStand;
	public boolean MustCrawl;
	public Orientation Direction;
	public boolean SkipGaps;

	public ClimbGap()
	{
		reset();
	}

	public void reset()
	{
		Block = null;
		Meta = -1;
		CanStand = false;
		MustCrawl = false;
		Direction = null;
		SkipGaps = false;
	}
}
```

---

## 역할

클라이밍 중 **갭(틈새) 블록 정보**를 담는 데이터 클래스. 사다리/덩굴 등을 타고 올라갈 때 통과하거나 이동할 수 있는 공간의 속성을 기록한다.

---

## import

```java
import net.minecraft.block.*;  // Block
```

---

## 필드

```java
public Block Block;        // 해당 위치의 블록 (null이면 갭 없음/미설정)
public int Meta;           // 블록 메타데이터 (-1: 미설정)
public boolean CanStand;   // 해당 갭에서 서 있을 수 있는가
public boolean MustCrawl;  // 해당 갭을 통과하려면 크롤링 필요한가
public Orientation Direction; // 갭의 방향 (null: 미설정)
public boolean SkipGaps;   // 갭을 건너뛰는가
```

6개 필드 모두 `public`.

---

## 생성자

```java
public ClimbGap()
{
    reset();
}
```

생성자에서 `reset()`을 호출해 초기화.

---

## `reset()`

```java
public void reset()
{
    Block = null;
    Meta = -1;
    CanStand = false;
    MustCrawl = false;
    Direction = null;
    SkipGaps = false;
}
```

모든 필드를 초기 상태로 되돌린다.  
- `Block = null`: 블록 없음
- `Meta = -1`: 유효하지 않은 메타데이터 값 (0 이상이 유효)
- `CanStand = false`, `MustCrawl = false`, `SkipGaps = false`: 기본적으로 모두 비활성
- `Direction = null`: 방향 없음

---

## 의존 관계

| 의존 대상 | 용도 |
|-----------|------|
| `net.minecraft.block.Block` | 갭 위치의 블록 참조 |
| `net.smart.moving.Orientation` | 갭 방향 enum/클래스 |

`ClimbGap` 자체는 데이터 컨테이너. 실제 갭 감지와 필드 설정은 `SmartMovingSelf` 또는 `FeetClimbing`/`HandsClimbing` 등 다른 클래스에서 수행 (이 파일에서는 확인 불가).

---

## 주요 관찰 사항

1. **단순 데이터 클래스**: 로직 없음. 필드 + 생성자 + `reset()` 만 존재. 값 설정은 외부에서 직접 `climbGap.Block = ...` 형태로 수행.

2. **`Meta = -1`**: 블록 메타데이터 미설정 상태를 `-1`로 표현. 1.7.10 블록 시스템에서 메타값은 0~15이므로 -1은 "정보 없음" 센티널.

3. **`CanStand` vs `MustCrawl`**: 두 플래그가 독립적으로 존재. 이론상 둘 다 false(기어갈 수 없음)이거나, `CanStand=true`(서 있음), `MustCrawl=true`(크롤링 필수) 세 가지 상태가 가능.

4. **`SkipGaps`**: "갭을 건너뛰다"는 의미로, 특정 조건에서 갭 감지를 무시하는 플래그로 보임. 실제 사용처는 이 파일 외부에서 확인 필요.

5. **재사용 패턴**: `reset()` 메서드가 있어서 매 틱 동일한 인스턴스를 재초기화해서 재사용하는 패턴임을 알 수 있음. 새 객체 생성 없이 상태 초기화.

6. **1.21.1 이식**: `Block` 타입은 그대로 사용 가능 (1.21.1에도 `net.minecraft.block.Block` 존재). `Meta` 필드는 1.21.1에서 메타데이터 시스템이 `BlockState`로 대체됐으므로 `BlockState Direction` 형태나 별도 처리가 필요할 수 있음.
