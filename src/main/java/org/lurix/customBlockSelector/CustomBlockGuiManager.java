package org.lurix.customBlockSelector;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles building and interaction logic for the custom block selection GUI.
 */
public class CustomBlockGuiManager implements Listener {

    public static final String GUI_TITLE = "Custom Block Selector";

    private final JavaPlugin plugin;
    private final NamespacedKey customBlockMarkerKey;
    private final NamespacedKey customModelDataKey;

    // Slot -> CustomModelData mapping for selectable items.
    private final Map<Integer, Integer> selectableModels = new HashMap<>();

    public CustomBlockGuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.customBlockMarkerKey = new NamespacedKey(plugin, "custom_block_item");
        this.customModelDataKey = new NamespacedKey(plugin, "custom_model_data");

        // You can change these IDs to match your resource pack models.
        selectableModels.put(10, 1001);
        selectableModels.put(12, 1002);
        selectableModels.put(14, 1003);
        selectableModels.put(16, 1004);
        selectableModels.put(28, 1005);
        selectableModels.put(30, 1006);
        selectableModels.put(32, 1007);
        selectableModels.put(34, 1008);
    }

    /**
     * Opens the 6-row GUI that lets players choose a custom block item.
     */
    public void openSelectorGui(Player player) {
        Inventory gui = Bukkit.createInventory(player, 54, GUI_TITLE);

        // Fill background with neutral panes to make buttons easy to see.
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // Add selectable custom block icons.
        for (Map.Entry<Integer, Integer> entry : selectableModels.entrySet()) {
            gui.setItem(entry.getKey(), buildGuiButton(entry.getValue()));
        }

        player.openInventory(gui);
    }

    /**
     * Creates a GUI button item using a CustomModelData value.
     */
    private ItemStack buildGuiButton(int customModelData) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§bCustom Block §7(" + customModelData + ")");
            meta.setCustomModelData(customModelData);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Builds the placeable item that players receive after selecting from the GUI.
     */
    private ItemStack buildPlaceableItem(int customModelData) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§aPlace Custom Block");
            meta.setCustomModelData(customModelData);
            meta.getPersistentDataContainer().set(customBlockMarkerKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(customModelDataKey, PersistentDataType.INTEGER, customModelData);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        // Prevent taking/moving GUI items.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta == null || !clickedMeta.hasCustomModelData()) {
            return;
        }

        int selectedModel = clickedMeta.getCustomModelData();

        // Close GUI and give one custom placeable item with same model data.
        player.closeInventory();
        player.getInventory().addItem(buildPlaceableItem(selectedModel));
    }

    public NamespacedKey getCustomBlockMarkerKey() {
        return customBlockMarkerKey;
    }

    public NamespacedKey getCustomModelDataKey() {
        return customModelDataKey;
    }
}
