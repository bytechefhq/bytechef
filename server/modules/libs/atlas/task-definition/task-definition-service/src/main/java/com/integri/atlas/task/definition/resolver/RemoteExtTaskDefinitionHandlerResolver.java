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

package com.integri.atlas.task.definition.resolver;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskAuthDefinition;
import com.integri.atlas.task.definition.model.TaskDefinition;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class RemoteExtTaskDefinitionHandlerResolver extends AbstractExtTaskDefinitionHandlerResolver {

    public static final String REMOTE = "REMOTE";

    public RemoteExtTaskDefinitionHandlerResolver(
        ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository
    ) {
        super(extTaskDefinitionHandlerRepository, REMOTE);
    }

    @Override
    protected TaskDefinitionHandler createTaskDefinitionHandler(String name) {
        return new RemoteExtTaskDefinitionHandlerProxy(name);
    }

    private static class RemoteExtTaskDefinitionHandlerProxy implements TaskDefinitionHandler {

        private final String name;

        private RemoteExtTaskDefinitionHandlerProxy(String name) {
            this.name = name;
        }

        @Override
        public List<TaskAuthDefinition> getTaskAuthDefinitions() {
            return null;
        }

        @Override
        public TaskDefinition getTaskDefinition() {
            return DSL.createTaskDefinition(name);
        }
    }
}
