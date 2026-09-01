package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.service.JobService;
import com.adobe.printservice.web.dto.JobCreateRequest;
import com.adobe.printservice.web.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Render-job lifecycle: submit, check status, list, and fetch results. Kept as a single
 * resource class (mirrors {@link RenderTemplateResource}) since all four endpoints share the
 * /jobs base path and the same {@link JobService}; request/response shapes live in
 * {@code web.dto}, error mapping lives in {@code web.exception}, so adding the next endpoint
 * here is just another method plus (if needed) another exception + handler.
 */
@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobService jobService;

    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobCreateRequest request) {
        Job job = jobService.submitJob(request.templateId(), request.parameters());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

}
