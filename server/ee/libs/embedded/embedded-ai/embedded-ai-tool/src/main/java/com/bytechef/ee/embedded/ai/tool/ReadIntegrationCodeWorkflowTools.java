/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.exception.IntegrationCodeWorkflowToolErrorType;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationCodeWorkflowInfo;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for reading embedded code workflow integrations. The embedded mirror of the automation
 * {@code ReadCodeWorkflowTools}, re-keyed from project ids onto integration ids.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ReadIntegrationCodeWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(ReadIntegrationCodeWorkflowTools.class);

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ReadIntegrationCodeWorkflowTools(IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade) {
        this.integrationCodeWorkflowFacade = integrationCodeWorkflowFacade;
    }

    @Tool(
        description = "Read the source code of a code workflow integration by its ID. Returns the source code as text.")
    public String getIntegrationCodeWorkflowSource(
        @ToolParam(description = "The ID of the code workflow integration") long integrationId) {

        try {
            String source = integrationCodeWorkflowFacade.getCodeWorkflowSource(integrationId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "getIntegrationCodeWorkflowSource({}): Retrieved code workflow source", integrationId);
            }

            return source;
        } catch (Exception e) {
            log.error(
                "getIntegrationCodeWorkflowSource({}): Failed to get code workflow source", integrationId, e);

            throw new ExecutionException(
                "Failed to get code workflow source: " + e.getMessage(), e,
                IntegrationCodeWorkflowToolErrorType.GET_SOURCE);
        }
    }

    @Tool(
        description = "List all code workflow integrations. Returns each integration's ID, component name, and language.")
    public List<IntegrationCodeWorkflowInfo> listIntegrationCodeWorkflows() {
        try {
            List<Integration> integrations = integrationCodeWorkflowFacade.getCodeWorkflowIntegrations();

            List<IntegrationCodeWorkflowInfo> integrationCodeWorkflowInfos = integrations.stream()
                .map(integration -> new IntegrationCodeWorkflowInfo(
                    integration.getId(), integration.getComponentName(), resolveLanguage(integration.getId())))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "listIntegrationCodeWorkflows(): Found {} code workflow integration(s)",
                    integrationCodeWorkflowInfos.size());
            }

            return integrationCodeWorkflowInfos;
        } catch (Exception e) {
            log.error("listIntegrationCodeWorkflows(): Failed to list code workflow integrations", e);

            throw new ExecutionException(
                "Failed to list code workflow integrations: " + e.getMessage(), e,
                IntegrationCodeWorkflowToolErrorType.LIST);
        }
    }

    private String resolveLanguage(Long integrationId) {
        return integrationCodeWorkflowFacade.getCodeWorkflowLanguage(integrationId)
            .orElse(null);
    }
}
