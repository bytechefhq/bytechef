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

package com.bytechef.automation.knowledgebase.web.graphql;

import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentTagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class KnowledgeBaseDocumentTagGraphQlController {

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentTagGraphQlController(
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentTagService = knowledgeBaseDocumentTagService;
    }

    @QueryMapping
    public List<String> knowledgeBaseDocumentTags() {
        return knowledgeBaseDocumentTagService.getAllTagNames();
    }

    @QueryMapping
    public List<KnowledgeBaseDocumentTagsEntry> knowledgeBaseDocumentTagsByDocument() {
        return knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseDocumentId()
            .entrySet()
            .stream()
            .map(entry -> new KnowledgeBaseDocumentTagsEntry(entry.getKey(), entry.getValue()))
            .toList();
    }

    @MutationMapping
    public boolean updateKnowledgeBaseDocumentTags(@Argument UpdateKnowledgeBaseDocumentTagsInput input) {
        List<String> tagNames = input.tags() == null ? List.of() : input.tags();

        knowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentTags(input.knowledgeBaseDocumentId(), tagNames);

        return true;
    }

    public record KnowledgeBaseDocumentTagsEntry(Long knowledgeBaseDocumentId, List<String> tags) {
    }

    public record UpdateKnowledgeBaseDocumentTagsInput(Long knowledgeBaseDocumentId, List<String> tags) {
    }
}
