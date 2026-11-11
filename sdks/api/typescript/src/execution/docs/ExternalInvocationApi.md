# ExternalInvocationApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getExternalInvocationsPage**](ExternalInvocationApi.md#getexternalinvocationspage) | **GET** /{externalUserId}/external-invocations | Returns a page of the connected user\&#39;s external invocations |



## getExternalInvocationsPage

> ExternalInvocationPage getExternalInvocationsPage(externalUserId, xEnvironment, source, outcome, startDate, endDate, pageNumber)

Returns a page of the connected user\&#39;s external invocations

Returns a page of the connected user\&#39;s external invocations: embedded MCP component and workflow tool calls, executeAction calls and executeTool calls. Tool inputs and outputs are not included.

### Example

```ts
import {
  Configuration,
  ExternalInvocationApi,
} from '';
import type { GetExternalInvocationsPageRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ExternalInvocationApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // ExternalInvocationSource | Return only invocations made through this source. (optional)
    source: ...,
    // ExternalInvocationOutcome | Return only invocations with this outcome. (optional)
    outcome: ...,
    // Date | Return only invocations recorded at or after this instant. (optional)
    startDate: 2013-10-20T19:20:30+01:00,
    // Date | Return only invocations recorded at or before this instant. (optional)
    endDate: 2013-10-20T19:20:30+01:00,
    // number | The number of the page to return. (optional)
    pageNumber: 56,
  } satisfies GetExternalInvocationsPageRequest;

  try {
    const data = await api.getExternalInvocationsPage(body);
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
| **source** | `ExternalInvocationSource` | Return only invocations made through this source. | [Optional] [Defaults to `undefined`] [Enum: MCP_SERVER, API_ACTION, API_TOOL] |
| **outcome** | `ExternalInvocationOutcome` | Return only invocations with this outcome. | [Optional] [Defaults to `undefined`] [Enum: SUCCESS, ERROR, CONNECTION_REQUIRED, TIMEOUT, APPROVAL_REQUIRED, APPROVAL_DENIED, RULE_BLOCKED, SUSPENDED] |
| **startDate** | `Date` | Return only invocations recorded at or after this instant. | [Optional] [Defaults to `undefined`] |
| **endDate** | `Date` | Return only invocations recorded at or before this instant. | [Optional] [Defaults to `undefined`] |
| **pageNumber** | `number` | The number of the page to return. | [Optional] [Defaults to `0`] |

### Return type

[**ExternalInvocationPage**](ExternalInvocationPage.md)

### Authorization

[jwtBearerAuth](../README.md#jwtBearerAuth), [bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of external invocations. |  -  |
| **400** | Bad request |  -  |
| **401** | Unauthorized |  -  |
| **403** | Forbidden |  -  |
| **404** | Not found |  -  |
| **500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

