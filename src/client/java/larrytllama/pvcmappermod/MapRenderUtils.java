package larrytllama.pvcmappermod;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.UUID;

import larrytllama.pvcmappermod.utils.CompatUtils;
import larrytllama.pvcmappermod.utils.GraphicsHelper;
import larrytllama.pvcmappermod.utils.ResIdentifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.network.chat.Component;

public class MapRenderUtils {

    public static void drawTooltipComponent(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, List<? extends Component> content, int x, int y) {
        int lines = content.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i < lines; i++) {
            int w = mcfont.width(content.get(i));
            if (w > maxSize) {
                maxSize = w;
            }
        }
        GraphicsHelper.drawTooltipBackground(context, x, y, maxSize, mcfont.lineHeight * lines);
        for (int i = 0; i < lines; i++) {
            GraphicsHelper.drawString(context, mcfont, content.get(i), x, y + (i * mcfont.lineHeight), 0xFFFFFFFF);
        }
    }

    public static void drawTooltipString(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, List<String> content, int x, int y) {
        int lines = content.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(content.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        GraphicsHelper.drawTooltipBackground(context, x, y, maxSize, mcfont.lineHeight * lines);
        for (int i = 0; i < lines; i++) {
            GraphicsHelper.drawString(context, mcfont, content.get(i), x, y + (i * mcfont.lineHeight), 0xFFFFFFFF);
        }
    }


    public static double getScale() {
        return 1 / Math.pow(2, 8);
    }

    public static double metersToPixels(double num) {
        return Math.round(num / getScale());
    }

    /**
     * Converts a raw world coordinate to the appropriate map tile coordinate.
     * Safely handles negative coordinate integer division mapping.
     */
    public static int worldToTileCoordinate(double worldCoord, int tileSize) {
        return (int) Math.floor(worldCoord / (double) tileSize);
    }

    /**
     * Converts a raw world coordinate to the local pixel offset inside its map tile.
     * Safely handles negative coordinate modulo wrapping.
     */
    public static double worldToLocalTileCoordinate(double worldCoord, int tileSize) {
        return worldCoord - (Math.floor(worldCoord / (double) tileSize) * tileSize);
    }

    // Improved drawLine
    public static void drawLine(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int currentY = y0;
        int lineMinX = x0;
        int lineMaxX = x0;

        while (true) {
            int e2 = err * 2;

            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }

            // If Y changed, draw the accumulated scanline before moving on
            if (y0 != currentY) {
                drawScanline(g, lineMinX, lineMaxX, currentY, color);
                currentY = y0;
                lineMinX = x0;
                lineMaxX = x0;
            } else {
                // Still on same Y, accumulate X
                lineMinX = Math.min(lineMinX, x0);
                lineMaxX = Math.max(lineMaxX, x0);
            }

            if (x0 == x1 && y0 == y1) {
                // Draw final scanline
                drawScanline(g, lineMinX, lineMaxX, currentY, color);
                break;
            }
        }
    }

    private static void drawScanline(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        g, int minX, int maxX, int y, int color) {
        
        if (y <= 0 || y >= g.guiHeight()) return;
        
        minX = Math.max(minX, 0);
        maxX = Math.min(maxX, g.guiWidth() - 1);
        
        if (minX <= maxX) {
            g.fill(minX, y, maxX + 1, y + 1, color);
        }
    }

    public static boolean doesIntersect(double x1, double y1, double x2, double y2, double boxX, double boxY, double boxWidth, double boxHeight) {
        Rectangle2D rect = new Rectangle2D.Double(boxX, boxY, boxWidth, boxHeight);
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
                                        
        // intersectsLine checks both endpoint containment and edge crossings automatically
        return rect.intersectsLine(line);
    }

    public static int networkTypeToColour(String type, String source) {
        // Source parameter included for debugging context as requested
        if (SettingsProvider.getInstance().debugMode) {
            // Very noisy debug if uncommented, leaving standard just in case
            // LogUtils.debug("[%s] Requesting colour for network type: %s", source, type);
        }
        
        // Brought these over from Full Screen Map and unified them!
        // This will prevent the biting of butts.
        switch (type) {
            case "ice":
            case "boat":
                return 0xFF13F2F2;
            case "rail":
                return 0xFF000000;
            case "pathMark":
            case "pathUnmark":
                return 0xFFFFFFFF;
            default:
                return 0x00000000;
        }
    }

    /** Source - https://stackoverflow.com/a/9462757
    * Posted by Ivan T, modified by community. See post 'Timeline' for change history
    * Retrieved 2026-06-16, License - CC BY-SA 3.0
    * (Hell yeah! I'm still using stack overflow)*/
    public static double bearing(double lat1, double lon1, double lat2, double lon2){
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);    
        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }
    public static String currentWorld = "Mondo";

    // Cache local player location every 5 ticks like vanilla map items (set to 1 for realtime)
    public static final int LOCAL_PLAYER_UPDATE_TICKS = 5;
    public static double lastLocalPlayerX = 0;
    public static double lastLocalPlayerZ = 0;
    public static float lastLocalPlayerYaw = 0f;
    public static String lastLocalPlayerWorld = "";

    public static void updateLocalPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || isInQueue()) return;

        if (LOCAL_PLAYER_UPDATE_TICKS <= 1 || mc.player.tickCount % LOCAL_PLAYER_UPDATE_TICKS == 0 || lastLocalPlayerWorld.isEmpty()) {
            lastLocalPlayerX = mc.player.getX();
            lastLocalPlayerZ = mc.player.getZ();
            lastLocalPlayerYaw = mc.player.getYRot() + 180f;
            lastLocalPlayerWorld = getCurrentWorld();
        }
    }

    public static boolean isWorld(String world) {
        if (world == null) return false;
        return currentWorld.equalsIgnoreCase(world);
    }

    public static boolean isInQueue() {
        return isWorld("Queue");
    }

    public static boolean isInTerra2() {
        return isWorld("Terra2");
    }

    public static String getCurrentWorld() {
        return currentWorld;
    }

    public static String getWorld(String mapOrWorld) {
        if (mapOrWorld == null) return "Mondo";
        String lower = mapOrWorld.toLowerCase();
        if (lower.contains("queue")) return "Queue";
        if (lower.contains("terra2")) return "Terra2";
        if (lower.contains("cielo")) return "Cielo";
        return "Mondo";
    }

    public static boolean isTerra2Map(String mapDimension) {
        return getWorld(mapDimension).equalsIgnoreCase("Terra2");
    }

    public static boolean isMondoMap(String mapDimension) {
        return getWorld(mapDimension).equalsIgnoreCase("Mondo");
    }

    public static boolean isSameWorld(String mapA, String mapB) {
        if (mapA == null || mapB == null) return false;
        return getWorld(mapA).equalsIgnoreCase(getWorld(mapB));
    }

    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "the_nether";
    public static final String END = "the_end";
    // public static final String SIFT = "the_sift";

    public static final double NETHER_SCALE = 8.0;
    public static final double OVERWORLD_TO_NETHER_SCALE = 1.0 / NETHER_SCALE;

    // Could this come from the API?
    public static final List<String> MAP_TILESETS = List.of(
        "minecraft_overworld",
        "minecraft_the_nether",
        "minecraft_terra2"
    );

    public static List<String> getWorldTilesets(String world) {
        List<String> list = new java.util.ArrayList<>();
        for (String tileset : MAP_TILESETS) {
            if (isSameWorld(tileset, world)) {
                list.add(tileset);
            }
        }
        return list;
    }

    public static String getDimensionType(String mapOrWorld) {
        if (mapOrWorld == null) return OVERWORLD;
        String lower = mapOrWorld.toLowerCase();
        if (lower.contains("nether")) return NETHER;
        if (lower.contains("the_end") || lower.endsWith("_end")) return END;
        if (lower.contains("overworld") || lower.contains("terra2")) return OVERWORLD;
        return "unknown";
    }

    public static boolean isOverworld(String mapOrDimension) {
        return OVERWORLD.equals(getDimensionType(mapOrDimension));
    }

    public static boolean isNether(String mapOrDimension) {
        return NETHER.equals(getDimensionType(mapOrDimension));
    }

    public static boolean isEnd(String mapOrDimension) {
        return END.equals(getDimensionType(mapOrDimension));
    }

    public static boolean isInOverworld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return isOverworld(CompatUtils.getIdentifier(mc.level.dimension()).getPath());
    }

    public static boolean isInNether() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return isNether(CompatUtils.getIdentifier(mc.level.dimension()).getPath());
    }

    public static boolean isInEnd() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return isEnd(CompatUtils.getIdentifier(mc.level.dimension()).getPath());
    }

    public static boolean isNetherProjection(String playerDimension, String mapDimension) {
        return isNether(playerDimension) && isOverworld(mapDimension);
    }

    public static String prettyDimensionType(String mapOrWorld) {
        String type = getDimensionType(mapOrWorld);
        switch (type) {
            case NETHER: return "Nether";
            case END:    return "End";
            case OVERWORLD: return "Overworld";
            default:     return "Other";
        }
    }

    public static String prettyDimensionName(String mapOrWorld) {
        if (mapOrWorld == null) return "Unknown World";

        String world = getWorld(mapOrWorld);
        String dimType = prettyDimensionType(mapOrWorld);

        // Mondo uses standard vanilla names
        if (world.equalsIgnoreCase("Mondo")) {
            return dimType;
        }

        // Secondary worlds (Terra2, Cielo)
        if (dimType.equalsIgnoreCase("Overworld")) {
            return world;
        }
        return world + " " + dimType;
    }

    public static double getDimensionScale(String playerWorld, String playerDimension, String mapDimension) {
        if (playerWorld == null || playerDimension == null || mapDimension == null) return 1.0;

        String mapWorld = getWorld(mapDimension);
        if (!getWorld(playerWorld).equalsIgnoreCase(mapWorld)) {
            return 0.0; // Different worlds never project onto each other
        }

        String playerDim = getDimensionType(playerDimension);
        String mapDim = getDimensionType(mapDimension);

        if (playerDim.equals(mapDim)) return 1.0;
        if (playerDim.equals(NETHER) && mapDim.equals(OVERWORLD)) return NETHER_SCALE;
        if (playerDim.equals(OVERWORLD) && mapDim.equals(NETHER)) return OVERWORLD_TO_NETHER_SCALE;

        // Any other / unknown dimension on the same world (End, Sift, etc.)
        // projects 1:1 on Overworld, and 1:8 (0.125) on Nether
        if (!playerDim.equals(NETHER) && !playerDim.equals(OVERWORLD)) {
            if (mapDim.equals(OVERWORLD)) return 1.0;
            if (mapDim.equals(NETHER)) return OVERWORLD_TO_NETHER_SCALE;
        }

        return 0.0;
    }

    public static double getDimensionScale(String playerDimension, String mapDimension) {
        if (playerDimension == null || mapDimension == null) return 1.0;
        return getDimensionScale(getWorld(playerDimension), playerDimension, mapDimension);
    }

    public static String getActiveTileset(boolean isInTerra2, String mcDimensionPath) {
        if (isInTerra2) {
            if (mcDimensionPath != null && (mcDimensionPath.equals("the_end") || mcDimensionPath.equals("minecraft_the_end"))) {
                return "minecraft_unknown_world";
            }
            // Terra2 overworld & nether both show the Terra2 map
            return "minecraft_terra2";
        }
        if (mcDimensionPath == null) return "minecraft_overworld";
        if (isNether(mcDimensionPath)) {
            return "minecraft_the_nether";
        }
        return "minecraft_overworld";
    }

    public static final ResIdentifier LOCATOR_DOT = ResIdentifier.of("minecraft",
            "textures/gui/sprites/hud/locator_bar_dot/default_1.png");
    public static final int END_MARKER_TINT = 0xFFE357FF; // #e357ff

    public static int uuidToPlayerColor(String uuid) {
        if (uuid == null) return 0xFFFFFFFF;
        try {
            int hashed = UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5")).hashCode();
            float[] hsv = Color.RGBtoHSB((hashed >> 16) & 0xFF, (hashed >> 8) & 0xFF, hashed & 0xFF, null);
            return 0xFF000000 | (Color.HSBtoRGB(hsv[0], Math.max(0.4f, hsv[1]), 1.0f) & 0xFFFFFF);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    public static String getTileUrl(String baseSource, String dimension, boolean useDarkTiles, int zoom, int tileX, int tileZ) {
        if (dimension == null) dimension = "minecraft_overworld";
        String targetDimension = dimension.equals("minecraft_terra2") && useDarkTiles ? "minecraft_terra2_night" : dimension;
        return String.format("%s%s/%d/%d_%d.png", baseSource, targetDimension, zoom, tileX, tileZ);
    }

    public static String getCurrentDimension() {
        if (isInQueue()) return "minecraft_unknown_world";
        if (Minecraft.getInstance().level == null) return "minecraft_overworld";
        String dimensionPath = CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
        return getActiveTileset(isInTerra2(), dimensionPath);
    }
}
