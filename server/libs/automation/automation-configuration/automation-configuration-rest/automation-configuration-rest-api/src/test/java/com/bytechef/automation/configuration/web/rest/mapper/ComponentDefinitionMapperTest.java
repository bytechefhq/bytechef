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

package com.bytechef.automation.configuration.web.rest.mapper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.configuration.web.rest.mapper.ComponentDefinitionMapper.ComponentDefinitionToComponentDefinitionBasicModelMapper;
import com.bytechef.automation.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.component.domain.ComponentDefinition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDefinitionMapperTest {

    private final ComponentDefinitionToComponentDefinitionBasicModelMapper componentDefinitionMapper =
        componentDefinition -> new ComponentDefinitionBasicModel();

    @Test
    void testAfterMappingCountsComponentInputs() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("slack").inputs(string("channel")));
        ComponentDefinitionBasicModel componentDefinitionBasicModel = new ComponentDefinitionBasicModel();

        componentDefinitionMapper.afterMapping(componentDefinition, componentDefinitionBasicModel);

        Assertions.assertThat(componentDefinitionBasicModel.getInputsCount())
            .isEqualTo(1);
    }

    @Test
    void testAfterMappingCountsNoComponentInputs() {
        ComponentDefinition componentDefinition = new ComponentDefinition(component("http"));
        ComponentDefinitionBasicModel componentDefinitionBasicModel = new ComponentDefinitionBasicModel();

        componentDefinitionMapper.afterMapping(componentDefinition, componentDefinitionBasicModel);

        Assertions.assertThat(componentDefinitionBasicModel.getInputsCount())
            .isZero();
    }
}
