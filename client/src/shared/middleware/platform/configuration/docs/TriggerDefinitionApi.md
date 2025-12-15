# TriggerDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentTriggerDefinition**](TriggerDefinitionApi.md#getcomponenttriggerdefinition) | **GET** /component-definitions/{componentName}/versions/{componentVersion}/trigger-definitions/{triggerName} | Get a trigger definition of a component |
| [**getComponentTriggerDefinitions**](TriggerDefinitionApi.md#getcomponenttriggerdefinitions) | **GET** /component-definitions/{componentName}/versions/{componentVersion}/trigger-definitions | Get a list of trigger definitions for a component |



## getComponentTriggerDefinition

> TriggerDefinition getComponentTriggerDefinition(componentName, componentVersion, triggerName)

Get a trigger definition of a component

Get a trigger definition of a component.

### Example

```ts
import {
  Configuration,
  TriggerDefinitionApi,
} from '';
import type { GetComponentTriggerDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TriggerDefinitionApi();

  const body = {
    // string | The name of the component.
    componentName: componentName_example,
    // number | The version of a component.
    componentVersion: 56,
    // string | The name of a trigger to get.
    triggerName: triggerName_example,
  } satisfies GetComponentTriggerDefinitionRequest;

  try {
    const data = await api.getComponentTriggerDefinition(body);
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
| **componentName** | `string` | The name of the component. | [Defaults to `undefined`] |
| **componentVersion** | `number` | The version of a component. | [Defaults to `undefined`] |
| **triggerName** | `string` | The name of a trigger to get. | [Defaults to `undefined`] |

### Return type

[**TriggerDefinition**](TriggerDefinition.md)

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


## getComponentTriggerDefinitions

> Array&lt;TriggerDefinitionBasic&gt; getComponentTriggerDefinitions(componentName, componentVersion)

Get a list of trigger definitions for a component

Get a list of trigger definitions for a component.

### Example

```ts
import {
  Configuration,
  TriggerDefinitionApi,
} from '';
import type { GetComponentTriggerDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new TriggerDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component.
    componentVersion: 56,
  } satisfies GetComponentTriggerDefinitionsRequest;

  try {
    const data = await api.getComponentTriggerDefinitions(body);
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

[**Array&lt;TriggerDefinitionBasic&gt;**](TriggerDefinitionBasic.md)

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

