# ApiCollectionEndpointApi

All URIs are relative to *http://localhost/api/automation/api-platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createApiCollectionEndpoint**](ApiCollectionEndpointApi.md#createapicollectionendpoint) | **POST** /api-collection-endpoints | Create a new API collection endpoint |
| [**deleteApiCollectionEndpoint**](ApiCollectionEndpointApi.md#deleteapicollectionendpoint) | **DELETE** /api-collection-endpoints/{id} | Delete an API collection endpoint |
| [**getApiCollectionEndpoint**](ApiCollectionEndpointApi.md#getapicollectionendpoint) | **GET** /api-collection-endpoints/{id} | Get an API collection endpoint by id |
| [**updateApiCollectionEndpoint**](ApiCollectionEndpointApi.md#updateapicollectionendpoint) | **PUT** /api-collection-endpoints/{id} | Update an existing API collection endpoint |



## createApiCollectionEndpoint

> ApiCollectionEndpoint createApiCollectionEndpoint(apiCollectionEndpoint)

Create a new API collection endpoint

Create a new API collection endpoint.

### Example

```ts
import {
  Configuration,
  ApiCollectionEndpointApi,
} from '';
import type { CreateApiCollectionEndpointRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionEndpointApi();

  const body = {
    // ApiCollectionEndpoint
    apiCollectionEndpoint: ...,
  } satisfies CreateApiCollectionEndpointRequest;

  try {
    const data = await api.createApiCollectionEndpoint(body);
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
| **apiCollectionEndpoint** | [ApiCollectionEndpoint](ApiCollectionEndpoint.md) |  | |

### Return type

[**ApiCollectionEndpoint**](ApiCollectionEndpoint.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API collection endpoint object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteApiCollectionEndpoint

> deleteApiCollectionEndpoint(id)

Delete an API collection endpoint

Delete an API collection endpoint.

### Example

```ts
import {
  Configuration,
  ApiCollectionEndpointApi,
} from '';
import type { DeleteApiCollectionEndpointRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionEndpointApi();

  const body = {
    // number | The id of an API collection endpoint.
    id: 789,
  } satisfies DeleteApiCollectionEndpointRequest;

  try {
    const data = await api.deleteApiCollectionEndpoint(body);
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
| **id** | `number` | The id of an API collection endpoint. | [Defaults to `undefined`] |

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


## getApiCollectionEndpoint

> ApiCollectionEndpoint getApiCollectionEndpoint(id)

Get an API collection endpoint by id

Get an API collection endpoint by id.

### Example

```ts
import {
  Configuration,
  ApiCollectionEndpointApi,
} from '';
import type { GetApiCollectionEndpointRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionEndpointApi();

  const body = {
    // number | The id of an API collection endpoint.
    id: 789,
  } satisfies GetApiCollectionEndpointRequest;

  try {
    const data = await api.getApiCollectionEndpoint(body);
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
| **id** | `number` | The id of an API collection endpoint. | [Defaults to `undefined`] |

### Return type

[**ApiCollectionEndpoint**](ApiCollectionEndpoint.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API collection endpoint object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateApiCollectionEndpoint

> ApiCollectionEndpoint updateApiCollectionEndpoint(id, apiCollectionEndpoint)

Update an existing API collection endpoint

Update an existing API collection endpoint.

### Example

```ts
import {
  Configuration,
  ApiCollectionEndpointApi,
} from '';
import type { UpdateApiCollectionEndpointRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionEndpointApi();

  const body = {
    // number | The id of an API collection endpoint.
    id: 789,
    // ApiCollectionEndpoint
    apiCollectionEndpoint: ...,
  } satisfies UpdateApiCollectionEndpointRequest;

  try {
    const data = await api.updateApiCollectionEndpoint(body);
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
| **id** | `number` | The id of an API collection endpoint. | [Defaults to `undefined`] |
| **apiCollectionEndpoint** | [ApiCollectionEndpoint](ApiCollectionEndpoint.md) |  | |

### Return type

[**ApiCollectionEndpoint**](ApiCollectionEndpoint.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated API collection endpoint object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

