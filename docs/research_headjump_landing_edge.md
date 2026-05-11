# 헤드점프 착지 모서리 잠김 BUG 리서치 (2026-05-12)

## 사용자 보고

> 헤드점프 착지 시 블록의 완전 완전 끝지점에 걸치면서 착지하면서 앞으로가서 그 아래 땅에 착지하면 땅 아래로 잠기는 문제

= 헤드점프 진행 중 → **블록 모서리** 박스 일부 걸침 → 사용자 forward → 모서리 넘어 아래 ground 에 착지 → **모델이 ground 아래로 잠김**.

## 가설

### 가설 A — heightOffset 잔존
헤드점프 종료 시 `standupIfPossible` 가 `heightOffset` 적절히 reset 못 함 → mixin offset 활성 잔존 → 박스 발 = entity.y +1m. 그 후 forward 이동 + 아래 ground 착지 → 박스 발 = new ground → entity.y = new ground -1m → 모델 origin (= entity.y) = ground -1m → **모델이 1m 잠김**.

### 가설 B — standupIfPossible 분기 잘못 매치
모서리 frame 에 `groundClose` (gap < 1) + `standUpPossible` (gap + overGap >= 1) 검사 결과 잘못 매치 → resetHeightOffset 분기 또는 standUp 분기 잘못 호출.

### 가설 C — setPos(y+1) 시점 mismatch
`justEndedHeadJump=true` 잔존 1 tick → 그 1 tick 에 standupIfPossible 다시 호출 → setPos(y+1) 더 push → 위치 어긋남.

### 가설 D — fix #19 (justEndedHeadJump 1-tick setPos) edge case
모서리 frame 시 박스 발 = block top → onGround=T → 헤드점프 종료 매치 → setPos(y+1). 다음 tick forward → 모서리 넘어 → 떨어짐. 박스 발 = new ground (= 1m down). entity.y 가 setPos(y+1) 잔존 → 위치 mismatch.

## 진단 필요 로그

| 필드 | 의미 |
|---|---|
| `isHeadJumping` / `wasHeadJumping` | 헤드점프 phase |
| `justEndedHeadJump` | 1-tick 종료 플래그 |
| `entity.y` / `lastRenderY` / `prevY` | 위치 |
| `box.minY` / `box.maxY` | 박스 |
| `heightOffset` | mixin offset 결정 |
| `onGround` / `fallDistance` | 착지 검출 |
| `vY` | 낙하 속도 |
| `POSE` / `dim` | 박스 차원 |
| **standupIfPossible 분기 매치** | 어느 sub-branch |

## 진단 시나리오

1. 헤드점프 진입 (W+sprint+grab+jump release + sneak 또는 평지 jump 차징).
2. 절벽 모서리 향해 진행.
3. 모서리에 박스 일부 걸침 + sneak hold 유지하며 forward.
4. 모서리 넘어 아래 ground 착지.
5. 사용자 시각 = 모델이 ground 아래로 잠김 보고.

## 관련 메모리 / 코드

- `feedback_pushup_lastRenderY_prevY_sync.md` — push up 시 4-곳 동기화 (entity.y, lastRenderY, prevY, cameraY).
- `feedback_pushup_stuck_guard.md` — push 가드.
- `project_transition_box_drop_complete.md` — fix #65~#71 transition box drop.
- `feedback_mixin_offset_box_drop_transition.md` — eye=1.62 → 0.62 전환 시 box drop.
- `SmartMovingClientState.standupIfPossible` (L3756~).
- `SmartMovingClientState.handleCrash` (L4010~).
- `SmartMovingClientState` L2196 헤드점프 종료 5-AND 분기.
- `MixinEntity.sm_offsetBoundingBox` 가드.
