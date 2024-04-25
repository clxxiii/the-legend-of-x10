package edu.oswego.cs.raft;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Session {

    private AtomicReference<SocketAddress> addressAtomicReference = new AtomicReference<>();
    // Last Message Received Time Stamp In Nano Time
    private AtomicLong LMRSTINT = new AtomicLong();
    private AtomicReference<RaftMembershipState> raftMembershipStateAtomicReference = new AtomicReference<>();
    private AtomicBoolean timedOut = new AtomicBoolean();

    public Session(SocketAddress socketAddress, long LMRSTINT, RaftMembershipState raftMembershipState) {
        this.addressAtomicReference.set(socketAddress);
        this.LMRSTINT.set(LMRSTINT);
        this.raftMembershipStateAtomicReference.set(raftMembershipState);
        timedOut.set(true);
    }

    public SocketAddress getSocketAddress() {
        return addressAtomicReference.get();
    }

    public long getLMRSTINT() {
        return LMRSTINT.get();
    }

    public RaftMembershipState getMembershipState() {
        return raftMembershipStateAtomicReference.get();
    }

    public boolean getTimedOut() {
        return timedOut.get();
    }

    public void setLMRSTINT(long nanoTime) {
        LMRSTINT.set(nanoTime);
    }
}
