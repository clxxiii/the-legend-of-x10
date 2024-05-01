package edu.oswego.cs.game;

import edu.oswego.cs.client.Command;

public class GameCommandOutput {
    public String username;
    public String textOutput;
    public boolean successful;

    public GameCommandOutput(){}

    public GameCommandOutput(String username, String textOutput, boolean successful) {
        this.username = username;
        this.textOutput = textOutput;
        this.successful = successful;
    }
}
