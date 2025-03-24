package org.opencraft.server.cmd.impl;

import org.opencraft.server.Server;
import org.opencraft.server.cmd.Command;
import org.opencraft.server.cmd.CommandParameters;
import org.opencraft.server.model.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClanCommand implements Command {

  private static final ClanCommand INSTANCE = new ClanCommand();
  private final Map<String, Set<String>> clanInvites = new HashMap<>();

  public static ClanCommand getCommand() {
    return INSTANCE;
  }

  @Override
  public void execute(Player player, CommandParameters params) {
    if (params.getArgumentCount() < 1) {
      player.getActionSender().sendChatMessage("Usage: /clan <create|delete|invite|accept|members|leave> <args>");
      return;
    }

    String subCommand = params.getStringArgument(0);

    switch (subCommand.toLowerCase()) {
      case "create":
        handleCreate(player, params);
        break;
      case "delete":
        handleDelete(player);
        break;
      case "invite":
        handleInvite(player, params);
        break;
      case "accept":
        handleAccept(player, params);
        break;
      case "members":
        handleMembers(player, params);
        break;
      case "leave":
        handleLeave(player);
        break;
      default:
        player.getActionSender().sendChatMessage("Invalid subcommand. Usage: /clan <create|delete|invite|accept|members|leave> <args>");
        break;
    }
  }

  private void handleCreate(Player player, CommandParameters params) {
    if (params.getArgumentCount() < 2) {
      player.getActionSender().sendChatMessage("Usage: /clan create <clanName>");
      return;
    }

    String clanName = params.getStringArgument(1);
    if (Server.clanManager.getClans().containsKey(clanName)) {
      player.getActionSender().sendChatMessage("Clan already exists.");
      return;
    }

    Server.clanManager.createClan(clanName, player.getName());
    Server.playerClans.put(player.getName(), clanName);

    player.getActionSender().sendChatMessage("Clan " + clanName + " has been created.");
  }

  private void handleDelete(Player player) {
    String clanName = Server.playerClans.get(player.getName());
    if (clanName == null) {
      player.getActionSender().sendChatMessage("You are not in a clan.");
      return;
    }

    Server.clanManager.removeClan(clanName);
    Server.playerClans.values().removeIf(clan -> clan.equals(clanName));
    player.getActionSender().sendChatMessage("Clan " + clanName + " has been deleted.");
  }

  private void handleInvite(Player player, CommandParameters params) {
    if (params.getArgumentCount() < 2) {
      player.getActionSender().sendChatMessage("Usage: /clan invite <player>");
      return;
    }

    String targetName = params.getStringArgument(1);
    Player target = Player.getPlayer(targetName, player.getActionSender());

    if (target == null) {
      player.getActionSender().sendChatMessage("Player not found.");
      return;
    }

    if (Server.playerClans.containsKey(target.getName())) {
      player.getActionSender().sendChatMessage(target.getName() + " is already in a clan.");
      return;
    }

    String clanName = Server.playerClans.get(player.getName());
    if (clanName == null) {
      player.getActionSender().sendChatMessage("You must be in a clan to invite players.");
      return;
    }

    clanInvites.computeIfAbsent(target.getName(), k -> new HashSet<>()).add(clanName);
    target.getActionSender().sendChatMessage(player.getName() + " has invited you to join their clan: " + clanName);
    target.getActionSender().sendChatMessage("    Type &b/clan accept " + player.getName() + " &fif you wish to join.");
    player.getActionSender().sendChatMessage("You have invited " + target.getName() + " to your clan.");
  }

  private void handleAccept(Player player, CommandParameters params) {
    if (params.getArgumentCount() < 2) {
      player.getActionSender().sendChatMessage("Usage: /clan accept <player>");
      return;
    }

    String inviterName = params.getStringArgument(1);
    String clanName = Server.playerClans.get(inviterName);

    // Check if the player has a valid invite and is not already in a clan
    if (clanName == null || !clanInvites.getOrDefault(player.getName(), new HashSet<>()).contains(clanName)) {
      player.getActionSender().sendChatMessage("You have no invite from " + inviterName);
      return;
    }

    clanInvites.get(player.getName()).remove(clanName);
    Server.clanManager.addMember(clanName, player.getName());
    Server.playerClans.put(player.getName(), clanName);

    Server.log(player.getName() + " has joined the clan: " + clanName);
    player.getActionSender().sendChatMessage("You have joined the clan: " + clanName);
  }


  private void handleMembers(Player player, CommandParameters params) {
    String clanName;

    if (params.getArgumentCount() < 2) {
      // No clan name specified, so default to the player's clan
      clanName = Server.playerClans.get(player.getName());
      if (clanName == null) {
        player.getActionSender().sendChatMessage("You are not in a clan.");
        return;
      }
    } else {
      clanName = params.getStringArgument(1);
      if (!Server.clanManager.getClans().containsKey(clanName)) {
        player.getActionSender().sendChatMessage("Clan " + clanName + " does not exist.");
        return;
      }
    }

    Set<String> members = Server.clanManager.getClans().get(clanName).getMembers();
    StringBuilder memberList = new StringBuilder("Members of " + clanName + ":");
    for (String member : members) {
      memberList.append("\n").append(member);
    }

    player.getActionSender().sendChatMessage(memberList.toString());
  }

  private void handleLeave(Player player) {
    String clanName = Server.playerClans.get(player.getName());
    if (clanName == null) {
      player.getActionSender().sendChatMessage("You are not in a clan.");
      return;
    }

    Server.clanManager.removeMember(clanName, player.getName());
    Server.playerClans.remove(player.getName());
    player.getActionSender().sendChatMessage("You have left the clan: " + clanName);
  }
}
