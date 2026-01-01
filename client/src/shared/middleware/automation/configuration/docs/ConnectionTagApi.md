# ConnectionTagApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getConnectionTags**](ConnectionTagApi.md#getconnectiontags) | **GET** /connections/tags | Get connection tags |
| [**updateConnectionTags**](ConnectionTagApi.md#updateconnectiontags) | **PUT** /connections/{id}/tags | Updates tags of an existing connection |



## getConnectionTags

> Array&lt;Tag&gt; getConnectionTags()

Get connection tags

Get connection tags.

### Example

```ts
import {
  Configuration,
  ConnectionTagApi,
} from '';
import type { GetConnectionTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionTagApi();

  try {
    const data = await api.getConnectionTags();
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
| **200** | The list of connection tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateConnectionTags

> updateConnectionTags(id, updateTagsRequest)

Updates tags of an existing connection

Updates tags of an existing connection.

### Example

```ts
import {
  Configuration,
  ConnectionTagApi,
} from '';
import type { UpdateConnectionTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionTagApi();

  const body = {
    // number | The id of the connection.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateConnectionTagsRequest;

  try {
    const data = await api.updateConnectionTags(body);
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
| **id** | `number` | The id of the connection. | [Defaults to `undefined`] |
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

