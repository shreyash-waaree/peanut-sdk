package com.keenon.peanut.supermarket.model;

import org.json.JSONException;
import org.json.JSONObject;

public class InterestPayload {
  private String product;
  private String name;
  private String mobile;
  private String email;
  private String contactTime;
  private int quantity;
  private String comments;
  private long submittedAt;
  private String robotId;
  private String location;

  public InterestPayload(
      String product,
      String name,
      String mobile,
      String email,
      String contactTime,
      int quantity,
      String comments,
      long submittedAt,
      String robotId,
      String location) {
    this.product = product;
    this.name = name;
    this.mobile = mobile;
    this.email = email;
    this.contactTime = contactTime;
    this.quantity = quantity;
    this.comments = comments;
    this.submittedAt = submittedAt;
    this.robotId = robotId;
    this.location = location;
  }

  public JSONObject toJson() throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("product", product);
    obj.put("name", name);
    obj.put("mobile", mobile);
    obj.put("email", email == null ? "" : email);
    obj.put("contactTime", contactTime == null ? "" : contactTime);
    obj.put("quantity", quantity);
    obj.put("comments", comments == null ? "" : comments);
    obj.put("submittedAt", submittedAt);
    obj.put("robotId", robotId == null ? "" : robotId);
    obj.put("location", location == null ? "" : location);
    return obj;
  }
}
