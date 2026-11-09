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

package com.bytechef.component.ai.rag.modular.query.augmenter.cluster;

import static com.bytechef.component.ai.rag.modular.query.augmenter.cluster.ContextualQueryAugmenter.ALLOW_EMPTY_CONTEXT;
import static com.bytechef.component.ai.rag.modular.query.augmenter.cluster.ContextualQueryAugmenter.EMPTY_CONTEXT_PROMPT_TEMPLATE;
import static com.bytechef.component.ai.rag.modular.query.augmenter.cluster.ContextualQueryAugmenter.PROMPT_TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

/**
 * @author Ivica Cardic
 */
class ContextualQueryAugmenterTest {

    private static final Query QUERY = new Query("Where did Milton make landfall?");

    @Test
    void testDefaultsPassTheQueryOnWhenNothingWasRetrieved() {
        Query augmented = augment(Map.of(), List.of());

        assertThat(augmented.text()).isEqualTo(QUERY.text());
    }

    @Test
    void testDefaultsTreatTheContextAsSupplementary() {
        Query augmented = augment(Map.of(), List.of(new Document("Milton made landfall near Siesta Key.")));

        assertThat(augmented.text())
            .contains("Milton made landfall near Siesta Key.")
            .contains("Where did Milton make landfall?")
            .contains("your own general knowledge");
    }

    @Test
    void testEmptyContextIsRefusedWhenNotAllowed() {
        Query augmented = augment(Map.of(ALLOW_EMPTY_CONTEXT, false), List.of());

        assertThat(augmented.text())
            .isNotEqualTo(QUERY.text())
            .contains("outside your knowledge base");
    }

    @Test
    void testStrictPromptTemplateWhenEmptyContextIsNotAllowed() {
        Query augmented = augment(
            Map.of(ALLOW_EMPTY_CONTEXT, false), List.of(new Document("Milton made landfall near Siesta Key.")));

        assertThat(augmented.text())
            .contains("Milton made landfall near Siesta Key.")
            .contains("no prior knowledge")
            .contains("just say that you don't know")
            .doesNotContain("your own general knowledge");
    }

    @Test
    void testCustomPromptTemplateWhenEmptyContextIsNotAllowed() {
        Query augmented = augment(
            Map.of(ALLOW_EMPTY_CONTEXT, false, PROMPT_TEMPLATE, "Use only: {context}\nQ: {query}"),
            List.of(new Document("Milton made landfall near Siesta Key.")));

        assertThat(augmented.text())
            .isEqualTo("Use only: Milton made landfall near Siesta Key.\nQ: Where did Milton make landfall?");
    }

    @Test
    void testCustomEmptyContextPromptTemplate() {
        Query augmented = augment(
            Map.of(ALLOW_EMPTY_CONTEXT, false, EMPTY_CONTEXT_PROMPT_TEMPLATE, "Say that the document does not say."),
            List.of());

        assertThat(augmented.text()).isEqualTo("Say that the document does not say.");
    }

    @Test
    void testCustomPromptTemplate() {
        Query augmented = augment(
            Map.of(PROMPT_TEMPLATE, "Answer only from this: {context}\nQuestion: {query}"),
            List.of(new Document("Milton made landfall near Siesta Key.")));

        assertThat(augmented.text()).isEqualTo(
            "Answer only from this: Milton made landfall near Siesta Key.\nQuestion: Where did Milton make landfall?");
    }

    @Test
    void testBlankPromptTemplatesFallBackToTheDefaults() {
        Query augmented = augment(
            Map.of(PROMPT_TEMPLATE, " ", EMPTY_CONTEXT_PROMPT_TEMPLATE, " "),
            List.of(new Document("Milton made landfall near Siesta Key.")));

        assertThat(augmented.text()).contains("your own general knowledge");
    }

    private static Query augment(Map<String, Object> parameters, List<Document> documents) {
        Parameters inputParameters = MockParametersFactory.create(parameters);

        org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter contextualQueryAugmenter =
            ContextualQueryAugmenter.apply(inputParameters, MockParametersFactory.create(Map.of()));

        return contextualQueryAugmenter.augment(QUERY, documents);
    }
}
