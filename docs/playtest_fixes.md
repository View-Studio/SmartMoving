# 플레이테스트 발견 이슈 — 1:1 번역 재작업

> 실기 테스트에서 원본(1.7.10)과 다른 동작을 재발견. 각 항목을 원자 단위로 분해하여
> 1:1 번역을 다시 돌리는 방식으로 수정한다. 이 문서 자체가 작업 진입점 역할.

---

## 🎯 현재 포커스

```
항목 #5  — 옵션토글 4단계 순환 (disabled / easy / medium / hard)
```

> **문서 진입 시 반드시 위 포커스 항목 먼저 진행**한다. 다른 항목은 현재 포커스가
> "처리 완료"로 마킹되기 전까지 손대지 않는다. 포커스 완료 시 아래 **포커스 순서**
> 다음 번호로 이 섹션의 번호를 갱신한다.

**포커스 순서 (작업 진행 순):**
1. **#5** 옵션토글 4단계 ← 현재
2. **#6** increase/decrease 실제 작동 안 됨
3. **#2** 스마트무빙 상태 이상 (구체 재현 케이스 수집 필요)
4. **#3** 상태 전환 조건 이상 (구체 재현 케이스 수집 필요)
5. **#4** 상태 전환 키 커맨드 조합 이상 (구체 재현 케이스 수집 필요)
6. **#1** 애니메이션 망가짐/이상 (구체 재현 케이스 수집 필요)

---

## 1:1 번역 룰 (PORTING_RULES.md + checklist_original_audit.md 발췌·통합)

```
원칙 1. 원자 단위 원칙
   항목 하나 = 원본 동작 하나. "X 기능 구현" 같은 복수 동작 금지.

원칙 2. 완전 추출 원칙
   리서치 단계에서 원본 클래스의 모든 메서드 × 모든 분기 × 모든 상수를
   빠짐없이 추출. 기록되지 않은 동작은 구현 대상이 아니다.

원칙 3. 추적 가능성 원칙
   코드에 TODO / [미확인] / 빈 메서드가 존재하면 반드시 대응 항목이
   이 문서 또는 PENDING_RESEARCH.md 에 등록되어 있어야 한다.

원칙 4. 미확인 차단 원칙 (감사 규칙 B)
   설계 근거(리서치 파일의 원본 코드)가 없으면 구현하지 않는다.
   "아마 이럴 것이다"로 코드를 작성하지 않는다.
   리서치 파일에 없는 동작은 → 리서치 파일을 먼저 보완한다.

원칙 5. 완료는 검증 이후 원칙
   항목을 [x]로 바꾸기 전에 완료 전 검증 체크리스트를 통과해야 한다.

감사 규칙 A. 전체 읽기 원칙
   리서치 파일은 처음부터 끝까지 전부 읽는다.
   대응 구현 파일도 처음부터 끝까지 전부 읽는다.

감사 규칙 C. 즉시 수정 원칙
   불일치를 발견한 순간 그 자리에서 수정한다.
   수정 후 컴파일 확인.

감사 규칙 D. 불일치 3분류
   [오역]   구현이 원본과 다른 값/조건/로직을 사용한다
   [누락]   원본의 분기·상수·조건이 구현에 존재하지 않는다
   [잉여]   원본에 없는 로직이 구현에 추가되어 있다

원자 단위 예시
   허용: "isClimbing=true일 때 매 틱 fallDistance=0 으로 리셋"
   금지: "클라이밍 구현" (복수 동작 포함)
   금지: "TODO Phase N 처리" (무엇인지 불명확)

완료 전 검증 체크리스트
   □ 의존 리서치 문서에서 관련 메서드/분기를 다시 읽었다
   □ 리서치 문서의 동작 설명과 구현 코드가 1:1로 대응된다
   □ 리서치 문서에 기록된 모든 분기(if/else)가 코드에 존재한다
   □ 리서치 문서에 기록된 모든 상수가 코드에 반영되었다
   □ 구현 중 발견한 신규 누락은 신규 항목으로 등록했다
   □ 메서드 호출 타이밍(훅 위치)이 원본과 일치한다
   □ 컴파일 성공
```

**우선순위**: 이 문서 > PORTING_RULES.md > CLAUDE.md > 개인 판단.

---

## 플레이테스트 발견 이슈 — 원본 (1.7.10) 과 달라진 부분

### #1 애니메이션 망가짐 / 이상함

- **증상**: 특정 상태의 애니메이션이 원본과 시각적으로 다름
- **관련 파일**: `MixinPlayerEntityModelClient.java` (`sm_animateClimbing` / `sm_animateCrawling` / `sm_animateSwimming` / `sm_animateDiving` / `sm_animateCeilingClimbing` / `sm_animateSliding` / `sm_animateHeadJumping` / `sm_animateFlying`)
- **의심 지점**:
  - `bipedOuter` 계층 부재로 body/limb 분리 근사 오역
  - 팔/다리 각도 공식 이식 시 sin/cos 분해 근사 사용(메모리 기록된 실패 패턴)
  - `feetDistSideFactor` / `handsDistSideFactor` 원본과 값 차이
- **재현 케이스 필요**: 어느 상태(수영/잠수/클라이밍/크롤링/슬라이딩/헤드점프/비행)에서 어떻게 이상한지 구체 보고
- **상태**: 대기 — 재현 케이스 수집 전까지 진입 금지

### #2 스마트무빙 상태 이상

- **증상**: `isCrawling`/`isDipping`/`isSwimming_sm`/`isDiving`/`isFast`/`isSlow` 등이 잘못된 시점에 true/false
- **관련 파일**: `SmartMovingClientState.tickEssential()`, `SmartMovingSwimmer.updateSwimState()`, `SmartMovingJumper.handleJumping()`
- **의심 지점**:
  - `canStandUp` 근사로 인한 크롤링 유지/해제 오판
  - `wasCrawling_st` / `wasClimbCrawling` / `wasSneaking` 저장 타이밍
  - `isSmall` 계산 위치가 다른 상태 변경보다 앞/뒤
- **재현 케이스 필요**: 어느 상태가 어느 상황에서 틀린지
- **상태**: 대기 — 재현 케이스 수집 전까지 진입 금지

### #3 상태 전환 조건 이상

- **증상**: 특정 조합에서 상태가 원본과 다른 타이밍에 전환됨
- **관련 파일**: `SmartMovingClientState.tickEssential()` (IMPL-01 크롤링 / IMPL-02 슬라이딩), `SmartMovingSwimmer` 수영↔크롤링, `SmartMovingJumper.updateWallJumpState()`, `SmartMovingClimber` 클라이밍
- **의심 지점**:
  - `wantCrawl` pre-compute 블록이 IMPL-01 실제 전환 전에 실행되어 `isCrawling` 재계산 순서 문제
  - `wantSlide` 조건 (`player.isSneaking()`) 이 `isSlow` 의존 → 순환
  - `wantWallJumping` 자기참조 식의 이전 틱 값 사용
- **재현 케이스 필요**
- **상태**: 대기

### #4 상태 전환 키 커맨드 조합 이상 (예: ctrl+grab+spacebar)

- **증상**: 특정 키 조합이 원본에서와 다른 동작 발동 (또는 발동 안 됨)
- **관련 파일**: `SmartMovingClientState.tickEssential()` 키 엣지 감지, `SmartMovingJumper.handleJumping()` 차지/헤드 점프 분기, `IMPL-03` 더블클릭 방향 점프
- **의심 지점**:
  - `jumpKeyStartPressed` 엣지 감지 vs `jumpPending` (vanilla 가로채기) 이중 시스템 우선순위 충돌
  - `sneakKeyStartPressed` / `sneakKeyStopPressed` 감지가 `jumpPending` 처리 후 클리어되는 문제
  - `isJumpCharging` / `isHeadJumpCharging` 게이트 조건 (원본 `isGroundSprinting` / `isRunning` 와 비교)
  - 차지 점프 해제(sneak 릴리즈 시 tryJump) 조건 원본 대조
- **재현 케이스 필요**: 어느 키 조합이 무슨 동작을 발동시켜야 하는데 뭐가 대신 일어나는지
- **상태**: 대기

### #5 옵션토글 4단계 순환 — **현재 포커스** ⚠️

- **증상**: 원본은 `/smoving config toggle` 또는 키바인딩으로 **disabled → easy → medium → hard → disabled** 4상태 순환. 현재 구현은 **on/off 2상태** 토글만.
- **이전 판단 오류 (내 오판)**: 이전 세션에서 "단일 config key 시스템(1.21.1 '_default' key 단순 on/off 토글)이 원본 `toggler==0 ↔ -1` 순환과 기능 동등"이라고 판단했지만 **틀렸음**. 원본 `configKeys = {"e", "m", "h"}` 길이 3 배열 + `toggler` 순환으로 4상태 구현.
- **원본 근거**:
  - `SmartMovingConfig.md` L610-L620:
    ```java
    _survivalConfigKeys       = Strings(...).defaults({"e", "m", "h"});
    _survivalDefaultConfigKey = String(...).defaults("m");
    _creativeConfigKeys       = Strings(...).defaults({"c"});
    _adventureConfigKeys      = Strings(...).defaults({"e", "m", "h"});
    _configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"));
    ```
  - `SmartMovingProperties.md` L325-L332 `toggle()`:
    ```java
    toggler++;
    if (toggler == length) toggler = -1;
    update();
    ```
    → `keys.length==3` 일 때 `0 → 1 → 2 → -1 → 0 → ...` (Easy → Medium → Hard → disabled → Easy)
  - `SmartMovingOptions.md` `isSneakToggleEnabled()` / `isCrawlToggleEnabled()` 같이 각 기능 토글은 `enabled` 플래그 기반.
- **관련 파일**:
  - `SmartMovingConfig.java` — `configKeys[]` / `currentConfigKey` / `toggler` 시스템 이식
  - `SmartMovingConfig.toggle()` — toggler 순환 1:1
  - `SmartMovingServer.adminToggleConfig(player)` — 4상태 로그 반영
  - `SmartMovingServer.logConfigState()` — 현재 "currentKey==null 경로만 이식"된 상태 → 키 이름 경로(Easy/Medium/Hard) 추가
  - 클라이언트 키바인딩 `configToggle` — toggler 순환 반영
- **작업 계획**:
  1. 리서치 파일에서 `setKeys` / `setCurrentKey` / `getCurrentKey` / `getNextKey` / `getCurrentKeyValue` 원본 코드 전체 덤프 (Agent WebFetch 활용)
  2. `SmartMovingConfig` 에 필드 추가: `configKeys[]` (default {"e","m","h"}), `toggler` (int, default 0 또는 -1), `currentConfigKey` (String, default "m")
  3. `SmartMovingConfig.toggle()` 재구현: toggler 순환 + enabled 재계산
  4. `SmartMovingConfig.getCurrentKey()`: `toggler==-1 ? null : configKeys[toggler]`
  5. `logConfigState` 의 키 이름 경로 활성화 (currentKey != null 분기)
  6. 클라이언트 configToggle 키바인딩 / `SmartMovingServer.adminToggleConfig` 모두 새 toggler 순환 반영
  7. **주의**: config key 값 차별화(e/m/h 별 다른 설정값)는 이식 안 함 — 현재 SmartMovingConfig 단일 값 구조 유지, toggler 자체만 순환(enabled on/off + 세 key 이름만). 원본 "4상태"는 이 구조로 재현 가능 (enabled = (toggler != -1), currentKey = toggler ≥ 0 시 key 이름).
- **상태**: 진행 중

### #6 increase/decrease 실제로 작동 안 함 (텍스트만 뜸)

- **증상**: `/smoving speed +N` 또는 speedIncrease 키 누르면 채팅 메시지는 뜨지만 실제 이동 속도는 변하지 않음.
- **관련 파일**:
  - `SmartMovingClient.java` — `SpeedChangePayload` 수신 핸들러 (L79-L104)
  - `SmartMovingServer.processSpeedChangePacket` — 서버측 응답 (현재 `changeSingleSpeed(username, diff)` 호출)
  - `SmartMovingConfig.getUserSpeedFactor()` — `(1 + speedUserFactor)^speedUserExponent`
  - `SmartMovingConfig` 값이 실제 이동 속도에 반영되는 경로 — `getUserSpeedFactor()` 호출처
- **의심 지점**:
  - 서버가 `changeSingleSpeed` 로 **서버측 playerSpeedExponents** 만 저장 → 클라이언트는 응답으로 `Config.changeSpeed(diff)` 호출하지만 `Config = SERVER_CONFIG` 상태에서 이 값 변경해봤자 다음 서버 재전송이 없어서 효과 없음
  - **더 중요**: `getUserSpeedFactor()` 가 실제 movement 로직 어디에서 **곱해지지 않음** (handleLand / handleSwimming / handleClimbing 등에서 적용 누락 가능성)
- **원본 근거 확인 필요**:
  - `SmartMovingSelf` 의 `landMotion` / `handleSwimming` 속도 계수 계산에서 `Config.getUserSpeedFactor()` 호출 위치
  - `Config.changeSpeed` 호출 후 클라이언트 반영 경로 (`isUserSpeedEnabled` / `_speedUser` 게이트 포함)
- **작업 계획** (포커스 진입 시):
  1. 원본 `SmartMovingSelf.landMotion` / `handleSwimming` 에서 `getUserSpeedFactor()` 호출 위치 확인
  2. 1.21.1 `SmartMovingClimber` / `SmartMovingSwimmer` / vanilla 이동 경로에서 해당 factor 가 곱해지는지 감사
  3. 누락이면 해당 위치에 `* cfg.getUserSpeedFactor()` 적용
  4. 클라이언트 `SpeedChangePayload` 수신 처리를 서버 브로드캐스트 경로와 정합 — `Config.changeSpeed(diff)` 가 `INSTANCE` 또는 `SERVER_CONFIG` 중 어느 인스턴스를 변경해야 하는지 원본 흐름 대조
- **상태**: 대기 (#5 완료 후 진입)

---

## 작업 템플릿 (각 포커스 항목 진입 시)

```
1. 이 문서의 "현재 포커스" 번호 확인
2. 해당 항목의 "원본 근거 확인 필요" 섹션 먼저 처리 — 리서치 파일 읽기,
   불충분하면 Agent WebFetch 로 원본 소스 직접 확보 후 리서치 파일 보완
3. 원자 단위로 세부 작업을 분해 (코드 수정 단위)
4. 수정 → 컴파일 확인 → 원본 대조 재검증
5. 체크리스트 업데이트 + 커밋
6. 이 문서의 "포커스 순서" 다음 번호로 "현재 포커스" 갱신
```

---

## 변경 이력

- **2026-04-23**: 문서 생성. 플레이테스트 발견 이슈 6종 기록. 포커스 순서 #5 → #6 → #2 → #3 → #4 → #1 확정. 현재 포커스 #5.
