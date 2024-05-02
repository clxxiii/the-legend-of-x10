package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CandidatePacket extends Packet{

    public final int termCount;
    public final int logPosition;

    public CandidatePacket(String username, int termCount, int logPosition) {
        super(username, Opcode.Candidate);
        this.termCount = termCount;
        this.logPosition = logPosition;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + usernameBytes.length + Integer.BYTES + Integer.BYTES;
        byte[] packetBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Candidate.code);
        buffer.putInt(termCount);
        buffer.putInt(logPosition);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static CandidatePacket bytesToPacket(ByteBuffer buffer) {
        int termCount = buffer.getInt();
        int logPosition = buffer.getInt();
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        return new CandidatePacket(new String(usernameBytes, StandardCharsets.UTF_8), termCount, logPosition);
    }
}
