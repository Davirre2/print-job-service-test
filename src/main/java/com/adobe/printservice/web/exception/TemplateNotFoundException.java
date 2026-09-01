package com.adobe.printservice.web.exception;

/**
 * Raised when a job is submitted against a templateId that has no matching {@code RenderTemplate}.
 * Mapped to {@code 400 Bad Request} by {@link GlobalExceptionHandler}.
 */
public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String templateId) {
        super("No template found with id '" + templateId + "'");
    }
}
