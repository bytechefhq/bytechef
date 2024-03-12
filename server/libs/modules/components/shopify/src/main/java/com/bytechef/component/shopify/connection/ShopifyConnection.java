package com.bytechef.component.shopify.connection;

import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class ShopifyConnection {
  public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
      .baseUri((connectionParameters, context) -> "https://${shopName}.myshopify.com/").authorizations(authorization(
      AuthorizationType.API_KEY.toLowerCase(), AuthorizationType.API_KEY)
      .title("API Key")
      .properties(

          string(VALUE)
              .label("Value")
              .required(true)
          ,string(ADD_TO)
      .label("Add to")
      .required(true)
      .defaultValue(ApiTokenLocation.QUERY_PARAMETERS.name())
      .hidden(true)

      )
  )
  ;
}
