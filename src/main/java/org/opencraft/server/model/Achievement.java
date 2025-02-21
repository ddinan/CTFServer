package org.opencraft.server.model;

public class Achievement {
  private String id;
  private String description;
  private int targetProgress;
  private int currentProgress;

  public Achievement(String id, String description, int targetProgress) {
    this.id = id;
    this.description = description;
    this.targetProgress = targetProgress;
    this.currentProgress = 0;
  }

  public void updateProgress(int progress) {
    currentProgress = progress;
  }

  public boolean isCompleted() {
    return currentProgress >= targetProgress;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public int getTargetProgress() {
    return targetProgress;
  }

  public int getCurrentProgress() {
    return currentProgress;
  }
}
