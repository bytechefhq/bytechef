# ProjectDeploymentTagApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getProjectDeploymentTags**](ProjectDeploymentTagApi.md#getprojectdeploymenttags) | **GET** /project-deployments/tags | Get project deployment tags |
| [**updateProjectDeploymentTags**](ProjectDeploymentTagApi.md#updateprojectdeploymenttags) | **PUT** /project-deployments/{id}/tags | Updates tags of an existing project deployment |



## getProjectDeploymentTags

> Array&lt;Tag&gt; getProjectDeploymentTags()

Get project deployment tags

Get project deployment tags.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentTagApi,
} from '';
import type { GetProjectDeploymentTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentTagApi();

  try {
    const data = await api.getProjectDeploymentTags();
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
| **200** | The list of project deployment tags. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateProjectDeploymentTags

> updateProjectDeploymentTags(id, updateTagsRequest)

Updates tags of an existing project deployment

Updates tags of an existing project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentTagApi,
} from '';
import type { UpdateProjectDeploymentTagsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentTagApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // UpdateTagsRequest
    updateTagsRequest: ...,
  } satisfies UpdateProjectDeploymentTagsRequest;

  try {
    const data = await api.updateProjectDeploymentTags(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
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

