# NotificationApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createNotification**](NotificationApi.md#createnotification) | **POST** /notifications | Create a notification entry |
| [**deleteNotification**](NotificationApi.md#deletenotification) | **DELETE** /notifications/{notificationId} | Delete a notification |
| [**getNotifications**](NotificationApi.md#getnotifications) | **GET** /notifications | Get a list of notifications |
| [**regenerateNotificationSigningSecret**](NotificationApi.md#regeneratenotificationsigningsecret) | **POST** /notifications/{notificationId}/signing-secret | Regenerate the signing secret of a webhook notification |
| [**testNotification**](NotificationApi.md#testnotification) | **POST** /notifications/test | Send a test notification |
| [**updateNotification**](NotificationApi.md#updatenotification) | **PUT** /notifications/{notificationId} | Update an existing notification |



## createNotification

> Notification createNotification(notification)

Create a notification entry

Create a notification entry

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { CreateNotificationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  const body = {
    // Notification
    notification: ...,
  } satisfies CreateNotificationRequest;

  try {
    const data = await api.createNotification(body);
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
| **notification** | [Notification](Notification.md) |  | |

### Return type

[**Notification**](Notification.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteNotification

> deleteNotification(notificationId)

Delete a notification

Delete a notification.

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { DeleteNotificationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  const body = {
    // number | The id of a notification.
    notificationId: 789,
  } satisfies DeleteNotificationRequest;

  try {
    const data = await api.deleteNotification(body);
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
| **notificationId** | `number` | The id of a notification. | [Defaults to `undefined`] |

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


## getNotifications

> Array&lt;Notification&gt; getNotifications()

Get a list of notifications

Get a list of notifications

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { GetNotificationsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  try {
    const data = await api.getNotifications();
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

[**Array&lt;Notification&gt;**](Notification.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## regenerateNotificationSigningSecret

> NotificationSigningSecret regenerateNotificationSigningSecret(notificationId)

Regenerate the signing secret of a webhook notification

Regenerates the signing secret of a webhook notification. The new secret is returned only once.

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { RegenerateNotificationSigningSecretRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  const body = {
    // number | The id of a notification.
    notificationId: 789,
  } satisfies RegenerateNotificationSigningSecretRequest;

  try {
    const data = await api.regenerateNotificationSigningSecret(body);
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
| **notificationId** | `number` | The id of a notification. | [Defaults to `undefined`] |

### Return type

[**NotificationSigningSecret**](NotificationSigningSecret.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The new signing secret. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## testNotification

> NotificationTestResult testNotification(notification, notificationId)

Send a test notification

Sends a sample JOB_FAILED notification to the configured destination without saving anything. Masked values are resolved against the stored notification when an id is given.

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { TestNotificationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  const body = {
    // Notification
    notification: ...,
    // number | The id of the saved notification whose masked values and signing secret the test uses. Omit it for a notification that is not saved yet. (optional)
    notificationId: 789,
  } satisfies TestNotificationRequest;

  try {
    const data = await api.testNotification(body);
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
| **notification** | [Notification](Notification.md) |  | |
| **notificationId** | `number` | The id of the saved notification whose masked values and signing secret the test uses. Omit it for a notification that is not saved yet. | [Optional] [Defaults to `undefined`] |

### Return type

[**NotificationTestResult**](NotificationTestResult.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The outcome of the test send. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateNotification

> Notification updateNotification(notificationId, notification)

Update an existing notification

Update an existing notification.

### Example

```ts
import {
  Configuration,
  NotificationApi,
} from '';
import type { UpdateNotificationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationApi();

  const body = {
    // number | The id of a notification.
    notificationId: 789,
    // Notification
    notification: ...,
  } satisfies UpdateNotificationRequest;

  try {
    const data = await api.updateNotification(body);
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
| **notificationId** | `number` | The id of a notification. | [Defaults to `undefined`] |
| **notification** | [Notification](Notification.md) |  | |

### Return type

[**Notification**](Notification.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated Notification object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

