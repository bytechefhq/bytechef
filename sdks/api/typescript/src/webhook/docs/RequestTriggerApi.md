# RequestTriggerApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**executeFrontendWorkflow**](RequestTriggerApi.md#executefrontendworkflow) | **POST** /workflows/{workflowUuid} | Execute a workflow |
| [**executeWorkflow**](RequestTriggerApi.md#executeworkflow) | **POST** /{externalUserId}/workflows/{workflowUuid} | Execute a workflow |



## executeFrontendWorkflow

> object executeFrontendWorkflow(workflowUuid, xEnvironment)

Execute a workflow

Executes a single workflow through its Request trigger and returns the result the workflow produces.

### Example

```ts
import {
  Configuration,
  RequestTriggerApi,
} from '';
import type { ExecuteFrontendWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RequestTriggerApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ExecuteFrontendWorkflowRequest;

  try {
    const data = await api.executeFrontendWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The result returned by the workflow\&#39;s request trigger. |  -  |
| **400** | Bad request |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **404** | Not found |  -  |
| **409** | Conflict |  -  |
| **422** | Unprocessable entity |  -  |
| **499** | Remote provider error |  -  |
| **500** | Internal server error |  -  |
| **501** | Not implemented |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## executeWorkflow

> object executeWorkflow(externalUserId, workflowUuid, xEnvironment)

Execute a workflow

Executes a single workflow of the connected user identified by external user id through its Request trigger and returns the result the workflow produces.

### Example

```ts
import {
  Configuration,
  RequestTriggerApi,
} from '';
import type { ExecuteWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new RequestTriggerApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ExecuteWorkflowRequest;

  try {
    const data = await api.executeWorkflow(body);
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The result returned by the workflow\&#39;s request trigger. |  -  |
| **400** | Bad request |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **404** | Not found |  -  |
| **409** | Conflict |  -  |
| **422** | Unprocessable entity |  -  |
| **499** | Remote provider error |  -  |
| **500** | Internal server error |  -  |
| **501** | Not implemented |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

