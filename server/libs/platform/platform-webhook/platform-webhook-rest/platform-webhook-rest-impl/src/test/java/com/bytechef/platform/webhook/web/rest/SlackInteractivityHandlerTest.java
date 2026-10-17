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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.webhook.web.rest.ApprovalResolutionLookup.Resolution;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SlackInteractivityHandlerTest {

    private static final String SIGNING_SECRET = "test-signing-secret";

    private final ApprovalResolutionLookup approvalResolutionLookup = mock(ApprovalResolutionLookup.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

    private SlackInteractivityHandler slackInteractivityHandler;

    @BeforeEach
    void setUp() throws Exception {
        slackInteractivityHandler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, null,
            RestClient.create("http://localhost:1"));

        Connection connection = new Connection();

        connection.setParameters(Map.of("signingSecret", SIGNING_SECRET));

        when(connectionService.getConnections(eq("slack"), anyInt(), any())).thenReturn(List.of(connection));
    }

    @Test
    void testVerifiedApproveCallbackResolvesTheApproval() throws Exception {
        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        when(jobResumeFacade.resumeJob(anyString(), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);

        verify(jobResumeFacade).resumeJob(anyString(), eq(Map.of("approved", true)), eq("@jane"));
    }

    @Test
    void testInvalidSignatureIsRejectedWithoutActing() throws Exception {
        String rawBody = rawBody(SlackInteractivityHandler.ACTION_DISCARD);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, "wrong-secret"));

        assertEquals(SlackInteractivityHandler.Result.UNAUTHORIZED, result);

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testConnectionEnumerationFailureIsReportedAsUnavailable() throws Exception {
        when(connectionService.getConnections(eq("slack"), anyInt(), any()))
            .thenThrow(new IllegalStateException("database unavailable"));

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.UNAVAILABLE, result);

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testStaleTimestampIsRejectedWithoutActing() throws Exception {
        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond() - 3600);

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.UNAUTHORIZED, result);

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testUnrelatedActionIsIgnored() {
        String rawBody = rawBody("some_other_action");
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(rawBody, timestamp, "v0=whatever");

        assertEquals(SlackInteractivityHandler.Result.IGNORED, result);

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testVerifiedDiscardWithoutModalResolvesImmediately() throws Exception {
        String rawBody = rawBody(SlackInteractivityHandler.ACTION_DISCARD);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        when(jobResumeFacade.resumeJob(anyString(), eq(Map.of("approved", false)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);

        verify(jobResumeFacade).resumeJob(anyString(), eq(Map.of("approved", false)), eq("@jane"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testRejectedDiscardModalIsWarnedAboutAndDiscardsWithoutComment(CapturedOutput capturedOutput)
        throws Exception {

        RestClient.Builder restClientBuilder = RestClient.builder();

        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        SlackInteractivityHandler handler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, null, restClientBuilder.build());

        Connection connection = new Connection();

        connection.setParameters(Map.of("signingSecret", SIGNING_SECRET, "access_token", "xoxb-token"));

        when(connectionService.getConnections(eq("slack"), anyInt(), any())).thenReturn(List.of(connection));
        when(jobResumeFacade.resumeJob(anyString(), eq(Map.of("approved", false)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        mockRestServiceServer.expect(requestTo("https://slack.com/api/views.open"))
            .andRespond(withSuccess("{\"ok\":false,\"error\":\"missing_scope\"}", MediaType.APPLICATION_JSON));
        mockRestServiceServer.expect(requestTo("https://hooks.slack.invalid/actions/x"))
            .andRespond(withSuccess());

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_DISCARD, defaultResumeId(), "trigger-1");
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = handler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);
        assertThat(capturedOutput.getAll()).contains("Could not open the Slack discard-comment modal")
            .contains("missing_scope");

        verify(jobResumeFacade).resumeJob(anyString(), eq(Map.of("approved", false)), eq("@jane"));

        mockRestServiceServer.verify();
    }

    @Test
    void testGoneClickOnAResolvedApprovalSaysItWasAlreadyResolved() throws Exception {
        assertGoneClickRewritesTheMessageWith(Resolution.RESOLVED,
            ":information_source: This approval was already resolved.");
    }

    @Test
    void testGoneClickOnAnExpiredApprovalSaysItExpired() throws Exception {
        assertGoneClickRewritesTheMessageWith(Resolution.EXPIRED,
            ":hourglass: This approval expired before it was resolved.");
    }

    @Test
    void testGoneClickOnACancelledApprovalSaysItIsNoLongerAvailable() throws Exception {
        assertGoneClickRewritesTheMessageWith(
            Resolution.UNAVAILABLE, ":warning: This approval is no longer available.");
    }

    private void assertGoneClickRewritesTheMessageWith(Resolution resolution, String expectedText)
        throws Exception {

        RestClient.Builder restClientBuilder = RestClient.builder();

        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        SlackInteractivityHandler handler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, null, restClientBuilder.build());

        when(jobResumeFacade.resumeJob(anyString(), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(defaultResumeId())).thenReturn(resolution);

        mockRestServiceServer.expect(requestTo("https://hooks.slack.invalid/actions/x"))
            .andExpect(
                content().json(
                    JsonUtils.write(Map.of("replace_original", true, "text", expectedText)), JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        handler.handle(rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        mockRestServiceServer.verify();
    }

    @Test
    void testViewSubmissionResolvesWithComment() throws Exception {
        String rawBody = viewSubmissionBody(SlackInteractivityHandler.CALLBACK_DISCARD_COMMENT, "looks risky");
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        when(jobResumeFacade.resumeJob(
            anyString(), eq(Map.of("approved", false, "comment", "looks risky")), eq("@jane")))
                .thenReturn(JobResumeOutcome.OK);

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);

        verify(jobResumeFacade).resumeJob(
            anyString(), eq(Map.of("approved", false, "comment", "looks risky")), eq("@jane"));
    }

    @Test
    void testViewSubmissionWithUnknownCallbackIsIgnored() throws Exception {
        String rawBody = viewSubmissionBody("some_other_modal", "whatever");
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = slackInteractivityHandler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.IGNORED, result);

        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testSignedTokenValueIsUnwrappedBeforeParsing() throws Exception {
        String innerToken = EncodingUtils.base64EncodeToString(
            "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
        String signedValue = "v1.9999999999." + innerToken + ".signature";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(signedValue)).thenReturn(java.util.Optional.of(innerToken));

        SlackInteractivityHandler handler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, approvalTokens,
            RestClient.create("http://localhost:1"));

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE, signedValue);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        when(jobResumeFacade.resumeJob(eq(signedValue), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        SlackInteractivityHandler.Result result = handler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);

        verify(jobResumeFacade).resumeJob(eq(signedValue), eq(Map.of("approved", true)), eq("@jane"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testUnverifiableSignedTokenIsLoggedAndIgnoredWithoutAConnectionLookup(CapturedOutput capturedOutput)
        throws Exception {

        String tamperedValue = "v1.9999999999.forged-token.signature";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(tamperedValue)).thenReturn(java.util.Optional.empty());

        SlackInteractivityHandler handler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, approvalTokens,
            RestClient.create("http://localhost:1"));

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE, tamperedValue);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        SlackInteractivityHandler.Result result = handler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.IGNORED, result);
        assertThat(capturedOutput.getAll()).contains("unverifiable or expired resume id v1.999999999…");

        verify(connectionService, never()).getConnections(anyString(), anyInt(), any());
        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    @Test
    void testNotYetSuspendedLeavesTheCardInPlaceAndRepliesEphemerally() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();

        MockRestServiceServer mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        SlackInteractivityHandler handler = new SlackInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, null, restClientBuilder.build());

        mockRestServiceServer.expect(requestTo("https://hooks.slack.invalid/actions/x"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().json(
                    "{\"response_type\":\"ephemeral\",\"replace_original\":false,"
                        + "\"text\":\"Still preparing — try again in a moment.\"}",
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        String rawBody = rawBody(SlackInteractivityHandler.ACTION_APPROVE);
        String timestamp = String.valueOf(Instant.now()
            .getEpochSecond());

        when(jobResumeFacade.resumeJob(anyString(), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        SlackInteractivityHandler.Result result = handler.handle(
            rawBody, timestamp, sign(timestamp, rawBody, SIGNING_SECRET));

        assertEquals(SlackInteractivityHandler.Result.HANDLED, result);

        mockRestServiceServer.verify();
    }

    private static String rawBody(String actionId) {
        return rawBody(actionId, defaultResumeId());
    }

    private static String rawBody(String actionId, String value) {
        return rawBody(actionId, value, null);
    }

    private static String rawBody(String actionId, String value, String triggerId) {
        String triggerIdField = triggerId == null ? "" : "\"trigger_id\":\"" + triggerId + "\",";

        String payload = "{\"type\":\"block_actions\"," + triggerIdField
            + "\"user\":{\"username\":\"jane\"},\"response_url\":"
            + "\"https://hooks.slack.invalid/actions/x\",\n"
            + " \"actions\":[{\"action_id\":\"" + actionId + "\",\"value\":\"" + value + "\"}]}";

        return "payload=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private static String defaultResumeId() {
        return EncodingUtils.base64EncodeToString(
            "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
    }

    private static String viewSubmissionBody(String callbackId, String comment) {
        String resumeId = EncodingUtils.base64EncodeToString(
            "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));

        String privateMetadata = JsonUtils.write(
            Map.of("resumeId", resumeId, "responseUrl", "https://hooks.slack.invalid/actions/x"));

        String payload = JsonUtils.write(
            Map.of(
                "type", "view_submission",
                "user", Map.of("username", "jane"),
                "view", Map.of(
                    "callback_id", callbackId,
                    "private_metadata", privateMetadata,
                    "state", Map.of(
                        "values", Map.of(
                            "comment_block", Map.of("comment", Map.of("value", comment)))))));

        return "payload=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private static String sign(String timestamp, String rawBody, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        HexFormat hexFormat = HexFormat.of();

        return "v0=" + hexFormat.formatHex(
            mac.doFinal(("v0:" + timestamp + ":" + rawBody).getBytes(StandardCharsets.UTF_8)));
    }
}
