package fr.atlantyka.chatbubbles;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AtlantykaChatBubbles extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, List<TextDisplay>> bubbles = new HashMap<>();
    private BukkitTask followTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("holochat").setExecutor(this);
        long interval = Math.max(1L, getConfig().getLong("update-interval-ticks", 2L));
        followTask = Bukkit.getScheduler().runTaskTimer(this, this::updateAll, 1L, interval);
        getLogger().info("AtlantykaChatBubbles active !");
    }

    @Override
    public void onDisable() {
        if (followTask != null) followTask.cancel();
        for (List<TextDisplay> list : bubbles.values()) {
            for (TextDisplay display : list) if (display.isValid()) display.remove();
        }
        bubbles.clear();
    }

    // Skript "on chat" utilise l'evenement Bukkit historique AsyncPlayerChatEvent.
    // On ecoute aussi les evenements annules pour conserver le chat personnalise.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message == null || message.isBlank()) return;
        UUID playerId = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(this, () -> showBubble(playerId, message));
    }

    private void showBubble(UUID id, String message) {
        Player player = Bukkit.getPlayer(id);
        if (player == null || !player.isOnline()) return;

        List<TextDisplay> list = bubbles.computeIfAbsent(id, ignored -> new ArrayList<>());
        int max = Math.max(1, getConfig().getInt("max-messages", 3));
        while (list.size() >= max) {
            TextDisplay oldest = list.remove(list.size() - 1);
            if (oldest.isValid()) oldest.remove();
        }

        Location location = player.getLocation().clone().add(0, getConfig().getDouble("height-offset", 2.25), 0);
        TextDisplay display = player.getWorld().spawn(location, TextDisplay.class);
        display.setText(message);
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(Math.max(1, getConfig().getInt("line-width", 200)));
        display.setShadowed(getConfig().getBoolean("shadow", true));
        display.setSeeThrough(getConfig().getBoolean("see-through", false));
        display.setDefaultBackground(false);
        display.setBackgroundColor(parseColor(getConfig().getString("background-color", "#80000000")));
        display.setTextOpacity((byte) Math.max(0, Math.min(255, getConfig().getInt("text-opacity", 255))));
        display.setPersistent(false);
        list.add(0, display);
        updatePlayer(player);

        long seconds = Math.max(1L, getConfig().getLong("duration-seconds", 5L));
        Bukkit.getScheduler().runTaskLater(this, () -> removeBubble(id, display), seconds * 20L);
    }

    private Color parseColor(String value) {
        try {
            String hex = (value == null ? "80000000" : value.trim()).replace("#", "");
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() == 6) parsed |= 0x80000000L;
            return Color.fromARGB((int) parsed);
        } catch (RuntimeException ex) {
            return Color.fromARGB(0x80000000);
        }
    }

    private void updateAll() {
        Iterator<Map.Entry<UUID, List<TextDisplay>>> it = bubbles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<TextDisplay>> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                for (TextDisplay display : entry.getValue()) if (display.isValid()) display.remove();
                it.remove();
            } else {
                updatePlayer(player);
            }
        }
    }

    private void updatePlayer(Player player) {
        List<TextDisplay> list = bubbles.get(player.getUniqueId());
        if (list == null) return;
        Location base = player.getLocation().clone();
        double height = getConfig().getDouble("height-offset", 2.25);
        double spacing = getConfig().getDouble("line-spacing", 0.30);
        for (int i = 0; i < list.size(); i++) {
            TextDisplay display = list.get(i);
            if (display.isValid()) display.teleport(base.clone().add(0, height + i * spacing, 0));
        }
    }

    private void removeBubble(UUID id, TextDisplay display) {
        if (display.isValid()) display.remove();
        List<TextDisplay> list = bubbles.get(id);
        if (list != null) {
            list.remove(display);
            if (list.isEmpty()) bubbles.remove(id);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        List<TextDisplay> list = bubbles.remove(event.getPlayer().getUniqueId());
        if (list != null) for (TextDisplay display : list) if (display.isValid()) display.remove();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("atlantykachatbubbles.admin")) {
            sender.sendMessage("§cTu n'as pas la permission.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§b[Atlantyka] §aConfiguration rechargée.");
            return true;
        }
        sender.sendMessage("§b/holochat reload §7- Recharge la configuration.");
        return true;
    }
}