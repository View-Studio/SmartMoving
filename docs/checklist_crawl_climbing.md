# Crawl-Climbing 작업 체크리스트

`docs/research_crawl_climbing.md` 기반.

## ⚠ 엄격 가드

- `sm_animateClimbing` 의 isCrawlClimbing 추가 분기 (L642-L673) 만 수정.
- isClimb 일반 매핑 (L465+) 영향 없음.
- crawl-climbing 기능 (state/box/collision) 절대 침범 X.

## Phase 1: setAngles 단일 노드 부모 효과 합산 fix

- [ ] `head.pitch = -bodyAngleX` → `head.pitch = 0f` (= 부모 + cancel = 무회전).
- [ ] `rightLeg.pitch = legAngleX` → `rightLeg.pitch = bodyAngleX + legAngleX` (= 부모 + local 합산).
- [ ] `leftLeg.pitch = legAngleX` → `leftLeg.pitch = bodyAngleX + legAngleX`.
- [ ] `rightArm.pitch += -bodyAngleX` 제거 (= 부모 + shoulder cancel = vanilla 결과).
- [ ] `leftArm.pitch += -bodyAngleX` 제거.
- [ ] body.pitch / leg.roll / leg.yaw 그대로 유지.

## Phase 2: 인게임 검증

- [ ] 빌드 성공.
- [ ] 사용자 테스트 — 좁은 천장 (height < 0.7) crawl-climb 자세가 원본과 일치.
- [ ] 모델 위로 올라옴 효과 사라짐.
- [ ] 다리 정상 앞으로 회전.
- [ ] head 무회전 (= mouse pitch 영향 X).

## Phase 3: 메모리 + 커밋

- [ ] 사용자 OK 시 커밋.
- [ ] 메모리 기록 (bipedTorso 부모 효과 매핑 패턴).
