package org.opencraft.server.cmd.impl;

import org.opencraft.server.cmd.Command;
import org.opencraft.server.cmd.CommandParameters;
import org.opencraft.server.model.Player;
import org.opencraft.server.model.World;
import tf.jacobsc.utils.RatingKt;

public class StartCommand implements Command {

  private static final StartCommand INSTANCE = new StartCommand();

  public static StartCommand getCommand() {
    return INSTANCE;
  }

  @Override
  public void execute(Player player, CommandParameters params) {
    if ((player.isOp()) || player.isVIP()) {
      int quality = RatingKt.matchQuality();
      World.getWorld().broadcast("- &aGame will start in 10 seconds!");
      World.getWorld().broadcast("- &aGame is rated. Game quality is " + quality + "%");
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    Thread.sleep(10 * 1000);
                  } catch (InterruptedException ex) {
                  }

                  for (Player other : World.getWorld().getPlayerList().getPlayers()) {
                    if (other.team != -1) {
                      other.sendToTeamSpawn();
                    }
                  }

                  World.getWorld().getGameMode().tournamentGameStarted = true;
                  World.getWorld().getGameMode().gameStartTime = System.currentTimeMillis();
                  World.getWorld().broadcast("- &aThe game has started!");

                  // Hide other spectators during tournament games for viewability
                  for (Player p : World.getWorld().getPlayerList().getPlayers()) {
                    if (p.team != -1) {
                      continue;
                    }

                    for (Player other : World.getWorld().getPlayerList().getPlayers()) {
                      // Don't hide self or non-spec players
                      if (other == p || other.team != -1) {
                        continue;
                      }

                      p.getActionSender().sendRemoveEntity(other); // Hide their player entity
                    }
                  }
                }
              })
          .start();
    } else {
      player.getActionSender().sendChatMessage("You must be OP to do that!");
    }
  }
}
