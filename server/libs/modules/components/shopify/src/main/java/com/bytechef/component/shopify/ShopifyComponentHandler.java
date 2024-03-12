package com.bytechef.component.shopify;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;

import static com.bytechef.component.definition.ComponentDSL.string;

/**
 *     This class will not be overwritten on the subsequent calls of generator.
 *     @generated
 */
@AutoService(OpenApiComponentHandler.class)
public class ShopifyComponentHandler extends AbstractShopifyComponentHandler {

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://${shopName}.myshopify.com/")
            .properties(
                string().required(true)
            );

        return modifiableConnectionDefinition;
    }
}
