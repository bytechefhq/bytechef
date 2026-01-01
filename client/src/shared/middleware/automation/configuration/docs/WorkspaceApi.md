# WorkspaceApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getUserWorkspaces**](WorkspaceApi.md#getuserworkspaces) | **GET** /users/{id}/workspaces | Get all user workspaces |



## getUserWorkspaces

> Array&lt;Workspace&gt; getUserWorkspaces(id)

Get all user workspaces

Get all user workspaces.

### Example

```ts
import {
  Configuration,
  WorkspaceApi,
} from '';
import type { GetUserWorkspacesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkspaceApi();

  const body = {
    // number | The id of a user.
    id: 789,
  } satisfies GetUserWorkspacesRequest;

  try {
    const data = await api.getUserWorkspaces(body);
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
| **id** | `number` | The id of a user. | [Defaults to `undefined`] |

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
| **200** | The list of user workspaces. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

