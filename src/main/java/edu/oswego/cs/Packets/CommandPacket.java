package edu.oswego.cs.Packets;

public abstract class CommandPacket extends Packet {

    public final CommandSubopcode commandSubopcode;

    public CommandPacket(CommandSubopcode commandSubopcode) {
        super(Opcode.Command);
        this.commandSubopcode = commandSubopcode;
    }
}
