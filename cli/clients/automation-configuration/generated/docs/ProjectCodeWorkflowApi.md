# ProjectCodeWorkflowApi

All URIs are relative to */api/automation/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deployProject**](ProjectCodeWorkflowApi.md#deployProject) | **POST** /projects/deploy | Deploy a new code based project |
| [**deployProjectWithHttpInfo**](ProjectCodeWorkflowApi.md#deployProjectWithHttpInfo) | **POST** /projects/deploy | Deploy a new code based project |



## deployProject

> void deployProject(workspaceId, projectFile)

Deploy a new code based project

Deploy a new code based project.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.ProjectCodeWorkflowApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        ProjectCodeWorkflowApi apiInstance = new ProjectCodeWorkflowApi(defaultClient);
        Long workspaceId = 56L; // Long | The id of a workspace this project will belong.
        File projectFile = new File("/path/to/file"); // File | The file of a code-native project.
        try {
            apiInstance.deployProject(workspaceId, projectFile);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectCodeWorkflowApi#deployProject");
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
| **workspaceId** | **Long**| The id of a workspace this project will belong. | [optional] |
| **projectFile** | **File**| The file of a code-native project. | [optional] |

### Return type


null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

## deployProjectWithHttpInfo

> ApiResponse<Void> deployProjectWithHttpInfo(workspaceId, projectFile)

Deploy a new code based project

Deploy a new code based project.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.ApiResponse;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.ProjectCodeWorkflowApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        ProjectCodeWorkflowApi apiInstance = new ProjectCodeWorkflowApi(defaultClient);
        Long workspaceId = 56L; // Long | The id of a workspace this project will belong.
        File projectFile = new File("/path/to/file"); // File | The file of a code-native project.
        try {
            ApiResponse<Void> response = apiInstance.deployProjectWithHttpInfo(workspaceId, projectFile);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectCodeWorkflowApi#deployProject");
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
| **workspaceId** | **Long**| The id of a workspace this project will belong. | [optional] |
| **projectFile** | **File**| The file of a code-native project. | [optional] |

### Return type


ApiResponse<Void>

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

