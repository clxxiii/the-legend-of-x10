package edu.oswego.cs.Packets;

import java.util.Arrays;
import java.util.Optional;

public enum ConnectSubopcode {
    ClientHello(1),
    ServerHello(2),
    ClientKey(3),
    Log(4),
    Redirect(5);

    public final short code;

    ConnectSubopcode(int i) {
        code = (short) i;
    }

    /**
     * Grab the associated ConnectSubopcode given a short.
     * @param i Short value of the ConnectSubopcode.
     * @return The associated ConnectSubopcode.
     */
    public static Optional<ConnectSubopcode> fromCode(short i) {
        return Arrays.stream(values()).filter(x -> x.code == i).findFirst();
    }
}
