package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class JobProcessingService {

    private final JobRepository jobRepository;

    public JobProcessingService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<Job> processNextJob() {
        Optional<Job> firstJob = jobRepository.findNextJobForUpdate(JobStatus.QUEUED);

        if (firstJob.isPresent()) {
            Job job = firstJob.get();
            job.setStatus(JobStatus.PROCESSING);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
        }

        return firstJob;
    }
}