/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubMcpServerUrlGuard {

    private final Set<String> allowedHosts;

    public AiHubMcpServerUrlGuard(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Hub.McpServer mcpServer = ai.getHub()
            .getMcpServer();

        this.allowedHosts = Set.copyOf(mcpServer.getAllowedHosts());
    }

    public void validate(String url) {
        UrlValidatorUtils.validate(url, allowedHosts);
    }
}
