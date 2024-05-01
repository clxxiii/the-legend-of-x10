package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.CommitCommandPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RaftCommitSender extends Thread {

    private final DatagramSocket socket;
    private final ConcurrentHashMap<String, Session> sessionMap;
    private final CommitCommandPacket packet;

    public RaftCommitSender(DatagramSocket socket, ConcurrentHashMap<String, Session> sessionMap, CommitCommandPacket packet) {
        this.socket = socket;
        this.sessionMap = sessionMap;
        this.packet = packet;
    }

    @Override
    public void run() {
        byte[] packetBytes = packet.packetToBytes();
        Consumer<Session> sessionTask = new Consumer<Session>() {
            @Override
            public void accept(Session session) {
                try {
                    if (session.getMembershipState() == RaftMembershipState.FOLLOWER && !session.getTimedOut()) {
                        DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, session.getSocketAddress());
                        socket.send(datagramPacket);
                    }
                } catch (IOException e) {
                    System.err.println("An IO Exception was thrown while trying to send a commit command packet.");
                }
            }
        };
        sessionMap.forEachValue(1, sessionTask);
    }
}
