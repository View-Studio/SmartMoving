# 애니메이션 포팅 시도 기록 — 실패/성공 분석

> 이 문서는 1.7.10 SmartMoving 애니메이션을 1.21.1 Fabric으로 포팅하면서 시도한 방법들을
> 커밋 히스토리와 세션 기록 기반으로 정리한 것이다.
> **실패한 방법은 다시 사용하지 말 것.**

---

## 핵심 구조 차이 (전제)

| 항목 | 1.7.10 원본 | 1.21.1 포팅 |
|------|------------|------------|
| 모델 구조 | 계층적: `bipedOuter → bipedTorso → 팔/다리/머리` | flat: 모든 파트가 world space에서 독립 |
| 각도 의미 | 팔/다리 = LOCAL (부모 상대) | 팔/다리 = WORLD (절대) |
| pivotY 의미 | 부모 그룹 전체를 이동 | 해당 파트만 이동 |
| 회전 적용 순서 | YZX/ZYX 등 상태별 다름 | pitch-first 내재적: Rx(pitch)→Ry(yaw)→Rz(roll) |

---

## 시도 1: LOCAL 값 직접 사용 (Phase 3 최초 구현)

**커밋:** `5b34eec` — Phase 3-1~3-6 애니메이션 기반 구현

**방법:**
- 원본 `bipedRightArm.rotateAngleX = HALF + EIGHTH` 그대로 사용
- `head.pitch = -(QUARTER - THIRTYTWOTH)` — 원본 LOCAL 값 그대로
- `body.pivotY`/`pivotX` 미설정

**결과: ❌ 실패**

**실패 이유:**
- 원본 값은 부모(bipedTorso/bipedOuter)가 회전된 상태에서의 LOCAL 값이었음
- 1.21.1에서 부모 없는 world 좌표계에 적용하면 완전히 다른 방향이 나옴
- 예: 크롤링에서 머리가 땅속으로 파고드는 현상

---

## 시도 2: LOCAL → WORLD 변환 (pitch 합산)

**커밋:** `827e0ab` — 애니메이션 계층 모델 좌표계 변환 버그 수정

**방법:**
- `world pitch = outerX + local pitch` (X축 회전은 부모 X와 단순 합산)
- `head.pitch` 수정: `-(bodyPitch)` → `0F` (flat 모델에서 world pitch=0)
- 천장 클라이밍 `arm.yaw` 수정: `-(rotateY)` → `0F` (부모 없으므로 상쇄 불필요)
- 헤드점프 `head.pitch` 수정: `-outerX/2` → `outerX/2` (world = outerX + (-outerX/2) = outerX/2)
- arm/leg LOCAL Z(roll) → 직접 `arm.roll`에 대입 (이 부분이 핵심 오류)

**결과: ❌ 부분 실패 (몸통 분리 지속)**

**실패 이유:**
- `body.pivotY` 미설정 — 크롤링 시 body만 이동, head/arm 위치는 그대로 → 파트 간 갭 발생
- arm/leg LOCAL Z → `arm.roll` 직접 대입은 이론상 맞지만, pitch-first 회전 순서의 정확한 의미 미파악으로 다른 부분에서 버그 잔재

---

## 시도 3: 3D 외재적 좌표 분해 (sin/cos 분해)

**커밋:** `97a3c2b` — 애니메이션 3D 좌표계 변환 전면 수정

**방법:**
```java
// outerX(body pitch)가 있을 때 local Z(roll)의 world 변환
world_yaw  = -localZ * sin(outerX)
world_roll =  localZ * cos(outerX)
```
- `body.pivotY = 0F` 전역 리셋 추가 (inject 시작 시)
- `body.pivotY = 3F` (크롤링), `5F` (슬라이딩) 개별 설정
- 수영 팔 spread: `arm.yaw`, `arm.roll` 분리 계산

**결과: ❌ 실패 ("전혀 안고쳐짐. 아까랑 똑같아.")**

**실패 이유:**
1. **3D 분해 자체가 틀림**: Minecraft 1.21.1의 ModelPart 회전은 **pitch-first 내재적 회전** (Rx→Ry→Rz). 이 순서에서는 `arm.pitch = outerX` + `arm.roll = localZ`가 정확히 `bipedOuter(X=outerX)` 아래 `child(Z=localZ)` 구조와 동일하게 동작함. sin/cos 외재적 분해는 **다른 회전 체계**에서 유효한 공식이며 Minecraft에 적용 시 잘못된 결과.
2. **`body.pivotY = 0F` 전역 리셋**: 매 틱 inject 시작 시 실행되어 바닐라 스니킹의 `body.pivotY = 3.2F`를 강제로 0으로 되돌림 → 스니킹 시 몸통/머리 분리 발생
3. **head/arm/leg pivotY 여전히 미설정**: `body.pivotY`만 바꾸고 나머지 파트는 그대로 → 1.21.1 flat 모델에서는 body만 이동, 나머지 파트는 제자리 → 분리 지속

---

## 시도 4: pitch-first 회전 이해 + 직접 roll 대입

**커밋:** 현재 세션 (미커밋, `PlayerEntityModelMixin.java` 재작성)

**방법:**
- sin/cos 분해 제거
- `arm.roll = original_arm_Z` 직접 대입 (올바른 방향)
- `body.pivotY = 0F` 전역 리셋 제거 → SmartMoving 상태별로만 설정
- `wasCrawling → body.pivotY = 0F` cleanup 코드 추가
- 크롤링: `body.pivotY = 3F` (body만)
- 슬라이딩: `body.pivotY = 5F` (body만)

**결과: ❌ 부분 실패 (몸통 분리 지속)**

**실패 이유:**
1. **head/arm/leg pivotY 여전히 미설정**: `body.pivotY = 3F`로 body는 3px 아래로 이동하지만, `head.pivotY`는 0F 그대로 → 목 부분에 3px 갭 발생
2. **`wasCrawling → body.pivotY = 0F` cleanup이 오히려 해로움**: 크롤링 직후 스니킹으로 전환하면 바닐라가 `body.pivotY = 3.2F` 설정 → 우리 cleanup이 이를 0F로 덮어씀 → 한 프레임 glitch

---

## 시도 5: 모든 pivotY 올바르게 설정 (현재)

**커밋:** 현재 세션

**방법:**
- `resetPivots()` 헬퍼 메서드 추가 (비-스니킹 기본값으로 reset)
- **크롤링 (bipedTorso 그룹 +3F):**
  - `head.pivotY = 3F`, `body.pivotY = 3F`, `rightArm.pivotY = 5F`, `leftArm.pivotY = 5F`
  - 다리는 bipedTorso 자식이 아니었으므로 `rightLeg.pivotY = 12F` (기본값)
- **슬라이딩 (bipedOuter 그룹 +5F):**
  - `head.pivotY = 5F`, `body.pivotY = 5F`, `rightArm.pivotY = 7F`, `leftArm.pivotY = 7F`
  - `rightLeg.pivotY = 17F`, `leftLeg.pivotY = 17F`
- **그 외 SmartMoving 상태**: `resetPivots()` 호출 → 스니킹 pivotY 간섭 방지
- **`wasCrawling` cleanup 완전 제거**: 바닐라 setAngles가 매 프레임 자동 복원

**결과: ⬜ 미확인 (게임 내 테스트 필요)**

**이론적 근거:**
- 바닐라 `BipedEntityModel.setAngles()` 디컴파일 결과: 스니킹/비스니킹 모두 모든 파트의 pivotY를 명시적으로 설정함
  - 비스니킹: head=0, body=0, arm=2, leg=12
  - 스니킹: head=4.2, body=3.2, arm=5.2, leg=12.2
- 1.7.10 원본의 `bipedTorso.rotationPointY = 3F`는 torso 그룹 전체(head+body+arm) 이동 → 1.21.1에서는 그룹이 없으므로 각 파트에 직접 설정해야 함

---

## 검증된 올바른 접근법 (Minecraft 1.21.1 ModelPart 회전 원칙)

### 1. pitch-first 내재적 회전
```
Rx(pitch) → Ry(yaw) → Rz(roll)
```
- `arm.pitch = outerX` + `arm.roll = localZ` = 원본 `bipedOuter(X=outerX)` 아래 `child(Z=localZ)`와 동일
- **sin/cos 외재적 분해 사용 금지** — Minecraft 회전 체계에 맞지 않음

### 2. pivotY는 해당 파트만 이동 (자식 파트 불포함)
- `body.pivotY = 3F`는 body만 이동, head/arms/legs 불포함
- 그룹 전체를 이동하려면 모든 파트에 개별 설정 필요

### 3. 바닐라 스니킹 pivotY 간섭 주의
- 바닐라 setAngles가 매 프레임 실행되어 sneaking/non-sneaking 블록에서 pivotY를 설정함
- SmartMoving inject는 @TAIL이므로 그 이후에 실행 → 바닐라가 설정한 pivotY를 덮어쓰거나 보존할 수 있음
- SmartMoving 상태 활성 시 → 명시적으로 원하는 pivotY로 덮어씀
- SmartMoving 상태 비활성 시 → pivotY 건드리지 않음 (바닐라에 위임)

### 4. 전역 리셋은 바닐라 관리 범위를 침범하지 않도록
- `body.yaw = 0F`, `body.roll = 0F`, `arm.yaw/roll = 0F`, `leg.yaw/roll = 0F` → OK (바닐라가 설정하지 않는 축)
- `body.pivotY = 0F` 전역 리셋 → **금지** (바닐라 스니킹 파괴)
- `wasCrawling → body.pivotY = 0F` cleanup → **금지** (바닐라가 자동 복원, 오히려 간섭)

---

## 빠른 참조: 1.21.1 바닐라 기본 pivotY 값

| 파트 | 비스니킹 기본값 | 스니킹 값 |
|------|--------------|---------|
| head | 0F | 4.2F |
| body | 0F | 3.2F |
| rightArm | 2F | 5.2F |
| leftArm | 2F | 5.2F |
| rightLeg | 12F | 12.2F |
| leftLeg | 12F | 12.2F |
| rightLeg.pivotZ | 0F | 4.0F |
| leftLeg.pivotZ | 0F | 4.0F |

> 출처: `~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar`의 `fvx.class` (BipedEntityModel) vineflower 디컴파일
