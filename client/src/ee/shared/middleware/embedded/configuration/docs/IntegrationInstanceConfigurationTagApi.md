# IntegrationInstanceConfigurationTagApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getIntegrationInstanceConfigurationTags**](IntegrationInstanceConfigurationTagApi.md#getintegrationinstanceconfigurationtags) | **GET** /integration-instance-configurations/tags | Get integration instance configuration tags |
| [**updateIntegrationInstanceConfigurationTags**](IntegrationInstanceConfigurationTagApi.md#updateintegrationinstanceconfigurationtags) | **PUT** /integration-instance-configurations/{id}/tags | Updates tags of an existing integration instance configuration |



## getIntegrationInstanceConfigurationTags

> Array&lt;Tag&gt; getIntegrationInstanceConfigurationTags()

Get integration instance configuration tags

Get integration instance configuration tags.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationTagApi,
} from '';
import type { GetIntegrationInstanceConfigurationTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationTagApi();

  try {
    const data = await api.getIntegrationInstanceConfigurationTags();
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
| **200** | The list of integration instance configuration tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateIntegrationInstanceConfigurationTags

> updateIntegrationInstanceConfigurationTags(id, updateTagsRequest)

Updates tags of an existing integration instance configuration

Updates tags of an existing integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationTagApi,
} from '';
import type { UpdateIntegrationInstanceConfigurationTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationTagApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateIntegrationInstanceConfigurationTagsRequest;

  try {
    const data = await api.updateIntegrationInstanceConfigurationTags(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
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

