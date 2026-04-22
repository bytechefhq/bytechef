/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI Gateway projects.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayProjectGraphQlController {

    private final AiGatewayProjectService aiGatewayProjectService;

    @SuppressFBWarnings("EI")
    AiGatewayProjectGraphQlController(AiGatewayProjectService aiGatewayProjectService) {
        this.aiGatewayProjectService = aiGatewayProjectService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiGatewayProject aiGatewayProject(@Argument long id) {
        return aiGatewayProjectService.getProject(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayProject> aiGatewayProjects(@Argument long workspaceId) {
        return aiGatewayProjectService.getProjectsByWorkspaceId(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProject createAiGatewayProject(@Argument CreateAiGatewayProjectInput input) {
        AiGatewayProject project = new AiGatewayProject(
            Long.parseLong(input.workspaceId()), input.name(), input.slug());

        if (input.cachingEnabled() != null) {
            project.setCachingEnabled(input.cachingEnabled());
        }

        if (input.cacheTtlMinutes() != null) {
            project.setCacheTtlMinutes(input.cacheTtlMinutes());
        }

        if (input.compressionEnabled() != null) {
            project.setCompressionEnabled(input.compressionEnabled());
        }

        if (input.description() != null) {
            project.setDescription(input.description());
        }

        if (input.logRetentionDays() != null) {
            project.setLogRetentionDays(input.logRetentionDays());
        }

        if (input.retryMaxAttempts() != null) {
            project.setRetryMaxAttempts(input.retryMaxAttempts());
        }

        if (input.routingPolicyId() != null) {
            project.setRoutingPolicyId(Long.parseLong(input.routingPolicyId()));
        }

        if (input.timeoutSeconds() != null) {
            project.setTimeoutSeconds(input.timeoutSeconds());
        }

        return aiGatewayProjectService.create(project);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayProject(@Argument long id) {
        aiGatewayProjectService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProject updateAiGatewayProject(
        @Argument long id, @Argument UpdateAiGatewayProjectInput input) {

        AiGatewayProject project = aiGatewayProjectService.getProject(id);

        if (input.cachingEnabled() != null) {
            project.setCachingEnabled(input.cachingEnabled());
        }

        if (input.cacheTtlMinutes() != null) {
            project.setCacheTtlMinutes(input.cacheTtlMinutes());
        }

        if (input.compressionEnabled() != null) {
            project.setCompressionEnabled(input.compressionEnabled());
        }

        if (input.description() != null) {
            project.setDescription(input.description());
        }

        if (input.logRetentionDays() != null) {
            project.setLogRetentionDays(input.logRetentionDays());
        }

        if (input.name() != null) {
            project.setName(input.name());
        }

        if (input.retryMaxAttempts() != null) {
            project.setRetryMaxAttempts(input.retryMaxAttempts());
        }

        if (input.routingPolicyId() != null) {
            project.setRoutingPolicyId(Long.parseLong(input.routingPolicyId()));
        }

        if (input.slug() != null) {
            project.setSlug(input.slug());
        }

        if (input.timeoutSeconds() != null) {
            project.setTimeoutSeconds(input.timeoutSeconds());
        }

        return aiGatewayProjectService.update(project);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayProjectInput(
        Boolean cachingEnabled, Integer cacheTtlMinutes, Boolean compressionEnabled, String description,
        Integer logRetentionDays, String name, Integer retryMaxAttempts, String routingPolicyId, String slug,
        Integer timeoutSeconds, String workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayProjectInput(
        Boolean cachingEnabled, Integer cacheTtlMinutes, Boolean compressionEnabled, String description,
        Integer logRetentionDays, String name, Integer retryMaxAttempts, String routingPolicyId, String slug,
        Integer timeoutSeconds) {
    }
}
