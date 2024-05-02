package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.Opcode;

import java.util.Arrays;
import java.util.Optional;

public enum Vote {
    NO(0),
    YES(1);

    public final short value;

    Vote(int i) {
        value = (short) i;
    }

    /**
     * Grab the associated opcode given a short.
     * @param i Short value of the opcode.
     * @return The associated opcode.
     */
    public static Optional<Vote> fromValue(short i) {
        return Arrays.stream(values()).filter(x -> x.value == i).findFirst();
    }
}
