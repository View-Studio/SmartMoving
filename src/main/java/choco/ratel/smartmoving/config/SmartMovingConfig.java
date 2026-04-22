package choco.ratel.smartmoving.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
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
    public float freeClimbingUpSpeedFactor   = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    public float freeClimbingDownSpeedFactor = 1.0F;  // PositiveFactor 기본값 1F (A-18 확인)
    public float ceilingClimbingSpeedFactor = 0.2F;
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

    // ── Jumping ─────────────────────────────────────────────────
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

    // ── Sliding ─────────────────────────────────────────────────
    // 원본: SmartMovingConfig._slideSlipperinessFactor (PositiveFactor, 기본값 1F)
    public float slideSlipperinessFactor = 1.0F;
    // 원본: SmartMovingConfig._slideParticlePeriodFactor (PositiveFactor, 기본값 0.5F)
    public float slideParticlePeriodFactor = 0.5F;
    // 원본: SmartMovingConfig._slidingSpeedStopFactor (PositiveFactor, 기본값 1F)
    public float slidingSpeedStopFactor = 1.0F;

    // ── Misc ────────────────────────────────────────────────────
    public boolean fly = true;
    public boolean slide = true;
    public boolean crawl = true;
    public boolean sneak = true;
    public boolean run = true;
    public boolean sprint = true;
    public boolean ceilingClimbing = true;

    // ── 활성화 플래그 ──────────────────────────────────────────
    /** SM 전체 on/off. toggle() 으로 반전 후 저장. */
    public boolean enabled = true;

    // ── Singleton / Config 전환 ────────────────────────────────
    /** 클라이언트 파일 기반 설정 (Options). 불변 싱글톤. */
    public static final SmartMovingConfig INSTANCE = new SmartMovingConfig();

    /** 서버 수신 설정 인스턴스. loadFromArray()로 갱신. */
    public static final SmartMovingConfig SERVER_CONFIG = new SmartMovingConfig();

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
        Properties props = new Properties();
        writeTo(props);
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
        vanillaStyle             = getBool(p,   "move.general.vanilla",           vanillaStyle);
        sneakFactor              = getFloat(p,  "move.sneak.factor",              sneakFactor);
        crawlFactor              = getFloat(p,  "move.crawl.factor",              crawlFactor);
        runFactor                = getFloat(p,  "move.run.factor",                runFactor);
        sprintFactor             = getFloat(p,  "move.sprint.factor",             sprintFactor);
        baseClimb                = getBool(p,   "move.climb.base",                baseClimb);
        freeClimb                = getBool(p,   "move.climb.free",                freeClimb);
        freeClimbingUpSpeedFactor   = getFloat(p, "move.climb.free.up.factor",   freeClimbingUpSpeedFactor);
        freeClimbingDownSpeedFactor = getFloat(p, "move.climb.free.down.factor", freeClimbingDownSpeedFactor);
        ceilingClimbingSpeedFactor = getFloat(p, "move.climb.ceiling.speed.factor", ceilingClimbingSpeedFactor);
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
        slideSlipperinessFactor  = getFloat(p,  "move.slide.slipperiness.factor", slideSlipperinessFactor);
        slideParticlePeriodFactor = getFloat(p, "move.slide.particle.period.factor", slideParticlePeriodFactor);
        slidingSpeedStopFactor   = getFloat(p,  "move.slide.speed.stop.factor",  slidingSpeedStopFactor);
        fly                      = getBool(p,   "move.fly",                       fly);
        slide                    = getBool(p,   "move.slide",                     slide);
        crawl                    = getBool(p,   "move.crawl",                     crawl);
        sneak                    = getBool(p,   "move.sneak",                     sneak);
        run                      = getBool(p,   "move.run",                       run);
        sprint                   = getBool(p,   "move.sprint",                    sprint);
        ceilingClimbing          = getBool(p,   "move.climb.ceiling",             ceilingClimbing);
    }

    private void writeTo(Properties p) {
        p.setProperty("move.enabled",                    String.valueOf(enabled));
        p.setProperty("move.speed.factor",               String.valueOf(speedFactor));
        p.setProperty("move.speed.user",                 String.valueOf(speedUser));
        p.setProperty("move.speed.user.factor",          String.valueOf(speedUserFactor));
        p.setProperty("move.speed.user.exponent",        String.valueOf(speedUserExponent));
        p.setProperty("move.general.vanilla",            String.valueOf(vanillaStyle));
        p.setProperty("move.sneak.factor",               String.valueOf(sneakFactor));
        p.setProperty("move.crawl.factor",               String.valueOf(crawlFactor));
        p.setProperty("move.run.factor",                 String.valueOf(runFactor));
        p.setProperty("move.sprint.factor",              String.valueOf(sprintFactor));
        p.setProperty("move.climb.base",                 String.valueOf(baseClimb));
        p.setProperty("move.climb.free",                 String.valueOf(freeClimb));
        p.setProperty("move.climb.free.up.factor",       String.valueOf(freeClimbingUpSpeedFactor));
        p.setProperty("move.climb.free.down.factor",     String.valueOf(freeClimbingDownSpeedFactor));
        p.setProperty("move.climb.ceiling.speed.factor", String.valueOf(ceilingClimbingSpeedFactor));
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
        p.setProperty("move.slide.slipperiness.factor",  String.valueOf(slideSlipperinessFactor));
        p.setProperty("move.slide.particle.period.factor", String.valueOf(slideParticlePeriodFactor));
        p.setProperty("move.slide.speed.stop.factor",    String.valueOf(slidingSpeedStopFactor));
        p.setProperty("move.fly",                        String.valueOf(fly));
        p.setProperty("move.slide",                      String.valueOf(slide));
        p.setProperty("move.crawl",                      String.valueOf(crawl));
        p.setProperty("move.sneak",                      String.valueOf(sneak));
        p.setProperty("move.run",                        String.valueOf(run));
        p.setProperty("move.sprint",                     String.valueOf(sprint));
        p.setProperty("move.climb.ceiling",              String.valueOf(ceilingClimbing));
    }

    /** getUserSpeedFactor() 공식: (1 + speedUserFactor)^speedUserExponent */
    public float getUserSpeedFactor() {
        if (!speedUser || speedUserExponent == 0) return 1F;
        return (float) Math.pow(1F + speedUserFactor, speedUserExponent);
    }

    public void toggle() {
        enabled = !enabled;
        save();
    }

    public void changeSpeed(int difference) {
        speedUserExponent += difference;
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
}
