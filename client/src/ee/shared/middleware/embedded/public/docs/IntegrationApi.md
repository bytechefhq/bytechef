# IntegrationApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getFrontendIntegration**](IntegrationApi.md#getfrontendintegration) | **GET** /integrations/{id} | Get an integration by id for particular external user |
| [**getFrontendIntegrations**](IntegrationApi.md#getfrontendintegrations) | **GET** /integrations | Get active integrations for particular external user |
| [**getIntegration**](IntegrationApi.md#getintegration) | **GET** /{externalUserId}/integrations/{id} | Get active integrations for particular external user |
| [**getIntegrations**](IntegrationApi.md#getintegrations) | **GET** /{externalUserId}/integrations | Get active integrations for particular external user |



## getFrontendIntegration

> Integration getFrontendIntegration(id, xEnvironment)

Get an integration by id for particular external user

Get an integration by id for particular external user.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetFrontendIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationApi(config);

  const body = {
    // number | The id of an integration.
    id: 789,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendIntegrationRequest;

  try {
    const data = await api.getFrontendIntegration(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Integration**](Integration.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The integration object. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendIntegrations

> Array&lt;IntegrationBasic&gt; getFrontendIntegrations(xEnvironment)

Get active integrations for particular external user

Get active integrations for particular external user.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetFrontendIntegrationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendIntegrationsRequest;

  try {
    const data = await api.getFrontendIntegrations(body);
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

[**Array&lt;IntegrationBasic&gt;**](IntegrationBasic.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of active integrations. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegration

> Integration getIntegration(externalUserId, id, xEnvironment)

Get active integrations for particular external user

Get active integrations for particular external user.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetIntegrationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // number | The id of an integration.
    id: 789,
    // Environment | The environment. (optional)
    xEnvironment: ...,
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **id** | `number` | The id of an integration. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Integration**](Integration.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of active integrations. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getIntegrations

> Array&lt;IntegrationBasic&gt; getIntegrations(externalUserId, xEnvironment)

Get active integrations for particular external user

Get active integrations for particular external user.

### Example

```ts
import {
  Configuration,
  IntegrationApi,
} from '';
import type { GetIntegrationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new IntegrationApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Array&lt;IntegrationBasic&gt;**](IntegrationBasic.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of active integrations. |  -  |
| **401** | Access token is missing or invalid |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

