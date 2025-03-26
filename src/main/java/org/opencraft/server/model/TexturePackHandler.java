package org.opencraft.server.model;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.opencraft.server.Server;
import org.opencraft.server.game.impl.GameSettings;

public class TexturePackHandler {

  public static final int CTF_BLOCK_SIZE_PX = 16;
  public static final int TEXTURE_WIDTH_BLOCKS = 16;
  public static final int TEXTURE_HEIGHT_BLOCKS = 32;

  public static boolean hasCustomTexturePack(String map) {
    File texturePackFile = new File("texturepacks/terrain_" + map + ".zip");
    return texturePackFile.exists();
  }

  protected static BufferedImage mergeTerrain(Image ctfTerrain, Image source) {
    /*
     * So...
     * first find size of texture file. Standard seems to be 16 tiles across.
     *    pixels per block = width in pixels / 16
     * Calculate ctf blocks height. We'll just say the default ctf texture has to be
     * 16 blocks by default. so the ctf blocks height is going to be
     *    number of rows = height in pixels / 16
     * Scale and place in bottom left.
     *    scale factor = pixels per block / 16
     * Bottom left is going to be 512 - 16 * number of rows
     */
    int pxPerBlock = source.getWidth(null) / TEXTURE_WIDTH_BLOCKS;

    // Get number of rows we are going to take up.
    int ctfRows = ctfTerrain.getHeight(null) / CTF_BLOCK_SIZE_PX;
    // Scale CTF image if needed.
    int scaleFactor = pxPerBlock / CTF_BLOCK_SIZE_PX;
    ctfTerrain = ctfTerrain.getScaledInstance(ctfTerrain.getWidth(null) * scaleFactor,
        ctfTerrain.getHeight(null) * scaleFactor, Image.SCALE_DEFAULT);

    // Make it a 16x32 texture by default I guess?
    BufferedImage target = new BufferedImage(pxPerBlock * TEXTURE_WIDTH_BLOCKS,
        pxPerBlock * TEXTURE_HEIGHT_BLOCKS, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = target.createGraphics();
    // Draw original image on first.
    graphics.drawImage(source, 0, 0, null);

    graphics.setComposite(AlphaComposite.Src);

    // Draw the scaled CTF Texture onto it.
    graphics.drawImage(ctfTerrain, 0, (TEXTURE_HEIGHT_BLOCKS - ctfRows) * pxPerBlock, null);

    return target;
  }

  protected static BufferedImage mergeClanBlocks(Image clanBlocks, Image source) {
    int pxPerBlock = source.getWidth(null) / TEXTURE_WIDTH_BLOCKS;
    int scaleFactor = pxPerBlock / CTF_BLOCK_SIZE_PX;

    // Create the base buffered image for the clanBlocks
    BufferedImage bufferedImage = new BufferedImage(clanBlocks.getWidth(null), clanBlocks.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = bufferedImage.createGraphics();
    g2d.drawImage(clanBlocks, 0, 0, null);
    g2d.dispose();

    int width = 16 * scaleFactor;
    int height = 16 * scaleFactor;

    // Maroon, red, orange, gold, yellow, lime, green, turquoise, cyan, blue, navy, purple, pink, white, silver, gray, black
    Map<String, int[]> colorMap = new HashMap<>();
    colorMap.put("maroon", new int[] { 0, 0 });
    colorMap.put("red", new int[] { 80, 0 });
    colorMap.put("orange", new int[] { 160, 0 });
    colorMap.put("gold", new int[] { 240, 0 });
    colorMap.put("yellow", new int[] { 0, 16 });
    colorMap.put("lime", new int[] { 80, 16 });
    colorMap.put("green", new int[] { 160, 16 });
    colorMap.put("turquoise", new int[] { 240, 16 });
    colorMap.put("cyan", new int[] { 0, 32 });
    colorMap.put("blue", new int[] { 80, 32 });
    colorMap.put("navy", new int[] { 160, 32 });
    colorMap.put("purple", new int[] { 240, 32 });
    colorMap.put("pink", new int[] { 0, 48 });
    colorMap.put("white", new int[] { 80, 48 });
    colorMap.put("silver", new int[] { 160, 48 });
    colorMap.put("gray", new int[] { 240, 48 });
    colorMap.put("black", new int[] { 0, 64 });

    // Get the color coordinates based on primary and secondary team colors
    String primaryColour = GameSettings.getString("PrimaryTeamColor").toLowerCase();
    int[] primaryCoords = colorMap.getOrDefault(primaryColour, new int[] { 80, 0 });  // Default to red if no match

    String secondaryColour = GameSettings.getString("SecondaryTeamColor").toLowerCase();
    int[] secondaryCoords = colorMap.getOrDefault(secondaryColour, new int[] { 80, 32 });  // Default to blue if no match

    int px = primaryCoords[0];
    int py = primaryCoords[1];
    int sx = secondaryCoords[0];
    int sy = secondaryCoords[1];

    // Crop the original image into multiple sub-images
    BufferedImage[] croppedImages = new BufferedImage[] {
        bufferedImage.getSubimage(px, py, width, height), // Primary mine
        bufferedImage.getSubimage(px + 16, py, width, height), // Primary flag
        bufferedImage.getSubimage(px + 32, py, width * 3, height), // Primary TNT
        bufferedImage.getSubimage(sx, sy, width, height), // Secondary mine
        bufferedImage.getSubimage(sx + 16, sy, width, height), // Secondary flag
        bufferedImage.getSubimage(sx + 32, sy, width * 3, height) // Secondary TNT
    };

    BufferedImage target = new BufferedImage(pxPerBlock * TEXTURE_WIDTH_BLOCKS, pxPerBlock * TEXTURE_HEIGHT_BLOCKS, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = target.createGraphics();

    // Draw original source image
    graphics.drawImage(source, 0, 0, null);
    graphics.setComposite(AlphaComposite.Src);

    AffineTransform transform = new AffineTransform();
    transform.scale(scaleFactor, scaleFactor);

    // Draw the new sub-images over the default terrain.png
    for (int i = 0; i < croppedImages.length; i++) {
      BufferedImage croppedImage = croppedImages[i];
      BufferedImage scaledImage = scaleImage(croppedImage, transform);

      int yOffset = 496 * scaleFactor;

      // Adjust offsets for each block's position in F10
      switch (i) {
        case 0:
          graphics.drawImage(scaledImage, 0, yOffset, null); break;  // Primary mine
        case 1:
          graphics.drawImage(scaledImage, 48 * scaleFactor, yOffset, null); break;  // Primary flag
        case 2:
          graphics.drawImage(scaledImage, 96 * scaleFactor, yOffset, null); break;  // Primary TNT
        case 3:
          graphics.drawImage(scaledImage, 16 * scaleFactor, yOffset, null); break;  // Secondary mine
        case 4:
          graphics.drawImage(scaledImage, 64 * scaleFactor, yOffset, null); break;  // Secondary flag
        case 5:
          graphics.drawImage(scaledImage, 176 * scaleFactor, yOffset, null); break;  // Secondary TNT
      }
    }

    graphics.dispose();
    return target;
  }

  private static BufferedImage scaleImage(BufferedImage image, AffineTransform transform) {
    BufferedImage scaledImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = scaledImage.createGraphics();
    g2d.drawImage(image, transform, null);
    g2d.dispose();
    return scaledImage;
  }

  public static void createPatchedTexturePack(String map) {
    try {
      File texturePackFile = new File("texturepacks/terrain_" + map + ".zip");
      File outputFile = new File("texturepacks_cache/terrain_" + map + ".zip");
      if (outputFile.exists()) {
        return;
      }

      ZipFile in = new ZipFile(texturePackFile);
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile));

      File fontFile = new File("texturepack_patch/default.png");
      out.putNextEntry(new ZipEntry("default.png"));
      Files.copy(fontFile.toPath(), out);
      File particlesFile = new File("texturepack_patch/particles.png");
      out.putNextEntry(new ZipEntry("particles.png"));
      Files.copy(particlesFile.toPath(), out);
      Image ctfTerrain = ImageIO.read(new File("texturepack_patch/ctf_terrain.png"));
      Image clanBlocks = ImageIO.read(new File("texturepack_patch/clan_blocks.png"));

      Enumeration<? extends ZipEntry> entries = in.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        switch (entry.getName()) {
          case "default.png":
            // use font file above
            break;
          case "particles.png":
            // use particles file above
            break;
          case "terrain.png":
            // Merge both terrain and clan block textures
            out.putNextEntry(new ZipEntry(entry.getName()));
            DataInputStream terrainData = new DataInputStream(in.getInputStream(entry));
            Image source = ImageIO.read(terrainData);

            // First, merge terrain, then merge clan blocks on top of that
            BufferedImage mergedImage = mergeTerrain(ctfTerrain, source);
            mergedImage = mergeClanBlocks(clanBlocks, mergedImage);

            // Save the final merged image to the ZIP
            ByteArrayOutputStream imgOutput = new ByteArrayOutputStream();
            ImageIO.write(mergedImage, "png", imgOutput);
            out.write(imgOutput.toByteArray());
            terrainData.close();
            break;
          default:
            out.putNextEntry(new ZipEntry(entry.getName()));
            DataInputStream dataIn = new DataInputStream(in.getInputStream(entry));
            byte[] bytes = new byte[(int) entry.getSize()];
            dataIn.readFully(bytes);
            out.write(bytes);
            dataIn.close();
            break;
        }
      }
      in.close();
      out.close();
    } catch (IOException ex) {
      Server.log(ex);
    }
  }
}
