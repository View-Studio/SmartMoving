package choco.ratel.smartmoving.client.skin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 개발 런 컨피그 전용 — 로컬 PNG 를 클라이언트 플레이어 스킨으로 고정.
 * 오프라인(--username) 모드에서 Mojang 스킨 미수신 → 기본 스티브 노출 문제 우회.
 *
 * 활성 조건: FabricLoader.isDevelopmentEnvironment() && SKIN_PATH 존재.
 * 첫 호출 (렌더 스레드) 시 NativeImageBackedTexture 로드 → TextureManager 등록.
 */
@Environment(EnvType.CLIENT)
public final class LocalSkinOverride {
    private static final Path SKIN_PATH = Path.of("C:/Work/minecraft/texture/my_skin.png");
    private static final Identifier SKIN_ID = Identifier.of("smartmoving", "local_dev_skin");

    private static boolean attempted = false;
    private static boolean ready = false;

    private LocalSkinOverride() {}

    public static Identifier get() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return null;
        if (!attempted) tryLoad();
        return ready ? SKIN_ID : null;
    }

    private static void tryLoad() {
        attempted = true;
        if (!Files.isRegularFile(SKIN_PATH)) {
            System.err.println("[SmartMoving] Local dev skin not found: " + SKIN_PATH);
            return;
        }
        try (InputStream in = Files.newInputStream(SKIN_PATH)) {
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            MinecraftClient.getInstance().getTextureManager().registerTexture(SKIN_ID, tex);
            ready = true;
            System.out.println("[SmartMoving] Local dev skin loaded: " + SKIN_PATH);
        } catch (IOException e) {
            System.err.println("[SmartMoving] Failed to load local dev skin: " + e);
        }
    }
}
