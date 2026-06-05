package dev.gtnhplanner.gtnhcalculatorutility.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandGTNHCalc extends CommandBase {

    @Override
    public String getCommandName() {
        return "gtnhcalc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtnhcalc hello";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("gtnhcalculator", "gtnhcu");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && "hello".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText("Hello from GTNH Calculator Utility!"));
            return;
        }
        sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
