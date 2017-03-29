package org.ethereum.manager;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 11.12.2014
 */
@Component
public class AdminInfo {
    private static final int ExecTimeListLimit = 10000;
    private final List<Long> blockExecTime = new LinkedList<>();
    private long startupTimeStamp;
    private boolean consensus = true;

    @PostConstruct
    public void init() {
        startupTimeStamp = System.currentTimeMillis();
    }

    public long getStartupTimeStamp() {
        return startupTimeStamp;
    }

    public boolean isConsensus() {
        return consensus;
    }

    public void lostConsensus() {
        consensus = false;
    }

    public void addBlockExecTime(long time){
        while (blockExecTime.size() > ExecTimeListLimit) {
            blockExecTime.remove(0);
        }
        blockExecTime.add(time);
    }

    public Long getExecAvg(){

        if (blockExecTime.isEmpty()) return 0L;

        long sum = 0;
        for (Long aBlockExecTime : blockExecTime) {
            sum += aBlockExecTime;
        }

        return sum / blockExecTime.size();
    }

    public List<Long> getBlockExecTime(){
        return blockExecTime;
    }
}
