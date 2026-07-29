/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowInfo;
import com.bytechef.ee.automation.ai.tool.exception.CodeWorkflowToolErrorType;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
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
public class ReadCodeWorkflowTools {

    private static final Logger log = LoggerFactory.getLogger(ReadCodeWorkflowTools.class);

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;
    private final ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier;

    @SuppressFBWarnings("EI")
    public ReadCodeWorkflowTools(
        ProjectCodeWorkflowFacade projectCodeWorkflowFacade,
        ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier) {

        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
        this.projectCodeWorkflowInfoSupplier = projectCodeWorkflowInfoSupplier;
    }

    @Tool(description = "Read the source code of a code workflow project by its ID. Returns the source code as text.")
    public String getCodeWorkflowSource(
        @ToolParam(description = "The ID of the code workflow project") long projectId) {

        try {
            String source = projectCodeWorkflowFacade.getCodeWorkflowSource(projectId);

            if (log.isDebugEnabled()) {
                log.debug("getCodeWorkflowSource({}): Retrieved code workflow source", projectId);
            }

            return source;
        } catch (Exception e) {
            log.error("getCodeWorkflowSource({}): Failed to get code workflow source", projectId, e);

            throw new ExecutionException(
                "Failed to get code workflow source: " + e.getMessage(), e, CodeWorkflowToolErrorType.GET_SOURCE);
        }
    }

    @Tool(
        description = "List all code workflow projects. Returns each project's ID, name, and language.")
    public List<CodeWorkflowProjectInfo> listCodeWorkflows() {
        try {
            List<Project> projects = projectCodeWorkflowFacade.getCodeWorkflowProjects();

            List<CodeWorkflowProjectInfo> codeWorkflowProjectInfos = projects.stream()
                .map(project -> new CodeWorkflowProjectInfo(
                    project.getId(), project.getName(), resolveLanguage(project.getId())))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug("listCodeWorkflows(): Found {} code workflow project(s)", codeWorkflowProjectInfos.size());
            }

            return codeWorkflowProjectInfos;
        } catch (Exception e) {
            log.error("listCodeWorkflows(): Failed to list code workflow projects", e);

            throw new ExecutionException(
                "Failed to list code workflow projects: " + e.getMessage(), e, CodeWorkflowToolErrorType.LIST);
        }
    }

    private String resolveLanguage(Long projectId) {
        Optional<CodeWorkflowInfo> codeWorkflowInfo = projectCodeWorkflowInfoSupplier.fetchCodeWorkflowInfo(
            projectId);

        return codeWorkflowInfo.map(CodeWorkflowInfo::language)
            .orElse(null);
    }

    public record CodeWorkflowProjectInfo(Long id, String name, String language) {
    }
}
