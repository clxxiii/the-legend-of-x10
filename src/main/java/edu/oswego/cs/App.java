package edu.oswego.cs;

import edu.oswego.cs.client.Client;
import edu.oswego.cs.raft.Raft;

import java.net.InetSocketAddress;

public class App {

    private static final int DEFAULT_PORT = 26910;

    public static void main(String[] args) throws Exception {
//        Raft raft = new Raft(DEFAULT_PORT, "DeveloperDave");
//        raft.startRaftGroup();
        Raft raft = new Raft(26910, "DeveloperDave");
        raft.joinRaftGroup(new InetSocketAddress("192.168.0.101", 26910));
        Client client = new Client(raft);
        client.start();
    }
}
