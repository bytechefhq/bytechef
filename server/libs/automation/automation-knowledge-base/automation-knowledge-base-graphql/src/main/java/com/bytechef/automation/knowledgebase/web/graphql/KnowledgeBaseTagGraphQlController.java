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

import com.bytechef.automation.knowledgebase.service.KnowledgeBaseTagService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class KnowledgeBaseTagGraphQlController {

    private final KnowledgeBaseTagService knowledgeBaseTagService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseTagGraphQlController(KnowledgeBaseTagService knowledgeBaseTagService) {
        this.knowledgeBaseTagService = knowledgeBaseTagService;
    }

    @QueryMapping
    public List<Tag> knowledgeBaseTags() {
        return knowledgeBaseTagService.getAllTags();
    }

    @QueryMapping
    public List<KnowledgeBaseTagsEntry> knowledgeBaseTagsByKnowledgeBase() {
        return knowledgeBaseTagService.getTagsByKnowledgeBaseId()
            .entrySet()
            .stream()
            .map(entry -> new KnowledgeBaseTagsEntry(entry.getKey(), entry.getValue()))
            .toList();
    }

    @MutationMapping
    public boolean updateKnowledgeBaseTags(@Argument UpdateKnowledgeBaseTagsInput input) {
        List<Tag> tags = input.tags() == null ? List.of() : input.tags()
            .stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                if (tagInput.id() != null) {
                    tag.setId(tagInput.id());
                }

                tag.setName(tagInput.name());

                return tag;
            })
            .collect(Collectors.toList());

        knowledgeBaseTagService.updateTags(input.knowledgeBaseId(), tags);

        return true;
    }

    public record KnowledgeBaseTagsEntry(Long knowledgeBaseId, List<Tag> tags) {
    }

    public record UpdateKnowledgeBaseTagsInput(Long knowledgeBaseId, List<TagInput> tags) {
    }

    public record TagInput(Long id, String name) {
    }
}
