package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Optional;

public class ConnectPacket extends Packet{
    public final ConnectSubopcode subopcode;
    public final byte[]  data;

    public ConnectPacket(ConnectSubopcode subopcode, byte[] data) {
        super(Opcode.Connect);
        this.subopcode = subopcode;
        this.data = data;
    }

    /**
     * Return a byte representation of the packet.
     * @return
     */
    public byte[] packetToBytes() {
        int bufferLength  = 2 * (Short.BYTES) + data.length;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(data);

        buffer.flip();
        return buffer.array();
    }

    /**
     * Return the given bytes in packet structure.
     * @param buffer
     * @return The ConnectPacket
     * @throws ParseException
     */
    public static ConnectPacket bytesToPacket(ByteBuffer buffer) throws ParseException {
        short subopcodeNumber = buffer.getShort();
        Optional<ConnectSubopcode> optionSubopcode = ConnectSubopcode.fromCode(subopcodeNumber);

        //Make sure something's actually there
        if(!optionSubopcode.isPresent()) {
            throw new ParseException("Invalid opcode", 0);
        }
        //Now get the ACTUAL opcode
        ConnectSubopcode opcode = optionSubopcode.get();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        return new ConnectPacket(opcode, data);
    }
}
