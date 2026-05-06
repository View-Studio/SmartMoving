package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 🔴 v26.18 (BUG-7 server tick 순서 race 진짜 root fix) — head/below ClimbableBlock 가드 추가.
 *
 * vanilla `MinecraftServer.tickWorlds` 정확 순서:
 *   1. playerManager.forEach(playerTick) → ServerPlayerEntity.playerTick → super.tick →
 *      PlayerEntity.tick → LivingEntity.tick → tickMovement → travel → 우리 inject.
 *   2. ServerWorld.tick (= entity tick + EntityTrackerEntry broadcast).
 *   3. networkIo.tick (= packet 처리, processStatePacket, onPlayerMove).
 *
 * ★★★ packet 처리가 우리 inject + broadcast 후. 즉 우리 inject 시점:
 *   - sm.isClimbCrawling 등 = 이전 server tick 의 packet 처리 결과 (= 1 tick lag).
 *   - server.player.x/y/z = 이전 server tick 의 PlayerMoveC2SPacket 결과.
 *
 * "가끔 됨" 의 진짜 root: client packet 도달 timing 운에 따라 server tick N-1 또는 N 의
 * networkIo 에 도달. N-1 도달 = server tick N 에 가드 통과. N 도달 = server tick N+1 까지 lag.
 *
 * v26.17 한계:
 *   - vanilla `player.isClimbing()` = player.getBlockPos() (= 발 위치) 의 BlockState 검사.
 *   - 사다리 끝 + 0.5m 위치 ICC 진입 시점 = floor(y) → AIR → isClimbing()=false → 가드 미통과.
 *   - self side `onClimbable` = SmartMovingClimber 의 hands(머리=y+1)/feet(y) 둘 다 검사. 더 넓음.
 *
 * v26.18 fix:
 *   - server-side 가드에 head/feet/below ClimbableBlock 검사 추가.
 *   - head BlockPos (y+1) ClimbableBlock 검사 → 사다리 끝 도달 시 cover.
 *   - below BlockPos (y-1) ClimbableBlock 검사 → 사다리 끝 통과 직후 frame cover.
 *   - 이 가드 = self side onClimbable 의 server-side substitute 더 정확.
 *
 * vanilla travel cancel + clamp + move 매핑은 v26.16 그대로 유지.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityServer {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void sm_iccVelClampServer(Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        // 🔴 v26.18 가드 확장: head/feet/below ClimbableBlock 검사 추가.
        //   - sm bit OR = SM relay packet 결과 (= 이전 server tick 의 networkIo 결과, lag 1 tick).
        //   - vanilla isClimbing() = 발 BlockPos 의 ClimbableBlock 검사 (즉시).
        //   - climbHead = 머리 BlockPos (y+1) ClimbableBlock 검사 (= 사다리 끝 도달 case cover).
        //   - climbBelow = 발 아래 BlockPos (y-1) ClimbableBlock 검사 (= 사다리 끝 통과 직후 case).
        boolean smBitClimb = sm.isClimbCrawling || sm.isClimbing || sm.isCrawlClimbing
                || sm.isCeilingClimbing;
        boolean vanillaClimb = player.isClimbing();
        boolean climbHead = false;
        boolean climbBelow = false;
        if (!smBitClimb && !vanillaClimb) {
            BlockPos head = BlockPos.ofFloored(player.getX(), player.getY() + 1.0, player.getZ());
            BlockPos below = BlockPos.ofFloored(player.getX(), player.getY() - 1.0, player.getZ());
            climbHead = player.getWorld().getBlockState(head).isIn(BlockTags.CLIMBABLE);
            climbBelow = player.getWorld().getBlockState(below).isIn(BlockTags.CLIMBABLE);
        }
        if (!(smBitClimb || vanillaClimb || climbHead || climbBelow)) return;

        // self side `MixinLivingEntityClient.sm_travel_client` L385-411 1:1.
        // 1) velocity clamp.
        Vec3d v = player.getVelocity();
        double clampH = 0.15D;
        double mx = Math.max(-clampH, Math.min(clampH, v.x));
        double mz = Math.max(-clampH, Math.min(clampH, v.z));
        double my = v.y;
        if (sm.isSneakButtonPressed && my < 0) {
            my = 0;
        } else if (my < -clampH) {
            my = -clampH;
        }
        if (mx != v.x || mz != v.z || my != v.y) {
            player.setVelocity(mx, my, mz);
        }
        player.fallDistance = 0;

        // 2) self side L409 동등: 우리 clamp velocity 로 직접 move (vanilla 의 추가 friction/
        //    gravity 적용 전 위치 갱신). collision 검사 진행.
        player.move(MovementType.SELF, player.getVelocity());

        // 3) self side L411 동등: vanilla travel 자체 cancel — 추가 motion 차단.
        ci.cancel();
    }
}
