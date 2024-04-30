package edu.oswego.cs.dungeon;

import java.util.ArrayList;

//TODO: Will likely get moved to the game package, but not sure if Eli's doing stuff in there  for room generation atm.
public class GameUser implements Fightable{

    public final String username;

    //TODO: When the user moves up a floor, this should be switched to Room 0 of that floor.
    /**
     * Room the user is in.
     */
    public Room currentRoom;

    /**
     * Items the user currently has.
     */
    public ArrayList<Item> inventory;

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

    @Override
    public int getHp() { return hp; }

    public String getRoomNumber() {
        return currentRoom.prettyRoomNumber();
    }

    /**
     * Check if player's dead.
     * @return True if HP is 0, false otherwise.
     */
    @Override
    public boolean isDead() {
        return getHp() == 0;
    }

    /**
     * Deduct HP from the user.
     * @param damage Amount of HP to deduct.
     * @return HP remaining.
     */
    @Override
    public int attacked(int damage) {
        if(hp - damage < 0) {
            hp = 0;
        } else {
            hp -= damage;
        }

        return hp;
    }

    @Override
    public void attack(String target) {

    }




}
