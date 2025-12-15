# ComponentDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentDefinition**](ComponentDefinitionApi.md#getcomponentdefinition) | **GET** /component-definitions/{componentName}/versions/{componentVersion} | Get a component definition |
| [**getComponentDefinitionVersions**](ComponentDefinitionApi.md#getcomponentdefinitionversions) | **GET** /component-definitions/{componentName}/versions | Get all component definition versions of a component |
| [**getConnectionComponentDefinition**](ComponentDefinitionApi.md#getconnectioncomponentdefinition) | **GET** /component-definitions/{componentName}/connection-versions/{connectionVersion} | Get a connection component definition |
| [**getUnifiedApiComponentDefinitions**](ComponentDefinitionApi.md#getunifiedapicomponentdefinitions) | **GET** /unified-api/{category}/component-definitions | Get all compatible component definitions for a unified API category |



## getComponentDefinition

> ComponentDefinition getComponentDefinition(componentName, componentVersion)

Get a component definition

Get a component definition.

### Example

```ts
import {
  Configuration,
  ComponentDefinitionApi,
} from '';
import type { GetComponentDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ComponentDefinitionApi();

  const body = {
    // string | The name of a component to get.
    componentName: componentName_example,
    // number | The version of a component to get.
    componentVersion: 56,
  } satisfies GetComponentDefinitionRequest;

  try {
    const data = await api.getComponentDefinition(body);
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
| **componentName** | `string` | The name of a component to get. | [Defaults to `undefined`] |
| **componentVersion** | `number` | The version of a component to get. | [Defaults to `undefined`] |

### Return type

[**ComponentDefinition**](ComponentDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getComponentDefinitionVersions

> Array&lt;ComponentDefinitionBasic&gt; getComponentDefinitionVersions(componentName)

Get all component definition versions of a component

Get all component definition versions of a component.

### Example

```ts
import {
  Configuration,
  ComponentDefinitionApi,
} from '';
import type { GetComponentDefinitionVersionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ComponentDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
  } satisfies GetComponentDefinitionVersionsRequest;

  try {
    const data = await api.getComponentDefinitionVersions(body);
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
| **componentName** | `string` | The name of a component. | [Defaults to `undefined`] |

### Return type

[**Array&lt;ComponentDefinitionBasic&gt;**](ComponentDefinitionBasic.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnectionComponentDefinition

> ComponentDefinition getConnectionComponentDefinition(componentName, connectionVersion)

Get a connection component definition

Get a connection component definition.

### Example

```ts
import {
  Configuration,
  ComponentDefinitionApi,
} from '';
import type { GetConnectionComponentDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ComponentDefinitionApi();

  const body = {
    // string | The name of a component to get.
    componentName: componentName_example,
    // number | The version of a component connection to get.
    connectionVersion: 56,
  } satisfies GetConnectionComponentDefinitionRequest;

  try {
    const data = await api.getConnectionComponentDefinition(body);
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
| **componentName** | `string` | The name of a component to get. | [Defaults to `undefined`] |
| **connectionVersion** | `number` | The version of a component connection to get. | [Defaults to `undefined`] |

### Return type

[**ComponentDefinition**](ComponentDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getUnifiedApiComponentDefinitions

> Array&lt;ComponentDefinitionBasic&gt; getUnifiedApiComponentDefinitions(category)

Get all compatible component definitions for a unified API category

Get all compatible component definitions for a unified API category.

### Example

```ts
import {
  Configuration,
  ComponentDefinitionApi,
} from '';
import type { GetUnifiedApiComponentDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ComponentDefinitionApi();

  const body = {
    // UnifiedApiCategory | The name of a unified API category.
    category: ...,
  } satisfies GetUnifiedApiComponentDefinitionsRequest;

  try {
    const data = await api.getUnifiedApiComponentDefinitions(body);
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
| **category** | `UnifiedApiCategory` | The name of a unified API category. | [Defaults to `undefined`] [Enum: ACCOUNTING, ATS, CRM, E_COMMERCE, HRIS, FILE_STORAGE, MARKETING_AUTOMATION, TICKETING] |

### Return type

[**Array&lt;ComponentDefinitionBasic&gt;**](ComponentDefinitionBasic.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

