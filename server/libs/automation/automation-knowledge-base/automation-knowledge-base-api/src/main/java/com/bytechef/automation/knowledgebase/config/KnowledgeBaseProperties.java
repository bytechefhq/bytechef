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

package com.bytechef.automation.knowledgebase.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.automation.knowledge-base")
@SuppressFBWarnings("EI")
public class KnowledgeBaseProperties {

    private final Embedding embedding = new Embedding();
    private final Ocr ocr = new Ocr();

    public Embedding getEmbedding() {
        return embedding;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public static class Embedding {
        private String model = "text-embedding-3-small";
        private Provider provider = Provider.OPENAI;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public enum Provider {
            OPENAI, OLLAMA, AZURE
        }
    }

    public static class Ocr {
        private Provider provider = Provider.NONE;
        private Mistral mistral = new Mistral();

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public Mistral getMistral() {
            return mistral;
        }

        public void setMistral(Mistral mistral) {
            this.mistral = mistral;
        }

        public enum Provider {
            NONE, AZURE, MISTRAL
        }

        public static class Mistral {
            private String apiKey;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }
        }
    }
}
