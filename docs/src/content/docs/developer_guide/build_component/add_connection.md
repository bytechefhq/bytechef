---
title: "Add Connection"
description: "How to create connection for a component."
---

In the `server/libs/modules/components/newcomponent/src/main/java/com/bytechef/component/newcomponent/connection`
package, the `NewComponentConnection` class defines the connection. The `CONNECTION_DEFINITION` constant contains all
the details about the connection, including its base URI, authorizations, properties, and more.

If your component uses OAuth2 authorization, you can define the authorization type, properties, authorization URL, and
scopes in the `authorizations` method of the `CONNECTION_DEFINITION` constant. The `properties` method allows you to
define the properties that are required for the connection, such as Client ID and Client Secret. Here is an example of
a connection with OAuth2 authorization:

``` java
public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "base url")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "authorization url")
                .scopes((connection, context) -> List.of("scope1", "scope2"))
                .tokenUrl((connectionParameters, context) -> "token url")
                .refreshUrl((connectionParameters, context) -> "refresh url"));
```

If another type of authorization is used, such as Basic or API Key, you can define it in the `authorizations` method
of the `CONNECTION_DEFINITION` constant. For more information, refer to the
[connection documentation](/developer_guide/component_specification/connection).
