package edu.oswego.cs.Packets;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ConnectionRedirectPacket extends ConnectPacket {

    public final SocketAddress originalAddress;

    public ConnectionRedirectPacket(SocketAddress originalAddress, String username, byte[] data) {
        super(ConnectSubopcode.Redirect, username, data);
        this.originalAddress = originalAddress;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] addrBytes = originalAddress.toString().replace("/", "").getBytes(StandardCharsets.UTF_8);
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int stringPaddingSize = 1;
        int bufferLength  = 2 * (Short.BYTES) + data.length + usernameBytes.length + stringPaddingSize + addrBytes.length + stringPaddingSize;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(addrBytes);
        buffer.put((byte) 0x00);
        buffer.put(data);

        buffer.flip();
        return buffer.array();
    }

    public static ConnectionRedirectPacket bytesToPacket(ByteBuffer buffer) {
        // get original limit
        int bufferLimit = buffer.limit();

        buffer.mark();
        while (buffer.hasRemaining() && buffer.get() != 0x00);
        buffer.limit(buffer.position() - 1);
        buffer.reset();
        byte[] usernameBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        String username = new String(usernameBytes);

        // allow the buffer to continue past the string
        buffer.limit(bufferLimit);
        // get the null character
        buffer.get();

        buffer.mark();
        while (buffer.hasRemaining() && buffer.get() != 0x00);
        buffer.limit(buffer.position() - 1);
        buffer.reset();
        byte[] addrBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(usernameBytes);
        String addr = new String(addrBytes);
        String[] addrSplit = addr.split(":");
        if (addrSplit.length != 2) return null;

        // allow the buffer to continue past the string
        buffer.limit(bufferLimit);
        // get the null character
        buffer.get();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new ConnectionRedirectPacket(new InetSocketAddress(addrSplit[0], Integer.parseInt(addrSplit[1])), username, data);
    }
}
