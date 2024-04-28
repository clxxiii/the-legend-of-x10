package edu.oswego.cs.mechanics;

import edu.oswego.cs.dungeon.Item;
import edu.oswego.cs.dungeon.Room;

import java.util.ArrayList;

//TODO: Will likely get moved to the game package, but not sure if Eli's doing stuff in there  for room generation atm.
public class GameUser {

    public final String username;

    //TODO: When the user moves up a floor, this should be switched to Room 0 of that floor.
    /**
     * Room the user is in.
     */
    Room currentRoom;

    /**
     * Items the user currently has.
     */
    ArrayList<Item> inventory;

    //TODO: Consider making this atomic
    /**
     * Health of the player.
     */
    private int hp = 100;

    /**
     * Amount of damage the user deals.
     */
    private int attackPower = 20;


    public GameUser(Room currentRoom, String username) {
        this.currentRoom = currentRoom;
        this.username = username;
    }


    public int getHp() { return hp; }

    public String getRoomNumber() {
        return currentRoom.prettyRoomNumber();
    }

    /**
     * Check if player's dead.
     * @return True if HP is 0, false otherwise.
     */
    public boolean isDead() {
        return getHp() == 0;
    }

    /**
     * Deduct HP from the user.
     * @param damage Amount of HP to deduct.
     * @return HP remaining.
     */
    public int attacked(int damage) {
        if(hp - damage < 0) {
            hp = 0;
        } else {
            hp -= damage;
        }

        return hp;
    }

    /**
     * Attempts to change the room the user is in.
     * @param direction Room to move to.  Valid values: 'N', 'S', 'E', 'W'
     * @return True on success, false if not.
     */
    //TODO: Need to notify that player has moved.  Rooms need to be updated to know what's up.  Raft and all that too.
    public boolean changeRooms(char direction) {

        char directionCaps = Character.toUpperCase(direction);

        switch(directionCaps) {
            case 'N':
                if(this.currentRoom.northExit == null) return false;

                this.currentRoom = this.currentRoom.northExit;
                return true;
            case 'S':
                if(this.currentRoom.southExit == null) return false;

                this.currentRoom = this.currentRoom.southExit;
                return true;
            case 'E':
                if(this.currentRoom.eastExit == null) return false;

                this.currentRoom = this.currentRoom.eastExit;
                return true;
            case 'W':
                if(this.currentRoom.westExit == null) return false;

                this.currentRoom = this.currentRoom.westExit;
                return true;
            default:
                return false;
        }
    }


}
