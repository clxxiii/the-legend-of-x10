package edu.oswego.cs.game;

import java.util.concurrent.atomic.AtomicInteger;

public class Action {
    private String command;
    private AtomicInteger numCommitted;

    public Action(String command) {
        this.command = command;
        this.numCommitted = new AtomicInteger(0);
    }

    public String getCommand() {
        return command;
    }

    public int getNumCommited() {
        return numCommitted.get();
    }

    public int incrementAndGetNumCommitted() {
        return numCommitted.incrementAndGet();
    }
}
