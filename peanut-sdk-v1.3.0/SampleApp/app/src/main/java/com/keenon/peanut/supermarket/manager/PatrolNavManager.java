package com.keenon.peanut.supermarket.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.keenon.peanut.sample.bean.MyPoint;
import com.keenon.peanut.sample.manager.NavManager;
import com.keenon.peanut.supermarket.model.PatrolConfig;
import com.keenon.peanut.supermarket.model.Waypoint;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrol loop using existing {@link NavManager} (Peanut SDK).
 */
public class PatrolNavManager implements Navigation.Listener {
  private static final String TAG = "PatrolNavManager";

  private final Context appContext;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private PatrolConfig patrolConfig;
  private boolean patrolling;
  private boolean paused;
  private int currentIndex;
  private Runnable dwellRunnable;

  public PatrolNavManager(Context context) {
    appContext = context.getApplicationContext();
  }

  public void init(PatrolConfig config) {
    this.patrolConfig = config;
    currentIndex = 0;
    if (config == null
        || config.getWaypoints() == null
        || config.getWaypoints().isEmpty()) {
      Log.w(TAG, "Patrol disabled: no waypoints");
      return;
    }
    List<MyPoint> points = buildMyPoints(config);
    NavManager nav = NavManager.getInstance();
    nav.init(this, 0, -1, true);
    nav.setSpeed(80);
    nav.setTargets(points);
    nav.prepare();
  }

  private List<MyPoint> buildMyPoints(PatrolConfig config) {
    int dwellMs = Math.max(5, config.getDwellSeconds()) * 1000;
    List<MyPoint> list = new ArrayList<>();
    for (Waypoint wp : config.getWaypoints()) {
      MyPoint p = new MyPoint();
      p.setManualControl(false);
      p.setDuration(dwellMs);
      RouteNode node = new RouteNode();
      int id = parseWaypointId(wp.getId());
      node.setId(id);
      node.setName(wp.getLabel() != null ? wp.getLabel() : ("Point " + id));
      p.setRouteNode(node);
      list.add(p);
    }
    return list;
  }

  private static int parseWaypointId(String raw) {
    if (raw == null) return 0;
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      Log.w(TAG, "Waypoint id must be numeric for RouteNode: " + raw);
      return 0;
    }
  }

  public void startPatrol() {
    if (patrolConfig == null
        || patrolConfig.getWaypoints() == null
        || patrolConfig.getWaypoints().isEmpty()) {
      return;
    }
    patrolling = true;
    paused = false;
    NavManager.getInstance().readyGo(true);
  }

  public void pausePatrol() {
    paused = true;
    cancelDwell();
    NavManager.getInstance().readyGo(false);
  }

  public void resumePatrol() {
    paused = false;
    if (patrolling) {
      NavManager.getInstance().readyGo(true);
    }
  }

  public void stopPatrol() {
    patrolling = false;
    paused = false;
    cancelDwell();
    NavManager.getInstance().stop();
  }

  public void release() {
    stopPatrol();
    NavManager.getInstance().release();
  }

  public String getCurrentWaypointLabel() {
    if (patrolConfig == null
        || patrolConfig.getWaypoints() == null
        || patrolConfig.getWaypoints().isEmpty()) {
      return "";
    }
    int i = currentIndex;
    if (i < 0 || i >= patrolConfig.getWaypoints().size()) {
      i = 0;
    }
    Waypoint w = patrolConfig.getWaypoints().get(i);
    return w.getLabel() != null ? w.getLabel() : "";
  }

  private void cancelDwell() {
    if (dwellRunnable != null) {
      mainHandler.removeCallbacks(dwellRunnable);
      dwellRunnable = null;
    }
  }

  private void scheduleDwellAdvance() {
    cancelDwell();
    if (patrolConfig == null || paused || !patrolling) return;
    long ms = patrolConfig.getDwellSeconds() * 1000L;
    dwellRunnable =
        () -> {
          if (paused || !patrolling) return;
          NavManager nav = NavManager.getInstance();
          nav.nextDes();
          nav.readyGo(true);
        };
    mainHandler.postDelayed(dwellRunnable, ms);
  }

  @Override
  public void onStateChanged(int state, int schedule) {
    if (state == Navigation.STATE_DESTINATION) {
      if (!patrolling || paused) return;
      scheduleDwellAdvance();
    }
    if (state == Navigation.STATE_COLLISION
        || state == Navigation.STATE_BLOCKED
        || state == Navigation.STATE_BLOCKING) {
      Log.w(TAG, "Nav state issue: " + state + ", skipping waypoint");
      mainHandler.post(
          () -> {
            if (!patrolling || paused) return;
            NavManager.getInstance().nextDes();
            NavManager.getInstance().readyGo(true);
          });
    }
  }

  @Override
  public void onRouteNode(int index, RouteNode routeNode) {
    currentIndex = index;
  }

  @Override
  public void onRoutePrepared(RouteNode... routeNodes) {
    // Route preparation fires during init; do not auto-drive unless patrol was explicitly started.
    if (patrolling && !paused) {
      NavManager.getInstance().readyGo(true);
    } else {
      NavManager.getInstance().readyGo(false);
    }
  }

  @Override
  public void onDistanceChanged(float distance) {}

  @Override
  public void onError(int code) {
    Log.w(TAG, "onError " + code + ", advance");
    mainHandler.post(
        () -> {
          if (!patrolling || paused) return;
          NavManager.getInstance().nextDes();
          NavManager.getInstance().readyGo(true);
        });
  }

  @Override
  public void onEvent(int event) {}
}
