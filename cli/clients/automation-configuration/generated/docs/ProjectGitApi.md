# ProjectGitApi

All URIs are relative to */api/automation/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**pullProjectFromGit**](ProjectGitApi.md#pullProjectFromGit) | **POST** /projects/{id}/git/pull | Pulls project from git repository. |
| [**pullProjectFromGitWithHttpInfo**](ProjectGitApi.md#pullProjectFromGitWithHttpInfo) | **POST** /projects/{id}/git/pull | Pulls project from git repository. |



## pullProjectFromGit

> void pullProjectFromGit(id)

Pulls project from git repository.

Pulls project from git repository.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.ProjectGitApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        ProjectGitApi apiInstance = new ProjectGitApi(defaultClient);
        Long id = 56L; // Long | The id of a project.
        try {
            apiInstance.pullProjectFromGit(id);
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectGitApi#pullProjectFromGit");
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
| **id** | **Long**| The id of a project. | |

### Return type


null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

## pullProjectFromGitWithHttpInfo

> ApiResponse<Void> pullProjectFromGitWithHttpInfo(id)

Pulls project from git repository.

Pulls project from git repository.

### Example

```java
// Import classes:
import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.ApiResponse;
import com.bytechef.cli.client.automation.Configuration;
import com.bytechef.cli.client.automation.models.*;
import com.bytechef.cli.client.automation.api.ProjectGitApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("/api/automation/v1");

        ProjectGitApi apiInstance = new ProjectGitApi(defaultClient);
        Long id = 56L; // Long | The id of a project.
        try {
            ApiResponse<Void> response = apiInstance.pullProjectFromGitWithHttpInfo(id);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (ApiException e) {
            System.err.println("Exception when calling ProjectGitApi#pullProjectFromGit");
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
| **id** | **Long**| The id of a project. | |

### Return type


ApiResponse<Void>

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

