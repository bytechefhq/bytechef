# WorkflowTestConfigurationApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteWorkflowTestConfigurationConnection**](WorkflowTestConfigurationApi.md#deleteworkflowtestconfigurationconnectionoperation) | **DELETE** /workflow-test-configurations/{workflowId}/workflow-nodes/{workflowNodeName}/{workflowConnectionKey}/connections | Delete a workflow test configuration connection |
| [**getWorkflowTestConfiguration**](WorkflowTestConfigurationApi.md#getworkflowtestconfiguration) | **GET** /workflow-test-configurations/{workflowId} | Get a workflow test configuration |
| [**getWorkflowTestConfigurationConnections**](WorkflowTestConfigurationApi.md#getworkflowtestconfigurationconnections) | **GET** /workflow-test-configurations/{workflowId}/workflow-nodes/{workflowNodeName}/connections | Get a workflow test configuration connections |
| [**saveWorkflowTestConfiguration**](WorkflowTestConfigurationApi.md#saveworkflowtestconfiguration) | **PUT** /workflow-test-configurations/{workflowId} | Create new or update an existing workflow test configuration |
| [**saveWorkflowTestConfigurationInputs**](WorkflowTestConfigurationApi.md#saveworkflowtestconfigurationinputsoperation) | **PUT** /workflow-test-configurations/{workflowId}/inputs | Save a workflow test configuration inputs |



## deleteWorkflowTestConfigurationConnection

> deleteWorkflowTestConfigurationConnection(workflowId, workflowNodeName, workflowConnectionKey, environmentId, deleteWorkflowTestConfigurationConnectionRequest)

Delete a workflow test configuration connection

Delete a workflow test configuration connection.

### Example

```ts
import {
  Configuration,
  WorkflowTestConfigurationApi,
} from '';
import type { DeleteWorkflowTestConfigurationConnectionOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestConfigurationApi();

  const body = {
    // string | The id of a testing workflow.
    workflowId: workflowId_example,
    // string | The action/trigger name defined in the workflow.
    workflowNodeName: workflowNodeName_example,
    // string | The key of a connection.
    workflowConnectionKey: workflowConnectionKey_example,
    // number | The id of an environment.
    environmentId: 789,
    // DeleteWorkflowTestConfigurationConnectionRequest
    deleteWorkflowTestConfigurationConnectionRequest: ...,
  } satisfies DeleteWorkflowTestConfigurationConnectionOperationRequest;

  try {
    const data = await api.deleteWorkflowTestConfigurationConnection(body);
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
| **workflowId** | `string` | The id of a testing workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The action/trigger name defined in the workflow. | [Defaults to `undefined`] |
| **workflowConnectionKey** | `string` | The key of a connection. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **deleteWorkflowTestConfigurationConnectionRequest** | [DeleteWorkflowTestConfigurationConnectionRequest](DeleteWorkflowTestConfigurationConnectionRequest.md) |  | |

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


## getWorkflowTestConfiguration

> WorkflowTestConfiguration getWorkflowTestConfiguration(workflowId, environmentId)

Get a workflow test configuration

Get a workflow test configuration.

### Example

```ts
import {
  Configuration,
  WorkflowTestConfigurationApi,
} from '';
import type { GetWorkflowTestConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestConfigurationApi();

  const body = {
    // string | The id of a workflow.
    workflowId: workflowId_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetWorkflowTestConfigurationRequest;

  try {
    const data = await api.getWorkflowTestConfiguration(body);
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
| **workflowId** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**WorkflowTestConfiguration**](WorkflowTestConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowTestConfigurationConnections

> Array&lt;WorkflowTestConfigurationConnection&gt; getWorkflowTestConfigurationConnections(workflowId, workflowNodeName, environmentId)

Get a workflow test configuration connections

Get a workflow test configuration connections.

### Example

```ts
import {
  Configuration,
  WorkflowTestConfigurationApi,
} from '';
import type { GetWorkflowTestConfigurationConnectionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestConfigurationApi();

  const body = {
    // string | The id of a testing workflow.
    workflowId: workflowId_example,
    // string | The action/trigger name defined in the workflow.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetWorkflowTestConfigurationConnectionsRequest;

  try {
    const data = await api.getWorkflowTestConfigurationConnections(body);
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
| **workflowId** | `string` | The id of a testing workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The action/trigger name defined in the workflow. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**Array&lt;WorkflowTestConfigurationConnection&gt;**](WorkflowTestConfigurationConnection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## saveWorkflowTestConfiguration

> WorkflowTestConfiguration saveWorkflowTestConfiguration(workflowId, workflowTestConfiguration)

Create new or update an existing workflow test configuration

Create new or update an existing workflow test configuration.

### Example

```ts
import {
  Configuration,
  WorkflowTestConfigurationApi,
} from '';
import type { SaveWorkflowTestConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestConfigurationApi();

  const body = {
    // string | The id of a workflow.
    workflowId: workflowId_example,
    // WorkflowTestConfiguration
    workflowTestConfiguration: ...,
  } satisfies SaveWorkflowTestConfigurationRequest;

  try {
    const data = await api.saveWorkflowTestConfiguration(body);
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
| **workflowId** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowTestConfiguration** | [WorkflowTestConfiguration](WorkflowTestConfiguration.md) |  | |

### Return type

[**WorkflowTestConfiguration**](WorkflowTestConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow test configuration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## saveWorkflowTestConfigurationInputs

> saveWorkflowTestConfigurationInputs(workflowId, environmentId, saveWorkflowTestConfigurationInputsRequest)

Save a workflow test configuration inputs

Save a workflow test configuration inputs.

### Example

```ts
import {
  Configuration,
  WorkflowTestConfigurationApi,
} from '';
import type { SaveWorkflowTestConfigurationInputsOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestConfigurationApi();

  const body = {
    // string | The id of a testing workflow.
    workflowId: workflowId_example,
    // number | The id of an environment.
    environmentId: 789,
    // SaveWorkflowTestConfigurationInputsRequest
    saveWorkflowTestConfigurationInputsRequest: ...,
  } satisfies SaveWorkflowTestConfigurationInputsOperationRequest;

  try {
    const data = await api.saveWorkflowTestConfigurationInputs(body);
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
| **workflowId** | `string` | The id of a testing workflow. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **saveWorkflowTestConfigurationInputsRequest** | [SaveWorkflowTestConfigurationInputsRequest](SaveWorkflowTestConfigurationInputsRequest.md) |  | |

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

