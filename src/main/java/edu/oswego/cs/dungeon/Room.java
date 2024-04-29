package edu.oswego.cs.dungeon;

import java.util.ArrayList;
import java.util.List;

//CHANGE BY VICTOR: Made room references public, made prettyRoomNumber public
public class Room {
  int roomNumber;
  public Room northExit;
  public Room eastExit;
  public Room southExit;
  public Room westExit;

  public List<Item> items;
  public List<Entity> entities;

  int xPos;
  int yPos;

  Room() {
    items = new ArrayList<>();
    entities = new ArrayList<>();
  }

  void setCoordinates(int x, int y) {
    xPos = x;
    yPos = y;
  }

  void addItem(Item i) {
    items.add(i);
  }

  void addEntity(Entity e) {
    entities.add(e);
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

  public String prettyRoomNumber() {
    if (roomNumber < 10) {
      return "00" + roomNumber;
    } else if (roomNumber < 100) {
      return "0" + roomNumber;
    } else {
      return "" + roomNumber;
    }
  }
}
