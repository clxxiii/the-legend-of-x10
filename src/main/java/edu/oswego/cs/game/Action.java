package edu.oswego.cs.game;

import java.util.concurrent.atomic.AtomicInteger;

public class Action {
    private String command;
    private AtomicInteger numConfirmed;
    private String userName;

    public Action(String userName, String command) {
        this.userName = userName;
        this.command = command;
        this.numConfirmed = new AtomicInteger(0);
    }

    public String getCommand() {
        return command;
    }

    public String getUserName() {
        return userName;
    }

    public int getNumCommited() {
        return numConfirmed.get();
    }

    public int incrementAndGetNumCommitted() {
        return numConfirmed.incrementAndGet();
    }
}
