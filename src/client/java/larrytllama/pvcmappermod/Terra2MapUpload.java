package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class Terra2MapUpload {
    public ChunkData getChunkData(LevelChunk chunk) {
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        if(blockColors == null) return null;
        ChunkData cd = new ChunkData();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        cd.chunkX = chunk.getPos().getMinBlockX() / 16;
        cd.chunkZ = chunk.getPos().getMinBlockZ() / 16;
        for(int z=15; z>-1; z--) {
            for (int x=0; x<16; x++) {
                cd.data[z][x] = new Block(0, 1.0f, 0.2f);
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if(surfaceY <= chunk.getMinY()) {
                    continue;
                }
                pos.set(chunk.getPos().getMinBlockX()+x, surfaceY, chunk.getPos().getMinBlockZ()+z);
                BlockState bs = chunk.getBlockState(pos);

                if(bs.isAir()) {
                    cd.data[z][x].c = 0x000000;
                } else {
                    cd.data[z][x].c = blockColors.getColor(bs, chunk.getLevel(), pos, 0);
                    
                    if (bs.is(Blocks.GRASS_BLOCK) || 
                        bs.is(Blocks.SHORT_GRASS) || 
                        bs.is(Blocks.TALL_GRASS) || 
                        bs.is(Blocks.FERN)) {
                        cd.data[z][x].c = chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value().getGrassColor(pos.getX(), pos.getZ());//BiomeColors.getAverageGrassColor(chunk.getLevel(), pos);
                    } else if (bs.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        cd.data[z][x].c = chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value().getWaterColor();//BiomeColors.getAverageWaterColor(chunk.getLevel(), pos);
                    } else if(cd.data[z][x].c == -1) {
                        // Standard solid block fallback
                        cd.data[z][x].c = bs.getMapColor(chunk.getLevel(), pos).col;
                    }
                }

                // Calculations for shading commence!
                // If at the top of the chunk, use the block below's calculations
                if(z == 0) {
                    cd.data[z][x].d = cd.data[1][x].d;
                    cd.data[z][x].n = cd.data[1][x].n;
                } else {

                    float slopeFactor = 1.0f;
                    try {
                        int northHeight = chunk.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ() - 1);
                        if(chunk.getMinY() == northHeight) {
                            // Chunk isn't loaded. Guess from the South?.
                            northHeight = chunk.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ() + 1);
                            if(pos.getY() > northHeight) {
                                slopeFactor = 0.75f;
                            } else if(pos.getY() < northHeight) {
                                slopeFactor = 1.15f;
                            }
                        } else {
                            if(pos.getY() > northHeight) {
                                slopeFactor = 1.15f;
                            } else if(pos.getY() < northHeight) {
                                slopeFactor = 0.75f;
                            }
                        }
                    } catch(Exception e) {
                        // Nevermind, we'll go without shading.
                    }

                    BlockPos posAbove = pos.above();

                    int dayLightValue = chunk.getLevel().getRawBrightness(posAbove, 0);
                    if (pos.getY() >= surfaceY && dayLightValue < 15) dayLightValue = 15;

                    cd.data[z][x].d = slopeFactor * (0.2f + 1.0f * 0.8f);
                }               
            }
        }
        return cd;
    }
}

class Block {
    float d;
    float n;
    int c;
    /**
     * c: Block colour (RGB)
     * d: Daytime lighting modifier
     * n: Nighttime lighting modifier
     */
    public Block(int c, float d, float n) {
        this.c = c;
        this.d = d;
        this.n = n;
    }
}

class ChunkData {
    int chunkX;
    int chunkZ;
    Block[][] data;
    public ChunkData() {
        this.data = new Block[16][16];
    }
}