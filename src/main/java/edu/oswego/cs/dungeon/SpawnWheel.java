package edu.oswego.cs.dungeon;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;

public class SpawnWheel<T> {
  HashMap<String, Integer> oddsList = new HashMap<>();
  HashMap<String, Class<T>> spawnableList = new HashMap<>();
  int wheelSlices = 0;
  private long seed;
  private int floor;
  private Random rand;

  // I spent at least 30 minutes trying to fix the type cast warning and failed so
  // I just added this instead.
  @SuppressWarnings("unchecked")
  public SpawnWheel(Class<T> clazz, int floor, long seed) {
    this.seed = seed;
    this.floor = floor;
    this.rand = new Random(this.seed);

    Class<T>[] items;
    try {
      Method getAll = clazz.getMethod("getAll");
      items = (Class<T>[]) getAll.invoke(clazz);
    } catch (ReflectiveOperationException e) {
      System.out.println("Given class is not Spawnable.");
      return;
    }
    for (Class<T> item : items) {
      int odds;
      try {
        Method spawnOddsMethod = item.getMethod("getSpawnOdds", new Class[] { int.class });
        odds = (int) spawnOddsMethod.invoke(item, this.floor);
      } catch (ReflectiveOperationException e) {
        System.out.println(
            item.getName() + " does not have a visible getSpawnOdds() method, and therefore cannot be spawned.");
        continue;
      }
      wheelSlices += odds;
      oddsList.put(item.getName(), odds);
      spawnableList.put(item.getName(), item);
    }
  }

  public Class<T> spinWheel() {
    int chance = rand.nextInt(wheelSlices);
    for (String spawnable : oddsList.keySet()) {
      int odds = oddsList.get(spawnable);
      chance -= odds;
      if (chance <= 0) {
        return spawnableList.get(spawnable);
      }
    }
    return null;
  }

  public T spinWheelAndMake() {
    int chance = rand.nextInt(wheelSlices);
    for (String name : oddsList.keySet()) {
      int odds = oddsList.get(name);
      chance -= odds;

      if (chance <= 0) {
        Class<T> spawnable = spawnableList.get(name);
        try {
          return spawnable.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
          System.out.println(name + " did not have a default constructor, so I cannot make one.");
          return null;
        }
      }

    }
    return null;
  }
}
