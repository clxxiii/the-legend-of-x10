package edu.oswego.cs.dungeon.item;

import edu.oswego.cs.dungeon.Item;

public class Packet extends Item {

  public Packet() {
    this.name = "Packet";
  }

  public static int getSpawnOdds(int floor) {
    return 100;
  }
}
