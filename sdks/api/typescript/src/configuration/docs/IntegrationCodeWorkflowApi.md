# IntegrationCodeWorkflowApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployIntegrationCodeWorkflow**](IntegrationCodeWorkflowApi.md#deployintegrationcodeworkflow) | **POST** /integration-code-workflows/deploy | Deploy a code based integration |



## deployIntegrationCodeWorkflow

> deployIntegrationCodeWorkflow(xEnvironment, enable, integrationFile)

Deploy a code based integration

Deploy a code based integration. Unlike the connected-user-scoped endpoints on this API, this operation is authenticated as the API key\&#39;s own ByteChef user and does not fabricate a connected-user identity.

### Example

```ts
import {
  Configuration,
  IntegrationCodeWorkflowApi,
} from '';
import type { DeployIntegrationCodeWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationCodeWorkflowApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // boolean | Whether to make the published integration live in the environment: create and enable its instance configuration when it has none there, otherwise move the existing one to the published version and enable it. (optional)
    enable: true,
    // Blob | The file of a code-native integration. (optional)
    integrationFile: BINARY_DATA_HERE,
  } satisfies DeployIntegrationCodeWorkflowRequest;

  try {
    const data = await api.deployIntegrationCodeWorkflow(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **enable** | `boolean` | Whether to make the published integration live in the environment: create and enable its instance configuration when it has none there, otherwise move the existing one to the published version and enable it. | [Optional] [Defaults to `false`] |
| **integrationFile** | `Blob` | The file of a code-native integration. | [Optional] [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `multipart/form-data`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

