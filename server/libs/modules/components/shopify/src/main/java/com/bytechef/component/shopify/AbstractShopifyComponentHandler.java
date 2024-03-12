package com.bytechef.component.shopify;

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

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.shopify.action.ShopifyCreateOrderAction;
import com.bytechef.component.shopify.connection.ShopifyConnection;
import java.lang.Override;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractShopifyComponentHandler implements OpenApiComponentHandler {
  private final ComponentDefinition componentDefinition = modifyComponent(
      component("shopify")
          .title("Shopify")
          .description("Shopify is an e-commerce platform for online stores and retail point-of-sale systems. The Shopify platform offers online retailers a suite of services, including payments, marketing, shipping and customer engagement tools.")
      )
      .actions(modifyActions(ShopifyCreateOrderAction.ACTION_DEFINITION))
  .connection(modifyConnection(ShopifyConnection.CONNECTION_DEFINITION)).triggers(getTriggers());

  @Override
  public ComponentDefinition getDefinition() {
    return componentDefinition;
  }
}
