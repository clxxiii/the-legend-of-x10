package edu.oswego.cs.raft;

import edu.oswego.cs.client.Command;

import java.util.Arrays;
import java.util.Optional;

public enum RaftAdministrationCommand {
    ADD_MEMBER("add_mem"),
    TIME_OUT_MEMBER("time_out"),
    ELECT_LEADER("elect_leader"),
    REMOVE_MEMBER("remove_mem");

    public final String name;

    RaftAdministrationCommand(String name) {
        this.name = name;
    }

    public static Optional<RaftAdministrationCommand> parse(String command) {
        return Arrays.stream(values())
                .filter(commandName-> commandName.name.equals(command)).findFirst();
    }
}
