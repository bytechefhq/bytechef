# ApiClientApi

All URIs are relative to *http://localhost/api/automation/api-platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createApiClient**](ApiClientApi.md#createapiclient) | **POST** /api-clients | Create a new API client |
| [**deleteApiClient**](ApiClientApi.md#deleteapiclient) | **DELETE** /api-client/{id} | Delete an API client |
| [**getApiClient**](ApiClientApi.md#getapiclient) | **GET** /api-client/{id} | Get an API client by id |
| [**getApiClients**](ApiClientApi.md#getapiclients) | **GET** /api-clients | Get API clients |
| [**updateApiClient**](ApiClientApi.md#updateapiclient) | **PUT** /api-client/{id} | Update an existing API client |



## createApiClient

> CreateApiClient200Response createApiClient(apiClient)

Create a new API client

Create a new API client.

### Example

```ts
import {
  Configuration,
  ApiClientApi,
} from '';
import type { CreateApiClientRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiClientApi();

  const body = {
    // ApiClient
    apiClient: ...,
  } satisfies CreateApiClientRequest;

  try {
    const data = await api.createApiClient(body);
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
| **apiClient** | [ApiClient](ApiClient.md) |  | |

### Return type

[**CreateApiClient200Response**](CreateApiClient200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The secret API key object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteApiClient

> deleteApiClient(id)

Delete an API client

Delete an API client.

### Example

```ts
import {
  Configuration,
  ApiClientApi,
} from '';
import type { DeleteApiClientRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiClientApi();

  const body = {
    // number | The id of an API client.
    id: 789,
  } satisfies DeleteApiClientRequest;

  try {
    const data = await api.deleteApiClient(body);
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
| **id** | `number` | The id of an API client. | [Defaults to `undefined`] |

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


## getApiClient

> ApiClient getApiClient(id)

Get an API client by id

Get an API client by id.

### Example

```ts
import {
  Configuration,
  ApiClientApi,
} from '';
import type { GetApiClientRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiClientApi();

  const body = {
    // number | The id of an API client.
    id: 789,
  } satisfies GetApiClientRequest;

  try {
    const data = await api.getApiClient(body);
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
| **id** | `number` | The id of an API client. | [Defaults to `undefined`] |

### Return type

[**ApiClient**](ApiClient.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API client object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getApiClients

> Array&lt;ApiClient&gt; getApiClients()

Get API clients

Get API clients.

### Example

```ts
import {
  Configuration,
  ApiClientApi,
} from '';
import type { GetApiClientsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiClientApi();

  try {
    const data = await api.getApiClients();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;ApiClient&gt;**](ApiClient.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of API clients. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateApiClient

> updateApiClient(id, apiClient)

Update an existing API client

Update an existing API client.

### Example

```ts
import {
  Configuration,
  ApiClientApi,
} from '';
import type { UpdateApiClientRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiClientApi();

  const body = {
    // number | The id of an API client.
    id: 789,
    // ApiClient
    apiClient: ...,
  } satisfies UpdateApiClientRequest;

  try {
    const data = await api.updateApiClient(body);
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
| **id** | `number` | The id of an API client. | [Defaults to `undefined`] |
| **apiClient** | [ApiClient](ApiClient.md) |  | |

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

