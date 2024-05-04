package edu.oswego.cs.raft;

import edu.oswego.cs.Packets.*;
import edu.oswego.cs.Security.Encryption;
import edu.oswego.cs.game.Action;
import edu.oswego.cs.gui.MainFrame;
import edu.oswego.cs.stateMachine.ReplicatedStateMachine;

import java.io.IOException;
import java.net.*;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Raft {
   
   private Timer heartBeatTimer = new Timer();
   private Timer timeoutTimer = new Timer();
   private Timer electionTimeoutTimer = new Timer();
   private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
   private final DatagramSocket serverSocket;
   public final AtomicReference<RaftMembershipState> raftMembershipState = new AtomicReference<>();
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
   private final ConcurrentHashMap<Integer, Action> actionMap = new ConcurrentHashMap<>();
   private final Encryption encryption = new Encryption();
   private final AtomicInteger termCounter = new AtomicInteger(0);
   private final AtomicBoolean voted = new AtomicBoolean(false);
   private final AtomicInteger voteCounter = new AtomicInteger(0);
   private final HashSet<String> voteSet = new HashSet<>();
   private final AtomicInteger clientCount = new AtomicInteger();

   /**
    * Creates a raft server instance that hasn't been started yet.
    * @param serverPort The designated port for sending/receiving messages.
    * @param clientUserName The username of the user who will be connected to this raft instance.
    * @throws SocketException
    */
   public Raft(int serverPort, String clientUserName) throws SocketException {
      serverSocket = new DatagramSocket(serverPort);
      encryption.generateKeys();
      raftSessionActive = false;
      lastActionConfirmed = new AtomicInteger(-1);
      MainFrame mainFrame = new MainFrame();
      mainFrame.setRaft(this);
      rsm = new ReplicatedStateMachine(log, lastActionConfirmed, gameActive, this, mainFrame, clientUserName);
      this.clientUserName = clientUserName;
      raftReceiver = new RaftReceiver(serverSocket, keepReceiving, this, clientUserName, logConfirmerObject, actionMap, followerLogMaintainerObject, log, encryption);
      raftReceiver.start();
   }

   /**
    * Starts up a Leader's Heartbeat that sends either heartbeat packets or command packets (if commands are queued up) every 10ms.
    */
   public void startHeartBeat() {
      heartBeatTimer = new Timer();
      TimerTask task = new TimerTask() {
         public void run() {
            Action action = queue.poll();
            int index= -1;
            byte[] messageBytes;
            Packet packet;
            if (action != null) {
               logLock.lock();
               try {
                  log.add(action);
                  index = log.size() - 1;
                  sessionMap.get(userNameOfLeader).setGreatestActionConfirmed(index);

               } finally {
                  logLock.unlock();
               }
               packet = new LogCommandPacket(clientUserName, index, termCounter.get(), action.getUserName(), action.getCommand());
               messageBytes = packet.packetToBytes();
            } else {
               packet = new HeartbeatPacket(clientUserName, lastActionConfirmed.get(), termCounter.get());
               messageBytes = packet.packetToBytes();
            }

            sessionMap.forEachValue(Long.MAX_VALUE, (value) -> {
               // send to socket address
               if (value.getMembershipState() == RaftMembershipState.FOLLOWER && !value.getTimedOut()) {
                  SocketAddress socketAddress = value.getSocketAddress();
                  sendPacket(messageBytes, socketAddress);
               }
            });

            // in case a log confirmer notification is missed.
            synchronized (logConfirmerObject) {
               logConfirmerObject.notify();
            }
         }
      };
      long periodInMS = 10;
      heartBeatTimer.schedule(task, 0, periodInMS);
   }

   /**
    * Stops the heartbeat timer.
    */
   public void stopHeartBeat() {
      heartBeatTimer.cancel();
   }

   /**
    * Starts a follower timeout for the leader. The timeout is checked every 300ms with the timeout threshold being 200ms.
    */
   public void startTimeoutTimer() {
      timeoutTimer = new Timer();
      TimerTask task = new TimerTask() {
         @Override
         public void run() {
            sessionMap.forEach(1, (key, value) -> {
               if (value.getMembershipState() == RaftMembershipState.FOLLOWER && !value.getTimedOut()) {
                  long nanoTime = System.nanoTime();
                  long disconnectThreshold = 200_000_000L;
                  long timeDifference = nanoTime - value.getLMRSTINT();
                  if (timeDifference > disconnectThreshold) {
                     value.setTimedOut(true);
                     queue.add(new Action(clientUserName, RaftAdministrationCommand.TIME_OUT_MEMBER.name + " " + key));
                  }
               }
            });
         }
      };
      long periodInMS = 300L;
      timeoutTimer.schedule(task, periodInMS, periodInMS);
   }

   /**
    * Stops Leader's follower timeout.
    */
   public void stopTimeout() {
      timeoutTimer.cancel();
   }

   /**
    * Adds a new session to the sessionMap if absent or updates an existing pending follower to a follower.
    * @param username The username of who was added to the raft session.
    * @param session The session associated with the user to be added.
    */
   public void addSession(String username, Session session) {
      // only add a user if they don't exist in the map
      Session userSession = sessionMap.putIfAbsent(username, session);
      if (userSession != null && userSession.getMembershipState() == RaftMembershipState.PENDING_FOLLOWER) {
         userSession.setLMRSTINT(System.nanoTime());
         userSession.setMembershipState(RaftMembershipState.PENDING_FOLLOWER, RaftMembershipState.FOLLOWER);
      }
      clientCount.incrementAndGet();
   }

   /**
    * Starts a raft group and promotes the raft instance to a leader. Actions for seed generation and user addition (adding its own instance) are added to the queue.
    */
   public void startRaftGroup() {
      encryption.generateSecretKey();
      raftMembershipState.set(RaftMembershipState.LEADER);
      raftSessionActive = true;
      this.userNameOfLeader = clientUserName;
      long seed = new Random().nextLong();
      queue.add(new Action(userNameOfLeader, RaftAdministrationCommand.SEED_DUNGEON.name + " " + seed));
      queue.add(new Action(userNameOfLeader, RaftAdministrationCommand.ADD_MEMBER.name + " " + userNameOfLeader + " " + System.nanoTime() + " " + serverSocket.getLocalSocketAddress().toString().replace("/", "")));
      sessionMap.put(clientUserName, new Session(serverSocket.getLocalSocketAddress(), System.nanoTime(), raftMembershipState.get()));
      startHeartBeat();
      startTimeoutTimer();
      (new RaftLogConfirmer(logConfirmerObject, sessionMap, lastActionConfirmed, gameActive, clientUserName, serverSocket, log)).start();
      rsm.start();
   }

   /**
    * Ensures all timers and threads are gracefully stopped before the program exits.
    */
   public void exitRaft() {
      stopHeartBeat();
      stopTimeout();
      gameActive.set(false);
      synchronized (logConfirmerObject) {
         logConfirmerObject.notify();
      }
      synchronized (followerLogMaintainerObject) {
         followerLogMaintainerObject.notify();
      }
      keepReceiving.set(false);
      stopElectionTimeout();
      serverSocket.close();
      rsm.stop();
   }

   /**
    * Joins a raft group and initializes the raft member to a follower.
    * @param groupAddress The target address of the group (can be a follower's address)
    */
   public void joinRaftGroup(SocketAddress groupAddress) {
      try {
         raftMembershipState.set(RaftMembershipState.FOLLOWER);
         ConnectionClientHelloPacket clientHelloPacket = new ConnectionClientHelloPacket(clientUserName, encryption.getPublicKey());
         byte[] connectHelloPacketBytes = clientHelloPacket.packetToBytes();
         DatagramPacket packet = new DatagramPacket(connectHelloPacketBytes, connectHelloPacketBytes.length, groupAddress);
         rsm.start();
         (new RaftFollowerLogMaintainer(raftMembershipState, logLock, log, lastActionConfirmed, followerLogMaintainerObject, actionMap)).start();
         serverSocket.send(packet);
      } catch (IOException e) {
         System.err.println("Something went wrong when trying to connect.");
         System.exit(1);
      }
   }

   /**
    * Commits an action in the log allowing it to be executed.
    * @param index The log index of the action to be committed.
    */
   public void commitAction(int index) {
      while (true) {
         int lastConfirmedIndex = lastActionConfirmed.get();
         int logSize = log.size();
         if (index > lastConfirmedIndex) {
            if (logSize > index) {
               lastActionConfirmed.compareAndSet(lastConfirmedIndex, index);
               synchronized (log) {
                  log.notify();
               }
            } else if (logSize < index) {
               lastActionConfirmed.compareAndSet(lastConfirmedIndex, logSize - 1);
               synchronized (log) {
                  log.notify();
               }
               break;
            }
         } else {
            break;
         }
      }
   }

   /**
    * How commands/messages to the log are submitted. This method checks whether the raft instance is the leader or a follower and passes the command accordingly.
    * @param command the string representation of a log command.
    */
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
               sendPacket(packetBytes, getLeaderAddr());
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
      if (client != null) {
         client.setTimedOut(false);
      }
      return true;
   }

   /**
    * Reconnects the user (if previously disconnected)
    * @param username The username of the previously disconnected user
    * @return a boolean depicting whether a session actually existed corresponding to the user.
    */
   public boolean reconnectUser(String username) {
      Session session  = sessionMap.get(username);
      if (username.equals(clientUserName)) return true;
      clientCount.incrementAndGet();
      if (session != null) {
         if (session.getMembershipState() == RaftMembershipState.DISCONNECTED) {
            session.setMembershipState(RaftMembershipState.DISCONNECTED, RaftMembershipState.FOLLOWER);
            session.setLMRSTINT(System.nanoTime());
            session.setTimedOut(false);
         }
      }
      return session != null;
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
      if (session != null ) {
         session.setLMRSTINT(System.nanoTime());
         // The user is actually still here.
         if (session.getTimedOut()) {
            session.setTimedOut(false);
         }
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

   /**
    * Updates a followers greatest confirmed action. This method enables the leader to know when to commit log indices.
    * @param username
    * @param actionNum
    */
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

   /**
    * Encrypts and sends a message.
    * @param bytes packet bytes to be sent.
    * @param socketAddress the target address.
    */
   public void sendPacket(byte[] bytes, SocketAddress socketAddress) {
      byte[] encryptedBytes = encryption.encryptMessageWithSecretKey(bytes);
      if (encryptedBytes != null) {
         try {
            serverSocket.send(new DatagramPacket(encryptedBytes, encryptedBytes.length, socketAddress));
         } catch (IOException e) {
            System.err.println("An IOException is thrown when trying to send a heartbeat.");
         }
      }
   }

   /**
    * Starts the election timer which checks between every 150 ms and 350 ms for a leader timeout. Timeout rng is used to avoid
    * ties.
    */
   public void startElectionTimeout() {
      electionTimeoutTimer = new Timer();
      long timeOut = (new Random()).longs(300_000_000L, 500_000_000L).findFirst().getAsLong();
      TimerTask task = new TimerTask() {
         @Override
         public void run() {
            boolean runElection = true;
            if (userNameOfLeader != null) {
               Session leaderSession = sessionMap.get(userNameOfLeader);
               if (leaderSession != null) {
                  long timeStamp = leaderSession.getLMRSTINT();
                  long timeDiff = System.nanoTime() - timeStamp;
                  if (timeDiff < timeOut) {
                     runElection = false;
                  }
               }
            }
            if (runElection) {
               runElection();
            }
         }
      };
      long periodInMS = new Random().longs(150, 350).findFirst().getAsLong();
      electionTimeoutTimer.schedule(task, periodInMS, periodInMS);
   }

   public void stopElectionTimeout() {
      electionTimeoutTimer.cancel();
   }

   /**
    * Upgrades a follower to the candidate status. Removes follower functionality and causes the raft instance to enter a candidate mode.
    */
   public void convertToCandidate() {
      raftMembershipState.set(RaftMembershipState.CANDIDATE);
      voted.set(false);
      termCounter.incrementAndGet();
      sessionMap.get(clientUserName).setMembershipState(RaftMembershipState.FOLLOWER, RaftMembershipState.CANDIDATE);
      if (userNameOfLeader != null && raftSessionActive && !userNameOfLeader.equals(clientUserName)) {
         sessionMap.get(userNameOfLeader).setMembershipState(RaftMembershipState.LEADER, RaftMembershipState.DISCONNECTED);
         queue.add(new Action(clientUserName, RaftAdministrationCommand.TIME_OUT_MEMBER.name + " " + userNameOfLeader));
         userNameOfLeader = null;
         clientCount.decrementAndGet();
      }
      synchronized (followerLogMaintainerObject) {
         followerLogMaintainerObject.notify();
      }
   }

   /**
    * Converts a candidate raft instance to leader raft instance. A heartbeat and client timeout is started.
    */
   public void convertToLeader() {
      raftMembershipState.set(RaftMembershipState.LEADER);
      sessionMap.get(clientUserName).setMembershipState(RaftMembershipState.CANDIDATE, RaftMembershipState.LEADER);
      this.userNameOfLeader = clientUserName;
      startHeartBeat();
      startTimeoutTimer();
      (new RaftLogConfirmer(logConfirmerObject, sessionMap, lastActionConfirmed, gameActive, clientUserName, serverSocket, log)).start();
   }

   /**
    * Converts a candidate back to a follower. Accounts for cases where multiple candidates existed simultaneously.
    */
   public void convertToFollower() {
      if (raftMembershipState.get() == RaftMembershipState.CANDIDATE) {
         raftMembershipState.set(RaftMembershipState.FOLLOWER);
         sessionMap.get(clientUserName).setMembershipState(RaftMembershipState.CANDIDATE, RaftMembershipState.FOLLOWER);
         (new RaftFollowerLogMaintainer(raftMembershipState, logLock, log, lastActionConfirmed, followerLogMaintainerObject, actionMap)).start();
      }
   }

   /**
    * Sends out candidate packets to all of the followers.
    */
   public void sendOutCandidatePackets() {
      CandidatePacket candidatePacket = new CandidatePacket(clientUserName, termCounter.get(), getLogPosition());
      byte[] packetBytes = candidatePacket.packetToBytes();
      sessionMap.forEachValue(1, (value) -> {
         if (value.getMembershipState() == RaftMembershipState.FOLLOWER) {
            sendPacket(packetBytes, value.getSocketAddress());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put(packetBytes);
            try {
               Packet packet = Packet.bytesToPacket(buffer);
            } catch (ParseException e) {
               System.out.println("Parse exception thrown");
            } catch (NullPointerException e) {
               System.out.println("null pointer");
            }
         }
      });
   }

   /**
    * Runs an election where a Follower is converted to a candidate and a vote commences. Upon election failure another election is started with new rng.
    */
   public void runElection() {
      convertToCandidate();
      stopElectionTimeout();
      if (!voted.get()) {
         voted.set(true);
         voteCounter.set(1);
         if (clientCount.get() == voteCounter.get()) {
            convertToLeader();
            return;
         }
         sendOutCandidatePackets();
         if (raftMembershipState.get() == RaftMembershipState.CANDIDATE) {
            startElectionTimeout();
         }
      }
   }

   public int getTermNum() {
      return termCounter.get();
   }

   public boolean setTermNum(int termNum) {
      while (termNum > termCounter.get()) {
         int expectedTermNum = termCounter.get();
         if (expectedTermNum < termNum) {
            boolean success = termCounter.compareAndSet(expectedTermNum, termNum);
            if (success) return true;
         } else {
            return false;
         }
      }
      return false;
   }

   public void demoteLeader() {
      if (userNameOfLeader != null) {
         Session session = sessionMap.get(userNameOfLeader);
         if (session != null) {
            session.setMembershipState(RaftMembershipState.LEADER, RaftMembershipState.DISCONNECTED);
         }
      }
   }

   public void resetVote() {
      resetVotes();
      voted.set(false);
   }

   public synchronized void addVote(String username, int termNum) {
      if (raftMembershipState.get() == RaftMembershipState.CANDIDATE && termNum == termCounter.get()) {
         int oldSize = voteSet.size();
         voteSet.add(username);
         if (voteSet.size() > oldSize) {
            voteCounter.incrementAndGet();
         }
         if (voteCounter.get() > clientCount.get() / 2) {
            convertToLeader();
         }
      }
   }

   public void resetVotes() {
      voteSet.clear();
   }

   public void timeOutUser(String username) {
      Session session = sessionMap.get(username);
      if (username.equals(clientUserName)) {
         return;
      }
      if (session != null) {
         session.setTimedOut(true);
         if (session.getMembershipState() == RaftMembershipState.FOLLOWER) {
            session.setMembershipState(RaftMembershipState.FOLLOWER, RaftMembershipState.DISCONNECTED);
            clientCount.decrementAndGet();
         }
      }
   }

   public boolean userIsReconnecting(String username) {
      Session session = sessionMap.get(username);
      if (session != null) {
          return session.getMembershipState() == RaftMembershipState.DISCONNECTED && !session.getTimedOut();
      }
      return false;
   }
}
