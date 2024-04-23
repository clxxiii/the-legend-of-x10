package edu.oswego.cs.raft;

import edu.oswego.cs.game.Action;
import edu.oswego.cs.game.GameStateMachine;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

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
   private final ConcurrentHashMap<String, SocketAddress> sessionMap = new ConcurrentHashMap<>();
   private final DatagramSocket serverSocket;
   private volatile RaftMembershipState raftMembershipState;
   private volatile boolean raftSessionActive;
   private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
   private final Lock logLock = new ReentrantLock();
   private final List<Action> log = new ArrayList<>();
   private final GameStateMachine gsm;
   private final AtomicInteger lastActionConfirmed;
   private final AtomicBoolean gameActive = new AtomicBoolean(true);

   public Raft(int serverPort) throws SocketException {
      serverSocket = new DatagramSocket(serverPort);
      raftSessionActive = true;
      lastActionConfirmed = new AtomicInteger(-1);
      gsm = new GameStateMachine(log, lastActionConfirmed, gameActive);
      queue.add("do");
      queue.add("the");
      queue.add("thing");
   }

   public void startHeartBeat() {
      TimerTask task = new TimerTask() {
         public void run() {
            System.out.println("heartbeat");
            String command = queue.poll();
            int termNum = -1;
            if (command != null) {
               logLock.lock();
               try {
                  log.add(new Action(command));
                  termNum = log.size() - 1;
                  confirmAction(termNum);
               } finally {
                  logLock.unlock();
               }
            }
            sessionMap.forEachValue(Long.MAX_VALUE, (value) -> {
               // TODO: send to socket address
               if (command != null) {
                  // send command
//                  System.out.println(command);
               } else {
                  // send empty message
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

   public void addSession(String session, SocketAddress socketAddress) {
      sessionMap.put(session, socketAddress);
   }

   public void startRaftGroup() {
      raftMembershipState = RaftMembershipState.LEADER;
      startHeartBeat();
      gsm.start();
   }

   public void exitRaft() {
      stopHeartBeat();
      gsm.stop();
   }

   public void joinRaftGroup(SocketAddress groupAddress) {
      raftMembershipState = RaftMembershipState.FOLLOWER;
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
}
