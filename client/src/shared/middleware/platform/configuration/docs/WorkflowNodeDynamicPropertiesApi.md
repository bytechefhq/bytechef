# WorkflowNodeDynamicPropertiesApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getClusterElementDynamicProperties**](WorkflowNodeDynamicPropertiesApi.md#getclusterelementdynamicproperties) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/dynamic-properties/{propertyName} | Get dynamic properties for an action or trigger property shown in the editor |
| [**getWorkflowNodeDynamicProperties**](WorkflowNodeDynamicPropertiesApi.md#getworkflownodedynamicproperties) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/dynamic-properties/{propertyName} | Get dynamic properties for an action or trigger property shown in the editor |



## getClusterElementDynamicProperties

> Array&lt;Property&gt; getClusterElementDynamicProperties(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, propertyName, environmentId, lookupDependsOnPaths)

Get dynamic properties for an action or trigger property shown in the editor

Get dynamic properties for an action or trigger property shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeDynamicPropertiesApi,
} from '';
import type { GetClusterElementDynamicPropertiesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeDynamicPropertiesApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node name.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // string | The name of a property.
    propertyName: propertyName_example,
    // number | The id of an environment.
    environmentId: 789,
    // Array<string> | The list of dependency paths. (optional)
    lookupDependsOnPaths: ...,
  } satisfies GetClusterElementDynamicPropertiesRequest;

  try {
    const data = await api.getClusterElementDynamicProperties(body);
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
| **id** | `string` | The workflow id | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **clusterElementType** | `string` | The name of a cluster element type. | [Defaults to `undefined`] |
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node name. | [Defaults to `undefined`] |
| **propertyName** | `string` | The name of a property. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **lookupDependsOnPaths** | `Array<string>` | The list of dependency paths. | [Optional] |

### Return type

[**Array&lt;Property&gt;**](Property.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of options. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowNodeDynamicProperties

> Array&lt;Property&gt; getWorkflowNodeDynamicProperties(id, workflowNodeName, propertyName, environmentId, lookupDependsOnPaths)

Get dynamic properties for an action or trigger property shown in the editor

Get dynamic properties for an action or trigger property shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeDynamicPropertiesApi,
} from '';
import type { GetWorkflowNodeDynamicPropertiesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeDynamicPropertiesApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a property.
    propertyName: propertyName_example,
    // number | The id of an environment.
    environmentId: 789,
    // Array<string> | The list of dependency paths. (optional)
    lookupDependsOnPaths: ...,
  } satisfies GetWorkflowNodeDynamicPropertiesRequest;

  try {
    const data = await api.getWorkflowNodeDynamicProperties(body);
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
| **id** | `string` | The workflow id | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **propertyName** | `string` | The name of a property. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **lookupDependsOnPaths** | `Array<string>` | The list of dependency paths. | [Optional] |

### Return type

[**Array&lt;Property&gt;**](Property.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of options. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

