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

package com.bytechef.platform.component.jdbc.handler.loader;

import com.bytechef.platform.component.JdbcComponentHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandlerImpl;
import com.bytechef.platform.component.task.handler.ComponentTaskHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentHandlerLoader implements ComponentHandlerLoader {

    @Override
    public List<ComponentHandlerEntry> loadComponentHandlers() {
        List<ComponentHandlerEntry> componentHandlerEntries = new ArrayList<>();

        for (JdbcComponentHandler jdbcComponentHandler : ServiceLoader.load(JdbcComponentHandler.class)) {
            JdbcComponentHandlerImpl jdbcComponentHandlerImpl = new JdbcComponentHandlerImpl(
                jdbcComponentHandler.getJdbcComponentDefinition());

            componentHandlerEntries.add(
                new ComponentHandlerEntry(
                    jdbcComponentHandlerImpl,
                    (actionName, actionDefinitionFacade) -> new ComponentTaskHandler(
                        jdbcComponentHandlerImpl.getName(), jdbcComponentHandlerImpl.getVersion(), actionName,
                        actionDefinitionFacade)));
        }

        return componentHandlerEntries;
    }
}
