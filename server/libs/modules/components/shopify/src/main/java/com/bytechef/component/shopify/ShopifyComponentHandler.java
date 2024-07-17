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

package com.bytechef.component.shopify;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.SHOP_NAME;
import static com.bytechef.component.shopify.util.ShopifyUtils.getBaseUrl;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.shopify.trigger.ShopifyNewCancelledOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewOrderTrigger;
import com.bytechef.component.shopify.trigger.ShopifyNewPaidOrderTrigger;
import com.bytechef.component.shopify.util.ShopifyUtils;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class ShopifyComponentHandler extends AbstractShopifyComponentHandler {

    @Override
    public List<ComponentDSL.ModifiableTriggerDefinition> getTriggers() {
        return List.of(
            ShopifyNewCancelledOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewOrderTrigger.TRIGGER_DEFINITION,
            ShopifyNewPaidOrderTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/shopify.svg")
            .categories(ComponentCategory.E_COMMERCE);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(AuthorizationType.API_KEY)
                    .title("API Key")
                    .properties(
                        string(SHOP_NAME)
                            .label("Shop name")
                            .required(true),
                        string(KEY)
                            .label("Access token")
                            .required(true)
                            .defaultValue("X-Shopify-Access-Token")
                            .hidden(true),
                        string(VALUE)
                            .label("Access Token")
                            .required(true)))
            .baseUri((connectionParameters, context) -> getBaseUrl(connectionParameters));
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "orderId")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options((ActionOptionsFunction<Long>) ShopifyUtils::getOrderIdOptions);
        } else if (Objects.equals(modifiableProperty.getName(), "__item")) {
            Optional<List<? extends ValueProperty<?>>> propertiesOptional =
                ((ModifiableObjectProperty) modifiableProperty).getProperties();

            for (BaseProperty baseProperty : propertiesOptional.get()) {
                if (Objects.equals(baseProperty.getName(), "order")) {
                    Optional<List<? extends ValueProperty<?>>> propertiesOptional1 =
                        ((ModifiableObjectProperty) baseProperty).getProperties();

                    for (BaseProperty baseProperty1 : propertiesOptional1.get()) {
                        if (Objects.equals(baseProperty1.getName(), "line_items")) {
                            Optional<List<? extends ValueProperty<?>>> items =
                                ((ModifiableArrayProperty) baseProperty1).getItems();

                            for (BaseProperty baseProperty2 : items.get()) {
                                Optional<List<? extends ValueProperty<?>>> propertiesOptional2 =
                                    ((ModifiableObjectProperty) baseProperty2).getProperties();

                                for (BaseProperty baseProperty3 : propertiesOptional2.get()) {
                                    if (Objects.equals(baseProperty3.getName(), PRODUCT_ID)) {
                                        ((ModifiableIntegerProperty) baseProperty3)
                                            .options((ActionOptionsFunction<Long>) ShopifyUtils::getProductIdOptions);
                                    } else if (Objects.equals(baseProperty3.getName(), "variant_id")) {
                                        ((ModifiableIntegerProperty) baseProperty3)
                                            .optionsLookupDependsOn("__item.order.line_items[index]." + PRODUCT_ID)
                                            .options((ActionOptionsFunction<Long>) ShopifyUtils::getVariantIdOptions);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return modifiableProperty;
    }

}
