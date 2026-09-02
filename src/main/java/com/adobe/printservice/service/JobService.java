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
import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    public static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final JobProcessingService jobProcessingService;
    private final RenderService renderService;

    public JobService(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository,
                      JobProcessingService jobProcessingService, RenderService renderService) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.jobProcessingService = jobProcessingService;
        this.renderService = renderService;
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
    public Optional<Job> claimNextJob() throws InterruptedException, RenderService.TransientRenderException {
        return jobProcessingService.processNextJob();
    }

    @Transactional(readOnly = true)
    public Optional<Job> getJob(String id) {
        return jobRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Job> listJobs(Optional<JobStatus> status) {
        return status
                .map(jobRepository::findAllByStatusOrderByCreatedAtAsc)
                .orElseGet(jobRepository::findAllByOrderByCreatedAtAsc);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyJobProcessing() {
        return jobRepository.existsByStatus(JobStatus.PROCESSING);
    }

    @Transactional(readOnly = true)
    public boolean hasQueuedJobs() {
        return jobRepository.existsByStatus(JobStatus.QUEUED);
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
    public boolean markAttemptFailed(String jobId, String errorMessage) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getStatus() == JobStatus.PROCESSING)
                .map(job -> {
                    job.setErrorMessage(errorMessage);
                    job.setUpdatedAt(Instant.now());

                    boolean willRetry = job.getAttempts() < MAX_ATTEMPTS;

                    if (willRetry) {
                        job.setStatus(JobStatus.QUEUED);
                    } else {
                        job.setStatus(JobStatus.FAILED);
                    }

                    jobRepository.save(job);

                    return willRetry;
                })
                .orElse(false);
    }
}
