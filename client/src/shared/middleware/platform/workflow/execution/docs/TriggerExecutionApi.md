# TriggerExecutionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getTriggerExecution**](TriggerExecutionApi.md#gettriggerexecution) | **GET** /trigger-executions/{id} | Get a trigger execution by id |
| [**getTriggerExecutionsPage**](TriggerExecutionApi.md#gettriggerexecutionspage) | **GET** /trigger-executions | Get a page of trigger execution |



## getTriggerExecution

> TriggerExecution getTriggerExecution(id)

Get a trigger execution by id

Get a trigger execution by id.

### Example

```ts
import {
  Configuration,
  TriggerExecutionApi,
} from '';
import type { GetTriggerExecutionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TriggerExecutionApi();

  const body = {
    // number | The id of a trigger execution to return.
    id: 789,
  } satisfies GetTriggerExecutionRequest;

  try {
    const data = await api.getTriggerExecution(body);
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
| **id** | `number` | The id of a trigger execution to return. | [Defaults to `undefined`] |

### Return type

[**TriggerExecution**](TriggerExecution.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The trigger execution object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTriggerExecutionsPage

> Page getTriggerExecutionsPage(pageNumber)

Get a page of trigger execution

Get a page of trigger execution.

### Example

```ts
import {
  Configuration,
  TriggerExecutionApi,
} from '';
import type { GetTriggerExecutionsPageRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TriggerExecutionApi();

  const body = {
    // number | The number of the page to return. (optional)
    pageNumber: 56,
  } satisfies GetTriggerExecutionsPageRequest;

  try {
    const data = await api.getTriggerExecutionsPage(body);
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
| **200** | The page of trigger executions. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

