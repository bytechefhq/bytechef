# ConnectedUserProjectWorkflowApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**enableConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#enableconnecteduserprojectworkflow) | **PATCH** /connected-user-project-workflows/{workflowUuid}/enable/{enable} | Enable/disable a connected user project workflow |
| [**getConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#getconnecteduserprojectworkflow) | **GET** /connected-user-project-workflows/{workflowUuid} | Get connected user project workflow. |
| [**publishConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#publishconnecteduserprojectworkflowoperation) | **POST** /connected-user-project-workflows/{workflowUuid}/publish | Publishes existing connected user project workflow |



## enableConnectedUserProjectWorkflow

> enableConnectedUserProjectWorkflow(workflowUuid, enable, xEnvironment)

Enable/disable a connected user project workflow

Enable/disable a connected user project workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { EnableConnectedUserProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // boolean | Enable/disable the project deployment.
    enable: true,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies EnableConnectedUserProjectWorkflowRequest;

  try {
    const data = await api.enableConnectedUserProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the project deployment. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

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


## getConnectedUserProjectWorkflow

> ConnectedUserProjectWorkflow getConnectedUserProjectWorkflow(workflowUuid, xEnvironment)

Get connected user project workflow.

Get connected user project workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { GetConnectedUserProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetConnectedUserProjectWorkflowRequest;

  try {
    const data = await api.getConnectedUserProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**ConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connected user project workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishConnectedUserProjectWorkflow

> publishConnectedUserProjectWorkflow(workflowUuid, publishConnectedUserProjectWorkflowRequest, xEnvironment)

Publishes existing connected user project workflow

Publishes existing connected user project workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { PublishConnectedUserProjectWorkflowOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // PublishConnectedUserProjectWorkflowRequest
    publishConnectedUserProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies PublishConnectedUserProjectWorkflowOperationRequest;

  try {
    const data = await api.publishConnectedUserProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **publishConnectedUserProjectWorkflowRequest** | [PublishConnectedUserProjectWorkflowRequest](PublishConnectedUserProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

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

