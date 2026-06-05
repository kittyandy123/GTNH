package dev.gtnhplanner.gtnhcalculatorutility.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.Loader;
import dev.gtnhplanner.gtnhcalculatorutility.export.ExportResult;
import dev.gtnhplanner.gtnhcalculatorutility.export.RecipeExporter;
import dev.gtnhplanner.gtnhcalculatorutility.gregtech.GregTechRecipeMapSummary;

public class CommandGTNHCalc extends CommandBase {

    @Override
    public String getCommandName() {
        return "gtnhcalc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtnhcalc hello|export|gt-summary";
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

        if (args.length == 1 && "gt-summary".equalsIgnoreCase(args[0])) {
            summarizeGregTech(sender);
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
            ExportResult result = new RecipeExporter().exportRecipes(minecraftDir);
            sendExportSummary(sender, result);
        } catch (IOException e) {
            sender.addChatMessage(new ChatComponentText("Failed to export recipes: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendExportSummary(ICommandSender sender, ExportResult result) {
        sender.addChatMessage(new ChatComponentText("Exported " + result.totalRecipes + " recipes to:"));
        sender.addChatMessage(new ChatComponentText(result.outputFile.getAbsolutePath()));
        sender.addChatMessage(new ChatComponentText("Recipe counts:"));

        for (Map.Entry<String, Integer> entry : result.recipeCountByMachine.entrySet()) {
            sender.addChatMessage(new ChatComponentText("- " + entry.getKey() + ": " + entry.getValue()));
        }
    }

    private void summarizeGregTech(ICommandSender sender) {
        List<String> lines = new GregTechRecipeMapSummary().summarizeRecipeMaps();

        sender.addChatMessage(new ChatComponentText("GregTech RecipeMaps found: " + lines.size()));

        int shown = 0;
        for (String line : lines) {
            sender.addChatMessage(new ChatComponentText("- " + line));
            shown++;

            if (shown >= 20) {
                sender.addChatMessage(new ChatComponentText("...truncated after 20 maps"));
                break;
            }
        }
    }
}
