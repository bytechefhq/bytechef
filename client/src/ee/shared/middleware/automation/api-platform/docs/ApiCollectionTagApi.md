# ApiCollectionTagApi

All URIs are relative to *http://localhost/api/automation/api-platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getApiCollectionTags**](ApiCollectionTagApi.md#getapicollectiontags) | **GET** /api-collections/tags | Get API collection tags |
| [**updateApiCollectionTags**](ApiCollectionTagApi.md#updateapicollectiontags) | **PUT** /api-collections/{id}/tags | Updates tags of an existing API collection |



## getApiCollectionTags

> Array&lt;Tag&gt; getApiCollectionTags()

Get API collection tags

Get API collection tags.

### Example

```ts
import {
  Configuration,
  ApiCollectionTagApi,
} from '';
import type { GetApiCollectionTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionTagApi();

  try {
    const data = await api.getApiCollectionTags();
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
| **200** | The list of API collection tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateApiCollectionTags

> updateApiCollectionTags(id, updateTagsRequest)

Updates tags of an existing API collection

Updates tags of an existing API collection.

### Example

```ts
import {
  Configuration,
  ApiCollectionTagApi,
} from '';
import type { UpdateApiCollectionTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApiCollectionTagApi();

  const body = {
    // number | The id of an API collection.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateApiCollectionTagsRequest;

  try {
    const data = await api.updateApiCollectionTags(body);
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

