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

package com.bytechef.platform.githubproxy.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.githubproxy.client.model.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplateAuthor;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplatePage;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplateSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkflowTemplatePageDeserializationTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .build();

    @Test
    void testDeserializesAPage() {
        String json = """
            {"content":[{"slug":"ai-email-classifier","title":"AI Email Classifier",
            "description":"Polls Gmail.","category":"ai","components":["googleMail","openAi"],
            "trigger":"googleMail","triggerLabel":"New email"}],
            "page":0,"size":20,"totalElements":1,"totalPages":1}
            """;

        WorkflowTemplatePage page = OBJECT_MAPPER.readValue(json, WorkflowTemplatePage.class);

        assertEquals(1, page.totalElements());
        assertEquals(0, page.page());

        WorkflowTemplateSummary summary = page.content()
            .getFirst();

        assertEquals("ai-email-classifier", summary.slug());
        assertEquals("ai", summary.category());
        assertEquals(List.of("googleMail", "openAi"), summary.components());
    }

    @Test
    void testDeserializesAFullTemplate() {
        String json = """
            {"slug":"loop","title":"Loop","description":"d","shortDescription":"s","category":"it",
            "components":["openAi"],"trigger":"","triggerLabel":"","keyFeatures":["f"],
            "prerequisites":["p"],"idealFor":"i","steps":["one"],"workflowDefinition":{"label":"L"},
            "author":{"name":"Jane","email":"j@x.io","role":"Eng","socialLinks":"https://x"},
            "lastModifiedDate":null}
            """;

        WorkflowTemplate template = OBJECT_MAPPER.readValue(json, WorkflowTemplate.class);

        assertEquals("loop", template.slug());

        WorkflowTemplateAuthor author = template.author();

        assertEquals("Jane", author.name());

        JsonNode workflowDefinitionJsonNode = template.workflowDefinition();

        JsonNode labelJsonNode = workflowDefinitionJsonNode.get("label");

        assertEquals("L", labelJsonNode.asString());
    }
}
