# 클라이밍/천장클라이밍 교차 분석

소스: SmartMoving 1.7.10 원본 + vanilla 1.21.1 Yarn 매핑

---

## HandsClimbing (Typesafe Enum)

### HandsClimbing enum 값
- 원본 동작: None(-3), Sink(-2), TopHold(-1), BottomHold(0), Up(1), FastUp(2) — Typesafe Enum 패턴 (Java 1.4 시대)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.7.10은 `public static final HandsClimbing None = new HandsClimbing(-3)` 패턴. 1.21.1은 Java enum 사용 가능
- 포팅 주의사항: Java enum으로 변환 시 `_value` int 비교 로직(IsRelevant: _value>None, IsUp: _value>BottomHold 등)을 ordinal 또는 compareTo로 교체해야 함. max() 메서드는 별도로 구현 필요

### HandsClimbing 상수
- 원본 동작: `NoGrab=0`, `UpGrab=1`, `MiddleGrab=2` — 애니메이션 분기용 int 상수, HandsClimbing enum과 별개
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음 (단순 int 상수)
- 포팅 주의사항: actualHandsClimbType 필드가 이 상수들을 int로 저장함. 네트워크 전송 시 4비트로 인코딩됨

### HandsClimbing.IsRelevant()
- 원본 동작: `_value > None._value` (None(-3) 제외 전부 relevant)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: Java enum이면 `this != None`으로 교체

### HandsClimbing.IsUp()
- 원본 동작: `_value > BottomHold._value` (Up(1), FastUp(2)만 true)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: Java enum이면 `this.ordinal() > BottomHold.ordinal()` 또는 별도 필드로 관리

### HandsClimbing.ToUp()
- 원본 동작: BottomHold → Up 전환. BottomHold 이외의 값은 그대로 반환
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 상태 전환 로직이므로 그대로 이식 가능

### HandsClimbing.ToDown()
- 원본 동작: TopHold → Sink 전환. TopHold 이외의 값은 그대로 반환
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 상태 전환 로직이므로 그대로 이식 가능

### HandsClimbing.max(other, inout_thisGap, otherGap)
- 원본 동작: _value 비교하여 더 큰 쪽 선택. CanStand/MustCrawl은 두 쪽을 OR 합산. 더 강한 쪽의 Block/Meta/Direction 값을 inout_thisGap에 복사
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: Meta 필드가 BlockState로 교체되면서 복사 방식 변경 필요
- 포팅 주의사항: ClimbGap.Meta → BlockState 전환과 연동해야 함

---

## FeetClimbing (Typesafe Enum)

### FeetClimbing enum 값
- 원본 동작: None(-3), BaseHold(-2), BaseWithHands(-1), TopWithHands(0), SlowUpWithHoldWithoutHands(1), SlowUpWithSinkWithoutHands(2), FastUp(3) — Typesafe Enum 패턴
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.7.10 Typesafe Enum → Java enum 변환 필요
- 포팅 주의사항: HandsClimbing과 동일하게 _value 비교 로직 전환 필요

### FeetClimbing 상수
- 원본 동작: `NoStep=0`, `DownStep=1` — 애니메이션 분기용 int 상수
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음 (단순 int 상수)
- 포팅 주의사항: actualFeetClimbType 필드가 이 상수들을 int로 저장. 네트워크 전송 시 4비트로 인코딩됨

### FeetClimbing.IsRelevant()
- 원본 동작: `_value > None._value` (None(-3) 제외 전부)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: Java enum이면 `this != None`

### FeetClimbing.IsIndependentlyRelevant()
- 원본 동작: `_value > BaseWithHands._value` — BaseWithHands(-1)보다 큰 것만. TopWithHands(0) 이상. 손 없이 발만으로도 클라이밍 가능한 상태를 나타냄
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: handleClimbing의 Simple 모드에서 이 값으로 motionY 결정. 오역 시 클라이밍 불가 버그 발생

### FeetClimbing.IsUp()
- 원본 동작: `_value > BaseHold._value` (BaseHold(-2) 초과, 즉 BaseWithHands(-1) 이상)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: IsIndependentlyRelevant와 다름. 혼동 주의

### FeetClimbing.max(other, inout_thisGap, otherGap)
- 원본 동작: HandsClimbing.max와 동일 패턴
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: Meta 필드 변환 필요
- 포팅 주의사항: HandsClimbing.max와 동일

---

## ClimbGap (데이터 클래스)

### ClimbGap 필드
- 원본 동작: `int Block`, `int Meta` (Meta=-1은 미설정), `boolean CanStand`, `boolean MustCrawl`, `Orientation Direction`, `boolean SkipGaps`
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: Block(int ID) + Meta(int) → BlockState(단일 객체)로 통합됨. 1.21.1에는 블록 int ID 없음
- 포팅 주의사항: Block+Meta 조합을 BlockState 하나로 교체해야 함. Meta=-1 미설정 상태를 BlockState==null 또는 별도 boolean으로 대체

### ClimbGap.reset()
- 원본 동작: Block=-1, Meta=-1, CanStand=false, MustCrawl=false, Direction=null, SkipGaps=false. static 인스턴스를 매 틱 reset()으로 재사용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 객체 재사용 패턴은 그대로 유지 가능
- 포팅 주의사항: BlockState로 교체 시 null 초기화로 변경

### out_handsClimbGap / out_feetClimbGap (static 인스턴스)
- 원본 동작: `static ClimbGap out_handsClimbGap = new ClimbGap()`, `out_feetClimbGap` — 매 틱 reset() 후 출력용으로 재사용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음 (패턴 유지 가능)
- 포팅 주의사항: static 필드이므로 멀티플레이어에서 플레이어별 인스턴스가 필요한 경우 별도 관리 필요

### inout_handsClimbing[1] / inout_feetClimbing[1] (static 배열)
- 원본 동작: `static HandsClimbing[] inout_handsClimbing = new HandsClimbing[1]`, `inout_feetClimbing` — 참조 반환용 1-element 배열
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: Java의 out 파라미터 패턴. Java 21에서도 동일하게 구현 가능
- 포팅 주의사항: Java enum으로 변환해도 배열 참조 전달 패턴은 그대로 유지 가능

---

## SmartMovingContext 클라이밍 속도 상수

### 클라이밍 모션 상수
- 원본 동작:
  - `FastUpMotion = 0.2D` — 빠른 상승
  - `CatchCrawlGapMotion = 0.17D` — 크롤 갭 진입 시
  - `MediumUpMotion = 0.14D` — 중간 상승
  - `SlowUpMotion = 0.1D` — 느린 상승
  - `HoldMotion = 0.08D` — 제자리 유지
  - `SinkDownMotion = 0.05D` — 천천히 하강
  - `ClimbDownMotion = 0.01D` — 사다리 아래로 내려가기
  - `ClimbPullMotion = 0.3F` — 넝쿨 당기기 moveForward 값
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla applyClimbingSpeed()는 y max(-0.15F) 클램프만 적용. SM은 이 상수들로 정밀하게 모션 제어
- 포팅 주의사항:
  - `ClimbDownMotion = 0.01D`는 vanilla 속도 스냅 기준(|v|<0.003)보다 크므로 스냅에 걸리지 않음
  - `HoldMotion = 0.08D`도 스냅 기준 이상이므로 안전
  - vanilla `applyClimbingSpeed()`가 SM 클라이밍 중 호출되면 y max(-0.15F)가 HoldMotion(0.08D)을 덮어쓸 수 있음 — 간섭 방지 필요

---

## SmartMoving 클래스 (상태 플래그)

### isClimbing
- 원본 동작: 클라이밍 전체 활성 여부. SmartMoving 클래스에 선언. SmartMovingSelf에서 set, 렌더러에서 읽음
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla `LivingEntity.isClimbing()`은 CLIMBABLE 태그 블록만 인식. SM의 isClimbing은 free climbing, fence climbing 등 확장 상태 포함
- 포팅 주의사항: vanilla `isClimbing()` Mixin 시 SM 상태와 충돌 위험. applyClimbingSpeed(), applyMovementInput()의 isClimbing() 조건이 SM climbing 중에 의도치 않게 발동/미발동할 수 있음

### isHandsVineClimbing / isFeetVineClimbing / isVineOnlyClimbing / isVineAnyClimbing
- 원본 동작: 넝쿨 기반 클라이밍 세부 상태 분류. 애니메이션 및 속도 계산에 사용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla는 Vine 블록을 단순히 CLIMBABLE로 처리. SM은 손/발 각각의 넝쿨 접촉 여부를 별도로 추적
- 포팅 주의사항: 1.21.1 Vine 블록의 방향 데이터는 BlockState `NORTH/SOUTH/EAST/WEST` 프로퍼티로 저장됨. 1.7.10 메타데이터 비트 마스크와 대응 방식 다름

### isClimbJumping / isClimbBackJumping / isWallJumping
- 원본 동작:
  - `isClimbJumping`: 클라이밍 중 위쪽 점프. setShouldClimbSpeed에서 `!isClimbHolding`이면 true
  - `isClimbBackJumping`: 벽 뒤로 점프. onStartClimbBackJump()에서 rotateAngleY += Half 또는 Quarter
  - `isWallJumping`: 벽 점프. onStartWallJump(angle)에서 fallDistance=0F, rotateAngleY=angle
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla 점프는 단순 velocity Y 설정. SM은 회전 각도와 연동
- 포팅 주의사항: rotateAngleY (1.7.10 ModelBase 필드) → 1.21.1 ModelPart.yaw 또는 렌더 단계에서의 별도 yaw 오프셋으로 매핑 필요

### isClimbCrawling / isCrawlClimbing
- 원본 동작:
  - `isClimbCrawling`: 클라이밍 중 크롤 상태 (손으로 오르다가 머리가 낮은 공간)
  - `isCrawlClimbing`: 크롤하면서 등반 (수평 크롤 + 수직 이동 복합)
  - 두 상태는 climbIntoCount 카운터로 전환됨
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 개념 없음
- 포팅 주의사항: climbIntoCount 카운터 관리 및 전환 조건(공간 높이 체크)을 그대로 이식해야 함

### isCeilingClimbing
- 원본 동작: 천장 매달리기 상태. 특정 블록(fenceIron, trapdoor 등)에서 발동
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 개념 없음
- 포팅 주의사항: 블록 인식 시스템 전면 재구현 필요 (아래 supportsCeilingClimbing 참조)

### actualHandsClimbType / actualFeetClimbType
- 원본 동작: int 타입. HandsClimbing 상수(NoGrab=0, UpGrab=1, MiddleGrab=2) / FeetClimbing 상수(NoStep=0, DownStep=1) 저장. 네트워크 패킷에 각 4비트로 인코딩
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음 (int 그대로 사용 가능)
- 포팅 주의사항: 네트워크 패킷 비트 레이아웃 그대로 유지해야 클라이언트-서버 동기화 유지

### angleJumpType
- 원본 동작: 각도 점프 타입 (클라이밍과 연관된 특수 점프 상태)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 렌더 단계에서 회전 각도에 영향

---

## SmartMovingBase 클라이밍 관련 메서드

### supportsCeilingClimbing(i, j, k)
- 원본 동작: `Config._ceilingClimbConfigurationObject` (Dictionary<Object, Set<Integer>>)에서 블록 이름 또는 인스턴스로 조회. 블록 이름에서 "tile." 접두사 제거 시도. metaDatas.isEmpty()이면 모든 메타 허용, 아니면 메타값이 Set에 포함되는지 체크
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이:
  - 1.7.10: Block ID(int) + 메타(int) 시스템
  - 1.21.1: BlockState 기반. 블록 이름은 `Identifier` (예: `minecraft:iron_bars`), 블록 프로퍼티로 상태 구분
  - "tile.fenceIron" → `minecraft:iron_bars`로 이름 매핑 필요
  - "tile.trapdoor/0/1/2/3" → `minecraft:oak_trapdoor` + BlockState 프로퍼티 조건으로 변환 필요
  - "tile.trapdoor_iron/0/1/2/3" → `minecraft:iron_trapdoor` + BlockState 프로퍼티 조건
- 포팅 주의사항:
  - 설정 문자열 파싱 시스템 전면 재구현 필요
  - 메타 기반 조건 → BlockState 프로퍼티 기반으로 교체
  - 기본값 블록들의 1.21.1 identifier 사전 확인 필요

### getOnLadderOrVine(isSmall, faceOnly)
- 원본 동작:
  - `isSmall=true`이면 `minj--` (한 칸 아래까지 사다리 탐색)
  - `faceOnly=true`이면 `facedOnlyTo` 방향만 탐색
  - 4방향(North/South/East/West) 순서로 탐색
  - 사다리: 인접 블록의 사다리 방향이 rotate(180)==player direction이면 클라이밍 가능
  - 넝쿨: `hasVineOrientation && isRemoteSolid` — 넝쿨 방향이 있고 뒤 블록이 솔리드이면 가능
  - 반환: HandsClimbing + FeetClimbing 상태
- 1.21.1 대응: `LivingEntity.isClimbing()` (부분 대응), 없음 — 직접 구현 필요
- 동작 차이:
  - vanilla isClimbing(): CLIMBABLE 태그 블록 위에 있으면 단순 true. 방향 체크 없음
  - SM: 사다리 방향 + 플레이어 방향 일치 여부 체크로 더 정밀한 제어
  - 1.21.1 사다리는 `FACING` BlockState 프로퍼티로 방향 저장
  - 1.21.1 Vine은 `NORTH/SOUTH/EAST/WEST` boolean 프로퍼티
- 포팅 주의사항:
  - Mixin으로 플레이어 주변 블록을 직접 쿼리하는 로직 구현 필요
  - isSmall 분기(minj--)는 플레이어가 작을 때(크롤/슬라이딩) 발생 — 1.21.1 EntityDimensions 시스템과 연동
  - 사다리 방향 비교: `LadderBlock.FACING` → `Direction.getOpposite()` 비교로 교체

### climbingUpIsBlockedByLadder(direction)
- 원본 동작: 머리 위 블록이 사다리이고 해당 방향이 `direction`과 일치하면 위로 올라가기 차단. `isCollidedHorizontally && isCollidedVertically && !onGround && moveForward>0` 조건에서 호출
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 로직 없음
- 포팅 주의사항: 머리 위 블록 쿼리는 1.21.1 `World.getBlockState(BlockPos)` 사용. 충돌 상태(horizontalCollision 등)는 Entity 필드로 접근 가능

### climbingUpIsBlockedByTrapDoor()
- 원본 동작: 머리 위 트랩도어가 열려있지 않으면 위로 올라가기 차단
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.7.10 트랩도어 메타 → 1.21.1 `TrapdoorBlock.OPEN` BlockState 프로퍼티
- 포팅 주의사항: BlockState 프로퍼티 조회로 교체

### climbingUpIsBlockedByCobbleStoneWall()
- 원본 동작: 머리 위 돌담장 블록이면 위로 올라가기 차단
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.7.10 cobblestoneWall → 1.21.1 `Blocks.COBBLESTONE_WALL` 등 WallBlock 계열
- 포팅 주의사항: WallBlock 계열 블록 목록 확인 필요

### getMaxPlayerSolidBetween / getMinPlayerSolidBetween
- 원본 동작: 임시로 플레이어 boundingBox의 Y 범위를 변경한 뒤 해당 범위 내 충돌 박스를 쿼리하여 최대/최솟값 반환
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.21.1에서 `World.getBlockCollisions(entity, box)` 또는 `World.canPlace` 계열 메서드로 대체 가능
- 포팅 주의사항: boundingBox를 임시 변경하는 것은 위험할 수 있음. 별도 Box 인스턴스를 계산에 사용하는 방식으로 이식 권장

### moveFlying(x, y, z)
- 원본 동작: `total = sqrt(sqrt(x²+z²) + y²)` — 비대칭 공식. treeDimensional=false이면 순수 수평 합산. 결과로 motionX/Y/Z에 각 성분 비율 적용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla 이동 공식과 다름 — SM의 의도적 비대칭 설계
- 포팅 주의사항: 공식을 정확히 이식해야 함. 오기 시 클라이밍 점프 방향 왜곡

### reverseHandleMaterialAcceleration()
- 원본 동작: 물 흐름 가속 +0.014D를 -0.014D로 역전. 클라이밍 중 물 흐름에 의한 미끄러짐 방지
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.21.1 물 흐름 처리는 `FluidState.getVelocity()` + `Entity.setVelocity()` 경로. 정확한 상쇄값 재확인 필요
- 포팅 주의사항: 1.21.1에서 물 흐름 가속이 적용되는 위치(travel() 내)와 상쇄 타이밍 맞춰야 함

### calculateSeparateCollisions()
- 원본 동작: X/Y/Z 각 방향 충돌 비트 플래그 반환. 클라이밍 중 방향별 충돌 체크에 사용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla Entity는 `horizontalCollision`, `verticalCollision`, `collidedSoftly` 등 단순 boolean만 제공
- 포팅 주의사항: X/Z 방향 분리가 필요하면 `Entity.move()` 결과 비교 또는 별도 AABB 충돌 쿼리로 구현

### correctOnUpdate()
- 원본 동작: `0.02 < f < 0.05`이고 isSmall이면 renderYawOffset 보정. 클라이밍과 무관하지만 isSmall 판정과 연관
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 렌더러 Mixin에서 처리해야 함
- 포팅 주의사항: 렌더 단계에서의 yaw 보정이므로 LivingEntityRenderer Mixin에서 적용

---

## SmartMovingSelf 클라이밍 관련 필드

### wantClimbUp / wantClimbDown / wantClimbCeiling
- 원본 동작:
  - `wantClimbUp`: wantClimb && moveForward>0 || (vine && jump && ...)
  - `wantClimbDown`: wantClimb && moveForward≤0 && !wantCrawl
  - `wantClimbCeiling`: Config.isCeilingClimbingEnabled() && grabButton.Pressed && !wantCrawlNotClimb && !isSneaking() && !disabled
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 의도 플래그 없음
- 포팅 주의사항: grabButton → 1.21.1 KeyBinding으로 매핑 필요

### isClimbHolding
- 원본 동작: 클라이밍 중 제자리 유지 상태. setShouldClimbSpeed에서 relevant=false이면 isClimbJumping=!isClimbHolding
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: isClimbJumping 전환 로직과 연동

### isClimbingStill / isNeighborClimbing
- 원본 동작:
  - `isClimbingStill`: 완전히 정지한 클라이밍
  - `isNeighborClimbing`: 인접한 블록에 걸쳐 클라이밍. wantClimbDown && isNeighborClimbing → moveForward=ClimbPullMotion
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 인접 블록 클라이밍 탐색 로직 이식 필요

### hasClimbGap / hasClimbCrawlGap / hasNeighborClimbGap / hasNeighborClimbCrawlGap
- 원본 동작: 클라이밍 상단의 공간 여부 플래그. hasClimbCrawlGap && isClimbCrawling && motionY>HoldMotion → min(CatchCrawlGapMotion, motionY)로 속도 제한
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 공간 높이 쿼리 로직은 AABB 충돌 쿼리로 이식 가능

### handsEdgeBlock / handsEdgeMeta / feetEdgeBlock / feetEdgeMeta
- 원본 동작: 손/발이 닿아있는 엣지 블록 ID와 메타
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: Block int ID + Meta int → BlockState로 통합
- 포팅 주의사항: BlockState 단일 필드로 교체

### distanceClimbedModified / nextClimbDistance
- 원본 동작: 클라이밍 발소리 거리 추적. afterMoveEntity에서 `distanceClimbedModified += distance * (isClimbing ? 1.2 : 0.9)`. 발소리 볼륨 *0.15F
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla는 `limbAnimator` 기반 발소리 거리 계산. SM은 별도 수치로 관리
- 포팅 주의사항: 1.21.1 발소리는 `Entity.playStepSound()` → `Entity.addMovementEffects()` 경로. Mixin으로 거리 계산 커스터마이징 필요

### climbIntoCount
- 원본 동작: crawl gap 진입 카운터. isCrawlClimbing/isClimbCrawling 전환에 사용. 0 초과면 setShouldClimbSpeed에서 HoldMotion 강제
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 카운터 증감 조건과 초기화 시점을 정확히 이식해야 함

---

## SmartMovingSelf 클라이밍 관련 메서드

### getSlowInputSpeedFactor() — 클라이밍 부분
- 원본 동작: `isCeilingClimbing → *=_ceilingClimbingSpeedFactor (0.2F)`. `isCrawlClimbing && !isClimbCrawling → *=_crawlFactor`
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 없음
- 포팅 주의사항: 속도 팩터 적용 순서가 중요. 다른 팩터와 곱셈 순서 유지

### getNonSlowInputSpeedFactor() — 클라이밍 부분
- 원본 동작: `isClimbing && (strafe≠0 || forward≠0) → *=_freeClimbingHorizontalSpeedFactor`. `wantClimbDown && isNeighborClimbing → moveForward = ClimbPullMotion` (vine 조건 별도)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 없음
- 포팅 주의사항: moveForward 강제 설정은 travel() 직전에 적용되어야 함

### handleLand() — 클라이밍 부분
- 원본 동작: isOnLadder(isClimbCrawling), isOnVine(isClimbCrawling) 체크 후 handleClimbing, handleCeilingClimbing 순서 호출
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla의 onLanding() 콜백과 다름
- 포팅 주의사항: 호출 순서(handleClimbing → handleCeilingClimbing) 유지

### landMotion() — 클라이밍 부분
- 원본 동작:
  - 사다리/넝쿨 위: motionX/Z ±0.15F 클램프
  - fallDistance = 0F (낙하 데미지 방지)
  - motionY = max(motionY, -0.15 * getCombinedSpeedFactor())
  - climbingUpIsBlocked 체크 → moveFlying(0F, -1F, 0.07F 또는 0.09F)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이:
  - vanilla applyClimbingSpeed()도 x/z ±0.15F, y max(-0.15F) 적용. SM은 getCombinedSpeedFactor() 추가로 곱함
  - fallDistance 리셋은 Entity.fallDistance 필드 직접 조작
- 포팅 주의사항: vanilla applyClimbingSpeed()와 중복 적용되지 않도록 분리 필요

### handleClimbing() (lines 814-1108)
- 원본 동작:
  - **Standard 모드**: `motionY = 0.2 * getCombinedSpeedFactor()`
  - **Simple 모드**: feet+hands FastUp → FastUpMotion; feet FastUp → MediumUpMotion; hands IsUp → SlowUpMotion
  - **Smart 모드**: substitute 판정으로 동작 결정
  - **Free 모드**:
    1. exhaustion 체크: `exhaustion <= _climbExhaustionStop && (wasClimbing || exhaustion <= _climbExhaustionStart)`
    2. 4방향(PZ,NZ,ZP,ZN) + 대각 4방향(PP,NP,NN,PN) 탐색
    3. HandsClimbing/FeetClimbing max()로 합산
    4. 속도 결정: wantClimbUp/Down 및 enum 값에 따라 결정
  - 넝쿨 판정 별도 처리
  - climbBackJump/wallJump 처리
  - handleCrash 호출
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이:
  - vanilla는 isClimbing()이면 applyMovementInput()에서 y=0.2 강제. SM은 모드별 세밀한 제어
  - SM Free 모드: 8방향 탐색 로직이 핵심 → 1.21.1 블록 쿼리로 재구현
- 포팅 주의사항:
  - 블록 탐색(4방향+대각)은 `World.getBlockState(BlockPos)` 직접 사용
  - getCombinedSpeedFactor()와 속도 팩터 곱셈 순서 유지
  - exhaustion은 Entity의 hunger/exhaustion 시스템과 무관한 SM 독자 필드

### handleCeilingClimbing()
- 원본 동작:
  - exhaustion 체크: `exhaustion <= _ceilingClimbExhaustionStop && (wasCeilingClimbing || exhaustion <= _ceilingClimbExhaustionStart)`
  - 조건: `wantClimbCeiling && !isClimbing && (!isCrawling || conflict) && !isCrawlClimbing`
  - jgap(머리 위 갭 크기)에 따른 Y 모션: jgap>1.2→0.12, jgap>1.115→0.08, else→0.04
  - fallDistance = 0F
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla에 해당 없음
- 포팅 주의사항: jgap 계산은 천장 블록까지의 거리. AABB 충돌 쿼리로 구현 필요

### resetClimbing()
- 원본 동작: isClimbing, isHandsVineClimbing, isFeetVineClimbing, isClimbJumping, isClimbBackJumping, isWallJumping, isClimbCrawling, isCrawlClimbing, isCeilingClimbing 모두 false. actualHandsClimbType=NoGrab, actualFeetClimbType=NoStep
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 매 틱 시작 시 반드시 호출되어야 함. 미호출 시 상태 sticky 버그 발생

### setShouldClimbSpeed(value, isUp, factor)
- 원본 동작:
  - `climbIntoCount > 0` → HoldMotion 강제 (crawl gap 진입 중)
  - 상승(isUp=true): `(value - HoldMotion) * _freeClimbingUpSpeedFactor * factor + HoldMotion`
  - 하강(isUp=false): `HoldMotion - (HoldMotion - value) * _freeClimbingDownSpeedFactor * factor`
  - `hasClimbCrawlGap && isClimbCrawling && value > HoldMotion → min(CatchCrawlGapMotion, value)`
  - relevant 판정: value<0 || value>motionY
  - `isClimbJumping = !relevant && !isClimbHolding`
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla 속도 설정은 단순 setVelocity(). SM은 팩터 보간 적용
- 포팅 주의사항: motionY → 1.21.1 `Entity.getVelocity().y` 로 대응

### setOnlyShouldClimbSpeed(value, isUp, factor)
- 원본 동작: setShouldClimbSpeed와 동일하나 isClimbing=true를 추가로 설정
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: setShouldClimbSpeed와 혼용 주의

### afterMoveEntity() — 클라이밍 부분
- 원본 동작: `distanceClimbedModified += distance * (isClimbing ? 1.2 : 0.9)`. 블록 발소리 볼륨 *0.15F
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla는 limbAnimator 거리 계산. SM은 별도 추적
- 포팅 주의사항: 발소리 볼륨 조정은 Mixin으로 SoundEvent 발생 직전에 적용

### updateEntityActionState() — 클라이밍 부분
- 원본 동작:
  - `grabButton.Pressed || autoLadder || autoVine → wouldWantClimb = true`
  - `wantClimbUp = wantClimb && moveForward>0 || (vine && jump && ...)`
  - `wantClimbDown = wantClimb && moveForward≤0 && !wantCrawl`
  - `wantClimbCeiling = Config.isCeilingClimbingEnabled() && grabButton.Pressed && !wantCrawlNotClimb && !isSneaking() && !disabled`
  - `isClimbHolding` 조건 설정
  - isCrawlClimbing/isClimbCrawling 전환(climbIntoCount 카운터)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla KeyBinding 시스템으로 grabButton 구현. autoLadder/autoVine은 config 기반
- 포팅 주의사항: `ClientPlayerEntity.input` 또는 별도 KeyBinding 클래스로 grabButton 구현 필요

### addToSendQueue() — 클라이밍 관련 비트
- 원본 동작: 단일 int 패킷에 비트 인코딩:
  - bit 0: isClimbing
  - bit 1: isHandsVineClimbing
  - bit 2: isFeetVineClimbing
  - bit 3: isClimbJumping
  - bit 4: isClimbBackJumping
  - bit 5: isWallJumping
  - bit 6: isCeilingClimbing
  - bits 7-10: actualHandsClimbType (4비트)
  - bits 11-14: actualFeetClimbType (4비트)
  - bit 15 (다른 위치): isCrawlClimbing
- 1.21.1 대응: 없음 — 직접 구현 필요 (Fabric 네트워킹)
- 동작 차이: 1.7.10 Netty 패킷 → Fabric `PacketByteBuf` 기반으로 전환
- 포팅 주의사항: 비트 레이아웃 정확히 유지해야 서버/클라이언트 동기화 보장

---

## SmartMovingConfig 클라이밍 관련 설정

### _baseClimb (String)
- 원본 동작: "free"(기본값), "smart", "simple", "standard" 중 하나. _isFreeBaseClimb 등 computed boolean 파생
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: config 파일 파싱 시스템 재구현 필요 (Fabric Config API 또는 직접 구현)

### _freeClimb (boolean)
- 원본 동작: true이면 Free climbing 활성화
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: _baseClimb와 별개 설정. 둘 다 체크 필요

### _freeClimbingUpSpeedFactor / _freeClimbingDownSpeedFactor / _freeClimbingHorizontalSpeedFactor
- 원본 동작: setShouldClimbSpeed의 보간 계산에 사용되는 속도 팩터
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 기본값 확인 필요 (SmartMovingConfig.md에서 기본값 명시 확인 요망)

### _freeOneLadderClimbUpSpeedFactor / _freeBothLadderClimbUpSpeedFactor
- 원본 동작: 한쪽 사다리(1.0153F), 양쪽 사다리(1.43F) 상승 속도 팩터
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 한쪽/양쪽 사다리 판정 로직 이식 필요

### _freeFenceClimbing (boolean)
- 원본 동작: 담장(fence) 클라이밍 허용 여부
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 1.21.1 fence 블록은 FenceBlock 계열. 블록 태그 `minecraft:fences` 사용 가능
- 포팅 주의사항: 1.21.1에서 담장 블록 태그로 판별 권장

### _freeClimbAutoLadder / _freeClimbAutoVine
- 원본 동작: grabButton 없이 자동으로 사다리/넝쿨 클라이밍
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: updateEntityActionState의 wouldWantClimb 조건에 직접 영향

### _climbExhaustionStart / _climbExhaustionStop
- 원본 동작: 기본값 60F/100F. exhaustion이 Start 이하이거나 이미 클라이밍 중이면 허용. Stop 초과면 차단
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla hunger/exhaustion과 별개의 SM 독자 exhaustion 필드
- 포팅 주의사항: SM exhaustion 증감 로직 전체를 이식해야 함

### _ceilingClimbing (boolean)
- 원본 동작: 천장 클라이밍 기능 활성화 여부
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: isCeilingClimbingEnabled() 체크와 연동

### _ceilingClimbingSpeedFactor (float)
- 원본 동작: 기본값 0.2F. getSlowInputSpeedFactor()에서 천장 클라이밍 속도에 곱함
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: 없음

### _ceilingClimbConfigurationString / _ceilingClimbConfigurationObject
- 원본 동작:
  - 기본값: `"tile.fenceIron"`, `"tile.trapdoor/0/1/2/3"`, `"tile.trapdoor_iron/0/1/2/3"`
  - Dictionary<Object, Set<Integer>>: 블록 이름 또는 인스턴스 → 허용 메타 Set
  - "tile." 접두사 제거 후 이름 비교
  - metaDatas.isEmpty() → 모든 메타 허용
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이:
  - `tile.fenceIron` → `minecraft:iron_bars`
  - `tile.trapdoor` → `minecraft:oak_trapdoor` (및 기타 목재 트랩도어)
  - `tile.trapdoor_iron` → `minecraft:iron_trapdoor`
  - 메타값 "/0/1/2/3" → BlockState 프로퍼티로 변환 필요
- 포팅 주의사항:
  - 설정 파싱 시스템 전면 재설계 필요
  - 블록 이름 매핑 테이블 작성 필요
  - BlockState 프로퍼티 조건 표현 방식 설계 필요 (예: `open=false`)

### _ceilingClimbExhaustionStart / _ceilingClimbExhaustionStop
- 원본 동작: 기본값 40F/100F. 일반 클라이밍보다 낮은 Start 값 (더 일찍 차단)
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: 없음
- 포팅 주의사항: SM 독자 exhaustion 필드 관리

### _freeClimbFallDamageStartDistance / _freeClimbFallDamageFactor / _freeClimbFallMaximumDistance
- 원본 동작: 기본값 각각 (values 2F/1F/3F), 2F, 3F. 클라이밍 후 낙하 데미지 커스텀 계산
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: vanilla 낙하 데미지는 `LivingEntity.fall()` 메서드에서 처리
- 포팅 주의사항: Mixin으로 `fall()` 또는 `damage()` 후킹 필요

---

## SmartMovingModel 클라이밍 애니메이션

### isClimb 분기 (isClimbing && !isCrawling && !isCrawlClimbing && !isClimbJumping)
- 원본 동작:
  - `bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle`
  - handsClimbType 스위치:
    - MiddleGrab: FrequenceUpFactor=0.6662F, DistanceUpFactor=2F, DistanceUpOffset=-Quarter
    - UpGrab: 동일 Freq/Dist, Offset=-2.5F
    - NoGrab(default): DistanceUpFactor=0, Offset=-0.5F
  - feetClimbType 스위치:
    - UpGrab: DistanceUpFactor=0.3F / verticalSpeed
    - default: 0
  - 팔 회전: `cos(totalVerticalDistance * FrequenceUpFactor + Half) * verticalSpeed * DistanceUpFactor + DistanceUpOffset`
  - 발 회전: `cos(totalVerticalDistance * FrequenceUpFactor) * fDist * verticalSpeed + fOffset`
  - isHandsVineClimbing: arm Y *= (1 + FrequenceUpFactor) = 1.6662, ±Eighth 추가
  - isFeetVineClimbing: `(cos(totalDistance + Half) + 1) * Thirtytwoth + Sixteenth` 패턴
  - handsClimbType==NoGrab && feetClimbType!=NoStep: torso.rotateAngleX=0.5F, head-=0.5F, pelvic-=0.5F, torso.rotationPointZ=-6.0F
- 1.21.1 대응: `PlayerEntityModel.setAngles()` Mixin — 없음 — 직접 구현 필요
- 동작 차이:
  - 1.7.10 ModelBase: bipedOuter, bipedHead, bipedBody 등 직접 접근
  - 1.21.1 PlayerEntityModel: `head`, `body`, `leftArm`, `rightArm`, `leftLeg`, `rightLeg` ModelPart
  - SmartRender의 ModelRotationRenderer(bipedOuter) → 1.21.1에서 별도 부모 ModelPart로 추가하거나 렌더러에서 직접 변환 행렬 조작
  - `rotateAngleY` → `ModelPart.yaw`, `rotateAngleX` → `ModelPart.pitch`, `rotationPointZ` → `ModelPart.pivotZ`
- 포팅 주의사항:
  - bipedOuter는 SmartRender의 커스텀 ModelPart — 1.21.1에서 재구현 또는 대체 방식 설계 필요
  - totalVerticalDistance, totalHorizontalDistance 등 SmartStatistics 값 연동 필요
  - RadiantToAngle 상수 확인 필요 (SmartMovingContext에서 정의)
  - Half, Quarter, Eighth, Thirtytwoth, Sixteenth 각도 상수 확인 필요

### isCrawlClimb 분기 (isCrawlClimbing || (isClimbing && isCrawling))
- 원본 동작:
  - smallOverGroundHeight 기반 계산:
    - `height = smallOverGroundHeight + 0.25F`
    - `height < bodyLength(0.7F)` → `bodyAngleX = acos(height/0.7F)`, `legAngleX = Quarter - bodyAngleX`
    - `height < 1.25F` → `bodyAngleX = 0`, `legAngleX = acos((height-0.7F)/0.55F)`
  - 조건별 torso/shoulder/head/leg pitch/roll 설정
  - isClimb 분기와 병합 처리
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: smallOverGroundHeight는 SmartStatistics 시스템에서 보간된 값
- 포팅 주의사항: SmartStatistics.smallOverGroundHeight 계산 이식 필요. 1.21.1에서 플레이어 위치와 지면 블록 간 거리를 실시간 계산해야 함

### isClimbJump 분기
- 원본 동작: 팔을 Half + Sixteenth 앞으로, ±Thirtytwoth Z 방향으로 벌림
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: ModelPart 각도 설정으로 이식 가능
- 포팅 주의사항: 각도 상수(Half, Sixteenth, Thirtytwoth) 값 확인 필요

### isCeilingClimb 분기
- 원본 동작:
  - `distance = totalHorizontalDistance * 0.7F`
  - `walkFactor threshold = 0.12951545F` (이 임계값으로 팔/다리 전환)
  - 팔: `cos(distance) * (±0.52F) + Half` 패턴
  - 다리: `-cos(distance) * 0.12F` 또는 `0.32F` 패턴
  - `bipedOuter.rotateAngleY = rotateY + horizontalAngle`
- 1.21.1 대응: 없음 — 직접 구현 필요
- 동작 차이: bipedOuter → 1.21.1 ModelPart 대체 필요. totalHorizontalDistance → SmartStatistics 연동
- 포팅 주의사항: horizontalAngle 계산 (플레이어 이동 방향 기반) 이식 필요

---

## SmartMovingRender 클라이밍 관련 처리

### renderPlayer() — 클라이밍 상태 변환
- 원본 동작:
  - `isClimb = moving.isClimbing && !moving.isCrawling && !moving.isCrawlClimbing && !moving.isClimbJumping`
  - `isClimbJump = moving.isClimbJumping`
  - `isCeilingClimb = moving.isCeilingClimbing`
  - `isCrawlClimb = moving.isCrawlClimbing || (moving.isClimbing && moving.isCrawling)`
- 1.21.1 대응: `PlayerEntityRenderer` Mixin — 없음 — 직접 구현 필요
- 동작 차이: vanilla 렌더러에서 이러한 상태 필터링 없음
- 포팅 주의사항: Mixin으로 PlayerEntityRenderer.render() 또는 setupTransforms() 호킹 필요

### rotatePlayer() — 클라이밍 yaw 처리
- 원본 동작: `isClimbing || isClimbCrawling || isCrawlClimbing || isFlying || isSwimming || isDiving || isCeilingClimbing || isHeadJumping || isSliding || isAngleJumping()` → `renderYawOffset = forwardRotation`
- 1.21.1 대응: `PlayerEntityRenderer.setupTransforms()` Mixin — 없음 — 직접 구현 필요
- 동작 차이: vanilla setupTransforms는 state 기반 변환만 적용. SM은 이동 방향 yaw를 강제
- 포팅 주의사항: forwardRotation(이동 방향 yaw)을 bodyYaw로 강제하는 로직. 1.21.1에서 `MatrixStack.multiply()` 또는 `entity.bodyYaw` 조작으로 구현

### renderPlayerAt() — heightOffset 적용
- 원본 동작: `EntityOtherPlayerMP && heightOffset != 0 → d1 += heightOffset` (Y 위치 보정)
- 1.21.1 대응: `PlayerEntityRenderer.getPositionOffset()` Mixin — 없음 — 직접 구현 필요
- 동작 차이: vanilla getPositionOffset()은 고정값 반환. SM은 heightOffset 동적 추가
- 포팅 주의사항: 멀티플레이어 다른 플레이어 렌더 시에만 적용. LocalPlayer 제외

### renderName() — 클라이밍/크롤 이름표
- 원본 동작:
  - `isCrawling && !isClimbing → temporaryIsSneaking = !_crawlNameTag.value`
  - `heightOffset == -1 → d1 -= 0.2F`
  - `originalIsSneaking && !temporary → d1 -= 0.05F`
- 1.21.1 대응: `EntityRenderer.renderLabelIfPresent()` Mixin — 없음 — 직접 구현 필요
- 동작 차이: 이름표 Y 위치 조정 로직
- 포팅 주의사항: `heightOffset==-1`은 크롤링 시 발생. 1.21.1 이름표 위치 조정은 renderLabelIfPresent()에서 Y 오프셋으로

---

## vanilla 클라이밍 관련 API

### LivingEntity.isClimbing()
- 원본 동작: CLIMBABLE 태그 블록(사다리, 덩굴, 발판 등) 위에 있으면 true. trapdoor는 특정 조건
- SM 충돌:
  - SM의 확장 클라이밍(free climbing, fence climbing, ceiling climbing)은 여기서 false 반환
  - SM isClimbing=true인데 vanilla isClimbing()=false → applyClimbingSpeed() 미호출
  - 반대로 SM이 vanilla isClimbing()을 오버라이드하면 applyMovementInput()의 y=0.2 강제가 발동
- 포팅 주의사항: SM 클라이밍 상태와 vanilla isClimbing() 사이의 관계를 명확히 정의해야 함. 가장 안전한 방법은 vanilla isClimbing()을 Mixin으로 오버라이드하지 않고 SM이 직접 velocity 제어

### LivingEntity.applyClimbingSpeed()
- 원본 동작: isClimbing()이면 x/z ±0.15F 클램프, y max(-0.15F). 스니킹 + 비발판 사다리 + PlayerEntity → y=0
- SM 충돌:
  - SM 클라이밍 중 vanilla isClimbing()=false이면 이 메서드 미호출 → x/z 클램프 없음
  - SM 클라이밍 중 vanilla isClimbing()=true이면 x/z 클램프가 SM 속도 제어와 충돌
- 포팅 주의사항: landMotion()의 motionX/Z 클램프(±0.15F)와 vanilla applyClimbingSpeed()의 중복 방지

### LivingEntity.applyMovementInput() — 클라이밍 분기
- 원본 동작: `(horizontalCollision || jumping) && isClimbing() → velocity.y = 0.2`
- SM 충돌:
  - SM 클라이밍 중 vanilla isClimbing()=false이면 이 분기 미발동
  - SM 자체적으로 motionY를 설정하므로 이 분기가 발동되면 SM 속도 덮어쓰기 발생
- 포팅 주의사항: SM 클라이밍 중 이 분기를 억제하거나 SM velocity 설정 이후에 처리해야 함

### LivingEntity.isHoldingOntoLadder()
- 원본 동작: `isSneaking()` — 스니킹이면 사다리에서 하강 방지
- SM 충돌: SM은 별도 climbHolding 로직. isSneaking()과 별개
- 포팅 주의사항: SM 클라이밍 중 isSneaking()=false이어도 홀딩 상태면 y=0 적용되어야 함

### Entity.fallDistance
- 원본 동작: 낙하 거리 추적. 착지 시 데미지 계산에 사용
- SM 사용: 클라이밍 중 fallDistance=0F로 리셋하여 낙하 데미지 방지
- 포팅 주의사항: 1.21.1에서 `Entity.fallDistance` 필드 직접 접근 가능. Mixin accessor 필요

### LivingEntity.updateLimbs()
- 원본 동작: travel() 끝에 항상 호출. limbAnimator 업데이트. 클라이밍 애니메이션과 무관하게 항상 실행
- SM 충돌: SM이 setAngles()를 완전히 오버라이드하면 limbAnimator 값은 사용되지 않으나 updateLimbs() 자체는 계속 실행됨
- 포팅 주의사항: 문제 없음. SM의 클라이밍 애니메이션은 setAngles()에서 limbAnimator를 무시하고 별도 totalVerticalDistance 등을 사용

### vanilla 속도 스냅 (|v| < 0.003 → 0)
- 원본 동작: tickMovement에서 x/y/z 각각 |v|<0.003이면 0으로 스냅
- SM 클라이밍 속도와의 관계:
  - `HoldMotion = 0.08D` → 스냅 기준(0.003) 이상, 스냅 안 됨
  - `ClimbDownMotion = 0.01D` → 스냅 기준(0.003) 이상, 스냅 안 됨
  - `SinkDownMotion = 0.05D` → 스냅 기준 이상, 안전
- 포팅 주의사항: 모든 SM 클라이밍 속도 상수가 스냅 기준 이상이므로 스냅 문제 없음

### ServerPlayNetworkHandler.onPlayerMove() — 클라이밍 관련
- 원본 동작:
  - `moved too quickly`: threshold=100 (non-elytra). `(distanceSq - velocityLenSq) > 100 * packetsSinceLastTick`이면 rubber-band
  - `moved wrongly`: move() 후 서버-클라이언트 위치 차이 > 0.0625이면 경고/rubber-band
  - `floating`: 공중에 비합법적으로 있으면 floatingTicks++, 80틱 초과 시 kick
- SM 클라이밍 충돌:
  - free climbing, ceiling climbing은 공중에서 Y velocity가 작거나 0 → floating 판정 위험
  - 클라이밍 중 이동 거리가 서버 물리 계산과 다르면 "moved wrongly" 발생
- 포팅 주의사항:
  - SM 클라이밍 상태를 서버에서 인식하여 floating 판정 예외 처리 필요
  - 서버에서도 클라이밍 속도 계산을 동일하게 수행해야 "moved wrongly" 방지
  - SmartMovingServer/SmartMovingServerPlayerBase에서 서버 측 클라이밍 처리 로직 이식 필요

---

## 전체 포팅 난이도 요약

### 최고 난이도 (완전 재구현 필요)

| 항목 | 이유 |
|---|---|
| `supportsCeilingClimbing()` 블록 인식 | Block ID/Meta → BlockState, 설정 파싱 시스템 전면 재설계 |
| `getOnLadderOrVine()` | 방향 기반 블록 탐색, isSmall 분기, 1.21.1 BlockState 방향 프로퍼티 매핑 |
| 클라이밍 애니메이션 (`setAngles()`) | bipedOuter 커스텀 ModelPart, SmartStatistics 연동, 수십 개의 각도 상수 |
| `handleClimbing()` Free 모드 | 8방향 탐색 + exhaustion + 속도 보간 전체 로직 |
| 서버 side 클라이밍 검증 | floating 판정 예외, "moved wrongly" 방지, 서버 물리 동기화 |
| 네트워크 패킷 인코딩 | 비트 레이아웃 보존, Fabric 네트워킹 재구현 |

### 고 난이도 (구조적 변환 필요)

| 항목 | 이유 |
|---|---|
| `HandsClimbing` / `FeetClimbing` Typesafe Enum → Java enum | max(), IsRelevant() 등 메서드 재구현 |
| `ClimbGap.Meta` → BlockState | Block+Meta 쌍 → BlockState 단일 객체 |
| vanilla `isClimbing()` / `applyClimbingSpeed()` 간섭 | SM climbing과 vanilla climbing 경계 정의 필요 |
| `applyMovementInput()` y=0.2 강제 억제 | SM climbing 중 vanilla 분기 방지 Mixin |
| `renderYawOffset` 강제 설정 | 1.21.1 렌더러 Mixin, bodyYaw 조작 |

### 중 난이도 (이식 가능, 주의 필요)

| 항목 | 이유 |
|---|---|
| `moveFlying()` 비대칭 공식 | 공식 정확히 이식만 하면 됨 |
| `setShouldClimbSpeed()` 속도 보간 | motionY → velocity.y 대응만 필요 |
| `distanceClimbedModified` 발소리 | Mixin으로 발소리 볼륨 조작 |
| `reverseHandleMaterialAcceleration()` | 1.21.1 물 흐름 가속 위치 확인 필요 |
| `climbIntoCount` 카운터 | 로직 단순, 이식 용이 |
| `fallDistance` 리셋 | accessor Mixin으로 직접 접근 가능 |

### 확인 필요 사항 (미확인)

- SmartMovingConfig.md에서 `_freeClimbingUpSpeedFactor`, `_freeClimbingDownSpeedFactor`, `_freeClimbingHorizontalSpeedFactor` 기본값 재확인 필요
- SmartMovingContext.md에서 `RadiantToAngle`, `Half`, `Quarter`, `Eighth`, `Thirtytwoth`, `Sixteenth` 상수값 재확인 필요
- SmartStatistics의 `totalVerticalDistance`, `totalHorizontalDistance`, `smallOverGroundHeight` 계산 방식 — SmartStatistics.md 별도 확인 필요
- 1.21.1에서 `calculateSeparateCollisions()` 대체 API 정확한 메서드 시그니처 미확인
- `_freeClimbFallDamageStartDistance`의 "values 2F/1F/3F" 의미 (세 값의 역할) 미확인
