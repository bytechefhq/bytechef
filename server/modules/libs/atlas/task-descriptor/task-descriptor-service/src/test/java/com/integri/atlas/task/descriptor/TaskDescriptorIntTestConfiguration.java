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

package com.integri.atlas.task.descriptor;

import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandler;
import com.integri.atlas.task.descriptor.handler.TaskDescriptorHandler;
import com.integri.atlas.task.descriptor.model.DSL;
import com.integri.atlas.task.descriptor.repository.ExtTaskAuthDescriptorHandlerRepository;
import com.integri.atlas.task.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import com.integri.atlas.task.descriptor.repository.memory.InMemoryExtTaskAuthDescriptorHandlerRepository;
import com.integri.atlas.task.descriptor.repository.memory.InMemoryExtTaskDescriptorHandlerRepository;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TaskDescriptorIntTestConfiguration {

    @Bean
    ExtTaskAuthDescriptorHandlerRepository extTaskAuthDescriptorHandlerRepository() {
        return new InMemoryExtTaskAuthDescriptorHandlerRepository();
    }

    @Bean
    ExtTaskDescriptorHandlerRepository extTaskDescriptorHandlerRepository() {
        return new InMemoryExtTaskDescriptorHandlerRepository();
    }

    @Bean
    TaskAuthDescriptorHandler memoryTaskAuthDescriptorHandler() {
        return () -> DSL.createTaskAuthDescriptors("csvFile", List.of(DSL.createTaskAuthDescriptor("auth1")));
    }

    @Bean
    TaskDescriptorHandler memoryTaskDDescriptorHandler() {
        return () -> DSL.createTaskDescriptor("csvFile");
    }
}
