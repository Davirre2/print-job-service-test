package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobWorkerTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private JobService jobService;

    @Autowired
    private JobWorker jobWorker;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void workerProcessesQueuedJobWithoutHttpRequest() {
        Job submitted = jobService.submitJob(INVOICE_TEMPLATE_ID, java.util.Map.of("recipient", "test"));

        jobWorker.processNextJob();

        Job processed = jobRepository.findById(submitted.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(processed.getAttempts()).isEqualTo(1);
        assertThat(processed.getResultContent()).isEqualTo("Rendered job " + submitted.getId());
        assertThat(processed.getErrorMessage()).isNull();
    }
}
