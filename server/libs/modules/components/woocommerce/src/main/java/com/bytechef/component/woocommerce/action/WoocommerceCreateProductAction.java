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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CATEGORIES;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DESCRIPTION;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DIMENSIONS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.HEIGHT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.IMAGES;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LENGTH;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MANAGE_STOCK;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PRODUCT_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.REGULAR_PRICE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SRC;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STOCK_QUANTITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STOCK_STATUS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.TAGS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.TYPE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.WEIGHT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.WIDTH;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.woocommerce.util.WoocommerceUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marija Horvat
 */
public class WoocommerceCreateProductAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createProduct")
        .title("Create Product")
        .description("Create a new product.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Product name.")
                .required(true),
            string(REGULAR_PRICE)
                .label("Regular Price")
                .description("Product regular price.")
                .required(true),
            string(TYPE)
                .label("Type")
                .description("Product type.")
                .options(
                    option("Simple", "simple"),
                    option("Grouped", "grouped"),
                    option("External", "external"),
                    option("Variable", "variable"))
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("Product description.")
                .required(false),
            bool(MANAGE_STOCK)
                .label("Manage Stock")
                .description("Stock management at product level.")
                .required(false),
            integer(STOCK_QUANTITY)
                .label("Stock Quantity")
                .description("Stock quantity.")
                .required(false),
            string(STOCK_STATUS)
                .label("Stock Status")
                .description("Controls the stock status of the product.")
                .options(
                    option("Instock", "instock"),
                    option("Outofstock", "outofstock"),
                    option("Onbackorder", "onbackorder"))
                .required(false),
            string(WEIGHT)
                .label("Weight")
                .description("Product weight (kg).")
                .required(false),
            object(DIMENSIONS)
                .label("Dimensions")
                .description("Product dimensions.")
                .required(false)
                .properties(
                    string(LENGTH)
                        .label("Length")
                        .description("Product length (cm)."),
                    string(WIDTH)
                        .label("Width")
                        .description("Product width (cm)."),
                    string(HEIGHT)
                        .label("Height")
                        .description("Product height (cm).")),
            array(CATEGORIES)
                .label("Categories")
                .description("List of categories.")
                .required(false)
                .options((OptionsFunction<String>) WoocommerceUtils::getCategoryIdOptions)
                .items(string()),
            array(TAGS)
                .label("Tags")
                .description("List of tags.")
                .options((OptionsFunction<String>) WoocommerceUtils::getTagIdOptions)
                .required(false)
                .items(string()),
            array(IMAGES)
                .label("Images")
                .description("List of images.")
                .required(false)
                .items(
                    object()
                        .properties(
                            string(SRC)
                                .label("Src")
                                .description("Image URL."),
                            string(NAME)
                                .label("Name")
                                .description("Image name."))))
        .output(outputSchema(PRODUCT_OUTPUT_PROPERTY))
        .perform(WoocommerceCreateProductAction::perform);

    private WoocommerceCreateProductAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        List<Map<String, String>> categories = convertValuesToMaps(inputParameters.getList(CATEGORIES, String.class));
        List<Map<String, String>> tags = convertValuesToMaps(inputParameters.getList(TAGS, String.class));

        return context.http(http -> http.post("/products"))
            .body(
                Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    REGULAR_PRICE, inputParameters.getRequiredString(REGULAR_PRICE),
                    TYPE, inputParameters.getString(TYPE),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    MANAGE_STOCK, inputParameters.getBoolean(MANAGE_STOCK),
                    STOCK_QUANTITY, inputParameters.getInteger(STOCK_QUANTITY),
                    STOCK_STATUS, inputParameters.getString(STOCK_STATUS),
                    WEIGHT, inputParameters.getString(WEIGHT),
                    DIMENSIONS, inputParameters.getMap(DIMENSIONS),
                    CATEGORIES, categories,
                    TAGS, tags,
                    IMAGES, inputParameters.getMap(IMAGES)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static List<Map<String, String>> convertValuesToMaps(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
            .map(value -> Map.of(ID, value))
            .collect(Collectors.toList());
    }
}
