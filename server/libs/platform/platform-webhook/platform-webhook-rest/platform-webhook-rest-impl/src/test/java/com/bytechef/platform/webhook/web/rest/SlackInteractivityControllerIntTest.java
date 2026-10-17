/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(ObjectMapperSetupExtension.class)
@ContextConfiguration(classes = SlackInteractivityController.class)
@WebMvcTest(controllers = SlackInteractivityController.class, properties = "bytechef.coordinator.enabled=true")
class SlackInteractivityControllerIntTest {

    private static final String RESUME_ID = JobResumeId.of("public", 42L)
        .toString();
    private static final String SIGNING_SECRET = "test-signing-secret";

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private JobResumeFacade jobResumeFacade;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    private String timestamp;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        timestamp = String.valueOf(now.getEpochSecond());

        Connection connection = new Connection();

        connection.setParameters(Map.of("signingSecret", SIGNING_SECRET));

        when(connectionService.getConnections(eq("slack"), anyInt(), any())).thenReturn(List.of(connection));
        when(jobResumeFacade.resumeJob(anyString(), any(), any())).thenReturn(JobResumeOutcome.OK);
    }

    @Test
    void testBodyWithPercentTwentyEncodedSpacesVerifiesAgainstTheRawBytes() throws Exception {
        String rawBody = "payload=" + URLEncoder.encode(payload(), StandardCharsets.UTF_8)
            .replace("+", "%20");

        perform(rawBody, sign(rawBody, SIGNING_SECRET)).andExpect(status().isOk());

        verify(jobResumeFacade).resumeJob(RESUME_ID, Map.of("approved", true), "@jane doe");
    }

    @Test
    void testBodyWithPlusEncodedSpacesVerifies() throws Exception {
        String rawBody = "payload=" + URLEncoder.encode(payload(), StandardCharsets.UTF_8);

        perform(rawBody, sign(rawBody, SIGNING_SECRET)).andExpect(status().isOk());

        verify(jobResumeFacade).resumeJob(RESUME_ID, Map.of("approved", true), "@jane doe");
    }

    @Test
    void testWrongSignatureIsRejected() throws Exception {
        String rawBody = "payload=" + URLEncoder.encode(payload(), StandardCharsets.UTF_8)
            .replace("+", "%20");

        perform(rawBody, sign(rawBody, "wrong-secret")).andExpect(status().isUnauthorized());

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    private ResultActions perform(String rawBody, String signature) throws Exception {
        return mockMvc.perform(
            post("/slack/interactivity")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Slack-Request-Timestamp", timestamp)
                .header("X-Slack-Signature", signature)
                .content(rawBody));
    }

    private static String payload() {
        return "{\"type\":\"block_actions\",\"user\":{\"username\":\"jane doe\"},"
            + "\"actions\":[{\"action_id\":\"approval_approve\",\"value\":\"" + RESUME_ID + "\"}]}";
    }

    private String sign(String rawBody, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        HexFormat hexFormat = HexFormat.of();

        return "v0=" + hexFormat.formatHex(
            mac.doFinal(("v0:" + timestamp + ":" + rawBody).getBytes(StandardCharsets.UTF_8)));
    }
}
