package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;

public class AckPacket extends Packet{

    public AckPacket() {
        super(Opcode.Ack);
    }

    @Override
    public byte[] packetToBytes() {
        byte[] ackBytes = new byte[Short.BYTES];
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort(Opcode.Ack.code);
        buffer.reset();
        buffer.get(ackBytes);
        return ackBytes;
    }
}
