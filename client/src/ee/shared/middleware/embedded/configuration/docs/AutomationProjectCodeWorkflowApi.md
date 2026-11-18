# AutomationProjectCodeWorkflowApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployAutomationProjectCodeWorkflow**](AutomationProjectCodeWorkflowApi.md#deployautomationprojectcodeworkflow) | **POST** /automation/projects/deploy | Deploy a new automation code workflow into the embedded catalog |



## deployAutomationProjectCodeWorkflow

> AutomationProjectCodeWorkflowDeployResult deployAutomationProjectCodeWorkflow(projectFile)

Deploy a new automation code workflow into the embedded catalog

Deploy a new automation code workflow into the embedded catalog.

### Example

```ts
import {
  Configuration,
  AutomationProjectCodeWorkflowApi,
} from '';
import type { DeployAutomationProjectCodeWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AutomationProjectCodeWorkflowApi();

  const body = {
    // Blob | The file of a code-native automation project. (optional)
    projectFile: BINARY_DATA_HERE,
  } satisfies DeployAutomationProjectCodeWorkflowRequest;

  try {
    const data = await api.deployAutomationProjectCodeWorkflow(body);
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
| **projectFile** | `Blob` | The file of a code-native automation project. | [Optional] [Defaults to `undefined`] |

### Return type

[**AutomationProjectCodeWorkflowDeployResult**](AutomationProjectCodeWorkflowDeployResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

