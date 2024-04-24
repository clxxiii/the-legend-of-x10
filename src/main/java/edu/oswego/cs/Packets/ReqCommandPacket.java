package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ReqCommandPacket extends CommandPacket {

    public final String command;

    public ReqCommandPacket(String username, String command) {
        super(CommandSubopcode.LogCommand, username);
        this.command = command;
    }

    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        int numOpCodes = 2;
        int paddingByte = 1;
        int byteCount = numOpCodes * Short.BYTES + usernameBytes.length + paddingByte + commandBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.RequestCommand.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(commandBytes);
        buffer.reset();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }
}
