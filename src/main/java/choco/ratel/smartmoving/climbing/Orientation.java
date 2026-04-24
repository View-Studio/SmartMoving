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
     * 에 사용. B-19a2/a3 이식 시 초기화 로직 추가.
     */
    protected static double jh_offset;

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
}
