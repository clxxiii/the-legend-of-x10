package edu.oswego.cs.game;

import edu.oswego.cs.client.Command;
import edu.oswego.cs.raft.Raft;
import edu.oswego.cs.raft.RaftAdministrationCommand;
import edu.oswego.cs.raft.RaftMembershipState;
import edu.oswego.cs.raft.Session;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameStateExcutor extends Thread {

    private final List<Action> readOnlyLog;
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted;
    private final AtomicBoolean gameActive;
    private final Raft raft;

    public GameStateExcutor(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicInteger lastActionExecuted, AtomicBoolean gameActive, Raft raft) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.lastActionExecuted = lastActionExecuted;
        this.raft = raft;
    }

    @Override
    public void run() {
        try {
            while (gameActive.get()) {
                synchronized (readOnlyLog) {
                    readOnlyLog.wait();
                }
                while (lastActionExecuted.get() < lastActionConfirmed.get() && readOnlyLog.size() - 1 > lastActionExecuted.get()) {
                    // execute command and increment lastActionExecuted.
                    Action action = readOnlyLog.get(lastActionExecuted.incrementAndGet());
                    String commandToBeParsed = action.getCommand();
                    System.out.println(commandToBeParsed);
                    String[] brokenDownCommand = commandToBeParsed.split(" ", 2);
                    Optional<Command> optionalCommand = Command.parse(brokenDownCommand[0]);
                    if (optionalCommand.isPresent()) {
                        Command command = optionalCommand.get();
                        if (command.equals(Command.CHAT)) {
                            if (brokenDownCommand.length > 1) System.out.println(action.getUserName() + ": " + brokenDownCommand[1]);
                        }
                    } else {
                        Optional<RaftAdministrationCommand> optionalRaftAdministrationCommand = RaftAdministrationCommand.parse(brokenDownCommand[0]);
                        if (optionalRaftAdministrationCommand.isPresent()) {
                            RaftAdministrationCommand raftAdministrationCommand = optionalRaftAdministrationCommand.get();
                            switch (raftAdministrationCommand) {
                                case ADD_MEMBER:
                                    handleAddMember(brokenDownCommand[1]);
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Game Executor Interrupted");
        }
    }

    public void handleAddMember(String commandArgs) {
        String[] args = commandArgs.split(" ");
        int numExpectedArgs = 3;
        if (args.length == numExpectedArgs) {
            String[] addr = args[2].split(":");
            if (addr.length == 2) {
                SocketAddress socketAddress = new InetSocketAddress(addr[0], Integer.parseInt(addr[1]));
                raft.addSession(args[0], new Session(socketAddress, Long.parseLong(args[1]), RaftMembershipState.FOLLOWER));
                System.out.println(args[0]);
            }
        }
    }
}
