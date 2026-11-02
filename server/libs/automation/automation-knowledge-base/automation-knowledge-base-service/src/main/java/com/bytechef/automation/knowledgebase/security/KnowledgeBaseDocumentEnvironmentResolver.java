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

package com.bytechef.automation.knowledgebase.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment of a knowledge base document through its knowledge base, for the
 * {@code 'KnowledgeBaseDocument'} token.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentEnvironmentResolver implements ResourceEnvironmentResolver {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseEnvironmentResolver knowledgeBaseEnvironmentResolver;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentEnvironmentResolver(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseEnvironmentResolver knowledgeBaseEnvironmentResolver) {

        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseEnvironmentResolver = knowledgeBaseEnvironmentResolver;
    }

    @Override
    public String resourceType() {
        return "KnowledgeBaseDocument";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(number.longValue())
            .map(KnowledgeBaseDocument::getKnowledgeBaseId)
            .flatMap(knowledgeBaseEnvironmentResolver::fetchEnvironment);
    }
}
