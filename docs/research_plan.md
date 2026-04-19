# SmartMoving 마이그레이션 리서치 계획

## 목표
Minecraft 1.7.10 SmartMoving(Forge/ASM) → 1.21.1 SmartMoving(Fabric/Mixin) 마이그레이션

## 리서치 2단계 접근법

### 1단계: 아키텍처 리서치 (1회)
- 전체 패키지/클래스 구조 파악
- 핵심 진입점 파악 (플레이어 상태, 입력, 렌더링)
- 저장: `docs/research_architecture.md`

### 2단계: 기능별 리서치 (기능마다 반복)
- 기능 하나 = 리서치 파일 하나
- 저장: `docs/research_<기능명>.md`

## 리서치 진행 순서

| 순서 | 주제 | 파일 | 상태 |
|------|------|------|------|
| 1 | 전체 아키텍처 | `research_architecture.md` | ✅ 완료 |
| 2 | 플레이어 상태 관리 | `research_player_state.md` | ✅ 완료 |
| 3 | 입력 처리 | `research_input.md` | ✅ 완료 |
| 4 | 이동 로직 (충돌박스/속도) | `research_movement.md` | 대기 |
| 5 | 렌더링/애니메이션 | `research_animation.md` | ✅ 완료 |
| 6 | 기어가기 (Crawling) | `research_crawling.md` | 대기 |
| 7 | 클라이밍 (Climbing) | `research_climbing.md` | 대기 |
| 8 | 그랩 (Grab) | `research_grab.md` | 대기 |
| 9 | 여우 무빙 (Slide/Prone) | `research_slide.md` | 대기 |
| 10 | 수영 강화 | `research_swimming.md` | 대기 |
| 11 | 점프 강화 | `research_jumping.md` | 대기 |

## 기능별 리서치 파일 템플릿

```markdown
# [기능명] 리서치

## 관련 클래스/파일
- 어떤 클래스들이 이 기능에 관여하는지

## 동작 원리
- 입력 → 상태변경 → 렌더링 흐름

## 핵심 코드 스니펫
- 중요한 로직 발췌 + 설명

## 1.21.1 마이그레이션 포인트
- 원본 방식 → 현재 대체 API

## 미확인 / 추가 조사 필요
- 모르는 것, 불확실한 것
```

## 리서치 → 구현 사이클

```
기능 리서치 완료
  → docs/research_<기능>.md 작성
  → docs/checklist_<기능>.md 작성
  → 구현
  → 리서치 파일 업데이트 (구현 중 새로 알게 된 것)
  → 다음 기능
```
