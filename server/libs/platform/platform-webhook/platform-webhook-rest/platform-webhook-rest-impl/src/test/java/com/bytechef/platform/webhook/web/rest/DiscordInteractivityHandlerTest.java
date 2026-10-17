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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.webhook.web.rest.ApprovalResolutionLookup.Resolution;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DiscordInteractivityHandlerTest {

    private static final String RESUME_ID = EncodingUtils.base64EncodeToString(
        "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8));
    private static final String SHORT_ID = "short-id";
    private static final String TIMESTAMP = "1700000000";

    private final ApprovalResolutionLookup approvalResolutionLookup = mock(ApprovalResolutionLookup.class);
    private final ApprovalShortTokenService approvalShortTokenService = mock(ApprovalShortTokenService.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

    private PrivateKey privateKey;
    private String publicKeyHex;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        privateKey = keyPair.getPrivate();

        byte[] encodedPublicKey = keyPair.getPublic()
            .getEncoded();

        HexFormat hexFormat = HexFormat.of();

        publicKeyHex = hexFormat.formatHex(
            Arrays.copyOfRange(encodedPublicKey, encodedPublicKey.length - 32, encodedPublicKey.length));

        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(RESUME_ID));
    }

    @Test
    void testValidSignatureAnswersThePing() throws Exception {
        String rawBody = "{\"type\":1}";

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(Map.of("type", 1));
    }

    @Test
    void testValidSignatureResolvesTheApproval() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
            Map.of("type", 7, "data", Map.of("content", "✅ Approved by @jane.", "components", List.of())));

        verify(jobResumeFacade).resumeJob(RESUME_ID, Map.of("approved", true), "@jane");
    }

    @Test
    void testGoneClickOnAResolvedApprovalSaysItWasAlreadyResolved() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.RESOLVED);

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.body()).isEqualTo(
            Map.of(
                "type", 7, "data",
                Map.of("content", "ℹ️ This approval was already resolved.", "components", List.of())));
    }

    @Test
    void testGoneClickOnAnExpiredApprovalSaysItExpired() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.GONE);
        when(approvalResolutionLookup.describe(RESUME_ID)).thenReturn(Resolution.EXPIRED);

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.body()).isEqualTo(
            Map.of(
                "type", 7, "data",
                Map.of("content", "⌛ This approval expired before it was resolved.", "components", List.of())));
    }

    @Test
    void testNotYetSuspendedRepliesEphemerallyAndKeepsTheMessage() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":d");

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), eq(Map.of("approved", false)), eq("@jane")))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
            Map.of("type", 4, "data", Map.of("content", "Still preparing — try again in a moment.", "flags", 64)));
    }

    @Test
    void testBadSignatureIsRejectedWithoutResuming() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        String signature = sign(rawBody.replace(":a", ":d"));

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(rawBody, signature, TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(401);

        verifyNeverResumed();
    }

    @Test
    void testMissingSignatureIsRejectedWithoutResuming() {
        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            buttonClickBody(SHORT_ID + ":a"), null, TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(401);

        verifyNeverResumed();
    }

    @Test
    void testMissingTimestampIsRejectedWithoutResuming() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(rawBody, sign(rawBody), null);

        assertThat(response.statusCode()).isEqualTo(401);

        verifyNeverResumed();
    }

    @Test
    void testMissingPublicKeyRejectsEveryCallback() throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler.Response response = handler(null).handle(rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(401);

        verifyNeverResumed();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testBlankPublicKeyIsWarnedAboutOnce(CapturedOutput capturedOutput) throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler discordInteractivityHandler = handler(" ");

        discordInteractivityHandler.handle(rawBody, sign(rawBody), TIMESTAMP);
        discordInteractivityHandler.handle(rawBody, sign(rawBody), TIMESTAMP);

        assertThat(countOccurrences(capturedOutput.getAll(), "no Discord application public key is configured"))
            .isEqualTo(1);

        verifyNeverResumed();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testUnparseablePublicKeyIsWarnedAbout(CapturedOutput capturedOutput) throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler.Response response = handler("not-hex").handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(capturedOutput.getAll()).contains("WARN")
            .contains("is not a valid hex-encoded Ed25519 key");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testSignatureMismatchIsNotWarnedAbout(CapturedOutput capturedOutput) throws Exception {
        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody + " "), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(capturedOutput.getAll()).doesNotContain("Rejecting Discord interactions");
    }

    @Test
    void testUnknownShortIdDoesNotResume() throws Exception {
        String rawBody = buttonClickBody("unknown:a");

        DiscordInteractivityHandler.Response response = handler(publicKeyHex).handle(
            rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.statusCode()).isEqualTo(200);

        verifyNeverResumed();
    }

    @Test
    void testSignedTokenIsUnwrappedBeforeResolvingTheTenant() throws Exception {
        String signedValue = "v1.9999999999." + RESUME_ID + ".signature";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(signedValue)).thenReturn(Optional.of(RESUME_ID));
        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(signedValue));
        when(jobResumeFacade.resumeJob(eq(signedValue), eq(Map.of("approved", true)), eq("@jane")))
            .thenReturn(JobResumeOutcome.OK);

        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler signedTokenHandler = new DiscordInteractivityHandler(
            approvalResolutionLookup, approvalShortTokenService, jobResumeFacade, approvalTokens, publicKeyHex);

        DiscordInteractivityHandler.Response response = signedTokenHandler.handle(rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.body()).isEqualTo(
            Map.of("type", 7, "data", Map.of("content", "✅ Approved by @jane.", "components", List.of())));

        verify(jobResumeFacade).resumeJob(signedValue, Map.of("approved", true), "@jane");
    }

    @Test
    void testTamperedSignedTokenIsIgnoredWithoutResuming() throws Exception {
        String tamperedValue = "v1.9999999999." + RESUME_ID + ".forged";

        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.resolveInnerToken(tamperedValue)).thenReturn(Optional.empty());
        when(approvalShortTokenService.fetchToken(SHORT_ID)).thenReturn(Optional.of(tamperedValue));

        String rawBody = buttonClickBody(SHORT_ID + ":a");

        DiscordInteractivityHandler signedTokenHandler = new DiscordInteractivityHandler(
            approvalResolutionLookup, approvalShortTokenService, jobResumeFacade, approvalTokens, publicKeyHex);

        DiscordInteractivityHandler.Response response = signedTokenHandler.handle(rawBody, sign(rawBody), TIMESTAMP);

        assertThat(response.body()).isEqualTo(
            Map.of(
                "type", 7, "data",
                Map.of("content", "⚠️ This approval is no longer available.", "components", List.of())));

        verifyNeverResumed();
    }

    private DiscordInteractivityHandler handler(String publicKey) {
        return new DiscordInteractivityHandler(
            approvalResolutionLookup, approvalShortTokenService, jobResumeFacade, null, publicKey);
    }

    private String sign(String rawBody) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");

        signature.initSign(privateKey);
        signature.update((TIMESTAMP + rawBody).getBytes(StandardCharsets.UTF_8));

        HexFormat hexFormat = HexFormat.of();

        return hexFormat.formatHex(signature.sign());
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int index = text.indexOf(fragment);

        while (index >= 0) {
            count++;

            index = text.indexOf(fragment, index + fragment.length());
        }

        return count;
    }

    private void verifyNeverResumed() {
        verify(jobResumeFacade, never()).resumeJob(anyString(), any(), any());
    }

    private static String buttonClickBody(String customId) {
        return "{\"type\":3,\"member\":{\"user\":{\"username\":\"jane\"}},\"data\":{\"custom_id\":\"" + customId +
            "\"}}";
    }
}
