package edu.oswego.cs.dungeon;

import java.util.List;

public class Room {
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
}
