package edu.oswego.cs.dungeon;

public class DungeonTester {
  public static void main(String[] args) {
    Dungeon d = new Dungeon(1003); 
    System.out.println(d.makeFloor().toString());
  } 
}
