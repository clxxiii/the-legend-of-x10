package edu.oswego.cs.raft;

public enum RaftMembershipState {
   PENDING_FOLLOWER,
   FOLLOWER,
   CANDIDATE,
   LEADER,
   SHUTDOWN
}
