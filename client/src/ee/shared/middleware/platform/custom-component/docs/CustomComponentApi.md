# CustomComponentApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteCustomComponent**](CustomComponentApi.md#deletecustomcomponent) | **DELETE** /custom-components/{id} | Delete an custom component |
| [**enableCustomComponent**](CustomComponentApi.md#enablecustomcomponent) | **PATCH** /custom-components/{id}/enable/{enable} | Enable/disable a custom component. |
| [**getCustomComponent**](CustomComponentApi.md#getcustomcomponent) | **GET** /custom-components/{id} | Get an custom component by id |
| [**getCustomComponents**](CustomComponentApi.md#getcustomcomponents) | **GET** /custom-components | Get Custom Components |



## deleteCustomComponent

> deleteCustomComponent(id)

Delete an custom component

Delete an custom component.

### Example

```ts
import {
  Configuration,
  CustomComponentApi,
} from '';
import type { DeleteCustomComponentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CustomComponentApi();

  const body = {
    // number | The id of an custom component.
    id: 789,
  } satisfies DeleteCustomComponentRequest;

  try {
    const data = await api.deleteCustomComponent(body);
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
| **id** | `number` | The id of an custom component. | [Defaults to `undefined`] |

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


## enableCustomComponent

> enableCustomComponent(id, enable)

Enable/disable a custom component.

Enable/disable a custom component.

### Example

```ts
import {
  Configuration,
  CustomComponentApi,
} from '';
import type { EnableCustomComponentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CustomComponentApi();

  const body = {
    // number | The id of the custom component.
    id: 789,
    // boolean | Enable/disable the custom component.
    enable: true,
  } satisfies EnableCustomComponentRequest;

  try {
    const data = await api.enableCustomComponent(body);
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
| **id** | `number` | The id of the custom component. | [Defaults to `undefined`] |
| **enable** | `boolean` | Enable/disable the custom component. | [Defaults to `undefined`] |

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


## getCustomComponent

> CustomComponent getCustomComponent(id)

Get an custom component by id

Get an custom component by id.

### Example

```ts
import {
  Configuration,
  CustomComponentApi,
} from '';
import type { GetCustomComponentRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CustomComponentApi();

  const body = {
    // number | The id of an custom component.
    id: 789,
  } satisfies GetCustomComponentRequest;

  try {
    const data = await api.getCustomComponent(body);
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
| **id** | `number` | The id of an custom component. | [Defaults to `undefined`] |

### Return type

[**CustomComponent**](CustomComponent.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The custom component object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getCustomComponents

> Array&lt;CustomComponent&gt; getCustomComponents()

Get Custom Components

Get Custom Components.

### Example

```ts
import {
  Configuration,
  CustomComponentApi,
} from '';
import type { GetCustomComponentsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new CustomComponentApi();

  try {
    const data = await api.getCustomComponents();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;CustomComponent&gt;**](CustomComponent.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A list of Custom Components. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

