package choco.ratel.smartmoving.state;

import choco.ratel.smartmoving.input.SmartMovingButton;
import choco.ratel.smartmoving.physics.CeilingClimbingHandler;
import choco.ratel.smartmoving.physics.ClimbingHandler;
import choco.ratel.smartmoving.physics.CrawlingHandler;
import choco.ratel.smartmoving.physics.FeetClimbing;
import choco.ratel.smartmoving.physics.HandsClimbing;
import choco.ratel.smartmoving.physics.SlidingHandler;
import choco.ratel.smartmoving.physics.JumpHandler;
import choco.ratel.smartmoving.physics.SwimmingHandler;
import net.minecraft.entity.player.PlayerEntity;

public class SmartMovingState {

    // ── Boolean 이동 출력 상태 ──
    public boolean isCrawling, wasCrawling;
    public boolean isClimbing, wasClimbing;
    public boolean isCrawlClimbing, isClimbCrawling;
    public boolean isSwimming, isDiving, isDipping;
    public boolean isSliding, isRopeSliding;
    public boolean isCeilingClimbing;
    public boolean isHeadJumping, isSprintJump, isWallJumping;
    public boolean isHandsVineClimbing, isFeetVineClimbing;
    public boolean isLevitating, isAerodynamic;
    public boolean isFast, isSlow;
    public boolean isGroundSprinting;

    // ── Boolean 내부 의도 상태 ──
    public boolean wantClimbUp, wantClimbDown, wantClimbCeiling;
    public boolean wantCrawlNotClimb, wouldIsSneaking;
    public boolean isClimbingStill, isClimbHolding;
    public boolean crawlToggled, sneakToggled;
    public boolean blockJumpTillButtonRelease;

    // ── Integer 카운터/타입 ──
    public int angleJumpType;
    public int handsEdgeMeta, feetEdgeMeta;
    // FeetClimbing.value / HandsClimbing.value (64비트 패킷 인코딩용)
    public int feetClimbingType  = FeetClimbing.None.value;
    public int handsClimbingType = HandsClimbing.None.value;
    public int leftJumpCount, rightJumpCount, backJumpCount, wallJumpCount;
    public int collidedHorizontallyTickCount;
    public int updateCounter;

    // ── Float 물리 값 ──
    public float exhaustion;
    public float maxExhaustionForAction;
    public float maxExhaustionToStartAction;
    public float jumpCharge, headJumpCharge;
    public float dippingDepth;
    public float horizontalCollisionAngle = Float.NaN;
    public float fadingPerspectiveFactor;
    public float heightOffset;

    // ── 이전 프레임 추적 ──
    public double prevMotionX, prevMotionY, prevMotionZ;
    public boolean wasOnGround;

    // ── 입력 버튼 (클라이언트에서 매 틱 갱신) ──
    public final SmartMovingButton forwardButton = new SmartMovingButton();
    public final SmartMovingButton backButton    = new SmartMovingButton();
    public final SmartMovingButton leftButton    = new SmartMovingButton();
    public final SmartMovingButton rightButton   = new SmartMovingButton();
    public final SmartMovingButton jumpButton    = new SmartMovingButton();
    public final SmartMovingButton sprintButton  = new SmartMovingButton();
    public final SmartMovingButton sneakButton   = new SmartMovingButton();
    public final SmartMovingButton grabButton    = new SmartMovingButton();

    public void reset() {
        isCrawling = wasCrawling = false;
        isClimbing = wasClimbing = false;
        isCrawlClimbing = isClimbCrawling = false;
        isSwimming = isDiving = isDipping = false;
        isSliding = isRopeSliding = false;
        isCeilingClimbing = false;
        isHeadJumping = isSprintJump = isWallJumping = false;
        isHandsVineClimbing = isFeetVineClimbing = false;
        isLevitating = isAerodynamic = false;
        isFast = isSlow = false;
        isGroundSprinting = false;

        wantClimbUp = wantClimbDown = wantClimbCeiling = false;
        wantCrawlNotClimb = wouldIsSneaking = false;
        isClimbingStill = isClimbHolding = false;
        crawlToggled = sneakToggled = false;
        blockJumpTillButtonRelease = false;

        angleJumpType = 0;
        handsEdgeMeta = feetEdgeMeta = 0;
        leftJumpCount = rightJumpCount = backJumpCount = wallJumpCount = 0;
        collidedHorizontallyTickCount = 0;
        updateCounter = 0;
        feetClimbingType  = FeetClimbing.None.value;
        handsClimbingType = HandsClimbing.None.value;

        exhaustion = maxExhaustionForAction = maxExhaustionToStartAction = 0F;
        jumpCharge = headJumpCharge = 0F;
        dippingDepth = 0F;
        horizontalCollisionAngle = Float.NaN;
        fadingPerspectiveFactor = 0F;
        heightOffset = 0F;

        prevMotionX = prevMotionY = prevMotionZ = 0D;
        wasOnGround = false;

        forwardButton.reset();
        backButton.reset();
        leftButton.reset();
        rightButton.reset();
        jumpButton.reset();
        sprintButton.reset();
        sneakButton.reset();
        grabButton.reset();
    }

    public boolean isSmall() {
        return isCrawling || isSliding || isCeilingClimbing;
    }

    public void tick(PlayerEntity player) {
        updateCounter++;
        // 매 틱 시작 시 플래그 초기화 (각 핸들러가 재설정)
        isSlow = false;
        isFast = false;

        // 이전 프레임 추적
        wasOnGround = player.isOnGround();
        isGroundSprinting = player.isSprinting() && player.isOnGround();

        SwimmingHandler.update(this, player);
        CrawlingHandler.update(this, player);
        ClimbingHandler.update(this, player);
        CeilingClimbingHandler.update(this, player);
        SlidingHandler.update(this, player);
        JumpHandler.update(this, player);
    }
}
