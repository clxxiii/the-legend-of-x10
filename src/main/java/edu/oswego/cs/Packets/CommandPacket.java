package edu.oswego.cs.Packets;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Optional;

public abstract class CommandPacket extends Packet {

    public final CommandSubopcode commandSubopcode;

    public CommandPacket(CommandSubopcode commandSubopcode, String username) {
        super(username, Opcode.Command);
        this.commandSubopcode = commandSubopcode;
    }

    public static CommandPacket bytesToPacket(ByteBuffer buffer) throws ParseException {
        //Get the opcode short from the packet
        short code = buffer.getShort();

        Optional<CommandSubopcode> subopcodeOptional = CommandSubopcode.fromCode(code);

        if (!subopcodeOptional.isPresent()) {
            throw new ParseException("Invalid opcode", 0);
        }

        CommandSubopcode subopcode = subopcodeOptional.get();

        switch (subopcode) {
            case RequestCommand:
                return ReqCommandPacket.bytesToPacket(buffer);
            case LogCommand:
                return LogCommandPacket.bytesToPacket(buffer);
            case ConfirmCommand:
                return ConfirmCommandPacket.bytesToPacket(buffer);
            case CommitCommand:
                return CommitCommandPacket.bytesToPacket(buffer);
            default:
                return null;
        }
    }
}
