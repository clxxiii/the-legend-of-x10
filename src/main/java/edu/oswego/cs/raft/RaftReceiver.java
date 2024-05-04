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

    /**
     * Creates a RaftReceiver Thread that accepts new packets and sends them off to a new a thread to be handled.
     * @param serverSocket The server socket associated with a raft instance.
     * @param keepReceiving An atomic boolean that allows for graceful shutdown of the receiver once the raft instance is exited.
     * @param localRaft A reference to the raft instance this handler is associated with.
     * @param username The username connected to the associated raft instance.
     * @param logConfirmerNotifier An object the RaftLogConfirmer waits on and can be notified upon certain packets being received. (Saves CPU cycles)
     * @param actionMap A map of all the actions that are cached for out of order message reordering.
     * @param followerLogMaintainerObject An object the FollowerLogMaintainer waits on and is notified when certain packets are received. (Saves CPU cycles)
     * @param readOnlyLog The raft log (intended to be read only)
     * @param encryption The encryption object that allows for Public Key, Private Key, and Secret Key use when sending/receiving messages.
     */
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
