# AutomationProjectCodeWorkflowApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployAutomationProjectCodeWorkflow**](AutomationProjectCodeWorkflowApi.md#deployautomationprojectcodeworkflow) | **POST** /automation-project-code-workflows/deploy | Deploy a new automation code workflow into the embedded catalog |
| [**listAutomationProjectCodeWorkflows**](AutomationProjectCodeWorkflowApi.md#listautomationprojectcodeworkflows) | **GET** /automation-project-code-workflows | List catalog projects in the embedded automation bridge |



## deployAutomationProjectCodeWorkflow

> AutomationProjectCodeWorkflowDeployResult deployAutomationProjectCodeWorkflow(xEnvironment, projectFile)

Deploy a new automation code workflow into the embedded catalog

Deploy a new automation code workflow into the embedded catalog. Unlike the connected-user-scoped endpoints on this API, this operation is authenticated as the API key\&#39;s own ByteChef user and does not fabricate a connected-user identity.

### Example

```ts
import {
  Configuration,
  AutomationProjectCodeWorkflowApi,
} from '';
import type { DeployAutomationProjectCodeWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectCodeWorkflowApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **projectFile** | `Blob` | The file of a code-native automation project. | [Optional] [Defaults to `undefined`] |

### Return type

[**AutomationProjectCodeWorkflowDeployResult**](AutomationProjectCodeWorkflowDeployResult.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listAutomationProjectCodeWorkflows

> Array&lt;AutomationProjectCodeWorkflow&gt; listAutomationProjectCodeWorkflows()

List catalog projects in the embedded automation bridge

List catalog projects in the embedded automation bridge. Unlike the connected-user-scoped endpoints on this API, this operation is authenticated as the API key\&#39;s own ByteChef user and does not fabricate a connected-user identity.

### Example

```ts
import {
  Configuration,
  AutomationProjectCodeWorkflowApi,
} from '';
import type { ListAutomationProjectCodeWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectCodeWorkflowApi(config);

  try {
    const data = await api.listAutomationProjectCodeWorkflows();
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

[**Array&lt;AutomationProjectCodeWorkflow&gt;**](AutomationProjectCodeWorkflow.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation workflow projects. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

