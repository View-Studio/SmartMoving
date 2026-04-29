# 엎드리기 애니메이션 1:1 fix 체크리스트

리서치: `docs/research_crawl_animation.md`
대상: `MixinPlayerEntityModelClient.sm_animateCrawling` (L805-L855)

---

## Phase 1 — HIGH 우선순위 (한 번에 모두 적용 가능)

### [ ] (2-1) `distance` 입력 정정
- 이전: `distance = limbSwing * 1.3f`.
- 새: `distance = sm.stats.getTotalHorizontalDistance(partialTicks) * 1.3f` (partial tick lerp).

### [ ] (2-2) `walkFactor`/`standFactor` 입력 정정
- 이전: `smFactor(limbSwingAmount, ...)`.
- 새: `smFactor(sm.stats.currentHorizontalSpeedFlattened, ...)` (원본 직접 read, lerp 없음).

### [ ] (2-3) `body` 회전 순서 YZX 매핑
- 이전: 직접 `body.pitch / body.yaw / body.roll` set (ModelPart 기본 ZYX).
- 새: `setAnglesYZX(body, pitch, yaw, roll)` 헬퍼 사용 (그랩 클라이밍 팔 패턴).

**시그니처 변경 필요**: `sm_animateCrawling(float, float, float, float)` → `sm_animateCrawling(SmartMovingClientState sm, float headYaw)`. 호출처 수정 (L318).

---

## Phase 2 — LOW (선택사항)

### [ ] (2-4) scale 가드 명시
- 원본 `if (scaleLegType != NoScaleStart)` / `scaleArmType != NoScaleStart` 가드.
- 메인 모델 = Scale 모드라 통과. 결과 동일.
- 명시성만 — 후순위.

---

## 잔존 차이 (의도된 차이 또는 1.21.1 매핑 한계)

- **bipedBody (Y) vs body 단일 노드** — 1.21.1 ModelPart parallel 구조라 정밀 1:1 어려움. body.X 큰 (78°) + Y/Z 작은 cos → 자식 효과 미세 → 시각 영향 미세.
- **viewHorizontalAngelOffset** = `headYaw` 매개변수 등가 (vanilla netHeadYaw). 매핑 정확.

---

## 작업 흐름

1. Phase 1 (2-1, 2-2, 2-3) 한 번에 적용 (서로 의존).
2. 빌드 → 사용자 인게임 검증.
3. OK 면 task #12 완료 + 커밋.
4. Phase 2 별도 요청 시.
