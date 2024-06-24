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

package com.bytechef.component.insightly;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.insightly.constant.InsightlyConstants.POD;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class InsightlyComponentHandler extends AbstractInsightlyComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/insightly.svg")
            .categories(List.of(ComponentCategory.CRM));
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .title("Basic Auth")
                    .properties(
                        string(POD)
                            .label("Pod")
                            .description(
                                "Your instances pod can be found under your API URL, e.g. " +
                                    "https://api.{pod}.insightly.com/v3.1")
                            .required(true),
                        string(USERNAME)
                            .label("API Key")
                            .required(true)))
            .baseUri(
                (connectionParameters, context) -> "https://api." + connectionParameters.getRequiredString(POD) +
                    ".insightly.com/v3.1");
    }
}
