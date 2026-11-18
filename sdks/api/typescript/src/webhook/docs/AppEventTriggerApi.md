# AppEventTriggerApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**executeFrontendWorkflows**](AppEventTriggerApi.md#executefrontendworkflows) | **POST** /app-events | Trigger all subscribed workflows |
| [**executeWorkflows**](AppEventTriggerApi.md#executeworkflows) | **POST** /{externalUserId}/app-events | Trigger all subscribed workflows |



## executeFrontendWorkflows

> executeFrontendWorkflows(xEnvironment)

Trigger all subscribed workflows

Fires an App Event for the connected user, starting every one of their enabled workflows that has an App Event trigger. Returns no body; inspect the runs through the workflow executions endpoints. The request body is read once and delivered to every matching workflow. If one or more workflows could not be started, the others still run and the response is a 500 problem detail whose failedWorkflows property lists the uuids of the workflows that were not started.

### Example

```ts
import {
  Configuration,
  AppEventTriggerApi,
} from '';
import type { ExecuteFrontendWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AppEventTriggerApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ExecuteFrontendWorkflowsRequest;

  try {
    const data = await api.executeFrontendWorkflows(body);
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

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
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


## executeWorkflows

> executeWorkflows(externalUserId, xEnvironment)

Trigger all subscribed workflows

Fires an App Event for the connected user identified by external user id, starting every one of their enabled workflows that has an App Event trigger. Returns no body; inspect the runs through the workflow executions endpoints. The request body is read once and delivered to every matching workflow. If one or more workflows could not be started, the others still run and the response is a 500 problem detail whose failedWorkflows property lists the uuids of the workflows that were not started.

### Example

```ts
import {
  Configuration,
  AppEventTriggerApi,
} from '';
import type { ExecuteWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AppEventTriggerApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ExecuteWorkflowsRequest;

  try {
    const data = await api.executeWorkflows(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
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

