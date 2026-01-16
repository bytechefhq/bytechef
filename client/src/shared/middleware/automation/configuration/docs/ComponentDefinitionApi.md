# ComponentDefinitionApi

All URIs are relative to */api/automation/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentDefinitions**](ComponentDefinitionApi.md#getcomponentdefinitions) | **GET** /component-definitions | Get all component definitions |



## getComponentDefinitions

> Array&lt;ComponentDefinitionBasic&gt; getComponentDefinitions(actionDefinitions, connectionDefinitions, triggerDefinitions, include)

Get all component definitions

Get all component definitions.

### Example

```ts
import {
  Configuration,
  ComponentDefinitionApi,
} from '';
import type { GetComponentDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ComponentDefinitionApi();

  const body = {
    // boolean | Use for filtering components which define action definitions. (optional)
    actionDefinitions: true,
    // boolean | Use for filtering components which define connection definitions. (optional)
    connectionDefinitions: true,
    // boolean | Use for filtering components which define trigger definitions. (optional)
    triggerDefinitions: true,
    // Array<string> | The list of component names to include in the result. (optional)
    include: ...,
  } satisfies GetComponentDefinitionsRequest;

  try {
    const data = await api.getComponentDefinitions(body);
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
| **actionDefinitions** | `boolean` | Use for filtering components which define action definitions. | [Optional] [Defaults to `undefined`] |
| **connectionDefinitions** | `boolean` | Use for filtering components which define connection definitions. | [Optional] [Defaults to `undefined`] |
| **triggerDefinitions** | `boolean` | Use for filtering components which define trigger definitions. | [Optional] [Defaults to `undefined`] |
| **include** | `Array<string>` | The list of component names to include in the result. | [Optional] |

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

