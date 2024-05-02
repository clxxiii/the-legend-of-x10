package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeartbeatPacket extends Packet {

    public final int lastConfirmed;

    public HeartbeatPacket(String username, int lastConfirmed) {
        super(username, Opcode.Heartbeat);
        this.lastConfirmed = lastConfirmed;
    }

    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + usernameBytes.length + Integer.BYTES;
        byte[] packetBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Heartbeat.code);
        buffer.putInt(lastConfirmed);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static HeartbeatPacket bytesToPacket(ByteBuffer buffer) {
        int lastConfirmed = buffer.getInt();
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        return new HeartbeatPacket(new String(usernameBytes, StandardCharsets.UTF_8), lastConfirmed);
    }
}
