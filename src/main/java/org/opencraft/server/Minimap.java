package org.opencraft.server;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ShortTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import org.opencraft.server.model.BlockConstants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public class Minimap {

  private int width, height, depth;
  private short[][][] blocks;
  private byte[] blocks0, blocks1;

  public void loadLevelData(String level) {
    try {
      FileInputStream fileIn = new FileInputStream("maps/" + level + ".cw");
      NBTInputStream nbtIn = new NBTInputStream(fileIn);

      CompoundMap classicWorld = ((CompoundTag) nbtIn.readTag()).getValue();

      width = ((ShortTag) classicWorld.get("X")).getValue();
      height = ((ShortTag) classicWorld.get("Z")).getValue();
      depth = ((ShortTag) classicWorld.get("Y")).getValue();
      blocks = new short[width][height][depth];
      blocks0 = new byte[width * height * depth];
      blocks1 = new byte[width * height * depth];

      // Load block data
      byte[] tmpBlocks = ((ByteArrayTag) classicWorld.get("BlockArray")).getValue();
      byte[] tmpBlocks2 = classicWorld.containsKey("BlockArray2")
          ? ((ByteArrayTag) classicWorld.get("BlockArray2")).getValue()
          : null;

      loadBlocks(tmpBlocks, tmpBlocks2);

      nbtIn.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void loadBlocks(byte[] blockArray, byte[] blockArray2) {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          int type0 = Server.getUnsigned(blockArray[(z * height + y) * width + x]);
          int type1 = 0;
          if (blockArray2 != null) {
            type1 = Server.getUnsigned(blockArray2[(z * height + y) * width + x]);
          }
          int type = type0 | (type1 << 8);
          blocks[x][y][z] = (short) type;
          blocks0[(z * height + y) * width + x] = (byte) type0;
          blocks1[(z * height + y) * width + x] = (byte) type1;
        }
      }
    }
  }

  public BufferedImage generateMinimap() {
    BufferedImage minimap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = minimap.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    for (int x = 0; x < width; x++) {
      for (int z = 0; z < height; z++) {
        boolean blockRendered = false;

        // Start checking from the topmost block (y = depth - 1) and move downward
        for (int y = depth - 1; y >= 0; y--) {
          int blockType = blocks[x][z][y];  // Get block type at (x, z, y)

          // If we find a non-air block, render it and break
          if (blockType != 0) {
            Color blockColor = getBlockColor(blockType);
            graphics.setColor(blockColor);
            graphics.fillRect(x, z, 1, 1); // Block = single pixel
            blockRendered = true;
            break;
          }
        }

        if (!blockRendered) {
          graphics.setColor(new Color(0, 0, 0, 0));
          graphics.fillRect(x, z, 1, 1);
        }
      }
    }

    return minimap;
  }

  private Color getBlockColor(int blockType) {
    switch (blockType) {
      case BlockConstants.STONE: return new Color(127, 127, 127);
      case BlockConstants.GRASS: return new Color(118, 182, 76);
      case BlockConstants.DIRT: return new Color(183, 133, 92);
      case BlockConstants.COBBLESTONE: return new Color(96, 96, 96);
      case BlockConstants.PLANKS: return new Color(188, 152, 92);
      case BlockConstants.ADMINIUM: return new Color(51, 51, 51);
      case BlockConstants.WATER:
      case BlockConstants.STILL_WATER: return new Color(61, 109, 255);
      case BlockConstants.LAVA:
      case BlockConstants.STILL_LAVA: return new Color(252, 87, 0);
      case BlockConstants.SAND: return new Color(221, 215, 160);
      case BlockConstants.GRAVEL: return new Color(137, 132, 130);
      case BlockConstants.ORE_GOLD: return new Color(255, 255, 181);
      case BlockConstants.ORE_IRON: return new Color(226, 192, 170);
      case BlockConstants.COAL: return new Color(55, 55, 55);
      case BlockConstants.TREE_TRUNK: return new Color(105, 84, 51);
      case BlockConstants.LEAVES: return new Color(73, 210, 48);
      case BlockConstants.SPONGE: return new Color(209, 209, 73);
      case BlockConstants.GLASS: return new Color(192, 245, 254);
      case BlockConstants.CLOTH_RED: return new Color(203, 46, 46);
      case BlockConstants.CLOTH_ORANGE: return new Color(203, 124, 46);
      case BlockConstants.CLOTH_YELLOW: return new Color(203, 203, 46);
      case BlockConstants.CLOTH_YELLOWGREEN: return new Color(124, 203, 46);
      case BlockConstants.CLOTH_GREEN: return new Color(46, 203, 124);
      case BlockConstants.CLOTH_GREENBLUE: return new Color(46, 203, 46);
      case BlockConstants.CLOTH_CYAN: return new Color(46, 203, 203); // Navy instead of cyan
      case BlockConstants.CLOTH_LIGHTBLUE: return new Color(95, 149, 203);
      case BlockConstants.CLOTH_BLUE: return new Color(110, 110, 203);
      case BlockConstants.CLOTH_PURPLE: return new Color(124, 46, 203);
      case BlockConstants.CLOTH_INDIGO: return new Color(159, 67, 203);
      case BlockConstants.CLOTH_VIOLET: return new Color(203, 46, 203);
      case BlockConstants.CLOTH_PINK: return new Color(203, 46, 124);
      case BlockConstants.CLOTH_DARKGRAY: return new Color(67, 67, 67);
      case BlockConstants.CLOTH_GRAY: return new Color(129, 129, 129);
      case BlockConstants.CLOTH_WHITE: return new Color(203, 203, 203);
      case BlockConstants.BAR_GOLD: return new Color(232, 208, 69);
      case BlockConstants.BAR_IRON: return new Color(209, 209, 209);
      case BlockConstants.STAIR_DOUBLE:
      case BlockConstants.STAIR: return new Color(200, 200, 200);
      case BlockConstants.BRICK_RED: return new Color(219, 68, 26);
      case BlockConstants.TNT: return new Color(201, 62, 23);
      case BlockConstants.PLANKS_BOOKSHELF: return new Color(188, 152, 98);
      case BlockConstants.COBBLESTONE_MOSSY: return new Color(61, 138, 61);
      case BlockConstants.OBSIDIAN: return new Color(16, 16, 24);

      default: return Color.WHITE; // Default color for unknown blocks
    }
  }

  public void saveMinimap(String outputPath) {
    try {
      BufferedImage minimap = generateMinimap();
      ImageIO.write(minimap, "PNG", new File(outputPath));
      System.out.println("Minimap saved to " + outputPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}