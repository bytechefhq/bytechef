# GitConfigurationApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getGitConfiguration**](GitConfigurationApi.md#getgitconfiguration) | **GET** /workspaces/{id}/git-configuration | Get git configuration of a workspace. |
| [**updateGitConfiguration**](GitConfigurationApi.md#updategitconfiguration) | **PUT** /workspaces/{id}/git-configuration | Update git configuration. |



## getGitConfiguration

> GitConfiguration getGitConfiguration(id)

Get git configuration of a workspace.

Get git configuration of a workspace.

### Example

```ts
import {
  Configuration,
  GitConfigurationApi,
} from '';
import type { GetGitConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new GitConfigurationApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
  } satisfies GetGitConfigurationRequest;

  try {
    const data = await api.getGitConfiguration(body);
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

[**GitConfiguration**](GitConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The git configuration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateGitConfiguration

> updateGitConfiguration(id, gitConfiguration)

Update git configuration.

Update git configuration.

### Example

```ts
import {
  Configuration,
  GitConfigurationApi,
} from '';
import type { UpdateGitConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new GitConfigurationApi();

  const body = {
    // number | The id of a workspace.
    id: 789,
    // GitConfiguration
    gitConfiguration: ...,
  } satisfies UpdateGitConfigurationRequest;

  try {
    const data = await api.updateGitConfiguration(body);
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
| **gitConfiguration** | [GitConfiguration](GitConfiguration.md) |  | |

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

