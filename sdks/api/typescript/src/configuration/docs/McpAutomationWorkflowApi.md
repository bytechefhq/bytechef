# McpAutomationWorkflowApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**disableFrontendMcpAutomationWorkflow**](McpAutomationWorkflowApi.md#disablefrontendmcpautomationworkflow) | **DELETE** /automation/workflows/{workflowUuid}/mcp/enable | Turn off the workflow\&#39;s availability to AI agents |
| [**disableMcpAutomationWorkflow**](McpAutomationWorkflowApi.md#disablemcpautomationworkflow) | **DELETE** /{externalUserId}/automation/workflows/{workflowUuid}/mcp/enable | Turn off the workflow\&#39;s availability to AI agents |
| [**enableFrontendMcpAutomationWorkflow**](McpAutomationWorkflowApi.md#enablefrontendmcpautomationworkflow) | **POST** /automation/workflows/{workflowUuid}/mcp/enable | Turn on the workflow\&#39;s availability to AI agents |
| [**enableMcpAutomationWorkflow**](McpAutomationWorkflowApi.md#enablemcpautomationworkflow) | **POST** /{externalUserId}/automation/workflows/{workflowUuid}/mcp/enable | Turn on the workflow\&#39;s availability to AI agents |



## disableFrontendMcpAutomationWorkflow

> disableFrontendMcpAutomationWorkflow(workflowUuid, xEnvironment)

Turn off the workflow\&#39;s availability to AI agents

Turn off the workflow\&#39;s availability to AI agents on every MCP server that offers it to the user.

### Example

```ts
import {
  Configuration,
  McpAutomationWorkflowApi,
} from '';
import type { DisableFrontendMcpAutomationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new McpAutomationWorkflowApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DisableFrontendMcpAutomationWorkflowRequest;

  try {
    const data = await api.disableFrontendMcpAutomationWorkflow(body);
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

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The user has no reference for the workflow. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## disableMcpAutomationWorkflow

> disableMcpAutomationWorkflow(externalUserId, workflowUuid, xEnvironment)

Turn off the workflow\&#39;s availability to AI agents

Turn off the workflow\&#39;s availability to AI agents on every MCP server that offers it to the user.

### Example

```ts
import {
  Configuration,
  McpAutomationWorkflowApi,
} from '';
import type { DisableMcpAutomationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new McpAutomationWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DisableMcpAutomationWorkflowRequest;

  try {
    const data = await api.disableMcpAutomationWorkflow(body);
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

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The user has no reference for the workflow. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableFrontendMcpAutomationWorkflow

> enableFrontendMcpAutomationWorkflow(workflowUuid, xEnvironment)

Turn on the workflow\&#39;s availability to AI agents

Turn on the workflow\&#39;s availability to AI agents on every MCP server that offers it to the user.

### Example

```ts
import {
  Configuration,
  McpAutomationWorkflowApi,
} from '';
import type { EnableFrontendMcpAutomationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new McpAutomationWorkflowApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies EnableFrontendMcpAutomationWorkflowRequest;

  try {
    const data = await api.enableFrontendMcpAutomationWorkflow(body);
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

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The user has no reference for the workflow. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableMcpAutomationWorkflow

> enableMcpAutomationWorkflow(externalUserId, workflowUuid, xEnvironment)

Turn on the workflow\&#39;s availability to AI agents

Turn on the workflow\&#39;s availability to AI agents on every MCP server that offers it to the user.

### Example

```ts
import {
  Configuration,
  McpAutomationWorkflowApi,
} from '';
import type { EnableMcpAutomationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new McpAutomationWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies EnableMcpAutomationWorkflowRequest;

  try {
    const data = await api.enableMcpAutomationWorkflow(body);
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

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **404** | The user has no reference for the workflow. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

