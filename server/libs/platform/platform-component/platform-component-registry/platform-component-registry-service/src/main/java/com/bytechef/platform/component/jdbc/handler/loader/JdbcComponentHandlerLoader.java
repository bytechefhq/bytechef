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

import com.bytechef.hermes.component.JdbcComponentDefinitionFactory;
import com.bytechef.platform.component.handler.ComponentTaskHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandler;
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

        for (JdbcComponentDefinitionFactory jdbcComponentDefinitionFactory : ServiceLoader.load(
            JdbcComponentDefinitionFactory.class)) {

            JdbcComponentHandler jdbcComponentHandler = new JdbcComponentHandler(
                jdbcComponentDefinitionFactory.getJdbcComponentDefinition());

            componentHandlerEntries.add(
                new ComponentHandlerEntry(
                    jdbcComponentHandler,
                    (actionName, actionDefinitionFacade) -> new ComponentTaskHandler(
                        jdbcComponentHandler.getName(), jdbcComponentHandler.getVersion(), actionName,
                        actionDefinitionFacade)));
        }

        return componentHandlerEntries;
    }
}
