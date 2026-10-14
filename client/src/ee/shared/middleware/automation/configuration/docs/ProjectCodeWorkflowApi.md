# ProjectCodeWorkflowApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployProject**](ProjectCodeWorkflowApi.md#deployproject) | **POST** /projects/deploy | Deploy a code based project |



## deployProject

> deployProject(workspaceId, projectFile)

Deploy a code based project

Deploy a code based project.

### Example

```ts
import {
  Configuration,
  ProjectCodeWorkflowApi,
} from '';
import type { DeployProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectCodeWorkflowApi();

  const body = {
    // number | The id of a workspace this project will belong.
    workspaceId: 789,
    // Blob | The file of a code-native project.
    projectFile: BINARY_DATA_HERE,
  } satisfies DeployProjectRequest;

  try {
    const data = await api.deployProject(body);
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
| **workspaceId** | `number` | The id of a workspace this project will belong. | [Defaults to `undefined`] |
| **projectFile** | `Blob` | The file of a code-native project. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

