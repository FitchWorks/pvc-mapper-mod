package larrytllama.pvcmappermod;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;

public class TransportNetwork {
    
    public static class Segment {
        public String streetName;
        public int nameWidth;
        public double lineBearing;
        public double[][] coords;
        public int colour;

        public Segment(double[][] coords, String streetName, int colour, int nameWidth, double lineBearing) {
            this.coords = coords;
            this.streetName = streetName;
            this.colour = colour;
            this.nameWidth = nameWidth;
            this.lineBearing = lineBearing;
        }
    }

    private final ArrayList<Segment> segments = new ArrayList<>();

    public ArrayList<Segment> getSegments() {
        return segments;
    }

    public void recalculate(Network[] allNetworks, String currentDimension, int zoomlevel, double scale, double mapX, double mapZ, double mapWidth, double mapHeight, String source) {

        segments.clear();
        
        // For each network
        for (int i=0;i<allNetworks.length;i++) {
            if(!allNetworks[i].dimension.equals(currentDimension)) continue;
            if(zoomlevel < 9 && (allNetworks[i].type.equals("pathMark") || allNetworks[i].type.equals("pathUnmark")) ) continue;
            for (int street=0;street<allNetworks[i].edges.length;street++) {
                if(allNetworks[i].edges[street] == null) continue;
                for (int line=0;line<allNetworks[i].edges[street].coords.length-1;line++) {
                    double[] startPoint = allNetworks[i].edges[street].coords[line];
                    double[] endPoint = allNetworks[i].edges[street].coords[line + 1];
                    if(endPoint == null) continue;
                    if(MapRenderUtils.doesIntersect(MapRenderUtils.metersToPixels(startPoint[1]), MapRenderUtils.metersToPixels(startPoint[0]), MapRenderUtils.metersToPixels(endPoint[1]), MapRenderUtils.metersToPixels(endPoint[0]), mapX, mapZ, (mapWidth / scale), (mapHeight / scale) )) {
                        // Calc where to draw the lines
                        double[][] linePoints = new double[][]{ 
                            new double[]{ (MapRenderUtils.metersToPixels(startPoint[1]) - mapX) * scale, (MapRenderUtils.metersToPixels(startPoint[0]) - mapZ) * scale}, 
                            new double[]{ (MapRenderUtils.metersToPixels(endPoint[1]) - mapX) * scale, (MapRenderUtils.metersToPixels(endPoint[0]) - mapZ) * scale}
                        };
                        segments.add(
                            new Segment(
                                linePoints, 
                                allNetworks[i].edges[street].name, 
                                MapRenderUtils.networkTypeToColour(allNetworks[i].type, source), 
                                Minecraft.getInstance().font.width(allNetworks[i].edges[street].name),
                                MapRenderUtils.bearing(startPoint[1], startPoint[0], endPoint[1], endPoint[0])
                            )
                        );
                    }
                }
            }
        }
    }
}
