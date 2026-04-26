# Focus #1 Phase B — 매 세션 진입 프롬프트

> **사용법**: 새 세션에서 다음 한 줄만 보내면 됩니다.
> ```
> docs/fix/focus_01_phase_B_prompt.md 따라서 포커스 #1 Phase B 작업 진행해줘.
> ```
> Claude 가 본 파일을 read → 아래 규칙대로 작업 + 직전 세션 종료 위치(§15)에서 자동 이어받음.

---

SmartMoving 1.7.10 Forge → 1.21.1 Fabric 포팅 프로젝트의 포커스 #1 — 애니메이션 망가짐/이상
작업을 이어서 진행한다. 현재 단계: **Phase B (R-10+ 본격 1:1 대응 / 코드 수정)**.

## 핵심 원칙 — 1:1 번역 (Phase B 특화)

이 포커스는 **엄격 1:1 번역**. 사용자 지시 (2026-04-26):
"에니메이션과 관련된 모든 코드를 싹 다 가져와서 그걸 전부 1대1 대응을 하는 작업이야 …
그렇게 해서 리서치 파일 보강 및 포커스 1 파일 보강을 한 후에 작업을 진행하는거야."

→ Phase B = Phase R(세션 3-23, 4,968 라인 매핑) 완료 후 발견된 [오역]/[누락]/[잉여] 16 그룹을
   **B-N 원자 단위**로 1:1 코드 수정. 보통 한 세션에 1-3 원자 진행.

절대 규칙:
- 1 원자 = 1 발견 그룹 = 1 분기 = 1 commit (가능한 경우). 여러 원자 묶음은 의존이 같을 때만.
- 원본 1차 자료 (sm_original 로컬 경로) 라인 정확 read. 청크 200줄 이내.
- 1.21.1 대응 위치 grep + read 로 사전 확인 (현재 미이식 / 부분 이식 여부).
- vanilla 동작 검증 — `docs/research/vanilla/*.md` 또는 필요 시 vanilla 소스 직접 read
  (특히 pivotY/scale/pose 등 setAngles reset 패턴, push/pop 타이밍).
- sin/cos 분해 근사 / body.pivotY 만 변경 / 전역 pivotY 리셋 금지 (메모리 실패 패턴).
- stale-comment-dependency 금지. 분기 본체 직접 비교.
- 라디안 그대로 (Half=π, Quarter=π/2 …). 도 단위 변환 금지.
- 표면 매핑만 허용 (vanilla API 이름 차이). 로직/공식/순서/회전순서/상수 정밀도는 원본 그대로.
- "빌드 성공 == 1:1 번역 완료" 아님. **side-by-side 라인별 매핑 + vanilla 동작 검증이 유일한 검증**.
- 한 원자 작업 중 신규 [오역]/[누락]/[잉여] 발견 시 §16 등재 + 새 B-N 원자 추가
  (자체 처리 vs deferred 판단).

## 직전 세션 종료 위치 (필수 인지)

- **세션 1-22** = Phase R (리서치/매핑) — 31 파일 4,968 라인 라인별 read + 1.21.1 매핑.
- **세션 23** = R-9 통합 — 발견 16 그룹 → B-N 14 + 인프라 B-X 1 + 검토 1 등록. **Phase R 종료**.
- **세션 24** = Phase B 진입 — **B-8 (rope sliding head/arm pivotY) 1:1 이식 완료**. 빌드 성공.

진입 시 반드시 `docs/fix/focus_01_animation.md` §15 작업 기록 최근 1-2 세션을 먼저 read 해서
직전 진행 상태 + 다음 권장 B-N 확인 (예: "세션 25 = B-18 / B-9").

## 세션 진입 고정 절차

반드시 아래 4 문서를 순서대로 읽는다 (필요 섹션만):

1. `docs/fix/playtest_fixes.md` (현재 포커스 = #1 진행 중 / Phase B 진입 확인)
2. `docs/fix/focus_01_animation.md` §10 B 섹션 (남은 B-N 목록) / §15 직전 세션 / §16 신규 발견
3. `docs/research/mapping/research_animation_line_by_line.md` §R-9 (B-N 매트릭스 + 의존 그래프)
4. **원본 로컬 경로** (B-N 별 1차 자료):

   | B-N | 발견 # | 원본 파일 (sm_original 로컬) | 핵심 라인 |
   |-----|---|---|---|
   | B-X 인프라 | (B-4~B-7 공통) | `SmartRender/.../statistics/SmartStatisticsData.java` | L40-L60 (calcualte 공식) |
   | B-4 | §16-10 | `SmartMoving/.../render/SmartMovingModel.java` | L80/L144/L200/L242-L244 |
   | B-5 | §16-12 | 동상 | L228/L232 |
   | B-6 | §16-13 | 동상 | L365-L367 |
   | B-7 | §16-20 | 동상 | L477-L479 |
   | B-8 | §16-11 | 동상 | L106/L117 ✅ 세션 24 완료 |
   | B-9 | §16-14 | 동상 | L285 |
   | B-10 | §16-15 | 동상 | L329 / L370-L371 |
   | B-11 | §16-16 | 동상 | L335 |
   | B-12 | §16-18 | 동상 | L401/L405 |
   | B-13 | §16-19 | 동상 | L442/L444/L448/L452/L453 |
   | B-14 | §16-22 | `SmartMoving/.../render/SmartMovingRender.java` | L124-L125 |
   | B-15 | §16-23 | 동상 | L132-L134 |
   | B-16 | §16-24 | `SmartRender/.../SmartRenderModel.java` | L251 |
   | B-17 | §16-25 | `SmartRender/.../ModelCapeRenderer.java` | L72-L73 |
   | B-18 | §16-17 | (잉여 제거) `MixinPlayerEntityModelClient.sm_animateCeilingClimbing` | L332 |

   sm_original 베이스 경로: `C:\Work\minecraft\porting\sm_original\`

## 권장 진행 순서 (의존 / 우선순위)

R-9.3 의존 그래프 + 단일/다중/신규/인프라 그룹화. 각 그룹 안에서는 자유 순서.

1. **단일 라인 (1-3줄, 의존 없음)** — 가장 단순, 빠른 진행:
   - B-8 ✅ (세션 24 완료) / **B-9** / **B-10** / **B-11** / **B-12** / **B-18**
2. **다중 라인 + MatrixStack 보정**:
   - B-13 (slide 5 라인 + setupTransforms `matrices.translate(0, -0.4F/16, 0)` 보정)
3. **신규 분기 (Mixin 메서드 추가)**:
   - B-14 (sm_getPositionOffset 분기) / B-15 (sm_captureBodyYaw 끝 보정)
4. **누적 검증 후**:
   - B-16 (cloak.pitch — vanilla setAngles 의 cloak reset 여부 사전 확인)
5. **인프라 선행 → 입력값 4 일괄 교체**:
   - **B-X 인프라** (verticalDistance/Speed + allDistance/Speed capture — SmartMovingClientState
     또는 신규 MixinClientPlayerEntity tick) → **B-4** / **B-5** / **B-6** / **B-7**
6. **신규 Mixin 클래스**:
   - B-17 (MixinCapeFeatureRenderer — outer.X 클램프)

세션 25 권장 = B-18 (1 라인 삭제 — 가장 빠름) 또는 B-9 (1 라인 추가) 또는 두 개 묶음.

## 원자 실행 루프 (B-N 단위)

1. **§15 read → 다음 B-N 결정** (직전 세션 로그의 "다음 단계" 항목 / §10 B 섹션 미체크 항목)
2. **원본 라인별 read** — Read tool offset+limit 정확 (B-N 표 참고). 분기 시작-종료 모두 포함.
3. **1.21.1 대응 위치 read** — Glob/Grep 으로 Mixin 메서드 위치 찾고 본체 read. 현재 미이식
   /부분 이식 여부 확인.
4. **vanilla 동작 검증** (필요 시):
   - pivotY 변경 → `BipedEntityModel_detail.md` setAngles reset 패턴 확인
   - cloak/scale → vanilla 의 매 프레임 reset 여부
   - getPositionOffset → vanilla `PlayerEntityRenderer_getPositionOffset.md`
5. **값 정확성 사전 검증**:
   - 어깨/Pelvic/Torso 등 SR 다층 노드 부재 보정 (1.21.1 자체 pivot 기본값에서 차감/가산)
   - 회전순서 (YZX/XZY/YXZ/ZYX 헬퍼 사용 / qY*qX*qZ 형 패턴)
   - 라디안 그대로 (Half/Quarter/Eighth 상수 — 도 변환 금지)
6. **코드 수정** (Edit tool / `src/client/java/.../mixin/client/` 또는 `src/client/java/.../client/`).
   주석에 `B-N / §16-X` 표기 + vanilla reset 안전성 또는 변경 근거 명시.
7. **빌드 검증** — `./gradlew compileJava compileClientJava --rerun-tasks` BUILD SUCCESSFUL 확인.
8. **focus_01 §10 B-N [ ] → [x]** + **§15 세션 N 로그 추가**:
   - 진행한 작업 / 사전 검증 / 코드 변경 위치 / 값 정확성 / 빌드 결과 / 다음 단계
9. **검증 체크리스트** ([근거]/[전수]/[검증]/[회귀]/[빌드]) 기록.
10. **커밋** — `feat(focus#1): R-10+ B-N — <분기> <짧은 요약> 1:1 이식 (세션 N)`.
11. **토큰 여유 있으면 다음 B-N**, 아니면 §15 에 다음 권장 B-N 명시 후 종료.

## 완료 전 검증 체크리스트 (B-N 단위)

- [근거] ✓ 원본 1차 자료 라인별 read 완료 (offset+limit 정확)
- [전수] ✓ 원본 변경 N 지점 모두 1.21.1 매핑 (skip 0)
- [분류] N/A (Phase B 는 R 분류 단계 아님)
- [발견] 신규 [오역]/[누락]/[잉여] 발견 시 §16 등재 + 새 B-N 후보 등록
- [검증] vanilla 동작 사전 검증 (필요 시) — reset 패턴 / 누적 위험 / push/pop 순서
- [값정확] SR 다층 노드 부재 보정 / 회전순서 / 라디안 직접 검증
- [회귀] 다른 분기 영향 없음 — vanilla 자동 reset 검증 또는 명시적 reset 추가
- [빌드] BUILD SUCCESSFUL
- [문서] §10 B-N [x] / §15 세션 로그

## 커밋 규칙

- B-N 1개 끝날 때마다 커밋 (또는 의존 같은 B-N 묶음).
- 메시지: `feat(focus#1): R-10+ B-N — <분기> <짧은 요약> 1:1 이식 (세션 N)`
- 잉여 제거: `refactor(focus#1): R-10+ B-N — <분기> 잉여 <항목> 제거 (세션 N)`
- HEREDOC 본문:
  - 원본 위치 + 변경 내용 매핑
  - 사전 검증 (vanilla reset 패턴 등)
  - 값 정확성 근거 (SR 노드 부재 보정 등)
  - 빌드 결과
  - 다음 권장 B-N
  - Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>

## 절대 금지

A. 포커스 범위 위반
- 다른 포커스 (#2.5/#2.6/#2.7/#3/#4/#5/#6) 손대기
- B-1/B-2/B-3 (세션 2 완결) 재작업 / B-8 (세션 24) 재작업
- Phase R 단계 (R-1~R-9) 회귀 작업

B. Phase B 원칙 위반
- vanilla reset 검증 없이 pivotY/scale/cloak 변경 (누적 위험 → 다른 분기 영향)
- 라디안 → 도 단위 변환 / 도 단위 → 라디안 변환 누락
- 회전순서 무시 (YZX/XZY/YXZ 분기에서 일반 XYZ 적용)
- sin/cos 분해 근사 (회전순서 헬퍼 우회)
- 추측 기반 매핑 ("아마 이게 맞을 것이다") — 1차 자료 read 필수
- 한 commit 에 여러 발견 그룹 묶기 (의존 없는 한 분리)

C. 의존 순서 위반
- B-4~B-7 을 B-X 인프라 선행 없이 시도
- B-13 의 MatrixStack `matrices.translate(0, -0.4F/16, 0)` 보정 누락 (body.offsetY 부재)
- B-17 (CapeFeatureRenderer Mixin) 을 fabric.mod.json mixins 등록 없이 진행

D. 기술/프로세스 위반
- 빌드 실패 상태로 커밋
- --no-verify / Git 파괴적 작업 사용자 허가 없이
- ⑤ 원본 로컬 read 가능한데 WebFetch 사용 (로컬 우선)
- vanilla 본체 직접 수정 (Mixin 으로만 제어)

## 진입 시 첫 액션

1. `docs/fix/focus_01_animation.md` §15 직전 1-2 세션 read → 어디까지 완료 / 다음 권장 B-N 확인
2. `docs/fix/focus_01_animation.md` §10 B 섹션 read → [ ] 항목 중 의존 없는 가장 우선 B-N 선택
3. 해당 B-N 의 §16 발견 항목 read → 분기 / 라인 / 입력값 확인
4. 원본 1차 자료 + 1.21.1 대응 위치 read
5. 원자 실행 루프 (위 11 단계) 진행
6. 토큰 여유 있으면 다음 B-N (보통 1-3 원자 / 세션). 아니면 §15 에 다음 권장 명시 후 종료

## 완료 조건 (전체 #1)

- [x] R-1 ~ R-9 모두 완료 (Phase R / 세션 3-23)
- [ ] R-10+ B-N 14 + 인프라 B-X 1 모두 [x] (Phase B / 세션 24+ )
  - [x] B-8 (세션 24)
  - [ ] 단일 라인 5 (B-9/B-10/B-11/B-12/B-18)
  - [ ] 다중 라인 1 (B-13)
  - [ ] 신규 분기 2 (B-14/B-15)
  - [ ] 누적 검증 후 1 (B-16)
  - [ ] 인프라 + 입력값 5 (B-X / B-4/B-5/B-6/B-7)
  - [ ] 신규 Mixin 1 (B-17)
- [ ] 빌드 BUILD SUCCESSFUL
- [ ] §14 회귀 감사 통과
- [ ] playtest_fixes.md "현재 포커스" → (#1 AI 완결 / 통합테스트 인계)
- [ ] 사용자 in-game 통합테스트 후 §3 16 케이스 매칭 확인 시 → 전체 완료

이 규칙 지키면서 작업 진행한다.
