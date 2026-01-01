# ConnectedUserApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteConnectedUser**](ConnectedUserApi.md#deleteconnecteduser) | **DELETE** /connected-users/{id} | Delete a connected user |
| [**enableConnectedUser**](ConnectedUserApi.md#enableconnecteduser) | **PATCH** /connected-users/{id}/enable/{enable} | Enable/disable a connected user |
| [**getConnectedUser**](ConnectedUserApi.md#getconnecteduser) | **GET** /connected-users/{id} | Get a connected user |
| [**getConnectedUsers**](ConnectedUserApi.md#getconnectedusers) | **GET** /connected-users | Get all connected users |



## deleteConnectedUser

> deleteConnectedUser(id)

Delete a connected user

Delete a connected user.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { DeleteConnectedUserRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserApi();

  const body = {
    // number | The id of an integration instance configuration.
    id: 789,
  } satisfies DeleteConnectedUserRequest;

  try {
    const data = await api.deleteConnectedUser(body);
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
| **id** | `number` | The id of an integration instance configuration. | [Defaults to `undefined`] |

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


## enableConnectedUser

> enableConnectedUser(id, enable)

Enable/disable a connected user

Enable/disable a connected user.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { EnableConnectedUserRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserApi();

  const body = {
    // number | The id of a connected user.
    id: 789,
    // boolean | Enable/disable the connected user.
    enable: true,
  } satisfies EnableConnectedUserRequest;

  try {
    const data = await api.enableConnectedUser(body);
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
| **id** | `number` | The id of a connected user. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the connected user. | [Defaults to `undefined`] |

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


## getConnectedUser

> ConnectedUser getConnectedUser(id)

Get a connected user

Get a connected user.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { GetConnectedUserRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserApi();

  const body = {
    // number | The id of an integration instance.
    id: 789,
  } satisfies GetConnectedUserRequest;

  try {
    const data = await api.getConnectedUser(body);
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
| **id** | `number` | The id of an integration instance. | [Defaults to `undefined`] |

### Return type

[**ConnectedUser**](ConnectedUser.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A connection object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getConnectedUsers

> Page getConnectedUsers(environmentId, credentialStatus, createDateFrom, createDateTo, integrationId, pageNumber, search)

Get all connected users

Get all connected users.

### Example

```ts
import {
  Configuration,
  ConnectedUserApi,
} from '';
import type { GetConnectedUsersRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new ConnectedUserApi();

  const body = {
    // number | The environment id. (optional)
    environmentId: 789,
    // CredentialStatus | The id of an integration instance. (optional)
    credentialStatus: ...,
    // Date | The start range of a create date. (optional)
    createDateFrom: 2013-10-20,
    // Date | The end range of a create date . (optional)
    createDateTo: 2013-10-20,
    // number | The id of an integration. (optional)
    integrationId: 789,
    // number | The number of the page to return. (optional)
    pageNumber: 56,
    // string | The name, email or external reference code of a connected user. (optional)
    search: search_example,
  } satisfies GetConnectedUsersRequest;

  try {
    const data = await api.getConnectedUsers(body);
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
| **environmentId** | `number` | The environment id. | [Optional] [Defaults to `undefined`] |
| **credentialStatus** | `CredentialStatus` | The id of an integration instance. | [Optional] [Defaults to `undefined`] [Enum: VALID, INVALID] |
| **createDateFrom** | `Date` | The start range of a create date. | [Optional] [Defaults to `undefined`] |
| **createDateTo** | `Date` | The end range of a create date . | [Optional] [Defaults to `undefined`] |
| **integrationId** | `number` | The id of an integration. | [Optional] [Defaults to `undefined`] |
| **pageNumber** | `number` | The number of the page to return. | [Optional] [Defaults to `0`] |
| **search** | `string` | The name, email or external reference code of a connected user. | [Optional] [Defaults to `undefined`] |

### Return type

[**Page**](Page.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The page of connected users. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

