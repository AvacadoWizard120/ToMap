package io.github.AvacadoWizard120.toMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public final class ToMap extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        getLogger().info("ToMap plugin has been enabled!");
        Objects.requireNonNull(this.getCommand("tomap")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("ToMap plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("tomap")) {
            if (args.length < 1) {
                sender.sendMessage("Usage: /tomap <url> [resize] [width] [height]");
                return true;
            }

            String url = args[0];
            boolean resize = args.length > 1 && args[1].equalsIgnoreCase("resize");
            int width = 1;
            int height = 1;

            if (resize && args.length > 3) {
                try {
                    width = Integer.parseInt(args[2]);
                    height = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid width or height. Using default 1x1.");
                }
            }

            Player player = (Player) sender;
            loadImageToMap(player, url, resize, width, height);
            return true;
        }

        return false;
    }

    private void loadImageToMap(Player player, String url, boolean resize, int width, int height) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URI uri = URI.create(url);
                BufferedImage image = ImageIO.read(uri.toURL());

                if (resize) {
                    image = resizeImage(image, width * 128, height * 128);
                }

                BufferedImage finalImage = image;
                Bukkit.getScheduler().runTask(this, () -> {
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            ItemStack map = new ItemStack(Material.FILLED_MAP);
                            MapMeta mapMeta = (MapMeta) map.getItemMeta();
                            MapView view = Bukkit.createMap(player.getWorld());

                            view.getRenderers().clear();
                            view.addRenderer(new ImageRenderer(finalImage, x * 128, y * 128));

                            assert mapMeta != null;
                            mapMeta.setMapView(view);
                            map.setItemMeta(mapMeta);

                            player.getInventory().addItem(map);
                        }
                    }
                    player.sendMessage("Image loaded onto map(s)!");
                });
            } catch (IOException e) {
                player.sendMessage("Error loading image: " + e.getMessage());
            }
        });
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        return resizedImage;
    }

    private static class ImageRenderer extends MapRenderer {
        private final BufferedImage image;
        private final int offsetX;
        private final int offsetY;

        public ImageRenderer(BufferedImage image, int offsetX, int offsetY) {
            this.image = image;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    if (x + offsetX < image.getWidth() && y + offsetY < image.getHeight()) {
                        int rgb = image.getRGB(x + offsetX, y + offsetY);
                        Color color = new Color(rgb, true);
                        canvas.setPixelColor(x, y, color);
                    }
                }
            }
        }
    }
}