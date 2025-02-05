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

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| key | Key | STRING | TEXT  |  | true  |
| value | Value | STRING | TEXT  |  | true  |
| addTo | Add to | STRING | SELECT  |  | true  |



### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |



### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  | true  |
| password | Password | STRING | TEXT  |  | true  |



### Digest Auth

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| username | Username | STRING | TEXT  |  | true  |
| password | Password | STRING | TEXT  |  | true  |



### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| authorizationUrl | Authorization URL | STRING | TEXT  |  | true  |
| tokenUrl | Token URL | STRING | TEXT  |  | true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| headerPrefix | Header Prefix | STRING | TEXT  |  | null  |
| scopes | Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |  null  |



### OAuth2 Implicit Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| authorizationUrl | Authorization URL | STRING | TEXT  |  | true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| headerPrefix | Header Prefix | STRING | TEXT  |  | null  |
| scopes | Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |  null  |



### OAuth2 Client Credentials

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| tokenUrl | Token URL | STRING | TEXT  |  | true  |
| clientId | Client Id | STRING | TEXT  |  | true  |
| clientSecret | Client Secret | STRING | TEXT  |  | true  |
| headerPrefix | Header Prefix | STRING | TEXT  |  | null  |
| scopes | Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |  null  |





<hr />



## Actions


### GET
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |




### POST
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| body | Body | {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} | OBJECT_BUILDER  |  The body of the request.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |




### PUT
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| body | Body | {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} | OBJECT_BUILDER  |  The body of the request.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |




### PATCH
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| body | Body | {STRING\(bodyContentType), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), {}\(bodyContent), STRING\(bodyContent), FILE_ENTRY\(bodyContent), STRING\(bodyContentMimeType), STRING\(bodyContentMimeType)} | OBJECT_BUILDER  |  The body of the request.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |




### DELETE
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |




### HEAD
The request method to use.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| uri | URI | STRING | TEXT  |  The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.  |  true  |
| allowUnauthorizedCerts | Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |  null  |
| responseType | Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |  null  |
| responseFilename | Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |  null  |
| headers | Headers | {} | OBJECT_BUILDER  |  Headers to send.  |  null  |
| queryParameters | Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |  null  |
| fullResponse | Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |  null  |
| followAllRedirects | Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |  null  |
| followRedirect | Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |  null  |
| ignoreResponseCode | Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |  null  |
| proxy | Proxy | STRING | TEXT  |  HTTP proxy to use.  |  null  |
| timeout | Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |  null  |






