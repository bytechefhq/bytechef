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

package com.bytechef.component.pipeliner;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pipeliner.constant.PipelinerConstants.SERVER_URL;
import static com.bytechef.component.pipeliner.constant.PipelinerConstants.SPACE_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class PipelinerComponentHandler extends AbstractPipelinerComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/pipeliner.svg")
            .categories(ComponentCategory.CRM);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .title("Basic Auth")
                    .properties(
                        string(SPACE_ID)
                            .label("Space Id")
                            .description("Your Space ID")
                            .required(true),
                        string(SERVER_URL)
                            .label("Server URL")
                            .options(
                                option("https://us-east.api.pipelinersales.com/api/v100/rest/spaces/",
                                    "https://us-east.api.pipelinersales.com/api/v100/rest/spaces/"),
                                option("https://eu-central.api.pipelinersales.com/api/v100/rest/spaces/",
                                    "https://eu-central.api.pipelinersales.com/api/v100/rest/spaces/"),
                                option("https://ca-central.api.pipelinersales.com/api/v100/rest/spaces/",
                                    "https://ca-central.api.pipelinersales.com/api/v100/rest/spaces/"),
                                option("https://ap-southeast.api.pipelinersales.com/api/v100/rest/spaces/",
                                    "https://ap-southeast.api.pipelinersales.com/api/v100/rest/spaces/"))
                            .required(true),
                        string(USERNAME)
                            .label("Username")
                            .required(true),
                        string(PASSWORD)
                            .label("Password")
                            .required(true)))
            .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(SERVER_URL) +
                connectionParameters.getRequiredString(SPACE_ID));

        return modifiableConnectionDefinition;
    }
}
