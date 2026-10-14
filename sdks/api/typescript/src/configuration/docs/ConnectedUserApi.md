# ConnectedUserApi

All URIs are relative to */api/embedded/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**updateConnectedUser**](ConnectedUserApi.md#updateconnecteduser) | **PATCH** /{externalUserId} | Update data of an external user |
| [**updateFrontendConnectedUser**](ConnectedUserApi.md#updatefrontendconnecteduser) | **PATCH** /me | Update data of an external user |



## updateConnectedUser

> updateConnectedUser(externalUserId, xEnvironment, requestBody)

Update data of an external user

Update data of an external user.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { UpdateConnectedUserRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: bearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserApi(config);

  const body = {
    // string | The external user id.
    externalUserId: externalUserId_example,
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // { [key: string]: any; } (optional)
    requestBody: Object,
  } satisfies UpdateConnectedUserRequest;

  try {
    const data = await api.updateConnectedUser(body);
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
| **requestBody** | `{ [key: string]: any; }` |  | [Optional] |

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


## updateFrontendConnectedUser

> updateFrontendConnectedUser(xEnvironment, requestBody)

Update data of an external user

Update data of an external user.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { UpdateFrontendConnectedUserRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: jwtBearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ConnectedUserApi(config);

  const body = {
    // Environment | The environment. (optional)
    xEnvironment: ...,
    // { [key: string]: any; } (optional)
    requestBody: Object,
  } satisfies UpdateFrontendConnectedUserRequest;

  try {
    const data = await api.updateFrontendConnectedUser(body);
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
| **requestBody** | `{ [key: string]: any; }` |  | [Optional] |

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

