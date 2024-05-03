package edu.oswego.cs.dungeon;

public class BossRoom extends Room {
  protected Dungeon dungeon;
  public Boss boss;

  public BossRoom(Dungeon dungeon) {
    this.dungeon = dungeon;
  }

  @Override
  public boolean isBossRoom() {
    return true;
  }
}
