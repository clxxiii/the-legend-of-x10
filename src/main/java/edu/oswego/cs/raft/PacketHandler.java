package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;
import edu.oswego.cs.Security.Encryption;
import edu.oswego.cs.game.Action;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PacketHandler extends Thread {
    private final DatagramPacket datagramPacket;
    private final Raft raft;
    private final String serverUsername;
    private final DatagramSocket serverSocket;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Object logConfirmerNotifier;
    private final ConcurrentHashMap<Integer, Action> actionMap;
    private final Object followerLogMaintainerObject;
    private final List<Action> readOnlyLog;
    private final Encryption encryption;

    public PacketHandler(DatagramPacket packet, Raft raft, String serverUsername, DatagramSocket serverSocket, ScheduledExecutorService scheduledExecutorService, Object logConfirmerNotifier, ConcurrentHashMap<Integer, Action> actionMap, Object followerLogMaintainerObject, List<Action> readOnlyLog, Encryption encryption) {
        this.datagramPacket = packet;
        this.raft = raft;
        this.serverUsername = serverUsername;
        this.serverSocket = serverSocket;
        this.scheduledExecutorService = scheduledExecutorService;
        this.logConfirmerNotifier = logConfirmerNotifier;
        this.actionMap = actionMap;
        this.followerLogMaintainerObject = followerLogMaintainerObject;
        this.readOnlyLog = readOnlyLog;
        this.encryption = encryption;
    }

    @Override
    public void run() {
        // do the thing
        SocketAddress socketAddress = datagramPacket.getSocketAddress();
        byte[] packetData = datagramPacket.getData();
        ByteBuffer packetBuffer = ByteBuffer.allocate(packetData.length);
        packetBuffer.put(packetData);
        packetBuffer.limit(datagramPacket.getLength());
        try {
            Packet packet = Packet.bytesToPacket(packetBuffer);
            if (packet != null) {
                raft.updateSessionTimeStamp(packet.username, socketAddress);
                Opcode opcode = packet.opcode;
                if (opcode == Opcode.Connect) {
                    ConnectPacket connectPacket = (ConnectPacket) packet;
                    if (connectPacket.subopcode == ConnectSubopcode.ClientHello) {
                        handleClientHello(connectPacket, socketAddress);
                    } else if (connectPacket.subopcode == ConnectSubopcode.ServerHello) {
                        handleServerHello(connectPacket, socketAddress);
                    } else if (connectPacket.subopcode == ConnectSubopcode.Redirect) {
                        handleClientRedirect(connectPacket);
                    }
                }
            } else {
                packetBuffer.rewind();
                byte[] encryptedBytes = new byte[packetBuffer.limit()];
                packetBuffer.get(encryptedBytes);
                byte[] decryptedBytes = encryption.decryptMessageWithSecretKey(encryptedBytes);
                if (decryptedBytes == null) return;
                packetBuffer.rewind();
                packetBuffer.limit(decryptedBytes.length);
                packetBuffer.put(decryptedBytes);
                packet = Packet.bytesToPacket(packetBuffer);
                if (packet != null) {
                    // update last message received time
                    raft.updateSessionTimeStamp(packet.username, socketAddress);
                    Opcode opcode = packet.opcode;
                    switch (opcode) {
                        case Connect:
                            handleConnectPacket(packet, socketAddress);
                            break;
                        case Command:
                            handleCommandPacket(packet, socketAddress);
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
                        case Vote:
                            handleVotePacket(packet, socketAddress);
                            break;
                        case Candidate:
                            handleCandidatePacket(packet, socketAddress);
                            break;
                    }
                }
            }
        } catch (ParseException e) {
            // Packet parsing exception thrown, ignore the packet
            System.out.println("Packet parsing exception thrown.");
        }

    }

    public void handleConnectPacket(Packet packet, SocketAddress socketAddr) {
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
        ConnectionClientHelloPacket clientHelloPacket = (ConnectionClientHelloPacket) connectPacket;
        if (raft.raftMembershipState.get() == RaftMembershipState.LEADER) {
            boolean successfulAdd = raft.addUser(connectPacket.username, socketAddr);
            if (successfulAdd) {
                ConnectPacket responsePacket = new ConnectionServerHelloPacket(serverUsername, encryption.encryptSecretKeyWithPublicKey(clientHelloPacket.publicKey));
                byte[] packetBytes = responsePacket.packetToBytes();
                try {
                    serverSocket.send(new DatagramPacket(packetBytes, packetBytes.length, socketAddr));
                } catch (IOException e) {
                    System.err.println("An IOException is thrown when trying to send a message.");
                }
            }
        } else {
            SocketAddress leaderAddr = raft.getLeaderAddr();
            if (leaderAddr != null) {
                ConnectionRedirectPacket connectionRedirectPacket = new ConnectionRedirectPacket(socketAddr, connectPacket.username, clientHelloPacket.publicKey);
                byte[] packetBytes = connectionRedirectPacket.packetToBytes();
                sendPacket(packetBytes, leaderAddr);
            }
        }
    }

    public void handleServerHello(ConnectPacket connectPacket, SocketAddress socketAddr) {
        ConnectionServerHelloPacket connectionServerHelloPacket = (ConnectionServerHelloPacket) connectPacket;
        if (!raft.raftSessionActive) {
            raft.setLeader(connectPacket.username, new Session(socketAddr, System.nanoTime(), RaftMembershipState.LEADER));
            byte[] encryptedSecretKey = connectionServerHelloPacket.encryptedSecretKey;
            byte[] secretKeyBytes = encryption.decryptMessageWithPrivateKey(encryptedSecretKey);
            encryption.setSecretKey(new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES"));
            ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.ClientKey, serverUsername, new byte[0]);
            byte[] packetBytes = responsePacket.packetToBytes();
            sendPacket(packetBytes, socketAddr);
        }
    }

    public void handleClientKey(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (raft.raftMembershipState.get() == RaftMembershipState.LEADER) {
                if (raft.userIsPending(connectPacket.username)) {
                    // add user to log
                    String command = RaftAdministrationCommand.ADD_MEMBER.name + " " + connectPacket.username + " " + System.nanoTime() + " " + socketAddr.toString().replace("/", "");
                    raft.addToRaftQueue(command);
                } else if (raft.userIsReconnecting(connectPacket.username)) {
                    String command = RaftAdministrationCommand.RECONNECT.name + " " + connectPacket.username;
                    raft.addToRaftQueue(command);
                }
                // tell client to log that the raft session is active
                ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.Log, serverUsername, new byte[0]);
                byte[] packetBytes = responsePacket.packetToBytes();
                sendPacket(packetBytes, socketAddr);

                // send log
                int logLength = raft.getLogLength();
                for (int i = 0; i < logLength; i++) {
                    Action action = raft.getLogIndex(i);
                    LogCommandPacket logCommandPacket = new LogCommandPacket(raft.getClientUserName(), i, raft.getTermNum(), action.getUserName(), action.getCommand());
                    byte[] logPacketBytes = logCommandPacket.packetToBytes();
                    // schedule these log command sends
                    scheduledExecutorService.schedule(() -> {
                        sendPacket(logPacketBytes, socketAddr);
                    }, 5L * i, TimeUnit.MILLISECONDS);
                }
        }
    }

    public void handleClientRedirect(ConnectPacket connectPacket) {
        ConnectionRedirectPacket packet = (ConnectionRedirectPacket) connectPacket;
        if (raft.raftMembershipState.get() == RaftMembershipState.FOLLOWER) {
            // send to leader
            SocketAddress hostAddr = raft.getLeaderAddr();
            if (hostAddr != null) {
                byte[] packetBytes = packet.packetToBytes();
                sendPacket(packetBytes, hostAddr);
            }
        } else if (raft.raftMembershipState.get() == RaftMembershipState.LEADER) {
            // send a server hello
            ConnectionClientHelloPacket clientHelloPacket = new ConnectionClientHelloPacket(packet.username, packet.publicKey);
            handleClientHello(clientHelloPacket, packet.originalAddress);
        }
    }

    public void handleConnectLog(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (raft.addrIsLeader(socketAddr) && !raft.raftSessionActive) {
            raft.raftSessionActive = true;
            raft.startElectionTimeout();
        }
    }

    public void handleCommandPacket(Packet packet, SocketAddress socketAddr) {
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
            LogCommandPacket logCommandPacket = new LogCommandPacket(raft.getClientUserName(), i, raft.getTermNum(), action.getUserName(), action.getCommand());
            byte[] packetBytes = logCommandPacket.packetToBytes();
            // schedule these log command sends
            scheduledExecutorService.schedule(() -> {
                sendPacket(packetBytes, socketAddr);
                }, 5L * i, TimeUnit.MILLISECONDS);
        }
    }

    public void handleRequestCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        ReqCommandPacket reqCommandPacket = (ReqCommandPacket) commandPacket;
        if (raft.raftMembershipState.get() == RaftMembershipState.LEADER) {
            raft.addActionToQueue(new Action(reqCommandPacket.username, reqCommandPacket.command));
        }
    }

    public void handleLogCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        LogCommandPacket logCommandPacket = (LogCommandPacket) commandPacket;
        if (raft.getTermNum() < logCommandPacket.termNum) {
            // convert to follower
            transformToFollower(logCommandPacket.username, socketAddress, logCommandPacket.termNum);
        }
        if (raft.raftMembershipState.get() == RaftMembershipState.FOLLOWER) {
            // commit command
            actionMap.putIfAbsent(logCommandPacket.actionNum, new Action(logCommandPacket.usernameAssocWithCommand, logCommandPacket.command));
            // notify maintainer
            synchronized (followerLogMaintainerObject) {
                followerLogMaintainerObject.notify();
            }
            // confirm addition
            ConfirmCommandPacket confirmCommandPacket = new ConfirmCommandPacket(serverUsername, logCommandPacket.actionNum);
            byte[] packetBytes = confirmCommandPacket.packetToBytes();
            sendPacket(packetBytes, socketAddress);
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
        sendPacket(packetBytes, socketAddress);
    }

    public void handleCommitCommandPacket(CommandPacket commandPacket, SocketAddress socketAddress) {
        if (raft.addrIsLeader(socketAddress)) {
            CommitCommandPacket commitCommandPacket = (CommitCommandPacket) commandPacket;
            raft.commitAction(commitCommandPacket.actionNum);
            if (raft.getLogPosition() < commitCommandPacket.actionNum) {
                // request missing log packets
                LogPacket logPacket = new LogPacket(serverUsername, raft.getLogPosition());
                byte[] packetBytes = logPacket.packetToBytes();
                sendPacket(packetBytes, socketAddress);
            }
        }
    }

    public void handleHeartbeatPacket(Packet packet, SocketAddress socketAddr) {
        HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
        if (raft.getTermNum() < heartbeatPacket.termCount) {
            // convert to follower
            transformToFollower(heartbeatPacket.username, socketAddr, heartbeatPacket.termCount);
        }
        if (raft.addrIsLeader(socketAddr)) {
            AckPacket ackPacket = new AckPacket(serverUsername);
            byte[] packetBytes = ackPacket.packetToBytes();
            sendPacket(packetBytes, socketAddr);
            if (raft.getLogPosition() < heartbeatPacket.lastConfirmed) {
                // request missing log packets
                LogPacket logPacket = new LogPacket(serverUsername, raft.getLogPosition());
                byte[] logPacketBytes = logPacket.packetToBytes();
                sendPacket(logPacketBytes, socketAddr);
            }
            raft.commitAction(heartbeatPacket.lastConfirmed);
            // Account for worst case where log notification is missed upon log commit.
            raft.notifyLog();
            synchronized (followerLogMaintainerObject) {
                followerLogMaintainerObject.notify();
            }
        }
    }

    public void handleAckPacket(Packet packet, SocketAddress socketAddr) {
        AckPacket ackPacket = (AckPacket) packet;
    }

    public void handleCandidatePacket(Packet packet, SocketAddress socketAddress) {
        CandidatePacket candidatePacket = (CandidatePacket) packet;
        if (candidatePacket.termCount > raft.getTermNum() && candidatePacket.logPosition >= raft.getLogPosition()) {
            // change to follower
            boolean success = transformToFollower(candidatePacket.username, socketAddress, candidatePacket.termCount);
            System.out.println(success);
            if (success) {
                // send vote
                VotePacket votePacket = new VotePacket(serverUsername, raft.getTermNum());
                sendPacket(votePacket.packetToBytes(), socketAddress);
                System.out.println(socketAddress);
            }
        }
    }

    public void handleVotePacket(Packet packet, SocketAddress socketAddress) {
        VotePacket votePacket = (VotePacket) packet;
        if (raft.raftMembershipState.get() == RaftMembershipState.CANDIDATE) {
            raft.addVote(votePacket.username, votePacket.termNum);
        }
    }

    public void sendPacket(byte[] bytes, SocketAddress socketAddress) {
        byte[] encryptedBytes = encryption.encryptMessageWithSecretKey(bytes);
        if (encryptedBytes != null) {
            try {
                serverSocket.send(new DatagramPacket(encryptedBytes, encryptedBytes.length, socketAddress));
            } catch (IOException e) {
                System.err.println("An IOException is thrown when trying to send a message.");
            }
        }
    }

    public boolean transformToFollower(String username, SocketAddress socketAddr, int termNum) {
        boolean success = raft.setTermNum(termNum);
        if (success) {
            raft.resetVote();
            raft.demoteLeader();
            raft.setLeader(username, new Session(socketAddr, System.nanoTime(), RaftMembershipState.LEADER));
            // rng the election time out
            raft.stopElectionTimeout();
            raft.startElectionTimeout();
            raft.convertToFollower();
        }
        return success;
    }
}
