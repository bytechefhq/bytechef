# Oauth2Api

All URIs are relative to */api/platform/internal*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getOAuth2AuthorizationParameters**](Oauth2Api.md#getoauth2authorizationparametersoperation) | **POST** /oauth2/authorization-parameters | Retrieves oauth2 authorization parameters |
| [**getOAuth2Properties**](Oauth2Api.md#getoauth2properties) | **GET** /oauth2/properties | Get OAuth2 properties |



## getOAuth2AuthorizationParameters

> OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(getOAuth2AuthorizationParametersRequest)

Retrieves oauth2 authorization parameters

Retrieves oauth2 authorization parameters.

### Example

```ts
import {
  Configuration,
  Oauth2Api,
} from '';
import type { GetOAuth2AuthorizationParametersOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new Oauth2Api();

  const body = {
    // GetOAuth2AuthorizationParametersRequest
    getOAuth2AuthorizationParametersRequest: ...,
  } satisfies GetOAuth2AuthorizationParametersOperationRequest;

  try {
    const data = await api.getOAuth2AuthorizationParameters(body);
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
| **getOAuth2AuthorizationParametersRequest** | [GetOAuth2AuthorizationParametersRequest](GetOAuth2AuthorizationParametersRequest.md) |  | |

### Return type

[**OAuth2AuthorizationParameters**](OAuth2AuthorizationParameters.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The object with oauth2 authorization parameters. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getOAuth2Properties

> OAuth2Properties getOAuth2Properties()

Get OAuth2 properties

Get OAuth2 properties.

### Example

```ts
import {
  Configuration,
  Oauth2Api,
} from '';
import type { GetOAuth2PropertiesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new Oauth2Api();

  try {
    const data = await api.getOAuth2Properties();
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

[**OAuth2Properties**](OAuth2Properties.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The OAuth2Properties object. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

