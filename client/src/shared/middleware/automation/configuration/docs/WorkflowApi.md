# WorkflowApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createProjectWorkflow**](WorkflowApi.md#createprojectworkflow) | **POST** /projects/{id}/workflows | Create new workflow and adds it to an existing project. |
| [**deleteWorkflow**](WorkflowApi.md#deleteworkflow) | **DELETE** /workflows/{id} | Delete a workflow |
| [**duplicateWorkflow**](WorkflowApi.md#duplicateworkflow) | **POST** /projects/{id}/workflows/{workflowId}/duplicate | Duplicates existing workflow. |
| [**getProjectVersionWorkflows**](WorkflowApi.md#getprojectversionworkflows) | **GET** /projects/{id}/versions/{projectVersion}/workflows | Get workflows for particular project version. |
| [**getProjectWorkflow**](WorkflowApi.md#getprojectworkflow) | **GET** /workflows/by-project-workflow-id/{projectWorkflowId} | Get workflow for particular project. |
| [**getProjectWorkflows**](WorkflowApi.md#getprojectworkflows) | **GET** /projects/{id}/workflows | Get workflows for particular project. |
| [**getWorkflow**](WorkflowApi.md#getworkflow) | **GET** /workflows/{id} | Get a workflow by id |
| [**getWorkflows**](WorkflowApi.md#getworkflows) | **GET** /workflows | Get all workflows. |
| [**updateWorkflow**](WorkflowApi.md#updateworkflow) | **PUT** /workflows/{id} | Update an existing workflow |



## createProjectWorkflow

> number createProjectWorkflow(id, workflow)

Create new workflow and adds it to an existing project.

Create new workflow and adds it to an existing project.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { CreateProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // Workflow
    workflow: ...,
  } satisfies CreateProjectWorkflowRequest;

  try {
    const data = await api.createProjectWorkflow(body);
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
| **id** | `number` | The id of a project. | [Defaults to `undefined`] |
| **workflow** | [Workflow](Workflow.md) |  | |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The project workflow id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


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


## duplicateWorkflow

> DuplicateWorkflow200Response duplicateWorkflow(id, workflowId)

Duplicates existing workflow.

Duplicates existing workflow.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { DuplicateWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // string | The id of a workflow.
    workflowId: workflowId_example,
  } satisfies DuplicateWorkflowRequest;

  try {
    const data = await api.duplicateWorkflow(body);
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
| **id** | `number` | The id of a project. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of a workflow. | [Defaults to `undefined`] |

### Return type

[**DuplicateWorkflow200Response**](DuplicateWorkflow200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The id of a new duplicated workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectVersionWorkflows

> Array&lt;Workflow&gt; getProjectVersionWorkflows(id, projectVersion, includeAllFields)

Get workflows for particular project version.

Get workflows for particular project version.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetProjectVersionWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // number | The version of a project.
    projectVersion: 56,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
  } satisfies GetProjectVersionWorkflowsRequest;

  try {
    const data = await api.getProjectVersionWorkflows(body);
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
| **id** | `number` | The id of a project. | [Defaults to `undefined`] |
| **projectVersion** | `number` | The version of a project. | [Defaults to `undefined`] |
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
| **200** | The array of project workflows. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectWorkflow

> Workflow getProjectWorkflow(projectWorkflowId)

Get workflow for particular project.

Get workflow for particular project.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a project workflow.
    projectWorkflowId: 789,
  } satisfies GetProjectWorkflowRequest;

  try {
    const data = await api.getProjectWorkflow(body);
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
| **projectWorkflowId** | `number` | The id of a project workflow. | [Defaults to `undefined`] |

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
| **200** | The project workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectWorkflows

> Array&lt;Workflow&gt; getProjectWorkflows(id)

Get workflows for particular project.

Get workflows for particular project.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetProjectWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies GetProjectWorkflowsRequest;

  try {
    const data = await api.getProjectWorkflows(body);
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
| **id** | `number` | The id of a project. | [Defaults to `undefined`] |

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
| **200** | The array of project workflows. |  -  |

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


## getWorkflows

> Array&lt;Workflow&gt; getWorkflows()

Get all workflows.

Get all workflows.

### Example

```ts
import {
  Configuration,
  WorkflowApi,
} from '';
import type { GetWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowApi();

  try {
    const data = await api.getWorkflows();
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

[**Array&lt;Workflow&gt;**](Workflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The array of workflows. |  -  |

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

