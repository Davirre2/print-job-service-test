package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Polls for queued jobs independently of incoming HTTP requests.
 */
@Component
public class JobWorker {

    private final JobService jobService;
    private final RenderService renderService;

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    public JobWorker(JobService jobService, RenderService renderService) {
        this.jobService = jobService;
        this.renderService = renderService;
    }

    @Scheduled(fixedDelayString = "${worker.poll-delay-ms:3000}")
    public void processNextJob() {
        Optional<Job> claimedJob = jobService.claimNextJob();
        claimedJob.ifPresent(job -> {
            log.info("Job claimed for processing: {} (attempt {})", job.getId(), job.getAttempts());
            render(job);
        });
    }

    private void render(Job job) {
        try {
            String result = renderService.render(job);

            jobService.markDone(job.getId(), result);

            log.info(
                    "Processed successfully, the job is now done: {}",
                    job.getId()
            );

        } catch (RenderService.TransientRenderException e) {
            handleRenderFailure(job, e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleRenderFailure(job, "Render interrupted");

        } catch (RuntimeException e) {
            handleRenderFailure(
                    job,
                    "Render failed: " + e.getMessage()
            );
        }
    }

    private void handleRenderFailure(Job job, String errorMessage) {
        boolean willRetry = jobService.markAttemptFailed(
                job.getId(),
                errorMessage
        );

        if (willRetry) {
            log.warn(
                    "Failed, trying again. Job: {}, attempt: {} of {}",
                    job.getId(),
                    job.getAttempts(),
                    JobService.MAX_ATTEMPTS
            );
        } else {
            log.error(
                    "Max attempts have been reached, processing failed. Job: {}",
                    job.getId()
            );
        }
    }
}
