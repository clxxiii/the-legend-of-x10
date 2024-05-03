package edu.oswego.cs.dungeon;

public class Boss extends Entity {

  private final static String PACKAGE_NAME = "edu.oswego.cs.dungeon.boss";
  private BossRoom room;

  public static Class<?>[] getAll() {
    return getAll(PACKAGE_NAME);
  }

  protected void setRoom(BossRoom room) {
    this.room = room;
  }

  /**
   * Deduct HP from the boss.
   * Generate the next floor upon death.
   * 
   * @param damage Amount of HP to deduct.
   * @return HP remaining.
   */
  @Override
  public int attacked(int damage) {
    if (getHp() - damage <= 0) {
      setHp(0);
      room.dungeon.makeFloor();
    } else {
      setHp(getHp() - damage);
    }

    return getHp();
  }
}
