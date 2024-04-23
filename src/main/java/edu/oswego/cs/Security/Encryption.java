package edu.oswego.cs.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.concurrent.ConcurrentHashMap;

public class Encryption {

    public String IP;

    public int keySize = 2048;

    // Referred to here: https://github.com/firatkucuk/diffie-hellman-helloworld/tree/main
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private ConcurrentHashMap<String, PublicKey> keyMap;
    private ConcurrentHashMap<String, byte[]> secretKeyMap;

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setReceivedPublicKey(PublicKey receivedKey, String ip) {
        if(keyMap == null) keyMap = new ConcurrentHashMap<>();
        keyMap.put(ip, receivedKey);
    }

    public void generateSecretKey(String IP) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);

            PublicKey receivedKey = keyMap.get(IP);
            keyAgreement.doPhase(receivedKey, true);

            byte[] secretKey = shortenKey(keyAgreement.generateSecret());

            if(secretKeyMap == null) secretKeyMap = new ConcurrentHashMap<>();
            secretKeyMap.put(IP, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //32 bytes is the max size allowed for AES
    private byte[] shortenKey(byte[] givenKey) {
        try {
            byte[] shortenedKey = new byte[32];
            System.arraycopy(givenKey, 0, shortenedKey, 0, shortenedKey.length);

            return shortenedKey;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void generateKeys() {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
            generator.initialize(keySize);

            KeyPair keyPair = generator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String decryptMessage(byte[] message, String IP) {
        try {
            byte[] secretKey = secretKeyMap.get(IP);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            return new String(cipher.doFinal(message));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] encryptMessage(String message, String IP) {
        try {
            byte[] secretKey = secretKeyMap.get(IP);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            return cipher.doFinal(message.getBytes());
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

        agent1.generateKeys();
        agent2.generateKeys();

        agent1.setReceivedPublicKey(agent2.getPublicKey(), agent2.IP);
        agent2.setReceivedPublicKey(agent1.getPublicKey(), agent1.IP);

        agent1.generateSecretKey(agent2.IP);
        agent2.generateSecretKey(agent1.IP);

        byte[] secretMessage = agent1.encryptMessage("Look at me still talking when there's science to do...", agent2.IP);
        String decrypted = agent2.decryptMessage(secretMessage, agent1.IP);

        System.out.println(decrypted);
    }

}
