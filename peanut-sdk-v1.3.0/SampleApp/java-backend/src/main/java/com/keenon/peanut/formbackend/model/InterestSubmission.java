package com.keenon.peanut.formbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InterestSubmission {
  public InterestSubmission() {}

  private String id;
  private String product;
  private String name;
  private String mobile;
  private String email;
  private String contactTime;
  private int quantity;
  private String comments;
  private long submittedAt;
  private long receivedAt;
  private String robotId;
  private String location;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getContactTime() {
    return contactTime;
  }

  public void setContactTime(String contactTime) {
    this.contactTime = contactTime;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
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

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }
}
