package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AckPacket extends Packet{

    public AckPacket(String username) {
        super(username, Opcode.Ack);
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + usernameBytes.length;
        byte[] ackBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Ack.code);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(ackBytes);
        return ackBytes;
    }

    public static AckPacket bytesToPacket(ByteBuffer buffer) {
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        return new AckPacket(new String(usernameBytes, StandardCharsets.UTF_8));
    }
}
