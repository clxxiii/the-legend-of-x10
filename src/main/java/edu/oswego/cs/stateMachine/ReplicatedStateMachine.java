package edu.oswego.cs.stateMachine;

import edu.oswego.cs.game.Action;
import edu.oswego.cs.raft.Raft;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplicatedStateMachine {
    private final List<Action> readOnlyLog;
    private int logIndex;
    private ExecutorService gameService = Executors.newSingleThreadExecutor();
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted = new AtomicInteger(-1);
    private final AtomicBoolean gameActive;
    private final Raft raft;

    public ReplicatedStateMachine(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicBoolean gameActive, Raft raft) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.raft = raft;
    }

    public void start() {
        gameService.execute(new ReplicatedStateExcutor(readOnlyLog, lastActionConfirmed, lastActionExecuted, gameActive, raft));
    }

    public void stop() {
        gameActive.set(false);
        synchronized (readOnlyLog) {
            readOnlyLog.notify();
        }
        gameService.shutdown();
    }
}
