# Focus #3 — 상태 전환 조건 이상

> 진입점: [`playtest_fixes.md`](playtest_fixes.md) → 현재 포커스가 #3 일 때 진입.

---

## 1. 진행 상황

| 필드 | 값 |
|------|---|
| 상태 | ⚪ 대기 (재현 케이스 수집 필요 / #2 선행) |
| 현재 단계 | 재현 케이스 수집 |
| 선행 의존 | #2 (상태값이 정확해야 전환 조건 검증 의미 있음) |

---

## 2. 증상 — 원본 vs 현재

특정 **조합** 에서 상태가 원본과 다른 **타이밍**에 전환됨. 혹은 전환 자체가 일어나지
않음. 플래그는 맞는데 전환 경로가 어긋나는 경우.

전형적인 의심:
- 크롤링 ↔ 수영 ↔ 잠수 전환
- 슬라이딩 진입 조건
- 클라이밍 → 크롤링 ↔ 점프
- 벽 점프 더블클릭 타이머 리셋

---

## 3. 재현 케이스 표 ⚠️ **진입 전 필수 수집**

| # | 시나리오 | 대상 전환 | 원본 기대 트리거 | 1.21.1 실제 | 원본 라인 근거 |
|---|---------|---------|---------------|-----------|-------------|
| 1 | (예) 전속력 + sneak + onGround | idle → isSliding=true | `sprint && sneak && onGround && !climbing && !headJump` | ? | `SmartMovingSelf.md` IMPL-02 대응 |
| 2 | (예) 물에서 벽으로 부딪힘 | isSwimming → isJumpingOutOfWater | `wantJumpOutOfWater && waterMovementTicks>10` | ? | `SmartMovingSwimmer.handleSwimming` §8-5 |
| 3 | | | | | |

---

## 4. 의존 관계

| 관계 | 대상 |
|------|------|
| 선행 의존 | **#2** (상태값 정확성) |
| 영향받는 후속 | #4 (키 커맨드 결과), #1 (애니메이션 전환 순간) |
| 영향 주는 완료 이식 | IMPL-01 크롤링 전환 / IMPL-02 슬라이딩 / `updateWallJumpState` / `handleClimbing` |

---

## 5. 원본 근거

### 5.1. 리서치 파일 인덱스

| 전환 영역 | 리서치 파일 | 섹션 |
|----------|------------|------|
| 크롤링 진입/유지/해제 | `SmartMovingSelf.md` | L2419-L2432 wouldWantCrawl / L2474 isCrawling = canCrawl && (wantCrawl \|\| mustCrawl) |
| 슬라이딩 진입 | `SmartMovingSelf.md` | IMPL-02 대응, slidingSpeedStopFactor 참조 |
| 벽 점프 트리거 | `SmartMovingSelf.md` + `mapping/jump.md` | L2863-2897 canWallJumping / wantWallJumping |
| 수영→크롤링 전환 | `SmartMovingSelf.md` | handleSwimming 내 standupIfPossible |
| 클라이밍 진입 | `SmartMovingSelf.md` | wouldWantClimb L1814-1822 |

### 5.2. 확보 필요

재현 케이스별 원본 전환 블록을 리서치 파일에서 확인. 부족하면 WebFetch:

```
대상 URL: SmartMovingSelf.java
추출 대상:
  1. <케이스 #N 대상 전환> 이 일어나는 조건식 전체
  2. 해당 전환이 호출되는 메서드 + 주변 30줄
  3. 전환 전후 다른 상태 플래그의 변경 흐름
```

---

## 6. 1:1 매핑 테이블

(재현 케이스 확보 후 구체 조건별로 작성)

---

## 7. 구조적 차이 / 근사 이식 지점

- `player.isSneaking()` ↔ `sm_isSneaking` override 순환 (sm_isSneaking 이 isSlow 참조)
  → 일부 조건에서 raw sneakKey 사용으로 회피 (이미 R-09 에서 적용)
- Button.StartPressed / StopPressed ↔ 엣지 감지 (`jumpKeyStartPressed` 등) — 이미 이식
- IMPL-01 블록 위치가 원본 `updateEntityActionState` 흐름 순서와 완전 일치하는가 점검

---

## 8. 현재 구현 스냅샷

재현 케이스 확보 후 해당 전환 블록 임베드.

---

## 9. 예상 수정 diff

케이스별 원인 식별 후 작성.

---

## 10. 원자 단위 작업 목록

### P. 재현 케이스 수집
- [ ] P-1. §3 표 최소 3행 채움
- [ ] P-2. 원본 기대 조건을 리서치 파일 라인 번호로 확인

### A. 원인 분석
- [ ] A-N. (케이스별 원본 조건식 vs 1.21.1 구현 대조)

### B. 수정
- [ ] B-N. (원인별)

### C. 검증
- [ ] C-1. 빌드
- [ ] C-2. 재현 케이스 전부 매칭
- [ ] C-3. 회귀 방지
- [ ] C-4. `playtest_fixes.md` "현재 포커스" → `#4` 갱신

---

## 11. 호출 타이밍 검증

원본 `updateEntityActionState` → handleJumping → handleSwimming → handleLava →
handleAlternativeFlying → handleLand → handleWallJumping 순서.
1.21.1 `sm_travel_client` 순서와 원본 순서 **실제 일치** 여부 재검토.

---

## 12. 테스트 프로토콜

(재현 케이스별 구체 테스트 수순)

---

## 13. 완료 전 검증 체크리스트

- [ ] §3 표 전부 매칭
- [ ] 각 전환 조건 원본 라인과 1:1 대응
- [ ] 훅 호출 순서가 원본과 일치
- [ ] 회귀 방지 감사 통과
- [ ] 빌드 성공

---

## 14. 회귀 방지 감사

| 기존 이식 | 영향 포인트 | 확인 |
|----------|-----------|------|
| `wantCrawl`/`mustCrawl` pre-compute | isSlow 이전 계산 순서 보장 | [ ] |
| R-09 토글 블록 | 토글 블록이 전환 플래그 소비 | [ ] |
| `fromSwimmingOrDiving` | 수영↔크롤 전환 연쇄 | [ ] |
| `wantWallJumping` 자기참조 식 | 이전 틱 값 사용 | [ ] |

---

## 15. 작업 기록

_(비어있음)_

---

## 16. 신규 발견

_(비어있음)_

---

## 17. 잔여 / 후속

- sm_travel_client 내 훅 호출 순서가 원본 updateEntityActionState 와 완전 일치하는지
  감사 — 별도 포커스 후보

---

## 18. 확정 원자 (포커스 #2 Extended 에서 이관)

### 18.1. B-N-standup-4 — vanilla `capabilities.isFlying` 자동 해제 복원

**출처**: 포커스 #2 Extended 세션 115 B-N-standup 근사 (4) / 세션 136 재평가. AABB 정밀
계열 (근사 1/2/3) 은 세션 136 에서 해소됨, 이것 하나만 client-server 네트워크 sync 필요 —
상태 전환 트리거 계열이라 #3 으로 이관.

**원본** (SmartMovingSelf L2196-L2201, `standupIfPossible(tryLanding, restoreFromFlying)`):
```java
if (tryLanding && groundClose && standUpPossible) {
    isFlying = false;                        // SM 내부 플래그
    sp.capabilities.isFlying = false;         // ★ vanilla 비행 상태 해제
    restoreFromFlying = true;
}
```

**트리거 조건** (정확 1:1):
- `tryLanding = isFlying && !Options._flyCloseToGround && horizontalSpeedSquare < 0.003D
  && sp.motionY > -0.03D` (원본 L2542) — 비행 중 속도 낮음 + 하강 시작
- `groundClose = gapUnderneight < 1D` (발 아래 1 블록 이내)
- `standUpPossible = gapUnderneight + gapOverneight >= 1D` (위·아래 공간 충분)

→ 3 조건 동시 만족 시 **vanilla Creative 비행 자동 해제** + `restoreFromFlying=true` 로
  `standUp()` 실행 (hitbox 복원 + 크롤/헤드점프 해제).

**현재 1.21.1 상태** (세션 136):
- `ClientState.standupIfPossible(player, tryLanding, restoreFromFlying)` 정밀 복원 (gap 공식
  전수) — 그러나 L2199 `sp.capabilities.isFlying = false` 는 **SM 측 `isFlying = false` 만**
  실행. vanilla `player.getAbilities().flying` 은 건드리지 않음.
- §7 근사 **B-N-standup-approx-4** 로 기록됨.

**현재 영향** (Creative 모드 한정):
- Creative 비행 중 좁은 공간 접근 → 원본은 자동 비행 해제 + 착지 → 1.21.1 은 SM 플래그만
  해제, vanilla 비행 계속. 플레이어가 수동 `F` 로 해제 필요.
- Survival 무관 (vanilla 비행 없음, SM SmartFlying 은 별도 경로).

**이식 불가 이유** (1.21.1 제약):
1. `player.getAbilities().flying = false` 를 client 측에서 직접 설정해도 **서버가 검증 없이
   받아주지 않음** — 다음 tick 에 서버 상태로 덮어씀.
2. 서버 → 클라이언트 동기화는 `PlayerAbilitiesS2CPacket` 으로 서버가 push. 클라 → 서버는
   `UpdatePlayerAbilitiesC2SPacket` 전송 필요.
3. SM 이 client 측 상태 전환을 결정해도, 서버에 알려주고 서버가 승인해야 함.

**해소 계획 원자**:
- [ ] **18.1a** MixinPlayer `@Accessor` 로 `PlayerAbilities.flying` setter 노출. 또는
      `player.getAbilities().flying` 이 public field 라 직접 할당 가능한지 재확인
      (B-42-B18b 패턴).
- [ ] **18.1b** `UpdatePlayerAbilitiesC2SPacket` 전송 로직 추가 — vanilla `Abilities` 변경 시
      client 가 server 로 ability state 전송. vanilla `ClientPlayerEntity.tickMovement` 에
      이 패턴 있음 (참고). 또는 `player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(...))`
      직접 호출.
- [ ] **18.1c** `standupIfPossible(player, tryLanding, restoreFromFlying)` 2-arg 오버로드의
      `tryLanding && groundClose && standUpPossible` 분기 본문 복원:
      ```java
      this.isFlying = false;
      player.getAbilities().flying = false;      // 18.1a 완료 후
      player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
      restoreFromFlying = true;
      ```
- [ ] **18.1d** 원본 L2542 `tryLanding` 계산 정밀 이식 확인:
      `boolean tryLanding = isFlying && !Options._flyCloseToGround.value
                         && horizontalSpeedSquare < 0.003D && sp.motionY > -0.03D`
      — 1.21.1 에 해당 계산 + `standupIfPossible(player, tryLanding, restoreFromFlying)`
      호출 경로 grep 확인. 원본 L2543-L2544 `if(restoreFromFlying || tryLanding)
      standupIfPossible(tryLanding, restoreFromFlying);` 호출 구조 이식.
- [ ] **18.1e** `Options._flyCloseToGround` Config 필드 이식 여부 확인. 미이식 시 추가
      (기본값 Unmodified = true). 원본 Options 위치이므로 `SmartMovingConfig` 에 boolean.
- [ ] **18.1f** 빌드 (`./gradlew compileJava compileClientJava --rerun-tasks`) + Creative
      플레이테스트 (비행 중 좁은 공간 접근 시 자동 비행 해제 + 착지 + 서버 sync 확인).

**트리거 재평가** (2026-04-25 세션 136):
- 1.21.1 에 `tryLanding` 계산 + `standupIfPossible(player, tryLanding, restoreFromFlying)`
  호출 경로가 **있는지** 확인 필요. 원본 L2542-L2544 대응 코드 없으면 18.1d 선행.
- 현재는 `standupIfPossible(player)` (무인자) 만 주로 호출되고, 2-arg 버전은 B-24 세션 53
  에서 `restoreFromFlying=true` 직후 1곳 호출만 있음 — 전수 확인 필요.

**원본 라인 참조**:
- `SmartMovingSelf.java` L2542 `tryLanding` 계산
- `SmartMovingSelf.java` L2543-L2544 `standupIfPossible(tryLanding, restoreFromFlying)` 호출
- `SmartMovingSelf.java` L2196-L2201 vanilla isFlying 해제 분기
- `SmartMovingConfig.java` `_flyCloseToGround` Config 필드 정의

**의존**:
- 포커스 #2 Extended 완료 (세션 134) + 세션 136 B-N-standup-3 해소 (gap 공식 복원) ✓
- B-42-B18b 결과 재사용 — vanilla public boolean field 에 Mixin 없이 할당 가능 여부 확인

**규모**: 소-중 (6 원자 / 2-3 세션 예상). Mixin 대신 public field 직접 접근 가능하면 네트워크
패킷 전송만 추가. 복잡성은 tryLanding 트리거 + sync 패킷 검증.

**회귀 감사**:
- Creative 플레이어만 영향 — Survival 무관.
- SM 측 `isFlying = false` 는 세션 115 에서 이미 이식. 이 해소는 vanilla 쪽 sync 만 추가.
- B-24 `restoreFromFlying=true` → `standupIfPossible(player, false, true)` 호출 구조는 유지.
  새 경로는 `tryLanding && ...` 조건 분기 추가.
