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

package com.bytechef.ai.copilot.config;

import com.bytechef.ai.copilot.advisor.EnvironmentAwareQuestionAnswerAdvisor;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotConfiguration {

    @Bean
    Advisor questionAnswerAdvisor(
        VectorStore copilotPgVectorStore, EmbeddingProviderStatusProvider embeddingProviderStatusProvider) {

        return new EnvironmentAwareQuestionAnswerAdvisor(
            copilotPgVectorStore, embeddingProviderStatusProvider, CopilotConstants.STATE_ENVIRONMENT_ID);
    }
}
