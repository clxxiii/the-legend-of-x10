package edu.oswego.cs.Packets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ConnectionServerHelloPacket extends ConnectPacket {

    public final byte[] encryptedSecretKey;

    public ConnectionServerHelloPacket(String username, byte[] encryptedSecretKey) {
        super(ConnectSubopcode.ServerHello, username, new byte[0]);
        this.encryptedSecretKey = encryptedSecretKey;
    }

    @Override
    public byte[] packetToBytes() {
        byte[] usernameBytes = this.username.getBytes(StandardCharsets.UTF_8);
        int stringPaddingSize = 1;
        int bufferLength  = 2 * (Short.BYTES) + data.length + usernameBytes.length + stringPaddingSize + encryptedSecretKey.length;

        ByteBuffer buffer = ByteBuffer.allocate(bufferLength);
        buffer.putShort(this.opcode.code);
        buffer.putShort(this.subopcode.code);
        buffer.put(usernameBytes);
        buffer.put((byte) 0x00);
        buffer.put(encryptedSecretKey);

        buffer.flip();
        return buffer.array();
    }

    public static ConnectionServerHelloPacket bytesToPacket(ByteBuffer buffer) {
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

        byte[] keyBytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(keyBytes);

        return new ConnectionServerHelloPacket(username, keyBytes);
    }
}
