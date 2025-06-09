/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceService {

    IntegrationInstance create(IntegrationInstance integrationInstance);

    IntegrationInstance create(long connectedUserId, long connectionId, long integrationInstanceConfigurationId);

    void delete(long id);

    Optional<IntegrationInstance> fetchIntegrationInstance(
        long connectedUserId, String componentName, Environment environment);

    IntegrationInstance getIntegrationInstance(
        long connectedUserId, List<String> componentNames, Environment environment);

    List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId);

    List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId, boolean enabled);

    List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId, Environment environment);

    List<IntegrationInstance> getIntegrationInstances(
        long connectedUserId, String componentName, Environment environment);

    List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds);

    IntegrationInstance getIntegrationInstance(long id);

    IntegrationInstance getIntegrationInstance(long connectedUserId, String workflowId, Environment environment);

    List<IntegrationInstance> getIntegrationInstances(long integrationInstanceConfigurationId);

    void updateEnabled(long id, boolean enable);
}
