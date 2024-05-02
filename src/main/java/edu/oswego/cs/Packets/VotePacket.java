package edu.oswego.cs.Packets;

import edu.oswego.cs.raft.Vote;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VotePacket extends Packet {

    public final int termNum;

    public VotePacket(String username, int termNum) {
        super(username, Opcode.Vote);
        this.termNum = termNum;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + usernameBytes.length + Integer.BYTES;
        byte[] packetBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Vote.code);
        buffer.putInt(termNum);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static VotePacket bytesToPacket(ByteBuffer buffer) {
        int termNum = buffer.getInt();
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        return new VotePacket(new String(usernameBytes, StandardCharsets.UTF_8), termNum);
    }
}
