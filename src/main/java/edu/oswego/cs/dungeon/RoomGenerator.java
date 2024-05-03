package edu.oswego.cs.dungeon;

import java.util.Random;

public class RoomGenerator {
  private long seed;
  private Random rand;
  private SpawnWheel<Item> itemWheel;
  private SpawnWheel<Entity> entityWheel;
  private SpawnWheel<Boss> bossWheel;
  private Dungeon dungeon;
  int createdRooms = 0;

  protected RoomGenerator(long seed, SpawnWheel<Item> itemWheel, SpawnWheel<Entity> entityWheel,
      SpawnWheel<Boss> bossWheel, Dungeon dungeon) {
    this.seed = seed;
    this.itemWheel = itemWheel;
    this.entityWheel = entityWheel;
    this.bossWheel = bossWheel;
    rand = new Random(this.seed);
    this.dungeon = dungeon;
  }

  protected Room generate() {
    Room room = new Room();
    room.roomNumber = createdRooms++;

    for (int chance : getItemOdds()) {
      if (!random(chance))
        break;

      room.addItem(itemWheel.spinWheelAndMake());
    }

    for (int chance : getEntityOdds()) {
      if (!random(chance))
        break;

      room.addEntity(entityWheel.spinWheelAndMake());
    }

    return room;
  }

  protected BossRoom generateBossRoom() {
    BossRoom room = new BossRoom(dungeon);
    room.roomNumber = createdRooms++;

    Boss boss = bossWheel.spinWheelAndMake();

    room.addEntity(boss);
    room.boss = boss;
    boss.setRoom(room);

    return room;
  }

  /**
   * Returns the probability of a certain amount of items being generated in a
   * room.
   * [1 item chance, 2 item chance (given 1 item has been spawned), ...]
   * 
   * @return
   */
  private int[] getItemOdds() {
    return new int[] { 50, 50 };
  }

  /**
   * Returns the probability of a certain amount of entities being generated in a
   * room.
   * [1 item chance, 2 item chance (given 1 item has been spawned), ...]
   * 
   * @return
   */
  private int[] getEntityOdds() {
    return new int[] { 10, 10 };
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