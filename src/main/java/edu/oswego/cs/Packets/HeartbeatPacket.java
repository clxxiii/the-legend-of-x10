package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;

public class HeartbeatPacket extends Packet {

    public HeartbeatPacket() {
        super(Opcode.Heartbeat);
    }

    public byte[] packetToBytes() {
        byte[] packetBytes = new byte[Short.BYTES];
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort(Opcode.Heartbeat.code);
        buffer.reset();
        buffer.get(packetBytes);
        return packetBytes;
    }
}
