package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;

public class PacketHandler extends Thread {
    private final DatagramPacket packet;
    private final Raft raft;

    public PacketHandler(DatagramPacket packet, Raft raft) {
        this.packet = packet;
        this.raft = raft;
    }

    @Override
    public void run() {
        // do the thing
        SocketAddress socketAddress = packet.getSocketAddress();
        byte[] packetData = packet.getData();
        ByteBuffer packetBuffer = ByteBuffer.allocate(packetData.length);
        packetBuffer.put(packetData);
        packetBuffer.reset();
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

    public void handleConnectPacket(Packet packet, SocketAddress clientAddr) {
        ConnectPacket connectPacket = (ConnectPacket) packet;
        switch (connectPacket.subopcode) {
            case ClientHello:
                handleClientHello(connectPacket, clientAddr);
                break;
            case ServerHello:
                break;
            case ClientKey:
                break;
            case Log:
                break;
        }
    }

    public void handleClientHello(ConnectPacket connectPacket, SocketAddress clientAddr) {
        boolean successfulAdd = raft.addUser(connectPacket.username, clientAddr);
    }

    public void handleCommandPakcet(Packet packet, SocketAddress clientAddr) {
        CommandPacket commandPacket = (CommandPacket) packet;
    }

    public void handleLogPacket(Packet packet, SocketAddress clientAddr) {
        LogCommandPacket logCommandPacket = (LogCommandPacket) packet;
    }

    public void handleHeartbeatPacket(Packet packet, SocketAddress clientAddr) {
        HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
    }

    public void handleAckPacket(Packet packet, SocketAddress clientAddr) {
        AckPacket ackPacket = (AckPacket) packet;
    }
}
