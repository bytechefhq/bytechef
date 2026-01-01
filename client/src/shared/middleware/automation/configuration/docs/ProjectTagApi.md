# ProjectTagApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getProjectTags**](ProjectTagApi.md#getprojecttags) | **GET** /projects/tags | Get project tags. |
| [**updateProjectTags**](ProjectTagApi.md#updateprojecttags) | **PUT** /projects/{id}/tags | Updates tags of an existing project. |



## getProjectTags

> Array&lt;Tag&gt; getProjectTags()

Get project tags.

Get project tags.

### Example

```ts
import {
  Configuration,
  ProjectTagApi,
} from '';
import type { GetProjectTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectTagApi();

  try {
    const data = await api.getProjectTags();
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
| **200** | The list of project tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateProjectTags

> updateProjectTags(id, updateTagsRequest)

Updates tags of an existing project.

Updates tags of an existing project.

### Example

```ts
import {
  Configuration,
  ProjectTagApi,
} from '';
import type { UpdateProjectTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectTagApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateProjectTagsRequest;

  try {
    const data = await api.updateProjectTags(body);
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
| **id** | `number` | The id of a project. | [Defaults to `undefined`] |
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

