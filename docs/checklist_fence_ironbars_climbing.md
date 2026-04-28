# 체크리스트: 울타리 / 철 창살 클라이밍 1:1 수정

리서치: `docs/research_fence_ironbars_climbing.md`

원본 SM 1.7.10 1:1 매핑 우선. 사용자 요구: fence + iron_bars 둘 다 자체 등반 + 위 잡기 작동 + 속도 원본 일치.

---

## Phase 1: 옵션 정렬 (가장 작은 수정으로 #1, #3 선해결) ✅ 완료

- [x] **1-1. `freeFenceClimbing` 기본값 `false` → `true`**
  - 파일: `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java` L494
  - 변경: `public boolean freeFenceClimbing = true;`
  - 근거: 원본 `Unmodified` 기본값 `true` (Properties.java L171-172). `Config.java` L123 `_freeFenceClimbing = Unmodified("move.climb.free.fence")`

- [x] **1-2. `freeClimbingHorizontalSpeedFactor` 필드 추가**
  - 파일: `src/main/java/choco/ratel/smartmoving/config/SmartMovingConfig.java`
  - 추가: `public float freeClimbingHorizontalSpeedFactor = 1.0F;` (PositiveFactor 기본값)
  - 설정 파일 키: `move.climb.free.horizontal.speed.factor`
  - 로드/저장 라인 추가 (기존 freeClimbingUpSpeedFactor 패턴 따라)
  - 근거: 원본 Config.L115 `_freeClimbingHorizontalSpeedFactor = PositiveFactor(...)`. PositiveFactor 기본값 `1F` (Properties.java L185-186)

- [x] **1-3. 빌드 검증** (`./gradlew compileJava` BUILD SUCCESSFUL)

---

## Phase 2: 자체 등반 / 위 잡기 떨어짐 방지 (#4) — 인게임 검증 보류

원본 흐름 정밀 분석 결과:
- iron_bars 위 잡기 → `hasHalfHold()` setHalfGrabType(HalfGrab) → handsClimbing 결정 로직 (Orientation L341-350) 에서 jh_offset 큰 위치 + grabType=HalfGrab → **`HandsClimbing.Sink`** 또는 `TopHold`.
- W 키 안 누름 → wantClimbDown=true → `handsClimbing.ToDown()` 으로 변환 (HandsClimbing L59-64): `TopHold→Sink`, `Sink→Sink`.
- wantClimbDown 분기 (L1028-1053): `BottomHold` 만 HoldMotion. `Sink.IsRelevant()=true` 인데 BottomHold 아님 + 마지막 fallback else → **`SinkDownMotion(0.05D)` 호출**.
- setLandMotions: motionY = (0.05 - 0.08) * 0.98 = -0.0294 → 매 틱 -0.0294 떨어짐.
- **즉 원본도 fence/iron_bars 위 잡고 가만히 있으면 천천히 떨어짐**.
- 일반 블록 grab 시 hold 되는 이유: 사용자가 벽 옆에서 grab → `jh_offset < _handClimbingHoldGap(=0.06)` → `HandsClimbing.BottomHold` → ToDown 변환 안됨 → wantClimbDown 분기에서 BottomHold + !feet.IsIndependentlyRelevant → **HoldMotion 적용 → 안 떨어짐**.
- 사용자 보고 #4 의 "일반 블록처럼 hold" 가정은 위 잡기 (jh_offset 큼) 와 일반 grab (jh_offset 작음) 의 매핑 차이로 원본도 다르게 동작.

→ **Phase 2 추가 코드 변경 없음**. 인게임 테스트로 우리 떨어지는 속도가 원본과 동일한지 검증.

원본 호출 흐름 재대조:
```
travel → onClimbable=false (free climb 경로)
       → wantFreeClimb=true
       → handleClimbing
           → seekClimbGap → handsClimbing/feetClimbing 결정
           → wantClimbUp 분기 (L631+):
               TopHold/BaseHold → setShouldClimbSpeed(HoldMotion)
           → wantClimbDown 분기 (L691+):
               BottomHold + !feet.IsIndependentlyRelevant → setShouldClimbSpeed(HoldMotion)
       → setOnlyShouldClimbSpeed → relevant 가드 → motionY 적용
```

- [ ] **2-1. iron_bars 위 잡기 시 실제 handsClimbing 값 확인**
  - 인게임 디버그 로그 또는 정적 분석으로 hasHalfHold → setHalfGrabType(HalfGrab) 결과가 어떤 HandsClimbing enum 으로 매핑되는지
  - 예상: `HandsClimbing.TopHold` (위 잡기 매달림) 또는 `HandsClimbing.MiddleGrab`
  - 만약 `handsClimbing=TopHold + feetClimbing=None` 인데 `wantClimbUp` 분기 안에만 TopHold 처리가 있다면, **사용자가 W 키 안 누른 상태**(forward=0 → wantClimbUp=false, wantClimbDown=true) 에서 TopHold 처리 누락

- [ ] **2-2. `wantClimbDown` 분기에 TopHold/HalfHold 보강 검토**
  - 파일: `src/client/java/choco/ratel/smartmoving/client/SmartMovingClimber.java` L691+
  - 원본 L1028-1053 정밀 재인용 후 우리 코드와 라인 단위 대조
  - **주의**: 원본은 wantClimbDown 분기에 TopHold 처리 없음. 대신 BottomHold 처리만.
  - 따라서 사용자가 W 키 누른 채 위 잡기 → `wantClimbUp=true` → TopHold 정상. W 키 안 누르면 `wantClimbDown=true` → 떨어지는 게 원본 동작 가능성
  - **결론 보류** — 인게임 테스트 후 결정

- [ ] **2-3. `MixinLivingEntityClient.travel` 의 `fallDistance=0` 리셋 조건 확장**
  - 파일: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` L307-315
  - 현재: `if (onClimbable) player.fallDistance = 0;`
  - 변경: `if (onClimbable || sm.isClimbing) player.fallDistance = 0;`
  - 근거: free climb 경로에서도 클라이밍 중에는 fallDistance 리셋 필요

- [ ] **2-4. `MixinLivingEntityClient.travel` 의 horizontal/vertical clamp 조건 재검토**
  - L292-302 horizontal clamp `if (onClimbable)` → `sm.isClimbing` 시에도 적용? 원본 대조 필요
  - L307-315 vertical clamp `if (onClimbable)` 와 동일 검토
  - 원본은 horizontal clamp 없음. 우리는 vanilla applyClimbingSpeed 의 0.15 효과를 그대로 두기 위한 듯
  - **단순 수정**: free climb 시 0.15 horizontal clamp 적용 안 함. 원본 1:1.

---

## Phase 3: 속도 1:1 (#2, #5) ✅ 핵심 수정 완료

- [x] **3-1. 원본 horizontal 속도 식 재구현** ✅
  - `SmartMovingMover.getNonSlowInputSpeedFactor` 에 `if (sm.isClimbing) speedFactor *= cfg.freeClimbingHorizontalSpeedFactor;` 추가.
  - 원본 가드 `if(moveStrafing != 0F || moveForward != 0F)` 는 movementInput 매개로만 motion 영향 → 입력 0 시 결과 동일이라 생략.
  - 적용 후 빌드 통과.

- [ ] **3-2. `MixinLivingEntityClient.L294-302` horizontal clamp 0.15 제거 또는 가드 추가**
  - 원본은 horizontal clamp 0.15 없음. vanilla 의 climbing speed 처리에 의존하나 우리는 mixin 으로 차단
  - 원본 1:1: clamp 제거. 단, `onClimbable` (사다리/덩굴) 만은 유지 검토 (vanilla applyClimbingSpeed 시뮬레이션)
  - **결정 보류** — 원본 SmartMovingSelf 의 ladder/vine 시 horizontal 처리 별도 조사 필요

- [ ] **3-3. `climbRawSpeed * climbSpeedFactor` 계산식 재검증**
  - 우리 코드 L245-290 의 식이 원본 `getCombinedSpeedFactor() * _freeClimbingHorizontalSpeedFactor` 와 일치하는지
  - 차이가 있으면 1:1 정렬

- [ ] **3-4. 빌드 + 인게임 속도 측정**
  - O 키로 _speedUserExponent 기본 0 (Speed: 100%) 상태에서 측정
  - 원본 1.7.10 (vanilla 모드 비활성) 와 동일 속도인지 비교
  - 측정 항목: (a) iron_bars 안 위로 등반, (b) iron_bars 위 잡고 횡이동, (c) fence 안 등반 (1-1 적용 후), (d) fence 위 잡고 횡이동

---

## Phase 4: 통합 인게임 테스트

- [ ] **4-1. fence 자체 등반** — w 키로 위로 올라가는지
- [ ] **4-2. fence 위 잡기** — 위에 fence 만 있고 본인 공중 → 잡고 매달리는지
- [ ] **4-3. fence 위 잡고 떨어지지 않는지** — 입력 없이 5초 유지
- [ ] **4-4. fence 위 잡고 횡이동 속도** — 일반 블록 grab hold 와 동일 속도
- [ ] **4-5. iron_bars 자체 등반** — 위로 올라가는 속도가 원본과 동일
- [ ] **4-6. iron_bars 위 잡기** — 매달리고 떨어지지 않음
- [ ] **4-7. iron_bars 위 잡고 횡이동 속도** — 원본과 동일
- [ ] **4-8. 일반 블록(돌 등) grab hold 와 fence/iron_bars 위 잡기 동작이 일치하는지**

---

## Phase 5: 커밋

- [ ] **5-1. Phase 1 단독 커밋** (옵션 기본값 + 필드 추가)
- [ ] **5-2. Phase 2 단독 커밋** (떨어짐 방지)
- [ ] **5-3. Phase 3 단독 커밋** (속도 1:1)

커밋 메시지 컨벤션은 CLAUDE.md 참조.
