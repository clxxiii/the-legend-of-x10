package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LogPacket extends Packet {

    public final int logIndex;

    public LogPacket(String username, int logIndex) {
        super(username, Opcode.Log);
        this.logIndex = logIndex;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int packetSize = Short.BYTES + usernameBytes.length + Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        buffer.putShort(Opcode.Log.code);
        buffer.putInt(logIndex);
        buffer.put(usernameBytes);
        buffer.flip();
        byte[] packetBytes = new byte[packetSize];
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static LogPacket bytesToPacket(ByteBuffer buffer) {
        int logIndex = buffer.getInt();
        byte[] userNameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(userNameBytes);
        return new LogPacket(new String(userNameBytes, StandardCharsets.UTF_8), logIndex);
    }
}
