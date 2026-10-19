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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WhatsAppInteractivityHandlerTest {

    private static final String ACCESS_TOKEN = "graph-access-token";
    private static final String APP_SECRET = "app-secret";
    private static final String GRAPH_MESSAGES_URL = "https://graph.facebook.com/v17.0/1234/messages";
    private static final String FOREIGN_TENANT_RESUME_ID = EncodingUtils.base64EncodeToString(
        "foreign:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
    private static final String RESUME_ID = EncodingUtils.base64EncodeToString(
        "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));

    private final ApprovalResolutionLookup approvalResolutionLookup = mock(ApprovalResolutionLookup.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

    private MockRestServiceServer mockRestServiceServer;
    private WhatsAppInteractivityHandler whatsAppInteractivityHandler;

    @BeforeEach
    void setUp() {
        whatsAppInteractivityHandler = handler(null);

        Connection connection = new Connection();

        connection.setParameters(
            Map.of("appSecret", APP_SECRET, "systemUserAccessToken", ACCESS_TOKEN, "phoneNumberId", "1234"));

        when(connectionService.getConnections(eq("whatsApp"), anyInt(), any())).thenReturn(List.of(connection));
    }

    @Test
    void testValidSignatureResolvesTheApproval() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.OK);

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.HANDLED);

        verify(jobResumeFacade).resumeJob(RESUME_ID, Map.of("approved", true), "Jane");
    }

    @Test
    void testSignatureWithTheWrongSecretIsRejectedWithoutResuming() throws Exception {
        String rawBody = buttonReplyBody("d:" + RESUME_ID);

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, "wrong-secret"));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.UNAUTHORIZED);

        verifyNeverResumed();
    }

    @Test
    void testSignatureOverADifferentBodyIsRejectedWithoutResuming() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(buttonReplyBody("d:" + RESUME_ID), APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.UNAUTHORIZED);

        verifyNeverResumed();
    }

    @Test
    void testMissingSignatureIsRejectedWithoutResuming() {
        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            buttonReplyBody("a:" + RESUME_ID), null);

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.UNAUTHORIZED);

        verifyNeverResumed();
    }

    @Test
    void testConnectionEnumerationFailureIsReportedAsUnavailable() throws Exception {
        when(connectionService.getConnections(eq("whatsApp"), anyInt(), any()))
            .thenThrow(new IllegalStateException("database unavailable"));

        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.UNAVAILABLE);

        verifyNeverResumed();
    }

    @Test
    void testExpiredApprovalRepliesToTheApprover() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.EXPIRED);

        expectReply("⌛ This approval expired before it was resolved.");

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.HANDLED);

        mockRestServiceServer.verify();
    }

    @Test
    void testAlreadyResolvedApprovalRepliesThatItWasResolved() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.RESOLVED);

        expectReply("ℹ️ This approval was already resolved.");

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.HANDLED);

        mockRestServiceServer.verify();
    }

    @Test
    void testNotYetSuspendedRepliesToTheApprover() throws Exception {
        String rawBody = buttonReplyBody("d:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", false)), eq("Jane")))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        expectReply("Still preparing — try again in a moment.");

        whatsAppInteractivityHandler.handle(rawBody, sign(rawBody, APP_SECRET));

        mockRestServiceServer.verify();
    }

    @Test
    void testInvalidIdRepliesThatTheApprovalIsNoLongerAvailable() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.INVALID_ID);

        expectReply("⚠️ This approval is no longer available.");

        whatsAppInteractivityHandler.handle(rawBody, sign(rawBody, APP_SECRET));

        mockRestServiceServer.verify();
    }

    @Test
    void testResolvedApprovalSendsNoReply() throws Exception {
        String rawBody = buttonReplyBody("a:" + RESUME_ID);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.OK);

        whatsAppInteractivityHandler.handle(rawBody, sign(rawBody, APP_SECRET));

        mockRestServiceServer.verify();
    }

    @Test
    void testSignedTokenIsUnwrappedBeforeResolvingTheTenant() throws Exception {
        String signedValue = "v1.9999999999." + RESUME_ID + ".signature";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(signedValue)).thenReturn(Optional.of(RESUME_ID));
        when(jobResumeFacade.resumeJob(eq(signedValue), eq(Map.of("approved", true)), eq("Jane")))
            .thenReturn(JobResumeOutcome.OK);

        String rawBody = buttonReplyBody("a:" + signedValue);

        WhatsAppInteractivityHandler.Result result = handler(approvalTokens).handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.HANDLED);

        verify(jobResumeFacade).resumeJob(signedValue, Map.of("approved", true), "Jane");
    }

    @Test
    void testTamperedSignedTokenIsIgnoredBeforeAnyConnectionLookup() throws Exception {
        String tamperedValue = "v1.9999999999." + FOREIGN_TENANT_RESUME_ID + ".forged";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(tamperedValue)).thenReturn(Optional.empty());

        String rawBody = buttonReplyBody("a:" + tamperedValue);

        WhatsAppInteractivityHandler.Result result = handler(approvalTokens).handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.IGNORED);

        verify(connectionService, never()).getConnections(anyString(), anyInt(), any());

        verifyNeverResumed();
    }

    @Test
    void testUnsignedForeignTenantIsOnlyUsedToScopeSignatureVerification() throws Exception {
        List<String> lookupTenantIds = new ArrayList<>();

        when(connectionService.getConnections(eq("whatsApp"), anyInt(), any())).thenAnswer(invocation -> {
            lookupTenantIds.add(TenantContext.getCurrentTenantId());

            return List.of();
        });

        String rawBody = buttonReplyBody("a:" + FOREIGN_TENANT_RESUME_ID);

        WhatsAppInteractivityHandler.Result result = whatsAppInteractivityHandler.handle(
            rawBody, sign(rawBody, APP_SECRET));

        assertThat(result).isEqualTo(WhatsAppInteractivityHandler.Result.UNAUTHORIZED);
        assertThat(lookupTenantIds).containsOnly("foreign");

        verifyNeverResumed();
    }

    private void expectReply(String text) {
        mockRestServiceServer.expect(requestTo(GRAPH_MESSAGES_URL))
            .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
            .andExpect(
                content().json(
                    JsonUtils.write(
                        Map.of(
                            "messaging_product", "whatsapp", "recipient_type", "individual", "to", "15550001",
                            "type", "text", "text", Map.of("body", text))),
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());
    }

    private WhatsAppInteractivityHandler handler(ApprovalTokens approvalTokens) {
        RestClient.Builder restClientBuilder = RestClient.builder();

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        return new WhatsAppInteractivityHandler(
            approvalResolutionLookup, connectionService, jobResumeFacade, approvalTokens, restClientBuilder.build());
    }

    private void verifyNeverResumed() {
        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    private static String buttonReplyBody(String buttonId) {
        return "{\"entry\":[{\"changes\":[{\"value\":{\"contacts\":[{\"profile\":{\"name\":\"Jane\"}}],"
            + "\"messages\":[{\"from\":\"15550001\",\"interactive\":{\"button_reply\":{\"id\":\"" + buttonId
            + "\"}}}]}}]}]}";
    }

    private static String sign(String rawBody, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        HexFormat hexFormat = HexFormat.of();

        return "sha256=" + hexFormat.formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
    }
}
