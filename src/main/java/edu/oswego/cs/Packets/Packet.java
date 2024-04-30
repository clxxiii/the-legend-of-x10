package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Optional;

//My name's gonna be on this, but Eli was a lot smarter than I was for
//packet structures in project 2, so I'm going by that.
//Comments are mine, however.
public abstract class Packet {
    public final Opcode opcode;
    public final String username;

    protected Packet(String username, Opcode opcode) {
        this.username = username;
        this.opcode = opcode;
    }

    public abstract byte[] packetToBytes();

    public static Packet bytesToPacket(ByteBuffer buffer) throws ParseException {
        buffer.flip();

        //Get the opcode short from the packet
        short code = buffer.getShort();
        Optional<Opcode> optionOpcode = Opcode.fromCode(code);

        //Make sure something's actually there
        if(!optionOpcode.isPresent()) {
            return null;
        }

        //Now get the ACTUAL opcode
        Opcode opcode = optionOpcode.get();

        //This seems redundant considering we got the stuff earlier straight from the bytes,
        //but that was necessary to validate we actually got a valid opcode.
        //TODO: Fill this out once we get actual packet types
        switch (opcode) {
            case Connect:
                return ConnectPacket.bytesToPacket(buffer);
            case Ack:
                return AckPacket.bytesToPacket(buffer);
            case Heartbeat:
                return HeartbeatPacket.bytesToPacket(buffer);
            case Command:
                return CommandPacket.bytesToPacket(buffer);
            case Log:
                return LogPacket.bytesToPacket(buffer);
            default:
                return null;
        }

    }
}
