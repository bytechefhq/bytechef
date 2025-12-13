# TaskDispatcherDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getTaskDispatcherDefinition**](TaskDispatcherDefinitionApi.md#gettaskdispatcherdefinition) | **GET** /task-dispatcher-definitions/{taskDispatcherName}/{taskDispatcherVersion} | Get a task dispatcher definition |
| [**getTaskDispatcherDefinitionVersions**](TaskDispatcherDefinitionApi.md#gettaskdispatcherdefinitionversions) | **GET** /task-dispatcher-definitions/{taskDispatcherName}/versions | Get all task dispatcher definition versions of a task dispatcher |
| [**getTaskDispatcherDefinitions**](TaskDispatcherDefinitionApi.md#gettaskdispatcherdefinitions) | **GET** /task-dispatcher-definitions | Get all task dispatcher definitions |



## getTaskDispatcherDefinition

> TaskDispatcherDefinition getTaskDispatcherDefinition(taskDispatcherName, taskDispatcherVersion)

Get a task dispatcher definition

Get a task dispatcher definition.

### Example

```ts
import {
  Configuration,
  TaskDispatcherDefinitionApi,
} from '';
import type { GetTaskDispatcherDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TaskDispatcherDefinitionApi();

  const body = {
    // string | The name of a task dispatcher to get.
    taskDispatcherName: taskDispatcherName_example,
    // number | The version of a task dispatcher to get.
    taskDispatcherVersion: 56,
  } satisfies GetTaskDispatcherDefinitionRequest;

  try {
    const data = await api.getTaskDispatcherDefinition(body);
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
| **taskDispatcherName** | `string` | The name of a task dispatcher to get. | [Defaults to `undefined`] |
| **taskDispatcherVersion** | `number` | The version of a task dispatcher to get. | [Defaults to `undefined`] |

### Return type

[**TaskDispatcherDefinition**](TaskDispatcherDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTaskDispatcherDefinitionVersions

> Array&lt;TaskDispatcherDefinitionBasic&gt; getTaskDispatcherDefinitionVersions(taskDispatcherName)

Get all task dispatcher definition versions of a task dispatcher

Get all task dispatcher definition versions of a task dispatcher.

### Example

```ts
import {
  Configuration,
  TaskDispatcherDefinitionApi,
} from '';
import type { GetTaskDispatcherDefinitionVersionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TaskDispatcherDefinitionApi();

  const body = {
    // string | The name of a task dispatcher.
    taskDispatcherName: taskDispatcherName_example,
  } satisfies GetTaskDispatcherDefinitionVersionsRequest;

  try {
    const data = await api.getTaskDispatcherDefinitionVersions(body);
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
| **taskDispatcherName** | `string` | The name of a task dispatcher. | [Defaults to `undefined`] |

### Return type

[**Array&lt;TaskDispatcherDefinitionBasic&gt;**](TaskDispatcherDefinitionBasic.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTaskDispatcherDefinitions

> Array&lt;TaskDispatcherDefinition&gt; getTaskDispatcherDefinitions()

Get all task dispatcher definitions

Get all task dispatcher definitions.

### Example

```ts
import {
  Configuration,
  TaskDispatcherDefinitionApi,
} from '';
import type { GetTaskDispatcherDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TaskDispatcherDefinitionApi();

  try {
    const data = await api.getTaskDispatcherDefinitions();
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

[**Array&lt;TaskDispatcherDefinition&gt;**](TaskDispatcherDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

