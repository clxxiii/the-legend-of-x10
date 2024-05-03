package edu.oswego.cs.dungeon;

import edu.oswego.cs.game.GameCommandOutput;

public class Item extends Spawnable {
  public String name;
  private final static String PACKAGE_NAME = "edu.oswego.cs.dungeon.item";

  public static Class<?>[] getAll() {
    return getAll(PACKAGE_NAME);
  }

  public GameCommandOutput use(Dungeon dungeon, GameUser user) {
    GameCommandOutput output = new GameCommandOutput(name, "You used the " + name + ", it did nothing.", false);

    return output;
  }
}
