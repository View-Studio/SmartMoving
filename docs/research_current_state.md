# 우리 모드 현재 구현 상태 분석

## 1. 상태 플래그 (SmartMovingState)

### 구현됨
| 플래그 | 설명 |
|--------|------|
| isCrawling, wasCrawling | 기어가기 |
| isClimbing, wasClimbing | 벽 클라이밍 |
| isCrawlClimbing, isClimbCrawling | 크롤+클라이밍 혼합 |
| isSwimming, isDiving, isDipping | 수영/잠수/물가 |
| isSliding | 슬라이딩 |
| isCeilingClimbing | 천장 클라이밍 |
| isHeadJumping | 헤드점프 중 |
| isWallJumping | 벽점프 (상태만, 물리 없음) |
| isHandsVineClimbing, isFeetVineClimbing | 덩굴 클라이밍 |
| isLevitating, isAerodynamic | 부양/공기역학 (상태만, 미사용) |
| isFast, isSlow | 속도 플래그 |
| isGroundSprinting | 지상 스프린트 |
| wantClimbUp, wantClimbDown, wantClimbCeiling | 클라이밍 의도 |
| wantCrawlNotClimb | 크롤-클라이밍 금지 |
| blockJumpTillButtonRelease | 점프 차단 플래그 |
| angleJumpType | 각도 점프 타입 (0~7) |
| feetClimbingType, handsClimbingType | 클라이밍 타입 enum |
| jumpCharge, headJumpCharge | 점프 충전량 |
| exhaustion, maxExhaustionForAction, maxExhaustionToStartAction | 탈진 (적립 로직 없음) |
| dippingDepth | 물가 깊이 |
| heightOffset | 렌더 Y 오프셋 (설정 안 됨) |
| updateCounter | 틱 카운터 |
| forwardButton, backButton, leftButton, rightButton | 이동 버튼 |
| jumpButton, sprintButton, sneakButton, grabButton | 액션 버튼 |

### 없음 (미구현)
| 플래그 | 설명 |
|--------|------|
| crawlToggled, sneakToggled | 토글 모드 |
| wouldIsSneaking | 스니크 의도 |
| isClimbingStill, isClimbHolding | 클라이밍 정지/홀드 |
| leftJumpCount, rightJumpCount, backJumpCount | 각도 점프 더블탭 카운터 → JumpHandler에 있음 |
| collidedHorizontallyTickCount | 수평 충돌 틱 카운터 |
| fadingPerspectiveFactor | 관점 페이딩 |
| prevMotionX/Y/Z | 이전 프레임 속도 |
| wasOnGround | 이전 프레임 지상 |

---

## 2. Physics Handlers 구현 현황

### SwimmingHandler ✅ 기본 구현
- 수위 계산: 구현됨
- isDipping / isSwimming / isDiving 전환: 구현됨
- applySwimPhysics: 구현됨
- 부력 조정 (7단계 그라디언트): 구현됨
- 잠수 피치 기반 수직 속도: 구현됨
- **미흡**: swimSpeedFactor가 실제 수영 속도에 미적용

### CrawlingHandler ✅ 기본 구현
- mustCrawl (머리 공간 체크): 구현됨
- canCrawl 조건: 구현됨
- wantCrawl (grab+sneak 조합): 구현됨
- isCrawling 상태 전이: 구현됨
- **미흡**: 토글 모드 없음 (crawlToggled)

### ClimbingHandler ✅ 기본 구현
- grab+jump = 위, grab+sneak = 아래: 구현됨
- isOnClimbableBlock: 구현됨
- FeetClimbing/HandsClimbing 타입 설정: 구현됨
- 덩굴 추적: 구현됨
- isRopeSliding: 구현됨
- **미흡**: 클라이밍에서 뛰어오르기(ClimbJump, ClimbBackJump) 없음

### CeilingClimbingHandler ✅ 기본 구현
- CLIMBABLE 태그 기반 감지: 구현됨
- jgap 계산 (3단계 Y속도): 구현됨
- getSolidHeightAbove 장애물 체크: 구현됨
- **미흡**: 딱히 없음

### SlidingHandler ✅ 기본 구현
- grab+sprint+sneak.start 진입: 구현됨
- 속도 기반 해제: 구현됨
- applySlidePhysics (슬리퍼리니스 기반 감속): 구현됨
- 방향키 방향 조정: 구현됨

### JumpHandler ⚠️ 부분 구현
- ChargeUp 충전/발동: 구현됨
- HeadUp 충전/발동: 구현됨
- 바닐라 점프 인터셉트: 구현됨
- **미구현**: 각도 점프 더블탭 감지 로직 (leftJumpCount 등 있지만 더블탭 실제 감지 없음)
- **미구현**: 벽 점프 물리
- **미구현**: 클라이밍 점프 (ClimbJump, ClimbBackJump)
- **미구현**: 헤드점프 수평→수직 속도 재분배

### 탈진 시스템 ❌ 미구현
- Config에 climbExhaustionEnabled = false
- exhaustion 필드는 있지만 값이 쌓이지 않음
- HUD는 표시 준비됐으나 값이 항상 0

---

## 3. 물리 연결 (LivingEntityTravelMixin)

### 구현됨
- @ModifyVariable: 크롤/천장클라이밍 속도 배율 조정
- @Inject INVOKE LivingEntity.move: 클라이밍/슬라이딩/수영 속도 적용

### 문제
- ClimbingHandler.HOLD_MOTION (0.08D) 값이 TravelMixin에서 하드코딩되지 않고
  wantClimbUp/Down이 아닐 때 0.0D로 처리됨 → 홀드 상태에서 추락

---

## 4. 히트박스

### LivingEntityDimensionsMixin ✅ 구현됨
- isSmall() (crawling||sliding||ceilingClimbing) → 0.6×0.8, eyeHeight 0.48

### PlayerEntityRendererMixin ✅ 구현됨
- getPositionOffset에 heightOffset 추가

### 문제 ❌
- **heightOffset가 어디서도 set되지 않음** → 항상 0
- 히트박스는 줄어드는데 렌더 위치가 보정 안 됨 → 플레이어가 공중에 뜨거나 땅에 묻힘

---

## 5. 애니메이션

### 구현됨
| 상태 | 애니메이션 |
|------|-----------|
| isCrawling | applyCrawlingAngles |
| isClimbing | applyClimbingAngles |
| isCeilingClimbing | applyCeilingClimbingAngles |
| isSliding | applySlidingAngles |
| isSwimming | applySwimmingAngles |
| isDiving | applyDivingAngles |
| isHeadJumping | applyHeadJumpingAngles |

### 미구현
| 상태 | 원본 애니메이션 |
|------|---------------|
| isWallJumping | 벽 점프 애니메이션 |
| isClimbJumping | 클라이밍에서 점프 |
| isClimbBackJumping | 클라이밍에서 뒤로 점프 |
| isAngleJumping | 각도 점프 자세 |
| isFlying / isLevitating | 비행/부양 자세 |
| isRopeSliding | 로프 하강 자세 |
| isCrawlClimbing | 좁은 공간 클라이밍 자세 |
| isFalling | 낙하 자세 |
| isSneaking (override) | 스니크 중 크롤 이름표 숨기기 |

---

## 6. 네트워킹

### 구현됨
- SmartMovingStatePayload (C2S, S2C relay): 구현됨
- StateEncoder 64비트 인코딩: 구현됨
- delta compression (이전 상태와 같으면 전송 안 함): 구현됨
- 원격 플레이어 상태 적용 (RemotePlayerManager): 구현됨

### 미구현
- Config 동기화 패킷
- Speed 변경 패킷
- 클라이밍 점프 콜백 (isClimbBackJumping, isWallJumping 콜백)

---

## 7. HUD

### 구현됨
- icons.png 기반 스프라이트 렌더링: 구현됨
- 점프 차지 바 (left, 9×9 아이콘): 구현됨
- 탈진 바 (right, 9×9 아이콘): 구현됨
- 갑옷/물속 Y오프셋: 구현됨

### 미흡
- 탈진 값이 항상 0 (시스템 미구현)
- 원본의 threshold 색상 구분 (green/yellow/red) 미구현

---

## 8. 설정 (Config)

### 구현됨
- 기능 ON/OFF 토글: 구현됨
- 물리 상수: 구현됨
- JSON 파일 저장/로드: 구현됨

### 미구현
- 인게임 설정 토글 UI (config_toggle 키는 있지만 화면 없음)
- 속도 증가/감소 키 바인딩 (speed_increase, speed_decrease 키는 있지만 로직 없음)
- 비행 관련 설정
- 각도점프 세부 설정
