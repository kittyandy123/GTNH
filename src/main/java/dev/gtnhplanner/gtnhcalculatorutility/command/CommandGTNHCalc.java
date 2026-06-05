package dev.gtnhplanner.gtnhcalculatorutility.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.Loader;
import dev.gtnhplanner.gtnhcalculatorutility.export.RecipeExporter;

public class CommandGTNHCalc extends CommandBase {

    @Override
    public String getCommandName() {
        return "gtnhcalc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtnhcalc hello|export";
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

        if (args.length == 1 && "export".equalsIgnoreCase(args[0])) {
            exportRecipes(sender);
            return;
        }

        sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    private void exportRecipes(ICommandSender sender) {
        try {
            File minecraftDir = Loader.instance()
                .getConfigDir()
                .getParentFile();
            File outputFile = new RecipeExporter().exportRecipes(minecraftDir);

            sender.addChatMessage(new ChatComponentText("Exported test recipe to " + outputFile.getAbsolutePath()));
        } catch (IOException e) {
            sender.addChatMessage(new ChatComponentText("Failed to export recipes: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
