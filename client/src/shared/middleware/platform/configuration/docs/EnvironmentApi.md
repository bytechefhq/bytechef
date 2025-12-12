# EnvironmentApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getEnvironments**](EnvironmentApi.md#getenvironments) | **GET** /environments | Retrieves oauth2 authorization parameters |



## getEnvironments

> Array&lt;Environment&gt; getEnvironments()

Retrieves oauth2 authorization parameters

Retrieves environments.

### Example

```ts
import {
  Configuration,
  EnvironmentApi,
} from '';
import type { GetEnvironmentsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new EnvironmentApi();

  try {
    const data = await api.getEnvironments();
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

[**Array&lt;Environment&gt;**](Environment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of Environment objects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

