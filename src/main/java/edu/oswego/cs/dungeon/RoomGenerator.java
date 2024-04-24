package edu.oswego.cs.dungeon;

import java.util.Random;

public class RoomGenerator {
  private long seed;
  Random rand;
  int createdRooms = 0;

  protected RoomGenerator(long seed) {
    this.seed = seed;
    rand = new Random(this.seed);
  }

  protected Room generate() {
    Room room = new Room();
    room.roomNumber = createdRooms++;
    return room;
  }
}