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

package com.bytechef.platform.ai.tool;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition.OptionsFunction;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ToolOptionsResolverTest {

    private static final OptionsFunction<String> OPTIONS_FUNCTION =
        (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> List.of();

    private final AtomicLong currentTimeMillis = new AtomicLong(1_000);

    @Mock
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    private ToolOptionsResolver toolOptionsResolver;

    @BeforeEach
    void beforeEach() {
        toolOptionsResolver = new ToolOptionsResolver(clusterElementDefinitionFacade, currentTimeMillis::get);
    }

    @Test
    void testResolveOptionsLoadsTheOptionsFromTheGivenConnection() {
        mockOptions(List.of(option("General", "general"), option("Random", "random")));

        List<Object> options = toolOptionsResolver.resolveOptions(
            "slack", 1, "sendMessage", channelProperty(), Map.of("text", "=fromAi('text')"), 7L);

        assertThat(options).containsExactly("general", "random");

        verify(clusterElementDefinitionFacade).executeOptions(
            eq("slack"), eq(1), eq("sendMessage"), eq("channel"), eq(Map.of()), eq(Map.of()), eq(List.of()),
            isNull(), eq(7L), eq(Map.of()), eq(Map.of()));
    }

    @Test
    void testResolveOptionsCachesTheOptionsUntilTheyExpire() {
        mockOptions(List.of(option("General", "general")));

        resolveChannelOptions(7L);
        resolveChannelOptions(7L);

        verify(clusterElementDefinitionFacade, times(1)).executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap());

        currentTimeMillis.addAndGet(ToolOptionsResolver.CACHE_TTL_MILLIS);

        resolveChannelOptions(7L);

        verify(clusterElementDefinitionFacade, times(2)).executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap());
    }

    @Test
    void testResolveOptionsCachesEachConnectionSeparately() {
        mockOptions(List.of(option("General", "general")));

        resolveChannelOptions(7L);
        resolveChannelOptions(8L);

        verify(clusterElementDefinitionFacade, times(2)).executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap());
    }

    @Test
    void testResolveOptionsReturnsNullWhenTheOptionsCannotBeLoaded() {
        when(clusterElementDefinitionFacade.executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap()))
                .thenThrow(new IllegalStateException("connection expired"));

        assertThat(resolveChannelOptions(7L)).isNull();
    }

    @Test
    void testResolveOptionsReturnsNullWhenThereAreNoOptions() {
        mockOptions(List.of());

        assertThat(resolveChannelOptions(7L)).isNull();
    }

    @Test
    void testResolveOptionsSkipsALookupWhoseDependencyIsEmpty() {
        Property property = Property.toProperty(
            string("channel").options(OPTIONS_FUNCTION)
                .optionsLookupDependsOn("workspace"));

        assertThat(
            toolOptionsResolver.resolveOptions(
                "slack", 1, "sendMessage", property, Map.of("workspace", "=fromAi('workspace')"), 7L))
                    .isNull();

        verify(clusterElementDefinitionFacade, never()).executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap());
    }

    @Test
    void testResolveOptionsPassesAFixedDependencyValue() {
        mockOptions(List.of(option("General", "general")));

        Property property = Property.toProperty(
            string("channel").options(OPTIONS_FUNCTION)
                .optionsLookupDependsOn("workspace"));

        toolOptionsResolver.resolveOptions("slack", 1, "sendMessage", property, Map.of("workspace", "acme"), 7L);

        verify(clusterElementDefinitionFacade).executeOptions(
            eq("slack"), eq(1), eq("sendMessage"), eq("channel"), eq(Map.of("workspace", "acme")), eq(Map.of()),
            eq(List.of("workspace")), isNull(), eq(7L), eq(Map.of()), eq(Map.of()));
    }

    @Test
    void testHasRuntimeOptionsOnlyForScalarPropertiesWithAnOptionsFunction() {
        assertThat(ToolOptionsResolver.hasRuntimeOptions(channelProperty())).isTrue();
        assertThat(
            ToolOptionsResolver.hasRuntimeOptions(
                Property.toProperty(string("priority").options(option("High", "high")))))
                    .isFalse();
        assertThat(
            ToolOptionsResolver.hasRuntimeOptions(
                Property.toProperty(array("channels").options(OPTIONS_FUNCTION))))
                    .isFalse();
    }

    private static Property channelProperty() {
        return Property.toProperty(string("channel").options(OPTIONS_FUNCTION));
    }

    private void mockOptions(List<com.bytechef.component.definition.Option<String>> options) {
        when(clusterElementDefinitionFacade.executeOptions(
            anyString(), eq(1), anyString(), anyString(), anyMap(), anyMap(), anyList(), any(), any(), anyMap(),
            anyMap()))
                .thenReturn(options.stream()
                    .map(Option::new)
                    .toList());
    }

    private List<Object> resolveChannelOptions(Long connectionId) {
        return toolOptionsResolver.resolveOptions("slack", 1, "sendMessage", channelProperty(), Map.of(), connectionId);
    }
}
