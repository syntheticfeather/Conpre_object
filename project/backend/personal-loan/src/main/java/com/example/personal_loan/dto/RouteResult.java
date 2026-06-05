package com.example.personal_loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.personal_loan.enums.ChatIntent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteResult {

    private ChatIntent intent;
    private double confidence;   // 0.0 ~ 1.0
    private String action;       // WORKFLOW or AGENT

    public RouteResult() {}

    public ChatIntent getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = ChatIntent.fromString(intent); }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
