# ConnectionDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentConnectionDefinition**](ConnectionDefinitionApi.md#getcomponentconnectiondefinition) | **GET** /component-definitions/{componentName}/connection-definition | Get connection definition for a component |
| [**getComponentConnectionDefinitions**](ConnectionDefinitionApi.md#getcomponentconnectiondefinitions) | **GET** /component-definitions/{componentName}/connection-definitions | Get all compatible connection definitions for a component |



## getComponentConnectionDefinition

> ConnectionDefinition getComponentConnectionDefinition(componentName, componentVersion)

Get connection definition for a component

Get connection definition for a component.

### Example

```ts
import {
  Configuration,
  ConnectionDefinitionApi,
} from '';
import type { GetComponentConnectionDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component. (optional)
    componentVersion: 56,
  } satisfies GetComponentConnectionDefinitionRequest;

  try {
    const data = await api.getComponentConnectionDefinition(body);
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
| **componentVersion** | `number` | The version of a component. | [Optional] [Defaults to `undefined`] |

### Return type

[**ConnectionDefinition**](ConnectionDefinition.md)

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


## getComponentConnectionDefinitions

> Array&lt;ConnectionDefinitionBasic&gt; getComponentConnectionDefinitions(componentName, componentVersion)

Get all compatible connection definitions for a component

Get all compatible connection definitions for a component.

### Example

```ts
import {
  Configuration,
  ConnectionDefinitionApi,
} from '';
import type { GetComponentConnectionDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectionDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component. (optional)
    componentVersion: 56,
  } satisfies GetComponentConnectionDefinitionsRequest;

  try {
    const data = await api.getComponentConnectionDefinitions(body);
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
| **componentVersion** | `number` | The version of a component. | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;ConnectionDefinitionBasic&gt;**](ConnectionDefinitionBasic.md)

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

