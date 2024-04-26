package edu.oswego.cs.dungeon;

import java.util.ArrayList;

public class Dungeon {
  private ArrayList<Floor> floors;
  private long seed;

  public Dungeon(long seed) {
    floors = new ArrayList<>();
    this.seed = seed;
  }

  public Floor makeFloor() {
    FloorGenerator generator = new FloorGenerator(seed);
    Floor newFloor = generator.generate(floors.size() + 1);
    floors.add(newFloor);
    updateSeed();
    return newFloor;
  }

  private void updateSeed() {
    seed ^= seed << 13;
    seed ^= seed >>> 7;
    seed ^= seed << 17;
  }

  public static void main(String[] args) {
    System.out.println(new Dungeon(123098123891l).makeFloor().toString());
  }
}
