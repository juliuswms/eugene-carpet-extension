package com.eugene;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.PriorityQueue;

// Kinda hacky but I didn't find a better way.
// This got more hacky when I realized that server.getTicks() advances ticks even when game is frozen

public final class TickDelayer {
    private TickDelayer() {}

    private static long gameTick = 0;

    private record Job(long runAt, Runnable task) implements Comparable<Job> {
        @Override public int compareTo(Job o) { return Long.compare(this.runAt, o.runAt); }
    }

    private static final PriorityQueue<Job> queue = new PriorityQueue<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TickDelayer::tick);
    }

    public static void after(MinecraftServer server, int delayTicks, Runnable task) {
        long runAt = gameTick + Math.max(0, delayTicks);
        queue.add(new Job(runAt, task));
    }

    private static void tick(MinecraftServer server) {
        if(!server.getTickManager().shouldTick()) return;

        gameTick++;
        server.getTickTimes();
        while (!queue.isEmpty() && queue.peek().runAt <= gameTick) {
            queue.poll().task.run();
        }
    }
}