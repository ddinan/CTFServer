package org.opencraft.server.model;

import java.io.File;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class AchievementManager {
  public Map<String, Achievement> achievements = new HashMap<>();
  public Map<String, Set<String>> playerAchievements = new HashMap<>();

  // Load achievements from XML
  public void loadAchievements(String filePath) {
    try {
      File file = new File(filePath);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(file);
      NodeList achievementNodes = doc.getElementsByTagName("achievement");

      for (int i = 0; i < achievementNodes.getLength(); i++) {
        Node node = achievementNodes.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          String id = element.getAttribute("id");
          String description = element.getElementsByTagName("description").item(0).getTextContent();
          int targetProgress = Integer.parseInt(element.getElementsByTagName("targetProgress").item(0).getTextContent());
          achievements.put(id, new Achievement(id, description, targetProgress));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void loadPlayerAchievements(String filePath) {
    try {
      File file = new File(filePath);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(file);
      NodeList playerNodes = doc.getElementsByTagName("player");

      for (int i = 0; i < playerNodes.getLength(); i++) {
        Node node = playerNodes.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          String username = element.getAttribute("username");
          Set<String> earnedAchievements = new HashSet<>();

          NodeList achievementNodes = element.getElementsByTagName("achievement");
          for (int j = 0; j < achievementNodes.getLength(); j++) {
            earnedAchievements.add(achievementNodes.item(j).getTextContent());
          }

          playerAchievements.put(username, earnedAchievements);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void savePlayerAchievements(String filePath) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.newDocument();
      Element root = doc.createElement("playerAchievements");
      doc.appendChild(root);

      // Loop through each player (username) and their earned achievements
      for (Map.Entry<String, Set<String>> entry : playerAchievements.entrySet()) {
        String username = entry.getKey();
        Set<String> earnedAchievements = entry.getValue();

        // Create a player element (using username as the ID)
        Element playerElement = doc.createElement("player");
        playerElement.setAttribute("username", username);

        // Create an element for each achievement the player has earned
        for (String achievementId : earnedAchievements) {
          Element achievementElement = doc.createElement("achievement");
          achievementElement.appendChild(doc.createTextNode(achievementId));
          playerElement.appendChild(achievementElement);
        }

        root.appendChild(playerElement);
      }

      // Write to file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(filePath));
      transformer.transform(source, result);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void updateProgress(String username, String achievementId, int progress) {
    Achievement achievement = achievements.get(achievementId);
    if (achievement != null) {
      achievement.updateProgress(progress);
      if (achievement.isCompleted() && !hasPlayerEarnedAchievement(username, achievementId)) {
        grantAchievement(username, achievementId);
      }
    }
  }

  public void incrementProgress(String username, String achievementId, int increment) {
    Achievement achievement = achievements.get(achievementId);
    if (achievement != null) {
      achievement.updateProgress(achievement.getCurrentProgress() + increment);
      if (achievement.isCompleted() && !hasPlayerEarnedAchievement(username, achievementId)) {
        grantAchievement(username, achievementId);
      }
    }
  }

  public boolean hasPlayerEarnedAchievement(String username, String achievementId) {
    Set<String> playerAchievementsSet = playerAchievements.getOrDefault(username, new HashSet<>());
    return playerAchievementsSet.contains(achievementId);
  }

  private void grantAchievement(String username, String achievementId) {
    playerAchievements.computeIfAbsent(username, k -> new HashSet<>()).add(achievementId);
    World.getWorld().broadcast(username + " has earned the achievement: " + achievementId);
    savePlayerAchievements("playerAchievements.xml");
  }


  public Map<String, Achievement> getAchievements() {
    return achievements;
  }
}
