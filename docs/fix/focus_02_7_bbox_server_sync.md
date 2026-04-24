# 포커스 #2.7 — BBox / POSE / EyeHeight 서버 동기화 + 원본 heightOffset 완전 재현

> **Phase 1 보강 포커스**. 세션 137 Phase 1 에서 client 측 `getBaseDimensions` /
> `updatePose` Mixin 에 모든 SM 상태별 원본 bbox (0.6 × 0.8 + eyeHeight 0.62F) 적용 완료.
> 본 포커스는 **서버 측 완전 동기화** + **원본 heightOffset 시맨틱 100% 재현**.
>
> **목표**:
> 1. server-side `MixinPlayerEntity.getBaseDimensions` / `updatePose` 가 client 와 **완전 일치**
>    한 dimensions/POSE 반환 → 좁은 통로 통과 / 낙하 데미지 / 질식 판정 **client = server**.
> 2. 1.21.1 Fabric payload packet (`SmartMovingNetwork.StatePayload`) 로 모든 SM 상태 서버
>    전송 + 서버 필드 저장 + Mixin 참조 경로 완성.
> 3. 원본 SM `heightOffset` 시맨틱 (`setHeightOffset(-1F)` → bbox minY+1 / height-1 동치)
>    을 1.21.1 POSE + EntityDimensions 시스템으로 **엄격 1:1** 매핑.
> 4. vanilla POSE 중 SM 과 충돌하는 것만 선별 차단 (elytra/trident/sleeping 등 유지).
>
> **진입 배경**: 세션 137 B-N-standup 재평가에서 원본 heightOffset 시맨틱 재검토 → 1.21.1
> POSE 시스템이 동치 대체지만 **SM 값 (bbox 0.6×1.0 / eyeHeight 0.4)** 가 원본 (0.6×0.8 /
> 0.62) 과 달랐음 → 0.2 블록 차이로 1 블록 통로 통과 실패. Phase 1 client 값 수정 완료.
> Phase 2 는 서버 sync 완성.

---

## 0. 현재 상태 (진입 시점)

### 이식 완료 (Phase 1 — 세션 137)
| 항목 | 위치 | 상태 |
|---|---|---|
| client `sm_getBaseDimensions_client` 모든 SM 상태 → 0.6 × 0.8 | MixinPlayerEntityClient L40-L71 | ✅ |
| client `sm_updatePose_client` POSE 매핑 확장 (8 상태 전수) | MixinPlayerEntityClient L85-L117 | ✅ |
| StatePayload encode/decode (22+ bit 전수) | SmartMovingState.java L40-L114 | ✅ |

### 미이식 / 근사 (이 포커스 범위)
| 항목 | 원인 |
|---|---|
| server `MixinPlayerEntity.getBaseDimensions` 구 로직 (0.6 × 1.0 크롤) | client Phase 1 과 불일치 — 수정 필요 |
| server `MixinPlayerEntity.updatePose` `isSmall` 만 체크 | 확장 상태 (isHeadJumping / isSwimming / isDiving / isFlying / isLevitating / isClimbCrawling) 반영 안 됨 |
| server `SmartMovingServer` 상태 필드 부족 | `isCrawling` / `isSmall` 만 존재. 나머지 8+ 필드 미이식 |
| StatePayload 수신 → server 필드 저장 경로 | server 필드 부족으로 일부만 저장 |
| `isClimbCrawling` / `isFlying` / `isLevitating` 네트워크 전송 | StatePayload 에 없음 — 추가 필요 |
| 원본 heightOffset 렌더링 보정 (`getBrightness` 등 `posY -= heightOffset`) | 1.21.1 에서는 eyeHeight Mixin 으로 대체 — 추가 보완 필요 |

---

## 1. 1:1 번역 룰

원본 `SmartMovingSelf.setHeightOffset(float offset)` (L1694-L1704):
```java
private void setHeightOffset(float offset) {
    resetHeightOffset();
    if (offset == 0F) return;
    heightOffset = offset;
    sp.boundingBox.minY -= heightOffset;    // minY += 1 (offset=-1 시)
    sp.height += heightOffset;               // height -= 1
}
private void resetHeightOffset() {
    sp.boundingBox.minY += heightOffset;    // 원위치 복원
    sp.height -= heightOffset;
    heightOffset = 0F;
}
```

원본 `SmartMovingServerPlayerBase.getEyeHeight()` (L142-L145):
```java
public float getEyeHeight() {
    return player.height - 0.18F;
}
```

→ 원본은 bbox + height + eyeHeight 3 필드를 동시에 관리. 1.21.1 대응:
- bbox + height → `EntityDimensions` (`getBaseDimensions` Mixin 으로 반환)
- eyeHeight → `EntityDimensions.withEyeHeight(float)` (동일 dimensions 내 지정)
- POSE → vanilla POSE 시스템 (`setPose` + 매 tick `updatePose` Mixin 으로 독점)

**client / server 양쪽** 에 동일 로직 적용 시 동치 달성.

### 표면 매핑
| 원본 | 1.21.1 |
|---|---|
| `sp.boundingBox.minY -= -1; sp.height += -1` (setHeightOffset(-1)) | `EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F)` 반환 |
| `sp.boundingBox` 원복 (resetHeightOffset) | SM 상태 false 시 vanilla `getBaseDimensions` 통과 |
| `sp.getEyeHeight() = height - 0.18F` | `withEyeHeight(height - 0.18F)` |
| 서버 측 state 유지 | `SmartMovingServer` 필드 + StatePayload 수신 저장 |
| client → server 전송 | `SmartMovingNetwork.StatePayload` (기존) |

### 금지
- client/server 값 불일치 (desync 근본 원인)
- `isCrawling` / `isSmall` 만 체크 (다른 상태 누락)
- 매 tick `setBoundingBox` 반복 hack (POSE 시스템이 대체)

---

## 2. 원본 상태 → POSE + Dimensions 매핑

### 2.1. 전수 매핑 표
| SM 상태 | 원본 heightOffset | bbox | eyeHeight | 1.21.1 POSE |
|---|---|---|---|---|
| isCrawling | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isClimbCrawling | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isHeadJumping | -1 | 0.6 × 0.8 | 0.62 | SLIDING |
| isSliding | -1 | 0.6 × 0.8 | 0.62 | SLIDING |
| isSwimming | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isDiving | -1 | 0.6 × 0.8 | 0.62 | SWIMMING |
| isFlying (SM) | -1 (진입 엣지) | 0.6 × 0.8 | 0.62 | SLIDING (수영 애니 회피) |
| isLevitating | -1 (진입 엣지) | 0.6 × 0.8 | 0.62 | SLIDING |
| isDipping | 0 | 0.6 × 1.8 | 1.62 | STANDING (vanilla) |
| STANDING | 0 | vanilla | vanilla | vanilla 통과 |
| CROUCHING | 0 | vanilla | vanilla | vanilla 통과 |
| FALL_FLYING (elytra) | - | vanilla 0.6 × 0.6 | vanilla | vanilla 통과 |
| SPIN_ATTACK (trident) | - | vanilla | vanilla | vanilla 통과 |

### 2.2. POSE 독점 차단 원칙
- **차단 대상** (SM 상태별 POSE 재정의): SWIMMING / SLIDING 2 개 POSE.
  vanilla 가 이 POSE 로 자동 전환해도 SM 상태와 다른 bbox 생성 가능 → 매 tick Mixin 으로
  덮어쓰기.
- **통과 대상** (vanilla 유지): STANDING / CROUCHING / FALL_FLYING / SPIN_ATTACK / SLEEPING /
  DYING / LONG_JUMPING. SM 과 무관 — elytra / trident / 잠자기 등 vanilla 기능 보존.

---

## 3. Phase 구조

### Phase A. server-side `SmartMovingServer` 필드 확장

**A-1. 누락 필드 추가** (원본 대응 + StatePayload 참조)
- [ ] A-1a. `public boolean isClimbCrawling` (StatePayload bit 14 추가 필요 시)
- [ ] A-1b. `public boolean isHeadJumping` (StatePayload bit 20, 서버 필드만 누락)
- [ ] A-1c. `public boolean isSliding` (StatePayload bit 21)
- [ ] A-1d. `public boolean isSwimming` (bit 11)
- [ ] A-1e. `public boolean isDiving` (bit 9)
- [ ] A-1f. `public boolean isDipping` (bit 10)
- [ ] A-1g. `public boolean isFlying` (bit ?) — StatePayload 존재 여부 확인
- [ ] A-1h. `public boolean isLevitating` (bit 19)

### Phase B. `SmartMovingState.java` 인코딩 확장

**B-1. 누락 bit 추가**
- [ ] B-1a. `isClimbCrawling` — bit 14 또는 신규 비트 할당 + encode/decode 양방향
- [ ] B-1b. `isFlying` — StatePayload 에 없으면 신규 bit 할당 (현재 ClientState.isFlying
     은 vanilla `getAbilities().flying` 기반 — 그 값 자체를 전송 여부 결정)
- [ ] B-1c. 전수 grep 하여 StatePayload 에 없는 필드 확인 + 추가

**B-2. StatePayload 수신부 (서버) 에서 모든 필드 저장**
- [ ] B-2. `SmartMovingNetwork` 서버 수신 핸들러에서 `SmartMovingState.decode(bits)` 결과를
     `SmartMovingServer` 필드에 전수 복사.

### Phase C. server `MixinPlayerEntity.getBaseDimensions` 재작성

**C-1. client 와 동일 로직 이식**
- [ ] C-1. `sm_getBaseDimensions_server` 를 client Phase 1 버전과 동일 구조로 재작성:
  ```java
  boolean smSmall = sm.isCrawling || sm.isClimbCrawling
                 || sm.isHeadJumping || sm.isSliding
                 || sm.isSwimming || sm.isDiving
                 || sm.isFlying || sm.isLevitating;
  if (smSmall) {
      cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
      return;
  }
  if (pose == EntityPose.SLIDING) {
      cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
  }
  ```
- [ ] C-1a. server 측 기존 `isSmall && pose == SWIMMING → 0.6 × 1.0` 근사 **제거** (client 수정에 맞춤).

### Phase D. server `MixinPlayerEntity.updatePose` POSE 매핑 확장

**D-1. POSE 매핑 전수** (client 와 동일)
- [ ] D-1. `sm_updatePose_server` 재작성:
  ```java
  if (sm.isCrawling || sm.isClimbCrawling) setPose(SWIMMING); cancel();
  else if (sm.isHeadJumping || sm.isSliding) setPose(SLIDING); cancel();
  else if (sm.isSwimming || sm.isDiving) setPose(SWIMMING); cancel();
  else if (sm.isFlying || sm.isLevitating) setPose(SLIDING); cancel();
  // isDipping / 그 외 → vanilla 통과
  ```

### Phase E. 원본 heightOffset 렌더링 보정 (원본 L1708-L1722)

원본에서 `getBrightness(f)` / `getBrightnessForRender(f)` 등이 `posY -= heightOffset` 으로
임시 조정 — heightOffset 시 머리가 minY+1 위치이므로 light level 계산을 머리 기준으로.

**E-1. vanilla eyeHeight 참조 여부 확인**
- [ ] E-1. 1.21.1 vanilla 가 light level / brightness 계산에 어떤 좌표 쓰는지 확인. 대부분
     `player.getEyeY()` (eyeHeight 기반) 참조 → 이미 Phase 1 eyeHeight 수정으로 자동 반영.

**E-2. 렌더 보정 잔여 확인**
- [ ] E-2. SM 상태에서 light level / 수중 효과 / 안개 등이 원본과 차이 있는지 플레이테스트.
     차이 있으면 별도 Mixin 고려.

### Phase F. 검증 + 회귀 감사 + 플레이테스트

**F-1. client / server dimensions 일치 검증**
- [ ] F-1. 단위 테스트 또는 debug 로그로 client `getBoundingBox()` == server `getBoundingBox()`
     동시 출력 — 동일 값 확인.

**F-2. vanilla 기능 회귀 감사**
- [ ] F-2. elytra 비행 / trident 공격 / 잠자기 / 돌고래 점프 등 SM 무관 vanilla 기능 정상
     작동 확인. POSE 차단이 해당 기능을 방해하지 않음 검증.

**F-3. 원본 게임플레이 시나리오 테스트**
- [ ] F-3a. 1 블록 높이 통로 크롤 통과 (원본 OK — Phase 1 까지 해결, 서버 동기화 포함 확인)
- [ ] F-3b. 깊은 물 → 얕은 물 전환 시 bbox 변화 (server sync 중요)
- [ ] F-3c. 슬라이딩 중 천장 bumping (bbox 0.8 동치)
- [ ] F-3d. 헤드점프 착지 (bbox 복원 타이밍)
- [ ] F-3e. SM 비행 (cfg.fly=true) 중 좁은 공간 bbox 0.8 유지
- [ ] F-3f. 멀티플레이어 타 플레이어 view 에서 POSE 애니 일치

**F-4. 네트워크 sync 검증**
- [ ] F-4. 고지연 (200ms+) 환경에서 상태 전환 시 rubber banding 없는지.

**F-5. 빌드**
- [ ] F-5. `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL + mod
     jar 실제 클라/서버 실행 테스트.

---

## 4. 의존 순서

```
Phase A (서버 필드 확장)
   ↓
Phase B (StatePayload 인코딩 확장 + 수신부 저장)
   ↓
Phase C (server getBaseDimensions 재작성, client 와 동치)
   ↓
Phase D (server updatePose POSE 매핑 전수)
   ↓
Phase E (렌더 보정 잔여 확인, 대부분 eyeHeight 로 자동)
   ↓
Phase F (검증 + 회귀 + 플레이테스트)
```

**규모**: A 8 필드 + B 1-3 bit + C 1 재작성 + D 1 재작성 + E 1-2 확인 + F 5+ 시나리오 =
**약 18-22 원자 / 예상 3-5 세션**.

---

## 5. 1:1 번역 체크리스트 (원자별 적용)

- [근거] 원본 라인 확보 (`C:\Work\minecraft\porting\sm_original\SmartMoving\`)
- [근거] Phase 1 client 측 구현과 값/로직 동일
- [대응] 원본 ↔ 1.21.1 side-by-side 1:1
- [분기] 모든 SM 상태 8+ 전수 반영
- [상수] `0.6F` / `0.8F` / `0.62F` / `1.8F` / `1.62F` 값 정확
- [타이밍] POSE 차단 위치 (HEAD cancel) client/server 동일
- [근사] 근사 없음 — 전수 1:1 목표 (focus 2.7 §7 0건 유지)
- [신규] 추가 상태 발견 시 §2.1 표 업데이트
- [회귀] vanilla 기능 (elytra/trident/수영 버프/잠자기) 영향 없음
- [빌드] `compileJava compileClientJava --rerun-tasks` 성공

---

## 6. 작업 기록 (세션별)

### 세션 0 — 2026-04-25 — 포커스 #2.7 신설

- Phase 1 (세션 137) 에서 client `sm_getBaseDimensions_client` / `sm_updatePose_client` 전수
  수정 완료. 모든 SM 상태 → 0.6 × 0.8 + eyeHeight 0.62F. POSE 매핑 확장 (SWIMMING/SLIDING
  차단, elytra/trident 등 vanilla 통과).
- 서버 측 `MixinPlayerEntity` 는 구 로직 (크롤 0.6×1.0) + 제한된 POSE 매핑 (isCrawling /
  isSmall 만) → client 와 불일치 → desync 위험.
- server 측 `SmartMovingServer` 필드 부족 (isCrawling / isSmall 만) → POSE 판정 불가능 상태.
- StatePayload 는 대부분 bit 인코딩 완료 (isClimbCrawling / isFlying 확인 필요) — 수신부
  저장 경로 보완 필요.
- Phase A/B/C/D/E/F 약 18-22 원자 계획 확정.

**다음 세션 권고**: Phase A-1 (SmartMovingServer 누락 필드 8 건 추가).

---

## 7. 근사 이식 지점 (이 포커스)

현재 0건. 완결 목표도 **0건**. client/server 완전 대칭 + 원본 heightOffset 시맨틱 100%
재현이 목표.

**잔존 가능 후보**:
- 없음. 모든 로직이 Mixin + EntityDimensions + StatePayload 로 매핑 가능.

---

## 8. 소비처 영향 감사

### 8.1. 포커스 #2 Extended (완료)
- B-N-standup (1)(2) "영향 0 확정" → **수정** 필요: Phase 1 결과 0.6×0.8 에서만 동치. 세션
  136 기록의 "POSE 시스템 대체 → 영향 0" 은 값 미일치 상태에서의 평가였으므로 정정.
  focus_02_state_issues.md §7 갱신.

### 8.2. 포커스 #2.5 (Jumper Factor, 현재 포커스)
- 독립. Jumper factor 와 bbox sync 는 무관.

### 8.3. 포커스 #2.6 (Lava Border)
- 독립. lava border 는 AABB 스캔, dimensions 무관.

### 8.4. 포커스 #3 상태 전환 조건
- 현재 §18.1 (B-N-standup-4 vanilla flying sync) 과 **연관**. Phase D updatePose + Phase B
  StatePayload 확장이 §18.1 의 `UpdatePlayerAbilitiesC2SPacket` sync 와 통합 가능.
- 가능하면 병행 진행.

### 8.5. 포커스 #6 Speed Change
- 이미 완료. payload 패킷 인프라 (SmartMovingNetwork) 는 이 포커스에서 재활용.

---

## 9. 참고 자료

### 원본 소스 경로
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`
  - L1681-L1704: `setHeightOffset` / `resetHeightOffset`
  - L1708-L1722: `getBrightness` 렌더 보정
  - L511/L518/L1369/L1382/L1390/L1400/L2129/L2512/L2519/L2555/L2798/L2829/L2851/L2858:
    `setHeightOffset(-1F)` 설정 지점 전수
- `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\playerapi\SmartMovingServerPlayerBase.java`
  - L142-L145: `getEyeHeight()` 공식 (height - 0.18F)

### 1.21.1 이식 대상
- `src/client/java/.../mixin/client/MixinPlayerEntityClient.java` (✅ Phase 1 완료)
- `src/main/java/.../mixin/MixinPlayerEntity.java` (Phase C/D 수정 대상)
- `src/main/java/.../server/SmartMovingServer.java` (Phase A 필드 추가)
- `src/main/java/.../network/SmartMovingState.java` (Phase B 인코딩 확장)
- `src/main/java/.../network/SmartMovingNetwork.java` (Phase B 수신부 저장)

### 연관 포커스
- 포커스 #2 Extended (완료 — 세션 134). B-N-standup 기록 세션 137 에서 Phase 1 연계 갱신.
- 포커스 #3 §18.1 (B-N-standup-4 vanilla flying sync). 본 포커스 Phase D 와 병합 가능.
