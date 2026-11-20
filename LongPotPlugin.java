package cz.mteus.longpot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LongPotPlugin extends JavaPlugin implements Listener {
    private static final int EIGHT_MINUTES = 8 * 60 * 20;
    private record Option(String key, String name, String effectKey, String potionName) {}
    private static final List<Option> OPTIONS = List.of(
        new Option("SPEED", "Speed", "speed", "SWIFTNESS"),
        new Option("STRENGTH", "Strength", "strength", "STRENGTH"),
        new Option("FIRE_RESISTANCE", "Fire Resistance", "fire_resistance", "FIRE_RESISTANCE"),
        new Option("REGENERATION", "Regeneration", "regeneration", "REGENERATION"),
        new Option("JUMP_BOOST", "Jump Boost", "jump_boost", "LEAPING"),
        new Option("NIGHT_VISION", "Night Vision", "night_vision", "NIGHT_VISION"),
        new Option("INVISIBILITY", "Invisibility", "invisibility", "INVISIBILITY"),
        new Option("WATER_BREATHING", "Water Breathing", "water_breathing", "WATER_BREATHING"),
        new Option("SLOW_FALLING", "Slow Falling", "slow_falling", "SLOW_FALLING"),
        new Option("POISON", "Poison", "poison", "POISON"),
        new Option("WEAKNESS", "Weakness", "weakness", "WEAKNESS"),
        new Option("SLOWNESS", "Slowness", "slowness", "SLOWNESS")
    );
    private static final String TITLE = ChatColor.DARK_PURPLE + "LongPot | 8-minute potions";
    private static final class GuiHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("LongPot enabled!");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        if (!player.hasPermission("longpot.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        openGui(player);
        return true;
    }

    private void openGui(Player player) {
        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.inventory = inv;
        for (int i = 0; i < OPTIONS.size(); i++) {
            Option option = OPTIONS.get(i);
            boolean enabled = getConfig().getBoolean("enabled." + option.key(), false);
            ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + option.name());
            meta.setLore(List.of(ChatColor.GRAY + "Duration: 8:00", enabled
                ? ChatColor.GREEN + "ENABLED - click to disable"
                : ChatColor.RED + "DISABLED - click to enable"));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        player.openInventory(inv);
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= OPTIONS.size() || slot >= event.getView().getTopInventory().getSize()) return;
        if (!player.hasPermission("longpot.admin")) return;
        Option option = OPTIONS.get(slot);
        boolean enabled = !getConfig().getBoolean("enabled." + option.key(), false);
        getConfig().set("enabled." + option.key(), enabled);
        saveConfig();
        openGui(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "LongPot: " + option.name() + " " + (enabled ? "enabled" : "disabled"));
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler public void onBrew(BrewEvent event) {
        // Modify the brewed output, not the input potions.
        List<ItemStack> results = event.getResults();
        for (int i = 0; i < results.size(); i++) {
            ItemStack result = results.get(i);
            if (result == null || !isPotion(result.getType())) continue;
            if (!(result.getItemMeta() instanceof PotionMeta meta)) continue;
            PotionType base = meta.getBasePotionType();
            if (base == null) continue;
            String typeName = base.name();
            for (Option option : OPTIONS) {
                if (!matches(typeName, option.potionName())) continue;
                if (!getConfig().getBoolean("enabled." + option.key(), false)) break;
                PotionEffectType effectType = Registry.EFFECT.get(NamespacedKey.minecraft(option.effectKey()));
                if (effectType == null) break;
                int amplifier = typeName.startsWith("STRONG_") ? 1 : 0;
                meta.addCustomEffect(new PotionEffect(effectType, EIGHT_MINUTES, amplifier), true);
                result.setItemMeta(meta);
                results.set(i, result);
                break;
            }
        }
    }

    private boolean isPotion(Material material) {
        return material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION;
    }

    private boolean matches(String type, String baseName) {
        return type.equals(baseName) || type.equals("LONG_" + baseName) || type.equals("STRONG_" + baseName);
    }
}
