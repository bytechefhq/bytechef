# ToolApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**executeTool**](ToolApi.md#executetooloperation) | **POST** /{externalUserId}/tools | Executes a tool and returns the result as the response |
| [**getTools**](ToolApi.md#gettools) | **GET** /{externalUserId}/tools | Returns a list with all the available tools |



## executeTool

> object executeTool(externalUserId, executeToolRequest, xEnvironment, xInstanceId)

Executes a tool and returns the result as the response

Executes a tool and returns the result as the response.

### Example

```ts
import {
  Configuration,
  ToolApi,
} from '';
import type { ExecuteToolOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ToolApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // ExecuteToolRequest
    executeToolRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // number | The integration instance Id that uniquely identifies the connected user\'s specific integration instance (optional)
    xInstanceId: 1234,
  } satisfies ExecuteToolOperationRequest;

  try {
    const data = await api.executeTool(body);
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
| **executeToolRequest** | [ExecuteToolRequest](ExecuteToolRequest.md) |  | |
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
| **200** | The provider\&#39;s response\&#39;. |  -  |
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


## getTools

> { [key: string]: Array&lt;Tool&gt;; } getTools(externalUserId, xEnvironment, categories, components, tools)

Returns a list with all the available tools

Returns a list with all the available tools.

### Example

```ts
import {
  Configuration,
  ToolApi,
} from '';
import type { GetToolsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ToolApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // Array<string> | Filter by specific integration categories, depending on the integrations the user has connected. (optional)
    categories: ...,
    // Array<string> | Filter by specific components, depending on the integrations the user has connected. (optional)
    components: ...,
    // Array<string> | Filter by specific tools, depending on the integrations the user has connected. (optional)
    tools: ...,
  } satisfies GetToolsRequest;

  try {
    const data = await api.getTools(body);
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
| **categories** | `Array<string>` | Filter by specific integration categories, depending on the integrations the user has connected. | [Optional] |
| **components** | `Array<string>` | Filter by specific components, depending on the integrations the user has connected. | [Optional] |
| **tools** | `Array<string>` | Filter by specific tools, depending on the integrations the user has connected. | [Optional] |

### Return type

**{ [key: string]: Array<Tool>; }**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth), [bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of tool objects. |  -  |
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

