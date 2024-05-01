package edu.oswego.cs.raft;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Session {

    private AtomicReference<SocketAddress> addressAtomicReference = new AtomicReference<>();
    // Last Message Received Time Stamp In Nano Time
    private AtomicLong LMRSTINT = new AtomicLong();
    private AtomicReference<RaftMembershipState> raftMembershipStateAtomicReference = new AtomicReference<>();
    private AtomicBoolean timedOut = new AtomicBoolean();
    private AtomicInteger greatestActionConfirmed;

    public Session(SocketAddress socketAddress, long LMRSTINT, RaftMembershipState raftMembershipState) {
        this.addressAtomicReference.set(socketAddress);
        this.LMRSTINT.set(LMRSTINT);
        this.raftMembershipStateAtomicReference.set(raftMembershipState);
        timedOut.set(false);
        greatestActionConfirmed = new AtomicInteger(-1);
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

    public void setTimedOut(boolean timedOut) {
        this.timedOut.set(timedOut);
    }

    public void setLMRSTINT(long nanoTime) {
        while (nanoTime > LMRSTINT.get()) {
            LMRSTINT.set(nanoTime);
        }
    }

    public int getGreatestActionConfirmed() {
        return greatestActionConfirmed.get();
    }

    public void setGreatestActionConfirmed(int replacement) {
        while (true) {
            int lastConfirmedIndex = greatestActionConfirmed.get();
            if (replacement > lastConfirmedIndex) {
                greatestActionConfirmed.compareAndSet(lastConfirmedIndex, replacement);
            } else {
                break;
            }
        }
    }

    public void setMembershipState(RaftMembershipState expectedState, RaftMembershipState state) {
        raftMembershipStateAtomicReference.compareAndSet(expectedState, state);
    }
}
