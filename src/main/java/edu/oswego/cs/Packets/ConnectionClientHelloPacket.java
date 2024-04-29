package edu.oswego.cs.Packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ConnectionClientHelloPacket extends ConnectPacket {

    public final PublicKey publicKey;

    public ConnectionClientHelloPacket(String username, PublicKey publicKey) {
        super(ConnectSubopcode.ClientHello, username, new byte[0]);
        this.publicKey = publicKey;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] keyBytes = publicKey.getEncoded();
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int stringPaddingSize = 1;
        int bufferLength  = 2 * (Short.BYTES) + data.length + usernameBytes.length + stringPaddingSize + keyBytes.length + stringPaddingSize;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(keyBytes);
        buffer.put((byte) 0x00);
        buffer.put(data);

        buffer.flip();
        return buffer.array();
    }

    public static ConnectionClientHelloPacket bytesToPacket(ByteBuffer buffer) {
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
        byte[] keyBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(keyBytes);
        PublicKey key = null;
        try {
            key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("RSA algorithm does not exist.");
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key spec.");
        }

        // allow the buffer to continue past the string
        buffer.limit(bufferLimit);
        // get the null character
        buffer.get();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new ConnectionClientHelloPacket(username, key);
    }
}
