# WorkflowApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteWorkflow**](WorkflowApi.md#deleteworkflow) | **DELETE** /workflows/{id} | Delete a workflow |
| [**getIntegrationVersionWorkflows**](WorkflowApi.md#getintegrationversionworkflows) | **GET** /integrations/{id}/versions/{integrationVersion}/workflows | Get workflows for particular integration version. |
| [**getIntegrationWorkflow**](WorkflowApi.md#getintegrationworkflow) | **GET** /workflows/by-integration-workflow-id/{integrationWorkflowId} | Get workflow for particular integration. |
| [**getIntegrationWorkflows**](WorkflowApi.md#getintegrationworkflows) | **GET** /integrations/{id}/workflows | Get integration workflows for particular integration |
| [**getWorkflow**](WorkflowApi.md#getworkflow) | **GET** /workflows/{id} | Get a workflow by id |
| [**updateWorkflow**](WorkflowApi.md#updateworkflow) | **PUT** /workflows/{id} | Update an existing workflow |



## deleteWorkflow

> deleteWorkflow(id)

Delete a workflow

Delete a workflow.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { DeleteWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // string | The id of the workflow to delete.
    id: id_example,
  } satisfies DeleteWorkflowRequest;

  try {
    const data = await api.deleteWorkflow(body);
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
| **id** | `string` | The id of the workflow to delete. | [Defaults to `undefined`] |

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


## getIntegrationVersionWorkflows

> Array&lt;Workflow&gt; getIntegrationVersionWorkflows(id, integrationVersion, includeAllFields)

Get workflows for particular integration version.

Get workflows for particular integration version.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetIntegrationVersionWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a integration.
    id: 789,
    // number | The version of a integration.
    integrationVersion: 56,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
  } satisfies GetIntegrationVersionWorkflowsRequest;

  try {
    const data = await api.getIntegrationVersionWorkflows(body);
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
| **id** | `number` | The id of a integration. | [Defaults to `undefined`] |
| **integrationVersion** | `number` | The version of a integration. | [Defaults to `undefined`] |
| **includeAllFields** | `boolean` | Use for including all fields or just basic ones. | [Optional] [Defaults to `true`] |

### Return type

[**Array&lt;Workflow&gt;**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The array of integration workflows. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationWorkflow

> Workflow getIntegrationWorkflow(integrationWorkflowId)

Get workflow for particular integration.

Get workflow for particular integration.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetIntegrationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of an integration workflow.
    integrationWorkflowId: 789,
  } satisfies GetIntegrationWorkflowRequest;

  try {
    const data = await api.getIntegrationWorkflow(body);
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
| **integrationWorkflowId** | `number` | The id of an integration workflow. | [Defaults to `undefined`] |

### Return type

[**Workflow**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationWorkflows

> Array&lt;Workflow&gt; getIntegrationWorkflows(id)

Get integration workflows for particular integration

Get integration workflows for particular integration.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetIntegrationWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of an integration.
    id: 789,
  } satisfies GetIntegrationWorkflowsRequest;

  try {
    const data = await api.getIntegrationWorkflows(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |

### Return type

[**Array&lt;Workflow&gt;**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated integration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflow

> Workflow getWorkflow(id)

Get a workflow by id

Get a workflow by id.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // string | The id of the workflow to get.
    id: id_example,
  } satisfies GetWorkflowRequest;

  try {
    const data = await api.getWorkflow(body);
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
| **id** | `string` | The id of the workflow to get. | [Defaults to `undefined`] |

### Return type

[**Workflow**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateWorkflow

> updateWorkflow(id, workflow)

Update an existing workflow

Update an existing workflow.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { UpdateWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // string | The id of the workflow to update.
    id: id_example,
    // Workflow
    workflow: ...,
  } satisfies UpdateWorkflowRequest;

  try {
    const data = await api.updateWorkflow(body);
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
| **id** | `string` | The id of the workflow to update. | [Defaults to `undefined`] |
| **workflow** | [Workflow](Workflow.md) |  | |

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

