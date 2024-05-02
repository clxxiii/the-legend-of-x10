package edu.oswego.cs.dungeon;

public class BossRoom extends Room {
  protected Dungeon dungeon;
  public Boss boss;

  @Override
  public boolean isBossRoom() {
    return true;
  }
}
