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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * MRU is mostly for map rendering and other UI helpers.
 * It handles things like world/dimension and local player state, coordinate scaling for
 * ow/nether, map decorations/icons, etc.
 */
public class MapRenderUtils {

    // Define all of the things!
    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "the_nether";
    public static final String END = "the_end";
    public static final String SIFT = "the_sift";

    public static final double OVERWORLD_BLOCKS_PER_NETHER = 8.0;

    public static final int END_MARKER_TINT = 0xFFE357FF; // #e357ff
    // Red tint for local player marker when in the Nether on Terra2 (#ff2119)
    public static final int NETHER_LOCAL_MARKER_TINT = 0xFFFF2119;

    public static final ResIdentifier LOCATOR_DOT = ResIdentifier.of("minecraft",
            "textures/gui/sprites/hud/locator_bar_dot/default_1.png");
    public static final ResIdentifier THIS_PLAYER = ResIdentifier.of("minecraft",
            "textures/map/decorations/player.png");
    public static final ResIdentifier OTHER_PLAYERS_OW = ResIdentifier.of("minecraft",
            "textures/map/decorations/frame.png");
    public static final ResIdentifier OTHER_PLAYERS_NETHER = ResIdentifier.of("minecraft",
            "textures/map/decorations/red_marker.png");
    public static final ResIdentifier OTHER_PLAYERS_SOMEWHERE = ResIdentifier.of("minecraft",
            "textures/map/decorations/blue_marker.png");
    public static final ResIdentifier TARGET_POINT = ResIdentifier.of("minecraft",
            "textures/map/decorations/target_point.png");

    public static final ResIdentifier SHOP_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/orange_banner.png");
    public static final ResIdentifier EVENT_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/magenta_banner.png");
    public static final ResIdentifier LANDMARK_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/yellow_banner.png");
    public static final ResIdentifier BASE_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/light_blue_banner.png");
    public static final ResIdentifier GRAY_BANNER = ResIdentifier.of("minecraft",
            "textures/map/decorations/light_gray_banner.png");

    public static final ResIdentifier NETHER_PORTAL = ResIdentifier.of("pvcmappermod",
            "textures/gui/portal.png");
    public static final ResIdentifier ENDER_CHEST = ResIdentifier.of("minecraft",
            "textures/item/ender_eye.png");
    public static final ResIdentifier TERRA2_PORTAL = ResIdentifier.of("minecraft",
            "textures/gui/sprites/icon/new_realm.png");
    public static final ResIdentifier UNKNOWN_FEATURE = ResIdentifier.of("minecraft",
            "textures/gui/sprites/icon/unseen_notification.png");

    public static String currentWorld = "Mondo";

    public static double lastLocalPlayerX = 0;
    public static double lastLocalPlayerZ = 0;
    public static float lastLocalPlayerYaw = 0f;
    public static String lastLocalPlayerWorld = "";

    public record DimensionConfig(
        String type,
        String tileset,
        String altTileset,
        double scaleToOverworld,
        String linkedType
    ) {}

    public record WorldConfig(
        String name,
        boolean selectableInMapViewer,
        List<DimensionConfig> dimensions
    ) {}

    public record LinkedDimension(
        String type,
        String displayName,
        double scale
    ) {}

    // now this could come from an API
    public static final List<WorldConfig> WORLDS = List.of(
        new WorldConfig("Mondo", true, List.of(
            new DimensionConfig(OVERWORLD, "minecraft_overworld", null, 1.0, NETHER),
            new DimensionConfig(NETHER, "minecraft_the_nether", null, OVERWORLD_BLOCKS_PER_NETHER, OVERWORLD),
            new DimensionConfig(END, null, null, 1.0, null)
        )),
        new WorldConfig("Terra2", true, List.of(
            new DimensionConfig(OVERWORLD, "minecraft_terra2", "minecraft_terra2_night", 1.0, NETHER),
            new DimensionConfig(NETHER, null, null, OVERWORLD_BLOCKS_PER_NETHER, OVERWORLD),
            new DimensionConfig(END, null, null, 1.0, null),
            new DimensionConfig(SIFT, null, null, 1.0, null)
        )),
        new WorldConfig("Cielo", true, List.of(
            new DimensionConfig(OVERWORLD, null, null, 1.0, null)
        )),
        new WorldConfig("Queue", false, List.of(
            new DimensionConfig(OVERWORLD, null, null, 1.0, null)
        ))
    );

    public static WorldConfig getWorldConfig(String target) {
        if (target != null) {
            for (WorldConfig wc : WORLDS) {
                if (wc.name().equals(target)) return wc;
                for (DimensionConfig dc : wc.dimensions()) {
                    if (target.equals(dc.tileset()) || target.equals(dc.altTileset())) {
                        return wc;
                    }
                }
            }
        }
        return WORLDS.get(0);
    }

    public static String getWorldName(String target) {
        return getWorldConfig(target).name();
    }

    public static List<String> getMappableWorlds() {
        List<String> list = new java.util.ArrayList<>();
        for (WorldConfig wc : WORLDS) {
            if (wc.selectableInMapViewer()) {
                list.add(wc.name());
            }
        }
        return list;
    }

    public static List<String> getAllTilesets() {
        List<String> list = new java.util.ArrayList<>();
        for (WorldConfig wc : WORLDS) {
            for (DimensionConfig dc : wc.dimensions()) {
                if (dc.tileset() != null) {
                    list.add(dc.tileset());
                }
            }
        }
        return list;
    }

    public static List<String> getWorldTilesets(String world) {
        WorldConfig wc = getWorldConfig(world);
        List<String> list = new java.util.ArrayList<>();
        for (DimensionConfig dc : wc.dimensions()) {
            if (dc.tileset() != null) {
                list.add(dc.tileset());
            }
        }
        return list;
    }

    public static DimensionConfig getDimensionConfig(String world, String dimension) {
        WorldConfig wc = getWorldConfig(world);
        String type = getDimensionType(dimension);
        for (DimensionConfig dc : wc.dimensions()) {
            if (dc.type().equals(type)) {
                return dc;
            }
        }
        return wc.dimensions().get(0);
    }

    public static String getDimensionType(String dimension) {
        if (dimension == null) return OVERWORLD;
        for (WorldConfig wc : WORLDS) {
            for (DimensionConfig dc : wc.dimensions()) {
                if (dimension.equals(dc.type()) || dimension.equals(dc.tileset()) || dimension.equals(dc.altTileset())) {
                    return dc.type();
                }
            }
        }
        if (dimension.startsWith("minecraft_")) {
            return  (dimension.substring("minecraft_".length()));
        }
        return "unknown";
    }

    public static String getActiveTileset(String world, String dimension) {
        WorldConfig wc = getWorldConfig(world);
        DimensionConfig dc = getDimensionConfig(wc.name(), dimension);
        if (dc.tileset() != null) {
            return dc.tileset();
        }
        // If dimension doesn't have its own tiles (Terra2 Nether, End, etc), fall back to that world's overworld tileset
        // which may not exist either
        for (DimensionConfig d : wc.dimensions()) {
            if (OVERWORLD.equals(d.type()) && d.tileset() != null) {
                return d.tileset();
            }
        }
        return null;
    }

    // this helps with determining if it's linked, as in, can you use it for fast travel
    public static LinkedDimension getLinkedDimension(String dimension) {
        if (dimension == null) return null;
        // get all the worlds that have this dimension
        WorldConfig wc = getWorldConfig(dimension);
        DimensionConfig dc = getDimensionConfig(wc.name(), dimension);
        if (dc.linkedType() == null) return null;

        DimensionConfig targetDc = getDimensionConfig(wc.name(), dc.linkedType());
        double scale = dc.scaleToOverworld() / targetDc.scaleToOverworld();
        return new LinkedDimension(targetDc.type(), prettyDimensionName(wc.name(), targetDc.type()), scale);
    }

    public static double getDimensionScale(String playerWorld, String playerDimension, String mapDimension) {
        if (playerWorld == null || playerDimension == null || mapDimension == null) return 0.0;

        WorldConfig playerWorldConfig = getWorldConfig(playerWorld);
        WorldConfig mapWorldConfig = getWorldConfig(mapDimension);
        if (!playerWorldConfig.name().equals(mapWorldConfig.name())) {
            return 0.0; // Different worlds never project onto each other
        }

        DimensionConfig playerDimensionConfig = getDimensionConfig(playerWorldConfig.name(), playerDimension);
        DimensionConfig mapDimensionConfig = getDimensionConfig(mapWorldConfig.name(), mapDimension);

        if (playerDimensionConfig.type().equals(mapDimensionConfig.type())) return 1.0;

        if (mapDimensionConfig.scaleToOverworld() > 0.0) {
            return playerDimensionConfig.scaleToOverworld() / mapDimensionConfig.scaleToOverworld();
        }
        return 1.0;
    }

    public static double getDimensionScale(String playerDimension, String mapDimension) {
        if (playerDimension == null || mapDimension == null) return 0.0;
        return getDimensionScale(getWorldName(playerDimension), playerDimension, mapDimension);
    }

    /**
     * Defines the base zoom offset relative to the Overworld.
     * Shifting zoom by 3 steps matches an 8x coordinate scale (2^3 = 8).
     */
    public static int getDimensionZoomOffset(String dimension) {
        return isNether(dimension) ? 3 : 0;
    }

    public static String prettyDimensionType(String dimension) {
        switch (getDimensionType(dimension)) {
            case NETHER:    return "Nether";
            case END:       return "End";
            case SIFT:      return "The Sift";
            case OVERWORLD: return "Overworld";
            default:        return "Other";
        }
    }

    public static String prettyDimensionName(String world, String dimType) {
        String typePretty = prettyDimensionType(dimType);
        if ("Mondo".equals(world)) {
            return typePretty;
        }
        if (OVERWORLD.equals(dimType)) {
            return world;
        }
        return world + " " + typePretty;
    }

    public static String prettyDimensionName(String dimension) {
        if (dimension == null) return "Unknown World";
        return prettyDimensionName(getWorldName(dimension), getDimensionType(dimension));
    }

    public static String getTileUrl(String baseSource, String dimension, boolean useDarkTiles, int zoom, int tileX, int tileZ) {
        if (dimension == null) dimension = "minecraft_overworld";
        String targetDimension = dimension;
        if (useDarkTiles) {
            WorldConfig wc = getWorldConfig(dimension);
            for (DimensionConfig dc : wc.dimensions()) {
                if (dimension.equals(dc.tileset()) && dc.altTileset() != null) {
                    targetDimension = dc.altTileset();
                    break;
                }
            }
        }
        return String.format("%s%s/%d/%d_%d.png", baseSource, targetDimension, zoom, tileX, tileZ);
    }

    public static String getCurrentWorld() {
        return currentWorld;
    }

    public static String getCurrentDimension() {
        if (isInQueue()) return null;
        if (Minecraft.getInstance().level == null) return "minecraft_overworld";
        String dimensionPath = CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
        return getActiveTileset(getCurrentWorld(), dimensionPath);
    }

    public static boolean isWorld(String world) {
        return currentWorld.equals(world);
    }

    public static boolean isInQueue() {
        return isWorld("Queue");
    }

    public static boolean isInTerra2() {
        return isWorld("Terra2");
    }

    public static boolean isMondoMap(String tileset) {
        return getWorldName(tileset).equals("Mondo");
    }

    public static boolean isTerra2Map(String tileset) {
        return getWorldName(tileset).equals("Terra2");
    }

    public static boolean isOverworld(String dimension) {
        return OVERWORLD.equals(getDimensionType(dimension));
    }

    public static boolean isNether(String dimension) {
        return NETHER.equals(getDimensionType(dimension));
    }

    public static boolean isEnd(String dimension) {
        return END.equals(getDimensionType(dimension));
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

    public static void updateLocalPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || isInQueue()) return;

        lastLocalPlayerX = mc.player.getX();
        lastLocalPlayerZ = mc.player.getZ();
        lastLocalPlayerYaw = mc.player.getYRot() + 180f;
        lastLocalPlayerWorld = getCurrentWorld();
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
     * Make sure negative tiles start at -1 not -0.
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

    public static boolean doesIntersect(double x1, double y1, double x2, double y2, double boxX, double boxY, double boxWidth, double boxHeight) {
        Rectangle2D rect = new Rectangle2D.Double(boxX, boxY, boxWidth, boxHeight);
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
                                        
        // intersectsLine checks both endpoint containment and edge crossings automatically
        return rect.intersectsLine(line);
    }

    // Google Mapsy cubic ease-in-out curve.
    public static double easeInOutCubic(double t) {
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;
    }

    public static double interpolateCamera(double start, double end, double progress) {
        return start + (end - start) * easeInOutCubic(progress);
    }

    // nice auto-map panning using an animation class
    public static class CameraPan {
        private final double startX;
        private final double startZ;
        private final double targetX;
        private final double targetZ;
        private final long startTime;
        private final long durationMs;
        private boolean active = true;

        public CameraPan(double startX, double startZ, double targetX, double targetZ, long durationMs) {
            this.startX = startX;
            this.startZ = startZ;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isActive() {
            return active;
        }

        public void cancel() {
            this.active = false;
        }

        public double getProgress() {
            if (!active) return 1.0;
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= durationMs) return 1.0;
            return (double) elapsed / durationMs;
        }

        public double getX() {
            return interpolateCamera(startX, targetX, getProgress());
        }

        public double getZ() {
            return interpolateCamera(startZ, targetZ, getProgress());
        }

        public boolean isFinished() {
            return !active || (System.currentTimeMillis() - startTime >= durationMs);
        }

        public double getTargetX() {
            return targetX;
        }

        public double getTargetZ() {
            return targetZ;
        }
    }

    public static ResIdentifier getPlayerMarker(String world) {
        String dimType = getDimensionType(world);
        switch (dimType) {
            case NETHER:
                return OTHER_PLAYERS_NETHER;
            case END:
                return THIS_PLAYER;
            case OVERWORLD:
            default:
                return OTHER_PLAYERS_OW;
        }
    }

    public static int getPlayerMarkerTint(String world) {
        return isEnd(world) ? END_MARKER_TINT : 0xFFFFFFFF;
    }

    public static int getLocalPlayerMarkerTint(String localDimensionPath, String mapDimension) {
        if (isEnd(localDimensionPath)) {
            return END_MARKER_TINT;
        } else if (isNetherProjection(localDimensionPath, mapDimension)) {
            // set the white one to red since it's a different graphic in 26.3+
            return NETHER_LOCAL_MARKER_TINT;
        }
        return 0xFFFFFFFF;
    }

    public static int getLocalPlayerMarkerTint(String localDimensionPath) {
        return getLocalPlayerMarkerTint(localDimensionPath, getCurrentDimension());
    }

    public static int getLocalPlayerMarkerTint() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0xFFFFFFFF;
        String localDim = CompatUtils.getIdentifier(mc.level.dimension()).getPath();
        return getLocalPlayerMarkerTint(localDim);
    }

    public static ResIdentifier getPlaceIcon(String placeType) {
        if (placeType == null) return GRAY_BANNER;
        switch (placeType) {
            case "farm":
            case "landmark":
            case "museum":
            case "lighthouse":
                return LANDMARK_BANNER;
            case "shop":
            case "mall":
                return SHOP_BANNER;
            case "union":
            case "base":
            case "town":
                return BASE_BANNER;
            case "event":
            case "pvp":
                return EVENT_BANNER;
            default:
                return GRAY_BANNER;
        }
    }

    public static ResIdentifier getPortalIcon(String portalType) {
        if (portalType == null) return UNKNOWN_FEATURE;
        switch (portalType) {
            case "portal":
            case "portalandechest":
            case "nether":
                return NETHER_PORTAL;
            case "echest":
                return ENDER_CHEST;
            case "terra2":
            case "terra2portal":
                return TERRA2_PORTAL;
            default:
                return UNKNOWN_FEATURE;
        }
    }

    public static String getPortalPrettyName(String portalType) {
        if (portalType == null) return "Unknown portal";
        switch (portalType) {
            case "portal":
            case "nether":
                return "Nether Portal";
            case "portalandechest":
                return "Nether Portal with Ender Chest";
            case "echest":
                return "Ender Chest";
            case "terra2":
            case "terra2portal":
                return "Terra2 Portal";
            default:
                return "Unknown portal";
        }
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

    public static void drawPlayerMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY, float yawRad, ResIdentifier marker, int tint) {
        context.pose().pushMatrix();
        context.pose().translate(screenX, screenY);
        context.pose().rotate(yawRad);
        // prevent rotation wobble because it's an odd number of pixels wide, so the center
        // is technically at 4.5 sub-pixel
        context.pose().translate(-4.5f, -4.5f);
        if (tint != 0 && tint != 0xFFFFFFFF) {
            context.blit(RenderPipelines.GUI_TEXTURED, marker.get(), 0, 0, 0, 0, 8, 8, 8, 8, tint);
        } else {
            context.blit(RenderPipelines.GUI_TEXTURED, marker.get(), 0, 0, 0, 0, 8, 8, 8, 8);
        }
        context.pose().popMatrix();
    }

    public static void drawPlayerMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY, float yawRad, ResIdentifier marker) {
        drawPlayerMarker(context, screenX, screenY, yawRad, marker, 0);
    }

    public static void drawLocalPlayerMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY, float yawRad) {
        drawPlayerMarker(context, screenX, screenY, yawRad, THIS_PLAYER, getLocalPlayerMarkerTint());
    }

    public static void drawLocalPlayerMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY, float yawRad, String mapDimension) {
        Minecraft mc = Minecraft.getInstance();
        String localDim = mc.level != null ? CompatUtils.getIdentifier(mc.level.dimension()).getPath() : "overworld";
        drawPlayerMarker(context, screenX, screenY, yawRad, THIS_PLAYER, getLocalPlayerMarkerTint(localDim, mapDimension));
    }

    public static void drawStaticMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY, ResIdentifier icon) {
        context.pose().pushMatrix();
        context.pose().translate(screenX, screenY);
        context.pose().translate(-4, -4);
        context.blit(RenderPipelines.GUI_TEXTURED, icon.get(), 0, 0, 0, 0, 8, 8, 8, 8);
        context.pose().popMatrix();
    }

    public static void drawPinMarker(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, float screenX, float screenY) {
        context.pose().pushMatrix();
        context.pose().translate(screenX, screenY);
        // Center 7px-wide arrow and place bottom tip on point
        context.pose().translate(-4.5f, -8.0f);
        context.blit(RenderPipelines.GUI_TEXTURED, TARGET_POINT.get(), 0, 0, 0, 0, 8, 8, 8, 8);
        context.pose().popMatrix();
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
}
