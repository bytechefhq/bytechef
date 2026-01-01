# IntegrationApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createIntegration**](IntegrationApi.md#createintegration) | **POST** /integrations | Create a new integration |
| [**createIntegrationWorkflow**](IntegrationApi.md#createintegrationworkflow) | **POST** /integrations/{id}/workflows | Create new workflow and adds it to an existing integration |
| [**deleteIntegration**](IntegrationApi.md#deleteintegration) | **DELETE** /integrations/{id} | Delete an integration |
| [**getIntegration**](IntegrationApi.md#getintegration) | **GET** /integrations/{id} | Get an integration by id |
| [**getIntegrationVersions**](IntegrationApi.md#getintegrationversions) | **GET** /integrations/{id}/versions | Get a integration versions. |
| [**getIntegrations**](IntegrationApi.md#getintegrations) | **GET** /integrations | Get integrations |
| [**publishIntegration**](IntegrationApi.md#publishintegrationoperation) | **POST** /integrations/{id}/publish | Publishes existing integration. |
| [**updateIntegration**](IntegrationApi.md#updateintegration) | **PUT** /integrations/{id} | Update an existing integration |



## createIntegration

> number createIntegration(integration)

Create a new integration

Create a new integration.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { CreateIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // Integration
    integration: ...,
  } satisfies CreateIntegrationRequest;

  try {
    const data = await api.createIntegration(body);
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
| **integration** | [Integration](Integration.md) |  | |

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
| **200** | The integration id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createIntegrationWorkflow

> number createIntegrationWorkflow(id, workflow)

Create new workflow and adds it to an existing integration

Create new workflow and adds it to an existing integration.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { CreateIntegrationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of an integration.
    id: 789,
    // Workflow
    workflow: ...,
  } satisfies CreateIntegrationWorkflowRequest;

  try {
    const data = await api.createIntegrationWorkflow(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |
| **workflow** | [Workflow](Workflow.md) |  | |

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
| **200** | The integration workflow id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteIntegration

> deleteIntegration(id)

Delete an integration

Delete an integration.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { DeleteIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of an integration.
    id: 789,
  } satisfies DeleteIntegrationRequest;

  try {
    const data = await api.deleteIntegration(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |

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


## getIntegration

> Integration getIntegration(id)

Get an integration by id

Get an integration by id.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of an integration.
    id: 789,
  } satisfies GetIntegrationRequest;

  try {
    const data = await api.getIntegration(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |

### Return type

[**Integration**](Integration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationVersions

> Array&lt;IntegrationVersion&gt; getIntegrationVersions(id)

Get a integration versions.

Get a integration versions.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetIntegrationVersionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of an integration.
    id: 789,
  } satisfies GetIntegrationVersionsRequest;

  try {
    const data = await api.getIntegrationVersions(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |

### Return type

[**Array&lt;IntegrationVersion&gt;**](IntegrationVersion.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of integration version objects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrations

> Array&lt;Integration&gt; getIntegrations(categoryId, integrationInstanceConfigurations, status, tagId, includeAllFields)

Get integrations

Get integrations.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetIntegrationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The category id used for filtering integrations. (optional)
    categoryId: 789,
    // boolean | Use for filtering integrations for which integration instance configurations exist. (optional)
    integrationInstanceConfigurations: true,
    // IntegrationStatus | Use for filtering integrations by status. (optional)
    status: ...,
    // number | The tag id of used for filtering integrations. (optional)
    tagId: 789,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
  } satisfies GetIntegrationsRequest;

  try {
    const data = await api.getIntegrations(body);
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
| **categoryId** | `number` | The category id used for filtering integrations. | [Optional] [Defaults to `undefined`] |
| **integrationInstanceConfigurations** | `boolean` | Use for filtering integrations for which integration instance configurations exist. | [Optional] [Defaults to `undefined`] |
| **status** | `IntegrationStatus` | Use for filtering integrations by status. | [Optional] [Defaults to `undefined`] [Enum: DRAFT, PUBLISHED] |
| **tagId** | `number` | The tag id of used for filtering integrations. | [Optional] [Defaults to `undefined`] |
| **includeAllFields** | `boolean` | Use for including all fields or just basic ones. | [Optional] [Defaults to `true`] |

### Return type

[**Array&lt;Integration&gt;**](Integration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of integrations. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishIntegration

> publishIntegration(id, publishIntegrationRequest)

Publishes existing integration.

Publishes existing integration.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { PublishIntegrationOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of a integration.
    id: 789,
    // PublishIntegrationRequest (optional)
    publishIntegrationRequest: ...,
  } satisfies PublishIntegrationOperationRequest;

  try {
    const data = await api.publishIntegration(body);
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
| **id** | `number` | The id of a integration. | [Defaults to `undefined`] |
| **publishIntegrationRequest** | [PublishIntegrationRequest](PublishIntegrationRequest.md) |  | [Optional] |

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


## updateIntegration

> updateIntegration(id, integration)

Update an existing integration

Update an existing integration.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { UpdateIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationApi();

  const body = {
    // number | The id of an integration.
    id: 789,
    // Integration
    integration: ...,
  } satisfies UpdateIntegrationRequest;

  try {
    const data = await api.updateIntegration(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |
| **integration** | [Integration](Integration.md) |  | |

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

