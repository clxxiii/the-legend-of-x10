package edu.oswego.cs.game;

import edu.oswego.cs.client.Command;

import java.util.List;
import java.util.Optional;
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
                    Action action = readOnlyLog.get(lastActionExecuted.incrementAndGet());
                    String commandToBeParsed = action.getCommand();
                    String[] brokenDownCommand = commandToBeParsed.split(" ", 2);
                    Optional<Command> optionalCommand = Command.parse(brokenDownCommand[0]);
                    if (optionalCommand.isPresent()) {
                        Command command = optionalCommand.get();
                        if (command.equals(Command.CHAT)) {
                            if (brokenDownCommand.length > 1) System.out.println(action.getUserName() + ": " + brokenDownCommand[1]);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Game Executor Interrupted");
        }
    }
}
