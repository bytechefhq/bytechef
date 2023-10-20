
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.jdbc.handler.loader;

import com.bytechef.hermes.component.JdbcComponentDefinitionFactory;
import com.bytechef.hermes.component.handler.ComponentTaskHandler;
import com.bytechef.hermes.component.jdbc.handler.JdbcComponentHandler;
import com.bytechef.hermes.component.handler.loader.ComponentHandlerLoader;

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
                    (actionName, actionDefinitionService) -> new ComponentTaskHandler(
                        jdbcComponentHandler.getName(), jdbcComponentHandler.getVersion(), actionName,
                        actionDefinitionService)));
        }

        return componentHandlerEntries;
    }
}
