# 그랩 클라이밍 애니메이션 1:1 작업 체크리스트

리서치: `docs/research_climbing_animation.md`
대상: `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java`
범위: **기본 블록 그랩 클라이밍** (사다리/넝쿨이 아닌 일반 블록).

작업 순서: HIGH → MID → LOW. 각 항목 단위로 사용자 인게임 검증 후 다음.

---

## Phase 1 — HIGH 우선순위

### [x] (D-4) NoGrab + non-NoStep 보정 위치 정정
- **현재** (`MixinPlayerEntityModelClient.java` L466-L473): `if (sm.isCrawlClimbing)` 블록 **안**.
- **원본** (`SmartMovingModel.java` L279-L286): `if(isCrawlClimb)` 블록 **밖**.
- **수정**: L466-L473 블록을 L474 `}` (CrawlClimb 종료) **밖** 으로 이동.
- **결과**: CrawlClimb 여부 무관, 모든 클라이밍에서 NoGrab + non-NoStep 시 보정 적용.
- **세부 매핑**:
  - `body.pitch = 0.5f` (원본 `bipedTorso.rotateAngleX = 0.5F`)
  - `head.pitch -= 0.5f` (원본 `bipedHead.rotateAngleX -= 0.5F`)
  - `body.pivotZ = -6f` (원본 `bipedTorso.rotationPointZ = -6.0F`)
  - `rightLeg.pitch -= 0.5f; leftLeg.pitch -= 0.5f` (원본 `bipedPelvic.rotateAngleX -= 0.5F` 의 1.21.1 매핑 — pelvic 부재로 다리 직접 적용)
- **인게임 검증**: 일반 블록 그랩 (NoGrab 상태 = 손을 잡지 않는 케이스) + 발만 이동하는 시나리오. CrawlClimb 가 아닌 일반 클라이밍에서 몸 앞으로 0.5rad 기울기 + 머리/다리 -0.5rad 보정이 보이는가.

---

## Phase 2 — MID 우선순위

### [x] (V-1) horizontalSpeed 출처 정정 (`limbSwingAmount` → `sm.stats.currentHorizontalSpeed`)
- **현재** (L343): `float horizontalSpeed = Math.min(0.5f, limbSwingAmount);`
- **원본** (L145): `float horizontalSpeed = Math.min(0.5f, currentHorizontalSpeed);`
- **수정**: `sm.stats.currentHorizontalSpeed` 로 교체.
- **검증 전제**: `SmartStatistics.calculate` 의 `currentHorizontalSpeed` 갱신이 정상 동작하는지 확인 (verticalSpeed 와 동일 패턴이므로 안전 가능성 높음).
- **인게임 검증**: 좌우 이동 시 팔/다리 yaw/roll 진폭이 변경되는지. limbSwingAmount 와 currentHorizontalSpeed 차이는 사다리에서 가만히 있을 때 (limbSwingAmount=0 vs currentHorizontalSpeed=평균값) 가시.

### [x] (D-1) bipedOuter.rotateAngleY 매핑 검증 — 이미 처리됨 (PASS)
- **원본** (L129): `bipedOuter.rotateAngleY = forwardRotation / RadiantToAngle;`
- **현재**: 우리 `sm_setupTransforms` ModifyArg `bodyYaw force` 로 대체 처리 가능성 (검증 필요).
- **검증 절차**:
  1. `MixinLivingEntityRenderer` (또는 setupTransforms inject 위치) 에서 클라이밍 분기 시 bodyYaw 값 확인.
  2. 원본 `forwardRotation` 식 (SmartRender 측) 과 비교.
  3. 일치하면 PASS 처리, 불일치하면 sm_animateClimbing 진입부에 `body.yaw` set 추가.
- **인게임 검증**: 측면(좌/우) 이동 시 몸과 팔의 회전이 카메라 정렬되는가.

---

## Phase 3 — LOW 우선순위 / 후순위

### [x] (D-2) 다리 rotationOrder = YZX — 다리 yaw=0 으로 영향 없음 (주석만)
- 현재 다리 yaw=0 으로 고정. 회전 순서 차이 결과 영향 없음.
- 원본 1:1 명시성 위해 `setAnglesYZX(rightLeg, ...)` 헬퍼 적용 가능. **선택사항.**

### [x] (D-3) CrawlClimb shoulder 매핑
- 원본 `bipedRightShoulder/LeftShoulder.rotateAngleX = -bodyAngleX` (L267-L268).
- 1.21.1 shoulder 부재 → arm.pitch 에 `-bodyAngleX` `+=` 누적.
- **CrawlClimb 시나리오만**. 사용자 요청 (기본 블록 그랩) 범위 밖이므로 후순위.

### [ ] (V-2) ordinal 매핑 정합성
- 우리 enum (NONE/SINK/TOP_HOLD/BOTTOM_HOLD/UP/FAST_UP) → 3-way (NoGrab/UpGrab/MiddleGrab) 변환.
- 동작 확인된 매핑이므로 변경 금지.
- 인게임 시각 차이 발견 시에만 원본 setShouldClimbSpeed 호출 패턴과 교차 대조.

---

## 작업 흐름

1. Phase 1 (D-4) 만 먼저 수정 → 빌드 → 사용자 인게임 검증.
2. 사용자 OK → Phase 2 (V-1, D-1) 순차 진행. 각 항목별 검증.
3. Phase 3 는 별도 요청 시.
4. 모든 작업 완료 후 회귀 점검 (vine 클라이밍, CrawlClimb 영향 확인) + 커밋.
