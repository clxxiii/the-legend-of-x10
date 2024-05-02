package edu.oswego.cs.stateMachine;

import edu.oswego.cs.client.Command;
import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Floor;
import edu.oswego.cs.dungeon.GameUser;
import edu.oswego.cs.dungeon.Room;
import edu.oswego.cs.game.Action;
import edu.oswego.cs.game.GameCommandOutput;
import edu.oswego.cs.gui.MainFrame;
import edu.oswego.cs.raft.Raft;
import edu.oswego.cs.raft.RaftAdministrationCommand;
import edu.oswego.cs.raft.RaftMembershipState;
import edu.oswego.cs.raft.Session;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplicatedStateExecutor extends Thread {

    private final List<Action> readOnlyLog;
    private final AtomicInteger lastActionConfirmed;
    private final AtomicInteger lastActionExecuted;
    private final AtomicBoolean gameActive;
    private final Raft raft;
    private final MainFrame mainFrame;
    private final String clientUsername;
    private Dungeon dungeon;
    private Floor currentFloor;
    private Floor firstFloor;
    private GameUser user;

    public ReplicatedStateExecutor(List<Action> readOnlyLog, AtomicInteger lastActionConfirmed, AtomicInteger lastActionExecuted, AtomicBoolean gameActive, Raft raft, MainFrame mainFrame, String clientUsername) {
        this.readOnlyLog = readOnlyLog;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.lastActionExecuted = lastActionExecuted;
        this.raft = raft;
        this.mainFrame = mainFrame;
        this.clientUsername = clientUsername;
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
                    String[] brokenDownCommand = commandToBeParsed.split(" ", 2);
                    Optional<Command> optionalCommand = Command.parse(brokenDownCommand[0]);
                    if (optionalCommand.isPresent()) {
                        Command command = optionalCommand.get();

                        GameCommandOutput output = new GameCommandOutput();
                        switch(command) {
                            case CHAT:
                                if (brokenDownCommand.length > 1) mainFrame.addMessage(action.getUserName() + ": " + brokenDownCommand[1]);
                                break;
                            case MOVE:
                                output = dungeon.move(action.getUserName(), brokenDownCommand[1].charAt(0));

                                if (output.username.equals(clientUsername)) {
                                    mainFrame.addMessage(output.textOutput);
                                    mainFrame.listRoomEnemies(user);
                                }
                                break;
                            case ATTACK:
                                output = dungeon.attack(action.getUserName(), brokenDownCommand[1]);
                        }
                    //vvvv  NO TOUCH  vvvvv
                    } else {
                        Optional<RaftAdministrationCommand> optionalRaftAdministrationCommand = RaftAdministrationCommand.parse(brokenDownCommand[0]);
                        if (optionalRaftAdministrationCommand.isPresent()) {
                            RaftAdministrationCommand raftAdministrationCommand = optionalRaftAdministrationCommand.get();
                            switch (raftAdministrationCommand) {
                                case ADD_MEMBER:
                                    String username = handleAddMember(brokenDownCommand[1]);
                                    dungeon.addUser(new GameUser(firstFloor.getEntrance(), username));
                                    break;
                                case SEED_DUNGEON: {
                                    long seed = Long.parseLong(brokenDownCommand[1]);
                                    startupDungeon(seed);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Game Executor Interrupted");
        }
    }

    public String handleAddMember(String commandArgs) {
        String[] args = commandArgs.split(" ");
        int numExpectedArgs = 3;
        if (args.length == numExpectedArgs) {
            int index = args[2].lastIndexOf(":");
            String addr = args[2].substring(0, index);
            int port = Integer.parseInt(args[2].substring(index + 1));
            SocketAddress socketAddress = new InetSocketAddress(addr, port);
            raft.addSession(args[0], new Session(socketAddress, Long.parseLong(args[1]), RaftMembershipState.FOLLOWER));
            return args[0];
        }

        return null;
    }

    public void startupDungeon(long seed) {
        this.dungeon = new Dungeon(seed);
        this.firstFloor = currentFloor = dungeon.makeFloor();
        user = new GameUser(currentFloor.getEntrance(), clientUsername);
        this.dungeon.addUser(user);
        mainFrame.initialize(user.username, user.getRoomNumber(), currentFloor);
    }

}
