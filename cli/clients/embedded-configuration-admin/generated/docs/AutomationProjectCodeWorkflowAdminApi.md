# AutomationProjectCodeWorkflowAdminApi

All URIs are relative to */api/platform/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployAutomationProjectCodeWorkflow**](AutomationProjectCodeWorkflowAdminApi.md#deployAutomationProjectCodeWorkflow) | **POST** /automation-project-code-workflows/deploy | Deploy a new automation code workflow into the embedded catalog |
| [**deployAutomationProjectCodeWorkflowWithHttpInfo**](AutomationProjectCodeWorkflowAdminApi.md#deployAutomationProjectCodeWorkflowWithHttpInfo) | **POST** /automation-project-code-workflows/deploy | Deploy a new automation code workflow into the embedded catalog |
| [**listAutomationProjectCodeWorkflows**](AutomationProjectCodeWorkflowAdminApi.md#listAutomationProjectCodeWorkflows) | **GET** /automation-project-code-workflows | List catalog projects in the embedded automation bridge |
| [**listAutomationProjectCodeWorkflowsWithHttpInfo**](AutomationProjectCodeWorkflowAdminApi.md#listAutomationProjectCodeWorkflowsWithHttpInfo) | **GET** /automation-project-code-workflows | List catalog projects in the embedded automation bridge |



## deployAutomationProjectCodeWorkflow

> AutomationProjectCodeWorkflowDeployResultModel deployAutomationProjectCodeWorkflow(projectFile)

Deploy a new automation code workflow into the embedded catalog

Deploy a new automation code workflow into the embedded catalog. Unlike the connected-user-scoped embedded internal endpoint, this operation is reachable with a plain platform API-key bearer token.

### Example

```java
// Import classes:
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiClient;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiException;
import com.bytechef.cli.client.embeddedconfigurationadmin.Configuration;
import com.bytechef.cli.client.embeddedconfigurationadmin.models.*;
import com.bytechef.cli.client.embeddedconfigurationadmin.api.AutomationProjectCodeWorkflowAdminApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/platform/v1");

        AutomationProjectCodeWorkflowAdminApi apiInstance = new AutomationProjectCodeWorkflowAdminApi(defaultClient);
        File projectFile = new File("/path/to/file"); // File | The file of a code-native automation project.
        try {
            AutomationProjectCodeWorkflowDeployResultModel result = apiInstance.deployAutomationProjectCodeWorkflow(projectFile);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AutomationProjectCodeWorkflowAdminApi#deployAutomationProjectCodeWorkflow");
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
| **projectFile** | **File**| The file of a code-native automation project. | [optional] |

### Return type

[**AutomationProjectCodeWorkflowDeployResultModel**](AutomationProjectCodeWorkflowDeployResultModel.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

## deployAutomationProjectCodeWorkflowWithHttpInfo

> ApiResponse<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflowWithHttpInfo(projectFile)

Deploy a new automation code workflow into the embedded catalog

Deploy a new automation code workflow into the embedded catalog. Unlike the connected-user-scoped embedded internal endpoint, this operation is reachable with a plain platform API-key bearer token.

### Example

```java
// Import classes:
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiClient;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiException;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiResponse;
import com.bytechef.cli.client.embeddedconfigurationadmin.Configuration;
import com.bytechef.cli.client.embeddedconfigurationadmin.models.*;
import com.bytechef.cli.client.embeddedconfigurationadmin.api.AutomationProjectCodeWorkflowAdminApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/platform/v1");

        AutomationProjectCodeWorkflowAdminApi apiInstance = new AutomationProjectCodeWorkflowAdminApi(defaultClient);
        File projectFile = new File("/path/to/file"); // File | The file of a code-native automation project.
        try {
            ApiResponse<AutomationProjectCodeWorkflowDeployResultModel> response = apiInstance.deployAutomationProjectCodeWorkflowWithHttpInfo(projectFile);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AutomationProjectCodeWorkflowAdminApi#deployAutomationProjectCodeWorkflow");
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
| **projectFile** | **File**| The file of a code-native automation project. | [optional] |

### Return type

ApiResponse<[**AutomationProjectCodeWorkflowDeployResultModel**](AutomationProjectCodeWorkflowDeployResultModel.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |


## listAutomationProjectCodeWorkflows

> List<AutomationWorkflowProjectModel> listAutomationProjectCodeWorkflows()

List catalog projects in the embedded automation bridge

List catalog projects in the embedded automation bridge. Unlike the connected-user-scoped embedded public endpoint, this operation is reachable with a plain platform API-key bearer token and does not fabricate a connected-user identity.

### Example

```java
// Import classes:
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiClient;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiException;
import com.bytechef.cli.client.embeddedconfigurationadmin.Configuration;
import com.bytechef.cli.client.embeddedconfigurationadmin.models.*;
import com.bytechef.cli.client.embeddedconfigurationadmin.api.AutomationProjectCodeWorkflowAdminApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/platform/v1");

        AutomationProjectCodeWorkflowAdminApi apiInstance = new AutomationProjectCodeWorkflowAdminApi(defaultClient);
        try {
            List<AutomationWorkflowProjectModel> result = apiInstance.listAutomationProjectCodeWorkflows();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AutomationProjectCodeWorkflowAdminApi#listAutomationProjectCodeWorkflows");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**List&lt;AutomationWorkflowProjectModel&gt;**](AutomationWorkflowProjectModel.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation workflow projects. |  -  |

## listAutomationProjectCodeWorkflowsWithHttpInfo

> ApiResponse<List<AutomationWorkflowProjectModel>> listAutomationProjectCodeWorkflowsWithHttpInfo()

List catalog projects in the embedded automation bridge

List catalog projects in the embedded automation bridge. Unlike the connected-user-scoped embedded public endpoint, this operation is reachable with a plain platform API-key bearer token and does not fabricate a connected-user identity.

### Example

```java
// Import classes:
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiClient;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiException;
import com.bytechef.cli.client.embeddedconfigurationadmin.ApiResponse;
import com.bytechef.cli.client.embeddedconfigurationadmin.Configuration;
import com.bytechef.cli.client.embeddedconfigurationadmin.models.*;
import com.bytechef.cli.client.embeddedconfigurationadmin.api.AutomationProjectCodeWorkflowAdminApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/platform/v1");

        AutomationProjectCodeWorkflowAdminApi apiInstance = new AutomationProjectCodeWorkflowAdminApi(defaultClient);
        try {
            ApiResponse<List<AutomationWorkflowProjectModel>> response = apiInstance.listAutomationProjectCodeWorkflowsWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling AutomationProjectCodeWorkflowAdminApi#listAutomationProjectCodeWorkflows");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

ApiResponse<[**List&lt;AutomationWorkflowProjectModel&gt;**](AutomationWorkflowProjectModel.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of automation workflow projects. |  -  |

