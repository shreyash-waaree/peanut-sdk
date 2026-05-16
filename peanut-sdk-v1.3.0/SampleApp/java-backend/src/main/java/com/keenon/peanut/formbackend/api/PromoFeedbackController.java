package com.keenon.peanut.formbackend.api;

import com.keenon.peanut.formbackend.model.FeedbackSubmission;
import com.keenon.peanut.formbackend.store.PromoNdjsonStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class PromoFeedbackController {

  private final PromoNdjsonStore store;

  public PromoFeedbackController(PromoNdjsonStore store) {
    this.store = store;
  }

  @PostMapping("/promo-feedback")
  public ResponseEntity<Map<String, String>> submitFeedback(@RequestBody FeedbackSubmission body) {
    if (isBlank(body.getProduct())) {
      return badRequest("product is required");
    }
    if (body.getRating() < 1 || body.getRating() > 5) {
      return badRequest("rating must be between 1 and 5");
    }
    String id = UUID.randomUUID().toString();
    body.setId(id);
    body.setReceivedAt(System.currentTimeMillis());
    try {
      store.appendFeedback(body);
    } catch (IOException e) {
      return ResponseEntity.status(500).body(error("could not store"));
    }
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "ok");
    resp.put("id", id);
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/admin/feedback")
  public ResponseEntity<List<FeedbackSubmission>> getFeedback(
      @RequestParam(defaultValue = "50") int limit) throws IOException {
    return ResponseEntity.ok(store.readLastFeedback(limit));
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static ResponseEntity<Map<String, String>> badRequest(String msg) {
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "error");
    resp.put("message", msg);
    return ResponseEntity.badRequest().body(resp);
  }

  private static Map<String, String> error(String msg) {
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "error");
    resp.put("message", msg);
    return resp;
  }
}
