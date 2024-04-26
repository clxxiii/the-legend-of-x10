package edu.oswego.cs.dungeon;

public class Item extends Spawnable {
  public String name;
  private final static String PACKAGE_NAME = "edu.oswego.cs.dungeon.item";

  public static Class<?>[] getAll() {
    return getAll(PACKAGE_NAME);
  }
}
