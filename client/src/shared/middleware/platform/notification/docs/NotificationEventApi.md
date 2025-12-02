# NotificationEventApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getNotificationEvents**](NotificationEventApi.md#getnotificationevents) | **GET** /notifications/events | Get a list of possible notification events |



## getNotificationEvents

> Array&lt;NotificationEvent&gt; getNotificationEvents()

Get a list of possible notification events

Get a list of possible notification events

### Example

```ts
import {
  Configuration,
  NotificationEventApi,
} from '';
import type { GetNotificationEventsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new NotificationEventApi();

  try {
    const data = await api.getNotificationEvents();
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

[**Array&lt;NotificationEvent&gt;**](NotificationEvent.md)

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

