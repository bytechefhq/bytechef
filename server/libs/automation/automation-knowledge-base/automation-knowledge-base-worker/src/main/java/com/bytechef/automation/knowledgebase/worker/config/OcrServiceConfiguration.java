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

package com.bytechef.automation.knowledgebase.worker.config;

import com.bytechef.automation.knowledgebase.worker.document.ocr.MistralOcrService;
import com.bytechef.automation.knowledgebase.worker.document.ocr.NoOpOcrService;
import com.bytechef.automation.knowledgebase.worker.document.ocr.OcrService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.KnowledgeBase.Ocr;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OCR services based on the configured provider.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class OcrServiceConfiguration {

    @Bean
    OcrService ocrService(ApplicationProperties applicationProperties) {
        Ocr ocrProperties = applicationProperties.getKnowledgeBase()
            .getOcr();

        Ocr.Provider provider = ocrProperties.getProvider();

        if (provider == Ocr.Provider.MISTRAL) {
            String apiKey = ocrProperties.getMistral()
                .getApiKey();

            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                    "Mistral OCR is enabled but API key is not configured. " +
                        "Set bytechef.knowledge-base.ocr.mistral.api-key");
            }

            return new MistralOcrService(apiKey);
        }

        return new NoOpOcrService();
    }
}
