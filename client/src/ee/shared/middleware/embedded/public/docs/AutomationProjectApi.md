# AutomationProjectApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getFrontendProjects**](AutomationProjectApi.md#getfrontendprojects) | **GET** /automation/projects | Get automation projects available to the connected user |
| [**getFrontendWorkflowTemplate**](AutomationProjectApi.md#getfrontendworkflowtemplate) | **GET** /automation/workflow-templates/{workflowUuid} | Get an automation workflow template available to the connected user |
| [**getProjects**](AutomationProjectApi.md#getprojects) | **GET** /{externalUserId}/automation/projects | Get automation projects available to a connected user identified by external user id |
| [**getWorkflowTemplate**](AutomationProjectApi.md#getworkflowtemplate) | **GET** /{externalUserId}/automation/workflow-templates/{workflowUuid} | Get an automation workflow template available to a connected user identified by external user id |



## getFrontendProjects

> Array&lt;AutomationProject&gt; getFrontendProjects(xEnvironment)

Get automation projects available to the connected user

Get automation projects available to the connected user.

### Example

```ts
import {
  Configuration,
  AutomationProjectApi,
} from '';
import type { GetFrontendProjectsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectApi(config);

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

[**Array&lt;AutomationProject&gt;**](AutomationProject.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation projects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendWorkflowTemplate

> AutomationProjectWorkflowTemplate getFrontendWorkflowTemplate(workflowUuid, xEnvironment)

Get an automation workflow template available to the connected user

Get an automation workflow template available to the connected user by its workflow uuid. Unlike the project listing, it also returns a template of a project not listed in the Automation Hub.

### Example

```ts
import {
  Configuration,
  AutomationProjectApi,
} from '';
import type { GetFrontendWorkflowTemplateRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectApi(config);

  const body = {
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendWorkflowTemplateRequest;

  try {
    const data = await api.getFrontendWorkflowTemplate(body);
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
| **workflowUuid** | `string` | The workflow template uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**AutomationProjectWorkflowTemplate**](AutomationProjectWorkflowTemplate.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The automation workflow template. |  -  |
| **404** | The template does not exist or is not available to the connected user. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjects

> Array&lt;AutomationProject&gt; getProjects(externalUserId, xEnvironment)

Get automation projects available to a connected user identified by external user id

Get automation projects available to a connected user identified by external user id.

### Example

```ts
import {
  Configuration,
  AutomationProjectApi,
} from '';
import type { GetProjectsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectApi(config);

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

[**Array&lt;AutomationProject&gt;**](AutomationProject.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation projects. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getWorkflowTemplate

> AutomationProjectWorkflowTemplate getWorkflowTemplate(externalUserId, workflowUuid, xEnvironment)

Get an automation workflow template available to a connected user identified by external user id

Get an automation workflow template available to a connected user identified by external user id by its workflow uuid. Unlike the project listing, it also returns a template of a project not listed in the Automation Hub.

### Example

```ts
import {
  Configuration,
  AutomationProjectApi,
} from '';
import type { GetWorkflowTemplateRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AutomationProjectApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetWorkflowTemplateRequest;

  try {
    const data = await api.getWorkflowTemplate(body);
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
| **workflowUuid** | `string` | The workflow template uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**AutomationProjectWorkflowTemplate**](AutomationProjectWorkflowTemplate.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The automation workflow template. |  -  |
| **404** | The template does not exist or is not available to the connected user. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

