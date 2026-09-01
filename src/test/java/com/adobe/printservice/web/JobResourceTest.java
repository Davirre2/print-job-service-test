package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void submitJob_existingTemplate_returns201WithQueuedJob() throws Exception {
        String body = """
                {
                  "templateId": "%s",
                  "parameters": { "recipient": "someone@example.com" }
                }
                """.formatted(INVOICE_TEMPLATE_ID);

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.templateId").value(INVOICE_TEMPLATE_ID))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.resultAvailable").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void getJob_existingId_returnsStatusAndResultAvailability() throws Exception {
        Job job = new Job();
        job.setTemplateId(INVOICE_TEMPLATE_ID);
        job.setStatus(JobStatus.FAILED);
        job.setAttempts(3);
        job.setErrorMessage("Transient render failure");
        jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attempts").value(3))
                .andExpect(jsonPath("$.errorMessage").value("Transient render failure"))
                .andExpect(jsonPath("$.resultAvailable").value(false));
    }

    @Test
    void getJob_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/jobs/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listJobs_withoutStatus_returnsAllJobs() throws Exception {
        saveJob(JobStatus.QUEUED);
        saveJob(JobStatus.DONE);
        saveJob(JobStatus.FAILED);

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void listJobs_withStatus_returnsOnlyMatchingJobs() throws Exception {
        saveJob(JobStatus.QUEUED);
        saveJob(JobStatus.DONE);
        saveJob(JobStatus.FAILED);
        saveJob(JobStatus.FAILED);

        mockMvc.perform(get("/jobs").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[1].status").value("FAILED"));
    }

    @Test
    void listJobs_withInvalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitJob_unknownTemplate_returns400() throws Exception {
        String body = """
                {
                  "templateId": "does-not-exist",
                  "parameters": {}
                }
                """;

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitJob_missingTemplateId_returns400() throws Exception {
        String body = """
                {
                  "parameters": {}
                }
                """;

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private Job saveJob(JobStatus status) {
        Job job = new Job();
        job.setTemplateId(INVOICE_TEMPLATE_ID);
        job.setStatus(status);
        return jobRepository.save(job);
    }
}
