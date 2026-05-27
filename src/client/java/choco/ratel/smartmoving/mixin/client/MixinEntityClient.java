package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
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
     * 5-8 (클라이언트) after:
     *   - 클라이밍 이동 거리 누적 (피로도 계산용)
     *   - 수영 소리 누적 (8-6)
     *
     * 원본: afterMoveEntity() — distanceSwom 누적, SwimSoundDistance 초과 시 소리 재생.
     * SwimSoundDistance = 1/0.7F ≈ 1.4286F (swim_dive.md 기록값).
     * 소리: volume=0.05F, pitch=1.0F ± rand*0.4F.
     *
     * 🔴 fix #73 2단계 (2026-05-10): STEP_HEIGHT=0 억제 / 복원 로직 일괄 제거.
     *   상세는 MixinEntity.sm_afterMove 주석 참조.
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void sm_afterMove_client(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;

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

        // 🔴 fix #14 revert (2026-05-08): mixin offset + sm_afterMove setPos 보정 매핑 시도 →
        //   vanilla `Entity.move` 의 collision 처리 (박스 발 = player.y +1m 기준 충돌 검사 →
        //   ground 도달 시 player.y = bb.minY - 1m = ground -1m → push out → 진동) 와 호환 X.
        //   원본 1:1 매핑 (박스 발 +1m + posY 변화 X) 를 1.21.1 매핑하려면 calculateBoundingBox
        //   가로채기 + setPos/prevY/lastRenderY/Camera baseline 동기화 등 복합 매핑 필요 — 별도
        //   사이클. 현재 setPos 보정은 비활성 + MixinEntity 의 POSE=SLIDING 가드로 mixin offset
        //   차단 → 박스 발 = player.y (STANDING 동일), 박스 머리 = player.y +0.8 (1m 낮음 = 위 작아짐).
        // if (sm.heightOffset != 0F && sm.isHeadJumping) {
        //     player.setPos(player.getX(), player.getY() + sm.heightOffset, player.getZ());
        // }

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

        // 🔴 fix #75 (BUG #2, 2026-05-10, 사용자 보고 "weeping/twisting vines + sneak 진동"):
        //   진동 사이클: pose toggle (STANDING ↔ CROUCHING) → 박스 height 토글 (1.8 ↔ 1.5)
        //     → vanilla move/collision 결과 onGround 토글 → 우리 sm_isSneaking 의
        //     `(isSlow && onGround)` 분기 결과 토글 → vanilla updatePose 가 isSneaking 따라
        //     pose 토글 → 무한 사이클.
        //   원본 SmartMovingSelf.isSneaking 식 자체는 1:1 매핑이지만 1.21.1 vanilla 의 박스
        //   height 변경 → onGround 토글 quirk + weeping/twisting vines 의 isHoldingOntoLadder
        //   특수 처리 결합으로 1.21.1 만 발생. 1.7.10 vanilla 에는 weeping/twisting vines 없음.
        //
        //   메모리 project_vine_animation_complete.md 의 의도 = "weeping/twisting vines 는
        //   vanilla 동작 그대로". sm_isSneaking 도 해당 시나리오에서 vanilla 위임 → vanilla 의
        //   안정된 SNEAKING flag (input.sneaking 그대로) 사용 → pose 안정 → 진동 X.
        net.minecraft.block.Block _blockAtPos = player.getWorld()
                .getBlockState(player.getBlockPos()).getBlock();
        if (_blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES
                || _blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES_PLANT
                || _blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES
                || _blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES_PLANT) {
            return;  // override skip → vanilla isSneaking 그대로
        }

        // 🔴 fix #83 (2026-05-12, 사용자 보고 "슬라이딩 시 화면 흔들림 + 발소리 + 비행 토글 가능"):
        //   1.21.1 vanilla bobbing/footstep/jump double-tap 비행 토글이 모두 isSneaking() 기반.
        //   원본 식 (= isSliding 가드 없음) 그대로 매핑하면 슬라이딩 시 isSneaking()=false →
        //   일반 walking 처리 → 사용자 보고 BUG.
        //   1.21.1 매핑에서는 isSliding 시 isSneaking()=true 강제 + clipAtLedge override (fix #84)
        //   로 step-back 부작용 차단.
        boolean result = (sm.isSlow && player.isOnGround())
                || (!cfg.sneak && sm.wouldIsSneaking && sm.jumpCharge > 0)
                || (!cfg.crawlOverEdge && sm.isCrawling && !sm.isClimbing)
                || sm.isSliding;
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
    // fix #175 v1/v2/v3 모두 시도 — inject 자체 활성 X (= dump FIX175-CHECK 매치 0). revert.
    // 다음 방향: main mixin (MixinEntity.sm_offsetBoundingBoxForFlying) 의 가드에 SM state 검사 추가.

    @Inject(method = "pushOutOfBlocks(DDD)V", at = @At("HEAD"), cancellable = true)
    private void sm_pushOutOfBlocks(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);
        if (sm.multiPlayerInitialized > 0) {
            sm.multiPlayerInitialized--;
            ci.cancel();
        }
    }

}
