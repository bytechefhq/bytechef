/*
 * Copyright 2025 ByteChef
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

package com.bytechef.embedded.execution.util;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionIdHelper {

    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectionIdHelper(
        ConnectedUserService connectedUserService, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
    }

    public Long getConnectionId(String componentName, Environment environment, Long instanceId) {
        Long connectionId;

        if (instanceId == null) {
            String externalId = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(environment, externalId);

            connectionId = integrationInstanceService
                .fetchFirstIntegrationInstance(connectedUser.getId(), componentName, environment)
                .map(IntegrationInstance::getConnectionId)
                .orElse(null);
        } else {
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

            connectionId = integrationInstance.getConnectionId();
        }

        return connectionId;
    }
}
