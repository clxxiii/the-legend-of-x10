package edu.oswego.cs.dungeon;

import java.util.List;

public class Room {
  protected int roomNumber;
  protected Room northExit;
  protected Room eastExit;
  protected Room southExit;
  protected Room westExit;

  protected List<Item> items;
  protected List<Entity> entities;

  protected int xPos;
  protected int yPos;

  protected Room() {
  }

  protected void setCoordinates(int x, int y) {
    xPos = x;
    yPos = y;
  }

  public String toString() {
    String topChar = northExit != null ? " " : "─";
    String rightChar = eastExit != null ? " " : "│";
    String bottomChar = southExit != null ? " " : "─";
    String leftChar = westExit != null ? " " : "│";

    return "┌─" + topChar + "─┐\n" +
        leftChar + prettyRoomNumber() + rightChar + "\n" +
        "└─" + bottomChar + "─┘";
  }

  String prettyRoomNumber() {
    if (roomNumber < 10) {
      return "00" + roomNumber;
    } else if (roomNumber < 100) {
      return "0" + roomNumber;
    } else {
      return "" + roomNumber;
    }
  }
}
