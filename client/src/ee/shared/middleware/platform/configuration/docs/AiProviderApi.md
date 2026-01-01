# AiProviderApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteAiProvider**](AiProviderApi.md#deleteaiprovider) | **DELETE** /ai-providers/{id} | Delete an AI provider |
| [**enableAiProvider**](AiProviderApi.md#enableaiprovider) | **PATCH** /ai-providers/{id}/enable/{enable} | e |
| [**getAiProviders**](AiProviderApi.md#getaiproviders) | **GET** /ai-providers | Get AI providers |
| [**updateAiProvider**](AiProviderApi.md#updateaiprovideroperation) | **PATCH** /ai-providers/{id} | Update an existing AI provider |



## deleteAiProvider

> deleteAiProvider(id)

Delete an AI provider

Delete an AI provider.

### Example

```ts
import {
  Configuration,
  AiProviderApi,
} from '';
import type { DeleteAiProviderRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AiProviderApi();

  const body = {
    // number | The id of an AI provider.
    id: 56,
  } satisfies DeleteAiProviderRequest;

  try {
    const data = await api.deleteAiProvider(body);
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
| **id** | `number` | The id of an AI provider. | [Defaults to `undefined`] |

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


## enableAiProvider

> enableAiProvider(id, enable)

e

Enable/disable an AI provider.

### Example

```ts
import {
  Configuration,
  AiProviderApi,
} from '';
import type { EnableAiProviderRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AiProviderApi();

  const body = {
    // number | The id of an AI provider.
    id: 56,
    // boolean | The enable status of an AI provider.
    enable: true,
  } satisfies EnableAiProviderRequest;

  try {
    const data = await api.enableAiProvider(body);
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
| **id** | `number` | The id of an AI provider. | [Defaults to `undefined`] |
| **enable** | `boolean` | The enable status of an AI provider. | [Defaults to `undefined`] |

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


## getAiProviders

> Array&lt;AiProvider&gt; getAiProviders()

Get AI providers

Get AI providers.

### Example

```ts
import {
  Configuration,
  AiProviderApi,
} from '';
import type { GetAiProvidersRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AiProviderApi();

  try {
    const data = await api.getAiProviders();
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

[**Array&lt;AiProvider&gt;**](AiProvider.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of AI providers. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateAiProvider

> updateAiProvider(id, updateAiProviderRequest)

Update an existing AI provider

Update an existing AI provider.

### Example

```ts
import {
  Configuration,
  AiProviderApi,
} from '';
import type { UpdateAiProviderOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AiProviderApi();

  const body = {
    // number | The id of an AI provider.
    id: 56,
    // UpdateAiProviderRequest
    updateAiProviderRequest: ...,
  } satisfies UpdateAiProviderOperationRequest;

  try {
    const data = await api.updateAiProvider(body);
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
| **id** | `number` | The id of an AI provider. | [Defaults to `undefined`] |
| **updateAiProviderRequest** | [UpdateAiProviderRequest](UpdateAiProviderRequest.md) |  | |

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

