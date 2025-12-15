# WorkflowNodeOutputApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getClusterElementOutput**](WorkflowNodeOutputApi.md#getclusterelementoutput) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/outputs | Get cluster element node output used in a workflow |
| [**getPreviousWorkflowNodeOutputs**](WorkflowNodeOutputApi.md#getpreviousworkflownodeoutputs) | **GET** /workflows/{id}/outputs | Get all dynamic workflow node outputs used in a workflow |
| [**getWorkflowNodeOutput**](WorkflowNodeOutputApi.md#getworkflownodeoutput) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/outputs | Get workflow node output of an action task or trigger used in a workflow |



## getClusterElementOutput

> WorkflowNodeOutput getClusterElementOutput(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, environmentId)

Get cluster element node output used in a workflow

Get cluster element node output used in a workflow.

### Example

```ts
import {
  Configuration,
  WorkflowNodeOutputApi,
} from '';
import type { GetClusterElementOutputRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeOutputApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetClusterElementOutputRequest;

  try {
    const data = await api.getClusterElementOutput(body);
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
| **id** | `string` | The workflow id | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **clusterElementType** | `string` | The name of a cluster element type. | [Defaults to `undefined`] |
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**WorkflowNodeOutput**](WorkflowNodeOutput.md)

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


## getPreviousWorkflowNodeOutputs

> Array&lt;WorkflowNodeOutput&gt; getPreviousWorkflowNodeOutputs(id, environmentId, lastWorkflowNodeName)

Get all dynamic workflow node outputs used in a workflow

Get all workflow node outputs used in a workflow.

### Example

```ts
import {
  Configuration,
  WorkflowNodeOutputApi,
} from '';
import type { GetPreviousWorkflowNodeOutputsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeOutputApi();

  const body = {
    // string | The workflow id for which to return all used action definitions
    id: id_example,
    // number | The id of an environment.
    environmentId: 789,
    // string | The name of the last workflow node (action task or trigger) up to which include the output schema (E.g. mailchimp_1, airtable_3) (optional)
    lastWorkflowNodeName: lastWorkflowNodeName_example,
  } satisfies GetPreviousWorkflowNodeOutputsRequest;

  try {
    const data = await api.getPreviousWorkflowNodeOutputs(body);
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
| **id** | `string` | The workflow id for which to return all used action definitions | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **lastWorkflowNodeName** | `string` | The name of the last workflow node (action task or trigger) up to which include the output schema (E.g. mailchimp_1, airtable_3) | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;WorkflowNodeOutput&gt;**](WorkflowNodeOutput.md)

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


## getWorkflowNodeOutput

> WorkflowNodeOutput getWorkflowNodeOutput(id, workflowNodeName, environmentId)

Get workflow node output of an action task or trigger used in a workflow

Get workflow node output of an action task or trigger used in a workflow.

### Example

```ts
import {
  Configuration,
  WorkflowNodeOutputApi,
} from '';
import type { GetWorkflowNodeOutputRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeOutputApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetWorkflowNodeOutputRequest;

  try {
    const data = await api.getWorkflowNodeOutput(body);
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
| **id** | `string` | The workflow id | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**WorkflowNodeOutput**](WorkflowNodeOutput.md)

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

