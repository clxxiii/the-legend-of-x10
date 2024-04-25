package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.ConnectPacket;
import edu.oswego.cs.Packets.ConnectSubopcode;
import edu.oswego.cs.Packets.HeartbeatPacket;
import edu.oswego.cs.Packets.Opcode;
import edu.oswego.cs.client.Command;
import edu.oswego.cs.game.Action;
import edu.oswego.cs.game.GameStateMachine;

import java.io.IOException;
import java.net.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Raft {
   
   private final Timer heartBeatTimer = new Timer();
   private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
   private final DatagramSocket serverSocket;
   public volatile RaftMembershipState raftMembershipState;
   public volatile boolean raftSessionActive;
   private final ConcurrentLinkedQueue<Action> queue = new ConcurrentLinkedQueue<>();
   private final Lock logLock = new ReentrantLock();
   private final List<Action> log = new ArrayList<>();
   private final GameStateMachine gsm;
   private final AtomicInteger lastActionConfirmed;
   private final AtomicBoolean gameActive = new AtomicBoolean(true);
   private volatile String userNameOfLeader;
   private volatile String clientUserName;
   private final RaftReceiver raftReceiver;
   private final AtomicBoolean keepReceiving = new AtomicBoolean(true);

   public Raft(int serverPort, String clientUserName) throws SocketException {
      serverSocket = new DatagramSocket(serverPort);
      raftSessionActive = false;
      lastActionConfirmed = new AtomicInteger(-1);
      gsm = new GameStateMachine(log, lastActionConfirmed, gameActive, this);
      this.clientUserName = clientUserName;
      raftReceiver = new RaftReceiver(serverSocket, keepReceiving, this, clientUserName);
      raftReceiver.start();
   }

   public void startHeartBeat() {
      TimerTask task = new TimerTask() {
         public void run() {
            Action action = queue.poll();
            int termNum = -1;
            byte[] messageBytes;
            if (action != null) {
               logLock.lock();
               try {
                  log.add(action);
                  termNum = log.size() - 1;
                  confirmAction(termNum);
               } finally {
                  logLock.unlock();
               }

            } else {
//               HeartbeatPacket heartbeatPacket = new HeartbeatPacket(clientUserName);
//               messageBytes = heartbeatPacket.packetToBytes();
            }

            HeartbeatPacket heartbeatPacket = new HeartbeatPacket(clientUserName);
            messageBytes = heartbeatPacket.packetToBytes();



            sessionMap.forEachValue(Long.MAX_VALUE, (value) -> {
               // TODO: send to socket address
               if (value.getMembershipState() == RaftMembershipState.FOLLOWER) {
                  SocketAddress socketAddress = value.getSocketAddress();
                  DatagramPacket datagramPacket = new DatagramPacket(messageBytes, messageBytes.length, socketAddress);
                  try {
                     serverSocket.send(datagramPacket);
                  } catch (IOException e) {
                     System.err.println("An IOException was thrown while trying to send a heartbeat");
                  }
               }
            });
         }
      };
      long periodInMS = 100;
      heartBeatTimer.schedule(task, 0, periodInMS);
   }

   public void stopHeartBeat() {
      heartBeatTimer.cancel();
   }

   public void addSession(String username, Session session) {
      sessionMap.put(username, session);
   }

   public void startRaftGroup() {
      raftMembershipState = RaftMembershipState.LEADER;
      raftSessionActive = true;
      this.userNameOfLeader = clientUserName;
      sessionMap.put(clientUserName, new Session(serverSocket.getLocalSocketAddress(), System.nanoTime(), raftMembershipState));
      startHeartBeat();
      gsm.start();
   }

   public void exitRaft() {
      stopHeartBeat();
      keepReceiving.set(false);
      serverSocket.close();
      gsm.stop();
   }

   public void joinRaftGroup(SocketAddress groupAddress) {
      try {
         raftMembershipState = RaftMembershipState.FOLLOWER;
         ConnectPacket connectPacket = new ConnectPacket(ConnectSubopcode.ClientHello, clientUserName, new byte[0]);
         byte[] connectHelloPacketBytes = connectPacket.packetToBytes();
         DatagramPacket packet = new DatagramPacket(connectHelloPacketBytes, connectHelloPacketBytes.length, groupAddress);
         serverSocket.send(packet);
      } catch (IOException e) {
         System.err.println("Something went wrong when trying to connect.");
         System.exit(1);
      }
   }

   public void confirmAction(int index) {
      while (true) {
         int lastConfirmedIndex = lastActionConfirmed.get();
         if (index > lastConfirmedIndex) {
            lastActionConfirmed.compareAndSet(lastConfirmedIndex, index);
            synchronized (log) {
               log.notify();
            }
         } else {
            break;
         }
      }
   }

   public void sendMessage(String command) {
      // get leader and send message
      if (userNameOfLeader != null) {
         if (clientUserName.equals(userNameOfLeader)) {
            queue.add(new Action(clientUserName, command));
         } else {
            // send message to leader
         }
      }
   }

   public void addToRaftQueue(String command) {
      queue.add(new Action(clientUserName, command));
   }

   public boolean addUser(String username, SocketAddress clientAddress) {
      Session client = sessionMap.putIfAbsent(username, new Session(clientAddress, System.nanoTime(), RaftMembershipState.PENDING_FOLLOWER));
      return client == null;
   }

   public void setLeader(String username, Session session) {
      userNameOfLeader = username;
      sessionMap.put(username, session);
   }

   public boolean userIsPending(String username) {
      Session session = sessionMap.get(username);
      return session != null && session.getMembershipState() == RaftMembershipState.PENDING_FOLLOWER;
   }

   public boolean addrIsLeader(SocketAddress socketAddress) {
      Session leaderSession = sessionMap.get(userNameOfLeader);
      return leaderSession != null && leaderSession.getSocketAddress().toString().equals(socketAddress.toString());
   }

   public void updateSessionTimeStamp(String username, SocketAddress socketAddress) {
      Session session = sessionMap.get(username);
      if (session != null && session.getSocketAddress().toString().equals(socketAddress.toString())) {
         session.setLMRSTINT(System.nanoTime());
      }
   }

   public SocketAddress getLeaderAddr() {
      if (userNameOfLeader != null) {
         Session session = sessionMap.get(userNameOfLeader);
         if (session != null) {
            return session.getSocketAddress();
         }
      }
      return null;
   }
}
