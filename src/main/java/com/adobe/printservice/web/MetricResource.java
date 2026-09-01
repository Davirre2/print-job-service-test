package com.adobe.printservice.web;

import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.dto.ObjectsCountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/objects_count")
public class MetricResource {

    JobRepository jobRepository;
    RenderTemplateRepository renderTemplateRepository;

    public MetricResource(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
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


    @GetMapping("/templates")
    public ResponseEntity<ObjectsCountResponse> countTemplates() {
        long templates = renderTemplateRepository.count();

        return ResponseEntity.ok(new ObjectsCountResponse(null, templates));
    }
}