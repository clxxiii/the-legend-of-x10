package edu.oswego.cs;

import edu.oswego.cs.gui.MainFrame;
import edu.oswego.cs.raft.Raft;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class App {

    private static final int DEFAULT_PORT = 26910;

    public static void main(String[] args) throws Exception {
        HashMap<String, String> map = Args.parse(args);
        int localPort;
        if (map.get("local_port") != null) {
            localPort = Integer.parseInt(map.get("local_port"));
        } else {
            localPort = DEFAULT_PORT;
        }

        String clientUsername = map.get("username");
        Raft raft = new Raft(localPort, clientUsername);

        if (map.get("action").equalsIgnoreCase("host")) {
            raft.startRaftGroup();
        } else {
            raft.joinRaftGroup(new InetSocketAddress(map.get("ip"), Integer.parseInt(map.get("port"))));
        }
    }
}
