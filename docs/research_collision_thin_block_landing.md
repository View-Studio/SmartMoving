# 콜리전 BUG — 공중 1칸 두께 블록 위 헤드점프/슬라이딩 착지 시 바닥 뚫고 떨어짐

> **상태**: deferred (= 별도 처리). 디버그 dump 필요.
> **보고 일자**: 2026-05-11
> **관련 영역**: 헤드점프/여우무빙/슬라이딩 종료 처리, 박스 transition, mixin offset 활성/차단 전환.

---

## 1. 사용자 보고 시나리오

> 공중에 1칸 두께로 깔아둔 블록 위에서 헤드점프 (여우무빙 포함) 또는 슬라이딩 도중 → 갑자기 착지할 때 바닥 (= 1칸 두께 블록) 을 뚫고 아래로 떨어짐.

전에도 한두 번 발생 보고된 같은 종류 BUG. 클라/서버 양쪽 의심.

## 2. 시나리오 물리 분석

| 상태 | entity.y | mixin offset | 박스 발 |
|------|----------|--------------|---------|
| STANDING | ground | 차단 (height=1.8) | entity.y |
| 헤드점프 (POSE=SLIDING) | ground (push 없음) | 활성 (height=0.8 + eyeHeight=1.62) | entity.y +1m |
| 슬라이딩 (POSE=SLIDING) | ground -1m (move(-1) push) | 활성 | entity.y +1m = ground |
| 엎드리기 (POSE=SWIMMING) | ground | 차단 (eyeHeight=0.62) | entity.y |

mixin offset 가드: `MixinEntity.sm_offsetBoundingBoxForFlying` L122:
```java
if (dim.eyeHeight() <= 1.0F && pose != EntityPose.SLIDING) return;
```
→ 활성 조건: `dim.height() < 1.0F && (dim.eyeHeight() > 1.0F || pose == SLIDING)`.

### 1칸 두께 블록 시나리오 (block top = B+1, block bottom = B, 그 위/아래 공중)

1. 블록 위 (entity.y = B+1) 에서 헤드점프 발사.
2. 점프 → 박스 = (entity.y+1, entity.y+1.8). entity.y 상승/하강.
3. 다시 블록 위 착지 (entity.y = B+1, onGround=true).
4. 헤드점프 종료 처리 → POSE 변경 → mixin offset 차단 → 박스 1m drop.
5. **어딘가에서 entity.y 가 B+1 보다 낮게 들어가서 박스가 block 아래 공간으로 떨어짐.**

## 3. 정적 분석 결과 (Agent 추적, 2026-05-11)

추적한 의심 영역:
- `SmartMovingClientState.java` L2180 헤드점프 5-AND 매치 분기.
- `standupIfPossible` L3640 standUp + L3786 toSlidingOrCrawling 분기.
- L3861 fix #25/#30 안전망 push 분기.
- L3935 fix #71 안전망 push 분기.
- L2258 SS-SlideStop fix #70 predictive drop push.
- `SmartMovingServer.java` L193-224 processStatePacket calculateDimensions.
- fox movement case B (= wasHeadJumping=false 강제) → 5-AND 매치 X 경로.

**결론**: 모든 분기가 1칸 두께 블록 시나리오에서 **표면상 정상 작동**. 정적 분석만으로 root cause 확정 불가.

## 4. 가장 유력한 의심점

### §4-A — fix #25/#30 안전망 push 분기 (L3861-3903)
**파일**: `SmartMovingClientState.java:3861-3903`

5-AND 매치 후 `justEndedHeadJump=true` + sneak/grab hold → `toSlidingOrCrawling` → `isSliding=true`. 그 후 fix #25/#30 매치:
```java
if (this.justEndedHeadJump && wasSmallBox && (this.isCrawling || this.isSliding)) {
    player.calculateDimensions();   // isSliding 분기 → eye=1.62, height=0.8
    Box bbAfter = player.getBoundingBox();   // mixin 활성 → bb.minY = entity.y+1
    ...
    boolean isMixinOffsetActive = _dim25.height() < 1.0F
            && (_dim25.eyeHeight() > 1.0F || player.getPose() == EntityPose.SLIDING);
    boolean stuck = !isMixinOffsetActive && bbAfter.minY < solidUnder - 1.0E-5;
    if (stuck) { push +1.0; }   // ← isMixinOffsetActive=true 라 push 안 함
```

**의심 흐름**: entity.y = B (block top -1) 잔존. 박스는 mixin 활성으로 (B+1, B+1.8) 정상 표시되지만 *entity.y 자체* 가 1m 낮음. 어떤 후속 처리에서 mixin offset 차단 + push +1m 안 되는 race window 가능.

### §4-B — server dim cache 잔존 race
**파일**: `SmartMovingServer.java:193-224`

`isHeadJumping=true → false` decode 시 `calculateDimensions()` skip (조건: `if (newHeadJumping)`). isSliding 변경이 다른 tick 으로 분리 송신되면 1 tick mismatch window — server 박스 발 vs client 박스 발 불일치 → server reconcile 가능성.

### §4-C — fix #70 push 후 박스 박힘 보정 누락
**파일**: `SmartMovingClientState.java:2318-2346`

SS-SlideStop fix #70 push 후 박스 박힘 검사 없음. push 양이 정확히 1m 가 아닌 경우 (epsilon error 또는 1칸 두께 블록 *아래* 다른 솔리드 있는 경우) 박스 박힘 가능.

## 5. 다음 단계 — 디버그 dump 필수

`feedback_debug_log_first.md` 메모리: **추측 13회 vs 로그 1회 비용 차이**. 정적 분석만으로 더 진행 = 추측 누적.

### dump 추가 위치
- `SmartMovingClientState.java` L2180 5-AND 매치 분기.
- L2258 SS-SlideStop.
- L3640 standUp.
- L3861 fix #25 안전망.
- L3935 fix #71 안전망.
- L2318 fix #70 push.
- `SmartMovingServer.java` L193+ processStatePacket.

### dump 내용
- tick number.
- entity.y, lastRenderY, prevY.
- bb.minY, bb.maxY.
- POSE, dim height, dim eyeHeight.
- isHeadJumping, isSliding, isCrawling, wasHeadJumping, justEndedHeadJump, heightOffset, restoreFromFlying.
- onGround, fallDistance.
- solidUnder (yMax=bb.minY+1.5).

### 검증 시나리오
1. 공중 1칸 두께 블록 (= 한 줄 솔리드) 깔기.
2. 블록 위에서 헤드점프 발사 → 다시 착지.
3. 블록 위에서 슬라이딩 진행 → 종료.
4. 블록 위에서 헤드점프 + sneak/grab hold → 자체 슬라이딩 진입.
5. 블록 위에서 여우무빙 (= 헤드점프 후 자동 슬라이딩 전환) → 종료.

각 시나리오 dump 결과 `docs/log_temp.txt` 에 붙여넣기. 정확 분기 + state + push 양 + 박스 발 위치 추적.

## 6. fix 방향 후보 (디버그 dump 결과 후 결정)

1. **§4-A**: fix #25/#30 안전망의 `isMixinOffsetActive` 가드 식 재검토. 1칸 두께 블록 시나리오에서 stuck 검사 누락.
2. **§4-B**: `SmartMovingServer:193-198` `isHeadJumping=true→false` 종료 시 무조건 `calculateDimensions` 호출.
3. **§4-C**: fix #70 push 후 fix #71 안전망 1:1 적용 (= push 후 박스 박힘 보정).

## 7. 관련 메모리 (이전 fix 들)

- `feedback_mixin_offset_box_drop_transition.md` — mixin offset 차단 → 박스 1m drop 패턴.
- `project_transition_box_drop_complete.md` — transition fix 7 정착 (2026-05-10).
- `feedback_pushup_lastRenderY_prevY_sync.md` — push up 4-곳 동기화.
- `feedback_safetynet_ymax_clamp_ceiling.md` — pushY ≤ 1m 가드.
- `feedback_solid_under_ymax_clamp.md` — yMax clamp 회피.
- `feedback_pushup_stuck_guard.md` — push up stuck 가드.
- `feedback_fix22_isSliding_branch_skip.md` — fix #22 분기 skip.
- `feedback_restoreFromFlying_5and_clear.md` — restoreFromFlying clear 누락 fix #72.
- `feedback_processStatePacket_calculateDimensions.md` — server processStatePacket calculateDimensions 필수.
- `feedback_server_reconcile_box_sync.md` — server reconcile 박스 sync 필수.
- `project_headjump_complete.md` — 헤드점프 완결.
- `project_fox_movement_complete.md` — 여우무빙 완결.
- `project_sliding_complete.md` — 슬라이딩 완결.

---

**문서 끝.**
