package edu.oswego.cs.gui;

import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Floor;


public class GuiMainTesting {

    public static void main(String[] args) throws Exception {
        Dungeon dungeon = new Dungeon(123098123891l);
        Floor currentFloor = dungeon.makeFloor();

        MainFrame frame = new MainFrame(dungeon, currentFloor);

        Floor newFloor = dungeon.makeFloor();
        frame.currentFloor = newFloor;
        frame.updateMapOutput();
    }
}
