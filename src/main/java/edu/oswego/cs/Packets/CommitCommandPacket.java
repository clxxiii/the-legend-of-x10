package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;

public class CommitCommandPacket extends CommandPacket {

    public final int actionNum;

    public CommitCommandPacket(int actionNum) {
        super(CommandSubopcode.CommitCommand);
        this.actionNum = actionNum;
    }

    public byte[] packetToBytes() {
        int numOpCodes = 2;
        int byteCount = numOpCodes * Short.BYTES + Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.CommitCommand.code);
        buffer.putInt(actionNum);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }
}
