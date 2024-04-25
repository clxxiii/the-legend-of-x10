package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeartbeatPacket extends Packet {

    public HeartbeatPacket(String username) {
        super(username, Opcode.Heartbeat);
    }

    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + usernameBytes.length;
        byte[] packetBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Heartbeat.code);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static HeartbeatPacket bytesToPacket(ByteBuffer buffer) {
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.put(usernameBytes);
        return new HeartbeatPacket(new String(usernameBytes, StandardCharsets.UTF_8));
    }
}
