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

package com.bytechef.component.vtiger.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vtiger.constant.VTigerConstants.PRODUCT_NAME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.PRODUCT_TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class VTigerCreateProductAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProduct")
        .title("Create Product")
        .description("Creates a new product for your CRM.")
        .properties(
            string(PRODUCT_NAME)
                .label("Product Name")
                .description("Name of the product.")
                .required(true),
            string(PRODUCT_TYPE)
                .options(
                    option("Solo", "Solo"),
                    option("Fixed Bundle", "Fixed Bundle"))
                .label("Product Type")
                .description("Type of the product.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("results")
                            .properties(
                                string(PRODUCT_NAME)
                                    .description("Name of the product."),
                                string(PRODUCT_TYPE)
                                    .description("Type of the product."),
                                string("assigned_user_id"),
                                string("id")
                                    .description("ID of the product.")))))
        .perform(VTigerCreateProductAction::perform);

    private VTigerCreateProductAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/create"))
            .body(
                Body.of(
                    "elementType", "Products",
                    "element",
                    Map.of(
                        PRODUCT_NAME, inputParameters.getRequiredString(PRODUCT_NAME),
                        PRODUCT_TYPE, inputParameters.getRequiredString(PRODUCT_TYPE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
