# WorkflowNodeDescriptionApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getClusterElementWorkflowNodeDescription**](WorkflowNodeDescriptionApi.md#getclusterelementworkflownodedescription) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-element-definition/{clusterElementName}/descriptions | Get an action description shown in the editor |
| [**getWorkflowNodeDescription**](WorkflowNodeDescriptionApi.md#getworkflownodedescription) | **GET** /workflows/{id}/workflow-nodes/{workflowNodeName}/descriptions | Get an action description shown in the editor |



## getClusterElementWorkflowNodeDescription

> GetWorkflowNodeDescription200Response getClusterElementWorkflowNodeDescription(id, workflowNodeName, clusterElementName, environmentId)

Get an action description shown in the editor

Get an action description shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeDescriptionApi,
} from '';
import type { GetClusterElementWorkflowNodeDescriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeDescriptionApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of an workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // string | The name of a cluster element to get.
    clusterElementName: clusterElementName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetClusterElementWorkflowNodeDescriptionRequest;

  try {
    const data = await api.getClusterElementWorkflowNodeDescription(body);
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
| **workflowNodeName** | `string` | The name of an workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **clusterElementName** | `string` | The name of a cluster element to get. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**GetWorkflowNodeDescription200Response**](GetWorkflowNodeDescription200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The editor description. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowNodeDescription

> GetWorkflowNodeDescription200Response getWorkflowNodeDescription(id, workflowNodeName, environmentId)

Get an action description shown in the editor

Get an action description shown in the editor.

### Example

```ts
import {
  Configuration,
  WorkflowNodeDescriptionApi,
} from '';
import type { GetWorkflowNodeDescriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeDescriptionApi();

  const body = {
    // string | The workflow id
    id: id_example,
    // string | The name of an workflow\'s action task or trigger (E.g. mailchimp_1)
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetWorkflowNodeDescriptionRequest;

  try {
    const data = await api.getWorkflowNodeDescription(body);
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
| **workflowNodeName** | `string` | The name of an workflow\&#39;s action task or trigger (E.g. mailchimp_1) | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**GetWorkflowNodeDescription200Response**](GetWorkflowNodeDescription200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The editor description. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

