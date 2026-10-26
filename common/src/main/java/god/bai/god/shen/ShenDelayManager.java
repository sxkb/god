package god.bai.god.shen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShenDelayManager {
    private static final List<DelayedTask> tasks = new ArrayList<>();
    private static final List<DelayedTask> pendingTasks = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable task) {
        if (delayTicks <= 0) {
            task.run();
        } else {
            pendingTasks.add(new DelayedTask(delayTicks, task));
        }
    }

    public static void tick() {
        if (!pendingTasks.isEmpty()) {
            tasks.addAll(pendingTasks);
            pendingTasks.clear();
        }

        if (tasks.isEmpty()) return;
        Iterator<DelayedTask> it = tasks.iterator();
        while (it.hasNext()) {
            DelayedTask task = it.next();
            task.remaining--;
            if (task.remaining <= 0) {
                try {
                    task.runnable.run();
                } catch (Exception ignored) {
                    // 防止单个任务异常影响后续任务
                }
                it.remove();
            }
        }

    }

    public static void clearAll() {
        tasks.clear();
        pendingTasks.clear();
    }

    private static class DelayedTask {
        int remaining;
        final Runnable runnable;

        DelayedTask(int remaining, Runnable runnable) {
            this.remaining = remaining;
            this.runnable = runnable;
        }
    }
}
