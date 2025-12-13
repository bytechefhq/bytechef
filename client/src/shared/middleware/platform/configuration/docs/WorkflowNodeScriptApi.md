# WorkflowNodeScriptApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**testWorkflowNodeScript**](WorkflowNodeScriptApi.md#testworkflownodescript) | **POST** /workflows/{id}/workflow-nodes/{workflowNodeName}/scripts | Execute a script for testing purposes |



## testWorkflowNodeScript

> ScriptTestExecution testWorkflowNodeScript(id, workflowNodeName, environmentId)

Execute a script for testing purposes

Execute a script for testing purposes.

### Example

```ts
import {
  Configuration,
  WorkflowNodeScriptApi,
} from '';
import type { TestWorkflowNodeScriptRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowNodeScriptApi();

  const body = {
    // string | The id of a workflow.
    id: id_example,
    // string | The name of a workflow node which uses the script component.
    workflowNodeName: workflowNodeName_example,
    // number | The id of an environment.
    environmentId: 789,
  } satisfies TestWorkflowNodeScriptRequest;

  try {
    const data = await api.testWorkflowNodeScript(body);
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
| **id** | `string` | The id of a workflow. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The name of a workflow node which uses the script component. | [Defaults to `undefined`] |
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**ScriptTestExecution**](ScriptTestExecution.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The script test execution object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

