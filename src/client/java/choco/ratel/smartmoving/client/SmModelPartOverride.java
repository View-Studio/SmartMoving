package choco.ratel.smartmoving.client;

import org.joml.Quaternionf;

/**
 * ModelPart 의 vanilla ZYX 회전 매트릭스를 우회하기 위한 interface.
 *
 * vanilla ModelPart.rotate 는 `rotationZYX(roll, yaw, pitch)` hardcoded.
 * sliding/back jump 등 큰 yaw (±π/2) 시 JOML getEulerAnglesZYX 분해 부정확 (gimbal lock).
 * 의도 quaternion 직접 적용으로 분해 손실 회피.
 *
 * 사용법:
 *   ((SmModelPartOverride)(Object)part).sm_setOverrideQuat(q);
 *   ((SmModelPartOverride)(Object)part).sm_clearOverrideQuat();
 */
public interface SmModelPartOverride {
    void sm_setOverrideQuat(Quaternionf q);
    void sm_clearOverrideQuat();
    boolean sm_hasOverrideQuat();
    Quaternionf sm_getOverrideQuat();
}
