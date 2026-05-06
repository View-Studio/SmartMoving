package choco.ratel.smartmoving.mixin.server;

import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 🔴 [TEMP DBG-7] BUG-7 진단 — server-side Entity.move + adjustMovementForCollisions + setPos
 * 모든 dump. ICC 활성 + EXIT 후 30 tick 한정.
 */
@Mixin(Entity.class)
public abstract class MixinServerEntityDbg {

    private static boolean sm_dbgActiveServer(Entity self) {
        if (!(self instanceof ServerPlayerEntity p)) return false;
        SmartMovingServer sm = SmartMovingServer.get(p);
        return sm.isClimbCrawling || sm.smIccDbgTicks > 0;
    }

    @Inject(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At("HEAD"))
    private void sm_dbgServerMoveHead(MovementType type, Vec3d vec, CallbackInfo ci) {
        Entity self = (Entity)(Object) this;
        if (!sm_dbgActiveServer(self)) return;
        org.slf4j.LoggerFactory.getLogger("SM-DBG7-S").info(
                "[S-move HEAD] name={} type={} vec=({}, {}, {}) y_before={}",
                self.getName().getString(), type,
                String.format("%.4f", vec.x), String.format("%.4f", vec.y), String.format("%.4f", vec.z),
                String.format("%.3f", self.getY())
        );
    }

    @Inject(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At("TAIL"))
    private void sm_dbgServerMoveTail(MovementType type, Vec3d vec, CallbackInfo ci) {
        Entity self = (Entity)(Object) this;
        if (!sm_dbgActiveServer(self)) return;
        org.slf4j.LoggerFactory.getLogger("SM-DBG7-S").info(
                "[S-move TAIL] name={} type={} y_after={}",
                self.getName().getString(), type,
                String.format("%.3f", self.getY())
        );
    }

    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"))
    private void sm_dbgServerAmfcHead(Vec3d motion, CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity)(Object) this;
        if (!sm_dbgActiveServer(self)) return;
        org.slf4j.LoggerFactory.getLogger("SM-DBG7-S").info(
                "[S-AMFC HEAD] name={} input=({}, {}, {}) bb=[{}..{}, {}..{}, {}..{}]",
                self.getName().getString(),
                String.format("%.4f", motion.x), String.format("%.4f", motion.y), String.format("%.4f", motion.z),
                String.format("%.3f", self.getBoundingBox().minX),
                String.format("%.3f", self.getBoundingBox().maxX),
                String.format("%.3f", self.getBoundingBox().minY),
                String.format("%.3f", self.getBoundingBox().maxY),
                String.format("%.3f", self.getBoundingBox().minZ),
                String.format("%.3f", self.getBoundingBox().maxZ)
        );
    }

    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("RETURN"))
    private void sm_dbgServerAmfcReturn(Vec3d motion, CallbackInfoReturnable<Vec3d> cir) {
        Entity self = (Entity)(Object) this;
        if (!sm_dbgActiveServer(self)) return;
        Vec3d output = cir.getReturnValue();
        boolean truncated = (Math.abs(motion.x - output.x) > 1e-4)
                || (Math.abs(motion.y - output.y) > 1e-4)
                || (Math.abs(motion.z - output.z) > 1e-4);
        org.slf4j.LoggerFactory.getLogger("SM-DBG7-S").info(
                "[S-AMFC RET] name={} output=({}, {}, {}) truncated={}",
                self.getName().getString(),
                String.format("%.4f", output.x), String.format("%.4f", output.y), String.format("%.4f", output.z),
                truncated
        );
    }

    @Inject(method = "setPosition(DDD)V", at = @At("HEAD"))
    private void sm_dbgServerSetPos(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity)(Object) this;
        if (!sm_dbgActiveServer(self)) return;
        // dx/dy/dz 중 하나라도 큰 변화 시만 dump.
        if (Math.abs(y - self.getY()) < 0.005
                && Math.abs(x - self.getX()) < 0.005
                && Math.abs(z - self.getZ()) < 0.005) return;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < Math.min(stack.length, 7); i++) {
            String cls = stack[i].getClassName();
            int dot = cls.lastIndexOf('.');
            sb.append(cls.substring(dot + 1)).append(".").append(stack[i].getMethodName());
            if (i < Math.min(stack.length, 7) - 1) sb.append(" ← ");
        }
        org.slf4j.LoggerFactory.getLogger("SM-DBG7-S").info(
                "[S-setPos HEAD] name={} | before=({}, {}, {}) new=({}, {}, {}) | d=({}, {}, {}) caller=[{}]",
                self.getName().getString(),
                String.format("%.3f", self.getX()),
                String.format("%.3f", self.getY()),
                String.format("%.3f", self.getZ()),
                String.format("%.3f", x),
                String.format("%.3f", y),
                String.format("%.3f", z),
                String.format("%.4f", x - self.getX()),
                String.format("%.4f", y - self.getY()),
                String.format("%.4f", z - self.getZ()),
                sb.toString()
        );
    }
}
