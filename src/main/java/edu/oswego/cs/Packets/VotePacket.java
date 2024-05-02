package edu.oswego.cs.Packets;

import edu.oswego.cs.raft.Vote;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VotePacket extends Packet {

    public final Vote vote;
    public final int termCount;

    public VotePacket(String username, Vote vote, int termCount) {
        super(username, Opcode.Vote);
        this.vote = vote;
        this.termCount = termCount;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int byteCount = Short.BYTES + Short.BYTES + usernameBytes.length + Integer.BYTES;
        byte[] packetBytes = new byte[byteCount];
        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        buffer.putShort(Opcode.Candidate.code);
        buffer.putShort(vote.value);
        buffer.putInt(termCount);
        buffer.put(usernameBytes);
        buffer.rewind();
        buffer.get(packetBytes);
        return packetBytes;
    }

    public static VotePacket bytesToPacket(ByteBuffer buffer) {
        int termCount = buffer.getInt();
        Optional<Vote> optionalVote = Vote.fromValue(buffer.getShort());
        if (!optionalVote.isPresent()) return null;
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        return new VotePacket(new String(usernameBytes, StandardCharsets.UTF_8), optionalVote.get(), termCount);
    }
}
