# ApiCollectionApi

All URIs are relative to *http://localhost/api/automation/api-platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createApiCollection**](ApiCollectionApi.md#createapicollection) | **POST** /api-collections | Create a new API collection |
| [**deleteApiCollection**](ApiCollectionApi.md#deleteapicollection) | **DELETE** /api-collections/{id} | Delete an API collection |
| [**getApiCollection**](ApiCollectionApi.md#getapicollection) | **GET** /api-collections/{id} | Get an API collection by id |
| [**getWorkspaceApiCollections**](ApiCollectionApi.md#getworkspaceapicollections) | **GET** /workspaces/{id}/api-collections | Get all workspace API collections |
| [**updateApiCollection**](ApiCollectionApi.md#updateapicollection) | **PUT** /api-collections/{id} | Update an existing API collection |



## createApiCollection

> ApiCollection createApiCollection(apiCollection)

Create a new API collection

Create a new API collection.

### Example

```ts
import {
  Configuration,
  ApiCollectionApi,
} from '';
import type { CreateApiCollectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionApi();

  const body = {
    // ApiCollection
    apiCollection: ...,
  } satisfies CreateApiCollectionRequest;

  try {
    const data = await api.createApiCollection(body);
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
| **apiCollection** | [ApiCollection](ApiCollection.md) |  | |

### Return type

[**ApiCollection**](ApiCollection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API collection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteApiCollection

> deleteApiCollection(id)

Delete an API collection

Delete an API collection.

### Example

```ts
import {
  Configuration,
  ApiCollectionApi,
} from '';
import type { DeleteApiCollectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionApi();

  const body = {
    // number | The id of an API collection.
    id: 789,
  } satisfies DeleteApiCollectionRequest;

  try {
    const data = await api.deleteApiCollection(body);
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
| **id** | `number` | The id of an API collection. | [Defaults to `undefined`] |

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


## getApiCollection

> ApiCollection getApiCollection(id)

Get an API collection by id

Get an API collection by id.

### Example

```ts
import {
  Configuration,
  ApiCollectionApi,
} from '';
import type { GetApiCollectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionApi();

  const body = {
    // number | The id of an API collection.
    id: 789,
  } satisfies GetApiCollectionRequest;

  try {
    const data = await api.getApiCollection(body);
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
| **id** | `number` | The id of an API collection. | [Defaults to `undefined`] |

### Return type

[**ApiCollection**](ApiCollection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API collection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkspaceApiCollections

> Array&lt;ApiCollection&gt; getWorkspaceApiCollections(id, environmentId, projectId, tagId)

Get all workspace API collections

Get all workspace API collections.

### Example

```ts
import {
  Configuration,
  ApiCollectionApi,
} from '';
import type { GetWorkspaceApiCollectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // number | The environment id. (optional)
    environmentId: 789,
    // number | The project ids used for filtering project deployments. (optional)
    projectId: 789,
    // number | The tag id of used for filtering project deployments. (optional)
    tagId: 789,
  } satisfies GetWorkspaceApiCollectionsRequest;

  try {
    const data = await api.getWorkspaceApiCollections(body);
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
| **environmentId** | `number` | The environment id. | [Optional] [Defaults to `undefined`] |
| **projectId** | `number` | The project ids used for filtering project deployments. | [Optional] [Defaults to `undefined`] |
| **tagId** | `number` | The tag id of used for filtering project deployments. | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;ApiCollection&gt;**](ApiCollection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A list of API collections. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateApiCollection

> ApiCollection updateApiCollection(id, apiCollection)

Update an existing API collection

Update an existing API collection.

### Example

```ts
import {
  Configuration,
  ApiCollectionApi,
} from '';
import type { UpdateApiCollectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionApi();

  const body = {
    // number | The id of an API collection.
    id: 789,
    // ApiCollection
    apiCollection: ...,
  } satisfies UpdateApiCollectionRequest;

  try {
    const data = await api.updateApiCollection(body);
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
| **id** | `number` | The id of an API collection. | [Defaults to `undefined`] |
| **apiCollection** | [ApiCollection](ApiCollection.md) |  | |

### Return type

[**ApiCollection**](ApiCollection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated API collection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

