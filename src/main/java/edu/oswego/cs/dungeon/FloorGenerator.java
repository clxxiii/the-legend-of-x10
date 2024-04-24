package edu.oswego.cs.dungeon;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class FloorGenerator {
  private long seed;
  private final Random rand;
  private RoomGenerator roomGen;
  private HashMap<String, Room> map = new HashMap<>();
  private LinkedList<Room> leaves = new LinkedList<>();
  private static final int FLOOR_ROOM_COUNT = 10;
  private int roomsToMake = 0;

  public FloorGenerator(long seed) {
    this.seed = seed;
    rand = new Random(this.seed);
  }

  public Floor generate() {
    map.clear();
    leaves.clear();
    roomsToMake = FLOOR_ROOM_COUNT;

    roomGen = new RoomGenerator(seed);

    Floor floor = new Floor();

    Room startingRoom = roomGen.generate();
    floor.entryPoint = startingRoom;

    startingRoom.setCoordinates(0, 0);
    map.put("0,0", startingRoom);
    leaves.add(startingRoom);

    while (!leaves.isEmpty() && roomsToMake > 2) {
      makeBranches();
    }

    floor.rooms = map;
    return floor;
  }

  private void makeBranches() {
    Room current = leaves.poll();

    // [N, E, S, W]
    int[] odds = getGenerationOdds();
    boolean connected = false;
    if (random(odds[0])) {
      makeRoom(ExitEnum.NORTH, current);
      connected = true;
    }
    if (random(odds[1])) {
      makeRoom(ExitEnum.EAST, current);
      connected = true;
    }
    if (random(odds[2])) {
      makeRoom(ExitEnum.SOUTH, current);
      connected = true;
    }
    if (random(odds[3])) {
      makeRoom(ExitEnum.WEST, current);
      connected = true;
    }
    if (!connected) {
      makeRoom(ExitEnum.random(rand), current);
    }
  }

  private void makeRoom(ExitEnum exit, Room connectedRoom) {
    int newX = connectedRoom.xPos + exit.x;
    int newY = connectedRoom.yPos + exit.y;

    if (map.containsKey(newX + "," + newY)) {
      // Flip a coin, if heads, link the two rooms.
      if (random(0))
        return;
      Room otherRoom = map.get(newX + "," + newY);
      int xDiff = otherRoom.xPos - connectedRoom.xPos;
      int yDiff = otherRoom.yPos - connectedRoom.yPos;
      if (yDiff == 0) {
        if (xDiff == 1) {
          connectedRoom.eastExit = otherRoom;
          otherRoom.westExit = connectedRoom;
        }
        if (xDiff == -1) {
          connectedRoom.westExit = otherRoom;
          otherRoom.eastExit = connectedRoom;
        }
      } else if (yDiff == 1) {
        connectedRoom.northExit = otherRoom;
        otherRoom.southExit = connectedRoom;
      } else {
        connectedRoom.southExit = otherRoom;
        otherRoom.northExit = connectedRoom;
      }
      return;
    }

    Room room = roomGen.generate();
    room.setCoordinates(newX, newY);
    map.put(newX + "," + newY, room);
    leaves.add(room);

    switch (exit) {
      case NORTH:
        connectedRoom.northExit = room;
        room.southExit = connectedRoom;
        break;
      case EAST:
        connectedRoom.eastExit = room;
        room.westExit = connectedRoom;
        break;
      case SOUTH:
        connectedRoom.southExit = room;
        room.northExit = connectedRoom;
        break;
      case WEST:
        connectedRoom.westExit = room;
        room.eastExit = connectedRoom;
        break;
    }
    roomsToMake--;
  }

  /**
   * Returns the probability of each room being generated in an array:
   * [N, E, S, W]
   * 
   * @return
   */
  private int[] getGenerationOdds() {
    return new int[] { 50, 50, 50, 50 };
  }

  /**
   * Generates a random boolean with a specific chance (out of 100)
   * 
   * @param r
   * @param chance
   * @return
   */
  private boolean random(int chance) {
    int num = rand.nextInt(100);
    return num < chance;
  }
}