# ClusterElementDefinitionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getComponentClusterElementDefinition**](ClusterElementDefinitionApi.md#getcomponentclusterelementdefinition) | **GET** /component-definitions/{componentName}/versions/{componentVersion}/cluster-element-definition/{clusterElementName} | Get a cluster element definition of a component |
| [**getRootComponentClusterElementDefinitions**](ClusterElementDefinitionApi.md#getrootcomponentclusterelementdefinitions) | **GET** /component-definitions/{rootComponentName}/versions/{rootComponentVersion}/cluster-element-definitions/{clusterElementType} | Get a cluster element definitions of a root component. |



## getComponentClusterElementDefinition

> ClusterElementDefinition getComponentClusterElementDefinition(componentName, componentVersion, clusterElementName)

Get a cluster element definition of a component

Get a cluster element definition of a component.

### Example

```ts
import {
  Configuration,
  ClusterElementDefinitionApi,
} from '';
import type { GetComponentClusterElementDefinitionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ClusterElementDefinitionApi();

  const body = {
    // string | The name of a component.
    componentName: componentName_example,
    // number | The version of a component.
    componentVersion: 56,
    // string | The name of a cluster element to get.
    clusterElementName: clusterElementName_example,
  } satisfies GetComponentClusterElementDefinitionRequest;

  try {
    const data = await api.getComponentClusterElementDefinition(body);
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
| **clusterElementName** | `string` | The name of a cluster element to get. | [Defaults to `undefined`] |

### Return type

[**ClusterElementDefinition**](ClusterElementDefinition.md)

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


## getRootComponentClusterElementDefinitions

> Array&lt;ClusterElementDefinitionBasic&gt; getRootComponentClusterElementDefinitions(rootComponentName, rootComponentVersion, clusterElementType)

Get a cluster element definitions of a root component.

Get a cluster element definitions of a root component.

### Example

```ts
import {
  Configuration,
  ClusterElementDefinitionApi,
} from '';
import type { GetRootComponentClusterElementDefinitionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ClusterElementDefinitionApi();

  const body = {
    // string | The name of a root component.
    rootComponentName: rootComponentName_example,
    // number | The version of a root component.
    rootComponentVersion: 56,
    // string | The name of a cluster elements to get.
    clusterElementType: clusterElementType_example,
  } satisfies GetRootComponentClusterElementDefinitionsRequest;

  try {
    const data = await api.getRootComponentClusterElementDefinitions(body);
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
| **rootComponentName** | `string` | The name of a root component. | [Defaults to `undefined`] |
| **rootComponentVersion** | `number` | The version of a root component. | [Defaults to `undefined`] |
| **clusterElementType** | `string` | The name of a cluster elements to get. | [Defaults to `undefined`] |

### Return type

[**Array&lt;ClusterElementDefinitionBasic&gt;**](ClusterElementDefinitionBasic.md)

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

