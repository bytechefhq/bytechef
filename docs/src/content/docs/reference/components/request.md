---
title: "Request"
description: "Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response."
---

Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response.


Categories: helpers


Type: request/v1

<hr />






## Triggers


### Auto Respond with HTTP 200 Status
Name: autoRespondWithHTTP200

The request trigger always replies immediately with an HTTP 200 status code in response to any incoming workflow request request. This guarantees execution of the request trigger, but does not involve any validation of the received request.

Type: STATIC_WEBHOOK


#### JSON Example
```json
{
  "label" : "Auto Respond with HTTP 200 Status",
  "name" : "autoRespondWithHTTP200",
  "type" : "request/v1/autoRespondWithHTTP200"
}
```


### Await Workflow and Respond
Name: awaitWorkflowAndRespond

You have the flexibility to set up your preferred response. After a workflow request is received, the request trigger enters a waiting state for the workflow's response.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| timeout | Timeout (ms) | INTEGER | The incoming request will time out after the specified number of milliseconds. The max wait time before a timeout is 5 minutes. | null |


#### JSON Example
```json
{
  "label" : "Await Workflow and Respond",
  "name" : "awaitWorkflowAndRespond",
  "parameters" : {
    "timeout" : 1
  },
  "type" : "request/v1/awaitWorkflowAndRespond"
}
```


<hr />

