# WorkflowTestApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**stopWorkflowTest**](WorkflowTestApi.md#stopworkflowtest) | **POST** /workflow-tests/{jobId}/stop | Stop workflow test run |



## stopWorkflowTest

> stopWorkflowTest(jobId)

Stop workflow test run

Abort an in-progress workflow test run identified by jobId. Sends an \&#39;error\&#39; event with \&#39;Aborted\&#39; and closes the SSE stream.

### Example

```ts
import {
  Configuration,
  WorkflowTestApi,
} from '';
import type { StopWorkflowTestRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new WorkflowTestApi();

  const body = {
    // string | The job identifier obtained from the start endpoint.
    jobId: jobId_example,
  } satisfies StopWorkflowTestRequest;

  try {
    const data = await api.stopWorkflowTest(body);
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
| **jobId** | `string` | The job identifier obtained from the start endpoint. | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Run aborted |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

