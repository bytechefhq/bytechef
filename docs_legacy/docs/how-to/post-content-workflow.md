---
id: post-content-workflow
title: Post Content Sample
sidebar_label: Post Content Workflow
---

# 1. Goal

Post file content to REST API endpoint and print response status in log.

# 2. Requirements
- understand Byte Chef concepts described in Get REST Response Workflow
- understand concept of REST API
- have REST API Endpoint of choice to test example

# 3. Implementation guide

- create yaml file post_content.yaml
  - copy/paste following content to the post_content.yaml file:
    ```
    label: Post Content to REST Endpoint
    inputs:
      - name: apiToken
        label: Token generated with Basic Auth Algorithm
        type: string
        required: true
      - name: apiEndpointUrl
        label: API Endpoint URL
        type: string
        required: true
      - name: sourceFileUrl
        label: Full path of file in local file system that contains data bytes
        type: string
        required: false

    tasks:
      - type: filesystem/v1/readFile
        name: csvFileEntry
        description: Read file at `filename` location on local file system and create `csvFileEntry` object in memory
        filename: ${sourceFileUrl}

      - type: httpClient/v1/post
        name: apiHttpPost
        description: >
          Post content of `csvFileEntry` to Liferay `apiEndpointUrl`. `bodyContent` is
          set to `csvFileEntry` to tell processor to seek content bytes in fileEntry `csvFileEntry`
          and bodyContentType `TXT` creates request's `ContentType` header value application/text
        label: Post content to Liferay REST endpoint
        uri: ${apiEndpointUrl}
        responseType: JSON
        headerParameters:
          Authorization:
            - Basic ${apiToken}
        bodyContentType: BINARY
        bodyContent: ${csvFileEntry}

      - type: logger/v1/info
        name: loggApiEndpointPostResponse
        description: Print received response content in log with INFO log level
        text: Result ${apiHttpPost}
    ```
## 3.1 Workflow Definition 
In this workflow `fileSystem` component of version `v1` is introduced. Its `readFile` operation requires `filename` passed via `sourceFileUrl` input argument. `name` declares `csvFileEntry` variable name accessible to subsequent tasks that will contain csv file content to be sent to endpoint.
Declared input arguments `apiToken` and `àpiEndpointUrl` are required for httpClient POST to set URL and create authorized connection to it.

Execution flow performs three steps in `tasks` list. The last step `logger/v1/info` prints out content of variable `restApiResponseContent`.

`httpClient/v1/post` mandatory fields are `uri` and `Authorization` header parameter. In this scenario `httpClient` sets bodyContentType to BINARY and bodyContent to file content preserved in csvFileEntry variable with expression ${csvFileEntry}.   
````
  - type: httpClient/v1/get
    name: restApiResponseContent
    label: Post content to Liferay REST endpoint
    uri: ${apiEndpointUrl}
    responseType: JSON
    headerParameters:
      Authorization:
        - Basic ${apiToken}
    bodyContentType: BINARY
    bodyContent: ${csvFileEntry}
````
Assuming practitioner understands `logger/v1/info` task setup and outcome we consider content of post-content.yaml is ready for execution.
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
