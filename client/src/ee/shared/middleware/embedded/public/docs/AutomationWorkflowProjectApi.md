# AutomationWorkflowProjectApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getFrontendProjects**](AutomationWorkflowProjectApi.md#getfrontendprojects) | **GET** /automation/projects | Get automation workflow projects available to the connected user |
| [**getProjects**](AutomationWorkflowProjectApi.md#getprojects) | **GET** /{externalUserId}/automation/projects | Get automation workflow projects available to a connected user identified by external user id |



## getFrontendProjects

> Array&lt;AutomationWorkflowProject&gt; getFrontendProjects(xEnvironment)

Get automation workflow projects available to the connected user

Get automation workflow projects available to the connected user.

### Example

```ts
import {
  Configuration,
  AutomationWorkflowProjectApi,
} from '';
import type { GetFrontendProjectsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationWorkflowProjectApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendProjectsRequest;

  try {
    const data = await api.getFrontendProjects(body);
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
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Array&lt;AutomationWorkflowProject&gt;**](AutomationWorkflowProject.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation workflow projects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjects

> Array&lt;AutomationWorkflowProject&gt; getProjects(externalUserId, xEnvironment)

Get automation workflow projects available to a connected user identified by external user id

Get automation workflow projects available to a connected user identified by external user id.

### Example

```ts
import {
  Configuration,
  AutomationWorkflowProjectApi,
} from '';
import type { GetProjectsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationWorkflowProjectApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetProjectsRequest;

  try {
    const data = await api.getProjects(body);
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
| **externalUserId** | `string` | The external user id. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**Array&lt;AutomationWorkflowProject&gt;**](AutomationWorkflowProject.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation workflow projects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

