package edu.oswego.cs.game;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameStateExcutor extends Thread {

    private final List<Action> readOnlyLog;
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted;
    private final AtomicBoolean gameActive;

    public GameStateExcutor(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicInteger lastActionExecuted, AtomicBoolean gameActive) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.lastActionExecuted = lastActionExecuted;
    }

    @Override
    public void run() {
        try {
            while (gameActive.get()) {
                synchronized (readOnlyLog) {
                    readOnlyLog.wait();
                }
                while (lastActionExecuted.get() < lastActionConfirmed.get()) {
                    // execute command and increment lastActionExecuted.
                    System.out.println(readOnlyLog.get(lastActionExecuted.incrementAndGet()));
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Game Executor Interrupted");
        }
    }
}
