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

package com.bytechef.component.jotform;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.jotform.util.JotformUtils;
import com.google.auto.service.AutoService;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class JotformComponentHandler extends AbstractJotformComponentHandler {

    private static final String REGION = "region";

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/jotform.svg")
            .categories(ComponentCategory.SURVEYS_AND_FEEDBACK);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.API_KEY)
                    .title("API Key")
                    .properties(
                        string(REGION)
                            .label("Region")
                            .options(
                                option("US (jotform.com)", "us"),
                                option("EU (eu.jotform.com)", "eu"))
                            .required(true),
                        string(KEY)
                            .label("Key")
                            .required(true)
                            .defaultValue("APIKEY")
                            .hidden(true),
                        string(VALUE)
                            .label("API Key")
                            .required(true)))
            .baseUri((connectionParameters, context) -> {
                String region = connectionParameters.getRequiredString(REGION);

                return region.equals("us") ? "https://api.jotform.com" : "https://eu-api.jotform.com";
            });
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "formId")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) JotformUtils::getFormOptions);
        }

        return modifiableProperty;
    }
}
