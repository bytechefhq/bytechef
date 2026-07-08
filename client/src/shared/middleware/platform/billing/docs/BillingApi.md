# BillingApi

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**cancelSubscription**](BillingApi.md#cancelsubscription) | **DELETE** /billing/subscription | Cancel the current billing subscription at period end |
| [**createCheckoutSession**](BillingApi.md#createcheckoutsession) | **POST** /billing/checkout | Create a Stripe Checkout Session |
| [**getCurrentSubscription**](BillingApi.md#getcurrentsubscription) | **GET** /billing/subscription | Get the current billing subscription |
| [**reactivateSubscription**](BillingApi.md#reactivatesubscription) | **POST** /billing/reactivate | Reactivate a cancelled-at-period-end billing subscription |
| [**upgradeSubscription**](BillingApi.md#upgradesubscription) | **PUT** /billing/subscription | Upgrade or downgrade the current billing subscription |



## cancelSubscription

> cancelSubscription()

Cancel the current billing subscription at period end

Cancel the current billing subscription at the end of the current billing period.

### Example

```ts
import {
  Configuration,
  BillingApi,
} from '';
import type { CancelSubscriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new BillingApi();

  try {
    const data = await api.cancelSubscription();
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


## createCheckoutSession

> CheckoutSession createCheckoutSession(checkoutSessionRequest)

Create a Stripe Checkout Session

Create a Stripe Checkout Session

### Example

```ts
import {
  Configuration,
  BillingApi,
} from '';
import type { CreateCheckoutSessionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new BillingApi();

  const body = {
    // CheckoutSessionRequest
    checkoutSessionRequest: ...,
  } satisfies CreateCheckoutSessionRequest;

  try {
    const data = await api.createCheckoutSession(body);
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
| **checkoutSessionRequest** | [CheckoutSessionRequest](CheckoutSessionRequest.md) |  | |

### Return type

[**CheckoutSession**](CheckoutSession.md)

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


## getCurrentSubscription

> BillingSubscription getCurrentSubscription()

Get the current billing subscription

Get the current billing subscription

### Example

```ts
import {
  Configuration,
  BillingApi,
} from '';
import type { GetCurrentSubscriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new BillingApi();

  try {
    const data = await api.getCurrentSubscription();
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

[**BillingSubscription**](BillingSubscription.md)

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


## reactivateSubscription

> reactivateSubscription()

Reactivate a cancelled-at-period-end billing subscription

Reactivate a subscription that was scheduled for cancellation at period end.

### Example

```ts
import {
  Configuration,
  BillingApi,
} from '';
import type { ReactivateSubscriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new BillingApi();

  try {
    const data = await api.reactivateSubscription();
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


## upgradeSubscription

> upgradeSubscription(checkoutSessionRequest)

Upgrade or downgrade the current billing subscription

Upgrade or downgrade the current billing subscription. Upgrades are applied immediately with prorated charge; downgrades are scheduled for the end of the current billing period.

### Example

```ts
import {
  Configuration,
  BillingApi,
} from '';
import type { UpgradeSubscriptionRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new BillingApi();

  const body = {
    // CheckoutSessionRequest
    checkoutSessionRequest: ...,
  } satisfies UpgradeSubscriptionRequest;

  try {
    const data = await api.upgradeSubscription(body);
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
| **checkoutSessionRequest** | [CheckoutSessionRequest](CheckoutSessionRequest.md) |  | |

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

