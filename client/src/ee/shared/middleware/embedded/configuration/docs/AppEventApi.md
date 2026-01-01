# AppEventApi

All URIs are relative to */api/embedded/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createAppEvent**](AppEventApi.md#createappevent) | **POST** /app-events | Create a new app event |
| [**deleteAppEvent**](AppEventApi.md#deleteappevent) | **DELETE** /app-events/{id} | Delete an app event |
| [**getAppEvent**](AppEventApi.md#getappevent) | **GET** /app-events/{id} | Get an app event by id |
| [**getAppEvents**](AppEventApi.md#getappevents) | **GET** /app-events | Get app events |
| [**updateAppEvent**](AppEventApi.md#updateappevent) | **PUT** /app-events/{id} | Update an existing app event |



## createAppEvent

> number createAppEvent(appEvent)

Create a new app event

Create a new app event.

### Example

```ts
import {
  Configuration,
  AppEventApi,
} from '';
import type { CreateAppEventRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AppEventApi();

  const body = {
    // AppEvent
    appEvent: ...,
  } satisfies CreateAppEventRequest;

  try {
    const data = await api.createAppEvent(body);
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
| **appEvent** | [AppEvent](AppEvent.md) |  | |

### Return type

**number**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The app event id. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteAppEvent

> deleteAppEvent(id)

Delete an app event

Delete an app event.

### Example

```ts
import {
  Configuration,
  AppEventApi,
} from '';
import type { DeleteAppEventRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AppEventApi();

  const body = {
    // number | The id of an app event.
    id: 789,
  } satisfies DeleteAppEventRequest;

  try {
    const data = await api.deleteAppEvent(body);
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
| **id** | `number` | The id of an app event. | [Defaults to `undefined`] |

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


## getAppEvent

> AppEvent getAppEvent(id)

Get an app event by id

Get an app event by id.

### Example

```ts
import {
  Configuration,
  AppEventApi,
} from '';
import type { GetAppEventRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AppEventApi();

  const body = {
    // number | The id of an app event.
    id: 789,
  } satisfies GetAppEventRequest;

  try {
    const data = await api.getAppEvent(body);
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
| **id** | `number` | The id of an app event. | [Defaults to `undefined`] |

### Return type

[**AppEvent**](AppEvent.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The app event object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getAppEvents

> Array&lt;AppEvent&gt; getAppEvents()

Get app events

Get app events.

### Example

```ts
import {
  Configuration,
  AppEventApi,
} from '';
import type { GetAppEventsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AppEventApi();

  try {
    const data = await api.getAppEvents();
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

[**Array&lt;AppEvent&gt;**](AppEvent.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The list of app events. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateAppEvent

> updateAppEvent(id, appEvent)

Update an existing app event

Update an existing app event.

### Example

```ts
import {
  Configuration,
  AppEventApi,
} from '';
import type { UpdateAppEventRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new AppEventApi();

  const body = {
    // number | The id of an app event.
    id: 789,
    // AppEvent
    appEvent: ...,
  } satisfies UpdateAppEventRequest;

  try {
    const data = await api.updateAppEvent(body);
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
| **id** | `number` | The id of an app event. | [Defaults to `undefined`] |
| **appEvent** | [AppEvent](AppEvent.md) |  | |

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

