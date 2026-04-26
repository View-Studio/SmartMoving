# 비행 처리 정밀 1:1 매핑 — 매 세션 진입 프롬프트

> **사용법**: 새 세션에서 다음 한 줄만 보내면 됩니다.
> ```
> docs/fix/flying_phase_prompt.md 따라서 비행 정밀 매핑 작업 진행해줘.
> ```
> Claude 가 본 파일을 read → 아래 규칙대로 작업 + 직전 세션 종료 위치(§진행 상태)에서 자동 이어받음.

---

SmartMoving 1.21.1 Fabric 포팅의 **비행 처리 (Creative + flyingEnabled)** 정밀 1:1
매핑 + 정정 작업. Phase R/B 패턴 미니 — 라인별 비교로 사용자 보고 10건
(BUG-1+8+25~34) 직접 원인 확정 + 일괄 정정.

## 핵심 원칙 — 1:1 번역 (비행 Phase 특화)

세션 37 사용자 보고 = SM 비행 시스템이 원본과 다름 (속도/착지/축/기울기/머리/스무스함/디테일 등 10건).
세션 37 BUG-1+8 (`player.move()` 누락) 만 해결로는 불충분 — 정밀 1:1 미흡 잔존.

### 1:1 번역 = 추측 회피 + 라인별 매핑

- **추측 기반 수정 금지** — Agent 가설 (BUG-25 NonSlowInput / BUG-31 부호 등) 검증 후만 적용.
- **모든 라인 매핑** — 원본 비행 처리 모든 라인을 1.21.1 매핑 표에 등재 (skip 0).
- **5 분류** — [정합] / [오역] / [누락] / [잉여] / [N/A].
- **발견 즉시 수정 원칙** (메모리 `feedback_fix_immediately.md`) — 매핑 중 차이 발견 시 즉시 정정 또는 정정 BUG 등재.
- **추측 회피 deferred 금지** (메모리 `feedback_no_premature_defer.md`) — vanilla 정확 시그니처 디스어셈블리 + 원본 sm_original 라인별 read.

### 비행 Phase 의 Phase R/B 와 다른 점

- **단일 시스템 (비행)** 한정 — 전체 애니메이션 (Phase R 31 파일 4,968 라인) 보다 작음 (~500 라인 추정)
- **사용자 보고 10건 직접 원인 확정** = 매핑 표의 [오역]/[누락]/[잉여] → BUG 매핑
- **Phase R 같은 청크 분할** 가능 — 단 작은 규모로 1-2 세션 완결 목표

## 직전 세션 종료 위치 (필수 인지)

진입 시 반드시 다음 두 파일 read:
1. `docs/fix/integration_test_bugs.md` 의 BUG-1+8/25~34 §해결 / §진행 상태
2. `docs/research/mapping/research_flying_line_by_line.md` 의 마지막 청크 + 진행 상태

세션별 진행 단계:
- **세션 37 (이전)**: BUG-1+8 (player.move 누락) 해결 + BUG-25~34 등재
- **세션 38+ (현재 작업)**: 비행 처리 라인별 정밀 매핑 시작
  - **F-1**: 원본 SmartMovingSelf 비행 처리 라인별 read (handleAlternativeFlying + handleLand 비행 분기 + standupIfPossible + wasFlying 엣지 + flyWhileOnGround)
  - **F-2**: 원본 SmartMovingBase.moveFlying L56-L93 + HorizontalAirDamping
  - **F-3**: 원본 SmartMovingModel isFlying 분기 L474-L520
  - **F-4**: 1.21.1 SmartMovingFlyer + sm_animateFlying + sm_setupTransforms isFlying 모두 read + 매핑
  - **F-5**: 차이점 표 + 사용자 보고 10건 → BUG 매핑
  - **F-6**: 일괄 정정 (또는 의존 별 분할 commit)
  - **F-7**: 빌드 + 사용자 인게임 검증 인계

## 세션 진입 고정 절차

반드시 아래 순서대로 read (필요 섹션만):

1. `docs/fix/integration_test_bugs.md` §BUG-1+8/25~34 + §후속 작업 큐
2. `docs/research/mapping/research_flying_line_by_line.md` 마지막 청크 + §진행 상태
3. **원본 1차 자료** (sm_original 로컬, **WebFetch 금지**):
   - `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingSelf.java`
     - L80, L602-L631, L633-L663, L1803-L1831, L2186-L2212, L2320, L2404, L2509+, L2542+
   - `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\SmartMovingBase.java`
     - moveFlying() L56-L93 + HorizontalAirDamping 정의
   - `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java`
     - L474-L520 isFlying 분기
   - `SmartMovingConfig.java` / `SmartMovingOptions.java` — 비행 옵션 기본값
4. **1.21.1 1차 자료**:
   - `src/client/java/choco/ratel/smartmoving/client/SmartMovingFlyer.java` (130 라인)
   - `src/client/java/choco/ratel/smartmoving/client/SmartMovingMover.java` (속도 헬퍼)
   - `src/client/java/choco/ratel/smartmoving/client/SmartMovingClientState.java` (isFlying 갱신 / standupIfPossible / tryLanding)
   - `src/client/java/choco/ratel/smartmoving/mixin/client/MixinLivingEntityClient.java` L158 (handleFlying 호출) + L172 (sprint 점프)
   - `src/client/java/choco/ratel/smartmoving/mixin/client/MixinClientPlayerEntity.java` L43 (sm_flyWhileOnGround)
   - `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityModelClient.java` (sm_animateFlying)
   - `src/client/java/choco/ratel/smartmoving/mixin/client/MixinPlayerEntityRenderer.java` (sm_setupTransforms isFlying)
5. **vanilla 1.21.1** (필요 시):
   - `.gradle/loom-cache/.../minecraft-clientOnly-*.jar` 추출 + `javap -p -c -l` (memory `reference_vanilla_extract.md`)
   - LivingEntity.travel() / jump() / move() 정확 시그니처

## F-단계 청크 분할 (1-3 세션)

| 세션 | F-단계 | 대상 | 추정 라인 |
|------|---|------|---|
| 38 | F-1 | 원본 SmartMovingSelf 비행 처리 (handleAlternativeFlying + handleLand 비행 분기 + standupIfPossible) | ~150 |
| 38 | F-2 | 원본 SmartMovingBase.moveFlying + HorizontalAirDamping | ~50 |
| 38 | F-3 | 원본 SmartMovingModel isFlying 분기 | ~50 |
| 39 | F-4 | 1.21.1 SmartMovingFlyer + sm_animateFlying + sm_setupTransforms 모두 read + 매핑 | ~200 |
| 39 | F-5 | 차이점 표 + 사용자 보고 10건 → BUG 매핑 | (정리) |
| 39+ | F-6 | 일괄 정정 (분할 가능) | 코드 수정 |
| 40 | F-7 | 빌드 + 인게임 검증 인계 | (마무리) |

**~500 라인 / 2-3 세션 완결 목표**.

## 라인별 매핑 출력 형식 (산출물)

**산출물 파일**: `docs/research/mapping/research_flying_line_by_line.md`
(animation_system.md / research_animation_line_by_line.md 와 별개 — 비행 매핑 전용).

**파일 구조** (Phase R 동일):
```
# 비행 원본 ↔ 1.21.1 라인별 매핑 (Flying Phase)

## F-1: SmartMovingSelf.handleAlternativeFlying (L602-L631)

| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L602 | `private boolean handleAlternativeFlying(...)` | `SmartMovingFlyer.handleFlying(...)` | [정합] | 메서드 |
| L604 | `boolean handleAlternativeFlying = !handledSwimming && !handledLava && sp.capabilities.isFlying && Config.isFlyingEnabled();` | `if (!sm.isFlying \|\| !cfg.fly) return false;` | [정합] | 진입 가드 |
| L607 | `resetSwimming();` | (1.21.1 sm_travel_client 의 swim 처리 후 진입) | [정합] | 호출 순서 |
| L608 | `resetClimbing();` | `MixinLivingEntityClient.sm_travel_client` L147-L153 | [정합] | reset |
| ...
```

**분류 5종** (Phase R 동일):
- `[정합]` — 1.21.1 에 동일/등가 구현 존재
- `[오역]` — 1.21.1 구현 존재하나 값/조건/로직 다름 (BUG 등록 대상)
- `[누락]` — 원본에 있으나 1.21.1 미이식 (BUG 등록 대상)
- `[잉여]` — 1.21.1 에만 있는 추가 로직 (BUG 등록 대상)
- `[N/A]` — 구조 부재 또는 vanilla 자동 처리

## 원자 실행 루프 (F-단계 단위)

1. ② §진행 상태 read → 다음 F-단계 결정
2. 해당 F-단계 ③ 원본 1차 자료 라인별 read (offset+limit, 200줄 이내)
3. 청크 라인별 매핑 표 작성 — `research_flying_line_by_line.md` 에 표 추가
4. 매핑 중 발견된 [오역]/[누락]/[잉여] 즉시 § 발견 표에 등재 (BUG 매핑 후보)
5. 청크 통계 (정합/오역/누락/잉여/N/A) 기록
6. F-N 끝 → 다음 F-N 결정
7. F-5 (사용자 보고 10건 → BUG 매핑) 완료 시 → F-6 (일괄 정정) 진입
8. F-6 정정 시 매 BUG 별 commit 분리 (또는 의존 묶음)
9. 빌드 검증 — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL
10. F-7 사용자 인게임 검증 인계

## 완료 전 검증 체크리스트 (F-단계 단위)

- [근거] ✓ 원본 sm_original 라인별 read 완료 (offset+limit 정확)
- [전수] ✓ 청크 내 모든 라인이 매핑 표에 등재 (skip 0건)
- [분류] ✓ 모든 라인이 5 분류 중 하나
- [발견] ✓ [오역]/[누락]/[잉여] 발견 시 § 발견 표 등재
- [통계] ✓ 청크 통계 기록
- [검증] ✓ 1.21.1 대응 위치 grep 으로 검증
- [BUG 매핑] ✓ 사용자 보고 10건과 발견 차이점 매핑 (F-5 단계)
- [빌드] ✓ F-6 정정 후 BUILD SUCCESSFUL

## 커밋 규칙

- F-단계 1개 끝날 때마다 커밋 (또는 분할).
- F-1~F-5 (매핑 단계): `docs(flying): F-N — <범위> 라인별 매핑 (세션 N)`
- F-6 (정정 단계): `fix(bug-N): <BUG 짧은 요약> — <원인> (Flying Phase F-6 / 세션 N)`
- HEREDOC 본문:
  - 청크 범위 + 매핑 라인 수
  - 통계 (정합/오역/누락/잉여/N/A)
  - 발견된 차이점 요약 (BUG 매핑 포함)
  - 빌드 결과 (F-6 만)
  - Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>

## 절대 금지

A. 1:1 번역 위반
- 원본 sm_original 라인별 read 누락 (가설 기반 수정 금지)
- WebFetch 사용 (sm_original 로컬 우선)
- "근사" 명분으로 원본과 다른 공식 사용
- vanilla 정확 시그니처 추측 (디스어셈블리 가능)

B. 진단 원칙 위반
- Agent 가설 검증 없이 적용 (라인별 비교로 확인 필수)
- 한 BUG 씩 수정 (다른 BUG 영향으로 검증 어려움)
- F-5 BUG 매핑 전 정정 진입 (F-6 진입 전 모든 차이점 식별 필수)

C. 라인별 매핑 위반
- 라인 skip (모든 라인 매핑 표 등재 의무)
- 분류 [N/A] 남발 (애매하면 [누락] 또는 [오역] 우선, F-5 검토)
- stale-comment 의존 (주석만 보고 분기 본체 안 봄)

D. 기술/프로세스 위반
- 빌드 실패 상태로 커밋 (F-6 코드 수정 시)
- --no-verify / Git 파괴적 작업 사용자 허가 없이
- vanilla / Mixin 본체 직접 수정 (F-6 에서만 + Mixin 통해)

## 진입 시 첫 액션

1. `integration_test_bugs.md` BUG-1+8/25~34 + §후속 큐 read → 직전 진행 상태 확인
2. `research_flying_line_by_line.md` 존재 여부 확인:
   - 미존재 → 본 prompt 의 § 라인별 매핑 출력 형식 따라 초기 구조 작성
   - 존재 → 마지막 청크 + 진행 상태 read
3. 다음 F-단계 결정 (예: 첫 진입 = F-1 / SmartMovingSelf.handleAlternativeFlying L602-L631)
4. 원본 1차 자료 read + 매핑 표 작성
5. 발견 차이점 즉시 등재
6. 토큰 여유 보면 다음 F-단계 또는 종료 + 다음 세션 안내

## 완료 조건 (Flying Phase 전체)

- [ ] F-1 ~ F-4 모두 완료 (라인별 매핑 ~500 라인)
- [ ] F-5 사용자 보고 10건 → BUG 매핑 완료
- [ ] F-6 모든 BUG 정정 + 빌드 BUILD SUCCESSFUL
- [ ] F-7 사용자 인게임 검증 통과 (10건 모두 정상 작동)
- [ ] 잔존 발견 시 BUG-N 추가 + 정정
- [ ] integration_test_bugs.md BUG-1+8/25~34 모두 [✅ 인게임 검증 완료]

## 추가 참고

- **메모리** (자동 인지):
  - `reference_original_sources.md` (sm_original 로컬 경로)
  - `reference_vanilla_extract.md` (vanilla jar 추출 + javap)
  - `feedback_no_premature_defer.md` (추측 회피 deferred 금지)
  - `feedback_fix_immediately.md` (발견 즉시 수정 원칙)
  - `feedback_audit_method.md` (전수 감사 1회 교차 대조)
  - `project_integration_bugs_session35.md` (세션 35 진단 4 분류)
- **이전 Phase prompt** (참고용 — 1:1 번역 원칙 학습):
  - `focus_01_phase_R_prompt.md` (Phase R 라인별 매핑 패턴)
  - `focus_01_phase_B_prompt.md` (Phase B R-10+ 본격 1:1)
  - `integration_test_bugs_prompt.md` (8 BUG 처리 패턴)

이 규칙 지키면서 작업 진행한다.
