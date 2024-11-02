/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceService {

    List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId);

    List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds);

    List<IntegrationInstance> getConnectedUserEnabledIntegrationInstances(long connectedUserId);

    IntegrationInstance getIntegrationInstance(long id);

    IntegrationInstance getIntegrationInstance(long connectedUserId, String workflowId, Environment environment);

    List<IntegrationInstance> getIntegrationInstances(long integrationInstanceConfigurationId);

    void updateEnabled(long id, boolean enable);

}
