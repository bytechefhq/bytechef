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

package com.bytechef.platform.component.jdbc.handler.loader;

import com.bytechef.platform.component.JdbcComponentHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandlerImpl;
import com.bytechef.platform.component.task.handler.ComponentTaskHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentHandlerLoader implements ComponentHandlerLoader {

    @Override
    public String getKind() {
        return "jdbc";
    }

    @Override
    public List<ComponentHandlerEntry> loadComponentHandlers() {
        List<ComponentHandlerEntry> componentHandlerEntries = new ArrayList<>();

        for (JdbcComponentHandler jdbcComponentHandler : ServiceLoader.load(
            JdbcComponentHandler.class, JdbcComponentHandler.class.getClassLoader())) {

            componentHandlerEntries.add(wrap(jdbcComponentHandler));
        }

        return componentHandlerEntries;
    }

    @Override
    public Optional<ComponentHandlerEntry> loadComponentHandler(String providerClassName) {
        // Anchored to the service interface's classloader; the thread-context classloader can differ on
        // Reactor/virtual threads and resolve providers against another copy of the interface ("not a subtype").
        return ServiceLoader.load(JdbcComponentHandler.class, JdbcComponentHandler.class.getClassLoader())
            .stream()
            .filter(provider -> {
                Class<? extends JdbcComponentHandler> type = provider.type();

                return Objects.equals(type.getName(), providerClassName);
            })
            .findFirst()
            .map(provider -> wrap(provider.get()));
    }

    @Override
    public List<ProviderEntry> loadProviderEntries() {
        List<ProviderEntry> providerEntries = new ArrayList<>();

        for (ServiceLoader.Provider<JdbcComponentHandler> provider : ServiceLoader
            .load(JdbcComponentHandler.class, JdbcComponentHandler.class.getClassLoader())
            .stream()
            .toList()) {

            Class<? extends JdbcComponentHandler> type = provider.type();

            providerEntries.add(new ProviderEntry(type.getName(), wrap(provider.get())));
        }

        return providerEntries;
    }

    private ComponentHandlerEntry wrap(JdbcComponentHandler jdbcComponentHandler) {
        JdbcComponentHandlerImpl jdbcComponentHandlerImpl = new JdbcComponentHandlerImpl(
            jdbcComponentHandler.getJdbcComponentDefinition());

        return new ComponentHandlerEntry(
            jdbcComponentHandlerImpl,
            (actionName, actionDefinitionFacade) -> new ComponentTaskHandler(
                jdbcComponentHandlerImpl.getName(), jdbcComponentHandlerImpl.getVersion(), actionName,
                actionDefinitionFacade));
    }
}
