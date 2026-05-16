package com.keenon.peanut.supermarket.manager;

import android.content.Context;
import android.util.Log;

import com.keenon.peanut.supermarket.model.FaqConfig;
import com.keenon.peanut.supermarket.model.FaqTrigger;
import com.keenon.peanut.supermarket.model.PatrolConfig;
import com.keenon.peanut.supermarket.model.Waypoint;
import com.keenon.peanut.supermarket.util.JsonUtil;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class ProductConfig {
  private static final String TAG = "ProductConfig";
  private static final String PROMO_DIR = "promo";
  private static final String FAQ_FILE = "product_faq.json";
  private static final String PATROL_FILE = "patrol_config.json";

  private final Context context;
  private FaqConfig faqConfig = defaultFaq();
  private PatrolConfig patrolConfig = defaultPatrol();

  public ProductConfig(Context context) {
    this.context = context.getApplicationContext();
  }

  public void load() {
    try {
      loadFaqConfig();
    } catch (Exception e) {
      Log.w(TAG, "FAQ load failed, using defaults", e);
      faqConfig = defaultFaq();
    }
    try {
      loadPatrolConfig();
    } catch (Exception e) {
      Log.w(TAG, "Patrol load failed, using defaults", e);
      patrolConfig = defaultPatrol();
    }
  }

  private void loadFaqConfig() throws IOException, JSONException {
    String json = readExternalOrAsset(FAQ_FILE);
    faqConfig = JsonUtil.parseFaqConfig(json);
  }

  private void loadPatrolConfig() throws IOException, JSONException {
    String json = readExternalOrAsset(PATROL_FILE);
    patrolConfig = JsonUtil.parsePatrolConfig(json);
  }

  private String readExternalOrAsset(String name) throws IOException {
    File ext =
        new File(context.getExternalFilesDir(null), PROMO_DIR + File.separator + name);
    if (ext.exists()) {
      return readFile(ext);
    }
    return readAsset(name);
  }

  private static String readFile(File f) throws IOException {
    FileInputStream fis = new FileInputStream(f);
    return readStream(fis);
  }

  private String readAsset(String name) throws IOException {
    InputStream is = context.getAssets().open(name);
    return readStream(is);
  }

  private static String readStream(InputStream is) throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) sb.append(line).append('\n');
    br.close();
    return sb.toString();
  }

  private static FaqConfig defaultFaq() {
    FaqConfig c = new FaqConfig();
    c.setProductName("Promotional Product");
    c.setDefaultReply("Thank you for visiting. See the Interest tab to leave your details.");
    FaqTrigger t = new FaqTrigger();
    t.setKeywords(Collections.singletonList("hello"));
    t.setReply("Hello! Ask me about this product or use the tabs below.");
    t.setNavigateToForm(false);
    c.setTriggers(Collections.singletonList(t));
    return c;
  }

  private static PatrolConfig defaultPatrol() {
    PatrolConfig p = new PatrolConfig();
    p.setPatrolMode("loop");
    p.setDwellSeconds(30);
    Waypoint w = new Waypoint();
    w.setId("1");
    w.setLabel("Point 1");
    p.setWaypoints(Collections.singletonList(w));
    return p;
  }

  public FaqConfig getFaqConfig() {
    return faqConfig;
  }

  public PatrolConfig getPatrolConfig() {
    return patrolConfig;
  }

  public String getProductName() {
    return faqConfig != null ? faqConfig.getProductName() : "";
  }
}
