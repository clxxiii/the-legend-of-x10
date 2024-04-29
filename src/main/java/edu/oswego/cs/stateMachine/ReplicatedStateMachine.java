package edu.oswego.cs.stateMachine;

import edu.oswego.cs.game.Action;
import edu.oswego.cs.gui.MainFrame;
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
    private final MainFrame mainFrame;
    private final String clientUsername;

    public ReplicatedStateMachine(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicBoolean gameActive, Raft raft, MainFrame mainFrame, String clientUsername) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.raft = raft;
        this.mainFrame = mainFrame;
        this.clientUsername = clientUsername;
    }

    public void start() {
        gameService.execute(new ReplicatedStateExecutor(readOnlyLog, lastActionConfirmed, lastActionExecuted, gameActive, raft, mainFrame, clientUsername));
    }

    public void stop() {
        gameActive.set(false);
        synchronized (readOnlyLog) {
            readOnlyLog.notify();
        }
        gameService.shutdown();
    }
}
