# WorkflowNodeTestOutputApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**checkWorkflowNodeTestOutputExists**](WorkflowNodeTestOutputApi.md#checkworkflownodetestoutputexists) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs/exists | Check if a workflow node test output exists |
| [**deleteWorkflowNodeTestOutput**](WorkflowNodeTestOutputApi.md#deleteworkflownodetestoutput) | **DELETE** /workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs | Delete existing workflow node test output |
| [**saveWorkflowNodeTestOutput**](WorkflowNodeTestOutputApi.md#saveworkflownodetestoutput) | **PUT** /workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs | Create a new or update existing workflow node test output |
| [**uploadWorkflowNodeSampleOutput**](WorkflowNodeTestOutputApi.md#uploadworkflownodesampleoutput) | **PUT** /workflows/{id}/workflow-nodes/{workflowNodeName}/test-outputs/sample-output | Upload a sample output to create a new or update existing workflow node test output |



## checkWorkflowNodeTestOutputExists

> CheckWorkflowNodeTestOutputExists200Response checkWorkflowNodeTestOutputExists(id, workflowNodeName, environmentId, createdDate)

Check if a workflow node test output exists

Check if a workflow node test output exists.

### Example

```ts
import {
  Configuration,
  WorkflowNodeTestOutputApi,
} from '';
import type { CheckWorkflowNodeTestOutputExistsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeTestOutputApi();

  const body = {
    // string | The id of a workflow.
    id: id_example,
    // string | The name of a workflow node for which to create test output objects.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // Date | Check if a test output exists after a specific date. (optional)
    createdDate: 2013-10-20T19:20:30+01:00,
  } satisfies CheckWorkflowNodeTestOutputExistsRequest;

  try {
    const data = await api.checkWorkflowNodeTestOutputExists(body);
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
| **id** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow node for which to create test output objects. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **createdDate** | `Date` | Check if a test output exists after a specific date. | [Optional] [Defaults to `undefined`] |

### Return type

[**CheckWorkflowNodeTestOutputExists200Response**](CheckWorkflowNodeTestOutputExists200Response.md)

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


## deleteWorkflowNodeTestOutput

> deleteWorkflowNodeTestOutput(id, workflowNodeName, environmentId)

Delete existing workflow node test output

Delete existing workflow node test output.

### Example

```ts
import {
  Configuration,
  WorkflowNodeTestOutputApi,
} from '';
import type { DeleteWorkflowNodeTestOutputRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeTestOutputApi();

  const body = {
    // string | The id of a workflow.
    id: id_example,
    // string | The name of a workflow node for which to create test output objects.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies DeleteWorkflowNodeTestOutputRequest;

  try {
    const data = await api.deleteWorkflowNodeTestOutput(body);
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
| **id** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow node for which to create test output objects. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

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


## saveWorkflowNodeTestOutput

> WorkflowNodeTestOutput saveWorkflowNodeTestOutput(id, workflowNodeName, environmentId)

Create a new or update existing workflow node test output

Create a new or update existing workflow node test output.

### Example

```ts
import {
  Configuration,
  WorkflowNodeTestOutputApi,
} from '';
import type { SaveWorkflowNodeTestOutputRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeTestOutputApi();

  const body = {
    // string | The id of a workflow.
    id: id_example,
    // string | The name of a workflow node for which to create test output objects.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies SaveWorkflowNodeTestOutputRequest;

  try {
    const data = await api.saveWorkflowNodeTestOutput(body);
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
| **id** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow node for which to create test output objects. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**WorkflowNodeTestOutput**](WorkflowNodeTestOutput.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow node test output object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## uploadWorkflowNodeSampleOutput

> WorkflowNodeTestOutput uploadWorkflowNodeSampleOutput(id, workflowNodeName, environmentId, body)

Upload a sample output to create a new or update existing workflow node test output

Upload a sample output to create a new or update existing workflow node test output.

### Example

```ts
import {
  Configuration,
  WorkflowNodeTestOutputApi,
} from '';
import type { UploadWorkflowNodeSampleOutputRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeTestOutputApi();

  const body = {
    // string | The id of a workflow.
    id: id_example,
    // string | The name of a workflow node for which to create test output objects.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // object
    body: Object,
  } satisfies UploadWorkflowNodeSampleOutputRequest;

  try {
    const data = await api.uploadWorkflowNodeSampleOutput(body);
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
| **id** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow node for which to create test output objects. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **body** | `object` |  | |

### Return type

[**WorkflowNodeTestOutput**](WorkflowNodeTestOutput.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow node test output object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

