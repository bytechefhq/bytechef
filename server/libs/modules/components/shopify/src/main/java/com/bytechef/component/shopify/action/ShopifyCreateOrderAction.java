package com.bytechef.component.shopify.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION_URL;
import static com.bytechef.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.REFRESH_URL;
import static com.bytechef.component.definition.Authorization.SCOPES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ShopifyCreateOrderAction {
  public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createOrder")
      .title("Creates an order")
      .description("Adds an order into a Shopify store.")
      .metadata(
          Map.of(
              "method", "POST",
  "path", "/admin/api/2024-01/orders.json"
  ,"bodyContentType", BodyContentType.JSON
  ,"mimeType", "application/json"

          )
      )
      .properties(string("baseId").label("Base Id").description("The base id.").required(true).metadata(
     Map.of(
       "type", PropertyType.PATH
     )
  )
  ,object("__item").properties(string("name").label("Name").required(false),integer("fav_number").label("Fav Number").required(false)).label("Item").metadata(
     Map.of(
       "type", PropertyType.BODY
     )
  )
  )
  .outputSchema(object().properties(dateTime("createdTime").required(false),object("fields").additionalProperties(
      array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
  .required(false)).metadata(
     Map.of(
       "responseType", ResponseType.JSON
     )
  )
  );
}
