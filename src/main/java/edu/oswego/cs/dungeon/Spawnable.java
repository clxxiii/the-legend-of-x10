package edu.oswego.cs.dungeon;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public abstract class Spawnable {
  public static int getSpawnOdds(int floor) {
    return 50;
  }

  /**
   * Shoutout
   * http://www.java2s.com/example/java/reflection/returns-all-classes-from-specified-package.html
   */
  public static Class<?>[] getAll(String packageName) {
    ArrayList<Class<?>> classes = new ArrayList<>();

    File directory = null;
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) {
        System.out.println("Can't get class loader");
        return null;
      }
      String path = packageName.replaceAll("\\.", "/");
      URL resource = cl.getResource(path);
      if (resource == null) {
        System.out.println("Could not get item resource.");
        return null;
      }
      directory = new File(resource.getFile());
    } catch (NullPointerException e) {
      System.out.println(packageName + " appeared to be an invalid package.");
      return null;
    }

    if (directory.exists()) {
      String[] files = directory.list();
      for (String name : files) {
        if (!name.endsWith(".class"))
          continue;

        try {
          classes.add((Class<?>) Class.forName(packageName + "." + name.substring(0, name.length() - 6)));
        } catch (ClassNotFoundException e) {
          System.out.println("Could not find packages correctly in ItemPackage.");
        }
      }
    } else {
      System.out.println(packageName + " appeared to be an invalid package.");
      return null;
    }

    Class<?>[] classArr = new Class[classes.size()];
    classes.toArray(classArr);
    return classArr;
  }
}
