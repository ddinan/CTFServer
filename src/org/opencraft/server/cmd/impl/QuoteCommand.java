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
package org.opencraft.server.cmd.impl;

import org.opencraft.server.Server;
import org.opencraft.server.cmd.Command;
import org.opencraft.server.cmd.CommandParameters;
import org.opencraft.server.model.Player;
import org.opencraft.server.model.World;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class QuoteCommand implements Command {
  private static final QuoteCommand INSTANCE = new QuoteCommand();
  private static final ArrayList<String> quotes = new ArrayList<String>();

  static {
    try {
      String[] array = Server.readFileAsString("quotes.txt").split("\n");
      for (String q : array) {
        quotes.add(q);
      }
    } catch (IOException ex) {
      Server.log(ex);
    }
  }

  public static QuoteCommand getCommand() {
    return INSTANCE;
  }

  @Override
  public void execute(Player player, CommandParameters params) {
    if (params.getArgumentCount() == 0) {
      String quote = quotes.get((int) (Math.random() * quotes.size()));
      World.getWorld().broadcast(">> &3" + quote);
    } else {
      if (params.getStringArgument(0).equals("add") && params.getArgumentCount() >= 2) {
        String text = "";
        for (int i = 1; i < params.getArgumentCount(); i++) {
          text += " " + params.getStringArgument(i);
        }
        text = text.substring(1);
        quotes.add(text);
        try {
          FileOutputStream out = new FileOutputStream("quotes.txt", true);
          out.write((text + "\n").getBytes());
          out.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
        World.getWorld()
            .broadcast("- &3Quote \"" + text + "\" added by " + player.getColoredName());
      } else {
        player.getActionSender().sendChatMessage("- &3Use /quote add [message] to add a quote.");
      }
    }
  }
}
