package com.eugene;

import java.util.List;

public class LecternInstructions {
    String jobName;
    double estimatedTotalTime;
    List<Integer> rawInstructions;
    int[] tickDelays;
    int homeTickDelay;

    public LecternInstructions(String jobName, double estimatedTotalTime,
                               List<Integer> rawInstructions, int[] tickDelays, int homeTickDelay){
        this.jobName = jobName;
        this.estimatedTotalTime = estimatedTotalTime;
        this.rawInstructions = rawInstructions;
        this.tickDelays = tickDelays;
        this.homeTickDelay = homeTickDelay;
    }

    public boolean isEmpty(){
        if(rawInstructions.isEmpty()) return true;
        return tickDelays.length - 1 <= 0;
    }
}
