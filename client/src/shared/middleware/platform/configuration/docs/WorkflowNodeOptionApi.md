# WorkflowNodeOptionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getClusterElementNodeOptions**](WorkflowNodeOptionApi.md#getclusterelementnodeoptions) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}/{clusterElementWorkflowNodeName}/options/{propertyName} | Get a cluster element property options shown in the editor |
| [**getWorkflowNodeOptions**](WorkflowNodeOptionApi.md#getworkflownodeoptions) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/options/{propertyName} | Get an action or trigger property options shown in the editor |



## getClusterElementNodeOptions

> Array&lt;Option&gt; getClusterElementNodeOptions(id, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, propertyName, environmentId, lookupDependsOnPaths, searchText)

Get a cluster element property options shown in the editor

Get a cluster element property options shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeOptionApi,
} from '';
import type { GetClusterElementNodeOptionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeOptionApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of a workflow\'s cluster root action task (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element type.
    clusterElementType: clusterElementType_example,
    // string | The name of a cluster element workflow node.
    clusterElementWorkflowNodeName: clusterElementWorkflowNodeName_example,
    // string | The name of a property.
    propertyName: propertyName_example,
    // number | The id of an environment.
    environmentId: 789,
    // Array<string> | The list of dependency paths. (optional)
    lookupDependsOnPaths: ...,
    // string | Optional search text used to filter option items (optional)
    searchText: searchText_example,
  } satisfies GetClusterElementNodeOptionsRequest;

  try {
    const data = await api.getClusterElementNodeOptions(body);
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
| **workflowNodeName** | `string` | The name of a workflow\&#39;s cluster root action task (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **clusterElementType** | `string` | The name of a cluster element type. | [Defaults to `undefined`] |
| **clusterElementWorkflowNodeName** | `string` | The name of a cluster element workflow node. | [Defaults to `undefined`] |
| **propertyName** | `string` | The name of a property. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |
| **lookupDependsOnPaths** | `Array<string>` | The list of dependency paths. | [Optional] |
| **searchText** | `string` | Optional search text used to filter option items | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Option&gt;**](Option.md)

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


## getWorkflowNodeOptions

> Array&lt;Option&gt; getWorkflowNodeOptions(id, workflowNodeName, propertyName, environmentId, lookupDependsOnPaths, searchText)

Get an action or trigger property options shown in the editor

Get an action or trigger property options shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeOptionApi,
} from '';
import type { GetWorkflowNodeOptionsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeOptionApi();

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
    // string | Optional search text used to filter option items (optional)
    searchText: searchText_example,
  } satisfies GetWorkflowNodeOptionsRequest;

  try {
    const data = await api.getWorkflowNodeOptions(body);
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
| **searchText** | `string` | Optional search text used to filter option items | [Optional] [Defaults to `undefined`] |

### Return type

[**Array&lt;Option&gt;**](Option.md)

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

