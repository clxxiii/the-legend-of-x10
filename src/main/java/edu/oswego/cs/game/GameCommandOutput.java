package edu.oswego.cs.game;

import edu.oswego.cs.client.Command;
import edu.oswego.cs.dungeon.Floor;
import edu.oswego.cs.dungeon.Room;

public class GameCommandOutput {
    public String username;
    public String textOutput;
    public boolean successful;
    public Room room;
    public Floor floor;

    public GameCommandOutput(){}

    public GameCommandOutput(String username, String textOutput, boolean successful) {
        this.username = username;
        this.textOutput = textOutput;
        this.successful = successful;
    }


}
