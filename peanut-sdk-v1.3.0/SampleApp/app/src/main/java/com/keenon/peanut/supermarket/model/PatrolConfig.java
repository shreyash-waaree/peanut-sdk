package com.keenon.peanut.supermarket.model;

import java.util.ArrayList;
import java.util.List;

public class PatrolConfig {
  private String patrolMode = "loop";
  private int dwellSeconds = 30;
  private List<Waypoint> waypoints = new ArrayList<>();

  public String getPatrolMode() {
    return patrolMode;
  }

  public void setPatrolMode(String patrolMode) {
    this.patrolMode = patrolMode;
  }

  public int getDwellSeconds() {
    return dwellSeconds;
  }

  public void setDwellSeconds(int dwellSeconds) {
    this.dwellSeconds = dwellSeconds;
  }

  public List<Waypoint> getWaypoints() {
    return waypoints;
  }

  public void setWaypoints(List<Waypoint> waypoints) {
    this.waypoints = waypoints != null ? waypoints : new ArrayList<Waypoint>();
  }
}
