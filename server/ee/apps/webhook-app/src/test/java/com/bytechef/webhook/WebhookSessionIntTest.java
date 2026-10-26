/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 */
@AutoConfigureMockMvc
@Import({
    RedisContainerConfiguration.class
})
@SpringBootTest(classes = WebhookApplication.class)
class WebhookSessionIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/embedded/v1/app-events"))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(401));
    }

    @Test
    void testPublicWebhookStaysOpen() throws Exception {
        int status;

        try {
            status = mockMvc.perform(post("/webhooks/unknown-id"))
                .andReturn()
                .getResponse()
                .getStatus();
        } catch (ServletException servletException) {
            status = 500;
        }

        assertThat(status).isNotIn(401, 403);
    }
}
