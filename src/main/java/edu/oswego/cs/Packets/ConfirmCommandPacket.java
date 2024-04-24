package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;

public class ConfirmCommandPacket extends CommandPacket {

    public final int actionNum;

    public ConfirmCommandPacket(int actionNum) {
        super(CommandSubopcode.ConfirmCommand);
        this.actionNum = actionNum;
    }

    public byte[] packetToBytes() {
        int numOpCodes = 2;
        int byteCount = numOpCodes * Short.BYTES + Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.ConfirmCommand.code);
        buffer.putInt(actionNum);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }
}
