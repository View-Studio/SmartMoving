package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 5-8 (클라이언트): move() HEAD/TAIL 훅 — STEP_HEIGHT 억제/복원 + 이동 거리 누적.
 * 8-6: 수영 소리 누적 — distanceSwom 기반 재생.
 *
 * MixinEntity (main) 의 서버 측 구현에 대응하는 클라이언트 측 구현.
 */
@Mixin(Entity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinEntityClient {

    /**
     * 5-8: beforeMove에서 STEP_HEIGHT=0으로 설정했을 때 원래 값을 저장한다.
     * -1.0 = 미설정.
     */
    @Unique
    private double sm_savedStepHeight_client = -1.0;

    /**
     * 5-8 (클라이언트) before: 크롤링/천장 클라이밍 중 STEP_HEIGHT=0 억제.
     * 원본 ySize=0 에 해당.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void sm_beforeMove_client(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isCrawling || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (attr != null) {
                sm_savedStepHeight_client = attr.getBaseValue();
                attr.setBaseValue(0.0);
            }
        }
    }

    /**
     * 5-8 (클라이언트) after:
     *   - STEP_HEIGHT 복원
     *   - 클라이밍 이동 거리 누적 (피로도 계산용)
     *   - 수영 소리 누적 (8-6)
     *
     * 원본: afterMoveEntity() — distanceSwom 누적, SwimSoundDistance 초과 시 소리 재생.
     * SwimSoundDistance = 1/0.7F ≈ 1.4286F (swim_dive.md 기록값).
     * 소리: volume=0.05F, pitch=1.0F ± rand*0.4F.
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void sm_afterMove_client(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;

        // STEP_HEIGHT 복원
        if (sm_savedStepHeight_client >= 0) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (attr != null) attr.setBaseValue(sm_savedStepHeight_client);
            sm_savedStepHeight_client = -1.0;
        }

        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // C-25: SmartStatistics 갱신 — prevX/Y/Z는 tickMovement HEAD에서 저장된 이전 위치
        sm.stats.calculate(player.prevX, player.prevY, player.prevZ,
                           player.getX(), player.getY(), player.getZ());

        // 클라이밍 이동 거리 누적
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            sm.distanceClimbedModified += movement.length() * (sm.isClimbing ? 1.2 : 0.9);
        }

        // 헤드점프 heightOffset 위치 보정 (C-20)
        // 원본: afterMoveEntity() — setPosition(x, y - heightOffset, z)
        // heightOffset = -1F 시: y - (-1F) = y + 1F → 플레이어를 1블록 위로 보정
        if (sm.heightOffset != 0F) {
            player.setPos(player.getX(), player.getY() - sm.heightOffset, player.getZ());
        }

        // 수영 소리 누적 (8-6)
        // isSwimming_sm: SM 수면 수영 상태 (vanilla isSwimming()과 구별하기 위해 필드명 구분)
        if (sm.isSwimming_sm) {
            sm.distanceSwom += movement.horizontalLength();
            // SwimSoundDistance = 1/0.7F (swim_dive.md: "distanceSwom > SwimSoundDistance (= 1/0.7F ≈ 1.4286F)")
            final float SWIM_SOUND_DISTANCE = 1.0F / 0.7F;
            if (sm.distanceSwom > SWIM_SOUND_DISTANCE) {
                sm.distanceSwom -= SWIM_SOUND_DISTANCE;
                // 원본 소리: "random.splash" → 1.21.1: SoundEvents.ENTITY_PLAYER_SWIM
                // volume=0.05F, pitch=1.0F ± rand*0.4F (swim_dive.md 기록값)
                player.playSound(
                        SoundEvents.ENTITY_PLAYER_SWIM,
                        0.05F,
                        1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.4F
                );
            }
        }
    }
}
