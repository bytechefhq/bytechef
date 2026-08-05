/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.workflow.connection;

import com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactory;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Maps a code workflow task's declared connections onto the platform's {@link ComponentConnection} model. The
 * {@code codeWorkflow} component declares no connection of its own — the connections a task needs come from the
 * components its perform invokes through {@code context.component}, declared in the source and emitted into the task's
 * {@code extensions.connections} map. Structurally the same problem the Script component solves, so this mirrors
 * {@code ScriptComponentConnectionFactory}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(3)
@ConditionalOnEEVersion
class CodeWorkflowComponentConnectionFactory implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowComponentConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        return ComponentConnection.of(
            extensions, workflowNodeName,
            (name, version) -> {
                ComponentDefinition curComponentDefinition = componentDefinitionService.getComponentDefinition(
                    name, version);

                return curComponentDefinition.isConnectionRequired();
            });
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(
            Objects.equals(componentDefinition.getName(), CodeWorkflowConstants.CODE_WORKFLOW) ? this : null);
    }
}
