# IntegrationInstanceConfigurationApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createIntegrationInstanceConfiguration**](IntegrationInstanceConfigurationApi.md#createintegrationinstanceconfiguration) | **POST** /integration-instance-configurations | Create a new integration instance configuration |
| [**createIntegrationInstanceConfigurationWorkflowJob**](IntegrationInstanceConfigurationApi.md#createintegrationinstanceconfigurationworkflowjob) | **POST** /integration-instance-configurations/{id}/workflows/{workflowId}/jobs | Create a request for running a new job |
| [**deleteIntegrationInstanceConfiguration**](IntegrationInstanceConfigurationApi.md#deleteintegrationinstanceconfiguration) | **DELETE** /integration-instance-configurations/{id} | Delete an integration instance configuration |
| [**enableIntegrationInstanceConfiguration**](IntegrationInstanceConfigurationApi.md#enableintegrationinstanceconfiguration) | **PATCH** /integration-instance-configurations/{id}/enable/{enable} | Enable/disable an integration instance configuration |
| [**enableIntegrationInstanceConfigurationWorkflow**](IntegrationInstanceConfigurationApi.md#enableintegrationinstanceconfigurationworkflow) | **PATCH** /integration-instance-configurations/{id}/workflows/{workflowId}/enable/{enable} | Enable/disable a workflow of an integration instance configuration |
| [**getIntegrationInstanceConfiguration**](IntegrationInstanceConfigurationApi.md#getintegrationinstanceconfiguration) | **GET** /integration-instance-configurations/{id} | Get an integration instance configuration by id |
| [**getIntegrationInstanceConfigurationConnectedUsers**](IntegrationInstanceConfigurationApi.md#getintegrationinstanceconfigurationconnectedusers) | **GET** /integration-instance-configurations/{id}/connected-users | Get the connected users of an integration instance configuration |
| [**getIntegrationInstanceConfigurationMoveImpact**](IntegrationInstanceConfigurationApi.md#getintegrationinstanceconfigurationmoveimpact) | **POST** /integration-instance-configurations/{id}/move-impact | Preview moving connected users to another integration instance configuration |
| [**getIntegrationInstanceConfigurationUpgradeImpact**](IntegrationInstanceConfigurationApi.md#getintegrationinstanceconfigurationupgradeimpact) | **POST** /integration-instance-configurations/{id}/upgrade-impact | Preview upgrading connected users to an integration instance configuration |
| [**getIntegrationInstanceConfigurations**](IntegrationInstanceConfigurationApi.md#getintegrationinstanceconfigurations) | **GET** /integration-instance-configurations | Get integration instance configurations |
| [**moveIntegrationInstanceConfigurationConnectedUsers**](IntegrationInstanceConfigurationApi.md#moveintegrationinstanceconfigurationconnectedusers) | **POST** /integration-instance-configurations/{id}/move | Move connected users to another integration instance configuration |
| [**updateIntegrationInstanceConfiguration**](IntegrationInstanceConfigurationApi.md#updateintegrationinstanceconfiguration) | **PUT** /integration-instance-configurations/{id} | Update an existing integration instance configuration |
| [**updateIntegrationInstanceConfigurationWorkflow**](IntegrationInstanceConfigurationApi.md#updateintegrationinstanceconfigurationworkflow) | **PUT** /integration-instance-configurations/{id}/workflows/{workflowId} | Update an existing integration instance configuration workflow |
| [**upgradeIntegrationInstanceConfigurationConnectedUsers**](IntegrationInstanceConfigurationApi.md#upgradeintegrationinstanceconfigurationconnectedusers) | **POST** /integration-instance-configurations/{id}/upgrade | Upgrade connected users to an integration instance configuration |



## createIntegrationInstanceConfiguration

> number createIntegrationInstanceConfiguration(integrationInstanceConfiguration)

Create a new integration instance configuration

Create a new integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { CreateIntegrationInstanceConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // IntegrationInstanceConfiguration
    integrationInstanceConfiguration: ...,
  } satisfies CreateIntegrationInstanceConfigurationRequest;

  try {
    const data = await api.createIntegrationInstanceConfiguration(body);
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
| **integrationInstanceConfiguration** | [IntegrationInstanceConfiguration](IntegrationInstanceConfiguration.md) |  | |

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
| **200** | The integration instance configuration id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createIntegrationInstanceConfigurationWorkflowJob

> CreateIntegrationInstanceConfigurationWorkflowJob200Response createIntegrationInstanceConfigurationWorkflowJob(id, workflowId)

Create a request for running a new job

Create a request for running a new job.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { CreateIntegrationInstanceConfigurationWorkflowJobRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // string | The id of the workflow to execute.
    workflowId: workflowId_example,
  } satisfies CreateIntegrationInstanceConfigurationWorkflowJobRequest;

  try {
    const data = await api.createIntegrationInstanceConfigurationWorkflowJob(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of the workflow to execute. | [Defaults to `undefined`] |

### Return type

[**CreateIntegrationInstanceConfigurationWorkflowJob200Response**](CreateIntegrationInstanceConfigurationWorkflowJob200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The id of a created job. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteIntegrationInstanceConfiguration

> deleteIntegrationInstanceConfiguration(id)

Delete an integration instance configuration

Delete an integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { DeleteIntegrationInstanceConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
  } satisfies DeleteIntegrationInstanceConfigurationRequest;

  try {
    const data = await api.deleteIntegrationInstanceConfiguration(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |

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


## enableIntegrationInstanceConfiguration

> enableIntegrationInstanceConfiguration(id, enable)

Enable/disable an integration instance configuration

Enable/disable an integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { EnableIntegrationInstanceConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // boolean | Enable/disable the integration instance configuration.
    enable: true,
  } satisfies EnableIntegrationInstanceConfigurationRequest;

  try {
    const data = await api.enableIntegrationInstanceConfiguration(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the integration instance configuration. | [Defaults to `undefined`] |

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


## enableIntegrationInstanceConfigurationWorkflow

> enableIntegrationInstanceConfigurationWorkflow(id, workflowId, enable)

Enable/disable a workflow of an integration instance configuration

Enable/disable a workflow of an integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { EnableIntegrationInstanceConfigurationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // string | The id of an integration workflow.
    workflowId: workflowId_example,
    // boolean | Enable/disable the workflow of an integration instance configuration.
    enable: true,
  } satisfies EnableIntegrationInstanceConfigurationWorkflowRequest;

  try {
    const data = await api.enableIntegrationInstanceConfigurationWorkflow(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
| **workflowId** | `string` | The id of an integration workflow. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the workflow of an integration instance configuration. | [Defaults to `undefined`] |

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


## getIntegrationInstanceConfiguration

> IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(id)

Get an integration instance configuration by id

Get an integration instance configuration by id.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { GetIntegrationInstanceConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
  } satisfies GetIntegrationInstanceConfigurationRequest;

  try {
    const data = await api.getIntegrationInstanceConfiguration(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |

### Return type

[**IntegrationInstanceConfiguration**](IntegrationInstanceConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration instance configuration object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstanceConfigurationConnectedUsers

> Array&lt;ConfigurationConnectedUser&gt; getIntegrationInstanceConfigurationConnectedUsers(id)

Get the connected users of an integration instance configuration

Get the connected users that have an integration instance on an integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { GetIntegrationInstanceConfigurationConnectedUsersRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
  } satisfies GetIntegrationInstanceConfigurationConnectedUsersRequest;

  try {
    const data = await api.getIntegrationInstanceConfigurationConnectedUsers(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |

### Return type

[**Array&lt;ConfigurationConnectedUser&gt;**](ConfigurationConnectedUser.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The connected users of the integration instance configuration. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstanceConfigurationMoveImpact

> ConfigurationMoveImpact getIntegrationInstanceConfigurationMoveImpact(id, configurationMoveRequest)

Preview moving connected users to another integration instance configuration

Preview what moving connected users from an integration instance configuration to another one would change.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { GetIntegrationInstanceConfigurationMoveImpactRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of the source integration instance configuration.
    id: 789,
    // ConfigurationMoveRequest
    configurationMoveRequest: ...,
  } satisfies GetIntegrationInstanceConfigurationMoveImpactRequest;

  try {
    const data = await api.getIntegrationInstanceConfigurationMoveImpact(body);
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
| **id** | `number` | The id of the source integration instance configuration. | [Defaults to `undefined`] |
| **configurationMoveRequest** | [ConfigurationMoveRequest](ConfigurationMoveRequest.md) |  | |

### Return type

[**ConfigurationMoveImpact**](ConfigurationMoveImpact.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | What the move would change. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstanceConfigurationUpgradeImpact

> ConfigurationMoveImpact getIntegrationInstanceConfigurationUpgradeImpact(id, configurationUpgradeRequest)

Preview upgrading connected users to an integration instance configuration

Preview what upgrading connected users to an integration instance configuration would change.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { GetIntegrationInstanceConfigurationUpgradeImpactRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of the target integration instance configuration.
    id: 789,
    // ConfigurationUpgradeRequest
    configurationUpgradeRequest: ...,
  } satisfies GetIntegrationInstanceConfigurationUpgradeImpactRequest;

  try {
    const data = await api.getIntegrationInstanceConfigurationUpgradeImpact(body);
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
| **id** | `number` | The id of the target integration instance configuration. | [Defaults to `undefined`] |
| **configurationUpgradeRequest** | [ConfigurationUpgradeRequest](ConfigurationUpgradeRequest.md) |  | |

### Return type

[**ConfigurationMoveImpact**](ConfigurationMoveImpact.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | What the upgrade would change. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrationInstanceConfigurations

> Array&lt;IntegrationInstanceConfiguration&gt; getIntegrationInstanceConfigurations(environmentId, integrationId, tagId, includeAllFields)

Get integration instance configurations

Get integration instance configurations.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { GetIntegrationInstanceConfigurationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The environment id. (optional)
    environmentId: 789,
    // number | The integration id used for filtering integration instance configurations. (optional)
    integrationId: 789,
    // number | The tag id of used for filtering integration instance configurations. (optional)
    tagId: 789,
    // boolean | Use for including all fields or just basic ones. (optional)
    includeAllFields: true,
  } satisfies GetIntegrationInstanceConfigurationsRequest;

  try {
    const data = await api.getIntegrationInstanceConfigurations(body);
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
| **environmentId** | `number` | The environment id. | [Optional] [Defaults to `undefined`] |
| **integrationId** | `number` | The integration id used for filtering integration instance configurations. | [Optional] [Defaults to `undefined`] |
| **tagId** | `number` | The tag id of used for filtering integration instance configurations. | [Optional] [Defaults to `undefined`] |
| **includeAllFields** | `boolean` | Use for including all fields or just basic ones. | [Optional] [Defaults to `true`] |

### Return type

[**Array&lt;IntegrationInstanceConfiguration&gt;**](IntegrationInstanceConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of integration instance configurations. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## moveIntegrationInstanceConfigurationConnectedUsers

> ConfigurationMoveResult moveIntegrationInstanceConfigurationConnectedUsers(id, configurationMoveRequest)

Move connected users to another integration instance configuration

Move connected users from an integration instance configuration to another one.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { MoveIntegrationInstanceConfigurationConnectedUsersRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of the source integration instance configuration.
    id: 789,
    // ConfigurationMoveRequest
    configurationMoveRequest: ...,
  } satisfies MoveIntegrationInstanceConfigurationConnectedUsersRequest;

  try {
    const data = await api.moveIntegrationInstanceConfigurationConnectedUsers(body);
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
| **id** | `number` | The id of the source integration instance configuration. | [Defaults to `undefined`] |
| **configurationMoveRequest** | [ConfigurationMoveRequest](ConfigurationMoveRequest.md) |  | |

### Return type

[**ConfigurationMoveResult**](ConfigurationMoveResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The outcome of the move. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateIntegrationInstanceConfiguration

> updateIntegrationInstanceConfiguration(id, integrationInstanceConfiguration)

Update an existing integration instance configuration

Update an existing integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { UpdateIntegrationInstanceConfigurationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // IntegrationInstanceConfiguration
    integrationInstanceConfiguration: ...,
  } satisfies UpdateIntegrationInstanceConfigurationRequest;

  try {
    const data = await api.updateIntegrationInstanceConfiguration(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
| **integrationInstanceConfiguration** | [IntegrationInstanceConfiguration](IntegrationInstanceConfiguration.md) |  | |

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


## updateIntegrationInstanceConfigurationWorkflow

> updateIntegrationInstanceConfigurationWorkflow(id, workflowId, integrationInstanceConfigurationWorkflow)

Update an existing integration instance configuration workflow

Update an existing integration instance configuration workflow.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { UpdateIntegrationInstanceConfigurationWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
    // number | The id of an integration instance configuration workflow.
    workflowId: 789,
    // IntegrationInstanceConfigurationWorkflow
    integrationInstanceConfigurationWorkflow: ...,
  } satisfies UpdateIntegrationInstanceConfigurationWorkflowRequest;

  try {
    const data = await api.updateIntegrationInstanceConfigurationWorkflow(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |
| **workflowId** | `number` | The id of an integration instance configuration workflow. | [Defaults to `undefined`] |
| **integrationInstanceConfigurationWorkflow** | [IntegrationInstanceConfigurationWorkflow](IntegrationInstanceConfigurationWorkflow.md) |  | |

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


## upgradeIntegrationInstanceConfigurationConnectedUsers

> ConfigurationMoveResult upgradeIntegrationInstanceConfigurationConnectedUsers(id, configurationUpgradeRequest)

Upgrade connected users to an integration instance configuration

Upgrade connected users to an integration instance configuration.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceConfigurationApi,
} from '';
import type { UpgradeIntegrationInstanceConfigurationConnectedUsersRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new IntegrationInstanceConfigurationApi();

  const body = {
    // number | The id of the target integration instance configuration.
    id: 789,
    // ConfigurationUpgradeRequest
    configurationUpgradeRequest: ...,
  } satisfies UpgradeIntegrationInstanceConfigurationConnectedUsersRequest;

  try {
    const data = await api.upgradeIntegrationInstanceConfigurationConnectedUsers(body);
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
| **id** | `number` | The id of the target integration instance configuration. | [Defaults to `undefined`] |
| **configurationUpgradeRequest** | [ConfigurationUpgradeRequest](ConfigurationUpgradeRequest.md) |  | |

### Return type

[**ConfigurationMoveResult**](ConfigurationMoveResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The outcome of the upgrade. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

