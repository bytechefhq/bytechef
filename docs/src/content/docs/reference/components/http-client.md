---
title: "HTTP Client"
description: "Makes an HTTP request and returns the response data."
---
## Reference
<hr />

Makes an HTTP request and returns the response data.


Categories: [HELPERS]


Version: 1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  |
| Value | STRING | TEXT  |  |
| Add to | STRING | SELECT  |  |



### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |



### Basic Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |



### Digest Auth

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Username | STRING | TEXT  |  |
| Password | STRING | TEXT  |  |



### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Authorization URL | STRING | TEXT  |  |
| Token URL | STRING | TEXT  |  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Header Prefix | STRING | TEXT  |  |
| Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |



### OAuth2 Implicit Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Authorization URL | STRING | TEXT  |  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Header Prefix | STRING | TEXT  |  |
| Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |



### OAuth2 Client Credentials

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token URL | STRING | TEXT  |  |
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |
| Header Prefix | STRING | TEXT  |  |
| Scopes | STRING | TEXT_AREA  |  Optional comma-delimited list of scopes  |





<hr />





## Actions


### GET
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




### POST
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Body Content Type | STRING | SELECT  |  Content-Type to use when sending body parameters.  |
| Body Content - JSON | {} | OBJECT_BUILDER  |  Body Parameters to send.  |
| Body Content - XML | {} | OBJECT_BUILDER  |  XML content to send.  |
| Body Content - Form Data | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Form URL-Encoded | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Raw | STRING | TEXT  |  The raw text to send.  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Content Type | STRING | TEXT  |  Mime-Type to use when sending raw body content.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




### PUT
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Body Content - JSON | {} | OBJECT_BUILDER  |  Body Parameters to send.  |
| Body Content - XML | {} | OBJECT_BUILDER  |  XML content to send.  |
| Body Content - Form Data | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Form URL-Encoded | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Raw | STRING | TEXT  |  The raw text to send.  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Body Content Type | STRING | SELECT  |  Content-Type to use when sending body parameters.  |
| Body Content - JSON | {} | OBJECT_BUILDER  |  Body Parameters to send.  |
| Body Content - XML | {} | OBJECT_BUILDER  |  XML content to send.  |
| Body Content - Form Data | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Form URL-Encoded | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Raw | STRING | TEXT  |  The raw text to send.  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Content Type | STRING | TEXT  |  Mime-Type to use when sending raw body content.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




### PATCH
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Body Content - JSON | {} | OBJECT_BUILDER  |  Body Parameters to send.  |
| Body Content - XML | {} | OBJECT_BUILDER  |  XML content to send.  |
| Body Content - Form Data | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Form URL-Encoded | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Raw | STRING | TEXT  |  The raw text to send.  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Body Content Type | STRING | SELECT  |  Content-Type to use when sending body parameters.  |
| Body Content - JSON | {} | OBJECT_BUILDER  |  Body Parameters to send.  |
| Body Content - XML | {} | OBJECT_BUILDER  |  XML content to send.  |
| Body Content - Form Data | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Form URL-Encoded | {} | OBJECT_BUILDER  |  Body parameters to send.  |
| Body Content - Raw | STRING | TEXT  |  The raw text to send.  |
| Body Content - Binary | FILE_ENTRY | FILE_ENTRY  |  The object property which contains a reference to the file to upload.  |
| Content Type | STRING | TEXT  |  Mime-Type to use when sending raw body content.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




### DELETE
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




### HEAD
The request method to use.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| URI | STRING | TEXT  |  The URI to make the request to  |
| Allow Unauthorized Certs | BOOLEAN | SELECT  |  Download the response even if SSL certificate validation is not possible.  |
| Response Format | STRING | SELECT  |  The format in which the data gets returned from the URL.  |
| Response Filename | STRING | TEXT  |  The name of the file if the response is returned as a file object.  |
| Headers | {} | OBJECT_BUILDER  |  Headers to send.  |
| Query Parameters | {} | OBJECT_BUILDER  |  Query parameters to send.  |
| Full Response | BOOLEAN | SELECT  |  Returns the full response data instead of only the body.  |
| Follow All Redirects | BOOLEAN | SELECT  |  Follow non-GET HTTP 3xx redirects.  |
| Follow GET Redirect | BOOLEAN | SELECT  |  Follow GET HTTP 3xx redirects.  |
| Ignore Response Code | BOOLEAN | SELECT  |  Succeeds also when the status code is not 2xx.  |
| Proxy | STRING | TEXT  |  HTTP proxy to use.  |
| Timeout | INTEGER | INTEGER  |  Time in ms to wait for the server to send a response before aborting the request.  |




