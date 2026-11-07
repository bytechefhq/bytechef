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

package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceVoiceAgentFilterTest {

    private static final ComponentHandler DEEPGRAM_HANDLER = () -> component("deepgram")
        .title("Deepgram")
        .clusterElements(voiceAgentElement("Deepgram Voice Agent"));

    private static final ComponentHandler ELEVEN_LABS_HANDLER = () -> component("elevenLabs")
        .title("ElevenLabs")
        .clusterElements(voiceAgentElement("ElevenLabs Voice Agent"));

    private static final ComponentHandler OPEN_AI_HANDLER = () -> component("openAi")
        .title("OpenAI")
        .clusterElements(voiceAgentElement("OpenAI Voice Agent"));

    private static final ComponentHandler TOOLS_ONLY_HANDLER = () -> component("toolsOnlyComponent")
        .title("Tools Only")
        .clusterElements(
            clusterElement("someTool")
                .type(TOOLS)
                .title("Some Tool"));

    private static final ComponentHandler PLAIN_HANDLER = () -> component("plainComponent")
        .title("Plain")
        .actions(action("get").title("Get"));

    @Test
    public void testGetComponentDefinitionsListsVoiceAgentProvidersForTheTriggersSlot() {
        ComponentDefinitionService componentDefinitionService = createComponentDefinitionService();

        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions(
            null, true, null, null, null, PlatformType.AUTOMATION);

        List<String> voiceAgentProviderNames = componentDefinitions.stream()
            .filter(componentDefinition -> componentDefinition.getClusterElements()
                .stream()
                .anyMatch(
                    clusterElementDefinition -> clusterElementDefinition.getType() == VoiceAgentFunction.VOICE_AGENT))
            .map(ComponentDefinition::getName)
            .toList();

        Assertions.assertThat(voiceAgentProviderNames)
            .containsExactlyInAnyOrder("deepgram", "elevenLabs", "openAi");
    }

    private static com.bytechef.component.definition.ClusterElementDefinition<VoiceAgentFunction> voiceAgentElement(
        String title) {

        return com.bytechef.component.definition.ComponentDsl.<VoiceAgentFunction>clusterElement("voiceAgent")
            .type(VoiceAgentFunction.VOICE_AGENT)
            .title(title);
    }

    private static ComponentDefinitionService createComponentDefinitionService() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
            applicationProperties,
            List.of(DEEPGRAM_HANDLER, ELEVEN_LABS_HANDLER, OPEN_AI_HANDLER, TOOLS_ONLY_HANDLER, PLAIN_HANDLER),
            List::of, List.of());

        return new ComponentDefinitionServiceImpl(
            List.of(new AllComponentDefinitionFilter()), componentDefinitionRegistry,
            Mockito.mock(ContextFactory.class));
    }

    private static class AllComponentDefinitionFilter implements ComponentDefinitionFilter {

        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return true;
        }

        @Override
        public boolean supports(PlatformType type) {
            return true;
        }
    }
}
