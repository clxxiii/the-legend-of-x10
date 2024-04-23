package edu.oswego.cs.dungeon;

import java.util.Random;

public enum ExitEnum {
  NORTH(0, 1),
  EAST(1, 0),
  SOUTH(0, -1),
  WEST(-1, 0);

  public int x;
  public int y;

  private ExitEnum(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public static ExitEnum random() {
    int rand = new Random().nextInt(3);
    switch (rand) {
      case 0:
        return ExitEnum.NORTH;
      case 1:
        return ExitEnum.EAST;
      case 2:
        return ExitEnum.SOUTH;
      default:
        return ExitEnum.WEST;
    }
  }
}
