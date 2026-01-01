# CategoryApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getProjectCategories**](CategoryApi.md#getprojectcategories) | **GET** /projects/categories | Get categories |



## getProjectCategories

> Array&lt;Category&gt; getProjectCategories()

Get categories

Get categories.

### Example

```ts
import {
  Configuration,
  CategoryApi,
} from '';
import type { GetProjectCategoriesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CategoryApi();

  try {
    const data = await api.getProjectCategories();
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

