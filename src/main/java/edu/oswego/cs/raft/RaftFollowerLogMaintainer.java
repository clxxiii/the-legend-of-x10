package edu.oswego.cs.raft;

import edu.oswego.cs.game.Action;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

public class RaftFollowerLogMaintainer extends Thread {

    private final AtomicBoolean isFollower;
    private final Lock logLock;
    private final AtomicInteger lastActionConfirmed;
    private final List<Action> log;
    private final Object followerLogMaintainerObject;
    private final ConcurrentHashMap<Integer, Action> actionMap;

    public RaftFollowerLogMaintainer(AtomicBoolean isFollower, Lock logLock, List<Action> log, AtomicInteger lastActionConfirmed, Object followerLogMaintainerObject, ConcurrentHashMap<Integer, Action> actionMap) {
        this.isFollower = isFollower;
        this.logLock = logLock;
        this.lastActionConfirmed = lastActionConfirmed;
        this.followerLogMaintainerObject = followerLogMaintainerObject;
        this.log = log;
        this.actionMap = actionMap;
    }

    @Override
    public void run() {
        try {
            while (isFollower.get()) {
                boolean madeAddition = true;
                while (log.size() <= lastActionConfirmed.get() && madeAddition) {
                    // make addition to log and keep going while log pieces exist
                    Action action = actionMap.get(log.size());
                    if (action != null) {
                        log.add(action);
                        synchronized (log) {
                            log.notify();
                        }
                    } else {
                        madeAddition = false;
                    }
                }
                synchronized (followerLogMaintainerObject) {
                    followerLogMaintainerObject.wait();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("An Interrupted Exception was thrown while trying to wait for a new action to be added in the Raft Follower Log Maintainer.");
        }
    }
}
