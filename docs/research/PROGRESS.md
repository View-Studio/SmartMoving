# 리서치 진행 현황

새 세션 시작 시 이 파일을 먼저 읽고, 체크 안 된 항목 중 맨 위부터 하나씩 진행한다.
하나 완료할 때마다 체크하고 커밋한다. 한 세션에 하나만 해도 된다.

---

## 작업 순서 원칙

1. 한 세션에 **파일 하나 또는 시스템 하나**만 한다.
2. 그 파일/시스템을 **완전히** 읽고 문서화한다. 절반만 하고 넘어가지 않는다.
3. 완료 즉시 커밋한다. 커밋이 곧 진행 저장이다.
4. 파일이 너무 크면 (SmartMovingSelf.java 등) 시스템 단위로 쪼개서 여러 세션에 나눠도 된다.
   - 예: SmartMovingSelf — 크롤링/슬라이딩 부분만 → 완료 → 커밋 → 다음 세션에 수영 부분

---

## SmartRender 원본 리서치

저장 위치: `docs/research/original/smartrender/`

- [ ] `SmartRenderModel.java` — 애니메이션 핵심 (17KB, 최우선)
- [ ] `ModelRotationRenderer.java` — 고급 회전 처리
- [ ] `SmartRenderRender.java` — 렌더 파이프라인
- [ ] `ModelPlayer.java` — ModelBiped 래퍼
- [ ] `RenderPlayer.java` — RenderPlayer 래퍼
- [ ] `IModelPlayer.java` / `IRenderPlayer.java` — 인터페이스
- [ ] `SmartRenderContext.java`, `SmartRenderUtilities.java`, 나머지 전체

---

## SmartMoving 원본 리서치

저장 위치: `docs/research/original/smartmoving/`

- [ ] `SmartMovingSelf.java` — 상태머신 전체 (103KB, 가장 큰 파일 — 시스템별로 쪼갤 것)
  - [ ] 크롤링/슬라이딩
  - [ ] 클라이밍/천장클라이밍
  - [ ] 수영/잠수
  - [ ] 점프 (여우무빙 포함)
  - [ ] 입력 처리
  - [ ] 탈진 시스템
- [ ] `SmartMovingBase.java` — 물리 유틸리티
- [ ] `SmartMovingConfig.java` — 설정값 전체
- [ ] `SmartMovingComm.java` — 네트워크 패킷
- [ ] `SmartMovingClient.java` / `SmartMovingServer.java`
- [ ] 나머지 전체

---

## vanilla 1.21.1 리서치

저장 위치: `docs/research/vanilla/`

- [ ] `LivingEntity.travel()` — 전체 흐름
- [ ] `LivingEntity.tick()` / `tickMovement()` — leaningPitch, 포즈 갱신
- [ ] `LivingEntity.updatePose()` → `trySetPose()` — 포즈 결정 로직
- [ ] `PlayerEntityRenderer.setupTransforms()` — 수영 회전 포함 전체
- [ ] `PlayerEntityModel.setAngles()` — vanilla가 파트에 설정하는 것 전체
- [ ] `LivingEntityRenderer.render()` — 렌더 파이프라인 순서
- [ ] `Entity.calculateDimensions()` — BoundingBox 갱신 흐름
- [ ] `LivingEntity.jump()` — 점프 속도 설정
- [ ] 서버 위치/속도 보정 코드 경로

---

## 교차 분석

저장 위치: `docs/research/mapping/`

각 시스템의 원본 리서치 + vanilla 리서치가 둘 다 완료된 후에 진행.

- [ ] 애니메이션 시스템 교차 분석
- [ ] 수영/잠수 교차 분석
- [ ] 크롤링/슬라이딩 교차 분석
- [ ] 클라이밍 교차 분석
- [ ] 점프 교차 분석
- [ ] 이동 속도/물리 교차 분석
