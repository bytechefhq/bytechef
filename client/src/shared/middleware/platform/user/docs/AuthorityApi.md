# AuthorityApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAuthorities**](AuthorityApi.md#getauthorities) | **GET** /authorities | Get all authorities |



## getAuthorities

> Array&lt;Authority&gt; getAuthorities()

Get all authorities

Get all authorities.

### Example

```ts
import {
  Configuration,
  AuthorityApi,
} from '';
import type { GetAuthoritiesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AuthorityApi();

  try {
    const data = await api.getAuthorities();
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

[**Array&lt;Authority&gt;**](Authority.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of authorities. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

