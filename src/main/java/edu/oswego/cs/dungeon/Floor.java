package edu.oswego.cs.dungeon;

import java.util.HashMap;

public class Floor {
  protected Room entryPoint;
  protected HashMap<String, Room> rooms;

  // Prevent users from constructing a Floor directly
  protected Floor() {
  }

  public Room getEntrance() {
    return entryPoint;
  }

  public String toString() {
    return new FloorPrinter(this).getDebugString();
  }
}
