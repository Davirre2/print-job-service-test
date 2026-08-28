package com.adobe.printservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Example of the MockMvc setup used elsewhere in this suite - provided so you don't need to
 * rediscover the wiring yourself. Template management is out of scope for this exercise; this
 * test only exercises the endpoint you're given, not anything you need to build.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RenderTemplateResourceTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTemplates_returnsSeededTemplates() throws Exception {
        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.id == '" + INVOICE_TEMPLATE_ID + "')].name").value("invoice-standard"));
    }

    @Test
    void getTemplate_existingId_returns200() throws Exception {
        mockMvc.perform(get("/templates/{id}", INVOICE_TEMPLATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("invoice-standard"));
    }

    @Test
    void getTemplate_missingId_returns404() throws Exception {
        mockMvc.perform(get("/templates/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
