/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.handler;

import com.bytechef.component.ComponentHandler;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.handler.reader.ComponentDefinitionReader;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiConnectorDynamicComponentHandlerRegistry implements DynamicComponentHandlerRegistry {

    private final ApiConnectorService apiConnectorService;
    private final ComponentDefinitionReader componentDefinitionReader;

    @SuppressFBWarnings("EI")
    public ApiConnectorDynamicComponentHandlerRegistry(
        ApiConnectorService apiConnectorService, ComponentDefinitionReader componentDefinitionReader) {

        this.apiConnectorService = apiConnectorService;
        this.componentDefinitionReader = componentDefinitionReader;
    }

    @Override
    public List<? extends ComponentHandler> getComponentHandlers() {
        return apiConnectorService.getApiConnectors()
            .stream()
            .filter(ApiConnector::getEnabled)
            .map(componentDefinitionReader::readComponentDefinition)
            .map(componentDefinition -> (ComponentHandler) () -> componentDefinition)
            .toList();
    }

    @Override
    public Optional<ComponentHandler> fetchComponentHandler(String name, int connectorVersion) {
        return apiConnectorService.fetchApiConnector(name, connectorVersion)
            .map(apiConnector -> () -> componentDefinitionReader.readComponentDefinition(apiConnector));
    }
}
