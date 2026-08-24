/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.web.graphql;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for promoting an API collection, MCP server, A2A server or project deployment from one environment to
 * another. Thin: authorization and business rules live on the
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} beans the facade dispatches to, so
 * every method here only needs the authenticated-user gate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class EnvironmentPromotionGraphQlController {

    private final EnvironmentPromotionFacade environmentPromotionFacade;

    @SuppressFBWarnings("EI")
    public EnvironmentPromotionGraphQlController(EnvironmentPromotionFacade environmentPromotionFacade) {
        this.environmentPromotionFacade = environmentPromotionFacade;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public EnvironmentPromotionPreviewModel environmentPromotionPreview(
        @Argument PromotionResourceType resourceType, @Argument long sourceId, @Argument long targetEnvironmentId) {

        EnvironmentPromotionPreview preview = environmentPromotionFacade.preview(
            resourceType, sourceId, targetEnvironmentId);

        return toModel(preview);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public EnvironmentPromotionResult promoteToEnvironment(@Argument PromoteToEnvironmentInput input) {
        Map<Long, Long> connectionMappings = new HashMap<>();

        for (PromotionConnectionMappingInput mapping : input.connectionMappings()) {
            connectionMappings.put(mapping.sourceConnectionId(), mapping.targetConnectionId());
        }

        return environmentPromotionFacade.promote(
            input.resourceType(), input.sourceId(), input.targetEnvironmentId(), connectionMappings);
    }

    // GraphQL exposes environments as ordinal ids; the facade DTO carries the Environment enum, so the conversion
    // happens here rather than on the DTO itself.
    private static EnvironmentPromotionPreviewModel toModel(EnvironmentPromotionPreview preview) {
        Environment sourceEnvironment = preview.sourceEnvironment();
        Environment targetEnvironment = preview.targetEnvironment();

        return new EnvironmentPromotionPreviewModel(
            preview.resourceType(), preview.sourceId(), sourceEnvironment.ordinal(), targetEnvironment.ordinal(),
            preview.existingTargetId(), preview.existingTargetName(), preview.projects(), preview.connections(),
            preview.warnings());
    }

    record PromoteToEnvironmentInput(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId,
        List<PromotionConnectionMappingInput> connectionMappings) {
    }

    record PromotionConnectionMappingInput(long sourceConnectionId, long targetConnectionId) {
    }

    /** GraphQL exposes environments as ordinal ids; the facade DTO carries the enum. */
    record EnvironmentPromotionPreviewModel(
        PromotionResourceType resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId,
        @Nullable Long existingTargetId, @Nullable String existingTargetName, List<PromotionProjectPreview> projects,
        List<PromotionConnectionMapping> connections, List<String> warnings) {
    }
}
