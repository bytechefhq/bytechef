# IntegrationCodeWorkflowApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployIntegration**](IntegrationCodeWorkflowApi.md#deployintegration) | **POST** /integrations/deploy | Deploy a new code based integration |



## deployIntegration

> deployIntegration(integrationFile)

Deploy a new code based integration

Deploy a new code based integration.

### Example

```ts
import {
  Configuration,
  IntegrationCodeWorkflowApi,
} from '';
import type { DeployIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationCodeWorkflowApi();

  const body = {
    // Blob | The file of a code-native integration. (optional)
    integrationFile: BINARY_DATA_HERE,
  } satisfies DeployIntegrationRequest;

  try {
    const data = await api.deployIntegration(body);
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
| **integrationFile** | `Blob` | The file of a code-native integration. | [Optional] [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

