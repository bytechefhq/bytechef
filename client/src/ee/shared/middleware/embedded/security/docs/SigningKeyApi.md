# SigningKeyApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createSigningKey**](SigningKeyApi.md#createsigningkey) | **POST** /signing-keys | Create a new Signing key |
| [**deleteSigningKey**](SigningKeyApi.md#deletesigningkey) | **DELETE** /signing-keys/{id} | Delete an Signing key |
| [**getSigningKey**](SigningKeyApi.md#getsigningkey) | **GET** /signing-keys/{id} | Get an Signing key by id |
| [**getSigningKeys**](SigningKeyApi.md#getsigningkeys) | **GET** /signing-keys | Get Signing keys |
| [**updateSigningKey**](SigningKeyApi.md#updatesigningkey) | **PUT** /signing-keys/{id} | Update an existing Signing key |



## createSigningKey

> CreateSigningKey200Response createSigningKey(signingKey)

Create a new Signing key

Create a new Signing key.

### Example

```ts
import {
  Configuration,
  SigningKeyApi,
} from '';
import type { CreateSigningKeyRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SigningKeyApi();

  const body = {
    // SigningKey
    signingKey: ...,
  } satisfies CreateSigningKeyRequest;

  try {
    const data = await api.createSigningKey(body);
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
| **signingKey** | [SigningKey](SigningKey.md) |  | |

### Return type

[**CreateSigningKey200Response**](CreateSigningKey200Response.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The Signing key object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteSigningKey

> deleteSigningKey(id)

Delete an Signing key

Delete an Signing key.

### Example

```ts
import {
  Configuration,
  SigningKeyApi,
} from '';
import type { DeleteSigningKeyRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SigningKeyApi();

  const body = {
    // number | The id of an Signing key.
    id: 789,
  } satisfies DeleteSigningKeyRequest;

  try {
    const data = await api.deleteSigningKey(body);
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
| **id** | `number` | The id of an Signing key. | [Defaults to `undefined`] |

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


## getSigningKey

> SigningKey getSigningKey(id)

Get an Signing key by id

Get an Signing key by id.

### Example

```ts
import {
  Configuration,
  SigningKeyApi,
} from '';
import type { GetSigningKeyRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SigningKeyApi();

  const body = {
    // number | The id of an Signing key.
    id: 789,
  } satisfies GetSigningKeyRequest;

  try {
    const data = await api.getSigningKey(body);
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
| **id** | `number` | The id of an Signing key. | [Defaults to `undefined`] |

### Return type

[**SigningKey**](SigningKey.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The Signing key object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getSigningKeys

> Array&lt;SigningKey&gt; getSigningKeys(environmentId)

Get Signing keys

Get Signing keys.

### Example

```ts
import {
  Configuration,
  SigningKeyApi,
} from '';
import type { GetSigningKeysRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SigningKeyApi();

  const body = {
    // number | The id of an environment.
    environmentId: 789,
  } satisfies GetSigningKeysRequest;

  try {
    const data = await api.getSigningKeys(body);
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
| **environmentId** | `number` | The id of an environment. | [Defaults to `undefined`] |

### Return type

[**Array&lt;SigningKey&gt;**](SigningKey.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of Signing keys. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateSigningKey

> updateSigningKey(id, signingKey)

Update an existing Signing key

Update an existing Signing key.

### Example

```ts
import {
  Configuration,
  SigningKeyApi,
} from '';
import type { UpdateSigningKeyRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new SigningKeyApi();

  const body = {
    // number | The id of an Signing key.
    id: 789,
    // SigningKey
    signingKey: ...,
  } satisfies UpdateSigningKeyRequest;

  try {
    const data = await api.updateSigningKey(body);
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
| **id** | `number` | The id of an Signing key. | [Defaults to `undefined`] |
| **signingKey** | [SigningKey](SigningKey.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

