package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.exception.TemplateNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Business logic for the render-job lifecycle. The controller stays thin and only talks to this
 * class; the background worker that will later move jobs QUEUED -> PROCESSING -> DONE/FAILED
 * should live here too, so every job state change goes through one place.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;

    public JobService(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
    }

    /**
     * Validates the template reference and persists a new job in {@code QUEUED} status.
     * Deliberately does no rendering work itself - actual processing is picked up
     * asynchronously by a background worker, which is not part of this method.
     */
    @Transactional
    public Job submitJob(String templateId, Map<String, Object> parameters) {
        if (!renderTemplateRepository.existsById(templateId)) {
            throw new TemplateNotFoundException(templateId);
        }

        Job job = new Job();
        job.setTemplateId(templateId);
        job.setParameters(parameters == null ? Map.of() : parameters);

        return jobRepository.save(job);
    }
}
