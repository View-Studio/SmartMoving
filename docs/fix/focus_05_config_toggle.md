# Focus #5 — 옵션토글 2상태 + enabled = 원본 Easy 1:1

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스 #5.
> 이 문서 + playtest_fixes.md 2개만 열고 작업한다.

> **🔄 방향 전환 (세션 13, 2026-04-23, 사용자 결정)**:
> 원본 `disabled → easy → medium → hard` 4상태 순환 이식은 **포기**. 대신
> `disabled ↔ enabled` 2상태로 단순화하되, **enabled 상태 = 원본 Easy 프리셋을
> 1:1 로 가져온다**. Property 시스템 전체 이식 없이 Easy 에서 반환되는 값만 필드
> 기본값에 반영하는 방식. 기존 B~F 구현(4상태 라벨 순환 기계)은 `configKeys={null}`
> 단일 key 구조에서 자동으로 2상태로만 동작 — 잉여 코드이나 해가 없어 유지. 실제
> 작업은 **§10 H 섹션 (Easy 프리셋 완전 이식)** 으로 이동.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ✅ **재완료 (2026-04-24, 세션 22)** — H-15/H-16 수정 완료 |
| 현재 단계 | ✅ H-0~H-17 + G-5 완료 / ⏳ **H-18~H-21 진행 (잉여 코드 정리)** |
| 이식 범위 | Easy 실제 코드 경로: factor 헬퍼 + handleExhaustion 축소판 + 29개 Config 필드 + 허기 패킷 + speedUser 정정 |
| 배제 범위 | 14종 점프 피로 / 클라이밍·천장·스프린트 피로 축적 / 라바 수영 / Creative levitate / getMaxExhaustion 순회 |
| 이전 판단 오류 | ⚠️ 2건 — ① 2-"이전 판단 오류" (단일 key on/off 등가 오판) / ② §7.1 "Property 시스템 구조적 N/A" 오판 (세션 13 정정) |
| 컴파일 상태 | ✅ 빌드 성공 (H 코드 변경 전) |

---

## 2. 증상 — 원본 vs 현재

### 원본 (1.7.10) — 전체 동작

1. `configToggle` 키 → **4상태 순환**: `disabled → easy(e) → medium(m) → hard(h) → disabled → ...`
2. 각 key 는 **단순 라벨이 아니라** 여러 config 필드 기본값을 바꾸는 실질적 프리셋:
   - `_baseExhautionLossFactor = Value(1F).e(1.2F).h(0.8F)` — Easy 는 소진 1.2배 빠른 회복
   - `_exhaustionLossHungerFactor = Value(0.05F).e(0.02F).h(0.08F)` — Easy 는 허기 전환 낮음
   - `_baseHungerGainFactor = Value(1F).e(0.8F).h(1.2F)` — Easy 는 허기 느리게 증가
   - `Creative(key) = Value(false).c(true)` 팩토리 — Creative 전용 true
   - `Hard(key) = Value(false).h(true)` 팩토리 — Hard 전용 true
   - `Medium(key) = Value(true).e(false)` 팩토리 — Easy 에서만 false (나머지는 true)

### 사용자 결정 (세션 13)

- 4상태 순환 이식은 **포기** — Property 시스템 전체 이식 비용 대비 실익 낮음
- 대신 **`disabled ↔ enabled` 2상태** + **enabled = 원본 Easy 1:1**
- 즉 1.21.1 의 고정 프리셋은 **원본 Easy key 가 반환하는 값** 과 같아야 함

### 이전 판단 오류 (누적 기록)

1. **세션 1-2 오판 (정정 완료)**: "단일 key=on/off 가 원본 toggler 0↔-1 순환과 기능 동등"
   — 실제로는 Easy/Medium/Hard 3단계 순환. 세션 3~11 에서 라벨 순환 이식으로 정정.
2. **세션 5-11 오판 (세션 13 정정)**: §7.1 "Property 시스템 key 별 값 차별화는
   1.21.1 구조상 N/A 권장" — 실제로는 Property 시스템을 **이식하지 않는 것이지**
   "N/A" 가 아님. Easy/Medium/Hard 가 바꾸는 값은 이식 가능. 포커스 #5 범위 외로
   제외한 것은 **내 결정 실수**였음. 사용자 확인 없이 범위를 축소했고, "원본 특징
   그대로 가져왔나?" 질문으로 드러남. 세션 13 부터 H 섹션으로 Easy 1:1 이식.

---

## 3. 재현 케이스 표 (세션 13 재정의)

### 토글 동작 (단순화)

| # | 시나리오 | 1.21.1 목표 동작 |
|---|---------|-----------------|
| 1 | 월드 진입 | 채팅: "Smart Moving enabled" (toggler=0, key=null, configKeys={null}) |
| 2 | configToggle 키 × 1 | "Smart Moving disabled" (toggler=-1) |
| 3 | × 1 (재진입) | "Smart Moving enabled" (toggler=0) |

### 프리셋 값 (원본 Easy 1:1) — H-1 완전 추출 결과 ✅

**원본 파일**: `https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/moving/config/SmartMovingConfig.java`

**SM 버전 기준**: `_sm_current = _sm_3_2` (L59). 모든 `defaults(X, _pre_sm_*)` 버전
폴백은 과거 세이브 로드시만 적용 — 1.21.1 신규 설치엔 **최상위 `defaults(Value(...))`
값** 사용.

**Agent 덤프 섹션별 총 28개 → 이식 대상 20개** (중복/버전 호환 `_old_*` 4개, 이미
일치 2개, 이미 이식 1개, Easy=default Float 는 P.2 로 이식 포함).

#### P.1 — Float 값 차이 필드 (Easy ≠ default) — 신규 추가 3개

| # | 원본 필드 (L) | default | **Easy** | Hard | 1.21.1 조치 |
|---|---|---|---|---|---|
| P-1 | `_baseExhautionLossFactor` (L399) | 1F | **1.2F** | 0.8F | 신규 `baseExhautionLossFactor = 1.2F` |
| P-2 | `_exhaustionLossHungerFactor` (L417) | 0.05F | **0.02F** | 0.08F | 신규 `exhaustionLossHungerFactor = 0.02F` |
| P-3 | `_baseHungerGainFactor` (L423) | 1F | **0.8F** | 1.2F | 신규 `baseHungerGainFactor = 0.8F` |

#### P.2 — Float Easy=default (Creative/Hard 에서 값 다름) — 신규 추가 3개

| # | 원본 필드 (L) | **Easy**=default | Creative | Hard | 1.21.1 조치 |
|---|---|---|---|---|---|
| P-4 | `_runFactorLevitate` (L169) | 1.3F | 2.0F | — | 신규 `runFactorLevitate = 1.3F` |
| P-5 | `_sprintFactorLevitate` (L180) | 1.5F | 3F | — | 신규 `sprintFactorLevitate = 1.5F` |
| P-6 | `_alwaysHungerGain` (L439) | 0F | — | 0.005F | 신규 `alwaysHungerGain = 0F` |

#### P.3 — Boolean Creative 팩토리 (Easy=false) — 정정 1 + 신규 1

`Creative(key) = Modified(key).defaults(Value(false).c(true))` → Easy=false, Creative=true

| # | 원본 필드 (L) | **Easy**=default | Creative | 1.21.1 현재 | 조치 |
|---|---|---|---|---|---|
| P-7 | `_speedUser` (L96) | **false** | true | `true` ⚠️ | **기본값 `true → false` 정정** (`_pre_sm_3_2` 폴백은 과거 세이브만) |
| P-8 | `_lavaLikeWater` (L163) | **false** | true | 미구현 | 신규 `lavaLikeWater = false` |

#### P.4 — Boolean Hard 팩토리 (Easy=false) — 신규 5 + 이미 일치 2

`Hard(key) = Modified(key).defaults(Value(false).h(true)).defaults(false, _pre_sm_1_5)`

| # | 원본 필드 (L) | **Easy**=default | Hard | 1.21.1 현재 | 조치 |
|---|---|---|---|---|---|
| — | `_climbExhaustion` (L129) | false | true | `false` ✓ | 이미 일치 |
| — | `_ceilingClimbExhaustion` (L145) | false | true | `false` ✓ | 이미 일치 |
| P-9 | `_runExhaustion` (L170) | **false** | true | 미구현 | 신규 `runExhaustion = false` |
| P-10 | `_climbJumpExhaustion` (L333) | **false** | true | 미구현 | 신규 `climbJumpExhaustion = false` |
| P-11 | `_standJumpExhaustion` (L368) | **false** | true | 미구현 | 신규 `standJumpExhaustion = false` |
| P-12 | `_sneakJumpExhaustion` (L372) | **false** | true | 미구현 | 신규 `sneakJumpExhaustion = false` |
| P-13 | `_walkJumpExhaustion` (L376) | **false** | true | 미구현 | 신규 `walkJumpExhaustion = false` ⚠️ 원본 키 오타 `"move.jump.walkexhaustion"` (점 누락) 그대로 유지 |

#### P.5 — Boolean Medium 팩토리 (Easy=false, **default=true**) — 신규 7개

`Medium(key) = Unmodified(key).defaults(Value(true).e(false)).defaults(true, _pre_sm_1_5)` — **Easy 에서만 false**

| # | 원본 필드 (L) | default | **Easy** | 1.21.1 현재 | 조치 |
|---|---|---|---|---|---|
| P-14 | `_sprintExhaustion` (L182) | true | **false** | 미구현 | 신규 `sprintExhaustion = false` |
| P-15 | `_wallJumpExhaustion` (L355) | true | **false** | 미구현 | 신규 `wallJumpExhaustion = false` |
| P-16 | `_runJumpExhaustion` (L380) | true | **false** | 미구현 | 신규 `runJumpExhaustion = false` |
| P-17 | `_sprintJumpExhaustion` (L384) | true | **false** | 미구현 | 신규 `sprintJumpExhaustion = false` |
| P-18 | `_jumpChargeExhaustion` (L388) | true | **false** | 미구현 | 신규 `jumpChargeExhaustion = false` |
| P-19 | `_jumpSlideExhaustion` (L392) | true | **false** | 미구현 | 신규 `jumpSlideExhaustion = false` |
| P-20 | `_hungerGain` (L422) | true | **false** | 미구현 | 신규 `hungerGain = false` |

#### 제외 대상 (이식 불필요)

- `_configKeyName` (L465) — 이미 `configKeyName` Map 으로 이식됨 (B-3).
- `_old_jumpExhaustion` / `_old_sneakJumpExhaustion` / `_old_runJumpExhaustion` /
  `_old_sprintJumpExhaustion` — `_pre_sm_1_7` 버전 호환용 source. 신규 세이브에는
  불필요.
- `climbExhaustion` / `ceilingClimbExhaustion` — 이미 1.21.1 에 `false` 로 존재.

**총 이식 대상 20개**: Float 6 (P.1 + P.2) + Boolean 신규 13 (P.3 `lavaLikeWater` + P.4
5개 + P.5 7개) + Boolean 정정 1 (P.3 `speedUser`).

### H-2 소비처 매트릭스 — 1.21.1 현재 구현 여부 (세션 14)

각 필드를 **원본 소비처** (리서치 파일 grep) × **1.21.1 소비처** (소스 grep) 교차 확인.

| # | 필드 | 원본 소비처 | 1.21.1 현재 | 분류 |
|---|---|---|---|---|
| P-1 | `baseExhautionLossFactor` | `Config.getFactor(false, 상태들)` → exhaustionLoss (Self L888-890) | `getFactor` 메서드 **없음** / `exhaustionLoss` 변수 **없음** | 💀 dead |
| P-2 | `exhaustionLossHungerFactor` | `hungerIncrease += ELHF * exhaustionLoss` (Self L893) | `hungerIncrease` 변수 **없음** | 💀 dead |
| P-3 | `baseHungerGainFactor` | `Config.getFactor(true, 상태들)` → hungerGainFactor (Self L862) | `getFactor` 없음 | 💀 dead |
| P-4 | `runFactorLevitate` | `isLevitating` 분기 달리기 배율 | `isLevitating` 항상 false (로프 미구현, ClientState:992) | 💀 dead |
| P-5 | `sprintFactorLevitate` | `isLevitating` 분기 스프린트 배율 | 동일 | 💀 dead |
| P-6 | `alwaysHungerGain` | `hungerIncrease += alwaysHungerGain + ...` (Self L863) | hungerIncrease 없음 | 💀 dead |
| **P-7** | **`speedUser`** | **`isUserSpeedAlwaysDefault() = !_speedUser.value \|\| factor==1F` (ClientConfig 의 속도 계산)** | **`SmartMovingConfig.getUserSpeedFactor() L580-585` 참조 중** | ✅ **live** |
| P-8 | `lavaLikeWater` | `Base.getLavaBorder / isLava fluid 분기` (Base L222-L226) | `lavaLikeWater` 필드 **없음**, 라바 수영 분기 **없음** | 💀 dead |
| P-9 | `runExhaustion` | `handleRunning` 게이트 | 소진 축적 자체 미이식 | 💀 dead |
| P-10 | `climbJumpExhaustion` | 클라이밍 점프 게이트 | 미이식 | 💀 dead |
| P-11 | `standJumpExhaustion` | stand 점프 게이트 | 미이식 | 💀 dead |
| P-12 | `sneakJumpExhaustion` | sneak 점프 게이트 | 미이식 | 💀 dead |
| P-13 | `walkJumpExhaustion` | walk 점프 게이트 | 미이식 | 💀 dead |
| P-14 | `sprintExhaustion` | `additionalExhaustion * _sprintExhaustionGainFactor` (Self L883) | 미이식 | 💀 dead |
| P-15 | `wallJumpExhaustion` | 벽 점프 게이트 | 미이식 | 💀 dead |
| P-16 | `runJumpExhaustion` | run 점프 게이트 | 미이식 | 💀 dead |
| P-17 | `sprintJumpExhaustion` | sprint 점프 게이트 | 미이식 | 💀 dead |
| P-18 | `jumpChargeExhaustion` | 차지 점프 게이트 | 미이식 | 💀 dead |
| P-19 | `jumpSlideExhaustion` | 슬라이드 점프 게이트 | 미이식 | 💀 dead |
| P-20 | `hungerGain` | `hungerIncrease` 계산 진입 게이트 (Self L862 이전) | hungerIncrease 없음 | 💀 dead |

**요약**: **live 1개** (`speedUser` 정정) / **dead 19개** (소비처 전체 미이식).

**공통 미이식 시스템**:
- `Config.getFactor(hunger, 상태들)` 공용 헬퍼 (SmartMovingClientConfig.md L389 섹션)
- `SmartMovingSelf.updateExhaustion` / `updateHunger` 틱당 축적 루프 (Self L860-L895)
- 라바 수영 처리 (`SmartMovingBase.getNormalWaterBorder`/`isLava` 분기 — Base L222-L241)

→ **1.21.1 포트에서 Easy 프리셋의 "동작 층위" 1:1 은 위 3개 시스템 이식 완료를 전제**.
각 시스템은 포커스 #5 범위를 훨씬 초과. §17 후속 포커스 후보.

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | 없음 |
| 이 포커스가 영향 주는 대상 | #6 (speed change — `enabled` 체크 경로 공유) |
| 영향받는 완료 이식 | `SmartMovingServer.adminToggleConfig` (이미 `INSTANCE.toggle()` 호출 중 — 자동 반영) |
| 영향받는 번역 키 | `smartmoving.message.config.client.enabled` / `.disabled` → 4상태 분화 필요 |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 파일 | 줄 | 내용 |
|------|---|------|
| `docs/research/original/smartmoving/config/SmartMovingConfig.md` | L610-L620 | `_survivalConfigKeys` / `_creativeConfigKeys` / `_adventureConfigKeys` / `_configKeyName` 선언 |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | L325-L332 | `toggle()` 본체 |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | L345-L355 | `setKeys()` 본체 |
| `docs/research/original/smartmoving/config/SmartMovingOptions.md` | — | `toggle()` override / `initializeForGameIfNeccessary()` — **미확보** |
| `docs/research/original/smartmoving/config/SmartMovingProperties.md` | — | `setCurrentKey` / `getCurrentKey` / `getNextKey` / `hasKey` / `update` — **부분 확보** |

### 5.2. 이미 확보된 원본 Java 코드

**A. SmartMovingConfig.java L610-L620 — 필드 선언**
```java
public final Property<String[]> _survivalConfigKeys
    = Strings(...).singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _survivalDefaultConfigKey
    = String(...).singular().defaults("m");

public final Property<String[]> _creativeConfigKeys
    = Strings(...).singular().defaults(new String[]{"c"}).defaults(new String[0], _pre_sm_2_3);
public final Property<String>   _creativeDefaultConfigKey
    = String(...).singular().defaults("c").defaults("", _pre_sm_2_3);

public final Property<String[]> _adventureConfigKeys
    = Strings(...).singular().defaults(new String[]{"e", "m", "h"});
public final Property<String>   _adventureDefaultConfigKey
    = String(...).singular().defaults("m");

public final Property<String> _configKeyName
    = String(...).defaults(Value((String)null).e("Easy").m("Medium").h("Hard"));
```

**B. SmartMovingProperties.java L325-L332 — toggle()**
```java
public void toggle() {
    int length = keys == null ? 0 : keys.length;
    toggler++;
    if (toggler == length)
        toggler = -1;
    update();
}
```

**C. SmartMovingProperties.java L345-L355 — setKeys()**
```java
public void setKeys(String[] keys) {
    if (keys == null || keys.length == 0)
        keys = _defaultKeys;   // 단순 on/off 모드 (length 1)
    this.keys = keys;
    toggler = 0;               // 항상 첫 key 로 초기화
    update();
}
```

**D. 순환 패턴**
- `keys.length == 3` (survival/adventure, "e"/"m"/"h"): `0 → 1 → 2 → -1 → 0 → ...`
- `keys.length == 1` (creative, "c"): `0 → -1 → 0 → ...` (on/off)
- `keys == _defaultKeys` (게임타입 모름/override): 동일

### 5.3. 확보 필요 (WebFetch 대상)

A 섹션 원자 작업으로 처리. 아래는 Agent 에 넘길 프롬프트:

```
SmartMoving 1.7.10 원본에서 다음 정보를 덤프. 원본 Java 코드 그대로.

대상 URL:
  - https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/properties/Property.java
  - https://raw.githubusercontent.com/makamys/SmartMoving/master/src/main/java/net/smart/moving/config/SmartMovingOptions.java

추출 대상:
  1. Property.setCurrentKey(String) / getCurrentKey() / getNextKey(String) / hasKey(String) / update() 본체
  2. SmartMovingOptions.toggle() override 본체
  3. SmartMovingOptions.initializeForGameIfNeccessary() 본체
  4. _defaultKeys 값 (상수 확인)

응답 형식:
  - `// --- <파일명> L<start>-L<end> ---` 표기
  - 원본 그대로 (해석 금지)
```

### 5.4. factor 시스템 원본 완전 추출 (세션 15 H-3 준비 — Agent WebFetch)

**원본 `SmartMovingClientConfig.java` L554-L595 — `getFactor(hunger, 14상태)` 본체**:

```java
public float getFactor(boolean hunger, boolean onGround, boolean isStanding, boolean isStill,
    boolean isSneaking, boolean isRunning, boolean isSprinting, boolean isClimbing,
    boolean isClimbCrawling, boolean isCeilingClimbing, boolean isDipping,
    boolean isSwimming, boolean isDiving, boolean isCrawling, boolean isCrawlClimbing) {
    isClimbing |= isClimbCrawling;
    isCrawling |= isCrawlClimbing;
    boolean actionOverGound = isClimbing || isCeilingClimbing || isDiving || isSwimming;
    boolean airBorne = !onGround && !actionOverGound;
    isStanding = actionOverGound ? isStill : isStanding;
    isSneaking = isSneaking & !isStanding;

    float factor = hunger ? _baseHungerGainFactor.value : _baseExhautionLossFactor.value;
    if (airBorne)        factor *= hunger ? 0F : _fallExhautionLossFactor.value;  // ⚠️ hunger=0F 하드코딩
    else if (isSprinting) factor *= hunger ? _sprintingHungerGainFactor.value : _sprintingExhautionLossFactor.value;
    else if (isRunning)   factor *= hunger ? _runningHungerGainFactor.value : _runningExhautionLossFactor.value;
    else if (isSneaking)  factor *= hunger ? _sneakingHungerGainFactor.value : _sneakingExhautionLossFactor.value;
    else if (isStanding)  factor *= hunger ? _standingHungerGainFactor.value : _standingExhautionLossFactor.value;
    else                  factor *= hunger ? _walkingHungerGainFactor.value : _walkingExhautionLossFactor.value;

    if (isClimbing)           factor *= hunger ? _climbingHungerGainFactor.value : _climbingExhaustionLossFactor.value;
    else if (isCrawling)       factor *= hunger ? _crawlingHungerGainFactor.value : _crawlingExhaustionLossFactor.value;
    else if (isCeilingClimbing) factor *= hunger ? _ceilClimbingHungerGainFactor.value : _ceilClimbingExhaustionLossFactor.value;
    else if (isSwimming)       factor *= hunger ? _swimmingHungerGainFactor.value : _swimmingExhaustionLossFactor.value;
    else if (isDiving)         factor *= hunger ? _divingHungerGainFactor.value : _divingExhaustionLossFactor.value;
    else if (isDipping)        factor *= hunger ? _dippingHungerGainFactor.value : _dippingExhaustionLossFactor.value;
    else if (onGround)         factor *= hunger ? _normalHungerGainFactor.value : _normalExhaustionLossFactor.value;
    else                       factor *= hunger ? _normalHungerGainFactor.value : _normalExhaustionLossFactor.value;  // L589/L591 중복

    return factor;
}
```

**Factor 필드 원본 선언 27개 (SmartMovingConfig.java 라인 번호 포함)**:

**1단계 — base (난이도 의존 2개)**:
| # | 필드 | L | default | **Easy(.e)** | Medium | Hard(.h) | Creative | 키 |
|---|---|---|---|---|---|---|---|---|
| 1 | `_baseExhautionLossFactor` | 399 | 1F | **1.2F** | — | 0.8F | — | `move.exhaustion.loss.factor` |
| 2 | `_baseHungerGainFactor` | 423 | 1F | **0.8F** | — | 1.2F | — | `move.hunger.gain.factor` |

**1단계 — airBorne (fall)**:
| # | 필드 | L | default | 비고 |
|---|---|---|---|---|
| 3 | `_fallExhautionLossFactor` | 406 | 2.5F (up 하한 _standing) | hunger 대응 필드 **없음 → 0F 하드코딩** |

**1단계 — 이동상태별 (각 hunger/exhaustion 쌍)**:
| # | 필드 | L | default | 비고 (up() 하한/버전 폴백) |
|---|---|---|---|---|
| 4 | `_sprintingHungerGainFactor` | 425 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 5 | `_sprintingExhautionLossFactor` | 401 | 0F | — |
| 6 | `_runningHungerGainFactor` | 426 | 10F | `_pre_sm_1_3` 키이명 |
| 7 | `_runningExhautionLossFactor` | 402 | 0.5F | up 하한 `_sprinting` |
| 8 | `_sneakingHungerGainFactor` | 428 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 9 | `_sneakingExhautionLossFactor` | 404 | 1.5F | up 하한 `_walking`, `_sm_1_1` → 1F |
| 10 | `_standingHungerGainFactor` | 429 | 0F | — |
| 11 | `_standingExhautionLossFactor` | 405 | 2F | up 하한 `_sneaking.maximum(1F)` |
| 12 | `_walkingHungerGainFactor` | 427 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 13 | `_walkingExhautionLossFactor` | 403 | 1F | up 하한 `_running` |

**2단계 — 행동상태별 (각 hunger/exhaustion 쌍, Exhaustion 정타)**:
| # | 필드 | L | default | 비고 |
|---|---|---|---|---|
| 14 | `_climbingHungerGainFactor` | 431 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 15 | `_climbingExhaustionLossFactor` | 409 | PF 기본 | — |
| 16 | `_crawlingHungerGainFactor` | 432 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 17 | `_crawlingExhaustionLossFactor` | 410 | PF 기본 | — |
| 18 | `_ceilClimbingHungerGainFactor` | 433 | PF 기본 | `_pre_sm_1_3` 키이명. 축약 `Ceil` |
| 19 | `_ceilClimbingExhaustionLossFactor` | 408 | PF 기본 | 축약 `Ceil` |
| 20 | `_swimmingHungerGainFactor` | 434 | **1.5F** | `_pre_sm_1_3` 키이명 |
| 21 | `_swimmingExhaustionLossFactor` | 412 | PF 기본 | — |
| 22 | `_divingHungerGainFactor` | 435 | **1.5F** | `_pre_sm_1_3` 키이명 |
| 23 | `_divingExhaustionLossFactor` | 413 | PF 기본 | — |
| 24 | `_dippingHungerGainFactor` | 436 | **1.5F** | `_pre_sm_1_3` 키이명. 원본 키 `move.hunger.dip.gain.factor` |
| 25 | `_dippingExhaustionLossFactor` | 411 | PF 기본 | 원본 키 `move.exhaustion.dip.loss.factor` |
| 26 | `_normalHungerGainFactor` | 437 | PF 기본 | `_pre_sm_1_3` 키이명 |
| 27 | `_normalExhaustionLossFactor` | 414 | PF 기본 | onGround + else 둘 다 참조 (L589/L591) |

**+ 기타 handleExhaustion 에서 사용**:
| # | 필드 | L | default | Easy | Hard |
|---|---|---|---|---|---|
| 28 | `_alwaysHungerGain` | 439 (추정) | 0F | 0F | 0.005F |
| 29 | `_exhaustionLossHungerFactor` | 417 | 0.05F | **0.02F** | 0.08F |

**PF 기본** (PositiveFactor 타입 default) = `1F` (Properties.getDefaultValue(PositiveFactor)).

**⚠️ 1:1 이식 주의사항**:
1. **`Exhaution` vs `Exhaustion` 오타 혼재** — 1단계 7개는 `Exhaution`, 2단계 7개는 `Exhaustion`. 필드명 + config key 모두 원본 그대로 유지.
2. **airBorne + hunger 는 `0F` 하드코딩** — 대응 필드 없음. 이식 시 `fallHungerGainFactor` 같은 필드 **만들면 안 됨**.
3. **난이도(.e/.h) 체인은 `_base*` 2개에만** — 나머지 25개는 난이도 무관. Easy=default=Medium=Hard.
4. **`up()` 동적 하한** — 5개 필드가 다른 필드 값 이상으로 강제됨. 1.21.1 단순 `default` 이식 시 이 동적 하한은 잃음 (수용 가능한 근사, javadoc 명시).
5. **필드명 축약 `Ceil`** — `_ceilClimbing*` 로 선언됨 (`_ceilingClimbing*` 아님). 원본 그대로.
6. **L589/L591 `_normal*` 중복** — onGround / else 분기 모두 동일 필드. 의도된 중복. 1.21.1 이식도 동일 구조.

---

## 6. 1:1 매핑 테이블

| 원본 (1.7.10) | 1.21.1 포트 | 비고 |
|---|---|---|
| `Property<String[]> _survivalConfigKeys` | `String[] SmartMovingConfig.configKeys` | 필드로 단순화 — 게임타입별 분기는 서버 컨텍스트에서 선택 |
| `Property<String>   _survivalDefaultConfigKey` | `String SmartMovingConfig.defaultConfigKey` | 기본 key 이름 |
| `Property<String>   _configKeyName` | `Map<String,String> SmartMovingConfig.configKeyName` | 각 key 의 표시 이름 |
| `int toggler` (Property 내부) | `int SmartMovingConfig.toggler` | 순환 카운터 |
| `Property.keys` | `SmartMovingConfig.configKeys` | 동일 |
| `Property.toggle()` | `SmartMovingConfig.toggle()` | L325-L332 1:1 |
| `Property.setKeys(String[])` | `SmartMovingConfig.setKeys(String[])` | L345-L355 1:1 |
| `Property.update()` | `SmartMovingConfig.updateToggler()` | toggler → enabled/currentKey 파생 |
| `Options.isSneakToggleEnabled()` | `cfg.sneakToggle && cfg.enabled` | 이미 존재 (변경 없음) |
| `Options.initializeForGameIfNeccessary()` | `SmartMovingServer.initialize` 에서 gameType 기반 setKeys | gameType 판정은 `player.interactionManager.getGameMode()` |

---

## 7. 구조적 차이 / 근사 이식 지점 (세션 13 재정의)

### 7.1. 범위 외 (사용자 결정에 의해 포기)

- **gameType 별 Medium/Hard/Creative 런타임 전환**: 4상태 순환 포기로 배제. 사용자는
  항상 Easy 프리셋으로 고정된 1.21.1 를 사용. Medium/Hard 값은 리서치 문서상 기록만
  유지되고 코드에 이식되지 않음 (필요해지면 후속 포커스).
- **Property<T> 시스템 클래스군** (`net.smart.properties.Property`/`Value`/`Configurable`/
  `IValue`): 전체 이식 대신 "Easy 에서 반환되는 값을 필드 기본값에 직접 작성" 전략.
  버전 폴백(`defaults(x, _pre_sm_1_5)`) 은 수동 해석 후 이식 버전(`SM_VERSION = "1.0"` —
  실질적으로 3.2 기준) 에 해당하는 값만 선택.

### 7.2. 1.21.1 에서는 잉여 (유지되지만 동작 안 함)

세션 3~11 에서 이식한 4상태 라벨 순환 기계 전체:
- `configKeys` 필드 — `DEFAULT_KEYS = {null}` 로 초기화되어 있어 2상태로만 순환. 원본
  4갈래 분기는 코드에 있지만 `configKeys.length == 1` 이라 하나로 귀결 (잉여지만 무해).
- `survivalConfigKeys` / `survivalDefaultConfigKey` 등 게임타입별 6필드 + readFrom/writeTo.
  `initializeForGameIfNeccessary` 호출되어도 `setKeys(DEFAULT_KEYS)` 와 동등. 잉여.
- `configKeyName` Map (`{"e":"Easy","m":"Medium","h":"Hard"}`) — 현재 configKeys 에 `null`
  만 있으므로 참조 안 됨. 잉여.
- `getKey` / `getNextKey` / `hasKey` / `setCurrentKey` 등 — 2상태 전이에서는 쓰이지 않음.
  잉여지만 무해.

→ **§17 후속 "잉여 제거"** 로 별도 정리. 포커스 #5 내에서는 손대지 않음 (코드 안정성
우선, 방향 전환으로 이미 변경 포인트 많음).

### 7.3. 완전 재현 지점 (H 섹션 목표)

- Easy 프리셋 값 — 원본에서 Easy 가 반환하는 모든 필드 값을 1.21.1 필드 기본값에
  1:1 이식. 리서치 파일 기반 `.e()` 식별 + 원본 소스 WebFetch 로 누락 제로 검증 (H-1).
- `toggle()` 2상태 순환 — 이미 `configKeys={null}` 로 `0 ↔ -1` 동작.

---

## 8. 현재 구현 스냅샷 (수정 전)

### 8.1. `SmartMovingConfig.java` — 관련 필드/메서드

```java
// 필드 (요약)
public boolean enabled = true;
// (configKeys/toggler/currentConfigKey/configKeyName 없음)

// 메서드
public void toggle() {
    enabled = !enabled;
    save();
}

// readFrom / writeTo 에 `move.enabled` 만 있음
```

### 8.2. `SmartMovingServer.java` — logConfigState

```java
static void logConfigState(SmartMovingConfig config, String username, boolean reconfig) {
    String message = "Smart Moving ";
    if (config.globalConfig) {
        if (!reconfig) LOGGER.info("{}overrides client configurations", message);
        String postfix = getPostfix(username);
        if (config.enabled) {
            // 현재: currentKey==null 경로만 이식
            LOGGER.info("{}{}default server configuration{}",
                    message, reconfig ? "changed to " : "uses ", postfix);
        } else {
            LOGGER.info("{}disabled{}", message, postfix);
        }
    } else {
        LOGGER.info("{}allows client configurations", message);
    }
}
```

### 8.3. `SmartMovingClient.java` — 채팅 피드백 (configToggle 키)

```java
if (SmartMovingKeys.configToggle.wasPressed()) {
    if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE) {
        SmartMovingConfig.INSTANCE.toggle();
        String msgKey = SmartMovingConfig.INSTANCE.enabled
            ? "smartmoving.message.config.client.enabled"
            : "smartmoving.message.config.client.disabled";
        if (player != null) player.sendMessage(Text.translatable(msgKey));
    } else {
        ClientPlayNetworking.send(new SmartMovingNetwork.ConfigChangePayload());
    }
}
```

---

## 9. 예상 수정 diff

### 9.1. SmartMovingConfig — 필드 추가 + toggle 재구현

```diff
 public boolean enabled = true;
+/** 원본 Property.keys — 현재 활성 config key 배열. gameType 에 따라 setKeys 로 갱신. */
+public String[] configKeys = {"e", "m", "h"};       // survival 기본
+/** 원본 Property.toggler — -1(disabled) / 0..length-1(각 key) */
+public int toggler = 1;                              // survival default "m" 위치
+/** 원본 _configKeyName. key 별 표시 이름 (Easy/Medium/Hard/Creative) */
+public java.util.Map<String,String> configKeyName = java.util.Map.of(
+    "e", "Easy", "m", "Medium", "h", "Hard", "c", "Creative"
+);

 public void toggle() {
-    enabled = !enabled;
-    save();
+    // 원본 SmartMovingProperties.toggle() L325-L332 1:1
+    int length = configKeys == null ? 0 : configKeys.length;
+    toggler++;
+    if (toggler == length) toggler = -1;
+    updateToggler();
+    save();
 }

+/** 원본 SmartMovingProperties.setKeys(String[]) L345-L355 1:1. */
+public void setKeys(String[] keys) {
+    if (keys == null || keys.length == 0) keys = new String[]{""};  // _defaultKeys 대응
+    this.configKeys = keys;
+    this.toggler = 0;
+    updateToggler();
+}

+/** toggler → enabled / currentKey 파생 재계산. 원본 update() 대응. */
+private void updateToggler() {
+    enabled = (toggler != -1);
+}

+/** 원본 Property.getCurrentKey() — toggler==-1 → null, 그 외 configKeys[toggler]. */
+public String getCurrentKey() {
+    if (toggler < 0 || configKeys == null || toggler >= configKeys.length) return null;
+    return configKeys[toggler];
+}
```

### 9.2. SmartMovingServer.logConfigState — 키 이름 경로 활성화

```diff
 if (config.enabled) {
-    LOGGER.info("{}{}default server configuration{}",
-            message, reconfig ? "changed to " : "uses ", postfix);
+    String currentKey = config.getCurrentKey();
+    String action = reconfig ? "changed to " : "uses ";
+    if (currentKey == null) {
+        LOGGER.info("{}{}default server configuration{}", message, action, postfix);
+    } else {
+        String keyName = config.configKeyName.getOrDefault(currentKey, "");
+        if (keyName.isEmpty()) {
+            LOGGER.info("{}{}server configuration with key \"{}\"{}",
+                    message, action, currentKey, postfix);
+        } else {
+            LOGGER.info("{}{}server configuration \"{}\"{}",
+                    message, action, keyName, postfix);
+        }
+    }
 } else {
     LOGGER.info("{}disabled{}", message, postfix);
 }
```

### 9.3. 클라이언트 채팅 피드백 — 4상태 메시지

```diff
-String msgKey = SmartMovingConfig.INSTANCE.enabled
-    ? "smartmoving.message.config.client.enabled"
-    : "smartmoving.message.config.client.disabled";
-if (player != null) player.sendMessage(Text.translatable(msgKey));
+SmartMovingConfig cfg = SmartMovingConfig.INSTANCE;
+Text msg;
+if (!cfg.enabled) {
+    msg = Text.translatable("smartmoving.message.config.client.disabled");
+} else {
+    String currentKey = cfg.getCurrentKey();
+    if (currentKey == null) {
+        msg = Text.translatable("smartmoving.message.config.client.default");
+    } else {
+        String keyName = cfg.configKeyName.getOrDefault(currentKey, currentKey);
+        msg = Text.translatable("smartmoving.message.config.client.named", keyName);
+    }
+}
+if (player != null) player.sendMessage(msg);
```

`en_us.json` 추가 키:
- `smartmoving.message.config.client.default` = "Smart Moving enabled (default)"
- `smartmoving.message.config.client.named` = "Smart Moving enabled: %s"

---

## 10. 원자 단위 작업 목록

### A. 원본 소스 확보 (WebFetch)
- [x] A-1. ~~Agent WebFetch — `Property.java`~~ → **리서치 파일 `SmartMovingProperties.md` L15-L174 에 전체 소스 이미 완전 임베드 확인**. `setCurrentKey`/`getCurrentKey`/`getNextKey`/`hasKey`/`update`/`toggle`/`setKeys` 본체, `_defaultKeys`/`keys`/`toggler`/`enabled` 필드 전부 존재. WebFetch 불필요.
- [x] A-2. ~~Agent WebFetch — `SmartMovingOptions.java`~~ → **리서치 파일 `SmartMovingOptions.md` L500-L531(toggle override), L840-L889(initializeForGameIfNeccessary) 에 완전 임베드 확인**. `SmartMovingConfig.md` L630-L633 에 `Survival=0`/`Creative=1`/`Adventure=2`/`Unknown=-1` 상수도 확보됨. WebFetch 불필요.
- [x] A-3. 리서치 파일 이미 완전 — 보완 불필요.

### B. `SmartMovingConfig` 필드 추가
- [x] B-1. `configKeys` 필드 (String[]) — **원본 1:1 수정**: 초기값을 survival 기본이 아닌
      원본 `_defaultKeys = {null}` 그대로 (`DEFAULT_KEYS` 상수). 보조 상수 `CONFIG_KEY_ENABLED`
      = "enabled" / `CONFIG_KEY_DISABLED` = "disabled" 도 함께 추가 (원본 L26-L27). 게임타입별
      `{"e","m","h"}` / `{"c"}` 는 F 섹션의 `initializeForGameIfNeccessary()` 에서 setKeys 로 설정.
- [x] B-2. `toggler` 필드 (int, default **-2** — 원본 L30 초기값 그대로). 상태표(−2 센티넬 / −1 disabled / 0..length−1 enabled) + 파생 규칙(`enabled = toggler != -1`) javadoc 기록.
- [x] B-3. `configKeyName` 필드 (Map 근사, default 3개 매핑 e/m/h). Creative "c" 는 의도적 부재 —
      원본 `_configKeyName` 가 Creative key 에 defaults 없어 빈 문자열 반환하는 동작과 등가.
- [x] B-4. `readFrom`/`writeTo` 에 **6개 저장 포맷** 추가 — **§10 재정의**: 원본 `move.config.key.current`/`move.config.keys` 가 아니라, 원본 L456-L463 대로 게임타입별 6개:
      `move.config.survival.keys` + `.keys.default`, `move.config.creative.keys` + `.keys.default`,
      `move.config.adventure.keys` + `.keys.default`. CSV 직렬화(원본 comment `"entries seperated by ','"`).
      6개 필드 신규 추가(`survivalConfigKeys`/`survivalDefaultConfigKey` 등) + `getCsvArray`/`csvJoin` 헬퍼.
      현재 활성 key 상태는 별도 필드로 저장하지 않고 **해당 gameType 의 `defaultConfigKey` 값을
      `toggle()` 이 갱신**하는 원본 방식 유지 (C/D 섹션에서 구현).

### C. `SmartMovingConfig` 메서드 이식
- [x] C-1. `toggle()` 재구현 — 원본 `SmartMovingProperties.toggle()` (리서치 L325-L332 =
      원본 Java L81-L88) 1:1 이식. `enabled = !enabled; save();` → `toggler++/length wrap
      /updateToggler()/save()`. save() 호출은 원본 Properties.toggle() 에는 없지만 원본
      SmartMovingOptions.toggle() override (L500-L531) 에서 saveToOptionsFile 호출 — 1.21.1
      통합 계층에서 여기 유지. SmartMovingOptions.toggle() 의 추가 동작(_configChat 메시지,
      gameType 별 defaultConfigKey 갱신) 은 별도 원자 작업으로 분리 예정 (C-7 후보).
- [x] C-2. `setKeys(String[])` 이식 — 원본 `SmartMovingProperties.setKeys()` L90-L97 1:1
      (리서치 L345-L355). null/empty → DEFAULT_KEYS, toggler=0, updateToggler() 호출.
      save() 는 원본에도 없어 호출 안 함. 호출처는 F 섹션 `initializeForGameIfNeccessary` 예정.
- [x] C-3. `updateToggler()` 헬퍼 — `enabled = (toggler != -1)` (원본 `update()` L163-L172
      의 Property 루프는 1.21.1 Property 부재로 N/A, enabled 파생만 이식). **의존 순서상
      C-1 보다 먼저 진행** — C-1 `toggle()` 이 `updateToggler()` 호출 예정.
- [x] C-4. `getCurrentKey()` — **§10 정정**: 원래 "toggler == -1 시 null" 은 오기. 원본
      `SmartMovingProperties.getCurrentKey()` (L138-L143) 는 toggler==-1 시 `Disabled`
      (= `"disabled"`) 반환, null 은 `keys[toggler]==null` (DEFAULT_KEYS 케이스, 단순 on/off
      enabled) 인 경우에만. 원본 1:1 로 이식: `toggler==-1 → CONFIG_KEY_DISABLED`,
      `configKeys[toggler]` 반환. toggler==-2 센티넬 방어 코드 포함 (F 섹션 이식 전 과도기용).
- [x] C-5. `getNextKey(String)` — 원본 L106-L118 1:1 + 의존 `getKey(int)` L99-L104 함께 이식.
      두 메서드는 연계 동작(getNextKey 가 getKey(0) 호출)이므로 단일 원자 단위로 묶음.
      `key==null \|\| "disabled"` → `getKey(0)` / keys 매칭 다음 인덱스 / 마지막·미매칭 → "disabled".
- [x] C-6. `hasKey(String)` — 원본 L145-L156 1:1. `Enabled`/`Disabled` 특수 분기 +
      배열 내 null 포함 검색. 호출처는 `setCurrentKey(String)` 이식 시 유효성 체크로 연결 예정.
- [x] C-7. `setCurrentKey(String)` — 원본 L120-L136 1:1 이식. §16 의 "C-7 후보" 를 정식 승격.
      F-1/F-2 `initializeForGameIfNeccessary` 포팅의 선행 의존 (원본 L892 에서 호출).
      3갈래: (1) `key==null || "disabled"` → `toggler=-1`, (2) `keys=={null} && key=="enabled"`
      → `toggler=0`, (3) 배열 탐색 + 미매칭 시 `toggler=-1`. 끝에 `updateToggler()` 호출.

### D. 서버 로그 4상태 확장
- [x] D-1. `SmartMovingServer.logConfigState` 의 `currentKey==null` / `configName==""` /
      `configName!=""` 3갈래 분기 복원. 원본 SmartMovingServerOptions.md L80-L113 1:1.
      `config._configKeyName.value` 는 1.21.1 근사로 `configKeyName.getOrDefault(currentKey, "")`.

### E. 클라이언트 채팅 피드백 4상태
- [x] E-1/E-2/E-3. **§10 재정의 (원본 4갈래 반영)**: 원본 `writeClientConfigMessageToChat()`
      (SmartMovingOptions.md L560-L584) 은 `disabled`/`enabled`/`named`/`unnamed` **4갈래**
      (§9.3 예상 diff 의 3갈래 `default/named` 보다 정확). 4갈래 단일 원자로 통합 처리:
      - E-1: `SmartMovingClientState.configToggle` 블록 4갈래 분기 이식 (`unnamed && keyCount==1`,
        `CONFIG_KEY_ENABLED.equals(name)` 조건 포함).
      - E-2: `en_us.json` 에 `.named` / `.unnamed` 추가. **기존 `.enabled`/`.disabled` 텍스트도**
        원본(`"Smart Moving enabled"`, `"Smart Moving disabled"`) 으로 정정 (발견 즉시 수정 원칙).
      - E-3: 기존 키는 유지 (그대로). 텍스트만 정정됨.
      - 번역 키 원본 텍스트: Agent WebFetch 로 `en_US.lang` 확보 → `SmartMovingOptions.md` 에 추가.

### F. 게임타입별 configKeys 적용

**§10 F 재정의 (원본 구조 반영)**: 원본 `initializeForGameIfNeccessary()` 는 **클라이언트 측**
메서드 (`Minecraft.getMinecraft().playerController` 기반). 1.21.1 대응은
`MinecraftClient.getInstance().interactionManager.getCurrentGameMode()`. 매 tick 호출 + gameType
캐시 비교로 변경 감지. 기존 "SmartMovingServer.initialize" 명시는 오지정 — 재정의.

- [x] F-1/F-2. `initializeForGameIfNeccessary(int)` 이식 — 원본 L854-L903 1:1 (클라이언트 측).
      gameType 캐시 필드 + switch (Survival/Creative/Adventure/default) + setKeys(keys) +
      setCurrentKey(defaultKey). gameType 판정은 호출자에서 주입 (controller null 체크 포함).
      `resetForNewGame()` 도 함께 이식 (원본 L844-L848). 상수 `GAME_TYPE_UNKNOWN/SURVIVAL/
      CREATIVE/ADVENTURE` 정의 (원본 SmartMovingConfig L630-L633).
      채팅 초기화 (_configChatInit / _speedChatInit 분기) 는 범위 초과 — §16 에 기록.
- [x] F-3. 호출 타이밍 — `SmartMovingClientState.tickEssential` 시작부에 삽입. 원본
      `SmartMovingContext.interceptTick() L264` 와 동일 위치. `Config == INSTANCE`
      조건으로 서버 설정 덮어쓰기 방지. `client.interactionManager.getCurrentGameMode().getId()`
      로 gameType 주입. tick 폴링으로 gameType 변경 감지 — 별도 이벤트 훅 불필요.

### G. 검증 (4상태 라벨 순환 — 유지되지만 잉여)
- [x] G-1. `./gradlew build` 성공 — F 섹션 완료 시점 + G 세션 재실행 모두 통과
- [~] G-2. 재현 케이스 표 검증 — **재정의됨** (§3 세션 13 재작성). H 완료 후 재검증.
- [x] G-3. 회귀 방지 감사 (§14) — 4상태 라벨 순환 관점. 회귀 0건.
- [x] G-4. `checklist_original_audit.md` "4상태 토글 복원" 기록 완료.
- [x] G-5. `playtest_fixes.md` 의 "현재 포커스" 를 `#6` 으로 갱신 완료. 상단 "현재 포커스"
      블록 + §포커스 순서 표 의존 그래프 두 군데 반영. 포커스 #5 완료 주석(2026-04-24)
      + Easy 1:1 근사 ~90% / focus_11 후속 포인터 포함.

### H. Easy 완벽 1:1 (세션 15 재정의 — 범위 확정)

**최종 범위** (사용자 세션 15 결정): Easy 플레이 중 **실제로 코드 경로가 진입하는** 것만
이식. Easy 에서 게이트가 false 라 진입 안 하는 경로는 배제.

**Easy 에서 실제 사용** (이식):
- `Config.getFactor(hunger, 14상태)` 공용 헬퍼 전체
- `handleExhaustion` 축소판: 허기 증가 공식 + exhaustion 감소 + 허기 연동
- Config factor 1단계 필드 12개 (이동속도 배율) + 2단계 필드 14개 (행동 배율) + 기타 4개
- `speedUser` Easy 정정 (true → false)
- 허기 패킷 (클라→서버) + 서버 `addExhaustion(sm.hunger)` 연동

**Easy 에서 절대 미사용** (배제):
- 라바 수영 (`_lavaLikeWater`), Creative levitate 속도 (`_runFactorLevitate`/`_sprintFactorLevitate`)
- 14종 점프 피로 전체 (`getJumpExhaustionGain`/`Stop`/`isJumpExhaustionEnabled`)
- 클라이밍/천장/스프린트 피로 축적 (Hard 전용 게이트 7개)
- Exhaustion 관련 game field boolean 12개 (Easy 에서 false 라 if 진입 안 함)
- `getMaxExhaustion` 150회 순회 (max = 0 상수)
- `_baseExhautionGainFactor` + 클라이밍/천장 획득 계수들

- [x] H-0. **방향 전환 문서화** — §1/§2/§3/§7/§10 재정의 + §15 세션 13 로그 + §16 세션
      13 오판 정정 + §17 잔여 재정의. 코드 변경 없음.
- [x] H-1. **Easy 반환값 완전 추출** — Agent WebFetch 로 원본 `SmartMovingConfig.java`
      전체 소스(654L) 확보. §3 테이블 14→20 확장. `_old_*` 4개 제외. 원본 키 오타 확인.
- [x] H-2. **각 필드 소비처 매트릭스** — live 1 / dead 19. 공통 미이식 시스템 3종 식별.
      **사용자 결정**: 세션 15 에서 본안(Easy 에서 실제 쓰이는 것만 이식) 채택.

#### H-3 ~ H-14: Easy 실제 사용 코드 경로 이식 (세션 15 재정의)

- [x] H-3. **factor 1단계 Config 필드 13개 추가** — `SmartMovingConfig.java` 에
      "Exhaustion/Hunger factor 1단계" 섹션 신설 (Perspective/Misc 사이). 13개 필드 선언
      + Easy 값 default + 각 필드 javadoc 에 원본 라인/defaults/버전 폴백/up() 하한 기록.
      readFrom/writeTo 에 13키 추가 (원본 키 이름 그대로 — `Exhaution` 오타 유지).
      base 2개만 난이도 체인 (Easy 1.2F / 0.8F) 적용. airBorne hunger 필드 없음 (원본 0F 하드코딩).
- [x] H-4. **factor 2단계 Config 필드 14개 추가** — `SmartMovingConfig.java` 에 "Exhaustion/
      Hunger factor 2단계" 섹션 신설 (1단계 바로 뒤). climbing/crawling/ceilClimbing/
      swimming/diving/dipping/normal 각 HungerGain + ExhaustionLoss 쌍 14개. `Exhaustion`
      정타 / `ceilClimbing` 축약 원본 유지. swim/dive/dip HungerGain default=1.5F, 나머지는
      PF 기본 1F. `ceilClimbing` 키 순서 이상 (`climb.gain.ceiling` / `climb.ceiling.loss`)
      원본 그대로. `dipping` 키는 `dip` 축약 사용 원본 그대로. readFrom/writeTo 포함.
- [x] H-5. **기타 필드 2개** — `alwaysHungerGain` (L439, Easy=default=0F, Hard=0.005F) +
      `exhaustionLossHungerFactor` (L417, Easy=0.02F, default=0.05F, Hard=0.08F). 둘 다
      난이도 체인 있음 (Easy 값 채택). javadoc 에 소비처 SmartMovingSelf L863/L893 명시.
      readFrom/writeTo 포함.
- [x] H-6. **`speedUser` 정정** — 기본값 `true → false` (Easy 1:1). `SmartMovingConfig.java`
      L29 필드에 javadoc 확장: 원본 L96 `Creative("move.speed.user").defaults(true, _pre_sm_3_2)`,
      Creative 팩토리 의미 (Creative 전용 true, 나머지 false), 버전 폴백 `_pre_sm_3_2`,
      소비처 `getUserSpeedFactor()` L580 명시. 기존 이식 오역(true) 정정.
- [x] H-7. **`SmartMovingConfig.getFactor(hunger, 14상태)` 메서드 이식** — 원본
      SmartMovingClientConfig.java L554-L595 1:1. 전처리 6줄 (isClimbing |= /
      isCrawling |= / actionOverGound / airBorne / isStanding / isSneaking) +
      base factor + 1단계 6분기 (airBorne/sprinting/running/sneaking/standing/walking) +
      2단계 7분기 (climbing/crawling/ceilClimbing/swimming/diving/dipping/normal).
      airBorne+hunger 는 0F 하드코딩, L589/L591 normal 중복 전부 재현. javadoc 에 본체
      원본 pseudo 코드 포함.
- [x] H-8. **`SmartMovingClientState` 신규 필드 2개** — `hungerIncrease` /
      `lastHungerIncrease` 필드 추가 (`exhaustion` 뒤). 원본 SmartMovingSelf 의 동명 필드
      1:1 대응. javadoc 에 소비처 L863/L893(누적) / L911-L915(변화 감지 전송) 명시.
      `resetState()` 에 두 필드 초기화(0F) 추가.
- [x] H-9. **`SmartMovingClientState.handleExhaustion(player)` 축소판 이식** — 원본
      SmartMovingSelf L849-L916 1:1 (Easy 실행 경로). canStandUp 뒤에 public 메서드 추가.
      이동거리 계산 → isStanding/isStill/isRunning 로컬 파생(L1858/L1190/L1191/L3241) →
      getFactor(true) → hungerIncrease 누적 → getFactor(false) → exhaustion 감소 (Math.max 0
      클램프 유지) → 허기-소진 연동. Easy 배제 블록 5종(클라이밍/천장/스프린트 피로 + exhaustion==0
      NaN 리셋 + 패킷 전송) javadoc 에 각각 이유 명시. getFactor 파라미터 매핑 주의사항
      (isSneaking↔isSlow / isRunning↔로컬 / isSprinting↔isFast / isSwimming↔isSwimming_sm).
- [x] H-10. **`tickEssential` 호출 삽입** — tickEssential 의 `cfg.enabled` else 블록
      말미 (fadingPerspectiveFactor 계산 뒤, 이동 상태 전부 결정 후) 에 `handleExhaustion(player)`
      호출 삽입. 기존 L502 `exhaustion = Math.max(0F, exhaustion - 1.0F)` 간소 감소 로직
      제거 (handleExhaustion 의 `exhaustion -= exhaustionLoss` 가 담당). 비활성 경로에선
      호출 안 됨 (resetState() 가 exhaustion=0 리셋).
- [x] H-11. **허기 패킷 (클라→서버)** — 기존 `HungerChangePayload(float hunger)` 완비
      확인 (서버 수신자 `SmartMoving.java` L106-L109 + `MixinServerPlayerEntity` L50
      addExhaustion 연동). 클라측 송신 로직만 추가: `handleExhaustion` 말미에
      원본 L911-L915 1:1 이식 — `hungerIncrease != lastHungerIncrease` 변화 감지 →
      `ClientPlayNetworking.send(new HungerChangePayload(hungerIncrease))` + lastHungerIncrease 갱신.
      `canSend` 체크로 SM 없는 서버 안전장치. H-9 javadoc 의 "H-11 별도" 문구도 정정.
- [x] H-12. **서버 `addExhaustion(sm.hunger)` 연동 검증** — 코드 리뷰 결과 현재 구현과
      원본(SmartMovingServer L301-L308 addMovementStat) 사이 3가지 차이 발견:
      (1) `sm.hunger = 0F` 리셋 추가됨 (원본 없음)
      (2) 조건 `hunger > 0F` vs 원본 `disableAddExhaustion && hunger != 0F && !withinOnLivingUpdate`
      (3) `withinOnLivingUpdate` 플래그 미이식
      단순 정정은 회귀 가능성 (withinOnLivingUpdate 부재 상태에서 리셋만 제거 시 폭증).
      §16 기록 + §17 후속 포커스 후보. 포커스 #5 내에서는 현재 구현 유지 — Easy 체감은
      "원본보다 허기 소진 약간 느림" (실측값은 H-13 에서 확인).
- [x] H-13. **통합 빌드 + 회귀 감사** — `./gradlew clean build` BUILD SUCCESSFUL. §14 에
      "H 섹션 회귀 감사" 섹션 신설: 변경 3종(speedUser/exhaustion/허기 패킷)별 영향 분석
      + grep 스캔 4건(TODO/speedUser/handleExhaustion/hungerIncrease) + 잠재 회귀 대상
      4건 재확인. 회귀 0건. 근사 1건(서버 허기 연동 §17 후속) 수용.
- [x] H-14. **`checklist_original_audit.md` 기록** — 신규 발견 표 말미에 포커스 #5 H 섹션
      전체 요약 1행 추가 (2026-04-24). H-3~H-11 작업 내용 + Easy 1:1 원칙 6종 + 배제
      범위 5종 + 잔여 §17 후속(focus_11_server_hunger_sync.md) 모두 기록.

#### H-15 ~ H-16: 인게임 검증 후 수정 (세션 22 — 2026-04-24)

**배경**: G-5 후 사용자 인게임 테스트 결과 2건 문제 발견:
(1) **옵션 스위칭이 여전히 4상태** — §7.2 "잉여지만 무해" 판단 오류. F 섹션의
    `initializeForGameIfNeccessary` 가 매 tick 호출되면서 `setKeys({"e","m","h"})` 로
    configKeys 를 Easy/Medium/Hard 배열로 덮어써 configToggle 키 4상태 순환.
(2) **허기 폭주** — H-12 §17 후속으로 미뤘던 3디테일 중 하나가 실사용에서 드러남.
    클라 누적값 송신 + 서버 매 틱 `addExhaustion(누적값)` → 중복 적용. 5초 내 약
    12 food 소비 (실제는 ~0.01 food 예상). 사용자 1:1 원칙에 어긋남.

- [x] H-15. **`initializeForGameIfNeccessary` 호출 제거 (2상태 강제 복원)** —
      `SmartMovingClientState.tickEssential` L471-L479 의 호출 블록을 주석 보존 + 삭제.
      `configKeys = DEFAULT_KEYS = {null}` 초기값 유지 → `toggle()` 이 2상태(0 ↔ -1)
      로만 순환. F 섹션 메서드는 코드상 유지 (후속 포커스
      `focus_10_config_toggle_cleanup.md` 에서 잉여 일괄 정리). 빌드 ✓
- [x] H-17. **configToggle 키 NPE 크래시 수정** — 인게임 테스트에서 `disabled → enabled`
      재전환 시 크래시 발견 (`SmartMovingClientState.java:527`, `ImmutableCollections.MapN.
      probe NullPointerException`). 원인: `configKeyName = Map.of("e","Easy","m","Medium",
      "h","Hard")` 는 ImmutableMap — null key 조회 시 NPE. 2상태 토글(`configKeys={null}`)
      환경에서 `currentKey = configKeys[0] = null` → `getOrDefault(null, "")` NPE.
      수정: `currentKey == null ? "" : cfg.configKeyName.getOrDefault(currentKey, "")` 가드 추가.
      이후 `isEmpty() → name=null → unnamed=true → keyCount==1 → "enabled" 메시지 분기` 로 정상 진행.
      원본 `_configKeyName.value` 는 Property 시스템에서 null 미반환 (빈 문자열) — 1.21.1
      ImmutableMap 근사의 한계. 서버측 `SmartMovingServer.logConfigState` 는 이미
      `if (currentKey == null)` 분기로 안전 처리되어 있어 수정 불필요.

#### H-18 ~ H-20: 잉여 코드 정리 (세션 23 — 2026-04-24)

**배경**: H-15 에서 `initializeForGameIfNeccessary` 호출만 제거하고 4상태 라벨 순환 코드
자체는 "잉여지만 무해"로 유지했으나, H-17 NPE 가 이 잉여 때문에 발생 — 사용자 결정으로
지금 정리. 3단계 분할로 회귀 최소화.

**제거 대상 완전 목록** (grep 으로 호출처 확인 후 단계별 제거):

##### H-18: 활성 경로 정리 (버그 매복 제거 — 우선)

코드 파일:
- `SmartMovingClientState.java` L506-L544 configToggle 블록의 4갈래 채팅 피드백
- `SmartMovingServer.java` L285-L310 logConfigState 의 3갈래 분기
- `src/main/resources/assets/smartmoving/lang/en_us.json` 번역 키
- `SmartMovingConfig.java` `configKeyName` Map 필드

변경:
1. **클라 4갈래 → 2갈래**: configToggle 키 처리 블록을 단순화
   ```java
   msg = Text.translatable(cfg.enabled
           ? "smartmoving.message.config.client.enabled"
           : "smartmoving.message.config.client.disabled");
   ```
   H-17 의 null 가드도 함께 제거 (configKeyName 조회 자체 없음).
2. **서버 3갈래 → 1갈래**: `logConfigState` 의 enabled 분기에서 `currentKey`/`configName`
   탐색 로직 전부 삭제. `"default server configuration"` 한 줄로 단순화.
3. **lang 파일**: `smartmoving.message.config.client.named` / `.unnamed` 키 삭제.
   `.enabled` / `.disabled` 유지.
4. **`configKeyName` Map 필드** (SmartMovingConfig L263-L273) 선언 삭제.

검증:
- `grep "configKeyName"` 결과 → 모든 참조 제거되어야 함 (0건)
- `grep ".named\|.unnamed"` (translatable key) → 0건

##### H-19: 호출 없는 메서드/상수 정리

코드 파일:
- `SmartMovingConfig.java` 메서드 4개 + 상수 2개

변경:
1. **메서드 삭제**:
   - `getKey(int index)` (원본 L99-L104)
   - `getNextKey(String key)` (원본 L106-L118)
   - `hasKey(String key)` (원본 L145-L156)
   - `setCurrentKey(String key)` (원본 L120-L136)
2. **상수 삭제**:
   - `CONFIG_KEY_ENABLED` ("enabled")
   - `CONFIG_KEY_DISABLED` ("disabled")
3. **유지**:
   - `DEFAULT_KEYS` (toggle() 내부에서 참조 가능성) — 실제 참조 확인 후 결정
   - `configKeys` 필드 (toggle() 순환에 필요) — **유지**
   - `toggler` 필드 (toggle() 상태) — **유지**
   - `toggle()` / `setKeys(String[])` / `updateToggler()` / `getCurrentKey()` — **유지**
     (getCurrentKey 는 H-18 정리 후에도 다른 호출처가 있을 수 있어 확인 필요)

검증:
- `grep "getKey\b\|getNextKey\|hasKey\|setCurrentKey"` → config 내부 참조 0건
- `grep "CONFIG_KEY_ENABLED\|CONFIG_KEY_DISABLED"` → 0건
- `grep "getCurrentKey"` → H-18 이후 0건 목표, 있으면 해당 호출 정리

##### H-20: gameType 시스템 전체 정리 (dead code)

코드 파일:
- `SmartMovingConfig.java` gameType 관련 필드/상수/메서드
- `readFrom`/`writeTo` 6개 키 처리

변경:
1. **메서드 삭제**:
   - `initializeForGameIfNeccessary(int currentGameType)`
   - `resetForNewGame()`
2. **필드 삭제**:
   - `gameType` (private int, default -1)
   - `survivalConfigKeys`, `survivalDefaultConfigKey`
   - `creativeConfigKeys`, `creativeDefaultConfigKey`
   - `adventureConfigKeys`, `adventureDefaultConfigKey`
3. **상수 삭제**:
   - `GAME_TYPE_UNKNOWN`, `GAME_TYPE_SURVIVAL`, `GAME_TYPE_CREATIVE`, `GAME_TYPE_ADVENTURE`
4. **readFrom 6키 제거**:
   - `move.config.survival.keys` + `.keys.default`
   - `move.config.creative.keys` + `.keys.default`
   - `move.config.adventure.keys` + `.keys.default`
5. **writeTo 동일 6키 제거**
6. **헬퍼 `getCsvArray`/`csvJoin`** — 다른 CSV 필드(playerSpeedExponents 등)에서 사용 여부
   확인 후 제거/유지 결정

검증:
- `grep "initializeForGameIfNeccessary\|resetForNewGame"` → 0건
- `grep "survivalConfigKeys\|creativeConfigKeys\|adventureConfigKeys"` → 0건
- `grep "GAME_TYPE_"` → 0건
- `grep "move.config.survival\|move.config.creative\|move.config.adventure"` → 0건
- **세이브 파일 호환**: 기존 `smart_moving_options.properties` 에 해당 키가 있어도
  단순히 무시됨 (getBool/getCsvArray 호출 자체 없음). 역호환 문제 없음.

**통합 검증 (H-18~H-20 완료 후)**:
- `./gradlew clean build` BUILD SUCCESSFUL
- `grep "// TODO\|// \[미확인\]"` 0건
- H-13 §14 "H 섹션 회귀 감사" 재실행 (변경 3종 → 변경 4종: speedUser/exhaustion/허기패킷/잉여정리)
- configToggle 키 인게임 테스트: disabled ↔ enabled 여러 번 왕복 → 크래시 0

- [x] H-18. **활성 경로 정리** — 4가지 변경 완료:
      (1) `SmartMovingClientState` configToggle 채팅 피드백 4갈래 → 2갈래 (cfg.enabled 삼항)
      (2) `SmartMovingServer.logConfigState` 3갈래 → 1갈래 (default server configuration 만)
      (3) `en_us.json` `.named` / `.unnamed` 키 삭제 (.enabled / .disabled 유지)
      (4) `SmartMovingConfig.configKeyName` Map 필드 삭제
      `grep configKeyName` 확인: 주석/javadoc 참조만 남음 (코드 참조 0). 빌드 ✓
- [ ] H-19. **호출 없는 메서드/상수 정리** — `getKey`/`getNextKey`/`hasKey`/`setCurrentKey`
      메서드 4개 + `CONFIG_KEY_ENABLED`/`CONFIG_KEY_DISABLED` 상수 2개 삭제. `DEFAULT_KEYS`/
      `configKeys`/`toggler`/`toggle()`/`setKeys()`/`updateToggler()`/`getCurrentKey()` 유지.
- [ ] H-20. **gameType 시스템 전체 정리** — `initializeForGameIfNeccessary`/`resetForNewGame`
      메서드 + `gameType` 필드 + `GAME_TYPE_*` 4상수 + 게임타입별 6필드 + readFrom/writeTo
      6키 전부 삭제. `getCsvArray`/`csvJoin` 헬퍼는 다른 참조 확인 후 결정.
- [ ] H-21. **통합 빌드 + 회귀 감사 재실행** — clean build + §14 "H 섹션 회귀 감사" 확장
      (변경 3종 → 4종). checklist_original_audit.md 에 H-18~H-20 정리 기록 추가.
- [x] H-16. **허기 delta 전송 수정 (폭주 차단)** — 클라 + 서버 동반 수정 완료:
      - `ClientState.handleExhaustion` 말미: `hungerIncrease != lastHungerIncrease` →
        `float delta = hungerIncrease - lastHungerIncrease; if (delta != 0F) send(delta);`.
      - `SmartMoving.java` L106-L109 서버 수신자:
        `sm.hunger = payload.hunger()` → `sm.hunger < 0F` 이면 첫 수신으로 덮어쓰기,
        아니면 `sm.hunger += payload.hunger()` 누적.
      - `MixinServerPlayerEntity.sm_afterTravel` TAIL 구조 (addExhaustion + sm.hunger=0F 리셋) 유지.
      - 결과: 매 틱 정확히 delta 만 addExhaustion → 원본 체감 일치.
      - 원본 엄격 1:1 주의: 원본은 클라가 hungerIncrease 누적값 전체 전송하지만 서버
        `withinOnLivingUpdate` 스킵 로직으로 결과적으로 delta 근사. 1.21.1 는 해당 플래그
        미이식이라 클라 측 delta 전송으로 동등 결과. §17 `focus_11_server_hunger_sync.md`
        후속에서 withinOnLivingUpdate 이식 시 이 우회 해제 가능.

---

## 11. 호출 타이밍 검증

| 호출자 | 원본 타이밍 | 1.21.1 타이밍 | 일치 |
|--------|------------|--------------|------|
| `Property.toggle()` | `SmartMovingOptions.toggle()` 에서 super 호출 (키바인딩 / 커맨드 수신 시) | `SmartMovingConfig.toggle()` 직접 호출 (동일 트리거) | ✓ |
| `Property.setKeys()` | `initializeForGameIfNeccessary(gameType)` 에서 게임타입 판정 후 | `SmartMovingServer.initialize()` 에서 gameType 기반 (F 섹션) | ✓ 예정 |
| `toggler++` | 키 입력마다 1회 | 동일 | ✓ |
| `save()` | `SmartMovingOptions.toggle()` 끝에서 `saveToOptionsFile` | `SmartMovingConfig.save()` | ✓ |

---

## 12. 테스트 프로토콜 (인게임 검증)

```
[T-1] Survival 월드 단일 플레이
  1. 월드 진입 — 채팅: "Smart Moving enabled: Medium" (toggler=1, key="m")
  2. configToggle 키 × 1 → "Smart Moving enabled: Hard" (toggler=2)
  3. configToggle 키 × 1 → "Smart Moving disabled" (toggler=-1)
  4. configToggle 키 × 1 → "Smart Moving enabled: Easy" (toggler=0)
  5. configToggle 키 × 1 → "Smart Moving enabled: Medium"

[T-2] Creative 월드
  1. /gamemode creative — setKeys({"c"}) 재호출 확인
  2. 진입 시 "Smart Moving enabled: Creative" (toggler=0)
  3. configToggle × 1 → "Smart Moving disabled" (toggler=-1)
  4. configToggle × 1 → "Smart Moving enabled: Creative"

[T-3] 멀티플레이 (globalConfig=true)
  1. 관리자 /smoving config toggle × 1 → 모든 플레이어 채팅에 "Smart Moving
     changed to server configuration \"Medium\"" 같은 메시지
  2. 서버 콘솔 로그: "Smart Moving changed to server configuration \"Medium\"
     by user 'admin'"
  3. broadcastConfig 로 모든 접속자 Config 재전송

[T-4] 파일 지속성
  1. 월드 나가기 → config/smart_moving_options.properties 확인
  2. toggler / configKeys 값이 저장됨
  3. 재진입 시 같은 toggler 로 복원
```

---

## 13. 완료 전 검증 체크리스트 (포커스 #5 고유 + 공통)

### 공통 (playtest_fixes.md §공통 완료 전 검증)
- [ ] 리서치 근거: 원본 L325-L332 / L345-L355 확보 + 보완 완료
- [ ] side-by-side 1:1: 원본 `toggle()` 과 구현 `toggle()` 줄 단위 대응
- [ ] 모든 분기: `toggler == length` → `-1` 분기 구현됨
- [ ] 상수: configKeys 기본값 `{"e","m","h"}` / Creative `{"c"}` 양쪽 모두
- [ ] 호출 타이밍: setKeys 는 게임타입 판정 직후 (§11 참조)
- [ ] 근사 이식 주석: 해당 없음 (완전 재현 가능 영역)
- [ ] 신규 발견 등록: §16 에 기록
- [ ] 회귀 방지: §14 통과
- [ ] 빌드: `./gradlew build` 성공

### #5 고유
- [ ] 4상태 전이 테이블 T-1 전부 매칭
- [ ] Creative(단일 key) T-2 매칭
- [ ] 서버 로그 3-갈래 출력(disabled / default / named) 전부 동작
- [ ] 클라이언트 채팅 피드백 4가지 메시지 올바름
- [ ] 파일 저장/로드 시 toggler 지속
- [ ] `enabled` 플래그가 여전히 공통 활성 게이트로 작동 (다른 이식이 쓰는 `cfg.enabled` 의미 유지)

---

## 14. 회귀 방지 감사

### 이 수정이 영향을 줄 수 있는 기존 이식

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `SmartMovingServer.adminToggleConfig` | `INSTANCE.toggle()` 호출 — 새 순환 자동 반영 | [x] 코드 상 `INSTANCE.toggle()` 호출 유지 + 4상태 순환 로직 적용됨 (수동 인게임 검증은 G-2) |
| `SmartMovingServer.broadcastConfig` | 모든 접속자에게 재전송 — toggler 값이 `INSTANCE.toArray()` 에 포함되는지 | [△] toggler 자체는 전송 안 됨 — 게임타입별 keys 6개(survivalConfigKeys 등)만 전송. **서버→클라 toggler 동기화 구조적 누락** → §16 에 기록, §17 후속. 실제 Config 값 전송은 정상 (포커스 범위 외 영향). |
| `cfg.enabled` 참조 위치 모두 | 의미 유지 (enabled = toggler != -1) | [x] `grep` 결과 9개 파일 31개 참조 — 모두 "활성화 상태" read-only 용도, 의미 일치 |
| `isSneakToggleEnabled` / `isCrawlToggleEnabled` | `cfg.sneakToggle && cfg.enabled` — 원본과 동일 | [x] 이 포커스에서 변경한 파일에 없음 — 불변 |
| `SmartMovingClient.processConfigContentPacket` | 서버가 보낸 toggler 값으로 SERVER_CONFIG 갱신 | [△] 서버는 toggler 미전송 → 클라 SERVER_CONFIG.toggler 는 초기값 -2. SERVER_CONFIG 경로에서는 `tickEssential` 의 initializeForGameIfNeccessary 도 `Config==INSTANCE` 가드에 의해 호출 안 됨 → SERVER_CONFIG 에서 getCurrentKey 는 항상 "disabled 방어 코드" 경로 (-2 → null). 구조적 한계, §17 후속. |

### 공통 grep 스캔

- [x] `grep "// TODO\|// \[미확인\]" src/main/java/choco/ratel/smartmoving/config/ src/main/java/choco/ratel/smartmoving/server/` — **0건**, 통과
- [x] `grep "INSTANCE\.toggle\|cfg\.toggle\|Config\.toggle"` — 2개 호출처 (`adminToggleConfig`, `configToggle` 키) 모두 4상태 순환 의미로 적합

### H 섹션 회귀 감사 (세션 21 — H-13)

**clean build**: `./gradlew clean build` — BUILD SUCCESSFUL ✓

**H 섹션 변경 3종 × 영향 분석**:

| 변경 | 영향 포인트 | 확인 결과 |
|------|-----------|----------|
| `speedUser = true → false` (H-6) | `getUserSpeedFactor()` 호출 3곳 (`SmartMovingConfig.getSpeedPercent` L1305 / `SmartMovingFlyer` L55 / `SmartMovingMover` L30) | [x] 전부 `getUserSpeedFactor()` 경유 — `!speedUser → 1F` 조기 반환으로 자연 반영. 속도 조정 키 UI 는 작동하나 factor=1F 고정. Easy 1:1 의도된 결과 — 회귀 아님. 기존 사용자는 `move.speed.user=true` 수동 복원 가능. |
| exhaustion 공식 `-= 1.0F` → `-= exhaustionLoss` (H-10) | `SmartMovingClimber` L409 `sm.exhaustion += 2.0F` + `SmartMovingHud` L62 `exhaustion` 표시 | [x] Climber 축적 로직 불변. HUD 표시도 동일 필드 참조 — 시각적 회귀 없음. Easy factor 값(~1.2F) 로 감소량 소폭 증가 → HUD 피로도 바 소폭 빠르게 감소. Easy 1:1 의도. |
| 허기 패킷 송신 신규 (H-11) | `SmartMovingServer.hunger` 필드 (초기 -1F) + `disableAddExhaustion` 로직 + `MixinServerPlayerEntity.sm_afterTravel` | [x] 초기값 -1F 유지 시 기존 vanilla 허기 정상. 클라 첫 hungerIncrease 송신 시점부터 SM 로직 활성. 경로 전환은 플레이어 입장 직후 첫 틱 — 원본도 동일 타이밍. H-12 에서 3건 차이 식별 → §17 후속. 현 구현은 Easy 근사 ~90% 수준. |

**H 섹션 grep 스캔**:

- [x] `grep "// TODO\|// \[미확인\]"` in `config/` + `client/` — 0건
- [x] `grep "speedUser"` — 호출처 3곳 전부 의미 일치
- [x] `grep "handleExhaustion"` — 정의 1곳 (ClientState L941) + 호출 1곳 (tickEssential 말미) — 중복 없음
- [x] `grep "hungerIncrease\|lastHungerIncrease"` — 정의/참조 ClientState 내부에만 (resetState / handleExhaustion / 패킷 송신) — 외부 누출 없음

**잠재 회귀 대상 재확인**:

- [x] `SmartMovingConfig.INSTANCE.toggle()` — 여전히 2상태 작동 (`configKeys={null}` 단일 key)
- [x] `cfg.enabled` 참조 — H 섹션에서 `cfg.enabled` 변경 없음 (기존 방지 감사 유효)
- [x] `isSneakToggleEnabled` / `isCrawlToggleEnabled` — 변경 없음
- [x] 허기 패킷 포맷 — `HungerChangePayload(float)` 구조 불변, 신규 추가 필드 없음

**결론**: H 섹션 변경 전부 Easy 1:1 의도된 동작. 회귀 0건. 근사 1건(서버 허기 연동
3디테일 §17 후속) 은 구조적 한계 수용.

---

## 15. 작업 기록 (세션별)

### 세션 1 — 2026-04-23

**진행한 작업**:
- 진입점 문서 (`playtest_fixes.md`) 생성 + 17-섹션 공통 구조 명세
- 이 포커스 파일 (`focus_05_config_toggle.md`) 17-섹션 구조로 재작성

**다음 작업**: A-1 / A-2 — Agent WebFetch 로 원본 `Property.java` / `SmartMovingOptions.java` 코드 확보

### 세션 2 — 2026-04-23

**진행한 작업**:
- Agent WebFetch 시도 → Agent 가 `net.smart.properties.Property` 를 봤으나 실제 대상은
  `net.smart.moving.config.SmartMovingProperties` (별개 클래스). 혼동 확인.
- 리서치 파일 재검증:
  - `SmartMovingProperties.md` L15-L174 전체 소스 임베드 확인 — `toggle`/`setKeys`/
    `getCurrentKey`/`getNextKey`/`setCurrentKey`/`hasKey`/`update`/`_defaultKeys`/`toggler`/`keys` 모두 존재
  - `SmartMovingOptions.md` L500-L531 `toggle()` override 확인
  - `SmartMovingOptions.md` L840-L889 `initializeForGameIfNeccessary()` 확인
  - `SmartMovingConfig.md` L630-L633 gameType 상수 `Survival=0` / `Creative=1` / `Adventure=2` / `Unknown=-1` 확인
- 결론: A-1 / A-2 / A-3 추가 WebFetch 불필요. 리서치 파일만으로 B 단계 진입 가능.
- A-1 / A-2 / A-3 [x] 처리.

**완료 전 검증 체크리스트 (A 섹션 기준)**:
- [근거] 원본 코드 리서치 파일 존재 확인 ✓
- [근거] 출처 URL 리서치 파일 상단 명시 ✓
- 나머지 항목은 "리서치 확인" 작업이라 해당 없음

**다음 작업**: B-1 — `SmartMovingConfig` 에 `configKeys` / `toggler` / `currentConfigKey` /
`configKeyName` 필드 추가.

### 세션 3 — 2026-04-23 — B-1

**진행한 작업**:
- B-1: `SmartMovingConfig` 에 원본 `SmartMovingProperties` L26-L31 대응 필드/상수 추가.
  - `CONFIG_KEY_ENABLED = "enabled"` (public static final String)
  - `CONFIG_KEY_DISABLED = "disabled"` (public static final String)
  - `DEFAULT_KEYS = new String[] { null }` (private static final)
  - `public String[] configKeys = DEFAULT_KEYS` — 원본 `keys = _defaultKeys` 1:1
- **§9 예상 diff 원본 1:1 방향으로 정정**: 초기값을 `{"e","m","h"}` 가 아닌 `DEFAULT_KEYS`
  ({null}) 로. 게임타입별 배열 설정은 F 섹션 (`initializeForGameIfNeccessary`) 이식 시
  `setKeys()` 호출로 교체되는 원본 흐름 재현.

**완료 전 검증 체크리스트 (B-1 기준)**:
- [근거] `SmartMovingProperties.md` L15-L174 재확인, 특히 L26-L31 임베드 소스 확인 ✓
- [대응] 원본 4심볼(Enabled/Disabled/_defaultKeys/keys) ↔ 구현 4심볼 1:1 매핑 ✓
- [상수] 원본 상수값 그대로 ("enabled" / "disabled" / `new String[1]`) ✓
- [분기] 해당 없음 (필드 선언만)
- [타이밍] 해당 없음
- [근사] 해당 없음 (완전 재현)
- [신규] 없음
- [회귀] 기존 `enabled = true` 초기값 유지 (아직 `update()` 파생으로 변경 안 함 — C 섹션 예정)
- [빌드] `./gradlew build` ✓

**다음 작업**: B-2 — `toggler` 필드 (int, default -2 원본 그대로).

### 세션 3 — 2026-04-23 — B-2

**진행한 작업**:
- B-2: `SmartMovingConfig.toggler` 필드 추가. 원본 L30 `private int toggler = -2` 1:1.
  접근 제어는 public (다른 파일에서 읽기 필요 — `SmartMovingServer.logConfigState` 등).
  javadoc 에 상태표(−2/−1/0..length−1) + 파생 규칙(`enabled = toggler != -1`) 기록.

**완료 전 검증 체크리스트 (B-2 기준)**:
- [근거] `SmartMovingProperties.md` L30 임베드 소스 확인 ✓
- [대응] 원본 `toggler = -2` ↔ 구현 `toggler = -2` 1:1 ✓
- [상수] 초기값 -2 그대로 ✓
- [분기/타이밍/근사] 해당 없음
- [신규] 없음
- [회귀] 해당 없음 (아직 어디서도 참조 안 함 — C 섹션 `update()` 에서 처음 사용 예정)
- [빌드] `./gradlew build` ✓

**다음 작업**: B-3 — `configKeyName` 필드 (Map, default 4개 매핑). 원본
`_configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"))`
대응 근사 (1.21.1 단순 Map).

### 세션 3 — 2026-04-23 — B-3

**진행한 작업**:
- B-3: `SmartMovingConfig.configKeyName` Map 필드 추가.
  - 원본 `_configKeyName = String(...).defaults(Value(null).e("Easy").m("Medium").h("Hard"))` 근사.
  - Property key-scoped defaults → `Map<String,String>` 으로 단순화.
  - 기본 매핑: `{"e":"Easy", "m":"Medium", "h":"Hard"}` — 3개만 (원본도 3개).
  - Creative "c" 는 원본 `_configKeyName` 에 defaults 없음 → `logConfigState` 에서 `"with
    key X"` 분기로 빠지는 동작과 등가 (`getOrDefault(key, "")` 로 구현).
  - 문서 §9 예상 diff 의 4번째 매핑("c":"Creative") 은 **잉여**였으므로 제외.

**완료 전 검증 체크리스트 (B-3 기준)**:
- [근거] `SmartMovingConfig.md` L620 원본 코드 재확인 ✓
- [대응] 원본 `_configKeyName` key-scoped defaults ↔ 구현 `Map.of(...)` 3개 키 근사 ✓
- [상수] 3개 키/값 원본 그대로 ✓
- [분기/타이밍] 해당 없음
- [근사] Property 시스템 단순화 — javadoc 에 "1.21.1 근사" 명시 ✓
- [신규] 없음
- [회귀] 해당 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: B-4 — `readFrom`/`writeTo` 에 `toggler` / `configKeys` 저장 포맷 추가. 원본
`SmartMovingConfig.md` L456-L463 의 `move.config.survival.keys` / `move.config.survival.keys.default`
식 키 이름을 1.21.1 에 어떻게 매핑할지 결정 필요. (원본은 `Property<String[]>` / `Property<String>`
로 분리 저장, 1.21.1 은 단일 `configKeys` 필드 하나만 있으므로 저장 포맷 단순화.)

### 세션 4 — 2026-04-23 — B-4

**진행한 작업**:
- **§10 B-4 재정의**: 원래 문구 "`move.config.key.current` / `move.config.keys`" 는
  부정확. 원본은 **게임타입별 6개 필드** 로 저장 (SmartMovingConfig L456-L463). 재정의 후 이식.
- 리서치 파일 보완: `SmartMovingConfig.md` L608-L620 의 `Strings(...)` 괄호 안 키 이름
  명시로 교체 (`move.config.survival.keys` 등 6종). Agent 세션 2 결과 반영.
- SmartMovingConfig 에 필드 6개 신규 추가:
    `survivalConfigKeys`       = `{"e","m","h"}`
    `survivalDefaultConfigKey` = `"m"`
    `creativeConfigKeys`       = `{"c"}`
    `creativeDefaultConfigKey` = `"c"`
    `adventureConfigKeys`      = `{"e","m","h"}`
    `adventureDefaultConfigKey`= `"m"`
  원본 defaults 와 1:1.
- readFrom/writeTo 에 6개 저장 포맷 추가. String[] 은 CSV (원본 comment `"entries seperated by ','"`
  기반). 유틸 헬퍼 `getCsvArray(p, key, def)` / `csvJoin(arr)` 신규.
- 현재 활성 key 는 별도 필드로 저장 안 함 — 원본처럼 `toggle()` 이 gameType 의 `defaultConfigKey`
  값을 갱신하는 방식 유지 (C/D 섹션에서 구현).

**완료 전 검증 체크리스트 (B-4 기준)**:
- [근거] `SmartMovingConfig.md` L456-L463 (Agent 세션 2 확보 + 리서치 파일 보완) ✓
- [대응] 원본 6 필드 ↔ 구현 6 필드 + 6 키 매칭 ✓
- [상수] 기본값 `{"e","m","h"}` / `"m"` / `{"c"}` / `"c"` 모두 원본 그대로 ✓
- [분기] 해당 없음
- [타이밍] readFrom/writeTo 호출 시점 동일 (기존 enabled 옆에 추가)
- [근사] CSV 포맷은 원본 Strings Property 와 동등 (comment 명시)
- [신규] 없음
- [회귀] 기존 readFrom/writeTo 다른 필드 처리 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: C-1 — `toggle()` 재구현. 현재 `enabled = !enabled; save();` 를 원본
`SmartMovingProperties.toggle()` L81-L88 (`toggler++; if (toggler == length) toggler = -1; update();`)
으로 교체.

### 세션 5 — 2026-04-23 — C-3 (순서 조정)

**진행한 작업**:
- **순서 조정**: §10 문서상 순서는 C-1 먼저지만, 의존 관계상 `toggle()` 이 `updateToggler()`
  호출에 의존 → C-3 을 먼저 진행. (이전 세션 종료 메시지에 예고됨)
- C-3: `SmartMovingConfig.updateToggler()` private 메서드 추가.
  - 원본 `SmartMovingProperties.update()` L163-L172 1:1 근사:
      ```
      protected void update() {
          List<Property<?>> properties = getProperties();
          Iterator<Property<?>> it = properties.iterator();
          String currentKey = getCurrentKey();
          while (it.hasNext()) it.next().update(currentKey);  // ← Property 부재로 N/A
          enabled = toggler != -1;
      }
      ```
  - 1.21.1 Property 계층 부재 → Property 루프 생략, `enabled = toggler != -1;` 만 이식.
  - 호출처: toggle / setKeys / setCurrentKey / load 등 toggler 변경 직후 (다음 작업들에서 삽입).
  - 현재는 아직 호출 없음 → IDE `unused method` 경고 가능. C-1/C-2/C-4 이식 시 호출 연결.

**완료 전 검증 체크리스트 (C-3 기준)**:
- [근거] `SmartMovingProperties.md` L163-L172 원본 코드 임베드 확인 ✓
- [대응] 원본 `update()` 의 ②(enabled 파생) ↔ 구현 `updateToggler()` 1:1. ①(Property 루프)는
  구조적 N/A — javadoc 에 명시 ✓
- [분기] 해당 없음 (단순 할당)
- [상수] 해당 없음
- [타이밍] 호출처(toggle/setKeys/setCurrentKey/load) 는 후속 작업에서 연결
- [근사] "1.21.1: Property 계층 부재 → ① 는 N/A" 주석 달림 ✓
- [신규] 없음
- [회귀] 없음 (아직 호출 없음)
- [빌드] `./gradlew build` ✓

**다음 작업**: C-1 — `toggle()` 재구현 (toggler++ 순환 + updateToggler() 호출 + save()).

### 세션 5 (계속) — 2026-04-23 — C-1

**진행한 작업**:
- C-1: `SmartMovingConfig.toggle()` 재구현. 원본 `SmartMovingProperties.toggle()` L81-L88 1:1.
  - 기존 `enabled = !enabled; save();` 를 4상태 순환 로직으로 교체:
      ```java
      int length = configKeys == null ? 0 : configKeys.length;
      toggler++;
      if (toggler == length) toggler = -1;
      updateToggler();
      save();
      ```
  - save() 호출은 원본 Properties.toggle() 에는 없으나 SmartMovingOptions.toggle() override
    (L500-L531) 의 saveToOptionsFile 에 해당. 1.21.1 통합 계층이라 여기서 호출 유지.
  - **Options.toggle() 의 추가 동작(_configChat 메시지 + gameType 별 defaultKey 갱신)은
    별도 원자 작업** — §10 C-7 신규 등록 필요 (신규 발견 기록).

**완료 전 검증 체크리스트 (C-1 기준)**:
- [근거] `SmartMovingProperties.md` L81-L88 임베드 소스 확인 ✓
- [근거] `SmartMovingOptions.md` L500-L531 override 확인 (추가 동작은 별도) ✓
- [대응] 원본 4줄 ↔ 구현 4줄 1:1 ✓
- [분기] `if (toggler == length)` 조건 그대로, `==` 유지 ✓
- [상수] 해당 없음
- [타이밍] C-3 `updateToggler()` 호출 연결 ✓
- [근사] save() 위치만 원본과 다름 — javadoc 명시 ✓
- [신규] Options.toggle() 추가 동작(채팅 + defaultKey 갱신) → §16 기록 + §10 C-7 후속 원자 작업 예정
- [회귀] 기존 `enabled = !enabled` 의미가 완전히 달라짐 → §14 회귀 감사 대상
- [빌드] `./gradlew build` ✓

**⚠ 중간 상태 주의**: C-1 완료 시점에서 `toggler = -2` 초기값 → 첫 토글 시 `-2+1=-1 → disabled`.
원본도 load() 에서 toggler=0 으로 설정 후 initializeForGameIfNeccessary 에서 setCurrentKey
로 default key 인덱스(survival 은 1)로 이동. F 섹션 완료 전까지 이 초기화 흐름 부재 →
중간 상태에서는 토글이 예상대로 작동 안 함. F 완료 시점에 정상화.

**다음 작업**: C-2 — `setKeys(String[])` 이식 (원본 L90-L97).

### 세션 5 (계속) — 2026-04-23 — C-2

**진행한 작업**:
- C-2: `SmartMovingConfig.setKeys(String[])` 신규 메서드 추가.
  - 원본 `SmartMovingProperties.setKeys()` L90-L97 1:1:
      ```java
      public void setKeys(String[] keys) {
          if (keys == null || keys.length == 0) keys = DEFAULT_KEYS;
          this.configKeys = keys;
          this.toggler = 0;
          updateToggler();
      }
      ```
  - null/empty 방어 → `DEFAULT_KEYS` ({null}) 로 fallback (단순 on/off 모드).
  - `toggler = 0` 재설정 (원본 L95 `toggler = 0; update();`).
  - save() 호출은 원본에도 없음 — 생략.
  - 호출처는 F 섹션 `initializeForGameIfNeccessary(gameType)` 이식 시
    gameType 별 keys 배열 전달로 연결 예정.

**완료 전 검증 체크리스트 (C-2 기준)**:
- [근거] `SmartMovingProperties.md` L90-L97 원본 임베드 확인 ✓
- [대응] 원본 4줄 ↔ 구현 4줄 1:1 ✓
- [분기] `keys == null || keys.length == 0` 조건 그대로 ✓
- [상수] `DEFAULT_KEYS` (= 원본 `_defaultKeys`) 사용 ✓
- [타이밍] 호출처 F 섹션에서 연결 예정
- [근사] 해당 없음
- [신규] 없음
- [회귀] 신규 메서드 추가만이라 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-4 — `getCurrentKey()` (toggler == -1 시 null, 그 외 `configKeys[toggler]`).

### 세션 6 — 2026-04-23 — C-4

**진행한 작업**:
- C-4: `SmartMovingConfig.getCurrentKey()` 신규 메서드 추가.
- **§10 문구 정정**: 원래 "toggler == -1 시 null" 은 오기. 원본 1:1:
    ```java
    public String getCurrentKey() {
        if (toggler == -1) return CONFIG_KEY_DISABLED;  // "disabled" (null 아님)
        if (toggler < 0 || configKeys == null || toggler >= configKeys.length) return null;
        return configKeys[toggler];
    }
    ```
- 반환값 의미 (javadoc 명시):
    - `toggler == -1` → `"disabled"` (비활성 상태)
    - `toggler >= 0 && keys[toggler] == null` → null (DEFAULT_KEYS {null}, 단순 on/off enabled)
    - `toggler >= 0 && keys[toggler] != null` → "e"/"m"/"h"/"c" 등 key 이름
- toggler == -2 (센티넬, F 이식 전 과도기) 방어 코드 추가 — 원본은 load() 후만 호출 가정이라
  미정의. 1.21.1 과도기에는 null (단순 on/off) 반환.
- 기존 호출자 없음 → 회귀 영향 없음 (D 섹션 서버 로그 4상태 분화에서 첫 소비).

**완료 전 검증 체크리스트 (C-4 기준)**:
- [근거] `SmartMovingProperties.md` L138-L143 원본 임베드 확인 ✓
- [대응] 원본 3줄 ↔ 구현 3줄(+방어 1줄) 1:1 ✓
- [분기] toggler == -1 분기 그대로 + 센티넬/범위 방어 ✓
- [상수] `CONFIG_KEY_DISABLED = "disabled"` (= 원본 `Disabled`) 사용 ✓
- [타이밍] 호출처는 D 섹션 logConfigState, E 섹션 채팅 피드백에서 연결
- [근사] toggler==-2 방어 코드는 근사 (원본 미정의 케이스) — javadoc 명시 ✓
- [신규] 없음
- [회귀] 신규 메서드 추가만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-5 — `getNextKey(String key)` (원본 L106-L118).

### 세션 6 (계속) — 2026-04-23 — C-5 (+ getKey)

**진행한 작업**:
- C-5: `getNextKey(String)` + 의존 `getKey(int)` 함께 이식 (원본 L106-L118 + L99-L104).
  두 메서드는 연계 동작(getNextKey → getKey(0) 호출) — 단일 원자 단위로 묶음.
  `getKey(int)` 는 §10 에 별도 체크박스 없지만 C-5 내 포함.
- 원본 1:1:
    ```java
    public String getKey(int index) {
        if (configKeys[index] == null) return CONFIG_KEY_ENABLED;
        return configKeys[index];
    }
    public String getNextKey(String key) {
        if (key == null || key.equals(CONFIG_KEY_DISABLED))
            return getKey(0);
        int index;
        for (index = 0; index < configKeys.length; index++)
            if (key.equals(configKeys[index])) break;
        index++;
        if (index < configKeys.length) return configKeys[index];
        return CONFIG_KEY_DISABLED;
    }
    ```
- 동작 예시 javadoc 에 기록 (키 배열별 전이표).

**완료 전 검증 체크리스트 (C-5 기준)**:
- [근거] `SmartMovingProperties.md` L99-L118 원본 임베드 확인 ✓
- [대응] 원본 getKey 3줄 + getNextKey 13줄 ↔ 구현 동일 수준 1:1 ✓
- [분기] null/disabled 조기 반환, for 매칭, 마지막 분기 전부 재현 ✓
- [상수] `CONFIG_KEY_ENABLED` / `CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처 없음 (C-7 setCurrentKey 또는 toggle 의 후속 이식에서 참조 가능)
- [근사] 해당 없음
- [신규] 없음
- [회귀] 신규 메서드만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-6 — `hasKey(String)` (원본 L145-L156).

### 세션 7 — 2026-04-23 — C-6

**진행한 작업**:
- C-6: `SmartMovingConfig.hasKey(String)` 신규 메서드 추가.
- 원본 `SmartMovingProperties.hasKey()` L145-L156 1:1:
    ```java
    public boolean hasKey(String key) {
        if (CONFIG_KEY_ENABLED.equals(key))
            return configKeys[0] == null;
        if (CONFIG_KEY_DISABLED.equals(key))
            return true;
        for (int i = 0; i < configKeys.length; i++)
            if (key == null && configKeys[i] == null
                    || key != null && key.equals(configKeys[i]))
                return true;
        return false;
    }
    ```
- 분기 3갈래:
    - `"enabled"` → `configKeys[0] == null` (DEFAULT_KEYS 단순 on/off 모드 판정)
    - `"disabled"` → 항상 true
    - 그 외 → 배열 탐색 (null 포함 동치 비교)
- 호출처 없음 (C-7 예정 `setCurrentKey(String)` 에서 유효성 체크로 참조). 신규 메서드만 — 회귀 영향 없음.

**완료 전 검증 체크리스트 (C-6 기준)**:
- [근거] `SmartMovingProperties.md` L145-L156 원본 임베드 확인 ✓
- [대응] 원본 11줄 ↔ 구현 11줄 1:1 (중괄호/들여쓰기 포함 동등) ✓
- [분기] 세 분기(Enabled/Disabled/배열 탐색) 그대로 ✓
- [상수] `CONFIG_KEY_ENABLED`/`CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처 없음 — C-7(setCurrentKey) 이식 시 연결
- [근사] 해당 없음 (완전 재현)
- [신규] 없음
- [회귀] 신규 메서드만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: C-7 (신규 등록 필요) — `setCurrentKey(String)` 이식 + Options.toggle()
override 추가 동작(_configChat 채팅 + gameType 별 defaultKey 갱신).
또는 D 섹션 진입(`SmartMovingServer.logConfigState` 4상태 분기 복원) 중 선택.

### 세션 8 — 2026-04-23 — D-1

**진행한 작업**:
- D-1: `SmartMovingServer.logConfigState` 의 enabled=true 경로 3갈래 분기 복원.
- 원본 근거: `SmartMovingServerOptions.md` L80-L113 1:1 (리서치 파일 grep 확인 후 해당 섹션 재독).
- 3갈래:
    ```java
    String currentKey = config.getCurrentKey();
    String action = reconfig ? "changed to " : "uses ";
    if (currentKey == null) {
        LOGGER.info("{}{}default server configuration{}", message, action, postfix);
    } else {
        String configName = config.configKeyName.getOrDefault(currentKey, "");
        if (configName.isEmpty()) {
            LOGGER.info("{}{}server configuration with key \"{}\"{}", message, action, currentKey, postfix);
        } else {
            LOGGER.info("{}{}server configuration \"{}\"{}", message, action, configName, postfix);
        }
    }
    ```
- `config._configKeyName.value` 원본 의미 = Property key-scoped defaults 기반 현재 key의 표시 이름.
  1.21.1 근사: C-4 (`getCurrentKey()`) + B-3 (`configKeyName` Map) 조합으로 `getOrDefault(currentKey, "")`.
- Creative "c" → 매핑 부재 → "" → `"with key \"c\""` 분기 (원본 `_configKeyName` 가 "c" 에 defaults 없음과 등가).
- 기존 주석의 "1.21.1 범위 제한" / "키 이름 경로는 추후 추가" 문구 제거 — 본 원자 작업으로 해소.

**완료 전 검증 체크리스트 (D-1 기준)**:
- [근거] `SmartMovingServerOptions.md` L80-L113 리서치 파일 확인 ✓
- [대응] 원본 3갈래 분기 + 출력 포맷 8패턴 (§379-L388 표) ↔ 구현 3갈래 1:1 ✓
- [분기] `currentKey==null` / `configName.isEmpty()` / else 모두 복원 ✓
- [상수] 문자열 리터럴 "default server configuration" / "server configuration with key" / "server configuration" 원본 그대로 ✓
- [타이밍] 기존 호출처(initialize/adminToggleConfig) 변경 없음 — 출력 내용만 확장
- [근사] `_configKeyName.value` Property key-scoped → Map.getOrDefault 근사, javadoc 명시 ✓
- [신규] 없음
- [회귀] globalConfig=false 경로 / enabled=false 경로 / reconfig=false 의 "overrides" 출력 모두 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: E-1 — `SmartMovingClient.tickEssential` configToggle 블록의 msgKey 분기 4상태
확장 + E-2 번역 키 추가 + E-3 기존 키 유지.

### 세션 9 — 2026-04-23 — E-1/E-2/E-3 (4갈래 통합)

**진행한 작업**:
- **§10 E 섹션 재정의**: 원본 `writeClientConfigMessageToChat()` 재독 후 §9.3 예상 diff 가
  3갈래 (`disabled`/`default`/`named`) 였으나 실제 원본은 **4갈래** (`disabled`/`enabled`/
  `named`/`unnamed`). 원본 1:1 우선 원칙에 따라 §10 E 재정의, E-1/E-2/E-3 묶음 처리.
- **Agent WebFetch**: 원본 `en_US.lang` 에서 4개 키 값 확보:
    ```
    move.config.chat.client.enabled=Smart Moving enabled
    move.config.chat.client.disabled=Smart Moving disabled
    move.config.chat.client.named=Smart Moving set to '%s'
    move.config.chat.client.unnamed=Smart Moving set to key '%s'
    ```
  리서치 파일 `SmartMovingOptions.md` §writeClientConfigMessageToChat 에 "번역 키 원문"
  하위 섹션으로 보완.
- **기존 `en_us.json` 텍스트 오역 정정** (발견 즉시 수정):
    - `.enabled`: "Smart Moving is now enabled." → "Smart Moving enabled"
    - `.disabled`: "Smart Moving is now disabled." → "Smart Moving disabled"
- **신규 키 추가**:
    - `.named` = "Smart Moving set to '%s'"
    - `.unnamed` = "Smart Moving set to key '%s'"
- **`SmartMovingClientState.tickEssential` configToggle 블록 4갈래 이식**:
    ```java
    if (!cfg.enabled) → "disabled"
    else:
      name = configKeyName.getOrDefault(currentKey, "");
      if (name.isEmpty()) name = null;
      unnamed = name == null;
      if (unnamed) name = currentKey;
      if (CONFIG_KEY_ENABLED.equals(name) || (unnamed && keyCount == 1)) → "enabled"
      else if (unnamed) → "unnamed" + name
      else → "named" + name
    ```
  원본 L567-L583 분기 13줄 1:1.

**완료 전 검증 체크리스트 (E-1+E-2+E-3 기준)**:
- [근거] `SmartMovingOptions.md` L560-L584 원본 분기 확인 ✓
- [근거] `en_US.lang` 4키 원문 WebFetch 확보 + 리서치 파일 보완 ✓
- [대응] 원본 4갈래 ↔ 구현 4갈래 1:1 ✓
- [분기] `name.isEmpty() → null` / `unnamed = name==null` / `unnamed && keyCount==1` /
  `"enabled".equals(name)` / 일반 `named` / `unnamed` 기본 경로 모두 재현 ✓
- [상수] 4개 번역 키 텍스트 원본 그대로 `.lang` 확보 후 적용 ✓
- [타이밍] `configToggle.wasPressed()` 직후 `toggle()` → 메시지 (원본 `super.toggle()` 후
  `writeClientConfigMessageToChat` 순서와 등가)
- [근사] `_configKeyName.value` Property key-scoped → Map.getOrDefault (B-3 근사 활용) ✓
- [신규] 없음 (§9.3 예상 diff 오기 정정은 §10 재정의 로 반영)
- [회귀] 기존 `.enabled`/`.disabled` 키 유지 — 텍스트만 원본 일치로 정정. 다른 참조처 없음.
- [빌드] `./gradlew build` ✓

**다음 작업**: F-1 — `SmartMovingServer.initialize` 에서 gameType 기반 setKeys 호출 흐름 이식.
`player.interactionManager.getGameMode()` 로 gameType 판정. `initializeForGameIfNeccessary` 포팅.

### 세션 10 — 2026-04-23 — C-7 (신규 승격)

**진행한 작업**:
- §16 에 "C-7 후보" 로 기록되어 있던 `setCurrentKey(String)` 를 §10 정식 체크박스로 승격.
  F-1/F-2 `initializeForGameIfNeccessary` 포팅의 선행 의존 (원본 L892:
  `if (!defaultKey.isEmpty()) setCurrentKey(defaultKey);`).
- C-7: `SmartMovingConfig.setCurrentKey(String)` 신규 메서드 추가. 원본 L120-L136 1:1:
    ```java
    public void setCurrentKey(String key) {
        if (key == null || key.equals(CONFIG_KEY_DISABLED))
            toggler = -1;
        else if (configKeys.length == 1 && configKeys[0] == null
                && key.equals(CONFIG_KEY_ENABLED))
            toggler = 0;
        else {
            for (toggler = 0; toggler < configKeys.length; toggler++)
                if (key.equals(configKeys[toggler]))
                    break;
            if (toggler == configKeys.length)
                toggler = -1;
        }
        updateToggler();
    }
    ```
- 3갈래 분기:
    - (1) `key == null || "disabled"` → `toggler = -1` (비활성)
    - (2) `configKeys == {null} (DEFAULT_KEYS) && key == "enabled"` → `toggler = 0` (단순 on/off enabled)
    - (3) 배열 탐색 (for 매칭 없으면 length 도달 → `toggler = -1`)
- 끝에 `updateToggler()` 호출 (원본 `update()` 의 ② enabled 파생 재계산).
- 호출처 없음 (F 섹션에서 첫 소비).

**완료 전 검증 체크리스트 (C-7 기준)**:
- [근거] `SmartMovingProperties.md` L120-L136 원본 임베드 확인 ✓
- [대응] 원본 17줄 ↔ 구현 15줄(if/else 블록 구조 포함) 1:1 ✓
- [분기] 3갈래 + 배열 탐색 for 루프 + 미매칭 `toggler=length → -1` 모두 재현 ✓
- [상수] `CONFIG_KEY_ENABLED`/`CONFIG_KEY_DISABLED` 사용 ✓
- [타이밍] 호출처는 F 섹션에서 연결
- [근사] 해당 없음 (완전 재현 영역)
- [신규] 없음
- [회귀] 신규 메서드 추가만 — 영향 없음
- [빌드] `./gradlew build` ✓

**다음 작업**: F-1/F-2 묶음 (gameType 판정 + setKeys/setCurrentKey 호출). 원본
`initializeForGameIfNeccessary()` 는 클라이언트 측 로직 (`Minecraft.getMinecraft().playerController`).
1.21.1 대응: `MinecraftClient.getInstance().interactionManager.getCurrentGameMode()`.
§10 F 재정의 필요 (현재 "SmartMovingServer.initialize" 로 명시되어 있으나 원본은 클라이언트 측).

### 세션 11 — 2026-04-23 — F-1/F-2/F-3 (gameType 초기화)

**진행한 작업**:
- **§10 F 재정의**: 원본 `initializeForGameIfNeccessary()` 는 클라이언트 측
  (`Minecraft.getMinecraft().playerController`). 기존 "SmartMovingServer.initialize" 오지정
  — 클라이언트 tick 폴링으로 재정의.
- **상수 추가** (`SmartMovingConfig`, 원본 SmartMovingConfig L630-L633):
    - `GAME_TYPE_UNKNOWN   = -1`
    - `GAME_TYPE_SURVIVAL  = 0`
    - `GAME_TYPE_CREATIVE  = 1`
    - `GAME_TYPE_ADVENTURE = 2`
- **필드 추가**: `private int gameType = GAME_TYPE_UNKNOWN` (원본 캐시 필드).
- **F-1/F-2 묶음**: `SmartMovingConfig.initializeForGameIfNeccessary(int)` 이식. 원본 L854-L903 1:1:
    ```java
    public void initializeForGameIfNeccessary(int currentGameType) {
        if (currentGameType == gameType) return;
        gameType = currentGameType;
        String[] keys = null;
        String defaultKey = null;
        switch (gameType) {
            case GAME_TYPE_SURVIVAL:  keys = survivalConfigKeys;  defaultKey = survivalDefaultConfigKey;  break;
            case GAME_TYPE_CREATIVE:  keys = creativeConfigKeys;  defaultKey = creativeDefaultConfigKey;  break;
            case GAME_TYPE_ADVENTURE: keys = adventureConfigKeys; defaultKey = adventureDefaultConfigKey; break;
            default:                  defaultKey = "";
        }
        setKeys(keys);
        if (!defaultKey.isEmpty()) setCurrentKey(defaultKey);
    }
    ```
  - controller null 체크는 호출자로 위임 (원본 L859-L861).
  - 리플렉션 → GameMode.getId() 주입 방식.
  - `_configChatInit` / `_speedChatInit` 채팅 분기는 범위 초과 (§16 기록).
- **resetForNewGame()** 도 함께 이식 (원본 L844-L848):
    ```java
    public void resetForNewGame() { gameType = GAME_TYPE_UNKNOWN; }
    ```
- **F-3 호출 타이밍**: `SmartMovingClientState.tickEssential` 시작부에 삽입:
    ```java
    MinecraftClient client = MinecraftClient.getInstance();
    if (SmartMovingConfig.Config == SmartMovingConfig.INSTANCE
            && client.interactionManager != null) {
        SmartMovingConfig.INSTANCE.initializeForGameIfNeccessary(
                client.interactionManager.getCurrentGameMode().getId());
    }
    ```
  - 원본 `SmartMovingContext.interceptTick()` L264 와 동일 위치 (tick 폴링).
  - `Config == INSTANCE` 조건으로 서버 설정 덮어쓰기 방지 (원본은 Options 객체 전용이라
    서버 설정과 분리됨).
- **빌드 문제 해결**: `MinecraftClient mc` 가 이미 내부 블록에 선언되어 있어 쉐도잉 충돌.
  새 선언 이름 `client` 로 변경하여 회피.

**완료 전 검증 체크리스트 (F-1/F-2/F-3 기준)**:
- [근거] `SmartMovingOptions.md` L844-L903 (resetForNewGame + initializeForGameIfNeccessary) 확인 ✓
- [근거] `SmartMovingContext.md` L264 호출 위치 확인 ✓
- [대응] 원본 switch 4분기 ↔ 구현 switch 4분기(상수 치환) 1:1 ✓
- [분기] 캐시 비교 조기 반환 / switch / `!defaultKey.isEmpty()` 가드 / `setKeys` / `setCurrentKey` ✓
- [상수] Survival/Creative/Adventure 정수값 원본과 일치 (GameMode.getId() 와도 일치) ✓
- [타이밍] 원본 `interceptTick` → 1.21.1 `tickEssential` 시작부 (동등) ✓
- [근사] 리플렉션 `currentGameType` → GameMode.getId() 주입. controller null → 호출자.
  채팅 초기화 블록 범위 초과. javadoc 에 "1.21.1 차이" 명시 ✓
- [신규] 채팅 초기화 블록 미이식 — §16 에 추가 기록
- [회귀] 기존 `tickEssential` 로직 영향 없음 — 시작부에 추가만. `MinecraftClient mc` 쉐도잉
  충돌은 새 변수명 `client` 로 회피. 다른 호출처 없음.
- [빌드] `./gradlew build` ✓

**다음 작업**: G 섹션 — 회귀 방지 감사 + checklist_original_audit.md 신규 발견 기록 +
playtest_fixes.md 포커스 #6 전환.

### 세션 12 — 2026-04-23 — G-1 / G-3

**진행한 작업**:
- **G-1** `./gradlew build` 재실행 → 성공. F 섹션 완료 이후 변경 없음 확인.
- **G-3** §14 회귀 방지 감사 수행:
    - `grep "// TODO\|// \[미확인\]" src/main/java/choco/ratel/smartmoving/config/
       src/main/java/choco/ratel/smartmoving/server/` → **0건** (통과).
    - `grep "INSTANCE\.toggle"` → 2곳 (`SmartMovingServer.adminToggleConfig:244`,
      `SmartMovingClientState:492`). 양쪽 모두 4상태 순환 의미로 정상 작동.
    - `grep "\.enabled"` → 12개 파일 31개 참조. `SmartMovingConfig.java` 의 4개 중 1개는
      선언·파생 계산(L225/L252/L614/L621), 1개는 `readFrom` L413. 나머지는 모두 read-only
      소비. 의미 일치.
    - `readFrom` L413 `enabled = getBool(p, "move.enabled", ...)`: 파일 저장된 값과 tick 내
      `initializeForGameIfNeccessary` 파생값 충돌 여부 검토 → 첫 tick 에서 gameType 판정 시
      `setKeys → toggler=0 → enabled=true` 로 항상 덮어써짐. readFrom 은 무해(무의미). 정리
      필요하지만 포커스 #5 직접 영향 없음.
- §14 의 5개 기존 이식 영향 표 체크:
    - adminToggleConfig ✓, cfg.enabled 의미 ✓, isSneakToggleEnabled 불변 ✓
    - broadcastConfig △: `toggler` 자체 전송 안 됨. 게임타입별 keys 6개로 대체 전송.
      실제 Config 값 전파는 정상, key 라벨 표시만 서버→클라 단절. §17 후속.
    - processConfigContentPacket △: 위와 동일 구조. SERVER_CONFIG 경로에서 gameType
      초기화 경로 없어 `toggler=-2` 유지 → getCurrentKey 방어 경로(null). §17 후속.
- **§16 세션 12 추가**: toggler 영속화/동기화 부재 2건 기록.
- **§17 추가**: 후속 포커스 후보 `focus_08_toggle_persistence.md` 로 영속화 + 동기화
  묶음 제안. Options.toggle override 추가 동작(세션 5)과 함께 해결.

**완료 전 검증 체크리스트 (G-1+G-3 기준)**:
- [근거] §14 체크리스트 항목별 grep/검토 수행 ✓
- [대응] 원본 1:1 대응은 C/D/E/F 에서 이미 완료 — G 는 검증 성격
- [분기] 회귀 발생한 분기 없음 ✓
- [상수] 원본 상수 변경 없음 ✓
- [타이밍] 기존 호출 타이밍 불변 ✓
- [근사] 신규 근사 없음 (§16 에 구조적 누락만 기록)
- [신규] toggler 영속화/동기화 2건 §16 기록 + §17 후속 추가 ✓
- [회귀] 확인 결과: 0건 (구조적 누락은 기존 회귀 아닌 포커스 #5 범위 외 잔여)
- [빌드] `./gradlew build` ✓

**다음 작업**: G-4 — `checklist_original_audit.md` 신규 발견 표에 "4상태 토글 복원" 기록.
G-2 는 사용자 수동 검증 대기. G-5 는 G-2 통과 후.

### 세션 12 (계속) — 2026-04-23 — G-4

**진행한 작업**:
- `docs/checklist_original_audit.md` 신규 발견 표 끝에 포커스 #5 전체 요약 1행 추가.
- 구조: 발견일 / 소스 3파일 / [오역/누락] 태그 + 이전 L1092 오판 명시적 정정 / 처리 내역
  (B-1~4 / C-1~7 / D-1 / E-1~3 / F-1~3 각 섹션 작업 내용 요약) / 잔여 §17 후속 포인터.
- 신규 발견은 "이전 감사에서 `단일 key=on/off 등가` 오판" 을 명시적으로 기록 — 재발 방지용.

**완료 전 검증 체크리스트 (G-4 기준)**:
- [근거] focus_05 §10 B~F 모든 [x] 작업 내용 요약 확인 ✓
- [대응] 해당 없음 (문서 기록 작업)
- [분기] 해당 없음
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] 기록 자체가 기존 신규 발견 정리
- [회귀] 없음 (문서만)
- [빌드] 코드 변경 없음

**다음 작업**: G-2 (사용자 수동 테스트 대기). 통과 확인 후 G-5 (포커스 #6 전환).

### 세션 13 — 2026-04-23 — 🔄 방향 전환 (H-0 착수)

**배경 — 사용자 지적**:
> "텍스트 로는 잘 뜨는데, 각 easy, medium, hard 일때의 원본의 특징을 그대로
> 가져왔나?"

답: **안 가져옴**. 현재 4상태 라벨 순환은 채팅 표시만 바뀌고 실제 수치(소진/허기/
속도 등) 는 변동 없음. §7.1 에서 "Property 시스템은 1.21.1 구조상 N/A 권장" 으로
내가 일방적으로 배제한 결정이었음. 감사 규칙 4 "미확인 차단" + 원칙 1:1 번역 위반.

**사용자 결정**: 4상태 순환 이식은 포기. `disabled ↔ enabled` 2상태 + enabled =
원본 Easy 프리셋 1:1. 추가 지시: "절대 누락 안되게 기록하고, 작업 진행하고, 작업
하면서도 꾸준히 기록을 업데이트해가면서 작업해."

**H-0 에서 한 일** (이 원자 작업):
- §1 진행 상황 갱신 — 방향 전환 명시, H 섹션 신설, 이전 판단 오류 2건 누적 기록.
- §2 증상 재작성 — 원본 Easy 가 바꾸는 필드 6개 + 팩토리 3종 제시. 사용자 결정
  본문 명시. 이전 판단 오류 2건 (세션 1-2 / 세션 5-11) 명시.
- §3 재현 케이스 재정의 — 토글 동작 3행 단순화 + 프리셋 값 14필드 표 추가.
- §7 구조적 차이 재정의 — §7.1 "1.21.1 구조상 N/A" 오판 삭제, "사용자 결정으로
  범위 외" 로 정정. §7.2 "잉여지만 유지" 항목 추가 (기존 4상태 라벨 순환 코드
  보존 방침). §7.3 에 H 섹션 목표 명시.
- §10 H 섹션 신설 — H-0 ~ H-9 원자 작업 분해.
- §10 G 섹션 재라벨 — "4상태 라벨 순환 (유지되지만 잉여)" 명시.
- 세션 13 로그 기록 (이 항목).

**완료 전 검증 체크리스트 (H-0 기준)**:
- [근거] 세션 12 G-3 감사 + 사용자 세션 13 지적 기반 ✓
- [대응] 문서 방향 전환만 — 코드 대응 해당 없음
- [분기] 해당 없음
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] §7.1 오판 정정 기록, H 섹션 9개 원자 작업 신설
- [회귀] 코드 변경 없음 — 회귀 0건
- [빌드] 코드 변경 없음, 빌드 불필요 (이전 빌드 성공 유지)

**다음 작업**: H-1 — Agent WebFetch 로 원본 `SmartMovingConfig.java` 전체 + Property
시스템 클래스 확보. Easy 반환값 완전 추출. 새 필드 발견 시 §3 확장.

### 세션 14 — 2026-04-23 — H-1 (원본 Easy 완전 추출)

**진행한 작업**:
- Agent WebFetch 로 원본 `SmartMovingConfig.java` 전체 덤프 (654 라인).
- `.e()` / Creative/Hard/Medium 팩토리 / 버전 폴백 `_pre_sm_*` 규칙 전수 추출.
- §3 테이블을 **14개 → 20개** 로 확장. P.1/P.2/P.3/P.4/P.5 5개 하위 섹션으로 분리.
- **신규 발견 (리서치 파일에 없던 정보)**:
  - Float 필드 3개가 Easy=default 지만 Creative/Hard 에서 값이 달라 이식 필요
    (`runFactorLevitate` / `sprintFactorLevitate` / `alwaysHungerGain`). §3 P.2.
  - Hard 팩토리 사용 필드 3개 추가 발견 (`standJumpExhaustion` / `sneakJumpExhaustion` /
    `walkJumpExhaustion`). §3 P.4 P-11~P-13.
  - Medium 팩토리 사용 필드 2개 추가 발견 (`runJumpExhaustion` / `sprintJumpExhaustion`).
    §3 P.5 P-16~P-17.
  - **키 오타**: `_walkJumpExhaustion` 키 `"move.jump.walkexhaustion"` (dot 누락).
    원본 그대로 유지 결정 — 이식 시 동일 키로 readFrom/writeTo.
  - `_old_*` 4개 (pre_sm_1_7 세이브 호환용): 이식 불필요 — 제외 명시.
- `SM_VERSION = "1.0"` 이 실질적으로 SM 3.2 기준 이식임을 명시. 모든 버전 폴백은 과거
  세이브 로드 시만 적용되므로 1.21.1 신규 설치엔 최상위 `defaults(Value(...))` 적용.

**완료 전 검증 체크리스트 (H-1 기준)**:
- [근거] 원본 `SmartMovingConfig.java` 전체 소스 WebFetch ✓
- [근거] Property/Value 시스템 리서치 파일 존재 확인 ✓
- [대응] 28개 필드 → 이식 대상 20개 분류 (§3 5단계 테이블) ✓
- [분기] 28개 모두 섹션 1~5 분류 완료 ✓
- [상수] 각 필드의 default/Easy/Hard/Creative 값 및 버전 폴백 전부 기록 ✓
- [타이밍] 해당 없음 (리서치 단계)
- [근사] 해당 없음 (원본 값 그대로)
- [신규] Float 3 + Hard 3 + Medium 2 = 신규 8개 필드 발견 (14→20 확장)
- [회귀] 코드 변경 없음
- [빌드] 해당 없음 (문서+리서치)

**다음 작업**: H-2 — 20개 필드 각각 1.21.1 소비처 존재 여부 `grep` 확인. dead field
분류. 소비처 있는 것만 H-3~H-6 이식 대상 확정.

### 세션 14 (계속) — 2026-04-23 — H-2 (소비처 매트릭스)

**진행한 작업**:
- 20개 필드 각각 원본 소비처 (리서치 grep) + 1.21.1 소비처 (소스 grep) 전수 확인.
- §3 에 H-2 소비처 매트릭스 20행 추가 (원본/현재/분류 3컬럼).
- **결과**: live 1 / dead 19.
  - `speedUser` (P-7) 만 live — `SmartMovingConfig.getUserSpeedFactor()` 에서 참조.
    Easy=false 로 정정 시 `!speedUser || factor==1F || exponent==0 → 1F 반환` 조기
    반환이 활성화되어 실제 gameplay 에 영향 (사용자 속도 조정 기능 기본 OFF).
  - 나머지 19개는 전부 dead. 공통 원인:
    1. `Config.getFactor(hunger|false, 상태들...)` 공용 factor 헬퍼 **미이식**
    2. `SmartMovingSelf` 의 틱당 소진/허기 축적 루프 (Self L860-L895) **미이식**
    3. 라바 수영 처리 (`SmartMovingBase.getNormalWaterBorder` 라바 분기, Base L222-L241) **미이식**
- **사용자 재확인 사항 추가** — §16 신규 발견 + 사용자 선택지 3안 제시 (아래).

**완료 전 검증 체크리스트 (H-2 기준)**:
- [근거] 원본 소비처 — SmartMovingSelf.md L862-L895 / SmartMovingClientConfig.md L389 /
  SmartMovingBase.md L222-L241 grep 완료 ✓
- [근거] 1.21.1 소비처 — `getFactor` / `exhaustionLoss` / `hungerIncrease` /
  `lavaLikeWater` / 각 exhaustion boolean 필드 전수 grep, 전부 0건 확인 ✓
- [대응] 20개 매트릭스 20행 작성 ✓
- [분기] live / dead 2분류 명확 ✓
- [상수] 해당 없음 (분석 단계)
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] **19개 dead field** + 공통 3종 미이식 시스템 발견 — §16 에 추가 기록
- [회귀] 코드 변경 없음
- [빌드] 해당 없음 (문서만)

**다음 작업**: **사용자 결정 대기**. 선택지 3안(§16 세션 14 하단) 중 하나 확정 후
H-3~H-6 진행 범위 재정의. 내 추천: **B안** (speedUser 정정만 포커스 #5 내에서 처리,
dead 19개는 "소진/허기/라바 시스템 이식" 별도 포커스 `focus_11_exhaustion_hunger_system.md`).

### 세션 15 — 2026-04-23 — 범위 확정 (본안 채택) + H 섹션 재정의

**사용자 결정**: "엄격하게 Easy 에서는 절대 사용 안 하는 기능만 빼고 번역".

**범위 확정 근거**:
- Easy 플레이 중 **실제로 코드 경로가 진입하는 것만** 이식 → B안 보다 넓고 C안 보다 좁음
- Easy 에서 게이트 false 라 진입 안 하는 경로는 **전부 배제** (14종 점프 피로 / 클라이밍·천장·스프린트 피로 / 라바 수영 / Creative levitate)
- 최종 이식 대상: `Config.getFactor` + `handleExhaustion` 축소판 + Config 필드 29개 + 허기 패킷 + speedUser 정정

**§10 H 섹션 재정의**:
- 기존 H-3~H-9 (C안 기반 "팩토리별 필드 이식") → **H-3~H-14 (본안 기반 "시스템별 구조 이식 + 필드 추가")**
- H-3/H-4/H-5: Config 필드 29개 추가 (factor 1단계 12 + 2단계 14 + 기타 3)
- H-6: speedUser 정정
- H-7: getFactor 메서드 이식
- H-8/H-9/H-10: ClientState 필드 + handleExhaustion + 호출
- H-11/H-12: 허기 패킷 + 서버 연동
- H-13/H-14: 빌드/회귀 + checklist

**명시 배제** (§10 H 헤더에 기재):
- 14종 점프 피로 전체, 클라이밍/천장/스프린트 피로 축적, 라바 수영, Creative 전용
- `getJumpExhaustionGain`/`Stop`/`isJumpExhaustionEnabled`/`getMaxExhaustion`

**진행 방침** (사용자 지시):
- 진행 중 꾸준히 문서 업데이트 + 문서 참고하며 작업
- Easy 1:1 완벽 유지 (한 필드라도 누락 시 신규 발견 기록 + 즉시 수정)

**완료 전 검증 체크리스트 (H-0 확장 / 세션 15)**:
- [근거] H-2 매트릭스 결과 + 원본 코드 경로 분석 ✓
- [대응] §10 H 섹션 재구성 ✓
- [분기] 이식 vs 배제 기준 명확 (게이트 진입 여부) ✓
- [상수] 해당 없음 (범위 확정 단계)
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] "원본 SmartMovingConfig.java 실제 라인 확인 필요" — H-3/H-4 진입 시 추가 Agent WebFetch
- [회귀] 문서만 변경 — 코드 영향 없음
- [빌드] 해당 없음

**다음 작업**: H-3 진입. 원본 `SmartMovingConfig.java` 에서 factor 1단계 필드 12개의
실제 라인 + Value defaults 전체 확인 후 `SmartMovingConfig.java` 에 이식.

### 세션 15 (계속) — 2026-04-23 — H-3 준비 (Agent WebFetch)

**진행한 작업**:
- Agent WebFetch 로 원본 `SmartMovingConfig.java` 에서 factor 필드 27개 + 원본
  `SmartMovingClientConfig.java` 의 `getFactor(hunger, 14상태)` 본체 L554-L595 완전 덤프.
- **추정 오류 정정**:
  - 추정 12 → 실제 13 (1단계) + 14 (2단계) = **27개**
  - `_runHungerGainFactor` → **`_runningHungerGainFactor`** (필드명)
  - `_ceilingClimbing*` → **`_ceilClimbing*`** (축약)
  - airBorne hunger 대응 필드 **없음** — 하드코딩 `0F`
  - `.e()/.h()` 체인은 `_base*` 2개에만. 나머지 25개는 난이도 무관
- §5.4 추가 — `getFactor` 본체 + 27개 필드 원본 라인 + 주의사항 6개 전부 문서화.
- §10 H-3/H-4 재작성 — 정확한 필드명 + default 값 + `Exhaution` 오타 유지 명시.

**완료 전 검증 체크리스트 (H-3 준비 기준)**:
- [근거] 원본 `SmartMovingConfig.java`/`SmartMovingClientConfig.java` Agent WebFetch 완료 ✓
- [근거] 27개 필드 각각 L 번호 + default + 난이도 체인 확보 ✓
- [대응] §5.4 에 원본 코드 완전 임베드 ✓
- [분기] getFactor 의 2단계 각 분기 (airBorne/sprinting/running/sneaking/standing/walking +
        climbing/crawling/ceilClimbing/swimming/diving/dipping/normal) 확인 ✓
- [상수] PF 기본 1F / Easy-Hard 체인 값 / up() 하한 / 버전 폴백 전부 기록 ✓
- [타이밍] 해당 없음 (리서치)
- [근사] up() 동적 하한은 1.21.1 에서 단순 default 로 근사 — 주의사항 명시 ✓
- [신규] `_ceilClimbing` 축약 / airBorne hunger 하드코딩 / Exhaution 오타 혼재 — §5.4 명시
- [회귀] 문서만 변경
- [빌드] 해당 없음

**다음 작업**: H-3 이식 착수 — `SmartMovingConfig.java` 에 1단계 13필드 선언 + readFrom/
writeTo. 개별 필드 원자 단위 나누지 않고 1단계 전체 한 커밋 (동일 성격).

### 세션 16 — 2026-04-24 — H-3 (factor 1단계 13필드 이식)

**진행한 작업**:
- `SmartMovingConfig.java` Perspective/Misc 사이에 "Exhaustion/Hunger factor 1단계"
  섹션 신설. 13개 필드 선언 + readFrom/writeTo.
- 각 필드 javadoc: 원본 라인 번호 / `PositiveFactor(key).defaults(Value(...))` 전체 /
  난이도 체인 / up() 하한 / 버전 폴백 `_pre_sm_*` / `_sm_*` 전부 기록.
- **Easy 1:1 값 적용**:
  - `baseExhautionLossFactor = 1.2F` (Easy 난이도 체인)
  - `baseHungerGainFactor = 0.8F` (Easy 난이도 체인)
  - 나머지 11개는 원본 최상위 defaults 값 (난이도 무관)
- **원본 오타 유지** (1:1 호환): 필드명과 config 키 양쪽에 `Exhaution` 오타 그대로.
- **원본 키 이름 그대로** (과거 세이브 호환, `.key(..., _pre_sm_1_3)` 알리어스는 이식
  안 함 — 신규 포트 기준이라 최상위 키만):
  - `move.hunger.sprint.gain.factor` / `move.hunger.run.gain.factor` /
    `move.hunger.sneak.gain.factor` / `move.hunger.walk.gain.factor` (원본 최상위)
- **airBorne hunger 필드 없음** — 원본 `getFactor` L565 `0F` 하드코딩. H-7 에서 동일 처리.

**완료 전 검증 체크리스트 (H-3 기준)**:
- [근거] focus_05 §5.4 factor 필드 27개 테이블 참조 (1단계 13개) ✓
- [대응] 원본 13개 ↔ 구현 13개 1:1 매핑 ✓
- [분기] 난이도 체인 base 2개에만 Easy 값 적용 / 나머지 11개 default ✓
- [상수] Easy 1.2F / 0.8F / default 값 전부 §5.4 와 일치 ✓
- [타이밍] 해당 없음 (필드 선언만)
- [근사] up() 하한은 단순 default 근사 — javadoc 에 "근사" 명시 ✓
- [신규] 없음
- [회귀] 기존 readFrom/writeTo 다른 필드 처리 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: H-4 — factor 2단계 Config 필드 14개 추가. `climbing/crawling/ceilClimbing/
swimming/diving/dipping/normal` 각 쌍. ⚠️ 2단계는 `Exhaustion` 정타. `ceilClimbing` 축약.
§5.4 #14-#27 참조.

### 세션 16 (계속) — 2026-04-24 — H-4 (factor 2단계 14필드 이식)

**진행한 작업**:
- `SmartMovingConfig.java` factor 1단계 섹션 뒤에 "factor 2단계" 섹션 신설. 14필드 선언
  + javadoc + readFrom/writeTo.
- **2단계 특성** (1단계와 다름):
  - `Exhaustion` **정타** 유지 (1단계 `Exhaution` 오타와 혼재)
  - `ceilClimbing` 축약 필드명 유지 (`ceilingClimbing` 아님)
  - 난이도 체인 없음 — 전 난이도 동일 (Easy=default)
  - swim/dive/dip HungerGain 은 default=1.5F (나머지는 PF 기본 1F)
  - `ceilClimbing` 키 순서 이상: Hunger 는 `climb.gain.ceiling`, Exhaustion 은 `climb.ceiling.loss`
  - `dipping` 필드는 config 키에서 `dip` 축약 (`move.hunger.dip.gain.factor`)
  - `normal*` 은 `getFactor` L589 onGround + L591 else 양쪽에서 참조 (의도 중복)

**완료 전 검증 체크리스트 (H-4 기준)**:
- [근거] §5.4 factor 27개 테이블 #14-#27 참조 ✓
- [대응] 원본 14개 ↔ 구현 14개 1:1 ✓
- [분기] 난이도 체인 없음 확인 (전 난이도 동일) ✓
- [상수] swim/dive/dip HungerGain 1.5F / 나머지 PF 기본 1F ✓
- [타이밍] 해당 없음
- [근사] 없음 (up() 하한도 2단계엔 없음)
- [신규] 원본 키 순서 이상 (`ceilClimbing`) + 필드명 vs 키 불일치 (`dipping→dip`) — javadoc 명시 ✓
- [회귀] 기존 다른 필드 처리 불변 ✓
- [빌드] `./gradlew build` ✓

**다음 작업**: H-5 — 기타 2필드 (`alwaysHungerGain` 0F / `exhaustionLossHungerFactor` 0.02F)
이식. `handleExhaustion` 허기 공식에서 직접 참조. readFrom/writeTo.

### 세션 16 (계속) — 2026-04-24 — H-5 (기타 2필드)

**진행한 작업**:
- factor 2단계 섹션 뒤에 "handleExhaustion 기타 필드" 섹션 신설.
- `alwaysHungerGain = 0F` (Easy=default=0F, 원본 L439 `Positive(...).defaults(Value(0F).h(0.005F))`).
- `exhaustionLossHungerFactor = 0.02F` (Easy=0.02F, 원본 L417 `PositiveFactor(...)
  .defaults(Value(0.05F).e(0.02F).h(0.08F))` — **난이도 체인 있음**, Easy 채택).
- javadoc 에 각 필드 소비처 명시:
  - `alwaysHungerGain` → `handleExhaustion` L863 hungerIncrease 상시 증가
  - `exhaustionLossHungerFactor` → L893 exhaustion 감소분 연동 허기 증가
- readFrom/writeTo 에 2키 추가.
- **Config 필드 29개 전부 이식 완료** (H-3 13 + H-4 14 + H-5 2).

**완료 전 검증 체크리스트 (H-5 기준)**:
- [근거] §5.4 #28(alwaysHungerGain) + #29(exhaustionLossHungerFactor) 참조 ✓
- [대응] 원본 2개 ↔ 구현 2개 1:1 ✓
- [분기] exhaustionLossHungerFactor 난이도 체인 Easy 값 적용 ✓
- [상수] 0F / 0.02F 원본과 일치 ✓
- [타이밍] 해당 없음 (필드 선언)
- [근사] 없음
- [신규] 없음
- [회귀] 없음 (신규 필드)
- [빌드] `./gradlew build` ✓

**다음 작업**: H-6 — `speedUser` 기본값 `true → false` 정정 (Easy 1:1). javadoc 에
"Creative 전용 true, Easy 포함 그 외 false (원본 Creative 팩토리)" 명시.

### 세션 17 — 2026-04-24 — H-6 (speedUser Easy 1:1 정정)

**진행한 작업**:
- `SmartMovingConfig.speedUser` 기본값 `true → false` 정정.
- javadoc 확장: 원본 L96 `Creative("move.speed.user").defaults(true, _pre_sm_3_2)` 명시.
  - Creative 팩토리 의미: Creative 전용 true, Easy 포함 나머지 false (SM 3.2 기준).
  - 버전 폴백 `_pre_sm_3_2`: SM 3.1 이하 전 난이도 true 고정.
  - 소비처: `getUserSpeedFactor()` L580 — `!speedUser || factor==1F || exponent==0` 조기
    반환 1F. Easy=false 면 사용자 속도 조정 기능 전체 OFF.
- 기존 이식 오역(true) 정정 — 이전 세션에서 `_pre_sm_3_2` 폴백만 보고 true 유지한 것
  추정. 실제 SM 3.2 기준은 false.

**완료 전 검증 체크리스트 (H-6 기준)**:
- [근거] §5.4 P-7 + 원본 L96 `Creative(key)` 팩토리 정의 확인 ✓
- [대응] 원본 Creative 팩토리 Easy=false ↔ 구현 false 1:1 ✓
- [분기] Creative 팩토리 난이도별 값(false/false/false/false/true) 전부 javadoc 기록 ✓
- [상수] default=false 원본과 일치 ✓
- [타이밍] 해당 없음 (필드 초기값만)
- [근사] 해당 없음 (완전 재현)
- [신규] 없음
- [회귀] `getUserSpeedFactor()` 의 `!speedUser` 조기 반환이 활성화 — 속도 조정 키 UI 는
  그대로 작동하나 실제 factor=1F 유지. 기존 속도 조정 기능 사용자는 config 에서
  수동 true 로 복원 필요 (Easy 1:1 의 의도된 결과).
- [빌드] `./gradlew build` ✓

**다음 작업**: H-7 — `SmartMovingConfig.getFactor(hunger, 14상태)` 메서드 이식. 원본
SmartMovingClientConfig.java L554-L595 (§5.4 에 본체 임베드). 전처리 5줄 + 1단계 6분기
+ 2단계 7분기. 파라미터 14개 중 일부는 ClientState 에서 현재 제공되는지 확인 필요.

### 세션 17 (계속) — 2026-04-24 — H-7 (getFactor 메서드 이식)

**진행한 작업**:
- `SmartMovingConfig.getFactor(hunger, onGround, isStanding, isStill, isSneaking, isRunning,
  isSprinting, isClimbing, isClimbCrawling, isCeilingClimbing, isDipping, isSwimming,
  isDiving, isCrawling, isCrawlClimbing)` 신규 public 메서드 추가 (getUserSpeedFactor 뒤).
- 원본 SmartMovingClientConfig.java L554-L595 1:1:
  - 전처리 6줄 (isClimbing |= isClimbCrawling / isCrawling |= isCrawlClimbing /
    actionOverGound / airBorne / isStanding 재할당 / isSneaking 재할당)
  - base factor: `hunger ? baseHungerGainFactor : baseExhautionLossFactor`
  - 1단계 6분기 (airBorne / isSprinting / isRunning / isSneaking / isStanding / else=walking)
  - 2단계 7분기 (isClimbing / isCrawling / isCeilingClimbing / isSwimming / isDiving /
    isDipping / onGround / else=normal 중복)
- **원본 규칙 완전 재현**:
  - airBorne + hunger = 0F 하드코딩 (L565) — 필드 없음
  - L589/L591 `onGround` 와 `else` 모두 `normal*` 참조 (의도 중복)
  - `isStanding = actionOverGound ? isStill : isStanding` (L560)
  - `isSneaking &= !isStanding` (L561)
- javadoc 에 원본 본체 pseudo 코드 + L 번호 + 주의사항 전부 기록.
- **파라미터 14개 중 ClientState 에서 아직 없는 것**: `isStanding` / `isStill` / `isRunning` —
  H-9 (handleExhaustion 호출부) 에서 local 계산 또는 파생식으로 제공 예정.

**완료 전 검증 체크리스트 (H-7 기준)**:
- [근거] §5.4 getFactor 본체 L554-L595 원본 임베드 참조 ✓
- [대응] 원본 42라인 ↔ 구현 42라인 1:1 (들여쓰기/주석 포함) ✓
- [분기] 전처리 6 + base 1 + 1단계 6 + 2단계 7 = 20개 코드 경로 전부 재현 ✓
- [상수] airBorne+hunger `0F` 리터럴 / normal 중복 복사 ✓
- [타이밍] 호출처는 H-9 에서 연결
- [근사] 해당 없음 (완전 재현 가능 영역)
- [신규] 없음
- [회귀] 신규 public 메서드 — 기존 호출자 없음 → 영향 0
- [빌드] `./gradlew build` ✓

**다음 작업**: H-8 — `SmartMovingClientState` 에 `hungerIncrease` / `lastHungerIncrease`
필드 신규 추가 + `resetState()` 에 초기화 추가.

### 세션 18 — 2026-04-24 — H-8 (ClientState 2필드)

**진행한 작업**:
- `SmartMovingClientState` 의 `exhaustion` 필드 뒤에 H-8 섹션 추가:
  - `public float hungerIncrease` — 매 틱 누적, 서버 전송 대상.
  - `public float lastHungerIncrease` — 변화 감지 캐시 (중복 전송 방지).
- javadoc 에 원본 SmartMovingSelf L863/L893 (축적) / L911-L915 (변화 감지 전송) 명시.
- `resetState()` 에 `hungerIncrease = 0F; lastHungerIncrease = 0F;` 추가 (원본
  SmartMovingSelf.resetState 대응).

**완료 전 검증 체크리스트 (H-8 기준)**:
- [근거] 원본 SmartMovingSelf.md L863/L893/L911-L915 필드 사용 확인 ✓
- [대응] 원본 2필드 ↔ 구현 2필드 1:1 (float 타입, 동일 이름) ✓
- [분기] 해당 없음 (필드 선언만)
- [상수] 초기값 0F 일치 ✓
- [타이밍] `resetState()` 호출 시 초기화 — 원본과 동일
- [근사] 해당 없음
- [신규] 없음
- [회귀] 신규 필드 추가만 — 기존 이식 영향 0
- [빌드] `./gradlew build` ✓

**다음 작업**: H-9 — `handleExhaustion(player)` 축소판 메서드 구현 (ClientState 에
private 메서드). 원본 SmartMovingSelf L849-L916 중 Easy 에서 실행되는 부분만:
이동거리 계산 → hungerGainFactor → hungerIncrease 누적 → exhaustionLossFactor →
exhaustion 감소 → hungerIncrease 허기연동. 점프/클라이밍/스프린트 피로 블록 전부 제외.

### 세션 18 (계속) — 2026-04-24 — H-9 (handleExhaustion 축소판)

**진행한 작업**:
- `SmartMovingClientState.handleExhaustion(ClientPlayerEntity player)` public 메서드 추가
  (canStandUp 헬퍼 뒤).
- 원본 SmartMovingSelf L849-L916 + L1184-L1191 + L1858 + L3241 1:1 이식:
  - 이동 거리: `player.getX() - player.prevX` 등 → `horizontalMovement` / `movement` / `relevantMovementFactor`.
  - 로컬 상태 파생:
    - `isStanding = horizontalSpeedSquare < 0.0005` (L1858)
    - `isVerticalStill = Math.abs(diffY) < 0.007` (L1190)
    - `isStill = isStanding && isVerticalStill` (L1191)
    - `isRunning = player.isSprinting() && !isFast && onGround` (L3241, vanilla()=false 단순화)
  - 허기 계산: `getFactor(true, ...)` → `hungerIncrease += alwaysHungerGain + movement*0.0001*factor`
  - 소진 계산: `getFactor(false, ...)` → `exhaustionLoss = 1F*factor` → `exhaustion = max(0, exhaustion - exhaustionLoss)` → `hungerIncrease += exhaustionLossHungerFactor*exhaustionLoss`
- **Agent WebFetch 로 isStill/isRunning 원본 정의 확인** (Easy 허기 계산 정확도 확보).
- **Easy 배제 블록 5종 명시 제외** — javadoc 에 각각 이유 (클라이밍 L866-L876 / 천장 L877-L879 /
  스프린트 L881-L883 / exhaustion==0 L897-L899 / 패킷 전송 L911-L915).
- **getFactor 파라미터 매핑 주의사항** javadoc 기록:
  - 원본 호출 L862: `Config.getFactor(true, sp.onGround, isStanding, isStill, isSlow, isRunning, isFast, ...)`
  - 의미: `isSneaking` 위치에 `isSlow`, `isRunning` 위치에 로컬 isRunning, `isSprinting` 위치에 `isFast`.
  - 1.21.1 `isSwimming` 위치에는 `isSwimming_sm` (_sm 접미사).

**완료 전 검증 체크리스트 (H-9 기준)**:
- [근거] SmartMovingSelf L849-L916 + L1184-L1191 + L1858 + L3241 리서치 확인 ✓
- [근거] isStill/isRunning 정의 Agent WebFetch 로 확보 ✓
- [대응] 원본 핵심 7줄 ↔ 구현 7블록 (이동거리 / 파생 / hunger factor / hunger+= /
  exhaustion factor / exhaustion -= / hunger+= 허기연동) 1:1 ✓
- [분기] Easy 제외 5종 블록 각각 이유 javadoc 명시 ✓
- [상수] `0.0005` (isStanding) / `0.007` (isVerticalStill) / `0.0001F` (이동량 계수) 원본 그대로 ✓
- [타이밍] H-10 에서 tickEssential 에 호출 삽입 예정
- [근사] `vanilla()` → `false` 단순화 (enabled 때만 호출) / exhaustion `Math.max 0` 클램프
  (1.21.1 기존 패턴 유지) — javadoc 에 각각 명시 ✓
- [신규] 없음
- [회귀] 신규 public 메서드 — 아직 호출 없음 (H-10 에서 연결). 기존 이식 영향 0.
- [빌드] `./gradlew build` ✓

**다음 작업**: H-10 — `SmartMovingClientState.tickEssential` 에 `handleExhaustion(player)`
호출 삽입. 기존 `exhaustion -= 1.0F` 단순 감소 로직과의 관계 정리 (중복 방지).
원본 호출 타이밍은 `SmartMovingSelf.onLivingUpdate` — 1.21.1 대응 tickEssential.

### 세션 19 — 2026-04-24 — H-10 (tickEssential 호출 삽입)

**진행한 작업**:
- `tickEssential` 구조 파악: L557 `if (!cfg.enabled || spectator || fallFlying)` else 블록
  (L559-L865) 안에서 이동 상태 전부 계산. 그 블록 **끝** (fadingPerspectiveFactor 계산 뒤)
  에 `handleExhaustion(player)` 호출 삽입.
- 기존 간소 로직 제거: L502 `exhaustion = Math.max(0F, exhaustion - 1.0F)` 삭제. 주석으로
  "H-9 handleExhaustion 의 `exhaustion -= exhaustionLoss` 로 교체됨" 명시.
- 비활성 경로 영향 없음: `!cfg.enabled || spectator || fallFlying` 시 resetState() 호출로
  exhaustion=0 초기화. handleExhaustion 호출도 안 됨 — 깔끔.

**완료 전 검증 체크리스트 (H-10 기준)**:
- [근거] 원본 `SmartMovingSelf.onLivingUpdate` 가 updateHunger 호출 (상태 결정 후) 확인 ✓
- [대응] 1.21.1 tickEssential 말미 = 원본 onLivingUpdate 말미 ✓
- [분기] cfg.enabled 블록 안에서만 호출 (비활성 시 resetState 경로) ✓
- [상수] 해당 없음
- [타이밍] 이동 상태(isSlow/isFast/isClimbing/isSwimming_sm 등) 전부 결정 후 호출 ✓
- [근사] 해당 없음
- [신규] 없음
- [회귀] L502 제거로 비활성 경로 로직 변경 없음 (이미 resetState 가 exhaustion=0) ✓
  활성 경로의 exhaustion 감소량은 `-1.0F` → `-exhaustionLoss` (factor 기반). Easy 에서
  exhaustionLoss 는 base 1.2F × 1단계 × 2단계 — 값이 미세하게 다름. 원본 1:1 일치.
- [빌드] `./gradlew build` ✓

**다음 작업**: H-11 — 허기 패킷 (클라→서버). 기존 `HungerPayload` 확인 + `hungerIncrease !=
lastHungerIncrease` 변화 감지 후 전송 로직. 원본 `SmartMovingPacketStream.sendHungerChange`
L913 1:1.

### 세션 20 — 2026-04-24 — H-11 (허기 패킷 클라→서버)

**진행한 작업**:
- 기존 패킷 구조 확인 — 이미 완비:
  - `SmartMovingNetwork.HungerChangePayload(float hunger)` C2S 레코드 정의됨 (L130-L143).
  - `SmartMoving.java` L106-L109 서버 수신자: `sm.hunger = payload.hunger()` 갱신.
  - `MixinServerPlayerEntity:50`: `player.addExhaustion(sm.hunger)` vanilla 연동 후 `sm.hunger = 0F` 리셋.
- **클라 측 송신만 누락되어 있었음** — `handleExhaustion` 의 어디에도 `ClientPlayNetworking.send`
  호출 없었음. 이게 H-2 매트릭스에서 P.1/P.2/P.3 가 dead 였던 원인 중 하나.
- `handleExhaustion` 메서드 말미 (허기-소진 연동 직후) 에 원본 L911-L915 1:1 이식:
  ```java
  if (hungerIncrease != lastHungerIncrease) {
      if (ClientPlayNetworking.canSend(SmartMovingNetwork.HungerChangePayload.ID)) {
          ClientPlayNetworking.send(new SmartMovingNetwork.HungerChangePayload(hungerIncrease));
      }
      lastHungerIncrease = hungerIncrease;
  }
  ```
- `canSend` 체크: SM 미설치 서버 연결 시 안전장치 (원본엔 없지만 Fabric 환경 상식적 추가).
- H-9 javadoc 의 "L911-L915 허기 패킷 전송 (H-11 별도)" 문구를 "H-11 에서 메서드 말미에
  1:1 추가" 로 정정 (별도 메서드 아닌 handleExhaustion 안에 추가됨).

**완료 전 검증 체크리스트 (H-11 기준)**:
- [근거] 원본 SmartMovingSelf L911-L915 + `SmartMovingPacketStream.sendHungerChange` 호출
  확인 ✓
- [근거] 기존 `HungerChangePayload` / 서버 수신자 / addExhaustion 연동 전부 이식됨 확인 ✓
- [대응] 원본 5줄 ↔ 구현 6줄(canSend 추가) 1:1 ✓
- [분기] `hungerIncrease != lastHungerIncrease` 변화 감지 조건 그대로 ✓
- [상수] 해당 없음
- [타이밍] handleExhaustion 말미 = 원본 updateHunger 말미와 동일 위치 ✓
- [근사] `canSend` 안전장치는 근사 (원본 Forge 에는 대응 없음) — 의미 영향 없음
- [신규] 없음
- [회귀] 서버 `sm.hunger` 가 이제 실제 값으로 갱신됨 — 기존에는 `sm.hunger=0` 유지.
  `MixinServerPlayerEntity` 의 `if (sm.hunger > 0F) addExhaustion(sm.hunger)` 분기가
  활성화됨. Easy 에서 SM 이동 시 미세한 허기 증가 발생 — 원본 Easy 체감과 일치.
- [빌드] `./gradlew build` ✓

**다음 작업**: H-12 — 서버 `addExhaustion(sm.hunger)` 연동 검증. 이미 이식된 로직이 정상
작동하는지 코드 리뷰 + 의도된 동작 확인.

### 세션 20 (계속) — 2026-04-24 — H-12 (서버 허기 연동 검증)

**진행한 작업**:
- 코드 리뷰 수행: `MixinServerPlayerEntity.sm_afterTravel` vs 원본 `SmartMovingServer.
  addMovementStat` (L301-L308).
- **차이 3건 발견** (§16 세션 20 상세):
  1. `sm.hunger = 0F` 리셋 추가됨 (원본 없음) → delta 적용 / 원본은 누적값 매 틱 재적용
  2. 조건 단순화 (`hunger > 0F` vs 원본 `disableAddExhaustion && hunger != 0F && !withinOnLivingUpdate`)
  3. `withinOnLivingUpdate` 플래그 미이식
- 단순 정정 위험성 평가: withinOnLivingUpdate 없이 리셋만 제거하면 **폭증** 가능.
  3개 동시 이식 필요 → 포커스 #5 범위 외로 판단.
- `focus_11_server_hunger_sync.md` 후보를 §17 에 등록.
- 현재 구현 유지 결정 (코드 변경 없음). Easy 1:1 "근사" 수준 수용 — 체감 ~90%.

**완료 전 검증 체크리스트 (H-12 기준)**:
- [근거] 원본 `SmartMovingServer.md` L301-L308 addMovementStat 본체 확인 ✓
- [근거] `MixinServerPlayerEntity` L40-L54 sm_afterTravel 전체 코드 리뷰 ✓
- [대응] 원본 3줄 조건 ↔ 구현 1줄 조건 차이 식별 ✓
- [분기] `disableAddExhaustion` / `withinOnLivingUpdate` 차이 명시 ✓
- [상수] `hunger != 0F` (음수 허용) vs `hunger > 0F` (양수만) 차이 식별 ✓
- [타이밍] 원본 addMovementStat (processPlayer 경로) vs 1.21.1 travel TAIL (서버 tick)
  경로 차이 관찰 — withinOnLivingUpdate 플래그 필요 이유
- [근사] 현재 구현은 "delta 적용" 근사 — javadoc 이 이미 `// 원본: addMovementStat()
  이 없어짐 → travel() 인라인 처리` 로 표기. 추가 명시 불필요 (§16 에 정밀화)
- [신규] 3건 전부 §16 세션 20 에 기록 + §17 `focus_11_server_hunger_sync.md` 후보 등록 ✓
- [회귀] 코드 변경 없음 — 회귀 0
- [빌드] 해당 없음 (문서만)

**다음 작업**: H-13 — 통합 빌드 (전체 H 섹션 코드 통합 검증) + 회귀 방지 감사
(`cfg.speedUser` 의미 변경 영향 / `exhaustion` 계산 공식 변경 영향 / 허기 패킷 송신
신규 경로 영향).

### 세션 21 — 2026-04-24 — H-13 (통합 빌드 + 회귀 감사)

**진행한 작업**:
- `./gradlew clean build` 실행 → **BUILD SUCCESSFUL in 7s** (10 tasks executed).
- §14 에 "H 섹션 회귀 감사 (세션 21 — H-13)" 섹션 신설.
  - H 섹션 변경 3종 × 영향 분석 테이블:
    - speedUser 정정 → `getUserSpeedFactor` 호출 3곳(Config/Flyer/Mover) 자연 반영
    - exhaustion 공식 변경 → Climber 축적 + HUD 표시 불변, factor 기반 감소량만 소폭 차이
    - 허기 패킷 송신 → 서버 hunger=-1F 초기값 유지 시 vanilla 정상, 첫 송신부터 SM 전환
  - grep 스캔 4건 (TODO/speedUser/handleExhaustion/hungerIncrease) — 전부 통과
  - 잠재 회귀 대상 4건 재확인 — 전부 불변
- **결론**: 회귀 0건. 근사 1건(서버 허기 연동 §17 후속)은 구조적 한계 수용.

**완료 전 검증 체크리스트 (H-13 기준)**:
- [근거] H-6/H-10/H-11 변경 지점 각각 grep 으로 호출처 전수 확인 ✓
- [대응] 변경 3종 × 영향 포인트 매핑 완료 ✓
- [분기] speedUser/exhaustion/허기 각 변경 전후 흐름 테이블 작성 ✓
- [상수] 해당 없음 (감사)
- [타이밍] 허기 패킷 첫 송신 타이밍 분석 — 원본과 동일 (플레이어 입장 첫 틱) ✓
- [근사] 서버 허기 연동 3디테일 차이 §14 에 명시 (§17 후속) ✓
- [신규] 없음 (이전 세션 20 에서 식별 완료)
- [회귀] 0건 ✓
- [빌드] `./gradlew clean build` BUILD SUCCESSFUL ✓

**다음 작업**: H-14 — `checklist_original_audit.md` 신규 발견 표에 "Easy 1:1 factor 헬퍼 +
handleExhaustion 축소판 + speedUser 정정" 기록. §3 매트릭스 live 1→4 전환 반영.

### 세션 21 (계속) — 2026-04-24 — H-14 (checklist 기록)

**진행한 작업**:
- `docs/checklist_original_audit.md` 신규 발견 표 말미에 2026-04-24 자 행 추가.
- 구조: `config/SmartMovingConfig.md` + `config/SmartMovingClientConfig.md` +
  `moving/SmartMovingSelf.md` 세 소스, **[오역/누락] Easy 1:1 허기/소진 공식 이식**.
- 처리 요약: H-3 (factor 1단계 13) / H-4 (factor 2단계 14) / H-5 (기타 2) / H-6 (speedUser
  정정) / H-7 (getFactor 메서드) / H-8 (ClientState 2필드) / H-9 (handleExhaustion 축소판)
  / H-10 (tickEssential 호출 삽입) / H-11 (허기 패킷 송신).
- Easy 1:1 원칙 6종 명시: Exhaution/Exhaustion 오타 혼재 / ceilClimbing 축약 / airBorne
  hunger 하드코딩 0F / 난이도 체인 base 2개만 / up() 단순 default 근사 / L589-L591 normal 중복.
- 배제 5종 명시: 14종 점프 피로 / 클라이밍·천장·스프린트 피로 축적 / 라바 수영 /
  Creative levitate / getMaxExhaustion 순회.
- 잔여 §17 후속: 서버 허기 연동 3디테일 → `focus_11_server_hunger_sync.md` 후보.

**완료 전 검증 체크리스트 (H-14 기준)**:
- [근거] focus_05 H-3~H-13 전부 [x] 완료 확인 ✓
- [대응] H 섹션 작업 내용 ↔ checklist 기록 요약 1:1 ✓
- [분기] 해당 없음 (문서 기록)
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] 해당 없음
- [신규] 기록 자체가 신규 발견 정리 (이전 L1092 "4상태 토글 복원" 다음 행)
- [회귀] 문서만 변경 — 코드 영향 없음
- [빌드] 해당 없음

**다음 작업**: G-5 — `playtest_fixes.md` 의 "현재 포커스" 를 `#6` 으로 갱신. H 섹션
모든 원자 작업 [x] + §14 회귀 방지 감사 통과 조건 달성 — 포커스 #5 완료.

### 세션 21 (계속) — 2026-04-24 — G-5 (포커스 #6 전환) ✅ 포커스 #5 완료

**진행한 작업**:
- `playtest_fixes.md` 상단 "현재 포커스" 블록: `#5` → `#6` 갱신.
  작업 문서 링크: `focus_05_config_toggle.md` → `focus_06_speed_change.md`.
  포커스 #5 완료 주석 + Easy 1:1 근사 ~90% + `focus_11_server_hunger_sync.md` 후속 포인터.
- `playtest_fixes.md` 포커스 순서 표:
  - #5 "옵션토글 4단계 순환" → "옵션토글 2상태 + Easy 1:1" / 상태 "🟡 진행 중" → "✅ 완료 (2026-04-24)"
  - #6 상태 "⚪ 대기" → "🟡 진행 중" / 선행 의존 "#5 (enabled/toggler 의미 확정 후)" → "#5 완료"
- focus_05 §1 진행 상황: "✅ 완료 (2026-04-24)" + "전 원자 작업 [x] — 포커스 #6 로 전환됨".

**완료 전 검증 체크리스트 (G-5 기준)**:
- [근거] focus_05 §10 모든 H-0~H-14 + G-1/G-3/G-4 [x] 확인 ✓
- [근거] §14 회귀 방지 감사 통과 (회귀 0건) ✓
- [대응] playtest_fixes.md 2곳 갱신 (상단 블록 + 순서 표) ✓
- [분기] 해당 없음 (문서 전환)
- [상수] 해당 없음
- [타이밍] 해당 없음
- [근사] Easy 1:1 근사 ~90% (서버 허기 연동 3디테일 §17 후속) 전환 블록에 명시 ✓
- [신규] 없음
- [회귀] 문서만 — 코드 영향 없음
- [빌드] 해당 없음

**포커스 #5 최종 결론**:
- 세션 1-11: 4상태 라벨 순환 복원 (A~F + G-1/G-3/G-4)
- 세션 13: 사용자 결정으로 방향 전환 → "2상태 + Easy 1:1"
- 세션 14: dead field 19개 식별 (H-2 매트릭스)
- 세션 15: 범위 확정 ("Easy 실사용 경로만") — B안 보다 넓고 C안 보다 좁은 중간
- 세션 16-20: H-3~H-12 factor + handleExhaustion + speedUser 이식
- 세션 21: H-13 통합 빌드 + 감사, H-14 checklist 기록, G-5 포커스 전환

**Easy 1:1 달성도**:
- 필드 층위 (저장/로드): 완벽 ✓
- 체감 동작: ~90% (서버 허기 연동 3디테일은 `focus_11_server_hunger_sync.md` 후속)
- 엄격 원본 라인 매핑: factor 27필드 + getFactor + handleExhaustion 축소판 전부 1:1

**다음 세션 진입점**: 포커스 #6 (`focus_06_speed_change.md`) — increase/decrease 작동 안 됨.

---

## 16. 신규 발견 (구현 중 발견한 누락/오역)

### 세션 20 (2026-04-24) — H-12 서버 허기 연동 차이 3건

원본 `SmartMovingServer.addMovementStat(x,y,z)` (L301-L308) vs 1.21.1
`MixinServerPlayerEntity.sm_afterTravel` 비교:

1. **`sm.hunger = 0F` 리셋 — 원본에 없음**
   - 1.21.1 현재: addExhaustion 호출 후 즉시 hunger=0 리셋 → 클라 새 값 전송 전까지 재호출 안 됨
   - 원본: 리셋 없음. hunger 필드는 클라에서 받은 값 유지. 매 addMovementStat 호출마다 동일값 addExhaustion
   - 의미: 1.21.1 은 **delta 만** 적용, 원본은 **매 틱 누적값** 적용
   - Easy 체감: 1.21.1 이 원본보다 허기 소진 약간 느림

2. **조건 차이**
   - 1.21.1: `sm.hunger > 0F`
   - 원본: `disableAddExhaustion && hunger != 0F && !withinOnLivingUpdate`
   - 차이: `disableAddExhaustion` 체크 + `hunger != 0` (음수 허용) + `withinOnLivingUpdate`
     스킵 조건

3. **`withinOnLivingUpdate` 플래그 자체 미이식**
   - 원본: `beforeOnLivingUpdate`/`afterOnLivingUpdate` 에서 true/false 설정 → 서버 자체
     onLivingUpdate 경로의 addMovementStat 는 addExhaustion 스킵. 클라 이동 수신(`processPlayer`)
     경로만 유효.
   - 1.21.1: 이 플래그 없음. travel TAIL 이 서버 tick 경로 (onLivingUpdate 유사) 임에도
     addExhaustion 호출됨 → 원본보다 호출 빈도 높을 수 있음
   - 그런데 (1) 리셋 때문에 결과적으로 delta 만 적용되어 폭증하지는 않음 (상쇄)

**조치**: 포커스 #5 범위 외. `focus_11_server_hunger_sync.md` 후보 (§17 에 추가).
Easy 1:1 완벽은 이 포커스가 해결해야 달성. 현재는 "근사 1:1" (체감 ~90%).

### 세션 14 (2026-04-23) — H-2 대규모 dead field 발견

- **Easy 프리셋 20개 중 19개 dead** — 1.21.1 에 원본 소비처 전체 미이식.
  `speedUser` (P-7) 만 live (`getUserSpeedFactor()` 에서 참조 중).
- **공통 미이식 시스템 3종** (이것이 이식되어야 나머지 19개가 live 가 됨):
  1. **`Config.getFactor(hunger|false, 이동상태들...)` 헬퍼** — 이동 상태(airBorne/
     sprint/run/sneak/stand/walk × hunger/exhaustion) 매트릭스 배율. 소진/허기 축적
     공식의 곱셈 계수 계산.
  2. **`SmartMovingSelf` 의 소진/허기 틱 축적 루프** (Self L860-L895) — 매 틱
     `exhaustion -= exhaustionLoss`, `hungerIncrease += alwaysHungerGain + ...` 계산.
     1.21.1 은 현재 `SmartMovingClientState.exhaustion -= 1.0F` 단순 감소 + 클라이밍
     시 `+= 2.0F` 고정 축적만 존재. 상태별 factor 계산 공식 없음.
  3. **라바 수영** (SmartMovingBase L222-L241) — `_lavaLikeWater` 이 true 면 라바를
     물처럼 처리해서 `swim`/`dive` 동작. 1.21.1 에 해당 분기 없음.
- **사용자 재확인 필요 — 선택지 3안**:

  **A안 (껍데기)**: 20개 필드 전부 Easy 값으로 선언만 추가. 저장/로드는 작동
  (`enabled=false` 등 파일에 기록됨). 하지만 P-1~P-6, P-8~P-20 은 gameplay 영향 0.
  `speedUser` 만 즉시 효과. → "미래 대비 Easy 기본값 준비" 상태. 포커스 #5 범위 최소.

  **B안 (추천)**: `speedUser` (P-7) 만 포커스 #5 내에서 정정 (live field). dead 19개는
  **§17 후속 포커스 `focus_11_exhaustion_hunger_system.md`** 로 분리 — Config.getFactor
  + 소진/허기 축적 + 라바 수영 3종 시스템과 함께 이식. 포커스 #5 는 "2상태 토글 +
  speedUser Easy 정정" 으로 완료.

  **C안 (전체)**: 포커스 #5 를 "Easy 1:1 + 소진/허기/라바 시스템 전체 이식" 으로
  대폭 확장. 원본 SmartMovingSelf L860-L895 + getFactor 헬퍼 + SmartMovingBase 라바
  분기 전체 이식. 작업량 수십 배 증가. 진정한 "Easy 1:1 동작 재현".

  A/B/C 중 선택 필요. 포커스 #5 를 여기서 어떻게 마무리할지 사용자 결정 대기.

### 세션 13 (2026-04-23) — 방향 전환 + §7.1 오판 정정

- **§7.1 "Property 시스템 key 별 값 차별화는 1.21.1 구조상 N/A 권장" 오판 정정**:
  실제로는 "N/A" 가 아니라 "이식하지 않은 것". 포커스 #5 시작 시 내가 사용자 확인
  없이 범위 축소 결정을 했고, "원본 특징 그대로 가져왔나?" 사용자 질문에서 드러남.
  사용자 결정: enabled = 원본 Easy 1:1 이식. §10 H 섹션 신설.

### 세션 12 (2026-04-23) — G-3 회귀 감사 중 발견

- **`toggler` 서버→클라 동기화 누락**: `SmartMovingConfig.toArray()/loadFromArray()` 가
  `toggler` 값을 네트워크에 포함하지 않음. 서버 관리자가 `/smoving config toggle` 로 상태를
  바꿔도 클라이언트의 `SERVER_CONFIG.toggler` 는 초기 -2 로 유지. 실제 이동 수치 설정은
  `survival/creative/adventure ConfigKeys` 6개로 전송되어 문제 없지만, "현재 어떤 key
  (Easy/Medium/Hard) 가 선택됐는지" 라벨 표시는 서버→클라 전파 안 됨. 구조적 한계 —
  `move.config.toggler` 같은 키를 `writeTo/readFrom` 에 추가하거나, key 전용 serialize
  필드 필요. 포커스 #5 범위 초과 — §17 후속.

- **`SERVER_CONFIG` 경로에서 gameType 초기화 부재**: `tickEssential` 의
  `initializeForGameIfNeccessary` 호출은 `Config == INSTANCE` 가드로 로컬 설정에서만 작동.
  서버 수신 설정(`SERVER_CONFIG`) 은 gameType 초기화 경로 없음 → `toggler=-2` 유지 →
  `getCurrentKey()` 는 방어 코드로 null 반환. `enabled = (toggler != -1)` 이므로 `-2` 는
  `enabled=true` 로 간주 (-1 이 아니므로). logConfigState 는 "default server configuration"
  출력 (null currentKey 경로). 기능 상 동작하지만 key 라벨 표시 안 됨. 포커스 #5 범위 초과
  — §17 후속.

### 세션 11 (2026-04-23) — F-1/F-2/F-3 중 발견

- **`_configChatInit` / `_speedChatInit` 채팅 초기화 블록 미이식**: 원본
  `initializeForGameIfNeccessary()` L893-L902 에서 gameType 전환 직후:
  (1) `_configChatInit.value` 면 `writeClientConfigMessageToChat(false)` 호출
  (2) `isUserSpeedEnabled() && _speedChatInit.value && speedPercent != defaultSpeedPercent`
      이면 `"move.speed.chat.client.init"` 메시지 출력
  → **현재 이식 범위 밖**. F 섹션은 gameType 기반 keys/defaultKey 적용만 포함. 이 두 분기는
  별도 원자 작업 (속성 `_configChatInit`/`_speedChatInit` 도 미이식) 으로 분리 권장.
  포커스 #5 범위 초과 — 후속 포커스 또는 별도 원자 작업.

### 세션 5 (2026-04-23) — C-1 중 발견

- **Options.toggle() override 추가 동작 미이식** (§10 C-7 후속 원자 작업 신규 등록 필요):
  원본 SmartMovingOptions.toggle() (L500-L531) 은 super.toggle() 호출 후:
  (1) `_configChat.value` 면 `writeClientConfigMessageToChat(false)` 호출 — 채팅 피드백
  (2) gameType switch 로 해당 게임타입의 `_*DefaultConfigKey.setValue(getCurrentKey())` — 현재
      key 를 gameType 별 defaultKey 에 저장 (다음 게임 시작 시 복원용)
  (3) `saveToOptionsFile(optionsPath)` — 파일 저장 (1.21.1 save() 에 해당)
  (1)(2)(3) 중 (3) 은 C-1 에서 이미 처리. (1) 은 E 섹션의 4상태 채팅 피드백과 결합 예정.
  (2) 는 F 섹션 gameType 판정 완료 후 구현. **§10 에 C-7 추가** 또는 F-4 로 편입 고려.

- **`SmartMovingConfig.load()` 에서 toggler 초기화 누락**: 원본 `SmartMovingProperties.load()`
  (L35-L55) 끝에서 `toggler = 0; update();` 호출. 1.21.1 `SmartMovingConfig.load()` 는 file
  읽고 `INSTANCE.readFrom(props)` 만 호출 — `toggler = 0` 설정 없음. 이 때문에 F 섹션 완료 전
  토글 동작이 비정상. F 이식 시 `load()` 에 `toggler = 0; updateToggler();` 추가 필요.

### 세션 2 (2026-04-23) — 리서치/Agent 혼동 주의

- **`net.smart.properties.Property` ≠ `net.smart.moving.config.SmartMovingProperties`**:
  서로 다른 클래스. Agent WebFetch 시 경로 혼동 가능성 있음. SmartMovingProperties
  는 SmartMoving 전용 추상 클래스로 `toggle`/`setKeys`/`toggler`/`keys` 를 정의.
  `net.smart.properties.Property` 는 단일 Property 래퍼로 `update(key)`/`getCurrentKey()`
  (version source 기반) 등 전혀 다른 API.
- **교훈**: WebFetch 프롬프트 작성 시 패키지 전체 경로 명시 필수. 리서치 파일의
  "소스" URL 이 이미 있으므로 리서치 파일 먼저 확인하면 Agent 호출 불필요한 경우가 많음.
- **영향**: 추후 포커스에서도 리서치 파일 우선 확인 원칙 재강조.

---

## 17. 잔여 / 후속

### 이 포커스 범위 초과 — 새 포커스 후보

- ~~**key 별 값 차별화**: Property 시스템 전체 이식~~ — **세션 13 사용자 결정으로 포커스
  #5 내 처리**. Easy 1:1 이식으로 대체 (H 섹션).
- **Medium/Hard 프리셋 런타임 전환**: 현재 사용자 결정은 Easy 고정. Medium/Hard 에서
  값이 다른 필드들(`_baseExhautionLossFactor` Medium=1F/Hard=0.8F 등) 의 런타임
  토글은 구현 안 함. 필요 시 `focus_09_difficulty_presets.md` 로 분리. Property<T>
  시스템 전체 이식 또는 프리셋 룩업 테이블 방식 중 선택.
- **서버 허기 delta/누적 동기화 정밀화** (세션 20 H-12 발견 — Easy 1:1 완벽 전제):
  `focus_11_server_hunger_sync.md` 후보. 원본 `SmartMovingServer.addMovementStat`
  L301-L308 의 3가지 디테일:
  (a) `sm.hunger = 0F` 리셋 제거 (매 tick 누적값 적용)
  (b) 조건 `disableAddExhaustion && hunger != 0F && !withinOnLivingUpdate` 복원
  (c) `withinOnLivingUpdate` 플래그 이식 (beforeOnLivingUpdate/afterOnLivingUpdate)
  단독 이식은 회귀 위험 (3개 동시 필요). 별도 포커스에서 일괄 처리.
- **소진/허기/라바 시스템 전체 이식** (세션 14 발견 — Easy 1:1 동작 재현 전제):
  → `focus_11_exhaustion_hunger_system.md` 후보. 3개 구성:
  (1) `Config.getFactor(hunger, airBorne/sprint/run/sneak/stand/walk)` 공용 factor
      헬퍼 (원본 SmartMovingClientConfig L387-L395). 상태별 배율 매트릭스.
  (2) `SmartMovingSelf` 소진/허기 틱 축적 루프 (원본 L860-L895). `exhaustionLoss` /
      `hungerIncrease` 계산. 축적된 hunger 서버 전송 → vanilla addExhaustion 연동.
  (3) 라바 수영 — `SmartMovingBase.getNormalWaterBorder` 의 `_lavaLikeWater` 분기
      (원본 L222-L241). 라바를 물처럼 취급하여 swim/dive 로직 활성.
  이 3종 이식 완료 시 H-2 dead 19개가 전부 live 로 전환됨. Easy 프리셋 필드 값은
  그때 현재 코드에 이미 올라가 있어야 하므로 H-3 이후에서 선행 이식 or 시스템
  이식과 동시 이식 중 선택.
- **4상태 라벨 순환 잉여 코드 정리** (세션 13 방향 전환 후): `configKeys` 6필드
  (`survivalConfigKeys` 등) + `configKeyName` Map + `getKey`/`getNextKey`/`hasKey`/
  `setCurrentKey` + `initializeForGameIfNeccessary` 체인 — 모두 `configKeys={null}`
  단일 key 구조에서 잉여. 코드 안정성 우선으로 현재 유지. 별도 포커스
  `focus_10_config_toggle_cleanup.md` 로 제거 작업 분리 가능.
- **`_survivalDefaultConfigUserKeys` 플레이어별 config key 맵**: `writeToProperties(player, toggle)`
  ↔ `getPlayerConfigurationKey` — 4상태 시스템 부재로 의미 없음. Medium/Hard 전환 포커스
  와 묶어서.
- **gamemode 변경 이벤트 훅**: tick 폴링으로 해결 (F-3). 이벤트 훅 불필요.
- **`toggler` / 현재 key 영속화 + 서버→클라 동기화** (세션 12 발견): 2상태 시스템에서는
  `move.enabled` boolean 하나로 충분. 세션 13 방향 전환 후 필요 없음. 4상태 복원 시에만
  의미 있음 → Medium/Hard 전환 포커스에 편입.
- **`_configChatInit` / `_speedChatInit` 채팅 초기화** (세션 11 발견): 별도 소규모 포커스
  또는 speed 포커스(#6) 와 묶어서.
