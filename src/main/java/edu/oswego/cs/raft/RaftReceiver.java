package edu.oswego.cs.raft;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftReceiver extends Thread {
    private final DatagramSocket serverSocket;
    private final AtomicBoolean keepReceiving;
    private final int DATA_PACKET_MAX_LEN = 1024;
    private final Raft localRaft;
    private final String username;

    public RaftReceiver(DatagramSocket serverSocket, AtomicBoolean keepReceiving, Raft localRaft, String username) {
        this.serverSocket = serverSocket;
        this.keepReceiving = keepReceiving;
        this.localRaft = localRaft;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            while (keepReceiving.get()) {
                byte[] data = new byte[DATA_PACKET_MAX_LEN];
                DatagramPacket datagramPacket = new DatagramPacket(data, DATA_PACKET_MAX_LEN);
                serverSocket.receive(datagramPacket);
                (new PacketHandler(datagramPacket, localRaft, username, serverSocket)).start();
            }
        } catch (IOException e) {
            // check if connection wasn't closed
            if (keepReceiving.get()) {
                System.out.println("An IOException was thrown from the Raft Receiver when trying to receive a data packet.");
                System.exit(1);
            }
        }
    }
}
