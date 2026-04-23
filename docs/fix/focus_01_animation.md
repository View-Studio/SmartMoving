# Focus #1 — 애니메이션 망가짐 / 이상함

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #1 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (재현 케이스 수집 / #2 선행) |
| 현재 단계 | 재현 케이스 수집 |
| 선행 의존 | #2 (애니메이션 입력 상태가 정확해야 의미 있음) |

---

## 2. 증상 — 원본 vs 현재

특정 상태의 애니메이션이 원본과 **시각적으로 다름**:
- 팔/다리/몸통 각도가 어긋남
- 회전축 방향 반대
- 애니메이션 정지 / 떨림
- 좌우 비대칭 오류

상태별 구분 필요 (수영/잠수/사다리/넝쿨/크롤/슬라이딩/헤드점프/비행/천장/벽점프/로프).

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

| # | 상태 | 상황 | 원본 모습 (근거) | 1.21.1 실제 | 차이 |
|---|------|------|---------------|-----------|------|
| 1 | (예) isCrawling | 전진 | 몸통 수평, 팔 교차 스윙 | 몸통 수평, 팔 스윙 방향 반대 | 좌우 대칭 반전 |
| 2 | (예) isHeadJumping | 공중 이동 | 몸이 전방으로 기울어짐 | 몸이 수직 | pitch 적용 안 됨 |
| 3 | (예) isFeetVineClimbing | 넝쿨 오르기 | 다리 roll 누적 스윙 | roll=0 | 누적 로직 부재 |
| 4 | | | | | |

**원본 모습 근거**: 가능한 한 원본 `SmartMovingModel.md` 라인 번호 + 코드 수식까지
특정. 불명확하면 "확인 필요" 로 표기하고 A 단계에서 WebFetch.

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2** (렌더 입력 상태) |
| 영향받는 후속 | 없음 |
| 영향 주는 완료 이식 | `MixinPlayerEntityModelClient` 전체 + `MixinPlayerEntityRenderer.sm_setupTransforms` / `sm_captureBodyYaw` |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 상태 | `SmartMovingModel.md` 섹션 | 분기 번호 |
|------|--------------------------|----------|
| isRopeSliding | L269-L302 | 1 |
| isClimb / isCrawlClimb | L305-L430 | 2 |
| isCeilingClimb | L457-L481 | 4 |
| isSwim | L485-L522 | 5 |
| isDive | L526-L557 | 6 |
| isCrawl | L645-L670 | 7 |
| isSlide | ~L680 | 8 |
| isFlying | L695-L720 | 9 |
| isHeadJumping | L725-L745 | 10 |
| isFalling | ~L750 | 11 |

각 상태 블록 내:
- rotateAngleX/Y/Z (pitch/yaw/roll)
- 팔/다리 각도 공식
- scale 함수 (`setArmScales` / `setLegScales`)
- 회전 순서 (`rotationOrder = ModelRotationRenderer.YZX` 등)

### 5.2. 이미 확보된 주요 공식 (이전 세션)

- isFeetVineClimbing pitch/roll 비대칭 분기 (이식 완료)
- isDive `rotateAngleX` 3-way (isLevitate/isJump/currentVerticalAngle) (이식 완료)
- isSwim/isDive horizontalAngle threshold (이식 완료)
- isCeilingClimb `rotateY + horizontalAngle` (이식 완료)
- 상태별 `bipedOuter.rotateAngleY` 분기 (이식 완료)

### 5.3. 확보 필요 (재현 케이스별)

```
대상 URL: SmartMovingModel.java
추출 대상:
  1. <케이스 #N 상태> 의 setRotationAngles 블록 전체
  2. 각 팔/다리 rotateAngleX/Y/Z 공식 — sin/cos/상수 그대로
  3. 회전 순서 (rotationOrder 설정)
  4. setArmScales / setLegScales 인자
```

---

## 6. 1:1 매핑 테이블

| 원본 | 1.21.1 | 비고 |
|---|---|---|
| `bipedHead` / `bipedBody` (ModelBiped) | `head` / `body` (PlayerEntityModel) | ✓ |
| `bipedLeftArm` / `bipedRightArm` | `leftArm` / `rightArm` | ✓ |
| `bipedLeftLeg` / `bipedRightLeg` | `leftLeg` / `rightLeg` | ✓ |
| `bipedOuter` | **대응 없음** | 구조적 N/A — sm_captureBodyYaw / sm_setupTransforms 로 body 단일 경로 근사 |
| `bipedTorso` / `bipedPelvic` / `bipedBreast` / `bipedNeck` / `bipedShoulder` | 대응 없음 | SR 전용 노드 — N/A |
| `rotateAngleX/Y/Z` | `pitch`/`yaw`/`roll` | ✓ |
| `rotationPointX/Y/Z` | `pivotX/Y/Z` | ✓ |
| `rotationOrder = YZX` | `setAnglesYZX` 헬퍼 | ✓ |
| `setArmScales(rx, ry, rz, lx, ly, lz)` | `setScaleArms(rightArm, leftArm, s)` 근사 | ⚠️ 3축 스케일 정밀도 손실 |

---

## 7. 구조적 차이 / 근사 이식 지점

### 7.1. 재현 불가 (구조적)

- `bipedOuter` 계층 부재 → 몸 전체 회전 ≠ 머리/팔/다리 회전 분리 불가
- SR 전용 노드(torso/pelvic/breast/neck/shoulder) 부재 → 세부 자세 손실
- `ModelRotationRenderer` 의 displayList + 6가지 회전 순서 시스템 → MatrixStack YXZ 기본만

### 7.2. 근사 이식

- body yaw override 를 sm_captureBodyYaw 로 단일 경로 적용 (bipedOuter 대체)
- pitch/yaw/roll 순서는 `setAnglesYZX` 헬퍼로 ZX 순으로 치환 (원본 YZX 와 수학 등가 검증)

### 7.3. 완전 재현 가능

- 개별 팔/다리 각도 공식 (cos/sin/상수) — 원본과 **동일 수식** 적용 시 완전 재현
- sin/cos 분해 근사는 **금지** (메모리 기록된 실패 패턴)

---

## 8. 현재 구현 스냅샷

재현 케이스별 관련 `sm_animateXxx` 블록 임베드.

---

## 9. 예상 수정 diff

케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집
- [ ] P-1. 각 상태별 재현 (스샷/동영상) + §3 표 채우기
- [ ] P-2. 원본 모습은 1.7.10 게임플레이 영상 참조 또는 원본 공식 수학 재현

### A. 원인 분석 (케이스마다)
- [ ] A-N. (케이스별 — 원본 공식 vs 현재 구현 side-by-side)

### B. 수정
- [ ] B-N. (케이스별 공식 교체)

### C. 검증
- [ ] C-1. 빌드
- [ ] C-2. 각 상태 시각 검증 — 원본과 육안 비교
- [ ] C-3. 회귀 방지 감사
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → (다음 없음, 전체 완료)

---

## 11. 호출 타이밍 검증

| 훅 | 원본 | 1.21.1 |
|---|------|--------|
| setRotationAngles / setAngles | 매 렌더 프레임 (render()) | MixinPlayerEntityModelClient.setAngles TAIL inject |
| bodyYaw 계산 | rotatePlayer() (renderYawOffset) | MixinPlayerEntityRenderer.sm_captureBodyYaw + @ModifyArg |
| X 기울기 (setupTransforms) | renderModel() matrix transform | MixinPlayerEntityRenderer.sm_setupTransforms TAIL |

일치: ✓ (이전 세션 검증됨)

---

## 12. 테스트 프로토콜

```
[T-N] 상태별 애니메이션 육안 검증
  1. F3 + B 로 hitbox 표시 (정확한 위치 확인)
  2. 3인칭 시점으로 자신 관찰
  3. 원본 1.7.10 동일 상태와 비교 (레퍼런스 영상/스샷)
  4. 특히 다음 확인:
     - 팔 스윙 방향 (전진 vs 후진)
     - 다리 교차 패턴 (좌우 번갈아)
     - 몸통 pitch (앞뒤 기울기)
     - 몸통 yaw (좌우 회전)
     - 애니메이션 속도 (속도에 따른 스윙 주기)
```

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표의 모든 케이스 시각 매칭 (근사 한도 내)
- [ ] 원본 공식이 sin/cos 분해 근사가 아닌 원본 그대로 사용
- [ ] 회전 순서(YZX 등) 원본과 일치
- [ ] 좌우 대칭/교차 패턴 원본과 일치
- [ ] 스케일 함수(setArmScales/setLegScales) 값 원본과 일치
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| isFeetVineClimbing pitch/roll 비대칭 | 일반 경로 가드 + vine 블록 누적 | [ ] |
| isDive rotateAngleX 3-way | isLevitate/isJump 분기 | [ ] |
| isCeilingClimb rotateY + horizontalAngle | `setupTransforms` 와 `captureBodyYaw` 둘 다 | [ ] |
| 상태별 bipedOuter.rotateAngleY 분기 | sm_captureBodyYaw 우선순위 체인 | [ ] |
| setAnglesYZX 헬퍼 | 회전 순서 일관 적용 | [ ] |

---

## 15. 작업 기록

_(비어있음)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- SR 전용 노드(bipedOuter/torso 등) 시각 재현을 위한 다층 모델 렌더는
  1.21.1 단일 PlayerEntityModel 구조상 근본적으로 불가 → 구조적 N/A
- 애니메이션 속도(스윙 주기) 세밀 조정은 별도 포커스 후보
