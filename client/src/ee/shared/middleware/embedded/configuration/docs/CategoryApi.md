# CategoryApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getIntegrationCategories**](CategoryApi.md#getintegrationcategories) | **GET** /integrations/categories | Get integration categories |



## getIntegrationCategories

> Array&lt;Category&gt; getIntegrationCategories()

Get integration categories

Get integration categories.

### Example

```ts
import {
  Configuration,
  CategoryApi,
} from '';
import type { GetIntegrationCategoriesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CategoryApi();

  try {
    const data = await api.getIntegrationCategories();
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

[**Array&lt;Category&gt;**](Category.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of categories. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

