# Remote 헤드점프 회전 부족 BUG — 완결 (2026-05-14)

## 최종 상태: ✅ 완결

사용자 명시: "이제 괜찮은거 같음" (2026-05-14).

## 적용된 fix 3 단계

### fix #97 — StatsPayload (= self stats angle 매 tick packet 동기화)

5 파일 변경: SmartMovingNetwork (= payload type), SmartMoving (= server receiver + broadcast), SmartMovingClient (= client receiver), SmartMovingClientState (= field), MixinPlayerEntityClient (= sm_tickStatsForRemote TAIL 덮어쓰기), MixinClientPlayerEntity (= sendPacket).

원래 BUG 해소: vanilla lerpPosAndRotation 5-tick 분할이 REMOTE 의 realDxYz 평탄화 → curVAng trajectory self 와 다른 흐름 → fade peak 추격 부족.

### fix #97-v2 — packet receive 안 stats 직접 즉시 set

vanilla MinecraftClient.tick 순서:
1. networkHandler.tick (= packet callback queue).
2. world.tick → entity.tick → sm_tickStatsForRemote (= stats.calculate + 덮어쓰기).
3. processTasks → client.execute lambda → fromPacket field set.
4. render → setupTransforms.

step 2 시점 fromPacket=NaN → 덮어쓰기 skip → stats 자체 atan2 결과 잔존 (= 0). 진입 frame target=π/2=90° 즉시 점프 → 사용자 보고 "진입 끊김".

해결: packet receive lambda (= step 3) 안에서 fromPacket + sm.stats 둘 다 즉시 set.

### fix #98 v5 — 공중 분기 setPos(prevY) + 4-동기화 + lerp cancel + 자세 visual hold + 해제식 정정

SmartMovingClient packet 처리 안 fix #92 가드 분리:
- 잠긴 상태 (= curY < groundY-0.005): fix #92 그대로 setPos(groundY) + 4-동기화 + lerp cancel.
- 공중 상태 (= curY > groundY+0.005): setPos(prevY=공중 위치) + 4-동기화 + lerp cancel + 자세 visual hold flag set.

`curY = remoteHJ.prevY` (= 이전 tick final y, lerp 진행 **전**) 사용 — `getY()` 는 step 2 lerp 진행 후 결과라 ground 도달.

자세 visual hold:
- setupTransforms X/Y 회전 분기 가드 OR `|| smRemoteHJVisualHold`.
- reset 가드 AND `&& !smRemoteHJVisualHold`.
- 해제식 `getY() >= groundY-0.005` (= ground **이상** 도달 시. 잠긴 시 `< ground` 매치 X → hold 유지).

## 5 가지 사용자 BUG 해소 매핑

| BUG | fix |
|---|---|
| 1. 헤드점프 사이클 마지막 머리 바닥 안 향함 | fix #97 (= stats trajectory self 와 일치) |
| 2. 진입 시 끊김 (90° 즉시 점프) | fix #97-v2 (= 진입 frame stale 차단) |
| 3. 콜리전 땅 닿기 전 standing 자세 | fix #98 v5 (= 자세 hold + 위치 ground 도달까지) |
| 4. 헤드점프 착지 시 잠김 | fix #98 v5 (= setPos(prevY) + lerp cancel) |
| 5. 도중/착지 후 부드럽지 못함 | fix #97/v2/#98 v5 부수 효과 |

## 시행착오 정리 (= 적용 안 한 fix)

- **fix #98 P1 (tick timeout)** — 5 tick timeout 자세 hold. 사용자 "틱기반 답없다" → 롤백.
- **fix #98-v2 server side mirror** — SmartMovingServer.processStatePacket 의 isHJ=true→false 시 setPos(+1m). 효과 broadcast 시점 안 잡힘 (= vanilla physics gravity 즉시 down). 잔존.
- **fix #98 v3 (getY 사용)** — packet 처리 시점 getY() 가 lerp 진행 후 결과 → 공중 검출 X. prevY 사용 (v3) 으로 정정.
- **fix #98 v4 (해제식 정정)** — `getY <= groundY+0.005` → `getY >= groundY-0.005`. 잠긴 시 매치 X.

## 관련 메모리

- `project_remote_headjump_anim_sync` — 완결 사실 + 코드 상세.
- `feedback_packet_callback_vs_entity_tick_order` — vanilla tick 순서.
- `feedback_hold_release_ground_above_check` — 해제식 패턴.
- `feedback_remote_stats_packet_self_mirror` — stats packet 동기화 패턴.

---

## 작업 재개 컨텍스트 (= 원본, 참고용)



## 사용자 보고 (verbatim)

> 다음 문제, 상대가 헤드점프할 때 리모트에서 상대 헤드점프 에니메이션이 부드럽게 나오지 않음.
> 그리고 상대가 여우무빙할 때 리모트에서 셀프에서 처럼 바로 몸이 수평으로 세팅되지 않음.

→ 여우무빙 부분은 **fix #96 (= wasSelfSlideFire packet bit 35 동기화)** 로 해결 완료.

> 헤드점프 페이드보간이 잘 안되는거 같은 버그 ... 원래 셀프에서는 헤드점프가 몸이 포물선으로 점프를 해서
> 마지막 쯤에는 몸이 머리가 바닥을 향하는 대각선 느낌인데 리모트는 머리가 바닥을 향하기 전에 끝나는 거 같음

= 진행 중인 작업. 본 문서 주제.

## 사용자 운영 원칙 (verbatim, 절대 준수)

- "무조건 확신할 수 있을 때 수정하는거고(당연히 전의 모든 기능들이 회귀하면 안됨)"
- "만약에 정보가 부족하면 무조건 로그를 디벨롭해"
- "완전 꼼꼼히 실제로 읽고, 실제로 분석해서 문제 찾아보고"
- `docs/log_temp.txt` 절대 삭제 금지 (= 사용자 인게임 로그 영구 파일)

## 사용자 직접 결정 (2026-05-14 본 세션 끝)

> 차라리 추가 로그를 더 깔아서 원인을 확실하게 특정짓는게 좋다고 판단함.

→ **다음 세션 1단계 = dump 식 강화 + 재테스트**. 코드 수정 보류.

## 현재 추가되어 있는 dump 위치

### 1. MixinPlayerEntityRenderer.java X 회전 분기 (L766-805 부근)

```java
// SELF
LOGGER.info("[TX-HJ-ANIM-SELF] uuid={} tick={} pt={} wasSSF={} curVAng={} curHAng={} thetaT={} thetaL={} prev={} vx={} vy={} vz={} hSpd={} y={} prevY={} lastRY={}",
    ...);
// REMOTE
LOGGER.info("[TX-HJ-ANIM-REMOTE] uuid={} ...");  // 동일 schema
```

### 2. MixinPlayerEntityRenderer.java Y 회전 분기 (L348-372 부근)

```java
LOGGER.info("[TX-HJ-YAW-SELF/REMOTE] uuid={} tick={} pt={} wasSSF={} snapDone={} targetY={} laggedY={} prev={}", ...);
```

5 frame 마다 1회 출력 (= tick mod 5 == 0).

## docs/log_temp.txt 분석 결과 (271 lines, 1회차)

### 핵심 트라젝토리 비교

| 측면 | tick | curVAng peak | vy (dump) | hSpd | thetaT | thetaL peak | 시각 결과 |
|---|---|---|---|---|---|---|---|
| **SELF** | 225 | **-1.234 rad** | -0.514 | 0.142 | 2.805 rad (**160°**) | ~2.5 | 머리 바닥 향함 ✓ |
| **REMOTE** | 330 | -0.497 | 0.420 (stale!) | 0.200 | 2.068 (118°) | 1.290 | 부족 |
| **REMOTE** | 465 | **-0.953 rad** | 0.420 (stale!) | 0.200 | 2.524 (**145°**) | **1.755** | thetaL 가 따라잡지 못함 |

### 핵심 발견

1. **REMOTE 의 curVAng peak 가 SELF 의 77%** (-0.953 vs -1.234). 그 자체로도 부족.
2. **REMOTE 의 thetaL 가 thetaT 따라잡지 못함**: tick 465 thetaT=2.524 vs thetaL peak=1.755 → 0.77 rad (44°) 모자람.
3. **REMOTE 의 dump vx/vy/vz 가 매 tick 동일 -0.109/0.420/0.168 stale** (line 218-262). 실제 y 는 변동.
4. **REMOTE 의 curVAng/thetaT 가 5 tick 1회 계단형 갱신** (tick 325→330→375→455→460→465).

## Root Cause 가설 두 갈래 (확정 안 됨)

### 가설 A: stats 입력 식 차이

| 측면 | 입력 식 | 위치 |
|---|---|---|
| SELF | `sm.stats.calculate(prevX, prevY, prevZ, getX, getY, getZ, yaw)` | `MixinEntityClient.java:54` |
| REMOTE | `sm.stats.calculate(0,0,0, dx, dy, dz, yaw)` where `dx=getX-prevX` | `MixinPlayerEntityClient.java:370` |

두 식은 **수학적 동등** (= 둘 다 diffY = getY - prevY 계산). 단 SELF 는 vanilla movement physics 가 매 tick 정확한 y 업데이트, REMOTE 는 vanilla 1.21.1 `OtherClientPlayerEntity.tick` 의 `lerpPosAndRotation` 가 5-tick 균등 보간 → **`getY - prevY` 가 진짜 vy 가 아니라 lerp step**.

vanilla `lerpPosAndRotation`: 매 tick `step = (target - cur) / lerpSteps_remaining`, lerpSteps 매 tick 감소. = **vy 평탄화 + 5 tick 1회 갱신**.

### 가설 B: lerpFadeAngle 누적 lag

`MixinPlayerEntityRenderer` 의 `lerpFadeAngle` 식이 매 frame `prev + (target - prev) × 0.2 × dt` 추격. REMOTE 의 thetaT 가 5 tick 1회만 갱신 → fade 가 100ms 안 완주 못함 → 갱신 시 prev=중간값 → 다음 lerp 또 추격. **누적 lag**.

→ 두 가설 다 plausible. 확실히 특정 안 됨.

## 사용자 결정: 다음 세션 1단계 — dump 강화

### 추가할 dump 항목

1. **실제 stats 입력값** (= dump 의 vy 가 stale getVelocity 인 것 fix):
   ```java
   double realDx = player.getX() - player.prevX;
   double realDy = player.getY() - player.prevY;
   double realDz = player.getZ() - player.prevZ;
   double realHSpd = Math.sqrt(realDx*realDx + realDz*realDz);
   ```
   기존 vx/vy/vz (= getVelocity) 옆에 realDx/realDy/realDz/realHSpd 같이 표시.

2. **lerpSteps 잔여 카운터** (= 가설 A 검증):
   - `((OtherClientPlayerEntity) remote).<lerp step fields>` 직접 dump.
   - 또는 mixin invoker 로 접근.

3. **stats 내부 prev/cur 필드** (= 가설 A 추가 검증):
   - `sm.stats.prevHorizontalAngle`, `sm.stats.prevCurrentHorizontalSpeed` 등 dump.

4. **tick HEAD/TAIL 의 위치** (= vanilla movement vs SM mixin 의 순서):
   - `MixinPlayerEntityClient.tick TAIL` 의 sm_tickStatsForRemote 호출 시점 vs lerpPosAndRotation 호출 시점.
   - lerpPosAndRotation 이 sm_tickStatsForRemote 보다 늦으면 stats 입력이 한 tick stale.

5. **server-side 처리** (= 사용자 질문 "서버로그는 없어도 되는건가?"):
   - 현재 분석 결론: server 에서 stats 계산 안 함 → server log 필요 없음.
   - 단 server packet broadcast 주기 / vanilla EntityTrackerEntry 의 sync 주기 확인 차원에서는 유용.
   - **다음 세션 의사결정 사항** — 사용자 합의 후 추가.

### 작업 순서

1. [ ] `docs/log_temp.txt` 비우지 말고 (= 사용자 영구 파일) **append 모드**로 신규 dump 사용 안내.
2. [ ] dump 식 강화 (위 1~3번 항목, 4번 항목은 invoker 필요할 수 있음).
3. [ ] 빌드 + 사용자에게 인게임 테스트 요청 ("self 가 헤드점프 + remote 측 1명 관찰").
4. [ ] log_temp.txt 재분석 → 가설 A/B 확정 또는 새 가설 발견.
5. [ ] fix 방향 사용자 합의 (= AskUserQuestion 으로).
6. [ ] fix 적용 → 재테스트 → 메모리 기록 → 커밋.

## 코드 위치 quick reference

| 항목 | 파일 | 라인 (대략) |
|---|---|---|
| SELF stats 식 | `src/client/.../mixin/client/MixinEntityClient.java` | L54 |
| REMOTE stats 식 | `src/client/.../mixin/client/MixinPlayerEntityClient.java` | L352-373 |
| SmartStatistics atan2 | `src/main/.../stat/SmartStatistics.java` | L137 |
| X 회전 dump | `src/client/.../mixin/client/MixinPlayerEntityRenderer.java` | L766-805 |
| Y 회전 dump | `src/client/.../mixin/client/MixinPlayerEntityRenderer.java` | L348-372 |
| wasSelfSlideFire reset | `MixinPlayerEntityRenderer.java` | L595, L829 |
| wasSelfSlideFire set | `SmartMovingClientState.java` | L2144 |
| wasSelfSlideFire packet | `SmartMovingState.java` | L72, L103, L136 |

## 관련 메모리 (= 다음 세션에서 자동 로드되는 메모리 인덱스)

- `project_wasselfslidefire_packet_sync` — fix #96 동작 흐름
- `feedback_tickessential_self_only` — tickEssential 가 self only 인 패턴
- `feedback_remote_motion_correction` — remote 측 self mirror 패턴
- `feedback_self_to_server_remote_one_to_one` — self → remote 1:1 복제 원칙
- `feedback_debug_log_first` — 추측 분기 N회 vs 로그 1회 비용
- `feedback_network_explain_three_sides` — multi BUG 분석 시 server/self/remote 3 분리 의무

## 회귀 방지 — 함부로 수정 금지 (메모리 명시)

- 헤드점프 X 회전 식 (fix #79~#89 정착): `project_headjump_final`
- 헤드점프 Y 회전 fade (fix #91): `project_headjump_yaw_fade_fix`
- 여우무빙 snap (fix #94/#95): `project_foxmovement_yaw_snap_fix`
- wasSelfSlideFire packet (fix #96): `project_wasselfslidefire_packet_sync`

→ remote 헤드점프 회전 부족 fix 시 위 4개 기능 회귀 검증 필수.

## 가설 A 가 맞을 경우 fix 후보

1. **stats packet 동기화**: self 의 stats.currentVerticalAngle/HorizontalAngle 매 tick packet 송신 → remote decode 시 sm.stats 직접 set. lerpPosAndRotation 영향 우회.
   - 장점: self↔remote 완전 동일 trajectory.
   - 단점: packet payload 증가 (Q12 short 2개 = 32bit).

2. **stats 자체 EMA 사용**: 원본 SmartStatistics 에 EMA legYaw 있음 (= currentHorizontalSpeed). REMOTE 의 stats 도 vanilla lerp 결과지만 SmartStatistics 의 EMA 가 평탄화. → 식 안 바꾸고 sm.stats 의 다른 필드 (= currentSpeed, totalDistance 등) 활용.

## 가설 B 가 맞을 경우 fix 후보

1. **lerpFadeAngle factor 증가**: headjump phase 안만 0.2 → 0.5. 매핑 1:1 위반 우려.
2. **fade prev 강제 동기화**: thetaT 갱신 감지 시 prev = (target × 0.7 + prev × 0.3) 같은 mid-blend.

## 다음 세션 첫 메시지 추천

```
docs/checklist_remote_headjump_rotation.md 읽고 작업 이어서.
1단계: dump 식 강화 (1~3번 항목).
빌드 후 사용자에게 인게임 테스트 요청.
```
