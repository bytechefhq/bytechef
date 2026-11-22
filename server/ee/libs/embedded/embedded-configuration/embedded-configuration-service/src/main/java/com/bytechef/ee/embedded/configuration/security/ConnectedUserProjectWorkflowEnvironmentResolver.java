/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserProjectWorkflowEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserService connectedUserService;
    private final ConnectUserProjectRepository connectUserProjectRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowEnvironmentResolver(
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserService connectedUserService, ConnectUserProjectRepository connectUserProjectRepository) {

        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserService = connectedUserService;
        this.connectUserProjectRepository = connectUserProjectRepository;
    }

    @Override
    public String resourceType() {
        return "ConnectedUserProjectWorkflow";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return connectedUserProjectWorkflowRepository.findById(number.longValue())
            .map(ConnectedUserProjectWorkflow::getConnectedUserProjectId)
            .flatMap(connectUserProjectRepository::findById)
            .map(ConnectedUserProject::getConnectedUserId)
            .flatMap(connectedUserId -> connectedUserService.fetchConnectedUser(connectedUserId))
            .map(ConnectedUser::getEnvironment);
    }
}
