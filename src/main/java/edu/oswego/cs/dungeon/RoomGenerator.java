package edu.oswego.cs.dungeon;

import java.util.Random;

public class RoomGenerator {
  private long seed;
  Random rand;

  protected RoomGenerator(long seed) {
    this.seed = seed;
    rand = new Random(this.seed);
  }

  protected Room generate() {
    return new Room();
  }
}