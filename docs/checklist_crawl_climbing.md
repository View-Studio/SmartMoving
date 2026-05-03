# Crawl-Climbing 작업 체크리스트

`docs/research_crawl_climbing.md` 기반.

## ⚠ 엄격 가드

- `sm_animateClimbing` 의 isCrawlClimbing 추가 분기 (L642-L673) 만 수정.
- isClimb 일반 매핑 (L465+) 영향 없음.
- crawl-climbing 기능 (state/box/collision) 절대 침범 X.

## ✅ 완결 (2026-05-04)

옵션 D (= setupTransforms root R_x + setAngles cancel) 매핑 + 3축 별도 fade 정착.
사용자 명시 "너무 완벽하다. 원본보다 더 부드럽고, 원본의 움직임은 그대로 가져오고" (2026-05-04).

자세한 내용은 메모리 `project_crawl_climbing_complete.md` 참조.
