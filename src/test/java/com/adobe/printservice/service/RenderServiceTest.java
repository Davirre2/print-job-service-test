package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "worker.poll-delay-ms=999999",
        "render.transient-failure-probability=0.0"
})
class RenderServiceTest {

    @Autowired
    private JobWorker jobWorker;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private RenderService renderService;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }


    @Test
    void whenRenderSucceeds_shouldMarkDone() throws Exception {
        Job job = createQueuedJob(0);
        job = jobRepository.save(job);

        String resultContent = "Rendered PDF content";
        when(renderService.render(any(Job.class))).thenReturn(resultContent);

        jobWorker.processNextJob();

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getResultContent()).isEqualTo(resultContent);
        assertThat(updated.getErrorMessage()).isNull();
    }

    @Test
    void whenRenderThrowsRuntimeException_shouldRetryOrFail() throws Exception {
        Job job = createQueuedJob(0);
        job = jobRepository.save(job);

        doThrow(new RuntimeException("Unexpected render error"))
                .when(renderService).render(any(Job.class));

        // When
        jobWorker.processNextJob();

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getErrorMessage()).contains("Render failed: Unexpected render error");
    }

    @Test
    void whenRenderThrowsInterruptedException_shouldRetry() throws Exception {
        Job job = createQueuedJob(0);
        job = jobRepository.save(job);

        doThrow(new InterruptedException("Render interrupted"))
                .when(renderService).render(any(Job.class));

        // When
        jobWorker.processNextJob();

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getErrorMessage()).isEqualTo("Render interrupted");
    }

    private Job createQueuedJob(int attempts) {
        Job job = new Job();
        job.setTemplateId("template-123");
        job.setParameters(Map.of("key", "value"));
        job.setStatus(JobStatus.QUEUED);
        job.setAttempts(attempts);
        return job;
    }
}