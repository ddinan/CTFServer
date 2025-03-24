package org.opencraft.server.model;

import java.util.Set;

public class Clan {
  private String name;
  private String leader;
  private Set<String> members;

  public Clan(String name, String leader, Set<String> members) {
    this.name = name;
    this.leader = leader;
    this.members = members;
  }

  public String getName() { return name; }
  public String getLeader() { return leader; }
  public Set<String> getMembers() { return members; }
}
