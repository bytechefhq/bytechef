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

package com.bytechef.embedded.execution.facade;

import com.bytechef.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ActionFacadeImpl implements ActionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ConnectionIdHelper connectionIdHelper;

    @SuppressFBWarnings("EI")
    public ActionFacadeImpl(ActionDefinitionFacade actionDefinitionFacade, ConnectionIdHelper connectionIdHelper) {
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.connectionIdHelper = connectionIdHelper;
    }

    @Override
    public Object executeAction(
        String componentName, Integer componentVersion, String actionName, Map<String, Object> inputParameters,
        Environment environment, @Nullable Long instanceId) {

        Long connectionId = connectionIdHelper.getConnectionId(componentName, environment, instanceId);

        return actionDefinitionFacade.executePerform(
            componentName, componentVersion, actionName, ModeType.EMBEDDED, null, null, null, null, inputParameters,
            connectionId == null ? Map.of() : Map.of(componentName, connectionId), Map.of(), false);
    }
}
