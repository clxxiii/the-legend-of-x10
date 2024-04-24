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
                        handleConnectPacket(packet);
                        break;
                    case Command:
                        handleCommandPakcet(packet);
                        break;
                    case Log:
                        handleLogPacket(packet);
                        break;
                    case Heartbeat:
                        handleHeartbeatPacket(packet);
                        break;
                    case Ack:
                        handleAckPacket(packet);
                        break;
                }
            }
        } catch (ParseException e) {
            // Packet parsing exception thrown, ignore the packet
            System.out.println("Packet parsing exception thrown.");
        }
    }

    public void handleConnectPacket(Packet packet) {
        ConnectPacket connectPacket = (ConnectPacket) packet;
    }

    public void handleCommandPakcet(Packet packet) {
        CommandPacket commandPacket = (CommandPacket) packet;
    }

    public void handleLogPacket(Packet packet) {
        LogCommandPacket logCommandPacket = (LogCommandPacket) packet;
    }

    public void handleHeartbeatPacket(Packet packet) {
        HeartbeatPacket heartbeatPacket = (HeartbeatPacket) packet;
    }

    public void handleAckPacket(Packet packet) {
        AckPacket ackPacket = (AckPacket) packet;
    }
}
