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

package com.bytechef.embedded.connectivity.facade;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ActionFacadeImpl implements ActionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ActionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ConnectedUserService connectedUserService,
        IntegrationInstanceService integrationInstanceService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public Object executeAction(
        String componentName, Integer componentVersion, String actionName, Long connectionId,
        Map<String, Object> input, Environment environment) {

        if (connectionId == null) {
            String externalId = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(environment, externalId);

            connectionId = integrationInstanceService
                .fetchFirstIntegrationInstance(connectedUser.getId(), componentName, environment)
                .map(IntegrationInstance::getConnectionId)
                .orElse(null);
        }

        return actionDefinitionFacade.executePerform(
            componentName, componentVersion, actionName, ModeType.EMBEDDED, null, null, null, null, input,
            connectionId == null ? Map.of() : Map.of(componentName, connectionId), Map.of(), false);
    }
}
