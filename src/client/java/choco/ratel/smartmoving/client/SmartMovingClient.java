package choco.ratel.smartmoving.client;

import choco.ratel.smartmoving.client.input.SmartMovingKeys;
import choco.ratel.smartmoving.config.SmartMovingConfig;
import choco.ratel.smartmoving.network.SmartMovingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public class SmartMovingClient implements ClientModInitializer {

    /**
     * 🔴 (2026-05-05 사용자 보고 — "a 콘피그 off 시 a 측에서 모두 vanilla 보임"):
     *   render-path 의 `Config.enabled` 검사가 자기 client cfg → a disabled 시 자기 측에서
     *   모든 player render 의 SM 분기 차단 → 모두 vanilla. remote 측은 자기 cfg=true 라 정상.
     *   해결: render-path enabled 검사를 self-only 로 한정.
     *     - self → 자기 Config.enabled
     *     - remote → 항상 true (= 자기 cfg 무관, remote 의 SM state 자체 분기)
     *   self 가 disabled 면 자기 SM state 모두 false (= state update / packet 송신 stop) 라
     *   remote 측에서 자기는 자동으로 vanilla 표시 (= 정확).
     */
    public static boolean isSmRenderEnabled(net.minecraft.entity.Entity entity) {
        if (SmartMovingConfig.Config.enabled) return true;
        // 자기 cfg disabled — remote player 만 통과 (= vanilla cfg 무관 + remote 의 SM 분기 정상).
        if (!(entity instanceof AbstractClientPlayerEntity ap)) return false;
        return ap != MinecraftClient.getInstance().player;
    }

    @Override
    public void onInitializeClient() {
        SmartMovingKeys.register();
        SmartMovingHud.register();
        registerClientReceivers();
        registerConnectionEvents();
        registerGameMessageHandler();
    }

    private static void registerConnectionEvents() {
        // 클라이언트 접속 해제 시 상태 인스턴스 정리 + Config 복원
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                SmartMovingClientState.remove(client.player);
            }
            // 서버 설정 전환을 복원 — 다음 서버 접속까지 클라이언트 설정 사용
            SmartMovingConfig.Config = SmartMovingConfig.INSTANCE;
            // 원본 SmartMovingServerConfig.reset() 대응 — SERVER_CONFIG 인스턴스 초기화.
            // 다음 서버 접속 시 이전 서버 설정 잔류 방지.
            SmartMovingConfig.resetServerConfig();
        });
    }

    private static void registerClientReceivers() {
        // State: 다른 플레이어의 이동 상태 수신 (서버 릴레이) — C-24
        // 원본: SmartMovingFactory.getOtherSmartMoving(entityId).processStatePacket(state)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    ClientWorld world = client.world;
                    if (world == null) return;
                    Entity entity = world.getEntityById(payload.entityId());
                    if (entity == null) return;
                    SmartMovingClientState target = SmartMovingClientState.get(entity.getUuid());
                    // 🔴 (Phase 2 multi BUG-7) remote ICC EXIT 시 박스/모델 1칸 down jump 차단.
                    //   원인: packet 처리 (= ICC=false 적용 + dim 변경) 시점과 PlayerEntity.tick HEAD
                    //         사이 1 tick lag → ICC dim offset (+1m bb) 사라진 후 entity.y 그대로 →
                    //         같은 tick 의 render frames 에서 bb=[entity.y..entity.y+0.8] (= 1m 아래) 보임.
                    //   해결: packet 처리 lambda 안에서 enter-edge 검출 + setPos +1m + lerp cancel
                    //         즉시 적용 → ICC=false 와 동시에 entity.y +1m → bb 일관성 유지.
                    //   self (= ClientPlayerEntity) 는 자체 fix (= 메모리 *project_isclimbcrawling_complete*).
                    boolean wasIcc = target.isClimbCrawling;
                    // 🔴 (Phase 2 multi BUG-15, 2026-05-13) remote 슬라이딩 진입/종료 매핑.
                    //   self side fix #60 (= move(0,-1,0)) + fix #62 v2 + fix #70 (= setPos(y+1)
                    //   predictive drop 검사) 가 remote 측 packet handler 에 누락 → remote 측에서
                    //   진입 시 1 tick 공중 1칸 위 + 종료 시 1 tick 땅속 1m BUG. BUG-7 ICC EXIT
                    //   v25.3 대칭 패턴 — packet 처리 lambda 안 즉시 setPos 로 1 tick lag 차단.
                    //   self (= ClientPlayerEntity) 는 자체 fix.
                    boolean wasSliding = target.isSliding;
                    boolean wasFlying = target.isFlying;
                    double slideBoxMinYBefore = 0.0;
                    double flyBoxMinYBefore = 0.0;
                    if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity rsPre
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        double bbMinY = rsPre.getBoundingBox().minY;
                        // calc dim *전* bb (= mixin offset 활성 시점). self fix #70 의 _currentBoxMinY70 등가.
                        if (wasSliding) slideBoxMinYBefore = bbMinY;
                        // 🔴 (BUG D fix, 2026-05-13) 비행 종료 직전 bb (= mixin offset 활성 시점) — gap 측정용.
                        if (wasFlying) flyBoxMinYBefore = bbMinY;
                    }
                    target.processStatePacket(payload.state());
                    // 🔴 (Phase 2 multi BUG-12/14) packet 도착 시 dim 즉시 갱신 → vanilla pose sync 대기
                    //   없이 sm.* 비트 따라 dim 결정. boolean OR 가드 제거 — 여러 비트 동시 변경 시
                    //   감지 누락 회피. dim 동일 시 vanilla 자체 영향 미미.
                    if (entity instanceof net.minecraft.entity.LivingEntity living) {
                        living.calculateDimensions();
                    }
                    // 🔴 (Phase 2 BUG-7) self side ICC ENTER 매핑 1:1 복제.
                    //   self side ICC 진입 (SmartMovingClientState L2367-L2423):
                    //     heightOffset = -1F + calculateDimensions + move(0, 0.05, 0) + entity.y 복원
                    //     + horizontalCollision 복원 + Camera 보정.
                    //   사용자 보고 "remote 진입 시점 x,z 뒤로 땡김 + y 내려감": move(0, 0.05, 0) +
                    //   entity.y 복원 + horizontalCollision 복원 매핑이 누락. self side 의 vanilla
                    //   collision 결과 cancel 메커니즘 (메모리 코멘트 = "ICC 진입/해제 매 tick 진동
                    //   시 entity.y +0.05m 누적 → 카메라 움찔움찔 fix") 그대로 적용.
                    //   heightOffset/Camera 는 self 식/1인칭 전용이라 remote 안 필요.
                    if (!wasIcc && target.isClimbCrawling
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteEnter
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        boolean wasColH = remoteEnter.horizontalCollision;
                        double iccEnterYBefore = remoteEnter.getY();
                        remoteEnter.move(net.minecraft.entity.MovementType.SELF,
                                new net.minecraft.util.math.Vec3d(0, 0.05, 0));
                        if (remoteEnter.getY() != iccEnterYBefore) {
                            remoteEnter.setPosition(remoteEnter.getX(), iccEnterYBefore, remoteEnter.getZ());
                        }
                        remoteEnter.horizontalCollision = wasColH;
                    }

                    if (wasIcc && !target.isClimbCrawling
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remote
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        // 🔴 (Phase 2 BUG-7 fix 정착) self side ICC EXIT 매핑 1:1 복제 (cd25cfc 패턴).
                        //   self side: setPosition(x, y+1, z) + lastRenderY/prevY +=1 + calculateDimensions
                        //              + bottom snap (사다리/덩굴 케이스 미적용).
                        //   isCrawling 가드 = self 가드 (mustCrawl||sneakPressedRaw||crawlToggled+gap<1)
                        //                     통과 결과 직접 검사.
                        //   calculateDimensions 는 packet handler 위 (L109-L111) 에서 이미 호출됨.
                        if (target.isCrawling) {
                            double newY = remote.getY() + 1.0;
                            remote.setPosition(remote.getX(), newY, remote.getZ());
                            remote.lastRenderY = newY;
                            remote.prevY = newY;
                            // 🔴 (BUG B fix, 2026-05-13) lerp cancel — BUG-15 동일 패턴.
                            //   vanilla OtherClientPlayerEntity.lerpPosAndRotation 이 매 tick srvY 로
                            //   entity.y 끌고 감 → 우리 setPos+1m 무효화 → 점진 -1m down → 사용자 보고
                            //   "1칸 아래 내려갔다 올라옴" 직접 원인. srvY/bti 동기화로 lerp 차단.
                            choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accI =
                                    (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remote;
                            accI.sm_setServerY(newY);
                            accI.sm_setBodyTrackingIncrements(0);
                            // 일반 블록 케이스만 bottom snap. 사다리/덩굴 케이스 미적용 (ladder maxY+1 위 유지).
                            if (!remote.isClimbing()) {
                                double minY = remote.getBoundingBox().minY;
                                double gap = minY - choco.ratel.smartmoving.client.SmartMovingClientState
                                        .getMaxPlayerSolidBetween(remote, minY - 1.0, minY, 0);
                                if (gap >= 0.0 && gap < 1.0) {
                                    remote.move(net.minecraft.entity.MovementType.SELF,
                                            new net.minecraft.util.math.Vec3d(0, -gap, 0));
                                    // bottom snap 후 srvY 재동기화 (= move 가 entity.y 변경했으므로).
                                    accI.sm_setServerY(remote.getY());
                                    accI.sm_setBodyTrackingIncrements(0);
                                }
                            }
                        }
                    }

                    // 🔴 (Phase 2 multi BUG-15, 2026-05-13) remote 슬라이딩 진입 — self fix #60 1:1.
                    //   self side L2106-L2111: heightOffset=-1 + isSliding=true + calc dim +
                    //   move(0,-1,0) (= entity.y -1m). dim eye=1.62 활성 + mixin offset bb +1m up →
                    //   bb 발 = entity.y + 1m = ground.
                    //   remote 측: SM state packet 도착 → isSliding=true + calc dim 까지 (위 L82-L84)
                    //     이미 완료. 그러나 self 의 entity.y -1m 가 server broadcast 도착 전 (1 tick lag)
                    //     → remote.y = old self.y (= self ground) → bb 발 = remote.y + 1m = self ground
                    //     + 1m = **공중 1칸 위 BUG**.
                    //   해결: packet 처리 lambda 안 즉시 setPos(y-1) → entity.y -= 1m → bb 발 = (remote.y
                    //     -1) + 1m = self ground 정합. lastRenderY/prevY 동기화 — vanilla lerp 점프 차단.
                    //   BUG-7 ICC EXIT v25.3 부호 반전 (+1 → -1) 패턴.
                    // 🔴 (BUG D fix, 2026-05-13) !wasFlying 가드 추가.
                    //   비행 → slide 직접 전환 시 *비행 종료 분기* 가 self side standupIfPossible 분기 3
                    //   (= move(0, -gap, 0)) 1:1 매핑으로 처리. 우리 slide 진입 fix 의 fixed -1m push 와
                    //   gap-변동 식 충돌 방지.
                    if (!wasSliding && target.isSliding && !wasFlying
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteSlideEnter
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accE =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteSlideEnter;
                        double newY = remoteSlideEnter.getY() - 1.0;
                        remoteSlideEnter.setPosition(remoteSlideEnter.getX(), newY, remoteSlideEnter.getZ());
                        remoteSlideEnter.lastRenderY = newY;
                        remoteSlideEnter.prevY = newY;
                        // 🔴 lerp cancel — vanilla OtherClientPlayerEntity.lerpPosAndRotation 이
                        //   srvY (= 직전 broadcast standing y) 로 entity.y 끌고 감 → 우리 setPos 무효화.
                        //   BUG-7 v25.3 패턴: srvY = newY + bti = 0 → 다음 broadcast 도착 전까지 lerp 차단.
                        accE.sm_setServerY(newY);
                        accE.sm_setBodyTrackingIncrements(0);
                    }

                    // 🔴 (Phase 2 multi BUG-15, 2026-05-13) remote 슬라이딩 종료 — self fix #62 v2 + #70 1:1.
                    //   self side L2337-L2360: predictive drop 검사 후 setPos(y+1) + lastRenderY/prevY +=1.
                    //   remote 측: SM state packet 도착 → isSliding=false + calc dim 까지 (위 L82-L84)
                    //     이미 완료 → mixin offset 차단 → bb 발 = remote.y. self 의 entity.y +1m push 가
                    //     server broadcast 도착 전 (1 tick lag) → remote.y = old self.y (= self ground - 1m)
                    //     → bb 발 = self ground - 1m = **땅속 1m BUG**.
                    //   해결: slideBoxMinYBefore (= calc dim 전, mixin offset 활성 시점 bb) 와 newY (=
                    //     calc dim 후 entity.y) 차이로 willDrop 검사 → drop > 0.5m 시 setPos(y+1).
                    //   회귀 차단: 비행 → 슬라이딩 종료 (fix #87) 등 slideBoxMinYBefore ≈ remote.y 케이스
                    //     (= mixin offset 이미 차단) → willDrop=false → push 안 함.
                    if (wasSliding && !target.isSliding
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteSlideExit
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accX =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteSlideExit;
                        double newY = remoteSlideExit.getY();
                        boolean willDrop = (slideBoxMinYBefore - newY) > 0.5;
                        if (willDrop) {
                            newY += 1.0;
                            remoteSlideExit.setPosition(remoteSlideExit.getX(), newY, remoteSlideExit.getZ());
                            remoteSlideExit.lastRenderY = newY;
                            remoteSlideExit.prevY = newY;
                            // 🔴 lerp cancel — 진입 분기와 동일 패턴 (srvY/bti 동기화 → lerp 차단).
                            accX.sm_setServerY(newY);
                            accX.sm_setBodyTrackingIncrements(0);
                        }
                    }

                    // 🔴 (BUG D fix, 2026-05-13) remote 비행 종료 — self side standupIfPossible 1:1 매핑.
                    //   self side `standupIfPossible(player, tryLanding=true, restoreFromFlying)` 의
                    //   3 분기를 *결과 비트* (target.isSliding/isCrawling) + *gap 측정* 으로 추정 + 각
                    //   분기별 entity.y 변화 식 1:1 적용:
                    //     - 분기 1 (resetHeightOffset, gap>=1 && !sneak): entity.y 변경 X. lerp cancel 만.
                    //     - 분기 2 (standUp, gap<1): entity.y += (1 - gap).
                    //     - 분기 3 (toSlidingOrCrawling, sneak+grab): entity.y -= gap. 결과 isSliding/
                    //       isCrawling=true → packet 으로 식별.
                    //
                    //   메모리 feedback_self_to_server_remote_one_to_one — "self 코드 그대로 1:1 복제".
                    //   메모리 feedback_packet_lambda_immediate_fix — "packet lambda 안 즉시 fix".
                    //   기존 sm_handleRemoteFlyingExitYSync (= 매 tick HEAD inject, fixed +1m) 보다
                    //   정확 — 분기별 정확한 entity.y 변화 식.
                    //
                    //   slide 진입 fix 와 충돌 방지: slide 진입 fix 에 !wasFlying 가드 추가 → 비행 →
                    //   slide 케이스는 *이 분기 (= 분기 3)* 에서 -gap push.
                    // 🔴 (BUG D fix, 2026-05-13) remote 비행 종료 — self side standupIfPossible 1:1 매핑.
                    //   self side 의 3 분기 결과 비트 + srvY 기반 gap 측정으로 분기 추정 + 분기별
                    //   entity.y 변화 식 1:1 적용.
                    //
                    //   gap 측정: flyBoxMinYBefore (= lerp lag 잔존) 대신 srvY + 1.0 (= server 정확값) 사용.
                    //   newY 식: groundY 직접 기반 (= self.y_final 매핑, gap 측정 부정확 영향 제거).
                    //
                    //   self side 식 유도 + dump 검증:
                    //     - 분기 1 (resetHO, gap>=1 && !sneak): self.y 변화 X.
                    //     - 분기 2 (standUp, gap<1): self.y_new = self.y + (1 - gap) = groundY.
                    //     - 분기 3 (toSldOrCr, sneak+grab): self side `move(0, -gap, 0)` 가 vanilla
                    //       collision 으로 self.y 변화 X (= dump delta=0). setPos 적용 시 remote.y
                    //       (= vanilla lerp lag 잔존) ↔ self.y_self 1m 차이로 *visual jump* 발생.
                    //       해결: 분기 3 setPos+lerp cancel 자체 제거 → vanilla 자체 lerp 자연 처리
                    //       (= 매 tick 1/bti step 점진, ~15 tick 부드러운 transition).
                    if (wasFlying && !target.isFlying
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteFly
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accF =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteFly;
                        double srvY = accF.sm_getServerY();
                        double srvBBMinY = srvY + 1.0;  // 비행 mixin offset 활성 = bb 발 = self.y + 1.
                        double groundY = choco.ratel.smartmoving.client.SmartMovingClientState
                                .getMaxPlayerSolidBetween(remoteFly,
                                        srvBBMinY - 1.1, srvBBMinY + 0.5, 0);
                        double gap = srvBBMinY - groundY;
                        boolean groundClose = gap < 1.0;

                        // 분기 3 (slide/crawl) 은 self.y 변화 X 라 fix skip → vanilla lerp 자연 처리.
                        if (!target.isSliding && !target.isCrawling && groundClose) {
                            // 분기 2 standUp — newY = groundY (= ground 표면).
                            double newY = groundY;
                            if (newY != remoteFly.getY()) {
                                remoteFly.setPosition(remoteFly.getX(), newY, remoteFly.getZ());
                                remoteFly.lastRenderY = newY;
                                remoteFly.prevY = newY;
                            }
                            // lerp cancel — vanilla broadcast 도착 전까지 lerp 차단.
                            accF.sm_setServerY(newY);
                            accF.sm_setBodyTrackingIncrements(0);
                        }
                        // 분기 1 (gap>=1) / 분기 3 (slide/crawl) — fix skip, vanilla 자체 처리.
                    }
                });
            });

        // ConfigContent: 서버 설정 수신 → Config 전환 (C-11)
        // 원본: SmartMovingComm.processConfigContentPacket(content, username, blockCode=false)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigContentPayload.ID,
            (payload, context) -> {
                String[] lines = payload.lines();
                MinecraftClient client = context.client();
                client.execute(() -> processConfigContentPacket(lines));
            });

        // ConfigChange: 서버가 "설정 변경 권한 없음"을 알림 (C-12)
        // 원본: SmartMovingOptions.writeNoRightsToChangeConfigMessageToChat(isConnectedToRemoteServer())
        // A-26: 원문 확인 — remote/local 구분 메시지 (en_us.json smartmoving.message.config.illegal.*)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.ConfigChangePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                boolean isRemote = isRemoteServer(client);
                String key = isRemote
                    ? "smartmoving.message.config.illegal.remote"
                    : "smartmoving.message.config.illegal.local";
                client.execute(() -> {
                    if (client.player != null) client.player.sendMessage(Text.translatable(key));
                });
            });

        // SpeedChange: 서버에서 속도 변경 동기화 (C-12)
        // 원본: difference==0 → 권한없음, !=0 → Config.changeSpeed(difference) + writeClientSpeedMessageToChat
        // A-26: 권한없음 메시지 원문 확인 (en_us.json smartmoving.message.speed.illegal.*)
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.SpeedChangePayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                int difference = payload.difference();
                if (difference != 0) {
                    client.execute(() -> {
                        SmartMovingConfig.Config.changeSpeed(difference);
                        // IMPL-05: 속도 변경 채팅 피드백 (원본: writeClientSpeedMessageToChat)
                        // 100% = 기본값("reset"), 그 외 = 변경된 값("change")
                        String percent = SmartMovingConfig.Config.getSpeedPercent();
                        String msgKey = "100".equals(percent)
                            ? "smartmoving.message.speed.client.reset"
                            : "smartmoving.message.speed.client.change";
                        if (client.player != null)
                            client.player.sendMessage(Text.translatable(msgKey, percent));
                    });
                } else {
                    boolean isRemote = isRemoteServer(client);
                    String key = isRemote
                        ? "smartmoving.message.speed.illegal.remote"
                        : "smartmoving.message.speed.illegal.local";
                    client.execute(() -> {
                        if (client.player != null) client.player.sendMessage(Text.translatable(key));
                    });
                }
            });
    }

    // 원본: SmartMovingComm.isConnectedToRemoteServer() — MinecraftServer.getServer()==null 등 3조건
    // 1.21.1: client.getServer()가 null이면 원격 서버 (통합 서버 없음)
    private static boolean isRemoteServer(MinecraftClient client) {
        return client.getServer() == null;
    }

    // ── 13-1/13-2: processConfigContentPacket ────────────────────────────────
    //
    // 원본: SmartMovingComm.processConfigContentPacket / processConfigPacket (SmartMovingComm.md 확인)
    // config_system.md 2-4 / 3-4 기반.
    //
    // content 값별 처리:
    //   null         → SM 완전 비활성. Config = INSTANCE 유지.
    //   length == 0  → 서버 설정 없음, 클라이언트에 위임. Config = INSTANCE.
    //   length > 0   → SERVER_CONFIG.loadFromArray(content) → Config = SERVER_CONFIG.
    //
    // 첫 수신(first=true) 시: sendConfigInfo 패킷 전송 (원본: SmartMovingConfig._sm_current = "3.2")
    // 주의: 서버 연결 해제 시 Config 복원은 registerConnectionEvents() DISCONNECT에서 처리.
    // A-26: 메시지 문자열 원본 확인 완료. 키 → en_us.json smartmoving.message.config.server.*
    private static void processConfigContentPacket(String[] content) {
        // 🔴 (2026-05-05 사용자 요청 — "config 는 자기 자체 toggle. 다른 player 무영향"):
        //   원본은 server admin 의 명시 활성 시만 broadcast 의도. 우리 server 매핑이
        //   무조건 broadcast → 모든 client Config 변경 → 모든 SM disabled BUG.
        //   해결: Config 자체 변경 제거. server-broadcast 무시 — 모든 client 자기 INSTANCE
        //   사용. 자기 disabled = 자기 SM state 갱신 안 함 → packet 송신 안 함 → 다른
        //   client 측에서 자기 vanilla 표시 (= 정확).
        //   SERVER_CONFIG 자체 갱신은 server-side 옵션 (= block-code 등) 이 사용 가능하도록
        //   유지. 그러나 client Config 는 INSTANCE 그대로.
        MinecraftClient client = MinecraftClient.getInstance();
        if (content == null || content.length == 0) {
            // 메시지만 (= 원본 message). Config 변경 안 함.
            return;
        }
        // SERVER_CONFIG 갱신 (= server-side 일부 옵션 read 용. client Config 무관).
        SmartMovingConfig.SERVER_CONFIG.loadFromArray(content);
        // ConfigInfo 송신 (= server 가 client 버전 인식).
        ClientPlayNetworking.send(new SmartMovingNetwork.ConfigInfoPayload(SmartMovingConfig.SM_VERSION));
    }

    // ── 4-4: processBlockCode ────────────────────────────────────────────────

    // 원본: SmartMovingComm.processBlockCode 를 채팅 히스토리 직접 스캔(Reflect+GuiNewChat)으로 처리.
    // 1.21.1: ClientReceiveMessageEvents.ALLOW_GAME 이벤트로 대체. 메시지 수신 시 즉시 처리 (updateCounter<10 스캔 불필요).
    // 원본 chatMessageList.remove(i--) 대응: ALLOW_GAME에서 false 반환으로 채팅 억제.
    private static void registerGameMessageHandler() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            // overlay=true는 액션바(핫바 위 표시) → 블록 코드 대상 아님 (B-19 확인)
            if (overlay) return true;
            String text = message.getString();
            if (text.startsWith("§0§1") && text.endsWith("§f§f")) {
                processBlockCode(text);
                return false; // 원본: chatMessageList.remove — 채팅창에서 억제
            }
            return true;
        });
    }

    // 원본: SmartMovingComm.processBlockCode(String text) — public static boolean
    // 1.21.1: SmartMovingConfig.Config 필드 직접 설정으로 단순화 (C-11: Config 전환 연동).
    //
    // 형식: "§0§1...§f§f" (앞 4자=시작마커, 뒤 4자=끝마커)
    // codes에 포함된 §코드에 해당하는 기능을 설정값으로 변경:
    //   §0 → baseClimb = true ("standard"), §1~§b → 각 기능 = false
    // codes에 없는 코드는 변경하지 않음 (현재 값 유지).
    //
    // Text.getString() §코드 포함 여부: 서버가 LiteralText로 전송한 경우 포함 가능 (B-19 확인).
    // 호출 전 마커 검사(`§0§1`/`§f§f`)는 registerGameMessageHandler에서 완료됨.
    private static void processBlockCode(String text) {
        String codes = text.substring(4, text.length() - 4);
        SmartMovingConfig cfg = SmartMovingConfig.Config;

        // §0 → _baseClimb="standard" → _isFreeBaseClimb/Smart/Simple 모두 false
        if (codes.contains("§0")) { cfg.freeClimb = false; cfg.simpleClimb = false; cfg.smartClimb = false; }
        if (codes.contains("§1")) cfg.freeClimb = false;
        if (codes.contains("§2")) cfg.ceilingClimbing = false;
        if (codes.contains("§3")) cfg.swim = false;
        if (codes.contains("§4")) cfg.dive = false;
        if (codes.contains("§5")) cfg.crawl = false;
        if (codes.contains("§6")) cfg.slide = false;
        if (codes.contains("§7")) cfg.fly = false;
        if (codes.contains("§8")) cfg.jumpCharge = false;
        if (codes.contains("§9")) cfg.headJump = false;
        if (codes.contains("§a")) cfg.angleJumpSide = false;
        if (codes.contains("§b")) cfg.angleJumpBack = false;
    }
}
