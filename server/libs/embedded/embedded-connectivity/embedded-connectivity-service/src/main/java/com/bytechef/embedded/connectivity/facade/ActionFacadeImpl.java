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

import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ActionFacadeImpl implements ActionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;

    @SuppressFBWarnings("EI")
    public ActionFacadeImpl(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @Override
    public Object executeAction(
        String componentName, Integer componentVersion, String actionName, Map<String, Object> input) {

        // TODO connection

        return actionDefinitionFacade.executePerform(
            componentName, componentVersion, actionName, AppType.EMBEDDED, null, null, null, input, Map.of(), Map.of());
    }
}
