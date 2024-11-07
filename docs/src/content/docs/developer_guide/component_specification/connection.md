---
title: "Connection"
---

The connection definition is used to specify the properties of a connection. Below is an explanation of each method that can be used in the connection definition:

- `connection()` - Initializes a new `ModifiableConnectionDefinition`, which serves as the foundation for defining connection properties.
- `authorizations(ModifiableAuthorization... authorizations)` - Specifies the authorization mechanisms required for the connection, allowing you to define multiple authorization types such as OAuth2, API Key, or Basic Auth.
- `baseUri(BaseUriFunction baseUri)` - Sets the base URI for the connection, which is the root URL used for all API requests made through this connection.
- `version(int version)` - Defines the version number of the connection, useful for managing changes and updates to the connection configuration over time.
