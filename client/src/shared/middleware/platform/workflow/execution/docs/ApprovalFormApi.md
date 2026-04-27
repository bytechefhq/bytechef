# ApprovalFormApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getApprovalForm**](ApprovalFormApi.md#getapprovalform) | **GET** /approval-form/{id} | Get an approval form |



## getApprovalForm

> ApprovalForm getApprovalForm(id)

Get an approval form

Get an approval form.

### Example

```ts
import {
  Configuration,
  ApprovalFormApi,
} from '';
import type { GetApprovalFormRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ApprovalFormApi();

  const body = {
    // string | The id of an approval form.
    id: id_example,
  } satisfies GetApprovalFormRequest;

  try {
    const data = await api.getApprovalForm(body);
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
| **id** | `string` | The id of an approval form. | [Defaults to `undefined`] |

### Return type

[**ApprovalForm**](ApprovalForm.md)

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

