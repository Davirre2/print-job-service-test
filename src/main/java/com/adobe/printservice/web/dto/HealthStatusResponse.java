package com.adobe.printservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthStatusResponse {

    private String status; // "UP" or "DOWN"
    private Boolean ready;
    private Map<String, Object> details;

    public HealthStatusResponse(String status) {
        this.status = status;
    }

    public HealthStatusResponse(String status, Boolean ready, Map<String, Object> details) {
        this.status = status;
        this.ready = ready;
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getReady() {
        return ready;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}