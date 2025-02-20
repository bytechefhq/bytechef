---
title: "RabbitMQ"
description: "RabbitMQ is an open-source message broker software that enables efficient communication between different systems, applications, and services. It supports multiple messaging protocols and facilitates a reliable and flexible messaging system."
---

RabbitMQ is an open-source message broker software that enables efficient communication between different systems, applications, and services. It supports multiple messaging protocols and facilitates a reliable and flexible messaging system.



Type: rabbitMQ/v1

<hr />



## Connections

Version: 1

null



<hr />



## Actions


### Send Message
Name: sendMessage

Send a new RabbitMQ message.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| queue | null | STRING | TEXT | The name of the queue to read from | true |
| message | null | OBJECT <details> <summary> Properties </summary> {} </details> | OBJECT_BUILDER | The name of the queue to read from | true |


#### JSON Example
```json
{
  "label" : "Send Message",
  "name" : "sendMessage",
  "parameters" : {
    "queue" : "",
    "message" : { }
  },
  "type" : "rabbitMQ/v1/sendMessage"
}
```




## Triggers


### New Message
Name: newMessage

Triggers on new RabbitMQ messages.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| queue | null | STRING | TEXT | The name of the queue to read from | true |


#### JSON Example
```json
{
  "label" : "New Message",
  "name" : "newMessage",
  "parameters" : {
    "queue" : ""
  },
  "type" : "rabbitMQ/v1/newMessage"
}
```


<hr />

