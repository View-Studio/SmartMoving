# 엎드리기 애니메이션 1:1 fix 체크리스트

리서치: `docs/research_crawl_animation.md`
대상: `MixinPlayerEntityModelClient.sm_animateCrawling` (L805-L855)

---

## Phase 1 — HIGH 우선순위 (한 번에 모두 적용 가능)

### [ ] (2-1) `distance` 입력 정정
- 이전: `distance = limbSwing * 1.3f`.
- 새: `distance = sm.stats.getTotalHorizontalDistance(partialTicks) * 1.3f` (partial tick lerp).

### [ ] (2-2) `walkFactor`/`standFactor` 입력 정정
- 이전: `smFactor(limbSwingAmount, ...)`.
- 새: `smFactor(sm.stats.currentHorizontalSpeedFlattened, ...)` (원본 직접 read, lerp 없음).

### [ ] (2-3) `body` 회전 순서 YZX 매핑
- 이전: 직접 `body.pitch / body.yaw / body.roll` set (ModelPart 기본 ZYX).
- 새: `setAnglesYZX(body, pitch, yaw, roll)` 헬퍼 사용 (그랩 클라이밍 팔 패턴).

**시그니처 변경 필요**: `sm_animateCrawling(float, float, float, float)` → `sm_animateCrawling(SmartMovingClientState sm, float headYaw)`. 호출처 수정 (L318).

---

## Phase 2 — LOW (선택사항)

### [ ] (2-4) scale 가드 명시
- 원본 `if (scaleLegType != NoScaleStart)` / `scaleArmType != NoScaleStart` 가드.
- 메인 모델 = Scale 모드라 통과. 결과 동일.
- 명시성만 — 후순위.

---

## 잔존 차이 (의도된 차이 또는 1.21.1 매핑 한계)

- **bipedBody (Y) vs body 단일 노드** — 1.21.1 ModelPart parallel 구조라 정밀 1:1 어려움. body.X 큰 (78°) + Y/Z 작은 cos → 자식 효과 미세 → 시각 영향 미세.
- **viewHorizontalAngelOffset** = `headYaw` 매개변수 등가 (vanilla netHeadYaw). 매핑 정확.

---

## 작업 흐름

1. Phase 1 (2-1, 2-2, 2-3) 한 번에 적용 (서로 의존).
2. 빌드 → 사용자 인게임 검증.
3. OK 면 task #12 완료 + 커밋.
4. Phase 2 별도 요청 시.

---

# Phase 3 (2026-05-03) — 자식 효과 매핑 (= entity 회전 방식)

리서치 부록.4 (`docs/research_crawl_animation.md`) 의 옵션 A.

**상태**: Phase 1 (수치 매핑) 완료. **자식 효과 (= bipedTorso 회전이 head/arm/leg 영향) 미매핑** 잔존.

## ⚠️ 절대 침범 금지 영역 — 엎드리기 **기능** (= state/박스/콜리전/입력)

본 작업은 **애니메이션만**. 엎드리기 **기능 시스템은 완결됨** (사용자 명시). 다음 영역 **수정/검토 금지**:

| 영역 | 위치 | 관련 메모리 |
|------|------|-----------|
| isCrawling state 진입/해제 | `SmartMovingClientState.tickEssential` (L1700+ canCrawl, isCrawling 식) | `project_isCrawlClimbing_complete.md` |
| heightOffset 처리 | SmartMovingClientState 의 heightOffset set/reset 로직 | `feedback_heightoffset_pattern.md` |
| dim 분기 (콜리전) | `MixinPlayerEntityClient.sm_getBaseDimensions_client` + `MixinPlayerEntity.sm_getBaseDimensions_server` | `feedback_smSmall_dim_omission.md`, `project_isCrawlClimbing_complete.md` Fix 7 |
| mixin offset 가드 | `MixinEntity.sm_offsetBoundingBoxForFlying` (height<1 && eye>1) | `project_isclimbcrawling_complete.md` |
| 클라/서버 박스 동기화 | `SmartMovingState` bit + `SmartMovingServer` | `feedback_server_reconcile_box_sync.md` |
| sneak 입력 검사 | `MixinKeyboardInput`, `MixinClientPlayerEntity.sm_isSneaking_ClientPlayer` | `feedback_movementInput_vs_isSneaking.md` |
| isCrawlClimbing 시스템 | `SmartMovingFlyer` 외 모든 isCrawlClimbing 분기 | `project_isCrawlClimbing_complete.md` |
| ICC (isClimbCrawling) 시스템 | `SmartMovingClientState` ICC 진입/해제 | `project_isclimbcrawling_complete.md` |

**위 영역 코드는 읽기만** — fix 적용 시 영향 검토용. 직접 수정 금지.

## 본 작업 변경 범위 — **애니메이션 2 파일만**

| 파일 | 메서드 | 변경 |
|------|--------|------|
| `MixinPlayerEntityRenderer.java` | `sm_setupTransforms` (L345-) | isCrawling 분기 **추가** (entity 회전) |
| `MixinPlayerEntityModelClient.java` | `sm_animateCrawling` (L844-893) | body.pitch / body.pivotY 두 라인만 수정 |

기능 영역과 분리됨. **다른 메서드 / 파일 건드리지 않음**.

## Phase 3-1: 사전 준비

- [ ] git 작업 영역 clean 확인 (= 다른 fix 영향 없게)
- [ ] 현재 `sm_animateCrawling` (L844-893) 파일 위치 + 라인 번호 재확인
- [ ] 현재 `sm_setupTransforms` (L345-) 의 isSliding 분기 (L390-L400) 패턴 참고

## Phase 3-2: 코드 변경 (2 곳, 한 번에 적용)

### [ ] 변경 A — `MixinPlayerEntityRenderer.sm_setupTransforms` 에 isCrawling 분기 추가
**위치**: isFlying 분기 직전 또는 isSliding 분기 직후 적절 위치.

**추가 코드**:
```java
// SM 엎드리기 (isCrawling): 원본 SmartMovingModel L404-L405 자식 효과 매핑.
//   bipedTorso.rotateAngleX = Quarter - Thirtytwoth = 78.75°
//   bipedTorso.rotationPointY = 3F (= 모든 자식 노드 +3 이동)
// 1.21.1 평탄 모델 → entity 회전 + translate 로 모든 노드 일괄 처리.
// `(isCrawling && !isClimbing)` 가드 = 원본 isCrawl 진입 조건 (SmartMovingRender L75) 1:1.
if (sm.isCrawling && !sm.isClimbing) {
    float tiltAngle = (float)(Math.PI / 2 - Math.PI / 16);  // = QUARTER - THIRTYTWOTH
    // 부호 반전 (memory feedback_render_scale_negation.md — vanilla scale(-1,-1,1) 보정)
    matrices.multiply(RotationAxis.POSITIVE_X.rotation(-tiltAngle));
    sm.smOuterTiltX = tiltAngle;
    // bipedTorso.rotationPointY = 3F → /16 픽셀→블록
    matrices.translate(0f, 3f / 16f, 0f);
}
```

### [ ] 변경 B — `sm_animateCrawling` 의 body 회전/위치 항 제거
**위치**: `MixinPlayerEntityModelClient.java:862-866`.

**변경 전**:
```java
setAnglesYZX(body,
        QUARTER - THIRTYTWOTH,                                // ← 제거 대상 (entity 회전이 대신)
        cos(distance + HALF) * SIXTYFOURTH * walkFactor,      // 유지 (LOCAL 미세 진동)
        cos(distance + QUARTER) * SIXTYFOURTH * walkFactor);  // 유지
body.pivotY = 3f;  // ← 제거 대상 (entity translate 가 대신)
```

**변경 후**:
```java
setAnglesYZX(body,
        0f,                                                   // ← 0 으로 (entity 회전 적용됨)
        cos(distance + HALF) * SIXTYFOURTH * walkFactor,
        cos(distance + QUARTER) * SIXTYFOURTH * walkFactor);
// body.pivotY = 3f 제거 (entity translate 가 대신)
```

**유지**:
- 머리 (head.roll/pitch/pivotZ) — LOCAL 보정
- 다리 (leg.pitch/leg.roll) — LOCAL
- 팔 (arm setAnglesYZX) — LOCAL
- legScales / armScales — LOCAL

## Phase 3-3: 빌드

- [ ] `./gradlew compileClientJava` 통과 확인

## Phase 3-4: 인게임 검증

### 자세 검증 (정상 동작 확인)
- [ ] 평지 sneak → 엎드린 자세 매핑 확인 — body 만 누워있는 거 아니라 head/arm/leg 모두 누운 자세
- [ ] 엎드린 채 이동 (W) — 자세 안정 (fade 없이 누운 상태 유지)
- [ ] 사용자 1인칭 시점 — 시점 위치 자연스러움 (= 엎드린 자세 시점)

### 기능 회귀 검증 (= 절대 침범 금지 영역 정상 작동 확인)
- [ ] **엎드리기 진입/해제**: sneak 누름/뗌 시 isCrawling state 정상 전환 (= 박스 dim 변화 정상)
- [ ] **콜리전 박스 dim**: F3 화면에서 height=0.8 유지 (Phase 3 변경이 dim 영향 0)
- [ ] **isCrawlClimbing 시나리오**: 평지 엎드림 → 블록 앞 + grab + W → 엎드린 채 climbing 정상 (= `project_isCrawlClimbing_complete.md` 회귀 X)
- [ ] **isClimbCrawling (ICC) 시나리오**: 일반 climbing → 좁은 갭 → ICC 정상 진입/해제 (= `project_isclimbcrawling_complete.md` 회귀 X)
- [ ] **ICC → isCrawlClimbing 자연 전환**: `project_isCrawlClimbing_complete.md` Fix 6/7 시나리오 정상

### 기타 회귀
- [ ] **MixinCapeFeatureRenderer (망토)**: smOuterTiltX 사용. 엎드린 채 망토 회전 자연스러움
- [ ] **다른 SM 자세** (isSliding/isSwimming/isFlying/isHeadJumping): 영향 0 (= sm_setupTransforms 안 다른 분기 그대로)

## Phase 3-5: 회귀 발견 시 대응

| 회귀 시나리오 | 원인 후보 | 대응 |
|-------------|----------|------|
| 자세가 두 배 회전 | scale(-1,-1,1) 부호 보정 누락 → POSITIVE_X.rotation(+tiltAngle) 사용 시 | `-tiltAngle` (음수) 확인 |
| 자세가 안 누움 | entity 회전이 적용 안 됨 | `sm_setupTransforms` TAIL inject 시점 확인 |
| 발이 땅속/공중 | translate(3/16) 위치 보정 부족 | `feedback_animation_porting.md` 의 heightOffset = 0 검증 |
| 머리 위치 잘못 | head.pivotZ = -2 가 LOCAL 좌표라 entity 회전 후 다른 방향으로 이동 가능 | head.pivotZ 부호/축 검증 |
| isCrawlClimbing 자세 깨짐 | sm_animateCrawling 변경 영향 | `(isCrawling && isClimbing && wasClimbCrawling)` dim 분기 + isCrawlClimbing 자세 분기 (`MixinPlayerEntityModelClient.java:620-650`) 별도 검토 |
| 사용자 시점 회전 (1인칭이 누운 시점으로) | entity 회전 = 카메라도 회전 | `MixinCamera` 또는 `setupTransforms` 시점 처리 검증 |

## Phase 3-6: 완료 처리

- [ ] 사용자 "완결" 명시 시 메모리 기록
  - `project_crawl_animation_complete.md` 신규 — 자식 효과 매핑 완결
  - `project_crawl_climbing_pending.md` 의 잔존 차이 3가지 (head.pitch / leg.pitch / arm.pitch 자식효과) 별도 관계 (이건 isCrawlClimb 분기 — 본 작업과 무관, 또는 같이 fix 가능)
- [ ] MEMORY.md 업데이트
- [ ] git commit (Phase 3 변경 2 파일만)

## 작업 원칙 재강조

1. **애니메이션만 변경** — 2 파일, 2 메서드, 약 5-10 라인 변경.
2. **기능 영역 코드 읽기만** — 영향 검토 위해 read-only.
3. **변경 전 회귀 영역 메모리 재확인** — 함부로 수정 금지 영역 인지.
4. **회귀 발견 즉시 대응** — Phase 3-5 표 따라 디버그 + 미해결 시 사용자 보고.
