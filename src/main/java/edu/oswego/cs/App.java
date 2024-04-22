package edu.oswego.cs;

import edu.oswego.cs.Shared.Encryption;

import java.security.Security;

public class App {

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
