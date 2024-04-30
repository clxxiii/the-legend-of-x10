package edu.oswego.cs.Packets;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ConnectionRedirectPacket extends ConnectPacket {

    public final SocketAddress originalAddress;
    public final PublicKey publicKey;

    public ConnectionRedirectPacket(SocketAddress originalAddress, String username, PublicKey publicKey) {
        super(ConnectSubopcode.Redirect, username, new byte[0]);
        this.originalAddress = originalAddress;
        this.publicKey = publicKey;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] keyBytes = publicKey.getEncoded();
        byte[] addrBytes = originalAddress.toString().replace("/", "").getBytes(StandardCharsets.UTF_8);
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int stringPaddingSize = 1;
        int bufferLength  = 2 * (Short.BYTES) + data.length + usernameBytes.length + stringPaddingSize + addrBytes.length + stringPaddingSize + keyBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(addrBytes);
        buffer.put((byte) 0x00);
        buffer.put(keyBytes);

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
        buffer.get(addrBytes);
        String addr = new String(addrBytes);
        String[] addrSplit = addr.split(":");
        if (addrSplit.length != 2) return null;

        // allow the buffer to continue past the string
        buffer.limit(bufferLimit);
        // get the null character
        buffer.get();

        byte[] keyBytes = new byte[buffer.remaining()];
        buffer.get(keyBytes);
        PublicKey key = null;
        try {
            key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("RSA algorithm does not exist.");
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key spec.");
        }
        return new ConnectionRedirectPacket(new InetSocketAddress(addrSplit[0], Integer.parseInt(addrSplit[1])), username, key);
    }
}
