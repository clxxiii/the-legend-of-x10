package edu.oswego.cs.dungeon;

public class Entity extends Spawnable {
  public String name;

  private final static String PACKAGE_NAME = "edu.oswego.cs.dungeon.entity";

  public static Class<?>[] getAll() {
    return getAll(PACKAGE_NAME);
  }
}
