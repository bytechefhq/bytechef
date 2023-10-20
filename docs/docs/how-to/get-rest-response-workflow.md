---
id: get-rest-response-workflow
title: Get REST Response Sample
sidebar_label: Get REST Response Workflow
---

# 1. Goal

Get response from REST API endpoint and print response content in log.

# 2. Requirements
- understand Byte Chef concepts described in Hello World Workflow
- understand concept of REST API
- have REST API Endpoint of choice to test example

# 3. Implementation guide

- create yaml file get_content.yaml
- copy/paste following content to the get-content.yaml file:
  ```
  label: Get Content from Liferay DXP
  inputs:
    - name: apiToken
      label: Token generated with Basic Auth Algorithm
      type: string
      required: true
    - name: apiEndpointUrl
      label: API Endpoint URL
      type: string
      required: true

  tasks:
    - type: httpClient/v1/get
      name: httpGetContentFromLiferay
      label: Get web content from Liferay DXP
      uri: ${apiEndpointUrl}
      responseType: JSON
      headerParameters:
        Authorization: Basic ${apiToken}

    - type: logger/v1/info
      name: loggHttpResponse
      text: Result ${httpGetContentFromLiferay}
  ```
## 3.1 Workflow Definition 
In this workflow `httpClient` component of version `v1` is introduced. Its `get` operation requires `apiEndpointUrl` and `apiToken` arguments which are passed via `inputs` node. `label` sets human-readable task name and `name` declares variable name accessible to subsequent tasks that will contain REST API response content.
Declare input arguments:
````
- name: apiToken
  type: string
  required: true
- name: apiEndpointUrl
  type: string
  required: true
````
Both arguments are required as httpClient GET needs URL to connect with and authorization credentials.

Execution flow performs two steps in `tasks` list. The later step is `logger/v1/info`. It prints out content of variable `restApiResponseContent`.

`httpClient/v1/get` mandatory fields are `uri` and `Authorization` header parameter. Description of `httpClient` usage variants and arguments may be found in component technical documentation. Setting `uri` to the value received via input argument using `${}` expression is pretty simple ${apiEndpointUrl}.   
````
  - type: httpClient/v1/get
    name: restApiResponseContent
    label: Get web content from Liferay DXP
    uri: ${apiEndpointUrl}
    responseType: JSON
    headerParameters:
      Authorization:
        - Basic ${apiToken}
```` 
Header parameters are passed in yaml map form where each parameter is key-value element where key is header name and value is array of header values:
````
  [HEADER_NAME]:
    - [HEADER_VALUE_1]
    - [HEADER_VALUE_2]
````
This is valid array example:
````
  Authorization:
    - Basic ${apiToken}
  ContentType:
    - application/json
  Accepts:
    - application/json
    - application/xml
````
Assuming practitioner understands `logger/v1/info` task setup and outcome we consider content of get-content.yaml is ready for execution.
## 3.2 Run Workflow
Take hello-world.yaml and paste it to [BYTE_CHEF_WORKFLOW_DEPLOY_FOLDER]. From browser position to `http://localhost:9555/webjars/swagger-ui/4.14.0/index.html#/JobController/postJob` and provide this payload:

````
{
 "workflowId": "samples/get-content",
  "inputs": {
    "apiToken": "dGVzdEBsaWZlcmF5LmNvbTpfdGVzdA==",
    "apiEndpointUrl": "http://localhost:8080/o/headless-admin-user/accounts"
  }
}
````
click execute and monitor main application terminal. Task is successfully executed if terminal contains log output in json format.

NOTE: apiToken refers to hash of username and password used by Basic authorization method. For this example I used an arbitrary online Basic authorization hash generator at https://www.blitter.se/utils/basic-authentication-header-generator/. As REST API endpoint I took Liferay DXP platform's Account headless endpoint. One can use any other endpoint.

## 3.3 References
A practitioner may refer to this and other completed workflow yaml samples in ByteChef samples folder [bytechef-samples](https://github.com/bytechefhq/bytechef/tree/master/server/apps/server-app/src/main/resources/workflows/samples/get-rest-response-workflow.yaml).
