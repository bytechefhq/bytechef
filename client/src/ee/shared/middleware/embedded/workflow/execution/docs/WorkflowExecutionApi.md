# WorkflowExecutionApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getWorkflowExecution**](WorkflowExecutionApi.md#getworkflowexecution) | **GET** /workflow-executions/{id} | Get workflow executions by id |
| [**getWorkflowExecutionTaskExecution**](WorkflowExecutionApi.md#getworkflowexecutiontaskexecution) | **GET** /workflow-executions/{id}/task-executions/{taskExecutionId} | Get a task execution by id |
| [**getWorkflowExecutionTaskExecutionChildren**](WorkflowExecutionApi.md#getworkflowexecutiontaskexecutionchildren) | **GET** /workflow-executions/{id}/task-executions/{taskExecutionId}/children | Get a page of task execution children |
| [**getWorkflowExecutionsPage**](WorkflowExecutionApi.md#getworkflowexecutionspage) | **GET** /workflow-executions | Get Integration workflow executions |



## getWorkflowExecution

> WorkflowExecution getWorkflowExecution(id, lazy)

Get workflow executions by id

Get workflow execution by id.

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
    // boolean | Whether to return only the top-level task executions, with their child counts, the focus path and the latest-attempt task execution count. Child jobs of subflow tasks are returned without their task executions. (optional)
    lazy: true,
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
| **lazy** | `boolean` | Whether to return only the top-level task executions, with their child counts, the focus path and the latest-attempt task execution count. Child jobs of subflow tasks are returned without their task executions. | [Optional] [Defaults to `false`] |

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


## getWorkflowExecutionTaskExecution

> TaskExecution getWorkflowExecutionTaskExecution(id, taskExecutionId)

Get a task execution by id

Get a single task execution\&#39;s input and output by id.

### Example

```ts
import {
  Configuration,
  WorkflowExecutionApi,
} from '';
import type { GetWorkflowExecutionTaskExecutionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowExecutionApi();

  const body = {
    // number | The id of an execution.
    id: 789,
    // number | The id of a task execution.
    taskExecutionId: 789,
  } satisfies GetWorkflowExecutionTaskExecutionRequest;

  try {
    const data = await api.getWorkflowExecutionTaskExecution(body);
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
| **taskExecutionId** | `number` | The id of a task execution. | [Defaults to `undefined`] |

### Return type

[**TaskExecution**](TaskExecution.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The task execution object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowExecutionTaskExecutionChildren

> TaskExecutionChildrenPage getWorkflowExecutionTaskExecutionChildren(id, taskExecutionId, pageNumber, pageSize)

Get a page of task execution children

Get a page of the children of a task execution of the execution or of one of its subflow jobs. A loop returns whole iterations, an on-error task returns its two branches on a single page.

### Example

```ts
import {
  Configuration,
  WorkflowExecutionApi,
} from '';
import type { GetWorkflowExecutionTaskExecutionChildrenRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowExecutionApi();

  const body = {
    // number | The id of an execution.
    id: 789,
    // number | The id of a task execution.
    taskExecutionId: 789,
    // number | The zero-based number of the page to return. (optional)
    pageNumber: 56,
    // number | The number of children or loop iterations per page, from 1 to 200. An on-error task returns both of its branches on page 0 whatever the page size. (optional)
    pageSize: 56,
  } satisfies GetWorkflowExecutionTaskExecutionChildrenRequest;

  try {
    const data = await api.getWorkflowExecutionTaskExecutionChildren(body);
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
| **taskExecutionId** | `number` | The id of a task execution. | [Defaults to `undefined`] |
| **pageNumber** | `number` | The zero-based number of the page to return. | [Optional] [Defaults to `0`] |
| **pageSize** | `number` | The number of children or loop iterations per page, from 1 to 200. An on-error task returns both of its branches on page 0 whatever the page size. | [Optional] [Defaults to `50`] |

### Return type

[**TaskExecutionChildrenPage**](TaskExecutionChildrenPage.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of task execution children. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowExecutionsPage

> Page getWorkflowExecutionsPage(environmentId, jobStatus, jobStartDate, jobEndDate, integrationId, integrationInstanceConfigurationId, connectedUserId, workflowId, pageNumber)

Get Integration workflow executions

Get Integration workflow executions.

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
    // number | The id of an environment. (optional)
    environmentId: 789,
    // 'CREATED' | 'STARTED' | 'STOPPED' | 'FAILED' | 'COMPLETED' | The status of an executed job (optional)
    jobStatus: jobStatus_example,
    // Date | The start date of a job. (optional)
    jobStartDate: 2013-10-20T19:20:30+01:00,
    // Date | The end date of a job. (optional)
    jobEndDate: 2013-10-20T19:20:30+01:00,
    // number | The id of a Integration. (optional)
    integrationId: 789,
    // number | The id of a Integration instance configuration. (optional)
    integrationInstanceConfigurationId: 789,
    // number | The id of a connected user. (optional)
    connectedUserId: 789,
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
| **environmentId** | `number` | The id of an environment. | [Optional] [Defaults to `undefined`] |
| **jobStatus** | `CREATED`, `STARTED`, `STOPPED`, `FAILED`, `COMPLETED` | The status of an executed job | [Optional] [Defaults to `undefined`] [Enum: CREATED, STARTED, STOPPED, FAILED, COMPLETED] |
| **jobStartDate** | `Date` | The start date of a job. | [Optional] [Defaults to `undefined`] |
| **jobEndDate** | `Date` | The end date of a job. | [Optional] [Defaults to `undefined`] |
| **integrationId** | `number` | The id of a Integration. | [Optional] [Defaults to `undefined`] |
| **integrationInstanceConfigurationId** | `number` | The id of a Integration instance configuration. | [Optional] [Defaults to `undefined`] |
| **connectedUserId** | `number` | The id of a connected user. | [Optional] [Defaults to `undefined`] |
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

