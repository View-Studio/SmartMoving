package choco.ratel.smartmoving.state;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

public final class SmartMovingAttachments {

    public static final AttachmentType<SmartMovingState> STATE = AttachmentRegistry.createDefaulted(
            Identifier.of("smartmoving", "state"),
            SmartMovingState::new
    );

    public static void register() {
        // STATE 필드 참조로 클래스 로딩 및 AttachmentType 등록 보장
    }

    private SmartMovingAttachments() {}
}
