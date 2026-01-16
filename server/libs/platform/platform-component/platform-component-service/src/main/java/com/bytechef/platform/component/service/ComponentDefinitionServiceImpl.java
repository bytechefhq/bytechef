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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("componentDefinitionService")
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final List<ComponentDefinitionFilter> componentDefinitionFilters;
    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(
        List<ComponentDefinitionFilter> componentDefinitionFilters,
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry) {

        this.componentDefinitionFilters = componentDefinitionFilters;
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public Optional<ComponentDefinition> fetchComponentDefinition(String name, @Nullable Integer version) {
        return componentDefinitionRegistry.fetchComponentDefinition(name, version)
            .map(ComponentDefinition::new);
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, @Nullable Integer version) {
        com.bytechef.component.definition.ComponentDefinition componentDefinition =
            componentDefinitionRegistry.getComponentDefinition(name, version);

        return new ComponentDefinition(componentDefinition);
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions,
        @Nullable List<String> include, PlatformType platformType) {

        ComponentDefinitionFilter componentDefinitionFilter = componentDefinitionFilters.stream()
            .filter(curComponentDefinitionFilter -> curComponentDefinitionFilter.supports(platformType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported mode type: " + platformType));

        List<ComponentDefinition> components = getComponentDefinitions()
            .stream()
            .filter(componentDefinitionFilter::filter)
            .filter(filter(actionDefinitions, connectionDefinitions, triggerDefinitions, include))
            .distinct()
            .toList();

        if (include != null && !include.isEmpty()) {
            components = new ArrayList<>(components);

            components.sort(Comparator.comparing(component -> include.indexOf(component.getName())));
        }

        return components;
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions(String query, PlatformType platformType) {
        String lowerCaseQuery = query.toLowerCase();

        ComponentDefinitionFilter componentDefinitionFilter = componentDefinitionFilters.stream()
            .filter(curComponentDefinitionFilter -> curComponentDefinitionFilter.supports(platformType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported mode type: " + platformType));

        return getComponentDefinitions()
            .stream()
            .filter(componentDefinitionFilter::filter)
            .filter(componentDefinition -> hasMatchingComponent(componentDefinition, lowerCaseQuery) ||
                hasMatchingAction(componentDefinition.getActions(), lowerCaseQuery) ||
                hasMatchingTrigger(componentDefinition.getTriggers(), lowerCaseQuery))
            .toList();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitionVersions(String name) {
        return componentDefinitionRegistry.getComponentDefinitions(name)
            .stream()
            .map(ComponentDefinition::new)
            .toList();
    }

    @Override
    public ComponentDefinition getConnectionComponentDefinition(String name, int connectionVersion) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> {
                if (name.equals(componentDefinition.getName())) {
                    Optional<ConnectionDefinition> connectionDefinitionOptional = componentDefinition.getConnection();

                    if (connectionDefinitionOptional.isEmpty()) {
                        return false;
                    }

                    ConnectionDefinition connectionDefinition = connectionDefinitionOptional.get();

                    return connectionDefinition.getVersion() == connectionVersion;
                }

                return false;
            })
            .findFirst()
            .map(ComponentDefinition::new)
            .orElseThrow(() -> new IllegalArgumentException(
                "Connection component definition with name: %s, connectionVersion: %d not found".formatted(
                    name, connectionVersion)));
    }

    @Override
    public boolean hasComponentDefinition(String name, @Nullable Integer version) {
        return componentDefinitionRegistry.hasComponentDefinition(name, version);
    }

    private static Predicate<ComponentDefinition> filter(
        @Nullable Boolean actionDefinitions, @Nullable Boolean connectionDefinitions,
        @Nullable Boolean triggerDefinitions, @Nullable List<String> include) {

        return componentDefinition -> {
            if (include == null || include.contains(componentDefinition.getName())) {
                if (actionDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getActions())) {
                    return true;
                }

                if (connectionDefinitions != null && componentDefinition.getConnection() != null) {
                    return true;
                }

                if (triggerDefinitions != null && !CollectionUtils.isEmpty(componentDefinition.getTriggers())) {
                    return true;
                }
            }

            return include == null && actionDefinitions == null && connectionDefinitions == null &&
                triggerDefinitions == null;
        };
    }

    private static boolean hasMatchingAction(List<ActionDefinition> actions, String lowerCaseQuery) {
        return actions.stream()
            .anyMatch(action -> Strings.CS.contains(StringUtils.lowerCase(action.getName()), lowerCaseQuery) ||
                (action.getTitle() != null &&
                    Strings.CS.contains(StringUtils.lowerCase(action.getTitle()), lowerCaseQuery)));
    }

    private static boolean hasMatchingComponent(ComponentDefinition componentDefinition, String lowerCaseQuery) {
        return Strings.CS.contains(
            StringUtils.lowerCase(componentDefinition.getName()), lowerCaseQuery) ||
            (componentDefinition.getTitle() != null &&
                Strings.CS.contains(
                    StringUtils.lowerCase(componentDefinition.getTitle()), lowerCaseQuery));
    }

    private static boolean hasMatchingTrigger(List<TriggerDefinition> triggers, String lowerCaseQuery) {
        return triggers.stream()
            .anyMatch(trigger -> Strings.CS.contains(
                StringUtils.lowerCase(trigger.getName()), lowerCaseQuery) ||
                (trigger.getTitle() != null &&
                    Strings.CS.contains(StringUtils.lowerCase(trigger.getTitle()), lowerCaseQuery)));
    }
}
