package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ConfirmCommandPacket extends CommandPacket {

    public final int actionNum;

    public ConfirmCommandPacket(String username, int actionNum) {
        super(CommandSubopcode.ConfirmCommand, username);
        this.actionNum = actionNum;
    }

    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int numOpCodes = 2;
        int byteCount = numOpCodes * Short.BYTES + Integer.BYTES + usernameBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.ConfirmCommand.code);
        buffer.putInt(actionNum);
        buffer.put(usernameBytes);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }
}
