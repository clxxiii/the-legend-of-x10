package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;

public class PacketHandler extends Thread {
    private final DatagramPacket packet;
    private final Raft raft;
    private final String serverUsername;
    private final DatagramSocket serverSocket;

    public PacketHandler(DatagramPacket packet, Raft raft, String serverUsername, DatagramSocket serverSocket) {
        this.packet = packet;
        this.raft = raft;
        this.serverUsername = serverUsername;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        // do the thing
        SocketAddress socketAddress = packet.getSocketAddress();
        byte[] packetData = packet.getData();
        ByteBuffer packetBuffer = ByteBuffer.allocate(packetData.length);
        packetBuffer.put(packetData);
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
            // TODO: send to leader
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
            if (raft.userIsPending(connectPacket.username)) {
                // add user to log
                String command = RaftAdministrationCommand.ADD_MEMBER.name + " " + connectPacket.username + " " + System.nanoTime() + " " + socketAddr.toString().replace("/", "");
                raft.addToRaftQueue(command);
            }
            // tell client to log that the raft session is active
            ConnectPacket responsePacket = new ConnectPacket(ConnectSubopcode.Log, serverUsername, new byte[0]);
            byte[] packetBytes = responsePacket.packetToBytes();
            DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, socketAddr);
            // TODO: start sending log
        }
    }

    public void handleConnectLog(ConnectPacket connectPacket, SocketAddress socketAddr) {
        if (!raft.raftSessionActive) {
            raft.raftSessionActive = true;
        }
    }

    public void handleCommandPakcet(Packet packet, SocketAddress socketAddr) {
        CommandPacket commandPacket = (CommandPacket) packet;
    }

    public void handleLogPacket(Packet packet, SocketAddress socketAddr) {
        LogCommandPacket logCommandPacket = (LogCommandPacket) packet;
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
        }
    }

    public void handleAckPacket(Packet packet, SocketAddress socketAddr) {
        AckPacket ackPacket = (AckPacket) packet;
        if (raft.raftMembershipState == RaftMembershipState.LEADER) {
            // update last message received
            raft.updateSessionTimeStamp(packet.username, socketAddr);
            System.out.println("Ack received from Raft Follower.");
        }
    }
}
