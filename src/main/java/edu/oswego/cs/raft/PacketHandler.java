package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;
import edu.oswego.cs.game.Action;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PacketHandler extends Thread {
    private final DatagramPacket packet;
    private final Raft raft;
    private final String serverUsername;
    private final DatagramSocket serverSocket;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Object logConfirmerNotifier;
    private final ConcurrentHashMap<Integer, Action> actionMap;
    private final Object followerLogMaintainerObject;
    private final List<Action> readOnlyLog;

    public PacketHandler(DatagramPacket packet, Raft raft, String serverUsername, DatagramSocket serverSocket, ScheduledExecutorService scheduledExecutorService, Object logConfirmerNotifier, ConcurrentHashMap<Integer, Action> actionMap, Object followerLogMaintainerObject, List<Action> readOnlyLog) {
        this.packet = packet;
        this.raft = raft;
        this.serverUsername = serverUsername;
        this.serverSocket = serverSocket;
        this.scheduledExecutorService = scheduledExecutorService;
        this.logConfirmerNotifier = logConfirmerNotifier;
        this.actionMap = actionMap;
        this.followerLogMaintainerObject = followerLogMaintainerObject;
        this.readOnlyLog = readOnlyLog;
    }

    @Override
    public void run() {
        // do the thing
        SocketAddress socketAddress = packet.getSocketAddress();
        byte[] packetData = packet.getData();
        ByteBuffer packetBuffer = ByteBuffer.allocate(packetData.length);
        packetBuffer.put(packetData);
        packetBuffer.limit(packet.getLength());
        try {
            Packet packet = Packet.bytesToPacket(packetBuffer);
            if (packet != null) {
                Opcode opcode = packet.opcode;
                switch (opcode) {
                    case Connect:
                        handleConnectPacket(packet, socketAddress);
                        break;
                    case Command:
                        handleCommandPakcet(packet, socketAddress);
                        break;
                    case Log:
                        handleLogPacket(packet, socketAddress);
                        break;
                    case Heartbeat:
                        handleHeartbeatPacket(packet, socketAddress);
                        break;
                    case Ack:
                        handleAckPacket(packet, socketAddress);
                        break;
                }
            }
        } catch (ParseException e) {
            // Packet parsing exception thrown, ignore the packet
            System.out.println("Packet parsing exception thrown.");
        }
    }

    public void handleConnectPacket(Packet packet, SocketAddress socketAddr) {
        System.out.println("Client Connect received.");
        ConnectPacket connectPacket = (ConnectPacket) packet;
        switch (connectPacket.subopcode) {
            case ClientHello:
                handleClientHello(connectPacket, socketAddr);
                break;
            case ServerHello:
                handleServerHello(connectPacket, socketAddr);
                break;
            case ClientKey:
                handleClientKey(connectPacket, socketAddr);
                break;
            case Log:
                handleConnectLog(connectPacket, socketAddr);
                break;
            case Redirect:
                handleClientRedirect(connectPacket);
                break;
        }
    }

    public void handleClientHello(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            boolean successfulAdd = raft.addUser(connectPacket.username, socketAddr);
            if (successfulAdd) {
                ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.ServerHello, serverUsername, new byte[0]);
                byte[] packetBytes = responsePacket.packetToBytes();
                DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddr);
                try {
                    serverSocket.send(datagramPacket);
                } catch (IOException e) {
                    System.out.println("Unable to send datagram packet in method handleClientHello");
                }
            }
        } else {
            SocketAddress leaderAddr = raft.getLeaderAddr();
            if (leaderAddr != null) {
                ConnectionRedirectPacket connectionRedirectPacket = new ConnectionRedirectPacket(socketAddr, connectPacket.username, connectPacket.data);
                byte[] packetBytes = connectionRedirectPacket.packetToBytes();
                DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, leaderAddr);
                try {
                    serverSocket.send(datagramPacket);
                } catch (IOException e) {
                    System.out.println("Unable to send datagram packet in method handleClientHello");
                }
            }
        }
    }

    public void handleServerHello(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (!raft.raftSessionActive) {
            raft.setLeader(connectPacket.username, new Session(socketAddr, System.nanoTime(), RaftMembershipState.LEADER));
            ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.ClientKey, serverUsername, new byte[0]);
            byte[] packetBytes = responsePacket.packetToBytes();
            DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddr);
            try {
                serverSocket.send(datagramPacket);
            } catch (IOException e) {
                System.out.println("Unable to send datagram packet in method handleServerHello");
            }
        }
    }

    public void handleClientKey(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            try {
                if (raft.userIsPending(connectPacket.username)) {
                    // add user to log
                    String command = RaftAdministrationCommand.ADD_MEMBER.name + " " + connectPacket.username + " " + System.nanoTime() + " " + socketAddr.toString().replace("/", "");
                    raft.addToRaftQueue(command);
                }
                // tell client to log that the raft session is active
                ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.Log, serverUsername, new byte[0]);
                byte[] packetBytes = responsePacket.packetToBytes();
                DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddr);
                serverSocket.send(datagramPacket);

                // send log
                int logLength = raft.getLogLength();
                for (int i = 0; i < logLength; i++) {
                    Action action = raft.getLogIndex(i);
                    LogCommandPacket logCommandPacket = new LogCommandPacket(action.getUserName(), i, action.getCommand());
                    byte[] logPacketBytes = logCommandPacket.packetToBytes();
                    // schedule these log command sends
                    scheduledExecutorService.schedule(() -> {
                        try {
                            serverSocket.send(new DatagramPacket(logPacketBytes, logPacketBytes.length, socketAddr));
                        } catch (IOException e) {
                            System.err.println("Unable to send scheduled log command.");
                        }
                    }, 5L * i, TimeUnit.MILLISECONDS);
                }
            } catch (IOException e) {
                System.err.println("Unable to send datagram packet in method handleClientKey");
            }
        }
    }

    public void handleClientRedirect(ConnectPacket connectPacket) {
        ConnectionRedirectPacket packet = (ConnectionRedirectPacket) connectPacket;
        if (raft.raftMembershipState == RaftMembershipState.FOLLOWER) {
            // send to leader
            SocketAddress hostAddr = raft.getLeaderAddr();
            if (hostAddr != null) {
                byte[] packetBytes = packet.packetToBytes();
                DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, hostAddr);
                try {
                    serverSocket.send(datagramPacket);
                } catch (IOException e) {
                    System.out.println("Unable to send datagram packet in method handleClientRedirect");
                }
            }
        } else if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            // send a server hello
            handleClientHello(packet, packet.originalAddress);
        }
    }

    public void handleConnectLog(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (raft.addrIsLeader(socketAddr) && !raft.raftSessionActive) {
            raft.raftSessionActive = true;
        }
    }

    public void handleCommandPakcet(Packet packet, SocketAddress socketAddr) {
        CommandPacket commandPacket = (CommandPacket) packet;
        switch (commandPacket.commandSubopcode) {
            case RequestCommand:
                handleRequestCommandPacket(commandPacket, socketAddr);
                break;
            case LogCommand:
                handleLogCommandPacket(commandPacket, socketAddr);
                break;
            case ConfirmCommand:
                handleConfirmCommandPacket(commandPacket, socketAddr);
                break;
            case CommitCommand:
                handleCommitCommandPacket(commandPacket, socketAddr);
                break;
        }
    }

    public void handleLogPacket(Packet packet, SocketAddress socketAddr) {
        LogPacket logPacket = (LogPacket) packet;
        int logIndex = logPacket.logIndex;
        if (logIndex < 0) {
            logIndex = 0;
        }
        for (int i = logIndex; i < readOnlyLog.size(); i++) {
            Action action = readOnlyLog.get(i);
            LogCommandPacket logCommandPacket = new LogCommandPacket(action.getUserName(), i, action.getCommand());
            byte[] packetBytes = logCommandPacket.packetToBytes();
            // schedule these log command sends
            scheduledExecutorService.schedule(() -> {
                try {
                    serverSocket.send(new DatagramPacket(packetBytes, packetBytes.length, socketAddr));
                } catch (IOException e) {
                    System.err.println("Unable to send scheduled log command.");
                }
                }, 5L * i, TimeUnit.MILLISECONDS);
        }
    }

    public void handleRequestCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        ReqCommandPacket reqCommandPacket = (ReqCommandPacket) commandPacket;
        if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            raft.addActionToQueue(new Action(reqCommandPacket.username, reqCommandPacket.command));
        }
    }

    public void handleLogCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        LogCommandPacket logCommandPacket = (LogCommandPacket) commandPacket;
        if (raft.raftMembershipState == RaftMembershipState.FOLLOWER) {
            // commit command
            actionMap.putIfAbsent(logCommandPacket.actionNum, new Action(logCommandPacket.username, logCommandPacket.command));
            // notify maintainer
            synchronized (followerLogMaintainerObject) {
                followerLogMaintainerObject.notify();
            }
            // confirm addition
            ConfirmCommandPacket confirmCommandPacket = new ConfirmCommandPacket(serverUsername, logCommandPacket.actionNum);
            byte[] packetBytes = confirmCommandPacket.packetToBytes();
            DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length, socketAddress);
            try {
                serverSocket.send(packet);
            } catch (IOException e) {
                System.err.println("An IOException was thrown while trying to send a log addition confirmation packet.");
            }
        }
    }

    public void handleConfirmCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        ConfirmCommandPacket confirmCommandPacket = (ConfirmCommandPacket) commandPacket;
        // update the clients greatest confirmation number
        raft.updateRaftFollowerGreatestConfirmedAction(confirmCommandPacket.username, ((ConfirmCommandPacket) commandPacket).actionNum);

        // notify confirmer to check if log entry is confirmed
        synchronized (logConfirmerNotifier) {
            logConfirmerNotifier.notify();
        }

        // send the position they should be commited up to
        CommitCommandPacket commitCommandPacket = new CommitCommandPacket(raft.getClientUserName(), raft.getLastActionConfirmed());
        byte[] packetBytes = commitCommandPacket.packetToBytes();
        DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddress);
        try {
            serverSocket.send(datagramPacket);
        } catch (IOException e) {
            System.err.println("An IOException was thrown when trying to handle a confirmed command packet");
        }
    }

    public void handleCommitCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        if (raft.addrIsLeader(socketAddress)) {
            CommitCommandPacket commitCommandPacket = (CommitCommandPacket) commandPacket;
            raft.commitAction(commitCommandPacket.actionNum);
            if (raft.getLogPosition() < commitCommandPacket.actionNum) {
                // TODO request missing log packets
                LogPacket logPacket = new LogPacket(serverUsername, raft.getLogPosition());
                byte[] packetBytes = logPacket.packetToBytes();
                DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddress);
                try {
                    serverSocket.send(datagramPacket);
                } catch (IOException e) {
                    System.err.println("An IOException was thrown when trying to request a log catchup.");
                }
            }
        }
    }

    public void handleHeartbeatPacket(Packet packet, SocketAddress socketAddr) {
        if (raft.addrIsLeader(socketAddr)) {
            AckPacket ackPacket = new AckPacket(serverUsername);
            byte[] packetBytes = ackPacket.packetToBytes();
            DatagramPacket responsePacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddr);
            try {
                serverSocket.send(responsePacket);
            } catch (IOException e) {
                System.out.println("Unable to send datagram packet in method handleHeartbeatPacket");
            }
            // Account for worst case where log notification is missed upon log commit.
            raft.notifyLog();
            synchronized (followerLogMaintainerObject) {
                followerLogMaintainerObject.notify();
            }
        }
    }

    public void handleAckPacket(Packet packet, SocketAddress socketAddr) {
        AckPacket ackPacket = (AckPacket) packet;
        if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            // update last message received
            raft.updateSessionTimeStamp(packet.username, socketAddr);
//            System.out.println("Ack received from Raft Follower.");
        }
    }
}
