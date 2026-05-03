# Crawl 머리 회전 — 작업 체크리스트

`docs/research_crawl_head_rotation.md` 기반.

## ⚠ 엄격 가드

- **에니메이션만 수정**. crawl 기능 (state/box/collision/input) 절대 침범 X.
- 다른 분기 (climbing/sliding/flying 등) head 처리 영향 X.

## Phase 1: head.yaw cancel 추가 (단일 fix)

- [ ] `MixinPlayerEntityModelClient.java` `sm_animateCrawling` (L867-L925) 의 머리 부분에 `head.yaw = 0f;` 추가
- [ ] 주석 추가: "원본 reset 후 isCrawl 분기 미설정 (= 0). vanilla setAngles 의 netHeadYaw cancel."

## Phase 2: 인게임 검증

- [ ] 빌드 성공 확인
- [ ] 사용자 테스트 — 엎드린 상태에서 마우스 좌우 회전이 원본과 같은지 확인
- [ ] 사용자 테스트 — 마우스 상하 회전이 머리에 영향 안 주는지 확인 (원본은 무시)

## Phase 3: cleanup 영향 검토

- [ ] 종료 엣지 cleanup (L247-L262) 에 head.yaw = 0 추가는 불필요 (vanilla 가 다음 frame 즉시 덮어씀). 추가 X.

## Phase 4: 메모리 + 커밋

- [x] 사용자 OK 시 커밋
- [x] 메모리 기록

## Phase 5: 추가 fix (2026-05-03 사용자 보고 정정)

### 5.1 max 작동 fix
- [x] `sm_captureBodyYaw` 의 `!smActive` 분기 위 isCrawl 단독 가드 추가.
- [x] 이전 도달 안 됐던 L298 분기 정리.

### 5.2 부드러운 lag fix
- [x] 원본 `bipedOuter.fadeRotateAngleY = true` + `GetIntermediateAngle` 0.2 lerp 매핑 = `applyFadeAngleDegrees`.
- [x] `smCrawlMode` 신규 flag 추가 (`SmartMovingClientState`).
- [x] `sm_captureBodyYaw` isCrawl 분기: `smStandardFadeActive=true` (body fade lag) + `smCrawlMode=true` (head 보정 skip).
- [x] `sm_modifyNetHeadYaw`: `smCrawlMode` 시 보정 skip → head.roll 가 vanilla netHeadYaw (= max 50° clamp) 그대로 사용.
