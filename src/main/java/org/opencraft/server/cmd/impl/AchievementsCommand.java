package org.opencraft.server.cmd.impl;

import org.opencraft.server.Server;
import org.opencraft.server.cmd.Command;
import org.opencraft.server.cmd.CommandParameters;
import org.opencraft.server.model.Player;
import org.opencraft.server.model.AchievementManager;

public class AchievementsCommand implements Command {

  private static final AchievementsCommand INSTANCE = new AchievementsCommand();

  /**
   * Gets the singleton instance of this command.
   *
   * @return The singleton instance of this command.
   */
  public static AchievementsCommand getCommand() {
    return INSTANCE;
  }

  @Override
  public void execute(Player player, CommandParameters params) {
    String targetPlayerName = player.getName();
    String option = "all";

    if (params.getArgumentCount() > 0) {
      String firstArgument = params.getStringArgument(0).toLowerCase();
      if (firstArgument.equals("locked") || firstArgument.equals("unlocked") || firstArgument.equals("all")) {
        option = firstArgument;
      } else {
        targetPlayerName = firstArgument;
      }
    }

    if (params.getArgumentCount() > 1) {
      option = params.getStringArgument(1).toLowerCase();
    }

    Player targetPlayer = Player.getPlayer(targetPlayerName, player.getActionSender());
    if (targetPlayer == null) {
      player.getActionSender().sendChatMessage("&cPlayer not found: " + targetPlayerName);
      return;
    }

    switch (option) {
      case "locked":
        showLockedAchievements(player, targetPlayer);
        break;
      case "unlocked":
        showUnlockedAchievements(player, targetPlayer);
        break;
      case "all":
        showAllAchievements(player, targetPlayer);
        break;
      default:
        player.getActionSender().sendChatMessage("&cInvalid option. Use 'locked', 'unlocked', or 'all'.");
        break;
    }
  }

  private void showAllAchievements(Player player, Player targetPlayer) {
    player.getActionSender().sendChatMessage("&7Achievements for &e" + targetPlayer.getName() + ":");

    Server.achievementManager.getAchievements().forEach((id, achievement) -> {
      String status = Server.achievementManager.hasPlayerEarnedAchievement(targetPlayer.getName(), id) ? "&aUnlocked" : "&7Locked";
      player.getActionSender().sendChatMessage("&e" + achievement.getDescription() + " - " + status);
    });
  }

  private void showLockedAchievements(Player player, Player targetPlayer) {
    player.getActionSender().sendChatMessage("&7Locked achievements for &e" + targetPlayer.getName() + ":");

    Server.achievementManager.getAchievements().forEach((id, achievement) -> {
      if (!Server.achievementManager.hasPlayerEarnedAchievement(targetPlayer.getName(), id)) {
        player.getActionSender().sendChatMessage("&e" + achievement.getDescription() + " - &7Locked");
      }
    });
  }

  private void showUnlockedAchievements(Player player, Player targetPlayer) {
    player.getActionSender().sendChatMessage("&7Unlocked achievements for &e" + targetPlayer.getName() + ":");

    Server.achievementManager.getAchievements().forEach((id, achievement) -> {
      if (Server.achievementManager.hasPlayerEarnedAchievement(targetPlayer.getName(), id)) {
        player.getActionSender().sendChatMessage("&e" + achievement.getDescription() + " - &aUnlocked");
      }
    });
  }
}