/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.ee.automation.ai.tool.exception.CodeWorkflowToolErrorType;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class CodeWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(CodeWorkflowTools.class);

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public CodeWorkflowTools(ProjectCodeWorkflowFacade projectCodeWorkflowFacade) {
        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
    }

    @Tool(
        description = "Create a new empty code workflow project. Supported languages are JAVASCRIPT, PYTHON and " +
            "RUBY (JAVA is not supported). Returns a confirmation message with the created project's ID and name.")
    public String createCodeWorkflow(
        @ToolParam(
            required = false,
            description = "The workspace ID for the project; defaults to the default workspace when omitted") Long workspaceId,
        @ToolParam(description = "The name of the code workflow project") String name,
        @ToolParam(
            description = "Language of the code workflow: JAVASCRIPT, PYTHON, or RUBY") String language) {

        Language resolvedLanguage = resolveLanguage(language);

        long resolvedWorkspaceId = workspaceId != null ? workspaceId : Workspace.DEFAULT_WORKSPACE_ID;

        try {
            Project project = projectCodeWorkflowFacade.createEmptyCodeWorkflow(
                resolvedWorkspaceId, name, resolvedLanguage);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createCodeWorkflow({}): Created code workflow project with id={}", name, project.getId());
            }

            return "Created code workflow project " + project.getId() + " (" + project.getName() + ").";
        } catch (Exception e) {
            log.error("createCodeWorkflow({}): Failed to create code workflow project", name, e);

            throw new ExecutionException(
                "Failed to create code workflow project: " + e.getMessage(), e, CodeWorkflowToolErrorType.CREATE);
        }
    }

    @Tool(
        description = "Update the source code of an existing code workflow project. Returns a confirmation message.")
    public String updateCodeWorkflowSource(
        @ToolParam(description = "The ID of the code workflow project to update") long projectId,
        @ToolParam(description = "The new source code content of the code workflow") String content) {

        try {
            projectCodeWorkflowFacade.updateCodeWorkflowSource(projectId, content);

            if (log.isDebugEnabled()) {
                log.debug("updateCodeWorkflowSource({}): Updated code workflow source", projectId);
            }

            return "Updated code workflow project " + projectId + ".";
        } catch (Exception e) {
            log.error("updateCodeWorkflowSource({}): Failed to update code workflow source", projectId, e);

            throw new ExecutionException(
                "Failed to update code workflow source: " + e.getMessage(), e,
                CodeWorkflowToolErrorType.UPDATE_SOURCE);
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
                CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        Language resolvedLanguage;

        try {
            resolvedLanguage = Language.valueOf(
                language.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON, RUBY.",
                CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        if (resolvedLanguage != Language.JAVASCRIPT && resolvedLanguage != Language.PYTHON
            && resolvedLanguage != Language.RUBY) {

            throw new ExecutionException(
                "Unsupported language '" + language + "'. Supported languages: JAVASCRIPT, PYTHON, RUBY.",
                CodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE);
        }

        return resolvedLanguage;
    }
}
