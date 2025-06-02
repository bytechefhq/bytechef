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

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectService {

    ConnectedUserProject create(ConnectedUserProject connectedUserProject);

    ConnectedUserProject create(long connectedUserId, long projectId);

    void delete(Long id);

    ConnectedUserProject getConnectedUserProject(Long id);

    Optional<ConnectedUserProject> fetchConnectUserProject(String externalUserId, Environment environment);

    List<ConnectedUserProject> getConnectedUserProjects();
}
