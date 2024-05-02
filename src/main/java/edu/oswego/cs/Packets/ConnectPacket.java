package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Optional;

public class ConnectPacket extends Packet{
    public final ConnectSubopcode subopcode;
    public final byte[]  data;

    public ConnectPacket(ConnectSubopcode subopcode, String username, byte[] data) {
        super(username, Opcode.Connect);
        this.subopcode = subopcode;
        this.data = data;
    }

    /**
     * Return a byte representation of the packet.
     * @return
     */
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int stringPaddingSize = 1;
        int bufferLength  = 2 * (Short.BYTES) + data.length + usernameBytes.length + stringPaddingSize;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
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
            return null;
        }
        //Now get the ACTUAL opcode
        ConnectSubopcode subOpcode = optionSubopcode.get();

        switch (subOpcode) {
            case Redirect:
                return ConnectionRedirectPacket.bytesToPacket(buffer);
            case ClientHello:
                return ConnectionClientHelloPacket.bytesToPacket(buffer);
            case ServerHello:
                return ConnectionServerHelloPacket.bytesToPacket(buffer);
            default:
                // get original limit
                int bufferLimit = buffer.limit();

                buffer.mark();
                while (buffer.hasRemaining() && buffer.get() != 0x00) ;
                buffer.limit(buffer.position() - 1);
                buffer.reset();
                byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
                buffer.get(usernameBytes);
                String username = new String(usernameBytes);

                // allow the buffer to continue past the string
                buffer.limit(bufferLimit);
                // get the null character
                buffer.get();

                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                return new ConnectPacket(subOpcode, username, data);
        }
    }
}
