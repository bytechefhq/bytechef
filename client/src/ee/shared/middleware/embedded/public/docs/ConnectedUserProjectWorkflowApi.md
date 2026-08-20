# ConnectedUserProjectWorkflowApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**copyFrontendWorkflowTemplate**](ConnectedUserProjectWorkflowApi.md#copyfrontendworkflowtemplate) | **POST** /automation/workflow-templates/{workflowUuid}/copy | Copy a catalog workflow template into a new user workflow |
| [**copyWorkflowTemplate**](ConnectedUserProjectWorkflowApi.md#copyworkflowtemplate) | **POST** /{externalUserId}/automation/workflow-templates/{workflowUuid}/copy | Copy a catalog workflow template into a new user workflow |
| [**createFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#createfrontendprojectworkflowoperation) | **POST** /automation/workflows | Create new workflow and adds it to an existing integration |
| [**createFrontendProjectWorkflowFromPrompt**](ConnectedUserProjectWorkflowApi.md#createfrontendprojectworkflowfrompromptoperation) | **POST** /automation/workflows/generate | Generate a new workflow from a natural language prompt |
| [**createProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#createprojectworkflow) | **POST** /{externalUserId}/automation/workflows | Create new workflow and adds it to an existing integration |
| [**createProjectWorkflowFromPrompt**](ConnectedUserProjectWorkflowApi.md#createprojectworkflowfromprompt) | **POST** /{externalUserId}/automation/workflows/generate | Generate a new workflow from a natural language prompt |
| [**deleteFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#deletefrontendprojectworkflow) | **DELETE** /automation/workflows/{workflowUuid} | Delete a workflow |
| [**deleteProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#deleteprojectworkflow) | **DELETE** /{externalUserId}/automation/workflows/{workflowUuid} | Delete a workflow |
| [**deprovisionFrontendWorkflowReference**](ConnectedUserProjectWorkflowApi.md#deprovisionfrontendworkflowreference) | **DELETE** /automation/workflow-templates/{workflowUuid}/provision | De-provision a workflow reference |
| [**deprovisionWorkflowReference**](ConnectedUserProjectWorkflowApi.md#deprovisionworkflowreference) | **DELETE** /{externalUserId}/automation/workflow-templates/{workflowUuid}/provision | De-provision a reference to a catalog code workflow |
| [**disableFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#disablefrontendprojectworkflow) | **DELETE** /automation/workflows/{workflowUuid}/enable | Disable a workflow |
| [**disableProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#disableprojectworkflow) | **DELETE** /{externalUserId}/automation/workflows/{workflowUuid}/enable | Disable a workflow |
| [**enableFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#enablefrontendprojectworkflow) | **POST** /automation/workflows/{workflowUuid}/enable | Enable a workflow |
| [**enableProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#enableprojectworkflow) | **POST** /{externalUserId}/automation/workflows/{workflowUuid}/enable | Enable a workflow |
| [**getFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#getfrontendprojectworkflow) | **GET** /automation/workflows/{workflowUuid} | Get a workflow by workflow reference code |
| [**getFrontendProjectWorkflows**](ConnectedUserProjectWorkflowApi.md#getfrontendprojectworkflows) | **GET** /automation/workflows | Get automation workflows for particular external user |
| [**getProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#getprojectworkflow) | **GET** /{externalUserId}/automation/workflows/{workflowUuid} | Get a workflow by workflow reference code |
| [**getProjectWorkflows**](ConnectedUserProjectWorkflowApi.md#getprojectworkflows) | **GET** /{externalUserId}/automation/workflows | Get automation workflows for particular external user |
| [**provisionFrontendWorkflowReference**](ConnectedUserProjectWorkflowApi.md#provisionfrontendworkflowreference) | **POST** /automation/workflow-templates/{workflowUuid}/provision | Provision a workflow reference |
| [**provisionWorkflowReference**](ConnectedUserProjectWorkflowApi.md#provisionworkflowreference) | **POST** /{externalUserId}/automation/workflow-templates/{workflowUuid}/provision | Provision a reference to a catalog code workflow |
| [**publishFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#publishfrontendprojectworkflowoperation) | **POST** /automation/workflows/{workflowUuid}/publish | Publishes existing workflow |
| [**publishProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#publishprojectworkflow) | **POST** /{externalUserId}/automation/workflows/{workflowUuid}/publish | Publishes existing workflow |
| [**updateFrontendProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#updatefrontendprojectworkflow) | **PUT** /automation/workflows/{workflowUuid} | Update an existing workflow |
| [**updateFrontendProjectWorkflowFromPrompt**](ConnectedUserProjectWorkflowApi.md#updatefrontendprojectworkflowfromprompt) | **POST** /automation/workflows/{workflowUuid}/generate | Update an existing workflow from a natural language prompt |
| [**updateFrontendWorkflowConfigurationConnection**](ConnectedUserProjectWorkflowApi.md#updatefrontendworkflowconfigurationconnectionoperation) | **PUT** /automation/workflows/{workflowUuid}/workflow-nodes/{workflowNodeName}/connection/{workflowConnectionKey} | Update a workflow configuration connection |
| [**updateProjectWorkflow**](ConnectedUserProjectWorkflowApi.md#updateprojectworkflow) | **PUT** /{externalUserId}/automation/workflows/{workflowUuid} | Update an existing workflow |
| [**updateProjectWorkflowFromPrompt**](ConnectedUserProjectWorkflowApi.md#updateprojectworkflowfromprompt) | **POST** /{externalUserId}/automation/workflows/{workflowUuid}/generate | Update an existing workflow from a natural language prompt |
| [**updateWorkflowConfigurationConnection**](ConnectedUserProjectWorkflowApi.md#updateworkflowconfigurationconnection) | **PUT** /{externalUserId}/automation/workflows/{workflowUuid}/workflow-nodes/{workflowNodeName}/connections/{workflowConnectionKey} | Update a workflow configuration connection |



## copyFrontendWorkflowTemplate

> string copyFrontendWorkflowTemplate(workflowUuid, xEnvironment)

Copy a catalog workflow template into a new user workflow

Copy a catalog workflow template into a new user workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CopyFrontendWorkflowTemplateRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CopyFrontendWorkflowTemplateRequest;

  try {
    const data = await api.copyFrontendWorkflowTemplate(body);
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

**string**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The new workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## copyWorkflowTemplate

> string copyWorkflowTemplate(externalUserId, workflowUuid, xEnvironment)

Copy a catalog workflow template into a new user workflow

Copy a catalog workflow template into a new user workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CopyWorkflowTemplateRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CopyWorkflowTemplateRequest;

  try {
    const data = await api.copyWorkflowTemplate(body);
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

**string**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The new workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createFrontendProjectWorkflow

> string createFrontendProjectWorkflow(createFrontendProjectWorkflowRequest, xEnvironment)

Create new workflow and adds it to an existing integration

Create new workflow and adds it to an existing integration.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CreateFrontendProjectWorkflowOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // CreateFrontendProjectWorkflowRequest
    createFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateFrontendProjectWorkflowOperationRequest;

  try {
    const data = await api.createFrontendProjectWorkflow(body);
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
| **createFrontendProjectWorkflowRequest** | [CreateFrontendProjectWorkflowRequest](CreateFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createFrontendProjectWorkflowFromPrompt

> string createFrontendProjectWorkflowFromPrompt(createFrontendProjectWorkflowFromPromptRequest, xEnvironment)

Generate a new workflow from a natural language prompt

Generate a new workflow for the connected user from a natural language prompt using AI Copilot.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CreateFrontendProjectWorkflowFromPromptOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // CreateFrontendProjectWorkflowFromPromptRequest
    createFrontendProjectWorkflowFromPromptRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateFrontendProjectWorkflowFromPromptOperationRequest;

  try {
    const data = await api.createFrontendProjectWorkflowFromPrompt(body);
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
| **createFrontendProjectWorkflowFromPromptRequest** | [CreateFrontendProjectWorkflowFromPromptRequest](CreateFrontendProjectWorkflowFromPromptRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The new workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createProjectWorkflow

> string createProjectWorkflow(externalUserId, createFrontendProjectWorkflowRequest, xEnvironment)

Create new workflow and adds it to an existing integration

Create new workflow and adds it to an existing integration.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CreateProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // CreateFrontendProjectWorkflowRequest
    createFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateProjectWorkflowRequest;

  try {
    const data = await api.createProjectWorkflow(body);
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
| **createFrontendProjectWorkflowRequest** | [CreateFrontendProjectWorkflowRequest](CreateFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createProjectWorkflowFromPrompt

> string createProjectWorkflowFromPrompt(externalUserId, createFrontendProjectWorkflowFromPromptRequest, xEnvironment)

Generate a new workflow from a natural language prompt

Generate a new workflow for a connected user identified by external user id from a natural language prompt using AI Copilot.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { CreateProjectWorkflowFromPromptRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // CreateFrontendProjectWorkflowFromPromptRequest
    createFrontendProjectWorkflowFromPromptRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies CreateProjectWorkflowFromPromptRequest;

  try {
    const data = await api.createProjectWorkflowFromPrompt(body);
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
| **createFrontendProjectWorkflowFromPromptRequest** | [CreateFrontendProjectWorkflowFromPromptRequest](CreateFrontendProjectWorkflowFromPromptRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The new workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteFrontendProjectWorkflow

> deleteFrontendProjectWorkflow(workflowUuid, xEnvironment)

Delete a workflow

Delete a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DeleteFrontendProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DeleteFrontendProjectWorkflowRequest;

  try {
    const data = await api.deleteFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

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
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteProjectWorkflow

> deleteProjectWorkflow(externalUserId, workflowUuid, xEnvironment)

Delete a workflow

Delete a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DeleteProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DeleteProjectWorkflowRequest;

  try {
    const data = await api.deleteProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

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
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deprovisionFrontendWorkflowReference

> deprovisionFrontendWorkflowReference(workflowUuid, xEnvironment)

De-provision a workflow reference

De-provision a reference to a catalog code workflow for the authenticated connected user.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DeprovisionFrontendWorkflowReferenceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DeprovisionFrontendWorkflowReferenceRequest;

  try {
    const data = await api.deprovisionFrontendWorkflowReference(body);
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

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deprovisionWorkflowReference

> deprovisionWorkflowReference(externalUserId, workflowUuid, xEnvironment)

De-provision a reference to a catalog code workflow

De-provision a reference to a catalog code workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DeprovisionWorkflowReferenceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DeprovisionWorkflowReferenceRequest;

  try {
    const data = await api.deprovisionWorkflowReference(body);
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

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## disableFrontendProjectWorkflow

> object disableFrontendProjectWorkflow(workflowUuid, xEnvironment)

Disable a workflow

Disable a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DisableFrontendProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DisableFrontendProjectWorkflowRequest;

  try {
    const data = await api.disableFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## disableProjectWorkflow

> object disableProjectWorkflow(externalUserId, workflowUuid, xEnvironment)

Disable a workflow

Disable a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { DisableProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies DisableProjectWorkflowRequest;

  try {
    const data = await api.disableProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableFrontendProjectWorkflow

> object enableFrontendProjectWorkflow(workflowUuid, xEnvironment)

Enable a workflow

Enable a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { EnableFrontendProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies EnableFrontendProjectWorkflowRequest;

  try {
    const data = await api.enableFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enableProjectWorkflow

> object enableProjectWorkflow(externalUserId, workflowUuid, xEnvironment)

Enable a workflow

Enable a workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { EnableProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies EnableProjectWorkflowRequest;

  try {
    const data = await api.enableProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendProjectWorkflow

> ConnectedUserProjectWorkflow getFrontendProjectWorkflow(workflowUuid, xEnvironment)

Get a workflow by workflow reference code

Get a workflow by workflow reference code.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { GetFrontendProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendProjectWorkflowRequest;

  try {
    const data = await api.getFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**ConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getFrontendProjectWorkflows

> Array&lt;ConnectedUserProjectWorkflow&gt; getFrontendProjectWorkflows(xEnvironment)

Get automation workflows for particular external user

Get automation workflows for particular external user.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { GetFrontendProjectWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetFrontendProjectWorkflowsRequest;

  try {
    const data = await api.getFrontendProjectWorkflows(body);
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

[**Array&lt;ConnectedUserProjectWorkflow&gt;**](ConnectedUserProjectWorkflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectWorkflow

> ConnectedUserProjectWorkflow getProjectWorkflow(externalUserId, workflowUuid, xEnvironment)

Get a workflow by workflow reference code

Get a workflow by workflow reference code.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { GetProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetProjectWorkflowRequest;

  try {
    const data = await api.getProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

[**ConnectedUserProjectWorkflow**](ConnectedUserProjectWorkflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProjectWorkflows

> Array&lt;ConnectedUserProjectWorkflow&gt; getProjectWorkflows(externalUserId, xEnvironment)

Get automation workflows for particular external user

Get automation workflows for particular external user.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { GetProjectWorkflowsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserProjectWorkflowApi();

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies GetProjectWorkflowsRequest;

  try {
    const data = await api.getProjectWorkflows(body);
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

[**Array&lt;ConnectedUserProjectWorkflow&gt;**](ConnectedUserProjectWorkflow.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## provisionFrontendWorkflowReference

> provisionFrontendWorkflowReference(workflowUuid, xEnvironment)

Provision a workflow reference

Provision a reference to a catalog code workflow for the authenticated connected user.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { ProvisionFrontendWorkflowReferenceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ProvisionFrontendWorkflowReferenceRequest;

  try {
    const data = await api.provisionFrontendWorkflowReference(body);
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

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## provisionWorkflowReference

> object provisionWorkflowReference(externalUserId, workflowUuid, xEnvironment)

Provision a reference to a catalog code workflow

Explicitly provision a reference to a catalog code workflow ahead of first invocation.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { ProvisionWorkflowReferenceRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow template uuid.
    workflowUuid: workflowUuid_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies ProvisionWorkflowReferenceRequest;

  try {
    const data = await api.provisionWorkflowReference(body);
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

**object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |
| **409** | A required connection could not be auto-wired. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishFrontendProjectWorkflow

> publishFrontendProjectWorkflow(workflowUuid, publishFrontendProjectWorkflowRequest, xEnvironment)

Publishes existing workflow

Publishes existing workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { PublishFrontendProjectWorkflowOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // PublishFrontendProjectWorkflowRequest
    publishFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies PublishFrontendProjectWorkflowOperationRequest;

  try {
    const data = await api.publishFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **publishFrontendProjectWorkflowRequest** | [PublishFrontendProjectWorkflowRequest](PublishFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## publishProjectWorkflow

> publishProjectWorkflow(externalUserId, workflowUuid, publishFrontendProjectWorkflowRequest, xEnvironment)

Publishes existing workflow

Publishes existing workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { PublishProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow uuid.
    workflowUuid: workflowUuid_example,
    // PublishFrontendProjectWorkflowRequest
    publishFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies PublishProjectWorkflowRequest;

  try {
    const data = await api.publishProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow uuid. | [Defaults to `undefined`] |
| **publishFrontendProjectWorkflowRequest** | [PublishFrontendProjectWorkflowRequest](PublishFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateFrontendProjectWorkflow

> updateFrontendProjectWorkflow(workflowUuid, createFrontendProjectWorkflowRequest, xEnvironment)

Update an existing workflow

Update an existing workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateFrontendProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // CreateFrontendProjectWorkflowRequest
    createFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateFrontendProjectWorkflowRequest;

  try {
    const data = await api.updateFrontendProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **createFrontendProjectWorkflowRequest** | [CreateFrontendProjectWorkflowRequest](CreateFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateFrontendProjectWorkflowFromPrompt

> string updateFrontendProjectWorkflowFromPrompt(workflowUuid, createFrontendProjectWorkflowFromPromptRequest, xEnvironment)

Update an existing workflow from a natural language prompt

Update an existing workflow for the connected user from a natural language prompt using AI Copilot.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateFrontendProjectWorkflowFromPromptRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The uuid of the workflow to update.
    workflowUuid: workflowUuid_example,
    // CreateFrontendProjectWorkflowFromPromptRequest
    createFrontendProjectWorkflowFromPromptRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateFrontendProjectWorkflowFromPromptRequest;

  try {
    const data = await api.updateFrontendProjectWorkflowFromPrompt(body);
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
| **workflowUuid** | `string` | The uuid of the workflow to update. | [Defaults to `undefined`] |
| **createFrontendProjectWorkflowFromPromptRequest** | [CreateFrontendProjectWorkflowFromPromptRequest](CreateFrontendProjectWorkflowFromPromptRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateFrontendWorkflowConfigurationConnection

> updateFrontendWorkflowConfigurationConnection(workflowUuid, workflowNodeName, workflowConnectionKey, updateFrontendWorkflowConfigurationConnectionRequest, xEnvironment)

Update a workflow configuration connection

Update a workflow configuration connection.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateFrontendWorkflowConfigurationConnectionOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // string | The action/trigger name defined in the workflow.
    workflowNodeName: workflowNodeName_example,
    // string | The name of a workflow connection key.
    workflowConnectionKey: workflowConnectionKey_example,
    // UpdateFrontendWorkflowConfigurationConnectionRequest
    updateFrontendWorkflowConfigurationConnectionRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateFrontendWorkflowConfigurationConnectionOperationRequest;

  try {
    const data = await api.updateFrontendWorkflowConfigurationConnection(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The action/trigger name defined in the workflow. | [Defaults to `undefined`] |
| **workflowConnectionKey** | `string` | The name of a workflow connection key. | [Defaults to `undefined`] |
| **updateFrontendWorkflowConfigurationConnectionRequest** | [UpdateFrontendWorkflowConfigurationConnectionRequest](UpdateFrontendWorkflowConfigurationConnectionRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateProjectWorkflow

> updateProjectWorkflow(externalUserId, workflowUuid, createFrontendProjectWorkflowRequest, xEnvironment)

Update an existing workflow

Update an existing workflow.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateProjectWorkflowRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // CreateFrontendProjectWorkflowRequest
    createFrontendProjectWorkflowRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateProjectWorkflowRequest;

  try {
    const data = await api.updateProjectWorkflow(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **createFrontendProjectWorkflowRequest** | [CreateFrontendProjectWorkflowRequest](CreateFrontendProjectWorkflowRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateProjectWorkflowFromPrompt

> string updateProjectWorkflowFromPrompt(externalUserId, workflowUuid, createFrontendProjectWorkflowFromPromptRequest, xEnvironment)

Update an existing workflow from a natural language prompt

Update an existing workflow for a connected user identified by external user id from a natural language prompt using AI Copilot.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateProjectWorkflowFromPromptRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The uuid of the workflow to update.
    workflowUuid: workflowUuid_example,
    // CreateFrontendProjectWorkflowFromPromptRequest
    createFrontendProjectWorkflowFromPromptRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateProjectWorkflowFromPromptRequest;

  try {
    const data = await api.updateProjectWorkflowFromPrompt(body);
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
| **workflowUuid** | `string` | The uuid of the workflow to update. | [Defaults to `undefined`] |
| **createFrontendProjectWorkflowFromPromptRequest** | [CreateFrontendProjectWorkflowFromPromptRequest](CreateFrontendProjectWorkflowFromPromptRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

**string**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated workflow uuid. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateWorkflowConfigurationConnection

> updateWorkflowConfigurationConnection(externalUserId, workflowUuid, workflowNodeName, workflowConnectionKey, updateFrontendWorkflowConfigurationConnectionRequest, xEnvironment)

Update a workflow configuration connection

Update a workflow configuration connection.

### Example

```ts
import {
  Configuration,
  ConnectedUserProjectWorkflowApi,
} from '';
import type { UpdateWorkflowConfigurationConnectionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserProjectWorkflowApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // string | The workflow reference of the workflow to delete.
    workflowUuid: workflowUuid_example,
    // string | The action/trigger name defined in the workflow.
    workflowNodeName: workflowNodeName_example,
    // string | The name of a workflow connection key.
    workflowConnectionKey: workflowConnectionKey_example,
    // UpdateFrontendWorkflowConfigurationConnectionRequest
    updateFrontendWorkflowConfigurationConnectionRequest: ...,
    // Environment | The environment. (optional)
    xEnvironment: ...,
  } satisfies UpdateWorkflowConfigurationConnectionRequest;

  try {
    const data = await api.updateWorkflowConfigurationConnection(body);
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
| **workflowUuid** | `string` | The workflow reference of the workflow to delete. | [Defaults to `undefined`] |
| **workflowNodeName** | `string` | The action/trigger name defined in the workflow. | [Defaults to `undefined`] |
| **workflowConnectionKey** | `string` | The name of a workflow connection key. | [Defaults to `undefined`] |
| **updateFrontendWorkflowConfigurationConnectionRequest** | [UpdateFrontendWorkflowConfigurationConnectionRequest](UpdateFrontendWorkflowConfigurationConnectionRequest.md) |  | |
| **xEnvironment** | `Environment` | The environment. | [Optional] [Defaults to `undefined`] [Enum: DEVELOPMENT, STAGING, PRODUCTION] |

### Return type

`void` (Empty response body)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

