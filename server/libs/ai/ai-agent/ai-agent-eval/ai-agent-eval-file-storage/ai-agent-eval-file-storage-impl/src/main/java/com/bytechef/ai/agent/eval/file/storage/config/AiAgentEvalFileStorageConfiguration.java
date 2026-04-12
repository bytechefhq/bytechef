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

package com.bytechef.ai.agent.eval.file.storage.config;

import com.bytechef.ai.agent.eval.file.storage.AiAgentEvalFileStorage;
import com.bytechef.ai.agent.eval.file.storage.AiAgentEvalFileStorageImpl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
class AiAgentEvalFileStorageConfiguration {

    @Bean
    AiAgentEvalFileStorage agentEvalFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        return new AiAgentEvalFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
