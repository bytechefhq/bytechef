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

package com.bytechef.component.shopify.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NAME;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_OPTIONS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STATUS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.TITLE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.USER_ERRORS_PROPERTY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VALUES;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ShopifyCreateProductAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProduct")
        .title("Create Product")
        .description("Create new product for your store.")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Title of new product.")
                .required(true),
            array(PRODUCT_OPTIONS)
                .label("Product Options")
                .description("Options that will describe the product, for example: size, color.")
                .required(false)
                .items(
                    object("productOption")
                        .label("Product Option")
                        .description(
                            "Option that will describe the product. Option will be defined with name and values.")
                        .properties(
                            string(NAME)
                                .label("Name")
                                .description("Name of the option that will describe the product.")
                                .required(true),
                            array(VALUES)
                                .label("Values")
                                .description("Possible values of the option that describes the product.")
                                .required(true)
                                .items(
                                    string("value")
                                        .label("Value")
                                        .description("Value of the option that describes the product.")
                                        .required(false)))))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("product")
                            .description("The created product.")
                            .properties(
                                string("id")
                                    .description("ID of the created product."),
                                string("title")
                                    .description("Title of the created product."),
                                array("options")
                                    .description("List of different product options.")
                                    .items(
                                        object()
                                            .properties(
                                                string("id")
                                                    .description("ID of product option."),
                                                string("name")
                                                    .description("Name of the product option."),
                                                integer("position")
                                                    .description("Position of the product option."),
                                                array("optionValues")
                                                    .items(
                                                        object()
                                                            .properties(
                                                                string("id")
                                                                    .description("ID of the option value."),
                                                                string("name")
                                                                    .description("Name of the option value."),
                                                                bool("hasVariants")
                                                                    .description(
                                                                        "Whether the product option has other variants.")))))),
                        USER_ERRORS_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/shopify_v1#create-product")
        .perform(ShopifyCreateProductAction::perform);

    private ShopifyCreateProductAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
            mutation CreateProduct($product: ProductCreateInput!) {
              productCreate(product: $product) {
                product {
                  id
                  title
                  options {
                    id
                    name
                    position
                    optionValues {
                      id
                      name
                      hasVariants
                    }
                  }
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        return executeGraphQlOperation(
            query,
            context,
            Map.of(
                "product", Map.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    STATUS, "ACTIVE",
                    PRODUCT_OPTIONS, getProductOptionsList(
                        inputParameters.getList(PRODUCT_OPTIONS, Object.class, List.of())))),
            "productCreate");
    }

    private static List<Map<String, Object>> getProductOptionsList(List<Object> options) {
        return options.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(option -> Map.of(
                NAME, option.get(NAME),
                VALUES, extractValues(option.get(VALUES))))
            .toList();
    }

    private static List<Map<String, String>> extractValues(Object valuesObject) {
        if (valuesObject instanceof List<?> values) {
            return values.stream()
                .map(value -> Map.of(NAME, String.valueOf(value)))
                .toList();
        }

        return List.of();
    }
}
