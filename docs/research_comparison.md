# 원본 vs 우리 모드 비교 분석

## 우선순위 분류
- 🔴 **버그**: 현재 구현이 잘못 작동함
- 🟠 **핵심 누락**: 원본의 중요 기능이 없음
- 🟡 **세부 누락**: 있으면 좋은 기능
- 🟢 **정상**: 제대로 구현됨

---

## 버그 목록 🔴

### B-1: heightOffset 미설정 → 렌더 위치 오류
- **현상**: 크롤링/슬라이딩/천장클라이밍 시 히트박스는 줄어드는데 플레이어 렌더가 보정 안 됨
- **원인**: `SmartMovingState.heightOffset` 필드가 어느 핸들러에서도 set되지 않음
- **원본**: 크롤링 시 heightOffset = -(1.8 - 0.8) / 2 = -0.5 계산해서 set
- **수정 위치**: CrawlingHandler/SlidingHandler/CeilingClimbingHandler에서 heightOffset 계산 후 state에 저장

### B-2: 클라이밍 홀드 시 추락
- **현상**: 벽에 붙어 grab만 누르면 (위/아래 없이) 추락
- **원인**: TravelMixin에서 wantClimbUp/Down 모두 false일 때 motionY = 0.0D로 설정
- **원본**: 홀드 시 motionY = HOLD_MOTION (0.08D)으로 천천히 위로 유지
- **수정 위치**: `LivingEntityTravelMixin.java` - 홀드 케이스 추가

### B-3: 각도 점프 더블탭 감지 불완전
- **현상**: 좌/우/뒤로 각도점프가 트리거되지 않음
- **원인**: JumpHandler에 leftJumpCount 등 카운터는 있지만, InputHandler에서 startPressed 이벤트를 JumpHandler로 전달하지 않음
- **수정 위치**: JumpHandler.update() 또는 InputHandler에서 각도점프 더블탭 로직 연결

### B-4: 헤드점프 수평속도 재분배 없음
- **현상**: 헤드점프가 위로만 뜨고 달리던 방향 관성이 그대로 유지됨
- **원인**: JumpHandler.tryJump(HEAD_UP)에서 수평→수직 속도 재분배 로직 미구현
- **원본**: `hMag`, `totalMotion`, `normalAngle`, `newAngle` 계산으로 속도 재분배
- **수정 위치**: JumpHandler.java

### B-5: 수영 중 sneakButton.startPressed 로 물에서 나오는 점프 안 됨
- **현상**: isDipping 상태에서 점프 시 vel.y = 0.3D 적용 안 됨
- **원인**: SwimmingHandler.applySwimPhysics에서 jumpButton 체크 후 vel.y 설정하는 로직 확인 필요

---

## 핵심 누락 기능 🟠

### M-1: 벽 점프 (Wall Jump)
- **원본**: 공중에서 벽에 닿을 때 grab + 방향키로 벽 점프
- **우리**: isWallJumping 상태 있지만 물리/트리거 없음
- **구현 필요**: WallJumpHandler 또는 JumpHandler에 로직 추가

### M-2: 클라이밍 점프 / 클라이밍 뒤로 점프
- **원본**: 클라이밍 중 grab + jump → 위로 점프, grab + back → 뒤로 점프
- **우리**: 클라이밍 중 점프 전혀 없음
- **구현 필요**: JumpHandler에 CLIMB_UP, CLIMB_BACK 케이스

### M-3: 각도 점프 완전 구현
- **원본**: 좌/우/뒤 방향키 더블탭으로 해당 방향 점프 (3틱 윈도우)
- **우리**: 더블탭 카운터는 있지만 연결 안 됨
- **구현 필요**: InputHandler에서 startPressed 이벤트 → JumpHandler 더블탭 카운터 연결

### M-4: 탈진(Exhaustion) 적립 시스템
- **원본**: 클라이밍/천장클라이밍 시 매 틱 exhaustion 증가, 초과 시 동작 차단
- **우리**: Config에 있지만 climbExhaustionEnabled = false, 실제 적립 없음
- **구현 필요**: ClimbingHandler/CeilingClimbingHandler에서 exhaustion 증가/감소 로직

### M-5: 크롤 토글 모드
- **원본**: grab+sneak 한 번 누르면 크롤 토글 (계속 들지 않아도 됨)
- **우리**: sneak 키 계속 눌러야 유지
- **구현 필요**: CrawlingHandler에서 crawlToggled 플래그 처리

---

## 세부 누락 기능 🟡

### D-1: 로프 슬라이딩 애니메이션
- 덩굴에서 grab 없이 내려갈 때의 애니메이션

### D-2: 낙하 애니메이션 (isFalling)
- 높은 곳에서 낙하 중 자세

### D-3: 각도 점프 애니메이션 (isAngleJumping)
- 각도 점프 중 자세

### D-4: 클라이밍 점프 애니메이션
- 클라이밍에서 뒤로 점프 중 자세

### D-5: 크롤-클라이밍 애니메이션 (isCrawlClimbing)
- 좁은 공간에서 클라이밍 자세

### D-6: 스니크 중 이름표 숨기기
- 원본: isCrawling && !sneakNameTag config → 이름표 숨김
- Config._crawlNameTag 관련

### D-7: 인게임 설정 UI
- config_toggle 키에 연결된 화면 없음

### D-8: 속도 증가/감소 키
- speed_increase, speed_decrease 키 바인딩 있지만 로직 없음

---

## 정상 구현 🟢

| 기능 | 상태 |
|------|------|
| 기본 기어가기 (크롤링) | ✅ |
| 기본 벽 클라이밍 | ✅ |
| 천장 클라이밍 | ✅ |
| 기본 슬라이딩 | ✅ |
| 수영/잠수/물가 | ✅ |
| 점프 차지 (ChargeUp) | ✅ |
| 헤드점프 차지 | ✅ (재분배만 없음) |
| 크롤링 히트박스 축소 | ✅ |
| 네트워킹 상태 동기화 | ✅ |
| HUD 스프라이트 렌더링 | ✅ |
| JSON 설정 저장/로드 | ✅ |
| 입력 버튼 상태 추적 | ✅ |
| 7가지 애니메이션 | ✅ |
| 사운드 관련 | 제거됨 (의도적) |

---

## 수정 우선순위 로드맵

### 1단계: 버그 수정 (즉시)
1. **B-2** 클라이밍 홀드 추락 수정 (TravelMixin 한 줄)
2. **B-1** heightOffset 계산/설정 (렌더 보정)

### 2단계: 핵심 기능 (단기)
3. **M-3** 각도 점프 더블탭 연결
4. **B-3** 각도 점프 트리거 수정
5. **B-4** 헤드점프 수평→수직 재분배
6. **M-1** 벽 점프 구현
7. **M-2** 클라이밍 점프/클라이밍 뒤로 점프

### 3단계: 완성도 (중기)
8. **M-4** 탈진 시스템 활성화
9. **M-5** 크롤 토글 모드
10. **D-1~D-5** 누락 애니메이션들

### 4단계: 마무리 (장기)
11. **D-7** 인게임 설정 UI
12. **D-8** 속도 키 로직
