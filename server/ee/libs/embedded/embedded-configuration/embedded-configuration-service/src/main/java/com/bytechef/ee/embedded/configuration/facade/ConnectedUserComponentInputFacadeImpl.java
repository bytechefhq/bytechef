/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectedUserComponentInputFacadeImpl implements ConnectedUserComponentInputFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserComponentInputFacadeImpl.class);

    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserComponentInputFacadeImpl(
        ComponentDefinitionFacade componentDefinitionFacade,
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectedUserService connectedUserService) {

        this.componentDefinitionFacade = componentDefinitionFacade;
        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserService = connectedUserService;
    }

    @Override
    public List<Option> getComponentInputOptions(
        String externalUserId, Environment environment, String componentName, int componentVersion, String groupName,
        String propertyName, Map<String, Object> lookupDependsOnValues, @Nullable String searchText,
        long connectionId) {

        if (!isEntitledConnection(externalUserId, environment, componentName, connectionId)) {
            return List.of();
        }

        return componentDefinitionFacade.executeWorkflowInputOptions(
            componentName, componentVersion, groupName, propertyName, lookupDependsOnValues,
            List.copyOf(lookupDependsOnValues.keySet()), searchText, connectionId);
    }

    private boolean isEntitledConnection(
        String externalUserId, Environment environment, String componentName, long connectionId) {

        Long connectedUserId = connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .orElse(null);

        List<ConnectionDTO> entitledConnections = connectedUserId == null
            ? List.of()
            : connectedUserConnectionFacade.getConnections(connectedUserId, componentName, List.of(connectionId));

        boolean entitled = !entitledConnections.isEmpty();

        if (!entitled && log.isWarnEnabled()) {
            log.warn(
                "Denying component input options of {}: connection {} is not a {} connection of connected user {}.",
                componentName, connectionId, componentName, externalUserId);
        }

        return entitled;
    }
}
