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

package com.bytechef.component.productboard.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.productboard.property.ProductboardFeatureProperties;
import com.bytechef.component.productboard.util.ProductboardUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ProductboardGetFeatureAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getFeature")
        .title("Get Feature")
        .description("Returns detail of a specific feature.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/features/{featureId}"

            ))
        .properties(string("featureId").label("Feature Id")
            .description("ID of the feature")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ProductboardUtils::getFeatureIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(object("data").properties(ProductboardFeatureProperties.PROPERTIES)
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ProductboardGetFeatureAction() {
    }
}
