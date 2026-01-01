# WorkflowExecutionApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getWorkflowExecution**](WorkflowExecutionApi.md#getworkflowexecution) | **GET** /workflow-executions/{id} | Get workflow executions by id |
| [**getWorkflowExecutionsPage**](WorkflowExecutionApi.md#getworkflowexecutionspage) | **GET** /workspaces/{id}/workflow-executions | Get project workflow executions |



## getWorkflowExecution

> WorkflowExecution getWorkflowExecution(id)

Get workflow executions by id

Get workflow executions by id.

### Example

```ts
import {
  Configuration,
  WorkflowExecutionApi,
} from '';
import type { GetWorkflowExecutionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowExecutionApi();

  const body = {
    // number | The id of an execution.
    id: 789,
  } satisfies GetWorkflowExecutionRequest;

  try {
    const data = await api.getWorkflowExecution(body);
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
| **id** | `number` | The id of an execution. | [Defaults to `undefined`] |

### Return type

[**WorkflowExecution**](WorkflowExecution.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The execution object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowExecutionsPage

> Page getWorkflowExecutionsPage(id, embedded, environmentId, jobStatus, jobStartDate, jobEndDate, projectId, projectDeploymentId, workflowId, pageNumber)

Get project workflow executions

Get project workflow executions.

### Example

```ts
import {
  Configuration,
  WorkflowExecutionApi,
} from '';
import type { GetWorkflowExecutionsPageRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowExecutionApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // boolean | If embedded automation workflows executions should be filtered. (optional)
    embedded: true,
    // number | The id of an environment. (optional)
    environmentId: 789,
    // 'CREATED' | 'STARTED' | 'STOPPED' | 'FAILED' | 'COMPLETED' | The status of an executed job (optional)
    jobStatus: jobStatus_example,
    // Date | The start date of a job. (optional)
    jobStartDate: 2013-10-20T19:20:30+01:00,
    // Date | The end date of a job. (optional)
    jobEndDate: 2013-10-20T19:20:30+01:00,
    // number | The id of a project. (optional)
    projectId: 789,
    // number | The id of a project deployment. (optional)
    projectDeploymentId: 789,
    // string | The id of a workflow. (optional)
    workflowId: workflowId_example,
    // number | The number of the page to return. (optional)
    pageNumber: 56,
  } satisfies GetWorkflowExecutionsPageRequest;

  try {
    const data = await api.getWorkflowExecutionsPage(body);
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
| **id** | `number` | The id of a workspace. | [Defaults to `undefined`] |
| **embedded** | `boolean` | If embedded automation workflows executions should be filtered. | [Optional] [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Optional] [Defaults to `undefined`] |
| **jobStatus** | `CREATED`, `STARTED`, `STOPPED`, `FAILED`, `COMPLETED` | The status of an executed job | [Optional] [Defaults to `undefined`] [Enum: CREATED, STARTED, STOPPED, FAILED, COMPLETED] |
| **jobStartDate** | `Date` | The start date of a job. | [Optional] [Defaults to `undefined`] |
| **jobEndDate** | `Date` | The end date of a job. | [Optional] [Defaults to `undefined`] |
| **projectId** | `number` | The id of a project. | [Optional] [Defaults to `undefined`] |
| **projectDeploymentId** | `number` | The id of a project deployment. | [Optional] [Defaults to `undefined`] |
| **workflowId** | `string` | The id of a workflow. | [Optional] [Defaults to `undefined`] |
| **pageNumber** | `number` | The number of the page to return. | [Optional] [Defaults to `0`] |

### Return type

[**Page**](Page.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of workflow executions. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

