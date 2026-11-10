# IntegrationInstanceApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteIntegrationInstance**](IntegrationInstanceApi.md#deleteintegrationinstance) | **DELETE** /integration-instances/{id} | Delete an integration instance |
| [**enableIntegrationInstance**](IntegrationInstanceApi.md#enableintegrationinstance) | **PATCH** /integration-instances/{id}/enable/{enable} | Enable/disable an integration instance |
| [**enableIntegrationInstanceWorkflow**](IntegrationInstanceApi.md#enableintegrationinstanceworkflow) | **PATCH** /integration-instances/{id}/workflows/{workflowId}/enable/{enable} | Enable/disable an integration instance workflow |
| [**getIntegrationInstance**](IntegrationInstanceApi.md#getintegrationinstance) | **GET** /integration-instances/{id} | Get an integration instance by id |
| [**getIntegrationInstanceConnection**](IntegrationInstanceApi.md#getintegrationinstanceconnection) | **GET** /integration-instances/{id}/connection | Get the connection of a provisioned integration instance |
| [**getProvisionableIntegrations**](IntegrationInstanceApi.md#getprovisionableintegrations) | **GET** /connected-users/{connectedUserId}/provisionable-integrations | Get the integrations that can be provisioned to the connected user |
| [**provisionIntegrationInstance**](IntegrationInstanceApi.md#provisionintegrationinstanceoperation) | **POST** /connected-users/{connectedUserId}/integration-instances | Provision an integration instance for the connected user |
| [**updateIntegrationInstanceCredentials**](IntegrationInstanceApi.md#updateintegrationinstancecredentials) | **PUT** /integration-instances/{id}/connection/credentials | Replaces the credentials of a provisioned integration instance |



## deleteIntegrationInstance

> deleteIntegrationInstance(id)

Delete an integration instance

Delete an integration instance.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { DeleteIntegrationInstanceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
  } satisfies DeleteIntegrationInstanceRequest;

  try {
    const data = await api.deleteIntegrationInstance(body);
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

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableIntegrationInstance

> enableIntegrationInstance(id, enable)

Enable/disable an integration instance

Enable/disable an integration instance.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { EnableIntegrationInstanceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // boolean | Enable/disable the integration instance.
    enable: true,
  } satisfies EnableIntegrationInstanceRequest;

  try {
    const data = await api.enableIntegrationInstance(body);
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
| **enable** | `boolean` | Enable/disable the integration instance. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableIntegrationInstanceWorkflow

> enableIntegrationInstanceWorkflow(id, workflowId, enable)

Enable/disable an integration instance workflow

Enable/disable an integration instance workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { EnableIntegrationInstanceWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // string | The id of an integration instance workflow.
    workflowId: workflowId_example,
    // boolean | Enable/disable the integration instance workflow.
    enable: true,
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of an integration instance workflow. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the integration instance workflow. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstance

> IntegrationInstance getIntegrationInstance(id)

Get an integration instance by id

Get an integration instance by id.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { GetIntegrationInstanceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
  } satisfies GetIntegrationInstanceRequest;

  try {
    const data = await api.getIntegrationInstance(body);
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

### Return type

[**IntegrationInstance**](IntegrationInstance.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration instance object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstanceConnection

> Connection getIntegrationInstanceConnection(id)

Get the connection of a provisioned integration instance

Get the connection of an integration instance provisioned by a tenant admin.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { GetIntegrationInstanceConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
  } satisfies GetIntegrationInstanceConnectionRequest;

  try {
    const data = await api.getIntegrationInstanceConnection(body);
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

### Return type

[**Connection**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProvisionableIntegrations

> Array&lt;ProvisionableIntegration&gt; getProvisionableIntegrations(connectedUserId)

Get the integrations that can be provisioned to the connected user

Get the integrations that can be provisioned to the connected user.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { GetProvisionableIntegrationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
  } satisfies GetProvisionableIntegrationsRequest;

  try {
    const data = await api.getProvisionableIntegrations(body);
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
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |

### Return type

[**Array&lt;ProvisionableIntegration&gt;**](ProvisionableIntegration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The provisionable integrations. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## provisionIntegrationInstance

> number provisionIntegrationInstance(connectedUserId, provisionIntegrationInstanceRequest)

Provision an integration instance for the connected user

Provision an integration instance for the connected user on a chosen integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { ProvisionIntegrationInstanceOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of a connected user.
    connectedUserId: 789,
    // ProvisionIntegrationInstanceRequest
    provisionIntegrationInstanceRequest: ...,
  } satisfies ProvisionIntegrationInstanceOperationRequest;

  try {
    const data = await api.provisionIntegrationInstance(body);
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
| **connectedUserId** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **provisionIntegrationInstanceRequest** | [ProvisionIntegrationInstanceRequest](ProvisionIntegrationInstanceRequest.md) |  | |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration instance id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateIntegrationInstanceCredentials

> updateIntegrationInstanceCredentials(id, updateConnectionCredentialsRequest)

Replaces the credentials of a provisioned integration instance

Replaces the credentials of the connection of an integration instance provisioned by a tenant admin.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { UpdateIntegrationInstanceCredentialsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
    // UpdateConnectionCredentialsRequest
    updateConnectionCredentialsRequest: ...,
  } satisfies UpdateIntegrationInstanceCredentialsRequest;

  try {
    const data = await api.updateIntegrationInstanceCredentials(body);
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
| **updateConnectionCredentialsRequest** | [UpdateConnectionCredentialsRequest](UpdateConnectionCredentialsRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

