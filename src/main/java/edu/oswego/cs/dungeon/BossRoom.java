package edu.oswego.cs.dungeon;

public class BossRoom extends Room {
  private Dungeon dungeon;
  public Boss boss;

  @Override
  public boolean isBossRoom() {
    return true;
  }
}
