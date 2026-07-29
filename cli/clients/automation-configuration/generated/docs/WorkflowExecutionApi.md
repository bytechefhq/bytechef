# WorkflowExecutionApi

All URIs are relative to */api/automation/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getWorkflowExecution**](WorkflowExecutionApi.md#getWorkflowExecution) | **GET** /workflow-executions/{id} | Get a workflow execution by id |
| [**getWorkflowExecutionWithHttpInfo**](WorkflowExecutionApi.md#getWorkflowExecutionWithHttpInfo) | **GET** /workflow-executions/{id} | Get a workflow execution by id |
| [**getWorkflowExecutionsPage**](WorkflowExecutionApi.md#getWorkflowExecutionsPage) | **GET** /workflow-executions | Get workflow executions |
| [**getWorkflowExecutionsPageWithHttpInfo**](WorkflowExecutionApi.md#getWorkflowExecutionsPageWithHttpInfo) | **GET** /workflow-executions | Get workflow executions |



## getWorkflowExecution

> WorkflowExecutionModel getWorkflowExecution(id)

Get a workflow execution by id

Get a workflow execution by id, including its inputs, outputs, error and task executions.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        WorkflowExecutionApi apiInstance = new WorkflowExecutionApi(defaultClient);
        Long id = 56L; // Long | The id of a workflow execution.
        try {
            WorkflowExecutionModel result = apiInstance.getWorkflowExecution(id);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling WorkflowExecutionApi#getWorkflowExecution");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **Long**| The id of a workflow execution. | |

### Return type

[**WorkflowExecutionModel**](WorkflowExecutionModel.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow execution object. |  -  |

## getWorkflowExecutionWithHttpInfo

> ApiResponse<WorkflowExecutionModel> getWorkflowExecutionWithHttpInfo(id)

Get a workflow execution by id

Get a workflow execution by id, including its inputs, outputs, error and task executions.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.ApiResponse;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        WorkflowExecutionApi apiInstance = new WorkflowExecutionApi(defaultClient);
        Long id = 56L; // Long | The id of a workflow execution.
        try {
            ApiResponse<WorkflowExecutionModel> response = apiInstance.getWorkflowExecutionWithHttpInfo(id);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling WorkflowExecutionApi#getWorkflowExecution");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **Long**| The id of a workflow execution. | |

### Return type

ApiResponse<[**WorkflowExecutionModel**](WorkflowExecutionModel.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The workflow execution object. |  -  |


## getWorkflowExecutionsPage

> PageModel getWorkflowExecutionsPage(workspaceId, xEnvironment, status, startDate, endDate, projectId, projectDeploymentId, workflowId, pageNumber)

Get workflow executions

Get the page of workflow executions of a workspace. Execution input, output and task data are never included in list responses - fetch a single execution by id for the full detail.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        WorkflowExecutionApi apiInstance = new WorkflowExecutionApi(defaultClient);
        Long workspaceId = 56L; // Long | The id of a workspace.
        EnvironmentModel xEnvironment = EnvironmentModel.fromValue("DEVELOPMENT"); // EnvironmentModel | The environment. Executions are filtered to the same environment the API key authenticated for; when omitted, PRODUCTION is used.
        WorkflowExecutionStatusModel status = WorkflowExecutionStatusModel.fromValue("CREATED"); // WorkflowExecutionStatusModel | The status of an execution.
        OffsetDateTime startDate = OffsetDateTime.now(); // OffsetDateTime | Return only executions that started at or after this instant.
        OffsetDateTime endDate = OffsetDateTime.now(); // OffsetDateTime | Return only executions that started at or before this instant.
        Long projectId = 56L; // Long | The id of a project.
        Long projectDeploymentId = 56L; // Long | The id of a project deployment.
        String workflowId = "workflowId_example"; // String | The id of a workflow.
        Integer pageNumber = 0; // Integer | The number of the page to return.
        try {
            PageModel result = apiInstance.getWorkflowExecutionsPage(workspaceId, xEnvironment, status, startDate, endDate, projectId, projectDeploymentId, workflowId, pageNumber);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling WorkflowExecutionApi#getWorkflowExecutionsPage");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **workspaceId** | **Long**| The id of a workspace. | |
| **xEnvironment** | [**EnvironmentModel**](.md)| The environment. Executions are filtered to the same environment the API key authenticated for; when omitted, PRODUCTION is used. | [optional] [enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **status** | [**WorkflowExecutionStatusModel**](.md)| The status of an execution. | [optional] [enum: CREATED, STARTED, STOPPED, CANCELLED, FAILED, COMPLETED] |
| **startDate** | **OffsetDateTime**| Return only executions that started at or after this instant. | [optional] |
| **endDate** | **OffsetDateTime**| Return only executions that started at or before this instant. | [optional] |
| **projectId** | **Long**| The id of a project. | [optional] |
| **projectDeploymentId** | **Long**| The id of a project deployment. | [optional] |
| **workflowId** | **String**| The id of a workflow. | [optional] |
| **pageNumber** | **Integer**| The number of the page to return. | [optional] [default to 0] |

### Return type

[**PageModel**](PageModel.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of workflow executions. |  -  |

## getWorkflowExecutionsPageWithHttpInfo

> ApiResponse<PageModel> getWorkflowExecutionsPageWithHttpInfo(workspaceId, xEnvironment, status, startDate, endDate, projectId, projectDeploymentId, workflowId, pageNumber)

Get workflow executions

Get the page of workflow executions of a workspace. Execution input, output and task data are never included in list responses - fetch a single execution by id for the full detail.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.ApiResponse;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        WorkflowExecutionApi apiInstance = new WorkflowExecutionApi(defaultClient);
        Long workspaceId = 56L; // Long | The id of a workspace.
        EnvironmentModel xEnvironment = EnvironmentModel.fromValue("DEVELOPMENT"); // EnvironmentModel | The environment. Executions are filtered to the same environment the API key authenticated for; when omitted, PRODUCTION is used.
        WorkflowExecutionStatusModel status = WorkflowExecutionStatusModel.fromValue("CREATED"); // WorkflowExecutionStatusModel | The status of an execution.
        OffsetDateTime startDate = OffsetDateTime.now(); // OffsetDateTime | Return only executions that started at or after this instant.
        OffsetDateTime endDate = OffsetDateTime.now(); // OffsetDateTime | Return only executions that started at or before this instant.
        Long projectId = 56L; // Long | The id of a project.
        Long projectDeploymentId = 56L; // Long | The id of a project deployment.
        String workflowId = "workflowId_example"; // String | The id of a workflow.
        Integer pageNumber = 0; // Integer | The number of the page to return.
        try {
            ApiResponse<PageModel> response = apiInstance.getWorkflowExecutionsPageWithHttpInfo(workspaceId, xEnvironment, status, startDate, endDate, projectId, projectDeploymentId, workflowId, pageNumber);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling WorkflowExecutionApi#getWorkflowExecutionsPage");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **workspaceId** | **Long**| The id of a workspace. | |
| **xEnvironment** | [**EnvironmentModel**](.md)| The environment. Executions are filtered to the same environment the API key authenticated for; when omitted, PRODUCTION is used. | [optional] [enum: DEVELOPMENT, STAGING, PRODUCTION] |
| **status** | [**WorkflowExecutionStatusModel**](.md)| The status of an execution. | [optional] [enum: CREATED, STARTED, STOPPED, CANCELLED, FAILED, COMPLETED] |
| **startDate** | **OffsetDateTime**| Return only executions that started at or after this instant. | [optional] |
| **endDate** | **OffsetDateTime**| Return only executions that started at or before this instant. | [optional] |
| **projectId** | **Long**| The id of a project. | [optional] |
| **projectDeploymentId** | **Long**| The id of a project deployment. | [optional] |
| **workflowId** | **String**| The id of a workflow. | [optional] |
| **pageNumber** | **Integer**| The number of the page to return. | [optional] [default to 0] |

### Return type

ApiResponse<[**PageModel**](PageModel.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of workflow executions. |  -  |

