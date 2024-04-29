package edu.oswego.cs.raft;

import edu.oswego.cs.Security.Encryption;
import edu.oswego.cs.game.Action;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftReceiver extends Thread {
    private final DatagramSocket serverSocket;
    private final AtomicBoolean keepReceiving;
    private final int DATA_PACKET_MAX_LEN = 1024;
    private final Raft localRaft;
    private final String username;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private final Object logConfirmerNotifier;
    private final ConcurrentHashMap<Integer, Action> actionMap;
    private final Object followerLogMaintainerObject;
    private final List<Action> readOnlyLog;
    private final Encryption encryption;

    public RaftReceiver(DatagramSocket serverSocket, AtomicBoolean keepReceiving, Raft localRaft, String username, Object logConfirmerNotifier, ConcurrentHashMap<Integer, Action> actionMap, Object followerLogMaintainerObject, List<Action> readOnlyLog, Encryption encryption) {
        this.serverSocket = serverSocket;
        this.keepReceiving = keepReceiving;
        this.localRaft = localRaft;
        this.username = username;
        this.logConfirmerNotifier = logConfirmerNotifier;
        this.actionMap = actionMap;
        this.followerLogMaintainerObject = followerLogMaintainerObject;
        this.readOnlyLog = readOnlyLog;
        this.encryption = encryption;
    }

    @Override
    public void run() {
        try {
            while (keepReceiving.get()) {
                byte[] data = new byte[DATA_PACKET_MAX_LEN];
                DatagramPacket datagramPacket = new DatagramPacket(data, DATA_PACKET_MAX_LEN);
                serverSocket.receive(datagramPacket);
                (new PacketHandler(datagramPacket, localRaft, username, serverSocket, scheduledExecutorService, logConfirmerNotifier, actionMap, followerLogMaintainerObject, readOnlyLog, encryption)).start();
            }
        } catch (IOException e) {
            // check if connection wasn't closed
            if (keepReceiving.get()) {
                System.out.println("An IOException was thrown from the Raft Receiver when trying to receive a data packet.");
                System.exit(1);
            }
        }
        scheduledExecutorService.shutdown();
    }
}
