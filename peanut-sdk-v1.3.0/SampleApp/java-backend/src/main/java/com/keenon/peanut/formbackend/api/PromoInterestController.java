package com.keenon.peanut.formbackend.api;

import com.keenon.peanut.formbackend.model.InterestSubmission;
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
public class PromoInterestController {

  private final PromoNdjsonStore store;

  public PromoInterestController(PromoNdjsonStore store) {
    this.store = store;
  }

  @PostMapping("/promo-interest")
  public ResponseEntity<Map<String, String>> submitInterest(@RequestBody InterestSubmission body) {
    if (isBlank(body.getName())) {
      return badRequest("name is required");
    }
    if (isBlank(body.getMobile()) || !body.getMobile().matches("\\d{10}")) {
      return badRequest("valid 10-digit mobile is required");
    }
    if (isBlank(body.getProduct())) {
      return badRequest("product is required");
    }
    String id = UUID.randomUUID().toString();
    body.setId(id);
    body.setReceivedAt(System.currentTimeMillis());
    try {
      store.appendInterest(body);
    } catch (IOException e) {
      return ResponseEntity.status(500).body(error("could not store"));
    }
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "ok");
    resp.put("id", id);
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/admin/interest")
  public ResponseEntity<List<InterestSubmission>> getInterest(
      @RequestParam(defaultValue = "50") int limit) throws IOException {
    return ResponseEntity.ok(store.readLastInterest(limit));
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
