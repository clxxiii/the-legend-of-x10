package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ReqCommandPacket extends CommandPacket {

    public final String command;

    public ReqCommandPacket(String command) {
        super(CommandSubopcode.LogCommand);
        this.command = command;
    }

    public byte[] packetToBytes() {
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        int numOpCodes = 2;
        int byteCount = numOpCodes * Short.BYTES + commandBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.RequestCommand.code);
        buffer.put(commandBytes);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }
}
