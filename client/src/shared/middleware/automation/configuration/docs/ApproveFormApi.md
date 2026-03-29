# ApproveFormApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getApproveForm**](ApproveFormApi.md#getapproveform) | **GET** /approve-form/{id} | Get an approve form |



## getApproveForm

> ApproveForm getApproveForm(id)

Get an approve form

Get an approve form.

### Example

```ts
import {
  Configuration,
  ApproveFormApi,
} from '';
import type { GetApproveFormRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApproveFormApi();

  const body = {
    // string | The id of an approve form.
    id: id_example,
  } satisfies GetApproveFormRequest;

  try {
    const data = await api.getApproveForm(body);
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
| **id** | `string` | The id of an approve form. | [Defaults to `undefined`] |

### Return type

[**ApproveForm**](ApproveForm.md)

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

