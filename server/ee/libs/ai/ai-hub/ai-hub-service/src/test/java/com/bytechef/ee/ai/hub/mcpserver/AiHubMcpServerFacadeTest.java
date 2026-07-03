/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubMcpServerFacadeTest {

    @Test
    void testAddMcpServerRejectsSsrfUrl() {
        AiHubMcpServerRepository mcpServerRepository = mock(AiHubMcpServerRepository.class);
        AiHubMcpServerToolRepository mcpServerToolRepository = mock(AiHubMcpServerToolRepository.class);
        AiHubMcpClientManager clientManager = mock(AiHubMcpClientManager.class);
        Encryption encryption = mock(Encryption.class);

        // Default ApplicationProperties => empty allowlist, so the SSRF guard blocks all private/loopback targets.
        AiHubMcpServerUrlGuard urlGuard = new AiHubMcpServerUrlGuard(new ApplicationProperties());

        AiHubMcpServerFacadeImpl facade = new AiHubMcpServerFacadeImpl(
            mcpServerRepository, mcpServerToolRepository, clientManager, encryption, urlGuard);

        // The cloud-metadata endpoint is a link-local literal, so the SSRF guard rejects it without any DNS lookup.
        // The registration must fail BEFORE persisting (and before the token is even encrypted).
        assertThatThrownBy(
            () -> facade.addMcpServer(1L, 10L, 0, "metadata", "http://169.254.169.254/latest/meta-data/", "secret"))
                .isInstanceOf(UrlValidationException.class);

        verify(mcpServerRepository, never()).save(ArgumentMatchers.any());
        verify(encryption, never()).encrypt(ArgumentMatchers.anyString());
    }
}
