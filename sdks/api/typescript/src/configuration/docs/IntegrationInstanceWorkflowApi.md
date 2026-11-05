# IntegrationInstanceWorkflowApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**disableFrontendIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#disablefrontendintegrationinstanceworkflow) | **DELETE** /integration-instances/{id}/workflows/{workflowUuid}/enable | Disable a workflow |
| [**disableIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#disableintegrationinstanceworkflow) | **DELETE** /{externalUserId}/integration-instances/{id}/workflows/{workflowUuid}/enable | Disable a workflow |
| [**enableFrontendIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#enablefrontendintegrationinstanceworkflow) | **POST** /integration-instances/{id}/workflows/{workflowUuid}/enable | Enable a workflow |
| [**enableIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#enableintegrationinstanceworkflow) | **POST** /{externalUserId}/integration-instances/{id}/workflows/{workflowUuid}/enable | Enable a workflow |
| [**getComponentInputOptions**](IntegrationInstanceWorkflowApi.md#getcomponentinputoptions) | **POST** /{externalUserId}/integration-instances/{id}/component-input-options | Get component input options |
| [**getFrontendComponentInputOptions**](IntegrationInstanceWorkflowApi.md#getfrontendcomponentinputoptions) | **POST** /integration-instances/{id}/component-input-options | Get component input options |
| [**updateFrontendIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#updatefrontendintegrationinstanceworkflowoperation) | **PUT** /integration-instances/{id}/workflows/{workflowUuid} | Update integration instance workflow |
| [**updateIntegrationInstanceWorkflow**](IntegrationInstanceWorkflowApi.md#updateintegrationinstanceworkflow) | **PUT** /{externalUserId}/integration-instances/{id}/workflows/{workflowUuid} | Update integration instance workflow |



## disableFrontendIntegrationInstanceWorkflow

> disableFrontendIntegrationInstanceWorkflow(id, workflowUuid)

Disable a workflow

Disable a workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { DisableFrontendIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
  } satisfies DisableFrontendIntegrationInstanceWorkflowRequest;

  try {
    const data = await api.disableFrontendIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |

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
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## disableIntegrationInstanceWorkflow

> disableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid)

Disable a workflow

Disable a workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { DisableIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
  } satisfies DisableIntegrationInstanceWorkflowRequest;

  try {
    const data = await api.disableIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |

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
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableFrontendIntegrationInstanceWorkflow

> enableFrontendIntegrationInstanceWorkflow(id, workflowUuid)

Enable a workflow

Enable a workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { EnableFrontendIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
  } satisfies EnableFrontendIntegrationInstanceWorkflowRequest;

  try {
    const data = await api.enableFrontendIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |

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
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableIntegrationInstanceWorkflow

> enableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid)

Enable a workflow

Enable a workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { EnableIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
  } satisfies EnableIntegrationInstanceWorkflowRequest;

  try {
    const data = await api.enableIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |

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
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getComponentInputOptions

> Array&lt;Option&gt; getComponentInputOptions(externalUserId, id, componentInputOptionsRequest)

Get component input options

Resolve dynamic option values for a component-defined workflow input property.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { GetComponentInputOptionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // string | The external id of a connected user.
    externalUserId: externalUserId_example,
    // number | The id of an integration instance.
    id: 789,
    // ComponentInputOptionsRequest
    componentInputOptionsRequest: ...,
  } satisfies GetComponentInputOptionsRequest;

  try {
    const data = await api.getComponentInputOptions(body);
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
| **externalUserId** | `string` | The external id of a connected user. | [Defaults to `undefined`] |
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **componentInputOptionsRequest** | [ComponentInputOptionsRequest](ComponentInputOptionsRequest.md) |  | |

### Return type

[**Array&lt;Option&gt;**](Option.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of resolved options. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendComponentInputOptions

> Array&lt;Option&gt; getFrontendComponentInputOptions(id, componentInputOptionsRequest)

Get component input options

Resolve dynamic option values for a component-defined workflow input property.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { GetFrontendComponentInputOptionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // ComponentInputOptionsRequest
    componentInputOptionsRequest: ...,
  } satisfies GetFrontendComponentInputOptionsRequest;

  try {
    const data = await api.getFrontendComponentInputOptions(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **componentInputOptionsRequest** | [ComponentInputOptionsRequest](ComponentInputOptionsRequest.md) |  | |

### Return type

[**Array&lt;Option&gt;**](Option.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of resolved options. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateFrontendIntegrationInstanceWorkflow

> updateFrontendIntegrationInstanceWorkflow(id, workflowUuid, updateFrontendIntegrationInstanceWorkflowRequest)

Update integration instance workflow

Update a workflow for a specific integration instance.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { UpdateFrontendIntegrationInstanceWorkflowOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // UpdateFrontendIntegrationInstanceWorkflowRequest
    updateFrontendIntegrationInstanceWorkflowRequest: ...,
  } satisfies UpdateFrontendIntegrationInstanceWorkflowOperationRequest;

  try {
    const data = await api.updateFrontendIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **updateFrontendIntegrationInstanceWorkflowRequest** | [UpdateFrontendIntegrationInstanceWorkflowRequest](UpdateFrontendIntegrationInstanceWorkflowRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateIntegrationInstanceWorkflow

> updateIntegrationInstanceWorkflow(externalUserId, id, workflowUuid, updateFrontendIntegrationInstanceWorkflowRequest)

Update integration instance workflow

Update a workflow for a specific integration instance.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceWorkflowApi,
} from '';
import type { UpdateIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // number | The id of an integration instance.
    id: 789,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // UpdateFrontendIntegrationInstanceWorkflowRequest
    updateFrontendIntegrationInstanceWorkflowRequest: ...,
  } satisfies UpdateIntegrationInstanceWorkflowRequest;

  try {
    const data = await api.updateIntegrationInstanceWorkflow(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **updateFrontendIntegrationInstanceWorkflowRequest** | [UpdateFrontendIntegrationInstanceWorkflowRequest](UpdateFrontendIntegrationInstanceWorkflowRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

