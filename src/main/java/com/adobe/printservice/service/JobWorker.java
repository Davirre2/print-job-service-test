package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
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

    public JobWorker(JobService jobService, RenderService renderService) {
        this.jobService = jobService;
        this.renderService = renderService;
    }

    @Scheduled(fixedDelayString = "${worker.poll-delay-ms:250}")
    public void processNextJob() {
        Optional<Job> claimedJob = jobService.claimNextJob();
        claimedJob.ifPresent(this::render);
    }

    private void render(Job job) {
        try {
            String result = renderService.render(job);
            jobService.markDone(job.getId(), result);
        } catch (RenderService.TransientRenderException e) {
            jobService.markAttemptFailed(job.getId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobService.markAttemptFailed(job.getId(), "Render interrupted");
        } catch (RuntimeException e) {
            jobService.markAttemptFailed(job.getId(), "Render failed: " + e.getMessage());
        }
    }
}
