# jump 차징 중 sneak 누름 시 BUG 리서치 (2026-05-12)

## 사용자 보고

> w+sprint+grab+jump 차징 중에 jump release 안 하면서 sneak 누르면 갑자기 그 자리에 뚝 멈추면서 땅에서 1칸 띄어진 상태의 엎드리기 + 중력으로 떨어짐. 콜리전 잠깐 위로 → 다시 plyer 로 돌아옴.

## 가설 (Frame 추적)

### Frame N (sneak start edge)
1. `tickEssential` 안 **자체 슬라이딩 발사 분기 매치** (`SmartMovingClientState` L2055~):
   - `cfg.slide && grab.isPressed() && wasGroundSprinting && !isCrawling && sneakKeyStartPressed && !isDipping`
   - 모든 조건 ✓ → 매치.
2. 효과:
   - `heightOffset = -1F`
   - `isSliding = true`
   - `isHeadJumping = false`
   - `player.calculateDimensions()` → POSE=SLIDING + dim 0.6×0.8.
   - `player.move(0, -1D, 0)` → entity.y -= 1m, 박스 발 = ground 정렬.
   - `tryJump(SLIDE_DOWN)` → horizontalMotion 부스트, vY=-0.078.

### Frame N+1 (사용자 sneak hold)
3. vanilla **sneak → sprint cancel** (1.21.1 vanilla 식) → motion 감속.
4. `tickEssential` SS-SlideStop 매치 (L2275~):
   - `isSliding && (!sneakPressedRaw || hSpd² < stopFactor*0.01)`
   - sneak hold (= raw 키 누름) → 첫 조건 X.
   - hSpd² 감속 → 두 번째 매치.
   - → `isSliding=false` + `toCrawling()` → `isCrawling=true`.
5. **fix #62 v2** (L2337~) 매치:
   - 가드: `!isHeadJumping && heightOffset==-1F` ✓
   - `_willDrop = (box.minY - entity.y) > 0.5` = 1.0 > 0.5 ✓ (POSE=SLIDING 잔존, mixin 활성, 박스 발 = entity.y+1)
   - → **setPos(y+1)** → entity.y = ground (push +1m).
   - heightOffset=0, calculateDimensions, cameraY=0.62.

### Frame N+2
6. vanilla updatePose → POSE: SLIDING → SWIMMING (= isCrawling 매핑).
7. dim=0.6×0.6 → mixin offset 차단 → 박스 발 = entity.y = ground. **박스 ground 위 (정상)**.

## 사용자 시각 결과 정리

| frame | 박스 발 | 모델 origin | 사용자 시각 |
|---|---|---|---|
| N | ground (= entity.y+1, mixin 활성) | ground-1 | 슬라이딩 진입 |
| N+1 (push 전) | ground (mixin 활성) | ground-1 | 슬라이딩 잔존 |
| N+1 (push 후) | **ground+1** (mixin 활성, entity.y push) | **ground** | "콜리전 위로 1칸" |
| N+2 (POSE 갱신) | **ground** (mixin 차단) | ground | "콜리전 돌아옴 + 엎드리기" |
| N+3~ | (gravity 떨어짐) | gravity | "땅으로 떨어짐" |

## 진단 필요 dump

| 위치 | 정보 |
|---|---|
| tickMovement HEAD/h2/TAIL | SM state, 박스, entity.y, heightOffset, vY, onG, POSE, dim |
| 자체 슬라이딩 발사 분기 진입 (L2055) | 매치 시점 + grab/sneak/sprint/wasGroundSprinting |
| SS-SlideStop 매치 (L2275) | hSpd², sneak, 결과 (isSliding=false set, toCrawling) |
| fix #62 v2 push (L2337) | _willDrop 계산값, push 전후 entity.y/box |

## 가설 검증 단계
1. Frame N 자체 슬라이딩 발사 매치 + 사용자 입력 확인.
2. Frame N+1 SS-SlideStop 매치 + hSpd² 측정.
3. Frame N+1 fix #62 v2 push +1m 발동 확인.
4. Frame N+2 POSE/박스 상태 확인.

## 관련 코드 / 메모리
- `SmartMovingClientState` L2055~ (자체 슬라이딩 발사), L2275~ (SS-SlideStop), L2337~ (fix #62 v2 push).
- `feedback_pushup_lastRenderY_prevY_sync.md` — push 4-곳 동기화 패턴.
- `feedback_mixin_offset_box_drop_transition.md` — POSE 변경 시 박스 drop.
- `feedback_slide_crawl_concurrent_state.md` — isSliding + isCrawling 동시 BUG.
