package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;
import edu.oswego.cs.game.Action;
import edu.oswego.cs.gui.MainFrame;
import edu.oswego.cs.stateMachine.ReplicatedStateMachine;

import java.io.IOException;
import java.net.*;

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
   private final ReplicatedStateMachine rsm;
   private final AtomicInteger lastActionConfirmed;
   private final AtomicBoolean gameActive = new AtomicBoolean(true);
   private volatile String userNameOfLeader;
   private volatile String clientUserName;
   private final RaftReceiver raftReceiver;
   private final AtomicBoolean keepReceiving = new AtomicBoolean(true);
   private final Object logConfirmerObject = new Object();
   private final Object followerLogMaintainerObject = new Object();
   private final AtomicBoolean isFollower;
   private final ConcurrentHashMap<Integer, Action> actionMap = new ConcurrentHashMap<>();
   private final MainFrame mainFrame;

   public Raft(int serverPort, String clientUserName, MainFrame mainFrame) throws SocketException {
      serverSocket = new DatagramSocket(serverPort);
      raftSessionActive = false;
      lastActionConfirmed = new AtomicInteger(-1);
      rsm = new ReplicatedStateMachine(log, lastActionConfirmed, gameActive, this, mainFrame, clientUserName);
      this.clientUserName = clientUserName;
      raftReceiver = new RaftReceiver(serverSocket, keepReceiving, this, clientUserName, logConfirmerObject, actionMap, followerLogMaintainerObject, log);
      isFollower = new AtomicBoolean(true);
      this.mainFrame = mainFrame;
      raftReceiver.start();
   }

   public void startHeartBeat() {
      TimerTask task = new TimerTask() {
         public void run() {
            Action action = queue.poll();
            int termNum = -1;
            byte[] messageBytes;
            Packet packet;
            if (action != null) {
               logLock.lock();
               try {
                  log.add(action);
                  termNum = log.size() - 1;
                  sessionMap.get(userNameOfLeader).setGreatestActionConfirmed(termNum);

               } finally {
                  logLock.unlock();
               }
               packet = new LogCommandPacket(action.getUserName(), termNum, action.getCommand());
               messageBytes = packet.packetToBytes();
            } else {
               packet = new HeartbeatPacket(clientUserName);
               messageBytes = packet.packetToBytes();
            }

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

            // in case a log confirmer notification is missed.
            synchronized (logConfirmerObject) {
               logConfirmerObject.notify();
            }
         }
      };
      long periodInMS = 100;
      heartBeatTimer.schedule(task, 0, periodInMS);
   }

   public void stopHeartBeat() {
      heartBeatTimer.cancel();
   }

   public void addSession(String username, Session session) {
      // only add a user if they don't exist in the map
      Session userSession = sessionMap.putIfAbsent(username, session);
      if (userSession != null && userSession.getMembershipState() == RaftMembershipState.PENDING_FOLLOWER) {
         userSession.setMembershipState(RaftMembershipState.PENDING_FOLLOWER, RaftMembershipState.FOLLOWER);
      }
   }

   public void startRaftGroup() {
      isFollower.set(false);
      raftMembershipState = RaftMembershipState.LEADER;
      raftSessionActive = true;
      this.userNameOfLeader = clientUserName;
      queue.add(new Action(userNameOfLeader, RaftAdministrationCommand.ADD_MEMBER.name + " " + userNameOfLeader + " " + System.nanoTime() + " " + serverSocket.getLocalAddress().toString().replace("/", "")));
      sessionMap.put(clientUserName, new Session(serverSocket.getLocalSocketAddress(), System.nanoTime(), raftMembershipState));
      startHeartBeat();
      (new RaftLogConfirmer(logConfirmerObject, sessionMap, lastActionConfirmed, gameActive, clientUserName, serverSocket, log)).start();
      rsm.start();
   }

   public void exitRaft() {
      stopHeartBeat();
      gameActive.set(false);
      synchronized (logConfirmerObject) {
         logConfirmerObject.notify();
      }
      isFollower.set(false);
      synchronized (followerLogMaintainerObject) {
         followerLogMaintainerObject.notify();
      }
      keepReceiving.set(false);
      serverSocket.close();
      rsm.stop();
   }

   public void joinRaftGroup(SocketAddress groupAddress) {
      try {
         raftMembershipState = RaftMembershipState.FOLLOWER;
         ConnectPacket connectPacket = new ConnectPacket(ConnectSubopcode.ClientHello, clientUserName, new byte[0]);
         byte[] connectHelloPacketBytes = connectPacket.packetToBytes();
         DatagramPacket packet = new DatagramPacket(connectHelloPacketBytes, connectHelloPacketBytes.length, groupAddress);
         rsm.start();
         (new RaftFollowerLogMaintainer(isFollower, logLock, log, lastActionConfirmed, followerLogMaintainerObject, actionMap)).start();
         serverSocket.send(packet);
      } catch (IOException e) {
         System.err.println("Something went wrong when trying to connect.");
         System.exit(1);
      }
   }

   public void commitAction(int index) {
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
            ReqCommandPacket reqCommandPacket = new ReqCommandPacket(clientUserName, command);
            byte[] packetBytes = reqCommandPacket.packetToBytes();
            if (getLeaderAddr() != null) {
               DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, getLeaderAddr());
               try {
                  serverSocket.send(datagramPacket);
               } catch (IOException e) {
                  System.err.println("An IOException was thrown while trying to send a server command request.");
               }
            }
         }
      }
   }

   public void addActionToQueue(Action action) {
      queue.add(action);
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

   public String getClientUserName() {
      return clientUserName;
   }

   public int getLastActionConfirmed() {
      return lastActionConfirmed.get();
   }

   public int getLogLength() {
      return log.size();
   }

   public Action getLogIndex(int i) {
      return log.get(i);
   }

   public void updateRaftFollowerGreatestConfirmedAction(String username, int actionNum) {
      Session session = sessionMap.get(username);
      if (session != null && session.getMembershipState() == RaftMembershipState.FOLLOWER) {
         session.setGreatestActionConfirmed(actionNum);
      }
   }

   // A method used for the worst case, log notification missed
   public void notifyLog() {
      synchronized (log) {
         log.notify();
      }
   }

   public int getLogPosition() {
      return log.size() - 1;
   }
}
