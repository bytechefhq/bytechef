# WorkspaceApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createWorkspace**](WorkspaceApi.md#createworkspace) | **POST** /workspaces | Create a new workspace |
| [**deleteWorkspace**](WorkspaceApi.md#deleteworkspace) | **DELETE** /workspaces/{id} | Delete a workspace |
| [**getWorkspace**](WorkspaceApi.md#getworkspace) | **GET** /workspaces/{id} | Get a workspace by id |
| [**getWorkspaces**](WorkspaceApi.md#getworkspaces) | **GET** /workspaces | Get workspaces |
| [**updateWorkspace**](WorkspaceApi.md#updateworkspace) | **PUT** /workspaces/{id} | Update an existing workspace |



## createWorkspace

> Workspace createWorkspace(workspace)

Create a new workspace

Create a workspace event.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { CreateWorkspaceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  const body = {
    // Workspace
    workspace: ...,
  } satisfies CreateWorkspaceRequest;

  try {
    const data = await api.createWorkspace(body);
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
| **workspace** | [Workspace](Workspace.md) |  | |

### Return type

[**Workspace**](Workspace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workspace object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteWorkspace

> deleteWorkspace(id)

Delete a workspace

Delete a workspace.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { DeleteWorkspaceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
  } satisfies DeleteWorkspaceRequest;

  try {
    const data = await api.deleteWorkspace(body);
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


## getWorkspace

> Workspace getWorkspace(id)

Get a workspace by id

Get a workspace by id.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { GetWorkspaceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
  } satisfies GetWorkspaceRequest;

  try {
    const data = await api.getWorkspace(body);
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

### Return type

[**Workspace**](Workspace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workspace object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkspaces

> Array&lt;Workspace&gt; getWorkspaces()

Get workspaces

Get workspaces.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { GetWorkspacesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  try {
    const data = await api.getWorkspaces();
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

[**Array&lt;Workspace&gt;**](Workspace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of workspaces. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateWorkspace

> Workspace updateWorkspace(id, workspace)

Update an existing workspace

Update an existing workspace.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { UpdateWorkspaceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // Workspace
    workspace: ...,
  } satisfies UpdateWorkspaceRequest;

  try {
    const data = await api.updateWorkspace(body);
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
| **workspace** | [Workspace](Workspace.md) |  | |

### Return type

[**Workspace**](Workspace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workspace object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

