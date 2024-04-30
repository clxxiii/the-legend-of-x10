package edu.oswego.cs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Args {

  public static HashMap<String, String> parse(String[] array) {
    HashMap<String, String> args = new HashMap<>();
    String[] input = Arrays.copyOf(array, 5);
    Scanner sc = new Scanner(System.in);

    if (array.length <= 0) {
      System.out.print("Welcome to The Legend of X10! What would you like to do? [host, join]: ");
      input[0] = sc.nextLine();
    }

    if (input[0].equalsIgnoreCase("host") || input[0].equalsIgnoreCase("join")) {
      args.put("action", input[0]);
    } else {
      System.out.println("First argument must be one of 'host' or 'join'.");
    }

    if (array.length <= 1) {
      System.out.print("What do you want your username to be: ");
      input[1] = sc.nextLine();
    }
    args.put("username", input[1]);

    if (input[0].equals("host")) {
      sc.close();
      return args;
    }

    if (array.length <= 2) {
      System.out.print("What IP would you like to connect to: ");
      input[2] = sc.nextLine();
    }
    args.put("ip", input[2]);

    if (array.length <= 3) {
      System.out.print("What Port is the game running on?: ");
      input[3] = sc.nextLine();
    }
    args.put("port", input[3]);

    if (array.length <= 4) {
      args.put("local_port", input[4]);
    }

    sc.close();
    return args;
  }
}
