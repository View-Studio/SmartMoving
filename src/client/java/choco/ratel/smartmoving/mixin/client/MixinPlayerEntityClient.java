package choco.ratel.smartmoving.mixin.client;

import choco.ratel.smartmoving.client.SmartMovingClientState;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 6-1 (클라이언트): getBaseDimensions() 커스텀 EntityDimensions 반환.
 * 6-4 (클라이언트): updatePose() Mixin — SM 상태별 포즈 강제 설정.
 *
 * getDimensions()는 final → getBaseDimensions()가 유일한 오버라이드 진입점.
 * Yarn: getBaseDimensions (intermediary: method_55694 확인 완료)
 * Yarn: updatePose (intermediary: method_7318 확인 완료)
 *
 * 서버 측 → MixinPlayerEntity.sm_getBaseDimensions_server(), sm_updatePose_server()
 */
@Mixin(PlayerEntity.class)
@Environment(EnvType.CLIENT)
public abstract class MixinPlayerEntityClient {

    /**
     * 6-1 (클라이언트): SM 포즈별 커스텀 EntityDimensions 반환.
     *
     * **1:1 번역 원칙** (세션 137): 원본 `setHeightOffset(-1F)` 시맨틱
     *   = `boundingBox.minY -= -1 = +1` + `height += -1` → bbox 키 1.8 → 0.8 축소
     *   = 원본 `getEyeHeight() = sp.height - 0.18F` → 0.62F
     *   → **모든 SM 활성 상태**: bbox 0.6 × 0.8 + eyeHeight 0.62F 로 통일.
     *
     * SM 활성 상태 (원본 setHeightOffset(-1F) 설정되는 경우):
     *   - isCrawling        (원본 L2829 crawl 진입 / L2798 climbCrawl 진입 등)
     *   - isClimbCrawling   (원본 L2798)
     *   - isHeadJumping     (원본 L2129 tryJump head)
     *   - isSliding         (원본 L2555 slide 직접 진입)
     *   - isSwimming_sm     (원본 L511 handleSwimming)
     *   - isDiving          (원본 L511)
     *   - isFlying (SM)     (원본 L2512 `isFlying && !wasFlying` 엣지)
     *   - isLevitating      (원본 L2519 `isLevitating && !wasLevitating` 엣지)
     *
     * 비활성 (heightOffset=0 유지, vanilla bbox):
     *   - isDipping: 수면 닿는 상태. 원본 setHeightOffset 없음 → STANDING 1.8 유지.
     *
     * POSE 자체는 `sm_updatePose_client` 에서 SWIMMING/SLIDING 중 적절한 것으로 가로챔.
     * dimensions 는 POSE 와 무관하게 SM 상태만 체크하여 반환 (원본 bbox 시맨틱 1:1).
     */
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sm_getBaseDimensions_client(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        // 🔴 (Phase 2 multi BUG-12/14) ClientPlayerEntity 가드 → AbstractClientPlayerEntity.
        //   기존: self only → remote 측 dim 매핑이 vanilla pose 만 의존 → packet timing 차이로
        //         sm.isCrawling=true + pose=STANDING 잔존 frame 발생 → STANDING dim 매핑 → 우리
        //         sm_setupTransforms R_x 회전 적용 → 1.8 height 모델이 회전 → 이상한 자세 (BUG-12).
        //   변경: 모든 player. dim 결정이 sm.* 비트 의존 → packet A (sm relay) 도착 즉시 dim 갱신.
        if (!((Object) this instanceof net.minecraft.client.network.AbstractClientPlayerEntity player)) return;
        // BUG-20 (세션 36): SM disabled 시 vanilla dimensions 그대로 사용.
        // 🔴 (Phase 2 fix-3-2) cfg.enabled → isSmRenderEnabled (BUG-CONFIG-2 일관성).
        //   self → Config.enabled / remote → 항상 true. self disabled 시 자기 dim vanilla,
        //   remote 의 dim 은 그 player 의 SM state 따라 결정.
        if (!choco.ratel.smartmoving.client.SmartMovingClient.isSmRenderEnabled(player)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): 비행/Levitate 분기 제거.
        //   원본 SmartMovingSelf.setHeightOffset (L1694-L1704) = `boundingBox.minY -= heightOffset
        //   + height += heightOffset` — **충돌 박스만 변경, eyeHeight 변경 없음**. 1.7.10 vanilla
        //   에 EntityPose 시스템 자체가 없어 비행 시 STANDING 모델 + eyeHeight 1.62 그대로 유지.
        //   1.21.1 잘못된 매핑: isFlying/isLevitating 시 0.6 × 0.8 + eyeHeight 0.62F 강제 →
        //   vanilla 가 카메라 위치를 0.62로 적용 → 카메라가 발끝으로 내려옴 + 모델 작게 그려짐.
        //   사용자 보고 BUG-29 "비행 시 몸 중심점이 발끝으로 바뀜" 직접 원인.
        //   정정: isFlying/isLevitating 조건 제거 → vanilla 1.21.1 STANDING POSE + 1.8 height +
        //   1.62 eyeHeight 그대로 → 원본 1.7.10 동작 1:1 매칭 (충돌 박스도 vanilla 처리).
        //
        // 원본 setHeightOffset(-1F) 상태 전수 — Crawling/Sliding/Swimming 등 모두 0.6 × 0.8 +
        // eyeHeight 0.62F. isDipping/isFlying/isLevitating 은 vanilla 통과.
        // 🔴 isClimbCrawling 분리: 원본 1.7.10 사다리 매달림 자세 STANDING 유지 (POSE 시스템
        //   없음). setHeightOffset(-1F) 는 박스만 변경, eyeHeight 변경 없음 (BUG-29 동일 패턴).
        //   1.21.1 잘못된 매핑: 0.6×0.8 + eyeHeight 0.62F 강제 → 카메라 발끝으로 1m 떨어짐 →
        //   사용자가 "엎드리기" 로 인식.
        //   정정: isClimbCrawling 시 박스만 0.6×0.8, eyeHeight 1.62F (vanilla STANDING 유지).
        // 🔴 isCrawling && isClimbing 케이스 추가: isClimbCrawling 해제 엣지의 toCrawling()
        //   호출 → 1 틱 isCrawling=true → 등반 중인데 eyeHeight 0.62F 강제 → 카메라
        //   1.62↔0.62 토글 = 사용자 보고 "몸 애니메이션 주기적 요동". 등반 중이면 STANDING
        //   eyeHeight 유지.
        // 🔴 ICC 분리: 원본 1.7.10 사다리 매달림 자세 STANDING 유지 (POSE 시스템
        //   없음). setHeightOffset(-1F) 는 박스만 변경, eyeHeight 변경 없음 (BUG-29 동일 패턴).
        //   1.21.1 잘못된 매핑: 0.6×0.8 + eyeHeight 0.62F 강제 → 카메라 발끝으로 1m 떨어짐 →
        //   사용자가 "엎드리기" 로 인식.
        //   정정: isClimbCrawling 시 박스만 0.6×0.8, eyeHeight 1.62F (vanilla STANDING 유지).
        // 🔴 isCrawling && isClimbing 케이스 추가: isClimbCrawling 해제 엣지의 toCrawling()
        //   호출 → 1 틱 isCrawling=true → 등반 중인데 eyeHeight 0.62F 강제 → 카메라
        //   1.62↔0.62 토글 = 사용자 보고 "몸 애니메이션 주기적 요동". 등반 중이면 STANDING
        //   eyeHeight 유지.
        // 🔴 (isCrawling && isClimbing) 분기 가드 추가 (사용자 보고 fix — 끊김, 2026-05-03):
        //   본래 의도 (메모리 코멘트): ICC 해제 엣지 1 tick 한정 (= toCrawling() 후 isCrawling=true
        //   잔존하면서 등반 자세 유지). 그러나 가드 없어 isCrawlClimbing 자가유지 중에도 매치 →
        //   박스 +1m offset → grip 영역 위로 → wantClimbHolding 매치 → 의도치 않은 ICC 진입 → ICC
        //   EXIT 시 entity.y +1m 변경 → 사용자 시점 점프 (= 끊김).
        //   해결: `&& wasClimbCrawling` 가드 추가 → ICC 활성 중 + 해제 엣지 1 tick 만 매치.
        //   isCrawlClimbing 자가유지 중 wasClimbCrawling=false → smSmall 분기로 떨어져 박스 안정.
        if (sm.isClimbCrawling || (sm.isCrawling && sm.isClimbing && sm.wasClimbCrawling)) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔴 비행 콜리전 — 사용자 명시 (2026-05-02): "비행은 건드리지 마".
        //   기존 매핑 (567dbec) 유지: dim=(0.6, 0.8, 1.62) + MixinEntity.sm_offsetBoundingBoxForFlying
        //   가드 (eyeHeight > 1) 통과 → 박스 +1 적용 → 사용자 시점 보존 매핑 그대로.
        //   ICC 와 다른 매핑 (ICC 는 setPos(y+1) + eyeHeight 0.62 원본 1:1).
        if (sm.isFlying || sm.isLevitating) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(1.62F));
            return;
        }
        // 🔴 isCrawlClimbing 추가 (사용자 보고 fix — 콜리전 standing 크기 BUG, 2026-05-03):
        //   isCrawlClimbing 진입 엣지 (원본 L2752 1:1) 에서 isCrawling=false 강제 → 다음 tick까지
        //   isCrawling=false 잔존 → smSmall 분기 미매치 → vanilla pose dim (CROUCHING 1.5) 통과
        //   = "standing 크기 콜리전" 사용자 보고. ICC 활성 + 사다리 grip 시 isCrawling=false 시점에
        //   도 동일 문제로 박스 더 작아짐 (vanilla SWIMMING 0.6).
        //   해결: smSmall 에 isCrawlClimbing 직접 추가 → 자가유지 동안 항상 (0.8, 0.62) 보장.
        //   eye=0.62 → mixin offset 가드 (eye>1) 미매치 → 박스 +1m offset 안 됨 → ICC EXIT 후
        //   entity.y +1m 위치에 박스 (entity.y, entity.y+0.8) = (old+1, old+1.8) = 원본 1:1.
        boolean smSmall = sm.isCrawling || sm.isCrawlClimbing
                       || sm.isHeadJumping || sm.isSliding
                       || sm.isSwimming_sm || sm.isDiving;
        if (smSmall) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // SLIDING POSE 가 vanilla 가 아닌 경로로 들어온 경우 (외부 모드 등) 보강 처리
        if (pose == EntityPose.SLIDING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 0.8F).withEyeHeight(0.62F));
            return;
        }

        // 🔴 orphan SWIMMING pose 가드 (2026-05-04 사용자 보고 — "1칸 진입 직전 슬라이딩 풀면
        //   가끔 콜리전 정상 엎드리기보다 작음 + 애니메이션 STANDING"):
        //   원인: 슬라이딩 → standing 1 frame 사이 server→client pose sync lag (1~4 frame)
        //         로 pose=SWIMMING 잔존. SM smSmall 분기 (isCrawling/isCrawlClimbing/
        //         isHeadJumping/isSliding/isSwimming_sm/isDiving) 모두 false → 위 분기 미매치
        //         → vanilla SWIMMING dim (0.6×0.6) 통과 → 박스 0.6×0.8 보다 작음.
        //         애니메이션은 SM 분기 모두 false 라 vanilla setAngles → STANDING 자세.
        //   해결: pose=SWIMMING + smSmall 미매치 = orphan → STANDING dim 강제 (sync 풀리면
        //         정상 standing). 메모리 feedback_smSmall_dim_omission.md 동일 패턴.
        if (pose == EntityPose.SWIMMING) {
            cir.setReturnValue(EntityDimensions.changing(0.6F, 1.8F).withEyeHeight(1.62F));
        }
    }

    /**
     * 11-4 (클라이언트): SM 비행 비활성화 시 공중 이동 억제.
     *
     * vanilla: flying → flySpeed, !flying+sprinting → 0.026F, otherwise → 0.02F.
     * SM: flying && !cfg.fly → 0.05F (A-30 확인값).
     * 이 메서드를 0.05F로 제한하면 vanilla travel()의 공중 XZ 가속이 억제된다.
     */
    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true)
    private void sm_getOffGroundSpeed(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        SmartMovingConfig cfg = SmartMovingConfig.Config;
        if (!cfg.enabled) return;
        if (player.getAbilities().flying && !cfg.fly) {
            cir.setReturnValue(0.05F);
            return;
        }
        // 🔴 (2026-04-28) SM 공중 가속 임시 비활성 — 사용자 보고 "점프 시 엄청 빨라짐" 디버깅.
        //   땅 (getMovementSpeed) 만 SM factor 적용 + 공중 (getOffGroundSpeed) 은 vanilla 그대로.
        //   가속 사라지면 공중 매핑 식 재설계.
    }

    /**
     * 6-4 (클라이언트): SM 이동 상태에서 vanilla updatePose() 취소 후 SM 포즈 강제 설정.
     *
     * **1:1 번역 원칙** (세션 137): 원본 SM 이 vanilla POSE 시스템 없던 시대 설계라
     * 상태별로 수동 bbox 조작. 1.21.1 는 POSE 기반 bbox 관리. SM 이 쓰는 상태별로 POSE 를
     * 독점 설정하여 vanilla 가 덮어쓰지 못하게 차단.
     *
     * 우선순위 매핑 (원본 heightOffset=-1F 활성 상태 전수):
     *   isCrawling / isClimbCrawling             → SWIMMING (엎드림 애니)
     *   isHeadJumping / isSliding                 → SLIDING  (납작 자세)
     *   isSwimming_sm / isDiving                  → SWIMMING (수영 애니)
     *   isFlying (SM) / isLevitating              → SLIDING  (비행 시 bbox 0.8 유지)
     *   isDipping                                 → STANDING (heightOffset=0, 수면 서있기)
     *   그 외                                     → vanilla updatePose() 통과 (elytra/trident 등 유지)
     *
     * dimensions 값 (0.6 × 0.8 + eyeHeight 0.62) 는 sm_getBaseDimensions_client 에서 전수 반영.
     */
    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void sm_updatePose_client(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        // BUG-22 (세션 36): SM disabled 시 vanilla updatePose 그대로 — sm.* 잔존 timing
        //   영향 차단. 비행 진입 첫 프레임에 SLIDING POSE 강제 잔존 가능 → motion 영향.
        if (!SmartMovingConfig.Config.enabled) return;
        SmartMovingClientState sm = SmartMovingClientState.get(player);

        if (sm.isCrawling && !sm.isClimbing) {
            // 엎드림 자세 — SWIMMING POSE 재활용 (vanilla 수영 애니 공유)
            // 🔴 isClimbCrawling 분기 제거: 원본 1.7.10 vanilla 에 EntityPose 시스템 없음 →
            //   isClimbCrawling 시 자세 STANDING 유지 (박스만 0.6×0.8 축소). SWIMMING POSE
            //   강제 시 사용자 시각적으로 사다리 막바지에서 수평 엎드림 자세 → "엎드리기" 인식.
            //   원본 1:1 매핑은 STANDING POSE + 작은 박스. dimensions 는 sm_getBaseDimensions
            //   가 isClimbCrawling 시에도 0.6×0.8 적용 (원본 setHeightOffset(-1F) 1:1).
            // 🔴 !isClimbing 가드: isClimbCrawling 해제 엣지의 toCrawling() 호출 → 1 틱
            //   isCrawling=true → 등반 중 SWIMMING POSE 발동 → STANDING↔SWIMMING 토글 →
            //   사용자 보고 "몸 애니메이션 주기적 요동". 등반 중이면 자세 STANDING 유지.
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        } else if (sm.isHeadJumping || sm.isSliding) {
            // 납작 자세 — SLIDING POSE
            player.setPose(EntityPose.SLIDING);
            ci.cancel();
        } else if (sm.isSwimming_sm || sm.isDiving) {
            // 수영/잠수 — SWIMMING POSE (vanilla 도 동일, 명시적 고정)
            player.setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
        // 🔴 BUG-29 진짜 원인 정정 (Flying Phase / 세션 45): isFlying/isLevitating 분기 제거.
        //   원본 1.7.10 vanilla 에 EntityPose 시스템 없음 → 비행 시 STANDING 모델 그대로 유지.
        //   1.21.1 잘못된 매핑: SLIDING POSE 강제 → vanilla 가 eyeHeight 작게 적용 → 카메라
        //   발끝으로 내려옴 + 모델 작게 그려짐. 사용자 보고 BUG-29 직접 원인.
        //   정정: 분기 제거 → vanilla 1.21.1 STANDING POSE 그대로 → 카메라 정상 (1.62 eyeHeight)
        //   + 모델 정상 (1.8 height) = 원본 1.7.10 동작 1:1 매칭.
        // isFlying/isLevitating/isDipping/그 외 → vanilla updatePose() 통과 (STANDING 등)
    }

    /**
     * 🔴 BUG-27 진짜 원인 정정 (Flying Phase / 세션 51): vanilla PlayerEntity.travel() 의 비행
     *   분기 motionY 덮어쓰기 차단.
     *
     * vanilla PlayerEntity.travel(Vec3d) 디컴파일:
     * <pre>
     *   if (this.getAbilities().flying && !this.hasVehicle()) {
     *       double d = this.getVelocity().y;          // 이전 motionY 저장
     *       super.travel(movementInput);               // → LivingEntity.travel
     *                                                  //   → sm_travel_client (HEAD inject)
     *                                                  //   → handleFlying (motionY 변경) + ci.cancel
     *                                                  //   → LivingEntity.travel 본체 skip
     *       Vec3d vec3d = this.getVelocity();          // 우리 SM 변경된 velocity
     *       this.setVelocity(vec3d.x, d * 0.6, vec3d.z); // ← motionY = 이전 * 0.6 강제!
     *       this.onLanding();
     *       this.setFlag(7, false);
     *   }
     * </pre>
     *
     * setVelocity(vec3d.x, **d * 0.6**, vec3d.z) 가 우리 SM motionY 변경 덮어씀 → SM 비행 시
     * 마우스 pitch → W 진행 방향 (motionY +0.0322) 효과 완전 무시 = 사용자 보고 BUG-27 직접 원인.
     *
     * 정정: setVelocity 호출 가로채서 SM 비행 활성 시 y 인자 무시 (현재 SM motionY 유지).
     * X/Z 는 vanilla 처리 그대로.
     */
    @Redirect(
        method = "travel",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(DDD)V")
    )
    private void sm_travel_setVelocity_flyFix(PlayerEntity self, double x, double y, double z) {
        if (self instanceof ClientPlayerEntity player
                && SmartMovingConfig.Config.enabled
                && SmartMovingClientState.get(player).isFlying) {
            // SM 비행 시 motionY *= 0.6 차단 — handleFlying 가 set 한 motionY 보존.
            // X/Z 는 vanilla 인자 그대로 (handleFlying 결과와 동일).
            self.setVelocity(x, self.getVelocity().y, z);
            return;
        }
        // 비SM 비행 (vanilla) → 원본 동작 그대로 (motionY *= 0.6).
        self.setVelocity(x, y, z);
    }

    /**
     * 🔴 (2026-05-05 Phase 1-A) 다른 player 도 매 tick stats 갱신 — 비행 자세 등의
     *   walkFactor/standFactor/totalDistance 식 source.
     *   자기 자신 (= ClientPlayerEntity) 은 MixinEntityClient.sm_afterMove_client 에서 갱신.
     *   다른 player (= AbstractClientPlayerEntity, ClientPlayerEntity 아님) 만 여기 처리.
     *   PlayerEntity.tickMovement TAIL — 모든 player tick 적용. instanceof 분기로 remote only.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void sm_tickStatsForRemote(CallbackInfo ci) {
        if (!SmartMovingConfig.Config.enabled) return;
        Object self = (Object) this;
        // local (= ClientPlayerEntity) 은 MixinEntityClient 에서 이미 처리.
        if (self instanceof ClientPlayerEntity) return;
        // remote 만 처리 — AbstractClientPlayerEntity (= 다른 player on client side).
        if (!(self instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(remote);
        // 🔴 (Phase 1-B fix-2) stats source: getVelocity() → position delta.
        //   원본 SmartStatistics.calculateAllStats: `diffX = sp.posX - sp.prevPosX` (= delta).
        //   기존 매핑은 server-sync velocity 가정했으나 vanilla 1.21.1 server 가
        //   다른 player 의 velocity field 를 sync 안 함 → 항상 (0,0,0) → stats 입력 0
        //   → 다른 player 팔다리 swing 안 흔들림 (BUG-D).
        //   fix: 원본대로 prev/cur position delta. interpolation 진동은 stats 자체 EMA 가 평균화.
        double dx = remote.getX() - remote.prevX;
        double dy = remote.getY() - remote.prevY;
        double dz = remote.getZ() - remote.prevZ;
        sm.stats.calculate(0, 0, 0,
                           dx, dy, dz,
                           remote.getYaw());
    }

    /**
     * 🔴 (Phase 2 multi BUG-10) sm_correctOnUpdate 의 remote 버전 — 이동 방향 bodyYaw 보정.
     *   self (ClientPlayerEntity) 는 MixinClientPlayerEntity.sm_correctOnUpdate 가 처리.
     *   remote 도 동일 식 적용 → wasd 누름 시 "몸통 이동 방향 치우침" 동작 일관.
     *   원본 SmartMovingBase.correctOnUpdate(isSmall) 동일 식. 0.02~0.05 m/tick 느린 이동 보정.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void sm_correctOnUpdateRemote(CallbackInfo ci) {
        if (!SmartMovingConfig.Config.enabled) return;
        Object self = (Object) this;
        if (self instanceof ClientPlayerEntity) return;  // self 는 MixinClientPlayerEntity 가 처리.
        if (!(self instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(remote);

        boolean isSmall = sm.isSwimming_sm || sm.isDiving || sm.isDipping || sm.isCrawling;
        if (!isSmall) return;

        double d  = remote.getX() - remote.prevX;
        double d1 = remote.getZ() - remote.prevZ;
        float f = (float) Math.sqrt(d * d + d1 * d1);
        if (f <= 0.02F || f >= 0.05F) return;

        float f1 = (float)(Math.atan2(d1, d) * 180.0 / Math.PI) - 90F;
        if (remote.handSwingProgress > 0.0F) f1 = remote.getYaw();

        float f4 = f1 - remote.bodyYaw;
        for (; f4 < -180F; f4 += 360F) {}
        for (; f4 >= 180F;  f4 -= 360F) {}
        float x = remote.bodyYaw + f4 * 0.3F;

        float f5 = remote.getYaw() - x;
        for (; f5 < -180F; f5 += 360F) {}
        for (; f5 >= 180F;  f5 -= 360F) {}
        if (f5 < -75F) f5 = -75F;
        if (f5 >= 75F)  f5 = 75F;

        remote.setBodyYaw(remote.getYaw() - f5);
        if (f5 * f5 > 2500F) remote.setBodyYaw(remote.bodyYaw + f5 * 0.2F);

        for (; remote.bodyYaw - remote.prevBodyYaw < -180F; remote.prevBodyYaw -= 360F) {}
        for (; remote.bodyYaw - remote.prevBodyYaw >= 180F; remote.prevBodyYaw += 360F) {}
    }

    /**
     * 🔴 (Phase 2 multi BUG-11 v3) remote 측 비행 종료 — entity.y +1.0 직접 set.
     *   사용자 평가: 묻히지 않음 + 살짝 플리킹 (= 가장 안정적 동작). 시각 한계 수용.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void sm_handleRemoteFlyingExitYSync(CallbackInfo ci) {
        if (!SmartMovingConfig.Config.enabled) return;
        Object self = (Object) this;
        if (self instanceof ClientPlayerEntity) return;
        if (!(self instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote)) return;
        SmartMovingClientState sm = SmartMovingClientState.get(remote);

        if (sm.smPrevWasFlyingForLerpFix && !sm.isFlying) {
            double newY = remote.getY() + 1.0;
            remote.setPosition(remote.getX(), newY, remote.getZ());
            remote.lastRenderY = newY;
            remote.prevY = newY;
        }
        sm.smPrevWasFlyingForLerpFix = sm.isFlying;
    }
}
