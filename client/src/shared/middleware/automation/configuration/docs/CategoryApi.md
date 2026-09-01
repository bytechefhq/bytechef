# CategoryApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getProjectCategories**](CategoryApi.md#getprojectcategories) | **GET** /workspaces/{id}/project-categories | Get project categories |



## getProjectCategories

> Array&lt;Category&gt; getProjectCategories(requestParameters: GetProjectCategoriesRequest)

Get project categories

Get project categories for a workspace.

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

  const requestParameters: GetProjectCategoriesRequest = {
    // The id of a workspace.
    id: 789,
  };

  try {
    const data = await api.getProjectCategories(requestParameters);
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
| ------------- | ------------- | ------------- | ------------- |
| **id** | **number** | The id of a workspace. | defaults to undefined |

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

