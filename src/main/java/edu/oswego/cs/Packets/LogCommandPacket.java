package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LogCommandPacket extends CommandPacket{

    public final int actionNum;
    public final String command;

    public LogCommandPacket(int actionNum, String command) {
        super(CommandSubopcode.LogCommand);
        this.command = command;
        this.actionNum = actionNum;
    }

    public byte[] packetToBytes() {
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        int numOpCodes = 2;
        int byteCount = numOpCodes * Short.BYTES + Integer.BYTES + commandBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.LogCommand.code);
        buffer.putInt(actionNum);
        buffer.put(commandBytes);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }

}
