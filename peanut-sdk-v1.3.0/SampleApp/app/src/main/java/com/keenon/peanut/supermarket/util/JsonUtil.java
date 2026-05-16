package com.keenon.peanut.supermarket.util;

import com.keenon.peanut.supermarket.model.FaqConfig;
import com.keenon.peanut.supermarket.model.FaqTrigger;
import com.keenon.peanut.supermarket.model.PatrolConfig;
import com.keenon.peanut.supermarket.model.Waypoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtil {

  private JsonUtil() {}

  public static FaqConfig parseFaqConfig(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    FaqConfig cfg = new FaqConfig();
    String pn = root.optString("product_name", null);
    if (pn == null || pn.isEmpty()) {
      pn = root.optString("productName", "");
    }
    cfg.setProductName(pn);
    cfg.setDefaultReply(root.optString("default_reply", root.optString("defaultReply", "")));
    JSONArray arr = root.optJSONArray("triggers");
    List<FaqTrigger> triggers = new ArrayList<>();
    if (arr != null) {
      for (int i = 0; i < arr.length(); i++) {
        JSONObject t = arr.getJSONObject(i);
        FaqTrigger ft = new FaqTrigger();
        JSONArray kw = t.optJSONArray("keywords");
        List<String> kws = new ArrayList<>();
        if (kw != null) {
          for (int j = 0; j < kw.length(); j++) {
            kws.add(kw.optString(j, ""));
          }
        }
        ft.setKeywords(kws);
        ft.setReply(t.optString("reply", ""));
        ft.setNavigateToForm(t.optBoolean("navigate_to_form", t.optBoolean("navigateToForm", false)));
        triggers.add(ft);
      }
    }
    cfg.setTriggers(triggers);
    return cfg;
  }

  public static PatrolConfig parsePatrolConfig(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    PatrolConfig cfg = new PatrolConfig();
    String mode = root.optString("patrol_mode", "loop");
    if (!"once".equals(mode) && !"loop".equals(mode)) {
      mode = "loop";
    }
    cfg.setPatrolMode(mode);
    int dwell = root.optInt("dwell_seconds", 30);
    if (dwell < 5) dwell = 5;
    if (dwell > 300) dwell = 300;
    cfg.setDwellSeconds(dwell);
    JSONArray wps = root.optJSONArray("waypoints");
    List<Waypoint> list = new ArrayList<>();
    if (wps != null) {
      for (int i = 0; i < wps.length(); i++) {
        JSONObject w = wps.getJSONObject(i);
        Waypoint wp = new Waypoint();
        wp.setId(w.optString("id", ""));
        wp.setLabel(w.optString("label", ""));
        list.add(wp);
      }
    }
    cfg.setWaypoints(list);
    return cfg;
  }
}
