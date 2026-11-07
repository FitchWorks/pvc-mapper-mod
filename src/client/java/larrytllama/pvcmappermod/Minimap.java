package larrytllama.pvcmappermod;

import static larrytllama.pvcmappermod.MapRenderUtils.prettyDimensionName;
import larrytllama.pvcmappermod.utils.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Definitions for players api
 */
class PlayerFetch {
    String world;
    Float armor;
    String name;
    int x;
    int y;
    int z;
    Float health;
    String uuid;
    Float yaw;
    Float pitch;
    Float roll;
    String afksince;
    String nickname;
    boolean isAFK;
    String[] ranks;
}

class PlaceFetch {
    int id;
    String name;
    String x; // Why tf did I make these strings
    String z; // Like, it's a coord - What was I thinking?
    String type;
    String dimension;
}

public class Minimap {

    public PlayerFetchUtils pfu;
    public SettingsProvider sp;
    public DirectionsProvider dp;
    public Network[] allNetworks;
    public static Minimap attach(PlayerFetchUtils pfu, SettingsProvider sp, DirectionsProvider dp) {
        Minimap minimap = new Minimap();
        minimap.pfu = pfu;
        minimap.dp = dp;
        pfu.fetchNetworksAsync().thenAccept(networks -> {
            minimap.allNetworks = networks;
        });
        minimap.sp = sp;
        //? if <26.1 {
        HudElementRegistry.attachElementBefore(VanillaHudElements.STATUS_EFFECTS,
//?} else {
/*        HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS,*///?}
                ResIdentifier.of("larrytllama.pvcmappermod", "before_chat").get(), minimap::render);
        return minimap;
    }

    private boolean hasNotBeenInitialisedYet = true;


    public ArrayList<PlaceFetch> placesList = new ArrayList<PlaceFetch>();

    private void sendPlayerListFeedback() {
        
        // Calculate tile size from zoom
        int tilesize = 1 << (17 - zoomlevel);
        MutableComponent message = Component.literal("PVC Mapper Minimap\n").withStyle(ChatFormatting.GREEN,
                ChatFormatting.ITALIC);
        Minecraft instance = Minecraft.getInstance();
        // cast do floor calc when casting to int for discrete boundaries & clean sharing
        int minX = (int) Math.floor(instance.player.getX()) - (tilesize / 2);
        int minZ = (int) Math.floor(instance.player.getZ()) - (tilesize / 2);
        int maxX = (int) Math.floor(instance.player.getX()) + (tilesize / 2);
        int maxZ = (int) Math.floor(instance.player.getZ()) + (tilesize / 2);
        Instant now = Instant.now();
        ArrayList<PlayerFetch> playersList = pfu.getPlayers();
        for (int i = 0; i < playersList.size(); i++) {
            PlayerFetch player = playersList.get(i);
            if ((player.x > minX && player.x < maxX) && (player.z > minZ && player.z < maxZ)) {
                Instant then = Instant.parse(player.afksince);
                Duration d = Duration.between(then, now);
                message.append(
                        // 1. LarryTLlama - 1234, -5678
                        // AFK Streak: 1d 2h 3m 4s
                        Component.literal(
                                (i + 1) + ". " + player.name + " - " + player.x + ", " + player.z + "\n" +
                                        "   AFK Streak: "
                                        + (d.toMinutes() < 2 ? "Not AFK"
                                                : ((d.toDaysPart() > 0 ? d.toDaysPart() + "d " : "") +
                                                        (d.toHoursPart() > 0 ? d.toHoursPart() + "h " : "") +
                                                        (d.toMinutesPart() > 0 ? d.toMinutesPart() + "m " : "") +
                                                        (d.toSecondsPart() > 0 ? d.toSecondsPart() + "s " : ""))))
                                .setStyle(
                                        Style.EMPTY.withColor(ChatFormatting.YELLOW)
                                                .withHoverEvent(new HoverEvent.ShowText(Component
                                                        .literal("Highlighted on minimap!\nUuid: " + player.uuid)))));
            }
        }
    }

    private void sendPlaceListFeedback() {

    }

    public void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("minimap")
                        .then(Commands.literal("players")
                                .executes(context -> {
                                    sendPlayerListFeedback();
                                    return 1;
                                }))
                        .then(Commands.literal("places")
                                .executes(context -> {
                                    sendPlaceListFeedback();
                                    return 1;
                                })));
        });
    }

    // Zoom level for map
    public static final int BASE_TILE_SIZE = 128;
    public int zoomlevel = 8;
    public int minimapTileSize = 80;
    public int topLeftX = 8;
    public int lastTileX = Integer.MIN_VALUE;
    public int lastTileZ = Integer.MIN_VALUE;
    private final ResIdentifier MENU_BG = ResIdentifier.of("minecraft",
            "textures/gui/menu_background.png");
    private final ResIdentifier MAP_BG = ResIdentifier.of("minecraft",
            "textures/map/map_background_checkerboard.png");
    

    private void renderMinimapTooltip(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, @Nullable List<String> text) {
        List<String> tooltipText = text == null
                ? List.of("Move your mouse over an icon on\n the map to see more details!")
                : text;
        int lines = tooltipText.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(tooltipText.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        int pad = getMinimapPadding();
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = topLeftX - pad - 5 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = topLeftX + minimapTileSize + pad + 5;
        }

        if(dp != null && dp.routeActive) {
            MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 24 + (mcfont.lineHeight * 2));
        } else {
            MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 8);
        }
    }

    private ResIdentifier playerTooltipSkin = PlayerSkinHelper.DEFAULT_SKIN;

    private void getTooltipPlayer(String uuid, String name) {
        PlayerSkinHelper.fetchSkin(uuid, "Minimap", (skin) -> {
            playerTooltipSkin = skin;
        });
    }

    private void renderMinimapTooltipPlayer(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, List<String> text, String uuid) {
        List<String> tooltipText = text;
        int lines = tooltipText.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(tooltipText.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        int pad = getMinimapPadding();
        int tooltipX = -1000;
        if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
            tooltipX = topLeftX - pad - 5 - maxSize;
        } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            tooltipX = topLeftX + minimapTileSize + pad + 5;
        } else {
            LogUtils.debug("[%s] Whoops no minimap pos: %s", "Minimap", sp.miniMapPos);
        }
        MapRenderUtils.drawTooltipString(context, tooltipText, tooltipX, 8);

        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin.get(), tooltipX + mcfont.width(text.get(0)) + 1, 8, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, playerTooltipSkin.get(), tooltipX + mcfont.width(text.get(0)) + 1, 8, 40, 8, 8, 8, 64, 64 );

        int rgbout = MapRenderUtils.uuidToPlayerColor(uuid);
        context.blit(RenderPipelines.GUI_TEXTURED, MapRenderUtils.LOCATOR_DOT.get(), tooltipX + mcfont.width(text.get(0)) + 11, 8, 0, 0, 8, 8, 9, 9, rgbout);
    }

    private String lastDimension = "";

    public void resetTileImageCache() {
        // Reset image cache
        // Including this one which should never realistically be a value
        // (Unless PVC's still expanding the map 15000 years later)
        lastTileX = Integer.MIN_VALUE;
        lastTileZ = Integer.MIN_VALUE;
    }

    public boolean isLoadingIn = true;
    public String[] spinnerParts = {" |", "/", "-", "\\", " |", "/", "-", "\\"};
    private int spinnerPart = 0;

    private final TransportNetwork transportNetwork = new TransportNetwork();

    // Base minimap size is 80px (100%). Every 25% step equals 20px.
    // Step 0 is Auto (targets ~22% screen height in 20px steps, clamped 60-160px).
    // Steps 1+ correspond to (step + 1) * 25% (step 1 = 50%, step 3 = 100%, etc.).
    public static int getAutoMapBoxSize(int screenHeight) {
        int target = (int) Math.round(screenHeight * 0.22);
        int step = (int) Math.round(target / 20.0);
        return Math.max(60, Math.min(160, step * 20));
    }

    public static int getMaxSliderStep(int screenHeight) {
        // Cap slider at 45% screen height, with a baseline minimum max of 7 steps (200% / 160px)
        return Math.max(7, (int) Math.floor((screenHeight * 0.45) / 20.0));
    }

    public static int percentToStep(int percent, int maxStep) {
        if (percent <= 0) return 0;
        int step = (percent / 25) - 1;
        return Math.max(1, Math.min(maxStep, step));
    }

    public static int stepToPercent(int step) {
        return step <= 0 ? 0 : (step + 1) * 25;
    }

    public static String getSliderLabel(int step, int screenHeight) {
        if (step == 0) {
            int autoSize = getAutoMapBoxSize(screenHeight);
            int autoPct = (int) Math.round((autoSize / 80.0) * 100.0);
            return "Auto (" + autoPct + "%)";
        }
        return ((step + 1) * 25) + "%";
    }

    public int getMapBoxSize() {
        if(sp.minimapSizePercent <= 0) {
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            return getAutoMapBoxSize(screenHeight);
        }
        return Math.max(40, (int) Math.round(80.0 * (sp.minimapSizePercent / 100.0)));
    }

    public int getMinimapPadding() {
        // Vanilla ItemInHandRenderer uses 7 units padding for a 128-unit map (~3.15 texture pixels on a 64x64 bg)
        return Math.max(2, (int) Math.round(minimapTileSize * (7.0 / 128.0)));
    }

    public void recalculateNetworks() {
        minimapTileSize = getMapBoxSize();
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) BASE_TILE_SIZE / tilesize;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String currentDimensionPath = mc.level != null ? CompatUtils.getIdentifier(mc.level.dimension()).getPath() : "overworld";
        String mapDimension = MapRenderUtils.getCurrentDimension();

        double dimensionScale = MapRenderUtils.getDimensionScale(MapRenderUtils.currentWorld, currentDimensionPath, mapDimension);
        double x = (MapRenderUtils.lastLocalPlayerX * dimensionScale) - ((minimapTileSize / 2.0) / scale);
        double z = (MapRenderUtils.lastLocalPlayerZ * dimensionScale) - ((minimapTileSize / 2.0) / scale);

        transportNetwork.recalculate(allNetworks, mapDimension, zoomlevel, scale, x, z, minimapTileSize, minimapTileSize, "Minimap");
    }

    private float tickAccumulator = 0f; // Track ticks for stuff

    int lastX = 0;
    int lastZ = 0;

    public boolean playerInCoords(int x1, int z1, int x2, int z2, String dimension, int x, int z) {
        double scale = MapRenderUtils.getDimensionScale(dimension, MapRenderUtils.getCurrentDimension());
        if (scale == 0.0) return false;
        double mappedX = x * scale;
        double mappedZ = z * scale;
        return x1 < mappedX && x2 > mappedX && z1 < mappedZ && z2 > mappedZ;
    }

    public void render(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, DeltaTracker tickCounter) {
        if(!sp.showInOtherPlaces && !pfu.isInPVC()) {
            return;
        }
        tickAccumulator += tickCounter.getRealtimeDeltaTicks();
        if(!sp.miniMapEnabled) return;
        minimapTileSize = getMapBoxSize();
        int screenwidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
            topLeftX = 8;
        } else {
            topLeftX = screenwidth - minimapTileSize - 8;
        }
        int topLeftZ = 8;
        int pad = getMinimapPadding();

        if (sp.minimapBackground == MinimapBackground.MAP_BG) {
            int bgSize = minimapTileSize + (pad * 2);
            context.blit(RenderPipelines.GUI_TEXTURED, MAP_BG.get(), topLeftX - pad, topLeftZ - pad, 0, 0, bgSize,
                    bgSize, bgSize, bgSize);
        } else {
            int bgSize = minimapTileSize + (pad * 2);
            context.blit(RenderPipelines.GUI_TEXTURED, MENU_BG.get(), topLeftX - pad, topLeftZ - pad, 0, 0, bgSize,
                    bgSize, 16, 16);
        }

        if(isLoadingIn) {
            Font font = Minecraft.getInstance().font;
            float textScale = minimapTileSize < 80 ? 0.75f : 1.0f;
            float centerX = topLeftX + (minimapTileSize / 2.0f);
            float centerY = topLeftZ + (minimapTileSize / 2.0f);
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(textScale, textScale);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Loading map"), 0, (int) (-font.lineHeight * 1.2f), 0xFFFFFFFF);
            if(spinnerPart == 8) spinnerPart = 0;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Please Wait " + spinnerParts[spinnerPart]).withStyle(ChatFormatting.BOLD), 0, (int) (font.lineHeight * 0.4f), 0xFFFFFFFF);
            context.pose().popMatrix();
            if(tickAccumulator >= 10f) {
                spinnerPart++;
                tickAccumulator = 0;
            }
            return;
        }

        if(MapRenderUtils.isInQueue()) {
            Font font = Minecraft.getInstance().font;
            // don't let the text bust out of the tiny box if its < 80px
            float textScale = minimapTileSize < 80 ? 0.75f : 1.0f;
            float centerX = topLeftX + (minimapTileSize / 2.0f);
            float centerY = topLeftZ + (minimapTileSize / 2.0f);
            context.pose().pushMatrix();
            context.pose().translate(centerX, centerY);
            context.pose().scale(textScale, textScale);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("You're in..."), 0, (int) (-font.lineHeight * 1.8f), 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("The Queue").withStyle(ChatFormatting.BOLD), 0, (int) (-font.lineHeight * 0.7f), 0xFFFFFFFF);
            if(spinnerPart == 8) spinnerPart = 0;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("Please Wait " + spinnerParts[spinnerPart]).withStyle(ChatFormatting.BOLD), 0, (int) (font.lineHeight * 0.9f), 0xFFFFFFFF);
            context.pose().popMatrix();
            
            if(tickAccumulator >= 10f) {
                spinnerPart++;
                tickAccumulator = 0;
            }
            return;
        }

        String currentDimension = MapRenderUtils.getCurrentDimension();
        if (currentDimension == null) {
            Font font = Minecraft.getInstance().font;
            GraphicsHelper.drawCenteredString(context, font, Component.literal("No map tiles"), topLeftX + (minimapTileSize/ 2), topLeftZ, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("available for"), topLeftX + (minimapTileSize/ 2), topLeftZ + font.lineHeight, 0xFFFFFFFF);
            GraphicsHelper.drawCenteredString(context, font, Component.literal("this world!"), topLeftX + (minimapTileSize/ 2), topLeftZ + (font.lineHeight * 2), 0xFFFFFFFF);
            int textCoordinatesPos = topLeftX + (minimapTileSize / 2);
            int textCoordinatesY = topLeftZ + minimapTileSize + pad + 2;
            GraphicsHelper.drawCenteredString(context, font, String.format("%d, %d, %d", Minecraft.getInstance().player.blockPosition().getX(),
                Minecraft.getInstance().player.blockPosition().getY(), Minecraft.getInstance().player.blockPosition().getZ()), textCoordinatesPos, textCoordinatesY, 0xFFFFFFFF);
            return;
        }

        if(!this.lastDimension.equals(currentDimension)) {
            this.lastDimension = currentDimension;
            // Reset image cache
            resetTileImageCache();
        }
        boolean tooltipApplied = false;
        MapRenderUtils.updateLocalPlayer();
        Minecraft mc = Minecraft.getInstance();
        String currentDimensionPath = mc.level != null ? CompatUtils.getIdentifier(mc.level.dimension()).getPath() : "overworld";
        String mapDimension = MapRenderUtils.getCurrentDimension();

        double dimensionScale = MapRenderUtils.getDimensionScale(MapRenderUtils.currentWorld, currentDimensionPath, mapDimension);
        double x = MapRenderUtils.lastLocalPlayerX * dimensionScale;
        double z = MapRenderUtils.lastLocalPlayerZ * dimensionScale;
        // Calculate tile size from zoom
        int renderZoom = Math.min(8, zoomlevel);
        int renderTileSize = 1 << (17 - renderZoom);
        int drawSize = BASE_TILE_SIZE * (1 << (zoomlevel - renderZoom));
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) BASE_TILE_SIZE / tilesize;

        double worldLeft = x - ((minimapTileSize / 2.0) / scale);
        double worldTop = z - ((minimapTileSize / 2.0) / scale);
        double worldRight = x + ((minimapTileSize / 2.0) / scale);
        double worldBottom = z + ((minimapTileSize / 2.0) / scale);

        // Figure out which tiles to draw based on viewport bounds
        // Make sure negative tiles start at -1 not -0
        // If I'm stood at 0, 0 this tile will be -1_-1.png and be shifted by half the map box
        // If I'm stood at 256, 256 this tile will be 0_0.png and align with the top-left
        int topLeftTileX = MapRenderUtils.worldToTileCoordinate(worldLeft, renderTileSize);
        int topLeftTileZ = MapRenderUtils.worldToTileCoordinate(worldTop, renderTileSize);
        int bottomRightTileX = MapRenderUtils.worldToTileCoordinate(worldRight, renderTileSize) + 1;
        int bottomRightTileZ = MapRenderUtils.worldToTileCoordinate(worldBottom, renderTileSize) + 1;

        // Save ourselves requesting content we already have.
        if (this.lastTileX != topLeftTileX || this.lastTileZ != topLeftTileZ) {
            recalculateNetworks();
            this.lastTileX = topLeftTileX;
            this.lastTileZ = topLeftTileZ;

            // Add places in too
            int minx = topLeftTileX * renderTileSize;
            int maxx = bottomRightTileX * renderTileSize;
            int minz = topLeftTileZ * renderTileSize;
            int maxz = bottomRightTileZ * renderTileSize;
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(new URI(String.format("%s/fetch/places/%s/%d/%d/%d/%d", NetworkUtils.API_V1,
                        currentDimension, minx, maxx, minz, maxz)))
                    .GET().build();
                NetworkUtils.HTTP_CLIENT.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Gson gson = new Gson();
                            placesList = new ArrayList<PlaceFetch>(Arrays.asList(gson.fromJson(response.body(), PlaceFetch[].class)));
                        } else {
                            LogUtils.error("Failed to fetch places from PVC Mapper! Code: " + response.statusCode());
                        }
                    })
                    .exceptionally(e -> {
                        LogUtils.error("Failed to fetch places from PVC Mapper!", e);
                        return null;
                    });
            } catch (Exception e) {
                LogUtils.error("Failed to fetch places from PVC Mapper!", e);
            }

            hasNotBeenInitialisedYet = false;
        }

        double xOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldLeft, renderTileSize);
        double zOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldTop, renderTileSize);

        context.enableScissor(topLeftX, topLeftZ, topLeftX + minimapTileSize, topLeftZ + minimapTileSize);
        // Draw each tile to be visible
        for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
            for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                String url = MapRenderUtils.getTileUrl(sp.mapTileSource, currentDimension, sp.useDarkTiles, renderZoom, iX, iZ);
                ResIdentifier tile = TextureUtils.getCachedTexture(url);
                if (tile == null) {
                    TextureUtils.fetchImmediateRemoteTexture(url, (id) -> {});
                    tile = TextureUtils.blurredTile;
                }
                context.pose().pushMatrix();
                context.pose().translate(
                        (float) ((topLeftX - (xOffsetFromTileStart * scale)) + 
                                ((iX - topLeftTileX) * drawSize)),
                        (float) ((topLeftZ - (zOffsetFromTileStart * scale)) + 
                                ((iZ - topLeftTileZ) * drawSize))
                );
                // Draw the texture with a size of drawSizexdrawSize
                // renderLayer, texture, x, y, width, height, u, v, regionWidth, regionHeight,
                // textureWidth, textureHeight
                context.blit(
                        RenderPipelines.GUI_TEXTURED, tile.get(),
                        0, 0,
                        0, 0,
                        drawSize, drawSize,
                        drawSize, drawSize
                );
                context.pose().popMatrix();
            }
        }

        // Draw networks first
        if(this.lastX != mc.player.getBlockX() || this.lastZ != mc.player.getBlockZ()) {
            recalculateNetworks();
        }
        this.lastX = mc.player.getBlockX();
        this.lastZ = mc.player.getBlockZ();
        if(sp.showNetworks && !sp.hideMinimapNetworks) {
            for (int i = 0; i < transportNetwork.getSegments().size(); i++) {
                TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                MapRenderUtils.drawLine(context, (int)line.coords[0][0] + topLeftZ, (int)line.coords[0][1] + topLeftX, (int)line.coords[1][0] + topLeftZ, (int)line.coords[1][1] + topLeftX, line.colour);
            }
        }

        context.disableScissor();

        // Check for mouse hover over the minimap
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight();
        int mouseX = (int) mx;
        int mouseY = (int) my;
        boolean mouseIsInMap = false;
        if (mouseX > topLeftX && mouseX < (topLeftX + minimapTileSize) && mouseY > topLeftZ
                && mouseY < (topLeftZ + minimapTileSize))
            mouseIsInMap = true;

        int minX = (int) Math.floor(worldLeft);
        int minZ = (int) Math.floor(worldTop);
        int maxX = (int) Math.ceil(worldRight);
        int maxZ = (int) Math.ceil(worldBottom);
        if(sp.showPlaces) {
            for (int i = 0; i < placesList.size(); i++) {
                PlaceFetch place = placesList.get(i);
                int placeX = Integer.parseInt(place.x);
                int placeZ = Integer.parseInt(place.z);
                if ((placeX > minX && placeX < maxX) && (placeZ > minZ && placeZ < maxZ)) {
                    double offsetFromPlayerX = (x - placeX) * scale;
                    double offsetFromPlayerZ = (z - placeZ) * scale;
                    float translateX = (float) ((topLeftX + (minimapTileSize / 2.0)) - offsetFromPlayerX);
                    float translateZ = (float) ((topLeftZ + (minimapTileSize / 2.0)) - offsetFromPlayerZ);
                    MapRenderUtils.drawStaticMarker(context, translateX, translateZ, MapRenderUtils.getPlaceIcon(place.type));

                    // Render tooltip if being hovered
                    if (mouseIsInMap && !tooltipApplied) {
                        if (mouseX > translateX - 4 && mouseX < translateX + 4 && mouseY > translateZ - 4
                                && mouseY < translateZ + 4) {
                            tooltipApplied = true;
                            renderMinimapTooltip(
                                    context,
                                    List.of(
                                            String.format("%s (P%d): Type: %s", place.name, place.id, place.type),
                                            String.format("%s, %s in %s", place.x, place.z,
                                                    prettyDimensionName(place.dimension))));
                        }
                    }
                }
            }
        }

        
        ArrayList<PlayerFetch> playersList = pfu.getPlayers();
        if(sp.showPlayers) {
            // Now do the other players
            for (int i = 0; i < playersList.size(); i++) {
                PlayerFetch player = playersList.get(i);
                // If player is us, ignore. We know our whole life story already
                if(mc.player.getName().equals(Component.literal(player.name))) continue;
                // If player isn't in our space, ignore
                if (playerInCoords(minX, minZ, maxX, maxZ, player.world, player.x, player.z)) {
                    // Draw their marker - Sort rotation
                    double offsetFromPlayerX;
                    double offsetFromPlayerZ;
                    double playerDimensionScale = MapRenderUtils.getDimensionScale(player.world, currentDimension);
                    offsetFromPlayerX = (x - (player.x * playerDimensionScale)) * scale;
                    offsetFromPlayerZ = (z - (player.z * playerDimensionScale)) * scale;
                    ResIdentifier playerMarkerChoice = MapRenderUtils.getPlayerMarker(player.world);
                    float translateX = (float) ((topLeftX + (minimapTileSize / 2.0)) - offsetFromPlayerX);
                    float translateZ = (float) (topLeftZ + (minimapTileSize / 2.0) - offsetFromPlayerZ);
                    context.enableScissor(topLeftX - 4, topLeftZ - 4, topLeftX + minimapTileSize + 4, topLeftZ + minimapTileSize + 4);
                    float playerYawRad = (float) Math.toRadians(player.yaw - 180);
                    MapRenderUtils.drawPlayerMarker(context, translateX, translateZ, playerYawRad, playerMarkerChoice, MapRenderUtils.getPlayerMarkerTint(player.world));
                    context.disableScissor();
                    // Render tooltip if being hovered
                    if (!tooltipApplied && mouseIsInMap) {
                        if (mouseX > translateX - 4 && mouseX < translateX + 4 && mouseY > translateZ - 4 && mouseY < translateZ + 4) {
                            tooltipApplied = true;
                            getTooltipPlayer(player.uuid, player.name);
                            renderMinimapTooltipPlayer(context,
                                List.of(
                                    player.name,
                                    String.format("%d, %d, %d in %s", player.x, player.y, player.z, prettyDimensionName(player.world)),
                                    String.format("Health: %.1f, Armor: %.1f", player.health, player.armor),
                                    player.afksince != null && 
                                        ((Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000) > 2 
                                        ? String.format("AFK for %d mins", (Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000)
                                        : "Currently Active"
                                ), player.uuid
                            );
                        }
                    }

                }

            }
        }


        // Get player yaw
        float yawDeg = MapRenderUtils.lastLocalPlayerYaw;
        float yawRad = (float) Math.toRadians(yawDeg);
        float localPlayerPosX = topLeftX + (minimapTileSize / 2.0f);
        float localPlayerPosZ = topLeftZ + (minimapTileSize / 2.0f);
        MapRenderUtils.drawLocalPlayerMarker(context, localPlayerPosX, localPlayerPosZ, yawRad);

        if(!tooltipApplied && (sp.showPlaces || sp.showPlayers) && /*? if <26.2 {*/mc.screen/*?} else {*//*mc.gui.screen()*//*?}*/ instanceof ChatScreen) {
            renderMinimapTooltip(context, List.of("Hover over the minimap's icons", "to view player or place details!"));
        }

        int textCoordinatesPos = topLeftX + (minimapTileSize / 2);
        int textCoordinatesY = topLeftZ + minimapTileSize + pad + 2;
        // Add coordinate string beneath minimap
        GraphicsHelper.drawCenteredString(context, mc.font, String.format("%d, %d, %d", mc.player.blockPosition().getX(),
                mc.player.blockPosition().getY(), mc.player.blockPosition().getZ()), textCoordinatesPos, textCoordinatesY, 0xFFFFFFFF);

        // Directions stuff
        if(dp != null && dp.routeActive) {
            List<String> cont = List.of();
            if(/*? if <26.2 {*/mc.screen/*?} else {*//*mc.gui.screen()*//*?}*/ instanceof ChatScreen) {
                cont = List.of(
                    String.format("%s %s %s", dp.directionArrow, dp.directionInstruction, dp.directionArrow),
                    String.format("in %dm", dp.directionDistance),
                    "To stop: /mapper stop-directions"
                );
            } else {
                cont = List.of(
                    String.format("%s %s %s", dp.directionArrow, dp.directionInstruction, dp.directionArrow),
                    String.format("in %dm", dp.directionDistance)
                );
            }
            int maxSize = 0;
            for (int i = 0; i < cont.size(); i++) {
                int w = mc.font.width(cont.get(i));
                if (w > maxSize) {
                    maxSize = w;
                }
            }
            int tooltipX = -1000;
            if(sp.miniMapPos == MiniMapPositions.TOP_RIGHT) {
                tooltipX = topLeftX - pad - 5 - maxSize;
            } else if(sp.miniMapPos == MiniMapPositions.TOP_LEFT) {
                tooltipX = topLeftX + minimapTileSize + pad + 5;
            }
            MapRenderUtils.drawTooltipString(context, cont, tooltipX, 8);   
        }
    }



}


