package choco.ratel.smartmoving.climbing;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
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
}
