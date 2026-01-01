# ProjectApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createProject**](ProjectApi.md#createproject) | **POST** /projects | Create a new project. |
| [**deleteProject**](ProjectApi.md#deleteproject) | **DELETE** /projects/{id} | Delete a project. |
| [**duplicateProject**](ProjectApi.md#duplicateproject) | **POST** /projects/{id}/duplicate | Duplicates existing project. |
| [**exportProject**](ProjectApi.md#exportproject) | **GET** /projects/{id}/export | Export project. |
| [**getProject**](ProjectApi.md#getproject) | **GET** /projects/{id} | Get a project by id. |
| [**getProjectVersions**](ProjectApi.md#getprojectversions) | **GET** /projects/{id}/versions | Get a project versions. |
| [**getWorkspaceProjects**](ProjectApi.md#getworkspaceprojects) | **GET** /workspaces/{id}/projects | Get projects by workspace id |
| [**importProject**](ProjectApi.md#importproject) | **POST** /workspaces/{workspaceId}/projects/import | Import project. |
| [**publishProject**](ProjectApi.md#publishprojectoperation) | **POST** /projects/{id}/publish | Publishes existing project. |
| [**updateProject**](ProjectApi.md#updateproject) | **PUT** /projects/{id} | Update an existing project. |



## createProject

> number createProject(project)

Create a new project.

Create a new project.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { CreateProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // Project
    project: ...,
  } satisfies CreateProjectRequest;

  try {
    const data = await api.createProject(body);
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
| **project** | [Project](Project.md) |  | |

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
| **200** | The project id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteProject

> deleteProject(id)

Delete a project.

Delete a project.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { DeleteProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies DeleteProjectRequest;

  try {
    const data = await api.deleteProject(body);
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


## duplicateProject

> Project duplicateProject(id)

Duplicates existing project.

Duplicates existing project.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { DuplicateProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies DuplicateProjectRequest;

  try {
    const data = await api.duplicateProject(body);
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

[**Project**](Project.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The duplicated project object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## exportProject

> Blob exportProject(id)

Export project.

Export project as a zip file.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { ExportProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies ExportProjectRequest;

  try {
    const data = await api.exportProject(body);
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

**Blob**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/octet-stream`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The project export file. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProject

> Project getProject(id)

Get a project by id.

Get a project by id.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { GetProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies GetProjectRequest;

  try {
    const data = await api.getProject(body);
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

[**Project**](Project.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The project object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectVersions

> Array&lt;ProjectVersion&gt; getProjectVersions(id)

Get a project versions.

Get a project versions.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { GetProjectVersionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies GetProjectVersionsRequest;

  try {
    const data = await api.getProjectVersions(body);
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

[**Array&lt;ProjectVersion&gt;**](ProjectVersion.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of project version objects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkspaceProjects

> Array&lt;Project&gt; getWorkspaceProjects(id, apiCollections, categoryId, includeAllFields, projectDeployments, status, tagId)

Get projects by workspace id

Get projects by workspace id.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { GetWorkspaceProjectsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // boolean | Use for filtering API Collection based projects. (optional)
    apiCollections: true,
    // number | The category id used for filtering projects. (optional)
    categoryId: 789,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
    // boolean | Use for filtering projects for which project deployments exist. (optional)
    projectDeployments: true,
    // ProjectStatus | Use for filtering projects per status. (optional)
    status: ...,
    // number | The tag id of used for filtering projects. (optional)
    tagId: 789,
  } satisfies GetWorkspaceProjectsRequest;

  try {
    const data = await api.getWorkspaceProjects(body);
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
| **apiCollections** | `boolean` | Use for filtering API Collection based projects. | [Optional] [Defaults to `undefined`] |
| **categoryId** | `number` | The category id used for filtering projects. | [Optional] [Defaults to `undefined`] |
| **includeAllFields** | `boolean` | Use for including all fields or just basic ones. | [Optional] [Defaults to `true`] |
| **projectDeployments** | `boolean` | Use for filtering projects for which project deployments exist. | [Optional] [Defaults to `undefined`] |
| **status** | `ProjectStatus` | Use for filtering projects per status. | [Optional] [Defaults to `undefined`] [Enum: DRAFT, PUBLISHED] |
| **tagId** | `number` | The tag id of used for filtering projects. | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Project&gt;**](Project.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of projects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## importProject

> number importProject(workspaceId, file)

Import project.

Import project from a zip file.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { ImportProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of the workspace.
    workspaceId: 789,
    // Blob
    file: BINARY_DATA_HERE,
  } satisfies ImportProjectRequest;

  try {
    const data = await api.importProject(body);
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
| **workspaceId** | `number` | The id of the workspace. | [Defaults to `undefined`] |
| **file** | `Blob` |  | [Defaults to `undefined`] |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The imported project id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishProject

> publishProject(id, publishProjectRequest)

Publishes existing project.

Publishes existing project.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { PublishProjectOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // PublishProjectRequest (optional)
    publishProjectRequest: ...,
  } satisfies PublishProjectOperationRequest;

  try {
    const data = await api.publishProject(body);
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
| **publishProjectRequest** | [PublishProjectRequest](PublishProjectRequest.md) |  | [Optional] |

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


## updateProject

> updateProject(id, project)

Update an existing project.

Update an existing project.

### Example

```ts
import {
  Configuration,
  ProjectApi,
} from '';
import type { UpdateProjectRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // Project
    project: ...,
  } satisfies UpdateProjectRequest;

  try {
    const data = await api.updateProject(body);
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
| **project** | [Project](Project.md) |  | |

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

