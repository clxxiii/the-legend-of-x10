package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.CommitCommandPacket;
import edu.oswego.cs.game.Action;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RaftLogConfirmer extends Thread {

    private final Object wakeyWakeyEggsAndBakey;
    private final ConcurrentHashMap<String, Session> sessionMap;
    private final AtomicInteger lastActionConfirmed;
    private final AtomicBoolean gameActive;
    private final String username;
    private final DatagramSocket datagramSocket;
    private final List<Action> log;

    public RaftLogConfirmer(Object wakeyWakeyEggsAndBakey, ConcurrentHashMap<String, Session> sessionMap, AtomicInteger lastActionConfirmed, AtomicBoolean gameActive, String username, DatagramSocket datagramSocket, List<Action> log) {
        this.wakeyWakeyEggsAndBakey = wakeyWakeyEggsAndBakey;
        this.sessionMap = sessionMap;
        this.lastActionConfirmed = lastActionConfirmed;
        this.gameActive = gameActive;
        this.username = username;
        this.datagramSocket = datagramSocket;
        this.log = log;
    }

    @Override
    public void run() {
        try {
            while (gameActive.get()) {
                synchronized (wakeyWakeyEggsAndBakey) {
                    wakeyWakeyEggsAndBakey.wait();
                }
                int initialConfirmedCommand = lastActionConfirmed.get();
                int previousConfirmedCommand = initialConfirmedCommand;
                int nextConfirmedCommand = previousConfirmedCommand;
                do {
                    previousConfirmedCommand = nextConfirmedCommand;
                    int commandNumToCheck = previousConfirmedCommand;
                    AtomicInteger countAboveCurrentConfirmed = new AtomicInteger(0);
                    AtomicInteger totalFollowers = new AtomicInteger(0);
                    Consumer<Session> sessionTask = new Consumer<Session>() {
                        @Override
                        public void accept(Session session) {
                            RaftMembershipState state = session.getMembershipState();
                            if (state == RaftMembershipState.FOLLOWER || state == RaftMembershipState.LEADER) {
                                if (state == RaftMembershipState.FOLLOWER) {
                                    totalFollowers.incrementAndGet();
                                }
                                if (session.getGreatestActionConfirmed() > commandNumToCheck) {
                                    countAboveCurrentConfirmed.incrementAndGet();
                                }
                            }
                        }
                    };
                    sessionMap.forEachValue(2, sessionTask);
                    if (countAboveCurrentConfirmed.get() > totalFollowers.get() / 2) {
                        nextConfirmedCommand = lastActionConfirmed.incrementAndGet();
                    }
                } while (nextConfirmedCommand > previousConfirmedCommand);
                // send commit to everyone
                if (nextConfirmedCommand > initialConfirmedCommand) {
                    (new RaftCommitSender(datagramSocket, sessionMap, new CommitCommandPacket(username, lastActionConfirmed.get()))).start();
                    synchronized (log) {
                        log.notify();
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("An Interrupted exception was thrown while the Raft Log Confirmer was executing.");
        }
    }
}
