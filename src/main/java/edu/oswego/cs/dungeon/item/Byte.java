package edu.oswego.cs.dungeon.item;

import edu.oswego.cs.dungeon.Item;

public class Byte extends Item {

  public Byte() {
    this.name = "Byte";
  }

  public static int getSpawnOdds(int floor) {
    return 100;
  }
}
