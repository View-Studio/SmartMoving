# SmartMoving 원본 아키텍처 리서치

> 원본 소스: https://github.com/makamys/SmartMoving (Minecraft 1.7.10, Forge/ASM 기반)

---

## 전체 패키지 구조

```
src/main/java/net/smart/
├── moving/                  # 핵심 mod 패키지 (29개 클래스)
│   ├── SmartMovingMod.java       # 진입점 (FML @Mod)
│   ├── SmartMovingSelf.java      # 플레이어 상태 머신 (103KB, 최대 클래스)
│   ├── SmartMovingBase.java      # 이동/충돌 물리 유틸리티
│   ├── SmartMoving.java          # 상태 추적 + 파티클
│   ├── SmartMovingClient.java    # 클라이언트 exhaustion/점프차지 관리
│   ├── SmartMovingServer.java    # 서버 사이드
│   ├── SmartMovingComm.java      # 네트워크 패킷
│   ├── SmartMovingFactory.java   # 플레이어 인스턴스 관리
│   ├── SmartMovingContext.java
│   ├── SmartMovingOther.java     # 원격 플레이어 처리
│   ├── ClimbGap.java             # 클라이밍 갭 감지
│   ├── FeetClimbing.java         # 발 클라이밍
│   ├── HandsClimbing.java        # 손 클라이밍
│   ├── Orientation.java          # 9방향 시스템
│   ├── Button.java               # 입력 키 관리
│   ├── config/                   # 설정 시스템
│   └── playerapi/                # PlayerAPI 훅
│       ├── SmartMovingPlayerBase.java      # 클라이언트 훅
│       └── SmartMovingServerPlayerBase.java # 서버 훅
├── render/                  # 렌더링 시스템
│   ├── SmartMovingModel.java     # 모델 애니메이션
│   ├── SmartMovingRender.java    # 렌더 파이프라인
│   ├── ModelPlayer.java
│   └── RenderPlayer.java
└── core/                    # ASM 바이트코드 변환
    ├── SmartCorePlugin.java       # FML IFMLLoadingPlugin 진입점
    ├── SmartCoreTransformer.java  # IClassTransformer
    ├── SmartCoreClassVisitor.java # ASM ClassVisitor
    └── SmartCoreMethodVisitor.java # ASM MethodVisitor
```

---

## 전체 아키텍처 레이어

```
Layer 1 (진입)    : FML Plugin + @Mod 애노테이션
Layer 2 (훅)      : PlayerAPI → SmartMovingPlayerBase (메서드 위임)
Layer 3 (상태)    : SmartMovingSelf — 중앙 상태 머신
Layer 4 (물리)    : SmartMovingBase — 충돌/클라이밍/액체 유틸
Layer 5 (애니메이션): SmartMovingModel/Render — 시각 피드백
Layer 6 (네트워크) : 패킷 기반 멀티플레이어 동기화
Layer 7 (바이트코드): ASM 변환 — 더 깊은 통합
```

---

## 플레이어 상태 관리 (SmartMovingSelf)

가장 핵심 클래스. **28개 이상의 boolean 상태 플래그**로 구성.

### 이동 상태 플래그
| 플래그 | 설명 |
|--------|------|
| `isCrawling` | 기어가기 |
| `isSliding` | 미끄러지기 |
| `isClimbing` | 일반 클라이밍 |
| `isCeilingClimbing` | 천장 클라이밍 |
| `isSwimming` | 수영 |
| `isDiving` | 다이빙 |
| `isDipping` | 물 표면 |
| `isLevitating` | 공중 부양 |
| `isFlying` | 비행 |
| `isJumping` | 점프 |
| `isHeadJumping` | 헤드 점프 |
| `isSprintJump` | 스프린트 점프 |
| `isWallJumping` | 벽 점프 |
| `isClimbCrawling` | 클라임+크롤 조합 |
| `isHandsVineClimbing` / `isFeetVineClimbing` | 덩굴 클라이밍 |

### 핵심 메서드
- `updateEntityActionState()` — 매 프레임 호출되는 메인 업데이트
- `moveEntityWithHeading()` — 방향별 이동 처리
- `addToSendQueue()` — 28개 상태 플래그를 64비트 패킷으로 인코딩 (멀티플레이어 동기화)
- `standupIfPossible()` — 자세 전환 처리

### 물리 값
- `exhaustion` — 누적 피로도
- `jumpCharge`, `headJumpCharge` — 기술 차지 추적
- `maxExhaustionForAction` — 행동 가능 임계값

---

## PlayerAPI 훅 시스템 (SmartMovingPlayerBase)

PlayerAPI 라이브러리를 통해 `EntityPlayer`에 커스텀 로직 주입.

### 주요 훅 카테고리
| 카테고리 | 메서드 |
|----------|--------|
| 이동 제어 | `moveEntityWithHeading()`, `beforeMoveEntity()`, `afterMoveEntity()` |
| 물리/충돌 | `pushOutOfBlocks()`, `isInsideOfMaterial()` |
| 애니메이션 | `getBrightness()`, `getFOVMultiplier()` |
| 상태 유지 | `updateEntityActionState()`, `writeEntityToNBT()` |

패턴: `localGetBrightness()` 같은 "local" 접두어로 부모 동작 보존 + 폴백 체인 구현.

---

## 이동/충돌 물리 (SmartMovingBase)

### 충돌 감지
- `calculateSeparateCollisions()` — 축 정렬 충돌 + 계단 오르기
- `getMaxPlayerSolidBetween()` / `getMinPlayerSolidBetween()` — 경계 감지
- `isCollided()` — 바운딩 박스 교차 테스트

### 클라이밍 시스템
- `getOnLadderOrVine()` — 오를 수 있는 블록 감지
- `Orientation.java` — **9방향 시스템** (중심 ZZ + 8방향) 으로 클라이밍 각도 계산
- `supportsCeilingClimbing()` — 블록 메타데이터 분석

### 액체 처리
- `getLiquidBorder()` — 물 높이 계산
- `reverseHandleMaterialAcceleration()` — 물 물리 수정

---

## 애니메이션 시스템 (SmartMovingModel)

### 핵심 구조
```java
// 매 프레임 호출 — 20개 이상의 상태 플래그 분기
setRotationAngles() {
    if (isCrawling) { ... }
    else if (isClimbing) { ... }
    else if (isSwimming) { ... }
    // ...
}
```

### 애니메이션 기법
- **진동 (Oscillation)**: `MathHelper.cos()` + 거리 배수로 사지 순환 움직임
- **보간 (Interpolation)**: `Factor()` 유틸로 속도 임계값 기반 부드러운 전환
- **기구학 (Kinematics)**: 천장 높이 기반 삼각함수로 크롤-클라임 각도 계산

### 신체 파트 (12개)
Head, LeftArm, RightArm, LeftLeg, RightLeg, Body, LeftShoulder, RightShoulder, Pelvis 등

---

## 렌더링 시스템 (SmartMovingRender)

- `renderPlayer()` — 18개 이상 이동 플래그 추출 후 3개 모델 동기화
  - 메인 바이패드 모델 (전체 스케일)
  - 흉갑 갑옷 모델 (팔/다리 스케일 없음)
  - 표준 갑옷 모델 (선택적 스케일)
- `renderPlayerAt()` — 크롤링 플레이어 높이 오프셋 보정 (멀티플레이어)
- `renderGuiIngame()` — 커스텀 exhaustion 바, 점프차지 바 UI

---

## 네트워크 동기화 (SmartMovingComm)

- 상태 패킷: 이동 동기화 (28개 플래그 → 64비트)
- 설정 패킷: 서버 설정 배포
- `SmartMovingFactory`: 싱글톤(로컬 플레이어) + 해시테이블(원격 플레이어) 인스턴스 관리

---

## ASM 변환 (SmartCorePlugin)

- `SmartCorePlugin` — FML `IFMLLoadingPlugin` 진입점
- `SmartCoreTransformer` — 클래스 로딩 시 바이트코드 변환 (`IClassTransformer`)
- 개발/프로덕션 환경 자동 감지 (obfuscation 여부)
- **목적**: Reflection보다 낮은 오버헤드, 초기화 전 베이스 클래스 수정 가능

---

## 1.21.1 Fabric 마이그레이션 대응표

| 원본 (1.7.10 Forge) | 현재 (1.21.1 Fabric) |
|---|---|
| `PlayerAPI` + `SmartMovingPlayerBase` | `@Mixin` on `ClientPlayerEntity` / `ServerPlayerEntity` |
| Forge Events | `ServerTickEvents`, `ClientTickEvents` (Fabric) |
| `SmartCoreTransformer` (ASM) | Mixin `@Inject`, `@Redirect`, `@ModifyVariable` |
| 커스텀 패킷 | `ServerPlayNetworking` / `ClientPlayNetworking` (Fabric) |
| `SmartMovingModel.setRotationAngles()` | `@Mixin(PlayerEntityModel)` → `setAngles()` |
| 커스텀 Config | Cloth Config API (또는 직접 구현) |
| `SmartMovingFactory` 인스턴스 관리 | `AttachmentType` (Fabric 1.21+) |

---

## 미확인 / 추가 조사 필요

- [ ] `Orientation.java` 9방향 시스템 세부 구현 — 클라이밍 방향 계산에 핵심
- [ ] `SmartCoreTransformer`가 정확히 어떤 클래스/메서드를 변환하는지
- [ ] `Factor()` 보간 유틸 구현 세부 사항
- [ ] 멀티플레이어에서 원격 플레이어 애니메이션 동기화 방식 (64비트 패킷 디코딩)
- [ ] 천장 클라이밍 시 충돌박스 변경 로직
