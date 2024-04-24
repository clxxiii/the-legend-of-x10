package edu.oswego.cs.Packets;

public abstract class CommandPacket extends Packet {

    public final CommandSubopcode commandSubopcode;
    public final String username;

    public CommandPacket(CommandSubopcode commandSubopcode, String username) {
        super(Opcode.Command);
        this.commandSubopcode = commandSubopcode;
        this.username = username;
    }
}
