package com.keenon.peanut.supermarket.model;

import java.util.ArrayList;
import java.util.List;

public class FaqConfig {
  private String productName = "";
  private List<FaqTrigger> triggers = new ArrayList<>();
  private String defaultReply = "";

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public List<FaqTrigger> getTriggers() {
    return triggers;
  }

  public void setTriggers(List<FaqTrigger> triggers) {
    this.triggers = triggers != null ? triggers : new ArrayList<FaqTrigger>();
  }

  public String getDefaultReply() {
    return defaultReply;
  }

  public void setDefaultReply(String defaultReply) {
    this.defaultReply = defaultReply;
  }
}
