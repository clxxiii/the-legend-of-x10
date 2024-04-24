package edu.oswego.cs;

import edu.oswego.cs.dungeon.Dungeon;
import edu.oswego.cs.dungeon.Floor;

public class App {

    public static void main(String[] args) throws Exception {
        Floor floor = new Dungeon((long) 9128332).makeFloor();
        System.out.println(floor.toString());
    }
}
