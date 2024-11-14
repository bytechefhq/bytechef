---
title: "Connection"
---

The connection definition is used to specify the properties of a connection. Below is an explanation of each method that can be used in the connection definition:

- `connection()` - Initializes a new `ModifiableConnectionDefinition`, which serves as the foundation for defining connection properties.
- `authorizations(ModifiableAuthorization... authorizations)` - Specifies the authorization mechanisms required for the connection, allowing you to define multiple authorization types such as OAuth2, API Key, or Basic Auth.
- `baseUri(BaseUriFunction baseUri)` - Sets the base URI for the connection, which is the root URL used for all API requests made through this connection.
- `version(int version)` - Defines the version number of the connection, useful for managing changes and updates to the connection configuration over time.

## Authorizations

Each authorization is defined under the `ModifiableAuthorization` class which is designed to handle the authorization for components.

### Modifiable Authorization

- `authorization(AuthorizationType authorizationType)` - Initializes a new `ModifiableAuthorization` with the specified authorization type. This is the starting point for configuring an authorization.
- `apply(ApplyFunction apply)` - Sets the function that applies the acquired authorization credentials to requests. This function ensures that requests are properly authenticated using the obtained credentials.
- `authorizationUrl(AuthorizationUrlFunction authorizationUrl)` - Sets the function that provides the authorization URL. This URL is used to initiate the authorization process, often involving user consent.
- `description(String description)` - Sets a human-readable description of the authorization. This can be used for documentation or display purposes.
- `refreshTokenFunction(RefreshTokenFunction refreshTokenFunction)` - Sets the function responsible for handling refresh tokens. Refresh tokens are used to obtain new access tokens without re-authenticating the user.
- `properties(P... properties)` - Sets a list of properties associated with the authorization. These properties can include configuration options or metadata.
- `refreshUrl(RefreshUrlFunction refreshUrl)` - Sets the function that provides the URL for refreshing authorization credentials. This URL is used to obtain new tokens or credentials.
- `scopes(ScopesFunction scopes)` - Sets the function that defines the scopes of access requested during authorization. Scopes specify the permissions granted to the application.
- `title(String title)` - Sets a title for the authorization. This can be used for display purposes or to distinguish between different authorizations.
- `tokenUrl(TokenUrlFunction tokenUrl)` - Sets the function that provides the token URL. This URL is used to exchange authorization codes for access tokens in OAuth2 flows.

### Examples

Each authorization type has different properties that are required for successful authorization. Below are examples for common authorization types, demonstrating how to configure them effectively.

#### Basic Auth

Basic Auth is a simple authentication scheme built into the HTTP protocol. It requires a username and password, which are sent with each request.

```
authorization(AuthorizationType.BASIC_AUTH)
    .title("Basic Auth")
    .properties(
        string(USERNAME)
            .label("Username")
            .required(true),
        string(PASSWORD)
            .label("Password")
            .required(true))
```

#### Bearer Token

Bearer Token authentication involves sending a token with each request. This token is typically obtained from an authorization server and represents the user's identity.

```
authorization(AuthorizationType.BEARER_TOKEN)
    .title("Bearer Token")
    .properties(
        string(TOKEN)
            .label("Token")
            .required(true))
```

#### OAuth2 Authorization

OAuth2 Authorization Code is a robust authorization framework that allows third-party applications to obtain limited access to a web service. It involves redirecting the user to an authorization server to obtain an authorization code, which is then exchanged for an access token.

```
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
    .refreshUrl((connectionParameters, context) -> "refresh url")
```
