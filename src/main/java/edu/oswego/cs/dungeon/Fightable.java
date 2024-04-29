package edu.oswego.cs.dungeon;

public interface Fightable {
    public int getHp();
    public boolean isDead();
    public int attacked(int damage);
    public void attack(String target);
}
