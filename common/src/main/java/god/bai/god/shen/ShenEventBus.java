package god.bai.god.shen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShenEventBus {
    private static final List<ShenLaw> activeLaws = new CopyOnWriteArrayList<>();

    public static void registerLaw(ShenLaw law) {
        if (law != null) {
            activeLaws.removeIf(l -> l.name.equals(law.name));
            if (law.enabled) {
                activeLaws.add(law);
            }
        }
    }

    public static void unregisterLaw(ShenLaw law) {
        if (law != null) {
            activeLaws.removeIf(l -> l.name.equals(law.name));
        }
    }

    public static void unregisterByName(String lawName) {
        activeLaws.removeIf(l -> l.name.equals(lawName));
    }

    public static void clearAll() {
        activeLaws.clear();
    }

    public static void fire(ShenEvent event) {
        if (event == null) return;

        for (ShenLaw law : activeLaws) {
            if (!law.enabled) continue;
            for (ShenRule rule : law.rules) {
                boolean matchEvent = rule.eventTypes.contains(event.type)
                    || rule.eventType == event.type;
                if (matchEvent) {
                    try {
                        ShenRuleExecutor.execute(rule, event);
                    } catch (Exception e) {
                        // 防止单条规则异常影响其他规则
                    }
                }
            }
        }
    }

    public static void fire(ShenEventType eventType, LivingEntity source, BlockPos pos, Level world) {
        fire(new ShenEvent(eventType, source, pos, world) {});
    }

    public static int getActiveLawCount() {
        return activeLaws.size();
    }

    public static List<ShenLaw> getActiveLaws() {
        return new ArrayList<>(activeLaws);
    }
}
