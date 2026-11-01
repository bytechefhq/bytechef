/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.config;

import com.bytechef.component.datastream.script.DataStreamCodeEditorScriptInputProvider;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.component.remote.client.config.ComponentWorker.ComponentWorkerServices;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.facade.ActionDefinitionFacadeImpl;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacadeImpl;
import com.bytechef.platform.component.facade.CodeEditorScriptInputFacadeImpl;
import com.bytechef.platform.component.facade.ComponentDefinitionFacadeImpl;
import com.bytechef.platform.component.facade.TriggerDefinitionFacadeImpl;
import com.bytechef.platform.component.service.ActionDefinitionServiceImpl;
import com.bytechef.platform.component.service.ClusterElementDefinitionServiceImpl;
import com.bytechef.platform.component.service.ComponentDefinitionServiceImpl;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionServiceImpl;
import com.bytechef.platform.component.service.TriggerDefinitionServiceImpl;
import com.bytechef.platform.component.service.UnifiedApiDefinitionServiceImpl;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentWorkerFactory {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final ObjectMapper objectMapper;
    private final TokenRefreshHandler tokenRefreshHandler;

    @SuppressFBWarnings("EI2")
    public ComponentWorkerFactory(
        ApplicationEventPublisher applicationEventPublisher, ComponentDefinitionRegistry componentDefinitionRegistry,
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory, ObjectMapper objectMapper, @Lazy TokenRefreshHandler tokenRefreshHandler) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.objectMapper = objectMapper;
        this.tokenRefreshHandler = tokenRefreshHandler;
    }

    public ComponentWorker start(String instanceId) throws IOException {
        return new ComponentWorker(
            instanceId, createServices(componentDefinitionRegistry, connectionDefinitionService), objectMapper);
    }

    public ComponentWorker start(String instanceId, ComponentDefinitionRegistry otherComponentDefinitionRegistry)
        throws IOException {

        return new ComponentWorker(
            instanceId,
            createServices(
                otherComponentDefinitionRegistry,
                new ConnectionDefinitionServiceImpl(
                    otherComponentDefinitionRegistry, contextFactory, tokenRefreshHandler)),
            objectMapper);
    }

    private ComponentWorkerServices createServices(
        ComponentDefinitionRegistry registry, ConnectionDefinitionService registryConnectionDefinitionService) {

        ActionDefinitionServiceImpl actionDefinitionService = new ActionDefinitionServiceImpl(
            registry, contextFactory);
        ClusterElementDefinitionServiceImpl clusterElementDefinitionService = new ClusterElementDefinitionServiceImpl(
            registry, contextFactory, new DefaultListableBeanFactory().getBeanProvider(MeterRegistry.class));
        ComponentDefinitionServiceImpl componentDefinitionService = new ComponentDefinitionServiceImpl(
            List.of(), registry, contextFactory);
        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            registry, contextFactory, applicationEventPublisher);

        List<String> componentNames = registry.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::getName)
            .distinct()
            .toList();

        return new ComponentWorkerServices(
            new ActionDefinitionFacadeImpl(connectionService, actionDefinitionService), actionDefinitionService,
            new ClusterElementDefinitionFacadeImpl(clusterElementDefinitionService, connectionService, contextFactory),
            clusterElementDefinitionService,
            new CodeEditorScriptInputFacadeImpl(
                List.of(new DataStreamCodeEditorScriptInputProvider(clusterElementDefinitionService, contextFactory)),
                connectionService),
            new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService),
            componentDefinitionService, registryConnectionDefinitionService,
            new TriggerDefinitionFacadeImpl(connectionService, triggerDefinitionService), triggerDefinitionService,
            new UnifiedApiDefinitionServiceImpl(registry), componentNames);
    }
}
