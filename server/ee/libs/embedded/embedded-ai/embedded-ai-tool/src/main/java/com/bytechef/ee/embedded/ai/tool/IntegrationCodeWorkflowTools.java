/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.exception.IntegrationCodeWorkflowToolErrorType;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationCodeWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCodeWorkflowTools.class);

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public IntegrationCodeWorkflowTools(IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade) {
        this.integrationCodeWorkflowFacade = integrationCodeWorkflowFacade;
    }

    @Tool(
        description = "Create a new empty code workflow integration. Supported languages are JAVASCRIPT and " +
            "PYTHON (JAVA is not supported). Returns a confirmation message with the created integration's ID and component name.")
    public String createIntegrationCodeWorkflow(
        @ToolParam(description = "The component name of the code workflow integration") String componentName,
        @ToolParam(
            description = "Language of the code workflow: JAVASCRIPT or PYTHON") String language) {

        Language resolvedLanguage = resolveLanguage(language);

        try {
            Integration integration = integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
                componentName, resolvedLanguage);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createIntegrationCodeWorkflow({}): Created code workflow integration with id={}", componentName,
                    integration.getId());
            }

            return "Created code workflow integration " + integration.getId() + " (" + integration.getComponentName()
                + ").";
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "createIntegrationCodeWorkflow(" + componentName + ")",
                "Failed to create code workflow integration", exception,
                IntegrationCodeWorkflowToolErrorType.CREATE);
        }
    }

    @Tool(
        description = "Update the source code of an existing code workflow integration. Returns a confirmation message.")
    public String updateIntegrationCodeWorkflowSource(
        @ToolParam(description = "The ID of the code workflow integration to update") long integrationId,
        @ToolParam(description = "The new source code content of the code workflow") String content) {

        try {
            integrationCodeWorkflowFacade.updateCodeWorkflowSource(integrationId, content);

            if (log.isDebugEnabled()) {
                log.debug("updateIntegrationCodeWorkflowSource({}): Updated code workflow source", integrationId);
            }

            return "Updated code workflow integration " + integrationId + ".";
        } catch (RuntimeException exception) {
            throw EmbeddedToolFailures.toExecutionException(
                log, "updateIntegrationCodeWorkflowSource(" + integrationId + ")",
                "Failed to update code workflow source", exception,
                IntegrationCodeWorkflowToolErrorType.UPDATE_SOURCE);
        }
    }

    private static Language resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new ExecutionException(
                "Language must not be blank. Supported languages: JAVASCRIPT, PYTHON.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        Language resolvedLanguage;

        try {
            resolvedLanguage = Language.valueOf(
                language.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        if (resolvedLanguage != Language.JAVASCRIPT && resolvedLanguage != Language.PYTHON) {
            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        return resolvedLanguage;
    }
}
