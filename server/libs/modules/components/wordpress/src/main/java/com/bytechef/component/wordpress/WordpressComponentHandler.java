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

package com.bytechef.component.wordpress;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.wordpress.constant.WordpressConstants.WEBSITE;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.wordpress.trigger.WordPressNewPostTrigger;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class WordpressComponentHandler extends AbstractWordpressComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(WordPressNewPostTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/wordpress.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
            .customAction(true);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "%s/wp-json"
                .formatted(connectionParameters.getRequiredString(WEBSITE)))
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .properties(
                        string(USERNAME)
                            .label("Application Password Name")
                            .description("Wordpress application password name.")
                            .required(true),
                        string(PASSWORD)
                            .label("Application Password")
                            .description("Application password, found in Users -> Profile.")
                            .required(true),
                        string(WEBSITE)
                            .label("Wordpress Website")
                            .description("Wordpress website of your Wordpress site. Can be found in user settings.")
                            .required(true)));
    }
}
