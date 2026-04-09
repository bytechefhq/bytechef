package com.bytechef.component.canva.connection;

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
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class CanvaConnection {
  public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
      .baseUri((connectionParameters, context) -> "https://api.canva.com/rest/v1/").authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
      .title("OAuth2 Authorization Code")
      .properties(
          string(CLIENT_ID)
              .label("Client Id")
              .required(true),
          string(CLIENT_SECRET)
              .label("Client Secret")
              .required(true)
      )
      .authorizationUrl((connectionParameters, context) -> "https://www.canva.com/api/oauth/authorize")
      .scopes((connectionParameters, context) -> Map.of("design:content:write", true, "design:content:read", true, "asset:read", true)).tokenUrl((connectionParameters, context) -> "https://api.canva.com/rest/v1/oauth/token")
  .refreshUrl((connectionParameters, context) -> "https://api.canva.com/rest/v1/oauth/token"))
  ;

  private CanvaConnection() {
  }
}
