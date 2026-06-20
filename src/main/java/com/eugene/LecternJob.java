package com.eugene;

import carpet.utils.Messenger;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class LecternJob {
    String jobName;
    double estimatedTotalTime;
    ServerPlayerEntity target;
    ServerCommandSource source;
    List<Integer> rawInstructions;
    LecternBlockEntity lectern;
    int instructionIndex = 0;
    boolean active = false;
    int currentTickDelay;
    int homeTickDelay;
    int[] tickDelays;
    int tickDelayIndex;
    MinecraftServer server;

    public LecternJob(ServerPlayerEntity target, ServerCommandSource source, LecternBlockEntity lectern, LecternInstructions instructions) {
        this.target = target;
        this.source = source;
        this.rawInstructions = instructions.rawInstructions;
        this.lectern = lectern;
        this.tickDelays = instructions.tickDelays;
        this.homeTickDelay = instructions.homeTickDelay;

        this.instructionIndex = 0;
        this.tickDelayIndex = 0;
        this.currentTickDelay = tickDelays[0];
        this.server = target.getServer();
    }

    private void step(){
        if (instructionIndex >= rawInstructions.size()) {
            Messenger.m(source, "w Done.");
            LecternActions.stopLecternJob(target, source);
            return;
        }
        int instruction = rawInstructions.get(instructionIndex);
        currentTickDelay = tickDelays[tickDelayIndex];
        if(!LecternActions.turnToPage(target, instruction, source)) {
            Messenger.m(source, "r Stopped unexpectedly printing at: " + instructionIndex);
            active = false;
            return;
        }
        if(!active){
            LecternActions.turnToPage(target,2, source);
            Messenger.m(source, "w Stopped printing at: " + instructionIndex);
            return;
        }
        if (instruction == 1){
            stepTickDelay();
        }else if(instruction != 2){
            currentTickDelay = homeTickDelay;
        }
        TickDelayer.after(server, currentTickDelay, this::step);
        instructionIndex++;
    }

    private void stepTickDelay(){
        tickDelayIndex = (tickDelayIndex + 1) % tickDelays.length; // wraps
        currentTickDelay = tickDelays[tickDelayIndex];
    }

    public void stop() {
        active = false;
    }

    public void start() {
        active = true;
        step();
    }
}
