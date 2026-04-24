package choco.ratel.smartmoving.climbing;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;

/**
 * 원본 `net.smart.moving.Orientation` (SmartMoving 1.7.10) 1:1 이식.
 *
 * **B-19a0 (세션 90) 범위**: 기본 구조 — 9 상수 + `_i`/`_k` + 각도 판정 헬퍼 + `rotate` +
 * `getOrientation` + `getClimbingOrientations` + `getHorizontalBorderGap`.
 *
 * **추후 서브 원자 범위 (미포함)**:
 *   * B-19a1: `SmartMovingContext` 헬퍼 (isOnLadderOrVine / isOnOpenTrapDoor / isRope /
 *     isOnWallRope / isBaseAccessible / isFullAccessible / isFullExtentAccessible /
 *     isJustLowerHalfExtentAccessible / isFullEmpty / isSolid) 이식
 *   * B-19a2: `isLadderSubstitute` 본체 + `hasHalfHold` / `hasBottomHold` — gap 1-5 계산 +
 *     `ClimbGap.canStand/mustCrawl` 설정 핵심
 *   * B-19a3: `handsClimbing()` / `feetClimbing()` 판정 메서드
 *   * B-19a4: `seekClimbGap` 메서드 + Climber.handleClimbing 연결
 *
 * B-19a0 는 `SmartMovingContext` 확장 없이 독립 클래스. B-19a1 완료 시
 * `extends SmartMovingContext` 재도입 + `isTunnelAhead` / `getKnownLadderOrientation` /
 * `isFeetLadderSubstitute` / `isHandsLadderSubstitute` / `baseVineClimbing` 등 추가.
 *
 * **원본 근거**: `.tmp_research/Orientation.java.md` L37-L205 + L998-L1085.
 */
public class Orientation {

    // ── 9 방향 상수 (원본 L39-L49) ──────────────────────────────────────────

    /** 원본 L39 — 중앙 (기본 값). rotate 에서 제외. */
    public static final Orientation ZZ = new Orientation(0, 0);

    /** 원본 L41 — +X 방향 (East). */
    public static final Orientation PZ = new Orientation(1, 0);
    /** 원본 L42 — +Z 방향 (South). */
    public static final Orientation ZP = new Orientation(0, 1);
    /** 원본 L43 — -X 방향 (West). */
    public static final Orientation NZ = new Orientation(-1, 0);
    /** 원본 L44 — -Z 방향 (North). */
    public static final Orientation ZN = new Orientation(0, -1);

    /** 원본 L46 — +X+Z 대각 (SE). */
    public static final Orientation PP = new Orientation(1, 1);
    /** 원본 L47 — -X-Z 대각 (NW). */
    public static final Orientation NN = new Orientation(-1, -1);
    /** 원본 L48 — +X-Z 대각 (NE). */
    public static final Orientation PN = new Orientation(1, -1);
    /** 원본 L49 — -X+Z 대각 (SW). */
    public static final Orientation NP = new Orientation(-1, 1);

    // ── Meta / 내부 상수 (원본 L51-L63) ─────────────────────────────────────

    /** 원본 L51 — 미설정 (기본). */
    public static final int DefaultMeta = -1;
    /** 원본 L52 — 덩굴이 정면에 있는 경우. */
    public static final int VineFrontMeta = 0;
    /** 원본 L53 — 덩굴이 측면에 있는 경우. */
    public static final int VineSideMeta = 1;

    /** 원본 L55-L59 — 수직 탐색 오프셋 (top=+2, middle=+1, base=0, sub=-1, subSub=-2). */
    protected static final int top    =  2;
    protected static final int middle =  1;
    protected static final int base   =  0;
    protected static final int sub    = -1;
    protected static final int subSub = -2;

    /** 원본 L61-L63 — 잡기 종류. */
    protected static final int NoGrab     = 0;
    protected static final int HalfGrab   = 1;
    protected static final int AroundGrab = 2;

    // ── Orthogonals HashSet (원본 L65-L73) ──────────────────────────────────

    /** 원본 L65-L73 — 4 직교 방향 HashSet. 정적 초기화. */
    public static final HashSet<Orientation> Orthogonals = new HashSet<>();

    static {
        Orthogonals.add(PZ);
        Orthogonals.add(ZP);
        Orthogonals.add(NZ);
        Orthogonals.add(ZN);
    }

    // ── 인스턴스 필드 (원본 L75-L79) ────────────────────────────────────────

    /** 원본 L75 — X 오프셋 (-1/0/+1). */
    protected int _i;
    /** 원본 L75 — Z 오프셋 (-1/0/+1). */
    protected int _k;
    /** 원본 L76 — 대각 여부 (`_i != 0 && _k != 0`). */
    private boolean _isDiagonal;
    /** 원본 L77 — 방향 각도 (도 단위, 0-360). */
    private float _directionAngle;
    /** 원본 L78 — 등반 가능 최소 각도. */
    private float _mimimumClimbingAngle;
    /** 원본 L79 — 등반 가능 최대 각도. */
    private float _maximumClimbingAngle;

    // ── 생성자 (원본 L81-L88) ───────────────────────────────────────────────

    private Orientation(int i, int k) {
        _i = i;
        _k = k;
        _isDiagonal = _i != 0 && _k != 0;

        setClimbingAngles();
    }

    // ── 각도 계산 (원본 L998-L1062) ─────────────────────────────────────────

    /**
     * 원본 L998-L1037 `setClimbingAngles()` — `_i`/`_k` 조합별 방향 각도 스위치.
     * 원본 기준 좌표계: +X = West(NZ), +Z = South(ZP), 0도 = South.
     *
     *   (-1,-1)=NN → 135도
     *   (-1, 0)=NZ →  90도
     *   (-1,+1)=NP →  45도
     *   ( 0,-1)=ZN → 180도
     *   ( 0, 0)=ZZ → 0~360 (전체)
     *   ( 0,+1)=ZP →   0도
     *   (+1,-1)=PN → 225도
     *   (+1, 0)=PZ → 270도
     *   (+1,+1)=PP → 315도
     */
    private boolean setClimbingAngles() {
        switch (_i) {
            case -1:
                switch (_k) {
                    case -1: return setClimbingAngles(135F);
                    case  0: return setClimbingAngles( 90F);
                    case  1: return setClimbingAngles( 45F);
                }
                break;
            case 0:
                switch (_k) {
                    case -1: return setClimbingAngles(180F);
                    case  0: return setClimbingAngles(0F, 360F);
                    case  1: return setClimbingAngles(  0F);
                }
                break;
            case 1:
                switch (_k) {
                    case -1: return setClimbingAngles(225F);
                    case  0: return setClimbingAngles(270F);
                    case  1: return setClimbingAngles(315F);
                }
                break;
        }
        return false;
    }

    /**
     * 원본 L1039-L1047 `setClimbingAngles(float directionAngle)`.
     * `_freeClimbingDiagonalDirectionAngle` (80F) / `_freeClimbingOrthogonalDirectionAngle`
     * (90F) 설정값의 절반을 전방 방향각 양쪽으로 펼쳐 min/max 계산.
     */
    private boolean setClimbingAngles(float directionAngle) {
        _directionAngle = directionAngle;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        float halfAreaAngle = (_isDiagonal
                ? cfg.freeClimbingDiagonalDirectionAngle
                : cfg.freeClimbingOrthogonalDirectionAngle) / 2F;
        return setClimbingAngles(directionAngle - halfAreaAngle,
                directionAngle + halfAreaAngle);
    }

    /**
     * 원본 L1049-L1062 `setClimbingAngles(min, max)`. 음수/360+ 정규화 후 필드 저장.
     */
    private boolean setClimbingAngles(float mimimumClimbingAngle, float maximumClimbingAngle) {
        if (mimimumClimbingAngle < 0F)   mimimumClimbingAngle += 360F;
        if (maximumClimbingAngle > 360F) maximumClimbingAngle -= 360F;

        _mimimumClimbingAngle = mimimumClimbingAngle;
        _maximumClimbingAngle = maximumClimbingAngle;

        return mimimumClimbingAngle != maximumClimbingAngle;
    }

    /**
     * 원본 L1064-L1069 — 이 방향의 전방각(`_directionAngle`) 이 주어진 회전 범위 내인지.
     * `getOrientation` 에서 플레이어 회전 허용 범위에 맞는 방향 찾을 때 사용.
     */
    private boolean isWithinAngle(float minimumRotation, float maximumRotation) {
        return isWithinAngle(_directionAngle, minimumRotation, maximumRotation);
    }

    /**
     * 원본 L1071-L1075 — 주어진 회전이 이 방향의 등반 가능 각도 범위 내인지.
     * `seekClimbGap` / `addTo` 에서 사용.
     */
    protected boolean isRotationForClimbing(float rotation) {
        return isWithinAngle(rotation, _mimimumClimbingAngle, _maximumClimbingAngle);
    }

    /**
     * 원본 L1077-L1085 — 회전 값이 [min, max] 범위 내인지. min > max 인 경우 (0도 경계
     * 통과) OR 로 처리.
     */
    private static boolean isWithinAngle(float rotation, float minimumRotation, float maximumRotation) {
        if (minimumRotation > maximumRotation)
            return rotation >= minimumRotation || rotation <= maximumRotation;
        return rotation >= minimumRotation && rotation <= maximumRotation;
    }

    // ── rotate (원본 L90-L165) ──────────────────────────────────────────────

    /**
     * 원본 L90-L165 — 이 방향을 `angle` 도만큼 회전한 결과 반환.
     * 지원 각도: 0 / ±45 / ±90 / ±135 / ±180.
     * ZZ 는 회전 불가 (RuntimeException).
     */
    public Orientation rotate(int angle) {
        if (this == ZZ) throw new RuntimeException("unrotatable orientation");

        switch (angle) {
            case 0:
                return this;
            case 45:
                if (this == PZ) return PP;
                if (this == PP) return ZP;
                if (this == ZP) return NP;
                if (this == NP) return NZ;
                if (this == NZ) return NN;
                if (this == NN) return ZN;
                if (this == ZN) return PN;
                if (this == PN) return PZ;
                throw new RuntimeException("unknown orientation \"" + this + "\"");
            case -45:
                if (this == PZ) return PN;
                if (this == PN) return ZN;
                if (this == ZN) return NN;
                if (this == NN) return NZ;
                if (this == NZ) return NP;
                if (this == NP) return ZP;
                if (this == ZP) return PP;
                if (this == PP) return PZ;
                throw new RuntimeException("unknown orientation \"" + this + "\"");
            case 90:   return rotate(45).rotate(45);
            case -90:  return rotate(-45).rotate(-45);
            case 135:  return rotate(180).rotate(-45);
            case -135: return rotate(-180).rotate(45);
            case 180:
            case -180:
                if (this == PZ) return NZ;
                if (this == PN) return NP;
                if (this == ZN) return ZP;
                if (this == NN) return PP;
                if (this == NZ) return PZ;
                if (this == NP) return PN;
                if (this == ZP) return ZN;
                if (this == PP) return NN;
                throw new RuntimeException("unknown orientation");
        }
        throw new RuntimeException("angle \"" + angle + "\" not supported");
    }

    // ── getOrientation (원본 L167-L205) ─────────────────────────────────────

    /**
     * 원본 L167-L205 — 플레이어 회전각 + 허용범위 기반으로 매칭되는 Orientation 반환.
     * 우선순위: orthogonal 먼저 (NZ/PZ/ZN/ZP), 그 다음 diagonal (NP/PN/NN/PP).
     * 매칭 없으면 null.
     *
     * 1.21.1 매핑: `p.rotationYaw` → `player.getYaw()`.
     */
    public static Orientation getOrientation(PlayerEntity p, float tolerance,
                                             boolean orthogonals, boolean diagonals) {
        float rotation = p.getYaw() % 360F;
        if (rotation < 0) rotation += 360F;

        float minimumRotation = rotation - tolerance;
        if (minimumRotation < 0) minimumRotation += 360F;

        float maximumRotation = rotation + tolerance;
        if (maximumRotation >= 360F) maximumRotation -= 360F;

        if (orthogonals) {
            if (NZ.isWithinAngle(minimumRotation, maximumRotation)) return NZ;
            if (PZ.isWithinAngle(minimumRotation, maximumRotation)) return PZ;
            if (ZN.isWithinAngle(minimumRotation, maximumRotation)) return ZN;
            if (ZP.isWithinAngle(minimumRotation, maximumRotation)) return ZP;
        }
        if (diagonals) {
            if (NP.isWithinAngle(minimumRotation, maximumRotation)) return NP;
            if (PN.isWithinAngle(minimumRotation, maximumRotation)) return PN;
            if (NN.isWithinAngle(minimumRotation, maximumRotation)) return NN;
            if (PP.isWithinAngle(minimumRotation, maximumRotation)) return PP;
        }
        return null;
    }

    // ── getClimbingOrientations + addTo (원본 L262-L297) ────────────────────

    private static HashSet<Orientation> _getClimbingOrientationsHashSet = null;

    /**
     * 원본 L262-L289 — 플레이어 회전각 기반으로 등반 가능한 방향 집합 반환.
     * orthogonals=true → NZ/PZ/ZN/ZP 체크. diagonals=true → NP/PN/NN/PP 체크.
     * 각 방향이 자신의 `_mimimumClimbingAngle ~ _maximumClimbingAngle` 범위에 rotation 을
     * 포함하면 add.
     *
     * **주의**: 반환 HashSet 은 정적 캐시 재사용 — 호출 간 mutate 주의.
     * 1.21.1 매핑: `p.rotationYaw` → `player.getYaw()`.
     */
    public static HashSet<Orientation> getClimbingOrientations(PlayerEntity p,
                                                               boolean orthogonals, boolean diagonals) {
        float rotation = p.getYaw() % 360F;
        if (rotation < 0) rotation += 360F;

        if (_getClimbingOrientationsHashSet == null)
            _getClimbingOrientationsHashSet = new HashSet<>();
        else
            _getClimbingOrientationsHashSet.clear();

        if (orthogonals) {
            NZ.addTo(rotation);
            PZ.addTo(rotation);
            ZN.addTo(rotation);
            ZP.addTo(rotation);
        }
        if (diagonals) {
            NP.addTo(rotation);
            PN.addTo(rotation);
            NN.addTo(rotation);
            PP.addTo(rotation);
        }
        return _getClimbingOrientationsHashSet;
    }

    /**
     * 원본 L293-L297 — `isRotationForClimbing(rotation)` 이면 정적 HashSet 에 추가.
     * `getClimbingOrientations` 전용 보조 메서드.
     */
    private void addTo(float rotation) {
        if (isRotationForClimbing(rotation))
            _getClimbingOrientationsHashSet.add(this);
    }

    // ── getHorizontalBorderGap (원본 L226-L247) ─────────────────────────────

    /**
     * 원본 L226-L247 — 플레이어 (i, k) 위치에서 이 방향 블록 경계까지의 거리.
     *   NZ: i%1 (플레이어 X 좌표 소수부)
     *   PZ: 1 - i%1
     *   ZN: k%1
     *   ZP: 1 - k%1
     *   기타: 0
     *
     * orthogonal 4방향만 의미 있음. diagonal/ZZ 는 0 반환.
     *
     * **Overload (static 위치 기반)**: 엔티티 위치가 아닌 임의 좌표 기반 계산 필요 시
     * 호출자가 직접 i, k 를 추출하여 static 버전 호출. 인스턴스 기반 오버로드 (원본 L231-L234
     * `base_id`/`base_kd` 사용) 는 SmartMovingContext 의존이므로 B-19a1 이후 추가.
     */
    public double getHorizontalBorderGap(double i, double k) {
        if (this == NZ) return i % 1;
        if (this == PZ) return 1 - (i % 1);
        if (this == ZN) return k % 1;
        if (this == ZP) return 1 - (k % 1);
        return 0D;
    }
}
