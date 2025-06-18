/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectService {

    boolean containsProjectDeployment(long projectDeploymentId);

    ConnectedUserProject create(ConnectedUserProject connectedUserProject);

    ConnectedUserProject create(long connectedUserId, long projectId);

    void delete(Long id);

    Optional<ConnectedUserProject> fetchConnectUserProject(String externalUserId, Environment environment);

    ConnectedUserProject getConnectedUserProject(Long id);

    @Transactional(readOnly = true)
    ConnectedUserProject getConnectedUserConnectedUserProject(Long connectedUserid);

    ConnectedUserProject getConnectUserProject(String externalUserId, Environment environment);

    List<ConnectedUserProject> getConnectedUserProjects(Long connectedUserId, Environment environment);
}
