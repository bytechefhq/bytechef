# WebhookTriggerTestApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**startWebhookTriggerTest**](WebhookTriggerTestApi.md#startwebhooktriggertest) | **POST** /webhooks/{workflowId}/test/start | Test a webhook trigger |
| [**stopWebhookTriggerTest**](WebhookTriggerTestApi.md#stopwebhooktriggertest) | **POST** /webhooks/{workflowId}/test/stop | Test a webhook trigger |



## startWebhookTriggerTest

> StartWebhookTriggerTest200Response startWebhookTriggerTest(workflowId, environmentId)

Test a webhook trigger

Test a webhook trigger.

### Example

```ts
import {
  Configuration,
  WebhookTriggerTestApi,
} from '';
import type { StartWebhookTriggerTestRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WebhookTriggerTestApi();

  const body = {
    // string | The id of a workflow.
    workflowId: workflowId_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies StartWebhookTriggerTestRequest;

  try {
    const data = await api.startWebhookTriggerTest(body);
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
| **workflowId** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**StartWebhookTriggerTest200Response**](StartWebhookTriggerTest200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns the webhook URL. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## stopWebhookTriggerTest

> stopWebhookTriggerTest(workflowId, environmentId)

Test a webhook trigger

Test a webhook trigger.

### Example

```ts
import {
  Configuration,
  WebhookTriggerTestApi,
} from '';
import type { StopWebhookTriggerTestRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WebhookTriggerTestApi();

  const body = {
    // string | The id of a workflow.
    workflowId: workflowId_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies StopWebhookTriggerTestRequest;

  try {
    const data = await api.stopWebhookTriggerTest(body);
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
| **workflowId** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

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

