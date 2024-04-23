package edu.oswego.cs.dungeon;

import java.util.ArrayList;

public class Dungeon {
  private ArrayList<Floor> floors;
  private long seed;

  public Dungeon(long seed) {
    floors = new ArrayList<>();
  }

  public Floor makeFloor() {
    FloorGenerator generator = new FloorGenerator(seed);
    Floor newFloor = generator.generate();
    floors.add(newFloor);
    updateSeed();
    return newFloor;
  }

  private void updateSeed() {
    seed ^= seed << 13;
    seed ^= seed >>> 7;
    seed ^= seed << 17;
  }

}
