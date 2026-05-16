package com.keenon.peanut.formbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackSubmission {
  public FeedbackSubmission() {}

  private String id;
  private String product;
  private int rating;
  private String wouldBuy;
  private String comments;
  private long submittedAt;
  private long receivedAt;
  private String robotId;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProduct() {
    return product;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public String getWouldBuy() {
    return wouldBuy;
  }

  public void setWouldBuy(String wouldBuy) {
    this.wouldBuy = wouldBuy;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public long getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(long submittedAt) {
    this.submittedAt = submittedAt;
  }

  public long getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(long receivedAt) {
    this.receivedAt = receivedAt;
  }

  public String getRobotId() {
    return robotId;
  }

  public void setRobotId(String robotId) {
    this.robotId = robotId;
  }
}
