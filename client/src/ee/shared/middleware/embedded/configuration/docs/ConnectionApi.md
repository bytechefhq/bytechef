# ConnectionApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createConnectedUserProjectWorkflowConnection**](ConnectionApi.md#createconnecteduserprojectworkflowconnection) | **POST** /connected-users/{connectedUserId}/workflows/{workflowUuid}/connections | Create a new connection for the connected user\&#39;s project workflow |
| [**createConnection**](ConnectionApi.md#createconnection) | **POST** /connections | Create a new connection |
| [**deleteConnection**](ConnectionApi.md#deleteconnection) | **DELETE** /connections/{id} | Delete a connection |
| [**getConnectedUserConnections**](ConnectionApi.md#getconnecteduserconnections) | **GET** /connected-users/{connectedUserId}/components/{componentName}/connections | Get all connected user\&#39;s connections |
| [**getConnection**](ConnectionApi.md#getconnection) | **GET** /connections/{id} | Get a connection by id |
| [**getConnections**](ConnectionApi.md#getconnections) | **GET** /connections | Get all connections |
| [**updateConnection**](ConnectionApi.md#updateconnectionoperation) | **PATCH** /connections/{id} | Update an existing connection |



## createConnectedUserProjectWorkflowConnection

> number createConnectedUserProjectWorkflowConnection(connectedUserId, workflowUuid, connection)

Create a new connection for the connected user\&#39;s project workflow

Create a new connection for the connected user\&#39;s project workflow.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { CreateConnectedUserProjectWorkflowConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Connection
    connection: ...,
  } satisfies CreateConnectedUserProjectWorkflowConnectionRequest;

  try {
    const data = await api.createConnectedUserProjectWorkflowConnection(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
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

