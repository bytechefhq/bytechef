---
title: "Webhook"
description: "Webhook is a method utilized by applications to supply real-time information to other apps. Such a process usually delivers data immediately as and when it occurs. Webhook Trigger enables users to receive callouts whenever a service provides the option of distributing signals to a user-defined URL."
---
## Reference
<hr />

Webhook is a method utilized by applications to supply real-time information to other apps. Such a process usually delivers data immediately as and when it occurs. Webhook Trigger enables users to receive callouts whenever a service provides the option of distributing signals to a user-defined URL.


Categories: [HELPERS]


Version: 1

<hr />




## Triggers


### Auto Respond with HTTP 200 status
The webhook trigger always replies immediately with an HTTP 200 status code in response to any incoming webhook request. This guarantees execution of the webhook trigger, but does not involve any validation of the received request.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
null





### Validate and respond
Upon receiving a webhook request, it goes through a validation process. Once validated, the webhook trigger responds to the sender with an appropriate HTTP status code.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| CSRF Token | STRING | TEXT  |





### Await workflow and respond
You have the flexibility to set up your preferred response. After a webhook request is received, the webhook trigger enters a waiting state for the workflow's response.

#### Type: STATIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| CSRF Token | STRING | TEXT  |
| Timeout (ms) | INTEGER | INTEGER  |





<hr />



