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
    private ExecutorService gameService = Executors.newSingleThreadExecutor();
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted = new AtomicInteger(-1);
    private final AtomicBoolean gameActive;
    private final Raft raft;
    private final MainFrame mainFrame;
    private final String clientUsername;

    /**
     * Created a replicated state machine thats intended to be run by a raft instance and guarantee a log is executed in order.
     * @param readOnlyLog A reference to log which is only to be read from.
     * @param lastActionConfirmed An Atomic Integer that represents the last action that is safe to execute.
     * @param gameActive An Atomic Boolean that allows the raft instance to gracefully shutdown the replicated state machine.
     * @param raft A reference to the associated raft instance.
     * @param mainFrame The gui connected to the raft instance.
     * @param clientUsername The username of the user who is connected to the local raft instance.
     */
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
