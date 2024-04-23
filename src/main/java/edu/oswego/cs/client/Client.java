package edu.oswego.cs.client;

import edu.oswego.cs.raft.Raft;

import java.util.Optional;
import java.util.Scanner;

public class Client {
  
   Raft localRaft;

   public Client(Raft localRaft) {
      this.localRaft = localRaft;
   }

   public void start() {
      Scanner in = new Scanner(System.in);
      while (true) {
         String input = in.nextLine();
         String[] inputBrokenDown = input.split(" ", 2);
         Optional<Command> commandOptional = Command.parse(inputBrokenDown[0]);
         if (commandOptional.isPresent()) {
            Command command = commandOptional.get();
            if (command.equals(Command.EXIT)) {
               localRaft.exitRaft();
               in.close();
               return;
            } else if (command.equals(Command.CHAT)) {
               localRaft.sendMessage(input);
            }
         }
      }
   }
}
