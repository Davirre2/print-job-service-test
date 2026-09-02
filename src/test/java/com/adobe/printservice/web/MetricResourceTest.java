package com.adobe.printservice.web;

import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The job table is shared with every other @SpringBootTest class in this suite (same cached
 * context, same H2 instance), and test class execution order isn't guaranteed. So job-count
 * assertions here are all done as deltas (count a baseline, submit a job, expect baseline + 1)
 * rather than fixed numbers. Template count is asserted directly, since templates are seeded
 * once at startup and nothing in the app mutates them afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MetricResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void countTemplates_returnsSeededTemplateCount() throws Exception {
        mockMvc.perform(get("/objects_count/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateCount").value(3))
                .andExpect(jsonPath("$.jobCount").doesNotExist());
    }

    @Test
    void countJobs_increasesBySubmittedJob() throws Exception {
        long before = jobRepository.count();

        jobService.submitJob(INVOICE_TEMPLATE_ID, Map.of("recipient", "metrics-test"));

        mockMvc.perform(get("/objects_count/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobCount").value(before + 1))
                .andExpect(jsonPath("$.templateCount").doesNotExist());
    }

    @Test
    void countAll_reflectsBothJobsAndTemplates() throws Exception {
        long before = jobRepository.count();

        jobService.submitJob(INVOICE_TEMPLATE_ID, Map.of("recipient", "metrics-test"));

        mockMvc.perform(get("/objects_count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobCount").value(before + 1))
                .andExpect(jsonPath("$.templateCount").value(3));
    }

    @Test
    void countJobsByStatus_reflectsNewlyQueuedJob() throws Exception {
        long before = jobService.listJobs(Optional.of(JobStatus.QUEUED)).size();

        jobService.submitJob(INVOICE_TEMPLATE_ID, Map.of("recipient", "metrics-test"));

        mockMvc.perform(get("/objects_count/jobs/status/{status}", "QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobCount").value(before + 1));
    }
}
