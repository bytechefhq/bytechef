/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The SOURCE-side half of connection-mapping validation, and the mandatory companion to
 * {@link ConnectionEnvironmentMapper#validate(long, com.bytechef.platform.configuration.domain.Environment, Map)}.
 *
 * <p>
 * {@code validate} constrains only the TARGET of each mapping — that it exists, is visible to the workspace, and
 * matches its source's component and version. Nothing in it constrains the KEYS, so a caller may submit a mapping for
 * any source connection id in the installation and have it validated and then applied. Every
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} must therefore run this check first,
 * against the set of connections the resource being promoted actually uses.
 * </p>
 *
 * <p>
 * The check takes a bare {@code Set<Long>} rather than deriving the ids itself, because where they come from differs
 * per resource type: an API collection's are its deployment's {@link SourceBinding}s, while an MCP server's are those
 * UNION its {@code mcp_component.connection_id}s. Building the union is the handler's job; refusing anything outside it
 * is this class's. {@link #sourceConnectionIds(List)} supplies the {@link SourceBinding} half, which every resource
 * type needs.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionConnectionScope {

    private PromotionConnectionScope() {
    }

    /**
     * The connection ids a deployment's source bindings name - the half of the union that is identical for every
     * resource type. Three of the four handlers pass this straight to
     * {@link #checkMappedConnectionsBelongToSource(Set, Map)}; MCP unions its {@code mcp_component.connection_id}s on
     * top of it first.
     *
     * @param sourceBindings the source deployment's connection bindings
     * @return the distinct connection ids they name, in encounter order
     */
    public static Set<Long> sourceConnectionIds(List<SourceBinding> sourceBindings) {
        Set<Long> sourceConnectionIds = new LinkedHashSet<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            sourceConnectionIds.add(sourceBinding.connectionId());
        }

        return sourceConnectionIds;
    }

    /**
     * Rejects any requested mapping whose SOURCE id is not one of the connections the promoted resource uses. Must be
     * called BEFORE
     * {@link ConnectionEnvironmentMapper#validate(long, com.bytechef.platform.configuration.domain.Environment, Map)},
     * so a smuggled source id is never handed to it.
     *
     * @param sourceConnectionIds every connection the resource being promoted binds, however the handler derives them
     * @param requestedMappings   source connection id to target connection id, as submitted by the caller
     * @throws ConfigurationException with {@link EnvironmentPromotionErrorType#TARGET_CONNECTION_INVALID} on the first
     *                                mapping that names a connection outside {@code sourceConnectionIds}
     */
    public static void checkMappedConnectionsBelongToSource(
        Set<Long> sourceConnectionIds, Map<Long, Long> requestedMappings) {

        for (Long mappedSourceConnectionId : requestedMappings.keySet()) {
            if (!sourceConnectionIds.contains(mappedSourceConnectionId)) {
                throw new ConfigurationException(
                    "Connection mapping references connection %s, which the promoted resource does not use"
                        .formatted(mappedSourceConnectionId),
                    EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID);
            }
        }
    }
}
