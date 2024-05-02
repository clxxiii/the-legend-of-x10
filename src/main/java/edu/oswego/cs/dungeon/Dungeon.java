package edu.oswego.cs.dungeon;

import edu.oswego.cs.game.GameCommandOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Dungeon {
    private ArrayList<Floor> floors;
    private HashMap<String, GameUser> currentUsers;
    private long seed;
    private Random rand;

    public void addUser(GameUser gameUser) {
        if (currentUsers == null)
            currentUsers = new HashMap<>();
        currentUsers.put(gameUser.username, gameUser);
        gameUser.currentRoom.addUser(gameUser);
    }

    public Dungeon(long seed) {
        floors = new ArrayList<>();
        this.seed = seed;
        this.rand = new Random(seed);
    }

    public Floor makeFloor() {
        FloorGenerator generator = new FloorGenerator(rand);
        Floor newFloor = generator.generate(floors.size() + 1);
        floors.add(newFloor);
        return newFloor;
    }

    // TODO: Send back output text if someone entered your room
    public GameCommandOutput move(String username, char direction) {
        GameCommandOutput output = new GameCommandOutput(username, "Can't move that way!", false);

        GameUser gameUser = currentUsers.get(username);
        char directionCaps = Character.toUpperCase(direction);

        Room roomToRemove = gameUser.currentRoom;

        switch (directionCaps) {
            case 'N':
                if (gameUser.currentRoom.northExit == null)
                    break;

                gameUser.currentRoom = gameUser.currentRoom.northExit;
                output.successful = true;
                break;
            case 'S':
                if (gameUser.currentRoom.southExit == null)
                    break;

                gameUser.currentRoom = gameUser.currentRoom.southExit;
                output.successful = true;
                break;
            case 'E':
                if (gameUser.currentRoom.eastExit == null)
                    break;

                gameUser.currentRoom = gameUser.currentRoom.eastExit;
                output.successful = true;
                break;
            case 'W':
                if (gameUser.currentRoom.westExit == null)
                    break;

                gameUser.currentRoom = gameUser.currentRoom.westExit;
                output.successful = true;
                break;
            default:
                break;
        }

        if (output.successful) {
            roomToRemove.removeUser(gameUser);
            gameUser.currentRoom.addUser(gameUser);
            output.textOutput = "Moved " + direction + ". Current room: " + gameUser.currentRoom.prettyRoomNumber()
                    + ".";
            output.room = gameUser.currentRoom;
        }

        return output;
    }

    public GameCommandOutput ascend(String username) {
        GameCommandOutput output = new GameCommandOutput(username, "Ascended!", true);
        GameUser gameUser = currentUsers.get(username);
        Room roomToRemove = gameUser.currentRoom;
        //Room roomToMove = this.floors.get(this.floors.size() -1 );
        return null;
    }

    public GameCommandOutput attack(String username, String target) {
        GameCommandOutput output = new GameCommandOutput(username, "", false);

        GameUser gameUser = currentUsers.get(username);
        if (gameUser.currentRoom.entities.isEmpty()) {
            output.textOutput = "Nothing here to attack!";
            return output;
        }

        List<Entity> entityList = gameUser.currentRoom.entities;
        Entity entity = null;
        for (int i = 0; i < entityList.size(); i++) {
            if (entityList.get(i).name.equalsIgnoreCase(target)) {
                entity = entityList.get(i);
                break;
            }
        }

        if (entity == null) {
            output.textOutput = "Specified entity is not in the room!";
            return output;
        }

        entity.attacked(gameUser.getAttackPower());
        output.successful = true;
        if (entity.isDead()) {
            output.textOutput = "You hit and killed " + entity.name + "!";
        } else {
            output.textOutput = "You hit " + entity.name + "! It has " + entity.getHp() + " HP remaining.";
        }
        return output;
    }
}
