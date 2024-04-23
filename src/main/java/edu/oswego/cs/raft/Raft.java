package edu.oswego.cs.raft;

import java.net.SocketAddress;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Raft {
   
   private final Timer heartBeatTimer = new Timer();
   private final ConcurrentHashMap<String, SocketAddress> sessionMap = new ConcurrentHashMap<>();

   public void startHeartBeat() {
      TimerTask task = new TimerTask() {
         public void run() {
            System.out.println("heartbeat");
            sessionMap.forEachValue(Long.MAX_VALUE, (value) -> {
               // send to socket address
               System.out.println(value);
            });
         }
      };

      heartBeatTimer.schedule(task, 0, 100);
   }

   public void stopHeartBeat() {
      heartBeatTimer.cancel();
   }

   public void addSession(String session, SocketAddress socketAddress) {
      sessionMap.put(session, socketAddress);
   }
}
