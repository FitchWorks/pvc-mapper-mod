package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.net.URI;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Checkbox.Builder;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;

public class FullScreenMap extends Screen {
    private PlayerFetchUtils pfu;
    public boolean overlayOpen;
    public int overlayItemID;
    public String overlayItemType;
    public FeatureTypes overlayFeature;
    public ResIdentifier overlayImage;
    public String overlayImageStatus;
    public ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
        Thread.ofPlatform().daemon().name("pvc-mapper-fsm-zoom").factory()
    );
    private boolean isMouseDown = false;
    public SettingsProvider sp;

    public Network[] allNetworks = new Network[0];
    public static FullScreenMap createScreen(Component title, PlayerFetchUtils pfu, SettingsProvider sp) {
        FullScreenMap fsm = new FullScreenMap(title);
        fsm.pfu = pfu;
        fsm.sp = sp;
        fsm.zoomlevel = Math.min(15, sp.miniMapZoom); 

        pfu.fetchNetworksAsync().thenAccept(networks -> {
            fsm.allNetworks = networks;
        });

        return fsm;
    }


    public FullScreenMap(Component title) {
        super(title);
    }

    public void showToast(String title, String content) {
        CompatUtils.addToast(
            new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(title),
                Component.literal(content)
            )
        );
    }

    public void navToCoords(int x, int z) {
        if (viewedTileset != null) {
            viewedWorld = MapRenderUtils.getWorldName(viewedTileset);
        }
        updateDimensionButtons();
        centreOnCoords(x, z, false);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_ESCAPE) {
            if (renderContextMenu) {
                renderContextMenu = false;
                return true;
            }
            // TODO: Figure out how to exit/hide the Filter popup with keyboard once it's open
            if (overlayOpen)
                overlayOpen = false;
            else
                CompatUtils.setScreen(null);
                //this.onClose(); // closes the screen
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    // Screen positions
    private int topLeftX = 0;
    private int topLeftZ = 0;
    public int zoomlevel = 8;
    public int maxZoomLevel = 15;
    public int minZoomLevel = 1;

    public double x = 0;
    public double z = 0;
    
    // Smooth camera pan animation state
    private MapRenderUtils.CameraPan cameraPan = null;

    private void stopCameraPan() {
        if (cameraPan != null) {
            cameraPan.cancel();
            cameraPan = null;
        }
    }
    
    public int minimapTileSize = 120;


    private boolean drawSponsorTooltip = false;

    double lastMouseX = 0;
    double lastMouseY = 0;

    // Check for new tiles, usually when mouse dragging happens
    private void loadOnscreenTiles() {
        if (viewedTileset == null || MapRenderUtils.getWorldTilesets(viewedWorld).isEmpty()) return;
        int renderZoom = Math.min(8, zoomlevel);
        int renderTileSize = 1 << (17 - renderZoom);
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        double worldLeft = x;
        double worldTop = z;
        double worldRight = x + (this.width / scale);
        double worldBottom = z + (this.height / scale);
        String dimension = "" + viewedTileset;
        // Figure out tile no. at top left/bottom right
        int topLeftTileX = MapRenderUtils.worldToTileCoordinate(worldLeft, renderTileSize) - 1;
        int topLeftTileZ = MapRenderUtils.worldToTileCoordinate(worldTop, renderTileSize) - 1;
        int bottomRightTileX = MapRenderUtils.worldToTileCoordinate(worldRight, renderTileSize) + 1;
        int bottomRightTileZ = MapRenderUtils.worldToTileCoordinate(worldBottom, renderTileSize) + 1;

        int thisZoomLevel = zoomlevel;
        for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
            if(thisZoomLevel != zoomlevel && !viewedTileset.equals(dimension)) break;
            for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                if(thisZoomLevel != zoomlevel && !viewedTileset.equals(dimension)) break;
                
                String url = MapRenderUtils.getTileUrl(sp.mapTileSource, dimension, sp.useDarkTiles, renderZoom, iX, iZ);
                
                // Rely on TextureUtils to manage the cache and deduplicate pending fetches
                TextureUtils.fetchRemoteTexture(url, (id) -> {});
            }
        }
    }
    public ArrayList<ClaimMarkers> shownClaims = new ArrayList<ClaimMarkers>();
    public void resetClaims() {
        shownClaims = new ArrayList<ClaimMarkers>();
        // top reasons not to show claims
        if (!sp.showClaims || zoomlevel < 8 || !MapRenderUtils.isMondoMap(viewedTileset)) {
            return;
        }
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        // cast to int the floor for discrete boundaries & clean sharing
        shownClaims = pfu.getClaimsInBounds(viewedTileset, (int) Math.floor(x), (int) Math.floor(x + (this.width / scale)), (int) Math.floor(z),
                (int) Math.floor(z + ((this.height - bottomMapOffset) / scale)));
    }

    public FeatureFetch[] shownFeatures = new FeatureFetch[0];

    private boolean isChangingFeatures = false;
    public void resetFeatures() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        pfu.fetchFeaturesAsync(
                // cast to int & floor when u need discrete boundaries or clean sharing
                viewedTileset,
                (int) Math.floor(x),
                (int) Math.floor(x + (this.width / scale)),
                (int) Math.floor(z),
                (int) Math.floor(z + ((this.height - bottomMapOffset) / scale)))
            .thenAccept(features -> {
                // Perform bounds calculation on the local 'features' array first to prevent
                // concurrency crashes where the main thread reads half-updated arrays.
                for (int i = 0; i < features.length; i++) {
                    if(features[i].featureType.equals("area")) {
                        if(features[i].bounds == null) continue;
                        for (int bound = 0; bound < features[i].bounds.length; bound++) {
                            features[i].bounds[bound][0] = MapRenderUtils.metersToPixels(features[i].bounds[bound][0]);
                            features[i].bounds[bound][1] = MapRenderUtils.metersToPixels(features[i].bounds[bound][1]);
                        }
                    }
                }
                
                // Safely assign the final array to the global state on the main game thread.
                minecraft.execute(() -> {
                    isChangingFeatures = true;
                    shownFeatures = features;
                    isChangingFeatures = false;
                });
            });
        
        recalculateNetworks();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e) {
        stopCameraPan();
        isMouseDown = true;
        clearHoverState();
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        x -= d / scale;
        z -= e / scale;
        loadOnscreenTiles();
        return super.mouseDragged(mouseButtonEvent, d, e);
    }

    double hasMovedX;
    double hasMovedZ;

    boolean renderContextMenu = false;
    double contextMenuX;
    double contextMenuY;
    int contextMenuWorldX;
    int contextMenuWorldZ;
    ArrayList<String> contextMenuItems = new ArrayList<String>(List.of(
        "Copy Coords",
        "Copy Coords in Nether/T2",
        "Copy Coords + Dimension",
        "Copy Web Link",
        "Open on Web ↗",
        "Centre Map Here"
    ));
    String[] contextMenuValues = new String[contextMenuItems.size()];
    int bestContextMenuWidth = 0;


    @Override
    public boolean mouseReleased(MouseButtonEvent mbe) {
        isMouseDown = false;
        if(hasMovedX != x || hasMovedZ != z) {
            resetClaims();
            resetFeatures();
        } else {
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;

            if (mbe.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            // Check to see if a thing has been clicked
            for (int i = shownFeatures.length - 1; i >= 0; i--) {
                if(shownFeatures[i].id == 1) {
                    LogUtils.debug("X/Z: " + ((shownFeatures[i].x - x) * scale) + " / " + ((shownFeatures[i].z - z) * scale));
                }
                if(shownFeatures[i].featureType.equals("area")) {
                    if(sp.showAreas) {
                        int itemWidth = minecraft.font.width(shownFeatures[i].name);
                        if( mbe.x() > ((shownFeatures[i].x - x) * scale) - (itemWidth / 2) &&
                            mbe.x() < ((shownFeatures[i].x - x) * scale) + (itemWidth / 2) &&
                            mbe.y() > ((shownFeatures[i].z - z) * scale) - (minecraft.font.lineHeight / 2) &&
                            mbe.y() < ((shownFeatures[i].z - z) * scale) + (minecraft.font.lineHeight / 2)) {
                            if(mbe.hasControlDown()) {
                                CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, (int) shownFeatures[i].x, (int) shownFeatures[i].z, MapRenderUtils.prettyDimensionName(viewedWorld, viewedTileset != null ? MapRenderUtils.getDimensionType(viewedTileset) : MapRenderUtils.OVERWORLD)), false));
                            } else {
                                LogUtils.debug("Feature clicked: " + shownFeatures[i].id);
                                int index = i;
                                pfu.fetchAreaAsync(shownFeatures[index].id)
                                    .thenAccept(feature -> {
                                        overlayFeature = feature;
                                        overlayItemID = shownFeatures[index].id;
                                        overlayItemType = "area";
                                        overlayOpen = true;
                                        overlayImage = null;
                                        overlayImageStatus = "Loading...";
                                        if(overlayFeature.area.image != null) {
                                            TextureUtils.fetchImmediateRemoteTexture(overlayFeature.area.image, (id) -> {
                                                overlayImage = id;
                                                overlayImageStatus = "No image available";
                                            });
                                        } else {
                                            overlayImageStatus = "No image available";
                                        }
                                    });
                            }

                        }
                    }
                } else {
                    if( mbe.x() > ((shownFeatures[i].x - x) * scale) - 4 &&
                        mbe.x() < ((shownFeatures[i].x - x) * scale) + 4 &&
                        mbe.y() > ((shownFeatures[i].z - z) * scale) - 4 &&
                        mbe.y() < ((shownFeatures[i].z - z) * scale) + 4) {
                        int index = i;
                        switch (shownFeatures[i].featureType) {
                            case "place":
                                if(mbe.hasControlDown() && sp.showPlaces) {
                                    CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", shownFeatures[i].name, (int) shownFeatures[i].x, (int) shownFeatures[i].z, MapRenderUtils.prettyDimensionName(viewedWorld, viewedTileset != null ? MapRenderUtils.getDimensionType(viewedTileset) : MapRenderUtils.OVERWORLD)), false));
                                } else if (sp.showPlaces) {
                                    pfu.fetchPlaceAsync(shownFeatures[index].id)
                                        .thenAccept(feature -> {
                                            overlayFeature = feature;
                                            overlayItemID = shownFeatures[index].id;
                                            overlayItemType = "place";
                                            overlayOpen = true;
                                            overlayImage = null;
                                            overlayImageStatus = "Loading...";
                                            if(overlayFeature.place.images != null) {
                                                TextureUtils.fetchImmediateRemoteTexture(overlayFeature.place.images, (id) -> {
                                                    overlayImage = id;
                                                    overlayImageStatus = "No image available";
                                                });
                                            } else {
                                                overlayImageStatus = "No image available";
                                            }
                                        });
                                }
                                break;

                            case "portal":
                                if(mbe.hasControlDown()) {
                                    CompatUtils.setScreen(minecraft, new ChatScreen(String.format("%s: %d, %d in %s", pfu.getPortalPrettyName(shownFeatures[i].type), (int) shownFeatures[i].x, (int) shownFeatures[i].z, MapRenderUtils.prettyDimensionName(viewedWorld, viewedTileset != null ? MapRenderUtils.getDimensionType(viewedTileset) : MapRenderUtils.OVERWORLD)), false));
                                }
                                break;
                            default:
                                showToast("Feature view error", "Unknown or unsupported feature type '" + shownFeatures[i].featureType + "'");
                                break;
                        }
                        break;
                    }
                }
            }
            }
        }
        mouseMoved(mbe.x(), mbe.y());
        return super.mouseReleased(mbe);
    }

    @Override
    public boolean mouseScrolled(double a, double b, double c, double d) {
        stopCameraPan();
        int scroll = (int) Math.signum(d);
        if (scroll == 0) return super.mouseScrolled(a, b, c, d);
        // Get cursor position in world offset
        int oldtilesize = 1 << (17 - zoomlevel);
        double oldscale = (double) minimapTileSize / oldtilesize;
        double oldCursorWorldOffsetX = a / oldscale;
        double oldCursorWorldOffsetZ = b / oldscale;
        // Move to new zoom level
        zoomlevel += scroll;
        zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel));
        // Set position to new x/z
        int newtilesize = 1 << (17 - zoomlevel);
        double newscale = (double) minimapTileSize / newtilesize;
        double newCursorWorldOffsetX = a / newscale;
        double newCursorWorldOffsetZ = b / newscale;
        x -= (newCursorWorldOffsetX - oldCursorWorldOffsetX);
        z -= (newCursorWorldOffsetZ - oldCursorWorldOffsetZ);
        int newZoomLevel = zoomlevel;
        executor.schedule(() -> {
            if(newZoomLevel == zoomlevel) {
                recalculateNetworks();
                loadOnscreenTiles();
                resetClaims();
            }
        }, 250, TimeUnit.MILLISECONDS);
        return super.mouseScrolled(a, b, c, d);
    }

    private int hoveredPlaceIndex;
    private int hoveredClaimIndex;
    private PlayerFetch hoveredPlayer = null;

    private boolean isCursorOverContextMenu(double mouseX, double mouseY) {
        return renderContextMenu &&
            mouseX >= contextMenuX && mouseX <= contextMenuX + bestContextMenuWidth &&
            mouseY >= contextMenuY && mouseY < contextMenuY + (minecraft.font.lineHeight * contextMenuItems.size());
    }

    private void clearHoverState() {
        hoveredPlaceIndex = -1;
        hoveredClaimIndex = -1;
        drawSponsorTooltip = false;
        hoveredPlayer = null;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Always clear active hover states first so starting a drag or entering the menu does not leave a stuck tooltip
        clearHoverState();

        // While dragging the map or hovering the context menu, freeze hover checks and keep grabbed coordinates static
        if (isMouseDown || isCursorOverContextMenu(mouseX, mouseY)) {
            super.mouseMoved(mouseX, mouseY);
            return;
        }

        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        if (sponsorBanner != null) {
            if (mouseY > (this.height - 28) && mouseY < (this.height - 3) && mouseX > 3 && mouseX < 196) {
                drawSponsorTooltip = true;
            }
        }

        // If mouse is in
        if (mouseY < this.height - bottomMapOffset) {
            for (int i = shownFeatures.length - 1; i >= 0; i--) {
                if (shownFeatures[i].featureType.equals("area")) {
                    double featureX = (minecraft.font.width(shownFeatures[i].name) * 0.5) / 2;
                    double featureY = (minecraft.font.lineHeight * 0.5) / 2;
                    if (mouseX > ((shownFeatures[i].x - x) * scale) - featureX &&
                            mouseX < ((shownFeatures[i].x - x) * scale) + featureX &&
                            mouseY > ((shownFeatures[i].z - z) * scale) - featureY &&
                            mouseY < ((shownFeatures[i].z - z) * scale) + featureY) {
                        hoveredPlaceIndex = i;
                        break;
                    }
                } else {
                    if (mouseX > ((shownFeatures[i].x - x) * scale) - 4
                            && mouseX < ((shownFeatures[i].x - x) * scale) + 4 &&
                            mouseY > ((shownFeatures[i].z - z) * scale) - 4
                            && mouseY < ((shownFeatures[i].z - z) * scale) + 4) {
                        hoveredPlaceIndex = i;
                        break;
                    }
                }
            }
            if(hoveredPlaceIndex == -1 && zoomlevel >= 8 && sp.showClaims) {
                for (int i = shownClaims.size() - 1; i >= 0; i--) {
                    ClaimMarkers claim = shownClaims.get(i);
                    if(claim.type.equals("polgyon")) continue;
                    if(mouseX > (claim.points[0].x - x) * scale &&
                        mouseX < (claim.points[1].x - x) * scale &&
                        mouseY > (claim.points[0].z - z) * scale &&
                        mouseY < (claim.points[1].z - z) * scale
                    ) {
                        hoveredClaimIndex = i;
                        break;
                    }
                }

            }

            // Set text to draw coords, round (floor) now
            currentLocationCoordinates = Component.literal(String.format("%d, %d", (int) Math.floor(x + (mouseX / scale)), (int) Math.floor(z + (mouseY / scale))));
        } else {
            currentLocationCoordinates = Component.literal("--, --");
        }

        super.mouseMoved(mouseX, mouseY);
    }

    public MutableComponent currentLocationCoordinates = Component.literal("--, --");

    @Override
    public boolean mouseClicked(MouseButtonEvent mbe, boolean bl) {
        stopCameraPan();
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        if (renderContextMenu) {
            if (mbe.button() == InputConstants.MOUSE_BUTTON_LEFT && isCursorOverContextMenu(mbe.x(), mbe.y())) {
                int clickedIndex = (int) Math.floor((mbe.y() - contextMenuY) / minecraft.font.lineHeight);
                if (clickedIndex >= 0 && clickedIndex < contextMenuItems.size()) {
                    if (clickedIndex < 3) {
                        if (contextMenuValues[clickedIndex] != null) {
                            try {
                                minecraft.keyboardHandler.setClipboard(contextMenuValues[clickedIndex]);
                                LogUtils.debug("Copied to clipboard: " + contextMenuValues[clickedIndex]);
                            } catch (Exception e) {
                                LogUtils.error("Failed to copy to clipboard", e);
                            }
                            // TODO: Add feedback confirming something copied to clipboard
                        }
                    } else if (clickedIndex == 3) {
                        try {
                            String url = contextMenuValues[3] != null ? contextMenuValues[3] : String.format("%s?x=%d&z=%d&dimension=%s", NetworkUtils.BASE_URL, contextMenuWorldX, contextMenuWorldZ, viewedTileset != null ? viewedTileset : "minecraft_overworld");
                            minecraft.keyboardHandler.setClipboard(url);
                            LogUtils.debug("Copied web link to clipboard: " + url);
                        } catch (Exception e) {
                            LogUtils.error("Failed to copy web link to clipboard", e);
                        }
                        // TODO: Add feedback confirming something copied to clipboard
                    } else if (clickedIndex == 4) {
                        ConfirmLinkScreen.confirmLinkNow(null, URI.create(String.format("%s?x=%d&z=%d&dimension=%s", NetworkUtils.BASE_URL, contextMenuWorldX, contextMenuWorldZ, viewedTileset != null ? viewedTileset : "minecraft_overworld")));
                    } else if (clickedIndex == 5) {
                        centreOnCoords(contextMenuWorldX, contextMenuWorldZ, true);
                    }
                }
                renderContextMenu = false;
                return true;
            }
            // Dismiss context menu on outside click and fall through as if menu wasn't open
            renderContextMenu = false;
        }

        // Context menu opens on right-click.
        // Note to self: mbe.isRight() is false no matter what
        // Why is it like this!? (Note to Larry: mbe.isRight() checks the right arrow key)
        if (mbe.button() == InputConstants.MOUSE_BUTTON_RIGHT && mbe.y() < this.height - bottomMapOffset) {


            bestContextMenuWidth = 0;
            contextMenuX = mbe.x();
            contextMenuY = mbe.y();
            // cast to int for discrete boundaries & clean sharing
            contextMenuWorldX = (int) Math.floor((contextMenuX / scale) + x);
            contextMenuWorldZ = (int) Math.floor((contextMenuY / scale) + z);
            MapRenderUtils.LinkedDimension linked = MapRenderUtils.getLinkedDimension(viewedTileset);
            contextMenuValues[0] = String.format("%d, %d", contextMenuWorldX, contextMenuWorldZ);
            contextMenuItems.set(0, "Copy: " + contextMenuValues[0]);
            if (linked != null) {
                int linkedX = (int) Math.floor(contextMenuWorldX * linked.scale());
                int linkedZ = (int) Math.floor(contextMenuWorldZ * linked.scale());
                contextMenuValues[1] = String.format("%d, %d in %s", linkedX, linkedZ, linked.displayName());
                contextMenuItems.set(1, "Copy: " + contextMenuValues[1]);
            } else {
                contextMenuValues[1] = null;
                contextMenuItems.set(1, "No linked dimension");
            }
            String dimType = viewedTileset != null ? MapRenderUtils.getDimensionType(viewedTileset) : MapRenderUtils.OVERWORLD;
            contextMenuValues[2] = String.format("%d, %d in %s", contextMenuWorldX, contextMenuWorldZ, MapRenderUtils.prettyDimensionName(viewedWorld, dimType));
            contextMenuItems.set(2, "Copy: " + contextMenuValues[2]);
            contextMenuValues[3] = String.format("%s?x=%d&z=%d&dimension=%s", NetworkUtils.BASE_URL, contextMenuWorldX, contextMenuWorldZ, viewedTileset != null ? viewedTileset : "minecraft_overworld");
            for (int i = 0; i < contextMenuItems.size(); i++) {
                bestContextMenuWidth = Math.max(bestContextMenuWidth, minecraft.font.width(contextMenuItems.get(i))) + 2;
            }
            if (contextMenuX + bestContextMenuWidth > this.width - 5) {
                contextMenuX = Math.max(5, this.width - bestContextMenuWidth - 5);
            }
            if (contextMenuY + (contextMenuItems.size() * minecraft.font.lineHeight) > this.height - bottomMapOffset) {
                contextMenuY = Math.max(5, this.height - bottomMapOffset - (contextMenuItems.size() * minecraft.font.lineHeight) - 2);
            }
            renderContextMenu = true;
            return true;
        }

        isMouseDown = true;
        hasMovedX = x;
        hasMovedZ = z;
        if (mbe.button() == InputConstants.MOUSE_BUTTON_LEFT && mbe.y() < this.height - bottomMapOffset) {


            currentLocationCoordinates = Component.literal(String.format("%d, %d", (int) Math.floor(x + (mbe.x() / scale)), (int) Math.floor(z + (mbe.y() / scale))));
        }
        if (mbe.button() == InputConstants.MOUSE_BUTTON_LEFT && mbe.y() > (this.height - 28) && mbe.y() < (this.height - 3) && mbe.x() > 3 && mbe.x() < 196) {
            ConfirmLinkScreen.confirmLinkNow(null, URI.create(sponsorURLString));
        }
        return super.mouseClicked(mbe, bl);
    }

    private String hoverPlayerName;
    private ResIdentifier hoverPlayerFace = PlayerSkinHelper.DEFAULT_SKIN;

    private void getTooltipPlayer(String uuid, String name) {
        PlayerSkinHelper.fetchSkin(uuid, "FullScreenMap", (skin) -> {
            hoverPlayerFace = skin;
        });
    }


    public void drawPlayerTooltip(/*? if <26.1 {*/GuiGraphics/*?} else {*//*GuiGraphicsExtractor*//*?}*/ context, PlayerFetch player, int x, int y) {

        List<MutableComponent> content = List.of(
                Component.literal(player.name).withStyle(ChatFormatting.BOLD),
                Component.literal(player.x + ", " + player.z + " - " + pfu.prettyDimensionName(player.world)),
                Component.literal("Health: " + (player.health / 2) + "/10 - Armor: " + (player.armor / 2) + "/10"),
                player.afksince != null && 
                    ((Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000) > 2 
                    ? Component.literal(String.format("AFK for %d mins", (Instant.now().toEpochMilli() - Instant.parse(player.afksince).toEpochMilli()) / 60000)) 
                    : Component.literal("Currently Active")
            );
        
        MapRenderUtils.drawTooltipComponent(context, content, x + TooltipRenderUtil.PADDING_LEFT, y + TooltipRenderUtil.PADDING_TOP);
        if (hoverPlayerName != player.name) {
            getTooltipPlayer(player.uuid, player.name);
            hoverPlayerName = player.name;
        }

        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace.get(),
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 8, 8, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, hoverPlayerFace.get(),
                x + TooltipRenderUtil.PADDING_LEFT + minecraft.font.width(content.get(0)) + 1,
                y + TooltipRenderUtil.PADDING_TOP, 40, 8, 8, 8, 64, 64);

        int rgbout = MapRenderUtils.uuidToPlayerColor(player.uuid);
        context.blit(RenderPipelines.GUI_TEXTURED, MapRenderUtils.LOCATOR_DOT.get(),
                x + Minecraft.getInstance().font.width(content.get(0)) + 13,
                y + TooltipRenderUtil.PADDING_TOP, 0, 0, 8, 8, 9, 9, rgbout);
    }

    // Improved drawLine

    private int bottomMapOffset = 31;

    public ResIdentifier sponsorBanner;
    public List<MutableComponent> sponsorHoverText;
    public String sponsorURLString;

    private final ResIdentifier searchIcon = ResIdentifier.of("minecraft",
            "textures/gui/sprites/icon/search.png");
    private final ResIdentifier settingsIcon = ResIdentifier.of("pvcmappermod",
            "textures/gui/settings.png");
    private final ResIdentifier compassIcon = ResIdentifier.of("minecraft",
            "textures/item/compass_19.png");
    private final ResIdentifier hopperIcon = ResIdentifier.of("minecraft",
            "textures/item/hopper.png");

    private final TransportNetwork transportNetwork = new TransportNetwork();

    public void recalculateNetworks() {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;

        transportNetwork.recalculate(allNetworks, viewedTileset, zoomlevel, scale, x, z, this.width, this.height - bottomMapOffset, "FullScreenMap");
    }

    public String viewedTileset = "minecraft_overworld";

    public boolean showFilters = false;

    double lastX = 0;
    double lastZ = 0;

    //? if <26.1 {
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
    //?} else {
    /*@Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {*///?}
        if (overlayOpen && overlayFeature != null) {
            GraphicsHelper.drawString(context, minecraft.font, Component.literal("Press ESC to go back").withStyle(ChatFormatting.ITALIC), 5, 5, 0xFF555555);
            if(overlayItemType.equals("place")) {
                // Title
                GraphicsHelper.drawCenteredString(context, minecraft.font, Component.literal(overlayFeature.place.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                // Dividing line
                GraphicsHelper.vLine(context, this.width / 2, 60, this.height - 20, 0xFF636363);
                // Description
                List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.place.description), (this.width / 2) - 20 );
                int descHeight = lines.size() * minecraft.font.lineHeight;
                for (int i = 0; i < lines.size(); i++) {
                    GraphicsHelper.drawString(context, minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                }
                
                // Location
                context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/link.png").get(), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                GraphicsHelper.drawString(context, minecraft.font, String.format("%s, %s in %s", overlayFeature.place.x, overlayFeature.place.z,pfu.prettyDimensionName(overlayFeature.place.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
            
                // Added by
                context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/accessibility.png").get(), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                GraphicsHelper.drawString(context, minecraft.font, String.format("Added by %s", overlayFeature.place.addedBy.name), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                // Wiki Link
                if(overlayFeature.place.wiki != null && overlayFeature.place.wiki.length() > 1) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/toast/social_interactions.png").get(), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, overlayFeature.place.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                }

                int currentHeight = 83 + descHeight + (minecraft.font.lineHeight * 3);

                // Features
                if(overlayFeature.place.features != null) {
                    if(overlayFeature.place.features.Public) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/new_realm.png").get(), (this.width/2) + 8, currentHeight, 0, 0, 24, 12, 24, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Made for public use", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.echest) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/item/ender_eye.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Has ender chest access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.portal) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/block/nether_portal.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 384);
                        GraphicsHelper.drawString(context, minecraft.font, "Has nether portal access nearby", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                    if(overlayFeature.place.features.historical) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/item/clock_00.png").get(), (this.width/2) + 14, currentHeight, 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, "Historical Place", (this.width / 2) + 32, currentHeight + 2, 0xFFFFFFFF);
                        currentHeight += minecraft.font.lineHeight + 5;
                    }
                }

                context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                if(overlayImage != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, overlayImage.get(), this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                } else {
                    GraphicsHelper.drawCenteredString(context, minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                }


            } else if(overlayItemType.equals("area")) {
                try {
                    // Title
                    GraphicsHelper.drawCenteredString(context, minecraft.font, Component.literal(overlayFeature.area.name).withStyle(ChatFormatting.BOLD), this.width / 2, 40, 0xFFFFFFFF);
                    // Dividing line
                    GraphicsHelper.vLine(context, this.width / 2, 60, this.height - 20, 0xFF636363);
                    // Description
                    List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(overlayFeature.area.description), (this.width / 2) - 20 );
                    int descHeight = lines.size() * minecraft.font.lineHeight;
                    for (int i = 0; i < lines.size(); i++) {
                        GraphicsHelper.drawString(context, minecraft.font, lines.get(i), (this.width / 2) + 10, 63 + (i * minecraft.font.lineHeight), 0xFFFFFFFF);
                    }

                    // Location
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/link.png").get(), (this.width/2) + 8, 66 + descHeight, 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, String.format("%s, %s in %s", overlayFeature.area.x, overlayFeature.area.z, pfu.prettyDimensionName(overlayFeature.area.dimension)), (this.width / 2) + 22, 68 + descHeight, 0xFFFFFFFF);
                
                    // Added by
                    context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/icon/accessibility.png").get(), (this.width/2) + 8, 72 + descHeight + minecraft.font.lineHeight, 0, 0, 12, 12, 12, 12);
                    GraphicsHelper.drawString(context, minecraft.font, String.format("Added by %s", overlayFeature.area.addedBy.username), (this.width / 2) + 22, 74 + descHeight + minecraft.font.lineHeight, 0xFFFFFFFF);

                    // Wiki Link
                    if(overlayFeature.area.wiki != null && overlayFeature.area.wiki.length() > 1) {
                        context.blit(RenderPipelines.GUI_TEXTURED, ResIdentifier.of("minecraft", "textures/gui/sprites/toast/social_interactions.png").get(), (this.width/2) + 8, 76 + descHeight + (minecraft.font.lineHeight * 2), 0, 0, 12, 12, 12, 12);
                        GraphicsHelper.drawString(context, minecraft.font, overlayFeature.area.wiki, (this.width / 2) + 22, 78 + descHeight + (minecraft.font.lineHeight * 2), 0xFFFFFFFF);
                    }

                    context.fill(this.width / 6, 63, (this.width / 2) - 10, 183, 0x70000000);

                    if(overlayImage != null) {
                        context.blit(RenderPipelines.GUI_TEXTURED, overlayImage.get(), this.width / 6, 63, 0, 0, (this.width / 2) - (this.width / 6) - 10, 120, (this.width / 2) - (this.width / 6) - 10, 120);
                    } else {
                        GraphicsHelper.drawCenteredString(context, minecraft.font, overlayImageStatus, (((this.width / 2) - 10 - (this.width / 6)) / 2) + (this.width / 6), 121, 0xFFFFFFFF);
                    }
                } catch(Exception e) {
                    LogUtils.debug("[PVC Mapper Mod] Unable to write area details to screen renderererer.");
                }
            }
        } else {
            // swoosh
            if (cameraPan != null) {
                if (cameraPan.isFinished()) {
                    x = cameraPan.getTargetX();
                    z = cameraPan.getTargetZ();
                    cameraPan = null;
                    loadOnscreenTiles();
                    resetFeatures();
                    resetClaims();
                } else {
                    // don't load tiles during pan, it can overload
                    x = cameraPan.getX();
                    z = cameraPan.getZ();
                }
            }

            int renderZoom = Math.min(8, zoomlevel);
            int renderTileSize = 1 << (17 - renderZoom);
            int drawSize = minimapTileSize * (1 << (zoomlevel - renderZoom));
            
            int tilesize = 1 << (17 - zoomlevel);
            double scale = (double) minimapTileSize / tilesize;

            double worldLeft = x;
            double worldTop = z;
            double worldRight = x + (this.width / scale);
            double worldBottom = z + ((this.height - bottomMapOffset) / scale);

            // Figure out tile no. at top left/bottom right
            int topLeftTileX = MapRenderUtils.worldToTileCoordinate(worldLeft, renderTileSize);
            int topLeftTileZ = MapRenderUtils.worldToTileCoordinate(worldTop, renderTileSize);
            int bottomRightTileX = MapRenderUtils.worldToTileCoordinate(worldRight, renderTileSize) + 1;
            int bottomRightTileZ = MapRenderUtils.worldToTileCoordinate(worldBottom, renderTileSize) + 1;

            // Iterate thru visible tiles
            double xOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldLeft, renderTileSize);
            double zOffsetFromTileStart = MapRenderUtils.worldToLocalTileCoordinate(worldTop, renderTileSize);
            context.scissorStack.push(new ScreenRectangle(0, 0, this.width, this.height - bottomMapOffset));
            if (viewedTileset == null || MapRenderUtils.getWorldTilesets(viewedWorld).isEmpty()) {
                GraphicsHelper.drawCenteredString(context, minecraft.font,
                    Component.literal("No map tiles available for " + viewedWorld),
                    this.width / 2, (this.height - bottomMapOffset) / 2 - 10, 0xFFFFFFFF);
                GraphicsHelper.drawCenteredString(context, minecraft.font,
                    Component.literal("Switch worlds or check back later!"),
                    this.width / 2, (this.height - bottomMapOffset) / 2 + 6, 0xFFAAAAAA);
            } else {
                for (int iX = topLeftTileX; iX < bottomRightTileX; iX++) {
                    for (int iZ = topLeftTileZ; iZ < bottomRightTileZ; iZ++) {
                        String url = MapRenderUtils.getTileUrl(sp.mapTileSource, viewedTileset, sp.useDarkTiles, renderZoom, iX, iZ);
                        ResIdentifier tile = TextureUtils.getCachedTexture(url);
                        if (tile == null)
                            tile = TextureUtils.blurredTile;
                        context.pose().pushMatrix();
                        context.pose().translate(
                                (float) ((topLeftX - (xOffsetFromTileStart * scale)) + // The top left GUI pos - how far
                                                                                       // from the start of the tile
                                        ((iX - topLeftTileX) * drawSize)), // + How many tiles along we are
                                (float) ((topLeftZ - (zOffsetFromTileStart * scale)) + // All the same
                                        ((iZ - topLeftTileZ) * drawSize)) // For the Z axis
                        );
                        context.blit(
                                RenderPipelines.GUI_TEXTURED, tile.get(), // Render tile with the following x pos:
                                0, 0,
                                0, 0, // u/v
                                drawSize, drawSize, // width/height
                                drawSize, drawSize // texturewidth/textureheight
                        );
                        context.pose().popMatrix();
                    }
                }
            }

            // Draw networks first
            if(sp.showNetworks) {
                if(this.lastX != this.x || this.lastZ != this.z) {
                    recalculateNetworks();
                }
                this.lastX = this.x;
                this.lastZ = this.z;
                for (int i = 0; i < transportNetwork.getSegments().size(); i++) {
                    TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                    MapRenderUtils.drawLine(context, (int)line.coords[0][0], (int)line.coords[0][1], (int)line.coords[1][0], (int)line.coords[1][1], line.colour);
                }

                int networkLinePadding = 75;

                // Draw street names on top
                if(zoomlevel > 9) {
                    for (int i = 0; i<transportNetwork.getSegments().size();i++) {
                        TransportNetwork.Segment line = transportNetwork.getSegments().get(i);
                        int nameLength = minecraft.font.width(line.streetName) / 2;
                        double[][] coords = line.coords;

                        // Figure out how many we can fit along, with padding on either side.
                        double lineLength = Math.sqrt( 
                                Math.pow(
                                    Math.max(coords[0][1], coords[1][1]) - Math.min(coords[1][1], coords[0][1]), 
                                (double)(2)) + Math.pow(
                                    Math.max(coords[1][0], coords[0][0]) - Math.min(coords[1][0], coords[0][0]),
                                (double)(2))
                            );
                        int numberToDraw = (int)Math.floor(lineLength / (nameLength + (networkLinePadding) / scale));
                            
                        while(numberToDraw > 0) {
                            context.pose().pushMatrix();
                            context.pose().translate((float) (coords[0][0]), (float) (coords[0][1]));
                            context.pose().rotate((float)Math.toRadians(line.lineBearing));
                            if(numberToDraw * (networkLinePadding + (line.nameWidth / 2)) > lineLength) {
                                context.pose().popMatrix();
                                numberToDraw -= 1;
                                continue;
                            }
                            context.pose().translate((float)(numberToDraw * (networkLinePadding + (line.nameWidth / 2))), 0);
                            //context.pose().rotate(-(float)Math.toRadians(line.lineBearing));
                            if(line.lineBearing > 90 && line.lineBearing < 270) context.pose().rotate((float)Math.toRadians(-180));
                            context.pose().scale((float) 0.5, (float) 0.5);
                            context.fill(-(line.nameWidth / 2) - 2, -2, (line.nameWidth / 2) + 2, minecraft.font.lineHeight + 2, 0x80000000);
                            GraphicsHelper.drawCenteredString(context, minecraft.font, line.streetName, 0, 0, 0xFFFFFFFF);
                            context.pose().popMatrix();
                            numberToDraw-=1;
                        }
                    }
                }
            }
            // Draw claims
            if(sp.showClaims && zoomlevel >= 8) {
                for (int i = 0; i < shownClaims.size(); i++) {
                    ClaimMarkers claim = shownClaims.get(i);
                    if (claim.type.equals("rectangle")) {
                        context.pose().pushMatrix();
                        context.pose().translate((float) ((claim.points[0].x - x) * scale),
                                (float) ((claim.points[0].z - z) * scale));
                        context.fill(0, 0, (int) ((claim.points[1].x - claim.points[0].x) * scale),
                                (int) ((claim.points[1].z - claim.points[0].z) * scale),
                                (int) Long.parseLong(String.format("%02X%s", Math.round(claim.fillOpacity * 255f),
                                        claim.fillColor.substring(1)), 16));

                        // Draw outlines
                        int outlineColor = (int) Long.parseLong("ff" + claim.color.substring(1), 16);
                        int width = (int) ((claim.points[1].x - claim.points[0].x) * scale);
                        int height = (int) ((claim.points[1].z - claim.points[0].z) * scale);
                        // Left
                        GraphicsHelper.vLine(context, 0, 0, height, outlineColor);
                        // Right
                        GraphicsHelper.vLine(context, width, 0, height, outlineColor);
                        // Top
                        GraphicsHelper.hLine(context, 0, width, 0, outlineColor);
                        // Bottom
                        GraphicsHelper.hLine(context, 0, width, height, outlineColor);
                        context.pose().popMatrix();
                        // context.submitOutline((int)((claim.points[0].x - x) * scale),
                        // (int)((claim.points[0].z - z) * scale), (int)((claim.points[1].x -
                        // claim.points[0].x) * scale) + 1, (int)((claim.points[1].z -
                        // claim.points[0].z) * scale) + 1, (int) Long.parseLong("ff" +
                        // claim.color.substring(1), 16));
                    }
                }
            }

            // Draw area bounds on hover before place labels
            if(sp.showAreas) {
                try {
                    if(!isMouseDown && !isChangingFeatures && hoveredPlaceIndex != -1 && hoveredPlaceIndex < shownFeatures.length && 
                        shownFeatures[hoveredPlaceIndex].featureType.equals("area") &&
                        shownFeatures[hoveredPlaceIndex].bounds != null) { 
                        int boundlength = shownFeatures[hoveredPlaceIndex].bounds.length;
                        for (int bound = 0; bound < boundlength  - 1; bound++) {
                            if( shownFeatures[hoveredPlaceIndex].bounds != null &&
                                shownFeatures[hoveredPlaceIndex].bounds[bound].length == 2
                            ) MapRenderUtils.drawLine(context,
                                (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][1] - x)*scale),
                                (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound][0] - z)*scale),
                                (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][1] - x)*scale),
                                (int) ((shownFeatures[hoveredPlaceIndex].bounds[bound + 1][0] - z)*scale),
                                0xFFFF0000
                            );
                        }
                        // Draw one to connect it back up too
                        if(shownFeatures[hoveredPlaceIndex].bounds.length == boundlength) MapRenderUtils.drawLine(context,
                            (int) ((shownFeatures[hoveredPlaceIndex].bounds[boundlength - 1][1] - x)*scale),
                            (int) ((shownFeatures[hoveredPlaceIndex].bounds[boundlength - 1][0] - z)*scale),
                            (int) ((shownFeatures[hoveredPlaceIndex].bounds[0][1] - x)*scale),
                            (int) ((shownFeatures[hoveredPlaceIndex].bounds[0][0] - z)*scale),
                            0xFFFF0000
                        );
                    } 
                } catch(Exception e) {
                    // I don't care, I'm losing it with the errors in this aaAAa-
                }
            }

            // Draw places
            for (int i = 0; i < shownFeatures.length; i++) {
                FeatureFetch feature = shownFeatures[i];
                if (feature.featureType.equals("place") && sp.showPlaces) {
                    MapRenderUtils.drawStaticMarker(context, (float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale),
                        pfu.getPlaceIcon(feature.type));
                } else if (feature.featureType.equals("area") && sp.showAreas) {
                    context.pose().pushMatrix();
                    context.pose().translate((float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale));
                    context.pose().scale((float) 0.5, (float) 0.5);
                    context.fill(-(minecraft.font.width(feature.name) / 2) - 2, -2,
                            (minecraft.font.width(feature.name) / 2) + 2, minecraft.font.lineHeight + 2, 0x80000000);
                    GraphicsHelper.drawCenteredString(context, minecraft.font, feature.name, 0, 0, 0xFFFFFFFF);
                    context.pose().popMatrix();
                } else if (feature.featureType.equals("portal")) { // TODO: Show portals filter
                    MapRenderUtils.drawStaticMarker(context, (float) ((feature.x - x) * scale), (float) ((feature.z - z) * scale),
                        pfu.getPortalIcon(feature.type));
                }
            }

            // Draw players
            if (!isMouseDown) {
                hoveredPlayer = null;
            }
            if (sp.showPlayers) {
                ArrayList<PlayerFetch> playersList = pfu.getPlayers();
                String localPlayerName = minecraft.player != null ? minecraft.player.getGameProfile().name() : "";
                PlayerFetch localPlayerFetch = null;
                for (int i = 0; i < playersList.size(); i++) {
                    PlayerFetch player = playersList.get(i);
                    // Skip local player and render them locally
                    if (player.name != null && player.name.equalsIgnoreCase(localPlayerName)) {
                        localPlayerFetch = player;
                        continue;
                    }

                    // Only render players who are in the world currently being viewed
                    String playerWorld = MapRenderUtils.getWorldName(player.world);
                    if (!viewedWorld.equals(playerWorld)) continue;

                    // If the world has no tilesets (e.g. Cielo), there is no canvas to project onto
                    if (viewedTileset == null) continue;

                    // Calculate the player's Effective X/Z coordinates for the map's current dimension.
                    // This must be done BEFORE checking if they are within the screen bounds (worldLeft/worldRight),
                    // otherwise players in the Nether will be skipped when zoomed in on the Overworld.
                    double dimensionScale = MapRenderUtils.getDimensionScale(player.world, viewedTileset);
                    if (dimensionScale <= 0.0) continue;

                    double effectiveX = player.x * dimensionScale;
                    double effectiveZ = player.z * dimensionScale;

                    if ((effectiveX > worldLeft && effectiveX < worldRight)
                            && (effectiveZ > worldTop && effectiveZ < worldBottom)) {
                        float offsetFromLeft = (float) ((effectiveX - worldLeft) * scale);
                        float offsetFromTop = (float) ((effectiveZ - worldTop) * scale);
                        ResIdentifier playerMarkerChoice = MapRenderUtils.getPlayerMarker(player.world);
                        float playerYawRad = (float) Math.toRadians(player.yaw - 180);
                        MapRenderUtils.drawPlayerMarker(context, offsetFromLeft, offsetFromTop, playerYawRad, playerMarkerChoice, MapRenderUtils.getPlayerMarkerTint(player.world));

                        if (!isMouseDown && !isCursorOverContextMenu(mouseX, mouseY)
                                && mouseX > offsetFromLeft - 4 && mouseX < offsetFromLeft + 4 && mouseY > offsetFromTop - 4
                                && mouseY < offsetFromTop + 4) {
                            hoveredPlayer = player;
                        }
                    }
                }

                // Render local player directly from minecraft.player
                if (minecraft.player != null && minecraft.level != null && !MapRenderUtils.isInQueue()) {
                    MapRenderUtils.updateLocalPlayer();

                    // Only render local player if we are viewing the world they are currently in
                    if (viewedWorld.equals(MapRenderUtils.lastLocalPlayerWorld) && viewedTileset != null) {
                        String localDimensionPath = CompatUtils.getIdentifier(minecraft.level.dimension()).getPath();
                        double myDimensionScale = MapRenderUtils.getDimensionScale(MapRenderUtils.lastLocalPlayerWorld, localDimensionPath, viewedTileset);

                        if (myDimensionScale > 0.0) {
                            double myEffectiveX = MapRenderUtils.lastLocalPlayerX * myDimensionScale;
                            double myEffectiveZ = MapRenderUtils.lastLocalPlayerZ * myDimensionScale;

                            if ((myEffectiveX > worldLeft && myEffectiveX < worldRight) && (myEffectiveZ > worldTop && myEffectiveZ < worldBottom)) {
                                float offsetFromLeft = (float) ((myEffectiveX - worldLeft) * scale);
                                float offsetFromTop = (float) ((myEffectiveZ - worldTop) * scale);
                                float localYawRad = (float) Math.toRadians(MapRenderUtils.lastLocalPlayerYaw);
                                MapRenderUtils.drawLocalPlayerMarker(context, offsetFromLeft, offsetFromTop, localYawRad, viewedTileset);

                                if (!isMouseDown && !isCursorOverContextMenu(mouseX, mouseY)
                                        && mouseX > offsetFromLeft - 4 && mouseX < offsetFromLeft + 4 && mouseY > offsetFromTop - 4
                                        && mouseY < offsetFromTop + 4) {
                                    if (localPlayerFetch != null) {
                                        hoveredPlayer = localPlayerFetch;
                                    }
                                }
                            }
                        }
                    }
                }
            }


            MutableComponent coords = currentLocationCoordinates;
            if(coords == null) coords = Component.literal("--, --");
            context.fill(2, this.height - bottomMapOffset - minecraft.font.lineHeight - 4, 6 + minecraft.font.width(coords.getString()), this.height - bottomMapOffset - 1, 0xB0000000);
            GraphicsHelper.drawString(context, minecraft.font, currentLocationCoordinates, 4, this.height - bottomMapOffset - minecraft.font.lineHeight - 2, 0xFFFFFFFF);
            context.scissorStack.pop();

            // Tooltips
            if (hoveredPlayer != null && sp.showPlayers) {
                // Players
                drawPlayerTooltip(context, hoveredPlayer, mouseX, mouseY);
            } else if (hoveredPlaceIndex != -1 && hoveredPlaceIndex < shownFeatures.length) {
                // Features
                String placeName = shownFeatures[hoveredPlaceIndex].name;
                String subText = shownFeatures[hoveredPlaceIndex].featureType.equals("portal") ?
                    pfu.getPortalPrettyName(shownFeatures[hoveredPlaceIndex].type) :
                    "Click to view details...";
                String placeId;
                boolean showTooltip = false;
                // "Hell yeah, he uses switch/cases. +10 aura points"
                switch (shownFeatures[hoveredPlaceIndex].featureType) {
                    case "place":
                        placeId = "P" + shownFeatures[hoveredPlaceIndex].id;
                        if (sp.showPlaces) showTooltip = true;
                        break;
                    case "area":
                        placeId = "A" + shownFeatures[hoveredPlaceIndex].id;
                        if (sp.showAreas) showTooltip = true;
                        break;
                    case "portal":
                        placeId = "SP" + shownFeatures[hoveredPlaceIndex].id;
                        showTooltip = true;
                        // TODO: Filter for portals
                        break;
                    default:
                        placeId = "" + shownFeatures[hoveredPlaceIndex].id;
                        showTooltip = true;
                        // TODO: Filter for whatever these are ¯\_(ツ)_/¯
                        break;
                }
                if(showTooltip) MapRenderUtils.drawTooltipComponent(context, List.of(
                    Component.literal(placeName).append(Component.literal(" (" + placeId + ")").withStyle(ChatFormatting.GRAY)),
                    Component.literal(subText).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                    Component.literal("Ctrl+Click to share Coords").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)),
                    mouseX + 7, mouseY + 4);
            } else if(hoveredClaimIndex != -1 && hoveredClaimIndex < shownClaims.size() && sp.showClaims && zoomlevel >= 8) {
                // And claims
                String claimHoverOwner = shownClaims.get(hoveredClaimIndex).popup.substring(32).replace("</span>", "");
                MapRenderUtils.drawTooltipComponent(context, List.of(
                    Component.literal("This claim is owned by:").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    Component.literal(claimHoverOwner.length() == 0 ? "Nobody (this is an admin claim!)" : claimHoverOwner)
                ), mouseX + 7, mouseY + 4);
            }

            // 387x50
            if (sponsorBanner != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, sponsorBanner.get(), 3, this.height - 28, 0, 0, 193, 25, 193, 25);
                if (drawSponsorTooltip) {
                    MapRenderUtils.drawTooltipComponent(context, sponsorHoverText, mouseX, mouseY);
                }
            } else {
                GraphicsHelper.drawString(context, Minecraft.getInstance().font, "Loading banner...", 10, this.height - 30, 0xFFFFFFFF);
            }

            if(showFilters) {
                GraphicsHelper.drawTooltipBackground(context, 30, 90, 30 + minecraft.font.width("Networks"), 120);
            }

            // Draw button builders
            //? if <26.1 {
            super.render(context, mouseX, mouseY, delta);
            //?} else {
            /*super.extractRenderState(context, mouseX, mouseY, delta);*///?}
            // Draw on top of buttons
            context.blit(RenderPipelines.GUI_TEXTURED, searchIcon.get(), this.width - 21, 9, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, settingsIcon.get(), this.width - 21, 34, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, compassIcon.get(), this.width - 21, this.height - bottomMapOffset - 21, 0, 0, 12, 12, 12, 12);
            context.blit(RenderPipelines.GUI_TEXTURED, hopperIcon.get(), 9, 94, 0, 0, 12, 12, 12, 12);

            // Zoom level number
            context.blit(RenderPipelines.GUI_TEXTURED,
                    ResIdentifier.of("minecraft", "textures/gui/sprites/widget/checkbox.png").get(),
                    5, 30, 0, 0, 20, 20, 20, 20);
            GraphicsHelper.drawCenteredString(context, minecraft.font, String.format("%d", zoomlevel), 15, 35, 0xFFFFFFFF);

            if(renderContextMenu) {
                double targetScreenX = (contextMenuWorldX - x) * scale;
                double targetScreenY = (contextMenuWorldZ - z) * scale;
                MapRenderUtils.drawPinMarker(context, (float) targetScreenX, (float) targetScreenY);

                context.pose().pushMatrix();
                context.pose().translate((float)contextMenuX, (float)contextMenuY);
                context.fill(0, -1, bestContextMenuWidth, (contextMenuItems.size() * minecraft.font.lineHeight) + 2,  0xaa3a3a3a);
                for (int i = 0; i < contextMenuItems.size(); i++) {
                    boolean isDisabled = (i == 1 && contextMenuValues[1] == null);
                    if (!isDisabled && mouseX > contextMenuX && mouseX < contextMenuX + bestContextMenuWidth &&
                        mouseY > contextMenuY + (i*minecraft.font.lineHeight) && mouseY < contextMenuY + (i*minecraft.font.lineHeight) + minecraft.font.lineHeight) {
                        context.fill(0, (i*minecraft.font.lineHeight) -1, bestContextMenuWidth, (i*minecraft.font.lineHeight) + minecraft.font.lineHeight -1, 0x3a000000);
                    }

                    int textColor = isDisabled ? 0xFF888888 : 0xFFFFFFFF;
                    GraphicsHelper.drawString(context, minecraft.font, contextMenuItems.get(i), (bestContextMenuWidth / 2)
                         - (minecraft.font.width(contextMenuItems.get(i)) / 2), i * minecraft.font.lineHeight, textColor);
                }

                context.pose().popMatrix();
            }
        }
    }

    private void changeMapScale(int zoomDelta, double coordScale) {
        stopCameraPan();
        int oldtilesize = 1 << (17 - zoomlevel);
        double oldscale = (double) minimapTileSize / oldtilesize;
        double cx = x + (this.width / 2.0) / oldscale;
        double cz = z + (this.height / 2.0) / oldscale;

        cx *= coordScale;
        cz *= coordScale;
        zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel + zoomDelta));

        int newtilesize = 1 << (17 - zoomlevel);
        double newscale = (double) minimapTileSize / newtilesize;
        // don't round quite yet so we have smoother map movement
        x = cx - (this.width / 2.0) / newscale;
        z = cz - (this.height / 2.0) / newscale;
    }

    public String viewedWorld = "Mondo";
    private Button worldButton;
    private Button dimensionButton;

    // 'centre' just for you, Larry
    public void centreOnCoords(double worldX, double worldZ) {
        centreOnCoords(worldX, worldZ, false);
    }
    public void centreOnCoords(double worldX, double worldZ, boolean animate) {
        int tilesize = 1 << (17 - zoomlevel);
        double scale = (double) minimapTileSize / tilesize;
        double targetX = worldX - ((this.width / 2.0) / scale);
        double targetZ = worldZ - (((this.height - bottomMapOffset) / 2.0) / scale);

        if (animate && (Math.abs(targetX - x) > 1.0 || Math.abs(targetZ - z) > 1.0)) {
            // they say 400-500ms is a good number for this kind of thing
            cameraPan = new MapRenderUtils.CameraPan(x, z, targetX, targetZ, 450);
        } else {
            stopCameraPan();
            x = targetX;
            z = targetZ;
            loadOnscreenTiles();
            resetFeatures();
            resetClaims();
        }
    }

    private void centreOnPlayer() {
        centreOnPlayer(false);
    }

    private void centreOnPlayer(boolean animate) {
        if (minecraft.player == null || minecraft.level == null || MapRenderUtils.isInQueue()) return;

        String playerWorld = MapRenderUtils.getCurrentWorld();
        String actualTileset = MapRenderUtils.getCurrentDimension();
        boolean switchedDimension = !java.util.Objects.equals(viewedTileset, actualTileset) || !viewedWorld.equals(playerWorld);
        if (switchedDimension) {
            String oldTileset = viewedTileset;
            viewedWorld = playerWorld;
            viewedTileset = actualTileset;
            updateDimensionButtons();
            if (viewedTileset != null && oldTileset != null) {
                int zoomDelta = MapRenderUtils.getDimensionZoomOffset(viewedTileset) - MapRenderUtils.getDimensionZoomOffset(oldTileset);
                if (zoomDelta != 0) {
                    zoomlevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, zoomlevel + zoomDelta));
                }
            }
        }

        if (viewedTileset == null) return;
        String playerDimension = CompatUtils.getIdentifier(minecraft.level.dimension()).getPath();
        double dimensionScale = MapRenderUtils.getDimensionScale(playerWorld, playerDimension, viewedTileset);
        centreOnCoords(minecraft.player.getX() * dimensionScale, minecraft.player.getZ() * dimensionScale, animate && !switchedDimension);
    }

    public void updateDimensionButtons() {
        List<String> worldTilesets = MapRenderUtils.getWorldTilesets(viewedWorld);
        boolean canSwitchDimensions = worldTilesets.size() > 1;
        if (worldButton != null) {
            worldButton.setMessage(Component.literal("World: " + viewedWorld).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Switch World")))));
        }
        if (dimensionButton != null) {
            dimensionButton.active = canSwitchDimensions;
            String dimLabel;
            if (worldTilesets.isEmpty()) {
                dimLabel = "None";
            } else if (!canSwitchDimensions && MapRenderUtils.isWorld(viewedWorld) && minecraft.level != null) {
                dimLabel = MapRenderUtils.prettyDimensionType(CompatUtils.getIdentifier(minecraft.level.dimension()).getPath());
            } else {
                dimLabel = MapRenderUtils.prettyDimensionType(viewedTileset);
            }
            dimensionButton.setMessage(Component.literal("Dimension: " + dimLabel).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Switch Dimension")))));
        }
    }

    private boolean mapPositioned = false;

    @Override
    public void removed() {
        super.removed();
        mapPositioned = false;
    }

    @Override
    protected void init() {
        isMouseDown = false;
        showFilters = false;
        if (!mapPositioned) {
            if (MapRenderUtils.isInQueue()) {
                viewedWorld = "Mondo";
                viewedTileset = "minecraft_overworld";
            } else {
                viewedWorld = MapRenderUtils.getCurrentWorld();
                viewedTileset = MapRenderUtils.getCurrentDimension();
            }
        }
        Button filtersSave = Button.builder(Component.literal("Save"), (btn) -> {
            sp.saveSettings();
            btn.active = false;
        }).bounds(30, 190, 30 + minecraft.font.width("Networks"), 20).tooltip(Tooltip.create(Component.literal("Save filters so they appear on the minimap."))).build();
        filtersSave.visible = false;
        filtersSave.active = false;
        this.addRenderableWidget(filtersSave);
        Checkbox placesCheckbox = Checkbox.builder(Component.literal("Places"), minecraft.font).selected(sp.showPlaces).onValueChange((checkbox, bl) -> {sp.showPlaces = bl; filtersSave.active = true;}).pos(30, 90).build();
        placesCheckbox.visible = false;
        this.addRenderableWidget(placesCheckbox);
        Checkbox areasCheckbox = Checkbox.builder(Component.literal("Areas"), minecraft.font).selected(sp.showAreas).onValueChange((checkbox, bl) -> {sp.showAreas = bl; filtersSave.active = true;}).pos(30, 110).build();
        areasCheckbox.visible = false;
        this.addRenderableWidget(areasCheckbox);
        Checkbox networksCheckbox = Checkbox.builder(Component.literal("Networks"), minecraft.font).selected(sp.showNetworks).onValueChange((checkbox, bl) -> {sp.showNetworks = bl; filtersSave.active = true;}).pos(30, 130).build();
        networksCheckbox.visible = false;
        this.addRenderableWidget(networksCheckbox);
        Checkbox playersCheckbox = Checkbox.builder(Component.literal("Players"), minecraft.font).selected(sp.showPlayers).onValueChange((checkbox, bl) -> {sp.showPlayers = bl; filtersSave.active = true;}).pos(30, 150).build();
        playersCheckbox.visible = false;
        this.addRenderableWidget(playersCheckbox);
        Checkbox claimsCheckbox = Checkbox.builder(Component.literal("Claims"), minecraft.font).selected(sp.showClaims).onValueChange((checkbox, bl) -> {sp.showClaims = bl; resetClaims(); filtersSave.active = true;}).pos(30, 170).build();
        claimsCheckbox.visible = false;
        this.addRenderableWidget(claimsCheckbox);

        Button filterBtn = Button.builder(Component.nullToEmpty(""), (btn) -> {
            showFilters = !showFilters;
            placesCheckbox.visible = showFilters;
            areasCheckbox.visible = showFilters;
            networksCheckbox.visible = showFilters;
            playersCheckbox.visible = showFilters;
            claimsCheckbox.visible = showFilters;
            filtersSave.visible = showFilters;
        }).bounds(5, 90, 20, 20).tooltip(Tooltip.create(Component.literal("Filters"))).build();

        Button negZoomBtn = Button.builder(Component.nullToEmpty("-"), (btn) -> {
            // Get current middle
            int oldtilesize = 1 << (17 - zoomlevel);
            double oldscale = (double) minimapTileSize / oldtilesize;
            double oldMiddleX = (this.width / 2.0) / oldscale;
            double oldMiddleZ = (this.height / 2.0) / oldscale;
            zoomlevel -= 1;
            if (zoomlevel < minZoomLevel)
                zoomlevel = minZoomLevel;
            // Set position to new x/z
            int newtilesize = 1 << (17 - zoomlevel);
            double newscale = (double) minimapTileSize / newtilesize;
            double newMiddleX = (this.width / 2.0) / newscale;
            double newMiddleZ = (this.height / 2.0) / newscale;
            x -= (newMiddleX - oldMiddleX);
            z -= (newMiddleZ - oldMiddleZ);
            loadOnscreenTiles();
            resetClaims();
        }).bounds(5, 55, 20, 20).tooltip(Tooltip.create(Component.literal("Zoom out"))).build();
        Button posZoomBtn = Button.builder(Component.nullToEmpty("+"), (btn) -> {
            // Get current middle
            int oldtilesize = 1 << (17 - zoomlevel);
            double oldscale = (double) minimapTileSize / oldtilesize;
            double oldMiddleX = (this.width / 2.0) / oldscale;
            double oldMiddleZ = (this.height / 2.0) / oldscale;
            zoomlevel += 1;
            if (zoomlevel > maxZoomLevel)
                zoomlevel = maxZoomLevel;
            // Set position to new x/z
            int newtilesize = 1 << (17 - zoomlevel);
            double newscale = (double) minimapTileSize / newtilesize;
            double newMiddleX = (this.width / 2.0) / newscale;
            double newMiddleZ = (this.height / 2.0) / newscale;
            x -= (newMiddleX - oldMiddleX);
            z -= (newMiddleZ - oldMiddleZ);
            loadOnscreenTiles();
            resetClaims();
        }).bounds(5, 5, 20, 20).tooltip(Tooltip.create(Component.literal("Zoom in"))).build();
        Button searchZoomBtn = Button.builder(Component.nullToEmpty(" "), (btn) -> {
            CompatUtils.setScreen(minecraft, new ChatScreen("/search ", false));
        }).bounds(this.width - 25, 5, 20, 20).tooltip(Tooltip.create(Component.literal("Search PVC Mapper"))).build();
        Button settingsBtn = Button.builder(Component.nullToEmpty(" "), (btn) -> {
            CompatUtils.setScreen(minecraft, ClothConfigScreen.createScreen(sp, this));
        }).bounds(this.width - 25, 30, 20, 20).tooltip(Tooltip.create(Component.literal("PVC Mapper Mod Settings"))).build();
        
        // Add sponsor banner code here
        SponsorUtils.getBannerAsync().thenAccept(banner -> {
            SponsorUtils.bannerToTexture(banner.imgurl, (rl) -> {
                this.sponsorBanner = rl;
            });
            sponsorHoverText = List.of(
                    Component.literal(banner.title).withStyle(ChatFormatting.BOLD)
                            .append(Component.literal(" (Click to view)").withStyle(ChatFormatting.GRAY)),
                    Component.literal(banner.description));
            sponsorURLString = banner.link;
        });

        // his and hers World and Dimension buttons
        int dimensionBtnWidth = minecraft.font.width("Dimension: Overworld") + 8;
        int dimensionBtnX = this.width - dimensionBtnWidth - 5;
        int worldBtnWidth = minecraft.font.width("World: Terra2") + 8;
        // make sure the buttons aren't too wide. Mom joke goes here.
        int maxButtonsWidth = Math.max(80, this.width - 209);
        if (dimensionBtnWidth + worldBtnWidth > maxButtonsWidth) {
            int totalWidth = dimensionBtnWidth + worldBtnWidth;
            dimensionBtnWidth = (dimensionBtnWidth * maxButtonsWidth) / totalWidth;
            worldBtnWidth = maxButtonsWidth - dimensionBtnWidth;
        }
        dimensionBtnX = this.width - dimensionBtnWidth - 5;
        int worldBtnX = dimensionBtnX - worldBtnWidth - 4;

        worldButton = Button.builder(
                Component.literal("World: " + viewedWorld).withStyle(
                    Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Switch World")))),
            (btn) -> {
                String oldTileset = viewedTileset;

                List<String> worldsWithMaps = MapRenderUtils.getMappableWorlds();
                int nextWorldIndex = 0;
                for (int i = 0; i < worldsWithMaps.size(); i++) {
                    if (worldsWithMaps.get(i).equals(viewedWorld)) {
                        nextWorldIndex = (i + 1) % worldsWithMaps.size();
                        break;
                    }
                }
                viewedWorld = worldsWithMaps.get(nextWorldIndex);
                List<String> worldTilesets = MapRenderUtils.getWorldTilesets(viewedWorld);
                viewedTileset = worldTilesets.isEmpty() ? null : worldTilesets.get(0);
                updateDimensionButtons();

                // Switching worlds never cross-projects coordinates (different servers)
                if (viewedTileset != null && oldTileset != null) {
                    int zoomDelta = MapRenderUtils.getDimensionZoomOffset(viewedTileset) - MapRenderUtils.getDimensionZoomOffset(oldTileset);
                    if (zoomDelta != 0) {
                        changeMapScale(zoomDelta, 1.0);
                    }
                }

                clearHoverState();
                loadOnscreenTiles();
                resetClaims();
                resetFeatures();
            }
        ).bounds(worldBtnX, this.height - 26, worldBtnWidth, 20).build();
        this.addRenderableWidget(worldButton);

        dimensionButton = Button.builder(
                Component.literal("Dimension: " + MapRenderUtils.prettyDimensionType(viewedTileset)).withStyle(
                    Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Switch Dimension")))),
                (btn) -> {
                List<String> worldTilesets = MapRenderUtils.getWorldTilesets(viewedWorld);
                if (worldTilesets.size() <= 1) return;

                String oldTileset = viewedTileset;
                
                // Find next dimension in cycle
                int nextIndex = 0;
                for (int i = 0; i < worldTilesets.size(); i++) {
                    if (worldTilesets.get(i).equals(oldTileset)) {
                        nextIndex = (i + 1) % worldTilesets.size();
                        break;
                    }
                }
                viewedTileset = worldTilesets.get(nextIndex);
                updateDimensionButtons();

                double coordScale = MapRenderUtils.getDimensionScale(oldTileset, viewedTileset);
                int zoomDelta = MapRenderUtils.getDimensionZoomOffset(viewedTileset) - MapRenderUtils.getDimensionZoomOffset(oldTileset);

                if (coordScale != 1.0 || zoomDelta != 0) {
                    // Shifting zoom by 3 steps exactly matches an 8x coordinate scale (2^3 = 8)
                    changeMapScale(zoomDelta, coordScale);
                }

                clearHoverState();
                loadOnscreenTiles();
                resetClaims();
                resetFeatures();
            }
        ).bounds(
            dimensionBtnX, this.height - 26, dimensionBtnWidth, 20).build();
        this.addRenderableWidget(dimensionButton);
        updateDimensionButtons();

        Button goToMe = Button.builder(Component.literal(""), (btn) -> {
            centreOnPlayer(true);
        }).tooltip(Tooltip.create(Component.literal("Go to Current Location"))).bounds(this.width - 25, this.height - bottomMapOffset - 25, 20, 20).build();
        this.addRenderableWidget(goToMe);
        goToMe.active = !MapRenderUtils.isInQueue() && MapRenderUtils.getCurrentDimension() != null;

        if (!mapPositioned) {
            boolean canCentreOnPlayer = sp.bigMapPos == BigMapPos.CENTRE_ON_PLAYER
                && !MapRenderUtils.isInQueue()
                && MapRenderUtils.getCurrentDimension() != null;

            if (canCentreOnPlayer) {
                centreOnPlayer();
            } else {
                centreOnCoords(0, 0);
            }
            mapPositioned = true;
        }
        loadOnscreenTiles();
        resetFeatures();

        this.addRenderableWidget(posZoomBtn);
        this.addRenderableWidget(negZoomBtn);
        this.addRenderableWidget(filterBtn);
        this.addRenderableWidget(searchZoomBtn);
        this.addRenderableWidget(settingsBtn);

    }
}

