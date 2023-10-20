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

package com.bytechef.hermes.descriptor;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.repository.ExtAuthenticationDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.repository.memory.InMemoryExtAuthenticationDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.repository.memory.InMemoryExtTaskDescriptorHandlerRepository;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DescriptorIntTestConfiguration {

    @Bean
    ExtAuthenticationDescriptorHandlerRepository memoryExtAuthenticationDescriptorHandlerRepository() {
        return new InMemoryExtAuthenticationDescriptorHandlerRepository();
    }

    @Bean
    ExtTaskDescriptorHandlerRepository memoryExtTaskDescriptorHandlerRepository() {
        return new InMemoryExtTaskDescriptorHandlerRepository();
    }

    @Bean
    AuthenticationDescriptorHandler memoryAuthenticationDescriptorHandler() {
        return () ->
                DSL.createAuthenticationDescriptors("csvFile", List.of(DSL.createAuthenticationDescriptor("auth1")));
    }

    @Bean
    TaskDescriptorHandler memoryTaskDDescriptorHandler() {
        return () -> DSL.createTaskDescriptor("csvFile");
    }
}
