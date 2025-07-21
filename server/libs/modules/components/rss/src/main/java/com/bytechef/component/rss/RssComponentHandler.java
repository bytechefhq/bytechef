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

package com.bytechef.component.rss;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.rss.constant.RssConstants.API_KEY;
import static com.bytechef.component.rss.constant.RssConstants.API_SECRET;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class RssComponentHandler extends AbstractRssComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/rss.svg")
            .categories(ComponentCategory.SOCIAL_MEDIA)
            .customAction(true);
    }

    @Override
    public ModifiableConnectionDefinition
        modifyConnection(ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://api.rss.app/v1")
            .authorizations(
                authorization(AuthorizationType.BEARER_TOKEN)
                    .properties(
                        string(API_KEY)
                            .label("API Key")
                            .description("Your API key can be found in Account Settings -> API.")
                            .required(true),
                        string(API_SECRET)
                            .label("API Secret")
                            .description("Your API secret can be found in Account Settings -> API.")
                            .required(true))
                    .apply((connectionParameters, context) -> {
                        String apiKey = connectionParameters.getRequiredString(API_KEY);
                        String apiSecret = connectionParameters.getRequiredString(API_SECRET);

                        return ApplyResponse.ofHeaders(
                            Map.of("Authorization", List.of("Bearer " + apiKey + ":" + apiSecret)));
                    }));
    }
}
