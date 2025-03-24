package org.opencraft.server.model;

import org.opencraft.server.Server;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;

public class ClanManager {

  private Map<String, Clan> clans = new HashMap<>();

  public void loadClans(String filePath) {
    try {
      File file = new File(filePath);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(file);
      NodeList clanNodes = doc.getElementsByTagName("clan");

      for (int i = 0; i < clanNodes.getLength(); i++) {
        Node node = clanNodes.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          String name = element.getAttribute("name");
          String leader = element.getAttribute("leader");
          Set<String> members = new HashSet<>();

          NodeList memberNodes = element.getElementsByTagName("member");
          for (int j = 0; j < memberNodes.getLength(); j++) {
            String member = memberNodes.item(j).getTextContent();
            members.add(member);
            Server.playerClans.put(member, name);
          }

          clans.put(name, new Clan(name, leader, members));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void saveClans(String filePath) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.newDocument();
      Element root = doc.createElement("clans");
      doc.appendChild(root);

      for (Clan clan : clans.values()) {
        Element clanElement = doc.createElement("clan");
        clanElement.setAttribute("name", clan.getName());
        clanElement.setAttribute("leader", clan.getLeader());

        for (String member : clan.getMembers()) {
          Element memberElement = doc.createElement("member");
          memberElement.appendChild(doc.createTextNode(member));
          clanElement.appendChild(memberElement);
        }

        root.appendChild(clanElement);
      }

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(filePath));
      transformer.transform(source, result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void createClan(String name, String leader) {
    if (!clans.containsKey(name)) {
      clans.put(name, new Clan(name, leader, new HashSet<>(Collections.singleton(leader))));
      saveClans("clans.xml");
    }
  }

  public void removeClan(String clanName) {
    if (clans.containsKey(clanName)) {
      clans.remove(clanName);
      saveClans("clans.xml");
    }
  }

  public void addMember(String clanName, String playerName) {
    Clan clan = clans.get(clanName);
    if (clan != null) {
      clan.getMembers().add(playerName);
      saveClans("clans.xml");
    }
  }

  public void removeMember(String clanName, String playerName) {
    Clan clan = clans.get(clanName);
    if (clan != null) {
      clan.getMembers().remove(playerName);
      saveClans("clans.xml");
    }
  }

  public Map<String, Clan> getClans() {
    return clans;
  }
}
