package edu.oswego.cs.Packets;

import java.util.Arrays;
import java.util.Optional;

public enum CommandSubopcode {
    RequestCommand(1),
    LogCommand(2),
    ConfirmCommand(3),
    CommitCommand(4);

    public final short code;

    CommandSubopcode(int i) {
        code = (short) i;
    }

    /**
     * Grab the associated CommandSubopcode given a short.
     * @param i Short value of the CommandSubopcode.
     * @return The associated CommandSubopcode.
     */
    public static Optional<CommandSubopcode> fromCode(short i) {
        return Arrays.stream(values()).filter(x -> x.code == i).findFirst();
    }
}
