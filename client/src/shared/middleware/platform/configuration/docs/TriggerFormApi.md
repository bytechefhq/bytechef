# TriggerFormApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getTriggerForm**](TriggerFormApi.md#gettriggerform) | **GET** /trigger-form/{id} | Get a trigger form |



## getTriggerForm

> TriggerForm getTriggerForm(id)

Get a trigger form

Get a trigger form.

### Example

```ts
import {
  Configuration,
  TriggerFormApi,
} from '';
import type { GetTriggerFormRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TriggerFormApi();

  const body = {
    // string | The id of a trigger form.
    id: id_example,
  } satisfies GetTriggerFormRequest;

  try {
    const data = await api.getTriggerForm(body);
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
| **id** | `string` | The id of a trigger form. | [Defaults to `undefined`] |

### Return type

[**TriggerForm**](TriggerForm.md)

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

