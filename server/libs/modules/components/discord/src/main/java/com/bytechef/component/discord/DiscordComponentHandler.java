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

package com.bytechef.component.discord;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.discord.constant.DiscordConstants.CONTENT;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID_PROPERTY;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.discord.action.DiscordSendDirectMessageAction;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Domiter
 */

@AutoService(OpenApiComponentHandler.class)
public class DiscordComponentHandler extends AbstractDiscordComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
        return List.of(DiscordSendDirectMessageAction.ACTION_DEFINITION);
    }

    @Override
    public List<ModifiableClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(tool(DiscordSendDirectMessageAction.ACTION_DEFINITION));
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {

        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            if (Objects.equals(modifiableActionDefinition.getName(), "sendChannelMessage")) {
                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                List<Property> properties = new ArrayList<>(propertiesOptional.orElse(List.of()));

                properties.addFirst(GUILD_ID_PROPERTY);

                modifiableActionDefinition.properties(properties);
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/discord.svg")
            .categories(ComponentCategory.COMMUNICATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        Optional<List<? extends Authorization>> optionalAuthorizations =
            modifiableConnectionDefinition.getAuthorizations();

        if (optionalAuthorizations.isPresent()) {
            List<? extends Authorization> authorizations = optionalAuthorizations.get();
            ModifiableAuthorization modifiableAuthorization = (ModifiableAuthorization) authorizations.getFirst();

            modifiableAuthorization.apply((connectionParameters, context) -> ofHeaders(
                Map.of(AUTHORIZATION, List.of("Bot " + connectionParameters.getRequiredString(TOKEN)))));
        }

        return modifiableConnectionDefinition;
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "type")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options(
                    option("GUILD_TEXT", 0, "a text channel within a server"),
                    option("GUILD_VOICE", 2, "a voice channel within a server"),
                    option("GUILD_CATEGORY", 4, "an organizational category that contains up to 50 channels"));
        } else if (Objects.equals(modifiableProperty.getName(), CONTENT)) {
            ((ModifiableStringProperty) modifiableProperty)
                .controlType(Property.ControlType.TEXT_AREA);
        }

        return modifiableProperty;
    }

}
