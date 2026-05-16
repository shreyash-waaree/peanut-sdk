package com.keenon.peanut.supermarket.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FeedbackPayload {
  private String product;
  private int rating;
  private String wouldBuy;
  private String comments;
  private long submittedAt;
  private String robotId;

  public FeedbackPayload(
      String product,
      int rating,
      String wouldBuy,
      String comments,
      long submittedAt,
      String robotId) {
    this.product = product;
    this.rating = rating;
    this.wouldBuy = wouldBuy;
    this.comments = comments;
    this.submittedAt = submittedAt;
    this.robotId = robotId;
  }

  public JSONObject toJson() throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("product", product);
    obj.put("rating", rating);
    obj.put("wouldBuy", wouldBuy == null ? "" : wouldBuy);
    obj.put("comments", comments == null ? "" : comments);
    obj.put("submittedAt", submittedAt);
    obj.put("robotId", robotId == null ? "" : robotId);
    return obj;
  }
}
