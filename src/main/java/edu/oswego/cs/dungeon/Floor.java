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
    // Get Offset for Array Indexes
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (Room room : rooms.values()) {
      if (minX > room.xPos)
        minX = room.xPos;
      if (minY > room.yPos)
        minY = room.yPos;
      if (maxX < room.xPos)
        maxX = room.xPos;
      if (maxY < room.yPos)
        maxY = room.yPos;
    }
    int xOffset = Math.abs(minX);
    int yOffset = Math.abs(minY);

    String[][][] map = new String[(maxY - minY) + 1][(maxX - minX) + 1][];
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        Room room = rooms.get(x + "," + y);
        if (room == null) {
          map[y + yOffset][x + xOffset] = new String[] { "     ", "     ", "     " };
          continue;
        }
        map[y + yOffset][x + xOffset] = room.toString().split("\n");
      }
    }

    // Printing the Map
    String output = "";
    for (int lineNum = map.length - 1; lineNum >= 0; lineNum--) {
      String[][] line = map[lineNum];
      String row0 = "";
      String row1 = "";
      String row2 = "";
      for (int cellNum = 0; cellNum < line.length; cellNum++) {
        String[] cell = line[cellNum];
        row0 += cell[0];
        row1 += cell[1];
        row2 += cell[2];
      }
      output += row0 + "\n" + row1 + "\n" + row2 + "\n";
    }

    // Print out the contents of each room:
    for (Room room : rooms.values()) {
      String roomString = room.prettyRoomNumber() + ": ";
      for (Item i : room.items) {
        roomString += i.name + ", ";
      }
      for (Entity e : room.entities) {
        roomString += e.name + ", ";
      }
      roomString = roomString.substring(0, roomString.length() - 2);
      output += roomString + "\n";
    }

    return output;
  }
}
