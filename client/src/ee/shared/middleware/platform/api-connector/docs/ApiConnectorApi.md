# ApiConnectorApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createApiConnector**](ApiConnectorApi.md#createapiconnector) | **POST** /api-connectors | Create a new API Connector |
| [**deleteApiConnector**](ApiConnectorApi.md#deleteapiconnector) | **DELETE** /api-connectors/{id} | Delete an API Connector |
| [**enableApiConnector**](ApiConnectorApi.md#enableapiconnector) | **PATCH** /api-connectors/{id}/enable/{enable} | Enable/disable a API Connector. |
| [**getApiConnector**](ApiConnectorApi.md#getapiconnector) | **GET** /api-connectors/{id} | Get an API Connector by id |
| [**getApiConnectors**](ApiConnectorApi.md#getapiconnectors) | **GET** /api-connectors | Get API Connectors |
| [**importOpenApiSpecification**](ApiConnectorApi.md#importopenapispecificationoperation) | **POST** /api-connectors/import | Create API Connector by importing OpenAPI specification |
| [**updateApiConnector**](ApiConnectorApi.md#updateapiconnector) | **PUT** /api-connectors/{id} | Update an existing API Connector |



## createApiConnector

> ApiConnector createApiConnector(apiConnector)

Create a new API Connector

Create a new API Connector.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { CreateApiConnectorRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // ApiConnector
    apiConnector: ...,
  } satisfies CreateApiConnectorRequest;

  try {
    const data = await api.createApiConnector(body);
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
| **apiConnector** | [ApiConnector](ApiConnector.md) |  | |

### Return type

[**ApiConnector**](ApiConnector.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API Connector object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteApiConnector

> deleteApiConnector(id)

Delete an API Connector

Delete an API Connector.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { DeleteApiConnectorRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // number | The id of the API Connector.
    id: 789,
  } satisfies DeleteApiConnectorRequest;

  try {
    const data = await api.deleteApiConnector(body);
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
| **id** | `number` | The id of the API Connector. | [Defaults to `undefined`] |

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


## enableApiConnector

> enableApiConnector(id, enable)

Enable/disable a API Connector.

Enable/disable a API Connector.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { EnableApiConnectorRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // number | The id of the API Connector.
    id: 789,
    // boolean | Enable/disable the API Connector.
    enable: true,
  } satisfies EnableApiConnectorRequest;

  try {
    const data = await api.enableApiConnector(body);
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
| **id** | `number` | The id of the API Connector. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the API Connector. | [Defaults to `undefined`] |

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


## getApiConnector

> ApiConnector getApiConnector(id)

Get an API Connector by id

Get an API Connector by id.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { GetApiConnectorRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // number | The id of the API Connector.
    id: 789,
  } satisfies GetApiConnectorRequest;

  try {
    const data = await api.getApiConnector(body);
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
| **id** | `number` | The id of the API Connector. | [Defaults to `undefined`] |

### Return type

[**ApiConnector**](ApiConnector.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API Connector object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getApiConnectors

> Array&lt;ApiConnector&gt; getApiConnectors()

Get API Connectors

Get API Connectors.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { GetApiConnectorsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  try {
    const data = await api.getApiConnectors();
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

[**Array&lt;ApiConnector&gt;**](ApiConnector.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A list of API Connectors. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## importOpenApiSpecification

> ApiConnector importOpenApiSpecification(importOpenApiSpecificationRequest)

Create API Connector by importing OpenAPI specification

Create API Connector by importing OpenAPI specification.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { ImportOpenApiSpecificationOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // ImportOpenApiSpecificationRequest
    importOpenApiSpecificationRequest: ...,
  } satisfies ImportOpenApiSpecificationOperationRequest;

  try {
    const data = await api.importOpenApiSpecification(body);
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
| **importOpenApiSpecificationRequest** | [ImportOpenApiSpecificationRequest](ImportOpenApiSpecificationRequest.md) |  | |

### Return type

[**ApiConnector**](ApiConnector.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The API Connector object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateApiConnector

> ApiConnector updateApiConnector(id, apiConnector)

Update an existing API Connector

Update an existing API Connector.

### Example

```ts
import {
  Configuration,
  ApiConnectorApi,
} from '';
import type { UpdateApiConnectorRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiConnectorApi();

  const body = {
    // number | The id of the API Connector.
    id: 789,
    // ApiConnector
    apiConnector: ...,
  } satisfies UpdateApiConnectorRequest;

  try {
    const data = await api.updateApiConnector(body);
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
| **id** | `number` | The id of the API Connector. | [Defaults to `undefined`] |
| **apiConnector** | [ApiConnector](ApiConnector.md) |  | |

### Return type

[**ApiConnector**](ApiConnector.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated API Connector object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

