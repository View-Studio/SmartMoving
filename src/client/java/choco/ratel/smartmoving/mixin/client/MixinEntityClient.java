package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        // BUG-24 (세션 36): SM disabled 시 STEP_HEIGHT 변경 안 함 → vanilla 정상.
        if (!SmartMovingConfig.Config.enabled) return;
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

        // STEP_HEIGHT 복원 (cfg.enabled 무관 — 이전 SM enabled 시 저장된 값 복원 보장)
        if (sm_savedStepHeight_client >= 0) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (attr != null) attr.setBaseValue(sm_savedStepHeight_client);
            sm_savedStepHeight_client = -1.0;
        }

        // BUG-24 (세션 36): SM disabled 시 SmartStatistics 갱신 + heightOffset 보정 + climb 거리
        //   누적 + swim 소리 모두 skip → vanilla 정상. 잔존 sm.heightOffset 으로 인한 player.setPos
        //   영향 차단 (비행 진입 뚜둑 가능 원인 1).
        if (!SmartMovingConfig.Config.enabled) return;

        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // C-25: SmartStatistics 갱신 — prevX/Y/Z는 tickMovement HEAD에서 저장된 이전 위치
        // yaw: 원본 SmartRenderRender.currentCameraAngle = rotationYaw / RadiantToAngle 용
        sm.stats.calculate(player.prevX, player.prevY, player.prevZ,
                           player.getX(), player.getY(), player.getZ(),
                           player.getYaw());

        // 클라이밍 이동 거리 누적
        if (sm.isClimbing || sm.isCrawlClimbing || sm.isCeilingClimbing) {
            sm.distanceClimbedModified += movement.length() * (sm.isClimbing ? 1.2 : 0.9);
        }

        // 헤드점프 heightOffset 위치 보정 (C-20)
        // 원본: afterMoveEntity() — setPosition(x, y - heightOffset, z)
        // heightOffset = -1F 시: y - (-1F) = y + 1F → 플레이어를 1블록 위로 보정
        // 🔴 BUG-28/29 동일 패턴 (사다리 등반 grab+sneak 보고): isClimbCrawling/isCrawling/
        //   isSliding 등 진입 엣지의 player.move() 호출 시 이 TAIL inject 발동 → setPos
        //   y+1 강제 → "플레이어가 위로 쑥 올라감" (사용자 보고).
        //   원본 setHeightOffset(-1F) 는 박스만 변경, player 위치 변경 X. setPos 보정은 head
        //   jump 전용 (C-20 의도). 다른 자세는 sm_getBaseDimensions_client 의 dimensions 변경
        //   만으로 박스 처리 (1.21.1 vanilla 자동 갱신). isHeadJumping 만 가드.
        if (sm.heightOffset != 0F && sm.isHeadJumping) {
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

    /**
     * isSneaking() 오버라이드 — SM 상태 기반 스니킹 판정.
     * 원본: SmartMovingSelf.isSneaking() (SmartMovingPlayerBase.java, 1972-1981줄)
     *
     * isSneaking()은 Entity에 정의됨 — LivingEntity/PlayerEntity 오버라이드 없음, Entity Mixin에서 처리.
     * Config 비활성 또는 탈것 탑승 시 vanilla에 위임.
     */
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void sm_isSneaking(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        if (!cfg.enabled || player.hasVehicle()) return;

        if (sm.forceIsSneaking != null) { cir.setReturnValue(sm.forceIsSneaking); return; }

        boolean result = (sm.isSlow && player.isOnGround())
                || (!cfg.sneak && sm.wouldIsSneaking && sm.jumpCharge > 0)
                || (!cfg.crawlOverEdge && sm.isCrawling && !sm.isClimbing);
        cir.setReturnValue(result);
    }

    /**
     * 🔴 사다리/덩굴 등반 + W + sneak 시 SNEAKING flag 설정 차단.
     *
     * vanilla 1.21.1 자세 결정 (CROUCHING) 메커니즘이 SNEAKING flag 또는 input.sneaking
     * 직접 사용 (정확한 method mapping 다름) → 우리 isSneaking() override 만으로 부족.
     *
     * Entity.setSneaking(boolean) 은 매 틱 vanilla 가 input.sneaking 으로 호출 →
     * SNEAKING flag (DataTracker) 설정. 이 flag 자체를 차단하면 자세 결정 + isSneaking()
     * + 모든 sneak 영향 일괄 차단.
     *
     * 사용자 의도: "사다리 + W + sneak → 아무 동작 없이 그냥 등반".
     *   - sm.isClimbing && forward > 0 && sneaking=true → setSneaking(true) 차단.
     *   - 그 외 모든 케이스: vanilla 정상 처리.
     */
    @Inject(method = "setSneaking", at = @At("HEAD"), cancellable = true)
    private void sm_setSneaking(boolean sneaking, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.isClimbing && player.input.movementForward > 0F && sneaking) {
            ci.cancel();
        }
    }

    /**
     * pushOutOfBlocks: 서버→클라이언트 위치 동기화 직후 억제.
     * 원본: SmartMovingSelf.pushOutOfBlocks (SmartMovingSelf.md L1263-1277)
     *
     * multiPlayerInitialized > 0이면 실행 취소 후 카운터 1 감소.
     * 1.21.1: Entity.pushOutOfBlocks(double,double,double) — Entity에 정의됨.
     */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void sm_pushOutOfBlocks(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.multiPlayerInitialized > 0) {
            sm.multiPlayerInitialized--;
            ci.cancel();
        }
    }

}
