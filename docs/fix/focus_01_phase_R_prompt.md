# Focus #1 Phase R — 매 세션 진입 프롬프트

> **사용법**: 새 세션에서 다음 한 줄만 보내면 됩니다.
> ```
> docs/fix/focus_01_phase_R_prompt.md 따라서 포커스 #1 Phase R 작업 진행해줘.
> ```
> Claude 가 본 파일을 read → 아래 규칙대로 작업 + 직전 세션 종료 위치(§15)에서 자동 이어받음.

---

SmartMoving 1.7.10 Forge → 1.21.1 Fabric 포팅 프로젝트의 포커스 #1 — 애니메이션 망가짐/이상
작업을 이어서 진행한다. 현재 단계: **Phase R (라인별 전수 1:1 대응)**.

## 핵심 원칙 — 1:1 번역 (Phase R 특화)

이 포커스는 **엄격 1:1 번역**. 사용자 지시 (2026-04-26):
"에니메이션과 관련된 모든 코드를 싹 다 가져와서 그걸 전부 1대1 대응을 하는 작업이야.
그 모든코드는 관련이 있는 파일들을 첫 라인부터 끝 라인까지 라인별로 코드를 하나씩
다 읽어서 가져와야되는거고, 그걸 청크별로 나눠서 효율적으로 토큰이 안터지게 작업을
해야되는거야. 그렇게 해서 리서치 파일 보강 및 포커스 1 파일 보강을 한 후에 작업을
진행하는거야."

→ Phase R = 31 파일 ~5,000줄 라인별 read + 라인별 1.21.1 매핑 → 리서치/포커스 보강 →
   그 결과로 발견된 [오역]/[누락]/[잉여] 를 R-10+ Phase B 신규 원자로 등록 + 본격 1:1
   대응 (코드 수정).

절대 규칙:
- 라인별 read 누락 금지. 첫 라인 ~ 끝 라인 전부 read. 청크 분할은 토큰 절약용.
- "이 라인은 중요하지 않으니 skip" 금지. 모든 라인은 매핑 표에 등재 (정합/오역/누락/
  잉여/N/A 5 분류).
- 청크 단위 atomic — 한 청크 read → 매핑 → 보강 → 다음 청크. 청크 중간 종료 시 진행
  상태 명확히 기록.
- sin/cos 분해 근사 / body.pivotY 만 변경 / 전역 pivotY 리셋 금지 (메모리 실패 패턴).
- stale-comment-dependency 금지. 주석 신뢰 금지, 분기 본체 직접 비교.
- 라디안 그대로 (Half=π, Quarter=π/2 …). 도 단위 변환 금지.
- 표면 매핑만 허용 (vanilla API 이름 차이). 로직/공식/순서/회전순서/상수 정밀도는 원본
  그대로.
- "빌드 성공 == 1:1 번역 완료" 아님. **side-by-side 라인별 매핑이 유일한 검증**.
- R 단계는 코드 수정 거의 없음 (read + 매핑 + 보강 위주). R-10+ 진입 후에야 본격 코드
  수정.

## 직전 세션 종료 위치 (필수 인지)

- **세션 1 (2026-04-25)**: 4 Agent 병렬 high-level 리서치. focus_01_animation.md 235→409줄
  보강. ⚠️ 라인별 전수 read 아님 — high-level summary 였음.
- **세션 2 (2026-04-26)**: 식별된 누락 3건 선제 1:1 보강. B-1 (setAnglesYXZ 헬퍼 + isSwim
  head/isSlide body), B-2 (setAnglesXZY 헬퍼 + isFlying/isFalling arm), B-3 (setArmScales/
  setLegScales + 5 호출). 빌드 성공. ⚠️ 세션 2 "AI 완결" 마킹은 정정됨 — 라인별 전수
  대응 안 했으므로.
- **세션 3+**: Phase R 진입. 이 세션부터 라인별 read + 매핑 본격 시작.

진입 시 반드시 `docs/fix/focus_01_animation.md` §15 작업 기록을 먼저 read 해서 최근
세션이 어디까지 진행했는지 확인 (예: "세션 4 = R-1 청크 3까지 완료 / 다음: R-1 청크 4").

## 세션 진입 고정 절차

반드시 아래 5 문서를 순서대로 읽는다 (필요 섹션만 — 다 읽지 말 것):

1. `docs/fix/playtest_fixes.md` (현재 포커스 = #1 진행 중 / Phase R 진입 확인)
2. `docs/fix/focus_01_animation.md` §1 / §10 (R 원자) / §15 (직전 세션 로그) / §16 (신규 발견)
3. `docs/research/mapping/animation_system.md` (보조 — high-level 매핑)
4. `docs/research/original/smartmoving/render/SmartMovingModel.md` 등 원본 리서치 (검증용)
5. **원본 로컬 경로** (Phase R 핵심 1차 자료):

   **SmartMoving render 11 파일 (~1,896줄)**:
   - `C:\Work\minecraft\porting\sm_original\SmartMoving\src\main\java\net\smart\moving\render\SmartMovingModel.java` (797) ★★★
   - `...\render\SmartMovingRender.java` (337) ★★★
   - `...\render\ModelPlayer.java` (167) ★
   - `...\render\RenderPlayer.java` (120) ★
   - `...\render\SmartRenderContext.java` (26) ★
   - `...\render\IModelPlayer.java` (34) ★
   - `...\render\IRenderPlayer.java` (42) ★
   - `...\render\playerapi\SmartMoving.java` (54) ★
   - `...\render\playerapi\SmartMovingModelPlayerBase.java` (181) ★★
   - `...\render\playerapi\SmartMovingRenderPlayerBase.java` (138) ★★

   **SmartRender 19 파일 (~2,486줄)**:
   - `C:\Work\minecraft\porting\sm_original\SmartRender\src\main\java\net\smart\render\SmartRenderModel.java` (469) ★★★
   - `...\ModelRotationRenderer.java` (368) ★★★
   - `...\SmartRenderRender.java` (227) ★★★
   - `...\ModelPlayer.java` (179) ★★
   - `...\RenderPlayer.java` (156) ★★
   - `...\SmartRenderUtilities.java` (108) ★★
   - `...\ModelCapeRenderer.java` (89) ★
   - `...\SmartRenderMod.java` (73) ★
   - `...\IModelPlayer.java` (62) ★
   - `...\ModelEarsRenderer.java` (60) ★
   - `...\ModelSpecialRenderer.java` (55) ★
   - `...\SmartRenderContext.java` (47) ★
   - `...\IRenderPlayer.java` (47) ★
   - `...\RendererData.java` (31) ★
   - `...\SmartRenderInfo.java` (28) ★
   - `...\SmartRenderInstall.java` (26) ★
   - `...\playerapi\SmartRender.java` (43) ★
   - `...\playerapi\SmartRenderModelPlayerBase.java` (248) ★★★
   - `...\playerapi\SmartRenderRenderPlayerBase.java` (162) ★★

   **SmartRender statistics 7 파일 (~620줄)**:
   - `...\statistics\SmartStatistics.java` (197) ★★
   - `...\statistics\SmartStatisticsFactory.java` (142) ★
   - `...\statistics\SmartStatisticsDatas.java` (75) ★
   - `...\statistics\SmartStatisticsData.java` (60) ★
   - `...\statistics\SmartStatisticsContext.java` (37) ★
   - `...\statistics\SmartStatisticsOther.java` (29) ★
   - `...\statistics\IEntityPlayerSP.java` (22) ★

   **합계: 31 파일 ~5,000줄.**

## Phase R 청크 분할 (8-10 세션)

| 세션 | R-단계 | 파일 | 누적 줄수 |
|------|---|------|---|
| 3 | R-1 | SmartMovingModel.java 4 청크 (L1-L200 / L201-L400 / L401-L600 / L601-L797) | 797 |
| 4 | R-2 | SmartMovingRender.java + SM render Context/IModel/IRender/ModelPlayer/RenderPlayer | ~726 |
| 5 | R-3 | SM render/playerapi 3 파일 | ~373 |
| 6 | R-4 | SmartRenderModel.java 3 청크 (L1-L160 / L161-L320 / L321-L469) | ~469 |
| 7 | R-5 | ModelRotationRenderer.java 2 청크 + RendererData/Cape/Ears/Special | ~603 |
| 8 | R-6 | SmartRenderRender.java + SmartRenderUtilities + Mod/Info/Install/Context/IModel/IRender | ~564 |
| 9 | R-7 | SR ModelPlayer/RenderPlayer + SR playerapi 3 파일 | ~788 |
| 10 | R-8 | SmartStatistics 일체 (7 파일) | ~620 |
| 11 | R-9 | 통합 라인별 매핑 표 + focus_01 §5/§10 대폭 보강 | (정리) |
| 12+ | R-10+ | 발견된 [오역]/[누락]/[잉여] B-N 원자 등록 + 본격 1:1 대응 (코드 수정) | (시작) |

## 라인별 매핑 출력 형식 (산출물)

**산출물 파일**: `docs/research/mapping/research_animation_line_by_line.md`
(animation_system.md 와 별개 — 라인별 매핑 표 전용).

**파일 구조**:
```
# 애니메이션 원본 ↔ 1.21.1 라인별 매핑 (Phase R)

## R-1: SmartMovingModel.java (797줄)

### 청크 1 (L1-L200)
| 원본 L | 원본 코드 (요약) | 1.21.1 매핑 | 분류 | 비고 |
|---|---|---|---|---|
| L14 | `package net.smart.moving.render;` | (해당 없음 — 패키지) | [N/A] | |
| L22 | `class SmartMovingModel extends SmartRenderContext` | `MixinPlayerEntityModelClient @Mixin(BipedEntityModel.class)` | [정합] | 클래스 매핑 |
...

청크 1 (L1-L200) 통계: 정합 145 / 오역 3 / 누락 2 / 잉여 1 / N/A 49 = 200 라인 전수.
```

**분류 5종**:
- `[정합]` — 1.21.1 에 동일/등가 구현 존재
- `[오역]` — 1.21.1 구현 존재하나 값/조건/로직 다름 (R-10+ B-N 등록 대상)
- `[누락]` — 원본에 있으나 1.21.1 미이식 (R-10+ B-N 등록 대상)
- `[잉여]` — 1.21.1 에만 있는 추가 로직 (R-10+ 검토 대상)
- `[N/A]` — 구조 부재 또는 vanilla 자동 처리 (§7 등재)

## 표면 매핑 (허용)

- `bipedHead` / `bipedBody` → `head` / `body`
- `bipedLeftArm` / `bipedRightArm` → `leftArm` / `rightArm`
- `bipedLeftLeg` / `bipedRightLeg` → `leftLeg` / `rightLeg`
- `rotateAngleX/Y/Z` → `pitch`/`yaw`/`roll`
- `rotationPointX/Y/Z` → `pivotX`/`pivotY`/`pivotZ`
- `ModelRotationRenderer.scaleX/scaleY/scaleZ` → `ModelPart.xScale/yScale/zScale` (B-08)
- `glPushMatrix/glPopMatrix` → `matrices.push()/pop()`
- `glRotatef(deg, ax, ay, az)` → `matrices.multiply(RotationAxis.POSITIVE_X.rotation(rad))`
- `rotationOrder = YZX` → `setAnglesYZX()` 헬퍼 (이식 완료 / B-1)
- `rotationOrder = ZXY` → `setAnglesZXY()` 헬퍼 (이식 완료)
- `rotationOrder = YXZ` → `setAnglesYXZ()` 헬퍼 (B-1 세션 2 완결)
- `rotationOrder = XZY` → `setAnglesXZY()` 헬퍼 (B-2 세션 2 완결)
- `rotationOrder = ZYX` → 헬퍼 부재 (어깨 ZYX 만 사용 / 어깨 노드 부재로 N/A)
- `bipedOuter.rotateAngleY` → `setupTransforms @ModifyArg(index=3)` bodyYaw 교체
- `bipedOuter.rotateAngleX` → `setupTransforms TAIL matrices.multiply(POSITIVE_X)`
- `totalHorizontalDistance` → `limbSwing` (entity.limbAnimator.getPos(tickDelta))
- `currentHorizontalSpeed` → `limbSwingAmount` (entity.limbAnimator.getSpeed(tickDelta))
- `totalTime` (SmartStatistics) → `animationProgress` (entity.age + tickDelta)
- `MathHelper.cos(x)` → `MathHelper.cos(x)` 동일
- `MathHelper.sqrt_float(x)` → `MathHelper.sqrt(x)`
- `Half = (float)Math.PI` → 1.21.1 동일
- `RadiantToAngle = 180/π` → `MathHelper.DEGREES_PER_RADIAN` 또는 동일
- 1.7.10 `setRotationAngles` ↔ 1.21.1 `setAngles` (`MixinPlayerEntityModelClient.sm_setAngles
  @Inject(at=TAIL)`)

## 원자 실행 루프 (Phase R 청크 단위)

1. ② focus_01_animation.md §15 에서 직전 세션 종료 위치 확인 + 다음 청크 결정
2. 해당 청크 ⑤ 원본 로컬 read (Read tool offset+limit, 200줄 이내)
3. 청크 라인별 매핑 작성 — `research_animation_line_by_line.md` 에 표 추가
4. 매핑 중 발견된 [오역]/[누락]/[잉여] 는 즉시 §16 신규 발견에 등재 (R-10+ B-N 후보)
5. 청크 통계 (정합/오역/누락/잉여/N/A) 기록
6. focus_01 §10 R-N [ ] → [x] (전 청크 완료 시)
7. focus_01 §15 세션 로그 추가 (청크 범위 + 통계 + 다음 청크)
8. 빌드 검증 — 코드 변경 없으면 skip. 변경 있으면 `./gradlew compileJava
   compileClientJava --rerun-tasks` 성공 확인
9. 커밋 (`docs(focus#1): R-N 청크 K — <범위> 라인별 매핑 (세션 N)`)
10. 토큰 여유 있으면 다음 청크 진입, 없으면 세션 종료 + 다음 세션 안내

## 완료 전 검증 체크리스트 (Phase R 청크 단위)

- [근거] ⑤ 원본 로컬 라인별 read 완료 (offset+limit 정확)
- [전수] 청크 내 모든 라인이 매핑 표에 등재 (skip 0건)
- [분류] 모든 라인이 5종 분류 (정합/오역/누락/잉여/N/A) 중 하나로 표시
- [발견] [오역]/[누락]/[잉여] 발견 시 §16 등재 + R-10+ B-N 후보 등록
- [통계] 청크 통계 기록
- [검증] 1.21.1 대응 위치 grep 으로 검증 (실제 존재 확인)
- [회귀] 본 청크 read 가 기존 이식 영향 없음 (R 는 read 위주이므로 통상 N/A)
- [빌드] 코드 변경 시 BUILD SUCCESSFUL

## 커밋 규칙

- 청크 1개 끝날 때마다 또는 R-단계 1개 끝날 때마다 커밋
- 메시지: `docs(focus#1): R-N 청크 K — <파일> L<start>-L<end> 라인별 매핑 (세션 N)`
- HEREDOC 본문:
  - 청크 범위 + 매핑 라인 수
  - 통계 (정합/오역/누락/잉여/N/A)
  - 발견된 주요 [오역]/[누락]/[잉여] 요약
  - R-10+ B-N 후보 등록 건수
  - Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>

## 절대 금지

A. 포커스 범위 위반
- 다른 포커스 (#2.5/#2.6/#2.7/#3/#4/#5/#6) 손대기
- B-1/B-2/B-3 (세션 2 완결) 재작업
- R-9 전에 R-10+ B-N 코드 수정 진입 (라인별 매핑이 먼저)

B. Phase R 원칙 위반
- 라인 skip (모든 라인 매핑 표 등재 의무)
- 청크 범위 초과 read (한 번에 200줄 초과 read 시 토큰 부담)
- 추측 기반 매핑 ("아마 이게 대응될 것이다")
- 1.21.1 대응 위치 grep 검증 생략
- 분류 [N/A] 남발 (애매하면 [누락] 또는 [오역] 우선, R-10+ 검토)
- stale-comment 의존 (주석만 보고 분기 본체 안 봄)
- 산출물 파일 (research_animation_line_by_line.md) 누락 — 매핑은 반드시 이 파일에 기록

C. 의존 순서 위반
- R-2 시작 전 R-1 4 청크 모두 완료
- R-9 통합 표 작성 전 R-1~R-8 모두 완료
- R-10+ 코드 수정 진입 전 R-9 완료

D. 기술/프로세스 위반
- 빌드 실패 상태로 커밋 (코드 변경 시)
- --no-verify / Git 파괴적 작업 사용자 허가 없이
- ⑤ 원본 로컬 read 가능한데 WebFetch 사용 (로컬 우선)
- vanilla / Mixin 본체 직접 수정 (R 는 read 위주, 코드 수정은 R-10+ 에서)

## 진입 시 첫 액션

1. `docs/fix/focus_01_animation.md` §15 read → 직전 세션 어디까지 진행했나 확인
2. 신규 파일 미생성이면 `docs/research/mapping/research_animation_line_by_line.md` 초기 구조 작성:
   ```
   # 애니메이션 원본 ↔ 1.21.1 라인별 매핑 (Phase R)

   사용자 지시 (2026-04-26): 31 파일 ~5,000줄 라인별 read + 1.21.1 매핑.
   분류: [정합] / [오역] / [누락] / [잉여] / [N/A].
   ```
3. 다음 청크 결정 (예: 첫 진입 시 = R-1 청크 1 / SmartMovingModel.java L1-L200)
4. 원자 실행 루프 진행 (위 10 단계)
5. 한 세션에서 가능한 만큼 진행 (보통 R-단계 하나 = 한 세션). 토큰 부족 시 §15 에 진행
   상태 명확히 기록 후 종료

## 완료 조건 (전체 #1)

- R-1 ~ R-9 모두 [x] (라인별 매핑 표 ~5,000줄 전수)
- R-10+ B-N 모두 [x] (발견된 [오역]/[누락]/[잉여] 모두 1:1 대응)
- 빌드 BUILD SUCCESSFUL
- §14 회귀 감사 통과
- playtest_fixes.md "현재 포커스" → (#1 AI 완결 / 통합테스트 인계)
- 사용자 in-game 통합테스트 후 §3 16 케이스 매칭 확인 시 → 전체 완료

이 규칙 지키면서 작업 진행한다.
