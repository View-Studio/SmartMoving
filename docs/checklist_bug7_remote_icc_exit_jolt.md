# BUG-7 잔존 덜컹 작업 체크리스트

> 리서치 파일: `docs/research_bug7_remote_icc_exit_jolt.md` 참조.

## Phase 0 — 다음 세션 시작 시 코드 상태 검증

- [ ] `git diff src/client/java/choco/ratel/smartmoving/client/SmartMovingClient.java` 로 v25.7.10 fix (= lastRenderY/prevY 동기화) 적용 확인.
- [ ] `git diff src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityClient.java` 로 watchfix threshold 적용 확인.
- [ ] `git diff src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` 로 `smIccExitYThresh`, `smIccExitIsLadder` field 확인.
- [ ] `./gradlew compileJava compileClientJava` 빌드 통과 확인.

## Phase 1 — 잔존 "약간 덜컹" 가능 root 검증

### 1-A. pose timing 검증 (= 가능 root 2순위)

- [ ] dump 의 4 EXIT 시점에서 pose 변화 timing 확인.
  - [ ] 1차 ladder (L2665~) pose=STANDING → SWIMMING 시점 측정.
  - [ ] 2차 그랩 (L3294~) pose timing.
  - [ ] 3차 ladder (L3670~) pose timing.
  - [ ] 4차 그랩 (L4002~) pose timing.
- [ ] pose 변화 시 bb height 변화량 측정 (= STANDING 1.8m → SWIMMING 0.6m).
- [ ] render Y 와 bb 위치 관계 mismatch 인지 검증.

### 1-B. X/Z lerp lag catch-up 검증 (= 가능 root 3순위)

- [ ] 4 EXIT 시점에서 entity.x/z 와 svX/svZ 차이 측정.
- [ ] EXIT 후 첫 tick 의 X/Z catch-up motion rate 측정.
- [ ] Y teleport 와 X/Z catch-up 의 동시 발생 여부.

## Phase 2 — fix 옵션 검증

### 2-A. 옵션 B (= pose 강제 + calculateDimensions) 우선

- [ ] SmartMovingClient.java packet handler 안 setPos 후 추가:
  ```java
  if (target.isCrawling) {
      remote.setPose(net.minecraft.entity.EntityPose.SWIMMING);
  }
  ```
- [ ] `remote.calculateDimensions()` 호출 위치 확인 (= 이미 L100, processStatePacket 후).
- [ ] 빌드 + 인게임 테스트.
- [ ] dump 분석 — pose 즉시 변경 + bb height 일관 검증.
- [ ] 사용자 검증 — "덜컹" 변화 보고.

### 2-B. 옵션 A (= 부분 동기화) — 옵션 B 효과 미미 시

- [ ] `partialOffset` 비율 결정 (= 0.5 기본).
- [ ] SmartMovingClient.java 안:
  ```java
  double partialOffset = offset * 0.5;
  remote.lastRenderY = newY - partialOffset;
  remote.prevY = newY - partialOffset;
  ```
- [ ] 빌드 + 인게임 테스트.
- [ ] dump 분석 — 2-step 분할 jump 검증.
- [ ] 사용자 검증.

### 2-C. 옵션 C (= ICC dim refactor) — 옵션 A/B 모두 미흡 시

- [ ] self side ICC dim 매핑 코드 식별 (`MixinEntity.sm_offsetBoundingBoxForFlying` 등).
- [ ] 매핑 변경 안 design (= entity.y -1m down 안 함 + bb 0.5m).
- [ ] 영향 범위 분석 — self 측 인게임 동작.
- [ ] 신중 적용 + 인게임 테스트.

### 2-D. 옵션 D (= 사용자 수용) — 마지막 수단

- [ ] "약간 덜컹" 수용 결정 시 fix 종결 + memory 저장.
- [ ] 다른 시나리오 진행.

## Phase 3 — DBG-7 dump 코드 정리

- [ ] root cause 식별 후 [TEMP DBG-7] 주석 코드 모두 제거.
  - [ ] SmartMovingClient.java 의 [B-FIX BEFORE/AFTER] dump.
  - [ ] MixinPlayerEntityClient.java 의 [B-TICK] dump.
  - [ ] MixinEntityClient.java (전체 새 파일).
  - [ ] MixinPlayerEntityRenderer.java 의 [B-RENDER] dump.
  - [ ] MixinPlayerEntity.java 의 server tick dump.
  - [ ] MixinServerPlayNetworkHandler.java 의 onPlayerMove dump.
  - [ ] SmartMovingServer.java 의 bit34 dump.
  - [ ] smartmoving.mixins.json 의 [TEMP DBG-7] 추가 entries.
- [ ] 빌드 + 정상 동작 확인.

## Phase 4 — 메모리 갱신

- [ ] `project_bug7_remote_icc_exit_complete.md` 메모리 업데이트:
  - [ ] v25.3 → v25.7.10 진화 history.
  - [ ] ladder/그랩 변수 분리 구조.
  - [ ] lastRenderY/prevY 동기화 패턴.
  - [ ] 잔존 BUG (= "약간 덜컹") 또는 정착 선언.
- [ ] 새 feedback memory 추가 (= 적절 시):
  - [ ] `feedback_render_lerp_prev_sync.md` (= setPos 시 lastRenderY/prevY 동기화 패턴).

## Phase 5 — Git commit

- [ ] 변경 파일 정리:
  - [ ] DBG-7 dump 제거 후 핵심 fix 만 남김.
  - [ ] `git status` 로 modified files 확인.
- [ ] commit message:
  ```
  fix(remote-icc-exit): v25.7.10 ladder/그랩 별도 변수 + render lerp prevY 동기화
  
  ladder/그랩 self side 매핑 본질적 차이 → 변수 분리 (offset, threshold).
  setPos +0.84/+1.0m 시 lastRenderY/prevY 동시 set → 1 frame instant teleport
  + 즉시 안정. v25.7.9 의 4-frame lerp jump 차단.
  ```

## 참고

- 사용자 발언 "약간 덜컹" = visual 인지의 trade-off. 완전 차단 어려움.
- 옵션 B 가 가장 작은 변경 + 가장 큰 효과 가능.
- 인게임 테스트 시 multi 환경 + remote rudals 시점 검증 필수.
