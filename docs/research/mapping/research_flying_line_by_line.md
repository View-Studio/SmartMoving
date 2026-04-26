# 비행 원본 ↔ 1.21.1 라인별 매핑 (Flying Phase)

> **목적**: SmartMoving 비행 처리 (Creative + flyingEnabled) 의 정밀 1:1 라인별 매핑.
> 사용자 보고 10건 (BUG-1+8 + BUG-25~34) 의 직접 원인 확정 + 일괄 정정 위한 진단 자료.

> **작업 prompt**: [`docs/fix/flying_phase_prompt.md`](../../fix/flying_phase_prompt.md)
> **BUG 등재**: [`docs/fix/integration_test_bugs.md`](../../fix/integration_test_bugs.md)

> **사용자 지시 (2026-04-26 세션 37)**: 비행 처리 정밀 1:1 매핑 — 추측 기반 수정 회피, 라인별 비교로 모든 차이점 식별.

> **분류 5종**: [정합] / [오역] / [누락] / [잉여] / [N/A]

---

## 진행 상태

| F-단계 | 대상 | 상태 | 세션 |
|---|---|---|---|
| F-1 | 원본 SmartMovingSelf 비행 처리 (handleAlternativeFlying + handleLand 비행 분기 + standupIfPossible + wasFlying 엣지 + flyWhileOnGround) | ⏳ 대기 | 38 예정 |
| F-2 | 원본 SmartMovingBase.moveFlying L56-L93 + HorizontalAirDamping | ⏳ 대기 | 38 예정 |
| F-3 | 원본 SmartMovingModel isFlying 분기 L474-L520 | ⏳ 대기 | 38 예정 |
| F-4 | 1.21.1 SmartMovingFlyer + sm_animateFlying + sm_setupTransforms + sm.isFlying 갱신 + standupIfPossible 모두 read + 매핑 | ⏳ 대기 | 39 예정 |
| F-5 | 차이점 표 + 사용자 보고 10건 → BUG 매핑 | ⏳ 대기 | 39 예정 |
| F-6 | 일괄 정정 (분할 가능) | ⏳ 대기 | 39+ 예정 |
| F-7 | 빌드 + 사용자 인게임 검증 인계 | ⏳ 대기 | 40 예정 |

---

## 사용자 보고 10건 (세션 35 + 37)

| BUG | 증상 (사용자 보고) | 우선순위 | F-N 매핑 (예상) |
|-----|-------|---|---|
| 1+8 ✅ | 비행 시 몸 고정 / SM 비행 시스템 미작동 | 🔴 | F-4 (player.move 호출 누락 / 세션 37 해결) |
| 25 | 비행 속도 원본보다 느림 | 🔴 | F-1/F-4 (speedFactor 계산) |
| 26 | 땅에 닿아도 착지 안 됨 | 🔴 | F-1 (standupIfPossible / tryLanding) |
| 27 | 위아래 보면서 전진 속도 너무 느림 | 🟠 | F-2 (moveFlying 정규화) |
| 28 | 처음 비행 시 하늘 끝까지 날아감 | 🔴 | F-1 (jump 분기 / 0.91F 감쇠) |
| 29 | 비행 진입 뚝 끊김 | 🟠 | F-1 (heightOffset / wasFlying 엣지) |
| 30 | 비행 가만히 있을 때 팔 회전 축 다름 | 🟠 | F-3 (isFlying 팔 setAnglesXZY) |
| 31 | 몸 기울기 방향 다름 (몸 앞쪽이 하늘) | 🔴 | F-3 (sm_setupTransforms theta 부호) |
| 32 | 머리 이상하게 고정 | 🟠 | F-3 (head.pitch ANIM-01) |
| 33 | 비행 애니메이션 프레임 끊김 | 🟠 | F-4 (stats.calculate timing) |
| 34 | 비행 디테일 (각도/움직임/속도/스무스함) 다름 | 🟢 | F-3/F-4 (전반 1:1) |

---

## F-1: 원본 SmartMovingSelf 비행 처리 (대기 — 세션 38 예정)

대상 라인:
- L80: `if(sp.capabilities.isFlying && !Config.isFlyingEnabled())` — 비행 가드
- **L602-L631**: `handleAlternativeFlying` 본체
- L633-L663: `handleLand` 비행 분기 (L637-L640 sprint+jump 가속)
- L1803-L1831: `beforeOnLivingUpdate` + `afterOnLivingUpdate` (flyWhileOnGround / wasCapabilitiesIsFlying)
- L2186-L2212: `standupIfPossible(boolean tryLanding, boolean restoreFromFlying)`
- L2320: `boolean isLevitating = sp.capabilities.isFlying && !isFlying;`
- L2404: `if(esp.capabilities.isFlying && (Config.isFlyingEnabled() || Config.isLevitateSmallEnabled())) mustCrawl = false;`
- L2509+: wasFlying 저장 + 엣지 처리
- L2542+: `tryLanding` 계산 + standupIfPossible 호출

→ 다음 세션 작업.

---

## F-2: 원본 SmartMovingBase.moveFlying + HorizontalAirDamping (대기 — 세션 38 예정)

대상 라인:
- L56-L93: `moveFlying(moveUpward, moveStrafing, moveForward, speedFactor, treeDimensional)` 본체
- HorizontalAirDamping 상수 정의

→ 다음 세션 작업.

---

## F-3: 원본 SmartMovingModel isFlying 분기 L474-L520 (대기 — 세션 38 예정)

대상 라인:
- L474-L520: isFlying 분기 (sm_animateFlying 본체 매핑)
- L477-L479: distance/walkFactor/standFactor 입력값
- L480: time = totalTime * 0.15F
- L481-L482: verticalAngle/horizontalAngle 계산
- L484-L520: 팔/다리/몸/머리 자세 계산

→ 다음 세션 작업.

---

## F-4: 1.21.1 비행 처리 모두 read + 매핑 (대기 — 세션 39 예정)

대상 파일:
- `SmartMovingFlyer.java` (130 라인) — 비행 motion 처리 + moveFlying 정규화
- `SmartMovingMover.java` — getCombinedSpeedFactor / getConfigSpeedFactor / getPotionSpeedFactor
- `SmartMovingClientState.java` — sm.isFlying 갱신 (L1298) / standupIfPossible (L2575+) / tryLanding (L1338)
- `MixinLivingEntityClient.java` — handleFlying 호출 (L158) + sprint 점프 (L172)
- `MixinClientPlayerEntity.java` — sm_flyWhileOnGround (L43)
- `MixinPlayerEntityModelClient.java` — sm_animateFlying (L575+)
- `MixinPlayerEntityRenderer.java` — sm_setupTransforms isFlying 분기

→ 다음 세션 작업 (F-1~F-3 완료 후).

---

## F-5: 차이점 표 + 사용자 보고 10건 → BUG 매핑 (대기 — 세션 39 예정)

F-1~F-4 완료 후 모든 [오역]/[누락]/[잉여] 종합 + 사용자 보고 10건과 매핑.

---

## F-6/F-7 (대기)

F-5 완료 후 일괄 정정 + 빌드 + 인게임 검증.

---

## 진행 메타

- **세션 37 (2026-04-26)**: 본 산출물 + flying_phase_prompt 초기 구조 작성. F-1~F-7 모두 ⏳ 대기.
- **세션 38+**: F-1~F-3 (원본 read + 매핑) 진입.
- **세션 39**: F-4~F-5 (1.21.1 매핑 + BUG 매핑).
- **세션 39+**: F-6 (정정).
- **세션 40**: F-7 (인게임 검증 인계).
