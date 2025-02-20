---
title: "HTTP Client"
description: "Makes an HTTP request and returns the response data."
---

Makes an HTTP request and returns the response data.


Categories: helpers


Type: httpClient/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| key | Key | STRING | TEXT |  | true |
| value | Value | STRING | TEXT |  | true |
| addTo | Add to | STRING <details> <summary> Options </summary> HEADER, QUERY_PARAMETERS </details> | SELECT |  | true |



### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |



### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Username | STRING | TEXT |  | true |
| password | Password | STRING | TEXT |  | true |



### Digest Auth

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| username | Username | STRING | TEXT |  | true |
| password | Password | STRING | TEXT |  | true |



### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| authorizationUrl | Authorization URL | STRING | TEXT |  | true |
| tokenUrl | Token URL | STRING | TEXT |  | true |
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| headerPrefix | Header Prefix | STRING | TEXT |  | null |
| scopes | Scopes | STRING | TEXT_AREA | Optional comma-delimited list of scopes | null |



### OAuth2 Implicit Code

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| authorizationUrl | Authorization URL | STRING | TEXT |  | true |
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| headerPrefix | Header Prefix | STRING | TEXT |  | null |
| scopes | Scopes | STRING | TEXT_AREA | Optional comma-delimited list of scopes | null |



### OAuth2 Client Credentials

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| tokenUrl | Token URL | STRING | TEXT |  | true |
| clientId | Client Id | STRING | TEXT |  | true |
| clientSecret | Client Secret | STRING | TEXT |  | true |
| headerPrefix | Header Prefix | STRING | TEXT |  | null |
| scopes | Scopes | STRING | TEXT_AREA | Optional comma-delimited list of scopes | null |





<hr />



## Actions


### GET
Name: get

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "GET",
  "name" : "get",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/get"
}
```


### POST
Name: post

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| body | Body | OBJECT <details> <summary> Properties </summary> {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} </details> | OBJECT_BUILDER | The body of the request. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "POST",
  "name" : "post",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "body" : {
      "bodyContentType" : "",
      "bodyContent" : {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      },
      "bodyContentMimeType" : ""
    },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/post"
}
```


### PUT
Name: put

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| body | Body | OBJECT <details> <summary> Properties </summary> {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} </details> | OBJECT_BUILDER | The body of the request. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "PUT",
  "name" : "put",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "body" : {
      "bodyContentType" : "",
      "bodyContent" : {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      },
      "bodyContentMimeType" : ""
    },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/put"
}
```


### PATCH
Name: patch

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| body | Body | OBJECT <details> <summary> Properties </summary> {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} </details> | OBJECT_BUILDER | The body of the request. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "PATCH",
  "name" : "patch",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "body" : {
      "bodyContentType" : "",
      "bodyContent" : {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      },
      "bodyContentMimeType" : ""
    },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/patch"
}
```


### DELETE
Name: delete

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "DELETE",
  "name" : "delete",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/delete"
}
```


### HEAD
Name: head

The request method to use.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| uri | URI | STRING | TEXT | The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it. | true |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Download the response even if SSL certificate validation is not possible. | null |
| responseType | Response Format | STRING <details> <summary> Options </summary> JSON, XML, TEXT, BINARY </details> | SELECT | The format in which the data gets returned from the URL. | null |
| responseFilename | Response Filename | STRING | TEXT | The name of the file if the response is returned as a file object. | null |
| headers | Headers | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Headers to send. | null |
| queryParameters | Query Parameters | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | Query parameters to send. | null |
| fullResponse | Full Response | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Returns the full response data instead of only the body. | null |
| followAllRedirects | Follow All Redirects | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow non-GET HTTP 3xx redirects. | null |
| followRedirect | Follow GET Redirect | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Follow GET HTTP 3xx redirects. | null |
| ignoreResponseCode | Ignore Response Code | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Succeeds also when the status code is not 2xx. | null |
| proxy | Proxy | STRING | TEXT | HTTP proxy to use. | null |
| timeout | Timeout | INTEGER | INTEGER | Time in ms to wait for the server to send a response before aborting the request. | null |


#### JSON Example
```json
{
  "label" : "HEAD",
  "name" : "head",
  "parameters" : {
    "uri" : "",
    "allowUnauthorizedCerts" : false,
    "responseType" : "",
    "responseFilename" : "",
    "headers" : { },
    "queryParameters" : { },
    "fullResponse" : false,
    "followAllRedirects" : false,
    "followRedirect" : false,
    "ignoreResponseCode" : false,
    "proxy" : "",
    "timeout" : 1
  },
  "type" : "httpClient/v1/head"
}
```




