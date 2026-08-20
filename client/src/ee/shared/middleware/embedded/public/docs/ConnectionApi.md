# ConnectionApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createFrontendConnection**](ConnectionApi.md#createfrontendconnection) | **POST** /components/{componentName}/connections | Create a connected user connection |
| [**deleteFrontendConnection**](ConnectionApi.md#deletefrontendconnection) | **DELETE** /connections/{id} | Delete a connection |
| [**getAllFrontendConnections**](ConnectionApi.md#getallfrontendconnections) | **GET** /connections | Get all connected user\&#39;s connections |
| [**getConnections**](ConnectionApi.md#getconnections) | **GET** /{externalUserId}/components/{componentName}/connections | Get all connected user\&#39;s connections |
| [**getFrontendConnections**](ConnectionApi.md#getfrontendconnections) | **GET** /components/{componentName}/connections | Get all connected user\&#39;s connections |
| [**reauthorizeFrontendConnection**](ConnectionApi.md#reauthorizefrontendconnection) | **POST** /connections/{id}/reauthorize | Reauthorize a connection |



## createFrontendConnection

> number createFrontendConnection(componentName, createConnectionRequest, xEnvironment)

Create a connected user connection

Create a connection for the authenticated connected user.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { CreateFrontendConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // string | The component name.
    componentName: componentName_example,
    // CreateConnectionRequest
    createConnectionRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateFrontendConnectionRequest;

  try {
    const data = await api.createFrontendConnection(body);
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
| **componentName** | `string` | The component name. | [Defaults to `undefined`] |
| **createConnectionRequest** | [CreateConnectionRequest](CreateConnectionRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**number**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The id of the created connection. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteFrontendConnection

> deleteFrontendConnection(id, xEnvironment)

Delete a connection

Delete a connection owned by the authenticated connected user.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { DeleteFrontendConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // number | The id of a connection.
    id: 789,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DeleteFrontendConnectionRequest;

  try {
    const data = await api.deleteFrontendConnection(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The connection does not exist or is not owned by the caller. |  -  |
| **409** | The connection is still used by an automation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getAllFrontendConnections

> Array&lt;Connection&gt; getAllFrontendConnections(xEnvironment)

Get all connected user\&#39;s connections

Get every connection owned by the authenticated connected user.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetAllFrontendConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetAllFrontendConnectionsRequest;

  try {
    const data = await api.getAllFrontendConnections(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnections

> Array&lt;Connection&gt; getConnections(externalUserId, componentName, xEnvironment, connectionIds)

Get all connected user\&#39;s connections

Get all connected user\&#39;s connections.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The component name.
    componentName: componentName_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // Array<number> | The list of allowed connection ids. (optional)
    connectionIds: ...,
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **componentName** | `string` | The component name. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **connectionIds** | `Array<number>` | The list of allowed connection ids. | [Optional] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendConnections

> Array&lt;Connection&gt; getFrontendConnections(componentName, xEnvironment, connectionIds)

Get all connected user\&#39;s connections

Get all connected user\&#39;s connections.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { GetFrontendConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // string | The component name.
    componentName: componentName_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // Array<number> | The list of allowed connection ids. (optional)
    connectionIds: ...,
  } satisfies GetFrontendConnectionsRequest;

  try {
    const data = await api.getFrontendConnections(body);
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
| **componentName** | `string` | The component name. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **connectionIds** | `Array<number>` | The list of allowed connection ids. | [Optional] |

### Return type

[**Array&lt;Connection&gt;**](Connection.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of connections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## reauthorizeFrontendConnection

> reauthorizeFrontendConnection(id, reauthorizeConnectionRequest, xEnvironment)

Reauthorize a connection

Replace the credentials of a connection owned by the authenticated connected user, keeping its id.

### Example

```ts
import {
  Configuration,
  ConnectionApi,
} from '';
import type { ReauthorizeFrontendConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectionApi(config);

  const body = {
    // number | The id of a connection.
    id: 789,
    // ReauthorizeConnectionRequest
    reauthorizeConnectionRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ReauthorizeFrontendConnectionRequest;

  try {
    const data = await api.reauthorizeFrontendConnection(body);
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
| **reauthorizeConnectionRequest** | [ReauthorizeConnectionRequest](ReauthorizeConnectionRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The connection does not exist or is not owned by the caller. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

