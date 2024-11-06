---
title: "Request"
description: "Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response."
---
## Reference
<hr />

Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response.


Categories: [helpers]


Version: 1

<hr />




## Triggers


### Auto Respond with HTTP 200 Status
The request trigger always replies immediately with an HTTP 200 status code in response to any incoming workflow request request. This guarantees execution of the request trigger, but does not involve any validation of the received request.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
null





### Await Workflow and Respond
You have the flexibility to set up your preferred response. After a workflow request is received, the request trigger enters a waiting state for the workflow's response.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Timeout (ms) | INTEGER | INTEGER  |  The incoming request will time out after the specified number of milliseconds. The max wait time before a timeout is 5 minutes.  |





<hr />



