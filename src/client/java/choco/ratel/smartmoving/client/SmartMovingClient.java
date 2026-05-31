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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        // 월드 / 서버 접속 시 채팅창에 브랜드 + 링크 메시지 전송 (self only).
        // 1줄: ──────────────  (상단 테두리)
        // 2줄: [SmartMoving] by View-Studio
        // 3줄: [GitHub]  ·  [Support ☕]
        // 4줄: ──────────────  (하단 테두리)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    if (client.player == null) return;
                    client.player.sendMessage(buildBorderLine(), false);
                    client.player.sendMessage(buildBrandLine(), false);
                    client.player.sendMessage(buildLinksLine(), false);
                    client.player.sendMessage(buildBorderLine(), false);
                }));

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

    /** 월드 접속 시 채팅에 표시할 브랜드 + 링크 메시지. */
    private static final String GITHUB_URL = "https://github.com/View-Studio/SmartMoving";
    private static final String SUPPORT_URL = "https://ko-fi.com/viewstudio";

    /** 가로 테두리 선 (상/하) — 공백 + strikethrough 으로 끊김 없는 연속 선. */
    private static Text buildBorderLine() {
        return Text.literal(" ".repeat(40))
                .styled(s -> s.withColor(Formatting.DARK_GRAY).withStrikethrough(true));
    }

    /** 1줄: 브랜드 + 작성자. */
    private static Text buildBrandLine() {
        MutableText tag = Text.literal("[SmartMoving]")
                .styled(s -> s.withColor(Formatting.GOLD));

        MutableText by = Text.literal(" by ")
                .styled(s -> s.withColor(Formatting.GRAY));

        MutableText author = Text.literal("View-Studio")
                .styled(s -> s.withColor(Formatting.WHITE));

        return Text.empty().append(tag).append(by).append(author);
    }

    /** 2줄: 클릭 가능한 GitHub + Support 링크. */
    private static Text buildLinksLine() {
        MutableText sep = Text.literal("  ·  ").styled(s -> s.withColor(Formatting.DARK_GRAY));

        MutableText github = Text.literal("[GitHub]")
                .styled(s -> s
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Open repository\n" + GITHUB_URL))));

        MutableText support = Text.literal("[Support ☕]")
                .styled(s -> s
                        .withColor(Formatting.YELLOW)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SUPPORT_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Buy me a coffee\n" + SUPPORT_URL))));

        return Text.empty().append(github).append(sep).append(support);
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
                    boolean wasHJ = target.isHeadJumping;
                    boolean wasICC = target.isClimbCrawling;
                    // 🔵 fix #154 (2026-05-22) — swim/dive → crawl direct transition snapshot (remote sink fix).
                    boolean wasSwim_fix154 = target.isSwimming_sm;
                    boolean wasDive_fix154 = target.isDiving;
                    boolean wasCR_fix154 = target.isCrawling;
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
                    // 🔴 (slide→crawl 박스 부유 fix, 2026-05-13) remote 측 POSE 즉시 갱신.
                    //   사용자 보고 BUG: REMOTE 측 slide → crawl 자동 전환 *사이* (~2 tick = 100ms)
                    //     동안 박스 자체가 ground 위 1m 부유 = "STANDING 콜리전 변환" 시각.
                    //   root cause (= dump 검증):
                    //     - SmartMovingState packet 도착 → isCrawling=true 갱신 + calculateDimensions
                    //       → sm_getBaseDimensions_client smSmall 분기 5 매치 → dim=(0.8, 0.62).
                    //     - vanilla EntityDataS2CPacket (POSE) broadcast lag ~2 tick → POSE=SLIDING
                    //       잔존.
                    //     - MixinEntity.sm_offsetBoundingBoxForFlying 가드 `eye<=1 && pose!=SLIDING`
                    //       매치 X (= pose==SLIDING 잔존) → mixin offset 활성 → bb=y+1.
                    //     - → 박스 ground top + 1m 부유 frame ~2 tick. 머리 위치 STANDING 박스 머리
                    //       (= y+1.8) 와 동일 → 사용자 시각 "STANDING 콜리전 변환".
                    //   self side: PlayerEntity.tick → updatePose → sm_updatePose_client 동일 tick
                    //     안 setPose(SWIMMING) 호출 → 다음 render frame bb=y. 부유 frame 거의 X.
                    //   해결: remote 측 packet lambda 안 setPose 즉시 호출. self side
                    //     sm_updatePose_client 와 동일 4 분기 매핑.
                    if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity apPose
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        net.minecraft.entity.EntityPose targetPose = null;
                        if (target.isCrawling && !target.isClimbing) {
                            targetPose = net.minecraft.entity.EntityPose.SWIMMING;
                        } else if (target.isHeadJumping || target.isSliding) {
                            targetPose = net.minecraft.entity.EntityPose.SLIDING;
                        } else if (target.isSwimming_sm || target.isDiving) {
                            targetPose = net.minecraft.entity.EntityPose.SWIMMING;
                        } else {
                            // 🔴 (벽 충돌 cycle fix, 2026-05-13) orphan POSE 잔존 frame 의 박스 부유 차단.
                            //   사용자 보고 BUG: 슬라이딩 중 벽 충돌 시 박스 위로 튀는 frame.
                            //   root cause (= dump 검증):
                            //     - self side wouldWantCrawl 식 + isClimbing 가드 매트릭스로 cycle 발생
                            //       (= slide→crawl→standing→crawl). tick N: isCR=true→false PKT 매치.
                            //     - remote 측 PKT 처리: target.isCrawling=false 갱신. 우리 4 분기 매트릭스
                            //       매치 X → setPose 호출 X.
                            //     - vanilla POSE broadcast lag ~1 tick → POSE=SLIDING 잔존.
                            //     - mixin offset 가드 `eye<=1 && pose!=SLIDING` 매치 X (= pose==SLIDING)
                            //       → mixin offset 활성 → bb=y+1 (= 박스 ground 위 1m 부유).
                            //   해결: 모든 SM phase 미활성 + POSE=SWIMMING/SLIDING 잔존 시 setPose(STANDING)
                            //     강제. mixin offset 가드 매치 → 차단 → bb=y.
                            //   회귀 차단:
                            //     - vanilla 자체 자세 (sleep/elytra/spin_attack 등): isFallFlying / isInSwimmingPose
                            //       가드. 매치 시 우리 fix skip.
                            //     - 일반 STANDING: curPose=STANDING → 매치 X (조건 SWIMMING|SLIDING) → skip.
                            //     - vanilla 자체 수영 (물 안): isInSwimmingPose=true → skip.
                            net.minecraft.entity.EntityPose curPose = apPose.getPose();
                            if ((curPose == net.minecraft.entity.EntityPose.SWIMMING
                                    || curPose == net.minecraft.entity.EntityPose.SLIDING)
                                    && !apPose.isInSwimmingPose()
                                    && !apPose.isFallFlying()) {
                                targetPose = net.minecraft.entity.EntityPose.STANDING;
                            }
                        }
                        if (targetPose != null && apPose.getPose() != targetPose) {
                            apPose.setPose(targetPose);
                            if (entity instanceof net.minecraft.entity.LivingEntity living2) {
                                living2.calculateDimensions();
                            }
                        }
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
                        // 🔴 fix #93 (2026-05-13, dump 검증 — 사용자 보고 "여우무빙 → 슬라이딩 동시 진입 시 잠깐 잠김"):
                        //   기존 newY = remoteSlideEnter.getY() - 1.0:
                        //     진입 직전 client.y 가 *헤드점프 진행 중 server.y broadcast lag* 로 잠긴
                        //     상태 (= ground 위 -0.022m) 시 newY = 잠긴 위치 - 1m → setPos 후 bb 발 =
                        //     ground 위 -0.022m (= 잠긴 상태 유지). 다음 broadcast 도착 후 vanilla lerp
                        //     ~3 tick 회복. 사용자 시각 "조금 잠겼다 회복".
                        //   해결: fix #92 패턴 차용. ground top 직접 측정 → newY = groundY - 1.
                        //     정상 진입: groundY = ground → 동일 결과. 잠긴 진입: groundY = ground →
                        //     ground 정렬. 회귀 X.
                        double bbMinBefore93 = remoteSlideEnter.getBoundingBox().minY;
                        double groundY93 = choco.ratel.smartmoving.client.SmartMovingClientState
                                .getMaxPlayerSolidBetween(remoteSlideEnter, bbMinBefore93 - 1.5, bbMinBefore93 + 0.5, 0);
                        double newY = groundY93 - 1.0;
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
                    // 🔵 fix #100 (2026-05-20, 사용자 보고 "슬라이딩 → 절벽 → 헤드점프 자동 전환 시 REMOTE
                    //   1칸 위 잠깐 올라갔다 정상 복귀"):
                    //   진짜 path: SmartMovingClientState L2309-L2315 의 SlideToHeadJumping 자동 전환.
                    //     = `isSliding=false; isHeadJumping=true; isAerodynamic=true` 비트만 set.
                    //     self side entity.y push 없음 (= L2389 push 가드 `!isHeadJumping` 매치 X).
                    //     POSE 측 line 126-127 `isHJ || isSL → SLIDING` → POSE=SLIDING 유지 → mixin offset
                    //     활성 유지 (MixinEntity L122 가드 `pose!=SLIDING` 매치 X) → bb=entity.y+1 유지.
                    //     = self 시각 무변화.
                    //   remote 측 본 분기: wasSliding=true && target.isSliding=false 매치.
                    //     slideBoxMinYBefore = remote.y+1 (calc dim 전 mixin 활성),
                    //     newY = remote.y (calc dim 후 entity.y, mixin offset 여전 활성).
                    //     willDrop = (remote.y+1 - remote.y) > 0.5 = **false positive true**.
                    //     → setPos(remote.y+1) + lerp cancel → POSE=SLIDING 유지로 mixin offset 활성 →
                    //     bb=(remote.y+1)+1=ground+2m → "1칸 위 부유" 시각. 다음 broadcast srvY=self.y
                    //     도착 시 lerp 재개 → -1m down → 정상 위치 복귀.
                    //   해결: 가드 `!target.isHeadJumping` 추가 — self side L2389 `!isHeadJumping` 가드 1:1
                    //     매칭. slide → HJ 자동 전환 시 push 차단. 다른 slide-exit 케이스 (crawl/standing/
                    //     비행) 는 isHJ=false 라 통과 → 기존 동작 유지.
                    if (wasSliding && !target.isSliding && !target.isHeadJumping
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

                    // 🔴 (BUG 2 fix, 2026-05-13) remote 헤드점프 착지 1칸 down jump 차단.
                    //   self side L2196 EXIT 분기 (SmartMovingClientState):
                    //     `wasHJ && !isHJ && onGround` → standupIfPossible(false, true) → entity.y +1m push.
                    //     검증 (= log_temp.txt 시도 1~3): SELF-EXIT-POST dy=1.000.
                    //   server side: self c2s SmartMovingState packet 이 c2s position packet 보다 빨리 처리
                    //     → SERVER-RECV 시 server.y = push 전 (70.000). 다음 server tick 에 71.000 갱신.
                    //   broadcast: SmartMovingState packet 이 EntityPositionS2CPacket 보다 ~1 server tick
                    //     빨리 broadcast (= reference_vanilla_server_tick_order, BUG-7/15/D 동일 메커니즘).
                    //   remote: PKT 도착 시 remote.y = 70.X (= lerp 가 srvY=70 추격 중). bbMin=remote.y+1
                    //     (mixin offset 활성, MixinEntity.sm_offsetBoundingBoxForFlying 의 SLIDING POSE 가드).
                    //     box 발은 ground 위 (= 잠수 X). **그러나 모델 origin = entity.y = 70.X = ground
                    //     아래 0.5~0.8m → 사용자 시각 "1칸 잠수"**. srvY=71 갱신 후 lerp 추격 ~3~15 tick
                    //     (= 150~750ms) 동안 잔존.
                    //   해결: BUG-7/15 동일 패턴 — packet lambda 안 즉시 setPos(groundY) + lerp cancel.
                    //     newY 식 = ground top 직접 측정 (= getMaxPlayerSolidBetween, yMax<bbMin 으로 천장
                    //     매치 차단). srvY+1 보다 정확 (= srvY 가 진입 frame 에 부정확한 broadcast 값 잔존
                    //     가능, 시도 3/5 검증).
                    //   가드 `!target.isSliding && !target.isCrawling`:
                    //     - 자체슬라이딩 자동 전환 (= 여우무빙, 시도 4/5): self.y 변화 X → fix 적용 X 가 정합.
                    //     - 헤드점프 → crawl 전환 등: 별도 fix 분기.
                    // 🔵 fix #99 (2026-05-14) — HJ→CR 직접 전환 분기.
                    //   사용자 보고: 헤드점프 + 벽 충돌 + 떨어짐 + GRAB+SNEAK 누름 → 자동 엎드리기
                    //     전환 시 REMOTE 잠깐 잠긴 visual.
                    //   진짜 path: HJ:true→false + CR:false→true 동시.
                    //   = self side `standupIfPossible` → `toSlidingOrCrawling` 의 else 분기 (= grab
                    //     매치 X) → `toCrawling()` 호출.
                    //   기존 fix 분기 빈공간: fix #93 (SL 진입) / fix #98 v5 (HJ→standing) / fix #90
                    //     (SL→CR) 모두 가드 미매치.
                    //   newY 식 = srvY + 1m (= server side fix #99 v3 push 후 broadcast 위치 정확).
                    //     groundY 직접 측정 (= fix #92/#93 패턴) 은 진입 frame 박스 dim 변화 (=
                    //     calculateDimensions 후 isCrawling dim) 로 yMax<ground top → clamp 잘못.
                    if (wasHJ && !target.isHeadJumping
                            && !wasSliding && !target.isSliding
                            && target.isCrawling
                            && !wasIcc && !target.isClimbCrawling
                            && !target.isClimbing && !target.isCeilingClimbing
                            && !target.isSwimming_sm && !target.isDiving
                            && !target.isFlying && !target.isLevitating
                            && !target.isAngleJumping() && !target.isRopeSliding
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteHJCr
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accHJCr =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteHJCr;
                        double newYHJCr = accHJCr.sm_getServerY() + 1.0;
                        remoteHJCr.setPosition(remoteHJCr.getX(), newYHJCr, remoteHJCr.getZ());
                        remoteHJCr.lastRenderY = newYHJCr;
                        remoteHJCr.prevY = newYHJCr;
                        accHJCr.sm_setServerY(newYHJCr);
                        accHJCr.sm_setBodyTrackingIncrements(0);
                    }
                    // 🔵 fix #154 (2026-05-22) — swim/dive → crawl direct transition remote sink fix.
                    //   사용자 보고: "swim → 1칸 공간 진입 자동 crawl 순간 remote 시 상대가 잠깐 땅으로 들어갔다 올라옴".
                    //
                    //   진짜 path:
                    //   - self side: fix #150 fromSwimmingOrDiving 분기 1 → setPos(y+1) + isCrawling=true.
                    //     self.y c2s = ground. server.y = ground (= c2s movement packet 자동 동기화).
                    //   - server side: fix #150 calculateDimensions 만 호출 (= mirror push 없음, self setPos 가
                    //     c2s 로 server 동기화). broadcast srvY = ground.
                    //   - remote side: 진입 시점 remote.y = ground - 1 (= 이전 swim broadcast srvY 의 lerp 결과).
                    //     CR dim → mixin offset 차단 → 박스 발 = remote.y = ground - 1 → 잠긴 visual.
                    //     vanilla lerp 가 srvY=ground 추격 → ~3 tick 도달.
                    //
                    //   메모리: [project_remote_landing_sink_pattern] fix #92/#93 패턴 (= groundY 직접 측정).
                    //   [feedback_remote_setpos_newY_srvY_plus_1]: server side mirror push 없으면 groundY-1 식.
                    //
                    //   fix: remote packet lambda 안 setPos(groundY-1) + lerp cancel. fix #92 패턴 1:1.
                    //   newY = groundY - 1 (= self.y 와 동기, mixin offset 차단 후 박스 발 = entity.y = ground).
                    // 🔵 fix #154 v4 (2026-05-22): multi-step transition history flag.
                    //   log 실측: server side broadcast 가 dv 변경 vs cr 변경 별도 2 packet. REMOTE 측
                    //   lambda 매 호출 was[dv=true]→cur[dv=false cr=false] (= dv 종료) +
                    //   was[dv=false]→cur[dv=false cr=true] (= cr 진입) 2 step. single-transition 가드 매치 X.
                    //   해결: swim/dive 종료 detection 시 flag set + crawl 진입 detection 시 flag 5 tick 안 검사.
                    boolean _swimEndedFix154 = (wasSwim_fix154 || wasDive_fix154)
                            && !target.isSwimming_sm && !target.isDiving;
                    if (_swimEndedFix154 && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity _rememF154
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        target.smRecentSwimDiveEndTick = _rememF154.age;
                    }
                    boolean _recentSwimEndedFix154 = entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity _checkRecent
                            && (_checkRecent.age - target.smRecentSwimDiveEndTick) >= 0
                            && (_checkRecent.age - target.smRecentSwimDiveEndTick) <= 10;
                    if (_recentSwimEndedFix154
                            && !wasCR_fix154 && target.isCrawling
                            && !wasHJ && !target.isHeadJumping
                            && !wasSliding && !target.isSliding
                            && !wasIcc && !target.isClimbCrawling
                            && !target.isClimbing && !target.isCeilingClimbing
                            && !target.isFlying && !target.isLevitating
                            && !target.isAngleJumping() && !target.isRopeSliding
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteSwCr
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        // 🔵 (2026-05-22 fix #154 v3) fix #99 v4 패턴 1:1 — newY = srvY + 1.
                        //   이전 v2 (groundY 직접 측정) 식: getMaxPlayerSolidBetween 의 clamp 가 잠긴 시
                        //     ground top 매치 X (= yMax 작음). v2 식 정상 위치 시도 ceiling 매치 (= yMax 큼).
                        //   해결: fix #99 v4 식 차용 — newY = srvY + 1. server side fix #153 v2 mirror push
                        //     적용 → server.y = self.y = ground. broadcast S2C state 시점 srvY prev = ground-1
                        //     (= 이전 broadcast 잔존). newY = prev srvY + 1 = ground = self.y. REMOTE.y 정렬.
                        //   [project_remote_hjcr_direct_transition_fix] / [feedback_remote_setpos_newY_srvY_plus_1].
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accSwCr =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteSwCr;
                        double newYSwCr = accSwCr.sm_getServerY() + 1.0;
                        remoteSwCr.setPosition(remoteSwCr.getX(), newYSwCr, remoteSwCr.getZ());
                        remoteSwCr.lastRenderY = newYSwCr;
                        remoteSwCr.prevY = newYSwCr;
                        accSwCr.sm_setServerY(newYSwCr);
                        accSwCr.sm_setBodyTrackingIncrements(0);
                        target.smRecentSwimDiveEndTick = -9999;  // 1회 한정 clear.
                    }
                    if (wasHJ && !target.isHeadJumping
                            && !target.isSliding && !target.isCrawling
                            && entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity remoteHJ
                            && !(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) {
                        choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor accH =
                                (choco.ratel.smartmoving.mixin.client.MixinLivingEntityAccessor)(Object) remoteHJ;
                        double bbMin = remoteHJ.getBoundingBox().minY;
                        // 🔴 fix #92 (2026-05-13, dump 검증 — 사용자 보고 "가끔 잠깐 잠겼다 회복"):
                        //   기존 yMax = bbMin - 0.01: 잠긴 상태 (= bbMin < ground top) 진입 시 검색 범위
                        //   ground top 매치 X → groundY = bbMin - 0.01 (= 잠긴 위치 그대로).
                        //   가드 |newY - curY| > 0.05 매치 X (얕은 잠김 0.010m) → setPos 적용 X →
                        //   srvY = 잠긴 위치 + bti=0 lerp cancel → client.y 잠긴 채 유지 → 다음 broadcast
                        //   (= srvY=ground top) 도착 시 vanilla lerp ~3 tick 회복.
                        //   fix: yMax = bbMin + 0.5 (= 위쪽 0.5m 확장) → 잠긴 상태도 ground top 매치 →
                        //   groundY 정확. 천장 (= bbMin+1.0 부근) 매치 X (= 1칸 공간 안전).
                        //   + 가드 0.005m 완화 → 얕은 잠김 (= 0.010m) 도 setPos 적용.
                        double groundY = choco.ratel.smartmoving.client.SmartMovingClientState
                                .getMaxPlayerSolidBetween(remoteHJ, bbMin - 1.5, bbMin + 0.5, 0);
                        // 🔵 (2026-05-14, fix #98 v3 — prevY 사용): client tick 순서 분석 결과.
                        //   1) networkHandler.tick → packet callback queue
                        //   2) world.tick → entity.tick → lerpPosAndRotation (= REMOTE.y 1-step 진행)
                        //   3) processTasks → packet 처리 (= 본 분기)
                        //   4) render
                        //   = packet 처리 시점 remoteHJ.getY() 가 이미 lerp 진행 후 (= ground 도달).
                        //   → hold 조건 (curY > groundY+0.005) 미매치 → cycle 2-9 fix 효과 X.
                        //   해결: prevY (= 이전 frame y, lerp 진행 전) 사용. EXIT 직전 frame 의 공중
                        //     위치 정확 검출. lastRenderY 와 prevY 둘 다 같은 값 (= resetPosition 직후).
                        double curY = remoteHJ.prevY;
                        double diff = curY - groundY;  // 양수 = 공중, 음수 = 잠긴
                        // 🔵 (2026-05-14, fix #98 위치 기반) fix #92 가드 분리 — 잠긴 vs 공중.
                        //   잠긴 상태 (= curY < groundY - 0.005): 기존 fix #92 — setPos + lerp cancel.
                        //   공중 상태 (= curY > groundY + 0.005): 사용자 보고 3번 BUG cause. setPos 안 함
                        //     (= REMOTE.y 가 자체 vanilla lerp 으로 자연 ground 도달). 그 사이 자세 visual
                        //     hold flag set + groundY 저장. setupTransforms X/Y 회전 분기 가드에 OR 로
                        //     추가됨 → 자세 유지. sm_tickStatsForRemote TAIL 의 위치 비교 (= REMOTE.y <=
                        //     groundY+0.005) 시 hold 해제. tick timeout 아님 (= 사용자 원칙 일치).
                        if (diff < -0.005) {
                            // 잠긴 상태 — 기존 fix #92.
                            double newY = groundY;
                            remoteHJ.setPosition(remoteHJ.getX(), newY, remoteHJ.getZ());
                            remoteHJ.lastRenderY = newY;
                            remoteHJ.prevY = newY;
                            // 🔴 lerp cancel — BUG-7 v25.3 / BUG-15 / BUG B 동일 패턴.
                            accH.sm_setServerY(newY);
                            accH.sm_setBodyTrackingIncrements(0);
                        } else if (diff > 0.005) {
                            // 🔵 (fix #98 v5) 공중 상태 — setPos(prevY) + 4-동기화 + lerp cancel + hold.
                            //   dump 검증: srvY=71 (= self.y push 전 잔존) → REMOTE.y lerp 가 잠긴 위치
                            //     추격 진행 → getY < groundY → 사용자 보고 "착지 시 잠김" cause.
                            //   해결: setPos(curY=prevY) + lerp cancel 으로 REMOTE.y 가 공중 위치
                            //     잔존. 다음 broadcast (= server.y=ground 도달) 도착 시 lerp 재개 →
                            //     REMOTE.y → ground. 자세 hold 동안 잠긴 trajectory 차단.
                            //   메모리 패턴: feedback_remote_setpos_lerp_cancel 4-동기화.
                            double newY = curY;  // prevY = 공중 위치 잔존.
                            remoteHJ.setPosition(remoteHJ.getX(), newY, remoteHJ.getZ());
                            remoteHJ.lastRenderY = newY;
                            remoteHJ.prevY = newY;
                            accH.sm_setServerY(newY);
                            accH.sm_setBodyTrackingIncrements(0);
                            // 자세 visual hold flag set.
                            target.smRemoteHJVisualHold = true;
                            target.smRemoteHJExitGroundY = groundY;
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

                        // 분기 결정:
                        //   - 분기 2 (standing 착지): !crawl && !slide && groundClose → serverY=groundY,
                        //     bti 보장 (>=3) → vanilla lerp 점진 진행. setPos 안 함 (= visual jump 차단).
                        //   - 분기 3-A (자연 crawl, BUG E): isCrawling && groundClose → setPos(srvY+1.0)
                        //     + lerp cancel. self side L3908 push +1m 매핑.
                        //   - 분기 3-B (sneak+grab slide): isSliding → fix skip (BUG D: self.y 변화 X
                        //     검증됨, vanilla lerp 자체 처리).
                        //   - 분기 1 (gap>=1): groundClose=false → fix skip.
                        if (!target.isSliding && !target.isCrawling && groundClose) {
                            // 🔴 BUG E 회귀 fix (2026-05-12): setPos+bti=0 = visual jump root.
                            //   진입 frame remote.y 가 vanilla lerp 진행 중 (예: 71.254) 일 때
                            //   setPos(groundY=71.000) + bti=0 = 즉시 0.254m visual jump down.
                            //   해결: setPos/lastRenderY/prevY 변경 제거. serverY (= lerp target) 만
                            //   newY 로 set + bti 유지 (0 이면 3 강제) → vanilla lerp 가 점진 진행 →
                            //   잠수 차단 효과 유지 + visual jump 제거.
                            double newY = groundY;
                            accF.sm_setServerY(newY);
                            int curBti = accF.sm_getBodyTrackingIncrements();
                            if (curBti < 3) {
                                accF.sm_setBodyTrackingIncrements(3);
                            }
                        } else if (target.isCrawling && !target.isSliding && groundClose) {
                            // 🔴 BUG E fix (2026-05-12): 분기 3-A 자연 crawl push +1m 매핑.
                            //   self side `SmartMovingClientState.standupIfPossible` L3908 가드 matches:
                            //     `wasSmallBox && isCrawling && wasFlying` → setPos(y + 1.0).
                            //   self.y_final = srvY + 1.0. 여기서 srvY 는 server 가 곧 broadcast 할 정확값
                            //   (= 비행 종료 직전 broadcast). self side 의 c2s packet 이 server 에 도달 +
                            //   server broadcast → ~1 tick 후 vanilla srvY 갱신 = 71.
                            //   해결: remote 측에서 *지금 즉시* setPos(srvY+1) + lerp cancel.
                            //
                            //   기존 BUG D fix 가 "분기 3 = self.y 변화 X = fix skip" 으로 가정한 오역
                            //   정정 — sneak+grab slide 케이스 만 self.y 변화 X. 자연 crawl 케이스는 +1m.
                            double newY = srvY + 1.0;
                            if (newY != remoteFly.getY()) {
                                remoteFly.setPosition(remoteFly.getX(), newY, remoteFly.getZ());
                                remoteFly.lastRenderY = newY;
                                remoteFly.prevY = newY;
                            }
                            accF.sm_setServerY(newY);
                            accF.sm_setBodyTrackingIncrements(0);
                        }
                        // 분기 1 (gap>=1) / 분기 3-B (sneak+grab slide) — fix skip, vanilla lerp 자체 처리.
                    }
                });
            });

        // 🔵 Stats: self → server → REMOTE 매 tick stats angle 동기화 (2026-05-14, Option F fix #97).
        //   원인: vanilla lerpPosAndRotation 가 REMOTE 의 위치를 5-tick 균등 분할 보간 → realDxYz
        //     평탄화 + spike → atan2 결과 thetaT 가 self trajectory 와 다른 흐름 → lerpFadeAngle
        //     0.2 lerp 가 peak 추격 시간 부족 → 헤드점프 마지막 머리 회전 부족.
        //   적용: packet 도착 시 sm.smRemoteStatsVerticalAngle/HorizontalAngle_fromPacket set.
        //     sm_tickStatsForRemote TAIL 에서 자체 stats.calculate 결과를 packet 값으로 덮어쓰기.
        ClientPlayNetworking.registerGlobalReceiver(SmartMovingNetwork.StatsPayload.ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    ClientWorld world = client.world;
                    if (world == null) return;
                    Entity entity = world.getEntityById(payload.entityId());
                    if (!(entity instanceof AbstractClientPlayerEntity ap)) return;
                    if (ap == client.player) return;  // 안전 가드 — server 가 self 제외 broadcast.
                    SmartMovingClientState target = SmartMovingClientState.get(ap);
                    target.smRemoteStatsVerticalAngle_fromPacket = payload.verticalAngle();
                    target.smRemoteStatsHorizontalAngle_fromPacket = payload.horizontalAngle();
                    // 🔵 (2026-05-14 fix #97-v2): packet 도착 시 sm.stats 직접 즉시 set.
                    //   원인: vanilla MinecraftClient.tick 순서 →
                    //     1) networkHandler.tick (= packet callback + client.execute queue)
                    //     2) world.tick → entity.tick → sm_tickStatsForRemote → stats.calculate + 덮어쓰기
                    //     3) processTasks → client.execute lambda 실행 → fromPacket set
                    //     4) render → setupTransforms
                    //   = 진입 tick 에 sm_tickStatsForRemote 시점 fromPacket=NaN → 덮어쓰기 skip →
                    //     stats.currentVerticalAngle = atan2(realDy=0, ...) = 0 잔존 →
                    //     setupTransforms 시 target=π/2 (= 90° 즉시 점프) → 사용자 보고 "진입 끊김".
                    //   fix: packet receive lambda 안 (= processTasks 안) 에서 stats 도 즉시 set.
                    //     다음 entity.tick 시 sm_tickStatsForRemote 가 stats.calculate (= 0 set) 후
                    //     덮어쓰기 (= packet 값) → 같은 결과. 진입 frame 만큼 1 tick stale 차단.
                    target.stats.currentVerticalAngle = payload.verticalAngle();
                    target.stats.currentHorizontalAngle = payload.horizontalAngle();
                    target.stats.prevHorizontalAngle = payload.horizontalAngle();
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
