package edu.oswego.cs.dungeon;

import java.io.Serializable;

public class Entity extends Spawnable implements Fightable {
  public String name;
  private int hp;
  private int damage;


  private final static String PACKAGE_NAME = "edu.oswego.cs.dungeon.entity";

  public static Class<?>[] getAll() {
    return getAll(PACKAGE_NAME);
  }

  @Override
  public int getHp() { return hp; }

  protected void setHp(int hp) { this.hp = hp; }

  /**
   * Check if player's dead.
   * @return True if HP is 0, false otherwise.
   */
  @Override
  public boolean isDead() {
    return getHp() == 0;
  }

  /**
   * Deduct HP from the user.
   * @param damage Amount of HP to deduct.
   * @return HP remaining.
   */
  @Override
  public int attacked(int damage) {
    if(hp - damage < 0) {
      hp = 0;
    } else {
      hp -= damage;
    }

    return hp;
  }

  @Override
  public void attack(String target) {

  }

}
