# ConnectionApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createConnection**](ConnectionApi.md#createconnection) | **POST** /connections | Create a new connection |
| [**deleteConnection**](ConnectionApi.md#deleteconnection) | **DELETE** /connections/{id} | Delete a connection |
| [**getConnection**](ConnectionApi.md#getconnection) | **GET** /connections/{id} | Get a connection by id |
| [**getWorkspaceConnections**](ConnectionApi.md#getworkspaceconnections) | **GET** /workspaces/{id}/connections | Get all workspace connections |
| [**updateConnection**](ConnectionApi.md#updateconnectionoperation) | **PATCH** /connections/{id} | Update an existing connection |



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


## getWorkspaceConnections

> Array&lt;Connection&gt; getWorkspaceConnections(id, componentName, connectionVersion, environmentId, tagId)

Get all workspace connections

Get all workspace connections.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetWorkspaceConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // string | The component name used for filtering connections. (optional)
    componentName: componentName_example,
    // number | The connection version. (optional)
    connectionVersion: 56,
    // number | The id of an environment. (optional)
    environmentId: 789,
    // number | The tag id of used for filtering connections. (optional)
    tagId: 789,
  } satisfies GetWorkspaceConnectionsRequest;

  try {
    const data = await api.getWorkspaceConnections(body);
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
| **id** | `number` | The id of a workspace. | [Defaults to `undefined`] |
| **componentName** | `string` | The component name used for filtering connections. | [Optional] [Defaults to `undefined`] |
| **connectionVersion** | `number` | The connection version. | [Optional] [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Optional] [Defaults to `undefined`] |
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

