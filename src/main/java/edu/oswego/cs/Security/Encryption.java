package edu.oswego.cs.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Encryption {

    public String IP;

    public int rsaKeySize = 2048;
    public int keySize = 256;
    public int ivSize = 16;

    // Referred to here: https://github.com/firatkucuk/diffie-hellman-helloworld/tree/main
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private ConcurrentHashMap<String, PublicKey> keyMap;
    private SecretKey secretKey;

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setReceivedPublicKey(PublicKey receivedKey, String ip) {
        if(keyMap == null) keyMap = new ConcurrentHashMap<>();
        keyMap.put(ip, receivedKey);
    }

    public void generateSecretKey() {
        try {

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize);
            secretKey = keyGen.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateKeys() {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(rsaKeySize);

            KeyPair keyPair = generator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Referenced Kirill's answer here: https://stackoverflow.com/questions/29575024/is-there-any-difference-if-i-init-aes-cipher-with-and-without-ivparameterspec
    public byte[] encryptMessageWithSecretKey(byte[] message)  {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

//            byte[] paramSpec = new byte[ivSize];
//            new IvParameterSpec(paramSpec);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new SecureRandom());
            byte[] encryptedMessage = cipher.doFinal(message);

            //This should end up having a length of 16.
            byte[] generatedIv = cipher.getIV();

            byte[] payload = new byte[generatedIv.length + encryptedMessage.length];
            System.arraycopy(generatedIv, 0, payload, 0, generatedIv.length);
            System.arraycopy(encryptedMessage, 0, payload, generatedIv.length, encryptedMessage.length);

            return payload;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] decryptMessageWithSecretKey(byte[] encryptedMessage) {
        try {
            //Should be 16
            byte[] receivedIv = new byte[16];
            byte[] encryptedData = new byte[encryptedMessage.length - receivedIv.length];

            System.arraycopy(encryptedMessage, 0, receivedIv, 0, receivedIv.length);
            System.arraycopy(encryptedMessage, receivedIv.length, encryptedData, 0, encryptedData.length);

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(receivedIv));

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String decryptMessage(byte[] message, String IP) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            return new String(cipher.doFinal(message));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] encryptMessageWithPublicKey(byte[] message, String IP) {
        try {
            PublicKey publicKey = keyMap.get(IP);
            //SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "RSA");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return cipher.doFinal(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decryptMessageWithPrivateKey(byte[] message) {
        try {
            //PublicKey publicKey = keyMap.get(IP);
            //SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "RSA");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);

            return cipher.doFinal(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main( String[] args) throws Exception {
        Security.setProperty("crypto.policy", "unlimited");

        final Encryption agent1 = new Encryption();
        agent1.IP = "196.168.0.1";
        final Encryption agent2 = new Encryption();
        agent2.IP = "192.168.0.2";

        //Both generate their own sets of RSA keys
        agent1.generateKeys();
        agent2.generateKeys();

        //Agent 1 generates the AES secret key
        agent1.generateSecretKey();

        //Agent 1 gets Agent 2's public key
        agent1.setReceivedPublicKey(agent2.publicKey, agent2.IP);

        //Agent 1 encrypts the secret key using Agent 2's public key
        byte[] encryptedMessage = agent1.encryptMessageWithPublicKey(agent1.secretKey.getEncoded(), agent2.IP);

        //Agent 2 decrypts the secret key using its private key
        byte[] receivedSecret = agent2.decryptMessageWithPrivateKey(encryptedMessage);

        int isSame = Arrays.compare(receivedSecret, agent1.secretKey.getEncoded());

        System.out.println("Received secret: " + Arrays.toString(receivedSecret));
        System.out.println("Original secret: " + Arrays.toString(agent1.secretKey.getEncoded()));

        agent2.secretKey = new SecretKeySpec(receivedSecret, 0, receivedSecret.length, "AES");

        String payload = "Look at me still talking when there's science to do...";
        System.out.println("Payload: " + payload);
        System.out.println("Payload bytes: " + Arrays.toString(payload.getBytes()));
        byte[] encryptedString = agent2.encryptMessageWithSecretKey(payload.getBytes());
        System.out.println("Encrypted string: " + Arrays.toString(encryptedString));

        byte[] decryptedBytes = agent1.decryptMessageWithSecretKey(encryptedString);
        System.out.println("Decrypted bytes: " + Arrays.toString(decryptedBytes));
        System.out.println("Decrypted string: " + new String(decryptedBytes));
    }

}
