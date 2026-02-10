package org.lurix.customBlockSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles placing and removing custom block entities.
 */
public class CustomBlockPlacementListener implements Listener {

    private final JavaPlugin plugin;
    private final CustomBlockGuiManager guiManager;

    // display UUID -> data
    private final Map<UUID, PlacedCustomBlock> placedBlocksByDisplay = new HashMap<>();
    // interaction UUID -> display UUID
    private final Map<UUID, UUID> interactionToDisplay = new HashMap<>();

    public CustomBlockPlacementListener(JavaPlugin plugin, CustomBlockGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main hand to avoid double processing.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!event.getAction().isRightClick()) {
            return;
        }

        if (event.getClickedBlock() == null || event.getBlockFace() == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (!isPlaceableCustomBlockItem(held)) {
            return;
        }

        // We are placing a custom entity block, so cancel normal block interaction.
        event.setCancelled(true);

        Integer customModelData = getCustomModelDataFromItem(held);
        if (customModelData == null) {
            return;
        }

        Location placeAt = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.0, 0.5);
        placeCustomBlock(placeAt, player, customModelData);

        // Consume one item in survival/adventure mode.
        if (player.getGameMode().name().equals("SURVIVAL") || player.getGameMode().name().equals("ADVENTURE")) {
            held.setAmount(held.getAmount() - 1);
        }
    }

    @EventHandler
    public void onRightClickEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        UUID displayIdToRemove = null;

        if (placedBlocksByDisplay.containsKey(clickedEntity.getUniqueId())) {
            displayIdToRemove = clickedEntity.getUniqueId();
        } else if (interactionToDisplay.containsKey(clickedEntity.getUniqueId())) {
            displayIdToRemove = interactionToDisplay.get(clickedEntity.getUniqueId());
        }

        if (displayIdToRemove == null) {
            return;
        }

        // Remove only when player is sneaking + right-clicking the custom block.
        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        removePlacedBlock(displayIdToRemove);
    }

    private boolean isPlaceableCustomBlockItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte marker = pdc.get(guiManager.getCustomBlockMarkerKey(), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private Integer getCustomModelDataFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        Integer fromPdc = meta.getPersistentDataContainer().get(guiManager.getCustomModelDataKey(), PersistentDataType.INTEGER);
        if (fromPdc != null) {
            return fromPdc;
        }

        // Fallback to direct ItemMeta value if needed.
        return meta.hasCustomModelData() ? meta.getCustomModelData() : null;
    }

    private void placeCustomBlock(Location location, Player player, int customModelData) {
        // Visual entity: BlockDisplay (modern replacement for armor-stand based displays).
        BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.BARRIER.createBlockData());
            spawned.setRotation(yawToCardinal(player.getYaw()), 0f);
        });

        // Interaction entity gives us an easy "block-like" hitbox for right-click removal.
        Interaction interaction = location.getWorld().spawn(location.clone().add(0.0, 0.5, 0.0), Interaction.class, spawned -> {
            spawned.setInteractionWidth(1.0f);
            spawned.setInteractionHeight(1.0f);
            spawned.setResponsive(true);
        });

        PlacedCustomBlock placed = new PlacedCustomBlock(
                display.getUniqueId(),
                interaction.getUniqueId(),
                location,
                customModelData
        );

        placedBlocksByDisplay.put(display.getUniqueId(), placed);
        interactionToDisplay.put(interaction.getUniqueId(), display.getUniqueId());
    }

    private float yawToCardinal(float yaw) {
        // Convert yaw into four basic 90° steps for block-like rotation.
        float normalized = (yaw % 360 + 360) % 360;
        int quadrant = Math.round(normalized / 90f) % 4;
        return quadrant * 90f;
    }

    private void removePlacedBlock(UUID displayId) {
        PlacedCustomBlock placed = placedBlocksByDisplay.remove(displayId);
        if (placed == null) {
            return;
        }

        Entity displayEntity = plugin.getServer().getEntity(placed.displayUuid());
        if (displayEntity != null && displayEntity.isValid()) {
            displayEntity.remove();
        }

        Entity interactionEntity = plugin.getServer().getEntity(placed.interactionUuid());
        if (interactionEntity != null && interactionEntity.isValid()) {
            interactionEntity.remove();
        }

        interactionToDisplay.remove(placed.interactionUuid());
    }

    /**
     * Removes all active placed blocks. Used on plugin disable.
     */
    public void removeAllPlacedBlocks() {
        for (UUID displayId : placedBlocksByDisplay.keySet().toArray(new UUID[0])) {
            removePlacedBlock(displayId);
        }
    }

    /**
     * Simple in-memory record for one placed custom block.
     */
    private record PlacedCustomBlock(UUID displayUuid, UUID interactionUuid, Location location, int customModelData) {
    }
}
