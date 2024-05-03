package edu.oswego.cs.dungeon;

import java.util.HashMap;

public class Floor {
  protected Dungeon dungeon;
  protected Room entryPoint;
  protected HashMap<String, Room> rooms;

  // Prevent users from constructing a Floor directly
  protected Floor(Dungeon dungeon) {
    this.dungeon = dungeon;
  }

  public Room getEntrance() {
    return entryPoint;
  }

  public String toString() {
    return new FloorPrinter(this).getDebugString();
  }
}
