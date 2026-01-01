# IntegrationTagApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getIntegrationTags**](IntegrationTagApi.md#getintegrationtags) | **GET** /integrations/tags | Get integration tags |
| [**updateIntegrationTags**](IntegrationTagApi.md#updateintegrationtags) | **PUT** /integrations/{id}/tags | Updates tags of an existing integration |



## getIntegrationTags

> Array&lt;Tag&gt; getIntegrationTags()

Get integration tags

Get integration tags.

### Example

```ts
import {
  Configuration,
  IntegrationTagApi,
} from '';
import type { GetIntegrationTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationTagApi();

  try {
    const data = await api.getIntegrationTags();
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

[**Array&lt;Tag&gt;**](Tag.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of integration tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateIntegrationTags

> updateIntegrationTags(id, updateTagsRequest)

Updates tags of an existing integration

Updates tags of an existing integration.

### Example

```ts
import {
  Configuration,
  IntegrationTagApi,
} from '';
import type { UpdateIntegrationTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationTagApi();

  const body = {
    // number | The id of an integration.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateIntegrationTagsRequest;

  try {
    const data = await api.updateIntegrationTags(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |
| **updateTagsRequest** | [UpdateTagsRequest](UpdateTagsRequest.md) |  | |

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

