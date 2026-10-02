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

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Option;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PiiEntityOptionsTest {

    @Test
    void testPickerOptionsMatchCatalogTypesExactly() {
        List<String> optionValues = PiiEntityOptions.getPiiDetectionOptions()
            .stream()
            .map(Option::getValue)
            .toList();
        List<String> catalogTypes = PiiPatternCatalog.ALL.stream()
            .map(PiiPatternCatalog.PiiPattern::type)
            .toList();

        assertThat(optionValues).containsExactlyInAnyOrderElementsOf(catalogTypes);
    }

    @Test
    void testAllSelectsTheWholeCatalogIncludingTheContextualTypes() {
        assertThat(PiiEntityOptions.selectedPatterns(ParametersFactory.create(Map.of("type", "ALL"))))
            .containsExactlyElementsOf(PiiPatternCatalog.ALL);
    }

    @Test
    void testSelectedReturnsOnlyTheSelectedPatterns() {
        assertThat(
            PiiEntityOptions.selectedPatterns(
                ParametersFactory.create(
                    Map.of("type", "SELECTED", "entities", List.of("EMAIL_ADDRESS", "PHONE_NUMBER")))))
                        .extracting(PiiPatternCatalog.PiiPattern::type)
                        .containsExactlyInAnyOrder("EMAIL_ADDRESS", "PHONE_NUMBER");
    }

    @Test
    void testSelectedIgnoresUnknownTypesAlongsideKnownOnes() {
        assertThat(
            PiiEntityOptions.selectedPatterns(
                ParametersFactory.create(Map.of("type", "SELECTED", "entities", List.of("EMAIL_ADDRESS", "BOGUS")))))
                    .extracting(PiiPatternCatalog.PiiPattern::type)
                    .containsExactly("EMAIL_ADDRESS");
    }

    @Test
    void testSelectedRequiresAtLeastOneEntity() {
        assertThatThrownBy(
            () -> PiiEntityOptions.selectedPatterns(
                ParametersFactory.create(Map.of("type", "SELECTED", "entities", List.of()))))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSelectedWithOnlyUnknownEntitiesFailsClosed() {
        assertThatThrownBy(
            () -> PiiEntityOptions.selectedPatterns(
                ParametersFactory.create(Map.of("type", "SELECTED", "entities", List.of("DOES_NOT_EXIST")))))
                    .isInstanceOf(IllegalArgumentException.class);
    }
}
