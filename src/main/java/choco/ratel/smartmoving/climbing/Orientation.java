package choco.ratel.smartmoving.climbing;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

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

    /**
     * 외부 패키지 접근용 X 오프셋 getter.
     * **포커스 #3 B-7 (세션 6)**: SmartMovingClimber 의 L963-L976 BottomHold ladder
     *   2-level 체크 이식 시 `ladderOrientation._i` 접근 필요 → public getter 추가.
     */
    public int getOffsetI() { return _i; }

    /** 외부 패키지 접근용 Z 오프셋 getter (포커스 #3 B-7 세션 6). */
    public int getOffsetK() { return _k; }
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
     * `base_id`/`base_kd` 사용) 는 getHorizontalBorderGap() 로 제공 (B-19a1a).
     */
    public double getHorizontalBorderGap(double i, double k) {
        if (this == NZ) return i % 1;
        if (this == PZ) return 1 - (i % 1);
        if (this == ZN) return k % 1;
        if (this == ZP) return 1 - (k % 1);
        return 0D;
    }

    /**
     * 원본 L231-L234 `getHorizontalBorderGap()` — 인스턴스 필드 `base_id`/`base_kd` 기반.
     * `seekClimbGap` 진입 시 `initialize(world, i, id, jhd, k, kd)` 가 `base_id=id`/`base_kd=kd`
     * 설정한 후 호출되는 패턴.
     */
    public double getHorizontalBorderGap() {
        return getHorizontalBorderGap(base_id, base_kd);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1a (세션 91) — 상태 필드 + 기본 블록 식별/판정 헬퍼
    // 원본: Orientation.java L2729-L2744 static 필드 + L1148-L2625 메서드 대역
    // ════════════════════════════════════════════════════════════════════════

    // ── 인스턴스 상태 필드 (원본 L2729-L2744) ───────────────────────────────
    //
    // 원본은 `Orientation extends SmartMovingContext` 의 static 필드. 1.21.1 에선 쓰레드
    // 안정성 개선 여지 있으나 **1:1 이식 원칙** 유지 — 동일하게 static 보관. 호출자가
    // `initialize(world, i, id, jhd, k, kd)` 로 전역 상태 설정 후 헬퍼 호출하는 패턴.

    /** 원본 L2729 — 현재 처리 중인 World. `initialize` 에서 설정. */
    protected static World world;
    /** 원본 L2731 — 플레이어 기준 Y (base_j). `all_offset = local_offset` 의 절대 Y 기준. */
    protected static int all_j;
    /** 원본 L2731 — 현재 탐색 대상 Y 오프셋 누적. `isLadderSubstitute` 진입 시 0 리셋. */
    protected static int all_offset;
    /** 원본 L2732 — base (플레이어 위치) X. */
    protected static int base_i;
    /** 원본 L2732 — base (플레이어 위치) Z. */
    protected static int base_k;
    /** 원본 L2733 — base X 소수부 포함 (플레이어 정확한 위치). */
    protected static double base_id;
    /** 원본 L2733 — base Z 소수부 포함. */
    protected static double base_kd;
    /** 원본 L2734 — remote (이 Orientation 방향 인접 블록) X. `base_i + _i`. */
    protected static int remote_i;
    /** 원본 L2734 — remote Z. `base_k + _k`. */
    protected static int remote_k;
    /** 원본 L2735 — 플레이어 크롤링 상태 (isSmallClimbing). */
    protected static boolean crawl;
    /** 원본 L2738 — local_offset 과 결합해 실제 Y 좌표 결정 (half block 단위 탐색). */
    protected static int local_half;
    /** 원본 L2739 — `all_j + all_offset` 의 상대 오프셋. 각 `isLadderSubstitute` 호출마다 변동. */
    protected static int local_offset;

    /** 원본 L2741 — 잡기 대상이 remote 블록인지 (base 가 아닌). */
    protected static boolean grabRemote;
    /** 원본 L2742 — 잡기 종류 (NoGrab/HalfGrab/AroundGrab). */
    protected static int grabType;
    /** 원본 L2743 — 잡은 블록 state. (원본은 `Block` + `grabMeta` int 쌍; 1.21.1 은
     *  `BlockState` 단일 객체 표면 매핑 — metadata 는 BlockState property 로 내장.) */
    protected static BlockState grabBlock;
    /** 원본 L2744 — `grabMeta` 원본 Meta 정수. 1.21.1 `grabBlock` 내부 property 로 접근 가능
     *  하므로 `DefaultMeta` (=-1) 기본값 유지. 세부 메타 쿼리는 `grabBlock` property 직접 읽기. */
    protected static int grabMeta = DefaultMeta;

    /**
     * `jh_offset` — 원본 `initializeOffset(double offset, ...)` 에서 설정하는 세로 오프셋
     * (플레이어 boundingBox 상대). `handsClimbing` 의 gap threshold 판정 (`_handClimbingHoldGap`)
     * 에 사용. B-19a2b 이식 완료.
     */
    protected static double jh_offset;

    /** 원본 L2730 `base_jhd` — `initialize` 에서 설정. `initializeOffset` 가 소비 (B-19a2b). */
    protected static double base_jhd;

    /** 원본 `local_halfOffset` — `initializeLocal` 내부 중간값 (B-19a2b). */
    protected static int local_halfOffset;

    /**
     * 원본 L2722-L2724 `_handClimbingHoldGap` — Config 값 기반 static final threshold.
     * `handsClimbing`/`feetClimbing` 에서 `jh_offset` 비교 기준 (B-19a3).
     *
     *   Math.min(0.25F, 0.06F * Math.max(upSpeedFactor, downSpeedFactor))
     *
     * Config 기본값 1.0F/1.0F 에서 = 0.06F.
     */
    private static final float _handClimbingHoldGap = Math.min(0.25F,
            0.06F * Math.max(
                    SmartMovingConfig.Config.freeClimbingUpSpeedFactor,
                    SmartMovingConfig.Config.freeClimbingDownSpeedFactor));

    /**
     * 원본 L2726-L2727 `_climbGapTemp` / `_climbGapOuterTemp` — static ClimbGap 인스턴스.
     * `handsClimbing`/`feetClimbing` (B-19a3) + `seekClimbGap` (B-19a4) 에서 재사용.
     */
    private static final ClimbGap _climbGapTemp = new ClimbGap();
    private static final ClimbGap _climbGapOuterTemp = new ClimbGap();

    // ── 블록 식별 헬퍼 (원본 L1188-L1214) ──────────────────────────────────

    /**
     * 원본 L1188-L1191 — `block == Block.getBlockFromName("ladder")`.
     * 1.21.1 매핑: `instanceof LadderBlock`. vanilla ladder 블록 식별.
     */
    public static boolean isLadder(BlockState state) {
        return state != null && state.getBlock() instanceof LadderBlock;
    }

    /**
     * 원본 L1193-L1196 — vanilla vine 블록 식별.
     */
    public static boolean isVine(BlockState state) {
        return state != null && state.getBlock() instanceof VineBlock;
    }

    /**
     * 원본 L1198-L1202 `isLadderOrVine` — ladder/vine/LadderKit 조합.
     *
     * **§7 근사 이식 (B-19a1a-approx-1)**: 원본 `isBlockIdOfType(block, _ladderKitLadderTypes)`
     * (LadderKit 모드 호환) 미이식 — 해당 모드 1.21.1 에 없음. 순수 vanilla ladder/vine 만 감지.
     */
    public static boolean isLadderOrVine(BlockState state) {
        // 근사 이식 — 원본과 차이: _ladderKitLadderTypes (LadderKit 모드) 제외
        return isLadder(state) || isVine(state);
    }

    /**
     * 원본 L2241-L2244 `isTrapDoor(Block block)` — vanilla trap door 식별.
     * 1.21.1 `instanceof TrapdoorBlock` (oak/iron/copper/bamboo 등 모든 trapdoor 포괄).
     */
    public static boolean isTrapDoor(BlockState state) {
        return state != null && state.getBlock() instanceof TrapdoorBlock;
    }

    /**
     * 원본 `isClosedTrapDoor(int metadata)` — metadata 의 3번째 비트 (0x4) 가 OPEN 상태.
     * 1.21.1 `TrapdoorBlock.OPEN` property.
     */
    public static boolean isClosedTrapDoor(BlockState state) {
        return isTrapDoor(state) && !state.get(TrapdoorBlock.OPEN);
    }

    /**
     * 원본 L1210-L1215 — `block.isLadder(world, i, j, k, player)` Forge hook.
     *
     * **§7 근사 이식 (B-19a1a-approx-2)**: 1.21.1 Fabric 에 동등 훅 없음.
     * `BlockTags.CLIMBABLE` 태그 기반 근사 — vanilla ladder/vine + 모드가 태그에 추가한
     * 블록 감지. 원본 의도 (플레이어 기준 등반 가능 여부) 와 거의 등가.
     */
    public static boolean isClimbable(World world, int i, int j, int k) {
        // 근사 이식 — 원본과 차이: Forge Block.isLadder(world,x,y,z,player) hook → BlockTags.CLIMBABLE
        BlockState state = world.getBlockState(new BlockPos(i, j, k));
        return state.isIn(BlockTags.CLIMBABLE);
    }

    // ── Material / Solid 헬퍼 (원본 L2155-L2175, L2616-L2619) ──────────────

    /**
     * 원본 L2616-L2619 `isSolid(Material material)` — `material.isSolid() && blocksMovement()`.
     *
     * **§7 근사 이식 (B-19a1a-approx-3)**: 1.21.1 (1.19+) 에서 Material API 완전 제거.
     * `state.isSolidBlock(world, pos)` 가 원본 2-조건 AND 와 가장 근접 — Material.isSolid 는
     * "블록 공간을 solid 로 채움", blocksMovement 은 "이동 막음". 1.21.1 isSolidBlock 은
     * "light propagation + collision" 통합 기준. 실용상 유사.
     */
    public static boolean isSolid(BlockState state, World world, BlockPos pos) {
        // 근사 이식 — 원본과 차이: Material API 제거 → state.isSolidBlock() 단일 기준
        return state != null && state.isSolidBlock(world, pos);
    }

    /**
     * 원본 L2155-L2175 `isFullEmpty(Block block)` — 블록이 플레이어 이동 비가로막음.
     *
     * 원본 체크 순서:
     *   (1) null → true
     *   (2) `!isSolid(material)` → empty
     *   (3) 예외: standing_sign / wall_sign / pressure_plate → empty 로 승격
     *   (4) 예외: ASGrapplingHook → empty
     *   (5) 예외: ASRope → non-empty (solid 로 복귀)
     *
     * **§7 근사 이식 (B-19a1a-approx-4)**: (4)(5) `hasASGrapplingHook`/`hasRopesPlus`
     * 모드 호환 미이식 — 해당 모드 1.21.1 에 없음 → 체크 생략.
     * (3) standing sign / wall sign 은 `AbstractSignBlock`/`WallSignBlock` 로 전수 감지.
     * pressure plate 는 `PressurePlateBlock`.
     */
    public static boolean isFullEmpty(BlockState state, World world, BlockPos pos) {
        if (state == null) return true;
        boolean empty = !isSolid(state, world, pos);
        if (!empty) {
            Block blk = state.getBlock();
            if (blk instanceof AbstractSignBlock)     empty = true;  // standing_sign + wall_sign 통합
            else if (blk instanceof WallSignBlock)    empty = true;  // 중복 안전
            else if (blk instanceof PressurePlateBlock) empty = true;
            // 근사 이식 — 원본과 차이: hasASGrapplingHook/hasRopesPlus 모드 체크 생략
        }
        return empty;
    }

    // ── World 접근 헬퍼 (원본 L2621-L2639) ──────────────────────────────────

    /**
     * 원본 L2621-L2624 `getBlock(int i, int j_offset, int k)`.
     * `local_offset + j_offset` Y 좌표의 블록 반환.
     */
    protected static BlockState getBlock(int i, int j_offset, int k) {
        return world.getBlockState(new BlockPos(i, local_offset + j_offset, k));
    }

    /**
     * 원본 L2636-L2639 `getBaseBlockId(int j_offset)`. base 위치 (플레이어 위치) 블록 반환.
     * 1.21.1 매핑: `Block` → `BlockState` (metadata 내장).
     */
    protected static BlockState getBaseBlockId(int j_offset) {
        return world.getBlockState(new BlockPos(base_i, local_offset + j_offset, base_k));
    }

    /**
     * 원본 `getRemoteBlockId(int j_offset)` — remote 위치 (인접 블록) 블록 반환.
     * 1.21.1 매핑: BlockState.
     */
    protected static BlockState getRemoteBlockId(int j_offset) {
        return world.getBlockState(new BlockPos(remote_i, local_offset + j_offset, remote_k));
    }

    // ── 기본 ladder/vine 체크 (원본 L1148-L1186) ────────────────────────────

    /**
     * 원본 L1148-L1158 `isOnLadder(int j_offset)` — base 위치에 ladder 가 있는지.
     *   (1) `isLadder(baseBlock)` → true
     *   (2) `isVine(baseBlock)` → false (vine 은 별도 체크)
     *   (3) `isClimbable(world, base_i, Y, base_k)` → true (Forge ladder hook)
     *   (4) → false
     */
    protected static boolean isOnLadder(int j_offset) {
        BlockState state = getBaseBlockId(j_offset);
        if (isLadder(state)) return true;
        if (isVine(state))   return false;
        if (isClimbable(world, base_i, local_offset + j_offset, base_k)) return true;
        return false;
    }

    /**
     * 원본 L1172-L1175 `isOnVine(int j_offset)` — base 위치에 vine 이 있는지.
     */
    protected static boolean isOnVine(int j_offset) {
        return isVine(getBaseBlockId(j_offset));
    }

    /**
     * 원본 L1182-L1186 `isOnLadderOrVine(int j_offset)`.
     * `isLadderOrVine(baseBlock) || isVine(grabBlock)` — base 의 ladder/vine 또는 이미
     * 잡고 있는 grabBlock 이 vine 인 경우.
     */
    protected static boolean isOnLadderOrVine(int j_offset) {
        return isLadderOrVine(getBaseBlockId(j_offset)) || isVine(grabBlock);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1b (세션 92) — front/back/rope/trapdoor 인스턴스 헬퍼
    // 원본: Orientation.java L1160-L1180 (Behind), L1217-L1238 (Front/Back instance),
    //       L1274-L1359 (orientation 역매핑), L1361-L1384 (Remote/TrapDoor orientation),
    //       L1443-L1485 (Rope), L1513-L1541 (TrapDoor front)
    // ════════════════════════════════════════════════════════════════════════

    // ── Ladder orientation 역매핑 (원본 L1274-L1359) ────────────────────────

    /**
     * 원본 L1325-L1359 `hasLadderOrientation(int i, int j_offset, int k)` — 이 Orientation
     * 에서 (i, j_offset, k) 위치의 ladder 가 **이 방향으로 접근 가능한지** 판정.
     *
     * 원본 매핑 (vanilla 1.7.10 ladder `metadata & 0x7`):
     *   5 → NZ (EAST facing → 서쪽 접근)
     *   4 → PZ (WEST facing → 동쪽 접근)
     *   2 → ZP (NORTH facing → 남쪽 접근)
     *   3 → ZN (SOUTH facing → 북쪽 접근)
     *
     * 1.21.1 매핑: `state.get(LadderBlock.FACING)` Direction 직접 비교. facing 방향의
     * 반대쪽 = 플레이어 탐색 방향.
     *
     * **§7 근사** (B-19a1b-approx-1): 원본 `_ladderKitLadderTypes` 분기 + `carpentersBlockData`
     * 분기 생략 (LadderKit / Carpenters 모드 미이식).
     */
    private boolean hasLadderOrientation(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        if (!isLadder(state)) return false;
        Direction facing = state.get(LadderBlock.FACING);
        if (this == NZ) return facing == Direction.EAST;
        if (this == PZ) return facing == Direction.WEST;
        if (this == ZP) return facing == Direction.NORTH;
        if (this == ZN) return facing == Direction.SOUTH;
        return false;
    }

    /**
     * 원본 L1311-L1323 `hasVineOrientation(World world, int i, int j, int k)` — (i, j, k)
     * 위치의 vine 이 이 Orientation 방향으로 붙어있는지.
     *
     * 원본 매핑 (vanilla 1.7.10 vine `metadata` 비트):
     *   bit 0 (0x1) = SOUTH → ZP
     *   bit 1 (0x2) = WEST  → NZ
     *   bit 2 (0x4) = NORTH → ZN
     *   bit 3 (0x8) = EAST  → PZ
     *
     * 1.21.1 매핑: `VineBlock.NORTH/SOUTH/EAST/WEST` BooleanProperty 직접 조회.
     *
     * **인스턴스 메서드** + `world` 파라미터 (static state 의존 없음) — static context
     * 밖에서도 호출 가능.
     */
    public boolean hasVineOrientation(World world, int i, int j, int k) {
        BlockState state = world.getBlockState(new BlockPos(i, j, k));
        if (!isVine(state)) return false;
        if (this == NZ) return state.get(VineBlock.WEST);
        if (this == PZ) return state.get(VineBlock.EAST);
        if (this == ZP) return state.get(VineBlock.SOUTH);
        if (this == ZN) return state.get(VineBlock.NORTH);
        return false;
    }

    /**
     * 원본 L1274-L1309 `getKnownLadderOrientation(World world, int i, int j, int k)` —
     * (i, j, k) 의 ladder facing 방향을 Orientation 으로 역매핑.
     *
     * 원본 `metadata & 0x7` 매핑은 `hasLadderOrientation` 과 동일. 1.21.1 `LadderBlock.FACING`
     * 직접 사용.
     *
     * **§7 근사** (B-19a1b-approx-1): `_ladderKitLadderTypes` (`metadata & 0x3`) 분기 생략.
     * 순수 vanilla ladder 만 역매핑.
     */
    public static Orientation getKnownLadderOrientation(World world, int i, int j, int k) {
        BlockState state = world.getBlockState(new BlockPos(i, j, k));
        if (!isLadder(state)) return null;
        Direction facing = state.get(LadderBlock.FACING);
        switch (facing) {
            case EAST:  return NZ;
            case WEST:  return PZ;
            case NORTH: return ZP;
            case SOUTH: return ZN;
            default:    return null;
        }
    }

    // ── Ladder/Vine front/back + Behind (원본 L1160-L1180, L1217-L1238) ────

    /** 원본 L1217-L1220 — base 위치 ladder 가 이 방향으로 정면인지. */
    protected boolean isOnLadderFront(int j_offset) {
        return hasLadderOrientation(base_i, j_offset, base_k);
    }

    /**
     * 원본 L1222-L1226 — remote 위치 ladder 가 이 방향의 **반대** 방향 (rotate(180)) 기준
     * 정면인지. 즉 "반대편에서 잡을 수 있는 ladder" 판정.
     */
    protected boolean isOnLadderBack(int j_offset) {
        return rotate(180).hasLadderOrientation(remote_i, j_offset, remote_k);
    }

    /** 원본 L1228-L1232 — base 위치 vine 이 이 방향으로 붙어있는지. */
    protected boolean isOnVineFront(int j_offset) {
        return hasVineOrientation(world, base_i, local_offset + j_offset, base_k);
    }

    /** 원본 L1234-L1238 — remote 위치 vine 이 반대 방향에서 붙어있는지. */
    protected boolean isOnVineBack(int j_offset) {
        return rotate(180).hasVineOrientation(world, remote_i, local_offset + j_offset, remote_k);
    }

    /** 원본 L1160-L1170 — remote 위치에 ladder 가 있는지 (뒤쪽 ladder 탐색용). */
    protected static boolean isBehindLadder(int j_offset) {
        BlockState state = getRemoteBlockId(j_offset);
        if (isLadder(state)) return true;
        if (isVine(state))   return false;
        if (isClimbable(world, remote_i, local_offset + j_offset, remote_k)) return true;
        return false;
    }

    /** 원본 L1177-L1180 — remote 위치에 vine 이 있는지. */
    protected static boolean isBehindVine(int j_offset) {
        return isVine(getRemoteBlockId(j_offset));
    }

    // ── Rope / WallRope — §7 B-19a1b-approx-2 전체 false 근사 ──────────────

    /**
     * 원본 L1443-L1446 `isRope(int j_offset)` — `fcRopeBlock` (BetterThanWolves) /
     * `blockRopeCentral` (RopesPlus) 모드 블록 체크.
     *
     * **§7 근사 이식** (B-19a1b-approx-2): 해당 모드 1.21.1 에 미이식 → 전체 false.
     */
    protected static boolean isRope(int j_offset) {
        // 근사 이식 — 원본과 차이: BetterThanWolves/RopesPlus 모드 블록 미이식
        return false;
    }

    /**
     * 원본 L1470-L1475 `isOnWallRope(int j_offset)` — `blockRope` (ASRope) 벽타기 로프 블록.
     *
     * **§7 근사 이식** (B-19a1b-approx-2): ASRope 모드 1.21.1 에 미이식 → 전체 false.
     */
    protected static boolean isOnWallRope(int j_offset) {
        // 근사 이식 — 원본과 차이: ASRope 모드 블록 미이식
        return false;
    }

    // ── TrapDoor (원본 L1367-L1384, L1513-L1541) ───────────────────────────

    /**
     * 원본 L1513-L1517 `isOnOpenTrapDoor(int j_offset)` — base 위치에 열린 trap door 존재.
     */
    protected static boolean isOnOpenTrapDoor(int j_offset) {
        BlockState state = getBaseBlockId(j_offset);
        return isTrapDoor(state) && !isClosedTrapDoor(state);
    }

    /**
     * 원본 L1519-L1541 `isTrapDoorFront(int trapDoorMetadata)` — trap door facing 이 이
     * Orientation 방향과 매치하는지.
     *
     * 원본 metadata 매핑 (vanilla 1.7.10 `metadata & 3`):
     *   0 = SOUTH, 1 = NORTH, 2 = EAST, 3 = WEST
     *
     * orthogonal: 단일 방향 매치. diagonal: 2개 중 어느 하나 매치.
     *
     * 1.21.1 매핑: `TrapdoorBlock.FACING` Direction 직접 비교. 원본은 int metadata 받으나
     * 1.21.1 은 BlockState 객체 내장이라 BlockState 파라미터로 수정 (표면 매핑).
     */
    private boolean isTrapDoorFront(BlockState state) {
        if (!isTrapDoor(state)) return false;
        Direction facing = state.get(TrapdoorBlock.FACING);
        if (this == NZ) return facing == Direction.WEST;
        if (this == PZ) return facing == Direction.EAST;
        if (this == ZP) return facing == Direction.SOUTH;
        if (this == ZN) return facing == Direction.NORTH;
        if (this == PN) return facing == Direction.EAST || facing == Direction.NORTH;
        if (this == PP) return facing == Direction.EAST || facing == Direction.SOUTH;
        if (this == NN) return facing == Direction.WEST || facing == Direction.NORTH;
        if (this == NP) return facing == Direction.WEST || facing == Direction.SOUTH;
        return false;
    }

    /**
     * 원본 L1366-L1384 `getOpenTrapDoorOrientation(World world, int i, int j, int k)` —
     * 열린 trap door 의 facing 방향을 Orientation 으로 역매핑.
     *
     * 원본 매핑:
     *   0 (SOUTH) → ZP, 1 (NORTH) → ZN, 2 (EAST) → PZ, 3 (WEST) → NZ
     */
    public static Orientation getOpenTrapDoorOrientation(World world, int i, int j, int k) {
        BlockState state = world.getBlockState(new BlockPos(i, j, k));
        if (!isTrapDoor(state) || isClosedTrapDoor(state)) return null;
        Direction facing = state.get(TrapdoorBlock.FACING);
        switch (facing) {
            case SOUTH: return ZP;
            case NORTH: return ZN;
            case EAST:  return PZ;
            case WEST:  return NZ;
            default:    return null;
        }
    }

    /**
     * 원본 L1361-L1364 `isRemoteSolid(World world, int i, int j, int k)` — (i+_i, j, k+_k)
     * 위치 블록이 solid 인지 (이 방향 인접 블록).
     *
     * Material API 제거 근사 (B-19a1a-approx-3 연동 — `isSolidBlock`).
     */
    public boolean isRemoteSolid(World world, int i, int j, int k) {
        BlockPos pos = new BlockPos(i + _i, j, k + _k);
        BlockState state = world.getBlockState(pos);
        return isSolid(state, world, pos);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c1 (세션 93) — 블록 식별 + stair/slab/fence/wall/door 헬퍼
    // 원본: Orientation.java L1544-L1615 (stair metadata), L2026-L2063 (slab),
    //       L2177-L2222 (fence/fenceGate), L2289-L2323 (door/doorFrontBlocked),
    //       L2333-L2340 (wallBlock).
    //
    // 1.21.1 매핑 핵심:
    //   - vanilla 1.7.10 stair `metadata & 3` (0=EAST/1=WEST/2=SOUTH/3=NORTH) ↔
    //     `StairsBlock.FACING` Direction property
    //   - `metadata & 4` (TOP half bit) ↔ `StairsBlock.HALF == BlockHalf.TOP`
    //   - slab `metadata & 8` ↔ `SlabBlock.TYPE == SlabType.TOP/BOTTOM`
    //   - door `metadata == 8` (upper marker) ↔ `DoorBlock.HALF == DoubleBlockHalf.UPPER`
    //   - fenceGate `metadata & 4 == 0` (closed) ↔ `!FenceGateBlock.OPEN`
    // ════════════════════════════════════════════════════════════════════════

    // ── Stair (원본 L1544-L1615) ────────────────────────────────────────────

    /**
     * 원본 L2059-L2063 `isStairCompact(Block)`.
     *
     * **§7 근사** (B-19a1c1-approx-1): 원본 `_knownCompactStairBlocks` (mod 추가 stair
     * 리스트) 체크 생략 — 해당 모드 1.21.1 미이식. vanilla StairsBlock 만 감지.
     */
    public static boolean isStairCompact(BlockState state) {
        // 근사 이식 — 원본과 차이: _knownCompactStairBlocks (mod stair) 제외
        return state != null && state.getBlock() instanceof StairsBlock;
    }

    /**
     * 원본 L1612-L1615 `isTopStairCompact(int stairMetadata)` — `(metadata & 4) != 0`.
     * 1.21.1: `StairsBlock.HALF == BlockHalf.TOP`.
     */
    public static boolean isTopStairCompact(BlockState state) {
        return isStairCompact(state) && state.get(StairsBlock.HALF) == BlockHalf.TOP;
    }

    /**
     * 원본 L1568-L1588 `isStairCompactFront(int stairMetadata)` — `metadata & 3` 를 facing
     * 방향으로 해석하여 이 Orientation 과 매치되는지.
     *
     * 원본 매핑 (`metadata & 3`):
     *   0 = EAST  → PZ
     *   1 = WEST  → NZ
     *   2 = SOUTH → ZP
     *   3 = NORTH → ZN
     *
     * 1.21.1 매핑: `StairsBlock.FACING` Direction 직접 비교.
     * diagonal 은 2 방향 OR.
     */
    public boolean isStairCompactFront(BlockState state) {
        if (!isStairCompact(state)) return false;
        Direction facing = state.get(StairsBlock.FACING);
        if (this == NZ) return facing == Direction.WEST;
        if (this == PZ) return facing == Direction.EAST;
        if (this == ZP) return facing == Direction.SOUTH;
        if (this == ZN) return facing == Direction.NORTH;
        if (this == PN) return facing == Direction.EAST  || facing == Direction.NORTH;
        if (this == PP) return facing == Direction.EAST  || facing == Direction.SOUTH;
        if (this == NN) return facing == Direction.WEST  || facing == Direction.NORTH;
        if (this == NP) return facing == Direction.WEST  || facing == Direction.SOUTH;
        return false;
    }

    /**
     * 원본 L1590-L1610 `isStairCompactBack` — facing 이 탐색 **반대** 방향인 경우.
     */
    public boolean isStairCompactBack(BlockState state) {
        if (!isStairCompact(state)) return false;
        Direction facing = state.get(StairsBlock.FACING);
        if (this == NZ) return facing == Direction.EAST;
        if (this == PZ) return facing == Direction.WEST;
        if (this == ZP) return facing == Direction.NORTH;
        if (this == ZN) return facing == Direction.SOUTH;
        if (this == PN) return facing == Direction.WEST  || facing == Direction.SOUTH;
        if (this == PP) return facing == Direction.WEST  || facing == Direction.NORTH;
        if (this == NN) return facing == Direction.EAST  || facing == Direction.SOUTH;
        if (this == NP) return facing == Direction.EAST  || facing == Direction.NORTH;
        return false;
    }

    /** 원본 L1556-L1560 — `isTopStairCompact && isStairCompactFront`. */
    public boolean isTopStairCompactFront(BlockState state) {
        return isTopStairCompact(state) && isStairCompactFront(state);
    }

    /** 원본 L1562-L1566 — `isTopStairCompact && isStairCompactBack`. */
    public boolean isTopStairCompactBack(BlockState state) {
        return isTopStairCompact(state) && isStairCompactBack(state);
    }

    /** 원본 L1550-L1554 — `!isTopStairCompact && isStairCompactFront` (bottom half + front). */
    public boolean isBottomStairCompactFront(BlockState state) {
        return isStairCompact(state) && !isTopStairCompact(state) && isStairCompactFront(state);
    }

    /** 원본 L1544-L1548 — `!isTopStairCompact && !isStairCompactBack`. */
    public boolean isBottomStairCompactNotBack(BlockState state) {
        return isStairCompact(state) && !isTopStairCompact(state) && !isStairCompactBack(state);
    }

    // ── Slab (원본 L2026-L2056) ────────────────────────────────────────────

    /**
     * 원본 L2053-L2056 `isHalfBlock(Block)` — `BlockSlab && !isOpaqueCube()`.
     * 1.21.1: `instanceof SlabBlock` + `SlabType != DOUBLE` (double slab 은 full block).
     *
     * **§7 근사** (B-19a1c1-approx-2): 원본 `_knownHalfBlocks` (mod slab) 체크 생략.
     */
    public static boolean isHalfBlock(BlockState state) {
        // 근사 이식 — 원본과 차이: _knownHalfBlocks (mod slab) 제외
        if (state == null || !(state.getBlock() instanceof SlabBlock)) return false;
        return state.get(SlabBlock.TYPE) != SlabType.DOUBLE;
    }

    /**
     * 원본 L2038-L2041 `isTopHalfBlock(Block, int metadata)` — `isHalfBlock && (metadata & 8) != 0`.
     * 1.21.1: `SlabType.TOP`.
     */
    public static boolean isTopHalfBlock(BlockState state) {
        return isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.TOP;
    }

    /**
     * 원본 L2026-L2036 `isBottomHalfBlock(Block, int metadata)`.
     *   (1) `isHalfBlock && (metadata & 8) == 0`
     *   (2) `block == bed` (BedBlock)
     *   (3) BetterThanWolves anchor + metadata == 1 (mod) — **§7 근사 생략**
     *
     * **§7 근사** (B-19a1c1-approx-3): BetterThanWolves anchor 체크 생략. vanilla BedBlock
     * 예외는 `instanceof BedBlock` 로 1:1 이식.
     */
    public static boolean isBottomHalfBlock(BlockState state) {
        if (state == null) return false;
        if (isHalfBlock(state) && state.get(SlabBlock.TYPE) == SlabType.BOTTOM) return true;
        // 근사 이식 — 원본과 차이: BetterThanWolves anchor 제외
        return state.getBlock() instanceof net.minecraft.block.BedBlock;
    }

    // ── Fence / FenceGate (원본 L2177-L2222) ────────────────────────────────

    /**
     * 원본 L2177-L2181 `isFenceBase(Block)` — `FenceBlock || WallBlock`.
     */
    public static boolean isFenceBase(BlockState state) {
        return state != null && (state.getBlock() instanceof FenceBlock
                              || state.getBlock() instanceof WallBlock);
    }

    /**
     * 원본 L2208-L2212 `isFenceGate(Block)` — `FenceGateBlock` (+ mod 리스트 근사 제외).
     */
    public static boolean isFenceGate(BlockState state) {
        return state != null && state.getBlock() instanceof FenceGateBlock;
    }

    /** 원본 L2219-L2222 `isClosedFenceGate(int metadata)` — `(metadata & 4) == 0`. */
    public static boolean isClosedFenceGate(BlockState state) {
        return isFenceGate(state) && !state.get(FenceGateBlock.OPEN);
    }

    /** 원본 L2214-L2217 `isOpenFenceGate(Block, int metadata)`. */
    public static boolean isOpenFenceGate(BlockState state) {
        return isFenceGate(state) && state.get(FenceGateBlock.OPEN);
    }

    /** 원본 L2183-L2187 `isFence(Block, int i, int j_offset, int k)` — base + closed gate. */
    public static boolean isFence(BlockState state) {
        return isFenceBase(state) || isClosedFenceGate(state);
    }

    /** 원본 L2189-L2192 `isFence(int i, int j_offset, int k)` — 좌표 기반 래퍼. */
    public static boolean isFence(int i, int j_offset, int k) {
        return isFence(getBlock(i, j_offset, k));
    }

    // ── Wall / Door (원본 L2289-L2340) ─────────────────────────────────────

    /**
     * 원본 L2333-L2340 `isWallBlock(Block, int i, int j_offset, int k)`.
     *   `BlockPane || isFence(block, i, j_offset, k) || (Carpenters && ...)`.
     *
     * **§7 근사** (B-19a1c1-approx-4): Carpenters `_blockCarpentersLadder` 분기 +
     * `_knownThinWallBlocks` (mod pane 리스트) 생략. vanilla PaneBlock + FenceBlock +
     * WallBlock (FenceBase) + closed FenceGate 만.
     */
    public static boolean isWallBlock(BlockState state) {
        // 근사 이식 — 원본과 차이: _knownThinWallBlocks + _blockCarpentersLadder 제외
        if (state == null) return false;
        return state.getBlock() instanceof PaneBlock || isFence(state);
    }

    /**
     * 원본 L2289-L2293 `isDoor(Block)` — vanilla wooden_door / iron_door.
     * 1.21.1: `DoorBlock` 은 oak/spruce/birch/... + iron + copper 등 전부 포괄.
     */
    public static boolean isDoor(BlockState state) {
        return state != null && state.getBlock() instanceof DoorBlock;
    }

    /**
     * 원본 L2295-L2298 `isDoorTop(int metaData)` — `metaData == 8` (upper half 마커).
     * 1.21.1: `DoorBlock.HALF == DoubleBlockHalf.UPPER`.
     */
    public static boolean isDoorTop(BlockState state) {
        return isDoor(state) && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c2 (세션 94) — isEmpty + isBaseAccessible + 좌표 기반 trapdoor 래퍼
    // 원본: Orientation.java L2224-L2238 (trapdoor 좌표 래퍼),
    //       L2342-L2399 (isBaseAccessible 2 오버로드), L2537-L2541 (isEmpty).
    // ════════════════════════════════════════════════════════════════════════

    // ── Trapdoor 좌표 기반 래퍼 (원본 L2224-L2238) ─────────────────────────

    /** 원본 L2236-L2238 `isTrapDoor(int i, int j_offset, int k)` — 좌표 → BlockState 래퍼. */
    protected static boolean isTrapDoor(int i, int j_offset, int k) {
        return isTrapDoor(getBlock(i, j_offset, k));
    }

    /** 원본 L2230-L2233 `isClosedTrapDoor(int i, int j_offset, int k)` — 좌표 래퍼. */
    protected static boolean isClosedTrapDoor(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        return isTrapDoor(state) && isClosedTrapDoor(state);
    }

    /** 원본 L2224-L2227 `isOpenTrapDoor(int i, int j_offset, int k)` — 좌표 래퍼. */
    protected static boolean isOpenTrapDoor(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        return isTrapDoor(state) && !isClosedTrapDoor(state);
    }

    // ── isFullEmpty 좌표 오버로드 (B-19a1a 확장 — static world 활용) ──────

    /**
     * 원본 `isFullEmpty(Block)` 단일 파라미터를 좌표 기반으로 오버로드.
     * 1.21.1 `isFullEmpty(BlockState, World, BlockPos)` 로 전달. 호출부 단순화.
     */
    protected static boolean isFullEmpty(int i, int j_offset, int k) {
        BlockPos pos = new BlockPos(i, local_offset + j_offset, k);
        return isFullEmpty(world.getBlockState(pos), world, pos);
    }

    // ── isEmpty (원본 L2537-L2541) ──────────────────────────────────────────

    /**
     * 원본 L2537-L2541 `isEmpty(int i, int j_offset, int k)`:
     *   `isFullEmpty(getBlock(i, j_offset, k)) && !isFence(i, j_offset - 1, k)`.
     *
     * 플레이어 지나갈 수 있는 공간 + 바로 아래가 fence 가 아님 (fence 는 1.5-높이 → 막힘).
     */
    protected static boolean isEmpty(int i, int j_offset, int k) {
        return isFullEmpty(i, j_offset, k) && !isFence(i, j_offset - 1, k);
    }

    // ── isBaseAccessible 2 오버로드 (원본 L2342-L2399) ──────────────────────

    /**
     * 원본 L2342-L2345 `isBaseAccessible(int j_offset)` — 단순 래퍼, bottom=false/full=false.
     */
    protected static boolean isBaseAccessible(int j_offset) {
        return isBaseAccessible(j_offset, false, false);
    }

    /**
     * 원본 L2347-L2399 `isBaseAccessible(int j_offset, boolean bottom, boolean full)` —
     * base 위치가 플레이어 점유 가능한 공간인지.
     *
     * 7 분기 (OR 누적):
     *   (1) `isEmpty(base_i, j_offset, base_k)` — 빈 공간 + 아래 fence 없음
     *   (2) [§7 근사 생략] RedPower wire 특수 처리
     *   (3) `isFullEmpty(baseBlock)` — 블록 공간 자체 비어있음
     *   (4) `isOpenTrapDoor(base_i, j_offset, base_k)` — 열린 trap door
     *   (5) bottom && `isClosedTrapDoor(base_i, j_offset, base_k)` — bottom 플래그 + 닫힌 trapdoor
     *   (6) !full && `isWallBlock(baseBlock, base_i, j_offset, base_k)` — full=false + 얇은 벽/펜스
     *   (7) [§7 근사 생략] ASRope — !full && mod rope
     *   (8) `isDoor(baseBlock)` — door 블록
     *   (9) [§7 근사 생략] Carpenters `_blockCarpentersLadder`
     *
     * **§7 B-19a1c2 근사 3건**:
     *   - RedPower wire 특수 판정 (`isRedPowerWire`/`getRpCoverSides`/`isRedPowerWireBottom/Top`
     *     체인) 전체 생략 — RedPower mod 1.21.1 미이식
     *   - ASRope 분기 생략 — ASRope mod 1.21.1 미이식 (B-19a1b `isRope`/`isOnWallRope`
     *     false 근사와 연동)
     *   - Carpenters `_blockCarpentersLadder` 분기 생략 — Carpenters mod 1.21.1 미이식
     */
    protected static boolean isBaseAccessible(int j_offset, boolean bottom, boolean full) {
        BlockState id = getBaseBlockId(j_offset);
        boolean accessible = isEmpty(base_i, j_offset, base_k);

        // 근사 이식 — 원본과 차이: RedPower wire 분기 (원본 L2352-L2369) 생략

        if (!accessible && isFullEmpty(getBaseBlockId(j_offset), world,
                new BlockPos(base_i, local_offset + j_offset, base_k)))
            accessible = true;

        if (!accessible && isOpenTrapDoor(base_i, j_offset, base_k))
            accessible = true;

        if (!accessible && bottom && isClosedTrapDoor(base_i, j_offset, base_k))
            accessible = true;

        if (!accessible && !full && isWallBlock(id))
            accessible = true;

        // 근사 이식 — 원본과 차이: ASRope 분기 (원본 L2385-L2389) 생략

        if (!accessible && isDoor(id))
            accessible = true;

        // 근사 이식 — 원본과 차이: Carpenters _blockCarpentersLadder 분기 (원본 L2394-L2396) 생략

        return accessible;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c3a (세션 95) — remoteLadderClimbing + isAccessAccessible + isDoorFrontBlocked
    // 원본: Orientation.java L1115-L1118 (remoteLadderClimbing),
    //       L2477-L2484 (isAccessAccessible), L2300-L2323 (isDoorFrontBlocked).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L1115-L1118 `remoteLadderClimbing(int j_offset)` — remote 위치에 ladder 가 있고
     * 그 ladder 가 **플레이어 반대 방향으로 (back)** 붙어있는지.
     *
     * 의미: 이 Orientation 방향으로 탐색 시, remote 위치에 ladder 가 back-facing 이면
     * "현재 위치에서 등반 중" 으로 간주 (즉 이 방향 블록 통과 차단).
     *
     * 의존: B-19a1b `isBehindLadder` + `isOnLadderBack`.
     */
    protected boolean remoteLadderClimbing(int j_offset) {
        return isBehindLadder(j_offset) && isOnLadderBack(j_offset);
    }

    /**
     * 원본 L2477-L2484 `isAccessAccessible(int j_offset)` — diagonal 방향 접근 시 필요.
     * orthogonal 에서는 항상 true. diagonal 에서는 두 개의 수직 경계 (remote_i/base_k
     * 및 base_i/remote_k) 가 모두 비어있어야 함 — 즉 diagonal 진입 시 양쪽 orthogonal
     * 위치도 통과 가능해야 함.
     */
    protected boolean isAccessAccessible(int j_offset) {
        if (!_isDiagonal) return true;
        return isEmpty(remote_i, j_offset, base_k)
            && isEmpty(base_i, j_offset, remote_k);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c3b (세션 96) — wall-flag 인프라
    // 원본: Orientation.java L1727-L1738 (isFenceGateFront), L1801-L1807 (headedToWall),
    //       L1953-L1999 (getWallFlag), L2001-L2004 (getAllWallsOnNoWall),
    //       L2006-L2009 (isTopHalf).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Orientation → 1.21.1 Direction 매핑 (orthogonal 4방향만). diagonal/ZZ 는 null.
     * 내부 사용 전용. `getWallFlag` / fence gate 등 property 조회 시 필요.
     *
     * 기준:
     *   NZ (-X, 서쪽 탐색) ↔ Direction.WEST
     *   PZ (+X, 동쪽 탐색) ↔ Direction.EAST
     *   ZN (-Z, 북쪽 탐색) ↔ Direction.NORTH
     *   ZP (+Z, 남쪽 탐색) ↔ Direction.SOUTH
     */
    private Direction toBlockDirection() {
        if (this == NZ) return Direction.WEST;
        if (this == PZ) return Direction.EAST;
        if (this == ZN) return Direction.NORTH;
        if (this == ZP) return Direction.SOUTH;
        return null;
    }

    /**
     * 원본 L1727-L1738 `isFenceGateFront(int metaData)` — fence gate 가 이 Orientation
     * 탐색 방향과 정렬되어 있는지.
     *
     * 원본 `metadata % 4`:
     *   0 = SOUTH facing, 1 = WEST, 2 = NORTH, 3 = EAST
     *   NZ/PZ → 0 or 2 (EW 축 gate = SOUTH/NORTH facing) — 서쪽/동쪽에서 통과 가능
     *   ZP/ZN → 1 or 3 (NS 축 gate = WEST/EAST facing) — 남쪽/북쪽에서 통과 가능
     *
     * 1.21.1 매핑: `FenceGateBlock.FACING` Direction. diagonal 방향은 false (원본과 동일).
     */
    private boolean isFenceGateFront(BlockState state) {
        if (!isFenceGate(state)) return false;
        Direction facing = state.get(FenceGateBlock.FACING);
        if (this == NZ || this == PZ)
            return facing == Direction.SOUTH || facing == Direction.NORTH;
        if (this == ZP || this == ZN)
            return facing == Direction.WEST  || facing == Direction.EAST;
        return false;
    }

    /**
     * 원본 L1801-L1807 `headedToWall(Orientation base, boolean result)` — 이 Orientation
     * 이 `base` 또는 ±45° 회전 결과와 같으면 `result` 반환, 아니면 false.
     *
     * 예: base=NZ, this=NZ/NN/NP 중 하나면 result 반환.
     */
    private boolean headedToWall(Orientation base, boolean result) {
        if (this == base || this == base.rotate(45) || this == base.rotate(-45))
            return result;
        return false;
    }

    /**
     * 원본 L2001-L2004 `getAllWallsOnNoWall(Block block)` — `block instanceof BlockPane`.
     *
     * 의미: pane 블록은 주변 연결 대상 없으면 4방향 "wall flag" 를 모두 off 로 반환하는데,
     * 그 경우 호출자가 이를 "전방향 wall 가정" 으로 재해석한다는 표시. `headedToFrontWall`
     * 에서 사용.
     */
    private static boolean getAllWallsOnNoWall(BlockState state) {
        return state != null && state.getBlock() instanceof PaneBlock;
    }

    /**
     * 원본 L2006-L2009 `isTopHalf(double d)` — 좌표 소수부의 2분의 1 격자 top/bottom 판정.
     *
     *   d = 좌표 (i 또는 k). 2배한 절대값 floor 의 odd/even 으로 top 여부 결정.
     *   ex: d = 0.3 → 0.6 → 0 (bottom), d = 0.7 → 1.4 → 1 (top).
     *
     * Pure math — 1:1 이식.
     */
    private static boolean isTopHalf(double d) {
        return (int)Math.abs(Math.floor(d * 2D)) % 2 == 1;
    }

    /**
     * 원본 L1953-L1999 `getWallFlag(Orientation direction, int i, int j_offset, int k,
     * Block block)` — 주어진 위치의 wall/pane/fence/gate 블록이 `direction` 방향으로
     * "연결되어 있는지" 판정.
     *
     * 원본 분기:
     *   (1) BlockPane → `canPaneConnectToBlock(neighbor)` 동적 계산
     *   (2) isFenceBase (Fence/Wall) → `canConnectFenceTo/WallTo(world, x, y, z)` 동적 계산
     *   (3) BetterMisc reflection → mod 근사 생략
     *   (4) isFenceGate → `isClosedFenceGate && isFenceGateFront(metaData)`
     *   (5) Carpenters → mod 근사 생략
     *   (6) default → false
     *
     * **§7 근사** (B-19a1c3b-approx-1): 1.21.1 에서는 Pane/Fence 는 `ConnectingBlock.NORTH/
     * SOUTH/EAST/WEST` BooleanProperty 를 이미 설정 저장. Wall 은 `WallBlock.NORTH_SHAPE`
     * 등 `EnumProperty<WallShape>` — `!= WallShape.NONE` 이면 연결. 원본은 호출 시점
     * 동적 계산, 1.21.1 은 BlockState 캐시 조회 — 대부분의 경우 동치 (neighbor 변경 후
     * 같은 tick 안에서는 약간 차이 가능).
     *
     * BetterMisc reflection (`_canConnectFenceTo`) + Carpenters (`getCarpentersBlockData`)
     * 분기 생략.
     */
    private boolean getWallFlag(Orientation direction, int i, int j_offset, int k, BlockState state) {
        if (state == null) return false;
        Direction d = direction.toBlockDirection();
        if (d == null) return false;

        Block block = state.getBlock();

        // 원본 분기 (1): BlockPane — 1.21.1: ConnectingBlock NORTH/SOUTH/EAST/WEST property
        if (block instanceof PaneBlock) {
            // 근사 이식 — 원본과 차이: canPaneConnectToBlock 동적 → BlockState property 캐시
            return getConnectingFlag(state, d);
        }

        // 원본 분기 (2): isFenceBase = FenceBlock || WallBlock
        if (isFenceBase(state)) {
            if (block instanceof FenceBlock) {
                // 근사 이식 — 원본과 차이: canConnectFenceTo 동적 → ConnectingBlock property 캐시
                return getConnectingFlag(state, d);
            }
            if (block instanceof WallBlock) {
                // 근사 이식 — 원본과 차이: canConnectWallTo 동적 → WallBlock *_SHAPE != NONE
                return getWallShapeFlag(state, d);
            }
            // 근사 이식 — 원본과 차이: BetterMisc `_canConnectFenceTo` reflection 분기 생략
            return false;
        }

        // 원본 분기 (4): FenceGate
        if (isFenceGate(state)) {
            return isClosedFenceGate(state) && isFenceGateFront(state);
        }

        // 원본 분기 (5): Carpenters — 근사 이식 — 원본과 차이: _blockCarpentersLadder 분기 생략
        return false;
    }

    /** `ConnectingBlock.NORTH/SOUTH/EAST/WEST` BooleanProperty 기반 방향 연결 조회. */
    private static boolean getConnectingFlag(BlockState state, Direction d) {
        switch (d) {
            case NORTH: return state.get(ConnectingBlock.NORTH);
            case SOUTH: return state.get(ConnectingBlock.SOUTH);
            case EAST:  return state.get(ConnectingBlock.EAST);
            case WEST:  return state.get(ConnectingBlock.WEST);
            default:    return false;
        }
    }

    /** `WallBlock.NORTH_SHAPE/SOUTH_SHAPE/EAST_SHAPE/WEST_SHAPE` WallShape 기반 연결 조회. */
    private static boolean getWallShapeFlag(BlockState state, Direction d) {
        switch (d) {
            case NORTH: return state.get(WallBlock.NORTH_SHAPE) != WallShape.NONE;
            case SOUTH: return state.get(WallBlock.SOUTH_SHAPE) != WallShape.NONE;
            case EAST:  return state.get(WallBlock.EAST_SHAPE)  != WallShape.NONE;
            case WEST:  return state.get(WallBlock.WEST_SHAPE)  != WallShape.NONE;
            default:    return false;
        }
    }

    /**
     * 원본 L2300-L2323 `isDoorFrontBlocked(int i, int j_offset, int k)` — 이 Orientation
     * 방향 이동 시 door 로 막히는지.
     *
     * 원본 vanilla 1.7.10 door metadata 매핑:
     *   case 8: upper half → lower half 재귀
     *   case 4 (EAST open) / case 1 (SOUTH closed):  return this._k < 0
     *   case 5 (SOUTH open) / case 2 (WEST closed): return this._i > 0
     *   case 6 (WEST open) / case 3 (NORTH closed):  return this._k > 0
     *   case 7 (NORTH open) / case 0 (EAST closed):  return this._i < 0
     *   default: return true (방어적)
     *
     * 1.21.1 매핑: `DoorBlock.FACING` Direction + `DoorBlock.OPEN` Boolean +
     * `DoorBlock.HALF` DoubleBlockHalf 조합으로 각 case 식별.
     */
    private boolean isDoorFrontBlocked(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        if (!isDoor(state)) return true;

        // 원본 case 8 — upper half 이면 lower half 재귀 체크
        if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
            return isDoorFrontBlocked(i, j_offset - 1, k);

        Direction facing = state.get(DoorBlock.FACING);
        boolean open = state.get(DoorBlock.OPEN);

        // 원본 case 4 (EAST open) / case 1 (SOUTH closed): this._k < 0
        if ((facing == Direction.EAST  &&  open) || (facing == Direction.SOUTH && !open))
            return this._k < 0;
        // 원본 case 5 (SOUTH open) / case 2 (WEST closed): this._i > 0
        if ((facing == Direction.SOUTH &&  open) || (facing == Direction.WEST  && !open))
            return this._i > 0;
        // 원본 case 6 (WEST open) / case 3 (NORTH closed): this._k > 0
        if ((facing == Direction.WEST  &&  open) || (facing == Direction.NORTH && !open))
            return this._k > 0;
        // 원본 case 7 (NORTH open) / case 0 (EAST closed): this._i < 0
        if ((facing == Direction.NORTH &&  open) || (facing == Direction.EAST  && !open))
            return this._i < 0;

        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c3c (세션 97) — headedToFrontWall + headedToRemoteFlatWall + isRemoteAccessible
    // 원본: Orientation.java L1741-L1757 (headedToFrontWall),
    //       L1941-L1951 (headedToRemoteFlatWall), L2401-L2475 (isRemoteAccessible).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L1741-L1757 `headedToFrontWall(int i, int j_offset, int k, Block block)` —
     * (i, j_offset, k) 위치 벽/펜스 블록이 4방향 중 어느 방향으로 연결되어 있고, 이 플레이어
     * Orientation 이 그 방향 반대쪽 (front 에서 접근) 이면 true.
     *
     * 논리:
     *   1. 4 방향 wall flag 수집 (`zn`/`zp`/`nz`/`pz` — 블록이 해당 방향으로 연결)
     *   2. `allOnNone` 블록 (pane) 이고 전부 false 면 4 방향 전부 true 처리 (고립 pane
     *      은 4 방향 연결 가정)
     *   3. 이 Orientation 이 NZ/PZ/ZN/ZP (및 ±45°) 중 어디면, 그 반대 방향 wall flag 반환
     *      - NZ 탐색 시 PZ(동쪽) 연결 확인
     *      - PZ 탐색 시 NZ(서쪽) 연결 확인
     *      - ZN 탐색 시 ZP(남쪽) 연결 확인
     *      - ZP 탐색 시 ZN(북쪽) 연결 확인
     */
    private boolean headedToFrontWall(int i, int j_offset, int k, BlockState state) {
        boolean zn = getWallFlag(ZN, i, j_offset, k, state);
        boolean zp = getWallFlag(ZP, i, j_offset, k, state);
        boolean nz = getWallFlag(NZ, i, j_offset, k, state);
        boolean pz = getWallFlag(PZ, i, j_offset, k, state);
        boolean allOnNone = getAllWallsOnNoWall(state);

        if (allOnNone && !zn && !zp && !nz && !pz)
            zn = zp = nz = pz = true;

        return headedToWall(NZ, pz)
            || headedToWall(PZ, nz)
            || headedToWall(ZN, zp)
            || headedToWall(ZP, zn);
    }

    /**
     * 원본 L1941-L1951 `headedToRemoteFlatWall(Block block, int j_offset)` — remote 위치
     * 블록이 "평평한 벽 (flat wall)" 패턴 — 플레이어 진행 방향과 직각으로 연결되고 다른
     * 방향으로는 연결 안 된 모양 — 인지.
     *
     *   !this-dir && +rotate(90) && !rotate(180) && +rotate(-90)
     *
     * 즉, 플레이어 탐색 방향 (this) 으로도, 반대 방향 (rotate 180) 으로도 연결 안 되고,
     * 양 옆 (rotate ±90) 으로만 연결됨.
     */
    private boolean headedToRemoteFlatWall(BlockState state, int j_offset) {
        return !getWallFlag(this,            remote_i, j_offset, remote_k, state)
            &&  getWallFlag(this.rotate(90), remote_i, j_offset, remote_k, state)
            && !getWallFlag(this.rotate(180),remote_i, j_offset, remote_k, state)
            &&  getWallFlag(this.rotate(-90),remote_i, j_offset, remote_k, state);
    }

    /**
     * 원본 L2401-L2475 `isRemoteAccessible(int j_offset)` — remote (이 Orientation 방향 인접)
     * 위치가 플레이어 점유 가능한지. 여러 분기의 OR 누적.
     *
     * 분기 그룹:
     *   (1) `isEmpty(remote_i, j_offset, remote_k)` — 빈 공간
     *   (2) [§7 근사 생략] RedPower wire 특수 판정
     *   (3) accessible 상태에서의 역체크 (base 위치의 trap door/door/ladder 가 막는지)
     *       - base `isTrapDoor && isTrapDoorFront` → 막힘
     *       - base `isDoor && isDoorFrontBlocked` → 막힘
     *       - `remoteLadderClimbing` → 막힘
     *   (4) remote trap door + closed → 접근 가능
     *   (5) !accessible 상태에서의 wall/fence/door/rope 복합 분기
     *       - remote wall block + !headedToFrontWall + !fence below
     *       - remote 아래 fence + !headedToFrontWall (+ base 아래 wall block) + cobblestone
     *         예외
     *       - remote door + !rotate(180).isDoorFrontBlocked
     *       - [§7 근사 생략] ASRope + !rotate(180).isASGrapplingHookFront
     *
     * **§7 근사 2건**:
     *   - RedPower 분기 생략 (B-19a1c2-approx-1 과 동일 패턴)
     *   - ASRope 분기 생략 (B-19a1b `isRope`/`isOnWallRope` false 와 연동)
     */
    protected boolean isRemoteAccessible(int j_offset) {
        boolean accessible = isEmpty(remote_i, j_offset, remote_k);

        // 근사 이식 — 원본과 차이: RedPower wire 분기 (원본 L2404-L2422) 생략

        if (accessible) {
            BlockState baseState = getBaseBlockId(j_offset);
            if (isTrapDoor(baseState))
                accessible = !isTrapDoorFront(baseState);

            if (accessible && isDoor(baseState))
                accessible = !isDoorFrontBlocked(base_i, j_offset, base_k);

            if (remoteLadderClimbing(j_offset))
                accessible = false;
        }

        if (!accessible && isTrapDoor(remote_i, j_offset, remote_k))
            accessible = isClosedTrapDoor(remote_i, j_offset, remote_k);

        if (!accessible) {
            BlockState remoteState = getRemoteBlockId(j_offset);
            if (isWallBlock(remoteState)
                    && !headedToFrontWall(remote_i, j_offset, remote_k, remoteState)
                    && !isFence(remote_i, j_offset - 1, remote_k))
                accessible = true;

            BlockState belowState = getRemoteBlockId(j_offset - 1);
            if (!accessible && isFence(belowState)
                    && (!headedToFrontWall(remote_i, j_offset - 1, remote_k, belowState)
                        || isWallBlock(getBaseBlockId(j_offset - 1))))
                if (belowState.getBlock() != Blocks.COBBLESTONE_WALL
                        || headedToRemoteFlatWall(belowState, -1))
                    accessible = true;

            if (!accessible && isDoor(remoteState)
                    && !rotate(180).isDoorFrontBlocked(remote_i, j_offset, remote_k))
                accessible = true;

            // 근사 이식 — 원본과 차이: ASRope 분기 (원본 L2467-L2471) 생략
        }

        return accessible;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a1c4 (세션 98) — isFullAccessible + isFullExtentAccessible +
    //                      isJustLowerHalfExtentAccessible + isUpperHalfFrontEmpty
    // 원본: Orientation.java L2486-L2535 + L2325-L2331 + L2543-L2586.
    // **B-19a1c (accessibility 판정) 최종 서브** — 완료 시 B-19a1 완료.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L2325-L2331 `getWallBlockId(int i, int j_offset, int k)` — 좌표 위치 블록이
     * wall block 이면 BlockState 반환, 아니면 null.
     */
    private static BlockState getWallBlockId(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        return isWallBlock(state) ? state : null;
    }

    /**
     * 원본 L2528-L2535 `isFullAccessible(int j_offset, boolean grabRemote)`.
     *
     * grabRemote=true  → `isBaseAccessible && isRemoteAccessible && isAccessAccessible`
     * grabRemote=false → `isEmpty(base_i, j_offset, base_k)` — 단순 base 빈 공간 체크
     *
     * 의존 전수 이식: B-19a1c2 isBaseAccessible/isEmpty + B-19a1c3c isRemoteAccessible +
     * B-19a1c3a isAccessAccessible.
     */
    protected boolean isFullAccessible(int j_offset, boolean grabRemote) {
        if (grabRemote)
            return isBaseAccessible(j_offset)
                && isRemoteAccessible(j_offset)
                && isAccessAccessible(j_offset);
        return isEmpty(base_i, j_offset, base_k);
    }

    /**
     * 원본 L2486-L2512 `isFullExtentAccessible(int j_offset, boolean grabRemote)`.
     *
     * 원본: `isFullAccessible` 반환값에 RedPower 추가 체크 (AND 조건).
     *
     * **§7 근사** (B-19a1c4-approx-1): RedPower wire 분기 (원본 L2490-L2510) 전체 생략 —
     * RedPower mod 1.21.1 미이식. 결과: `isFullAccessible` 그대로 반환.
     */
    protected boolean isFullExtentAccessible(int j_offset, boolean grabRemote) {
        boolean accessible = isFullAccessible(j_offset, grabRemote);
        // 근사 이식 — 원본과 차이: RedPower wire 분기 (원본 L2490-L2510) 생략
        return accessible;
    }

    /**
     * 원본 L2514-L2526 `isJustLowerHalfExtentAccessible(int j_offset)` — remote 위치가
     * top half (slab top / stair top front) 이면 lower half 공간이 비어있음 → 접근 가능.
     *
     * 의존: B-19a1c1 `isTopHalfBlock` / `isStairCompact` / `isTopStairCompactFront`.
     * 근사 없음.
     */
    protected boolean isJustLowerHalfExtentAccessible(int j_offset) {
        BlockState remoteState = getRemoteBlockId(j_offset);
        boolean accessible = false;
        if (!accessible)
            accessible = isTopHalfBlock(remoteState);
        if (!accessible)
            accessible = isStairCompact(remoteState) && isTopStairCompactFront(remoteState);
        return accessible;
    }

    /**
     * 원본 L2543-L2586 `isUpperHalfFrontEmpty(int i, int j_offset, int k)` — 해당 위치의
     * upper half (Y+0.5~+1.0) 가 이 Orientation 방향 전방에서 비어있는지.
     *
     * 원본 분기 순서 (empty OR 누적):
     *   (1) `isFullEmpty(block)` — 블록 전체 빈 공간
     *   (2) `isBottomHalfBlock(block, meta)` — bottom slab (upper half 비어있음)
     *   (3) `isStairCompact && isBottomStairCompactFront` — bottom stair front
     *   (4) [§7 근사 생략] RedPower wire 특수 판정
     *   (5) `isTrapDoor(block)` — trap door (open 여부 무관 upper half 비어있음)
     *   (6) wallBlock + (!headedToFrontWall || 반대쪽도 wallBlock) — wall 패턴 관통 가능
     *   (7) [§7 근사 생략] LadderKit + `rotate(180).hasLadderOrientation` → empty=false 복귀
     *
     * **§7 근사** (B-19a1c4-approx-2 / approx-3): RedPower + LadderKit 분기 생략.
     */
    protected boolean isUpperHalfFrontEmpty(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        BlockPos pos = new BlockPos(i, local_offset + j_offset, k);
        boolean empty = isFullEmpty(state, world, pos);

        if (!empty) {
            if (isBottomHalfBlock(state))
                empty = true;

            if (!empty && isStairCompact(state) && isBottomStairCompactFront(state))
                empty = true;
        }

        // 근사 이식 — 원본과 차이: RedPower wire 분기 (원본 L2559-L2567) 생략

        if (!empty && isTrapDoor(state))
            empty = true;

        if (!empty) {
            BlockState wallState = getWallBlockId(i, j_offset, k);
            if (wallState != null
                    && (!headedToFrontWall(i, j_offset, k, wallState)
                        || isWallBlock(getBlock(i - _i, j_offset, k - _k))))
                empty = true;
        }

        // 근사 이식 — 원본과 차이: LadderKit 분기 (원본 L2581-L2583) 생략 —
        // `isBlockIdOfType(block, _ladderKitLadderTypes) && rotate(180).hasLadderOrientation(...)`
        // 이 true 면 empty=false 복귀. vanilla ladder 는 `isLadderOrVine` 에 포함되어
        // `isFullEmpty` 에서 이미 non-empty 처리됨 — 기본 vanilla 동작은 보존됨.

        return empty;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2a1 (세션 99) — wall 판정 보조 (headedToFrontSideWall + headedToBaseWall
    //                       3 오버로드 + headedToBaseGrabWall 2 오버로드)
    // 원본: Orientation.java L1759-L1798 (headedToFrontSideWall),
    //       L1809-L1862 (headedToBaseWall 3), L1865-L1938 (headedToBaseGrabWall 2).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L1759-L1798 `headedToFrontSideWall(int i, int j_offset, int k, Block block)` —
     * wall/pane/fence 블록이 이 Orientation 의 **측면** 방향 (base_id/base_kd 의 half 위치
     * 조합에 따른 오프셋 방향) 으로 연결되어 있는지.
     *
     * 논리: 4방향 wall flag 수집 → `base_id`/`base_kd` 의 topHalf 조합 4가지 중 하나
     * 선택 → 4방향 `headedToWall(NZ/PZ/ZN/ZP, ...)` OR. 원본과 동일.
     */
    private boolean headedToFrontSideWall(int i, int j_offset, int k, BlockState state) {
        boolean zn = getWallFlag(ZN, i, j_offset, k, state);
        boolean zp = getWallFlag(ZP, i, j_offset, k, state);
        boolean nz = getWallFlag(NZ, i, j_offset, k, state);
        boolean pz = getWallFlag(PZ, i, j_offset, k, state);
        boolean allOnNone = getAllWallsOnNoWall(state);

        if (allOnNone && !zn && !zp && !nz && !pz)
            zn = zp = nz = pz = true;

        boolean iTop = isTopHalf(base_id);
        boolean kTop = isTopHalf(base_kd);
        if (iTop) {
            if (kTop) {
                return headedToWall(NZ, zp) || headedToWall(PZ, zp)
                    || headedToWall(ZN, pz) || headedToWall(ZP, pz);
            } else {
                return headedToWall(NZ, zn) || headedToWall(PZ, zn)
                    || headedToWall(ZN, pz) || headedToWall(ZP, pz);
            }
        } else {
            if (kTop) {
                return headedToWall(NZ, zp) || headedToWall(PZ, zp)
                    || headedToWall(ZN, nz) || headedToWall(ZP, nz);
            } else {
                return headedToWall(NZ, zn) || headedToWall(PZ, zn)
                    || headedToWall(ZN, nz) || headedToWall(ZP, nz);
            }
        }
    }

    /**
     * 원본 L1809-L1839 `headedToBaseWall(int j_offset, Block block)` — base 위치 wall/pane
     * 블록이 이 Orientation 의 대각/직교 패턴에 맞게 연결되어 있는지.
     *
     * base_id/base_kd 의 topHalf 2x2 조합에 따라 4 diagonal 중 하나 선택, 그 diagonal 과
     * 인접 2 orthogonal 조합을 `headedToBaseWall(diagonal, left, right, ...)` 로 전달.
     */
    private boolean headedToBaseWall(int j_offset, BlockState state) {
        boolean zn = getWallFlag(ZN, base_i, j_offset, base_k, state);
        boolean zp = getWallFlag(ZP, base_i, j_offset, base_k, state);
        boolean nz = getWallFlag(NZ, base_i, j_offset, base_k, state);
        boolean pz = getWallFlag(PZ, base_i, j_offset, base_k, state);
        boolean allOnNone = getAllWallsOnNoWall(state);

        if (allOnNone && !zn && !zp && !nz && !pz)
            zn = zp = nz = pz = true;

        boolean leaf = zn || zp || nz || pz;
        boolean coreOnly = !allOnNone && !leaf;

        boolean iTop = isTopHalf(base_id);
        boolean kTop = isTopHalf(base_kd);
        if (iTop) {
            if (kTop)  return headedToBaseWall(NN, NZ, ZN, zp, nz, pz, zn, coreOnly, leaf);
            else       return headedToBaseWall(NP, NZ, ZP, zn, nz, pz, zp, coreOnly, leaf);
        } else {
            if (kTop)  return headedToBaseWall(PN, PZ, ZN, zp, pz, nz, zn, coreOnly, leaf);
            else       return headedToBaseWall(PP, PZ, ZP, zn, pz, nz, zp, coreOnly, leaf);
        }
    }

    /**
     * 원본 L1841-L1855 `headedToBaseWall(Orientation diagonal, Orientation left, Orientation
     * right, ...)` — 이 Orientation 이 diagonal/left/right 중 어느 하나와 매치하는지로
     * 결과 분기.
     *
     * diagonal 이면 `leaf || coreOnly`.
     * left 이면 `headedToBaseWall(leftFront, rightFrontOpposite, rightFront,
     *                              leftFrontOpposite, coreOnly)`.
     * right 이면 `headedToBaseWall(rightFront, leftFrontOpposite, leftFront,
     *                               rightFrontOpposite, coreOnly)`.
     */
    private boolean headedToBaseWall(Orientation diagonal, Orientation left, Orientation right,
                                     boolean leftFront, boolean rightFrontOpposite,
                                     boolean rightFront, boolean leftFrontOpposite,
                                     boolean co, boolean leaf) {
        if (this == diagonal) return leaf || co;
        if (this == left)
            return headedToBaseWall(leftFront, rightFrontOpposite, rightFront,
                    leftFrontOpposite, co);
        if (this == right)
            return headedToBaseWall(rightFront, leftFrontOpposite, leftFront,
                    rightFrontOpposite, co);
        return false;
    }

    /**
     * 원본 L1857-L1863 `headedToBaseWall(boolean front, boolean sideOpposite, boolean side,
     * boolean frontOpposite, boolean coreOnly)` — 5 개 flag 의 OR 조합.
     *
     *   front || (sideOpposite && !side) || (frontOpposite && !front && !side) || coreOnly
     */
    private static boolean headedToBaseWall(boolean front, boolean sideOpposite, boolean side,
                                            boolean frontOpposite, boolean coreOnly) {
        return front
            || (sideOpposite && !side)
            || (frontOpposite && !front && !side)
            || coreOnly;
    }

    /**
     * 원본 L1865-L1915 `headedToBaseGrabWall(int j_offset, Block block)` — grab 방향 wall
     * 패턴 판정. base + above (j_offset+1) 위치의 wall flag 를 조합하여 결정.
     *
     * 논리:
     *   1. base 위치 4방향 wall flag 수집
     *   2. above 블록이 fullEmpty → 4방향 above 전부 false
     *      isWallBlock → 4방향 above flag 수집
     *      기타 → 4방향 above 전부 true
     *   3. base_id/base_kd topHalf 2x2 조합으로 `headedToBaseGrabWall(i, k, ...)` 호출
     *      (front/side/frontOpposite/sideOpposite 및 above 4개 파라미터 전달)
     */
    private boolean headedToBaseGrabWall(int j_offset, BlockState state) {
        boolean zn = getWallFlag(ZN, base_i, j_offset, base_k, state);
        boolean zp = getWallFlag(ZP, base_i, j_offset, base_k, state);
        boolean nz = getWallFlag(NZ, base_i, j_offset, base_k, state);
        boolean pz = getWallFlag(PZ, base_i, j_offset, base_k, state);
        boolean allOnNone = getAllWallsOnNoWall(state);

        if (allOnNone && !zn && !zp && !nz && !pz)
            zn = zp = nz = pz = true;

        boolean azn, azp, anz, apz;
        BlockState aboveState = getBlock(base_i, j_offset + 1, base_k);
        BlockPos abovePos = new BlockPos(base_i, local_offset + j_offset + 1, base_k);
        if (isFullEmpty(aboveState, world, abovePos)) {
            azn = azp = anz = apz = false;
        } else if (isWallBlock(aboveState)) {
            azn = getWallFlag(ZN, base_i, j_offset + 1, base_k, aboveState);
            azp = getWallFlag(ZP, base_i, j_offset + 1, base_k, aboveState);
            anz = getWallFlag(NZ, base_i, j_offset + 1, base_k, aboveState);
            apz = getWallFlag(PZ, base_i, j_offset + 1, base_k, aboveState);
            boolean aboveAllOnNone = getAllWallsOnNoWall(aboveState);

            if (aboveAllOnNone && !azn && !azp && !anz && !apz)
                azn = azp = anz = apz = true;
        } else {
            azn = azp = anz = apz = true;
        }

        boolean iTop = isTopHalf(base_id);
        boolean kTop = isTopHalf(base_kd);
        if (iTop) {
            if (kTop) return headedToBaseGrabWall(-this._i, -this._k, zp, pz, nz, zn, azp, apz, anz, azn);
            else      return headedToBaseGrabWall(-this._i,  this._k, pz, zn, zp, nz, apz, azn, azp, anz);
        } else {
            if (kTop) return headedToBaseGrabWall( this._i, -this._k, nz, zp, zn, pz, anz, azp, azn, apz);
            else      return headedToBaseGrabWall( this._i,  this._k, zn, nz, pz, zp, azn, anz, apz, azp);
        }
    }

    /**
     * 원본 L1917-L1938 `headedToBaseGrabWall(int i, int k, ...)` — 10 boolean flag 조합으로
     * grab wall 판정. i/k 는 플레이어 방향 부호 (± 1 / 0).
     *
     * 5 분기 OR:
     *   (1) sideOpposite && !aboveSideOpposite && !front && !aboveFront && i == 1
     *   (2) frontOpposite && !aboveFrontOpposite && !side && !aboveSide && k == 1
     *   (3) side && !aboveSide && k >= 0
     *   (4) front && !aboveFront && k >= 0
     *   (5) frontOpposite && !aboveFrontOpposite && !aboveFront && i == 1 && k >= 0
     *   (6) sideOpposite && !aboveSideOpposite && !aboveSide && k == 1 && i >= 0
     */
    private static boolean headedToBaseGrabWall(int i, int k,
                                                boolean front, boolean side,
                                                boolean frontOpposite, boolean sideOpposite,
                                                boolean aboveFront, boolean aboveSide,
                                                boolean aboveFrontOpposite, boolean aboveSideOpposite) {
        if (sideOpposite && !aboveSideOpposite && !front && !aboveFront && i == 1)
            return true;
        if (frontOpposite && !aboveFrontOpposite && !side && !aboveSide && k == 1)
            return true;
        if (side && !aboveSide && k >= 0)
            return true;
        if (front && !aboveFront && k >= 0)
            return true;
        if (frontOpposite && !aboveFrontOpposite && !aboveFront && i == 1 && k >= 0)
            return true;
        if (sideOpposite && !aboveSideOpposite && !aboveSide && k == 1 && i >= 0)
            return true;
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2a2 (세션 100) — vine 보조 (baseVineClimbing + remoteVineClimbing 각 2 오버로드)
    // 원본: Orientation.java L1087-L1103 + L1105-L1113 (baseVineClimbing),
    //       L1120-L1132 + L1134-L1146 (remoteVineClimbing).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L1087-L1103 `baseVineClimbing(int j_offset)` — base 위치에 vine 이 있을 때,
     * 이 Orientation 방향으로 등반 가능한 vine meta 타입 반환.
     *
     *   (1) `isOnVine(j_offset)` false → `DefaultMeta` (-1)
     *   (2) `isOnVineFront(j_offset)` (vine 이 이 방향으로 붙어있음) → `VineFrontMeta` (0)
     *   (3) 4 orthogonal (PZ/NZ/ZP/ZN) 중 어느 하나가 `baseVineClimbing(j_offset, orientation)` true →
     *       `VineSideMeta` (1)
     *   (4) 외 → `DefaultMeta`
     */
    protected int baseVineClimbing(int j_offset) {
        boolean result = isOnVine(j_offset);
        if (result) {
            result = isOnVineFront(j_offset);
            if (result)
                return VineFrontMeta;

            if (baseVineClimbing(j_offset, PZ)
                    || baseVineClimbing(j_offset, NZ)
                    || baseVineClimbing(j_offset, ZP)
                    || baseVineClimbing(j_offset, ZN))
                return VineSideMeta;
        }
        return DefaultMeta;
    }

    /**
     * 원본 L1105-L1113 `baseVineClimbing(int j_offset, Orientation orientation)` — 지정한
     * orientation 기준 vine 측면 등반 가능 판정.
     *
     *   (1) `orientation == this` → false (자기 자신은 front 판정에서 처리됨)
     *   (2) `orientation.rotate(180).hasVineOrientation(world, base_i, local_offset + j_offset, base_k)` AND
     *       `orientation.getHorizontalBorderGap() >= 0.65` → true
     *
     * 의미: vine 이 orientation 의 반대쪽에 붙어있고 (rotate(180)), 플레이어가 orientation
     * 방향 경계에 충분히 가까움 (0.65 이상) → 그 orientation 으로 등반 가능.
     */
    protected boolean baseVineClimbing(int j_offset, Orientation orientation) {
        if (orientation == this) return false;

        return orientation.rotate(180).hasVineOrientation(world, base_i,
                local_offset + j_offset, base_k)
            && orientation.getHorizontalBorderGap() >= 0.65;
    }

    /**
     * 원본 L1120-L1132 `remoteVineClimbing(int j_offset)` — remote 위치 vine 등반 meta.
     *
     *   (1) `isBehindVine && isOnVineBack` → `VineFrontMeta`
     *   (2) 4 orthogonal 중 어느 하나 `remoteVineClimbing(j_offset, orientation)` true →
     *       `VineSideMeta`
     *   (3) 외 → `DefaultMeta`
     */
    protected int remoteVineClimbing(int j_offset) {
        if (isBehindVine(j_offset) && isOnVineBack(j_offset))
            return VineFrontMeta;

        if (remoteVineClimbing(j_offset, PZ)
                || remoteVineClimbing(j_offset, NZ)
                || remoteVineClimbing(j_offset, ZP)
                || remoteVineClimbing(j_offset, ZN))
            return VineSideMeta;

        return DefaultMeta;
    }

    /**
     * 원본 L1134-L1146 `remoteVineClimbing(int j_offset, Orientation orientation)`.
     *
     *   (1) `orientation == this` → false
     *   (2) `(base_i - orientation._i, j_offset, base_k - orientation._k)` 좌표에서
     *       vine 블록 + `orientation.hasVineOrientation` + `getHorizontalBorderGap >= 0.65F`
     *
     * 의미: orientation 의 반대쪽 한 칸 위치에 vine 이 그 orientation 방향으로 붙어있고,
     * 경계 거리 조건 만족.
     */
    protected boolean remoteVineClimbing(int j_offset, Orientation orientation) {
        if (orientation == this) return false;

        int i = base_i - orientation._i;
        int k = base_k - orientation._k;
        BlockState state = getBlock(i, j_offset, k);
        return isVine(state)
            && orientation.hasVineOrientation(world, i, local_offset + j_offset, k)
            && orientation.getHorizontalBorderGap() >= 0.65F;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2a3 (세션 101) — half-solid 판정 (isLowerHalfFrontFullEmpty +
    //                       isUpperHalfFrontAnySolid + isUpperHalfFrontFullSolid)
    // 원본: Orientation.java L2065-L2115 + L2117-L2125 + L2127-L2153.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L2065-L2115 `isLowerHalfFrontFullEmpty(int i, int j_offset, int k)` — 해당 위치의
     * lower half (Y~Y+0.5) 가 이 Orientation 방향 전방에서 비어있는지.
     *
     * 원본 분기 순서 (empty OR 누적, LadderKit 만 empty=false 복귀):
     *   (1) `isFullEmpty(block)` — 블록 전체 빈 공간
     *   (2) [§7 근사 생략] RedPower wire
     *   (3) [§7 근사 생략] BetterThanWolves anchor (metadata==0)
     *   (4) `isStairCompact && isTopStairCompactFront` — top stair front (아래 빔)
     *   (5) `isHalfBlock && SlabType == TOP` (원본 `isHalfBlockTopMetaData`)
     *   (6) wallBlock + !headedToFrontWall — wall 통과 가능
     *   (7) door + !rotate(180).isDoorFrontBlocked — door 열림
     *   (8) [§7 근사 생략] ASRope + !rotate(180).isASGrapplingHookFront
     *   (9) [§7 근사 생략] LadderKit + rotate(180).hasLadderOrientation → empty=false 복귀
     *
     * **§7 근사** (B-19a2a3-approx-1): RedPower + BetterThanWolves + ASRope + LadderKit 4
     * mod 분기 생략. vanilla ladder 는 `isFullEmpty` 에서 non-empty 처리됨.
     */
    protected boolean isLowerHalfFrontFullEmpty(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        boolean empty = isFullEmpty(i, j_offset, k);

        // 근사 이식 — 원본과 차이: RedPower wire 분기 (원본 L2070-L2078) 생략
        // 근사 이식 — 원본과 차이: BetterThanWolves anchor 분기 (원본 L2080-L2086) 생략

        if (!empty && isStairCompact(state) && isTopStairCompactFront(state))
            empty = true;

        if (!empty && isTopHalfBlock(state))
            empty = true;

        if (!empty && isWallBlock(state)
                && !headedToFrontWall(i, j_offset, k, state))
            empty = true;

        if (!empty && isDoor(state)
                && !rotate(180).isDoorFrontBlocked(i, j_offset, k))
            empty = true;

        // 근사 이식 — 원본과 차이: ASRope 분기 (원본 L2104-L2108) 생략
        // 근사 이식 — 원본과 차이: LadderKit 분기 (원본 L2110-L2112) 생략 —
        // vanilla ladder 는 `isLadderOrVine` 에 포함되어 `isFullEmpty` 에서 non-empty 처리됨.

        return empty;
    }

    /**
     * 원본 L2117-L2125 `isUpperHalfFrontAnySolid(int i, int j_offset, int k)` —
     * upper half 가 solid 이나, wall block 이고 이 Orientation 방향으로 연결 안 된 경우
     * (headedToFrontWall false) solid=false 로 감쇠.
     */
    protected boolean isUpperHalfFrontAnySolid(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        boolean solid = isUpperHalfFrontFullSolid(i, j_offset, k);
        if (solid && isWallBlock(state)
                && !headedToFrontWall(i, j_offset, k, state))
            solid = false;
        return solid;
    }

    /**
     * 원본 L2127-L2153 `isUpperHalfFrontFullSolid(int i, int j_offset, int k)` —
     * `isSolid(material)` 기본 + 얇은/관통 블록 예외.
     *
     *   (1) null → false
     *   (2) `isSolid` 기본
     *   (3) standing_sign / wall_sign / pressurePlate / trapDoor → solid=false
     *   (4) [§7 근사 생략] ASGrapplingHook → solid=false
     *   (5) openFenceGate → solid=false
     *   (6) [§7 근사 생략] Carpenters `_blockCarpentersLadder` → solid=false
     *
     * **§7 근사** (B-19a2a3-approx-2): ASGrapplingHook / Carpenters mod 1.21.1 미이식 생략.
     */
    protected static boolean isUpperHalfFrontFullSolid(int i, int j_offset, int k) {
        BlockState state = getBlock(i, j_offset, k);
        if (state == null) return false;

        BlockPos pos = new BlockPos(i, local_offset + j_offset, k);
        boolean solid = isSolid(state, world, pos);

        if (solid) {
            Block block = state.getBlock();
            if (block instanceof AbstractSignBlock) solid = false;   // standing_sign + wall_sign 통합
            else if (block instanceof WallSignBlock) solid = false;  // 중복 안전
            else if (block instanceof PressurePlateBlock) solid = false;
            else if (isTrapDoor(state)) solid = false;
            // 근사 이식 — 원본과 차이: ASGrapplingHook 분기 (원본 L2143-L2145) 생략
            else if (isOpenFenceGate(state)) solid = false;
            // 근사 이식 — 원본과 차이: Carpenters _blockCarpentersLadder 분기 (원본 L2149-L2151) 생략
        }
        return solid;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2a4 (세션 102) — 잔여 보조
    //   vanilla 1:1: getTriple / isHeadedToRope
    //   mod 근사 (false/null): isOnMiddleLadderFront / getCarpentersBlockData /
    //                           isOnAnchorFront / isASGrapplingHookFront /
    //                           getRopeId / getAnchorId / isASRope / isASGrapplingHook
    // 원본: Orientation.java L1241-L1259 / L1261-L1272 / L1386-L1412 / L1414-L1440 /
    //       L1443-L1484 / L1487-L1511 / L2011-L2024.
    // ════════════════════════════════════════════════════════════════════════

    // ── vanilla 1:1 ─────────────────────────────────────────────────────────

    /**
     * 원본 L2011-L2024 `getTriple(double primary, double secondary)` — 두 좌표 소수부
     * 중심 오프셋 비교로 -1 / 0 / 1 반환.
     *
     *   primary = primary - floor(primary) - 0.5
     *   secondary = secondary - floor(secondary) - 0.5
     *   |primary|*2 < |secondary| → 0
     *   primary > 0 → 1
     *   primary < 0 → -1
     *   0 → 0
     *
     * Pure math — 근사 없음.
     */
    private static int getTriple(double primary, double secondary) {
        primary = primary - Math.floor(primary) - 0.5;
        secondary = secondary - Math.floor(secondary) - 0.5;

        if (Math.abs(primary) * 2 < Math.abs(secondary))
            return 0;
        else if (primary > 0)
            return 1;
        else if (primary < 0)
            return -1;
        else
            return 0;
    }

    /**
     * 원본 L1386-L1412 `isHeadedToRope()` — `base_id`/`base_kd` 의 (i, k) triple 조합을
     * 9 분기 (3x3) 로 매칭하여 Orientation 과 비교.
     *
     *   iTriple > 0, kTriple > 0 → this == NN
     *   iTriple > 0, kTriple < 0 → this == NP
     *   iTriple > 0, kTriple = 0 → this == NZ
     *   iTriple < 0, kTriple > 0 → this == PN
     *   iTriple < 0, kTriple < 0 → this == PP
     *   iTriple < 0, kTriple = 0 → this == PZ
     *   iTriple = 0, kTriple > 0 → this == ZN
     *   iTriple = 0, kTriple < 0 → this == ZP
     *   iTriple = 0, kTriple = 0 → this == ZZ
     *
     * rope mod 판정용이지만 자체는 pure logic — 근사 없이 1:1.
     */
    protected boolean isHeadedToRope() {
        int iTriple = getTriple(base_id, base_kd);
        int kTriple = getTriple(base_kd, base_id);

        if (iTriple > 0) {
            if (kTriple > 0)      return this == NN;
            else if (kTriple < 0) return this == NP;
            else                  return this == NZ;
        } else if (iTriple < 0) {
            if (kTriple > 0)      return this == PN;
            else if (kTriple < 0) return this == PP;
            else                  return this == PZ;
        } else {
            if (kTriple > 0)      return this == ZN;
            else if (kTriple < 0) return this == ZP;
            else                  return this == ZZ;
        }
    }

    // ── mod 근사 (§7 B-19a2a4-approx 1/2/3) ────────────────────────────────

    /**
     * 원본 L1261-L1272 `getCarpentersBlockData(i, j_offset, k)` — Carpenters mod
     * `TileEntity` 에서 reflection 으로 블록 data 추출.
     *
     * **§7 근사** (B-19a2a4-approx-1): Carpenters mod 1.21.1 미이식 → **항상 -1 반환**.
     */
    private static int getCarpentersBlockData(int i, int j_offset, int k) {
        // 근사 이식 — 원본과 차이: Carpenters mod 미이식 → 항상 -1
        return -1;
    }

    /**
     * 원본 L1241-L1259 `isOnMiddleLadderFront(j_offset)` — Carpenters block 의 metadata
     * switch 만 의존. `getCarpentersBlockData` 가 항상 -1 반환하므로 switch 매치 없음 →
     * **항상 false**.
     *
     * **§7 근사** (B-19a2a4-approx-1 연동): Carpenters mod 미이식.
     */
    protected boolean isOnMiddleLadderFront(int j_offset) {
        // 근사 이식 — 원본과 차이: Carpenters mod 미이식 → 항상 false
        return false;
    }

    /**
     * 원본 L1414-L1440 `isOnAnchorFront(j_offset)` — BetterThanWolves anchor 블록의 metadata
     * switch.
     *
     * **§7 근사** (B-19a2a4-approx-2): BetterThanWolves mod 1.21.1 미이식 → **항상 false**.
     * anchor 블록 존재 자체가 없음 (`isAnchorId` → false).
     */
    protected boolean isOnAnchorFront(int j_offset) {
        // 근사 이식 — 원본과 차이: BetterThanWolves anchor mod 미이식 → 항상 false
        return false;
    }

    /**
     * 원본 L1487-L1511 `isASGrapplingHookFront(int metaData)` — ASGrapplingHook mod 의 8
     * metadata 비트 조합 판정.
     *
     * **§7 근사** (B-19a2a4-approx-3): ASGrapplingHook mod 1.21.1 미이식 → **항상 false**.
     * `isASGrapplingHook` 자체가 false 반환되어 선행 조건에서 제거되나 안전상 직접 false.
     *
     * 원본은 `int metaData` 파라미터이나 1.21.1 에선 사용 지점이 근사 삭제되므로 파라미터
     * 형태와 무관 — `BlockState` 로 표면 매핑 (호출부 일관성).
     */
    protected boolean isASGrapplingHookFront(BlockState state) {
        // 근사 이식 — 원본과 차이: ASGrapplingHook mod 미이식 → 항상 false
        return false;
    }

    /**
     * 원본 L1443+ `getRopeId(j_offset)` — BetterThanWolves `fcRopeBlock` / RopesPlus
     * `blockRopeCentral` 판정 후 block 반환.
     *
     * **§7 근사** (B-19a2a4-approx-3): 두 mod 모두 미이식 → **항상 null**.
     */
    protected static BlockState getRopeId(int j_offset) {
        // 근사 이식 — 원본과 차이: BetterThanWolves/RopesPlus rope 미이식 → null
        return null;
    }

    /**
     * 원본 `getAnchorId(j_offset)` — BetterThanWolves `fcAnchor` 판정.
     *
     * **§7 근사** (B-19a2a4-approx-2): BetterThanWolves mod 미이식 → **항상 null**.
     */
    protected static BlockState getAnchorId(int j_offset) {
        // 근사 이식 — 원본과 차이: BetterThanWolves anchor mod 미이식 → null
        return null;
    }

    /**
     * 원본 `isASRope(Block block)` — ASRope mod `blockRope` 판정.
     *
     * **§7 근사** (B-19a2a4-approx-3): ASRope mod 미이식 → **항상 false**.
     */
    protected static boolean isASRope(BlockState state) {
        // 근사 이식 — 원본과 차이: ASRope mod 미이식 → 항상 false
        return false;
    }

    /**
     * 원본 `isASGrapplingHook(Block block)` — ASGrapplingHook mod `blockGrHk` 판정.
     *
     * **§7 근사** (B-19a2a4-approx-3): ASGrapplingHook mod 미이식 → **항상 false**.
     */
    protected static boolean isASGrapplingHook(BlockState state) {
        // 근사 이식 — 원본과 차이: ASGrapplingHook mod 미이식 → 항상 false
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2b (세션 103) — grab 상태 세팅 + initialize 헬퍼
    // 원본: Orientation.java L937-L995 (setHalfGrabType/setBottomGrabType/setGrabType),
    //       L2686-L2720 (initialize / initializeOffset / initializeLocal).
    // ════════════════════════════════════════════════════════════════════════

    // ── initialize 계열 (원본 L2686-L2720) ─────────────────────────────────

    /**
     * 원본 L2686-L2699 `initialize(World w, int i, double id, double jhd, int k, double kd)`.
     * 이 Orientation 기반 ladder gap 탐색 시작 시 static 상태 초기화.
     *
     *   world = w
     *   base_i/base_id/base_jhd/base_k/base_kd 설정
     *   remote_i = base_i + _i
     *   remote_k = base_k + _k
     */
    protected void initialize(World w, int i, double id, double jhd, int k, double kd) {
        world = w;

        base_i = i;
        base_id = id;
        base_jhd = jhd;
        base_k = k;
        base_kd = kd;

        remote_i = i + _i;
        remote_k = k + _k;
    }

    /**
     * 원본 L2701-L2713 `initializeOffset(double offset_halfs, boolean isClimbCrawling,
     * boolean isCrawlClimbing, boolean isCrawling)`.
     *
     *   crawl = isClimbCrawling || isCrawlClimbing || isCrawling
     *   offset_jhd = base_jhd + offset_halfs
     *   offset_jh = floor(offset_jhd)
     *   jh_offset = offset_jhd - offset_jh
     *   all_j = offset_jh / 2
     *   all_offset = offset_jh % 2
     *
     * 1.21.1 매핑: `MathHelper.floor_double(x)` → `MathHelper.floor(x)` (yarn 표면 매핑).
     */
    protected static void initializeOffset(double offset_halfs,
                                           boolean isClimbCrawling,
                                           boolean isCrawlClimbing,
                                           boolean isCrawling) {
        crawl = isClimbCrawling || isCrawlClimbing || isCrawling;

        double offset_jhd = base_jhd + offset_halfs;
        int offset_jh = MathHelper.floor(offset_jhd);
        jh_offset = offset_jhd - offset_jh;

        all_j = offset_jh / 2;
        all_offset = offset_jh % 2;
    }

    /**
     * 원본 L2715-L2720 `initializeLocal(int localOffset)`.
     *
     *   local_halfOffset = localOffset + all_offset
     *   local_half = |local_halfOffset| % 2
     *   local_offset = all_j + (local_halfOffset - local_half) / 2
     */
    protected static void initializeLocal(int localOffset) {
        local_halfOffset = localOffset + all_offset;
        local_half = Math.abs(local_halfOffset) % 2;
        local_offset = all_j + (local_halfOffset - local_half) / 2;
    }

    // ── setGrabType 계열 (원본 L937-L995) ──────────────────────────────────

    /**
     * 원본 L987-L995 `setGrabType(int type, Block block, boolean remote, boolean hasGrab,
     * int metaClimb)` — 최종 static 필드 할당 + hasGrab 반환.
     *
     *   grabRemote = remote
     *   grabType = hasGrab ? type : NoGrab
     *   grabBlock = block
     *   grabMeta = metaClimb
     *   return hasGrab
     *
     * 1.21.1 매핑: `Block` → `BlockState` (B-19a1a 필드 타입).
     */
    private static boolean setGrabType(int type, BlockState block, boolean remote,
                                       boolean hasGrab, int metaClimb) {
        grabRemote = remote;
        grabType = hasGrab ? type : NoGrab;
        grabBlock = block;
        grabMeta = metaClimb;
        return hasGrab;
    }

    /** 원본 L937-L940 `setHalfGrabType(int type, Block block)` — 래퍼 (`remote=true`). */
    protected boolean setHalfGrabType(int type, BlockState block) {
        return setHalfGrabType(type, block, true);
    }

    /** 원본 L942-L945 `setHalfGrabType(int type, Block block, boolean remote)` — 래퍼
     *  (`metaClimb=-1`). */
    protected boolean setHalfGrabType(int type, BlockState block, boolean remote) {
        return setHalfGrabType(type, block, remote, -1);
    }

    /**
     * 원본 L947-L960 `setHalfGrabType(int type, Block block, boolean remote, int metaClimb)` —
     * half-grab 판정 본체.
     *
     *   hasGrab = type != NoGrab
     *   hasGrab && remote && _isDiagonal 일 때 대각 진입 측면 2방향 `isUpperHalfFrontEmpty`
     *   (CCW=rotate(90)/CW=rotate(-90)) 둘 다 true 여야 grab 유지. 아니면 hasGrab=false.
     *   결과 setGrabType 위임.
     */
    protected boolean setHalfGrabType(int type, BlockState block, boolean remote, int metaClimb) {
        boolean hasGrab = type != NoGrab;
        if (hasGrab && remote && _isDiagonal) {
            boolean edgeConnectCCW = rotate(90).isUpperHalfFrontEmpty(base_i, 0, remote_k);
            boolean edgeConnectCW  = rotate(-90).isUpperHalfFrontEmpty(remote_i, 0, base_k);
            hasGrab &= edgeConnectCCW && edgeConnectCW;
        }
        return setGrabType(type, block, remote, hasGrab, metaClimb);
    }

    /** 원본 L962-L965 `setBottomGrabType(int type, Block block)` — 래퍼. */
    protected boolean setBottomGrabType(int type, BlockState block) {
        return setBottomGrabType(type, block, true);
    }

    /** 원본 L967-L970 `setBottomGrabType(int type, Block block, boolean remote)` — 래퍼. */
    protected boolean setBottomGrabType(int type, BlockState block, boolean remote) {
        return setBottomGrabType(type, block, remote, -1);
    }

    /**
     * 원본 L972-L985 `setBottomGrabType(int type, Block block, boolean remote, int metaClimb)` —
     * bottom-grab 판정 본체.
     *
     * setHalfGrabType 과 유사하나 diagonal 엣지 체크에 `isLowerHalfFrontFullEmpty` 사용
     * (upper half 가 아닌 lower half 관통 여부).
     */
    protected boolean setBottomGrabType(int type, BlockState block, boolean remote, int metaClimb) {
        boolean hasGrab = type != NoGrab;
        if (hasGrab && remote && _isDiagonal) {
            boolean edgeConnectCCW = rotate(90).isLowerHalfFrontFullEmpty(base_i, 0, remote_k);
            boolean edgeConnectCW  = rotate(-90).isLowerHalfFrontFullEmpty(remote_i, 0, base_k);
            hasGrab &= edgeConnectCCW && edgeConnectCW;
        }
        return setGrabType(type, block, remote, hasGrab, metaClimb);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2c (세션 104) — hasHalfHold 본체
    // 원본: Orientation.java L608-L726 (120줄).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L608-L726 `hasHalfHold()` — 플레이어 중앙 (y=0) 레벨 grab 가능 판정.
     *
     * 분기 순서 (첫 return 에서 종료, 미매치면 최종 `NoGrab`):
     *   (1) FreeBaseClimb: isOnLadder+isOnLadderFront → AroundGrab (base)
     *   (2) FreeBaseClimb: remoteLadderClimbing → AroundGrab (remote)
     *   (3) [§7 근사 생략] BetterThanWolves/RopesPlus rope/anchor
     *   (4) isEmpty(base) + remote==iron_bars + headedToFrontWall → HalfGrab (remote)
     *   (5) wallId==iron_bars + headedToBaseWall(0) → HalfGrab (base)
     *   (6) wallId != null + isOnMiddleLadderFront → AroundGrab (base, remoteId)
     *   (7) freeFenceClimbing 블록 (7 서브 분기):
     *       a) remote fence + front + (!baseFence → HalfGrab / baseFence+sideWall → HalfGrab)
     *       b) remoteBelow fence + front + (!baseBelowFence → HalfGrab /
     *          baseBelowFence+sideWall → HalfGrab)
     *       c) wallId fence + headedToBaseWall → HalfGrab
     *       d) belowWallId fence + headedToBaseWall → HalfGrab
     *       e) remote==cobblestone_wall + !headedToRemoteFlatWall → HalfGrab
     *       f) remoteBelow==cobblestone_wall + !headedToRemoteFlatWall → HalfGrab
     *   (8) isBottomHalfBlock(remote) OR (stair + bottomNotBack + !(baseBelow stair
     *       bottomFront)) → HalfGrab (remote)
     *   (9) trapDoor closed (remote) → HalfGrab (remote)
     *   (10) trapDoor open (base) → HalfGrab (base)
     *   (11) [§7 근사 생략] ASGrapplingHook/RopesPlus isASRope + isASGrapplingHookFront
     *   (12) FreeBaseClimb: baseVineClimbing(0) > -1 → HalfGrab (vine, meta)
     *   (13) FreeBaseClimb: remoteVineClimbing(0) > -1 → HalfGrab (vine, meta)
     *   (14) 외 → NoGrab
     *
     * **§7 근사** (B-19a2c-approx-1): mod 3 카테고리 분기 (BetterThanWolves rope/anchor /
     * RopesPlus / ASRope / ASGrapplingHook) 생략 — B-19a2a4 의 getRopeId/getAnchorId/isASRope/
     * isASGrapplingHook false/null 근사와 연동.
     */
    protected boolean hasHalfHold() {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        if (cfg.isFreeBaseClimb()) {
            if (isOnLadder(0) && isOnLadderFront(0))
                return setHalfGrabType(AroundGrab, getBaseBlockId(0), false);

            if (remoteLadderClimbing(0))
                return setHalfGrabType(AroundGrab, getRemoteBlockId(0), true);
        }

        // 근사 이식 — 원본과 차이: BetterThanWolves/RopesPlus rope+anchor 분기 (원본 L621-L629) 생략

        BlockState remoteState = getRemoteBlockId(0);
        if (isEmpty(base_i, 0, base_k)) {
            if (remoteState.getBlock() == Blocks.IRON_BARS
                    && headedToFrontWall(remote_i, 0, remote_k, remoteState))
                return setHalfGrabType(HalfGrab, remoteState);
        }

        BlockState wallState = getWallBlockId(base_i, 0, base_k);
        if (wallState != null && wallState.getBlock() == Blocks.IRON_BARS
                && headedToBaseWall(0, wallState))
            return setHalfGrabType(HalfGrab, wallState, false);
        if (wallState != null && isOnMiddleLadderFront(0))
            return setHalfGrabType(AroundGrab, remoteState, false);

        if (cfg.freeFenceClimbing) {
            if (isFence(remoteState)
                    && headedToFrontWall(remote_i, 0, remote_k, remoteState)) {
                if (!isFence(getBaseBlockId(0)))
                    return setHalfGrabType(HalfGrab, remoteState);
                else if (headedToFrontSideWall(remote_i, 0, remote_k, remoteState))
                    return setHalfGrabType(HalfGrab, remoteState);
            }

            BlockState remoteBelowState = getRemoteBlockId(-1);
            if (isFence(remoteBelowState)
                    && headedToFrontWall(remote_i, -1, remote_k, remoteBelowState)) {
                if (!isFence(getBaseBlockId(-1)))
                    return setHalfGrabType(HalfGrab, remoteState);
                else if (headedToFrontSideWall(remote_i, -1, remote_k, remoteBelowState))
                    return setHalfGrabType(HalfGrab, remoteState);
            }

            if (isFence(wallState) && headedToBaseWall(0, wallState))
                return setHalfGrabType(HalfGrab, wallState, false);

            BlockState belowWallState = getWallBlockId(base_i, -1, base_k);
            if (isFence(belowWallState) && headedToBaseWall(-1, belowWallState))
                return setHalfGrabType(HalfGrab, belowWallState, false);

            if (remoteState.getBlock() == Blocks.COBBLESTONE_WALL
                    && !headedToRemoteFlatWall(remoteState, 0))
                return setHalfGrabType(HalfGrab, remoteState);

            if (remoteBelowState.getBlock() == Blocks.COBBLESTONE_WALL
                    && !headedToRemoteFlatWall(remoteBelowState, -1))
                return setHalfGrabType(HalfGrab, remoteBelowState);
        }

        // (8) bottom half block OR (stair bottom-not-back AND !(baseBelow stair bottom-front))
        if (isBottomHalfBlock(remoteState)
                || (isStairCompact(remoteState)
                    && isBottomStairCompactNotBack(remoteState)
                    && !(isStairCompact(getBaseBlockId(-1))
                         && isBottomStairCompactFront(getBaseBlockId(-1)))))
            return setHalfGrabType(HalfGrab, remoteState);

        // (9) remote trap door closed
        if (isTrapDoor(remoteState) && isClosedTrapDoor(remoteState))
            return setHalfGrabType(HalfGrab, remoteState);

        // (10) base trap door open
        BlockState baseState = getBaseBlockId(0);
        if (isTrapDoor(baseState) && !isClosedTrapDoor(baseState))
            return setHalfGrabType(HalfGrab, baseState, false);

        // 근사 이식 — 원본과 차이: ASGrapplingHook/RopesPlus isASRope + isASGrapplingHookFront
        //                        분기 (원본 L701-L711) 생략

        if (cfg.isFreeBaseClimb()) {
            int meta = baseVineClimbing(0);
            if (meta > DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);
            meta = remoteVineClimbing(0);
            if (meta > DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);
        }

        return setHalfGrabType(NoGrab, null);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2d (세션 105) — hasBottomHold 본체
    // 원본: Orientation.java L728-L935 (200+줄).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L728-L935 `hasBottomHold()` — 플레이어 하부 (y=-1) 레벨 grab 가능 판정.
     * `hasHalfHold` 와 유사 구조이나 하부 전용 복합 중첩 분기 많음.
     *
     * 주요 분기 (미매치면 최종 `NoGrab`):
     *   (1) FreeBaseClimb: 4 ladder 체크 (base-1 / base 0 / remote-1 / remote 0) → AroundGrab
     *   (2) [§7 근사 생략] BetterThanWolves/RopesPlus rope/anchor 분기
     *   (3) [§7 근사 생략] RedPower wire 4 sub 분기
     *   (4) isEmpty(-1) + remoteBelow iron_bars + frontWall → HalfGrab
     *   (5) freeFenceClimbing 3 서브:
     *       a) remoteBelow fence + frontWall (baseBelow fence 여부로 2갈래)
     *       b) remoteBelow cobblestone_wall + !flatWall
     *       c) remote cobblestone_wall + !flatWall
     *   (6) belowWallId 존재 시 중첩 분기:
     *       a) isEmpty 양옆(0, -1) + iron_bars + baseWall(-1) → BottomGrab
     *       b) isOnMiddleLadderFront(-1) → AroundGrab (remote, HALF)
     *       c) headedToBaseGrabWall(-1) → BottomGrab
     *       d) freeFenceClimbing + fence + baseWall(-1) → BottomGrab
     *   (7) 복합 중첩 (remoteLowerHalfEmpty + isBaseAccessible(-1, true, false) + 6 AND 체크):
     *       → BottomGrab
     *   (8) stair compact remote + topStair+!topBack + upperHalfFrontFullSolid → BottomGrab
     *   (9) baseBelow open trap door → BottomGrab
     *   (10) baseBelow door top + frontBlocked + isBaseAccessible(0) → BottomGrab
     *   (11) [§7 근사 생략] ASGrapplingHook/RopesPlus 4 sub 분기
     *   (12)-(15) FreeBaseClimb: 4 vine (base-1 / base 0 / remote-1 / remote 0) → HalfGrab
     *   (16) 외 → NoGrab
     *
     * **§7 근사** (B-19a2d-approx-1): RedPower + BetterThanWolves + ASGrapplingHook +
     * RopesPlus + ASRope mod 분기 전부 생략. B-19a2a4 false/null 근사 및 B-19a2c 의
     * 동일 패턴과 일관.
     */
    protected boolean hasBottomHold() {
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        if (cfg.isFreeBaseClimb()) {
            if (isOnLadder(-1) && isOnLadderFront(-1))
                return setBottomGrabType(AroundGrab, getBaseBlockId(-1), false);

            if (isOnLadder(0) && isOnLadderFront(0))
                return setBottomGrabType(AroundGrab, getBaseBlockId(0), false);

            if (remoteLadderClimbing(-1))
                return setBottomGrabType(AroundGrab, getRemoteBlockId(-1), true);

            if (remoteLadderClimbing(0))
                return setBottomGrabType(AroundGrab, getRemoteBlockId(0), true);
        }

        // 근사 이식 — 원본과 차이: BetterThanWolves/RopesPlus rope+anchor 분기 (원본 L749-L759) 생략

        BlockState remoteState = getRemoteBlockId(0);
        BlockState remoteBelowState = getRemoteBlockId(-1);
        boolean remoteLowerHalfEmpty = isLowerHalfFrontFullEmpty(remote_i, 0, remote_k);

        // 근사 이식 — 원본과 차이: RedPower wire 4 sub 분기 (원본 L765-L795) 생략

        if (isEmpty(base_i, -1, base_k)) {
            if (remoteBelowState.getBlock() == Blocks.IRON_BARS
                    && headedToFrontWall(remote_i, -1, remote_k, remoteBelowState))
                return setBottomGrabType(HalfGrab, remoteBelowState);
        }

        if (cfg.freeFenceClimbing) {
            BlockState baseBelowBlockState = getBaseBlockId(-1);
            if (isFence(remoteBelowState)
                    && headedToFrontWall(remote_i, -1, remote_k, remoteBelowState)) {
                if (!isFence(baseBelowBlockState))
                    return setBottomGrabType(HalfGrab, remoteBelowState);
                else if (headedToFrontSideWall(remote_i, -1, remote_k, remoteBelowState))
                    return setBottomGrabType(HalfGrab, remoteBelowState);
            }

            if (remoteBelowState.getBlock() == Blocks.COBBLESTONE_WALL
                    && !headedToRemoteFlatWall(remoteBelowState, -1))
                return setHalfGrabType(HalfGrab, remoteBelowState);

            if (remoteState.getBlock() == Blocks.COBBLESTONE_WALL
                    && !headedToRemoteFlatWall(remoteState, 0))
                return setHalfGrabType(HalfGrab, remoteState);
        }

        BlockState belowWallBlockState = getWallBlockId(base_i, -1, base_k);
        if (belowWallBlockState != null) {
            if (isEmpty(base_i - _i, 0, base_k - _k)
                    && isEmpty(base_i - _i, -1, base_k - _k)) {
                if (belowWallBlockState.getBlock() == Blocks.IRON_BARS
                        && headedToBaseWall(-1, belowWallBlockState))
                    return setBottomGrabType(HalfGrab, belowWallBlockState, false);
                if (isOnMiddleLadderFront(-1))
                    return setHalfGrabType(AroundGrab, remoteState, false);

                if (headedToBaseGrabWall(-1, belowWallBlockState))
                    return setBottomGrabType(HalfGrab, belowWallBlockState, false);
            }

            if (cfg.freeFenceClimbing
                    && isFence(belowWallBlockState)
                    && headedToBaseWall(-1, belowWallBlockState))
                return setBottomGrabType(HalfGrab, belowWallBlockState, false);
        }

        // (7) 복합 중첩 — 원본 L853-L865
        if (remoteLowerHalfEmpty && isBaseAccessible(-1, true, false))
            if (isUpperHalfFrontAnySolid(remote_i, -1, remote_k))
                if (!isBottomHalfBlock(remoteBelowState))
                    if (!isStairCompact(remoteBelowState)
                            || !isBottomStairCompactFront(remoteBelowState))
                        if (!isDoor(remoteBelowState) || isDoorTop(remoteBelowState))
                            if (!isDoor(getBaseBlockId(0))
                                    || !isDoorFrontBlocked(base_i, 0, base_k))
                                if (cfg.freeFenceClimbing
                                        || !isFence(remote_i, -1, remote_k))
                                    return setBottomGrabType(HalfGrab, remoteBelowState);

        // (8) 원본 L867-L874 stair compact top-front + upper half front full solid
        if (isStairCompact(remoteState)) {
            if (isTopStairCompact(remoteState)
                    && !isTopStairCompactBack(remoteState)
                    && isUpperHalfFrontFullSolid(remote_i, -1, remote_k))
                return setBottomGrabType(HalfGrab, remoteBelowState);
        }

        BlockState baseBelowState = getBaseBlockId(-1);

        // (9) baseBelow open trap door — 원본 L879-L881
        if (isTrapDoor(baseBelowState) && !isClosedTrapDoor(baseBelowState))
            return setBottomGrabType(HalfGrab, baseBelowState, false);

        // (10) baseBelow door top + frontBlocked + baseAccessible(0) — 원본 L883-L886
        if (isDoor(baseBelowState) && isDoorTop(baseBelowState)
                && isDoorFrontBlocked(base_i, -1, base_k)
                && isBaseAccessible(0))
            return setBottomGrabType(HalfGrab, baseBelowState, false);

        // 근사 이식 — 원본과 차이: ASGrapplingHook/RopesPlus 4 sub 분기 (원본 L888-L909) 생략

        if (cfg.isFreeBaseClimb()) {
            int meta = baseVineClimbing(-1);
            if (meta != DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);

            meta = baseVineClimbing(0);
            if (meta != DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);

            meta = remoteVineClimbing(-1);
            if (meta != DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);

            meta = remoteVineClimbing(0);
            if (meta != DefaultMeta)
                return setHalfGrabType(HalfGrab, Blocks.VINE.getDefaultState(), false, meta);
        }

        return setBottomGrabType(NoGrab, null);
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a2e (세션 106) — isLadderSubstitute 본체 + 4 메서드 / B-19 도미노 해소 실체
    // 원본: Orientation.java L299-L327 (public Feet/Hands + internal wrapper),
    //       L477-L606 (isLadderSubstitute 본체 130줄 gap 1-5 계산).
    //
    // **핵심**: gap 계산 + `ClimbGap.canStand=gap>3` / `mustCrawl=gap>1 && gap<4` 설정 —
    // B-17/B-18/B-16c 공식의 `hasClimbGap`/`hasClimbCrawlGap` 무력화를 해소.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L299-L306 `isFeetLadderSubstitute(World world, int bi, int j, int bk)` — 발 레벨
     * ladder 대체 가능 여부. `(bi+_i, j, bk+_k)` 원격 위치에서 middle/base 두 offset gap 체크.
     */
    public boolean isFeetLadderSubstitute(World w, int bi, int j, int bk) {
        int i = bi + _i;
        int k = bk + _k;
        return isLadderSubstitute(w, i, j, k, middle) > 0
            || isLadderSubstitute(w, i, j, k, base) > 0;
    }

    /**
     * 원본 L308-L316 `isHandsLadderSubstitute(World world, int bi, int j, int bk)` — 손 레벨
     * ladder 대체 가능. middle/base/sub 3 offset gap 체크.
     */
    public boolean isHandsLadderSubstitute(World w, int bi, int j, int bk) {
        int i = bi + _i;
        int k = bk + _k;
        return isLadderSubstitute(w, i, j, k, middle) > 0
            || isLadderSubstitute(w, i, j, k, base) > 0
            || isLadderSubstitute(w, i, j, k, sub) > 0;
    }

    /**
     * 원본 L318-L327 `isLadderSubstitute(World worldObj, int i, int j, int k, int halfOffset)` —
     * 외부 API 래퍼. static 상태 필드 (world/remote_i/all_j/remote_k/all_offset) 를 설정하고
     * 내부 본체 호출.
     */
    private int isLadderSubstitute(World worldObj, int i, int j, int k, int halfOffset) {
        world = worldObj;
        remote_i = i;
        all_j = j;
        remote_k = k;
        all_offset = 0;
        return isLadderSubstitute(halfOffset, null);
    }

    /**
     * 원본 L477-L606 `isLadderSubstitute(int local_Offset, ClimbGap out_climbGap)` — gap 계산
     * 본체 (130줄). local_half (0/1) 로 upper/lower half 분기, 각각 hasHalfHold/hasBottomHold
     * 로 grab 가능 확인 후 `overLadder`/`overAccessible`/`overFullAccessible` 조합으로
     * gap 1-5 결정.
     *
     * 최종 `ClimbGap` 설정 (gap > 0 시):
     *   state = grabBlock
     *   canStand = gap > 3  ← B-17/B-18 `hasClimbGap` 의 원천
     *   mustCrawl = gap > 1 && gap < 4  ← `hasClimbCrawlGap` 의 원천
     *   direction = this
     *
     * 1.21.1 매핑:
     *   - `out_climbGap.Block = grabBlock` → `out_climbGap.state = grabBlock` (BlockState)
     *   - `out_climbGap.Meta = grabMeta` **생략** — 1.21.1 ClimbGap 에 meta 필드 없음
     *     (BlockState 내재 표면 매핑).
     */
    protected int isLadderSubstitute(int local_Offset, ClimbGap out_climbGap) {
        initializeLocal(local_Offset);

        int gap;
        if (local_half == 1) {
            // upper half — hasHalfHold 로 grab 가능 여부 확인
            if (hasHalfHold()) {
                if (!grabRemote) {
                    // base grab (플레이어 자신이 잡음)
                    boolean overLadder = isOnLadderOrVine(0)
                                      || isOnOpenTrapDoor(0)
                                      || isRope(0)
                                      || isOnWallRope(0);
                    boolean overOverLadder = isOnLadderOrVine(1)
                                          || isOnOpenTrapDoor(1)
                                          || isRope(1)
                                          || isOnWallRope(1);
                    boolean overAccessible = isBaseAccessible(1, false, true);
                    boolean overOverAccessible = isBaseAccessible(2, false, true);
                    boolean overFullAccessible = overAccessible
                            && isFullAccessible(1, grabRemote);
                    boolean overOverFullAccessible = overAccessible
                            && isFullExtentAccessible(2, grabRemote);

                    if (overLadder) {
                        if (overOverLadder)
                            gap = 1;
                        else if (overOverAccessible)
                            gap = 1;
                        else
                            gap = 1;
                    } else if (overAccessible) {
                        if (overFullAccessible) {
                            if (overOverFullAccessible)
                                gap = 5;
                            else
                                gap = crawl ? 3 : 5;
                        } else if (overOverLadder)
                            gap = 5;
                        else
                            gap = 1;
                    } else
                        gap = 1;
                } else if (isBaseAccessible(0)) {
                    // remote grab (이 방향 블록을 잡음)
                    if (isUpperHalfFrontEmpty(remote_i, 0, remote_k)) {
                        if (isFullAccessible(1, grabRemote)) {
                            if (isFullExtentAccessible(2, grabRemote))
                                gap = 5;
                            else if (isJustLowerHalfExtentAccessible(2))
                                gap = 4;
                            else
                                gap = 3;
                        } else
                            gap = 1;
                    } else
                        gap = 1;
                } else
                    gap = 0;
            } else
                gap = 0;
        } else {
            // lower half — hasBottomHold 로 grab 가능 확인
            if (hasBottomHold()) {
                if (!grabRemote) {
                    boolean overLadder = isOnLadderOrVine(0)
                                      || isOnOpenTrapDoor(0)
                                      || isRope(0)
                                      || isOnWallRope(0);
                    // 원본 L548-L550: 마지막은 `isOnWallRope(0)` (0 이지 1 아님) — 원본 버그
                    // 가능성 있으나 **1:1 이식** 원칙 유지.
                    boolean overOverLadder = isOnLadderOrVine(1)
                                          || isOnOpenTrapDoor(1)
                                          || isRope(1)
                                          || isOnWallRope(0);
                    boolean overAccessible = isBaseAccessible(0, false, true);
                    boolean overOverAccessible = isBaseAccessible(1, false, true);
                    boolean overFullAccessible = overAccessible
                            && isFullAccessible(0, grabRemote);
                    boolean overOverFullAccessible = overAccessible
                            && isFullExtentAccessible(1, grabRemote);

                    if (overLadder) {
                        if (overOverLadder)
                            gap = 1;
                        else if (overOverAccessible)
                            gap = 1;
                        else
                            gap = 1;
                    } else if (overAccessible) {
                        if (overFullAccessible) {
                            if (overOverAccessible) {
                                if (overOverFullAccessible)
                                    gap = 4;
                                else
                                    gap = crawl ? 2 : 4;
                            } else
                                gap = 2;
                        } else if (overOverLadder)
                            gap = 2;
                        else
                            gap = 1;
                    } else
                        gap = 1;
                } else if (isBaseAccessible(0)) {
                    if (isFullAccessible(0, grabRemote)) {
                        if (isFullExtentAccessible(1, grabRemote))
                            gap = 4;
                        else
                            gap = 2;
                    } else
                        gap = 1;
                } else
                    gap = 0;
            } else
                gap = 0;
        }

        if (out_climbGap != null && gap > 0) {
            out_climbGap.state = grabBlock;
            // 근사 이식 — 원본과 차이: out_climbGap.Meta = grabMeta 생략 (1.21.1 BlockState
            // 에 metadata 내재, ClimbGap 에 Meta 필드 없음).
            out_climbGap.canStand  = gap > 3;
            out_climbGap.mustCrawl = gap > 1 && gap < 4;
            out_climbGap.direction = this.toBlockDirection();
        }
        return gap;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a3 (세션 107) — handsClimbing() + feetClimbing() 판정
    // 원본: Orientation.java L329-L395 (handsClimbing) + L398-L475 (feetClimbing).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L329-L395 `handsClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, out_climbGap)`.
     *
     * 손 레벨 ladder 상태 판정. `initializeOffset(3D, ...)` 후 4 halfOffset (middle/base/
     * sub/subSub) 별 `isLadderSubstitute` 호출 결과로 HandsClimbing + ClimbGap 결정.
     *
     *   middle: gap > 0 → jh_offset 기반 Up 또는 None
     *   base: gap > 0 → jh_offset 기반 BottomHold 또는 Up
     *   sub (SkipGaps 설정 후): gap > 0 && !(isCrawling && gap > 1) → 4 갈래
     *     (FastUp / TopHold / Up / Sink) — isClimbCrawling / grabType / jh_offset 조합
     *   subSub: gap > 0 && !isCrawling → 3 갈래 (TopHold / FastUp / Sink)
     */
    protected HandsClimbing handsClimbing(boolean isClimbCrawling, boolean isCrawlClimbing,
                                          boolean isCrawling, ClimbGap out_climbGap) {
        out_climbGap.reset();
        _climbGapTemp.reset();

        initializeOffset(3D, isClimbCrawling, isCrawlClimbing, isCrawling);

        HandsClimbing result = HandsClimbing.NONE;
        int gap;

        ClimbGap[] outArr = { out_climbGap };

        if ((gap = isLadderSubstitute(middle, _climbGapTemp)) > 0) {
            if (jh_offset > 1D - _handClimbingHoldGap)
                result = result.max(HandsClimbing.UP, outArr, _climbGapTemp);
            else
                result = result.max(HandsClimbing.NONE, outArr, _climbGapTemp);
        }

        if ((gap = isLadderSubstitute(base, _climbGapTemp)) > 0) {
            if (jh_offset < _handClimbingHoldGap)
                result = result.max(HandsClimbing.BOTTOM_HOLD, outArr, _climbGapTemp);
            else
                result = result.max(HandsClimbing.UP, outArr, _climbGapTemp);
        }

        _climbGapTemp.skipGaps = isClimbCrawling || isCrawlClimbing;

        if ((gap = isLadderSubstitute(sub, _climbGapTemp)) > 0
                && !(isCrawling && gap > 1)) {
            if (!isClimbCrawling && gap > 2) {
                result = result.max(HandsClimbing.FAST_UP, outArr, _climbGapTemp);
            } else if (isClimbCrawling && gap > 1) {
                result = result.max(HandsClimbing.FAST_UP, outArr, _climbGapTemp);
            } else {
                if (jh_offset < _handClimbingHoldGap) {
                    if (grabType == AroundGrab)
                        result = result.max(HandsClimbing.UP, outArr, _climbGapTemp);
                    else
                        result = result.max(HandsClimbing.TOP_HOLD, outArr, _climbGapTemp);
                } else {
                    if (grabType == AroundGrab)
                        result = result.max(HandsClimbing.TOP_HOLD, outArr, _climbGapTemp);
                    else
                        result = result.max(HandsClimbing.SINK, outArr, _climbGapTemp);
                }
            }
        }

        if ((gap = isLadderSubstitute(subSub, _climbGapTemp)) > 0 && !isCrawling) {
            if ((gap > 2 && !isCrawlClimbing)
                    || grabType == AroundGrab
                    || (gap > 1 && isClimbCrawling)) {
                if (jh_offset < _handClimbingHoldGap && !isClimbCrawling)
                    result = result.max(HandsClimbing.TOP_HOLD, outArr, _climbGapTemp);
                else if (isClimbCrawling)
                    result = result.max(HandsClimbing.FAST_UP, outArr, _climbGapTemp);
                else
                    result = result.max(HandsClimbing.SINK, outArr, _climbGapTemp);
            }
        }

        return result;
    }

    /**
     * 원본 L398-L475 `feetClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, out_climbGap)`.
     *
     * 발 레벨 ladder 상태 판정. `initializeOffset(0D, ...)` 후 4 halfOffset (top/middle/
     * base/sub) 별 `isLadderSubstitute` 호출 결과로 FeetClimbing + ClimbGap 결정.
     *
     *   top: gap > 0 → None 선택 (placeholder — ClimbGap 만 업데이트)
     *   middle (SkipGaps 설정 후): gap > 0 && !isCrawling → 4 갈래
     *     * gap > 3 && !isClimbCrawling: FastUp 또는 None (isCrawlClimbing 여부)
     *     * (isClimbCrawling || isCrawlClimbing) && gap > 1: BaseWithHands/FastUp
     *     * gap > 2: SlowUpWithHoldWithoutHands / None
     *     * else: TopWithHands
     *   base: gap > 0 → jh_offset / gap / isCrawl 조합 5 갈래
     *   sub: gap > 0 → None
     *   최종: isCrawlClimbing || isCrawling → BaseWithHands 강제 승격
     */
    protected FeetClimbing feetClimbing(boolean isClimbCrawling, boolean isCrawlClimbing,
                                        boolean isCrawling, ClimbGap out_climbGap) {
        out_climbGap.reset();
        _climbGapTemp.reset();

        initializeOffset(0D, isClimbCrawling, isCrawlClimbing, isCrawling);
        FeetClimbing result = FeetClimbing.NONE;
        int gap;

        ClimbGap[] outArr = { out_climbGap };

        if ((gap = isLadderSubstitute(top, _climbGapTemp)) > 0)
            result = result.max(FeetClimbing.NONE, outArr, _climbGapTemp);

        _climbGapTemp.skipGaps = isClimbCrawling || isCrawlClimbing;

        if ((gap = isLadderSubstitute(middle, _climbGapTemp)) > 0 && !isCrawling) {
            if (gap > 3 && !isClimbCrawling) {
                if (!isCrawlClimbing)
                    result = result.max(FeetClimbing.FAST_UP, outArr, _climbGapTemp);
                else
                    result = result.max(FeetClimbing.NONE, outArr, _climbGapTemp);
            } else if ((isClimbCrawling || isCrawlClimbing) && gap > 1) {
                if (isCrawlClimbing)
                    result = result.max(FeetClimbing.BASE_WITH_HANDS, outArr, _climbGapTemp);
                else
                    result = result.max(FeetClimbing.FAST_UP, outArr, _climbGapTemp);
            } else if (gap > 2) {
                if (!isClimbCrawling)
                    result = result.max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS,
                            outArr, _climbGapTemp);
                else
                    result = result.max(FeetClimbing.NONE, outArr, _climbGapTemp);
            } else {
                result = result.max(FeetClimbing.TOP_WITH_HANDS, outArr, _climbGapTemp);
            }
        }

        if ((gap = isLadderSubstitute(base, _climbGapTemp)) > 0) {
            if (gap > 3 && !isCrawling && !isCrawlClimbing) {
                result = result.max(FeetClimbing.FAST_UP, outArr, _climbGapTemp);
            } else if (gap > 2 && !isCrawling) {
                if (!isClimbCrawling) {
                    if (jh_offset < _handClimbingHoldGap)
                        result = result.max(FeetClimbing.SLOW_UP_WITH_HOLD_WITHOUT_HANDS,
                                outArr, _climbGapTemp);
                    else
                        result = result.max(FeetClimbing.SLOW_UP_WITH_SINK_WITHOUT_HANDS,
                                outArr, _climbGapTemp);
                } else {
                    result = result.max(FeetClimbing.NONE, outArr, _climbGapTemp);
                }
            } else {
                if (jh_offset < 1D - _handClimbingHoldGap)
                    result = result.max(FeetClimbing.BASE_WITH_HANDS, outArr, _climbGapTemp);
                else
                    result = result.max(FeetClimbing.BASE_HOLD, outArr, _climbGapTemp);
            }
        }

        if (isLadderSubstitute(sub, _climbGapTemp) > 0)
            result = result.max(FeetClimbing.NONE, outArr, _climbGapTemp);

        if (isCrawlClimbing || isCrawling)
            result = result.max(FeetClimbing.BASE_WITH_HANDS, outArr, _climbGapTemp);

        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // B-19a4 (세션 108) — seekClimbGap 메서드 이식
    // 원본: Orientation.java L207-L224.
    // **B-19 도미노 해소의 외부 API 진입점** — Climber 에서 각 방향 호출.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 원본 L207-L224 `seekClimbGap(rotation, world, i, id, jhd, k, kd, isClimbCrawling,
     * isCrawlClimbing, isCrawling, inout_handsClimbing, inout_feetClimbing,
     * out_handsClimbGap, out_feetClimbGap)`.
     *
     * 이 Orientation 이 플레이어 회전각 범위 (`isRotationForClimbing`) 에 맞을 때만
     * 내부 상태 (`initialize`) 설정하고 `handsClimbing`/`feetClimbing` 판정 결과를
     * inout 파라미터에 max 누적. 각 방향별 호출 결과를 합산하는 패턴.
     *
     * 1.21.1 매핑: `HandsClimbing.max` / `FeetClimbing.max` 의 `inout_thisGap` 이
     * `ClimbGap[]` 배열 래퍼이므로 `out_handsClimbGap` / `out_feetClimbGap` 을 배열
     * 래핑해서 전달.
     */
    public void seekClimbGap(float rotation, World w, int i, double id,
                             double jhd, int k, double kd,
                             boolean isClimbCrawling, boolean isCrawlClimbing, boolean isCrawling,
                             HandsClimbing[] inout_handsClimbing, FeetClimbing[] inout_feetClimbing,
                             ClimbGap out_handsClimbGap, ClimbGap out_feetClimbGap) {
        if (isRotationForClimbing(rotation)) {
            initialize(w, i, id, jhd, k, kd);

            ClimbGap[] handsArr = { out_handsClimbGap };
            inout_handsClimbing[0] = inout_handsClimbing[0].max(
                    handsClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, _climbGapOuterTemp),
                    handsArr, _climbGapOuterTemp);

            ClimbGap[] feetArr = { out_feetClimbGap };
            inout_feetClimbing[0] = inout_feetClimbing[0].max(
                    feetClimbing(isClimbCrawling, isCrawlClimbing, isCrawling, _climbGapOuterTemp),
                    feetArr, _climbGapOuterTemp);
        }
    }
}
