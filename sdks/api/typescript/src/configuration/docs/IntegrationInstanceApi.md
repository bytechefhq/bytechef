# IntegrationInstanceApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createFrontendIntegrationInstance**](IntegrationInstanceApi.md#createfrontendintegrationinstanceoperation) | **POST** /integrations/{id}/instances | Create and configure an integration instance |
| [**createIntegrationInstance**](IntegrationInstanceApi.md#createintegrationinstance) | **POST** /{externalUserId}/integrations/{id}/instances | Connect and configure an integration instance to an existing integration |
| [**deleteFrontendIntegrationInstance**](IntegrationInstanceApi.md#deletefrontendintegrationinstance) | **DELETE** /integration-instances/{id} | Delete an integration instance |
| [**deleteIntegrationInstance**](IntegrationInstanceApi.md#deleteintegrationinstance) | **DELETE** /{externalUserId}/integration-instances/{id} | Delete an integration instance |



## createFrontendIntegrationInstance

> number createFrontendIntegrationInstance(id, createFrontendIntegrationInstanceRequest, xEnvironment)

Create and configure an integration instance

Creates and configures a new integration instance that connects to the specified integration, providing access to its functionality and enabling integration with external services and systems.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { CreateFrontendIntegrationInstanceOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceApi(config);

  const body = {
    // number | The id of an integration.
    id: 789,
    // CreateFrontendIntegrationInstanceRequest
    createFrontendIntegrationInstanceRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateFrontendIntegrationInstanceOperationRequest;

  try {
    const data = await api.createFrontendIntegrationInstance(body);
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
| **createFrontendIntegrationInstanceRequest** | [CreateFrontendIntegrationInstanceRequest](CreateFrontendIntegrationInstanceRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**number**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration instance id. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createIntegrationInstance

> number createIntegrationInstance(externalUserId, id, createFrontendIntegrationInstanceRequest, xEnvironment)

Connect and configure an integration instance to an existing integration

Connects an integration instance to an existing integration, allowing access to its functionality and resources.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { CreateIntegrationInstanceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // number | The id of an integration.
    id: 789,
    // CreateFrontendIntegrationInstanceRequest
    createFrontendIntegrationInstanceRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateIntegrationInstanceRequest;

  try {
    const data = await api.createIntegrationInstance(body);
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
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |
| **createFrontendIntegrationInstanceRequest** | [CreateFrontendIntegrationInstanceRequest](CreateFrontendIntegrationInstanceRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**number**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration instance id. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteFrontendIntegrationInstance

> deleteFrontendIntegrationInstance(id)

Delete an integration instance

Delete an integration instance.

### Example

```ts
import {
  Configuration,
  IntegrationInstanceApi,
} from '';
import type { DeleteFrontendIntegrationInstanceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceApi(config);

  const body = {
    // number | The id of an integration instance.
    id: 789,
  } satisfies DeleteFrontendIntegrationInstanceRequest;

  try {
    const data = await api.deleteFrontendIntegrationInstance(body);
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


## deleteIntegrationInstance

> deleteIntegrationInstance(externalUserId, id)

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
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationInstanceApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |

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

