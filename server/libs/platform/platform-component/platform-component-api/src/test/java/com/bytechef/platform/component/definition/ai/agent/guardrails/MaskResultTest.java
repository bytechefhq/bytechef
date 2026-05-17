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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class MaskResultTest {

    @Test
    void unchangedFactoryReturnsTheSharedSingleton() {
        assertThat(MaskResult.unchanged())
            .as("unchanged() must return the shared UNCHANGED instance so identity-equality works")
            .isSameAs(MaskResult.UNCHANGED);
    }

    @Test
    void entitiesFactoryReturnsUnchangedWhenInputIsNullOrEmpty() {
        assertThat(MaskResult.entities(null)).isSameAs(MaskResult.UNCHANGED);
        assertThat(MaskResult.entities(Map.of())).isSameAs(MaskResult.UNCHANGED);
    }

    @Test
    void entitiesFactoryDropsNullAndEmptyValuesAndReturnsUnchangedIfAllAreDropped() {
        Map<String, List<String>> input = new HashMap<>();

        input.put("EMAIL", List.of());
        input.put("PHONE", null);
        input.put(null, List.of("ignored"));

        assertThat(MaskResult.entities(input))
            .as("an entities map with no surviving values must collapse to UNCHANGED")
            .isSameAs(MaskResult.UNCHANGED);
    }

    @Test
    void entitiesFactoryStripsNullValueElementsWhilePreservingTheRest() {
        Map<String, List<String>> input = new HashMap<>();

        List<String> mixed = new ArrayList<>();

        mixed.add("a@b.com");
        mixed.add(null);
        mixed.add("");
        mixed.add("c@d.com");

        input.put("EMAIL", mixed);

        MaskResult result = MaskResult.entities(input);

        assertThat(result).isInstanceOf(MaskResult.Entities.class);

        Map<String, List<String>> entities = ((MaskResult.Entities) result).entities();

        assertThat(entities)
            .containsOnlyKeys("EMAIL")
            .extractingByKey("EMAIL", org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
            .containsExactly("a@b.com", "c@d.com");
    }

    @Test
    void entitiesRecordRejectsNullMap() {
        assertThatThrownBy(() -> new MaskResult.Entities(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("entities");
    }

    @Test
    void entitiesRecordRejectsNullTypeKey() {
        Map<String, List<String>> input = new HashMap<>();

        input.put(null, List.of("v"));

        assertThatThrownBy(() -> new MaskResult.Entities(input))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("type");
    }

    @Test
    void entitiesRecordRejectsNullValuesList() {
        Map<String, List<String>> input = new HashMap<>();

        input.put("EMAIL", null);

        assertThatThrownBy(() -> new MaskResult.Entities(input))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("values");
    }

    @Test
    void entitiesRecordCopiesTheOuterMapAndInnerListsSoLaterMutationsDoNotLeak() {
        Map<String, List<String>> outer = new HashMap<>();

        List<String> inner = new ArrayList<>();

        inner.add("a@b.com");
        outer.put("EMAIL", inner);

        MaskResult.Entities result = new MaskResult.Entities(outer);

        outer.put("PHONE", List.of("555-1212"));
        inner.add("c@d.com");

        assertThat(result.entities())
            .as("outer mutation must not affect the snapshot")
            .containsOnlyKeys("EMAIL");
        assertThat(result.entities()
            .get("EMAIL"))
                .as("inner mutation must not affect the snapshot")
                .containsExactly("a@b.com");
    }

    @Test
    void entitiesRecordEntitiesMapIsImmutable() {
        MaskResult.Entities result = new MaskResult.Entities(Map.of("EMAIL", List.of("a@b.com")));

        assertThatThrownBy(() -> result.entities()
            .put("PHONE", List.of("555")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.entities()
            .get("EMAIL")
            .add("mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void maskedFactoryReturnsUnchangedWhenTextEqualsOriginal() {
        assertThat(MaskResult.masked("hello", "hello")).isSameAs(MaskResult.UNCHANGED);
    }

    @Test
    void maskedFactoryReturnsMaskedRecordWhenTextDiffersFromOriginal() {
        MaskResult result = MaskResult.masked("h<R>llo", "hello");

        assertThat(result).isInstanceOf(MaskResult.Masked.class);
        assertThat(((MaskResult.Masked) result).text()).isEqualTo("h<R>llo");
    }

    @Test
    void maskedFactoryRejectsNullText() {
        assertThatThrownBy(() -> MaskResult.masked(null, "anything"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("text");
    }

    @Test
    void maskedFactoryRejectsNullOriginalText() {
        assertThatThrownBy(() -> MaskResult.masked("anything", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("originalText");
    }

    @Test
    void maskedRecordRejectsNullText() {
        assertThatThrownBy(() -> new MaskResult.Masked(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("text");
    }

    @Test
    void sealedHierarchyIsExhaustivelyDispatchable() {
        // Regression pin: if a future maintainer adds a fourth permit, this switch will not compile,
        // forcing the advisor's matching switch to be updated in lockstep.
        for (MaskResult result : List.of(
            MaskResult.UNCHANGED,
            new MaskResult.Masked("masked"),
            new MaskResult.Entities(Map.of("EMAIL", List.of("a@b.com"))))) {

            String label = switch (result) {
                case MaskResult.Unchanged ignored -> "unchanged";
                case MaskResult.Masked ignored -> "masked";
                case MaskResult.Entities ignored -> "entities";
            };

            assertThat(label).isNotBlank();
        }
    }
}
