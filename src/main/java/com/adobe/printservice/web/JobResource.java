package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.service.JobService;
import com.adobe.printservice.service.JobWorker;
import com.adobe.printservice.web.dto.JobCreateRequest;
import com.adobe.printservice.web.dto.JobResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobService jobService;
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);


    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobCreateRequest request) {
        Job job = jobService.submitJob(request.templateId(), request.parameters());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable String id) {
        return jobService.getJob(id)
                .map(job -> ResponseEntity.ok(JobResponse.from(job)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<JobResponse> listJobs(@RequestParam(required = false) JobStatus status) {
        return jobService.listJobs(Optional.ofNullable(status)).stream()
                .map(JobResponse::from)
                .toList();
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<String> getJobResult(@PathVariable String id) {
        Job job = jobService.getJob(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.getStatus() == JobStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Job processing failed"
            );
        }

        if (job.getStatus() != JobStatus.DONE) {
            throw new IllegalArgumentException(
                    "Job processing not finished yet"
            );
        }

        return ResponseEntity.ok(job.getResultContent());

    }
}
