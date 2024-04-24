package edu.oswego.cs;

import edu.oswego.cs.client.Client;
import edu.oswego.cs.raft.Raft;

import java.net.InetSocketAddress;

public class App {

    private static final int DEFAULT_PORT = 26910;

    public static void main(String[] args) throws Exception {
        Raft raft = new Raft(DEFAULT_PORT, "DeveloperDave");
        raft.startRaftGroup();
        Client client = new Client(raft);
        client.start();
    }
}
