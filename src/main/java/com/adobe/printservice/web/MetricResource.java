package com.adobe.printservice.web;

import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.service.JobService;
import com.adobe.printservice.web.dto.JobResponse;
import com.adobe.printservice.web.dto.ObjectsCountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/objects_count")
public class MetricResource {

    JobRepository jobRepository;
    RenderTemplateRepository renderTemplateRepository;
    JobService jobService;

    public MetricResource(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository, JobService jobService) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ObjectsCountResponse> countAll() {
        long jobs = jobRepository.count();
        long templates = renderTemplateRepository.count();

        return ResponseEntity.ok(new ObjectsCountResponse(jobs, templates));
    }


    @GetMapping("/jobs")
    public ResponseEntity<ObjectsCountResponse> countJobs() {
        long jobs = jobRepository.count();

        return ResponseEntity.ok(new ObjectsCountResponse(jobs, null));
    }

    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<ObjectsCountResponse> countJobsByStatus(@PathVariable JobStatus status){
        long jobs = jobService.listJobs(Optional.ofNullable(status)).stream()
                .map(JobResponse::from)
                .count();

        return ResponseEntity.ok(new ObjectsCountResponse(jobs, null));
    }


    @GetMapping("/templates")
    public ResponseEntity<ObjectsCountResponse> countTemplates() {
        long templates = renderTemplateRepository.count();

        return ResponseEntity.ok(new ObjectsCountResponse(null, templates));
    }
}