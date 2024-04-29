package edu.oswego.cs;

import edu.oswego.cs.client.Client;
import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Floor;
import edu.oswego.cs.dungeon.GameUser;
import edu.oswego.cs.gui.MainFrame;
import edu.oswego.cs.raft.Raft;

import java.net.InetSocketAddress;

public class App {

    private static final int DEFAULT_PORT = 26910;

    public static void main(String[] args) throws Exception {
        String clientUsername = "Victor";
        MainFrame mainFrame = new MainFrame();
        Raft raft = new Raft(DEFAULT_PORT, clientUsername, mainFrame);
        mainFrame.setRaft(raft);
        raft.startRaftGroup();
//        Raft raft = new Raft(26910, "DeveloperDave");
//        raft.joinRaftGroup(new InetSocketAddress("192.168.0.101", 26910));
    }
}
