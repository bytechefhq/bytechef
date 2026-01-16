# ProjectDeploymentApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createProjectDeployment**](ProjectDeploymentApi.md#createprojectdeployment) | **POST** /project-deployments | Create a new project deployment |
| [**createProjectDeploymentWorkflowJob**](ProjectDeploymentApi.md#createprojectdeploymentworkflowjob) | **POST** /project-deployments/{id}/workflows/{workflowId}/jobs | Create a request for running a new job |
| [**deleteProjectDeployment**](ProjectDeploymentApi.md#deleteprojectdeployment) | **DELETE** /project-deployments/{id} | Delete a project deployment |
| [**enableProjectDeployment**](ProjectDeploymentApi.md#enableprojectdeployment) | **PATCH** /project-deployments/{id}/enable/{enable} | Enable/disable a project deployment |
| [**enableProjectDeploymentWorkflow**](ProjectDeploymentApi.md#enableprojectdeploymentworkflow) | **PATCH** /project-deployments/{id}/workflows/{workflowId}/enable/{enable} | Enable/disable a workflow of a project deployment |
| [**getProjectDeployment**](ProjectDeploymentApi.md#getprojectdeployment) | **GET** /project-deployments/{id} | Get a project deployment by id |
| [**getWorkspaceProjectDeployments**](ProjectDeploymentApi.md#getworkspaceprojectdeployments) | **GET** /workspaces/{id}/project-deployments | Get project deployments |
| [**updateProjectDeployment**](ProjectDeploymentApi.md#updateprojectdeployment) | **PUT** /project-deployments/{id} | Update an existing project deployment |
| [**updateProjectDeploymentWorkflow**](ProjectDeploymentApi.md#updateprojectdeploymentworkflow) | **PUT** /project-deployments/{id}/project-deployment-workflows/{projectDeploymentWorkflowId} | Update an existing project deployment workflow |



## createProjectDeployment

> number createProjectDeployment(projectDeployment)

Create a new project deployment

Create a new project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { CreateProjectDeploymentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // ProjectDeployment
    projectDeployment: ...,
  } satisfies CreateProjectDeploymentRequest;

  try {
    const data = await api.createProjectDeployment(body);
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
| **projectDeployment** | [ProjectDeployment](ProjectDeployment.md) |  | |

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
| **200** | The project deployment id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createProjectDeploymentWorkflowJob

> CreateProjectDeploymentWorkflowJob200Response createProjectDeploymentWorkflowJob(id, workflowId)

Create a request for running a new job

Create a request for running a new job.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { CreateProjectDeploymentWorkflowJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // string | The id of the workflow to execute.
    workflowId: workflowId_example,
  } satisfies CreateProjectDeploymentWorkflowJobRequest;

  try {
    const data = await api.createProjectDeploymentWorkflowJob(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of the workflow to execute. | [Defaults to `undefined`] |

### Return type

[**CreateProjectDeploymentWorkflowJob200Response**](CreateProjectDeploymentWorkflowJob200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The id of a created job. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteProjectDeployment

> deleteProjectDeployment(id)

Delete a project deployment

Delete a project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { DeleteProjectDeploymentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
  } satisfies DeleteProjectDeploymentRequest;

  try {
    const data = await api.deleteProjectDeployment(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |

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


## enableProjectDeployment

> enableProjectDeployment(id, enable)

Enable/disable a project deployment

Enable/disable a project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { EnableProjectDeploymentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // boolean | Enable/disable the project deployment.
    enable: true,
  } satisfies EnableProjectDeploymentRequest;

  try {
    const data = await api.enableProjectDeployment(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the project deployment. | [Defaults to `undefined`] |

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


## enableProjectDeploymentWorkflow

> enableProjectDeploymentWorkflow(id, workflowId, enable)

Enable/disable a workflow of a project deployment

Enable/disable a workflow of a project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { EnableProjectDeploymentWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // string | The id of a project workflow.
    workflowId: workflowId_example,
    // boolean | Enable/disable the workflow of a project deployment.
    enable: true,
  } satisfies EnableProjectDeploymentWorkflowRequest;

  try {
    const data = await api.enableProjectDeploymentWorkflow(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of a project workflow. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the workflow of a project deployment. | [Defaults to `undefined`] |

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


## getProjectDeployment

> ProjectDeployment getProjectDeployment(id)

Get a project deployment by id

Get a project deployment by id.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { GetProjectDeploymentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
  } satisfies GetProjectDeploymentRequest;

  try {
    const data = await api.getProjectDeployment(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |

### Return type

[**ProjectDeployment**](ProjectDeployment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The project deployment object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkspaceProjectDeployments

> Array&lt;ProjectDeployment&gt; getWorkspaceProjectDeployments(id, environmentId, projectId, tagId, includeAllFields)

Get project deployments

Get project deployments.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { GetWorkspaceProjectDeploymentsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // number | The id of an environment. (optional)
    environmentId: 789,
    // number | The project ids used for filtering project deployments. (optional)
    projectId: 789,
    // number | The tag id of used for filtering project deployments. (optional)
    tagId: 789,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
  } satisfies GetWorkspaceProjectDeploymentsRequest;

  try {
    const data = await api.getWorkspaceProjectDeployments(body);
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
| **environmentId** | `number` | The id of an environment. | [Optional] [Defaults to `undefined`] |
| **projectId** | `number` | The project ids used for filtering project deployments. | [Optional] [Defaults to `undefined`] |
| **tagId** | `number` | The tag id of used for filtering project deployments. | [Optional] [Defaults to `undefined`] |
| **includeAllFields** | `boolean` | Use for including all fields or just basic ones. | [Optional] [Defaults to `true`] |

### Return type

[**Array&lt;ProjectDeployment&gt;**](ProjectDeployment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of project deployments. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateProjectDeployment

> updateProjectDeployment(id, projectDeployment)

Update an existing project deployment

Update an existing project deployment.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { UpdateProjectDeploymentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // ProjectDeployment
    projectDeployment: ...,
  } satisfies UpdateProjectDeploymentRequest;

  try {
    const data = await api.updateProjectDeployment(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
| **projectDeployment** | [ProjectDeployment](ProjectDeployment.md) |  | |

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


## updateProjectDeploymentWorkflow

> updateProjectDeploymentWorkflow(id, projectDeploymentWorkflowId, projectDeploymentWorkflow)

Update an existing project deployment workflow

Update an existing project deployment workflow.

### Example

```ts
import {
  Configuration,
  ProjectDeploymentApi,
} from '';
import type { UpdateProjectDeploymentWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectDeploymentApi();

  const body = {
    // number | The id of a project deployment.
    id: 789,
    // number | The id of a project deployment workflow.
    projectDeploymentWorkflowId: 789,
    // ProjectDeploymentWorkflow
    projectDeploymentWorkflow: ...,
  } satisfies UpdateProjectDeploymentWorkflowRequest;

  try {
    const data = await api.updateProjectDeploymentWorkflow(body);
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
| **id** | `number` | The id of a project deployment. | [Defaults to `undefined`] |
| **projectDeploymentWorkflowId** | `number` | The id of a project deployment workflow. | [Defaults to `undefined`] |
| **projectDeploymentWorkflow** | [ProjectDeploymentWorkflow](ProjectDeploymentWorkflow.md) |  | |

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

