package edu.oswego.cs.Packets;

import java.util.Arrays;
import java.util.Optional;

public enum Opcode {
    Connect(1),
    Command(2);

    public final short code;

    Opcode(int i) {
        code = (short) i;
    }

    /**
     * Grab the associated opcode given a short.
     * @param i Short value of the opcode.
     * @return The associated opcode.
     */
    public static Optional<Opcode> fromCode(short i) {
        return Arrays.stream(values()).filter(x -> x.code == i).findFirst();
    }
}
