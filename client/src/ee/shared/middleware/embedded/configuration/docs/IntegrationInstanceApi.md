# IntegrationInstanceApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteIntegrationInstance**](IntegrationInstanceApi.md#deleteintegrationinstance) | **DELETE** /integration-instances/{id} | Delete an integration instance |
| [**enableIntegrationInstance**](IntegrationInstanceApi.md#enableintegrationinstance) | **PATCH** /integration-instances/{id}/enable/{enable} | Enable/disable an integration instance |
| [**enableIntegrationInstanceWorkflow**](IntegrationInstanceApi.md#enableintegrationinstanceworkflow) | **PATCH** /integration-instances/{id}/workflows/{workflowId}/enable/{enable} | Enable/disable an integration instance workflow |
| [**getIntegrationInstance**](IntegrationInstanceApi.md#getintegrationinstance) | **GET** /integration-instances/{id} | Get an integration instance by id |



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

