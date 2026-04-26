# 통합테스트 8 버그 처리 — 매 세션 진입 프롬프트

> **사용법**: 새 세션에서 다음 한 줄만 보내면 됩니다.
> ```
> docs/fix/integration_test_bugs_prompt.md 따라서 통합테스트 버그 작업 진행해줘.
> ```
> Claude 가 본 파일을 read → 아래 규칙대로 작업 + 직전 세션 종료 위치(§후속 작업 큐)에서 자동 이어받음.

---

SmartMoving 1.7.10 Forge → 1.21.1 Fabric 포팅 프로젝트의 **세션 35 인게임 통합테스트
8 버그** 처리 작업. 포커스 #1 (애니메이션) Phase R + Phase B 완료 후 발견. 본 작업은
#1 단독 해결 불가 — 다수가 #2/#2.5/#2.6/#2.7 의 입력 상태/POSE/BBox/액체 경계 동기화
정확성에 의존.

## 핵심 원칙 — 1:1 번역 + 통합 디버깅 + 인게임 검증

이 작업은 **엄격 1:1 번역 + 통합 흐름 진단**. 사용자 지시 (2026-04-26):
- "원본 코드 볼때는 웹페치 말고 sm_original 로컬 경로 써."
- "해당 작업을 위해 필요한 모든 것을 다 찾아 라인 하나하나 다 읽어 분석. 1:1 번역."
- "추측 회피 명분으로 deferred 결정 금지" (세션 33 B-17 실패 사례 학습).

→ Phase R/B 와 다른 점:
- **부분 이식된 코드 진단** — BUG-8 SmartMovingFlyer 처럼 코드는 있는데 작동 안 함 → 호출 조건 / inject 순서 / vanilla 충돌 / 동기화 검증 필요
- **다른 포커스 의존** — 단일 파일 내 1:1 번역 외 통합 흐름 분석 필요
- **인게임 검증 필수** — 코드 변경 후 사용자 in-game 재현 확인 필요 (AI 단독 검증 불가)
- **여러 의심 지점에서 우선순위 결정** — 5-6 개 의심 지점 모두 동시 진단 어려움 → 가장 가능성 높은 것부터

절대 규칙:
- 1 작업 = 1 BUG (또는 명확한 동일 원인 묶음 = BUG-1+BUG-8 처럼).
- 원본 1차 자료 (sm_original 로컬) 라인 정확 read. WebFetch 금지.
- 1.21.1 vanilla 본체 필요 시 `.gradle/loom-cache/.../minecraft-clientOnly-*.jar` 추출 + `javap -p -c -l` 디스어셈블리 (sources jar 부재 우회).
- 의심 지점 분석 시 추측 금지 — 코드 read + grep + 호출처 추적으로 확인.
- BUG 처리 후 사용자 인게임 재현 결과 받기 전에는 "완료" 마킹 금지 (코드 변경 + 빌드 통과 = "AI 완결" / 사용자 확인 = "통합테스트 완결").
- 신규 [오역]/[누락]/[잉여] 발견 시 integration_test_bugs.md 또는 focus 별 docs §16 등재 + 새 BUG-N 또는 B-N 원자 추가 판단.

## 직전 세션 종료 위치 (필수 인지)

세션 진입 시 반드시 `docs/fix/integration_test_bugs.md` §후속 작업 큐 read 해서 어디까지
완료/진행 중인지 확인:

- ✅ **BUG-6 I/O 키 비활성화** (세션 35 완료 — `SmartMovingClientState.java` L894-L910)
- ⏳ **BUG-7** (sm_setAngles flyingCreative `Config.enabled` 가드)
- ⏳ **BUG-1 + BUG-8** (비행 고정 + SM 비행 시스템 미작동 / 함께 진단)
- ⏳ **BUG-3** (crawl 진동 / #2.7 POSE/BBox)
- ⏳ **BUG-5** (swim 진동 / #2.6 lava liquid border)
- ⏳ **BUG-2** (climbing 작동 안 함)
- ⏳ **BUG-4** (다이빙 망가짐)

## 세션 진입 고정 절차

반드시 아래 4-5 문서를 순서대로 읽는다 (필요 섹션만):

1. `docs/fix/playtest_fixes.md` (현재 포커스 + 통합테스트 결과 안내 확인)
2. `docs/fix/integration_test_bugs.md` (전체 — 8 버그 + 진행 권장 순서 + 후속 큐 + 진단 4 분류)
3. 진행 대상 BUG 의 §의심 지점 + §리서치 필요 파일 정독
4. 영향 포커스 docs (예: BUG-3 = `focus_02_7_bbox_server_sync.md`, BUG-5 = `focus_02_6_lava_liquid_border.md`) — 현재 진행 상태 + 핵심 매핑 확인
5. **원본 + 1.21.1 1차 자료** (BUG 별 §리서치 필요 파일 명시):
   - 원본 베이스: `C:\Work\minecraft\porting\sm_original\SmartMoving\` / `SmartRender\`
   - 1.21.1 베이스: `C:\Users\user\IdeaProjects\SmartMoving\src\{main,client}\java\choco\ratel\smartmoving\`
   - vanilla 1.21.1 (필요 시): `.gradle/loom-cache/.../minecraft-clientOnly-*.jar` + javap

## 진행 권장 순서 (의존 / 우선순위)

`integration_test_bugs.md` §진행 권장 순서 표 참고:

| 순서 | BUG | 의존 / 비고 |
|------|-----|---|
| 1 | **BUG-7** | 단순 가드 추가 (의존 없음) — 가장 빠른 진행 가능 |
| 2 | ✅ BUG-6 (완료) | I/O 키 비활성화 (세션 35) |
| 3 | **BUG-1 + BUG-8** | SmartMovingFlyer 진단 / 동일 원인 가능 / 함께 진단 |
| 4 | **BUG-3** | #2.7 POSE/BBox 동기화 검증 (이미 96%) |
| 5 | **BUG-5** | #2.6 lava liquid border (이미 90%) |
| 6 | **BUG-2** | #2 climbing 진입 로직 |
| 7 | **BUG-4** | #4 키 조합 + isDiving 종료 |

## 원자 실행 루프 (BUG 단위)

각 BUG 1개 = 1 원자 (또는 동일 원인 묶음). 흐름:

1. **§후속 작업 큐 read** — 다음 진행 대상 BUG 결정
2. **BUG §의심 지점 + §리서치 필요 파일 정독** (integration_test_bugs.md)
3. **원본 라인별 read** — sm_original 의 핵심 메서드/분기 (offset+limit 정확)
4. **1.21.1 코드 현재 상태 read** — 영향 받는 Mixin / SmartMovingClientState / 헬퍼
5. **의심 지점 우선순위 결정** — 가장 가능성 높은 것부터 진단:
   - 호출 조건 검증 (Mixin `at` / `target` / 조건문)
   - 시그니처/타입 검증 (vanilla jar 디스어셈블리 가능)
   - 동기화 검증 (sm.xxx 갱신 위치 grep)
   - vanilla 충돌 검증 (`@Inject` 순서 / `ci.cancel()` / 중복 처리)
6. **원인 확정 후 코드 수정** — 1:1 번역 원칙 + 영향 분석 (다른 분기 회귀 위험)
7. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL
8. **integration_test_bugs.md 갱신**:
   - BUG §해결 시도 / §진행 상태 / §결과 요약 추가
   - §후속 작업 큐 [ ] → [ AI 완결 / 인게임 검증 대기 ]
9. **영향 포커스 docs 갱신** — 필요 시 (예: focus_02_7 의 추가 보강 시 §15 로그)
10. **커밋** — 컨벤션 따라 (`fix(bug-N): <BUG 짧은 요약> <원인> <해결>`)
11. **사용자 인게임 재현 결과 대기** — 이 BUG 완료 후 다음 BUG 진행
12. **사용자 인게임 결과**:
    - 정상 동작 → BUG [x] 완료 / 다음 BUG 진행
    - 여전히 버그 → 추가 진단 (다음 의심 지점)
    - 부분 개선 → 추가 BUG-N 등재 (잔여 증상)

## 완료 전 검증 체크리스트 (BUG 단위)

- [근거] ✓ 원본 sm_original 라인별 read 완료 (offset+limit 정확)
- [전수] ✓ BUG §의심 지점 모두 분석 — 우선순위 결정 + 원인 확정
- [검증] vanilla 본체 필요 시 디스어셈블리로 정확 시그니처 확인
- [영향] 코드 변경의 다른 분기/포커스 회귀 영향 분석
- [빌드] BUILD SUCCESSFUL
- [문서] integration_test_bugs.md 의 BUG §해결 / §후속 큐 갱신
- [커밋] 컨벤션 + BUG 번호 + 원인 + 해결 명시
- [인게임 대기] 사용자 인게임 재현 결과 대기 마킹

## 커밋 규칙

- BUG 1건 처리 후 커밋 (또는 명확한 동일 원인 묶음).
- 메시지: `fix(bug-N): <BUG 짧은 요약> — <원인 한 줄> (세션 N)`
- 잉여/refactor: `refactor(bug-N): <짧은 요약> — <원인> (세션 N)`
- 분석만/문서만: `docs(bug-N): <BUG 분석> — <발견 사항> (세션 N)`
- HEREDOC 본문:
  - BUG 증상 (사용자 보고)
  - 원본 위치 + 1.21.1 위치
  - 의심 지점 분석 결과 (어떤 의심이 원인이었는지)
  - 코드 변경 내용 + 회귀 영향 분석
  - 빌드 결과
  - 인게임 검증 대기 명시
  - Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>

## 절대 금지

A. 작업 범위 위반
- 다른 BUG 묶어 한 commit 처리 (의존 없는 한 분리)
- 인게임 검증 결과 받기 전 "완료" 마킹 (오직 "AI 완결" 만 가능)
- 추측 기반 원인 결정 — 의심 지점 분석 시 코드 read 로 확인 필수

B. 진단 원칙 위반
- 1.21.1 vanilla 본체 추측 (디스어셈블리 가능한데 안 함)
- 의심 지점 1개만 보고 코드 수정 (5-6개 모두 분석 후 결정)
- 호출 조건 / inject 순서 / 동기화 검증 누락

C. 1:1 번역 위반
- 원본 sm_original 라인별 read 누락
- WebFetch 사용 (로컬 sm_original 우선)
- "근사" 명분으로 원본과 다른 공식 사용

D. 회귀 위반
- 다른 BUG 또는 다른 포커스 회귀 영향 분석 누락
- vanilla 분기 영향 분석 누락
- reset 인프라 (sm_setAngles HEAD 4 필드 / smOuterTiltX 등) 호환성 검증 누락

E. 기술/프로세스 위반
- 빌드 실패 상태로 커밋
- --no-verify / Git 파괴적 작업 사용자 허가 없이
- BUG-6 (I/O 비활성화) 재활성화 사용자 명시 요청 없이

## 진입 시 첫 액션

1. `docs/fix/integration_test_bugs.md` §후속 작업 큐 read → 다음 BUG 결정
2. 해당 BUG §의심 지점 + §리서치 필요 파일 정독
3. 원본 sm_original + 1.21.1 1차 자료 read
4. 의심 지점 우선순위 분석 + 원인 확정
5. 원자 실행 루프 (위 12 단계) 진행
6. 토큰 여유 있으면 다음 BUG (보통 1 BUG / 세션 — 인게임 검증 대기 필요).
   아니면 §후속 큐 갱신 후 종료 + 다음 세션 안내.

## 완료 조건 (전체 통합테스트)

- [ ] 8 BUG 모두 [AI 완결] 처리
- [ ] 사용자 인게임 재현으로 모든 BUG 정상 동작 확인
- [ ] 신규 발견 BUG 모두 등재 + 처리
- [ ] BUG-6 (I/O 키 비활성화) 사용자 재활성화 요청 시 복원
- [ ] integration_test_bugs.md §후속 작업 큐 모두 [x]
- [ ] playtest_fixes.md 현재 포커스 → 다음 단계로 전환

## 통합테스트 진단 4 분류 (참고)

`integration_test_bugs.md` §진단 원칙 의 4 분류 (사용자 보고 → 식별):

1. **상태 진입 실패** (#2 / #2.5 / #2.6 / #2.7) — SM 분기 자체 활성화 실패. 애니메이션 무관.
2. **상태 유지 실패** (#2 / #2.7) — 진입 후 매 틱 false→true 진동. 위아래 흔들림 / 뚝뚝뚝.
3. **상태 종료 실패** (#2 / #2.7) — 잔여 상태로 다음 동작 영향. "원상태 복귀 실패".
4. **애니메이션 정합** (#1) — 위 3 정상 후만 의미. sm_setAngles 결과 시각 차이.

→ #1 은 마지막 단계. 입력 상태 부정확 시 애니메이션 보강 무용.

## 추가 참고

- **메모리** (자동 인지):
  - `project_integration_bugs_session35.md` — 8 버그 진단 패턴
  - `project_io_key_disabled.md` — BUG-6 비활성화 위치 + 재활성화 절차
  - `reference_vanilla_extract.md` — vanilla 1.21.1 본체 추출 방법
  - `feedback_no_premature_defer.md` — 추측 회피 deferred 금지
  - `reference_original_sources.md` — sm_original 로컬 경로
- **포커스 #1 prompt** (참고용 — 1:1 번역 원칙 학습):
  - `focus_01_phase_R_prompt.md` (Phase R / 라인별 매핑)
  - `focus_01_phase_B_prompt.md` (Phase B / R-10+ 본격 1:1)

이 규칙 지키면서 작업 진행한다.
