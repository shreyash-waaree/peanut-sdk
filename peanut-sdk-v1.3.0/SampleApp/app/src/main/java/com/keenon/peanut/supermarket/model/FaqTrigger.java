package com.keenon.peanut.supermarket.model;

import java.util.ArrayList;
import java.util.List;

public class FaqTrigger {
  private List<String> keywords = new ArrayList<>();
  private String reply = "";
  private boolean navigateToForm;

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords != null ? keywords : new ArrayList<String>();
  }

  public String getReply() {
    return reply;
  }

  public void setReply(String reply) {
    this.reply = reply;
  }

  public boolean isNavigateToForm() {
    return navigateToForm;
  }

  public void setNavigateToForm(boolean navigateToForm) {
    this.navigateToForm = navigateToForm;
  }
}
