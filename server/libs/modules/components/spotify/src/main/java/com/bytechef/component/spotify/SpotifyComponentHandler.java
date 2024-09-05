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

package com.bytechef.component.spotify;

import com.bytechef.component.OpenAPIComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction;
import com.bytechef.component.spotify.util.SpotifyUtils;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
@AutoService(OpenAPIComponentHandler.class)
public class SpotifyComponentHandler extends AbstractSpotifyComponentHandler {

    @Override
    public List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of(SpotifyCreatePlaylistAction.ACTION_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/spotify.svg");
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "playlist_id")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) SpotifyUtils::getPlaylistOptions);
        }

        return modifiableProperty;
    }
}
