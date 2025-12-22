# WorkflowNodeParameterApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteClusterElementParameter**](WorkflowNodeParameterApi.md#deleteclusterelementparameteroperation) | **DELETE** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/parameters | Deletes a cluster element parameter |
| [**deleteWorkflowNodeParameter**](WorkflowNodeParameterApi.md#deleteworkflownodeparameter) | **DELETE** /workflows/{id}/workflow-nodes/{workflowNodeName}/parameters | Deletes a workflow node parameter |
| [**getClusterElementParameterDisplayConditions**](WorkflowNodeParameterApi.md#getclusterelementparameterdisplayconditions) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/display-conditions | Get cluster element property options shown in the editor |
| [**getWorkflowNodeParameterDisplayConditions**](WorkflowNodeParameterApi.md#getworkflownodeparameterdisplayconditions) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/display-conditions | Get action or trigger display conditions shown in the editor |
| [**updateClusterElementParameter**](WorkflowNodeParameterApi.md#updateclusterelementparameteroperation) | **PATCH** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/parameters | Updates a cluster element parameter |
| [**updateWorkflowNodeParameter**](WorkflowNodeParameterApi.md#updateworkflownodeparameteroperation) | **PATCH** /workflows/{id}/workflow-nodes/{workflowNodeName}/parameters | Updates a workflow node parameter |



## deleteClusterElementParameter

> DeleteClusterElementParameter200Response deleteClusterElementParameter(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, environmentId, deleteClusterElementParameterRequest)

Deletes a cluster element parameter

Deletes a cluster element parameter.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { DeleteClusterElementParameterOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node name.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // DeleteClusterElementParameterRequest
    deleteClusterElementParameterRequest: ...,
  } satisfies DeleteClusterElementParameterOperationRequest;

  try {
    const data = await api.deleteClusterElementParameter(body);
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
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node name. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **deleteClusterElementParameterRequest** | [DeleteClusterElementParameterRequest](DeleteClusterElementParameterRequest.md) |  | |

### Return type

[**DeleteClusterElementParameter200Response**](DeleteClusterElementParameter200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow node parameters. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteWorkflowNodeParameter

> DeleteClusterElementParameter200Response deleteWorkflowNodeParameter(id, workflowNodeName, environmentId, deleteClusterElementParameterRequest)

Deletes a workflow node parameter

Deletes a workflow node parameter.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { DeleteWorkflowNodeParameterRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // DeleteClusterElementParameterRequest
    deleteClusterElementParameterRequest: ...,
  } satisfies DeleteWorkflowNodeParameterRequest;

  try {
    const data = await api.deleteWorkflowNodeParameter(body);
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
| **deleteClusterElementParameterRequest** | [DeleteClusterElementParameterRequest](DeleteClusterElementParameterRequest.md) |  | |

### Return type

[**DeleteClusterElementParameter200Response**](DeleteClusterElementParameter200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow node parameters. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getClusterElementParameterDisplayConditions

> GetClusterElementParameterDisplayConditions200Response getClusterElementParameterDisplayConditions(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, environmentId)

Get cluster element property options shown in the editor

Get cluster element options shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { GetClusterElementParameterDisplayConditionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node name.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetClusterElementParameterDisplayConditionsRequest;

  try {
    const data = await api.getClusterElementParameterDisplayConditions(body);
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
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node name. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**GetClusterElementParameterDisplayConditions200Response**](GetClusterElementParameterDisplayConditions200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow node parameter display conditions. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowNodeParameterDisplayConditions

> GetClusterElementParameterDisplayConditions200Response getWorkflowNodeParameterDisplayConditions(id, workflowNodeName, environmentId)

Get action or trigger display conditions shown in the editor

Get action or trigger display conditions shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { GetWorkflowNodeParameterDisplayConditionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetWorkflowNodeParameterDisplayConditionsRequest;

  try {
    const data = await api.getWorkflowNodeParameterDisplayConditions(body);
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

[**GetClusterElementParameterDisplayConditions200Response**](GetClusterElementParameterDisplayConditions200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow node parameter display conditions. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateClusterElementParameter

> DeleteClusterElementParameter200Response updateClusterElementParameter(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, environmentId, updateClusterElementParameterRequest)

Updates a cluster element parameter

Updates a cluster element parameter.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { UpdateClusterElementParameterOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node name.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // UpdateClusterElementParameterRequest
    updateClusterElementParameterRequest: ...,
  } satisfies UpdateClusterElementParameterOperationRequest;

  try {
    const data = await api.updateClusterElementParameter(body);
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
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node name. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **updateClusterElementParameterRequest** | [UpdateClusterElementParameterRequest](UpdateClusterElementParameterRequest.md) |  | |

### Return type

[**DeleteClusterElementParameter200Response**](DeleteClusterElementParameter200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow node parameters. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateWorkflowNodeParameter

> DeleteClusterElementParameter200Response updateWorkflowNodeParameter(id, workflowNodeName, environmentId, updateWorkflowNodeParameterRequest)

Updates a workflow node parameter

Updates a workflow node parameter.

### Example

```ts
import {
  Configuration,
  WorkflowNodeParameterApi,
} from '';
import type { UpdateWorkflowNodeParameterOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeParameterApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
    // UpdateWorkflowNodeParameterRequest
    updateWorkflowNodeParameterRequest: ...,
  } satisfies UpdateWorkflowNodeParameterOperationRequest;

  try {
    const data = await api.updateWorkflowNodeParameter(body);
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
| **updateWorkflowNodeParameterRequest** | [UpdateWorkflowNodeParameterRequest](UpdateWorkflowNodeParameterRequest.md) |  | |

### Return type

[**DeleteClusterElementParameter200Response**](DeleteClusterElementParameter200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow node parameters. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

