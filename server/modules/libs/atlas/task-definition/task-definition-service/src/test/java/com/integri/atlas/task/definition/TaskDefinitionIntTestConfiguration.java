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

package com.integri.atlas.task.definition;

import com.integri.atlas.task.definition.handler.TaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskAuthDefinition;
import com.integri.atlas.task.definition.model.TaskDefinition;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import com.integri.atlas.task.definition.repository.memory.InMemoryExtTaskDefinitionHandlerRepository;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TaskDefinitionIntTestConfiguration {

    @Bean
    ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository() {
        return new InMemoryExtTaskDefinitionHandlerRepository();
    }

    @Bean
    TaskDefinitionHandler memoryTaskDefinitionHandler() {
        return new TaskDefinitionHandler() {
            @Override
            public List<TaskAuthDefinition> getTaskAuthDefinitions() {
                return null;
            }

            @Override
            public TaskDefinition getTaskDefinition() {
                return DSL.createTaskDefinition("memory");
            }
        };
    }
}
