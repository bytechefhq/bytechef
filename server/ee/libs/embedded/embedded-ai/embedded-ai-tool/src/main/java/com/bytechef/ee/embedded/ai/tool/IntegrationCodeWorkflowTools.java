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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for creating and updating embedded code workflow integrations. The embedded mirror of the automation
 * {@code CodeWorkflowTools}, re-keyed from workspace-scoped project names onto globally-keyed component names: embedded
 * integrations are not workspace-scoped, so there is no workspace id parameter here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class IntegrationCodeWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCodeWorkflowTools.class);

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public IntegrationCodeWorkflowTools(IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade) {
        this.integrationCodeWorkflowFacade = integrationCodeWorkflowFacade;
    }

    @Tool(
        description = "Create a new empty code workflow integration. Supported languages are JAVASCRIPT, PYTHON and " +
            "RUBY (JAVA is not supported). Returns a confirmation message with the created integration's ID and component name.")
    public String createIntegrationCodeWorkflow(
        @ToolParam(description = "The component name of the code workflow integration") String componentName,
        @ToolParam(
            description = "Language of the code workflow: JAVASCRIPT, PYTHON, or RUBY") String language) {

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
        } catch (Exception e) {
            log.error(
                "createIntegrationCodeWorkflow({}): Failed to create code workflow integration", componentName, e);

            throw new ExecutionException(
                "Failed to create code workflow integration: " + e.getMessage(), e,
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
        } catch (Exception e) {
            log.error(
                "updateIntegrationCodeWorkflowSource({}): Failed to update code workflow source", integrationId, e);

            throw new ExecutionException(
                "Failed to update code workflow source: " + e.getMessage(), e,
                IntegrationCodeWorkflowToolErrorType.UPDATE_SOURCE);
        }
    }

    /**
     * Maps the incoming language name to {@link Language}, rejecting anything other than JAVASCRIPT, PYTHON, and RUBY.
     * JAVA is a valid {@link Language} member but code workflows created through this tool are always
     * polyglot-script-backed, so it is rejected here even though the facade would also reject it.
     */
    private static Language resolveLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new ExecutionException(
                "Language must not be blank. Supported languages: JAVASCRIPT, PYTHON, RUBY.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        Language resolvedLanguage;

        try {
            resolvedLanguage = Language.valueOf(
                language.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON, RUBY.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        if (resolvedLanguage != Language.JAVASCRIPT && resolvedLanguage != Language.PYTHON
            && resolvedLanguage != Language.RUBY) {

            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON, RUBY.",
                IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        return resolvedLanguage;
    }
}
