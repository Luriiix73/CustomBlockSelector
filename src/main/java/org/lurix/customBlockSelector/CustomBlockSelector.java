package org.lurix.customBlockSelector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point.
 */
public class CustomBlockSelector extends JavaPlugin {

    private CustomBlockGuiManager guiManager;
    private CustomBlockPlacementListener placementListener;

    @Override
    public void onEnable() {
        // Create managers/listeners once when the plugin starts.
        this.guiManager = new CustomBlockGuiManager(this);
        this.placementListener = new CustomBlockPlacementListener(this, guiManager);

        // Register listener for GUI and placement actions.
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(placementListener, this);

        // Register the command executor.
        PluginCommand command = getCommand("customblocks");
        if (command != null) {
            command.setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        // Clean up spawned entities when the plugin is disabled/reloaded.
        if (placementListener != null) {
            placementListener.removeAllPlacedBlocks();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        guiManager.openSelectorGui(player);
        return true;
    }
}
