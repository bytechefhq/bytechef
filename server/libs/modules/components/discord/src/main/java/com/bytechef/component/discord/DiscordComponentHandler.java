/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.discord.constant.DiscordConstants.BASE_URL;
import static com.bytechef.component.discord.constant.DiscordConstants.CHANNEL_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID_PROPERTY;
import static com.bytechef.component.discord.constant.DiscordConstants.RECIPIENT_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property;
import com.bytechef.component.discord.action.DiscordSendDirectMessageAction;
import com.bytechef.component.discord.util.DiscordUtils;
import com.bytechef.definition.BaseProperty;
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
    public List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of(DiscordSendDirectMessageAction.ACTION_DEFINITION);
    }

    @Override
    public List<? extends ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {

        for (ModifiableActionDefinition modifiableActionDefinition : actionDefinitions) {
            if (Objects.equals(modifiableActionDefinition.getName(), "sendChannelMessage")) {
                Optional<List<? extends Property>> propertiesOptional = modifiableActionDefinition.getProperties();

                List<Property> properties = new ArrayList<>(propertiesOptional.get());

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
            .icon("path:assets/discord.svg");
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(
                    AuthorizationType.BEARER_TOKEN.toLowerCase(), AuthorizationType.BEARER_TOKEN)
                        .title("Bearer Token")
                        .properties(
                            string(TOKEN)
                                .label("Bot token")
                                .required(true))
                        .apply((connectionParameters, context) -> ofHeaders(
                            Map.of(AUTHORIZATION, List.of("Bot " + connectionParameters.getRequiredString(TOKEN))))))
            .baseUri((connectionParameters, context) -> BASE_URL);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), GUILD_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) DiscordUtils::getGuildIdOptions);
        } else if (Objects.equals(modifiableProperty.getName(), CHANNEL_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) DiscordUtils::getChannelIdOptions);
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends Property.ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "type")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .options(
                            option("GUILD_TEXT", 0, "a text channel within a server"),
                            option("GUILD_VOICE", 2, "a voice channel within a server"),
                            option("GUILD_CATEGORY", 4, "an organizational category that contains up to 50 channels"));
                } else if (Objects.equals(baseProperty.getName(), RECIPIENT_ID)) {
                    ((ModifiableStringProperty) baseProperty)
                        .options((ActionOptionsFunction<String>) DiscordUtils::getGuildMemberIdOptions);
                }
            }
        }

        return modifiableProperty;
    }

}
