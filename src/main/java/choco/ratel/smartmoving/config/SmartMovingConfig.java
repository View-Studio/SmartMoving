package choco.ratel.smartmoving.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * SmartMoving 설정 파일 로드/저장.
 * 원본 net.smart.properties 시스템을 java.util.Properties로 단순화.
 * 설정 파일: config/smart_moving_options.properties
 */
public class SmartMovingConfig {

    // ── 버전 (config_system.md: SmartMovingConfig._sm_current = "3.2" → 1.21.1 포트는 "1.0") ──
    public static final String SM_VERSION = "1.0";

    // ── 서버 배포 제어 플래그 (config_system.md 2-1, 5-3) ───────────
    /** true → 접속한 모든 클라이언트에게 이 설정을 강제 적용. */
    public boolean globalConfig = false;
    /** true → 플레이어별 개인 설정 관리 활성화 (현재 미구현 — globalConfig 우선). */
    public boolean serverConfig = false;

    // ── Global Speed ────────────────────────────────────────────
    public float speedFactor = 1F;
    public boolean speedUser = true;
    public float speedUserFactor = 0.2F;
    public int speedUserExponent = 0;
    /**
     * 원본: Property<Map<String,Integer>> _speedUsersExponents = IntegerMap("move.speed.users.exponents").singular();
     * 플레이어별 속도 지수 맵. username → exponent.
     * 파일 저장 형식: 원본 Value.tryParseIntegerMap() 대응 — CSV `"user1,5,user2,-2,..."`.
     * toArray(username) 시 move.speed.user.exponent 값을 해당 플레이어 개인 값으로 치환.
     */
    public Map<String, Integer> playerSpeedExponents = new HashMap<>();

    // ── Movement Modes ──────────────────────────────────────────
    public boolean vanillaStyle = false;
    public float sneakFactor = 0.3F;
    public float crawlFactor = 0.15F;
    public float runFactor = 1.3F;
    public float sprintFactor = 1.5F;

    // ── Climbing ────────────────────────────────────────────────
    // 원본: Options._baseClimb = "standard" (processBlockCode §0 코드). "standard" → true, 그 외 → false
    public boolean baseClimb = true;
    public boolean freeClimb = true;
    /** 원본: _baseClimb = "simple" — grab 없이 표면 접촉만으로 자동 클라이밍 */
    public boolean simpleClimb = false;
    /** 원본: _baseClimb = "smart" — 인접 블록 substitute 판정으로 자동 클라이밍 */
    public boolean smartClimb = false;
    public float freeClimbingUpSpeedFactor   = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    public float freeClimbingDownSpeedFactor = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    public float ceilingClimbingSpeedFactor = 0.2F;
    // 원본: Config._freeClimbFallDamageStartDistance = 2F, _freeClimbFallDamageFactor = 2F
    public float freeClimbFallDamageStartDistance = 2.0F;
    public float freeClimbFallDamageFactor        = 2.0F;
    // 원본: Options._climbJumpBackHeadOnGrab — false=grabNotPressed시 headJump, true=grabbed시 headJump
    public boolean climbJumpBackHead = false;
    public boolean climbExhaustion = false;
    public float climbExhaustionStart = 60F;
    public float climbExhaustionStop = 100F;
    public boolean ceilingClimbExhaustion = false;
    public float ceilingClimbExhaustionStart = 40F;
    public float ceilingClimbExhaustionStop = 100F;

    // ── Swimming / Diving ───────────────────────────────────────
    public boolean swim = true;
    public float swimSpeedFactor = 1F;
    public boolean dive = true;
    public float diveSpeedFactor = 1F;
    /**
     * 원본: _swimDownOnSneak = Unmodified("move.swim.down.sneak") → 기본값 true
     * true → 수영 중 스니크 시 하강. SmartMovingSelf wouldWantSneak 및 swimDown 계산에 사용.
     */
    public boolean swimDownOnSneak = true;
    /**
     * 원본: _diveDownOnSneak = Unmodified("move.dive.down.sneak") → 기본값 true
     * true → 잠수 중 스니크 시 하강. SmartMovingSelf wouldWantSneak 및 diveDown 계산에 사용.
     */
    public boolean diveDownOnSneak = true;

    // ── Jumping ─────────────────────────────────────────────────
    // 원본: _wallUpJump = Unmodified("move.jump.wall") → 기본값 true
    public boolean wallUpJump = true;
    // 원본: _wallHeadJump = Unmodified("move.jump.wall.head") → 기본값 true
    public boolean wallHeadJump = true;
    // 원본: _wallUpJumpFallMaximumDistance = Positive(...).defaults(2F)
    public float wallUpJumpFallMaximumDistance = 2F;
    // 원본: _wallHeadJumpFallMaximumDistance = Positive(...).defaults(3F)
    public float wallHeadJumpFallMaximumDistance = 3F;
    // 원본: _wallUpJumpOrthogonalTolerance = Positive(...).defaults(5F)
    public float wallUpJumpOrthogonalTolerance = 5F;
    // 원본: _wallUpJumpVerticalFactor = DecreasingFactor(...).defaults(0.4F)
    // tryJump(WallUp, angle!=null): verticalMotion = -0.078 + 0.498 * wallUpJumpVerticalFactor
    public float wallUpJumpVerticalFactor = 0.4F;
    // 원본: _wallHeadJumpVerticalFactor = DecreasingFactor(...).defaults(0.3F)
    // tryJump(WallHead): verticalMotion = -0.078 + 0.498 * (wallUpJumpVerticalFactor + wallHeadJumpVerticalFactor)
    public float wallHeadJumpVerticalFactor = 0.3F;
    // 원본: _wallUpJumpHorizontalFactor = DecreasingFactor(...).defaults(0.15F)
    public float wallUpJumpHorizontalFactor = 0.15F;
    // 원본: _wallHeadJumpHorizontalFactor = DecreasingFactor(...).defaults(0.15F)
    public float wallHeadJumpHorizontalFactor = 0.15F;
    public boolean jumpCharge = true;
    public float jumpChargeMaximum = 20F;
    public float jumpChargeFactor = 1.3F;
    public boolean headJump = true;
    public float headJumpControlFactor = 0.2F;
    public float headJumpChargeMaximum = 10F;
    public boolean angleJumpSide = true;
    public boolean angleJumpBack = true;
    public float angleJumpHorizontalFactor = 0.3F;
    public float angleJumpVerticalFactor = 0.2F;
    /**
     * 원본: _angleJumpDoubleClickTicks = Positive("move.jump.angle.double.click.ticks").singular().up(3F, 2F)
     * 각도 점프 더블클릭 감지 타이머(틱). 첫 클릭 후 이 시간 내에 두 번째 클릭 시 트리거.
     * 기본 3F, 최솟값 2F. 사용 시 (int)Math.ceil(value).
     */
    public float angleJumpDoubleClickTicks = 3F;
    /**
     * 원본: _wallJumpDoubleClick = Unmodified("move.jump.wall.double.click").singular()
     * true=더블클릭+홀드로 벽 점프 발동 / false=싱글클릭+홀드. Unmodified 기본값 true.
     */
    public boolean wallJumpDoubleClick = true;
    /**
     * 원본: _wallJumpDoubleClickTicks = Positive("move.jump.wall.double.click.ticks").singular().up(3F, 2F)
     * 벽 점프 더블클릭 타이머(틱). 기본 3F, 최솟값 2F.
     */
    public float wallJumpDoubleClickTicks = 3F;

    // ── Sliding ─────────────────────────────────────────────────
    // 원본: SmartMovingConfig._slideSlipperinessFactor (PositiveFactor, 기본값 1F)
    public float slideSlipperinessFactor = 1.0F;
    // 원본: SmartMovingConfig._slideParticlePeriodFactor (PositiveFactor, 기본값 0.5F)
    public float slideParticlePeriodFactor = 0.5F;
    // 원본: SmartMovingConfig._slidingSpeedStopFactor (PositiveFactor, 기본값 1F)
    public float slidingSpeedStopFactor = 1.0F;

    // ── Flying ──────────────────────────────────────────────────
    // 원본: Config._flyingSpeedFactor (PositiveFactor, 기본값 1F)
    public float flyingSpeedFactor = 1.0F;
    // 원본: Options._flyControlVertical — pitch 방향 3D 이동 활성화 여부 (기본값 true)
    public boolean flyControlVertical = true;
    /**
     * 원본: _diveControlVertical = Unmodified("move.dive.control.vertical").singular()
     * 기본값 true. 잠수 중 pitch 방향 3D 이동 활성화 여부.
     * SmartMovingSelf.handleSwimming (isDiving 분기) 에서 moveFlying 5-인자 treeDimensional 파라미터로 전달.
     */
    public boolean diveControlVertical = true;

    // ── Crawl edge protection ──────────────────────────────────
    // 원본: SmartMovingConfig._crawlOverEdge = Unmodified("move.crawl.edge") → 기본값 true
    // false → 크롤링 중 엣지에서 isSneaking=true 강제 (추락 방지)
    public boolean crawlOverEdge = true;

    // ── Crawl / Sneak toggle ────────────────────────────────────
    // 원본: Options._crawlToggle = Modified("move.crawl.toggle") → 기본값 false
    // false=홀드(스닉키 누름 유지), true=토글(grab 한 번 눌러 크롤링 고정/해제)
    public boolean crawlToggle = false;
    /**
     * 원본: _sneakToggle = Modified("move.sneak.toggle") → 기본값 false
     * false=홀드(스닉키 누름 유지), true=토글(스닉키 한 번 눌러 스닉 고정/해제).
     * 원본 isSneakToggleEnabled() = _sneakToggle.value && enabled.
     */
    public boolean sneakToggle = false;

    // ── Name tag display ────────────────────────────────────────
    // 원본: _sneakNameTag = Modified("move.sneak.name") → 기본값 true
    // true → 스니킹 중에도 이름 태그 64 거리 기준 (vanilla는 32)
    public boolean sneakNameTag = true;
    // 원본: _crawlNameTag = Modified("move.crawl.name") → 기본값 true
    // true → 크롤 중 이름 태그 표시, false → 숨김
    public boolean crawlNameTag = true;

    // ── HUD display ────────────────────────────────────────────
    /**
     * 원본: _displayExhaustionBar = Unmodified("move.gui.exhaustion.bar").singular()
     * 기본값 true (Unmodified). 소진 바 HUD 표시 여부.
     * SmartMovingRender.renderGuiIngame 진입 조건:
     *   Config.enabled && (displayExhaustionBar || displayJumpChargeBar)
     */
    public boolean displayExhaustionBar = true;
    /**
     * 원본: _displayJumpChargeBar = Unmodified("move.gui.jump.charge.bar").singular()
     * 기본값 true. 점프 차지 바 HUD 표시 여부.
     */
    public boolean displayJumpChargeBar = true;

    // ── Flying close-to-ground ─────────────────────────────────
    // 원본: _flyCloseToGround = Modified("move.fly.ground.close") → 기본값 true
    public boolean flyCloseToGround = true;
    // 원본: _flyWhileOnGround = Modified("move.fly.ground.collide").depends(_flyCloseToGround) → 기본값 true
    // flyCloseToGround=false이면 비활성. 착지 시 비행 유지.
    public boolean flyWhileOnGround = true;

    // ── Perspective (FOV) ──────────────────────────────────────
    // 원본: _perspectiveFadeFactor = PositiveFactor.values(0.5F, 0.1F, 1F)
    public float perspectiveFadeFactor = 0.5F;
    // 원본: _perspectiveSpeedFactor = Float.defaults(1F)
    public float perspectiveSpeedFactor = 1F;
    // 원본: _perspectiveSpeedFactorMax = PositiveFactor.defaults(0F)
    public float perspectiveSpeedFactorMax = 0F;
    // 원본: _perspectiveRunFactor = Float.defaults(1F)
    public float perspectiveRunFactor = 1F;
    // 원본: _perspectiveSprintFactor = Float.defaults(1.5F)
    public float perspectiveSprintFactor = 1.5F;

    // ── Misc ────────────────────────────────────────────────────
    public boolean fly = true;
    public boolean slide = true;
    public boolean crawl = true;
    public boolean sneak = true;
    public boolean run = true;
    public boolean sprint = true;
    public boolean ceilingClimbing = true;

    // ── 활성화 플래그 ──────────────────────────────────────────
    /**
     * SM 활성화 상태. 원본 SmartMovingProperties.enabled (L33).
     * `update()` 호출 시 `toggler != -1` 로 파생 — 직접 쓰기 금지, toggle()/setCurrentKey 경유.
     */
    public boolean enabled = true;

    // ── Config key 토글 시스템 (원본 SmartMovingProperties L26-L31) ─────────────────
    /** 원본 SmartMovingProperties.Enabled — `getKey(0)` 이 null 일 때 반환되는 문자열. */
    public static final String CONFIG_KEY_ENABLED  = "enabled";
    /** 원본 SmartMovingProperties.Disabled — `getNextKey`/`setCurrentKey`/`hasKey` 에서 비활성 표시. */
    public static final String CONFIG_KEY_DISABLED = "disabled";
    /**
     * 원본 SmartMovingProperties._defaultKeys = new String[1] = {null}.
     * 단일 null 요소 — `keys[0] == null` 이면 단순 on/off 토글 모드.
     */
    private static final String[] DEFAULT_KEYS = new String[] { null };

    /**
     * 원본 SmartMovingProperties.keys (L31) 대응.
     * 현재 유효한 config key 배열. 초기값 {null} (단순 on/off) — `setKeys(...)` 호출로
     * 게임타입별 배열({"e","m","h"} / {"c"} 등) 로 교체. `initializeForGameIfNeccessary()`
     * 이식(F 섹션)에서 gameType 판정 후 setKeys 호출.
     */
    public String[] configKeys = DEFAULT_KEYS;

    /**
     * 원본 SmartMovingProperties.toggler (L30) 대응. 초기값 -2 (load 전 센티넬).
     * 상태표:
     *   -2 : 초기화 전 (load() 미호출)
     *   -1 : disabled (enabled = false)
     *   0..configKeys.length-1 : enabled, 현재 key 인덱스
     * 파생: `enabled = (toggler != -1)` — `update()` 에서 동기화. 직접 쓰기 금지, toggle()/
     *   setCurrentKey() 경유.
     */
    public int toggler = -2;

    // ── Singleton / Config 전환 ────────────────────────────────
    /** 클라이언트 파일 기반 설정 (Options). 불변 싱글톤. */
    public static final SmartMovingConfig INSTANCE = new SmartMovingConfig();

    /**
     * 서버 수신 설정 인스턴스. loadFromArray()로 갱신.
     * 연결 해제 시 resetServerConfig() 호출로 새 인스턴스 교체 — 원본 SmartMovingServerConfig.reset() 대응.
     * volatile — 클라이언트 렌더/틱 스레드 간 가시성 보장.
     */
    public static volatile SmartMovingConfig SERVER_CONFIG = new SmartMovingConfig();

    /**
     * 현재 활성 설정 참조 (원본: SmartMovingContext.Config).
     * 초기값: INSTANCE (클라이언트 자체 설정).
     * 서버 설정 수신 시: SERVER_CONFIG로 전환.
     * 서버 설정 해제 시: INSTANCE로 복원.
     * volatile — 클라이언트 렌더/틱 스레드 간 가시성 보장.
     */
    public static volatile SmartMovingConfig Config = INSTANCE;

    private static final String FILE_NAME = "smart_moving_options.properties";

    private SmartMovingConfig() {}

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        File file = configFile.toFile();
        if (!file.exists()) {
            save();
            return;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }
        INSTANCE.readFrom(props);
    }

    /**
     * 서버 수신 설정 인스턴스를 기본값으로 리셋한다.
     * 원본: SmartMovingServerConfig.reset() — properties/topProperties 저장소 clear.
     * 1.21.1: 단일 저장소 구조이므로 새 인스턴스로 교체하여 모든 필드를 기본값으로 복원.
     * 호출: 서버 연결 해제 시 (SmartMovingClient.DISCONNECT).
     */
    public static void resetServerConfig() {
        SERVER_CONFIG = new SmartMovingConfig();
    }

    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();
        INSTANCE.writeTo(props);
        try (FileOutputStream out = new FileOutputStream(configFile.toFile())) {
            props.store(out, "SmartMoving configuration");
        } catch (IOException ignored) {}
    }

    /**
     * 서버로부터 수신한 flat String[] 배열 [k1,v1,k2,v2,...] 을 이 인스턴스에 적용한다.
     * 원본: SmartMovingServerConfig.loadFromProperties(String[] content, boolean blockCode)
     * 패리티 수준: 홀수 길이 배열은 malformed로 간주하고 처리 중단.
     */
    public void loadFromArray(String[] content) {
        if (content.length % 2 != 0) return;
        Properties props = new Properties();
        for (int i = 0; i + 1 < content.length; i += 2) {
            props.setProperty(content[i], content[i + 1]);
        }
        readFrom(props);
    }

    /**
     * 이 인스턴스의 설정을 flat String[] [k1,v1,k2,v2,...] 배열로 직렬화한다.
     * 원본: SmartMovingServerOptions.writeToProperties() 정상 경로 (config_system.md M-12)
     * 서버가 클라이언트에게 ConfigContent 패킷으로 전송할 때 사용한다.
     */
    public String[] toArray() {
        return toArray(null);
    }

    /**
     * 플레이어별 속도 치환 포함 직렬화.
     * 원본: SmartMovingServerOptions.writeToProperties(mp, key) 내부 iteration 중
     *       `entry.setValue(config._speedUserExponent.getValueString(userExponent))` 로직.
     * username != null + playerSpeedExponents 에 값 존재 시 `move.speed.user.exponent` 키 값을
     * 해당 플레이어 개인 지수로 치환. 전역 전송(username=null)은 모든 플레이어 공통값.
     */
    public String[] toArray(String username) {
        Properties props = new Properties();
        writeTo(props);
        if (username != null) {
            Integer userExponent = playerSpeedExponents.get(username);
            if (userExponent != null) {
                props.setProperty("move.speed.user.exponent", String.valueOf(userExponent));
            }
        }
        String[] result = new String[props.size() * 2];
        int i = 0;
        for (String key : props.stringPropertyNames()) {
            result[i++] = key;
            result[i++] = props.getProperty(key);
        }
        return result;
    }

    private void readFrom(Properties p) {
        enabled                  = getBool(p,   "move.enabled",                   enabled);
        speedFactor              = getFloat(p,  "move.speed.factor",              speedFactor);
        speedUser                = getBool(p,   "move.speed.user",                speedUser);
        speedUserFactor          = getFloat(p,  "move.speed.user.factor",         speedUserFactor);
        speedUserExponent        = getInt(p,    "move.speed.user.exponent",       speedUserExponent);
        playerSpeedExponents     = parseIntegerMap(p.getProperty("move.speed.users.exponents"));
        vanillaStyle             = getBool(p,   "move.general.vanilla",           vanillaStyle);
        sneakFactor              = getFloat(p,  "move.sneak.factor",              sneakFactor);
        crawlFactor              = getFloat(p,  "move.crawl.factor",              crawlFactor);
        runFactor                = getFloat(p,  "move.run.factor",                runFactor);
        sprintFactor             = getFloat(p,  "move.sprint.factor",             sprintFactor);
        baseClimb                = getBool(p,   "move.climb.base",                baseClimb);
        freeClimb                = getBool(p,   "move.climb.free",                freeClimb);
        simpleClimb              = getBool(p,   "move.climb.simple",              simpleClimb);
        smartClimb               = getBool(p,   "move.climb.smart",               smartClimb);
        freeClimbingUpSpeedFactor   = getFloat(p, "move.climb.free.up.factor",   freeClimbingUpSpeedFactor);
        freeClimbingDownSpeedFactor = getFloat(p, "move.climb.free.down.factor", freeClimbingDownSpeedFactor);
        ceilingClimbingSpeedFactor = getFloat(p, "move.climb.ceiling.speed.factor", ceilingClimbingSpeedFactor);
        freeClimbFallDamageStartDistance = getFloat(p, "move.climb.free.fall.damage.start", freeClimbFallDamageStartDistance);
        freeClimbFallDamageFactor        = getFloat(p, "move.climb.free.fall.damage.factor", freeClimbFallDamageFactor);
        climbJumpBackHead        = getBool(p,   "move.climb.jump.back.head",      climbJumpBackHead);
        climbExhaustion          = getBool(p,   "move.climb.exhaustion",          climbExhaustion);
        climbExhaustionStart     = getFloat(p,  "move.climb.exhaustion.start",    climbExhaustionStart);
        climbExhaustionStop      = getFloat(p,  "move.climb.exhaustion.stop",     climbExhaustionStop);
        ceilingClimbExhaustion   = getBool(p,   "move.climb.ceiling.exhaustion",  ceilingClimbExhaustion);
        ceilingClimbExhaustionStart = getFloat(p, "move.climb.ceiling.exhaustion.start", ceilingClimbExhaustionStart);
        ceilingClimbExhaustionStop  = getFloat(p, "move.climb.ceiling.exhaustion.stop",  ceilingClimbExhaustionStop);
        swim                     = getBool(p,   "move.swim",                      swim);
        swimSpeedFactor          = getFloat(p,  "move.swim.speed.factor",         swimSpeedFactor);
        dive                     = getBool(p,   "move.dive",                      dive);
        diveSpeedFactor          = getFloat(p,  "move.dive.speed.factor",         diveSpeedFactor);
        swimDownOnSneak          = getBool(p,   "move.swim.down.sneak",           swimDownOnSneak);
        diveDownOnSneak          = getBool(p,   "move.dive.down.sneak",           diveDownOnSneak);
        wallUpJump                     = getBool(p,  "move.jump.wall",                     wallUpJump);
        wallHeadJump                   = getBool(p,  "move.jump.wall.head",                wallHeadJump);
        wallUpJumpFallMaximumDistance  = getFloat(p, "move.jump.wall.fall.maximum",        wallUpJumpFallMaximumDistance);
        wallHeadJumpFallMaximumDistance = getFloat(p, "move.jump.wall.head.fall.maximum",  wallHeadJumpFallMaximumDistance);
        wallUpJumpOrthogonalTolerance   = getFloat(p, "move.jump.wall.orthogonal.tolerance", wallUpJumpOrthogonalTolerance);
        wallUpJumpVerticalFactor       = getFloat(p, "move.jump.wall.vertical.factor",     wallUpJumpVerticalFactor);
        wallHeadJumpVerticalFactor     = getFloat(p, "move.jump.wall.head.vertical.factor", wallHeadJumpVerticalFactor);
        wallUpJumpHorizontalFactor     = getFloat(p, "move.jump.wall.horizontal.factor",   wallUpJumpHorizontalFactor);
        wallHeadJumpHorizontalFactor   = getFloat(p, "move.jump.wall.head.horizontal.factor", wallHeadJumpHorizontalFactor);
        jumpCharge               = getBool(p,   "move.jump.charge",               jumpCharge);
        jumpChargeMaximum        = getFloat(p,  "move.jump.charge.maximum",       jumpChargeMaximum);
        jumpChargeFactor         = getFloat(p,  "move.jump.charge.factor",        jumpChargeFactor);
        headJump                 = getBool(p,   "move.jump.head.charge",          headJump);
        headJumpControlFactor    = getFloat(p,  "move.forward.jump.control.factor", headJumpControlFactor);
        headJumpChargeMaximum    = getFloat(p,  "move.forward.jump.charge.maximum", headJumpChargeMaximum);
        angleJumpSide            = getBool(p,   "move.jump.angle.side",           angleJumpSide);
        angleJumpBack            = getBool(p,   "move.jump.angle.back",           angleJumpBack);
        angleJumpHorizontalFactor = getFloat(p, "move.jump.angle.horizontal.factor", angleJumpHorizontalFactor);
        angleJumpVerticalFactor  = getFloat(p,  "move.jump.angle.vertical.factor", angleJumpVerticalFactor);
        angleJumpDoubleClickTicks = getFloat(p, "move.jump.angle.double.click.ticks", angleJumpDoubleClickTicks);
        wallJumpDoubleClick      = getBool(p,   "move.jump.wall.double.click",    wallJumpDoubleClick);
        wallJumpDoubleClickTicks = getFloat(p,  "move.jump.wall.double.click.ticks", wallJumpDoubleClickTicks);
        slideSlipperinessFactor  = getFloat(p,  "move.slide.slipperiness.factor", slideSlipperinessFactor);
        slideParticlePeriodFactor = getFloat(p, "move.slide.particle.period.factor", slideParticlePeriodFactor);
        slidingSpeedStopFactor   = getFloat(p,  "move.slide.speed.stop.factor",  slidingSpeedStopFactor);
        flyingSpeedFactor        = getFloat(p,  "move.fly.speed.factor",          flyingSpeedFactor);
        flyControlVertical       = getBool(p,   "move.fly.control.vertical",      flyControlVertical);
        diveControlVertical      = getBool(p,   "move.dive.control.vertical",     diveControlVertical);
        flyCloseToGround         = getBool(p,   "move.fly.ground.close",          flyCloseToGround);
        flyWhileOnGround         = getBool(p,   "move.fly.ground.collide",        flyWhileOnGround);
        fly                      = getBool(p,   "move.fly",                       fly);
        slide                    = getBool(p,   "move.slide",                     slide);
        crawl                    = getBool(p,   "move.crawl",                     crawl);
        crawlOverEdge            = getBool(p,   "move.crawl.edge",                crawlOverEdge);
        crawlToggle              = getBool(p,   "move.crawl.toggle",              crawlToggle);
        sneakToggle              = getBool(p,   "move.sneak.toggle",              sneakToggle);
        sneakNameTag             = getBool(p,   "move.sneak.name",                sneakNameTag);
        crawlNameTag             = getBool(p,   "move.crawl.name",                crawlNameTag);
        displayExhaustionBar     = getBool(p,   "move.gui.exhaustion.bar",        displayExhaustionBar);
        displayJumpChargeBar     = getBool(p,   "move.gui.jump.charge.bar",       displayJumpChargeBar);
        sneak                    = getBool(p,   "move.sneak",                     sneak);
        run                      = getBool(p,   "move.run",                       run);
        sprint                   = getBool(p,   "move.sprint",                    sprint);
        ceilingClimbing          = getBool(p,   "move.climb.ceiling",             ceilingClimbing);
        perspectiveFadeFactor    = getFloat(p,  "move.perspective.fade.factor",   perspectiveFadeFactor);
        perspectiveSpeedFactor   = getFloat(p,  "move.perspective.speed.factor",  perspectiveSpeedFactor);
        perspectiveSpeedFactorMax= getFloat(p,  "move.perspective.speed.factor.max", perspectiveSpeedFactorMax);
        perspectiveRunFactor     = getFloat(p,  "move.perspective.run.factor",    perspectiveRunFactor);
        perspectiveSprintFactor  = getFloat(p,  "move.perspective.sprint.factor", perspectiveSprintFactor);
    }

    private void writeTo(Properties p) {
        p.setProperty("move.enabled",                    String.valueOf(enabled));
        p.setProperty("move.speed.factor",               String.valueOf(speedFactor));
        p.setProperty("move.speed.user",                 String.valueOf(speedUser));
        p.setProperty("move.speed.user.factor",          String.valueOf(speedUserFactor));
        p.setProperty("move.speed.user.exponent",        String.valueOf(speedUserExponent));
        p.setProperty("move.speed.users.exponents",      serializeIntegerMap(playerSpeedExponents));
        p.setProperty("move.general.vanilla",            String.valueOf(vanillaStyle));
        p.setProperty("move.sneak.factor",               String.valueOf(sneakFactor));
        p.setProperty("move.crawl.factor",               String.valueOf(crawlFactor));
        p.setProperty("move.run.factor",                 String.valueOf(runFactor));
        p.setProperty("move.sprint.factor",              String.valueOf(sprintFactor));
        p.setProperty("move.climb.base",                 String.valueOf(baseClimb));
        p.setProperty("move.climb.free",                 String.valueOf(freeClimb));
        p.setProperty("move.climb.simple",               String.valueOf(simpleClimb));
        p.setProperty("move.climb.smart",                String.valueOf(smartClimb));
        p.setProperty("move.climb.free.up.factor",       String.valueOf(freeClimbingUpSpeedFactor));
        p.setProperty("move.climb.free.down.factor",     String.valueOf(freeClimbingDownSpeedFactor));
        p.setProperty("move.climb.ceiling.speed.factor", String.valueOf(ceilingClimbingSpeedFactor));
        p.setProperty("move.climb.free.fall.damage.start", String.valueOf(freeClimbFallDamageStartDistance));
        p.setProperty("move.climb.free.fall.damage.factor", String.valueOf(freeClimbFallDamageFactor));
        p.setProperty("move.climb.jump.back.head",       String.valueOf(climbJumpBackHead));
        p.setProperty("move.climb.exhaustion",           String.valueOf(climbExhaustion));
        p.setProperty("move.climb.exhaustion.start",     String.valueOf(climbExhaustionStart));
        p.setProperty("move.climb.exhaustion.stop",      String.valueOf(climbExhaustionStop));
        p.setProperty("move.climb.ceiling.exhaustion",   String.valueOf(ceilingClimbExhaustion));
        p.setProperty("move.climb.ceiling.exhaustion.start", String.valueOf(ceilingClimbExhaustionStart));
        p.setProperty("move.climb.ceiling.exhaustion.stop",  String.valueOf(ceilingClimbExhaustionStop));
        p.setProperty("move.swim",                       String.valueOf(swim));
        p.setProperty("move.swim.speed.factor",          String.valueOf(swimSpeedFactor));
        p.setProperty("move.dive",                       String.valueOf(dive));
        p.setProperty("move.dive.speed.factor",          String.valueOf(diveSpeedFactor));
        p.setProperty("move.swim.down.sneak",            String.valueOf(swimDownOnSneak));
        p.setProperty("move.dive.down.sneak",            String.valueOf(diveDownOnSneak));
        p.setProperty("move.jump.wall",                      String.valueOf(wallUpJump));
        p.setProperty("move.jump.wall.head",                 String.valueOf(wallHeadJump));
        p.setProperty("move.jump.wall.fall.maximum",         String.valueOf(wallUpJumpFallMaximumDistance));
        p.setProperty("move.jump.wall.head.fall.maximum",    String.valueOf(wallHeadJumpFallMaximumDistance));
        p.setProperty("move.jump.wall.orthogonal.tolerance", String.valueOf(wallUpJumpOrthogonalTolerance));
        p.setProperty("move.jump.wall.vertical.factor",      String.valueOf(wallUpJumpVerticalFactor));
        p.setProperty("move.jump.wall.head.vertical.factor", String.valueOf(wallHeadJumpVerticalFactor));
        p.setProperty("move.jump.wall.horizontal.factor",    String.valueOf(wallUpJumpHorizontalFactor));
        p.setProperty("move.jump.wall.head.horizontal.factor", String.valueOf(wallHeadJumpHorizontalFactor));
        p.setProperty("move.jump.charge",                String.valueOf(jumpCharge));
        p.setProperty("move.jump.charge.maximum",        String.valueOf(jumpChargeMaximum));
        p.setProperty("move.jump.charge.factor",         String.valueOf(jumpChargeFactor));
        p.setProperty("move.jump.head.charge",           String.valueOf(headJump));
        p.setProperty("move.forward.jump.control.factor", String.valueOf(headJumpControlFactor));
        p.setProperty("move.forward.jump.charge.maximum", String.valueOf(headJumpChargeMaximum));
        p.setProperty("move.jump.angle.side",            String.valueOf(angleJumpSide));
        p.setProperty("move.jump.angle.back",            String.valueOf(angleJumpBack));
        p.setProperty("move.jump.angle.horizontal.factor", String.valueOf(angleJumpHorizontalFactor));
        p.setProperty("move.jump.angle.vertical.factor", String.valueOf(angleJumpVerticalFactor));
        p.setProperty("move.jump.angle.double.click.ticks", String.valueOf(angleJumpDoubleClickTicks));
        p.setProperty("move.jump.wall.double.click",     String.valueOf(wallJumpDoubleClick));
        p.setProperty("move.jump.wall.double.click.ticks", String.valueOf(wallJumpDoubleClickTicks));
        p.setProperty("move.slide.slipperiness.factor",  String.valueOf(slideSlipperinessFactor));
        p.setProperty("move.slide.particle.period.factor", String.valueOf(slideParticlePeriodFactor));
        p.setProperty("move.slide.speed.stop.factor",    String.valueOf(slidingSpeedStopFactor));
        p.setProperty("move.fly.speed.factor",           String.valueOf(flyingSpeedFactor));
        p.setProperty("move.fly.control.vertical",       String.valueOf(flyControlVertical));
        p.setProperty("move.dive.control.vertical",      String.valueOf(diveControlVertical));
        p.setProperty("move.fly.ground.close",           String.valueOf(flyCloseToGround));
        p.setProperty("move.fly.ground.collide",         String.valueOf(flyWhileOnGround));
        p.setProperty("move.fly",                        String.valueOf(fly));
        p.setProperty("move.slide",                      String.valueOf(slide));
        p.setProperty("move.crawl",                      String.valueOf(crawl));
        p.setProperty("move.crawl.edge",                 String.valueOf(crawlOverEdge));
        p.setProperty("move.crawl.toggle",               String.valueOf(crawlToggle));
        p.setProperty("move.sneak.toggle",               String.valueOf(sneakToggle));
        p.setProperty("move.sneak.name",                 String.valueOf(sneakNameTag));
        p.setProperty("move.crawl.name",                 String.valueOf(crawlNameTag));
        p.setProperty("move.gui.exhaustion.bar",         String.valueOf(displayExhaustionBar));
        p.setProperty("move.gui.jump.charge.bar",        String.valueOf(displayJumpChargeBar));
        p.setProperty("move.sneak",                      String.valueOf(sneak));
        p.setProperty("move.run",                        String.valueOf(run));
        p.setProperty("move.sprint",                     String.valueOf(sprint));
        p.setProperty("move.climb.ceiling",              String.valueOf(ceilingClimbing));
        p.setProperty("move.perspective.fade.factor",    String.valueOf(perspectiveFadeFactor));
        p.setProperty("move.perspective.speed.factor",   String.valueOf(perspectiveSpeedFactor));
        p.setProperty("move.perspective.speed.factor.max", String.valueOf(perspectiveSpeedFactorMax));
        p.setProperty("move.perspective.run.factor",     String.valueOf(perspectiveRunFactor));
        p.setProperty("move.perspective.sprint.factor",  String.valueOf(perspectiveSprintFactor));
    }

    /** getUserSpeedFactor() 공식: (1 + speedUserFactor)^speedUserExponent */
    public float getUserSpeedFactor() {
        // 원본: isUserSpeedAlwaysDefault() = !speedUser || speedUserFactor==1F
        if (!speedUser || speedUserFactor == 1F || speedUserExponent == 0) return 1F;
        return (float) Math.pow(1F + speedUserFactor, speedUserExponent);
    }

    public void toggle() {
        enabled = !enabled;
        save();
    }

    public void changeSpeed(int difference) {
        speedUserExponent += difference;
    }

    /**
     * 플레이어별 속도 지수 조정.
     * 원본: SmartMovingServerOptions.changeSingleSpeed(player, difference):
     *   Integer exp = _speedUsersExponents.get(username);
     *   if (exp == null) exp = _speedUserExponent.value;
     *   exp += diff;
     *   _speedUsersExponents.put(username, exp);
     *   saveToOptionsFile();
     * 개인 값이 없으면 전역 지수를 기준으로 시작.
     */
    public synchronized void changeSingleSpeed(String username, int difference) {
        Integer exponent = playerSpeedExponents.get(username);
        if (exponent == null) exponent = speedUserExponent;
        exponent += difference;
        playerSpeedExponents.put(username, exponent);
    }

    /** 현재 속도를 정수 퍼센트 문자열로 반환 (원본: SmartMovingOptions.getSpeedPercent()) */
    public String getSpeedPercent() {
        return String.valueOf((int)(getUserSpeedFactor() * 100));
    }

    private static float getFloat(Properties p, String key, float def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Float.parseFloat(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    /**
     * 원본: Value.tryParseIntegerMap(String value) (Value.md L555-568).
     * `,`으로 분할 → 짝수 인덱스=키, 홀수=값. parseInt 실패 항목은 스킵(원본은 NPE 가능).
     */
    private static Map<String, Integer> parseIntegerMap(String raw) {
        Map<String, Integer> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) return result;
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i++];
            if (i < parts.length) {
                try { result.put(key, Integer.parseInt(parts[i].trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    /** parseIntegerMap 역방향 — `"user1,5,user2,-2,..."` 형식 생성. */
    private static String serializeIntegerMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey()).append(',').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }
}
