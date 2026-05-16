package com.keenon.peanut.supermarket.backend;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class OfflineCache {
  private final File cacheFile;
  private final ReentrantLock lock = new ReentrantLock();

  public OfflineCache(Context context) {
    cacheFile = new File(context.getFilesDir(), "offline_queue.ndjson");
  }

  public void store(String type, JSONObject payload) throws IOException {
    lock.lock();
    try {
      JSONObject entry = new JSONObject();
      try {
        entry.put("id", UUID.randomUUID().toString());
        entry.put("type", type);
        entry.put("payload", payload.toString());
        entry.put("cachedAt", System.currentTimeMillis());
      } catch (JSONException e) {
        throw new IOException(e);
      }
      FileWriter fw = new FileWriter(cacheFile, true);
      fw.write(entry.toString());
      fw.write("\n");
      fw.close();
    } finally {
      lock.unlock();
    }
  }

  public List<Entry> getAll() throws IOException {
    lock.lock();
    try {
      List<Entry> out = new ArrayList<>();
      if (!cacheFile.exists()) return out;
      BufferedReader br = new BufferedReader(new FileReader(cacheFile));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        try {
          JSONObject o = new JSONObject(line);
          Entry e = new Entry();
          e.id = o.optString("id", "");
          e.type = o.optString("type", "");
          e.json = o.optString("payload", "");
          out.add(e);
        } catch (JSONException ignored) {
        }
      }
      br.close();
      return out;
    } finally {
      lock.unlock();
    }
  }

  public void remove(String id) throws IOException {
    lock.lock();
    try {
      List<Entry> keep = new ArrayList<>();
      for (Entry e : getAllUnlocked()) {
        if (!id.equals(e.id)) keep.add(e);
      }
      FileWriter fw = new FileWriter(cacheFile, false);
      for (Entry e : keep) {
        JSONObject o = new JSONObject();
        try {
          o.put("id", e.id);
          o.put("type", e.type);
          o.put("payload", e.json);
          o.put("cachedAt", System.currentTimeMillis());
        } catch (JSONException ex) {
          throw new IOException(ex);
        }
        fw.write(o.toString());
        fw.write("\n");
      }
      fw.close();
    } finally {
      lock.unlock();
    }
  }

  private List<Entry> getAllUnlocked() throws IOException {
    List<Entry> out = new ArrayList<>();
    if (!cacheFile.exists()) return out;
    BufferedReader br = new BufferedReader(new FileReader(cacheFile));
    String line;
    while ((line = br.readLine()) != null) {
      if (line.trim().isEmpty()) continue;
      try {
        JSONObject o = new JSONObject(line);
        Entry e = new Entry();
        e.id = o.optString("id", "");
        e.type = o.optString("type", "");
        e.json = o.optString("payload", "");
        out.add(e);
      } catch (JSONException ignored) {
      }
    }
    br.close();
    return out;
  }

  public void rewriteAll(List<Entry> entries) throws IOException {
    lock.lock();
    try {
      FileWriter fw = new FileWriter(cacheFile, false);
      for (Entry e : entries) {
        JSONObject o = new JSONObject();
        try {
          o.put("id", e.id);
          o.put("type", e.type);
          o.put("payload", e.json);
          o.put("cachedAt", System.currentTimeMillis());
        } catch (JSONException ex) {
          throw new IOException(ex);
        }
        fw.write(o.toString());
        fw.write("\n");
      }
      fw.close();
    } finally {
      lock.unlock();
    }
  }

  public static class Entry {
    public String id;
    public String type;
    public String json;
  }
}
