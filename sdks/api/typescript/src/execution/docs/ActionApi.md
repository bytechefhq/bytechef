# ActionApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**executeAction**](ActionApi.md#executeactionoperation) | **POST** /{externalUserId}/components/{componentName}/versions/{componentVersion}/actions/{actionName} | Executes an action of a component and returns the result as the response |



## executeAction

> object executeAction(externalUserId, componentName, componentVersion, actionName, executeActionRequest, xEnvironment, xInstanceId)

Executes an action of a component and returns the result as the response

Executes an action of a component and returns the result as the response.

### Example

```ts
import {
  Configuration,
  ActionApi,
} from '';
import type { ExecuteActionOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActionApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The component name.
    componentName: componentName_example,
    // number | The component version.
    componentVersion: 56,
    // string | The name of the action to call.
    actionName: actionName_example,
    // ExecuteActionRequest
    executeActionRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // number | The integration instance Id that uniquely identifies the connected user\'s specific integration instance (optional)
    xInstanceId: 1234,
  } satisfies ExecuteActionOperationRequest;

  try {
    const data = await api.executeAction(body);
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
| **componentName** | `string` | The component name. | [Defaults to `undefined`] |
| **componentVersion** | `number` | The component version. | [Defaults to `undefined`] |
| **actionName** | `string` | The name of the action to call. | [Defaults to `undefined`] |
| **executeActionRequest** | [ExecuteActionRequest](ExecuteActionRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **xInstanceId** | `number` | The integration instance Id that uniquely identifies the connected user\&#39;s specific integration instance | [Optional] [Defaults to `undefined`] |

### Return type

**object**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth), [bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of component action objects. |  -  |
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

