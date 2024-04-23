package edu.oswego.cs.game;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameStateMachine {
    private final List<Action> readOnlyLog;
    private int logIndex;
    private ExecutorService gameService = Executors.newSingleThreadExecutor();
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted = new AtomicInteger(-1);
    private final AtomicBoolean gameActive;

    public GameStateMachine(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicBoolean gameActive) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
    }

    public void start() {
        gameService.execute(new GameStateExcutor(readOnlyLog, lastActionConfirmed, lastActionExecuted, gameActive));
    }

    public void stop() {
        gameActive.set(false);
        synchronized (readOnlyLog) {
            readOnlyLog.notify();
        }
        gameService.shutdown();
    }
}
