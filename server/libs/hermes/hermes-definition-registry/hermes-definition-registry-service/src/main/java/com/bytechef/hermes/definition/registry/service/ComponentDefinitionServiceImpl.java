
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.AuthorizationDTO;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionBasicDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionServiceImpl implements ComponentDefinitionService {

    private final List<ComponentDefinition> componentDefinitions;

    @SuppressFBWarnings("EI2")
    public ComponentDefinitionServiceImpl(List<ComponentDefinition> componentDefinitions) {
        this.componentDefinitions = componentDefinitions;
    }

    @Override
    public Mono<ComponentDefinitionDTO> getComponentDefinitionMono(String name, Integer version) {
        return Mono.just(
            toComponentDefinitionDTO(
                CollectionUtils.getFirst(
                    componentDefinitions,
                    componentDefinition -> name.equalsIgnoreCase(componentDefinition.getName()) &&
                        version == componentDefinition.getVersion())));
    }

    @Override
    public Mono<List<ComponentDefinitionDTO>> getComponentDefinitionsMono() {
        return Mono.just(CollectionUtils.map(componentDefinitions, this::toComponentDefinitionDTO));
    }

    @Override
    public Mono<List<ComponentDefinitionDTO>> getComponentDefinitionsMono(String name) {
        return Mono.just(
            CollectionUtils.map(
                CollectionUtils.filter(
                    componentDefinitions,
                    componentDefinition -> Objects.equals(componentDefinition.getName(), name)),
                this::toComponentDefinitionDTO));
    }

    private ComponentDefinitionDTO toComponentDefinitionDTO(ComponentDefinition componentDefinition) {
        return new ComponentDefinitionDTO(
            CollectionUtils.map(componentDefinition.getActions(), this::toActionDefinitionBasicDTO),
            toConnectionDefinitionDTO(componentDefinition.getConnection()),
            componentDefinition.getDisplay(), componentDefinition.getName(), componentDefinition.getResources(),
            CollectionUtils.map(componentDefinition.getTriggers(), this::toTriggerDefinitionBasicDTO),
            componentDefinition.getVersion());
    }

    private ActionDefinitionBasicDTO toActionDefinitionBasicDTO(ActionDefinition actionDefinition) {
        return new ActionDefinitionBasicDTO(
            actionDefinition.getBatch(), actionDefinition.getDisplay(), actionDefinition.getName(),
            actionDefinition.getResources());
    }

    private List<AuthorizationDTO> toAuthorizationDTOs(List<? extends Authorization> authorizations) {
        return CollectionUtils.map(
            authorizations,
            authorization -> new AuthorizationDTO(
                authorization.getDisplay(), authorization.getName(), authorization.getProperties(),
                authorization.getType()));
    }

    private ConnectionDefinitionBasicDTO toConnectionDefinitionDTO(ConnectionDefinition connectionDefinition) {
        return new ConnectionDefinitionBasicDTO(
            connectionDefinition.getDisplay(), connectionDefinition.getName(), connectionDefinition.getResources());
    }

    private TriggerDefinitionBasicDTO toTriggerDefinitionBasicDTO(TriggerDefinition triggerDefinition) {
        return new TriggerDefinitionBasicDTO(
            triggerDefinition.getBatch(), triggerDefinition.getDisplay(), triggerDefinition.getName(),
            triggerDefinition.getResources(), triggerDefinition.getType());
    }
}
