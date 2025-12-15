# ActionDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentActionDefinition**](ActionDefinitionApi.md#getcomponentactiondefinition) | **GET** /component-definitions/{componentName}/versions/{componentVersion}/action-definitions/{actionName} | Get an action definition of a component |
| [**getComponentActionDefinitions**](ActionDefinitionApi.md#getcomponentactiondefinitions) | **GET** /component-definitions/{componentName}/versions/{componentVersion}/action-definitions | Get a list of action definitions for a component |



## getComponentActionDefinition

> ActionDefinition getComponentActionDefinition(componentName, componentVersion, actionName)

Get an action definition of a component

Get an action definition of a component.

### Example

```ts
import {
  Configuration,
  ActionDefinitionApi,
} from '';
import type { GetComponentActionDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ActionDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component.
    componentVersion: 56,
    // string | The name of the action to get.
    actionName: actionName_example,
  } satisfies GetComponentActionDefinitionRequest;

  try {
    const data = await api.getComponentActionDefinition(body);
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
| **componentVersion** | `number` | The version of a component. | [Defaults to `undefined`] |
| **actionName** | `string` | The name of the action to get. | [Defaults to `undefined`] |

### Return type

[**ActionDefinition**](ActionDefinition.md)

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


## getComponentActionDefinitions

> Array&lt;ActionDefinitionBasic&gt; getComponentActionDefinitions(componentName, componentVersion)

Get a list of action definitions for a component

Get a list of action definitions for a component.

### Example

```ts
import {
  Configuration,
  ActionDefinitionApi,
} from '';
import type { GetComponentActionDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ActionDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component.
    componentVersion: 56,
  } satisfies GetComponentActionDefinitionsRequest;

  try {
    const data = await api.getComponentActionDefinitions(body);
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
| **componentVersion** | `number` | The version of a component. | [Defaults to `undefined`] |

### Return type

[**Array&lt;ActionDefinitionBasic&gt;**](ActionDefinitionBasic.md)

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

