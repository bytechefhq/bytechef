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

package com.bytechef.component.vtiger.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vtiger.constant.VTigerConstants.CREATE_PRODUCT;
import static com.bytechef.component.vtiger.constant.VTigerConstants.INSTANCE_URL;
import static com.bytechef.component.vtiger.constant.VTigerConstants.PRODUCT_NAME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.PRODUCT_TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class VTigerCreateProductAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_PRODUCT)
        .title("Create a Product")
        .description("Create a new Product for your CRM")
        .properties(
            string(PRODUCT_NAME)
                .label("Product Name")
                .description("Name of the product")
                .required(true),
            string(PRODUCT_TYPE)
                .options(
                    option("Solo", "Solo"),
                    option("Fixed Bundle", "Fixed Bundle"))
                .label("Product Type")
                .description("Type of the product")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("results")
                        .properties(
                            string("id"),
                            string(PRODUCT_NAME),
                            string(PRODUCT_TYPE),
                            string("createdtime"),
                            string("source"),
                            string("assigned_user_id"))))
        .perform(VTigerCreateProductAction::perform);

    private VTigerCreateProductAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, String> paramMap = paramMapFill(inputParameters);

        return context
            .http(http -> http.post(connectionParameters.getRequiredString(INSTANCE_URL) +
                "/restapi/v1/vtiger/default/create"))
            .body(
                Body.of(
                    "elementType", "Products",
                    "element", paramMap))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, String> paramMapFill(Parameters inputParameters) {
        Map<String, String> paramMap = new HashMap<>();

        paramMap.put(PRODUCT_NAME, inputParameters.getRequiredString(PRODUCT_NAME));
        paramMap.put(PRODUCT_TYPE, inputParameters.getRequiredString(PRODUCT_TYPE));

        return paramMap;
    }
}
