# ConnectionApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createConnectedUserConnection**](ConnectionApi.md#createconnecteduserconnection) | **POST** /connected-users/{connectedUserId}/connections | Create a new connection for the connected user |
| [**createConnection**](ConnectionApi.md#createconnection) | **POST** /connections | Create a new connection |
| [**deleteConnectedUserConnection**](ConnectionApi.md#deleteconnecteduserconnection) | **DELETE** /connected-users/{connectedUserId}/connections/{connectionId} | Deletes a connection of a connected user |
| [**deleteConnection**](ConnectionApi.md#deleteconnection) | **DELETE** /connections/{id} | Delete a connection |
| [**getAllConnectedUserConnections**](ConnectionApi.md#getallconnecteduserconnections) | **GET** /connected-users/{connectedUserId}/connections | Get all connections a connected user can use |
| [**getConnectedUserConnections**](ConnectionApi.md#getconnecteduserconnections) | **GET** /connected-users/{connectedUserId}/components/{componentName}/connections | Get all connected user\&#39;s connections |
| [**getConnection**](ConnectionApi.md#getconnection) | **GET** /connections/{id} | Get a connection by id |
| [**getConnections**](ConnectionApi.md#getconnections) | **GET** /connections | Get all connections |
| [**updateConnectedUserConnectionCredentials**](ConnectionApi.md#updateconnecteduserconnectioncredentials) | **PUT** /connected-users/{connectedUserId}/connections/{connectionId}/credentials | Replaces the credentials of a provisioned connected user connection |
| [**updateConnection**](ConnectionApi.md#updateconnectionoperation) | **PATCH** /connections/{id} | Update an existing connection |
| [**updateConnectionCredentials**](ConnectionApi.md#updateconnectioncredentialsoperation) | **PUT** /connections/{id}/credentials | Replaces the credentials of an existing connection |



## createConnectedUserConnection

> number createConnectedUserConnection(connectedUserId, connection)

Create a new connection for the connected user

Create a new connection for the connected user.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { CreateConnectedUserConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // Connection
    connection: ...,
  } satisfies CreateConnectedUserConnectionRequest;

  try {
    const data = await api.createConnectedUserConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **connection** | [Connection](Connection.md) |  | |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connection id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createConnection

> number createConnection(connection)

Create a new connection

Create a new connection.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { CreateConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // Connection
    connection: ...,
  } satisfies CreateConnectionRequest;

  try {
    const data = await api.createConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connection** | [Connection](Connection.md) |  | |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connection id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteConnectedUserConnection

> deleteConnectedUserConnection(connectedUserId, connectionId)

Deletes a connection of a connected user

Deletes a connection a connected user created or a tenant admin provisioned for them.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { DeleteConnectedUserConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // number | The id of a connection.
    connectionId: 789,
  } satisfies DeleteConnectedUserConnectionRequest;

  try {
    const data = await api.deleteConnectedUserConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **connectionId** | `number` | The id of a connection. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteConnection

> deleteConnection(id)

Delete a connection

Delete a connection.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { DeleteConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connection.
    id: 789,
  } satisfies DeleteConnectionRequest;

  try {
    const data = await api.deleteConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a connection. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getAllConnectedUserConnections

> Array&lt;Connection&gt; getAllConnectedUserConnections(connectedUserId)

Get all connections a connected user can use

Get all connections a connected user can use, across every component.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetAllConnectedUserConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
  } satisfies GetAllConnectedUserConnectionsRequest;

  try {
    const data = await api.getAllConnectedUserConnections(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnectedUserConnections

> Array&lt;Connection&gt; getConnectedUserConnections(connectedUserId, componentName, connectionIds)

Get all connected user\&#39;s connections

Get all connected user\&#39;s connections.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetConnectedUserConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // string | The component name.
    componentName: componentName_example,
    // Array<number> | The list of allowed connection ids. (optional)
    connectionIds: ...,
  } satisfies GetConnectedUserConnectionsRequest;

  try {
    const data = await api.getConnectedUserConnections(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **componentName** | `string` | The component name. | [Defaults to `undefined`] |
| **connectionIds** | `Array<number>` | The list of allowed connection ids. | [Optional] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnection

> Connection getConnection(id)

Get a connection by id

Get a connection by id.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connection.
    id: 789,
  } satisfies GetConnectionRequest;

  try {
    const data = await api.getConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a connection. | [Defaults to `undefined`] |

### Return type

[**Connection**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnections

> Array&lt;Connection&gt; getConnections(componentName, connectionVersion, environmentId, tagId)

Get all connections

Get all connections.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // string | The component name used for filtering connections. (optional)
    componentName: componentName_example,
    // number | The connection version. (optional)
    connectionVersion: 56,
    // number | The environment id. (optional)
    environmentId: 789,
    // number | The tag id of used for filtering connections. (optional)
    tagId: 789,
  } satisfies GetConnectionsRequest;

  try {
    const data = await api.getConnections(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **componentName** | `string` | The component name used for filtering connections. | [Optional] [Defaults to `undefined`] |
| **connectionVersion** | `number` | The connection version. | [Optional] [Defaults to `undefined`] |
| **environmentId** | `number` | The environment id. | [Optional] [Defaults to `undefined`] |
| **tagId** | `number` | The tag id of used for filtering connections. | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateConnectedUserConnectionCredentials

> updateConnectedUserConnectionCredentials(connectedUserId, connectionId, updateConnectionCredentialsRequest)

Replaces the credentials of a provisioned connected user connection

Replaces the credentials of a connection a tenant admin provisioned for a connected user.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { UpdateConnectedUserConnectionCredentialsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // number | The id of a connection.
    connectionId: 789,
    // UpdateConnectionCredentialsRequest
    updateConnectionCredentialsRequest: ...,
  } satisfies UpdateConnectedUserConnectionCredentialsRequest;

  try {
    const data = await api.updateConnectedUserConnectionCredentials(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **connectionId** | `number` | The id of a connection. | [Defaults to `undefined`] |
| **updateConnectionCredentialsRequest** | [UpdateConnectionCredentialsRequest](UpdateConnectionCredentialsRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateConnection

> updateConnection(id, updateConnectionRequest)

Update an existing connection

Update an existing connection.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { UpdateConnectionOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connection.
    id: 789,
    // UpdateConnectionRequest
    updateConnectionRequest: ...,
  } satisfies UpdateConnectionOperationRequest;

  try {
    const data = await api.updateConnection(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a connection. | [Defaults to `undefined`] |
| **updateConnectionRequest** | [UpdateConnectionRequest](UpdateConnectionRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateConnectionCredentials

> updateConnectionCredentials(id, updateConnectionCredentialsRequest)

Replaces the credentials of an existing connection

Replaces the credentials of an existing connection.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { UpdateConnectionCredentialsOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connection.
    id: 789,
    // UpdateConnectionCredentialsRequest
    updateConnectionCredentialsRequest: ...,
  } satisfies UpdateConnectionCredentialsOperationRequest;

  try {
    const data = await api.updateConnectionCredentials(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | `number` | The id of a connection. | [Defaults to `undefined`] |
| **updateConnectionCredentialsRequest** | [UpdateConnectionCredentialsRequest](UpdateConnectionCredentialsRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

