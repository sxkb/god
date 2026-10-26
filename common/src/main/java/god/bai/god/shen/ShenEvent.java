package god.bai.god.shen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class ShenEvent {
    public final ShenEventType type;
    public final LivingEntity source;
    public final LivingEntity target;
    public final BlockPos pos;
    public final Level world;
    public final String chatMessage;
    public ShenRule rule;
    public int loopCount = 0;
    public final Map<String, String> params = new HashMap<>();

    public ShenEvent(ShenEventType type, LivingEntity source, BlockPos pos, Level world) {
        this(type, source, null, pos, world, "");
    }

    public ShenEvent(ShenEventType type, LivingEntity source, LivingEntity target, BlockPos pos, Level world) {
        this(type, source, target, pos, world, "");
    }

    public ShenEvent(ShenEventType type, LivingEntity source, LivingEntity target, BlockPos pos, Level world, String chatMessage) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.pos = pos;
        this.world = world;
        this.chatMessage = chatMessage;
    }

    public Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("type", type);
        context.put("source", source);
        context.put("target", target);
        context.put("pos", pos);
        context.put("world", world);
        context.put("chatMessage", chatMessage);
        return context;
    }
}
