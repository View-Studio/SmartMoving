package choco.ratel.smartmoving.mixin;

import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.server.SmartMovingServer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (서버): getBaseDimensions() 커스텀 EntityDimensions 반환.
 * 6-4 (서버): updatePose() Mixin — SM 상태별 포즈 강제 설정.
 *
 * LivingEntity.getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 * Yarn: updatePose (intermediary: method_7318 확인 완료)
 *
 * 클라이언트 측 → MixinPlayerEntityClient.sm_getBaseDimensions_client(), sm_updatePose_client()
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    /**
     * 6-1 (서버): SM 포즈별 커스텀 EntityDimensions 반환.
     *
     * **포커스 #2.7 Phase C 재작성 (세션 4, 2026-04-25)**: 클라와 완전 대칭 (옵션 3).
     *   원본 `setHeightOffset(-1F)` (SmartMovingSelf L1694-L1704) → `sp.height = 1.8F + (-1F)
     *   = 0.8F` + `getEyeHeight() = height - 0.18F = 0.62F` (ServerPlayerBase L142-L145).
     *   → 모든 small 상태 (isCrawling/isCrawlClimbing/isHeadJumping/isSliding/isSwimming/
     *   isDiving/isFlying/isLevitating) 통합 0.6×0.8 + 0.62F.
     *
     * 클라 (MixinPlayerEntityClient.sm_getBaseDimensions_client) 와 동일 8 SM OR 분기.
     * isFlying 은 vanilla `abilities.flying + cfg.fly + !isSwimming + !isDiving` 원본 공식
     * (SmartMovingSelf L2509-L2515) 1:1.
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_server(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): 비행/Levitate 분기 제거 (서버측).
        //   클라측 sm_getBaseDimensions_client 와 동일 정정 — 비행 시 vanilla STANDING 통과.
        //   원본 1.7.10 vanilla 에 EntityPose 시스템 없음 → 비행 시 STANDING 모델 + eyeHeight
        //   1.62 그대로. 1.21.1 잘못 매핑 = SLIDING 강제 → 카메라 발끝.
        //   클라/서버 대칭 유지: 둘 다 비행 시 vanilla STANDING POSE 통과 → datatracker 일관.
        //
        // 원본 setHeightOffset(-1F) 상태 전수 — Crawling/Sliding/Swimming 등만 0.6 × 0.8 +
        // eyeHeight 0.62F. isFlying/isLevitating 은 vanilla 통과.
        // 🔴 isClimbCrawling 분기 추가 (사용자 보고 fix — X/Z 땡김 server reconcile 차단):
        //   ICC 활성 중 클라 dim eyeHeight=1.62 (mixin offset 박스 +1 적용) ↔ 서버 vanilla
        //   dim 1.8 (mixin offset 미적용) → 박스 1m Y 차이 → 서버가 박스 ladder collision
        //   안 → 차단 → server position correction packet → 클라 위치 reset.
        //   클라 sm_getBaseDimensions_client 와 동일한 isClimbCrawling 분기 추가 → 서버 박스
        //   클라와 동일 (entity.y+1, entity.y+1.8) → 서버 reconcile 발생 X.
        // 🔴 isCrawlClimbing ICC 분기 추가 시도 revert (2026-05-03):
        //   클라 sm_getBaseDimensions_client 와 동일 — isCrawlClimbing 시 박스 +1m mixin offset
        //   발동 → ladder grip 영역 벗어남 → 의도치 않은 ICC 발동. 원본 1:1 = smSmall dim
        //   (0.8, 0.62) 적용 (원본 setHeightOffset 호출 없음 → 이전 isCrawling 박스 잔존).
        if (sm.isClimbCrawling) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔴 isHeadJumping 별도 분기 (2026-05-08, 다단계 fix 후 정착) — 클라 측과 1:1 대칭:
        //   1차 dim (0.6, 0.8, 1.62F) → MixinEntity offset 가드 매치 → 박스 +1m → 무한 루프 BUG.
        //   2차 dim (0.6, **1.8**, 1.62F) → height STANDING → offset 가드 미매치 → 박스 STANDING.
        //   클라 (MixinPlayerEntityClient) 와 동일 매핑 — server reconcile 차단.
        if (sm.isHeadJumping) {
            // 🔴 fix #13 (2026-05-08): 클라 측과 동일 매핑 — dim 0.6×0.8 + eyeHeight 1.62F.
            //   MixinEntity.sm_offsetBoundingBoxForFlying 의 POSE=SLIDING 가드로 mixin offset 차단.
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔴 fix #59 (2026-05-10, dump 분석 결과 — 사용자 보고 "여우무빙 시 EYEHEIGHT 가 콜리전
        //   아래에 설정"):
        //   isSliding 시 fix #56 의 POSE.SLIDING 가드로 mixin offset 활성 → 박스 +1m up
        //   (= bb.minY = entity.y + 1m). 그러나 dim eyeHeight=0.62 잔존 → 카메라 = entity.y +
        //   0.62 → 박스 (entity.y+1, entity.y+1.8) 아래 0.38m.
        //   1.7.10/1.12.2 의 setHeightOffset(-1) 효과 = boundingBox +1m up + height 0.8 +
        //   eyeHeight 변경 X (= STANDING 1.62 잔존, position 변경 X 매핑).
        //   해결: isSliding 시 eyeHeight=1.62 (= STANDING) → 카메라 = entity.y + 1.62 →
        //   박스 안 (= entity.y+1 ~ entity.y+1.8 안에 entity.y+1.62) 정상.
        if (sm.isSliding) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔵 (2026-05-20, Phase 0 검증 fix #102) swim/dive 별도 분기 — 클라 매핑 1:1 대칭.
        //   원본 1.7.10 `setHeightOffset(-1F)` 효과 = 박스 +1m up + eyeHeight 1.62 STANDING 유지.
        //   client side `sm_getBaseDimensions_client` 의 swim/dive 분기 변경과 1:1 동기화 필수
        //   (= server reconcile + multi remote bb 정합).
        if (sm.isSwimming || sm.isDiving) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔴 v26.12 — v26.5 가드 폐기 (client/server 양쪽 동기화).
        //   STANDING dim fall through BUG root. Fix 7 의 의도 (= smSmall 에 isCrawlClimbing 추가)
        //   1:1 복원. 사용자 보고 "STANDING → 엎드림" transition 차단.
        boolean smSmall = sm.isCrawling || sm.isCrawlClimbing;
        if (smSmall) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // 🔴 비행 콜리전 원본 1:1 복원 시도 (사용자 요청, 단계 1):
        //   원본 setHeightOffset(-1F) (L1694-L1704) → height 0.8, eyeHeight 변경 없음 (1.62).
        //   1.21.1 매핑: dimensions = (0.6, 0.8, eyeHeight=1.62).
        //   서버 측 isFlying 공식 (원본 L2510): cfg.fly && abilities.flying && !swim && !dive.
        //   클라/서버 hitbox 일관 위해 양측 동일 적용. instanceof ServerPlayerEntity 가드는
        //   메서드 진입부에 이미 있음 — player 변수 사용 가능.
        boolean serverIsFlying = SmartMovingConfig.Config.fly
                              && player.getAbilities().flying
                              && !sm.isSwimming
                              && !sm.isDiving;
        if (serverIsFlying || sm.isLevitating) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }

        // SLIDING POSE 가 vanilla 가 아닌 경로로 들어온 경우 보강 (외부 모드 등).
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // 🔴 orphan SWIMMING pose 가드 (2026-05-04 사용자 보고 — "1칸 진입 직전 슬라이딩 풀면
        //   가끔 콜리전 정상 엎드리기보다 작음"):
        //   클라 sm_getBaseDimensions_client 와 동일 가드 — pose=SWIMMING + smSmall 미매치 시
        //   STANDING dim 강제. 클라/서버 대칭으로 박스 sync 일관 유지 (server reconcile 차단).
        if (pose == EntityPose.SWIMMING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.8F).withEyeHeight(1.62F));
        }
    }

    /**
     * 6-4 (서버): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * **포커스 #2.7 Phase D-1 (세션 4, 2026-04-25)**: 클라와 완전 대칭 (옵션 3).
     *   클라 (MixinPlayerEntityClient.sm_updatePose_client) 와 동일 4 분기 매핑:
     *     isCrawling || isCrawlClimbing            → SWIMMING (엎드림)
     *     isHeadJumping || isSliding               → SLIDING  (납작)
     *     isSwimming || isDiving                   → SWIMMING (수영)
     *     isFlying (원본 공식) || isLevitating     → SLIDING  (비행)
     *     이외                                     → vanilla 통과
     *
     * 클라/서버 POSE 동일 sync → datatracker 진동 0.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_server(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);

        if (sm.isCrawling || sm.isCrawlClimbing) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isHeadJumping || sm.isSliding) {
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        } else if (sm.isSwimming || sm.isDiving) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): 비행/Levitate 분기 제거 (서버측).
        //   클라측 sm_updatePose_client 와 동일 정정 — 비행 시 vanilla STANDING POSE 통과.
        //   원본 1.7.10 vanilla 에 POSE 시스템 없음 → 비행 시 STANDING 그대로. 1.21.1 잘못 매핑
        //   = SLIDING 강제 → 카메라 발끝. 정정: vanilla 통과 → STANDING POSE → eyeHeight 1.62.
        //   클라/서버 대칭 유지 → datatracker 진동 0.
        // isFlying/isLevitating/isDipping/그 외 → vanilla updatePose() 통과 (STANDING 등)
    }

    /**
     * 낙하 중 sneak pose 차단 — vanilla updatePose() 의 isSneaking() 호출만 redirect.
     *
     * 원본 1.7.10 1:1 근거 (SmartMovingModel.animateSneaking L681-685):
     *   `if(isStandard && !isAngleJumping) imp.superAnimateSneaking(...);`
     *   isFalling 분기 진입 시 isStandard=false → vanilla sneak 시각 적용 SKIP.
     *
     * 1.21.1 EntityPose 시스템 차이:
     *   1.7.10 vanilla 는 sneak 시 boundingBox 변경 없음 + eyeHeight 0.08 차감만 → 떨림 미미.
     *   1.21.1 vanilla 는 pose=CROUCHING → boundingBox.height 1.5 (vs 1.8) + eyeHeight 1.27
     *     (vs 1.62) → 카메라/hitbox 토글 시 lerp 점프 발생.
     *
     * 해결: PlayerEntity.updatePose 의 isSneaking() 호출 (bytecode offset 68) 만 redirect →
     *   낙하 중에는 false 반환 → CROUCHING 분기 (offset 67-88) skip → STANDING fallthrough.
     *   vanilla 의 다른 분기 (FallFlying/Sleeping/Swimming/SpinAttack) 우선순위는 그대로.
     *
     * 위치: main mixin set 이라 client + server 양측 자동 적용 (instanceof 가드 불필요).
     *   - 클라: pose=STANDING → hitbox/eye height 토글 없음 + setModelPose 가 model.sneaking=false
     *     자동 set → BipedEntityModel.setAngles 의 sneak 분기 자동 SKIP.
     *   - 서버: pose=STANDING → 클라/서버 hitbox 동기화 → 좁은 천장 통과 등 위치 reject 회피.
     *
     * SM 상태 (Crawling/CrawlClimbing/HeadJumping/Sliding/Swimming/Diving) 는 sm_updatePose_*
     * 가 이미 ci.cancel() 후 setPose() 처리 → vanilla 본체 미실행 → 이 redirect 영향 없음.
     */
    @Redirect(method = "updatePose",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/entity/player/PlayerEntity;isSneaking()Z"))
    private boolean sm_redirectIsSneakingForFallingPose(PlayerEntity self) {
        boolean original = self.isSneaking();
        if (!original) return false;
        if (!SmartMovingConfig.Config.enabled) return original;

        // isFalling 가드 (sm_setAnglesHead 와 동일 조건):
        //   !onGround && fallDistance > fallAnimationDistanceMinimum && !touchingWater &&
        //   !abilities.flying.
        // SM 상태 (isClimbing 등) 는 SmartMovingClientState/Server 에 분리 저장이라 양측
        // 직접 참조 불가. 그러나 climbing 중에는 SmartMovingClimber 가 fallDistance=0 으로
        // reset 하므로 fallDistance > minimum 조건이 자연스레 climbing 제외.
        boolean isFalling = !self.isOnGround()
                && self.fallDistance > SmartMovingConfig.Config.fallAnimationDistanceMinimum
                && !self.isTouchingWater()
                && !self.getAbilities().flying;
        if (isFalling) return false;

        // 🔴 fix #77 (BUG #2.6, 2026-05-10, 사용자 보고 "weeping/twisting vines + sneak 시 박스 작아짐"):
        //   일반 사다리/덩굴 + sneak: sm_resetSneakInClimb 가드 (`sm.isClimbing` 등) 매치 →
        //     setPose(STANDING) 강제 → 박스 1.8m 유지. 매달림 = SM 자체 motion 처리.
        //   weeping/twisting vines (vanilla bypass = sm.isClimbing=false): sm_resetSneakInClimb 미트리거
        //     → fix #75 후 vanilla isSneaking()=true → vanilla updatePose → CROUCHING → 박스 1.5m.
        //
        //   해결: vanilla updatePose 안 isSneaking() 호출만 redirect → false 반환 → CROUCHING 분기
        //     skip → STANDING fallthrough. isHoldingOntoLadder() = isSneaking() 호출은 redirect 영향
        //     X (이건 vanilla 의 다른 메서드 안에서 호출) → 매달림 유지 (= 기능 보존).
        //   메모리 project_vine_animation_complete.md 의 "weeping/twisting vines = 사다리 자세
        //     + vanilla 매달림 motion" 의도와 정합.
        //   이 redirect 는 PlayerEntity (main mixin) 라 client + server 양측 자동 적용.
        net.minecraft.block.Block blockAtPos = self.getWorld()
                .getBlockState(self.getBlockPos()).getBlock();
        if (blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES
                || blockAtPos == net.minecraft.block.Blocks.WEEPING_VINES_PLANT
                || blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES
                || blockAtPos == net.minecraft.block.Blocks.TWISTING_VINES_PLANT) {
            return false;
        }

        return original;
    }

    /**
     * 3-3: addExhaustion() 소진 차단.
     * disableAddExhaustion이 true인 동안 vanilla 소진 추가를 전부 차단한다.
     *
     * addExhaustion()은 PlayerEntity에서 선언되므로 여기서 inject.
     * ServerPlayerEntity에는 선언되어 있지 않아 MixinServerPlayerEntity에서 inject 불가.
     */
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void sm_addExhaustion(float exhaustion, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        SmartMovingServer sm = SmartMovingServer.get(player);
        if (sm.disableAddExhaustion) {
            ci.cancel();
        }
    }
}
