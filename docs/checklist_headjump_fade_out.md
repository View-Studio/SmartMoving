# 헤드점프 fade Y 회전 fix #91 — 체크리스트

## 배경
사용자 보고: 헤드점프 중 벽에 부딪혀 옆으로 튕길 때 모델 회전 (rotateAngleY) 의 중간 이어짐이 없음. 1.12.2 에는 부드러운 lerp 있음.

분석: 원본 SmartRenderModel L208-209 `bipedOuter.fadeRotateAngleY = true` (= 기본). 우리 1.21.1 매핑의 isHeadJumping/isRopeSliding 분기에서 fade 적용 누락 → instant set.

## 작업 단계

- [x] **단계 1**: 원본 1.7.10 / 1.12.2 코드 검토 — `docs/research_headjump_fade_out.md`
  - 1.7.10/1.12.2 동일 식 확인.
  - 원본 `fadeRotateAngleY = true` (EntityPig 외 기본).
  - `GetIntermediateAngle` 의 `prev + (target - prev) * deltaTime * 0.2F` 식.

- [x] **단계 2**: state field 추가 — `SmartMovingClientState.java`
  - `smHeadJumpYaw_prev` (= 직전 frame lerp 결과, 라디안)
  - `smHeadJumpYawFade_prevTime` (= 직전 frame totalTime)

- [x] **단계 3**: isHeadJumping/isRopeSliding 분기 fade 적용 — `MixinPlayerEntityRenderer.java` L342-
  - `lerpFadeAngle(smHeadJumpYaw_prev, currentHorizontalAngle, ..., animationProgress)`
  - prev 갱신.

- [x] **단계 4**: 외 분기 prev 매 frame 갱신 추가 — `MixinPlayerEntityRenderer.java` L815-
  - `!isHeadJumping && !isRopeSliding` 시 `prev = 직전 bodyYaw` (= 비행 prev 갱신 패턴).
  - 진입 첫 frame 자연 시작점.

- [x] **단계 5**: 컴파일 검증.

- [ ] **단계 6**: 사용자 인게임 테스트
  - 헤드점프 + 벽 박음 → 몸 회전 부드러움 확인.
  - 자체슬라이딩 발사 시각 회귀 X 확인.
  - 일반 헤드점프 (= 벽 안 박은) 시각 회귀 X 확인.
  - 헤드점프 → 슬라이딩 자동 전환 (여우무빙) 회귀 X 확인.

- [ ] **단계 7**: self/remote 검증.
  - self side 1인칭 무관 (= 모델 안 보임). 3인칭 시각 변화 확인.
  - remote side 시각 (= 다른 플레이어 헤드점프) 부드러움 확인.

- [ ] **단계 8**: 커밋.

## 회귀 위험 (분석)

| 시나리오 | 영향 | 위험도 |
|----------|------|--------|
| 헤드점프 진행 중 + 벽 박음 | 자연 fade lerp 작동 (= fix 의도) | 없음 |
| 헤드점프 진입 frame | 마우스 yaw → 이동 방향 5 tick lerp. 원본 동일 | 없음 (원본 1:1) |
| 자체슬라이딩 발사 (=fix #81) | thetaTarget=π/2 강제 (= X 회전). Y 회전 무관 | 없음 |
| 헤드점프 → 슬라이딩 자동 전환 | isSliding 분기 매치 (= 헤드점프 fade 분기 미진입). 슬라이딩 fade 없이 instant 유지 | 현재와 동일 |
| 헤드점프 종료 → standing | 외 분기 진입. fade out 없음 (= 원본 동일) | 없음 |

## 메모리 참조
- [[research-headjump-fade-out]] (= 이 작업 리서치)
- [[feedback-headjump-animation-scope]] (= setAngles 영역 한정, 시각 변경 OK)
- [[feedback-fade-prev-pre-entry-pose]] (= fade prev 시작값 = 진입 직전 자세)
- [[project-headjump-final]] (= fix #79~#89 정착, fix #91 추가)
