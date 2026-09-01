package com.adobe.printservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectsCountResponse {

    private Long jobCount;
    private Long templateCount;

    public ObjectsCountResponse() {
    }

    public ObjectsCountResponse(Long jobCount, Long templateCount) {
        this.jobCount = jobCount;
        this.templateCount = templateCount;
    }

    public Long getJobCount() {
        return jobCount;
    }

    public void setJobCount(Long jobCount) {
        this.jobCount = jobCount;
    }

    public Long getTemplateCount() {
        return templateCount;
    }

    public void setTemplateCount(Long templateCount) {
        this.templateCount = templateCount;
    }
}