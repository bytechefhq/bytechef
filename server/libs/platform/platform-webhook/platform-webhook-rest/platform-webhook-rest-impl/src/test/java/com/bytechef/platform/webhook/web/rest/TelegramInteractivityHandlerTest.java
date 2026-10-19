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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.webhook.web.rest.ApprovalResolutionLookup.Resolution;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TelegramInteractivityHandlerTest {

    private static final String BOT_TOKEN = "123456:secret-bot-token";
    private static final String RESUME_ID = EncodingUtils.base64EncodeToString(
        "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
    private static final String FOREIGN_TENANT_RESUME_ID = EncodingUtils.base64EncodeToString(
        "foreign:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
    private static final String SECRET_TOKEN = "webhook-secret";
    private static final String SHORT_ID = "short-id";
    private static final String TELEGRAM_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";

    private final ApprovalResolutionLookup approvalResolutionLookup = mock(ApprovalResolutionLookup.class);
    private final ApprovalShortTokenService approvalShortTokenService = mock(ApprovalShortTokenService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final Connection connection = new Connection();
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

    private MockRestServiceServer mockRestServiceServer;
    private TelegramInteractivityHandler telegramInteractivityHandler;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        telegramInteractivityHandler = new TelegramInteractivityHandler(
            approvalResolutionLookup, approvalShortTokenService, connectionService, jobResumeFacade, null,
            restClientBuilder.build());

        connection.setParameters(Map.of("botToken", BOT_TOKEN, "webhookSecretToken", SECRET_TOKEN));

        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(RESUME_ID));
        when(connectionService.getConnections(eq("telegram"), anyInt(), any())).thenReturn(List.of(connection));
    }

    @Test
    void testValidSecretTokenResolvesTheApprovalAndEditsTheMessage() {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andExpect(content().json("{\"callback_query_id\":\"cb-1\"}", JsonCompareMode.STRICT))
            .andRespond(withSuccess());
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "editMessageText"))
            .andExpect(
                content().json(
                    "{\"chat_id\":7,\"message_id\":9,\"text\":\"✅ Approved by @jane.\"}", JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.HANDLED);

        verify(jobResumeFacade).resumeJob(RESUME_ID, Map.of("approved", true), "@jane");

        mockRestServiceServer.verify();
    }

    @Test
    void testGoneClickOnAResolvedApprovalSaysItWasAlreadyResolved() {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.RESOLVED);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andRespond(withSuccess());
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "editMessageText"))
            .andExpect(
                content().json(
                    "{\"chat_id\":7,\"message_id\":9,\"text\":\"ℹ️ This approval was already resolved.\"}",
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        telegramInteractivityHandler.handle(callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        mockRestServiceServer.verify();
    }

    @Test
    void testGoneClickOnAnExpiredApprovalSaysItExpired() {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.EXPIRED);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andRespond(withSuccess());
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "editMessageText"))
            .andExpect(
                content().json(
                    "{\"chat_id\":7,\"message_id\":9,\"text\":\"⌛ This approval expired before it was resolved.\"}",
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        telegramInteractivityHandler.handle(callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        mockRestServiceServer.verify();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testFailedTelegramCallDoesNotLogTheBotToken(CapturedOutput capturedOutput) {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andRespond(withException(new IOException("connection reset")));
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "editMessageText"))
            .andRespond(withException(new IOException("connection reset")));

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.HANDLED);
        assertThat(capturedOutput.getAll()).contains("Telegram answerCallbackQuery call failed")
            .doesNotContain(BOT_TOKEN);
    }

    @Test
    void testNotYetSuspendedAnswersTheCallbackAndKeepsTheMessage() {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", false)), eq("@jane")))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andExpect(
                content().json(
                    "{\"callback_query_id\":\"cb-1\",\"text\":\"Still preparing — try again in a moment.\"}",
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":d"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.HANDLED);

        mockRestServiceServer.verify();
    }

    @Test
    void testWrongSecretTokenIsRejectedWithoutResuming() {
        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":a"), "wrong-secret");

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.UNAUTHORIZED);

        verifyNeverResumed();
    }

    @Test
    void testMissingSecretTokenIsRejectedWithoutResuming() {
        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":a"), null);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.UNAUTHORIZED);

        verifyNeverResumed();
    }

    @Test
    void testConnectionEnumerationFailureIsReportedAsUnavailable() {
        when(connectionService.getConnections(eq("telegram"), anyInt(), any()))
            .thenThrow(new IllegalStateException("database unavailable"));

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.UNAVAILABLE);

        verifyNeverResumed();
    }

    @Test
    void testUnknownShortIdIsIgnoredAndTheCallbackAnswered() {
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andExpect(
                content().json(
                    "{\"callback_query_id\":\"cb-1\",\"text\":\"This approval is no longer available.\"}",
                    JsonCompareMode.STRICT))
            .andRespond(withSuccess());

        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody("unknown:a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.IGNORED);

        verifyNeverResumed();

        mockRestServiceServer.verify();
    }

    @Test
    void testUnknownShortIdWithWrongSecretTokenIsNotAnswered() {
        TelegramInteractivityHandler.Result result = telegramInteractivityHandler.handle(
            callbackBody("unknown:a"), "wrong-secret");

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.IGNORED);

        verifyNeverResumed();

        mockRestServiceServer.verify();
    }

    @Test
    void testSignedTokenIsUnwrappedBeforeResolvingTheTenant() {
        String signedValue = "v1.9999999999." + RESUME_ID + ".signature";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(signedValue)).thenReturn(Optional.of(RESUME_ID));
        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(signedValue));
        when(jobResumeFacade.resumeJob(eq(signedValue), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        TelegramInteractivityHandler signedTokenHandler = handler(approvalTokens);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andRespond(withSuccess());
        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "editMessageText"))
            .andRespond(withSuccess());

        TelegramInteractivityHandler.Result result = signedTokenHandler.handle(
            callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.HANDLED);

        verify(jobResumeFacade).resumeJob(signedValue, Map.of("approved", true), "@jane");
    }

    @Test
    void testUnverifiableSignedTokenIsIgnoredBeforeAnyTenantScopedLookup() {
        String tamperedValue = "v1.9999999999." + FOREIGN_TENANT_RESUME_ID + ".forged";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);
        List<String> lookupTenantIds = new ArrayList<>();

        when(approvalTokens.resolveInnerToken(tamperedValue)).thenReturn(Optional.empty());
        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(tamperedValue));
        when(connectionService.getConnections(eq("telegram"), anyInt(), any())).thenAnswer(invocation -> {
            lookupTenantIds.add(TenantContext.getCurrentTenantId());

            return List.of(connection);
        });

        TelegramInteractivityHandler signedTokenHandler = handler(approvalTokens);

        mockRestServiceServer.expect(requestTo(TELEGRAM_API + "answerCallbackQuery"))
            .andRespond(withSuccess());

        TelegramInteractivityHandler.Result result = signedTokenHandler.handle(
            callbackBody(SHORT_ID + ":a"), SECRET_TOKEN);

        assertThat(result).isEqualTo(TelegramInteractivityHandler.Result.IGNORED);
        assertThat(lookupTenantIds).isNotEmpty()
            .doesNotContain("foreign");

        verifyNeverResumed();
    }

    private TelegramInteractivityHandler handler(ApprovalTokens approvalTokens) {
        RestClient.Builder restClientBuilder = RestClient.builder();

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        return new TelegramInteractivityHandler(
            approvalResolutionLookup, approvalShortTokenService, connectionService, jobResumeFacade, approvalTokens,
            restClientBuilder.build());
    }

    private void verifyNeverResumed() {
        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    private static String callbackBody(String data) {
        return "{\"callback_query\":{\"id\":\"cb-1\",\"from\":{\"username\":\"jane\"},\"data\":\"" + data +
            "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":7}}}}";
    }
}
