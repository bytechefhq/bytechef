# ProjectGitApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getProjectGitConfiguration**](ProjectGitApi.md#getprojectgitconfiguration) | **GET** /projects/{id}/project-git-configuration | Get git configuration of a project . |
| [**getProjectRemoteBranches**](ProjectGitApi.md#getprojectremotebranches) | **GET** /projects/{id}/git/remote-branches | Get remote branches of a project git repository. |
| [**getWorkspaceProjectGitConfigurations**](ProjectGitApi.md#getworkspaceprojectgitconfigurations) | **GET** /workspaces/{id}/project-git-configurations | Get project git configurations of a workspace. |
| [**pullProjectFromGit**](ProjectGitApi.md#pullprojectfromgit) | **POST** /projects/{id}/git/pull | Pulls project from git repository. |
| [**updateProjectGitConfiguration**](ProjectGitApi.md#updateprojectgitconfiguration) | **PUT** /projects/{id}/project-git-configuration | Update git configuration of an existing project. |



## getProjectGitConfiguration

> ProjectGitConfiguration getProjectGitConfiguration(id)

Get git configuration of a project .

Get git configuration of a project.

### Example

```ts
import {
  Configuration,
  ProjectGitApi,
} from '';
import type { GetProjectGitConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectGitApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies GetProjectGitConfigurationRequest;

  try {
    const data = await api.getProjectGitConfiguration(body);
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

[**ProjectGitConfiguration**](ProjectGitConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The project git configuration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectRemoteBranches

> Array&lt;string&gt; getProjectRemoteBranches(id)

Get remote branches of a project git repository.

Get remote branches of a project git repository.

### Example

```ts
import {
  Configuration,
  ProjectGitApi,
} from '';
import type { GetProjectRemoteBranchesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectGitApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies GetProjectRemoteBranchesRequest;

  try {
    const data = await api.getProjectRemoteBranches(body);
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

**Array<string>**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of remote branches. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkspaceProjectGitConfigurations

> Array&lt;ProjectGitConfiguration&gt; getWorkspaceProjectGitConfigurations(id)

Get project git configurations of a workspace.

Get project git configurations of a workspace.

### Example

```ts
import {
  Configuration,
  ProjectGitApi,
} from '';
import type { GetWorkspaceProjectGitConfigurationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectGitApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
  } satisfies GetWorkspaceProjectGitConfigurationsRequest;

  try {
    const data = await api.getWorkspaceProjectGitConfigurations(body);
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

[**Array&lt;ProjectGitConfiguration&gt;**](ProjectGitConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of workspace project git configuration objects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## pullProjectFromGit

> pullProjectFromGit(id)

Pulls project from git repository.

Pulls project from git repository.

### Example

```ts
import {
  Configuration,
  ProjectGitApi,
} from '';
import type { PullProjectFromGitRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectGitApi();

  const body = {
    // number | The id of a project.
    id: 789,
  } satisfies PullProjectFromGitRequest;

  try {
    const data = await api.pullProjectFromGit(body);
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


## updateProjectGitConfiguration

> updateProjectGitConfiguration(id, projectGitConfiguration)

Update git configuration of an existing project.

Update git configuration of an existing project.

### Example

```ts
import {
  Configuration,
  ProjectGitApi,
} from '';
import type { UpdateProjectGitConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ProjectGitApi();

  const body = {
    // number | The id of a project.
    id: 789,
    // ProjectGitConfiguration
    projectGitConfiguration: ...,
  } satisfies UpdateProjectGitConfigurationRequest;

  try {
    const data = await api.updateProjectGitConfiguration(body);
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
| **projectGitConfiguration** | [ProjectGitConfiguration](ProjectGitConfiguration.md) |  | |

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

