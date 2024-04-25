package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LogCommandPacket extends CommandPacket{

    public final int actionNum;
    public final String command;

    public LogCommandPacket(String username, int actionNum, String command) {
        super(CommandSubopcode.LogCommand, username);
        this.command = command;
        this.actionNum = actionNum;
    }

    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        int numOpCodes = 2;
        int paddingByte = 1;
        int byteCount = numOpCodes * Short.BYTES + Integer.BYTES + usernameBytes.length + paddingByte + commandBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Command.code);
        buffer.putShort(CommandSubopcode.LogCommand.code);
        buffer.putInt(actionNum);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(commandBytes);
        buffer.rewind();
        byte[] packetBytes = new byte[byteCount];
        buffer.put(packetBytes);
        return packetBytes;
    }

    public static LogCommandPacket bytesToPacket(ByteBuffer buffer) {
        int actionNum = buffer.getInt();
        // get original limit
        int bufferLimit = buffer.limit();

        buffer.mark();
        while (buffer.hasRemaining() && buffer.get() != 0x00);
        buffer.limit(buffer.position() - 1);
        buffer.reset();
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        String username = new String(usernameBytes, StandardCharsets.UTF_8);

        buffer.limit(bufferLimit);
        // get padding
        buffer.get();

        byte[] commandBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(commandBytes);
        String command = new String(commandBytes, StandardCharsets.UTF_8);
        return new LogCommandPacket(username, actionNum, command);
    }

}
