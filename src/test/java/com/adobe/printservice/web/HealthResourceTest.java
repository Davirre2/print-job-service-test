package com.adobe.printservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void liveness_returns200WithMinimalBody() throws Exception {
        mockMvc.perform(get("/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ready").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void readiness_databaseAndStorageUp_returns200WithPerCheckDetails() throws Exception {
        mockMvc.perform(get("/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.details.database").value("UP"))
                .andExpect(jsonPath("$.details.storage").value("UP"));
    }
}
