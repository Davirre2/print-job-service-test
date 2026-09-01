package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.web.exception.TemplateNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class JobService {

    public static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;

    public JobService(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
    }

    /**
     * Validates the template reference and persists a new job in QUEUED status.
     * No rendering work happens on the HTTP request thread.
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

    /**
     * Atomically claims the oldest queued job for the background worker.
     * The transaction ends before the actual render begins.
     */
    @Transactional
    public Optional<Job> claimNextJob() {
        return jobRepository.findFirstByStatusOrderByCreatedAtAsc(JobStatus.QUEUED)
                .map(job -> {
                    job.setStatus(JobStatus.PROCESSING);
                    job.setAttempts(job.getAttempts() + 1);
                    job.setErrorMessage(null);
                    job.setUpdatedAt(Instant.now());
                    return jobRepository.save(job);
                });
    }

    @Transactional
    public void markDone(String jobId, String result) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == JobStatus.PROCESSING) {
                job.setResultContent(result);
                job.setErrorMessage(null);
                job.setStatus(JobStatus.DONE);
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }
        });
    }

    @Transactional
    public void markAttemptFailed(String jobId, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != JobStatus.PROCESSING) {
                return;
            }

            job.setErrorMessage(errorMessage);
            job.setUpdatedAt(Instant.now());

            if (job.getAttempts() < MAX_ATTEMPTS) {
                job.setStatus(JobStatus.QUEUED);
            } else {
                job.setStatus(JobStatus.FAILED);
            }

            jobRepository.save(job);
        });
    }
}
