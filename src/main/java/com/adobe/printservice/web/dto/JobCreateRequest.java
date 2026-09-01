package com.adobe.printservice.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Payload for {@code POST /jobs}.
 */
public record JobCreateRequest(

        @NotBlank(message = "templateId is required")
        String templateId,

        Map<String, Object> parameters
) {
}
