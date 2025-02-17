/*
 * Jacob_'s Capture the Flag for Minecraft Classic and ClassiCube
 * Copyright (c) 2010-2014 Jacob Morgan
 * Based on OpenCraft v0.2
 *
 * OpenCraft License
 *
 * Copyright (c) 2009 Graham Edgecombe, S�ren Enevoldsen and Brett Russell.
 * All rights reserved.
 *
 * Distribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Distributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Distributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     * Neither the name of the OpenCraft nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opencraft.server.net;

import com.google.common.collect.ImmutableList;
import org.opencraft.server.Configuration;
import org.opencraft.server.Constants;
import org.opencraft.server.Server;
import org.opencraft.server.model.BlockChange;
import org.opencraft.server.model.CustomBlockDefinition;
import org.opencraft.server.model.Entity;
import org.opencraft.server.model.Level;
import org.opencraft.server.model.Player;
import org.opencraft.server.model.Position;
import org.opencraft.server.model.Rotation;
import org.opencraft.server.model.TexturePackHandler;
import org.opencraft.server.model.World;
import org.opencraft.server.net.packet.PacketBuilder;
import org.opencraft.server.persistence.LoadPersistenceRequest;
import org.opencraft.server.persistence.SavedGameManager;
import org.opencraft.server.task.Task;
import org.opencraft.server.task.TaskQueue;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A utility class for sending packets.
 *
 * @author Graham Edgecombe
 */
public class ActionSender {

  /** The session. */
  private MinecraftSession session;

  /**
   * Creates the action sender.
   *
   * @param session The session.
   */
  public ActionSender(MinecraftSession session) {
    this.session = session;
  }

  /**
   * Sends a login response.
   *
   * @param protocolVersion The protocol version.
   * @param name The server name.
   * @param message The server message of the day.
   * @param op Operator flag.
   */
  public void sendLoginResponse(int protocolVersion, String name, String message, boolean op) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(0));
    bldr.putByte("protocol_version", protocolVersion);
    bldr.putString("server_name", name);
    bldr.putString("server_message", message);
    bldr.putByte("user_type", op ? 100 : 0);
    session.send(bldr.toPacket());
  }

  /**
   * Sends a login failure.
   *
   * @param message The message to send to the client.
   */
  public void sendLoginFailure(String message) {
    Server.d("Disconencting " + session.getIP() + " (" + session.username + ") " + message);
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(14));
    bldr.putString("reason", message);
    session.send(bldr.toPacket());
    session.close();
  }

  /** Sends the level init packet. */
  public void sendLevelInit(int size) {
    session.setAuthenticated();
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(2));
    bldr.putInt("size", size);
    session.send(bldr.toPacket());
  }

  /**
   * Sends a level block/chunk.
   *
   * @param len The length of the chunk.
   * @param chunk The chunk data.
   * @param percent The percentage.
   */
  public void sendLevelBlock(int len, byte[] chunk, int percent) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(3));
    bldr.putShort("chunk_length", len);
    bldr.putByteArray("chunk_data", chunk);
    bldr.putByte("percent", percent);
    session.send(bldr.toPacket());
  }

  /** Sends the level finish packet. */
  public void sendLevelFinish() {
    TaskQueue.getTaskQueue()
        .push(
            new Task() {
              public void execute() {
                try {
                  // for thread safety
                  final Level level = World.getWorld().getLevel();
                  PacketBuilder bldr =
                      new PacketBuilder(
                          PersistingPacketManager.getPacketManager().getOutgoingPacket(4));
                  bldr.putShort("width", level.getWidth());
                  bldr.putShort("height", level.getHeight());
                  bldr.putShort("depth", level.getDepth());
                  session.send(bldr.toPacket());
                  Position spawn = level.getSpawnPosition();
                  Rotation r = level.getSpawnRotation();
                  sendSpawn(
                      (byte) -1,
                      session.getPlayer().nameId,
                      session.getPlayer().getColoredName(),
                      session.getPlayer().getTeamName(),
                      session.getPlayer().getName(),
                      session.getPlayer().getListName(),
                      session.getPlayer().getSkinUrl(),
                      spawn.getX(),
                      spawn.getY(),
                      spawn.getZ(),
                      (byte) r.getRotation(),
                      (byte) r.getLook(),
                      false,
                      true);
                  // now load the player's game (TODO in the future do this in parallel with loading
                  // the
                  // level)
                  SavedGameManager.getSavedGameManager()
                      .queuePersistenceRequest(new LoadPersistenceRequest(session.getPlayer()));

                  session.setReady();
                  World.getWorld().completeRegistration(session);
                } catch (Exception ex) {
                  Server.log(ex);
                }
              }
            });
  }

  /**
   * Sends a teleport.
   *
   * @param position The new position.
   * @param rotation The new rotation.
   */
  public void sendTeleport(Position position, Rotation rotation) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(8));

    bldr.putByte("id", -1);
    bldr.putShort("x", position.getX());
    bldr.putShort("y", position.getY());
    bldr.putShort("z", position.getZ());
    bldr.putByte("rotation", rotation.getRotation());
    bldr.putByte("look", rotation.getLook());
    session.send(bldr.toPacket());
  }

  public void sendExtEntityTeleport(Position position, Rotation rotation) {
    PacketBuilder bldr =
            new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(54));

    bldr.putByte("id", 255);
    bldr.putByte("behaviour", 0b00000111);
    bldr.putShort("x", position.getX());
    bldr.putShort("y", position.getY());
    bldr.putShort("z", position.getZ());
    bldr.putByte("rotation", rotation.getRotation());
    bldr.putByte("look", rotation.getLook());
    session.send(bldr.toPacket());
  }

  public void sendAddPlayer(Player player, boolean isSelf) {
    if (player == null) {
      return;
    }

    sendSpawn(
        (byte) player.getId(),
        (byte) player.nameId,
        player.getColoredName(),
        player.getTeamName(),
        player.getName(),
        player.getListName(),
        player.getSkinUrl(),
        player.getPosition().getX(),
        player.getPosition().getY(),
        player.getPosition().getZ(),
        (byte) player.getRotation().getRotation(),
        (byte) player.getRotation().getLook(),
        isSelf,
        session.getPlayer().canSee(player));
  }

  public void sendSpawn(
      byte id,
      short nameId,
      String colorName,
      String teamName,
      String name,
      String listName,
      String skinUrl,
      int x,
      int y,
      int z,
      byte rotation,
      byte look,
      boolean isSelf,
      boolean isVisible) {
    if (session.isExtensionSupported("ExtPlayerList", 2)) {
      short fixNameId = (nameId == session.getPlayer().nameId ? -1 : nameId);
      sendAddPlayerName(fixNameId, name, listName, teamName, (byte) 1);
      if (!isSelf && isVisible) {
        sendExtSpawn(id, colorName, skinUrl != null ? skinUrl : name, x, y, z, rotation, look);
      }
    } else if (!isSelf) {
      PacketBuilder bldr =
          new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(7));
      bldr.putByte("id", id);
      bldr.putString("name", colorName);
      bldr.putShort("x", x);
      bldr.putShort("y", y);
      bldr.putShort("z", z);
      bldr.putByte("rotation", rotation);
      bldr.putByte("look", look);
      session.send(bldr.toPacket());
    }
  }

  public void sendExtSpawn(Player player) {
    sendExtSpawn(
        (byte) player.getId(),
        player.getColoredName(),
        player.getName(),
        player.getPosition().getX(),
        player.getPosition().getY(),
        player.getPosition().getZ(),
        (byte) player.getRotation().getRotation(),
        (byte) player.getRotation().getLook());
  }

  public void sendAddPlayerName(
      short id, String name, String listName, String groupName, byte groupRank) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(22));
    bldr.putShort("id", id);
    bldr.putString("player_name", name);
    bldr.putString("list_name", listName);
    bldr.putString("group_name", groupName);
    bldr.putByte("group_rank", groupRank);
    session.send(bldr.toPacket());
  }

  public void sendRemovePlayerName(short id) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(24));
    bldr.putShort("id", id);
    session.send(bldr.toPacket());
  }

  public void sendExtSpawn(
      byte id, String name, String skinName, int x, int y, int z, byte rotation, byte look) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(33));
    bldr.putByte("id", id);
    bldr.putString("name", name);
    bldr.putString("skin_name", skinName);
    bldr.putShort("x", x);
    bldr.putShort("y", y);
    bldr.putShort("z", z);
    bldr.putByte("rotation", rotation);
    bldr.putByte("look", look);
    session.send(bldr.toPacket());
  }

  /**
   * Sends the update entity packet.
   *
   * @param entity The entity being updated.
   */
  public void sendUpdateEntity(Entity entity) {
    final Position oldPosition = entity.getOldPosition();
    Position position = entity.getPosition();

    final Rotation oldRotation = entity.getOldRotation();
    final Rotation rotation = entity.getRotation();

    final int deltaX = -oldPosition.getX() - position.getX();
    final int deltaY = -oldPosition.getY() - position.getY();
    final int deltaZ = -oldPosition.getZ() - position.getZ();

    final int deltaRotation = -oldRotation.getRotation() - rotation.getRotation();
    final int deltaLook = -oldRotation.getLook() - rotation.getLook();
    if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
      if (deltaX > Byte.MAX_VALUE
          || deltaX < Byte.MIN_VALUE
          || deltaY > Byte.MAX_VALUE
          || deltaY < Byte.MIN_VALUE
          || deltaZ > Byte.MAX_VALUE
          || deltaZ < Byte.MIN_VALUE
          || deltaRotation > Byte.MAX_VALUE
          || deltaRotation < Byte.MIN_VALUE
          || deltaLook > Byte.MAX_VALUE
          || deltaLook < Byte.MIN_VALUE) {
        // teleport
        PacketBuilder bldr =
            new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(8));
        bldr.putByte("id", entity.getId());
        bldr.putShort("x", position.getX());
        bldr.putShort("y", position.getY());
        bldr.putShort("z", position.getZ());
        bldr.putByte("rotation", rotation.getRotation());
        bldr.putByte("look", rotation.getLook());
        session.send(bldr.toPacket());
      } else {
        // send move and rotate packet
        PacketBuilder bldr =
            new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(9));
        bldr.putByte("id", entity.getId());
        bldr.putByte("delta_x", deltaX);
        bldr.putByte("delta_y", deltaY);
        bldr.putByte("delta_z", deltaZ);
        bldr.putByte("delta_rotation", deltaRotation);
        bldr.putByte("delta_look", deltaLook);
        session.send(bldr.toPacket());
      }
    }
  }

  public void sendTeleport(int id, int x, int y, int z, int rotation, int look) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(8));
    bldr.putByte("id", id);
    bldr.putShort("x", x);
    bldr.putShort("y", y);
    bldr.putShort("z", z);
    bldr.putByte("rotation", rotation);
    bldr.putByte("look", look);
    session.send(bldr.toPacket());
  }

  /**
   * Sends the remove entity packet.
   *
   * @param id The entity being removed.
   */
  public void sendRemoveEntity(int id) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(12));
    bldr.putByte("id", id);
    session.send(bldr.toPacket());
  }

  public void sendRemoveEntity(Entity entity) {
    sendRemoveEntity(entity.getOldId());
  }

  public void sendRemovePlayer(Player p) {
    sendRemoveEntity(p);
    if (session.isExtensionSupported("ExtPlayerList", 2)) {
      this.sendRemovePlayerName(p.nameId);
    }
  }

  public void sendExtInfo() {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(16));
    bldr.putString("app_name", "OpenCraftCTF");
    bldr.putShort("extension_count", Constants.NUM_CPE_EXTENSIONS);
    session.send(bldr.toPacket());
  }

  public void sendExtEntry(String name, int version) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(17));
    bldr.putString("ext_name", name);
    bldr.putInt("ext_version", version);
    session.send(bldr.toPacket());
  }

  public void sendCustomBlockSupport() {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(19));
    bldr.putByte("support_level", Constants.CUSTOM_BLOCK_LEVEL);
    session.send(bldr.toPacket());
  }

  public void sendHoldThis(int slot, short block) {
    final short[] slots = new short[] {1, 4, 45, 3, 5, 17, 18, 2, 44};
    sendHoldThis(block);
    sendHoldThis(slots[slot]);
  }

  public void sendHoldThis(short block) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(20));
    bldr.putShort("block_to_hold", block);
    bldr.putByte("prevent_change", (byte) 0);
    session.send(bldr.toPacket());
  }

  public void sendHackControl(boolean enableHacks) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(32));
    bldr.putByte("flying", enableHacks ? (byte) 1 : (byte) 0);
    bldr.putByte("noclip", enableHacks ? (byte) 1 : (byte) 0);
    bldr.putByte("speeding", enableHacks ? (byte) 1 : (byte) 0);
    bldr.putByte("spawn_control", enableHacks ? (byte) 1 : (byte) 0);
    bldr.putByte("third_person_view", (byte) 1);
    bldr.putShort("jump_height", (short) -1);
    session.send(bldr.toPacket());
  }

  public void sendCPEHandshake() {
    sendExtInfo();
    for (int i = 0; i < Constants.NUM_CPE_EXTENSIONS; i++) {
      sendExtEntry(Constants.CPE_EXT_NAMES[i], Constants.CPE_EXT_VERSIONS[i]);
    }
  }

  public void sendMapAspect() {
    Level level = World.getWorld().getLevel();
    String texturePack;
    String server = Configuration.getConfiguration().isTest() ? "http://127.0.0.1:"+Constants.WEB_PORT : "https://buildism.net/ctf";
    if (level.props.getProperty("texturepack") != null) {
      texturePack = server + "/texturepack.zip?map=" + level.props.getProperty("texturepack");
    } else if (TexturePackHandler.hasCustomTexturePack(level.id)) {
      texturePack = server + "/texturepack.zip?map=" + level.id;
    } else {
      texturePack = server + "/texturepack.zip?map=default";
    }
    sendMapUrl(texturePack);
    sendMapProperty(0, level.sideBlock);
    sendMapProperty(1, level.edgeBlock);
    sendMapProperty(2, level.edgeHeight);
    sendMapProperty(3, level.cloudHeight);
    sendMapProperty(4, level.viewDistance);
    sendMapProperty(5, level.cloudSpeed);
    sendMapProperty(6, level.weatherSpeed);
    sendMapProperty(7, level.weatherFade);
    sendMapProperty(8, level.expFog);
    sendMapProperty(9, level.sideOffset);
  }

  private void sendMapUrl(String url) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(40));
    bldr.putString("url", url);
    session.send(bldr.toPacket());
  }

  public void sendMapProperty(int type, int value) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(41));
    bldr.putByte("type", type);
    bldr.putInt("value", value);
    session.send(bldr.toPacket());
  }

  public void sendMapColor(int id, short r, short g, short b) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(25));
    bldr.putByte("color", id);
    bldr.putShort("r", r);
    bldr.putShort("g", g);
    bldr.putShort("b", b);
    session.send(bldr.toPacket());
  }

  public void sendMapColors() {
    Level level = World.getWorld().getLevel();
    short[][] colors = level.colors;
    for (int i = 0; i < colors.length; i++) {
      if (colors[i][0] == -1) {
        sendMapColor(
            i,
            Constants.DEFAULT_COLORS[i][0],
            Constants.DEFAULT_COLORS[i][1],
            Constants.DEFAULT_COLORS[i][2]);
      } else {
        sendMapColor(i, colors[i][0], colors[i][1], colors[i][2]);
      }
    }
  }

  public void sendPing(boolean serverToClient, int data) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(43));
    bldr.putByte("server_to_client", serverToClient ? 1 : 0);
    bldr.putShort("data", data);
    session.send(bldr.toPacket());
  }

  public void sendDefineBlockExt(CustomBlockDefinition block) {
    if (block.blockDraw == Constants.BLOCK_DRAW_SPRITE) {
      sendDefineBlock(
          block.id,
          block.name,
          block.solid,
          block.movementSpeed,
          block.textureTop,
          block.textureLeft,
          block.textureBottom,
          block.emitsLight,
          block.walkSound,
          block.fullBright,
          0,
          Constants.BLOCK_DRAW_OPAQUE,
          block.fogDensity,
          block.fogR,
          block.fogG,
          block.fogB
      );
      return;
    }
    sendDefineBlockExt(
        block.id,
        block.name,
        block.solid,
        block.movementSpeed,
        block.textureTop,
        block.textureLeft,
        block.textureRight,
        block.textureFront,
        block.textureBack,
        block.textureBottom,
        block.emitsLight,
        block.walkSound,
        block.fullBright,
        block.minX,
        block.minY,
        block.minZ,
        block.maxX,
        block.maxY,
        block.maxZ,
        block.blockDraw,
        block.fogDensity,
        block.fogR,
        block.fogG,
        block.fogB);
  }

  public void sendDefineBlock(
      int id,
      String name,
      int solid,
      int movementSpeed,
      int textureTop,
      int textureSide,
      int textureBottom,
      boolean emitsLight,
      int walkSound,
      boolean fullBright,
      int shape,
      int blockDraw,
      int fogDensity,
      int fogR,
      int fogG,
      int fogB) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(35));
    bldr.putShort("id", id);
    bldr.putString("name", name);
    bldr.putByte("solid", solid);
    bldr.putByte("movement_speed", movementSpeed);
    bldr.putShort("texture_top", textureTop);
    bldr.putShort("texture_side", textureSide);
    bldr.putShort("texture_bottom", textureBottom);
    bldr.putByte("emits_light", emitsLight ? 1 : 0);
    bldr.putByte("walk_sound", walkSound);
    bldr.putByte("full_bright", fullBright ? 1 : 0);
    bldr.putByte("shape", shape);
    bldr.putByte("block_draw", blockDraw);
    bldr.putByte("fog_density", fogDensity);
    bldr.putByte("fog_r", fogR);
    bldr.putByte("fog_g", fogG);
    bldr.putByte("fog_b", fogB);
    session.send(bldr.toPacket());
  }

  public void sendDefineBlockExt(
      int id,
      String name,
      int solid,
      int movementSpeed,
      int textureTop,
      int textureLeft,
      int textureRight,
      int textureFront,
      int textureBack,
      int textureBottom,
      boolean emitsLight,
      int walkSound,
      boolean fullBright,
      int minX,
      int minY,
      int minZ,
      int maxX,
      int maxY,
      int maxZ,
      int blockDraw,
      int fogDensity,
      int fogR,
      int fogG,
      int fogB) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(37));
    bldr.putShort("id", id);
    bldr.putString("name", name);
    bldr.putByte("solid", solid);
    bldr.putByte("movement_speed", movementSpeed);
    bldr.putShort("texture_top", textureTop);
    bldr.putShort("texture_left", textureLeft);
    bldr.putShort("texture_right", textureRight);
    bldr.putShort("texture_front", textureFront);
    bldr.putShort("texture_back", textureBack);
    bldr.putShort("texture_bottom", textureBottom);
    bldr.putByte("emits_light", emitsLight ? 1 : 0);
    bldr.putByte("walk_sound", walkSound);
    bldr.putByte("full_bright", fullBright ? 1 : 0);
    bldr.putByte("min_x", minX);
    bldr.putByte("min_y", minY);
    bldr.putByte("min_z", minZ);
    bldr.putByte("max_x", maxX);
    bldr.putByte("max_y", maxY);
    bldr.putByte("max_z", maxZ);
    bldr.putByte("block_draw", blockDraw);
    bldr.putByte("fog_density", fogDensity);
    bldr.putByte("fog_r", fogR);
    bldr.putByte("fog_g", fogG);
    bldr.putByte("fog_b", fogB);
    session.send(bldr.toPacket());
  }

  public void sendBlockPermissions(int id, boolean place, boolean delete) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(28));
    bldr.putShort("id", id);
    bldr.putByte("place", place ? 1 : 0);
    bldr.putByte("delete", delete ? 1 : 0);
    session.send(bldr.toPacket());
  }

  public void sendRemoveBlockDefinition(int id) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(36));
    bldr.putShort("id", id);
    session.send(bldr.toPacket());
  }

  public void sendInventoryOrder(int id, int order) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(44));
    bldr.putShort("id", id);
    bldr.putShort("order", order);
    session.send(bldr.toPacket());
  }

  public void sendChangeModel(int id, String model) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(29));
    bldr.putByte("id", id);
    bldr.putString("model", model);
    session.send(bldr.toPacket());
  }

  public void sendEntityProperty(int id, int property, int value) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(42));
    bldr.putByte("id", id);
    bldr.putByte("key", property);
    bldr.putInt("value", value);
    session.send(bldr.toPacket());
  }

  public void sendHotkey(String label, String action, int keyCode, byte modifier) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(21));
    bldr.putString("label", label);
    bldr.putString("action", action);
    bldr.putInt("key", keyCode);
    bldr.putByte("modifier", modifier);
    session.send(bldr.toPacket());
  }

  /**
   * Sends a chat message.
   *
   * @param message The message.
   */
  public void sendChatMessage(String message) {
    sendChatMessage(message, 0);
  }

  /**
   * Sends a block.
   *
   * @param x X coordinate.
   * @param y Y coordinate.
   * @param z Z coordinate.
   * @param type BlockDefinition type.
   */
  public void sendBlock(int x, int y, int z, short type) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(6));
    bldr.putShort("x", x);
    bldr.putShort("y", y);
    bldr.putShort("z", z);
    bldr.putShort("type", type);
    session.send(bldr.toPacket());
  }

  public void sendDefineEffect(
      int id,
      int u1,
      int u2,
      int v1,
      int v2,
      int r,
      int g,
      int b,
      int frameCount,
      int particleCount,
      int size,
      int sizeVariation,
      int spread,
      int speed,
      int gravity,
      int lifetime,
      int lifetimeVariation,
      int collide,
      int fullBright) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(48));
    bldr.putByte("id", id);
    bldr.putByte("u1", u1);
    bldr.putByte("v1", v1);
    bldr.putByte("u2", u2);
    bldr.putByte("v2", v2);
    bldr.putByte("r", r);
    bldr.putByte("g", g);
    bldr.putByte("b", b);
    bldr.putByte("frame_count", frameCount);
    bldr.putByte("particle_count", particleCount);
    bldr.putByte("size", size);
    bldr.putInt("size_variation", sizeVariation);
    bldr.putShort("spread", spread);
    bldr.putInt("speed", speed);
    bldr.putInt("gravity", gravity);
    bldr.putInt("lifetime", lifetime);
    bldr.putInt("lifetime_variation", lifetimeVariation);
    bldr.putByte("collide", collide);
    bldr.putByte("full_bright", fullBright);
    session.send(bldr.toPacket());
  }

  public void sendSpawnEffect(
      int id,
      int posX,
      int posY,
      int posZ,
      int originX,
      int originY,
      int originZ) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(49));
    bldr.putByte("id", id);
    bldr.putInt("pos_x", posX);
    bldr.putInt("pos_y", posY);
    bldr.putInt("pos_z", posZ);
    bldr.putInt("origin_x", originX);
    bldr.putInt("origin_y", originY);
    bldr.putInt("origin_z", originZ);
    session.send(bldr.toPacket());
  }

  public void sendSelectionCuboid(
          int id,
          String label,
          short startX,
          short startY,
          short startZ,
          short endX,
          short endY,
          short endZ,
          short red,
          short green,
          short blue,
          short opacity) {
    PacketBuilder bldr =
            new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(26));
    bldr.putByte("id", id);
    bldr.putString("label", label);
    bldr.putShort("start_x", startX);
    bldr.putShort("start_y", startY);
    bldr.putShort("start_z", startZ);
    bldr.putShort("end_x", endX);
    bldr.putShort("end_y", endY);
    bldr.putShort("end_z", endZ);
    bldr.putShort("r", red);
    bldr.putShort("g", green);
    bldr.putShort("b", blue);
    bldr.putShort("opacity", opacity);
    session.send(bldr.toPacket());
  }

  public void sendRemoveSelectionCuboid(
          int id) {
    PacketBuilder bldr =
            new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(27));
    bldr.putByte("id", id);
    session.send(bldr.toPacket());
  }

  public void sendHotbar(int id, int index) {
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(45));
    bldr.putShort("id", id);
    bldr.putByte("index", index);
    session.send(bldr.toPacket());
  }

  public void sendBlockUpdate(Level level, ImmutableList<BlockChange> blockChanges) {
    byte[] indices = new byte[1024];
    byte[] blocks = new byte[320];

    int i = 0;
    int j = 0;
    for (BlockChange blockChange : blockChanges) {
      int index =
          (blockChange.z * level.getHeight() + blockChange.y) * level.getWidth() + blockChange.x;

      indices[i] = (byte) (index >>> 24);
      indices[i + 1] = (byte) (index >>> 16);
      indices[i + 2] = (byte) (index >>> 8);
      indices[i + 3] = (byte) (index);
      i += 4;

      blocks[j] = (byte) blockChange.type;
      j++;
    }

    // The high 2 bits of each block are packed into the remaining 64 bytes.
    // See https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Classic_Protocol_Extension#Affect_on_BulkBlockUpdate
    int k = 0;
    for (BlockChange blockChange : blockChanges) {
      int highBits = (blockChange.type & 0b1100000000) >>> 8;
      int byteIndex = 256 + (k / 4);
      int bitIndex = ((k % 4) * 2);
      blocks[byteIndex] |= (byte) (highBits << bitIndex);
      k++;
    }

    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(38));
    bldr.putByte("count", blockChanges.size() - 1);
    bldr.putByteArray("indices", indices);
    bldr.putByteArray320("blocks", blocks);
    session.send(bldr.toPacket());
  }

  /**
   * Sends a chat message.
   *
   * @param message The message.
   */
  public void sendChatMessage(String message, int messageType) {
    if (messageType != 0 && !session.ccUser) {
      return;
    }
    PacketBuilder bldr =
        new PacketBuilder(PersistingPacketManager.getPacketManager().getOutgoingPacket(13));
    String message2 = "";
    int maxLength = 64;
    if (message.length() > maxLength) {
      for (int i = maxLength; i > 0; i--) {
        if (message.charAt(i) == ' ' && !((i == 1 || i == 4) && message.charAt(0) == '>')) {
          maxLength = i;
          break;
        }
      }
      message2 = message.substring(maxLength);
      int idx = message.lastIndexOf("&");
      if (idx != -1) {
        if (message2.charAt(0) == '&') {
          message2 = message2.substring(2);
        }
        message2 = "&" + message.charAt(idx + 1) + message2;
      }
      message = message.substring(0, maxLength);
      if (message.charAt(message.length() - 2) == '&') {
        message = message.substring(0, maxLength - 2);
      } else if (message.charAt(message.length() - 1) == '&') {
        message = message.substring(0, maxLength - 1);
      }
    }
    bldr.putByte("id", messageType);
    bldr.putString("message", message);
    session.send(bldr.toPacket());
    if (!message2.equals("")) {
      sendChatMessage("> " + message2, messageType);
    }
  }
}
